package com.hudson.hibernatesynchronizer.editors.synchronizer;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;

import com.hudson.hibernatesynchronizer.editors.AnnotationHover;
import com.hudson.hibernatesynchronizer.editors.synchronizer.completion.CompletionProcessor;

public class Configuration extends SourceViewerConfiguration {
	private DoubleClickStrategy doubleClickStrategy;
	private XMLTagScanner tagScanner;
	private Scanner scanner;
	private ColorManager colorManager;
	private Editor editor;

	public Configuration(ColorManager colorManager, Editor editor) {
		this.colorManager = colorManager;
		this.editor = editor;
	}
	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return new String[] {
			IDocument.DEFAULT_CONTENT_TYPE,
			PartitionScanner.XML_COMMENT,
			PartitionScanner.XML_TAG };
	}
	public ITextDoubleClickStrategy getDoubleClickStrategy(
		ISourceViewer sourceViewer,
		String contentType) {
		if (doubleClickStrategy == null)
			doubleClickStrategy = new DoubleClickStrategy();
		return doubleClickStrategy;
	}

	protected Scanner getXMLScanner() {
		if (scanner == null) {
			scanner = new Scanner(colorManager);
			scanner.setDefaultReturnToken(
				new Token(
					new TextAttribute(
						colorManager.getColor(ColorManager.COLOR_DEFAULT))));
		}
		return scanner;
	}
	protected XMLTagScanner getXMLTagScanner() {
		if (tagScanner == null) {
			tagScanner = new XMLTagScanner(colorManager);
			tagScanner.setDefaultReturnToken(
				new Token(
					new TextAttribute(
						colorManager.getColor(ColorManager.COLOR_TAG))));
		}
		return tagScanner;
	}

	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		PresentationReconciler reconciler = new PresentationReconciler();

		DefaultDamagerRepairer dr =
			new DefaultDamagerRepairer(getXMLTagScanner());
		reconciler.setDamager(dr, PartitionScanner.XML_TAG);
		reconciler.setRepairer(dr, PartitionScanner.XML_TAG);

		dr = new DefaultDamagerRepairer(getXMLScanner());
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

		NonRuleBasedDamagerRepairer ndr =
			new NonRuleBasedDamagerRepairer(
				new TextAttribute(
					colorManager.getColor(ColorManager.COLOR_XML_COMMENT)));
		reconciler.setDamager(ndr, PartitionScanner.XML_COMMENT);
		reconciler.setRepairer(ndr, PartitionScanner.XML_COMMENT);

		return reconciler;
	}

	public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) {
		return new AnnotationHover();
	}

    public IContentAssistant getContentAssistant(ISourceViewer aSourceViewer)
    {
        ContentAssistant assistant = new ContentAssistant();
        CompletionProcessor completionProcessor = new CompletionProcessor(editor.getFile());
        assistant.setContentAssistProcessor(completionProcessor, IDocument.DEFAULT_CONTENT_TYPE);
        assistant.setContentAssistProcessor(completionProcessor, PartitionScanner.XML_TAG);
        assistant.enableAutoInsert(true);
        assistant.enableAutoActivation(true);

        return assistant;
    }
}