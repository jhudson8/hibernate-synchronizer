/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.wizard;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.velocity.Template;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
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
import com.hudson.hibernatesynchronizer.db.DBTable;
import com.hudson.hibernatesynchronizer.popup.actions.AddMappingReference;
import com.hudson.hibernatesynchronizer.util.HSUtil;

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

public class NewMappingWizard extends Wizard implements INewWizard {
    private NewMappingWizardPage page;

    private ISelection selection;

    /**
     * Constructor for NewMappingWizard.
     */
    public NewMappingWizard() {
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
        page = new NewMappingWizardPage(selection);
        addPage(page);
    }

    /**
     * This method is called when 'Finish' button is pressed in the wizard. We
     * will create an operation and run it using wizard as execution context.
     */
    public boolean performFinish() {
        final String containerName = page.getContainerName();
        final List tables = page.getTables();
        final String packageName = page.getPackage();
        final Properties props = page.getProperties();
        Plugin.saveProperty(page.project.getProject(), "package", packageName);
        int foreignKeys = 0;
        for (Iterator i = tables.iterator(); i.hasNext();) {
            DBTable tbl = (DBTable) i.next();
            tbl.setProperties(props);
            foreignKeys += tbl.getForeignKeys().size();
        }
        try {
            Template template = Constants.templateGenerator
                    .getTemplate("Mapping.vm");
            Connection c = page.getConnection(null);
            final MappingWizardRunnable runnable = new MappingWizardRunnable(
                    containerName, tables, packageName, props, template, c,
                    getShell());
            ProgressMonitorDialog dialog = new ProgressMonitorDialog(getShell());
            try {
                dialog.run(false, true, new IRunnableWithProgress() {
                    public void run(IProgressMonitor monitor)
                            throws InvocationTargetException,
                            InterruptedException {
                        try {
                            ResourcesPlugin.getWorkspace().run(runnable,
                                    monitor);
                        } catch (Exception e) {
                            throw new InvocationTargetException(e);
                        } finally {
                            monitor.done();
                        }
                    }
                });
            } catch (Exception e) {
            } finally {
                if (null != c)
                    c.close();
            }
            if (null != runnable.files && runnable.files.length > 0) {
                AddMappingReference.addMappingReference(page.project
                        .getProject(), runnable.files, false, getShell());
            }
        } catch (Exception e) {
            HSUtil.showError(e.getMessage(), getShell());
        }
        return true;
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