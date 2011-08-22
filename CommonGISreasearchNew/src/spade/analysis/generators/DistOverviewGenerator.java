package spade.analysis.generators;

import java.awt.Component;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.plot.DistOverviewComponent;
import spade.analysis.system.Supervisor;
import spade.lib.lang.Language;
import spade.vis.database.AttributeDataPortion;

public class DistOverviewGenerator extends VisGenerator {

	static ResourceBundle res = Language.getTextResource("spade.analysis.generators.Res");

	public DistOverviewGenerator() {
	}

	protected boolean isNumeric(AttributeDataPortion dt, Vector attr) {
		for (int i = 0; i < attr.size(); i++)
			if (!dt.isAttributeNumeric(dt.getAttrIndex((String) attr.elementAt(i))))
				return false;
		return true;
	}

	@Override
	public boolean isApplicable(AttributeDataPortion dataTable, Vector attributes) {
		if (!dataTable.hasData() || !isNumeric(dataTable, attributes))
			return false;
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
		return new DistOverviewComponent(sup, dataTable, attributes);
	}
}
