package spade.analysis.classification;

import java.awt.Color;
import java.util.Vector;

import spade.lib.help.Helper;
import spade.lib.util.QSortAlgorithm;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeTypes;
import spade.vis.database.ThematicDataItem;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: Fraunhofer AIS.SPADE</p>
 * @author not attributable
 * @version 1.0
 * changes:
 *  => hdz, 04.2004:
 *     - applicable for few numeric values and
 *     - sorting of numerical values before creating view
 */
public class QualitativeCrossClassifier extends QualitativeClassifier {

	/**
	* Checks if this visualization method is applicable to the specified number
	* of attributes having the specified types. The possible types see in
	* @see spade.vis.database.AttributeTypes
	* hdz 2004.04 changed, for applying to few numerical values
	*/
	@Override
	public boolean isApplicable(int attrNumber, char attrTypes[]) {
		boolean testNumerical = data.isValuesCountBelow(attr, Helper.getMaxNumValuesForQualitativeVis());
		return attrNumber == 2 && (AttributeTypes.isNominalType(attrTypes[0]) || (testNumerical && AttributeTypes.isNumericType(attrTypes[0])))
				&& (AttributeTypes.isNominalType(attrTypes[1]) || (testNumerical && AttributeTypes.isNumericType(attrTypes[1])));
	}

	/**
	* Informs whether this classifier allows the classified attributes to
	* be transformed. This classifier returns false.
	*/
	@Override
	public boolean getAllowTransform() {
		return false;
	}

	protected Vector valsMult[] = null; // values of both attributes

	protected int counts[][] = null; // counts of combinations
	protected int comb[][] = null; // combinations with non-zero occurance
	protected Color colorsTwo[][] = null; // colors

	@Override
	public String getAttributeName() {
		return getAttributeName(0) + " : " + getAttributeName(1);
	}

	/**
	* Makes the attribute store the current class names and colors for further use
	* Should be empty in cross classification because there is no resulting attribute
	*/
	@Override
	protected void storeClassNamesAndColors() {
	}

	@Override
	public int getRecordClass(ThematicDataItem dit) {
		int ncl = getNClasses();
		if (ncl == 0)
			return -1;
		if (dit == null)
			return -1;
		if (valsMult == null) {
			makeValueList();
		}
		if (valsMult == null)
			return -1;
		int attrIdx0 = getAttrColumnN(0), attrIdx1 = getAttrColumnN(1);
		String val0 = dit.getAttrValueAsString(attrIdx0);
		if (val0 == null)
			return -1;
		val0 = val0.trim();
		int n0 = valsMult[0].indexOf(val0);
		String val1 = dit.getAttrValueAsString(attrIdx1);
		if (val1 == null)
			return -1;
		val1 = val1.trim();
		int n1 = valsMult[1].indexOf(val1);
		for (int i = 0; i < ncl; i++)
			if (n0 == comb[i][0] && n1 == comb[i][1])
				return i;
		return -1;
	}

	@Override
	protected void makeValueList() {
		// building lists of values of individual attributes
		valsMult = new Vector[2];
		Attribute attr[] = new Attribute[2];
		for (int n = 0; n < 2; n++) {
			attr[n] = data.getAttribute(getAttrId(n));
			if (!attr[n].hasChildren() && attr[n].isClassification()) {
				String str[] = attr[n].getValueList();
				if (str == null)
					return;
				// hdz added sorting of values if they are integers
				if (attr[n].isNumeric()) {
					QSortAlgorithm.sort_as_number(str);
				}

				valsMult[n] = new Vector(20, 5);
				for (String element : str) {
					valsMult[n].addElement(element);
				}
			} else { // extracting different values
				valsMult[n] = getAllAttrValues(n);
			}
		}
		// initialising cross-counts
		counts = new int[valsMult[0].size()][];
		for (int n0 = 0; n0 < counts.length; n0++) {
			counts[n0] = new int[valsMult[1].size()];
			for (int n1 = 0; n1 < counts[n0].length; n1++) {
				counts[n0][n1] = 0;
			}
		}
		// checking attribute values and colors
		Color colorsOne[][] = new Color[2][];
		for (int k = 0; k < 2; k++) {
			colorsOne[k] = attr[k].getValueColors();
			if (colorsOne[k] == null) {
				String v[] = new String[valsMult[k].size()];
				for (int kk = 0; kk < v.length; kk++) {
					v[kk] = (String) valsMult[k].elementAt(kk);
				}
				attr[k].setValueListAndColors(v, null);
				attr[k].setupDefaultColors();
				v = attr[k].getValueList();
				valsMult[k].removeAllElements();
				for (String element : v) {
					valsMult[k].addElement(element);
				}
				colorsOne[k] = attr[k].getValueColors();
			}
		}
		// counting combinations
		int attrIdx0 = getAttrColumnN(0), attrIdx1 = getAttrColumnN(1);
		for (int i = 0; i < data.getDataItemCount(); i++) {
			String val0 = data.getAttrValueAsString(attrIdx0, i);
			if (val0 == null) {
				continue;
			}
			val0 = val0.trim();
			int n0 = valsMult[0].indexOf(val0);
			String val1 = data.getAttrValueAsString(attrIdx1, i);
			if (val1 == null) {
				continue;
			}
			val1 = val1.trim();
			int n1 = valsMult[1].indexOf(val1);
			if (n0 >= 0 && n1 >= 0) {
				counts[n0][n1]++;
			}
		}
		int nclasses = 0; // number of non-zero counts;
		for (int[] count : counts) {
			for (int element : count)
				if (element > 0) {
					nclasses++;
				}
		}
		// building resulting classes
		vals = new Vector(nclasses, 10);
		comb = new int[nclasses][];
		int i = 0;
		for (int n0 = 0; n0 < counts.length; n0++) {
			for (int n1 = 0; n1 < counts[n0].length; n1++)
				if (counts[n0][n1] > 0) {
					comb[i] = new int[3];
					comb[i][0] = n0;
					comb[i][1] = n1;
					comb[i][2] = counts[n0][n1];
					if (valsMult[0].elementAt(n0).equals(valsMult[1].elementAt(n1))) {
						vals.addElement(valsMult[0].elementAt(n0));
					} else {
						vals.addElement(valsMult[0].elementAt(n0) + " : " + valsMult[1].elementAt(n1));
					}
					i++;
				}
		}
		// now building mixed colors
		int r0[] = new int[valsMult[0].size()], g0[] = new int[valsMult[0].size()], b0[] = new int[valsMult[0].size()], r1[] = new int[valsMult[1].size()], g1[] = new int[valsMult[1].size()], b1[] = new int[valsMult[1].size()];
		for (int k = 0; k < colorsOne[0].length; k++) {
			r0[k] = colorsOne[0][k].getRed();
			g0[k] = colorsOne[0][k].getGreen();
			b0[k] = colorsOne[0][k].getBlue();
		}
		for (int k = 0; k < colorsOne[1].length; k++) {
			r1[k] = colorsOne[1][k].getRed();
			g1[k] = colorsOne[1][k].getGreen();
			b1[k] = colorsOne[1][k].getBlue();
		}
		colors = new Vector(nclasses, 10);
		i = 0;
		for (int n0 = 0; n0 < counts.length; n0++) {
			for (int n1 = 0; n1 < counts[n0].length; n1++)
				if (counts[n0][n1] > 0) {
					float cf = 0.5f;
					int r = Math.round(cf * r0[n0] + (1 - cf) * r1[n1]), g = Math.round(cf * g0[n0] + (1 - cf) * g1[n1]), b = Math.round(cf * b0[n0] + (1 - cf) * b1[n1]);
					float hsb[] = new float[3];
					Color.RGBtoHSB(r, g, b, hsb);
					if (n0 != n1) {
						if (n0 < n1) {
							hsb[1] /= 2f;
						}
						if (n0 > n1) {
							hsb[2] /= 2f;
						}
					}
					colors.addElement(Color.getHSBColor(hsb[0], hsb[1], hsb[2]));
					i++;
				}
		}
	}

	@Override
	protected void checkValues() {
		setup();
	}

}
