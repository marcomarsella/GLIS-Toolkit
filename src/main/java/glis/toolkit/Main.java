package glis.toolkit;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.http.HttpStatus;
import org.joox.Match;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.Sql2o;
import org.w3c.dom.Document;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.joox.JOOX.$;

public class Main {

    private static final String CONFIG_PATH = "config.txt";
	private static final String TIMESTAMP   = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());

	private static String  glisUrl;
	private static String  glisUsername;
	private static String  glisPassword;
	private static Integer qlimit;
	private static Sql2o   sql2o;

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
			process("register", fDOI);
			process("update", fDOI);

			// remove lock file
			fLock.delete();
		}
		catch (Exception e) {
            e.printStackTrace();
		}
    }

	private static void process(String operation, Writer fDOI) throws Exception {

		// build list of pgrfa ids to register
		List<String> ids = select(conn ->
				conn.createQuery("select id from pgrfas where operation=:operation and processed=:processed")
						.addParameter("operation", operation)
						.addParameter("processed", "n"))
				.stream()
				.limit(qlimit)
				.map(m -> m.get("id").toString())
				.collect(Collectors.toList());

		// process each pgrfa
        Map<String, Object> conf = new HashMap<> ();
		conf.put("glis_username",glisUsername);
		conf.put("glis_password",glisPassword);

		Integer count = 0;
		for (String id: ids) {
			Map<String, Object> pgrfa = select(conn -> conn.createQuery("select * from pgrfas where id=:id").addParameter("id", id)).get(0);

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

			//Extract relevant information, if available
			String doi      = $(response).child("doi").text();
			String sampleId = $(response).child("sampleid").text();
			String genus    = $(response).child("genus").text();
			String error    = $(response).child("error").text();

			//System.out.println(genus + "\t" + sampleId + "\t" + doi + "\t" + error);

			String result   = ((error != null) && (!error.isEmpty())) ? "KO" : "OK";

			if (httpResponse.getStatus() == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
				System.err.println(sampleId + " - " + genus + " - " + error);
				System.exit(1);
			}
			if (Objects.equals(result,"OK")) {
				fDOI.write(wiews + "\t" + pid + "\t" + genus + "\t" + sampleId + "\t" + doi + "\n");
			}
			// Write result to DB
			insertResult(operation, result, doi, sampleId, genus, error);
			markAsProcessed(id);
			count++;
			System.out.println("Processed sample [" + count.toString() + "][" + sampleId + "]: " + result);
		}
	}

	private static Document buildDocument(String id, Map<String, Object> pgrfa, Map<String, Object> conf) throws ParserConfigurationException {

		// get related tables
		List<Map<String, Object>> actors      = select(conn -> conn.createQuery("select * from actors      where pgrfa_id=:pgrfa_id").addParameter("pgrfa_id", id));
		List<Map<String, Object>> identifiers = select(conn -> conn.createQuery("select * from identifiers where pgrfa_id=:pgrfa_id").addParameter("pgrfa_id", id));
		List<Map<String, Object>> names       = select(conn -> conn.createQuery("select * from names       where pgrfa_id=:pgrfa_id").addParameter("pgrfa_id", id));
		List<Map<String, Object>> progdois    = select(conn -> conn.createQuery("select * from progdois    where pgrfa_id=:pgrfa_id").addParameter("pgrfa_id", id));
		List<Map<String, Object>> targets     = select(conn -> conn.createQuery("select * from targets     where pgrfa_id=:pgrfa_id").addParameter("pgrfa_id", id));
		List<Map<String, Object>> tkws        = select(conn -> conn.createQuery("select k.* from tkws k, targets t where t.pgrfa_id=:pgrfa_id and t.id=k.target_id").addParameter("pgrfa_id", id));

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
	private static void markAsProcessed(String id) {
		final String query = "update pgrfas set processed = 'y' where id=:id";
		try (Connection conn = sql2o.open()) {
			conn.createQuery(query)
					.addParameter("id", Integer.parseInt(id))
					.executeUpdate();
		}
	}

	// more readable than sql2o.withConnection...
	private static void insertResult(String operation, String result, String doi, String sampleId, String genus, String error) {
		final String query = "insert into results (operation, genus, sample_id, doi, result, error) values(:ope, :gen, :sid, :doi, :res, :err)";
		try (Connection conn = sql2o.open()) {
			conn.createQuery(query)
					.addParameter("ope", operation)
					.addParameter("gen", genus)
					.addParameter("sid", sampleId)
					.addParameter("doi", doi)
					.addParameter("res", result)
					.addParameter("err", error)
					.executeUpdate();
		}
	}

	// more readable than sql2o.withConnection...
	private static List<Map<String, Object>> select(Function<Connection, Query> query) {
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
