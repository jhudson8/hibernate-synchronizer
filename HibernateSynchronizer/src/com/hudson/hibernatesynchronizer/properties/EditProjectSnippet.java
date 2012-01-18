/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.properties;

import java.io.StringWriter;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.hudson.hibernatesynchronizer.Constants;
import com.hudson.hibernatesynchronizer.Plugin;
import com.hudson.hibernatesynchronizer.custom.Snippet;
import com.hudson.hibernatesynchronizer.custom.TemplateManager;

/**
 * @author <a href="mailto: joe@binamics.com">Joe Hudson </a>
 */
public class EditProjectSnippet extends Dialog {

    private HibernateProperties parent;

    private IProject project;

    private Snippet snippet;

    private Label nameLBL;

    private Text nameTXT;

    private Label descriptionLBL;

    private Text descriptionTXT;

    private StyledText content;

    public EditProjectSnippet(Shell parentShell, HibernateProperties parent,
            IProject project, Snippet snippet) {
        super(parentShell);
        this.parent = parent;
        this.project = project;
        this.snippet = snippet;
    }

    protected Control createDialogArea(Composite parent) {
        Composite composite = new Composite(parent, SWT.NULL);
        GridLayout layout = new GridLayout(2, false);
        composite.setLayout(layout);

        try {
            Composite c = new Composite(composite, SWT.NULL);
            GridData gd = new GridData();
            gd.horizontalSpan = 2;
            c.setLayoutData(gd);

            nameLBL = new Label(composite, SWT.NULL);
            nameLBL.setText("Name:");
            nameLBL.setEnabled(false);
            nameTXT = new Text(composite, SWT.BORDER);
            nameTXT.setEnabled(false);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.grabExcessHorizontalSpace = true;
            gd.widthHint = 200;
            nameTXT.setLayoutData(gd);
            nameTXT.setText(snippet.getName());
            descriptionLBL = new Label(composite, SWT.NULL);
            descriptionLBL.setText("Description:");
            descriptionTXT = new Text(composite, SWT.BORDER);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.widthHint = 300;
            gd.grabExcessHorizontalSpace = true;
            descriptionTXT.setLayoutData(gd);
            descriptionTXT.setText(snippet.getDescription());

            content = new StyledText(composite, SWT.BORDER | SWT.H_SCROLL
                    | SWT.V_SCROLL);
            gd = new GridData();
            gd.grabExcessHorizontalSpace = true;
            gd.horizontalSpan = 2;
            gd.widthHint = 440;
            gd.heightHint = 200;
            content.setLayoutData(gd);
            content.setText(snippet.getContent());
        } catch (Exception e) {
            this.close();
            MessageDialog.openError(getShell(), "An error has occured", e
                    .getMessage());
        }
        return parent;
    }

    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, 1, "Save", true);
        createButton(parent, 2, "Cancel", true);

    }

    protected void buttonPressed(int buttonId) {
        try {
            if (buttonId == 1) {
                try {
                    try {
                        Constants.customGenerator.evaluate(
                                new VelocityContext(), new StringWriter(),
                                Velocity.class.getName(), content.getText());
                    } catch (Exception e) {
                        MessageDialog.openError(getParentShell(),
                                "Velocity Error", e.getMessage());
                        return;
                    }
                    snippet.setDescription(descriptionTXT.getText().trim());
                    snippet.setContent(content.getText());
                    snippet.save(project);
                    TemplateManager.getInstance().reloadSnippets();
                    parent.reloadSnippets();
                    this.close();
                } catch (Exception e) {
                    Plugin.logError(e);
                    this.close();
                }
            } else if (buttonId == 2) {
                this.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}