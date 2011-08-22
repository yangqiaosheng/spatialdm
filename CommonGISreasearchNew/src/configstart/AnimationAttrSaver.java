package configstart;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;

import spade.lib.util.IdUtil;
import spade.lib.util.StringUtil;
import spade.vis.spec.AnimationAttrSpec;
import spade.vis.spec.SpecSaver;

/**
* Reads a specification of an attribute involved in an animated visualization
* from a file or writes an attribute specification to a file.
*/
public class AnimationAttrSaver implements SpecSaver {
	/**
	* The tag to be used for storing an attribute specification in a file. Every
	* specification is stored as a sequence of lines starting with <TAG_NAME>
	* and ending with </TAG_NAME>.
	*/
	public static final String TAG_NAME = "attribute_description"; //must not be translated!
	/**
	* An alternative tag used for backward compatibility. The class
	* AnimationAttrSaver was initially designed for saving descriptions of
	* attributes used in map animation. Later it was applied to attributes used in
	* other tools. To avoid misunderstandings, a different tag was introduced.
	* However, to be able to process snapshots created earlier, the previous
	* tag is preserved as ALT_TAG_NAME.
	*/
	public static final String ALT_TAG_NAME = "animation_attribute"; //must not be translated!

	/**
	* Checks if the given string is an allowed opening tag name for this reader/writer.
	*/
	public static boolean isValidTagName(String str) {
		if (str == null)
			return false;
		return str.equalsIgnoreCase(TAG_NAME) || str.equalsIgnoreCase(ALT_TAG_NAME);
	}

	/**
	* Reads an attribute specification, i.e. a sequence of lines starting
	* with <tagName> and ending with </tagName>. The first argument is
	* the tag name (must be equal to TAG_NAME), the second argument is the
	* BufferedReader used to read the data. Returns the specification read.
	*/
	@Override
	public Object readSpecification(String tagName, BufferedReader br) throws IOException {
		if (br == null || !isValidTagName(tagName))
			return null;
		AnimationAttrSpec asp = new AnimationAttrSpec();
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
				if (str.equalsIgnoreCase(tagName)) {
					break;
				}
			} else if (str.startsWith("<")) {
				analyseTaggedFragment(str, br, asp);
			} else {
				int idx = str.indexOf("=");
				if (idx > 0 && idx < str.length() - 1) {
					String key = str.substring(0, idx).trim().toLowerCase(), val = StringUtil.removeQuotes(str.substring(idx + 1).trim());
					if (key.length() > 0 && val.length() > 0) {
						analyseKeyAndValue(key, val, asp);
					}
				}
			}
		}
		return asp;
	}

	/**
	* Analyses a specification fragment starting with a tag. A signal of a tag is
	* the symbol "<". If the tag was recognized and read successfully, stores the
	* results of the analysis in the tool specification passed as the third
	* argument.
	*/
	protected void analyseTaggedFragment(String line, BufferedReader br, AnimationAttrSpec asp) throws IOException {
		if (line.equalsIgnoreCase("<fixed_parameter>")) {
			String name = null, value = null;
			while (true) {
				String str = br.readLine();
				if (str == null) {
					break;
				}
				str = str.trim();
				if (str.length() < 1) {
					continue;
				}
				if (str.equalsIgnoreCase("</fixed_parameter>")) {
					break;
				}
				int idx = str.indexOf('=');
				if (idx > 0 && idx < str.length() - 1) {
					String key = str.substring(0, idx).trim().toLowerCase(), val = StringUtil.removeQuotes(str.substring(idx + 1).trim());
					if (key.length() > 0 && val.length() > 0)
						if (key.equals("name")) {
							name = val;
						} else if (key.equals("value")) {
							value = val;
						}
				}
			}
			if (name != null && value != null) {
				if (asp.fixedParams == null) {
					asp.fixedParams = new Vector(10, 10);
					asp.fixedParamVals = new Vector(10, 10);
				}
				asp.fixedParams.addElement(name);
				asp.fixedParamVals.addElement(value);
			}
		}
	}

	/**
	* Analyses the given pair consisting of a keyword and a value and sets the
	* corresponding field in the given ToolSpec.
	*/
	protected void analyseKeyAndValue(String key, String val, AnimationAttrSpec asp) {
		if (key.equals("attribute")) {
			asp.attribute = val;
		} else if (key.equals("parent")) {
			asp.parent = val;
		} else if (key.equals("is_time_dependent")) {
			asp.isTimeDependent = val.equalsIgnoreCase("true");
		} else if (key.equals("offset")) {
			try {
				asp.offset = Integer.valueOf(val).intValue();
			} catch (NumberFormatException nfe) {
				asp.offset = 0;
			}
		}
	}

	/**
	* Stores the given attribute specification to a file as a sequence of
	* lines starting with <TAG_NAME> and ending with </TAG_NAME>.
	*/
	@Override
	public void writeSpecification(Object spec, DataOutputStream writer) throws IOException {
		if (spec == null || writer == null)
			return;
		if (!(spec instanceof AnimationAttrSpec))
			return;
		AnimationAttrSpec asp = (AnimationAttrSpec) spec;
		writer.writeBytes("<" + TAG_NAME + ">\n");
		if (asp.attribute != null) {
			writer.writeBytes("attribute=\"" + IdUtil.getPureAttrId(asp.attribute) + "\"\n");
		}
		if (asp.parent != null) {
			writer.writeBytes("parent=\"" + IdUtil.getPureAttrId(asp.parent) + "\"\n");
		}
		writer.writeBytes("is_time_dependent=" + ((asp.isTimeDependent) ? "true" : "false") + "\n");
		writer.writeBytes("offset=" + asp.offset + "\n");
		if (asp.fixedParams != null) {
			for (int i = 0; i < asp.fixedParams.size(); i++) {
				writer.writeBytes("<fixed_parameter>\n");
				writer.writeBytes("name=\"" + (String) asp.fixedParams.elementAt(i) + "\"\n");
				writer.writeBytes("value=\"" + asp.fixedParamVals.elementAt(i).toString() + "\"\n");
				writer.writeBytes("</fixed_parameter>\n");
			}
		}
		writer.writeBytes("</" + TAG_NAME + ">\n");
	}
}
