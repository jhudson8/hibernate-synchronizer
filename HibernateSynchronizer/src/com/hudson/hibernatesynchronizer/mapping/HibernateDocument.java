package com.hudson.hibernatesynchronizer.mapping;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.hudson.hibernatesynchronizer.Plugin;
import com.hudson.hibernatesynchronizer.exceptions.HibernateSynchronizerException;
import com.hudson.hibernatesynchronizer.util.HibernateDOMParser;

public class HibernateDocument {
	
	private Document document;
	private IFile file;
	private String packageName;
	private List classes;
	private List queries;
	private long lastModTime;

	private boolean hasTopLevelSubclassNodes;

	/**
	 * Load the document from the file contents
	 * @param file
	 */
	public HibernateDocument (IFile file) throws Exception {
		this.file = file;
		load (file.getContents());
		this.lastModTime = file.getLocalTimeStamp();
	}

	/**
	 * Load this document from the given input stream
	 * @param is
	 */
	public void load (InputStream is) throws Exception {
		classes = new ArrayList();
		queries = new ArrayList();

		HibernateDOMParser domParser = new HibernateDOMParser();
		try {
			/*
			// I can't use this until I figure out how to get line numbers from the DocumentBuilder
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder parser = dbf.newDocumentBuilder();
			parser.setEntityResolver(new LocalEntityResolver());
			Document doc = parser.parse(is);
			*/
			domParser.parse(new InputSource(is));
			document = domParser.getDocument();

			Node node = document.getDocumentElement();
			if (null != node) {
				NamedNodeMap attributes = node.getAttributes();
				if (null != attributes) {
					Node attNode = attributes.getNamedItem("package");
					if (null != attNode) {
						packageName = attNode.getNodeValue();
						if (packageName.endsWith(".")) {
							packageName = packageName.substring(0, packageName.length() - 1);
						}
					}
				}
			}
			NodeList nl = document.getElementsByTagName("class");
			classes = new ArrayList();
			for (int j=0; j<nl.getLength(); j++) {
				node = nl.item(j);
				classes.add(new HibernateClass(node, packageName, file.getProject(), this));
			}
			if (classes.size() >1) {
			    int parentClasses = 0;
			    HibernateClass parentClass = null;
			    for (Iterator i=classes.iterator(); i.hasNext(); ) {
			        HibernateClass hc = (HibernateClass) i.next();
			        if (!hc.isSubclass()) {
			            parentClass = hc;
			            parentClasses++;
			        }
			    }
			}

			// subclasses
			nl = document.getElementsByTagName("subclass");
			hasTopLevelSubclassNodes = (nl.getLength() > 0);
			nl = document.getElementsByTagName("joined-subclass");
			hasTopLevelSubclassNodes = (hasTopLevelSubclassNodes || nl.getLength() > 0);

			NodeList queries = document.getElementsByTagName("query");
			this.queries = new ArrayList();
			if (queries.getLength() > 0 && classes.size() > 0) {
				for (int k=0; k<queries.getLength(); k++) {
					Node query = queries.item(k);
					this.queries.add(new HibernateQuery(query, (HibernateClass) classes.get(0)));
				}
			}
			queries = document.getElementsByTagName("sql-query");
			if (queries.getLength() > 0 && classes.size() > 0) {
				for (int k=0; k<queries.getLength(); k++) {
					Node query = queries.item(k);
					this.queries.add(new HibernateQuery(query, (HibernateClass) classes.get(0)));
				}
			}
			if (classes.size() == 1) {
				((HibernateClass) classes.get(0)).setQueries(this.queries);
			}
		}
		catch (HibernateSynchronizerException e) {
			if (null != e.getNode()) {
				Integer lineNumber = domParser.getLineNumber(e.getNode());
				if (null != lineNumber) e.setLineNumber(lineNumber.intValue());
			}
			throw e;
		}
		finally {
			if (null != is) is.close();
		}
	}

	/**
	 * Load this document from the given input stream
	 * @param is
	 */
	public boolean loadTopLevelSubclasses (Map classes) throws Exception {
		NodeList nl = document.getElementsByTagName("subclass");
		for (int i=0; i<nl.getLength(); i++) {
			Node node = nl.item(i);
			Node att = node.getAttributes().getNamedItem("extends");
			if (null != att) {
				String parentClassName = att.getNodeValue();
				if (parentClassName.indexOf('.') < 0 && null != packageName) {
					parentClassName = att.getNodeValue() + "." + parentClassName;
					HibernateClass hc = (HibernateClass) classes.get(parentClassName);
					if (null != hc) {
						HibernateClass subclass = new HibernateClass(node, packageName, hc, file.getProject(), true, HibernateClass.TYPE_SUBCLASS, this);
						hc.subclassList.add(subclass);
						this.classes.add(subclass);
					}
				}
			}
		}
		nl = document.getElementsByTagName("joined-subclass");
		for (int i=0; i<nl.getLength(); i++) {
			Node node = nl.item(i);
			Node att = node.getAttributes().getNamedItem("extends");
			if (null != att) {
				String parentClassName = att.getNodeValue();
				if (parentClassName.indexOf('.') < 0 && null != packageName) parentClassName = packageName + "." + parentClassName;
				HibernateClass hc = (HibernateClass) classes.get(parentClassName);
				if (null != hc) {
					HibernateClass subclass = new HibernateClass(node, packageName, hc, file.getProject(), true, HibernateClass.TYPE_JOINED_SUBCLASS, this);
					hc.subclassList.add(subclass);
					this.classes.add(subclass);
				}
			}
		}
		if (this.classes.size() == 0) {
			return false;
		}
		else if (this.classes.size() == 1) {
			NodeList queries = document.getElementsByTagName("query");
			this.queries = new ArrayList();
			if (queries.getLength() > 0 && classes.size() > 0) {
				for (int k=0; k<queries.getLength(); k++) {
					Node query = queries.item(k);
					this.queries.add(new HibernateQuery(query, (HibernateClass) this.classes.get(0)));
				}
			}
			queries = document.getElementsByTagName("sql-query");
			if (queries.getLength() > 0 && classes.size() > 0) {
				for (int k=0; k<queries.getLength(); k++) {
					Node query = queries.item(k);
					this.queries.add(new HibernateQuery(query, (HibernateClass) this.classes.get(0)));
				}
			}
			((HibernateClass) this.classes.get(0)).setQueries(this.queries);
		}
		else {
			// see if there is one parent class
			int parentCount = 0;
			HibernateClass singleParent = null;
			for (Iterator i=this.classes.iterator(); i.hasNext(); ) {
				HibernateClass hc = (HibernateClass) i.next();
				if (!hc.isSubclass()) {
					parentCount++;
					singleParent = hc;
				}
			}
			if (parentCount == 1) singleParent.setQueries(this.queries);
		}
		return true;
	}

	public long getLastModTime () {
		return lastModTime;
	}

	public List getClasses() {
		return classes;
	}

	public List getQueries() {
		return queries;
	}

	/**
	 * @return Returns the file.
	 */
	public IFile getFile() {
		return file;
	}
	
	public String getFilePathInProject () {
		IPath path = file.getFullPath();
		StringBuffer sb = new StringBuffer();
		for (int i=0; i<path.segmentCount(); i++) {
			if (i > 0) {
				sb.append("/");
				sb.append(path.segments()[i]);
			}
		}
		return sb.toString();
	}

	/**
	 * @return Returns the packageName.
	 */
	public String getPackageName() {
		return packageName;
	}

	/**
	 * @return Returns the hasTopLevelSubclassNodes.
	 */
	protected boolean hasTopLevelSubclassNodes() {
		return hasTopLevelSubclassNodes;
	}

	/**
	 * Return the XML document relating to this mapping file
	 */
	public Document getDocument () {
		return document;
	}
	
	void addClass (HibernateClass hc) {
	    if (null == classes) classes = new ArrayList();
	    classes.add(hc);
	}

	public class LocalEntityResolver implements EntityResolver {
		/* (non-Javadoc)
		 * @see org.xml.sax.EntityResolver#resolveEntity(java.lang.String, java.lang.String)
		 */
		public InputSource resolveEntity(String publicId, String systemId)
				throws SAXException, IOException {
			if (Plugin.getDefault().isDevMode()) {
				File file = new File(Platform.resolve(Plugin.getDefault().getDescriptor().getInstallURL()).getFile() + "/src/hibernate-mapping.dtd");
				return new InputSource(new FileInputStream(file));
			}
			else {
				return new InputSource(getClass().getClassLoader().getResourceAsStream("/hibernate-mapping.dtd"));
			}
		}
	}

}