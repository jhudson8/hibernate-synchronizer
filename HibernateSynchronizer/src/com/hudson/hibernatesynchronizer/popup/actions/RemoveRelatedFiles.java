/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.popup.actions;

import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.hudson.hibernatesynchronizer.obj.HibernateClass;
import com.hudson.hibernatesynchronizer.obj.HibernateDocument;
import com.hudson.hibernatesynchronizer.parser.DOMHelper;
import com.hudson.hibernatesynchronizer.parser.HibernateDOMParser;
import com.hudson.hibernatesynchronizer.util.HSUtil;
import com.hudson.hibernatesynchronizer.util.SynchronizerThread;
import com.hudson.hibernatesynchronizer.util.XMLHelper;

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
                        "Are you sure you want to delete all related class and resources to the selected mapping files?")) {
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
                            dialog.run(false, true,
                                    new IRunnableWithProgress() {
                                        public void run(IProgressMonitor monitor)
                                                throws InvocationTargetException,
                                                InterruptedException {
                                            try {
                                                RemoveRelatedFilesRunnable runnable = new RemoveRelatedFilesRunnable(
                                                        project, files,
                                                        monitor, shell);
                                                ResourcesPlugin.getWorkspace()
                                                        .run(runnable, monitor);
                                            } catch (Exception e) {
                                                throw new InvocationTargetException(
                                                        e);
                                            } finally {
                                                monitor.done();
                                            }
                                        }
                                    });
                        } catch (Exception e) {
                        }
                    } else {
                        HSUtil
                                .showError(
                                        "The selected files must belong to a single project",
                                        shell);
                    }
                }
            }
        }
    }

    public static void removeRelatedFiles(IProject project, IFile[] files,
            Shell shell, IProgressMonitor monitor) {
        for (int i = 0; i < files.length; i++) {
            try {
                HibernateDOMParser domParser = new HibernateDOMParser(files[i]
                        .getContents());
                HibernateDocument doc = DOMHelper.getHibernateDocument(
                        domParser, project);
                IPackageFragmentRoot root = HSUtil.getProjectRoot(JavaCore
                        .create(project));
                if (null != monitor)
                    monitor.beginTask("Removing Files",
                            doc.getClasses().size() * 10);
                for (Iterator iter = doc.getClasses().iterator(); iter
                        .hasNext();) {
                    HibernateClass hc = (HibernateClass) iter.next();
                    SynchronizerThread.removeReferences(files[i].getName(), hc,
                            root, monitor);
                    if (files[i].exists()) {
                        files[i].delete(true, true, null);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @see IActionDelegate#selectionChanged(IAction, ISelection)
     */
    public void selectionChanged(IAction action, ISelection selection) {
    }

    private static Node addMapping(String mappingName, Node configNode,
            Node lastMappingNode, Document doc) {
        Element newElement = doc.createElement("mapping");
        newElement.setAttribute("resource", mappingName);
        configNode.appendChild(newElement);
        return newElement;
    }

    private static String getFileDirectory(String fileName) {
        if (null == fileName)
            return null;
        fileName = fileName.replace('\\', '/');
        int index = fileName.lastIndexOf('/');
        if (index >= 0) {
            return fileName.substring(0, index);
        } else
            return fileName;
    }

    private static String getDirectoryDifference(String configFile,
            String mapFile) {
        String confDir = getFileDirectory(configFile);
        String mapDir = getFileDirectory(mapFile);
        if (mapDir.startsWith(confDir)) {
            if (mapDir.length() > confDir.length()) {
                return mapDir.substring(confDir.length() + 1, mapDir.length())
                        .replace('\\', '/');
            } else {
                return "";
            }
        } else {
            return null;
        }
    }

    public static String getDocumentContents(Document doc,
            HibernateDOMParser parser, InputStream contents) throws Exception {
        StringWriter sw = new StringWriter();
        XMLHelper xmlHelper = new XMLHelper(new PrintWriter(sw));
        xmlHelper.printTree(doc.getDocumentElement(), parser, contents);
        return sw.toString();
    }

    public class RemoveRelatedFilesRunnable implements IWorkspaceRunnable {
        private IProject project;

        private IFile[] files;

        private IProgressMonitor monitor;

        private Shell shell;

        public RemoveRelatedFilesRunnable(IProject project, IFile[] files,
                IProgressMonitor monitor, Shell shell) {
            this.project = project;
            this.files = files;
            this.monitor = monitor;
            this.shell = shell;
        }

        public void run(IProgressMonitor noUse) throws CoreException {
            RemoveRelatedFiles.removeRelatedFiles(project, files, shell,
                    monitor);
        }
    }
}