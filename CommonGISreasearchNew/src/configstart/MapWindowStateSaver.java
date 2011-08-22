package configstart;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.StringTokenizer;

import spade.vis.geometry.RealRectangle;
import spade.vis.spec.MapWindowSpec;
import spade.vis.spec.WinSpec;

/**
* Reads a tool specification from a file or writes a tool specification to a
* file. A specification in a file is a sequence of lines starting with
* <keyword> and ending with </keyword>.
*/
public class MapWindowStateSaver extends WinStateSaver {
	/**
	* Constructs an instance of a tool specification class
	* (spade.vis.spec.MapWindowSpec).
	* May be overridden for constructing decsendants of spade.vis.spec.WinSpec.
	*/
	@Override
	protected WinSpec constructSpecInstance() {
		return new MapWindowSpec();
	}

	/**
	* Analyses the given pair consisting of a keyword and a value and sets the
	* corresponding field in the given ToolSpec.
	*/
	@Override
	protected void analyseKeyAndValue(String key, String val, WinSpec spec) {
		if (key == null || spec == null)
			return;
		if (!(spec instanceof MapWindowSpec)) {
			super.analyseKeyAndValue(key, val, spec);
			return;
		}
		MapWindowSpec wsp = (MapWindowSpec) spec;
		if (key.equalsIgnoreCase("windowId")) {
			wsp.windowId = val;
		} else if (key.equals("primary")) {
			wsp.primary = new Boolean(val).booleanValue();
		} else if (key.equals("extent")) {
			StringTokenizer stt = new StringTokenizer(val, " (,);");
			float bou[] = new float[4];
			boolean gotAll = false;
			for (int i = 0; i < 4 && stt.hasMoreTokens(); i++) {
				try {
					bou[i] = new Float(stt.nextToken()).floatValue();
					if (i == 3) {
						gotAll = true;
					}
				} catch (NumberFormatException ne) {
					break;
				}
			}
			if (gotAll) {
				wsp.extent = new RealRectangle(bou[0], bou[1], bou[2], bou[3]);
			}
		} else {
			super.analyseKeyAndValue(key, val, wsp);
		}
	}

	/**
	* Reserved for overriding in descendants. May store a fragment of the
	* specification pertinent to a descendant. Is called from the method
	* writeSpecification(...), which stores the part of the specification
	* common for all tools.
	*/
	@Override
	protected void writeSpecificSpecPart(WinSpec spec, DataOutputStream writer) throws IOException {
		if (spec == null || writer == null)
			return;
		if (!(spec instanceof MapWindowSpec)) {
			super.writeSpecificSpecPart(spec, writer);
			return;
		}
		MapWindowSpec wsp = (MapWindowSpec) spec;
		if (wsp.windowId != null) {
			writer.writeBytes("windowId=" + wsp.windowId + "\n");
		}
		writer.writeBytes("primary=" + wsp.primary + "\n");
		if (wsp.extent != null) {
			writer.writeBytes("extent=(" + wsp.extent.rx1 + "," + wsp.extent.ry1 + "," + wsp.extent.rx2 + "," + wsp.extent.ry2 + ")\n");
		}
	}
}