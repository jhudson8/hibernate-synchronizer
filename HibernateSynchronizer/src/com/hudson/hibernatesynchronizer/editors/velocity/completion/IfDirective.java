package com.hudson.hibernatesynchronizer.editors.velocity.completion;

import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;

public class IfDirective extends AbstractDirective {

	/* (non-Javadoc)
	 * @see com.hudson.hibernatesynchronizer.editors.velocity.cursor.AbstractDirective#canAddVariables()
	 */
	protected boolean canAddVariables() {
		return false;
	}

	public List getCompletionProposals (IProject project, int pos, Map addedValues, ClassLoader loader) throws Exception {
		return getCompletionProposals(project, document, pos, addedValues, loader, false);
	}
}
