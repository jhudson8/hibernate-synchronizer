/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.dialog;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.hudson.hibernatesynchronizer.Plugin;
import com.hudson.hibernatesynchronizer.custom.Snippet;
import com.hudson.hibernatesynchronizer.custom.TemplateManager;

/**
 * @author <a href="mailto: joe@binamics.com">Joe Hudson </a>
 */
public class SnippetChoiceDialog extends Dialog {

    private IJavaProject project;

    private Combo snippetsCBO;

    private Snippet snippetChoice;

    private BooleanFieldEditor showCustom;

    private List allSnippets;

    private List customSnippets;

    public SnippetChoiceDialog(Shell parentShell, IJavaProject project) {
        super(parentShell);
        this.project = project;
        try {
            this.allSnippets = TemplateManager.getInstance().getSnippets(
                    project.getProject());
            this.customSnippets = TemplateManager.getInstance()
                    .getCustomSnippetsOnly(project.getProject());
        } catch (Exception ioe) {
            Plugin.logError(ioe);
        }
    }

    protected Control createDialogArea(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);
        container.setLayout(new GridLayout(2, false));

        Composite c = new Composite(container, SWT.NULL);
        c.setLayout(new FillLayout(SWT.HORIZONTAL));
        GridData gd = new GridData();
        gd.horizontalSpan = 2;
        c.setLayoutData(gd);
        showCustom = new BooleanFieldEditor("ShowCustom",
                "Show only custom snippets", c);
        showCustom.setPropertyChangeListener(new IPropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent event) {
                reloadSnippets();
            }
        });
        if (customSnippets.size() == 0)
            showCustom.setEnabled(false, c);
        showCustom.setPropertyChangeListener(new IPropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent event) {
                reloadSnippets();
            }
        });

        Label label = null;
        Text text = null;
        label = new Label(container, SWT.NULL);
        label.setText("Snippet:");
        snippetsCBO = new Combo(container, SWT.READ_ONLY);
        reloadSnippets();
        return container;
    }

    private void reloadSnippets() {
        snippetsCBO.removeAll();
        List sList = allSnippets;
        if (showCustom.getBooleanValue()) {
            sList = customSnippets;
        }
        for (Iterator i = sList.iterator(); i.hasNext();) {
            Snippet s = (Snippet) i.next();
            snippetsCBO.add(s.getName());
        }
        if (sList.size() > 0)
            snippetsCBO.select(0);
    }

    public Snippet getSnippet() {
        return snippetChoice;
    }

    protected void okPressed() {
        List sList = allSnippets;
        if (showCustom.getBooleanValue()) {
            sList = customSnippets;
        }
        try {
            List snippets = TemplateManager.getInstance().getSnippets(
                    project.getProject());
            snippetChoice = (Snippet) snippets.get(snippetsCBO
                    .getSelectionIndex());
        } catch (IOException e) {
            MessageDialog.openError(getShell(), "Error", e.getMessage());
        }
        super.okPressed();
    }
}