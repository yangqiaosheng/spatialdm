package spade.analysis.generators;

import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.plot.CorrelationOneToNComponent;
import spade.analysis.system.Supervisor;
import spade.lib.lang.Language;
import spade.vis.database.AttributeDataPortion;

public class CorrelationOneToNGenerator extends VisGenerator {

	static ResourceBundle res = Language.getTextResource("spade.analysis.generators.Res");

	public CorrelationOneToNGenerator() {
	}

	protected boolean isNumeric(AttributeDataPortion dt, Vector attr) {
		for (int i = 0; i < attr.size(); i++) {
			int z = dt.findAttrByName(dt.getAttributeName((String) attr.elementAt(i)));
			if (!dt.isAttributeNumeric(z))
				return false;
			else {
				continue;
			}
		}
		return true;
	}

	@Override
	public boolean isApplicable(AttributeDataPortion dataTable, Vector attributes) {
		if (!dataTable.hasData())
			return false;
		else if (!isNumeric(dataTable, attributes))
			return false;
		else
			return true;
	}

	/**
	 * Tries to produce a graphic presenting the specified attribute set from the given
	 * AttributeDataPortion. The graphic is displayed by a certain Component. If the graphic cannot be
	 * produced, returns null. The argument methodId is the identifier of the visualization method
	 * which is implemented by this component. The vector attributes contains identifiers of the
	 * attributes to be visualised. The argument supervisor contains a reference to the supervisor
	 * that supports linkage between multiple displays. The argument properties may specify individual
	 * properties for the display to be constructed.
	 *
	 * @param methodId String
	 * @param sup Supervisor
	 * @param dataTable AttributeDataPortion
	 * @param attributes Vector
	 * @param properties Hashtable
	 * @return Component
	 */
	@Override
	public java.awt.Component constructDisplay(String methodId, Supervisor sup, AttributeDataPortion dataTable, Vector attributes, Hashtable properties) {

		if (!isApplicable(dataTable, attributes))
			return null;
		else {
			CorrelationOneToNComponent comp = new CorrelationOneToNComponent(sup, dataTable, attributes);
			comp.setMethodId(methodId);
			comp.setProperties(properties);
			sup.registerTool(comp);
			return comp;
		}
	}

}
