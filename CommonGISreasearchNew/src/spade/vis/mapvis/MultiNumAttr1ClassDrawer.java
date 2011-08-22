package spade.vis.mapvis;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import spade.analysis.classification.NumAttr1Classifier;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.Metrics;
import spade.lib.basicwin.OKDialog;
import spade.lib.color.ColorSelDialog;
import spade.lib.util.FloatArray;
import spade.lib.util.NumRange;
import spade.lib.util.StringUtil;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataTable;

/**
* Represents results of multiple 1-dimensional classifications by special signs
* with coloured segments.
*/
public class MultiNumAttr1ClassDrawer extends MultiClassPresenter {
	/**
	* Common Min and Max values for all attributes used in the visualization
	*/
	protected double dataMin = Double.NaN, dataMax = Double.NaN;

	/**
	* Checks if this visualization method is applicable to the given set of
	* attributes. All the attributes must be numeric.
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
		if (nattr < 2) {
			err = errors[7];
			return false;
		}
		if (table == null) {
			err = errors[1];
			return false;
		}
		dataMin = Float.NaN;
		dataMax = Float.NaN;
		for (int i = 0; i < attr.size(); i++) {
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
			if (nr == null || Double.isNaN(nr.maxValue)) {
				continue;
			}
			if (Double.isNaN(dataMin) || dataMin > nr.minValue) {
				dataMin = nr.minValue;
			}
			if (Double.isNaN(dataMax) || dataMax < nr.maxValue) {
				dataMax = nr.maxValue;
			}
		}
		return true;
	}

	/**
	* Checks semantic applicability of this visualization method to the
	* attributes previously set in the visualizer. Uses the DataInformer to
	* get semantic information about the attributes. Returns true if the
	* attributes are comparable.
	*/
	@Override
	public boolean checkSemantics() {
		err = null;
		if (attr == null || attr.size() < 2)
			return true;
		if (table == null) {
			err = errors[1];
			return false;
		}
		if (!(table instanceof DataTable)) {
			err = errors[11];
			return false;
		}
		DataTable dt = (DataTable) table;
		if (dt.getSemanticsManager() == null) {
			err = errors[11];
			return false;
		}
		boolean result = dt.getSemanticsManager().areAttributesComparable(attr, visName);
		// following string: "The attributes are not comparable!"
		if (!result) {
			err = res.getString("The_attributes_are1");
		}
		return result;
	}

	/**
	* The Visualizer sets its parameters.
	*/
	@Override
	public void setup() {
		if (table == null || attr == null || !isApplicable(attr))
			return;
		float br = (float) (dataMax - dataMin) / 3;
		if (classifiers == null || classifiers.size() != attr.size()) {
			classifiers = new Vector(attr.size(), 1);
			float breaks[] = new float[2];
			breaks[0] = (float) dataMin + br;
			breaks[1] = (float) dataMin + br * 2;
			for (int i = 0; i < attr.size(); i++) {
				NumAttr1Classifier ncl = new NumAttr1Classifier(table, (String) attr.elementAt(i));
				classifiers.addElement(ncl);
				if (aTrans != null) {
					ncl.setAttributeTransformer(aTrans, false);
				}
				if (subAttr != null && subAttr.size() > i && subAttr.elementAt(i) != null) {
					ncl.setSubAttributes((Vector) subAttr.elementAt(i), 0);
				}
				if (invariants != null && invariants.size() > i && invariants.elementAt(i) != null) {
					ncl.setInvariant((String) invariants.elementAt(i), 0);
				}
				ncl.setBreaks(breaks, 2);
			}
		} else {
			NumAttr1Classifier ncl = (NumAttr1Classifier) classifiers.elementAt(0);
			FloatArray breaks = ncl.getBreaks();
			if (breaks == null) {
				breaks = new FloatArray(10, 10);
			}
			for (int i = breaks.size() - 1; i >= 0; i--)
				if (breaks.elementAt(i) <= dataMin || breaks.elementAt(i) >= dataMax) {
					breaks.removeElementAt(i);
				}
			if (breaks.size() < 1) {
				breaks.addElement((float) dataMin + br);
				breaks.addElement((float) dataMin + br * 2);
			}
			ncl.setBreaks(breaks);
			for (int i = 1; i < classifiers.size(); i++) {
				ncl = (NumAttr1Classifier) classifiers.elementAt(i);
				ncl.setBreaks(breaks);
			}
		}
		if (symbol != null) {
			setupSymbol(symbol);
		}
	}

	/**
	* Draws the part of the legend explaining this specific presentation method.
	*/
	@Override
	public Rectangle drawMethodSpecificLegend(Graphics g, int startY, int leftmarg, int prefW) {
		g.setColor(Color.black);
		Rectangle r = drawAttrNames(g, startY, leftmarg, prefW, true);
		if (r == null) {
			r = new Rectangle(leftmarg, startY, 0, 0);
		}
		int y = r.y + r.height;
		if (classifiers != null && classifiers.size() > 0 && !Double.isNaN(dataMin) && dataMin < dataMax) {
			NumAttr1Classifier ncl = (NumAttr1Classifier) classifiers.elementAt(0);
			if (ncl.getNClasses() > 0) {
				FontMetrics fm = g.getFontMetrics();
				int asc = fm.getAscent(), step = asc + 5, rectW = Metrics.mm() * 4, ssx = leftmarg + rectW + Metrics.mm();
				y += asc / 2;

				String str = StringUtil.doubleToStr(dataMax, dataMin, dataMax);
				g.drawString(str, ssx, y + asc / 2);
				int mx = ssx + fm.stringWidth(str);
				int y0 = y;
				for (int i = ncl.getNClasses() - 1; i >= 0; i--) {
					g.setColor(ncl.getClassColor(i));
					g.fillRect(leftmarg, y, rectW, step);
					g.setColor(Color.black);
					g.drawRect(leftmarg, y, rectW, step);
					if (i == 0) {
						str = StringUtil.doubleToStr(dataMin, dataMin, dataMax);
					} else {
						str = StringUtil.doubleToStr(ncl.getBreak(i - 1), dataMin, dataMax);
					}
					y += step;
					g.drawString(str, ssx, y + asc / 2);
					int x = ssx + fm.stringWidth(str);
					if (x > mx) {
						mx = x;
					}
				}
				if (r.width < mx - leftmarg) {
					r.width = mx - leftmarg;
				}
				y += Metrics.mm();
			}
		}
		r.height = y - startY;
		return r;
	}

	/**
	* Returns the minimum value among all attributes
	*/
	public double getDataMin() {
		return dataMin;
	}

	/**
	* Returns the maximum value among all attributes
	*/
	public double getDataMax() {
		return dataMax;
	}

	/**
	* Returns the class breaks used in the individual classifiers
	*/
	public FloatArray getBreaks() {
		if (classifiers == null || classifiers.size() < 1)
			return null;
		return ((NumAttr1Classifier) classifiers.elementAt(0)).getBreaks();
	}

	/**
	* Sets the class breaks to be used in the individual classifiers
	*/
	public void setBreaks(FloatArray breaks) {
		if (classifiers == null)
			return;
		for (int i = 0; i < classifiers.size(); i++) {
			((NumAttr1Classifier) classifiers.elementAt(i)).setBreaks(breaks);
		}
		setColorsInSignInstance();
	}

	/**
	* Sets the class breaks to be used in the individual classifiers
	*/
	public void setBreaks(float[] br, int nBreaks) {
		if (classifiers == null)
			return;
		for (int i = 0; i < classifiers.size(); i++) {
			((NumAttr1Classifier) classifiers.elementAt(i)).setBreaks(br, nBreaks);
		}
		setColorsInSignInstance();
	}

	/**
	* Sets the value of the class break with the given index
	*/
	public void setBreak(float br, int idx) {
		if (classifiers == null)
			return;
		for (int i = 0; i < classifiers.size(); i++) {
			((NumAttr1Classifier) classifiers.elementAt(i)).setBreak(br, idx);
		}
	}

	/**
	* Sets the hue to be used in the individual classifiers for the representation
	* of attribute values above the midpoint
	*/
	public void setPositiveHue(float hue) {
		if (classifiers == null)
			return;
		for (int i = 0; i < classifiers.size(); i++) {
			((NumAttr1Classifier) classifiers.elementAt(i)).setPositiveHue(hue);
		}
	}

	/**
	* Returns the hue used in the individual classifiers for the representation
	* of attribute values above the midpoint
	*/
	public float getPositiveHue() {
		if (classifiers == null || classifiers.size() < 1)
			return 0f;
		return ((NumAttr1Classifier) classifiers.elementAt(0)).getPositiveHue();
	}

	/**
	* Sets the hue to be used in the individual classifiers for the representation
	* of attribute values below the midpoint
	*/
	public void setNegativeHue(float hue) {
		if (classifiers == null)
			return;
		for (int i = 0; i < classifiers.size(); i++) {
			((NumAttr1Classifier) classifiers.elementAt(i)).setNegativeHue(hue);
		}
	}

	/**
	* Returns the hue used in the individual classifiers for the representation
	* of attribute values above the midpoint
	*/
	public float getNegativeHue() {
		if (classifiers == null || classifiers.size() < 1)
			return 0f;
		return ((NumAttr1Classifier) classifiers.elementAt(0)).getNegativeHue();
	}

	/**
	* Sets the middle color of the diverging color scale
	*/
	public void setMiddleColor(Color color) {
		if (classifiers == null)
			return;
		for (int i = 0; i < classifiers.size(); i++) {
			((NumAttr1Classifier) classifiers.elementAt(i)).setMiddleColor(color);
		}
	}

	/**
	* Returns the middle color of the diverging color scale
	*/
	public Color getMiddleColor() {
		if (classifiers == null || classifiers.size() < 1)
			return Color.white;
		return ((NumAttr1Classifier) classifiers.elementAt(0)).getMiddleColor();
	}

	public void setClassColor(Color color, int classN) {
		if (classifiers == null)
			return;
		for (int i = 0; i < classifiers.size(); i++) {
			((NumAttr1Classifier) classifiers.elementAt(i)).setClassColor(color, classN);
		}
		setColorsInSignInstance();
	}

	/**
	* Replies whether the color scale used by this visualization method may be
	* changed. This does not include interactive analytical manipulation.
	*/
	@Override
	public boolean canChangeColors() {
		return true;
	}

	/**
	* Gives the user an opportunity to change interactively colors assigned to
	* classes. This method starts the dialog for class color changing.
	*/
	@Override
	public void startChangeColors() {
		float hues[] = new float[2];
		hues[0] = getPositiveHue();
		hues[1] = getNegativeHue();
		String prompts[] = new String[2];
		// following text: "Positive color ?"
		prompts[0] = res.getString("Positive_color_");
		// following text: "Negative color ?"
		prompts[1] = res.getString("Negative_color_");
		ColorSelDialog csd = new ColorSelDialog(2, hues, getMiddleColor(), prompts, true, true);
		OKDialog okd = new OKDialog(CManager.getAnyFrame(null), csd.getName(), true);
		okd.addContent(csd);
		okd.show();
		if (okd.wasCancelled())
			return;
		setPositiveHue(csd.getHueForItem(0));
		setNegativeHue(csd.getHueForItem(1));
		setMiddleColor(csd.getMidColor());
		if (pcSupport != null) {
			pcSupport.firePropertyChange("hues", null, null);
		}
	}

//ID
	public double getMiddleValue() {
		if (classifiers == null || classifiers.size() < 1)
			return Double.NaN;
		else
			return ((NumAttr1Classifier) classifiers.elementAt(0)).getMiddleValue();
	}

	@Override
	public Hashtable getVisProperties() {
		Hashtable param = null;
		try {
			param = super.getVisProperties();
		} catch (Exception ex) {
		}
		if (param == null) {
			param = new Hashtable();
		}

//    param.put("dataMin", String.valueOf(dataMin));
//    param.put("dataMax", String.valueOf(dataMax));
		param.put("posHue", String.valueOf(getPositiveHue()));
		param.put("negHue", String.valueOf(getNegativeHue()));
		param.put("middleColor", Integer.toHexString(getMiddleColor().getRGB()).substring(2));
		if (useSigns) {
			param.put("columns", String.valueOf(getNColumns()));
		}

		if (classifiers == null || classifiers.size() < 1 || ((NumAttr1Classifier) classifiers.elementAt(0)).getBreaks() == null) {
			param.put("breaks", "null");
		} else {
			param.put("middleValue", String.valueOf(getMiddleValue()));
			FloatArray breaks = ((NumAttr1Classifier) classifiers.elementAt(0)).getBreaks();
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < breaks.size(); i++) {
				sb.append(String.valueOf(breaks.elementAt(i)));
				sb.append(" ");
			}
			param.put("breaks", sb.toString());
		}

		return param;
	}

	@Override
	public void setVisProperties(Hashtable param) {
		float temp;
//    temp = new Float((String)param.get("dataMin")).floatValue();
//    if (!Float.isNaN(temp)) dataMin = temp;
//    temp = new Float((String)param.get("dataMax")).floatValue();
		try {
			temp = new Float((String) param.get("posHue")).floatValue();
			if (!Float.isNaN(temp)) {
				setPositiveHue(temp);
			}
		} catch (Exception ex) {
		}
		try {
			temp = new Float((String) param.get("negHue")).floatValue();
			if (!Float.isNaN(temp)) {
				setNegativeHue(temp);
			}
		} catch (Exception ex) {
		}
		try {
			setMiddleColor(new Color(Integer.parseInt((String) param.get("middleColor"), 16)));
		} catch (Exception ex) {
		}
		try {
			setNColumns(Integer.parseInt((String) param.get("columns")));
		} catch (Exception ex) {
		}

		String sBreaks = (String) param.get("breaks");
		if (sBreaks == null || sBreaks.length() < 1 || sBreaks.equals("null")) {
			setBreaks(null);
		} else {
			StringTokenizer st = new StringTokenizer(sBreaks, " ");
			FloatArray newbreaks = new FloatArray();
			while (st.hasMoreTokens()) {
				newbreaks.addElement(new Float(st.nextToken()).floatValue());
			}
			setBreaks(newbreaks);
		}

		float middleValue = new Float((String) param.get("middleValue")).floatValue();
		if (classifiers != null) {
			for (int i = 0; i < classifiers.size(); i++) {
				((NumAttr1Classifier) classifiers.elementAt(i)).setMiddleValue(middleValue);
			}
		}

		super.setVisProperties(param);
//    setDataMinMax(dataMin, dataMax);
//    setFocuserMinMax(focuserMin, focuserMax);
	}
//~ID
}
