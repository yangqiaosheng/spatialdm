package spade.lib.util;

import java.util.StringTokenizer;

public class IdMaker {
	static int last = 0;

	public static String makeId() {
		return "attr" + (++last);
	}

	public static String makeId(String name, IdentifierUseChecker checkRoot) {
		if (name == null)
			return makeId();
		//ensure that all characters in the identifier are Latin
		for (int i = 0; i < name.length(); i++) {
			char ch = name.charAt(i);
			if (ch < ' ') {
				name = name.replace(ch, ' ');
			} else if (ch > (char) 127) {
				name = name.replace(ch, (char) (ch % ('z' - ' ') + ' '));
			}
		}
		StringTokenizer st = new StringTokenizer(name, " .,:;()[]{}<>$%|\'\"/\\!?=+-*\r\n");
		if (!st.hasMoreTokens())
			return makeId();
		String id = st.nextToken().toLowerCase();
		if (st.hasMoreTokens()) {
			if (id.length() > 3) {
				id = id.substring(0, 3);
			}
			while (id.length() < 11 && st.hasMoreTokens()) {
				String tok = st.nextToken().toLowerCase();
				if (tok.length() > 3) {
					tok = tok.substring(0, 3);
				}
				id += "_" + tok;
			}
		}
		if (id.length() > 11) {
			id = id.substring(0, 11);
		}
		int k = 0;
		String idcore = id;
		while (checkRoot.isIdentifierUsed(id)) {
			String s = "_" + String.valueOf(++k);
			if (idcore.length() + s.length() > 11) {
				idcore = idcore.substring(0, 11 - s.length());
			}
			id = idcore + s;
		}
		//System.out.println(id);
		return id;
	}
}