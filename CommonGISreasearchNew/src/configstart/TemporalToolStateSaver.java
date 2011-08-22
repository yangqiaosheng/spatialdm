package configstart;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;

import spade.vis.spec.AnimationAttrSpec;
import spade.vis.spec.TemporalToolSpec;
import spade.vis.spec.ToolSpec;

/**
* Reads a specification of a tool dealing with time-dependent data from a file
* or writes a tool specification to a file. A specification in a file is a
* sequence of lines starting with <keyword> and ending with </keyword>.
*/
public class TemporalToolStateSaver extends ToolStateSaver {
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
	* Checks if the given string is an allowed opening tag name for this reader/writer.
	*/
	@Override
	public boolean isValidTagName(String str) {
		if (str == null)
			return false;
		return str.equalsIgnoreCase("temporal_vis");
	}

	/**
	* Constructs an instance of a tool specification class (spade.vis.spec.ToolSpec).
	* May be overridden for constructing decsendants of spade.vis.spec.ToolSpec.
	*/
	@Override
	protected ToolSpec constructToolSpec() {
		return new TemporalToolSpec();
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
		if (!(tsp instanceof TemporalToolSpec)) {
			super.analyseTaggedFragment(line, br, tsp);
			return;
		}
		String ln = line;
		if (ln.startsWith("<")) {
			ln = ln.substring(1).trim();
		}
		if (ln.endsWith(">")) {
			ln = ln.substring(0, ln.length() - 1).trim();
		}
		TemporalToolSpec ttsp = (TemporalToolSpec) tsp;
		if (AnimationAttrSaver.isValidTagName(ln)) {
			AnimationAttrSaver saver = getAnimationAttrSaver();
			Object spec = saver.readSpecification(ln, br);
			if (spec != null && (spec instanceof AnimationAttrSpec)) {
				if (ttsp.attrSpecs == null) {
					ttsp.attrSpecs = new Vector(20, 10);
				}
				ttsp.attrSpecs.addElement(spec);
			}
		} else {
			super.analyseTaggedFragment(line, br, tsp);
		}
	}

	/**
	* Stores a fragment of the specification describing time-dependent attributes.
	* Is called from the method writeSpecification(...) of the ancestor class,
	* which stores the part of the specification common for all tools.
	*/
	@Override
	protected void writeSpecificSpecPart(ToolSpec spec, DataOutputStream writer) throws IOException {
		if (!(spec instanceof TemporalToolSpec))
			return;
		TemporalToolSpec ttsp = (TemporalToolSpec) spec;
		if (ttsp.attrSpecs == null || ttsp.attrSpecs.size() < 1)
			return;
		AnimationAttrSaver attrSaver = getAnimationAttrSaver();
		for (int i = 0; i < ttsp.attrSpecs.size(); i++) {
			attrSaver.writeSpecification(ttsp.attrSpecs.elementAt(i), writer);
		}
	}
}
