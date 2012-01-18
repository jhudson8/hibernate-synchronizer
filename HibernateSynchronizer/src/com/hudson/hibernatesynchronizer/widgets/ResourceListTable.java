package com.hudson.hibernatesynchronizer.widgets;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import com.hudson.hibernatesynchronizer.resource.Resource;
import com.hudson.hibernatesynchronizer.util.UIUtil;



public class ResourceListTable implements SelectionListener, MouseListener, KeyListener {

	private Composite parent;
	private List resources;
	private AddUpdateDeleteHandler addUpdateDeleteHandler;
	private ImportExportHandler importExportHandler;
	private int colspan;
	private Shell shell;

	// cache
	private Table table;
	private Button editButton;
	private Button deleteButton;
	private Button resetButton;
	private Button importButton;
	private Button exportButton;
	private Button selectAllButton;
	private Button selectNoneButton;

	public ResourceListTable (
			Composite parent,
			List resources,
			AddUpdateDeleteHandler addUpdateDeleteHandler,
			ImportExportHandler importExportHandler,
			int colspan,
			Shell shell) {
		this.parent = parent;
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

		int tableType = SWT.BORDER | SWT.V_SCROLL | SWT.CHECK | SWT.FULL_SELECTION;
		if (null == importExportHandler) tableType = SWT.BORDER | SWT.V_SCROLL | SWT.FULL_SELECTION;
		table = new Table(composite, tableType);
		table.setVisible(true);
		table.setLinesVisible (false);
		table.setHeaderVisible(false);
		table.addSelectionListener(this);
		table.addMouseListener(this);
		table.addKeyListener(this);
		GridData data = new GridData (GridData.FILL_BOTH);
		data.grabExcessHorizontalSpace = true;
		data.grabExcessVerticalSpace = true;
		data.heightHint = 150;
		table.setLayoutData(data);

		// create the column
//		TableColumn nameColumn = new TableColumn(table, SWT.LEFT);
//		nameColumn.setText(UIUtil.getResourceLabel("Name"));
//		ColumnLayoutData nameColumnLayout = new ColumnWeightData(100, false);

		// set columns in Table layout
//		TableLayout tableLayout = new TableLayout();
//		tableLayout.addColumnData(nameColumnLayout);
//		table.setLayout(tableLayout);
//		table.layout();

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
						reloadTable();
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
					for (int i=0; i<table.getItemCount(); i++) {
						if (table.getItem(i).getChecked()) {
							expResources.add(resources.get(i));
							table.getItem(i).setChecked(false);
						}
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

			new Label(buttonComposite, SWT.NULL);
			selectAllButton = new Button(buttonComposite, SWT.NATIVE);
			selectAllButton.setText(UIUtil.getResourceLabel("SelectAll"));
			selectAllButton.addSelectionListener(new SelectionListener () {
				public void widgetSelected(SelectionEvent e) {
					TableItem[] items = table.getItems();
					for (int i=0; i<items.length; i++) {
						items[i].setChecked(true);
					}
					if (items.length > 0) {
						deleteButton.setEnabled(true);
						exportButton.setEnabled(true);
					}
					else {
						deleteButton.setEnabled(false);
						exportButton.setEnabled(false);
					}
				}
				public void widgetDefaultSelected(SelectionEvent e) {}
			});
			selectNoneButton = new Button(buttonComposite, SWT.NATIVE);
			selectNoneButton.setText(UIUtil.getResourceLabel("SelectNone"));
			selectNoneButton.addSelectionListener(new SelectionListener () {
				public void widgetSelected(SelectionEvent e) {
					exportButton.setEnabled(false);
					TableItem[] items = table.getItems();
					for (int i=0; i<items.length; i++) {
						items[i].setChecked(false);
					}
					if (table.getSelectionCount() == 0) {
						deleteButton.setEnabled(false);
					}
					else {
						deleteButton.setEnabled(true);
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
		
		reloadTable();
	}
	
	private void reloadTable () {
		editButton.setEnabled(false);
		deleteButton.setEnabled(false);
		if (null != selectAllButton) selectAllButton.setEnabled(false);
		if (null != selectNoneButton) selectNoneButton.setEnabled(false);
		if (null != exportButton) exportButton.setEnabled(false);
		table.removeAll();
		try {
			for (Iterator i=resources.iterator(); i.hasNext(); ) {
				Resource resource = (Resource) i.next();
				TableItem item = new TableItem(table, SWT.NULL);
				item.setText(new String[]{resource.getName()});
			}
			if (table.getItemCount() == 0) {
				if (null != selectAllButton) selectAllButton.setEnabled(false);
				if (null != selectNoneButton) selectNoneButton.setEnabled(false);
			}
			else {
				if (null != selectAllButton) selectAllButton.setEnabled(true);
				if (null != selectNoneButton) selectNoneButton.setEnabled(true);
			}
			table.redraw();
		}
		catch (Exception e) {
			UIUtil.pluginError(e, shell);
		}
	}

	// LISTENERS
	public void widgetSelected(SelectionEvent e) {
		int count = 0;
		for (int i=0; i<table.getItems().length; i++) {
			if (table.getItems()[i].getChecked()) count++;
		}
		if (null != importExportHandler) {
			if (count > 0) {
				exportButton.setEnabled(true);
				deleteButton.setEnabled(true);
			}
			else {
				exportButton.setEnabled(false);
				deleteButton.setEnabled(false);
			}
		}
		if (table.getSelectionCount() == 1) {
			int index = table.getSelectionIndex();
			Resource resource = (Resource) resources.get(index);
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
	public void widgetDefaultSelected(SelectionEvent e) {}

	public void mouseDoubleClick(MouseEvent e) {
		int index = table.getSelectionIndex();
		if (index >= 0) {
			try {
				Resource resource = (Resource) resources.get(index);
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
			int index = table.getSelectionIndex();
			if (index >= 0) {
				try {
					Resource resource = (Resource) resources.get(index);
					if (addUpdateDeleteHandler.canDelete(resource)) {
						if (UIUtil.confirm("DeleteResource", shell)) {
							resources = addUpdateDeleteHandler.deleteResource(resource, shell);
							reloadTable();
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
				reloadTable();
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
			int index = table.getSelectionIndex();
			if (index >= 0) {
				try {
					Resource resource = (Resource) resources.get(index);
					resources = addUpdateDeleteHandler.updateResource(resource, shell);
					// reloadTable();
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
			int index = table.getSelectionIndex();
			if (index >= 0) {
				try {
					Resource resource = (Resource) resources.get(index);
					if (addUpdateDeleteHandler.canDelete(resource)) {
						if (UIUtil.confirm("DeleteResource", shell)) {
							resources = addUpdateDeleteHandler.deleteResource(resource, shell);
							reloadTable();
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
			int index = table.getSelectionIndex();
			if (index >= 0) {
				try {
					Resource resource = (Resource) resources.get(index);
					resources = addUpdateDeleteHandler.restore(resource, shell);
					reloadTable();
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