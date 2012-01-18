/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.editors.synchronizer.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import com.hudson.hibernatesynchronizer.Plugin;
import com.hudson.hibernatesynchronizer.mapping.HibernateDocument;
import com.hudson.hibernatesynchronizer.mapping.HibernateMappingManager;
import com.hudson.hibernatesynchronizer.util.Synchronizer;
import com.hudson.hibernatesynchronizer.util.UIUtil;

/**
 * @author <a href="mailto: joe@binamics.com">Joe Hudson </a>
 */
public class RemoveRelatedFiles implements IObjectActionDelegate {

    private IWorkbenchPart part;

    /**
     * Constructor for Action1.
     */
    public RemoveRelatedFiles() {
        super();
    }

    /**
     * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
     */
    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        this.part = targetPart;
    }

    /**
     * @see IActionDelegate#run(IAction)
     */
    public void run(IAction action) {
        final Shell shell = new Shell();
        if (MessageDialog
                .openConfirm(
                        shell,
                        "File Removal Confirmation",
                        "Are you sure you want to delete all related classes and resources to the selected mapping files?")) {
            ISelectionProvider provider = part.getSite().getSelectionProvider();
            if (null != provider) {
                if (provider.getSelection() instanceof StructuredSelection) {
                    StructuredSelection selection = (StructuredSelection) provider
                            .getSelection();
                    Object[] obj = selection.toArray();
                    final IFile[] files = new IFile[obj.length];
                    IProject singleProject = null;
                    boolean isSingleProject = true;
                    for (int i = 0; i < obj.length; i++) {
                        if (obj[i] instanceof IFile) {
                            IFile file = (IFile) obj[i];
                            files[i] = file;
                            if (null == singleProject)
                                singleProject = file.getProject();
                            if (!singleProject.getName().equals(
                                    file.getProject().getName())) {
                                isSingleProject = false;
                            }
                        }
                    }
                    if (isSingleProject) {
                        final IProject project = singleProject;
                        ProgressMonitorDialog dialog = new ProgressMonitorDialog(
                                part.getSite().getShell());
                        try {
                            dialog.open();
                            
                            ResourcesPlugin.getWorkspace().run(
                            		new IWorkspaceRunnable() {
		                                public void run(IProgressMonitor monitor) throws CoreException {
				                            try {
				                                removeRelatedFiles(project, files, shell, monitor);
				                            } catch (Exception e) {
				                            	throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, IStatus.OK, e.getMessage(), e));
				                            } finally {
				                                monitor.done();
				                            }
		                                }
		                        	},
									dialog.getProgressMonitor());
                        }
                        catch (Exception e) {
                        	UIUtil.pluginError(e, shell);
                        }
                        finally {
                        	dialog.close();
                        }
                    } else {
                    	UIUtil.pluginError("SingleProjectSelectedFiles", shell);
                    }
                }
            }
        }
    }

    public static void removeRelatedFiles(IProject project, IFile[] files,
            Shell shell, IProgressMonitor monitor) throws Exception {
    	List documents = new ArrayList();
        for (int i = 0; i < files.length; i++) {
        	HibernateMappingManager.getInstance(project).notifyMappingFile(files[i]);
        	HibernateDocument doc = HibernateMappingManager.getInstance(project).getHibernateDocument(files[i]);
        	if (null != doc) documents.add(doc);
        }
        HibernateDocument[] docArr = (HibernateDocument[]) documents.toArray(new HibernateDocument[documents.size()]);
    	Synchronizer synchronizer = new Synchronizer(docArr, true, monitor, shell);
    	synchronizer.removeFiles(false);
    }

    /**
     * @see IActionDelegate#selectionChanged(IAction, ISelection)
     */
    public void selectionChanged(IAction action, ISelection selection) {
    }
}