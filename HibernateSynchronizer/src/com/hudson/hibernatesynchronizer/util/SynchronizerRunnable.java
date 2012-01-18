/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.util;

import java.util.Iterator;
import java.util.List;

import org.apache.velocity.context.Context;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import com.hudson.hibernatesynchronizer.Constants;
import com.hudson.hibernatesynchronizer.Plugin;
import com.hudson.hibernatesynchronizer.obj.HibernateClass;
import com.hudson.hibernatesynchronizer.preferences.TemplatePreferences;

/**
 * @author <a href="mailto: joe@binamics.com">Joe Hudson </a>
 */
public class SynchronizerRunnable implements IWorkspaceRunnable {
    private IJavaProject project;

    private IPackageFragmentRoot root;

    private List classes;

    private IFile syncFile;

    private Context context;

    private boolean force;

    private String currentFileName;

    private IProgressMonitor monitor;

    public SynchronizerRunnable(IJavaProject project,
            IPackageFragmentRoot root, List classes, IFile syncFile,
            Context context, boolean force, String currentFileName,
            IProgressMonitor monitor) {
        this.project = project;
        this.root = root;
        this.classes = classes;
        this.syncFile = syncFile;
        this.context = context;
        this.force = force;
        this.currentFileName = currentFileName;
        this.monitor = monitor;
    }

    public void run(IProgressMonitor noUse) throws CoreException {
        if (null != monitor)
            monitor.subTask(currentFileName + ": initializing state");
        boolean managersEnabled = (!Boolean.FALSE.toString().equals(
                Plugin.getProperty(project.getProject(),
                        Constants.PROP_DAO_ENABLED)));
        boolean boEnabled = (!Boolean.FALSE.toString().equals(
                Plugin.getProperty(project.getProject(),
                        Constants.PROP_BO_ENABLED)));
        String customEnabledStr = Plugin.getDefault().getPreferenceStore()
                .getString(TemplatePreferences.P_CUSTOM_TEMPLATES_ENABLED);
        boolean customEnabled = true;
        if (null != customEnabledStr && customEnabledStr.trim().length() > 0) {
            try {
                customEnabled = new Boolean(customEnabledStr).booleanValue();
            } catch (Exception e) {
            }
        }
        if (null != monitor)
            monitor.worked(1);

        if (null != monitor)
            monitor.subTask(currentFileName + ": building project ClassLoader");
        if (null != monitor)
            monitor.worked(1);
        for (Iterator i = classes.iterator(); i.hasNext();) {
            if (SynchronizerThread.synchronizeClass((HibernateClass) i.next(),
                    root, boEnabled, managersEnabled, customEnabled, monitor,
                    currentFileName, syncFile, context, force))
                return;
        }
    }
}