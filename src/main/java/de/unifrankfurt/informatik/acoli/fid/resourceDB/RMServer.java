package de.unifrankfurt.informatik.acoli.fid.resourceDB;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.http.HttpResponse;
import org.apache.tinkerpop.gremlin.driver.Client;
import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.google.gson.Gson;

import de.unifrankfurt.informatik.acoli.fid.exec.Executer;
import de.unifrankfurt.informatik.acoli.fid.gremlinQuery.EmbeddedQuery;
import de.unifrankfurt.informatik.acoli.fid.gremlinQuery.ServerQuery;
import de.unifrankfurt.informatik.acoli.fid.types.DetectionMethod;
import de.unifrankfurt.informatik.acoli.fid.types.DetectionSource;
import de.unifrankfurt.informatik.acoli.fid.types.FileFormat;
import de.unifrankfurt.informatik.acoli.fid.types.FileInfo;
import de.unifrankfurt.informatik.acoli.fid.types.LanguageMatch;
import de.unifrankfurt.informatik.acoli.fid.types.LinghubResource;
import de.unifrankfurt.informatik.acoli.fid.types.MetadataSource;
import de.unifrankfurt.informatik.acoli.fid.types.ModelMatch;
import de.unifrankfurt.informatik.acoli.fid.types.ModelType;
import de.unifrankfurt.informatik.acoli.fid.types.ParseResult;
import de.unifrankfurt.informatik.acoli.fid.types.ProcessState;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceFormat;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceState;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceType;
import de.unifrankfurt.informatik.acoli.fid.types.UpdatePolicy;
import de.unifrankfurt.informatik.acoli.fid.types.VocabularyMatch;
import de.unifrankfurt.informatik.acoli.fid.types.VocabularyType;
import de.unifrankfurt.informatik.acoli.fid.util.IndexUtils;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;
import de.unifrankfurt.informatik.acoli.fid.xml.Template;
import de.unifrankfurt.informatik.acoli.fid.xml.TemplateQuality;
import edu.emory.mathcs.backport.java.util.Arrays;


public class RMServer implements ResourceManager{
	
	
	Cluster cluster;
	ServerQuery queries;
	UpdateManager updateManager;

	public RMServer(Cluster cluster, UpdatePolicy updatePolicy) {
		
		this.cluster = cluster;
		this.queries = new ServerQuery(cluster);
		this.updateManager = new UpdateManager(this, updatePolicy);
	}
	
	
	@Override 
	public Cluster getCluster() {
		return this.cluster;
	}
	
	@Override
	public Graph getGraph(){
		return null;
	}
	

	public EmbeddedQuery getQueries(){
		return null;
	}
	
	@Override
	public ServerQuery getServerQueries() {
		return this.queries;
	}

	public Client getClient() {
		return this.queries.getClient();
	}
	
	/**
	 * Verify if a resource is new and create/update entry in the resource database
	 * @param resourceInfo 
	 * @param header Header info
	 * @return if true then download resource else skip the resource
	 */
	@Override
	public void registerResource(ResourceInfo resourceInfo, HttpResponse header) {
		
		resourceInfo.setHttpResponseValues(header); // !
		
		// Verify if entry for resource in resource db exists (is actual)
		ResourceState resourceState = updateManager.getResourceState (resourceInfo);
		resourceInfo.setResourceState(resourceState);
		
		Utils.debug(resourceState.toString());
		
  	    switch (resourceState) {
  	    		
  	    	case ResourceHasNotChanged :
  	    		Utils.debug("is unchanged ... nothing to do");
  	    		return;

  	    	case ResourceHasChanged :
  	    		
  	    		if (resourceInfo.getFileInfo().getResourceType() != ResourceType.ONTOLOGY) {
  	    			resetResource(resourceInfo.getDataURL(), false);
  	    		}
  	    		resourceInfo.setResource(getResource(resourceInfo.getDataURL()));
  	    		// restore linghubAttributes
  	    		getMetadataValues(resourceInfo);
  	    		updateResourceMetadata(resourceInfo);
  	    		updateResourceHeaderData(resourceInfo);
  	    		
  	    		return;
  	    
  	    	case ResourceNotInDB :
  	    		resourceInfo.setResource(addResource(resourceInfo));
  	    		return;
  	    		
  	    	default :
  	    		Utils.debug("ResourceState : "+resourceState+" not recognized !");
  	    		return;
  	    	}  	    
	}
	

	@Override
	public Vertex addResource(ResourceInfo resourceInfo) {
		
		// TODO
		if (resourceExists(resourceInfo.getDataURL())) {
			Utils.debug("Error addResource : trying to add existing resource "+resourceInfo.getDataURL());
			//return this.getResource(resourceInfo.getDataURL());
		}
		
		// Parse HTTP header
		int responseCode = resourceInfo.getHttpStatusCode();
		String lastModified = resourceInfo.getHttpLastModified();
		String contentType = resourceInfo.getHttpContentType();
		Long contentLength = resourceInfo.getHttpContentLength();
		String eTag = resourceInfo.getHttpETag();
		
		String dataUrl = resourceInfo.getDataURL();
		String metaDataUrl = resourceInfo.getMetaDataURL();
		String metaDataUrl2 = resourceInfo.getMetaDataURL2();
		String resourceFormat = resourceInfo.getResourceFormat().toString();
		String detectionType = resourceInfo.getDetectionMethod().toString();
		
		if(dataUrl == null) {
			Utils.debug("Cannot add resource without data url !");
			System.exit(0);
		}
		
		if(resourceFormat == null) {
			Utils.debug("Cannot add resource without resource format !");
			System.exit(0);
		};
		
		if(metaDataUrl == null) {metaDataUrl = "";};
		
		HashMap <String, Object> values = new HashMap <String, Object> ();
		//values.put("responseCode", responseCode);
		//values.put("contentLength", contentLength);
		// !!! addV only works with cmd-line gremlin-server !!!
		// !!! addVertex only works with from java started gremlin-server !!!
		
		// TODO do it this way g.addV('person').property('name','stephen')
		// since addV(prop/value pairs) deprecated !
		
		
		
		String addVertexQuery = "g.addV('"+ResourceVertex+"')"
				+ ".property('"+ResourceUrl+"','"+dataUrl+"')"
				+ ".property('"+ResourceMetaUrl+"','"+metaDataUrl+"')"
				+ ".property('"+ResourceMetaUrl2+"','"+metaDataUrl2+"')"
				+ ".property('"+Resource4mat+"','"+resourceFormat+"')"
				+ ".property('"+ResourceDetectionType+"','"+detectionType+"')"
				+ ".property('"+ResourceResponseCode+"',"+responseCode+")"
				+ ".property('"+ResourceContentType+"','"+contentType+"')"
				+ ".property('"+ResourceSize+"',"+contentLength+")"
				+ ".property('"+ResourceETag+"','"+eTag+"')"
				+ ".property('"+ResourceLastModified+"','"+lastModified+"')";

		
		// add resource vertex
		Vertex resourceVertex = queries.addVertex(addVertexQuery, values);
		
		// add meta-data
		addResourceMetadata(resourceInfo);

		return resourceVertex;	
	}
	
	@Override
	public Boolean updateResourceHeaderData(ResourceInfo resourceInfo) {
		
		// Check resource exists ?
		if (!resourceExists(resourceInfo.getDataURL())) return false;
		
		Utils.debug("updateResourceHeaderData");
		
		String query = makeResourceQuery(resourceInfo.getDataURL());
		HashMap <String, Object> values = new HashMap <String, Object> ();
				
		values.put("parameter", resourceInfo.getHttpResponseCode());
		queries.setProperty(query+".property('"+ResourceResponseCode+"',parameter)", values);
		
		values.put("parameter", resourceInfo.getHttpContentType());
		queries.setProperty(query+".property('"+ResourceContentType+"',parameter)", values);
		
		values.put("parameter", resourceInfo.getHttpContentLength());
		queries.setProperty(query+".property('"+ResourceSize+"',parameter)", values);
		
		values.put("parameter", resourceInfo.getHttpLastModified());
		queries.setProperty(query+".property('"+ResourceLastModified+"',parameter)", values);
		
		values.put("parameter", resourceInfo.getHttpETag());
		queries.setProperty(query+".property('"+ResourceETag+"',parameter)", values);
		
		return true;
	}
	
	
	@Override
	public Vertex addResourceMetadata(ResourceInfo resourceInfo) {
		
		// Check if resource metadata exists
		ArrayList<Vertex> existingMetadata = getResourceMetadata(resourceInfo.getDataURL());
		if (existingMetadata.size() > 0) {
			if (existingMetadata.size() == 1) {
				return existingMetadata.get(0);
			} else {
				Utils.debug("Error : resource "+resourceInfo.getDataURL()
						+"has multiple meta-data vertices ("+existingMetadata.size()+") !");
				return existingMetadata.get(0);
			}
		}
		
		LinghubResource mda = resourceInfo.getLinghubAttributes();
		HashMap <String, Object> values = new HashMap <String, Object> ();
		//values.put("responseCode", responseCode);
		// !!! addV only works with cmd-line gremlin-server !!!
		// !!! addVertex only works with from java started gremlin-server !!!
		
		// TODO do it this way g.addV('person').property('name','stephen')
		// since addV(prop/value pairs) deprecated !
		
		String addVertexQuery = "metadataVertex = g.addV('"+MetadataVertex+"')"
				+ ".property('"+MetaFormat+"','"+mda.getFormat()+"')"
				+ ".property('"+MetaType+"','"+mda.getType()+"')"
				+ ".property('"+MetaRights+"','"+mda.getRights()+"')"
				+ ".property('"+MetaPublisher+"','"+mda.getPublisher()+"')"
				+ ".property('"+MetaTitle+"','"+mda.getTitle()+"')"
				+ ".property('"+MetaUbTitle+"','"+mda.getUbTitle()+"')"
				+ ".property('"+MetaDescription+"','"+mda.getDescription()+"')"
				+ ".property('"+MetaCreator+"','"+mda.getCreator()+"')"
				+ ".property('"+MetaContributor+"','"+mda.getContributor()+"')"
				+ ".property('"+MetaYear+"','"+mda.getYear()+"')"
				+ ".property('"+MetaContact+"','"+mda.getEmailContact()+"')"
				+ ".property('"+MetaWebpage+"','"+mda.getWebpage()+"')"
				+ ".property('"+MetaDatasource+"','"+mda.getMetadataSource().name()+"')"
				+ ".property('"+MetaSubject+"','"+mda.getKeywords()+"')"
				+ ".property('"+MetaDate+"',"+mda.getDate().getTime()+")"
				+ ".property('"+MetaDcLanguages+"','"+mda.getDcLanguageString()+"')"
				+ ".property('"+MetaDctLanguages+"','"+mda.getDctLanguageString()+"')"
				+ ".property('"+MetaLocation+"','"+mda.getLocation()+"').next()";

		String resourceQuery = "resourceVertex = "+makeResourceQuery(resourceInfo.getDataURL())+".next()";
		String addEdgeQuery = "resourceVertex.addEdge('"+MetadataEdge+"',metadataVertex)";
		String addMetadataQuery = resourceQuery+";"+addVertexQuery+";"+addEdgeQuery;
		Utils.debug(addMetadataQuery);
		queries.genericQuery(addMetadataQuery, values);
		
		
		try {
			return getResourceMetadata(resourceInfo.getDataURL()).get(0);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	
	@Override
	public void updateFileUnitInfo(ResourceInfo resourceInfo) {
		
		// Update totalTokenCount in model matchings
		HashMap<Integer, Integer> conllOrXmlTokenCounts;
		HashMap<String, Integer> rdfTokenCounts;
		
		// Update totalTokenCount in model matchings
		switch (resourceInfo.getFileInfo().getFileFormat()) {
		
		case CONLL :
		case XML :
			
			conllOrXmlTokenCounts = resourceInfo.getFileInfo().getConllColumnDifferentValues();
			for (ModelMatch mm : resourceInfo.getFileInfo().getModelMatchings()) {
				mm.setTotalTokenCount(conllOrXmlTokenCounts.get(mm.getConllColumn()));
			}
			break;
			
		case RDF :
			
			rdfTokenCounts = resourceInfo.getFileInfo().getRdfPropertyDifferentValues();
			for (ModelMatch mm : resourceInfo.getFileInfo().getModelMatchings()) {
				mm.setTotalTokenCount(rdfTokenCounts.get(mm.getRdfProperty()));
			}
			break;
			
		default :
			break;
		}
		
		
		// update database
		String addVertexQuery = "";
		FileFormat fileFormat;
		HashSet <String> doneRdfProperties = new HashSet <String>();
		HashSet <Integer> doneConllColumns = new HashSet <Integer>();
		HashSet <String> doneXmlAttributes = new HashSet <String>();

		HashMap <String, Object> values = new HashMap <String, Object> ();
		
		
		clearFileUnits(resourceInfo);
		
		for (ModelMatch mm : resourceInfo.getFileInfo().getModelMatchings()) {
			
			fileFormat = resourceInfo.getFileInfo().getFileFormat();
			if (!mm.getXmlAttribute().isEmpty()) fileFormat = FileFormat.XML; // legacy (if no extra XML format)
			
			switch (fileFormat) {
			
			case RDF :
				
				// add property only once
				if (doneRdfProperties.contains(mm.getRdfProperty())) {
					addVertexQuery="";
					break;
				} else {
					doneRdfProperties.add(mm.getRdfProperty());
				}
				
				addVertexQuery = "unitVertex = g.addV('"+UnitVertex+"')"
						+ ".property('"+UnitType+"','"+FileFormat.RDF.name()+"')"
						+ ".property('"+UnitRdfProperty+"','"+mm.getRdfProperty()+"')"
						+ ".property('"+UnitTokenCount+"',"+mm.getTotalTokenCount()+").next()";
				break;
				
			case CONLL :
				
				// add property only once
				if (doneConllColumns.contains(mm.getConllColumn())) {
					addVertexQuery="";
					break;
				} else {
					doneConllColumns.add(mm.getConllColumn());
				}
				
				addVertexQuery = "unitVertex = g.addV('"+UnitVertex+"')"
						+ ".property('"+UnitType+"','"+FileFormat.CONLL.name()+"')"
						+ ".property('"+UnitColumn+"',"+mm.getConllColumn()+")"
						+ ".property('"+UnitTokenCount+"',"+mm.getTotalTokenCount()+").next()";
				break;

			case XML :	// (not used by now)
				
				// add property only once
				if (doneXmlAttributes.contains(mm.getXmlAttribute())) {
					addVertexQuery="";
					break;
				} else {
					doneXmlAttributes.add(mm.getXmlAttribute());
				}
				
				addVertexQuery = "unitVertex = g.addV('"+UnitVertex+"')"
						+ ".property('"+UnitType+"','"+FileFormat.XML.name()+"')"
						+ ".property('"+UnitXmlAttribute+"','"+mm.getXmlAttribute()+"')"
						+ ".property('"+UnitColumn+"',"+mm.getConllColumn()+")"
						+ ".property('"+UnitTokenCount+"',"+mm.getTotalTokenCount()+").next()";
				break;

			default :
				
				addVertexQuery = "";
				break;
			}
			
			if (addVertexQuery.isEmpty()) continue;
			

		String resourceQuery = "fileVertex = "+makeFileQuery(resourceInfo)+".next()";
		String addEdgeQuery = "fileVertex.addEdge('"+UnitEdge+"',unitVertex)";
		String addUnitQuery = resourceQuery+";"+addVertexQuery+";"+addEdgeQuery;
		Utils.debug(addUnitQuery);
		queries.genericQuery(addUnitQuery, values);
		
		}
	}
	
	
	private void clearFileUnits(ResourceInfo resourceInfo) {
		
		String fileQuery = makeFileQuery(resourceInfo);
		String removefileUnitsQuery = fileQuery+".outE().hasLabel('"+UnitEdge+"').inV()";
		//Utils.debug(removeEdgeQuery);
		queries.genericDeleteQuery(removefileUnitsQuery);
	}
	
	
	@Override
	public HashMap<String, Integer> getRdfTokenCounts(ResourceInfo resourceInfo) {
		
		HashMap<String, Integer> rdfTokenCounts = new HashMap <String, Integer>();
		
		String fileQuery = makeFileQuery(resourceInfo);
		String query = fileQuery
			  + ".outE('"+ResourceManager.UnitEdge+"')"
			  + ".inV().has('"+ResourceManager.UnitType+"','"+FileFormat.RDF+"')"
			  + ".group().by('"+ResourceManager.UnitRdfProperty+"')"
			  + ".by('"+ResourceManager.UnitTokenCount+"')";
		

			
		HashMap<String, ArrayList<Integer>> result  = (HashMap<String, ArrayList<Integer>>) this.getServerQueries().genericMapQuery(query);
		if (rdfTokenCounts != null) {
			for (String key : result.keySet()) {
				rdfTokenCounts.put(key, result.get(key).get(0));
			}
			return rdfTokenCounts;
		}
		else return new HashMap<String, Integer>();
	}
	
	
	@Override
	public HashMap<Integer, Integer> getConllTokenCounts(ResourceInfo resourceInfo) {
		
		HashMap<Integer, Integer> conllTokenCounts = new HashMap<Integer, Integer>();
		
		String fileQuery = makeFileQuery(resourceInfo);
		String query = fileQuery
				  + ".outE('"+ResourceManager.UnitEdge+"')"
				  + ".inV().has('"+ResourceManager.UnitType+"','"+FileFormat.CONLL+"')"
				  + ".group().by('"+ResourceManager.UnitColumn+"')"
				  + ".by('"+ResourceManager.UnitTokenCount+"')";
			
		HashMap<Integer, ArrayList<Integer>> result  = (HashMap<Integer, ArrayList<Integer>>) this.getServerQueries().genericMapQuery(query);
		if (result != null) {
			for (int key : result.keySet()) {
				conllTokenCounts.put(key, result.get(key).get(0));
			}
			return conllTokenCounts;
		}
		else return new HashMap<Integer, Integer>();
	}
	
	
	@Override
	public HashMap<String, Integer> getXmlTokenCountsByAttribute(ResourceInfo resourceInfo) {
		
		HashMap<String, Integer> conllXmlTokenCounts = new HashMap<String, Integer>();
		
		String fileQuery = makeFileQuery(resourceInfo);
		String query = fileQuery
				  + ".outE('"+ResourceManager.UnitEdge+"')"
				  + ".inV().has('"+ResourceManager.UnitType+"','"+FileFormat.XML+"')"
				  + ".group().by('"+ResourceManager.UnitXmlAttribute+"')"
				  + ".by('"+ResourceManager.UnitTokenCount+"')";
			
		HashMap<String, ArrayList<Integer>> result  = (HashMap<String, ArrayList<Integer>>) this.getServerQueries().genericMapQuery(query);
		if (result != null) {
			for (String key : result.keySet()) {
				conllXmlTokenCounts.put(key, result.get(key).get(0));
			}
			return conllXmlTokenCounts;
		}
		else return new HashMap<String, Integer>();
	}
	
	
	@Override
	public HashMap<Integer, Integer> getXmlTokenCountsByColumn(ResourceInfo resourceInfo) {
		
		Utils.debug("getXmlTokenCountsByColumn");
		
		HashMap<Integer, Integer> conllXmlTokenCounts = new HashMap<Integer, Integer>();
		
		String fileQuery = makeFileQuery(resourceInfo);
		String query = fileQuery
				  + ".outE('"+ResourceManager.UnitEdge+"')"
				  + ".inV().has('"+ResourceManager.UnitType+"','"+FileFormat.XML+"')"
				  + ".group().by('"+ResourceManager.UnitColumn+"')"
				  + ".by('"+ResourceManager.UnitTokenCount+"')";
		
		Utils.debug("debug "+query);
			
		HashMap<Integer, ArrayList<Integer>> result  = (HashMap<Integer, ArrayList<Integer>>) this.getServerQueries().genericMapQuery(query);
		if (result != null) {
			for (int key : result.keySet()) {
				conllXmlTokenCounts.put(key, result.get(key).get(0));
			}
			return conllXmlTokenCounts;
		}
		else return new HashMap<Integer, Integer>();
	}
	
	
	@Override
	public HashMap<String, Integer> getXmlAttributes2ConllColumns(ResourceInfo resourceInfo) {
		
		HashMap<String, Integer> xmlAttributes2ConllColumns = new HashMap<String, Integer>();
		
		String fileQuery = makeFileQuery(resourceInfo);
		String query = fileQuery
				  + ".outE('"+ResourceManager.UnitEdge+"')"
				  + ".inV().has('"+ResourceManager.UnitType+"','"+FileFormat.RDF+"')"
				  + ".group().by('"+ResourceManager.UnitXmlAttribute+"')"
				  + ".by('"+ResourceManager.UnitColumn+"')";
			
		HashMap<String, ArrayList<Integer>> result  = (HashMap<String, ArrayList<Integer>>) this.getServerQueries().genericMapQuery(query);
		if (result != null) {
			for (String key : result.keySet()) {
				xmlAttributes2ConllColumns.put(key, result.get(key).get(0));
			}
			return xmlAttributes2ConllColumns;
		}
		else return new HashMap<String, Integer>();
	}
	
	
	@Override
	public HashMap<Integer, String> getConllColumns2XmlAttributes(ResourceInfo resourceInfo) {
		
		HashMap<Integer, String> conllColumns2XmlAttributes = new HashMap<Integer, String>();
		
		String fileQuery = makeFileQuery(resourceInfo);
		String query = fileQuery
				  + ".outE('"+ResourceManager.UnitEdge+"')"
				  + ".inV().has('"+ResourceManager.UnitType+"','"+FileFormat.RDF+"')"
				  + ".group().by('"+ResourceManager.UnitColumn+"')"
				  + ".by('"+ResourceManager.UnitXmlAttribute+"')";
			
		HashMap<Integer, ArrayList<String>> result  = (HashMap<Integer, ArrayList<String>>) this.getServerQueries().genericMapQuery(query);
		if (result != null) {
			for (int key : result.keySet()) {
				conllColumns2XmlAttributes.put(key, result.get(key).get(0));
			}
			return conllColumns2XmlAttributes;
		}
		else return new HashMap<Integer, String>();
	}
	
		
	
	@Override
	public Vertex getResource(String resourceIdentifier) {

		ArrayList <Vertex> resource = 
				queries.genericVertexQuery("g.V().hasLabel('"+ResourceVertex+"').has('"+ResourceUrl+"','"+resourceIdentifier+"')");
		if (resource.isEmpty()) return null;
		else return resource.get(0);
	}
	
	
	@Override
	public boolean resourceExists(String resourceIdentifier) {
		if (getResource(resourceIdentifier) != null) return true;
		else return false;
	}
	
	@Override
	/**
	 * Delete a resource together witch all contained files from the database 
	 * @param resourceIdentifier resourceID
	 */
	public void deleteResource(String resourceIdentifier) {
		
		// delete token edges, unit info and hits from model graph 
		resetResource(resourceIdentifier, true);
		
		// Delete all nodes on outgoing edges of resource (file vertices + metadata vertices)
		queries.genericDeleteQuery("g.V().hasLabel('"+ResourceVertex+"').has('"+ResourceUrl+"','"+resourceIdentifier+"').out()");
		// Delete resource
		queries.genericDeleteQuery("g.V().hasLabel('"+ResourceVertex+"').has('"+ResourceUrl+"','"+resourceIdentifier+"')");
	}
	
	/**
	 * Remove results for resource in model graph; remove token edges in registry; remove unit info in registry
	 */
	@Override
	public void resetResource(String resourceIdentifier, Boolean deleteUnitInfo) {
		
		// Delete token edges
		queries.genericDeleteQuery("g.V().hasLabel('"+ResourceVertex+"').has('"+ResourceUrl+"','"+resourceIdentifier+"').outE('"+ResourceManager.FileEdge+"').inV().outE('"+ResourceManager.TokenEdge+"')");
		
		if (deleteUnitInfo) {
		// Delete unit info
			queries.genericDeleteQuery("g.V().hasLabel('"+ResourceVertex+"').has('"+ResourceUrl+"','"+resourceIdentifier+"').outE('"+ResourceManager.FileEdge+"').inV().outE('"+ResourceManager.UnitEdge+"').inV()");
		}
		
		// Delete HIT and TAG nodes for the resource
		try {
		Executer.getDataDBQueries().deleteHitVertices(resourceIdentifier);
		Utils.debug("Deleted "+resourceIdentifier+" successfully from results database !");
		} catch (Exception e) {
			e.printStackTrace();
			Utils.debug("Ignore in test !");
		}
	}
	
	
	/**
	 * Remove results for a single file of a resource in the model graph; remove token edges in registry; remove unit info in registry
	 */
	@Override
	public void resetResourceFile(String resourceIdentifier, String relFilePath) {
		
		// Delete token edges of file
		queries.genericDeleteQuery("g.V().hasLabel('"+ResourceVertex+"').has('"+ResourceUrl+"','"+resourceIdentifier+"')"
		+ ".outE('"+ResourceManager.FileEdge+"').inV().has('"+ResourceManager.FilePathRel+"','"+relFilePath+"')"
		+ ".outE('"+ResourceManager.TokenEdge+"')");
		
		// Delete unit info of file
		queries.genericDeleteQuery("g.V().hasLabel('"+ResourceVertex+"').has('"+ResourceUrl+"','"+resourceIdentifier+"')"
		+ ".outE('"+ResourceManager.FileEdge+"').inV().has('"+ResourceManager.FilePathRel+"','"+relFilePath+"')"
		+ ".outE('"+ResourceManager.UnitEdge+"').inV()");
		
		// Delete HIT and TAG nodes for the resource
		try {
		Executer.getDataDBQueries().deleteHitVertices(resourceIdentifier, relFilePath);
		Utils.debug("Deleted "+resourceIdentifier+" -> "+relFilePath+" successfully from results database !");
		} catch (Exception e) {
			e.printStackTrace();
			Utils.debug("Ignore in test !");
		}
	}
	
	
	@Override
	public String getResourceMetaDataURL(String resourceIdentifier) {
		return getResource(resourceIdentifier).value(ResourceMetaUrl);
	}
	
	@Override
	public String getResourceMetaDataURL2(String resourceIdentifier) {
		return getResource(resourceIdentifier).value(ResourceMetaUrl2);
	}

	@Override
	public ResourceFormat getResourceFormat(String resourceIdentifier) {
		return ResourceFormat.valueOf(getResource(resourceIdentifier).value(Resource4mat));
		
	}
	
	
	@Override
	public int getResourceResponseCode(String resourceIdentifier) {
		return (Integer) getResource(resourceIdentifier).value(ResourceResponseCode);
	}
	
	@Override
	public String getResourceContentType(String resourceIdentifier) {
		return (String) getResource(resourceIdentifier).value(ResourceContentType);
	}
	
	@Override
	public String getResourceLastModified(String resourceIdentifier) {
		return getResource(resourceIdentifier).value(ResourceLastModified);
	}
	
	@Override
	public Long getResourceSize(String resourceIdentifier) {
		Long size = 0L;
		try {size = new Long(getResource(resourceIdentifier).value(ResourceSize).toString());}catch(Exception e) {
			e.printStackTrace();
		}
		return size;
	}
	
	
	
	@Override
	public boolean resourceHadResults(String resourceIdentifier) {
		boolean results = false;
		for (Vertex f : getResourceFiles(resourceIdentifier)) {
			if (f.value(FileStatusCode).equals(IndexUtils.FoundDocumentsInIndex))
				results = true;break;
		}
		return results;
	}

	
	@Override
	public Vertex addFile(ResourceInfo resourceInfo, FileFormat fileFormat) {
		
		resourceInfo.getFileInfo().setFileFormat(fileFormat);
		
		// Get file size
		long fileBytes = 0;
		try {
			fileBytes = FileUtils.sizeOf(resourceInfo.getFileInfo().getResourceFile());
		} catch (Exception e){
			e.printStackTrace();
			// Use size of resource instead !
			fileBytes = resourceInfo.getHttpContentLength();
		}
		
		resourceInfo.getFileInfo().setFileSizeInBytes(fileBytes);
		
		Utils.debug("file:"+resourceInfo.getFileInfo().getResourceFile().getAbsolutePath());
		Utils.debug("filebytes"+resourceInfo.getFileInfo().getFileSizeInBytes());
		Utils.debug("fileMbytes"+resourceInfo.getFileInfo().getFileSizeAsMBytes());
		
		
		// check if file exists and return vertex, identify file with FileInfo.getFileId()
		Vertex fileVertex = getResourceFile(resourceInfo);
		if (fileVertex != null) {
			Utils.debug("file exists");
			
			// update absFilePath (changes if the download folder has changed since first parse)
			setFileAbsPath(resourceInfo.getDataURL(), 
					resourceInfo.getFileInfo().getFileId(), resourceInfo.getFileInfo().getAbsFilePath());
				
			// update proccessing start ok
			updateFileProcessingStartDate(resourceInfo);
			
			// update file size
			setFileSize(resourceInfo.getResource(), fileVertex, resourceInfo.getFileInfo().getFileSizeInBytes());
			
			// update file format
			setFileFormat(resourceInfo.getResource(), fileVertex, fileFormat);
			
			// reread updated fileVertex
			fileVertex = getResourceFile(resourceInfo);
			
			// update resourceInfo object values
			getFileValues(resourceInfo, fileVertex);
			
			return fileVertex;
		}
		
		
		Utils.debug("Adding new file vertex "+resourceInfo.getFileInfo().getFileId());
		
		// Create file vertex
		HashMap <String, Object> values = new HashMap <String, Object> ();
		//values.put("fileId", resourceInfo.getFileInfo().getFileId());
		values.put("fileTripleCount", new Long(0));
		values.put("fileBytes", fileBytes);

		String resourceQuery = "resourceVertex = g.V()"
				+ ".hasLabel('"+ResourceVertex+"')"
				+ ".has('"+ResourceUrl+"','"+resourceInfo.getResource().value(ResourceUrl)+"').next()";
		
		
		String addVertexQuery = "fileVertex = g.addV('"+FileVertex+"')"
				//+ ".property('"+FileId+"',fileId)"
				+ ".property('"+FileName+"','"+resourceInfo.getFileInfo().getFileName()+"')"
				+ ".property('"+FilePathAbs+"','"+resourceInfo.getFileInfo().getAbsFilePath()+"')"
				+ ".property('"+FilePathRel+"','"+resourceInfo.getFileInfo().getRelFilePath()+"')"
				//+ ".property('"+FilePath+"','"+resourceInfo.getFileInfo().getResourceFile().getAbsolutePath()+"')"
				//+ ".property('"+FileBytes+"',fileBytes)"
				+ ".property('"+File4ormat+"','"+fileFormat.toString()+"')"
				+ ".property('"+FileStatusCode+"','"+IndexUtils.FoundDocumentsInIndex+"')"
				+ ".property('"+FileProcessState+"','"+resourceInfo.getFileInfo().getProcessState().name()+"')"
				+ ".property('"+FileTripleCount+"',fileTripleCount)"
				+ ".property('"+FileSizeInBytes+"',"+resourceInfo.getFileInfo().getFileSizeInBytes()+")"
				+ ".property('"+FileErrorCode+"','')"
				+ ".property('"+FileLanguageSample+"','')"
				+ ".property('"+FileComment+"','"+resourceInfo.getFileInfo().getComment()+"')"
				+ ".property('"+FileProcessingStartDate+"',"+resourceInfo.getFileInfo().getProcessingStartDate().getTime()+")"
				+ ".property('"+FileProcessingEndDate+"',"+resourceInfo.getFileInfo().getProcessingEndDate().getTime()+")"
				+ ".property('"+FileAcceptedDate+"',"+resourceInfo.getFileInfo().getAcceptedDateGetTime()+")"
				+ ".property('"+FileSample+"','')" 
				// sample throws error if text contains quotes - setting later (see below) works everytime !
				+ ".property('"+FileErrorMsg+"','').next()";
		
		
		String addEdgeQuery = "resourceVertex.addEdge('"+FileEdge+"',fileVertex)";
		String addFileQuery = resourceQuery+";"+addVertexQuery+";"+addEdgeQuery;
		queries.genericQuery(addFileQuery, values);
		// Cannot return fileVertex from complex addFileQuery (actually returns added Edge)
		// Instead get fileVertex from extra query !
		String fileQuery = makeFileQuery(resourceInfo.getDataURL(), resourceInfo.getFileInfo().getRelFilePath());
		
		ArrayList<Vertex> files = queries.genericVertexQuery(fileQuery);
		if (files.isEmpty()) return null;
		else {
			try {
				// sample text can throw error if contains quotes
				// TODO escape quote characters, etc.
				setFileSample(resourceInfo.getResource(), files.get(0), resourceInfo.getFileInfo().getSample());
			} catch (Exception e) {
				e.printStackTrace();
			}
			//resourceInfo.getFileInfo().setFileVertex(files.get(0));
			return files.get(0);
		}
		
	}
	
	@Override
	public ArrayList <Vertex> getResourceFiles(Vertex resource) {
		System.err.println(resource.values(ResourceUrl));
		return getResourceFiles((String) resource.value(ResourceUrl));
	}
	
	
	@Override
	public ArrayList <Vertex> getResourceFiles(String resourceIdentifier) {
		String query = "g.V().hasLabel('"+ResourceVertex+"').has('"+ResourceUrl+"','"+resourceIdentifier+"').outE('"+FileEdge+"').inV()";
		return queries.genericVertexQuery(query);
	}
	
	
	@Override
	public ArrayList <Vertex> getResourceMetadata(String resourceIdentifier) {
		String query = "g.V().hasLabel('"+ResourceVertex+"').has('"+ResourceUrl+"','"+resourceIdentifier+"').outE('"+MetadataEdge+"').inV()";
		return queries.genericVertexQuery(query);
	}
	
	
	@Override
	public ArrayList <Vertex> getResourceFilesWithHits(String resourceIdentifier) {
		String query = "g.V().hasLabel('"+ResourceVertex+"').has('"+ResourceUrl+"','"+resourceIdentifier+"').outE('"+FileEdge+"').inV()"
				+ ".has('"+FileStatusCode+"','"+IndexUtils.FoundDocumentsInIndex+"')";
		return queries.genericVertexQuery(query);
	}
	
	
	
	
	
	/**
	 * Get file vertex for a file that is contained in a resource
	 * @param resourceVertex
	 * @param fileIdentifier - is the filename in case of a single file and the relative file path otherwise (e.g. archive/file.txt, archive/subfolder/file.txt) for
	 *        a file which is part of an archive !)
	 * @return File vertex
	 */
	@Override
	public Vertex getResourceFile(Vertex resourceVertex, String fileIdentifier) {
		return getResourceFile((String) resourceVertex.value(ResourceUrl), fileIdentifier);
	}
	
	/**
	 * Get file vertex for a file that is contained in a resource
	 * @param resourceInfo object that contains resource and file information 
	 * @param fileIdentifier - is the filename in case of a single file and the relative file path otherwise (e.g. archive/file.txt, archive/subfolder/file.txt) for
	 *        a file which is part of an archive !)
	 * @return File vertex
	 */
	@Override
	public Vertex getResourceFile(ResourceInfo resourceInfo) {
		return getResourceFile(resourceInfo.getDataURL(), resourceInfo.getFileInfo().getRelFilePath());
		
	}
	
	/**
	 * Get file vertex for a file that is contained in a resource
	 * @param resourceIdentifier 
	 * @param fileIdentifier - is the filename in case of a single file and the relative file path otherwise (e.g. archive/file.txt, archive/subfolder/file.txt) for
	 *        a file which is part of an archive !)
	 * @return File vertex
	 */
	@Override
	public Vertex getResourceFile(String resourceIdentifier, String fileIdentifier) {
		
		String query = makeFileQuery(resourceIdentifier, fileIdentifier);
		Utils.debug("getResourceFile :"+query);

		ArrayList<Vertex> fileVertexList = queries.genericVertexQuery(query);
		if (!fileVertexList.isEmpty()) {
			Utils.debug(fileVertexList.size());
			return fileVertexList.get(0);
		} else {
			return null;
		}
	}
	
	
	@Override
	public String getFileName(Vertex resource, String fileIdentifier) {
		return getResourceFile(resource, fileIdentifier).value(FileName);
	}
	
	@Override
	public FileFormat getFileFormat(Vertex resource, String fileIdentifier) {
		return FileFormat.valueOf(getResourceFile(resource, fileIdentifier).value(File4ormat));
	}
	
	@Override
	public void setFileErrorCode(Vertex resource, Vertex file, String errorCode) {
		
		String query = makeFileQuery(resource, file);
		HashMap <String, Object> values = new HashMap <String, Object> ();
		values.put("errorCode", errorCode);
		queries.setProperty(query+".property('"+FileErrorCode+"',errorCode)", values);
	}
	
	@Override
	public String getFileErrorCode(Vertex resource, String fileIdentifier) {
		return getResourceFile(resource, fileIdentifier).value(FileErrorCode);
	}

	@Override
	public void setFileErrorMsg(Vertex resource, Vertex file, String errorMsg) {
		
		if (errorMsg == null) return;
		String query = makeFileQuery(resource, file);
		HashMap <String, Object> values = new HashMap <String, Object> ();
		values.put("errorMsg", errorMsg);
		queries.setProperty(query+".property('"+FileErrorMsg+"',errorMsg)", values);
	}
	
	@Override
	public String getFileErrorMsg(Vertex resource, String fileIdentifier) {
		return getResourceFile(resource, fileIdentifier).value(FileErrorMsg);
	}
	
	@Override
	public void setFileError(Vertex resource, Vertex file, String errorCode, String errorMsg) {
		setFileErrorMsg(resource, file, errorMsg);
		setFileErrorCode(resource,file, errorCode);
	}
	
	
	@Override
	public void setFileRelPath(String resourceIdentifier, String fileIdentifier, String newFileIdentifier) {
		String query = makeFileQuery(resourceIdentifier, fileIdentifier);
		HashMap <String, Object> values = new HashMap <String, Object> ();
		values.put("newFileIdentifier", newFileIdentifier);
		queries.setProperty(query+".property('"+ResourceManager.FilePathRel+"',newFileIdentifier)", values);
	}
	

	@Override
	public void setFileStatusCode(Vertex resource, Vertex file, String statusCode) {
		
		String query = makeFileQuery(resource, file);
		HashMap <String, Object> values = new HashMap <String, Object> ();
		values.put("statusCode", statusCode);
		queries.setProperty(query+".property('"+FileStatusCode+"',statusCode)", values);
	}
	
	@Override
	public String getFileStatusCode(Vertex resource, String fileIdentifier) {
		return getResourceFile(resource, fileIdentifier).value(FileStatusCode);
	}
	
	@Override
	public void setFileLanguageSample(Vertex resource, Vertex file, String textSample) {
		String query = makeFileQuery(resource, file);
		HashMap <String, Object> values = new HashMap <String, Object> ();
		values.put("textSample", textSample);
		queries.setProperty(query+".property('"+FileLanguageSample+"', textSample)", values);
	}

	@Override
	public String getFileLanguageSample(Vertex resource, String fileIdentifier) {
		return getResourceFile(resource, fileIdentifier).value(FileLanguageSample);
	}
	
	@Override
	public void setFileSample(Vertex resource, Vertex file, String textSample) {
		String query = makeFileQuery(resource, file);
		HashMap <String, Object> values = new HashMap <String, Object> ();
		values.put("textSample", textSample.replace("'", "")); // delete quote (will crash query otherwise !)
		queries.setProperty(query+".property('"+FileSample+"', textSample)", values);
	}

	@Override
	public String getFileSample(Vertex resource, String fileIdentifier) {
		return getResourceFile(resource, fileIdentifier).value(FileSample);
	}
	
	@Override
	public void setFileTripleCount(Vertex resource, Vertex file, long tripleCount) {
		
		String query = makeFileQuery(resource, file);
		HashMap <String, Object> values = new HashMap <String, Object> ();
		values.put("tripleCount", tripleCount);
		queries.setProperty(query+".property('"+FileTripleCount+"',tripleCount)", values);
	}
	
	
	@Override
	public void setFileSize (Vertex resource, Vertex file, long fileSizeInBytes) {
		
		String query = makeFileQuery(resource, file);
		HashMap <String, Object> values = new HashMap <String, Object> ();
		values.put("fileSizeInBytes", fileSizeInBytes);
		queries.setProperty(query+".property('"+FileSizeInBytes+"',fileSizeInBytes)", values);
	}
	
	
	@Override
	public void setFileFormat(Vertex resource, Vertex file,
			FileFormat fileFormat) {
		String query = makeFileQuery(resource, file);
		HashMap <String, Object> values = new HashMap <String, Object> ();
		values.put("fileFormat", fileFormat.toString());
		queries.setProperty(query+".property('"+File4ormat+"',fileFormat)", values);
	}
	
	
	@Override
	public Long getFileBytes(Vertex resource, String fileIdentifier) {
		return new Long(getResourceFile(resource, fileIdentifier).value(FileSizeInBytes));
	}
	
	@Override
	public Long getFileTripleCount(Vertex resource, String fileIdentifier) {
		return new Long(getResourceFile(resource, fileIdentifier).value(FileTripleCount));
	}

	@Override
	public ArrayList <Vertex> getDoneResources() {
		return queries.genericVertexQuery("g.V().hasLabel('"+ResourceVertex+"')"
				+ ".not(has('"+ResourceManager.Resource4mat+"','"+ResourceFormat.ONTOLOGY.name()+"'))"
				+ ".not(has('"+ResourceManager.Resource4mat+"','"+ResourceFormat.LINGHUB.name()+"'))"
				+ ".dedup()");
		}
	
	@Override
	public int getDoneResourceCount() {
		return getDoneResources().size();
	}
	
	@Override
	public ArrayList <Vertex> getDoneFileResources() {
		return queries.genericVertexQuery("g.V()"
				+ ".hasLabel('"+ResourceVertex+"')"
				+ ".not(has('"+ResourceManager.Resource4mat+"','"+ResourceFormat.ONTOLOGY.name()+"'))"
				+ ".not(has('"+ResourceManager.Resource4mat+"','"+ResourceFormat.LINGHUB.name()+"'))"
				+ ".outE('"+ResourceManager.FileEdge+"')"
				+ ".inV().dedup()");
	}
	
	@Override
	public int getDoneFileResourceCount() {
		return getDoneFileResources().size();
	}
	
	
	/**
	 * Only XML files without any result have the FileFormat XML right now
	 * TODO Keep XML FileFormat also for XML files that have been converted to CONLL 
	 */
	@Override
	public int getXMLResourcesWithFileFormatXML() {
		String query = "g.V().hasLabel('"+FileVertex+"').has('"+File4ormat+"','XML').dedup()";
		Utils.debug(query);
		ArrayList<Vertex> x = queries.genericVertexQuery(query);
		return x.size();
	}

	@Override
	public int getXMLResourcesWithModelOrLanguageCount() {
		String query = 
				"g.V().hasLabel('"+FileVertex+"').as('f')"+
				".or(outE('"+ModelEdge+"').not(has('"+XMLAttribute+"','')),outE('"+LanguageEdgeLexvo+"').not(has('"+XMLAttribute+"',''))).select('f').dedup()";
		Utils.debug(query);
		ArrayList<Vertex> x = queries.genericVertexQuery(query);
		return x.size();
	}
	
	
	@Override
	public int getXMLResourcesWithUnselectedModelAndLanguageCount() {
		String query = 
				"g.V().hasLabel('"+FileVertex+"').outE('"+ModelEdge+"')"+
				".not(has('"+XMLAttribute+"','')).outV().as('f')"+
			    ".where(outE('"+ModelEdge+"')"+
				".not(has('"+XMLAttribute+"','')).has('"+Selected+"',true).count().is(0)).select('f').dedup()";
		Utils.debug(query);
		ArrayList<Vertex> x = queries.genericVertexQuery(query);
		return x.size();
	}
	
	@Override
	public int getXMLResourcesWithSelectedModelsCount() {
		String query = 
				"g.V().hasLabel('"+FileVertex+"').as('f')"+
				".outE('"+ModelEdge+"').not(has('"+XMLAttribute+"','')).has('"+Selected+"',true).select('f').dedup()";
		Utils.debug(query);
		ArrayList<Vertex> x = queries.genericVertexQuery(query);
		return x.size();
	}
	
	@Override
	public int getXMLResourcesWithSelectedLanguagesCount() {
		String query = 
				"g.V().hasLabel('"+FileVertex+"').as('f')"+
				".outE('"+LanguageEdgeLexvo+"').not(has('"+XMLAttribute+"','')).has('"+Selected+"',true).select('f').dedup()";
		Utils.debug(query);
		ArrayList<Vertex> x = queries.genericVertexQuery(query);
		return x.size();
	}
	

	@Override
	public HashSet<String> getDoneResourceUrls() {
		ArrayList <Vertex> resources = getDoneResources();
		HashSet <String> result = new HashSet <String>();
		for (Vertex v : resources) {
			result.add(v.value(ResourceManager.ResourceUrl));
		}
		return result;
	}
	
	
	/**
	 * Get scanned resources which do contain files with the given status code. In case that a resource
	 * contains multiple files multiple
	 * files per resource
	 * @param statusCode File status code
	 * @param checkAll Return resource only if all files have that status code (if FALSE then return
	 * the resource in case at least one file with that status code exists)
	 * @return Resources that have a file with the given statusCode
	 */
	@Override
	public HashSet<String> getDoneResourcesWithFileWithStatus(String statusCode, boolean checkAll) {
		HashSet<String> result = new HashSet <String>();
		
		if (!checkAll) {
		for (Vertex resource : getDoneResources()) {
			for (Vertex f : getResourceFiles(resource)) {
				if (f.value(FileStatusCode).equals(statusCode))
					result.add(resource.value(ResourceUrl));
					break;
			}
		}
		} else {
		for (Vertex resource : getDoneResources()) {
			boolean allTrue = true;
			for (Vertex f : getResourceFiles(resource)) {
				if (!f.value(FileStatusCode).equals(statusCode))
					allTrue = false;
					break;
			}
			if (allTrue) {
				result.add(resource.value(ResourceUrl));
			}
		}
		}
		
		return result;
	}

	/**
	 * Add a new language from lexvo.org
	 * @param lexvoUrl The lexvo.org URL
	 * @param description Language description
	 */
	@Override
	public Vertex addLanguage(URL lexvoUrl, String description) {
		
		// Check language exists
		Vertex existingLanguage = getLanguage(lexvoUrl);
		if (existingLanguage != null) return existingLanguage;
		
		// Create language vertex
		
		
		String addVertexQuery = "g.addV('"+LanguageVertex+"')"
				+ ".property('"+LanguageLexvoUrl+"','"+lexvoUrl.toString()+"')"
				+ ".property('"+LanguageDescription+"','"+description+"')";
		
		/*
		String addVertexQuery = "g.addV(label,'"
				+LanguageVertex			+"','"
				+LanguageLexvoUrl		+"','"+lexvoUrl.toString()	+"','"
				+LanguageDescription	+"','"+description			+"')";
		*/
		
		return queries.addVertex(addVertexQuery);
	}
	
	
	/**
	 * Add a new vocabulary
	 * @param vocabulary
	 * @param description vocabulary description
	 */
	@Override
	public Vertex addVocabulary(VocabularyType vocabulary, String description) {
		
		// Check vocabulary exists
		Vertex existingVocabulary = getVocabulary(vocabulary);
		if (existingVocabulary != null) return existingVocabulary;
		
		// Create vocabulary vertex
		String addVertexQuery = "g.addV('"+VocabularyVertex+"')"
				+ ".property('"+Vocabularytype+"','"+vocabulary.toString()+"')"
				+ ".property('"+VocabularyDescription+"','"+description+"')";
		
		return queries.addVertex(addVertexQuery);
	}
	
	
	
	/**
	 * Add a new token
	 * @param token
	 */
	@Override
	public Vertex addToken(String token) {
		
		// Check token exists
		Vertex existingToken = getToken(token);
		if (existingToken != null) return existingToken;
		
		// Create vocabulary vertex
		String addVertexQuery = "g.addV('"+TokenVertex+"')"
				+ ".property('"+TokenString+"','"+token+"')";
		
		return queries.addVertex(addVertexQuery);
	}
	
	
	
	/**
	 * Add a new RDF predicate
	 * @param predicateUrl
	 */
	@Override
	public Vertex addPredicate(String predicateUrl) {
		
		// Check token exists
		Vertex existingPredicate = getPredicate(predicateUrl);
		if (existingPredicate != null) return existingPredicate;
		
		// Create vocabulary vertex
		String addVertexQuery = "g.addV('"+PredicateVertex+"')"
				+ ".property('"+PredicateUrl+"','"+predicateUrl+"')"
				+ ".property('"+PredicateDisabled+"',false)"
				+ ".property('"+PredicateDefault+"',false)"
				+ ".property('"+PredicateType+"','')";
		
		
		return queries.addVertex(addVertexQuery);
	}
	
	
	
	/**
	 * Delete language resource
	 * @param lexvoUrl The lexvo.org URL
	 */
	@Override
	public void deleteLanguage(URL lexvoUrl) {
		String query = "g.V().hasLabel('"+LanguageVertex+"').has('"+LanguageLexvoUrl+"','"+lexvoUrl.toString()+"')";
		Utils.debug(query);
		// Delete language vertex
		queries.genericDeleteQuery(query);
	}
	
	/**
	 * Get a language by lexvo URL or iso-code string value. If both arguments are set then the lexvo URL will be used.
	 * @param lexvoUrl
	 */
	@Override
	public Vertex getLanguage(URL lexvoUrl) {
		
		String query = null;
		if (lexvoUrl != null)
			query = "g.V().hasLabel('"+LanguageVertex+"').has('"+LanguageLexvoUrl+"','"+lexvoUrl.toString()+"')";
		else
			return null;
		
		ArrayList <Vertex> language = queries.genericVertexQuery(query);
		
		if (language.isEmpty()) return null;
		else return language.get(0);
	}


	/**
	 * Add all languages found in file
	 * @param fileVertex
	 * @param languageMatches Set of language codes
	 */
	@Override
	public void updateFileLanguages(Vertex resourceVertex, Vertex fileVertex, FileInfo fileInfo) {
		
		updateFileLanguages((String) resourceVertex.value(ResourceUrl), (String) fileVertex.value(FilePathRel), fileInfo, false);
	}


	@Override
	public Integer getFileId(Vertex resource, String fileIdentifier) {
		return getResourceFile(resource, fileIdentifier).value(FileId);
	}


	@Override
	public Vertex addModel(ModelType modelType) {
		
		Vertex existingModel = getModel(modelType);
		if (existingModel != null) return existingModel;
		
		// Create model vertex
		String addVertexQuery = "g.addV('"+ModelVertex+"')"
				+ ".property('"+Modeltype+"','"+modelType.toString()+"')";
		
		return queries.addVertex(addVertexQuery);
		
	}
	
	
	@Override
	public void deleteModel(ModelType modelType) {
		
		// Delete model vertex
		queries.genericDeleteQuery("g.V().hasLabel('"+ModelVertex+"').has('"+Modeltype+"','"+modelType.toString()+"')");
	}
	
	

	/**
	 * Add model information to a file if the tag set of the model was used
	 * @param resourceIdentifier
	 * @param fileIdentifier
	 * @param fileInfo
	 * 
	 * TODO why use fileIdentifier and fileInfo separately ?
	 * TODO why use resourceInfo and fileInfo separately ?
	 */
	@Override
	public void updateFileModels(String resourceIdentifier, String fileIdentifier, FileInfo fileInfo, boolean forceOverwrite) {
		
		// update db
		clearFileModels(resourceIdentifier, fileIdentifier);
		
		HashSet <String> doneModels = new HashSet <String> ();
		for (ModelMatch mm : fileInfo.getModelMatchings()) {
		
		Utils.debug(mm.getModelType().name());
	
		// Do not add edge to same model twice !
		String modelId = mm.getModelType().toString()+"#"+mm.getConllColumn()+"#"+mm.getRdfProperty()+"#"+mm.getXmlAttribute();
		if (doneModels.contains(modelId)) continue;
		else doneModels.add(modelId);
		
		Vertex modelVertex = getModel(mm.getModelType());
		
		// Add new model to resource database
		if (modelVertex == null) {
			modelVertex = addModel(mm.getModelType());
		}
		

		// Add edge from file to found model
		String modelQuery = "modelVertex = g.V().hasLabel('"+ModelVertex+"').has('"+Modeltype+"','"+mm.getModelType().toString()+"').next()";
		String fileQuery = "fileVertex = "+makeFileQuery(resourceIdentifier, fileIdentifier)+".next()";
		String addEdgeQuery = modelQuery+";"+fileQuery+";"+
		"fileVertex.addEdge('"+ModelEdge+"',modelVertex,'"+
							   Detectionmethod+"','"+mm.getDetectionMethod().name()+"','"+
							   DifferentHitTypes+"',"+mm.getDifferentHitTypes()+",'"+
							   ExclusiveHitTypes+"',"+mm.getExclusiveHitTypes()+",'"+
							   TotalHitCount+"',"+mm.getHitCountTotal()+",'"+
							   ExclusiveHitCount+"',"+mm.getExclusiveHitCountTotal()+",'"+
							   HitConllColumn+"',"+mm.getConllColumn()+",'"+
							   ModelCoverage+"',"+mm.getCoverage()+"f"+",'"+
							   Selected+"',"+mm.isSelected()+",'"+
							   Confidence+"',"+mm.getConfidence()+"f"+",'"+
							   Detectionsource+"','"+mm.getDetectionSource().name()+"','"+
							   ModelRecall+"',"+mm.getRecall()+"f"+",'"+
							   ModelRdfProperty+"','"+mm.getRdfProperty()+"','"+
							   ModelWasUpdated+"',"+mm.getDateGetTime()+",'"+
							   ModelUpdateText+"','"+mm.getUpdateText()+"','"+
							   XMLAttribute+"','"+mm.getXmlAttribute()+"','"+
							   ModelFalseNegativeTypes+"',"+mm.getFalseNegativeTypes()+",'"+
							   ModelFalseNegativeCount+"',"+mm.getFalseNegativeCount()+")";
		
		
		
		//Utils.debug(addEdgeQuery);
		queries.addEdge(addEdgeQuery);
		//Utils.debug("after addEdge");
		
		}
	}
	
	
	private void clearFileModels(String resourceIdentifier, String fileIdentifier) {
		String fileQuery = makeFileQuery(resourceIdentifier, fileIdentifier);
		
		String removeEdgeQuery = fileQuery+".outE('"+ModelEdge+"')";
		Utils.debug(removeEdgeQuery);
		queries.genericDeleteQuery(removeEdgeQuery);
	}
	
	
	/**
	 * Update language information for a file
	 * @param resourceIdentifier
	 * @param fileIdentifier
	 * @param languageMatches
	 * @param forceOverwrite - true = write languages, false = update languages
	 */
	@Override
	public void updateFileLanguages(String resourceIdentifier, String fileIdentifier, FileInfo fileInfo, boolean forceOverwrite) {

		if (!forceOverwrite) {
			// merge new and old computation (keep manual selected languages)
			ResourceInfo empty = new ResourceInfo(); empty.setResource(getResource(resourceIdentifier));
			getFileValues(empty, getResourceFile(resourceIdentifier, fileIdentifier)); // get old values from db
			fileInfo.updateLanguageMatchings(empty.getFileInfo().getLanguageMatchings(), fileInfo.getLanguageMatchings(),fileInfo.getFileFormat());
		}

		// write to db
		clearFileLanguages(resourceIdentifier, fileIdentifier, null);
		
		ArrayList <String> doneLanguages = new ArrayList <String>();
		
		for (LanguageMatch languageMatch : fileInfo.getLanguageMatchings()) {
			
			// don't add same language edge twice
			if (doneLanguages.contains("")) continue;
			else doneLanguages.add(languageMatch.getLanguageISO639Identifier());
			
			Utils.debug("Set file language "+languageMatch.getLanguageISO639Identifier());
			Utils.debug("Lexvo url "+languageMatch.getLexvoUrl());
			if (languageMatch.getLexvoUrl() != null) {
				
				Vertex langVertex = getLanguage(languageMatch.getLexvoUrl());
				
				// Case : language not in database
				if (langVertex == null) {
					Utils.debug("adding new language vertex for "+languageMatch.getLexvoUrl());
					langVertex = addLanguage(languageMatch.getLexvoUrl(), "");
				} 

				String langQuery = "langVertex = g.V().hasLabel('"+LanguageVertex+"').has('"+LanguageLexvoUrl+"','"+languageMatch.getLexvoUrl().toString()+"').next()";
				String fileQuery = "fileVertex = "+makeFileQuery(resourceIdentifier, fileIdentifier)+".next()";
				String addEdgeQuery = langQuery+";"+fileQuery+";"+
						"fileVertex.addEdge('"+LanguageEdgeLexvo+"',langVertex,'"+
											   Detectionmethod+"','"+languageMatch.getDetectionMethod().name()+"','"+
											   HitConllColumn+"',"+languageMatch.getConllColumn()+",'"+
											   DifferentHitTypes+"',"+languageMatch.getDifferentHitTypes()+",'"+
											   TotalHitCount+"',"+languageMatch.getHitCount()+",'"+
											   Detectionsource+"','"+languageMatch.getDetectionSource().name()+"','"+
											   Selected+"',"+languageMatch.isSelected()+",'"+
											   LanguageMinProb+"',"+languageMatch.getMinProb()+"f,'"+
											   LanguageMaxProb+"',"+languageMatch.getMaxProb()+"f,'"+
											   LanguageAverageProb+"',"+languageMatch.getAverageProb()+"f,'"+
											   LanguageNameEn+"','"+languageMatch.getLanguageNameEn()+"','"+
											   LanguageRdfProperty+"','"+languageMatch.getRdfProperty()+"','"+
											   LanguageWasUpdated+"',"+languageMatch.getDateGetTime()+",'"+
											   LanguageUpdateText+"','"+languageMatch.getUpdateText()+"','"+
											   XMLAttribute+"','"+languageMatch.getXmlAttribute()+"')";
				
				
				queries.addEdge(addEdgeQuery);
			}
		}
	}
	
	
	private void clearFileLanguages(String resourceIdentifier, String fileIdentifier, ArrayList<DetectionMethod> deleteDetectionMethods) {
		String fileQuery = makeFileQuery(resourceIdentifier, fileIdentifier);
		
		// Delete any associated language if no detection type is provided
		if (deleteDetectionMethods == null || deleteDetectionMethods.isEmpty()) {
			deleteDetectionMethods = new ArrayList<DetectionMethod>();
			deleteDetectionMethods.addAll(Arrays.asList(DetectionMethod.values()));
		}
		for (DetectionMethod dm : deleteDetectionMethods) {
			String removeEdgeQuery = fileQuery+".outE().hasLabel('"+LanguageEdgeLexvo+"').has('"+Detectionmethod+"','"+dm.name()+"')";
			Utils.debug("clearFileLanguages");
			Utils.debug(removeEdgeQuery);
			queries.genericDeleteQuery(removeEdgeQuery);
			Utils.debug("check :");
		}
		String checkEdgeQuery = fileQuery+".outE('"+LanguageEdgeLexvo+"').inV()";//hasLabel('"+LanguageEdgeLexvo+"').has('"+Detectionmethod+"','"+dm.name()+"')";
		ArrayList<Vertex> check = queries.genericVertexQuery(checkEdgeQuery);
		if (check == null || check.isEmpty()) Utils.debug("O.K.");
	}


	/**
	 * Get model vertex for ModelType or null if not existing
	 * @param modelType
	 * @return modelVertex
	 */
	@Override
	public Vertex getModel(ModelType modelType) {
		
		String query = "g.V().hasLabel('"+ModelVertex+"').has('"+Modeltype+"','"+modelType.toString()+"')";
		ArrayList <Vertex> model = queries.genericVertexQuery(query);
		
		if (model.isEmpty()) return null;
		else return model.get(0);
	}
	
	
	/**
	 * Get model vertex for ModelType or null if not existing
	 * @param vocabulary
	 * @return modelVertex
	 */
	@Override
	public Vertex getVocabulary(VocabularyType vocabulary) {
		
		String query = "g.V().hasLabel('"+VocabularyVertex+"').has('"+Vocabularytype+"','"+vocabulary.toString()+"')";
		ArrayList <Vertex> model = queries.genericVertexQuery(query);
		
		if (model.isEmpty()) return null;
		else return model.get(0);
	}
	
	
	/**
	 * Get token vertex
	 * @param token string
	 * @return tokenVertex
	 */
	@Override
	public Vertex getToken(String token) {
		
		//Utils.debug("getToken : "+token+"#"+token.length());
		String query = "g.V().hasLabel('"+TokenVertex+"').has('"+TokenString+"','"+token+"')";
		ArrayList <Vertex> tokens = queries.genericVertexQuery(query);
		
		if (tokens.isEmpty()) return null;
		else return tokens.get(0);
	}
	
	
	/**
	 * Get RDF predicate vertex
	 * @param predicateUrl string
	 * @return tokenVertex
	 */
	@Override
	public Vertex getPredicate(String predicateUrl) {
		
		String query = "g.V().hasLabel('"+PredicateVertex+"').has('"+PredicateUrl+"','"+predicateUrl+"')";
		ArrayList <Vertex> predicates = queries.genericVertexQuery(query);
		
		if (predicates.isEmpty()) return null;
		else return predicates.get(0);
	}
	
	
	
	/**
	 * Check if model is loaded
	 * @param modelType
	 * @return true or false
	 */
	@Override
	public boolean isModelLoaded(ModelType modelType) {

		if (!(getModel (modelType) == null)) {
			return true;
		} else
			return false;
	}


	/**
	 * Add all models found for a file
	 * @param resourceVertex
	 * @param fileVertex
	 * @param fileInfo
	 */
	@Override
	public void updateFileModels(Vertex resourceVertex, Vertex fileVertex, FileInfo fileInfo) {
		
		updateFileModels((String)resourceVertex.value(ResourceUrl), (String) fileVertex.value(FilePathRel), fileInfo, false);
	}

	
	
	@Override
	public ArrayList <Vertex> getFileLanguages(Vertex resource, Vertex file) {
		
		Utils.debug("getFileLanguages");
		String fileQuery = makeFileQuery(resource, file);
		String languageQuery = fileQuery+".outE('"+LanguageEdgeLexvo+"').inV().dedup()"; 
		// dedup because a file can have multiple edges (with different attributes) to a Language (e.g. #writtenRep, #label)
		return queries.genericVertexQuery(languageQuery);
	}
	
	@Override
	public ArrayList <Vertex> getFileModels(Vertex resource, Vertex file) {
		
		String fileQuery = makeFileQuery(resource, file);
		String modelQuery = fileQuery+".outE('"+ModelEdge+"').inV().dedup()";
		// dedup because a file can have multiple edges (with different attributes) to a Model (e.g. #partOfSpeech, #type)
		return queries.genericVertexQuery(modelQuery);
	}
	

	@Override
	public ArrayList <Vertex> getFileVocabularies(Vertex resource, Vertex file) {
		
		String fileQuery = makeFileQuery(resource, file);
		String modelQuery = fileQuery+".outE('"+VocabularyEdge+"').inV().dedup()";
		return queries.genericVertexQuery(modelQuery);
	}
	
	
	private String makeResourceQuery(String resourceIdentifier) {
		String query = "g.V()"
				+ ".hasLabel('"+ResourceVertex+"')"
				+ ".has('"+ResourceUrl+"','"+resourceIdentifier+"')";
		
		return query;
	}
	
	/**
	 * 
	 * @param resource
	 * @param file
	 * @return
	 */
	private String makeFileQuery(Vertex resource, Vertex file) {
		String query = "g.V()"
				+ ".hasLabel('"+ResourceVertex+"')"
				+ ".has('"+ResourceUrl+"','"+resource.value(ResourceUrl)+"')"
				+ ".outE('"+FileEdge+"')"
				+ ".inV()"
				+ ".has('"+FilePathRel+"','"+file.value(FilePathRel)+"')";
		
		return query;
	}
	
	/**
	 * Generate gremlin query for the given resource and file identifier
	 * @param resourceIdentifier 
	 * @param filePathRel Relative file path (is the filename in case of a single file and archive/file.txt (e.g. archive/subfolder/file.txt) for
	 *        a file which is part of an archive !
	 * @return
	 */
	private String makeFileQuery(String resourceIdentifier, String filePathRel) {
		String query = "g.V()"
				+ ".hasLabel('"+ResourceVertex+"')"
				+ ".has('"+ResourceUrl+"','"+resourceIdentifier+"')"
				+ ".outE('"+FileEdge+"')"
				+ ".inV()"
				+ ".has('"+FilePathRel+"','"+filePathRel+"')";
		
		return query;
	}
	
	
	private String makeFileQuery(ResourceInfo resourceInfo) {
		String query = "g.V()"
				+ ".hasLabel('"+ResourceVertex+"')"
				+ ".has('"+ResourceUrl+"','"+resourceInfo.getDataURL()+"')"
				+ ".outE('"+FileEdge+"')"
				+ ".inV()"
				+ ".has('"+FilePathRel+"','"+resourceInfo.getFileInfo().getRelFilePath()+"')";
		return query;
	}
	
	
	/**
	 * DELETE the Registry database. Be careful using this command !!!
	 */
	@Override
	public void deleteDatabase() {
		queries.genericDeleteQuery("g.V()");
		queries.genericDeleteQuery("g.E()");		
	}
	
	@Override
	public ArrayList <Vertex> getVertices() {
		return queries.genericVertexQuery("g.V()");
	}
	
	@Override
	public ArrayList <Edge> getEdges() {
		return queries.genericEdgeQuery("g.E()");
	}

	@Override
	public void closeDb() {
		cluster.close();
	}


	/**
	 * Get all resources
	 * @return
	 */
	@Override
	public ArrayList<ResourceInfo> getAllResourcesRI() {
		
		HashSet <ParseResult> withResults = new HashSet<ParseResult>();
		withResults.add(ParseResult.ERROR);
		withResults.add(ParseResult.NONE);
		withResults.add(ParseResult.SUCCESS);
		withResults.add(ParseResult.UNKNOWN);
		return getDoneResourcesRI(withResults);
	}
	
	/**
	 * Get only resources that yielded a result (e.g. model or language found)
	 * @return
	 */
	@Override
	public ArrayList<ResourceInfo> getSuccessFullResourcesRI() {
		
		HashSet <ParseResult> withResults = new HashSet<ParseResult>();
		withResults.add(ParseResult.SUCCESS);
		return getDoneResourcesRI(withResults);
	}
	
	
	/**
	 * Get all resources that had an error during processing or did not finish
	 * @return
	 */
	@Override
	public ArrayList<ResourceInfo> getErrorResourcesRI() {
		
		HashSet <ParseResult> withResults = new HashSet<ParseResult>();
		withResults.add(ParseResult.ERROR);
		withResults.add(ParseResult.UNKNOWN);
		return getDoneResourcesRI(withResults);
	}

	
	/**
	 * Get all resources that yielded no results (also with errors)
	 * @return
	 */
	@Override
	public ArrayList<ResourceInfo> getUnSuccessFullResourcesRI() {
		
		HashSet <ParseResult> withResults = new HashSet<ParseResult>();
		withResults.add(ParseResult.ERROR);
		withResults.add(ParseResult.NONE);
		withResults.add(ParseResult.UNKNOWN);
		return getDoneResourcesRI(withResults);
	}
	
	@Override
	public ArrayList<ResourceInfo> getDoneResourceRI(Vertex resourceVertex) {
		
		// ALL
		HashSet <ParseResult> withResults = new HashSet<ParseResult>();
		withResults.add(ParseResult.ERROR);
		withResults.add(ParseResult.NONE);
		withResults.add(ParseResult.SUCCESS);
		withResults.add(ParseResult.UNKNOWN);
		ArrayList<Vertex> resources = new ArrayList<Vertex>();
		resources.add(resourceVertex);
		return getDoneResourcesRI_(resources, withResults);
	}
	
	
	@Override
	public ArrayList<ResourceInfo> getDoneResourceRI(String resourceIdentifier) {
		
		Vertex resource = getResource(resourceIdentifier);
		return getDoneResourceRI(resource);
	}
	
	
	ArrayList<ResourceInfo> getDoneResourcesRI(HashSet<ParseResult> withParseResults) {
		
		ArrayList<Vertex> resources = getDoneResources();
		return getDoneResourcesRI_(resources, withParseResults);
	 }

	// TODO do only one call for all types of parse results because each call (e.g. successfull, unsuccessful, error) will
	// do the same computation over and over again.
    // see below (// Determine if the resource is in the desired result set) is done after the file values have been computed
	private ArrayList<ResourceInfo> getDoneResourcesRI_(ArrayList<Vertex> resources, HashSet<ParseResult> withParseResults) {

			ArrayList <ResourceInfo> resourceInfoList = new ArrayList <ResourceInfo>();
			
			int resourceCounter=1;
			int successfulFileCounter=1;
			int sum = resources.size();
			for (Vertex resource : resources) {
				
		 		String dataUrl = resource.value(ResourceManager.ResourceUrl);
				String metaUrl = resource.value(ResourceManager.ResourceMetaUrl);
				String metaUrl2 = resource.value(ResourceManager.ResourceMetaUrl2);
				String format = resource.value(ResourceManager.Resource4mat);
				//String detectedBy = resource.value(ResourceManager.ResourceDetectionType);
				String detectedBy = "LINGHUB";
				
				Utils.debug("resource basic # "+(resourceCounter++)+"/"+sum);
				Utils.debug(dataUrl);
				Utils.debug(metaUrl);
				Utils.debug("#");
				
				
				//if(dataUrl.contains("MASC-3.0.0_PTB.owl.tgz")) continue;
			
				int resourceResponseCode = resource.value(ResourceManager.ResourceResponseCode);
				Long resourceSize = Long.parseLong(resource.value(ResourceManager.ResourceSize).toString());
				String resourceContentType = resource.value(ResourceManager.ResourceContentType);
				String resourceLastModified = resource.value(ResourceManager.ResourceLastModified);
				String resourceETag = resource.value(ResourceManager.ResourceETag);
				
	 			ResourceInfo resourceInfo = new ResourceInfo(dataUrl, metaUrl, metaUrl2, ResourceFormat.valueOf(format),
						resourceResponseCode, resourceSize, resourceContentType, resourceLastModified,
						resourceETag);
	 			resourceInfo.setDetectionMethod(DetectionMethod.valueOf(detectedBy));
	 			resourceInfo.setResource(resource);
	 			
	 			// get stored meta-data from database
	 			getMetadataValues(resourceInfo);
	 			
		 		// Get all files of a resource	 		
			 	ArrayList <Vertex> fileVertices = getResourceFiles(dataUrl);
		 		
			 	int fileCounter = 1;
			 	int fileSum=fileVertices.size();
			 	
		 		for (Vertex f : fileVertices) {
		 			
		 			ResourceInfo clownedResourceInfo = (ResourceInfo) SerializationUtils.clone(resourceInfo);
		 			// below : all files in a resource use the same object for meta-data
		 			clownedResourceInfo.setLinghubAttributes(resourceInfo.getLinghubAttributes());
		 			getFileValues(clownedResourceInfo, f);
					// Create own resourceInfo object for each file
		 			
					// Determine if the resource is in the desired result set
					if(withParseResults.contains(clownedResourceInfo.getFileInfo().getParseResult())) {
							resourceInfoList.add(clownedResourceInfo);
		 			}
					
					fileCounter++;
		 		}
			}
			
			return resourceInfoList;
	}
	
	
	
	/**
	 * (old test)
	 * @deprecated
	 * @see use getSuccesfullResourcesRI(), getUnSuccesfullResourcesRI() ...
	 */
	@Override
	public ArrayList<ResourceInfo> getDoneResourcesRI() {
		return getDoneResourcesRI(false);
	}
	
	
	/**
	 * @deprecated
	 */
	@Override
	public ArrayList<ResourceInfo> getDoneResourcesRI(boolean onlySuccessfull) {
		
		if (onlySuccessfull) {
			return getSuccessFullResourcesRI();
		} else {
			return getUnSuccessFullResourcesRI();
		}
	}
	

	@Override
	public ArrayList<Edge> getModelFileEdge(Vertex resource, Vertex f, Vertex model) {
		
		String fileQuery = makeFileQuery(resource, f);
		String modelFileQuery = fileQuery+".outE('"+ModelEdge+"').as('e').inV().has('"+Modeltype+"','"+model.value(Modeltype)+"').select('e')";
		return queries.genericEdgeQuery(modelFileQuery);		
	}
	
	@Override
	public ArrayList<Edge> getVocabularyFileEdge(Vertex resource, Vertex f, Vertex vocabulary) {
		
		String fileQuery = makeFileQuery(resource, f);
		String modelFileQuery = fileQuery+".outE('"+VocabularyEdge+"').as('e').inV().has('"+Vocabularytype+"','"+vocabulary.value(Vocabularytype)+"').select('e')";
		return queries.genericEdgeQuery(modelFileQuery);		
	}
	
	
	@Override
	public ArrayList<Edge> getLanguageFileEdge(Vertex resource, Vertex f, Vertex language) {
		
		String fileQuery = makeFileQuery(resource, f);
		String languageFileQuery = fileQuery+".outE('"+LanguageEdgeLexvo+"').as('e').inV().has('"+LanguageLexvoUrl+"','"+language.value(LanguageLexvoUrl)+"').select('e')";
		return queries.genericEdgeQuery(languageFileQuery);		
	}


	@Override
	public boolean setResourceDataUrl(String dataUrl, String newDataUrl) {
		
		// Check if newDataUrl allready exists ?
		if (resourceExists(newDataUrl)) return false;
		
		String query = makeResourceQuery(dataUrl);
		HashMap <String, Object> values = new HashMap <String, Object> ();
		values.put("dataUrl", newDataUrl);
		Utils.debug(query+".property('"+ResourceUrl+"',dataUrl)");
		queries.setProperty(query+".property('"+ResourceUrl+"',dataUrl)", values);
		return true;
	}

	@Override
	public void setResourceMetaDataUrl(String dataUrl,String metaDataUrl) {
		String query = makeResourceQuery(dataUrl);
		HashMap <String, Object> values = new HashMap <String, Object> ();
		values.put("metaDataUrl", metaDataUrl);
		Utils.debug(query+".property('"+ResourceMetaUrl+"',metaDataUrl)");
		queries.setProperty(query+".property('"+ResourceMetaUrl+"',metaDataUrl)", values);
	}
	
	
	@Override
	public void setResourceMetaDataUrl2(String dataUrl, String metaDataUrl) {
		String query = makeResourceQuery(dataUrl);
		HashMap <String, Object> values = new HashMap <String, Object> ();
		values.put("metaDataUrl2", metaDataUrl);
		Utils.debug(query+".property('"+ResourceMetaUrl2+"',metaDataUrl)");
		queries.setProperty(query+".property('"+ResourceMetaUrl2+"',metaDataUrl)", values);
	}

	@Override
	public Vertex addXMLTemplate(Template template) {
		
		// Check if template with id exists and delete old version 
		// (other implementation could return null to indicate that no template was added) 
		if (getXMLTemplate(template.getId()) != null) {
			removeXMLTemplate(template.getId());
		}
		
		Gson gson = new Gson();
		String serializedTemplate = gson.toJson(template);
		String addVertexQuery = "g.addV('"+XMLTemplateVertex+"')"
				+ ".property('"+XMLTemplateId+"','"+template.getId()+"')"
				+ ".property('"+XMLTemplateDescription+"','"+template.description+"')"
				+ ".property('"+XMLTemplateDefinition+"','"+serializedTemplate+"');";

		
		// XMLTemplateVertex
		// Vertex Properties
		// XMLTemplateId
		// XMLTemplateDefinition
		// TODO Auto-generated method stub

		return queries.addVertex(addVertexQuery);
	}

	@Override
	public Template getXMLTemplate(String xmlTemplateIdentifier) {
		ArrayList <Vertex> xmlTemplate = 
			queries.genericVertexQuery("g.V().hasLabel('"+XMLTemplateVertex+"')"
					+ ".has('"+XMLTemplateId+"','"+xmlTemplateIdentifier+"')");
		if (xmlTemplate.isEmpty()) return null;
		else {

			Gson gson = new Gson();
			String dryTemplate = xmlTemplate.get(0).value(XMLTemplateDefinition);
			Template hydratedTemplate = gson.fromJson(dryTemplate, Template.class);
			return hydratedTemplate;
		}
	}

	@Override
	public ArrayList<Template> getAllXMLTemplates(){
		ArrayList<Template> result = new ArrayList<>();
		Gson gson = new Gson();
		ArrayList<Vertex> xmlTemplateVertices =
				queries.genericVertexQuery("g.V().hasLabel('"+XMLTemplateVertex+"')");
		if (!xmlTemplateVertices.isEmpty()){
			for (Vertex v : xmlTemplateVertices){
				result.add(gson.fromJson((String) v.value(XMLTemplateDefinition), Template.class));
			}
		}
		else{
			result = null;
		}
		return result;
	}

	
	@Override
	public void removeXMLTemplate(String xmlTemplateIdentifier) {
		// same as the get query but deleting the results.
		queries.genericDeleteQuery("g.V().hasLabel('"+XMLTemplateVertex+"')"
				+ ".has('"+XMLTemplateId+"','"+xmlTemplateIdentifier+"')");
	}

	@Override
	public void updateProcessState(ResourceInfo resourceInfo) {
		String query = makeFileQuery(resourceInfo);
		HashMap <String, Object> values = new HashMap <String, Object> ();
		values.put("processState", resourceInfo.getFileInfo().getProcessState().name());
		queries.setProperty(query+".property('"+FileProcessState+"',processState)", values);		
	}

	
	@Override
	public HashSet<String> getGlobalProcessState(ResourceInfo resourceInfo, String resourcePrefix) {
		
		HashSet<String> resources2beChanged = new HashSet<String>();
		String query = "";
		
		boolean test = true; // only used for test now !
		if (test) {
		
			query = "g.V()"
				+ ".hasLabel('"+ResourceVertex+"')"
				+".filter {it.get().value('"+ResourceUrl+"').matches('"+resourcePrefix+".*')}.dedup()";
		
			for (Vertex v : queries.genericVertexQuery(query)) {
				resources2beChanged.add(v.value(ResourceUrl));
			}
			
			return resources2beChanged;
			
		} else {
			
			// can not fire query because also resources that are not in the view of successful resources will be changed
			/*query = "g.V()"
					+ ".hasLabel('"+ResourceVertex+"')"
					+".filter {it.get().value('"+ResourceUrl+"').matches('"+resourcePrefix+".*')}"
					+ ".outE('"+FileEdge+"')" // TODO add filter on file property, e.g. has(FileProcessState != EXCLUDED)
					+ ".inV()";
			
			HashMap <String, Object> values = new HashMap <String, Object> ();
			values.put("processState", processState.name());
			queries.setProperty(query+".property('"+FileProcessState+"',processState)", values);*/
			return resources2beChanged;
		} 
	}
	
	
	@Override
	public void updateFileComment(ResourceInfo resourceInfo) { 
		String query = makeFileQuery(resourceInfo);
		HashMap <String, Object> values = new HashMap <String, Object> ();
		values.put("comment", resourceInfo.getFileInfo().getComment());
		queries.setProperty(query+".property('"+FileComment+"',comment)", values);
	}

	@Override
	public void updateFileProcessingStartDate(ResourceInfo resourceInfo) {
		String query = makeFileQuery(resourceInfo);
		HashMap <String, Object> values = new HashMap <String, Object> ();
		values.put("processStart", resourceInfo.getFileInfo().getProcessingStartDate().getTime());
		queries.setProperty(query+".property('"+FileProcessingStartDate+"',processStart)", values);

	}
	
	/**
	 * Marks end of processing. In order to free the processed files space the file object of the corresponding resourceInfo will be deleted !
	 */
	@Override
	public void updateFileProcessingEndDate(ResourceInfo resourceInfo) {
		String query = makeFileQuery(resourceInfo);
		HashMap <String, Object> values = new HashMap <String, Object> ();
		values.put("processEnd", resourceInfo.getFileInfo().getProcessingEndDate().getTime());
		queries.setProperty(query+".property('"+FileProcessingEndDate+"',processEnd)", values);
		
		// clear file object
		resourceInfo.getFileInfo().clearFileObject();
	}
	
	
	@Override
	public void updateFileAcceptedDate(ResourceInfo resourceInfo) {
		String query = makeFileQuery(resourceInfo);
		HashMap <String, Object> values = new HashMap <String, Object> ();
		values.put("accepted", resourceInfo.getFileInfo().getAcceptedDateGetTime());
		queries.setProperty(query+".property('"+FileAcceptedDate+"',accepted)", values);
	}

	@Override
	public Edge addXMLTemplateQualityEdge(ResourceInfo resourceInfo, TemplateQuality templateQuality) {
		
		String fileQuery = "fileVertex = "+makeFileQuery(resourceInfo)+".next()";
		String templateQuery = "templateVertex = g.V().hasLabel('"+XMLTemplateVertex+"').has('"+XMLTemplateId+"','"+templateQuality.getTemplate().getId()+"').next()";
		
		StringBuilder addQualitiesQuery = new StringBuilder();
		addQualitiesQuery.append(fileQuery+";"+templateQuery+";"+
				"fileVertex.addEdge('"+XMLQualityEdge+"',templateVertex,");
		ArrayList<String> qualityEntries = new ArrayList<>();
		templateQuality.getQuality().entrySet().forEach(e -> {
			qualityEntries.add("'"+e.getKey()+"','"+e.getValue()+"'");
		});
		addQualitiesQuery.append(String.join(",",qualityEntries));
		addQualitiesQuery.append(")");
		
		return queries.addEdge(addQualitiesQuery.toString());
	}

	@Override
	public void updateFileVocabularies(Vertex resourceVertex, Vertex fileVertex, ArrayList<VocabularyMatch> vocabularyMatches) {
		updateFileVocabularies((String) resourceVertex.value(ResourceUrl), (String) fileVertex.value(FilePathRel), vocabularyMatches);
	}

	@Override
	public void updateFileVocabularies(String resourceIdentifier, String fileIdentifier, ArrayList<VocabularyMatch> vocabularyMatches) {

		// delete existing vocabularies for that file
		clearFileVocabularies(resourceIdentifier, fileIdentifier, null);
		
		for (VocabularyMatch vm : vocabularyMatches) {
			
			Vertex vocabularyVertex = getVocabulary(vm.getVocabulary());
			
			// Add new model to resource database if not vocabulary not present
			if (vocabularyVertex == null) {
				vocabularyVertex = addVocabulary(vm.getVocabulary(),"no info");
			}
			
			// Add edge from file to found vocabulary
			String vocabularyQuery = "vocabularyVertex = g.V().hasLabel('"+VocabularyVertex+"').has('"+Vocabularytype+"','"+vm.getVocabulary().toString()+"').next()";
			String fileQuery = "fileVertex = "+makeFileQuery(resourceIdentifier, fileIdentifier)+".next()";
			String addEdgeQuery = vocabularyQuery+";"+fileQuery+";"+
			"fileVertex.addEdge('"+VocabularyEdge+"',vocabularyVertex,'"+
								   Detectionmethod+"','"+vm.getDetectionMethod().toString()+"')";
			
			Utils.debug(addEdgeQuery);
			queries.addEdge(addEdgeQuery);
			
			}
	}

	public void clearFileVocabularies(String resourceIdentifier, String fileIdentifier, ArrayList<DetectionMethod> deleteDetectionMethods) {
		
		String fileQuery = makeFileQuery(resourceIdentifier, fileIdentifier);
		
		// Delete any associated vocabulary if no detection type is provided
		if (deleteDetectionMethods == null || deleteDetectionMethods.isEmpty()) {
			deleteDetectionMethods = new ArrayList<DetectionMethod>();
			deleteDetectionMethods.addAll(Arrays.asList(DetectionMethod.values()));
		}
		for (DetectionMethod dm : deleteDetectionMethods) {
			String removeEdgeQuery = fileQuery+".outE().hasLabel('"+VocabularyEdge+"').has('"+Detectionmethod+"','"+dm.name()+"')";
			Utils.debug(removeEdgeQuery);
			queries.genericDeleteQuery(removeEdgeQuery);
		}
		
	}
	
	
	@Override
	public void updateFileTokens(Vertex resourceVertex, Vertex fileVertex, HashMap<Integer, HashMap<String, Long>> tokenMap) {
		updateFileTokens((String) resourceVertex.value(ResourceUrl), (String) fileVertex.value(FilePathRel), tokenMap);
	}
	
	@Override
	public void updateFileTokens(String resourceIdentifier, String fileIdentifier, HashMap<Integer, HashMap<String, Long>> tokenMap) {

		// delete existing tokens for that file
		clearFileTokens(resourceIdentifier, fileIdentifier);
		
		int tokenLimitForColumn = 1000; // prevent writing of thousands of useless text tokens

		/*Utils.debug("updateFileTokens :");
		for (int x : tokenMap.keySet()) {
			Utils.debug(x+" : "+tokenMap.get(x).size());
		}
		Utils.debug();*/
		
		for (int column : tokenMap.keySet()) {
			
			HashMap<String, Long> tokens = tokenMap.get(column);
			
			// prevent writing of thousands of useless text tokens
			if (tokens.size() >= tokenLimitForColumn) {
				Utils.debug("skipping token column "+column+" because of token count : "+tokenMap.get(column).size());
				continue; // skip writing of all tokens
			}
			
			for (String token : tokens.keySet()) {
			
			token = token.replace("'", "");
			if (token.isEmpty()) continue;
			if (Executer.featureIgnoreList.contains(token.split("=")[0].trim()) || token.startsWith("MWE")) continue;	// skip useless features 
			
			Vertex tokenVertex = getToken(token);
			
			// Add new token to resource database if not present
			if (tokenVertex == null) {
				try {
					tokenVertex = addToken(token);
				} catch (Exception e){continue;};
			}
			
			// Add edge from file to found token
			String tokenQuery = "tokenVertex = g.V().hasLabel('"+TokenVertex+"').has('"+TokenString+"','"+token+"').next()";
			String fileQuery = "fileVertex = "+makeFileQuery(resourceIdentifier, fileIdentifier)+".next()";
			String addEdgeQuery = tokenQuery+";"+fileQuery+";"+
			"fileVertex.addEdge('"+TokenEdge+"',tokenVertex,'"+
								   TokenColumn+"',"+column+",'"+
								   TokenCount+"','"+tokens.get(token)+"')"; // TODO write Long not String
			
			//Utils.debug(addEdgeQuery);
			queries.addEdge(addEdgeQuery);
			
			}
		}
	}
	
	
	public void clearFileTokens(String resourceIdentifier, String fileIdentifier) {
		
		String fileQuery = makeFileQuery(resourceIdentifier, fileIdentifier);
		String removeEdgeQuery = fileQuery+".outE().hasLabel('"+TokenEdge+"')";
		//Utils.debug(removeEdgeQuery);
		queries.genericDeleteQuery(removeEdgeQuery);
	}
	
	
	public void clearFilePredicates(String resourceIdentifier, String fileIdentifier) {
		
		String fileQuery = makeFileQuery(resourceIdentifier, fileIdentifier);
		String removeEdgeQuery = fileQuery+".outE().hasLabel('"+PredicateEdge+"')";
		//Utils.debug(removeEdgeQuery);
		queries.genericDeleteQuery(removeEdgeQuery);
	}
	
	
	@Override
	public ArrayList<Integer> getFileTokenColumns(ResourceInfo resourceInfo) {
		
		String tokenQuery = 
				"g.V().hasLabel('"+ResourceVertex+"').has('"+ResourceUrl+"','"+resourceInfo.getDataURL()+"').outE('"+FileEdge+"').inV()"+
				".has('"+FilePathRel+"','"+resourceInfo.getFileInfo().getRelFilePath()+"').outE('"+TokenEdge+"')"+
				".values('"+TokenColumn+"').dedup()";
						
		return queries.genericIntegerQuery(tokenQuery);
	}
	
	
	@Override
	public ArrayList<String> getFileTokens(ResourceInfo resourceInfo, int column) {
		
		String tokenQuery = 
				"g.V().hasLabel('"+ResourceVertex+"').has('"+ResourceUrl+"','"+resourceInfo.getDataURL()+"').outE('"+FileEdge+"').inV()"+
				".has('"+FilePathRel+"','"+resourceInfo.getFileInfo().getRelFilePath()+"').outE('"+TokenEdge+"')"+
				".has('"+TokenColumn+"',"+column+").inV().values('"+TokenString+"')";
		//Utils.debug("+++"+tokenQuery);
		
		return queries.genericStringQuery(tokenQuery);
	}
	
	
	@Override
	public HashMap<String, Long> getFileTokensWithCount(ResourceInfo resourceInfo, int column) {
		
		HashMap <String, Long> result = new HashMap<String, Long>();
		String token = "";
		Long tokenCount = 0L;
		
		String tokenQuery = 
				"g.V().hasLabel('"+ResourceVertex+"').has('"+ResourceUrl+"','"+resourceInfo.getDataURL()+"').outE('"+FileEdge+"').inV()"+
				".has('"+FilePathRel+"','"+resourceInfo.getFileInfo().getRelFilePath()+"').outE('"+TokenEdge+"')"+
				".as('x').has('"+TokenColumn+"',"+column+").inV().as('y')"+
				".select('y','x').by('"+TokenString+"').by('"+TokenCount+"')";
				
		// Get unmatched tokens in a CONLL column
    	ArrayList<LinkedHashMap<String, String>> tokens = (ArrayList<LinkedHashMap<String, String>>)
    			queries.genericListMapQuery(tokenQuery);
    	
    	if (tokens.isEmpty()) {Utils.debug("column "+column+ " has no tokens ");return result;}
    	
    	// sort tokens by tag (e.g. NOUN) or feature (x=y)
    	for (LinkedHashMap<String, String> map : tokens) {
    		
    		//Utils.debug("token : "+map.get("y")+" #"+map.get("x"));
    		token = map.get("y");
    		tokenCount = Long.parseLong(map.get("x"));
    		result.put(token, tokenCount);
		}
		
		return result;
	}
	
	
	@Override
	public void updateResourceMetadata(ResourceInfo resourceInfo) {
		
		// delete metadata of resource
		clearResourceMetadata(resourceInfo.getDataURL());
		Utils.debug("cleared");
		
		// add new metadata
		addResourceMetadata(resourceInfo);
	}
	
	
	public void clearResourceMetadata(String resourceIdentifier) {
		String resourceQuery = makeResourceQuery(resourceIdentifier);
		String removeMetadataQuery = resourceQuery+".outE().hasLabel('"+MetadataEdge+"').inV()";
		//Utils.debug(removeEdgeQuery);
		queries.genericDeleteQuery(removeMetadataQuery);
	}
	
	
	@Override
	public void setPredicateDisabled(String predicateUrl) {
		setPredicateOnOff(predicateUrl, true);
	}
	
	@Override
	public void setPredicateEnabled(String predicateUrl) {
		setPredicateOnOff(predicateUrl, false);
	}
	
	
	private void setPredicateOnOff(String predicateUrl, boolean enabled) {
		String query = "g.V()"
				+ ".hasLabel('"+PredicateVertex+"')"
				+ ".has('"+PredicateUrl+"','"+predicateUrl+"')";
				
		HashMap <String, Object> values = new HashMap <String, Object> ();
		values.put("Enabled", enabled);
		queries.setProperty(query+".property('"+PredicateDisabled+"',Enabled)", values);
	}
	
	
	@Override
	public void setPredicateDefault(String predicateUrl) {
		String query = "g.V()"
				+ ".hasLabel('"+PredicateVertex+"')"
				+ ".has('"+PredicateUrl+"','"+predicateUrl+"')";
				
		HashMap <String, Object> values = new HashMap <String, Object> ();
		values.put("Default", true);
		queries.setProperty(query+".property('"+PredicateDefault+"',Default)", values);
	}
	
	
	@Override
	public HashSet<String> getUnsuccessfulPredicates() {
		return queries.evaluatePredicates().get("bad");
	}
	
	
	@Override
	public HashSet<String> getSuccessfulPredicates() {
		return queries.evaluatePredicates().get("good");
	}
	
	
	@Override
	public void updateFilePredicates(Vertex resourceVertex, Vertex fileVertex, HashMap<String, Boolean> predicateMap) {
		updateFilePredicates((String) resourceVertex.value(ResourceUrl), (String) fileVertex.value(FilePathRel), predicateMap);
	}
	
	@Override
	public void updateFilePredicates(String resourceIdentifier, String fileIdentifier, HashMap<String, Boolean> predicateMap) {

		// delete present predicate edges for that file
		clearFilePredicates(resourceIdentifier, fileIdentifier);
		
		for (String predicate : predicateMap.keySet()) {
			
			Vertex predicateVertex = getPredicate(predicate);
			
			// Add new predicate to resource database if not present
			if (predicateVertex == null) {
				predicateVertex = addPredicate(predicate);
			}
			
			// Add edge from file to found predicate as well if the predicate was successful
			String predicateQuery = "predicateVertex = g.V().hasLabel('"+PredicateVertex+"').has('"+PredicateUrl+"','"+predicate+"').next()";
			String fileQuery = "fileVertex = "+makeFileQuery(resourceIdentifier, fileIdentifier)+".next()";
			String addEdgeQuery = predicateQuery+";"+fileQuery+";"+
			"fileVertex.addEdge('"+PredicateEdge+"',predicateVertex,'"+
								   PredicateSuccessful+"',"+predicateMap.get(predicate)+")";
			
			//Utils.debug(addEdgeQuery);
			queries.addEdge(addEdgeQuery);
			
			}
	}
	
	
	/**
	 * Restore resource meta-data values from database
	 * @param resourceInfo
	 * @param m meta-data vertex
	 */
	public void getMetadataValues(ResourceInfo resourceInfo) {
		
		// get stored meta-data from database
		ArrayList <Vertex> metadataVertices = getResourceMetadata(resourceInfo.getDataURL());
	
		Vertex mdv = null;
		if (!metadataVertices.isEmpty()) {
			mdv = metadataVertices.get(0);
		} 
		
		LinghubResource metadata = new LinghubResource();
			
		if (mdv != null) {
			
			metadata.setContributor(mdv.value(ResourceManager.MetaContributor));
			metadata.setCreator(mdv.value(ResourceManager.MetaCreator));
			metadata.setDate(new Date(Long.valueOf(mdv.value(ResourceManager.MetaDate).toString())));
			metadata.setYear(mdv.value(ResourceManager.MetaYear));
			metadata.setEmailContact(mdv.value(ResourceManager.MetaContact));
			metadata.setWebpage(mdv.value(ResourceManager.MetaWebpage));
			String mds = mdv.value(ResourceManager.MetaDatasource);
			if (!mds.trim().isEmpty()) {metadata.setMetadataSource(MetadataSource.valueOf(mds.toUpperCase()));}
			metadata.setKeywords(mdv.value(ResourceManager.MetaSubject));
			metadata.setDescription(mdv.value(ResourceManager.MetaDescription));
			metadata.setFormat(mdv.value(ResourceManager.MetaFormat));
			metadata.setRights(mdv.value(ResourceManager.MetaRights));
			metadata.setLocation(mdv.value(ResourceManager.MetaLocation));
			metadata.setPublisher(mdv.value(ResourceManager.MetaPublisher));
			metadata.setTitle(mdv.value(ResourceManager.MetaTitle));
			metadata.setUbTitle(mdv.value(ResourceManager.MetaUbTitle));
			metadata.setType(mdv.value(ResourceManager.MetaType));
			metadata.setDcLanguageString(mdv.value(ResourceManager.MetaDcLanguages));
			metadata.setDctLanguageString(mdv.value(ResourceManager.MetaDctLanguages));
			
		} else {
			// Set dummy meta-data values
			metadata.setTitle("");
			metadata.setUbTitle("");
			metadata.setDescription("");
			metadata.setCreator("");
			metadata.setContributor("");
			metadata.setDate(new Date());
			metadata.setYear("");
			metadata.setEmailContact("");
			metadata.setWebpage("");
			metadata.setMetadataSource(MetadataSource.NONE);
			metadata.setKeywords("");
			metadata.setFormat("");
			metadata.setRights("");
			metadata.setLocation("");
			metadata.setPublisher("");
			metadata.setType("");
			metadata.setDcLanguageString("");
			metadata.setDctLanguageString("");
		}
		
		resourceInfo.setLinghubAttributes(metadata);
	}
	

	/**
	 * Restore file values from database
	 * @param resourceInfo
	 * @param f fileVertex
	 */
	public void getFileValues(ResourceInfo resourceInfo, Vertex f) {
		
 			//Integer fileId = f.value(ResourceManager.FileId);
 			String absFilePath = f.value(ResourceManager.FilePathAbs);
 			String fileName = f.value(ResourceManager.FileName);
 			String relFilePath = f.value(ResourceManager.FilePathRel);
 			String fileFormat = f.value(ResourceManager.File4ormat);
 			String statusCode	= f.value(ResourceManager.FileStatusCode);
 			ProcessState processState	= ProcessState.valueOf((String)f.value(ResourceManager.FileProcessState));
 			Long tripleCount	= f.value(ResourceManager.FileTripleCount);
 			Long fileSizeInBytes	= Long.parseLong(f.value(ResourceManager.FileSizeInBytes).toString());
 			if (fileSizeInBytes == 0) {
 				fileSizeInBytes = resourceInfo.getHttpContentLength();
 			} // set missing values
 			String errorCode	= f.value(ResourceManager.FileErrorCode);
 			String errorMsg		= f.value(ResourceManager.FileErrorMsg);
 			String comment		= f.value(ResourceManager.FileComment);
 			Date fileProcessingStartDate = new Date(Long.valueOf(f.value(ResourceManager.FileProcessingStartDate).toString()));
			Date fileProcessingEndDate = new Date(Long.valueOf(f.value(ResourceManager.FileProcessingEndDate).toString()));
			
			Date fileAcceptedDate = null;
			long dateLong = Long.valueOf(f.value(ResourceManager.FileAcceptedDate).toString());
			if (dateLong > 0) {
				fileAcceptedDate = new Date(dateLong);
			}
			String sample		= f.value(ResourceManager.FileSample);
 			ArrayList <LanguageMatch> languageMatchings = new ArrayList <LanguageMatch>();
 			ArrayList <ModelMatch> modelMatchings = new ArrayList <ModelMatch>();
 			ArrayList <VocabularyMatch> vocabularyMatchings = new ArrayList <VocabularyMatch>();
 			
 			//resourceInfo.getFileInfo().setFileId(fileId);
 			resourceInfo.getFileInfo().setAbsFilePath(absFilePath);
 			resourceInfo.getFileInfo().setResourceFile(new File(absFilePath),Paths.get(relFilePath));
 			resourceInfo.getFileInfo().setFileFormat(FileFormat.valueOf(fileFormat));
 			resourceInfo.getFileInfo().setStatusCode(statusCode);
 			resourceInfo.getFileInfo().setTripleCount(tripleCount);
 			resourceInfo.getFileInfo().setFileSizeInBytes(fileSizeInBytes);
 			resourceInfo.getFileInfo().setErrorCode(errorCode);
 			resourceInfo.getFileInfo().setErrorMsg(errorMsg);
 			resourceInfo.getFileInfo().setProcessState(processState);
 			resourceInfo.getFileInfo().setComment(comment);
 			resourceInfo.getFileInfo().setProcessingStartDate(fileProcessingStartDate);
 			resourceInfo.getFileInfo().setProcessingEndDate(fileProcessingEndDate);
 			resourceInfo.getFileInfo().setAcceptedDate(fileAcceptedDate);
 			resourceInfo.getFileInfo().setSample(sample);
 			
 			Utils.debug("File : "+fileName);
 			
 			
	 		// Get all languages in a file	 		
			ArrayList <Vertex> langVertices = getFileLanguages(resourceInfo.getResource(), f);
			Utils.debug("getFileValues :"+langVertices.size());
			int fcounter = 0;
			for (Vertex lang : langVertices) {
				String lexvoUrl = lang.value(ResourceManager.LanguageLexvoUrl);
				Utils.debug("langvertex :"+(fcounter++)+":"+lexvoUrl);
				
				ArrayList <Edge> languageEdges = getLanguageFileEdge(resourceInfo.getResource(), f, lang);
				for (Edge e : languageEdges) {
				
					//Utils.debug(lexvoUrl);
					LanguageMatch languageMatch;
					try {
						languageMatch = new LanguageMatch(new URL(lexvoUrl));
						languageMatch.setDetectionMethod(DetectionMethod.valueOf(e.value(ResourceManager.Detectionmethod)));
						if (e.keys().contains(ResourceManager.HitConllColumn))
							languageMatch.setConllColumn(e.value(ResourceManager.HitConllColumn));
						languageMatch.setDifferentHitTypes(Long.parseLong(e.value(ResourceManager.DifferentHitTypes).toString()));
						languageMatch.setHitCount(Long.parseLong(e.value(ResourceManager.TotalHitCount).toString()));
						languageMatch.setMinProb(Float.parseFloat(e.value(ResourceManager.LanguageMinProb).toString()));
						languageMatch.setMaxProb(Float.parseFloat(e.value(ResourceManager.LanguageMaxProb).toString()));
						languageMatch.setAverageProb(Float.parseFloat(e.value(ResourceManager.LanguageAverageProb).toString()));
						languageMatch.setLanguageNameEn(e.value(ResourceManager.LanguageNameEn).toString());
						languageMatch.setDetectionSource(DetectionSource.valueOf(e.value(ResourceManager.Detectionsource).toString()));
						languageMatch.setSelected(Boolean.parseBoolean(e.value(ResourceManager.Selected).toString()));
						languageMatch.setXmlAttribute(e.value(ResourceManager.XMLAttribute));
						languageMatch.setRdfProperty(e.value(ResourceManager.LanguageRdfProperty));
						
						Date languageDate;
						try {
							long languageDateLong = Long.valueOf(e.value(ResourceManager.LanguageWasUpdated).toString());
							languageDate = new Date(languageDateLong);
						} catch (Exception exception) {
							languageDate = new Date(0);
							Utils.debug("no language date found");
						}
						languageMatch.setDate(languageDate);
						languageMatch.setUpdateText(e.value(ResourceManager.LanguageUpdateText));
						
						languageMatchings.add(languageMatch);
						
						//Utils.debug(languageMatch.getConllColumn()+","+lexvoUrl+","+languageMatch.getAverageProb());
						
					} catch (Exception exception) {
						//languageMatch = new LanguageMatch(new File(lexvoUrl).getName());
						exception.printStackTrace();
					}	
				}
			}
				
			// Get all models in a file	 		
			ArrayList <Vertex> modelVertices = getFileModels(resourceInfo.getResource(), f);
			//Utils.debug("models : "+modelVertices.size());
			
			for (Vertex model : modelVertices) {
				String modelType = model.value(ResourceManager.Modeltype);
				//Utils.debug("modelType"+modelType);
				//resourceInfo.getFileInfo().setModelType(ModelType.valueOf(modelType));
				
				ArrayList <Edge> modelTypeEdges = getModelFileEdge(resourceInfo.getResource(), f, model);
				//Utils.debug("model edges : "+modelTypeEdges.size());
				
				for (Edge e : modelTypeEdges) {
					ModelMatch modelMatch = new ModelMatch(ModelType.valueOf(modelType));
					//Utils.debug("found edge");
					modelMatch.setHitCountTotal(new Long((Integer) e.value(ResourceManager.TotalHitCount)));
					modelMatch.setExclusiveHitCountTotal(new Long((Integer) e.value(ResourceManager.ExclusiveHitCount)));
					modelMatch.setDifferentHitTypes(new Long ((Integer) e.value(ResourceManager.DifferentHitTypes)));
					modelMatch.setExclusiveHitTypes(new Long ((Integer) e.value(ResourceManager.ExclusiveHitTypes)));
					modelMatch.setDetectionMethod(DetectionMethod.valueOf(e.value(ResourceManager.Detectionmethod)));
					modelMatch.setConllColumn(e.value(ResourceManager.HitConllColumn));
					modelMatch.setCoverage(e.value(ResourceManager.ModelCoverage));
					modelMatch.setSelected(e.value(ResourceManager.Selected));
					modelMatch.setConfidence(Float.parseFloat(e.value(ResourceManager.Confidence).toString()));
					modelMatch.setDetectionSource(DetectionSource.valueOf(e.value(ResourceManager.Detectionsource).toString()));
					modelMatch.setRecall(Float.parseFloat(e.value(ResourceManager.ModelRecall).toString()));
					modelMatch.setFalseNegativeTypes(new Long ((Integer) e.value(ResourceManager.ModelFalseNegativeTypes)));
					modelMatch.setFalseNegativeCount(new Long ((Integer) e.value(ResourceManager.ModelFalseNegativeCount)));
					modelMatch.setXmlAttribute(e.value(ResourceManager.XMLAttribute));
					modelMatch.setRdfProperty(e.value(ResourceManager.ModelRdfProperty));
					
					Date modelDate;
					try {
						long modelDateLong = Long.valueOf(e.value(ResourceManager.ModelWasUpdated).toString());
						modelDate = new Date(modelDateLong);
					} catch (Exception exception) {
						modelDate = new Date(0);
						Utils.debug("no model date found");
					}
					modelMatch.setDate(modelDate);
					try {
						modelMatch.setUpdateText(e.value(ResourceManager.ModelUpdateText));
					} catch (Exception ex1) {
						modelMatch.setUpdateText("");
					}
					
					
					modelMatchings.add(modelMatch);
					
		
					/*if (modelMatch.getConllColumn() == 3) {
					Utils.debug("+model : "+modelMatch.getModelType());
					Utils.debug("+column : "+modelMatch.getConllColumn());
					Utils.debug("+selected : "+modelMatch.isSelected());
					//break; // array contains exactly 1 edge ! 
					}*/
				}
			}
			
			
			// Get all vocabularies in a file	 		
			ArrayList <Vertex> vocabularyVertices = getFileVocabularies(resourceInfo.getResource(), f);
			Utils.debug("vocabularyVertices "+vocabularyVertices.size());
			
			for (Vertex vocabulary : vocabularyVertices) {
				String vocabularyType = vocabulary.value(ResourceManager.Vocabularytype);
									
				ArrayList <Edge> vocabularyTypeEdges = getVocabularyFileEdge(resourceInfo.getResource(), f, vocabulary);
				Utils.debug("vocabularyTypeEdges "+vocabularyTypeEdges.size());
				// Vocabulary can be matched by different prefixes (e.g. olia-top,olia-system,olia)
				for (Edge e : vocabularyTypeEdges) {
					
					Utils.debug("vedge for "+vocabularyType);
					VocabularyMatch vocabularyMatch = new VocabularyMatch(VocabularyType.valueOf(vocabularyType));
					//Utils.debug("found edge");
					vocabularyMatch.setDetectionMethod(DetectionMethod.valueOf(e.value(ResourceManager.Detectionmethod)));
					vocabularyMatchings.add(vocabularyMatch);
				}
			}
			
			
			resourceInfo.getFileInfo().setLanguageMatchings(languageMatchings);
			resourceInfo.getFileInfo().setModelMatchings(modelMatchings);
			resourceInfo.getFileInfo().setVocabularyMatchings(vocabularyMatchings);
			
			
			// Restore conllcolumn2xmlAttr map
			HashMap <Integer, String> conllcolumn2xmlAttr = new HashMap <Integer, String>();
			for (ModelMatch mm : modelMatchings) {
				if (!mm.getXmlAttribute().isEmpty()) {
					conllcolumn2xmlAttr.put(mm.getConllColumn(), mm.getXmlAttribute());
				}
			}
			resourceInfo.getFileInfo().setConllcolumn2XMLAttr(conllcolumn2xmlAttr);
			
			
			//for (int column : resourceInfo.getFileInfo().getConllColumnsWithModels()) {
				
				//HashSet<ModelMatch> mms = resourceInfo.getFileInfo().getAllModelMatchingsForColumn(column);
				/*for (ModelMatch mm : mms) {
					Utils.debug(mm.getConllColumn()+","+mm.getModelType()+","+mm.isSelected()+","+mm.getCoverage()+","+mm.getDifferentHitTypes()+","+mm.getHitCountTotal()+","+mm.getExclusiveHitTypes()+","+mm.getExclusiveHitCountTotal());
				}*/
			//}
			
			// Update the resource state on the basis of the resourceType and the quality of the detected models
			// (must be called after fileInfo object is complete !!!)
			resourceInfo.getFileInfo().checkOrDisableProcessState();
			
	}

	@Override
	public void initPredicates() {
		
		HashMap<String,Boolean> defaultPredicates = new HashMap<String,Boolean>();
		defaultPredicates.put("http://www.w3.org/1999/02/22-rdf-syntax-ns#type", true);
		defaultPredicates.put("http://www.w3.org/1999/02/22-rdf-syntax-ns#rest", false);
		defaultPredicates.put("http://www.w3.org/2000/01/rdf-schema#subClassOf", false);
		defaultPredicates.put("http://www.w3.org/2002/07/owl#intersectionOf", false);
		defaultPredicates.put("http://www.w3.org/2002/07/owl#unionOf", false);
		defaultPredicates.put("http://www.w3.org/2002/07/owl#disjointWith", false);
		defaultPredicates.put("http://www.w3.org/2002/07/owl#sameAs", false);
		defaultPredicates.put("http://www.w3.org/2002/07/owl#complementOf", false);
		defaultPredicates.put("http://www.w3.org/2002/07/owl#equivalentClass", false);
		defaultPredicates.put("http://www.w3.org/2002/07/owl#differentFrom", false);
		defaultPredicates.put("http://www.w3.org/1999/02/22-rdf-syntax-ns#value", false);
		defaultPredicates.put("http://xmlns.com/foaf/0.1/name", false);
		defaultPredicates.put("http://xmlns.com/foaf/0.1/nick", false);
		defaultPredicates.put("http://lexvo.org/ontology#represents", false);
		
		

		// Update predicates in database that are disabled (false) or are always enabled (true) by default
		for (String p : defaultPredicates.keySet()) {
			this.addPredicate(p);
			
			if (defaultPredicates.get(p) == true) {
				this.setPredicateDefault(p);
			} else {
				this.setPredicateDisabled(p);
			}
		}
	}

	/**
	 * Deletes a single file of a resource. If the resource contains only a single file then the resource is also deleted
	 * @param resourceIdentifier resourceID
	 * @param relFilePath	fileID
	 */
	@Override
	public void deleteResourceFile(String resourceIdentifier, String relFilePath) {
		
		boolean found = false;
		int fileCount;
		
		// Check if resource contains file
		ArrayList<Vertex> files = getResourceFiles(resourceIdentifier);
		fileCount = files.size();
		//Utils.debug("initial file count : "+fileCount);
		for (Vertex file : files) {
			//Utils.debug(file.value(ResourceManager.FilePathRel));
			if (file.value(ResourceManager.FilePathRel).equals(relFilePath)) {
				found = true;break;
			}
		}
		
		// File not found in resource
		if (!found) return;
		
		// if file was the last file in the resource -> delete resource
		if (fileCount == 1) {
			deleteResource(resourceIdentifier);
			return;
		}
		
		// remove token edges, unit info and hits from model graph
		resetResourceFile(resourceIdentifier, relFilePath);

		// Delete resource file
		queries.genericDeleteQuery("g.V().hasLabel('"+ResourceVertex+"').has('"+ResourceUrl+"','"+resourceIdentifier+"')"
		+ ".outE('"+ResourceManager.FileEdge+"').inV().has('"+ResourceManager.FilePathRel+"','"+relFilePath+"')");
		
		
		
		//ArrayList<Vertex> files___ = getResourceFiles(resourceIdentifier);
		//Utils.debug("remaining file count : "+files___.size());
	}

	
	@Override
	public void setFileName(String resourceIdentifier, String fileIdentifier,
			String newFileName) {
			String query = makeFileQuery(resourceIdentifier, fileIdentifier);
			HashMap <String, Object> values = new HashMap <String, Object> ();
			values.put("newFileName", newFileName);
			queries.setProperty(query+".property('"+ResourceManager.FileName+"',newFileName)", values);
	}

	
	@Override
	public void setFileAbsPath(String resourceIdentifier, String fileIdentifier,
			String newFilePath) {
		String query = makeFileQuery(resourceIdentifier, fileIdentifier);
		HashMap <String, Object> values = new HashMap <String, Object> ();
		values.put("newFilePath", newFilePath);
		queries.setProperty(query+".property('"+ResourceManager.FilePathAbs+"',newFilePath)", values);
	}

	@Override
	public void updateFileModels(ResourceInfo resourceInfo, boolean forceOverwrite) {
		
		updateFileModels(
				resourceInfo.getDataURL(),
				resourceInfo.getFileInfo().getRelFilePath(),
				resourceInfo.getFileInfo(),
				forceOverwrite);
	}

	@Override
	public void deleteResourceFileTokens(ResourceInfo resourceInfo, int col) {
		String fileQuery = makeFileQuery(resourceInfo);
		String query = fileQuery+
					   ".outE('"+ResourceManager.TokenEdge+"').has('"+ResourceManager.TokenColumn+"',"+col+")";
		
		queries.genericDeleteQuery(query);
	}


}
