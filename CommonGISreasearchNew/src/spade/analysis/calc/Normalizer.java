package spade.analysis.calc;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Label;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.OKDialog;
import spade.lib.lang.Language;
import spade.lib.util.IdMaker;
import spade.lib.util.IdUtil;
import spade.lib.util.StringUtil;
import spade.vis.database.AttrDescriptor;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import ui.AttributeChooser;

/**
* Normalizes values of selected attribute(s) by the value of
* their sum or by a value of some other attribute
*/
public class Normalizer extends BaseCalculator implements ActionListener, ItemListener {
	protected Checkbox cbByWhat[] = null;
	static ResourceBundle res = Language.getTextResource("spade.analysis.calc.Res");
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
	* The descriptor of the attribute to divide by
	*/
	protected AttrDescriptor divAttrDescr = null;
	/**
	* The label showing the name of the attribute to divide by
	*/
	protected Label attrNameLabel = null;

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
		Panel p = new Panel();
		p.setLayout(new ColumnLayout());
		// following text: "Normalize attributes:"
		p.add(new Label(res.getString("Normalize_attributes_"), Label.CENTER));
		int nlines = 0;
		Panel np = new Panel(new ColumnLayout());
		if (attrDescr == null || attrDescr.size() < 1) {
			for (int element : fn) {
				np.add(new Label(dTable.getAttributeName(element)));
			}
			nlines = fn.length;
		} else {
			for (int i = 0; i < attrDescr.size(); i++) {
				AttrDescriptor ad = (AttrDescriptor) attrDescr.elementAt(i);
				np.add(new Label(ad.getName()));
			}
			nlines = attrDescr.size();
		}
		if (nlines < 5) {
			p.add(new Line(false));
			p.add(np);
			p.add(new Line(false));
		} else {
			ScrollPane scp = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
			scp.add(np);
			p.add(scp);
		}
		Panel pp = new Panel();
		pp.setLayout(new BorderLayout());
		p.add(pp);
		CheckboxGroup cbg = new CheckboxGroup();
		Checkbox cbHow[] = new Checkbox[2];
		// following text: cbHow[0]=new Checkbox("% of",true,cbg),"West"
		pp.add(cbHow[0] = new Checkbox(res.getString("_of"), true, cbg), BorderLayout.WEST);
		// following text: cbHow[1]=new Checkbox("divided by",false,cbg),"East"
		pp.add(cbHow[1] = new Checkbox(res.getString("divided_by"), false, cbg), BorderLayout.EAST);
		p.add(new Line(false));
		cbg = new CheckboxGroup();
		cbByWhat = new Checkbox[3];
		// following text: cbByWhat[0]=new Checkbox("their sum",true,cbg),"West"
		p.add(cbByWhat[0] = new Checkbox(res.getString("their_sum"), true, cbg), BorderLayout.WEST);
		pp = new Panel();
		pp.setLayout(new BorderLayout());
		p.add(pp);
		// following text: cbByWhat[1]=new Checkbox("",false,cbg),"West"
		pp = new Panel(new BorderLayout());
		pp.add(cbByWhat[1] = new Checkbox(res.getString("Attribute"), false, cbg), BorderLayout.WEST);
		Button b = new Button(res.getString("Select_attribute"));
		b.setActionCommand("choose_attribute");
		b.addActionListener(this);
		pp.add(b, BorderLayout.EAST);
		p.add(pp);
		attrNameLabel = new Label("", Label.CENTER);
		p.add(attrNameLabel);
		pp = new Panel();
		pp.setLayout(new BorderLayout());
		p.add(pp);
		// following text: cbByWhat[2]=new Checkbox("number:",false,cbg),"West"
		pp.add(cbByWhat[2] = new Checkbox(res.getString("number_"), false, cbg), BorderLayout.WEST);
		for (Checkbox element : cbByWhat) {
			element.addItemListener(this);
		}
		TextField tf = new TextField("2.0", 10);
		pp.add(tf, BorderLayout.CENTER);
		p.add(new Line(false));
		// following text: "Compute percentages or ratios"
		OKDialog dlg = new OKDialog(CManager.getAnyFrame(), res.getString("Compute_percentages"), true);
		dlg.addContent(p);
		dlg.show();
		if (dlg.wasCancelled())
			return null;
		if (cbByWhat[1].getState() && divAttrDescr == null) {
			err = res.getString("attr_not_specified");
			return null;
		}
		float fDivideBy = 0f;
		if (cbByWhat[2].getState()) {
			try {
				fDivideBy = Float.valueOf(tf.getText()).floatValue();
			} catch (NumberFormatException nfe) {
				err = res.getString("ill_divide_value");
				return null;
			}
			if (fDivideBy == 0f) {
				err = res.getString("ill_divide_value");
				return null;
			}
		}
		int oldAttrN = dTable.getAttrCount();
		String endname = " " + ((cbHow[0].getState()) ? res.getString("_of") : res.getString("divided_by")) + " ";
		if (cbByWhat[1].getState()) {
			endname += divAttrDescr.getName();
		} else if (cbByWhat[0].getState()) {
			if (attrDescr != null)
				if (attrDescr.size() > 1) {
					endname += res.getString("sum") + " " + res.getString("of");
					for (int i = 0; i < attrDescr.size() && endname.length() < 60; i++) {
						AttrDescriptor ad = (AttrDescriptor) attrDescr.elementAt(i);
						if (i > 0) {
							endname += ", ";
						}
						String str = ad.getName();
						if (endname.length() + str.length() > 60) {
							endname += "...";
							break;
						} else {
							endname += str;
						}
					}
				} else {
					endname += res.getString("total");
				}
			else {
				endname += res.getString("sum") + " " + res.getString("of");
				for (int i = 0; i < fn.length && endname.length() < 60; i++) {
					if (i > 0) {
						endname += ", ";
					}
					String str = dTable.getAttributeName(fn[i]);
					if (endname.length() + str.length() > 60) {
						endname += "...";
						break;
					} else {
						endname += str;
					}
				}
			}
		} else {
			endname += fDivideBy;
		}

		int attrOrigin = (cbHow[0].getState()) ? AttributeTypes.percent : AttributeTypes.ratio;
		if (cbByWhat[0].getState())
			if (attrOrigin == AttributeTypes.percent) {
				attrOrigin = AttributeTypes.percent_in_sum;
			} else {
				attrOrigin = AttributeTypes.ratio_in_sum;
			}
		else if (cbByWhat[2].getState())
			if (attrOrigin == AttributeTypes.percent) {
				attrOrigin = AttributeTypes.percent_in_const;
			} else {
				attrOrigin = AttributeTypes.ratio_in_const;
			}

		boolean hasParamDependency = false, multipleParams = false;
		if (attrDescr != null) {
			for (int i = 0; i < attrDescr.size() && !multipleParams; i++) {
				AttrDescriptor ad = (AttrDescriptor) attrDescr.elementAt(i);
				if (ad.children != null && ad.children.size() > 1) {
					hasParamDependency = true;
					multipleParams = ad.getNVaryingParams() > 1;
				}
			}
		}
		if (multipleParams && cbByWhat[0].getState() && attrDescr.size() > 1) {
			err = res.getString("too_many_attributes") + " " + res.getString("select_one_with_many_params");
			return null;
		}
		Vector attrs = new Vector(fn.length, 5);
		int resN[] = new int[fn.length];
		for (int i = 0; i < resN.length; i++) {
			resN[i] = -1;
		}
		Vector sourceAttr = null;
		if (cbByWhat[1].getState()) {
			sourceAttr = new Vector(2, 5);
			sourceAttr.addElement(divAttrDescr.attr.getIdentifier());
		} else if (cbByWhat[0].getState()) {
			if (attrDescr == null) {
				sourceAttr = new Vector(fn.length, 5);
				for (int element : fn) {
					sourceAttr.addElement(dTable.getAttributeId(element));
				}
			} else {
				sourceAttr = new Vector(attrDescr.size(), 5);
				for (int i = 0; i < attrDescr.size(); i++) {
					AttrDescriptor ad = (AttrDescriptor) attrDescr.elementAt(i);
					sourceAttr.addElement(ad.attr.getIdentifier());
				}
			}
		} else {
			sourceAttr = new Vector(1, 5);
		}

		Vector sumDescr = null;
		if (multipleParams && cbByWhat[0].getState()) { //division by sums
			AttrDescriptor ad = (AttrDescriptor) attrDescr.elementAt(0);
			sumDescr = askAboutVaryingParams(ad, false);
			if (sumDescr == null || sumDescr.size() < 1)
				return null;
		}
		//generate new attributes
		if (sumDescr != null) {
			int k = 0;
			Attribute parent = null;
			String name = null;
			for (int i = 0; i < sumDescr.size(); i++) {
				AttrDescriptor ad = (AttrDescriptor) sumDescr.elementAt(i);
				if (parent == null) {
					name = ad.attr.getName() + endname;
					parent = new Attribute(IdMaker.makeId(name, dTable) + "_parent", AttributeTypes.real);
					parent.setName(name);
				}
				for (int j = 0; j < ad.children.size(); j++) {
					Attribute at = (Attribute) ad.children.elementAt(j);
					fn[k] = dTable.getAttrIndex(at.getIdentifier());
					resN[k] = dTable.addDerivedAttribute(name, AttributeTypes.real, attrOrigin, sourceAttr);
					Attribute child = dTable.getAttribute(resN[k]);
					for (int n = 0; n < at.getParameterCount(); n++) {
						child.addParamValPair(at.getParamValPair(n));
					}
					parent.addChild(child);
					attrs.addElement(child.getIdentifier());
					++k;
					//System.out.println(k+") "+child.getName());
				}
			}
		} else if (attrDescr == null) {
			for (int i = 0; i < fn.length; i++) {
				Attribute at = dTable.getAttribute(fn[i]);
				String name = at.getName() + endname;
				Vector attr = (Vector) sourceAttr.clone();
				attr.insertElementAt(at.getIdentifier(), 0);
				resN[i] = dTable.addDerivedAttribute(name, AttributeTypes.real, attrOrigin, attr);
			}
		} else {
			int k = 0;
			for (int i = 0; i < attrDescr.size(); i++) {
				AttrDescriptor ad = (AttrDescriptor) attrDescr.elementAt(i);
				if (ad.children == null || ad.children.size() < 2) {
					Attribute at = ad.attr;
					if (ad.children != null && ad.children.size() > 0) {
						at = (Attribute) ad.children.elementAt(0);
					}
					String name = at.getName() + endname;
					Vector attr = (Vector) sourceAttr.clone();
					attr.insertElementAt(at.getIdentifier(), 0);
					fn[k] = dTable.getAttrIndex(at.getIdentifier());
					resN[k] = dTable.addDerivedAttribute(name, AttributeTypes.real, attrOrigin, attr);
					Attribute resAttr = dTable.getAttribute(resN[k]);
					attrs.addElement(resAttr.getIdentifier());
					++k;
				} else {
					String name = ad.attr.getName() + endname;
					Attribute parent = new Attribute(IdMaker.makeId(name, dTable) + "_parent", AttributeTypes.real);
					parent.setName(name);
					parent.setIdentifier(IdUtil.makeUniqueAttrId(parent.getIdentifier(), dTable.getContainerIdentifier()));
					for (int j = 0; j < ad.children.size(); j++) {
						Attribute at = (Attribute) ad.children.elementAt(j);
						Vector attr = sourceAttr;
						if (!cbByWhat[0].getState()) {
							attr = (Vector) sourceAttr.clone();
							attr.insertElementAt(at.getIdentifier(), 0);
						}
						fn[k] = dTable.getAttrIndex(at.getIdentifier());
						resN[k] = dTable.addDerivedAttribute(name, AttributeTypes.real, attrOrigin, attr);
						Attribute child = dTable.getAttribute(resN[k]);
						for (int n = 0; n < at.getParameterCount(); n++)
							if (!ad.isInvariantParameter(at.getParamName(n))) {
								child.addParamValPair(at.getParamValPair(n));
							}
						parent.addChild(child);
						attrs.addElement(child.getIdentifier());
						++k;
					}
				}
			}
		}
		if (attrs.size() < fn.length) {
			err = res.getString("no_param_corresp");
			return null; //failed to generate new attributes
		}

		//calculations
		if (cbByWhat[0].getState()) { //divide by sum
			int k = 0;
			if (sumDescr != null) {
				for (int i = 0; i < sumDescr.size(); i++) {
					AttrDescriptor ad = (AttrDescriptor) sumDescr.elementAt(i);
					//System.out.println("Compute sums for "+ad.getName());
					int sn[] = new int[ad.children.size()], rn[] = new int[ad.children.size()];
					for (int j = 0; j < ad.children.size(); j++) {
						sn[j] = fn[k];
						rn[j] = resN[k];
						++k;
					}
					/*
					System.out.print("Source columns:");
					for (int j=0; j<sn.length; j++)
					  System.out.print(" "+sn[j]);
					System.out.println();
					System.out.print("Result columns:");
					for (int j=0; j<rn.length; j++)
					  System.out.print(" "+rn[j]);
					System.out.println();
					*/
					divideBySum(sn, rn, cbHow[0].getState());
				}
				System.out.println("done");
			} else if (hasParamDependency && attrDescr.size() > 1) {
				AttrDescriptor ad = (AttrDescriptor) attrDescr.elementAt(0);
				for (int i = 0; i < ad.children.size(); i++) {
					Attribute child = (Attribute) ad.children.elementAt(i);
					Vector vSum = new Vector(attrDescr.size(), 1);
					vSum.addElement(child);
					for (int j = 1; j < attrDescr.size(); j++) {
						AttrDescriptor ad1 = (AttrDescriptor) attrDescr.elementAt(j);
						Attribute at = ad1.findCorrespondingAttribute(child);
						if (at != null) {
							vSum.addElement(at);
						}
					}
					if (vSum.size() < 2) {
						continue;
					}
					int sn[] = new int[vSum.size()], rn[] = new int[vSum.size()];
					for (int j = 0; j < vSum.size(); j++) {
						Attribute at = (Attribute) vSum.elementAt(j);
						sn[j] = dTable.getAttrIndex(at.getIdentifier());
						rn[j] = -1;
						for (int n = 0; n < fn.length && rn[j] < 0; n++)
							if (sn[j] == fn[n]) {
								rn[j] = resN[n];
							}
					}
					divideBySum(sn, rn, cbHow[0].getState());
				}
			} else {
				divideBySum(fn, resN, cbHow[0].getState());
			}
		} else if (cbByWhat[1].getState()) { //divide by attribute
			if (divAttrDescr.children == null || divAttrDescr.children.size() < 2) {
				Attribute at = divAttrDescr.attr;
				if (divAttrDescr.children != null && divAttrDescr.children.size() > 0) {
					at = (Attribute) divAttrDescr.children.elementAt(0);
				}
				int colN = dTable.getAttrIndex(at.getIdentifier());
				divideByColumn(fn, colN, resN, cbHow[0].getState());
			} else { //for each source column, find the corresponding column to divide by
				if (!hasParamDependency) {
					err = res.getString("no_param_corresp");
					return null; //no correspondence between attribute parameters
				}
				int colsToDivBy[] = new int[fn.length];
				int k = 0;
				for (int i = 0; i < attrDescr.size(); i++) {
					AttrDescriptor ad = (AttrDescriptor) attrDescr.elementAt(i);
					if (ad.children == null || ad.children.size() < 1) {
						err = res.getString("no_param_corresp");
						return null; //no correspondence between attribute parameters
					}
					for (int j = 0; j < ad.children.size(); j++) {
						Attribute at = (Attribute) ad.children.elementAt(j);
						Attribute atDivBy = divAttrDescr.findCorrespondingAttribute(at);
						if (atDivBy == null) {
							err = res.getString("no_param_corresp");
							return null; //no correspondence between attribute parameters
						}
						colsToDivBy[k] = dTable.getAttrIndex(atDivBy.getIdentifier());
						++k;
					}
				}
				divideByColumns(fn, colsToDivBy, resN, cbHow[0].getState());
			}
		} else if (cbByWhat[2].getState()) { //divide by constant
			divideByConstant(fn, fDivideBy, resN, cbHow[0].getState());
		}
		return attrs;
	}

	/**
	* Divides the values in the specified columns (sourceCols) by the given
	* constant and writes the results to the columns with the numbers specified
	* in the resCols.
	*/
	protected void divideByConstant(int sourceCols[], float constant, int resCols[], boolean percents) {
		if (sourceCols == null || constant == 0 || dTable == null)
			return;
		int prec[] = new int[sourceCols.length];
		for (int i = 0; i < sourceCols.length; i++) {
			prec[i] = -1;
		}
		if (percents) {
			constant /= 100;
		}
		int addPrec = 1;
		float c = Math.abs(constant / 10);
		while (c > 1) {
			++addPrec;
			c /= 10;
		}
		for (int j = 0; j < dTable.getDataItemCount(); j++) {
			DataRecord rec = dTable.getDataRecord(j);
			for (int i = 0; i < sourceCols.length; i++) {
				double val = rec.getNumericAttrValue(sourceCols[i]);
				if (Double.isNaN(val)) {
					rec.setAttrValue(null, resCols[i]);
				} else {
					double res = val / constant;
					if (prec[i] < 0) {
						String stVal = rec.getAttrValueAsString(sourceCols[i]);
						int idx = stVal.indexOf('.');
						if (idx < 0) {
							idx = stVal.indexOf(',');
						}
						if (idx < 0) {
							prec[i] = addPrec;
						} else {
							prec[i] = stVal.length() - idx - 1 + addPrec;
						}
						if (prec[i] < 1) {
							prec[i] = 1;
						}
					}
					rec.setNumericAttrValue(res, StringUtil.doubleToStr(res, prec[i]), resCols[i]);
				}
			}
		}
	}

	/**
	* Divides the values in the specified columns (sourceCols) by the value in the
	* column with the given index and writes the results to the columns with the
	* numbers specified in the resCols.
	*/
	protected void divideByColumn(int sourceCols[], int colN, int resCols[], boolean percents) {
		if (sourceCols == null || colN < 0 || dTable == null)
			return;
		int prec[] = new int[sourceCols.length];
		for (int i = 0; i < sourceCols.length; i++) {
			prec[i] = -1;
		}
		int addPrec = -1;
		for (int j = 0; j < dTable.getDataItemCount(); j++) {
			DataRecord rec = dTable.getDataRecord(j);
			double vDivBy = rec.getNumericAttrValue(colN);
			if (Double.isNaN(vDivBy) || vDivBy == 0) {
				for (int i = 0; i < sourceCols.length; i++) {
					rec.setAttrValue(null, resCols[i]);
				}
			} else {
				if (percents) {
					vDivBy /= 100;
				}
				if (addPrec < 0) {
					String stVal = rec.getAttrValueAsString(colN);
					int idx = stVal.indexOf('.');
					if (idx < 0) {
						idx = stVal.indexOf(',');
					}
					if (idx < 0) {
						addPrec = stVal.length();
					} else {
						addPrec = idx;
					}
				}
				for (int i = 0; i < sourceCols.length; i++) {
					double val = rec.getNumericAttrValue(sourceCols[i]);
					if (Double.isNaN(val)) {
						rec.setAttrValue(null, resCols[i]);
					} else {
						double res = val / vDivBy;
						if (prec[i] < 0) {
							String stVal = rec.getAttrValueAsString(sourceCols[i]);
							int idx = stVal.indexOf('.');
							if (idx < 0) {
								idx = stVal.indexOf(',');
							}
							if (idx < 0) {
								prec[i] = addPrec;
							} else {
								prec[i] = stVal.length() - idx - 1 + addPrec;
							}
							if (percents) {
								prec[i] -= 2;
							}
							if (prec[i] < 1) {
								prec[i] = 1;
							}
						}
						rec.setNumericAttrValue(res, StringUtil.doubleToStr(res, prec[i]), resCols[i]);
					}
				}
			}
		}
	}

	/**
	* Divides the values in the specified columns (sourceCols) by their sum
	* and writes the results to the columns with the numbers specified in the
	* resCols.
	*/
	protected void divideBySum(int sourceCols[], int resCols[], boolean percents) {
		if (sourceCols == null || dTable == null)
			return;
		int prec[] = new int[sourceCols.length];
		for (int i = 0; i < sourceCols.length; i++) {
			prec[i] = -1;
		}
		for (int j = 0; j < dTable.getDataItemCount(); j++) {
			double sum = 0f;
			boolean noValue = false;
			DataRecord rec = dTable.getDataRecord(j);
			for (int sourceCol : sourceCols) {
				double val = rec.getNumericAttrValue(sourceCol);
				//if (Double.isNaN(val)) noValue=true;
				if (!Double.isNaN(val)) {
					sum += val;
				}
			}
			if (noValue || sum == 0) {
				for (int resCol : resCols) {
					rec.setAttrValue(null, resCol);
				}
			} else {
				for (int k = 0; k < resCols.length; k++) {
					double res = rec.getNumericAttrValue(sourceCols[k]);
					if (Double.isNaN(res)) {
						rec.setAttrValue(null, resCols[k]);
						continue;
					}
					res /= sum;
					if (percents) {
						res *= 100;
					}
					if (prec[k] < 0) {
						String stVal = rec.getAttrValueAsString(sourceCols[k]);
						int idx = stVal.indexOf('.');
						if (idx < 0) {
							idx = stVal.indexOf(',');
						}
						if (idx < 0) {
							prec[k] = 0;
						} else {
							prec[k] = stVal.length() - idx - 1;
						}
						if (percents) {
							prec[k] -= 2;
						}
						if (prec[k] < 1) {
							prec[k] = 1;
						}
					}
					rec.setNumericAttrValue(res, StringUtil.doubleToStr(res, prec[k]), resCols[k]);
				}
			}
		}
	}

	/**
	* Divides the values in the specified columns (sourceCols) by the values in the
	* columns with the index given in divCols and writes the results to the columns
	* with the numbers specified in the resCols. The dimensions of all three arrays
	* must be the same!
	*/
	protected void divideByColumns(int sourceCols[], int divCols[], int resCols[], boolean percents) {
		if (sourceCols == null || dTable == null)
			return;
		int prec[] = new int[sourceCols.length];
		for (int i = 0; i < sourceCols.length; i++) {
			prec[i] = -1;
		}
		for (int j = 0; j < dTable.getDataItemCount(); j++) {
			DataRecord rec = dTable.getDataRecord(j);
			for (int i = 0; i < sourceCols.length; i++) {
				double val = rec.getNumericAttrValue(sourceCols[i]);
				if (Double.isNaN(val)) {
					rec.setAttrValue(null, resCols[i]);
					continue;
				}
				double vDivBy = rec.getNumericAttrValue(divCols[i]);
				if (Double.isNaN(vDivBy) || vDivBy == 0) {
					rec.setAttrValue(null, resCols[i]);
					continue;
				}
				double res = val / vDivBy;
				if (percents) {
					res *= 100;
				}
				if (prec[i] < 0) {
					String stVal = rec.getAttrValueAsString(sourceCols[i]);
					int idx = stVal.indexOf('.');
					if (idx < 0) {
						idx = stVal.indexOf(',');
					}
					if (idx < 0) {
						prec[i] = 0;
					} else {
						prec[i] = stVal.length() - idx - 1;
					}
					stVal = rec.getAttrValueAsString(divCols[i]);
					int addPrec = 0;
					idx = stVal.indexOf('.');
					if (idx < 0) {
						idx = stVal.indexOf(',');
					}
					if (idx >= 0) {
						addPrec = idx;
					} else {
						addPrec = stVal.length();
					}
					prec[i] += addPrec;
					if (percents) {
						prec[i] -= 2;
					}
					if (prec[i] < 1) {
						prec[i] = 1;
					}
				}
				rec.setNumericAttrValue(res, StringUtil.doubleToStr(res, prec[i]), resCols[i]);
			}
		}
	}

	@Override
	public void itemStateChanged(ItemEvent ie) {
		if (cbByWhat[1].getState()) {
			if (divAttrDescr != null) {
				attrNameLabel.setText(divAttrDescr.getName());
			}
		} else {
			attrNameLabel.setText("");
		}
	}

	/**
	* Selection of the attribute to divide by
	*/
	@Override
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if (cmd == null)
			return;
		if (cmd.equals("choose_attribute")) {
			AttributeChooser attrSel = new AttributeChooser();
			attrSel.setSelectOnlyOne(true);
			if (attrSel.selectColumns(dTable, null, null, true, res.getString("Select_attribute"), null) == null)
				return;
			Vector v = attrSel.getAttrDescriptors();
			if (v == null || v.size() < 1)
				return;
			divAttrDescr = (AttrDescriptor) v.elementAt(0);
			cbByWhat[1].setState(true);
			attrNameLabel.setText(divAttrDescr.getName());
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
