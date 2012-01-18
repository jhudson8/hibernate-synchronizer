/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.editor;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.BasicTextEditorActionContributor;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.RetargetTextEditorAction;

import com.hudson.hibernatesynchronizer.Plugin;

/**
 * @author <a href="mailto: joe@binamics.com">Joe Hudson </a>
 */
public class HibernateContributor extends BasicTextEditorActionContributor {

    private RetargetTextEditorAction fContentAssist;

    /**
     * Defines the menu actions and their action handlers.
     */
    public HibernateContributor() {
        createActions();
    }

    protected void createActions() {
        fContentAssist = new RetargetTextEditorAction(Plugin.getDefault()
                .getResourceBundle(), HibernateEditor.PREFIX + "ContentAssist.");
    }

    /**
     * Sets the active editor to the actions provided by this contributor.
     * 
     * @param aPart
     *            the editor
     */
    private void doSetActiveEditor(IEditorPart aPart) {
        // Set the underlying action (registered by the according editor) in
        // the action handlers
        ITextEditor editor = null;
        if (aPart instanceof ITextEditor) {
            editor = (ITextEditor) aPart;
        }
        fContentAssist.setAction(getAction(editor, Plugin.CONTENT_ASSIST));
    }
}