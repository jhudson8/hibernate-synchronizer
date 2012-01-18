package com.hudson.hibernatesynchronizer.editors.synchronizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.texteditor.ContentAssistAction;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.MarkerUtilities;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.xml.sax.SAXParseException;

import com.hudson.hibernatesynchronizer.Plugin;
import com.hudson.hibernatesynchronizer.editors.synchronizer.completion.Attribute;
import com.hudson.hibernatesynchronizer.editors.synchronizer.completion.CursorState;
import com.hudson.hibernatesynchronizer.editors.synchronizer.completion.Node;
import com.hudson.hibernatesynchronizer.editors.synchronizer.completion.NodeEvaluator;
import com.hudson.hibernatesynchronizer.editors.synchronizer.outline.OutlinePage;
import com.hudson.hibernatesynchronizer.exceptions.HibernateSynchronizerException;
import com.hudson.hibernatesynchronizer.mapping.HibernateClass;
import com.hudson.hibernatesynchronizer.mapping.HibernateClassId;
import com.hudson.hibernatesynchronizer.mapping.HibernateClassProperty;
import com.hudson.hibernatesynchronizer.mapping.HibernateComponentClass;
import com.hudson.hibernatesynchronizer.mapping.HibernateDocument;
import com.hudson.hibernatesynchronizer.mapping.HibernateMappingManager;
import com.hudson.hibernatesynchronizer.mapping.HibernateQuery;


public class Editor extends TextEditor implements MouseListener, MouseMoveListener, KeyListener, NodeEvaluator, ISelectionChangedListener {

	private ColorManager colorManager;
	private String packageName;
	private StyledText styledText;

	public Editor() {
		super();
		colorManager = new ColorManager();
		setSourceViewerConfiguration(new Configuration(colorManager, this));
		setDocumentProvider(new DocumentProvider());
	}
	public void dispose() {
		colorManager.dispose();
		super.dispose();
	}

	public ITextViewer getViewer () {
		return getSourceViewer();
	}

	protected void createActions() {
		super.createActions();
		// Add content assist propsal action
		ContentAssistAction action = new ContentAssistAction(
				Plugin.getDefault().getResourceBundle(),
				"XMLEditor.ContentAssist", this);
		action.setActionDefinitionId(
				ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
				setAction("XML.ContentAssist", action);
	}

	private OutlinePage outlinePage;
	public Object getAdapter(Class adapter) {
		if (IContentOutlinePage.class.equals(adapter)) {
			if (outlinePage == null) {
				outlinePage = new OutlinePage(this);
				outlinePage.addSelectionChangedListener(this);
			}
			return outlinePage;
		}
		return super.getAdapter(adapter);
	}

	private List linkRegions = new ArrayList();
	
	private void addLinkRange (int offsetStart, int length, ActionPerformer actionPerformer) {
		if (null != styledText) {
			Object[] objArr = new Object[2];
			Color fg = colorManager.getColor(ColorManager.COLOR_LINK);
			StyleRange r = new StyleRange(offsetStart, length, fg, null);
			styledText.setStyleRange(r);
			objArr[0] = r;
			objArr[1] = actionPerformer;
			linkRegions.add(objArr);
		}
	}

	Cursor handCursor;
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		handCursor = new Cursor(parent.getDisplay(), SWT.CURSOR_HAND);
		styledText = getSourceViewer().getTextWidget();
        styledText.addMouseMoveListener(this);
        styledText.addMouseListener(this);
        styledText.addKeyListener(this);
        refreshLinks();
	}

	public void refreshLinks () {
		if (null != styledText) {
			linkRegions.clear();
			CursorState.evaluate(getSourceViewer().getDocument(), this);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.AbstractTextEditor#editorSaved()
	 */
	protected void editorSaved() {
		super.editorSaved();
		try {
			deleteMarkers();
		}
		catch (CoreException e) {}
		try {
			HibernateMappingManager.getInstance(getFile().getProject()).notifyMappingSave(getFile());
			if (null != outlinePage) outlinePage.refresh();
		}
		catch (SAXParseException e) {
			addProblemMarker(e.getMessage(), e.getLineNumber());
		}
		catch (HibernateSynchronizerException e) {
			addProblemMarker(e.getMessage(), e.getLineNumber());
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.AbstractTextEditor#doSetInput(org.eclipse.ui.IEditorInput)
	 */
	protected void doSetInput(IEditorInput input) throws CoreException {
		super.doSetInput(input);
		refreshLinks();
		try {
			HibernateMappingManager.getInstance(getFile().getProject()).notifyMappingEdit(getFile());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Return the file associated with this editor
	 */
	public IFile getFile () {
		return ((IFileEditorInput) getEditorInput()).getFile();
	}

	/**
	 * Return the HibernateDocument associated with this editor
	 */
	public HibernateDocument getDocument () {
		return HibernateMappingManager.getInstance(getFile().getProject()).getHibernateDocument(getFile());
	}

	public void addWarningMarker (String message, int line) {
		addMarker(getFile(), message, line, IMarker.SEVERITY_WARNING);
	}

	public void addProblemMarker (String message, int line) {
		addMarker(getFile(), message, line, IMarker.SEVERITY_ERROR);
	}

	private void addMarker(IFile file, String message, int line, int markerType) {
		try {
			Map attributes = new HashMap(5);
			attributes.put(IMarker.SEVERITY, new Integer(markerType));
			attributes.put(IMarker.MESSAGE, message);
			attributes.put(IMarker.TEXT, message);
			attributes.put(IMarker.LINE_NUMBER, new Integer(line));
			MarkerUtilities.createMarker(file, attributes, IMarker.PROBLEM);
		} catch (Exception e) {}
	}
	
	public void deleteMarkers () throws CoreException {
		getFile().deleteMarkers(null, true, IResource.DEPTH_INFINITE);
	}

	public void mouseMove(MouseEvent e) {
        StyledText text = (StyledText) e.widget;
        try {
            int offset = text.getOffsetAtLocation(new Point(e.x, e.y));
            boolean hand = false;
            for (Iterator i=linkRegions.iterator(); i.hasNext(); ) {
            	Object[] objArr = (Object[]) i.next();
            	StyleRange sr = (StyleRange) objArr[0];
            	if (offset >= sr.start && offset <= sr.start + sr.length) {
            		// we have a link
            		hand = true;
            		ActionPerformer ap = (ActionPerformer) objArr[1];
            		// it's kind of annoying
            		// styledText.setToolTipText(ap.getToolTipText());
            	}
            }
            if (hand) text.setCursor(handCursor);
            else {
            	text.setCursor(null);
            	styledText.setToolTipText(null);
            }
        }
        catch (IllegalArgumentException e1) {
        	text.setCursor(null);
        }
	}

	public void mouseDoubleClick(MouseEvent e) {
        StyledText text = (StyledText) e.widget;
        int offset = text.getCaretOffset();
        for (Iterator i=linkRegions.iterator(); i.hasNext(); ) {
        	Object[] objArr = (Object[]) i.next();
        	StyleRange sr = (StyleRange) objArr[0];
        	if (offset >= sr.start && offset <= sr.start + sr.length) {
        		ActionPerformer ap = (ActionPerformer) objArr[1];
        		try {
        			ap.performAction();
        		}
        		catch (Exception exc) {
        			Plugin.trace(exc);
        		}
        	}
        }
	}
    public void mouseDown(MouseEvent e) {
    }
    public void mouseUp(MouseEvent e) {
    }

    private static final char CHAR_QUOTE = '\"';
    private static final char CHAR_NL = '\n';
	public void keyPressed(KeyEvent e) {
		if (e.character == CHAR_NL || e.character == CHAR_QUOTE) refreshLinks();
	}
	public void keyReleased(KeyEvent e) {
	}
	
	public boolean evaluate(Node node) {
		if (node.getName().equals("hibernate-mapping")) {
			Attribute attribute = node.getAttribute("package");
			if (null != attribute) this.packageName = attribute.getValue();
			else this.packageName = null;
		}
		else if (node.getName().equals("many-to-one") || node.getName().equals("key-many-to-one")) {
			Attribute attribute = node.getAttribute("class");
			if (null != attribute) {
				String fileClass = attribute.getValue();
				if (-1 == fileClass.indexOf('.') && null != packageName) {
					fileClass = packageName + "." + fileClass;
				}
				HibernateClass hc = HibernateMappingManager.getInstance(getFile().getProject()).getHibernateClass(fileClass);
				if (null != hc) {
					HibernateDocument doc = hc.getDocument();
					if (null != doc) { 
						addLinkRange(attribute.getValueOffset(), attribute.getValue().length(), new ActionPerformerFileLink(doc.getFile()));
					}
				}
			}
		}
		else if (node.getName().equals("class")) {
			Attribute attribute = node.getAttribute("name");
			if (null != attribute) {
				String className = attribute.getValue();
				if (-1 == className.indexOf('.') && null != packageName) {
					className = packageName + "." + className;
				}
				IJavaProject javaProject = JavaCore.create(getFile().getProject());
				addLinkRange(attribute.getValueOffset(), attribute.getValue().length(), new ActionPerformerClassLink(className, javaProject));
			}
		}
		else if (node.getName().equals("joined-subclass")) {
			Attribute attribute = node.getAttribute("name");
			if (null != attribute) {
				String className = attribute.getValue();
				if (-1 == className.indexOf('.') && null != packageName) {
					className = packageName + "." + className;
				}
				IJavaProject javaProject = JavaCore.create(getFile().getProject());
				addLinkRange(attribute.getValueOffset(), attribute.getValue().length(), new ActionPerformerClassLink(className, javaProject));
			}
			attribute = node.getAttribute("extends");
			if (null != attribute) {
				String className = attribute.getValue();
				if (-1 == className.indexOf('.') && null != packageName) {
					className = packageName + "." + className;
				}
				HibernateClass hc = HibernateMappingManager.getInstance(getFile().getProject()).getHibernateClass(className);
				if (null != hc) {
					HibernateDocument doc = hc.getDocument();
					if (null != doc) { 
						addLinkRange(attribute.getValueOffset(), attribute.getValue().length(), new ActionPerformerFileLink(doc.getFile()));
					}
				}
			}
		}
		else if (node.getName().equals("component")) {
			Attribute attribute = node.getAttribute("class");
			if (null != attribute) {
				String className = attribute.getValue();
				if (-1 == className.indexOf('.') && null != packageName) {
					className = packageName + "." + className;
				}
				IJavaProject javaProject = JavaCore.create(getFile().getProject());
				addLinkRange(attribute.getValueOffset(), attribute.getValue().length(), new ActionPerformerClassLink(className, javaProject));
			}
		}
		else if (node.getName().equals("composite-id")) {
			Attribute attribute = node.getAttribute("class");
			if (null != attribute) {
				String className = attribute.getValue();
				if (-1 == className.indexOf('.') && null != packageName) {
					className = packageName + "." + className;
				}
				IJavaProject javaProject = JavaCore.create(getFile().getProject());
				addLinkRange(attribute.getValueOffset(), attribute.getValue().length(), new ActionPerformerClassLink(className, javaProject));
			}
		}
		else if (node.getName().equals("set") || node.getName().equals("array") || node.getName().equals("map") || node.getName().equals("bag") || node.getName().equals("list")) {
			Attribute attribute = node.getAttribute("table");
			if (null != attribute) {
				String tableName = attribute.getValue();
				HibernateClass hc = HibernateMappingManager.getInstance(getFile().getProject()).getHibernateClassByTableName(tableName);
				if (null != hc) {
					HibernateDocument doc = hc.getDocument();
					if (null != doc) { 
						addLinkRange(attribute.getValueOffset(), attribute.getValue().length(), new ActionPerformerFileLink(doc.getFile()));
					}
				}
			}
		}
		return true;
	}

	public void selectAndReveal(Node node) {
			int offset = node.getNameStart();
			selectAndReveal(offset, 0, offset, 0);
	}
	
	/**
	 * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
	 */
	public void selectionChanged(SelectionChangedEvent event) {
		// outline selection listener
		if (event.getSource() instanceof OutlinePage) {
			outlinePage = (OutlinePage) event.getSource();
			if (event.getSelection() instanceof IStructuredSelection) {
				IStructuredSelection selection  = (IStructuredSelection) event.getSelection();
				NodeFinder nodeFinder = new NodeFinder(selection.getFirstElement());
				CursorState.evaluate(getViewer().getDocument(), nodeFinder);
				if (null != nodeFinder.foundNode) {
					selectAndReveal(nodeFinder.foundNode);
				}
			}
		}
	}
	
	public class NodeFinder implements NodeEvaluator {
		private HibernateClassId id;
		private HibernateClass hc;
		private HibernateClassProperty prop;
		private HibernateQuery query;
		private Node foundNode;

		public NodeFinder (Object object) {
			if (object instanceof HibernateClassProperty) {
				this.prop = (HibernateClassProperty) object;
			}
			else if (object instanceof HibernateClass) {
				this.hc = (HibernateClass) object;
			}
			else if (object instanceof HibernateClassId) {
				this.id = (HibernateClassId) object;
			}
			else if (object instanceof HibernateQuery) {
				this.query = (HibernateQuery) object;
			}
		}
		
		public boolean evaluate (Node node) {
			if (null != hc) {
				if (node.getName().equalsIgnoreCase("class") || node.getName().equalsIgnoreCase("subclass") || node.getName().equalsIgnoreCase("joined-subclass") || node.getName().equalsIgnoreCase("component")) {
					Attribute attribute = node.getAttribute("name");
					if (null != attribute) {
						if (hc instanceof HibernateComponentClass) {
							if (attribute.getValue().equals(((HibernateComponentClass) hc).getName())) {
								foundNode = node;
								return false;
							}
						}
						else if (attribute.getValue().equals(hc.getValueObjectClassName()) || attribute.getValue().equals(hc.getAbsoluteValueObjectClassName())) {
							foundNode = node;
							return false;
						}
					}
				}
			}
			else if (null != prop) {
				Attribute attribute = node.getAttribute("name");
				if (null != attribute) {
					if (attribute.getValue().equals(prop.getName())) {
						if (isOwnedBy(node, prop.getParent())) {
							foundNode = node;
							return false;
						}
					}
				}
			}
			else if (null != id) {
				if (node.getName().equals("id")) {
					Attribute attribute = node.getAttribute("name");
					if (null != attribute) {
						if (attribute.getValue().equals(id.getProperty().getName())) {
							if (isOwnedBy(node, id.getParent())) {
								foundNode = node;
								return false;
							}
						}
					}
				}
				else if (node.getName().equals("composite-id")) {
					if (isOwnedBy(node, id.getParent())) {
						foundNode = node;
						return false;
					}
				}
			}
			else if (null != query) {
				if (node.getName().equals("query")) {
					Attribute attribute = node.getAttribute("name");
					if (null != attribute) {
						if (attribute.getValue().equals(query.getName())) {
							foundNode = node;
							return false;
						}
					}
				}
			}
			return true;
		}
		
		private boolean isOwnedBy (Node node, HibernateClass parent) {
			Node parentNode = node.getParent();
			if (null == parentNode) return false;
			else {
				if (parentNode.getName().equals("class") || parentNode.getName().equals("subclass") || parentNode.getName().equals("joined-subclass")) {
					Attribute attribute = parentNode.getAttribute("name");
					if (null != attribute) return attribute.getValue().equals(parent.getValueObjectClassName()) || attribute.getValue().equals(parent.getAbsoluteValueObjectClassName());
				}
				else if (parentNode.getName().equals("component")) {
					Attribute attribute = parentNode.getAttribute("name");
					if (null != attribute) return attribute.getValue().equals(((HibernateComponentClass) parent).getName());
				}
				else if (parentNode.getName().equals("composite-id")) {
					return isOwnedBy (parentNode, parent);
				}
			}
			return false;
		}
	}
}