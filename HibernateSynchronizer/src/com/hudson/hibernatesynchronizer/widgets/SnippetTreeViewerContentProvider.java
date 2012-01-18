package com.hudson.hibernatesynchronizer.widgets;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.hudson.hibernatesynchronizer.resource.Snippet;

/**
 * @author Joe Hudson
 */
public class SnippetTreeViewerContentProvider implements ITreeContentProvider {
	private Object[] topLevelElements;

	/**
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof Object[]) {
			Object[] objArr = (Object[]) parentElement;
			List snippets = (List) objArr[1];
			Object[] rtnArr = new Object[snippets.size()];
			int index = 0;
			for (Iterator i=snippets.iterator(); i.hasNext(); ) {
				rtnArr[index++] = i.next();
			}
			return rtnArr;
		}
		else return null;
	}

	/**
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
	 */
	public Object getParent(Object element) {
		return null;
	}

	/**
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 */
	public boolean hasChildren(Object element) {
		return (element instanceof Object[]);
	}

	/**
	 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
	 */
	public Object[] getElements(Object inputElement) {
		List snippets = (List) inputElement;
		List baseDAO = new ArrayList();
		List baseRootDAO = new ArrayList();
		List baseValueObject = new ArrayList();
		List common = new ArrayList();
		List valueObject = new ArrayList();
		List valueObjectPK = new ArrayList();
		List rootDAO = new ArrayList();
		List dao = new ArrayList();
		List baseValueObjectPK = new ArrayList();
		List other = new ArrayList();

		for (Iterator i=snippets.iterator(); i.hasNext(); ) {
			Snippet s = (Snippet) i.next();
			if (s.getName().startsWith("BaseDAO")) baseDAO.add(s);
			else if (s.getName().startsWith("BaseRootDAO")) baseRootDAO.add(s);
			else if (s.getName().startsWith("c_")) common.add(s);
			else if (s.getName().startsWith("ValueObjectPK")) valueObjectPK.add(s);
			else if (s.getName().startsWith("ValueObject")) valueObject.add(s);
			else if (s.getName().startsWith("BaseValueObjectPK")) baseValueObjectPK.add(s);
			else if (s.getName().startsWith("BaseValueObject")) baseValueObject.add(s);
			else if (s.getName().startsWith("RootDAO")) rootDAO.add(s);
			else if (s.getName().startsWith("DAO")) dao.add(s);
			else other.add(s);
		}

		int size = 9 + other.size();
		topLevelElements = new Object[size];
		int index = 0;
		topLevelElements[index++] = new Object[] {"DAO", dao};
		topLevelElements[index++] = new Object[] {"Base DAO", baseDAO};
		topLevelElements[index++] = new Object[] {"Root DAO", rootDAO};
		topLevelElements[index++] = new Object[] {"Base Root DAO", baseRootDAO};
		topLevelElements[index++] = new Object[] {"Value Object", valueObject};
		topLevelElements[index++] = new Object[] {"Value Object PK", valueObjectPK};
		topLevelElements[index++] = new Object[] {"Base Value Object", baseValueObject};
		topLevelElements[index++] = new Object[] {"Base Value Object PK", baseValueObjectPK};
		topLevelElements[index++] = new Object[] {"Common", common};
		for (Iterator i=other.iterator(); i.hasNext(); ) {
			topLevelElements[index++] = i.next();
		}
		
		return topLevelElements;
	}

	/**
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	public void dispose() {
	}

	/**
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

}
