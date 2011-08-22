package spade.analysis.calc;

import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Frame;
import java.awt.Label;
import java.awt.Panel;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.geocomp.EnterFormula;
import spade.analysis.geocomp.trans.Calc;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.RowLayout;
import spade.lib.basicwin.TextCanvas;
import spade.lib.lang.Language;
import spade.lib.util.IdMaker;
import spade.lib.util.IdUtil;
import spade.vis.database.AttrDescriptor;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeTypes;

/**
* Calculates sums of attributes
*/
public class FormulaCalculator extends BaseCalculator {

	static public ResourceBundle res = Language.getTextResource("spade.analysis.calc.Res");
	/*following text: "Computes the arbitrary formula "+
	  "involving arithmetic constants, values of attributes in each row "+
	  "of the table, and elementary fuctions"*/
	public static final String expl = res.getString("Computes_the4") + res.getString("involving_arithmetic") + res.getString("of_the_table_and");

	/*following text: "Select at least one numeric attribute "+
	  "for the computation of the arithmetic sum of their values."*/
	public static final String prompt = res.getString("Select_at_least_one") + res.getString("for_the_computation4");

	/**
	* Returns the minimum number of attributes needed for this computation
	*/
	@Override
	public int getMinAttrNumber() {
		return 1;
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
	* Performs the calculation and returns the list of identifiers of attributes
	* added to the table
	*/
	@Override
	public Vector doCalculation() {
		boolean treatAsUnrelated = true; //columns to be treated as unrelated
		if (attrDescr != null && attrDescr.size() < fn.length) {
			//some parameter-dependent attribute(s) was/were selected
			if (attrDescr.size() == 1) {
				//A single parameter-dependent attribute was selected.
				//Ask the user if he intends to deal with the separate columns or with
				//the attribute as a whole
				ColumnLayout cl = new ColumnLayout();
				cl.setAlignment(ColumnLayout.Hor_Stretched);
				Panel p = new Panel(cl);
				p.add(new Label(res.getString("single_param_dep_attr_selected")));
				p.add(new Label(res.getString("how_to_use")));
				p.add(new Label(""));
				RowLayout rl = new RowLayout();
				rl.setStretchLast(true);
				Panel pp = new Panel(rl);
				CheckboxGroup cbg = new CheckboxGroup();
				Checkbox wholeCB = new Checkbox("", true, cbg);
				pp.add(wholeCB);
				TextCanvas tc = new TextCanvas();
				tc.addTextLine(res.getString("attr_whole1"));
				tc.addTextLine(res.getString("attr_whole2"));
				pp.add(tc);
				p.add(pp);
				rl = new RowLayout();
				rl.setStretchLast(true);
				pp = new Panel(rl);
				Checkbox indivCB = new Checkbox("", false, cbg);
				pp.add(indivCB);
				tc = new TextCanvas();
				tc.addTextLine(((AttrDescriptor) attrDescr.elementAt(0)).children.size() + " " + res.getString("indiv_columns1"));
				tc.addTextLine(res.getString("indiv_columns2"));
				pp.add(tc);
				p.add(pp);
				p.add(new Label(""));
				OKDialog okd = new OKDialog(CManager.getAnyFrame(), res.getString("par_dep_attr"), true);
				okd.addContent(p);
				okd.show();
				if (okd.wasCancelled())
					return null;
				treatAsUnrelated = indivCB.getState();
			} else {
				treatAsUnrelated = false;
			}
		}
		Vector aNames = new Vector((treatAsUnrelated) ? fn.length : attrDescr.size(), 10);
		if (treatAsUnrelated) {
			for (int element : fn) {
				aNames.addElement(dTable.getAttributeName(element));
			}
		} else {
			for (int i = 0; i < attrDescr.size(); i++) {
				aNames.addElement(((AttrDescriptor) attrDescr.elementAt(i)).getName());
			}
		}

		EnterFormula enterFormula = new EnterFormula(aNames);
		Frame win = null;
		if (win == null) {
			win = CManager.getAnyFrame();
		}
		// following text: "Specify the formula"
		OKDialog okd = new OKDialog(win, res.getString("Specify_the_formula"), true);
		okd.addContent(enterFormula);
		okd.show();
		if (okd.wasCancelled())
			return null;
		Calc ca = new Calc(fn.length);
		if (!ca.MakeCalcTrack(enterFormula.getFormula())) {
			// following text: "Invalid formula!"
			err = res.getString("Invalid_formula_");
			return null;
		}
		int[] inUse = ca.indexInUse();
		if (inUse == null) {
			// following text: "Invalid formula!"
			err = res.getString("Invalid_formula_");
			return null;
		}

		// following text: "Formula with "
		String name = res.getString("Formula_with_");
		int nAttr = (treatAsUnrelated) ? fn.length : attrDescr.size();
		for (int i = 0; i < nAttr && name.length() < 60; i++) {
			String str = (treatAsUnrelated) ? dTable.getAttributeName(fn[i]) : ((AttrDescriptor) attrDescr.elementAt(i)).getName();
			if (i > 0)
				if (name.length() + str.length() > 60) {
					name += "; ...";
					break;
				} else {
					name += "; ";
				}
			name += str;
		}

		if (treatAsUnrelated)
			return doParameterIndependentCalculation(ca, inUse, name);
		return doParameterDependentCalculation(ca, inUse, name);
	}

	/**
	* Makes the calculation for the specified array of field numbers. Returns the
	* index of the resulting attribute.
	*/
	protected int computeFormula(int cn[], Calc ca, int inUse[], String resAttrName) {
		if (cn == null || ca == null || inUse == null)
			return -1;
		for (int element : inUse) {
			int k = element - 1;
			if (k < 0 || k >= cn.length || cn[k] < 0)
				return -1;
		}
		Vector inclAttrs = new Vector(cn.length, 1);
		for (int element : cn)
			if (element >= 0) {
				inclAttrs.addElement(dTable.getAttributeId(element));
			}
		int attrIdx = dTable.addDerivedAttribute(resAttrName, AttributeTypes.real, AttributeTypes.compute, inclAttrs);

		float results[] = new float[dTable.getDataItemCount()];
		for (int j = 0; j < results.length; j++) {
			boolean noValue = false;
			results[j] = Float.NaN;
			for (int element : inUse) {
				double v = dTable.getNumericAttrValue(cn[element - 1], j);
				if (Double.isNaN(v)) {
					noValue = true;
					break;
				}
				ca.setElement(element, v);
			}

			if (!noValue) {
				results[j] = (float) ca.useTrack();
				if (Float.isInfinite(results[j])) {
					results[j] = Float.NaN;
				}
			}
		}
		dTable.setNumericAttributeValues(results, attrIdx);
		return attrIdx;
	}

	/**
	* Used when the attributes participating in the formula do not depend on
	* parameters or parameter dependency is ignored
	*/
	protected Vector doParameterIndependentCalculation(Calc ca, int inUse[], String resAttrName) {

		int attrIdx = computeFormula(fn, ca, inUse, resAttrName);
		if (attrIdx < 0)
			return null;

		Vector attrs = new Vector(1, 5);
		attrs.addElement(dTable.getAttributeId(attrIdx));
		return attrs;
	}

	/**
	* Used when the attributes participating in the formula depend on parameters
	*/
	protected Vector doParameterDependentCalculation(Calc ca, int inUse[], String resAttrName) {
		if (attrDescr == null || attrDescr.size() < 1 || ca == null || inUse == null)
			return null;
		//check what attributes are actually in use
		boolean used[] = new boolean[attrDescr.size()];
		int nUsed = 0;
		for (int i = 0; i < attrDescr.size(); i++) {
			used[i] = false;
			for (int j = 0; j < inUse.length && !used[i]; j++) {
				used[i] = i + 1 == inUse[j];
			}
			if (used[i]) {
				++nUsed;
			}
		}
		if (nUsed < 1) { //none of the attributes is used in the formula!
			err = res.getString("Invalid_formula_");
			return null;
		}
		//find the attribute with the biggest number of children
		AttrDescriptor ad = null;
		int maxChildren = 0;
		for (int i = 0; i < attrDescr.size(); i++)
			if (used[i]) {
				AttrDescriptor ads = (AttrDescriptor) attrDescr.elementAt(i);
				if (ads.children == null)
					if (ad == null) {
						ad = ads;
					} else {
						;
					}
				else if (ads.children.size() > maxChildren) {
					ad = ads;
					maxChildren = ads.children.size();
				}
			}
		if (maxChildren < 2) //no parameter-dependent attribute is actually used in the formula
			return doParameterIndependentCalculation(ca, inUse, resAttrName);

		int cn[] = new int[attrDescr.size()];
		for (int i = 0; i < cn.length; i++) {
			cn[i] = -1;
		}
		Vector attrs = new Vector(maxChildren, 1);
		//construct the parent attribute
		Attribute parent = new Attribute(IdMaker.makeId(resAttrName, dTable) + "_parent", AttributeTypes.real);
		parent.setName(resAttrName);
		parent.setIdentifier(IdUtil.makeUniqueAttrId(parent.getIdentifier(), dTable.getContainerIdentifier()));

		for (int i = 0; i < ad.children.size(); i++) {
			Attribute at = (Attribute) ad.children.elementAt(i);
			boolean corrError = false;
			for (int j = 0; j < attrDescr.size() && !corrError; j++)
				if (used[j]) {
					AttrDescriptor ad1 = (AttrDescriptor) attrDescr.elementAt(j);
					Attribute at1 = null;
					if (ad1.equals(ad)) {
						at1 = at;
					} else {
						at1 = ad1.findCorrespondingAttribute(at);
					}
					if (at1 == null || at1.hasChildren()) {
						corrError = true;
					} else {
						cn[j] = dTable.getAttrIndex(at1.getIdentifier());
					}
				}
			if (corrError) {
				continue;
			}
			int attrIdx = computeFormula(cn, ca, inUse, resAttrName);
			Attribute child = dTable.getAttribute(attrIdx);
			for (int j = 0; j < at.getParameterCount(); j++)
				if (!ad.isInvariantParameter(at.getParamName(j))) {
					child.addParamValPair(at.getParamValPair(j));
				}
			parent.addChild(child);
			attrs.addElement(child.getIdentifier());
		}
		if (attrs.size() < 1) {
			err = res.getString("no_param_corresp");
			return null;
		}
		return attrs;
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