package com.hudson.hibernatesynchronizer.mapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.hudson.hibernatesynchronizer.Constants;
import com.hudson.hibernatesynchronizer.Plugin;
import com.hudson.hibernatesynchronizer.exceptions.AttributeNotSpecifiedException;
import com.hudson.hibernatesynchronizer.exceptions.TransientPropertyException;
import com.hudson.hibernatesynchronizer.util.HSUtil;


public class HibernateClass extends BaseElement implements Comparable {
	
	public static final int TYPE_CLASS = 1;
	public static final int TYPE_SUBCLASS = 2;
	public static final int TYPE_JOINED_SUBCLASS = 3;
	public static final int TYPE_COMPONENT = 4;

	private IProject project;

	private HibernateDocument document;
	private HibernateClass parent;
	private String packageName;
	private String proxy;
	private String tableName;
	protected String absoluteValueObjectClassName;
	private HibernateClassId id;
	private HibernateClassProperty version;
	private HibernateClassProperty timestamp;

	private List properties = new ArrayList();
	private List manyToOneList = new ArrayList();
	private List oneToOneList = new ArrayList();
	private List collectionList = new ArrayList();
	private List componentList = new ArrayList();
	List subclassList = new ArrayList();
	private List queries = new ArrayList();

	private String daoPackage;
	private String baseDAOPackage;
	private String baseValueObjectPackage;
	private String rootDAOPackage;
	private String baseRootDAOClassName;
	private String absoluteBaseRootDAOClassName;
	private String rootDAOClassName;
	private String absoluteRootDAOClassName;
	
	private boolean syncDAO = true;
	private boolean syncValueObject = true;
	private boolean syncCustom = true;
	private int type;
	private boolean isParent;
	private String scope = "public";
	
	// cache
	List alternateKeys;
	List requiredFields;
	Map allProperties;
	Map allPropertiesWithComposite;
	Map allPropertiesByColumn;

	/**
	 * Constructor for non-subclass
	 * @param node the XML node
	 * @param packageName the package name for this class
	 * @param project the Eclipse project
	 */
	public HibernateClass (Node node, String packageName, IProject project, HibernateDocument document) {
		this(node, packageName, null, project, document);
	}

	/**
	 * Constructor for subclass
	 * @param node the XML node
	 * @param packageName the package name for this class
	 * @param parent the parent for this subclass
	 * @param project the Eclipse project
	 */
	public HibernateClass (Node node, String packageName, HibernateClass parent, IProject project, HibernateDocument document) {
		this(node, packageName, parent, project, true, TYPE_CLASS, document);
	}

	/**
	 * Constructor for component
	 * @param node the XML node
	 * @param packageName the package name for this class
	 * @param parent the parent for this component
	 * @param project the Eclipse project
	 * @param validate whether or not to allow full data
	 * @param type the type of class (matches the TYPE statics in this class)
	 */
	public HibernateClass (Node node, String packageName, HibernateClass parent, IProject project, boolean validate, int type, HibernateDocument document) {
		this.type = type;
		this.packageName = packageName;
		this.project = project;
		this.parent = parent;
		this.document = document;
		setNode(node);
		NamedNodeMap attributes = node.getAttributes();
		for (int i=0; i<attributes.getLength(); i++) {
			Node attNode = attributes.item(i);
			if (attNode.getNodeName().equals("table")) {
				tableName = attNode.getNodeValue();
			}
			if (attNode.getNodeName().equals("proxy")) {
				proxy = attNode.getNodeValue();
			}
			else if ((type == TYPE_CLASS || type == TYPE_JOINED_SUBCLASS) && attNode.getNodeName().equals("name")) {
				setValueObjectClassName(attNode.getNodeValue());
			}
			else if ((type == TYPE_SUBCLASS) && attNode.getNodeName().equals("name")) {
				setValueObjectClassName(attNode.getNodeValue());
			}
		}

		if (node.hasChildNodes()) {
			Node child = node.getFirstChild();
			while (null != child) {
				if (child.getNodeName().equals("meta")) {
					String key = null;
					String value = null;
					Node attNode = child.getAttributes().getNamedItem("attribute");
					if (null != attNode) {
						key = attNode.getNodeValue();
					}
					// check for the auto-DAO meta attribute for class level disabling of the DAO generation
					value = getNodeText(child);
					if (null != key && null != value) {
						if (null != key && null != value) {
							if (key.equals(Constants.PROP_SYNC_DAO) && value.toUpperCase().startsWith("F")) {
								syncDAO = false;
							}
							else if (key.equals(Constants.PROP_SYNC_VALUE_OBJECT) && value.toUpperCase().startsWith("F")) {
								syncValueObject = false;
							}
							else if (key.equals(Constants.PROP_SYNC_CUSTOM) && value.toUpperCase().startsWith("F")) {
								syncCustom = false;
							}
							else if (key.equals("scope-class")) {
								scope = value;
							}
						}
					}
				}
				try {
					if (child.getNodeName().equals("property")) {
				        properties.add(new HibernateClassProperty(this, child));
					}
					else if (child.getNodeName().equals("many-to-one")) {
						manyToOneList.add(new HibernateClassProperty(this, child, HibernateClassProperty.TYPE_MANY_TO_ONE, packageName));
					}
					else if (child.getNodeName().equals("one-to-one")) {
						oneToOneList.add(new HibernateClassProperty(this, child, HibernateClassProperty.TYPE_ONE_TO_ONE, packageName));
					}
					else if (child.getNodeName().equals(HibernateClassCollectionProperty.TYPE_SET)) {
						collectionList.add(new HibernateClassCollectionProperty(this, child, HibernateClassCollectionProperty.TYPE_SET, packageName, project));
					}
					else if (child.getNodeName().equals(HibernateClassCollectionProperty.TYPE_ARRAY) || child.getNodeName().equals(HibernateClassCollectionProperty.TYPE_PRIMITIVE_ARRAY)) {
						collectionList.add(new HibernateClassCollectionProperty(this, child, child.getNodeName(), packageName, project));
					}
					else if (child.getNodeName().equals(HibernateClassCollectionProperty.TYPE_BAG)) {
						collectionList.add(new HibernateClassCollectionProperty(this, child, HibernateClassCollectionProperty.TYPE_BAG, packageName, project));
					}
					else if (child.getNodeName().equals(HibernateClassCollectionProperty.TYPE_LIST)) {
						collectionList.add(new HibernateClassCollectionProperty(this, child, HibernateClassCollectionProperty.TYPE_LIST, packageName, project));
					}
					else if (child.getNodeName().equals(HibernateClassCollectionProperty.TYPE_MAP)) {
						collectionList.add(new HibernateClassCollectionProperty(this, child, HibernateClassCollectionProperty.TYPE_MAP, packageName, project));
					}
					else if (child.getNodeName().equals("version")) {
						this.version = new HibernateClassProperty(this, child);
					}
					else if (child.getNodeName().equals("timestamp")) {
						this.timestamp = new HibernateClassProperty(this, child, false);
						if (null == this.timestamp.getType()) this.timestamp.setType(Date.class.getName());
					}
					else if (child.getNodeName().equals("component")) {
						componentList.add(new HibernateComponentClass(child, packageName, this, false, project));
					}
					else if (child.getNodeName().equals("dynamic-component")) {
						componentList.add(new HibernateComponentClass(child, packageName, this, true, project));
					}
					else if (child.getNodeName().equals("subclass")) {
					    HibernateClass subclass = new HibernateClass(child, packageName, this, project, true, TYPE_SUBCLASS, document);
						subclassList.add(subclass);
						document.addClass(subclass);
					}
					else if (child.getNodeName().equals("joined-subclass")) {
					    HibernateClass subclass = new HibernateClass(child, packageName, this, project, true, TYPE_JOINED_SUBCLASS, document);
						subclassList.add(subclass);
						document.addClass(subclass);
					}
				}
				catch (TransientPropertyException e)
				{}
				child = child.getNextSibling();
			}

			child = node.getFirstChild();
		}
		saveMetaData(node);
		id = new HibernateClassId(this, node, packageName);
		if (!id.exists()) {
			id = null;
		}
		if (validate && (null == absoluteValueObjectClassName || absoluteValueObjectClassName.length() == 0)) {
			throw new AttributeNotSpecifiedException(node, "name");
		}
		if (null != getAbsoluteValueObjectProxyClassName() && getAbsoluteValueObjectProxyClassName().equals(getAbsoluteValueObjectClassName())) {
			proxy = null;
		}
	}

	/**
	 * Set the name of this class
	 * @param name
	 */
	protected void setValueObjectClassName (String name) {
		if (null != packageName && name.indexOf(".") < 0) {
			absoluteValueObjectClassName = packageName + "." + name;
		}
		else {
			absoluteValueObjectClassName = name;
		}
	}

	/**
	 * Return the name of the extension class without any package prefix that represents the
	 * value object used by hibernate for persistance
	 */
	public String getValueObjectClassName() {
		if (null != absoluteValueObjectClassName) {
			return HSUtil.getClassPart(absoluteValueObjectClassName);
		}
		else {
			return null;
		}
	}

	/**
	 * Return the fully qualified name of the extension class that represents the value object
	 * used by hibernate for persistance
	 * @return the fully qualified class name
	 */
	public String getAbsoluteValueObjectClassName() {
		return absoluteValueObjectClassName;
	}

	/**
	 * Return the relative class name of the proxy or null if N/A
	 */
	public String getValueObjectProxyClassName () {
		if (null == proxy) return null;
		else if (proxy.indexOf('.') >= 0)
			return proxy.substring(proxy.lastIndexOf('.') + 1, proxy.length());
		else return proxy;
	}

	/**
	 * Return the fully qualitifed name of the proxy or null if N/A
	 */
	public String getAbsoluteValueObjectProxyClassName () {
		if (null == proxy) return null;
		return getProxyPackage() + "." + getValueObjectProxyClassName();
	}

	/**
	 * Return the name of the extension class without any package prefix that represents the
	 * value object used by hibernate for persistance (or the proxy if exists)
	 */
	public String getValueObjectSignatureClassName() {
		String absoluteSignatureClassName = getAbsoluteValueObjectSignatureClassName();
		if (null != absoluteSignatureClassName) {
			return HSUtil.getClassPart(absoluteSignatureClassName);
		}
		else {
			return null;
		}
	}

	/**
	 * Return the fully qualified name of the extension class that represents the business object
	 * used by hibernate for persistance (or the proxy if exists)
	 * @return the fully qualified class name
	 */
	public String getAbsoluteValueObjectSignatureClassName() {
		String proxyClassName = getAbsoluteValueObjectProxyClassName();
		if (null != proxyClassName) return proxyClassName;
		else return absoluteValueObjectClassName;
	}

	/**
	 * Return the name without the package prefix of the base value object class used for the hibernate persistance 
	 */
	public String getBaseValueObjectClassName () {
		return "Base" + getValueObjectClassName();
	}

	/**
	 * Return the name without the package prefix of the base DAO class used as the SessionFactory wrapper
	 */
	public String getBaseDAOClassName () {
		return "Base" + getValueObjectClassName() + "DAO";
	}

	/**
	 * Return the name without the package prefix of the extension DAO class used as the SessionFactory wrapper
	 */
	public String getDAOClassName () {
		return getValueObjectClassName() + "DAO";
	}

	/**
	 * Return the fully qualified class name of the DAO used for the hibernate persistance
	 */
	public String getAbsoluteDAOClassName () {
		return getDAOPackage() + "." + getDAOClassName();
	}

	/**
	 * Return the fully qualified class name of the Base DAO used for the hibernate persistance
	 */
	public String getAbsoluteBaseDAOClassName () {
		return getBaseDAOPackage() + "." + getBaseDAOClassName();
	}

	/**
	 * Return the fully qualified class name of the base value object class used for the hibernate persistance
	 */
	public String getAbsoluteBaseValueObjectClassName () {
		return getBaseValueObjectPackage() + "." + getBaseValueObjectClassName();
	}

	/**
	 * Return the package prefix without the class name of the extension class that represents
	 * the value object used by hibernate for persistance
	 * @return the package name
	 */
	public String getValueObjectPackage () {
		return HSUtil.getPackagePart(absoluteValueObjectClassName);
	}

	/**
	 * Return the package of the proxy for this class or null if N/A
	 */
	public String getProxyPackage () {
		if (null == proxy) return null;
		else return getValueObjectPackage();
	}

	/**
	 * Return the package prefix that relates to the base DAO class used as the wrapper to the SessionFactory access
	 */
	public String getBaseValueObjectPackage () {
		if (null == baseValueObjectPackage) {
			String basePackageStyle = Plugin.getProperty(project, Constants.PROP_BASE_VO_PACKAGE_STYLE);
			String basePackageName = Plugin.getProperty(project, Constants.PROP_BASE_VO_PACKAGE_NAME);
			if (Constants.PROP_VALUE_SAME.equals(basePackageStyle)) {
				baseValueObjectPackage = getValueObjectPackage();
			}
			else if (Constants.PROP_VALUE_ABSOLUTE.equals(basePackageStyle)) {
				if (null == basePackageName) basePackageName = Constants.DEFAULT_BASE_VO_PACKAGE;
				baseValueObjectPackage = basePackageName;
			}
			else {
				// relative
				if (null == basePackageName) basePackageName = Constants.DEFAULT_BASE_VO_PACKAGE;
				baseValueObjectPackage = HSUtil.addPackageExtension(getValueObjectPackage(), basePackageName); 
			}
		}
		return baseValueObjectPackage;
	}

	/**
	 * Return the package prefix that relates to the extension DAO class used as the wrapper to the SessionFactory access
	 */
	public String getDAOPackage () {
		if (null == daoPackage) {
			String daoPackageStyle = Plugin.getProperty(project, Constants.PROP_DAO_PACKAGE_STYLE);
			String daoPackageName = Plugin.getProperty(project, Constants.PROP_DAO_PACKAGE_NAME);
			if (Constants.PROP_VALUE_SAME.equals(daoPackageStyle)) {
				daoPackage = getValueObjectPackage();
			}
			else if (Constants.PROP_VALUE_ABSOLUTE.equals(daoPackageStyle)) {
				if (null == daoPackageName) daoPackageName = Constants.DEFAULT_DAO_PACKAGE;
				daoPackage = daoPackageName;
			}
			else {
				if (null == daoPackageName) daoPackageName = Constants.DEFAULT_DAO_PACKAGE;
				daoPackage = HSUtil.addPackageExtension(getValueObjectPackage(), daoPackageName); 
			}
		}
		return daoPackage;
	}

	/**
	 * Return the package prefix that relates to the base DAO class used as the wrapper to the SessionFactory access
	 */
	public String getBaseDAOPackage () {
		if (null == baseDAOPackage) {
			boolean useBaseBusinessObjPackage = true;
			try {
				String s = Plugin.getProperty(project, Constants.PROP_BASE_DAO_USE_BASE_PACKAGE);
				if (null != s) useBaseBusinessObjPackage = new Boolean(s).booleanValue();
			}
			catch (Exception e) {}
			if (useBaseBusinessObjPackage) {
				baseDAOPackage = getBaseValueObjectPackage();
			}
			else {
				String baseDAOPackageStyle = Plugin.getProperty(project, Constants.PROP_BASE_DAO_PACKAGE_STYLE);
				String baseDAOPackageName = Plugin.getProperty(project, Constants.PROP_BASE_DAO_PACKAGE_NAME);
				if (Constants.PROP_VALUE_SAME.equals(baseDAOPackageStyle)) {
					baseDAOPackage = getDAOPackage();
				}
				else if (Constants.PROP_VALUE_ABSOLUTE.equals(baseDAOPackageStyle)) {
					if (null == baseDAOPackageName) baseDAOPackageName = Constants.DEFAULT_BASE_DAO_PACKAGE;
					baseDAOPackage = baseDAOPackageName;
				}
				else {
					if (null == baseDAOPackageName) baseDAOPackageName = Constants.DEFAULT_BASE_DAO_PACKAGE;
					baseDAOPackage = HSUtil.addPackageExtension(getDAOPackage(), baseDAOPackageName); 
				}
			}
		}
		return baseDAOPackage;
	}

	/**
	 * Return the package prefix of the root DAO class.
	 */
	public String getRootDAOPackage () {
		return getDAOPackage();
	}

	/**
	 * Return a descriptive label based on the class name (or using the 'label' meta override)
	 */
	public String getLabel() {
		if (getCustomProperties().size() == 0)
			return HSUtil.getPropDescription(getValueObjectClassName());
		else {
			String label = get(IHibernateClassProperty.LABEL_METADATA);
			if (null == label)
				return HSUtil.getPropDescription(getValueObjectClassName());
			else
				return label;
		}
	};

	/**
	 * Return the variable name related to this class that will be used for the generation
	 */
	public String getVarName () {
		return HSUtil.firstLetterLower(getValueObjectClassName());
	}

	/**
	 * Return the id object for the given HibernateClass
	 */
	public HibernateClassId getId() {
		if (type == TYPE_COMPONENT) return getId(false);
		else return getId(true);
	}

	/**
	 * Return the id object for the given HibernateClass
	 */
	public HibernateClassId getId(boolean goToParent) {
		if (goToParent) {
			if (null != parent) return parent.getId();
			else return id;
		}
		else return id;
	}

	/**
	 * return the properties that represent the standard columns in the that relating to this hibernate class
	 * @return a List of HibernateClassProperty objects
	 */
	public List getProperties() {
		return properties;
	}
	
	/**
	 * Return a list of all properties including the properties are hidden inside the components that belong to
	 * this class
	 * @return a list of IHibernateClassProperty objects
	 */
	public List getPropertiesWithComponents () {
	    if (null == propertiesWithComponents) {
	        propertiesWithComponents = new ArrayList();
	        for (Iterator i=getProperties().iterator(); i.hasNext(); ) {
	            propertiesWithComponents.add(i.next());
	        }
	        for (Iterator i=getComponentList().iterator(); i.hasNext(); ) {
	            propertiesWithComponents.add(i.next());
	        }
	    }
	    return propertiesWithComponents;
	}
	private List propertiesWithComponents;

	/**
	 * Return the objects that represent many-to-one relationships for the hibernate class
	 * @return a List of HibernateClassProperty objects
	 */
	public List getManyToOneList() {
		return manyToOneList;
	}

	/**
	 * Return the objects that represent one-to-one relationships for the hibernate class
	 * @return a List of HibernateClassProperty objects
	 */
	public List getOneToOneList() {
		return oneToOneList;
	}

	/**
	 * Return the name of the table that will be used for persistance of this hibernate class
	 */
	public String getTableName() {
		if (null == tableName && null != parent) return parent.getTableName();
		else return tableName;
	}

	/**
	 * Return the list of collection objects for this hibernate class
	 * @return a List of HibernateClassCollectionProperty objects
	 */
	public List getCollectionList() {
		return collectionList;
	}

	/**
	 * Return a list of class that will subclass this hibernate class
	 * @return a list of HibernateClass objects
	 */
	public List getSubclassList() {
		return subclassList;
	}

	/**
	 * Return a list of the components that are defined for this class
	 * @return a list of ComponentHibernateClass objects
	 */
	public List getComponentList() {
		return componentList;
	}
	
	/**
	 * Return the parent HibernateClass object if this is a subclass or null if N/A
	 * @return the parent of the subclass
	 */
	public HibernateClass getParent() {
		return parent;
	}

	/**
	 * Return true if this class is a subclass or false if not
	 */
	public boolean isSubclass () {
		return (null != getParent());
	}

	/**
	 * Return a list of objects represent the query definitions related to this class.
	 * <p><b>Note: since queries are not defined within the class node, queries will
	 * only be added if there is a single class definition in the mapping configuration
	 * file.</b></p>
	 * @return a list of HibernateQuery objects
	 */
	public List getQueries() {
		return queries;
	}

	/**
	 * Set the queries for this class
	 * @param a List of HibernateQuery objects
	 */
	public void setQueries(List queries) {
		this.queries = queries;
	}

	/**
	 * Return the eclipse project related to the resource
	 * @return the current eclipse project
	 */
	public IProject getProject() {
		return project;
	}

	/**
	 * @return true if this class is been allowed to auto-sync the related DAO class and false if not 
	 */
	public boolean canSyncDAO() {
		return syncDAO;
	}

	/**
	 * @return true if this class is been allowed to auto-sync the value object files and false if not 
	 */
	public boolean canSyncValueObject() {
		return syncValueObject;
	}

	/**
	 * @return true if this class is been allowed to auto-sync the custom templates and false if not
	 */
	public boolean canSyncCustom() {
		return syncCustom;
	}

	/**
	 * Return the fully qualified class name of the base root DAO class.
	 */
	public String getAbsoluteBaseRootDAOClassName () {
		if (null == absoluteBaseRootDAOClassName) {
			boolean useCustomDAO = Plugin.getBooleanProperty(project, Constants.PROP_USE_CUSTOM_ROOT_DAO, false);
			if (useCustomDAO) {
				if (null == absoluteBaseRootDAOClassName) absoluteBaseRootDAOClassName = Plugin.getProperty(project, Constants.PROP_CUSTOM_ROOT_DAO_CLASS);
			}
			else {
				absoluteBaseRootDAOClassName = getBaseDAOPackage() + "._BaseRootDAO";
			}
		}
		return absoluteBaseRootDAOClassName;
	}

	/**
	 * Return the fully qualified class name of the root DAO class.
	 */
	public String getAbsoluteRootDAOClassName () {
		return getDAOPackage() + "." + getRootDAOClassName();
	}

	/**
	 * Return the class name of the root DAO class without the package prefix.
	 */
	public String getRootDAOClassName () {
		return "_RootDAO";
	}

	/**
	 * @return true if this class uses a custom DAO and false if not
	 */
	public boolean useCustomDAO () {
		String useCustomDAO = Plugin.getProperty(project, Constants.PROP_USE_CUSTOM_ROOT_DAO);
		if (null != useCustomDAO && useCustomDAO.equalsIgnoreCase(Boolean.TRUE.toString())) {
			return (null != Plugin.getProperty(project, Constants.PROP_CUSTOM_ROOT_DAO_CLASS));
		}
		return false;
	}
	
	/**
	 * Return the class name of the root DAO class without the package prefix.
	 */
	public String getBaseRootDAOClassName () {
		if (null == baseRootDAOClassName) {
			String s = getAbsoluteBaseRootDAOClassName();
			int index = s.lastIndexOf(".");
			if (index > 0) baseRootDAOClassName = s.substring(index+1, s.length());
			else baseRootDAOClassName = s;
		}
		return baseRootDAOClassName;
	}

	/**
	 * Return the HibernateClassProperty that relates to the version or null if N/A
	 */
	public HibernateClassProperty getVersion() {
		return version;
	}

	/**
	 * Return the HibernateClassProperty that relates to the timestamp or null if N/A
	 */
	public HibernateClassProperty getTimestamp() {
		return timestamp;
	}

	/**
	 * Return a list of properties that relate to the alternate keys (or a 0 length list if N/A)
	 * @return a list of IHibernateClassProperty objects
	 */
	public List getAlternateKeys() {
		if (null == alternateKeys) {
			alternateKeys = new ArrayList();
			for (Iterator i=getProperties().iterator(); i.hasNext(); ) {
				HibernateClassProperty prop = (HibernateClassProperty) i.next();
				if (prop.isAlternateKey()) alternateKeys.add(prop);
			}
			for (Iterator i=getOneToOneList().iterator(); i.hasNext(); ) {
				HibernateClassProperty prop = (HibernateClassProperty) i.next();
				if (prop.isAlternateKey()) alternateKeys.add(prop);
			}
			for (Iterator i=getManyToOneList().iterator(); i.hasNext(); ) {
				HibernateClassProperty prop = (HibernateClassProperty) i.next();
				if (prop.isAlternateKey()) alternateKeys.add(prop);
			}
		}
		return alternateKeys;
	}

	/**
	 * Return a list of properties that are required
	 * @return a list of IHibernateClassProperty objects
	 */
	public List getRequiredFields() {
		if (null == requiredFields) {
			requiredFields = new ArrayList();
			for (Iterator i=getOneToOneList().iterator(); i.hasNext(); ) {
				HibernateClassProperty prop = (HibernateClassProperty) i.next();
				if (prop.isRequired()) requiredFields.add(prop);
			}
			for (Iterator i=getManyToOneList().iterator(); i.hasNext(); ) {
				HibernateClassProperty prop = (HibernateClassProperty) i.next();
				if (prop.isRequired()) requiredFields.add(prop);
			}
			for (Iterator i=getProperties().iterator(); i.hasNext(); ) {
				HibernateClassProperty prop = (HibernateClassProperty) i.next();
				if (prop.isRequired()) requiredFields.add(prop);
			}
		}
		return requiredFields;
	}

	/**
	 * Return the root parent of a subclass or this if no subclass
	 */
	public HibernateClass getParentRoot () {
		if (isSubclass()) return parent.getParentRoot();
		else return this;
	}

	/**
	 * Return all the properties of the class but do not include any composite key properties
	 * @return a list of IHibernateClassProperty
	 */
	public Collection getAllProperties () {
		return getAllProperties(false);
	}

	/**
	 * Return all the properties of the class
	 * @addCompositeKeyProperties set to true if you want to add properties that are part of the composite key
	 * @return a list of IHibernateClassProperty objects
	 */
	public Collection getAllProperties (boolean addCompositeKeyProperties) {
		Map values = null;
		if (addCompositeKeyProperties) values = allPropertiesWithComposite;
		else values = allProperties;
		if (null == values) {
			values = new HashMap();
			for (Iterator i=getProperties().iterator(); i.hasNext(); ) {
				HibernateClassProperty hcp = (HibernateClassProperty) i.next();
				values.put(HSUtil.firstLetterUpper(hcp.getName()), hcp);
			}
			for (Iterator i=getManyToOneList().iterator(); i.hasNext(); ) {
				HibernateClassProperty hcp = (HibernateClassProperty) i.next();
				values.put(HSUtil.firstLetterUpper(hcp.getName()), hcp);
			}
			for (Iterator i=getOneToOneList().iterator(); i.hasNext(); ) {
				HibernateClassProperty hcp = (HibernateClassProperty) i.next();
				values.put(HSUtil.firstLetterUpper(hcp.getName()), hcp);
			}
			for (Iterator i=getComponentList().iterator(); i.hasNext(); ) {
				HibernateComponentClass hcc = (HibernateComponentClass) i.next();
				for (Iterator i1=hcc.getProperties().iterator(); i1.hasNext(); ) {
				    HibernateClassProperty hcp = (HibernateClassProperty) i1.next();
				    values.put(HSUtil.firstLetterUpper(hcp.getName()), hcp);
				}
			}
			if (addCompositeKeyProperties) {
				if (null != getId() && getId().isComposite()) {
					for (Iterator i=getId().getProperties().iterator(); i.hasNext(); ) {
						HibernateClassProperty hcp = (HibernateClassProperty) i.next();
						values.put(HSUtil.firstLetterUpper(hcp.getName()), hcp);
					}
				}
			}
			else {
				if (null != getId() && (!getId().isComposite() || getId().hasExternalClass())) {
					values.put(HSUtil.firstLetterUpper(getId().getProperty().getName()), getId().getProperty());
				}
			}
			if (addCompositeKeyProperties) allPropertiesWithComposite = values;
			else allProperties = values;
		}
		return values.values();
	}

	/**
	 * Return the property matching the property name
	 */
	public IHibernateClassProperty getProperty(String propName) {
		if (null == propName) return null;
		getAllProperties();
		return (IHibernateClassProperty) allProperties.get(HSUtil.firstLetterUpper(propName));
	}

	/**
	 * Return the property matching the column name
	 */
	public IHibernateClassProperty getPropertyByColName(String colName) {
		if (null == colName) return null;
		if (null == allPropertiesByColumn) {
			allPropertiesByColumn = new HashMap();
			if (null != getId()) {
				if (getId().isComposite()) {
					for (Iterator i=getId().getProperties().iterator(); i.hasNext(); ) {
						HibernateClassProperty hcp = (HibernateClassProperty) i.next();
						allPropertiesByColumn.put(hcp.getColumn(), hcp);
					}
				}
				else {
					HibernateClassProperty hcp = (HibernateClassProperty) getId().getProperty();
					allPropertiesByColumn.put(hcp.getColumn(), hcp);
				}
			}
			for (Iterator i=getProperties().iterator(); i.hasNext(); ) {
				HibernateClassProperty hcp = (HibernateClassProperty) i.next();
				allPropertiesByColumn.put(hcp.getColumn(), hcp);
			}
			for (Iterator i=getManyToOneList().iterator(); i.hasNext(); ) {
				HibernateClassProperty hcp = (HibernateClassProperty) i.next();
				allPropertiesByColumn.put(hcp.getColumn(), hcp);
			}
			for (Iterator i=getOneToOneList().iterator(); i.hasNext(); ) {
				HibernateClassProperty hcp = (HibernateClassProperty) i.next();
				allPropertiesByColumn.put(hcp.getColumn(), hcp);
			}
			for (Iterator i=getComponentList().iterator(); i.hasNext(); ) {
				HibernateComponentClass hcc = (HibernateComponentClass) i.next();
				for (Iterator j=hcc.getProperties().iterator(); j.hasNext(); ) {
					IHibernateClassProperty hcp = (IHibernateClassProperty) j.next();
					if (null != hcp.getColumn()) allPropertiesByColumn.put(hcp.getColumn(), hcp);
				}
			}
			if (null != getId()) {
				if (getId().isComposite()) {
					for (Iterator i=getId().getProperties().iterator(); i.hasNext(); ) {
						HibernateClassProperty hcp = (HibernateClassProperty) i.next();
						allPropertiesByColumn.put(hcp.getColumn(), hcp);
					}
				}
			}
			else {
				allProperties.put(getId().getProperty().getColumn(), getId().getProperty());
			}
		}
		return (IHibernateClassProperty) allPropertiesByColumn.get(colName);
	}

	/**
	 * Return true if this class has a proxy and false if not
	 */
	public boolean hasProxy () {
		return null != proxy;
	}

	public boolean isComponent () {
		return type == TYPE_COMPONENT;
	}

	/**
	 * Return the class scope of the value object 
	 * @return
	 */
	public String getValueObjectScope() {
		return scope;
	}

	/**
	 * Return the document that relates to this mapping file
	 */
	public HibernateDocument getDocument () {
		return document;
	}

	/**
	 * Return the reserved properties associated with this element
	 */
	protected String[] getReservedProperties() {
		return RP;
	}
	private static final String[] RP = new String[] {"name", "table", "discriminator", "mutable", "schema", "proxy", "dynamic-update", "dynamic-insert", "select-before-update", "polymorphism", "where", "persister", "batch-size", "optimistic-lock", "lazy", "class-description"};

	/**
	 * Compare this to another object
	 */
	public int compareTo(Object arg0) {
		if (arg0 instanceof HibernateClass) {
			return getValueObjectClassName().compareTo(((HibernateClass) arg0).getValueObjectClassName());
		}
		else {
			return -1;
		}
	}
}