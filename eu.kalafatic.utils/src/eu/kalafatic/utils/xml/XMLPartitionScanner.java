/*******************************************************************************
 * Copyright (c) 2010, Petr Kalafatic (gemini@kalafatic.eu).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU GPL Version 3 
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.txt  
 * 
 * Contributors:
 *     Petr Kalafatic - initial API and implementation
 ******************************************************************************/
package eu.kalafatic.utils.xml;

import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.Token;

import eu.kalafatic.utils.rules.NonMatchingRule;
import eu.kalafatic.utils.rules.StartTagRule;
import eu.kalafatic.utils.rules.XMLTextPredicateRule;

/**
 * The Class class XMLPartitionScanner.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class XMLPartitionScanner extends RuleBasedPartitionScanner {

	/** The Constant XML_TAG. */
	public final static String XML_TAG = "__xml_tag";

	/** The Constant XML_DEFAULT. */
	public final static String XML_DEFAULT = "__xml_default";

	/** The Constant XML_COMMENT. */
	public final static String XML_COMMENT = "__xml_comment";

	/** The Constant XML_PI. */
	public final static String XML_PI = "__xml_pi";

	/** The Constant XML_DOCTYPE. */
	public final static String XML_DOCTYPE = "__xml_doctype";

	/** The Constant XML_CDATA. */
	public final static String XML_CDATA = "__xml_cdata";

	/** The Constant XML_START_TAG. */
	public final static String XML_START_TAG = "__xml_start_tag";

	/** The Constant XML_END_TAG. */
	public final static String XML_END_TAG = "__xml_end_tag";

	/** The Constant XML_TEXT. */
	public final static String XML_TEXT = "__xml_text";

	/**
	 * Instantiates a new xML partition scanner.
	 */
	public XMLPartitionScanner() {

		IToken tag = new Token(XML_TAG);
		IToken xmlComment = new Token(XML_COMMENT);
		IToken xmlPI = new Token(XML_PI);
		IToken startTag = new Token(XML_START_TAG);
		IToken endTag = new Token(XML_END_TAG);
		IToken docType = new Token(XML_DOCTYPE);
		IToken text = new Token(XML_TEXT);

		IPredicateRule[] rules = new IPredicateRule[7];

		rules[0] = new NonMatchingRule();
		rules[1] = new MultiLineRule("<!--", "-->", xmlComment);
		rules[2] = new MultiLineRule("<?", "?>", xmlPI);
		rules[3] = new MultiLineRule("</", ">", endTag);
		rules[4] = new StartTagRule(startTag);
		rules[5] = new MultiLineRule("<!DOCTYPE", ">", docType);
		rules[6] = new XMLTextPredicateRule(text);

		setPredicateRules(rules);
	}
}
