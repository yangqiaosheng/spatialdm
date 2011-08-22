package spade.analysis.generators;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Label;
import java.awt.Panel;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.plot.ScatterPlot;
import spade.analysis.plot.ScatterPlotWithFocusers;
import spade.analysis.system.Supervisor;
import spade.analysis.transform.AttributeTransformer;
import spade.analysis.transform.TransformRestorer;
import spade.analysis.transform.TransformerGenerator;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.PlotCanvas;
import spade.lib.lang.Language;
import spade.vis.database.AttributeDataPortion;
import spade.vis.spec.ToolSpec;
import spade.vis.spec.TransformSequenceSpec;

/**
* ScatterplotGenerator can construct a scatterplot presenting a given pair of
* attributes from a given AttributeDataPortion.
*/

public class ScatterplotGenerator extends VisGenerator {
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
		if (attributes == null || attributes.size() < 1) {
			// following Text: "No attributes specified!
			err = res.getString("No_attributes");
			return false;
		}
		if (attributes.size() != 2) {
			// following Text: "Wrong number of attributes! A scatterplot can present exactly two attributes!"
			err = res.getString("Wrong_number_of");
			return false;
		}
		int fn1 = dataTable.getAttrIndex((String) attributes.elementAt(0));
		if (fn1 < 0) {
			// following Text: "The attribute "+attributes.elementAt(0)+" is not found in the data source!"
			err = res.getString("The_attribute") + attributes.elementAt(0) + res.getString("is_not_found_in_the");
			return false;
		}
		if (!dataTable.isAttributeNumeric(fn1) && !dataTable.isAttributeTemporal(fn1)) {
			// following Text: "The attribute "+attributes.elementAt(0)+" is not numeric!
			err = res.getString("The_attribute") + attributes.elementAt(0) + res.getString("is_not_numeric_");
			return false;
		}
		int fn2 = dataTable.getAttrIndex((String) attributes.elementAt(1));
		if (fn2 < 0) {
			// following Text: "The attribute "+attributes.elementAt(1)+" is not found in the data source!"
			err = res.getString("The_attribute") + attributes.elementAt(1) + res.getString("is_not_found_in_the");
			return false;
		}
		if (!dataTable.isAttributeNumeric(fn2) && !dataTable.isAttributeTemporal(fn2)) {
			// following Text: "The attribute "+attributes.elementAt(1)+" is not numeric!"
			err = res.getString("The_attribute") + attributes.elementAt(1) + res.getString("is_not_numeric_");
			return false;
		}
		return true;
	}

	/**
	* Tries to produce a scatterplot presenting the specified attribute pair from
	* the given AttributeDataPortion. Returns a Panel including a ScatterPlot.
	* If a scatterplot cannot be produced, returns null.
	* The vector attributes contains identifiers of the attributes to be
	* visualised.
	* The argument supervisor contains a reference to the supervisor that
	* supports linkage between multiple displays.
	* The argument properties may specify individual properties for the
	* display to be constructed.
	*/
	@Override
	public Component constructDisplay(String methodId, Supervisor sup, AttributeDataPortion dataTable, Vector attributes, Hashtable properties) {
		return constructDisplay(methodId, sup, dataTable, attributes, properties, null);
	}

	/**
	* Same as above, but tries to reconstruct data transformers according
	* to the given specifications.
	*/
	protected Component constructDisplay(String methodId, Supervisor sup, AttributeDataPortion dataTable, Vector attributes, Hashtable properties, TransformSequenceSpec transformSpecs) {
		if (!isApplicable(dataTable, attributes))
			return null;
		ScatterPlot sp = new ScatterPlotWithFocusers(true, true, sup, sup);
		sp.setMethodId(methodId);
		sp.setDataSource(dataTable);
		int fn1 = dataTable.getAttrIndex((String) attributes.elementAt(0)), fn2 = dataTable.getAttrIndex((String) attributes.elementAt(1));
		sp.setFieldNumbers(fn1, fn2);
		AttributeTransformer aTrans = null;
		if (transformSpecs == null) {
			aTrans = TransformerGenerator.makeTransformer("math");
			if (aTrans != null) {
				aTrans.setAllowIndividualTransformation(true);
			}
		} else {
			aTrans = TransformRestorer.restoreTransformerSequence(transformSpecs);
		}
		if (aTrans != null) {
			aTrans.setDataTable(dataTable);
			aTrans.setAttributes(attributes);
			if (aTrans.isValid()) {
				if (transformSpecs != null) {
					aTrans.doTransformation();
				}
				sp.setAttributeTransformer(aTrans, true);
			} else {
				aTrans = null;
			}
		}
		sp.setIsZoomable(true);
		sp.setup();
		sp.checkWhatSelected();
		PlotCanvas canvas = new PlotCanvas();
		sp.setCanvas(canvas);
		canvas.setContent(sp);
		Panel p = new Panel(new BorderLayout());
		p.add(canvas, "Center");
		p.add(new Label("Y: " + dataTable.getAttributeName(fn2), Label.LEFT), "North");
		Label l = new Label("X: " + dataTable.getAttributeName(fn1), Label.RIGHT);
		if (aTrans == null) {
			p.add(l, "South");
		} else {
			Panel pp = new Panel(new ColumnLayout());
			pp.add(l);
			pp.add(new Line(false));
			pp.add(aTrans.getUI());
			p.add(pp, "South");
		}
		// following Text: "Scatterplot "
//    p.setName(res.getString("Scatterplot")+sp.getInstanceN());
		sp.setProperties(properties);
		sup.registerTool(sp);
		p.setName(sp.getName());
		return p;
	}

	/**
	* Tries to produce a scatterplot according to the given specification.
	* If the graphic cannot be produced, returns null. 
	* The argument supervisor contains a reference to the supervisor that
	* supports linkage between multiple displays.
	*/
	@Override
	public Component constructDisplay(ToolSpec spec, Supervisor sup, AttributeDataPortion dataTable) {
		if (spec == null)
			return null;
		return constructDisplay(spec.methodId, sup, dataTable, spec.attributes, spec.properties, spec.transformSeqSpec);
	}
}
