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
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

/**
 * The Class class XMLTextPredicateRule.
 * 
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class XMLTextPredicateRule implements IPredicateRule
{

	/** The token. */
	private IToken token;
	
	/** The chars read. */
	private int charsRead;
	
	/** The white space only. */
	private boolean whiteSpaceOnly;
	
	/** The in cdata. */
	boolean inCdata;

	/**
	 * Instantiates a new xML text predicate rule.
	 * 
	 * @param text
	 *            the text
	 */
	public XMLTextPredicateRule(IToken text)
	{
		this.token = text;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.rules.IPredicateRule#getSuccessToken()
	 */
	public IToken getSuccessToken()
	{
		return token;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.rules.IPredicateRule#evaluate(org.eclipse.jface.text.rules.ICharacterScanner, boolean)
	 */
	public IToken evaluate(ICharacterScanner scanner, boolean resume)
	{
		return evaluate(scanner);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.rules.IRule#evaluate(org.eclipse.jface.text.rules.ICharacterScanner)
	 */
	public IToken evaluate(ICharacterScanner scanner)
	{

		reinit();
		
		int c = 0;

		//carry on reading until we find a bad char
		//int chars = 0;
		while (isOK(c = read(scanner), scanner))
		{
			//add character to buffer
			if (c == ICharacterScanner.EOF)
			{
				return Token.UNDEFINED;
			}

			whiteSpaceOnly = whiteSpaceOnly && (Character.isWhitespace((char) c));
		}

		unread(scanner);

		//if we have only read whitespace characters, go back to where evaluation started and return undefined token
		if (whiteSpaceOnly)
		{
			rewind(scanner, charsRead);
			return Token.UNDEFINED;
		}

		return token;

	}


	/**
	 * Checks if is oK.
	 * 
	 * @param cc
	 *            the cc
	 * @param scanner
	 *            the scanner
	 * @return true, if is oK
	 */
	private boolean isOK(int cc, ICharacterScanner scanner)
	{
		
		char c = (char) cc;

		if (!inCdata)
		{
			if (c == '<')
			{

				int cdataCharsRead = 0;

				for (int i = 0; i < "![CDATA[".length(); i++)
				{
					//whiteSpaceOnly = false;

					c = (char) read(scanner);
					cdataCharsRead++;
					
					if (c != "![CDATA[".charAt(i))
					{
						
						//we don't have a match - wind back only the cdata characters
						rewind(scanner, cdataCharsRead);
						inCdata = false;
						return false;
					}
				}

				inCdata = true;
				return true;

				//return false;
			}
		}
		else
		{

			if (c == ']')
			{

				for (int i = 0; i < "]>".length(); i++)
				{

					c = (char) read(scanner);

					if (c != "]>".charAt(i))
					{
						//we're still in the CData section, so just continue processing
						return true;
					}
				}

				//we found all the matching characters at the end of the CData section, so break out of this
				inCdata = false;

				//we're still in XML text
				return true;

			}
		}

		return true;

	}
	
	

	/**
	 * Rewind.
	 * 
	 * @param scanner
	 *            the scanner
	 * @param theCharsRead
	 *            the the chars read
	 */
	private void rewind(ICharacterScanner scanner, int theCharsRead)
	{
		while (theCharsRead > 0)
		{
			theCharsRead--;
			unread(scanner);
		}
	}
	
	/**
	 * Unread.
	 * 
	 * @param scanner
	 *            the scanner
	 */
	private void unread(ICharacterScanner scanner)
	{
		scanner.unread();
		charsRead--;
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
		charsRead++;
		return c;
	}
	

	/**
	 * Reinit.
	 */
	private void reinit()
	{
		charsRead = 0;
		whiteSpaceOnly = true;
	}

}