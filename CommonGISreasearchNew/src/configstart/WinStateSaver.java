package configstart;

import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.StringTokenizer;

import spade.lib.util.StringUtil;
import spade.vis.spec.SpecSaver;
import spade.vis.spec.WinSpec;

/**
* Stores and reads information about properties of a window such as its title,
* size, location, as well as possible individual properties.
*/
public class WinStateSaver implements SpecSaver {
	/**
	* Constructs an instance of a tool specification class (spade.vis.spec.WinSpec).
	* May be overridden for constructing decsendants of spade.vis.spec.WinSpec.
	*/
	protected WinSpec constructSpecInstance() {
		return new WinSpec();
	}

	/**
	* Reads a single specification, i.e. a sequence of lines starting
	* with <tagName> and ending with </tagName>. The first argument is
	* the tag name (e.g. "tool"), the second argument is the BufferedReader used
	* to read the data. Returns the specification read.
	*/
	@Override
	public Object readSpecification(String tagName, BufferedReader br) throws IOException {
		if (tagName == null || br == null)
			return null;
		WinSpec wsp = constructSpecInstance();
		wsp.tagName = tagName.toLowerCase();
		while (true) {
			String str = br.readLine();
			if (str == null) {
				break;
			}
			str = str.trim();
			if (str.length() < 1) {
				continue;
			}
			if (str.startsWith("</") && str.endsWith(">")) {
				str = str.substring(2, str.length() - 1);
				if (str.equalsIgnoreCase(wsp.tagName)) {
					break;
				}
			} else if (str.startsWith("<")) {
				analyseTaggedFragment(str, br, wsp);
			} else {
				int idx = str.indexOf("=");
				if (idx > 0 && idx < str.length() - 1) {
					String key = str.substring(0, idx).trim().toLowerCase(), val = StringUtil.removeQuotes(str.substring(idx + 1).trim());
					if (key.length() > 0 && val.length() > 0) {
						analyseKeyAndValue(key, val, wsp);
					}
				}
			}
		}
		return wsp;
	}

	/**
	* Analyses a specification fragment starting with a tag. A signal of a tag is
	* the symbol "<". If the tag was recognized and read successfully, stores the
	* results of the analysis in the tool specification passed as the third
	* argument and returns.
	*/
	protected void analyseTaggedFragment(String line, BufferedReader br, WinSpec wsp) throws IOException {
		if (line.equalsIgnoreCase("<properties>")) {
			wsp.properties = PropertySaver.readProperties(br);
		}
	}

	/**
	* Analyses the given pair consisting of a keyword and a value and sets the
	* corresponding field in the given ToolSpec.
	*/
	protected void analyseKeyAndValue(String key, String val, WinSpec wsp) {
		if (key.equals("title")) {
			wsp.title = val;
		} else if (key.equals("bounds")) {
			StringTokenizer stt = new StringTokenizer(val, " (,);");
			int bou[] = new int[4];
			boolean gotAll = false;
			for (int i = 0; i < 4 && stt.hasMoreTokens(); i++) {
				try {
					bou[i] = Integer.valueOf(stt.nextToken()).intValue();
					if (i == 3) {
						gotAll = true;
					}
				} catch (NumberFormatException ne) {
					break;
				}
			}
			if (gotAll) {
				wsp.bounds = new Rectangle(bou[0], bou[1], bou[2], bou[3]);
			}
		}
	}

	/**
	* Stores the given specification to a file as a sequence of
	* lines starting with <tagName> and ending with </tagName>.
	*/
	@Override
	public void writeSpecification(Object spec, DataOutputStream writer) throws IOException {
		if (spec == null || writer == null)
			return;
		if (!(spec instanceof WinSpec))
			return;
		WinSpec wsp = (WinSpec) spec;
		if (wsp.tagName == null)
			return;
		writer.writeBytes("<" + wsp.tagName + ">\n");
		if (wsp.title != null) {
			writer.writeBytes("title=" + wsp.title + "\n");
		}
		if (wsp.bounds != null) {
			writer.writeBytes("bounds=(" + wsp.bounds.x + "," + wsp.bounds.y + "," + wsp.bounds.width + "," + wsp.bounds.height + ")\n");
		}
		writeSpecificSpecPart(wsp, writer);
		if (wsp.properties != null) {
			PropertySaver.writeProperties(wsp.properties, writer);
		}
		writer.writeBytes("</" + wsp.tagName + ">\n");
	}

	/**
	* Reserved for overriding in descendants. May store a fragment of the
	* specification pertinent to a descendant. Is called from the method
	* writeSpecification(...), which stores the part of the specification
	* common for all tools.
	*/
	protected void writeSpecificSpecPart(WinSpec spec, DataOutputStream writer) throws IOException {
	}
}