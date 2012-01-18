package com.hudson.hibernatesynchronizer.mapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.xml.sax.SAXParseException;

import com.hudson.hibernatesynchronizer.Plugin;
import com.hudson.hibernatesynchronizer.exceptions.HibernateSynchronizerException;
import com.hudson.hibernatesynchronizer.util.Synchronizer;


public class HibernateMappingManager {

	private static String currentProjectName;

	private static HibernateMappingManager instance;
	private boolean loading;
	private boolean loaded;

	private IProject project;
	private IResource mappingsFolder;
	private Map projectMappings = new HashMap();
	private Map projectDocuments = new HashMap();
	private Map classDocuments = new HashMap();
	private Map tableClasses = new HashMap();

	/**
	 * Private constructor for singleton functionality
	 */
	private HibernateMappingManager () {}

	/**
	 * Return the project-level singleton ResourceManager
	 */
	public static HibernateMappingManager getInstance (IProject project) {
		if (null == currentProjectName || !currentProjectName.equals(project.getName()) || null == instance) {
			instance = new HibernateMappingManager();
			instance.project = project;
			currentProjectName = project.getName();
		}
		return instance;
	}

	/**
	 * Notify the manager that this file is currently being viewed or edited
	 * @param file
	 */
	public void notifyMappingEdit (IFile file) {
		notifyMappingFile(file);
		if (!(loading || loaded)) loadAsync(file);
	}

	/**
	 * Notify the manager that this file is currently being saved
	 * @param file
	 */
	public void notifyMappingSave (IFile file) throws SAXParseException, HibernateSynchronizerException {
		try {
			notifyMappingFile(file);
			waitForLoad();
		}
		catch (Exception e) {
			// bad file content
		}
		try {
			clearDocumentCache (file);
			final HibernateDocument document = new HibernateDocument(file);
			if (document.hasTopLevelSubclassNodes()) document.loadTopLevelSubclasses(projectMappings);
			projectDocuments.remove(file.getName());
			cacheDocument(document, file);

			IJavaProject javaProject = JavaCore.create(file.getProject());
            try {
                ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
                    public void run(IProgressMonitor monitor)
                            throws CoreException {
                        try {
                        	Synchronizer synchronizer = new Synchronizer(document, false, null, null);
                            synchronizer.synchronize();
                        } catch (Exception e) {
                        	throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, IStatus.OK, e.getMessage(), e));
                        } finally {
                            monitor.done();
                        }
                    }
                },
                new NullProgressMonitor());
            } catch (Exception e) {
                Plugin.log(e);
            }
		}
		catch (Exception e) {
			if (e instanceof SAXParseException) throw (SAXParseException) e;
			else if (e instanceof HibernateSynchronizerException) throw (HibernateSynchronizerException) e;
			Plugin.trace(e);
		}
	}

	/**
	 * Notify the manager the given file is a Hibernate mapping file
	 * @param file
	 */
	public void notifyMappingFile (IFile file) {
		if (!(loading || loaded)) loadAsync(file);
		else {
			if (null == instance.projectDocuments.get(file.getName())) {
				loadAsync(file);
				instance = new HibernateMappingManager();
				instance.project = project;
				currentProjectName = project.getName();
			}
		}
	}

	/**
	 * Return the HibernateClass associated with the given classname
	 * @param className
	 * @return
	 */
	public HibernateClass getHibernateClass (String className) {
		waitForLoad();
		return (HibernateClass) projectMappings.get(className);
	}

	/**
	 * Return the loaded document associated with the file given
	 * @param file
	 * @return
	 */
	public HibernateDocument getHibernateDocument (IFile file) {
		notifyMappingFile(file);
		waitForLoad();
		return modCheck((HibernateDocument) projectDocuments.get(file.getName()));
	}

	/**
	 * Return the loaded document associated with the fully qualified class name given
	 * @param file
	 * @return
	 */
	public HibernateDocument getHibernateDocument (String className) {
		waitForLoad();
		return modCheck((HibernateDocument) classDocuments.get(className));
	}

	/**
	 * Return the HibernateClass associated with the given table name
	 */
	public HibernateClass getHibernateClassByTableName (String table) {
		waitForLoad();
		return (HibernateClass) tableClasses.get(table);
	}

	/**
	 * Return all knows mapping files 
	 * @return a list of IFile objects
	 */
	public List getFiles () {
		List files = new ArrayList();
		for (Iterator i=projectDocuments.values().iterator(); i.hasNext(); ) {
			HibernateDocument doc = modCheck((HibernateDocument) i.next());
			files.add(doc.getFile());
		}
		return files;
	}

	private HibernateDocument modCheck (HibernateDocument doc) {
		if (null == doc) return null;
		if (null != doc.getFile() && doc.getLastModTime() < doc.getFile().getLocalTimeStamp()) {
			try {
				final HibernateDocument document = new HibernateDocument(doc.getFile());
				try {
					for (Iterator i=doc.getClasses().iterator(); i.hasNext(); ) {
						HibernateClass hc = (HibernateClass) i.next();
						projectMappings.remove(hc.getAbsoluteValueObjectClassName());
						classDocuments.remove(hc.getAbsoluteValueObjectClassName());
						tableClasses.remove(hc.getTableName());
					}
					projectDocuments.remove(doc.getFile().getName());

					if (document.hasTopLevelSubclassNodes()) document.loadTopLevelSubclasses(projectMappings);
					projectDocuments.remove(doc.getFile().getName());
					cacheDocument(document, doc.getFile());
				}
				catch (Exception e) {
					if (e instanceof SAXParseException) throw (SAXParseException) e;
					else if (e instanceof HibernateSynchronizerException) throw (HibernateSynchronizerException) e;
					Plugin.trace(e);
				}
				return document;
			}
			catch (Exception e) {
				return null;
			}
		}
		else return doc;
	}

	/**
	 * Return all knows mapping files as documents
	 * @return a list of HibernateDocument objects
	 */
	public List getDocuments () {
		List documents = new ArrayList();
		for (Iterator i=projectDocuments.values().iterator(); i.hasNext(); ) {
			documents.add(i.next());
		}
		return documents;
	}

	/**
	 * Return all knows hibernate classes
	 * @return a list of HibernateClasses objects
	 */
	public List getClasses () {
		List classes = new ArrayList();
		for (Iterator i=tableClasses.values().iterator(); i.hasNext(); ) {
			classes.add(i.next());
		}
		return classes;
	}

	/**
	 * Halt processing if a request for data is entered before the asynchronous processing is complete
	 */
	private void waitForLoad () {
		if (loaded) return;
		long now = System.currentTimeMillis();
		long killTime = now + MAX_WAIT_TIME;
		while (!loaded && System.currentTimeMillis() < killTime) {
			try {
				Thread.sleep(SLEEP_TIME);
				if (loaded) return;
			}
			catch (Exception e) {}
		}
		// we still haven't loaded after 2 minutes... something is wrong
		throw new RuntimeException("The mapping files could not be fully loaded after 30 seconds... timed out");
	}
	private static final long MAX_WAIT_TIME = 30 * 1000;
	private static final long SLEEP_TIME = 1000;

	/**
	 * Load all files in the given file directory asynchronously
	 * @param file
	 */
	private void loadAsync (IFile file) {
		if (null == mappingsFolder) {
			setMappingsFolder(file);
		}
		Runner runner = new Runner(project);
		runner.start();
	}

	/**
	 * Load all mappping files in the same directory as the one given
	 * @param file
	 */
	private synchronized void load () {
		if (!loading && !loaded) {
			loading = true;
			try {
			    Plugin.trace("Loading mapping resources");
				IResource[] resources = getMappingMembers();
				List subclassDocs = new ArrayList();
				if (null != resources) {
					for (int i=0; i<resources.length; i++) {
						try {
							if (resources[i] instanceof IFile) {
								IFile file = (IFile) resources[i];
								Plugin.trace("Loading file: " + file);
								HibernateDocument document = new HibernateDocument(file);
								if (document.hasTopLevelSubclassNodes()) subclassDocs.add(document);
								else cacheDocument(document, file);
							}
						}
						catch (Throwable e) {
							Plugin.trace(e);
						}
					}
					while (subclassDocs.size() > 0) {
						List unloadedSubclasses = new ArrayList();
						boolean subclassAdded = false;
						for (Iterator i=subclassDocs.iterator(); i.hasNext(); ) {
							HibernateDocument doc = (HibernateDocument) i.next();
							if (!doc.loadTopLevelSubclasses(projectMappings)) {
								unloadedSubclasses.add(doc);
							}
							else {
								subclassAdded = true;
								cacheDocument(doc, doc.getFile());
							}
						}
						if (!subclassAdded) break;
						else {
							subclassDocs = unloadedSubclasses;
						}
					}
				}
			}
			catch (Exception e) {
				Plugin.log(e);
			}
			loaded = true;
		}
	}

	/**
	 * Return all mapping resources within the mappings folder
	 * @throws CoreException
	 */
	private IResource[] getMappingMembers () throws CoreException {
		if (null == mappingsFolder) return null;
		else {
			if (mappingsFolder instanceof IProject) {
				return ((IProject) mappingsFolder).members();
			}
			else if (mappingsFolder instanceof IFolder) {
				return ((IFolder) mappingsFolder).members();
			}
			else return null;
		}
	}

	/**
	 * Clear the cache associated with the file
	 * @param file
	 */
	private void clearDocumentCache (IFile file) {
		HibernateDocument document = getHibernateDocument(file);
		if (null != document) {
			for (Iterator i=document.getClasses().iterator(); i.hasNext(); ) {
				HibernateClass hc = (HibernateClass) i.next();
				projectMappings.remove(hc.getAbsoluteValueObjectClassName());
				classDocuments.remove(hc.getAbsoluteValueObjectClassName());
				tableClasses.remove(hc.getTableName());
			}
		}
		projectDocuments.remove(file.getName());
	}

	/**
	 * Cache the document as associated HibernateClass objects for re-use
	 * @param document the document
	 * @param file the file
	 */
	private void cacheDocument(HibernateDocument document, IFile file) {
		if (document.getClasses().size() > 0)
		projectDocuments.put(file.getName(), document);
		for (Iterator iter=document.getClasses().iterator(); iter.hasNext(); ) {
			HibernateClass hc = (HibernateClass) iter.next();
			projectMappings.put(hc.getAbsoluteValueObjectClassName(), hc);
			classDocuments.put(hc.getAbsoluteValueObjectClassName(), document);
			tableClasses.put(hc.getTableName(), hc);
		}
	}

	/**
	 * Set the mappings folder so we can scan it
	 * @param file a single mapping file to retrieve the folder from
	 */
	private void setMappingsFolder (IFile file) {
		try {
			IResource resource = file.getParent();
			if (resource instanceof IProject) {
				mappingsFolder = (IProject) resource;
			}
//			else if (resource instanceof IJavaProject) {
//				mappingsFolder = ((IJavaProject) resource).getProject();
//			}
//			else if (resource instanceof IPackageFragment) {
//				mappingsFolder = (IFolder) ((IPackageFragmentRoot) resource).getCorrespondingResource();
//			}
			else mappingsFolder = (IFolder) resource;
		}
		catch (Exception e) {
			Plugin.log(e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Used for asynchronous processing
	 */
	public class Runner extends Thread {
		private IProject project;

		public Runner (IProject project) {
			this.project = project;
		}
		
		public void run () {
			HibernateMappingManager.getInstance(project).load();
		}
	}
}