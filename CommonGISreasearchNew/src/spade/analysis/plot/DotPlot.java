package spade.analysis.plot;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.util.Hashtable;
import java.util.Vector;

import spade.analysis.system.Supervisor;
import spade.lib.basicwin.Focuser;
import spade.lib.basicwin.Metrics;
import spade.lib.util.Formats;
import spade.lib.util.StringUtil;
import spade.time.TimeMoment;
import spade.vis.action.ObjectEventHandler;

/**
* A class that represents values of single numeric attribute on a dot plot.
* The data to represent are taken from an AttributeDataPortion.
*/

public class DotPlot extends Plot {
	public static final int minH = 2 * Metrics.mm();
	/**
	* Used to generate unique identifiers of instances of DotPlot
	*/
	protected static int nInstances = 0;

	protected boolean isHorisontal = true, focuserLeft = false;
	protected boolean toDrawTexts = true, focuserDrawsTexts = true;;
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
	* Focusers are used for focusing on value subranges of the attributes
	*/
	private Focuser focuser = null;

	public Focuser getFocuser() {
		return focuser;
	}

	/**
	* If this variable is true, the dot plot is allowed to reset the delimiters
	* in the focuser so that they always move to the object with the nearest value.
	* By default is true.
	*/
	protected boolean mayMoveDelimiters = true;

	public void setMayMoveDelimiters(boolean value) {
		mayMoveDelimiters = value;
	}

	/**
	* Constructs a DotPlot. The argument isIndependent shows whether
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
	public DotPlot(boolean isHorisontal, boolean isIndependent, boolean allowSelection, Supervisor sup, ObjectEventHandler handler) {
		super(isIndependent, allowSelection, sup, handler);
		this.isHorisontal = isHorisontal;
		focuserLeft = isHorisontal;
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
			double v = getNumericAttrValue(fn, i);
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
	@Override
	public void reset() {
		min = max = Double.NaN;
		if (dataTable == null || fn < 0) {
			dots = null;
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
		if (Double.isNaN(min) || Double.isNaN(max)) {
			findMinMax();
		}
		if (dots == null || dots.length != dataTable.getDataItemCount()) {
			dots = new DotPlotObject[dataTable.getDataItemCount()];
			for (int i = 0; i < dots.length; i++) {
				dots[i] = new DotPlotObject();
			}
		}
		for (int i = 0; i < dots.length; i++) {
			dots[i].reset();
			dots[i].id = dataTable.getDataItemId(i);
		}
		applyFilter();
		plotImageValid = bkgImageValid = false;
		constructFocuser();
	}

	@Override
	public boolean reloadAttributeData(Vector v) {
		//System.out.println("Dot plot reloads attribute data");
		if (v == null)
			return false;
		for (int i = 0; i < v.size(); i++) {
			String AttrID = (String) v.elementAt(i);
			if (AttrID.equals(dataTable.getAttributeId(fn))) {
				plotImageValid = bkgImageValid = false;
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

	@Override
	public boolean hasData() {
		return dots != null && !Double.isNaN(min);
	}

	public void setFocuserOnLeft(boolean value) {
		focuserLeft = value;
	}

	protected void constructFocuser() {
		if (!isZoomable)
			return;
		if (focuser == null) {
			focuser = new Focuser();
			focuser.setAttributeNumber(fn);
			focuser.setIsVertical(!isHorisontal);
			focuser.setAbsMinMax(min, max);
			focuser.setAbsMinMaxTime(minTime, maxTime);
			focuser.setSpacingFromAxis(0);
			focuser.setIsLeft(focuserLeft); //top position
			//focuser.setIsLeft(false); //this is to check bottom position
			focuser.setBkgColor(bkgColor);
			focuser.setPlotAreaColor(plotAreaColor);
			//focuser.toDrawCurrMinMax=!isHorizontal;
			focuser.setToDrawCurrMinMax(!isHorisontal);
			focuser.addFocusListener(this);
			focuser.setTextDrawing(focuserDrawsTexts);
		}
	}

	protected void defineAlignment() {
		if (mayDefineAlignment && aligner != null) {
			if (isHorisontal) {
				aligner.setMargins(mx1, mx2, -1, -1);
			} else {
				aligner.setMargins(-1, -1, my2, my1);
			}
		}
	}

	protected void drawFocuser(Graphics g) {
		if (focuser == null)
			return;
		if (isZoomable) {
			if (isHorisontal)
				if (focuserLeft) {
					focuser.setAlignmentParameters(bounds.x + mx1, bounds.y + my1, width); //top position
				} else {
					focuser.setAlignmentParameters(bounds.x + mx1, bounds.y + bounds.height - my2, width); //bottom position
				}
			else if (focuserLeft) {
				focuser.setAlignmentParameters(bounds.x + mx1, bounds.y + my1 + height, height); //left position
			} else {
				focuser.setAlignmentParameters(bounds.x + mx1 + width, bounds.y + my1 + height, height); //right position
			}
			focuser.draw(g);
		}
	}

	private String prevMinStr = null, prevMaxStr = null;

	public void setTextDrawing(boolean value) {
		toDrawTexts = value;
	}

	public void setFocuserDrawsTexts(boolean value) {
		focuserDrawsTexts = value;
		if (focuser != null) {
			focuser.setTextDrawing(value);
		}
	}

	public void drawTexts(Graphics g) {
		if (!toDrawTexts)
			return;
		if (focuser != null) {
			drawTexts(g, focuser.getCurrMin(), focuser.getCurrMax());
		} else {
			drawTexts(g, min, max);
		}
	}

	protected String numToString(double num, double min, double max) {
		if (minTime == null)
			return StringUtil.doubleToStr(num, min, max);
		return minTime.valueOf((long) num).toString();
	}

	public void drawTexts(Graphics g, double mint, double maxt) {
		if (!toDrawTexts)
			return;
		FontMetrics fm = g.getFontMetrics();
		int fh = fm.getHeight(), asc = fm.getAscent();
		if (isHorisontal) {
			int lmax = (prevMaxStr == null) ? 0 : fm.stringWidth(prevMaxStr);
			g.setColor(Color.white);
			int y = (focuserLeft) ? bounds.y + my1 + height + asc : bounds.y + asc;
			if (focuser != null && prevMinStr != null && focuser.MinDelimMoving()) {
				g.drawString(prevMinStr, bounds.x + mx1, y);
			}
			if (focuser != null && prevMaxStr != null && focuser.MaxDelimMoving()) {
				g.drawString(prevMaxStr, bounds.x + mx1 + width - lmax, y);
			}
			//g.fillRect(bounds.x+mx1,bounds.y+my1+height,width,asc);
			g.setColor(Color.blue.darker());
			prevMinStr = numToString(mint, mint, maxt);
			prevMaxStr = numToString(maxt, mint, maxt);
			lmax = fm.stringWidth(prevMaxStr);
			g.drawString(prevMinStr, bounds.x + mx1, y);
			g.drawString(prevMaxStr, bounds.x + mx1 + width - lmax, y);
		} else if (focuser != null) {
			; // drawing is done in the focuser
		} else {
			int x = (focuserLeft) ? mx1 + minH + 2 : 2;
			g.setColor(Color.white);
			g.fillRect(x, bounds.y + my1, bounds.width - x + 1, height);
			g.setColor(Color.black);
			String str = numToString(mint, mint, maxt);
			g.drawString(str, x, mapY(mint));
			str = numToString(maxt, mint, maxt);
			g.drawString(str, x, mapY(maxt) + asc);
		}
	}

	@Override
	public void drawReferenceFrame(Graphics g) {
		if (bounds == null || !hasData())
			return;
		constructFocuser();
		FontMetrics fm = g.getFontMetrics();
		int fh = fm.getHeight(), asc = fm.getAscent();
		mx1 = -1;
		mx2 = -1;
		my1 = -1;
		my2 = -1;
		if (!mayDefineAlignment && aligner != null)
			if (isHorisontal) {
				mx1 = aligner.getLeft();
				mx2 = aligner.getRight();
			} else {
				my1 = aligner.getBottom();
				my2 = aligner.getTop();
			}
		if (mx1 < 0 || mx2 < 0) {
			mx1 = mx2 = (isHorisontal) ? minMarg : 0;
			if (isZoomable && !isHorisontal)
				if (focuserLeft) {
					mx1 += focuser.getRequiredWidth(g) + 1; //left position
				} else {
					mx2 += focuser.getRequiredWidth(g) + 1; //right position
				}
		}
		if (my1 < 0 || my2 < 0) {
			my1 = my2 = (isHorisontal) ? 1 : minMarg;
			if (toDrawTexts)
				if (isHorisontal)
					if (focuserLeft) {
						my2 += asc + 1;
					} else {
						my1 += asc + 1;
					}
				else if (!isZoomable && !focuserLeft) {
					String str = numToString(min, min, max);
					int sw = fm.stringWidth(str);
					str = numToString(max, min, max);
					int sw1 = fm.stringWidth(str);
					if (sw < sw1) {
						sw = sw1;
					}
					mx1 += sw;
					if (bounds.width - mx1 - mx2 < minH) {
						mx1 = bounds.width - mx2 - minH;
					}
				}
			if (isZoomable && isHorisontal)
				if (focuserLeft) {
					my1 = focuser.getRequiredWidth(g); //top position
				} else {
					my2 = bounds.height - my1 - minH; //bottom position
					if (my2 < focuser.getRequiredWidth(g)) {
						my2 = focuser.getRequiredWidth(g);
					}
				}
		}
		defineAlignment();
		if (bounds.width - mx1 - mx2 < minH || bounds.height - my1 - my2 < minH)
			return;
		width = bounds.width - mx1 - mx2;
		if (!isHorisontal && width > minH) {
			width = minH;
		}
		height = bounds.height - my1 - my2;
		if (isHorisontal && height > minH) {
			height = minH;
		}

		drawFocuser(g);

		g.setColor(bkgColor);
		if (isHorisontal) {
			g.fillRect(bounds.x + mx1 - PlotObject.rad - 1, bounds.y + my1, PlotObject.rad + 1, height + 1);
			g.fillRect(bounds.x + mx1 + width, bounds.y + my1, PlotObject.rad + 1, height + 1);
		} else {
			g.fillRect(bounds.x + mx1, bounds.y + my1 - PlotObject.rad - 1, width + 1, PlotObject.rad + 1);
			g.fillRect(bounds.x + mx1, bounds.y + my1 + height, width + 1, PlotObject.rad + 1);
		}
		g.setColor(plotAreaColor);
		g.fillRect(bounds.x + mx1, bounds.y + my1, width + 1, height + 1);
		g.setColor(Color.darkGray);
		g.drawLine(bounds.x + mx1, bounds.y + my1, bounds.x + mx1, bounds.y + my1 + height);
		g.drawLine(bounds.x + mx1 + width, bounds.y + my1, bounds.x + mx1 + width, bounds.y + my1 + height);
		g.drawLine(bounds.x + mx1, bounds.y + my1, bounds.x + mx1 + width, bounds.y + my1);
		g.drawLine(bounds.x + mx1, bounds.y + my1 + height, bounds.x + mx1 + width, bounds.y + my1 + height);

		g.setColor(Color.gray);
		int p = 0;
		if (isHorisontal) {
			p = width / 10;
			for (int i = 1; i <= 10; i++) {
				g.drawLine(bounds.x + mx1 + p, bounds.y + my1, bounds.x + mx1 + p, bounds.y + my1 + height);
				if (i < 10) {
					p += (width - p) / (10 - i);
				}
			}
		} else {
			p = 0;
			for (int i = 0; i <= 9; i++) {
				g.drawLine(bounds.x + mx1, bounds.y + my1 + p, bounds.x + mx1 + width, bounds.y + my1 + p);
				p += (height - p) / (10 - i);
			}
		}
		g.drawLine(bounds.x + mx1, bounds.y + my1, bounds.x + mx1 + width, bounds.y + my1);
		drawTexts(g);
	}

	@Override
	public void countScreenCoordinates() {
		for (int i = 0; i < dots.length; i++) {
			double v = getNumericAttrValue(fn, i);
			if (!Double.isNaN(v)) {
				if (isHorisontal) {
					dots[i].x = mapX(v);
					dots[i].y = bounds.y + my1 + Metrics.mm(); //mapY(v2);
				} else {
					dots[i].y = mapY(v);
					dots[i].x = bounds.x + mx1 + Metrics.mm(); //mapY(v2);
				}
			}
		}
	}

	@Override
	public int mapX(double v) {
		return bounds.x + mx1 + (int) Math.round((v - min) / (max - min) * width);
	}

	@Override
	public int mapY(double v) {
		return bounds.y + my1 + height - (int) Math.round((v - min) / (max - min) * height);
	}

	@Override
	public double absX(int x) {
		return min + (max - min) * (x - mx1 - bounds.x) / width;
	}

	@Override
	public double absY(int y) {
		return min + (max - min) * (height - y + my1 + bounds.y) / height;
	}

	@Override
	public Dimension getPreferredSize() {
		int w = minH + 2;
		if (isZoomable) {
			w += Focuser.mainWidth + Metrics.mm() * 2;
			if (focuserDrawsTexts) {
				w += (isHorisontal) ? Metrics.mm() * 3 : Metrics.mm() * 8;
			}
		}
		if (toDrawTexts)
			if (isHorisontal) {
				w += Metrics.mm() * 3;
			} else if (!focuserDrawsTexts) {
				w += Metrics.mm() * 8;
			}
		return (isHorisontal) ? new Dimension(40 * Metrics.mm(), w) : new Dimension(w, 40 * Metrics.mm());
	}

	// focuser methods - begin
	private int[] prevLinePos = null;

	private void drawLimitLine(int n, int pos) { // n==0 -> min; n==1 -> max
		if (canvas == null)
			return;
		if (prevLinePos == null) {
			prevLinePos = new int[2];
			prevLinePos[0] = prevLinePos[1] = -1;
		}
		if (pos != prevLinePos[n]) {
			Graphics gr = canvas.getGraphics();
			gr.setColor(Color.magenta);
			gr.setXORMode(plotAreaColor);
			if (isHorisontal) {
				int y1 = bounds.y + my1, y2 = bounds.y + my1 + height, y = 0;
				if (focuser == null) {
					y1 -= Metrics.mm();
					y2 += Metrics.mm();
				} else {
					y = focuser.getAxisPosition();
					if (y > y2) {
						y1 -= Metrics.mm();
					} else {
						y2 += Metrics.mm();
					}
				}
				if (prevLinePos[n] >= bounds.x + mx1 && prevLinePos[n] <= bounds.x + mx1 + width) {
					//erase the previous line
					gr.drawLine(prevLinePos[n], y1, prevLinePos[n], y2);
				}
				prevLinePos[n] = pos;
				if (prevLinePos[n] >= bounds.x + mx1 && prevLinePos[n] <= bounds.x + mx1 + width) {
					// draw new line
					gr.drawLine(prevLinePos[n], y1, prevLinePos[n], y2);
					if (focuser != null) {
						//connect the line to the delimiter
						int x = (n == 0) ? focuser.getMinPos() : focuser.getMaxPos();
						if (y < y1) {
							gr.drawLine(x, y, prevLinePos[n], y1);
						} else {
							gr.drawLine(x, y, prevLinePos[n], y2);
						}
					}
				}
			} else {
				int x1 = bounds.x + mx1, x2 = bounds.x + mx1 + width, x = 0;
				if (focuser == null) {
					x1 -= Metrics.mm();
					x2 += Metrics.mm();
				} else {
					x = focuser.getAxisPosition();
					if (x > x2) {
						x1 -= Metrics.mm();
					} else {
						x2 += Metrics.mm();
					}
				}
				if (prevLinePos[n] >= bounds.y + my1 && prevLinePos[n] <= bounds.y + my1 + height) {
					//erase the previous line
					gr.drawLine(x1, prevLinePos[n], x2, prevLinePos[n]);
				}
				prevLinePos[n] = pos;
				if (prevLinePos[n] >= bounds.y + my1 && prevLinePos[n] <= bounds.y + my1 + height) {
					// draw new line
					gr.drawLine(x1, prevLinePos[n], x2, prevLinePos[n]);
					if (focuser != null) {
						//connect the line to the delimiter
						int y = (n == 0) ? focuser.getMinPos() : focuser.getMaxPos();
						if (x < x1) {
							gr.drawLine(x, y, x1, prevLinePos[n]);
						} else {
							gr.drawLine(x, y, x2, prevLinePos[n]);
						}
					}
				}
			}
			gr.setPaintMode();
			gr.dispose();
		}
	}

	@Override
	public void focusChanged(Object source, double low, double up) {
		if (isHidden)
			return;
		if (!isZoomable || focuser == null)
			return;
		if (!(source instanceof Focuser))
			return;
		plotImageValid = bkgImageValid = false;
		if (prevLinePos != null) {
			prevLinePos[0] = prevLinePos[1] = -1;
		}
		double lowerLimit = low, upperLimit = up;
		if (Double.isNaN(lowerLimit)) {
			lowerLimit = focuser.getAbsMin();
		}
		if (Double.isNaN(upperLimit)) {
			upperLimit = focuser.getAbsMax();
		}
		// adjust focuser to a next value
		if (!focuser.getIsUsedForQuery() && mayMoveDelimiters) {
			if (lowerLimit < min) { // dragging left or down
				double breakVal = focuser.getAbsMin();
				for (int i = 0; i < dataTable.getDataItemCount(); i++) {
					double v = getNumericAttrValue(fn, i);
					if (Double.isNaN(v) || v > lowerLimit) {
						continue;
					}
					if (v > breakVal) {
						breakVal = v;
					}
				}
				lowerLimit = breakVal;
			} else { // dragging right or up
				double breakVal = focuser.getAbsMax();
				for (int i = 0; i < dataTable.getDataItemCount(); i++) {
					double v = getNumericAttrValue(fn, i);
					if (Double.isNaN(v) || v < lowerLimit) {
						continue;
					}
					if (v < breakVal) {
						breakVal = v;
					}
				}
				lowerLimit = breakVal;
			}
			if (upperLimit < max) { // dragging left or down
				double breakVal = focuser.getAbsMin();
				for (int i = 0; i < dataTable.getDataItemCount(); i++) {
					double v = getNumericAttrValue(fn, i);
					if (Double.isNaN(v) || v > upperLimit) {
						continue;
					}
					if (v > breakVal) {
						breakVal = v;
					}
				}
				upperLimit = breakVal;
			} else { // dragging right or up
				double breakVal = focuser.getAbsMax();
				for (int i = 0; i < dataTable.getDataItemCount(); i++) {
					double v = getNumericAttrValue(fn, i);
					if (Double.isNaN(v) || v < upperLimit) {
						continue;
					}
					if (v < breakVal) {
						breakVal = v;
					}
				}
				upperLimit = breakVal;
			}
		}
		if (!focuser.getIsUsedForQuery()) {
			min = lowerLimit;
			max = upperLimit;
		}
		if (Double.isNaN(low) || Double.isNaN(up) || low != lowerLimit || up != upperLimit) {
			focuser.setCurrMinMax(lowerLimit, upperLimit);
		}
		if (canvas != null) {
			canvas.repaint();
		}
	}

	@Override
	public void limitIsMoving(Object source, int n, double currValue) {
		if (isHidden)
			return;
		if (!isZoomable || focuser == null)
			return;
		if (canvas == null)
			return;
		if (!(source instanceof Focuser))
			return;
		drawLimitLine(n, (isHorisontal) ? mapX(currValue) : mapY(currValue));
		Graphics g = canvas.getGraphics();
		drawTexts(g, focuser.getCurrMin(), focuser.getCurrMax());
		g.dispose();
	}

	// focuser methods - end

	protected int dragX1 = -1, dragY1 = -1, dragX2 = dragX1, dragY2 = dragY1;
	protected boolean dragging = false;

	@Override
	public void mouseDragged(MouseEvent e) {
		if (isHidden)
			return;
		if (canvas == null)
			return;
		int x = e.getX(), y = e.getY();
		if (isZoomable) {
			if (focuser.isMouseCaptured()) {
				Graphics g = canvas.getGraphics();
				focuser.mouseDragged(x, y, g);
				g.dispose();
				return;
			}
		}
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
		if (isHidden)
			return;
		int x = e.getX(), y = e.getY();
		if (isPointInPlotArea(x, y)) {
			dragX1 = dragX2 = x;
			dragY1 = dragY2 = y;
		} else if (isZoomable) {
			focuser.captureMouse(x, y);
			if (focuser.isMouseCaptured()) {
				canvas.setCursor(new Cursor((isHorisontal) ? Cursor.W_RESIZE_CURSOR : Cursor.N_RESIZE_CURSOR));
			}
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (isHidden)
			return;
		if (isZoomable && focuser.isMouseCaptured()) {
			focuser.releaseMouse();
			canvas.setCursor(Cursor.getDefaultCursor());
			return;
		}
		if (!dragging) {
			selectObjectAt(dragX1, dragY1, e);
		} else {
			drawFrame(dragX1, dragY1, dragX2, dragY2);
			dragging = false;
			selectInFrame(dragX1, dragY1, dragX2, dragY2, e);
			canvas.setCursor(Cursor.getDefaultCursor());
		}
		dragX1 = dragY1 = dragX2 = dragY2 = -1;
	}

	@Override
	protected void drawFrame(int x0, int y0, int x, int y) {
		if (canvas == null)
			return;
		Graphics gr = canvas.getGraphics();
		gr.setColor(Color.magenta);
		gr.setXORMode(plotAreaColor);
		gr.drawLine(x0, y0, x, y0);
		gr.drawLine(x, y0, x, y);
		gr.drawLine(x, y, x0, y);
		gr.drawLine(x0, y, x0, y0);
		gr.setPaintMode();
		gr.dispose();
	}

	@Override
	public void destroy() {
		super.destroy();
		if (focuser != null) {
			focuser.destroy();
		}
	}

	@Override
	public Vector getAttributeList() {
		Vector a = null;
		if (dataTable != null && fn > -1) {
			a = new Vector(1, 2);
			a.addElement(dataTable.getAttributeId(fn));
		}
		return a;
	}

//ID
	/**
	 * Returns custom properties of the tool: String -> String
	 * By default, returns null.
	 */
	@Override
	public Hashtable getProperties() {
		Hashtable prop = new Hashtable();
		prop.put("focuserMin", String.valueOf(focuser.getCurrMin()));
		prop.put("focuserMax", String.valueOf(focuser.getCurrMax()));
		return prop;
	}

	/**
	 * After the plot is constructed, it may be requested to setup its individual
	 * properties according to the given list of stored properties.
	 * The base Plot class does nothing in this method.
	 */
	@Override
	public void setProperties(Hashtable properties) {
		super.setProperties(properties);
		try {
			focuser.setCurrMinMax(new Double((String) properties.get("focuserMin")).doubleValue(), new Double((String) properties.get("focuserMax")).doubleValue());
		} catch (Exception ex) {
		}
	}

//~ID

	/**
	* Used to generate unique identifiers of instances of Plot's descendants.
	* The base class Plot calls this method in the constructor.
	*/
	@Override
	protected void countInstance() {
		instanceN = ++nInstances;
	}

	/**
	* Returns "Dot_Plot".
	* Used to generate unique identifiers of instances of Plot's descendants.
	* Plot uses this method in the method getIdentifier().
	*/
	@Override
	public String getPlotTypeName() {
		return "Dot_Plot";
	}
}
