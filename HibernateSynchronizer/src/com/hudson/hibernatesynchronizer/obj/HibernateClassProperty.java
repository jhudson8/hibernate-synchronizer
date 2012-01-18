/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.obj;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.hudson.hibernatesynchronizer.editor.HibernateEditor;
import com.hudson.hibernatesynchronizer.exception.AttributeNotSpecifiedException;
import com.hudson.hibernatesynchronizer.exception.TransientPropertyException;
import com.hudson.hibernatesynchronizer.util.ClassInfo;
import com.hudson.hibernatesynchronizer.util.HSUtil;

/**
 * @author <a href="mailto: joe@binamics.com">Joe Hudson </a>
 * 
 * This represents data related to the 'property' node of the 'class' node in
 * the hibernate mapping configuration file.
 */
public class HibernateClassProperty extends BaseElement implements Comparable,
        IHibernateClassProperty {

    public static final int TYPE_PROPERTY = 1;

    public static final int TYPE_MANY_TO_ONE = 2;

    public static final int TYPE_ONE_TO_ONE = 3;

    public static final int TYPE_COLLECTION = 4;

    static final Map typeMap = new HashMap();

    static final Map primitiveMap = new HashMap();

    private HibernateClass parent;

    protected String name;

    protected String type;

    protected String column;

    protected boolean notNull;

    protected boolean alternateKey;

    protected boolean primaryKey;

    protected Integer length;

    private String actualType;

    private int refType;

    static {
        typeMap.put("string", String.class.getName());
        typeMap.put("binary", "byte[]");
        typeMap.put("int", "int");
        typeMap.put("float", "float");
        typeMap.put("long", "long");
        typeMap.put("double", "double");
        typeMap.put("char", "char");
        typeMap.put("yes_no", "boolean");
        typeMap.put("true_false", "boolean");
        typeMap.put("byte", "byte");
        typeMap.put("integer", Integer.class.getName());
        typeMap.put("currency", "java.util.Currency");
        typeMap.put("big_decimal", BigDecimal.class.getName());
        typeMap.put("character", Character.class.getName());
        typeMap.put("calendar", Calendar.class.getName());
        typeMap.put("calendar_date", Calendar.class.getName());
        typeMap.put("date", Date.class.getName());
        typeMap.put(Timestamp.class.getName(), Date.class.getName());
        typeMap.put("timestamp", Date.class.getName());
        typeMap.put("time", Date.class.getName());
        typeMap.put("locale", Locale.class.getName());
        typeMap.put("timezone", TimeZone.class.getName());
        typeMap.put("class", Class.class.getName());
        typeMap.put("serializable", Serializable.class.getName());
        typeMap.put("object", Object.class.getName());
        typeMap.put("blob", Blob.class.getName());
        typeMap.put("clob", Clob.class.getName());
        typeMap.put("text", String.class.getName());

        primitiveMap.put("int", Integer.class.getName());
        primitiveMap.put("short", Integer.class.getName());
        primitiveMap.put("float", Float.class.getName());
        primitiveMap.put("long", Long.class.getName());
        primitiveMap.put("double", Double.class.getName());
        primitiveMap.put("char", Character.class.getName());
        primitiveMap.put("boolean", Boolean.class.getName());
        primitiveMap.put("byte", Byte.class.getName());
    }

    public HibernateClassProperty(HibernateClass parent, Node node)
            throws TransientPropertyException {
        this(parent, node, TYPE_PROPERTY, null, true, false);
    }

    public HibernateClassProperty(HibernateClass parent, Node node,
            boolean validate) throws TransientPropertyException {
        this(parent, node, TYPE_PROPERTY, null, validate, false);
    }

    public HibernateClassProperty(HibernateClass parent, Node node,
            int refType, String packageName) throws TransientPropertyException {
        this(parent, node, refType, packageName, true, false);
    }

    public HibernateClassProperty(HibernateClass parent, Node node,
            int refType, String packageName, boolean validate,
            boolean isPrimaryKey) throws TransientPropertyException {
        this.parent = parent;
        setParentRoot(parent);
        setNode(node);
        this.refType = refType;
        this.primaryKey = isPrimaryKey;
        NamedNodeMap attributes = node.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attNode = attributes.item(i);
            if (attNode.getNodeName().equals("name")) {
                name = attNode.getNodeValue();
            } else if (attNode.getNodeName().equals("column")) {
                column = attNode.getNodeValue();
            } else if (attNode.getNodeName().equals("length")) {
                try {
                    setLength(new Integer(attNode.getNodeValue().trim()));
                } catch (Exception e) {
                }
            } else if (attNode.getNodeName().equals("not-null")) {
                if (attNode.getNodeValue().trim().length() > 0
                        && attNode.getNodeValue().trim().substring(0, 1)
                                .equalsIgnoreCase("t")) {
                    notNull = true;
                }
            } else if (attNode.getNodeName().equals("alternate-key")) {
                if (attNode.getNodeValue().trim().length() > 0
                        && attNode.getNodeValue().trim().substring(0, 1)
                                .equalsIgnoreCase("t")) {
                    alternateKey = true;
                }
            } else if (attNode.getNodeName().equals("type")) {
                if (!(isManyToOne() || isOneToOne()))
                    type = attNode.getNodeValue();
            } else if (attNode.getNodeName().equals("class")) {
                type = attNode.getNodeValue();
                if ((isManyToOne() || isOneToOne()) && null != packageName
                        && type.indexOf(".") == -1) {
                    type = packageName + "." + type;
                }
            }
        }

        Node child = node.getFirstChild();
        Node attNode = null;
        while (null != child) {
            if (child.getNodeName().equals("column")) {
                attributes = child.getAttributes();
                attNode = attributes.getNamedItem("name");
                if (null != attNode) {
                    column = attNode.getNodeValue();
                }
            } else if (child.getNodeName().equals("meta")) {
                String key = null;
                String value = null;
                attNode = child.getAttributes().getNamedItem("attribute");
                if (null != attNode) {
                    key = attNode.getNodeValue();
                }
                value = getNodeText(child);
                if (null != key && null != value) {
                    if (null != key && null != value) {
                        if (key.equals("alternate-key")
                                && value.toUpperCase().startsWith("T")) {
                            alternateKey = true;
                        } else if (key.equals("gen-property")
                                && value.toUpperCase().startsWith("T")) {
                            throw new TransientPropertyException();
                        }
                    }
                }
            }
            child = child.getNextSibling();
        }
        saveMetaData(node);
        actualType = get("PropertyType");
        if (null == actualType)
            actualType = get("ActualType");
        if (null != actualType) {
            getCustomProperties().remove("PropertyType");
            type = actualType;
        }

        if (validate && (null == name || name.length() == 0)) {
            throw new AttributeNotSpecifiedException(node, "name");
        }
        if (validate && (null == type || type.length() == 0)) {
            if (isManyToOne() || isOneToOne()) {
                throw new AttributeNotSpecifiedException(node, "class");
            } else {
                throw new AttributeNotSpecifiedException(node, "type");
            }
        }
    }

    public String toString() {
        return "\tColumn Name: " + name + "; Type: " + getFullClassName()
                + "; Column: " + column + "\n";
    }

    /**
     * Return the column name that this property represents
     */
    public String getColumn() {
        if (null != column) {
            return column;
        } else {
            return getName();
        }
    }

    /**
     * Return the defined property name for this property
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
     * Return the actual property name for this property (first letter upper
     * case)
     */
    public String getPropName() {
        return HSUtil.firstLetterUpper(getName());
    }

    /**
     * Return the getter name (without the parenthesis) for this property
     * 
     * @return the getter name
     */
    public String getGetterName() {
        String fullClassName = getFullClassName();
        if (fullClassName.equals("boolean")
                || fullClassName.equals(Boolean.class.getName())) {
            return "is" + getPropName();
        } else {
            return "get" + getPropName();
        }
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
        return "_" + HSUtil.firstLetterLower(getName());
    }

    /**
     * Return the value that was specified as the "type" attribute for this
     * property
     * 
     * @return the type attribute
     */
    public String getType() {
        return type;
    }

    void setType(String type) {
        this.type = type;
    }

    public String getSignatureFullClassName() {
        String className = getFullClassName();
        ClassInfo ci = (ClassInfo) HibernateEditor.getClassesCache(
                parent.getCurrentProject()).get(className);
        if (null == ci || null == ci.getProxyClassName())
            return className;
        else
            return ci.getClassPackage() + "." + ci.getProxyClassName();
    }

    public String getSignatureClassName() {
        String className = getClassName();
        ClassInfo ci = (ClassInfo) HibernateEditor.getClassesCache(
                parent.getCurrentProject()).get(className);
        if (null == ci || null == ci.getProxyClassName())
            return className;
        else
            return ci.getProxyClassName();
    }

    /**
     * Return the fully qualified class name that represents this property
     */
    public String getFullClassName() {
        if (null == type)
            return null;
        else {
            String rtnType = (String) typeMap.get(type);
            if (null == rtnType)
                return type;
            else
                return rtnType;
        }
    }

    /**
     * Return the name of the class without the the package prefix that
     * represents this property
     */
    public String getClassName() {
        return HSUtil.getClassPart(getFullClassName());
    }

    /**
     * Return the package prefix for this property class without the class name
     */
    public String getPackageName() {
        return HSUtil.getPackagePart(getFullClassName());
    }

    /**
     * Return the parent class for this property
     * 
     * @return the parent HibernateClass
     */
    public HibernateClass getParent() {
        return parent;
    }

    /**
     * Return true if this property can be determined as an enumeration and
     * false if not
     */
    public boolean isEnumeration() {
        return (!isManyToOne() && null != getParent().getPackageName()
                && getParent().getPackageName().trim().length() > 0 && getPackageName()
                .equals(getParent().getPackageName()));
    }

    /**
     * Return true if this property can not be null and false otherwise
     */
    public boolean isNotNull() {
        return notNull;
    }

    /**
     * Return true if this property can not be null and false otherwise
     */
    public boolean isRequired() {
        return notNull;
    }

    /**
     * @return
     */
    public boolean isAlternateKey() {
        return alternateKey;
    }

    public boolean isPrimaryKey() {
        return primaryKey;
    }

    /**
     * @param isAlternateKey
     */
    public void setAlternateKey(boolean isAlternateKey) {
        this.alternateKey = isAlternateKey;
        parent.alternateKeys = null;
    }

    /**
     * Return true if this property is a many-to-one and false otherwise
     */
    public boolean isManyToOne() {
        return refType == TYPE_MANY_TO_ONE;
    }

    /**
     * Return true if this property is a many-to-one and false otherwise
     */
    public boolean isOneToOne() {
        return refType == TYPE_ONE_TO_ONE;
    }

    public boolean isPrimitive() {
        return (null != primitiveMap.get(getFullClassName()));
    }

    public String getObjectClass() {
        if (!isPrimitive()) {
            return getFullClassName();
        } else {
            return (String) primitiveMap.get(getFullClassName());
        }
    }

    private static final String[] IP = new String[] { "class", "name", "type",
            "column", "update", "insert", "formula", "access", "unsaved-value",
            "length", "unique", "not-null", "alternate-key",
            "field-description" };

    protected String[] getInvalidProperties() {
        return IP;
    }

    public int compareTo(Object arg0) {
        if (arg0 instanceof HibernateClassProperty) {
            return getPropName().compareTo(
                    ((HibernateClassProperty) arg0).getPropName());
        } else {
            return -1;
        }
    }

    /**
     * @return Returns the length.
     */
    public Integer getLength() {
        return length;
    }

    /**
     * @param length
     *            The length to set.
     */
    public void setLength(Integer length) {
        this.length = length;
    }

    public String getStaticName() {
        return HSUtil.getStaticName(name);
    }
}