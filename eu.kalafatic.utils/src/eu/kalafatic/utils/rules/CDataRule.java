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
 * The Class class CDataRule.
 * 
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class CDataRule implements IRule
{

	/** The token. */
	IToken fToken;
	
	/** The buffer. */
	StringBuffer buffer = new StringBuffer();
	
	/** The chars read. */
	int charsRead = 0;

	/** The match string. */
	private String matchString;	
	
	/** The Constant START_MATCH_STRING. */
	private static final String START_MATCH_STRING  = "<![CDATA[";
	
	/** The Constant END_MATCH_STRING. */
	private static final String END_MATCH_STRING = "]]>";

	
	/**
	 * Instantiates a new c data rule.
	 * 
	 * @param token
	 *            the token
	 * @param start
	 *            the start
	 */
	public CDataRule(IToken token, boolean start)
	{
		super();
		this.fToken = token;
		this.matchString = start?START_MATCH_STRING:END_MATCH_STRING;
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

		charsRead = 0;
		int c = read(scanner);

		if (c == matchString.charAt(0))
		{
			do
			{
				c = read(scanner);
			}
			while (isOK((char) c));

			if (charsRead == matchString.length())
			{
				return fToken;
			}
			else
			{
				rewind(scanner);
				return Token.UNDEFINED;
			}

		}

		scanner.unread();
		return Token.UNDEFINED;
	}

	/**
	 * Rewind.
	 * 
	 * @param scanner
	 *            the scanner
	 */
	private void rewind(ICharacterScanner scanner)
	{
		int rewindLength = charsRead;
		while (rewindLength > 0)
		{
			scanner.unread();
			rewindLength--;
		}
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
		charsRead++;
		return c;
	}

	/**
	 * Checks if is oK.
	 * 
	 * @param c
	 *            the c
	 * @return true, if is oK
	 */
	private boolean isOK(char c)
	{
		if (charsRead >= matchString.length())
			return false;
		if (matchString.charAt(charsRead - 1) == c)
			return true;
		else
			return false;
	}
}