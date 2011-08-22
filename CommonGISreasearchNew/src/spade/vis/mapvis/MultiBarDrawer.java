package spade.vis.mapvis;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.beans.PropertyChangeListener;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.Vector;

import spade.lib.basicwin.Drawing;
import spade.lib.basicwin.StringInRectangle;
import spade.lib.color.CS;
import spade.lib.lang.Language;
import spade.lib.util.StringUtil;
import spade.vis.database.DataTable;
import spade.vis.database.ThematicDataItem;
import spade.vis.geometry.BarChart;
import spade.vis.geometry.Sign;

/**
* Implements visualization of several comparable attributes by bar charts.
*/

public class MultiBarDrawer extends MultiNumberDrawer implements AttrColorHandler, SignDrawer {
	static ResourceBundle res = Language.getTextResource("spade.vis.mapvis.Res");
	public static final int mm = Math.round(java.awt.Toolkit.getDefaultToolkit().getScreenResolution() / 25.33f);

	/**
	* An instance of a BarChart
	*/
	protected BarChart symbol = null;

	/**
	* Constructs an instance of BarChart for further use depending on the
	* required diagram type
	*/
	protected void checkCreateSymbol() {
		if (symbol == null) {
			symbol = new BarChart();
			setupSymbol(symbol);
			symbol.setCmpMode(true);
			symbol.cmpValue = (float) ((0f - MIN) / (MAX - MIN));
			int w = Math.round(attr.size() * Sign.mm * 1.5f);
			symbol.setMaxWidth(w);
		}
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
	* This method informs whether the Visualizer produces diagrams.
	* This is important for defining the order of drawing of GeoLayers on the
	* map: the diagrams should be drawn on top of all geography.
	*/
	@Override
	public boolean isDiagramPresentation() {
		return true;
	}

	/**
	* The Visualizer sets its parameters. Calls parent's setup, and then setups
	* its sign.
	*/
	@Override
	public void setup() {
		super.setup();
		setGlobalMinMax();
		if (symbol != null) {
			setupSymbol(symbol);
		}
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
		if (super.adjustToDataChange(valuesTransformed)) {
			setGlobalMinMax();
			if (symbol != null) {
				setupSymbol(symbol);
			}
			return true;
		}
		return false;
	}

	/**
	* Informs whether this visualizer allows each of the visualized attributes to
	* be transformed individually (returns true) or requires all attributes to
	* be transformed in the same way (returns false). Returns false because
	* the attributes must be comparable.
	*/
	@Override
	public boolean getAllowTransformIndividually() {
		return false;
	}

	protected void setupSymbol(BarChart sgn) {
		sgn.setMayChangeProperty(Sign.USE_FRAME, false);
		int attrN = (attr == null) ? 1 : attr.size();
		sgn.setNSegments(attrN);
		for (int i = 0; i < attrN; i++) {
			float f = (float) (0.2 + 0.8 * Math.random());
			if (i == 2) {
				f = (sgn.getSegmentPart(0) + sgn.getSegmentPart(1)) / 2;
			}
			sgn.setSegmentPart(i, f);
			sgn.setSegmentWeight(i, 1.0f / attrN);
		}
		if (colorHandler == null || attr == null) {
			sgn.setDefaultColors();
		} else {
			for (int i = 0; i < attrN; i++) {
				sgn.setSegmentColor(i, super.getColorForAttribute(i));
			}
		}
	}

	/**
	* Sets the list of identifiers of attributes to be visualized.
	* Additionally, full names of the attributes (if different from the
	* identifiers) may be specified.
	* UtilitySignDrawer redefines the method from the ancestor in order to
	* reset the sign structure
	*/
	@Override
	public void setAttributes(Vector attributes, Vector attrNames) {
		super.setAttributes(attributes, attrNames);
		setGlobalMinMax();
		if (symbol != null) {
			setupSymbol(symbol);
		}
	}

	double cmpValAbs = 0f, cmpValRel = Double.NaN;

	@Override
	public void setCmp(double cmpValAbs) {
		if (symbol != null) {
			symbol.setCmpMode(true);
		}
		this.cmpValAbs = cmpValAbs;
		this.cmpValRel = (cmpValAbs - MIN) / (MAX - MIN);
		notifyVisChange();
	}

	protected double MIN = Double.NaN, MAX = Double.NaN;

	protected void setGlobalMinMax() {
		MIN = dataMIN;
		MAX = dataMAX;
		if (MIN > 0) {
			MIN = 0f;
		}
		if (MAX < 0) {
			MAX = 0f;
		}
		if (focuserMIN > MIN) {
			MIN = focuserMIN;
		}
		if (focuserMAX < MAX) {
			MAX = focuserMAX;
		}
	}

	@Override
	public void setFocuserMinMax(double focuserMin, double focuserMax) {
		this.focuserMIN = focuserMin;
		this.focuserMAX = focuserMax;
		setGlobalMinMax();
		notifyVisChange();
	}

	/**
	* This method is called by a GeoObject. The GeoObject sends its DataItem
	* to the Visualizer, and the Visualizer returns an Object specifying the
	* presentation of data contained in this DataItem. This may be, in
	* particular, Color or Diagram. Typically the argument DataItem is
	* a ThematicDataItem, although in some cases (e.g. manual classification)
	* this is not required.
	*/
	@Override
	public Object getPresentation(ThematicDataItem dit) {
		if (dit == null || attr == null || attr.size() < 1)
			return null;
		if (sdController != null && !sdController.mustDrawObject(dit.getId()))
			return null;
		if (Double.isNaN(MIN)) {
			setGlobalMinMax();
		}
		if (Double.isNaN(cmpValRel)) {
			//if (cmpValAbs<MIN) cmpValAbs=MIN;
			//if (cmpValAbs>MAX) cmpValAbs=MAX;
			cmpValRel = (cmpValAbs - MIN) / (MAX - MIN);
		}
		checkCreateSymbol();
		symbol.cmpValue = Float.NaN;
		boolean allNaNs = true;
		for (int i = 0; i < attr.size(); i++)
			if (Double.isNaN(getDataMin(i))) {
				symbol.setSegmentPart(i, 0.0f);
			} else {
				double v = getNumericAttrValue(dit, i);
				if (Double.isNaN(v)) {
					symbol.setSegmentPart(i, 0.0f);
				} else {
					allNaNs = false;
					v = getSizeForValue(v);
					symbol.setSegmentPart(i, (float) v);
				}
			}
		return (allNaNs) ? null : symbol;
	}

	/**
	*  Function returns part in range (-1;-1) for specific attribute value.
	*  If value is out of focus interval, a special constant returned instead:
	*  -10f - if value is lies left to focusser and lower than reference;
	*  +5f  - if value is lies left to focusser and higher than reference;
	*  -5f  - if value is lies right to focusser and lower than reference;
	*  +10f - if value is lies right to focusser and higher than reference;
	*/
	protected float getSizeForValue(double value) {
		if (value == cmpValAbs)
			return 0f;
		//if (value<focuserMIN) return -10f;
		//if (value>focuserMAX) return +10f;
		if (value < focuserMIN)
			return ((value > cmpValAbs) ? +5f : -10f);
		if (focuserMAX < value)
			return ((value > cmpValAbs) ? +10f : -5f);

		double minv = MIN - cmpValAbs, maxv = MAX - cmpValAbs;
		value -= cmpValAbs;
		double maxMod = Math.abs(maxv);
		if (Math.abs(minv) > maxMod) {
			maxMod = Math.abs(minv);
		}
		double ystart = 0;
		if (minv > 0) {
			ystart = minv;
		} else if (maxv < 0) {
			ystart = -maxv;
		}
		float r = (float) ((Math.abs(value) - ystart) / (maxMod - ystart));
		return value > 0 ? r : -r;
	}

	/**
	* Draws a small picture symbolizing this presentation technique
	*/
	@Override
	public void drawIcon(Graphics g, int x, int y, int w, int h) {
		checkCreateSymbol();
		for (int i = 0; i < attr.size(); i++) {
			symbol.setSegmentPart(i, (1.0f + i) / attr.size());
		}
		int width = symbol.getMaxWidth(), height = symbol.getMaxHeight();
		symbol.setMaxSizes(w - 2, h - 2);
		symbol.draw(g, x, y + h - 1);
		symbol.setMaxSizes(width, height);
	}

	/**
	* Draws the part of the legend explaining this specific presentation method.
	*/
	@Override
	public Rectangle drawMethodSpecificLegend(Graphics g, int startY, int leftMarg, int prefW) {
		Color gray = Color.getHSBColor(0.0f, 0.0f, 0.7f), gray1 = Color.getHSBColor(0.0f, 0.0f, 0.85f);
		if (Double.isNaN(MIN)) {
			setGlobalMinMax();
		}

		checkCreateSymbol();
		int rSize = 4 * mm, gap = 2 * mm, arrowSize = 4 * mm, minHeight = symbol.getMinHeight(), maxHeight = symbol.getMaxHeight(), barWidth = symbol.getMaxWidth() / attr.size();
		int y = startY, maxX = leftMarg;
		int posX = leftMarg + gap / 2;
		int fh = g.getFontMetrics().getHeight();

		Point p = null;
		for (int i = 0; i < attr.size(); i++) {
			g.setColor(symbol.getSegmentColor(i));
			g.fillRect(posX, y, rSize, rSize);
			g.setColor(Color.black);
			p = StringInRectangle.drawText(g, getAttrName(i), posX + rSize + gap, y, prefW - rSize - gap, false);
			y = (p.y - startY > rSize) ? p.y : startY + rSize;
			if (maxX < p.x) {
				maxX = p.x;
			}
		}

		boolean focusL = (getDataMin() >= 0 ? (focuserMIN > 0) : (focuserMIN > getDataMin())), focusR = Math.abs(getDataMax() - focuserMAX) > 0.001 * Math.abs(focuserMAX);
		String sValue = "";

		double values[] = { (focusR ? getDataMax() : focuserMAX), focuserMAX, cmpValAbs, focuserMIN, (focusL ? getDataMin() : focuserMIN) };
		spade.lib.util.QSortAlgorithm.sort(values, true);
		y += gap;
		for (int i = 0; i < 5; i++) {
			float r = getSizeForValue(values[i]);
			posX = leftMarg + gap / 2;
			sValue = StringUtil.doubleToStr(values[i], getDataMin(), getDataMax());
			boolean fill = (Math.abs(r) > 0f && Math.abs(r) <= 1.0f);
			int barH = maxHeight;
			if (Math.abs(r) > 2) {
				barH = (Math.abs(r) > 9.9f ? maxHeight : minHeight);
			} else {
				barH = Math.round(barH * Math.abs(r));
			}

			if ((!focusR && (i == 0) && values[i] != cmpValAbs) || (!focusL && (i == 4))) {
				continue;
			}

			if (values[i] != cmpValAbs && values[i] != getDataMin() && values[i] != getDataMax() && barH < 1) {
				barH = minHeight;
				if (values[i] < cmpValAbs) {
					r -= 5;
				}
				if (values[i] > cmpValAbs) {
					r += 5;
				}
			}
			if (i > 0 && values[i] == cmpValAbs && values[i] == values[i - 1]) {
				continue;
			}
			if (focusL && values[i] == getDataMin() && values[i] > cmpValAbs) {
				continue;
			}

			Color c = null;
			if (attr.size() < 2) {
				c = getColorForAttribute(0);
				if (r < 0) {
					c = new Color(Color.HSBtoRGB(CS.getHue(c), CS.getSaturation(c) / 2f, CS.getBrightness(c)));
				}
			} else {
				c = (r > 0 ? gray : gray1);
			}
			g.setColor(c);
			if (fill) {
				g.fillRect(posX, y, barWidth, barH);
			}
			if (attr.size() > 1 || fill) {
				g.setColor(Color.black);
			}
			g.drawRect(posX, y, barWidth, barH);
			g.setColor(Color.black);
			g.drawLine(posX - gap / 2, (r >= 0 ? y + barH : y), posX + barWidth + gap / 2, (r >= 0 ? y + barH : y));
			posX += barWidth + gap;
			Drawing.drawHorizontalArrow(g, posX, (r >= 0 ? y : y + barH) - arrowSize / 8, arrowSize, arrowSize / 4, false, true);
			posX += arrowSize + gap;
			p = StringInRectangle.drawText(g, sValue, posX, (r >= 0 ? y : y + barH) - fh / 2, prefW - posX, false);
			if (p.y > (y + barH + gap)) {
				y = p.y + gap;
			} else {
				y += barH + gap;
			}
		}
		if (maxX < p.x) {
			maxX = p.x;
		}
		//y+=gap;
		y = p.y;
		return new Rectangle(leftMarg, startY, maxX - leftMarg, y - startY);
	}

	/**
	* A method from the AttrColorHandler interface.
	* Returns the color used to represent the specified attribute.
	* May return null.
	*/
	@Override
	public Color getColorForAttribute(String attrId) {
		int idx = getAttrIndex(attrId);
		if (idx >= 0)
			return getColorForAttribute(idx);
		return null;
	}

	@Override
	public Color getColorForAttribute(int idx) {
		if (symbol != null && idx >= 0 && idx < symbol.getNSegments())
			return symbol.getSegmentColor(idx);
		return super.getColorForAttribute(idx);
	}

	/**
	* A method from the AttrColorHandler interface.
	* Sets the color to be used to represent the specified attribute.
	*/
	@Override
	public void setColorForAttribute(Color color, String attrId) {
		setColorForAttribute(color, getAttrIndex(attrId));
	}

	@Override
	public void setColorForAttribute(Color color, int attrIdx) {
		if (attrIdx >= 0 && attrIdx < attr.size()) {
			if (symbol != null && !color.equals(symbol.getSegmentColor(attrIdx))) {
				symbol.setSegmentColor(attrIdx, color);
				notifyVisChange();
			}
			super.setColorForAttribute(color, attrIdx);
		}
	}

	/**
	* Sets the colors for the specified group of attributes.
	*/
	@Override
	public void setColorsForAttributes(Vector colors, Vector attrIds) {
		if (attrIds == null || colors == null || attrIds.size() < 1 || colors.size() < 1)
			return;
		int nChanged = 0;
		for (int i = 0; i < attrIds.size() && i < colors.size(); i++) {
			String attrId = (String) attrIds.elementAt(i);
			int idx = getAttrIndex(attrId);
			if (idx >= 0 && idx < attr.size()) {
				++nChanged;
				Color color = (Color) colors.elementAt(i);
				if (symbol != null && !color.equals(symbol.getSegmentColor(idx))) {
					symbol.setSegmentColor(idx, color);
				}
				super.setColorForAttribute(color, idx);
			}
		}
		if (nChanged > 0) {
			notifyVisChange();
		}
	}

	/**
	* Methods from the interface SignDrawer
	*/
	@Override
	public Sign getSignInstance() {
		checkCreateSymbol();
		BarChart bar = new BarChart();
		bar.setCmpMode(false);
		setupSymbol(bar);
		bar.setMinSizes(symbol.getMinWidth(), symbol.getMinHeight());
		bar.setMaxSizes(symbol.getMaxWidth(), symbol.getMaxHeight());
		bar.setMustDrawFrame(symbol.getMustDrawFrame());
		return bar;
	}

	/**
	* Through this method a SignDrawer is informed that some of the sign properties
	* have changed. The sign drawer must care about repainting the map.
	* The possible identifiers of sign properties are listed in the class
	* @see spade.vis.geometry.Sign
	*/
	@Override
	public void signPropertyChanged(int propertyId, Sign sgn) {
		if (sgn == null || symbol == null)
			return;
		if (propertyId == Sign.MAX_SIZE) {
			symbol.setMaxSizes(sgn.getMaxWidth(), sgn.getMaxHeight());
			notifyVisChange();
		}
	}

	@Override
	public synchronized void addPropertyChangeListener(PropertyChangeListener l) {
		if (colorHandler != null) {
			colorHandler.addPropertyChangeListener(l);
		}
	}

	@Override
	public synchronized void removePropertyChangeListener(PropertyChangeListener l) {
		if (colorHandler != null) {
			colorHandler.removePropertyChangeListener(l);
		}
	}

	/**
	* Must reply whether any parameters of this visualization method may be
	* changed, e.g. colors or sizes of signs. This does not include interactive
	* analytical manipulation.
	* Returns true.
	*/
	@Override
	public boolean canChangeParameters() {
		return true;
	}

	/**
	* Constructs and displays a dialog for changing parameters of this
	* visualization method: minimum and maximum sizes of the signs and
	* colors of sign elements.
	*/
	@Override
	public void startChangeParameters() {
		SignParamsController pc = new SignParamsController();
		pc.startChangeParameters(this);
	}

//ID
	@Override
	public double getCmp() {
		return cmpValAbs;
	}

	@Override
	public Hashtable getVisProperties() {
		Hashtable param;
		param = super.getVisProperties();
		if (param == null) {
			param = new Hashtable();
		}

		checkCreateSymbol();
		param.put("maxWidth", String.valueOf(symbol.getMaxWidth()));
		param.put("maxHeight", String.valueOf(symbol.getMaxHeight()));
		param.put("cmpTo", String.valueOf(cmpValAbs));

/*    if (symbol == null)
      param.put("colors", "null");
    else {*/
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < this.getAttributeColors().size()/*symbol.getNSegments()*/; i++) {
			sb.append(Integer.toHexString((getColorForAttribute(i)/*symbol.getSegmentColor(i))*/.getRGB())).substring(2));
			sb.append(" ");
		}
		param.put("colors", sb.toString());
//    }

		return param;
	}

	@Override
	public void setVisProperties(Hashtable param) {
		checkCreateSymbol();
		try {
			symbol.setMaxHeight(new Integer((String) param.get("maxHeight")).intValue());
		} catch (Exception ex) {
		}
		try {
			symbol.setMaxWidth(new Integer((String) param.get("maxWidth")).intValue());
		} catch (Exception ex) {
		}
		try {
			setCmp(new Double((String) param.get("cmpTo")).doubleValue());
			cmpTo = symbol.cmpValue;
		} catch (Exception ex) {
		}

		String sColors = (String) param.get("colors");
		if (sColors == null || sColors.length() < 1 || sColors.equals("null")) {
			symbol.setDefaultColors();
		} else {
			StringTokenizer st = new StringTokenizer(sColors, " ");
			int i = 0;
			while (st.hasMoreTokens()) {
				setColorForAttribute(new Color(Integer.parseInt(st.nextToken(), 16)), i);
				i++;
			}
		}

		super.setVisProperties(param);
	}
//~ID
}
