package spade.lib.util;

// no texts

public class SplitString {
	String AString = null;
	int k = 0, len = 0;

	public SplitString(String str) {
		AString = new String(str);
		if (AString != null) {
			len = AString.length();
		}
	};

	public boolean HasValues() {
		if (AString == null)
			return false;
		char c;
		try {
			while (k < len && ((c = AString.charAt(k)) == ' ' || c == ',')) {
				++k;
			}
		} catch (StringIndexOutOfBoundsException e) {
			return false;
		}
		if (k >= len)
			return false;
		return true;
	};

	public String GetNextValue() {
		if (AString == null)
			return null;
		try {
			char c = 0;
			while (k < len && ((c = AString.charAt(k)) == ' ' || c == ',')) {
				++k;
			}
			if (k >= len)
				return null;
			int n;
			if (c == '\"') {
				++k;
				n = AString.indexOf('\"', k);
				if (n < 0) {
					n = len;
				}
			} else {
				n = AString.indexOf(' ', k);
				if (n < 0) {
					n = len;
				}
				int n1 = AString.indexOf(',', k);
				if (n1 >= 0 && n1 < n) {
					n = n1;
				}
			}
			String result = AString.substring(k, n);
			k = n + 1;
			return result;
		} catch (StringIndexOutOfBoundsException e) {
			return null;
		}
	};
};