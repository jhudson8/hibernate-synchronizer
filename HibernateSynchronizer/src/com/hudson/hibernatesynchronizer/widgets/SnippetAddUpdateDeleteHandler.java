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
import com.hudson.hibernatesynchronizer.resource.Snippet;
import com.hudson.hibernatesynchronizer.util.EditorUtil;
import com.hudson.hibernatesynchronizer.util.UIUtil;

/**
 * @author Joe Hudson
 */
public class SnippetAddUpdateDeleteHandler implements AddUpdateDeleteHandler {
	
	private IProject project;
	
	// assume workspace level
	public SnippetAddUpdateDeleteHandler () {
	}
	
	// project level
	public SnippetAddUpdateDeleteHandler (IProject project) {
		this.project = project;
	}
	
	public List addResource(Shell shell) throws Exception {
		ResourceNameSelectorDialog dialog = new ResourceNameSelectorDialog("SnippetName", shell);
		if (IDialogConstants.OK_ID == dialog.open()) {
			String name = dialog.getName();
			for (Iterator i=ResourceManager.getWorkspaceSnippets().iterator(); i.hasNext(); ) {
				Snippet s = (Snippet) i.next();
				if (s.getName().equals(name)) return ResourceManager.getWorkspaceSnippets();
			}
			Snippet snippet = new Snippet();
			snippet.setName(name);
			if (null != project) {
				snippet.setProject(project);
				ResourceManager.getInstance(project).save(snippet);
			}
			else {
				ResourceManager.saveWorkspaceResource(snippet);
			}
			EditorUtil.openPage(snippet, shell);
			MessageDialog.openInformation(shell, UIUtil.getResourceTitle("ResourceModification"), UIUtil.getResourceText("ResourceInEditor"));
		}
		return ResourceManager.getWorkspaceSnippets();
	}

	public List deleteResource(Resource resource, Shell shell) throws Exception {
		if (null == project) {
			ResourceManager.deleteWorkspaceResource(resource);
			return ResourceManager.getWorkspaceSnippets();
		}
		else {
			ResourceManager.getInstance(project).delete(resource);
			return ResourceManager.getInstance(project).getSnippets();
		}
	}

	public List updateResource(Resource resource, Shell shell) throws Exception {
		EditorUtil.openPage(resource, shell);
		MessageDialog.openInformation(shell, UIUtil.getResourceTitle("ResourceModification"), UIUtil.getResourceText("ResourceInEditor"));
		return ResourceManager.getWorkspaceSnippets();
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
			return ResourceManager.getWorkspaceSnippets();
		else
			return ResourceManager.getInstance(project).getSnippets();
	}
}