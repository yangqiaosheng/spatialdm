package spade.analysis.generators;

import java.awt.Component;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.plot.DispersionGraph;
import spade.analysis.system.Supervisor;
import spade.lib.lang.Language;
import spade.vis.database.AttributeDataPortion;

/**
* Generates a frequency histogram display for a numeric attribute
*/
public class DispersionGraphGenerator extends VisGenerator {
	static ResourceBundle res = Language.getTextResource("spade.analysis.generators.Res");

	@Override
	public boolean isApplicable(AttributeDataPortion dataTable, Vector attributes) {
		if (dataTable == null) {
			//following text: "No data source provided!"
			err = res.getString("No_data_source");
			return false;
		}
		if (!dataTable.hasData()) {
			//following text:"No data in the data source!"
			err = res.getString("No_data_in_the_data");
			return false;
		}
		if (attributes == null || attributes.size() < 1) {
			//following text:"No attributes specified!"
			err = res.getString("No_attributes");
			return false;
		}
		int nattr = attributes.size();
		if (nattr > 1) {
			//following text:"Too many attributes! A dot plot can present only single attribute!"
			err = res.getString("Too_many_attributes_A");
			return false;
		}
		int idx = dataTable.getAttrIndex((String) attributes.elementAt(0));
		if (idx < 0) {
			//following text:"The attribute "+attributes.elementAt(0)+" is not found in the data source!"
			err = res.getString("The_attribute") + attributes.elementAt(0) + res.getString("is_not_found_in_the");
			return false;
		}
		if (!dataTable.isAttributeNumeric(idx) && !dataTable.isAttributeTemporal(idx)) {
			//following text:"The attribute "+attributes.elementAt(0)+" is not numeric!"
			err = res.getString("The_attribute") + attributes.elementAt(0) + res.getString("is_not_numeric_");
			return false;
		}
		return true;
	}

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
	@Override
	public Component constructDisplay(String methodId, Supervisor sup, AttributeDataPortion dataTable, Vector attributes, Hashtable properties) {
		if (!isApplicable(dataTable, attributes))
			return null;
		DispersionGraph dg = new DispersionGraph(dataTable, (String) attributes.elementAt(0), sup);
		dg.getGraph().setMethodId(methodId);
		dg.getGraph().setProperties(properties);
		sup.registerTool(dg.getGraph());
		dg.setName(dg.getGraph().getName());
		return dg;
	}
}
