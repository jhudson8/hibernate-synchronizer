package com.hudson.hibernatesynchronizer.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.Context;
import org.eclipse.core.internal.resources.ResourceException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

import com.hudson.hibernatesynchronizer.Constants;
import com.hudson.hibernatesynchronizer.Plugin;
import com.hudson.hibernatesynchronizer.mapping.HibernateClass;
import com.hudson.hibernatesynchronizer.mapping.HibernateClassCollectionProperty;
import com.hudson.hibernatesynchronizer.mapping.HibernateComponentClass;
import com.hudson.hibernatesynchronizer.mapping.HibernateDocument;
import com.hudson.hibernatesynchronizer.mapping.HibernateMappingManager;
import com.hudson.hibernatesynchronizer.resource.ResourceManager;
import com.hudson.hibernatesynchronizer.resource.Snippet;
import com.hudson.hibernatesynchronizer.resource.SnippetContext;
import com.hudson.hibernatesynchronizer.resource.Template;
import com.hudson.hibernatesynchronizer.resource.TemplateLocation;


public class Synchronizer implements IWorkspaceRunnable {

	public static final String META_AUTO_SYNC = "sync";

	public static final String PARAM_CLASS = "class";
	public static final String PARAM_CONSTRUCTOR = "constructor";
	public static final String PARAM_CONTENT = "content";
	public static final String PARAM_CUSTOM_PLACEHOLDER = "custom_placeholder";
	public static final String PARAM_PACKAGE = "package";
	public static final String PARAM_CLASS_NAME = "className";
	public static final String PARAM_PATH = "path";
	public static final String PARAM_FILE_NAME = "fileName";
	public static final String PARAM_DOCUMENTS = "documents";
	public static final String PARAM_DOCUMENT = "document";
	public static final String PARAM_FILES = "files";
	public static final String PARAM_FILE = "file";
	public static final String PARAM_EXCEPTION_CLASS = "exceptionClass";
	public static final String PARAM_CONTEXT_OBJECT = "obj";
	public static final String PARAM_SNIPPET = "snippet";
	public static final String PARAM_NOW = "now";
	public static final String PARAM_PROJECT = "project";
	public static final String PARAM_UTIL = "util";

	public static final String SETUP_SNIPPET_CLASS = "SetupClass";

	public static final String MESSAGE_WARNING = "WARNING:";
	public static final String MESSAGE_ERROR = "ERROR:";
	public static final String MESSAGE_FATAL = "FATAL:";

	public static final String TEMPLATE_BASE_VALUE_OBJECT = "BaseValueObject";
	public static final String TEMPLATE_VALUE_OBJECT = "ValueObject";
	public static final String SNIPPET_VALUE_OBJECT_CONSTRUCTOR = "ValueObjectConstructor";
	public static final String TEMPLATE_VALUE_OBJECT_PROXY = "ValueObjectProxy";
	public static final String TEMPLATE_VALUE_OBJECT_PROXY_CONTENTS = "ValueObjectProxyContents";
	public static final String TEMPLATE_VALUE_OBJECT_PK = "ValueObjectPK";
	public static final String SNIPPET_VALUE_OBJECT_PK_CONSTRUCTOR = "ValueObjectPKConstructor";
	public static final String TEMPLATE_BASE_VALUE_OBJECT_PK = "BaseValueObjectPK";
	public static final String TEMPLATE_BASE_ROOT_DAO = "BaseRootDAO";
	public static final String TEMPLATE_ROOT_DAO = "RootDAO";
	public static final String TEMPLATE_BASE_DAO = "BaseDAO";
	public static final String TEMPLATE_DAO = "DAO";
	
	public static final String EXTENSION_JAVA = ".java";
	
	public static final String MARKER_CONSTRUCTOR = "CONSTRUCTOR MARKER";
	public static final String MARKER_GENERATED_CONTENT = "GENERATED CONTENT MARKER";
	
	public static final int DIRECTIVE_STOP_PROCESSING_GLOBAL = 0;
	public static final int DIRECTIVE_STOP_PROCESSING_LOCAL = 1;
	public static final int DIRECTIVE_KEEP_PROCESSING = 2;

	private HibernateDocument[] documents;
	private boolean force;
	private IProgressMonitor progressMonitor;
	private Shell shell;
	private Context context;

	// cache
	private IJavaProject javaProject;
	boolean valueObjectGenerationEnabled;
	boolean daoGenerationEnabled;
	boolean customGenerationEnabled;

	public Synchronizer (
			HibernateDocument document,
			boolean force,
			IProgressMonitor progressMonitor,
			Shell shell) {
		this.documents = new HibernateDocument[]{document};
		this.force = force;
		this.progressMonitor = progressMonitor;
		this.shell = shell;
		loadCache();
	}

	public Synchronizer (
			HibernateDocument[] documents,
			boolean force,
			IProgressMonitor progressMonitor,
			Shell shell) {
		this.documents = documents;
		this.force = force;
		this.progressMonitor = progressMonitor;
		this.shell = shell;
		loadCache();
	}

	/**
	 * Load internal cache of stuff before any processing can take place
	 */
	private void loadCache () {
		if (null != documents && documents.length > 0) {
			javaProject = JavaCore.create(documents[0].getFile().getProject());
			valueObjectGenerationEnabled = Plugin.getBooleanProperty(javaProject.getProject(), Constants.PROP_GENERATION_VALUE_OBJECT_ENABLED, true);
			daoGenerationEnabled = Plugin.getBooleanProperty(javaProject.getProject(), Constants.PROP_GENERATION_DAO_ENABLED, true);
			customGenerationEnabled = Plugin.getBooleanProperty(javaProject.getProject(), Constants.PROP_GENERATION_CUSTOM_ENABLED, true);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IWorkspaceRunnable#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void run(IProgressMonitor monitor) throws CoreException {
		this.progressMonitor = monitor;
		try {
			synchronize();
		}
		catch (Throwable t) {
			Plugin.log(t);
			throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, IStatus.OK, t.getMessage(), t));
		}
	}

	/**
	 * Synchronize all the class templates asynchronously
	 * @throws CoreException
	 */
	public void synchronizeASync () {
		try {
			// make sure we have all the project source locations available before we spawn a thread
			HSUtil.getProjectRoot(javaProject, shell);
			if (customGenerationEnabled) {
				List templateLocations = ResourceManager.getInstance(javaProject.getProject()).getTemplateLocations();
				for (Iterator i=templateLocations.iterator(); i.hasNext(); ) {
					TemplateLocation templateLocation = (TemplateLocation) i.next();
					HSUtil.getProjectRoot(JavaCore.create(templateLocation.getOutputProject()), shell);
				}
			}
		}
		catch (Exception e) {
			Plugin.log(e);
			if (null == shell) shell = new Shell();
			MessageDialog.openError(shell, "Synchronization Error", e.getMessage());
			return;
		}
		Runner runner = new Runner(this);
		runner.start();
	}

	/**
	 * Synchronize all the class templates synchronously
	 * @throws CoreException
	 */
	public void synchronize () throws CoreException {
		Object contextObject = getContextObject(documents[0].getFile().getProject());
		if (null != progressMonitor && progressMonitor.isCanceled()) return;
		context = Synchronizer.getDefaultContext(javaProject, contextObject);
		context.put(PARAM_FILES, HibernateMappingManager.getInstance(documents[0].getFile().getProject()).getFiles());
		context.put(PARAM_DOCUMENTS, HibernateMappingManager.getInstance(documents[0].getFile().getProject()).getDocuments());
		
		for (int i=0; i<documents.length; i++) {
			HibernateDocument hd = documents[i];
			for (Iterator iter=hd.getClasses().iterator(); iter.hasNext(); ) {
				if (null == progressMonitor || !progressMonitor.isCanceled()) {
					HibernateClass hc = (HibernateClass) iter.next();
					synchronizeClass(javaProject, hc, valueObjectGenerationEnabled, daoGenerationEnabled, customGenerationEnabled, hd.getFile().getName(), context, force, hd.getFile(), progressMonitor, shell);
					long freeMemory = Runtime.getRuntime().freeMemory();
					if (freeMemory < 2000000) {
						// we need to clean up to keep going
						System.gc();
						int loopCount = 0;
						if (null != progressMonitor) progressMonitor.subTask("Cleaning up resources, current free memory: " + freeMemory / 1024 + " kb");
						while (freeMemory < 2000000) {
							try {
								Thread.sleep(2000);
							}
							catch (Exception e) {}
							if (null != progressMonitor && progressMonitor.isCanceled()) return;
							freeMemory = Runtime.getRuntime().freeMemory();
							progressMonitor.subTask("Cleaning up resources, current free memory: " + freeMemory / 1024 + " kb");
							System.gc();
							if (loopCount++ > 6) throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, IStatus.OK, hd.getFile().getName() + ":\nThe synchronization was halted due to low available memory\nConsider synchronizing less files at a time", null));
							if (loopCount > 3) {
								// try to clear the context
								context = Synchronizer.getDefaultContext(javaProject, contextObject);
								context.put(PARAM_FILES, HibernateMappingManager.getInstance(documents[0].getFile().getProject()).getFiles());
								context.put(PARAM_DOCUMENTS, HibernateMappingManager.getInstance(documents[0].getFile().getProject()).getDocuments());
							}
						}
					}
				}
			}
		}
	}
	
	public void removeFiles (boolean removeMappingFile) throws CoreException {
		Object contextObject = getContextObject(documents[0].getFile().getProject());
		if (null != progressMonitor && progressMonitor.isCanceled()) return;
		context = Synchronizer.getDefaultContext(javaProject, contextObject);
		context.put(PARAM_FILES, HibernateMappingManager.getInstance(documents[0].getFile().getProject()).getFiles());
		context.put(PARAM_DOCUMENTS, HibernateMappingManager.getInstance(documents[0].getFile().getProject()).getDocuments());

		for (int i=0; i<documents.length; i++) {
			HibernateDocument hd = documents[i];
			for (Iterator iter=hd.getClasses().iterator(); iter.hasNext(); ) {
				if (null == progressMonitor || !progressMonitor.isCanceled()) {
					HibernateClass hc = (HibernateClass) iter.next();
					removeClassFiles(javaProject, hc, context, hd.getFile().getName(), hd.getFile(), progressMonitor, shell);
					long freeMemory = Runtime.getRuntime().freeMemory();
					if (freeMemory < 2000000) {
						// we need to clean up to keep going
						System.gc();
						int loopCount = 0;
						if (null != progressMonitor) progressMonitor.subTask("Cleaning up resources, current free memory: " + freeMemory / 1024 + " kb");
						while (freeMemory < 2000000) {
							try {
								Thread.sleep(2000);
							}
							catch (Exception e) {}
							if (null != progressMonitor && progressMonitor.isCanceled()) return;
							freeMemory = Runtime.getRuntime().freeMemory();
							progressMonitor.subTask("Cleaning up resources, current free memory: " + freeMemory / 1024 + " kb");
							System.gc();
							if (loopCount++ > 6) throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, IStatus.OK, "The deletion was halted due to low available memory", null));
							if (loopCount > 3) {
								// try to clear the context
								context = Synchronizer.getDefaultContext(javaProject, contextObject);
								context.put(PARAM_FILES, HibernateMappingManager.getInstance(documents[0].getFile().getProject()).getFiles());
								context.put(PARAM_DOCUMENTS, HibernateMappingManager.getInstance(documents[0].getFile().getProject()).getDocuments());
							}
						}
					}
				}
			}
		}
	}

	/**
	 * The the contents for any special directives
	 * @param contents the contents
	 * @param syncFile the synchronization file (to add any markers)
	 * @param context the current context
	 * @return a static DIRECTIVE variable defined in this class
	 */
	public static int checkContents (String contents, IFile syncFile, Context context) {
		if (contents.startsWith(MESSAGE_WARNING)) {
			String message = contents.substring(MESSAGE_WARNING.length(), contents.length());
			String key = MESSAGE_WARNING + syncFile.getName() + message;
			if (null == context.get(key) && null != syncFile) {
				context.put(key, Boolean.TRUE);
				if (null != syncFile) EditorUtil.addWarningMarker(syncFile, message, 0);
			}
		}
		else if (contents.startsWith(MESSAGE_ERROR)) {
			String message = contents.substring(MESSAGE_ERROR.length(), contents.length());
			String key = MESSAGE_ERROR + syncFile.getName() + message;
			if (null == context.get(key) && null != syncFile) {
				context.put(key, Boolean.TRUE);
				if (null != syncFile) EditorUtil.addProblemMarker(syncFile, message, 0);
				return DIRECTIVE_STOP_PROCESSING_LOCAL;
			}
		}
		else if (contents.startsWith(MESSAGE_FATAL)) {
			String message = contents.substring(MESSAGE_FATAL.length(), contents.length());
			String key = MESSAGE_FATAL + syncFile.getName() + message;
			if (null == context.get(key) && null != syncFile) {
				context.put(key, Boolean.TRUE);
				if (null != syncFile) EditorUtil.addProblemMarker(syncFile, message, 0);
			}
			return DIRECTIVE_STOP_PROCESSING_GLOBAL;
		}
		return DIRECTIVE_KEEP_PROCESSING;
	}

	private void removeClassFiles (
			IJavaProject javaProject,
			HibernateClass hc,
			Context context,
			String currentFileName,
			IFile syncFile,
			IProgressMonitor monitor,
			Shell shell) throws CoreException {
		IProject project = javaProject.getProject();
		IPackageFragmentRoot root = HSUtil.getProjectRoot(javaProject, shell);
		if (null == root) {
		    throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, IStatus.OK, "The source root could not be determined for project: " + javaProject, null));
		}
		context.put(PARAM_CLASS, hc);
		context.put(PARAM_DOCUMENT, HibernateMappingManager.getInstance(javaProject.getProject()).getHibernateDocument(syncFile));
		context.put(PARAM_FILE, syncFile);
		context.put(PARAM_FILE_NAME, syncFile.getName());

		try {
			// process initial setup snippet if it exists
			Snippet snippet = ResourceManager.getInstance(project).getSnippet(SETUP_SNIPPET_CLASS);
			if (null != snippet) {
				snippet.merge(context).trim();
			}
		}
		catch (Exception e)
		{}

		if (null != monitor && monitor.isCanceled()) return;
		if (null != monitor) monitor.subTask(currentFileName + ": removing base value object");
		removeClassFile(context, root, hc.getBaseValueObjectPackage(), hc.getBaseValueObjectClassName());
		if (null != monitor) monitor.worked(1);
		if (null != monitor) monitor.subTask(currentFileName + ": removing extension business object");
		removeClassFile(context, root, hc.getValueObjectPackage(), hc.getValueObjectClassName());
		if (hc.hasProxy())
			removeClassFile(context, root, hc.getProxyPackage(), hc.getValueObjectProxyClassName());
		if (null != monitor) monitor.worked(1);

		if (null != monitor && monitor.isCanceled()) return;
		if (null != monitor) monitor.subTask(currentFileName + ": removing base value object");
		removeClassFile(context, root, hc.getBaseValueObjectPackage(), hc.getBaseValueObjectClassName());
		if (null != monitor) monitor.worked(1);
		if (null != monitor) monitor.subTask(currentFileName + ": removing extension business object");
		removeClassFile(context, root, hc.getValueObjectPackage(), hc.getValueObjectClassName());
		if (hc.hasProxy())
			removeClassFile(context, root, hc.getProxyPackage(), hc.getValueObjectProxyClassName());
		if (null != monitor) monitor.worked(1);

		if (null != monitor) {
			if (monitor.isCanceled()) return;
			monitor.subTask(currentFileName + ": removing components");
			
		}
		for (Iterator iter=hc.getComponentList().iterator(); iter.hasNext(); ) {
			HibernateComponentClass hcc = (HibernateComponentClass) iter.next();
			if (!hcc.isDynamic()) {
				context.put(PARAM_CLASS, hcc);
				removeClassFile(context, root, hcc.getBaseValueObjectPackage(), hcc.getBaseValueObjectClassName());
				removeClassFile(context, root, hcc.getValueObjectPackage(), hcc.getClassName());
			}
		}
		for (Iterator i=hc.getCollectionList().iterator(); i.hasNext(); ) {
			for (Iterator i2=((HibernateClassCollectionProperty) i.next()).getCompositeList().iterator(); i2.hasNext(); ) {
				HibernateComponentClass chc = (HibernateComponentClass) i2.next();
				context.put(PARAM_CLASS, chc);
				removeClassFile(context, root, chc.getBaseValueObjectPackage(), chc.getBaseValueObjectClassName());
				removeClassFile(context, root, chc.getValueObjectPackage(), chc.getClassName());
			}
		}
		if (null != monitor) monitor.worked(1);
		context.put(PARAM_CLASS, hc);

		if (null != monitor) {
			if (monitor.isCanceled()) return;
			monitor.subTask(currentFileName + ": removing id");
			
		}
		if (null != hc.getId() && hc.getId().isComposite() && hc.getId().hasExternalClass()) {
			removeClassFile(context, root, hc.getId().getProperty().getPackage(), hc.getId().getProperty().getClassName());
			removeClassFile(context, root, hc.getBaseValueObjectPackage(), "Base" + hc.getId().getProperty().getClassName());
		}
		if (null != monitor) monitor.worked(1);

		context.put(PARAM_CLASS, hc);
		if (null != monitor) {
			if (monitor.isCanceled()) return;
			monitor.worked(1);
			monitor.subTask(currentFileName + ": removing base DAO");
			monitor.worked(1);
		}
		removeClassFile(context, root, hc.getBaseDAOPackage(), hc.getBaseDAOClassName());
		if (null != monitor) {
			if (monitor.isCanceled()) return;
			monitor.worked(1);
			monitor.subTask(currentFileName + ": removing extension DAO");
		}
		removeClassFile(context, root, hc.getDAOPackage(), hc.getDAOClassName());
		if (null != monitor) monitor.worked(1);

		// custom templates
		if (customGenerationEnabled) {
			List templateLocations = ResourceManager.getInstance(javaProject.getProject()).getTemplateLocations();
			for (Iterator i=templateLocations.iterator(); i.hasNext(); ) {
				if (null != monitor && monitor.isCanceled()) return;
				TemplateLocation templateLocation = (TemplateLocation) i.next();
				removeCustomFile(templateLocation, hc, context, syncFile, force, shell);
			}
		}
	}

	/**
	 * Synchronize all files associated with the given HibernateClass
	 * @param hc the HibernateClass
	 * @param javaProject the Eclipse Java project
	 * @param valueObjectGenerationEnabled true if value object generation is enabled
	 * @param daoGenerationEnabled true if DAO generation is enabled
	 * @param customGenerationEnabled true if custom template generation is enabled
	 * @param currentFileName the name of the file that is currently being synchronized
	 * @param context the Velocity context
	 * @param force true to force all custom template overwrites even if the template is not to be overwritten
	 * @param syncFile the IFile being synchronized for marker support
	 * @param monitor the progress monitor
	 * @return
	 */
	private boolean synchronizeClass (
			IJavaProject javaProject,
			HibernateClass hc,
			boolean valueObjectGenerationEnabled,
			boolean daoGenerationEnabled,
			boolean customGenerationEnabled,
			String currentFileName,
			Context context,
			boolean force,
			IFile syncFile,
			IProgressMonitor monitor,
			Shell shell) throws CoreException {
		
		String doSync = hc.get(META_AUTO_SYNC);
		if (null != doSync && doSync.trim().toUpperCase().startsWith("F")) return true;
		
		IProject project = javaProject.getProject();
		IPackageFragmentRoot root = HSUtil.getProjectRoot(javaProject, shell);
		if (null == root) {
		    throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, IStatus.OK, "The source root could not be determined for project: " + javaProject, null));
		}
		context.put(PARAM_CLASS, hc);
		context.put(PARAM_DOCUMENT, HibernateMappingManager.getInstance(javaProject.getProject()).getHibernateDocument(syncFile));
		context.put(PARAM_FILE, syncFile);
		context.put(PARAM_FILE_NAME, syncFile.getName());
		try {
			// process initial setup snippet if it exists
			Snippet snippet = ResourceManager.getInstance(project).getSnippet(SETUP_SNIPPET_CLASS);
			if (null != snippet) {
				String result = snippet.merge(context).trim();
				int rtn = checkContents(result, syncFile, context);
				if (rtn == DIRECTIVE_STOP_PROCESSING_LOCAL) return true;
				else if (rtn == DIRECTIVE_STOP_PROCESSING_GLOBAL) return false;
			}
		}
		catch (Exception e)
		{}
		if (null != monitor && monitor.isCanceled()) return false;

		try {
			if (valueObjectGenerationEnabled && hc.canSyncValueObject()) {
				// create value object and proxy
				if (null != monitor) monitor.subTask(currentFileName + ": generating base value object");
				generateClassFile(TEMPLATE_BASE_VALUE_OBJECT, context, root, hc.getBaseValueObjectPackage(), hc.getBaseValueObjectClassName(), true);
				if (null != monitor) monitor.worked(1);
				if (null != monitor) monitor.subTask(currentFileName + ": generating extension business object");
				generateExtensionClassFile(TEMPLATE_VALUE_OBJECT, SNIPPET_VALUE_OBJECT_CONSTRUCTOR, context, root, hc.getValueObjectPackage(), hc.getValueObjectClassName());
				if (hc.hasProxy())
					generateProxyClassFile(TEMPLATE_VALUE_OBJECT_PROXY, TEMPLATE_VALUE_OBJECT_PROXY_CONTENTS, context, root, hc.getProxyPackage(), hc.getValueObjectProxyClassName());
				if (null != monitor) monitor.worked(1);

				// create subclasses
//				if (null != monitor) monitor.subTask(currentFileName + ": generating subclasses");
//				if (hc.getSubclassList().size() > 0) {
//					for (Iterator i2=hc.getSubclassList().iterator(); i2.hasNext(); ) {
//						HibernateClass subclass = (HibernateClass) i2.next();
//						if (synchronizeClass(javaProject, subclass, valueObjectGenerationEnabled, daoGenerationEnabled, customGenerationEnabled, currentFileName, context, force, syncFile, monitor, shell)) return true;
//					}
//					context.put(PARAM_CLASS, hc);
//				}
//				if (null != monitor) monitor.worked(1);

				// class components
				if (null != monitor) {
					if (monitor.isCanceled()) return false;
					monitor.subTask(currentFileName + ": generating components");
					
				}
				for (Iterator iter=hc.getComponentList().iterator(); iter.hasNext(); ) {
					HibernateComponentClass hcc = (HibernateComponentClass) iter.next();
					if (!hcc.isDynamic()) {
						context.put(PARAM_CLASS, hcc);
						generateClassFile(TEMPLATE_BASE_VALUE_OBJECT, context, root, hcc.getBaseValueObjectPackage(), hcc.getBaseValueObjectClassName(), true);
						generateExtensionClassFile(TEMPLATE_VALUE_OBJECT, SNIPPET_VALUE_OBJECT_CONSTRUCTOR, context, root, hcc.getValueObjectPackage(), hcc.getClassName());
					}
				}
				for (Iterator i=hc.getCollectionList().iterator(); i.hasNext(); ) {
					for (Iterator i2=((HibernateClassCollectionProperty) i.next()).getCompositeList().iterator(); i2.hasNext(); ) {
						HibernateComponentClass chc = (HibernateComponentClass) i2.next();
						context.put(PARAM_CLASS, chc);
						generateClassFile(TEMPLATE_BASE_VALUE_OBJECT, context, root, chc.getBaseValueObjectPackage(), chc.getBaseValueObjectClassName(), true);
						generateExtensionClassFile(TEMPLATE_VALUE_OBJECT, SNIPPET_VALUE_OBJECT_CONSTRUCTOR, context, root, chc.getValueObjectPackage(), chc.getClassName());
					}
				}
				if (null != monitor) monitor.worked(1);
				context.put(PARAM_CLASS, hc);

				// composite id
				if (null != monitor) {
					if (monitor.isCanceled()) return false;
					monitor.subTask(currentFileName + ": generating id");
					
				}
				if (null != hc.getId() && hc.getId().isComposite() && hc.getId().hasExternalClass()) {
					generateExtensionClassFile(TEMPLATE_VALUE_OBJECT_PK, SNIPPET_VALUE_OBJECT_PK_CONSTRUCTOR, context, root, hc.getId().getProperty().getPackage(), hc.getId().getProperty().getClassName());
					generateClassFile(TEMPLATE_BASE_VALUE_OBJECT_PK, context, root, hc.getBaseValueObjectPackage(), "Base" + hc.getId().getProperty().getClassName(), true);
				}
				if (null != monitor) monitor.worked(1);

				// TODO: implement user types
//				if (null != monitor) monitor.subTask(currentFileName + ": generating enumerations");
//				for (Iterator i2=hc.getProperties().iterator(); i2.hasNext(); ) {
//					HibernateClassProperty prop = (HibernateClassProperty) i2.next();
//					if (prop.isEnumeration()) {
//						context.put("field", prop);
//						writeClass("Enumeration.vm", context, root, prop.getPackageName(), prop.getClassName(), false);
//					}
//				}
//				if (null != monitor) monitor.worked(1);
			}
			else if (null != monitor) monitor.worked(6);

			// dao's
			if (daoGenerationEnabled && hc.canSyncDAO()) {
				context.put(PARAM_CLASS, hc);
				if (null != monitor) {
					if (monitor.isCanceled()) return false;
					monitor.subTask(currentFileName + ": generating root DAO");
					
				}
				if (!hc.useCustomDAO()) {
					generateClassFile(TEMPLATE_BASE_ROOT_DAO, context, root, hc.getBaseDAOPackage(), hc.getBaseRootDAOClassName(), true);
				}
				generateClassFile(TEMPLATE_ROOT_DAO, context, root, hc.getRootDAOPackage(), hc.getRootDAOClassName(), false);
				if (null != monitor) {
					if (monitor.isCanceled()) return false;
					monitor.worked(1);
					monitor.subTask(currentFileName + ": generating base DAO");
					monitor.worked(1);
				}
				generateClassFile(TEMPLATE_BASE_DAO, context, root, hc.getBaseDAOPackage(), hc.getBaseDAOClassName(), true);
				if (null != monitor) {
					if (monitor.isCanceled()) return false;
					monitor.worked(1);
					monitor.subTask(currentFileName + ": generating extension DAO");
				}
				generateClassFile(TEMPLATE_DAO, context, root, hc.getDAOPackage(), hc.getDAOClassName(), false);
				if (null != monitor) monitor.worked(1);
			}
			else if (null != monitor) monitor.worked(3);

			// custom templates
			if (customGenerationEnabled && hc.canSyncCustom()) {
				List templateLocations = ResourceManager.getInstance(javaProject.getProject()).getTemplateLocations();
				for (Iterator i=templateLocations.iterator(); i.hasNext(); ) {
					if (null != monitor && monitor.isCanceled()) return false;
					TemplateLocation templateLocation = (TemplateLocation) i.next();
					if (!generateCustomFile(templateLocation, hc, context, syncFile, force, shell)) return false;
				}
			}
		}
		catch( Exception e )
		{
			Plugin.log(e);
		}
		return true;
	}

	private void removeCustomFile (
			TemplateLocation templateLocation,
			HibernateClass hibernateClass,
			Context context,
			IFile syncFile,
			boolean force,
			Shell shell) {
		try {
			context.remove(PARAM_CUSTOM_PLACEHOLDER);
			String fileName = templateLocation.getName(context);
			String packageOrPath = templateLocation.getLocation(context);
			IProject project = templateLocation.getOutputProject();
			if (null != project) {
				if (templateLocation.getTemplate().isClassTemplate()) {
					IJavaProject javaProject = JavaCore.create(project);
					IPackageFragmentRoot root = HSUtil.getProjectRoot(javaProject, shell);
					String className = fileName;
					fileName += EXTENSION_JAVA;
					IPackageFragment fragment = root.getPackageFragment(packageOrPath);
					if (!fragment.exists()) return;
					else {
						ICompilationUnit unit = fragment.getCompilationUnit(fileName);
						if (unit.exists()) unit.delete(true, null);
					}
				}
				else {
					IFile file = project.getFile(packageOrPath + "/" + fileName);
					if (file.exists()) file.delete(true, true, null);
				}
			}
		}
		catch (Exception e) {}
	}

	/**
	 * Write the contents that relate to the given TemplateLocation Template
	 * @param templateLocation the template location
	 * @param hibernateClass the current HibernateClass that the generation relates to
	 * @param context the Velocity context
	 * @param syncFile the current IFile that being synchronized
	 * @param force true to overwrite even if the templateLocation is not set to overwrite
	 * @return true to keep processing and false to stop
	 */
	private boolean generateCustomFile (
			TemplateLocation templateLocation,
			HibernateClass hibernateClass,
			Context context,
			IFile syncFile,
			boolean force,
			Shell shell) {
		context.remove(PARAM_CUSTOM_PLACEHOLDER);
		try {
			String fileName = templateLocation.getName(context);
			String packageOrPath = templateLocation.getLocation(context);
			IProject project = templateLocation.getOutputProject();
			if (null != project) {
				if (templateLocation.getTemplate().isClassTemplate()) {
					// output is a java class
					IJavaProject javaProject = JavaCore.create(project);
					IPackageFragmentRoot root = HSUtil.getProjectRoot(javaProject, shell);
					String className = fileName;
					fileName += EXTENSION_JAVA;
					IPackageFragment fragment = root.getPackageFragment(packageOrPath);
					if (!fragment.exists()) fragment = root.createPackageFragment(packageOrPath, true, null);
					context.put(PARAM_PACKAGE, packageOrPath);
					context.put(PARAM_CLASS_NAME, className);
					String content = templateLocation.getTemplate().merge(context);
					context.remove(PARAM_PACKAGE);
					context.remove(PARAM_CLASS_NAME);
					int rtn = checkContents(content, syncFile, context);
					if (rtn == DIRECTIVE_KEEP_PROCESSING) {
						ICompilationUnit unit = fragment.getCompilationUnit(fileName);
						if (!unit.exists() || force || templateLocation.shouldOverride()) {
							writeCompilationUnit(fragment, unit, fileName, content, null);
						}
						writeCompilationUnit(fragment, unit, fileName, content, null);
					}
					else if (rtn == DIRECTIVE_STOP_PROCESSING_GLOBAL) {
						return false;
					}
				}
				else {
					// output is a resource file
					context.put(PARAM_PATH, packageOrPath);
					context.put(PARAM_FILE_NAME, fileName);
					String content = templateLocation.getTemplate().merge(context);
					context.remove(PARAM_PATH);
					context.remove(PARAM_FILE_NAME);
					int rtn = checkContents(content, syncFile, context);
					if (rtn == DIRECTIVE_KEEP_PROCESSING) {
						writeResourceFile(content, packageOrPath, fileName, templateLocation.getOutputProject());
					}
					else if (rtn == DIRECTIVE_STOP_PROCESSING_GLOBAL) {
						return false;
					}
				}
			}
		}
		catch (Exception e) {
			MessageDialog.openWarning(null, "An error has occured while creating custom template: " + templateLocation.getTemplate().getName(), e.getMessage());
		}
		return true;
	}

	/**
	 * Generate a class and it's constructor related to the templates given
	 * @param templateName the class template name
	 * @param constructorTemplateName the class constructor template name
	 * @param context the Velocity context
	 * @param fragmentRoot the compilation unit fragment root
	 * @param packageName the package name
	 * @param className the class name
	 * @throws JavaModelException
	 */
	private void generateExtensionClassFile (
			String templateName,
			String constructorSnippetName,
			Context context,
			IPackageFragmentRoot fragmentRoot,
			String packageName,
			String className) throws JavaModelException {
		IPackageFragment fragment = fragmentRoot.getPackageFragment(packageName);
		className = className + EXTENSION_JAVA;
		context.put(PARAM_PACKAGE, packageName);
		try {
			if (null != fragment) {
				if (!fragment.exists()) {
					fragment = fragmentRoot.createPackageFragment(packageName, false, null);
				}
				ICompilationUnit unit = fragment.getCompilationUnit(className);
				if (unit.exists()) {
					// just try to rewrite the constructors
					String existingContent = unit.getSource();
					MarkerContents mc = HSUtil.getMarkerContents(existingContent, MARKER_CONSTRUCTOR);
					if (null != mc) {
						try {
							Snippet snippet = ResourceManager.getInstance(fragmentRoot.getJavaProject().getProject()).getSnippet(constructorSnippetName);
							String content = snippet.merge(context);
							if (null != mc.getContents() && mc.getContents().trim().equals(content.toString().trim())) return;
							else {
								String newContent = mc.getPreviousContents() + content + "\n" + mc.getPostContents();
								unit = writeCompilationUnit(fragment, unit, className, newContent, null);
							}
						}
						catch (JavaModelException e) {}
					}
				}
				else {
					// new file... write the entire class
					try {
						Snippet snippet = ResourceManager.getInstance(fragmentRoot.getJavaProject().getProject()).getSnippet(constructorSnippetName);
						String constructorContents = snippet.merge(context);
						VelocityContext subContext = new VelocityContext(context);
						subContext.put(PARAM_CONSTRUCTOR, constructorContents);
						Template template = ResourceManager.getInstance(fragmentRoot.getJavaProject().getProject()).getTemplate(templateName);
						String content = template.merge(subContext);
						unit = writeCompilationUnit(fragment, unit, className, content, null);
					}
					catch (JavaModelException e) {}
				}
			}
		}
		catch (Exception e) {
			if (e instanceof JavaModelException) throw (JavaModelException) e;
			else MessageDialog.openWarning(null, "An error has occured: " + e.getClass(), e.getMessage());
		}
		context.remove(PARAM_PACKAGE);
	}

	/**
	 * Create the given proxy class
	 * @param templateName the proxy template name
	 * @param contentTemplateName the template name referencing the generated proxy methods
	 * @param context the Velocity context
	 * @param fragmentRoot the package fragment root
	 * @param packageName the package name
	 * @param className the name of the proxy interface to be created
	 * @throws JavaModelException
	 */
	private void generateProxyClassFile (
			String templateName,
			String contentTemplateName,
			Context context,
			IPackageFragmentRoot fragmentRoot,
			String packageName,
			String className) throws JavaModelException {
		IPackageFragment fragment = fragmentRoot.getPackageFragment(packageName);
		String fileName = className + EXTENSION_JAVA;
		context.put(PARAM_PACKAGE, packageName);
		try {
			if (null != fragment) {
				if (!fragment.exists()) {
					fragment = fragmentRoot.createPackageFragment(packageName, false, null);
				}
				ICompilationUnit unit = fragment.getCompilationUnit(fileName);
				if (unit.exists()) {
					String content = unit.getSource();
					MarkerContents mc = HSUtil.getMarkerContents(content, MARKER_GENERATED_CONTENT);
					if (null != mc) {
						try {
							// rewrite all but generated content
							Template template = ResourceManager.getInstance(fragmentRoot.getJavaProject().getProject()).getTemplate(contentTemplateName);
							String customContent = template.merge(context);
							if (null != mc.getContents() && mc.getContents().trim().equals(customContent.trim())) return;
							else {
								content = mc.getPreviousContents() + customContent + mc.getPostContents();
								writeCompilationUnit(fragment, unit, fileName, content, null);
							}
						}
						catch (JavaModelException e) {}
					}
				}
				else {
					try {
						// create a new proxy class
						Template template = ResourceManager.getInstance(fragmentRoot.getJavaProject().getProject()).getTemplate(contentTemplateName);
						String content = template.merge(context);
						VelocityContext subContext = new VelocityContext(context);
						subContext.put(PARAM_CONTENT, content);
						template = ResourceManager.getInstance(fragmentRoot.getJavaProject().getProject()).getTemplate(templateName);
						content = template.merge(subContext);
						writeCompilationUnit(fragment, unit, fileName, content, null);
					}
					catch (JavaModelException e) {}
				}
			}
		}
		catch (Exception e) {
			if (e instanceof JavaModelException) throw (JavaModelException) e;
			else MessageDialog.openWarning(null, "An error has occured: " + e.getClass(), e.getMessage());
		}
		context.remove(PARAM_PACKAGE);
	}

	/**
	 * Perform the generation with the specified template and save the results 
	 * @param velocityTemplate the Velocity template name
	 * @param context the Velocity context
	 * @param fragmentRoot the compilation fragment root
	 * @param packageName the name of the package to create the CompilationUnit in
	 * @param className the name of the class to generate
	 * @param force true to overwrite existing content and false to only create new content
	 * @throws JavaModelException
	 */
	private void generateClassFile (
			String velocityTemplate,
			Context context,
			IPackageFragmentRoot fragmentRoot,
			String packageName,
			String className,
			boolean force) throws JavaModelException {
		IPackageFragment fragment = fragmentRoot.getPackageFragment(packageName);
		String fileName = className + EXTENSION_JAVA;
		context.put(PARAM_PACKAGE, packageName);
		try {
			if (!fragment.exists()) {
				fragment = fragmentRoot.createPackageFragment(packageName, false, null);
			}
			ICompilationUnit unit = fragment.getCompilationUnit(fileName);
			Template template = ResourceManager.getInstance(fragmentRoot.getJavaProject().getProject()).getTemplate(velocityTemplate);
			if (!unit.exists() || force) {
				String content = template.merge(context);
				unit = writeCompilationUnit(fragment, unit, fileName, content, progressMonitor);
			}
		}
		catch (Exception e) {
		    Plugin.log(e);
			if (e instanceof JavaModelException) throw (JavaModelException) e;
			else MessageDialog.openWarning(null, "An error has occured: " + e.getClass(), e.getMessage());
		}
		context.remove(PARAM_PACKAGE);
	}

	private void removeClassFile(
			Context context,
			IPackageFragmentRoot fragmentRoot,
			String packageName,
			String className) throws JavaModelException {
		IPackageFragment fragment = fragmentRoot.getPackageFragment(packageName);
		String fileName = className + EXTENSION_JAVA;
		context.put(PARAM_PACKAGE, packageName);

		if (!fragment.exists()) {
			return;
		}
		else {
			ICompilationUnit unit = fragment.getCompilationUnit(fileName);
			if (unit.exists()) {
				unit.delete(true, null);
			}
		}
	}

	/**
	 * Overwrite the given contents into the given ICompilationUnit
	 * @param fragment the package fragment
	 * @param currentUnit the unit to replace the contents in (or null if new)
	 * @param fileName the file name
	 * @param content the new contents
	 * @param monitor an progress monitor (or null)
	 * @return the newly create compilation unit
	 * @throws JavaModelException
	 */
	private ICompilationUnit writeCompilationUnit (
			IPackageFragment fragment,
			ICompilationUnit currentUnit,
			String fileName,
			String content,
			IProgressMonitor monitor) throws JavaModelException {
		if (null != currentUnit) {
			if (currentUnit.exists()) {
				String existingContent = currentUnit.getBuffer().getContents();
				if (null != existingContent && existingContent.equals(content)) return currentUnit;
				else return fragment.createCompilationUnit(fileName, content, true, monitor);
			}
			else {
				return fragment.createCompilationUnit(fileName, content, true, monitor);
			}
		}
		else return null;
	}

	/**
	 * Write the given contents to the file described by the path and file name for the given project.
	 * If the file already exists and the contents are the same as the ones given, do nothing and return false.
	 * @param content the new file contents
	 * @param path the file path without the file name
	 * @param fileName the file name
	 * @param project
	 * @return true if the file was written and false if not
	 * @throws CoreException
	 */
	public boolean writeResourceFile (
			String content,
			String path,
			String fileName,
			IProject project) throws CoreException, IOException {
		IFile file = project.getFile(path + "/" + fileName);
		if (!file.exists()) {
			StringTokenizer st = new StringTokenizer(path, "/");
			// make sure the directory exists
			StringBuffer sb = new StringBuffer();
			while (st.hasMoreTokens()) {
				if (sb.length() > 0) sb.append('/');
				sb.append(st.nextToken());
				IFolder folder = project.getFolder(sb.toString());
				if (!folder.exists()) folder.create(false, true, null);
			}
			file.create(new ByteArrayInputStream(content.getBytes()), true, null);
			return true;
		}
		else {
			String existingContent = null;
			try {
				existingContent = HSUtil.getStringFromStream(file.getContents());
			}
			catch (ResourceException re) {
				// possible not refreshed
				file.refreshLocal(IResource.DEPTH_ONE, null);
				existingContent = HSUtil.getStringFromStream(file.getContents());
			}
			if (null != existingContent && existingContent.equals(content)) return false;
			else {
				file.setContents(new ByteArrayInputStream(content.getBytes()), true, true, null);
				return true;
			}
		}
	}

	private String ct =
		" * This class has been automatically generated by Hibernate Synchronizer.\n" +
		" * For more information or documentation, visit The Hibernate Synchronizer page\n" +
		" * at http://www.binamics.com/hibernatesync or contact Joe Hudson at joe@binamics.com.\n";
	private String addContent (String contents, String className) {
		int commentIndex = contents.indexOf("/*");
		commentIndex = commentIndex - 1;
		int classIndex = contents.indexOf("class " + className);
		if (classIndex < 0) {
			classIndex = contents.indexOf("interface " + className);
		}
		if (classIndex < 0) {
			return addContent(contents, 0, false);
		}
		else {
			if (commentIndex < classIndex && commentIndex >= 0) {
				int nl = commentIndex;
				while (true) {
					nl ++;
					if (contents.getBytes()[nl] == '\n') break;
				}
				nl ++;
				return addContent(contents, nl, true);
			}
			else {
				int nl = classIndex;
				while (true) {
					nl --;
					if (contents.getBytes()[nl] == '\n') break;
				}
				nl ++;
				return addContent(contents, nl, false);
			}
		}
	}

	private String addContent(String contents, int pos,
			boolean isCommentPart) {
		String p1 = null;
		if (pos > 0)
			p1 = contents.substring(0, pos);
		else
			p1 = "";
		String p2 = contents.substring(pos, contents.length());
		if (isCommentPart) {
			return p1 + ct + " *\n" + p2;
		} else {
			return p1 + "/**\n" + ct + " */\n" + p2;
		}
	}

	/**
	 * Create a context to be used for synchronization
	 * @param hc
	 * @param project
	 * @param contextObject
	 * @return
	 */
	public static Context getDefaultContext (
			IJavaProject project,
			Object contextObject) {

		Context context = new VelocityContext();
		context.put(PARAM_NOW, new Date());
		context.put(PARAM_PROJECT, project);
		context.put("dollar", "$");
		context.put("notDollar", "$!");
		
		context.put(PARAM_SNIPPET, new SnippetContext(context, project.getProject()));
		boolean useCustomDAO = Plugin.getBooleanProperty(project.getProject(), Constants.PROP_USE_CUSTOM_ROOT_DAO, false);
		if (useCustomDAO) {
			String daoException = Plugin.getProperty(project.getProject(), Constants.PROP_BASE_DAO_EXCEPTION);
			if (null != daoException && daoException.trim().length() > 0) {
				context.put(PARAM_EXCEPTION_CLASS, daoException.trim());
			}
			else context.put(PARAM_EXCEPTION_CLASS, "net.sf.hibernate.HibernateException");
		}
		else {
			context.put(PARAM_EXCEPTION_CLASS, "net.sf.hibernate.HibernateException");
		}
		context.put(PARAM_UTIL, new HSUtil());
		// TODO add template string parameters
//		context.put("stringUtil", new StringUtils());
//		try {
//			Map properties = TemplateManager.getInstance().getTemplateParameters(project.getProject());
//			for (Iterator i=properties.entrySet().iterator(); i.hasNext(); ) {
//				Map.Entry entry = (Map.Entry) i.next();
//				context.put((String) entry.getKey(), entry.getValue());
//			}
//		}
//		catch (CoreException ce) {}
		if (null != contextObject) {
			if (contextObject instanceof Map) {
				for (Iterator i=((Map) contextObject).entrySet().iterator(); i.hasNext(); ) {
					Map.Entry entry = (Map.Entry) i.next();
					if (null != entry.getKey()) {
						context.put(entry.getKey().toString(), entry.getValue());
					}
				}
			}
			context.put(PARAM_CONTEXT_OBJECT, contextObject);
		}
		return context;
	}

	/**
	 * Return the user defined context object or null if N/A
	 * @param project the current project
	 * @return the user defined object
	 */
	public static Object getContextObject (IProject project) {
		try {
			String coName = Plugin.getProperty(project, Constants.PROP_CONTEXT_OBJECT);
			if (null != coName && coName.length() > 0) {
				coName = coName.trim();
				ProjectClassLoader cl = new ProjectClassLoader(JavaCore.create(project));
				return cl.loadClass(coName).newInstance();
			}
		}
		catch (Exception e) {
			Plugin.trace(e);
		}
		return null;
	}

	/**
	 * Used for asynchronous processing
	 */
	public class Runner extends Thread {
		private Synchronizer synchronizer;

		public Runner (Synchronizer synchronizer) {
			this.synchronizer = synchronizer;
		}
		
		public void run () {
			try {
				ResourcesPlugin.getWorkspace().run(synchronizer, null);
			}
			catch (Throwable t) {
				Plugin.log(t);
			}
		}
	}
}