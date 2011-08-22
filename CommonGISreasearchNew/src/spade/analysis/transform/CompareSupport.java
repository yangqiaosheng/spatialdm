package spade.analysis.transform;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.lib.lang.Language;
import spade.lib.util.NumStat;

/**
* Implements various comparisons of numeric attributes, e.g. with an object,
* a fixed value, mean, median, etc.
*/
public class CompareSupport extends BaseAttributeTransformer {
	static ResourceBundle res = Language.getTextResource("spade.analysis.transform.Res");
	/**
	* Possible modes of comparison
	*/
	public static final int COMP_NONE = 0, COMP_OBJECT = 1, COMP_VALUE = 2, COMP_AVG = 3, COMP_MEDIAN = 4, COMP_FIRST = 0, COMP_LAST = 4;
	/**
	* The same modes as strings (to be used for saving and restoring states)
	*/
	public static final String compModeNames[] = { "none", "object", "value", "mean", "median" };
	/**
	* Current transformation (comparison) mode
	*/
	protected int currMode = COMP_NONE;
	/**
	* The index of the table row indicating the object to compare with in the
	* comparison mode COMP_OBJECT
	*/
	protected int cmpRowN = -1;
	/**
	* The identifier of the object to compare with in the comparison mode COMP_OBJECT.
	* Used additionally to the number of the table row because the object may be
	* specified before a reference to the table is set (e.g. when restoring
	* previously saved transformers).
	*/
	protected String cmpObjId = null;
	/**
	* The value to compare with in the comparison mode COMP_VALUE
	*/
	protected double cmpVal = Double.NaN;
	/**
	* Indicates whether ratios or differences (default) are computed in the
	* visual comparison mode
	*/
	protected boolean computeRatios = false;
	/**
	* Value statistics for the columns to be transformed. Used for comparison
	* with mean and median
	*/
	protected NumStat stat[] = null;

	/**
	* Returns the identifier (unique name) of this transformation method.
	*/
	@Override
	public String getMethodName() {
		return "compare";
	}

	/**
	* Returns the parameters (properties) of this transformation method so that
	* the state of the transformer may be completely restored.
	*/
	@Override
	public Hashtable getProperties() {
		if (currMode == COMP_NONE)
			return null;
		Hashtable prop = super.getProperties();
		if (prop == null) {
			prop = new Hashtable();
		}
		prop.put("mode", compModeNames[currMode]);
		if (currMode == COMP_OBJECT && (cmpObjId != null || (cmpRowN >= 0 && table != null))) {
			if (cmpObjId == null) {
				cmpObjId = table.getDataItemId(cmpRowN);
			}
			prop.put("ref_object", cmpObjId);
		} else if (currMode == COMP_VALUE && !Double.isNaN(cmpVal)) {
			prop.put("ref_value", String.valueOf(cmpVal));
		}
		prop.put("ratios", String.valueOf(computeRatios));
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
					currMode = i;
					break;
				}
		}
		obj = prop.get("ref_object");
		if (obj != null) {
			setVisCompObjId(obj.toString());
		}
		obj = prop.get("ref_value");
		if (obj != null) {
			try {
				cmpVal = Double.valueOf(obj.toString()).doubleValue();
			} catch (NumberFormatException e) {
				cmpVal = Double.NaN;
			}
		}
		obj = prop.get("ratios");
		if (obj != null) {
			computeRatios = Boolean.valueOf(obj.toString()).booleanValue();
		}
		super.setProperties(prop);
	}

	/**
	* Informs whether the attributes may be (potentially) transformed
	* individually. Returns false
	*/
	@Override
	public boolean getAllowIndividualTransformation() {
		return false;
	}

	/**
	* The transformer is given a list of attribute identifiers to be transformed.
	* Possibly, not all attributes can be transformed by a specific Transformer.
	*/
	@Override
	public void setAttributes(Vector attrIds) {
		if (attrIds == null || attrIds.size() < 1)
			return;
		setColumnNumbers(table.getRelevantColumnNumbers(attrIds));
	}

	/**
	* Informs whether the transformer has all necessary settings for the
	* transformation and whether the data given to it can be transformed.
	*/
	@Override
	public boolean isValid() {
		return table != null && transColList != null && transColList.size() > 0;
	}

	/**
	* Sets the current mode of data transformation common for all attributes.
	* Does not automatically do the transformation! Call doTransformation()
	* explicitly for the changes of the settings could actually have an effect.
	*/
	public void setTransMode(int mode) {
		if (mode != currMode && mode >= COMP_FIRST && mode <= COMP_LAST) {
			currMode = mode;
		}
	}

	/**
	* Returns the current mode of data transformation common for all attributes.
	*/
	public int getTransMode() {
		return currMode;
	}

	/**
	* In the mode of visual comparison with a selected object, returns the name
	* of the object.
	*/
	public String getVisCompObjName() {
		if (cmpRowN < 0 || cmpRowN >= table.getDataItemCount())
			return null;
		return table.getDataItemName(cmpRowN);
	}

	/**
	* In the mode of visual comparison with a selected object, sets the object
	* to compare with (by specifying its name). Returns true if correctly done,
	* i.e. the object name has been found in the table, and it was different from
	* the previously set object.
	* Does not automatically do the transformation! Call doTransformation()
	* explicitly for the changes of the settings could actually have an effect.
	*/
	public boolean setVisCompObjName(String name) {
		if (name == null)
			return false;
		int rowN = -1;
		for (int i = 0; i < table.getDataItemCount() && rowN < 0; i++)
			if (name.equalsIgnoreCase(table.getDataItemName(i))) {
				rowN = i;
			}
		if (rowN >= 0 && rowN != cmpRowN) {
			cmpRowN = rowN;
			cmpObjId = table.getDataItemId(rowN);
			return true;
		}
		return false;
	}

	/**
	* In the mode of visual comparison with a selected object, sets the object
	* to compare with (by specifying its identifier). Returns true if correctly done,
	* i.e. the object name has been found in the table, and it was different from
	* the previously set object.
	* Does not automatically do the transformation! Call doTransformation()
	* explicitly for the changes of the settings could actually have an effect.
	*/
	public boolean setVisCompObjId(String id) {
		cmpObjId = id;
		cmpRowN = -1;
		if (cmpObjId == null || table == null)
			return false;
		int rowN = table.indexOf(cmpObjId);
		if (rowN >= 0 && rowN != cmpRowN) {
			cmpRowN = rowN;
			return true;
		}
		return false;
	}

	/**
	* In the mode of visual comparison with a selected value returns the current
	* value to compare with.
	*/
	public double getVisCompValue() {
		return cmpVal;
	}

	/**
	* In the mode of visual comparison with a selected value sets the value to
	* compare with. Returns true if correctly done, i.e. the value lies between
	* the absolute minimum and maximum values, and it was different from
	* the previously set value.
	* Does not automatically do the transformation! Call doTransformation()
	* explicitly for the changes of the settings could actually have an effect.
	*/
	public boolean setVisCompValue(double value) {
		if (value != cmpVal) {
			cmpVal = value;
			return true;
		}
		return false;
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
	* Reacts to changes of the data in the table
	*/
	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		stat = null;
		super.propertyChange(pce);
	}

	/**
	* Performs the necessary data transformation and puts the results in the
	* array data.
	*/
	@Override
	public void doTransformation() {
		if (table == null || !table.hasData() || transColList == null || transColList.size() < 1)
			return;
		if (data == null || data.length != table.getDataItemCount() || data[0].length != transColList.size()) {
			data = new double[table.getDataItemCount()][transColList.size()];
		}
		if (stat == null && (currMode == COMP_AVG || currMode == COMP_MEDIAN)) {
			stat = new NumStat[transColList.size()];
			for (int i = 0; i < transColList.size(); i++) {
				stat[i] = (prevTrans != null) ? prevTrans.getNumAttrStatistics(transColList.elementAt(i), false) : table.getNumAttrStatistics(transColList.elementAt(i));
			}
		} else if (currMode == COMP_OBJECT && cmpRowN < 0) {
			if (cmpObjId != null) {
				cmpRowN = table.indexOf(cmpObjId);
			}
			if (cmpRowN < 0) {
				currMode = COMP_NONE;
			}
		} else if (currMode == COMP_VALUE && Double.isNaN(cmpVal)) {
			currMode = COMP_NONE;
		}
		for (int i = 0; i < transColList.size(); i++) {
			int nc = transColList.elementAt(i);
			for (int nr = 0; nr < table.getDataItemCount(); nr++) {
				data[nr][i] = Double.NaN;
				double val = getOrigAttrValue(nc, nr);
				if (Double.isNaN(val)) {
					continue;
				}
				if (currMode == COMP_NONE) {
					data[nr][i] = val;
					continue;
				}
				double v = Double.NaN; //reference value for comparison
				switch (currMode) {
				case COMP_OBJECT:
					v = getOrigAttrValue(nc, cmpRowN);
					break;
				case COMP_VALUE:
					v = cmpVal;
					break;
				case COMP_AVG:
				case COMP_MEDIAN:
					if (stat[i] != null) {
						v = (currMode == COMP_AVG) ? stat[i].mean : stat[i].median;
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
		if (currMode == COMP_NONE)
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
	 * Returns the description of the data transformation made. The description may be
	 * shown to the user. If no transformation has been made, returns null.
	 */
	@Override
	public String getDescription() {
		if (currMode == COMP_NONE) {
			if (nextTrans != null)
				return nextTrans.getDescription();
			return null;
		}
		String descr = ((computeRatios) ? res.getString("ratio_to") : res.getString("difference_to")) + " ";
		if (currMode == COMP_OBJECT) {
			descr += getVisCompObjName();
		} else if (currMode == COMP_VALUE) {
			descr += getVisCompValue();
		} else if (currMode == COMP_AVG) {
			descr += res.getString("mean");
		} else if (currMode == COMP_MEDIAN) {
			descr += res.getString("median");
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
	* Returns the UI for setting the transformation parameters in this transformer
	*/
	@Override
	public Component getIndividualUI() {
		return new CompareSupportUI(this);
	}
}
