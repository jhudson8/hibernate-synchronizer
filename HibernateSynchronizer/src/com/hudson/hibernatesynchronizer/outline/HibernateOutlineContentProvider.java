/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.outline;

import java.util.Collections;
import java.util.Iterator;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IFileEditorInput;

import com.hudson.hibernatesynchronizer.obj.HibernateClass;
import com.hudson.hibernatesynchronizer.obj.HibernateClassCollectionProperty;
import com.hudson.hibernatesynchronizer.obj.HibernateClassId;
import com.hudson.hibernatesynchronizer.obj.HibernateClassProperty;
import com.hudson.hibernatesynchronizer.obj.HibernateDocument;
import com.hudson.hibernatesynchronizer.obj.HibernateQuery;
import com.hudson.hibernatesynchronizer.util.Synchronizer;

/**
 * @author <a href="mailto: joe@binamics.com">Joe Hudson </a>
 */
public class HibernateOutlineContentProvider implements ITreeContentProvider {
    private HibernateOutlinePage outlinePage;

    private HibernateDocument document;

    boolean showOneToOne = true;

    boolean showManyToOne = true;

    boolean showProperties = true;

    boolean showCollections = true;

    boolean showComponents = true;

    boolean showQueries = true;

    boolean showOnlyRequired = false;

    public HibernateOutlineContentProvider(HibernateOutlinePage outlinePage) {
        this.outlinePage = outlinePage;
    }

    public Object[] getChildren(Object parentElement) {
        if (parentElement instanceof HibernateClass) {
            HibernateClass hc = (HibernateClass) parentElement;
            Object[] arr = null;
            int count = 0;
            if (hc.getId() != null)
                count++;
            count += hc.getSubclassList().size();
            if (showOneToOne) {
                if (!showOnlyRequired)
                    count += hc.getOneToOneList().size();
                else {
                    for (Iterator i = hc.getOneToOneList().iterator(); i
                            .hasNext();) {
                        if (((HibernateClassProperty) i.next()).isRequired())
                            count++;
                    }
                }
            }
            if (showManyToOne) {
                if (!showOnlyRequired)
                    count += hc.getManyToOneList().size();
                else {
                    for (Iterator i = hc.getManyToOneList().iterator(); i
                            .hasNext();) {
                        if (((HibernateClassProperty) i.next()).isRequired())
                            count++;
                    }
                }
            }
            if (showProperties) {
                if (!showOnlyRequired)
                    count += hc.getProperties().size();
                else {
                    for (Iterator i = hc.getProperties().iterator(); i
                            .hasNext();) {
                        if (((HibernateClassProperty) i.next()).isRequired())
                            count++;
                    }
                }
            }
            if (showCollections)
                count += hc.getCollectionList().size();
            if (showComponents)
                count += hc.getComponentList().size();
            arr = new Object[count];
            int index = 0;
            Collections.sort(hc.getSubclassList());
            for (Iterator i = hc.getSubclassList().iterator(); i.hasNext();) {
                arr[index++] = i.next();
            }
            if (null != hc.getId()) {
                arr[index++] = hc.getId();
            }
            if (showOneToOne) {
                Collections.sort(hc.getOneToOneList());
                for (Iterator i = hc.getOneToOneList().iterator(); i.hasNext();) {
                    HibernateClassProperty prop = (HibernateClassProperty) i
                            .next();
                    if (!showOnlyRequired || prop.isRequired())
                        arr[index++] = prop;
                }
            }
            if (showManyToOne) {
                Collections.sort(hc.getManyToOneList());
                for (Iterator i = hc.getManyToOneList().iterator(); i.hasNext();) {
                    HibernateClassProperty prop = (HibernateClassProperty) i
                            .next();
                    if (!showOnlyRequired || prop.isRequired())
                        arr[index++] = prop;
                }
            }
            if (showProperties) {
                Collections.sort(hc.getProperties());
                for (Iterator i = hc.getProperties().iterator(); i.hasNext();) {
                    HibernateClassProperty prop = (HibernateClassProperty) i
                            .next();
                    if (!showOnlyRequired || prop.isRequired())
                        arr[index++] = prop;
                }
            }
            if (showComponents) {
                Collections.sort(hc.getComponentList());
                for (Iterator i = hc.getComponentList().iterator(); i.hasNext();) {
                    arr[index++] = i.next();
                }
            }
            if (showCollections) {
                Collections.sort(hc.getCollectionList());
                for (Iterator i = hc.getCollectionList().iterator(); i
                        .hasNext();) {
                    arr[index++] = i.next();
                }
            }
            return arr;
        } else if (parentElement instanceof HibernateClassId) {
            HibernateClassId id = (HibernateClassId) parentElement;
            if (null != id.getProperties() && id.getProperties().size() > 1) {
                Object[] arr = new Object[id.getProperties().size()];
                Collections.sort(id.getProperties());
                int index = 0;
                for (Iterator i = id.getProperties().iterator(); i.hasNext(); index++) {
                    arr[index] = i.next();
                }
                return arr;
            }
        }
        return null;
    }

    public boolean hasChildren(Object element) {
        if (element instanceof HibernateClass) {
            HibernateClass hc = (HibernateClass) element;
            int count = 0;
            if (hc.getId() != null)
                count++;
            count += hc.getSubclassList().size();
            if (showOneToOne) {
                if (!showOnlyRequired)
                    count += hc.getOneToOneList().size();
                else {
                    for (Iterator i = hc.getOneToOneList().iterator(); i
                            .hasNext();) {
                        if (((HibernateClassProperty) i.next()).isRequired())
                            count++;
                    }
                }
            }
            if (showManyToOne) {
                if (!showOnlyRequired)
                    count += hc.getManyToOneList().size();
                else {
                    for (Iterator i = hc.getManyToOneList().iterator(); i
                            .hasNext();) {
                        if (((HibernateClassProperty) i.next()).isRequired())
                            count++;
                    }
                }
            }
            if (showProperties) {
                if (!showOnlyRequired)
                    count += hc.getProperties().size();
                else {
                    for (Iterator i = hc.getProperties().iterator(); i
                            .hasNext();) {
                        if (((HibernateClassProperty) i.next()).isRequired())
                            count++;
                    }
                }
            }
            if (showCollections)
                count += hc.getCollectionList().size();
            if (showComponents)
                count += hc.getComponentList().size();
            return (0 < count);
        } else if (element instanceof HibernateClassId
                && null != ((HibernateClassId) element).getProperties()) {
            return (((HibernateClassId) element).getProperties().size() > 1);
        } else
            return false;
    }

    public Object getParent(Object element) {
        if (element instanceof HibernateClass) {
            return ((HibernateClass) element).getParent();
        } else if (element instanceof HibernateClassProperty) {
            return ((HibernateClassProperty) element).getParent();
        } else if (element instanceof HibernateClassCollectionProperty) {
            return ((HibernateClassCollectionProperty) element).getParent();
        } else if (element instanceof HibernateClassId) {
            return ((HibernateClassId) element).getParent();
        } else if (element instanceof HibernateQuery) {
            return ((HibernateQuery) element).getParent();
        }
        return null;
    }

    public Object[] getElements(Object inputElement) {
        if (null != document) {
            int index = 0;
            Object[] arr = new Object[document.getClasses().size()
                    + document.getQueries().size()];
            Collections.sort(document.getClasses());
            for (int i = 0; i < document.getClasses().size(); i++) {
                arr[index++] = document.getClasses().get(i);
            }
            Collections.sort(document.getQueries());
            for (int i = 0; i < document.getQueries().size(); i++) {
                arr[index++] = document.getQueries().get(i);
            }
            return arr;
        } else
            return null;
    }

    public void dispose() {
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        HibernateDocument document = Synchronizer
                .getClasses((IFileEditorInput) outlinePage.getEditor()
                        .getEditorInput());
        if (null != document)
            this.document = document;
    }

    public HibernateDocument getDocument() {
        return document;
    }

    public void setDocument(HibernateDocument document) {
        this.document = document;
    }
}