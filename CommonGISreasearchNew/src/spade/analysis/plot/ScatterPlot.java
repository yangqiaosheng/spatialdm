package spade.analysis.plot;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.util.Vector;

import spade.analysis.system.Supervisor;
import spade.lib.basicwin.Metrics;
import spade.lib.util.NumRange;
import spade.lib.util.StringUtil;
import spade.time.TimeMoment;
import spade.vis.action.ObjectEventHandler;

/**
* A class that represents values of two numeric attributes on a scatter plot.
* The data to represent are taken from an AttributeDataPortion.
*/

public class ScatterPlot extends Plot {

	/**
	* Used to generate unique identifiers of instances ScatterPlot
	*/
	protected static int nInstances = 0;

	/**
	* The numbers of the fields (attributes) to be represented.
	*/
	protected int fn1 = -1, fn2 = -1;

	/**
	* The minimum and maximum values in each of the two fields
	*/
	protected double min1 = Double.NaN, min2 = Double.NaN, max1 = Double.NaN, max2 = Double.NaN;
	/**
	 * If the first attribute is temporal, these fields contain the absolute
	 * minimum and maximum time moments for this attribute.
	 */
	protected TimeMoment minTime1 = null, maxTime1 = null;
	/**
	 * If the second attribute is temporal, these fields contain the absolute
	 * minimum and maximum time moments for this attribute.
	 */
	protected TimeMoment minTime2 = null, maxTime2 = null;

	/**
	* Constructs a ScatterPlot. The argument isIndependent shows whether
	* this plot is displayed separately and, hence, should be registered at the
	* supervisor as an event source or it is a part of some larger plot.
	* The variable allowSelection shows whether the plot should listen to
	* mouse events and transform them to object selection events.
	* Supervisor provides access of a plot to the Highlighter (common for
	* all data displays) and in this way links together all displays
	* The argument handler is a reference to the component the plot
	* should send object events to. In a case when the plot is displayed
	* independently, the ObjectEventHandler is the supervisor (the supervisor
	* implements this interface). Otherwise, the handler is the larger plot in
	* which this plot is included as a part.
	* The larger plot should implement the ObjectEventHandler interface.
	*/

	public ScatterPlot(boolean isIndependent, boolean allowSelection, Supervisor sup, ObjectEventHandler handler) {
		super(isIndependent, allowSelection, sup, handler);
	}

	public void setFieldNumbers(int n1, int n2) {
		fn1 = n1;
		fn2 = n2;
	}

	public void setMinMax(double minHor, double maxHor, double minVert, double maxVert) {
		min1 = minHor;
		max1 = maxHor;
		min2 = minVert;
		max2 = maxVert;
	}

	/**
	* Resets its internal data, including the array of screen objects.
	* Called when records are added to or removed from the table.
	*/
	@Override
	public void reset() {
		min1 = max1 = min2 = max2 = Double.NaN;
		if (dataTable == null || fn1 < 0 || fn2 < 0) {
			dots = null;
			return;
		}
		setup();
		redraw();
	}

	public void setup() {
		if (dataTable == null || fn1 < 0 || fn2 < 0)
			return;
		min1 = max1 = min2 = max2 = Double.NaN;
		minTime1 = maxTime1 = minTime2 = maxTime2 = null;
		if (dots == null || dots.length != dataTable.getDataItemCount()) {
			dots = new DotPlotObject[dataTable.getDataItemCount()];
			for (int i = 0; i < dots.length; i++) {
				dots[i] = new DotPlotObject();
			}
		}
		if (Double.isNaN(min1) || Double.isNaN(max1)) {
			NumRange nr = getAttrValueRange(dataTable.getAttributeId(fn1));
			if (nr != null) {
				min1 = nr.minValue;
				max1 = nr.maxValue;
			}
		}
		if (Double.isNaN(min2) || Double.isNaN(max2)) {
			NumRange nr = getAttrValueRange(dataTable.getAttributeId(fn2));
			if (nr != null) {
				min2 = nr.minValue;
				max2 = nr.maxValue;
			}
		}
		boolean temp1 = dataTable.isAttributeTemporal(fn1), temp2 = dataTable.isAttributeTemporal(fn2);
		if (temp1 || temp2) {
			for (int i = 0; i < dataTable.getDataItemCount(); i++) {
				if (temp1) {
					Object val = dataTable.getAttrValue(fn1, i);
					if (val == null || !(val instanceof TimeMoment)) {
						continue;
					}
					TimeMoment t = (TimeMoment) val;
					if (minTime1 == null || minTime1.compareTo(t) > 0) {
						minTime1 = t;
					}
					if (maxTime1 == null || maxTime1.compareTo(t) < 0) {
						maxTime1 = t;
					}
				}
				if (temp2) {
					Object val = dataTable.getAttrValue(fn2, i);
					if (val == null || !(val instanceof TimeMoment)) {
						continue;
					}
					TimeMoment t = (TimeMoment) val;
					if (minTime2 == null || minTime2.compareTo(t) > 0) {
						minTime2 = t;
					}
					if (maxTime2 == null || maxTime2.compareTo(t) < 0) {
						maxTime2 = t;
					}
				}
			}
			if (minTime1 != null) {
				minTime1 = minTime1.getCopy();
			}
			if (maxTime1 != null) {
				maxTime1 = maxTime1.getCopy();
			}
			if (minTime2 != null) {
				minTime2 = minTime2.getCopy();
			}
			if (maxTime2 != null) {
				maxTime2 = maxTime2.getCopy();
			}
		}
		for (int i = 0; i < dots.length; i++) {
			dots[i].reset();
			dots[i].id = dataTable.getDataItemId(i);
		}
		applyFilter();
		plotImageValid = bkgImageValid = false;
	}

	@Override
	public boolean reloadAttributeData(Vector v) {
		if (v == null)
			return false;
		for (int i = 0; i < v.size(); i++) {
			String AttrID = (String) v.elementAt(i);
			if (AttrID.equals(dataTable.getAttributeId(fn1)) || AttrID.equals(dataTable.getAttributeId(fn2))) {
				setup();
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean hasData() {
		return dots != null && !Double.isNaN(min1) && !Double.isNaN(min2);
	}

	public String numToString(double num, double min, double max, TimeMoment time) {
		if (time == null)
			return StringUtil.doubleToStr(num, min, max);
		return time.valueOf((long) num).toString();
	}

	public String numToStringHor(double num) {
		return numToString(num, min1, max1, minTime1);
	}

	public String numToStringVert(double num) {
		return numToString(num, min2, max2, minTime2);
	}

	@Override
	public void drawReferenceFrame(Graphics g) {
		FontMetrics fm = g.getFontMetrics();
		int fh = fm.getHeight(), asc = fm.getAscent();
		String strmin1 = numToString(min1, min1, max1, minTime1), strmin2 = numToString(min2, min2, max2, maxTime2), strmax1 = numToString(max1, min1, max1, minTime1), strmax2 = numToString(max2, min2, max2, maxTime2);
		int lmin2 = fm.stringWidth(strmin2), lmax1 = fm.stringWidth(strmax1), lmax2 = fm.stringWidth(strmax2), leftTextFieldW = (lmin2 > lmax2) ? lmin2 : lmax2;
		mx1 = minMarg + leftTextFieldW;
		mx2 = minMarg;
		my1 = minMarg;
		my2 = minMarg + asc;
		if (bounds.width - mx1 - mx2 < Metrics.mm() * 5 || bounds.height - my1 - my2 < Metrics.mm() * 5)
			return;
		width = bounds.width - mx1 - mx2;
		height = bounds.height - my1 - my2;
		int x0 = bounds.x + mx1 - PlotObject.rad;
		g.setColor(plotAreaColor);
		g.fillRect(bounds.x + mx1, bounds.y + my1, width, height);
		g.setColor(Color.darkGray);
		g.drawLine(bounds.x + mx1, bounds.y + my1, bounds.x + mx1, bounds.y + my1 + height);
		g.drawLine(bounds.x + mx1, bounds.y + my1 + height, bounds.x + mx1 + width, bounds.y + my1 + height);
		g.setColor(Color.gray);
		int p = width / 10;
		for (int i = 1; i <= 10; i++) {
			g.drawLine(bounds.x + mx1 + p, bounds.y + my1, bounds.x + mx1 + p, bounds.y + my1 + height);
			if (i < 10) {
				p += (width - p) / (10 - i);
			}
		}
		p = 0;
		for (int i = 0; i <= 9; i++) {
			g.drawLine(bounds.x + mx1, bounds.y + my1 + p, bounds.x + mx1 + width, bounds.y + my1 + p);
			p += (height - p) / (10 - i);
		}
		g.setColor(Color.blue.darker());
		g.drawString(strmax2, bounds.x + mx1 - lmax2 - 2, bounds.y + my1 + asc);
		g.drawString(strmin2, bounds.x + mx1 - lmin2 - 2, bounds.y + my1 + height);
		g.drawString(strmin1, bounds.x + mx1, bounds.y + my1 + height + asc);
		g.drawString(strmax1, bounds.x + mx1 + width - lmax1, bounds.y + my1 + height + asc);
	}

	@Override
	public void countScreenCoordinates() {
		for (int i = 0; i < dots.length; i++) {
			double v1 = getNumericAttrValue(fn1, i), v2 = getNumericAttrValue(fn2, i);
			if (!Double.isNaN(v1) && !Double.isNaN(v2)) {
				dots[i].x = mapX(v1);
				dots[i].y = mapY(v2);
			} else {
				dots[i].reset();
			}
		}
	}

	@Override
	public int mapX(double v) {
		return bounds.x + mx1 + (int) Math.round((v - min1) / (max1 - min1) * width);
	}

	@Override
	public int mapY(double v) {
		return bounds.y + my1 + height - (int) Math.round((v - min2) / (max2 - min2) * height);
	}

	@Override
	public double absX(int x) {
		return min1 + (max1 - min1) * (x - mx1 - bounds.x) / width;
	}

	@Override
	public double absY(int y) {
		return min2 + (max2 - min2) * (height - y + my1 + bounds.y) / height;
	}

	protected int dragX1 = -1, dragY1 = -1, dragX2 = dragX1, dragY2 = dragY1;
	protected boolean dragging = false;
	protected int prevLinePos[] = null;

	@Override
	public void mouseDragged(MouseEvent e) {
		if (canvas == null)
			return;
		int x = e.getX(), y = e.getY();

		if (!dragging && !isPointInPlotArea(dragX1, dragY1))
			return;
		dragging = dragging || Math.abs(x - dragX1) > 5 || Math.abs(y - dragY1) > 5;
		if (!dragging)
			return;
		if (x < bounds.x + mx1) {
			x = bounds.x + mx1;
		}
		if (x > bounds.x + mx1 + width) {
			x = bounds.x + mx1 + width;
		}
		if (y < bounds.y + my1) {
			y = bounds.y + my1;
		}
		if (y > bounds.y + my1 + height) {
			y = bounds.y + my1 + height;
		}
		if (x == dragX2 && y == dragY2)
			return;
		drawFrame(dragX1, dragY1, dragX2, dragY2);
		dragX2 = x;
		dragY2 = y;
		drawFrame(dragX1, dragY1, dragX2, dragY2);
		if ((dragX2 - dragX1) * (dragY2 - dragY1) > 0) {
			canvas.setCursor(new Cursor(Cursor.NW_RESIZE_CURSOR));
		} else {
			canvas.setCursor(new Cursor(Cursor.NE_RESIZE_CURSOR));
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {
		int x = e.getX(), y = e.getY();
		if (isPointInPlotArea(x, y)) {
			dragX1 = dragX2 = x;
			dragY1 = dragY2 = y;
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (!dragging)
			if (selectObjectAt(dragX1, dragY1, e)) {
				;
			} else {
				processMouseClick(dragX1, dragY1);
			}
		else {
			drawFrame(dragX1, dragY1, dragX2, dragY2);
			dragging = false;
			selectInFrame(dragX1, dragY1, dragX2, dragY2, e);
			canvas.setCursor(Cursor.getDefaultCursor());
		}
		dragX1 = dragY1 = dragX2 = dragY2 = -1;
	}

	protected void processMouseClick(int scrX, int scrY) {
	}

	@Override
	public Vector getAttributeList() {
		Vector a = null;
		if (dataTable != null && fn1 > -1 && fn2 > -1) {
			a = new Vector(2, 2);
			a.addElement(dataTable.getAttributeId(fn1));
			a.addElement(dataTable.getAttributeId(fn2));
		}
		return a;
	}

	/**
	* Used to generate unique identifiers of instances of Plot's descendants.
	* The base class Plot calls this method in the constructor.
	*/
	@Override
	protected void countInstance() {
		instanceN = ++nInstances;
	}

	/**
	* Returns "Scatter_Plot".
	* Used to generate unique identifiers of instances of Plot's descendants.
	* Plot uses this method in the method getIdentifier().
	*/
	@Override
	public String getPlotTypeName() {
		return "Scatter_Plot";
	}

}
