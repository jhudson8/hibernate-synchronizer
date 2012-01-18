package com.hudson.hibernatesynchronizer.editors.synchronizer.outline;

import org.eclipse.swt.graphics.Image;

import com.hudson.hibernatesynchronizer.Plugin;
import com.hudson.hibernatesynchronizer.mapping.HibernateClass;
import com.hudson.hibernatesynchronizer.mapping.HibernateClassCollectionProperty;
import com.hudson.hibernatesynchronizer.mapping.HibernateClassId;
import com.hudson.hibernatesynchronizer.mapping.HibernateClassProperty;
import com.hudson.hibernatesynchronizer.mapping.HibernateComponentClass;
import com.hudson.hibernatesynchronizer.mapping.HibernateDocument;
import com.hudson.hibernatesynchronizer.mapping.HibernateQuery;

public class LabelProvider extends org.eclipse.jface.viewers.LabelProvider {

	public Image getImage(Object element) {
		if (element instanceof HibernateDocument) {
			return null;
		}
		else if (element instanceof HibernateComponentClass) {
			return Plugin.getDefault().getImageRegistry().get("nav_component");
		}
		else if (element instanceof HibernateClass) {
			return Plugin.getDefault().getImageRegistry().get("nav_class");
		}
		else if (element instanceof HibernateClassId) {
			return Plugin.getDefault().getImageRegistry().get("nav_key");
		}
		else if (element instanceof HibernateClassCollectionProperty) {
			return Plugin.getDefault().getImageRegistry().get("nav_list");
		}
		else if (element instanceof HibernateClassProperty) {
			if (((HibernateClassProperty) element).isManyToOne())
				if (((HibernateClassProperty) element).isRequired())
					return Plugin.getDefault().getImageRegistry().get("nav_many_to_one_required");
				else
					return Plugin.getDefault().getImageRegistry().get("nav_many_to_one");
			else if (((HibernateClassProperty) element).isOneToOne()) {
				if (((HibernateClassProperty) element).isRequired())
					return Plugin.getDefault().getImageRegistry().get("nav_one_to_one_required");
				else
					return Plugin.getDefault().getImageRegistry().get("nav_one_to_one");
			} else {
				if (((HibernateClassProperty) element).isRequired())
					return Plugin.getDefault().getImageRegistry().get("nav_property_required");
				else
					return Plugin.getDefault().getImageRegistry().get("nav_property");
			}
		}
		else if (element instanceof HibernateQuery) {
			return Plugin.getDefault().getImageRegistry().get("nav_query");
		}
		else return null;
	}	
	
	public String getText(Object element) {
		if (element instanceof HibernateDocument) {
			return ((HibernateDocument) element).getFile().getName();
		}
		else if (element instanceof HibernateComponentClass) {
			return ((HibernateComponentClass) element).getName();
		}
		else if (element instanceof HibernateClass) {
			return ((HibernateClass) element).getValueObjectClassName();
		}
		else if (element instanceof HibernateClassCollectionProperty) {
			return ((HibernateClassCollectionProperty) element).getName();
		}
		else if (element instanceof HibernateClassId) {
			if (null != ((HibernateClassId) element).getProperty().getName())
				return ((HibernateClassId) element).getProperty().getName();
			else
				return "";
		}
		else if (element instanceof HibernateClassProperty) {
			return ((HibernateClassProperty) element).getName();
		}
		else if (element instanceof HibernateQuery) {
			return ((HibernateQuery) element).getName();
		}
		else return null;
	}
}