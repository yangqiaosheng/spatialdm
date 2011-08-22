package configstart;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import spade.lib.util.StringUtil;
import spade.vis.spec.ConditionSpec;
import spade.vis.spec.QuerySpec;
import spade.vis.spec.ToolSpec;

/**
* Reads a specification of a query tool from a file or writes a tool
* specification to a file. A specification in a file is a
* sequence of lines starting with <keyword> and ending with </keyword>.
*/
public class QuerySaver extends ToolStateSaver {
	/**
	* Checks if the given string is an allowed opening tag name for this reader/writer.
	*/
	@Override
	public boolean isValidTagName(String str) {
		if (str == null)
			return false;
		return str.equalsIgnoreCase("query");
	}

	/**
	* Constructs an instance of a tool specification class (spade.vis.spec.ToolSpec).
	* May be overridden for constructing decsendants of spade.vis.spec.ToolSpec.
	*/
	@Override
	protected ToolSpec constructToolSpec() {
		return new QuerySpec();
	}

	/**
	* Analyses a specification fragment starting with a tag. A signal of a tag is
	* the symbol "<". If the tag was recognized and read successfully, stores the
	* results of the analysis in the tool specification passed as the third
	* argument.
	*/
	@Override
	protected void analyseTaggedFragment(String line, BufferedReader br, ToolSpec tsp) throws IOException {
		if (line == null || br == null || tsp == null)
			return;
		if (!(tsp instanceof QuerySpec)) {
			super.analyseTaggedFragment(line, br, tsp);
			return;
		}
		QuerySpec qsp = (QuerySpec) tsp;
		if (line.equalsIgnoreCase("<condition>")) {
			String type = null;
			Hashtable cDescr = new Hashtable();
			while (true) {
				String str = br.readLine();
				if (str == null) {
					break;
				}
				str = str.trim();
				if (str.length() < 1) {
					continue;
				}
				if (str.equalsIgnoreCase("</condition>")) {
					break;
				}
				int idx = str.indexOf('=');
				if (idx <= 0 || idx >= (str.length() - 1)) {
					continue;
				}
				String key = str.substring(0, idx).trim().toLowerCase(), val = StringUtil.removeQuotes(str.substring(idx + 1).trim());
				if (key.length() > 0 && val.length() > 0)
					if (key.equalsIgnoreCase("type")) {
						type = val;
					} else {
						cDescr.put(key, val);
					}
			}
			if (type != null && cDescr.size() > 0) {
				ConditionSpec csp = new ConditionSpec();
				csp.type = type;
				csp.description = cDescr;
				if (qsp.conditions == null) {
					qsp.conditions = new Vector(20, 10);
				}
				qsp.conditions.addElement(csp);
			}
		} else {
			super.analyseTaggedFragment(line, br, tsp);
		}
	}

	/**
	* Stores a fragment of the specification describing query conditions.
	* Is called from the method writeSpecification(...) of the ancestor class,
	* which stores the part of the specification common for all tools.
	*/
	@Override
	protected void writeSpecificSpecPart(ToolSpec spec, DataOutputStream writer) throws IOException {
		if (!(spec instanceof QuerySpec))
			return;
		QuerySpec qsp = (QuerySpec) spec;
		if (qsp.conditions == null || qsp.conditions.size() < 1)
			return;
		for (int i = 0; i < qsp.conditions.size(); i++) {
			ConditionSpec csp = (ConditionSpec) qsp.conditions.elementAt(i);
			if (csp.type == null || csp.description == null || csp.description.size() < 1) {
				continue;
			}
			writer.writeBytes("<condition>\n");
			writer.writeBytes("type=" + csp.type + "\n");
			for (Enumeration keys = csp.description.keys(); keys.hasMoreElements();) {
				String key = (String) keys.nextElement();
				writer.writeBytes(key + "=" + String.valueOf(csp.description.get(key)) + "\n");
			}
			writer.writeBytes("</condition>\n");
		}
	}
}
