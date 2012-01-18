/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.parser;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import org.apache.xerces.parsers.DOMParser;
import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.QName;
import org.apache.xerces.xni.XMLAttributes;
import org.apache.xerces.xni.XMLLocator;
import org.apache.xerces.xni.XNIException;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

/**
 * @author <a href="mailto: joe@binamics.com">Joe Hudson </a>
 */
public class HibernateDOMParser extends DOMParser {
    private XMLLocator locator;

    private HashMap nodeMap = new HashMap();

    public HibernateDOMParser(InputStream is) throws SAXException, IOException {
        try {
            setEntityResolver(new BlankDTDEntityResolver());
            this.setFeature(
                    "http://apache.org/xml/features/dom/defer-node-expansion",
                    false);
            this.parse(new InputSource(is));
        } catch (SAXNotRecognizedException e) {
        } catch (SAXNotSupportedException e) {
        }
    }

    /**
     * @see org.apache.xerces.xni.XMLDocumentHandler#startDocument(org.apache.xerces.xni.XMLLocator,
     *      java.lang.String, org.apache.xerces.xni.Augmentations)
     */
    public void startDocument(XMLLocator locator, String arg1,
            Augmentations arg2) throws XNIException {
        super.startDocument(locator, arg1, arg2);
        this.locator = locator;
    }

    public void startElement(QName elementQName, XMLAttributes attrList,
            Augmentations augs) throws XNIException {
        super.startElement(elementQName, attrList, augs);
        try {

            Node node = (Node) this.getProperty(DOMParser.CURRENT_ELEMENT_NODE);
            if (null != node) {
                nodeMap.put(node, new Integer(locator.getLineNumber()));
            }
        } catch (SAXNotRecognizedException e) {
        } catch (SAXNotSupportedException e) {
        }

    }

    public Integer getLineNumber(Node node) {
        return (Integer) nodeMap.get(node);
    }

    public Integer getFirstNodeLineNumber() {
        return getLineNumber(getDocument().getDocumentElement());
    }

    public void startDTD(XMLLocator arg0, Augmentations arg1)
            throws XNIException {
    }

    public void endDTD(Augmentations arg0) throws XNIException {
    }
}