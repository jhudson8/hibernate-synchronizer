/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.popup.actions;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.net.NoRouteToHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.Context;
import org.apache.velocity.util.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.xml.sax.SAXParseException;

import com.hudson.hibernatesynchronizer.Constants;
import com.hudson.hibernatesynchronizer.Plugin;
import com.hudson.hibernatesynchronizer.custom.Template;
import com.hudson.hibernatesynchronizer.custom.TemplateManager;
import com.hudson.hibernatesynchronizer.dialog.TemplateGenerationDialog;
import com.hudson.hibernatesynchronizer.dialog.TemplateGenerationParameter;
import com.hudson.hibernatesynchronizer.exception.HibernateSynchronizerException;
import com.hudson.hibernatesynchronizer.obj.HibernateClass;
import com.hudson.hibernatesynchronizer.parser.DOMHelper;
import com.hudson.hibernatesynchronizer.parser.HibernateDOMParser;
import com.hudson.hibernatesynchronizer.util.EditorUtil;
import com.hudson.hibernatesynchronizer.util.HSUtil;
import com.hudson.hibernatesynchronizer.util.ProjectClassLoader;
import com.hudson.hibernatesynchronizer.util.ShallowProjectClassLoader;

/**
 * @author <a href="mailto: joe@binamics.com">Joe Hudson </a>
 */
public class TemplateGeneration implements IObjectActionDelegate {

    private IWorkbenchPart part;

    /**
     * Constructor for Action1.
     */
    public TemplateGeneration() {
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

        try {
            List templates = TemplateManager.getInstance().getTemplates();
            Shell shell = new Shell();
            if (templates.size() == 0) {
                MessageDialog
                        .openError(
                                shell,
                                "Template Generation Error",
                                "You must define templates before you can use this function.\n(Window >> Preferences >> Hibernate Templates)");
                return;
            }
            ISelectionProvider provider = part.getSite().getSelectionProvider();
            if (null != provider) {
                if (provider.getSelection() instanceof StructuredSelection) {
                    StructuredSelection selection = (StructuredSelection) provider
                            .getSelection();
                    Object[] obj = selection.toArray();
                    IFile[] files = new IFile[obj.length];
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
                    TemplateGenerationParameter param = new TemplateGenerationParameter();
                    TemplateGenerationDialog dialog = new TemplateGenerationDialog(
                            shell, JavaCore.create(singleProject), templates,
                            param);
                    int rtn = dialog.open();
                    if (rtn == Dialog.OK) {
                        IJavaProject javaProject = JavaCore
                                .create(singleProject);
                        Template template = param.template;
                        if (template.isJavaClass()) {
                            String packageName = param.container;
                            String className = param.fileName;
                            IPackageFragmentRoot root = HSUtil
                                    .getProjectRoot(javaProject);
                            if (null != root) {
                                IPackageFragment fragment = root
                                        .getPackageFragment(packageName);
                                className = className + Constants.EXT_JAVA;
                                if (null != fragment) {
                                    if (!fragment.exists()) {
                                        fragment = root.createPackageFragment(
                                                packageName, false, null);
                                    }
                                    ICompilationUnit unit = fragment
                                            .getCompilationUnit(className);
                                    Context context = getDefaultContext(
                                            selection, javaProject);
                                    if (null != context) {
                                        StringWriter sw = new StringWriter();
                                        Constants.customGenerator.evaluate(
                                                context, sw, Velocity.class
                                                        .getName(), template
                                                        .getContent());
                                        unit = fragment.createCompilationUnit(
                                                className, sw.toString(), true,
                                                null);
                                        if (unit.getResource() instanceof IFile)
                                            EditorUtil.openPage((IFile) unit
                                                    .getResource());
                                    }
                                }
                            }
                        } else {
                            String path = param.container + "/"
                                    + param.fileName;
                            IFile file = ResourcesPlugin.getWorkspace()
                                    .getRoot().getFile(new Path(path));
                            Context context = getDefaultContext(selection,
                                    javaProject);
                            if (null != context) {
                                StringWriter sw = new StringWriter();
                                Constants.customGenerator.evaluate(context, sw,
                                        Velocity.class.getName(), template
                                                .getContent());
                                if (!file.exists()) {
                                    file
                                            .create(new ByteArrayInputStream(sw
                                                    .toString().getBytes()),
                                                    true, null);
                                } else {
                                    file.delete(true, null);
                                    file
                                            .create(new ByteArrayInputStream(sw
                                                    .toString().getBytes()),
                                                    true, null);
                                }
                                EditorUtil.openPage(file);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            HSUtil.showError(e.getMessage(), new Shell());
        }
    }

    private Context getDefaultContext(StructuredSelection selection,
            IJavaProject project) {
        Context context = new VelocityContext();
        List classes = getAllClasses(selection);
        if (null == classes)
            return null;
        context.put("classes", classes);
        context.put("project", project);
        context.put("hsUtil", new HSUtil());
        if (classes.size() == 1) {
            context.put("class", classes.get(0));
        }
        context.put("now", new Date());
        context.put("stringUtil", new StringUtils());

        try {
            String customObjectStr = Plugin.getProperty(project.getProject(),
                    Constants.PROP_CONTEXT_OBJECT);
            Object contextObject = null;
            if (null != customObjectStr && customObjectStr.trim().length() > 0) {
                ClassLoader loader = new ShallowProjectClassLoader(project,
                        getClass().getClassLoader());
                try {
                    try {
                        contextObject = loader.loadClass(customObjectStr)
                                .newInstance();
                    } catch (ClassNotFoundException e) {
                        loader = new ProjectClassLoader(project, getClass()
                                .getClassLoader());
                        contextObject = loader.loadClass(customObjectStr)
                                .newInstance();
                    }
                } catch (Exception e) {
                }
            }
            if (null != contextObject) {
                if (contextObject instanceof Map) {
                    for (Iterator i = ((Map) contextObject).entrySet()
                            .iterator(); i.hasNext();) {
                        Map.Entry entry = (Map.Entry) i.next();
                        if (null != entry.getKey()) {
                            context.put(entry.getKey().toString(), entry
                                    .getValue());
                        }
                    }
                }
                context.put("obj", contextObject);
            }
        } catch (CoreException e) {
        }
        return context;
    }

    /**
     * @see IActionDelegate#selectionChanged(IAction, ISelection)
     */
    public void selectionChanged(IAction action, ISelection selection) {
    }

    private List getAllClasses(StructuredSelection selection) {
        List allClasses = new ArrayList();
        Object[] obj = selection.toArray();
        IFile[] files = new IFile[obj.length];
        HibernateDOMParser domParser = null;
        boolean errorFound = false;
        for (int i = 0; i < obj.length; i++) {
            if (obj[i] instanceof IFile) {
                IFile file = (IFile) obj[i];
                try {
                    domParser = new HibernateDOMParser(file.getContents());
                    List classes = DOMHelper.getHibernateDocument(domParser,
                            file.getProject()).getClasses();
                    boolean valid = true;
                    for (Iterator iter = classes.iterator(); iter.hasNext();) {
                        HibernateClass hc = (HibernateClass) iter.next();
                        if (null == hc.getPackageName()
                                || hc.getPackageName().length() == 0) {
                            valid = false;
                        }
                    }
                    allClasses.addAll(classes);
                } catch (HibernateSynchronizerException e) {
                    errorFound = true;
                    int lineNumber = e.getLineNumber();
                    if (null != e.getNode()
                            && null != domParser.getLineNumber(e.getNode())) {
                        lineNumber = domParser.getLineNumber(e.getNode())
                                .intValue();
                    }
                    if (lineNumber <= 0) {
                        lineNumber = 1;
                    }
                    EditorUtil.addProblemMarker(file, e.getMessage(),
                            lineNumber);
                } catch (SAXParseException e) {
                    errorFound = true;
                    EditorUtil.addProblemMarker(file, e.getMessage(), e
                            .getLineNumber());
                } catch (NoRouteToHostException nrthe) {
                    errorFound = true;
                    EditorUtil
                            .addProblemMarker(
                                    file,
                                    "A NoRouteToHostException occured while process the synchronization.  Either remove the external namespace and DTD definitions or connect to the internet (or configure your proxy).",
                                    1);
                    MessageDialog
                            .openError(
                                    Display.getCurrent().getActiveShell(),
                                    "An error has occured: NoRouteToHostException",
                                    "This usually occurs if you have namespace references or DTD validation and are either not connected to the internet or do not have your proxy setting correctly configured.\n\nPlease resolve these issues to use the HibernateSynchronizer plugin.");
                } catch (Throwable e) {
                    errorFound = true;
                    EditorUtil.addProblemMarker(file, e.getMessage(), 1);
                    MessageDialog.openError(Display.getCurrent()
                            .getActiveShell(), "An error has occured", e
                            .getMessage());
                    Plugin.logError(e);
                }
            }
        }
        if (errorFound)
            return null;
        else
            return allClasses;
    }
}