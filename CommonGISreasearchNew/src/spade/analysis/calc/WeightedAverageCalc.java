package spade.analysis.calc;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.generators.PCPGenerator;
import spade.analysis.plot.FNReorder;
import spade.analysis.plot.PCPlot;
import spade.analysis.system.Supervisor;
import spade.lib.basicwin.SplitLayout;
import spade.lib.lang.Language;
import spade.vis.database.AttributeTypes;
import spade.vis.database.TableStat;

public class WeightedAverageCalc extends CalcDlg implements ActionListener {

	static ResourceBundle res = Language.getTextResource("spade.analysis.calc.Res");
	// following text: "Calculates weighted average of several attributes"
	public static final String expl = res.getString("Calculates_weighted");

	// following text: "Select at least two numeric attributes."
	public static final String prompt = res.getString("Select_at_least_two1");

	protected TableStat tStat = null;

	protected SplitLayout splL = null;

	protected WAPanel cp = null;
	protected PCPlot pcp = null;
	protected FNReorder pcpl = null;
	protected String AttrIDaverage = null;

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
		// following text: "Weighted average calculation"
		setTitle(res.getString("Weighted_average1"));
		setLayout(new BorderLayout());
		Panel mainP = new Panel();
		add(mainP, "Center");
		mainP.setLayout(splL = new SplitLayout(mainP, SplitLayout.VERT));
		Vector attr = null;
		if (fn != null) {
			attr = new Vector(fn.length, 1);
			for (int element : fn) {
				attr.addElement(dTable.getAttributeId(element));
			}
		}
		cp = new WAPanel(this, dTable, attr, false, false); // Control Panel
		cp.hideArrows();
		splL.addComponent(cp, 0.4f);
		pack();
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		if (ae.getSource() == pcpl) {
			int dragged = pcpl.getDragged(), draggedTo = pcpl.getDraggedTo();
			float groupBreak = pcpl.getGroupBreak();
			if (draggedTo <= groupBreak) {
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
				pcpFn[fn.length] = dTable.getAttrIndex(AttrIDaverage);
				pcp.setWeights(null);
				pcp.setFn(pcpFn);
				pcpl.setFn(pcpFn);
				pcpl.setGroupBreak((float) (pcpFn.length - 1.5));
				setSize(getSize().width, Math.round(getSize().height * (fn.length + 0f) / oldFnL));
				pack();
				supervisor.notifyGlobalPropertyChange(Supervisor.eventDisplayedAttrs);
			} else { // "weightsChanged" - just recompute !
			}
			updateDataTable();
		}
	}

	public void updateDataTable() {
		// recomputing columns
		int column = dTable.getAttrIndex(AttrIDaverage);
		float vals[] = compute();
		dTable.setNumericAttributeValues(vals, column);
		// inform all displays about change of values
		Vector attr = new Vector(2, 1);
		attr.addElement(AttrIDaverage);
		dTable.notifyPropertyChange("values", null, attr);
	}

	@Override
	protected void start() {
		calculateWeightedAverage();
	}

	public void calculateWeightedAverage() {
		// computing new column
		float vals[] = compute();
		// adding the column to the table
		Vector sourceAttrs = new Vector(fn.length, 5);
		for (int element : fn) {
			sourceAttrs.addElement(dTable.getAttributeId(element));
		}
		//Following text: Weighted average
		int idx = dTable.addDerivedAttribute(res.getString("Weighted_average"), AttributeTypes.real, AttributeTypes.average, sourceAttrs);
		for (int i = 0; i < dTable.getDataItemCount(); i++) {
			dTable.getDataRecord(i).setAttrValue("" + vals[i], idx);
		}
		AttrIDaverage = dTable.getAttributeId(idx);
		// set prohibited attributes for CP
		String pa[] = new String[1];
		pa[0] = AttrIDaverage;
		cp.setProhibitedAttributes(pa);
		// prepare results
		Vector resultAttrs = new Vector(2, 5);
		resultAttrs.addElement(AttrIDaverage);
		Vector mapAttr = new Vector(1, 5);
		mapAttr.addElement(AttrIDaverage);
		// add attribute dependency and notify about new attribute
		attrAddedToTable(resultAttrs);
		// show results of calculations on the map
		tryShowOnMap(mapAttr, "value_paint", true);
		PCPGenerator vg = new PCPGenerator();
		Component c = vg.constructDisplay("parallel_coordinates", supervisor, dTable, getInvolvedAttrs(), null);
		splL.addComponent(c, 2f);
		pack();
		supervisor.notifyGlobalPropertyChange(Supervisor.eventDisplayedAttrs);
		this.pcp = vg.getPCP();
		this.pcpl = vg.getFNReorder();
		pcpl.setGroupBreak((float) (fn.length - 0.5));
		pcpl.addActionListener(this);
	}

	public float[] compute() {
		float W[] = cp.getWeights();
		float results[] = new float[dTable.getDataItemCount()];
		for (int n = 0; n < dTable.getDataItemCount(); n++) {
			results[n] = 0f;
			for (int i = 0; i < fn.length; i++) {
				double v = dTable.getNumericAttrValue(fn[i], n);
				if (Double.isNaN(v)) {
					results[n] = Float.NaN;
					break;
				}
				results[n] += v * W[i];
			}
		}
		return results;
	}

	public Vector getInvolvedAttrs() {
		Vector v = new Vector(fn.length + 1);
		for (int element : fn) {
			v.addElement(new String(dTable.getAttributeId(element)));
		}
		v.addElement(new String(AttrIDaverage));
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
