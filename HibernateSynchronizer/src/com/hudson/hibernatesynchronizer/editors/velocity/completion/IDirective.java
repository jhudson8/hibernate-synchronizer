package com.hudson.hibernatesynchronizer.editors.velocity.completion;

import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.IDocument;

public interface IDirective {

	public void load (int start, int length, IDocument document);

	public boolean requiresEnd();
	
	public void addVariableAdditions(IProject project, ClassLoader classLoader, Map addedValues);
	
	public boolean isStackScope();
	
	public boolean isCursorInDirective (int pos);

	public List getCompletionProposals(IProject project, int pos, Map addedValues, ClassLoader loader) throws Exception;
}
