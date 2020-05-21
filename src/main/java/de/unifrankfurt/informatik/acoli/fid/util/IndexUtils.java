package de.unifrankfurt.informatik.acoli.fid.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.jena.ext.com.google.common.io.Files;

import de.unifrankfurt.informatik.acoli.fid.types.ResourceFormat;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo;


/**
 * @author frank
 */

public class IndexUtils {
	
	public static final String FoundDocumentsInIndex = "000";
	public static final String NoDocumentsFoundInIndex = "888";
	public static final String ParseError = "666";
	public static final String FileTypeNotSupported = "777";
	
	// Encode file type endings with '.' and in lower case (upper case is tested automatically)
	public static final String [] sparqlEndpointType = {"sparql"};
	public static final String [] rdfFileType = {".nt",".ttl",".rdf",".owl",".nq",".n3",".ntriples"};
	public static final String [] conllFileType = {".conll", ".conllu", ".conllx"};
	public static final String [] tarFileType = {".tar", ".tar.gz", ".tgz", ".tar.bz2", ".tbz2", ".tbz", ".tar.xz", ".tar.lzma",".tgz"};
	public static final String [] gzipFileType = {".gz", ".gzip"};
	public static final String [] zip7zBz2RarArchiveFileType = {".zip", ".7z", ".bz2", ".rar"};
	public static final String [] htmlFileType = {".html",".htm",".xhtml"};
	public static final String [] jsonFileType = {".json"};
	public static final String [] phpFileType = {".php"};
	public static final String [] pdfFileType = {".pdf"};
	public static final String [] psFileType = {".ps"};
	public static final String [] xmlFileType = {".xml",".treex"};
	public static final String [] csvFileType = {".csv"};
	public static final String [] tsvFileType = {".tsv"};
	public static final String [] exelFileType = {".xls",".xlsx"};
	public static final String [] textFileType = {".txt",".doc",".docx",".odt"};
	public static final String [] graphicsFileType = {".jpg",".jpeg",".png",".tiff"};
	public static final String [] soundFileType = {".wav",".mp3",".ogg"};
	public static final String metaSharePrefix = "http://metashare.";
	public static final String metaShareDownloadMarker = "I agree to these licence terms and would like to download the resource.";
	public static final String [] blacklist = {
		"http://diglib.hab.de","http://paradisec.org.au/fieldnotes","dbpedia",
		"http://mlode.nlp2rdf.org/datasets/mlsa.nt.gz,http://gnoss.com/gnoss.owl,https://clarin-pl.eu/dspace/bitstream/handle/11321/39/czywieszki1.1.zip?sequence=1"
		};
	public static final String [] metadataBlacklist = {"http://linghub.lider-project.eu/clarin/Nederands_Instituut_voor_Beeld_en_Geluid_OAI_PMH_repository"};

	
	public static final String ERROR_UNCOMPRESSED_FILE_SIZE_LIMIT_EXCEEDED = "Resource exceeds FileSizeLimit";
	public static final String ERROR_COMPRESSED_FILE_SIZE_LIMIT_EXCEEDED ="Resource exceeds compressedFileSizeLimit";
	public static final String ERROR_DECOMPRESSION = "Decompression error";
	public static final String ERROR_FTP_SUPPORT = "FTP protocol not supported";
	public static final String ERROR_RESOURCE_UP_TO_DATE = "The resource is up-to-date";
	public static final String ERROR_UNKNOWN_HOST = "UnknownHostException";
	public static final String ERROR_HTTP_CONNECTION = "HttpHostConnectException";
	public static final String ERROR_TIMEOUT = "TimeoutException";
	public static final String ERROR_CONLL_INVALID = "Conll file format is invalid";
	public static final String ERROR_CONLL_FILE_TOO_SMALL = "Conll File too small";
	public static final String ERROR_OUT_OF_DISK_SPACE = "Available disk space not sufficient";
	public static final String ERROR_MAX_ARCHIVE_FILE_COUNT_EXCEEDED = "Archive file has more files than allowed";
	public static final String ERROR_FILE_TYPE_NOT_SUPPORTED = "The file type could not be handeled";
	public static final String ERROR_IN_RDF_VALIDATION = "The validation of the RDF did not succeed";


	static final String linghubResourceQueries = 
			
				"PREFIX dcat: <http://www.w3.org/ns/dcat#>"+
				"PREFIX dct: <http://purl.org/dc/terms/>"+
				"PREFIX dc: <http://purl.org/dc/elements/1.1/>"+
				"PREFIX metashare: <http://purl.org/ms-lod/MetaShare.ttl#>"+
				"PREFIX rdfs: <http://rdfs.org/ns/void#>"+

				"SELECT ?accessUrl ?distribution ?dataset WHERE {"+
				  "?dataset rdf:type rdfs:Dataset. "+
				  "?dataset dcat:distribution ?distribution."+
				  "?distribution dcat:accessURL ?accessUrl ."+
				"}"+

				"### Querystart ###"+
				"PREFIX dcat: <http://www.w3.org/ns/dcat#>"+
				"PREFIX dct: <http://purl.org/dc/terms/>"+
				"PREFIX dc: <http://purl.org/dc/elements/1.1/>"+
				"PREFIX metashare: <http://purl.org/ms-lod/MetaShare.ttl#>"+
				"SELECT ?dataset ?distribution ?accessUrl  WHERE {"+
				"  ?dataset rdf:type dcat:Dataset. "+
				"  ?dataset dcat:contactPoint/metashare:affiliation/metashare:communicationInfo/dcat:distribution ?distribution."+
				" ?distribution dcat:accessURL ?accessUrl ."+
				"}";
	
	static final String linghubMetadataQueries =
			
				"PREFIX dcat: <http://www.w3.org/ns/dcat#>"+
				"PREFIX dct: <http://purl.org/dc/terms/>"+
				"PREFIX dc: <http://purl.org/dc/elements/1.1/>"+
				"PREFIX metashare: <http://purl.org/ms-lod/MetaShare.ttl#>"+
				"PREFIX foaf: <http://xmlns.com/foaf/0.1/>"+

				"SELECT ?dataset ?title ?description ?language ?rights ?date ?creator ?contributor ?subject ?homepage WHERE {"+

				  "?dataset dcat:distribution ?z."+
				  "?z dcat:accessURL !ACCESSURL! ."+
				  
				  "OPTIONAL {?dataset dct:title ?title}."+
				  "OPTIONAL {?dataset dct:description ?description}."+
				  "OPTIONAL {?dataset dct:language ?language}."+
				  "OPTIONAL {?dataset dct:rights ?rights}."+
				  "OPTIONAL {?dataset dct:date ?date}."+
				  "OPTIONAL {?dataset dct:creator ?creator}."+
				  "OPTIONAL {?dataset dct:contributor ?contributor}."+
				  "OPTIONAL {?dataset dct:subject ?subject}."+
				  "OPTIONAL {?dataset foaf:homepage ?homepage}."+
				  
				  
				"}"+
				"### Querystart ###"+
				"PREFIX dcat: <http://www.w3.org/ns/dcat#>"+
				"PREFIX dct: <http://purl.org/dc/terms/>"+
				"PREFIX dc: <http://purl.org/dc/elements/1.1/>"+
				"PREFIX metashare: <http://purl.org/ms-lod/MetaShare.ttl#>"+
				"PREFIX foaf: <http://xmlns.com/foaf/0.1/>"+

				"SELECT ?title ?description ?language ?rights ?date ?creator ?contributor ?subject ?homepage WHERE {"+

				  "?v dcat:distribution ?z."+
				  "?z dcat:accessURL !ACCESSURL! ."+
				  
				  "OPTIONAL {?v dc:title ?title}."+
				  "OPTIONAL {?v dc:description ?description}."+
				  "OPTIONAL {?v dc:language ?language}."+
				  "OPTIONAL {?v dc:rights ?rights}."+
				  "OPTIONAL {?v dc:date ?date}."+
				  "OPTIONAL {?v dc:creator ?creator}."+
				  "OPTIONAL {?v dc:contributor ?contributor}."+
				  "OPTIONAL {?v dc:subject ?subject}."+
				  "OPTIONAL {?v foaf:homepage ?homepage}.";
			
			
	
	/**
	 * Configuration defaults for FidConfig.xml. Set a default value to null if the application
	 * cannot start if it is missing.
	 */
	static HashMap <String, Object> configDefaults = new HashMap <String, Object> () {
		private static final long serialVersionUID = 1L;
	{
				put("Databases.GremlinServer.conf", null);
				put("Databases.GremlinServer.home", null);
				put("Databases.Registry.Neo4jDirectory", "");
				put("Databases.Data.Neo4jDirectory", null);
				put("Databases.Blazegraph.loadProperties", "");
				put("Databases.Postgres.usePostgres", false);
				put("Databases.Postgres.keyFile", "");
				put("Databases.Postgres.remoteHost", "");
				put("Databases.Postgres.database", "");
				put("Databases.Postgres.databaseUser", "");
				put("Databases.Postgres.databasePassword", "");
				put("Databases.Postgres.sshUser", "");
				put("Databases.deleteRdfDataAfterIndex", false);
				put("Databases.retryUnsuccessfulRdfData", false);
				
				put("RunParameter.downloadFolder", null);
				put("RunParameter.htmlFolder", "");
				put("RunParameter.urlSeedFile","/tmp/urlSeedFile");
				put("RunParameter.urlPoolFile","/tmp/urlpool");
				put("RunParameter.urlFilter","CONLL,RDF,ARCHIVE");
				put("RunParameter.updatePolicy", "UPDATE_ALL");
				put("RunParameter.threads", 1);
				put("RunParameter.decompressionUtility","7z");
				put("RunParameter.RdfPredicateFilterOn",false);
				put("RunParameter.ExitProcessDiskSpaceLimit",1000);			// in Megabytes (Exit process if free disk space below)
				put("RunParameter.MaxArchiveFileCount",30000);				// Skip archive files which do contain more than MaxArchiveFileCount files
				put("RunParameter.compressedFileSizeLimit",2048576000); 	// in bytes (<!-- in bytes (1 GB = 1073741824 bytes)
				put("RunParameter.uncompressedFileSizeLimit",2048576000);	// in bytes 
				put("RunParameter.isoCodeMapDirectory",""); // TODO set priority
				put("RunParameter.XMLParserConfiguration.matchingMeasurement","RECALL");
				put("RunParameter.XMLParserConfiguration.sampleSentenceSize",10);
				put("RunParameter.startExternalQueue",true);
				put("RunParameter.OptimaizeExtraProfilesDirectory","");
				put("RunParameter.OptimaizeAnnotationModelsProfilesDirectory","");
				put("RunParameter.LexvoRdfFile","");
				put("RunParameter.RdfExportFile", "/tmp/FidExport.rdf");
				put("RunParameter.JsonExportFile", "/tmp/JsonExport.json");
				put("RunParameter.AnnohubRelease", "/tmp/AnnoHubDataset.rdf");
				put("RunParameter.RdfPredicateFilterOn", false);
				put("RunParameter.useBllOntologiesFromSVN", false);
				put("RunParameter.BLLOntologiesDirectory", "");
				put("RunParameter.convert2RdfXmlScript","/bash/convert2RdfXml");
				put("RunParameter.debugOutput", true);
				put("RunParameter.guiPropertiesFile", "");
				
				put("Linghub.linghubDataDumpURL","http://linghub.org/linghub.nt.gz");
				put("Linghub.linghubQueries.resourceQueries",linghubResourceQueries);
				put("Linghub.linghubQueries.metadataQueries",linghubMetadataQueries);
				put("Linghub.statusCodeFilter","");				// deprecated ??
				put("Linghub.useQueries", false);
				put("Linghub.enabled", false);
				
				put("OWL.BLL.ModelDefinitionsFile","");
				put("OWL.BLL.BllOntology","https://valian.uni-frankfurt.de/svn/repository/intern/Virtuelle_Fachbibliothek/UB/OWL/BLLThesaurus/bll-ontology.rdf");
				put("OWL.BLL.BllLink","https://valian.uni-frankfurt.de/svn/repository/intern/Virtuelle_Fachbibliothek/UB/OWL/BLLThesaurus/bll-link.rdf");
				put("OWL.BLL.BllLanguageLink","https://valian.uni-frankfurt.de/svn/repository/intern/Virtuelle_Fachbibliothek/UB/OWL/BLLThesaurus/bll-language-link.ttl");
				put("OWL.modelUpdateMode", "manual");
				put("OWL.modelUpdateHitDeletePolicy", "manual");
				 
				// Sampling parameter    (for each folder of an language resource archive)
				// maxSamples          : (set -1 for unlimited samples) Maximum number of samples to be taken (from all folders)
				// thresholdForGood    : Stop parsing more files after thresholdForGood files have been parsed successfully
				// thresholdForBad     : Stop parsing more files after thresholdForGood files have been parsed unsuccessfully
				// activationThreshold : If the file count is smaller than the activationThreshold all files will be parsed
				
				put("Sampling.Rdf.maxSamples",1000);
				put("Sampling.Rdf.activationThreshold",100);
				put("Sampling.Rdf.thresholdForGood",200);
				put("Sampling.Rdf.thresholdForBad",10);
				put("Sampling.Xml.maxSamples",15);
				put("Sampling.Xml.activationThreshold",40);
				put("Sampling.Xml.thresholdForGood",3);
				put("Sampling.Xml.thresholdForBad",2);
				put("Sampling.Conll.maxSamples",15);
				put("Sampling.Conll.activationThreshold",20);
				put("Sampling.Conll.thresholdForGood",3);
				put("Sampling.Conll.thresholdForBad",3);
				
				put("Processing.ConllParser.conllFileMinLineCount",10);
				put("Processing.ConllParser.conllFileMaxLineCount",-1);	// -1 = unlimited
				put("Processing.ConllParser.maxSampleSentenceSize",100);
				put("Processing.GenericXmlFileHandler.xmlValueSampleCount",10);
				put("Processing.GenericXmlFileHandler.makeConllSentenceCount", 15000);
				put("Processing.XMLAttributeEvaluator.processDuplicates",false);
				put("Processing.ModelEvaluator.autoDeleteConllModelsWithTrivialResults",false);
				
				put("Clarin.clarinQueries","SELECT title, description, resource_type, date, author, licence, publisher, language from metadata where link = 'ACCESSURL';");
				
				// legacy options
				put("SearchEngine.SERVICE_URI","http://localhost:3030/ds/data");
				put("SearchEngine.PATH-SearchTerms","/home/vifa/VifaRun/searchEngine/searchTerms.ttl");
				put("SearchEngine.PATH-SearchTermsConcise","/home/vifa/VifaRun/searchEngine/searchterms_concise.ttl");
				put("SearchEngine.PATH-OLiA-TDB","/home/vifa/olia-tdb/");
				put("SearchEngine.PATH-SearchEngine-TDB","/home/vifa/se-tdb/");
				put("SearchEngine.PATH-CONLL","/home/vifa/conll/");
				put("SearchEngine.resultFile","/home/vifa/VifaRun/searchEngine/searchEngineResults.ttl");
				put("SearchEngine.xmlExportFile","/home/vifa/VifaRun/searchEngine/ub-export.xml");
				put("SearchEngine.xmlExportBase","/home/vifa/VifaRun/searchEngine/llod-mods-base.xml");
				put("SearchEngine.conciseSearchTerms",true);
				
				put("OliaSVN.path","/home/vifa/svn/vifa-owl/BLLThesaurus/bll-ontology.ttl");
				//put("Clarin.path","/home/vifa/svn/vifa-owl/BLLThesaurus/bll-link.rdf");
				//put("Clarin.path","/home/vifa/svn/olia-sf/trunk/owl/stable/bll-link.rdf");
				put("OliaSVN.path","/home/vifa/svn/olia-sf/trunk/owl/stable/");
				put("OliaSVN.path","/home/vifa/svn/olia-sf/trunk/owl/core/");
				put("OliaSVN.path","/home/vifa/svn/olia-sf/trunk/owl/experimental/univ_dep/");
				put("OliaSVN.path","/home/vifa/svn/olia-sf/trunk/owl/experimental/lexinfo/");
				put("OliaSVN.path","/home/vifa/svn/olia-sf/trunk/owl/experimental/gold/");
				put("OliaSVN.path","/home/vifa/svn/olia-sf/trunk/owl/experimental/dcr/6.owl");
				put("OliaSVN.path","/home/vifa/svn/olia-sf/trunk/owl/experimental/dcr/dcr-link.rdf");
				//put("OliaSVN.exception","multext_east");
				put("OliaSVN.exception","");
				put("OliaSVN.clarinQueries","old");
				put("OliaSVN.oliaregex","((http://purl.org/olia/(olia)|(system)).*)");
				put("OliaSVN.bllregex","((http://data.linguistik.de/bll).*)");
				
				put("CONLL.col",3);
				put("CONLL.col",4);
				put("CONLL.col",5);
				put("CONLL.col",8);
				put("CONLL.tag","conll:POS");
				put("CONLL.tag","conll:UPOS");
				put("CONLL.tag","conll:XPOS");
				put("CONLL.tag","conll:CPOS");
				put("CONLL.tag","conll:POSTAG");
				put("CONLL.tag","conll:UPOSTAG");
				put("CONLL.tag","conll:XPOSTAG");
				put("CONLL.tag","conll:CPOSTAG");
				put("CONLL.tag","conll:FEAT");
				put("CONLL.tag","conll:FEATS");
				put("CONLL.tag","conll:EDGE");
				put("CONLL.tag","conll:DEP<");
				put("CONLL.tag","conll:DEPS");
				put("CONLL.tag","conll:DEPREL");
				put("CONLL.tag","conll:DEPRELS");
				put("CONLL.tag","conll:PDEPREL");
				put("CONLL.tag","conll:PDEPRELS");
				put("CONLL.amURL","http://ud-pos-all.owl</amURL");
				put("CONLL.amURL","http://ud-dep-all.owl");
				
				put("PrefixURIs.bll-skos","http://data.linguistik.de/bll/bll-thesaurus#");
				put("PrefixURIs.bll-owl","http://data.linguistik.de/bll/bll-ontology#");
				put("PrefixURIs.bll-link","http://purl.org/olia/bll-link.rdf#");
				put("PrefixURIs.bll-tit","http://data.linguistik.de/records/bll/");
				put("PrefixURIs.bll-tit-link","http://data.linguistik.de/bll/bll-index#");
				put("PrefixURIs.olia","http://purl.org/olia/olia.owl#");
				put("PrefixURIs.olia-top","http://purl.org/olia/olia-top.owl#");
				put("PrefixURIs.olia-system","http://purl.org/olia/system.owl#");
				put("PrefixURIs.rdf","http://www.w3.org/1999/02/22-rdf-syntax-ns#");
				put("PrefixURIs.rdfs","http://www.w3.org/2000/01/rdf-schema#");
				put("PrefixURIs.skos","http://www.w3.org/2004/02/skos/core#");
				put("PrefixURIs.owl","http://www.w3.org/2002/07/owl#");
				put("PrefixURIs.conll","http://ufal.mff.cuni.cz/conll2009-st/task-description.html#");
				put("PrefixURIs.dcr","http://www.isocat.org/ns/dcr.rdf#");
				put("PrefixURIs.void","http://rdfs.org/ns/void#");
				put("PrefixURIs.xsd","http://www.w3.org/2001/XMLSchema#");
				put("PrefixURIs.afn","http://jena.hpl.hp.com/ARQ/function#");
	}};
	
	



	
	/**
	 * Filter resource by its URL extension and set the associated ResourceFormat
	 * @param resourceMap
	 * @param suffixes Filter URLs by suffix
	 * @param rf set this ResourceFormat for found resources
	 * @return resources that match the given filter
	 */
	private static HashSet<ResourceInfo> filterResources(
		
		HashMap <String, ResourceInfo> resourceMap, String[] suffixes,
		ResourceFormat rf) {		
		
		HashSet <ResourceInfo> out = new HashSet <ResourceInfo>();
		HashSet <String> usedKeys = new HashSet <String> ();
		boolean ok;
		for (String url : resourceMap.keySet()) {
			for (String suffix : suffixes) {
				
				ok = false;
				
				if (!url.startsWith("file:")) {
				try {
					URI uri = URI.create(url);
					if ((uri.getPath() != null && uri.getPath().toLowerCase().endsWith(suffix)) 
					||	(uri.getQuery() != null && uri.getQuery().toLowerCase().endsWith(suffix))
					||  (uri.getFragment() != null && uri.getFragment().toLowerCase().endsWith(suffix)))
					{
						ok = true;
					}
					} catch (Exception e) {
						//System.out.println(url);
					}
				} 
				else {
					if (new File(url).getName().toLowerCase().endsWith(suffix)) {
						ok = true;
					}
				}
				if (ok) {
					ResourceInfo ri = resourceMap.get(url);
					ri.setResourceFormat(rf);
					out.add(ri);
					usedKeys.add(url);
				}
			}
		}
		
		// remove filtered resources from resource map
		for (String key : usedKeys) {
			resourceMap.remove(key);
		}

		return out;
	}
	
	
	
	/**
	 * Filter URLs which have one of the given suffixes
	 * @param Set with URLs
	 * @param cut If true then remove found extension from URL
	 * @param ext Array with filter extensions
	 * @return URLs that have ext as suffix
	 */
	public static HashSet <String> filterSuffix (HashSet <String> set, String [] suffixes) {
		HashSet <String> out = new HashSet <String>();
		
		for (String url : set) {
			for (String suffix : suffixes) {
				
				try {
				if (URI.create(url).getPath().toLowerCase().endsWith(suffix) ||
					URI.create(url).getQuery().toLowerCase().endsWith(suffix)) {
						out.add(url);
				}
				} catch (Exception e) {
					//System.out.println(url);
				}
			}
		}
		
		return out;
	}
	
	
	/**
	 * Filter URLs which have one of the given prefixes
	 * @param Set with URLs
	 * @param cut If true then remove found extension from URL
	 * @param ext Array with filter extensions
	 * @return URLs that have ext as suffix
	 */
	public static HashSet <String> filterPrefix (HashSet <String> set, String [] prefix) {
		HashSet <String> out = new HashSet <String>();
		
		for (String url : set) {
			for (String suffix : prefix) {
				
				try {
				if (URI.create(url).getPath().toLowerCase().startsWith(suffix)) {
						out.add(url);
				}
				} catch (Exception e) {
					//System.out.println(url);
				}
			}
		}
		
		return out;
	}
	
	

	public static boolean fileIsCompressed (String file, String decompressionUtility) {
		boolean hasArchiveExt = (!filterSuffix (
				new HashSet<String>(Arrays.asList(file)),
				(String []) ArrayUtils.addAll(ArrayUtils.addAll(zip7zBz2RarArchiveFileType,tarFileType),gzipFileType)).isEmpty());
		if (hasArchiveExt) {
			// filename has no archive format extension
		return true;
		} else {
			if(FilenameUtils.getExtension(file).isEmpty()) {
			// filename has no extension -> check file type with 7z (samples file)
			//return ScriptUtils.isArchive(file, decompressionUtility);
			return false;
		} else {
			// filename has other extension -> file has no archive format
			return false;
		}
		}
	}
	
	
	public static boolean fileHasExtension (String file, String [] extensions) {
		return (!filterSuffix (
				new HashSet<String>(Arrays.asList(file)),extensions).isEmpty());
	}
	
	
	public static boolean fileIsLoadable (String file, String [] extensions) {
		return (!filterSuffix (
				new HashSet<String>(Arrays.asList(file)),
				(String []) ArrayUtils.addAll(ArrayUtils.addAll(
				ArrayUtils.addAll(zip7zBz2RarArchiveFileType,tarFileType),gzipFileType),rdfFileType)).isEmpty());
	}
	
	
	
	
	/**
	 * Resource filter which uses two extension lists by testing any combination from them <p>
	 * (e.g. filename.nt.gz) where suffix = .gz && preSuffix = .nt
	 * @param resources
	 * @param suffix
	 * @param preSuffix 
	 * @param rf set this format in filtered resources
	 * @return resources that match the given filter
	 */
	public static HashSet <ResourceInfo> filter2 (HashMap <String, ResourceInfo> resources, String [] suffix, String [] preSuffix, ResourceFormat rf) {
		
		String [] combinedSuffix = new String [suffix.length * preSuffix.length];
		int i = 0;
		for (String psf : preSuffix) {
			for (String sf : suffix) {
				combinedSuffix[i] = psf+sf;
				i++;
			}
		}
		
		return filterResources(resources, combinedSuffix, rf);
	}
	
	
	/**
	 * String filter which uses two extension lists by testing any combination from them <p>
	 * (e.g. filename.nt.gz) where suffix = .gz && preSuffix = .nt
	 * @param set String set
	 * @param suffix Array with suffixes
	 * @param preSuffix Array with preSuffixes 
	 * @return
	 */
	public static HashSet <String> filter2 (HashSet <String> set, String [] suffix, String [] preSuffix) {
		
		String [] combinedSuffix = new String [suffix.length * preSuffix.length];
		int i = 0;
		for (String psf : preSuffix) {
			for (String sf : suffix) {
				combinedSuffix[i] = psf+sf;
				i++;
			}
		}
		
		return filterSuffix(set, combinedSuffix);
	}
	
	
	
	/**
	 * Function for filtering resources that are sparql endpoints
	 * @param resources
	 * @param rf set this format in filtered resources
	 * @return resources that match the given filter
	 */
	public static HashSet <ResourceInfo> filterSparql(HashMap <String,ResourceInfo> resources, ResourceFormat rf) {
		return filterResources (resources, sparqlEndpointType, rf);
	}
	
	
	/**
	 * Function for filtering URLs that show SPARQL endpoints
	 * @return Set with URLs that are sparql endpoints
	 */
	public static HashSet <String> filterSparql(HashSet <String> set) {
		return filterSuffix (set, sparqlEndpointType);
	}
	
	
	/**
	 * Function for filtering URLs that show gzip format
	 * @param resources
	 * @param rf set this format in filtered resources
	 * @return resources that match the given filter
	 */
	public static HashSet <ResourceInfo> filterGzip (HashMap <String, ResourceInfo> resources, ResourceFormat rf) {
		return filterResources (resources, gzipFileType, rf);
	}
	
	
	/**
	 * Function for filtering URLs that are in gzip format
	 * @param Set of file path
	 * @return List of gzipped resources
	 */
	public static HashSet <String> filterGzip (HashSet <String> set) {
		return filterSuffix (set, gzipFileType);
	}
	
	
	
	/**
	 * Function for filtering resources that are in tar or (gziped tar) format
	 * @param resources
	 * @param rf set this format in filtered resources
	 * @return resources that match the given filter
	 */
	public static HashSet <ResourceInfo> filterTarNgz (HashMap <String,ResourceInfo> resources, ResourceFormat rf) {
		return filterResources (resources, tarFileType, rf);
	}
	
	
	/**
	 * Function for filtering URLs that are in tar or (zipped tar) format
	 * @param Set of file path
	 * @return List resources in tar format
	 */
	public static HashSet <String> filterTarNgz (HashSet <String> set) {
		return filterSuffix (set, tarFileType);
	}
	
	
	
	/**
	 * Function for filtering URLs that show a archive format other than gzip or tar
	 * @param resources
	 * @param rf set this format in filtered resources
	 * @return resources that match the given filter
	 */
	public static HashSet <ResourceInfo> filterZip7zBz2RarArchive(HashMap <String,ResourceInfo> resources, ResourceFormat rf) {
		return filterResources (resources, zip7zBz2RarArchiveFileType, rf);
	}
	
	
	/**
	 * Function for filtering URLs that show a archive format other than gzip or tar
	 * @param URLs
	 * @return Set of resources in archive format other than gzip
	 */
	public static HashSet <String> filterZip7zBz2RarArchive(HashSet <String> set) {
		return filterSuffix (set, zip7zBz2RarArchiveFileType);
	}
	
	
	/**
	 * Function for filtering URLs that show a archive format
	 * @param URLs
	 * @return Set of resources which relate to archive files
	 */
	public static HashSet <String> filterArchive(HashSet <String> set, String decompressionUtility) {
		
		HashSet <String> result = new HashSet <String> ();
		for (String file : set) {
			if (fileIsCompressed(file, decompressionUtility)) {
				result.add(file);
			}
		}
		
		return result;
	}
	
	
	/**
	 * Function for filtering URLs that show a RDF format
	 * @return List of RDF resources
	 */
	public static HashSet <String> filterRdf(HashSet <String> set) {
		return filterSuffix (set, rdfFileType);
	}
	
	
	/**
	 * Function for filtering URLs that show a XML format
	 * @return List of RDF resources
	 */
	public static HashSet <String> filterXml(HashSet <String> set) {
		return filterSuffix (set, xmlFileType);
	}
	
	
	/**
	 * Function for filtering URLs that show a RDF format
	 * @param resources
	 * @param rf set this format in filtered resources
	 * @return resources that match the given filter
	 */
	public static HashSet <ResourceInfo> filterRDF(HashMap <String, ResourceInfo> resources, ResourceFormat rf) {
		return filterResources (resources, rdfFileType, rf);
	}
	
	
	/**
	 * Function for filtering URLs that show a CONLL format
	 * @param resources
	 * @param rf set this format in filtered resources
	 * @return resources that have conll format
	 */
	public static HashSet <ResourceInfo> filterCONLL(HashMap <String, ResourceInfo> resources, ResourceFormat rf) {
		return filterResources (resources, conllFileType, rf);
	}
	
	
	/**
	 * Function for filtering URLs that show a HTML format
	 * @param resourceMp
	 * @param rdf
	 */
	public static HashSet <ResourceInfo> filterHTML(HashMap <String, ResourceInfo> resources, ResourceFormat rf) {
		return filterResources (resources, htmlFileType, rf);
		
	}
	
	/**
	 * Function for filtering URLs that show a HTML format
	 * @param resourceMp
	 * @param assigned format to all filtered resources
	 */
	public static HashSet <ResourceInfo> filterJSON(HashMap <String, ResourceInfo> resources, ResourceFormat rf) {
		return filterResources (resources, jsonFileType, rf);
		
	}
	
	/**
	 * Function for filtering URLs that show a PDF format
	 * @param resourceMp
	 * @param assigned format to all filtered resources
	 */
	public static HashSet <ResourceInfo> filterPDF(HashMap <String, ResourceInfo> resources, ResourceFormat rf) {
		return filterResources (resources, pdfFileType, rf);
		
	}
	
	/**
	 * Function for filtering URLs that show a Postscript format
	 * @param resourceMp
	 * @param assigned format to all filtered resources
	 */
	public static HashSet <ResourceInfo> filterPostscript(HashMap <String, ResourceInfo> resources, ResourceFormat rf) {
		return filterResources (resources, psFileType, rf);
		
	}
	
	
	/**
	 * Function for filtering URLs that show a XML format
	 * @param resourceMp
	 * @param assigned format to all filtered resources
	 */
	public static HashSet <ResourceInfo> filterXML(HashMap <String, ResourceInfo> resources, ResourceFormat rf) {
		return filterResources (resources, xmlFileType, rf);
		
	}
	
	
	/**
	 * Function for filtering URLs that show an archive format
	 * @param resourceMp
	 * @param assigned format to all filtered resources
	 */
	public static HashSet <ResourceInfo> filterARCHIVE(HashMap <String, ResourceInfo> resources, ResourceFormat rf) {
		return filterResources (resources, (String[])ArrayUtils.addAll(zip7zBz2RarArchiveFileType,ArrayUtils.addAll(gzipFileType, tarFileType)), rf);
		
	}
	
	
	
	/**
	 * Function for filtering URLs that show a TEXT format
	 * @param resourceMp
	 * @param assigned format to all filtered resources
	 */
	public static HashSet <ResourceInfo> filterText(HashMap <String, ResourceInfo> resources, ResourceFormat rf) {
		return filterResources (resources, textFileType, rf);
		
	}
	
	/**
	 * Function for filtering URLs that show a CSV format
	 * @param resourceMp
	 * @param assigned format to all filtered resources
	 */
	public static HashSet <ResourceInfo> filterCSV(HashMap <String, ResourceInfo> resources, ResourceFormat rf) {
		return filterResources (resources, csvFileType, rf);
		
	}
	
	
	/**
	 * Function for filtering URLs that show a TSV format
	 * @param resourceMp
	 * @param assigned format to all filtered resources
	 */
	public static HashSet <ResourceInfo> filterTSV(HashMap <String, ResourceInfo> resources, ResourceFormat rf) {
		return filterResources (resources, tsvFileType, rf);
		
	}
	
	/**
	 * Function for filtering URLs that show Exel format
	 * @param resourceMp
	 * @param assigned format to all filtered resources
	 */
	public static HashSet <ResourceInfo> filterExel(HashMap <String, ResourceInfo> resources, ResourceFormat rf) {
		return filterResources (resources, exelFileType, rf);
		
	}
	
	/**
	 * Function for filtering URLs that show a graphics format
	 * @param resourceMp
	 * @param assigned format to all filtered resources
	 */
	public static HashSet <ResourceInfo> filterGraphics(HashMap <String, ResourceInfo> resources, ResourceFormat rf) {
		return filterResources (resources, graphicsFileType, rf);
		
	}
	
	/**
	 * Function for filtering URLs that show a sound format
	 * @param resourceMp
	 * @param assigned format to all filtered resources
	 */
	public static HashSet <ResourceInfo> filterSound(HashMap <String, ResourceInfo> resources, ResourceFormat rf) {
		return filterResources (resources, soundFileType, rf);
		
	}
	
	/**
	 * Recursively get files from root directory
	 * @param root Directory
	 * @return Set with files
	 */
	public static HashSet <String> listRecFilesInDir (File root) {
		
		if (!root.exists() || !root.isDirectory()) return null;
		
		HashSet <String> allFiles = new HashSet <String> ();
		Queue<File> dirs = new LinkedList<File>();
		dirs.add(root);
		
		while (!dirs.isEmpty()) {
		  for (File f : dirs.poll().listFiles()) {
		    if (f.isDirectory()) {
		      dirs.add(f);
		    } else if (f.isFile()) {
		      allFiles.add(f.getAbsolutePath());
		    }
		  }
		}
		
		return allFiles;
	}
	
	
	/**
	 * Convert a list of file path to a map where files are sorted by their parent folder
	 * @param listOfFilePath
	 * @return Map folderPath -> filePath
	 */
	public static HashMap <String, ArrayList<String>> convertFileList2FolderMap(HashSet<String> listOfFilePath) {
		
		HashMap <String, ArrayList<String>> result = new HashMap <String, ArrayList<String>>();
		String key;
		for (String path : listOfFilePath) {
			key = (new File(path)).getParent();
			if (!result.containsKey(key)) {
				ArrayList <String> filesInDir = new ArrayList<String>();
				filesInDir.add(path);
				result.put(key, filesInDir);
			} else {
				ArrayList <String> filesInDir = result.get(key);
				filesInDir.add(path);
				result.put(key, filesInDir);
			}
		}
		return result;
	}
	
	
	
	/**
	 * Verify configuration file for missing parameter (does not verify values though)
	 * @param config Configuration 
	 * @return parameter set is complete
	 */
	public static boolean checkConfigAndSetDefaultValues(XMLConfiguration config) {

		// print config default values in file
		printConfiguration(config);
		
		boolean complete = true;
		for (String param : configDefaults.keySet()) {
			
			if (!config.containsKey(param)) {
				
				// Try using the default parameter
				if (configDefaults.get(param) != null) {
					config.addProperty(param, configDefaults.get(param));
				} else {
					complete = false;
					System.out.println("Configuration error : parameter "+param+" is missing !");
				}
			}
		}
		
		
		if (!config.getBoolean("Linghub.enabled")) {
			config.setProperty("Linghub.useQueries", false);
		}
		
		// with default values
		// printConfiguration(config);
				
		return complete;
	}
	
	
	
	private static void printConfiguration(XMLConfiguration config) {
		
		System.out.println("*******************************");
		System.out.println("* Using configuration options *");
		System.out.println("*******************************");
		
		Iterator<String> iterator = config.getKeys();
		while (iterator.hasNext()) {
			String key = iterator.next();
			System.out.print("# "+key+" :");
			System.out.println(config.getProperty(key));
		}
	}


	
	/**
	 * URL validation using org.apache.commons.validator.Validator
	 * @param url
	 * @return
	 */
	public static boolean isValidURL(String url) {
		
		try {
		if (url.startsWith("file:")) return true;
		
		UrlValidator urlValidator = new UrlValidator(UrlValidator.ALLOW_ALL_SCHEMES + UrlValidator.ALLOW_LOCAL_URLS);
		if (urlValidator.isValid(url)) {
		   return true;
		} else {
		   return false;
		}
		} catch (Exception e){e.printStackTrace();}
		return false;
	}
	
	
	
	/**
	 * Check if an url uses the file protocol
	 * @param url
	 * @return true if protocol is file
	 */
	public static boolean urlHasFileProtocol(String urlString) {
		
		try {
			URL url = new URL(urlString);
			if (url.getProtocol().equals("file")) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e){}
		return false;
	}
	
	
	/**
	 * Convert /path or file:/path or file://path -> file:///path
	 * @param url
	 * @return
	 */
	public static String checkFileURL (String url) {
		
		if (url == null || url.isEmpty()) return null;
		
		if  (!url.startsWith("http://")
		&&	 !url.startsWith("https://")
		&&	 !url.startsWith("file://")
		&&	 !url.startsWith("ftp://")
		&&	 !url.startsWith("urn:")
		&&	 !url.startsWith("ssh://")) {
			
			try {
				//System.out.println("what "+url);
				url = "file://"+new URL (url).getPath();
			} catch (MalformedURLException e) {
				Utils.debug("checkFileURL ERROR "+ url+" !!!!!!!!!!!!!!!!!");
				// TODO Auto-generated catch block
				//e.printStackTrace();
				return null;
			}
		}
		
		return url;
	}

/**
 * Filter resource by Scheme.
 * @param resourceMap
 * @param schemes List of allowed schemes
 * @return Resources which have scheme
 */
public static HashSet <ResourceInfo> filterResourcesWithScheme(HashMap<String, ResourceInfo> resourceMap, String [] schemes) {
		
		HashSet <ResourceInfo> filteredResources = new HashSet <ResourceInfo> ();
		HashSet <String> usedKeys = new HashSet <String> ();
		
		for (ResourceInfo rsi : resourceMap.values()) {
			//String uriScheme = URI.create(new File(rsi.getDataURL()).getPath()).getScheme();
			for (String s : schemes) {
			if (rsi.getDataURL().startsWith(s+":")) {
				// fails with some URLs which start url: but from obviously different charset
				filteredResources.add(rsi);
				usedKeys.add(rsi.getDataURL());
				break;
				}
			}
		}
		// remove filtered resources from resource map
		for (String key : usedKeys) {
			resourceMap.remove(key);
			}
		
		return filteredResources;
	}


/**
 * Filter resources which do not have a extension in their URL and set the ResourceFormat to HTML
 * @param resourceMap
 * @param format ResourceFormat will be set for each filtered resource
 * @return Resources without extension
 */
public static HashSet <ResourceInfo> filterResourcesWithoutExtension(HashMap<String, ResourceInfo> resourceMap, ResourceFormat format) {
		
		HashSet <ResourceInfo> filteredResources = new HashSet <ResourceInfo> ();
		HashSet <String> usedKeys = new HashSet <String> ();
		
		for (ResourceInfo rsi : resourceMap.values()) {
			try {
			if (!new File(URI.create(rsi.getDataURL()).getPath()).getName().contains(".")) {
				rsi.setResourceFormat(format);
				filteredResources.add(rsi);
				usedKeys.add(rsi.getDataURL());
			}} catch (Exception e) {}
		}
		
		// remove filtered resources from resource map
		for (String key : usedKeys) {
			resourceMap.remove(key);
			}
		
		return filteredResources;
	}
	
	
	/**
	 * Filter resources by ResourceFormat
	 * @param resourceMap
	 * @return resources which have the given resource format
	 */
	public static HashSet <ResourceInfo> filterResourcesWithFormat(HashMap<String, ResourceInfo> resourceMap, ResourceFormat rf) {
		
		HashSet <ResourceInfo> filteredResources = new HashSet <ResourceInfo> ();
		HashSet <String> usedKeys = new HashSet <String> ();
		
		for (ResourceInfo rsi : resourceMap.values()) {
			if (rsi.getResourceFormat().equals(rf)) {
				filteredResources.add(rsi);
				usedKeys.add(rsi.getDataURL());
			}
		}
		
		// remove filtered resources from resource map
		for (String key : usedKeys) {
			resourceMap.remove(key);
			}
		
		return filteredResources;
	}
	
	
	
	/**
	 * Filter resources which belong to META-SHARE
	 * @param resourceMap
	 * @return META-SHARE resources
	 */
	public static HashSet <ResourceInfo> filterMetaShare(
			HashMap<String, ResourceInfo> resourceMap) {
			
		HashSet <ResourceInfo> metaShareResources = new HashSet <ResourceInfo> ();
		HashSet <String> usedKeys = new HashSet <String> ();
		
		for (ResourceInfo rsi : resourceMap.values()) {
			String url = rsi.getDataURL();
			if (url.startsWith(IndexUtils.metaSharePrefix)) {
				try {
					rsi.setResourceFormat(ResourceFormat.METASHARE);
					
					/*
					// Parse URL type
					if (url.contains("/browse/")) {
						/* change metashare URL to download page
						   Example : http://metashare.metanet4u.eu/repository/browse/
						   acopost-a-collection-of-pos-taggers/
						   acae1ab62f3e11e2a2aa782bcb074135cbaf365868fe4aecb947bcf617c8395b/
						   
						   newUrl = metaSharePrefix+"/download/"+acae1ab62f3e11e2a2aa782bcb074135cbaf365868fe4aecb947bcf617c8395b/
						   
						rsi.setDataURL(new URL (IndexUtils.metaSharePrefix+"/download/"+new File(url).getName()).toString());
					} else {
					if (url.contains("/download/")) {
					}
					}
					*/

					metaShareResources.add(rsi);
					usedKeys.add(rsi.getDataURL());
				} catch (Exception e) {e.printStackTrace();}
			}
		}
		
		// remove filtered resources from resource map
		for (String key : usedKeys) {
			resourceMap.remove(key);
			}
		
		return metaShareResources;
	}
	
	
	/**
	 * Function for filtering URLs that are in CoNLL format
	 * @return List of CoNLL resources
	 */
	public static HashSet <String> filterConll(HashSet <String> set) {
		return filterSuffix (set, conllFileType);
	}
	
	
	
	public static boolean unpackFile (File file, File downloadFolder, String decompressionUtility, XMLConfiguration config) {
  
		HashSet <String> compressedFiles = new HashSet <String> ();
		long fileCount = 0;
		
		try {
		// Copy a local file to the download folder
		if (!file.getParent().equals(downloadFolder.getAbsolutePath())) {
			System.out.println("Copy : "+file+ " -> "+new File (downloadFolder,file.getName()).getAbsolutePath());
			Files.copy(file, new File (downloadFolder,file.getName()));
			
			// initialize compressed file list
			compressedFiles.add(new File (downloadFolder,file.getName()).getAbsolutePath());
		} else {
			// initialize compressed file list
			compressedFiles.add(file.getAbsolutePath());
		}

		// Recursively expand compressed file
		int stopper = 0;
		while (!compressedFiles.isEmpty()) {
		
		for (String filePath : compressedFiles) {
			fileCount += ScriptUtils.unpack7z(filePath, decompressionUtility);
			if (fileCount > config.getLong("RunParameter.MaxArchiveFileCount")) {
				System.out.println(IndexUtils.ERROR_MAX_ARCHIVE_FILE_COUNT_EXCEEDED+ "- limit is "+config.getLong("RunParameter.MaxArchiveFileCount"));
				return false;
			}
			}
			
			// initialize next round of unpacking
			compressedFiles = IndexUtils.filterArchive(IndexUtils.listRecFilesInDir(downloadFolder), decompressionUtility);
			
			//for (String x : compressedFiles) 
			//	System.out.println(x);
			
			stopper ++;
			// anything wrong ?
			if (stopper > 20) break;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	
	/**
	 * Neu schreiben ohne Maps etc. !!!
	 * @param resourceInfo
	 * @return
	 */
	public static ResourceFormat determineResourceFormat(ResourceInfo resourceInfo) {
		
		HashMap <String, ResourceInfo> resource = new HashMap <String, ResourceInfo>();
		HashSet <ResourceInfo> result = new HashSet <ResourceInfo> ();
		resource.put(resourceInfo.getDataURL(), resourceInfo);
		
		// filter functions set ResourceFormat in resourceInfo accordingly
		result.addAll(filterCONLL(resource, ResourceFormat.CONLL));
		result.addAll(filterRDF(resource, ResourceFormat.RDF));
		result.addAll(filterXML(resource, ResourceFormat.XML));
		result.addAll(filterARCHIVE(resource, ResourceFormat.ARCHIVE));
		result.addAll(filterHTML(resource, ResourceFormat.HTML));
		
		if (result.iterator().hasNext()) {
			return result.iterator().next().getResourceFormat();
		} else {
			return ResourceFormat.UNKNOWN;
		}
	}
	
	
	/**
	 * Get the format of a file. Only recognizes conll, rdf, xml, tsv and csv formats, returns unknown
	 * otherwise !
	 * @param resourceInfo with FileInfo
	 * @return ResourceFormat
	 */
	// TODO add other file types
	public static ResourceFormat determineFileFormat(ResourceInfo resourceInfo) {
		
		HashMap <String, ResourceInfo> resource = new HashMap <String, ResourceInfo>();
		HashSet <ResourceInfo> result = new HashSet <ResourceInfo> ();
		resource.put("file:/"+resourceInfo.getFileInfo().getAbsFilePath(), resourceInfo);
		
		// filter functions set ResourceFormat in resourceInfo accordingly
		result.addAll(filterCONLL(resource, ResourceFormat.CONLL));
		result.addAll(filterRDF(resource, ResourceFormat.RDF));
		result.addAll(filterXML(resource, ResourceFormat.XML));
		result.addAll(filterTSV(resource, ResourceFormat.TSV));
		result.addAll(filterCSV(resource, ResourceFormat.CSV));
		//result.addAll(filterARCHIVE(resource, ResourceFormat.ARCHIVE));
		//result.addAll(filterHTML(resource, ResourceFormat.HTML));
		
		if (result.iterator().hasNext()) {
			return result.iterator().next().getResourceFormat();
		} else {
			return ResourceFormat.UNKNOWN;
		}
	}
	
	
	/**
	 * Retrieve lineCount lines from file
	 * @param resourceInfo
	 * @param lineCount
	 * @return
	 */
	public static String getFileSample(ResourceInfo resourceInfo, int lineCount) {
		
		String fileSample = "";
		final int preroll=0;
		try {

			BufferedReader br = new BufferedReader(new FileReader(resourceInfo.getFileInfo().getAbsFilePath()));
			String line;
			int lineCounter = 0;
		    while ((line = br.readLine()) != null && lineCounter++ < lineCount + preroll) {
		       if(lineCounter > preroll) {
		    	   fileSample += line+"\n";
		       }
		    }
		    br.close();
		    } catch (Exception e) {e.printStackTrace();}
		
		resourceInfo.getFileInfo().setSample(fileSample);
		return fileSample;
	}
	
	
	
	public static void main (String [] args) {
		HashSet <String> x = new HashSet <String>();
		x.add("https://clarin-pl.eu/dspace/bitstream/handle/11321/115/wyniki.csv.zip?sequence=7");
		System.out.println(filterZip7zBz2RarArchive(x).iterator().next().toString());
		
	}

}
