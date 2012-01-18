/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.outline;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.w3c.dom.Node;

import com.hudson.hibernatesynchronizer.editor.HibernateEditor;
import com.hudson.hibernatesynchronizer.obj.BaseElement;

/**
 * @author <a href="mailto: joe@binamics.com">Joe Hudson </a>
 */
public class HibernateOutlineDoubleClickListener implements
        IDoubleClickListener {
    private HibernateOutlinePage outlinePage;

    public HibernateOutlineDoubleClickListener(HibernateOutlinePage outlinePage) {
        this.outlinePage = outlinePage;
    }

    public void doubleClick(DoubleClickEvent event) {
        Object selectedElement = getSelectedElement(event.getSelection());
        if (selectedElement instanceof BaseElement) {
            selectLine(((BaseElement) selectedElement).getNode());
        }
    }

    private void selectLine(Node node) {
        IWorkbenchPage workbenchPage = outlinePage.getSite().getPage();
        if (workbenchPage.getActiveEditor() instanceof HibernateEditor) {
            HibernateEditor editor = (HibernateEditor) workbenchPage
                    .getActiveEditor();
            Integer lineNumber = outlinePage.getContentProvider().getDocument()
                    .getParser().getLineNumber(node);
            if (null != lineNumber) {
                InputStream is = null;
                try {
                    is = ((IFileEditorInput) editor.getEditorInput()).getFile()
                            .getContents();
                    char compChar = '\n';
                    int currentLineNumber = 1;
                    int index = 0;
                    while (currentLineNumber < lineNumber.intValue()) {
                        int i = is.read();
                        if (i == -1)
                            break;
                        char c = (char) i;
                        index++;
                        if (c == compChar)
                            currentLineNumber++;
                    }
                    int endIndex = index;
                    while (true) {
                        int i = is.read();
                        if (i == -1)
                            break;
                        else {
                            char c = (char) i;
                            endIndex++;
                            if (c == compChar)
                                break;
                        }
                    }
                    ITextSelection selection = (ITextSelection) editor
                            .getSelectionProvider().getSelection();
                    if (!selection.isEmpty()) {
                        editor.selectAndReveal(index, endIndex - index);
                    }
                } catch (Exception e) {
                    if (null != is)
                        try {
                            is.close();
                        } catch (IOException ioe) {
                        }
                }
            }
        }
    }

    private Object getSelectedElement(ISelection selection) {
        Object element = null;
        if (selection instanceof IStructuredSelection) {
            element = ((IStructuredSelection) selection).getFirstElement();
        }
        return element;
    }
}