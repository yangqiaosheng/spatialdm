package spade.analysis.calc;

import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Label;
import java.awt.Panel;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.OKDialog;
import spade.lib.lang.Language;
import spade.vis.database.AttributeTypes;
import spade.vis.database.TableStat;

/**
* Orders table rows according to the descending order of values of a numeric
* attribute and produces a new integer attribute that indicates for each row
* its order
*/
public class OrdererByValues extends BaseCalculator {
	static ResourceBundle res = Language.getTextResource("spade.analysis.calc.Res");
	/*following text: "Orders table rows according to the "+
	 * "descending order of values of a numeric attribute and produces a new "+
	 * "integer attribute that indicates for each row its order."*/
	public static final String expl = res.getString("Orders_table_rows") + res.getString("descending_order_of") + res.getString("integer_attribute");

	/* following text: "Select exactly one numeric attribute "+
	 * "for finding the order of the table rows." */
	public static final String prompt = res.getString("Select_exactly_one") + res.getString("for_finding_the_order");

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
		return 1;
	}

	/**
	* Performs the calculation and returns the list of identifiers of attributes
	* added to the table
	*/
	@Override
	public Vector doCalculation() {
		Panel p = new Panel();
		p.setLayout(new ColumnLayout());
		// following text: "Select ordering direction:"
		p.add(new Label(res.getString("Select_ordering")));
		CheckboxGroup cbg = new CheckboxGroup();
		Checkbox cbInc, cbDec;
		// following text: "Decrease"
		p.add(cbDec = new Checkbox(res.getString("Decrease"), true, cbg));
		// following text: "Increase"
		p.add(cbInc = new Checkbox(res.getString("Increase"), false, cbg));
		// following text: "Ordering"
		OKDialog dlg = new OKDialog(CManager.getAnyFrame(), res.getString("Ordering"), true);
		dlg.addContent(p);
		dlg.show();
		boolean DecVsInc = cbDec.getState();
		//following text: Decrease:Increase order
		String name = ((DecVsInc) ? (res.getString("Decrease")) : (res.getString("Increase"))) + (res.getString("order_")) + dTable.getAttributeName(fn[0]);
		int val[] = (DecVsInc) ? TableStat.getOrderOfColumn(dTable, fn[0]) : TableStat.getOrderOfColumnIncrease(dTable, fn[0]);
		// adding the column to the table
		Vector sourceAttr = new Vector(1, 5);
		sourceAttr.addElement(dTable.getAttributeId(fn[0]));
		int idx = dTable.addDerivedAttribute(name, AttributeTypes.integer, AttributeTypes.order, sourceAttr);
		for (int i = 0; i < dTable.getDataItemCount(); i++) {
			dTable.getDataRecord(i).setAttrValue(String.valueOf(val[i]), idx);
		}
		Vector attrs = new Vector(1, 5);
		attrs.addElement(dTable.getAttributeId(idx));
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