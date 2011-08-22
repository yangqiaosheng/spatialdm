package spade.analysis.plot;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.Drawable;
import spade.lib.basicwin.Focuser;
import spade.lib.basicwin.Metrics;
import spade.lib.util.Aligner;
import spade.lib.util.Formats;
import spade.time.TimeMoment;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.ObjectFilter;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Nov 15, 2010
 * Time: 4:39:41 PM
 * Used instead of DotPlot. Does not represent individual objects by circles but
 * object frequencies by shading
 */
public class DensityPlot implements Drawable, Destroyable, PropertyChangeListener, MouseListener, MouseMotionListener {
	public static final int minH = 2 * Metrics.mm();
	/**
	* The canvas in which this object should draw itself
	*/
	protected Canvas canvas = null;
	/**
	* The boundaries in which the object should fit itself
	*/
	protected Rectangle bounds = null;
	/**
	* Colors of the background of the canvas and of the plot area
	*/
	public static Color bkgColor = Color.white, plotAreaColor = Color.lightGray;
	/**
	* Aligner is used to align horisontally or vertically several plots
	*/
	protected Aligner aligner = null;
	/**
	* The source of the data to be shown on the plot
	*/
	protected AttributeDataPortion dataTable = null;
	/**
	* ObjectFilter may be associated with the table and contain results of data
	* querying. Only data satisfying the current query (if any) are displayed
	*/
	protected ObjectFilter tf = null;
	/**
	* The number of the field (attribute) to be represented.
	*/
	protected int fn = -1;
	/**
	* The minimum and maximum values of the field
	*/
	protected double min = Double.NaN, max = Double.NaN;
	/**
	 * If the attribute is temporal, these fields contain the absolute minimum and
	 * maximum time moments.
	 */
	protected TimeMoment minTime = null, maxTime = null;
	/**
	 * The value range of the attribute is divided into bins (intervals).
	 * For each bin, this array contains the respective count of the objects fitting
	 * in this bin
	 */
	protected long counts[] = null;
	/**
	 * Counts of the objects after filtering
	 */
	protected long activeCounts[] = null;
	/**
	 * max value of counts = size of the most populated bin
	 */
	protected long maxActiveCount = 0;
	/**
	 * The lower limits of the bins
	 */
	protected double lowLim[] = null;

	/**
	* Focusers are used for focusing on value subranges of the attributes
	*/
	private Focuser focuser = null;

	public Focuser getFocuser() {
		return focuser;
	}

	public void setDataSource(AttributeDataPortion tbl) {
		this.dataTable = tbl;
		if (dataTable != null) {
			dataTable.addPropertyChangeListener(this);
			tf = dataTable.getObjectFilter();
			if (tf != null) {
				tf.addPropertyChangeListener(this);
			}
		}
	}

	public void setFieldNumber(int n) {
		fn = n;
	}

	public int getFieldNumber() {
		return fn;
	}

	public void findMinMax() {
//    min=max=Double.NaN;
//    minTime=null; maxTime=null;
		boolean temporal = dataTable.isAttributeTemporal(fn);
		for (int i = 0; i < dataTable.getDataItemCount(); i++) {
			double v = dataTable.getNumericAttrValue(fn, i);
			if (!Double.isNaN(v)) {
				if (Double.isNaN(min) || v < min) {
					min = v;
				}
				if (Double.isNaN(max) || v > max) {
					max = v;
				}
			}
			if (temporal) {
				Object val = dataTable.getAttrValue(fn, i);
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
		}
		if (minTime != null) {
			minTime = minTime.getCopy();
		}
		if (maxTime != null) {
			maxTime = maxTime.getCopy();
		}
	}

	public void setMinMax(double minValue, double maxValue) {
		min = minValue;
		max = maxValue;
		if (focuser != null) {
			focuser.setAbsMinMax(min, max);
		}
	}

	/**
	 * If the attribute is temporal, sets the absolute minimum and maximum time
	 * moments. Thios information is used for displaying "nice" dates in text
	 * fields.
	 */
	public void setAbsMinMaxTime(TimeMoment t1, TimeMoment t2) {
		minTime = t1;
		maxTime = t2;
	}

	/**
	* Resets its internal data, including the array of screen objects.
	* Called when records are added to or removed from the table.
	*/
	public void reset() {
		min = max = Double.NaN;
		if (dataTable == null || fn < 0) {
			counts = activeCounts = null;
			lowLim = null;
			return;
		}
		setup();
		if (focuser != null) {
			focuser.setAbsMinMax(min, max);
		}
		redraw();
	}

	public void setup() {
		if (dataTable == null || fn < 0)
			return;
		counts = activeCounts = null;
		lowLim = null;
		if (Double.isNaN(min) || Double.isNaN(max)) {
			findMinMax();
		}
		constructFocuser();
		setupBins();
	}

	protected void setupBins() {
		// setup bins: lowLim, counts, activeCounts
		int n = (height == 0) ? 0 : (int) Math.ceil(width / height);
		if (n == 0 || (lowLim != null && n == lowLim.length))
			return;
		maxActiveCount = 0;
		lowLim = new double[n];
		for (int i = 1; i <= n; i++) {
			lowLim[i - 1] = min + i * (max - min) / (n + 1);
		}
		counts = new long[n + 1];
		activeCounts = new long[n + 1];
		for (int i = 0; i < counts.length; i++) {
			counts[i] = activeCounts[i] = 0;
		}
		if (n == 0) { // just a single bin
			counts[0] = dataTable.getDataItemCount();
			activeCounts[0] = 0;
			for (int rn = 0; rn < dataTable.getDataItemCount(); rn++)
				if (tf.isActive(rn)) {
					activeCounts[0]++;
				}
			maxActiveCount = activeCounts[0];
		} else {
			for (int rn = 0; rn < dataTable.getDataItemCount(); rn++) {
				double v = dataTable.getNumericAttrValue(fn, rn);
				int bin = 0;
				for (int i = 0; i < lowLim.length; i++)
					if (v >= lowLim[i]) {
						bin = i + 1;
					} else {
						break;
					}
				counts[bin]++;
				if (tf.isActive(rn)) {
					activeCounts[bin]++;
					if (activeCounts[bin] > maxActiveCount) {
						maxActiveCount = activeCounts[bin];
					}
				}
			}
			//System.out.println("* n="+n+", max="+maxActiveCount);
		}
	}

	protected void constructFocuser() {
		if (focuser == null) {
			focuser = new Focuser();
			focuser.setAttributeNumber(fn);
			focuser.setIsVertical(false);
			focuser.setAbsMinMax(min, max);
			focuser.setAbsMinMaxTime(minTime, maxTime);
			focuser.setSpacingFromAxis(0);
			focuser.setIsLeft(true); //top position
			//focuser.setIsLeft(false); //this is to check bottom position
			focuser.setBkgColor(bkgColor);
			focuser.setPlotAreaColor(plotAreaColor);
			//focuser.toDrawCurrMinMax=!isHorizontal;
			focuser.setToDrawCurrMinMax(false);
			focuser.setTextDrawing(false);
		}
	}

	public boolean reloadAttributeData(Vector v) {
		//System.out.println("Dot plot reloads attribute data");
		if (v == null)
			return false;
		for (int i = 0; i < v.size(); i++) {
			String AttrID = (String) v.elementAt(i);
			if (AttrID.equals(dataTable.getAttributeId(fn))) {
				double oldMin = min, oldMax = max;
				findMinMax();
				setup();
				if (Formats.areEqual(oldMin, min) && Formats.areEqual(oldMax, max))
					return true;
				if (focuser == null)
					return true;
				boolean lowLim = !Formats.areEqual(focuser.getCurrMin(), focuser.getAbsMin()), upLim = !Formats.areEqual(focuser.getCurrMax(), focuser.getAbsMax());
				focuser.setAbsMinMax(min, max);
				if (!lowLim || !upLim) {
					focuser.setCurrMinMax((lowLim) ? focuser.getCurrMin() : min, (upLim) ? focuser.getCurrMax() : max);
				}
				//min=focuser.getCurrMin();
				if (!focuser.getIsUsedForQuery()) {
					min = focuser.getCurrMin();
					max = focuser.getCurrMax();
				}
				//System.out.println("Dot plot has reloaded attribute data!");
				return true;
			}
		}
		return false;
	}

	public void applyFilter() {
		lowLim = null; // bin should be recomputed
		setupBins();
	}

	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		if (pce.getSource().equals(tf)) {
			if (pce.getPropertyName().equals("destroyed")) {
				tf.removePropertyChangeListener(this);
				tf = null;
			}
			if (!destroyed) {
				applyFilter();
				redraw();
			}
		} else if (pce.getSource().equals(aligner)) {
			redraw();
		} else if (pce.getSource().equals(dataTable)) {
			if (pce.getPropertyName().equals("destroyed") || pce.getPropertyName().equals("structure_complete")) {
				destroy();
			} else if (pce.getPropertyName().equals("filter")) {
				if (tf != null) {
					tf.removePropertyChangeListener(this);
				}
				tf = dataTable.getObjectFilter();
				if (tf != null) {
					tf.addPropertyChangeListener(this);
				}
				applyFilter();
				redraw();
			} else if (pce.getPropertyName().equals("values")) {
				applyFilter();
				Vector v = (Vector) pce.getNewValue(); // list of changed attributes
				if (reloadAttributeData(v)) {
					redraw();
				}
			} else if (pce.getPropertyName().equals("data_added") || pce.getPropertyName().equals("data_removed") || pce.getPropertyName().equals("data_updated")) {
				reset();
			}
		}
	}

	protected boolean destroyed = false;

	/**
	* Makes necessary operations for destroying, e.g. unregisters from
	* listening highlighting and other events.
	*/
	@Override
	public void destroy() {
		if (tf != null) {
			tf.removePropertyChangeListener(this);
		}
		if (dataTable != null) {
			dataTable.removePropertyChangeListener(this);
		}
		destroyed = true;
	}

	/**
	* Replies whether is destroyed or not
	*/
	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

//------------- Functions from the Drawable interface -------------------
	/**
	* Sets the canvas in which this object should draw itself
	*/
	@Override
	public void setCanvas(Canvas c) {
		canvas = c;
		canvas.addMouseListener(this);
		canvas.addMouseMotionListener(this);
	}

	public Canvas getCanvas() {
		return canvas;
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(100 * Metrics.mm(), minH);
	}

	protected static int minMarg = Focuser.getRequiredMargin();
	protected int width = 0, height = 0, mx1 = minMarg, mx2 = minMarg, my1 = minMarg, my2 = minMarg;

	@Override
	public void setBounds(Rectangle bounds) {
		this.bounds = bounds;
		if (bounds != null) {
			width = bounds.width - mx1 - mx2;
			height = bounds.height - my1 - my2;
		}
	}

	@Override
	public Rectangle getBounds() {
		return bounds;
	}

	protected Rectangle boxes[] = null;
	protected int dragStartX = -1, dragLastX = -1;
	protected double dragMin = Double.NaN, dragMax = Double.NaN;

	@Override
	public void draw(Graphics g) {
		if (bounds == null || dataTable == null || fn < 0)
			return;
		constructFocuser();
		//FontMetrics fm=g.getFontMetrics();
		//int fh=fm.getHeight(), asc=fm.getAscent();
		mx1 = -1;
		mx2 = -1;
		my1 = -1;
		my2 = -1;
		if (aligner != null) {
			mx1 = aligner.getLeft();
			mx2 = aligner.getRight();
		}
		if (mx1 < 0 || mx2 < 0) {
			mx1 = mx2 = minMarg;
		}
		if (my1 < 0 || my2 < 0) {
			my1 = focuser.getRequiredWidth(g);
			my2 = 1;
		}
		if (bounds.width - mx1 - mx2 < minH || bounds.height - my1 - my2 < minH)
			return;
		width = bounds.width - mx1 - mx2;
		height = bounds.height - my1 - my2;
		if (height > minH) {
			height = minH;
		}
		setupBins();

		drawFocuser(g);

		g.setColor(bkgColor);
		g.fillRect(bounds.x + mx1 - PlotObject.rad - 1, bounds.y + my1, PlotObject.rad + 1, height + 1);
		g.fillRect(bounds.x + mx1 + width, bounds.y + my1, PlotObject.rad + 1, height + 1);

		// fill white rectangle beore drawing bins
		g.fillRect(bounds.x + mx1, bounds.y + my1, width + 1, height + 1);

		// line between focuser and bins
		g.setColor(Color.darkGray);
		g.drawLine(bounds.x + mx1, bounds.y + my1, bounds.x + mx1 + width, bounds.y + my1);

		if (boxes == null || boxes.length != counts.length) {
			boxes = new Rectangle[counts.length];
			for (int i = 0; i < boxes.length; i++) {
				boxes[i] = null;
			}
		}

		int x1 = bounds.x + mx1, x2 = bounds.x + mx1 + width;
		for (int i = 0; i < counts.length; i++) {
			if (counts.length > 1) {
				x1 = bounds.x + mx1 + i * width / counts.length;
				x2 = bounds.x + mx1 + (i + 1) * width / counts.length;
			}
			if (boxes[i] == null) {
				boxes[i] = new Rectangle(x1, bounds.y + my1, x2 - x1, height);
			} else {
				boxes[i].x = x1;
				boxes[i].width = x2 - x1;
			}
			if (counts[i] > 0) {
				if (activeCounts[i] > 0) {
					int rgb = Math.round(255 * (1 - 1f * activeCounts[i] / maxActiveCount));
					g.setColor(new Color(rgb, rgb, rgb));
					g.fillRect(x1, bounds.y + my1 + 1, x2 - x1 + 1, height);
					g.setColor(Color.darkGray);
				} else {
					g.setColor(Color.lightGray);
				}
				g.drawLine(x1, bounds.y + my1 + height, x2, bounds.y + my1 + height);
			}
		}
	}

	protected void drawFocuser(Graphics g) {
		if (focuser == null)
			return;
		focuser.setAlignmentParameters(bounds.x + mx1, bounds.y + my1, width); //top position
		focuser.draw(g);
	}

	protected void drawDrag(int x) {
		if (dragStartX < 0 || x < 0 || boxes == null || boxes[0] == null)
			return;
		Graphics g = canvas.getGraphics();
		g.setColor(Color.magenta);
		g.setXORMode(plotAreaColor);
		int x1 = dragStartX; //calcMidX(dragStartX);
		int x2 = x; //calcMidX(x);
		int y = boxes[0].y + boxes[0].height / 2;
		g.drawLine(x1, y, x2, y);
		g.drawLine(x1, y + 2, x1, y - 2);
		g.drawLine(x2, y + 2, x2, y - 2);
		/*
		String str=StringUtil.doubleToStr(dragMin,dragMin,dragMax)+".."+
		           StringUtil.doubleToStr(dragMax,dragMin,dragMax);
		g.setColor(Color.black);
		g.drawString(str,
		             (x1 + x2) / 2 - g.getFontMetrics().stringWidth(str) / 2 + 1,
		             y + 1 + g.getFontMetrics().getHeight());
		g.setColor(Color.white);
		g.drawString(str, (x1 + x2) / 2 - g.getFontMetrics().stringWidth(str) / 2,
		             y + 2 + g.getFontMetrics().getHeight());
		*/
	}

	public void redraw() {
		if (canvas == null || bounds == null || dataTable == null || fn < 0)
			return;
		Graphics g = canvas.getGraphics();
		if (g == null)
			return;
		draw(g);
		g.dispose();
	}

	public void setAligner(Aligner al) {
		aligner = al;
	}

	protected void drag(int x) {
		if (dragStartX < 0) {
			dragStartX = x;
		} else {
			if (dragLastX >= 0) {
				drawDrag(dragLastX);
			}
			dragLastX = x;
			drawDrag(dragLastX);
			int x1 = Math.min(dragStartX, dragLastX), x2 = Math.max(dragStartX, dragLastX);
			if (boxes.length < 2 || x1 <= boxes[1].x) {
				dragMin = min;
			} else {
				dragMin = Double.NaN;
				for (int i = 1; Double.isNaN(dragMin) && i < boxes.length; i++)
					if (x1 >= boxes[i].x && x1 <= boxes[i].x + boxes[i].width) {
						dragMin = lowLim[i - 1];
					}
			}
			int bl = boxes.length;
			if (boxes.length < 2 || x2 >= boxes[bl - 1].x + boxes[bl - 1].width) {
				dragMax = max;
			} else {
				dragMax = Double.NaN;
				for (int i = bl - 1; Double.isNaN(dragMax) && i >= 0; i--)
					if (x2 >= boxes[i].x) {
						dragMax = (i == bl - 1) ? max : lowLim[i];
					}
			}
			//System.out.println("* x1="+x1+", x2="+x2+", min="+dragMin+", max="+dragMax);
		}
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if (canvas == null || focuser == null)
			return;
		if (focuser.isMouseCaptured()) {
			Graphics g = canvas.getGraphics();
			focuser.mouseDragged(e.getX(), e.getY(), g);
			g.dispose();
			return;
		}
		if (boxes != null && boxes[0] != null && (dragStartX >= 0 || (e.getY() >= boxes[0].y && e.getY() <= boxes[0].y + boxes[0].width))) {
			drag(e.getX());
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {
		focuser.captureMouse(e.getX(), e.getY());
		if (focuser.isMouseCaptured()) {
			canvas.setCursor(new Cursor(Cursor.W_RESIZE_CURSOR));
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (focuser.isMouseCaptured()) {
			focuser.releaseMouse();
			canvas.setCursor(Cursor.getDefaultCursor());
			return;
		}
		if (dragStartX >= 0) {
			drawDrag(e.getX());
			if (!Double.isNaN(dragMin)) {
				focuser.setCurrMinMax(dragMin, dragMax);
			}
			dragMin = min;
			dragMax = max;
			dragStartX = -1;
			dragLastX = -1;
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mouseMoved(MouseEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}
}
