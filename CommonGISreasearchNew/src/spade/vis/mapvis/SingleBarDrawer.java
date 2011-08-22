package spade.vis.mapvis;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Hashtable;

import spade.lib.util.DoubleArray;
import spade.vis.database.ThematicDataItem;
import spade.vis.geometry.BarChart;
import spade.vis.geometry.Sign;

/**
* Implements visualization of a single numeric attribute by standalone bars.
* In fact, differs from its base class MultiBarDrawer only in the method
* isApplicable(attrNumber,attrTypes). Necessary for DataMapper.
*/

public class SingleBarDrawer extends NumberDrawer implements SignDrawer {
	public static final int mm = Math.round(java.awt.Toolkit.getDefaultToolkit().getScreenResolution() / 25.33f);
	/**
	* An instance of a BarChart
	*/
	protected BarChart sign = null;

	/**
	* Constructs an instance of BarChart for further use depending on the
	* required diagram type
	*/
	protected void checkCreateSign() {
		if (sign == null) {
			sign = new BarChart();
			setupSign(sign);
			sign.setMaxWidth(Sign.mm * 2);
			sign.setCmpMode(true);
			sign.cmpValue = 0f;
			sign.mayModifyShades = false;
		}
	}

	protected void setupSign(BarChart sgn) {
		sgn.setMayChangeProperty(Sign.USE_FRAME, false);
		sgn.setNSegments(1);
		sgn.setSegmentPart(0, 1.0f);
		sgn.setSegmentWeight(0, 1.0f);
		sgn.setSegmentColor(0, getColor(focuserMax, cmpTo));
	}

	/**
	* returns a bar chart representing data from ThematicDataItem
	*/
	@Override
	public Object getPresentation(ThematicDataItem dit) {
		if (dit == null)
			return null;
		if (attr == null || attr.size() < 1)
			return null;
		if (Double.isNaN(getDataMin()))
			return null;
		if (sdController != null && !sdController.mustDrawObject(dit.getId()))
			return null;
		double fval = getNumericAttrValue(dit, 0);
		if (Double.isNaN(fval))
			return null;
		return getChartForValue(fval);
	}

	protected BarChart getChartForValue(Double fval) {
		checkCreateSign();
		sign.cmpValue = 0f;
		if (!Double.isNaN(fval)) {
			Color c = (fval < cmpTo) ? Color.getHSBColor(negHue, 1.0f, 1.0f) : ((Math.abs(fval - cmpTo) < epsilon) ? Color.white : Color.getHSBColor(posHue, 1.0f, 1.0f));
			sign.setSegmentColor(0, c);
		} else {
			sign.setSegmentColor(0, Color.gray);
		}
		sign.setSegmentPart(0, (Double.isNaN(fval)) ? 0.0f : getSizeForValue(fval));
		return sign;
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
		if (value == cmpTo)
			return 0f;
		if (value < focuserMin)
			return ((value > cmpTo) ? +5f : -10f);
		if (focuserMax < value)
			return ((value > cmpTo) ? +10f : -5f);

		double minv = focuserMin - cmpTo, maxv = focuserMax - cmpTo;
		value -= cmpTo;
		double maxMod = Math.abs(maxv);
		if (Math.abs(minv) > maxMod) {
			maxMod = Math.abs(minv);
		}
		float r = (float) (Math.abs(value) / maxMod);
		return value > 0 ? r : -r;
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
	* visualization method: minimum and maximum sizes of the signs.
	*/
	@Override
	public void startChangeParameters() {
		SignParamsController pc = new SignParamsController();
		pc.startChangeParameters(this);
	}

	/**
	* Methods from the interface SignDrawer
	*/
	@Override
	public Sign getSignInstance() {
		checkCreateSign();
		BarChart bar = new BarChart();
		bar.setCmpMode(false);
		setupSign(bar);
		bar.setMinSizes(sign.getMinWidth(), sign.getMinHeight());
		bar.setMaxSizes(sign.getMaxWidth(), sign.getMaxHeight());
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
		if (sgn == null || sign == null)
			return;
		if (propertyId == Sign.MAX_SIZE) {
			sign.setMaxSizes(sgn.getMaxWidth(), sgn.getMaxHeight());
			notifyVisChange();
		}
		notifyVisChange();
	}

	/**
	* Draws the part of the legend that explains how numeric attribute values
	* are graphically encoded on the map, assuming that the name of the attribute
	* is already shown.
	*/
	@Override
	protected Rectangle showNumberEncoding(Graphics g, int startY, int leftMarg, int prefW) {
		checkCreateSign();
		FontMetrics fm = g.getFontMetrics();
		int gap = 2 * mm, maxSize = sign.getMaxHeight(), y = startY, maxX = leftMarg, fontHeight = fm.getHeight(), asc = fm.getAscent();
		boolean cmp = cmpTo >= getDataMin() && cmpTo <= getDataMax();
		double epsilon = 0.0001 * Math.abs(getDataMax() - getDataMin());
		DoubleArray values = new DoubleArray(5, 1);
		values.addElement(dataMin);
		if (focuserMin > dataMin) {
			values.addElement(focuserMin);
		}
		if (cmp) {
			values.addElement(cmpTo);
			if (cmpTo < focuserMin) {
				values.addElement(focuserMin - 2 * epsilon);
			}
			if (cmpTo > focuserMax) {
				values.addElement(focuserMax + 2 * epsilon);
			}
		}
		if (focuserMax < dataMax) {
			values.addElement(focuserMax);
		}
		values.addElement(dataMax);
		for (int i = 0; i < values.size() - 1; i++)
			if (values.elementAt(i) > values.elementAt(i + 1)) {
				int k = i;
				for (int j = i - 1; j >= 0 && values.elementAt(j) > values.elementAt(i + 1); j--) {
					k = j;
				}
				double f = values.elementAt(i + 1);
				values.removeElementAt(i + 1);
				values.insertElementAt(f, k);
			}
		g.setColor(Color.black);
		double prevVal = Double.NaN;
		for (int j = values.size() - 1; j >= 0; j--) { // lower values are lower in the legend
			double fval = values.elementAt(j);
			if (!Double.isNaN(prevVal) && Math.abs(fval - prevVal) < epsilon) {
				continue;
			}
			prevVal = fval;
			String sValue = getValueAsString(fval);
			BarChart bc = getChartForValue(fval);
			int height = bc.getMaxHeight();
			float part = Math.abs(getSizeForValue(fval));
			if (part < 1f) {
				height = Math.round(part * height);
			}
			if (part > 2f && part < 6f) {
				height = bc.getMinHeight();
			}
			if (height < fontHeight) {
				height = fontHeight;
			}
			int ypos = (fval >= cmpTo) ? y + height : y;
			bc.draw(g, leftMarg + gap, ypos);
			int xpos = leftMarg + 2 * gap + bc.getMaxWidth();
			if (fval < cmpTo) {
				ypos += asc;
			}
			g.drawString(sValue, xpos, ypos);
			int x = xpos + fm.stringWidth(sValue);
			if (maxX < x) {
				maxX = x;
			}
			y += height + gap;
		}
		y += gap;
		return new Rectangle(leftMarg, startY, maxX - leftMarg, y - startY);
	}

	/**
	* Draws a small picture symbolizing this presentation technique
	*/
	@Override
	public void drawIcon(Graphics g, int x, int y, int w, int h) {
		checkCreateSign();
		sign.setSegmentPart(0, 1.0f);
		int width = sign.getMaxWidth(), height = sign.getMaxHeight();
		sign.setMaxSizes((width > w - 2) ? w - 2 : width, h - 2);
		sign.draw(g, x, y, w, h);
		sign.setMaxSizes(width, height);
	}

//ID
	@Override
	public Hashtable getVisProperties() {
		Hashtable param;
		param = super.getVisProperties();
		if (param == null) {
			param = new Hashtable();
		}

		checkCreateSign();
		param.put("maxWidth", String.valueOf(sign.getMaxWidth()));
		param.put("maxHeight", String.valueOf(sign.getMaxHeight()));

		return param;
	}

	@Override
	public void setVisProperties(Hashtable param) {
		checkCreateSign();
		try {
			sign.setMaxHeight(new Integer((String) param.get("maxHeight")).intValue());
		} catch (Exception ex) {
		}
		try {
			sign.setMaxWidth(new Integer((String) param.get("maxWidth")).intValue());
		} catch (Exception ex) {
		}

		super.setVisProperties(param);
	}
//~ID
}
