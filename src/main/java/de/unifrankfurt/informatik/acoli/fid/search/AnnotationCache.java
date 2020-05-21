package de.unifrankfurt.informatik.acoli.fid.search;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;

import org.apache.tinkerpop.gremlin.structure.Vertex;

public class AnnotationCache {
	
	private HashSet <String> tagDefinitions = new HashSet<String>();
	private HashSet <String> classDefinitions = new HashSet<String>();
	private GWriter writer;
	
	
	public AnnotationCache(GWriter writer) {
		
		this.writer = writer;
		update();
	}
	
	
	public void update() {
		
		System.out.println("Loading tag definitions ...");
		ArrayList <Vertex> tagVertices = writer.getQueries().getTagNodes();
		String tag="";
		String tagClass="";
		for (Vertex v : tagVertices) {
			tag = "";tagClass="";
			tag = v.value(GWriter.TagTag);
			tagClass = v.value(GWriter.TagClass);
			//if (tag.contains(":")) {
			//System.out.println(tag);
			tagDefinitions.add(tag);
			classDefinitions.add(tagClass);
			//}
		}

		System.out.println("Tag definitions : "+tagDefinitions.size());
		
		System.out.println("Loading class definitions ...");
		ArrayList <Vertex> classVertices = writer.getQueries().getClassNodes();
		
		for (Vertex v : classVertices) {
			String c = v.value(GWriter.ClassClass);
			try {
				URL url = new URL(c);
				classDefinitions.add(c);
				//System.out.println(c);
			} catch (MalformedURLException e) {}
		}

		System.out.println("Class definitions : "+classDefinitions.size());
	}
	
	
	public boolean isAnnotationTag(String token) {
		
		if (tagDefinitions.contains(token) || isRegexToken(token)) return true;
		else 
		return false;
	}
	
	
	public boolean isAnnotationClass(String token) {
		
		if (classDefinitions.contains(token)) return true;
		else
		return false;
	}
	
	private boolean isRegexToken(String token) {
		
		if (token.contains("$") ||
			token.contains("*") ||
			token.contains("^") ||
			token.contains(".") ||
			token.contains("[") ||
			token.contains("|")) return true;
		else
		return false;
	}


	public HashSet<String> getTagDefinitions() {
		return tagDefinitions;
	}


	public HashSet<String> getClassDefinitions() {
		return classDefinitions;
	}

	
}
