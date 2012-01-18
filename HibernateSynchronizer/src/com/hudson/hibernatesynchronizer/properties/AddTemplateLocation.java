package com.hudson.hibernatesynchronizer.properties;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.core.PackageFragment;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
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
import com.hudson.hibernatesynchronizer.resource.ResourceManager;
import com.hudson.hibernatesynchronizer.resource.Template;
import com.hudson.hibernatesynchronizer.resource.TemplateLocation;

public class AddTemplateLocation extends Dialog {

	private HibernateProperties parent;
	private IProject project;

	private Combo templatesCBO;
	private Text nameTXT;
	private Text locationTXT;
	private BooleanFieldEditor overwrite;
	private BooleanFieldEditor enabled;
	private Button locationSearchBTN;
	private Combo projectCBO;
	private Label locationLBL;
	private Label resourceNameLBL;
	private Label projectLBL;
	private PreferenceStore store = new PreferenceStore();
	
	public AddTemplateLocation(Shell parentShell, HibernateProperties parent, IProject project) {
		super(parentShell);
		this.parent = parent;
		this.project = project;
	}

	protected Control createDialogArea(Composite parent) {
		
		Composite composite = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout(3, false);
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));

		try {
			Label label = new Label(composite, SWT.NULL);
			label.setText("Template:");
			templatesCBO = new Combo(composite, SWT.READ_ONLY);
			List templates = ResourceManager.getInstance(project).getNonRequiredTemplates();
			int templateCount = 0;
			if (null != templates && templates.size() > 0) {
				for (Iterator i=templates.iterator(); i.hasNext(); ) {
					Template template = (Template) i.next();
					if (null == ResourceManager.getInstance(project).getTemplateLocation(template.getName())) {
						templateCount ++;
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
			templatesCBO.addSelectionListener(new SelectionListener () {
				public void widgetSelected(SelectionEvent e) {
					try {
						String templateName = templatesCBO.getItem(templatesCBO.getSelectionIndex());
						Template template = ResourceManager.getInstance(project).getTemplate(templateName);
						if (null != template) {
							String newLabel = null;
							String newName = null;
							if (template.isClassTemplate()) {
								newLabel = "Package:";
								newName = "Name:";
								projectLBL.setVisible(true);
								projectCBO.setVisible(true);
							}
							else {
								newLabel = "Location:";
								newName = "Name:";
								projectLBL.setVisible(false);
								projectCBO.setVisible(false);
							}
							if (!newLabel.equals(locationLBL.getText())) {
								locationTXT.setText("");
								locationLBL.setText(newLabel);
								resourceNameLBL.setText(newName);
							}
						}
					}
					catch (Exception exc) {
						exc.printStackTrace();
					}
				}

				public void widgetDefaultSelected(SelectionEvent e) {}
			});
			String templateName = templatesCBO.getItem(templatesCBO.getSelectionIndex());
			Template template = ResourceManager.getInstance(project).getTemplate(templateName);

			
			projectLBL = new Label(composite, SWT.NULL);
			projectLBL.setText("Output Project:");
			projectCBO = new Combo(composite, SWT.READ_ONLY);
			gd = new GridData();
			gd.horizontalSpan = 2;
			projectCBO.setLayoutData(gd);
			IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
			for (int i=0; i<projects.length; i++) {
				try {
					JavaCore.create(projects[i]).getChildren();
					projectCBO.add(projects[i].getName());
				}
				catch (Exception e) {}
			}
			if (projectCBO.getItemCount() > 0) projectCBO.select(0);
			if (!template.isClassTemplate()) {
				projectLBL.setVisible(false);
				projectCBO.setVisible(false);
			}

			label = new Label(composite, SWT.NULL);
			gd = new GridData();
			gd.horizontalSpan = 3;
			gd.grabExcessHorizontalSpace = true;
			label.setLayoutData(gd);
			label.setText("Tip: you can use Velocity variables in the fields below.");

			resourceNameLBL = new Label(composite, SWT.NULL);
			if (template.isClassTemplate()) resourceNameLBL.setText("Output Name:");
			else resourceNameLBL.setText("Output Name:");
			nameTXT = new Text(composite, SWT.BORDER);
			gd = new GridData();
			gd.horizontalSpan = 2;
			gd.grabExcessHorizontalSpace = true;
			gd.widthHint = 270;
			nameTXT.setLayoutData(gd);

			locationLBL = new Label(composite, SWT.NULL);
			if (template.isClassTemplate()) locationLBL.setText("Output Package:");
			else locationLBL.setText("Output Location:");
			locationTXT = new Text(composite, SWT.BORDER);
			gd = new GridData();
			gd.widthHint = 220;
			locationTXT.setLayoutData(gd);
			locationSearchBTN = new Button(composite, SWT.NATIVE);
			locationSearchBTN.setText("Browse");
			gd = new GridData();
			locationSearchBTN.setLayoutData(gd);
			locationSearchBTN.addSelectionListener(new SelectionListener () {
				public void widgetSelected(SelectionEvent e) {
					try {
						String templateName = templatesCBO.getItem(templatesCBO.getSelectionIndex());
						Template template = ResourceManager.getInstance(project).getTemplate(templateName);
						if (null != template) {
							if (template.isClassTemplate()) {
								IJavaProject javaProject = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot().getProject(projectCBO.getItem(projectCBO.getSelectionIndex())));
								SelectionDialog sd = JavaUI.createPackageDialog(getShell(), javaProject, IJavaElementSearchConstants.CONSIDER_REQUIRED_PROJECTS);
								sd.open();
								Object[] objects = sd.getResult();
								if (null != objects && objects.length > 0) {
									PackageFragment pf = (PackageFragment) objects[0];
									locationTXT.setText(pf.getElementName());
								}
							}
							else {
								ContainerSelectionDialog d = new ContainerSelectionDialog(getShell(), project, false, "Resource location selection");
								d.open();
								Object[] arr = d.getResult();
								StringBuffer sb = new StringBuffer();
								for (int i=0; i<arr.length; i++) {
									Path path = (Path) arr[i];
									for (int j=0; j<path.segments().length; j++) {
										if (j > 0) sb.append("/");
										sb.append(path.segments()[j]);
									}
									locationTXT.setText(sb.toString());
								}								
							}
						}
					}
					catch (Exception exc) {
						Plugin.log(exc);
					}
				}

				public void widgetDefaultSelected(SelectionEvent e) {}
			});
		}
		catch (Exception e) {
			Plugin.log(e);
			MessageDialog.openError(parent.getShell(), "An error has occured", e.getMessage());
		}

		new Label(composite, SWT.NULL);
		Composite subComp = new Composite(composite, SWT.NULL);
		overwrite = new BooleanFieldEditor("TemplateOverwrite", "Overwrite if a resource/class already exists", subComp);
		overwrite.setPreferenceStore(store);

		return parent;
	}

	protected void createButtonsForButtonBar(Composite parent) {
		createButton(
			parent,
			IDialogConstants.OK_ID,
			"Save",
			true);
		createButton(
			parent,
			IDialogConstants.CANCEL_ID,
			IDialogConstants.CANCEL_LABEL,
			true);
	}

	/**
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	protected void okPressed() {
		try {
			TemplateLocation templateLocation = new TemplateLocation(project);
			String templateName = templatesCBO.getItem(templatesCBO.getSelectionIndex());
			Template template = ResourceManager.getInstance(project).getTemplate(templateName);
			templateLocation.setTemplate(template);
			templateLocation.setName(nameTXT.getText().trim());
			
			String locationText = locationTXT.getText().trim();
			locationText = locationText.replace('\\', '/');
			String projectName = null;
			int index = locationText.indexOf("/");
			if (index >= 0) {
				projectName = locationText.substring(0, index);
				IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
				if (null == project) {
					if (locationText.startsWith("/")) locationText = locationText.substring(1, locationText.length());
					locationText = project.getName() + "/" + locationText;
				}
			}
			else {
				if (templateLocation.getTemplate().isClassTemplate()) {
					IJavaProject javaProject = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot().getProject(projectCBO.getItem(projectCBO.getSelectionIndex())));
					locationText = javaProject.getProject().getName() + "/" + locationText;
				}
				else locationText = project.getName() + "/" + locationText;
			}
			templateLocation.setLocation(locationText);

			templateLocation.setOverride(overwrite.getBooleanValue());
			templateLocation.setEnabled(true);
			List errors = templateLocation.validate();
			if (errors.size() == 0) {
				ResourceManager.getInstance(project).addTemplateLocation(templateLocation);
				parent.reloadTemplates();
				super.okPressed();
				this.close();
			}
			else {
				StringBuffer sb = new StringBuffer();
				for (int i=0; i<errors.size(); i++) {
					if (i > 0) sb.append("\n");
					sb.append(errors.get(i));
				}
				MessageDialog.openError(getParentShell(), "Validation Error", sb.toString());
			}
		}
		catch (Exception e) {
			Plugin.log(e);
			this.close();
		}
	}
	
	
	/**
	 * @see org.eclipse.jface.dialogs.Dialog#cancelPressed()
	 */
	protected void cancelPressed() {
		super.cancelPressed();
		this.close();
	}
}