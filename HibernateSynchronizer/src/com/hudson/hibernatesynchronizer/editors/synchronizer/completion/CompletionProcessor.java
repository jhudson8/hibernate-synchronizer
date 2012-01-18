package com.hudson.hibernatesynchronizer.editors.synchronizer.completion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateCompletionProcessor;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.swt.graphics.Image;

public class CompletionProcessor extends TemplateCompletionProcessor implements IContentAssistProcessor {

	private IFile file;
	
	public CompletionProcessor (IFile file) {
		this.file = file;
	}
	
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		IDocument doc = viewer.getDocument();
		CursorState cursorState = CursorState.getCursorState(doc, offset);
		if (null != cursorState.getCurrentNode()) {
			try {
				Node currentNode = cursorState.getCurrentNode();
				if (null != currentNode) {
					int state = currentNode.getState(offset);
					if (state == CursorState.STATE_WAITING_FOR_NODE_END) {
						return new ICompletionProposal[]{new CompletionProposal(">", offset, 0, offset+1)};
					}
					else if (state == CursorState.STATE_NODE_NAME || state == CursorState.STATE_WAITING_FOR_NODE_NAME) {
						int type = currentNode.getType();
						if (type == CursorState.TYPE_FOOTER) {
							Node headerNode = currentNode.getParent();
							if (null != headerNode) {
								String text = headerNode.getName();
								String actual = headerNode.getName() + '>';
								// scan for end node
								int endIndex = currentNode.getNameStart() + currentNode.getName().length();
								try {
									int i = currentNode.getNameStart() + currentNode.getName().length();
									char c = doc.getChar(i);
									while (Character.isWhitespace(c)) c = doc.getChar(++i);
									if (c == '>') {
										endIndex = i + 1;
									}
								}
								catch (BadLocationException e) {}
								return new ICompletionProposal[]{new CompletionProposal(actual, currentNode.getNameStart(), endIndex - currentNode.getNameStart(), actual.length(), null, text, null, null)};
							}
							else
								return null;
						}
						else {
							int start = currentNode.getNameStart();
							int end = start + currentNode.getName().length();
							String prefixUpper = doc.get(start, offset-start).toUpperCase();
							String[] proposalArr = Suggestor.getNodeSuggestions(currentNode.getParent());
							if (null == proposalArr) return null;
							List rtn = new ArrayList();
							for (int i=0; i<proposalArr.length; i++) {
								if (proposalArr[i].toUpperCase().startsWith(prefixUpper)) {
									rtn.add(new CompletionProposal(
											proposalArr[i],
											start,
											currentNode.getName().length(),
											proposalArr[i].length()));
								}
							}
							return getProposalArray(rtn);
						}
					}
					else if (state == CursorState.STATE_WAITING_FOR_ATTRIBUTE_NAME || state == CursorState.STATE_ATTRIBUTE_NAME) {
						Attribute attribute = currentNode.getAttribute(offset);
						if (null == attribute) attribute = currentNode.getAttribute(offset+1);
						int start = offset;
						int replaceLength = 0;
						String currentAttributeName = null;
						if (null != attribute) {
							 start = attribute.getNameOffset();
							 replaceLength = attribute.getName().length();
							 currentAttributeName = attribute.getName();
						}
						else {
							int i=offset;
							try {
								while (Character.isLetterOrDigit(doc.getChar(--i)));
							}
							catch (BadLocationException e) {}
							i++;
							start = i;
							i=offset;
							try {
								while (Character.isLetterOrDigit(doc.getChar(++i)));
							}
							catch (BadLocationException e) {}
							i--;
							replaceLength = i - start;
							currentAttributeName = doc.get(i, replaceLength);
						}
						StringBuffer postStr = new StringBuffer();
						int equalsIndex = -1;
						int index = start + currentAttributeName.length();
						try {
							char c = doc.getChar(index);
							while (Character.isWhitespace(c) || c == '=') {
								if (c == '=') {
									equalsIndex = index;
									break;
								}
								c = doc.getChar(++index);
							}
						}
						catch (BadLocationException e) {
						}
						if (equalsIndex == -1) {
							postStr.append("=\"");
						}
						
						String prefixUpper = doc.get(start, offset-start).toUpperCase();
						List proposalList = Suggestor.getAttributeSuggestions(currentNode, currentAttributeName);
						if (null == proposalList) return null;
						List rtn = new ArrayList();
						for (Iterator i=proposalList.iterator(); i.hasNext(); ) {
							String attributeName = (String) i.next();
							String actual = attributeName;
							if (postStr.length() > 0) {
								actual += postStr.toString();
							}
							if (attributeName.toUpperCase().startsWith(prefixUpper)) {
								rtn.add(new CompletionProposal(
										actual,
										start,
										replaceLength,
										actual.length(),
										null,
										attributeName,
										null,
										null));
							}
						}
						return getProposalArray(rtn);
					}
					else if (state == CursorState.STATE_WAITING_FOR_ATTRIBUTE_VALUE_QUOTE) {
						char c = doc.getChar(offset);
						int i=offset;
						try {
							while (Character.isWhitespace(doc.getChar(++i))) {}
							if (doc.getChar(i) == '\"') {
								// trim the spaces
								return new ICompletionProposal[] {new CompletionProposal("\"", offset, i-offset+1, 1)};
							}
						}
						catch (BadLocationException e) {}
						return null;
					}
					else if (state == CursorState.STATE_ATTRIBUTE_VALUE) {
						Attribute attribute = currentNode.getAttribute(offset);
						if (null == attribute) return null;
						int start = attribute.getValueOffset();
						
						StringBuffer postStr = new StringBuffer();
						int quoteIndex = -1;
						int index = start + attribute.getValue().length();
						try {
							char c = doc.getChar(index);
							while (true) {
								if (c == '\"') {
									quoteIndex = index;
									break;
								}
								else if (Character.isWhitespace(c)) {
									break;
								}
								c = doc.getChar(++index);
							}
						}
						catch (BadLocationException e) {
						}
						if (quoteIndex == -1) {
							postStr.append("\"");
						}
									
						String prefixUpper = doc.get(start, offset-start).toUpperCase();
						try {
							String[] proposalArr = Suggestor.getAttributeValueSuggestions(currentNode.getName(), attribute.getName());
							if (null == proposalArr) return null;
							List rtn = new ArrayList();
							for (int i=0; i<proposalArr.length; i++) {
								String attributeValue = proposalArr[i];
								String actual = attributeValue;
								if (postStr.length() > 0) {
									actual += postStr.toString();
								}
								if (attributeValue.toUpperCase().startsWith(prefixUpper)) {
									rtn.add(new CompletionProposal(
											actual,
											start,
											attribute.getValue().length(),
											actual.length(),
											null,
											attributeValue,
											null,
											null));
								}
							}
							return getProposalArray(rtn);
						}
						catch (ClassAttributeValueException e) {
							// class values
							String[] proposalArr = Suggestor.getClassSuggestions(file.getProject());
							String packageName = null;
							Node parentNode = currentNode;
							while (null != parentNode.getParent()) parentNode = parentNode.getParent();
							Attribute packageNameAttribute = parentNode.getAttribute("package");
							if (null != packageNameAttribute) {
								packageName = packageNameAttribute.getValue();
								if (packageName.trim().length() == 0) packageName = null;
							}

							List rtn = new ArrayList();
							for (int i=0; i<proposalArr.length; i++) {
								String attributeValue = proposalArr[i];
								String className = attributeValue;
								boolean useClassNameAsValue = false;
								int dotIndex = className.lastIndexOf('.');
								if (dotIndex > 0) {
									className = className.substring(dotIndex+1, className.length());
								}
								if (null != packageName && attributeValue.startsWith(packageName)) {
									useClassNameAsValue = true;
								}

								String actual = attributeValue;
								if (useClassNameAsValue) actual = className;
								if (postStr.length() > 0) {
									actual += postStr.toString();
								}
								if (attributeValue.toUpperCase().startsWith(prefixUpper) || className.toUpperCase().startsWith(prefixUpper)) {
									rtn.add(new CompletionProposal(
											actual,
											start,
											attribute.getValue().length(),
											actual.length(),
											null,
											className,
											null,
											null));
								}
							}
							return getProposalArray(rtn);
						}
					}
				}
			}
			catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		return null;
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
		return new char[]{'/', '<', '\"'};
	}
	
	private ICompletionProposal[] getProposalArray (List proposals) {
		if (null == proposals) return null;
		Collections.sort(proposals, new CompletionProposalComparator());
		ICompletionProposal[] proposalArr = new ICompletionProposal[proposals.size()];
		int index = 0;
		for (Iterator i=proposals.iterator(); i.hasNext(); ) {
			proposalArr[index++] = (ICompletionProposal) i.next();
		}
		return proposalArr;
	}

	public class CompletionProposalComparator implements Comparator {
		
		public int compare(Object o1, Object o2) {
			if (null == o1) return -1;
			else if (null == o2) return 1;
			else return (((ICompletionProposal) o1).getDisplayString().compareTo(((ICompletionProposal) o2).getDisplayString()));
		}
	}
}