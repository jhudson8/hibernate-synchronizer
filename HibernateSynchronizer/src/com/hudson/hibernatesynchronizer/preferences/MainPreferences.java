package com.hudson.hibernatesynchronizer.preferences;

import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.graphics.Color;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.hudson.hibernatesynchronizer.Constants;
import com.hudson.hibernatesynchronizer.Plugin;
import com.hudson.hibernatesynchronizer.editors.velocity.ColorManager;
import com.hudson.hibernatesynchronizer.util.UIUtil;

/**
 * @author Joe Hudson
 */
public class MainPreferences
	extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage {

	public MainPreferences() {
		super(GRID);
		setPreferenceStore(Plugin.getDefault().getPreferenceStore());
		initializeDefaults();
	}

	public void init(IWorkbench workbench) {
	}

	/**
	 * Initialize the default values
	 */
	private void initializeDefaults() {
		IPreferenceStore store = getPreferenceStore();
		ColorManager velocityColorManager = new ColorManager();
		store.setDefault(Constants.PROP_CUSTOM_TEMPLATES_ENABLED, true);
		store.setDefault(ColorManager.COLOR_FOREACH_DIRECTIVE,
				getDefaultStoreValue(velocityColorManager.getDefaultColor(ColorManager.COLOR_FOREACH_DIRECTIVE)));
		store.setDefault(ColorManager.COLOR_IF_DIRECTIVE,
				getDefaultStoreValue(velocityColorManager.getDefaultColor(ColorManager.COLOR_IF_DIRECTIVE)));
		store.setDefault(ColorManager.COLOR_SET_DIRECTIVE,
				getDefaultStoreValue(velocityColorManager.getDefaultColor(ColorManager.COLOR_SET_DIRECTIVE)));
		store.setDefault(ColorManager.COLOR_VARIABLE,
				getDefaultStoreValue(velocityColorManager.getDefaultColor(ColorManager.COLOR_VARIABLE)));
		store.setDefault(ColorManager.COLOR_COMMENT,
				getDefaultStoreValue(velocityColorManager.getDefaultColor(ColorManager.COLOR_COMMENT)));

		com.hudson.hibernatesynchronizer.editors.synchronizer.ColorManager xmlColorManager = new com.hudson.hibernatesynchronizer.editors.synchronizer.ColorManager();
		store.setDefault(com.hudson.hibernatesynchronizer.editors.synchronizer.ColorManager.COLOR_DEFAULT,
				getDefaultStoreValue(xmlColorManager.getDefaultColor(com.hudson.hibernatesynchronizer.editors.synchronizer.ColorManager.COLOR_DEFAULT)));
		store.setDefault(com.hudson.hibernatesynchronizer.editors.synchronizer.ColorManager.COLOR_PROC_INSTR,
				getDefaultStoreValue(xmlColorManager.getDefaultColor(com.hudson.hibernatesynchronizer.editors.synchronizer.ColorManager.COLOR_PROC_INSTR)));
		store.setDefault(com.hudson.hibernatesynchronizer.editors.synchronizer.ColorManager.COLOR_STRING,
				getDefaultStoreValue(xmlColorManager.getDefaultColor(com.hudson.hibernatesynchronizer.editors.synchronizer.ColorManager.COLOR_STRING)));
		store.setDefault(com.hudson.hibernatesynchronizer.editors.synchronizer.ColorManager.COLOR_TAG,
				getDefaultStoreValue(xmlColorManager.getDefaultColor(com.hudson.hibernatesynchronizer.editors.synchronizer.ColorManager.COLOR_TAG)));
		store.setDefault(com.hudson.hibernatesynchronizer.editors.synchronizer.ColorManager.COLOR_XML_COMMENT,
				getDefaultStoreValue(xmlColorManager.getDefaultColor(com.hudson.hibernatesynchronizer.editors.synchronizer.ColorManager.COLOR_XML_COMMENT)));
		store.setDefault(com.hudson.hibernatesynchronizer.editors.synchronizer.ColorManager.COLOR_LINK,
				getDefaultStoreValue(xmlColorManager.getDefaultColor(com.hudson.hibernatesynchronizer.editors.synchronizer.ColorManager.COLOR_LINK)));
	}


	/**
	 * Creates the field editors. Field editors are abstractions of
	 * the common GUI blocks needed to manipulate various types
	 * of preferences. Each field editor knows how to save and
	 * restore itself.
	 */
	public void createFieldEditors() {
		addField(new ColorFieldEditor(ColorManager.COLOR_FOREACH_DIRECTIVE, UIUtil.getResourceLabel("ForeachDirective"), getFieldEditorParent()));
		addField(new ColorFieldEditor(ColorManager.COLOR_IF_DIRECTIVE, UIUtil.getResourceLabel("IfDirective"), getFieldEditorParent()));
		addField(new ColorFieldEditor(ColorManager.COLOR_SET_DIRECTIVE, UIUtil.getResourceLabel("SetDirective"), getFieldEditorParent()));
		addField(new ColorFieldEditor(ColorManager.COLOR_VARIABLE, UIUtil.getResourceLabel("VelocityVariable"), getFieldEditorParent()));
		addField(new ColorFieldEditor(ColorManager.COLOR_COMMENT, UIUtil.getResourceLabel("VelocityComment"), getFieldEditorParent()));

		addField(new ColorFieldEditor(com.hudson.hibernatesynchronizer.editors.synchronizer.ColorManager.COLOR_PROC_INSTR, UIUtil.getResourceLabel("XMLInstruction"), getFieldEditorParent()));
		addField(new ColorFieldEditor(com.hudson.hibernatesynchronizer.editors.synchronizer.ColorManager.COLOR_STRING, UIUtil.getResourceLabel("XMLString"), getFieldEditorParent()));
		addField(new ColorFieldEditor(com.hudson.hibernatesynchronizer.editors.synchronizer.ColorManager.COLOR_TAG, UIUtil.getResourceLabel("XMLTag"), getFieldEditorParent()));
		addField(new ColorFieldEditor(com.hudson.hibernatesynchronizer.editors.synchronizer.ColorManager.COLOR_XML_COMMENT, UIUtil.getResourceLabel("XMLComment"), getFieldEditorParent()));
		addField(new ColorFieldEditor(com.hudson.hibernatesynchronizer.editors.synchronizer.ColorManager.COLOR_LINK, UIUtil.getResourceLabel("XMLClassLink"), getFieldEditorParent()));
	}

	private String getDefaultStoreValue (Color color) {
		if (null == color) return "0,0,0";
		else return color.getRed() + "," + color.getGreen() + "," + color.getBlue();
	}
}