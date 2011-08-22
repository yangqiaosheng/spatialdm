package spade.analysis.generators;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Label;
import java.awt.Panel;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.plot.DotPlot;
import spade.analysis.system.Supervisor;
import spade.lib.basicwin.PlotCanvas;
import spade.lib.lang.Language;
import spade.vis.database.AttributeDataPortion;

/**
* DotPlotGenerator can construct a dot plot plot presenting a given
* attribute from a given AttributeDataPortion.
*/

public class DotPlotVGenerator extends DotPlotGenerator {
	static ResourceBundle res = Language.getTextResource("spade.analysis.generators.Res");

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
	@Override
	public Component constructDisplay(String methodId, Supervisor sup, AttributeDataPortion dataTable, Vector attributes, Hashtable properties) {
		if (!isApplicable(dataTable, attributes))
			return null;
		DotPlot dp = constructDotPlot(methodId, sup, dataTable, attributes, false, properties);
		PlotCanvas canvas = new PlotCanvas();
		dp.setCanvas(canvas);
		canvas.setContent(dp);
		canvas.setInsets(2, 5, 2, 5);

		Panel p = new Panel(new BorderLayout());
		p.add(canvas, "Center");
		int idx = dataTable.getAttrIndex((String) attributes.elementAt(0));
		p.add(new Label(dataTable.getAttributeName(idx)), "North");
		// following text: "Dot plot "
//    p.setName(res.getString("Dot_plot")+dp.getInstanceN());
		p.setName(dp.getName());
		return p;
	}
}
