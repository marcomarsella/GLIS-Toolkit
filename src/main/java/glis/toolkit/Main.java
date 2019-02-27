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
import java.math.BigInteger;
import java.io.*;
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
    private static String  dbVersion;
    private static Integer qlimit;
	private static Sql2o   sql2o;
	private static Boolean doiLog;
	private static Writer  fDOI;

	/*
	 * Main method, sets up the environment and invokes registration and update functions
	 */
    public static void main(String[] args) {

		try {
			File fLock = new File("lock.lck");
			// file-based locking mechanism to ensure that only one instance is running at any given time
			if (!fLock.exists()) {
				fLock.createNewFile();
			} else {
				System.err.println("Lock file 'lock.lck' exists. Either another instance of the Toolkit is running or it has terminated in an error. Please make sure no other instance of the Toolkit is running, or fix the error, before removing the lock file and trying again");
				System.exit(1);
			}
			// read configuration
			PropertiesConfiguration config = new Configurations().properties(new File(CONFIG_PATH));
			String url = config.getString("db.url");
			String username = config.getString("db.username");
			String password = config.getString("db.password");

			//Initialize HSQLDB driver if used in db.url
			if (url.contains(":hsqldb:")) Class.forName("org.hsqldb.jdbcDriver");

			//Initialize Postgresql driver if used in db.url
			if (url.contains(":postgresql:")) Class.forName("org.postgresql.Driver");

			sql2o = new Sql2o(url, username, password);
			glisUrl = config.getString("glis.url");
			qlimit = Integer.parseInt(config.getString("db.query_limit"));
			glisUsername = config.getString("glis.username");
            glisPassword = config.getString("glis.password");
            String temp = config.getString("doi.log");
            if (temp == null) {
            	doiLog = false;
			} else {
				doiLog = temp.toLowerCase().equals("y");
			}

            // If DOI log is requested, create writer and write header. Otherwise fDOI stays NULL
            if (doiLog) {
				String fDOIName = TIMESTAMP + "_" + "DOI.txt";
				fDOI = new FileWriter(fDOIName);
				fDOI.write("WIEWS\tPID\tGenus\tSampleID\tDOI\n");    // Write header to DOI log
			}

			//Get DB version and sets to 1 if not specified
            dbVersion = config.getString("db.version");
            dbVersion =((dbVersion != null) && (!dbVersion.isEmpty())) ? dbVersion : "1";

			//Print configuration to both the console and to errors.txt
			String configuration = "Configuration\n" +
				"Database URL:      [" + url + "]\n" +
				"Database username: [" + username + "]\n" +
                "Database password: [" + password + "]\n" +
                "Database version:  [" + dbVersion + "]\n" +
				"Query limit:       [" + qlimit + "]\n" +
				"GLIS URL:          [" + glisUrl + "]\n" +
				"GLIS username:     [" + glisUsername + "]\n" +
				"GLIS password:     [" + glisPassword + "]\n" +
				"Write DOI log:     [" + (doiLog ? "Yes" : "No") + "]\n";
			System.err.println(configuration);

			// Register first then update
				process("register", fDOI);
				process("update", fDOI);

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

    /*
     * Finds pgrfas rows to process and processes them according to the operation requested
     */
	private static void process(String operation, Writer fDOI) throws Exception {

		// Build list of pgrfa ids to register. Order by id to ensure a logical sequence
		List<String> ids = select(conn ->
				conn.createQuery("select id from pgrfas where operation=:operation and processed=:processed order by id")
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
        Map<String, Object> conf = new HashMap<> ();
		conf.put("glis_username",glisUsername);
		conf.put("glis_password",glisPassword);

		// Process each pgrfa in the list obtained by the query
		Integer count = 0;
		for (String id: ids) {

			// Extract main pgrfa record
			Map<String, Object> pgrfa = select(conn -> conn.createQuery("select * from pgrfas where id=:id").addParameter("id", new BigInteger(id))).get(0);

			// Save values to be used in response object
			String   wiews      = pgrfa.get("hold_wiews").toString();
			String   pid        = pgrfa.get("hold_pid").toString();

			// Build the XML document according to the DB version
            Document doc = null;
            if (dbVersion.equals("1")) {
                doc = buildDocumentV1(id, pgrfa, conf);
            } else {
                String sample_id = pgrfa.get("sample_id").toString();
                doc = buildDocumentV2(sample_id, pgrfa, conf);
            }

			// Transform XML using the XSL stylesheet and then transform again removing all empty elements
			Document request    = transform(doc,     "transform.xsl");
			request             = transform(request, "prune.xsl");
			String   xmlRequest = $(request).toString();

			// Attempts the HTTP POST transaction to GLIS and obtains result
			try {
				HttpResponse<String> httpResponse = Unirest.post(glisUrl)
						.header("accept", "application/xml")
						.body(xmlRequest)
						.asString();
				String   xmlResponse = httpResponse.getBody();
				Document response = $(xmlResponse).document();

				// Extract relevant information, if available
				String doi      = $(response).child("doi").text();
				String sampleId = $(response).child("sampleid").text();
				String genus    = $(response).child("genus").text();
				String error    = $(response).child("error").text();

				// Set the result of the transaction
				String result   = ((error != null) && (!error.isEmpty())) ? "KO" : "OK";

				// If there was a connection error, abort
				if (httpResponse.getStatus() == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
					System.out.println(sampleId + " - " + genus + " - " + error);
					Unirest.shutdown();
					if (fDOI != null) fDOI.close();
					System.exit(1);
				}

				// Write to DOI log if it has been created, if successful and operation is "register"
				if ((fDOI != null) && Objects.equals(operation, "register") && Objects.equals(result,"OK")) {
					fDOI.write(wiews + "\t" + pid + "\t" + genus + "\t" + sampleId + "\t" + doi + "\n");
				}

				// Write result to DB and mark pgrfas row as processed
				insertResult(operation, result, doi, sampleId, genus, error);
				markAsProcessed(id);

				// Provide some feedback on the operation
				count++;
				System.out.println("Processed pgrfa row [count: " + count.toString() + "/" + idNum + "] [id: " + id + "] [sample_id: " + sampleId + "]: " + result);
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
    private static Document buildDocumentV1(String id, Map<String, Object> pgrfa, Map<String, Object> conf) throws ParserConfigurationException {

        // Get related rowsets
        List<Map<String, Object>> actors      = select(conn -> conn.createQuery("select * from actors      where pgrfa_id=:pgrfa_id").addParameter("pgrfa_id", id));
        List<Map<String, Object>> identifiers = select(conn -> conn.createQuery("select * from identifiers where pgrfa_id=:pgrfa_id").addParameter("pgrfa_id", id));
        List<Map<String, Object>> names       = select(conn -> conn.createQuery("select * from names       where pgrfa_id=:pgrfa_id").addParameter("pgrfa_id", id));
        List<Map<String, Object>> progdois    = select(conn -> conn.createQuery("select * from progdois    where pgrfa_id=:pgrfa_id").addParameter("pgrfa_id", id));
        List<Map<String, Object>> targets     = select(conn -> conn.createQuery("select * from targets     where pgrfa_id=:pgrfa_id").addParameter("pgrfa_id", id));
        List<Map<String, Object>> tkws        = select(conn -> conn.createQuery("select k.* from tkws k, targets t where t.pgrfa_id=:pgrfa_id and t.id=k.target_id").addParameter("pgrfa_id", id));

        Match result = $("root",
                addMap("conf",  conf),
                addMap("pgrfa", pgrfa));
        addList(result, "actor",      actors);
        addList(result, "identifier", identifiers);
        addList(result, "name",       names);
        addList(result, "progdoi",    progdois);
        addList(result, "target",     targets);
        addList(result, "tkw",        tkws);
        return result.document();
    }

    /*
     * Builds the XML document by extracting the various rows related to the passed pgrfa
     * Works on v2 database schema
     */
    private static Document buildDocumentV2(String sid, Map<String, Object> pgrfa, Map<String, Object> conf) throws ParserConfigurationException {

        // Get related rowsets
        List<Map<String, Object>> actors      = select(conn -> conn.createQuery("select * from actors      where sample_id=:sid").addParameter("sid", sid));
        List<Map<String, Object>> identifiers = select(conn -> conn.createQuery("select * from identifiers where sample_id=:sid").addParameter("sid", sid));
        List<Map<String, Object>> names       = select(conn -> conn.createQuery("select * from names       where sample_id=:pgrfa_id").addParameter("pgrfa_id", sid));
        List<Map<String, Object>> targets     = select(conn -> conn.createQuery("select * from targets     where sample_id=:sid").addParameter("sid", sid));

        //System.out.println("DEBUG: QUERIES DONE");

        List<Map<String, Object>> progdois = explodeProgDois(pgrfa.get("progdois"), sid);
        List<Map<String, Object>> tkws     = explodeTkws(targets);
        Match result = $("root",
                addMap("conf",  conf),
                addMap("pgrfa", pgrfa));
        addList(result, "actor",      actors);
        addList(result, "identifier", identifiers);
        addList(result, "name",       names);
        addList(result, "progdoi",    progdois);
        addList(result, "target",     targets);
        addList(result, "tkw",        tkws);
		return result.document();
    }

	/*
     * Transforms the XML document using the passed XSLT stylesheet
     */
    private static Document transform(Document doc, String xslPath) {
		return $(doc)
				.transform(Main.class.getClassLoader().getResourceAsStream(xslPath))
				.document();
	}

	/*
	 * Wrapper for the update operation to the pgrfas table
	*/
	private static void markAsProcessed(String id) {
		final String query = "update pgrfas set processed = 'y' where id=:id";
		try (Connection conn = sql2o.open()) {
			conn.createQuery(query)
					.addParameter("id", new BigInteger(id))
					.executeUpdate();
		}
	}

	/*
	 * Wrapper for the insert operation into the results table
	 */
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

	/*
	 * Wrapper for the select operation
	 */
	private static List<Map<String, Object>> select(Function<Connection, Query> query) {
		try (Connection conn = sql2o.open()) {
			return query.apply(conn).executeAndFetchTable().asList();
		}
	}

	/*
	 * Wrapper to add a list of maps to the XML document
	 */
	private static Match addList(Match m, String label, List<Map<String, Object>> list) {
		list.forEach(map -> m.append(addMap(label, map)));
		return m;
	}

	/*
	 * Wrapper to add a map to the XML document
	 */
	private static Match addMap(String label, Map<String, Object> map) {
		Match m = $(label);
		map.entrySet().stream()
				.filter(entry -> entry.getValue() != null)
				.forEach(entry -> m.append($(entry.getKey()).text(entry.getValue().toString())));
		return m;
	}


	/*
     * Process progenitor DOIs list by splitting the field into a new list of maps
	 */
	private static List<Map<String, Object>> explodeProgDois(Object listObj, String sid) {
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
    private static List<Map<String, Object>> explodeTkws(List<Map<String, Object>> targets) {
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

}
