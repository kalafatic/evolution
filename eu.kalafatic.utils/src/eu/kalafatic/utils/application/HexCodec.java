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
package eu.kalafatic.utils.application;

import java.util.Formatter;

/**
 * The Class class HexCodec.
 * 
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class HexCodec {

	/** The Constant kDigits. */
	private static final char[] kDigits = { '0', '1', '2', '3', '4', '5', '6',
			'7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

	/** The HEXES. */
	private static String HEXES = "0123456789ABCDEF";

	// ---------------------------------------------------------------

	/**
	 * Bytes to hex.
	 * 
	 * @param raw
	 *            the raw
	 * @return the char[]
	 */
	public static char[] bytesToHex(byte[] raw) {
		int length = raw.length;
		char[] hex = new char[length * 2];
		for (int i = 0; i < length; i++) {
			int value = (raw[i] + 256) % 256;
			int highIndex = value >> 4;
			int lowIndex = value & 0x0f;
			hex[i * 2 + 0] = kDigits[highIndex];
			hex[i * 2 + 1] = kDigits[lowIndex];
		}
		return hex;
	}

	// ---------------------------------------------------------------

	/**
	 * Hex to bytes.
	 * 
	 * @param hex
	 *            the hex
	 * @return the byte[]
	 */
	public static byte[] hexToBytes(char[] hex) {
		int length = hex.length / 2;
		byte[] raw = new byte[length];
		for (int i = 0; i < length; i++) {
			int high = Character.digit(hex[i * 2], 16);
			int low = Character.digit(hex[i * 2 + 1], 16);
			int value = (high << 4) | low;
			if (value > 127) {
				value -= 256;
			}
			raw[i] = (byte) value;
		}
		return raw;
	}

	// ---------------------------------------------------------------

	/**
	 * Hex string to bytes.
	 * 
	 * @param hex
	 *            the hex
	 * @return the byte[]
	 */
	public static byte[] hexStringToBytes(String hex) {
		return hexToBytes(hex.toCharArray());
	}

	// ---------------------------------------------------------------

	/**
	 * Bytes to hex string.
	 * 
	 * @param raw
	 *            the raw
	 * @return the string
	 */
	public static String bytesToHexString(byte[] raw) {
		final StringBuilder hex = new StringBuilder(2 * raw.length);

		for (final byte b : raw) {
			hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(
					HEXES.charAt((b & 0x0F)));
		}
		return hex.toString();
	}

	// ---------------------------------------------------------------

	/**
	 * As hex.
	 * 
	 * @param buf
	 *            the buf
	 * @return the string
	 */
	public static String asHex(byte[] buf) {
		Formatter formatter = new Formatter();
		for (byte b : buf) {
			formatter.format("%02x", b);
		}
		return formatter.toString();
	}
}
