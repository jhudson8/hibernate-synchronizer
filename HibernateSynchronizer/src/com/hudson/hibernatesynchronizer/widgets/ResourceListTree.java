package com.hudson.hibernatesynchronizer.widgets;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import com.hudson.hibernatesynchronizer.resource.Resource;
import com.hudson.hibernatesynchronizer.util.UIUtil;



public class ResourceListTree implements ISelectionChangedListener, MouseListener, KeyListener  {

	private Composite parent;
	private ITreeContentProvider treeContentProvider;
	private IBaseLabelProvider labelProvider;
	private List resources;
	private AddUpdateDeleteHandler addUpdateDeleteHandler;
	private ImportExportHandler importExportHandler;
	private int colspan;
	private Shell shell;

	// cache
	private TreeViewer treeViewer;
	private Button editButton;
	private Button deleteButton;
	private Button resetButton;
	private Button importButton;
	private Button exportButton;
	private Button selectAllButton;
	private Button selectNoneButton;

	public ResourceListTree (
			Composite parent,
			ITreeContentProvider treeContentProvider,
			IBaseLabelProvider labelProvider,
			List resources,
			AddUpdateDeleteHandler addUpdateDeleteHandler,
			ImportExportHandler importExportHandler,
			int colspan,
			Shell shell) {
		this.parent = parent;
		this.treeContentProvider = treeContentProvider;
		this.labelProvider = labelProvider;
		this.resources = resources;
		this.addUpdateDeleteHandler = addUpdateDeleteHandler;
		this.importExportHandler = importExportHandler;
		this.colspan = colspan;
		this.shell = shell;
		draw();
	}

	private void draw() {
		Composite composite = new Composite(parent, SWT.NULL);
		int numColumns = 1;
		if (null != importExportHandler) numColumns = 2;
		GridLayout gl = new GridLayout(numColumns, false);
		composite.setLayout(gl);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.grabExcessHorizontalSpace = true;
		gd.grabExcessVerticalSpace = true;
		gd.horizontalSpan = colspan;
		composite.setLayoutData(gd);

		treeViewer = new TreeViewer(composite);
		treeViewer.setContentProvider(treeContentProvider);
		treeViewer.setLabelProvider(labelProvider);
		GridData data = new GridData (GridData.FILL_BOTH);
		data.grabExcessHorizontalSpace = true;
		data.grabExcessVerticalSpace = true;
		data.heightHint = 150;
		treeViewer.getControl().setLayoutData(data);
		treeViewer.addSelectionChangedListener(this);
		treeViewer.getControl().addKeyListener(this);
		treeViewer.getControl().addMouseListener(this);

		if (null != importExportHandler) {
			Composite buttonComposite = new Composite(composite, SWT.NULL);
			data = new GridData ();
			data.horizontalAlignment = GridData.CENTER;
			data.verticalAlignment = GridData.BEGINNING;
			buttonComposite.setLayoutData(data);
			FillLayout fl = new FillLayout();
			fl.type = SWT.VERTICAL;
			fl.spacing = 2;
			buttonComposite.setLayout(fl);
			importButton = new Button(buttonComposite, SWT.NATIVE);
			importButton.setText(UIUtil.getResourceLabel("Import"));
			importButton.addSelectionListener(new SelectionListener () {
				public void widgetSelected(SelectionEvent e) {
					try {
						resources = importExportHandler.importResources(null, shell);
						reloadTree();
					}
					catch (Exception exc) {
						UIUtil.pluginError(exc, shell);
					}
				}
				public void widgetDefaultSelected(SelectionEvent e) {}
			});
			exportButton = new Button(buttonComposite, SWT.NATIVE);
			exportButton.setText(UIUtil.getResourceLabel("Export"));
			exportButton.setEnabled(false);
			exportButton.addSelectionListener(new SelectionListener () {
				public void widgetSelected(SelectionEvent e) {
					List expResources = new ArrayList();
					IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();
					for (Iterator i=selection.iterator(); i.hasNext(); ) {
						expResources.add(i.next());
					}
					try {
						importExportHandler.exportResources(expResources, null, shell);
					}
					catch (Exception exc) {
						UIUtil.pluginError(exc, shell);
					}
				}
				public void widgetDefaultSelected(SelectionEvent e) {}
			});
		}

		Composite buttonComposite = new Composite(composite, SWT.NULL);
		data = new GridData ();
		data.horizontalAlignment = GridData.BEGINNING;
		data.verticalAlignment = GridData.BEGINNING;
		data.horizontalSpan = 2;
		buttonComposite.setLayoutData(data);
		FillLayout fl = new FillLayout();
		fl.type = SWT.HORIZONTAL;
		fl.spacing = 2;
		buttonComposite.setLayout(fl);
		Button button = new Button(buttonComposite, SWT.NATIVE);
		button.setText(UIUtil.getResourceLabel("New"));
		button.addSelectionListener(new AddButtonListener());
		editButton = new Button(buttonComposite, SWT.NATIVE);
		editButton.setText(UIUtil.getResourceLabel("Edit"));
		editButton.setEnabled(false);
		editButton.addSelectionListener(new EditButtonListener());
		deleteButton = new Button(buttonComposite, SWT.NATIVE);
		deleteButton.setText(UIUtil.getResourceLabel("Delete"));
		deleteButton.setEnabled(false);
		deleteButton.addSelectionListener(new DeleteButtonListener());
		resetButton = new Button(buttonComposite, SWT.NATIVE);
		resetButton.setText(UIUtil.getResourceLabel("Reset"));
		resetButton.setEnabled(false);
		resetButton.addSelectionListener(new ResetButtonListener());
		
		reloadTree();
	}

	private void reloadTree () {
		editButton.setEnabled(false);
		deleteButton.setEnabled(false);
		if (null != selectAllButton) selectAllButton.setEnabled(false);
		if (null != selectNoneButton) selectNoneButton.setEnabled(false);
		if (null != exportButton) exportButton.setEnabled(false);
		
		treeViewer.setInput(resources);
		treeViewer.refresh();
	}

	/**
	 * Return all selected resources
	 * @return
	 */
	public List getSelection () {
		List rtnList = new ArrayList();
		if (treeViewer.getSelection() instanceof IStructuredSelection) {
			IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();
			for (Iterator i=selection.iterator(); i.hasNext(); ) {
				Object obj = i.next();
				if (obj instanceof Resource) {
					rtnList.add(obj);
				}
			}
		}
		return rtnList;
	}

	// LISTENERS
	/**
	 * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
	 */
	public void selectionChanged(SelectionChangedEvent event) {
		List selection = getSelection();
		if (null != importExportHandler) {
			if (selection.size() > 0) {
				exportButton.setEnabled(true);
				deleteButton.setEnabled(true);
			}
			else {
				exportButton.setEnabled(false);
				deleteButton.setEnabled(false);
			}
		}
		if (selection.size() == 1) {
			Resource resource = (Resource) selection.get(0);
			try {
				if (addUpdateDeleteHandler.canDelete(resource))
					deleteButton.setEnabled(true);
				else
					deleteButton.setEnabled(false);
			}
			catch (Exception e1) {
				deleteButton.setEnabled(false);
			}
			editButton.setEnabled(true);
			try {
				if (addUpdateDeleteHandler.canRestore(resource))
					resetButton.setEnabled(true);
				else
					resetButton.setEnabled(false);
			}
			catch (Exception e1) {
				resetButton.setEnabled(false);
			}
		}
		else {
			editButton.setEnabled(false);
			deleteButton.setEnabled(false);
			resetButton.setEnabled(false);
		}
	}

	public void mouseDoubleClick(MouseEvent e) {
		List selectedSnippets = getSelection();
		if (selectedSnippets.size() == 1) {
			try {
				Resource resource = (Resource) selectedSnippets.get(0);
				resources = addUpdateDeleteHandler.updateResource(resource, shell);
			}
			catch (Exception exc) {
				UIUtil.pluginError(exc, shell);
			}
		}
	}
	public void mouseDown(MouseEvent e) {};
	public void mouseUp(MouseEvent e) {};

	public void keyPressed(KeyEvent e) {
		if (e.keyCode == SWT.DEL) {
			List selectedSnippets = getSelection();
			if (selectedSnippets.size() == 1) {
				try {
					Resource resource = (Resource) selectedSnippets.get(0);
					if (addUpdateDeleteHandler.canDelete(resource)) {
						if (UIUtil.confirm("DeleteResource", shell)) {
							resources = addUpdateDeleteHandler.deleteResource(resource, shell);
							reloadTree();
						}
					}
				}
				catch (Exception exc) {
					UIUtil.pluginError(exc, shell);
				}
			}
		}
	}
	public void keyReleased(KeyEvent e) {}

	public class AddButtonListener implements SelectionListener {
		public void widgetSelected(SelectionEvent e) {
			try {
				resources = addUpdateDeleteHandler.addResource(shell);
				reloadTree();
			}
			catch (Exception exc) {
				UIUtil.pluginError(exc, shell);
			}
		}
		public void widgetDefaultSelected(SelectionEvent e) {
		}
	}

	public class EditButtonListener implements SelectionListener {
		public void widgetSelected(SelectionEvent e) {
			List selectedSnippets = getSelection();
			if (selectedSnippets.size() == 1) {
				try {
					Resource resource = (Resource) selectedSnippets.get(0);
					resources = addUpdateDeleteHandler.updateResource(resource, shell);
				}
				catch (Exception exc) {
					UIUtil.pluginError(exc, shell);
				}
			}
		}
		public void widgetDefaultSelected(SelectionEvent e) {
		}
	}

	public class DeleteButtonListener implements SelectionListener {
		public void widgetSelected(SelectionEvent e) {
			List selectedSnippets = getSelection();
			if (selectedSnippets.size() == 1) {
				try {
					Resource resource = (Resource) selectedSnippets.get(0);
					if (addUpdateDeleteHandler.canDelete(resource)) {
						if (UIUtil.confirm("DeleteResource", shell)) {
							resources = addUpdateDeleteHandler.deleteResource(resource, shell);
							reloadTree();
						}
					}
				}
				catch (Exception exc) {
					UIUtil.pluginError(exc, shell);
				}
			}
		}
		public void widgetDefaultSelected(SelectionEvent e) {
		}
	}

	public class ResetButtonListener implements SelectionListener {
		public void widgetSelected(SelectionEvent e) {
			List selectedSnippets = getSelection();
			if (selectedSnippets.size() == 1) {
				try {
					Resource resource = (Resource) selectedSnippets.get(0);
					resources = addUpdateDeleteHandler.restore(resource, shell);
					reloadTree();
				}
				catch (Exception exc) {
					UIUtil.pluginError(exc, shell);
				}
			}
		}
		public void widgetDefaultSelected(SelectionEvent e) {
		}
	}
}