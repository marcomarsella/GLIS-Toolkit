package glis.toolkit;

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
            var config = new Configurations().properties(new File(CONFIG_PATH));
            var url = config.getString("db.url");
            var username = config.getString("db.username");
            var password = config.getString("db.password");
            var sql2o = new Sql2o(url, username, password);

            // build list of pgrfas to register
            var pgrfas = select(sql2o, conn ->
                    conn.createQuery("select * from pgrfas where operation=:operation and processed=:processed")
                            .addParameter("operation", "register")
                            .addParameter("processed", "n"));

            // register each pgrfa
            for (var pgrfa : pgrfas) {

                var sampleId = pgrfa.get("sample_id").toString();

                // get related tables
                var actors = select(sql2o,      conn -> conn.createQuery("select * from actors      where sample_id=:sample_id").addParameter("sample_id", sampleId));
                var identifiers = select(sql2o, conn -> conn.createQuery("select * from identifiers where sample_id=:sample_id").addParameter("sample_id", sampleId));
                var names = select(sql2o,       conn -> conn.createQuery("select * from names       where sample_id=:sample_id").addParameter("sample_id", sampleId));
                var progdois = select(sql2o,    conn -> conn.createQuery("select * from progdois    where sample_id=:sample_id").addParameter("sample_id", sampleId));
                var targets = select(sql2o,     conn -> conn.createQuery("select * from targets     where sample_id=:sample_id").addParameter("sample_id", sampleId));
                var data = new TreeMap<String, Object>();
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

                var json = new JSONObject(data);
                //var json = new Gson().toJsonTree(data);
                var xml = XML.toString(json, "root");

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