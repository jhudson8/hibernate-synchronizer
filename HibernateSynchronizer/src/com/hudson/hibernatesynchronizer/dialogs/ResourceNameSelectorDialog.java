package com.hudson.hibernatesynchronizer.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.hudson.hibernatesynchronizer.util.UIUtil;
import com.hudson.hibernatesynchronizer.widgets.TextEditor;

/**
 * @author Joe Hudson
 */
public class ResourceNameSelectorDialog extends Dialog {

	private String labelRef;
	private TextEditor nameEditor;
	
	private String name;
	
	public ResourceNameSelectorDialog(String labelRef, Shell shell) {
		super(shell);
		this.labelRef = labelRef;
	}

	/**
	 * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
	 */
	protected void configureShell(Shell newShell) {
		newShell.setText("Please enter the resource name");
		super.configureShell(newShell);
	}

	/**
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		container.setLayout(new GridLayout(2, false));
		
		nameEditor = new TextEditor(container, labelRef, 200, null);
		
		return container;
	}
	
	/**
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	protected void okPressed() {
		name = nameEditor.getText().getText().trim();
		boolean valid = true;
		// we want only characters or numbers
		for (int i=0; i<name.length(); i++) {
			if (!Character.isLetterOrDigit(name.charAt(i))) {
				valid = false;
				break;
			}
		}
		if (!valid) UIUtil.validationError("ResourceNameAlphaDigit", getShell());
		if (name.length() == 0) {
			valid = false;
			UIUtil.validationError("ResourceNameNoLength", getShell());
		}
		if (valid) super.okPressed();
	}

	public String getName () {
		return name;
	}
}