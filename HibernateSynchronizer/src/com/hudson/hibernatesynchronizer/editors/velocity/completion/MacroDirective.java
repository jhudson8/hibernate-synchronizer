package com.hudson.hibernatesynchronizer.editors.velocity.completion;

import java.util.Map;

import org.eclipse.core.resources.IProject;


public class MacroDirective extends AbstractDirective {

	/* (non-Javadoc)
	 * @see com.hudson.hibernatesynchronizer.editors.velocity.cursor.AbstractDirective#loadVariables()
	 */
	protected void addVariables(IProject project, ClassLoader loader, Map variables) {
		super.loadVariables(project, loader, variables);
		System.out.println(getInsideText());
	}
}
