/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.dialog;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.core.PackageFragment;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.eclipse.ui.dialogs.SelectionDialog;

import com.hudson.hibernatesynchronizer.custom.Template;
import com.hudson.hibernatesynchronizer.custom.TemplateManager;

/**
 * @author <a href="mailto: joe@binamics.com">Joe Hudson </a>
 */
public class TemplateGenerationDialog extends Dialog {

    private List templates;

    private IJavaProject project;

    private TemplateGenerationParameter param;

    private Text containerText;

    private Text fileNameText;

    private Combo templatesCBO;

    private Label containerLabel;

    private Label fileNameLabel;

    private Template template;

    public TemplateGenerationDialog(Shell parentShell, IJavaProject project,
            List templates, TemplateGenerationParameter param) {
        super(parentShell);
        this.project = project;
        this.templates = templates;
        this.param = param;
    }

    protected Control createDialogArea(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);
        container.setLayout(new GridLayout(3, false));

        Label label = null;
        try {
            label = new Label(container, SWT.NULL);
            label.setText("Template:");
            templatesCBO = new Combo(container, SWT.READ_ONLY);
            List templates = TemplateManager.getInstance().getTemplates();
            if (null != templates && templates.size() > 0) {
                for (Iterator i = templates.iterator(); i.hasNext();) {
                    Template template = (Template) i.next();
                    templatesCBO.add(template.getName());
                }
                templatesCBO.select(0);
            }
            templatesCBO.addSelectionListener(new SelectionListener() {
                public void widgetSelected(SelectionEvent e) {
                    dialogChanged();
                }

                public void widgetDefaultSelected(SelectionEvent e) {
                }
            });
            new Label(container, SWT.NULL);
        } catch (Exception e) {
        }

        containerLabel = new Label(container, SWT.NULL);

        containerText = new Text(container, SWT.BORDER | SWT.SINGLE);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.widthHint = 150;
        containerText.setLayoutData(gd);
        containerText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                dialogChanged();
            }
        });

        Button button = new Button(container, SWT.PUSH);
        button.setText("Browse");
        button.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                handleBrowse();
            }
        });
        fileNameLabel = new Label(container, SWT.NULL);

        fileNameText = new Text(container, SWT.BORDER | SWT.SINGLE);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.widthHint = 150;
        fileNameText.setLayoutData(gd);
        fileNameText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                dialogChanged();
            }
        });
        dialogChanged();

        return container;
    }

    private void dialogChanged() {
        try {
            String templateName = templatesCBO.getItem(templatesCBO
                    .getSelectionIndex());
            template = TemplateManager.getInstance().findTemplateByName(
                    templateName);
            if (null != template) {
                if (template.isJavaClass()) {
                    containerLabel.setText("&Package:  ");
                    fileNameLabel.setText("&Class name:");
                } else {
                    containerLabel.setText("&Container:");
                    fileNameLabel.setText("&File name:   ");
                }
            }
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

    private void handleBrowse() {

        try {
            String templateName = templatesCBO.getItem(templatesCBO
                    .getSelectionIndex());
            template = TemplateManager.getInstance().findTemplateByName(
                    templateName);
            if (null != template) {
                if (template.isJavaClass()) {
                    SelectionDialog sd = JavaUI
                            .createPackageDialog(
                                    getShell(),
                                    project,
                                    IJavaElementSearchConstants.CONSIDER_REQUIRED_PROJECTS);
                    sd.open();
                    Object[] objects = sd.getResult();
                    if (null != objects && objects.length > 0) {
                        PackageFragment pf = (PackageFragment) objects[0];
                        containerText.setText(pf.getElementName());
                    }
                } else {
                    ContainerSelectionDialog dialog = new ContainerSelectionDialog(
                            getShell(), ResourcesPlugin.getWorkspace()
                                    .getRoot(), false,
                            "Select new file container");
                    if (dialog.open() == ContainerSelectionDialog.OK) {
                        Object[] result = dialog.getResult();
                        if (result.length == 1) {
                            containerText.setText(((IPath) result[0])
                                    .toOSString());
                        }
                    }
                }
            }
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

    protected void okPressed() {
        try {
            String templateName = templatesCBO.getItem(templatesCBO
                    .getSelectionIndex());
            template = TemplateManager.getInstance().findTemplateByName(
                    templateName);
            if (null != template) {
                if (template.isJavaClass()) {
                    if (containerText.getText().indexOf("/") >= 0
                            || containerText.getText().indexOf("\\") >= 0) {
                        if (containerText.getText() == null)
                            containerText.setText("");
                        showError("The package is invalid");
                        return;
                    } else if (null == fileNameText.getText()
                            || fileNameText.getText().trim().length() == 0
                            || fileNameText.getText().indexOf(".") >= 0) {
                        showError("The class name is invalid");
                    }
                } else {
                    if (null == fileNameText.getText()
                            || fileNameText.getText().trim().length() == 0) {
                        showError("The file name is invalid");
                        return;
                    }
                    if (null == containerText.getText()
                            || containerText.getText().trim().length() == 0
                            || containerText.getText().indexOf(".") >= 0) {
                        containerText.setText(File.separator
                                + project.getProject().getName());
                    }
                }
            } else {
                showError("You must select a template");
                return;
            }
            param.template = template;
            param.container = containerText.getText();
            param.fileName = fileNameText.getText();
            super.okPressed();
        } catch (Exception e) {
            super.cancelPressed();
        }
    }

    protected void showError(String error) {
        MessageDialog.openError(new Shell(), "Error", error);
    }
}