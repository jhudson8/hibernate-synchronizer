/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.editor;

import org.eclipse.jface.text.rules.IWhitespaceDetector;

/**
 * @author <a href="mailto: joe@binamics.com">Joe Hudson </a>
 */
public class XMLWhitespaceDetector implements IWhitespaceDetector {

    public boolean isWhitespace(char c) {
        return (c == ' ' || c == '\t' || c == '\n' || c == '\r');
    }
}