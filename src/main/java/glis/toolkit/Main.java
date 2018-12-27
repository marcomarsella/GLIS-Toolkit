package glis.toolkit;

import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.sql2o.Connection;
import org.sql2o.Sql2o;

import java.io.File;

public class Main {

    public static final String CONFIG_PATH = "config.properties";

    public static void main(String[] args) {
        try {
            var config   = new Configurations().properties(new File(CONFIG_PATH));
            var url      = config.getString("db.url");
            var username = config.getString("db.username");
            var password = config.getString("db.password");
            var db       = new Sql2o(url, username, password);

            try (Connection conn = db.open()) {
                var pgrfas = conn.createQuery("select * from pgrfas where operation=:operation and processed=:processed")
                        .addParameter("operation", "register")
                        .addParameter("processed", "n")
                        .executeAndFetchTable().asList();

                for (var pgrfa: pgrfas) {
                    System.out.println(pgrfa);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
