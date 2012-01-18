/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.outline;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import com.hudson.hibernatesynchronizer.Plugin;
import com.hudson.hibernatesynchronizer.obj.HibernateClass;
import com.hudson.hibernatesynchronizer.obj.HibernateClassCollectionProperty;
import com.hudson.hibernatesynchronizer.obj.HibernateClassId;
import com.hudson.hibernatesynchronizer.obj.HibernateClassProperty;
import com.hudson.hibernatesynchronizer.obj.HibernateComponentClass;
import com.hudson.hibernatesynchronizer.obj.HibernateQuery;

/**
 * @author <a href="mailto: joe@binamics.com">Joe Hudson </a>
 */
public class HibernateOutlineLabelProvider extends LabelProvider {

    public Image getImage(Object element) {
        if (element instanceof HibernateComponentClass) {
            return Plugin.getDefault().getImageRegistry().get("nav_component");
        } else if (element instanceof HibernateClass) {
            return Plugin.getDefault().getImageRegistry().get("nav_class");
        } else if (element instanceof HibernateClassId) {
            return Plugin.getDefault().getImageRegistry().get("nav_key");
        } else if (element instanceof HibernateClassCollectionProperty) {
            return Plugin.getDefault().getImageRegistry().get("nav_list");
        } else if (element instanceof HibernateClassProperty) {
            if (((HibernateClassProperty) element).isManyToOne())
                if (((HibernateClassProperty) element).isRequired())
                    return Plugin.getDefault().getImageRegistry().get(
                            "nav_many_to_one_required");
                else
                    return Plugin.getDefault().getImageRegistry().get(
                            "nav_many_to_one");
            else if (((HibernateClassProperty) element).isOneToOne()) {
                if (((HibernateClassProperty) element).isRequired())
                    return Plugin.getDefault().getImageRegistry().get(
                            "nav_one_to_one_required");
                else
                    return Plugin.getDefault().getImageRegistry().get(
                            "nav_one_to_one");
            } else {
                if (((HibernateClassProperty) element).isRequired())
                    return Plugin.getDefault().getImageRegistry().get(
                            "nav_property_required");
                else
                    return Plugin.getDefault().getImageRegistry().get(
                            "nav_property");
            }
        } else if (element instanceof HibernateQuery) {
            return Plugin.getDefault().getImageRegistry().get("nav_query");
        } else
            return null;
    }

    public String getText(Object element) {
        if (element instanceof HibernateComponentClass) {
            return ((HibernateComponentClass) element).getJavaName();
        } else if (element instanceof HibernateClass) {
            return ((HibernateClass) element).getClassName();
        } else if (element instanceof HibernateClassCollectionProperty) {
            return ((HibernateClassCollectionProperty) element).getName();
        } else if (element instanceof HibernateClassId) {
            if (null != ((HibernateClassId) element).getProperty().getName())
                return ((HibernateClassId) element).getProperty().getName();
            else
                return "";
        } else if (element instanceof HibernateClassProperty) {
            return ((HibernateClassProperty) element).getName();
        } else if (element instanceof HibernateQuery) {
            return ((HibernateQuery) element).getName();
        } else
            return null;
    }
}