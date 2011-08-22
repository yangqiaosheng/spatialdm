package spade.vis.mapvis;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Hashtable;
import java.util.Vector;

import spade.lib.basicwin.CManager;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.StringInRectangle;
import spade.lib.color.CS;
import spade.lib.color.ColorSelDialog;
import spade.lib.util.NumRange;
import spade.lib.util.StringUtil;
import spade.time.TimeMoment;
import spade.vis.database.AttributeTypes;
import spade.vis.geometry.Triangle;

/**
* Common parent class for the classes presenting a single numeric
* attribute by painting or symbol size.
*/
public abstract class NumberDrawer extends DataPresenter {
	protected double focuserMin = Double.NaN, focuserMax = Double.NaN, cmpTo = 0.0f, dataMin = Double.NaN, dataMax = Double.NaN, epsilon = Double.NaN; //comparison tolerance
	public float posHue = 0.125f, negHue = 0.6f;
	protected Triangle tr = null;
	/**
	 * If the attribute is temporal, these fields contain the absolute minimum and
	 * maximum time moments.
	 */
	protected TimeMoment minTime = null, maxTime = null;

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
		if (attr.size() > 1) {
			err = errors[6];
			return false;
		}
		if (table == null) {
			err = errors[1];
			return false;
		}
		String id = (String) attr.elementAt(0);
		char type = table.getAttributeType(id);
		if (!AttributeTypes.isNumericType(type) && !AttributeTypes.isTemporal(type)) {
			err = errors[2];
			return false;
		}
		NumRange nr = (subAttr == null || subAttr.size() < 1 || subAttr.elementAt(0) == null) ? getAttrValueRange(id) : getAttrValueRange((Vector) subAttr.elementAt(0));
		if (nr == null || Double.isNaN(nr.maxValue)) {
			err = id + ": " + errors[3];
			return false;
		}
		/*
		if (nr.maxValue<=nr.minValue) {
		  err=id+": "+errors[4];
		  return false;
		}
		*/
		dataMin = nr.minValue;
		dataMax = nr.maxValue;
		if (cmpTo != 0.0f)
			if (cmpTo < dataMin || cmpTo > dataMax) {
				cmpTo = 0.0f;
			}
		if (AttributeTypes.isTemporal(type) && table != null) {
			int attrN = table.getAttrIndex(id);
			for (int i = 0; i < table.getDataItemCount(); i++) {
				Object val = table.getAttrValue(attrN, i);
				if (val == null || !(val instanceof TimeMoment)) {
					continue;
				}
				TimeMoment t = (TimeMoment) val;
				if (minTime == null || minTime.compareTo(t) > 0) {
					minTime = t;
				}
				if (maxTime == null || maxTime.compareTo(t) < 0) {
					maxTime = t;
				}
			}
			if (minTime != null) {
				minTime = minTime.getCopy();
			}
			if (maxTime != null) {
				maxTime = maxTime.getCopy();
			}
		}
		return true;
	}

	/**
	* Must check semantic applicability of this visualization method to the
	* attributes previously set in the visualizer. Here there is no need in this
	* because only one attribute is visualized. Returns true.
	*/
	@Override
	public boolean checkSemantics() {
		return true;
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
		setFocuserMinMax(dataMin, dataMax);
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
		String id = (String) attr.elementAt(0);
		NumRange nr = (subAttr == null || subAttr.size() < 1 || subAttr.elementAt(0) == null) ? getAttrValueRange(id) : getAttrValueRange((Vector) subAttr.elementAt(0));
		if (nr == null || Double.isNaN(nr.maxValue))
			return false;
		if (nr.minValue == dataMin && nr.maxValue == dataMax)
			return false;
		if (!valuesTransformed && nr.minValue >= dataMin && nr.maxValue <= dataMax)
			return false;
		if (valuesTransformed || dataMin > nr.minValue) {
			dataMin = nr.minValue;
		}
		if (valuesTransformed || dataMax < nr.maxValue) {
			dataMax = nr.maxValue;
		}
		if (cmpTo != 0.0f)
			if (cmpTo < dataMin || cmpTo > dataMax) {
				cmpTo = 0.0f;
			}
		if (AttributeTypes.isTemporal(table.getAttributeType(id)) && table != null) {
			int attrN = table.getAttrIndex(id);
			for (int i = 0; i < table.getDataItemCount(); i++) {
				Object val = table.getAttrValue(attrN, i);
				if (val == null || !(val instanceof TimeMoment)) {
					continue;
				}
				TimeMoment t = (TimeMoment) val;
				if (minTime == null || minTime.compareTo(t) > 0) {
					minTime = t;
				}
				if (maxTime == null || maxTime.compareTo(t) < 0) {
					maxTime = t;
				}
			}
			if (minTime != null) {
				minTime = minTime.getCopy();
			}
			if (maxTime != null) {
				maxTime = maxTime.getCopy();
			}
		}
		setFocuserMinMax(dataMin, dataMax);
		return true;
	}

	public double getFocuserMin() {
		return focuserMin;
	}

	public double getFocuserMax() {
		return focuserMax;
	}

	public double getCmp() {
		return cmpTo;
	}

	public TimeMoment getCmpAsTime() {
		if (minTime == null)
			return null;
		return minTime.valueOf((long) cmpTo);
	}

	public String getFocuserMinAsString() {
		return getValueAsString(focuserMin);
	}

	public String getFocuserMaxAsString() {
		if (maxTime == null)
			return StringUtil.doubleToStr(focuserMax, focuserMin, focuserMax);
		return maxTime.valueOf((long) focuserMax).toString();
	}

	public String getCmpAsString() {
		return getValueAsString(cmpTo);
	}

	public String getValueAsString(double val) {
		if (minTime == null)
			return StringUtil.doubleToStr(val, focuserMin, focuserMax);
		return minTime.valueOf((long) val).toString();
	}

	public boolean isAttrTemporal() {
		return minTime != null && maxTime != null;
	}

	public TimeMoment getMinTime() {
		return minTime;
	}

	public TimeMoment getMaxTime() {
		return maxTime;
	}

	public void setFocuserMinMax(double focuserMin, double focuserMax) {
		this.focuserMin = focuserMin;
		this.focuserMax = focuserMax;
		computeComparisonTolerance();
		notifyVisChange();
	}

	public void computeComparisonTolerance() {
		if (!Double.isNaN(focuserMin) && !Double.isNaN(focuserMax)) {
			epsilon = 0.0001 * (focuserMax - focuserMin);
		} else if (!Double.isNaN(dataMin) && !Double.isNaN(dataMax)) {
			epsilon = 0.0001 * (dataMax - dataMin);
		} else {
			epsilon = 0;
		}
	}

	public void setCmp(double cmpTo) {
		this.cmpTo = cmpTo;
		notifyVisChange();
	}

	public void setColors(float posHue, float negHue) {
		this.posHue = posHue;
		this.negHue = negHue;
		notifyVisChange();
	}

	public float getPositiveHue() {
		return posHue;
	}

	public float getNegativeHue() {
		return negHue;
	}

	public double getDataMin() {
		return dataMin;
	}

	public double getDataMax() {
		return dataMax;
	}

	public void setDataMinMax(double dataMin, double dataMax) {
		this.dataMin = dataMin;
		this.dataMax = dataMax;
		if (minTime != null) {
			minTime = minTime.valueOf((long) dataMin);
		}
		if (maxTime != null) {
			maxTime = maxTime.valueOf((long) dataMax);
		}
		computeComparisonTolerance();
	}

	/**
	* Replies whether color shades (degrees of darkness) are used for representing
	* attribute values. By default, returns false.
	*/
	public boolean usesShades() {
		return false;
	}

	/**
	* Returns 0 if the value fits between min and max, -1 if it is less than min,
	* 1 if it is higher than max, and 999 if the value is null or not a number.
	*/
	public int whereIsValue(String value) {
		if (value == null)
			return 999;
		double v = 0.0f;
		try {
			v = Double.valueOf(value).doubleValue();
		} catch (NumberFormatException nfe) {
			return 999;
		}
		if (v < focuserMin)
			return -1;
		if (v > focuserMax)
			return 1;
		return 0;
	}

	/**
	* If a value does not fit to the interval [min,max], it is shown by a
	* special sign painted depending on whether the value is less than min
	* or more than max.
	*/
	public Color getNearColor(int where) {
		if (where == -1)
			if (focuserMin > cmpTo)
				return CS.getColor(0.2f, posHue);
			else
				return CS.getColor(0.7f, negHue);
		if (where == 1)
			if (focuserMax >= cmpTo)
				return CS.getColor(0.7f, posHue);
			else
				return CS.getColor(0.2f, negHue);
		return null;
	}

	//-------------------- getColor --------------------
	/**
	* Returns the color generated from posHue or negHue, depending on the current
	* value and the reference value for comparison
	*/
	public Color getColor(double value, double CompareTo) {
		if (value == CompareTo)
			return Color.white;
		float hue = (value > CompareTo) ? posHue : negHue;
		return CS.getColor(0.7f, hue);
	}

	public Color getPositiveColor() {
		return CS.getColor(0.7f, posHue);
	}

	public Color getNegativeColor() {
		return CS.getColor(0.7f, negHue);
	}

	//----------------------- getPaleColor ----------------------------
	/**
	* Returns an unsaturated version of positive or negative color hue
	*/
	public Color getPaleColor(boolean positive) {
		if (positive)
			return CS.getColor(0.2f, posHue);
		return CS.getColor(0.2f, negHue);
	}

	/**
	* If the value is beyond the focus interval (<minv or >maxv), returns
	* Triangle to represent this value in a map
	*/
	public Triangle checkMakeTriangle(double value) {
		if (value >= focuserMin && value <= focuserMax)
			return null;
		if (tr == null) {
			tr = new Triangle();
		}
		tr.isPositive = value >= cmpTo;
		int where = (value < focuserMin) ? -1 : 1;
		Color c = getNearColor(where);
		tr.setColor(c);
		tr.setBorderColor(c);
		return tr;
	}

	/**
	* According to the convention, this method draws the part of the legend
	* explaining this specific presentation method. However, as NumberDrawer
	* is an abstract class, it only shows the name of the attribute and then
	* calls the method showNumberEncoding.
	*/
	@Override
	public Rectangle drawMethodSpecificLegend(Graphics g, int startY, int leftMarg, int prefW) {
		g.setColor(Color.black);
		Point p = StringInRectangle.drawText(g, getAttrName(0), leftMarg, startY, prefW, true);
		Rectangle r = showNumberEncoding(g, p.y + 10, leftMarg, prefW);
		p.x -= leftMarg;
		p.y -= startY;
		if (r == null)
			return new Rectangle(leftMarg, startY, p.x, p.y);
		r.height += p.y;
		r.y = startY;
		if (r.width < p.x) {
			r.width = p.x;
		}
		return r;
	}

	/**
	* Draws the part of the legend that explains how numeric attribute values
	* are graphically encoded on the map, assuming that the name of the attribute
	* is already shown.
	*/
	abstract protected Rectangle showNumberEncoding(Graphics g, int startY, int leftMarg, int prefW);

	/**
	* Replies whether the color scale used by this visualization method may be
	* changed. This does not include interactive analytical manipulation.
	* Returns true.
	*/
	@Override
	public boolean canChangeColors() {
		return true;
	}

	/**
	* Constructs and displays a dialog for changing color hues.
	*/
	@Override
	public void startChangeColors() {
		float hues[] = new float[2];
		hues[0] = posHue;
		hues[1] = negHue;
		String prompts[] = new String[2];
		// following text:"Positive color ?"
		prompts[0] = res.getString("Positive_color_");
		// following text:"Negative color ?"
		prompts[1] = res.getString("Negative_color_");
		ColorSelDialog csd = new ColorSelDialog(2, hues, null, prompts, true, usesShades());
		OKDialog okd = new OKDialog(CManager.getAnyFrame(), csd.getName(), true);
		okd.addContent(csd);
		okd.show();
		if (okd.wasCancelled())
			return;
		setColors(csd.getHueForItem(0), csd.getHueForItem(1));
	}

//ID
	@Override
	public Hashtable getVisProperties() {
		Hashtable param;
		param = super.getVisProperties();
		if (param == null) {
			param = new Hashtable();
		}

		param.put("focuserMin", String.valueOf(focuserMin));
		param.put("focuserMax", String.valueOf(focuserMax));
		param.put("cmpTo", String.valueOf(cmpTo));
//    param.put("dataMin", String.valueOf(dataMin));
//    param.put("dataMax", String.valueOf(dataMax));
//    param.put("epsilon", String.valueOf(epsilon));
		param.put("posHue", String.valueOf(posHue));
		param.put("negHue", String.valueOf(negHue));

		return param;
	}

	@Override
	public void setVisProperties(Hashtable param) {
		double temp;
		try {
			temp = new Double((String) param.get("focuserMin")).doubleValue();
			if (!Double.isNaN(temp)) {
				focuserMin = temp;
			}
		} catch (Exception ex) {
		}
		try {
			temp = new Double((String) param.get("focuserMax")).doubleValue();
			if (!Double.isNaN(temp)) {
				focuserMax = temp;
			}
		} catch (Exception ex) {
		}
		try {
			temp = new Double((String) param.get("cmpTo")).doubleValue();
			if (!Double.isNaN(temp)) {
				cmpTo = temp;
			}
		} catch (Exception ex) {
		}

//    temp = new Float((String)param.get("dataMin")).floatValue();
//    if (!Float.isNaN(temp)) dataMin = temp;
//    temp = new Float((String)param.get("dataMax")).floatValue();
//    if (!Float.isNaN(temp)) dataMax = temp;
//    epsilon = new Float((String)param.get("epsilon")).floatValue();
		try {
			float t = new Float((String) param.get("posHue")).floatValue();
			if (!Float.isNaN(t)) {
				posHue = t;
			}
		} catch (Exception ex) {
		}
		try {
			float t = new Float((String) param.get("negHue")).floatValue();
			if (!Float.isNaN(t)) {
				negHue = t;
			}
		} catch (Exception ex) {
		}

		super.setVisProperties(param);
//    setDataMinMax(dataMin, dataMax);
//    setFocuserMinMax(focuserMin, focuserMax);
	}
//~ID
}
