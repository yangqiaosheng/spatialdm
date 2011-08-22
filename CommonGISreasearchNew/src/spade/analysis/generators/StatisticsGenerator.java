package spade.analysis.generators;

import java.awt.Component;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.plot.StatisticsMultiPanel;
import spade.analysis.plot.StatisticsPanel;
import spade.analysis.system.Supervisor;
import spade.lib.lang.Language;
import spade.vis.database.AttributeDataPortion;

/**
* StatisticsGenerator can construct a Statistics Panel for a given
* attribute from a given AttributeDataPortion.
*/

public class StatisticsGenerator extends VisGenerator {

	static ResourceBundle res = Language.getTextResource("spade.analysis.generators.Res");
	protected AttributeDataPortion dataTable = null;
	protected Supervisor supervisor = null;

	/*
	* Checks applicability of the method to selected data
	 */
	@Override
	public boolean isApplicable(AttributeDataPortion dataTable, Vector attributes) {
		if (dataTable == null) {
			// following text: "No data source provided!"
			err = res.getString("No_data_source");
			return false;
		}
		if (!dataTable.hasData()) {
			// following text: "No data in the data source!"
			err = res.getString("No_data_in_the_data");
			return false;
		}
		if (attributes == null || attributes.size() < 1) {
			// following text: "No attributes specified!"
			err = res.getString("No_attributes");
			return false;
		}
		int nattr = attributes.size();
		/*
		if (nattr>1) {
		  err="Too many attributes! Statistics panel can present only single attribute!";
		  return false;
		} */
		for (int i = 0; i < nattr; i++) {
			int idx = dataTable.getAttrIndex((String) attributes.elementAt(0));
			if (idx < 0) {
				// following text: "The attribute "+attributes.elementAt(0)+" is not found in the data source!"
				err = res.getString("The_attribute") + attributes.elementAt(0) + res.getString("is_not_found_in_the");
				return false;
			}
			if (!dataTable.isAttributeNumeric(idx)) {
				// following text: "The attribute "+attributes.elementAt(0)+" is not numeric!"
				err = res.getString("The_attribute") + attributes.elementAt(0) + res.getString("is_not_numeric_");
				return false;
			}
		}
		return true;
	}

	/**
	* Tries to produce a statistics panel presenting the specified attribute
	* from the given AttributeDataPortion. Returns a Panel including statistics.
	* If a statistics panel cannot be produced, returns null.
	* The vector attributes contains identifiers of the attributes to be
	* visualised.
	* The argument supervisor contains a reference to the supervisor that
	* supports linkage between multiple displays.
	* The argument properties may specify individual properties for the
	* display to be constructed.
	*/
	@Override
	public Component constructDisplay(String methodId, Supervisor supervisor, AttributeDataPortion dataTable, Vector attributes, Hashtable properties) {
		if (!isApplicable(dataTable, attributes))
			return null;
		this.supervisor = supervisor;
		this.dataTable = dataTable;
		if (attributes.size() == 1) {
			StatisticsPanel sps = new StatisticsPanel(supervisor, (String) attributes.elementAt(0), dataTable);
			// following text: "(statistics)"
//      sps.setName(dataTable.getAttributeName(dataTable.getAttrIndex((String)attributes.elementAt(0)))+res.getString("_statistics_"));
			sps.setMethodId(methodId);
			sps.setProperties(properties);
			supervisor.registerDataDisplayer(sps);
			supervisor.registerTool(sps);
			return sps;
		} else {
			StatisticsMultiPanel smp = new StatisticsMultiPanel(supervisor, dataTable, attributes);
			smp.setMethodId(methodId);
			smp.setProperties(properties);
			supervisor.registerDataDisplayer(smp);
			supervisor.registerTool(smp);
			return smp;
		}
	}

}
