package com.hudson.hibernatesynchronizer.editors.synchronizer;

/**
 * @author Joe Hudson
 */
public interface ActionPerformer {

	public void performAction () throws Exception;

	public String getToolTipText ();
}
