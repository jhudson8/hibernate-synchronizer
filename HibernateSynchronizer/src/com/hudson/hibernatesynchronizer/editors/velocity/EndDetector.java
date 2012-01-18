package com.hudson.hibernatesynchronizer.editors.velocity;

import org.eclipse.jface.text.rules.IWordDetector;

public class EndDetector implements IWordDetector {

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.rules.IWordDetector#isWordPart(char)
	 */
	public boolean isWordPart(char c) {
		// return (c == 'e' || c == 'n' || c == 'd');
		return false;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.rules.IWordDetector#isWordStart(char)
	 */
	public boolean isWordStart(char c) {
		return (c == '#');
	}
}
