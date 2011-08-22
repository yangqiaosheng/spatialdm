package spade.analysis.calc;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.generators.PCPGenerator;
import spade.analysis.plot.FNReorder;
import spade.analysis.plot.PCPlot;
import spade.analysis.system.Supervisor;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.DialogContent;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.SplitLayout;
import spade.lib.lang.Language;
import spade.lib.util.DoubleArray;
import spade.lib.util.StringUtil;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataTable;
import spade.vis.database.TableStat;

class SensitivityParametersPanel extends Panel implements DialogContent {

	static ResourceBundle res = Language.getTextResource("spade.analysis.calc.Res");
	float curWeights[] = null;
	int fn[] = null;
	DataTable dTable = null;
	double vmin[] = null, vmax[] = null;
	int vni[] = null;
	TextField tfMin[] = null, tfMax[] = null, tfNI[] = null;

	public SensitivityParametersPanel(float curWeights[], int fn[], DataTable dTable) {
		super();
		this.curWeights = curWeights;
		this.fn = fn;
		this.dTable = dTable;
		vmin = new double[fn.length];
		vmax = new double[fn.length];
		vni = new int[fn.length];
		for (int i = 0; i < fn.length; i++) {
			vmin[i] = curWeights[i] / 1.5f;
			vmax[i] = curWeights[i] * 1.5f;
			if (vmax[i] > 1) {
				vmax[i] = 1f;
			}
		}
		tfMin = new TextField[fn.length];
		tfMax = new TextField[fn.length];
		tfNI = new TextField[fn.length];
		setLayout(new GridLayout(1 + 2 * fn.length, 1));
		// following text: "Let's shake the weights !"
		add(new Label(res.getString("Lstw"), Label.CENTER));
		for (int i = 0; i < fn.length; i++) {
			Panel pp = new Panel();
			pp.setLayout(new BorderLayout());
			pp.add(new Label(dTable.getAttributeName(fn[i]), Label.LEFT), "West");
			pp.add(new Label(StringUtil.floatToStr(curWeights[i])), "East");
			add(pp);
			pp = new Panel();
			pp.setLayout(new FlowLayout());
			pp.add(tfNI[i] = new TextField("20"));
			// following text: "iterations between weights"
			pp.add(new Label(res.getString("ibw")));
			pp.add(tfMin[i] = new TextField(StringUtil.doubleToStr(vmin[i], vmin[i], vmax[i])));
			// following text: " and"
			pp.add(new Label(res.getString(" and")));
			pp.add(tfMax[i] = new TextField(StringUtil.doubleToStr(vmax[i], vmin[i], vmax[i])));
			add(pp);
		}
	}

	public double[] getVMin() {
		return vmin;
	}

	public double[] getVMax() {
		return vmax;
	}

	public int[] getVNI() {
		return vni;
	}

	protected String errMsg = null;

	@Override
	public String getErrorMessage() {
		return errMsg;
	}

	@Override
	public boolean canClose() {
		for (int i = 0; i < fn.length; i++) {
			try {
				vmin[i] = Double.valueOf(tfMin[i].getText()).doubleValue();
				if (vmin[i] < 0) {
					// following text: "weight should be non-negative"
					errMsg = res.getString("wsbnn");
					return false;
				}
				if (vmin[i] > curWeights[i]) {
					tfMin[i].requestFocus();
					// following text: "current weight should be between Min and Max"
					errMsg = res.getString("cwsbbmm");
					return false;
				}
			} catch (NumberFormatException nfi) {
				tfMin[i].requestFocus();
				// following text: "wrong number in the numeric field"
				errMsg = res.getString("wnitnf");
				return false;
			}
			try {
				vmax[i] = Double.valueOf(tfMax[i].getText()).doubleValue();
				if (vmax[i] > 1) {
					// following text: "weight should be less than 1.0"
					errMsg = res.getString("wsblt");
					return false;
				}
				if (vmax[i] < curWeights[i]) {
					tfMax[i].requestFocus();
					// following text: "current weight should be between Min and Max"
					errMsg = res.getString("cwsbbmm");
					return false;
				}
			} catch (NumberFormatException nfi) {
				tfMax[i].requestFocus();
				// following text: "wrong number in the numeric field"
				errMsg = res.getString("wnitnf");
				return false;
			}
			try {
				vni[i] = Integer.valueOf(tfNI[i].getText()).intValue();
				if (vni[i] < 0 || vni[i] == 1) {
					// following text: "wrong number of iterations
					errMsg = res.getString("wnoi");
					return false;
				}
			} catch (NumberFormatException nfi) {
				tfNI[i].requestFocus();
				// following text: "wrong number in the numeric field"
				errMsg = "wnitnf";
				return false;
			}
		}
		return true;
	}
}

public class IdealPointCalc extends CalcDlg implements ActionListener, ItemListener {
	/*following text: "Supports multi-criteria selection: \"wraps\" "+
	 * "values of several attributes (selection criteria) in a single evaluation "+
	 * "score. The criteria may differ in relative importance that is reflected "+
	 * "in their weights." */
	public static final String expl = res.getString("Supportsmcs") + res.getString("vosa") + res.getString("Tcmd") + res.getString("itw");
	/*following text: "Select at least two numeric attributes "+
	 * "representing your selection criteria." */
	public static final String prompt = res.getString("Select_at_least_two") + res.getString("rycs");

	protected TableStat tStat = null;

	protected SplitLayout splL = null;

	protected WAPanel cp = null;
	protected PCPlot pcp = null;
	protected FNReorder pcpl = null;
	protected String AttrIDscore = null, AttrIDorder = null;
	protected Checkbox cbClassify = null;

	/**
	* Returns the minimum number of attributes needed for this computation
	*/
	@Override
	public int getMinAttrNumber() {
		return 2;
	}

	/**
	* Returns the maximum number of attributes needed for this computation.
	* If the maximum number is unlimited, returns -1.
	*/
	@Override
	public int getMaxAttrNumber() {
		return -1;
	}

	/**
	* Constructs the dialog appearance
	*/
	@Override
	public void makeInterface() {
		// following text: "\" Ideal point\" decision support method"
		setTitle(res.getString("Ipdsm"));
		setLayout(new BorderLayout());
		Panel mainP = new Panel();
		// following text: "Center"
		add(mainP, "Center");
		mainP.setLayout(splL = new SplitLayout(mainP, SplitLayout.VERT));
		Vector attr = null;
		if (fn != null) {
			attr = new Vector(fn.length, 1);
			for (int element : fn) {
				attr.addElement(dTable.getAttributeId(element));
			}
		}
		cp = new WAPanel(this, dTable, attr, true); // Control Panel
		splL.addComponent(cp, 0.4f);
		Panel bottomP = new Panel();
		bottomP.setLayout(new ColumnLayout());
		bottomP.add(new Line(false));
		boolean classify = supervisor != null && supervisor.getSystemSettings() != null && supervisor.getSystemSettings().checkParameterValue("DECISION_TYPE", "classification");
		// following text: "Classify results"
		cbClassify = new Checkbox(res.getString("classres"), classify);
		cbClassify.addItemListener(this);
		((ColumnLayout) bottomP.getLayout()).setAlignment(ColumnLayout.Hor_Left);
		bottomP.add(cbClassify);
		// following text: "Run sensitivity analysis with current weights ..."
		Button b = new Button(res.getString("Rsawcw"));
		bottomP.add(b);
		b.addActionListener(this);
		b.setActionCommand("sensitivityAnalysis");
		if (dTable.hasDecisionSupporter()) {
			// following text: "Make decision"
			b = new Button(res.getString("Make_decision"));
			bottomP.add(b);
			b.addActionListener(this);
			b.setActionCommand("decision");
		}
		add(bottomP, "South");
		pack();
	}

	protected boolean orderAfterScore = true;

	@Override
	public void actionPerformed(ActionEvent ae) {
		if (ae.getActionCommand() != null && ae.getActionCommand().equals("decision")) {
			if (AttrIDorder != null) {
				dTable.notifyDecisionColumnAdded(AttrIDorder);
			}
			return;
		}
		if (ae.getSource() == pcpl) {
			int dragged = pcpl.getDragged(), draggedTo = pcpl.getDraggedTo();
			float groupBreak = pcpl.getGroupBreak();
			if (draggedTo > groupBreak) {
				orderAfterScore = !orderAfterScore;
			} else {
				cp.fnReordered(dragged, draggedTo);
				Vector attr = cp.getAttributes();
				if (attr == null) {
					fn = null;
				} else {
					fn = new int[attr.size()];
					for (int i = 0; i < attr.size(); i++) {
						fn[i] = dTable.getAttrIndex((String) attr.elementAt(i));
					}
				}
			}
			setWeightsInPcp(true);
			return;
		}
		if (ae.getSource() == cp) {
			if (ae.getActionCommand().equals("fnChanged")) {

				int oldFnL = fn.length;
				Vector attr = cp.getAttributes();
				if (attr == null) {
					fn = null;
				} else {
					fn = new int[attr.size()];
					for (int i = 0; i < attr.size(); i++) {
						fn[i] = dTable.getAttrIndex((String) attr.elementAt(i));
					}
				}
				int pcpFn[] = new int[fn.length + 2];
				for (int i = 0; i < fn.length; i++) {
					pcpFn[i] = fn[i];
				}
				if (orderAfterScore) {
					pcpFn[fn.length] = dTable.getAttrIndex(AttrIDscore);
					pcpFn[fn.length + 1] = dTable.getAttrIndex(AttrIDorder);
				} else {
					pcpFn[fn.length] = dTable.getAttrIndex(AttrIDorder);
					pcpFn[fn.length + 1] = dTable.getAttrIndex(AttrIDscore);
				}
				/*
				 * A.O. 2004-07-30
				 * don't redraw the plot with setWeights, as there will
				 * be a PropertyChange event, which redraws it anyway.
				 */
				pcp.setWeights(null, false);
				pcp.setFn(pcpFn);
				pcpl.setFn(pcpFn);
				pcpl.setGroupBreak((float) (pcpFn.length - 2.5));
				setSize(getSize().width, Math.round(getSize().height * (fn.length + 0f) / oldFnL));
				pack();
				supervisor.notifyGlobalPropertyChange(Supervisor.eventDisplayedAttrs);
			} else { // "weightsChanged" - just recompute !
			}
			updateDataTable();
		}
		if (ae.getSource() instanceof Button) {
			doSensitivityAnalysis();
		}
	}

	public void updateDataTable() {
		// recomputing columns
		int column = dTable.getAttrIndex(AttrIDscore);
		double vals[] = compute();
		dTable.setNumericAttributeValues(vals, column);
		//for (int i=0; i<dTable.getDataItemCount(); i++)
		//  dTable.getDataRecord(i).setNumericAttrValue(vals[i],column);
		int order[] = TableStat.getOrderOfColumn(dTable, column);
		column = dTable.getAttrIndex(AttrIDorder);
		for (int i = 0; i < dTable.getDataItemCount(); i++) {
			//dTable.getDataRecord(i).setNumericAttrValue(order[i],column);
			dTable.getDataRecord(i).setAttrValue((order[i] == -1) ? "" : String.valueOf(order[i]), column);
		}
		// set weights in PCPlot
		// 
		// A.O. 2004-07-30
		// without instant redraw. The PropertyChange event
		// will redraw the Plot.
		setWeightsInPcp(false);
		// inform all displays about change of values
		Vector attr = new Vector(2, 1);
		attr.addElement(AttrIDscore);
		attr.addElement(AttrIDorder);
		dTable.notifyPropertyChange("values", null, attr);
	}

	protected static int N = 0;

	public void doSensitivityAnalysis() {
		// 0. Set parameters
		SensitivityParametersPanel spp = new SensitivityParametersPanel(cp.getWeights(), fn, dTable);
		// following text: "Sensitivity analysis parameters"
		OKDialog okDlg = new OKDialog(this, res.getString("sap"), true);
		okDlg.addContent(spp);
		okDlg.show();
		double vmin[] = spp.getVMin(), vmax[] = spp.getVMax();
		int vNI[] = spp.getVNI();
		if (okDlg.wasCancelled())
			return;
		// 1. save current weights, values and order
		int columnVals = dTable.getAttrIndex(AttrIDscore), columnOrder = dTable.getAttrIndex(AttrIDorder);
		double vals[] = new double[dTable.getDataItemCount()];
		int order[] = new int[dTable.getDataItemCount()], orderCurrent[] = new int[dTable.getDataItemCount()], orderMin[] = new int[dTable.getDataItemCount()], orderMax[] = new int[dTable.getDataItemCount()];
		double orderMean[] = new double[dTable.getDataItemCount()];
		for (int i = 0; i < dTable.getDataItemCount(); i++) {
			vals[i] = dTable.getDataRecord(i).getNumericAttrValue(columnVals);
		}
		for (int i = 0; i < dTable.getDataItemCount(); i++) {
			double f = dTable.getDataRecord(i).getNumericAttrValue(columnOrder);
			order[i] = (Double.isNaN(f)) ? -1 : (int) Math.round(f);
		}
		float weights[] = cp.getWeights();
		// 2. iterate through all criteria: shaking weights
		int nAddedColumns = 0;
		for (int i = 0; i < order.length; i++) {
			orderMin[i] = orderMax[i] = -1;
			orderMean[i] = 0;
		}
		Vector sourceAttrs = new Vector(fn.length, 5);
		for (int element : fn) {
			sourceAttrs.addElement(dTable.getAttributeId(element));
		}
		int NIterations = 0;
		for (int k = 0; k < fn.length; k++)
			if (vNI[k] > 1) {
				NIterations += vNI[k];
				for (int j = 0; j <= vNI[k] - 1; j++) {
					cp.adjustWeights(k, (float) (vmin[k] + (vmax[k] - vmin[k]) * j / (vNI[k] - 1)));
					double v[] = compute();
					for (int i = 0; i < dTable.getDataItemCount(); i++) {
						dTable.getDataRecord(i).setNumericAttrValue(v[i], columnVals);
					}
					orderCurrent = TableStat.getOrderOfColumn(dTable, columnVals);
					for (int i = 0; i < order.length; i++) {
						orderMean[i] += orderCurrent[i];
						if (orderMin[i] == -1 || orderCurrent[i] < orderMin[i]) {
							orderMin[i] = orderCurrent[i];
						}
						if (orderMax[i] == -1 || orderCurrent[i] > orderMax[i]) {
							orderMax[i] = orderCurrent[i];
						}
					}
					nAddedColumns++;
					dTable.addDerivedAttribute("tmpColumn" + nAddedColumns, AttributeTypes.integer, AttributeTypes.evaluate_rank, sourceAttrs);
					for (int i = 0; i < order.length; i++) {
						//dTable.getDataRecord(i).setNumericAttrValue(orderCurrent[i],dTable.getAttrCount()-1);
						dTable.getDataRecord(i).setAttrValue((orderCurrent[i] == -1) ? "" : String.valueOf(orderCurrent[i]), dTable.getAttrCount() - 1);
					}
				}
				cp.adjustWeights(k, weights[k]);
			}
		for (int i = 0; i < order.length; i++) {
			orderMean[i] /= NIterations;
		}
		// 3. Compute median and variance
		int fn[] = new int[nAddedColumns];
		for (int i = 0; i < fn.length; i++) {
			fn[i] = dTable.getAttrCount() - nAddedColumns + i;
		}
		float orderVariance[] = DoubleArray.double2float(TableStat.getVarianceOfColumns(dTable, fn));
		dTable.removeAttributes(dTable.getAttrCount() - nAddedColumns, dTable.getAttrCount() - 1);
		// 4. restore current weights, values and order
		dTable.setNumericAttributeValues(vals, columnVals);
		for (int i = 0; i < dTable.getDataItemCount(); i++) {
			dTable.getDataRecord(i).setAttrValue((order[i] == -1) ? "" : String.valueOf(order[i]), columnOrder);
		}
		//dTable.getDataRecord(i).setNumericAttrValue(order[i],columnOrder);
		//dTable.setNumericAttributeValues(order,columnOrder);
		/*
		for (int i=0; i<dTable.getDataItemCount(); i++) {
		  dTable.getDataRecord(i).setNumericAttrValue(vals[i],columnVals);
		  dTable.getDataRecord(i).setNumericAttrValue(order[i],columnOrder);
		}
		*/
		// 5. save statistics to the table
		/*
		System.out.println("* Statistics: min, max, average order");
		for (int i=0; i<order.length; i++)
		  System.out.println("* "+dTable.getDataItemId(i)+
		                     " <"+dTable.getDataItemName(i)+"> "+
		                     orderMin[i]+" "+orderMax[i]+" "+orderMean[i]);
		*/
		Vector vattr = new Vector();
		int idx = dTable.addDerivedAttribute("MinOrder" + N, AttributeTypes.integer, AttributeTypes.evaluate_rank, sourceAttrs);
		dTable.addDerivedAttribute("MaxOrder" + N, AttributeTypes.integer, AttributeTypes.evaluate_rank, sourceAttrs);
		dTable.addDerivedAttribute("MeanOrder" + N, AttributeTypes.integer, AttributeTypes.evaluate_rank, sourceAttrs);
		dTable.addDerivedAttribute("VarianceOfOrder" + N, AttributeTypes.integer, AttributeTypes.evaluate_rank, sourceAttrs);
		//dTable.setNumericAttributeValues(orderMin,idx);
		//dTable.setNumericAttributeValues(orderMax,idx+1);
		//dTable.setNumericAttributeValues(orderMean,idx+2);
		dTable.setNumericAttributeValues(orderVariance, idx + 3);

		for (int i = 0; i < order.length; i++) {
			dTable.getDataRecord(i).setAttrValue((orderMin[i] == -1) ? "" : String.valueOf(orderMin[i]), idx);
			dTable.getDataRecord(i).setAttrValue((orderMax[i] == -1) ? "" : String.valueOf(orderMax[i]), idx + 1);
			dTable.getDataRecord(i).setAttrValue((orderMean[i] == -1) ? "" : String.valueOf(orderMean[i]), idx + 2);
			//dTable.getDataRecord(i).setNumericAttrValue(orderVariance[i],idx+3);
		}

		for (int n = 0; n < 3; n++) {
			vattr.addElement(dTable.getAttributeId(idx + n));
		}
		dTable.getSemanticsManager().setAttributesComparable(vattr);
		vattr.addElement(dTable.getAttributeId(idx + 3));
		attrAddedToTable(vattr);
		N++;
	}

	public void setWeightsInPcp(boolean redraw) { // set weights in PCPlot
		float w[] = cp.getWeights(), weights[] = new float[w.length + 2];
		boolean isMax[] = cp.getIsMax();
		for (int i = 0; i < w.length; i++) {
			weights[i] = w[i] * ((isMax[i]) ? 1 : -1);
		}
		if (orderAfterScore) {
			weights[w.length] = 1;
			weights[w.length + 1] = -1;
		} else {
			weights[w.length] = -1;
			weights[w.length + 1] = +1;
		}
		pcp.setWeights(weights, redraw);
	}

	@Override
	protected void start() {
		calculateIdealPoint();
	}

	public void calculateIdealPoint() {
		// computing new column
		double vals[] = compute();
		// adding the column to the table
		Vector sourceAttrs = new Vector(fn.length, 5);
		for (int element : fn) {
			sourceAttrs.addElement(dTable.getAttributeId(element));
		}

		int idx = dTable.addDerivedAttribute("Evaluation score", AttributeTypes.real, AttributeTypes.evaluate_score, sourceAttrs);
		for (int i = 0; i < dTable.getDataItemCount(); i++) {
			dTable.getDataRecord(i).setAttrValue("" + vals[i], idx);
		}
		AttrIDscore = dTable.getAttributeId(idx);
		// computing orders
		int order[] = TableStat.getOrderOfColumn(dTable, idx);
		idx = dTable.addDerivedAttribute("Ranking", AttributeTypes.integer, AttributeTypes.evaluate_rank, sourceAttrs);
		for (int i = 0; i < dTable.getDataItemCount(); i++) {
			//dTable.getDataRecord(i).setAttrValue(""+order[i],idx);
			dTable.getDataRecord(i).setAttrValue((order[i] == -1) ? "" : String.valueOf(order[i]), idx);
		}
		AttrIDorder = dTable.getAttributeId(idx);
		// set prohibited attributes for CP
		String pa[] = new String[2];
		pa[0] = AttrIDscore;
		pa[1] = AttrIDorder;
		cp.setProhibitedAttributes(pa);
		// prepare results
		Vector resultAttrs = new Vector(2, 5);
		resultAttrs.addElement(AttrIDscore);
		resultAttrs.addElement(AttrIDorder);
		Vector mapAttr = new Vector(1, 5);
		mapAttr.addElement(AttrIDorder);
		//add attribute dependency and notify about new attribute
		attrAddedToTable(resultAttrs);
		// show results of calculations on the map
		tryShowOnMap(mapAttr, cbClassify.getState() ? "class1D" : "value_paint", true);
		PCPGenerator vg = new PCPGenerator();
		float w[] = cp.getWeights(), weights[] = new float[w.length + 2];
		boolean isMax[] = cp.getIsMax();
		for (int i = 0; i < w.length; i++) {
			weights[i] = w[i] * ((isMax[i]) ? 1 : -1);
		}
		if (true) {
			weights[w.length] = 1;
			weights[w.length + 1] = -1;
		}
		//else { weights[w.length]=-1; weights[w.length+1]=+1; }
		Object o[] = vg.constructDisplayIPCalc(supervisor, dTable, getInvolvedAttrs(), weights);
		//setPCP((Component)o[0],(java.awt.Choice)o[1],(PCPlot)o[2]);
		splL.addComponent((Component) o[0], 2f);
		pack();
		supervisor.notifyGlobalPropertyChange(Supervisor.eventDisplayedAttrs);
		this.pcp = (PCPlot) o[2];
		this.pcpl = (FNReorder) o[3];
		pcpl.setGroupBreak((float) (fn.length - 0.5));
		pcpl.addActionListener(this);
		setWeightsInPcp(true);
	}

	@Override
	public void itemStateChanged(ItemEvent ie) {
		if (ie.getSource() == cbClassify) {
			Vector mapAttr = new Vector(1, 5);
			mapAttr.addElement(AttrIDorder);
			tryShowOnMap(mapAttr, cbClassify.getState() ? "class1D" : "value_paint", true);
		}
	}

	protected double SepMinus[] = null, SepPlus[] = null, Closeness[] = null;

	public double[] compute() {
		if (tStat == null) {
			tStat = new TableStat();
			tStat.setDataTable(dTable);
		}
		boolean isMax[] = cp.getIsMax();
		float W[] = cp.getWeights();
		if (SepMinus == null) {
			SepMinus = new double[dTable.getDataItemCount()];
		}
		if (SepPlus == null) {
			SepPlus = new double[dTable.getDataItemCount()];
		}
		if (Closeness == null) {
			Closeness = new double[dTable.getDataItemCount()];
		}
		double minC = Double.NaN, maxC = Double.NaN;
		for (int n = 0; n < dTable.getDataItemCount(); n++) {
			SepMinus[n] = 0;
			SepPlus[n] = 0;
			Closeness[n] = Double.NaN;
			try {
				for (int i = 0; i < fn.length; i++)
					if (isMax[i]) {
						SepPlus[n] += W[i] * Math.pow((dTable.getNumericAttrValue(fn[i], n) - tStat.getMin(fn[i])) / (tStat.getMax(fn[i]) - tStat.getMin(fn[i])) - 1, 2);
						SepMinus[n] += W[i] * Math.pow((dTable.getNumericAttrValue(fn[i], n) - tStat.getMin(fn[i])) / (tStat.getMax(fn[i]) - tStat.getMin(fn[i])) - 0, 2);
					} else {
						SepPlus[n] += W[i] * Math.pow((dTable.getNumericAttrValue(fn[i], n) - tStat.getMax(fn[i])) / (tStat.getMax(fn[i]) - tStat.getMin(fn[i])) + 1, 2);
						SepMinus[n] += W[i] * Math.pow((dTable.getNumericAttrValue(fn[i], n) - tStat.getMax(fn[i])) / (tStat.getMax(fn[i]) - tStat.getMin(fn[i])) + 0, 2);
					}
				SepPlus[n] = Math.sqrt(SepPlus[n]);
				SepMinus[n] = Math.sqrt(SepMinus[n]);
			} catch (ArithmeticException ae) {
				System.out.println("* " + ae);
			}
			Closeness[n] = SepMinus[n] / (SepPlus[n] + SepMinus[n]);
			if (Double.isNaN(minC) || minC > Closeness[n]) {
				minC = Closeness[n];
			}
			if (Double.isNaN(maxC) || maxC < Closeness[n]) {
				maxC = Closeness[n];
				//System.out.println("* "+n+", - "+SepMinus[n]+", + "+SepPlus[n]+", ="+Closeness[n]);
			}
		}
		for (int n = 0; n < Closeness.length; n++) {
			Closeness[n] = 100 * (Closeness[n] - minC) / (maxC - minC);
		}
		return Closeness;
	}

	public Vector getInvolvedAttrs() {
		Vector v = new Vector(fn.length + 2);
		for (int element : fn) {
			v.addElement(new String(dTable.getAttributeId(element)));
		}
		v.addElement(new String(AttrIDscore));
		v.addElement(new String(AttrIDorder));
		return v;
	}

	/**
	* Returns an explanation about this calculation method
	*/
	@Override
	public String getExplanation() {
		return expl;
	}

	/**
	* Returns an instruction for the user about what and how many attributes to
	* select.
	*/
	@Override
	public String getAttributeSelectionPrompt() {
		return prompt;
	}
}
