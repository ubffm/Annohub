package de.unifrankfurt.informatik.acoli.fid.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import de.unifrankfurt.informatik.acoli.fid.exec.Executer;

public class Utils {
	

	/**
	 * Write string to file. If file not exists it will be created.
	 * @param file
	 * @param fileContent
	 * @return success
	 */
	public static boolean writeFile(File file, String fileContent) {
		try {
			file.createNewFile();	
			FileWriter writer = new FileWriter(file);
            BufferedWriter bw = new BufferedWriter(writer);
            bw.write(fileContent);
            bw.close();
            System.out.println("Saving "+file.getAbsolutePath()+" finished successfull !");
            return true;
            
    } catch (IOException e) {
        	Utils.debug("File : "+file.getAbsolutePath());
            System.err.format("IOException: %s%n", e);
	} catch (Exception e) {
		e.printStackTrace();
	}
		return false;
	}
	
	

	/**
	 * Encodes a string with sha256 hash function. If the function is not supported on a system, the vanilla
	 * java <code>hashCode()</code> function is used.
	 * @param stringToHash the String that should be hashed
	 * @return the hash Value in a string representation
	 */
	 public static String sha256(String stringToHash) {
	 	String hashedString;
	 	try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] hash = md.digest(stringToHash.getBytes(StandardCharsets.UTF_8));
			hashedString = Base64.getEncoder().encodeToString(hash);
		} catch (NoSuchAlgorithmException e) {
	 		int hash = stringToHash.hashCode();
	 		hashedString = Integer.toString(hash);
		 }
	 	return hashedString;
	 }
	 
	 
	 
	 /**
		 * If the string contains "n/a" then return the empty string.
		 * @param s
		 * @param filterText
		 * @return filtered string
		 */
	public static String filterNa(String s) {
			if (s.toLowerCase().contains("n/a")) return "";
			else
			return s;
	}
	
	
	public static void debug(Object s) {
		if (Executer.getFidConfig().getBoolean("RunParameter.debugOutput") == true) {
			System.out.println(s);
		}
	}
	
	public static void debugNor(Object s) {
		if (Executer.getFidConfig().getBoolean("RunParameter.debugOutput") == true) {
			System.out.print(s);
		}
	}
	
}
