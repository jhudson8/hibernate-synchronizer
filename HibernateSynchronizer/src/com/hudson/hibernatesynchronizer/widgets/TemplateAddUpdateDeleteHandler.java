package com.hudson.hibernatesynchronizer.widgets;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

import com.hudson.hibernatesynchronizer.dialogs.ResourceNameSelectorDialog;
import com.hudson.hibernatesynchronizer.resource.Resource;
import com.hudson.hibernatesynchronizer.resource.ResourceManager;
import com.hudson.hibernatesynchronizer.resource.Template;
import com.hudson.hibernatesynchronizer.util.EditorUtil;
import com.hudson.hibernatesynchronizer.util.UIUtil;

/**
 * @author Joe Hudson
 */
public class TemplateAddUpdateDeleteHandler implements AddUpdateDeleteHandler {
	
	private IProject project;
	
	// assume workspace level
	public TemplateAddUpdateDeleteHandler () {
	}
	
	// project level
	public TemplateAddUpdateDeleteHandler (IProject project) {
		this.project = project;
	}
	
	public List addResource(Shell shell) throws Exception {
		ResourceNameSelectorDialog dialog = new ResourceNameSelectorDialog("TemplateName", shell);
		if (IDialogConstants.OK_ID == dialog.open()) {
			String name = dialog.getName();
			for (Iterator i=ResourceManager.getWorkspaceTemplates().iterator(); i.hasNext(); ) {
				Template s = (Template) i.next();
				if (s.getName().equals(name)) return ResourceManager.getWorkspaceTemplates();
			}
			Template template = new Template();
			template.setName(name);
			if (null != project) {
				template.setProject(project);
				ResourceManager.getInstance(project).save(template);
			}
			else {
				ResourceManager.saveWorkspaceResource(template);
			}
			EditorUtil.openPage(template, shell);
			MessageDialog.openInformation(shell, UIUtil.getResourceTitle("ResourceModification"), UIUtil.getResourceText("ResourceInEditor"));
		}
		return ResourceManager.getNonRequiredWorkspaceTemplates();
	}

	public List deleteResource(Resource resource, Shell shell) throws Exception {
		if (null == project) {
			ResourceManager.deleteWorkspaceResource(resource);
			return ResourceManager.getNonRequiredWorkspaceTemplates();
		}
		else {
			ResourceManager.getInstance(project).delete(resource);
			return ResourceManager.getInstance(project).getTemplates();
		}
	}
	public List updateResource(Resource resource, Shell shell) throws Exception {
		EditorUtil.openPage(resource, shell);
		MessageDialog.openInformation(shell, UIUtil.getResourceTitle("ResourceModification"), UIUtil.getResourceText("ResourceInEditor"));
		return ResourceManager.getWorkspaceTemplates();
	}

	/**
	 * @see com.hudson.hibernatesynchronizer.widgets.AddUpdateDeleteHandler#canDelete(com.hudson.hibernatesynchronizer.resource.Resource)
	 */
	public boolean canDelete(Resource resource) throws Exception {
		return (!(ResourceManager.isRequiredResource(resource)));
	}

	/**
	 * @see com.hudson.hibernatesynchronizer.widgets.AddUpdateDeleteHandler#canRestore(com.hudson.hibernatesynchronizer.resource.Resource)
	 */
	public boolean canRestore(Resource resource) throws Exception {
		return (null == project && resource.isModified() && ResourceManager.isRequiredResource(resource));
	}

	/**
	 * @see com.hudson.hibernatesynchronizer.widgets.AddUpdateDeleteHandler#restore(com.hudson.hibernatesynchronizer.resource.Resource, org.eclipse.swt.widgets.Shell)
	 */
	public List restore(Resource resource, Shell shell) throws Exception {
		ResourceManager.restore(resource);
		if (null == project)
			return ResourceManager.getNonRequiredWorkspaceTemplates();
		else
			return ResourceManager.getInstance(project).getTemplates();
	}
}