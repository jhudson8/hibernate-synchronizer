/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.editor.actions;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.velocity.context.Context;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.hudson.hibernatesynchronizer.Constants;
import com.hudson.hibernatesynchronizer.custom.Snippet;
import com.hudson.hibernatesynchronizer.dialog.SnippetChoiceDialog;
import com.hudson.hibernatesynchronizer.editor.HibernateEditor;
import com.hudson.hibernatesynchronizer.obj.HibernateClass;
import com.hudson.hibernatesynchronizer.obj.HibernateDocument;
import com.hudson.hibernatesynchronizer.obj.IHibernateClassProperty;
import com.hudson.hibernatesynchronizer.parser.HibernateDOMParser;
import com.hudson.hibernatesynchronizer.util.HSUtil;
import com.hudson.hibernatesynchronizer.util.SynchronizerThread;

/**
 * @author <a href="mailto: joe@binamics.com">Joe Hudson </a>
 */
public class SnippetToClipboard implements IEditorActionDelegate {

    private HibernateEditor editor;

    public void setActiveEditor(IAction arg0, IEditorPart arg1) {
        if (arg1 instanceof HibernateEditor) {
            arg0.setEnabled(true);
            this.editor = (HibernateEditor) arg1;
        } else {
            arg0.setEnabled(false);
        }
    }

    public void run(IAction action) {
        try {
            HibernateDocument doc = editor.getDocument();
            if (null != doc) {
                if (doc.getClasses().size() > 1) {
                    MessageDialog
                            .openError(
                                    new Shell(),
                                    "Error",
                                    "You may only have a single class referenced in the mapping file to use this feature.");
                    return;
                } else if (doc.getClasses().size() == 0) {
                    MessageDialog
                            .openError(new Shell(), "Error",
                                    "You must have a class referenced in the mapping file to use this feature.");
                    return;
                }
            } else {
                MessageDialog.openError(new Shell(), "Error",
                        "The hibernate class mapping could not be determined.");
                return;
            }
            IFileEditorInput fie = (IFileEditorInput) editor.getEditorInput();
            ISelection selection = editor.getEditorSite()
                    .getSelectionProvider().getSelection();
            Context context = SynchronizerThread.getDefaultContext(
                    (HibernateClass) doc.getClasses().get(0), editor
                            .getProject());
            context.put("classes", doc.getClasses());
            if (selection instanceof ITextSelection) {
                ITextSelection textSelection = (ITextSelection) selection;
                int start = textSelection.getOffset();
                int end = textSelection.getLength();
                if (end > 0) {
                    IDocumentProvider provider = editor.getDocumentProvider();
                    IEditorInput input = editor.getEditorInput();
                    String s = provider.getDocument(input).get(start, end);

                    try {
                        HibernateDOMParser domParser = new HibernateDOMParser(
                                new ByteArrayInputStream(s.getBytes()));
                        Document selDoc = domParser.getDocument();
                        loadContext(context, selDoc, doc);
                    } catch (Exception e) {
                        try {
                            s = "<class>" + s + "</class>";
                            HibernateDOMParser domParser = new HibernateDOMParser(
                                    new ByteArrayInputStream(s.getBytes()));
                            Document selDoc = domParser.getDocument();
                            loadContext(context, selDoc, doc);
                        } catch (Exception e1) {
                            MessageDialog
                                    .openError(new Shell(), "Error",
                                            "You must select text that resolves to a well formed XML structure");
                            return;
                        }
                    }
                }
            }
            SnippetChoiceDialog scd = new SnippetChoiceDialog(new Shell(),
                    editor.getProject());
            if (Dialog.OK == scd.open()) {
                Snippet s = scd.getSnippet();
                if (null != s) {
                    StringWriter sw = new StringWriter();
                    Constants.customGenerator.evaluate(context, sw,
                            SnippetToClipboard.class.getName(), s.getContent());

                    Display display = Display.getCurrent();
                    Clipboard clipboard = new Clipboard(display);
                    TextTransfer transfer = TextTransfer.getInstance();
                    clipboard.setContents(new Object[] { sw.toString() },
                            new TextTransfer[] { transfer });
                    clipboard.dispose();
                }
            }
        } catch (Exception e) {
            HSUtil.showError(e.getMessage(), new Shell());
        }
    }

    private void loadContext(Context context, Document selection,
            HibernateDocument doc) {
        Map properties = new HashMap();
        for (Iterator i = ((HibernateClass) doc.getClasses().get(0))
                .getAllProperties().iterator(); i.hasNext();) {
            IHibernateClassProperty prop = (IHibernateClassProperty) i.next();
            properties.put(prop.getName(), prop);
        }
        for (Iterator i = ((HibernateClass) doc.getClasses().get(0))
                .getSubclassList().iterator(); i.hasNext();) {
            HibernateClass c = (HibernateClass) i.next();
            for (Iterator i2 = c.getProperties().iterator(); i2.hasNext();) {
                IHibernateClassProperty prop = (IHibernateClassProperty) i2
                        .next();
                properties.put(prop.getName(), prop);
            }
        }
        List contextProps = new ArrayList();
        getContextProperties(selection.getDocumentElement(), properties,
                contextProps);
        if (contextProps.size() > 0) {
            context.put("prop", contextProps.get(0));
            context
                    .put("class", ((IHibernateClassProperty) contextProps
                            .get(0)).getParent());
            context.put("props", contextProps);
        }
    }

    private void getContextProperties(Node node, Map properties,
            List outProperties) {
        if (node.getNodeName().equals("class")
                || node.getNodeName().equals("subclass")
                || node.getNodeName().equals("hibernate-mapping")) {
            NodeList nl = node.getChildNodes();
            for (int i = 0; i < nl.getLength(); i++) {
                node = nl.item(i);
                getContextProperties(node, properties, outProperties);
            }
        } else {
            NamedNodeMap nnm = node.getAttributes();
            if (null != nnm) {
                Node n = nnm.getNamedItem("name");
                if (null != n && null != properties.get(n.getNodeValue())) {
                    outProperties.add(properties.get(n.getNodeValue()));
                }
            }
        }
    }

    public void selectionChanged(IAction arg0, ISelection arg1) {
        if (null != editor)
            arg0.setEnabled(true);
    }
}