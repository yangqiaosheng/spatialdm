package spade.analysis.tools.distances;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.util.HashMap;
import java.util.Vector;

import spade.lib.basicwin.CManager;
import spade.lib.basicwin.Dialogs;
import spade.lib.basicwin.OKDialog;
import spade.lib.util.StringUtil;
import spade.time.TimeMoment;
import spade.vis.database.Attribute;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.database.ThematicDataItem;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Jan 17, 2011
 * Time: 1:03:36 PM
 * Extends clustering in LayerClusterer by involving thematic attributes
 */
public class DistanceByAttributes {
	/**
	 * The layer in which objects are clustered
	 */
	protected DGeoLayer layer = null;
	/**
	 * The table with thematic data associated with the layer
	 */
	protected DataTable table = null;
	/**
	 * The indexes of the table columns with the attribute values
	 */
	protected int colNs[] = null;
	/**
	 * The types of the columns: 'N' for numeric, 'T' for temporal, and 'C' for all others
	 */
	protected char colTypes[] = null;
	/**
	 * For numeric and temporal columns, contains distance (difference) thresholds.
	 * For non-numeric columns, contains NaNs.
	 */
	protected double thresholds[] = null;
	/**
	 * Some of the selected attributes may be periodic, e.g. direction from 0 to 359 (360==0).
	 * For such attributes, this array contains the lengths of the periods.
	 * For the remaining attributes, the lengths are assumed to be 0.
	 */
	protected long periodLengths[] = null;

	/**
	 * Returns the name of this Distance Computer
	 */
	public String getMethodName() {
		return "Attribute-based distance";
	}

	/**
	 * The table with thematic data associated with the layer to be clustered
	 */
	public DataTable getTable() {
		return table;
	}

	/**
	 * The table with thematic data associated with the layer to be clustered
	 */
	public void setTable(DataTable table) {
		this.table = table;
	}

	/**
	 * The layer in which objects are clustered
	 */
	public DGeoLayer getLayer() {
		return layer;
	}

	/**
	 * The layer in which objects are clustered
	 */
	public void setLayer(DGeoLayer layer) {
		this.layer = layer;
	}

	/**
	 * Sets the attributes to be involved in the clustering.
	 * The input vector consists of identifiers of user-selected attributes
	 */
	public void setAttributes(Vector attrIds) {
		if (table == null || attrIds == null || attrIds.size() < 1)
			return;
		colNs = new int[attrIds.size()];
		for (int i = 0; i < colNs.length; i++) {
			colNs[i] = table.getAttrIndex((String) attrIds.elementAt(i));
		}
		colTypes = new char[colNs.length];
		for (int i = 0; i < colNs.length; i++) {
			Object val = table.getSampleAttrValue(colNs[i]);
			if (val != null && (val instanceof TimeMoment)) {
				colTypes[i] = 'T';
			} else if (table.isAttributeNumeric(colNs[i])) {
				colTypes[i] = 'N';
			} else {
				colTypes[i] = 'C';
			}
		}
	}

	/**
	 * Replies if it has everything what is needed for the work
	 */
	public boolean isReady() {
		return layer != null && table != null && colNs != null && colTypes != null;
	}

	/**
	 * If necessary, asks the user about additional parameters required for
	 * this Distance Computer. Returns true if successful.
	 * For each numeric attribute, asks the distance threshold.
	 */
	public boolean askParameters() {
		if (table == null || colTypes == null)
			return false;
		int nToAsk = 0;
		for (char colType : colTypes)
			if (colType != 'C') {
				++nToAsk;
			}
		if (nToAsk < 1)
			return true;
		//get the minimum and maximum values of all numeric and temporal attributes
		double min[] = new double[colNs.length], max[] = new double[colNs.length];
		TimeMoment tMin[] = new TimeMoment[colNs.length], tMax[] = new TimeMoment[colNs.length];
		for (int i = 0; i < colNs.length; i++) {
			min[i] = max[i] = Double.NaN;
			tMin[i] = tMax[i] = null;
		}
		for (int j = 0; j < layer.getObjectCount(); j++)
			if (layer.isObjectActive(j)) {
				DGeoObject obj = layer.getObject(j);
				ThematicDataItem it = obj.getData();
				if (it == null || !(it instanceof DataRecord)) {
					continue;
				}
				DataRecord rec = (DataRecord) it;
				for (int i = 0; i < colNs.length; i++)
					if (colTypes[i] == 'N') {
						double val = rec.getNumericAttrValue(colNs[i]);
						if (Double.isNaN(val)) {
							continue;
						}
						if (Double.isNaN(min[i]) || min[i] > val) {
							min[i] = val;
						}
						if (Double.isNaN(max[i]) || max[i] < val) {
							max[i] = val;
						}
					} else if (colTypes[i] == 'T') {
						Object val = rec.getAttrValue(colNs[i]);
						if (val == null || !(val instanceof TimeMoment)) {
							continue;
						}
						TimeMoment t = (TimeMoment) val;
						if (tMin[i] == null || tMin[i].compareTo(t) > 0) {
							tMin[i] = t;
						}
						if (tMax[i] == null || tMax[i].compareTo(t) < 0) {
							tMax[i] = t;
						}
					}
			}
		//ask the user to specify thresholds for numeric and temporal attributes
		GridBagLayout gb = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 1.0f;
		c.weighty = 1.0f;
		c.fill = GridBagConstraints.HORIZONTAL;
		Panel p = new Panel(gb);
		Label l = new Label((nToAsk > 1) ? "Set thresholds for the attributes:" : "Set a threshold for the attribute:", Label.CENTER);
		c.gridwidth = GridBagConstraints.REMAINDER;
		gb.setConstraints(l, c);
		p.add(l);
		TextField tf[] = new TextField[colNs.length];
		for (int i = 0; i < colNs.length; i++) {
			tf[i] = null;
		}
		for (int i = 0; i < colTypes.length; i++)
			if (colTypes[i] != 'C') {
				l = new Label(table.getAttributeName(colNs[i]));
				c.gridwidth = GridBagConstraints.REMAINDER;
				gb.setConstraints(l, c);
				p.add(l);
				if (colTypes[i] == 'N') {
					if (Double.isNaN(min[i])) {
						l = new Label(" - no numeric values found in the table!", Label.RIGHT);
						gb.setConstraints(l, c);
						p.add(l);
					} else {
						c.gridwidth = 2;
						l = new Label("min = " + StringUtil.doubleToStr(min[i], min[i], max[i]));
						gb.setConstraints(l, c);
						p.add(l);
						l = new Label("max = " + StringUtil.doubleToStr(max[i], min[i], max[i]));
						gb.setConstraints(l, c);
						p.add(l);
						l = new Label("range = " + StringUtil.doubleToStr(max[i] - min[i], min[i], max[i]));
						gb.setConstraints(l, c);
						p.add(l);
						l = new Label("threshold =");
						c.gridwidth = 1;
						gb.setConstraints(l, c);
						p.add(l);
						tf[i] = new TextField(10);
						c.gridwidth = GridBagConstraints.REMAINDER;
						gb.setConstraints(tf[i], c);
						p.add(tf[i]);
					}
				} else if (colTypes[i] == 'T') {
					if (tMin[i] == null) {
						l = new Label(" - no temporal values found in the table!", Label.RIGHT);
						gb.setConstraints(l, c);
						p.add(l);
					} else {
						c.gridwidth = 2;
						l = new Label("min = " + tMin[i].toString());
						gb.setConstraints(l, c);
						p.add(l);
						l = new Label("max = " + tMax[i].toString());
						gb.setConstraints(l, c);
						p.add(l);
						l = new Label("range = " + tMax[i].subtract(tMin[i]));
						gb.setConstraints(l, c);
						p.add(l);
						l = new Label("threshold =");
						c.gridwidth = 1;
						gb.setConstraints(l, c);
						p.add(l);
						tf[i] = new TextField(10);
						c.gridwidth = GridBagConstraints.REMAINDER;
						gb.setConstraints(tf[i], c);
						p.add(tf[i]);
					}
				} else {
					l = new Label(" - a qualitative attribute; no threshold is required", Label.RIGHT);
					gb.setConstraints(l, c);
					p.add(l);
				}
			}
		OKDialog dia = new OKDialog(CManager.getAnyFrame(), "Thresholds?", true);
		dia.addContent(p);
		boolean got = false;
		while (!got) {
			dia.show();
			if (dia.wasCancelled())
				return false;
			thresholds = new double[colNs.length];
			got = true;
			for (int i = 0; i < colNs.length && got; i++) {
				thresholds[i] = Double.NaN;
				if (tf[i] != null) {
					String str = tf[i].getText();
					if (str == null || str.trim().length() < 1) {
						if (!Dialogs.askYesOrNo(CManager.getAnyFrame(), "No threshold value is given for attribute \"" + table.getAttributeName(colNs[i]) + "\"! " + "Treat the values as qualitative (compare for equal or not equal)?",
								"Qualitative values?")) {
							got = false;
						}
					} else {
						try {
							thresholds[i] = Double.parseDouble(str);
							if (thresholds[i] <= 0) {
								Dialogs.showMessage(CManager.getAnyFrame(), "Positive threshold value is required for attribute \"" + table.getAttributeName(colNs[i]) + "\"!", "Threshold?");
								got = false;
							}
						} catch (Exception e) {
							Dialogs.showMessage(CManager.getAnyFrame(), "Not a numeric value for attribute \"" + table.getAttributeName(colNs[i]) + "\"!", "Threshold?");
							got = false;
						}
					}
				}
			}
		}
		if (thresholds != null) {
			periodLengths = new long[colNs.length];
			for (int i = 0; i < colNs.length; i++) {
				periodLengths[i] = 0;
				if (!Double.isNaN(thresholds[i])) {
					Attribute at = table.getAttribute(colNs[i]);
					if (at.isPeriodic()) {
						periodLengths[i] = at.getPeriodLength();
					}
				}
			}
		}
		return true;
	}

	/**
	 * Returns a string describing the current parameter settings of the method.
	 * May return null if no additional parameters are used.
	 */
	public String getParameterDescription() {
		if (colNs == null)
			return null;
		String str = "attributes: ";
		for (int i = 0; i < colNs.length; i++) {
			if (i > 0) {
				str += "; ";
			}
			str += table.getAttributeName(colNs[i]);
			if (thresholds != null && !Double.isNaN(thresholds[i])) {
				str += "(" + thresholds[i] + ")";
			}
		}
		return str;
	}

	/**
	 * Returns its parameters, which are necessary and sufficient
	 * for setting up the DistanceComputer. Each parameter has a name,
	 * which is used as a key in the HashMap.
	 * The values of the parameters must be stored in the hashmap as strings!
	 * This is necessary for saving the parameters in a text file.
	 * If the argument is not null, adds its parameter-value pairs to the
	 * given hashmap.
	 */
	public HashMap getParameters(HashMap params) {
		if (colNs == null)
			return null;
		if (params == null) {
			params = new HashMap(20);
		}
		params.put("N_Attributes", new Integer(colNs.length));
		for (int i = 0; i < colNs.length; i++) {
			params.put("attribute_" + (i + 1), table.getAttributeId(colNs[i]));
			if (thresholds != null && !Double.isNaN(thresholds[i])) {
				params.put("threshold_" + (i + 1), new Double(thresholds[i]));
			}
		}
		return params;
	}

	/**
	 * Does its internal settings according to the given list of parameters
	 */
	public void setup(HashMap params) {
		if (params == null)
			return;
		Integer ina = (Integer) params.get("N_Attributes");
		if (ina == null)
			return;
		int nAttr = ina.intValue();
		if (nAttr < 1)
			return;
		Vector<String> attr = new Vector<String>(nAttr, 1);
		for (int i = 0; i < nAttr; i++) {
			attr.addElement((String) params.get("attribute_" + (i + 1)));
		}
		setAttributes(attr);
		for (int i = 0; i < nAttr; i++) {
			Double val = (Double) params.get("threshold_" + (i + 1));
			if (val == null) {
				continue;
			}
			if (thresholds == null) {
				thresholds = new double[nAttr];
				for (int j = 0; j < thresholds.length; j++) {
					thresholds[j] = Double.NaN;
				}
			}
			thresholds[i] = val.doubleValue();
		}
	}

	/**
	 * For the given objects, whose indexes are specified in the array objToSelectIdxs,
	 * computes distances to the object with the index objIdx in the space of attribue values,
	 * i.e. normalized differences between the attribute values (divided by the thresholds).
	 * Returns an array of the same length as objToSelectIdxs with the normalized distances:
	 * <1 means below the threshold and >1 means above the threshold
	 */
	public double[] getDistancesByAttributes(int objIdx, int objToSelectIdxs[]) {
		if (layer == null || colNs == null || objIdx < 0 || objIdx >= layer.getObjectCount() || objToSelectIdxs == null || objToSelectIdxs.length < 1)
			return null;
		DGeoObject gObj0 = layer.getObject(objIdx);
		if (gObj0 == null)
			return null;
		DataRecord rec = (DataRecord) gObj0.getData();
		if (rec == null)
			return null;
		Object values[] = new Object[colNs.length];
		double numValues[] = new double[colNs.length];
		TimeMoment tValues[] = new TimeMoment[colNs.length];
		for (int i = 0; i < colNs.length; i++) {
			values[i] = rec.getAttrValue(colNs[i]);
			numValues[i] = Double.NaN;
			tValues[i] = null;
			if (values[i] != null && thresholds != null && !Double.isNaN(thresholds[i])) {
				if (colTypes[i] == 'T' && (values[i] instanceof TimeMoment)) {
					tValues[i] = (TimeMoment) values[i];
				} else if (colTypes[i] == 'N') {
					numValues[i] = rec.getNumericAttrValue(colNs[i]);
				}
			}
		}
		double dist[] = new double[objToSelectIdxs.length];
		for (int k = 0; k < objToSelectIdxs.length; k++) {
			int j = objToSelectIdxs[k];
			dist[k] = (j == objIdx) ? 0 : 2;
			if (j != objIdx && layer.isObjectActive(j)) {
				DGeoObject gObj = layer.getObject(j);
				if (gObj == null) {
					continue;
				}
				rec = (DataRecord) gObj.getData();
				if (rec == null) {
					continue;
				}
				boolean ok = true;
				int nNumAttr = 0;
				dist[k] = 0;
				for (int i = 0; i < colNs.length && ok; i++) {
					Object val = rec.getAttrValue(colNs[i]);
					if (val == null) {
						ok = values[i] == null;
					} else {
						ok = values[i] != null;
					}
					if (!ok) {
						break;
					}
					if (val == null) {
						continue;
					}
					if (!Double.isNaN(numValues[i])) {
						double dVal = rec.getNumericAttrValue(colNs[i]);
						ok = !Double.isNaN(dVal);
						if (ok) {
							double diff = Math.abs(dVal - numValues[i]);
							if (periodLengths != null && periodLengths[i] > 0 && diff > periodLengths[i] / 2) {
								diff = periodLengths[i] - diff;
							}
							ok = diff <= thresholds[i];
							if (ok) {
								diff /= thresholds[i];
								dist[k] += diff;
								++nNumAttr;
							}
						}
					} else if (tValues[i] != null) {
						ok = val instanceof TimeMoment;
						if (ok) {
							TimeMoment t = (TimeMoment) val;
							double diff = Math.abs(t.subtract(tValues[i]));
							if (periodLengths != null && periodLengths[i] > 0 && diff > periodLengths[i] / 2) {
								diff = periodLengths[i] - diff;
							}
							ok = diff <= thresholds[i];
							if (ok) {
								diff /= thresholds[i];
								dist[k] += diff;
								++nNumAttr;
							}
						}
					} else {
						ok = val.equals(values[i]);
					}
				}
				if (ok) {
					if (nNumAttr > 1) {
						dist[k] /= nNumAttr;
					}
				} else {
					dist[k] = 2;
				}
			}
		}
		return dist;
	}
}
