package com.hudson.hibernatesynchronizer.editors.velocity.completion;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.CompletionProposal;

import com.hudson.hibernatesynchronizer.Plugin;
import com.hudson.hibernatesynchronizer.mapping.HibernateClass;
import com.hudson.hibernatesynchronizer.mapping.HibernateClassCollectionProperty;
import com.hudson.hibernatesynchronizer.mapping.HibernateClassId;
import com.hudson.hibernatesynchronizer.mapping.HibernateClassProperty;
import com.hudson.hibernatesynchronizer.mapping.HibernateComponentClass;
import com.hudson.hibernatesynchronizer.mapping.HibernateDocument;
import com.hudson.hibernatesynchronizer.mapping.HibernateQuery;
import com.hudson.hibernatesynchronizer.mapping.IHibernateClassProperty;
import com.hudson.hibernatesynchronizer.resource.ResourceManager;
import com.hudson.hibernatesynchronizer.resource.Snippet;
import com.hudson.hibernatesynchronizer.resource.SnippetContext;

public abstract class AbstractDirective implements IDirective {

	protected int start;
	protected int length;
	protected int end = Integer.MIN_VALUE;
	protected IDocument document;
	private String content;

	/* (non-Javadoc)
	 * @see com.hudson.hibernatesynchronizer.editors.velocity.cursor.IDirective#load(int, int, org.eclipse.jface.text.Document)
	 */
	public void load(int start, int length, IDocument document) {
		this.start = start;
		this.length = length;
		this.document = document;
	}

	/* (non-Javadoc)
	 * @see com.hudson.hibernatesynchronizer.editors.velocity.cursor.IDirective#addVariableAdditions(java.lang.ClassLoader, java.util.Map)
	 */
	public void addVariableAdditions(IProject project, ClassLoader classLoader, Map variables) {
		if (canAddVariables()) {
			loadVariables(project, classLoader, variables);
		}
	}
	/* (non-Javadoc)
	 * @see com.hudson.hibernatesynchronizer.editors.velocity.cursor.IDirective#isStackScope()
	 */
	public boolean isStackScope() {
		return true;
	}
	/* (non-Javadoc)
	 * @see com.hudson.hibernatesynchronizer.editors.velocity.cursor.IDirective#requiresEnd()
	 */
	public boolean requiresEnd() {
		return true;
	}
	
	protected boolean canAddVariables () {
		return true;
	}
	
	protected void loadVariables (IProject project, ClassLoader classLoader, Map variables) {}
	
	protected String getContent () {
		if (null == content) {
			try {
				content = document.get(start, length);
			}
			catch (BadLocationException e) {
				Plugin.trace(e);
			}
		}
		return content;
	}
	
	protected String getInsideText () {
		int index = getContent().indexOf("(");
		if (index >= 0) {
			return getContent().substring(index + 1, getContent().length() - 1);
		}
		else {
			return null;
		}
	}
	
	public boolean isCursorInDirective (int pos) {
		if (end == Integer.MIN_VALUE) {
			int i = findEndIndex();
			if (i >= 0) end = start + i;
		}
		return (pos >= start && pos <= end);
	}
	
	public int findEndIndex () {
		return getContent().indexOf(")");
	}

	public List getCompletionProposals (IProject project, int pos, Map addedValues, ClassLoader loader) throws Exception {
		return null;
	}
	
	public static List getCompletionProposals (IProject project, IDocument document, int pos, Map addedValues, ClassLoader loader, boolean listValue) throws Exception {
		int i = pos-1;
		char c = document.getChar(i);
		boolean seenBrace = false;
		boolean seenExclamation = false;
		while (Character.isLetterOrDigit(c) || c == '_' || c == '.' || c == '$' || c == '{' || c == '!') {
			if (c == '{') seenBrace = true;
			if (c == '$') {
				break;
			}
			else {
				if (seenExclamation) seenExclamation = false;
				if (c == '!') seenExclamation = true;
			}
			i--;
			if (i>0) c = document.getChar(i);
			else break;
		}
		String text = document.get(i, pos-i);
		if (text.startsWith("$")) {
			int removeSize = 0;
			if (text.startsWith("${")) removeSize = 2;
			else if (text.startsWith("$!{")) removeSize = 3;
			else if (text.startsWith("$!")) removeSize = 2;
			else removeSize = 1;
			text = text.substring(removeSize, text.length());
			int index = text.lastIndexOf('.');
			if (index > 0) {
				String parentToken = text.substring(0, index);
				String prefix = text.substring(index+1, text.length());
				Class parentClass = getObjectClass(project, parentToken, loader, addedValues, listValue);
				if (null == parentClass && parentToken.equals("prop") || parentToken.equals("property")) {
					parentClass = IHibernateClassProperty.class;
				}
				if (null != parentClass) {
					return getCompletionProposals(project, document, parentClass, prefix, i+index+removeSize+1, seenBrace);
				}
			}
			else {
				return getCompletionProposals(project, document, addedValues, i+removeSize, text, seenBrace);
			}
		}
		return null;
	}

	public static Class getObjectClass (IProject project, String token, ClassLoader loader, Map additionalClasses, boolean listValue) {
		StringTokenizer st = new StringTokenizer(token, ".");
		Class parentClass = null;
		while (st.hasMoreTokens()) {
			if (null == parentClass) {
				// first time in
				parentClass = getClassMatch(project, st.nextToken(), additionalClasses, (listValue && !st.hasMoreTokens()));
				if (null == parentClass) break;
			}
			else {
				parentClass = getClassMatch (project, parentClass, st.nextToken(), additionalClasses, listValue && !st.hasMoreTokens());
				if (null == parentClass) break;
			}
		}
		return parentClass;
	}
	
	public static Class getClassMatch (IProject project, String token, Map additionalClasses, boolean listValue) {
		if (null == token) return null;
		if (listValue) {
			if (token.equals("classes")) return HibernateClass.class;
			else if (token.equals("files")) return IFile.class;
		}
		else {
			if (token.equals("classes")) return List.class;
			else if (token.equals("snippet")) return SnippetContext.class;
			else if (token.equals("class")) return HibernateClass.class;
			else if (token.equals("now")) return Date.class;
			else if (token.equals("documents")) return HibernateDocument.class;
			else if (token.equals("file")) return IFile.class;
			else if (token.equals("document")) return HibernateDocument.class;
			else {
				if (null != additionalClasses) return (Class) additionalClasses.get(token);
			}
		}
		return null;
	}

	public static Class getClassMatch (IProject project, Class parentClass, String token, Map additionalClasses, boolean listValue) {
		int index = token.indexOf("(");
		if (index > 0) {
			// we have parameters
			token = token.substring(0, token.indexOf("(")-1);
		}
		else {
			token = "get" + token;
		}
		if (null == parentClass) return null;
		if (listValue) {
			if (parentClass.getName().equals(HibernateClass.class.getName())) {
				if (token.equals("getAllProperties")
						|| token.equals("getAlternateKeys") 
						|| token.equals("getProperties")
						|| token.equals("getPropertiesWithComponents")
						|| token.equals("getRequiredFields")
						) {
					return IHibernateClassProperty.class;
				}
				else if (token.equals("getManyToOneList")
						|| token.equals("getOneToOneList")
						) {
					return HibernateClassProperty.class;
				}
				else if (token.equals("getCollectionList")) {
					return HibernateClassCollectionProperty.class;
				}
				else if (token.equals("getComponentList")) {
					return HibernateComponentClass.class;
				}
			}
			else if (parentClass.getName().equals(HibernateClassCollectionProperty.class.getName())) {
				if (token.equals("getCompositeList")) {
					return HibernateComponentClass.class;
				}
			}
			else if (parentClass.getName().equals(HibernateClassId.class.getName())) {
				if (token.equals("getProperties")) {
					return HibernateClassProperty.class;
				}
			}
			else if (parentClass.getName().equals(HibernateDocument.class.getName())) {
				if (token.equals("getClasses")) {
					return HibernateClass.class;
				}
			}
			else if (parentClass.getName().equals(HibernateDocument.class.getName())) {
				if (token.equals("getQueries")) {
					return HibernateQuery.class;
				}
			}
		}
		else {
			Method[] methods = parentClass.getMethods();
			for (int i=0; i<methods.length; i++) {
				if (methods[i].getName().equals(token)) {
					return methods[i].getReturnType();
				}
			}
		}
		return null;
	}

	public static List getCompletionProposals(IProject project, IDocument document, Class parentClass, String prefix, int startIndex, boolean seenBrace) {
		int endIndex = startIndex + prefix.length();
		boolean seenEndBrace = false;
		int dotIndex = -1;
		boolean inMethodParams = false;
		try {
			char c = document.getChar(endIndex);
			while (Character.isLetterOrDigit(c) || c == '_' || c == '}' || c == '.' || c == '(' || c == ')') {
				if (c == '.') {
					dotIndex = endIndex;
					break;
				}
				else if (c == '}') {
					if (!seenEndBrace) {
						seenEndBrace = true;
						break;
					}
				}
				else if (c == '(') {
					inMethodParams = true;
				}
				else if (c == ')') {
					if (inMethodParams) endIndex++;
					break;
				}
				c = document.getChar(++endIndex);
			}
		}
		catch (BadLocationException e) {}
		if (dotIndex >= 0) endIndex = dotIndex;
		List proposals = new ArrayList();
		if (null != parentClass) {
			String pUpper = prefix.toUpperCase();
			if (parentClass.getName().equals(SnippetContext.class.getName())) {
				List snippets = null;
				if (null != project)
					snippets = ResourceManager.getInstance(project).getSnippets();
				else
					snippets = ResourceManager.getWorkspaceSnippets();
				for (Iterator i=snippets.iterator(); i.hasNext(); ) {
					Snippet snippet = (Snippet) i.next();
					if (snippet.getName().toUpperCase().startsWith(pUpper)) {
						String actual = snippet.getName();
						if (seenBrace && !seenEndBrace && dotIndex < 0) actual = actual + "}";
						proposals.add(new CompletionProposal(
								actual,
								startIndex,
								endIndex-startIndex,
								actual.length(),
								null, snippet.getName(), null, null));
					}
				}
			}
			else {
				for (int i=0; i<parentClass.getMethods().length; i++) {
					Method m = parentClass.getMethods()[i];
					if (!m.getDeclaringClass().getName().equals(Object.class.getName())) {
						boolean added = false;
						if (m.getName().startsWith("get")  && m.getParameterTypes().length == 0) {
							String mName = m.getName().substring(3, m.getName().length());
							if (mName.toUpperCase().startsWith(pUpper)) {
								String actual = mName;
								if (seenBrace && !seenEndBrace && dotIndex < 0) actual = mName + "}";
								proposals.add(new CompletionProposal(
										actual,
										startIndex,
										endIndex-startIndex,
										mName.length(),
										null, mName, null, null));
								added = true;
							}
						}
						if (!added) {
							String mName = m.getName();
							if (mName.toUpperCase().startsWith(prefix.toUpperCase())) {
								StringBuffer display = new StringBuffer();
								display.append(mName);
								display.append("(");
								for (int j=0; j<m.getParameterTypes().length; j++) {
									if (j > 0) display.append(", ");
									display.append(m.getParameterTypes()[j].getName());
								}
								display.append(")");
								String actual = mName + "()";
								int tLength = actual.length();
								if (m.getParameterTypes().length > 0) tLength--;
								if (seenBrace && !seenEndBrace && dotIndex < 0) actual = actual + "}";
								proposals.add(new CompletionProposal(actual,
										startIndex, endIndex-startIndex, tLength,
										null, display.toString(), null, null));
							}
						}
					}
				}
			}
		}
		return proposals;
	}

	private static String[] staticProposals = {"now", "classes", "class", "documents", "document", "project", "snippet", "files", "file", "package", "fileName", "path"};
	public static List getCompletionProposals(IProject project, IDocument document, Map addedValues, int startIndex, String prefix, boolean seenBrace) {
		int endIndex = startIndex + prefix.length();
		boolean seenEndBrace = false;
		int dotIndex = -1;
		try {
			char c = document.getChar(endIndex);
			boolean inMethodParams = false;
			while (Character.isLetterOrDigit(c) || c == '_' || c == '}' || c == '.' || c == '(' || c == ')') {
				if (c == '.') {
					dotIndex = endIndex;
					break;
				}
				else if (c == '}') {
					if (!seenEndBrace) seenEndBrace = true;
					else break;
				}
				else if (c == '(') {
					inMethodParams = true;
				}
				else if (c == ')') {
					if (inMethodParams) endIndex++;
					break;
				}
				c = document.getChar(++endIndex);
			}
		}
		catch (BadLocationException e) {}
		if (dotIndex >= 0) endIndex = dotIndex;
		List proposals = new ArrayList();
		String pUpper = prefix.toUpperCase();
		for (int i=0; i<staticProposals.length; i++) {
			String propName = staticProposals[i];
			if (propName.toUpperCase().startsWith(pUpper)) {
				String actual = propName;
				if (seenBrace && !seenEndBrace && dotIndex < 0) actual = propName + "}";
				proposals.add(new CompletionProposal(
						actual,
						startIndex,
						endIndex-startIndex,
						propName.length(),
						null, propName, null, null));
			}
		}
		if (null != addedValues) {
			for (Iterator i=addedValues.entrySet().iterator(); i.hasNext(); ) {
				Map.Entry entry = (Map.Entry) i.next();
				String propName = (String) entry.getKey();
				if (propName.toUpperCase().startsWith(pUpper)) {
					String actual = propName;
					if (seenBrace && !seenEndBrace && dotIndex < 0) actual = propName + "}";
					proposals.add(new CompletionProposal(
							actual,
							startIndex,
							endIndex-startIndex,
							propName.length(),
							null, propName, null, null));
				}
			}
		}
		return proposals;
	}
}