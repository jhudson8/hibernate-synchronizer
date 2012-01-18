package com.hudson.hibernatesynchronizer.resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

import com.hudson.hibernatesynchronizer.Constants;
import com.hudson.hibernatesynchronizer.Plugin;
import com.hudson.hibernatesynchronizer.util.HSUtil;
import com.hudson.hibernatesynchronizer.util.Synchronizer;

public class ResourceManager {

	private static final String PATH_BASE = ".hibernateSynchronizer3";
	private static final String PATH_TEMPLATES = ".hibernateSynchronizer3/templates";
	private static final String PATH_SNIPPETS = ".hibernateSynchronizer3/snippets";
	private static final String TEMPLATE_LOC = "/templates/";
	private static final String SNIPPET_LOC = "/snippets/";

	private static String[] ALL_TEMPLATES = {
			Synchronizer.TEMPLATE_BASE_VALUE_OBJECT,
			Synchronizer.TEMPLATE_VALUE_OBJECT,
			Synchronizer.TEMPLATE_VALUE_OBJECT_PROXY,
			Synchronizer.TEMPLATE_VALUE_OBJECT_PROXY_CONTENTS,
			Synchronizer.TEMPLATE_VALUE_OBJECT_PK,
			Synchronizer.TEMPLATE_BASE_VALUE_OBJECT_PK,
			Synchronizer.TEMPLATE_BASE_ROOT_DAO,
			Synchronizer.TEMPLATE_ROOT_DAO,
			Synchronizer.TEMPLATE_BASE_DAO,
			Synchronizer.TEMPLATE_DAO
	};
	
	private static String[] ALL_SNIPPETS = {
			// TODO add all of the snippets
			"BaseDAOImports",
			"BaseDAOClassComments",
			"BaseDAOClassDefinition",
			"BaseDAOQueryNames",
			"BaseDAOInstanceMethod",
			"BaseDAORequiredMethods",
			"BaseDAOCustomContents",
			"BaseDAOFinderMethods",
			"BaseDAOActionMethods",
			"BaseRootDAOImports",
			"BaseRootDAOClassComments",
			"BaseRootDAOClassDefinition",
			"BaseRootDAORequiredMethods",
			"BaseRootDAOCustomContents",
			"BaseRootDAOFinderMethods",
			"BaseRootDAOActionMethods",
			"BaseRootDAOSessionMethods",
			"BaseValueObjectClassComments",
			"BaseValueObjectClassDefinition",
			"BaseValueObjectConstructor",
			"BaseValueObjectCustomContents",
			"BaseValueObjectEqualityMethods",
			"BaseValueObjectGetterSetter",
			"BaseValueObjectIdGetterSetter",
			"BaseValueObjectImports",
			"BaseValueObjectStaticProperties",
			"BaseValueObjectToString",
			"BaseValueObjectVariableDefinitions",
			"c_CustomProperties",
			"c_Getter",
			"c_Setter",
			"ValueObjectClassComments",
			"ValueObjectConstructor",
			"ValueObjectClassDefinition",
			"ValueObjectCustomContents",
			"ValueObjectImports",
			"ValueObjectPKConstructor",
			"ValueObjectPKCustomContents",
			"ValueObjectPKImports",
			"ValueObjectPKClassDefinition",
			"RootDAOClassComments",
			"RootDAOClassDefinition",
			"RootDAOImports",
			"DAOClassComments",
			"DAOCustomContents",
			"DAOClassDefinition",
			"DAOImports",
			"BaseValueObjectPKClassDefinition",
			"BaseValueObjectPKClassComments",
			"BaseValueObjectPKConstructor",
			"BaseValueObjectPKCustomContents",
			"BaseValueObjectPKEqualityMethods",
			"BaseValueObjectPKGetterSetter",
			"BaseValueObjectPKImports",
			"BaseValueObjectPKVariableDefinitions"
	};

	private static final Map resourceManagers = new HashMap();

	private static Map workspaceTemplatesMap;
	private static Map workspaceSnippetsMap;
	private static List workspaceTemplates;
	private static List workspaceSnippets;

	private IProject project;
	private Map projectSnippets;
	private List projectSnippetsList;
	private Map projectTemplates;
	private List projectTemplatesList;
	private List allSnippetsList;
	private Map allSnippetsMap;
	private List allTemplatesList;
	private Map allTemplatesMap;
	private List templateLocations;
	private Map templateLocationMap;
	private Properties templateParametersMap;

	/**
	 * Private constructor for project singleton functionality
	 */
	private ResourceManager () {}

	/**
	 * Return the project-level singleton ResourceManager
	 */
	public static ResourceManager getInstance (IProject project) {
		ResourceManager manager = (ResourceManager) resourceManagers.get(project.getName());
		if (null == manager) {
			manager = new ResourceManager();
			if (null == workspaceSnippets || null == workspaceTemplates) {
				reloadWorkspaceCache();
			}
			try {
				IFolder folder = project.getFolder(new Path(PATH_BASE));
				if (!folder.exists()) folder.create(true, true, null);
				folder = project.getFolder(new Path(PATH_TEMPLATES));
				if (!folder.exists()) folder.create(true, true, null);
				folder = project.getFolder(new Path(PATH_SNIPPETS));
				if (!folder.exists()) folder.create(true, true, null);
				manager.project = project;
				resourceManagers.put(project.getName(), manager);
				manager.reloadProjectCache();
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return manager;
	}

	/**
	 * Initialize workspace resources when a new version of the plug-in is being run for the first time
	 * @throws Exception
	 */
	public static void initializePluginResources (boolean devMode) throws Exception {
		reloadWorkspaceCache();
		initializePluginTemplates(devMode);
		initializePluginSnippets(devMode);
		reloadWorkspaceCache();
	}

	/**
	 * Load the templates from the jar and save them to the workspace
	 * @throws Exception
	 */
	private static void initializePluginTemplates (boolean devMode) throws Exception {
		String DEV_MODE_DIR = Platform.resolve(Plugin.getDefault().getDescriptor().getInstallURL()).getFile() + "/src" + TEMPLATE_LOC;
		File templateDirectory = getWorkspaceTemplatesDirectory();
		for (int i=0; i<ALL_TEMPLATES.length; i++) {
			Template resource = getWorkspaceTemplate(ALL_TEMPLATES[i]);
			if (null == resource || !resource.isModified()) {
				String s = null;
				if (devMode)
					s = HSUtil.getStringFromStream(new FileInputStream(new File(DEV_MODE_DIR + ALL_TEMPLATES[i] + Constants.EXTENSION_TEMPLATE)));
				else
					s = HSUtil.getStringFromStream(ResourceManager.class.getClassLoader().getResourceAsStream(TEMPLATE_LOC + ALL_TEMPLATES[i] + Constants.EXTENSION_TEMPLATE));
				File workspaceTemplateDirectory = Plugin.getDefault().getStateLocation().append(new Path(TEMPLATE_LOC)).makeAbsolute().toFile();
				File outFile = new File(workspaceTemplateDirectory.getAbsolutePath() + "/" + ALL_TEMPLATES[i] + Constants.EXTENSION_TEMPLATE);
				FileOutputStream out = new FileOutputStream(outFile);
				try {
					out.write(s.getBytes());
				}
				finally {
					if (null != out) out.close();
				}
			}
		}
	}

	public static String getTemplateContents (String templateName) throws Exception {
		if (Plugin.devMode) {
			String DEV_MODE_DIR = Platform.resolve(Plugin.getDefault().getDescriptor().getInstallURL()).getFile() + "/src/";
			return HSUtil.getStringFromStream(new FileInputStream(new File(DEV_MODE_DIR + templateName)));
		}
		else {
			return HSUtil.getStringFromStream(ResourceManager.class.getClassLoader().getResourceAsStream(templateName));
		}
	}
	
	/**
	 * Load the snippets from the jar and save them to the workspace
	 * @throws Exception
	 */
	private static void initializePluginSnippets (boolean devMode) throws Exception {
		String DEV_MODE_DIR = Platform.resolve(Plugin.getDefault().getDescriptor().getInstallURL()).getFile() + "/src" + SNIPPET_LOC;
		File snippetDirectory = getWorkspaceSnippetsDirectory();
		for (int i=0; i<ALL_SNIPPETS.length; i++) {
			Snippet resource = getWorkspaceSnippet(ALL_SNIPPETS[i]);
			if (null == resource || !resource.isModified()) {
				String s = null;
				if (devMode)
					s = HSUtil.getStringFromStream(new FileInputStream(new File(DEV_MODE_DIR + ALL_SNIPPETS[i] + Constants.EXTENSION_SNIPPET)));
				else
					s = HSUtil.getStringFromStream(ResourceManager.class.getClassLoader().getResourceAsStream(SNIPPET_LOC + ALL_SNIPPETS[i] + Constants.EXTENSION_SNIPPET));
				File workspaceTemplateDirectory = Plugin.getDefault().getStateLocation().append(new Path(SNIPPET_LOC)).makeAbsolute().toFile();
				File outFile = new File(workspaceTemplateDirectory.getAbsolutePath() + "/" + ALL_SNIPPETS[i] + Constants.EXTENSION_SNIPPET);
				FileOutputStream out = new FileOutputStream(outFile);
				try {
					out.write(s.getBytes());
				}
				finally {
					if (null != out) out.close();
				}
			}
		}
	}

	/**
	 * Reload the resource manager project cache if a change occured
	 */
	public void reloadProjectCache () {
		try {
			// project templates
			projectTemplates = new HashMap();
			projectTemplatesList = new ArrayList();
			IFolder folder = project.getFolder(new Path(PATH_TEMPLATES));
			IResource[] members = folder.members();
			for (int i=0; i<members.length; i++) {
				if (members[i] instanceof IFile) {
					IFile file = (IFile) members[i];
					if (file.getName().endsWith(Constants.EXTENSION_TEMPLATE)) {
						Template template = new Template();
						template.load(file);
						projectTemplates.put(template.getName(), template);
						projectTemplatesList.add(template);
					}
				}
			}

			// project snippets
			projectSnippets = new HashMap();
			projectSnippetsList = new ArrayList();
			folder = project.getFolder(new Path(PATH_SNIPPETS));
			members = folder.members();
			for (int i=0; i<members.length; i++) {
				if (members[i] instanceof IFile) {
					IFile file = (IFile) members[i];
					if (file.getName().endsWith(Constants.EXTENSION_SNIPPET)) {
						Snippet snippet = new Snippet();
						snippet.load(file);
						Plugin.trace("Loading project snippet: " + snippet.getName());
						projectSnippets.put(snippet.getName(), snippet);
						projectSnippetsList.add(snippet);
					}
				}
			}

			// load up template cache
			allTemplatesMap = new HashMap();
			for (Iterator i=workspaceTemplates.iterator(); i.hasNext(); ) {
				Template t = (Template) i.next();
				allTemplatesMap.put(t.getName(), t);
			}
			for (Iterator i=projectTemplates.values().iterator(); i.hasNext(); ) {
				Template t = (Template) i.next();
				allTemplatesMap.put(t.getName(), t);
			}
			allTemplatesList = new ArrayList(allTemplatesMap.size());
			for (Iterator i=allTemplatesMap.values().iterator(); i.hasNext(); ) {
				Template t = (Template) i.next();
				allTemplatesList.add(t);
			}
			Collections.sort(allTemplatesList);

			// load up snippet cache
			allSnippetsMap = new HashMap();
			for (Iterator i=workspaceSnippets.iterator(); i.hasNext(); ) {
				Snippet t = (Snippet) i.next();
				allSnippetsMap.put(t.getName(), t);
			}
			for (Iterator i=projectSnippets.values().iterator(); i.hasNext(); ) {
				Snippet t = (Snippet) i.next();
				allSnippetsMap.put(t.getName(), t);
			}
			allSnippetsList = new ArrayList(allSnippetsMap.size());
			for (Iterator i=allSnippetsMap.values().iterator(); i.hasNext(); ) {
				Snippet t = (Snippet) i.next();
				allSnippetsList.add(t);
			}
			Collections.sort(allSnippetsList);
			
			// load template locations cache
			refreshTemplateLocations();
			
			// load the statically defined template parameters
			refreshTemplateParameters();
		}
		catch (Exception e) {
			// something bad has happened
			Plugin.log(e);
			MessageDialog.openError(new Shell(), "Unrecoverable Error", e.getMessage());
		}
	}

	/**
	 * Reload the workspace project cache
	 */
	public static void reloadWorkspaceCache () {
		resourceManagers.clear();
		try {
			// workspace templates
			workspaceTemplatesMap = new HashMap();
			workspaceTemplates = new ArrayList();
			File templatesDirectory = getWorkspaceTemplatesDirectory();
			File[] files = templatesDirectory.listFiles();
			for (int i=0; i<files.length; i++) {
				File file = files[i];
				if (!file.isDirectory() && file.getName().endsWith(Constants.EXTENSION_TEMPLATE)) {
					Template template = new Template();
					template.load(file);
					workspaceTemplatesMap.put(template.getName(), template);
					workspaceTemplates.add(template);
				}
			}
			Collections.sort(workspaceTemplates);

			// workspace snippets
			workspaceSnippetsMap = new HashMap();
			workspaceSnippets = new ArrayList();
			File snippetsDirectory = getWorkspaceSnippetsDirectory();
			files = snippetsDirectory.listFiles();
			for (int i=0; i<files.length; i++) {
				File file = files[i];
				if (!file.isDirectory() && file.getName().endsWith(Constants.EXTENSION_SNIPPET)) {
					Snippet snippet = new Snippet();
					snippet.load(file);
					Plugin.trace("Loading workspace snippet: " + snippet.getName());
					workspaceSnippetsMap.put(snippet.getName(), snippet);
					workspaceSnippets.add(snippet);
				}
			}
			Collections.sort(workspaceSnippets);
		}
		catch (Exception e) {
			// something bad has happened
			Plugin.log(e);
			MessageDialog.openError(new Shell(), "Unrecoverable Error", e.getMessage());
		}
	}

	public static void restore (Resource resource) throws Exception {
		if (null == resource.getProject()) {
			if (resource instanceof Snippet) {
				String contents = null;
				if (Plugin.devMode) {
					String DEV_MODE_DIR = Platform.resolve(Plugin.getDefault().getDescriptor().getInstallURL()).getFile() + "/src" + SNIPPET_LOC;
					contents = HSUtil.getStringFromStream(new FileInputStream(new File(DEV_MODE_DIR + resource.getFileName())));
				}
				else {
					contents = HSUtil.getStringFromStream(ResourceManager.class.getClassLoader().getResourceAsStream(SNIPPET_LOC + resource.getFileName()));
				}
				File workspaceTemplateDirectory = Plugin.getDefault().getStateLocation().append(new Path(SNIPPET_LOC)).makeAbsolute().toFile();
				File outFile = new File(workspaceTemplateDirectory.getAbsolutePath() + "/" + resource.getFileName());
				FileOutputStream out = new FileOutputStream(outFile);
				try {
					out.write(contents.getBytes());
				}
				finally {
					if (null != out) out.close();
				}
			}
			else if (resource instanceof Template) {
				String contents = null;
				if (Plugin.devMode) {
					String DEV_MODE_DIR = Platform.resolve(Plugin.getDefault().getDescriptor().getInstallURL()).getFile() + "/src" + TEMPLATE_LOC;
					contents = HSUtil.getStringFromStream(new FileInputStream(new File(DEV_MODE_DIR + resource.getFileName())));
				}
				else {
					contents = HSUtil.getStringFromStream(ResourceManager.class.getClassLoader().getResourceAsStream(TEMPLATE_LOC + resource.getFileName()));
				}
				File workspaceTemplateDirectory = Plugin.getDefault().getStateLocation().append(new Path(TEMPLATE_LOC)).makeAbsolute().toFile();
				File outFile = new File(workspaceTemplateDirectory.getAbsolutePath() + "/" + resource.getFileName());
				FileOutputStream out = new FileOutputStream(outFile);
				try {
					out.write(contents.getBytes());
				}
				finally {
					if (null != out) out.close();
				}
			}
			reloadWorkspaceCache();
		}
	}

	/**
	 * Return the requestes snippet by looking in the project and then the workspace
	 * @param name the snippet name
	 */
	public Snippet getSnippet (String name) {
		return (Snippet) allSnippetsMap.get(name);
	}

	/**
	 * Return all snippets associated with the project and workspace
	 * @return a list of Snippet objects
	 */
	public List getSnippets () {
		return allSnippetsList;
	}

	/**
	 * Return all snippets associated with the project
	 * @return a list of Snippet objects
	 */
	public List getProjectSnippets () {
		return projectSnippetsList;
	}

	/**
	 * Return the requested template by looking in the project and then in the workspace
	 * @param name the template name
	 */
	public Template getTemplate (String name) {
		return (Template) allTemplatesMap.get(name);
	}

	/**
	 * Rename the given workspace resource
	 * @param resource the resource
	 * @param newName the new resource name
	 * @throws Exception
	 */
	public static void renameWorkspaceResource (Resource resource, String newName) throws Exception {
		resource.rename(newName);
		reloadWorkspaceCache();
	}

	/**
	 * Rename the given project resource
	 * @param resource the resource
	 * @param newName the new resource name
	 * @throws Exception
	 */
	public void rename (Resource resource, String newName) throws Exception {
		resource.rename(newName);
		reloadProjectCache();
	}

	/**
	 * Delete the given workspace resource
	 * @param resource
	 * @throws Exception
	 */
	public static void deleteWorkspaceResource (Resource resource) throws Exception {
		resource.delete();
		reloadWorkspaceCache();
	}

	/**
	 * Delete the given resource
	 * @param resource
	 * @throws Exception
	 */
	public void delete (Resource resource) throws Exception {
		resource.delete();
		reloadProjectCache();
	}

	/**
	 * Save the new resource as a workspace resource
	 * @param resource
	 * @throws Exception
	 */
	public static void saveWorkspaceResource (Resource resource) throws Exception {
		resource.save();
		reloadWorkspaceCache();
	}

	/**
	 * Save the new resource as a project resource
	 * @param resource
	 * @throws Exception
	 */
	public void save (Resource resource) throws Exception {
		resource.save();
		reloadProjectCache();
	}

	/**
	 * Add a statically defined context parameter for this project
	 * @param name the parameter name
	 * @param value the parameter value
	 */
	public void addTemplateParameter (String name, String value) {
		templateParametersMap.put(name, value);
		persistTemplateParameters();
	}

	/**
	 * Update the given template parameter
	 * @param name the parameter name
	 * @param value the new parameter value
	 */
	public void updateTemplateParameter (String name, String value) {
		addTemplateParameter(name, value);
	}

	/**
	 * Remove the template parameter with the name given
	 * @param name
	 */
	public void deleteTemplateParameter (String name) {
		templateParametersMap.remove(name);
		persistTemplateParameters();
	}
	
	/**
	 * Return the template parameter value related to the given name or null if it does not exist
	 * @param name the parameter name
	 */
	public String getTemplateParameter (String name) {
		return templateParametersMap.getProperty(name);
	}

	/**
	 * Return all project template parameters
	 */
	public Properties getTemplateParameters () {
		return templateParametersMap;
	}

	/**
	 * Return the names of all template parameters sorted in alphabetical order
	 * @return a List of String objects
	 */
	public List getTemplateParameterNames () {
		List sortedNames = new ArrayList(templateParametersMap.size());
		for (Iterator i=templateParametersMap.keySet().iterator(); i.hasNext(); ) {
			sortedNames.add(i.next());
		}
		Collections.sort(sortedNames);
		return sortedNames;
	}
	
	private void persistTemplateParameters () {
		StringBuffer sb = new StringBuffer();
		for (Iterator i=templateParametersMap.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry entry = (Map.Entry) i.next();
			if (sb.length() > 0) sb.append("&");
			try {
				sb.append(URLEncoder.encode((String) entry.getKey(), "UTF-8"));
				sb.append("=");
				sb.append(URLEncoder.encode((String) entry.getValue(), "UTF-8"));
			}
			catch (UnsupportedEncodingException e) {
				Plugin.log(e);
			}
		}
		Plugin.setProperty(project, Constants.PROP_TEMPLATE_PARAMETERS, sb.toString());
	}
	
	private void refreshTemplateParameters () {
		templateParametersMap = new Properties();
		String ptString = Plugin.getProperty(project, Constants.PROP_TEMPLATE_PARAMETERS);
		if (null != ptString && ptString.trim().length() > 0) {
			StringTokenizer st = new StringTokenizer(ptString, "&");
			while (st.hasMoreTokens()) {
				String s = st.nextToken();
				int index = s.indexOf("=");
				try {
					String key = URLDecoder.decode(s.substring(0, index), "UTF-8");
					String value = URLDecoder.decode(s.substring(index+1, s.length()), "UTF-8");
					templateParametersMap.put(key, value);
				}
				catch (UnsupportedEncodingException e) {
					Plugin.log(e);
				}
			}
		}
	}

	/**
	 * Return all template (enabled and disabled) for the current project
	 * @return a List of TemplateLocation objects
	 */
	public List getTemplateLocations () {
		return templateLocations;
	}

	/**
	 * Add a new template location
	 * @param templateLocation
	 */
	public void addTemplateLocation (TemplateLocation templateLocation) {
		templateLocations.add(templateLocation);
		templateLocationMap.put(templateLocation.getTemplate().getName(), templateLocation);
		persistTemplateLocations();
	}

	/**
	 * Update the given template location.  It must be the original one (using ==) retrieved from the ResourceManager
	 * @param templateLocation
	 */
	public void updateTemplateLocation (TemplateLocation templateLocation) {
		for (Iterator i=templateLocationMap.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry entry = (Map.Entry) i.next();
			TemplateLocation templateLocation2 = (TemplateLocation) entry.getValue();
			if (templateLocation2 == templateLocation) {
				templateLocationMap.remove(entry.getKey());
				break;
			}
		}
		templateLocationMap.put(templateLocation.getTemplate().getName(), templateLocation);
		persistTemplateLocations();
	}

	/**
	 * Delete the given template location
	 * @param templateLocation
	 */
	public void deleteTemplateLocation (TemplateLocation templateLocation) {
		templateLocations.remove(templateLocation);
		templateLocationMap.remove(templateLocation.getTemplate().getName());
		persistTemplateLocations();
	}

	private void persistTemplateLocations () {
		StringBuffer ptString = new StringBuffer();
		boolean started = false;
		for (Iterator i=templateLocations.iterator(); i.hasNext(); ) {
			TemplateLocation templateLocation = (TemplateLocation) i.next();
			if (started) ptString.append(";");
			else started = true;
			ptString.append(templateLocation.toString());
		}
		Plugin.setProperty(project, Constants.PROP_PROJECT_TEMPLATE_LOCATIONS, ptString.toString());
	}
	
	private void refreshTemplateLocations () {
		templateLocationMap = new HashMap();
		templateLocations = new ArrayList();

		String ptString = Plugin.getProperty(project, Constants.PROP_PROJECT_TEMPLATE_LOCATIONS);
		if (null != ptString) {
			StringTokenizer st = new StringTokenizer(ptString, ";");
			while (st.hasMoreTokens()) {
				try {
					TemplateLocation templateLocation = new TemplateLocation(st.nextToken(), project);
					if (templateLocation.isValid()) {
						templateLocations.add(templateLocation);
						templateLocationMap.put(templateLocation.getTemplate().getName(), templateLocation);
					}
				}
				catch (IOException ioe) {
					Plugin.log(ioe);
				}
			}
		}
	}

	public TemplateLocation getTemplateLocation (String templateName) {
		return (TemplateLocation) templateLocationMap.get(templateName);
	}

	public static File getWorkspaceTemplatesDirectory () {
		File templateDirectory = Plugin.getDefault().getStateLocation().append("templates").makeAbsolute().toFile();
		if (!templateDirectory.exists()) templateDirectory.mkdir();
		return templateDirectory;
	}

	public static File getWorkspaceSnippetsDirectory () {
		File snippetDirectory = Plugin.getDefault().getStateLocation().append("snippets").makeAbsolute().toFile();
		if (!snippetDirectory.exists()) snippetDirectory.mkdir();
		return snippetDirectory;
	}

	public static List getWorkspaceSnippets () {
		return workspaceSnippets;
	}
	
	public static Snippet getWorkspaceSnippet (String name) {
		return (Snippet) workspaceSnippetsMap.get(name);
	}

	public static List getWorkspaceTemplates () {
		return workspaceTemplates;
	}

	public static List getNonRequiredWorkspaceTemplates () {
		List nonRequiredWorkspaceTemplates = new ArrayList();
		for (Iterator i=getWorkspaceTemplates().iterator(); i.hasNext(); ) {
			Template t = (Template) i.next();
			if (!isRequiredResource(t)) nonRequiredWorkspaceTemplates.add(t);
		}
		return nonRequiredWorkspaceTemplates;
	}

	public List getProjectTemplates () {
		return projectTemplatesList;
	}
	
	public List getTemplates () {
		return allTemplatesList;
	}

	public List getNonRequiredTemplates () {
		List nonRequiredTemplates = new ArrayList();
		for (Iterator i=getTemplates().iterator(); i.hasNext(); ) {
			Template t = (Template) i.next();
			if (!isRequiredResource(t)) nonRequiredTemplates.add(t);
		}
		return nonRequiredTemplates;
	}

	public static Template getWorkspaceTemplate (String name) {
		return (Template) workspaceTemplatesMap.get(name);
	}

	public static boolean isRequiredResource (Resource resource) {
		if (resource instanceof Snippet) {
			for (int i=0; i<ALL_SNIPPETS.length; i++) {
				if (resource.getName().equals(ALL_SNIPPETS[i])) return true;
			}
			return false;
		}
		else if (resource instanceof Template) {
			for (int i=0; i<ALL_TEMPLATES.length; i++) {
				if (resource.getName().equals(ALL_TEMPLATES[i])) return true;
			}
			return false;
		}
		else return false;
	}
}