package spade.vis.database;

import java.util.Vector;

import spade.lib.util.IdMaker;
import spade.lib.util.IntArray;
import spade.lib.util.IntRange;
import spade.time.Date;
import spade.time.TimeCount;
import spade.time.TimeMoment;
import spade.vis.spec.CaptionParamDescription;
import spade.vis.spec.DataSourceSpec;
import spade.vis.spec.ParamExpert;

/**
* Using information about caption parameters specified in the Data Source
* Specification (DataSourceSpec) of a DataTable, finds table columns containing
* values of the same attribute but referring to different parameter values.
* Creates attributes depending on parameters and "super-attributes" having these
* attributes as their children.
*/
public class CaptionParamProcessor implements TableProcessor, ParamExpert {
	/**
	* Using information about caption parameters specified in the Data Source
	* Specification (DataSourceSpec) of a DataTable, finds table columns containing
	* values of the same attribute but referring to different parameter values.
	* Creates attributes depending on parameters and "super-attributes" having
	* these attributes as their children.
	*/
	@Override
	public void processTable(DataTable table) {
		if (table == null || !table.hasData() || table.getDataSource() == null || !(table.getDataSource() instanceof DataSourceSpec))
			return;
		Vector descriptors = ((DataSourceSpec) table.getDataSource()).descriptors;
		if (descriptors == null)
			return; //no information about parameters
		for (int i = 0; i < descriptors.size(); i++)
			if (descriptors.elementAt(i) != null && (descriptors.elementAt(i) instanceof CaptionParamDescription)) {
				processParamDescr((CaptionParamDescription) descriptors.elementAt(i), table);
			}
	}

	/**
	* Processes a particular parameter description. Uses information in which
	* table columns there are parameters provided by the argument parColumns
	* (contains the names of the columns with parameters).
	*/
	protected void processParamDescr(CaptionParamDescription pd, DataTable table) {
		System.out.println("Processing parameter " + pd.paramName + " isTemporal=" + pd.isTemporal);
		if (pd.paramName == null || pd.attrs == null || pd.attrs.size() < 1 || pd.paramValues == null || pd.paramValues.size() < 1) {
			System.out.println("ERROR: illegal parameter specification!");
			return;
		}
		Parameter par = new Parameter();
		par.setName(pd.paramName);
		//if the parameter is temporal, transform its values into TimeMoments
		if (pd.isTemporal) {
			//check each scheme whether it is simple (i.e. contains a single count)
			//or compound
			boolean simpleDate = true;
			if (pd.scheme != null && pd.scheme.length() > 0) {
				int nsymb = 0;
				for (int j = 0; j < CaptionParamDescription.TIME_SYMBOLS.length && nsymb < 2; j++)
					if (pd.scheme.indexOf(CaptionParamDescription.TIME_SYMBOLS[j]) >= 0) {
						++nsymb;
					}
				simpleDate = nsymb < 2;
				//check if the scheme contains any symbols other than the legal date/time
				//symbols
				for (int j = 0; j < pd.scheme.length() && simpleDate; j++) {
					simpleDate = CaptionParamDescription.isTimeSymbol(pd.scheme.charAt(j));
				}
			}
			for (int i = 0; i < pd.paramValues.size(); i++) {
				String val = (String) pd.paramValues.elementAt(i);
				if (val == null || val.length() < 1) {
					System.out.println("ERROR: missing value " + (i + 1) + " of parameter " + pd.paramName + "!");
					continue;
				}
				TimeMoment tm = null;
				if (simpleDate) {
					tm = new TimeCount();
					if (!tm.setMoment(val)) {
						tm = null;
					}
				} else {
					Date d = new Date();
					d.setDateScheme(pd.scheme);
					if (d.setMoment(val)) {
						tm = d;
						if (pd.shownScheme != null) {
							d.setDateScheme(pd.shownScheme);
						}
					}
				}
				if (tm == null) {
					System.out.println("ERROR: illegal value " + (i + 1) + " of parameter " + pd.paramName + ": " + val + "; failed to transform to a time moment");
				} else {
					par.addValue(tm);
				}
			}
		} else {
			for (int i = 0; i < pd.paramValues.size(); i++) {
				par.addValue(pd.paramValues.elementAt(i));
			}
		}
		for (int i = 0; i < pd.attrs.size(); i++) {
			if (pd.colNumbers.elementAt(i) instanceof IntArray) {
				IntArray numbers = (IntArray) pd.colNumbers.elementAt(i);
				int first = 0;
				while (numbers.elementAt(first) < 0 && first < numbers.size()) {
					++first;
				}
				if (first >= numbers.size()) {
					continue;
				}
				Attribute parent = new Attribute(IdMaker.makeId((String) pd.attrs.elementAt(i), table), table.getAttributeType(numbers.elementAt(first)));
				parent.setName((String) pd.attrs.elementAt(i));
				String firstId = table.getAttributeId(numbers.elementAt(first));
				for (int j = first; j < numbers.size(); j++)
					if (numbers.elementAt(j) >= 0) {
						Attribute attr = table.getAttribute(numbers.elementAt(j));
						if (parent.getType() == AttributeTypes.character && attr.getType() != AttributeTypes.character) {
							parent.setType(attr.getType());
						}
						parent.addChild(attr);
						attr.addParamValPair(par.getName(), par.getValue(j % par.getValueCount()));
					}
			} else if (pd.colNumbers.elementAt(i) instanceof IntRange) {
				IntRange range = (IntRange) pd.colNumbers.elementAt(i);
				int first = range.from, last = range.to;
				if (last < 0) {
					last = table.getAttrCount() - 1;
				}
				Attribute parent = new Attribute(IdMaker.makeId((String) pd.attrs.elementAt(i), table), table.getAttributeType(first));
				parent.setName((String) pd.attrs.elementAt(i));
				String firstId = table.getAttributeId(first);
				for (int j = first; j <= last; j++) {
					Attribute attr = table.getAttribute(j);
					if (parent.getType() == AttributeTypes.character && attr.getType() != AttributeTypes.character) {
						parent.setType(attr.getType());
					}
					parent.addChild(attr);
					attr.addParamValPair(par.getName(), par.getValue((j - first) % par.getValueCount()));
				}
			}
		}
		if (pd.isTemporal) {
			par.sortValues();
		} else if (pd.mustBeOrdered()) {
			Vector order = pd.getValueOrder();
			if (order == null || order.size() < 2) {
				par.sortValues();
			} else {
				par.setValueOrder(order);
			}
		}
		table.addParameter(par);
	}

	/**
	* For the given table, creates a description of its parameters relevant for
	* the specified columns. The columns are specified through their identifiers.
	* If no column identifiers are given, all table columns are considered.
	* Returns a vector of descriptions (instances of spade.vis.spec.CaptionParamDescription).
	*/
	@Override
	public Vector describeParameters(AttributeDataPortion table, Vector colIds) {
		if (table == null || table.getParamCount() < 1 || table.getAttrCount() < 1)
			return null;
		if (colIds != null)
			if (colIds.size() < 1) {
				colIds = null;
			} else if (colIds.size() < 2)
				return null;
		int nattr = (colIds == null) ? table.getAttrCount() : colIds.size();
		Vector parents = new Vector(nattr, 1); //the parents of the column attributes
		//for each parent - indexes of the corresponding children columns
		Vector childNums = new Vector(nattr, 1);
		for (int i = 0; i < nattr; i++) {
			int idx = (colIds == null) ? i : table.getAttrIndex((String) colIds.elementAt(i));
			if (idx < 0) {
				continue;
			}
			Attribute at = table.getAttribute(idx), parent = at.getParent();
			if (parent != null) {
				int pidx = parents.indexOf(parent);
				IntArray chidxs = null;
				if (pidx < 0) {
					parents.addElement(parent);
					chidxs = new IntArray(nattr, 1);
					childNums.addElement(chidxs);
				} else {
					chidxs = (IntArray) childNums.elementAt(pidx);
				}
				chidxs.addElement(idx);
			}
		}
		if (parents.size() < 1)
			return null;
		//Remove parents having only one child
		//For parents having more children retrieve distinguishing parameters and values
		Vector parVals[] = new Vector[table.getParamCount()];
		for (int i = 0; i < table.getParamCount(); i++) {
			parVals[i] = null;
		}
		for (int i = parents.size() - 1; i >= 0; i--) {
			IntArray chidxs = (IntArray) childNums.elementAt(i);
			int nChildren = chidxs.size();
			if (nChildren < 2) {
				parents.removeElementAt(i);
				childNums.removeElementAt(i);
			} else {
				Vector ids = new Vector(nChildren, 1);
				for (int j = 0; j < nChildren; j++) {
					ids.addElement(table.getAttributeId(chidxs.elementAt(j)));
				}
				Vector pvv = table.getDistinguishingParameters(ids);
				if (pvv == null || pvv.size() < 1) {
					parents.removeElementAt(i);
					childNums.removeElementAt(i);
				} else {
					for (int j = 0; j < pvv.size(); j++) {
						Vector pv = (Vector) pvv.elementAt(j);
						if (pv == null || pv.size() < 3) {
							continue;
						}
						String parName = (String) pv.elementAt(0);
						int paridx = -1;
						for (int k = 0; k < table.getParamCount() && paridx < 0; k++)
							if (parName.equals(table.getParameter(k).getName())) {
								paridx = k;
							}
						if (paridx < 0) {
							continue;
						}
						pv.removeElementAt(0); //removed parameter name
						if (parVals[paridx] == null) {
							parVals[paridx] = pv;
						} else {
							for (int k = 0; k < pv.size(); k++)
								if (!parVals[paridx].contains(pv.elementAt(k))) {
									parVals[paridx].addElement(pv.elementAt(k));
								}
						}
					}
				}
			}
		}
		if (parents.size() < 1)
			return null; //no attributes differing by parameters
		int npar = 0;
		;
		for (Vector parVal : parVals)
			if (parVal != null) {
				++npar;
			}
		if (npar < 1)
			return null; //no distinguishing parameters found
		Vector parDescr = new Vector(npar, 1);
		for (int i = 0; i < parVals.length; i++)
			if (parVals[i] != null) {
				Parameter par = table.getParameter(i);
				CaptionParamDescription dsc = new CaptionParamDescription();
				dsc.paramName = par.getName();
				dsc.isTemporal = par.isTemporal();
				if (dsc.isTemporal && (parVals[i].elementAt(0) instanceof Date)) {
					Date d = (Date) parVals[i].elementAt(0);
					dsc.scheme = d.scheme;
				}
				if (parVals[i].elementAt(0) instanceof String) {
					dsc.paramValues = parVals[i];
				} else {
					dsc.paramValues = new Vector(parVals[i].size(), 1);
					for (int j = 0; j < parVals[i].size(); j++) {
						dsc.paramValues.addElement(parVals[i].elementAt(j).toString());
					}
				}
				dsc.attrs = new Vector(parents.size(), 1);
				dsc.colNumbers = new Vector(parents.size(), 1);
				for (int j = 0; j < parents.size(); j++) {
					IntArray chidxs = (IntArray) childNums.elementAt(j);
					//find the maximum number of columns referring to the same parameter value
					int maxNOccur = 0;
					for (int k = 0; k < parVals[i].size(); k++) {
						int nOccur = 0;
						for (int n = 0; n < chidxs.size(); n++) {
							Attribute at = table.getAttribute(chidxs.elementAt(n));
							if (at.hasParamValue(par.getName(), parVals[i].elementAt(k))) {
								++nOccur;
							}
						}
						if (nOccur > maxNOccur) {
							maxNOccur = nOccur;
						}
					}
					if (maxNOccur < 1) {
						continue;
					}
					IntArray cn = new IntArray(chidxs.size(), 1);
					boolean added[] = new boolean[chidxs.size()];
					for (int k = 0; k < chidxs.size(); k++) {
						added[k] = false;
					}
					for (int nocc = 0; nocc < maxNOccur; nocc++) {
						for (int k = 0; k < parVals[i].size(); k++) {
							boolean found = false;
							for (int n = 0; n < chidxs.size() && !found; n++)
								if (!added[n]) {
									Attribute at = table.getAttribute(chidxs.elementAt(n));
									found = at.hasParamValue(par.getName(), parVals[i].elementAt(k));
									if (found) {
										added[n] = true;
										int idx = chidxs.elementAt(n);
										if (colIds != null) {
											//find the column index among the selected columns
											String id = table.getAttributeId(idx);
											idx = colIds.indexOf(id);
										}
										if (idx >= 0) {
											cn.addElement(idx);
										} else {
											found = false;
										}
									}
								}
							if (!found) {
								cn.addElement(-1);
							}
						}
					}
					dsc.attrs.addElement(((Attribute) parents.elementAt(j)).getName());
					//check if the list of column numbers can be replaced by a range
					boolean range = true;
					for (int k = 1; k < cn.size() && range; k++) {
						range = cn.elementAt(k) == cn.elementAt(k - 1) + 1;
					}
					if (!range) {
						dsc.colNumbers.addElement(cn);
					} else {
						IntRange r = new IntRange();
						r.from = cn.elementAt(0);
						r.to = cn.elementAt(cn.size() - 1);
						dsc.colNumbers.addElement(r);
					}
				}
				if (dsc.attrs.size() > 0) {
					parDescr.addElement(dsc);
				}
			}
		if (parDescr.size() < 1)
			return null;
		return parDescr;
	}
}