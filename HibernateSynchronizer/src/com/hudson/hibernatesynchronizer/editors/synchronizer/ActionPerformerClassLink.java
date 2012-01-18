package com.hudson.hibernatesynchronizer.editors.synchronizer;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.ui.JavaUI;

/**
 * @author Joe Hudson
 */
public class ActionPerformerClassLink implements ActionPerformer {

	private String fullClassName;
	private IJavaProject javaProject;
	
	public ActionPerformerClassLink (String className, IJavaProject javaProject) {
		this.fullClassName = className;
		this.javaProject = javaProject;
	}
	
	public void performAction() throws Exception {
		String packageName = "";
		String className = fullClassName + ".java";
		int index = fullClassName.lastIndexOf('.');
		if (index > 0) {
			packageName = fullClassName.substring(0, index);
			className = fullClassName.substring(index+1, fullClassName.length()) + ".java";
		}
		IPackageFragmentRoot[] roots = javaProject.getAllPackageFragmentRoots();
		for (int i=0; i<roots.length; i++) {
			try {
				if (!roots[i].isArchive()) {
					IPackageFragment frag = roots[i].getPackageFragment(packageName);
					for (int j=0; j<frag.getChildren().length; j++) {
						if (frag.getChildren()[j] instanceof ICompilationUnit) {
							if (((ICompilationUnit) frag.getChildren()[j]).getElementName().equals(className)) {
								ICompilationUnit unit = (ICompilationUnit) frag.getChildren()[j];
								JavaUI.openInEditor(unit);
							}
						}
					}
				}
			}
			catch (Exception e) {
			}
		}
	}

	public String getToolTipText () {
		return "Double click to open the " + fullClassName + " source";
	}
}