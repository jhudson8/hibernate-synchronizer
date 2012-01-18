/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.editor;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.texteditor.ContentAssistAction;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import com.hudson.hibernatesynchronizer.Plugin;
import com.hudson.hibernatesynchronizer.obj.HibernateClass;
import com.hudson.hibernatesynchronizer.obj.HibernateDocument;
import com.hudson.hibernatesynchronizer.outline.HibernateOutlinePage;
import com.hudson.hibernatesynchronizer.parser.ParserThread;
import com.hudson.hibernatesynchronizer.util.ClassInfo;
import com.hudson.hibernatesynchronizer.util.FileCache;
import com.hudson.hibernatesynchronizer.util.Synchronizer;

/**
 * @author <a href="mailto: joe@binamics.com">Joe Hudson </a>
 */
public class HibernateEditor extends TextEditor {

    public static final String PREFIX = "HibernateEditor.";

    private ColorManager colorManager;

    private HibernateOutlinePage outlinePage;

    private HibernateDocument document;

    public HibernateEditor() {
        super();
        colorManager = new ColorManager();
        setSourceViewerConfiguration(new XMLConfiguration(colorManager, this));
        setDocumentProvider(new XMLDocumentProvider());
    }

    public void dispose() {
        colorManager.dispose();
        super.dispose();
    }

    public ISourceViewer getHibernateSourceViewer() {
        return getSourceViewer();
    }

    protected void editorSaved() {
        IFileEditorInput input = (IFileEditorInput) getEditorInput();
        initFile(input.getFile());
        document = Synchronizer.synchronize(input, getSourceViewer(), this,
                outlinePage, Display.getCurrent().getActiveShell());
    }

    protected void doSetInput(IEditorInput input) throws CoreException {
        super.doSetInput(input);
        ParserThread thread = new ParserThread(this, ((IFileEditorInput) input)
                .getFile());
        thread.start();
    }

    protected void createActions() {
        super.createActions();
        // Add content assist propsal action
        ContentAssistAction action = new ContentAssistAction(Plugin
                .getDefault().getResourceBundle(), PREFIX + "ContentAssist.",
                this);
        action
                .setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
        setAction(Plugin.CONTENT_ASSIST, action);
    }

    public Object getAdapter(Class adapter) {

        if (IContentOutlinePage.class.equals(adapter)) {
            if (outlinePage == null) {
                outlinePage = new HibernateOutlinePage(this);
            }
            return outlinePage;
        }
        return super.getAdapter(adapter);
    }

    public HibernateDocument getDocument() {
        if (null == document) {
            document = Synchronizer.synchronize(
                    (IFileEditorInput) getEditorInput(), getSourceViewer(),
                    this, outlinePage, Display.getCurrent().getActiveShell());
        }
        return document;
    }

    public boolean shouldReload(File file) {
        FileCache fc = (FileCache) fileCacheMap.get(file.getAbsolutePath());
        if (null != fc) {
            return (fc.lastModified < file.lastModified());
        } else
            return true;
    }

    public static Map classesMap = new HashMap();

    private static Map fileCacheMap = new HashMap();

    private static Map allClassesMap = new HashMap();

    private static Map externalSubclassesMap = new HashMap();

    public static void cache(IProject project, File file, List classes) {
        allClassesMap.remove(project.getName());
        FileCache fc = new FileCache();
        fc.lastModified = file.lastModified();
        fc.filePath = file.getAbsolutePath();
        List classInfoList = new ArrayList(classes.size());
        Map classesCache = getClassesCache(project);
        for (int i = 0; i < classes.size(); i++) {
            HibernateClass hc = (HibernateClass) classes.get(i);
            ClassInfo ci = new ClassInfo(hc);
            classInfoList.add(ci);
            classesCache.put(hc.getFullClassName(), ci);
        }
        fc.classes = classInfoList;
        Map fileCache = (Map) fileCacheMap.get(project.getName());
        if (null == fileCache) {
            fileCache = new HashMap();
            fileCacheMap.put(project.getName(), fileCache);
        }
        fileCache.put(file.getAbsolutePath(), fc);
    }

    public static void initFile(IFile file) {
    }

    public String[] getAllClassChoices(IProject project) {
        String[] allClasses = (String[]) allClassesMap.get(project.getName());
        if (null == allClasses) {
            Map fileCache = (Map) fileCacheMap.get(project.getName());
            if (null == fileCache) {
                fileCache = new HashMap();
                fileCacheMap.put(project.getName(), fileCache);
            }
            int count = 0;
            for (Iterator i = fileCache.values().iterator(); i.hasNext();) {
                count += ((FileCache) i.next()).classes.size();
            }
            allClasses = new String[count];
            List allClassesList = new ArrayList(count);
            for (Iterator i = fileCache.values().iterator(); i.hasNext();) {
                allClassesList.addAll(((FileCache) i.next()).classes);
            }
            Collections.sort(allClassesList);
            int index = 0;
            for (Iterator i = allClassesList.iterator(); i.hasNext();) {
                ClassInfo ci = (ClassInfo) i.next();
                allClasses[index++] = ci.getFullClassName();
            }
            allClassesMap.put(project.getName(), allClasses);
        }
        return allClasses;
    }

    public static Map getClassesCache(IProject project) {
        Map classes = (Map) classesMap.get(project.getName());
        if (null == classes) {
            classes = new HashMap();
            classesMap.put(project.getName(), classes);
        }
        return classes;
    }

    public IJavaProject getProject() {
        return JavaCore.create(((IFileEditorInput) getEditorInput()).getFile()
                .getProject());
    }
}