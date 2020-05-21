package de.unifrankfurt.informatik.acoli.fid.util;

import java.util.HashMap;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;

public class JenaUtils {
	
	
	/**
	 * Get the mapping of ISO639-3 codes to BLL concepts. Bll-concepts with no 639-3 associated code are omitted !
	 * @param bll-language-link file
	 * @return map
	 */
	public static HashMap<String,String> loadBllLanguageLinkMap(String bllLanguageLinkFile) {
		
		HashMap<String,String> bllLangMap = new HashMap<String,String>();
		
		Dataset dataset = DatasetFactory.createMem();
		// TDBFactory.createDataset(tripleStoreFile.getPath()); // persistent database
		
		// In-memory, transactional Dataset new in version 3.1 
		// (supports dataset.begin(ReadWrite.READ); dataset.end(); 
		
		System.out.println("Load BLL language linking");
		loadRdfFile2Mem(dataset, bllLanguageLinkFile,"bll");
		
		
		String query =
				"PREFIX lvont: <http://lexvo.org/ontology#> "
				+ "SELECT distinct ?P3 ?bllConcept WHERE {GRAPH ?g {"
				+ "?indiv a ?bllConcept ."
				+ "?indiv lvont:iso639P3Code ?P3.}}"; 
		
		ResultSet result = query(dataset, query);
		String bllConcept;
		while (result.hasNext()) {
    		QuerySolution x = result.next();
    		bllConcept = x.get("bllConcept").toString();
    		if (x.get("P3") != null) {
    			if (!bllConcept.endsWith("NamedIndividual")) {
        			//System.out.println(bllConcept+":"+x.get("P3"));
    				bllLangMap.put(x.get("P3").toString(),bllConcept);
    			}
    		}
    	}
		
		dataset.close();
		return bllLangMap;
	}
	
	
	
	public static ResultSet query(Dataset dataset, String queryString) {

		if (dataset.supportsTransactions()) {
			dataset.begin(ReadWrite.READ); 	
		}

		 try(QueryExecution qExec = QueryExecutionFactory.create(queryString, dataset)) {
		     ResultSet rs = qExec.execSelect();
		     return ResultSetFactory.copyResults(rs);
		 }
		 finally {
			 if (dataset.supportsTransactions()) {
				 dataset.end();
			 }
		 }
	}
	
	
	/**
	 * Load RDF file into memory database
	 * @param dataset
	 * @param rdfFileURL
	 * @param graphName
	 * @return success
	 */
	public static boolean loadRdfFile2Mem (Dataset dataset, String rdfFileURL, String graphName) {
		
	    long startTime = System.currentTimeMillis();
	    boolean writeError = false;
	    
	    if (dataset.supportsTransactions()) {
	    	dataset.begin(ReadWrite.READ);
	    }
	    try
	    {
	    	
		System.out.println( "Loading "+rdfFileURL);
	    System.out.println("into graph "+graphName+" ..." );
	    Model m = dataset.getNamedModel(graphName);
	    RDFDataMgr.read(m, rdfFileURL);
	    if (dataset.supportsTransactions()) {
	    	dataset.commit();
	    }
	    
	    } catch (Exception e) {
	    	writeError = true;
	    	e.printStackTrace();
	    }
	    finally
	    {	
	    	if (dataset.supportsTransactions()) {
	    		dataset.end();
	    	}
	    	////r.gc();
	    	//System.out.println(r.freeMemory());
	    	//to free memory call the r.gc(); method and the call freeMemory()
	    	if (writeError) {
	    		
	    	}
	    }
	    
	    long finishTime = System.currentTimeMillis() ;
	    long time = finishTime - startTime;
	    System.out.println( "Loading finished after " + time + "ms" );
		return writeError;
	}
	
	
}
