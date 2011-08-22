package spade.analysis.calc;

import java.awt.Color;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.Vector;

import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.OKDialog;
import spade.lib.lang.Language;
import spade.lib.util.DoubleArray;
import spade.lib.util.IdMaker;
import spade.lib.util.IdUtil;
import spade.lib.util.IntArray;
import spade.lib.util.NumValManager;
import spade.lib.util.StringUtil;
import spade.vis.database.AttrDescriptor;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;

/**
* Calculates user-specified percentiles (e.g. 25%, 50% (median), 80%, etc.)
* among selected table columns. Returns a vector with the identifiers of the
* new attributes added to the table.
*/
public class PercentileCalculator extends BaseCalculator {
	static ResourceBundle res = Language.getTextResource("spade.analysis.calc.Res");
	public static final String expl = res.getString("Computes_percentiles");
	public static final String prompt = res.getString("Select_attr_for_percentiles");

	/**
	* Returns the minimum number of attributes needed for this computation
	*/
	@Override
	public int getMinAttrNumber() {
		return 5;
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
		//ask the user about the required percentiles
		TextField tf = new TextField(30);
		Panel p = new Panel(new ColumnLayout());
		p.add(new Label(res.getString("Specify_percentiles")));
		p.add(new Label(res.getString("Example") + ": 0 50 80 100"));
		p.add(tf);
		Label errLab = new Label("");
		p.add(errLab);
		OKDialog okd = new OKDialog(CManager.getAnyFrame(), res.getString("Percentiles"), true);
		okd.addContent(p);
		boolean error = false;
		IntArray perc = new IntArray(20, 10);
		do {
			perc.removeAllElements();
			okd.show();
			if (okd.wasCancelled())
				return null;
			String str = tf.getText();
			if (str == null || str.trim().length() < 1) {
				error = true;
				errLab.setText(res.getString("No_percentiles"));
			} else {
				str = str.trim();
				StringTokenizer st = new StringTokenizer(str, " ,;%", false);
				while (st.hasMoreTokens() && !error) {
					String tok = st.nextToken();
					try {
						int k = Integer.valueOf(tok).intValue();
						if (k < 0 || k > 100) {
							error = true;
						} else {
							perc.addElement(k);
						}
					} catch (NumberFormatException e) {
						error = true;
					}
					;
					if (error) {
						errLab.setText(res.getString("Illegal_percentile") + ": " + tok);
						int idx = str.indexOf(tok), len = tok.length();
						if (idx == 0) {
							tf.setText(str.substring(len));
						} else if (idx > 0) {
							tf.setText(str.substring(0, idx) + str.substring(idx + len));
						}
					}
				}
			}
			if (error) {
				errLab.setBackground(Color.red.darker());
				errLab.setForeground(Color.yellow);
			}
		} while (error && perc.size() < 1);
		//check if the selected attributes depend on parameters
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
					return doParameterDependentCalculation(perc.getTrimmedArray());
			}
		}
		Vector sourceAttr = new Vector(fn.length, 5);
		for (int element : fn) {
			sourceAttr.addElement(dTable.getAttributeId(element));
		}
		Vector attrs = new Vector(perc.size(), 5);
		int resCols[] = new int[perc.size()];
		for (int np = 0; np < perc.size(); np++) {
			String name = perc.elementAt(np) + "% " + res.getString("percentile_of") + " ";
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
			// adding the column to the table
			resCols[np] = dTable.addDerivedAttribute(name, AttributeTypes.real, AttributeTypes.percentile, sourceAttr);
			attrs.addElement(dTable.getAttributeId(resCols[np]));
		}
		computePercentiles(dTable, perc.getTrimmedArray(), fn, resCols);
		return attrs;
	}

	/**
	* In the given table, calculates the percentiles specified in perc from the
	* columns specified in sourceCols and writes the results to the columns
	* specified in resCols
	*/
	protected void computePercentiles(DataTable table, int perc[], int sourceCols[], int resCols[]) {
		if (table == null || perc == null || perc.length < 1 || sourceCols == null || sourceCols.length < 2 || resCols == null || resCols.length < perc.length)
			return;
		DoubleArray values = new DoubleArray(perc.length, 1);
		int prec = -1;
		for (int rn = 0; rn < table.getDataItemCount(); rn++) {
			DataRecord rec = table.getDataRecord(rn);
			int rPrec = 0;
			values.removeAllElements();
			for (int sourceCol : sourceCols) {
				double val = rec.getNumericAttrValue(sourceCol);
				if (Double.isNaN(val)) {
					continue;
				}
				values.addElement(val);
				if (prec < 0) {
					String stVal = rec.getAttrValueAsString(sourceCol);
					int pidx = stVal.indexOf('.'), p = stVal.length();
					if (pidx < 0) {
						p = 0;
					} else {
						p -= pidx + 1;
					}
					if (p > rPrec) {
						rPrec = p;
					}
				}
			}
			if (values.size() < 1) {
				for (int resCol : resCols) {
					rec.setAttrValue(null, resCol);
				}
				continue;
			}
			if (prec >= 0) {
				rPrec = prec;
			} else if (values.size() == sourceCols.length) {
				prec = rPrec;
			}
			if (values.size() == 1) {
				String stVal = StringUtil.doubleToStr(values.elementAt(0), rPrec);
				for (int resCol : resCols) {
					rec.setNumericAttrValue(values.elementAt(0), stVal, resCol);
				}
				continue;
			}
			double fPerc[] = NumValManager.getPercentiles(values, perc);
			for (int i = 0; i < perc.length; i++) {
				rec.setNumericAttrValue(fPerc[i], StringUtil.doubleToStr(fPerc[i], rPrec), resCols[i]);
			}
		}
	}

	/**
	* Used when the attributes to be aggregated depend on parameters
	*/
	protected Vector doParameterDependentCalculation(int perc[]) {
		if (attrDescr == null || attrDescr.size() < 2)
			return null;
		AttrDescriptor ad = (AttrDescriptor) attrDescr.elementAt(0);
		Attribute parents[] = new Attribute[perc.length];
		for (int pn = 0; pn < perc.length; pn++) {
			String name = perc[pn] + "% " + res.getString("percentile_of") + " " + ad.attr.getName();
			Vector v = new Vector(10, 10);
			v.addElement(ad.attr);
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
			parents[pn] = new Attribute(IdMaker.makeId(name, dTable) + "_parent_" + pn, AttributeTypes.real);
			parents[pn].setName(name);
			parents[pn].setIdentifier(IdUtil.makeUniqueAttrId(parents[pn].getIdentifier(), dTable.getContainerIdentifier()));
		}
		Vector attrs = new Vector(ad.children.size(), 1);
		int cn[] = new int[attrDescr.size()];
		int resCols[] = new int[perc.length];
		Vector sourceAttr = new Vector(attrDescr.size(), 1);
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
			for (int pn = 0; pn < perc.length; pn++) {
				resCols[pn] = dTable.addDerivedAttribute(parents[pn].getName(), AttributeTypes.real, AttributeTypes.percentile, sourceAttr);
				Attribute child = dTable.getAttribute(resCols[pn]);
				for (int j = 0; j < at.getParameterCount(); j++)
					if (!ad.isInvariantParameter(at.getParamName(j))) {
						child.addParamValPair(at.getParamValPair(j));
					}
				parents[pn].addChild(child);
				attrs.addElement(child.getIdentifier());
			}
			computePercentiles(dTable, perc, cn, resCols);
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