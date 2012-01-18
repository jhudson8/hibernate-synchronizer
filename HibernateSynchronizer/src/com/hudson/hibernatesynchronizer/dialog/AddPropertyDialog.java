/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.dialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.FileEditorInput;

import com.hudson.hibernatesynchronizer.editor.CursorState;
import com.hudson.hibernatesynchronizer.editor.HibernateEditor;
import com.hudson.hibernatesynchronizer.editor.NodeAttribute;
import com.hudson.hibernatesynchronizer.util.HSUtil;

/**
 * @author <a href="mailto: joe@binamics.com">Joe Hudson </a>
 */
public class AddPropertyDialog extends Dialog {

    private HibernateEditor editor;

    private Properties properties = new Properties();

    private String nodeName;

    private String[] attributes;

    private boolean fKey;

    private Text column;

    private Text name;

    public AddPropertyDialog(Shell parentShell, HibernateEditor editor,
            String nodeName, String[] attributes, boolean fKey) {
        super(parentShell);
        this.editor = editor;
        this.nodeName = nodeName;
        this.attributes = attributes;
        this.fKey = fKey;
    }

    protected Control createDialogArea(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);
        container.setLayout(new GridLayout(2, false));

        Label label = null;
        Text text = null;
        label = new Label(container, SWT.NULL);
        label.setText("column:");
        column = new Text(container, SWT.BORDER | SWT.SINGLE);
        column.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent event) {
                String text = ((Text) event.widget).getText();
                if (null != text) {
                    properties.put("column", text);
                    if (fKey) {
                        String propName = HSUtil.firstLetterLower(HSUtil
                                .getPropName(text));
                        String fkPropName = null;
                        if (propName.endsWith("Id")) {
                            fkPropName = propName.substring(0, propName
                                    .length() - 2);
                        } else if (propName.endsWith("Oid")) {
                            fkPropName = propName.substring(0, propName
                                    .length() - 3);
                        } else
                            fkPropName = propName;
                        name.setText(fkPropName);
                    } else {
                        name.setText(HSUtil.firstLetterLower(HSUtil
                                .getPropName(text)));
                    }
                } else {
                    properties.remove("column");
                    name.setText("");
                }

            }
        });
        GridData gd = new GridData();
        gd.widthHint = 120;
        column.setLayoutData(gd);

        label = new Label(container, SWT.NULL);
        label.setText("name:");
        name = new Text(container, SWT.BORDER | SWT.SINGLE);
        name.addModifyListener(new TextModifyListener("name", properties));
        gd = new GridData();
        gd.widthHint = 120;
        name.setLayoutData(gd);

        loadFields(container, nodeName, attributes);

        return container;
    }

    private void loadFields(Composite container, String nodeName,
            String[] attributes) {
        Label label = null;
        for (int i = 0; i < attributes.length; i++) {
            String attribute = attributes[i];
            NodeAttribute na = new NodeAttribute(nodeName, attribute);
            String[] suggestions = (String[]) CursorState.valueSuggestions
                    .get(na);
            if (null == suggestions) {
                if (null != CursorState.classSuggestions.get(na)) {
                    suggestions = editor
                            .getAllClassChoices(((FileEditorInput) editor
                                    .getEditorInput()).getFile().getProject());
                }
            }
            label = new Label(container, SWT.NULL);
            label.setText(attribute + ":");
            if (null != suggestions) {

                List values = new ArrayList(suggestions.length);
                for (int j = 0; j < suggestions.length; j++) {
                    String suggestion = suggestions[j];
                    values.add(suggestion);
                }
                Collections.sort(values);

                Combo combo = new Combo(container, SWT.READ_ONLY | SWT.SINGLE);
                if (!attribute.equals("type") && !attribute.equals("class")) {
                    combo.add("");
                }
                for (int j = 0; j < values.size(); j++) {
                    combo.add((String) values.get(j));
                }
                combo.select(0);
                combo.addModifyListener(new ComboModifyListener(attribute,
                        properties));
            } else {
                Text text = new Text(container, SWT.READ_ONLY | SWT.SINGLE);
                text.addModifyListener(new TextModifyListener(attribute,
                        properties));
                GridData gd = new GridData();
                gd.widthHint = 120;
                text.setLayoutData(gd);
            }
        }
    }

    public class TextModifyListener implements ModifyListener {
        private String attributeName;

        private Properties props;

        public TextModifyListener(String attributeName, Properties props) {
            this.attributeName = attributeName;
            this.props = props;
        }

        public void modifyText(ModifyEvent event) {
            String text = ((Text) event.widget).getText();
            if (null != text) {
                props.put(attributeName, text);
            } else {
                properties.remove(attributeName);
            }
        }
    }

    public class ComboModifyListener implements ModifyListener {
        private String attributeName;

        private Properties props;

        public ComboModifyListener(String attributeName, Properties props) {
            this.attributeName = attributeName;
            this.props = props;
        }

        public void modifyText(ModifyEvent event) {
            Combo combo = (Combo) event.widget;
            // String text = combo.getItem(combo.getSelectionIndex());
            String text = combo.getText();
            if (null != text) {
                props.put(attributeName, text);
            } else {
                properties.remove(attributeName);
            }
        }
    }

    protected void okPressed() {
        if (null == name.getText() || name.getText().length() == 0) {
            showError("You must enter a name");
            return;
        }
        super.okPressed();
    }

    protected void showError(String error) {
        MessageDialog.openError(new Shell(), "Error", error);
    }

    public Properties getProperties() {
        return properties;
    }
}