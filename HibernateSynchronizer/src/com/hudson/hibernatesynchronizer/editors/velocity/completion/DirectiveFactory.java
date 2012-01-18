package com.hudson.hibernatesynchronizer.editors.velocity.completion;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITypedRegion;

import com.hudson.hibernatesynchronizer.editors.velocity.PartitionScanner;

public class DirectiveFactory {

	public static boolean isEndDirective (String directiveType) {
		return PartitionScanner.END_PARTITION.equals(directiveType);
	}

	public static IDirective getDirective (String directiveType, ITypedRegion region, IDocument document) {
		IDirective directive = null;
		if (region.getType().equals(PartitionScanner.FOREACH_PARTITION)) {
			directive = new ForeachDirective();
		}
		else if (region.getType().equals(PartitionScanner.IF_PARTITION)) {
			directive = new IfDirective();
		}
		else if (region.getType().equals(PartitionScanner.MACRO_PARTITION)) {
			directive = new MacroDirective();
		}
		else if (region.getType().equals(PartitionScanner.SET_PARTITION)) {
			directive = new SetDirective();
		}
		if (null != directive) directive.load(region.getOffset(), region.getLength(), document);
		return directive;
	}
}
