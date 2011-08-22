package spade.analysis.generators;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Choice;
import java.awt.Component;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.classification.BroadcastClassesCP;
import spade.analysis.plot.FNReorder;
import spade.analysis.plot.PCPlot;
import spade.analysis.system.Supervisor;
import spade.analysis.transform.AttributeTransformer;
import spade.analysis.transform.TransformRestorer;
import spade.analysis.transform.TransformerGenerator;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.FoldablePanel;
import spade.lib.basicwin.PlotCanvas;
import spade.lib.basicwin.RowLayout;
import spade.lib.basicwin.SplitLayout;
import spade.lib.lang.Language;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.DataTable;
import spade.vis.spec.ToolSpec;
import spade.vis.spec.TransformSequenceSpec;

/**
* PCPGenerator can construct a parallel coordinates plot presenting a given
* set of attributes (two and more) from a given AttributeDataPortion.
*/

public class PCPGenerator extends VisGenerator implements ItemListener {
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
		if (nattr < 2) {
			// following text:"Too few attributes! A PCP can present at least two attributes!"
			err = res.getString("Too_few_attributes_A1");
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
				if (dataTable instanceof DataTable) {
					Attribute attr = ((DataTable) dataTable).getAttribute(idx);
					if (!attr.isClassification()) {
						// following text:"The attribute "+attributes.elementAt(i)+" is not numeric!"
						err = res.getString("The_attribute") + attributes.elementAt(i) + res.getString("is_not_numeric_");
						return false;
					}
				}
			}
		}
		return true;
	}

	protected PCPlot pcp = null;

	public PCPlot getPCP() {
		return pcp;
	}

	protected FNReorder pcpl = null;

	public FNReorder getFNReorder() {
		return pcpl;
	}

	protected Checkbox cbLines = null, cbAggrs = null, cbFlows = null, cbShapes = null, cbHreact = null, cbHproduce = null, cbUseAlpha = null, cbQuantiles[] = null;
	protected Choice chN = null;
	protected Panel pQuantiles = null;

	/**
	* Tries to produce a PCP presenting the specified attribute set
	* from the given AttributeDataPortion. Returns a Panel including a PCP.
	* If a PCP cannot be produced, returns null.
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

		Panel p = new Panel();
		p.setLayout(new BorderLayout());
		Panel pp = new Panel();
		p.add("Center", pp);

		SplitLayout spl = new SplitLayout(pp, SplitLayout.VERT);
		pp.setLayout(spl);

		pcp = new PCPlot(true, true, sup, sup);
		pcp.setMethodId(methodId);
		pcp.setDataSource(dataTable);
		pcp.setIsZoomable(true);

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
				pcp.setAttributeTransformer(aTrans, true);
			} else {
				aTrans = null;
			}
		}
		int fn[] = new int[attributes.size()];
		for (int i = 0; i < fn.length; i++) {
			fn[i] = dataTable.getAttrIndex((String) attributes.elementAt(i));
		}
		pcp.setup(fn);

		pcp.checkWhatSelected();
		PlotCanvas canvas_pcp = new PlotCanvas();
		pcp.setCanvas(canvas_pcp);
		canvas_pcp.setContent(pcp);
		spl.addComponent(canvas_pcp, 0.8f);

		PlotCanvas canvas_pcpl = new PlotCanvas();
		pcpl = new FNReorder(dataTable, canvas_pcpl, fn, -1, -1);
		pcpl.addActionListener(pcp);
		pcpl.setCanvas(canvas_pcpl);
		canvas_pcpl.setContent(pcpl);
		spl.addComponent(canvas_pcpl, 0.2f);

		Choice c = new Choice();
		c.addItemListener(pcp);
		// following text:"Individual Min and Max
		c.add(res.getString("Individual_Min_and"));
		// following text:"Common Min and Max"
		c.add(res.getString("Common_Min_and_Max"));
		// following text:"Selected object(s)"
		c.add(res.getString("Selected_object_s_"));
		// following text:"Min-Max, Medians and Quartiles"
		c.add(res.getString("Min_Max_Medians_and"));
		// following text:"Medians and Quartiles"
		c.add(res.getString("Medians_and_Quartiles"));
		// following text:"Means and Std.deviations"
		c.add(res.getString("Means_and_Std"));
		// following text:"Standard deviation"
		c.add(res.getString("Standard_deviation"));

		try {
			c.select(Integer.parseInt((String) properties.get("alignment")));
		} catch (Exception ex) {
		}

		if (sup != null && sup.getHighlighter(dataTable.getEntitySetIdentifier()) != null) {
			Vector v = sup.getHighlighter(dataTable.getEntitySetIdentifier()).getSelectedObjects();
			if (v != null && v.size() > 0) {
				c.select(2);
			}
		}

		pp = createControlPanel(pcp, c, sup, dataTable);
		if (aTrans != null) {
			Panel bottom = new Panel(new ColumnLayout());
			bottom.add(aTrans.getUI());
			bottom.add(pp);
			p.add(bottom, BorderLayout.SOUTH);
		} else {
			p.add(pp, BorderLayout.SOUTH);
		}
		// following text:"Parallel coordinates "
//    p.setName(res.getString("Parallel_coordinates")+pcp.getInstanceN());
		pcp.setProperties(properties);
		sup.registerTool(pcp);
		p.setName(pcp.getName());
		return p;
	}

	/*
	* Special variant for similarity calculations
	*/
	public Component constructDisplaySCalc(Supervisor sup, AttributeDataPortion dataTable, Vector attributes, Vector referenceObjects) {
		if (!isApplicable(dataTable, attributes))
			return null;

		Panel p = new Panel();
		p.setLayout(new BorderLayout());
		Panel pp = new Panel();
		p.add("Center", pp);

		SplitLayout spl = new SplitLayout(pp, SplitLayout.VERT);
		pp.setLayout(spl);

		pcp = new PCPlot(true, true, sup, sup);
		pcp.setDataSource(dataTable);
		pcp.setIsZoomable(true);
		int fn[] = new int[attributes.size()];
		for (int i = 0; i < fn.length; i++) {
			fn[i] = dataTable.getAttrIndex((String) attributes.elementAt(i));
		}
		pcp.setSpecialMode(PCPlot.similarityMode);
		pcp.setup(fn, referenceObjects);
		pcp.checkWhatSelected();
		PlotCanvas canvas_pcp = new PlotCanvas();
		pcp.setCanvas(canvas_pcp);
		canvas_pcp.setContent(pcp);
		spl.addComponent(canvas_pcp, 0.8f);

		PlotCanvas canvas_pcpl = new PlotCanvas();
		FNReorder pcpl = new FNReorder(dataTable, canvas_pcpl, fn, -1, -1);
		pcpl.addActionListener(pcp);
		canvas_pcpl.setContent(pcpl);
		spl.addComponent(canvas_pcpl, 0.2f);

		Choice c = new Choice();
		c.addItemListener(pcp);
		// following text:"Reference object(s)"
		c.add(res.getString("Reference_object_s_"));
		c.setEnabled(false);

		constructBCCP(pp, pcp, sup, dataTable);

		p.add(createControlPanel(pcp, c, sup, dataTable), BorderLayout.SOUTH);
		// following text:"Parallel coordinates "
		p.setName(res.getString("Parallel_coordinates") + pcp.getInstanceN());
		return p;
	}

	/*
	* Special variant for dominant calculations
	*/
	public Object[] constructDisplayDCalc(Supervisor sup, AttributeDataPortion dataTable, Vector attributes) {
		if (!isApplicable(dataTable, attributes))
			return null;

		Object results[] = new Object[4];

		Panel p = new Panel();
		p.setLayout(new BorderLayout());
		Panel pp = new Panel();
		p.add("Center", pp);

		SplitLayout spl = new SplitLayout(pp, SplitLayout.VERT);
		pp.setLayout(spl);

		pcp = new PCPlot(true, true, sup, sup);
		pcp.setDataSource(dataTable);
		pcp.setIsZoomable(true);
		int fn[] = new int[attributes.size()];
		for (int i = 0; i < fn.length; i++) {
			fn[i] = dataTable.getAttrIndex((String) attributes.elementAt(i));
		}
		pcp.setSpecialMode(PCPlot.dominantAttrMode);
		pcp.setup(fn);
		pcp.checkWhatSelected();
		PlotCanvas canvas_pcp = new PlotCanvas();
		pcp.setCanvas(canvas_pcp);
		canvas_pcp.setContent(pcp);
		spl.addComponent(canvas_pcp, 0.8f);

		PlotCanvas canvas_pcpl = new PlotCanvas();
		FNReorder pcpl = new FNReorder(dataTable, canvas_pcpl, fn, -1, -1);
		pcpl.addActionListener(pcp);
		canvas_pcpl.setContent(pcpl);
		spl.addComponent(canvas_pcpl, 0.2f);

		Choice c = new Choice();
		c.addItemListener(pcp);
		// following text:"Common Min and Max"
		c.add(res.getString("Common_Min_and_Max"));
		// following text:"Individual Min and Max"
		c.add(res.getString("Individual_Min_and"));
		// following text:"Min-Max, Medians and Quartiles"
		c.add(res.getString("Min_Max_Medians_and"));
		// following text:"Means and Std.deviations"
		c.add(res.getString("Means_and_Std"));
		c.select(1);
		c.setEnabled(false);

		p.add(createControlPanel(pcp, c, sup, dataTable), BorderLayout.SOUTH);

		results[0] = p;
		results[1] = c;
		results[2] = pcp;
		results[3] = pcpl;
		return results;
	}

	/*
	* Special variant for similarity classification
	*/
	public Component constructDisplaySCCalc(Supervisor sup, AttributeDataPortion dataTable, Vector attributes, Vector referenceObjects1, Vector referenceObjects2) {
		if (!isApplicable(dataTable, attributes))
			return null;

		Panel p = new Panel();
		p.setLayout(new BorderLayout());
		Panel pp = new Panel();
		p.add("Center", pp);

		SplitLayout spl = new SplitLayout(pp, SplitLayout.VERT);
		pp.setLayout(spl);

		pcp = new PCPlot(true, true, sup, sup);
		pcp.setDataSource(dataTable);
		pcp.setIsZoomable(true);
		int fn[] = new int[attributes.size()];
		for (int i = 0; i < fn.length; i++) {
			fn[i] = dataTable.getAttrIndex((String) attributes.elementAt(i));
		}
		pcp.setSpecialMode(PCPlot.similarityClassMode);
		pcp.setup(fn, referenceObjects1, referenceObjects2);
		pcp.checkWhatSelected();
		PlotCanvas canvas_pcp = new PlotCanvas();
		pcp.setCanvas(canvas_pcp);
		canvas_pcp.setContent(pcp);
		spl.addComponent(canvas_pcp, 0.8f);

		PlotCanvas canvas_pcpl = new PlotCanvas();
		FNReorder pcpl = new FNReorder(dataTable, canvas_pcpl, fn, -1, -1);
		pcpl.addActionListener(pcp);
		canvas_pcpl.setContent(pcpl);
		spl.addComponent(canvas_pcpl, 0.2f);

		Choice c = new Choice();
		c.addItemListener(pcp);
		// following text:"Samples of the classes"
		c.add(res.getString("Samples_of_the"));
		c.setEnabled(false);

		p.add(createControlPanel(pcp, c, sup, dataTable), BorderLayout.SOUTH);
		// following text:"Parallel coordinates "
		p.setName(res.getString("Parallel_coordinates") + pcp.getInstanceN());
		return p;
	}

	/**
	* Tries to produce a parallel coordinate plot according to the given specification.
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

	/*
	* Special variant for multicriteria evaluation
	*/
	public Object[] constructDisplayIPCalc(Supervisor sup, AttributeDataPortion dataTable, Vector attributes, float weights[]) {
		if (!isApplicable(dataTable, attributes))
			return null;

		Object results[] = new Object[4];

		Panel p = new Panel();
		p.setLayout(new BorderLayout());
		Panel pp = new Panel();
		p.add("Center", pp);

		SplitLayout spl = new SplitLayout(pp, SplitLayout.VERT);
		pp.setLayout(spl);

		pcp = new PCPlot(true, true, sup, sup);
		pcp.setDataSource(dataTable);
		pcp.setIsZoomable(true);
		int fn[] = new int[attributes.size()];
		for (int i = 0; i < fn.length; i++) {
			fn[i] = dataTable.getAttrIndex((String) attributes.elementAt(i));
		}
		//pcp.setSpecialMode(PCPlot.criteriaMode);
		//pcp.setAlignmentMode(PCPlot.AlignICwithWeights);
		pcp.setup(fn, weights);
		pcp.checkWhatSelected();
		PlotCanvas canvas_pcp = new PlotCanvas();
		pcp.setCanvas(canvas_pcp);
		canvas_pcp.setContent(pcp);
		spl.addComponent(canvas_pcp, 0.8f);

		PlotCanvas canvas_pcpl = new PlotCanvas();
		FNReorder pcpl = new FNReorder(dataTable, canvas_pcpl, fn, -1, -1);
		pcpl.addActionListener(pcp);
		canvas_pcpl.setContent(pcpl);
		spl.addComponent(canvas_pcpl, 0.2f);

		Choice c = new Choice();
		c.addItemListener(pcp);
		// following text:"Right-aligned axes (weighted)"
		c.add(res.getString("Right_aligned_axes"));
		// following text:"Right-aligned axes (non-weighted)"
		c.add(res.getString("Right_aligned_axes1"));
		// following text:"Center-aligned axes (weighted)"
		c.add(res.getString("Center_aligned_axes"));
		// following text:"Center-aligned axes (non-weighted)"
		c.add(res.getString("Center_aligned_axes1"));
		// following text:"Left-aligned axes (weighted)"
		c.add(res.getString("Left_aligned_axes"));
		// following text:"Left-aligned axes (non-weighted)"
		c.add(res.getString("Left_aligned_axes_non"));
		c.select(0);
		//c.setEnabled(false);

		p.add(createControlPanel(pcp, c, sup, dataTable), BorderLayout.SOUTH);
		results[0] = p;
		results[1] = c;
		results[2] = pcp;
		results[3] = pcpl;
		return results;
	}

	protected Panel createControlPanel(PCPlot pcp, Choice ch, Supervisor sup, AttributeDataPortion dataTable) {
		Panel p = new Panel();
		p.setLayout(new ColumnLayout());
		Panel pp = new Panel();
		pp.setLayout(new RowLayout(0, 0));
		pp.add(new Label("Show:"));
		pp.add(cbLines = new Checkbox("Lines", true));
		pp.add(cbAggrs = new Checkbox("Aggregates:", false));
		CheckboxGroup cbg = new CheckboxGroup();
		pp.add(cbFlows = new Checkbox("Flows", cbg, true));
		pp.add(cbShapes = new Checkbox("Shapes", cbg, false));
		pp.add(new Label(" | "));
		pp.add(new Label("Highlighting:"));
		pp.add(cbHreact = new Checkbox("react", true));
		pp.add(cbHproduce = new Checkbox("produce", true));
		p.add(pp);
		pp = new Panel();
		pp.setLayout(new RowLayout(0, 0));
		pp.add(new Label("Quantiles:"));
		pp.add(cbUseAlpha = new Checkbox("transparent", false));
		pp.add(pQuantiles = new Panel(new RowLayout(0, 0)));
		pQuantiles.add(chN = new Choice());
		for (int i = 0; i < PCPlot.maxNQuantiles; i++) {
			chN.add(String.valueOf(1 + i));
		}
		chN.select(4);
		chN.addItemListener(this);
		cbQuantiles = new Checkbox[5];
		for (int i = 0; i < cbQuantiles.length; i++) {
			pQuantiles.add(cbQuantiles[i] = new Checkbox(String.valueOf(1 + i), true));
		}
		pcp.setIndividualCheckboxes(cbLines, cbAggrs, cbFlows, cbShapes, cbHreact, cbHproduce, cbUseAlpha);
		pcp.setQuantileCheckboxes(cbQuantiles);
		p.add(pp);
		pp = new Panel();
		pp.setLayout(new BorderLayout());
		pp.add("West", new Label(res.getString("Alignment_")));
		pp.add("Center", ch);
		/* Using alpha blending for lines. Eliminated because works extremely slow !
		c=new Choice();
		c.addItemListener((ItemListener)pcp);
		pp.add("East",c);
		c.add("alpha=32"); c.add("alpha=64"); c.add("alpha=96");
		c.add("alpha=128"); c.add("alpha=192"); c.add("alpha=255");
		c.select(5);
		*/
		// Calculating counts for flows f=through quantiles
		/*
		Button b=new Button("Flow counts");
		b.addActionListener(pcp);
		pp.add(b,BorderLayout.EAST);
		*/
		p.add(pp);
		constructBCCP(p, pcp, sup, dataTable);
		return new FoldablePanel(p, new Label("Parallel coordinates control panel"));
	}

	protected void constructBCCP(Panel p, PCPlot pcp, Supervisor sup, AttributeDataPortion dataTable) {
		BroadcastClassesCP pClassesLines = null, pClassesFlows = null;
		Panel ppp = new Panel(new BorderLayout()), pppp = null;
		p.add(ppp);
		pppp = new Panel();
		pppp.setLayout(new RowLayout(3, 0));
		pppp.add(pClassesLines = new BroadcastClassesCP(dataTable.getEntitySetIdentifier(), "Classes (lines):", "eventShowClassesLines", true, true, false));
		ppp.add(pppp, BorderLayout.NORTH);
		pppp = new Panel();
		pppp.setLayout(new RowLayout(3, 0));
		pppp.add(pClassesFlows = new BroadcastClassesCP(dataTable.getEntitySetIdentifier(), "Classes (flows):", "eventShowClassesFlows", false, false, true));
		ppp.add(pppp, BorderLayout.SOUTH);
		pClassesLines.addPropertyChangeListener(pcp);
		pClassesLines.setSupervisor(sup);
		pClassesFlows.addPropertyChangeListener(pcp);
		pClassesFlows.setSupervisor(sup);
	}

	@Override
	public void itemStateChanged(ItemEvent ie) {
		if (ie.getSource() instanceof Choice && ie.getSource().equals(chN)) {
			if (cbQuantiles != null) {
				for (int i = cbQuantiles.length - 1; i >= 0; i--) {
					pQuantiles.remove(cbQuantiles[i]);
				}
			}
			cbQuantiles = new Checkbox[1 + chN.getSelectedIndex()];
			for (int i = 0; i < cbQuantiles.length; i++) {
				pQuantiles.add(cbQuantiles[i] = new Checkbox(String.valueOf(1 + i), true));
			}
			CManager.validateAll(pQuantiles);
			pcp.setQuantileCheckboxes(cbQuantiles);
			pcp.redraw();
		}
	}

}
