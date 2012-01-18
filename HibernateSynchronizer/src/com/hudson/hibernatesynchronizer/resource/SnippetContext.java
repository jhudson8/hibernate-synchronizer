package com.hudson.hibernatesynchronizer.resource;

import org.apache.velocity.context.Context;
import org.eclipse.core.resources.IProject;

import com.hudson.hibernatesynchronizer.Plugin;

public class SnippetContext {

	private Context context;
	private IProject project;

	/**
	 * Constructor
	 * @param context the Velocity context
	 * @param project the current project
	 */
	public SnippetContext(Context context, IProject project) {
		this.context = context;
		this.project = project;
	}

	/**
	 * Transform the snippet matching the name given with the current context and return the results
	 * @param name the snippet name
	 * @return the post transformation results
	 */
	public String get(String name) {
		try {
			Snippet s = ResourceManager.getInstance(project).getSnippet(name);
			if (null == s) return null;
			else return s.merge(context);
		}
		catch (Exception e) {
			Plugin.log("Snippet ERROR: " + name, e);
			return null;
		}
	}
}