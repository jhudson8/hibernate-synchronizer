/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.exception;

import org.w3c.dom.Node;

/**
 * @author <a href="mailto: joe@binamics.com">Joe Hudson </a>
 */
public abstract class HibernateSynchronizerException extends RuntimeException {

    private Node node;

    int lineNumber = 0;

    private Throwable cause;

    public HibernateSynchronizerException() {
        super();
    }

    public HibernateSynchronizerException(String arg0) {
        super(arg0);
    }

    public HibernateSynchronizerException(String arg0, Throwable arg1) {
        super(arg0);
        this.cause = arg1;
    }

    public HibernateSynchronizerException(Throwable arg0) {
        this.cause = arg0;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public Node getNode() {
        return node;
    }

    protected void setNode(Node node) {
        this.node = node;
    }

    protected void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public Throwable getCause() {
        return cause;
    }
}