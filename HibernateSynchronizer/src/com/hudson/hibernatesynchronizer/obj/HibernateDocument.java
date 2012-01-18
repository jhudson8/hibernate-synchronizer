/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.obj;

import java.util.List;

import com.hudson.hibernatesynchronizer.parser.HibernateDOMParser;

/**
 * @author <a href="mailto: joe@binamics.com">Joe Hudson </a>
 */
public class HibernateDocument {
    private List classes;

    private List queries;

    private HibernateDOMParser parser;

    public HibernateDocument(List classes, List queries,
            HibernateDOMParser parser) {
        this.classes = classes;
        this.queries = queries;
        this.parser = parser;
    }

    public List getClasses() {
        return classes;
    }

    public List getQueries() {
        return queries;
    }

    public HibernateDOMParser getParser() {
        return parser;
    }
}