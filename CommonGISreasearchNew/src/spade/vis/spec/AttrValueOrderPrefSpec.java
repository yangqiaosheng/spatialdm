package spade.vis.spec;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;

import spade.lib.util.StringUtil;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 12-Mar-2007
 * Time: 12:59:37
 * Specifies a preferred order of attribute values, for a qualitative attribute
 */
public class AttrValueOrderPrefSpec implements TagReader, java.io.Serializable {
	/**
	 * The name of the attribute
	 */
	public String attrName = null;
	/**
	 * The ordered list of values (strings)
	 */
	public Vector values = null;

	/**
	* Stores the description of the value order preference as a sequence of
	* lines starting with <AttrValueOrderPreference> and ending with
	 * </AttrValueOrderPreference>.
	*/
	@Override
	public void writeDescription(DataOutputStream writer) throws IOException {
		if (writer == null)
			return;
		if (attrName == null || values == null || values.size() < 1)
			return;
		writer.writeBytes("<AttrValueOrderPreference>\n");
		writer.writeBytes("attribute_name=\"" + attrName + "\"\n");
		for (int i = 0; i < values.size(); i++) {
			writer.writeBytes((String) values.elementAt(i) + "\n");
		}
		writer.writeBytes("</AttrValueOrderPreference>\n");
	}

	/**
	 * Reads a description of color preferences. A description must be a
	 * sequence of lines starting with <AttrValueColorPreference> and ending with
	 * </AttrValueColorPreference>. The first argument is the first string of the
	 * tag (i.e. the string starting with "<AttrValueColorPreference"), the
	 * second argument is the BufferedReader used to read the description.
	 * Returns true if the description has been successfully read,
	 * or at least the end line has been encountered.
	 */
	@Override
	public boolean readDescription(String str, BufferedReader br) throws IOException {
		if (str == null || br == null || !str.startsWith("<") || !str.substring(1).toLowerCase().startsWith("attrvalueorderpreference"))
			return false;
		boolean endFound = false;
		while (true) {
			str = br.readLine();
			if (str == null) {
				break;
			}
			str = str.trim();
			if (str.length() < 1) {
				continue;
			}
			if (str.startsWith("*")) {
				continue; //this is a comment
			}
			if (str.equalsIgnoreCase("</attrvalueorderpreference>")) {
				endFound = true;
				break;
			}
			if (str.toLowerCase().startsWith("attribute_name=")) {
				attrName = StringUtil.removeQuotes(str.substring(15).trim());
				continue;
			}
			if (values == null) {
				values = new Vector(10, 10);
			}
			values.addElement(StringUtil.removeQuotes(str));
		}
		if (!endFound)
			return false;
		return true;
	}
}
