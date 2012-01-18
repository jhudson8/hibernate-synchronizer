package com.hudson.hibernatesynchronizer.preferences;

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
import com.hudson.hibernatesynchronizer.widgets.DummyFieldEditor;
import com.hudson.hibernatesynchronizer.widgets.ResourceListTree;
import com.hudson.hibernatesynchronizer.widgets.SnippetAddUpdateDeleteHandler;
import com.hudson.hibernatesynchronizer.widgets.SnippetImportExportHandler;
import com.hudson.hibernatesynchronizer.widgets.SnippetTreeViewerContentProvider;
import com.hudson.hibernatesynchronizer.widgets.SnippetTreeViewerLabelProvider;

/**
 * @author Joe Hudson
 */
public class SnippetPreferences
	extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage {

	public SnippetPreferences() {
		super(GRID);
		setPreferenceStore(Plugin.getDefault().getPreferenceStore());
		setDescription("Snippets are small pieces of code that can be referenced from templates.\n" +
				"Snippets created at the workspace level can be overridden at the project level.");
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
		new ResourceListTree(
				composite,
				new SnippetTreeViewerContentProvider(),
				new SnippetTreeViewerLabelProvider(),
				ResourceManager.getWorkspaceSnippets(),
				new SnippetAddUpdateDeleteHandler(),
				new SnippetImportExportHandler(),
				1,
				getShell());

		addField(new DummyFieldEditor());
	}
}