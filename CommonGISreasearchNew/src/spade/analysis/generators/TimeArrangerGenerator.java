package spade.analysis.generators;

import java.awt.Component;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.Supervisor;
import spade.lib.lang.Language;
import spade.time.vis.TimeArranger;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataTable;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Nov 3, 2009
 * Time: 12:02:38 PM
 */
public class TimeArrangerGenerator extends VisGenerator {
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
			// following Text: "No data in the data source!";
			err = res.getString("No_data_in_the_data");
			return false;
		}
		if (!(dataTable instanceof DataTable)) {
			err = "The table is not an instance of DataTable!";
			return false;
		}
		DataTable tbl = (DataTable) dataTable;
		if (tbl.getNatureOfItems() != DataTable.NATURE_TIME) {
			err = "The objects described in the table are not time moments!";
			return false;
		}
		if (attributes == null || attributes.size() < 1) {
			// following Text: "No attributes specified!
			err = res.getString("No_attributes");
			return false;
		}
		int nattr = attributes.size();
		if (nattr > 1) {
			err = "Too many attributes! A single attribute is required!";
			return false;
		}
		int idx = dataTable.getAttrIndex((String) attributes.elementAt(0));
		if (idx < 0) {
			//following text:"The attribute "+attributes.elementAt(0)+" is not found in the data source!"
			err = res.getString("The_attribute") + attributes.elementAt(0) + res.getString("is_not_found_in_the");
			return false;
		}
		if (!AttributeTypes.isNominalType(dataTable.getAttributeType(idx)) && !dataTable.getAttribute(idx).isClassification()) {
			err = "The attribute is not a classification attribute!";
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
		int idx = dataTable.getAttrIndex((String) attributes.elementAt(0));
		TimeArranger tar = new TimeArranger((DataTable) dataTable, idx, sup);
		return tar;
	}
}
