package de.unifrankfurt.informatik.acoli.fid.exec;


import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

import de.unifrankfurt.informatik.acoli.fid.types.ExecutionMode;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;



public class Run {
	
	static String configurationFilePath = "";
	static String alternativeConfigFile = "";
	static String seedFile = "";
	
	/**
	 * Load FIDConfig.xml
	 * @param configurationFile
	 * @return
	 */
	public static XMLConfiguration loadFIDConfig(String configurationFile) {
		
		XMLConfiguration fidConfig = null;
		Configurations configs = new Configurations();
    	try {
    	    fidConfig = configs.xml(configurationFile);
    	}
    	catch (ConfigurationException cex)
    	{
    		cex.printStackTrace();
    		System.exit(0);
    	}
    	
    	return fidConfig;
	}
	
	
	/**
	 * Load FIDConfig.xml configuration file
	 * @return Configuration
	 */
	public static XMLConfiguration loadFIDConfig() {
		
		String defaultConfigurationFileName = "FIDConfig.xml";
		String customConfigFile = System.getenv().get("FID_CONFIG_FILE"); // set in /etc/environment !
		
		
		// 1. Try command-line option -CX
	    if (!alternativeConfigFile.isEmpty()) {
	    	configurationFilePath = Paths.get(alternativeConfigFile).toString();
	    	System.out.println("Using config : "+configurationFilePath);
	    }
	    
		else 
	    	
		// 2. Try FID_CONFIG_FILE environment variable
		if (Files.exists(Paths.get(customConfigFile))) {
				configurationFilePath = Paths.get(customConfigFile).toString();
				System.out.println("Using FID_CONFIG_FILE "+configurationFilePath);
		}
		
	    else

		// 3. Try FIDConfig.xml in working directory
	    if (Files.exists(Paths.get(System.getProperty("user.dir")+"/"+defaultConfigurationFileName))) {
	    	configurationFilePath = Paths.get(System.getProperty("user.dir")+"/"+defaultConfigurationFileName).toString();
	    	System.out.println("Using config "+Paths.get(System.getProperty("user.dir")+"/"+defaultConfigurationFileName).toString());
	    }
		
	    else 
	    
		// 4. Web-app start
	    if (Run.class.getResource("/FIDConfig.xml") != null) {
	    	configurationFilePath = Run.class.getResource("/FIDConfig.xml").getPath();
	    	System.out.println("Using config /FIDConfig.xml");
	    }

	    // 5. FIDConfig.xml Not found -> exit
		else 
			{ System.out.println("Configuration file "+configurationFilePath+" not found !");
			  System.exit(0);}
		
		XMLConfiguration config = loadFIDConfig(configurationFilePath);
		
		if(!seedFile.isEmpty()) config.setProperty("RunParameter.urlSeedFile", seedFile);
		
		return config;
	}
	
	
	/**
	 * Run the application
	 * @param args
	 */
	public static void main (String [] args) {
		
				
		Executer executer = null;
		XMLConfiguration fidConfig;

		
		//******************************
		//  APPLICATION CONFIGURATION  *
		//******************************
		
		
		String [] defaultArgs = {"-h"};
		
		
		CommandLine commandLine;

	    Option optionHelp = Option.builder("h").longOpt("help").desc("Show this help").build();
	    Option optionInit = Option.builder("IN").longOpt("init").desc("Initialize application - deletes all data !").build();
	    Option optionSetExecute = Option.builder("EX").longOpt("execute").desc("Run").build();
	    // Option optionMakeResult = Option.builder("MR").longOpt("make-result").desc("Create the result output of the llod search.").build();
	    //Option optionUpdateModels = Option.builder("UP").longOpt("update-models").desc("Update all OLiA models").build();
	    //Option optionMakeUrlpool = Option.builder("MP").longOpt("make-urlpool").desc("Create urlpool file with list of resources that will be processed with -EX").build();
	    //Option optionDeleteResource = Option.builder("DR").longOpt("delete-resource").hasArg().argName("resource-download-URL").desc("Delete a single resource from the database. The resource is identified by its data-download URL.").build();
	    //Option optionRefreshOLiA = Option.builder("RO").longOpt("refresh-olia").desc("Create new OLiA BLL search terms map.").build();
	    //Option optionExportXml = Option.builder("XM").longOpt("export-xml").desc("Create Mods-XML for existing searchEngineResults including Linghub metadata.").build();
	    //Option optionDDBExport = Option.builder("ED").longOpt("export-data").hasArg().argName("export-file").desc("Only export the existing data DB to GraphML (XML) file").build();
	    //Option optionRunPatch = Option.builder("PA").longOpt("database-patch").desc("Run a database patch - see code in Executer.executePatch()").build();
	    Option optionConfigFile = Option.builder("CX").longOpt("config-file").hasArg().argName("configfile").desc("Provide configuration file").build();
	    Option optionSeedFile = Option.builder("SD").longOpt("seed-file").hasArg().argName("seedfile").desc("Provide seed file with URLs to be processed").build();

	    
	       
	    Options options = new Options();
	    options.addOption(optionHelp);
	    options.addOption(optionSetExecute);
	    //options.addOption(optionMakeResult);
	    //options.addOption(optionMakeUrlpool);
	    //options.addOption(optionRefreshOLiA);
	    //options.addOption(optionExportXml);
	    options.addOption(optionInit);
	    //options.addOption(optionDeleteResource);
	    //options.addOption(optionDDBExport);
	    //options.addOption(optionRunPatch);
	    //options.addOption(optionUpdateModels);
	    options.addOption(optionConfigFile);
	    options.addOption(optionSeedFile);

	    
	    CommandLineParser parser = new DefaultParser();
	  
	    String header = "FID Runner";
	    String footer = "";
	    HelpFormatter formatter = new HelpFormatter();
	    if (args.length > 0) {
	    	defaultArgs = args;
	    }
	    
	    boolean printConfig=false;
	    

		try
		    {
		        commandLine = parser.parse(options, defaultArgs);
		        
		        /*
		         * Show help
		         */
		        if (commandLine.hasOption("h"))
		        {
		        	System.out.println(header);
		        	formatter.printHelp("program", " ", options, footer, true);
		        	return;
		        }
		        
		        
		        if (commandLine.hasOption("CX")) {
		        	alternativeConfigFile = commandLine.getOptionValue("CX");
	        	}
		        
		        
		        if (commandLine.hasOption("SD")) {
		        	seedFile = commandLine.getOptionValue("SD");
	        	}
		        
		        
		        if (commandLine.hasOption("EX") || commandLine.hasOption("IN")) 
		        {
		        	System.out.println("Starting ...");
		        	
		        	fidConfig = Run.loadFIDConfig();
		        	
		    		// disable queue for command-line processing
		        	fidConfig.setProperty("RunParameter.startExternalQueue", "false");
		    		
		        	// set seed file in config
		        	if (!seedFile.isEmpty()) {
		    			fidConfig.setProperty("RunParameter.urlSeedFile", seedFile);
		    		}
		        	
		        	
		    		executer = new Executer(fidConfig);
		    		executer.setExecutionMode(ExecutionMode.UNDEFINED);
		    		
		        } else {
		        	System.out.println("Not enough arguments !");
		        }
		        
		        
		        /**
		         * Run the application
		         */
		        if (commandLine.hasOption("EX"))
		        {

		        	executer.setExecutionMode(ExecutionMode.ADD);
		        	executer.run();
		        	return;
		        }
		        
		        
		        if (commandLine.hasOption("IN")) {
		        	
		        	System.out.println("You are about to delete everything - do you want to proceed ?");
		        	//Scanner scanner = new Scanner(System.in);
		         
		            String input = "yes";//scanner.nextLine();
		        	
		        	if(input.toLowerCase().equals("y")|| input.toLowerCase().equals("yes")) {
		        	    System.out.println("... proceeding with initialization !");
		        	} else {
		        		System.out.println("initialization canceled !");
		        		return;
		        	}
		        	
		        	executer.setExecutionMode(ExecutionMode.INIT);
		    		executer.run();
		    		
		    		System.out.println("Database initialized - now run with -EX option to add data !");
		    		return;
		        }
		        
		       
		        /*
		         * Update all OLiA models
		         */
		       /* if (commandLine.hasOption("UP"))
		        {
		        	executer.setExecutionMode(ExecutionMode.UPDATEMODELS);
		        	executer.run();
		        	return;
		        	
		        }*/
		        
		        
		        // Create URL-list of files to be processed but do not execute !
		       /* if (commandLine.hasOption("MP")) {
	
		        	executer.setExecutionMode(ExecutionMode.MAKEURLPOOL);
		        	executer.run();
		        	return;
		        }*/
		        
		        
		       
		        
		       /* 
		         * Run database patch. The patch code is hard-coded in the method Executer.executePatch()
		         
		        if (commandLine.hasOption("PA"))
		        {
		        	executer.setExecutionMode(ExecutionMode.RUNDBPATCH);
		        	executer.run();
		        	return;
		        	
		        }*/
		        
		        
		        
		        /*
		         * Make results using SearchEngine (deprecated)
		         
		        if (commandLine.hasOption("MR"))
		        {
		        	executer.setExecutionMode(ExecutionMode.MAKERESULT);
		        	executer.run();
		        	return;
		        	
		        }*/
		        
		        
		        /*if (commandLine.hasOption("ED")) {
		        	
		        	try {
						executer.getDataDbConfig().setDatabaseExportJsonFile(new File(commandLine.getOptionValue("ED")));
					} catch (Exception e) {
						e.printStackTrace();
					}
		        	executer.setExecutionMode(ExecutionMode.EXPORTDDB);
		        	executer.run();
		        	return;
		        }*/
		        
		       
		        /*// Update SearchTerms (deprecated)
		        if (commandLine.hasOption("RO"))
		        {
		        	Constants.loadConfig(configurationFilePath);
		        	try {
		        		SearchTerms terms = SearchTerms.getUniqueSearchTerms(true);
		        	} catch (Exception e) {
		        		e.printStackTrace();
		        	}
		        	return;
		        }*/
		        

		        /*// Create XML Export in Mods format for UB. (deprecated)
		        if (commandLine.hasOption("XM"))
		        {
		        	Constants.loadConfig(configurationFilePath);
		        	String input = fidConfig.getString("SearchEngine.resultFile");
		        	String output = fidConfig.getString("SearchEngine.xmlExportFile");
		        	LinghubExporter.main(("-onlythesaurus -i " + input + " -o " + output).split(" "));
		        	try {
		        		SearchTerms terms = SearchTerms.getUniqueSearchTerms(false);
		        	} catch (Exception e) {
		        		e.printStackTrace();
		        	}
		        	return;
		        }*/
		        
		    }
		    catch (ParseException exception)
		    {
		        Utils.debugNor("Parse error: ");
		        System.out.println(exception.getMessage());
		    }	
	}
}
