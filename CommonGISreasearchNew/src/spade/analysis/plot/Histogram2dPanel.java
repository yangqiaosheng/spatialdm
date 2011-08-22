package spade.analysis.plot;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Choice;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Vector;

import spade.analysis.generators.PlotGeneratorsDescriptor;
import spade.analysis.system.Supervisor;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.FoldablePanel;
import spade.lib.basicwin.Line;
import spade.vis.database.AttributeDataPortion;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: Apr 7, 2010
 * Time: 5:50:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class Histogram2dPanel extends Panel implements ItemListener, ActionListener {

	/**
	* Identifier of this visualization method
	*/
	protected String methodId = null;

	/**
	* Used to generate unique identifiers of instances
	*/
	protected static int nInstances = 0;
	protected int instanceN = 0;

	public static String renderingStyles[] = { "bubbles", "rectangles", "vertical bars", "horizontal bars" };

	Histogram2dCanvas hc = null;
	TextField tfN1 = null, tfN2 = null, tfF1 = null, tfF2 = null;
	Checkbox cbQuery = null, cbBuffer = null, cbNBins = null, cbRoundingBins = null, cbCumulative = null, cbCumX = null, cbCumY = null, cbCumFromMin = null, cbCumToMax = null, cbRendering[] = null, cbSquareCells = null;
	Choice chOper = null, chAttr = null;

	public Histogram2dPanel(AttributeDataPortion table, Vector attributes, Supervisor supervisor) {
		super();
		instanceN = ++nInstances;
		setLayout(new BorderLayout());
		// Canvas
		hc = new Histogram2dCanvas(table, attributes, supervisor);
		add(hc, BorderLayout.CENTER);

		// Controls
		Panel cp = new Panel(new BorderLayout()), cpBins = new Panel(new BorderLayout()), cpAggrAndRender = new Panel(new BorderLayout()), cpAggregation = new Panel(new ColumnLayout()), cpRendering = new Panel(new ColumnLayout()), cpAggrOper = new Panel(
				new BorderLayout());
		FoldablePanel fp = new FoldablePanel(cp, new Label("Control panel"));
		add(fp, BorderLayout.SOUTH);
		cp.add(cpBins, BorderLayout.CENTER);
		Panel p = new Panel(new BorderLayout());
		p.add(new Line(true), BorderLayout.WEST);
		p.add(cpAggrAndRender, BorderLayout.EAST);
		cp.add(p, BorderLayout.EAST);
		cp.add(cpAggrOper, BorderLayout.SOUTH);
		cpAggrAndRender.add(cpAggregation, BorderLayout.WEST);
		cpAggrAndRender.add(cpRendering, BorderLayout.EAST);
		cpAggrAndRender.add(new Line(true), BorderLayout.CENTER);

		CheckboxGroup cbg = new CheckboxGroup();
		cbNBins = new Checkbox("N bins:", cbg, false);
		cbNBins.addItemListener(this);
		cbRoundingBins = new Checkbox("Rounding to:", cbg, true);
		cbRoundingBins.addItemListener(this);
		tfN1 = new TextField("3", 3);
		tfN1.addActionListener(this);
		tfN2 = new TextField("3", 3);
		tfN2.addActionListener(this);
		cbBuffer = new Checkbox("make buffer around min/max", true);
		cbBuffer.addItemListener(this);
		cbQuery = new Checkbox("use attribute ranges after query");
		cbQuery.addItemListener(this);
		tfF1 = new TextField("1", 3);
		tfF1.addActionListener(this);
		tfF2 = new TextField("1", 3);
		tfF2.addActionListener(this);

		p = new Panel(new ColumnLayout());
		p.add(new Line(false));
		p.add(new Label("Binning settings", Label.CENTER));
		p.add(new Line(false));
		cpBins.add(p, BorderLayout.NORTH);

		p = new Panel(new ColumnLayout());
		p.add(cbBuffer);
		p.add(cbQuery);
		cpBins.add(p, BorderLayout.SOUTH);

		p = new Panel(new ColumnLayout());
		p.add(cbNBins);
		p.add(tfN1);
		p.add(tfN2);
		cpBins.add(p, BorderLayout.WEST);

		p = new Panel(new ColumnLayout());
		p.add(cbRoundingBins);
		p.add(tfF1);
		p.add(tfF2);
		cpBins.add(p, BorderLayout.EAST);

		p = new Panel(new ColumnLayout());
		p.add(new Label("- for attributes -", Label.CENTER));
		p.add(new Label(table.getAttributeName((String) attributes.elementAt(0)), Label.CENTER));
		p.add(new Label(table.getAttributeName((String) attributes.elementAt(1)), Label.CENTER));
		cpBins.add(p, BorderLayout.CENTER);

		cpRendering.add(new Line(false));
		cpRendering.add(new Label("Appearance:"));
		cpRendering.add(new Line(false));
		cbg = new CheckboxGroup();
		cbRendering = new Checkbox[4];
		for (int i = 0; i < cbRendering.length; i++) {
			cbRendering[i] = new Checkbox(renderingStyles[i], i == 2, cbg);
			cpRendering.add(cbRendering[i]);
			cbRendering[i].addItemListener(this);
		}
		cpRendering.add(new Line(false));
		cbSquareCells = new Checkbox("square cells", false);
		cbSquareCells.addItemListener(this);
		cpRendering.add(cbSquareCells);

		chOper = new Choice();
		chOper.addItem("count");
		chOper.addItem("max value");
		chOper.addItem("avg value");
		chOper.addItem("N distinct");
		p = new Panel(new BorderLayout());
		p.add(chOper, BorderLayout.CENTER);
		p.add(new Label("Measure:"), BorderLayout.WEST);
		p.add(new Label("of"), BorderLayout.EAST);
		cpAggrOper.add(p, BorderLayout.WEST);
		chAttr = new Choice();
		for (int i = 0; i < table.getAttrCount(); i++) {
			chAttr.addItem(table.getAttributeName(i));
		}
		cpAggrOper.add(chAttr, BorderLayout.CENTER);
		Button b = new Button("Go");
		b.addActionListener(this);
		cpAggrOper.add(b, BorderLayout.EAST);
		cpAggrOper.add(new Line(false), BorderLayout.NORTH);

		cbCumulative = new Checkbox("Cumulative:", false);
		cbg = new CheckboxGroup();
		cbCumX = new Checkbox("along X", true, cbg);
		cbCumY = new Checkbox("along Y", false, cbg);
		cbg = new CheckboxGroup();
		cbCumFromMin = new Checkbox("from min", true, cbg);
		cbCumToMax = new Checkbox("to max", false, cbg);
		cbCumulative.addItemListener(this);
		cbCumX.addItemListener(this);
		cbCumY.addItemListener(this);
		cbCumFromMin.addItemListener(this);
		cbCumToMax.addItemListener(this);
		cbCumX.setEnabled(false);
		cbCumY.setEnabled(false);
		cbCumFromMin.setEnabled(false);
		cbCumToMax.setEnabled(false);
		cpAggregation.add(new Line(false));
		cpAggregation.add(new Label("Aggregation"));
		cpAggregation.add(cbCumulative);
		cpAggregation.add(cbCumX);
		cpAggregation.add(cbCumY);
		cpAggregation.add(new Line(false));
		cpAggregation.add(cbCumFromMin);
		cpAggregation.add(cbCumToMax);

		tfN1.setEnabled(false);
		tfN2.setEnabled(false);
		actionPerformed(new ActionEvent(tfF1, 0, ""));
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		if (ae.getSource() instanceof Button) {
			hc.setAggregation(chOper.getSelectedIndex(), chAttr.getSelectedIndex());
		} else if (ae.getSource() instanceof TextField) {
			TextField tf = (TextField) ae.getSource();
			if (tf.equals(tfN1) || tf.equals(tfN2)) {
				int n = 3;
				try {
					n = Integer.valueOf(tf.getText()).intValue();
				} catch (NumberFormatException nfo) {
					tf.setText("" + n);
				}
				hc.setNBins((tf.equals(tfN1)) ? 0 : 1, n);
			} else {
				float f1 = 1, f2 = 1;
				try {
					f1 = Float.valueOf(tfF1.getText()).floatValue();
					if (f1 <= 0) {
						f1 = 1;
					}
					int n = (int) Math.round((hc.getMax(0) - hc.getMin(0)) / f1);
					while (n > 1000) {
						f1 *= 10;
						n /= 10;
						tfF1.setText("" + f1);
					}
				} catch (NumberFormatException nfo) {
					tf.setText("" + f1);
				}
				try {
					f2 = Float.valueOf(tfF2.getText()).floatValue();
					if (f2 <= 0) {
						f2 = 1;
					}
					int n = (int) Math.round((hc.getMax(1) - hc.getMin(1)) / f2);
					while (n > 1000) {
						f2 *= 10;
						n /= 10;
						tfF2.setText("" + f2);
					}
				} catch (NumberFormatException nfo) {
					tf.setText("" + f2);
				}
				hc.setRoundingTo(f1, f2);
			}
		}
	}

	@Override
	public void itemStateChanged(ItemEvent ie) {
		if (ie.getSource().equals(cbCumulative)) {
			cbCumX.setEnabled(cbCumulative.getState());
			cbCumY.setEnabled(cbCumulative.getState());
			cbCumFromMin.setEnabled(cbCumulative.getState());
			cbCumToMax.setEnabled(cbCumulative.getState());
			hc.setCumulativeMode(cbCumulative.getState(), cbCumX.getState(), cbCumFromMin.getState());
			return;
		}
		if (ie.getSource().equals(cbCumX) || ie.getSource().equals(cbCumY) || ie.getSource().equals(cbCumFromMin) || ie.getSource().equals(cbCumToMax)) {
			hc.setCumulativeMode(cbCumulative.getState(), cbCumX.getState(), cbCumFromMin.getState());
			return;
		}
		if (ie.getSource().equals(cbNBins)) {
			tfN1.setEnabled(true);
			tfN2.setEnabled(true);
			tfF1.setEnabled(false);
			tfF2.setEnabled(false);
			actionPerformed(new ActionEvent(tfN1, 0, ""));
			actionPerformed(new ActionEvent(tfN2, 0, ""));
		}
		if (ie.getSource().equals(cbRoundingBins)) {
			tfN1.setEnabled(false);
			tfN2.setEnabled(false);
			tfF1.setEnabled(true);
			tfF2.setEnabled(true);
			actionPerformed(new ActionEvent(tfF1, 0, ""));
		}
		if (ie.getSource().equals(cbQuery)) {
			hc.setAdjustMinMaxbyQuery(cbQuery.getState());
		}
		if (ie.getSource().equals(cbBuffer)) {
			hc.setBufferMinMax(cbBuffer.getState());
		}
		for (int i = 0; i < cbRendering.length; i++)
			if (cbRendering[i].getState()) {
				hc.setRenderingStyle(i);
			}
		if (ie.getSource().equals(cbSquareCells)) {
			hc.setSquareCells(cbSquareCells.getState());
		}
	}

	@Override
	public String getName() {
		String name = PlotGeneratorsDescriptor.getToolName(getMethodId()) + " " + getInstanceN();
		if (name == null) {
			name = getMethodId() + " " + getInstanceN();
		}
		return name;
	}

	/**
	* Through this function the component constructing the plot can set the
	* identifier of the visualization method.
	*/
	public void setMethodId(String id) {
		methodId = id;
	}

	/**
	* Returns the identifier of the visualization method implemented by this
	* class.
	*/
	public String getMethodId() {
		return methodId;
	}

	public int getInstanceN() {
		return instanceN;
	}

	//ToDo: codes for saveable: get/set Properties and getSpecification

}
