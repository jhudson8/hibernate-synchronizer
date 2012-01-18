/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.util;

import com.hudson.hibernatesynchronizer.obj.HibernateClass;

/**
 * @author <a href="mailto: joe@binamics.com">Joe Hudson </a>
 */
public class ClassInfo implements Comparable {
    private String className;

    private String classPackage;

    private String proxyClassName;

    public ClassInfo(HibernateClass hc) {
        this.className = hc.getClassName();
        this.classPackage = hc.getPackageName();
        this.proxyClassName = hc.getProxyClassName();
    }

    /**
     * @return Returns the className.
     */
    public String getClassName() {
        return className;
    }

    /**
     * @param className
     *            The className to set.
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * @return Returns the classPackage.
     */
    public String getClassPackage() {
        return classPackage;
    }

    /**
     * @param classPackage
     *            The classPackage to set.
     */
    public void setClassPackage(String classPackage) {
        this.classPackage = classPackage;
    }

    /**
     * @return Returns the proxyClassName.
     */
    public String getProxyClassName() {
        return proxyClassName;
    }

    /**
     * @param proxyClassName
     *            The proxyClassName to set.
     */
    public void setProxyClassName(String proxyClassName) {
        this.proxyClassName = proxyClassName;
    }

    public String getFullClassName() {
        if (null == classPackage || classPackage.trim().length() == 0) {
            return className;
        } else {
            return classPackage + "." + className;
        }
    }

    public int compareTo(Object arg0) {
        if (arg0 instanceof ClassInfo) {
            return getClassName().compareTo(((ClassInfo) arg0).getClassName());
        } else {
            return -1;
        }
    }
}