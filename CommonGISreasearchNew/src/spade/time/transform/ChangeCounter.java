package spade.time.transform;

import java.awt.Component;
import java.util.Hashtable;
import java.util.ResourceBundle;

import spade.lib.lang.Language;
import spade.lib.util.DoubleArray;
import spade.lib.util.IntArray;
import spade.lib.util.NumValManager;
import spade.time.TimeMoment;
import spade.vis.database.ThematicDataItem;

/**
* Implements various comparisons of time-dependent attributes, e.g. with the
* previous time moment, with the fixed time moment, etc.
*/
public class ChangeCounter extends TimeAttrTransformer {
	static ResourceBundle res = Language.getTextResource("spade.time.transform.Res");
	/**
	* Possible modes of visual comparison
	*/
	public static final int COMP_NONE = 0, COMP_PREV_MOMENT = 1, COMP_FIXED_MOMENT = 2, COMP_MEAN = 3, COMP_MEDIAN = 4, COMP_FIRST = 0, COMP_LAST = 4;
	/**
	* The same modes as strings (to be used for saving and restoring states)
	*/
	public static final String compModeNames[] = { "none", "previous_moment", "fixed_moment", "mean", "median" };
	/**
	* Current mode of visual comparison: one of the constants
	* COMP_NONE (=0, default), COMP_PREV_MOMENT (=1), COMP_FIXED_MOMENT (=2),
	* COMP_OBJECT (=3), COMP_VALUE (=4), COMP_AVG (=5), COMP_MEDIAN (=6).
	*/
	protected int visCompMode = COMP_NONE;
	/**
	* The index of the value of the temporal parameter indicating the time moment
	* to compare with in the comparison mode COMP_FIXED_MOMENT
	*/
	protected int cmpTimeIdx = -1;
	/**
	* The string representation of the time moment to compare with in the
	* comparison mode COMP_FIXED_MOMENT. Used additionally to the index of the
	* value of the temporal parameter (@see cmpTimeIdx) because the moment may be
	* specified before a reference to the table is set (e.g. when restoring
	* previously saved transformers).
	*/
	protected String cmpTime = null;
	/**
	* Indicates whether ratios or differences (default) are computed in the
	* visual comparison mode
	*/
	protected boolean computeRatios = false;
	/**
	 * In comparison to mean or to median, indicates if the differences are normalized
	 * by the standard deviation or inter-quartile distance, respectively
	 */
	protected boolean normalize = false;

	/**
	* Returns the identifier (unique name) of this transformation method.
	*/
	@Override
	public String getMethodName() {
		return "time_compare";
	}

	/**
	* Returns the parameters (properties) of this transformation method so that
	* the state of the transformer may be completely restored.
	*/
	@Override
	public Hashtable getProperties() {
		if (visCompMode == COMP_NONE)
			return null;
		Hashtable prop = super.getProperties();
		if (prop == null) {
			prop = new Hashtable();
		}
		prop.put("mode", compModeNames[visCompMode]);
		if (visCompMode == COMP_FIXED_MOMENT) {
			TimeMoment t = getVisCompMoment();
			if (t != null) {
				prop.put("ref_moment", t.toString());
			}
		}
		prop.put("ratios", String.valueOf(computeRatios));
		if (visCompMode == COMP_MEAN || visCompMode == COMP_MEDIAN) {
			prop.put("normalize", String.valueOf(normalize));
		}
		return prop;
	}

	/**
	* Sets the parameters (properties) of this transformation method.
	*/
	@Override
	public void setProperties(Hashtable prop) {
		if (prop == null)
			return;
		Object obj = prop.get("mode");
		if (obj != null) {
			String modeStr = obj.toString();
			for (int i = 0; i < compModeNames.length; i++)
				if (modeStr.equalsIgnoreCase(compModeNames[i])) {
					visCompMode = i;
					break;
				}
		}
		obj = prop.get("ref_moment");
		if (obj != null) {
			cmpTime = obj.toString();
		}
		obj = prop.get("ratios");
		if (obj != null) {
			computeRatios = Boolean.valueOf(obj.toString()).booleanValue();
		}
		obj = prop.get("normalize");
		if (obj != null) {
			normalize = Boolean.valueOf(obj.toString()).booleanValue();
		}
		super.setProperties(prop);
	}

	/**
	 * Returns the description of the data transformation made. The description may be
	 * shown to the user. If no transformation has been made, returns null.
	 */
	@Override
	public String getDescription() {
		if (visCompMode == COMP_NONE) {
			if (nextTrans != null)
				return nextTrans.getDescription();
			return null;
		}
		String descr = ((computeRatios) ? res.getString("ratio_to") : res.getString("difference_to")) + " ";
		if (visCompMode == COMP_PREV_MOMENT) {
			descr += res.getString("previous");
		} else if (visCompMode == COMP_FIXED_MOMENT) {
			descr += getVisCompMoment();
		} else if (visCompMode == COMP_MEAN) {
			descr += res.getString("mean");
		} else if (visCompMode == COMP_MEDIAN) {
			descr += res.getString("median");
		}
		if ((visCompMode == COMP_MEAN || visCompMode == COMP_MEDIAN) && normalize) {
			descr += "; normalised";
		}
		if (nextTrans != null) {
			String d1 = nextTrans.getDescription();
			if (d1 != null) {
				descr += "; " + d1;
			}
		}
		return descr;
	}

	/**
	* Returns the current mode of visual comparison: one of the constants
	* COMP_NONE (=0, default), COMP_PREV_MOMENT (=1), COMP_FIXED_MOMENT (=2),
	* COMP_OBJECT (=3), COMP_VALUE (=4).
	*/
	public int getVisCompMode() {
		return visCompMode;
	}

	/**
	* Sets the current mode of visual comparison. This must be one of the constants
	* COMP_NONE (=0, default), COMP_PREV_MOMENT (=1), COMP_FIXED_MOMENT (=2).
	* Does not automatically do the transformation! Call doTransformation()
	* explicitly for the changes of the settings could actually have an effect.
	*/
	public void setVisCompMode(int mode) {
		if (mode != visCompMode && mode >= COMP_FIRST && mode <= COMP_LAST) {
			visCompMode = mode;
			if (visCompMode == COMP_FIXED_MOMENT)
				if (cmpTimeIdx < 0) {
					cmpTimeIdx = 0;
				}
		}
	}

	/**
	 * In comparison to mean or to median, indicates if the differences are normalized
	 * by the standard deviation or inter-quartile distance, respectively
	 */
	public boolean getNormalize() {
		return normalize;
	}

	/**
	 * In comparison to mean or to median, sets whether the differences must be normalized
	 * by the standard deviation or inter-quartile distance, respectively
	 */
	public void setNormalize(boolean normalize) {
		this.normalize = normalize;
	}

	/**
	* In the mode of visual comparison with a fixed time moment, returns the
	* moment to compare with.
	*/
	public TimeMoment getVisCompMoment() {
		if (tPar == null)
			return null;
		if (cmpTimeIdx >= 0 && cmpTimeIdx < tPar.getValueCount())
			return (TimeMoment) tPar.getValue(cmpTimeIdx);
		if (cmpTime != null && setVisCompMoment(cmpTime))
			return (TimeMoment) tPar.getValue(cmpTimeIdx);
		return null;
	}

	/**
	* In the mode of visual comparison with a fixed time moment, returns the
	* index of this moment among the values of the temporal parameter.
	*/
	public int getVisCompMomentIdx() {
		return cmpTimeIdx;
	}

	/**
	* In the mode of visual comparison with a fixed time moment, sets the
	* moment to compare with. Returns true if correctly done, i.e. the moment
	* has been found among the parameter values, and it was different from the
	* previously set moment.
	* Does not automatically do the transformation! Call doTransformation()
	* explicitly for the changes of the settings could actually have an effect.
	*/
	public boolean setVisCompMoment(TimeMoment moment) {
		if (moment != null) {
			for (int i = 0; i < tPar.getValueCount(); i++)
				if (tPar.getValue(i).equals(moment))
					return setVisCompMomentIdx(i);
		}
		return false;
	}

	/**
	* In the mode of visual comparison with a fixed time moment, sets the
	* moment to compare with by specifying its index among the values of the
	* temporal parameter. Returns true if correctly done, i.e. the moment
	* has been found among the parameter values, and it was different from the
	* previously set moment.
	* Does not automatically do the transformation! Call doTransformation()
	* explicitly for the changes of the settings could actually have an effect.
	*/
	public boolean setVisCompMomentIdx(int idx) {
		if (idx >= 0 && idx != cmpTimeIdx) {
			cmpTimeIdx = idx;
			return true;
		}
		return false;
	}

	/**
	* In the mode of visual comparison with a fixed time moment, sets the
	* moment to compare with, by transforming the given string to a TimeMoment.
	* Returns true if correctly done, i.e. the string was successfully transformed
	* into a time moment, the moment has been found among the parameter values,
	* and it was different from the previously set moment.
	* Does not automatically do the transformation! Call doTransformation()
	* explicitly for the changes of the settings could actually have an effect.
	*/
	public boolean setVisCompMoment(String timeStr) {
		if (timeStr == null || timeStr.length() < 1)
			return false;
		TimeMoment tm = ((TimeMoment) tPar.getValue(0)).getCopy();
		if (!tm.setMoment(timeStr))
			return false;
		return setVisCompMoment(tm);
	}

	/**
	* Informs whether ratios or differences are computed in the visual comparison
	* mode
	*/
	public boolean getComputeRatios() {
		return computeRatios;
	}

	/**
	* Sets whether ratios or differences must be computed in the visual comparison
	* mode.
	* Does not automatically do the transformation! Call doTransformation()
	* explicitly for the changes of the settings could actually have an effect.
	*/
	public void setComputeRatios(boolean value) {
		computeRatios = value;
	}

	/**
	* Performs the necessary data transformation and puts the results in the
	* internal array.
	*/
	@Override
	public void doTransformation() {
		if (table == null || !table.hasData() || transColList == null || transColList.size() < 1)
			return;
		if (data == null || data.length != table.getDataItemCount() || data[0].length != transColList.size()) {
			data = new double[table.getDataItemCount()][transColList.size()];
		}
		if (visCompMode == COMP_FIXED_MOMENT && cmpTimeIdx < 0) {
			if (cmpTime != null) {
				setVisCompMoment(cmpTime);
			}
			if (cmpTimeIdx < 0) {
				visCompMode = COMP_NONE;
			}
		}
		if (visCompMode == COMP_MEAN || visCompMode == COMP_MEDIAN) {
			for (int na = 0; na < timeAttrColNs.size(); na++) {
				IntArray colNs = (IntArray) timeAttrColNs.elementAt(na);
				DoubleArray vals = new DoubleArray(colNs.size(), 1);
				for (int nr = 0; nr < table.getDataItemCount(); nr++) {
					for (int i = 0; i < transColList.size(); i++) {
						int nc = transColList.elementAt(i);
						if (idxTimeAttrColNs[nc] == na) {
							data[nr][i] = Double.NaN;
						}
					}
					vals.removeAllElements();
					ThematicDataItem rec = (ThematicDataItem) table.getDataItem(nr);
					for (int i = 0; i < colNs.size(); i++) {
						vals.addElement(getOrigAttrValue(colNs.elementAt(i), rec));
					}
					double refV = Double.NaN;
					double q123[] = null;
					if (visCompMode == COMP_MEAN) {
						refV = NumValManager.getMean(vals);
					} else {
						int perc[] = { 25, 50, 75 };
						q123 = NumValManager.getPercentiles(vals, perc);
						if (q123 != null && q123.length >= 3) {
							refV = q123[1];
						} else {
							q123 = null;
						}
					}
					if (Double.isNaN(refV)) {
						continue;
					}
					double divBy = 1;
					if (normalize && !computeRatios)
						if (visCompMode == COMP_MEAN) {
							divBy = NumValManager.getStdD(vals, refV);
						} else if (q123 != null) {
							divBy = q123[2] - q123[0];
						}
					if (divBy == 0) {
						divBy = 1;
					}
					for (int i = 0; i < transColList.size(); i++) {
						int nc = transColList.elementAt(i);
						if (idxTimeAttrColNs[nc] == na) {
							double val = getOrigAttrValue(nc, rec);
							if (computeRatios)
								if (refV == 0) {
									val = Double.NaN;
								} else {
									val /= refV;
								}
							else {
								val -= refV;
							}
							data[nr][i] = val / divBy;
						}
					}
				}
			}
		} else {
			for (int i = 0; i < transColList.size(); i++) {
				int nc = transColList.elementAt(i);
				IntArray colNs = null;
				int idxColNs = -1;
				if (idxTimeAttrColNs[nc] >= 0) {
					colNs = (IntArray) timeAttrColNs.elementAt(idxTimeAttrColNs[nc]);
					idxColNs = colNs.indexOf(nc);
				}
				for (int nr = 0; nr < table.getDataItemCount(); nr++) {
					data[nr][i] = Double.NaN;
					ThematicDataItem rec = (ThematicDataItem) table.getDataItem(nr);
					double val = getOrigAttrValue(nc, rec);
					if (Double.isNaN(val)) {
						continue;
					}
					if (visCompMode == COMP_NONE) {
						data[nr][i] = val;
						continue;
					}
					double v = Double.NaN; //reference value for comparison
					switch (visCompMode) {
					case COMP_PREV_MOMENT:
						if (idxColNs > 0 && colNs.elementAt(idxColNs - 1) >= 0) {
							v = getOrigAttrValue(colNs.elementAt(idxColNs - 1), rec);
						}
						break;
					case COMP_FIXED_MOMENT:
						if (colNs.elementAt(cmpTimeIdx) >= 0) {
							v = getOrigAttrValue(colNs.elementAt(cmpTimeIdx), rec);
						}
						break;
					}
					if (Double.isNaN(v)) {
						val = Double.NaN;
					} else if (computeRatios)
						if (v == 0) {
							val = Double.NaN;
						} else {
							val /= v;
						}
					else {
						val -= v;
					}
					data[nr][i] = val;
				}
			}
		}
		if (nextTrans != null) {
			nextTrans.doTransformation();
		} else {
			notifyValuesChange();
		}
	}

	/**
	* Returns the string representation of the transformed value corresponding to
	* the given row and column of the original table. If the value is not
	* transformed, returns the value returned by the next transformer or null.
	*/
	@Override
	public String getTransformedValueAsString(int rowN, int colN) {
		if (rowN < 0 || colN < 0)
			return null;
		String val = null;
		if (nextTrans != null) {
			val = nextTrans.getTransformedValueAsString(rowN, colN);
		}
		if (visCompMode == COMP_NONE)
			return val;
		if (data == null || rowN >= data.length || transColNs == null || colN >= transColNs.length || transColNs[colN] < 0)
			return val;
		if (Double.isNaN(data[rowN][transColNs[colN]]))
			return null;
		String str = String.valueOf(data[rowN][transColNs[colN]]);
		if (val == null)
			return str;
		return str + " >> " + val;
	}

	/**
	* Returns the UI for setting the transformation parameters in this transformer
	*/
	@Override
	public Component getIndividualUI() {
		return new ChangeCountUI(this);
	}
}
