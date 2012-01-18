package com.hudson.hibernatesynchronizer.properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.hudson.hibernatesynchronizer.Plugin;
import com.hudson.hibernatesynchronizer.resource.ResourceManager;
import com.hudson.hibernatesynchronizer.util.HSUtil;

public class EditTemplateParameter extends Dialog {

	private HibernateProperties parent;
	private IProject project;
	private String key;
	
	private Text keyText;
	private Text valueText;
	
	public EditTemplateParameter(Shell parentShell, HibernateProperties parent, IProject project, String key) {
		super(parentShell);
		this.parent = parent;
		this.project = project;
		this.key = key;
	}

	protected Control createDialogArea(Composite parent) {
		
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout(2, false));

		Label label = new Label(composite, SWT.NULL);
		label.setText("Name:");
		keyText = new Text(composite, SWT.BORDER);
		keyText.setText(key);
		keyText.setEnabled(false);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 200;
		keyText.setLayoutData(gd);

		label = new Label(composite, SWT.NULL);
		label.setText("Value:");
		valueText = new Text(composite, SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 200;
		valueText.setLayoutData(gd);
		valueText.setText(ResourceManager.getInstance(project).getTemplateParameter(key));

		return parent;
	}

	protected void createButtonsForButtonBar(Composite parent) {
		createButton(
			parent,
			1,
			"Save",
			true);
		createButton(
			parent,
			2,
			"Cancel",
			true);

	}

	protected void buttonPressed(int buttonId) {
		try {
			if (buttonId == 1) {
				try {
					String key = keyText.getText().trim();
					if (key.length() == 0) {
						HSUtil.showError("The parameter name must not be null", getShell());
						return;
					}
					if (key.indexOf(' ') > 0) {
						HSUtil.showError("The parameter name must not contain spaces", getShell());
						return;
					}
					String value = valueText.getText().trim();
					if (value.length() == 0) {
						HSUtil.showError("The value must not be null", getShell());
						return;
					}
					ResourceManager.getInstance(project).updateTemplateParameter(key, value);
					parent.reloadTemplateParameters();
					this.close();
				}
				catch (Exception e) {
					Plugin.log(e);
					this.close();
				}
			}
			else if (buttonId == 2) {
				this.close();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

}