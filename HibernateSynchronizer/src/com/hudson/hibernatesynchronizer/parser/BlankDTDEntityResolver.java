/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.parser;

import java.io.ByteArrayInputStream;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

/**
 * @author <a href="mailto: joe@binamics.com">Joe Hudson </a>
 */
public class BlankDTDEntityResolver implements EntityResolver {

    public InputSource resolveEntity(String publicId, String systemId) {
        return new InputSource(new ByteArrayInputStream("".getBytes()));
    }
}