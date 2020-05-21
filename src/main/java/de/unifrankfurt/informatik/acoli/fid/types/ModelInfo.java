package de.unifrankfurt.informatik.acoli.fid.types;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FilenameUtils;

public class ModelInfo {
	
	private File file = null;
	private ModelType modelType = null;
	private URL url = null;
	private String name = null;
	private String fileName = null;
	private boolean active = true;
	private ModelUsage usage = null;
	private String documentationUrl = null;
	
	private final static String oliaHtmlBase = "http://www.acoli.informatik.uni-frankfurt.de/resources/olia/html/";

	
	public ModelInfo(String urlString, boolean active, ModelType modelType, ModelUsage usage, String documentationUrl) {
		try {
			this.url = new URL (urlString);
			this.fileName = new File(url.getPath()).getName();
			this.name = FilenameUtils.removeExtension(fileName).toLowerCase();
			this.file = null;
			this.active = active;
			this.modelType = modelType;
			this.usage = usage;
			this.documentationUrl = documentationUrl;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	public File getFile() {
		return file;
	}
	
	public void setFile(File file) {
		this.file = file;
		this.fileName = file.getName();
	}
	
	public ModelType getModelType() {
		return modelType;
	}
	
	public void setModelType(ModelType modelType) {
		this.modelType = modelType;
	}

	public URL getUrl() {
		return url;
	}

	public void setUrl(URL ontology) {
		this.url = ontology;
	}

	public void setOntology(String ontologyUrl) {	
		try {
			this.url = new URL (ontologyUrl);
		} catch (MalformedURLException e) {}
	}

	/**
	 * File name without extension as lower case
	 * @return 
	 */
	public String getName() {
		return name;
	}

	public String getFileName() {
		return fileName;
	}
	
	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	
	public void setModelUsage(ModelUsage usage) {
		this.usage = usage;
	}
	
	public ModelUsage getModelUsage() {
		return this.usage;
	}
	
	/**
	 *(Experimental) Get a link to http://www.acoli.informatik.uni-frankfurt.de/resources/olia/html/ .
	 * The link provides a detailed description of OLiA annotation/linking model
	 * for the model
	 * @return
	 */
	public String getOliaHtmlInfoUrl() {

		try {
			Path path = Paths.get(getUrl().getFile());
			String modelName = FilenameUtils.removeExtension(path.getFileName().toString());
			return oliaHtmlBase+path.subpath(path.getNameCount()-2, path.getNameCount()-1).toString()+"/"+modelName+".html";
		} catch (Exception e) {
		}
		
		return null;
	}
	
	public String getDocumentationUrl() {
		return documentationUrl;
	}
	
	public void setDocumentationUrl(String documentationUrl) {
		this.documentationUrl = documentationUrl;
	}
}
