/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.obj;

import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.hudson.hibernatesynchronizer.exception.AttributeNotSpecifiedException;
import com.hudson.hibernatesynchronizer.util.HSUtil;

/**
 * @author <a href="mailto: joe@binamics.com">Joe Hudson </a>
 * 
 * This represents data related to the 'composite-element' node of the hibernate
 * mapping configuration file.
 */
public class HibernateComponentClass extends HibernateClass implements
        IHibernateClassProperty {

    private HibernateClassProperty componentParent;

    private String name;

    private boolean dynamic;

    /**
     * @param node
     * @param packageName
     * @param parent
     * @param currentProject
     */
    public HibernateComponentClass(Node node, String packageName,
            HibernateClass parent, boolean dynamic, IProject currentProject) {
        super(node, packageName, parent, currentProject, false, TYPE_COMPONENT);
        this.dynamic = dynamic;
        NamedNodeMap attributes = node.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attNode = attributes.item(i);
            if (attNode.getNodeName().equals("class")) {
                super.setName(attNode.getNodeValue());
            }
        }
        if (node.hasChildNodes()) {
            Node child = node.getFirstChild();
            while (null != child) {
                if (child.getNodeName().equals("parent")) {
                    componentParent = new HibernateClassProperty(this, child,
                            false);
                }
                child = child.getNextSibling();
            }
        }
        if (null == fullClassName && !isDynamic()) {
            throw new AttributeNotSpecifiedException(node, "class");
        }
    }

    /**
     * @see com.hudson.hibernatesynchronizer.obj.HibernateClass#setName(java.lang.String)
     */
    public String getName() {
        return name;
    }

    /**
     * Return a descriptive label based on the property name
     */
    public String getLabel() {
        if (getCustomProperties().size() == 0)
            return HSUtil.getPropDescription(getName());
        else {
            String label = get(IHibernateClassProperty.LABEL_METADATA);
            if (null == label)
                return HSUtil.getPropDescription(getName());
            else
                return label;
        }
    }

    /**
     * @see com.hudson.hibernatesynchronizer.obj.HibernateClass#setName(java.lang.String)
     */
    protected void setName(String name) {
        this.name = name;
    }

    /**
     * @return Returns the compositeParent or null if the 'parent' node was not
     *         specified.
     */
    public HibernateClassProperty getComponentParent() {
        return componentParent;
    }

    /**
     * Return the actual property name for this property (first letter upper
     * case)
     */
    public String getPropName() {
        return HSUtil.firstLetterUpper(name);
    }

    /**
     * Return the getter name (without the parenthesis) for this property
     * 
     * @return the getter name
     */
    public String getGetterName() {
        return "get" + getPropName();
    }

    /**
     * Return the setter name (without the parenthesis) for this property
     * 
     * @return the setter name
     */
    public String getSetterName() {
        return "set" + getPropName();
    }

    /**
     * Return the name used as the Java variable name for this property (first
     * letter lower case)
     * 
     * @return the Java variable name
     */
    public String getJavaName() {
        return "_" + HSUtil.firstLetterLower(name);
    }

    public boolean isSubclass() {
        return false;
    }

    public HibernateClass getParentRoot() {
        return this;
    }

    private static final String[] IP = new String[] { "class", "name", "table",
            "discriminator", "mutable", "schema", "proxy", "dynamic-update",
            "dynamic-insert", "select-before-update", "polymorphism", "where",
            "persister", "batch-size", "optimistic-lock", "lazy" };

    protected String[] getInvalidProperties() {
        return IP;
    }

    public String getColumn() {
        return null;
    }

    /**
     * @return Returns the dynamic.
     */
    public boolean isDynamic() {
        return dynamic;
    }

    /**
     * @param dynamic
     *            The dynamic to set.
     */
    public void setDynamic(boolean dynamic) {
        this.dynamic = dynamic;
    }

    public String getFullClassName() {
        if (isDynamic())
            return Map.class.getName();
        else
            return super.getFullClassName();
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

    public boolean isComponent() {
        return true;
    }
}