/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.exception;

import org.w3c.dom.Node;

/**
 * @author <a href="mailto: joe@binamics.com">Joe Hudson </a>
 */
public class CompositeKeyException extends HibernateSynchronizerException {

    public CompositeKeyException(Node node, String message) {
        super(message);
        setNode(node);
    }
}