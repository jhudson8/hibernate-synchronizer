package com.hudson.hibernatesynchronizer.editors.velocity;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WordPatternRule;

import com.hudson.hibernatesynchronizer.editors.velocity.completion.VariableRule;


public class PartitionScanner extends RuleBasedPartitionScanner {
	public final static String FOREACH_PARTITION = "__foreach_partition";
	public final static String FOREACH_END_PARTITION = "__foreach_end_partition";
	public final static String SET_PARTITION = "__set_partition";
	public final static String IF_PARTITION = "__if_partition";
	public final static String IF_END_PARTITION = "__if_end_partition";
	public final static String END_PARTITION = "__end_partition";
	public final static String MACRO_PARTITION = "__macro_partition";
	public final static String VARIABLE_PARTITION = "__variable_partition";
	public final static String COMMENT_PARTITION = "__comment_partition";
	
	private IDocument document;
	
	public PartitionScanner() {
		IPredicateRule[] predicateRules = new IPredicateRule[] {
			new DirectiveRule(new Token(FOREACH_PARTITION), "foreach"),
			new DirectiveRule(new Token(SET_PARTITION), "set"),
			new DirectiveRule(new Token(IF_PARTITION), "if"),
			new SingleLineRule("#macro", ")", new Token(MACRO_PARTITION)),
			new EndRule(new Token(FOREACH_END_PARTITION), "foreach"),
			new EndRule(new Token(IF_END_PARTITION), "if"),
			new WordPatternRule(new EndDetector(), "#end", "", new Token(END_PARTITION)),
			new SingleLineRule("#else", null, new Token(IF_PARTITION)),
			new MultiLineRule("#*", "*#", new Token(COMMENT_PARTITION)),
			new SingleLineRule("##", "\n", new Token(COMMENT_PARTITION)),
			new SingleLineRule("${", "}", new Token(VARIABLE_PARTITION)),
			new SingleLineRule("$!{", "}", new Token(VARIABLE_PARTITION)),
			new VariableRule(new Token(VARIABLE_PARTITION))
		};
		
		setPredicateRules(predicateRules);
	}

	public IDocument getDocument () {
		return document;
	}

	/**
	 * @see org.eclipse.jface.text.rules.IPartitionTokenScanner#setPartialRange(org.eclipse.jface.text.IDocument, int, int, java.lang.String, int)
	 */
	public void setPartialRange(IDocument document, int offset, int length,
			String contentType, int partitionOffset) {
		this.document = document;
		super.setPartialRange(document, offset, length, contentType,
				partitionOffset);
	}
	/**
	 * @see org.eclipse.jface.text.rules.ITokenScanner#setRange(org.eclipse.jface.text.IDocument, int, int)
	 */
	public void setRange(IDocument document, int offset, int length) {
		this.document = document;
		super.setRange(document, offset, length);
	}
}