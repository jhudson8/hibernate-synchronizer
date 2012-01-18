/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.exception;

import org.w3c.dom.Node;

/**
 * @author <a href="mailto: joe@binamics.com">Joe Hudson </a>
 */
public class AttributeNotSpecifiedException extends
        HibernateSynchronizerException {

    private String attribute;

    public AttributeNotSpecifiedException(Node node, String attribute) {
        setNode(node);
        this.attribute = attribute;
    }

    public String getAttribute() {
        return attribute;
    }

    public String getMessage() {
        return "the \"" + attribute + "\" attribute is required for the \""
                + getNode().getLocalName() + "\" node";
    }
}