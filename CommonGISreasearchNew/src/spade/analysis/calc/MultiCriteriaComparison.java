package spade.analysis.calc;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import spade.lib.basicwin.Arrow;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.Slider;
import spade.lib.basicwin.SplitLayout;
import spade.lib.util.StringUtil;
import spade.vis.action.HighlightListener;
import spade.vis.database.AttributeTypes;
import spade.vis.database.ObjectFilter;
import spade.vis.database.SemanticsManager;
import spade.vis.database.TableStat;

public class MultiCriteriaComparison extends CalcDlg implements ActionListener, ItemListener, PropertyChangeListener, HighlightListener {
	// following texts: "equivalent", "better", "worse", "incomparable"
	protected static String valueList[] = { res.getString("equivalent"), res.getString("better"), res.getString("worse"), res.getString("incomparable") };
	public static Color valueColor[] = { Color.white, Color.green, Color.red, Color.yellow };

	protected String AttrID = null;
	/*following text: "Select at least two numeric attributes "+
	 * "representing your selection criteria." */
	protected static final String prompt = res.getString("Select_at_least_two") + res.getString("rycs");
	// following text: "Classifies options in relation to a selected one"
	protected static final String MCC_expl = res.getString("MCC_expl");

	protected Label lref = null;
	protected Slider slInaccuracy = null, slCriteria[] = null, slSynchro = null;
	protected Arrow arrows[] = null;
	protected Checkbox chDynaUpdateCr = null, chDynaUpdateIA = null, chSynchro = null, chStrategy[] = null;
	protected Button bSelRef = null;

	protected float inaccuracy = 0f;
	protected String refOptionId = null;
	protected float refVals[] = null;

	protected ObjectFilter tfilter = null;

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

	@Override
	protected void start() {
		dTable.addPropertyChangeListener(this);
		tfilter = dTable.getObjectFilter();
		if (tfilter != null) {
			tfilter.addPropertyChangeListener(this);
		}
		supervisor.registerHighlightListener(this, dTable.getEntitySetIdentifier());
		calculateMCC();
	}

	/**
	* Returns an explanation about this calculation method
	*/
	@Override
	public String getExplanation() {
		return MCC_expl;
	}

	/**
	* Returns an instruction for the user about what and how many attributes to
	* select.
	*/
	@Override
	public String getAttributeSelectionPrompt() {
		return prompt;
	}

	@Override
	protected void makeInterface() {
		tStat = new TableStat();
		tStat.setDataTable(dTable);
		refVals = new float[fn.length];
		for (int k = 0; k < fn.length; k++) {
			refVals[k] = (float) (tStat.getMin(fn[k]) + tStat.getMax(fn[k])) / 2f;
		}
		SemanticsManager sm = dTable.getSemanticsManager();
		// following text: "Multiple Criteria Comparison"
		setTitle(res.getString("MCC"));
		setLayout(new BorderLayout());
		Panel mp = new Panel();
		add(mp, BorderLayout.CENTER);
		mp.setLayout(new ColumnLayout());
		// following text: "Reference values of the criteria:"
		mp.add(new Label(res.getString("Ref_val_of_criteria"), Label.CENTER));
		mp.add(new Line(false));
		Panel pcrit = new Panel();
		mp.add(pcrit);
		SplitLayout spl = new SplitLayout(pcrit, SplitLayout.VERT);
		pcrit.setLayout(spl);
		Panel pleft = new Panel(new GridLayout(fn.length, 1)), pright = new Panel(new GridLayout(fn.length, 1));
		spl.addComponent(pleft, 0.4f);
		spl.addComponent(pright, 0.6f);
		arrows = new Arrow[fn.length];
		slCriteria = new Slider[fn.length];
		for (int k = 0; k < fn.length; k++) {
			Panel p = new Panel();
			p.setLayout(new BorderLayout());
			pleft.add(p);
			boolean b = true;
			if (sm != null && sm.isAttributeCostCriterion(dTable.getAttributeId(fn[k]))) {
				b = false;
			}
			arrows[k] = new Arrow(this, b, k);
			p.add(arrows[k], BorderLayout.WEST);
			p.add(new Label(dTable.getAttributeName(fn[k])), BorderLayout.CENTER);
			p = new Panel();
			p.setLayout(new BorderLayout());
			pright.add(p);
			slCriteria[k] = new Slider(this, tStat.getMin(fn[k]), tStat.getMax(fn[k]), refVals[k]);
			TextField tf = new TextField(StringUtil.doubleToStr(slCriteria[k].getValue(), slCriteria[k].getAbsMin(), slCriteria[k].getAbsMax()), 6);
			slCriteria[k].setTextField(tf);
			p.add(tf, BorderLayout.WEST);
			p.add(slCriteria[k], BorderLayout.CENTER);
		}
		mp.add(new Line(false));
		Panel p = new Panel();
		p.setLayout(new FlowLayout());
		mp.add(p);
		// following text: "Synchronise relative values"
		chSynchro = new Checkbox(res.getString("Sync_Rel_Val"), false);
		chSynchro.addItemListener(this);
		p.add(chSynchro);
		TextField tf = new TextField("50", 6);
		p.add(tf);
		slSynchro = new Slider(this, 0f, 100f, 50f);
		slSynchro.setTextField(tf);
		mp.add(slSynchro);
		slSynchro.setEnabled(false);
		tf.setEnabled(false);
		// following text: "Dynamic update"
		chDynaUpdateCr = new Checkbox(res.getString("Dynamic update"), false);
		chDynaUpdateCr.addItemListener(this);
		mp.add(chDynaUpdateCr);
		mp.add(new Line(false));
		p = new Panel();
		p.setLayout(new BorderLayout());
		mp.add(p);
		// following text: "Reference option(s):"
		Label l = new Label(res.getString("Roptions"));
		p.add(l, BorderLayout.WEST);
		lref = new Label("");
		p.add(lref, BorderLayout.CENTER);
		setLref();
		CheckboxGroup cbg = new CheckboxGroup();
		chStrategy = new Checkbox[3];
		// following text: "Average","Best","Worst"
		chStrategy[0] = new Checkbox(res.getString("Average"), true, cbg);
		chStrategy[1] = new Checkbox(res.getString("best"), false, cbg);
		chStrategy[2] = new Checkbox(res.getString("worst"), false, cbg);
		p = new Panel();
		p.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		mp.add(p);
		// following text: "Compare to"
		p.add(new Label(res.getString("Compare to")));
		for (Checkbox element : chStrategy) {
			p.add(element);
			element.addItemListener(this);
		}
		// following text: "values"
		p.add(new Label(res.getString("values")));
		mp.add(new Line(false));
		p = new Panel();
		p.setLayout(new FlowLayout());
		mp.add(p);
		// following text: "Tolerance"
		Label lina = new Label(res.getString("Tolerance") + ":", Label.LEFT);
		p.add(lina);
		tf = new TextField("0", 6);
		p.add(tf);
		slInaccuracy = new Slider(this, 0f, 1f, inaccuracy);
		slInaccuracy.setTextField(tf);
		mp.add(slInaccuracy);
		// following text: "Dynamic update"
		chDynaUpdateIA = new Checkbox(res.getString("Dynamic update"), false);
		chDynaUpdateIA.addItemListener(this);
		mp.add(chDynaUpdateIA);
		mp.setSize(400, 200);
		pack();
	}

	private void setLref() {
		Vector v = supervisor.getHighlighter(dTable.getEntitySetIdentifier()).getSelectedObjects();
		String str = "";
		if (v != null) {
			for (int i = 0; i < v.size(); i++) {
				String id = (String) v.elementAt(i);
				str += ((i == 0) ? "" : ", ") + dTable.getDataItemName(dTable.getObjectIndex(id));
			}
		}
		lref.setText(str);
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		if (ae.getSource().equals(slInaccuracy)) {
			inaccuracy = (float) slInaccuracy.getValue();
			updateDataTable();
			return;
		}
		if (ae.getSource().equals(slSynchro)) {
			refOptionId = null;
			setLref();
			setAllRefVals(0.01f * (float) slSynchro.getValue());
			updateDataTable();
			return;
		}
		for (int k = 0; k < fn.length; k++)
			if (ae.getSource().equals(slCriteria[k])) {
				refOptionId = null;
				setLref();
				if (chSynchro.getState()) {
					float ratio = (float) ((slCriteria[k].getValue() - slCriteria[k].getAbsMin()) / (slCriteria[k].getAbsMax() - slCriteria[k].getAbsMin()));
					if (!arrows[k].isMax()) {
						ratio = 1 - ratio;
					}
					slSynchro.setValue(100 * ratio);
					setAllRefVals(ratio);
				}
				refVals[k] = (float) slCriteria[k].getValue();
				updateDataTable();
				return;
			}
		if (ae.getSource() instanceof Arrow) {
			for (int i = 0; i < fn.length; i++)
				if (ae.getSource().equals(arrows[i])) {
					SemanticsManager sm = dTable.getSemanticsManager();
					if (sm != null) {
						if (arrows[i].isMax()) {
							sm.setAttributeIsBenefitCriterion(dTable.getAttributeId(fn[i]));
						} else {
							sm.setAttributeIsCostCriterion(dTable.getAttributeId(fn[i]));
						}
					}
					updateDataTable();
					return;
				}
		}
	}

	@Override
	public void itemStateChanged(ItemEvent ie) {
		if (ie.getSource().equals(chSynchro)) {
			if (chSynchro.getState()) {
				setAllRefVals(0.01f * (float) slSynchro.getValue());
				slSynchro.getTextField().setEnabled(true);
				slSynchro.setEnabled(true);
			} else {
				slSynchro.getTextField().setEnabled(false);
				slSynchro.setEnabled(false);
			}
			updateDataTable();
			return;
		}
		if (ie.getSource().equals(chStrategy[0]) || ie.getSource().equals(chStrategy[1]) || ie.getSource().equals(chStrategy[2])) {
			setRefValsByOptions();
			updateDataTable();
			return;
		}
		if (ie.getSource().equals(chDynaUpdateCr)) {
			for (Slider element : slCriteria) {
				element.setNAD(chDynaUpdateCr.getState());
			}
			slSynchro.setNAD(chDynaUpdateCr.getState());
		}
		if (ie.getSource().equals(chDynaUpdateIA)) {
			slInaccuracy.setNAD(chDynaUpdateIA.getState());
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		if (pce.getPropertyName().equals("Filter")) {
			updateDataTable();
			return;
		}
		if (pce.getSource().equals(dTable)) {
			if (pce.getPropertyName().equals("data_added") || pce.getPropertyName().equals("data_removed") || pce.getPropertyName().equals("data_updated")) {
				updateDataTable();
				return;
			}
			if (pce.getPropertyName().equals("filter")) {
				if (tfilter != null) {
					tfilter.removePropertyChangeListener(this);
				}
				tfilter = dTable.getObjectFilter();
				if (tfilter != null) {
					tfilter.addPropertyChangeListener(this);
				}
				updateDataTable();
				return;
			}
			if (pce.getPropertyName().equals("values")) {
				Vector v = (Vector) pce.getNewValue(); // list of changed attributes
				for (int element : fn)
					if (v.indexOf(new String(dTable.getAttributeId(element))) >= 0) {
						updateDataTable();
						return;
					}
			}
		}
	}

	/**
	* Notification from the highlighter about change of the set of objects to be
	* transiently highlighted.
	*/
	@Override
	public void highlightSetChanged(Object source, String setId, Vector hlObj) {
	}

	/**
	* Notification from the highlighter about change of the set of objects to be
	* selected (durably highlighted).
	*/
	@Override
	public void selectSetChanged(Object source, String setId, Vector hlObj) {
		setRefValsByOptions();
		updateDataTable();
	}

	private void setRefValsByOptions() {
		setLref();
		Vector v = supervisor.getHighlighter(dTable.getEntitySetIdentifier()).getSelectedObjects();
		if (v == null || v.size() == 0)
			return;
		for (int k = 0; k < refVals.length; k++)
			if (chStrategy[0].getState()) {
				refVals[k] = 0f;
			} else {
				refVals[k] = Float.NaN;
			}
		for (int i = 0; i < v.size(); i++) {
			int idx = dTable.getObjectIndex((String) v.elementAt(i));
			for (int k = 0; k < fn.length; k++) {
				float val = (float) dTable.getNumericAttrValue(fn[k], idx);
				if (chStrategy[0].getState()) {
					refVals[k] += val;
				}
				if (chStrategy[1].getState())
					if (Float.isNaN(refVals[k]) || (arrows[k].isMax() && val > refVals[k]) || (!arrows[k].isMax() && val < refVals[k])) {
						refVals[k] = val;
					}
				if (chStrategy[2].getState())
					if (Float.isNaN(refVals[k]) || (arrows[k].isMax() && val < refVals[k]) || (!arrows[k].isMax() && val > refVals[k])) {
						refVals[k] = val;
					}
			}
		}
		if (chStrategy[0].getState()) {
			for (int k = 0; k < fn.length; k++) {
				refVals[k] /= v.size();
			}
		}
		slSynchro.getTextField().setEnabled(false);
		slSynchro.setEnabled(false);
		chSynchro.setState(false);
		for (int k = 0; k < fn.length; k++)
			if (!Float.isNaN(refVals[k])) {
				slCriteria[k].setValue(refVals[k]);
			}
	}

	private void setAllRefVals(float ratio) {
		for (int k = 0; k < fn.length; k++) {
			float max = (float) slCriteria[k].getAbsMax(), min = (float) slCriteria[k].getAbsMin();
			if (arrows[k].isMax()) {
				refVals[k] = min + (max - min) * ratio;
			} else {
				refVals[k] = min + (max - min) * (1 - ratio);
			}
			slCriteria[k].setValue(refVals[k]);
		}
	}

	protected void calculateMCC() {
		// computing new column
		int vals[] = compute();
		// adding the column to the table
		Vector sourceAttrs = new Vector(fn.length, 5);
		for (int element : fn) {
			sourceAttrs.addElement(dTable.getAttributeId(element));
		}
		// following text: "Multiple Criteria Comparison"
		int idx = dTable.addDerivedAttribute(res.getString("MCC"), 'C', AttributeTypes.classify, sourceAttrs);
		dTable.getAttribute(idx).setValueListAndColors(valueList, valueColor);
		for (int i = 0; i < dTable.getDataItemCount(); i++) {
			switch (vals[i]) {
			case -1:
				dTable.getDataRecord(i).setAttrValue("", idx);
				break;
			case 0:
				dTable.getDataRecord(i).setAttrValue(valueList[0], idx);
				break;
			case 1:
				dTable.getDataRecord(i).setAttrValue(valueList[1], idx);
				break;
			case 2:
				dTable.getDataRecord(i).setAttrValue(valueList[2], idx);
				break;
			case 3:
				dTable.getDataRecord(i).setAttrValue(valueList[3], idx);
				break;
			}
		}
		AttrID = dTable.getAttributeId(idx);
		// prepare results
		Vector resultAttrs = new Vector(1, 5);
		resultAttrs.addElement(AttrID);
		Vector mapAttr = new Vector(1, 5);
		mapAttr.addElement(AttrID);
		//add attribute dependency and notify about new attribute
		attrAddedToTable(resultAttrs);
		// show results of calculations on the map
		tryShowOnMap(mapAttr, "qualitative_colour", true);
		supervisor.notifyGlobalPropertyChange(spade.analysis.system.Supervisor.eventDisplayedAttrs);
	}

	public void updateDataTable() {
		// recomputing columns
		int idx = dTable.getAttrIndex(AttrID);
		int vals[] = compute();
		for (int i = 0; i < dTable.getDataItemCount(); i++) {
			switch (vals[i]) {
			case -1:
				dTable.getDataRecord(i).setAttrValue("", idx);
				break;
			case 0:
				dTable.getDataRecord(i).setAttrValue(valueList[0], idx);
				break;
			case 1:
				dTable.getDataRecord(i).setAttrValue(valueList[1], idx);
				break;
			case 2:
				dTable.getDataRecord(i).setAttrValue(valueList[2], idx);
				break;
			case 3:
				dTable.getDataRecord(i).setAttrValue(valueList[3], idx);
				break;
			}
		}
		// inform all displays about change of values
		Vector attr = new Vector(1, 1);
		attr.addElement(AttrID);
		dTable.notifyPropertyChange("values", null, attr);
	}

	protected int[] compute() {
		// obtain Standard Deviations
		float stdd[] = null;
		if (inaccuracy > 0) {
			stdd = new float[fn.length];
			for (int i = 0; i < fn.length; i++) {
				stdd[i] = (float) tStat.getStdDev(fn[i]);
			}
		}
		int N = dTable.getDataItemCount();
		int vals[] = new int[N];
		for (int i = 0; i < N; i++) {
			vals[i] = -2;
			// check for missing values
			for (int element : fn)
				if (Double.isNaN(dTable.getNumericAttrValue(element, i))) {
					vals[i] = -1;
					break;
				}
			if (vals[i] == -1) {
				continue;
			}
			// is option <i> equivalent to the reference?
			if (refOptionId != null && refOptionId.equals(dTable.getDataItemId(i))) {
				vals[i] = 0;
				continue;
			}
			if (vals[i] == 0) {
				continue;
			}
			boolean equal = true;
			for (int k = 0; k < fn.length && equal; k++) {
				equal = Math.abs(refVals[k] - dTable.getNumericAttrValue(fn[k], i)) <= ((inaccuracy == 0) ? 0 : inaccuracy * stdd[k]);
			}
			if (equal) {
				vals[i] = 0;
				continue;
			}
			// is option <i> better than the reference?
			boolean better = true;
			for (int k = 0; k < fn.length && better; k++)
				if (arrows[k].isMax()) {
					better = dTable.getNumericAttrValue(fn[k], i) >= refVals[k] - ((inaccuracy == 0) ? 0 : inaccuracy * stdd[k]);
				} else {
					better = dTable.getNumericAttrValue(fn[k], i) <= refVals[k] + ((inaccuracy == 0) ? 0 : inaccuracy * stdd[k]);
				}
			if (better) {
				vals[i] = 1;
				continue;
			}
			// is option <i> worse than the reference?
			boolean worse = true;
			for (int k = 0; k < fn.length && worse; k++)
				if (arrows[k].isMax()) {
					worse = dTable.getNumericAttrValue(fn[k], i) <= refVals[k] + ((inaccuracy == 0) ? 0 : inaccuracy * stdd[k]);
				} else {
					worse = dTable.getNumericAttrValue(fn[k], i) >= refVals[k] - ((inaccuracy == 0) ? 0 : inaccuracy * stdd[k]);
				}
			if (worse) {
				vals[i] = 2;
				continue;
			}
			// option <i> is not comparable
			vals[i] = 3;
		}

		return vals;
	}

	@Override
	public void dispose() {
		super.dispose();
		supervisor.removeHighlightListener(this, dTable.getEntitySetIdentifier());
		dTable.removePropertyChangeListener(this);
		if (tfilter != null) {
			tfilter.removePropertyChangeListener(this);
		}
	}

}