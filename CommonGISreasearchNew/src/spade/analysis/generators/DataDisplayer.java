package spade.analysis.generators;

import java.awt.Component;
import java.util.Hashtable;
import java.util.Vector;

import spade.analysis.system.Supervisor;
import spade.analysis.tools.ToolKeeper;
import spade.vis.database.AttributeDataPortion;
import spade.vis.spec.ToolSpec;

/**
* The class is used for generation of various graphical data displays
*/
public class DataDisplayer {
	/**
	* The object managing descriptions of all available display generators. A
	* generator should descend from the abstract class VisGenerator
	*/
	protected ToolKeeper toolKeeper = new ToolKeeper(new PlotGeneratorsDescriptor());
	/**
	* The error message
	*/
	protected String err = null;

	/**
	* Returns the number of available methods
	*/
	public int getAvailableMethodCount() {
		return toolKeeper.getAvailableToolCount();
	}

	/**
	* Returns the identifier of the AVAILABLE method with the given index
	*/
	public String getAvailableMethodId(int methodN) {
		return toolKeeper.getAvailableToolId(methodN);
	}

	/**
	* Returns the full name of the AVAILABLE method with the given index
	*/
	public String getAvailableMethodName(int methodN) {
		return toolKeeper.getAvailableToolName(methodN);
	}

	/**
	* Replies whether the display method with the given identifier
	* is implemented or available in the system.
	*/
	public boolean isMethodAvailable(String methodId) {
		return toolKeeper.isToolAvailable(methodId);
	}

	/**
	* Replies if the given display type is attribute-free (i.e. does not visualize
	* any attributes)
	*/
	public static boolean isMethodAttributeFree(String methodId) {
		return PlotGeneratorsDescriptor.isAttributeFree(methodId);
	}

	/**
	* Constructs a display generator according to the given method identifier.
	*/
	protected VisGenerator getGenerator(String methodId) {
		Object tool = toolKeeper.getTool(methodId);
		if (tool != null && (tool instanceof VisGenerator))
			return (VisGenerator) tool;
		err = toolKeeper.getErrorMessage();
		return null;
	}

	/**
	* Checks if the display method with the given identifier is applicable to the
	* given data
	*/
	public boolean isDisplayMethodApplicable(String methodId, AttributeDataPortion dtab, Vector attr) {
		err = null;
		VisGenerator vg = getGenerator(methodId);
		if (vg != null)
			if (vg.isApplicable(dtab, attr))
				return true;
			else {
				err = vg.getErrorMessage();
			}
		return false;
	}

	/**
	* Applies the display method with the given identifier to the data in order to
	* generate a data display. Returns the resulting display (as a Component)
	* The argument properties may specify individual properties for the
	* display to be constructed.
	*/
	public Component makeDisplay(Supervisor sup, AttributeDataPortion dtab, Vector attr, //attributes to visualize
			String methodId, //id of vis. method to apply
			Hashtable properties) {
		err = null;
		VisGenerator vg = getGenerator(methodId);
		if (vg == null)
			return null;
		Component c = vg.constructDisplay(methodId, sup, dtab, attr, properties);
		if (c == null) {
			err = vg.getErrorMessage();
		}
		return c;
	}

	/**
	* Tries to produce a graphical display according to the given specification.
	* Returns the resulting display (as a Component).
	* If the graphic cannot be produced, returns null. 
	*/
	public Component makeDisplay(ToolSpec spec, Supervisor sup, AttributeDataPortion dtab) {
		if (spec == null)
			return null;
		err = null;
		VisGenerator vg = getGenerator(spec.methodId);
		if (vg == null)
			return null;
		Component c = vg.constructDisplay(spec, sup, dtab);
		if (c == null) {
			err = vg.getErrorMessage();
		}
		return c;
	}

	/**
	* If construction of the graphical display failed, returns the message
	* explaining the reason of this.
	*/
	public String getErrorMessage() {
		return err;
	}
}
