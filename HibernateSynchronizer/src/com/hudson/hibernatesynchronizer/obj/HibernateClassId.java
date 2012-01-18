/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.obj;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Node;

import com.hudson.hibernatesynchronizer.exception.CompositeKeyException;
import com.hudson.hibernatesynchronizer.exception.TransientPropertyException;

/**
 * @author <a href="mailto: joe@binamics.com">Joe Hudson </a>
 * 
 * This represents data related to the 'id' or 'composite-id' nodes of the
 * hibernate mapping configuration file.
 */
public class HibernateClassId extends BaseElement implements Comparable {

    private HibernateClass parent;

    private HibernateIdGenerator generator;

    private List properties;

    private HibernateClassProperty property;

    private boolean exists = false;

    private boolean isComposite = false;

    public HibernateClassId(HibernateClass parent, Node node, String packageName) {
        this.parent = parent;
        setParentRoot(parent);
        Node compositeNode = null;
        if (node.hasChildNodes()) {
            Node child = node.getFirstChild();
            while (null != child) {
                if (child.getNodeName().equals("id")) {
                    saveMetaData(child);
                    setNode(child);
                    property = new HibernateClassProperty(parent, child,
                            HibernateClassProperty.TYPE_PROPERTY, null, true,
                            true);
                    exists = true;
                    if (node.hasChildNodes()) {
                        Node cChild = child.getFirstChild();
                        while (null != cChild) {
                            if (cChild.getNodeName().equals("generator")) {
                                generator = new HibernateIdGenerator(this,
                                        cChild);
                            }
                            cChild = cChild.getNextSibling();
                        }
                    }
                    break;
                } else if (child.getNodeName().equals("composite-id")) {
                    saveMetaData(child);
                    setNode(child);
                    compositeNode = child;
                    properties = new ArrayList();
                    isComposite = true;
                    exists = true;
                    property = new HibernateClassProperty(parent, child,
                            HibernateClassProperty.TYPE_MANY_TO_ONE,
                            packageName, false, true);
                    if (node.hasChildNodes()) {
                        Node cChild = child.getFirstChild();
                        while (null != cChild) {
                            try {
                                if (cChild.getNodeName().equals("key-property")) {
                                    properties
                                            .add(new HibernateClassProperty(
                                                    parent,
                                                    cChild,
                                                    HibernateClassProperty.TYPE_PROPERTY,
                                                    null, true, true));
                                } else if (cChild.getNodeName().equals(
                                        "key-many-to-one")) {
                                    properties
                                            .add(new HibernateClassProperty(
                                                    parent,
                                                    cChild,
                                                    HibernateClassProperty.TYPE_MANY_TO_ONE,
                                                    packageName, true, true));
                                }
                            } catch (TransientPropertyException e) {
                            }
                            cChild = cChild.getNextSibling();
                        }
                    }
                }
                child = child.getNextSibling();
            }
        }
        if (exists) {
            if (null == getProperty().getType()) {
                getProperty().setType(parent.getFullClassName());
            }
            saveMetaData(node);
            if (isComposite()) {
                if (null == getProperties() || getProperties().size() <= 1) {
                    throw new CompositeKeyException(compositeNode,
                            "Composite ids must have 2 or more properties");
                }
            }
        }
    }

    public boolean hasExternalClass() {
        return !getProperty().getType().equals(parent.getFullClassName());
    }

    /**
     * Return the HibernateClass that this id belongs to
     */
    public HibernateClass getParent() {
        return parent;
    }

    /**
     * Return either the column property if this is a non-composite or the
     * object that represents the composite object.
     * 
     * @return a HibernateClassProperty representing the field value
     */
    public HibernateClassProperty getProperty() {
        return property;
    }

    /**
     * Return true if the id is a composite id and false otherwise
     */
    public boolean isComposite() {
        return isComposite;
    }

    /**
     * If this is a composite id, return a list of properties that make up the
     * composite
     * 
     * @return a List of HibernateClassProperty objects
     */
    public List getProperties() {
        return properties;
    }

    /**
     * Return true if the id exists for the parent and false if not
     */
    public boolean exists() {
        return exists;
    }

    /**
     * @return Returns the id generator.
     */
    public HibernateIdGenerator getGenerator() {
        return generator;
    }

    private static final String[] IP = new String[] { "name", "type", "column",
            "unsaved-value", "access", "class" };

    protected String[] getInvalidProperties() {
        return IP;
    }

    public int compareTo(Object arg0) {
        if (arg0 instanceof HibernateClassId) {
            return getProperty().getPropName().compareTo(
                    ((HibernateClassId) arg0).getProperty().getPropName());
        } else {
            return -1;
        }
    }
}