package spade.lib.basicwin;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import spade.lib.util.Formats;
import spade.lib.util.StringUtil;
import spade.time.TimeMoment;

/**
* A widget for selection on a value subrange of a numeric attribute.
* Focuser is intentionally not implemented as a Canvas to allow for
* inclusion into other canvases, e.g. with a scatterplot. The canvas that
* uses a focuser should care about its appropriate alignment with other
* elements of the graphic, e.g. with one of the scatterplot axes.
* For this purpose it uses the method setBounds(...).
* The canvas should also transmit to the Focuser the mouse events happening
* in the area of the Focuser.
* A Focuser does not connect directly to a data source. Instead it gets from
* its owner the minimum and the maximum values of the attribute.
* To avoid confusion when several Focusers are present simultaneously, keeps
* the number of the attribute.
* A Focuser may be given a pair of TextFields where the user can specify
* exact minimum and maximum values. The Focuser listens to action events from
* these text fields and changes accordingly the positions of the delimiters.
* And vice versa, when positions of the delimiters change, the Focuser
* sets appropriately the values in the text fields.
*/
public class Focuser implements ActionListener, Drawable {

	public static final int mm = Math.round(java.awt.Toolkit.getDefaultToolkit().getScreenResolution() / 25.33f);
	public static final int delimiterW = Math.round(1.2f * mm), delimiterH = 3 * mm, mainWidth = delimiterH + mm, halfDH = delimiterH / 2;

	protected static int X[] = new int[4], Y[] = new int[4];

	protected Canvas canvas = null;

	@Override
	public void setCanvas(Canvas canvas) {
		this.canvas = canvas;
	}

	/**
	* The origin and the length of the axis the focuser should be aligned with
	*/
	protected int x0 = 0, y0 = 0, length = 0;

	/**
	* Indicates whether the Focuser is placed on the left or on the right of
	* the axis it is aligned with (for the horisontal case, above and
	* below, respectively).
	*/
	protected boolean leftAligned = true;

	/**
	* Spacing between the Focuser and the axis it is aligned with.
	* The default value (0) can be changed by the owner.
	*/
	protected int space = 0;

	/**
	* The required width of the widget (including texts)
	*/
	protected int width = mainWidth;

	/**
	* A Focuser may have either horisontal or vertical orientation
	*/
	protected boolean isVertical = false;
	/**
	* A Focuser may be used either for zooming or for dynamic query.
	* It is drawn differently in these cases.
	*/
	protected boolean usedForQuery = false;
	/**
	* Indficates whether the Focuser should draw texts (limits)
	*/
	protected boolean toDrawTexts = true;

	/**
	* The attribute number (not used inside the class; may be useful for
	* identification when there are several Focusers).
	*/
	protected int attrN = -1;

	/**
	* The text fields for specification of exact minimum and maximum limits
	*/
	protected TextField minTF = null, maxTF = null;
	/**
	* Indicates "destroyed" state. Initially id false.
	*/
	protected boolean destroyed = false;
	/**
	   it has the value "left" or "right" for the presentation with single delimiters
	 */
	protected String single = "";
	/**
	* Absolute minimum and maximum values of the attribute
	*/
	protected double absMin = Double.NaN, absMax = absMin;

	public double getAbsMin() {
		return absMin;
	}

	public double getAbsMax() {
		return absMax;
	}

	/**
	 * If the attribute is temporal, these fields contain the absolute minimum and
	 * maximum time moments.
	 */
	protected TimeMoment minTime = null, maxTime = null;
	/**
	 * If the attribute is temporal, these fields contain the current minimum and
	 * maximum time moments.
	 */
	protected TimeMoment currMinTime = null, currMaxTime = null;

	/**
	* String representation of the minimum and maximum values of the attribute
	*/
	protected String minStr = null, maxStr = null;

	/**
	* Boundaries of the "hot spots" reacting to mouse pressing, i.e. of the
	* delimiters.
	*/
	protected Rectangle minDelim = null, maxDelim = null, minmaxDelim = null;
	protected int minmaxPos = 0; // >0 if minmax, shows screen distance to min

	/**
	* Currently selected minimum and maximum values of the attribute
	*/
	protected double currMin = Double.NaN, currMax = absMin;

	@Override
	public Rectangle getBounds() {
		return new Rectangle(0, 0, width, 5 * mm);
	};

	public double getCurrMin() {
		return currMin;
	}

	public double getCurrMax() {
		return currMax;
	}

	public boolean isRestricted() {
		return currMin > absMin || currMax < absMax;
	}

	/**
	* Drawing current min and max values in vertical focuser
	*/
	private boolean toDrawCurrMinMax = false;

	public void setToDrawCurrMinMax(boolean toDrawCurrMinMax) {
		this.toDrawCurrMinMax = toDrawCurrMinMax;
	}

	/*
	* previous min and max ... valid only during dragging, afterwards Float.NaN
	*/
	protected double prevMin = Double.NaN, prevMax = Double.NaN;

	public double getPrevMin() {
		return prevMin;
	}

	public double getPrevMax() {
		return prevMax;
	}

	@Override
	public Dimension getPreferredSize() {
		Dimension d = new Dimension(100, 20);
		//if (canvas!=null) d=new Dimension(canvas.getSize());
		return d;
	}

	/**
	* Current positions of the delimiters (reflecting currently selected
	* minimum and maximum values of the attribute).
	*/
	protected int minPos = -1, maxPos = -1;

	/**
	* Indicate whether the minimum delimiter or maximum delimiter are
	* currently being moved by mouse dragging.
	*/
	protected boolean minDelimMoving = false, maxDelimMoving = false;

	public boolean MinDelimMoving() {
		return minDelimMoving;
	}

	public boolean MaxDelimMoving() {
		return maxDelimMoving;
	}

	/**
	* Colors of the background and of the plot area
	*/
	protected Color bkgColor = Color.white, plotAreaColor = Color.lightGray;

	/**
	* Listeners of the focusing events
	*/
	protected Vector listeners = null;

	public static int getRequiredMargin() {
		return delimiterW;
	}

	/**
	* Adds a listener of focusing events
	*/
	public void addFocusListener(FocusListener flist) {
		if (flist == null)
			return;
		if (listeners == null) {
			listeners = new Vector(5, 5);
		}
		if (listeners.contains(flist))
			return;
		listeners.addElement(flist);
	}

	public void removeFocusListener(FocusListener flist) {
		if (flist == null || listeners == null)
			return;
		int idx = listeners.indexOf(flist);
		if (idx < 0)
			return;
		listeners.removeElementAt(idx);
	}

	/**
	* Notifies all the registered listeners about change of the focus interval
	*/
	public void notifyFocusChange() {
		setTextsInTextFields();
		if (listeners == null)
			return;
		for (int i = 0; i < listeners.size(); i++) {
			((FocusListener) listeners.elementAt(i)).focusChanged(this, currMin, currMax);
		}
	}

	/**
	* Notifies all the registered listeners about one of the limits being moved
	*/
	protected void notifyLimitMoving(int n, double currValue) {
		setTextsInTextFields();
		if (listeners == null)
			return;
		for (int i = 0; i < listeners.size(); i++) {
			((FocusListener) listeners.elementAt(i)).limitIsMoving(this, n, currValue);
		}
	}

	/**
	* Sets whether the Focuser has vertical or horizontal orientation.
	*/
	public void setIsVertical(boolean value) {
		isVertical = value;
	}

	/**
	* Sets whether the Focuser is used for querying or for zooming.
	*/
	public void setIsUsedForQuery(boolean value) {
		usedForQuery = value;
	}

	public boolean getIsUsedForQuery() {
		return usedForQuery;
	}

	/**
	* Sets the number of the numeric attribute the value range of which is
	* to be delimited through the Focuser.
	* The attribute number is not used inside the class; may be useful for
	* identification when there are several Focusers.
	*/
	public void setAttributeNumber(int n) {
		attrN = n;
	}

	public int getAttributeNumber() {
		return attrN;
	}

	/**
	* Sets the absolute minimum and maximum values of the attribute
	*/
	public void setAbsMinMax(double min, double max) {
		if (Double.isNaN(min)) {
			min = absMin;
		}
		if (Double.isNaN(max)) {
			max = absMax;
		}
		if (Formats.areEqual(min, absMin) && Formats.areEqual(max, absMax))
			return;
		absMin = min;
		absMax = max;
		minMaxToStrings();
		if (Double.isNaN(currMin) || currMin < absMin) {
			currMin = absMin;
		}
		if (Double.isNaN(currMax) || currMax > absMax) {
			currMax = absMax;
		}
		tryGetCurrTimeInterval();
		setTextsInTextFields();
	}

	/**
	 * Sets values of minStr and maxStr, which represent the absolute minimum and
	 * maximum as strings
	 */
	protected void minMaxToStrings() {
		if (minTime != null) {
			minStr = minTime.toString();
		} else {
			minStr = StringUtil.doubleToStr(absMin, absMin, absMax);
		}
		if (maxTime != null) {
			maxStr = maxTime.toString();
		} else {
			maxStr = StringUtil.doubleToStr(absMax, absMin, absMax);
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
		if (minTime != null && maxTime != null) {
			currMinTime = minTime.getCopy();
			currMaxTime = maxTime.getCopy();
			minMaxToStrings();
			setTextsInTextFields();
		}
	}

	/**
	 * If the attribute is temporal, transforms the numeric values of currMin and
	 * currMax into time moments currMinTime and currMaxTime.
	 */
	protected void tryGetCurrTimeInterval() {
		/*
		System.out.println("tryGetCurrTimeInterval: minTime = "+minTime+
		    "; maxTime = "+maxTime);
		*/
		if (minTime == null || maxTime == null)
			return;
		if (Double.isNaN(currMin) || Double.isNaN(currMax))
			return;
		currMinTime = minTime.valueOf((long) currMin);
		currMaxTime = maxTime.valueOf((long) currMax);
		//System.out.println("currMinTime = "+currMinTime.toString());
		//System.out.println("currMaxTime = "+currMaxTime.toString());
	}

	/**
	* Sets the current minimum and maximum values of the attribute
	* (possibly, delimited)
	*/
	public void setCurrMinMax(double min, double max) {
		if (Double.isNaN(min)) {
			min = absMin;
		}
		if (Double.isNaN(max)) {
			max = absMax;
		}
		if (Formats.areEqual(min, currMin) && Formats.areEqual(max, currMax))
			return;
		currMin = min;
		currMax = max;
		if (currMin < absMin) {
			currMin = absMin;
		}
		if (currMax > absMax) {
			currMax = absMax;
		}
		tryGetCurrTimeInterval();
		setTextsInTextFields();
		notifyFocusChange();
	}

	/**
	* Sets the absolute and current minimum and maximum values of the attribute
	*/
	public void setMinMax(double amin, double amax, double cmin, double cmax) {
		absMin = amin;
		absMax = amax;
		currMin = cmin;
		currMax = cmax;
		if (currMin < absMin) {
			currMin = absMin;
		}
		if (currMax > absMax) {
			currMax = absMax;
		}
		tryGetCurrTimeInterval();
		minMaxToStrings();
		setTextsInTextFields();
	}

	/**
	* Calculates the attribute value corresponding to the position
	* pos on the axis. Depends on the length of the axis. Does not depend on the
	* current focus: the axis of a Focuser is not zoomed.
	*/
	protected double pos2Value(int pos) {
		if (length <= 0 || Double.isNaN(absMin) || Double.isNaN(absMax))
			return Double.NaN;
		if (isVertical) {
			pos = y0 - pos;
		} else {
			pos -= x0; //position relative to axis origin
		}
		return absMin + (absMax - absMin) * pos / length;
	}

	/**
	* Calculates the position on the axis corresponding to the given attribute
	* value. Depends on the length of the axis. Does not depend on the
	* current focus: the axis of a Focuser is not zoomed.
	*/
	protected int value2Pos(double value) {
		if (length <= 0 || Double.isNaN(absMin) || Double.isNaN(absMax) || Double.isNaN(value))
			return Integer.MIN_VALUE;
		int pos = (int) Math.round((value - absMin) * length / (absMax - absMin));
		//position relative to axis origin
		if (isVertical)
			return y0 - pos;
		return x0 + pos;
	}

	/**
	* Sets the origin and the length of the axis the focuser should be aligned
	* with. The owner of the Focuser should care about sufficient space for
	* drawing delimiters left (below) the origin of the axis and right (above)
	* its end. For this purpose it may use the method getRequiredMargin()
	*/
	public void setAlignmentParameters(int x, int y, int length) {
		x0 = x;
		y0 = y;
		this.length = length;
	}

	/**
	* "left" indicates whether the Focuser is placed on the left or on the right of
	* the axis it is aligned with (for the horisontal case, above and
	* below, respectively).
	*/
	public void setIsLeft(boolean left) {
		leftAligned = left;
	}

	/**
	* Sets whether the Focuser should draw texts (minimum and maximum)
	*/
	public void setTextDrawing(boolean value) {
		toDrawTexts = value;
	}

	/**
	* Sets the spacing between the Focuser and the axis it is aligned with.
	*/
	public void setSpacingFromAxis(int spacing) {
		space = spacing;
	}

	/**
	* Returns the required width (for a horizontal focuser this is actually the
	* height).
	* The width includes the place for showing minimum and maximum values.
	* Using the Graphics (more exactly, its FontMetrics), calculates how much
	* place is needed for the texts.
	*/
	public int getRequiredWidth(Graphics g) {
		if (width > mainWidth)
			return width;
		if (!toDrawTexts || g == null || minStr == null || maxStr == null)
			return mainWidth;
		FontMetrics fm = g.getFontMetrics();
		if (!isVertical) {
			width = mainWidth + fm.getAscent();
		} else {
			int l1 = fm.stringWidth(minStr), l2 = fm.stringWidth(maxStr);
			if (l1 > l2) {
				width = mainWidth + l1;
			} else {
				width = mainWidth + l2;
			}
		}
		return width;
	}

	/**
	* The focuser should know about the background color of the canvas it
	* is drawn in.
	*/
	public void setBkgColor(Color color) {
		bkgColor = color;
	}

	@Override
	public void setBounds(Rectangle bounds) {
	}

	/**
	* The focuser should know about the color of the plot area of the graphics
	* it is aligned with.
	*/
	public void setPlotAreaColor(Color color) {
		plotAreaColor = color;
	}

	/**
	*  Used to update focuser canvas if values of absmin or absmax changed
	*/
	public void refresh() {
		if (canvas != null) {
			canvas.repaint();
		}
	}

	@Override
	public void draw(Graphics g) {
		if (minDelim == null)
			if (isVertical) {
				minDelim = new Rectangle(0, 0, delimiterH + 1, delimiterW + 1);
			} else {
				minDelim = new Rectangle(0, 0, delimiterW + 1, delimiterH + 1);
			}
		if (maxDelim == null)
			if (isVertical) {
				maxDelim = new Rectangle(0, 0, delimiterH + 1, delimiterW + 1);
			} else {
				maxDelim = new Rectangle(0, 0, delimiterW + 1, delimiterH + 1);
			}
		if (minmaxDelim == null)
			if (isVertical) {
				minmaxDelim = new Rectangle(0, 0, delimiterH + 1, delimiterW + 1);
			} else {
				minmaxDelim = new Rectangle(0, 0, delimiterW + 1, delimiterH + 1);
			}
		determineDelimiterPositions();
		paintBackground(g);
		drawAxis(g);
		drawMinMax(g);
		drawCurrMinMax(g);
	}

	protected void determineDelimiterPositions() {
		minPos = value2Pos(currMin);
		maxPos = value2Pos(currMax);
	}

	protected void paintBackground(Graphics g) {
		int x = 0, y = 0, w = 0, h = 0;
		if (isVertical) {
			x = (leftAligned) ? x0 - space - width : x0 + space + 1;
			w = width + 1;
			y = y0 - length - delimiterW + 1;
			h = length + delimiterW * 2 + 1;

		} else {
			x = x0 - delimiterW - 1;
			w = length + delimiterW * 2 + 2;
			y = (leftAligned) ? y0 - space - width : y0 + space;
			h = width;
		}
		g.setColor(bkgColor);
		g.fillRect(x, y, w, h);

	}

	protected void drawAxis(Graphics g) {
		if (isVertical) { // Is Vertical
			if (minPos > y0) {
				minPos = y0;
			}
			if (maxPos < y0 - length) {
				maxPos = y0 - length;
			}
			//the position of the axis
			int x = getAxisPosition();
			//background
			int ys = y0 - length;
			int x1 = (leftAligned) ? x - halfDH : x, w = halfDH + 1;
			if (canvas != null) {
				if (leftAligned) {
					x1 = 0;
					w = x - x1 + 1;
				} else {
					w = canvas.getSize().width - x1 + 1;
				}
			}
			g.setColor(bkgColor);
			g.fillRect((leftAligned) ? x0 - space - mainWidth : x0 + space + 1, ys - delimiterW, mainWidth + 1, length + 2 * delimiterW + 1);
			g.fillRect(x1, ys, w, length + 1);
			X[0] = x;
			Y[0] = maxPos;
			X[1] = (leftAligned) ? x0 - space : x0 + space + 1;
			Y[1] = (usedForQuery) ? maxPos : ys;
			X[2] = X[1];
			Y[2] = (usedForQuery) ? minPos : y0;
			X[3] = X[0];
			Y[3] = minPos;
			g.setColor(plotAreaColor);
			g.fillPolygon(X, Y, 4);
			g.fillRect(x1, maxPos, w, minPos - maxPos + 1);
			g.setColor(Color.blue.darker());
			g.drawLine(X[0], Y[0], X[1], Y[1]);
			g.drawLine(X[2], Y[2], X[3], Y[3]);
			//the axis
			g.setColor(Color.black);
			g.drawLine(x, y0, x, y0 - length); //the vertical axis is oriented upwards!
			g.drawLine(x - mm, y0, x + mm, y0);
			g.drawLine(x - mm, y0 - length, x + mm, y0 - length);
			//the top triangle delimiter
			g.setColor(Color.blue.darker());
			if (!single.equalsIgnoreCase("top")) {
				X[0] = x;
				Y[0] = minPos;
				X[1] = x - halfDH;
				Y[1] = minPos + delimiterW;
				X[2] = X[1] + delimiterH;
				Y[2] = Y[1];
				X[3] = X[0];
				Y[3] = Y[0];
				g.setColor(Color.blue.darker());
				g.fillPolygon(X, Y, 4);
				g.drawPolygon(X, Y, 4);
				minDelim.x = X[1];
				minDelim.y = Y[0];
				minmaxDelim.x = X[1];

			}
			//the bottom triangle delimiter
			if (!single.equalsIgnoreCase("bottom")) {
				X[0] = x;
				Y[0] = maxPos;
				X[1] = x - halfDH;
				Y[1] = maxPos - delimiterW;
				X[2] = X[1] + delimiterH;
				Y[2] = Y[1];
				X[3] = X[0];
				Y[3] = Y[0];
				g.fillPolygon(X, Y, 4);
				g.drawPolygon(X, Y, 4);
				maxDelim.x = X[1];
				maxDelim.y = Y[1];
				minmaxDelim.y = maxDelim.y + maxDelim.height - 1;
			}
			minmaxDelim.height = minDelim.y - maxDelim.y - maxDelim.height;
		} else { // Is Horisontal
			//the position of the axis
			int y = getAxisPosition();
			//background
			g.setColor(bkgColor);
			g.fillRect(x0 - delimiterW - 1, (leftAligned) ? y0 - space - mainWidth : y0 + space + 1, length + 2 * delimiterW + 2, mainWidth);
			X[0] = minPos;
			Y[0] = y;
			X[1] = (usedForQuery) ? minPos : x0;
			Y[1] = (leftAligned) ? y0 - space : y0 + space + 1;
			X[2] = (usedForQuery) ? maxPos : x0 + length;
			Y[2] = Y[1];
			X[3] = maxPos;
			Y[3] = Y[0];
			g.setColor(plotAreaColor);
			g.fillPolygon(X, Y, 4);
			g.fillRect(minPos, (leftAligned) ? y - halfDH : y, maxPos - minPos + 1, halfDH + 1);
			g.setColor(Color.blue.darker());
			g.drawLine(X[0], Y[0], X[1], Y[1]);
			g.drawLine(X[2], Y[2], X[3], Y[3]);
			//the axis
			g.setColor(Color.black);
			g.drawLine(x0, y, x0 + length, y);
			g.drawLine(x0, y - mm, x0, y + mm);
			g.drawLine(x0 + length, y - mm, x0 + length, y + mm);
			//the left triangle delimiter
			if (minPos < x0) {
				minPos = x0;
			}
			if (!single.equalsIgnoreCase("right")) {
				X[0] = minPos;
				Y[0] = y;
				X[1] = minPos - delimiterW;
				Y[1] = y - halfDH;
				X[2] = X[1];
				Y[2] = Y[1] + delimiterH;
				X[3] = X[0];
				Y[3] = Y[0];
				g.setColor(Color.blue.darker());
				g.fillPolygon(X, Y, 4);
				g.drawPolygon(X, Y, 4);
				minDelim.x = X[1];
				minDelim.y = Y[1];
				minmaxDelim.y = Y[1];
			}
			//the right triangle delimiter
			if (maxPos > x0 + length) {
				maxPos = x0 + length;
			}
			if (!single.equalsIgnoreCase("left")) {
				X[0] = maxPos;
				Y[0] = y;
				X[1] = maxPos + delimiterW;
				Y[1] = y - halfDH;
				X[2] = X[1];
				Y[2] = Y[1] + delimiterH;
				X[3] = X[0];
				Y[3] = Y[0];
				g.fillPolygon(X, Y, 4);
				g.drawPolygon(X, Y, 4);
				maxDelim.x = X[0];
				maxDelim.y = Y[1];
				minmaxDelim.x = minDelim.x + maxDelim.width - 1;
				minmaxDelim.width = maxDelim.x - minDelim.x - minDelim.width;
			}
		}
	}

	protected void drawMinMax(Graphics g) {
		if (!toDrawTexts)
			return;
		FontMetrics fm = g.getFontMetrics();
		int asc = fm.getAscent();
		if (isVertical) {
			//the positions of the texts
			int x = (leftAligned) ? x0 - space - mainWidth : x0 + space + mainWidth + 1;
			g.setColor(Color.black);
			g.drawString(minStr, (leftAligned) ? x - fm.stringWidth(minStr) : x, y0);
			g.drawString(maxStr, (leftAligned) ? x - fm.stringWidth(maxStr) : x, y0 - length + asc);
		} else {
			//the positions of the texts
			int y = (leftAligned) ? y0 - space - mainWidth - asc : y0 + space + mainWidth;
			g.setColor(Color.black);
			g.drawString(minStr, x0, y + asc);
			g.drawString(maxStr, x0 + length - fm.stringWidth(maxStr), y + asc);
		}
	}

	protected String prevMinStr = null, prevMaxStr = null;

	protected void drawCurrMinMax(Graphics g) {
		if (!toDrawTexts || !toDrawCurrMinMax)
			return;
		FontMetrics fm = g.getFontMetrics();
		int asc = fm.getAscent();
		if (isVertical) {
			//the positions of the texts
			int x = (leftAligned) ? x0 - space - mainWidth : x0 + space + mainWidth + 1;
			g.setColor(Color.white);
			if (prevMinStr != null && MinDelimMoving()) {
				g.drawString(prevMinStr, (leftAligned) ? x - fm.stringWidth(minStr) : x, y0 + asc - 2 * asc);
			}
			if (prevMaxStr != null && MaxDelimMoving()) {
				g.drawString(prevMaxStr, (leftAligned) ? x - fm.stringWidth(minStr) : x, y0 - length + 2 * asc);
			}
			g.setColor(Color.blue);
			if (currMin != absMin) {
				if (currMinTime != null) {
					prevMinStr = currMinTime.toString();
				} else {
					prevMinStr = String.valueOf(currMin);
				}
				g.drawString(prevMinStr, (leftAligned) ? x - fm.stringWidth(minStr) : x, y0 + asc - 2 * asc);
			}
			if (currMax != absMax) {
				if (currMaxTime != null) {
					prevMaxStr = currMaxTime.toString();
				} else {
					prevMaxStr = String.valueOf(currMax);
				}
				g.drawString(prevMaxStr, (leftAligned) ? x - fm.stringWidth(maxStr) : x, y0 - length + 2 * asc);
			}
		} else {
			/*
			//the positions of the texts
			int y=(leftAligned)?y0-space-mainWidth-asc:y0+space+mainWidth;
			g.setColor(Color.black);
			g.drawString(minStr,x0,y+asc);
			g.drawString(maxStr,x0+length-fm.stringWidth(maxStr),y+asc);
			*/
		}
	}

	/**
	* Returns the position of its lower delimiter
	*/
	public int getMinPos() {
		return value2Pos(currMin);
	}

	/**
	* Returns the position of its upper delimiter
	*/
	public int getMaxPos() {
		return value2Pos(currMax);
	}

	public boolean getIsVertical() {
		return isVertical;
	}

	/**
	* Returns the position of the axis (y if horizontal, x if vertical)
	*/
	public int getAxisPosition() {
		if (isVertical)
			return (leftAligned) ? x0 - space - mainWidth + halfDH : x0 + space + mainWidth - halfDH;
		return (leftAligned) ? y0 - space - mainWidth + halfDH : y0 + space + mainWidth - halfDH;
	}

	/**
	* Reports whether the point (x,y) (e.g. the mouse position) is within one of
	* the delimiters.
	*/
	public boolean isPointOnDelimiter(int x, int y) {
		return minDelim.contains(x, y) || maxDelim.contains(x, y) || minmaxDelim.contains(x, y);
	}

	/**
	* Reports whether the mouse is captured, i.e. the Focuser interprets
	* mouse dragging events as movement of one of the delimiters
	*/
	public boolean isMouseCaptured() {
		return minDelimMoving || maxDelimMoving;
	}

	/**
	* Makes the Focuser capture the mouse, i.e. start interpreting
	* mouse dragging events as movement of one of the delimiters.
	* (x,y) indicate the current mouse position
	* Returns true if the mouse is captured (the mouse should point at one of
	* the delimiters)
	*/
	public boolean captureMouse(int x, int y) {
		if (isMouseCaptured())
			return true;
		// minmaxPos: distance from minPos in direction to maxPos
		if (minmaxDelim.contains(x, y)) {
			minmaxPos = (isVertical) ? (minPos - maxPos) - (y - minmaxDelim.y) : x - minmaxDelim.x;
		} else {
			minmaxPos = 0;
		}
		if (minDelim.contains(x, y) || minmaxDelim.contains(x, y)) {
			minDelimMoving = true;
		}
		if (maxDelim.contains(x, y) || minmaxDelim.contains(x, y)) {
			maxDelimMoving = true;
		} // return true; }
		return isPointOnDelimiter(x, y);
		// return false;
	}

	/**
	* Makes the Focuser release the mouse and stop interpreting
	* mouse dragging events as movement of one of the delimiters.
	* (x,y) indicate the current mouse position
	*/
	public void releaseMouse() {
		boolean changed = false;
		if (minDelimMoving) {
			//int pos=value2Pos(currMin);
			//if (pos!=minPos) {
			currMin = pos2Value(minPos);
			changed = true;
			//}
			minDelimMoving = false;
		}
		if (maxDelimMoving) {
			//int pos=value2Pos(currMax);
			//if (pos!=maxPos) {
			currMax = pos2Value(maxPos);
			changed = true;
			//}
			maxDelimMoving = false;
		}
		prevMin = Double.NaN;
		prevMax = Double.NaN;
		if (changed) {
			notifyFocusChange();
		}
	}

	/**
	* Called by the owner of the Focuser when the mouse is dragged in the
	* Focuser area (if the mouse has been captured by this Focuser before)
	* The arguments x and y indicate the actual position of the mouse.
	* Passing of the Graphics allows the Focuser to redraw itself.
	*/
	public void mouseDragged(int x, int y, Graphics g) {
		if (!isMouseCaptured())
			return;
		if (Double.isNaN(prevMin)) {
			prevMin = currMin;
		}
		if (Double.isNaN(prevMax)) {
			prevMax = currMax;
		}
		int pos = (isVertical) ? y0 - y : x - x0; //position relative to axis origin
		if (minmaxPos == 0) {
			if (pos < 0) {
				pos = 0;
			}
			if (pos > length) {
				pos = length;
			}
		} else if (isVertical) {
			if (pos - minmaxPos < 0) {
				pos = minmaxPos;
			}
			if (pos + (minPos - maxPos - minmaxPos) > length) {
				pos = length - (minPos - maxPos - minmaxPos);
			}
		} else {
			if (pos - minmaxPos < 0) {
				pos = minmaxPos;
			}
			if (pos + (maxPos - minPos - minmaxPos) > length) {
				pos = length - (maxPos - minPos - minmaxPos);
			}
		}
		int minRP = (isVertical) ? y0 - minPos : minPos - x0, maxRP = (isVertical) ? y0 - maxPos : maxPos - x0;
		//positions of the minimum and maximum delimiters relative to axis origin
		if (minDelimMoving && maxDelimMoving) {
			if (isVertical) {
				minPos = y0 - pos + minmaxPos;
				maxPos = minPos - (maxRP - minRP);
			} else {
				minPos = pos + x0 - minmaxPos;
				maxPos = minPos + (maxRP - minRP);
			}
			currMin = pos2Value(minPos);
			currMax = pos2Value(maxPos);
		} else if (minDelimMoving) {
			if (maxRP - pos < mm) {
				pos = maxRP - mm;
			}
			if (pos == minRP)
				return;
			minPos = (isVertical) ? y0 - pos : pos + x0;
			currMin = pos2Value(minPos);
		} else if (maxDelimMoving) {
			if (pos - minRP < mm) {
				pos = minRP + mm;
			}
			if (pos == maxRP)
				return;
			maxPos = (isVertical) ? y0 - pos : pos + x0;
			currMax = pos2Value(maxPos);
		}
		tryGetCurrTimeInterval();
		drawAxis(g);
		drawMinMax(g);
		drawCurrMinMax(g);
		if (minDelimMoving) {
			notifyLimitMoving(0, pos2Value(minPos));
		}
		if (maxDelimMoving) {
			notifyLimitMoving(1, pos2Value(maxPos));
		}
	}

	/**
	* Sets the Text fields for exact specification of minimum and maximum limits
	*/
	public void setTextFields(TextField minField, TextField maxField) {
		if (minTF != null) {
			minTF.removeActionListener(this);
		}
		if (maxTF != null) {
			maxTF.removeActionListener(this);
		}
		minTF = minField;
		maxTF = maxField;
		if (minTF != null) {
			minTF.addActionListener(this);
		}
		if (maxTF != null) {
			maxTF.addActionListener(this);
		}
	}

	/**
	* Reaction to action events from the text fields in which minimum and
	* maximum values can be specified
	*/
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == minTF || e.getSource() == maxTF) {
			double min = currMin, max = currMax;
			String str = (minTF == null) ? "" : minTF.getText().trim();
			boolean changed = false;
			if (str.length() > 0) {
				boolean got = false;
				if (minTime != null) {
					TimeMoment t = minTime.getCopy();
					got = t.setMoment(str);
					if (got) {
						if (t.compareTo(minTime) < 0) {
							t = minTime.getCopy();
						} else if (t.compareTo(maxTime) >= 0) {
							t = maxTime.getPrevious();
						}
						if (t.compareTo(currMinTime) != 0) {
							changed = true;
							min = t.toNumber();
						}
					}
				}
				if (!got) {
					try {
						min = Double.valueOf(str).doubleValue();
						if (min < absMin) {
							min = absMin;
						}
						if (min != currMin) {
							changed = true;
						}
					} catch (NumberFormatException nfe) {
					}
				}
			}
			str = (maxTF == null) ? "" : maxTF.getText().trim();
			if (str.length() > 0) {
				boolean got = false;
				if (maxTime != null) {
					TimeMoment t = maxTime.getCopy();
					got = t.setMoment(str);
					if (got) {
						if (t.compareTo(minTime) <= 0) {
							t = minTime.getNext();
						} else if (t.compareTo(maxTime) > 0) {
							t = maxTime.getCopy();
						}
						if (t.compareTo(currMaxTime) != 0) {
							changed = true;
							max = t.toNumber();
						}
					}
				}
				if (!got) {
					try {
						max = Double.valueOf(str).doubleValue();
						if (max > absMax || max <= min) {
							max = absMax;
						}
						if (max != currMax) {
							changed = true;
						}
					} catch (NumberFormatException nfe) {
					}
				}
			}
			if (!changed || min >= max) {
				setTextsInTextFields();
				return;
			}
			//currMin=min; currMax=max;
			setCurrMinMax(min, max);
			if (canvas != null) {
				Graphics g = canvas.getGraphics();
				draw(g);
				g.dispose();
			}
			//notifyFocusChange();
		}
	}

	protected void setTextsInTextFields() {
		if (minTF != null)
			if (currMinTime != null) {
				minTF.setText(currMinTime.toString());
			} else {
				minTF.setText(StringUtil.doubleToStr(currMin, absMin, absMax, false));
			}
		if (maxTF != null)
			if (currMaxTime != null) {
				maxTF.setText(currMaxTime.toString());
			} else {
				maxTF.setText(StringUtil.doubleToStr(currMax, absMin, absMax, true));
			}
	}

	@Override
	public void destroy() {
		if (minTF != null) {
			minTF.removeActionListener(this);
		}
		if (maxTF != null) {
			maxTF.removeActionListener(this);
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

	/**
	 * Set single delimiter
	 * @param single = "left" or "right", null - two delimiters (default)
	 */
	public void setSingleDelimiter(String single) {
		if (single == null) {
			this.single = null;
			return;
		}
		if (single.equalsIgnoreCase("left") || single.equalsIgnoreCase("right") || single.equalsIgnoreCase("bottom") || single.equalsIgnoreCase("top")) {
			this.single = single;
		}
	}

}
