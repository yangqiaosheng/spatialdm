package spade.analysis.generators;

import java.awt.Component;
import java.util.Hashtable;
import java.util.Vector;

import spade.analysis.plot.Histogram2dPanel;
import spade.analysis.system.Supervisor;
import spade.vis.database.AttributeDataPortion;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: Apr 7, 2010
 * Time: 5:40:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class Histogram2dGenerator extends VisGenerator {
	/*
	* Checks applicability of the method to selected data
	*/
	@Override
	public boolean isApplicable(AttributeDataPortion dataTable, Vector attributes) {
		if (dataTable == null) {
			err = "No data source provided!";
			return false;
		}
		if (!dataTable.hasData()) {
			err = "No data in the data source!";
			return false;
		}
		if (attributes == null /*|| attributes.size()<1*/) {
			err = "No attributes specified!";
			return false;
		}
		int nattr = attributes.size();
		if (nattr != 2) {
			err = "2D histogram represents two numeric attributes!";
			return false;
		}
		int idx = dataTable.getAttrIndex((String) attributes.elementAt(0));
		if (idx < 0) {
			err = "The attribute " + attributes.elementAt(0) + " is not found in the data source!";
			return false;
		}
		if (!dataTable.isAttributeNumeric(idx) && !dataTable.isAttributeTemporal(idx)) {
			err = "The attribute " + attributes.elementAt(0) + " is not numeric!";
			return false;
		}
		return true;
	}

	/**
	* Generates a 2d frequency histogram display. If such a display cannot be produced,
	* returns null.
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
		Histogram2dPanel ph = new Histogram2dPanel(dataTable, attributes, sup);
		ph.setMethodId(methodId);
/*
    ph.setProperties(properties);
    sup.registerTool(ph);
*/
		return ph;
	}
}
