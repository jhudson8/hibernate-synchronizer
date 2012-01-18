package com.hudson.hibernatesynchronizer.editors.synchronizer.completion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITypedRegion;

public class Node {

	private IDocument doc;
	private Node parentNode;
	private int offsetStart;
	private int offsetEnd;
	
	private String name;
	private int nameStart = -1;
	private List attributes;
	private Map attributeMap;
	private int state = -1;
	private int type = CursorState.TYPE_HEADER;
	
	public Node (Node parentNode, ITypedRegion region, IDocument doc) {
		this.parentNode = parentNode;
		this.doc = doc;
		this.offsetStart = region.getOffset() + 1;
		this.offsetEnd = region.getOffset() + region.getLength() - 1;
	}

	public Node (Node parentNode, int offsetStart, int offsetEnd, IDocument doc) {
		this.parentNode = parentNode;
		this.doc = doc;
		this.offsetStart = offsetStart;
		this.offsetEnd = offsetEnd;
	}
	
	public Attribute getAttribute(int offset) {
		if (null == attributes) return null;
		for (Iterator i=getAttributes().iterator(); i.hasNext(); ) {
			Attribute a = (Attribute) i.next();
			int end = a.getValueOffset();
			if (null != a.getValue()) end += a.getValue().length();
			if (offset > a.getNameOffset() && offset < (a.getValueOffset() + a.getValue().length() + 1)) return a;
		}
		return null;
	}
	
	public boolean inNodeName (int offset) {
		return (offset >= nameStart && offset <= (nameStart + name.length()));
	}

	public String getName() {
		if (null == name) loadName();
		return name;
	}
	public int getNameStart() {
		if (nameStart == -1) loadName();
		return nameStart;
	}
	public int getOffsetEnd() {
		return offsetEnd;
	}
	public int getOffsetStart() {
		return offsetStart;
	}
	public List getAttributes() {
		if (null == attributes && type != CursorState.TYPE_FOOTER) loadAttributes();
		return attributes;
	}
	public Attribute getAttribute(String attributeName) {
		if (null == attributeMap && type != CursorState.TYPE_FOOTER) loadAttributes();
		if (null == attributeMap) return null;
		return (Attribute) attributeMap.get(attributeName);
	}
	public Node getParent () {
		return parentNode;
	}

	public int getState (int offset) {
		if (state == -1) {
			getName();
			getAttributes();
		}
		if (offset <= nameStart) {
			return CursorState.STATE_WAITING_FOR_NODE_NAME;
		}
		else if (offset <= nameStart + name.length()) {
			return CursorState.STATE_NODE_NAME;
		}
		else {
			Attribute attribute = getAttribute(offset);
			if (null != attribute) {
				return attribute.getState(offset);
			}
			else if (null != getAttribute(offset+1)) return CursorState.STATE_WAITING_FOR_ATTRIBUTE_NAME;
			else if (null != getAttribute(offset-1)) return CursorState.STATE_END_ATTRIBUTE_VALUE;
			else return CursorState.STATE_WAITING_FOR_ATTRIBUTE_NAME;
		}
	}

	public int getType () {
		if (type == -1) {
			getName();
			getAttributes();
		}
		return type;
	}

	private void loadName() {
		StringBuffer sb = new StringBuffer();
		boolean started = false;
		boolean encounteredSpace = false;
		try {
			for (int i=offsetStart; i<doc.getLength(); i++) {
				char c = doc.getChar(i);
				if (Character.isWhitespace(c)) {
					if (started) {
						encounteredSpace = true;
						break;
					}
				}
				else {
					if (c == CursorState.CHAR_GT_SIGN) break;
					else if (c == CursorState.CHAR_SLASH) {
						if (sb.length() == 0) {
							type = CursorState.TYPE_FOOTER;
						}
						else break;
					}
					else {
						if (!started) {
							if (c == '<') {
								int startCheck = i;
								int firstWhiteSpace = -1;
								try {
									char cTemp = doc.getChar(++startCheck);
									while (Character.isWhitespace(cTemp) || cTemp == '/') {
										if (firstWhiteSpace == -1 && Character.isWhitespace(cTemp)) firstWhiteSpace = startCheck;
										cTemp = doc.getChar(++startCheck);
									}
									if (doc.getChar(startCheck) == '<' && firstWhiteSpace != -1) nameStart = firstWhiteSpace;
									else nameStart = startCheck;
								}
								catch (BadLocationException e) {}
							}
							else nameStart = i;
							started = true;
						}
						if (c != '<') sb.append(c);
					}
				}
			}
		}
		catch (BadLocationException e) {}
		this.name = sb.toString();
		if (sb.length() == 0) state = CursorState.STATE_WAITING_FOR_NODE_NAME;
		else if (encounteredSpace) state = CursorState.STATE_WAITING_FOR_ATTRIBUTE_NAME;
		else state = CursorState.STATE_NODE_NAME;
	}

	private void loadAttributes() {
		// make sure the name is loaded first
		getName();

		if (type != CursorState.TYPE_FOOTER) {
			attributes = new ArrayList();
			attributeMap = new HashMap();
			int startOffset = getNameStart()+getName().length();
			int endOffset = offsetEnd;
			if (state == -1) state = CursorState.STATE_WAITING_FOR_ATTRIBUTE_NAME;
	
			StringBuffer attributeName = new StringBuffer();
			StringBuffer attributeValue = new StringBuffer();
			Attribute currentAttribute = new Attribute();
			
			try {
				for (int i=startOffset; i<endOffset; i++) {
					char c = doc.getChar(i);
					if (Character.isWhitespace(c)) {
						switch (state) {
							case CursorState.STATE_ATTRIBUTE_NAME:
								state = CursorState.STATE_WAITING_FOR_EQUAL;
								currentAttribute.setName(attributeName.toString());
								break;
							case CursorState.STATE_ATTRIBUTE_VALUE:
								if (c == '\n') i = endOffset + 1;
								else attributeValue.append(c);
								break;
							case CursorState.STATE_END_ATTRIBUTE_VALUE:
								state = CursorState.STATE_WAITING_FOR_ATTRIBUTE_NAME;
								break;
						}
					}
					else if (c == CursorState.CHAR_QUOTE) {
						switch (state) {
							case CursorState.STATE_WAITING_FOR_ATTRIBUTE_VALUE_QUOTE:
								state = CursorState.STATE_ATTRIBUTE_VALUE;
								currentAttribute.setValueOffset(i+1);
								break;
							case CursorState.STATE_ATTRIBUTE_VALUE:
								state = CursorState.STATE_END_ATTRIBUTE_VALUE;
								currentAttribute.setValue(attributeValue.toString());
								attributes.add(currentAttribute);
								attributeMap.put(currentAttribute.getName(), currentAttribute);
								currentAttribute = new Attribute();
								attributeName = new StringBuffer();
								attributeValue = new StringBuffer();
								break;
						}
					}
					else if (c == CursorState.CHAR_EQUAL) {
						switch (state) {
							case CursorState.STATE_ATTRIBUTE_NAME:
								currentAttribute.setName(attributeName.toString());
							case CursorState.STATE_WAITING_FOR_EQUAL:
								state = CursorState.STATE_WAITING_FOR_ATTRIBUTE_VALUE_QUOTE;
								currentAttribute.setEqualOffset(i);
								break;
							case CursorState.STATE_ATTRIBUTE_VALUE:
								attributeValue.append(c);
								break;
								
						}
					}
					else if (c == CursorState.CHAR_SLASH) {
						switch (state) {
							case CursorState.STATE_WAITING_FOR_ATTRIBUTE_NAME:
								i = endOffset + 1;
								state = CursorState.STATE_WAITING_FOR_NODE_END;
								type = CursorState.TYPE_HEADER_FLAT;
								break;
						}
					}
					else {
						switch (state) {
							case CursorState.STATE_ATTRIBUTE_NAME:
								attributeName.append(c);
								break;
							case CursorState.STATE_ATTRIBUTE_VALUE:
								attributeValue.append(c);
								break;
							case CursorState.STATE_WAITING_FOR_ATTRIBUTE_NAME:
								state = CursorState.STATE_ATTRIBUTE_NAME;
								currentAttribute.setNameOffset(i);
								attributeName.append(c);
								break;
						}
					}
				}
			}
			catch (BadLocationException e) {}
			if (state == CursorState.STATE_ATTRIBUTE_VALUE) {
				currentAttribute.setValue(attributeValue.toString().trim());
				attributes.add(currentAttribute);
				attributeMap.put(currentAttribute.getName(), currentAttribute);
				currentAttribute = null;
				attributeName = null;
				attributeValue = null;
			}
			else if (state == CursorState.STATE_ATTRIBUTE_NAME) {
				currentAttribute.setName(attributeName.toString().trim());
				currentAttribute.setValue("");
				attributes.add(currentAttribute);
				attributeMap.put(currentAttribute.getName(), currentAttribute);
				currentAttribute = null;
				attributeName = null;
				attributeValue = null;
			}
		}
	}
	
	public String toString () {
		StringBuffer sb = new StringBuffer();
		sb.append(getName() + "\n");
		for (int i=0; i<getAttributes().size(); i++) {
			Attribute a = (Attribute) getAttributes().get(i);
			sb.append("\t" + a.getName() + " = " + a.getValue() + "\n");
		}
		return sb.toString();
	}
}