package com.hudson.hibernatesynchronizer.editors.synchronizer.completion;

import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.eclipse.core.resources.IProject;

import com.hudson.hibernatesynchronizer.mapping.HibernateClass;
import com.hudson.hibernatesynchronizer.mapping.HibernateMappingManager;

/**
 * @author Joe Hudson
 */
public class Suggestor {

	public static Map nodeSuggestions;
	public static Map attributeSuggestions;
	public static Map valueSuggestions;
	public static Map classSuggestions;

	static {
		setupSuggestions();
	}
	
	public static void setupSuggestions () {
		String[] TRUE_FALSE = {"true", "false"};
		String[] TRUE_FALSE_AUTO = {"true", "false", "auto"};
		String[] ALL_KNOWN_TYPES = {"binary", "int", "integer", "long", "short", "float", "double", "char", "character", "byte", "boolean", "yes_no", "true_false", "string", "date", "time", "timestamp", "calendar", "calendar_date", "big_decimal", "locale", "timezone", "currency", "class", "binary", "text", "serializable", "clob", "blob", "object",
				String.class.getName(), Integer.class.getName(), "java.util.Currency", BigDecimal.class.getName(), Character.class.getName(), Calendar.class.getName(), Date.class.getName(), Timestamp.class.getName(), Time.class.getName(), Locale.class.getName(), TimeZone.class.getName(), Class.class.getName(), Object.class.getName(), 
				Blob.class.getName(), Clob.class.getName(), Float.class.getName(), Long.class.getName(), Double.class.getName(), Boolean.class.getName(), Byte.class.getName()};
		String[] PRIMITIVE_TYPES = {"int", "float", "long", "double", "char", "boolean", "byte"};
		String[] KEY_TYPES = ALL_KNOWN_TYPES;
		String[] VERSION_TYPES = {"int", "long", "timestamp", "date", "calendar"};
		String[] ACCESS_TYPES = {"field", "property", "ClassName"};
		String[] UNSAVED_VALUE_TYPES = {"any", "none"};
		String[] CASCADE_TYPES = {"all", "none", "save-update", "delete"};
		String[] CASCADE_TYPES_ORPHAN = {"all", "none", "save-update", "delete", "delete-orphan"};
		String[] SORT_TYPES = {"unsorted", "natural", "comparatorClass"};
		
		nodeSuggestions = new HashMap();
		nodeSuggestions.put("hibernate-mapping", new String[] {"class", "query", "import", "subclass"});
		nodeSuggestions.put("class", new String[] {"cache", "meta", "composite-id", "id", "version", "timestamp", "discriminator", "property", "many-to-one", "one-to-one", "set", "map", "bag", "array", "primitive-array", "list", "subclass", "joined-subclass", "component", "dynamic-component", "any", "element", "many-to-many", "idbag"});
		nodeSuggestions.put("id", new String[] {"meta", "generator"});
		nodeSuggestions.put("generator", new String[] {"param"});
		nodeSuggestions.put("composite-id", new String[] {"meta", "key-property", "key-many-to-one"});
		nodeSuggestions.put("component", new String[] {"meta", "property", "many-to-one", "parent"});
		nodeSuggestions.put("dynamic-component", new String[] {"meta", "property", "many-to-one", "parent"});
		nodeSuggestions.put("subclass", new String[] {"meta", "version", "timestamp", "property", "many-to-one", "one-to-one", "set", "map", "bag", "array", "list", "component", "dynamic-component", "subclass"});
		nodeSuggestions.put("joined-subclass", new String[] {"meta", "version", "timestamp", "property", "many-to-one", "one-to-one", "set", "map", "bag", "array", "list", "component", "dynamic-component", "key"});
		nodeSuggestions.put("any", new String[] {"meta", "meta-value", "column"});
		nodeSuggestions.put("property", new String[] {"meta", "column"});
		nodeSuggestions.put("map", new String[] {"cache", "meta", "key", "index", "element", "composite-element", "index-many-to-many", "one-to-many", "many-to-many"});
		nodeSuggestions.put("set", new String[] {"cache", "meta", "key", "element", "composite-element", "one-to-many", "many-to-many", "many-to-any"});
		nodeSuggestions.put("bag", new String[] {"cache", "meta", "key", "element", "composite-element", "one-to-many", "many-to-many", "many-to-any"});
		nodeSuggestions.put("array", new String[] {"meta", "key", "index", "element", "composite-element", "many-to-many", "one-to-many"});
		nodeSuggestions.put("primitive-array", new String[] {"meta", "key", "index", "element"});
		nodeSuggestions.put("list", new String[] {"cache", "meta", "key", "index", "element", "composite-element", "index-many-to-many", "one-to-many", "many-to-many"});
		nodeSuggestions.put("composite-element", new String[] {"meta", "property"});
		nodeSuggestions.put("idbag", new String[] {"meta", "collection-id", "key", "many-to-many", "one-to-many"});
		nodeSuggestions.put("collection-id", new String[] {"meta", "generator"});
		
		
		attributeSuggestions = new HashMap();
		attributeSuggestions.put("cache", new String[] {"usage"});
		attributeSuggestions.put("import", new String[] {"class", "rename"});
		attributeSuggestions.put("meta", new String[] {"attribute"});
		attributeSuggestions.put("query", new String[] {"name"});
		attributeSuggestions.put("class", new String[] {"name", "table", "discriminator-value", "mutable", "schema", "proxy", "dynamic-update", "dynamic-insert", "select-before-update", "polymorphism", "where", "persister", "batch-size", "optimistic-lock", "lazy"});
		attributeSuggestions.put("id", new String[] {"name", "type", "column", "unsaved-value", "access"});
		attributeSuggestions.put("key-property", new String[] {"name", "type", "column"});
		attributeSuggestions.put("key", new String[] {"name", "type", "column"});
		attributeSuggestions.put("composite-id", new String[] {"name", "class", "unsaved-value", "access"});
		attributeSuggestions.put("key-many-to-one", new String[] {"name", "type", "column", "class"});
		attributeSuggestions.put("discriminator", new String[] {"column", "type", "force"});
		attributeSuggestions.put("version", new String[] {"name", "column", "type", "access", "unsaved-value"});
		attributeSuggestions.put("timestamp", new String[] {"column", "name", "access", "unsaved-value"});
		attributeSuggestions.put("property", new String[] {"column", "name", "type", "update", "insert", "formula", "access", "length", "not-null", "unique"});
		attributeSuggestions.put("many-to-one", new String[] {"column", "name", "class", "cascade", "outer-join", "update", "insert", "property-ref", "access", "unique"});
		attributeSuggestions.put("one-to-one", new String[] {"column", "name", "class", "cascade", "outer-join", "property-ref", "access"});
		attributeSuggestions.put("component", new String[] {"name", "class", "insert", "update", "access"});
		attributeSuggestions.put("dynamic-component", new String[] {"name", "class", "insert", "update", "access"});
		attributeSuggestions.put("subclass", new String[] {"name", "discriminator-value", "proxy", "lazy", "dynamic-update", "dynamic-insert"});
		attributeSuggestions.put("joined-subclass", new String[] {"name", "table", "proxy", "lazy", "dynamic-update", "dynamic-insert", "extends"});
		attributeSuggestions.put("any", new String[] {"name", "id-type", "meta-type", "cascade", "access"});
		attributeSuggestions.put("meta-value", new String[] {"value", "class"});
		attributeSuggestions.put("column", new String[] {"name", "sql-type", "length", "not-null", "unique"});
		attributeSuggestions.put("map", new String[] {"name", "table", "schema", "lazy", "inverse", "cascade", "sort", "order-by", "where", "outer-join", "batch-size", "access"});
		attributeSuggestions.put("index", new String[] {"column", "type"});
		attributeSuggestions.put("element", new String[] {"column", "type"});
		attributeSuggestions.put("index-many-to-many", new String[] {"column", "class"});
		attributeSuggestions.put("many-to-many", new String[] {"column", "class", "outer-join"});
		attributeSuggestions.put("set", new String[] {"name", "table", "sort", "order-by", "inverse", "lazy", "cascade"});
		attributeSuggestions.put("bag", new String[] {"name", "table", "sort", "order-by", "inverse", "lazy", "cascade"});
		attributeSuggestions.put("array", new String[] {"name", "table", "sort", "order-by", "cascade", "inverse"});
		attributeSuggestions.put("primitive-array", new String[] {"name", "table", "sort", "order-by", "cascade", "inverse"});
		attributeSuggestions.put("list", new String[] {"name", "table", "sort", "order-by", "inverse", "lazy", "cascade"});
		attributeSuggestions.put("composite-element", new String[] {"class"});
		attributeSuggestions.put("idbag", new String[] {"name", "table", "lazy"});
		attributeSuggestions.put("collection-id", new String[] {"column", "type"});
		attributeSuggestions.put("parent", new String[] {"name"});
		attributeSuggestions.put("generator", new String[] {"class"});
		
		classSuggestions = new HashMap();
		classSuggestions.put(new NodeAttribute("key-many-to-one", "class"), Boolean.TRUE);
		classSuggestions.put(new NodeAttribute("many-to-one", "class"), Boolean.TRUE);
		classSuggestions.put(new NodeAttribute("one-to-one", "class"), Boolean.TRUE);
		
		
		valueSuggestions = new HashMap();
		valueSuggestions.put(new NodeAttribute("cache", "usage"), new String[] {"transactional", "read-write", "nonstrict-read-write", "read-only"});
		valueSuggestions.put(new NodeAttribute("class", "dynamic-update"), TRUE_FALSE);
		valueSuggestions.put(new NodeAttribute("class", "dynamic-insert"), TRUE_FALSE);
		valueSuggestions.put(new NodeAttribute("class", "select-before-update"), TRUE_FALSE);
		valueSuggestions.put(new NodeAttribute("class", "polymorphism"), new String[] {"implicit", "explicit"});
		valueSuggestions.put(new NodeAttribute("class", "optimistic-lock"), new String[] {"none", "version", "dirty", "all"});
		valueSuggestions.put(new NodeAttribute("class", "lazy"), TRUE_FALSE);
		valueSuggestions.put(new NodeAttribute("id", "unsaved-value"), UNSAVED_VALUE_TYPES);
		valueSuggestions.put(new NodeAttribute("id", "access"), ACCESS_TYPES);
		valueSuggestions.put(new NodeAttribute("id", "type"), ALL_KNOWN_TYPES);
		valueSuggestions.put(new NodeAttribute("generator", "class"), new String[] {"increment", "identity", "sequence", "hilo", "seqhilo", "uuid.hex", "uuid.string", "native", "assigned", "foreign", "vm"});
		valueSuggestions.put(new NodeAttribute("composite-id", "access"), ACCESS_TYPES);
		valueSuggestions.put(new NodeAttribute("composite-id", "unsaved-value"), UNSAVED_VALUE_TYPES);
		valueSuggestions.put(new NodeAttribute("key-property", "type"), KEY_TYPES);
		valueSuggestions.put(new NodeAttribute("discriminator", "type"), ALL_KNOWN_TYPES);
		valueSuggestions.put(new NodeAttribute("discriminator", "force"), TRUE_FALSE);
		valueSuggestions.put(new NodeAttribute("version", "type"), VERSION_TYPES);
		valueSuggestions.put(new NodeAttribute("version", "access"), ACCESS_TYPES);
		valueSuggestions.put(new NodeAttribute("version", "unsaved-value"), new String[] {"null", "negative", "undefined"});
		valueSuggestions.put(new NodeAttribute("timestamp", "access"), ACCESS_TYPES);
		valueSuggestions.put(new NodeAttribute("version", "unsaved-value"), new String[] {"null", "undefined"});
		valueSuggestions.put(new NodeAttribute("property", "type"), ALL_KNOWN_TYPES);
		valueSuggestions.put(new NodeAttribute("property", "update"), TRUE_FALSE);
		valueSuggestions.put(new NodeAttribute("property", "insert"), TRUE_FALSE);
		valueSuggestions.put(new NodeAttribute("property", "access"), ACCESS_TYPES);
		valueSuggestions.put(new NodeAttribute("property", "not-null"), TRUE_FALSE);
		valueSuggestions.put(new NodeAttribute("property", "unique"), TRUE_FALSE);
		valueSuggestions.put(new NodeAttribute("many-to-one", "outer-join"), TRUE_FALSE_AUTO);
		valueSuggestions.put(new NodeAttribute("many-to-one", "update"), TRUE_FALSE);
		valueSuggestions.put(new NodeAttribute("many-to-one", "insert"), TRUE_FALSE);
		valueSuggestions.put(new NodeAttribute("many-to-one", "access"), ACCESS_TYPES);
		valueSuggestions.put(new NodeAttribute("many-to-one", "unique"), TRUE_FALSE);
		valueSuggestions.put(new NodeAttribute("many-to-one", "not-null"), TRUE_FALSE);
		valueSuggestions.put(new NodeAttribute("many-to-one", "cascade"), CASCADE_TYPES);
		valueSuggestions.put(new NodeAttribute("one-to-one", "cascade"), CASCADE_TYPES);
		valueSuggestions.put(new NodeAttribute("one-to-one", "constrained"), TRUE_FALSE);
		valueSuggestions.put(new NodeAttribute("one-to-one", "outer-join"), TRUE_FALSE_AUTO);
		valueSuggestions.put(new NodeAttribute("one-to-one", "access"), ACCESS_TYPES);
		valueSuggestions.put(new NodeAttribute("one-to-one", "not-null"), TRUE_FALSE);
		valueSuggestions.put(new NodeAttribute("component", "insert"), TRUE_FALSE);
		valueSuggestions.put(new NodeAttribute("component", "update"), TRUE_FALSE);
		valueSuggestions.put(new NodeAttribute("component", "access"), ACCESS_TYPES);
		valueSuggestions.put(new NodeAttribute("dynamic-component", "insert"), TRUE_FALSE);
		valueSuggestions.put(new NodeAttribute("dynamic-component", "update"), TRUE_FALSE);
		valueSuggestions.put(new NodeAttribute("dynamic-component", "access"), ACCESS_TYPES);
		valueSuggestions.put(new NodeAttribute("subclass", "dynamic-update"), TRUE_FALSE);
		valueSuggestions.put(new NodeAttribute("subclass", "dynamic-insert"), TRUE_FALSE);
		valueSuggestions.put(new NodeAttribute("subclass", "lazy"), TRUE_FALSE);
		valueSuggestions.put(new NodeAttribute("joined-subclass", "dynamic-update"), TRUE_FALSE);
		valueSuggestions.put(new NodeAttribute("joined-subclass", "dynamic-insert"), TRUE_FALSE);
		valueSuggestions.put(new NodeAttribute("joined-subclass", "lazy"), TRUE_FALSE);
		valueSuggestions.put(new NodeAttribute("any", "cascade"), CASCADE_TYPES);
		valueSuggestions.put(new NodeAttribute("any", "access"), ACCESS_TYPES);
		valueSuggestions.put(new NodeAttribute("column", "not-null"), TRUE_FALSE);
		valueSuggestions.put(new NodeAttribute("column", "unique"), TRUE_FALSE);
		valueSuggestions.put(new NodeAttribute("map", "lazy"), TRUE_FALSE);
		valueSuggestions.put(new NodeAttribute("map", "inverse"), TRUE_FALSE);
		valueSuggestions.put(new NodeAttribute("map", "cascade"), CASCADE_TYPES_ORPHAN);
		valueSuggestions.put(new NodeAttribute("map", "sort"), SORT_TYPES);
		valueSuggestions.put(new NodeAttribute("map", "outer-join"), TRUE_FALSE_AUTO);
		valueSuggestions.put(new NodeAttribute("map", "access"), ACCESS_TYPES);
		valueSuggestions.put(new NodeAttribute("set", "lazy"), TRUE_FALSE);
		valueSuggestions.put(new NodeAttribute("set", "inverse"), TRUE_FALSE);
		valueSuggestions.put(new NodeAttribute("set", "cascade"), CASCADE_TYPES_ORPHAN);
		valueSuggestions.put(new NodeAttribute("bag", "lazy"), TRUE_FALSE);
		valueSuggestions.put(new NodeAttribute("bag", "inverse"), TRUE_FALSE);
		valueSuggestions.put(new NodeAttribute("bag", "cascade"), CASCADE_TYPES_ORPHAN);
		valueSuggestions.put(new NodeAttribute("list", "lazy"), TRUE_FALSE);
		valueSuggestions.put(new NodeAttribute("list", "inverse"), TRUE_FALSE);
		valueSuggestions.put(new NodeAttribute("list", "cascade"), CASCADE_TYPES_ORPHAN);
		valueSuggestions.put(new NodeAttribute("index", "type"), KEY_TYPES);
		valueSuggestions.put(new NodeAttribute("element", "type"), ALL_KNOWN_TYPES);
		valueSuggestions.put(new NodeAttribute("many-to-many", "outer-join"), TRUE_FALSE_AUTO);
		valueSuggestions.put(new NodeAttribute("array", "cascade"), CASCADE_TYPES);
		valueSuggestions.put(new NodeAttribute("primitive-array", "cascade"), CASCADE_TYPES);
		valueSuggestions.put(new NodeAttribute("idbag", "lazy"), TRUE_FALSE);
		valueSuggestions.put(new NodeAttribute("collection-id", "type"), KEY_TYPES);
	}
	
	public static String[] getNodeSuggestions (Node parentNode) {
		if (null != parentNode)
			return (String[]) nodeSuggestions.get(parentNode.getName());
		else
			return null;
	}

	public static List getAttributeSuggestions (Node node, Attribute currentAttribute) {
		if (null == currentAttribute) return getAttributeSuggestions(node, (String) null);
		else return getAttributeSuggestions(node, currentAttribute.getName());
	}

	public static List getAttributeSuggestions (Node node, String currentAttributeName) {
		if (null == node) return null;
		String[] sArr = (String[]) attributeSuggestions.get(node.getName());
		if (null == sArr) return null;
		List suggestions = new ArrayList();
		for (int i=0; i<sArr.length; i++) {
			boolean used = false;
			for (Iterator iter1 = node.getAttributes().iterator(); iter1.hasNext(); ) {
				Attribute att = (Attribute) iter1.next();
				if (att.getName().equalsIgnoreCase(sArr[i])) {
					used = true;
					break;
				}
			}
			if (!used || (null != currentAttributeName && currentAttributeName.length() > 0 && sArr[i].startsWith(currentAttributeName))) {
				suggestions.add(sArr[i]);
			}
		}
		return suggestions;
	}

	public static String[] getAttributeValueSuggestions (String nodeName, String attributeName) throws ClassAttributeValueException {
		NodeAttribute attr = new NodeAttribute(nodeName, attributeName);
		if (null != classSuggestions.get(attr)) {
			throw new ClassAttributeValueException();
		}
		else {
			return (String[]) valueSuggestions.get(attr);
		}
	}

	public static String[] getClassSuggestions (IProject project) {
		List classes = HibernateMappingManager.getInstance(project).getClasses();
		String[] classesArr = new String[classes.size()];
		int index = 0;
		for (Iterator i=classes.iterator(); i.hasNext(); index++) {
			classesArr[index] = ((HibernateClass) i.next()).getAbsoluteValueObjectClassName();
		}
		return classesArr;
	}
}