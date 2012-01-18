/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.wizard;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;

import com.hudson.hibernatesynchronizer.Constants;
import com.hudson.hibernatesynchronizer.Plugin;
import com.hudson.hibernatesynchronizer.resource.ResourceManager;
import com.hudson.hibernatesynchronizer.util.DatabaseResolver;
import com.hudson.hibernatesynchronizer.util.EditorUtil;
import com.hudson.hibernatesynchronizer.util.TransactionFactoryResolver;

/**
 * @author <a href="mailto: joe@binamics.com">Joe Hudson </a>
 * 
 * This is a sample new wizard. Its role is to create a new file resource in the
 * provided container. If the container resource (a folder or a project) is
 * selected in the workspace when the wizard is opened, it will accept it as the
 * target container. The wizard creates one file with the extension "hbm". If a
 * sample multi-page editor (also available as a template) is registered for the
 * same extension, it will be able to open it.
 */

public class NewConfigurationWizard extends Wizard implements INewWizard {
    private NewConfigurationWizardPage page;

    private ISelection selection;

    /**
     * Constructor for NewConfigurationWizard.
     */
    public NewConfigurationWizard() {
        super();
        setNeedsProgressMonitor(true);

        try {
            URL prefix = new URL(Plugin.getDefault().getDescriptor()
                    .getInstallURL(), "icons/");
            ImageDescriptor id = ImageDescriptor.createFromURL(new URL(prefix,
                    "new_wizard.gif"));
            setDefaultPageImageDescriptor(id);
        } catch (MalformedURLException e) {
        }
    }

    /**
     * Adding the page to the wizard.
     */

    public void addPages() {
        page = new NewConfigurationWizardPage(selection);
        addPage(page);
    }

    /**
     * This method is called when 'Finish' button is pressed in the wizard. We
     * will create an operation and run it using wizard as execution context.
     */
    public boolean performFinish() {
        final String containerName = page.getContainerName();
        final String fileName = page.getFileName();
        final String databaseName = page.getDatabaseName();
        final String appServerName = page.getApplicationServerName();
        final String databaseURL = page.getDatabaseURL();
        final String driverClass = page.getDriverClass();
        final String username = page.getUsername();
        final String password = page.getPassword();
        final String sessionFactoryName = page.getSessionFactoryName();
        final String datasourceName = page.getDatasourceName();
        final String datasourceJNDIUrl = page.getDatasourceJNDIUrl();
        final String datasourceJNDIClassName = page
                .getDatasourceJNDIClassName();
        final String datasourceUserName = page.getDatasourceUserName();
        final String datasourcePassword = page.getDatasourcePassword();
        IRunnableWithProgress op = new IRunnableWithProgress() {
            public void run(IProgressMonitor monitor)
                    throws InvocationTargetException {
                try {
                    doFinish(containerName, fileName, databaseName,
                            appServerName, databaseURL, driverClass, username,
                            password, datasourceName, datasourceJNDIUrl,
                            datasourceJNDIClassName, datasourceUserName,
                            datasourcePassword, sessionFactoryName, monitor);
                } catch (CoreException e) {
                    throw new InvocationTargetException(e);
                } finally {
                    monitor.done();
                }
            }
        };
        try {
            getContainer().run(true, false, op);
        } catch (InterruptedException e) {
            return false;
        } catch (InvocationTargetException e) {
            e.getTargetException().printStackTrace();
            Throwable realException = e.getTargetException();
            MessageDialog.openError(getShell(), "Error", realException
                    .getMessage());
            return false;
        }
        return true;
    }

    /**
     * The worker method. It will find the container, create the file if missing
     * or just replace its contents, and open the editor on the newly created
     * file.
     */

    private void doFinish(String containerName, String fileName,
            String databaseName, String appServerName, String localDatabaseURL,
            String localDriverClass, String localUsername,
            String localPassword, String datasourceName,
            String datasourceJNDIUrl, String datasourceClassName,
            String datasourceUserName, String datasourcePassword,
            String sessionFactoryName, IProgressMonitor monitor)
            throws CoreException {
        // create a sample file
        monitor.beginTask("Creating " + fileName, 2);
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IResource resource = root.findMember(new Path(containerName));
        if (!resource.exists() || !(resource instanceof IContainer)) {
            throwCoreException("Container \"" + containerName
                    + "\" does not exist.");
        }
        IContainer container = (IContainer) resource;
        final IFile file = container.getFile(new Path(fileName));
        Plugin.setProperty(file.getProject(),
                Constants.PROP_CONFIGURATION_FILE, file.getFullPath()
                        .toOSString());
        try {

            InputStream stream = null;
            try {
            	String mappingTemplate = ResourceManager.getTemplateContents("templates/NewConfiguration.vm");
                StringWriter sw = new StringWriter();
                VelocityContext context = new VelocityContext();
                context.put("databaseName", databaseName);
                context.put("databaseResolver", DatabaseResolver.getInstance());
                context.put("appServerName", appServerName);
                context.put("transactionFactoryResolver",
                        TransactionFactoryResolver.getInstance());
                context.put("databaseURL", localDatabaseURL);
                context.put("driverClass", localDriverClass);
                context.put("localUsername", localUsername);
                context.put("localPassword", localPassword);
                context.put("datasourceName", datasourceName);
                context.put("datasourceJNDIUrl", datasourceJNDIUrl);
                context.put("datasourceJNDIClassName", datasourceClassName);
                context.put("datasourceUsername", datasourceUserName);
                context.put("datasourcePassword", datasourcePassword);
                context.put("sessionFactoryName", sessionFactoryName);
                Constants.templateGenerator.evaluate(context, sw, Velocity.class.getName(), mappingTemplate);
                stream = new ByteArrayInputStream(sw.toString().getBytes());
            } catch (Exception e) {
                stream = new ByteArrayInputStream("".getBytes());
            }

            if (file.exists()) {
                file.setContents(stream, true, true, monitor);
            } else {
                file.create(stream, true, monitor);
            }
            stream.close();
            try {
                IProject project = file.getProject();
                if (null != localDriverClass
                        && localDriverClass.trim().length() > 0)
                    Plugin.setProperty(file.getProject(), "driver",
                            localDriverClass);
                if (null != localDatabaseURL
                        && localDatabaseURL.trim().length() > 0)
                    Plugin.setProperty(file.getProject(), "databaseUrl",
                            localDatabaseURL);
                if (null != localUsername && localUsername.trim().length() > 0)
                    Plugin.setProperty(file.getProject(), "username",
                            localUsername);
            } catch (Exception e) {
            }
        } catch (IOException e) {
        }
        monitor.worked(1);
        monitor.setTaskName("Opening file for editing...");
        getShell().getDisplay().asyncExec(new Runnable() {
            public void run() {
                EditorUtil.openPage(file);
            }
        });
        monitor.worked(1);
    }

    private void throwCoreException(String message) throws CoreException {
        IStatus status = new Status(IStatus.ERROR,
                "com.hudson.hibernatesynchronizer", IStatus.OK, message, null);
        throw new CoreException(status);
    }

    /**
     * We will accept the selection in the workbench to see if we can initialize
     * from it.
     * 
     * @see IWorkbenchWizard#init(IWorkbench, IStructuredSelection)
     */
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        this.selection = selection;
    }
}