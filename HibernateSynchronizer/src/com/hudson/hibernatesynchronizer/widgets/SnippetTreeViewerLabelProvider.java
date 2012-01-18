package com.hudson.hibernatesynchronizer.widgets;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import com.hudson.hibernatesynchronizer.Plugin;
import com.hudson.hibernatesynchronizer.resource.Resource;
import com.hudson.hibernatesynchronizer.resource.Snippet;
import com.hudson.hibernatesynchronizer.util.HSUtil;

/**
 * @author Joe Hudson
 */
public class SnippetTreeViewerLabelProvider extends LabelProvider {


	/**
	 * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
	 */
	public String getText(Object element) {
		if (element instanceof Object[]) {
			return (String) (((Object[]) element)[0]);
		}
		else if (element instanceof Resource) {
			String name = ((Resource) element).getName();
			if (name.startsWith("BaseDAO")) return HSUtil.getPropDescription(name.substring(7, name.length()));
			else if (name.startsWith("BaseRootDAO")) return HSUtil.getPropDescription(name.substring(11, name.length()));
			else if (name.startsWith("c_")) return HSUtil.getPropDescription(name.substring(2, name.length()));
			else if (name.startsWith("ValueObjectPK")) return HSUtil.getPropDescription(name.substring(13, name.length()));
			else if (name.startsWith("ValueObject")) return HSUtil.getPropDescription(name.substring(11, name.length()));
			else if (name.startsWith("BaseValueObjectPK")) return HSUtil.getPropDescription(name.substring(17, name.length()));
			else if (name.startsWith("BaseValueObject")) return HSUtil.getPropDescription(name.substring(15, name.length()));
			else if (name.startsWith("RootDAO")) return HSUtil.getPropDescription(name.substring(7, name.length()));
			else if (name.startsWith("DAO")) return HSUtil.getPropDescription(name.substring(3, name.length()));
			else return name;
		}
		else return super.getText(element);
	}

	
	/**
	 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
	 */
	public Image getImage(Object element) {
		if (element instanceof Object[]) {
			return Plugin.getDefault().getImageRegistry().get("template");
		}
		else if (element instanceof Snippet) {
			return Plugin.getDefault().getImageRegistry().get("snippet");
		}
		else return null;
	}
}