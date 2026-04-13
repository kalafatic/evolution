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

/**
 * The Enum enum EExt.
 * 
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public enum EExt {

	/** The TXT. */
	TXT(0, ".txt", EMimeType.TXT, EMimeType.TXT),

	/** The XML. */
	XML(1, ".xml", EMimeType.XML, EMimeType.XML),

	/** The XSD. */
	XSD(1, ".xsd", EMimeType.XML, EMimeType.XML),

	/** The ECORE. */
	ECORE(1, ".ecore", EMimeType.XML, EMimeType.XML),

	/** The HTM. */
	HTM(2, ".htm", EMimeType.HTM, EMimeType.XML),

	/** The HTML. */
	HTML(3, ".html", EMimeType.HTM, EMimeType.XML),

	/** The JNLP. */
	JNLP(1, ".jnlp", EMimeType.JNLP, EMimeType.XML),

	/** The TORRENT. */
	TORRENT(4, ".torrent", EMimeType.TORRENT, EMimeType.TORRENT),

	/** The JPEG. */
	JPEG(3, ".jpeg", EMimeType.JPEG, EMimeType.JPEG),

	/** The JPG. */
	JPG(3, ".jpg", EMimeType.JPEG, EMimeType.JPEG),

	/** The PNG. */
	PNG(3, ".png", EMimeType.JPEG, EMimeType.JPEG),

	/** The GIF. */
	GIF(3, ".gif", EMimeType.JPEG, EMimeType.JPEG),

	/** The CSS. */
	CSS(3, ".css", EMimeType.JPEG, EMimeType.JPEG),

	/** The PDF. */
	PDF(3, ".pdf", EMimeType.JPEG, EMimeType.JPEG),

	;

	/** The index. */
	public int index;

	/** The ext. */
	public String ext;

	/** The mime. */
	public EMimeType mime;

	/** The protocol. */
	public EMimeType protocol;

	/**
	 * Instantiates a new e ext.
	 * 
	 * @param index
	 *            the index
	 * @param ext
	 *            the ext
	 * @param mime
	 *            the mime
	 * @param protocol
	 *            the protocol
	 */
	EExt(int index, String ext, EMimeType mime, EMimeType protocol) {
		this.index = index;
		this.ext = ext;
		this.mime = mime;
		this.protocol = protocol;
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the extension.
	 * 
	 * @param ext
	 *            the ext
	 * @return the extension
	 */
	public static EExt getExtension(String ext) {
		for (EExt eExt : EExt.values()) {
			if (ext.equalsIgnoreCase(eExt.ext)) {
				return eExt;
			}
		}
		return null;
	}
}
