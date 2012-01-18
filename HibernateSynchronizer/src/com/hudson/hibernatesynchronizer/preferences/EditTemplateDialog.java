/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.preferences;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.hudson.hibernatesynchronizer.Constants;
import com.hudson.hibernatesynchronizer.Plugin;
import com.hudson.hibernatesynchronizer.custom.ProjectTemplate;
import com.hudson.hibernatesynchronizer.custom.Template;
import com.hudson.hibernatesynchronizer.custom.TemplateManager;

/**
 * @author <a href="mailto: joe@binamics.com">Joe Hudson </a>
 */
public class EditTemplateDialog extends Dialog {

    private TemplatePreferences parent;

    private Template template;

    private Text name;

    private Combo context;

    private Text description;

    private StyledText content;

    private String previousName;

    private boolean isSnippet;

    public EditTemplateDialog(Shell parentShell, TemplatePreferences parent,
            Template template, boolean isSnippet) {
        super(parentShell);
        this.template = template;
        this.previousName = template.getName();
        this.parent = parent;
        this.isSnippet = isSnippet;
    }

    protected Control createDialogArea(Composite parent) {
        Composite composite = new Composite(parent, SWT.NULL);
        composite.setLayout(new GridLayout(3, false));

        Label label = new Label(composite, SWT.NULL);
        label.setText("Name:");
        name = new Text(composite, SWT.BORDER);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.grabExcessHorizontalSpace = true;
        if (isSnippet) {
            gd.horizontalSpan = 2;
        }
        gd.widthHint = 275;
        name.setLayoutData(gd);
        if (isSnippet)
            name.setEnabled(false);
        name.setText(template.getName());
        if (!isSnippet) {
            context = new Combo(composite, SWT.READ_ONLY);
            context.add("Java class");
            context.add("Resource");
            if (template.isJavaClass())
                context.select(0);
            else
                context.select(1);
            gd = new GridData();
            gd.horizontalAlignment = SWT.END;
            gd.grabExcessHorizontalSpace = true;
            context.setLayoutData(gd);
        }
        label = new Label(composite, SWT.NULL);
        label.setText("Description:");
        description = new Text(composite, SWT.BORDER);
        description.setText(template.getDescription());
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalSpan = 2;
        gd.widthHint = 385;
        description.setLayoutData(gd);
        content = new StyledText(composite, SWT.BORDER | SWT.H_SCROLL
                | SWT.V_SCROLL);
        gd = new GridData(GridData.FILL_BOTH);
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;
        gd.horizontalSpan = 3;
        gd.widthHint = 440;
        gd.heightHint = 400;
        content.setLayoutData(gd);
        content.setText(template.getContent());

        return parent;
    }

    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, 1, "Save", true);
        createButton(parent, 2, "Cancel", true);

    }

    public void loadTemplate() {
        template.setName(name.getText().trim());
        if (!isSnippet)
            template.setContext(context.getItem(context.getSelectionIndex()));
        template.setDescription(description.getText().trim());
        template.setContent(content.getText());
    }

    protected void buttonPressed(int buttonId) {
        try {
            if (buttonId == 1) {
                String s = content.getText().trim();
                try {
                    Constants.customGenerator.evaluate(new VelocityContext(),
                            new StringWriter(), Velocity.class.getName(), s);
                } catch (Exception e) {
                    MessageDialog.openError(getParentShell(), "Velocity Error",
                            e.getMessage());
                    return;
                }
                loadTemplate();
                try {
                    List errors = template.validate();
                    if (errors.size() == 0) {
                        template.save();
                        this.close();

                        if (!template.getName().equals(previousName)
                                && !isSnippet) {
                            IProject[] projects = ResourcesPlugin
                                    .getWorkspace().getRoot().getProjects();
                            for (int i = 0; i < projects.length; i++) {
                                ProjectTemplate pt = TemplateManager
                                        .getInstance()
                                        .findProjectTemplateByName(projects[i],
                                                previousName,
                                                template.getName());
                                if (null != pt) {
                                    TemplateManager
                                            .getInstance()
                                            .saveProjectTemplates(
                                                    projects[i],
                                                    TemplateManager
                                                            .getInstance()
                                                            .getProjectTemplates(
                                                                    projects[i],
                                                                    previousName,
                                                                    template
                                                                            .getName()));
                                }
                            }
                        }

                        if (!isSnippet) {
                            TemplateManager.getInstance().reloadTemplates();
                            Plugin.getDefault().reloadVelocityEngines();
                            parent.reloadTemplates();
                        } else {
                            TemplateManager.getInstance().reloadSnippets();
                            parent.reloadSnippets();
                        }
                    } else {
                        StringBuffer sb = new StringBuffer();
                        for (int i = 0; i < errors.size(); i++) {
                            if (i > 0)
                                sb.append("\n");
                            sb.append(errors.get(i));
                        }
                        MessageDialog.openError(getParentShell(),
                                "Validation Error", sb.toString());
                    }
                } catch (IOException e) {
                    Plugin.logError(e);
                }
            } else if (buttonId == 2) {
                this.close();
            }
        } catch (Exception e) {
        }
    }
}