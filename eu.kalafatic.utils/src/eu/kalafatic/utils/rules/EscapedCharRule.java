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
package eu.kalafatic.utils.rules;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

/**
 * The Class class EscapedCharRule.
 * 
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class EscapedCharRule implements IRule
{

	/** The token. */
	IToken fToken;
	
	/** The buffer. */
	StringBuffer buffer = new StringBuffer();

	/**
	 * Instantiates a new escaped char rule.
	 * 
	 * @param token
	 *            the token
	 */
	public EscapedCharRule(IToken token)
	{
		super();
		this.fToken = token;
	}

	/*
	 * @see IRule#evaluate(ICharacterScanner)
	 */
	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.rules.IRule#evaluate(org.eclipse.jface.text.rules.ICharacterScanner)
	 */
	public IToken evaluate(ICharacterScanner scanner)
	{

		buffer.setLength(0);

		int c = read(scanner);
		if (c == '&')
		{
		    
		    int i = 0;
			do
			{
				c = read(scanner);
				i++;
				
				if (c == '<' || c == ']')
				{
				    System.out.println("Char " + (char)c);
					for (int j= i-1; j > 0; j--)
						scanner.unread();
					return Token.UNDEFINED;
				}
			}
			while (c != ';');
			return fToken;
		}

		scanner.unread();
		return Token.UNDEFINED;
	}
	

	/**
	 * Read.
	 * 
	 * @param scanner
	 *            the scanner
	 * @return the int
	 */
	private int read(ICharacterScanner scanner)
	{
		int c = scanner.read();
		buffer.append((char) c);
		return c;
	}

	
}