/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.exception;

/**
 * @author <a href="mailto: joe@binamics.com">Joe Hudson </a>
 */
public class TransientPropertyException extends HibernateSynchronizerException {

    /**
     *  
     */
    public TransientPropertyException() {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * @param arg0
     */
    public TransientPropertyException(String arg0) {
        super(arg0);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param arg0
     * @param arg1
     */
    public TransientPropertyException(String arg0, Throwable arg1) {
        super(arg0, arg1);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param arg0
     */
    public TransientPropertyException(Throwable arg0) {
        super(arg0);
        // TODO Auto-generated constructor stub
    }
}