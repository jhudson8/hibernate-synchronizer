package com.hudson.hibernatesynchronizer.resource;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.hudson.hibernatesynchronizer.Constants;
import com.hudson.hibernatesynchronizer.widgets.BooleanEditor;

public class Template extends AbstractResource {

	private static final String PROP_RESOURCE_TYPE = "ResourceType";
	private static final String PROP_JAVA_CLASS = "JavaClass";
	
	public static final String TYPE_CLASS = "C";
	public static final String TYPE_RESOURCE = "R";
	
	private String type;

	/**
	 * @see com.hudson.hibernatesynchronizer.resource.Resource#addToEditor(org.eclipse.swt.widgets.Composite)
	 */
	public Map addToEditor(Composite composite, Object listener) {
		Map map = new HashMap();
		new Label(composite, SWT.NULL);
		Boolean defaultValue = new Boolean(isClassTemplate());
		BooleanEditor typeEditor = new BooleanEditor(composite, PROP_JAVA_CLASS, PROP_JAVA_CLASS, new Boolean(isClassTemplate()), 1);
		typeEditor.getEditor().setPropertyChangeListener((IPropertyChangeListener) listener);
		map.put(PROP_JAVA_CLASS, typeEditor);
		return map;
	}

	/**
	 * @see com.hudson.hibernatesynchronizer.resource.AbstractResource#evaluateEditorProperties(java.util.Map)
	 */
	protected void evaluateEditorProperties(Map entryMap) {
		if (null != entryMap) {
			BooleanEditor typeEditor = (BooleanEditor) entryMap.get(PROP_JAVA_CLASS);
			if (typeEditor.getBooleanValue()) type = TYPE_CLASS;
			else type = TYPE_RESOURCE;
		}
		super.evaluateEditorProperties(entryMap);
	}

	/**
	 * @see com.hudson.hibernatesynchronizer.resource.AbstractResource#evaluateMetaData(java.util.Properties)
	 */
	protected void evaluateMetaData(Properties properties) {
		type = properties.getProperty(PROP_RESOURCE_TYPE);
	}

	/**
	 * Return true if this template is related to a Java class and false if not
	 */
	public boolean isClassTemplate () {
		return TYPE_CLASS.equals(type);
	}

	/**
	 * Return true if this template is related to a resource file and false if not
	 */
	public boolean isResourceTemplate () {
		return (null == type || TYPE_RESOURCE.equals(type));
	}

	protected Properties getMetaData () {
		if (null != type) {
			Properties p = new Properties();
			p.setProperty(PROP_RESOURCE_TYPE, type);
			return p;
		}
		else return null;
	}

	protected String getResourceDirectory() {
		return "templates";
	}
	protected String getFileExtension() {
		return Constants.EXTENSION_TEMPLATE;
	}
}