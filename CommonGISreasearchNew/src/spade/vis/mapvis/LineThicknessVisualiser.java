package spade.vis.mapvis;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Hashtable;

import spade.analysis.manipulation.Trapezoid;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.Drawing;
import spade.lib.basicwin.Metrics;
import spade.lib.basicwin.OKDialog;
import spade.vis.database.ThematicDataItem;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 29-Jan-2007
 * Time: 11:43:36
 * Visualises a numeric attribute associated with linear objects by line
 * thickness
 */
public class LineThicknessVisualiser extends NumberDrawer implements DefaultColorUser, SizeChecker {
	/**
	 * The default color (used, in particular, for drawing the icon).
	 * Think how to use the color of the layer!
	 */
	protected Color defaultColor = Color.red;
	/**
	 * The minimum and maximum line thickness, in pixels.
	 */
	protected int minThickness = 0, maxThickness = 30;
	/**
	 * The thicknesses for values which are out of the focus interval
	 */
	protected int lessThanMinThickness = 0, moreThanMaxThickness = 3;
	/**
	 * A single instance used in getPresentation, to avoid creation of
	 * many instances
	 */
	protected LineDrawSpec lds = new LineDrawSpec();

	/**
	 * Returns an instance of LineDrawSpec, which specifies how to represent the
	 * data from the given ThematicDataItem.
	 */
	@Override
	public Object getPresentation(ThematicDataItem dit) {
		//LineDrawSpec lds=new LineDrawSpec();
		lds.draw = false;
		lds.thickness = lessThanMinThickness;
		lds.color = Color.lightGray;
		lds.transparent = true;
		if (dit == null)
			return lds;
		if (attr == null || attr.size() < 1)
			return lds;
		if (sdController != null && !sdController.mustDrawObject(dit.getId()))
			return lds;
		lds.draw = true;
		if (Double.isNaN(getDataMin()))
			return lds;
		double value = getNumericAttrValue(dit, 0);
		if (Double.isNaN(value))
			return lds;
		if (value < focuserMin)
			//lds.draw=false;
			return lds;
		if (value > focuserMax) {
			lds.thickness = moreThanMaxThickness;
			return lds;
		}
		lds.transparent = false;
		lds.color = defaultColor;
		double r = (value - focuserMin) * (maxThickness - minThickness) / (focuserMax - focuserMin);
		lds.thickness = minThickness + (int) Math.round(r);
		return lds;
	}

	/**
	 * This method informs whether the Visualizer produces diagrams.
	 * This is important for defining the order of drawing of GeoLayers on the
	 * map: the diagrams should be drawn on top of all geography.
	 * Returns false.
	 */
	@Override
	public boolean isDiagramPresentation() {
		return false;
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
	* visualization method: minimum and maximum thickness of the lines.
	*/
	@Override
	public void startChangeParameters() {
		int sizes[] = { minThickness, maxThickness };
		String texts[] = { "Minimum thickness:", "Maximum thickness:" };
		SizeChanger sch = new SizeChanger(sizes, texts, 1, 50, this);
		OKDialog okd = new OKDialog(CManager.getAnyFrame(), "Line thickness", true);
		okd.addContent(sch);
		okd.show();
		if (okd.wasCancelled())
			return;
		sizes = sch.getIntSizes();
		minThickness = sizes[0];
		maxThickness = sizes[1];
		notifyVisChange();
	}

	public Color getDefaultColor() {
		return defaultColor;
	}

	public void setDefaultColor(Color defaultColor) {
		this.defaultColor = defaultColor;
	}

	public int getMinThickness() {
		return minThickness;
	}

	public void setMinThickness(int minThickness) {
		this.minThickness = minThickness;
	}

	public int getMaxThickness() {
		return maxThickness;
	}

	public void setMaxThickness(int maxThickness) {
		this.maxThickness = maxThickness;
	}

	/**
	* Draws the part of the legend that explains how numeric attribute values
	* are graphically encoded on the map, assuming that the name of the attribute
	* is already shown.
	*/
	@Override
	protected Rectangle showNumberEncoding(Graphics g, int startY, int leftMarg, int prefW) {
		int y = startY, maxX = 0;
		int totalW = prefW - leftMarg - 3, barH = 2 + maxThickness;
		int dx1 = 0, dx2 = 0;
		if (focuserMin > dataMin) {
			dx1 = totalW / 3;
		}
		if (focuserMax < dataMax) {
			dx2 = totalW / 3;
		}
		Trapezoid tr = new Trapezoid();
		tr.setIsHorizontal(true);
		tr.setColor(defaultColor);
		tr.setMinSize(minThickness);
		tr.setMaxSize(maxThickness);
		Rectangle bounds = new Rectangle(leftMarg + dx1, startY, totalW - dx1 - dx2, barH);
		tr.setBounds(bounds);
		int my = bounds.y + bounds.height / 2;
		if (focuserMin > dataMin) {
			g.setColor(Color.lightGray);
			Drawing.drawLine(g, lessThanMinThickness, leftMarg, my, bounds.x, my, true, false);
		}
		if (focuserMax < dataMax) {
			g.setColor(Color.lightGray);
			Drawing.drawLine(g, moreThanMaxThickness, bounds.x + bounds.width, my, leftMarg + totalW, my, true, false);
		}
		tr.draw(g);
		if (leftMarg + totalW > maxX) {
			maxX = leftMarg + totalW;
		}
		int by = y + barH;

		FontMetrics fm = g.getFontMetrics();
		int fh = fm.getHeight(), asc = fm.getAscent();
		y = by + Metrics.mm() / 2;
		g.setColor(Color.black);

		int x0 = leftMarg;
		if (focuserMin > dataMin) {
			String str = getValueAsString(dataMin);
			g.drawString(str, x0, y + asc);
			g.drawLine(x0, my, x0, y);
			y += fh;
			int rx = x0 + fm.stringWidth(str);
			if (maxX < rx) {
				maxX = rx;
			}
		}
		x0 = bounds.x;
		String str = getFocuserMinAsString();
		g.drawString(str, x0, y + asc);
		g.drawLine(x0, my, x0, y);
		y += fh;
		int rx = x0 + fm.stringWidth(str);
		if (maxX < rx) {
			maxX = rx;
		}
		x0 += bounds.width;
		g.drawLine(x0, by, x0, y);
		str = getFocuserMaxAsString();
		int w = fm.stringWidth(str);
		x0 -= w;
		if (x0 < leftMarg) {
			x0 = leftMarg;
		}
		g.drawString(str, x0, y + asc);
		y += fh;
		rx = x0 + w;
		if (maxX < rx) {
			maxX = rx;
		}
		if (focuserMax < dataMax) {
			x0 = bounds.x + bounds.width + dx2;
			g.drawLine(x0, my, x0, y);
			str = getValueAsString(dataMax);
			w = fm.stringWidth(str);
			x0 -= w;
			if (x0 < leftMarg) {
				x0 = leftMarg;
			}
			g.drawString(str, x0, y + asc);
			y += fh;
			rx = x0 + w;
			if (maxX < rx) {
				maxX = rx;
			}
		}
		y += Metrics.mm();
		return new Rectangle(leftMarg, startY, maxX - leftMarg, y - startY);
	}

	/**
	* Draws a small picture symbolizing this presentation technique
	*/
	@Override
	public void drawIcon(Graphics g, int x, int y, int w, int h) {
		int th = minThickness, dx = (w - 3 * th - 9) / 3;
		if (dx < 2) {
			dx = 2;
		}
		g.setColor(defaultColor);
		Rectangle r = g.getClipBounds();
		g.setClip(x, y, w, h);
		Drawing.drawLine(g, th, x, y, x + dx, y + h, true, false);
		x += dx + th;
		th += 3;
		Drawing.drawLine(g, th, x, y, x + dx, y + h, true, false);
		x += dx + th;
		th += 3;
		Drawing.drawLine(g, th, x, y, x + dx, y + h, true, false);
		if (r != null) {
			g.setClip(r.x, r.y, r.width, r.height);
		} else {
			g.setClip(null);
		}
	}

	@Override
	public Hashtable getVisProperties() {
		Hashtable param = new Hashtable();
		param.put("minThickness", String.valueOf(minThickness));
		param.put("maxThickness", String.valueOf(maxThickness));
		param.put("focuserMin", String.valueOf(focuserMin));
		param.put("focuserMax", String.valueOf(focuserMax));
		if (defaultColor != null) {
			param.put("defaultColor", Integer.toHexString(defaultColor.getRGB()).substring(2));
		}
		return param;
	}

	@Override
	public void setVisProperties(Hashtable param) {
		String str = (String) param.get("focuserMin");
		if (str != null) {
			try {
				float temp = Float.valueOf(str).floatValue();
				if (!Float.isNaN(temp)) {
					focuserMin = temp;
				}
			} catch (Exception ex) {
			}
		}
		str = (String) param.get("focuserMax");
		if (str != null) {
			try {
				float temp = Float.valueOf(str).floatValue();
				if (!Float.isNaN(temp)) {
					focuserMax = temp;
				}
			} catch (Exception ex) {
			}
		}
		str = (String) param.get("minThickness");
		if (str != null) {
			try {
				int k = Integer.parseInt(str);
				if (k > 0) {
					minThickness = k;
				}
			} catch (Exception ex) {
			}
		}
		str = (String) param.get("maxThickness");
		if (str != null) {
			try {
				int k = Integer.parseInt(str);
				if (k > 0) {
					maxThickness = k;
				}
			} catch (Exception ex) {
			}
		}
		str = (String) param.get("defaultColor");
		if (str != null) {
			try {
				int k = Integer.parseInt(str, 16);
				defaultColor = new Color(k);
			} catch (Exception ex) {
			}
		}
	}

	/**
	 * DefaultColorUser interface: sets the default line color
	 */
	@Override
	public void setDefaultLineColor(Color lineColor) {
		if (lineColor != null) {
			defaultColor = lineColor;
		}
		notifyVisChange();
	}

	/**
	 * DefaultColorUser interface: should set the default fill color.
	 * In this case, the fill color is ignored.
	 */
	@Override
	public void setDefaultFillColor(Color fillColor) {
	}

	/**
	 * SizeChecker interface:
	 * Checks the correctness of sizes, e.g. that the maximum size is greater than
	 * the minimum size.
	 * The sizes are specified in the agrument array.
	 * Returns a message explaining what is wrong with the sizes. If everything is
	 * right, returns null.
	 */
	@Override
	public String checkSizes(float sizes[]) {
		if (sizes == null)
			return "No sizes specified!";
		if (sizes.length < 2)
			return "2 sizes are expected!";
		if (Float.isNaN(sizes[0]))
			return "The minimum size is not specified!";
		if (Float.isNaN(sizes[1]))
			return "The maximum size is not specified!";
		if (sizes[0] >= sizes[1])
			return "The minimum size must be less than the maximum size!";
		return null;
	}
}
