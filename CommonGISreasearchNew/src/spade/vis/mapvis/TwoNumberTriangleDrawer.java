package spade.vis.mapvis;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Hashtable;
import java.util.Vector;

import spade.lib.basicwin.Drawing;
import spade.lib.basicwin.StringInRectangle;
import spade.lib.util.StringUtil;
import spade.vis.database.ThematicDataItem;
import spade.vis.geometry.Sign;
import spade.vis.geometry.Triangle;

/**
* Represents values of two numeric attributes by triangles
*/
public class TwoNumberTriangleDrawer extends MultiNumberDrawer implements SignDrawer {
	public static final int mm = Math.round(java.awt.Toolkit.getDefaultToolkit().getScreenResolution() / 25.33f);
	protected Triangle tr = null;

	/**
	* Constructs an instance of Triangle for further use.
	*/
	protected void checkCreateSign() {
		if (tr != null)
			return;
		tr = new Triangle();
		tr.isPositive = true;
		tr.setMinSizes(Math.round(mm * 0.5f), Math.round(mm * 0.5f));
		tr.setMaxSizes(12 * mm, 15 * mm);
	}

	/**
	* Checks if this visualization method is applicable to the given set of
	* attributes. May use the DataInformer to check types and values
	* of the attributes.
	*/
	@Override
	public boolean isApplicable(Vector attr) {
		minAttrNumber = maxAttrNumber = 2;
		return super.isApplicable(attr);
	}

	/**
	* Must check semantic applicability of this visualization method to the
	* attributes previously set in the visualizer. Here there is no need in this
	* because the relationship between the attributes is not relevant.
	* Returns true.
	*/
	@Override
	public boolean checkSemantics() {
		return true;
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
	* This method is called by a GeoObject. The GeoObject sends its DataItem
	* to the Visualizer, and the Visualizer returns an Object specifying the
	* presentation of data contained in this DataItem. This may be, in
	* particular, Color or Diagram. Typically the argument DataItem is
	* a ThematicDataItem, although in some cases (e.g. manual classification)
	* this is not required.
	*/
	@Override
	public Object getPresentation(ThematicDataItem item) {
		if (item == null)
			return null;
		if (attr == null || attr.size() < 2)
			return null;
		if (Double.isNaN(getDataMin(0)) || Double.isNaN(getDataMin(1)))
			return null;
		ThematicDataItem dit = item;
		if (sdController != null && !sdController.mustDrawObject(dit.getId()))
			return null;
		double v1 = getNumericAttrValue(dit, 0);
		if (Double.isNaN(v1))
			return null;
		double v2 = getNumericAttrValue(dit, 1);
		if (Double.isNaN(v2))
			return null;
		checkCreateSign();
		int minW = tr.getMinWidth(), maxW = tr.getMaxWidth(), maxDW = maxW - minW, minH = tr.getMinHeight(), maxH = tr.getMaxHeight(), maxDH = maxH - minH;
		int w = minW + (int) Math.round((v2 - getDataMin(1)) * maxDW / (getDataMax(1) - getDataMin(1)));
		int h = minH + (int) Math.round((v1 - getDataMin(0)) * maxDH / (getDataMax(0) - getDataMin(0)));
		tr.setSizes(w, h);
		return tr;
	}

	/**
	* Draws a small picture symbolizing this presentation technique
	*/
	@Override
	public void drawIcon(Graphics g, int x, int y, int w, int h) {
		checkCreateSign();
		int wt = w / 3 - 4, ht = h - 4;
		if (wt >= Sign.mm) {
			tr.setSizes(wt, ht);
			tr.draw(g, x + 2 + wt / 2, y + 2 + ht);
			x += wt + 4;
			wt = 2 * w / 3 - 2;
			ht = h * 3 / 4 - 4;
			tr.setSizes(wt, ht);
			tr.draw(g, x + wt / 2, y + 2 + ht + (h - ht) / 2);
		} else {
			wt = w / 2;
			tr.setSizes(wt, ht);
			tr.draw(g, x + w / 2, y + 2 + ht);
		}
	}

	/**
	* Draws the part of the legend explaining this specific presentation method.
	*/
	@Override
	public Rectangle drawMethodSpecificLegend(Graphics g, int startY, int leftMarg, int prefW) {
		checkCreateSign();
		int arrowSize = 4 * mm, gap = 2 * mm;
		g.setColor(Color.black);
		Drawing.drawVerticalArrow(g, leftMarg, startY + 1, arrowSize, arrowSize - 2, true, true);
		Point p = StringInRectangle.drawText(g, getAttrName(0), leftMarg + arrowSize + mm, startY, prefW - arrowSize, false);
		int y = (p.y - startY > arrowSize) ? p.y : startY + arrowSize, maxX = p.x;
		Drawing.drawHorizontalArrow(g, leftMarg + 1, y, arrowSize - 2, arrowSize, true, true);
		p = StringInRectangle.drawText(g, getAttrName(1), leftMarg + arrowSize + mm, y, prefW - arrowSize, false);
		y = (p.y - y > arrowSize) ? p.y : y + arrowSize;
		if (maxX < p.x) {
			maxX = p.x;
		}
		int minW = tr.getMinWidth(), maxW = tr.getMaxWidth(), minH = tr.getMinHeight(), maxH = tr.getMaxHeight();
		tr.setSizes(minW, maxH);
		int x = leftMarg;
		tr.draw(g, x + minW / 2, y + maxH);
		x += minW + gap;
		tr.setSizes(maxW, maxH);
		tr.draw(g, x + maxW / 2, y + maxH);
		x += maxW + mm;
		FontMetrics fm = g.getFontMetrics();
		int asc = fm.getAscent();
		String str = StringUtil.doubleToStr(getDataMax(0), getDataMin(0), getDataMax(0));
		g.setColor(Color.black);
		g.drawString(str, x, y + asc);
		int w = fm.stringWidth(str);
		if (x + w > maxX) {
			maxX = x + w;
		}
		y += maxH + gap;
		x = leftMarg;
		tr.setSizes(minW, minH);
		tr.draw(g, x + minW / 2, y + minH);
		x += minW + gap;
		tr.setSizes(maxW, minH);
		tr.draw(g, x + maxW / 2, y + minH);
		x += maxW + mm;
		str = StringUtil.doubleToStr(getDataMin(0), getDataMin(0), getDataMax(0));
		g.setColor(Color.black);
		g.drawString(str, x, y + minH);
		w = fm.stringWidth(str);
		if (x + w > maxX) {
			maxX = x + w;
		}
		y += minH + mm;
		x = leftMarg + minW + gap + maxW / 2;
		g.setColor(Color.darkGray);
		g.drawLine(x, y, x, y + asc / 2);
		g.drawLine(x, y + asc / 2, x + 3, y + asc / 2);
		x += 4;
		str = StringUtil.doubleToStr(getDataMax(1), getDataMin(1), getDataMax(1));
		g.setColor(Color.black);
		g.drawString(str, x, y + asc);
		w = fm.stringWidth(str);
		if (x + w > maxX) {
			maxX = x + w;
		}
		y += fm.getHeight();
		x = leftMarg + minW / 2;
		g.setColor(Color.darkGray);
		g.drawLine(x, y, x, y + asc / 2);
		g.drawLine(x, y + asc / 2, x + 3, y + asc / 2);
		x += 4;
		str = StringUtil.doubleToStr(getDataMin(1), getDataMin(1), getDataMax(1));
		g.drawString(str, x, y + asc);
		w = fm.stringWidth(str);
		if (x + w > maxX) {
			maxX = x + w;
		}
		y += fm.getHeight();
		return new Rectangle(leftMarg, startY, maxX - leftMarg, y - startY);
	}

	/**
	* Methods from the interface SignDrawer
	*/
	@Override
	public Sign getSignInstance() {
		checkCreateSign();
		Triangle triangle = new Triangle();
		triangle.isPositive = true;
		triangle.setMinSizes(tr.getMinWidth(), tr.getMinHeight());
		triangle.setMaxSizes(tr.getMaxWidth(), tr.getMaxHeight());
		triangle.setColor(tr.getColor());
		triangle.setBorderColor(tr.getColor());
		return triangle;
	}

	/**
	* Through this method a SignDrawer is informed that some of the sign properties
	* have changed. The sign drawer must care about repainting the map.
	* The possible identifiers of sign properties are listed in the class
	* @see spade.vis.geometry.Sign
	*/
	@Override
	public void signPropertyChanged(int propertyId, Sign sgn) {
		if (sgn == null || tr == null)
			return;
		switch (propertyId) {
		case Sign.MAX_SIZE:
			tr.setMaxSizes(sgn.getMaxWidth(), sgn.getMaxHeight());
			break;
		case Sign.MIN_SIZE:
			tr.setMinSizes(sgn.getMinWidth(), sgn.getMinHeight());
			break;
		case Sign.COLOR:
			tr.setColor(sgn.getColor());
			tr.setBorderColor(sgn.getColor());
			break;
		default:
			return;
		}
		notifyVisChange();
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

//ID
	@Override
	public Hashtable getVisProperties() {
		Hashtable param = null;
//    param = super.getVisProperties();
		if (param == null) {
			param = new Hashtable();
		}

		param.put("maxWidth", String.valueOf(tr.getMaxWidth()));
		param.put("maxHeight", String.valueOf(tr.getMaxHeight()));
		param.put("minWidth", String.valueOf(tr.getMinWidth()));
		param.put("minHeight", String.valueOf(tr.getMinHeight()));
		param.put("color", Integer.toHexString(tr.getColor().getRGB()).substring(2));

		return param;
	}

	@Override
	public void setVisProperties(Hashtable param) {
		try {
			tr.setMaxHeight(new Integer((String) param.get("maxHeight")).intValue());
		} catch (Exception ex) {
		}
		try {
			tr.setMaxWidth(new Integer((String) param.get("maxWidth")).intValue());
		} catch (Exception ex) {
		}
		try {
			tr.setMinHeight(new Integer((String) param.get("minHeight")).intValue());
		} catch (Exception ex) {
		}
		try {
			tr.setMinWidth(new Integer((String) param.get("minWidth")).intValue());
		} catch (Exception ex) {
		}
		try {
			tr.setColor(new Color(Integer.parseInt((String) param.get("color"), 16)));
			tr.setBorderColor(new Color(Integer.parseInt((String) param.get("color"), 16)));
		} catch (Exception ex) {
		}

		super.setVisProperties(param);
	}
//~ID
}
