/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.util;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.Context;
import org.apache.velocity.util.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.MessageDialog;

import com.hudson.hibernatesynchronizer.Constants;
import com.hudson.hibernatesynchronizer.Plugin;
import com.hudson.hibernatesynchronizer.custom.ProjectTemplate;
import com.hudson.hibernatesynchronizer.custom.Snippet;
import com.hudson.hibernatesynchronizer.custom.SnippetContext;
import com.hudson.hibernatesynchronizer.custom.TemplateManager;
import com.hudson.hibernatesynchronizer.obj.HibernateClass;
import com.hudson.hibernatesynchronizer.obj.HibernateClassCollectionProperty;
import com.hudson.hibernatesynchronizer.obj.HibernateClassProperty;
import com.hudson.hibernatesynchronizer.obj.HibernateComponentClass;

/**
 * @author <a href="mailto: joe@binamics.com">Joe Hudson </a>
 */
public class SynchronizerThread extends Thread {

    public static final String NO_SAVE = ":NoSave";

    public static final String WARNING = ":Warning:";

    public static final String ERROR = ":Error:";

    public static final String FATAL = ":Fatal:";

    private IJavaProject project;

    private IPackageFragmentRoot root;

    private List classes;

    private IFile syncFile;

    private Context context;

    private boolean force;

    private boolean managersEnabled;

    private boolean boEnabled;

    private boolean customEnabled;

    public SynchronizerThread(IJavaProject project, IPackageFragmentRoot root,
            List classes, IFile syncFile, Context context, boolean force) {
        this.project = project;
        this.root = root;
        this.classes = classes;
        this.syncFile = syncFile;
        this.context = context;
        this.force = force;
    }

    public static void synchronize(IJavaProject project,
            IPackageFragmentRoot root, List classes, IProgressMonitor monitor,
            String currentFileName, IFile syncFile, Context context,
            boolean force) {
        try {
            ResourcesPlugin.getWorkspace().run(
                    new SynchronizerRunnable(project, root, classes, syncFile,
                            context, force, currentFileName, monitor), null);
        } catch (CoreException ce) {
            ce.printStackTrace();
        }
    }

    public void run() {
        synchronize(project, root, classes, null, null, syncFile, context,
                force);
    }

    static boolean synchronizeClass(HibernateClass hc,
            IPackageFragmentRoot root, boolean boEnabled,
            boolean managersEnabled, boolean customEnabled,
            IProgressMonitor monitor, String currentFileName, IFile syncFile,
            Context context, boolean force) {
        context.put("class", hc);
        try {
            Snippet snippet = TemplateManager.getInstance().findSnippetByName(
                    "ClassSetup", root.getJavaProject().getProject());
            if (null != snippet) {
                StringWriter sw = new StringWriter();
                Constants.customGenerator.evaluate(context, sw, Velocity.class
                        .getName(), snippet.getContent());
                String trimmed = sw.toString().trim();
                if (trimmed.startsWith(WARNING)) {
                    String message = trimmed.substring(WARNING.length(),
                            trimmed.length());
                    if (null != syncFile) {
                        String key = WARNING + syncFile.getName() + message;
                        if (null == context.get(key)) {
                            context.put(key, Boolean.TRUE);
                            EditorUtil.addWarningMarker(syncFile, message, 0);
                        }
                    }
                } else if (trimmed.startsWith(ERROR)) {
                    String message = trimmed.substring(ERROR.length(), trimmed
                            .length());
                    if (null != syncFile) {
                        String key = ERROR + syncFile.getName() + message;
                        if (null == context.get(key)) {
                            context.put(key, Boolean.TRUE);
                            EditorUtil.addProblemMarker(syncFile, message, 0);
                        }
                    }
                } else if (trimmed.startsWith(FATAL)) {
                    String message = trimmed.substring(FATAL.length(), trimmed
                            .length());
                    if (null != syncFile) {
                        String key = FATAL + syncFile.getName() + message;
                        if (null == context.get(key)) {
                            context.put(key, Boolean.TRUE);
                            EditorUtil.addProblemMarker(syncFile, message, 0);
                        }
                    }
                    return false;
                }
            }
        } catch (Exception e) {
        }

        try {
            if (boEnabled) {
                if (null != monitor)
                    monitor.subTask(currentFileName
                            + ": generating base business object");
                writeClass("BaseClass.vm", context, root, hc
                        .getBasePackageName(), hc.getBaseClassName(), true);
                if (null != monitor)
                    monitor.worked(1);
                if (null != monitor)
                    monitor.subTask(currentFileName
                            + ": generating extension business object");
                writeExtensionClass("Class.vm", "ClassConstructor.vm", context,
                        root, hc.getPackageName(), hc.getClassName());
                if (hc.hasProxy())
                    writeProxyClass("ClassProxy.vm", "ClassProxyContents.vm",
                            context, root, hc.getProxyPackageName(), hc
                                    .getProxyClassName());

                if (null != monitor)
                    monitor.worked(1);

                // subclasses
                if (null != monitor)
                    monitor
                            .subTask(currentFileName
                                    + ": generating subclasses");
                if (hc.getSubclassList().size() > 0) {
                    for (Iterator i2 = hc.getSubclassList().iterator(); i2
                            .hasNext();) {
                        HibernateClass subclass = (HibernateClass) i2.next();
                        if (synchronizeClass(subclass, root, boEnabled,
                                managersEnabled, customEnabled, monitor,
                                currentFileName, syncFile, context, force))
                            return true;
                    }
                    context.put("class", hc);
                }
                if (null != monitor)
                    monitor.worked(1);

                // class components
                if (null != monitor)
                    monitor
                            .subTask(currentFileName
                                    + ": generating components");
                for (Iterator iter = hc.getComponentList().iterator(); iter
                        .hasNext();) {
                    HibernateComponentClass chc = (HibernateComponentClass) iter
                            .next();
                    if (!chc.isDynamic()) {
                        context.put("class", chc);
                        writeClass("BaseClass.vm", context, root, chc
                                .getBasePackageName(), chc.getBaseClassName(),
                                true);
                        writeExtensionClass("Class.vm", "ClassConstructor.vm",
                                context, root, chc.getPackageName(), chc
                                        .getClassName());
                    }
                }
                for (Iterator i = hc.getCollectionList().iterator(); i
                        .hasNext();) {
                    for (Iterator i2 = ((HibernateClassCollectionProperty) i
                            .next()).getCompositeList().iterator(); i2
                            .hasNext();) {
                        HibernateComponentClass chc = (HibernateComponentClass) i2
                                .next();
                        context.put("class", chc);
                        writeClass("BaseClass.vm", context, root, chc
                                .getBasePackageName(), chc.getBaseClassName(),
                                true);
                        writeExtensionClass("Class.vm", "ClassConstructor.vm",
                                context, root, chc.getPackageName(), chc
                                        .getClassName());
                    }
                }
                if (null != monitor)
                    monitor.worked(1);
                context.put("class", hc);
                // composite id
                if (null != monitor)
                    monitor.subTask(currentFileName + ": generating id");
                if (null != hc.getId() && hc.getId().isComposite()
                        && hc.getId().hasExternalClass()) {
                    writeExtensionClass("ClassPK.vm", "PKConstructor.vm",
                            context, root, hc.getId().getProperty()
                                    .getPackageName(), hc.getId().getProperty()
                                    .getClassName());
                    writeClass("BaseClassPK.vm", context, root, hc
                            .getBasePackageName(), "Base"
                            + hc.getId().getProperty().getClassName(), true);
                }
                if (null != monitor)
                    monitor.worked(1);
                // enumeration
                if (null != monitor)
                    monitor.subTask(currentFileName
                            + ": generating enumerations");
                for (Iterator i2 = hc.getProperties().iterator(); i2.hasNext();) {
                    HibernateClassProperty prop = (HibernateClassProperty) i2
                            .next();
                    if (prop.isEnumeration()) {
                        context.put("field", prop);
                        writeClass("Enumeration.vm", context, root, prop
                                .getPackageName(), prop.getClassName(), false);
                    }
                }
                if (null != monitor)
                    monitor.worked(1);
            } else if (null != monitor)
                monitor.worked(6);
            if (managersEnabled && hc.canMakeDAO()) {
                context.put("class", hc);
                if (null != monitor)
                    monitor.subTask(currentFileName + ": generating root DAO");
                if (!hc.usesCustomDAO()) {
                    writeClass("BaseRootDAO.vm", context, root, hc
                            .getBaseDAOPackageName(), hc
                            .getBaseRootDAOClassName(), true);
                }
                writeClass("RootDAO.vm", context, root, hc
                        .getRootDAOPackageName(), hc.getRootDAOClassName(),
                        false);
                if (null != monitor)
                    monitor.worked(1);
                if (null != monitor)
                    monitor.subTask(currentFileName + ": generating base DAO");
                if (null != monitor)
                    monitor.worked(1);
                writeClass("BaseDAO.vm", context, root, hc
                        .getBaseDAOPackageName(), hc.getBaseDAOClassName(),
                        true);
                if (null != monitor)
                    monitor.worked(1);
                if (null != monitor)
                    monitor.subTask(currentFileName
                            + ": generating extension DAO");
                writeClass("DAO.vm", context, root, hc.getDAOPackageName(), hc
                        .getDAOClassName(), false);
                if (null != monitor)
                    monitor.worked(1);
            } else if (null != monitor)
                monitor.worked(3);
            if (customEnabled) {
                List templates = TemplateManager
                        .getInstance()
                        .getProjectTemplates(root.getJavaProject().getProject());
                for (Iterator iter = templates.iterator(); iter.hasNext();) {
                    ProjectTemplate projectTemplate = (ProjectTemplate) iter
                            .next();
                    if (writeCustom(root.getJavaProject().getProject(), root,
                            projectTemplate, hc, context, syncFile, force))
                        return true;
                }
            }
        } catch (Exception e) {
            Plugin.logError(e);
        }
        return false;
    }

    private static void writeExtensionClass(String templateName,
            String constructorTemplateName, Context context,
            IPackageFragmentRoot fragmentRoot, String packageName,
            String className) throws JavaModelException {
        IPackageFragment fragment = fragmentRoot
                .getPackageFragment(packageName);
        className = className + ".java";
        try {
            if (null != fragment) {
                if (!fragment.exists()) {
                    fragment = fragmentRoot.createPackageFragment(packageName,
                            false, null);
                }
                ICompilationUnit unit = fragment.getCompilationUnit(className);
                if (unit.exists()) {
                    String content = unit.getSource();
                    MarkerContents mc = HSUtil.getMarkerContents(content,
                            "CONSTRUCTOR MARKER");
                    if (null != mc) {
                        try {
                            Template template = Constants.templateGenerator
                                    .getTemplate(constructorTemplateName);
                            StringWriter sw = new StringWriter();
                            template.merge(context, sw);
                            if (null != mc.getContents()
                                    && mc.getContents().trim().equals(
                                            sw.toString().trim()))
                                return;
                            content = mc.getPreviousContents() + sw.toString()
                                    + mc.getPostContents();
                            unit = fragment.createCompilationUnit(className,
                                    content, true, null);
                        } catch (JavaModelException e) {
                        }
                    }
                } else {
                    try {
                        Template template = Constants.templateGenerator
                                .getTemplate(constructorTemplateName);
                        StringWriter sw = new StringWriter();
                        template.merge(context, sw);
                        VelocityContext subContext = new VelocityContext(
                                context);
                        subContext.put("constructors", sw.toString());
                        template = Constants.templateGenerator
                                .getTemplate(templateName);
                        sw = new StringWriter();
                        template.merge(subContext, sw);
                        unit = fragment.createCompilationUnit(className, sw
                                .toString(), true, null);
                    } catch (JavaModelException e) {
                    }
                }
            }
        } catch (Exception e) {
            if (e instanceof JavaModelException)
                throw (JavaModelException) e;
            else
                MessageDialog.openWarning(null, "An error has occured: "
                        + e.getClass(), e.getMessage());
        }
    }

    public static void writeClass(String velocityTemplate, Context context,
            IPackageFragmentRoot fragmentRoot, String packageName,
            String className, boolean force) throws JavaModelException {
        IPackageFragment fragment = fragmentRoot
                .getPackageFragment(packageName);
        String fileName = className + ".java";
        try {
            if (null != fragment) {
                if (!fragment.exists()) {
                    fragment = fragmentRoot.createPackageFragment(packageName,
                            false, null);
                }
                ICompilationUnit unit = fragment.getCompilationUnit(fileName);
                if (!unit.exists() && !force) {
                    Template template = Constants.templateGenerator
                            .getTemplate(velocityTemplate);
                    StringWriter sw = new StringWriter();
                    template.merge(context, sw);
                    try {
                        unit = fragment.createCompilationUnit(fileName,
                                addContent(sw.toString(), className), false,
                                null);
                    } catch (JavaModelException e) {
                    }
                } else {
                    if (force) {
                        org.apache.velocity.Template template = Constants.templateGenerator
                                .getTemplate(velocityTemplate);
                        StringWriter sw = new StringWriter();
                        template.merge(context, sw);
                        try {
                            if (unit.exists()) {
                                String existingContent = unit.getBuffer()
                                        .getContents();
                                if (null != existingContent
                                        && existingContent
                                                .equals(sw.toString()))
                                    return;
                            }
                            unit = createCompilationUnit(fragment, unit,
                                    fileName, addContent(sw.toString(),
                                            className), true, null);
                        } catch (JavaModelException e) {
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (e instanceof JavaModelException)
                throw (JavaModelException) e;
            else
                MessageDialog.openWarning(null, "An error has occured: "
                        + e.getClass(), e.getMessage());
        }
    }

    private static void writeProxyClass(String templateName,
            String contentTemplateName, Context context,
            IPackageFragmentRoot fragmentRoot, String packageName,
            String className) throws JavaModelException {
        IPackageFragment fragment = fragmentRoot
                .getPackageFragment(packageName);
        String fileName = className + ".java";
        try {
            if (null != fragment) {
                if (!fragment.exists()) {
                    fragment = fragmentRoot.createPackageFragment(packageName,
                            false, null);
                }
                ICompilationUnit unit = fragment.getCompilationUnit(fileName);
                if (unit.exists()) {
                    String content = unit.getSource();
                    MarkerContents mc = HSUtil.getMarkerContents(content,
                            "GENERATED CONTENT MARKER");
                    if (null != mc) {
                        try {
                            Template template = Constants.templateGenerator
                                    .getTemplate(contentTemplateName);
                            StringWriter sw = new StringWriter();
                            template.merge(context, sw);
                            if (null != mc.getContents()
                                    && mc.getContents().trim().equals(
                                            sw.toString().trim()))
                                return;
                            content = mc.getPreviousContents() + sw.toString()
                                    + mc.getPostContents();
                            createCompilationUnit(fragment, unit, fileName,
                                    content, true, null);
                        } catch (JavaModelException e) {
                        }
                    }
                } else {
                    try {
                        Template template = Constants.templateGenerator
                                .getTemplate(contentTemplateName);
                        StringWriter sw = new StringWriter();
                        template.merge(context, sw);
                        VelocityContext subContext = new VelocityContext(
                                context);
                        subContext.put("contents", sw.toString());
                        template = Constants.templateGenerator
                                .getTemplate(templateName);
                        sw = new StringWriter();
                        template.merge(subContext, sw);
                        unit = fragment.createCompilationUnit(fileName,
                                addContent(sw.toString(), className), true,
                                null);
                    } catch (JavaModelException e) {
                    }
                }
            }
        } catch (Exception e) {
            if (e instanceof JavaModelException)
                throw (JavaModelException) e;
            else
                MessageDialog.openWarning(null, "An error has occured: "
                        + e.getClass(), e.getMessage());
        }
    }

    private static ICompilationUnit createCompilationUnit(
            IPackageFragment fragment, ICompilationUnit currentUnit,
            String fileName, String content, boolean force,
            IProgressMonitor monitor) throws JavaModelException {
        if (null != currentUnit) {
            if (currentUnit.exists()) {
                String existingContent = currentUnit.getBuffer().getContents();
                if (null != existingContent && existingContent.equals(content))
                    return currentUnit;
            }
        }
        return fragment
                .createCompilationUnit(fileName, content, force, monitor);
    }

    private static String addContent(String contents, String className) {
        int commentIndex = contents.indexOf("/*");
        commentIndex = commentIndex - 1;
        int classIndex = contents.indexOf("class " + className);
        if (classIndex < 0) {
            classIndex = contents.indexOf("interface " + className);
        }
        if (classIndex < 0) {
            return addContent(contents, 0, false);
        } else {
            if (commentIndex < classIndex && commentIndex >= 0) {
                int nl = commentIndex;
                while (true) {
                    nl++;
                    if (contents.getBytes()[nl] == '\n')
                        break;
                }
                nl++;
                return addContent(contents, nl, true);
            } else {
                int nl = classIndex;
                while (true) {
                    nl--;
                    if (contents.getBytes()[nl] == '\n')
                        break;
                }
                nl++;
                return addContent(contents, nl, false);
            }
        }
    }

    private static String ct = " * This class has been automatically generated by Hibernate Synchronizer.\n"
            + " * For more information or documentation, visit The Hibernate Synchronizer page\n"
            + " * at http://www.binamics.com/hibernatesync or contact Joe Hudson at joe@binamics.com.\n";

    private static String addContent(String contents, int pos,
            boolean isCommentPart) {
        String p1 = null;
        if (pos > 0)
            p1 = contents.substring(0, pos);
        else
            p1 = "";
        String p2 = contents.substring(pos, contents.length());
        if (isCommentPart) {
            return p1 + ct + " *\n" + p2;
        } else {
            return p1 + "/**\n" + ct + " */\n" + p2;
        }
    }

    private static boolean writeCustom(IProject project,
            IPackageFragmentRoot fragmentRoot, ProjectTemplate projectTemplate,
            HibernateClass hc, Context context, IFile syncFile, boolean force) {
        context.remove("custom_placeholder");
        try {
            if (projectTemplate.getTemplate().isJavaClass()) {
                StringWriter sw = new StringWriter();
                Constants.customGenerator.evaluate(context, sw, Velocity.class
                        .getName(), projectTemplate.getName());
                String className = sw.toString();
                String fileName = className + ".java";
                context.put("className", className);
                sw = new StringWriter();
                Constants.customGenerator.evaluate(context, sw, Velocity.class
                        .getName(), projectTemplate.getLocation());
                String packageName = sw.toString();
                IPackageFragment fragment = fragmentRoot
                        .getPackageFragment(packageName);
                if (null != fragment) {
                    context.put("package", packageName);
                    sw = new StringWriter();
                    Constants.customGenerator.evaluate(context, sw,
                            Velocity.class.getName(), packageName);
                    String location = sw.toString();
                    if (!fragment.exists()) {
                        fragment = fragmentRoot.createPackageFragment(
                                packageName, false, null);
                    }
                    ICompilationUnit unit = fragment
                            .getCompilationUnit(fileName);
                    sw = new StringWriter();
                    Constants.customGenerator.evaluate(context, sw,
                            Velocity.class.getName(), projectTemplate
                                    .getTemplate().getContent());
                    try {
                        String trimmed = sw.toString().trim();
                        if (NO_SAVE.equals(trimmed)) {
                            return false;
                        } else if (trimmed.startsWith(WARNING)) {
                            String message = trimmed.substring(
                                    WARNING.length(), trimmed.length());
                            if (null != syncFile) {
                                String key = WARNING + syncFile.getName()
                                        + message;
                                if (null == context.get(key)) {
                                    context.put(key, Boolean.TRUE);
                                    EditorUtil.addWarningMarker(syncFile,
                                            message, 0);
                                }
                            }
                            return false;
                        } else if (trimmed.startsWith(ERROR)) {
                            String message = trimmed.substring(ERROR.length(),
                                    trimmed.length());
                            if (null != syncFile) {
                                String key = ERROR + syncFile.getName()
                                        + message;
                                if (null == context.get(key)) {
                                    context.put(key, Boolean.TRUE);
                                    EditorUtil.addProblemMarker(syncFile,
                                            message, 0);
                                }
                            }
                            return false;
                        } else if (trimmed.startsWith(FATAL)) {
                            String message = trimmed.substring(FATAL.length(),
                                    trimmed.length());
                            if (null != syncFile) {
                                String key = FATAL + syncFile.getName()
                                        + message;
                                if (null == context.get(key)) {
                                    context.put(key, Boolean.TRUE);
                                    EditorUtil.addProblemMarker(syncFile,
                                            message, 0);
                                }
                            }
                            return true;
                        } else {
                            if (!unit.exists()) {
                                unit = fragment.createCompilationUnit(fileName,
                                        addContent(sw.toString(), className),
                                        false, null);
                            } else {
                                String content = addContent(sw.toString(),
                                        className);
                                if (force || projectTemplate.shouldOverride()) {
                                    unit = createCompilationUnit(fragment,
                                            unit, fileName, content, true, null);
                                }
                            }
                        }
                    } catch (JavaModelException e) {
                    }
                    context.remove("package");
                }
                context.remove("className");
            } else {
                StringWriter sw = new StringWriter();
                Constants.customGenerator.evaluate(context, sw, Velocity.class
                        .getName(), projectTemplate.getName());
                String name = sw.toString();
                sw = new StringWriter();
                Constants.customGenerator.evaluate(context, sw, Velocity.class
                        .getName(), projectTemplate.getLocation());
                String location = sw.toString();
                location = location.replace('\\', '/');
                if (!location.startsWith("/")) {
                    location = "/" + location;
                }
                if (location.endsWith("/")) {
                    location = location.substring(0, location.length() - 1);
                }
                context.put("path", location);
                context.put("fileName", name);
                sw = new StringWriter();
                Constants.customGenerator.evaluate(context, sw, Velocity.class
                        .getName(), projectTemplate.getTemplate().getContent());
                String content = sw.toString();
                String trimmed = sw.toString().trim();
                if (NO_SAVE.equals(trimmed)) {
                    return false;
                } else if (trimmed.startsWith(WARNING)) {
                    String message = trimmed.substring(WARNING.length(),
                            trimmed.length());
                    if (null != syncFile) {
                        String key = WARNING + syncFile.getName() + message;
                        if (null == context.get(key)) {
                            context.put(key, Boolean.TRUE);
                            EditorUtil.addWarningMarker(syncFile, message, 0);
                        }
                    }
                    return false;
                } else if (trimmed.startsWith(ERROR)) {
                    String message = trimmed.substring(ERROR.length(), trimmed
                            .length());
                    if (null != syncFile) {
                        String key = ERROR + syncFile.getName() + message;
                        if (null == context.get(key)) {
                            context.put(key, Boolean.TRUE);
                            EditorUtil.addProblemMarker(syncFile, message, 0);
                        }
                    }
                    return false;
                } else if (trimmed.startsWith(FATAL)) {
                    String message = trimmed.substring(FATAL.length(), trimmed
                            .length());
                    if (null != syncFile) {
                        String key = FATAL + syncFile.getName() + message;
                        if (null == context.get(key)) {
                            context.put(key, Boolean.TRUE);
                            EditorUtil.addProblemMarker(syncFile, message, 0);
                        }
                    }
                    return true;
                } else {
                    IFile file = project.getFile(location + "/" + name);
                    if (!file.exists()) {
                        StringTokenizer st = new StringTokenizer(location, "/");
                        StringBuffer sb = new StringBuffer();
                        while (st.hasMoreTokens()) {
                            if (sb.length() > 0)
                                sb.append('/');
                            sb.append(st.nextToken());
                            IFolder folder = project.getFolder(sb.toString());
                            if (!folder.exists())
                                folder.create(false, true, null);
                        }
                        file.create(new ByteArrayInputStream(sw.toString()
                                .getBytes()),
                                (projectTemplate.shouldOverride() || force),
                                null);
                    } else {
                        if (force || projectTemplate.shouldOverride()) {
                            try {
                                if (file.exists()) {
                                    String existingContent = HSUtil
                                            .getStringFromStream(file
                                                    .getContents());
                                    if (null != existingContent
                                            && existingContent.equals(sw
                                                    .toString()))
                                        return false;
                                }
                            } catch (JavaModelException jme) {
                            }
                            file.setContents(new ByteArrayInputStream(sw
                                    .toString().getBytes()), (projectTemplate
                                    .shouldOverride() || force), false, null);
                        }
                    }
                }
                context.remove("path");
            }
        } catch (Exception e) {
            MessageDialog.openWarning(null,
                    "An error has occured while creating custom template: "
                            + projectTemplate.getTemplate().getName(), e
                            .getMessage());
        }
        return false;
    }

    public static Context getDefaultContext(HibernateClass hc,
            IJavaProject project) throws JavaModelException {
        Object contextObject = null;
        String customObjectStr = Plugin.getProperty(project.getProject(),
                Constants.PROP_CONTEXT_OBJECT);
        if (null != customObjectStr && customObjectStr.trim().length() > 0) {
            if (null != customObjectStr && customObjectStr.trim().length() > 0) {
                ClassLoader loader = new ProjectClassLoader(project,
                        Synchronizer.class.getClassLoader());
                try {
                    contextObject = loader.loadClass(customObjectStr)
                            .newInstance();
                } catch (Exception e) {
                }
            }
        }
        return getDefaultContext(hc, project, contextObject);
    }

    public static Context getDefaultContext(HibernateClass hc,
            IJavaProject project, Object contextObject) {
        Context context = new VelocityContext();
        context.put("now", new Date());
        context.put("project", project);
        context.put("dollar", "$");
        context.put("notDollar", "$!");

        context.put("snippet",
                new SnippetContext(context, project.getProject()));
        String useCustomManager = Plugin.getProperty(project.getProject(),
                Constants.PROP_USE_CUSTOM_ROOT_DAO);
        if (null != useCustomManager
                && useCustomManager.equalsIgnoreCase(Boolean.TRUE.toString())) {
            String daoException = Plugin.getProperty(project.getProject(),
                    Constants.PROP_BASE_DAO_EXCEPTION);
            if (null != daoException && daoException.trim().length() > 0) {
                context.put("exceptionClass", daoException.trim());
            } else {
                context.put("exceptionClass",
                        "net.sf.hibernate.HibernateException");
            }
        } else {
            context
                    .put("exceptionClass",
                            "net.sf.hibernate.HibernateException");
        }
        if (null != hc)
            context.put("class", hc);
        context.put("hsUtil", new HSUtil());
        context.put("stringUtil", new StringUtils());
        try {
            Map properties = TemplateManager.getInstance()
                    .getTemplateParameters(project.getProject());
            for (Iterator i = properties.entrySet().iterator(); i.hasNext();) {
                Map.Entry entry = (Map.Entry) i.next();
                context.put((String) entry.getKey(), entry.getValue());
            }
        } catch (CoreException ce) {
        }
        if (null != contextObject) {
            if (contextObject instanceof Map) {
                for (Iterator i = ((Map) contextObject).entrySet().iterator(); i
                        .hasNext();) {
                    Map.Entry entry = (Map.Entry) i.next();
                    if (null != entry.getKey()) {
                        context
                                .put(entry.getKey().toString(), entry
                                        .getValue());
                    }
                }
            }
            context.put("obj", contextObject);
        }
        return context;
    }

    public static void removeReferences(String currentFileName,
            HibernateClass hc, IPackageFragmentRoot root,
            IProgressMonitor monitor) {
        Context context = getDefaultContext(hc, root.getJavaProject(), null);
        try {
            Snippet snippet = TemplateManager.getInstance().findSnippetByName(
                    "Setup", root.getJavaProject().getProject());
            if (null != snippet) {
                StringWriter sw = new StringWriter();
                Constants.customGenerator.evaluate(context, sw, Velocity.class
                        .getName(), snippet.getContent());
            }
        } catch (Exception e) {
        }
        if (null != monitor)
            monitor
                    .subTask(currentFileName
                            + ": removing base business object");
        removeClass(root, hc.getBasePackageName(), hc.getBaseClassName());
        if (null != monitor)
            monitor.worked(1);
        if (null != monitor)
            monitor.subTask(currentFileName
                    + ": removing extension business object");
        removeClass(root, hc.getPackageName(), hc.getClassName());
        if (hc.hasProxy())
            removeClass(root, hc.getProxyPackageName(), hc.getProxyClassName());
        if (null != monitor)
            monitor.worked(1);

        // subclasses
        if (null != monitor)
            monitor.subTask(currentFileName + ": removing subclasses");
        if (hc.getSubclassList().size() > 0) {
            for (Iterator i2 = hc.getSubclassList().iterator(); i2.hasNext();) {
                HibernateClass subclass = (HibernateClass) i2.next();
                removeReferences(currentFileName, subclass, root, monitor);
            }
        }
        if (null != monitor)
            monitor.worked(1);

        // class components
        if (null != monitor)
            monitor.subTask(currentFileName + ": removing components");
        for (Iterator iter = hc.getComponentList().iterator(); iter.hasNext();) {
            HibernateComponentClass chc = (HibernateComponentClass) iter.next();
            removeClass(root, chc.getBasePackageName(), chc.getBaseClassName());
            removeClass(root, chc.getPackageName(), chc.getClassName());
        }
        if (null != monitor)
            monitor.worked(1);
        context.put("class", hc);
        // composite id
        if (null != monitor)
            monitor.subTask(currentFileName + ": removing id");
        if (null != hc.getId() && hc.getId().isComposite()) {
            removeClass(root, hc.getId().getProperty().getPackageName(), hc
                    .getId().getProperty().getClassName());
            removeClass(root, hc.getBasePackageName(), "Base"
                    + hc.getId().getProperty().getClassName());
        }
        if (null != monitor)
            monitor.worked(1);
        // enumeration
        if (null != monitor)
            monitor.subTask(currentFileName + ": removing enumerations");
        for (Iterator i2 = hc.getProperties().iterator(); i2.hasNext();) {
            HibernateClassProperty prop = (HibernateClassProperty) i2.next();
            if (prop.isEnumeration()) {
                removeClass(root, prop.getPackageName(), prop.getClassName());
            }
        }
        if (null != monitor)
            monitor.worked(1);

        if (null != monitor)
            monitor.subTask(currentFileName + ": removing base DAO");
        if (null != monitor)
            monitor.worked(1);
        removeClass(root, hc.getBaseDAOPackageName(), hc.getBaseDAOClassName());
        if (null != monitor)
            monitor.worked(1);
        if (null != monitor)
            monitor.subTask(currentFileName + ": removing extension DAO");
        removeClass(root, hc.getDAOPackageName(), hc.getDAOClassName());
        if (null != monitor)
            monitor.worked(1);

        try {
            List templates = TemplateManager.getInstance().getProjectTemplates(
                    root.getJavaProject().getProject());
            for (Iterator iter = templates.iterator(); iter.hasNext();) {
                ProjectTemplate projectTemplate = (ProjectTemplate) iter.next();
                removeCustom(root.getJavaProject().getProject(), root,
                        projectTemplate, hc, context);
            }
        } catch (CoreException ce) {
        }
    }

    private static void removeClass(IPackageFragmentRoot fragmentRoot,
            String packageName, String className) {
        IPackageFragment fragment = fragmentRoot
                .getPackageFragment(packageName);
        className = className + ".java";

        if (null != fragment) {
            if (!fragment.exists()) {
                return;
            }
            try {
                ICompilationUnit unit = fragment.getCompilationUnit(className);
                if (unit.exists()) {
                    unit.delete(true, null);
                }
            } catch (JavaModelException jme) {
            }
        }
    }

    private static void removeCustom(IProject project,
            IPackageFragmentRoot fragmentRoot, ProjectTemplate projectTemplate,
            HibernateClass hc, Context context) {
        try {
            if (projectTemplate.getName().indexOf("$class") >= 0
                    || projectTemplate.getName().indexOf("${class") >= 0
                    || projectTemplate.getName().indexOf("$!class") >= 0
                    || projectTemplate.getName().indexOf("$!{class") >= 0) {

                StringWriter sw = new StringWriter();
                Constants.customGenerator.evaluate(context, sw, Velocity.class
                        .getName(), projectTemplate.getName());
                String fileName = sw.toString();
                sw = new StringWriter();
                Constants.customGenerator.evaluate(context, sw, Velocity.class
                        .getName(), projectTemplate.getLocation());
                String location = sw.toString();
                if (projectTemplate.getTemplate().isJavaClass()) {
                    fileName = fileName + Constants.EXT_JAVA;
                    IPackageFragment fragment = fragmentRoot
                            .getPackageFragment(location);
                    if (fragment.exists()) {
                        fragment = fragmentRoot.createPackageFragment(location,
                                false, null);
                        ICompilationUnit unit = fragment
                                .getCompilationUnit(fileName);
                        if (unit.exists()) {
                            unit.delete(true, null);
                        }
                    }
                } else {
                    IFile file = project.getFile(location + "/" + fileName);
                    if (file.exists()) {
                        file.delete(true, true, null);
                    }
                }
            }
        } catch (Exception e) {
        }
    }
}