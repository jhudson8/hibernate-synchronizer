/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.editor;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.reconciler.AbstractReconciler;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;

/**
 * @author <a href="mailto: joe@binamics.com">Joe Hudson </a>
 */
public class HibernateReconciler extends AbstractReconciler {
    private HibernateEditor editor;

    private IDocument document;

    public HibernateReconciler(HibernateEditor editor) {
        this.editor = editor;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.reconciler.AbstractReconciler#process(org.eclipse.jface.text.reconciler.DirtyRegion)
     */
    protected void process(DirtyRegion dirtyRegion) {
        /*
         * IFile file = ((IFileEditorInput) editor.getEditorInput()).getFile();
         * int numMarkers = 0; try { numMarkers = EditorUtil.countMarkers(file); }
         * catch (CoreException ce) {} String text = dirtyRegion.getText(); if
         * (null != text) { if ((numMarkers > 0 && (text.indexOf(">") >= 0 ||
         * text.indexOf("\"") >= 0)) || (numMarkers == 0 && (text.indexOf(">") >=
         * 0))) { IJavaProject project = JavaCore.create(file.getProject());
         * HibernateDOMParser domParser = null;
         * EditorUtil.removeMarkers(editor.getHibernateSourceViewer()); try {
         * domParser = new HibernateDOMParser(new
         * ByteArrayInputStream(document.get().getBytes()));
         * DOMHelper.getHibernateDocument(domParser, project.getProject()); }
         * catch (SAXParseException spe) { EditorUtil.addProblemMarker(file,
         * spe.getMessage(), spe.getLineNumber()); } catch
         * (HibernateSynchronizerException hse) {
         * EditorUtil.addProblemMarker(file, hse.getMessage(),
         * hse.getLineNumber()); } catch (Exception e) {} } }
         */
    }

    protected void reconcilerDocumentChanged(IDocument document) {
        this.document = document;
    }

    public IReconcilingStrategy getReconcilingStrategy(String type) {
        return null;
    }
}