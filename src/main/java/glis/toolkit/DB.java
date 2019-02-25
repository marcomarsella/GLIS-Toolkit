package glis.toolkit;

import org.hsqldb.Server;
import org.hsqldb.persist.HsqlProperties;

import static org.hsqldb.Database.CLOSEMODE_NORMAL;

public class DB {

    public static void main(String[] args) {
        try {
            System.err.println("Starting embedded database");

            HsqlProperties p = new HsqlProperties();
            p.setProperty("server.database.0", "file:db/glistk;user=glistk;password=glistk;hsqldb.default_table_type=cached");
            p.setProperty("server.dbname.0", "glistk");
            Server server = new Server();
            server.setProperties(p);
            server.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.err.println("Stopping down database");

                server.shutdownCatalogs(CLOSEMODE_NORMAL);
            }));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
