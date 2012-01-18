package com.hudson.hibernatesynchronizer.resource;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.Context;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.widgets.Composite;

import com.hudson.hibernatesynchronizer.Constants;
import com.hudson.hibernatesynchronizer.Plugin;
import com.hudson.hibernatesynchronizer.util.HSUtil;

public abstract class AbstractResource implements Resource, Comparable {

	private char META_DATA_CHAR = ':';
	private char META_DATA_SEPARATOR_CHAR = '-';
	private String PROP_MODIFIED = "Modified";
	private String PROP_DESCRIPTION = "Description";

	private IFile file;
	private File fileSystemFile;
	private IProject project;
	private String filePath = Constants.FOLDER_HS + "/" + getResourceDirectory() + "/";
	private String name;
	private String content;
	private String description;
	private boolean modified;
	
	/* (non-Javadoc)
	 * @see xmlplugin.resource.IResource#getName()
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name of this resource
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/* (non-Javadoc)
	 * @see xmlplugin.resource.IResource#getProject()
	 */
	public IProject getProject() {
		return project;
	}

	/**
	 * Set the project of this resource
	 * @param project the project to set
	 */
	public void setProject(IProject project) {
		this.project = project;
	}

	/* (non-Javadoc)
	 * @see xmlplugin.resource.IResource#getContent()
	 */
	public String getContent() {
		if (null == content) return "";
		else return content;
	}

	/**
	 * Set the content for this resource
	 * @param content the content to set
	 */
	public void setContent(String content) {
		this.content = content;
	}

	/**
	 * Set whether this resource has been modified
	 * @param modified
	 */
	public void setModified (boolean modified) {
		this.modified = modified;
	}

	/* (non-Javadoc)
	 * @see xmlplugin.resource.IResource#isModified()
	 */
	public boolean isModified() {
		return modified;
	}

	/* (non-Javadoc)
	 * @see xmlplugin.resource.IResource#merge(org.apache.velocity.context.Context)
	 */
	public String merge(Context context) throws Exception {
		StringWriter sw = new StringWriter();
		Constants.templateGenerator.evaluate(context, sw, Velocity.class.getName(), content);
		return sw.toString();
	}

	/* (non-Javadoc)
	 * @see xmlplugin.resource.IResource#delete()
	 */
	public void delete() throws Exception {
		if (null != file)
			file.delete(true, true, null);
		else if (null != fileSystemFile)
			fileSystemFile.delete();
	}

	/* (non-Javadoc)
	 * @see xmlplugin.resource.IResource#rename(java.lang.String)
	 */
	public void rename(String newName) throws Exception {
		// TODO fix rename method
		file.move(new Path(filePath + "/" + newName), true, false, null);
	}

	/* (non-Javadoc)
	 * @see xmlplugin.resource.IResource#save()
	 */
	public void save() throws Exception {
		String fileName = getFileName();
		String fileContents = getFormattedFileContents();
		if (null == fileContents) fileContents = "";
		if (null == project) {
			File workspaceTemplateDirectory = Plugin.getDefault().getStateLocation().append(new Path(getResourceDirectory())).makeAbsolute().toFile();
			fileSystemFile = new File(workspaceTemplateDirectory.getAbsolutePath() + "/" + fileName);
			FileOutputStream out = new FileOutputStream(fileSystemFile);
			try {
				out.write(fileContents.getBytes());
			}
			finally {
				if (null != out) out.close();
			}
		}
		else {
			file = project.getFile(new Path(filePath + fileName));
			if (!file.exists()) file.create(new ByteArrayInputStream(fileContents.getBytes()), true, null);
			else file.setContents(new ByteArrayInputStream(fileContents.getBytes()), true, true, null);
		}
	}

	/* (non-Javadoc)
	 * @see xmlplugin.resource.IResource#load(org.eclipse.core.resources.IFile)
	 */
	public void load(IFile file) throws Exception {
		this.file = file;
		this.project = file.getProject();
		this.filePath = file.getLocation().toString().replace('\\', '/');
		this.filePath = filePath.substring(0, filePath.lastIndexOf('/'));
		load(file.getName(), file.getContents());
	}

	/* (non-Javadoc)
	 * @see xmlplugin.resource.IResource#load(java.io.File)
	 */
	public void load(File file) throws Exception {
		this.fileSystemFile = file;
		this.filePath = file.getAbsolutePath().toString().replace('\\', '/');
		this.filePath = filePath.substring(0, filePath.lastIndexOf('/'));
		load(file.getName(), new FileInputStream(file));
	}

	/* (non-Javadoc)
	 * @see xmlplugin.resource.IResource#load(java.lang.String, java.io.InputStream)
	 */
	public void load(String fileName, InputStream is) throws Exception {
		onLoad(fileName, is);
		evaluateFileName (fileName);
		evaluateFileContent (is);
	}

	/**
	 * Called when the resource is loaded from a file
	 */
	protected void onLoad (String fileName, InputStream fileContents) {
	}

	/**
	 * Called to set the name based on the file name on load
	 * @param file
	 * @throws Exception
	 */
	protected void evaluateFileName (String fileName) throws Exception {
		String s = URLDecoder.decode(fileName, "UTF-8");
		int index = s.indexOf('.');
		if (index > 0) s = s.substring(0, index);
		setName(s);
	}

	/**
	 * Called to set the template contents based on the file contents
	 * @param contents the file contents
	 * @throws Exception
	 */
	protected void evaluateFileContent (InputStream contents) throws Exception {
		String s = HSUtil.getStringFromStream(contents);
		int startIndex = 0;
		Properties properties = new Properties();
		int currentIndex = 0;
		boolean foundMetaData = false;
		while (s.length() > currentIndex+1) {
			if (s.charAt(currentIndex) == META_DATA_CHAR) {
				int newIndex = s.indexOf('\n', currentIndex);
				boolean keepGoing = true;
				if (newIndex < 0) {
					newIndex = s.length();
					keepGoing = false;
					currentIndex = s.length()+1;
				}
				String propertyContent = s.substring(currentIndex+1, newIndex).trim();
				properties.load(new ByteArrayInputStream(propertyContent.getBytes()));
				currentIndex = newIndex+1;
				foundMetaData = true;
				if (!keepGoing) break;
			}
			else if (s.charAt(currentIndex) == META_DATA_SEPARATOR_CHAR) {
				currentIndex = s.indexOf('\n', currentIndex);
				if (currentIndex <= 0) currentIndex = s.length()+1;
				foundMetaData = true;
				break;
			}
			else break;
		}
		if (foundMetaData && currentIndex < s.length()) {
			s = s.substring(currentIndex+1, s.length());
		}				
		this.content = s;
		s = properties.getProperty(PROP_MODIFIED);
		if (null != s && s.equals(Boolean.TRUE.toString())) setModified(true);
		setDescription(properties.getProperty(PROP_DESCRIPTION));
		evaluateMetaData(properties);
	}

	protected abstract void evaluateMetaData (Properties properties);

	public String getFileName () {
		return getFormattedFileName() + getFileExtension();
	}

	/**
	 * Return the formatted name of the file that this relates to
	 */
	protected String getFormattedFileName () {
		try {
			return URLEncoder.encode(getName(), "UTF-8");
		}
		catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * @return Returns the description.
	 */
	public String getDescription() {
		return description;
	}
	/**
	 * @param description The description to set.
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	
	/**
	 * Return the contents as how then should be saved to a file
	 */
	public String getFormattedFileContents () {
		Properties props = getMetaData();
		if (null == props) props = new Properties();
		if (isModified()) props.put(PROP_MODIFIED, Boolean.TRUE);
		if (null != getDescription()) props.put(PROP_DESCRIPTION, getDescription());
		StringBuffer sb = new StringBuffer();
		for (Iterator i=props.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry entry = (Map.Entry) i.next();
			sb.append(META_DATA_CHAR);
			sb.append(entry.getKey() + "=" + entry.getValue() + "\n");
		}
		sb.append(META_DATA_SEPARATOR_CHAR + "\n");
		sb.append(getContent());
		return sb.toString();
	}

	protected Properties getMetaData () {
		return null;
	}

	/**
	 * Return the Eclipse file associated with this resource
	 */
	public IFile getIFile() {
		return file;
	}
	/**
	 * Return the filesystem file associated with this resource
	 */
	public File getFile () {
		return fileSystemFile;
	}

	protected abstract String getResourceDirectory ();

	protected abstract String getFileExtension ();

	/**
	 * @see com.hudson.hibernatesynchronizer.resource.Resource#addToEditor(org.eclipse.swt.widgets.Composite)
	 */
	public Map addToEditor(Composite composite, Object listener) {
		return null;
	}

	/**
	 * @see com.hudson.hibernatesynchronizer.resource.Resource#evaluate(java.util.Map, java.lang.String)
	 */
	public String evaluate(Map entryMap, String contents) {
		evaluateEditorProperties(entryMap);
		setContent(contents);
		return getFormattedFileContents();
	}

	protected void evaluateEditorProperties (Map entryMap) {}

	public int compareTo(Object o) {
		if (o == null) return 0;
		else return getName().compareTo(((Resource) o).getName());
	}
}