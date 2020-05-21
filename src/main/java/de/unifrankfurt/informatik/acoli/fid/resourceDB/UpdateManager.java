package de.unifrankfurt.informatik.acoli.fid.resourceDB;

import org.apache.http.HttpResponse;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import de.unifrankfurt.informatik.acoli.fid.types.ResourceFormat;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceState;
import de.unifrankfurt.informatik.acoli.fid.types.UpdatePolicy;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;


// TODO retryUnsuccessfullData
//retryUnsuccessfulRdfData in makeUrlPool einbauen
//boolean retryUnsuccessfulRdfData = config.getString("Database.retryUnsuccessfulRdfData").equals("true");

/**
 * Logic for updating resources
 * @author frank
 *
 */
public class UpdateManager {
	
	
	private ResourceManager resourceManager;
	private UpdatePolicy updatePolicy;
	
	
	public UpdateManager(ResourceManager resourceManager, UpdatePolicy updatePolicy) {
		
		this.resourceManager = resourceManager;
		this.updatePolicy = updatePolicy;
	}
	
	
	 /**
		 * Check if a resource has already been processed (is in the ldh database) 
		 * or if a newer version is available which will case a rescan of the resource
		 * (by using the parameter from the server response header)
	     * @return True for download false for skip
		 */
	    public ResourceState getResourceState (
	    		ResourceInfo info) {
	    	
	    	Utils.debug("Using update policy : "+updatePolicy);
			
	  	    // NO URL
	  	    if (info.getDataURL().isEmpty()) {
	  	    	 return ResourceState.Invalid;
	  	    }
	  	    
	  	    // Get resource from graph
	  	    Vertex resource = resourceManager.getResource(info.getDataURL());
	  	    if (resource == null) {
	  	    	return ResourceState.ResourceNotInDB;
	  	    }
	  	    
	  	    // Override UpdatePolicy if forceRescan flag is set in resource
	  	    if (info.getFileInfo().getForceRescan()) {
	  	    	// reset flag
	  	    	info.getFileInfo().setForceRescan(false);
	  	    	// return changed state -> process file
	  	    	return ResourceState.ResourceHasChanged;
	  	    }
	  	    
	  	    
	  	   /* Update ontology if the file is new or changed
			*/
			if (info.getResourceFormat().equals(ResourceFormat.ONTOLOGY)) {
				return dataFileIsActual(resource, info);
			}
			
	  	    /* Never update an already scanned HTML page
	  	     TODO Because the web page as well as the downloaded data files (links) on that
	  	     page are in the ldh database an update will possibly download these data files twice !
			*/
			if (info.getResourceFormat().equals(ResourceFormat.HTML)) {
				return ResourceState.ResourceHasNotChanged;
			}
			
	  	    	  	    
	  	    ResourceState resourceState;
	  	    
	  	    switch (updatePolicy) {
	  	    
	  	    	case UPDATE_ALL :
	  	    		resourceState = ResourceState.ResourceHasChanged;
	  	    		break;
	  	    	
	  	    	case UPDATE_CHANGED :
	  	    		 // Determine if a file has to be updated from its status in the ldh database
	  	  	    	resourceState = dataFileIsActual(resource, info);
	  	  	    	break;
	  	  	    	
	 	    	case UPDATE_NEW :
	 	    		 // Determine if a file has to be updated from its status in the ldh database
	 	  	    	resourceState = dataFileIsActual(resource, info);
	 	  	    	if (resourceState != ResourceState.ResourceNotInDB)
	 	  	    		resourceState = ResourceState.ResourceHasNotChanged;
	 	  	    	break;
	  	  	    
	  	  	    default :
	  	  	    	resourceState = ResourceState.ResourceHasNotChanged;
	  	    }
	  	  
	  	    return resourceState;
	  	}
	    	
	    /**
	     * Determine if a data file has to be updated in the RDF store. 
	     * @param ldhGraph URL of the data file
	     * @param retryUnsuccessfulRdfData TODO
	     * @param contentLength Length in bytes of the data file
	     * @return
	     */
	    private ResourceState dataFileIsActual (Vertex resource, ResourceInfo resourceInfo) {
	    
	    	// resource not in db
	    	if (resource == null) return ResourceState.ResourceNotInDB;
	    	
	    	boolean retryUnsuccessfulRdfData = false;
	    	
	    	Long contentLength = 0L;
	    	Long oldContentLength = new Long(resource.value(ResourceManager.ResourceSize).toString());
			// values from last scan
			contentLength = resourceInfo.getHttpContentLength();
			
			String eTag = "";
	    	String oldEtag = resource.value(ResourceManager.ResourceETag);
			// values from last scan
			eTag = resourceInfo.getHttpETag();
			
			
			Utils.debug("content-length old : "+oldContentLength);
			Utils.debug("content-length new : "+contentLength);
			
	  	    // 1. Content-length header field might be empty - in this case try ETag header field
			if (contentLength.longValue() == 0) {
				
				Utils.debug("http-etag old : "+oldEtag);
				Utils.debug("http-etag new : "+eTag);
				
				if (!oldEtag.equals(eTag)) return ResourceState.ResourceHasChanged;
			}
			
			// 2. Use content-length to decide if resource has changed
			if ((contentLength.longValue() != oldContentLength.longValue()) 
					&& (resourceManager.resourceHadResults(resource.value(ResourceManager.ResourceUrl)) || retryUnsuccessfulRdfData))
				return ResourceState.ResourceHasChanged;
			
			return ResourceState.ResourceHasNotChanged;
			
		}
	    

}
