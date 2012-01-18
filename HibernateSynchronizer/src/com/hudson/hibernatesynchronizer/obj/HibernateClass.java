/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.obj;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.hudson.hibernatesynchronizer.Constants;
import com.hudson.hibernatesynchronizer.Plugin;
import com.hudson.hibernatesynchronizer.exception.AttributeNotSpecifiedException;
import com.hudson.hibernatesynchronizer.exception.TransientPropertyException;
import com.hudson.hibernatesynchronizer.util.HSUtil;

/**
 * @author <a href="mailto: joe@binamics.com">Joe Hudson </a>
 * 
 * This represents data related to the 'class' node of the hibernate mapping
 * configuration file.
 */
public class HibernateClass extends BaseElement implements Comparable {

    public static final int TYPE_CLASS = 1;

    public static final int TYPE_SUBCLASS = 2;

    public static final int TYPE_JOINED_SUBCLASS = 3;

    public static final int TYPE_COMPONENT = 4;

    private String packageName;

    private String proxy;

    private IProject currentProject;

    private HibernateClass parent;

    private String tableName;

    protected String fullClassName;

    private HibernateClassId id;

    private HibernateClassProperty version;

    private HibernateClassProperty timestamp;

    private List properties = new ArrayList();

    private List manyToOneList = new ArrayList();

    private List oneToOneList = new ArrayList();

    private List collectionList = new ArrayList();

    private List componentList = new ArrayList();

    private List subclassList = new ArrayList();

    private List queries = new ArrayList();

    private String managerPackage;

    private String baseManagerPackage;

    private String basePackage;

    private boolean canMakeManager = true;;

    private String rootManagerPackage;

    private String rootManagerShortClass;

    private String rootManagerClass;

    private int type;

    private boolean isParent;

    // cache
    List alternateKeys;

    List requiredFields;

    Map allProperties;

    Map allPropertiesWithComposite;

    Map allPropertiesByColumn;

    public HibernateClass(Node node, String packageName, IProject currentProject) {
        this(node, packageName, null, currentProject);
    }

    public HibernateClass(Node node, String packageName, HibernateClass parent,
            IProject currentProject) {
        this(node, packageName, parent, currentProject, true, TYPE_CLASS);
    }

    public HibernateClass(Node node, String packageName, HibernateClass parent,
            IProject currentProject, boolean validate, int type) {
        this.type = type;
        this.packageName = packageName;
        this.currentProject = currentProject;
        this.parent = parent;
        setNode(node);
        NamedNodeMap attributes = node.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attNode = attributes.item(i);
            if (attNode.getNodeName().equals("table")) {
                tableName = attNode.getNodeValue();
            }
            if (attNode.getNodeName().equals("proxy")) {
                proxy = attNode.getNodeValue();
            } else if ((type == TYPE_CLASS || type == TYPE_JOINED_SUBCLASS || type == TYPE_COMPONENT)
                    && attNode.getNodeName().equals("name")) {
                setName(attNode.getNodeValue());
            } else if ((type == TYPE_SUBCLASS)
                    && attNode.getNodeName().equals("name")) {
                setName(attNode.getNodeValue());
            } else if (attNode.getNodeName().equals(Constants.PROP_AUTO_DAO)) {
                String s = attNode.getNodeValue();
                if (s.length() > 0 && s.substring(0, 1).equalsIgnoreCase("F")) {
                    canMakeManager = false;
                } else {
                    canMakeManager = true;
                }
            }
        }

        if (node.hasChildNodes()) {
            Node child = node.getFirstChild();
            while (null != child) {
                if (child.getNodeName().equals("meta")) {
                    String key = null;
                    String value = null;
                    Node attNode = child.getAttributes().getNamedItem(
                            "attribute");
                    if (null != attNode) {
                        key = attNode.getNodeValue();
                    }
                    value = getNodeText(child);
                    if (null != key && null != value) {
                        if (null != key && null != value) {
                            if (key.equals(Constants.PROP_AUTO_DAO)
                                    && value.toUpperCase().startsWith("F")) {
                                canMakeManager = false;
                            }
                        }
                    }
                }
                try {
                    if (child.getNodeName().equals("property")) {
                        properties.add(new HibernateClassProperty(this, child));
                    } else if (child.getNodeName().equals("many-to-one")) {
                        manyToOneList.add(new HibernateClassProperty(this,
                                child, HibernateClassProperty.TYPE_MANY_TO_ONE,
                                packageName));
                    } else if (child.getNodeName().equals("one-to-one")) {
                        oneToOneList.add(new HibernateClassProperty(this,
                                child, HibernateClassProperty.TYPE_ONE_TO_ONE,
                                packageName));
                    } else if (child.getNodeName().equals(
                            HibernateClassCollectionProperty.TYPE_SET)) {
                        collectionList
                                .add(new HibernateClassCollectionProperty(
                                        this,
                                        child,
                                        HibernateClassCollectionProperty.TYPE_SET,
                                        packageName, currentProject));
                    } else if (child.getNodeName().equals(
                            HibernateClassCollectionProperty.TYPE_ARRAY)
                            || child
                                    .getNodeName()
                                    .equals(
                                            HibernateClassCollectionProperty.TYPE_PRIMITIVE_ARRAY)) {
                        collectionList
                                .add(new HibernateClassCollectionProperty(this,
                                        child, child.getNodeName(),
                                        packageName, currentProject));
                    } else if (child.getNodeName().equals(
                            HibernateClassCollectionProperty.TYPE_BAG)) {
                        collectionList
                                .add(new HibernateClassCollectionProperty(
                                        this,
                                        child,
                                        HibernateClassCollectionProperty.TYPE_BAG,
                                        packageName, currentProject));
                    } else if (child.getNodeName().equals(
                            HibernateClassCollectionProperty.TYPE_LIST)) {
                        collectionList
                                .add(new HibernateClassCollectionProperty(
                                        this,
                                        child,
                                        HibernateClassCollectionProperty.TYPE_LIST,
                                        packageName, currentProject));
                    } else if (child.getNodeName().equals(
                            HibernateClassCollectionProperty.TYPE_MAP)) {
                        collectionList
                                .add(new HibernateClassCollectionProperty(
                                        this,
                                        child,
                                        HibernateClassCollectionProperty.TYPE_MAP,
                                        packageName, currentProject));
                    } else if (child.getNodeName().equals("version")) {
                        this.version = new HibernateClassProperty(this, child);
                    } else if (child.getNodeName().equals("timestamp")) {
                        this.timestamp = new HibernateClassProperty(this,
                                child, false);
                        if (null == this.timestamp.getType())
                            this.timestamp.setType(Date.class.getName());
                    } else if (child.getNodeName().equals("component")) {
                        componentList.add(new HibernateComponentClass(child,
                                packageName, this, false, currentProject));
                    } else if (child.getNodeName().equals("dynamic-component")) {
                        componentList.add(new HibernateComponentClass(child,
                                packageName, this, true, currentProject));
                    } else if (child.getNodeName().equals("subclass")) {
                        subclassList.add(new HibernateClass(child, packageName,
                                this, currentProject, true, TYPE_SUBCLASS));
                    } else if (child.getNodeName().equals("joined-subclass")) {
                        subclassList.add(new HibernateClass(child, packageName,
                                this, currentProject, true,
                                TYPE_JOINED_SUBCLASS));
                    }
                } catch (TransientPropertyException e) {
                }
                child = child.getNextSibling();
            }

            child = node.getFirstChild();
        }
        saveMetaData(node);
        id = new HibernateClassId(this, node, packageName);
        if (!id.exists()) {
            id = null;
        }
        if (validate && (null == fullClassName || fullClassName.length() == 0)) {
            throw new AttributeNotSpecifiedException(node, "name");
        }
        if (null != getFullProxyClassName()
                && getFullProxyClassName().equals(getFullClassName())) {
            proxy = null;
        }
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Table Name: " + tableName + "; Class: " + fullClassName
                + "\n");
        for (int i = 0; i < properties.size(); i++) {
            sb.append(properties.get(i));
        }
        return sb.toString();
    }

    public boolean hasProxy() {
        return null != proxy;
    }

    public String getProxyClassName() {
        if (null == proxy)
            return null;
        else if (proxy.indexOf('.') >= 0)
            return proxy.substring(proxy.lastIndexOf('.') + 1, proxy.length());
        else
            return proxy;
    }

    public String getFullProxyClassName() {
        if (null == proxy)
            return null;
        return getProxyPackageName() + "." + getProxyClassName();
    }

    public String getProxyPackageName() {
        if (null == proxy)
            return null;
        else
            return getPackageName();
    }

    protected void setName(String name) {
        if (null != packageName && name.indexOf(".") < 0) {
            fullClassName = packageName + "." + name;
        } else {
            fullClassName = name;
        }
    }

    /**
     * Return the fully qualified name of the extension class that represents
     * the business object used by hibernate for persistance
     * 
     * @return the fully qualified class name
     */
    public String getFullClassName() {
        return fullClassName;
    }

    /**
     * Return the fully qualified name of the extension class that represents
     * the business object used by hibernate for persistance (of the prozy if
     * exists)
     * 
     * @return the fully qualified class name
     */
    public String getSignatureFullClassName() {
        String proxyClassName = getFullProxyClassName();
        if (null != proxyClassName)
            return proxyClassName;
        else
            return fullClassName;
    }

    /**
     * Return a descriptive label based on the class name
     */
    public String getLabel() {
        if (getCustomProperties().size() == 0)
            return HSUtil.getPropDescription(getClassName());
        else {
            String label = get(IHibernateClassProperty.LABEL_METADATA);
            if (null == label)
                return HSUtil.getPropDescription(getClassName());
            else
                return label;
        }
    };

    /**
     * Return the variable name related to this class
     */
    public String getVarName() {
        return HSUtil.firstLetterLower(getClassName());
    }

    /**
     * Return the variable name related to the DAO for this class
     */
    public String getDAOVarName() {
        return HSUtil.firstLetterLower(getClassName() + "DAO");
    }

    /**
     * Return the name of the extension class without any package prefix that
     * represents the business object used by hibernate for persistance
     * 
     * @return the class name
     */
    public String getClassName() {
        if (null != fullClassName) {
            return HSUtil.getClassPart(fullClassName);
        } else {
            return null;
        }
    }

    /**
     * Return the name of the extension class without any package prefix that
     * represents the business object used by hibernate for persistance (or the
     * proxy if exists)
     * 
     * @return the class name
     */
    public String getSignatureClassName() {
        String signatureFullClassName = getSignatureFullClassName();
        if (null != signatureFullClassName) {
            return HSUtil.getClassPart(signatureFullClassName);
        } else {
            return null;
        }
    }

    /**
     * Return the package prefix without the class name of the extension class
     * that represents the business object used by hibernate for persistance
     * 
     * @return the package name
     */
    public String getPackageName() {
        return HSUtil.getPackagePart(fullClassName);
    }

    /**
     * Return the id object for the given HibernateClass
     */
    public HibernateClassId getId() {
        if (null != parent)
            return parent.getId();
        else
            return id;
    }

    /**
     * return the properties that represent the standard columns in the that
     * relating to this hibernate class
     * 
     * @return a List of HibernateClassProperty objects
     */
    public List getProperties() {
        return properties;
    }

    private List propertiesWithComponents;

    public List getPropertiesWithComponents() {
        if (null == propertiesWithComponents) {
            propertiesWithComponents = new ArrayList();
            for (Iterator i = getProperties().iterator(); i.hasNext();) {
                propertiesWithComponents.add(i.next());
            }
            for (Iterator i = getComponentList().iterator(); i.hasNext();) {
                propertiesWithComponents.add(i.next());
            }
        }
        return propertiesWithComponents;
    }

    /**
     * Return the objects that represent many-to-one relationships for the
     * hibernate class
     * 
     * @return a List of HibernateClassProperty objects
     */
    public List getManyToOneList() {
        return manyToOneList;
    }

    /**
     * Return the objects that represent one-to-one relationships for the
     * hibernate class
     * 
     * @return a List of HibernateClassProperty objects
     */
    public List getOneToOneList() {
        return oneToOneList;
    }

    /**
     * Return the name of the table that will be used for persistance of this
     * hibernate class
     * 
     * @return the table name
     */
    public String getTableName() {
        if (null == tableName && null != parent)
            return parent.getTableName();
        else
            return tableName;
    }

    /**
     * Return the package prefix that relates to the base DAO class used as the
     * wrapper to the SessionFactory access
     * 
     * @return the base DAO package prefix
     */
    public String getBasePackageName() {
        if (null == basePackage) {
            String basePackageStyle = Plugin.getProperty(currentProject,
                    Constants.PROP_BASE_PACKAGE_STYLE);
            String basePackageName = Plugin.getProperty(currentProject,
                    Constants.PROP_BASE_PACKAGE_NAME);
            if (Constants.PROP_VALUE_SAME.equals(basePackageStyle)) {
                basePackage = getPackageName();
            } else if (Constants.PROP_VALUE_ABSOLUTE.equals(basePackageStyle)) {
                if (null == basePackageName)
                    basePackageName = Constants.DEFAULT_BASE_PACKAGE;
                basePackage = basePackageName;
            } else {
                if (null == basePackageName)
                    basePackageName = Constants.DEFAULT_BASE_PACKAGE;
                basePackage = HSUtil.addPackageExtension(getPackageName(),
                        basePackageName);
            }
        }
        return basePackage;
    }

    /**
     * Return the package prefix that relates to the extension DAO class used as
     * the wrapper to the SessionFactory access
     * 
     * @return the extension DAO package prefix
     */
    public String getDAOPackageName() {
        if (null == managerPackage) {
            String daoPackageStyle = Plugin.getProperty(currentProject,
                    Constants.PROP_DAO_PACKAGE_STYLE);
            String daoPackageName = Plugin.getProperty(currentProject,
                    Constants.PROP_DAO_PACKAGE_NAME);
            if (Constants.PROP_VALUE_SAME.equals(daoPackageStyle)) {
                managerPackage = getPackageName();
            } else if (Constants.PROP_VALUE_ABSOLUTE.equals(daoPackageStyle)) {
                if (null == daoPackageName)
                    daoPackageName = Constants.DEFAULT_DAO_PACKAGE;
                managerPackage = daoPackageName;
            } else {
                if (null == daoPackageName)
                    daoPackageName = Constants.DEFAULT_DAO_PACKAGE;
                managerPackage = HSUtil.addPackageExtension(getPackageName(),
                        daoPackageName);
            }
        }
        return managerPackage;
    }

    /**
     * Return the package prefix that relates to the base DAO class used as the
     * wrapper to the SessionFactory access
     * 
     * @return the base DAO package prefix
     */
    public String getBaseDAOPackageName() {
        if (null == baseManagerPackage) {
            boolean useBaseBusinessObjPackage = true;
            try {
                String s = Plugin.getProperty(currentProject,
                        Constants.PROP_BASE_DAO_USE_BASE_PACKAGE);
                if (null != s)
                    useBaseBusinessObjPackage = new Boolean(s).booleanValue();
            } catch (Exception e) {
            }
            if (useBaseBusinessObjPackage) {
                baseManagerPackage = getBasePackageName();
            } else {
                String baseDAOPackageStyle = Plugin.getProperty(currentProject,
                        Constants.PROP_BASE_DAO_PACKAGE_STYLE);
                String baseDAOPackageName = Plugin.getProperty(currentProject,
                        Constants.PROP_BASE_DAO_PACKAGE_NAME);
                if (Constants.PROP_VALUE_SAME.equals(baseDAOPackageStyle)) {
                    baseManagerPackage = getDAOPackageName();
                } else if (Constants.PROP_VALUE_ABSOLUTE
                        .equals(baseDAOPackageStyle)) {
                    if (null == baseDAOPackageName)
                        baseDAOPackageName = Constants.DEFAULT_BASE_DAO_PACKAGE;
                    baseManagerPackage = baseDAOPackageName;
                } else {
                    if (null == baseDAOPackageName)
                        baseDAOPackageName = Constants.DEFAULT_BASE_DAO_PACKAGE;
                    baseManagerPackage = HSUtil.addPackageExtension(
                            getDAOPackageName(), baseDAOPackageName);
                }
            }
        }
        return baseManagerPackage;
    }

    /**
     * Return the name without the package prefix of the base class used for the
     * hibernate persistance
     * 
     * @return the base class name without the package prefix
     */
    public String getBaseClassName() {
        return "Base" + getClassName();
    }

    /**
     * Return the name without the package prefix of the base DAO class used as
     * the SessionFactory wrapper
     * 
     * @return the name of the base DAO class without the package prefix
     */
    public String getBaseDAOClassName() {
        return "Base" + getClassName() + "DAO";
    }

    /**
     * Return the name without the package prefix of the extension DAO class
     * used as the SessionFactory wrapper
     * 
     * @return the name of the extension DAO class without the package prefix
     */
    public String getDAOClassName() {
        return getClassName() + "DAO";
    }

    /**
     * Return the fully qualified class name of the DAO used for the hibernate
     * persistance
     * 
     * @return the fully qualified base class name
     */
    public String getFullDAOClassName() {
        return getDAOPackageName() + "." + getDAOClassName();
    }

    /**
     * Return the fully qualified class name of the Base DAO used for the
     * hibernate persistance
     * 
     * @return the fully qualified base class name
     */
    public String getFullBaseDAOClassName() {
        return getBaseDAOPackageName() + "." + getBaseDAOClassName();
    }

    /**
     * Return the fully qualified class name of the base class used for the
     * hibernate persistance
     * 
     * @return the fully qualified base class name
     */
    public String getFullBaseClassName() {
        return getBasePackageName() + "." + getBaseClassName();
    }

    /**
     * Return the list of collection objects for this hibernate class
     * 
     * @return a List of HibernateClassCollectionProperty objects
     */
    public List getCollectionList() {
        return collectionList;
    }

    /**
     * Return a list of class that will subclass this hibernate class
     * 
     * @return a list of HibernateClass objects
     */
    public List getSubclassList() {
        return subclassList;
    }

    /**
     * Return a list of the components that are defined for this class
     * 
     * @return a list of ComponentHibernateClass objects
     */
    public List getComponentList() {
        return componentList;
    }

    /**
     * Return the parent HibernateClass object if this is a subclass or null if
     * N/A
     * 
     * @return the parent of the subclass
     */
    public HibernateClass getParent() {
        return parent;
    }

    /**
     * Return true if this class is a subclass or false if not
     */
    public boolean isSubclass() {
        return (null != getParent());
    }

    /**
     * Return a list of objects represent the query definitions related to this
     * class.
     * <p>
     * <b>Note: since queries are not defined within the class node, queries
     * will only be added if there is a single class definition in the mapping
     * configuration file. </b>
     * </p>
     * 
     * @return a list of HibernateQuery objects
     */
    public List getQueries() {
        return queries;
    }

    /**
     * Set the queries for this class
     * 
     * @param a
     *            List of HibernateQuery objects
     */
    public void setQueries(List queries) {
        this.queries = queries;
    }

    /**
     * Return the eclipse project related to the resource that is currently
     * being saved.
     * 
     * @return the current eclipse project
     */
    public IProject getCurrentProject() {
        return currentProject;
    }

    /**
     * @return true if this class is been allowed to auto-generate the related
     *         DAO class and false if not
     */
    public boolean canMakeDAO() {
        return canMakeManager;
    }

    /**
     * Return the package prefix of the root DAO class.
     */
    public String getRootDAOPackageName() {
        return getDAOPackageName();
    }

    /**
     * Return the fully qualified class name of the root DAO class.
     */
    public String getBaseRootDAOFullClassName() {
        if (null == rootManagerClass) {
            String useCustomManager = Plugin.getProperty(currentProject,
                    Constants.PROP_USE_CUSTOM_ROOT_DAO);
            if (null != useCustomManager
                    && useCustomManager.equalsIgnoreCase(Boolean.TRUE
                            .toString())) {
                if (null == rootManagerClass)
                    rootManagerClass = Plugin.getProperty(currentProject,
                            Constants.PROP_CUSTOM_ROOT_DAO_CLASS);
            } else {
                rootManagerClass = getBaseDAOPackageName() + "._BaseRootDAO";
            }
        }
        return rootManagerClass;
    }

    /**
     * Return the fully qualified class name of the root DAO class.
     */
    public String getRootDAOFullClassName() {
        return getDAOPackageName() + "." + getRootDAOClassName();
    }

    /**
     * Return the class name of the root DAO class without the package prefix.
     */
    public String getRootDAOClassName() {
        return "_RootDAO";
    }

    public boolean usesCustomDAO() {
        String useCustomManager = Plugin.getProperty(currentProject,
                Constants.PROP_USE_CUSTOM_ROOT_DAO);
        if (null != useCustomManager
                && useCustomManager.equalsIgnoreCase(Boolean.TRUE.toString())) {
            if (null == rootManagerClass)
                rootManagerClass = Plugin.getProperty(currentProject,
                        Constants.PROP_CUSTOM_ROOT_DAO_CLASS);
            if (null != rootManagerClass) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return the class name of the root DAO class without the package prefix.
     */
    public String getBaseRootDAOClassName() {
        if (null == rootManagerShortClass) {
            String useCustomManager = Plugin.getProperty(currentProject,
                    Constants.PROP_USE_CUSTOM_ROOT_DAO);
            if (null != useCustomManager
                    && useCustomManager.equalsIgnoreCase(Boolean.TRUE
                            .toString())) {
                if (null == rootManagerClass)
                    rootManagerClass = Plugin.getProperty(currentProject,
                            Constants.PROP_CUSTOM_ROOT_DAO_CLASS);
                if (null != rootManagerClass) {
                    int index = rootManagerClass.lastIndexOf(".");
                    if (index > 0) {
                        rootManagerShortClass = rootManagerClass.substring(
                                index, rootManagerPackage.length());
                    } else {
                        rootManagerShortClass = rootManagerClass;
                    }
                } else {
                    rootManagerShortClass = "_BaseRootDAO";
                }
            } else {
                rootManagerShortClass = "_BaseRootDAO";
            }
        }
        return rootManagerShortClass;
    }

    /**
     * Return the HibernateClassProperty that relates to the version or null if
     * N/A
     */
    public HibernateClassProperty getVersion() {
        return version;
    }

    /**
     * Return the HibernateClassProperty that relates to the timestamp or null
     * if N/A
     */
    public HibernateClassProperty getTimestamp() {
        return timestamp;
    }

    /**
     * Return a list of ClassProperty objects that relate to the alternate keys
     * (or a 0 length list if N/A)
     */
    public List getAlternateKeys() {
        if (null == alternateKeys) {
            alternateKeys = new ArrayList();
            for (Iterator i = getProperties().iterator(); i.hasNext();) {
                HibernateClassProperty prop = (HibernateClassProperty) i.next();
                if (prop.isAlternateKey())
                    alternateKeys.add(prop);
            }
            for (Iterator i = getOneToOneList().iterator(); i.hasNext();) {
                HibernateClassProperty prop = (HibernateClassProperty) i.next();
                if (prop.isAlternateKey())
                    alternateKeys.add(prop);
            }
            for (Iterator i = getManyToOneList().iterator(); i.hasNext();) {
                HibernateClassProperty prop = (HibernateClassProperty) i.next();
                if (prop.isAlternateKey())
                    alternateKeys.add(prop);
            }
        }
        return alternateKeys;
    }

    /**
     * Return a list of ClassProperty objects that are required
     */
    public List getRequiredFields() {
        if (null == requiredFields) {
            requiredFields = new ArrayList();
            for (Iterator i = getOneToOneList().iterator(); i.hasNext();) {
                HibernateClassProperty prop = (HibernateClassProperty) i.next();
                if (prop.isRequired())
                    requiredFields.add(prop);
            }
            for (Iterator i = getManyToOneList().iterator(); i.hasNext();) {
                HibernateClassProperty prop = (HibernateClassProperty) i.next();
                if (prop.isRequired())
                    requiredFields.add(prop);
            }
            for (Iterator i = getProperties().iterator(); i.hasNext();) {
                HibernateClassProperty prop = (HibernateClassProperty) i.next();
                if (prop.isRequired())
                    requiredFields.add(prop);
            }
        }
        return requiredFields;
    }

    /**
     * Return the root parent of a subclass or this if no subclass
     */
    public HibernateClass getParentRoot() {
        if (isSubclass())
            return parent.getParentRoot();
        else
            return this;
    }

    /**
     * Return all the properties of the class
     * 
     * @return a list of IHibernateClassProperty
     */
    public Collection getAllProperties() {
        return getAllProperties(false);
    }

    /**
     * Return all the properties of the class
     * 
     * @return a list of IHibernateClassProperty
     */
    public Collection getAllProperties(boolean addCompositeKeyProperties) {
        Map values = null;
        if (addCompositeKeyProperties)
            values = allPropertiesWithComposite;
        else
            values = allProperties;
        if (null == values) {
            values = new HashMap();
            for (Iterator i = getProperties().iterator(); i.hasNext();) {
                HibernateClassProperty hcp = (HibernateClassProperty) i.next();
                values.put(HSUtil.firstLetterUpper(hcp.getName()), hcp);
            }
            for (Iterator i = getManyToOneList().iterator(); i.hasNext();) {
                HibernateClassProperty hcp = (HibernateClassProperty) i.next();
                values.put(HSUtil.firstLetterUpper(hcp.getName()), hcp);
            }
            for (Iterator i = getOneToOneList().iterator(); i.hasNext();) {
                HibernateClassProperty hcp = (HibernateClassProperty) i.next();
                values.put(HSUtil.firstLetterUpper(hcp.getName()), hcp);
            }
            for (Iterator i = getComponentList().iterator(); i.hasNext();) {
                HibernateComponentClass hcc = (HibernateComponentClass) i
                        .next();
                for (Iterator i1 = hcc.getProperties().iterator(); i1.hasNext();) {
                    HibernateClassProperty hcp = (HibernateClassProperty) i1
                            .next();
                    values.put(HSUtil.firstLetterUpper(hcp.getName()), hcp);
                }
            }
            if (addCompositeKeyProperties) {
                if (null != getId() && getId().isComposite()) {
                    for (Iterator i = getId().getProperties().iterator(); i
                            .hasNext();) {
                        HibernateClassProperty hcp = (HibernateClassProperty) i
                                .next();
                        values.put(HSUtil.firstLetterUpper(hcp.getName()), hcp);
                    }
                }
            } else {
                if (null != getId()
                        && (!getId().isComposite() || getId()
                                .hasExternalClass())) {
                    values.put(HSUtil.firstLetterUpper(getId().getProperty()
                            .getName()), getId().getProperty());
                }
            }
            if (addCompositeKeyProperties)
                allPropertiesWithComposite = values;
            else
                allProperties = values;
        }
        return values.values();
    }

    /**
     * Return the property matching the property name
     */
    public IHibernateClassProperty getProperty(String propName) {
        if (null == propName)
            return null;
        getAllProperties();
        return (IHibernateClassProperty) allProperties.get(HSUtil
                .firstLetterUpper(propName));
    }

    /**
     * Return the property matching the column name
     */
    public IHibernateClassProperty getPropertyByColName(String colName) {
        if (null == colName)
            return null;
        if (null == allPropertiesByColumn) {
            allPropertiesByColumn = new HashMap();
            if (null != getId()) {
                if (getId().isComposite()) {
                    for (Iterator i = getId().getProperties().iterator(); i
                            .hasNext();) {
                        HibernateClassProperty hcp = (HibernateClassProperty) i
                                .next();
                        allPropertiesByColumn.put(hcp.getColumn(), hcp);
                    }
                } else {
                    HibernateClassProperty hcp = (HibernateClassProperty) getId()
                            .getProperty();
                    allPropertiesByColumn.put(hcp.getColumn(), hcp);
                }
            }
            for (Iterator i = getProperties().iterator(); i.hasNext();) {
                HibernateClassProperty hcp = (HibernateClassProperty) i.next();
                allPropertiesByColumn.put(hcp.getColumn(), hcp);
            }
            for (Iterator i = getManyToOneList().iterator(); i.hasNext();) {
                HibernateClassProperty hcp = (HibernateClassProperty) i.next();
                allPropertiesByColumn.put(hcp.getColumn(), hcp);
            }
            for (Iterator i = getOneToOneList().iterator(); i.hasNext();) {
                HibernateClassProperty hcp = (HibernateClassProperty) i.next();
                allPropertiesByColumn.put(hcp.getColumn(), hcp);
            }
            for (Iterator i = getComponentList().iterator(); i.hasNext();) {
                HibernateComponentClass hcc = (HibernateComponentClass) i
                        .next();
                for (Iterator j = hcc.getProperties().iterator(); j.hasNext();) {
                    IHibernateClassProperty hcp = (IHibernateClassProperty) j
                            .next();
                    if (null != hcp.getColumn())
                        allPropertiesByColumn.put(hcp.getColumn(), hcp);
                }
            }
            if (null != getId()) {
                if (getId().isComposite()) {
                    for (Iterator i = getId().getProperties().iterator(); i
                            .hasNext();) {
                        HibernateClassProperty hcp = (HibernateClassProperty) i
                                .next();
                        allPropertiesByColumn.put(hcp.getColumn(), hcp);
                    }
                }
            } else {
                allProperties.put(getId().getProperty().getColumn(), getId()
                        .getProperty());
            }
        }
        return (IHibernateClassProperty) allPropertiesByColumn.get(colName);
    }

    private static final String[] IP = new String[] { "name", "table",
            "discriminator", "mutable", "schema", "proxy", "dynamic-update",
            "dynamic-insert", "select-before-update", "polymorphism", "where",
            "persister", "batch-size", "optimistic-lock", "lazy",
            "class-description" };

    protected String[] getInvalidProperties() {
        return IP;
    }

    public int compareTo(Object arg0) {
        if (arg0 instanceof HibernateClass) {
            return getClassName().compareTo(
                    ((HibernateClass) arg0).getClassName());
        } else {
            return -1;
        }
    }
}