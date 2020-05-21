package de.unifrankfurt.informatik.acoli.fid.owl;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.io.FileUtils;

import com.jayway.jsonpath.JsonPath;

import de.unifrankfurt.informatik.acoli.fid.types.InvalidModelDefinitionException;
import de.unifrankfurt.informatik.acoli.fid.types.ModelGroup;
import de.unifrankfurt.informatik.acoli.fid.types.ModelInfo;
import de.unifrankfurt.informatik.acoli.fid.types.ModelType;
import de.unifrankfurt.informatik.acoli.fid.types.ModelUsage;
import de.unifrankfurt.informatik.acoli.fid.util.LocateUtils;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;

/**
 * OLIA model definitions 
 * @author frank
 *
 */
public class ModelDefinition {
	
		
	// ModelDefinitions
	private static LinkedHashMap <ModelType, ModelGroup> modelDef = new LinkedHashMap <ModelType, ModelGroup>();
	
	// Maps generated from modelDef
	private static LinkedHashMap<ModelType, String> modelType2ModelNameNice = new LinkedHashMap<ModelType, String>();
	private static LinkedHashMap<ModelType, String[]> models2ClassNamespaces = new LinkedHashMap<ModelType, String[]>();
	private static LinkedHashMap<ModelType, String[]> models2TagNamespaces = new LinkedHashMap<ModelType, String[]>();

	private LocateUtils locateUtils = new LocateUtils();

	
	public ModelDefinition(XMLConfiguration config) throws InvalidModelDefinitionException {
		
		File modelFile = null;
		if (config.containsKey("OWL.ModelDefinitionFile")) {
			modelFile = new File(config.getString("OWL.ModelDefinitionFile"));
		}
		
		if (modelFile == null || !modelFile.exists()) {
			
			// use default model definitions
			modelFile = locateUtils.getLocalFile("/ModelDef.json");
		}
		
		if (!readModelDef(modelFile)) {
			throw new InvalidModelDefinitionException("");
		};
	}
	
	/**
	 * Reading model definitions from ModelDef.json file inits all model definitions
	 * @param success 
	 */
	public static boolean readModelDef(File jsonFile) {
		
		System.out.println("\n\nReading OLiA model definitions from file : "+jsonFile.getAbsolutePath());
		
		LinkedHashMap<ModelType, ModelGroup> newModelDef = new LinkedHashMap <ModelType, ModelGroup>();	
	
		int errors=0;
		int modelGroups=0;
		try {
			String jsonString = FileUtils.readFileToString(jsonFile, StandardCharsets.UTF_8);
			modelGroups = JsonPath.read(jsonString, "$.models.length()");
			int namespaces;
			int files;
			String access = "";
			
			String modelID;
			String documentationUrl;
			String niceName;
			String ns;
			String url;
			String modelUsage;
			boolean active;
			String fileDocumentationURL;
			
			int i = 0;
			int j;
			
			ModelType modelType;
			
			while (i < modelGroups) {
				
				ModelGroup mg = new ModelGroup();
				modelType = null;
				
				// reset variables
				modelID="";
				documentationUrl="";
				niceName="";
				ns= "";
				url="";
				modelUsage="";
				active=false;
				fileDocumentationURL = "";
				
				// create path for model i
				access = "$.models["+i+"]";
				
				System.out.println("*** Model "+(i+1)+" ***");
				
				/*
				 *  R E A D
				 *  
				 *  M O D E L 
				 *  
				 *  P A R A M E T E R
				 *
				 */
				try{modelID = JsonPath.read(jsonString, access+".modelID");
				System.out.println("Model ID   : "+modelID);
				modelType = ModelType.valueOf(modelID);
				mg.setModelType(modelType);
				}catch(Exception e){errors++;e.printStackTrace();}
				
				try{niceName = JsonPath.read(jsonString, access+".niceName");
				System.out.println("Nice name  : "+niceName);
				mg.setNiceName(niceName);
				}catch(Exception e){errors++;e.printStackTrace();}
				
				try{documentationUrl = JsonPath.read(jsonString, access+".documentationUrl");
				System.out.println("Doc URL    : "+documentationUrl);
				mg.setDocumentationUrl(documentationUrl);
				}catch(Exception e){errors++;e.printStackTrace();}
				
				
				
				// N A M E S P A C E S
				namespaces = JsonPath.read(jsonString, access+".namespaces.length()");
				j = 0;
				Utils.debugNor("Namespaces : ");
				HashSet<String> jns = new HashSet<String>();
				while (j < namespaces) {
					
					try{ns = JsonPath.read(jsonString, access+".namespaces["+j+"]");
					Utils.debugNor(ns+" ");
					jns.add(ns);
					}catch(Exception e){errors++;e.printStackTrace();}
					
					j++;
				}
				System.out.println();
				mg.setClassNameSpaces(jns.toArray(new String[jns.size()]));
				mg.setTagNameSpaces(jns.toArray(new String[jns.size()]));
				

				
				
				// F I L E S
				files = JsonPath.read(jsonString, access+".files.length()");
				j = 0;
				System.out.println("Files :");
				ArrayList<ModelInfo> modelInfoList = new ArrayList<ModelInfo>();
				while (j < files) {
					
					try{url = JsonPath.read(jsonString, access+".files["+j+"].url");
					System.out.println((j+1)+" "+url);
					}catch(Exception e){errors++;e.printStackTrace();}
					
					try{modelUsage = JsonPath.read(jsonString, access+".files["+j+"].modelUsage");
					System.out.println("Usage   : "+modelUsage);
					}catch(Exception e){errors++;e.printStackTrace();}
					
					try{fileDocumentationURL = JsonPath.read(jsonString, access+".files["+j+"].documentationURL");
					System.out.println("Doc URL : "+fileDocumentationURL);
					}catch(Exception e){errors++;e.printStackTrace();}
					
					/*try{active = JsonPath.read(jsonString, access+".files["+j+"].active");
					System.out.println(active);
					}catch(Exception e){errors++;e.printStackTrace();}*/
					
					ModelInfo mi = new ModelInfo(url, true, modelType, ModelUsage.valueOf(modelUsage), fileDocumentationURL);
					modelInfoList.add(mi);
				
					j++;
				}
				
				mg.setModelFiles(modelInfoList.toArray(new ModelInfo[modelInfoList.size()]));
				newModelDef.put(modelType, mg);
				
				i++;
			}
			
			System.out.println("#modelGroups = "+modelGroups);
			System.out.println("Errors : "+errors);
			if (errors > 0) {
				return false;
			} else {
				
				/*
				 * I F 
				 *  
				 * N O  
				 *  
				 * E R R O R 
				 * 
				 * T H E N
				 * 
				 * U S E
				 * 
				 * N E W
				 * 
				 * M O D E L   D E F I N I T I O N S
				 *
				 */
				
				modelDef = newModelDef;
				makeMaps();
				return true;
			}
		} catch (Exception e){
			e.printStackTrace();
			System.out.println("#modelGroups = "+modelGroups);
			System.out.println("Errors : "+errors);
			return false;
		}
	}
	
	
	/**
	 * Export model definitions from nodelDef variable
	 */
	public void saveModelDef(File exportFile) {
		
	}
	
	
	public static void main(String[] args) {
		
		File file = new File("/home/debian7/Arbeitsfläche/ModelDef.json");
		readModelDef(file);
	}

	public LinkedHashMap<ModelType, ModelGroup> getModelDefinitions() {
		return modelDef;
	}
	
	public LinkedHashMap<ModelType, String> getModelType2ModelNameNice() {
		return modelType2ModelNameNice;
	}
	
	
	
	private static void makeMaps() {
		
		models2ClassNamespaces.clear();
		models2TagNamespaces.clear();
		modelType2ModelNameNice.clear();
		
		for (ModelType mt : modelDef.keySet()) {
				models2ClassNamespaces.put(mt, modelDef.get(mt).getClassNameSpaces());
				models2TagNamespaces.put(mt, modelDef.get(mt).getTagNameSpaces());
				modelType2ModelNameNice.put(mt, modelDef.get(mt).getNiceName());
		}
	}

	
	public LinkedHashMap<ModelType, String[]> getModels2TagNamespaces() {
		return models2TagNamespaces;
	}

	public LinkedHashMap<ModelType, String[]> getModels2ClassNamespaces() {
		return models2ClassNamespaces;
	}

}
