/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.parser;

import java.io.File;
import java.io.FileInputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.JavaCore;

import com.hudson.hibernatesynchronizer.editor.HibernateEditor;
import com.hudson.hibernatesynchronizer.obj.HibernateDocument;
import com.hudson.hibernatesynchronizer.util.Synchronizer;

/**
 * @author <a href="mailto: joe@binamics.com">Joe Hudson </a>
 */
public class ParserThread extends Thread {
    private HibernateEditor editor;

    private IFile currentFile;

    public ParserThread(HibernateEditor editor, IFile currentFile) {
        this.editor = editor;
        this.currentFile = currentFile;
    }

    public void run() {
        try {
            IPath path = currentFile.getParent().getLocation();
            File[] files = path.toFile().listFiles();
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                try {
                    if (editor.shouldReload(file)) {
                        HibernateDocument doc = Synchronizer.getClasses(
                                new FileInputStream(file), JavaCore
                                        .create(currentFile.getProject()));
                        HibernateEditor.cache(currentFile.getProject(), file,
                                doc.getClasses());
                    }
                } catch (Exception e) {
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}