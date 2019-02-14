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

    public static final String CONFIG_PATH = "config.properties";

    public static void main(String[] args) {
        try {
            // read configuration
            PropertiesConfiguration config   = new Configurations().properties(new File(CONFIG_PATH));
            String                  url      = config.getString("db.url");
            String                  username = config.getString("db.username");
            String                  password = config.getString("db.password");
            Sql2o                   sql2o    = new Sql2o(url, username, password);

            // build list of pgrfas to register
            List<Map<String, Object>> pgrfas = select(sql2o, conn ->
                    conn.createQuery("select * from pgrfas where operation=:operation and processed=:processed")
                            .addParameter("operation", "register")
                            .addParameter("processed", "n"));

            // register each pgrfa
            for (Map<String, Object> pgrfa : pgrfas) {

                String sampleId = pgrfa.get("sample_id").toString();

                // get related tables
                List<Map<String, Object>> actors = select(sql2o,      conn -> conn.createQuery("select * from actors      where sample_id=:sample_id").addParameter("sample_id", sampleId));
                List<Map<String, Object>> identifiers = select(sql2o, conn -> conn.createQuery("select * from identifiers where sample_id=:sample_id").addParameter("sample_id", sampleId));
                List<Map<String, Object>> names = select(sql2o,       conn -> conn.createQuery("select * from names       where sample_id=:sample_id").addParameter("sample_id", sampleId));
                List<Map<String, Object>> progdois = select(sql2o,    conn -> conn.createQuery("select * from progdois    where sample_id=:sample_id").addParameter("sample_id", sampleId));
                List<Map<String, Object>> targets = select(sql2o,     conn -> conn.createQuery("select * from targets     where sample_id=:sample_id").addParameter("sample_id", sampleId));
                Map<String, Object> data = new TreeMap<>();
                data.put("pgrfas", pgrfas);
                data.put("actors", actors);
                data.put("identifiers", identifiers);
                data.put("names", names);
                data.put("progdois", progdois);
                data.put("targets", targets);

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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // more readable than sql2o.withConnection...
    private static List<Map<String, Object>> select(Sql2o sql2o, Function<Connection, Query> query) {
        try (Connection conn = sql2o.open()) {
            return query.apply(conn).executeAndFetchTable().asList();
        }
    }
}