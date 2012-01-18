package com.hudson.hibernatesynchronizer.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;

import org.apache.xerces.parsers.DOMParser;
import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.QName;
import org.apache.xerces.xni.XMLAttributes;
import org.apache.xerces.xni.XMLLocator;
import org.apache.xerces.xni.XNIException;
import org.w3c.dom.Node;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

/**
 * @author Joe Hudson
 */
public class HibernateDOMParser  extends DOMParser {

	private XMLLocator locator; 

	private HashMap nodeMap = new HashMap();
	
	public HibernateDOMParser () throws SAXException, IOException {
		try {
			setEntityResolver(new LocalEntityResolver());
			this.setFeature( "http://apache.org/xml/features/dom/defer-node-expansion", false ); 
		}
		catch (SAXNotRecognizedException e) {}
		catch (SAXNotSupportedException e) {}
	}
	
	/**
	 * @see org.apache.xerces.xni.XMLDocumentHandler#startDocument(org.apache.xerces.xni.XMLLocator, java.lang.String, org.apache.xerces.xni.Augmentations)
	 */
	public void startDocument(XMLLocator locator, String arg1, Augmentations arg2)
	throws XNIException {
		super.startDocument(locator, arg1, arg2);
		this.locator = locator;
	}
	
	public void startElement(QName elementQName, XMLAttributes attrList, Augmentations augs) 
	throws XNIException {
		super.startElement(elementQName, attrList, augs);
		try {
			
			Node node = (Node) this.getProperty(DOMParser.CURRENT_ELEMENT_NODE);
			if (null != node) {
				nodeMap.put(node, new Integer(locator.getLineNumber()));
			}
		}
		catch (SAXNotRecognizedException e) {
		}
		catch (SAXNotSupportedException e) {
		}
		
	}
	
	public Integer getLineNumber (Node node) {
		return (Integer) nodeMap.get(node);
	}

	public Integer getFirstNodeLineNumber () {
		return getLineNumber(getDocument().getDocumentElement());
	}

	public void startDTD(XMLLocator arg0, Augmentations arg1)
		throws XNIException {
	}

	public void endDTD(Augmentations arg0) throws XNIException {
	}

	public class LocalEntityResolver implements EntityResolver {
		/* (non-Javadoc)
		 * @see org.xml.sax.EntityResolver#resolveEntity(java.lang.String, java.lang.String)
		 */
		public InputSource resolveEntity(String publicId, String systemId)
				throws SAXException, IOException {
//			if (Plugin.getDefault().isDevMode()) {
//				File file = new File(Platform.resolve(Plugin.getDefault().getDescriptor().getInstallURL()).getFile() + "/src/hibernate-mapping.dtd");
//				return new InputSource(new FileInputStream(file));
//			}
//			else {
//				return new InputSource(getClass().getClassLoader().getResourceAsStream("/hibernate-mapping.dtd"));
//			}
			return new InputSource(new ByteArrayInputStream("".getBytes()));
		}
	}
}