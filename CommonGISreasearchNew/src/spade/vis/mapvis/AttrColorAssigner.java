package spade.vis.mapvis;

import java.awt.Color;
import java.util.Vector;

import spade.lib.color.CS;
import spade.lib.util.Comparable;
import spade.lib.util.QSortAlgorithm;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.Parameter;

/**
* If a table contains parameter-dependent attributes, tries to smartly assign
* colors to individual columns, depending on the parameter values.
*/
public class AttrColorAssigner {
	/**
	* If the given table table contains parameter-dependent attributes, tries to
	* smartly assign colors to individual columns, depending on the parameter
	* values.
	*/
	static public void assignColors(AttributeDataPortion table, AttrColorHandler cHandler) {
		if (table == null || cHandler == null)
			return;
		if (table.getAttrCount() < 2 || table.getParamCount() < 1)
			return;
		Vector attr = table.getTopLevelAttributes();
		if (attr == null || attr.size() < 1)
			return;
		for (int i = 0; i < attr.size(); i++) {
			assignColorsToAttrChildren((Attribute) attr.elementAt(i), table, cHandler);
		}
	}

	/**
	* Tries to smartly assign colors to children of the givent parent attribute,
	* depending on the parameter values.
	*/
	static protected void assignColorsToAttrChildren(Attribute parent, AttributeDataPortion table, AttrColorHandler cHandler) {
		if (parent == null || table == null || cHandler == null || parent.getChildrenCount() < 2)
			return;
		int nPar = table.getParamCount();
		Vector parVals = new Vector(nPar, 1);
		for (int i = 0; i < nPar; i++) {
			parVals.addElement(null);
		}
		for (int i = 0; i < parent.getChildrenCount(); i++) {
			Attribute attr = parent.getChild(i);
			if (attr.getParameterCount() < 1)
				return;
			for (int j = 0; j < nPar; j++) {
				Parameter par = table.getParameter(j);
				Object val = attr.getParamValue(par.getName());
				if (val == null) {
					continue;
				}
				Vector v = (Vector) parVals.elementAt(j);
				if (v == null) {
					v = new Vector(par.getValueCount(), 1);
					v.addElement(val);
					parVals.setElementAt(v, j);
				} else if (!v.contains(val)) {
					v.addElement(val);
				}
			}
		}
		int nDistPar = 0, tParIdx = -1;
		for (int i = 0; i < nPar; i++) {
			Vector v = (Vector) parVals.elementAt(i);
			if (v == null || v.size() < 2) {
				parVals.setElementAt(null, i);
			} else {
				++nDistPar;
				Parameter par = table.getParameter(i);
				Vector v1 = new Vector(v.size(), 1);
				for (int j = 0; j < par.getValueCount(); j++) {
					Object value = par.getValue(j);
					if (v.contains(value)) {
						v1.addElement(value);
					}
				}
				parVals.setElementAt(v1, i);
				if (tParIdx < 0 && par.isTemporal()) {
					tParIdx = i;
				}
			}
		}
		if (nDistPar < 1 || nDistPar > 3)
			return;
		if (tParIdx < 0 && nDistPar > 2)
			return; //too many parameters
		//check which parameters are ordered
		boolean ordered[] = new boolean[nPar];
		for (int i = 0; i < nPar; i++) {
			ordered[i] = false;
			if (parVals.elementAt(i) != null) {
				Parameter par = table.getParameter(i);
				if (par.isTemporal()) {
					ordered[i] = true;
				} else {
					Vector vals = (Vector) parVals.elementAt(i);
					//check whether the values can be interpreted as ordered
					if (vals.elementAt(0) instanceof Comparable) {
						QSortAlgorithm.sort(vals);
						ordered[i] = true;
					} else if (vals.elementAt(0) instanceof String) {
						//check if all strings can be transformed into numbers
						boolean allNumbers = true;
						int ivals[] = new int[vals.size()];
						for (int j = 0; j < vals.size() && allNumbers; j++) {
							String str = (String) vals.elementAt(j);
							try {
								ivals[j] = Integer.valueOf(str).intValue();
								if (ivals[j] < 0) {
									allNumbers = false;
								}
							} catch (NumberFormatException e) {
								allNumbers = false;
							}
						}
						if (allNumbers) {
							ordered[i] = true;
							QSortAlgorithm.sort(ivals);
							Vector v = new Vector(vals.size(), 1);
							for (int ival : ivals) {
								boolean found = false;
								for (int k = 0; k < vals.size() && !found; k++)
									if (vals.elementAt(k) != null) {
										String str = (String) vals.elementAt(k);
										try {
											int n = Integer.valueOf(str).intValue();
											if (n == ival) {
												v.addElement(str);
												found = true;
												vals.setElementAt(null, k);
											}
										} catch (NumberFormatException e) {
										}
									}
							}
							parVals.setElementAt(v, i);
						}
					}
				}
			}
		}
		int nloops = 1;
		if (tParIdx >= 0) {
			++nloops;
		}
		Vector colAttrIds = new Vector(100, 100), attrColors = new Vector(100, 100);
		for (int loopN = 0; loopN < nloops; loopN++) {
			if (nDistPar == 1) {
				Parameter par = null;
				Vector vals = null;
				boolean isOrdered = false;
				for (int i = 0; i < nPar && par == null; i++)
					if (parVals.elementAt(i) != null) {
						par = table.getParameter(i);
						vals = (Vector) parVals.elementAt(i);
						isOrdered = ordered[i];
					}
				int nvals = vals.size();
				Color colors[] = new Color[nvals];
				float hue = 0.0f;
				if (isOrdered) {
					Vector attr = table.getTopLevelAttributes();
					int tAttrTotal = attr.size();
					hue = 1.0f * attr.indexOf(parent) / attr.size();
				}
				for (int i = 0; i < nvals; i++)
					if (isOrdered) {
						colors[i] = CS.getLegibleColor((i + 0.5f) / (nvals - 1), hue);
					} else {
						colors[i] = CS.getNthPureColor(i, nvals);
					}
				String parName = par.getName();
				for (int i = 0; i < parent.getChildrenCount(); i++) {
					Attribute attr = parent.getChild(i);
					Object val = attr.getParamValue(parName);
					if (val != null) {
						int idx = vals.indexOf(val);
						if (idx >= 0) {
							String id = (loopN == 0) ? attr.getIdentifier() : parent.getIdentifier() + " (" + parName + "=" + val.toString() + ")";
							//cHandler.setColorForAttribute(colors[idx],id);
							colAttrIds.addElement(id);
							attrColors.addElement(colors[idx]);
						}
					}
				}
			} else if (nDistPar == 2) {
				Parameter par1 = null, par2 = null;
				Vector vals1 = null, vals2 = null;
				boolean isOrdered1 = false, isOrdered2 = false;
				for (int i = 0; i < nPar && (par1 == null || par2 == null); i++)
					if (parVals.elementAt(i) != null)
						if (par1 == null) {
							par1 = table.getParameter(i);
							vals1 = (Vector) parVals.elementAt(i);
							isOrdered1 = ordered[i];
						} else {
							par2 = table.getParameter(i);
							vals2 = (Vector) parVals.elementAt(i);
							isOrdered2 = ordered[i];
						}
				if (isOrdered1 && !isOrdered2) {
					Parameter par = par1;
					par1 = par2;
					par2 = par;
					Vector v = vals1;
					vals1 = vals2;
					vals2 = v;
					isOrdered1 = false;
					isOrdered2 = true;
				}
				int nvals1 = vals1.size(), nvals2 = vals2.size();
				Color colors[][] = new Color[nvals1][nvals2];
				for (int i = 0; i < nvals1; i++) {
					float hue = (isOrdered1) ? 0.7f * (nvals1 - i - 1) / nvals1 : CS.getNthHue(i, nvals1);
					for (int j = 0; j < nvals2; j++) {
						colors[i][j] = CS.getLegibleColor((j + 0.5f) / nvals2, hue);
					}
				}
				String parName1 = par1.getName(), parName2 = par2.getName();
				for (int i = 0; i < parent.getChildrenCount(); i++) {
					Attribute attr = parent.getChild(i);
					Object val1 = attr.getParamValue(parName1), val2 = attr.getParamValue(parName2);
					if (val1 != null && val2 != null) {
						int idx1 = vals1.indexOf(val1), idx2 = vals2.indexOf(val2);
						if (idx1 >= 0 && idx2 >= 0) {
							String id = (loopN == 0) ? attr.getIdentifier() : parent.getIdentifier() + " (" + parName1 + "=" + val1.toString() + "; " + parName2 + "=" + val2.toString() + ")";
							//cHandler.setColorForAttribute(colors[idx1][idx2],id);
							colAttrIds.addElement(id);
							attrColors.addElement(colors[idx1][idx2]);
						}
					}
				}
			}
			if (tParIdx >= 0 && loopN == 0) {
				parVals.setElementAt(null, tParIdx);
				--nDistPar;
				if (nDistPar < 1) {
					break;
				}
			}
		}
		if (colAttrIds != null && colAttrIds.size() > 0) {
			cHandler.setColorsForAttributes(attrColors, colAttrIds);
		}
	}
}