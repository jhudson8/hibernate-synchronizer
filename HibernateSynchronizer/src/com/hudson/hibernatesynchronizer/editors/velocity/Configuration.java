package com.hudson.hibernatesynchronizer.editors.velocity;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;

import com.hudson.hibernatesynchronizer.editors.velocity.completion.CompletionProcessor;

public class Configuration extends SourceViewerConfiguration {
	private DoubleClickStrategy doubleClickStrategy;
	private ColorManager colorManager;
	private Editor editor;

	public Configuration(ColorManager colorManager, Editor editor) {
		this.editor = editor;
		this.colorManager = colorManager;
	}
	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return new String[] {
			IDocument.DEFAULT_CONTENT_TYPE,
			PartitionScanner.FOREACH_PARTITION,
			PartitionScanner.END_PARTITION,
			PartitionScanner.IF_PARTITION,
			PartitionScanner.MACRO_PARTITION,
			PartitionScanner.SET_PARTITION,
			PartitionScanner.FOREACH_END_PARTITION,
			PartitionScanner.IF_END_PARTITION,
			PartitionScanner.VARIABLE_PARTITION,
			PartitionScanner.COMMENT_PARTITION,
		};
	}
	public ITextDoubleClickStrategy getDoubleClickStrategy(
		ISourceViewer sourceViewer,
		String contentType) {
		if (doubleClickStrategy == null)
			doubleClickStrategy = new DoubleClickStrategy();
		return doubleClickStrategy;
	}

	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		PresentationReconciler reconciler = new PresentationReconciler();

		NonRuleBasedDamagerRepairer ndr =
			new NonRuleBasedDamagerRepairer(
				new TextAttribute(
					colorManager.getColor(ColorManager.COLOR_FOREACH_DIRECTIVE)));
		reconciler.setDamager(ndr, PartitionScanner.FOREACH_PARTITION);
		reconciler.setRepairer(ndr, PartitionScanner.FOREACH_PARTITION);
		reconciler.setDamager(ndr, PartitionScanner.FOREACH_END_PARTITION);
		reconciler.setRepairer(ndr, PartitionScanner.FOREACH_END_PARTITION);
		ndr =
			new NonRuleBasedDamagerRepairer(
				new TextAttribute(
					colorManager.getColor(ColorManager.COLOR_SET_DIRECTIVE)));
		reconciler.setDamager(ndr, PartitionScanner.SET_PARTITION);
		reconciler.setRepairer(ndr, PartitionScanner.SET_PARTITION);
		ndr =
			new NonRuleBasedDamagerRepairer(
				new TextAttribute(
					colorManager.getColor(ColorManager.COLOR_IF_DIRECTIVE)));
		reconciler.setDamager(ndr, PartitionScanner.IF_PARTITION);
		reconciler.setRepairer(ndr, PartitionScanner.IF_PARTITION);
		reconciler.setDamager(ndr, PartitionScanner.IF_END_PARTITION);
		reconciler.setRepairer(ndr, PartitionScanner.IF_END_PARTITION);

		ndr =
			new NonRuleBasedDamagerRepairer(
				new TextAttribute(
					colorManager.getColor(ColorManager.COLOR_VARIABLE)));
		reconciler.setDamager(ndr, PartitionScanner.VARIABLE_PARTITION);
		reconciler.setRepairer(ndr, PartitionScanner.VARIABLE_PARTITION);

		ndr =
			new NonRuleBasedDamagerRepairer(
				new TextAttribute(
					colorManager.getColor(ColorManager.COLOR_COMMENT)));
		reconciler.setDamager(ndr, PartitionScanner.COMMENT_PARTITION);
		reconciler.setRepairer(ndr, PartitionScanner.COMMENT_PARTITION);
		return reconciler;
	}

    public IContentAssistant getContentAssistant(ISourceViewer aSourceViewer)
    {
        ContentAssistant assistant = new ContentAssistant();
        CompletionProcessor completionProcessor = new CompletionProcessor(editor);
        assistant.setContentAssistProcessor(completionProcessor, IDocument.DEFAULT_CONTENT_TYPE);
        assistant.setContentAssistProcessor(completionProcessor, PartitionScanner.FOREACH_PARTITION);
        assistant.setContentAssistProcessor(completionProcessor, PartitionScanner.IF_PARTITION);
        assistant.setContentAssistProcessor(completionProcessor, PartitionScanner.MACRO_PARTITION);
        assistant.setContentAssistProcessor(completionProcessor, PartitionScanner.SET_PARTITION);
        assistant.setContentAssistProcessor(completionProcessor, PartitionScanner.VARIABLE_PARTITION);
        assistant.enableAutoInsert(true);
        assistant.enableAutoActivation(true);
        return assistant;
    }
}