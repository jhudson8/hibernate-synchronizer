/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.popup.actions;

import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.NoRouteToHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.Context;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.w3c.dom.Document;
import org.xml.sax.SAXParseException;

import com.hudson.hibernatesynchronizer.Constants;
import com.hudson.hibernatesynchronizer.Plugin;
import com.hudson.hibernatesynchronizer.custom.Snippet;
import com.hudson.hibernatesynchronizer.custom.TemplateManager;
import com.hudson.hibernatesynchronizer.editor.HibernateEditor;
import com.hudson.hibernatesynchronizer.exception.HibernateSynchronizerException;
import com.hudson.hibernatesynchronizer.obj.HibernateClass;
import com.hudson.hibernatesynchronizer.obj.HibernateDocument;
import com.hudson.hibernatesynchronizer.parser.DOMHelper;
import com.hudson.hibernatesynchronizer.parser.HibernateDOMParser;
import com.hudson.hibernatesynchronizer.util.EditorUtil;
import com.hudson.hibernatesynchronizer.util.HSUtil;
import com.hudson.hibernatesynchronizer.util.ProjectClassLoader;
import com.hudson.hibernatesynchronizer.util.SynchronizerThread;

/**
 * @author <a href="mailto: joe@binamics.com">Joe Hudson </a>
 */
public class SynchronizeFiles implements IObjectActionDelegate {

    private IWorkbenchPart part;

    /**
     * Constructor for Action1.
     */
    public SynchronizeFiles() {
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
        Shell shell = new Shell();
        final ISelectionProvider provider = part.getSite()
                .getSelectionProvider();
        if (null != provider) {
            if (provider.getSelection() instanceof StructuredSelection) {
                ProgressMonitorDialog dialog = new ProgressMonitorDialog(part
                        .getSite().getShell());
                try {
                    dialog.run(false, true, new IRunnableWithProgress() {
                        public void run(IProgressMonitor monitor)
                                throws InvocationTargetException,
                                InterruptedException {
                            try {
                                IStructuredSelection selection = (StructuredSelection) provider
                                        .getSelection();
                                monitor.beginTask("Synchronizing files...",
                                        2 + (selection.toArray().length * 14));
                                exec(selection, monitor);
                            } catch (Exception e) {
                                throw new InvocationTargetException(e);
                            } finally {
                                monitor.done();
                            }
                        }
                    });
                } catch (Exception e) {
                }
            }
        }
    }

    private void exec(IStructuredSelection selection, IProgressMonitor monitor) {
        Shell shell = new Shell();
        Object[] obj = selection.toArray();
        HibernateDOMParser domParser = null;
        Map fileMap = new HashMap();
        IProject project = null;

        Map documents = new HashMap();
        for (int i = 0; i < obj.length && !monitor.isCanceled(); i++) {
            if (obj[i] instanceof IFile) {
                IFile file = (IFile) obj[i];
                HibernateEditor.initFile(file);
                project = file.getProject();
                try {
                    monitor.subTask(file.getName() + ": creating DOM");
                    domParser = new HibernateDOMParser(file.getContents());
                    Document doc = domParser.getDocument();
                    HibernateDocument hd = DOMHelper.getHibernateDocument(
                            domParser, file.getProject());
                    documents.put(file, hd);
                    List classes = hd.getClasses();
                    HibernateEditor.cache(file.getProject(), file.getFullPath()
                            .toFile(), classes);
                    monitor.worked(1);
                } catch (SAXParseException e) {
                    EditorUtil.addProblemMarker(file, e.getMessage(), e
                            .getLineNumber());
                } catch (NoRouteToHostException nrthe) {
                    EditorUtil
                            .addProblemMarker(
                                    file,
                                    "This is not synchronized with the Java model.  A NoRouteToHostException occured while process the synchronization.  Either remove the external namespace and DTD definitions or connect to the internet (or configure your proxy).",
                                    1);
                    MessageDialog
                            .openError(
                                    shell,
                                    "An error has occured: NoRouteToHostException",
                                    "This usually occurs if you have namespace references or DTD validation and are either not connected to the internet or do not have your proxy setting correctly configured.\n\nPlease resolve these issues to use the HibernateSynchronizer plugin.");
                } catch (Throwable e) {
                    EditorUtil.addProblemMarker(file, e.getMessage(), 1);
                    MessageDialog.openError(shell, "An error has occured", e
                            .getMessage());
                    Plugin.logError(e);
                }
            }
        }
        if (null == project)
            return;
        boolean valid = true;
        try {
            IJavaProject javaProject = JavaCore.create(project);
            IPackageFragmentRoot root = HSUtil.getProjectRoot(javaProject);
            if (valid && null != root) {
                Object contextObject = null;
                String customObjectStr = Plugin.getProperty(javaProject
                        .getProject(), Constants.PROP_CONTEXT_OBJECT);
                if (null != customObjectStr
                        && customObjectStr.trim().length() > 0) {
                    ClassLoader loader = new ProjectClassLoader(javaProject,
                            getClass().getClassLoader());
                    try {
                        contextObject = loader.loadClass(customObjectStr)
                                .newInstance();
                    } catch (Exception e) {
                    }
                }
                Context context = SynchronizerThread.getDefaultContext(null,
                        javaProject, contextObject);
                try {
                    Snippet snippet = TemplateManager.getInstance()
                            .findSnippetByName("Setup",
                                    javaProject.getProject());
                    if (null != snippet) {
                        StringWriter sw = new StringWriter();
                        Constants.customGenerator.evaluate(context, sw,
                                Velocity.class.getName(), snippet.getContent());
                        String trimmed = sw.toString().trim();
                        if (trimmed.startsWith(SynchronizerThread.WARNING)) {
                            String message = trimmed.substring(
                                    SynchronizerThread.WARNING.length(),
                                    trimmed.length());
                            MessageDialog
                                    .openWarning(shell, "Warning", message);
                        } else if (trimmed.startsWith(SynchronizerThread.ERROR)) {
                            String message = trimmed.substring(
                                    SynchronizerThread.ERROR.length(), trimmed
                                            .length());
                            MessageDialog.openError(shell,
                                    "An error has occured", message);
                            valid = false;
                        } else if (trimmed.startsWith(SynchronizerThread.FATAL)) {
                            String message = trimmed.substring(
                                    SynchronizerThread.FATAL.length(), trimmed
                                            .length());
                            MessageDialog.openError(shell,
                                    "An error has occured", message);
                            valid = false;
                        }
                    }
                } catch (Exception e) {
                }
                if (valid) {
                    for (int i = 0; i < obj.length && !monitor.isCanceled(); i++) {
                        if (obj[i] instanceof IFile) {
                            IFile file = (IFile) obj[i];
                            try {
                                file.deleteMarkers(null, false, 1);
                                monitor
                                        .subTask(file.getName()
                                                + ": retrieving hibernate class objects");
                                HibernateDocument hd = (HibernateDocument) documents
                                        .get(file);
                                monitor.worked(1);
                                if (null != hd) {
                                    List classes = hd.getClasses();
                                    for (Iterator iter = classes.iterator(); iter
                                            .hasNext();) {
                                        HibernateClass hc = (HibernateClass) iter
                                                .next();
                                        fileMap.put(hc, file);
                                        if (null == hc.getPackageName()
                                                || hc.getPackageName().length() == 0) {
                                            valid = false;
                                        }
                                    }
                                    SynchronizerThread.synchronize(javaProject,
                                            root, classes, monitor, file
                                                    .getName(), file, context,
                                            force());
                                }
                            } catch (HibernateSynchronizerException e) {
                                int lineNumber = e.getLineNumber();
                                if (null != e.getNode()
                                        && null != domParser.getLineNumber(e
                                                .getNode())) {
                                    lineNumber = domParser.getLineNumber(
                                            e.getNode()).intValue();
                                }
                                if (lineNumber <= 0) {
                                    lineNumber = 1;
                                }
                                EditorUtil.addProblemMarker(file, e
                                        .getMessage(), lineNumber);
                            } catch (Throwable e) {
                                EditorUtil.addProblemMarker(file, e
                                        .getMessage(), 1);
                                MessageDialog.openError(shell,
                                        "An error has occured", e.getMessage());
                                Plugin.logError(e);
                            }
                        }
                    }
                }
            }
        } catch (CoreException ce) {
            Plugin.logError(ce);
        }
    }

    /**
     * @see IActionDelegate#selectionChanged(IAction, ISelection)
     */
    public void selectionChanged(IAction action, ISelection selection) {
    }

    public boolean force() {
        return false;
    }
}