/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.obj;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.hudson.hibernatesynchronizer.exception.AttributeNotSpecifiedException;

/**
 * @author <a href="mailto: joe@binamics.com">Joe Hudson </a>
 */
public class HibernateIdGenerator extends BaseElement {

    private HibernateClassId parent;

    private String generatorClass;

    public HibernateIdGenerator(HibernateClassId parent, Node node) {
        this.parent = parent;
        setNode(node);
        NamedNodeMap attributes = node.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attNode = attributes.item(i);
            if (attNode.getNodeName().equals("class")) {
                this.generatorClass = attNode.getNodeValue();
            }
        }
        saveMetaData(node);
        if (null == generatorClass) {
            throw new AttributeNotSpecifiedException(node, "class");
        }
    }

    /**
     * @return Returns the generatorClass.
     */
    public String getGeneratorClass() {
        return generatorClass;
    }

    /**
     * @return Returns the parent.
     */
    public HibernateClassId getParent() {
        return parent;
    }

    private static final String[] IP = new String[] { "class" };

    protected String[] getInvalidProperties() {
        return IP;
    }
}