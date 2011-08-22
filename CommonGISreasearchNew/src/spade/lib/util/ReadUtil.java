package spade.lib.util;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.StringTokenizer;
import java.util.Vector;

/**
* Helpful functions for reading descriptions and specifications containing
* names in multiple languages
*/

public class ReadUtil {

	public static Vector getLanguages(String str) {
		if (str == null)
			return null;
		if (!str.startsWith("*language") && !str.startsWith("*LANGUAGE"))
			return null;
		StringTokenizer st = new StringTokenizer(str, " /\r\n");
		st.nextToken();
		if (!st.hasMoreTokens())
			return null;
		Vector v = new Vector(st.countTokens());
		while (st.hasMoreTokens()) {
			v.addElement(st.nextToken().toUpperCase());
		}
		return v;
	}

	public static String getNthName(String str, int n) {
		StringTokenizer st = new StringTokenizer(str.trim(), "/\r\n");
		int max = st.countTokens();
		if (n >= max) {
			n = max - 1;
		}
		for (int i = 0; i < n; i++) {
			st.nextToken();
		}
		String p = st.nextToken().trim();
		if (p.startsWith("\"")) {
			p = p.substring(1);
		}
		if (p.endsWith("\"")) {
			p = p.substring(0, p.length() - 1);
		}
		return p;
	}

	/**
	* Reads parameters from a file with the following format of each line:
	* <param_name>=<param_value> or <param_name>="<param_value>".
	* Returns a vector each element of which is an array of two strings:
	* the first string is parameter name, the second is the value of this
	* parameter.
	*/
	public static Vector getParameters(BufferedReader reader) {
		if (reader == null)
			return null;
		Vector params = null;
		while (true) {
			try {
				String s = reader.readLine();
				//System.out.println("<"+s+">");
				if (s == null) {
					break;
				}
				s = s.trim();
				if (s.length() < 1) {
					continue;
				}
				StringTokenizer st = new StringTokenizer(s, " =\r\n");
				if (st.countTokens() < 2) {
					continue;
				}
				String key = st.nextToken(), val = st.nextToken("=\r\n");
				if (val == null) {
					continue;
				}
				val = StringUtil.removeQuotes(val);
				key = key.trim();
				val = val.trim();
				if (key.length() < 1 || val.length() < 1) {
					continue;
				}
				if (params == null) {
					params = new Vector(20, 10);
				}
				String pp[] = new String[2];
				pp[0] = key;
				pp[1] = val;
				params.addElement(pp);
			} catch (IOException ioe) {
				System.out.println("Exception reading parameters: " + ioe);
			}
		}
		return params;
	}

	/**
	* "Path" is an URL or a path in the local file system
	*/
	public static Vector getParameters(String path) {
		return getParameters(openStream(path));
	}

	public static Vector getParameters(URL url) {
		if (url == null)
			return null;
		try {
			InputStream stream = url.openStream();
			return getParameters(stream);
		} catch (IOException ioe) {
			System.out.println("Cannot open URL " + url.toString() + ": " + ioe);
		}
		return null;
	}

	public static Vector getParameters(InputStream stream) {
		if (stream == null)
			return null;
		BufferedReader br = new BufferedReader(new InputStreamReader(stream));
		if (br == null)
			return null;
		Vector result = getParameters(br);
		try {
			stream.close();
		} catch (IOException ioe) {
		}
		return result;
	}

	/**
	* Opens the stream on the specified data source. If the source
	* string starts with HTTP or FILE, accesses the source as a URL, otherwise
	* opens it as a local file
	*/
	public static InputStream openStream(String dataSource) {
		if (dataSource == null)
			return null;
		int idx = dataSource.indexOf(':');
		boolean isURL = false;
		if (idx > 0) {
			String pref = dataSource.substring(0, idx);
			if (pref.equalsIgnoreCase("HTTP") || pref.equalsIgnoreCase("FILE")) {
				isURL = true;
			}
		}
		try {
			if (isURL) { //try to access the source as a URL
				URL url = new URL(dataSource);
				return url.openStream();
			} else {
				File file = new File(dataSource);
				if (file.exists())
					return new FileInputStream(file);
				URL url = URLSupport.makeURLbyPath(null, dataSource);
				if (url == null) {
					System.out.println("Cannot find " + dataSource);
				} else
					return url.openStream();
			}
		} catch (IOException ioe) {
			System.out.println("Error accessing " + dataSource + ": " + ioe);
		}
		return null;
	}

	/**
	* Reads parameters from a file with the format of parameters as in an
	* an applet tag in the HTML format, that is
	* <PARAM name="name" value="value">
	*/
	public static Vector getAppletParameters(BufferedReader reader) {
		if (reader == null)
			return null;
		Vector params = null;
		while (true) {
			try {
				String s = reader.readLine();
				//System.out.println("<"+s+">");
				if (s == null) {
					break;
				}
				s = s.trim();
				if (s.length() < 1) {
					continue;
				}
				int idx = s.indexOf(' ');
				if (idx < 1) {
					continue;
				}
				String tok = s.substring(0, idx);
				if (!tok.equalsIgnoreCase("<PARAM")) {
					continue;
				}
				tok = s.substring(idx + 1).trim();
				String name = null, value = null;
				for (int i = 0; i < 2; i++) {
					idx = tok.indexOf('=');
					if (idx < 1) {
						break;
					}
					String key = tok.substring(0, idx).trim();
					if (key.equalsIgnoreCase("name") || key.equalsIgnoreCase("value")) {
						tok = tok.substring(idx + 1).trim();
						String val = null;
						if (tok.startsWith("\"")) {
							idx = tok.indexOf('\"', 1);
							if (idx > 0) {
								val = tok.substring(1, idx);
							} else {
								val = tok.substring(1);
							}
						} else {
							idx = tok.indexOf(' ');
							int idx1 = tok.indexOf('>');
							if (idx1 >= 0 && (idx < 0 || idx1 < idx)) {
								idx = idx1;
							}
							if (idx > 0) {
								val = tok.substring(0, idx);
							} else {
								val = tok;
							}
						}
						val = val.trim();
						if (key.equalsIgnoreCase("name")) {
							name = val;
						} else {
							value = val;
						}
					}
					tok = tok.substring(idx + 1).trim();
				}
				if (name != null) {
					if (params == null) {
						params = new Vector(20, 10);
					}
					String pp[] = new String[2];
					pp[0] = name;
					pp[1] = value;
					params.addElement(pp);
					//System.out.println(name+"="+value);
				}
			} catch (IOException ioe) {
				System.out.println("Exception reading parameters: " + ioe);
			}
		}
		return params;
	}

	/**
	* "Path" is an URL or a path in the local file system
	*/
	public static Vector getAppletParameters(String path) {
		return getAppletParameters(openStream(path));
	}

	public static Vector getAppletParameters(URL url) {
		if (url == null)
			return null;
		try {
			InputStream stream = url.openStream();
			return getAppletParameters(stream);
		} catch (IOException ioe) {
			System.out.println("Cannot open URL " + url.toString() + ": " + ioe);
		}
		return null;
	}

	public static Vector getAppletParameters(InputStream stream) {
		if (stream == null)
			return null;
		BufferedReader br = new BufferedReader(new InputStreamReader(stream));
		if (br == null)
			return null;
		Vector result = getAppletParameters(br);
		try {
			stream.close();
		} catch (IOException ioe) {
		}
		return result;
	}

	/**
	* Returns color corresponding to the string (e.g. "red", "darkred" etc.)
	*/
	public static Color getColor(String colorName) {
		if (colorName == null || colorName.length() < 1)
			return null;
		colorName = colorName.trim().toLowerCase();
		boolean dark = colorName.startsWith("dark"), bright = !dark && colorName.startsWith("bright");
		Color color = null;
		if (colorName.endsWith("white")) {
			color = Color.white;
		} else if (colorName.endsWith("black")) {
			color = Color.black;
		} else if (colorName.endsWith("blue")) {
			color = Color.blue;
		} else if (colorName.endsWith("cyan")) {
			color = Color.cyan;
		} else if (colorName.endsWith("gray")) {
			color = Color.gray;
		} else if (colorName.endsWith("green")) {
			color = Color.green;
		} else if (colorName.endsWith("magenta")) {
			color = Color.magenta;
		} else if (colorName.endsWith("orange")) {
			color = Color.orange;
		} else if (colorName.endsWith("pink")) {
			color = Color.pink;
		} else if (colorName.endsWith("red")) {
			color = Color.red;
		} else if (colorName.endsWith("yellow")) {
			color = Color.yellow;
		}
		if (color == null)
			return null;
		if (dark)
			return color.darker();
		if (bright)
			return color.brighter();
		return color;
	}
}
