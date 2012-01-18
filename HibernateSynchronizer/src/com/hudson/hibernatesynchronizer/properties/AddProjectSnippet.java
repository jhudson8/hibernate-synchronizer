package com.hudson.hibernatesynchronizer.properties;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.hudson.hibernatesynchronizer.Plugin;
import com.hudson.hibernatesynchronizer.resource.ResourceManager;
import com.hudson.hibernatesynchronizer.resource.Snippet;
import com.hudson.hibernatesynchronizer.util.EditorUtil;
import com.hudson.hibernatesynchronizer.util.UIUtil;

public class AddProjectSnippet extends Dialog {

	private HibernateProperties parent;
	private IProject project;

	private Label snippetsLBL;
	private Combo snippetsCBO;
	private Label nameLBL;
	private Text nameTXT;
	private BooleanFieldEditor override;
	
	public AddProjectSnippet(Shell parentShell, HibernateProperties parent, IProject project) {
		super(parentShell);
		this.parent = parent;
		this.project = project;
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
			override = new BooleanFieldEditor("override", "Override an existing snippet", c);
			override.setPropertyChangeListener(new IPropertyChangeListener () {
				public void propertyChange(PropertyChangeEvent event) {
					if (override.getBooleanValue()) {
						snippetsLBL.setVisible(true);
						snippetsCBO.setVisible(true);
						nameLBL.setEnabled(false);
						nameTXT.setEnabled(false);
						resetFromCombo();
					}
					else {
						snippetsLBL.setVisible(false);
						snippetsCBO.setVisible(false);
						nameLBL.setEnabled(true);
						nameTXT.setEnabled(true);
					}
				}
			});
			
			snippetsLBL = new Label(composite, SWT.NULL);
			snippetsLBL.setText("Snippets");
			snippetsLBL.setVisible(false);
			snippetsCBO = new Combo(composite, SWT.READ_ONLY);
			for (Iterator i=ResourceManager.getInstance(project).getSnippets().iterator(); i.hasNext(); ) {
				Snippet s = (Snippet) i.next();
				snippetsCBO.add(s.getName());
			}
			snippetsCBO.select(0);
			snippetsCBO.setVisible(false);
			snippetsCBO.addSelectionListener(new SelectionListener () {
				public void widgetSelected(SelectionEvent e) {
					resetFromCombo();
				}
				public void widgetDefaultSelected(SelectionEvent e){}
			});
			
			nameLBL = new Label(composite, SWT.NULL);
			nameLBL.setText("Name:");
			nameTXT = new Text(composite, SWT.BORDER);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.grabExcessHorizontalSpace = true;
			gd.widthHint = 200;
			nameTXT.setLayoutData(gd);
		}
		catch (Exception e) {
			this.close();
			MessageDialog.openError(getShell(), "An error has occured", e.getMessage());
		}
		return parent;
	}
	
	public void resetFromCombo() {
		int index = snippetsCBO.getSelectionIndex();
		if (index < 0) return;
		Snippet s = (Snippet) ResourceManager.getInstance(project).getSnippets().get(index);
		nameTXT.setText(s.getName());
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
			String name = nameTXT.getText().trim();
			if (name.length() == 0) {
				MessageDialog.openError(getParentShell(), "Configuration error", "The snippet name must be supplied");
				return;
			}
			else {
				List projectSnippets = ResourceManager.getInstance(project).getProjectSnippets();
				boolean found = false;
				for (Iterator i=projectSnippets.iterator(); i.hasNext(); ) {
					Snippet s = (Snippet) i.next();
					if (s.getName().equals(name)) {
						found = true;
						break;
					}
				}
				if (found) {
					MessageDialog.openError(getParentShell(), "Configuration error", "You must choose a name that does not already exist in your project");
					return;
				}
				if (!override.getBooleanValue()) {
					if (null != ResourceManager.getInstance(project).getSnippet(name)) {
						MessageDialog.openError(getParentShell(), "Configuration error", "You must choose a name that does not already exist");
						return;
					}
				}
				Snippet s = new Snippet();
				s.setProject(project);
				s.setName(name);
				if (override.getBooleanValue()) {
					Snippet sTemp = ResourceManager.getInstance(project).getSnippet(name);
					s.setDescription(sTemp.getDescription());
					s.setContent(sTemp.getContent());
				}
				ResourceManager.getInstance(project).save(s);
				EditorUtil.openPage(s, getShell());
				parent.reloadSnippets();
				MessageDialog.openInformation(getShell(), UIUtil.getResourceTitle("ResourceModification"), UIUtil.getResourceText("ResourceInEditor"));
				super.okPressed();
				this.close();
			}
		}
		catch (Exception e) {
			Plugin.log(e);
			this.close();
		}
	}

}