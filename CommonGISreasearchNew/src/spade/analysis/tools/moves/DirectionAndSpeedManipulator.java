package spade.analysis.tools.moves;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Vector;

import spade.analysis.manipulation.Manipulator;
import spade.analysis.system.Supervisor;
import spade.lib.basicwin.CheckboxPanel;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.FocusListener;
import spade.lib.basicwin.Focuser;
import spade.lib.basicwin.FocuserCanvas;
import spade.lib.basicwin.FoldablePanel;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.Slider;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.Parameter;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: Mar 4, 2008
 * Time: 11:59:37 AM
 * To change this template use File | Settings | File Templates.
 */
public class DirectionAndSpeedManipulator extends Panel implements ActionListener, ItemListener, Manipulator, FocusListener {

	/**
	* The ValueFlowVisualizer to manipulate
	*/
	protected DirectionAndSpeedVisualizer dasVis = null;

	protected Parameter pars[] = null; // parameters of current aggregation (at least 1: DandS, may be also time)

	protected CheckboxPanel cpAttr = null, cpParams[] = null;
	protected Checkbox cbShow[] = null;

	protected Focuser f = null;

	protected Checkbox cbStressMax = null;
	protected Slider slStressMax[] = null;
	protected TextField tfStressMax[] = null;
	protected Checkbox cbStressMaxMode[] = null;

	/**
	* Construction of map manipulator. Returns true if successfully constructed.
	* Arguments: 1) supervisor responsible for dynamic linking of displays;
	* 2) visualizer or classifier to be manipulated; 3) table with source data;
	* 4) table filter
	* The Manipulator must check if the visualizer has the appropriate type.
	*/
	@Override
	public boolean construct(Supervisor sup, Object visualizer, AttributeDataPortion table) {
		if (visualizer == null || !(visualizer instanceof DirectionAndSpeedVisualizer))
			return false;
		dasVis = (DirectionAndSpeedVisualizer) visualizer;
		Vector vAttr = dasVis.getDASAttributeList();
		pars = dasVis.getParameters();
		setLayout(new ColumnLayout());
		// selection of attributes
		if (vAttr != null && vAttr.size() > 0) {
			Checkbox cbAttr[] = new Checkbox[vAttr.size()];
			for (int i = 0; i < vAttr.size(); i++) {
				cbAttr[i] = new Checkbox(((Attribute) vAttr.elementAt(i)).getName(), i == 0);
			}
			Label label = new Label("Attribute");
			cpAttr = new CheckboxPanel(this, label, cbAttr);
			add(new FoldablePanel(cpAttr, label));
			add(new Line(false));
		}
		// selection of parameters
		cpParams = new CheckboxPanel[pars.length];
		for (int i = 1; i < pars.length; i++) {
			Checkbox cb[] = new Checkbox[pars[i].getValueCount()];
			for (int j = 0; j < cb.length; j++) {
				cb[j] = new Checkbox(pars[i].getValue(j).toString(), j == 0);
			}
			Label label = new Label(pars[i].getName());
			cpParams[i] = new CheckboxPanel(this, label, cb);
			add(new FoldablePanel(cpParams[i], label));
			add(new Line(false));
		}
		// focuser for values
		Panel pb = new Panel(new BorderLayout(0, 0));
		pb.add(new Label("Focus:"), BorderLayout.CENTER);
		TextField tfmin = new TextField("0", 5), tfmax = new TextField("" + dasVis.getMax(), 5);
		pb.add(tfmin, BorderLayout.WEST);
		pb.add(tfmax, BorderLayout.EAST);
		add(pb);
		f = new Focuser();
		f.setIsVertical(false);
		f.addFocusListener(this);
		f.setAbsMinMax(0f, dasVis.getMax());
		f.setCurrMinMax(0f, dasVis.getMax());
		f.setTextFields(tfmin, tfmax);
		f.setIsUsedForQuery(true);
		add(new FocuserCanvas(f, false));
		add(new Line(false));
		// selection of segments to be shown
		cbShow = new Checkbox[pars[0].getValueCount()];
		Panel pf = new Panel(new GridLayout((cbShow.length == 5) ? 1 : 2, 5, 0, 0));
		for (int i = 0; i < cbShow.length; i++) {
			cbShow[i] = new Checkbox(pars[0].getValue(i).toString(), true);
			cbShow[i].addItemListener(this);
			cbShow[i].setBackground(dasVis.getSegmentColor(i));
			if (i == 5) {
				pf.add(new Label(""));
			}
			pf.add(cbShow[i]);
		}
		add(pf);
		add(new Line(false));
		// dominant direction controls
		add(cbStressMax = new Checkbox("Dominant directions only, threshold:", false));
		cbStressMax.addItemListener(this);
		CheckboxGroup cbg = new CheckboxGroup();
		cbStressMaxMode = new Checkbox[2];
		cbStressMaxMode[0] = new Checkbox("ratio(%) to next:", cbg, true);
		cbStressMaxMode[1] = new Checkbox("difference to next:", cbg, false);
		tfStressMax = new TextField[2];
		slStressMax = new Slider[2];
		pb = new Panel(new BorderLayout(0, 0));
		pb.add(cbStressMaxMode[0], BorderLayout.CENTER);
		tfStressMax[0] = new TextField("0", 5);
		pb.add(tfStressMax[0], BorderLayout.EAST);
		add(pb);
		add(slStressMax[0] = new Slider(this, 0f, 100f, 0f));
		slStressMax[0].setTextField(tfStressMax[0]);
		pb = new Panel(new BorderLayout(0, 0));
		pb.add(cbStressMaxMode[1], BorderLayout.CENTER);
		tfStressMax[1] = new TextField("0", 5);
		pb.add(tfStressMax[1], BorderLayout.EAST);
		add(pb);
		add(slStressMax[1] = new Slider(this, 0f, dasVis.getMax(), 0f));
		slStressMax[1].setTextField(tfStressMax[1]);
		for (int i = 0; i < cbStressMaxMode.length; i++) {
			cbStressMaxMode[i].setEnabled(cbStressMax.getState());
			cbStressMaxMode[i].addItemListener(this);
			tfStressMax[i].setEnabled(cbStressMax.getState() && cbStressMaxMode[i].getState());
			slStressMax[i].setEnabled(cbStressMax.getState() && cbStressMaxMode[i].getState());
			slStressMax[i].setNAD(true);
		}
		add(new Line(false));

		return true;
	}

	@Override
	public void itemStateChanged(ItemEvent ie) {
		if (ie.getSource().equals(cbStressMax) || ie.getSource().equals(cbStressMaxMode[0]) || ie.getSource().equals(cbStressMaxMode[1])) {
			dasVis.setStressMaxValue(cbStressMax.getState(), cbStressMaxMode[0].getState(), (float) slStressMax[0].getValue() / 100f, (float) slStressMax[1].getValue());
			for (int i = 0; i < cbStressMaxMode.length; i++) {
				cbStressMaxMode[i].setEnabled(cbStressMax.getState());
				tfStressMax[i].setEnabled(cbStressMax.getState() && cbStressMaxMode[i].getState());
				slStressMax[i].setEnabled(cbStressMax.getState() && cbStressMaxMode[i].getState());
				slStressMax[i].repaint();
			}
			return;
		}
		for (int i = 0; i < cbShow.length; i++)
			if (ie.getSource().equals(cbShow[i])) {
				dasVis.setDrawSegment(i, cbShow[i].getState());
				return;
			}
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		if (ae.getSource().equals(slStressMax[0]) || ae.getSource().equals(slStressMax[1])) {
			dasVis.setStressMaxValue(cbStressMax.getState(), cbStressMaxMode[0].getState(), (float) slStressMax[0].getValue() / 100f, (float) slStressMax[1].getValue());
			return;
		}
		if (ae.getSource().equals(cpAttr)) {
			dasVis.setIdxAttr(cpAttr.getSelectedIndex());
			f.setAbsMinMax(0f, dasVis.getMax());
			f.setCurrMinMax(0f, dasVis.getMax());
			f.refresh();
			slStressMax[1].setAbsMax(dasVis.getMax());
			slStressMax[1].setValue(0f);
			return;
		}
		for (int i = 0; i < cpParams.length; i++)
			if (ae.getSource().equals(cpParams[i])) {
				dasVis.setIdxParsValue(i, cpParams[i].getSelectedIndex());
				return;
			}
	}

	@Override
	public void focusChanged(Object source, double lowerLimit, double upperLimit) {
		if (source.equals(f)) {
			dasVis.setFocuserMinMax(lowerLimit, upperLimit);
		}
	}

	@Override
	public void limitIsMoving(Object source, int n, double currValue) { // n==0 -> min, n==1 -> max
		if (source.equals(f)) {
			dasVis.setFocuserMinMax(f.getCurrMin(), f.getCurrMax());
		}
	}
}
