package com.hudson.hibernatesynchronizer.editors.synchronizer;

import org.eclipse.core.resources.IFile;

import com.hudson.hibernatesynchronizer.util.EditorUtil;


/**
 * @author Joe Hudson
 */
public class ActionPerformerFileLink implements ActionPerformer {

	private IFile linkToDocument;
	
	public ActionPerformerFileLink (IFile linkToDocument) {
		this.linkToDocument = linkToDocument;
	}
	
	public void performAction() throws Exception {
		EditorUtil.openPage(linkToDocument);
	}

	public String getToolTipText () {
		return "Double click to open the " + linkToDocument.getName() + " mapping file";
	}
}