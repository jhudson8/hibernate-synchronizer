package com.hudson.hibernatesynchronizer.mapping;

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

import com.hudson.hibernatesynchronizer.exceptions.AttributeNotSpecifiedException;
import com.hudson.hibernatesynchronizer.exceptions.TransientPropertyException;
import com.hudson.hibernatesynchronizer.util.HSUtil;


/**
 * @author <a href="mailto: jhudson8@users.sourceforge.net">Joe Hudson</a>
 * 
 * This represents data related to the 'property' node of the 'class' node in the hibernate
 * mapping configuration file.
 */
public class HibernateClassProperty extends BaseElement implements Comparable, IHibernateClassProperty {

	public static final int TYPE_PROPERTY = 1;
	public static final int TYPE_MANY_TO_ONE = 2;
	public static final int TYPE_ONE_TO_ONE = 3;
	public static final int TYPE_COLLECTION = 4;
	
	static final Map typeMap = new HashMap();
	static final Map primitiveMap = new HashMap();
	static final Map reservedVarNames = new HashMap();
	
	protected HibernateClass parent;
	protected String name;
	protected String type;
	protected String column;
	protected boolean notNull;
	protected boolean alternateKey;
	protected boolean primaryKey;
	protected Integer length;
	private String propertyType;
	private String scopeGet = "public";
	private String scopeSet = "public";
	private String scopeField = "private";
	private String finderMethod;
	
	private int refType;

	static {
		// these are all the known properties
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

		// these are all the known primitives
		primitiveMap.put("int", Integer.class.getName());
		primitiveMap.put("short", Integer.class.getName());
		primitiveMap.put("float", Float.class.getName());
		primitiveMap.put("long", Long.class.getName());
		primitiveMap.put("double", Double.class.getName());
		primitiveMap.put("char", Character.class.getName());
		primitiveMap.put("boolean", Boolean.class.getName());
		primitiveMap.put("byte", Byte.class.getName());
		
		// all known reserved variable names
		reservedVarNames.put("abstract", Boolean.TRUE);
		reservedVarNames.put("boolean", Boolean.TRUE);
		reservedVarNames.put("break", Boolean.TRUE);
		reservedVarNames.put("byte", Boolean.TRUE);
		reservedVarNames.put("case", Boolean.TRUE);
		reservedVarNames.put("catch", Boolean.TRUE);
		reservedVarNames.put("char", Boolean.TRUE);
		reservedVarNames.put("class", Boolean.TRUE);
		reservedVarNames.put("const", Boolean.TRUE);
		reservedVarNames.put("continue", Boolean.TRUE);
		reservedVarNames.put("default", Boolean.TRUE);
		reservedVarNames.put("do", Boolean.TRUE);
		reservedVarNames.put("double", Boolean.TRUE);
		reservedVarNames.put("else", Boolean.TRUE);
		reservedVarNames.put("extends", Boolean.TRUE);
		reservedVarNames.put("false", Boolean.TRUE);
		reservedVarNames.put("final", Boolean.TRUE);
		reservedVarNames.put("finally", Boolean.TRUE);
		reservedVarNames.put("float", Boolean.TRUE);
		reservedVarNames.put("for", Boolean.TRUE);
		reservedVarNames.put("goto", Boolean.TRUE);
		reservedVarNames.put("assert", Boolean.TRUE);
		reservedVarNames.put("if", Boolean.TRUE);
		reservedVarNames.put("implements", Boolean.TRUE);
		reservedVarNames.put("import", Boolean.TRUE);
		reservedVarNames.put("instanceof", Boolean.TRUE);
		reservedVarNames.put("int", Boolean.TRUE);
		reservedVarNames.put("interface", Boolean.TRUE);
		reservedVarNames.put("long", Boolean.TRUE);
		reservedVarNames.put("native", Boolean.TRUE);
		reservedVarNames.put("new", Boolean.TRUE);
		reservedVarNames.put("null", Boolean.TRUE);
		reservedVarNames.put("package", Boolean.TRUE);
		reservedVarNames.put("private", Boolean.TRUE);
		reservedVarNames.put("protected", Boolean.TRUE);
		reservedVarNames.put("public", Boolean.TRUE);
		reservedVarNames.put("return", Boolean.TRUE);
		reservedVarNames.put("short", Boolean.TRUE);
		reservedVarNames.put("static", Boolean.TRUE);
		reservedVarNames.put("strictfp", Boolean.TRUE);
		reservedVarNames.put("super", Boolean.TRUE);
		reservedVarNames.put("switch", Boolean.TRUE);
		reservedVarNames.put("synchronized", Boolean.TRUE);
		reservedVarNames.put("this", Boolean.TRUE);
		reservedVarNames.put("throw", Boolean.TRUE);
		reservedVarNames.put("throws", Boolean.TRUE);
		reservedVarNames.put("transient", Boolean.TRUE);
		reservedVarNames.put("true", Boolean.TRUE);
		reservedVarNames.put("try", Boolean.TRUE);
		reservedVarNames.put("void", Boolean.TRUE);
		reservedVarNames.put("volatile", Boolean.TRUE);
		reservedVarNames.put("while", Boolean.TRUE);
	}

	/**
	 * Constructor for standard property
	 * @param parent the HibernateClass this property belongs to
	 * @param node the XML node
	 * @throws TransientPropertyException
	 */
	public HibernateClassProperty (HibernateClass parent, Node node) throws TransientPropertyException {
		this(parent, node, TYPE_PROPERTY, null, true, false);
	}

	/**
	 * Constructor for standard property (validating optional)
	 * @param parent the HibernateClass this property belongs to
	 * @param node the XML node
	 * @param validate
	 * @throws TransientPropertyException
	 */
	public HibernateClassProperty (HibernateClass parent, Node node, boolean validate) throws TransientPropertyException {
		this(parent, node, TYPE_PROPERTY, null, validate, false);
	}

	/**
	 * Constructor for many-to-one property
	 * @param parent the HibernateClass this property belongs to
	 * @param node the XML node
	 * @param refType the relational type
	 * @param packageName the package for the relational class
	 * @throws TransientPropertyException
	 */
	public HibernateClassProperty (HibernateClass parent, Node node, int refType, String packageName) throws TransientPropertyException {
		this(parent, node, refType, packageName, true, false);
	}

	/**
	 * Constructor
	 * @param parent the HibernateClass this property belongs to
	 * @param node the XML node
	 * @param refType the relational type
	 * @param packageName the package for the relational class
	 * @param validate
	 * @param primaryKey
	 * @throws TransientPropertyException
	 */
	public HibernateClassProperty (HibernateClass parent, Node node, int refType, String packageName, boolean validate, boolean primaryKey) throws TransientPropertyException {
		this.parent = parent;
		setParentRoot(parent);
		setNode(node);
		this.refType = refType;
		this.primaryKey = primaryKey;
		NamedNodeMap attributes = node.getAttributes();
		for (int i=0; i<attributes.getLength(); i++) {
			Node attNode = attributes.item(i);
			if (attNode.getNodeName().equals("name")) {
				name = attNode.getNodeValue();
			}
			else if (attNode.getNodeName().equals("column")) {
				column = attNode.getNodeValue();
			}
			else if (attNode.getNodeName().equals("length")) {
				try {
					setLength(new Integer(attNode.getNodeValue().trim()));
				}
				catch (Exception e) {}
			}
			else if (attNode.getNodeName().equals("not-null")) {
				if (attNode.getNodeValue().trim().length() > 0 && attNode.getNodeValue().trim().substring(0, 1).equalsIgnoreCase("t")) {
					notNull = true;
				}
			}
			else if (attNode.getNodeName().equals("type")) {
				if (!(isManyToOne() || isOneToOne())) type = attNode.getNodeValue();
			}
			else if (attNode.getNodeName().equals("class")) {
				type = attNode.getNodeValue();
				if ((isManyToOne() || isOneToOne()) && null != packageName && type.indexOf(".") == -1) {
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
			}
			else if (child.getNodeName().equals("meta")) {
				String key = null;
				String value = null;
				attNode = child.getAttributes().getNamedItem("attribute");
				if (null != attNode) {
					key = attNode.getNodeValue();
				}
				value = getNodeText(child);
				if (null != key && null != value) {
					if (null != key && null != value) {
						if ((key.equals("alternate-key") && value.toUpperCase().startsWith("T"))
								|| (key.equals("use-in-equals") && value.toUpperCase().startsWith("T"))) {
							alternateKey = true;
						}
						if (key.equals("finder-method")) {
							finderMethod = value;
						}
						else if (key.equals("gen-property") && value.toUpperCase().startsWith("F")) {
							throw new TransientPropertyException();
						}
					}
				}
			}
			child = child.getNextSibling();
		}
		saveMetaData (node);

		// see if the implementation class is overridden
		if (null != get("property-type", true)) type = get("property-type", true);
		if (null != get("scope-get", true)) scopeGet = get("scope-get", true);
		if (null != get("scope-set", true)) scopeSet = get("scope-set", true);
		if (null != get("scope-field", true)) scopeField = get("scope-field", true);

		// validate
		if (validate && (null == name || name.length() == 0)) {
			throw new AttributeNotSpecifiedException(node, "name");
		}
		if (validate && (null == type || type.length() == 0)) {
			if (isManyToOne() || isOneToOne()) {
				throw new AttributeNotSpecifiedException(node, "class");
			}
			else {
				throw new AttributeNotSpecifiedException(node, "type");
			}
		}
	}

	/**
	 * Return the column name that this property represents
	 */
	public String getColumn() {
		if (null != column) {
			return column;
		}
		else {
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
	 * Return a descriptive label based on the property name or the label meta attribute if exists
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
	 * Return the actual property name for this property (first letter upper case)
	 */
	public String getPropName() {
		return HSUtil.firstLetterUpper(getName());
	}

	/**
	 * Return the getter name (without the parenthesis) for this property
	 * @return the getter name
	 */
	public String getGetterName() {
		String fullClassName = getAbsoluteClassName();
		if (fullClassName.equals("boolean") || fullClassName.equals(Boolean.class.getName())) {
			return "is" + getPropName();
		}
		else {
			return "get" + getPropName();
		}
	}

	/**
	 * Return the setter name (without the parenthesis) for this property
	 * @return the setter name
	 */
	public String getSetterName() {
		return "set" + getPropName();
	}

	/**
	 * Return the name used as the Java variable name for this property (first letter lower case)
	 * @return the Java variable name
	 */
	public String getVarName() {
		if (null == reservedVarNames.get(getName().toLowerCase())) return HSUtil.firstLetterLower(getName());
		else return "m_" + HSUtil.firstLetterLower(getName());
	}

	/**
	 * Return the value that was specified as the "type" attribute for this property
	 * @return the type attribute
	 */
	public String getType() {
		return type;
	}

	/**
	 * Set the type related to this property
	 * @param type
	 */
	void setType (String type) {
		this.type = type;
	}

	/**
	 * Return the fully qualified signature of the class representing this property
	 * (or the actual class if it is not a proxy)
	 */
	public String getAbsoluteSignatureClassName () {
		String className = getAbsoluteClassName();
		HibernateClass hc = HibernateMappingManager.getInstance(getParent().getProject()).getHibernateClass(className);
		if (null == hc) return className;
		else return hc.getAbsoluteValueObjectSignatureClassName();
	}

	/**
	 * Return the name of the class representing this property
	 * (or the actual class if it is not a proxy)
	 * @return
	 */
	public String getSignatureClassName () {
		String className = getClassName();
		HibernateClass hc = HibernateMappingManager.getInstance(getParent().getProject()).getHibernateClass(className);
		if (null == hc) return className;
		else return hc.getValueObjectClassName();
	}

	/**
	 * Return the fully qualified class name that represents this property
	 */
	public String getAbsoluteClassName() {
		if (null == type) return null;
		else {
			String rtnType = (String) typeMap.get(type);
			if (null == rtnType) return type;
			else return rtnType;
		}
	}

	/**
	 * Return the name of the class without the the package prefix that represents this property
	 */
	public String getClassName () {
		return HSUtil.getClassPart(getAbsoluteClassName());
	}

	/**
	 * Return the package prefix for this property class without the class name
	 */
	public String getPackage() {
		return HSUtil.getPackagePart(getAbsoluteClassName());
	}

	/**
	 * Return the parent class for this property
	 * @return the parent HibernateClass
	 */
	public HibernateClass getParent() {
		return parent;
	}

	/**
	 * Return true if this property can be determined as an user type and false if not
	 */
	public boolean isUserType () {
		return (!isManyToOne()
				&& null != getParent().getValueObjectPackage()
				&& getParent().getValueObjectPackage().trim().length() > 0
				&& getPackage().equals(getParent().getValueObjectPackage()));
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
	public boolean isManyToOne () {
		return refType == TYPE_MANY_TO_ONE;
	}

	/**
	 * Return true if this property is a many-to-one and false otherwise
	 */
	public boolean isOneToOne () {
		return refType == TYPE_ONE_TO_ONE;
	}

	/**
	 * Return true if the type of this property represents a primitive
	 */
	public boolean isPrimitive() {
		return (null != primitiveMap.get(type));
	}

	/**
	 * Return the object class representation for this class
	 */
	public String getObjectClass () {
		if (!isPrimitive()) {
			return getAbsoluteClassName();
		}
		else {
			return (String) primitiveMap.get(type);
		}
	}

	/**
	 * @return Returns the length.
	 */
	public Integer getLength() {
		return length;
	}

	/**
	 * @param length The length to set.
	 */
	public void setLength(Integer length) {
		this.length = length;
	}

	/**
	 * Return the static name for this property
	 * @return
	 */
	public String getStaticName () {
		return HSUtil.getStaticName(name);
	}

	/**
	 * Return the HibernateClass representing the parent to the foreign key relationship
	 * or null if N/A
	 */
	public HibernateClass getForeignParent () {
		return HibernateMappingManager.getInstance(parent.getProject()).getHibernateClass(getAbsoluteClassName());
	}

	/**
	 * Return the "getter" scope
	 */
	public String getScopeGet () {
		return scopeGet;
	}

	/**
	 * Return the "setter" scope
	 */
	public String getScopeSet () {
		return scopeSet;
	}

	/**
	 * Return the field scope
	 */
	public String getScopeField () {
		return scopeField;
	}

	/**
	 * Return the name of a finder method to create in the DAO
	 */
	public String getFinderMethod () {
		return finderMethod;
	}

	/**
	 * Return the reserved properties associated with this element
	 */
	protected String[] getReservedProperties() {
		return IP;
	}
	private static final String[] IP = new String[] {"class", "name", "type", "column", "update", "insert", "formula", "access", "unsaved-value", "length", "unique", "not-null", "alternate-key", "field-description", "scope-set", "scope-get", "scope-field", "use-in-equals", "property-type", "gen-property"};

	/**
	 * Compare this to another object
	 */
	public int compareTo(Object arg0) {
		if (arg0 instanceof HibernateClassProperty) {
			return getPropName().compareTo(((HibernateClassProperty) arg0).getPropName());
		}
		else {
			return -1;
		}
	}
}