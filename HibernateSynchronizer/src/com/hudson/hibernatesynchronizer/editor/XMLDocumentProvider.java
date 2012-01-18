/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.editor;

import java.io.InputStream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.DefaultPartitioner;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.editors.text.FileDocumentProvider;

/**
 * @author <a href="mailto: joe@binamics.com">Joe Hudson </a>
 */
public class XMLDocumentProvider extends FileDocumentProvider {

    protected IDocument createDocument(Object element) throws CoreException {
        IDocument document = super.createDocument(element);
        if (document != null) {
            IDocumentPartitioner partitioner = new DefaultPartitioner(
                    new XMLPartitionScanner(), new String[] {
                            XMLPartitionScanner.XML_TAG,
                            XMLPartitionScanner.XML_COMMENT });
            partitioner.connect(document);
            document.setDocumentPartitioner(partitioner);
        }
        return document;
    }

    public void setContent(IEditorInput fie, InputStream is)
            throws CoreException {
        IDocument document = createEmptyDocument();
        setDocumentContent(document, is, null);
        doSaveDocument(null, fie, document, true);
    }
}