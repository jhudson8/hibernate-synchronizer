package com.hudson.hibernatesynchronizer.editors.synchronizer.outline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.hudson.hibernatesynchronizer.mapping.HibernateClass;
import com.hudson.hibernatesynchronizer.mapping.HibernateClassCollectionProperty;
import com.hudson.hibernatesynchronizer.mapping.HibernateClassId;
import com.hudson.hibernatesynchronizer.mapping.HibernateClassProperty;
import com.hudson.hibernatesynchronizer.mapping.HibernateDocument;
import com.hudson.hibernatesynchronizer.mapping.HibernateQuery;

public class ContentProvider implements ITreeContentProvider {
	private OutlinePage outlinePage;
	
	boolean m_showOneToOne = false;
	boolean m_showManyToOne = false;
	boolean m_showProperties = false;
	boolean m_showCollections = false;
	boolean m_showComponents = false;
	boolean m_showQueries = false;
	boolean showOnlyRequired = false;
	
	boolean showOneToOne = true;
	boolean showManyToOne = true;
	boolean showProperties = true;
	boolean showCollections = true;
	boolean showComponents = true;
	boolean showQueries = true;
	
	public ContentProvider (OutlinePage outlinePage) {
		this.outlinePage = outlinePage;
	}
	
	public void refresh () {
		showOneToOne = m_showOneToOne;
		showManyToOne = m_showManyToOne;
		showProperties = m_showProperties;
		showCollections = m_showCollections;
		showComponents = m_showComponents;
		showQueries = m_showQueries;
		if (!(showOneToOne || showManyToOne || showProperties || showCollections || showComponents || showQueries)) {
			showOneToOne = true;
			showManyToOne = true;
			showProperties = true;
			showCollections = true;
			showComponents = true;
			showQueries = true;
		}
	}

	public Object[] getChildren(Object parentElement) {
		
		if (parentElement instanceof HibernateDocument) {
			HibernateDocument doc = (HibernateDocument) parentElement;
			List list = new ArrayList();
			list.addAll(doc.getClasses());
			if (showQueries) list.addAll(doc.getQueries());
			return list.toArray();
		}
		else if (parentElement instanceof HibernateClass) {
			HibernateClass hc = (HibernateClass) parentElement;
			Object[] arr = null;
			int count = 0;
			if (hc.getId(false) != null) count++;
			count += hc.getSubclassList().size();
			if (showOneToOne) {
				if (!showOnlyRequired) count += hc.getOneToOneList().size();
				else {
					for (Iterator i=hc.getOneToOneList().iterator(); i.hasNext(); ) {
						if (((HibernateClassProperty) i.next()).isRequired()) count ++;
					}
				}
			}
			if (showManyToOne) {
				if (!showOnlyRequired) count += hc.getManyToOneList().size();
				else {
					for (Iterator i=hc.getManyToOneList().iterator(); i.hasNext(); ) {
						if (((HibernateClassProperty) i.next()).isRequired()) count ++;
					}
				}
			}
			if (showProperties) {
				if (!showOnlyRequired) count += hc.getProperties().size();
				else {
					for (Iterator i=hc.getProperties().iterator(); i.hasNext(); ) {
						if (((HibernateClassProperty) i.next()).isRequired()) count ++;
					}
				}
			}
			if (showCollections) count += hc.getCollectionList().size();
			if (showComponents) count += hc.getComponentList().size();
			arr = new Object[count];
			int index = 0;
			Collections.sort(hc.getSubclassList());
			for (Iterator i=hc.getSubclassList().iterator(); i.hasNext(); ) {
				arr[index++] = i.next();
			}
			if (null != hc.getId(false)) {
				arr[index++] = hc.getId(false);
			}
			if (showOneToOne) {
				Collections.sort(hc.getOneToOneList());
				for (Iterator i=hc.getOneToOneList().iterator(); i.hasNext(); ) {
					HibernateClassProperty prop = (HibernateClassProperty) i.next();
					if (!showOnlyRequired || prop.isRequired()) arr[index++] = prop;
				}
			}
			if (showManyToOne) {
				Collections.sort(hc.getManyToOneList());
				for (Iterator i=hc.getManyToOneList().iterator(); i.hasNext(); ) {
					HibernateClassProperty prop = (HibernateClassProperty) i.next();
					if (!showOnlyRequired || prop.isRequired()) arr[index++] = prop;
				}
			}
			if (showProperties) {
				Collections.sort(hc.getProperties());
				for (Iterator i=hc.getProperties().iterator(); i.hasNext(); ) {
					HibernateClassProperty prop = (HibernateClassProperty) i.next();
					if (!showOnlyRequired || prop.isRequired()) arr[index++] = prop;
				}
			}
			if (showComponents) {
				Collections.sort(hc.getComponentList());
				for (Iterator i=hc.getComponentList().iterator(); i.hasNext(); ) {
					arr[index++] = i.next();
				}
			}
			if (showCollections) {
				Collections.sort(hc.getCollectionList());
				for (Iterator i=hc.getCollectionList().iterator(); i.hasNext(); ) {
					arr[index++] = i.next();
				}
			}
			return arr;
		}
		else if (parentElement instanceof HibernateClassId) {
			HibernateClassId id = (HibernateClassId) parentElement;
			if (null != id.getProperties() && id.getProperties().size() > 1) {
				Object[] arr = new Object[id.getProperties().size()];
				Collections.sort(id.getProperties());
				int index = 0;
				for (Iterator i=id.getProperties().iterator(); i.hasNext(); index++) {
					arr[index] = i.next();
				}
				return arr;
			}
		}
		return null;
	}

	public boolean hasChildren(Object element) {
		if (element instanceof HibernateDocument) {
			return ((HibernateDocument) element).getClasses().size() > 0;
		}
		if (element instanceof HibernateClass) {
			HibernateClass hc = (HibernateClass) element;
			int count = 0;
			if (hc.getId(false) != null) count++;
			count += hc.getSubclassList().size();
			if (showOneToOne) {
				if (!showOnlyRequired) count += hc.getOneToOneList().size();
				else {
					for (Iterator i=hc.getOneToOneList().iterator(); i.hasNext(); ) {
						if (((HibernateClassProperty) i.next()).isRequired()) count ++;
					}
				}
			}
			if (showManyToOne) {
				if (!showOnlyRequired) count += hc.getManyToOneList().size();
				else {
					for (Iterator i=hc.getManyToOneList().iterator(); i.hasNext(); ) {
						if (((HibernateClassProperty) i.next()).isRequired()) count ++;
					}
				}
			}
			if (showProperties) {
				if (!showOnlyRequired) count += hc.getProperties().size();
				else {
					for (Iterator i=hc.getProperties().iterator(); i.hasNext(); ) {
						if (((HibernateClassProperty) i.next()).isRequired()) count ++;
					}
				}
			}
			if (showCollections) count += hc.getCollectionList().size();
			if (showComponents) count += hc.getComponentList().size();
			return (0 < count);
		}
		else if (element instanceof HibernateClassId && null != ((HibernateClassId) element).getProperties()) {
			return (((HibernateClassId) element).getProperties().size() > 1);
		}
		else return false;
	}
	
	public Object getParent(Object element) {
		if (element instanceof HibernateClass) {
			return ((HibernateClass) element).getParent();
		}
		else if (element instanceof HibernateClassProperty) {
			return ((HibernateClassProperty) element).getParent();
		}
		else if (element instanceof HibernateClassCollectionProperty) {
			return ((HibernateClassCollectionProperty) element).getParent();
		}
		else if (element instanceof HibernateClassId) {
			return ((HibernateClassId) element).getParent();
		}
		else if (element instanceof HibernateQuery) {
			return ((HibernateQuery) element).getParent();
		}
		return null;
	}

	public Object[] getElements(Object inputElement) {
		if (null != outlinePage.getEditor().getDocument()) {
			int index = 0;
			int size = 0;
			size += outlinePage.getEditor().getDocument().getClasses().size();
			if (showQueries) size += outlinePage.getEditor().getDocument().getQueries().size();
			Object[] arr = new Object[size];
			Collections.sort(outlinePage.getEditor().getDocument().getClasses());
			for (int i=0; i<outlinePage.getEditor().getDocument().getClasses().size(); i++) {
				arr[index++] = outlinePage.getEditor().getDocument().getClasses().get(i); 
			}
			if (showQueries) {
				Collections.sort(outlinePage.getEditor().getDocument().getQueries());
				for (int i=0; i<outlinePage.getEditor().getDocument().getQueries().size(); i++) {
					arr[index++] = outlinePage.getEditor().getDocument().getQueries().get(i); 
				}
			}
			return arr;
		}
		else
			return null;
	}

	public void dispose() {
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
//		this.document = outlinePage.getEditor().getDocument();
//		HibernateDocument document = Synchronizer.getClasses((IFileEditorInput) outlinePage.getEditor().getEditorInput());
//		if (null != document) this.document = document;
	}
}