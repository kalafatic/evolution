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

/**
 * The Class class XMLStringPartitionScanner.
 * 
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class XMLStringPartitionScanner extends RuleBasedPartitionScanner {

	/** The Constant XML_STRING. */
	public final static String XML_STRING = "__xml_string";

	/**
	 * Instantiates a new xML string partition scanner.
	 */
	public XMLStringPartitionScanner() {
		IToken stringToken = new Token(XML_STRING);
		IPredicateRule[] rules = new IPredicateRule[1];

		rules[0] = new MultiLineRule("\"", "\"", stringToken, '\\');
		setPredicateRules(rules);
	}

}
