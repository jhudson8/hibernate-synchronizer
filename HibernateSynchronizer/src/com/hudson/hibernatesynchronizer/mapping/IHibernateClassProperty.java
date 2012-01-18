package com.hudson.hibernatesynchronizer.mapping;

import java.util.List;

public interface IHibernateClassProperty {

	public static final String LABEL_METADATA = "label";
	
	/**
	 * Return the defined property name for this property
	 */
	public String getName();

	/**
	 * Return a descriptive label based on the property name
	 */
	public String getLabel();
	
	/**
	 * Return the actual property name for this property (first letter upper case)
	 */
	public String getPropName();

	/**
	 * Return the getter name (without the parenthesis) for this property
	 * @return the getter name
	 */
	public String getGetterName();

	/**
	 * Return the setter name (without the parenthesis) for this property
	 * @return the setter name
	 */
	public String getSetterName();

	/**
	 * Return the name used as the Java variable name for this property (first letter lower case)
	 * @return the Java variable name
	 */
	public String getVarName();
	
	/**
	 * Return the fully qualified class name that represents this property
	 */
	public String getAbsoluteClassName();

	/**
	 * Return the name of the class without the the package prefix that represents this property
	 */
	public String getClassName ();

	/**
	 * Return the fully qualified class name or interface if applicable that represents this property
	 */
	public String getAbsoluteSignatureClassName();

	/**
	 * Return the name of the class or interface if applicable without the the package prefix that represents this property
	 */
	public String getSignatureClassName ();

	/**
	 * Return the package prefix for this property class without the class name
	 */
	public String getPackage();

	/**
	 * Return the parent class for this property
	 * @return the parent HibernateClass
	 */
	public HibernateClass getParent();
	
	/**
	 * Return the column name that this represents
	 * @return the column name
	 */
	public String getColumn();

	/**
	 * Return the meta-data associated with this element
	 */
	public List getMetaData  ();

	/**
	 * Return true if the type of this property represents a primitive
	 */
	public boolean isPrimitive();

	/**
	 * Return the object class representation for this class
	 */
	public String getObjectClass ();

	/**
	 * Return the static name for this property
	 */
	public String getStaticName ();
}
