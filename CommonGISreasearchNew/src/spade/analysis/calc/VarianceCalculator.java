package spade.analysis.calc;

import java.util.ResourceBundle;
import java.util.Vector;

import spade.lib.lang.Language;
import spade.lib.util.IdMaker;
import spade.lib.util.IdUtil;
import spade.vis.database.AttrDescriptor;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeTypes;
import spade.vis.database.TableStat;

/**
* Calculates variance among given table columns (specified in fn). Returns a
* vector with the identifier of the new attribute added to the table.
*/
public class VarianceCalculator extends BaseCalculator {

	static ResourceBundle res = Language.getTextResource("spade.analysis.calc.Res");
	/* following text: "Computes the statistical variance between "+
	 * "values of two or more attributes in each row of the table."*/
	public static final String expl = res.getString("Computes_the3") + res.getString("values_of_two_or_more");

	/*following text: "Select at least two numeric attributes "+
	  "for the computation of the statistical variance between their values "*/
	public static final String prompt = res.getString("Select_at_least_two") + res.getString("for_the_computation3");

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
		if (attrDescr != null) {
			if (attrDescr.size() == 1) {
				AttrDescriptor ad = (AttrDescriptor) attrDescr.elementAt(0);
				if (ad.getNVaryingParams() > 1) {
					Vector descr = askAboutVaryingParams(ad, true);
					if (descr == null)
						return null;
					attrDescr = descr;
				}
			}
			if (attrDescr.size() > 1) {
				boolean varying = false;
				for (int i = 0; i < attrDescr.size() && !varying; i++) {
					varying = ((AttrDescriptor) attrDescr.elementAt(i)).getNVaryingParams() > 0;
				}
				if (varying)
					return doParameterDependentCalculation();
			}
		}
		String name = res.getString("Variance:");
		if (attrDescr != null && attrDescr.size() == 1) {
			AttrDescriptor ad = (AttrDescriptor) attrDescr.elementAt(0);
			name += ad.getName();
		} else {
			for (int i = 0; i < fn.length && name.length() < 60; i++) {
				String str = dTable.getAttributeName(fn[i]);
				if (i > 0 && name.length() + str.length() > 60) {
					name += "; ...";
					break;
				}
				if (i > 0) {
					name += "; ";
				}
				name += str;
			}
		}
		Vector sourceAttr = new Vector(fn.length, 5);
		for (int element : fn) {
			sourceAttr.addElement(dTable.getAttributeId(element));
		}
		double sum[] = TableStat.getVarianceOfColumns(dTable, fn);
		// adding the column to the table
		int idx = dTable.addDerivedAttribute(name, AttributeTypes.real, AttributeTypes.variance, sourceAttr);
		dTable.setNumericAttributeValues(sum, idx);
		Vector attrs = new Vector(1, 5);
		attrs.addElement(dTable.getAttributeId(idx));
		return attrs;
	}

	/**
	* Used when the attributes to be aggregated depend on parameters
	*/
	protected Vector doParameterDependentCalculation() {
		if (attrDescr == null || attrDescr.size() < 2)
			return null;
		AttrDescriptor ad = (AttrDescriptor) attrDescr.elementAt(0);
		Vector v = new Vector(10, 10);
		v.addElement(ad.attr);
		String name = res.getString("Variance:") + ad.attr.getName();
		for (int i = 1; i < attrDescr.size() && name.length() < 60; i++) {
			AttrDescriptor ad1 = (AttrDescriptor) attrDescr.elementAt(i);
			if (v.contains(ad1.attr)) {
				continue;
			}
			v.addElement(ad1.attr);
			String str = ad1.attr.getName();
			if (name.length() + str.length() > 60) {
				name += "; ...";
				break;
			}
			name += "; " + str;
		}
		int cn[] = new int[attrDescr.size()];
		Vector attrs = new Vector(ad.children.size(), 1);
		Vector sourceAttr = new Vector(attrDescr.size(), 1);
		Attribute parent = new Attribute(IdMaker.makeId(name, dTable) + "_parent", AttributeTypes.real);
		parent.setName(name);
		parent.setIdentifier(IdUtil.makeUniqueAttrId(parent.getIdentifier(), dTable.getContainerIdentifier()));
		for (int i = 0; i < ad.children.size(); i++) {
			sourceAttr.removeAllElements();
			Attribute at = (Attribute) ad.children.elementAt(i);
			sourceAttr.addElement(at.getIdentifier());
			cn[0] = dTable.getAttrIndex(at.getIdentifier());
			for (int j = 1; j < attrDescr.size(); j++) {
				AttrDescriptor ad1 = (AttrDescriptor) attrDescr.elementAt(j);
				Attribute at1 = ad1.findCorrespondingAttribute(at);
				if (at1 == null || at1.hasChildren()) {
					cn[j] = -1;
				} else {
					cn[j] = dTable.getAttrIndex(at1.getIdentifier());
					sourceAttr.addElement(at1.getIdentifier());
				}
			}
			int attrIdx = dTable.addDerivedAttribute(name, AttributeTypes.real, AttributeTypes.variance, sourceAttr);
			double results[] = TableStat.getVarianceOfColumns(dTable, cn);
			dTable.setNumericAttributeValues(results, attrIdx);
			Attribute child = dTable.getAttribute(attrIdx);
			for (int j = 0; j < at.getParameterCount(); j++)
				if (!ad.isInvariantParameter(at.getParamName(j))) {
					child.addParamValPair(at.getParamValPair(j));
				}
			parent.addChild(child);
			attrs.addElement(child.getIdentifier());
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