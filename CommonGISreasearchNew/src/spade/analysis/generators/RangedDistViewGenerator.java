package spade.analysis.generators;

import java.awt.Component;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.classification.QualitativeClassifier;
import spade.analysis.classification.RangedDistPanel;
import spade.analysis.system.Supervisor;
import spade.lib.help.Helper;
import spade.lib.lang.Language;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.AttributeTypes;

/**
* Generates a ranged frequency histogram display for a qualitative attribute
* changes: hdz, 2004.004 applicable for few numeric values
*/
public class RangedDistViewGenerator extends VisGenerator {
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
			//following text:"Too many attributes! This graph can present only single attribute!"
			err = res.getString("Too_many_attributes_A");
			return false;
		}
		int idx = dataTable.getAttrIndex((String) attributes.elementAt(0));
		if (idx < 0) {
			//following text:"The attribute "+attributes.elementAt(0)+" is not found in the data source!"
			err = res.getString("The_attribute") + attributes.elementAt(0) + res.getString("is_not_found_in_the");
			return false;
		}

		//hdz changed
		//if (!AttributeTypes.isNominalType(dataTable.getAttributeType(idx))) {
		char at = dataTable.getAttributeType(idx);
		boolean testNumerical = dataTable.isValuesCountBelow(attributes, Helper.getMaxNumValuesForQualitativeVis());

		if (!((testNumerical && AttributeTypes.isNumericType(at)) || (AttributeTypes.isNominalType(at)))) {
			//following text:"The attribute "+attributes.elementAt(0)+" is not qualitative!"
			err = res.getString("The_attribute") + attributes.elementAt(0) + res.getString("is_not_qualitative_");
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
		//hdz added
		//if (!isApplicable(dataTable,attributes)) return null;
		if (!isApplicable(dataTable, attributes))
			return null;

		String attrId = (String) attributes.elementAt(0);
		QualitativeClassifier qClassifier = new QualitativeClassifier(dataTable, attrId);
		qClassifier.setMethodId("qualitative_colour");
		boolean ranged = false;
		try {
			ranged = new Boolean((String) properties.get("ranged")).booleanValue();
		} catch (Exception ex) {
		}
		RangedDistPanel p = new RangedDistPanel(sup, qClassifier, ranged);
//    p.setName(dataTable.getAttributeName(attrId)+": "+res.getString("ranged_distrib"));
		p.getGraph().setMethodId(methodId);
		p.getGraph().setProperties(properties);
		sup.registerTool(p.getGraph());
		p.setName(p.getGraph().getName());
		return p;
	}
}
