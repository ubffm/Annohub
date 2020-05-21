package de.unifrankfurt.informatik.acoli.fid.services;


import java.io.File;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.FileUtils;

import de.unifrankfurt.informatik.acoli.fid.resourceDB.ResourceManager;


@Path("/getResources")
public class GetAllResourceInfo {
	
  ResourceManager resourceManager;
  @Context ServletContext context;
  
  

@GET
@Produces(MediaType.APPLICATION_JSON)
public String getAllResourceInfoJSON() {
	  
	try {
		File jsonExportFile = new File(context.getInitParameter("jsonFilePath"));
		return FileUtils.readFileToString(jsonExportFile,"utf-8");
		
	} catch (Exception e) {
		e.printStackTrace();
		return "An error occured : "+e.getMessage();
	}
}
 

}