/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.editor.actions;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;

import com.hudson.hibernatesynchronizer.editor.HibernateEditor;
import com.hudson.hibernatesynchronizer.editor.XMLDocumentProvider;
import com.hudson.hibernatesynchronizer.obj.HibernateDocument;
import com.hudson.hibernatesynchronizer.util.HSUtil;
import com.hudson.hibernatesynchronizer.util.Synchronizer;
import com.hudson.hibernatesynchronizer.util.XMLHelper;

/**
 * @author <a href="mailto: joe@binamics.com">Joe Hudson </a>
 */
public class FormatAction implements IEditorActionDelegate {

    private IEditorPart editorPart;

    public void setActiveEditor(IAction arg0, IEditorPart arg1) {
        arg0.setEnabled(true);
        this.editorPart = arg1;
    }

    public void run(IAction arg0) {
        if (editorPart.isDirty()) {
            HSUtil
                    .showError(
                            "You must save the document before performing this operation.",
                            new Shell());
            return;
        }
        IFileEditorInput fie = (IFileEditorInput) editorPart.getEditorInput();
        try {
            HibernateDocument hd = Synchronizer.getClasses(fie);
            StringWriter sw = new StringWriter();
            XMLHelper xmlHelper = new XMLHelper(new PrintWriter(sw));
            xmlHelper.printTree(hd.getParser().getDocument(), hd.getParser(),
                    fie.getFile().getContents());
            ((XMLDocumentProvider) ((HibernateEditor) editorPart)
                    .getDocumentProvider()).setContent(fie,
                    new ByteArrayInputStream(sw.toString().getBytes()));
        } catch (Exception e) {
            HSUtil.showError(e.getMessage(), new Shell());
        }
    }

    public void selectionChanged(IAction arg0, ISelection arg1) {
        arg0.setEnabled(true);
    }
}