package de.unifrankfurt.informatik.acoli.fid.exec;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.activemq.broker.BrokerService;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFFormat;
import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.io.IoCore;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;

import de.unifrankfurt.informatik.acoli.fid.activemq.Consumer;
import de.unifrankfurt.informatik.acoli.fid.activemq.Producer;
import de.unifrankfurt.informatik.acoli.fid.detector.OptimaizeLanguageTools1;
import de.unifrankfurt.informatik.acoli.fid.gremlinQuery.EmbeddedQuery;
import de.unifrankfurt.informatik.acoli.fid.linghub.UrlBroker;
import de.unifrankfurt.informatik.acoli.fid.owl.ModelDefinition;
import de.unifrankfurt.informatik.acoli.fid.owl.OntologyManager;
import de.unifrankfurt.informatik.acoli.fid.resourceDB.RMEmbedded;
import de.unifrankfurt.informatik.acoli.fid.resourceDB.RMServer;
import de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager;
import de.unifrankfurt.informatik.acoli.fid.resourceDB.TemplateManager;
import de.unifrankfurt.informatik.acoli.fid.search.GWriter;
import de.unifrankfurt.informatik.acoli.fid.search.GWriterN;
import de.unifrankfurt.informatik.acoli.fid.search.GWriterT;
import de.unifrankfurt.informatik.acoli.fid.search.GraphTools;
import de.unifrankfurt.informatik.acoli.fid.serializer.RDFSerializer;
import de.unifrankfurt.informatik.acoli.fid.spider.ConllFileHandler;
import de.unifrankfurt.informatik.acoli.fid.spider.DownloadManager;
import de.unifrankfurt.informatik.acoli.fid.spider.GenericRdfFileHandler;
import de.unifrankfurt.informatik.acoli.fid.spider.GenericXMLFileHandler;
import de.unifrankfurt.informatik.acoli.fid.spider.Statistics;
import de.unifrankfurt.informatik.acoli.fid.spider.VifaWorker;
import de.unifrankfurt.informatik.acoli.fid.spider.VifaWorkerMQ;
import de.unifrankfurt.informatik.acoli.fid.types.DBType;
import de.unifrankfurt.informatik.acoli.fid.types.DatabaseConfiguration;
import de.unifrankfurt.informatik.acoli.fid.types.ExecutionMode;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceFormat;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo;
import de.unifrankfurt.informatik.acoli.fid.types.UpdatePolicy;
import de.unifrankfurt.informatik.acoli.fid.ub.PostgresManager;
import de.unifrankfurt.informatik.acoli.fid.util.IndexUtils;
import de.unifrankfurt.informatik.acoli.fid.util.LocateUtils;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;

/**
 * Class with main function that starts the process
 * 
 * @author frank
 * 
 */

public class Executer {
	
	private static DatabaseConfiguration registryDbConfig;
	private static DatabaseConfiguration dataDbConfig;
	private static XMLConfiguration fidConfig;
	private static UpdatePolicy updatePolicy;
	private Cluster cluster;
	
	private ExecutorService exs;
	private BrokerService activemqBroker;
	public final static String MQ_IN_1 = "WORKER-IN-1";
	public final static String MQ_OUT_1 = "WORKER-OUT-1";
	public final static String MQ_Default = "SERVICE-1";
	private Runnable[] workers;
	private Future<?>[] workerRef;
	
	public ResourceManager resourceManager = null;
	public TemplateManager templateManager;
	
	/*
	Use separate instances !!!
	RdfFileHandler rdfFileHandler = null;     // use own instance for each worker thread because of rdfStreamParser.reset(); in handler
	ConllFileHandler conllFileHandler = null; // not safe to use same instance in each worker thread !!!
	DownloadManager downloadManager = null;   // application crashes with multiple threads if only one instance is used for all worker threads !!!
	*/
	
	UrlBroker urlBroker = null;
	OntologyManager ontologyManager = null;
	ModelDefinition modelDefinition = null;
	
	public static GWriter writer = null;
	public Graph graph = null;
	
	Queue <ResourceInfo> _queue;
	Queue <ResourceInfo> _finishedQueue;
	HashSet <ResourceInfo> _resources = new HashSet <ResourceInfo>();
	static ArrayList <ResourceInfo> defaultResources = new ArrayList<ResourceInfo>();
	
	public static final String conllNs = "ufal.mff.cuni.cz/conll2009-st/task-description.html#";
	public static final HashSet<String> featureIgnoreList = new HashSet<String>() {
		private static final long serialVersionUID = 1363621361L;
	{
				add("word");
				add("msd");
				add("Translit");
				add("LTranslit");
				add("ref");
				add("Gloss");
				add("MGloss");
				add("MSeg");
				add("LGloss");
				add("LId");
				add("LDeriv");
				add("Vib");
				add("PLemma");
				add("PForm");
				add("Root");
				add("VForm");
				add("Vform");
				add("MorphInd");
				add("MWE");
				add("LvtbNodeId");
				add("Id");
				add("En");
				add("Orig");
				add("Morphs");
				add("Offset");
				add("LDeriv");
				add("LNumValue");
				add("ChunkId");
				add("Tam");
				add("AltTag");
				add("Alt");
				add("msd");
				add("word");
	}};
	
	
	private static boolean updateModels = true;
	
	 // 50 gigabytes max file size (uncompressed) (1 GB = 1073741824 bytes)
	private static Long uncompressedFileSizeLimitDefault = 50*1073741824L;
	
	// 1 gigabyte max file size compressed
	private static Long compressedFileSizeLimitDefault = 1073741824L;
	
	
	private boolean isTestRun = false;
	private boolean onlyUpdateModels = false;
	private boolean resetRegistryDbAtStartup = false;
	private boolean resetDataDbAtStartup = false;
	
	private ExecutionMode executionMode = ExecutionMode.UNDEFINED;
	
	public static final float coverageThresholdOnLoad = 0.4f; // only select models automatically with coverage >= threshold
	
	public LocateUtils locateUtils = new LocateUtils();



	public void setTestRun(boolean isTestRun) {
		this.isTestRun = isTestRun;
	}
	

	// Constructor 1
	public Executer (XMLConfiguration fidconfig) {
		
		fidConfig = fidconfig;
		
		// SET DEFAULT DATABASE CONFIGURATION
		// REG-DB : GremlinServer
		fidConfig.setProperty("DatabaseConfigurations.RegistryDBType","GremlinServer");

		// MOD-DB : Neo4J embedded
		fidConfig.setProperty("DatabaseConfigurations.DataDBType","Neo4J");
		
		makeDatabaseConfiguration();
	}

	

	// Constructor 2
	public Executer (DatabaseConfiguration registryDbConfiguration, DatabaseConfiguration dataDbConfiguration, XMLConfiguration fidconfig) {
		
		registryDbConfig = registryDbConfiguration;
		dataDbConfig = dataDbConfiguration;
		fidConfig = fidconfig;
	}


	/**
	 * Check application configuration parameter before running
	 */
	private boolean isConfigurationOK() {
		
		// Check VifaConfig.xml
		if (!IndexUtils.checkConfigAndSetDefaultValues(fidConfig)) return false;
		
		// Set the update policy after setting default values
		enableUpdatePolicy();

		Statistics.initialize();
	    //Statistics.printReport();			
		return true; 
	}
	

	
	public void run(ExecutionMode exeMode) {
		this.executionMode = exeMode;
		run();
	}
	
	
	/**
	 * Run the application
	 */
	public void run() {
		
		// Check configuration
		if (!isConfigurationOK()) {System.out.println("Config Error !");System.exit(0);}
		
		initializeLanguageDetector();
		
		// If data database is loaded from file then reset all
		if (dataDbConfig.getDbType() != DBType.TinkerGraph && dataDbConfig.getDatabaseImportJsonFile() != null) {
			
			System.out.println("Changing execution mode from "+this.executionMode+" to execution mode RESET because"
					+ " of data database reload "+dataDbConfig.getDatabaseImportJsonFile());
			this.executionMode = ExecutionMode.RESET;
		}
		
		System.out.println("\n\nStarting application in execution mode : "+this.executionMode.toString());
		
		
		switch (this.executionMode) {
		
			case INIT :		// Delete everything and reload models in dataDb
				
				updateModels = true;
				resetRegistryDbAtStartup = true;
				resetDataDbAtStartup = true;

				break;
			
			case ADD :		// Add more data
				
				updateModels = false;
				resetRegistryDbAtStartup = false;
				resetDataDbAtStartup = false;
				
				break;
				
			case CLEAN :	// Reset registry (includes removing hit nodes, but keep models !!!
				
				updateModels = false;
				resetRegistryDbAtStartup = true;
				resetDataDbAtStartup = false;
				
				break;
			
			case RESET :	// Reset all data (do not update models)
				
				updateModels = false;
				resetRegistryDbAtStartup = true;
				resetDataDbAtStartup = true;
				
				break;
				
			case DBSTART :	// Startup databases and return (test only)
				
				updateModels = false;
				resetRegistryDbAtStartup = false;
				resetDataDbAtStartup = false;
				
				break;
				
			case RUNDBPATCH :	// Run database patch
				
				updateModels = false;
				resetRegistryDbAtStartup = false;
				resetDataDbAtStartup = false;
				
				break;

			case UNDEFINED :// Only use parameters from configuration file and setters
				
				updateModels = false;
				resetRegistryDbAtStartup = false;
				resetDataDbAtStartup = false;
				break;
				
			case MAKERESULT :
				
				updateModels = false;
				resetRegistryDbAtStartup = false;
				resetDataDbAtStartup = false;
				break;
				
			case MAKEURLPOOL :
				
				updateModels = false;
				resetRegistryDbAtStartup = false;
				resetDataDbAtStartup = false;
				break;
				
			case SERVICE :
				
				updateModels = false;
				resetRegistryDbAtStartup = false;
				resetDataDbAtStartup = false;
				break;
				
			case EXPORTDDB :
			case EXPORTRDB :
				
				updateModels = false;
				resetRegistryDbAtStartup = false;
				resetDataDbAtStartup = false;
				break;
				
			case UPDATEMODELS :
				
				updateModels = true;
				resetRegistryDbAtStartup = false;
				resetDataDbAtStartup = false;
				break;
				
			
			default :		// Only use parameters from configuration file and setters
				
				updateModels = false;
				resetRegistryDbAtStartup = false;
				resetDataDbAtStartup = false;
				break;
		}
		
		// NEVER delete a database with a configuration parameter - use explicite function call !
		// TODO remove parameter resetRegistryDbAtStartup, resetDataDbAtStartup !
		if (executionMode != ExecutionMode.INIT) {
			resetRegistryDbAtStartup = false;
			resetDataDbAtStartup = false;
		}
		
		
		// Enable automatic model update from configuration parameter
		if (fidConfig.getString("OWL.modelUpdateMode").equals("auto")) {
			// not yet updateModels = true;
		}

		
		//if (!isConfigurationOK()) {System.out.println("Config Error !");System.exit(0);}
		
		System.out.println("\nDATABASE CONFIGURATIONS");
		registryDbConfig.setName("Registry");
		registryDbConfig.printConfiguration();
		dataDbConfig.setName("Model");
		dataDbConfig.printConfiguration();
		
		
		// Instantiate data database
		switch (dataDbConfig.getDbType()) {
		
		case TinkerGraph :
			
			graph = TinkerGraph.open();
			
			try {
				graph.io(IoCore.graphson()).readGraph(dataDbConfig.getDatabaseImportJsonFile().getAbsolutePath());
				System.out.println("Loaded graph : "+dataDbConfig.getDatabaseImportJsonFile().getAbsolutePath());
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("(Could not load json file !)");
			}
			
			writer = new GWriterT (graph, fidConfig);
			if (resetDataDbAtStartup) {
				writer.deleteDatabase();
				System.out.println("DELETING MODEL DATABASE");
			}
			break;
		
		case Neo4J :
			
			writer = new GWriterN (dataDbConfig.getDatabaseDirectory(), fidConfig);
			
			if (resetDataDbAtStartup) {
				writer.deleteDatabase();
				System.out.println("DELETING MODEL DATABASE");
			}
			
			if (dataDbConfig.getDatabaseImportJsonFile() != null) {
				try { // Load data from json file into neo4j
					writer.getGraph().io(IoCore.graphson()).readGraph(dataDbConfig.getDatabaseImportJsonFile().getAbsolutePath());
					System.out.println("Loaded graph : "+dataDbConfig.getDatabaseImportJsonFile().getAbsolutePath());
				} catch (Exception e) {
					e.printStackTrace();
					System.out.println("(Could not load json file !)");
				}
			}
			
			
			break;
			
		default :

			System.out.println("Database type not implemented !");
			break;
		}
		
		
    	// Instantiate registry database 
		switch (registryDbConfig.getDbType()) {
		
		case Neo4J :

			resourceManager = new RMEmbedded(
					registryDbConfig.getDatabaseDirectory().getAbsolutePath(),
	    			updatePolicy
	    			);
			if (resetRegistryDbAtStartup) {
				resourceManager.deleteDatabase();
				System.out.println("DELETING REGISTRY DATABASE");
				
				if (!resetDataDbAtStartup) {
					
					// Delete hit nodes in data database
					writer.deleteHitVertices();
				}
			}
			break;
					
			
		case GremlinServer :
			
			cluster = Cluster.open(makeBasicGremlinClusterConfig());
			//cluster = Cluster.open();
			//cluster = Cluster.build().port(8182).create();
			

			resourceManager = new RMServer(cluster, updatePolicy);
			if (resetRegistryDbAtStartup) {
				resourceManager.deleteDatabase();
				System.out.println("DELETING REGISTRY DATABASE");
				
				if (!resetDataDbAtStartup) {
				
					// Delete hit nodes in data database
					writer.deleteHitVertices();
				}
			}
			break;
		
			
		case TinkerGraph :
			
			TinkerGraph registryGraph = TinkerGraph.open();
			
			try {
				registryGraph.io(IoCore.graphson()).readGraph(registryDbConfig.getDatabaseImportJsonFile().getAbsolutePath());
				System.out.println("Loaded graph : "+registryDbConfig.getDatabaseImportJsonFile().getAbsolutePath());
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("(Could not load json file !)");
			}
			
			resourceManager = new RMEmbedded(
					registryGraph,
	    			updatePolicy
	    			);
			
			if (resetRegistryDbAtStartup) {
				resourceManager.deleteDatabase();
				System.out.println("CLEARING REGISTRY DATABASE");
				
				if (!resetDataDbAtStartup) {
					
					// Delete hit nodes in data database
					writer.deleteHitVertices();
				}
			}
			break;
			
		
		default :
			System.out.println("Database type not implemented !");
    		System.exit(0);
		}
				
		
		
		if (this.executionMode == ExecutionMode.DBSTART) {
			System.out.println("Close DB manually with closeDBConnections() !!");
			return;
		}
		
		if (this.executionMode == ExecutionMode.RUNDBPATCH) {
			System.out.println("Running database patch :");
			executePatch();
			System.out.println("Running database patch finished !");
			closeDBConnections();
			return;
		}
		
		
		/*if (this.executionMode == ExecutionMode.MAKERESULT) {
			makeResult();
			closeDBConnections();
			return;
		}*/
		
		// Only make json serialisation of Data database
		if (this.executionMode == ExecutionMode.EXPORTDDB) {
			try {
				
				writer.saveAsML(dataDbConfig.getDatabaseExportJsonFile().getAbsolutePath());
				//GraphTools.saveAsJSON(writer.getGraph(), dataDbConfig.getDatabaseExportJsonFile().getAbsolutePath());
				System.out.println("Exporting data database to file : "+dataDbConfig.getDatabaseExportJsonFile().getAbsolutePath()+" done !");
			} catch (Exception e) {
				System.out.println("DataDB export error :");
				e.printStackTrace();
			}
			closeDBConnections();
			return;
		}
		
		// Only make json serialisation of Data database
		if (this.executionMode == ExecutionMode.EXPORTRDB) {
			
			try {
				
				if (!resourceManager.getClass().equals(RMServer.class)) {	
				
					System.out.println("Exporting registry database -> "+registryDbConfig.getDatabaseExportJsonFile().getAbsolutePath());
					GraphTools.saveAsJSON(resourceManager.getGraph(), registryDbConfig.getDatabaseExportJsonFile().getAbsolutePath());
				} else {
					System.out.println("Sorry, can export local registryDB but not server registryDB !");
					System.out.println("To run a query please connect to gremlin server via :");
					System.out.println(":remote connect tinkerpop.server conf/remote.yaml session");
					System.out.println(":remote console)");
					System.out.println();
				}
				
				} catch (Exception e) {
					System.out.println("RegistryDB export error :");
					e.printStackTrace();
					}
			
				closeDBConnections();
				return;
		}
		
		 
		// Init language detector
		OptimaizeLanguageTools1.setFIDConfig(fidConfig);
		
		// Init OLiA model definitions
		try {
			modelDefinition = new ModelDefinition(fidConfig);
		} catch (Exception e) {
			System.out.println("Model definitions have an error,"
					+ " please check \n"+fidConfig.getString("OWL.ModelDefinitionFile")+" !");
			System.exit(0);
		}
		
		// Init blacklisted predicates
		resourceManager.initPredicates();
		
    	// Update ontology models
    	ontologyManager = new OntologyManager(this, new DownloadManager(
    			resourceManager, // global instance (Neo4J) or separate instance from above (GremlinServer)
    			new File (fidConfig.getString("RunParameter.downloadFolder")), updatePolicy, fidConfig, 60), fidConfig, modelDefinition);
    	
    	
    	// Init TemplateManager
    	templateManager = new TemplateManager(resourceManager);
    	
    	// Update ontology models (default true)
    	if(updateModels) {
    		
    			switch (this.executionMode) {
    			
    			case INIT :
    				ontologyManager.initModelGraph();
    				//ontologyManager.updateModelsOld(null);
    				break;
    				
    			case UPDATEMODELS :
    				ontologyManager.updateOliaModels();
    				break;
    			
    			// only active iff RunParameter.modelUpdateMode = auto
    			default :
    				//ontologyManager.updateAllModels();
    				break;
    			}
    		
    			
    			//loadXMLTemplates(templateManager);
    	} else {
    		if (this.executionMode == ExecutionMode.CLEAN) {
    			//loadXMLTemplates(templateManager);
    		}
    	}
    	
    	
    	// Breakpoint for -IN option (Initialization)
    	if (this.executionMode == ExecutionMode.UPDATEMODELS || 
    		this.executionMode == ExecutionMode.INIT) {
    		closeDBConnections();
			return;
		}
    	
    	// Break point for test
    	if (onlyUpdateModels) return;
    	
    	
    	loadXMLTemplates(templateManager);
    	    	
    	
    	// For test only !
    	// Use given resources (and not linghub resources) by setting defaultResources to s.t.
    	if (defaultResources != null && !defaultResources.isEmpty()) {
    		_resources.addAll(defaultResources);
    		
    	} else {
    				
    		/* Update local Linghub dump always but in case UpdatePolicy == UPDATE_ALL (use UPDATE_ALLL instead) */
    		
        	if (!UpdatePolicy.valueOf(fidConfig.getString("RunParameter.updatePolicy").toUpperCase()).
        			equals(UpdatePolicy.UPDATE_ALL)			 &&
        			fidConfig.getBoolean("Linghub.enabled")
        			)
        	{
	        	ResourceInfo linghubDumpOnline = new ResourceInfo(fidConfig.getString("Linghub.linghubDataDumpURL"),
	        			"http://linghub.org","http://linghub/dummy/dataset", ResourceFormat.LINGHUB);
	        	_resources.add(linghubDumpOnline);
	        	VifaWorker linghubUpdate = new VifaWorker(
	        			0,
	        			new ConcurrentLinkedQueue<ResourceInfo>(_resources),
	        			new GenericRdfFileHandler (writer, resourceManager),
	            		new ConllFileHandler(writer, resourceManager, fidConfig),
	        			fidConfig,
	        			new DownloadManager(
	        					resourceManager, // global instance (Neo4J) or separate instance from above (GremlinServer)
	        	    			new File (fidConfig.getString("RunParameter.downloadFolder")), registryDbConfig.getUpdatePolicy(), fidConfig, 10),
	        			null, 
	        			new GenericXMLFileHandler(writer, resourceManager, templateManager, fidConfig));
	        	
		        	System.out.println("Updating linghub dump !");
		        	linghubUpdate.run();
        	}	
        	
        	urlBroker = new UrlBroker(
        			resourceManager,
        			fidConfig);
        	
        	_resources.clear();
        	_resources = urlBroker.makeUrlPool();
    	}
    	
    	
    	if (this.executionMode == ExecutionMode.MAKERESULT) {
			makeResult();
			closeDBConnections();
			return;
		}
		

    	
    	// Breakpoint for -mf make-filelist option
    	if (this.executionMode == ExecutionMode.MAKEURLPOOL) {
    		System.out.println(".. done making filelist !");
    		closeDBConnections();
			return;
		}
    	
 
    	// Start activemq (see activemq package)	
    	
    	// *** Start main process ***
    	_queue = new ConcurrentLinkedQueue<ResourceInfo>(_resources);
    	_finishedQueue = new ConcurrentLinkedQueue<ResourceInfo>();

    	
    	
  	    int threadPoolSize;
  	    try {
  	    	threadPoolSize = Integer.parseInt(fidConfig.getString("RunParameter.threads"));
  	    }
  	    catch (Exception e) {
  	    	threadPoolSize = 1;
  	    }
  	    
  	  if (this.executionMode == ExecutionMode.SERVICE) {
  		  threadPoolSize = 0;
  	  }
  	    
		
  	    System.out.println("Starting application with "+threadPoolSize+" thread(s)");
  	    
		exs = Executors.newFixedThreadPool(threadPoolSize+1);
	    
	    workers = new Runnable[threadPoolSize+1];
	    workerRef = new Future<?>[threadPoolSize+1];
	    for (int i = 0; i < threadPoolSize+1; i++) {
	    	
	    	
	    	switch (registryDbConfig.getDbType()) {
	    	
	    		case TinkerGraph :
	    		case Neo4J :	// Share same resourceManager
	    		
	    		if (i != threadPoolSize) {
		        workers[i] = new VifaWorker(
		            		i, 
		            		_queue,
		            		new GenericRdfFileHandler (writer, resourceManager),
		            		new ConllFileHandler(writer, resourceManager, fidConfig),
		            		fidConfig,
		            		new DownloadManager(
		                			resourceManager,
		                			new File (fidConfig.getString("RunParameter.downloadFolder")),
		                			updatePolicy, fidConfig, 10),
		            		urlBroker, // TODO also seperate object per thread ???
		            		new GenericXMLFileHandler(writer, resourceManager, templateManager, fidConfig)
		            		);
	    		} else {
	    			
	    		// Start one worker with activemq for consuming web-service requests
	    		if (this.executionMode == ExecutionMode.SERVICE) {
	    		
	    		// Force update !
	    		updatePolicy = UpdatePolicy.UPDATE_ALL;
	    		
	    		workers[i] = new VifaWorkerMQ(
	            		i, 
	            		new Consumer(Executer.MQ_IN_1),
	            		new Producer(Executer.MQ_OUT_1),
	            		new GenericRdfFileHandler (writer, resourceManager),
	            		new ConllFileHandler(writer, resourceManager, fidConfig),
	            		fidConfig,
	            		new DownloadManager(
	                			resourceManager,
	                			new File (fidConfig.getString("RunParameter.downloadFolder")),
	                			updatePolicy, fidConfig, 10),
	            		new GenericXMLFileHandler(writer, resourceManager, templateManager, fidConfig)
	            		);
	    		}
	    		}
	    		
		        	break;
		        			

	    		case GremlinServer :	// Each worker gets its own resourceManager (client)
	    			 
	    			 ResourceManager resourceManager = new RMServer(cluster, updatePolicy);
	    			 
	    			 if (i != threadPoolSize) {
	    			 workers[i] = new VifaWorker(
			            		i, 
			            		_queue,
			            		new GenericRdfFileHandler (writer, resourceManager),
			            		new ConllFileHandler(writer, resourceManager, fidConfig),
			            		fidConfig,
			            		new DownloadManager(
			                			resourceManager,
			                			new File (fidConfig.getString("RunParameter.downloadFolder")),
			                			updatePolicy, fidConfig, 10),
			            		urlBroker, // TODO also separate object per thread ???
			            		new GenericXMLFileHandler(writer, resourceManager, templateManager, fidConfig)
			            		);
	    			} else {
	    			// Start one worker with activemq for consuming web-service requests
	    			if (this.executionMode == ExecutionMode.SERVICE) {	
	    		    	workers[i] = new VifaWorkerMQ(
    	            		i, 
    	            		new Consumer(Executer.MQ_IN_1),
    	            		new Producer(Executer.MQ_OUT_1),
    	            		new GenericRdfFileHandler (writer, resourceManager),
    	            		new ConllFileHandler(writer, resourceManager, fidConfig),
    	            		fidConfig,
    	            		new DownloadManager(
    	                			resourceManager,
    	                			new File (fidConfig.getString("RunParameter.downloadFolder")),
    	                			updatePolicy, fidConfig, 10),
    	            		new GenericXMLFileHandler(writer, resourceManager, templateManager, fidConfig)
    	            		);
	    			}
	    			}
	    			
	    			break;
	    			
	    			
	    		default :
	    			System.out.println("Database type not recognized - stopping !");
	    			System.exit(0);
	    			break;
	    			
	    	}
	    	
	    	
			// avoid starting non existing worker in case external queue is off
	    	// TODO more clear implementation for external queue !
	        if (i != threadPoolSize) {
	        	//exs.execute(workers[i]);
	        	workerRef[i] = exs.submit(workers[i]);
	        }
	        else {
	        	if (fidConfig.getBoolean("RunParameter.startExternalQueue")) {
	        		//exs.execute(workers[i]);
	        		workerRef[i] = exs.submit(workers[i]);
	        	}
	        }
	        }
	    
	    	if (this.executionMode != ExecutionMode.SERVICE) {
	    	
	    	exs.shutdown(); //exs.shutdownNow(); // will shutdown immediately
	         
	    	
	    	// Stopping service will terminate the application
	    	// This is required for test runs. The application will run 'forever' since
	    	// the service worker runs in an endless loop waiting for messages in the activemq
	    	if (isTestRun  && fidConfig.getBoolean("RunParameter.startExternalQueue")) {
	    		stopService();
	    	}
	        
	    	try {
	          // Wait for all workers to shutdown
	  		while (!exs.awaitTermination(20, TimeUnit.SECONDS)) {
	  				  System.out.println("VifaWorkers still running ...");
	  				}
	  		} catch (InterruptedException e) {
	  			e.printStackTrace();
	  		}
	    	
	    	closeDBConnections();
	    	
	    	}
	}
	
	private void initializeLanguageDetector() {


		File externalOptimaizeLanguageProfilesDir = null;
		ArrayList<File> optimaizeLanguageProfiles = new ArrayList<File>();
		File lexvoRdfFile=null;
		
		
		// Set directory that contains extra language profiles for Optimaize language detector
		if (fidConfig.containsKey("RunParameter.OptimaizeExtraProfilesDirectory")) {
			externalOptimaizeLanguageProfilesDir = new File(fidConfig.getString("RunParameter.OptimaizeExtraProfilesDirectory"));
		}
		
		
		if (externalOptimaizeLanguageProfilesDir == null || !externalOptimaizeLanguageProfilesDir.exists()) {
			
			// Use extra language profiles			
			if (this.executionMode != ExecutionMode.SERVICE) {
				
				HashSet<String> profileNames = new HashSet<String>(locateUtils.getJarFolderFileList("OptimaizeExtraProfiles/ok/"));
				
				System.out.println("");
				
				System.out.println("Using additional language profiles :");
				for (String profileName : profileNames) {
					System.out.print(profileName+" ");
					optimaizeLanguageProfiles.add(locateUtils.getLocalFile("/OptimaizeExtraProfiles/ok/"+profileName));
				}
			} else {
				
				List<File> profiles = locateUtils.getLocalDirectoryFileList("/OptimaizeExtraProfiles/ok/");
				
				System.out.println("Using additional language profiles :");
				for (File profile : profiles) {
					System.out.print(profile.getName()+" ");
					// Via fs
					optimaizeLanguageProfiles.add(profile);
					// Via resourceAsStream
					//optimaizeLanguageProfiles.add(locateUtils.getLocalFile("/OptimaizeExtraProfiles/ok/"+profile.getName()));
				}
				
			}
			System.out.println();
		
		} else {
			// Load extra language profiles from custom external directory
			System.out.println("Using custom additional profiles :");

			for (File file : externalOptimaizeLanguageProfilesDir.listFiles()){
				if (!file.isDirectory()) {
					System.out.println(file.getAbsolutePath());
					optimaizeLanguageProfiles.add(file);
				}
			}
		}
		
		
		// Set lexvo RDF file
		if (fidConfig.containsKey("RunParameter.LexvoRdfFile")) {
			lexvoRdfFile = new File(fidConfig.getString("RunParameter.LexvoRdfFile"));
		}
		
		// Use default lexvo file
		if (lexvoRdfFile == null || !lexvoRdfFile.exists()) {
			lexvoRdfFile = locateUtils.getLocalFile("/owl/lexvo/lexvo_2013-02-09.rdf.gz");
		}
		
		
		// Init languages
		OptimaizeLanguageTools1.initLanguageDetector(fidConfig, optimaizeLanguageProfiles, lexvoRdfFile);
	}


	/**
	 * Run database patch
	 */
	private void executePatch() {
		
		// DON'T !!!
		// executer.setUpdatePolicy(UpdatePolicy.UPDATE_ALL);
		// executer.setExecutionMode(ExecutionMode.DBSTART);
		// executer.run();
		// DON'T !!!
		
		
		// Archived patches (do not run again)
		// 1. Patches.addRdfProperty2ModelsTest (success)
		// 2. Patches.addFileSizePropertyTest (success)
		// 3. Patches.addMetadataSubjectPropertyTest (success)
		// 4. Patches.deleteNoThingClasses (success)
		// 5. Patches.addBLLModelTest (success)
		// 6. Patches.addRdfPropertyUbTitle2MetadataTest (success)
		// 7. Patches.fixFileRelPathFileNameFileAbsPathTest (success)
		// 8. Patches.deleteUnprocessedFiles (success)
		// 9. Patches.deleteResourceWithOnlyUnprocessedFiles (success)
		// 10. Patches.changeFormatXML2CONLL (success)
		// 11. UpdateNamespacesPatch.updateModelNameSpaces (success)
		// 12. addResourceETagPropertyTest (success)
		// 13. updateUnitTokenCounts (success)
		// 14. removeCorruptTokens (success)
		// 15. deleteDuplicateClasses (success)
		// 16. addModelLangaugeEdgeDateProperty (success)
		// 17. addLanguageModelUpdateTextProperty (success)
	}
	
	

	public static Configuration makeBasicGremlinClusterConfig() {
        final Configuration conf = new BaseConfiguration();
        conf.setProperty("hosts", Arrays.asList("127.0.0.1"));
        conf.setProperty("connectionPool.maxContentLength", 8000000); // important !
        return conf;
    }
	
    public static Configuration makeGremlinClusterConfig() {
        final Configuration conf = new BaseConfiguration();
        conf.setProperty("port", 8182);
        conf.setProperty("nioPoolSize", 16);
        conf.setProperty("workerPoolSize", 32);
        //conf.setProperty("username", "user1");
        //conf.setProperty("password", "password1");
        //conf.setProperty("jaasEntry", "JaasIt");
        //conf.setProperty("protocol", "protocol0");
        conf.setProperty("hosts", Arrays.asList("127.0.0.1"));
        //conf.setProperty("serializer.className", "my.serializers.MySerializer");
        //conf.setProperty("serializer.config.any", "thing");
        conf.setProperty("connectionPool.enableSsl", false);
        //conf.setProperty("connectionPool.keyCertChainFile", "X.509");
        //conf.setProperty("connectionPool.keyFile", "PKCS#8");
        //conf.setProperty("connectionPool.keyPassword", "password1");
        //conf.setProperty("connectionPool.trustCertChainFile", "pem");
        conf.setProperty("connectionPool.minSize", 100);
        conf.setProperty("connectionPool.maxSize", 200);
        conf.setProperty("connectionPool.minSimultaneousUsagePerConnection", 300);
        conf.setProperty("connectionPool.maxSimultaneousUsagePerConnection", 400);
        conf.setProperty("connectionPool.maxInProcessPerConnection", 600);
        conf.setProperty("connectionPool.minInProcessPerConnection", 500);
        conf.setProperty("connectionPool.maxWaitForConnection", 700);
        conf.setProperty("connectionPool.maxContentLength", 8000000);
        conf.setProperty("connectionPool.reconnectInterval", 900);
        conf.setProperty("connectionPool.resultIterationBatchSize", 1100);
        //conf.setProperty("connectionPool.channelizer", "channelizer0");

        return conf;
    }
	
	
	public void closeDBConnections() {
		try {
			
			writer.getGraph().close();
			resourceManager.closeDb();
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public void skipMQJob() {
		
		System.out.println("stopMQJob");

		int mqWorkerId = workerRef.length-1;
		VifaWorkerMQ mqw = (VifaWorkerMQ) workers[mqWorkerId];
		
		// close url consumer queue in worker first !!! (otherwise queue will not reconnect)
		mqw.getResourceConsumer().close();
		
		// stop MQ worker thread
		boolean z = workerRef[mqWorkerId].cancel(true);
		
		// wait until worker is stopped
		while (!workerRef[mqWorkerId].isCancelled()) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		// restart MQ worker thread
		workerRef[mqWorkerId] = exs.submit(mqw);
	}
	
	public void stopMQWorker() {
		System.out.println("stopMQWorker");
		workerRef[workerRef.length-1].cancel(true);
	}
	
	
	public void stopService() {
		 // Stop activemq -> is needed to terminate web-service worker
        try {
  			activemqBroker.stop();
  			System.out.println("Activemq stop");
  		} catch (Exception e) {
  			e.printStackTrace();
  		}
		
	}
	
	public void stop() {
		
        try {
        	System.out.println("Stopping service");
        	exs.shutdown();
        	closeDBConnections();
        	
  		} catch (Exception e) {
  			e.printStackTrace();
  		}
        
        // will stop the webapp
        //System.exit(0);
		
	}
	
	
	/**
	 * Create RDF from results in database which is then used for the UB export (non - service mode)
	 */
	// TODO implementation after several changes not tested
	public void makeResult() {
		
		
		System.out.println("exportRDF");
		
		// I.   Get successfull resources
		ArrayList<ResourceInfo> rfl = getResourceManager().getSuccessFullResourcesRI();
	
		// II.  Add Linghub metadata		(if no manual metadata exists)
		if (fidConfig.getBoolean("Linghub.enabled")) {
			getUrlBroker().sparqlLinghubAttributes(true, rfl);
		}
		
		// IIa) Add Clarin metadata			(if no manual or Linghub metadata exists)
		PostgresManager mng = new PostgresManager (getFidConfig());
		mng.updateClarinMetadata(rfl);
		
		// III. Add match info to resources objects that have results
		getWriter().getQueries().getFileResults(rfl, getResourceManager());
		
		// IV. Add BLL results
    	String exportString = "";		
		
		//HashMap<String,HashSet<String>> bllModelMap = getWriter().getQueries().queryBll(bllMatrixParser);

		HashSet<String> doneResources = new HashSet<String>();
		
		Model rdfModel = RDFSerializer.serializeResourceInfo2RDFModelIntern(
				(ArrayList<ResourceInfo>) rfl,
				getWriter().getBllTools(),
				fidConfig,
				getModelDefinition()
				);
				

		//rdfModel = RDFSerializer.serializeResourceInfo2RDFModel((ArrayList<ResourceInfo>) rfl, null, null, null);
		System.out.println("RDF model built !");

		exportString = RDFSerializer.serializeModel(rdfModel, RDFFormat.TURTLE_PRETTY);
		
		//System.out.println(exportString); // testing
		System.out.println("RDF model serialized !");
		String rdfExportFile = fidConfig.getString("RunParameter.RdfExportFile");

		if (!exportString.isEmpty()) {
    		System.out.println("Saving RDF export to file "+rdfExportFile+" ...");
    		Utils.writeFile(new File(rdfExportFile), exportString);
    	}
		
    	/*
    	HashMap<String,HashSet<String>> bllModelMap = new HashMap<String,HashSet<String>>(); 
    	int counter = 1;
		int all = rfl.size();
		for (ResourceInfo resourceInfo : rfl) {
			if (!doneResources.contains(resourceInfo.getDataURL())) {
				doneResources.add(resourceInfo.getDataURL());
				System.out.println((counter++)+"/"+all+" querying BLL on "+resourceInfo.getDataURL());
				bllModelMap.putAll(getWriter().getQueries().queryBllNew(bllMatrixParser, resourceInfo));
			}
		}*/
	}
	
	

	public void setRegistryDbConfig(DatabaseConfiguration registryDbConfig_) {
		registryDbConfig = registryDbConfig_;
	}
	
	
	public void setDataDbConfig(DatabaseConfiguration dataDbConfig_) {
		dataDbConfig = dataDbConfig_;
	}
	
	
	public void setVifaConfig(XMLConfiguration vifaConfig) {
		fidConfig = vifaConfig;
	}
	
	public void setUpdatePolicy(UpdatePolicy updatePolicy_) {
		updatePolicy = updatePolicy_;
		fidConfig.setProperty("RunParameter.updatePolicy", updatePolicy.toString());
	}
	
	
	public void setResources(ResourceInfo testResource) {
		defaultResources.clear();
		defaultResources.add(testResource);
	}
	
	public void setResources(ArrayList<ResourceInfo> testResources) {
		defaultResources = testResources;
	}

	public static EmbeddedQuery getDataDBQueries() {
		return writer.getQueries();
	}
	
	
	public GWriter getWriter() {
		return writer;
	}
	
	public ResourceManager getResourceManager() {
		return resourceManager;
	}
	
	public Long getUncompressedFileSizeLimitDefault() {
		return this.uncompressedFileSizeLimitDefault;
	}

	public void setUncompressedFileSizeLimit(long limitInBytes) {
		fidConfig.setProperty("RunParameter.uncompressedFileSizeLimit", limitInBytes);
	}
	
	public Long getCompressedFileSizeLimitDefault() {
		return compressedFileSizeLimitDefault;
	}

	public void setCompressedFileSizeLimit(Long limitInBytes) {
		fidConfig.setProperty("RunParameter.compressedFileSizeLimit", limitInBytes);
	}

	public ExecutionMode getExecutionMode() {
		return executionMode;
	}

	public void setExecutionMode(ExecutionMode executionMode) {
		this.executionMode = executionMode;
	}

	public DatabaseConfiguration getDataDbConfig() {
		return dataDbConfig;
		
	}

	public static XMLConfiguration getFidConfig() {
		return fidConfig;
	}
	
	private void loadXMLTemplates(TemplateManager templateManager) {
		
		File templateFile = null;
		
		if (fidConfig.containsKey("RunParameter.XMLParserConfiguration.templateFolder")) {
			templateFile = new File(fidConfig.getString("RunParameter.XMLParserConfiguration.templateFolder"));
		}
		
		// Use default templates
		if (templateFile == null || !templateFile.exists()) {
			templateFile = locateUtils.getLocalFile("/templates.json");
		}
		
		templateManager.loadTemplatesToDatabase_(templateFile);
		System.out.println("Loaded templates from "+templateFile.getAbsolutePath());
	}
	
	
	private static void makeDatabaseConfiguration() {
		
		//*****************************
		//   DATABASE CONFIGURATION   *
		//*****************************
		
		// Registry database //
		switch (DBType.valueOf(fidConfig.getString("DatabaseConfigurations.RegistryDBType"))) {
		
		case GremlinServer :
			registryDbConfig = new DatabaseConfiguration (DBType.GremlinServer,null,null,null,null);
			break;
			
		case Neo4J :
			registryDbConfig = new DatabaseConfiguration (
					DBType.Neo4J,new File(fidConfig.getString("Databases.Registry.Neo4jDirectory")),null,null,null);
			break;
		
		case TinkerGraph :
			registryDbConfig = new DatabaseConfiguration (
					DBType.TinkerGraph,null,null,null,null);
			break;
			
		default :
			// Error type not recognized !
			break;
		}
		

		// Data database //
		switch (DBType.valueOf(fidConfig.getString("DatabaseConfigurations.DataDBType"))) {
		
		case GremlinServer :
			dataDbConfig = new DatabaseConfiguration (DBType.GremlinServer,null,null,null,null);
			break;
			
		case Neo4J :
			dataDbConfig = new DatabaseConfiguration (
					DBType.Neo4J,new File(fidConfig.getString("Databases.Data.Neo4jDirectory")),null,null,null);
			break;
		
		case TinkerGraph :
			dataDbConfig = new DatabaseConfiguration (
					DBType.TinkerGraph,null,null,null,null);
			break;
			
		default :
			// Error type not recognized !
			break;
		
		}
	}
	
	
	
	private static void enableUpdatePolicy() {
		
		if (registryDbConfig.getUpdatePolicy() != null) {
			updatePolicy = registryDbConfig.getUpdatePolicy();
		} else {
		updatePolicy = UpdatePolicy.valueOf(fidConfig.getString("RunParameter.updatePolicy"));
		}
	}
	

	public Runnable[] getWorkers() {
		return workers;
	}

	public Queue<ResourceInfo> get_queue() {
		return _queue;
	}

	public Queue<ResourceInfo> get_finishedQueue() {
		return _finishedQueue;
	}

	public Cluster getCluster() {
		return cluster;
	}

	public UrlBroker getUrlBroker() {
		return urlBroker;
	}


	public ModelDefinition getModelDefinition() {
		return this.modelDefinition;
	}
	
	
	
	
	
}
