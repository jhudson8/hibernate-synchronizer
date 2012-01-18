/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.obj;

import java.util.List;

/**
 * @author <a href="mailto: joe@binamics.com">Joe Hudson </a>
 */
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
     * Return the actual property name for this property (first letter upper
     * case)
     */
    public String getPropName();

    /**
     * Return the getter name (without the parenthesis) for this property
     * 
     * @return the getter name
     */
    public String getGetterName();

    /**
     * Return the setter name (without the parenthesis) for this property
     * 
     * @return the setter name
     */
    public String getSetterName();

    /**
     * Return the name used as the Java variable name for this property (first
     * letter lower case)
     * 
     * @return the Java variable name
     */
    public String getJavaName();

    /**
     * Return the fully qualified class name that represents this property
     */
    public String getFullClassName();

    /**
     * Return the name of the class without the the package prefix that
     * represents this property
     */
    public String getClassName();

    /**
     * Return the package prefix for this property class without the class name
     */
    public String getPackageName();

    /**
     * Return the parent class for this property
     * 
     * @return the parent HibernateClass
     */
    public HibernateClass getParent();

    /**
     * Return the column name that this represents
     * 
     * @return the column name
     */
    public String getColumn();

    public List getMetaData();
}