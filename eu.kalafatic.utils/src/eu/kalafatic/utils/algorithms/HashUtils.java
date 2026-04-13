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
package eu.kalafatic.utils.algorithms;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import eu.kalafatic.utils.lib.EEncoding;
import eu.kalafatic.utils.lib.EHash;
import eu.kalafatic.utils.log.Log;
import eu.kalafatic.utils.preferences.ECorePreferences;

/**
 * The Class class HashUtils.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class HashUtils {

	/** The INSTANCE. */
	private volatile static HashUtils INSTANCE;

	/**
	 * Gets the single instance of HashUtils.
	 * @return single instance of HashUtils
	 */
	public static HashUtils getInstance() {
		if (INSTANCE == null) {
			synchronized (HashUtils.class) {
				INSTANCE = new HashUtils();
			}
		}
		return INSTANCE;
	}

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/**
	 * Gets the hash.
	 * @param input the input
	 * @param algorithm the algorithm
	 * @return the hash
	 */
	public byte[] getHash(byte[] input, EHash algorithm) {
		MessageDigest md;
		byte[] sha1Hash = null;
		try {
			md = MessageDigest.getInstance(algorithm.getLiteral());
			sha1Hash = new byte[20];

			md.reset();
			md.update(input, 0, input.length);
			sha1Hash = md.digest();

		} catch (NoSuchAlgorithmException e) {
			Log.log(ECorePreferences.MODULE, e);
		}
		return sha1Hash;
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the hash.
	 * @param input the input
	 * @param algorithm the algorithm
	 * @param salt the salt
	 * @return the hash
	 */
	public byte[] getHash(byte[] input, EHash algorithm, String salt) {
		MessageDigest md;
		byte[] sha1Hash = null;
		try {
			md = MessageDigest.getInstance(algorithm.getLiteral());
			sha1Hash = new byte[20];

			md.reset();
			md.update(input, 0, input.length);
			sha1Hash = md.digest();

		} catch (NoSuchAlgorithmException e) {
			Log.log(ECorePreferences.MODULE, e);
		}
		return sha1Hash;
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the hash.
	 * @param type the type
	 * @param text the text
	 * @param salt the salt
	 * @return the hash
	 * @throws NoSuchAlgorithmException the no such algorithm exception
	 * @throws UnsupportedEncodingException the unsupported encoding exception
	 */
	public String getHash(String type, String text, String salt) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		MessageDigest md;
		md = MessageDigest.getInstance(type);
		byte[] hash = new byte[md.getDigestLength()];
		if (!salt.isEmpty()) {
			md.update(salt.getBytes(EEncoding.ISO_1.getLiteral()), 0, salt.length());
		}
		md.update(text.getBytes(EEncoding.ISO_1.getLiteral()), 0, text.length());
		hash = md.digest();
		return convertToHex(hash);
	}

	// ---------------------------------------------------------------

	/**
	 * Convert to hex.
	 * @param data the data
	 * @return the string
	 */
	private String convertToHex(byte[] data) {
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < data.length; i++) {
			int halfbyte = (data[i] >>> 4) & 0x0F;
			int two_halfs = 0;
			do {
				if ((0 <= halfbyte) && (halfbyte <= 9)) {
					buf.append((char) ('0' + halfbyte));
				} else {
					buf.append((char) ('a' + (halfbyte - 10)));
				}
				halfbyte = data[i] & 0x0F;
			} while (two_halfs++ < 1);
		}
		return buf.toString();
	}

	public boolean checkBlockHash(byte[] payload, byte[] hash) {
		try {
			MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
			messageDigest.update(payload);
			return messageDigest.digest().equals(hash);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean[] checkFileSum(String filePath, byte[] hashes, int blockSize) throws IOException {
		FileChannel fileChannel = null;
		try {
			boolean[] result = new boolean[hashes.length / 20];
			fileChannel = new FileInputStream(filePath).getChannel();
			ByteBuffer buffer = ByteBuffer.allocate(blockSize);

			for (int i = 0; i < result.length; i++) {
				fileChannel.read(buffer);
				buffer.flip();

				result[i] = checkBlockHash(buffer.array(), Arrays.copyOfRange(hashes, i * 20, (i + 1) * 20));

				buffer.clear();
			}
		} finally {
			if (fileChannel != null) {
				fileChannel.close();
			}
		}
		return null;
	}
}
