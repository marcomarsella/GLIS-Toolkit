package glis.toolkit;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.json.JSONObject;
import org.json.XML;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.Sql2o;

import java.io.File;
import java.util.*;
import java.util.function.Function;

public class Main {

    public static final String CONFIG_PATH = "config.txt";

    public static void main(String[] args) {
        try {
            // read configuration
            PropertiesConfiguration config   = new Configurations().properties(new File(CONFIG_PATH));
            String                  url      = config.getString("db.url");
            String                  username = config.getString("db.username");
            String                  password = config.getString("db.password");
            Integer                 qlimit   = Integer.parseInt(config.getString("db.query_limit"));
            String                  glisUrl  = config.getString("glis.url");
            String             glisUsername  = config.getString("glis.username");
            String             glisPassword  = config.getString("glis.password");
            Sql2o                   sql2o    = new Sql2o(url, username, password);

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
			process(sql2o, "register", qlimit);
			process(sql2o, "update",   qlimit);
			
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
	private static void process(Sql2o sql2o, String operation, Integer qlimit) {

		// build list of pgrfas to process according to operation
		List<Map<String, Object>> pgrfas = select(sql2o, conn ->
				conn.createQuery("select * from pgrfas where operation=:operation and processed=:processed limit :qlimit")
						.addParameter("operation", operation)
						.addParameter("processed", "n")
						.addParameter("qlimit", qlimit));

		// process each pgrfa
		for (Map<String, Object> pgrfa : pgrfas) {

			String pgrfaId = pgrfa.get("id").toString();

			// get related tables
			List<Map<String, Object>> actors = select(sql2o,      conn -> conn.createQuery("select * from actors      where pgrfa_id=:pgrfa_id").addParameter("pgrfa_id", pgrfaId));
			List<Map<String, Object>> identifiers = select(sql2o, conn -> conn.createQuery("select * from identifiers where pgrfa_id=:pgrfa_id").addParameter("pgrfa_id", pgrfaId));
			List<Map<String, Object>> names = select(sql2o,       conn -> conn.createQuery("select * from names       where pgrfa_id=:pgrfa_id").addParameter("pgrfa_id", pgrfaId));
			List<Map<String, Object>> progdois = select(sql2o,    conn -> conn.createQuery("select * from progdois    where pgrfa_id=:pgrfa_id").addParameter("pgrfa_id", pgrfaId));
			List<Map<String, Object>> targets = select(sql2o,     conn -> conn.createQuery("select * from targets     where pgrfa_id=:pgrfa_id").addParameter("pgrfa_id", pgrfaId));
			List<Map<String, Object>> tkws    = select(sql2o,     conn -> conn.createQuery("select k.* from tkws k, targets t where t.pgrfa_id=:pgrfa_id and t.id=k.target_id").addParameter("pgrfa_id", pgrfaId));
			Map<String, Object> data = new TreeMap<>();
			data.put("pgrfas", pgrfas);
			data.put("actors", actors);
			data.put("identifiers", identifiers);
			data.put("names", names);
			data.put("progdois", progdois);
			data.put("targets", targets);
			data.put("tkws", tkws);

			/*
			XStream xstream = new XStream();
			xstream.registerConverter(new MapEntryConverter(mapper));
			xstream.alias("root", TreeMap.class);
			var xml = xstream.toXML(data);
			*/

			JSONObject json = new JSONObject(data);
			String     xml = XML.toString(json);

			System.out.println(xml);
		}
	}


    // more readable than sql2o.withConnection...
    private static List<Map<String, Object>> select(Sql2o sql2o, Function<Connection, Query> query) {
        try (Connection conn = sql2o.open()) {
            return query.apply(conn).executeAndFetchTable().asList();
        }
    }
}