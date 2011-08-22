package spade.vis.spec;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;

/**
* This class is able to return for a given keyword a TagReader able to read
* a sequence of lines starting with <keyword ...> and ending with </keyword>
* and get some description from it. The class contains a list of known
* keywords and the corresponding classes.
*/
public class TagReaderFactory {
	/**
	* The list of known keywords and the corresponding classes. The classes must
	* implement the interface TagReader.
	*/
	protected static String tagReaders[][] = { { "TimeReference", "spade.time.TimeRefDescription" }, { "CaptionParameter", "spade.vis.spec.CaptionParamDescription" }, { "Parameter", "spade.vis.spec.ParamDescription" },
			{ "LinkData", "spade.vis.spec.LinkDataDescription" }, { "AttrValueColorPreference", "spade.vis.spec.AttrValueColorPrefSpec" }, { "AttrValueOrderPreference", "spade.vis.spec.AttrValueOrderPrefSpec" } };

	/**
	* Returns a tag reader able to read a tag starting with the given keyword.
	* May return null if there is no apropriate reader.
	*/
	public static TagReader getTagReader(String keyword) {
		if (keyword == null)
			return null;
		for (String[] tagReader : tagReaders)
			if (tagReader[0].equalsIgnoreCase(keyword)) {
				try {
					TagReader reader = (TagReader) Class.forName(tagReader[1]).newInstance();
					return reader;
				} catch (Exception e) {
					return null;
				}
			}
		return null;
	}

	/**
	* Skips a "tag", i.e. a sequence of lines starting with <keyword ...> and
	* ending with </keyword>, if no appropriate reader for this tag exists.
	* The first argument of the method is the first string of the tag (i.e. any
	* string starting with "<"), the second argument is the data source to read.
	*/
	public static void skipTag(String header, BufferedReader br) throws IOException {
		if (header == null || br == null || !header.startsWith("<"))
			return;
		String str = header.substring(1).toLowerCase();
		int k = str.indexOf('>');
		if (k > 0) {
			str = str.substring(0, k).trim();
		}
		k = str.indexOf(' ');
		if (k < 0) {
			k = str.length();
		}
		String key = str.substring(0, k);
		//search for the end tag
		boolean end = false;
		do {
			try {
				str = br.readLine();
			} catch (EOFException eof) {
				break;
			}
			if (str != null) {
				str = str.trim();
				if (str.length() < 1) {
					continue;
				}
				if (str.startsWith("</")) {//this the end tag
					str = str.toLowerCase();
					end = str.startsWith("</" + key);
				}
			}
		} while (!end && str != null);
	}
}