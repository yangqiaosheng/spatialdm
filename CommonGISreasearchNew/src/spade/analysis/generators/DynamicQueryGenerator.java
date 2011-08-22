package spade.analysis.generators;

import java.awt.Component;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.plot.DynamicQueryShell;
import spade.analysis.system.Supervisor;
import spade.lib.lang.Language;
import spade.vis.database.AttributeDataPortion;

/**
* DynamicQueryGenerator can construct a dynamic query device with a given
* set of attributes from a given AttributeDataPortion.
*/

public class DynamicQueryGenerator extends VisGenerator {
	static ResourceBundle res = Language.getTextResource("spade.analysis.generators.Res");

	/*
	* Checks applicability of the method to selected data
	*/
	@Override
	public boolean isApplicable(AttributeDataPortion dataTable, Vector attributes) {
		if (dataTable == null) {
			// following text:"No data source provided!"
			err = res.getString("No_data_source");
			return false;
		}
		if (!dataTable.hasData()) {
			// following text:  "No data in the data source!"
			err = res.getString("No_data_in_the_data");
			return false;
		}
		if (attributes == null || attributes.size() < 1) {
			// following text: "No attributes specified!"
			err = res.getString("No_attributes");
			return false;
		}
		int nattr = attributes.size();
		if (nattr < 1) {
			// following text: "Too few attributes! A Dynamic Query can present at least one attribute!"
			err = res.getString("Too_few_attributes_A");
			return false;
		}
		for (int i = 0; i < nattr; i++) {
			int idx = dataTable.getAttrIndex((String) attributes.elementAt(i));
			if (idx < 0) {
				// following text: "Too few attributes! A Dynamic Query can present at least one attribute!"
				err = res.getString("The_attribute") + attributes.elementAt(i) + res.getString("is_not_found_in_the");
				return false;
			}
			if (!dataTable.isAttributeNumeric(idx)) {
				// following text: "Too few attributes! A Dynamic Query can present at least one attribute!"
				err = res.getString("The_attribute") + attributes.elementAt(i) + res.getString("is_not_numeric_");
				return false;
			}
		}
		return true;
	}

	/**
	* Tries to produce a dynamic query presenting the specified attribute set
	* from the given AttributeDataPortion. Returns a Panel including a DQ.
	* If a DQ cannot be produced, returns null.
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
		DynamicQueryShell dqs = new DynamicQueryShell();
		dqs.construct(sup, dataTable, attributes);
		// following text: "Dynamic query "
		dqs.setName(res.getString("Dynamic_query") + dqs.getInstanceN());
		dqs.getDynamicQuery().setMethodId(methodId);
		dqs.getDynamicQuery().setProperties(properties);
		sup.registerTool(dqs.getDynamicQuery());
		return dqs;
	}

}
