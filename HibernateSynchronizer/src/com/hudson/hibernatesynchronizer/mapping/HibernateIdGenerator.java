package com.hudson.hibernatesynchronizer.mapping;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.hudson.hibernatesynchronizer.exceptions.AttributeNotSpecifiedException;


public class HibernateIdGenerator extends BaseElement {

	private HibernateClassId parent;
	private String generatorClass;
	
	public HibernateIdGenerator (HibernateClassId parent, Node node) {
		this.parent = parent;
		setNode(node);
		NamedNodeMap attributes = node.getAttributes();
		for (int i=0; i<attributes.getLength(); i++) {
			Node attNode = attributes.item(i);
			if (attNode.getNodeName().equals("class")) {
				this.generatorClass = attNode.getNodeValue();
			}
		}
		saveMetaData(node);
		if (null == generatorClass) {
			throw new AttributeNotSpecifiedException(node, "class");
		}
	}
	
	/**
	 * @return Returns the generatorClass.
	 */
	public String getGeneratorClass() {
		return generatorClass;
	}

	/**
	 * @return Returns the parent.
	 */
	public HibernateClassId getParent() {
		return parent;
	}

	/**
	 * Return the reserved properties associated with this element
	 */
	protected String[] getReservedProperties() {
		return IP;
	}
	private static final String[] IP = new String[] {"class"};
}