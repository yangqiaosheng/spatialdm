package spade.analysis.classification;

import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.Vector;

import spade.lib.basicwin.CManager;
import spade.lib.basicwin.Metrics;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.StringInRectangle;
import spade.lib.color.CS;
import spade.lib.color.ColorSelDialog;
import spade.lib.color.ColorBrewer.Schemes;
import spade.lib.lang.Language;
import spade.lib.util.FloatArray;
import spade.lib.util.IntArray;
import spade.lib.util.NumRange;
import spade.lib.util.StringUtil;
import spade.time.TimeMoment;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.AttributeTypes;
import spade.vis.database.ThematicDataItem;

/**
* Implements classification of objects on the basis of a single numeric
* attribute by breaking its value range into intervals.
*/

public class NumAttr1Classifier extends TableClassifier implements SingleAttributeClassifier {
	static ResourceBundle res = Language.getTextResource("spade.analysis.classification.Res");

	protected double minVal = Double.NaN, maxVal = Double.NaN;
	protected FloatArray breaks = null;
	protected int initialNClasses = 7;
	/**
	 * If the attribute is temporal, these fields contain the absolute minimum and
	 * maximum time moments.
	 */
	protected TimeMoment minTime = null, maxTime = null;
	/**
	* Hues for bichromatic color scale
	*/
	protected float posHue = -1101.0f, negHue = 0.6f;
	/**
	* Color for the "middle" class in dichromatic color scale
	*/
	protected Color middleColor = Color.white;
	/**
	* The colors corresponding to the classes
	*/
	protected Vector colors = null;

	public NumAttr1Classifier() {
	}

	public NumAttr1Classifier(AttributeDataPortion data, String attrId) {
		this();
		if (data != null) {
			setTable(data);
		}
		setAttribute(attrId);
	}

	/**
	* Checks if this visualization method is applicable to the specified number
	* of attributes having the specified types. The possible types see in
	* @see spade.vis.database.AttributeTypes
	*/
	@Override
	public boolean isApplicable(int attrNumber, char attrTypes[]) {
		return attrNumber == 1 && (AttributeTypes.isNumericType(attrTypes[0]) || AttributeTypes.isTemporal(attrTypes[0]));
	}

	/**
	* Sets the attribute to be used for classification.
	*/
	public void setAttribute(String attrId) {
		Vector v = new Vector(1, 1);
		v.addElement(attrId);
		setAttributes(v);
	}

	/**
	* Returns the identifier of the numeric attribute used for
	* classification
	*/
	@Override
	public String getAttrId() {
		if (attr == null || attr.size() < 1)
			return null;
		return (String) attr.elementAt(0);
	}

	/**
	 * Returns the index (column number) in the table of the attribute used for
	 * the classification
	 */
	@Override
	public int getAttrColumnN() {
		return super.getAttrColumnN(0);
	}

	public void setBreaks(FloatArray breaks) {
		this.breaks = breaks;
	}

	public void setBreaks(float[] br, int nBreaks) {
		if (breaks == null) {
			breaks = new FloatArray(10, 10);
		} else {
			breaks.removeAllElements();
		}
		for (int i = 0; i < nBreaks; i++) {
			breaks.addElement(br[i]);
		}
	}

	public void setBreak(float br, int idx) {
		if (idx >= 0 && breaks != null && breaks.size() > idx) {
			breaks.setElementAt(br, idx);
		}
	}

	public float getBreak(int idx) {
		if (idx >= 0 && breaks != null && breaks.size() > idx)
			return breaks.elementAt(idx);
		return Float.NaN;
	}

	public FloatArray getBreaks() {
		return breaks;
	}

	@Override
	public int getNClasses() {
		if (breaks == null)
			return 1;
		return breaks.size() + 1;
	}

	public Vector getColors() {
		return colors;
	}

	public void setColors(Vector colors) {
		this.colors = colors;
	}

	protected void findMinMax() {
		minVal = Double.NaN;
		maxVal = Double.NaN;
		NumRange r = getAttrValueRange(0);
		if (r != null) {
			minVal = r.minValue;
			maxVal = r.maxValue;
		}
		findMinMaxTime();
	}

	public double getMaxVal() {
		return maxVal;
	}

	public double getMinVal() {
		return minVal;
	}

	/**
	 * If the attribute is temporal, finds the minimum and maximum time moments
	 */
	public void findMinMaxTime() {
		minTime = null;
		maxTime = null;
		if (data == null)
			return;
		int colN = getAttrColumnN(0);
		if (colN < 0)
			return;
		if (!data.isAttributeTemporal(colN))
			return;
		for (int i = 0; i < data.getDataItemCount(); i++) {
			Object val = data.getAttrValue(colN, i);
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

	/**
	 * Informs if the attribute is temporal
	 */
	public boolean isAttrTemporal() {
		return minTime != null && maxTime != null;
	}

	/**
	 * If the attribute is temporal, returns the minimum time moment
	 */
	public TimeMoment getMinTime() {
		return minTime;
	}

	/**
	 * If the attribute is temporal, returns the maximum time moment
	 */
	public TimeMoment getMaxTime() {
		return maxTime;
	}

	public void setInitialNClasses(int initialNClasses) {
		this.initialNClasses = initialNClasses;
	}

	/**
	* Prepares its internal variables to the classification.
	*/
	@Override
	public void setup() {
		findMinMax();
		if (breaks == null || breaks.size() < 1) {
			float br[] = AutoNumClassifier.doEqualIntervals(initialNClasses, minVal, maxVal);
			if (br != null) {
				breaks = new FloatArray(20, 10);
				for (int i = 1; i < br.length - 1; i++) {
					breaks.addElement(br[i]);
				}
			}
		}

		int ncl = getNClasses();
		if (colors == null || colors.size() < ncl) {
			colors = new Vector(20, 10);
			for (int i = 0; i < ncl; i++) {
				colors.addElement(Schemes.getColor(1f * i / (ncl - 1), 1, true));
			}
			middleColor = (Color) colors.elementAt(ncl / 2);
		}
		notifyClassesChange();
	}

	/**
	 * Normally the colors are generated by Slider. This method is needed
	 * when no slider exists (e.g. in a case when the classifier is used
	 * without any UI).
	 */
	public void setupColors() {
		if (colors == null) {
			colors = new Vector(20, 10);
		} else {
			colors.removeAllElements();
		}
		int ncl = getNClasses();
		//check if this is a ColorBrewer scale
		boolean isInverted = posHue <= -1100;
		int cbScaleN = (posHue >= 0) ? -1 : -1000 - (int) posHue - ((isInverted) ? 100 : 0);
		if (cbScaleN >= 0) {// ColorBrewer!
			for (int i = 0; i < ncl; i++) {
				colors.addElement(Schemes.getColor(1f * i / (ncl - 1), cbScaleN, isInverted));
			}
			middleColor = (Color) colors.elementAt(ncl / 2);
		} else {
			int mpc = ncl / 2;
			for (int i = 0; i < ncl; i++)
				if (i == mpc) {
					colors.addElement(middleColor);
				} else if (i < mpc) {
					colors.addElement((negHue >= 0) ? CS.getColor(((float) mpc - i) / mpc, negHue) : Color.white);
				} else {
					colors.addElement((posHue >= 0) ? CS.getColor(((float) i - mpc) / (ncl - mpc), posHue) : Color.white);
				}
		}
	}

	public String getValueAsString(double val) {
		if (minTime == null)
			return StringUtil.doubleToStr(val, minVal, maxVal);
		return minTime.valueOf((long) val).toString();
	}

	@Override
	public String getClassName(int classN) {
		if (classN < 0 || classN > getNClasses())
			return null;
		if (Double.isNaN(minVal) || Double.isNaN(maxVal)) {
			findMinMax();
		}
		if (Double.isNaN(minVal) || Double.isNaN(maxVal))
			return null;
		if (getNClasses() == 1)
			return getValueAsString(minVal) + ".." + getValueAsString(maxVal);
		if (classN == 0)
			return "< " + getValueAsString(breaks.elementAt(0));
		if (classN == breaks.size())
			return ">= " + getValueAsString(breaks.elementAt(classN - 1));
		return "[" + getValueAsString(breaks.elementAt(classN - 1)) + ".." + getValueAsString(breaks.elementAt(classN)) + ")";
	}

	/*
	* color processing
	*/
	public void setPositiveHue(float hue) {
		posHue = hue;
	}

	public float getPositiveHue() {
		return posHue;
	}

	public void setNegativeHue(float hue) {
		negHue = hue;
	}

	public float getNegativeHue() {
		return negHue;
	}

	/**
	* Sets the middle color of the diverging color scale
	*/
	public void setMiddleColor(Color color) {
		if (color != null) {
			middleColor = color;
		}
	}

	/**
	* Returns the middle color of the diverging color scale
	*/
	public Color getMiddleColor() {
		return middleColor;
	}

	@Override
	public Color getClassColor(int classN) {
		if (classN < 0 || classN > getNClasses())
			return null;
		if (colors != null && classN < colors.size())
			return (Color) colors.elementAt(classN);
		return CS.getNthPureColor(classN, getNClasses());
	}

	public void setClassColor(Color color, int classN) {
		if (classN < 0 || classN > getNClasses())
			return;
		if (colors == null) {
			colors = new Vector(20, 10);
		}
		if (classN < colors.size()) {
			colors.setElementAt(color, classN);
		} else {
			for (int i = colors.size(); i < classN; i++) {
				colors.addElement(CS.getNthPureColor(i, getNClasses()));
			}
			colors.addElement(color);
		}
//ID
/*
    if (color.getRGB()==middleColor.getRGB()) {
      if (classN==0) middleValue=(breaks.elementAt(0)+minVal)/2; else
      if (classN==colors.size()-1) middleValue=(maxVal+breaks.elementAt(classN-1))/2; else
      middleValue=(breaks.elementAt(classN)+breaks.elementAt(classN-1))/2;
    }
*/
//~ID
	}

	@Override
	public int getRecordClass(ThematicDataItem dit) {
		if (dit == null)
			return -1;
		double val = getNumericAttrValue(dit, 0);
		if (Double.isNaN(val))
			return -1;
		if (getNClasses() == 1)
			return 0;
		for (int i = 0; i < breaks.size(); i++)
			if (val < breaks.elementAt(i))
				return i;
		return breaks.size();
	}

	/**
	 * Returns the color corresponding to the given attribute value
	 */
	@Override
	public Color getColorForValue(Object value) {
		if (value == null)
			return null;
		double numVal = Double.NaN;
		if (value instanceof Double) {
			numVal = ((Double) value).doubleValue();
		} else if (value instanceof TimeMoment) {
			numVal = ((TimeMoment) value).toNumber();
		} else {
			try {
				numVal = Double.valueOf(value.toString()).doubleValue();
			} catch (NumberFormatException nfe) {
			}
		}
		if (Double.isNaN(numVal))
			return null;
		if (getNClasses() == 1)
			return getClassColor(0);
		for (int i = 0; i < breaks.size(); i++)
			if (numVal < breaks.elementAt(i))
				return getClassColor(i);
		return getClassColor(breaks.size());
	}

	/**
	* A LegendDrawer should be able to add its description at the end of the
	* legend formed by previous legend drawers. The argument startY specifies
	* the vertical position from which the LegendDrawer should start drawing
	* its part of the legend.The argument leftMarg specifies the left margin
	* (amount of space on the left to be kept blank). The argument prefW
	* specifies the preferrable width of the legend (to avoid horizontal
	* scrolling).
	* The method should return the rectangle occupied by the drawn part of
	* the legend.
	*/
	@Override
	public Rectangle drawLegend(Component c, Graphics g, int startY, int leftmarg, int prefW) {
		return drawClassStatistics(g, startY, leftmarg, prefW);
	}

	@Override
	public Rectangle drawClassStatistics(Graphics g, int startY, int leftmarg, int prefW) {
		if (data == null || attr == null || attr.size() < 1)
			return new Rectangle(leftmarg, startY, 0, 0);

		Point p = StringInRectangle.drawText(g, getAttributeName(0), leftmarg, startY, prefW, true);
		int y = p.y, maxX = p.x;

		if (breaks == null || breaks.size() < 1)
			return new Rectangle(leftmarg, startY, maxX - leftmarg, y - startY);
		if (Double.isNaN(minVal) || Double.isNaN(maxVal)) {
			findMinMax();
		}
		if (Double.isNaN(minVal) || Double.isNaN(maxVal))
			return new Rectangle(leftmarg, startY, maxX - leftmarg, y - startY);

		FontMetrics fm = g.getFontMetrics();
		int asc = fm.getAscent(), step = asc + 5, rectW = Metrics.mm() * 4, ssx = leftmarg + rectW + Metrics.mm();
		y += asc / 2;

		String str = getValueAsString(maxVal);
		g.setColor(Color.black);
		g.drawString(str, ssx, y + asc / 2);
		int x = ssx + fm.stringWidth(str);
		if (x > maxX) {
			maxX = x;
		}
		int y0 = y, mx = ssx;
		for (int i = getNClasses() - 1; i >= 0; i--) {
			g.setColor(getClassColor(i));
			g.fillRect(leftmarg, y, rectW, step);
			g.setColor(Color.black);
			g.drawRect(leftmarg, y, rectW, step);
			if (breaks == null || i == 0) {
				str = getValueAsString(minVal);
			} else {
				str = getValueAsString(breaks.elementAt(i - 1));
			}
			y += step;
			g.drawString(str, ssx, y + asc / 2);
			x = ssx + fm.stringWidth(str);
			if (x > mx) {
				mx = x;
			}
		}
		if (maxX < mx) {
			maxX = mx;
		}

		int nTotal = getSetSize();
		if (nTotal > 0) {
			y = y0 + step / 2 + asc / 2;
			mx += 2 * Metrics.mm();
			IntArray counts = getClassSizes(), countsFlt = getFilteredClassSizes();
			for (int i = getNClasses() - 1; i >= 0; i--) {
				float perc = 100.0f * counts.elementAt(i) / nTotal;
				str = String.valueOf(counts.elementAt(i)) + " objects (" + StringUtil.floatToStr(perc, 0.0f, 100.0f) + "%)";
				if (countsFlt != null && counts.elementAt(i) != countsFlt.elementAt(i)) {
					perc = 100.0f * countsFlt.elementAt(i) / counts.elementAt(i);
					str += "; active: " + countsFlt.elementAt(i) + " objects (" + StringUtil.floatToStr(perc, 0.0f, 100.0f) + "%)";
				}
				g.drawString(str, mx, y);
				x = mx + fm.stringWidth(str);
				if (x > maxX) {
					maxX = x;
				}
				y += step;
			}
		} else {
			y += asc;
		}

		return new Rectangle(leftmarg, startY, maxX - leftmarg + Metrics.mm(), y - startY);
	}

	/**
	* Checks if new values appeared in data and if this affects the classes
	*/
	@Override
	protected void checkValues() {
		findMinMax();
	}

	/**
	* A classifier must give the user an opportunity to change interactively
	* colors assigned to classes. This method starts the procedure of class
	* color changing.
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
		notifyChange("hues");
	}

//ID
	private double middleValue = Double.NaN;

	public double getMiddleValue() {
//    if (!Float.isNaN(middleValue))
//      return middleValue;
//    else
		if (colors == null)
			return middleValue;
		else {
			double val = Double.NaN;
			for (int i = 0; i < colors.size(); i++)
				if (((Color) colors.elementAt(i)).equals(middleColor)) {
					if (i == 0) {
						val = (breaks.elementAt(0) + minVal) / 2;
					} else if (i == colors.size() - 1) {
						val = (maxVal + breaks.elementAt(i - 1)) / 2;
					} else {
						val = (breaks.elementAt(i) + breaks.elementAt(i - 1)) / 2;
					}
				}
			if (Double.isNaN(val))
				return (minVal + maxVal) / 2;
			return val;
		}
	}

	public void setMiddleValue(double value) {
		middleValue = value;
	}

	@Override
	public Hashtable getVisProperties() {
		Hashtable param;
		param = super.getVisProperties();
		if (param == null) {
			param = new Hashtable();
		}

//    param.put("minVal", String.valueOf(minVal));
//    param.put("maxVal", String.valueOf(maxVal));
		param.put("posHue", String.valueOf(posHue));
		param.put("negHue", String.valueOf(negHue));
		param.put("middleColor", Integer.toHexString(middleColor.getRGB()).substring(2));

		if (breaks == null) {
			param.put("breaks", "null");
		} else {
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < breaks.size(); i++) {
				sb.append(String.valueOf(breaks.elementAt(i)));
				sb.append(" ");
			}
			param.put("breaks", sb.toString());
		}
/*
    if (colors == null)
      param.put("colors", "null");
    else {
      StringBuffer sb = new StringBuffer();
      for (int i=0; i<colors.size(); i++) {
        sb.append(Integer.toHexString(((Color)colors.elementAt(i)).getRGB()).substring(2));
        sb.append(" ");
      }
      param.put("colors", sb.toString());
    }
*/

		param.put("middleValue", String.valueOf(getMiddleValue()));

		return param;
	}

	@Override
	public void setVisProperties(Hashtable param) {
		float temp;
//    temp = new Float((String)param.get("minVal")).floatValue();
//    if (!Float.isNaN(temp)) minVal = temp;
//    temp = new Float((String)param.get("maxVal")).floatValue();
//    if (!Float.isNaN(temp)) maxVal = temp;
		temp = new Float((String) param.get("posHue")).floatValue();
		if (!Float.isNaN(temp)) {
			posHue = temp;
		}
		temp = new Float((String) param.get("negHue")).floatValue();
		if (!Float.isNaN(temp)) {
			negHue = temp;
		}
		try {
			middleColor = new Color(Integer.parseInt((String) param.get("middleColor"), 16));
		} catch (Exception ex) {
		}

		String sBreaks = (String) param.get("breaks");
		if (sBreaks == null || sBreaks.length() < 1 || sBreaks.equals("null")) {
			breaks = null;
		} else {
			StringTokenizer st = new StringTokenizer(sBreaks, " ");
			FloatArray newbreaks = new FloatArray();
			while (st.hasMoreTokens()) {
				newbreaks.addElement(new Float(st.nextToken()).floatValue());
			}
			breaks = newbreaks;
		}

		middleValue = new Float((String) param.get("middleValue")).floatValue();

//    notifyChange("hues");
		super.setVisProperties(param);
	}
//~ID
}
