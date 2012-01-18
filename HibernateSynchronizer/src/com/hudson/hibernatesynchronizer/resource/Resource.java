package com.hudson.hibernatesynchronizer.resource;

import java.io.File;
import java.util.Map;

import org.apache.velocity.context.Context;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.swt.widgets.Composite;

public interface Resource {

	/**
	 * Return the name of the resource
	 */
	public String getName();

	/**
	 * Return the project of the resource or null if it belongs to the workspace
	 */
	public IProject getProject();

	/**
	 * Return the content of the resource
	 */
	public String getContent();

	/**
	 * Load the resource from the given file
	 * @param file
	 */
	public void load (IFile file) throws Exception;

	/**
	 * Load the resource from the given file
	 * @param fileName the fileName
	 * @param is the file contents
	 */
	public void load (File file) throws Exception;

	/**
	 * Save the template
	 * @param file
	 * @throws Exception
	 */
	public void save () throws Exception;
	
	/**
	 * Rename the resource to the new name given
	 * @param newName the new resource name
	 * @throws Exception
	 */
	public void rename (String newName) throws Exception;

	/**
	 * Delete this resource
	 * @throws Exception
	 */
	public void delete () throws Exception;

	/**
	 * Return the results of this content with the Context given
	 * @param context
	 * @return the results
	 */
	public String merge (Context context) throws Exception;

	/**
	 * Return true if the resource has been modified from the original value and false if not
	 */
	public boolean isModified();

	/**
	 * Set whether this resource has been modified or not
	 * @param modified
	 */
	public void setModified (boolean modified);
	
	/**
	 * Return the description of this resource
	 */
	public String getDescription ();
	
	/**
	 * Set the description of this resource
	 */
	public void setDescription (String description);

	/**
	 * Return the file associated with this resource
	 */
	public File getFile ();
	
	/**
	 * Return the Eclipse file associated with this resource or null if it is a workspace resource
	 */
	public IFile getIFile ();

	/**
	 * Return the file name associated with this resource
	 */
	public String getFileName ();
	
	/**
	 * Return the actual contents of the underlying resource
	 */
	public String getFormattedFileContents ();

	/**
	 * Add any custom elements to the composite (2 column grid)
	 * @param composite
	 * @return the properties to reference the added elements
	 */
	public Map addToEditor (Composite composite, Object listener);

	/**
	 * Evaluate the editor content and properties to return the actual contents to persist
	 * @param entryMap
	 * @param contents
	 */
	public String evaluate (Map entryMap, String contents);
}
