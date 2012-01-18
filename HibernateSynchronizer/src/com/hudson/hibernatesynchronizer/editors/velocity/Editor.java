package com.hudson.hibernatesynchronizer.editors.velocity;

import java.io.StringWriter;
import java.util.Map;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.texteditor.ContentAssistAction;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;

import com.hudson.hibernatesynchronizer.Constants;
import com.hudson.hibernatesynchronizer.Plugin;
import com.hudson.hibernatesynchronizer.resource.Resource;
import com.hudson.hibernatesynchronizer.resource.ResourceEditorInput;

public class Editor extends TextEditor implements IPropertyChangeListener, KeyListener {

	private ColorManager colorManager;
	private com.hudson.hibernatesynchronizer.widgets.TextEditor descriptionText;
	private Map customProperties;
	private boolean m_isDirty = false;

	public Editor() {
		super();
		colorManager = new ColorManager();
		setSourceViewerConfiguration(new Configuration(colorManager, this));
		setDocumentProvider(new DocumentProvider(this));
	}
	public void dispose() {
		colorManager.dispose();
		super.dispose();
	}

	public String getResourceDescription () {
		return descriptionText.getText().getText();
	}
	
	public ITextViewer getViewer () {
		return getSourceViewer();
	}
	
	public Map getCustomProperties () {
		return customProperties;
	}

	protected void createActions() {
		super.createActions();
		// Add content assist propsal action
		ContentAssistAction action = new ContentAssistAction(
				Plugin.getDefault().getResourceBundle(),
				"VelocityEditor.ContentAssist", this);
		action.setActionDefinitionId(
				ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
				setAction("Velocity.ContentAssist", action);
		action.setEnabled(true);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.AbstractTextEditor#doSetInput(org.eclipse.ui.IEditorInput)
	 */
	protected void doSetInput(IEditorInput input) throws CoreException {
		super.doSetInput(input);
	}

	public boolean isDirty () {
		return (super.isDirty() || m_isDirty);
	}

	public void doSave(IProgressMonitor progressMonitor) {
		String contents = getSourceViewer().getDocument().get();
		try {
			Constants.templateGenerator.evaluate(new VelocityContext(), new StringWriter(), Velocity.class.getName(), contents);
		}
		catch (Exception e) {
			int lineNumber = 0;
			int index = e.getMessage().indexOf("at line ");
			if (index >= 0) {
				int newIndex = index + 8;
				StringBuffer sb = new StringBuffer();
				char c = e.getMessage().charAt(newIndex++);
				while (Character.isDigit(c)) {
					sb.append(c);
					c = e.getMessage().charAt(newIndex++);
				}
				if (sb.length() > 0) {
					lineNumber = Integer.parseInt(sb.toString());
				}
			}
			int colNumber = 0;
			MessageDialog.openError(getSourceViewer().getTextWidget().getShell(), "Parsing Error", e.getMessage());
			return;
		}
		super.doSave(progressMonitor);
	}

	/**
	 * Return the project associated with this resource or null if a workspace resource
	 */
	public IProject getProject () {
		if (getEditorInput() instanceof ResourceEditorInput) {
			if (null != ((ResourceEditorInput) getEditorInput()).getIFile()) {
				return ((ResourceEditorInput) getEditorInput()).getIFile().getProject();
			}
		}
		return null;
	}

	/**
	 * Return the resouce associated with this editor
	 */
	public Resource getResource () {
		if (getEditorInput() instanceof ResourceEditorInput) {
			return((ResourceEditorInput) getEditorInput()).getResource();
		}
		return null;
	}

	/**
	 * @see org.eclipse.ui.IWorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createPartControl(Composite parent) {
		Composite fParent= new Composite(parent, SWT.NONE);
		GridLayout gridLayout = new GridLayout(1, false);
		gridLayout.horizontalSpacing = 0;
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		fParent.setLayout(gridLayout);

		if (getEditorInput() instanceof ResourceEditorInput) {
			Composite sub1 = new Composite(fParent, SWT.NONE);
			sub1.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
			sub1.setLayout(new GridLayout(2, false));
			descriptionText = new com.hudson.hibernatesynchronizer.widgets.TextEditor(sub1, "Description", "");
			descriptionText.getText().addKeyListener(this);
			if (null != getResource().getDescription()) descriptionText.getText().setText(getResource().getDescription());
			customProperties = getResource().addToEditor(sub1, this);
			
			Composite sub2 = new Composite(fParent, SWT.NONE);
			sub2.setLayout(new FillLayout(SWT.VERTICAL));
			GridData gridData = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
			sub2.setLayoutData(gridData);
			super.createPartControl(sub2);
		}
		else {
			Label label = new Label(fParent, SWT.NULL);
			label.setText("You must use the Hibernate Synchronizer menu to open a snippet or template");
		}
	}

	/**
	 * @see org.eclipse.ui.texteditor.AbstractTextEditor#editorSaved()
	 */
	protected void editorSaved() {
		m_isDirty = false;
		super.editorSaved();
	}

	/**
	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
		m_isDirty = true;
		firePropertyChange(PROP_DIRTY);
	}

	/**
	 * @see org.eclipse.swt.events.KeyListener#keyPressed(org.eclipse.swt.events.KeyEvent)
	 */
	public void keyPressed(KeyEvent e) {
	}
	/**
	 * @see org.eclipse.swt.events.KeyListener#keyReleased(org.eclipse.swt.events.KeyEvent)
	 */
	public void keyReleased(KeyEvent e) {
		m_isDirty = true;
		firePropertyChange(PROP_DIRTY);
	}
}