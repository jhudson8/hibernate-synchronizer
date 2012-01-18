/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.wizard;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
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
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.eclipse.ui.dialogs.SelectionDialog;

import com.hudson.hibernatesynchronizer.Plugin;
import com.hudson.hibernatesynchronizer.util.DatabaseResolver;
import com.hudson.hibernatesynchronizer.util.TransactionFactoryResolver;

/**
 * @author <a href="mailto: joe@binamics.com">Joe Hudson </a>
 * 
 * The "New" wizard page allows setting the container for the new file as well
 * as the file name. The page will only accept file name without the extension
 * OR with the extension that matches the expected one (hbm).
 */

public class NewConfigurationWizardPage extends WizardPage {
    private IJavaProject project;

    private Text containerText;

    private Text fileText;

    private ISelection selection;

    private Combo databaseCombo;

    private Combo appServerCombo;

    private Text localDatabaseURLText;

    private Text localDriverClassText;

    private Text localUserNameText;

    private Text localPasswordText;

    private Text datasourceNameText;

    private Text datasourceJNDIUrlText;

    private Text datasourceJNDIClassText;

    private Text datasourceUserNameText;

    private Text datasourcePasswordText;

    private Text sessionFactoryNameText;

    /**
     * Constructor for SampleNewWizardPage.
     * 
     * @param pageName
     */
    public NewConfigurationWizardPage(ISelection selection) {
        super("wizardPage");
        setTitle("Hibernate Configuration File");
        setDescription("This wizard creates a new Hibernate configuration file.");
        this.selection = selection;
    }

    /**
     * @see IDialogPage#createControl(Composite)
     */
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);
        GridLayout layout = new GridLayout();
        container.setLayout(layout);
        layout.numColumns = 3;
        layout.verticalSpacing = 9;
        Label label = new Label(container, SWT.NULL);
        label.setText("&Container:");

        containerText = new Text(container, SWT.BORDER | SWT.SINGLE);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        containerText.setLayoutData(gd);
        containerText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                dialogChanged();
            }
        });

        Button button = new Button(container, SWT.PUSH);
        button.setText("Browse...");
        button.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                handleBrowse();
            }
        });
        label = new Label(container, SWT.NULL);
        label.setText("&File name:");

        fileText = new Text(container, SWT.BORDER | SWT.SINGLE);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        fileText.setLayoutData(gd);
        fileText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                dialogChanged();
            }
        });
        label = new Label(container, SWT.NULL);

        label = new Label(container, SWT.NULL);
        label.setText("&Session Factory Name:");
        sessionFactoryNameText = new Text(container, SWT.BORDER | SWT.SINGLE);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        sessionFactoryNameText.setLayoutData(gd);
        label = new Label(container, SWT.NULL);

        label = new Label(container, SWT.NULL);
        label.setText("&Database Type:");
        databaseCombo = new Combo(container, SWT.READ_ONLY);
        List databaseNames = DatabaseResolver.getDatabaseNames();
        for (Iterator i = databaseNames.iterator(); i.hasNext();) {
            databaseCombo.add(i.next().toString());
        }
        databaseCombo.select(0);
        label = new Label(container, SWT.NULL);

        label = new Label(container, SWT.NULL);
        label.setText("&Application Server:");
        appServerCombo = new Combo(container, SWT.READ_ONLY);
        appServerCombo.add("N/A");
        List appServerNames = TransactionFactoryResolver
                .getApplicationServers();
        for (Iterator i = appServerNames.iterator(); i.hasNext();) {
            appServerCombo.add(i.next().toString());
        }
        appServerCombo.select(0);
        label = new Label(container, SWT.NULL);

        label = new Label(container, SWT.NULL);
        label.setText("&Connection:");
        gd = new GridData();
        gd.verticalAlignment = GridData.VERTICAL_ALIGN_BEGINNING;
        label.setLayoutData(gd);

        TabFolder tf = new TabFolder(container, SWT.NULL);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalSpan = 2;
        tf.setLayoutData(gd);
        Composite tab1Container = new Composite(tf, SWT.NULL);
        layout = new GridLayout();
        layout.numColumns = 3;
        layout.verticalSpacing = 9;
        tab1Container.setLayout(layout);
        tf.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        TabItem tab1 = new TabItem(tf, SWT.NULL);
        tab1.setText("Local");
        tab1.setControl(tab1Container);

        label = new Label(tab1Container, SWT.NULL);
        label.setText("Driver Class:");
        localDriverClassText = new Text(tab1Container, SWT.BORDER | SWT.SINGLE);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.widthHint = 200;
        localDriverClassText.setLayoutData(gd);
        localDriverClassText.setEnabled(false);
        localDriverClassText.setBackground(new Color(null, 255, 255, 255));

        Button driverButton = new Button(tab1Container, SWT.NATIVE);
        driverButton.setText("Browse");
        driverButton.addMouseListener(new DriverMouseListener(this));

        label = new Label(tab1Container, SWT.NULL);
        label.setText("Database URL:");
        localDatabaseURLText = new Text(tab1Container, SWT.BORDER | SWT.SINGLE);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        gd.widthHint = 200;
        localDatabaseURLText.setLayoutData(gd);

        label = new Label(tab1Container, SWT.NULL);
        label.setText("Username:");
        localUserNameText = new Text(tab1Container, SWT.BORDER | SWT.SINGLE);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        gd.widthHint = 200;
        localUserNameText.setLayoutData(gd);

        label = new Label(tab1Container, SWT.NULL);
        label.setText("Password:");
        localPasswordText = new Text(tab1Container, SWT.BORDER | SWT.SINGLE);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        gd.widthHint = 200;
        localPasswordText.setLayoutData(gd);

        Composite tab2Container = new Composite(tf, SWT.NULL);
        layout = new GridLayout();
        layout.numColumns = 2;
        layout.verticalSpacing = 9;
        tab2Container.setLayout(layout);
        TabItem tab2 = new TabItem(tf, SWT.NULL);
        tab2.setText("Datasource");
        tab2.setControl(tab2Container);
        TabItem[] tabList = { tab1, tab2 };
        tf.setSelection(tabList);

        label = new Label(tab2Container, SWT.NULL);
        label.setText("Name:");
        datasourceNameText = new Text(tab2Container, SWT.BORDER | SWT.SINGLE);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        datasourceNameText.setLayoutData(gd);

        label = new Label(tab2Container, SWT.NULL);
        label.setText("JNDI URL:");
        datasourceJNDIUrlText = new Text(tab2Container, SWT.BORDER | SWT.SINGLE);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        datasourceJNDIUrlText.setLayoutData(gd);

        label = new Label(tab2Container, SWT.NULL);
        label.setText("JNDI Class:");
        datasourceJNDIClassText = new Text(tab2Container, SWT.BORDER
                | SWT.SINGLE);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        datasourceJNDIClassText.setLayoutData(gd);

        label = new Label(tab2Container, SWT.NULL);
        label.setText("Username:");
        datasourceUserNameText = new Text(tab2Container, SWT.BORDER
                | SWT.SINGLE);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        datasourceUserNameText.setLayoutData(gd);

        label = new Label(tab2Container, SWT.NULL);
        label.setText("Password:");
        datasourcePasswordText = new Text(tab2Container, SWT.BORDER
                | SWT.SINGLE);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        datasourcePasswordText.setLayoutData(gd);

        label = new Label(tab2Container, SWT.NULL);
        label.setText("* Only the datasource name is required");
        gd = new GridData(GridData.CENTER);
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalSpan = 2;
        label.setLayoutData(gd);

        containerText.forceFocus();
        initialize();
        dialogChanged();
        setControl(container);
    }

    /**
     * Tests if the current workbench selection is a suitable container to use.
     */

    private void initialize() {
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
                    projectChanged(((IResource) obj).getProject());
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
        fileText.setText("hibernate.cfg.xml");
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
                containerText.setText(((IPath) result[0]).toOSString());
                try {
                    projectChanged(ResourcesPlugin.getWorkspace().getRoot()
                            .getFile((IPath) result[0]).getProject());
                } catch (Exception e) {
                }
            }
        }
    }

    /**
     * Ensures that both text fields are set.
     */

    private void dialogChanged() {
        String container = getContainerName();
        String fileName = getFileName();

        if (container.length() == 0) {
            updateStatus("File container must be specified");
            return;
        }
        if (fileName.length() == 0) {
            updateStatus("File name must be specified");
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

    public String getFileName() {
        return fileText.getText();
    }

    public String getDatabaseName() {
        return databaseCombo.getItem(databaseCombo.getSelectionIndex());
    }

    public String getApplicationServerName() {
        return appServerCombo.getItem(appServerCombo.getSelectionIndex());
    }

    public String getDatabaseURL() {
        return localDatabaseURLText.getText();
    }

    public String getDriverClass() {
        return localDriverClassText.getText();
    }

    public String getUsername() {
        return localUserNameText.getText();
    }

    public String getPassword() {
        return localPasswordText.getText();
    }

    public String getDatasourceName() {
        return datasourceNameText.getText();
    }

    public String getDatasourceJNDIUrl() {
        return datasourceJNDIUrlText.getText();
    }

    public String getDatasourceJNDIClassName() {
        return datasourceJNDIClassText.getText();
    }

    public String getDatasourceUserName() {
        return datasourceUserNameText.getText();
    }

    public String getDatasourcePassword() {
        return datasourcePasswordText.getText();
    }

    public String getSessionFactoryName() {
        return sessionFactoryNameText.getText();
    }

    public class DriverMouseListener implements MouseListener {
        private NewConfigurationWizardPage page;

        public DriverMouseListener(NewConfigurationWizardPage page) {
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
                    localDriverClassText.setText(type.getFullyQualifiedName());
                }
            } catch (JavaModelException jme) {
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
                if (null != driverStr)
                    localDriverClassText.setText(driverStr);
                if (null != databaseUrlStr)
                    localDatabaseURLText.setText(databaseUrlStr);
                if (null != usernameStr)
                    localUserNameText.setText(usernameStr);
            }
        } catch (Exception e) {
        }
    }
}