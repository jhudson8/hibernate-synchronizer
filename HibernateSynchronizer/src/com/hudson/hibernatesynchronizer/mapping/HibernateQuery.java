package com.hudson.hibernatesynchronizer.mapping;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.hudson.hibernatesynchronizer.util.HSUtil;


/**
 * @author <a href="mailto: jhudson8@users.sourceforge.net">Joe Hudson</a>
 * 
 * This represents data related to the 'query' node of the hibernate
 * mapping configuration file.
 */
public class HibernateQuery extends BaseElement implements Comparable {

	private String name;
	private HibernateClass parent;

	public HibernateQuery (Node node, HibernateClass parent) {
		this.parent = parent;
		setParentRoot(parent);
		setNode(node);
		NamedNodeMap attributes = node.getAttributes();
		for (int i=0; i<attributes.getLength(); i++) {
			Node attNode = attributes.item(i);
			if (attNode.getNodeName().equals("name")) {
				name = attNode.getNodeValue();
			}
		}
		saveMetaData(node);
	}

	/**
	 * Return the name of this query
	 */
	public String getName () {
		return name;
	}

	/**
	 * Return the static variable name for this query
	 */
	public String getStaticName () {
		return HSUtil.getStaticName(name);
	}

	/**
	 * Return the parent HibernateClass
	 */
	public HibernateClass getParent() {
		return parent;
	}

	/**
	 * Compare this to another object
	 */
	public int compareTo(Object arg0) {
		if (arg0 instanceof HibernateQuery) {
			return getName().compareTo(((HibernateQuery) arg0).getName());
		}
		else {
			return -1;
		}
	}
}
