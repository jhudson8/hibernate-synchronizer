package com.hudson.hibernatesynchronizer.editors.synchronizer.completion;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import com.hudson.hibernatesynchronizer.mapping.HibernateClass;
import com.hudson.hibernatesynchronizer.mapping.HibernateDocument;


/**
 * @author Joe Hudson
 */
public class QuerySuggestor {

	Node queryNode;
	private int state = STATE_PRE_INITIAL;
	
	private static final int STATE_PRE_INITIAL = 0;
	private static final int STATE_INITIAL = 1;
	private static final int STATE_AFTER_SELECT_TOKEN = 2;
	private static final int STATE_AFTER_FROM_TOKEN = 3;
	private static final int STATE_AFTER_WHERE_TOKEN = 4;
	private static final int STATE_IN_SELECT_FIELD = 5;
	private static final int STATE_IN_FROM_TABLE = 6;
	
	
	public QuerySuggestor (Node queryNode) {
		this.queryNode = queryNode;
	}

	public List getSuggestions (int offset, IDocument doc, HibernateDocument hd) {
		List suggestions = new ArrayList();
		int index = getNextWordStart(offset, doc);
		if (index == -1) return null;
		String text = getWord(index, doc);
		if (null == text) {
			suggestions.add("select");
			suggestions.add("from");
			return suggestions;
		}
		if (text.equalsIgnoreCase("select")) {
			index = getNextWordStart(index, doc);
			if (offset >= index) {
				// beginning of select fields statement
				if (hd.getClasses().size() == 1) {
					suggestions.add(((HibernateClass) hd.getClasses().get(0)).getVarName());
					return suggestions;
				}
				else return null;
			}
		}
		index = getNextWordStart(index, doc);
		String nextWord = getWord(index, doc);
		while (index > 0 && nextWord != null && index < offset) {
			if (nextWord.equalsIgnoreCase("from")) break;
		}
		if (index >= offset) return null;
		if (null != nextWord) {
			index += 5;
		}
		return null;
	}

	private int getNextWordStart(int offset, IDocument doc) {
		try {
			char c = doc.getChar(offset);
			while (Character.isWhitespace(c)) c = doc.getChar(++offset);
			return offset;
		}
		catch (BadLocationException e) {
			return -1;
		}
	}

	private String getWord (int offset, IDocument doc) {
		try {
			StringBuffer sb = new StringBuffer();
			char c = doc.getChar(offset);
			while (!Character.isWhitespace(c)) {
				sb.append(c);
				c = doc.getChar(++offset);
			}
			return sb.toString();
		}
		catch (BadLocationException e) {
			return null;
		}
	}
}
