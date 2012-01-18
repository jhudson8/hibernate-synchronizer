/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.editor;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.ui.part.FileEditorInput;

/**
 * @author <a href="mailto: joe@binamics.com">Joe Hudson </a>
 */
public class HibernateContentAssistProcessor implements IContentAssistProcessor {

    private HibernateEditor hEditor;

    public HibernateContentAssistProcessor(HibernateEditor editor) {
        hEditor = editor;
    }

    public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer,
            int documentOffset) {

        try {
            InputStream is = new ByteArrayInputStream(viewer.getDocument()
                    .get().getBytes());
            CursorState state = CursorState.getInstance(
                    ((FileEditorInput) hEditor.getEditorInput()).getFile()
                            .getProject(), is, documentOffset);
            List suggestions = state.getSuggestions(documentOffset, hEditor);
            if (null == suggestions)
                return null;
            else {
                int index = 0;
                ICompletionProposal[] props = new ICompletionProposal[suggestions
                        .size()];
                for (Iterator i = suggestions.iterator(); i.hasNext(); index++) {
                    ICompletionProposal prop = (ICompletionProposal) i.next();
                    props[index] = prop;
                }
                return props;
            }

        } catch (Exception e) {
        }
        return null;
    }

    public IContextInformation[] computeContextInformation(ITextViewer viewer,
            int documentOffset) {
        return null;
    }

    public char[] getCompletionProposalAutoActivationCharacters() {
        char[] arr = new char[2];
        arr[0] = '"';
        arr[1] = '/';
        return arr;
    }

    public char[] getContextInformationAutoActivationCharacters() {
        return null;
    }

    public IContextInformationValidator getContextInformationValidator() {
        return null;
    }

    public String getErrorMessage() {
        return null;
    }

    private ICompletionProposal[] getVariableProposals(String aPrefix,
            int anOffset) {
        return null;
    }
}