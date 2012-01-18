package com.hudson.hibernatesynchronizer.editors.velocity.completion;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.SingleLineRule;

/**
 * @author Joe Hudson
 */
public class VariableRule extends SingleLineRule {

	/**
	 * @param startSequence
	 * @param endSequence
	 * @param token
	 */
	public VariableRule(IToken token) {
		super("$", null, token);
	}


	/**
	 * @see org.eclipse.jface.text.rules.PatternRule#endSequenceDetected(org.eclipse.jface.text.rules.ICharacterScanner)
	 */
	protected boolean endSequenceDetected(ICharacterScanner scanner) {
		char c;
		char[][] delimiters= scanner.getLegalLineDelimiters();
		boolean previousWasEscapeCharacter = false;	
		while ((c = (char) scanner.read()) != ICharacterScanner.EOF) {
			if (c == fEscapeCharacter) {
				// Skip the escaped character.
				scanner.read();
			} else if (!(Character.isLetterOrDigit(c) || c == '.' || c == '_' || c == '-')) {
				scanner.unread();
				// Check if the specified end sequence has been found.
				return true;
			} else if (fBreaksOnEOL) {
				// Check for end of line since it can be used to terminate the pattern.
				for (int i= 0; i < delimiters.length; i++) {
					if (c == delimiters[i][0] && sequenceDetected(scanner, delimiters[i], true)) {
						if (!fEscapeContinuesLine || !previousWasEscapeCharacter)
							return true;
					}
				}
			}
			previousWasEscapeCharacter = (c == fEscapeCharacter);
		}
		if (fBreaksOnEOF) return true;
		scanner.unread();
		return false;
	}
}
