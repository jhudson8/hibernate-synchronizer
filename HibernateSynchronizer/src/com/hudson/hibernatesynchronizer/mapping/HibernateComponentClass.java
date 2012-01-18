package com.hudson.hibernatesynchronizer.mapping;

import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.hudson.hibernatesynchronizer.exceptions.AttributeNotSpecifiedException;
import com.hudson.hibernatesynchronizer.util.HSUtil;

/**
 * @author <a href="mailto: jhudson8@users.sourceforge.net">Joe Hudson</a>
 * 
 * This represents data related to the 'composite-element' node of the hibernate
 * mapping configuration file.
 */
public class HibernateComponentClass extends HibernateClass implements IHibernateClassProperty {

	private HibernateClassProperty componentParent;
	private String name;
	private boolean dynamic;
	private String propertyType;
	private String scopeGet = "public";
	private String scopeSet = "public";
	private String scopeField = "private";

	/**
	 * @param node
	 * @param packageName
	 * @param parent
	 * @param currentProject
	 */
	public HibernateComponentClass(
		Node node,
		String packageName,
		HibernateClass parent,
		boolean dynamic,
		IProject currentProject) {
		super(node, packageName, parent, currentProject, false, TYPE_COMPONENT, null);
		this.dynamic = dynamic;
		NamedNodeMap attributes = node.getAttributes();
		for (int i=0; i<attributes.getLength(); i++) {
			Node attNode = attributes.item(i);
			if (attNode.getNodeName().equals("class")) {
				super.setValueObjectClassName(attNode.getNodeValue());
			}
			else if (attNode.getNodeName().equals("name")) {
				setName(attNode.getNodeValue());
			}
		}
		if (node.hasChildNodes()) {
			Node child = node.getFirstChild();
			while (null != child) {
				if (child.getNodeName().equals("parent")) {
					componentParent = new HibernateClassProperty(this, child, false);
				}
				child = child.getNextSibling();
			}
		}
		if (null == absoluteValueObjectClassName && !isDynamic()) {
			throw new AttributeNotSpecifiedException(node, "class");
		}
		getMetaData();
		if (null != get("scope-get", true)) scopeGet = get("scope-get", true);
		if (null != get("scope-set", true)) scopeSet = get("scope-set", true);
		if (null != get("scope-field", true)) scopeField = get("scope-field", true);
	}

	/**
	 * @see com.hudson.hibernatesynchronizer.mapping.HibernateClass#setName(java.lang.String)
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
	 * @see com.hudson.hibernatesynchronizer.mapping.HibernateClass#setName(java.lang.String)
	 */
	protected void setName(String name) {
		this.name = name;
	}
	
	/**
	 * @return Returns the compositeParent or null if the 'parent' node was
	 * not specified.
	 */
	public HibernateClassProperty getComponentParent() {
		return componentParent;
	}

	/**
	 * Return the actual property name for this property (first letter upper case)
	 */
	public String getPropName() {
		return HSUtil.firstLetterUpper(name);
	}

	/**
	 * Return the getter name (without the parenthesis) for this property
	 * @return the getter name
	 */
	public String getGetterName() {
		return "get" + getPropName();
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
		return "m_" + HSUtil.firstLetterLower(name);
	}

	/**
	 * This is never a subclass (return false)
	 */
	public boolean isSubclass() {
		return false;
	}

	/**
	 * Return self as parent
	 */
	public HibernateClass getParentRoot() {
		return this;
	}

	/**
	 * Return the reserved properties associated with this element
	 */
	protected String[] getReservedProperties() {
		return IP;
	}
	private static final String[] IP = new String[] {"class", "name", "table", "discriminator", "mutable", "schema", "proxy", "dynamic-update", "dynamic-insert", "select-before-update", "polymorphism", "where", "persister", "batch-size", "optimistic-lock", "lazy"};

	/**
	 * This relates to no column... return null
	 */
	public String getColumn () {
		return null;
	}

	/**
	 * @return Returns the dynamic.
	 */
	public boolean isDynamic() {
		return dynamic;
	}
	/**
	 * @param dynamic The dynamic to set.
	 */
	public void setDynamic(boolean dynamic) {
		this.dynamic = dynamic;
	}

	/**
	 * Return the fully qualified class name
	 * @return
	 */
	public String getFullClassName() {
		if (isDynamic()) return Map.class.getName();
		else return super.getAbsoluteValueObjectClassName();
	}

	/**
	 * Return the package for this component
	 */
	public String getPackage () {
		return super.getValueObjectPackage();
	}

	/**
	 * Return the static variable name for this class property
	 */
	public String getStaticName () {
		return HSUtil.getStaticName(name);
	}

	/**
	 * This is a component... return true
	 */
	public boolean isComponent () {
	    return true;
	}

	public boolean isPrimitive() {
		return false;
	}

	/**
	 * Return the object class representation for this class
	 */
	public String getObjectClass () {
		return getAbsoluteValueObjectClassName();
	}

	public String getClassName () {
		return getValueObjectClassName();
	}
	
	public String getSignatureClassName () {
		return getValueObjectSignatureClassName();
	}
	
	public String getAbsoluteSignatureClassName () {
		return getAbsoluteValueObjectSignatureClassName();
	}
	
	public String getAbsoluteClassName () {
		return getAbsoluteValueObjectClassName();
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
}