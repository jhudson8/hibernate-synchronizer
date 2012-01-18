package com.hudson.hibernatesynchronizer.editors.velocity.completion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateCompletionProcessor;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.swt.graphics.Image;

import com.hudson.hibernatesynchronizer.editors.velocity.Editor;


public class CompletionProcessor extends TemplateCompletionProcessor implements IContentAssistProcessor {

	private Editor editor;
	
	public CompletionProcessor (Editor editor) {
		this.editor = editor;
	}
	
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		try {
			IDocument doc = viewer.getDocument();
			List typedOffsets = new ArrayList();
			String[] categories = doc.getPositionCategories();
			
			for (int i=0; i<categories.length; i++) {
				
				Position[] positions = doc.getPositions(categories[i]);
				for (int j=0; j<positions.length; j++) {
					typedOffsets.add(new Integer(positions[j].getOffset()));
				}
			}
			Collections.sort(typedOffsets);
			
			Stack directiveStack = new Stack();
			List noStackDirectives = new ArrayList();
			ITypedRegion region = null;
			IDirective lastDirective = null;
			for (Iterator i=typedOffsets.iterator(); i.hasNext(); ) {
				int tOffset = ((Integer) i.next()).intValue();
				if (tOffset > offset) break;
				region = doc.getPartition(tOffset);
				if (DirectiveFactory.isEndDirective(region.getType())) {
					// remove from the directiveStack
					if (directiveStack.size() > 0) {
						directiveStack.pop();
					}
				}
				else {
					IDirective directive = DirectiveFactory.getDirective(region.getType(), region, doc);
					if (null != directive) {
						if (directive.requiresEnd()) directiveStack.push(directive);
						else {
							noStackDirectives.add(directive);
							
						}
						lastDirective = directive;
					}
				}
			}
			List proposals = null;
			Map variableAdditions = new HashMap();
			for (Iterator i=noStackDirectives.iterator(); i.hasNext(); ) {
				IDirective directive = (IDirective) i.next();
				directive.addVariableAdditions(editor.getProject(), Thread.currentThread().getContextClassLoader(), variableAdditions);
			}
			for (Iterator i=directiveStack.iterator(); i.hasNext(); ) {
				IDirective directive = (IDirective) i.next();
				directive.addVariableAdditions(editor.getProject(), Thread.currentThread().getContextClassLoader(), variableAdditions);
			}
			if (null != lastDirective && lastDirective.isCursorInDirective(offset)) {
				proposals = lastDirective.getCompletionProposals(editor.getProject(), offset, variableAdditions, Thread.currentThread().getContextClassLoader());
			}
			else {
				// process completions for the body
				proposals = AbstractDirective.getCompletionProposals(editor.getProject(), doc, offset, variableAdditions, Thread.currentThread().getContextClassLoader(), false);
			}
			if (null != proposals) {
				Collections.sort(proposals, new CompletionProposalComparator());
				ICompletionProposal[] proposalArr = new ICompletionProposal[proposals.size()];
				int index=0;
				for (Iterator i=proposals.iterator(); i.hasNext(); ) {
					proposalArr[index++] = (ICompletionProposal) i.next();
				}
				return proposalArr;
			}
			else return null;
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	protected TemplateContextType getContextType(ITextViewer viewer, IRegion region) {
		return null;
	}
	protected Image getImage(Template template) {
		return null;
	}
	protected Template[] getTemplates(String contextTypeId) {
		return null;
	}
	public char[] getCompletionProposalAutoActivationCharacters() {
		return new char[]{'.', '$'};
	}

	public class CompletionProposalComparator implements Comparator {
		
		public int compare(Object o1, Object o2) {
			if (null == o1) return -1;
			else if (null == o2) return 1;
			else return (((ICompletionProposal) o1).getDisplayString().compareTo(((ICompletionProposal) o2).getDisplayString()));
		}
	}
}