package com.hudson.hibernatesynchronizer.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.hudson.hibernatesynchronizer.Constants;
import com.hudson.hibernatesynchronizer.Plugin;
import com.hudson.hibernatesynchronizer.resource.ResourceManager;
import com.hudson.hibernatesynchronizer.util.UIUtil;
import com.hudson.hibernatesynchronizer.widgets.ResourceListTable;
import com.hudson.hibernatesynchronizer.widgets.TemplateAddUpdateDeleteHandler;
import com.hudson.hibernatesynchronizer.widgets.TemplateImportExportHandler;

/**
 * @author Joe Hudson
 */
public class TemplatePreferences
	extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage {

	public TemplatePreferences() {
		super(GRID);
		setPreferenceStore(Plugin.getDefault().getPreferenceStore());
		setDescription("Templates are used to generate custom classes/resources within your workspace when hibernate mapping files are modified.");
		initializeDefaults();
	}

	public void init(IWorkbench workbench) {
	}

	/**
	 * Initialize the default values
	 */
	private void initializeDefaults() {
		IPreferenceStore store = getPreferenceStore();
		store.setDefault(Constants.PROP_CUSTOM_TEMPLATES_ENABLED, true);
	}


	/**
	 * Creates the field editors. Field editors are abstractions of
	 * the common GUI blocks needed to manipulate various types
	 * of preferences. Each field editor knows how to save and
	 * restore itself.
	 */
	public void createFieldEditors() {
		Composite composite = new Composite(getFieldEditorParent(), SWT.NONE);
		composite.setLayout(new GridLayout(1, false));
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		new ResourceListTable(
				composite,
				ResourceManager.getNonRequiredWorkspaceTemplates(),
				new TemplateAddUpdateDeleteHandler(),
				new TemplateImportExportHandler(),
				1,
				getShell());
		addField(
				new BooleanFieldEditor(
						Constants.PROP_CUSTOM_TEMPLATES_ENABLED,
						UIUtil.getResourceText("EnableCustomTemplateGeneration"),
						getFieldEditorParent()));
	}
}