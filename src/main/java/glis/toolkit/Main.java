package glis.toolkit;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.http.HttpException;
import org.apache.http.HttpStatus;
import org.joox.Match;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.Sql2o;
import org.w3c.dom.Document;

import javax.xml.parsers.ParserConfigurationException;
import java.text.*;
import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.joox.JOOX.$;

public class Main {

    public static final String CONFIG_PATH = "config.txt";
    public static final String TIMESTAMP   = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());

	public static String  glisUrl;
	public static String  glisUsername;
	public static String  glisPassword;
	public static Integer qlimit;
	public static Sql2o   sql2o;

    public static void main(String[] args) {
        
		File fLock = new File("lock.lck");
		try (Writer fDOI = new FileWriter(TIMESTAMP + "_" + "DOI.txt")) {

        	// file-based locking mechanism to ensure that only one instance is running at any given time
			if (!fLock.exists()) {
  				fLock.createNewFile();
			}
			else {
				System.out.println("Lock file 'lock.lck' exists. Either another instance of the Toolkit is running or it has terminated in an error. Please make sure no other instance of the Toolkit is running, or fix the error, before removing the lock file and trying again");
				System.exit(1);
			}
            // read configuration
            PropertiesConfiguration config   = new Configurations().properties(new File(CONFIG_PATH));
            String                  url      = config.getString("db.url");
            String                  username = config.getString("db.username");
            String                  password = config.getString("db.password");
            
            sql2o        = new Sql2o(url, username, password);
            glisUrl      = config.getString("glis.url");
            qlimit       = Integer.parseInt(config.getString("db.query_limit"));
            glisUsername = config.getString("glis.username");
            glisPassword = config.getString("glis.password");

			//Print configuration
			System.out.println("Configuration");
			System.out.println("Database URL:      [" + url + "]");
			System.out.println("Database username: [" + username + "]");
			System.out.println("Database password: [" + password + "]");
			System.out.println("Query limit:       [" + qlimit + "]");
			System.out.println("GLIS URL:          [" + glisUrl + "]");
			System.out.println("GLIS username:     [" + glisUsername + "]");
			System.out.println("GLIS password:     [" + glisPassword + "]");
			
			// Register first then update
			process("register");
			process("update");
		}
		catch (Exception e) {
            e.printStackTrace();
		}
		finally {
			// remove lock file
			fLock.delete();
		}
    }

	private static void process(String operation) throws Exception {

		// build list of pgrfa ids to register
		List<String> ids = select(sql2o, conn ->
				conn.createQuery("select id from pgrfas where operation=:operation and processed=:processed")
						.addParameter("operation", operation)
						.addParameter("processed", "n"))
				.stream()
				.limit(qlimit)
				.map(m -> m.get("id").toString())
				.collect(Collectors.toList());

		// process each pgrfa
		Integer count = 0;
        Map<String, Object> conf = new HashMap<> ();
		conf.put("glis_username",glisUsername);
		conf.put("glis_password",glisPassword);

		for (String id: ids) {
			Map<String, Object> pgrfa = select(sql2o, conn -> conn.createQuery("select * from pgrfas where id=:id").addParameter("id", id)).get(0);

			String   wiews      = pgrfa.get("hold_wiews").toString();
			String   pid        = pgrfa.get("hold_pid").toString();
			Document doc        = buildDocument(id, pgrfa, conf);

			Document request    = transform(doc,     "transform.xsl");
			request             = transform(request, "prune.xsl");
			String   xmlRequest = $(request).toString();

			HttpResponse<String> httpResponse = Unirest.post(glisUrl)
					.header("accept", "application/xml")
					.body(xmlRequest)
					.asString();
			String   xmlResponse = httpResponse.getBody();
			Document response = $(xmlResponse).document();
			if (httpResponse.getStatus() == HttpStatus.SC_OK) {

				// do something...
			}
			else {
				String sampleId = $(response).child("sampleid").text();
				String genus    = $(response).child("genus").text();
				String error    = $(response).child("error").text();

				System.err.println(sampleId + " - " + genus + " - " + error);

				throw new HttpException(httpResponse.getStatus() + " " + httpResponse.getStatusText());
			}
		}
	}

	private static Document buildDocument(String id, Map<String, Object> pgrfa, Map<String, Object> conf) throws ParserConfigurationException {

		// get related tables
		List<Map<String, Object>> actors      = select(sql2o, conn -> conn.createQuery("select * from actors      where pgrfa_id=:pgrfa_id").addParameter("pgrfa_id", id));
		List<Map<String, Object>> identifiers = select(sql2o, conn -> conn.createQuery("select * from identifiers where pgrfa_id=:pgrfa_id").addParameter("pgrfa_id", id));
		List<Map<String, Object>> names       = select(sql2o, conn -> conn.createQuery("select * from names       where pgrfa_id=:pgrfa_id").addParameter("pgrfa_id", id));
		List<Map<String, Object>> progdois    = select(sql2o, conn -> conn.createQuery("select * from progdois    where pgrfa_id=:pgrfa_id").addParameter("pgrfa_id", id));
		List<Map<String, Object>> targets     = select(sql2o, conn -> conn.createQuery("select * from targets     where pgrfa_id=:pgrfa_id").addParameter("pgrfa_id", id));
		List<Map<String, Object>> tkws        = select(sql2o, conn -> conn.createQuery("select k.* from tkws k, targets t where t.pgrfa_id=:pgrfa_id and t.id=k.target_id").addParameter("pgrfa_id", id));

		return $("root",
				 addMap($("conf"),        conf),
				 addMap($("pgrfa"),       pgrfa),
				 addList($("actor"),      actors),
				 addList($("identifier"), identifiers),
				 addList($("name"),       names),
				 addList($("progdoi"),    progdois),
				 addList($("target"),     targets),
				 addList($("tkw"),        tkws)
		).document();
	}

	private static Document transform(Document doc, String xslPath) {
		return $(doc)
				.transform(Main.class.getClassLoader().getResourceAsStream(xslPath))
				.document();
	}

    // more readable than sql2o.withConnection...
    private static List<Map<String, Object>> select(Sql2o sql2o, Function<Connection, Query> query) {
        try (Connection conn = sql2o.open()) {
            return query.apply(conn).executeAndFetchTable().asList();
        }
    }

	private static Match addList(Match m, List<Map<String, Object>> list) {
		list.forEach(map -> addMap(m, map));
		return m;
	}

	private static Match addMap(Match m, Map<String, Object> map) {
		map.entrySet().stream()
				.filter(entry -> entry.getValue() != null)
				.forEach(entry -> m.append($(entry.getKey()).text(entry.getValue().toString())));
		return m;
	}
}
