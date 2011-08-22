package spade.analysis.geocomp.mutil;

public class UChar {

	public static String pad(String s, int l) {
		StringBuffer sb = new StringBuffer(s);
		while (sb.length() < l) {
			sb.append(' ');
		}
		return sb.toString();
	}

	public static int strpos2(String s, String si, int i) {
		int p;

		p = s.indexOf(si);

		if ((p < 0) || (si.length() == 0))
			return 0;

		return p + i;
	}

	public static String strdelete(String s, int p, int n) {
		int l = s.length();

		p--;

		if ((p >= l) || (n == 0))
			return s;

		if ((p + n) >= l)
			return s.substring(0, p);

		return s.substring(0, p) + s.substring(p + n, l);
	}

	public static String strsub(String s, int p, int n) {
		int l = s.length();

		p--;

		if ((p >= l) || (n == 0))
			return "";

		int k = p + n;
		if (k > l) {
			k = l;
		}

		return s.substring(p, k);
	}

	public static String strinsert(String si, String s, int p) {
		int l, i;

		l = s.length();
		i = si.length();

		p--;

		if ((p > l) || (i == 0))
			return s;

		return s.substring(0, p) + si + s.substring(p, l);
	}

	public static String charinsert(char si, String s, int p) {
		int l, i;

		l = s.length();
		i = 1;

		p--;

		if ((p > l) || (i == 0))
			return s;

		return s.substring(0, p) + si + s.substring(p, l);
	}

}