package com.hudson.hibernatesynchronizer.editors.synchronizer.completion;

public interface NodeEvaluator {
	
	/**
	 * Evaluate the node given and return true to keep processing and false to stop processing
	 */
	public boolean evaluate (Node node);
}
