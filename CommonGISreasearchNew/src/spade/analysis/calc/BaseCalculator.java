package spade.analysis.calc;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Label;
import java.awt.Panel;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.TextCanvas;
import spade.lib.lang.Language;
import spade.lib.util.VectorUtil;
import spade.vis.database.AttrDescriptor;
import spade.vis.database.Attribute;
import spade.vis.database.DataTable;

/**
* A few common data members and methods for all components implementing the
* interface Calculator
*/
public abstract class BaseCalculator implements Calculator {
	static ResourceBundle res = Language.getTextResource("spade.analysis.calc.Res");

	protected DataTable dTable = null;
	/**
	* The numbers (indexes in the table) of the source attributes for calculations
	*/
	protected int fn[] = null;
	/**
	* The descriptors of the source attributes for calculations. A descriptor
	* contains a reference to an attribute and, possibly, a list of selected
	* values of relevant parameters. The elements of the vector are instances of
	* the class spade.vis.database.AttrDescriptor.
	*/
	protected Vector attrDescr = null;
	/**
	* The error message
	*/
	protected String err = null;

	/**
	* Sets the table in which to do calculations
	*/
	@Override
	public void setTable(DataTable table) {
		dTable = table;
	}

	/**
	* Returns the table in which the calculations are done
	*/
	@Override
	public DataTable getTable() {
		return dTable;
	}

	/**
	* Sets the numbers of the source attributes for calculations
	*/
	@Override
	public void setAttrNumbers(int attrNumbers[]) {
		fn = attrNumbers;
	}

	/**
	 * Returns the numbers of the source attributes for calculations
	 */
	@Override
	public int[] getAttrNumbers() {
		return fn;
	}

	/**
	* Sets the descriptors of the source attributes for calculations. A descriptor
	* contains a reference to an attribute and, possibly, a list of selected
	* values of relevant parameters. The elements of the vector are instances of
	* the class spade.vis.database.AttrDescriptor.
	*/
	@Override
	public void setAttrDescriptors(Vector attrDescr) {
		this.attrDescr = attrDescr;
	}

	/**
	 * Should return false if the Calculator only modifies the values of the selected
	 * attributes but does not create any new attributes. By default, returns true.
	 */
	@Override
	public boolean doesCreateNewAttributes() {
		return true;
	}

	/**
	* Performs the calculation and returns the list of identifiers of attributes
	* added to the table
	*/
	@Override
	public abstract Vector doCalculation();

	/**
	* If there was an error in computation, returns the error message
	*/
	@Override
	public String getErrorMessage() {
		return err;
	}

	/**
	* This method is relevant for calculators aggregating values contained in
	* several table columns, for example, summator, average calculator,
	* variance calculator, etc.
	* If the user selected one attribute depending on two or more parameters,
	* he/she is asked which of the parameters are aggregated and which remain
	* varying. If some of the parameters must remain varying, the method
	* generates from the original attribute descriptor a set of descriptors for
	* each value or combination of values of either aggregated or varying
	* parameter(s), depending on the value of the argument splitAggregated
	* (true: each descriptor corresponds to a combination of values of the
	* aggregated parameters, false - to a value combination of varying parameters).
	* Returns a vector of the generated descriptors (in particular, it may consist
	* of the source descriptor, if all parameters need to be aggregated).
	* If the user has cancelled attribute selection, returns null.
	*/
	protected static Vector askAboutVaryingParams(AttrDescriptor ad, boolean splitAggregated) {
		if (ad == null)
			return null;
		int nVarP = ad.getNVaryingParams();
		if (nVarP < 2) {
			Vector v = new Vector(1, 1);
			v.addElement(ad);
			return v;
		}
		Panel p = new Panel(new ColumnLayout());
		p.add(new Label(res.getString("The_attribute"), Label.LEFT));
		p.add(new Label(ad.getName(), Label.CENTER));
		p.add(new Label(res.getString("dep_on_several_params"), Label.LEFT));
		TextCanvas tc = new TextCanvas();
		tc.setText(res.getString("Please_indicate"));
		p.add(tc);
		int parIdxs[] = new int[nVarP];
		Checkbox aggCB[] = new Checkbox[nVarP];
		int k = 0;
		for (int i = 0; i < ad.parVals.length; i++)
			if (ad.parVals[i].size() > 2) {
				parIdxs[k] = i;
				tc = new TextCanvas();
				tc.addTextLine(ad.parVals[i].elementAt(0).toString());
				String str = res.getString("Values") + ": " + ad.parVals[i].elementAt(1).toString();
				for (int j = 2; j < ad.parVals[i].size(); j++) {
					str += "; " + ad.parVals[i].elementAt(j).toString();
				}
				tc.addTextLine(str);
				Panel pp = new Panel(new BorderLayout());
				Panel tp = new Panel(new ColumnLayout());
				tp.add(new Label(String.valueOf(k + 1) + ")"));
				pp.add(tp, BorderLayout.WEST);
				pp.add(tc, BorderLayout.CENTER);
				CheckboxGroup cbg = new CheckboxGroup();
				aggCB[k] = new Checkbox(res.getString("aggregate"), true, cbg);
				Checkbox cb = new Checkbox(res.getString("vary"), false, cbg);
				tp = new Panel(new ColumnLayout());
				tp.add(aggCB[k]);
				tp.add(cb);
				pp.add(tp, BorderLayout.EAST);
				p.add(pp);
				++k;
			}
		OKDialog dlg = new OKDialog(CManager.getAnyFrame(), res.getString("Indicate_var_par"), true);
		dlg.addContent(p);
		boolean noAggregated = true;
		do {
			dlg.show();
			if (dlg.wasCancelled())
				return null;
			noAggregated = true;
			for (int i = 0; i < aggCB.length && noAggregated; i++) {
				noAggregated = !aggCB[i].getState();
			}
			if (noAggregated) {
				OKDialog dia = new OKDialog(CManager.getAnyFrame(), res.getString("Nothing_to_aggregate"), false);
				tc = new TextCanvas();
				tc.addTextLine(res.getString("Nothing_to_aggregate"));
				tc.addTextLine(res.getString("Aggregate_at_least_one"));
				dia.addContent(tc);
				dia.show();
			}
		} while (noAggregated);

		String parNames[] = null; //parameters used for splitting
		Vector valueLists[] = null; //all relevant values of the parameters used for splitting
		boolean split[] = null; //which of the source parameters are used for splitting

		if (splitAggregated) {
			int nAggregated = 0;
			for (Checkbox element : aggCB)
				if (element.getState()) {
					++nAggregated;
				}
			if (nAggregated == nVarP) {
				Vector v = new Vector(1, 1);
				v.addElement(ad);
				return v;
			}
			//split the original attribute descriptor into a set of descriptors,
			//according to the number of parameters involved in the aggregation and
			//their values
			parNames = new String[nAggregated];
			split = new boolean[ad.parVals.length];
			for (int i = 0; i < ad.parVals.length; i++) {
				split[i] = false;
			}
			//1) collect all relevant values of the aggregated parameters
			//   for convenience, simultaneously remember what parameters are aggregated
			valueLists = new Vector[nAggregated];
			k = 0;
			for (int i = 0; i < aggCB.length; i++)
				if (aggCB[i].getState()) {
					int idx = parIdxs[i];
					parNames[k] = ad.parVals[idx].elementAt(0).toString();
					split[idx] = true;
					valueLists[k] = new Vector(ad.parVals[idx].size() - 1, 1);
					for (int j = 1; j < ad.parVals[idx].size(); j++) {
						valueLists[k].addElement(ad.parVals[idx].elementAt(j));
					}
					++k;
				}
		} else {
			int nVarying = 0;
			for (int i = 0; i < aggCB.length; i++)
				if (!aggCB[i].getState()) {
					++nVarying;
				}
			if (nVarying == 0) {
				Vector v = new Vector(1, 1);
				v.addElement(ad);
				return v;
			}
			//split the original attribute descriptor into a set of descriptors,
			//according to the number of varying parameters and their values
			parNames = new String[nVarying];
			split = new boolean[ad.parVals.length];
			for (int i = 0; i < ad.parVals.length; i++) {
				split[i] = false;
			}
			//1) collect all relevant values of the varying parameters
			//   for convenience, simultaneously remember what parameters are aggregated
			valueLists = new Vector[nVarying];
			k = 0;
			for (int i = 0; i < aggCB.length; i++)
				if (!aggCB[i].getState()) {
					int idx = parIdxs[i];
					parNames[k] = ad.parVals[idx].elementAt(0).toString();
					split[idx] = true;
					valueLists[k] = new Vector(ad.parVals[idx].size() - 1, 1);
					for (int j = 1; j < ad.parVals[idx].size(); j++) {
						valueLists[k].addElement(ad.parVals[idx].elementAt(j));
					}
					++k;
				}
		}
		//2) get all value combinations of the aggregated parameters
		Vector combs = VectorUtil.getAllCombinations(valueLists);
		//3) for each combination, create a descriptor
		Vector descr = new Vector(combs.size(), 1);
		for (int i = 0; i < combs.size(); i++) {
			Object comb[] = (Object[]) combs.elementAt(i);
			AttrDescriptor ad1 = new AttrDescriptor();
			ad1.children = new Vector(ad.children.size(), 1);
			for (int j = 0; j < ad.children.size(); j++) {
				Attribute child = (Attribute) ad.children.elementAt(j);
				boolean ok = true;
				for (int n = 0; n < parNames.length && ok; n++) {
					ok = child.hasParamValue(parNames[n], comb[n]);
				}
				if (ok) {
					ad1.children.addElement(child);
				}
			}
			if (ad1.children.size() > 0) {
				ad1.children.trimToSize();
				descr.addElement(ad1);
			} else {
				continue;
			}
			ad1.attr = ad.attr;
			ad1.parVals = new Vector[ad.parVals.length];
			for (int j = 0; j < ad.parVals.length; j++)
				if (!split[j]) {
					ad1.parVals[j] = ad.parVals[j];
				} else {
					ad1.parVals[j] = new Vector(2, 1);
					String name = ad.parVals[j].elementAt(0).toString();
					ad1.parVals[j].addElement(name);
					int idx = -1;
					for (int n = 0; n < parNames.length && idx < 0; n++)
						if (parNames[n].equals(name)) {
							idx = n;
						}
					ad1.parVals[j].addElement(comb[idx]);
				}
		}
		/**/
		//for control, print the descriptors
		for (int i = 0; i < descr.size(); i++) {
			AttrDescriptor ad1 = (AttrDescriptor) descr.elementAt(i);
			System.out.println((i + 1) + ") " + ad1.getName());
		}
		/**/
		if (descr.size() < 2)
			return null;
		return descr;
	}
}