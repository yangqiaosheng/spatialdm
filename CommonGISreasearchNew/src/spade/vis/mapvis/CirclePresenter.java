package spade.vis.mapvis;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Hashtable;

import spade.lib.basicwin.Drawing;
import spade.lib.basicwin.StringInRectangle;
import spade.lib.util.DoubleArray;
import spade.vis.database.ThematicDataItem;
import spade.vis.geometry.Circle;
import spade.vis.geometry.Sign;

/**
* a class to visualize value of a single numeric attribut by sign "circle"
*/
public class CirclePresenter extends NumberDrawer implements SignDrawer {
	public static final int mm = Math.round(java.awt.Toolkit.getDefaultToolkit().getScreenResolution() / 25.33f);

	protected Circle cir = null;
	public int absMinD = 0, // absMinD - if no limit for min diameter
			minD = 2 * mm, maxD = 10 * mm; // minD is a minimal, maxD is a maximal diameter
	/**
	*  Vis. option: the minimal diameter of circle is used for min attr.value
	*/
//ID
	public boolean useMinSize = false;

//~ID
	/**
	* Constructs an instance of Cirle for further use.
	*/
	protected void checkCreateSign() {
		if (cir != null)
			return;
		cir = new Circle();
		cir.setUsesMinSize(useMinSize);
		cir.setMinSizes(minD, minD);
		cir.setMaxSizes(maxD, maxD);
		posHue = 0.95f;
		negHue = 0.55f;
	}

	/**
	* returns a color, diagram, or icon representing data from ThematicDataItem
	*/

	@Override
	public Object getPresentation(ThematicDataItem dit) {
		if (dit == null || attr == null || attr.size() < 1)
			return null;
		if (Double.isNaN(getDataMin()))
			return null;
		if (sdController != null && !sdController.mustDrawObject(dit.getId()))
			return null;
		double fval = getNumericAttrValue(dit, 0);
		if (Double.isNaN(fval))
			return null;
		return getCircleForValue(fval);
	}

	protected Circle getCircleForValue(double fval) {
		checkCreateSign();
		Color c = (fval < cmpTo) ? Color.getHSBColor(negHue, 1.0f, 1.0f) : ((Math.abs(fval - cmpTo) < epsilon) ? Color.white : Color.getHSBColor(posHue, 1.0f, 1.0f));
		cir.setColor(c);
		cir.setBorderColor(c.darker());
		int h;
		int minH = useMinSize ? cir.getMinHeight() : absMinD, maxH = cir.getMaxHeight();

		cir.fill = true;

		if (fval - focuserMax > 0) {
			cir.fill = false;
			fval = focuserMax;
		}
		if (fval - focuserMin < 0) {
			cir.fill = false;
			fval = focuserMin;
		}

		double newMax = getDataMax() - focuserMax > 0 ? focuserMax : getDataMax();
		double newMin = getDataMin() - focuserMin < 0 ? focuserMin : getDataMin();
		fval -= cmpTo;
		newMax -= cmpTo;
		newMin -= cmpTo;
		double absMax = Math.abs(newMax), absMin = (newMin < 0) ? 0 : newMin;
		if (Math.abs(newMin) > absMax) {
			absMax = Math.abs(newMin);
		}

		if (useMinSize) {
			float f = (float) Math.sqrt((Math.abs(fval) - absMin) / (absMax - absMin));
			h = minH + Math.round(f * (maxH - minH));
		} else {
			float f = (float) Math.sqrt(Math.abs(fval) / absMax);
			h = Math.round(f * maxH);
		}
		cir.setSizes(h, h);

		return cir;
	}

	/**
	* isDiagramPresentation returns true because it produces diagrams or icons;
	*/
	@Override
	public boolean isDiagramPresentation() {
		return true;
	}

	/**
	* Draws the part of the legend that explains how numeric attribute values
	* are graphically encoded on the map, assuming that the name of the attribute
	* is already shown.
	*/
	@Override
	protected Rectangle showNumberEncoding(Graphics g, int startY, int leftMarg, int prefW) {
		checkCreateSign();
		int gap = 2 * mm, arrowSize = 4 * mm, minSize = (useMinSize ? cir.getMinWidth() : 0), maxSize = cir.getMaxWidth(), y = startY, maxX = leftMarg, fontHeight = g.getFontMetrics().getHeight();
		Point p = StringInRectangle.drawText(g, res.getString("Circle_area_is"), leftMarg + gap / 2, y, prefW - gap, false);
		y = p.y + gap / 2;
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
			Circle cSample = getCircleForValue(fval);
			int diam = cSample.getHeight();
			if (useMinSize && diam < minSize) {
				diam = minSize;
			}
			int xpos = leftMarg + gap + maxSize / 2 - diam / 2;
			xpos = leftMarg + gap + maxSize / 2 - diam / 2;
			g.setColor(cSample.getColor());
			if (fval >= focuserMin && fval <= focuserMax) {
				if (fval == cmpTo) {
					g.setColor(Color.white);
				}
				g.fillOval(xpos - 1, y - 1, diam + 2, diam + 2);
			}
			if (fval == cmpTo) {
				g.setColor(Color.black);
			}
			g.drawOval(xpos, y, diam, diam);
			xpos += (diam / 2 + maxSize / 2 + gap);
			y += diam / 2;
			g.setColor(Color.black);
			Drawing.drawHorizontalArrow(g, xpos, y, arrowSize, arrowSize / 4, false, true);
			xpos += arrowSize + gap;
			y -= fontHeight / 2;
			String sValue = getValueAsString(fval);
			p = StringInRectangle.drawText(g, sValue, xpos, y, prefW - gap - xpos, false);
			if (diam > fontHeight) {
				y = p.y + (diam / 2 - fontHeight / 2 + gap);
			} else {
				y = p.y + fontHeight / 2 + gap;
			}
			xpos = leftMarg;
		}
		return new Rectangle(leftMarg, startY, maxX - leftMarg, y - startY);
	}

	/**
	* Draws a small picture symbolizing this presentation technique
	*/
	@Override
	public void drawIcon(Graphics g, int x, int y, int w, int h) {
		checkCreateSign();
		int width = cir.getMaxWidth(), height = cir.getMaxHeight();
		cir.setMaxSizes(w - 2, h - 2);
		cir.setSizes(w - 2, h - 2);
		Color c = Color.getHSBColor(posHue, 1.0f, 1.0f);
		cir.setColor(c);
		cir.setBorderColor(c.darker());
		cir.draw(g, x, y, w, h);
		cir.setMaxSizes(width, height);
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
		int minSignSize = useMinSize ? cir.getMinWidth() : absMinD;
		Circle circle = new Circle();
		circle.setMayChangeProperty(Sign.MAX_SIZE, true);
		circle.setMayChangeProperty(Sign.MIN_SIZE, useMinSize);
		circle.setMayChangeProperty(Sign.SIZE, false);
		circle.setMayChangeProperty(Sign.COLOR, false);
		circle.setMayChangeProperty(Sign.BORDER_COLOR, false);
		circle.setUsesMinSize(useMinSize);
		circle.setSizes(cir.getDiameter(), cir.getDiameter());
		circle.setMinSizes(minSignSize, minSignSize);
		circle.setMaxSizes(cir.getMaxWidth(), cir.getMaxHeight());
		circle.setColor(cir.getColor());
		circle.setBorderColor(cir.getColor());
		return circle;
	}

	/**
	* Through this method a SignDrawer is informed that some of the sign properties
	* have changed. The sign drawer must care about repainting the map.
	* The possible identifiers of sign properties are listed in the class
	* @see spade.vis.geometry.Sign
	*/
	@Override
	public void signPropertyChanged(int propertyId, Sign sgn) {
		if (sgn == null || cir == null)
			return;
		switch (propertyId) {
		case Sign.MAX_SIZE:
			cir.setMaxSizes(sgn.getMaxWidth(), sgn.getMaxHeight());
			break;
		case Sign.MIN_SIZE:
			cir.setMinSizes(sgn.getMinWidth(), sgn.getMinHeight());
			break;
		default:
			return;
		}
		notifyVisChange();
	}

	public void setUseMinSize(boolean useMinSize) {
		this.useMinSize = useMinSize;
		if (cir == null) {
			checkCreateSign();
		} else {
			cir.setUsesMinSize(useMinSize);
		}
		notifyVisChange();
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
//    param.put("absMinD", String.valueOf(absMinD));
		param.put("minD", String.valueOf(cir.getMinHeight()));
		param.put("maxD", String.valueOf(cir.getMaxHeight()));
		param.put("useMinSize", String.valueOf(useMinSize));

		return param;
	}

	@Override
	public void setVisProperties(Hashtable param) {
//    try {
//      absMinD = new Integer((String)param.get("absMinD")).intValue();
//    } catch (Exception ex) {}
		try {
			minD = new Integer((String) param.get("minD")).intValue();
		} catch (Exception ex) {
		}
		try {
			maxD = new Integer((String) param.get("maxD")).intValue();
		} catch (Exception ex) {
		}
		try {
			useMinSize = new Boolean((String) param.get("useMinSize")).booleanValue();
		} catch (Exception ex) {
		}
		cir = null;
		checkCreateSign();

		super.setVisProperties(param);
	}
//~ID
}
