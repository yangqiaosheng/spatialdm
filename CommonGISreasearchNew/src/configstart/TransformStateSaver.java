package configstart;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;

import spade.lib.util.StringUtil;
import spade.vis.spec.SpecSaver;
import spade.vis.spec.TransformSequenceSpec;
import spade.vis.spec.TransformSpec;

/**
* Used for saving and restoring states of sequences of attribute transformers,
* which may be attached to maps or graphs.
*/
public class TransformStateSaver implements SpecSaver {
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
		if (!tagName.equalsIgnoreCase("transformation"))
			return null;
		TransformSequenceSpec tss = new TransformSequenceSpec();
		while (true) {
			String str = br.readLine();
			if (str == null) {
				break;
			}
			str = str.trim();
			if (str.length() < 1) {
				continue;
			}
			if (str.equalsIgnoreCase("</transformation>")) {
				break;
			}
			if (str.equalsIgnoreCase("<transformer>")) {
				TransformSpec tsp = readTransformerSpec(br);
				if (tsp != null) {
					if (tss.transSp == null) {
						tss.transSp = new Vector(10, 10);
					}
					tss.transSp.addElement(tsp);
				}
			}
		}
		if (tss.transSp == null || tss.transSp.size() < 1)
			return null;
		return tss;
	}

	/**
	* Reads the specification of a single attribute transformer.
	*/
	public TransformSpec readTransformerSpec(BufferedReader br) throws IOException {
		if (br == null)
			return null;
		TransformSpec tsp = new TransformSpec();
		while (true) {
			String str = br.readLine();
			if (str == null) {
				break;
			}
			str = str.trim();
			if (str.length() < 1) {
				continue;
			}
			if (str.equalsIgnoreCase("</transformer>")) {
				break;
			}
			if (str.equalsIgnoreCase("<properties>")) {
				tsp.properties = PropertySaver.readProperties(br);
			} else {
				int idx = str.indexOf("=");
				if (idx > 0 && idx < str.length() - 1) {
					String key = str.substring(0, idx).trim().toLowerCase(), val = StringUtil.removeQuotes(str.substring(idx + 1).trim());
					if (key.equals("method")) {
						tsp.methodId = val;
					}
				}
			}
		}
		if (tsp.methodId == null)
			return null;
		return tsp;
	}

	/**
	* Stores the given specification to a file as a sequence of
	* lines starting with <keyword ...> and ending with </keyword>.
	*/
	@Override
	public void writeSpecification(Object spec, DataOutputStream writer) throws IOException {
		if (spec == null || writer == null)
			return;
		if (!(spec instanceof TransformSequenceSpec))
			return;
		TransformSequenceSpec tsp = (TransformSequenceSpec) spec;
		if (tsp.transSp == null || tsp.transSp.size() < 1)
			return;
		writer.writeBytes("<transformation>\n");
		for (int i = 0; i < tsp.transSp.size(); i++)
			if (tsp.transSp.elementAt(i) != null && (tsp.transSp.elementAt(i) instanceof TransformSpec)) {
				writeTransformerSpec((TransformSpec) tsp.transSp.elementAt(i), writer);
			}
		writer.writeBytes("</transformation>\n");
	}

	/**
	* Stores the specification of a single attribute transformer.
	*/
	public void writeTransformerSpec(TransformSpec tsp, DataOutputStream writer) throws IOException {
		if (tsp == null || writer == null || tsp.methodId == null)
			return;
		writer.writeBytes("<transformer>\n");
		writer.writeBytes("method=" + tsp.methodId + "\n");
		if (tsp.properties != null) {
			PropertySaver.writeProperties(tsp.properties, writer);
		}
		writer.writeBytes("</transformer>\n");
	}
}
