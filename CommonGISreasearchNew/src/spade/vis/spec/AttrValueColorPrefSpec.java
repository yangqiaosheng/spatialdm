package spade.vis.spec;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.Vector;

import spade.lib.util.StringUtil;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 13-Feb-2007
 * Time: 16:45:59
 * Describes preferred colors assigned to values of a qualitative or integer
 * attribute.
 */
public class AttrValueColorPrefSpec implements TagReader, java.io.Serializable {
	/**
	 * The name of the attribute
	 */
	public String attrName = null;
	/**
	 * A set of pairs <value,color>. Each pair is an array consisting of 2 objects.
	 */
	public Vector colorPrefs = null;

	/**
	* Stores the description of the color preferences as a sequence of
	* lines starting with <AttrValueColorPreference> and ending with
	 * </AttrValueColorPreference>.
	*/
	@Override
	public void writeDescription(DataOutputStream writer) throws IOException {
		if (writer == null)
			return;
		if (attrName == null || colorPrefs == null || colorPrefs.size() < 1)
			return;
		writer.writeBytes("<AttrValueColorPreference>\n");
		writer.writeBytes("attribute_name=\"" + attrName + "\"\n");
		for (int i = 0; i < colorPrefs.size(); i++) {
			Object pair[] = (Object[]) colorPrefs.elementAt(i);
			if (pair == null || pair[0] == null || pair[1] == null) {
				continue;
			}
			if (!(pair[1] instanceof Color)) {
				continue;
			}
			Color color = (Color) pair[1];
			writer.writeBytes("<ValueColor>\n");
			writer.writeBytes("value=\"" + pair[0].toString() + "\"\n");
			writer.writeBytes("color=" + Integer.toHexString(color.getRGB()).substring(2) + "\n");
			writer.writeBytes("</ValueColor>\n");
		}
		writer.writeBytes("</AttrValueColorPreference>\n");
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
		if (str == null || br == null || !str.startsWith("<") || !str.substring(1).toLowerCase().startsWith("attrvaluecolorpreference"))
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
			if (str.equalsIgnoreCase("<valuecolor>")) {
				Object pair[] = readValueColor(str, br);
				if (pair != null) {
					if (colorPrefs == null) {
						colorPrefs = new Vector(20, 10);
					}
					colorPrefs.addElement(pair);
				}
				continue;
			}
			if (str.equalsIgnoreCase("</attrvaluecolorpreference>")) {
				endFound = true;
				break;
			}
			StringTokenizer st = new StringTokenizer(str, "=");
			if (st.countTokens() < 2) {
				continue;
			}
			String w1 = StringUtil.removeQuotes(st.nextToken().trim()), w2 = StringUtil.removeQuotes(st.nextToken().trim());
			if (w1 == null || w2 == null || w1.length() < 1 || w2.length() < 1) {
				continue;
			}
			if (w1.equalsIgnoreCase("attribute_name")) {
				attrName = w2;
			}
		}
		if (!endFound)
			return false;
		return true;
	}

	/**
	 * Reads a specification of a pair <value,color>, which starts from the tag
	 * <ValueColor> and ends with </ValueColor>
	 */
	protected Object[] readValueColor(String str, BufferedReader br) throws IOException {
		if (str == null || br == null || !str.startsWith("<") || !str.substring(1).toLowerCase().startsWith("valuecolor"))
			return null;
		boolean endFound = false;
		Object result[] = null;
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
			if (str.equalsIgnoreCase("</valuecolor>")) {
				endFound = true;
				break;
			}
			StringTokenizer st = new StringTokenizer(str, "=");
			if (st.countTokens() < 2) {
				continue;
			}
			String w1 = StringUtil.removeQuotes(st.nextToken().trim()), w2 = StringUtil.removeQuotes(st.nextToken().trim());
			if (w1 == null || w2 == null || w1.length() < 1 || w2.length() < 1) {
				continue;
			}
			if (w1.equalsIgnoreCase("value") || w1.equalsIgnoreCase("color")) {
				if (result == null) {
					result = new Object[2];
					result[0] = null;
					result[1] = null;
				}
				if (w1.equalsIgnoreCase("value")) {
					result[0] = w2;
				} else {
					result[1] = new Color(Integer.parseInt(w2, 16));
				}
			}
		}
		if (!endFound)
			return null;
		if (result == null || result[0] == null || result[1] == null)
			return null;
		return result;
	}
}
