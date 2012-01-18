/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.editor;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;

/**
 * @author <a href="mailto: joe@binamics.com">Joe Hudson </a>
 */
public class XMLScanner extends RuleBasedScanner {

    public XMLScanner(ColorManager manager) {
        IToken procInstr = new Token(new TextAttribute(manager
                .getColor(IXMLColorConstants.PROC_INSTR)));

        IRule[] rules = new IRule[2];
        //Add rule for processing instructions
        rules[0] = new SingleLineRule("<?", "?>", procInstr);
        // Add generic whitespace rule.
        rules[1] = new WhitespaceRule(new XMLWhitespaceDetector());

        setRules(rules);
    }
}