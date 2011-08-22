package configstart;

import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.Vector;

import spade.lib.util.IdUtil;
import spade.lib.util.StringUtil;
import spade.vis.spec.SpecSaver;
import spade.vis.spec.ToolSpec;
import spade.vis.spec.TransformSequenceSpec;

/**
* Reads a tool specification from a file or writes a tool specification to a
* file. A specification in a file is a sequence of lines starting with
* <keyword> and ending with </keyword>.
*/
public class ToolStateSaver implements SpecSaver {
	/**
	* The list of allowed opening tag names for this reader/writer
	*/
	protected static String tags[] = { "map", "chart", "tool" };

	/**
	* Checks if the given string is an allowed opening tag name for this reader/writer.
	*/
	public boolean isValidTagName(String str) {
		if (str == null || str.length() < 1)
			return false;
		for (String tag : tags)
			if (tag.equalsIgnoreCase(str))
				return true;
		return false;
	}

	/**
	* Constructs an instance of a tool specification class (spade.vis.spec.ToolSpec).
	* May be overridden for constructing decsendants of spade.vis.spec.ToolSpec.
	*/
	protected ToolSpec constructToolSpec() {
		return new ToolSpec();
	}

	/**
	* Reads a tool specification, i.e. a sequence of lines starting
	* with <tagName> and ending with </tagName>. The first argument is
	* the tag name (e.g. "tool"), the second argument is the BufferedReader used
	* to read the data. Returns the specification read.
	*/
	@Override
	public Object readSpecification(String tagName, BufferedReader br) throws IOException {
		if (tagName == null || br == null)
			return null;
		if (!isValidTagName(tagName))
			return null;
		ToolSpec tsp = constructToolSpec();
		tsp.tagName = tagName.toLowerCase();
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
				if (str.equalsIgnoreCase(tsp.tagName)) {
					break;
				}
			} else if (str.startsWith("<")) {
				analyseTaggedFragment(str, br, tsp);
			} else {
				int idx = str.indexOf("=");
				if (idx > 0 && idx < str.length() - 1) {
					String key = str.substring(0, idx).trim().toLowerCase(), val = StringUtil.removeQuotes(str.substring(idx + 1).trim());
					if (key.length() > 0 && val.length() > 0) {
						analyseKeyAndValue(key, val, tsp);
					}
				}
			}
		}
		return tsp;
	}

	/**
	* Analyses a specification fragment starting with a tag. A signal of a tag is
	* the symbol "<". If the tag was recognized and read successfully, stores the
	* results of the analysis in the tool specification passed as the third
	* argument and returns.
	*/
	protected void analyseTaggedFragment(String line, BufferedReader br, ToolSpec tsp) throws IOException {
		if (line.equalsIgnoreCase("<attributes>")) {
			tsp.attributes = readAttributeList(br);
		} else if (line.equalsIgnoreCase("<properties>")) {
			tsp.properties = PropertySaver.readProperties(br);
		} else if (line.equalsIgnoreCase("<transformation>")) {
			tsp.transformSeqSpec = readTransformSpec(br);
		}
	}

	/**
	* Analyses the given pair consisting of a keyword and a value and sets the
	* corresponding field in the given ToolSpec.
	*/
	protected void analyseKeyAndValue(String key, String val, ToolSpec tsp) {
		if (key.equals("table")) {
			tsp.table = val;
		} else if (key.equals("method") || key.equals("type") || key.equals("tool"))
			if (!val.equalsIgnoreCase("none")) {
				tsp.methodId = val;
			} else {
				;
			}
		else if (key.equals("chartid")) {
			tsp.chartId = val;
		} else if (key.equals("location")) {
			tsp.location = val;
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
				tsp.bounds = new Rectangle(bou[0], bou[1], bou[2], bou[3]);
			}
		}
	}

	/**
	* Reads a list of attribute identifiers contained between lines <attributes>
	* and </attributes>. Each attribute identifier comes in a separate line.
	*/
	protected Vector readAttributeList(BufferedReader br) throws IOException {
		if (br == null)
			return null;
		Vector attr = new Vector(10, 5);
		while (true) {
			String str = br.readLine();
			if (str == null) {
				break;
			}
			str = str.trim();
			if (str.length() < 1) {
				continue;
			}
			if (str.equalsIgnoreCase("</attributes>")) {
				break;
			}
			attr.addElement(str);
		}
		if (attr.size() < 1)
			return null;
		attr.trimToSize();
		return attr;
	}

	/**
	* Reads a sequence of attribute transformers attached to the tool.
	*/
	protected TransformSequenceSpec readTransformSpec(BufferedReader br) throws IOException {
		if (br == null)
			return null;
		SpecSaver trSaver = StateSaverFactory.getSpecSaver("transformation");
		if (trSaver != null) {
			Object spec = trSaver.readSpecification("transformation", br);
			if (spec != null && (spec instanceof TransformSequenceSpec))
				return (TransformSequenceSpec) spec;
		} else {
			while (true) {
				String str = br.readLine();
				if (str == null) {
					break;
				}
				str = str.trim();
				if (str.equalsIgnoreCase("</transformation>")) {
					break;
				}
			}
		}
		return null;
	}

	/**
	* Stores the given specification to a file as a sequence of
	* lines starting with <keyword ...> and ending with </keyword>.
	*/
	@Override
	public void writeSpecification(Object spec, DataOutputStream writer) throws IOException {
		if (spec == null || writer == null)
			return;
		if (!(spec instanceof ToolSpec))
			return;
		ToolSpec tsp = (ToolSpec) spec;
		if (tsp.tagName == null)
			return;
		writer.writeBytes("<" + tsp.tagName + ">\n");
		if (tsp.methodId != null) {
			writer.writeBytes("method=" + tsp.methodId + "\n");
		}
		if (tsp.chartId != null) {
			writer.writeBytes("chartId=" + tsp.chartId + "\n");
		}
		if (tsp.table != null) {
			writer.writeBytes("table=" + tsp.table + "\n");
		}
		if (tsp.attributes != null && tsp.attributes.size() > 0) {
			writer.writeBytes("<attributes>\n");
			for (int i = 0; i < tsp.attributes.size(); i++)
				if (tsp.attributes.elementAt(i) != null) {
					writer.writeBytes(IdUtil.getPureAttrId((String) tsp.attributes.elementAt(i)) + "\n");
				}
			writer.writeBytes("</attributes>\n");
		}
		if (tsp.transformSeqSpec != null) {
			SpecSaver trSaver = StateSaverFactory.getSpecSaver("transformation");
			if (trSaver != null) {
				trSaver.writeSpecification(tsp.transformSeqSpec, writer);
			}
		}
		if (tsp.location != null) {
			writer.writeBytes("location=" + tsp.location + "\n");
		}
		if (tsp.bounds != null) {
			writer.writeBytes("bounds=(" + tsp.bounds.x + "," + tsp.bounds.y + "," + tsp.bounds.width + "," + tsp.bounds.height + ")\n");
		}
		if (tsp.properties != null) {
			PropertySaver.writeProperties(tsp.properties, writer);
		}
		writeSpecificSpecPart(tsp, writer);
		writer.writeBytes("</" + tsp.tagName + ">\n");
	}

	/**
	* Reserved for overriding in descendants. May store a fragment of the
	* specification pertinent to a descendant. Is called from the method
	* writeSpecification(...), which stores the part of the specification
	* common for all tools.
	*/
	protected void writeSpecificSpecPart(ToolSpec spec, DataOutputStream writer) throws IOException {
	}
}
