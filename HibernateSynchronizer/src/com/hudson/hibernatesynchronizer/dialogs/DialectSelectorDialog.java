package com.hudson.hibernatesynchronizer.dialogs;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.hudson.hibernatesynchronizer.util.DatabaseResolver;
import com.hudson.hibernatesynchronizer.widgets.ComboEditor;

/**
 * @author Joe Hudson
 */
public class DialectSelectorDialog extends Dialog {

	private ComboEditor databaseTypes;
	private IProject project;
	private String selectedDialect;
	
	private String name;
	
	public DialectSelectorDialog(Shell shell) {
		super(shell);
	}

	/**
	 * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
	 */
	protected void configureShell(Shell newShell) {
		newShell.setText("Please select the database type");
		super.configureShell(newShell);
	}

	/**
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		container.setLayout(new GridLayout(2, false));
		
		databaseTypes = new ComboEditor(container, "DatabaseType");
		List databaseNames = DatabaseResolver.getDatabaseNames();
		String selection = null;
		for (Iterator i=databaseNames.iterator(); i.hasNext(); ) {
			String databaseType = (String) i.next();
			if (null == selection) selection = databaseType;
			databaseTypes.add(databaseType);
		}
		databaseTypes.select(selection);
		
		Label label = new Label(container, SWT.NULL);
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);
		label.setText("The following resources must be in your project classpath\n" +
					"\t- cglib\n" +
					"\t- commons-collections\n" +
					"\t- commons-logging\n" +
					"\t- dom4j\n" +
					"\t- odmg\n" +
					"\t- hibernate");
		
		return container;
	}
	
	/**
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	protected void okPressed() {
		selectedDialect = DatabaseResolver.getInstance().resolve(databaseTypes.getSelection());
	   super.okPressed();
	}
	
	public String getDialect () {
	    return selectedDialect;
	}
}