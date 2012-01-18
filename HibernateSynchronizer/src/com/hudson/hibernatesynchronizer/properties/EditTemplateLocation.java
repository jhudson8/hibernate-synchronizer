package com.hudson.hibernatesynchronizer.properties;

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
import org.eclipse.jface.preference.IPreferenceStore;
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
import com.hudson.hibernatesynchronizer.resource.TemplateLocation;

public class EditTemplateLocation extends Dialog {

	private HibernateProperties parent;
	private IProject project;
	private TemplateLocation templateLocation;

	private Text nameTXT;
	private Text locationTXT;
	private BooleanFieldEditor overwrite;
	private BooleanFieldEditor enabled;
	private Button locationSearchBTN;
	private Combo projectCBO;
	private Label locationLBL;
	private Label resourceNameLBL;
	private Label projectLBL;
	
	public EditTemplateLocation(Shell parentShell, HibernateProperties parent, IProject project, TemplateLocation templateLocation) {
		super(parentShell);
		this.parent = parent;
		this.project = project;
		this.templateLocation = templateLocation;
	}

	protected Control createDialogArea(Composite parent) {
		
		Composite composite = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout(3, false);
		composite.setLayout(layout);

		try {
			Label label = new Label(composite, SWT.NULL);
			label.setText("Template:");
			label = new Label(composite, SWT.NULL);
			label.setText(templateLocation.getTemplate().getName());
			GridData gd = new GridData();
			gd.horizontalSpan = 2;
			gd.grabExcessHorizontalSpace = true;
			label.setLayoutData(gd);


			if (templateLocation.getTemplate().isClassTemplate()) {
				projectLBL = new Label(composite, SWT.NULL);
				projectLBL.setText("Project:");
				projectCBO = new Combo(composite, SWT.READ_ONLY);
				gd = new GridData();
				gd.horizontalSpan = 2;
				projectCBO.setLayoutData(gd);
				IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
				for (int i=0; i<projects.length; i++) {
					try {
						JavaCore.create(projects[i]).getChildren();
						projectCBO.add(projects[i].getName());
						if (null != templateLocation.getOutputProject() && templateLocation.getOutputProject().getName().equals(projects[i].getName())) {
							projectCBO.select(i);
						}
					}
					catch (Exception e) {}
				}
			}

			label = new Label(composite, SWT.NULL);
			gd = new GridData();
			gd.horizontalSpan = 3;
			gd.grabExcessHorizontalSpace = true;
			label.setLayoutData(gd);
			label.setText("Tip: you can use Velocity variables in the fields below.");

			resourceNameLBL = new Label(composite, SWT.NULL);
			if (templateLocation.getTemplate().isClassTemplate()) resourceNameLBL.setText("Name:");
			else resourceNameLBL.setText("Name:");
			nameTXT = new Text(composite, SWT.BORDER);
			nameTXT.setText(templateLocation.getName());
			gd = new GridData();
			gd.horizontalSpan = 2;
			gd.widthHint = 270;
			gd.grabExcessHorizontalSpace = true;
			nameTXT.setLayoutData(gd);

			locationLBL = new Label(composite, SWT.NULL);
			if (templateLocation.getTemplate().isClassTemplate()) locationLBL.setText("Package:");
			else locationLBL.setText("Location:");
			locationTXT = new Text(composite, SWT.BORDER);
			if (templateLocation.getTemplate().isClassTemplate()) locationTXT.setText(templateLocation.getLocation());
			else locationTXT.setText(templateLocation.getOutputProjectName() + "/" + templateLocation.getLocation());
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
						if (templateLocation.getTemplate().isClassTemplate()) {
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
					catch (Exception exc) {
						exc.printStackTrace();
					}
				}

				public void widgetDefaultSelected(SelectionEvent e) {}
			});
		}
		catch (Exception e) {
			e.printStackTrace();
			MessageDialog.openError(parent.getShell(), "An error has occured", e.getMessage());
		}

		IPreferenceStore store = new PreferenceStore();
		store.setValue("TemplateOverwrite", templateLocation.shouldOverride());
		new Label(composite, SWT.NULL);
		Composite subComp = new Composite(composite, SWT.NULL);
		overwrite = new BooleanFieldEditor("TemplateOverwrite", "Overwrite if a resource/class already exists", subComp);
		overwrite.setPreferenceStore(store);
		overwrite.load();

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
			templateLocation.setName(nameTXT.getText().trim());
			if (templateLocation.getTemplate().isClassTemplate()) {
				if (templateLocation.getName().endsWith(".java")) {
					MessageDialog.openError(getParentShell(), "Validation Error", "You must not use an extension in the class name");
				}
			}

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
				ResourceManager.getInstance(project).updateTemplateLocation(templateLocation);
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