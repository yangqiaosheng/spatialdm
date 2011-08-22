package spade.analysis.generators;

import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.plot.StackedDotPlot;
import spade.analysis.system.Supervisor;
import spade.lib.basicwin.TImgButton;
import spade.lib.lang.Language;
import spade.vis.database.AttributeDataPortion;

/**
* DotPlotGenerator can construct a dot plot plot presenting a given
* attribute from a given AttributeDataPortion.
*/

public abstract class StackedDotPlotGenerator extends VisGenerator {
	static ResourceBundle res = Language.getTextResource("spade.analysis.generators.Res");

	/*
	* Checks applicability of the method to selected data
	*/
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
		if (!dataTable.isAttributeNumeric(idx)) {
			//following text:"The attribute "+attributes.elementAt(0)+" is not numeric!"
			err = res.getString("The_attribute") + attributes.elementAt(0) + res.getString("is_not_numeric_");
			return false;
		}
		return true;
	}

	/**
	* Tries to produce a dot plot presenting the specified attribute
	* from the given AttributeDataPortion. Returns a Panel including a dot plot.
	* If a dot plot cannot be produced, returns null.
	* The vector attributes contains identifiers of the attributes to be
	* visualised.
	* The argument supervisor contains a reference to the supervisor that
	* supports linkage between multiple displays.
	* The argument properties may specify individual properties for the
	* display to be constructed.
	*/
	public StackedDotPlot constructStackedDotPlot(String methodId, Supervisor sup, AttributeDataPortion dataTable, Vector attributes, boolean isHorisontal, Hashtable properties, TImgButton b) {
		if (!isApplicable(dataTable, attributes))
			return null;
		StackedDotPlot jp = new StackedDotPlot(isHorisontal, true, true, sup, sup, attributes, b);
		jp.setMethodId(methodId);
		jp.setDataSource(dataTable);
		jp.setFieldNumber(dataTable.getAttrIndex((String) attributes.elementAt(0)));
		jp.setIsZoomable(true);
		jp.setup();
		jp.checkWhatSelected();
		jp.setProperties(properties);
		sup.registerTool(jp);
		return jp;
	}
}
