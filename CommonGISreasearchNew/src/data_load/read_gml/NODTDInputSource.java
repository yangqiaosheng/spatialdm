package data_load.read_gml;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class NODTDInputSource extends InputSource {

	private static final String XMLDECL = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>";

	static class Reader extends java.io.InputStreamReader {
		private int count = 0, length = 0;
		private StringReader sr;

		Reader(InputStream in, String header) {
			super(in);
			sr = new StringReader(header);
			length = header.length();
		}

		Reader(InputStream in, String header, String enc) throws java.io.UnsupportedEncodingException {
			super(in, enc);
			sr = new StringReader(header);
			length = header.length();
		}

		@Override
		public int read() throws IOException {
			if (sr != null && count < length) {
				++count;
				return sr.read();
			}
			return super.read();
		}

		@Override
		public int read(char buf[], int off, int len) throws IOException {
			if (sr == null || count == length)
				return super.read(buf, off, len);
			return count = sr.read(buf, off, length);
		}
	}

	private StringBuffer sb = new StringBuffer();
	private String path;
	private boolean init = true;
	private boolean opened = false;

	public NODTDInputSource(String id) {
		this(id, true);
	}

	public NODTDInputSource(String id, boolean init) {
		this.init = init;
		setSystemId(id);
	}

	@Override
	public void setSystemId(String systemId) {
		URL url;
		try {
			url = new URL(systemId);
		} catch (java.net.MalformedURLException e0) { /* suppose it's a file name */
			try {
				url = new URL("file:" + reslash(new java.io.File(systemId).getAbsolutePath()));
			} catch (java.net.MalformedURLException e1) {
				/* cannot normally happen */
				throw new RuntimeException(e1.toString());
			}
		}
		super.setSystemId(url.toString());
	}

	@Override
	public java.io.Reader getCharacterStream() {
		if (!init)
			return null;
		if (!opened) {
			try {
				if (super.getCharacterStream() == null) {
					if (getByteStream() != null) {
						init(getByteStream());
					} else {
						String s = getSystemId();
						if (s == null) {
							s = getPublicId();
						}
						init(new URL(s).openStream());
					}
				}
				opened = true;
			} catch (Exception e) {
				throw new RuntimeException(e.toString());
			}
		}
		return super.getCharacterStream();
	}

	private char read(java.io.DataInputStream in) throws IOException {
		char c = (char) in.read();
		sb.append(c);
		return c;
	}

	private void init(InputStream is) throws IOException, SAXException {
		int xmlDecl = -1;
		char vsc;
		boolean encDecl = false;
		String encName = null;
		sb.setLength(0);
		java.io.DataInputStream in = new java.io.DataInputStream(is);
		for (;;) {
			while (read(in) != '<') {
				;
			}
			switch (read(in)) {
			case '?':
				int i = sb.length();
				while (read(in) != '>') {
					;
				}
				xmlDecl = sb.length();
				char ch[] = new char[sb.length() - i - 1];
				sb.getChars(i, sb.length() - 1, ch, 0);
				String xmldecl = new String(ch);
				int index = xmldecl.indexOf("encoding");
				if (index != -1) {
					encDecl = true;
					index += 8;
					while ((xmldecl.charAt(index) != '"') && (xmldecl.charAt(index) != '\'')) {
						index++;
					}
					vsc = xmldecl.charAt(index++);
					int beg = index;
					while ((xmldecl.charAt(index++) != vsc)) {
						;
					}
					int end = --index;
					encName = xmldecl.substring(beg, end);
				}
				continue;
			case '!':
				i = sb.length();
				if (read(in) == '-') {
					while (read(in) != '>') {
						;
					}
					continue;
				}
				while (read(in) != ' ') {
					;
				}
				ch = new char[sb.length() - i - 1];
				sb.getChars(i, sb.length() - 1, ch, 0);
				if ((new String(ch)).equals("DOCTYPE")) {
					while (read(in) != '>') {
						;
					}
					sb.setLength(i - 2);
				} //else fatal error
				continue;
			default:
				if (xmlDecl == -1) {
					sb.insert(0, XMLDECL);
					xmlDecl = XMLDECL.length();
				} else if (!encDecl) {
					sb = new StringBuffer(sb.toString().substring(xmlDecl + 1, sb.length()));
					sb.insert(0, XMLDECL);
					xmlDecl = XMLDECL.length();
				}
				setCharacterStream(new Reader(in, sb.toString(), EncodingName.toJava(encName)));
				return;
			}
		}
	}

	private static String reslash(String s) {
		String t = "";
		String slash = System.getProperty("file.separator");
		int i = 0, j;
		for (;;) {
			j = s.indexOf(slash, i);
			if (j == -1) {
				j = s.length();
			}
			if (i != j) {
				t = t + s.substring(i, j);
			}
			if (j != s.length()) {
				t = t + "/";
				i = j + slash.length();
			} else {
				break;
			}
		}
		return t;
	}
}
