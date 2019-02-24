package glis.toolkit;

import org.hsqldb.Server;
import org.hsqldb.persist.HsqlProperties;

import static org.hsqldb.Database.CLOSEMODE_NORMAL;

public class DB {

    public static void main(String[] args) {
        try {
            System.err.println("starting database");

            HsqlProperties p = new HsqlProperties();
            p.setProperty("server.database.0", "file:db/glis;user=glis;password=glis;hsqldb.default_table_type=cached");
            p.setProperty("server.dbname.0", "glis");
            Server server = new Server();
            server.setProperties(p);
            server.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.err.println("shutting down database");

                server.shutdownCatalogs(CLOSEMODE_NORMAL);
            }));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
