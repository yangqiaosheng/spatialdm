package spade.analysis.calc;

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.OKDialog;
import spade.lib.lang.Language;
import spade.lib.util.IdMaker;
import spade.lib.util.IdUtil;
import spade.lib.util.IntArray;
import spade.vis.database.AttrDescriptor;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeTypes;

/**
* Calculates differences and ratios between attributes
*/

public class ChangeCalculator extends BaseCalculator implements ActionListener {

	static ResourceBundle res = Language.getTextResource("spade.analysis.calc.Res");
	/* following text: "For two numeric attributes computes their / arithmetic
	 * difference, or ratio, or the difference in relation to one /
	 * of the attributes."*/
	public static final String expl = res.getString("For_two_numeric") + res.getString("arithmetic_difference") + res.getString("of_the_attributes_");

	/* following text: "Select exactly two numeric attributes /
	 * for the computation of their absolute or relative difference or ratio"*/
	public static final String prompt = res.getString("Select_exactly_two") + res.getString("for_the_computation1");

	private Label chla = null, chlb = null;
	private String name1 = null, name2 = null;

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
		return 2;
	}

	/**
	* Sets the numbers of the source attributes for calculations.
	* Makes a copy of the array passed as an argument because the order of
	* the attributes may change
	*/
	@Override
	public void setAttrNumbers(int attrNumbers[]) {
		if (attrNumbers != null) {
			fn = new int[attrNumbers.length];
			for (int i = 0; i < fn.length; i++) {
				fn[i] = attrNumbers[i];
			}
		}
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		if (ae.getActionCommand().equals("invert")) {
			String name = name1;
			name1 = name2;
			name2 = name;
			chla.setText("A = " + name1);
			chlb.setText("B = " + name2);
			if (fn.length == 2) {
				int i = fn[0];
				fn[0] = fn[1];
				fn[1] = i;
			}
			if (attrDescr != null && attrDescr.size() == 2) {
				Object ad = attrDescr.elementAt(1);
				attrDescr.removeElementAt(1);
				attrDescr.insertElementAt(ad, 0);
			}
		}
	}

	/**
	* Performs the calculation and returns the list of identifiers of attributes
	* added to the table
	*/
	@Override
	public Vector doCalculation() {
		if (attrDescr == null || attrDescr.size() != 2) {
			name1 = dTable.getAttributeName(fn[0]);
			name2 = dTable.getAttributeName(fn[1]);
		} else {
			AttrDescriptor ad = (AttrDescriptor) attrDescr.elementAt(0);
			name1 = ad.getName();
			ad = (AttrDescriptor) attrDescr.elementAt(1);
			name2 = ad.getName();
		}
		Panel chp = new Panel(new ColumnLayout());
		// following text: "Compute Change / Ratio / Difference"
		chp.add(new Label(res.getString("Ccrd"), Label.CENTER));
		chp.add(new Line(false));
		CheckboxGroup cbg = new CheckboxGroup();
		Checkbox cbMode[] = new Checkbox[5];
		chp.add(cbMode[0] = new Checkbox("A - B", true, cbg));
		chp.add(cbMode[1] = new Checkbox("A / B", false, cbg));
		chp.add(cbMode[2] = new Checkbox("100 * A / B", false, cbg));
		chp.add(cbMode[3] = new Checkbox("(A - B) / B", false, cbg));
		chp.add(cbMode[4] = new Checkbox("100 * (A - B) / B", false, cbg));
		// following text: "For these attributes:"
		chp.add(new Label(res.getString("Fta"), Label.LEFT));
		Panel p = new Panel();
		p.setLayout(new FlowLayout());
		Panel pp = new Panel();
		pp.setLayout(new ColumnLayout());
		pp.add(chla = new Label("A = " + name1));
		pp.add(chlb = new Label("B = " + name2));
		p.add(pp);
		pp = new Panel();
		pp.setLayout(new FlowLayout());
		// following text: "Invert"
		Button chb = new Button(res.getString("Invert"));
		chb.setActionCommand("invert");
		chb.addActionListener(this);
		pp.add(chb);
		p.add(pp);
		chp.add(p);
		chp.add(new Line(false));
		// following text: "Compute changes etc."
		OKDialog dlg = new OKDialog(CManager.getAnyFrame(), res.getString("Cc"), true);
		dlg.addContent(chp);
		dlg.show();
		if (dlg.wasCancelled())
			return null;
		int attrOrigin = AttributeTypes.difference;
		int mode = -1;
		for (int i = 0; i < cbMode.length && mode < 0; i++)
			if (cbMode[i].getState()) {
				mode = i;
			}
		String name = null;
		switch (mode) {
		case 0:
			name = name1 + " - " + name2;
			break;
		case 1:
			name = name1 + " / " + name2;
			attrOrigin = AttributeTypes.ratio;
			break;
		case 2:
			name = name1 + " % of " + name2;
			attrOrigin = AttributeTypes.percent;
			break;
		case 3:
			name = "(" + name1 + " - " + name2 + ") / " + name2;
			attrOrigin = AttributeTypes.change_ratio;
			break;
		case 4:
			name = "(" + name1 + " - " + name2 + ") % of " + name2;
			attrOrigin = AttributeTypes.change_percent;
			break;
		}
		if (fn.length == 2 || attrDescr == null || attrDescr.size() != 2) {
			Vector sourceAttr = new Vector(2, 2);
			sourceAttr.addElement(dTable.getAttributeId(fn[0]));
			sourceAttr.addElement(dTable.getAttributeId(fn[1]));
			int attrIdx = dTable.addDerivedAttribute(name, AttributeTypes.real, attrOrigin, sourceAttr);
			double results[] = computeChange(fn[0], fn[1], mode);
			dTable.setNumericAttributeValues(results, attrIdx);
			Vector attrs = new Vector(1, 1);
			attrs.addElement(dTable.getAttributeId(attrIdx));
			return attrs;
		}
		//a more complex case: two parameter-dependent attributes
		AttrDescriptor descrA = (AttrDescriptor) attrDescr.elementAt(0), descrB = (AttrDescriptor) attrDescr.elementAt(1);
		//divide all the columns into "A" and "B" columns
		Vector colA = new Vector(fn.length - 1, 1), colB = new Vector(fn.length - 1, 1);
		IntArray fnA = new IntArray(fn.length - 1, 1), fnB = new IntArray(fn.length - 1, 1);
		for (int element : fn) {
			Attribute at = dTable.getAttribute(element), parent = at.getParent();
			if ((parent == null && at.equals(descrA.attr)) || (parent != null && parent.equals(descrA.attr) && descrA.children.contains(at))) {
				colA.addElement(at);
				fnA.addElement(element);
			} else {
				colB.addElement(at);
				fnB.addElement(element);
			}
		}
		if (fnA.size() == 1) {
			Object obj = colA.elementAt(0);
			int fn = fnA.elementAt(0);
			for (int i = 1; i < fnB.size(); i++) {
				colA.addElement(obj);
				fnA.addElement(fn);
			}
		} else if (fnB.size() == 1) {
			Object obj = colB.elementAt(0);
			int fn = fnB.elementAt(0);
			for (int i = 1; i < fnA.size(); i++) {
				colB.addElement(obj);
				fnB.addElement(fn);
			}
		} else {
			// compose colB and fnB so that each element corresponds to the element
			// in colA and fnA with the same index (i.e. refers to the same parameter
			// values)
			colB.removeAllElements();
			fnB.removeAllElements();
			for (int i = colA.size() - 1; i >= 0; i--) {
				Attribute atA = (Attribute) colA.elementAt(i), atB = descrB.findCorrespondingAttribute(atA);
				if (atB == null || atB.hasChildren()) { //something is wrong
					colA.removeElementAt(i);
					fnA.removeElementAt(i);
				} else {
					colB.insertElementAt(atB, 0);
					fnB.insertElementAt(dTable.getAttrIndex(atB.getIdentifier()), 0);
				}
			}
			if (colA.size() < 1) {
				err = res.getString("no_param_corresp");
				return null;
			}
		}
		Vector attrs = new Vector(fnA.size(), 1);
		Vector sourceAttr = new Vector(2, 1);
		Attribute parent = new Attribute(IdMaker.makeId(name, dTable) + "_parent", AttributeTypes.real);
		parent.setName(name);
		parent.setIdentifier(IdUtil.makeUniqueAttrId(parent.getIdentifier(), dTable.getContainerIdentifier()));
		for (int i = 0; i < fnA.size(); i++) {
			sourceAttr.removeAllElements();
			Attribute atA = (Attribute) colA.elementAt(i), atB = (Attribute) colB.elementAt(i);
			sourceAttr.addElement(atA.getIdentifier());
			sourceAttr.addElement(atB.getIdentifier());
			int attrIdx = dTable.addDerivedAttribute(name, AttributeTypes.real, attrOrigin, sourceAttr);
			double results[] = computeChange(fnA.elementAt(i), fnB.elementAt(i), mode);
			dTable.setNumericAttributeValues(results, attrIdx);
			Attribute child = dTable.getAttribute(attrIdx);
			for (int j = 0; j < atA.getParameterCount(); j++)
				if (!descrA.isInvariantParameter(atA.getParamName(j))) {
					child.addParamValPair(atA.getParamValPair(j));
				}
			for (int j = 0; j < atB.getParameterCount(); j++) {
				String parName = atB.getParamName(j);
				if (!child.hasParameter(parName) && !descrB.isInvariantParameter(parName)) {
					child.addParamValPair(atB.getParamValPair(j));
				}
			}
			parent.addChild(child);
			attrs.addElement(child.getIdentifier());
		}
		return attrs;
	}

	/**
	* Computes the difference, ratio, etc. (depending on the value of the
	* argument mode) between the table columns with the given numbers.
	* Returns an array with the calculated numbers. The length is equal
	* to the number of rows in the table.
	*/
	protected double[] computeChange(int cnA, int cnB, int mode) {
		if (dTable == null || dTable.getDataItemCount() < 1)
			return null;
		double results[] = new double[dTable.getDataItemCount()];
		for (int j = 0; j < dTable.getDataItemCount(); j++) {
			results[j] = Double.NaN;
			double vA = dTable.getNumericAttrValue(cnA, j);
			if (Double.isNaN(vA)) {
				continue;
			}
			double vB = dTable.getNumericAttrValue(cnB, j);
			if (Double.isNaN(vB)) {
				continue;
			}
			if (vB == 0f && mode > 0) {
				continue;
			}
			double v = Double.NaN;
			switch (mode) {
			case 0:
				v = vA - vB;
				break;
			case 1:
				v = vA / vB;
				break;
			case 2:
				v = vA * 100 / vB;
				break;
			case 3:
				v = (vA - vB) / vB;
				break;
			case 4:
				v = (vA - vB) * 100 / vB;
				break;
			}
			if (!Double.isNaN(v)) {
				results[j] = v;
			}
		}
		return results;
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