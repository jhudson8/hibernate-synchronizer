package com.hudson.hibernatesynchronizer.editors.synchronizer.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import com.hudson.hibernatesynchronizer.Plugin;
import com.hudson.hibernatesynchronizer.mapping.HibernateDocument;
import com.hudson.hibernatesynchronizer.mapping.HibernateMappingManager;
import com.hudson.hibernatesynchronizer.util.Synchronizer;
import com.hudson.hibernatesynchronizer.util.UIUtil;

/**
 * @author Joe Hudson
 */
public class SynchronizeFiles implements IObjectActionDelegate {

    private IWorkbenchPart part;

    public void run(IAction action) {
        final Shell shell = new Shell();
        ISelectionProvider provider = part.getSite().getSelectionProvider();
        if (null != provider) {
            if (provider.getSelection() instanceof IStructuredSelection) {
                try {
                    IStructuredSelection selection = (IStructuredSelection) provider.getSelection();
                    Object[] obj = selection.toArray();
                    List documents = new ArrayList();
                    for (int i=0; i<obj.length; i++) {
                        if (obj[i] instanceof IFile) {
                            IFile file = (IFile) obj[i];
                            file.deleteMarkers(null, true, IResource.DEPTH_ZERO);
                            HibernateMappingManager.getInstance(file.getProject()).notifyMappingFile(file);
                            HibernateDocument doc = HibernateMappingManager.getInstance(file.getProject()).getHibernateDocument(file);
                            if (null != doc) documents.add(doc);
                        }
                    }
                    final HibernateDocument[] documentsArr = new HibernateDocument[documents.size()];
                    int index = 0;
                    for (Iterator i=documents.iterator(); i.hasNext(); index++) {
                        documentsArr[index] = (HibernateDocument) i.next();
                    }
                    ProgressMonitorDialog dialog = new ProgressMonitorDialog(part.getSite().getShell());
                    dialog.setCancelable(true);
                    dialog.open();
                    try {
                        ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
                            public void run(IProgressMonitor monitor)
                                    throws CoreException {
                                try {
                                    monitor.beginTask("Synchronizing files...", 2 + (documentsArr.length * 8));
                                    Synchronizer synchronizer = new Synchronizer(documentsArr, shouldForce(), monitor, shell);
                                    synchronizer.synchronize();
                                } catch (Exception e) {
                                	throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, IStatus.OK, e.getMessage(), e));
                                } finally {
                                    monitor.done();
                                }
                            }
                        },
                        dialog.getProgressMonitor());
                    } catch (Exception e) {
                        Plugin.log(e);
                        UIUtil.pluginError(e, shell);
                    }
                    finally {
                    	dialog.close();
                    }
                }
                catch (Exception e) {
                    UIUtil.pluginError(e, shell);
                }
            }
        }
    }

    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        this.part = targetPart;
    }

    public void selectionChanged(IAction action, ISelection selection) {
    }
   
    protected boolean shouldForce () {
        return false;
    }
}
