package com.hudson.hibernatesynchronizer.resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;

import com.hudson.hibernatesynchronizer.Constants;
import com.hudson.hibernatesynchronizer.util.HSUtil;


public class TemplateLocation {
	
	public static final String SEPARATOR = ":";
	
	private IProject project;
	private String outputProjectName;
	private Template template;
	private String location;
	private String name;
	private boolean override;
	private boolean enabled;

	public TemplateLocation () {}

	public TemplateLocation (IProject project) {
		outputProjectName = project.getName();
	}

	/**
	 * Constructor
	 * @param templateInfo the template locaton contents
	 * @param project the Eclipse project
	 * @throws IOException
	 */
	public TemplateLocation (InputStream templateInfo, IProject project) throws IOException {
		this.project = project;
		restore(HSUtil.getStringFromStream(templateInfo));
	}

	/**
	 * Constructor
	 * @param templateInfo the template locaton contents
	 * @param project the Eclipse project
	 * @throws IOException
	 */
	public TemplateLocation (String templateInfo, IProject project) throws IOException {
		this.project = project;
		restore(templateInfo);
	}

	/**
	 * Restore this template location given the String input
	 * @param s the string contents to restore from
	 * @throws IOException
	 */
	public void restore (String s) throws IOException {
		int start = 0;
		int index = s.indexOf(SEPARATOR);
		String templateName = s.substring(start, index);
		template = ResourceManager.getInstance(project).getTemplate(templateName);
		if (null != template) {
			start = index + 1;
			index = s.indexOf(SEPARATOR, start);
			name = s.substring(start, index);
			start = index + 1;
			index = s.indexOf(SEPARATOR, start);
			location = s.substring(start, index);
			int pIndex = location.indexOf("/");
			outputProjectName = location.substring(0, pIndex);
			location = location.substring(pIndex+1, location.length());
			start = index + 1;
			index = s.indexOf(SEPARATOR, start);
			override = new Boolean(s.substring(start, index)).booleanValue();
			start = index + 1;
			enabled = new Boolean(s.substring(start, s.length())).booleanValue();
		}
	}

	/**
	 * Return the string representation of this template location
	 */
	public String toString () {
		StringBuffer sb = new StringBuffer();
		sb.append(template.getName() + SEPARATOR);
		sb.append(name + SEPARATOR);
		sb.append(outputProjectName + "/");
		sb.append(location + SEPARATOR);
		sb.append(new Boolean(override).toString() + SEPARATOR);
		sb.append(new Boolean(enabled).toString());
		return sb.toString();
	}

	/**
	 * @return
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * @param enabled
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * @return
	 */
	public String getLocation() {
		return location;
	}

	/**
	 * Return the file path transformed with the given Velocity context
	 * @param context the velocity context
	 * @return the transformed name
	 * @throws IOException
	 * @throws ResourceNotFoundException
	 * @throws MethodInvocationException
	 * @throws ParseErrorException
	 */
	public String getLocation (Context context) throws ParseErrorException, MethodInvocationException, ResourceNotFoundException, IOException {
		StringWriter sw = new StringWriter();
		Constants.templateGenerator.evaluate(context, sw, Velocity.class.getName(), location);
		return sw.toString();
	}

	/**
	 * @param location
	 */
	public void setLocation(String location) {
		if (null == location) location = "";
		else location = location.trim();
		int index = location.indexOf('/');
		if (index == -1) {
			outputProjectName = location;
			this.location = "";
		}
		else {
			String test = location.substring(0, index);
			if (null != ResourcesPlugin.getWorkspace().getRoot().getProject(test)) {
				outputProjectName = test;
				this.location = location.substring(index+1, location.length());
			}
			else {
				outputProjectName = project.getName();
				this.location = location;
			}
		}
	}

	/**
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * Return the name transformed with the given Velocity context
	 * @param context the velocity context
	 * @return the transformed name
	 * @throws IOException
	 * @throws ResourceNotFoundException
	 * @throws MethodInvocationException
	 * @throws ParseErrorException
	 */
	public String getName (Context context) throws ParseErrorException, MethodInvocationException, ResourceNotFoundException, IOException {
		StringWriter sw = new StringWriter();
		Constants.templateGenerator.evaluate(context, sw, Velocity.class.getName(), name);
		return sw.toString();
	}

	/**
	 * @param name
	 */
	public void setName(String name) {
		if (null == name) this.name = null;
		else this.name = name.trim();
	}

	/**
	 * @return
	 */
	public boolean shouldOverride() {
		return override;
	}

	/**
	 * @param override
	 */
	public void setOverride(boolean override) {
		this.override = override;
	}

	/**
	 * @return
	 */
	public Template getTemplate() {
		return template;
	}

	/**
	 * @param template
	 */
	public void setTemplate(Template template) {
		this.template = template;
	}

	/**
	 * Validate the contents of this location based upon the template that this location references.
	 * A 0 length list means that it is valid
	 * @return a List of String errors
	 */
	public List validate () {
		List errors = new ArrayList();
		if (null == name || name.length() == 0) {
			errors.add("You must enter the name");
		}
		else if (name.indexOf(':') >= 0) {
			errors.add("The name must not contain a ':'");
		}
		if (null == location || location.trim().length() == 0) {
			location = "";
		}
		else if (location.indexOf(':') >= 0) {
			errors.add("The location must not contain a ':'");
		}
		else {
			if (getTemplate().isClassTemplate() && (location.indexOf("/") >  0 || location.indexOf("\\") > 0)) {
				errors.add("The package can not have file separators");
			}
		}
		return errors;
	}

	/**
	 * Return the output project for this template.  This is not necessarily the project that the template
	 * location belongs to.
	 */
	public IProject getOutputProject () {
		if (null == outputProjectName) return null;
		else return ResourcesPlugin.getWorkspace().getRoot().getProject(outputProjectName);
	}

	/**
	 * Return the name of the output project
	 */
	public String getOutputProjectName () {
		return outputProjectName;
	}

	public boolean isValid () {
		return (null != outputProjectName && null != template);
	}
}