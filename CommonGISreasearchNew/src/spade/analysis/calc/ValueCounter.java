package spade.analysis.calc;

import java.util.ResourceBundle;
import java.util.Vector;

import spade.lib.lang.Language;
import spade.lib.util.StringUtil;
import spade.vis.database.AttributeTypes;

/**
* Calculates the number of different values in given table columns (specified
* in fn) and the number of their occurrences. Returns a vector with
* identifiers of the new attributes added to the table.
*/
public class ValueCounter extends BaseCalculator {

	static ResourceBundle res = Language.getTextResource("spade.analysis.calc.Res");
	/*following text: "For a group of homogeneous attributes "+
	 * "calculates in each table row the number of occurrences of each value. "+
	 * "Produces as many new attributes as there are different values for all "+
	 * "selected attributes in total. Applicable to attributes with small number "+
	 * "of possible values."*/
	public static final String expl = res.getString("For_a_group_of") + res.getString("calculates_in_each") + res.getString("Produces_as_many_new") + res.getString("selected_attributes") + res.getString("of_possible_values_");

	/*following text:  "Select two or more homogeneous attributes "+
	* "for counting value occurrences in a table row."*/
	public static final String prompt = res.getString("Select_two_or_more") + res.getString("for_counting_value");

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
	* Performs the calculation and returns the list of identifiers of attributes
	* added to the table
	*/
	@Override
	public Vector doCalculation() {
		// listing unique values
		String vals[] = new String[10];
		int nvals = 0;
		for (int element : fn) {
			for (int j = 0; j < dTable.getDataItemCount(); j++) {
				String val = dTable.getAttrValueAsString(element, j);
				boolean found = false;
				for (int k = 0; k < nvals; k++)
					if (StringUtil.sameStrings(vals[k], val)) {
						found = true;
						break;
					}
				if (!found) {
					nvals++;
					if (nvals >= vals.length) {
						// following text: "Number of different values is to big: >="
						err = res.getString("Number_of_different") + nvals;
						return null;
					}
					vals[nvals - 1] = val;
				}
			}
		}
		// print the list
		//System.out.println("* unique values:");
		//for (int i=0; i<nvals; i++)
		//  System.out.println("* "+i+" "+vals[i]);
		// sorting the list
		for (int i = 0; i < nvals - 1; i++) {
			for (int j = i + 1; j < nvals; j++)
				if (vals[i].compareTo(vals[j]) > 0) {
					String v = vals[i];
					vals[i] = vals[j];
					vals[j] = v;
				}
		}
		// print the list
		//System.out.println("* sorted list:");
		//for (int i=0; i<nvals; i++)
		//  System.out.println("* "+i+" "+vals[i]);
		// counting values
		float n[][] = new float[nvals][];
		for (int i = 0; i < nvals; i++) {
			n[i] = new float[dTable.getDataItemCount()];
			for (int j = 0; j < dTable.getDataItemCount(); j++) {
				n[i][j] = 0;
			}
		}
		for (int element : fn) {
			for (int j = 0; j < dTable.getDataItemCount(); j++) {
				String val = dTable.getAttrValueAsString(element, j);
				int m = -1;
				for (int k = 0; k < nvals; k++)
					if (StringUtil.sameStrings(vals[k], val)) {
						m = k;
						break;
					}
				if (m >= 0) {
					n[m][j] += 1;
				}
			}
		}
		// normalizing values by number of attributes
		/*
		for (int i=0; i<nvals; i++)
		  for (int j=0; j<dTable.getDataItemCount(); j++)
		    n[i][j]=n[i][j]*100/selAttrN.size();
		*/
		Vector sourceAttr = new Vector(fn.length, 5);
		for (int element : fn) {
			sourceAttr.addElement(dTable.getAttributeId(element));
		}
		// adding data to the table
		for (int i = 0; i < nvals; i++) {
			dTable.addDerivedAttribute("N(" + vals[i] + ")", AttributeTypes.integer, AttributeTypes.value_count, sourceAttr);
		}
		for (int i = 0; i < nvals; i++) {
			for (int j = 0; j < dTable.getDataItemCount(); j++) {
				dTable.getDataRecord(j).setAttrValue("" + (int) n[i][j], dTable.getAttrCount() - nvals + i);
			}
		}
		// prepare results
		Vector resultAttrs = new Vector(nvals, 5);
		for (int i = 0; i < nvals; i++) {
			resultAttrs.addElement(dTable.getAttributeId(dTable.getAttrCount() - nvals + i));
		}
		return resultAttrs;
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