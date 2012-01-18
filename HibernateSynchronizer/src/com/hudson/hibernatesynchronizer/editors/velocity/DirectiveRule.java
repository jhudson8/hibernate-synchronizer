package com.hudson.hibernatesynchronizer.editors.velocity;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.SingleLineRule;


/**
 * @author Joe Hudson
 */
public class DirectiveRule extends SingleLineRule {

	private static final char NBR_SGN = '#';
	
	public DirectiveRule(IToken token, String directive) {
		super(NBR_SGN + directive, null, token);
	}

	protected boolean endSequenceDetected(ICharacterScanner scanner) {
		int stack = 0;
		int readChars = 1;
		char c = (char) scanner.read();
		while (c != '\n') {
			if (c == '(') stack++;
			else if (c == ')') {
				if (stack <= 1) {
					return true;
				}
				else stack --;
			}
			c = (char) scanner.read();
			readChars++;
		}
		for (int i=0; i<readChars; i++) scanner.unread();
		return false;
	}
}