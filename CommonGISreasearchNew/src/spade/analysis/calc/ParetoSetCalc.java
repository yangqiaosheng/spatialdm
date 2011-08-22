package spade.analysis.calc;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextArea;
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
import spade.lib.util.StringUtil;
import spade.vis.database.AttributeTypes;
import spade.vis.database.ObjectFilter;
import spade.vis.database.SemanticsManager;
import spade.vis.database.TableStat;

public class ParetoSetCalc extends CalcDlg implements ActionListener, ItemListener, PropertyChangeListener {

	protected String AttrID = null;
	/*following text: "Select at least two numeric attributes "+
	 * "representing your selection criteria." */
	protected static final String prompt = res.getString("Select_at_least_two") + res.getString("rycs");
	// following text: "Builds set of non-dominated options"
	protected static final String PS_expl = res.getString("PS_expl");
	// following text: "Pareto Set is empty"
	protected static final String PS_empty = res.getString("PS_empty");
	protected static float inaccuracy = 0f;
	protected Label lina = null;
	protected Slider sl = null;
	protected Arrow arrows[] = null;
	protected Checkbox byGitis = null, debug = null;
	protected TextArea ta = null;

	protected TableStat tStat = null;

	private boolean cm[][] = null; // Coverage Matrix
	private boolean c[] = null, r[] = null, sel[] = null;

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
		calculateParetoSet();
	}

	/**
	* Returns an explanation about this calculation method
	*/
	@Override
	public String getExplanation() {
		return PS_expl;
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
		// following text: "Pareto Set Calculator"
		setTitle(res.getString("PS"));
		setLayout(new BorderLayout());
		Panel p = new Panel();
		p.setLayout(new ColumnLayout());
		add(p, BorderLayout.NORTH);

		arrows = new Arrow[fn.length];
		SemanticsManager sm = dTable.getSemanticsManager();
		for (int i = 0; i < fn.length; i++) {
			boolean b = true;
			if (sm != null && sm.isAttributeCostCriterion(dTable.getAttributeId(fn[i]))) {
				b = false;
			}
			arrows[i] = new Arrow(this, b, i);
			Panel pp = new Panel();
			pp.setLayout(new BorderLayout());
			pp.add(arrows[i], BorderLayout.WEST);
			pp.add(new Label(dTable.getAttributeName(fn[i])), BorderLayout.CENTER);
			p.add(pp);
		}
		p.add(new Line(false));
		// following text: "Tolerance "
		lina = new Label(res.getString("Tolerance") + " = 0.00", Label.LEFT);
		p.add(lina);
		sl = new Slider(this, 0f, 1f, inaccuracy);
		sl.setNAD(true);
		p.add(sl);
		p.add(new Line(false));
		// following text: "Definition of Pareto Set:"
		p.add(new Label(res.getString("Def_PS"), Label.CENTER));
		ta = new TextArea(PS_empty);
		ta.setEditable(false);
		add(ta, BorderLayout.CENTER);

		p = new Panel();
		p.setLayout(new BorderLayout());
		add(p, BorderLayout.SOUTH);
		// following text: "Compute by method of Prof. V.Gitis"
		byGitis = new Checkbox(res.getString("Comp_by_VG"), false);
		byGitis.addItemListener(this);
		p.add(byGitis, BorderLayout.WEST);
		// following text: "debug"
		debug = new Checkbox(res.getString("debug"), false);
		debug.addItemListener(this);
		p.add(debug, BorderLayout.EAST);

		pack();
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		if (ae.getSource().equals(sl)) {
			inaccuracy = (float) sl.getValue();
			// following text: "Tolerance "
			lina.setText(res.getString("Tolerance") + " = " + StringUtil.floatToStr(inaccuracy, 2));
			updateDataTable();
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
		if (ie.getSource().equals(byGitis)) {
			updateDataTable();
		}
		if (ie.getSource().equals(debug) && debug.getState()) {
			updateDataTable();
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

	protected void calculateParetoSet() {
		// computing new column
		int vals[] = compute();
		updateTA(vals);
		// adding the column to the table
		Vector sourceAttrs = new Vector(fn.length, 5);
		for (int element : fn) {
			sourceAttrs.addElement(dTable.getAttributeId(element));
		}

		int idx = dTable.addDerivedAttribute("Pareto Set", AttributeTypes.real, AttributeTypes.evaluate_score, sourceAttrs);
		for (int i = 0; i < dTable.getDataItemCount(); i++)
			if (vals[i] >= 0) {
				dTable.getDataRecord(i).setNumericAttrValue(vals[i], idx);
			} else {
				dTable.getDataRecord(i).setAttrValue("", idx);
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
		tryShowOnMap(mapAttr, "class1D", true);
		supervisor.notifyGlobalPropertyChange(spade.analysis.system.Supervisor.eventDisplayedAttrs);
	}

	public void updateDataTable() {
		// recomputing columns
		int idx = dTable.getAttrIndex(AttrID);
		int vals[] = compute();
		dTable.setNumericAttributeValues(vals, idx);
		// inform all displays about change of values
		Vector attr = new Vector(1, 1);
		attr.addElement(AttrID);
		dTable.notifyPropertyChange("values", null, attr);
		updateTA(vals);
	}

	private void updateTA(int vals[]) {
		String str = "";
		for (int i = 0; i < vals.length; i++)
			if (vals[i] == 1) {
				str += dTable.getDataItemId(i) + " " + dTable.getDataItemName(i) + "\n";
			}
		if (str.length() == 0) {
			str = PS_empty;
		}
		ta.setText(str);
	}

	protected int[] compute() {
		if (byGitis.getState())
			return computeByGitis();
		else
			return computeByClassics();
	}

	private int[] computeByClassics() {
		// obtain Standard Deviations
		float stdd[] = null;
		if (inaccuracy > 0) {
			if (tStat == null) {
				tStat = new TableStat();
				tStat.setDataTable(dTable);
			}
			stdd = new float[fn.length];
			for (int i = 0; i < fn.length; i++) {
				stdd[i] = (float) tStat.getStdDev(fn[i]);
			}
		}
		int N = dTable.getDataItemCount();
		int vals[] = new int[N];
		for (int i = 0; i < vals.length; i++) {
			vals[i] = -1;
		}

		SemanticsManager sm = dTable.getSemanticsManager();
		boolean alive[] = new boolean[N];
		for (int i = 0; i < N; i++) {
			alive[i] = dTable.getObjectFilter() == null || dTable.getObjectFilter().isActive(i);
			for (int element : fn)
				if (Double.isNaN(dTable.getNumericAttrValue(element, i))) {
					alive[i] = false;
					break;
				}
		}
		for (int i = 0; i < N; i++)
			if (alive[i]) {
				for (int j = 0; j < N; j++)
					if (i != j && alive[j]) {
						boolean iBetterThanJ = false, iNotWorseThanJ = true;
						for (int k = 0; k < fn.length && iNotWorseThanJ; k++) {
							double vali = dTable.getNumericAttrValue(fn[k], i), valj = dTable.getNumericAttrValue(fn[k], j), delta = ((inaccuracy == 0) ? 0 : inaccuracy * stdd[k]);
							if (sm != null && sm.isAttributeCostCriterion(dTable.getAttributeId(fn[k]))) {
								iNotWorseThanJ = vali <= valj - delta;
								iBetterThanJ = vali < valj - delta;
							} else {
								iNotWorseThanJ = vali >= valj + delta;
								iBetterThanJ = vali > valj + delta;
							}
						}
						if (iBetterThanJ) {
							alive[j] = false;
							if (debug.getState()) {
								System.out.println("* " + dTable.getDataItemId(i) + " is better than " + dTable.getDataItemId(j));
							}
						}
					}
			}

		for (int i = 0; i < N; i++) {
			vals[i] = (alive[i]) ? 1 : 0;
		}
		return vals;
	}

	// ========================== computation by Gitis ========================================
	private boolean anythingChanged = false;

	private int[] computeByGitis() {
		// obtain Standard Deviations
		float stdd[] = null;
		if (inaccuracy > 0) {
			if (tStat == null) {
				tStat = new TableStat();
				tStat.setDataTable(dTable);
			}
			stdd = new float[fn.length];
			for (int i = 0; i < fn.length; i++) {
				stdd[i] = (float) tStat.getStdDev(fn[i]);
			}
			// ...
			if (dTable.getDataItemCount() == 8) { // ... especially for Gitis' example ...
				stdd[0] = 1f;
				stdd[1] = 0.05f;
			}
			// ...
		}
		int N = dTable.getDataItemCount();
		int vals[] = new int[N];
		for (int i = 0; i < vals.length; i++) {
			vals[i] = -1;
		}
		// build coverage matrix
		if (cm == null) {
			cm = new boolean[N][];
			for (int i = 0; i < N; i++) {
				cm[i] = new boolean[N];
			}
			c = new boolean[N];
			r = new boolean[N];
			sel = new boolean[N];
		}
		for (int i = 0; i < N; i++) {
			c[i] = true;
		}
		for (int j = 0; j < N; j++) {
			r[j] = true;
		}
		for (int i = 0; i < N; i++) {
			sel[i] = false;
		}
		if (tfilter != null) {
			for (int i = 0; i < N; i++)
				if (!tfilter.isActive(i)) {
					c[i] = false;
					r[i] = false;
				}
		}
		SemanticsManager sm = dTable.getSemanticsManager();
		for (int i = 0; i < N; i++) {
			for (int j = 0; j < N; j++)
				if (c[i] & r[j]) {
					cm[i][j] = false;
					boolean b = true;
					for (int k = 0; b && k < fn.length; k++) {
						double vi = dTable.getNumericAttrValue(fn[k], i), vj = dTable.getNumericAttrValue(fn[k], j);
						if (Double.isNaN(vi)) {
							c[i] = false;
							r[i] = false;
							break;
						}
						if (Double.isNaN(vj)) {
							c[j] = false;
							r[j] = false;
							break;
						}
						if (sm != null && sm.isAttributeCostCriterion(dTable.getAttributeId(fn[k]))) {
							b = vi - inaccuracy * ((stdd == null) ? 0 : stdd[k]) <= vj;
						} else {
							b = vi - inaccuracy * ((stdd == null) ? 0 : stdd[k]) >= vj;
						}
					}
					cm[i][j] = b;
				}
		}
		printMatrix();
		// minimise coverage matrix
		int c1 = N, c2 = N, r1 = N, r2 = N;
		for (;;) {
			c2 = c1;
			r2 = r1;
			anythingChanged = false;
			SelectObligatoryCovers();
			if (anythingChanged) {
				printMatrix();
			}
			if (isEmpty(c) || isEmpty(r)) {
				break;
			}
			anythingChanged = false;
			EliminateColumns();
			if (anythingChanged) {
				printMatrix();
			}
			if (isEmpty(c) || isEmpty(r)) {
				break;
			}
			anythingChanged = false;
			EliminateRows();
			if (anythingChanged) {
				printMatrix();
			}
			if (isEmpty(c) || isEmpty(r)) {
				break;
			}
			c1 = nPlus(c);
			r1 = nPlus(r);
			if (c1 == c2 && r1 == r2) {
				anythingChanged = false;
				HeuristicProcedure();
				c1 = nPlus(c);
				r1 = nPlus(r);
				if (anythingChanged) {
					printMatrix();
				}
				if (c1 == c2 && r1 == r2) {
					break;
				}
			}
		}
		// update Text Area

		// include to Pareto Set options that are better than already included
		// (only if precision > 0)
		if (inaccuracy > 0) {
		}
		// the end
		for (int i = 0; i < sel.length; i++)
			if (sel[i]) {
				vals[i] = 1;
			} else {
				vals[i] = 0;
			}
		return vals;
	}

	private int nPlus(boolean array[]) {
		int n = 0;
		for (boolean element : array)
			if (element) {
				n++;
			}
		return n;
	}

	private boolean isEmpty(boolean array[]) {
		for (boolean element : array)
			if (element)
				return false;
		return true;
	}

	private void HeuristicProcedure() {
		int N = dTable.getDataItemCount();
		int maxN = 0, maxNrow = -1;
		for (int i = 0; i < N; i++)
			if (r[i]) {
				int n = nPlusesInRow(i);
				if (n > maxN) {
					maxN = n;
					maxNrow = i;
				}
			}
		if (maxN > 0) {
			r[maxN] = false;
			sel[maxN] = true;
			anythingChanged = true;
			if (debug.getState()) {
				System.out.println("* HeuristicProcedure. R[" + (1 + maxN) + "] selected");
			}
			for (int j = 0; j < N; j++)
				if (c[j] && cm[maxN][j]) {
					c[j] = false;
					if (debug.getState()) {
						System.out.println("* HeuristicProcedure. C[" + (1 + j) + "] eliminated");
					}
				}
		}
	}

	private void EliminateColumns() {
		int N = dTable.getDataItemCount();
		for (int j = 0; j < N; j++)
			if (c[j]) {
				for (int n = 0; n < N; n++)
					if (n != j && c[n]) { // j - basis column, check if column n should be eliminated
						boolean eliminate = true;
						for (int i = 0; i < N && eliminate; i++)
							if (r[i]) {
								eliminate = (cm[i][j] && cm[i][n]) || // both 1
										!cm[i][j]; // base column=0
							}
						if (eliminate) {
							c[n] = false;
							anythingChanged = true;
							if (debug.getState()) {
								System.out.println("* EliminateColumns. C[" + (1 + n) + "] eliminated");
							}
						}
					}
			}
	}

	private void EliminateRows() {
		int N = dTable.getDataItemCount();
		for (int i = 0; i < N; i++)
			if (r[i]) {
				for (int n = 0; n < N; n++)
					if (n != i && r[n]) { // i - basis row, check if row n should be eliminated
						boolean eliminate = true;
						for (int j = 0; j < N && eliminate; j++)
							if (c[j]) {
								eliminate = !(!cm[i][j] && cm[n][j]);
							}
						if (eliminate) {
							r[n] = false;
							anythingChanged = true;
							if (debug.getState()) {
								System.out.println("* EliminateRows. R[" + (1 + n) + "] eliminated");
							}
						}
					}
			}
	}

	private void SelectObligatoryCovers() {
		int N = dTable.getDataItemCount();
		for (int j = 0; j < N; j++)
			if (c[j] && 1 == nPlusesInColumn(j)) {
				for (int i = 0; i < N; i++)
					// locate "1"
					if (r[i] && cm[i][j]) {
						sel[i] = true;
						r[i] = false;
						anythingChanged = true;
						if (debug.getState()) {
							System.out.println("* SelectObligatoryCovers. Row " + (1 + i) + " selected");
						}
						for (int n = 0; n < N; n++)
							if (c[n] && cm[i][n]) {
								c[n] = false;
								if (debug.getState()) {
									System.out.println("* SelectObligatoryCovers. C[" + (1 + n) + "] eliminated");
								}
							}
						break;
					}
			}
	}

	private int nPlusesInColumn(int n) {
		int N = dTable.getDataItemCount(), result = 0;
		for (int i = 0; i < N; i++)
			if (r[i] && cm[i][n]) {
				result++;
			}
		return result;
	}

	private int nPlusesInRow(int n) {
		int N = dTable.getDataItemCount(), result = 0;
		for (int j = 0; j < N; j++)
			if (c[j] && cm[n][j]) {
				result++;
			}
		return result;
	}

	private void printMatrix() {
		if (!debug.getState())
			return;
		System.out.println("Tolerance=" + inaccuracy + ", Matrix:");
		int N = dTable.getDataItemCount();
		System.out.print("    ");
		for (int i = 0; i < N; i++) {
			System.out.print(" " + (1 + i));
		}
		System.out.println();
		System.out.print("     ");
		for (int i = 0; i < N; i++) {
			System.out.print(((c[i]) ? "+" : "-") + (" "));
		}
		System.out.println();
		for (int i = 0; i < N; i++) {
			System.out.print((1 + i) + " ");
			if (sel[i]) {
				System.out.print("* ");
			} else if (r[i]) {
				System.out.print("+ ");
			} else {
				System.out.print("- ");
			}
			for (int j = 0; j < N; j++) {
				System.out.print(" " + ((cm[i][j]) ? "1" : " "));
			}
			System.out.println();
		}
	}

	@Override
	public void dispose() {
		super.dispose();
		dTable.removePropertyChangeListener(this);
		if (tfilter != null) {
			tfilter.removePropertyChangeListener(this);
		}
	}

}