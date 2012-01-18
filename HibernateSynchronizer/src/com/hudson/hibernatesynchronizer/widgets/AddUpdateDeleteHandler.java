package com.hudson.hibernatesynchronizer.widgets;

import java.util.List;

import org.eclipse.swt.widgets.Shell;

import com.hudson.hibernatesynchronizer.resource.Resource;


public interface AddUpdateDeleteHandler {

	/**
	 * Perform any steps to add the resource and return a list of all resources
	 * @throws Exception
	 */
	public List addResource(Shell shell) throws Exception;

	/**
	 * Perform any steps to update the given resource an return a list of all resources
	 * @param resource the resource to update
	 * @return
	 */
	public List updateResource(Resource resource, Shell shell) throws Exception;

	/**
	 * Perform any steps to delete the resource and return true if the resource was deleted
	 * @param resource the resource to delete
	 * @throws Exception
	 */
	public List deleteResource(Resource resource, Shell shell) throws Exception;

	/**
	 * Return true if the resource can be deleted and false if not
	 * @param resource
	 * @throws Exception
	 */
	public boolean canDelete (Resource resource) throws Exception;

	/**
	 * Restore the given resource to it's original form
	 * @param resource
	 * @param shell
	 * @throws Exception
	 */
	public List restore (Resource resource, Shell shell) throws Exception;

	/**
	 * Return true if the resource can be restored and false if not
	 * @param resource
	 * @throws Exception
	 */
	public boolean canRestore (Resource resource) throws Exception;
}