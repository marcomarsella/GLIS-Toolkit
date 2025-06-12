package glis.toolkit;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.joox.Match;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.Sql2o;
import org.w3c.dom.Document;

import javax.net.ssl.SSLContext;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.X509Certificate;
import java.sql.Driver;
import java.sql.DriverManager;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.joox.JOOX.$;

public class Toolkit {

    static final String CONFIG_PATH = "config.txt";
    static final String TIMESTAMP = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());

    static final List<String> tableListV1 = Arrays.asList("actors","identifiers","names","progdois","tkws","targets","pgrfas");   //Order used to truncate tables
    static final List<String> tableListV2 = Arrays.asList("actors","identifiers","names","targets","pgrfas");   //Order used to truncate tables

    /*
     * Main method, sets up the environment and invokes registration and update functions
     */
    public static void main(String[] args) {

        File fLock = new File("lock.lck");
        Boolean doiLog;
        Writer fDOI = null;
        try {
            // file-based locking mechanism to ensure that only one instance is running at any given time
            if (!fLock.exists()) {
                fLock.createNewFile();
            } else {
                System.err.println("ERROR: Lock file 'lock.lck' exists. Either another instance of the Toolkit is running or it has terminated in an error. Please make sure no other instance of the Toolkit is running, or fix the error, before removing the lock file and trying again");
                System.exit(1);
            }
            // read configuration
            PropertiesConfiguration config = new Configurations().properties(new File(CONFIG_PATH));

            String temp = config.getString("doi.log");
            if (temp == null) {
                doiLog = false;
            } else {
                doiLog = temp.toLowerCase().equals("y");
            }

            int argsLen = args.length;
            if (argsLen == 0 ) {    //No arguments, print help
                help();
            } else {
                String command = args[0];
                switch (command) {
                    case "process":
                        if (doiLog) {   // If DOI log is requested, create writer and write header. Otherwise fDOI stays NULL
                            String fDOIName = TIMESTAMP + "_" + "DOI.txt";
                            fDOI = new FileWriter(fDOIName);
                            fDOI.write("WIEWS\tPID\tGenus\tSampleID\tDOI\n");    // Write header to DOI log
                        }
                        new Toolkit(config, doiLog).process(fDOI);
                        break;
                    case "load":
                        new Toolkit(config, doiLog).load(args);
                        break;
                    case "zapdb":
                        new Toolkit(config, doiLog).zap(args);
                        break;
                    default:
                        System.err.println("ERROR: Unknown command: " + command + ". Type 'java -jar toolkit.jar' for help.");
                        break;
                }
            }

            // Remove lock file
            fLock.delete();
            System.out.println("Processing complete.");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fDOI != null) {
                try {
                    fDOI.close();
                } catch (IOException e) {
                    // This is unrecoverable. Just report it and move on
                    e.printStackTrace();
                }
            }
        }
    }


    private static void help() {
        final String[] help = {
            "\nThis is the GLIS Integration Toolkit 4.0\n",
            "The following commands are accepted:",
            "java -jar toolkit.jar process\n     Processes registrations and updates\n",
            "java -jar toolkit.jar zapdb\n     Empties the Toolkit DB\n",
            "java -jar toolkit.jar load <table> <file>\n     Loads table <table> from the TAB-separated file <file>\n",
            "java -jar toolkit.jar\n     Prints this help\n"
        };
        Arrays.stream(help).forEach(System.out::println);
    }


    String glisUrl;
    String glisUsername;
    String glisPassword;
    String dbUrl;
    String dbSchema;
    String dbSchemaConf;
    String dbUsername;
    String dbPassword;
    String dbVersion;
    String consType;
    Integer qlimit;
    Sql2o sql2o;

    Toolkit(PropertiesConfiguration config, boolean doiLog) throws Exception {
        glisUrl = config.getString("glis.url").replaceAll("\\s+","").toLowerCase();   //Remove any whitespace and convert to lowercase
        qlimit = Integer.parseInt(config.getString("db.query_limit"));
        glisUsername = config.getString("glis.username");
        glisPassword = config.getString("glis.password");

        dbUrl     = config.getString("db.url");
        dbUsername = config.getString("db.username");
        dbPassword = config.getString("db.password");

        // Get DB schema name and set to string if not defined
        dbSchemaConf = config.getString("db.schema");
        if (dbSchemaConf == null) {
            dbSchema = "";
        } else if (!dbSchemaConf.isEmpty()) {
            dbSchema = dbSchemaConf + ".";
        }

        //Get DB version and sets to 1 if not specified
        dbVersion = config.getString("db.version");
        dbVersion = ((dbVersion != null) && (!dbVersion.isEmpty())) ? dbVersion : "1";

        //Set consType from URL
        int pos = glisUrl.indexOf("constype=");
        if (pos > 0) { //glisURL contains constype
            String temp = glisUrl.substring(pos);
            consType = temp.substring(9);   //Get whatever is after the equal sign
        } else {
            consType = "ex";    //constype not provided, defaults to "ex"
        }
        //Print configuration to both the console and to errors.txt
        String configuration = "Configuration\n" +
                "Database URL:      [" + dbUrl + "]\n" +
                "Database schema:   [" + dbSchemaConf + "]\n" +
                "Database username: [" + dbUsername + "]\n" +
                "Database password: [" + dbPassword + "]\n" +
                "Database version:  [" + dbVersion + "]\n" +
                "Query limit:       [" + qlimit + "]\n" +
                "GLIS URL:          [" + glisUrl + "]\n" +
                "GLIS username:     [" + glisUsername + "]\n" +
                "GLIS password:     [" + glisPassword + "]\n" +
                "constype:          [" + consType + "]\n" +
                "Write DOI log:     [" + (doiLog ? "Yes" : "No") + "]\n";
        System.err.println(configuration);

        System.err.println("Toolkit version: 4.0.1");

        if (!dbUrl.contains(":hsqldb:")) {  //If we are not using the embedded database
            loadDriver();
        }
        sql2o = new Sql2o(dbUrl, dbUsername, dbPassword);
    }

    void process(Writer fDOI) throws Exception {
        // Register first then update
        process("register", fDOI);
        process("update", fDOI);
    }

    /*
     * Finds pgrfas rows to process and processes them according to the operation requested
     */
    void process(String operation, Writer fDOI) throws Exception {

        // Build list of pgrfa ids to register. Order by id to ensure a logical sequence
        List<String> ids = select(conn ->
                conn.createQuery("select id from " + dbSchema + "pgrfas where operation=:operation and processed=:processed order by id")
                        .addParameter("operation", operation)
                        .addParameter("processed", "n"))
                .stream()
                .limit(qlimit)
                .map(m -> m.get("id").toString())
                .collect(Collectors.toList());

        // Check if any row has been found with processed = 'n'
        Integer idNum = ids.size();
        if (idNum == 0) {
            System.out.println("No pgrfas rows found to " + operation);
            return;
        } else {
            System.out.println(ids.size() + " pgrfas rows found to " + operation);
        }

        // Build the config element to be added to the XML
        Map<String, Object> conf = new HashMap<>();
        conf.put("glis_username", glisUsername);
        conf.put("glis_password", glisPassword);

        //Build custom HTTP Client to avoid errors with AWS cookies
        RequestConfig requestConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build();
        HttpClient customHttpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .setSSLHostnameVerifier(new NoopHostnameVerifier())
                .build();
        Unirest.setHttpClient(customHttpClient);

/*
        //DISABLED
        // For GLIS test server, disable SSL checking if the Java version is not recent enough
        if (glisUrl.contains("glistest")) {
            String version = Runtime.class.getPackage().getImplementationVersion();
            System.err.println("Java version: " + version);
            String[] vArr = version.split("_");
            if (Integer.parseInt(vArr[1]) < 101) {
                System.err.println("Creating custom HTTP client for GLIS test server");
                SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustSelfSignedStrategy() {
                    public boolean isTrusted(X509Certificate[] chain, String authType) {
                        return true;
                    }
                }).build();
                HttpClient customHttpClient = HttpClients.custom().setSSLContext(sslContext)
                        .setSSLHostnameVerifier(new NoopHostnameVerifier()).build();
                Unirest.setHttpClient(customHttpClient);
                System.out.println("Custom HTTP Client created ");
            }
        }
*/

        // Process each pgrfa in the list obtained by the query
        Integer count = 0;
        for (String id : ids) {

            // Extract main pgrfa record
            Map<String, Object> pgrfa = select(conn -> conn.createQuery("select * from pgrfas where id=:id").addParameter("id", new BigInteger(id))).get(0);

            // Save values to be used in response object
            Object temp = pgrfa.get("hold_wiews");
            String wiews = null;
            if (temp != null) wiews = temp.toString();
            String pid = pgrfa.get("hold_pid").toString();  //This one is never null

            // Build the XML document according to the DB version
            Document doc = null;
            if (dbVersion.equals("1")) {
                doc = buildDocumentV1(id, pgrfa, conf);
            } else {
                String sample_id = pgrfa.get("sample_id").toString();
                doc = buildDocumentV2(sample_id, pgrfa, conf);
            }

            // Transform XML using the XSL stylesheet and then transform again removing all empty elements
            String xslName = "transform-"+consType+".xsl";  //Build XSL template name from constype
            Document request = transform(doc, xslName);
            request = transform(request, "prune.xsl");
            String xmlRequest = $(request).toString();

            // Attempts the HTTPS POST transaction to GLIS and obtains result
            try {
                HttpResponse<String> httpResponse = Unirest.post(glisUrl)
                        .header("Accept", "application/xml")
                        .header("User-Agent", "GLISTK/4.0 GLIS-Toolkit version 4.0")
                        .body(xmlRequest)
                        .asString();
                String xmlResponse = httpResponse.getBody();
                //DEBUG System.err.println(xmlResponse);
                Document response = $(xmlResponse).document();

                // Extract relevant information, if available
                String doi      = $(response).child("doi").text();
                String sampleId = (pgrfa.get("sample_id") == null)? "N/A" : pgrfa.get("sample_id").toString();
                String genus    = (pgrfa.get("genus") == null)? "N/A" : pgrfa.get("genus").toString();
                String error    = $(response).child("error").text();

                // Set the result of the transaction
                String result = ((error != null) && (!error.isEmpty())) ? "KO" : "OK";

                // If there was a connection error, abort
                if (httpResponse.getStatus() == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
                    String name     = $(response).child("name").text();
                    String message  = $(response).child("message").text();
                    System.out.println("Error in GLIS transaction: SampleID " + sampleId + " - " + genus + " - " + name + " - " + message);
                    Unirest.shutdown();
                    if (fDOI != null) fDOI.close();
                    System.exit(1);
                }

                // Write to DOI log if it has been created, if successful and operation is "register"
                if ((fDOI != null) && Objects.equals(operation, "register") && Objects.equals(result, "OK")) {
                    fDOI.write((wiews != null ? wiews : "") + "\t" + pid + "\t" + genus + "\t" + sampleId + "\t" + doi + "\n");
                }

                // Write result to DB and mark pgrfas row as processed
                insertResult(operation, result, doi, sampleId, genus, error);
                markAsProcessed(id);

                // Provide some feedback on the operation
                count++;
                System.out.println("Processed pgrfa row: " + count.toString() + "/" + idNum + " [id: " + id + "] [sample_id: " + sampleId + "]: " + result);
            }
            // Catch any exception and log it to the console and to the errors file
            catch (com.mashape.unirest.http.exceptions.UnirestException e) {
                e.printStackTrace();
                Unirest.shutdown();
                if (fDOI != null) fDOI.close();
                System.exit(1);
            }
        }
        Unirest.shutdown();
    }

    /*
     * Builds the XML document by extracting the various rows related to the passed pgrfa
     * Works on v1 database schema
     */
    Document buildDocumentV1(String id, Map<String, Object> pgrfa, Map<String, Object> conf) throws ParserConfigurationException {

        // Get related rowsets
        List<Map<String, Object>> actors = select(conn -> conn.createQuery("select * from " + dbSchema + "actors where pgrfa_id=:pgrfa_id").addParameter("pgrfa_id", id));
        List<Map<String, Object>> identifiers = select(conn -> conn.createQuery("select * from " + dbSchema + "identifiers where pgrfa_id=:pgrfa_id").addParameter("pgrfa_id", id));
        List<Map<String, Object>> names = select(conn -> conn.createQuery("select * from " + dbSchema + "names where pgrfa_id=:pgrfa_id").addParameter("pgrfa_id", id));
        List<Map<String, Object>> progdois = select(conn -> conn.createQuery("select * from " + dbSchema + "progdois where pgrfa_id=:pgrfa_id").addParameter("pgrfa_id", id));
        List<Map<String, Object>> targets = select(conn -> conn.createQuery("select * from " + dbSchema + "targets      where pgrfa_id=:pgrfa_id").addParameter("pgrfa_id", id));
        List<Map<String, Object>> tkws = select(conn -> conn.createQuery("select k.* from " + dbSchema + "tkws k, " + dbSchema + "targets t where t.pgrfa_id=:pgrfa_id and t.id=k.target_id").addParameter("pgrfa_id", id));

        return buildDocument(pgrfa, conf, actors, identifiers, names, progdois, targets, tkws);
    }

    /*
     * Builds the XML document by extracting the various rows related to the passed pgrfa
     * Works on v2 database schema
     */
    Document buildDocumentV2(String sid, Map<String, Object> pgrfa, Map<String, Object> conf) throws ParserConfigurationException {

        // Get related rowsets
        List<Map<String, Object>> actors = select(conn -> conn.createQuery("select * from " + dbSchema + "actors where sample_id=:sid").addParameter("sid", sid));
        List<Map<String, Object>> identifiers = select(conn -> conn.createQuery("select * from " + dbSchema + "identifiers where sample_id=:sid").addParameter("sid", sid));
        List<Map<String, Object>> names = select(conn -> conn.createQuery("select * from " + dbSchema + "names where sample_id=:pgrfa_id").addParameter("pgrfa_id", sid));
        List<Map<String, Object>> targets = select(conn -> conn.createQuery("select * from " + dbSchema + "targets where sample_id=:sid").addParameter("sid", sid));

        //System.out.println("DEBUG: QUERIES DONE");

        List<Map<String, Object>> progdois = explodeProgDois(pgrfa.get("progdois"), sid);
        List<Map<String, Object>> tkws = explodeTkws(targets);
        return buildDocument(pgrfa, conf, actors, identifiers, names, progdois, targets, tkws);
    }

    private Document buildDocument(Map<String, Object> pgrfa,
                                   Map<String, Object> conf,
                                   List<Map<String, Object>> actors,
                                   List<Map<String, Object>> identifiers,
                                   List<Map<String, Object>> names,
                                   List<Map<String, Object>> progdois,
                                   List<Map<String, Object>> targets,
                                   List<Map<String, Object>> tkws) {
        Match result = $("root",
                addMap("conf", conf),
                addMap("pgrfa", pgrfa));
        addList(result, "actor", actors);
        addList(result, "identifier", identifiers);
        addList(result, "name", names);
        addList(result, "progdoi", progdois);
        addList(result, "target", targets);
        addList(result, "tkw", tkws);
        return result.document();
    }

    /*
     * Transforms the XML document using the passed XSLT stylesheet
     */
    Document transform(Document doc, String xslPath) {
        return $(doc)
                .transform(Toolkit.class.getClassLoader().getResourceAsStream(xslPath))
                .document();
    }

    /*
     * Wrapper for the update operation to the pgrfas table
     */
    void markAsProcessed(String id) {
        final String query = "update " + dbSchema + "pgrfas set processed = 'y' where id=:id";
        try (Connection conn = sql2o.open()) {
            conn.createQuery(query)
                    .addParameter("id", new BigInteger(id))
                    .executeUpdate();
        }
    }

    /*
     * Wrapper for the insert operation into the results table
     */
    void insertResult(String operation, String result, String doi, String sampleId, String genus, String error) {
        final String query = "insert into " + dbSchema + "results (operation, genus, sample_id, doi, result, error) values(:ope, :gen, :sid, :doi, :res, :err)";
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

    /*
     * Wrapper for the select operation
     */
    List<Map<String, Object>> select(Function<Connection, Query> query) {
        try (Connection conn = sql2o.open()) {
            return query.apply(conn).executeAndFetchTable().asList();
        }
    }

    /*
     * Wrapper to add a list of maps to the XML document
     */
    Match addList(Match m, String label, List<Map<String, Object>> list) {
        list.forEach(map -> m.append(addMap(label, map)));
        return m;
    }

    /*
     * Wrapper to add a map to the XML document
     */
    Match addMap(String label, Map<String, Object> map) {
        Match m = $(label);
        map.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .forEach(entry -> m.append($(entry.getKey()).text(entry.getValue().toString())));
        return m;
    }

    /*
     * Process progenitor DOIs list by splitting the field into a new list of maps
     */
    List<Map<String, Object>> explodeProgDois(Object listObj, String sid) {
        List<Map<String, Object>> subList = new ArrayList<Map<String, Object>>();
        if (listObj != null) {
            String list = listObj.toString();
            if (!list.isEmpty()) {
                String[] temp = list.split("\\s*\\|\\s*"); // '|' is the separator
                for (String itm : temp) {
                    Map<String, Object> map = new HashMap<String, Object>();
                    map.put("sample_id", sid);
                    map.put("doi", itm.trim());
                    subList.add(map);
                }
            }
        }
        return subList;
    }

    /*
     * Process target keywords by splitting the target.tkws field into a new list of maps
     */
    List<Map<String, Object>> explodeTkws(List<Map<String, Object>> targets) {
        List<Map<String, Object>> tkws = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> target : targets) {
            String tid = target.get("id") + "";
            Object tkwObj = target.get("tkws");
            if (tkwObj != null) {
                String tkwString = tkwObj.toString();
                if ((tkwString != null) && (!tkwString.isEmpty())) {
                    String[] temp = tkwString.split("\\s*\\|\\s*"); // '|' is the separator
                    for (String tkw : temp) {
                        Map<String, Object> tkmap = new HashMap<String, Object>();
                        tkmap.put("target_id", tid);
                        tkmap.put("value", tkw.trim());
                        tkws.add(tkmap);
                    }
                }
            }
        }
        return tkws;
    }

    void loadDriver() throws Exception {
        File jarDir = new File(Toolkit.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();
        if (jarDir.toString().endsWith("target")) {	//To handle maven builds into target directory
            jarDir = jarDir.getParentFile();
        }
        File jdbcDir = new File(jarDir, "jdbcDriver");
        URLClassLoader loader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        method.setAccessible(true);
        try (Stream<Path> filePathStream= Files.walk(jdbcDir.toPath())) {
            filePathStream.forEach(filePath -> {
                if (Files.isRegularFile(filePath) && filePath.toString().endsWith(".jar")) {	//Try to load only .jar files
                    try {method.invoke(loader, filePath.toFile().toURI().toURL());} catch (Exception e){};
                }
            });
        }
        System.err.println("Drivers:");
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver d = drivers.nextElement();
            System.err.println("- " + d);
        }
    }


    private void zap(String[] args) throws Exception {
        try (Connection conn = sql2o.open()) {
            switch (dbVersion) {
                case "1":
                    for (String table : tableListV1) {
                        conn.createQuery("DELETE FROM " + dbSchema + table.toLowerCase()).executeUpdate();
                    }
                    conn.createQuery("DELETE FROM " + dbSchema + "results").executeUpdate();
                    break;
                case "2":
                    for (String table : tableListV2) {
                        conn.createQuery("TRUNCATE TABLE " + dbSchema + table.toLowerCase()).executeUpdate();
                    }
                    conn.createQuery("TRUNCATE TABLE " + dbSchema + "results").executeUpdate();
                    break;
                default:
                    System.err.println("ERROR: Unknown database version: " + dbVersion);
                    break;
            }
        }
        System.out.println("Database emptied");
    }


    private void load(String[] args) throws Exception {
        if (args.length != 3) {
            System.err.println("Usage: java -jar toolkit.jar load <table name> <file name>");
            System.exit(1);
        }
        String table = args[1].toLowerCase();
        String fileName = args[2];

        if ((dbVersion.equals("1") && !tableListV1.contains(table)) || (dbVersion.equals("2") && !tableListV2.contains(table))) {
            System.err.println("ERROR: Version " + dbVersion + " database does not include table " + table);
            return;
        }

        switch (dbVersion) {
            case "1":
                loadV1(table, fileName);
                break;
            case "2":
                loadV2(table, fileName);
                break;
            default:
                System.err.println("ERROR: Unknown database version: " + dbVersion);
                break;
        }
    }

    private void loadV1(String table, String fileName) throws Exception {
        //DB version 1 tables structure
        //DB version 1 tables structure
        final String[] actorsV1Cols = {"ID", "PGRFA_ID", "ROLE", "WIEWS", "PID", "NAME", "ADDRESS", "COUNTRY"};
        final String[] identifiersV1Cols = {"ID", "PGRFA_ID", "TYPE", "VALUE"};
        final String[] namesV1Cols = {"ID", "PGRFA_ID", "NAME_TYPE", "NAME"};
        final String[] pgrfasV1Cols = {
                "ID", "OPERATION", "SAMPLE_ID", "PROCESSED", "SAMPLE_DOI", "DATE", "HOLD_WIEWS", "HOLD_PID", "HOLD_NAME", "HOLD_ADDRESS", "HOLD_COUNTRY", "METHOD", "GENUS", "SPECIES",
                "SP_AUTH", "SUBTAXA", "ST_AUTH", "BIO_STATUS", "MLS_STATUS", "HISTORICAL", "PROV_SID", "PROVENANCE", "COLL_SID", "COLL_MISS_ID", "COLL_SITE", "COLL_LAT",
                "COLL_LON", "COLL_UNCERT", "COLL_DATUM", "COLL_GEOREF", "COLL_ELEVATION", "COLL_DATE", "COLL_SOURCE", "ANCESTRY"
        };
        final String[] progdoisV1Cols = {"ID", "PGRFA_ID", "DOI"};
        final String[] targetsV1Cols = {"ID", "PGRFA_ID", "VALUE"};
        final String[] tkwsV1Cols = {"ID", "TARGET_ID", "VALUE"};

// DB queries
        final String queryActorsV1 = "insert into " + dbSchema + "actors (ID,PGRFA_ID,ROLE,WIEWS,PID,NAME,ADDRESS,COUNTRY) values(:id,:pgid,:role,:wiews,:pid,:name,:addr,:ctry)";
        final String queryIdentifiersV1 = "insert into " + dbSchema + "identifiers (ID,PGRFA_ID,TYPE,VALUE) values(:id,:pgid,:type,:val)";
        final String queryNamesV1 = "insert into " + dbSchema + "names (ID,PGRFA_ID,NAME_TYPE,NAME) values(:id,:pgid,:type,:name)";
        final String queryPgrfasV1 = "insert into " + dbSchema +
                "pgrfas (ID,OPERATION,SAMPLE_ID,PROCESSED,SAMPLE_DOI,DATE,HOLD_WIEWS,HOLD_PID,HOLD_NAME,HOLD_ADDRESS,HOLD_COUNTRY,METHOD,GENUS,SPECIES,SP_AUTH,SUBTAXA,ST_AUTH,BIO_STATUS,MLS_STATUS,HISTORICAL,PROV_SID,PROVENANCE,COLL_SID,COLL_MISS_ID,COLL_SITE,COLL_LAT,COLL_LON,COLL_UNCERT,COLL_DATUM,COLL_GEOREF,COLL_ELEVATION,COLL_DATE,COLL_SOURCE,ANCESTRY) " +
                "values(:id,:oper,:pgid,:proc,:sdoi,:date,:hwiews,:hpid,:hname,:hadd,:hcty,:meth,:gen,:spec,:spau,:stax,:stau,:bio,:mls,:hist,:psid,:prov,:csid,:cmid,:csit,:clat,:clon,:cunc,:cdum,:cgrf,:cele,:cdat,:csrc,:ance)";
        final String queryProgdoisV1 = "insert into " + dbSchema + "progdois (ID,PGRFA_ID,DOI) values(:id,:pgid,:doi)";
        final String queryTargetsV1 = "insert into " + dbSchema + "targets (ID,PGRFA_ID,VALUE) values(:id,:pgid,:val)";
        final String queryTkwsV1 = "insert into " + dbSchema + "tkws (ID,TARGET_ID,VALUE) values(:id,:tgid,:val)";

        try (Connection conn = sql2o.open()) {
            try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
                String dummy = "";
                boolean header = true;
                int cnt = 0;
                for (String line; (line = br.readLine()) != null; ) {
                    if (header) {
                        String[] head = line.split("\t");
                        boolean ok = false;
                        switch (table) {
                            case "actors":
                                ok = Arrays.equals(head, actorsV1Cols);
                                break;
                            case "identifiers":
                                ok = Arrays.equals(head, identifiersV1Cols);
                                break;
                            case "names":
                                ok = Arrays.equals(head, namesV1Cols);
                                break;
                            case "pgrfas":
                                ok = Arrays.equals(head, pgrfasV1Cols);
                                break;
                            case "progdois":
                                ok = Arrays.equals(head, progdoisV1Cols);
                                break;
                            case "targets":
                                ok = Arrays.equals(head, targetsV1Cols);
                                break;
                            case "tkws":
                                ok = Arrays.equals(head, tkwsV1Cols);
                                break;
                        }
                        if (!ok) {
                            System.err.println("ERROR: Header is not correct for table " + table);
                            return;
                        }
                        header = false;
                        continue;
                    }
                    String[] arr = line.split("\t", -1);
                    Arrays.parallelSetAll(arr, (i) -> arr[i].trim());   //Trim all items
                    if (arr[0].trim().length() > 0) {   //Only when operation is defined
                        cnt++;
                        // DEBUG System.err.println("Processing line " + cnt + " array size: " + arr.length);
                        switch (table) {
                            case "actors":
                                conn.createQuery(queryActorsV1)
                                        .addParameter("id", arr[0])
                                        .addParameter("pgid", arr[1])
                                        .addParameter("role", arr[2])
                                        .addParameter("wiews", arr[3])
                                        .addParameter("pid", arr[4])
                                        .addParameter("name", arr[5])
                                        .addParameter("addr", arr[6])
                                        .addParameter("ctry", arr[7])
                                        .executeUpdate();
                                break;
                            case "identifiers":
                                conn.createQuery(queryIdentifiersV1)
                                        .addParameter("id", arr[0])
                                        .addParameter("pgid", arr[1])
                                        .addParameter("type", arr[2])
                                        .addParameter("val", arr[3])
                                        .executeUpdate();
                                break;
                            case "names":
                                conn.createQuery(queryNamesV1)
                                        .addParameter("id", arr[0])
                                        .addParameter("pgid", arr[1])
                                        .addParameter("type", arr[2])
                                        .addParameter("name", arr[3])
                                        .executeUpdate();
                                break;
                            case "pgrfas":
                                conn.createQuery(queryPgrfasV1)
                                        .addParameter("id", arr[0])
                                        .addParameter("oper", arr[1])
                                        .addParameter("sid", arr[2])
                                        .addParameter("proc", arr[3])
                                        .addParameter("sdoi", arr[4])
                                        .addParameter("date", arr[5])
                                        .addParameter("hwiews", arr[6])
                                        .addParameter("hpid", arr[7])
                                        .addParameter("hname", arr[8])
                                        .addParameter("hadd", arr[9])
                                        .addParameter("hcty", arr[10])
                                        .addParameter("meth", arr[11])
                                        .addParameter("gen", arr[12])
                                        .addParameter("spec", arr[13])
                                        .addParameter("spau", arr[14])
                                        .addParameter("stax", arr[15])
                                        .addParameter("stau", arr[16])
                                        .addParameter("bio", arr[17])
                                        .addParameter("mls", arr[18])
                                        .addParameter("hist", arr[19])
                                        .addParameter("psid", arr[20])
                                        .addParameter("prov", arr[21])
                                        .addParameter("csid", arr[22])
                                        .addParameter("cmid", arr[23])
                                        .addParameter("csit", arr[24])
                                        .addParameter("clat", arr[25])
                                        .addParameter("clon", arr[26])
                                        .addParameter("cunc", arr[27])
                                        .addParameter("cdum", arr[28])
                                        .addParameter("cgrf", arr[29])
                                        .addParameter("cele", arr[30].length() == 0 ? null : arr[30])
                                        .addParameter("cdat", arr[31])
                                        .addParameter("csrc", arr[32])
                                        .addParameter("ance", arr[33])
                                        .executeUpdate();
                                break;
                            case "progdois":
                                conn.createQuery(queryProgdoisV1)
                                        .addParameter("id", arr[0])
                                        .addParameter("pgid", arr[1])
                                        .addParameter("doi", arr[2])
                                        .executeUpdate();
                                break;
                            case "targets":
                                conn.createQuery(queryTargetsV1)
                                        .addParameter("id", arr[0])
                                        .addParameter("pgid", arr[1])
                                        .addParameter("val", arr[2])
                                        .executeUpdate();
                                break;
                            case "tkws":
                                conn.createQuery(queryTkwsV1)
                                        .addParameter("id", arr[0])
                                        .addParameter("tgid", arr[1])
                                        .addParameter("val", arr[2])
                                        .executeUpdate();
                                break;
                        }
                    }
                }
            }
        }
    }

    private void loadV2 (String table, String fileName) throws Exception {
        //DB version 2 tables structure
        final String[] actorsV2Cols = {"SAMPLE_ID","ROLE","WIEWS","PID","NAME","ADDRESS","COUNTRY"};
        final String[] identifiersV2Cols = {"SAMPLE_ID","TYPE","VALUE"};
        final String[] namesV2Cols = {"SAMPLE_ID","NAME_TYPE","NAME"};
        final String[] pgrfasV2Cols = {
                "OPERATION","SAMPLE_ID","PROCESSED","SAMPLE_DOI","DATE","HOLD_WIEWS","HOLD_PID","HOLD_NAME","HOLD_ADDRESS","HOLD_COUNTRY","METHOD","GENUS","SPECIES",
                "SP_AUTH","SUBTAXA","ST_AUTH","BIO_STATUS","MLS_STATUS","HISTORICAL","PROGDOIS","PROV_SID","PROVENANCE","COLL_SID","COLL_MISS_ID","COLL_SITE","COLL_LAT",
                "COLL_LON","COLL_UNCERT","COLL_DATUM","COLL_GEOREF","COLL_ELEVATION","COLL_DATE","COLL_SOURCE","ANCESTRY"
        };
        final String[] targetsV2Cols = {"SAMPLE_ID","VALUE","TKWS"};

// DB queries
        final String queryActorsV2      = "insert into " + dbSchema + "actors (SAMPLE_ID,ROLE,WIEWS,PID,NAME,ADDRESS,COUNTRY) values(:sid,:role,:wiews,:pid,:name,:addr,:ctry)";
        final String queryIdentifiersV2 = "insert into " + dbSchema + "identifiers (SAMPLE_ID,TYPE,VALUE) values(:sid,:type,:val)";
        final String queryNamesV2       = "insert into " + dbSchema + "names (SAMPLE_ID,NAME_TYPE,NAME) values(:sid,:type,:name)";
        final String queryPgrfasV2      = "insert into " + dbSchema +
                "pgrfas (OPERATION,SAMPLE_ID,PROCESSED,SAMPLE_DOI,DATE,HOLD_WIEWS,HOLD_PID,HOLD_NAME,HOLD_ADDRESS,HOLD_COUNTRY,METHOD,GENUS,SPECIES,SP_AUTH,SUBTAXA,ST_AUTH,BIO_STATUS,MLS_STATUS,HISTORICAL,PROGDOIS,PROV_SID,PROVENANCE,COLL_SID,COLL_MISS_ID,COLL_SITE,COLL_LAT,COLL_LON,COLL_UNCERT,COLL_DATUM,COLL_GEOREF,COLL_ELEVATION,COLL_DATE,COLL_SOURCE,ANCESTRY) " +
                "values(:oper,:sid,:proc,:sdoi,:date,:hwiews,:hpid,:hname,:hadd,:hcty,:meth,:gen,:spec,:spau,:stax,:stau,:bio,:mls,:hist,:pdoi,:psid,:prov,:csid,:cmid,:csit,:clat,:clon,:cunc,:cdum,:cgrf,:cele,:cdat,:csrc,:ance)";
        final String queryTargetsV2      = "insert into " + dbSchema + "targets (SAMPLE_ID,VALUE,TKWS) values(:sid,:val,:tkws)";

        try (Connection conn = sql2o.open()) {
            try(BufferedReader br = new BufferedReader(new FileReader(fileName))) {
                String dummy ="";
                boolean header = true;
                int cnt = 0;
                for (String line; (line = br.readLine()) != null; ) {
                    if (header) {
                        String[] head = line.split("\t");
                        boolean ok = false;
                        switch (table) {
                            case "actors":
                                ok = Arrays.equals(head, actorsV2Cols);
                                break;
                            case "identifiers":
                                ok = Arrays.equals(head, identifiersV2Cols);
                                break;
                            case "names":
                                ok = Arrays.equals(head, namesV2Cols);
                                break;
                            case "pgrfas":
                                ok = Arrays.equals(head, pgrfasV2Cols);
                                break;
                            case "targets":
                                ok = Arrays.equals(head, targetsV2Cols);
                                break;
                        }
                        if (!ok) {
                            System.err.println("ERROR: Header is not correct for table " + table);
                            return;
                        }
                        header = false;
                        continue;
                    }
                    String[] arr = line.split("\t", -1);
                    Arrays.parallelSetAll(arr, (i) -> arr[i].trim());   //Trim all items
                    if (arr[0].trim().length() > 0) {   //Only when operation is defined
                        cnt++;
                        // DEBUG System.err.println("Processing line " + cnt + " array size: " + arr.length);
                        switch (table) {
                            case "actors":
                                conn.createQuery(queryActorsV2)
                                        .addParameter("sid", arr[0])
                                        .addParameter("role", arr[1])
                                        .addParameter("wiews", arr[2])
                                        .addParameter("pid", arr[3])
                                        .addParameter("name", arr[4])
                                        .addParameter("addr", arr[5])
                                        .addParameter("ctry", arr[6])
                                        .executeUpdate();
                                break;
                            case "identifiers":
                                conn.createQuery(queryIdentifiersV2)
                                        .addParameter("sid", arr[0])
                                        .addParameter("type", arr[1])
                                        .addParameter("val", arr[2])
                                        .executeUpdate();
                                break;
                            case "names":
                                conn.createQuery(queryNamesV2)
                                        .addParameter("sid", arr[0])
                                        .addParameter("type", arr[1])
                                        .addParameter("name", arr[2])
                                        .executeUpdate();
                                break;
                            case "pgrfas":
                                conn.createQuery(queryPgrfasV2)
                                        .addParameter("oper", arr[0])
                                        .addParameter("sid", arr[1])
                                        .addParameter("proc", arr[2])
                                        .addParameter("sdoi", arr[3])
                                        .addParameter("date", arr[4])
                                        .addParameter("hwiews", arr[5])
                                        .addParameter("hpid", arr[6])
                                        .addParameter("hname", arr[7])
                                        .addParameter("hadd", arr[8])
                                        .addParameter("hcty", arr[9])
                                        .addParameter("meth", arr[10])
                                        .addParameter("gen", arr[11])
                                        .addParameter("spec", arr[12])
                                        .addParameter("spau", arr[13])
                                        .addParameter("stax", arr[14])
                                        .addParameter("stau", arr[15])
                                        .addParameter("bio", arr[16])
                                        .addParameter("mls", arr[17])
                                        .addParameter("hist", arr[18])
                                        .addParameter("pdoi", arr[19])
                                        .addParameter("psid", arr[20])
                                        .addParameter("prov", arr[21])
                                        .addParameter("csid", arr[22])
                                        .addParameter("cmid", arr[23])
                                        .addParameter("csit", arr[24])
                                        .addParameter("clat", arr[25])
                                        .addParameter("clon", arr[26])
                                        .addParameter("cunc", arr[27])
                                        .addParameter("cdum", arr[28])
                                        .addParameter("cgrf", arr[29])
                                        .addParameter("cele", arr[30].length() == 0 ? null : arr[30])
                                        .addParameter("cdat", arr[31])
                                        .addParameter("csrc", arr[32])
                                        .addParameter("ance", arr[33])
                                        .executeUpdate();
                                break;
                            case "targets":
                                conn.createQuery(queryTargetsV2)
                                        .addParameter("sid", arr[0])
                                        .addParameter("val", arr[1])
                                        .addParameter("tkws", arr[2])
                                        .executeUpdate();
                                break;
                        }
                    }
                }
            }
        }
    }
}
