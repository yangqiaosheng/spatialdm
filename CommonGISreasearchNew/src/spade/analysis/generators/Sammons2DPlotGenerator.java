package spade.analysis.generators;

import java.awt.Component;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.plot.Sammons2DPlot;
import spade.analysis.plot.Sammons2DPlotPanel;
import spade.analysis.system.Supervisor;
import spade.lib.lang.Language;
import spade.vis.database.AttributeDataPortion;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Apr 7, 2010
 * Time: 4:15:54 PM
 * Generates a scatterplot-like display of a 2D projection of
 * multidimensional data obtained with the use of the Sammon#s projection algorithm.
 */
public class Sammons2DPlotGenerator extends VisGenerator {
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
			// following text:"No data in the data source!"
			err = res.getString("No_data_in_the_data");
			return false;
		}
		if (attributes == null || attributes.size() < 1) {
			// following text:"No attributes specified!"
			err = res.getString("No_attributes");
			return false;
		}
		int nattr = attributes.size();
		if (nattr < 3) {
			// following text:"Too few attributes! A PCP can present at least two attributes!"
			err = "Too few attributes! Must be at least 3!";
			return false;
		}
		for (int i = 0; i < nattr; i++) {
			int idx = dataTable.getAttrIndex((String) attributes.elementAt(i));
			if (idx < 0) {
				// following text:"The attribute "+attributes.elementAt(i)+" is not found in the data source!"
				err = res.getString("The_attribute") + attributes.elementAt(i) + res.getString("is_not_found_in_the");
				return false;
			}
			if (!dataTable.isAttributeNumeric(idx) && !dataTable.isAttributeTemporal(idx)) {
				// following text:"The attribute "+attributes.elementAt(i)+" is not numeric!"
				err = res.getString("The_attribute") + attributes.elementAt(i) + res.getString("is_not_numeric_");
				return false;
			}
		}
		return true;
	}

	/**
	* Tries to produce a Sammon's projection plot presenting the specified attribute set
	* from the given AttributeDataPortion.
	* If the display cannot be produced, returns null.
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
		Sammons2DPlot sp = new Sammons2DPlot(true, true, sup, sup);
		sp.setMethodId(methodId);
		sp.setDataSource(dataTable);
		int colNs[] = new int[attributes.size()];
		for (int i = 0; i < attributes.size(); i++) {
			colNs[i] = dataTable.getAttrIndex((String) attributes.elementAt(i));
		}
		if (!sp.setup(colNs))
			return null;
		sp.setProperties(properties);
		sp.checkWhatSelected();
		sup.registerTool(sp);
		Sammons2DPlotPanel p = new Sammons2DPlotPanel(sp);
		return p;
	}
}
