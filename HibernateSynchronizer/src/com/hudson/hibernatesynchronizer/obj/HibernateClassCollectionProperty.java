/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.obj;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.hudson.hibernatesynchronizer.exception.AttributeNotSpecifiedException;

/**
 * @author <a href="mailto: joe@binamics.com">Joe Hudson </a>
 * 
 * This represents data related to the 'set', 'bag', 'list', 'map', and 'array'
 * nodes of the hibernate mapping configuration file.
 */
public class HibernateClassCollectionProperty extends HibernateClassProperty
        implements Comparable {

    public static final String TYPE_SET = "set";

    public static final String TYPE_BAG = "bag";

    public static final String TYPE_LIST = "list";

    public static final String TYPE_MAP = "map";

    public static final String TYPE_ARRAY = "array";

    public static final String TYPE_PRIMITIVE_ARRAY = "primitive-array";

    private String propType;

    private String implementation;

    private List compositeList = new ArrayList();

    /**
     * @param parent
     * @param node
     * @param isManyToOne
     */
    public HibernateClassCollectionProperty(HibernateClass parent, Node node,
            String propType, String packageName, IProject currentProject) {
        super(parent, node, TYPE_PROPERTY, packageName, false, false);
        this.propType = propType;
        if (isSet()) {
            super.type = Set.class.getName();
            this.implementation = HashSet.class.getName();
        } else if (isBag()) {
            super.type = Collection.class.getName();
            this.implementation = ArrayList.class.getName();
        } else if (isList()) {
            super.type = List.class.getName();
            this.implementation = ArrayList.class.getName();
        } else if (isMap()) {
            super.type = Map.class.getName();
            this.implementation = HashMap.class.getName();
        } else if (isArray()) {
            Node child = node.getFirstChild();
            while (null != child) {
                if (child.getNodeName().equals("many-to-many")
                        || child.getNodeName().equals("one-to-many")
                        || child.getNodeName().equals("many-to-any")) {
                    NamedNodeMap attributes = child.getAttributes();
                    if (null != attributes) {
                        Node attNode = attributes.getNamedItem("class");
                        if (null != attNode) {
                            String className = attNode.getNodeValue();
                            if (null != packageName
                                    && className.indexOf(".") < 0) {
                                className = packageName + "." + className;
                            }
                            super.type = className + "[]";
                            this.implementation = className;
                        }
                    }
                } else if (child.getNodeName().equals("element")) {
                    NamedNodeMap attributes = child.getAttributes();
                    if (null != attributes) {
                        Node attNode = attributes.getNamedItem("type");
                        if (null != attNode) {
                            String className = attNode.getNodeValue();
                            if (!propType.equals(TYPE_PRIMITIVE_ARRAY)) {
                                String s = (String) HibernateClassProperty.typeMap
                                        .get(className);
                                if (null != s)
                                    className = s;
                                if (null != packageName
                                        && className.indexOf(".") < 0) {
                                    className = packageName + "." + className;
                                }
                            }
                            super.type = className + "[]";
                            this.implementation = className;
                        }
                    }
                }
                child = child.getNextSibling();
            }
        }
        Node child = node.getFirstChild();
        while (null != child) {
            if (child.getNodeName().equals("composite-element")
                    || child.getNodeName().equals("nested-composite-element")) {
                compositeList.add(new HibernateComponentClass(child,
                        packageName, parent, false, currentProject));
            }
            child = child.getNextSibling();
        }
        if (null == getName() || getName().length() == 0) {
            throw new AttributeNotSpecifiedException(node, "name");
        }
        String signatureClass = get("SignatureClass");
        if (null != signatureClass) {
            super.type = signatureClass;
            clear("SignatureClass");
        }
        String implementationClass = get("ImplementationClass");
        if (null != implementationClass) {
            this.implementation = implementationClass;
            clear("ImplementationClass");
        }
    }

    /**
     * Return true if this collection represents a Set and false otherwise
     */
    public boolean isSet() {
        return TYPE_SET.equals(propType);
    }

    /**
     * Return true if this collection represents a Bag and false otherwise
     */
    public boolean isBag() {
        return TYPE_BAG.equals(propType);
    }

    /**
     * Return true if this collection represents a List and false otherwise
     */
    public boolean isList() {
        return TYPE_LIST.equals(propType);
    }

    /**
     * Return true if this collection represents a Map and false otherwise
     */
    public boolean isMap() {
        return TYPE_MAP.equals(propType);
    }

    /**
     * Return true if this collection represents an array and false otherwise
     */
    public boolean isArray() {
        return TYPE_ARRAY.equals(propType)
                || TYPE_PRIMITIVE_ARRAY.equals(propType);
    }

    public String getStaticName() {
        if (null == name)
            return name;
        if (name.toUpperCase().equals(name))
            return "PROP_" + name;
        else {
            StringBuffer sb = new StringBuffer();
            sb.append("PROP");
            for (int i = 0; i < name.toCharArray().length; i++) {
                char c = name.toCharArray()[i];
                if (Character.isUpperCase(c) || i == 0) {
                    sb.append("_");
                    sb.append(Character.toUpperCase(c));
                } else {
                    sb.append(Character.toUpperCase(c));
                }
            }
            return sb.toString();
        }
    }

    /**
     * Return the fully qualified implementation class based on the type of
     * collection this represents.
     */
    public String getImplementation() {
        return implementation;
    }

    private static final String[] IP = new String[] { "name", "table",
            "schema", "lazy", "inverse", "cascade", "sort", "order-by",
            "where", "outer-join", "batch-size", "access", "inverse" };

    protected String[] getInvalidProperties() {
        return IP;
    }

    public int compareTo(Object arg0) {
        if (arg0 instanceof HibernateClassCollectionProperty) {
            return getPropName().compareTo(
                    ((HibernateClassCollectionProperty) arg0).getPropName());
        } else {
            return -1;
        }
    }

    /**
     * @return Returns the compositeList.
     */
    public List getCompositeList() {
        return compositeList;
    }
}