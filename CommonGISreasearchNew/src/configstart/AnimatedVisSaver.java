package configstart;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;

import spade.lib.util.StringUtil;
import spade.vis.spec.AnimatedVisSpec;
import spade.vis.spec.AnimationAttrSpec;
import spade.vis.spec.SpecSaver;
import spade.vis.spec.ToolSpec;

public class AnimatedVisSaver implements SpecSaver {
	/**
	* The tag to be used for storing this specification in a file. Every
	* specification is stored as a sequence of lines starting with <TAG_NAME>
	* and ending with <TAG_NAME>.
	*/
	public static final String TAG_NAME = "animated_tool"; //must not be translated!

	/**
	* Reads a specification of an animated visualization, i.e. a sequence of lines
	* starting with <tagName> and ending with </tagName>. The first argument is
	* the tag name (must be equal to TAG_NAME), the second argument is the
	* BufferedReader used to read the data. Returns the specification read.
	*/
	@Override
	public Object readSpecification(String tagName, BufferedReader br) throws IOException {
		if (tagName == null || br == null)
			return null;
		if (!tagName.equals(TAG_NAME))
			return null;
		AnimatedVisSpec asp = new AnimatedVisSpec();
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
				if (str.equalsIgnoreCase(TAG_NAME)) {
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
	* Allows to avoid repeated constructions of instances of AnimationAttrSaver
	* when it is necessary to read or write several attribute descriptions.
	*/
	protected AnimationAttrSaver attrSaver = null;

	/**
	* Constructs an instance of AnimationAttrSaver or returns a previously
	* constructed instance.
	*/
	protected AnimationAttrSaver getAnimationAttrSaver() {
		if (attrSaver == null) {
			attrSaver = new AnimationAttrSaver();
		}
		return attrSaver;
	}

	/**
	* Analyses a specification fragment starting with a tag. A signal of a tag is
	* the symbol "<". If the tag was recognized and read successfully, stores the
	* results of the analysis in the tool specification passed as the third
	* argument.
	*/
	protected void analyseTaggedFragment(String line, BufferedReader br, AnimatedVisSpec asp) throws IOException {
		if (line == null || br == null || asp == null)
			return;
		if (line.startsWith("<")) {
			line = line.substring(1).trim();
		}
		if (line.endsWith(">")) {
			line = line.substring(0, line.length() - 1).trim();
		}
		if (AnimationAttrSaver.isValidTagName(line)) {
			AnimationAttrSaver saver = getAnimationAttrSaver();
			Object spec = saver.readSpecification(line, br);
			if (spec != null && (spec instanceof AnimationAttrSpec)) {
				if (asp.attrSpecs == null) {
					asp.attrSpecs = new Vector(20, 10);
				}
				asp.attrSpecs.addElement(spec);
			}
		} else {
			SpecSaver saver = StateSaverFactory.getSpecSaver(line);
			if (saver != null) {
				Object spec = saver.readSpecification(line, br);
				if (spec != null && (spec instanceof ToolSpec)) {
					asp.visSpec = (ToolSpec) spec;
				}
			}
		}
	}

	/**
	* Analyses the given pair consisting of a keyword and a value and sets the
	* corresponding field in the given ToolSpec.
	*/
	protected void analyseKeyAndValue(String key, String val, AnimatedVisSpec asp) {
		if (key.equals("table")) {
			asp.table = val;
		}
	}

	/**
	* Stores the given specification of an animated visualization to a file as a
	* sequence of lines starting with <TAG_NAME> and ending with </TAG_NAME>.
	*/
	@Override
	public void writeSpecification(Object spec, DataOutputStream writer) throws IOException {
		if (spec == null || writer == null)
			return;
		if (!(spec instanceof AnimatedVisSpec))
			return;
		AnimatedVisSpec avsp = (AnimatedVisSpec) spec;
		writer.writeBytes("<" + TAG_NAME + ">\n");
		if (avsp.table != null) {
			writer.writeBytes("table=" + avsp.table + "\n");
		}
		if (avsp.attrSpecs != null && avsp.attrSpecs.size() > 0) {
			AnimationAttrSaver attrSaver = getAnimationAttrSaver();
			for (int i = 0; i < avsp.attrSpecs.size(); i++) {
				attrSaver.writeSpecification(avsp.attrSpecs.elementAt(i), writer);
			}
		}
		if (avsp.visSpec != null) {
			SpecSaver saver = StateSaverFactory.getSpecSaver(avsp.visSpec.tagName);
			if (saver != null) {
				saver.writeSpecification(avsp.visSpec, writer);
			}
		}
		writer.writeBytes("</" + TAG_NAME + ">\n");
	}
}
