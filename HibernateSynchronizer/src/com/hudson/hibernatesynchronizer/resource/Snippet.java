package com.hudson.hibernatesynchronizer.resource;

import java.util.Properties;

import com.hudson.hibernatesynchronizer.Constants;

public class Snippet extends AbstractResource {
	/**
	 * @see com.hudson.hibernatesynchronizer.resource.AbstractResource#evaluateMetaData(java.util.Properties)
	 */
	protected void evaluateMetaData(Properties properties) {
	}
	protected String getFileExtension() {
		return Constants.EXTENSION_SNIPPET;
	}
	protected String getResourceDirectory() {
		return "snippets";
	}
}
