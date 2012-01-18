package com.hudson.hibernatesynchronizer.editors.synchronizer.actions;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import com.hudson.hibernatesynchronizer.Plugin;
import com.hudson.hibernatesynchronizer.dialogs.DialectSelectorDialog;
import com.hudson.hibernatesynchronizer.mapping.HibernateDocument;
import com.hudson.hibernatesynchronizer.mapping.HibernateMappingManager;
import com.hudson.hibernatesynchronizer.util.EditorUtil;
import com.hudson.hibernatesynchronizer.util.ProjectClassLoader;
import com.hudson.hibernatesynchronizer.util.UIUtil;

/**
 * @author Joe Hudson
 */
public class HBM2DDL implements IObjectActionDelegate {

    private IWorkbenchPart part;

    public void run(IAction action) {
        final Shell shell = new Shell();
        ISelectionProvider provider = part.getSite().getSelectionProvider();
        if (null != provider) {
            if (provider.getSelection() instanceof IStructuredSelection) {
                try {
                	IProject project = null;
                    IStructuredSelection selection = (IStructuredSelection) provider.getSelection();
                    Object[] obj = selection.toArray();
                    List documents = new ArrayList();
                    for (int i=0; i<obj.length; i++) {
                        if (obj[i] instanceof IFile) {
                            IFile file = (IFile) obj[i];
                            file.deleteMarkers(null, true, IResource.DEPTH_ZERO);
                            HibernateMappingManager.getInstance(file.getProject()).notifyMappingFile(file);
                            HibernateDocument doc = HibernateMappingManager.getInstance(file.getProject()).getHibernateDocument(file);
                            if (null != doc) {
                            	project = file.getProject();
                            	documents.add(doc);
                            }
                        }
                    }
                    
                    try {
                    	DialectSelectorDialog dialectDialog = new DialectSelectorDialog(shell);
                    	if (dialectDialog.open() == IDialogConstants.OK_ID) {
                    		FileDialog fileDialog = new FileDialog(shell);
                    		String fileName = fileDialog.open();
                    		if (null != fileName) {
			                    ProjectClassLoader cl = new ProjectClassLoader(JavaCore.create(project));
			                    Object configuration =  cl.loadClass("net.sf.hibernate.cfg.Configuration").newInstance();
			                    configuration.getClass().getMethod(
			                    		"setProperty",
										new Class[]{String.class, String.class}).invoke(
												configuration,
												new Object[]{"hibernate.dialect", dialectDialog.getDialect()});
			                    
			                    for (Iterator i=documents.iterator(); i.hasNext(); ) {
			                    	HibernateDocument doc = (HibernateDocument) i.next();
			                    	configuration.getClass().getMethod(
			                    			"addFile",
											new Class[]{File.class}).invoke(
													configuration,
													new Object[]{doc.getFile().getLocation().makeAbsolute().toFile()});
			                    }
			                    Object schemaExport = cl.loadClass(
			                    		"net.sf.hibernate.tool.hbm2ddl.SchemaExport").getDeclaredConstructor(
			                    				new Class[]{configuration.getClass()}).newInstance(new Object[]{configuration});
			                    schemaExport.getClass().getMethod(
			                    		"setOutputFile",
										new Class[]{String.class}).invoke(
												schemaExport,
												new Object[]{fileName});
			                    
			                    Method method = null;
			                    for (int i=0; i<schemaExport.getClass().getMethods().length; i++) {
			                    	Method methodt = schemaExport.getClass().getMethods()[i];
			                    	if (methodt.getName().equals("create")) method = methodt;
			                    }
			                    
			                    method.invoke(
									schemaExport,
									new Object[]{Boolean.FALSE, Boolean.FALSE});
			                    EditorUtil.openExternalFile(new File(fileName), Plugin.getDefault().getWorkbench().getActiveWorkbenchWindow());
                    		}
                    	}
                    }
                    catch (Throwable t) {
                    	if (t instanceof InvocationTargetException)
                    		UIUtil.pluginError(((InvocationTargetException)t).getCause(), shell);
                    	else
                    		UIUtil.pluginError(t, shell);
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
