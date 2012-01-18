/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.parser;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.hudson.hibernatesynchronizer.obj.HibernateClass;
import com.hudson.hibernatesynchronizer.obj.HibernateDocument;
import com.hudson.hibernatesynchronizer.obj.HibernateQuery;

/**
 * @author <a href="mailto: joe@binamics.com">Joe Hudson </a>
 */
public class DOMHelper {

    public static HibernateDocument getHibernateDocument(
            HibernateDOMParser parser, IProject project) {
        Document doc = parser.getDocument();
        Node node = doc.getDocumentElement();
        String packageName = null;
        if (null != node) {
            NamedNodeMap attributes = node.getAttributes();
            if (null != attributes) {
                Node attNode = attributes.getNamedItem("package");
                if (null != attNode) {
                    packageName = attNode.getNodeValue();
                    if (packageName.endsWith(".")) {
                        packageName = packageName.substring(0, packageName
                                .length() - 1);
                    }
                }
            }
        }
        NodeList nl = doc.getElementsByTagName("class");
        List classes = new ArrayList();
        for (int j = 0; j < nl.getLength(); j++) {
            node = nl.item(j);
            classes.add(new HibernateClass(node, packageName, project));
        }
        NodeList queries = doc.getElementsByTagName("query");
        List queryList = new ArrayList();
        if (queries.getLength() > 0) {
            for (int k = 0; k < queries.getLength(); k++) {
                Node query = queries.item(k);
                queryList.add(new HibernateQuery(query,
                        (HibernateClass) classes.get(0)));
            }
        }
        if (classes.size() == 1) {
            ((HibernateClass) classes.get(0)).setQueries(queryList);
        }
        return new HibernateDocument(classes, queryList, parser);
    }
}