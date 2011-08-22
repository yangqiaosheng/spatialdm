package spade.vis.mapvis;

import java.util.Hashtable;
import java.util.Vector;

import spade.lib.util.NumRange;
import spade.vis.database.AttributeTypes;

/**
* Common parent class for the classes presenting several numeric attributes
*/
public abstract class MultiNumberDrawer extends DataPresenter {
	/**
	* Contains instances of NumRange, which store Min and Max values for all
	* attributes in the data set
	*/
	protected Vector dataMinMax = null;
	protected double dataMIN = Double.NaN, dataMAX = Double.NaN, // Min among Mins and Max among Maxes
			focuserMIN = Double.NaN, focuserMAX = Double.NaN, // focuser restrictions
			cmpTo = 0.0;
	protected double epsilon = Double.NaN; //comparison tolerance
	/**
	* Minimum number of attributes required for this visualizer (by default 2)
	*/
	protected int minAttrNumber = 2;
	/**
	* Maximum number of attributes required for this visualizer (by default Integer.MAX_VALUE)
	*/
	protected int maxAttrNumber = Integer.MAX_VALUE;

	public void setFocuserMinMax(double focuserMin, double focuserMax) {
		this.focuserMIN = focuserMin;
		this.focuserMAX = focuserMax;
		notifyVisChange();
	}

	public double getCmp() {
		return cmpTo;
	}

	public void setCmp(double cmpTo) {
		this.cmpTo = cmpTo;
		notifyVisChange();
	}

	public void computeComparisonTolerance() {
		if (!Double.isNaN(dataMIN) && !Double.isNaN(dataMAX)) {
			epsilon = 0.0001 * (dataMAX - dataMIN);
		} else if (!Double.isNaN(focuserMIN) && !Double.isNaN(focuserMAX)) {
			epsilon = 0.0001 * (focuserMAX - focuserMIN);
		} else {
			epsilon = 0.0001;
		}
	}

	/**
	* Checks if this visualization method is applicable to the given set of
	* attributes. May use the DataInformer to check types and values
	* of the attributes.
	*/
	@Override
	public boolean isApplicable(Vector attr) {
		err = null;
		if (attr == null || attr.size() < 1) {
			err = errors[0];
			return false;
		}
		int nattr = 0;
		for (int i = 0; i < attr.size(); i++)
			if (attr.elementAt(i) != null) {
				++nattr;
			}
		if (nattr > maxAttrNumber) {
			err = errors[8];
			return false;
		}
		if (nattr < minAttrNumber) {
			err = errors[7];
			return false;
		}
		if (table == null) {
			err = errors[1];
			return false;
		}
		if (dataMinMax == null) {
			dataMinMax = new Vector(attr.size(), 10);
		} else {
			dataMinMax.removeAllElements();
		}
		dataMIN = Double.NaN;
		dataMAX = Double.NaN;
		for (int i = 0; i < attr.size(); i++) {
			dataMinMax.addElement(null);
			String id = (String) attr.elementAt(i);
			if (id == null) {
				continue;
			}
			char type = table.getAttributeType(id);
			if (!AttributeTypes.isNumericType(type) && !AttributeTypes.isTemporal(type)) {
				err = id + ": " + errors[2];
				return false;
			}
			NumRange nr = (subAttr == null || subAttr.size() <= i || subAttr.elementAt(i) == null) ? getAttrValueRange(id) : getAttrValueRange((Vector) subAttr.elementAt(i));
			/*
			if (nr==null || Double.isNaN(nr.maxValue)) {
			  err=id+": "+errors[3];
			  return false;
			}
			*/
			if (nr == null || Double.isNaN(nr.maxValue)) {
				continue;
			}
			dataMinMax.setElementAt(nr, i);
			/*
			if (nr.maxValue<=nr.minValue) {
			  err=id+": "+errors[4];
			  return false;
			}
			*/
			if (Double.isNaN(dataMIN) || dataMIN > nr.minValue) {
				dataMIN = nr.minValue;
			}
			if (Double.isNaN(dataMAX) || dataMAX < nr.maxValue) {
				dataMAX = nr.maxValue;
			}
		}
		focuserMIN = dataMIN;
		focuserMAX = dataMAX;
		if (cmpTo != 0.0f)
			if (cmpTo < dataMIN || cmpTo > dataMAX) {
				cmpTo = 0.0f;
			}
		computeComparisonTolerance();
		return true;
	}

	/**
	 * Should return true if the visualization changes.
	 * The argument "valuesTransformed" indicates whether the values have been
	 * transformed (e.g. by an attribute transformer), which means that the
	 * value range might completely change. In this case, the visualiser may
	 * need to reset its parameters. Otherwise, a slight adaptation may be
	 * sufficient, if needed at all.
	 */
	@Override
	public boolean adjustToDataChange(boolean valuesTransformed) {
		if (attr == null || attr.size() < 1)
			return false;
		double min = Double.NaN, max = Double.NaN;
		for (int i = 0; i < attr.size(); i++) {
			String id = (String) attr.elementAt(i);
			if (id == null) {
				continue;
			}
			NumRange nr = (subAttr == null || subAttr.size() <= i || subAttr.elementAt(i) == null) ? getAttrValueRange(id) : getAttrValueRange((Vector) subAttr.elementAt(i));
			if (nr == null || Double.isNaN(nr.maxValue)) {
				continue;
			}
			dataMinMax.setElementAt(nr, i);
			if (Double.isNaN(min) || min > nr.minValue) {
				min = nr.minValue;
			}
			if (Double.isNaN(max) || max < nr.maxValue) {
				max = nr.maxValue;
			}
		}
		if (min == dataMIN && max == dataMAX)
			return false;
		if (!valuesTransformed && min >= dataMIN && max <= dataMAX)
			return false;
		if (valuesTransformed || dataMIN > min) {
			dataMIN = focuserMIN = min;
		}
		if (valuesTransformed || dataMAX < max) {
			dataMAX = focuserMAX = max;
		}
		if (cmpTo != 0.0f)
			if (cmpTo < dataMIN || cmpTo > dataMAX) {
				cmpTo = 0.0f;
			}
		return true;
	}

	public double getDataMin(int attrN) {
		if (attrN < 0 || dataMinMax == null || attrN >= dataMinMax.size())
			return Double.NaN;
		NumRange nr = (NumRange) dataMinMax.elementAt(attrN);
		if (nr == null)
			return Double.NaN;
		return nr.minValue;
	}

	public double getDataMax(int attrN) {
		if (attrN < 0 || dataMinMax == null || attrN >= dataMinMax.size())
			return Double.NaN;
		NumRange nr = (NumRange) dataMinMax.elementAt(attrN);
		if (nr == null)
			return Double.NaN;
		return nr.maxValue;
	}

	public double getDataMin() {
		return dataMIN;
	}

	public double getDataMax() {
		return dataMAX;
	}

	public void setDataMinMax(int attrN, double minValue, double maxValue) {
		if (attr == null || attrN < 0 || attrN > attr.size())
			return;
		if (dataMinMax == null) {
			dataMinMax = new Vector(attr.size(), 10);
		}
		for (int i = dataMinMax.size(); i < attr.size(); i++) {
			dataMinMax.addElement(null);
		}
		NumRange nr = new NumRange();
		nr.minValue = minValue;
		nr.maxValue = maxValue;
		dataMinMax.setElementAt(nr, attrN);
		if (Double.isNaN(dataMIN) || minValue < dataMIN) {
			dataMIN = minValue;
		}
		if (Double.isNaN(dataMAX) || maxValue > dataMAX) {
			dataMAX = maxValue;
		}
	}

	/**
	* The Visualizer sets its parameters. For this purpose it may need
	* data statistics. Statistics are received from a StatisticsProvider.
	* A reference to a StatisticsProvider may be received from the DataInformer.
	*/
	@Override
	public void setup() {
		if (!isApplicable(attr))
			return;
	}

	/**
	* Adds one more attribute to be visualized to the current set
	* of the visualized attributes.
	*/
	public void addAttribute(String attrId, String attrName) {
		if (attr == null) {
			attr = new Vector(10, 10);
		}
		attr.addElement(attrId);
		if (attrName != null || attrNames != null) {
			if (attrNames == null) {
				attrNames = new Vector(10, 10);
			}
			while (attrNames.size() < attr.size() - 1) {
				attrNames.addElement(null);
			}
			attrNames.addElement(attrName);
		}
		colNs = null;
		if (dataMinMax != null)
			if (table != null) {
				NumRange nr = getAttrValueRange(attrId);
				if (nr != null) {
					if (dataMIN > nr.minValue) {
						dataMIN = nr.minValue;
					}
					if (dataMAX < nr.maxValue) {
						dataMAX = nr.maxValue;
					}
				}
				dataMinMax.addElement(nr);
			} else {
				dataMinMax.addElement(null);
			}
	}

	/**
	* Removes one of the attributes being currently visualized.
	*/
	public void removeAttribute(String attrId) {
		removeAttribute(this.getAttrIndex(attrId));
	}

	/**
	* Removes one of the attributes being currently visualized.
	*/
	public void removeAttribute(int attrIdx) {
		if (attr == null || attrIdx < 0 || attrIdx >= attr.size())
			return;
		attr.removeElementAt(attrIdx);
		if (attrNames != null && attrIdx < attrNames.size()) {
			attrNames.removeElementAt(attrIdx);
		}
		if (invariants != null && attrIdx < invariants.size()) {
			invariants.removeElementAt(attrIdx);
		}
		if (subAttr != null && attrIdx < subAttr.size()) {
			subAttr.removeElementAt(attrIdx);
		}
		colNs = null;
		if (dataMinMax != null) {
			dataMinMax.removeElementAt(attrIdx);
		}
	}

//ID
	public double getFocuserMin() {
		return focuserMIN;
	}

	public double getFocuserMax() {
		return focuserMAX;
	}

	@Override
	public Hashtable getVisProperties() {
		Hashtable param;
		param = super.getVisProperties();
		if (param == null) {
			param = new Hashtable();
		}

		param.put("focuserMin", String.valueOf(focuserMIN));
		param.put("focuserMax", String.valueOf(focuserMAX));

		return param;
	}

	@Override
	public void setVisProperties(Hashtable param) {
		double temp = Double.NaN;
		String str = (String) param.get("focuserMin");
		if (str != null) {
			try {
				temp = new Double(str).doubleValue();
			} catch (NumberFormatException e) {
			}
		}
		if (!Double.isNaN(temp)) {
			focuserMIN = temp;
		}
		str = (String) param.get("focuserMax");
		temp = Double.NaN;
		if (str != null) {
			try {
				temp = new Double(str).doubleValue();
			} catch (NumberFormatException e) {
			}
		}
		if (!Double.isNaN(temp)) {
			focuserMAX = temp;
		}

		super.setVisProperties(param);
	}
//~ID
}
