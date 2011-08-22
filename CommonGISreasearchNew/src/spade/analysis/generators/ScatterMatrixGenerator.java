package spade.analysis.generators;

import java.awt.Component;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.plot.ScatterMatrix;
import spade.analysis.system.Supervisor;
import spade.lib.lang.Language;
import spade.vis.database.AttributeDataPortion;

/**
* ScatterMatrixGenerator can construct a scatterplot matrix presenting a given
* set of attributes (two and more) from a given AttributeDataPortion.
*/

public class ScatterMatrixGenerator extends VisGenerator {
	static ResourceBundle res = Language.getTextResource("spade.analysis.generators.Res");

	/*
	* Checks applicability of the method to selected data
	*/
	@Override
	public boolean isApplicable(AttributeDataPortion dataTable, Vector attributes) {
		if (dataTable == null) {
			// following Text: "No data source provided!"
			err = res.getString("No_data_source");
			return false;
		}
		if (!dataTable.hasData()) {
			// following Text: "No data in the data source!"
			err = res.getString("No_data_in_the_data");
			return false;
		}
		if (attributes == null || attributes.size() < 1) {
			// following Text: "No attributes specified!"
			err = res.getString("No_attributes");
			return false;
		}
		int nattr = attributes.size();
		if (nattr < 2) {
			// following Text: "Too few attributes! A scatterplot matrix can present at least two attributes!"
			err = res.getString("Too_few_attributes_A2");
			return false;
		}
		for (int i = 0; i < nattr; i++) {
			int idx = dataTable.getAttrIndex((String) attributes.elementAt(i));
			if (idx < 0) {
				// following Text: "The attribute "+attributes.elementAt(i)+" is not found in the data source!"
				err = res.getString("The_attribute") + attributes.elementAt(i) + res.getString("is_not_found_in_the");
				return false;
			}
			if (!dataTable.isAttributeNumeric(idx) && !dataTable.isAttributeTemporal(idx)) {
				// following Text: "The attribute "+attributes.elementAt(i)+" is not numeric!"
				err = res.getString("The_attribute") + attributes.elementAt(i) + res.getString("is_not_numeric_");
				return false;
			}
		}
		return true;
	}

	/**
	* Tries to produce a scatterplot matrix presenting the specified attribute set
	* from the given AttributeDataPortion. Returns a Panel containing a number of
	* instances of ScatterPlot.
	* If a scatterplot matrix cannot be produced, returns null.
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
		ScatterMatrix sm = new ScatterMatrix(sup, dataTable, attributes);
		// following Text: "Scatterplot matrix "
//    sm.setName(res.getString("Scatterplot_matrix")+sm.getInstanceN());
		sm.setMethodId(methodId);
		sm.setProperties(properties);
		sup.registerTool(sm);
		return sm;
	}
}
