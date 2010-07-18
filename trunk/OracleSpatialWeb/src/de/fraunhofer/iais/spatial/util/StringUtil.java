package de.fraunhofer.iais.spatial.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class StringUtil {

	/**
	 * change the format of the month
	 * eg. "January" --> "01"
	 * @param s - String
	 * @return String
	 */
	public static String FullMonth2Num(String s) {
		s = s.replaceAll("January", "01").replaceAll("February", "02").replaceAll("March", "03").replaceAll("April", "04").replaceAll("May", "05").replaceAll("June", "06").replaceAll("July", "07").replaceAll("August", "08").replaceAll("September",
				"09").replaceAll("October", "10").replaceAll("November", "11").replaceAll("December", "12");
		return s;
	}

	/**
	 * change the format of the month
	 * eg. "Jan." --> "01"
	 * @param s - String
	 * @return String
	 */
	public static String ShortMonth2Num(String s) {
		s = s.replaceAll("Jan.", "01").replaceAll("Feb.", "02").replaceAll("Mar.", "03").replaceAll("Apr.", "04").replaceAll("May.", "05").replaceAll("Jun.", "06").replaceAll("Jul.", "07").replaceAll("Aug.", "08").replaceAll("Sep.", "09")
				.replaceAll("Oct.", "10").replaceAll("Nov.", "11").replaceAll("Dec.", "12");
		return s;
	}

	/**
	 * change the format of the day for the xml request
	 * eg. "1" --> "01"
	 * @param s - String
	 * @return String
	 */
	public static String ShortNum2Long(String s) {
		s = s.replaceAll("<day>1</day>", "<day>01</day>").replaceAll("<day>2</day>", "<day>02</day>").replaceAll("<day>3</day>", "<day>03</day>").replaceAll("<day>4</day>", "<day>04</day>").replaceAll("<day>5</day>", "<day>05</day>").replaceAll(
				"<day>6</day>", "<day>06</day>").replaceAll("<day>7</day>", "<day>07</day>").replaceAll("<day>8</day>", "<day>08</day>").replaceAll("<day>9</day>", "<day>09</day>").replaceAll("<hour>0</hour>", "<hour>00</hour>").replaceAll(
				"<hour>1</hour>", "<hour>01</hour>").replaceAll("<hour>2</hour>", "<hour>02</hour>").replaceAll("<hour>3</hour>", "<hour>03</hour>").replaceAll("<hour>4</hour>", "<hour>04</hour>").replaceAll("<hour>5</hour>", "<hour>05</hour>")
				.replaceAll("<hour>6</hour>", "<hour>06</hour>").replaceAll("<hour>7</hour>", "<hour>07</hour>").replaceAll("<hour>8</hour>", "<hour>08</hour>").replaceAll("<hour>9</hour>", "<hour>09</hour>");
		return s;
	}

	/**
	 * change the format of the weekday for the xml request
	 * eg. "Monday" --> "Mon"
	 * @param s - String
	 * @return String
	 */
	public static String LongWeekday2Short(String s) {
		s = s.replaceAll("<weekday>Monday</weekday>", "<weekday>Mon</weekday>").replaceAll("<weekday>Tuesday</weekday>", "<weekday>Tue</weekday>").replaceAll("<weekday>Wednesday</weekday>", "<weekday>Wed</weekday>").replaceAll(
				"<weekday>Thursday</weekday>", "<weekday>Thu</weekday>").replaceAll("<weekday>Friday</weekday>", "<weekday>Fri</weekday>").replaceAll("<weekday>Saturday</weekday>", "<weekday>Sat</weekday>").replaceAll("<weekday>Sunday</weekday>",
				"<weekday>Sun</weekday>");
		return s;
	}

	/**
	 * replace all the html tag
	 * @param s - String
	 * @return String
	 */
	public static String escapeHtml(String s) {
		s = s.replaceAll("<", "&lt").replaceAll(">", "&gt");
		return s;
	}

	/**
	 * generate a random file name 
	 * format: yyMMddHHmmss-UUID(first 8 bit)
	 * @param d - current Date
	 * @return random file name
	 */
	public static String genFilename(Date d) {
		String dateStr = new SimpleDateFormat("yyMMddHHmmss-").format(d);
		return dateStr + UUID.randomUUID().toString().substring(0, 8);
	}

	/**
	 * The HMAC algorithm:
	 * function hmac (key, message)
	 *	    if (length(key) > blocksize) then
	 *	        key = hash(key) // keys longer than blocksize are shortened
	 *	    end if
	 *	    if (length(key) < blocksize) then
	 *	        key = key + zeroes(blocksize - length(key)) // keys shorter than blocksize are zero-padded
	 *	    end if
	 *	    
	 *	    o_key_pad = [0x5c * blocksize] ^ key // Where blocksize is that of the underlying hash function
	 *	    i_key_pad = [0x36 * blocksize] ^ key // Where ^ is exclusive or (XOR)
	 *	    
	 *	    return hash(o_key_pad + hash(i_key_pad + message)) // Where + is concatenation
	 *	end function 
	 * @param message - input byte array
	 * @param keyString - key
	 * @return hmac byte array
	 */
	private static String hmac(byte[] message, String keyString) {

		//Hash Function: MD5/SHA/SHA512
		String hashFunction = "MD5";

		// Block Size for MD5 and SHA-1
		int blockSize = 512;

		// pad the key with zero to reach the blockSize
		byte[] key = new byte[blockSize / 8];
		for (int i = 0; i < blockSize / 8; i++) {
			if (i < keyString.length()) {
				key[i] = keyString.getBytes()[i];
			} else {
				key[i] = 0x00;
			}
		}

		// 512bit: opad=0x5C and ipad=0x36
		byte[] opad = new byte[blockSize / 8];
		byte[] ipad = new byte[blockSize / 8];
		for (int i = 0; i < blockSize / 8; i++) {
			opad[i] = 0x5C;
			ipad[i] = 0x36;
		}

		// o_key_pad = [0x5c * blocksize] ^ key
		// i_key_pad = [0x36 * blocksize] ^ key
		byte[] okeypad = new byte[blockSize / 8];
		byte[] ikeypad = new byte[blockSize / 8];
		for (int i = 0; i < blockSize / 8; i++) {
			okeypad[i] = (byte) (opad[i] ^ key[i]);
			ikeypad[i] = (byte) (ipad[i] ^ key[i]);
		}

		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance(hashFunction);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}

		// ikeypad_message = i_key_pad + message
		byte[] ikeypad_message = new byte[ikeypad.length + message.length];
		System.arraycopy(ikeypad, 0, ikeypad_message, 0, ikeypad.length);
		System.arraycopy(message, 0, ikeypad_message, ikeypad.length, message.length);

		// ihash = hash(i_key_pad + message)
		md.update(ikeypad_message);
		byte[] ihash = md.digest();

		// okeypad_message = o_key_pad + hash(i_key_pad + message)
		byte[] okeypad_message = new byte[okeypad.length + ihash.length];
		System.arraycopy(okeypad, 0, okeypad_message, 0, okeypad.length);
		System.arraycopy(ihash, 0, okeypad_message, okeypad.length, ihash.length);

		// ohash = hash(o_key_pad + hash(i_key_pad + message))
		md.update(okeypad_message);
		byte[] ohash = md.digest();
		String hmac = byteArrayToHexString(ohash);
		return hmac;
	}

	/**
	 * convert the byte array to the Hex String
	 * @param bytes
	 * @return Hex String
	 */
	public static String byteArrayToHexString(byte[] bytes) {
		StringBuffer sb = new StringBuffer(bytes.length * 2);
		for (byte b : bytes) {
			int v = b & 0xff;
			if (v < 16) {
				sb.append('0');
			}
			sb.append(Integer.toHexString(v));
		}
		return sb.toString().toUpperCase();
	}

	/**
	 * convert the one byte to the 2 bit Hex String
	 * @param byte
	 * @return Hex String
	 */
	public static String byteToHexString(byte b) {
		StringBuffer sb = new StringBuffer(2);
		int v = b & 0xff;
		if (v < 16) {
			sb.append('0');
		}
		sb.append(Integer.toHexString(v));
		return sb.toString().toUpperCase();
	}
}
