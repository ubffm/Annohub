package de.unifrankfurt.informatik.acoli.fid.serializer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.unifrankfurt.informatik.acoli.fid.owl.ModelDefinition;
import de.unifrankfurt.informatik.acoli.fid.types.FileFormat;
import de.unifrankfurt.informatik.acoli.fid.types.FileResult;
import de.unifrankfurt.informatik.acoli.fid.types.LanguageMatch;
import de.unifrankfurt.informatik.acoli.fid.types.MetadataSource;
import de.unifrankfurt.informatik.acoli.fid.types.ModelMatch;
import de.unifrankfurt.informatik.acoli.fid.types.ModelType;
import de.unifrankfurt.informatik.acoli.fid.types.ProcessState;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceFormat;
import de.unifrankfurt.informatik.acoli.fid.types.ResourceInfo;
import de.unifrankfurt.informatik.acoli.fid.util.AnnotationUtil;
import de.unifrankfurt.informatik.acoli.fid.util.IndexUtils;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;

public class JSONSerializer {
	
	
	/**
	 * Export of accepted resources in JSON format
	 * @param resourceInfoList
	 * @param allowedProcessStates
	 * @return
	 */
	public static String serializeResourceInfos2JSON (List <ResourceInfo> resourceInfoList, HashSet<ProcessState> allowedProcessStates, ModelDefinition modelDefinition) {
		 
	    ObjectMapper mapper = new ObjectMapper();
	    ObjectNode response = mapper.getNodeFactory().objectNode();
	    //ObjectNode response = JsonNodeFactory.instance.objectNode();
		
		HashMap<String,ArrayList<ResourceInfo>> sortedByResource = new HashMap<String,ArrayList<ResourceInfo>>();
		// sort files by resource
		for (ResourceInfo resourceInfo : resourceInfoList) {
			if (!sortedByResource.containsKey(resourceInfo.getDataURL())) {
				ArrayList<ResourceInfo> rlist = new ArrayList<ResourceInfo>();
				rlist.add(resourceInfo);
				sortedByResource.put(resourceInfo.getDataURL(), rlist);
			} else {
				ArrayList<ResourceInfo> rlist = sortedByResource.get(resourceInfo.getDataURL());
				rlist.add(resourceInfo);
				sortedByResource.put(resourceInfo.getDataURL(), rlist);
			}
		}

		HashMap<Integer,ArrayList<ResourceInfo>> sortedByMetadata = new HashMap<Integer,ArrayList<ResourceInfo>>();
		// sort resources by meta-data (bundle different resources that belong together,
		// e.g. http://brown.nlp2rdf.org/lod/a01.ttl, http://brown.nlp2rdf.org/lod/a02.ttl 
		for (String sortedResourceKey : sortedByResource.keySet()) {

			if (sortedByResource.get(sortedResourceKey).size() != 1) continue;
			int metadataHash = sortedByResource.get(sortedResourceKey).get(0).getLinghubAttributes().getHashCode();
			MetadataSource metadataSource = sortedByResource.get(sortedResourceKey).get(0).getLinghubAttributes().getMetadataSource();
			if (metadataSource != MetadataSource.USER || metadataHash == 0) continue; // skip resources that do not have any metadata or are not userDefined
			
			//Utils.debug(" hash "+metadataHash);
			if (!sortedByMetadata.containsKey(metadataHash)) {
				ArrayList<ResourceInfo> rlist = new ArrayList<ResourceInfo>();
				rlist.add(sortedByResource.get(sortedResourceKey).get(0));
				sortedByMetadata.put(metadataHash, rlist);
			} else {
				ArrayList<ResourceInfo> rlist = sortedByMetadata.get(metadataHash);
				rlist.add(sortedByResource.get(sortedResourceKey).get(0));
				sortedByMetadata.put(metadataHash, rlist);
			}
		}
		
		
		// update sortedByResource with bundled resources from sortedByMetadata
		for (int metadataHash : sortedByMetadata.keySet()) {
			
			if (sortedByMetadata.get(metadataHash).size() < 2) continue;
			
			// delete single resources
			for (ResourceInfo r : sortedByMetadata.get(metadataHash)) {
				sortedByResource.remove(r.getDataURL());
				Utils.debug("adding to bundle "+metadataHash+" : "+r.getDataURL());
			}
			// add bundled resource
			//Utils.debug("add "+metadataHash);
			sortedByResource.put(Integer.toString(metadataHash), sortedByMetadata.get(metadataHash));
		}
		
		
		HashSet<String> resourceLanguages = new HashSet<String>();
		HashSet<ModelType> resourceModels = new HashSet<ModelType>();
		HashSet<String> doneLanguages = new HashSet<String>();
		HashSet<ModelType> doneModels = new HashSet<ModelType>();

		
		int id = 1;
		String foundAnnotation ="";
		for (String key : sortedByResource.keySet()) {
			
			// create resource node
			ResourceInfo rs = sortedByResource.get(key).get(0);
			ObjectNode node = JsonNodeFactory.instance.objectNode();
			node.put("resourceId", rs.getDataURL().hashCode());
	 		node.put("dataUrl", rs.getDataURL());
	 		node.put("metaDataURL", rs.getMetaDataURL());
	 		node.put("resourceFormat", rs.getResourceFormat().toString());
	 		node.put("resourceSizeInBytes", rs.getHttpContentLength());
	 			
	 		
	 		// create meta-data node
	 		ObjectNode metadata = JsonNodeFactory.instance.objectNode();
	 		if (!Utils.filterNa(rs.getLinghubAttributes().getTitle()).isEmpty()) 
	 			metadata.put("title",rs.getLinghubAttributes().getTitle());
	 		if (!Utils.filterNa(rs.getLinghubAttributes().getDescription()).isEmpty()) 
	 			metadata.put("description",rs.getLinghubAttributes().getDescription());
	 		if (!rs.getLinghubAttributes().getCreatorList().isEmpty()) {
				ArrayNode creatorList = JsonNodeFactory.instance.arrayNode();
				for (String c : rs.getLinghubAttributes().getCreatorList()) {
					creatorList.add(c);
				}
	 			metadata.put("creator",creatorList);
	 		}
	 		if (!rs.getLinghubAttributes().getContributorList().isEmpty()) {
	 			ArrayNode contributorList = JsonNodeFactory.instance.arrayNode();
				for (String c : rs.getLinghubAttributes().getContributorList()) {
					contributorList.add(c);
				}
	 			metadata.put("contributor",contributorList);
	 		}
	 		if (!Utils.filterNa(rs.getLinghubAttributes().getEmailContact()).isEmpty()) 
	 			metadata.put("contact",rs.getLinghubAttributes().getEmailContact());
	 		if (!Utils.filterNa(rs.getLinghubAttributes().getWebpage()).isEmpty()) 
	 			metadata.put("webpage",rs.getLinghubAttributes().getWebpage());
	 		if (!Utils.filterNa(rs.getLinghubAttributes().getType()).isEmpty()) 
	 			metadata.put("type",rs.getLinghubAttributes().getType());
	 		if (!Utils.filterNa(rs.getLinghubAttributes().getFormat()).isEmpty()) 
	 			metadata.put("format",rs.getLinghubAttributes().getFormat());
	 		if (!Utils.filterNa(rs.getLinghubAttributes().getRights()).isEmpty()) 
	 			metadata.put("licence",rs.getLinghubAttributes().getRights());
	 		if (!rs.getLinghubAttributes().getPublisherList().isEmpty()) {
	 			ArrayNode publisherList = JsonNodeFactory.instance.arrayNode();
				for (String c : rs.getLinghubAttributes().getPublisherList()) {
					publisherList.add(c);
				}
	 			metadata.put("publisher",publisherList);
	 		}
	 		if (!Utils.filterNa(rs.getLinghubAttributes().getYear()).isEmpty()) 
	 			metadata.put("year",rs.getLinghubAttributes().getYear());
	 		//if (rs.getLinghubAttributes().getDate() != null) 
	 		//	metadata.put("date",rs.getLinghubAttributes().getDate().toString());
	 		if (!Utils.filterNa(rs.getLinghubAttributes().getLocation()).isEmpty()) 
	 			metadata.put("location",rs.getLinghubAttributes().getLocation());
	 		if (!rs.getLinghubAttributes().getLinghubLanguagesAsString().equals("---")) 
	 			metadata.put("languages",rs.getLinghubAttributes().getLinghubLanguagesAsString()); 
	 		
	 		ArrayNode files = JsonNodeFactory.instance.arrayNode();
	 		
	 		
	 		boolean hasFiles = false;
	 		// reset
	 		resourceLanguages.clear();
	 		resourceModels.clear();
	 		
	 		// For all files in resource
		 	for (ResourceInfo resourceInfo : sortedByResource.get(key)) {
		 		
		 		// skip unwanted resources for export (e.g. disabled, check)
		 		if  (!allowedProcessStates.isEmpty() && 
		 			 !allowedProcessStates.contains(resourceInfo.getFileInfo().getProcessState())) continue;
		 		
		 		hasFiles=true;
		 		ObjectNode file = JsonNodeFactory.instance.objectNode();
		 		file.put("fileId",(resourceInfo.getDataURL()+resourceInfo.getFileInfo().getFileId()).hashCode());
		 		file.put("fileRelPath",resourceInfo.getFileInfo().getFileId());
		 		//file.put("fileName",resourceInfo.getFileInfo().getResourceFile().getName());
		 		file.put("fileName",resourceInfo.getFileInfo().getFileName());
		 		ResourceFormat x = IndexUtils.determineFileFormat(resourceInfo);
		 		if (x != ResourceFormat.UNKNOWN) file.put("fileFormat",x.name());
		 		// TODO line below is correct, but does show XML files with CONLL type
		 		//file.put("fileFormat",resourceInfo.getFileInfo().getFileFormat().toString());
		 		file.put("fileSizeInBytes", Long.parseLong(resourceInfo.getFileInfo().getFileSizeInBytes().toString()));// use double because float (bug) shows uncorrect value !
		 		file.put("resourceType",resourceInfo.getFileInfo().getResourceType().name());
	
	
				ArrayNode languages = JsonNodeFactory.instance.arrayNode();
				doneLanguages.clear();
				for (LanguageMatch lm : resourceInfo.getFileInfo().getSelectedLanguages()) {
					
						if (!doneLanguages.contains(lm.getLanguageISO639Identifier())) {
							doneLanguages.add(lm.getLanguageISO639Identifier());
							ObjectNode language = JsonNodeFactory.instance.objectNode();
							language.put("languageISO639Identifier",lm.getLanguageISO639Identifier());
							
							if (resourceInfo.getFileInfo().getFileFormat() == FileFormat.CONLL) {
								language.put("conllColumn", lm.getConllColumn());
							}
							
							languages.add(language);
							resourceLanguages.add(lm.getLanguageISO639Identifier());
						}
				}
				file.put("LANGUAGES", languages);
	
				ArrayNode models = JsonNodeFactory.instance.arrayNode();
				doneModels.clear();
				for (ModelMatch mm : resourceInfo.getFileInfo().getSelectedModels()) {
					
					// Skip unwanted empty type
					if (mm.getModelType() == ModelType.UNKNOWN) continue;
					
					resourceModels.add(mm.getModelType());
					//resourceModels.add(mm.getModelType().name());
					
					ObjectNode model;
					if (!doneModels.contains(mm.getModelType())) {
						model = JsonNodeFactory.instance.objectNode();
						
						if (modelDefinition.getModelType2ModelNameNice().get(mm.getModelType()) == null) {
							Utils.debug("modelNameNice error : "+mm.getModelType());
						}
						model.put("modelType", modelDefinition.getModelType2ModelNameNice().get(mm.getModelType()));
						
						/*if (resourceInfo.getFileInfo().getFileFormat() == FileFormat.CONLL) {
							model.put("conllColumn", mm.getConllColumn());
						}*/
						models.add(model);
						doneModels.add(mm.getModelType());
					} 
					
					//model.put("modelType", mm.getModelType().name());
					
					
					/*ArrayNode fileResults = JsonNodeFactory.instance.arrayNode();
					if (resourceInfo.getFileInfo().getFileResults().containsKey(mm)) {
						for (FileResult fr : resourceInfo.getFileInfo().getFileResults().get(mm)) {
							ObjectNode fileResult = JsonNodeFactory.instance.objectNode();
							if (fr.getFeatureName().isEmpty()) {
								foundAnnotation = fr.getFoundTagOrClass();
							} else {
								foundAnnotation = fr.getFeatureName();
							}
							// Skip unmatched annotations
							if (foundAnnotation.equals(AnnotationUtil.unmatchedAnnotations)) continue;
							
							fileResult.put("found", foundAnnotation);
							fileResult.put("matchingTagOrClass", fr.getMatchingTagOrClass());
							fileResult.put("matchCount", fr.getMatchCount());
							fileResults.add(fileResult);
						}
						model.put("RESULTS", fileResults);
					}*/
					
					//models.add(model);
				}
				file.put("MODELS", models);
				files.add(file);
		 	}
		 	
		 	if (metadata.size() > 0) { // skip metadata node if no metadata
		 		node.put("METADATA", metadata);
		 	}
		 	
		 	node.put("fileCount",files.size());
		 	node.put("FILES", files);
		 	
		 	// add agregated models, languages
	 		ArrayNode aggregatedLanguages = JsonNodeFactory.instance.arrayNode();
	 		for (String alang : resourceLanguages) {
	 			aggregatedLanguages.add(alang);
	 		}
	 		ArrayNode aggregatedModels = JsonNodeFactory.instance.arrayNode();
	 		for (ModelType amodel : resourceModels) {
	 			
	 			ObjectNode am = JsonNodeFactory.instance.objectNode();
				am.put("modelType", modelDefinition.getModelType2ModelNameNice().get(amodel));
				//Utils.debug(amodel.name());
				if(!modelDefinition.getModelDefinitions().get(amodel).getDocumentationUrl().isEmpty()) {
					am.put("modelDoc", modelDefinition.getModelDefinitions().get(amodel).getDocumentationUrl());
				}
				//Utils.debug(amodel.name() + "done");
	 			aggregatedModels.add(am);
	 		}
			node.put("aggregatedLanguages", aggregatedLanguages);
	 		node.put("aggregatedModels", aggregatedModels); 
		 	
	 		
		 	if (hasFiles) { // skip files node if no files
		 		response.put("RESOURCE"+id, node);
		 		id++;
		 	}
		}
	 	
	   try {
			return mapper.writeValueAsString(response);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
	 	
	 	return null;
		}
	
	
/**
 * Old export of accepted resources in JSON format
 * @param resourceInfoList
 * @param allowedProcessStates
 * @return
 * @deprecated
 */
public static ObjectNode serializeResourceInfosSingle (List <ResourceInfo> resourceInfoList, HashSet<ProcessState> allowedProcessStates) {
		
		ObjectNode response = JsonNodeFactory.instance.objectNode();
		
		int id = 1;
		String foundAnnotation ="";
	 	for (ResourceInfo resourceInfo : resourceInfoList) {
	 		
	 		// skip unwanted resources for export (e.g. disabled, check)
	 		if  (!allowedProcessStates.isEmpty() && 
	 			 !allowedProcessStates.contains(resourceInfo.getFileInfo().getProcessState())) continue;
	 		
	 		ObjectNode node = JsonNodeFactory.instance.objectNode();
	 		node.put("dataUrl", resourceInfo.getDataURL());
	 		node.put("metaDataURL", resourceInfo.getMetaDataURL());
	 		node.put("resourceFormat", resourceInfo.getResourceFormat().toString());
	 		
	 		ObjectNode file = JsonNodeFactory.instance.objectNode();
	 		file.put("fileId",resourceInfo.getFileInfo().getFileId());
	 		file.put("fileName",resourceInfo.getFileInfo().getResourceFile().getName());
	 		file.put("fileFormat",resourceInfo.getFileInfo().getFileFormat().toString());
	 		file.put("filesizeInBytes", Double.parseDouble(resourceInfo.getFileInfo().getFileSizeInBytes().toString()));// use double because float (bug) shows uncorrect value !
	 		file.put("resourceType",resourceInfo.getFileInfo().getResourceType().name());


			ArrayNode languages = JsonNodeFactory.instance.arrayNode();
				for (LanguageMatch lm : resourceInfo.getFileInfo().getSelectedLanguages()) {
					ObjectNode language = JsonNodeFactory.instance.objectNode();
					language.put("languageISO639Identifier",lm.getLanguageISO639Identifier());
					
					if (resourceInfo.getFileInfo().getFileFormat() == FileFormat.CONLL) {
						language.put("conllColumn", lm.getConllColumn());
					}
					
					languages.add(language);
			}
			file.put("LANGUAGES", languages);

			ArrayNode models = JsonNodeFactory.instance.arrayNode();
			for (ModelMatch mm : resourceInfo.getFileInfo().getSelectedModels()) {
				ObjectNode model = JsonNodeFactory.instance.objectNode();
				model.put("modelType", mm.getModelType().name());
				
				if (resourceInfo.getFileInfo().getFileFormat() == FileFormat.CONLL) {
					model.put("conllColumn", mm.getConllColumn());
				}
				
				ArrayNode fileResults = JsonNodeFactory.instance.arrayNode();
				if (resourceInfo.getFileInfo().getFileResults().containsKey(mm)) {
					for (FileResult fr : resourceInfo.getFileInfo().getFileResults().get(mm)) {
						ObjectNode fileResult = JsonNodeFactory.instance.objectNode();
						if (fr.getFeatureName().isEmpty()) {
							foundAnnotation = fr.getFoundTagOrClass();
						} else {
							foundAnnotation = fr.getFeatureName();
						}
						// Skip unmatched annotations
						if (foundAnnotation.equals(AnnotationUtil.unmatchedAnnotations)) continue;
						
						fileResult.put("found", foundAnnotation);
						fileResult.put("matchingTagOrClass", fr.getMatchingTagOrClass());
						fileResult.put("matchCount", fr.getMatchCount());
						fileResults.add(fileResult);
					}
					model.put("RESULTS", fileResults);
				}
				
				models.add(model);
			}
			file.put("MODELS", models);
			
			node.put("FILE ", file);
			response.put("RESOURCE "+id, node);
			id++;
	 	}
	 	
	 	
	 	return response;
	}

}
