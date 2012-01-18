package com.hudson.hibernatesynchronizer.editors.velocity;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.DefaultPartitioner;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.editors.text.FileDocumentProvider;

import com.hudson.hibernatesynchronizer.Plugin;
import com.hudson.hibernatesynchronizer.resource.ResourceEditorInput;
import com.hudson.hibernatesynchronizer.resource.ResourceManager;

public class DocumentProvider extends FileDocumentProvider {

	private Editor editor;
	
	public DocumentProvider (Editor editor) {
		this.editor = editor;
	}
	
	protected IDocument createDocument(Object element) throws CoreException {
		IDocument document = super.createDocument(element);
		try {
			if (document != null) {
				IDocumentPartitioner partitioner =
					new DefaultPartitioner(
						new PartitionScanner(),
						new String[] {
							PartitionScanner.FOREACH_PARTITION,
							PartitionScanner.END_PARTITION,
							PartitionScanner.IF_PARTITION,
							PartitionScanner.MACRO_PARTITION,
							PartitionScanner.SET_PARTITION,
							PartitionScanner.FOREACH_END_PARTITION,
							PartitionScanner.IF_END_PARTITION,
							PartitionScanner.VARIABLE_PARTITION,
							PartitionScanner.COMMENT_PARTITION,
						});
				partitioner.connect(document);
				document.setDocumentPartitioner(partitioner);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return document;
	}

	protected boolean setDocumentContent(IDocument document, IEditorInput editorInput, String encoding) throws CoreException {
		if (editorInput instanceof ResourceEditorInput) {
			setDocumentContent(document, new ByteArrayInputStream(((ResourceEditorInput) editorInput).getResource().getContent().getBytes()), encoding);
			return true;
		}
		return super.setDocumentContent(document, editorInput, encoding);
	}

	/**
	 * @see org.eclipse.ui.texteditor.AbstractDocumentProvider#doSaveDocument(org.eclipse.core.runtime.IProgressMonitor, java.lang.Object, org.eclipse.jface.text.IDocument, boolean)
	 */
	protected void doSaveDocument(IProgressMonitor monitor, Object element,
			IDocument document, boolean overwrite) throws CoreException {
		if (element instanceof ResourceEditorInput) {
			Map props = editor.getCustomProperties();
			String contents = document.get();
			ResourceEditorInput resourceEditorInput = (ResourceEditorInput) element;
			
			resourceEditorInput.getResource().setDescription(editor.getResourceDescription());
			resourceEditorInput.getResource().setModified(true);
			String outContents = resourceEditorInput.getResource().evaluate(props, contents);
			if (null != resourceEditorInput.getFile()) {
				// workspace level resource
				try {
					FileOutputStream fos = null;
					try {
						fos = new FileOutputStream(resourceEditorInput.getFile(), false);
						fos.write(outContents.getBytes());
						ResourceManager.reloadWorkspaceCache();
					}
					finally {
						if (null != fos) fos.close();
					}
				}
				catch (Exception e) {
					throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, IStatus.OK, e.getMessage(), e));
				}
			}
			else if (null != resourceEditorInput.getIFile()) {
				// project level resource
				resourceEditorInput.getIFile().setContents(new ByteArrayInputStream(outContents.getBytes()), true, true, null);
				ResourceManager.getInstance(resourceEditorInput.getIFile().getProject()).reloadProjectCache();
			}
		}
	}
}