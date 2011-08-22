package spade.analysis.generators;

import java.awt.Component;
import java.util.Hashtable;
import java.util.Vector;

import spade.analysis.system.Supervisor;
import spade.vis.database.AttributeDataPortion;
import spade.vis.spec.ToolSpec;

/**
* VisGenerator can construct an object visualizing a given set of attributes
* from a given AttributeDataPortion. For example, this may be a scatterplot
* or a map.
*/

public abstract class VisGenerator {
	/**
	* Contains the error message, if the method is not applicable to specified
	* data or construction of the VisGenerator fails.
	*/
	public String err = null;

	/*
	* Checks applicability of the method to selected data
	*/
	public abstract boolean isApplicable(AttributeDataPortion dataTable, Vector attributes);

	/**
	* Tries to produce a graphic presenting the specified attribute set from
	* the given AttributeDataPortion. The graphic is displayed by a certain
	* Component. If the graphic cannot be produced, returns null.
	* The argument methodId is the identifier of the visualization method
	* which is implemented by this component.
	* The vector attributes contains identifiers of the attributes to be
	* visualised.
	* The argument supervisor contains a reference to the supervisor that
	* supports linkage between multiple displays.
	* The argument properties may specify individual properties for the
	* display to be constructed.
	*/
	public abstract Component constructDisplay(String methodId, Supervisor sup, AttributeDataPortion dataTable, Vector attributes, Hashtable properties);

	/**
	* Tries to produce a graphical display according to the given specification.
	* If the graphic cannot be produced, returns null. 
	* The argument supervisor contains a reference to the supervisor that
	* supports linkage between multiple displays.
	*/
	public Component constructDisplay(ToolSpec spec, Supervisor sup, AttributeDataPortion dataTable) {
		if (spec == null)
			return null;
		return constructDisplay(spec.methodId, sup, dataTable, spec.attributes, spec.properties);
	}

	/**
	* If construction of the graphical display failed, returns the message
	* explaining the reason of this.
	*/
	public String getErrorMessage() {
		return err;
	}

}
