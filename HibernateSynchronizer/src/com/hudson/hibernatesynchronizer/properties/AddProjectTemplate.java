/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.properties;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.core.PackageFragment;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.swt.SWT;
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

import com.hudson.hibernatesynchronizer.Plugin;
import com.hudson.hibernatesynchronizer.custom.ProjectTemplate;
import com.hudson.hibernatesynchronizer.custom.Template;
import com.hudson.hibernatesynchronizer.custom.TemplateManager;

/**
 * @author <a href="mailto: joe@binamics.com">Joe Hudson </a>
 */
public class AddProjectTemplate extends Dialog {

    private HibernateProperties parent;

    private IProject project;

    private Combo templatesCBO;

    private Text nameTXT;

    private Text locationTXT;

    private BooleanFieldEditor overwrite;

    private BooleanFieldEditor enabled;

    private Button locationSearchBTN;

    private Label locationLBL;

    private Label resourceNameLBL;

    private PreferenceStore store = new PreferenceStore();

    public AddProjectTemplate(Shell parentShell, HibernateProperties parent,
            IProject project) {
        super(parentShell);
        this.parent = parent;
        this.project = project;
    }

    protected Control createDialogArea(Composite parent) {

        Composite composite = new Composite(parent, SWT.NULL);
        GridLayout layout = new GridLayout(3, false);
        composite.setLayout(layout);

        try {
            Label label = new Label(composite, SWT.NULL);
            label.setText("Template:");
            templatesCBO = new Combo(composite, SWT.READ_ONLY);
            List templates = TemplateManager.getInstance().getTemplates();
            int templateCount = 0;
            if (null != templates && templates.size() > 0) {
                for (Iterator i = templates.iterator(); i.hasNext();) {
                    Template template = (Template) i.next();
                    if (null == TemplateManager.getInstance()
                            .findProjectTemplate(project, template.getId())) {
                        templateCount++;
                        templatesCBO.add(template.getName());
                    }
                }
                if (templateCount > 0) {
                    templatesCBO.select(0);
                }
            }
            GridData gd = new GridData();
            gd.horizontalSpan = 2;
            gd.grabExcessHorizontalSpace = true;
            templatesCBO.setLayoutData(gd);
            templatesCBO.addSelectionListener(new SelectionListener() {
                public void widgetSelected(SelectionEvent e) {
                    try {
                        String templateName = templatesCBO.getItem(templatesCBO
                                .getSelectionIndex());
                        Template template = TemplateManager.getInstance()
                                .findTemplateByName(templateName);
                        if (null != template) {
                            String newLabel = null;
                            String newName = null;
                            if (template.isJavaClass()) {
                                newLabel = "Package:";
                                newName = "Name:";
                            } else {
                                newLabel = "Location:";
                                newName = "Name:";
                            }
                            if (!newLabel.equals(locationLBL.getText())) {
                                locationTXT.setText("");
                                locationLBL.setText(newLabel);
                                resourceNameLBL.setText(newName);
                            }
                        }
                    } catch (Exception exc) {
                        exc.printStackTrace();
                    }
                }

                public void widgetDefaultSelected(SelectionEvent e) {
                }
            });
            String templateName = templatesCBO.getItem(templatesCBO
                    .getSelectionIndex());
            Template template = TemplateManager.getInstance()
                    .findTemplateByName(templateName);

            label = new Label(composite, SWT.NULL);
            gd = new GridData();
            gd.horizontalSpan = 3;
            gd.grabExcessHorizontalSpace = true;
            label.setLayoutData(gd);
            label
                    .setText("Tip: you can use Velocity variables in the fields below.");

            resourceNameLBL = new Label(composite, SWT.NULL);
            if (template.isJavaClass())
                resourceNameLBL.setText("Name:");
            else
                resourceNameLBL.setText("Name:");
            nameTXT = new Text(composite, SWT.BORDER);
            gd = new GridData();
            gd.horizontalSpan = 2;
            gd.grabExcessHorizontalSpace = true;
            gd.widthHint = 200;
            nameTXT.setLayoutData(gd);
            nameTXT.setLayoutData(gd);

            locationLBL = new Label(composite, SWT.NULL);
            if (template.isJavaClass())
                locationLBL.setText("Package:");
            else
                locationLBL.setText("Location:");
            locationTXT = new Text(composite, SWT.BORDER);
            gd = new GridData();
            gd.widthHint = 200;
            locationTXT.setLayoutData(gd);
            locationSearchBTN = new Button(composite, SWT.NATIVE);
            locationSearchBTN.setText("Browse");
            gd = new GridData();
            locationSearchBTN.setLayoutData(gd);
            locationSearchBTN.addSelectionListener(new SelectionListener() {
                public void widgetSelected(SelectionEvent e) {
                    try {
                        String templateName = templatesCBO.getItem(templatesCBO
                                .getSelectionIndex());
                        Template template = TemplateManager.getInstance()
                                .findTemplateByName(templateName);
                        if (null != template) {
                            if (template.isJavaClass()) {
                                IJavaProject javaProject = JavaCore
                                        .create(project);
                                SelectionDialog sd = JavaUI
                                        .createPackageDialog(
                                                getShell(),
                                                javaProject,
                                                IJavaElementSearchConstants.CONSIDER_REQUIRED_PROJECTS);
                                sd.open();
                                Object[] objects = sd.getResult();
                                if (null != objects && objects.length > 0) {
                                    PackageFragment pf = (PackageFragment) objects[0];
                                    locationTXT.setText(pf.getElementName());
                                }
                            } else {
                                ContainerSelectionDialog d = new ContainerSelectionDialog(
                                        getShell(), project, false,
                                        "Resource location selection");
                                d.open();
                                Object[] arr = d.getResult();
                                StringBuffer sb = new StringBuffer();
                                for (int i = 0; i < arr.length; i++) {
                                    Path path = (Path) arr[i];
                                    for (int j = 0; j < path.segments().length; j++) {
                                        if (j == 0) {
                                            if (!path.segments()[j]
                                                    .equals(project.getName())) {
                                                MessageDialog
                                                        .openError(
                                                                getParentShell(),
                                                                "Location Error",
                                                                "You may only choose a location in the current project");
                                                return;
                                            }
                                        } else {
                                            sb.append("/");
                                            sb.append(path.segments()[j]);
                                        }
                                    }
                                    locationTXT.setText(sb.toString());
                                }
                            }
                        }
                    } catch (Exception exc) {
                        exc.printStackTrace();
                    }
                }

                public void widgetDefaultSelected(SelectionEvent e) {
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            MessageDialog.openError(parent.getShell(), "An error has occured",
                    e.getMessage());
        }

        new Label(composite, SWT.NULL);
        Composite subComp = new Composite(composite, SWT.NULL);
        overwrite = new BooleanFieldEditor("TemplateOverwrite",
                "Overwrite if a resource/class already exists", subComp);
        overwrite.setPreferenceStore(store);

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
                    ProjectTemplate projectTemplate = new ProjectTemplate();
                    String templateName = templatesCBO.getItem(templatesCBO
                            .getSelectionIndex());
                    Template template = TemplateManager.getInstance()
                            .findTemplateByName(templateName);
                    projectTemplate.setTemplate(template);
                    projectTemplate.setName(nameTXT.getText().trim());
                    projectTemplate.setLocation(locationTXT.getText().trim());
                    projectTemplate.setOverride(overwrite.getBooleanValue());
                    projectTemplate.setEnabled(true);
                    List errors = projectTemplate.validate();
                    if (errors.size() == 0) {
                        List projectTemplates = TemplateManager.getInstance()
                                .getProjectTemplates(project);
                        projectTemplates.add(projectTemplate);
                        TemplateManager.getInstance().saveProjectTemplates(
                                project, projectTemplates);
                        parent.reloadTemplates();
                        this.close();
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