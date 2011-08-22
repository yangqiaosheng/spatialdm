package spade.lib.util;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Mar 10, 2009
 * Time: 11:53:30 AM
 */
public class XMLUtil {
	/**
	 * If the given string contains characters with codes >127, replaces those
	 * characters with sequences &#code; E.g. � will be represented as &#228;
	 */
	public static String encodeSpecChars(String str) {
		if (str == null || str.length() < 1)
			return str;
		StringBuffer sb = new StringBuffer(6 * str.length());
		for (int i = 0; i < str.length(); i++) {
			char ch = str.charAt(i);
			if (ch < 128) {
				sb.append(ch);
			} else {
				sb.append("&#" + ((int) ch) + ";");
			}
		}
		return sb.toString();
	}

	/**
	 * Encodes the ampersand character by "&#38;".
	 * Use with caution: do not aply to strings containing entity references!
	 */
	public static String encodeAmpersand(String str) {
		if (str == null || str.length() < 1)
			return str;
		int k = str.indexOf('&');
		if (k < 0)
			return str;
		StringBuffer sb = new StringBuffer(5 * str.length());
		while (k >= 0) {
			if (k > 0) {
				sb.append(str, 0, k);
			}
			sb.append("&#38;");
			str = str.substring(k + 1);
			k = str.indexOf('&');
		}
		sb.append(str);
		return sb.toString();
	}

	/**
	 * Replaces the ampersand character by "and".
	 * Use with caution: do not aply to strings containing entity references!
	 */
	public static String replaceAmpersand(String str) {
		if (str == null || str.length() < 1)
			return str;
		int k = str.indexOf('&');
		if (k < 0)
			return str;
		StringBuffer sb = new StringBuffer(5 * str.length());
		while (k >= 0) {
			if (k > 0) {
				sb.append(str, 0, k);
			}
			sb.append(" and ");
			str = str.substring(k + 1);
			k = str.indexOf('&');
		}
		sb.append(str);
		return sb.toString();
	}
}
