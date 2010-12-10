/*
 * Copyright (c) 2005 Aetrion LLC.
 */

package com.aetrion.flickr.util;

/**
 * Byte utilities.
 *
 * @author Anthony Eden
 */
public class ByteUtilities {

	static char[] hexChar = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

	/**
	 * Convert a byte array to a hex string.
	 *
	 * @param b The byte array
	 * @return The hex String
	 */
	public static String toHexString(byte[] b) {
		StringBuffer sb = new StringBuffer(b.length * 2);
		for (byte element : b) {
			// look up high nibble char
			sb.append(hexChar[(element & 0xf0) >>> 4]);

			// look up low nibble char
			sb.append(hexChar[element & 0x0f]);
		}
		return sb.toString();
	}

}
