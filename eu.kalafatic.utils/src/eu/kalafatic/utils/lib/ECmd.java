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
package eu.kalafatic.utils.lib;

import static eu.kalafatic.utils.constants.FCMDConstants.DOC_ASSOC;
import static eu.kalafatic.utils.constants.FCMDConstants.DOC_COMMAND;
import static eu.kalafatic.utils.constants.FCMDConstants.FILE_COMMAND;
import static eu.kalafatic.utils.constants.FCMDConstants.IMG_ASSOC;
import static eu.kalafatic.utils.constants.FCMDConstants.IMG_COMMAND;
import static eu.kalafatic.utils.constants.FCMDConstants.PDF_ASSOC;
import static eu.kalafatic.utils.constants.FCMDConstants.PDF_COMMAND;
import static eu.kalafatic.utils.constants.FCMDConstants.TXT_ASSOC;
import static eu.kalafatic.utils.constants.FCMDConstants.TXT_COMMAND;

import java.util.List;

/**
 * The Enum enum ECmd.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public enum ECmd {

	/** The OPE n_ file. */
	OPEN_FILE(0, null, FILE_COMMAND),

	/** The OPE n_ txt. */
	OPEN_TXT(1, TXT_ASSOC, TXT_COMMAND),

	/** The OPE n_ doc. */
	OPEN_DOC(2, DOC_ASSOC, DOC_COMMAND),

	/** The OPE n_ pdf. */
	OPEN_PDF(3, PDF_ASSOC, PDF_COMMAND),

	/** The OPE n_ img. */
	OPEN_IMG(4, IMG_ASSOC, IMG_COMMAND)

	;

	/** The index. */
	public int index;

	/** The assoc. */
	public List<String> assoc;

	/** The command. */
	public String command;

	// Constructor
	/**
	 * Instantiates a new e cmd.
	 * @param index the index
	 * @param assoc the assoc
	 * @param ecmd the ecmd
	 */
	ECmd(int index, List<String> assoc, String ecmd) {
		this.index = index;
		this.assoc = assoc;
		this.command = ecmd;
	}
}
