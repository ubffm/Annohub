package de.unifrankfurt.informatik.acoli.fid.types;
/**
 * Update strategy : <br>
 * ALL updates new files and already seen files <br>
 * CHANGED updates new files and already seen files that have changed <br>
 * NEW update only new files
 * @author frank
 *
 */
public enum UpdatePolicy {
	
	UPDATE_ALL,				// Update all resources in the database (new or changed)
	UPDATE_ALLL,			// like above but also update the linghub RDF dump
	UPDATE_CHANGED,			// Update all changed and add all new resources
	UPDATE_NEW				// Add only those resource that are not in the database
	
}
