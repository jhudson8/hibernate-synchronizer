/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.wizard;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ColumnLayoutData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.eclipse.ui.dialogs.SelectionDialog;

import com.hudson.hibernatesynchronizer.Plugin;
import com.hudson.hibernatesynchronizer.db.DBTable;
import com.hudson.hibernatesynchronizer.util.IErrorHandler;
import com.hudson.hibernatesynchronizer.util.ProjectClassLoader;

/**
 * @author <a href="mailto: joe@binamics.com">Joe Hudson </a>
 * 
 * The "New" wizard page allows setting the container for the new file as well
 * as the file name. The page will only accept file name without the extension
 * OR with the extension that matches the expected one (hbm).
 */

public class NewMappingWizardPage extends WizardPage implements IErrorHandler {
    private ISelection selection;

    private Text containerText;

    private Text fileText;

    Text tablePattern;

    Text schemaPattern;

    private Text driverText;

    private Text databaseUrlText;

    private Text usernameText;

    private Text passwordText;

    private Text packageText;

    private Button tableRefreshButton;

    private Button selectAllButton;

    private Button selectNoneButton;

    private Button packageButton;

    Table table;

    private Composite setComp;

    private BooleanFieldEditor useLazyLoading;

    private BooleanFieldEditor createSets;

    private BooleanFieldEditor startLowerCase;

    private Text extensionText;

    private Text generatorText;

    private Text compositeIdNameText;

    private BooleanFieldEditor useProxies;

    private Text proxyNameText;

    private Composite proxyComposite;

    protected IJavaProject project;

    ClassLoader loader;

    Class notifiedDriver;

    String currentDriverClass;

    Map tables;

    static Map drivers = new HashMap();

    /**
     * Constructor for SampleNewWizardPage.
     * 
     * @param pageName
     */
    public NewMappingWizardPage(ISelection selection) {
        super("wizardPage");
        setTitle("Hibernate Mapping File");
        setDescription("This wizard creates a new Hibernate mapping file.");
        this.selection = selection;
    }

    /**
     * @see IDialogPage#createControl(Composite)
     */
    public void createControl(Composite parent) {
        TabFolder folder = new TabFolder(parent, SWT.NONE);
        setControl(folder);
        folder.setLayoutData(new GridData(GridData.FILL_BOTH));
        TabItem item = new TabItem(folder, SWT.NONE);
        item.setText("Configuration");
        Composite composite = addConfiguration(folder);
        item.setControl(composite);

        item = new TabItem(folder, SWT.NONE);
        item.setText("Properties");
        composite = addProperties(folder);
        item.setControl(composite);
    }

    public Composite addProperties(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);
        GridLayout layout = new GridLayout(1, false);
        container.setLayout(layout);
        layout.verticalSpacing = 9;

        Composite subComp = new Composite(container, SWT.NULL);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL
                | GridData.GRAB_HORIZONTAL);
        gd.horizontalSpan = 2;
        subComp.setLayoutData(gd);
        layout = new GridLayout();
        subComp.setLayout(layout);
        layout.numColumns = 3;
        Label label = new Label(subComp, SWT.NULL);
        label.setText("Extension");
        extensionText = new Text(subComp, SWT.BORDER);
        String extension = Plugin.getDefault().getPreferenceStore().getString(
                "Extension");
        if (null != extension && extension.trim().length() > 0)
            extensionText.setText(extension);
        else
            extensionText.setText("hbm");
        gd = new GridData();
        gd.widthHint = 150;
        gd.horizontalSpan = 2;
        extensionText.setLayoutData(gd);

        label = new Label(subComp, SWT.NULL);
        label.setText("Composite ID Name");
        compositeIdNameText = new Text(subComp, SWT.BORDER);
        String compositeIdName = Plugin.getDefault().getPreferenceStore()
                .getString("CompositeIdName");
        if (null != compositeIdName && compositeIdName.trim().length() > 0)
            compositeIdNameText.setText(compositeIdName);
        else
            compositeIdNameText.setText("id");
        gd = new GridData();
        gd.widthHint = 150;
        gd.horizontalSpan = 2;
        compositeIdNameText.setLayoutData(gd);

        label = new Label(subComp, SWT.NULL);
        label.setText("ID Generator");
        generatorText = new Text(subComp, SWT.BORDER);
        String generator = Plugin.getDefault().getPreferenceStore().getString(
                "Generator");
        if (null != generator && generator.trim().length() > 0)
            generatorText.setText(generator);
        else
            generatorText.setText("vm");
        gd = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        gd.widthHint = 150;
        generatorText.setLayoutData(gd);
        Button browseButton = new Button(subComp, SWT.NATIVE);
        browseButton.setText("Browse");
        browseButton.addMouseListener(new MouseListener() {
            public void mouseDown(MouseEvent e) {
                try {
                    IJavaSearchScope searchScope = null;
                    if (null != project) {
                        searchScope = SearchEngine
                                .createJavaSearchScope(new IJavaElement[] { project });
                    } else {
                        searchScope = SearchEngine.createWorkspaceScope();
                    }
                    SelectionDialog sd = JavaUI
                            .createTypeDialog(
                                    getShell(),
                                    new ApplicationWindow(getShell()),
                                    searchScope,
                                    IJavaElementSearchConstants.CONSIDER_CLASSES,
                                    false);
                    sd.open();
                    Object[] objects = sd.getResult();
                    if (null != objects && objects.length > 0) {
                        IType type = (IType) objects[0];
                        generatorText.setText(type.getFullyQualifiedName());
                    }
                } catch (JavaModelException jme) {
                }
            }

            public void mouseDoubleClick(MouseEvent e) {
            }

            public void mouseUp(MouseEvent e) {
            }
        });

        new Label(subComp, SWT.NULL);

        setComp = new Composite(container, SWT.NULL);
        layout = new GridLayout();
        container.setLayout(layout);
        layout.numColumns = 2;
        gd = new GridData();
        gd.horizontalSpan = 2;
        gd.grabExcessHorizontalSpace = true;
        subComp.setData(gd);
        createSets = new BooleanFieldEditor("CreateSets",
                "Generate Sets to represent inverse foreign relationships",
                setComp);
        Plugin.getDefault().getPreferenceStore().setDefault("CreateSets", true);
        createSets.setPreferenceStore(Plugin.getDefault().getPreferenceStore());
        createSets.load();
        createSets.setPropertyChangeListener(new IPropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent event) {
                useLazyLoading
                        .setEnabled(createSets.getBooleanValue(), setComp);
            }
        });
        useLazyLoading = new BooleanFieldEditor("UseLazyLoading",
                "Use Lazy Loading", setComp);
        useLazyLoading.setPreferenceStore(Plugin.getDefault()
                .getPreferenceStore());
        useLazyLoading.load();
        if (!createSets.getBooleanValue())
            useLazyLoading.setEnabled(false, setComp);
        startLowerCase = new BooleanFieldEditor("StartLowerCase",
                "Start Properties with Lower Case", setComp);
        startLowerCase.setPreferenceStore(Plugin.getDefault()
                .getPreferenceStore());
        startLowerCase.load();
        startLowerCase.setPropertyChangeListener(new IPropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent event) {
                compositeIdNameCheck();
            }
        });

        subComp = new Composite(setComp, SWT.NULL);
        subComp.setLayout(new GridLayout(3, false));
        useProxies = new BooleanFieldEditor("UseProxies", "Use Proxy Classes",
                subComp);
        useProxies.setPreferenceStore(Plugin.getDefault().getPreferenceStore());
        useProxies.load();
        useProxies.setPropertyChangeListener(new IPropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent event) {
                setProxyComposite();
            }
        });
        proxyComposite = new Composite(subComp, SWT.NULL);
        proxyComposite.setLayout(new GridLayout(2, false));
        label = new Label(proxyComposite, SWT.NULL);
        label.setText("    Proxy Class Name");
        proxyNameText = new Text(proxyComposite, SWT.BORDER);
        String proxyClassName = Plugin.getDefault().getPreferenceStore()
                .getString("ProxyClassName");
        if (null != proxyClassName && proxyClassName.trim().length() > 0)
            proxyNameText.setText(proxyClassName);
        else
            proxyNameText.setText("${className}Proxy");
        gd = new GridData();
        gd.widthHint = 150;
        proxyNameText.setLayoutData(gd);
        setProxyComposite();
        compositeIdNameCheck();
        return container;
    }

    private void compositeIdNameCheck() {
        String s = compositeIdNameText.getText().trim();
        if (s.length() > 0 && s.indexOf('.') < 0) {
            if (startLowerCase.getBooleanValue()) {
                compositeIdNameText.setText(s.substring(0, 1).toLowerCase()
                        + s.substring(1, s.length()));
            } else {
                compositeIdNameText.setText(s.substring(0, 1).toUpperCase()
                        + s.substring(1, s.length()));
            }
        }
    }

    public void setProxyComposite() {
        if (useProxies.getBooleanValue()) {
            proxyComposite.setVisible(true);
        } else {
            proxyComposite.setVisible(false);
        }
    }

    public Composite addConfiguration(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);
        GridLayout layout = new GridLayout();
        container.setLayout(layout);
        layout.numColumns = 3;
        layout.verticalSpacing = 9;

        Label label = new Label(container, SWT.NULL);
        label.setText("&Container:");

        containerText = new Text(container, SWT.BORDER | SWT.SINGLE);
        containerText.setEnabled(false);
        containerText.setBackground(new Color(null, 255, 255, 255));
        GridData gd = new GridData();
        gd.widthHint = 250;
        containerText.setLayoutData(gd);
        containerText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                dialogChanged();
            }
        });
        Button containerButton = new Button(container, SWT.NATIVE);
        containerButton.setText("Browse");
        containerButton.addMouseListener(new ContainerMouseListener(this));

        label = new Label(container, SWT.NULL);
        gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalSpan = 3;
        label.setLayoutData(gd);

        label = new Label(container, SWT.NULL);
        label.setText("&Driver:");
        driverText = new Text(container, SWT.BORDER | SWT.SINGLE);
        driverText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                dialogChanged();
            }
        });
        driverText.setEnabled(false);
        driverText.setBackground(new Color(null, 255, 255, 255));
        gd = new GridData();
        gd.widthHint = 250;
        driverText.setLayoutData(gd);
        Button driverButton = new Button(container, SWT.NATIVE);
        driverButton.setText("Browse");
        driverButton.addMouseListener(new DriverMouseListener(this));

        label = new Label(container, SWT.NULL);
        label.setText("&Database URL:");
        databaseUrlText = new Text(container, SWT.BORDER | SWT.SINGLE);
        databaseUrlText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                dialogChanged();
            }
        });
        gd = new GridData();
        gd.horizontalSpan = 2;
        gd.widthHint = 250;
        databaseUrlText.setLayoutData(gd);

        label = new Label(container, SWT.NULL);
        label.setText("&Username:");
        usernameText = new Text(container, SWT.BORDER | SWT.SINGLE);
        usernameText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                dialogChanged();
            }
        });
        gd = new GridData();
        gd.horizontalSpan = 2;
        gd.widthHint = 150;
        usernameText.setLayoutData(gd);

        label = new Label(container, SWT.NULL);
        label.setText("&Password:");
        passwordText = new Text(container, SWT.BORDER | SWT.SINGLE);
        passwordText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                dialogChanged();
            }
        });
        passwordText.setEchoChar('*');
        gd = new GridData();
        gd.horizontalSpan = 2;
        gd.widthHint = 150;
        passwordText.setLayoutData(gd);

        label = new Label(container, SWT.NULL);
        label.setText("Table pattern:");
        tablePattern = new Text(container, SWT.BORDER | SWT.SINGLE);
        tablePattern.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                dialogChanged();
            }
        });
        tablePattern.setEnabled(true);
        tablePattern.setBackground(new Color(null, 255, 255, 255));
        gd = new GridData();
        gd.horizontalSpan = 2;
        gd.widthHint = 250;
        tablePattern.setLayoutData(gd);

        label = new Label(container, SWT.NULL);
        label.setText("Schema pattern:");
        schemaPattern = new Text(container, SWT.BORDER | SWT.SINGLE);
        schemaPattern.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                dialogChanged();
            }
        });
        schemaPattern.setEnabled(true);
        schemaPattern.setBackground(new Color(null, 255, 255, 255));
        gd = new GridData();
        gd.horizontalSpan = 2;
        gd.widthHint = 250;
        schemaPattern.setLayoutData(gd);

        label = new Label(container, SWT.NULL);
        label.setText("Tables");
        table = new Table(container, SWT.BORDER | SWT.H_SCROLL
                | SWT.FULL_SELECTION | SWT.CHECK);
        table.setVisible(true);
        table.setLinesVisible(false);
        table.setHeaderVisible(false);
        table.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent e) {
                dialogChanged();
            }

            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });
        GridData data = new GridData();
        data.heightHint = 150;
        data.widthHint = 250;
        table.setLayoutData(data);

        // create the columns
        TableColumn nameColumn = new TableColumn(table, SWT.LEFT);
        ColumnLayoutData nameColumnLayout = new ColumnWeightData(100, false);

        // set columns in Table layout
        TableLayout tableLayout = new TableLayout();
        tableLayout.addColumnData(nameColumnLayout);
        table.setLayout(tableLayout);

        Composite buttonContainer = new Composite(container, SWT.NULL);
        buttonContainer.setLayout(new GridLayout(1, true));
        gd = new GridData();
        gd.verticalAlignment = GridData.BEGINNING;
        gd.horizontalAlignment = GridData.BEGINNING;
        buttonContainer.setLayoutData(gd);
        tableRefreshButton = new Button(buttonContainer, SWT.PUSH);
        tableRefreshButton.setText("Refresh");
        tableRefreshButton
                .setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        tableRefreshButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                ProgressMonitorDialog dialog = new ProgressMonitorDialog(
                        getShell());
                try {
                    dialog.run(false, true, new IRunnableWithProgress() {
                        public void run(IProgressMonitor monitor)
                                throws InvocationTargetException,
                                InterruptedException {
                            try {
                                monitor.beginTask("Refreshing tables...", 21);
                                refreshTables(monitor);
                            } catch (Exception e) {
                                throw new InvocationTargetException(e);
                            } finally {
                                monitor.done();
                            }
                        }
                    });
                } catch (Exception exc) {
                }
            }
        });
        selectAllButton = new Button(buttonContainer, SWT.PUSH);
        selectAllButton.setText("Select All");
        selectAllButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        selectAllButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                try {
                    for (int i = 0; i < table.getItemCount(); i++) {
                        table.getItem(i).setChecked(true);
                    }
                    dialogChanged();
                } catch (Exception exc) {
                }
            }
        });
        selectNoneButton = new Button(buttonContainer, SWT.PUSH);
        selectNoneButton.setText("Select None");
        selectNoneButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        selectNoneButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                try {
                    for (int i = 0; i < table.getItemCount(); i++) {
                        table.getItem(i).setChecked(false);
                    }
                    dialogChanged();
                } catch (Exception exc) {
                }
            }
        });

        label = new Label(container, SWT.NULL);
        label.setText("&Package:");
        packageText = new Text(container, SWT.BORDER | SWT.SINGLE);
        packageText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                dialogChanged();
            }
        });
        gd = new GridData();
        gd.widthHint = 250;
        packageText.setLayoutData(gd);

        packageButton = new Button(container, SWT.NATIVE);
        packageButton.setText("Browse");
        packageButton.addMouseListener(new MouseListener() {
            public void mouseDown(MouseEvent e) {
                if (null != project) {
                    try {
                        IJavaSearchScope searchScope = SearchEngine
                                .createWorkspaceScope();
                        SelectionDialog sd = JavaUI
                                .createPackageDialog(
                                        getShell(),
                                        project,
                                        IJavaElementSearchConstants.CONSIDER_REQUIRED_PROJECTS);
                        sd.open();
                        Object[] objects = sd.getResult();
                        if (null != objects && objects.length > 0) {
                            IPackageFragment pf = (IPackageFragment) objects[0];
                            packageText.setText(pf.getElementName());
                        }
                    } catch (JavaModelException jme) {
                        jme.printStackTrace();
                    }
                }
            }

            public void mouseDoubleClick(MouseEvent e) {
            }

            public void mouseUp(MouseEvent e) {
            }

        });

        if (selection != null && selection.isEmpty() == false
                && selection instanceof IStructuredSelection) {
            IStructuredSelection ssel = (IStructuredSelection) selection;
            if (ssel.size() == 1) {
                Object obj = ssel.getFirstElement();
                if (obj instanceof IResource) {
                    IContainer cont;
                    if (obj instanceof IContainer)
                        cont = (IContainer) obj;
                    else
                        cont = ((IResource) obj).getParent();
                    containerText.setText(cont.getFullPath().toString());
                    projectChanged(cont.getProject());
                } else if (obj instanceof IPackageFragment) {
                    IPackageFragment frag = (IPackageFragment) obj;
                    containerText.setText(frag.getPath().toString());
                    projectChanged(frag.getJavaProject().getProject());
                } else if (obj instanceof IPackageFragmentRoot) {
                    IPackageFragmentRoot root = (IPackageFragmentRoot) obj;
                    containerText.setText(root.getPath().toString());
                    projectChanged(root.getJavaProject().getProject());
                } else if (obj instanceof IJavaProject) {
                    IJavaProject proj = (IJavaProject) obj;
                    containerText.setText("/" + proj.getProject().getName());
                    projectChanged(proj.getProject());
                } else if (obj instanceof IProject) {
                    IProject proj = (IProject) obj;
                    containerText.setText("/" + proj.getName());
                    projectChanged(proj);
                }
            }
        }

        containerText.forceFocus();
        initialize();
        dialogChanged();
        return container;
    }

    private void refreshTables(IProgressMonitor monitor) {
        final IProgressMonitor finalMonitor = monitor;
        monitor.subTask("Clearing stale entries");
        table.removeAll();
        dialogChanged();
        monitor.worked(1);
        MappingWizardConnectionRunnable runnable = new MappingWizardConnectionRunnable(
                this, finalMonitor);
        BusyIndicator.showWhile(null, runnable);
    }

    /**
     * Tests if the current workbench selection is a suitable container to use.
     */

    private void initialize() {
        if (selection != null && selection.isEmpty() == false
                && selection instanceof IStructuredSelection) {
            IStructuredSelection ssel = (IStructuredSelection) selection;
            if (ssel.size() > 1)
                return;
            Object obj = ssel.getFirstElement();
            if (obj instanceof IResource) {
                IContainer container;
                if (obj instanceof IContainer)
                    container = (IContainer) obj;
                else
                    container = ((IResource) obj).getParent();
                containerText.setText(container.getFullPath().toString());
            }
        }
    }

    /**
     * Uses the standard container selection dialog to choose the new value for
     * the container field.
     */

    private void handleBrowse() {
        ContainerSelectionDialog dialog = new ContainerSelectionDialog(
                getShell(), ResourcesPlugin.getWorkspace().getRoot(), false,
                "Select new file container");
        if (dialog.open() == ContainerSelectionDialog.OK) {
            Object[] result = dialog.getResult();
            if (result.length == 1) {
                containerText.setText(((Path) result[0]).toOSString());
            }
        }
    }

    /**
     * Ensures that both text fields are set.
     */

    void dialogChanged() {
        if (table.getItemCount() > 0) {
            selectAllButton.setEnabled(true);
            selectNoneButton.setEnabled(true);
        } else {
            selectAllButton.setEnabled(false);
            selectNoneButton.setEnabled(false);
        }
        String container = getContainerName();

        if ((null == driverText.getText() || driverText.getText().trim()
                .length() == 0)
                || (null == databaseUrlText.getText() || databaseUrlText
                        .getText().trim().length() == 0)) {
            tableRefreshButton.setEnabled(false);
        } else {
            tableRefreshButton.setEnabled(true);
        }
        if (null == project) {
            packageButton.setEnabled(false);
        } else {
            packageButton.setEnabled(true);
        }

        if (container.length() == 0) {
            updateStatus("File container must be specified");
            return;
        }
        int selectedItems = 0;
        for (int i = 0; i < table.getItems().length; i++) {
            TableItem item = table.getItems()[i];
            if (item.getChecked()) {
                selectedItems++;
            }
        }
        if (table.getItems().length == 0) {
            updateStatus("Click \"Refresh\" after entering your database connection information");
            return;
        } else if (selectedItems == 0) {
            updateStatus("You must select 1 or more tables by clicking in the checkbox");
            return;
        } else if (getPackage().length() == 0) {
            updateStatus("You must specify a valid (non-default) package for your business objects");
            return;
        }

        updateStatus(null);
    }

    private void updateStatus(String message) {
        setErrorMessage(message);
        setPageComplete(message == null);
    }

    public String getContainerName() {
        return containerText.getText();
    }

    public Connection getConnection(IProgressMonitor monitor) {
        if (null != project) {
            try {
                if (driverText.getText().trim().length() > 0
                        && databaseUrlText.getText().trim().length() > 0
                        && usernameText.getText().trim().length() > 0) {
                    monitor.subTask("Saving project entries");
                    Plugin.setProperty(project.getProject(), "driver",
                            driverText.getText());
                    Plugin.setProperty(project.getProject(), "databaseUrl",
                            databaseUrlText.getText());
                    Plugin.setProperty(project.getProject(), "username",
                            usernameText.getText());
                    Plugin.saveProperties(project.getProject());
                }
                if (null != monitor)
                    monitor.worked(1);
            } catch (Exception e) {
            }
        }

        try {
            Driver driverObject = (Driver) drivers.get(driverText.getText());
            if (null == driverObject) {
                if (null == project) {
                    MessageDialog
                            .openError(
                                    getShell(),
                                    "The project could not be established",
                                    "An unexpected error occured establishing the Java projected related to the current project");
                    return null;
                }
                Class driverClass = null;
                try {
                    if (null != monitor)
                        monitor.subTask("Creating project ClassLoader");
                    loader = new ProjectClassLoader(this.project, getClass()
                            .getClassLoader());
                    if (null == loader) {
                        MessageDialog.openError(getShell(),
                                "Error establishing connection",
                                "The class loader could not be created");
                        return null;
                    }
                    if (null != monitor)
                        monitor.worked(3);
                    if (null != monitor)
                        monitor.subTask("Loading driver Class");
                    if (null == driverText.getText()) {
                        MessageDialog.openError(getShell(),
                                "Error establishing connection",
                                "The driver must be specified");
                        return null;
                    }
                    driverClass = loader.loadClass(driverText.getText());
                    if (null != monitor)
                        monitor.worked(13);
                } catch (Exception e) {
                    if (null != monitor)
                        monitor.worked(7);
                    StringBuffer msg = new StringBuffer();
                    msg.append("The locations checked were:");
                    URL[] urls = ((ProjectClassLoader) loader).getURLs();
                    if (null != urls) {
                        for (int i = 0; i < urls.length; i++) {
                            if (null != urls[i])
                                msg.append("\n" + urls[i].getFile());
                        }
                    }
                    try {
                        FileDialog fd = new FileDialog(getShell(), SWT.SINGLE);
                        fd.setText("Please select the driver jar location");
                        fd.open();
                        String fileName = fd.getFilterPath() + "/"
                                + fd.getFileName();
                        urls = new URL[1];
                        File f = new File(fileName);
                        urls[0] = f.toURL();
                        loader = new URLClassLoader(urls);
                        driverClass = loader.loadClass(driverText.getText());
                        if (null != monitor)
                            monitor.worked(6);
                    } catch (Exception exc) {
                        if (null != loader) {
                            Plugin.logError(e);
                            if (null != urls[0])
                                msg.append("\n" + urls[0].getFile());
                            MessageDialog.openError(getShell(),
                                    "The driver could not be located", msg
                                            .toString());
                        } else {
                            MessageDialog.openError(getShell(),
                                    "The class loader could not be created", e
                                            .getMessage());
                        }
                        return null;
                    }
                }

                if (null != monitor)
                    monitor.subTask("Instantiating driver");
                try {
                    // Instantiate it
                    Object tObj = driverClass.newInstance();
                    // Then create a Driver object
                    driverObject = (Driver) tObj;
                    drivers.put(driverText.getText(), driverObject);
                } catch (ClassCastException e) {
                    MessageDialog.openError(getShell(),
                            "Error establishing connection",
                            "The driver class could not be casted as a Driver");
                    return null;
                } catch (Exception e) {
                    MessageDialog.openError(getShell(),
                            "Error establishing connection",
                            "The driver class was located but could not be instantiated\n\n"
                                    + e.getMessage());
                    return null;
                }
                if (null != monitor)
                    monitor.worked(1);
            } else {
                if (null != monitor)
                    monitor.worked(17);
            }

            // And a connection
            if (null != monitor)
                monitor.subTask("Establishing connection");
            Properties prop = new Properties();
            prop.setProperty("user", usernameText.getText());
            prop.setProperty("password", passwordText.getText());

            Connection con = driverObject.connect(databaseUrlText.getText(),
                    prop);
            if (null != monitor)
                monitor.worked(2);
            if (null == con) {
                MessageDialog
                        .openError(
                                getShell(),
                                "Error establishing connection",
                                "This is most likely due to the URL not matching the expected value by the JDBC driver.  Please verify your settings.");
            }
            return con;
        } catch (SQLException e) {
            MessageDialog.openError(getShell(),
                    "Error establishing connection", e.getMessage());
            return null;
        } catch (Exception e) {
            MessageDialog.openError(getShell(), "Unknown database driver", e
                    .getMessage());
            return null;
        }
    }

    public List getTables() {
        List tableList = new ArrayList();
        for (int i = 0; i < table.getItems().length; i++) {
            TableItem item = table.getItems()[i];
            if (item.getChecked()) {
                DBTable table = (DBTable) tables.get(item.getText(0));
                tableList.add(table);
            }
        }
        return tableList;
    }

    public String getPackage() {
        String packageStr = packageText.getText();
        while (packageStr.startsWith(".")) {
            packageStr = packageStr.substring(1, packageStr.length());
        }
        while (packageStr.endsWith(".")) {
            packageStr = packageStr.substring(0, packageStr.length() - 1);
        }
        return packageStr;
    }

    public Properties getProperties() {
        Properties p = new Properties();
        if (null != extensionText.getText()
                && extensionText.getText().trim().length() > 0) {
            p.put("Extension", extensionText.getText().trim());
            Plugin.getDefault().getPreferenceStore().setValue("Extension",
                    extensionText.getText().trim());
        }
        createSets.store();
        if (createSets.getBooleanValue())
            p.setProperty("CreateSets", Boolean.TRUE.toString());
        useLazyLoading.store();
        if (useLazyLoading.getBooleanValue())
            p.setProperty("UseLazyLoading", Boolean.TRUE.toString());
        startLowerCase.store();
        if (startLowerCase.getBooleanValue())
            p.setProperty("StartLowerCase", Boolean.TRUE.toString());
        if (null != generatorText.getText()
                && generatorText.getText().trim().length() > 0) {
            p.put("Generator", generatorText.getText().trim());
            Plugin.getDefault().getPreferenceStore().setValue("Generator",
                    generatorText.getText().trim());
        }
        if (null != compositeIdNameText.getText()
                && compositeIdNameText.getText().trim().length() > 0) {
            p.put("CompositeIdName", compositeIdNameText.getText().trim());
            Plugin.getDefault().getPreferenceStore().setValue(
                    "CompositeIdName", compositeIdNameText.getText().trim());
        }
        if (useProxies.getBooleanValue())
            p.setProperty("UseProxies", Boolean.TRUE.toString());
        useProxies.store();
        if (null != proxyNameText.getText()
                && proxyNameText.getText().trim().length() > 0) {
            p.put("ProxyClassName", proxyNameText.getText().trim());
            Plugin.getDefault().getPreferenceStore().setValue("ProxyClassName",
                    proxyNameText.getText().trim());
        }
        return p;
    }

    public class DriverMouseListener implements MouseListener {
        private NewMappingWizardPage page;

        public DriverMouseListener(NewMappingWizardPage page) {
            this.page = page;
        }

        public void mouseDown(MouseEvent e) {
            try {
                IJavaSearchScope searchScope = null;
                if (null != project) {
                    searchScope = SearchEngine
                            .createJavaSearchScope(new IJavaElement[] { project });
                } else {
                    searchScope = SearchEngine.createWorkspaceScope();
                }
                SelectionDialog sd = JavaUI.createTypeDialog(getShell(),
                        new ApplicationWindow(getShell()), searchScope,
                        IJavaElementSearchConstants.CONSIDER_CLASSES, false);
                sd.open();
                Object[] objects = sd.getResult();
                if (null != objects && objects.length > 0) {
                    IType type = (IType) objects[0];
                    driverText.setText(type.getFullyQualifiedName());
                }
            } catch (JavaModelException jme) {
            }
        }

        public void mouseDoubleClick(MouseEvent e) {
        }

        public void mouseUp(MouseEvent e) {
        }
    }

    public class ContainerMouseListener implements MouseListener {
        private NewMappingWizardPage page;

        public ContainerMouseListener(NewMappingWizardPage page) {
            this.page = page;
        }

        public void mouseDown(MouseEvent e) {
            try {
                ContainerSelectionDialog d = new ContainerSelectionDialog(
                        getShell(), null, false, "Resource location selection");
                d.open();
                Object[] arr = d.getResult();
                StringBuffer sb = new StringBuffer();
                IProject projectT = null;
                for (int i = 0; i < arr.length; i++) {
                    IPath path = (Path) arr[i];
                    containerText.setText(path.toString());
                    if (path.segments().length > 0) {
                        projectT = ResourcesPlugin.getWorkspace().getRoot()
                                .getProject(path.segments()[0]);
                    }
                }
                if (null != projectT) {
                    projectChanged(projectT);
                }
            } catch (Exception exc) {
            }
        }

        public void mouseDoubleClick(MouseEvent e) {
        }

        public void mouseUp(MouseEvent e) {
        }
    }

    private void projectChanged(IProject project) {
        try {
            if (null == this.project
                    || !this.project.getProject().getName().equals(
                            project.getName())) {
                this.project = JavaCore.create(project);
                String driverStr = Plugin.getProperty(project, "driver");
                String databaseUrlStr = Plugin.getProperty(project,
                        "databaseUrl");
                String usernameStr = Plugin.getProperty(project, "username");
                String packageStr = Plugin.getProperty(project, "package");
                this.currentDriverClass = null;
                this.loader = null;
                if (null != driverStr)
                    driverText.setText(driverStr);
                if (null != databaseUrlStr)
                    databaseUrlText.setText(databaseUrlStr);
                if (null != usernameStr)
                    usernameText.setText(usernameStr);
                if (null != packageStr)
                    packageText.setText(packageStr);
            }
        } catch (Exception e) {
        }
    }

    public void onError(String message, Throwable exception) {
        if (null == message && null != exception)
            message = exception.getMessage();
        if (null == message && null != exception)
            message = exception.getClass().getName();
        MessageDialog.openError(getShell(), "An error has occured", message);
    }
}