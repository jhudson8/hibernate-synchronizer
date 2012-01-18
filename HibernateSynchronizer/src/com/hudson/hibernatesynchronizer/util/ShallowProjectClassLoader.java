/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.util;

import java.net.URL;
import java.net.URLClassLoader;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

/**
 * @author <a href="mailto: joe@binamics.com">Joe Hudson </a>
 */
public class ShallowProjectClassLoader extends URLClassLoader {

    public ShallowProjectClassLoader(IJavaProject project, ClassLoader parent)
            throws JavaModelException {
        super(getURLSFromProject(project), parent);
    }

    private static URL[] getURLSFromProject(IJavaProject project)
            throws JavaModelException {
        IPackageFragmentRoot[] roots = project.getAllPackageFragmentRoots();
        URL[] urls = new URL[roots.length];
        for (int i = 0; i < roots.length; i++) {
            try {
                if (!roots[i].isArchive()) {
                    IPath path = roots[i].getJavaProject().getOutputLocation();
                    String binPath = path.toOSString();
                    binPath = binPath.replace('\\', '/');
                    path = roots[i].getResource().getLocation();
                    String actualPath = path.toOSString();
                    actualPath = actualPath.replace('\\', '/');
                    int index = binPath.indexOf("/", 1);
                    if (index > 0) {
                        actualPath = actualPath
                                + binPath.substring(index, binPath.length());
                    }
                    if (!actualPath.endsWith("/"))
                        actualPath = actualPath + "/";
                    urls[i] = new URL("file://" + actualPath);
                }
            } catch (Exception e) {
            }
        }
        return urls;
    }
}