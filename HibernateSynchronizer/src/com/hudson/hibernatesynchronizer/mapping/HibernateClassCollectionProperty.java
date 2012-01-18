package com.hudson.hibernatesynchronizer.mapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.resources.IProject;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.hudson.hibernatesynchronizer.exceptions.AttributeNotSpecifiedException;
import com.hudson.hibernatesynchronizer.util.HSUtil;


/**
 * @author <a href="mailto: jhudson8@users.sourceforge.net">Joe Hudson</a>
 * 
 * This represents data related to the 'set', 'bag', 'list', 'map', and 'array'
 * nodes of the hibernate mapping configuration file.
 */
public class HibernateClassCollectionProperty extends HibernateClassProperty implements Comparable {

	public static final String TYPE_SET = "set";
	public static final String TYPE_BAG = "bag";
	public static final String TYPE_LIST = "list";
	public static final String TYPE_MAP = "map";
	public static final String TYPE_ARRAY = "array";
	public static final String TYPE_PRIMITIVE_ARRAY = "primitive-array";

	private String propType;
	private String implementation;
	private String absoluteChildClassName;
	private String childTableName;

	private List compositeList = new ArrayList();

	/**
	 * @param parent
	 * @param node
	 * @param isManyToOne
	 */
	public HibernateClassCollectionProperty (
		HibernateClass parent,
		Node node,
		String propType,
		String packageName,
		IProject currentProject) {
		super(parent, node, TYPE_PROPERTY, packageName, false, false);
		this.propType = propType;
		if (isSet()) {
			super.type = Set.class.getName();
			this.implementation = TreeSet.class.getName();
			setChildClass(node);
		}
		else if (isBag()) {
			super.type = Collection.class.getName();
			this.implementation = ArrayList.class.getName();
			setChildClass(node);
		}
		else if (isList()) {
			super.type = List.class.getName();
			this.implementation = ArrayList.class.getName();
			setChildClass(node);
		}
		else if (isMap()) {
			super.type = Map.class.getName();
			this.implementation = HashMap.class.getName();
			setChildClass(node);
		}
		else if (isArray()) {
			Node child = node.getFirstChild();
			while (null != child) {
				if (child.getNodeName().equals("many-to-many") || child.getNodeName().equals("one-to-many") || child.getNodeName().equals("many-to-any")) {
					NamedNodeMap attributes = child.getAttributes();
					if (null != attributes) {
						Node attNode = attributes.getNamedItem("class");
						if (null != attNode) {
							String className = attNode.getNodeValue();
							if (null != packageName && className.indexOf(".") < 0) {
								className = packageName + "." + className;
							}
							super.type = className + "[]";
							this.implementation = className;
						}
					}
				}
				else if (child.getNodeName().equals("element")) {
					NamedNodeMap attributes = child.getAttributes();
					if (null != attributes) {
						Node attNode = attributes.getNamedItem("type");
						if (null != attNode) {
							String className = attNode.getNodeValue();
							if (!propType.equals(TYPE_PRIMITIVE_ARRAY)) {
								String s = (String) HibernateClassProperty.typeMap.get(className);
								if (null != s) className = s;
								if (null != packageName && className.indexOf(".") < 0) {
									className = packageName + "." + className;
								}
							}
							super.type = className + "[]";
							this.implementation = className;
						}
					}
				}
				child = child.getNextSibling();
			}
			setChildClass(node);
		}
		Node child = node.getFirstChild();
		while (null != child) {
			if (child.getNodeName().equals("composite-element") || child.getNodeName().equals("nested-composite-element")) {
				compositeList.add(new HibernateComponentClass(child, packageName, parent, false, currentProject));
			}
			child = child.getNextSibling();
		}
		if (null == getName() || getName().length() == 0) {
			throw new AttributeNotSpecifiedException(node, "name");
		}
		String signatureClass = get("SignatureClass");
		if (null != signatureClass) {
			super.type = signatureClass;
			clear("SignatureClass");
		}
		String implementationClass = get("ImplementationClass");
		if (null != implementationClass) {
			this.implementation = implementationClass;
			clear("ImplementationClass");
		}
	}

	private void setChildClass (Node node) {
		Node classAttribute = node.getAttributes().getNamedItem("class");
		if (null != classAttribute) {
			String className = classAttribute.getNodeValue();
			if (null != parent.getDocument().getPackageName() && className.indexOf(".") < 0) {
				className = parent.getDocument().getPackageName() + "." + className;
			}
			this.absoluteChildClassName = className;
		}
		else {
			Node tableAttribute = node.getAttributes().getNamedItem("table");
			if (null != tableAttribute) {
				childTableName = tableAttribute.getNodeValue();
			}
			else {
				Node child = node.getFirstChild();
				while (null != child) {
					if (child.getNodeName().equals("many-to-many") || child.getNodeName().equals("one-to-many") || child.getNodeName().equals("many-to-any")) {
						NamedNodeMap attributes = child.getAttributes();
						if (null != attributes) {
							Node attNode = attributes.getNamedItem("class");
							if (null != attNode) {
								String className = attNode.getNodeValue();
								if (null != parent.getDocument().getPackageName() && className.indexOf(".") < 0) {
									className = parent.getDocument().getPackageName() + "." + className;
								}
								this.absoluteChildClassName = className;
							}
						}
					}
					child = child.getNextSibling();
				}
			}
		}
	}

	/**
	 * @return Returns the list of composite-element class associate with this collection
	 * @return a List of HibernateComponentClass objects
	 */
	public List getCompositeList() {
		return compositeList;
	}

	/**
	 * Return true if this collection represents a Set and false otherwise
	 */
	public boolean isSet() {
		return TYPE_SET.equals(propType);
	}

	/**
	 * Return true if this collection represents a Bag and false otherwise
	 */
	public boolean isBag() {
		return TYPE_BAG.equals(propType);
	}

	/**
	 * Return true if this collection represents a List and false otherwise
	 */
	public boolean isList() {
		return TYPE_LIST.equals(propType);
	}

	/**
	 * Return true if this collection represents a Map and false otherwise
	 */
	public boolean isMap() {
		return TYPE_MAP.equals(propType);
	}

	/**
	 * Return true if this collection represents an array and false otherwise
	 */
	public boolean isArray() {
		return TYPE_ARRAY.equals(propType) || TYPE_PRIMITIVE_ARRAY.equals(propType);
	}

	/**
	 * Return the static variable definition name based on this property
	 */
	public String getStaticName () {
		return HSUtil.getStaticName(name);
	}

	/**
	 * Return the plural representatio of the property name
	 * @return
	 */
	public HibernateClass getChildClass () {
		if (null != absoluteChildClassName)
			return HibernateMappingManager.getInstance(parent.getDocument().getFile().getProject()).getHibernateClass(absoluteChildClassName);
		else if (null != childTableName)
			return HibernateMappingManager.getInstance(parent.getDocument().getFile().getProject()).getHibernateClassByTableName(childTableName);
		else return null;
	}

	/**
	 * Return the fully qualified implementation class based on the type of
	 * collection this represents.
	 */
	public String getAbsoluteImplementationClassName() {
		return implementation;
	}

	/**
	 * Return the reserved properties associated with this element
	 */
	private static final String[] IP = new String[] {"name", "table", "schema", "lazy", "inverse", "cascade", "sort", "order-by", "where", "outer-join", "batch-size", "access", "inverse"};
	protected String[] getReservedProperties() {
		return IP;
	}

	/**
	 * Compare this to another object
	 */
	public int compareTo(Object arg0) {
		if (arg0 instanceof HibernateClassCollectionProperty) {
			return getPropName().compareTo(((HibernateClassCollectionProperty) arg0).getPropName());
		}
		else {
			return -1;
		}
	}
}