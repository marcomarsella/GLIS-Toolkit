package glis.toolkit;

import com.github.fluentxml4j.FluentXml;
import com.jamesmurty.utils.XMLBuilder;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
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

public class Main {

    public static final String CONFIG_PATH = "config.txt";
    public static String timeStamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
	public static Writer fDOI         = null;
	public static String glisUrl      = null;
	public static String glisUsername = null;
	public static String glisPassword = null;
	public static Integer qlimit      = 0;
	public static Sql2o sql2o = null;

    public static void main(String[] args) {
        
        File fLock = null;

		try {
  			fDOI = new FileWriter(timeStamp + "_" + "DOI.txt");       
        	//File-based locking mechanism to ensure that only one instance is running at any given time
        	fLock = new File("lock.lck");
			if(!fLock.exists()){
  				fLock.createNewFile();
			} else {
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

			fDOI.close();			// Close DOI file
			fLock.delete();			// Remove lock file
        } catch (Exception e) {
            e.printStackTrace();
		}
    }

	private static void process(String operation) throws ParserConfigurationException {

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
		for (String id: ids) {

			Map<String, Object> conf = new HashMap<> ();
			conf.put("glis_username",glisUsername);
			conf.put("glis_password",glisPassword);

			Document doc     = buildDocument(id, conf);
			Document message = transformDocument(doc);

			FluentXml.serialize(message).to(System.out);
		}
	}

	private static Document buildDocument(String id, Map<String, Object> conf) throws ParserConfigurationException {
		// get related tables
		List<Map<String, Object>> pgrfa       = select(sql2o, conn -> conn.createQuery("select * from pgrfas      where id=:id").addParameter("id", id));
		List<Map<String, Object>> actors      = select(sql2o, conn -> conn.createQuery("select * from actors      where pgrfa_id=:pgrfa_id").addParameter("pgrfa_id", id));
		List<Map<String, Object>> identifiers = select(sql2o, conn -> conn.createQuery("select * from identifiers where pgrfa_id=:pgrfa_id").addParameter("pgrfa_id", id));
		List<Map<String, Object>> names       = select(sql2o, conn -> conn.createQuery("select * from names       where pgrfa_id=:pgrfa_id").addParameter("pgrfa_id", id));
		List<Map<String, Object>> progdois    = select(sql2o, conn -> conn.createQuery("select * from progdois    where pgrfa_id=:pgrfa_id").addParameter("pgrfa_id", id));
		List<Map<String, Object>> targets     = select(sql2o, conn -> conn.createQuery("select * from targets     where pgrfa_id=:pgrfa_id").addParameter("pgrfa_id", id));
		List<Map<String, Object>> tkws        = select(sql2o, conn -> conn.createQuery("select k.* from tkws k, targets t where t.pgrfa_id=:pgrfa_id and t.id=k.target_id").addParameter("pgrfa_id", id));

		XMLBuilder builder = XMLBuilder.create("root");
		addMap(builder.element("conf"), conf);
		addList(builder, "pgrfa",      pgrfa);
		addList(builder, "actor",      actors);
		addList(builder, "identifier", identifiers);
		addList(builder, "name",       names);
		addList(builder, "progdoi",    progdois);
		addList(builder, "target",     targets);
		addList(builder, "tkw",       tkws);
		return builder.getDocument();
	}

	private static Document transformDocument(Document doc) {
		Document requestXslt = FluentXml.parse(Main.class.getClassLoader().getResourceAsStream("transform.xsl")).document();
		return FluentXml.transform(doc)
				.withStylesheet(requestXslt)
				.toDocument();
	}

    // more readable than sql2o.withConnection...
    private static List<Map<String, Object>> select(Sql2o sql2o, Function<Connection, Query> query) {
        try (Connection conn = sql2o.open()) {
            return query.apply(conn).executeAndFetchTable().asList();
        }
    }

	private static void addList(XMLBuilder builder, String name, List<Map<String, Object>> list) {
		list.forEach(map -> addMap(builder.element(name), map));
	}

	private static void addMap(XMLBuilder builder, Map<String, Object> map) {
		map.entrySet().stream()
				.filter(entry -> entry.getValue() != null)
				.forEach(entry -> builder.element(entry.getKey()).text(entry.getValue().toString()));
	}
}
