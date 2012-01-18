package com.hudson.hibernatesynchronizer.widgets;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.widgets.Shell;


public interface ImportExportHandler {

	/**
	 * Import the resources and return the new set of all resources
	 * @throws Exception
	 */
	public List importResources(IProject project, Shell shell) throws Exception;

	/**
	 * Export the selected resouces
	 * @param selectedResources the List of Resource objects
	 * @throws Exception
	 */
	public void exportResources(List selectedResources, IProject project, Shell shell) throws Exception;
}
