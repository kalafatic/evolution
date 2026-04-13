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
package eu.kalafatic.utils.convert;

import static eu.kalafatic.utils.constants.FUIConstants.GB;
import static eu.kalafatic.utils.constants.FUIConstants.H;
import static eu.kalafatic.utils.constants.FUIConstants.KB;
import static eu.kalafatic.utils.constants.FUIConstants.M;
import static eu.kalafatic.utils.constants.FUIConstants.MB;
import static eu.kalafatic.utils.constants.FUIConstants.S;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * The Class class ConvertUtils.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class ConvertUtils {

	/**
	 * Gets the integer.
	 * @param object the object
	 * @return the integer
	 */
	public static Integer getInteger(Object object) {
		Integer integer = null;
		if (object instanceof Long) {
			Long l = (Long) object;
			integer = new Integer(l.intValue());
		} else {
			integer = (Integer) object;
		}
		return integer;
	}

	// ---------------------------------------------------------------

	/**
	 * Unsigned byte to int.
	 * @param b the b
	 * @return the int
	 */
	public static int unsignedByteToInt(byte b) {
		return b & 0xFF;
	}

	// ---------------------------------------------------------------

	/**
	 * Byte buffer to byte array.
	 * @param byteBuffer the byte buffer
	 * @param offset the offset
	 * @return the byte[]
	 */
	public synchronized byte[] byteBufferToByteArray(ByteBuffer byteBuffer, int offset) {
		byteBuffer.rewind();
		byte[] array = new byte[offset];
		byteBuffer.get(array);
		return array;
	}

	// ---------------------------------------------------------------

	/**
	 * Byte arrays to byte array.
	 * @param size the size
	 * @param bytes the bytes
	 * @return the byte[]
	 */
	public synchronized byte[] byteArraysToByteArray(int size, byte[]... bytes) {
		byte[] result = null;
		ByteBuffer byteBuffer;

		try {
			byteBuffer = ByteBuffer.allocateDirect(size);
			result = new byte[size];

			for (int i = 0; i < bytes.length; i++) {
				byteBuffer.put(bytes[i]);
			}
			// byteBuffer.flip(); // wrong limit
			byteBuffer.rewind();
			byteBuffer.get(result);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	// ---------------------------------------------------------------

	/**
	 * Decode two byte number.
	 * @param portArray the port array
	 * @param index the index
	 * @return the int
	 */
	public int decodeTwoByteNumber(byte[] portArray, int index) {
		return ((portArray[index] & 0xff) * 256 + (portArray[index + 1] & 0xff));
	}

	// ---------------------------------------------------------------

	/**
	 * Decode four byte number.
	 * @param b the b
	 * @return the int
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public int decodeFourByteNumber(final byte[] b) throws IOException {
		return (b[0] & 0xff) << 24 | (b[1] & 0xff) << 16 | (b[2] & 0xff) << 8 | (b[3] & 0xff);
	}

	// ---------------------------------------------------------------

	/**
	 * Decode four byte number.
	 * @param b the b
	 * @param i the i
	 * @return the int
	 */
	public int decodeFourByteNumber(byte[] b, int i) {
		return (b[i] & 0xff) << 24 | (b[i + 1] & 0xff) << 16 | (b[i + 2] & 0xff) << 8 | (b[i + 3] & 0xff);
	}

	// ---------------------------------------------------------------

	/**
	 * Encode four byte number.
	 * @param i the i
	 * @return the byte[]
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public byte[] encodeFourByteNumber(int i) throws IOException {
		return new byte[] { (byte) (i >> 24), (byte) (i >> 16), (byte) (i >> 8), (byte) i };
	}

	// ---------------------------------------------------------------

	/**
	 * Int to byte array.
	 * @param value the value
	 * @return the byte[]
	 */
	public byte[] intToByteArray(int value) {
		return new byte[] { (byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8), (byte) value };
	}

	// ---------------------------------------------------------------

	/**
	 * Decode.
	 * @param byteBuffer the byte buffer
	 * @return the string
	 * @throws CharacterCodingException the character coding exception
	 */
	public String decode(ByteBuffer byteBuffer) throws CharacterCodingException {
		Charset charset = Charset.forName("us-ascii");
		CharsetDecoder decoder = charset.newDecoder();
		CharBuffer charBuffer = decoder.decode(byteBuffer);
		String result = charBuffer.toString();
		return result;
	}

	// ---------------------------------------------------------------

	/**
	 * Convert model bitfield to real bitfield.
	 * @param modelBitfield the model bitfield
	 * @return the byte[]
	 */
	public byte[] convertModelBitfieldToRealBitfield(boolean[] modelBitfield) {

		int size = modelBitfield.length / 8;
		if ((modelBitfield.length % 8) > 0) {
			++size;
		}
		byte[] bitfield = new byte[size];

		boolean[] completeModelBitfield = new boolean[size * 8];
		System.arraycopy(modelBitfield, 0, completeModelBitfield, 0, modelBitfield.length);

		int count = 0;
		for (int i = 0; i < bitfield.length; i++) {
			bitfield[i] += completeModelBitfield[count++] == true ? 128 : 0;
			bitfield[i] += completeModelBitfield[count++] == true ? 64 : 0;
			bitfield[i] += completeModelBitfield[count++] == true ? 32 : 0;
			bitfield[i] += completeModelBitfield[count++] == true ? 16 : 0;
			bitfield[i] += completeModelBitfield[count++] == true ? 8 : 0;
			bitfield[i] += completeModelBitfield[count++] == true ? 4 : 0;
			bitfield[i] += completeModelBitfield[count++] == true ? 2 : 0;
			bitfield[i] += completeModelBitfield[count++] == true ? 1 : 0;
		}
		return bitfield;
	}

	// ---------------------------------------------------------------

	/**
	 * Trimm last piece.
	 * @param realBitfield the real bitfield
	 * @param lastIndex the last index
	 * @param rest the rest
	 * @return the byte[]
	 */
	public static byte[] trimmLastPiece(byte[] realBitfield, int lastIndex, int rest) {
		StringBuffer sb = new StringBuffer("00000000");

		for (int i = 0; i < rest; i++) {
			sb.setCharAt(i, '1');
		}
		BigInteger bi = new BigInteger(sb.toString(), 2);
		realBitfield[lastIndex] = bi.byteValue();
		return realBitfield;
	}

	// ---------------------------------------------------------------

	/**
	 * Mac to string.
	 * @param ch the ch
	 * @param macAddress the mac address
	 * @return the string
	 */
	public static String macToString(char ch, byte[] macAddress) {

		StringBuffer sb = new StringBuffer(17);
		for (int i = 44; i >= 0; i -= 4) {
			int nibble = ((int) (byte2Long(macAddress) >>> i)) & 0xf;
			char nibbleChar = (char) (nibble > 9 ? nibble + ('A' - 10) : nibble + '0');
			sb.append(nibbleChar);
			if ((i & 0x7) == 0 && i != 0) {
				sb.append(ch);
			}
		}
		return sb.toString();
	}

	// ---------------------------------------------------------------

	/**
	 * Byte2 long.
	 * @param addr the addr
	 * @return the long
	 */
	public static long byte2Long(byte addr[]) {
		long address = 0;
		if (addr != null) {
			if (addr.length == 6) {
				address = unsignedByteToLong(addr[5]);
				address |= (unsignedByteToLong(addr[4]) << 8);
				address |= (unsignedByteToLong(addr[3]) << 16);
				address |= (unsignedByteToLong(addr[2]) << 24);
				address |= (unsignedByteToLong(addr[1]) << 32);
				address |= (unsignedByteToLong(addr[0]) << 40);
			}
		}
		return address;
	}

	// ---------------------------------------------------------------

	/**
	 * Contains.
	 * @param array the array
	 * @param value the value
	 * @return true, if successful
	 */
	public boolean contains(boolean[] array, boolean value) {
		for (boolean b : array) {
			if (b == value) {
				return true;
			}
		}
		return false;
	}

	// ---------------------------------------------------------------

	/**
	 * Unsigned byte to long.
	 * @param b the b
	 * @return the long
	 */
	private static long unsignedByteToLong(byte b) {
		return (long) b & 0xFF;
	}

	// ---------------------------------------------------------------

	/**
	 * Log2.
	 * @param x the x
	 * @return the double
	 */
	public static double log2(double x) {
		// Math.log is base e, natural log, ln
		return Math.log(x) / Math.log(2);
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the number as memory size.
	 * @param size the size
	 * @return the number as memory size
	 */
	public static String getNumberAsMemorySize(long size) {
		long number = 0;

		if (size >= GB) {
			number = size / GB;
			return Long.toString(number) + " GB";

		} else if (size >= MB) {
			number = size / MB;
			return Long.toString(number) + " MB";

		} else if (size >= KB) {
			number = size / KB;
			return Long.toString(number) + " kB";
		}
		return Long.toString(size);
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the number as time.
	 * @param time the time
	 * @return the number as time
	 */
	public static String getNumberAsTime(long time) {
		long number = 0;
		String output = "";

		if (time >= H) {
			number = time / H;
			output += Long.toString(number) + " h ";
			time -= number * H;
		}
		if (time >= M) {
			number = time / M;
			output += Long.toString(number) + " m ";
			time -= number * M;
		}
		if (time >= S) {
			number = time / S;
			output += Long.toString(number) + " s ";
			time -= number * S;
		}
		if (time > 0) {
			output += Long.toString(time) + " ms";
		}
		return output;
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the number as speed.
	 * @param size the size
	 * @return the number as speed
	 */
	public static String getNumberAsSpeed(float size) {
		float number = 0;

		if (size > MB) {
			number = size / MB;
			return formatNumber(number) + " MB/s";
		} else if (size > KB) {
			number = size / KB;
			return formatNumber(number) + " kB/s";
		}
		return formatNumber(size) + " B/s";
	}

	// ---------------------------------------------------------------

	/**
	 * Format number.
	 * @param number the number
	 * @return the string
	 */
	public static String formatNumber(float number) {
		NumberFormat form = new DecimalFormat("#,###,###,##0.00");
		return form.format(number);
	}



}
