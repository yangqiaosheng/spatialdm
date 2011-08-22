package configstart;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

/**
* Saves and restores properties from/to a hashtable in an ASCII file.
* The format in the file is:
* <properties>
* name1=value1
* name2=value2
* ...
* </properties>
*/
public class PropertySaver {
	/**
	* Reads a list of arbitrary properties from the specified source until the
	* line </properties> occurs. Each property must come in a separate line in
	* the format property_name=value.
	*/
	public static Hashtable readProperties(BufferedReader br) throws IOException {
		if (br == null)
			return null;
		Hashtable prop = new Hashtable();
		while (true) {
			String str = br.readLine();
			if (str == null) {
				break;
			}
			str = str.trim();
			if (str.length() < 1) {
				continue;
			}
			if (str.equalsIgnoreCase("</properties>")) {
				break;
			}
			int idx = str.indexOf('=');
			if (idx <= 0 || idx >= (str.length() - 1)) {
				continue;
			}
			prop.put(str.substring(0, idx).trim(), str.substring(idx + 1).trim());
		}
		if (prop.size() < 1)
			return null;
		return prop;
	}

	/**
	* Stores the given properties in the specified stream in the format
	* <properties>
	* name1=value1
	* name2=value2
	* ...
	* </properties>
	*/
	public static void writeProperties(Hashtable properties, DataOutputStream writer) throws IOException {
		if (properties != null) {
			writer.writeBytes("<properties>\n");
			for (Enumeration keys = properties.keys(); keys.hasMoreElements();) {
				String key = (String) keys.nextElement();
				writer.writeBytes(key + "=" + String.valueOf(properties.get(key)) + "\n");
			}
			writer.writeBytes("</properties>\n");
		}
	}
}