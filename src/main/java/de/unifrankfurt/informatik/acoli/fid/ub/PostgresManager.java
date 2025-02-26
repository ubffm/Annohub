package de.unifrankfurt.informatik.acoli.fid.ub;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.configuration2.XMLConfiguration;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import de.unifrankfurt.informatik.acoli.fid.detector.ContentTypeDetector;
import de.unifrankfurt.informatik.acoli.fid.parser.JsonMetadataTools;
import de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager;
import de.unifrankfurt.informatik.acoli.fid.spider.DownloadManager;
import de.unifrankfurt.informatik.acoli.fid.types.MetadataSource;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceFormat;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo;
import de.unifrankfurt.informatik.acoli.fid.util.LocateUtils;
 
public class PostgresManager {
     
    private String remote_host="";
    private String database = "";
    private String database_user="";
    private String database_password="";
    private String sshUser="";
    private int assigned_port = 5432;
    private Session session = null;
    private Connection conn = null;
	private String pkFile;
	private XMLConfiguration config = null;
	private DownloadManager dm;

	private LocateUtils locateUtils = new LocateUtils();

    
    // Remote host and port
    final static int local_port=5432;
    final static int remote_port=5432;
 
    
    // Logger
    private final static Logger LOGGER =
            Logger.getLogger(PostgresManager.class.getName());
    
    
    public PostgresManager(XMLConfiguration config) {
    	
    	pkFile = config.getString("Databases.Postgres.keyFile");
    	remote_host = config.getString("Databases.Postgres.remoteHost");
    	database = config.getString("Databases.Postgres.database");
    	database_user = config.getString("Databases.Postgres.databaseUser");
    	database_password = config.getString("Databases.Postgres.databasePassword");
    	sshUser = config.getString("Databases.Postgres.sshUser");  	
    	this.config = config;
    	
		dm = new DownloadManager(null, null, null, null, 60);

    }
    
    /**
     * @param pkFile (use empty String for local connection)
     * @deprecated
     */
    public PostgresManager(String pkFile) {
    	
    	this.pkFile = pkFile;
    }


    /**
     * Connect to a PostgreSQL database
     * @return a Connection object
     * @param database name
     */
    private Connection connect(String database) {

        if (!this.pkFile.isEmpty()) {
  
        	try {
  
	            JSch jsch = new JSch();
	            jsch.addIdentity(this.pkFile, "");
	             
	            // Create SSH session.  Port 22 is your SSH port which
	            // is open in your firewall setup.
	            session = jsch.getSession(sshUser, remote_host, 22);
	
	            // Additional SSH options.  See your ssh_config manual for
	            // more options.  Set options according to your requirements.
	            java.util.Properties config = new java.util.Properties();
	            config.put("StrictHostKeyChecking", "no");
	            config.put("Compression", "yes");
	            config.put("ConnectionAttempts","2");
	             
	            session.setConfig(config);
	             
	            // Connect
	            session.connect();
	         
	            // Create the tunnel through port forwarding.  
	            // This is basically instructing jsch session to send 
	            // data received from local_port in the local machine to 
	            // remote_port of the remote_host
	            // assigned_port is the port assigned by jsch for use,
	            // it may not always be the same as
	            // local_port.
	             
	            assigned_port = session.setPortForwardingL(local_port,"127.0.0.1", remote_port);
	            //System.out.println("assigned port : "+assigned_port );
	             
	        } catch (JSchException e) {
	        	shutdown();
	        	System.out.println("1");
	            LOGGER.log(Level.SEVERE, e.getMessage()); return null;
	        }
	         
	        if (assigned_port == 0) {
	        	shutdown();
	        	System.out.println("2");
	            LOGGER.log(Level.SEVERE, "Port forwarding failed !"); 
	            return null;
	        }
    	}
 
        // Database access credintials.  Make sure this user has
        // "connect" access to this database;
         
        // Build the  database connection URL.
        StringBuilder url =
                new StringBuilder("jdbc:postgresql://localhost:");
         
        // use assigned_port to establish database connection
        url.append(assigned_port).append ("/").append(database).append ("?user=").
                append(database_user).append ("&password=").
                append (database_password);
                 
        
        try {
            Class.forName(
                    "org.postgresql.Driver").newInstance();
            conn = DriverManager.getConnection(url.toString());
            System.out.println("Connected to the PostgreSQL server successfully.");
            return conn;
        
        } catch (ClassNotFoundException |
                IllegalAccessException |
                InstantiationException |
                java.sql.SQLException e) {
        		shutdown();
            	LOGGER.log(Level.SEVERE, e.getMessage());
            	System.out.println("3");
            	System.out.println(e.getMessage());
            	e.printStackTrace();
        }
        
		return conn; 
    }
    /**
     * Make connection to postgres database with tunneling
     * @param pkfile Keyfile
     * @param auser username of host
     * @param ahost ip address of host
     * @param apassword pw of user
     */
    public PostgresManager(String pkfile, String auser, String ahost, String apassword){
        conn = connect(pkfile, auser, ahost, apassword);
    }

    /**
     * Connect to the PostgreSQL database with tunneling
     * @return a Connection object
     */
    private Connection connect(String pkFile, String auser, String ahost, String apassword) {

        int assigned_port;

        try {

            JSch jsch = new JSch();
            jsch.addIdentity(pkFile, "");

            // Create SSH session
            session = jsch.getSession(auser, ahost);
            session.setPassword(apassword);

            // Additional SSH options.  See your ssh_config manual for
            // more options.  Set options according to your requirements.
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            config.put("Compression", "yes");
            config.put("ConnectionAttempts","2");

            session.setConfig(config);


            session.connect();
            LOGGER.log(Level.INFO, "Successfully connected to "+ahost+" with user "+auser);
            // We're now at ahost and setup the second connection.
            assigned_port = session.setPortForwardingL(4422, remote_host, 22);
            // We'll create a second session towards to PostgreSQL port on remote machine

            Session secondSession = jsch.getSession(sshUser, "localhost", 4422);
            secondSession.setConfig(config);
            secondSession.connect(); // authentification was saved in jsch above
            LOGGER.log(Level.INFO, "Sucessfully connected to "+remote_host+" with user "+sshUser);

            // And finally setup the port forwarding from remote host to calling machine.


        } catch (JSchException e) {
            shutdown();
            System.out.println("1");
            LOGGER.log(Level.SEVERE, e.getMessage()); return null;
        }

        if (assigned_port == 0) {
            shutdown();
            System.out.println("2");
            LOGGER.log(Level.SEVERE, "Port forwarding failed !");
            return null;
        }

        StringBuilder url = new StringBuilder("jdbc:postgresql://localhost:");
        url.append(assigned_port).append("/").append(database).append("?user=")
                .append(database_user).append("&password=")
                .append(database_password);

        try {
            Class.forName(
                    "org.postgresql.Driver").newInstance();
            conn = DriverManager.getConnection(url.toString());
            System.out.println("Connected to the PostgreSQL server successfully.");
            return conn;

        } catch (ClassNotFoundException |
                IllegalAccessException |
                InstantiationException |
                java.sql.SQLException e) {
            shutdown();
            LOGGER.log(Level.SEVERE, e.getMessage());
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

        return conn;
    }
    
    public void shutdown() {
    	try {
    		
    		// Close SSH session (if open)
    		if (!this.pkFile.isEmpty()) {
    			// free ports
    			getSession().delPortForwardingL(local_port);
    			getSession().delPortForwardingR(remote_port);
        		session.disconnect();
    		}
    		
    		conn.close();

    	} catch (Exception e) {
    		e.printStackTrace();
    	}
	}

    /**
     * Retrieve all results from external database. Convert to objects and detect ResourceFormat
     * @return ResourceInfo list of resources
     */
    public ArrayList<ResourceInfo> getExternalCrawlerResultsAsResourceInfos() {
    	ArrayList<ResourceInfo> empty = new ArrayList<ResourceInfo>();
    	return getExternalCrawlerResultsAsResourceInfos(null);
    }

	/**
     * Retrieve results from external database with given ResourceFormat. Convert to objects and detect ResourceFormat
     * @param Resource type filter
     * @return ResourceInfo list
     */
    public ArrayList<ResourceInfo> getExternalCrawlerResultsAsResourceInfos(ResourceFormat formatFilter) {
    	
    	// Connect to database (hardcoded)
    	conn = connect("linghub_analysis");if (conn == null) return null;
    	
    	ArrayList<ResourceInfo> resourceInfos = new ArrayList<ResourceInfo>();
    	
    	// Get all URLs with their respective file extension and mime type
        String SQL = "SELECT linghub,url,type_ending_in_url,magic_mime_type FROM webpages,types "
        		+ "where webpages.url_id = types.webpage_id";

        try {
        	
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(SQL);
        while (rs.next()) {
        	resourceInfos.add(new ResourceInfo(
        			rs.getString("url"),
        			rs.getString("linghub") , 
        			ContentTypeDetector.getResourceFormatFromMIMEAndFileExtension(
        			rs.getString("magic_mime_type"),
        			rs.getString("type_ending_in_url"))));
        }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
        
        // close connnection
        shutdown();
 
        if (formatFilter == null) {
        	return resourceInfos;
        }
        else {
        	return (ArrayList<ResourceInfo>) resourceInfos.stream()
        	    .filter(p -> p.getResourceFormat() == formatFilter).collect(Collectors.toList());
        }
    }
    
    
    /**
     * Retrieve results from external database with given ResourceFormat. Convert to objects and detect ResourceFormat
     * @param rfl Resource-list
     * @param Resource type filter
     * @return ResourceInfo list
     */
    public void updateClarinMetadata(List<ResourceInfo> rfl) {
    	
    	// Connect to database (hardcoded)
    	conn = connect(database);if (conn == null) {
    		return;
    	}
    	
    	System.out.println("updateClarinMetadata");
    	System.out.println("updating Clarin metadata ...");
    	
    	HashMap<String,String> links = new HashMap<String,String>();
    	
    	// 1. query all available data links
        String linkQuery = "SELECT link, source from metadata";

        try {
        	
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(linkQuery);
            while (rs.next()) {
            	links.put(rs.getString("link"), rs.getString("source"));
            }
            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
         }
    	
        /*for (String url : links) {
        	System.out.println("link:"+url);
        }*/
        System.out.println("CLARIN links # :"+links.size());
        
        String SQL = config.getString("Clarin.clarinQueries");
        String sqlQuery = "";
        
        
        // 1.a) read metadata for spraakbanken from local json file
        HashMap<String, String> spraakbankenMetadata = 
        		JsonMetadataTools.readSpraakbankenJsonFile1(locateUtils.getLocalFile("/metadata/spraakbanken.json"));
        
        System.out.println("Spraakbanken links # :"+spraakbankenMetadata.size());

        
    	// 2. fetch metadata for resources
        boolean metadataFromCLARIN;
        
        for (ResourceInfo ri : rfl) {
        	
        	metadataFromCLARIN = false;
        	
        	// skip resources that already have metadata
        	if (ri.getLinghubAttributes().getMetadataSource() != MetadataSource.NONE) continue;
        	
	        if (links.keySet().contains(ri.getDataURL())) {
	        	metadataFromCLARIN = true;
	        	System.out.println("CLARIN contains link : "+ri.getDataURL());
	        	sqlQuery = SQL.replace("ACCESSURL", ri.getDataURL());
        	} else {
        		if (spraakbankenMetadata.containsKey(ri.getDataURL())) {
        			System.out.println("Spraakbanken contains link : "+ri.getDataURL());
        			System.out.println("Checking CLARIN source ...");
        			String link_ = "";
        			for (String l : links.keySet()) {
        				// match resource (link is full resource URL, spraakbankenMetadata has only last path element (name)
        				if (links.get(l).endsWith(spraakbankenMetadata.get(ri.getDataURL()))) {
        					link_ = l;
        					System.out.println("... found !");
        					break;
        				}
        			}
        			if (!link_.isEmpty()) {
        	        	sqlQuery = SQL.replace("ACCESSURL", link_);
        			} else {
    					System.out.println("... nothing found !");
        				System.out.println("trying Spraakbanken json ...");
        				
        				// Get the json document 
        				// String jsonUrl = "https://spraakbanken.gu.se/eng/resource/"+spraakbankenMetadata.get(ri.getDataURL())+"/json";
        				// Trying to add metadata from Spraakbanken website
        				try {
	        				if (!JsonMetadataTools.	
	        				        readSpraakbankenJsonFile2(locateUtils.getLocalFile("/metadata/spraakbanken2.json"),ri)){
	        				
	        				System.out.println("not found !");continue;
	        				}
	        				else {
	        					System.out.println("success !");
	        					continue; // continue with next resource !
	        				}
        				} catch (Exception e) {
        					System.out.println("Error spraakbanken : "+ri.getDataURL());
        					e.printStackTrace();
        					continue;
        				}
        			}
        		} else {
					System.out.println("... nothing found !");
        			continue;
        		}
        	}
	        
	        System.out.println("adding metadata for : "+ri.getDataURL());
        	
	        try {
	        	
	        Statement stmt = conn.createStatement();
	        ResultSet rs = stmt.executeQuery(sqlQuery);
	        while (rs.next()) {
	        	
	        	if (metadataFromCLARIN) {
	        		System.out.println("METADATA from CLARIN for : "+ri.getDataURL()+" "+ri.getFileInfo().getFileId());
	        		System.out.println("title "+!rs.getString("title").isEmpty());
	        		System.out.println("description "+!rs.getString("description").isEmpty());
	        	}
	        	ri.getLinghubAttributes().setTitle(rs.getString("title"));
	        	ri.getLinghubAttributes().setDescription(rs.getString("description"));
	        	ri.getLinghubAttributes().setType(rs.getString("resource_type"));
	        	//ri.getLinghubAttributes().setDate(new Date(rs.getString("date")));
	        	ri.getLinghubAttributes().setCreator(rs.getString("author"));
	        	ri.getLinghubAttributes().setRights(rs.getString("licence"));
	        	ri.getLinghubAttributes().setYear(rs.getString("date"));
	        	ri.getLinghubAttributes().setPublisher(rs.getString("publisher"));
	        	ri.getLinghubAttributes().setDcLanguageString(rs.getString("language"));
	        	ri.getLinghubAttributes().setDctLanguageString(rs.getString("language"));
	        	ri.getLinghubAttributes().setMetadataSource(MetadataSource.CLARIN);
	        	ri.setMetaDataURL(ResourceManager.MetaDataFromClarin);
	        }
	        } catch (SQLException ex) {
	            System.out.println(ex.getMessage());
	        }
        }
        // close connection
        shutdown();
        System.out.println("done !");
    }

    
    /**
     * Make SQL query on postgres database
     * @param sqlQuery query string
     * @return ResultSet results
     */
    public ResultSet makeQuery(String sqlQuery) {
    	
        try {
	        Statement stmt = conn.createStatement();
	        ResultSet rs = stmt.executeQuery(sqlQuery);
	        return rs;
        
        } catch (Exception ex) {
        	
            System.out.println(ex.getMessage());
            ex.printStackTrace();
            return null;
        }
    }
    

    private Session getSession() {
    	return session;
    }

    private Connection getConnection() {
    	return conn;
    }
    

}
