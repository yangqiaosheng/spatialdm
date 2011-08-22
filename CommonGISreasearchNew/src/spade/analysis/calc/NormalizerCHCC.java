package spade.analysis.calc;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Choice;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.OKDialog;
import spade.lib.lang.Language;
import spade.vis.database.AttributeTypes;

/**
* Normalizes values of selected attribute(s) by the value of
* their sum or by a value of some other attribute
*/
public class NormalizerCHCC extends BaseCalculator implements ItemListener {
	static ResourceBundle res = Language.getTextResource("spade.analysis.calc.Res");

	protected Checkbox cbByWhat[] = null;
	protected Choice ch = null;
	/*following text: "Divides values of each of the selected "+
	 * "attributes by values of another selected attribute, or by the sum of values "+
	 * "of these attributes, or by a fixed number. Suitable for calculation of "+
	 * "proportions of parts in a whole."*/
	public static final String expl = res.getString("Divides_values_of") + res.getString("attributes_by_values") + res.getString("of_these_attributes") + res.getString("proportions_of_parts");
	/* following text: "Select one or more numeric attributes "+
	 * "to be divided by another attribute (to be selected later on), or by the "+
	 * "sum of these attributes, or by a fixed number (to be selected later on)."*/
	public static final String prompt = res.getString("Select_one_or_more") + res.getString("to_be_divided_by") + res.getString("sum_of_these");

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
		ColumnLayout cl = new ColumnLayout();
		cl.setAlignment(ColumnLayout.Hor_Left);
		Panel p = new Panel();
		p.setLayout(cl);

		Panel pp = new Panel();
		pp.setLayout(new BorderLayout());
		p.add(pp);
		CheckboxGroup cbg = new CheckboxGroup();
		Checkbox cbHow[] = new Checkbox[2];
		pp.add(cbHow[0] = new Checkbox(res.getString("percentage"), true, cbg), "North");
		pp.add(cbHow[1] = new Checkbox(res.getString("ratio"), false, cbg), "South");

		p.add(new Label(res.getString("sel_attr"), Label.CENTER));
		List selAttr = new List();
		selAttr.validate();

		for (int element : fn) {
			selAttr.add(dTable.getAttributeName(element));
		}
		p.add(selAttr);

		p.add(new Label(res.getString("div_by"), Label.CENTER));
		cbg = new CheckboxGroup();
		cbByWhat = new Checkbox[3];
		p.add(cbByWhat[0] = new Checkbox(res.getString("their_sum"), true, cbg), "West");
		cbByWhat[0].addItemListener(this);
		pp = new Panel();
		pp.setLayout(new BorderLayout());
		p.add(pp);
		pp.add(cbByWhat[1] = new Checkbox(res.getString("another_attribute"), false, cbg), "West");
		ch = new Choice();
		ch.addItemListener(this);
		ch.add(res.getString("Select_attribute"));
		int N[] = new int[dTable.getAttrCount()];
		for (int i = 0; i < dTable.getAttrCount(); i++)
			if (dTable.isAttributeNumeric(i)) {
				ch.add(dTable.getAttributeName(i));
				N[ch.getItemCount() - 2] = i;
			}
		pp.add(ch, "Center");
		pp = new Panel();
		pp.setLayout(new BorderLayout());
		p.add(pp);
		pp.add(cbByWhat[2] = new Checkbox(res.getString("a_constant"), false, cbg), "West");
		TextField tf = new TextField("2.0", 10);
		pp.add(tf, "Center");
		OKDialog dlg = new OKDialog(CManager.getAnyFrame(), res.getString("compute"), true);
		dlg.addContent(p);
		dlg.show();
		if (dlg.wasCancelled())
			return null;
		if (cbByWhat[1].getState() && ch.getSelectedIndex() == 0)
			return null;
		float fDivideBy = 0f;
		if (cbByWhat[2].getState()) {
			try {
				fDivideBy = Float.valueOf(tf.getText()).floatValue();
			} catch (NumberFormatException nfe) {
				return null;
			}
			if (fDivideBy == 0f)
				return null;
		}
		int oldAttrN = dTable.getAttrCount(), divBy = (cbByWhat[1].getState()) ? N[ch.getSelectedIndex() - 1] : -1;
		String endname = " " + ((cbHow[0].getState()) ? res.getString("_of") + " " : res.getString("divided_by1"));
		if (cbByWhat[1].getState()) {
			endname += dTable.getAttributeName(divBy);
		} else if (cbByWhat[0].getState()) {
			endname += res.getString("sum");
		} else {
			endname += fDivideBy;
		}
		int attrOrigin = (cbHow[0].getState()) ? AttributeTypes.percent : AttributeTypes.ratio;
		Vector sourceAttr = new Vector(5, 5);
		if (cbByWhat[1].getState()) {
			sourceAttr.addElement(dTable.getAttributeId(divBy));
		} else if (cbByWhat[0].getState()) {
			if (attrOrigin == AttributeTypes.percent) {
				attrOrigin = AttributeTypes.percent_in_sum;
			} else {
				attrOrigin = AttributeTypes.ratio_in_sum;
			}
			for (int element : fn) {
				sourceAttr.addElement(dTable.getAttributeId(element));
			}
		} else if (cbByWhat[2].getState())
			if (attrOrigin == AttributeTypes.percent) {
				attrOrigin = AttributeTypes.percent_in_const;
			} else {
				attrOrigin = AttributeTypes.ratio_in_const;
			}
		for (int element : fn) {
			String name = dTable.getAttributeName(element) + endname;
			Vector attr = (Vector) sourceAttr.clone();
			attr.insertElementAt(dTable.getAttributeId(element), 0);
			dTable.addDerivedAttribute(name, AttributeTypes.real, attrOrigin, attr);
		}
		float results[][] = new float[fn.length][];
		for (int i = 0; i < fn.length; i++) {
			results[i] = new float[dTable.getDataItemCount()];
			for (int j = 0; j < dTable.getDataItemCount(); j++) {
				results[i][j] = Float.NaN;
			}
		}
		for (int j = 0; j < dTable.getDataItemCount(); j++) {
			boolean noValue = false;
			double sum = 0f;
			if (divBy >= 0) {
				sum = dTable.getNumericAttrValue(divBy, j);
				if (Double.isNaN(sum) || sum == 0f) {
					noValue = true;
				}
			} else if (fDivideBy != 0f) {
				sum = fDivideBy;
			} else {
				sum = 0f;
				for (int element : fn) {
					double v = dTable.getNumericAttrValue(element, j);
					if (Double.isNaN(v)) {
						noValue = true;
						break;
					}
					sum += v;
				}
				if (sum == 0f) {
					noValue = true;
				}
			}
			if (!noValue) {
				for (int i = 0; i < fn.length; i++) {
					double v = dTable.getNumericAttrValue(fn[i], j);
					if (Double.isNaN(v)) {
						noValue = true;
						continue;
					}
					v /= sum;
					if (cbHow[0].getState()) {
						v *= 100; // percents
					}
					results[i][j] = (float) v;
				}
			}
		}
		for (int i = 0; i < fn.length; i++) {
			dTable.setNumericAttributeValues(results[i], oldAttrN + i);
		}
		Vector attrs = new Vector(fn.length, 5);
		for (int i = 0; i < fn.length; i++) {
			attrs.addElement(dTable.getAttributeId(oldAttrN + i));
		}
		return attrs;
	}

	@Override
	public void itemStateChanged(ItemEvent ie) {
		if (ie.getSource() == ch && ch.getSelectedIndex() > 0) {
			cbByWhat[1].setState(true);
		}
		if (ie.getSource() == cbByWhat[0] && cbByWhat[0].getState()) {
			ch.select(0);
		}
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