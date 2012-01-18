package com.hudson.hibernatesynchronizer.editors.velocity.completion;

import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;


public class ForeachDirective extends AbstractDirective {

	private static final int STATE_WAITING_FOR_1 = 1;
	private static final int STATE_IN_1 = 2;
	private static final int STATE_WAITING_FOR_IN = 3;
	private static final int STATE_IN_IN = 4;
	private static final int STATE_WAITING_FOR_2 = 5;
	private static final int STATE_IN_2 = 6;
	private static final int STATE_AFTER_2 = 7;
	private static final int STATE_IN_2_PARAMETERS = 8;
	private static final int STATE_IN_2_PARAMETERS_QUOTE = 8;
	
	protected void loadVariables(IProject project, ClassLoader loader, Map variables) {
		String content = getInsideText();
		StringBuffer token = new StringBuffer();
		StringBuffer value = new StringBuffer();
		int state = STATE_WAITING_FOR_1;
		char[] arr = content.toCharArray();
		for (int i=0; i<arr.length; i++) {
			char c = arr[i];
			if (Character.isLetterOrDigit(c)) {
				if (state == STATE_WAITING_FOR_1 || state == STATE_IN_1) {
					if (state == STATE_WAITING_FOR_1) state = STATE_IN_1;
					token.append(c);
				}
				else if (state == STATE_WAITING_FOR_2 || state == STATE_IN_2) {
					if (state == STATE_WAITING_FOR_2) state = STATE_IN_2;
					value.append(c);
				}
				else if (state == STATE_WAITING_FOR_IN) {
					if (c == 'i' && arr.length >= i+2 && arr[i+1]=='n') {
						i++;
						state = STATE_WAITING_FOR_2;
					}
				}
				else break;
			}
			else if (c == '.') {
				if (state == STATE_IN_1) {
					break;
				}
				else if (state == STATE_IN_2) {
					state = STATE_IN_2;
					value.append(c);
				}
				else break;
			}
			else if (Character.isWhitespace(c)) {
				if (state == STATE_IN_1) {
					state = STATE_WAITING_FOR_IN;
				}
				else if (state == STATE_IN_2) {
					state = STATE_AFTER_2;
				}
			}
			else if (c == '(') {
				if (state == STATE_IN_2 || state == STATE_AFTER_2) {
					for (int j=i+1; j<arr.length; j++) {
						if (arr[j] != ')') value.append(arr[j]);
						else break;
					}
				}
				break;
			}
			else if (c == ')') {
				break;
			}
		}
		if (state == STATE_IN_2 || state == STATE_AFTER_2) {
			// we have good data
			Class c = getObjectClass(project, value.toString(), loader, variables, true);
			variables.put(token.toString(), c);
		}
	}

	public List getCompletionProposals (IProject project, int pos, Map addedValues, ClassLoader loader) throws Exception {
		return getCompletionProposals(project, document, pos, addedValues, loader, false);
	}
}