package com.hudson.hibernatesynchronizer.wizard;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.Context;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ide.IDE;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.hudson.hibernatesynchronizer.Constants;
import com.hudson.hibernatesynchronizer.Plugin;
import com.hudson.hibernatesynchronizer.db.Container;
import com.hudson.hibernatesynchronizer.db.DBTable;
import com.hudson.hibernatesynchronizer.mapping.HibernateClass;
import com.hudson.hibernatesynchronizer.mapping.HibernateClassProperty;
import com.hudson.hibernatesynchronizer.mapping.HibernateComponentClass;
import com.hudson.hibernatesynchronizer.mapping.HibernateDocument;
import com.hudson.hibernatesynchronizer.mapping.HibernateMappingManager;
import com.hudson.hibernatesynchronizer.resource.ResourceManager;
import com.hudson.hibernatesynchronizer.util.EditorUtil;
import com.hudson.hibernatesynchronizer.util.HSUtil;
import com.hudson.hibernatesynchronizer.util.XMLFormatter;

public class MappingWizardRunnable implements IWorkspaceRunnable {
	private String[] selectedTableNames;
	private String containerName;
	private Container tableContainer;
	private Connection connection;
	private IJavaProject javaProject;
	private IPackageFragmentRoot root;
	private Shell shell;
	public MappingWizardRunnable(IJavaProject javaProject, String containerName, Container tableContainer,
			String packageStr, String[] selectedTableNames, Properties props, Connection connection, Shell shell) throws CoreException {
		this.javaProject = javaProject;
		this.containerName = containerName;
		this.tableContainer = tableContainer;
		this.selectedTableNames = selectedTableNames;
		tableContainer.setProperties(props);
		tableContainer.setPackageName(packageStr);
		this.connection = connection;
		this.shell = shell;
		root = HSUtil.getProjectRoot(javaProject, shell);
	}
	
	private static final String INTEGER = "integer";
	private static final String STRING = "string";
	public void run(IProgressMonitor monitor) throws CoreException {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IResource resource = root.findMember(new Path(containerName));
		if (!resource.exists() || !(resource instanceof IContainer)) {
			return;
		}
		IContainer container = (IContainer) resource;
		
		monitor.beginTask("Creating files...", tableContainer.getTables().size());

		IFile file = null;
		Throwable lastThrowable = null;
		int index = 0;
		
		Context context;
		int count = 0;
		String extension = tableContainer.getProperty("Extension");
		if (null == extension || extension.trim().length() == 0) extension = Constants.EXTENSION_HBM;
		extension = extension.trim();
		if (!extension.startsWith(".")) extension = "." + extension;
		try {
			String mappingTemplate = ResourceManager.getTemplateContents("templates/MappingWizard.vm");
			for (Iterator i = tableContainer.getTables().values().iterator(); i.hasNext(); index++) {
				DBTable table = (DBTable) i.next();
				boolean doContinue = false;
				for (int j=0; j<selectedTableNames.length; j++) {
					if (selectedTableNames[j].equals(table.getName())) {
						doContinue = true;
						break;
					}
				}
				if (doContinue) {
					try {
						String fileName = table.getHibernateClassName() + extension;
						monitor.subTask("Creating " + fileName);
						file = container.getFile(new Path(fileName));
						
						context = new VelocityContext();
						for (Iterator i2=tableContainer.getProperties().entrySet().iterator(); i2.hasNext(); ) {
							Map.Entry entry = (Map.Entry) i2.next();
							context.put((String) entry.getKey(), entry.getValue());
						}
						context.put("packageName", tableContainer.getPackageName());
						context.put("table", table);
						StringWriter sw = new StringWriter();
						if (file.exists()) {
							context.put("fileExists", Boolean.TRUE);
							String queryNodes = null;
							String mapNodes = null;
							String listNodes = null;
							String bagNodes = null;
							String subclassNodes = null;
							String arrayNodes = null;
							try {
								HibernateDocument doc = HibernateMappingManager.getInstance(javaProject.getProject()).getHibernateDocument(file);
								if (doc.getClasses().size() == 1) {
									table.setHibernateClassName(((HibernateClass) doc.getClasses().get(0)).getValueObjectClassName());
								}
								addContextReference (doc.getDocument(), "query", context, "queries", "\t");
								addContextReference (doc.getDocument(), "map", context, "maps", "\t\t");
								addContextReference (doc.getDocument(), "list", context, "lists", "\t\t");
								addContextReference (doc.getDocument(), "bag", context, "bags", "\t\t");
								addContextReference (doc.getDocument(), "array", context, "arrays", "\t\t");
								addContextReference (doc.getDocument(), "set", context, "sets", "\t\t");
								addContextReference (doc.getDocument(), "primitive-array", context, "primitiveArrays", "\t\t");
								addContextReference (doc.getDocument(), "subclass", context, "subclasses", "\t\t");
								addContextReference (doc.getDocument(), "one-to-one", context, "oneToOnes", "\t\t");
								addContextReference (doc.getDocument(), "many-to-many", context, "manyToManies", "\t\t");
								addContextReference (doc.getDocument(), "dynamicComponents", context, "dynamic-component", "\t\t");
								HibernateClass hc = (HibernateClass) doc.getClasses().get(0);
								if (null != hc.getMetaData() && hc.getMetaData().size() > 0) {
									StringWriter sw1 = new StringWriter();
									PrintWriter pw = new PrintWriter(sw1);
									XMLFormatter formatter = new XMLFormatter(pw);
									formatter.printNodes(hc.getMetaData(), "\t\t");
									table.setMetaData(sw1.toString());
								}
								List components = hc.getComponentList();
								if (components.size() > 0) {
									List componentNodes = new ArrayList();
									for (Iterator iter=components.iterator(); iter.hasNext(); ) {
										HibernateComponentClass hcc = (HibernateComponentClass) iter.next();
										boolean fullMatch = true;
										for (Iterator i2=hcc.getProperties().iterator(); i2.hasNext(); ) {
											HibernateClassProperty prop = (HibernateClassProperty) i2.next();
											if (null == table.getColumn(prop.getColumn())) fullMatch = false;
										}
										for (Iterator i2=hcc.getManyToOneList().iterator(); i2.hasNext(); ) {
											HibernateClassProperty prop = (HibernateClassProperty) i2.next();
											if (null == table.getColumn(prop.getColumn())) fullMatch = false;
										}
										if (fullMatch) {
											for (Iterator i2=hcc.getProperties().iterator(); i2.hasNext(); ) {
												HibernateClassProperty prop = (HibernateClassProperty) i2.next();
												table.removeColumn(prop.getColumn());
											}
											for (Iterator i2=hcc.getManyToOneList().iterator(); i2.hasNext(); ) {
												HibernateClassProperty prop = (HibernateClassProperty) i2.next();
												table.removeColumn(prop.getColumn());
											}
											componentNodes.add(hcc.getNode());
										}
									}
									if (componentNodes.size() > 0) {
										StringWriter sw1 = new StringWriter();
										PrintWriter pw = new PrintWriter(sw1);
										XMLFormatter formatter = new XMLFormatter(pw);
										formatter.printNodes(componentNodes, "\t\t");
										context.put("components", sw1.toString());
									}
								}
							}
							catch (Exception e) {
								Plugin.log(e);
							}
							if (null != tableContainer.getProperties().get("UseProxies")) {
								StringWriter proxyClassName = new StringWriter();
								VelocityContext subCntext = new VelocityContext();
								subCntext.put("className", table.getHibernateClassName());
								Constants.templateGenerator.evaluate(subCntext, proxyClassName, Velocity.class.getName(), tableContainer.getProperties().getProperty("ProxyClassName"));
								context.put("proxyClass", proxyClassName.toString());
							}
							sw = new StringWriter();
							Constants.templateGenerator.evaluate(context, sw, Velocity.class.getName(), mappingTemplate);
							context.remove("proxyClass");
							InputStream stream = new ByteArrayInputStream(sw.toString().getBytes());
							file.setContents(stream, true, true, monitor);
						} else {
							if (null != tableContainer.getProperties().get("UseProxies")) {
								StringWriter proxyClassName = new StringWriter();
								VelocityContext subCntext = new VelocityContext();
								subCntext.put("className", table.getHibernateClassName());
								Constants.templateGenerator.evaluate(subCntext, sw, Velocity.class.getName(), tableContainer.getProperties().getProperty("ProxyClassName"));
								context.put("proxyClass", sw.toString());
							}
							sw = new StringWriter();
							Constants.templateGenerator.evaluate(context, sw, Velocity.class.getName(), mappingTemplate);
							context.remove("proxyClass");
							String contents = sw.toString();
							// make it look nice
							/*
							try {
								HibernateDOMParser parser = new HibernateDOMParser(new ByteArrayInputStream(contents.getBytes()));
								Document doc = parser.getDocument();
								StringWriter swT = new StringWriter();
								PrintWriter pw = new PrintWriter(swT);
								new XMLHelper(pw).printTree(doc.getDocumentElement(), parser, new ByteArrayInputStream(contents.getBytes()));
								contents = swT.toString();
							}
							catch (Exception e) {
								Plugin.logError(e);
							}
							*/
							
							InputStream stream = new ByteArrayInputStream(contents.getBytes());
							file.create(stream, true, monitor);
						}
						EditorUtil
								.addWarningMarker(
										file,
										"You should synchronize this file after viewing the contents",
										1);
						IDE.setDefaultEditor(file, "com.hudson.hibernatesynchronizer.editors.synchronizer.Editor");
						monitor.worked(1);
						count++;
					} catch (Throwable t) {
						Plugin.log(t);
						lastThrowable = t;
					}
				}
			}
		}
		catch (Exception e) {
			Plugin.log(e);
			
		}
	}
	
	private void loadEnumerations(String s, Map enumerations) {
		if (null != s) {
			StringTokenizer st = new StringTokenizer(s, ",");
			while (st.hasMoreTokens()) {
				String enum = st.nextToken();
				enumerations.put(enum, enum);
			}
		}
	}
	
	private String writeEnumeration (Map enumerations) {
		StringBuffer sb = new StringBuffer();
		for (Iterator i=enumerations.values().iterator(); i.hasNext(); ) {
			if (sb.length() > 0) sb.append(",");
			sb.append(i.next());
		}
		return sb.toString();
	}

	private void addContextReference (Document doc, String nodeName, Context context, String contextRef, String indent) {
		String str = getNodes(doc, nodeName, indent);
		if (null != str) {
			context.put(contextRef, str);
		}
	}
	
	private String getNodes (Document document, String nodeName, String indent) {
		Element element = document.getDocumentElement();
		NodeList nl = element.getElementsByTagName(nodeName);
		if (nl.getLength() > 0) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			XMLFormatter formatter = new XMLFormatter(pw);
			formatter.printNodes(nl, indent);
			return sw.toString();
		}
		else {
			return null;
		}
	}
}