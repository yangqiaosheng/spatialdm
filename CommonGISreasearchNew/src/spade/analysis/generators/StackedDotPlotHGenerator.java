package spade.analysis.generators;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.Panel;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.plot.StackedDotPlot;
import spade.analysis.system.Supervisor;
import spade.lib.basicwin.PlotCanvas;
import spade.lib.basicwin.TImgButton;
import spade.lib.lang.Language;
import spade.vis.database.AttributeDataPortion;

/**
* DotPlotGenerator can construct a dot plot plot presenting a given
* attribute from a given AttributeDataPortion.
*/
public class StackedDotPlotHGenerator extends StackedDotPlotGenerator {
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
		TImgButton b = new TImgButton("/icons/arrow.gif");
		TImgButton b2 = new TImgButton("/icons/shift.gif");
		b.setActionCommand("switch");
		b2.setActionCommand("inverse");
		b.setEnabled(false);
		StackedDotPlot jp = constructStackedDotPlot(methodId, sup, dataTable, attributes, true, properties, b);
		PlotCanvas canvas = new PlotCanvas();
		jp.setCanvas(canvas);
		sup.addPropertyChangeListener(jp);
		b.addActionListener(jp);
		b2.addActionListener(jp);

		//b.setSize(5,5);
		System.out.println("constructed");

		canvas.setContent(jp);
		canvas.setInsets(5, 2, 5, 2);
		canvas.setBackground(Color.lightGray);

		Panel p = new Panel(new BorderLayout());
		Panel s = new Panel(new BorderLayout());
		Panel f = new Panel(new FlowLayout());

		p.add(canvas, "Center");

		int idx = dataTable.getAttrIndex((String) attributes.elementAt(0));
		s.add(new Label(dataTable.getAttributeName(idx)), "West");
		f.add(b);
		f.add(b2);
		s.add(f, "East");
		p.add(s, "North");
		// following text: "Stacked Dot Plot "
//    p.setName(res.getString("stacked_Dot_plot")+jp.getInstanceN());

		return p;
	}
}
