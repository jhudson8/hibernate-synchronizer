/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.obj;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.hudson.hibernatesynchronizer.util.HSUtil;

/**
 * @author <a href="mailto: joe@binamics.com">Joe Hudson </a>
 */
public class BaseElement {

    private Map properties = new Hashtable(4);

    private Map allProperties = new Hashtable(4);

    private Node node;

    private HibernateClass rootClass;

    public void set(String key, String value) {
        if (null != key && null != value) {
            String[] invalidProps = getInvalidProperties();
            boolean canSave = true;
            if (null != invalidProps) {
                for (int i = 0; i < invalidProps.length; i++) {
                    if (key.equals(invalidProps[i]))
                        canSave = false;
                }
            }
            if (canSave) {
                if (value.indexOf('\"') < 0 && value.indexOf('\n') < 0) {
                    properties.put(HSUtil.getJavaTitle(key), value);
                }
            }
            allProperties.put(HSUtil.getJavaTitle(key), value);
        }
    }

    public void clear(String key) {
        properties.remove(HSUtil.getJavaTitle(key));
        allProperties.remove(HSUtil.getJavaTitle(key));
    }

    public String get(String key) {
        if (null == key)
            return null;
        else {
            return (String) properties.get(HSUtil.getJavaTitle(key));
        }
    }

    public String getJavaDoc(String key) {
        return getJavaDoc(key, " ");
    }

    public String getJavaDoc(String key, String indent) {
        String content = get(key, true);
        if (null == content)
            return null;
        else {
            StringBuffer sb = new StringBuffer();
            StringTokenizer st = new StringTokenizer(content, "\n");
            while (st.hasMoreTokens()) {
                if (sb.length() > 0)
                    sb.append('\n');
                sb.append(indent);
                sb.append("* ");
                sb.append(st.nextToken().trim());
            }
            return sb.toString();
        }
    }

    public String get(String key, boolean force) {
        if (!force)
            return get(key);
        else {
            if (null == key)
                return null;
            else {
                return (String) allProperties.get(HSUtil.getJavaTitle(key));
            }
        }
    }

    public Map getCustomProperties() {
        return properties;
    }

    protected String[] getInvalidProperties() {
        return null;
    }

    public Node getNode() {
        return node;
    }

    protected String getNodeText(Node node) {
        for (int i = 0; i < node.getChildNodes().getLength(); i++) {
            if (node.getChildNodes().item(i).getNodeType() == Node.TEXT_NODE) {
                String text = node.getChildNodes().item(i).getNodeValue()
                        .trim();
                if (text.length() == 0)
                    return null;
                else
                    return text;
            }
        }
        return null;
    }

    protected void saveMetaData(Node node) {
        Node child = node.getFirstChild();
        Node attNode = null;
        NamedNodeMap attributes;
        while (null != child) {
            if (child.getNodeName().equals("meta")) {
                String key = null;
                String value = null;
                attNode = child.getAttributes().getNamedItem("attribute");
                if (null != attNode) {
                    key = attNode.getNodeValue();
                }
                value = getNodeText(child);
                if (null != key && null != value) {
                    set(key, value);
                }
            }
            child = child.getNextSibling();
        }
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public HibernateClass getParentRoot() {
        return rootClass;
    }

    public void setParentRoot(HibernateClass rootClass) {
        this.rootClass = rootClass;
    }

    public List getMetaData() {
        if (null == node)
            return null;
        List nodes = null;
        NodeList nl = node.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            if (nl.item(i).getNodeName().equals("meta")) {
                if (null == nodes)
                    nodes = new ArrayList();
                nodes.add(nl.item(i));
            }
        }
        return nodes;
    }
}