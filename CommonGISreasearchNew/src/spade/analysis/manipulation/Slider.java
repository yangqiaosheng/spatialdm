package spade.analysis.manipulation;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import spade.lib.basicwin.Drawable;
import spade.lib.color.CS;
import spade.lib.color.ColorBrewer.Schemes;
import spade.lib.util.Aligner;
import spade.lib.util.DoubleArray;
import spade.lib.util.IntArray;
import spade.lib.util.StringUtil;
import spade.time.TimeMoment;

/**
* A Slider is a component that allows the user to break interactively the
* value range of a numeric attribute into intervals. This component may
* also be used for setting of a reference value for visual comparison.
* For this purpose the internal variable maxNBreaks should be set to 1 using
* the method setMaxNBreaks(int).
*/
public class Slider implements Drawable, MouseListener, MouseMotionListener, PropertyChangeListener, ActionListener {
	public static final int mm = Math.round(java.awt.Toolkit.getDefaultToolkit().getScreenResolution() / 25.33f);
	public static final int width = 4 * mm, arrowW = mm, arrowW2 = arrowW / 2, arrowH = mm, minDist = arrowW;
	public static Color hideColor = Color.gray;
	protected static int X[] = new int[4], Y[] = new int[4];
	protected boolean isHorisontal = true;
	private boolean otherCursor = false;
	/**
	* Predefined colors used for classification
	* if it set, all other color-related variables are ignored
	*/
	protected Color colors[] = null;
	/**
	* Color for the "middle" class in dichromatic color scale
	*/
	protected Color middleColor = Color.white;//new Color(255,255,128);
	/**
	* The canvas in which this object should draw itself
	*/
	protected Canvas canvas = null;
	/**
	* The boundaries in which the object should fit itself
	* origBounds are used in case of alignment with other garphics
	*/
	protected Rectangle bounds = null, origBounds = null;
	/**
	* Represented numeric interval and the "zero" value (in world units)
	* The "zero" value is used for producing bichromatic color scales.
	*/
	protected double min = Double.NaN, max = Double.NaN, cmpTo = 0.0;
	/**
	* Limits imposed by focusing. May have an impact on the appearance of the slider.
	*/
	protected double focusMin = Double.NaN, focusMax = Double.NaN;
	/**
	 * If the attribute is temporal, these fields contain the absolute minimum and
	 * maximum time moments.
	 */
	protected TimeMoment minTime = null, maxTime = null;
	/**
	 * If the attribute is temporal, these fields contain the current minimum and
	 * maximum time moments (e.g. constrained by a focuser).
	 */
	protected TimeMoment currMinTime = null, currMaxTime = null;
	/**
	* Positions of the sliders (in world units)
	*/
	protected DoubleArray breaks = new DoubleArray(10, 10);
	/**
	* Maximum number of breaks
	*/
	protected int maxNBreaks = 100;
	/**
	* Numbers of classes to paint in gray
	*/
	protected IntArray hiddenClasses = new IntArray(10, 10);
	/**
	* Screen coordinates in that the represented interval is mapped
	*/
	protected int x0 = 0, x1 = 0;
	/**
	* Sliders positions in screen coordinates
	*/
	protected IntArray slPos = new IntArray(10, 10);
	/**
	* Hues for bichromatic color scale
	* is some of hues <0, color band is not painted (remains white)
	*/
	protected float posHue = 0.0f, negHue = 0.6f;
	/**
	* Indicates whether a continuous color scale should be drawn in the background.
	* Makes sense only when there is no more than one slider
	*/
	protected boolean useShades = false;
	/**
	* Listeners of slider events.
	*/
	protected Vector listeners = null;
	/**
	* Aligner is used to align horisontally or vertically several plots
	*/
	protected Aligner aligner = null;
	/**
	* The text field in which the user can specify exact values of the breaks.
	* May be absent. When the textfield is present, the slider 1) writes to it
	* the current break values; 2) listens to action events from it and
	* sets the slider(s) in the corresponding position(s).
	*/
	protected TextField breakTF = null;
	/**
	* Indicates "destroyed" state. Initially is false.
	*/
	protected boolean destroyed = false;

	/**
	* Sets whether a continuous color scale should be drawn in the background.
	* Makes sense only when there is no more than one slider
	*/
	public void setUseShades(boolean value) {
		useShades = value;
	}

	/**
	* Sets the predefined colors to be used for classification
	* If the colors are specified, all other color-related variables are ignored
	*/
	public void setColors(Color colors[]) {
		this.colors = colors;
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

	/**
	* Sets the orientation of the slider
	*/
	public void setIsHorisontal(boolean value) {
		isHorisontal = value;
	}

	/**
	* Sets the canvas in which this object should draw itself
	*/
	@Override
	public void setCanvas(Canvas c) {
		canvas = c;
		if (canvas != null) {
			canvas.addMouseListener(this);
			canvas.addMouseMotionListener(this);
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
			fillTextField();
		}
	}

	/**
	* The text field in which the user can specify exact values of the breaks.
	* When the textfield is given to the slider, the slider 1) writes to it
	* the current break values; 2) listens to action events from it and
	* sets the slider(s) in the corresponding position(s).
	*/
	public void setTextField(TextField tf) {
		if (breakTF != null) {
			breakTF.removeActionListener(this);
		}
		breakTF = tf;
		if (breakTF != null) {
			breakTF.addActionListener(this);
		}
		fillTextField();
	}

	/**
	* Shows in the text field linked to this slider current break values.
	*/
	public void fillTextField() {
		if (breakTF != null) {
			String str = "";
			for (int i = 0; i < getNBreaks(); i++) {
				if (i > 0) {
					str += " ";
				}
				if (minTime == null) {
					str += StringUtil.doubleToStr(getBreakValue(i), getMin(), getMax());
				} else if (maxNBreaks == 1) {
					str += minTime.valueOf((long) getBreakValue(i)).toString();
				} else {
					str += "\"" + minTime.valueOf((long) getBreakValue(i)).toString() + "\"";
				}
			}
			breakTF.setText(str);
		}
	}

	@Override
	public Dimension getPreferredSize() {
		return (isHorisontal) ? new Dimension(30 * mm, width + 2 * arrowH) : new Dimension(width + 2 * arrowH, 30 * mm);
	}

	/**
	* Sets boundaries in which the object should fit itself
	*/
	@Override
	public void setBounds(Rectangle bounds) {
		this.bounds = bounds;
		if (aligner != null) {
			origBounds = new Rectangle(bounds);
			if (isHorisontal)
				if (aligner.getLeft() >= 0 && aligner.getRight() >= 0) {
					bounds.x += aligner.getLeft();
					bounds.width -= aligner.getLeft() + aligner.getRight();
				} else {
					;
				}
			else if (aligner.getTop() >= 0 && aligner.getBottom() >= 0) {
				bounds.y += aligner.getTop();
				bounds.height -= aligner.getTop() + aligner.getBottom();
			}
		}
		if (bounds != null)
			if (isHorisontal) {
				x0 = bounds.x;
				x1 = x0 + bounds.width - 1;
			} else {
				x0 = bounds.y;
				x1 = bounds.y + bounds.height - 1;
			}
	}

	@Override
	public Rectangle getBounds() {
		return bounds;
	}

	public boolean containsPoint(int x, int y) {
		if (bounds == null)
			return false;
		return bounds.contains(x, y);
	}

	/**
	* Sets minimum and maximum values (the numeric interval to be represented)
	*/
	public void setMinMax(double min, double max) {
		setMinMaxCmp(min, max, cmpTo);
	}

	/**
	* Sets minimum and maximum values (the numeric interval to be represented)
	* and the midpoint
	*/
	public void setMinMaxCmp(double min, double max, double midp) {
		if (min == this.min && max == this.max && cmpTo == midp)
			return;
		this.min = min;
		this.max = max;
		cmpTo = midp;
		focusMin = min;
		focusMax = max;
		if (minTime != null && maxTime != null) {
			minTime = minTime.valueOf((long) min);
			maxTime = maxTime.valueOf((long) max);
			currMinTime = minTime.getCopy();
			currMaxTime = maxTime.getCopy();
		}
		for (int i = getNBreaks() - 1; i >= 0; i--)
			if (breaks.elementAt(i) <= min || breaks.elementAt(i) >= max) {
				removeBreak(i);
			}
		if (maxNBreaks == 1 && breaks.size() == 0 && cmpTo > min && cmpTo < max) {
			addBreak(cmpTo);
		}
		redraw();
		notifyBreaksChange();
	}

	public double getMin() {
		return min;
	}

	public double getMax() {
		return max;
	}

	/**
	* Sets the limits imposed by focusing. May have an impact on the appearance
	* of the slider.
	*/
	public void setFocusMinMax(double lowLimit, double upLimit) {
		focusMin = lowLimit;
		focusMax = upLimit;
		if (minTime != null && maxTime != null) {
			currMinTime = minTime.valueOf((long) focusMin);
			currMaxTime = maxTime.valueOf((long) focusMax);
		}
		redraw();
	}

	/**
	* Sets the "zero" value (in world units)
	* The "zero" value is used for producing bichromatic color scales.
	*/
	public void setMidPoint(double f) {
		cmpTo = f;
		if (maxNBreaks == 1)
			if (getNBreaks() < 1) {
				addBreak(cmpTo);
			} else {
				setBreakValue(f, 0);
			}
	}

	/**
	* Returns the "zero" value (in world units)
	* The "zero" value is used for producing bichromatic color scales.
	*/
	public double getMidPoint() {
		return cmpTo;
	}

	/**
	* Sets the maximum number of breaks
	*/
	public void setMaxNBreaks(int nbr) {
		maxNBreaks = nbr;
	}

	public int getMaxNBreaks() {
		return maxNBreaks;
	}

	/**
	* Adds a break (slider) corresponding to the given number
	*/
	public void addBreak(double value) {
		if (maxNBreaks == 1 && (value < min || value > max))
			return;
		if (maxNBreaks > 1 && (value <= min || value >= max))
			return;
		if (maxNBreaks > 1 && maxNBreaks == breaks.size())
			return;
		boolean inserted = false;
		for (int i = 0; i < breaks.size() && !inserted; i++)
			if (breaks.elementAt(i) == value)
				return;
			else if (breaks.elementAt(i) > value) {
				breaks.insertElementAt(value, i);
				slPos.insertElementAt(abs2Scr(value), i);
				inserted = true;
			}
		if (!inserted) {
			breaks.addElement(value);
			slPos.addElement(abs2Scr(value));
		}
		if (maxNBreaks == 1) {
			cmpTo = value;
		}
	}

	/**
	* Returns the current number of breaks
	*/
	public int getNBreaks() {
		return breaks.size();
	}

	/**
	* Returns the current breaks
	*/
	public DoubleArray getBreaks() {
		return breaks;
	}

	/**
	* Returns the value of the break with the given number
	*/
	public double getBreakValue(int n) {
		if (n < 0 || n >= getNBreaks())
			return Double.NaN;
		return breaks.elementAt(n);
	}

	/**
	* Sets the specified break to the specified number
	*/
	public void setBreakValue(double value, int n) {
		if (n < 0 || n >= getNBreaks())
			return;
		breaks.setElementAt(value, n);
		slPos.setElementAt(abs2Scr(value), n);
		if (maxNBreaks == 1) {
			cmpTo = value;
			/*
			System.out.println("setBreakValue: maxNBreaks="+maxNBreaks+", break N "+n+
			  " is set to "+value+", midPoint="+cmpTo);
			*/
		}
	}

	/**
	* Removes the break with the given number
	*/
	public void removeBreak(int n) {
		if (n < 0 || n >= getNBreaks())
			return;
		breaks.removeElementAt(n);
		slPos.removeElementAt(n);
	}

	public void removeAllBreaks() {
		breaks.removeAllElements();
		slPos.removeAllElements();
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
	* Operations to hide and expose classes
	*/
	public boolean isClassHidden(int classN) {
		return hiddenClasses.indexOf(classN) >= 0;
	}

	public void setClassIsHidden(boolean value, int classN) {
		int idx = hiddenClasses.indexOf(classN);
		if (value)
			if (idx < 0) {
				hiddenClasses.addElement(classN);
			} else {
				; //already hidden
			}
		else if (idx >= 0) {
			hiddenClasses.removeElementAt(idx);
		} else {
			; //not hidden
		}
	}

	public void exposeAllClasses() {
		hiddenClasses.removeAllElements();
	}

	/**
	* Gets the color with the given index
	*/
	public Color getColor(int idx) {
		if (colors != null && colors.length > idx)
			return colors[idx];
		if (idx < 0)
			return Color.gray;
		if (maxNBreaks == 1) //used for visual comparison
			if (cmpTo >= max)
				return (negHue >= 0) ? Color.getHSBColor(negHue, 1.0f, 1.0f) : Color.white;
			else if (idx == 1 || getNBreaks() < 1)
				return (posHue >= 0) ? Color.getHSBColor(posHue, 1.0f, 1.0f) : Color.white;
			else
				return (negHue >= 0) ? Color.getHSBColor(negHue, 1.0f, 1.0f) : Color.white;
		if (isClassHidden(idx))
			return hideColor;
		//check if this is a ColorBrewer scale
		boolean isInverted = posHue <= -1100;
		int cbScaleN = (posHue >= 0) ? -1 : -1000 - (int) posHue - ((isInverted) ? 100 : 0);
		if (cmpTo <= min) //monochromatic scale with the use of posHue
			return (cbScaleN >= 0) ? Schemes.getColor(0.5f + 0.5f * (idx + 1) / (getNBreaks() + 1), cbScaleN, isInverted) : CS.getColor((float) (idx + 1) / (getNBreaks() + 1), posHue);
		if (cmpTo >= max) //monochromatic scale with the use of negHue
			return (cbScaleN >= 0) ? Schemes.getColor(0.5f - 0.5f * (getNBreaks() - idx + 1) / (getNBreaks() + 1), cbScaleN, isInverted) : CS.getColor(((float) getNBreaks() - idx + 1) / (getNBreaks() + 1), negHue);
		//in what class the midpoint is?
		int mpc = -1;
		for (int i = 0; i < getNBreaks() && mpc < 0; i++)
			if (cmpTo < getBreakValue(i)) {
				mpc = i;
			}
		if (mpc < 0) {
			mpc = getNBreaks();
		}
		if (cbScaleN >= 0) {// ColorBrewer!
			if (idx == mpc)
				return Schemes.getColor(0.5f, cbScaleN, isInverted);
			return (Schemes.getColor((idx > mpc) ? 0.5f + 0.5f * ((float) idx - mpc) / (getNBreaks() - mpc) : 0.5f - 0.5f * ((float) mpc - idx) / mpc, cbScaleN, isInverted));
		} else {
			if (idx == mpc)
				return middleColor;
			if (idx < mpc)
				return (negHue >= 0) ? CS.getColor(((float) mpc - idx) / mpc, negHue) : Color.white;
			return (posHue >= 0) ? CS.getColor(((float) idx - mpc) / (getNBreaks() - mpc), posHue) : Color.white;
		}
	}

	/**
	* Transforms world units into screen coordinates
	*/
	protected int abs2Scr(double val) {
		if (x1 <= x0)
			return 0;
		return x0 + (int) Math.round((val - min) * (x1 - x0) / (max - min));
	}

	/**
	* Transforms screen coordinates into world units
	*/
	protected double scr2Abs(int x) {
		if (x < x0 || x > x1)
			return Double.NaN;
		return min + (x - x0) * (max - min) / (x1 - x0);
	}

	protected void countSliderPositions() {
		for (int i = 0; i < breaks.size(); i++) {
			int pos = abs2Scr(breaks.elementAt(i));
			if (i < slPos.size()) {
				slPos.setElementAt(pos, i);
			} else {
				slPos.addElement(pos);
			}
		}
	}

	/**
	* Draws the object in the given graphics.
	*/
	@Override
	public void draw(Graphics g) {
		if (g == null || bounds == null)
			return;
		countSliderPositions();
		//draw the background
		g.setColor((canvas == null) ? Color.white : canvas.getBackground());
		if (isHorisontal) {
			g.fillRect(bounds.x - 2, bounds.y, bounds.width + 1 + 4, arrowH + 1);
			g.fillRect(bounds.x - 2, bounds.y + bounds.height - arrowH - 1, bounds.width + 1 + 4, arrowH + 1);
		} else {
			g.fillRect(bounds.x, bounds.y - 2, arrowH + 1, bounds.height + 1 + 4);
			g.fillRect(bounds.x + bounds.width - arrowH - 1, bounds.y - 2, arrowH + 1, bounds.height + 1 + 4);
		}
		/*
		if (isHorisontal){
		  g.fillRect(bounds.x,bounds.y,bounds.width+1,arrowH+1);
		  g.fillRect(bounds.x,bounds.y+bounds.height-arrowH-1,bounds.width+1,arrowH+1);
		}
		else {
		  g.fillRect(bounds.x,bounds.y,arrowH+1,bounds.height+1);
		  g.fillRect(bounds.x+bounds.width-arrowH-1,bounds.y,arrowH+1,bounds.height+1);
		} */
		if (Double.isNaN(focusMin) || focusMin < min) {
			focusMin = min;
		}
		if (Double.isNaN(focusMax) || focusMax > max) {
			focusMax = max;
		}
		int xx0 = abs2Scr(focusMin), xx1 = abs2Scr(focusMax);
		g.setColor((canvas == null) ? Color.white : canvas.getBackground());
		if (xx0 > x0)
			if (isHorisontal) {
				g.fillRect(x0, bounds.y + arrowH, xx0 - x0, bounds.height - 2 * arrowH - 1);
			} else {
				g.fillRect(bounds.x + arrowH, bounds.y + bounds.height - xx0, bounds.width - 2 * arrowH - 1, xx0);
			}
		if (xx1 < x1)
			if (isHorisontal) {
				g.fillRect(xx1, bounds.y + arrowH, x1 - xx1, bounds.height - 2 * arrowH - 1);
			} else {
				g.fillRect(bounds.x + arrowH, bounds.y + bounds.height - x1, bounds.width - 2 * arrowH - 1, x1 - xx1 + x0);
			}
		//g.fillRect(bounds.x,bounds.y,bounds.width,bounds.height);
		if (useShades && slPos.size() <= 1) { //draw a continuous color scale
			int nSteps = (xx1 - xx0) / 3;
			double v = focusMin;
			int x = xx0 - x0;
			for (int i = 0; i < nSteps; i++) {
				double v1 = v + (focusMax - v) / (nSteps - i);
				if (v < cmpTo && v1 > cmpTo) {
					v1 = cmpTo;
				}
				if (posHue >= 0 && negHue >= 0) {
					g.setColor(CS.makeColor((float) focusMin, (float) focusMax, (float) v, (float) cmpTo, posHue, negHue));
				} else {
					g.setColor(Color.white);
				}
				int dx = abs2Scr(v1) - x - x0;
				if (v == cmpTo) {
					x -= 2;
					dx = 4;
					if (x < 0) {
						dx = x + dx;
						x = 0;
					}
					v1 = scr2Abs(x0 + x + dx);
				}
				if (x0 + x + dx > xx1) {
					dx = xx1 - x - x0;
				}
				if (dx > 0)
					if (isHorisontal) {
						g.fillRect(x0 + x, bounds.y + arrowH, dx + 1, bounds.height - 2 * arrowH - 1);
					} else {
						g.fillRect(bounds.x + arrowH, bounds.y + bounds.height - x - dx, bounds.width - 2 * arrowH - 1, dx + 1);
					}
				x += dx;
				v = v1;
			}
		} else {
			int x = xx0;
			for (int i = 0; i < slPos.size() + 1; i++) {
				int xx = (i < slPos.size()) ? slPos.elementAt(i) : xx1;
				if (xx > xx1) {
					xx = xx1;
				}
				if (xx <= x) {
					continue;
				}
				g.setColor(getColor(i));
				if (isHorisontal) {
					g.fillRect(x, bounds.y + arrowH, xx - x + 1, bounds.height - 2 * arrowH - 1);
				} else {
					int from = bounds.y + x1 - xx, h = xx - x;
					g.fillRect(bounds.x + arrowH, from, bounds.width - 2 * arrowH, h);
				}
				x = xx;
			}
		}
		g.setColor(Color.gray);
		if (isHorisontal) {
			g.drawRect(x0, bounds.y + arrowH, x1 - x0, bounds.height - 2 * arrowH - 1);
		} else {
			g.drawRect(bounds.x + arrowH, x0, bounds.width - 2 * arrowH - 1, x1 - x0);
		}
		//Arrows
		g.setColor(Color.black);
		for (int i = 0; i < slPos.size(); i++) {
			int x = slPos.elementAt(i);
			if (x >= x0 && x <= x1)
				if (isHorisontal) {
					g.drawLine(x, bounds.y + 1, x, bounds.y + bounds.height - 1);
					X[0] = x;
					Y[0] = bounds.y;
					X[1] = X[0] - arrowW2;
					Y[1] = Y[0] + arrowH;
					X[2] = X[1] + arrowW;
					Y[2] = Y[1];
					X[3] = X[0];
					Y[3] = Y[0];
					g.fillPolygon(X, Y, 3);
					g.drawPolygon(X, Y, 4);
					Y[0] = bounds.y + bounds.height - 1;
					Y[1] = Y[0] - arrowH;
					Y[2] = Y[1];
					Y[3] = Y[0];
					g.fillPolygon(X, Y, 3);
					g.drawPolygon(X, Y, 4);
				} else {
					X[0] = bounds.x;
					Y[0] = bounds.y + x1 - x;
					X[1] = X[0] + arrowH;
					Y[1] = Y[0] - arrowW2;
					X[2] = X[1];
					Y[2] = Y[1] + arrowW;
					X[3] = X[0];
					Y[3] = Y[0];
					g.fillPolygon(X, Y, 3);
					g.drawPolygon(X, Y, 4);
					X[0] = bounds.x + bounds.width - 1;
					X[1] = X[0] - arrowH;
					X[2] = X[1];
					X[3] = X[0];
					g.fillPolygon(X, Y, 3);
					g.drawPolygon(X, Y, 4);
					g.drawLine(bounds.x + 1, Y[0], bounds.x + bounds.width - 1, Y[0]);
				}
		}
		//show the midpoint position
		if (maxNBreaks > 1 && cmpTo >= focusMin && cmpTo <= focusMax) {
			int x = abs2Scr(cmpTo);
			g.setColor(Color.darkGray);
			if (isHorisontal) {
				g.drawLine(x, bounds.y + bounds.height - arrowH + 1, x, bounds.y + bounds.height - 1);
			} else {
				g.drawLine(bounds.x + bounds.width - arrowH + 1, bounds.y + x1 - x, bounds.x + bounds.width - 1, bounds.y + x1 - x);
			}
		}
	}

	public void redraw() {
		if (canvas != null) {
			Graphics g = canvas.getGraphics();
			if (g != null) {
				draw(g);
				g.dispose();
			}
		}
	}

	/**
	* Makes necessary operations for destroying, e.g. unregisters from
	* listening highlighting and other events.
	*/
	@Override
	public void destroy() {
		if (canvas != null) {
			canvas.removeMouseListener(this);
			canvas.removeMouseMotionListener(this);
		}
		if (breakTF != null) {
			breakTF.removeActionListener(this);
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

//-------------- notification about slider events --------------------

	public void addSliderListener(SliderListener slist) {
		if (slist == null)
			return;
		if (listeners == null) {
			listeners = new Vector(5, 5);
		}
		if (listeners.indexOf(slist) < 0) {
			listeners.addElement(slist);
		}
	}

	public void removeSliderListener(SliderListener slist) {
		if (slist == null || listeners == null)
			return;
		int idx = listeners.indexOf(slist);
		if (idx >= 0) {
			listeners.removeElementAt(idx);
		}
	}

	public void notifyBreaksChange() {
		fillTextField();
		if (listeners == null)
			return;
		for (int i = 0; i < listeners.size(); i++) {
			((SliderListener) listeners.elementAt(i)).breaksChanged(this, breaks.getArray(), breaks.size());
		}
	}

	public void notifyBreakMoving(int n, double currValue) {
		fillTextField();
		if (listeners == null)
			return;
		for (int i = 0; i < listeners.size(); i++) {
			((SliderListener) listeners.elementAt(i)).breakIsMoving(this, n, currValue);
		}
	}

	public void notifyColorsChanged() {
		if (listeners == null)
			return;
		for (int i = 0; i < listeners.size(); i++) {
			((SliderListener) listeners.elementAt(i)).colorsChanged(this);
		}
	}

//-------------- processing of mouse events --------------------------

	protected void restoreCursor() {
		if (otherCursor && canvas != null) {
			canvas.setCursor(Cursor.getDefaultCursor());
			otherCursor = false;
		}
	}

	protected void setMovementCursor() {
		if (!otherCursor && canvas != null) {
			canvas.setCursor(new Cursor((isHorisontal) ? Cursor.W_RESIZE_CURSOR : Cursor.N_RESIZE_CURSOR));
			otherCursor = true;
		}
	}

	protected void checkChangeCursor(int mx, int my) {
		if (!containsPoint(mx, my)) {
			restoreCursor();
		} else {
			int pos = (isHorisontal) ? mx : bounds.y + x1 - my;
			if (pos < x0 || pos > x1) {
				restoreCursor();
			} else {
				boolean fit = false;
				for (int i = 0; i < slPos.size() && !fit; i++) {
					int x = slPos.elementAt(i);
					fit = pos >= x - arrowW2 && pos <= x + arrowW2;
				}
				if (fit) {
					setMovementCursor();
				} else {
					restoreCursor();
				}
			}
		}
	}

	private int mpos0 = -1;
	private int activeSl = -1;
	private boolean isDragging = false;

	@Override
	public void mousePressed(MouseEvent e) {
		if (!containsPoint(e.getX(), e.getY()))
			return;
		activeSl = -1;
		if (isHorisontal) {
			mpos0 = e.getX();
		} else {
			mpos0 = bounds.y + x1 - e.getY();
		}
		if (mpos0 < x0 || mpos0 > x1) {
			mpos0 = -1;
			return;
		}
		for (int i = 0; i < slPos.size() && activeSl < 0; i++) {
			int x = slPos.elementAt(i);
			if (mpos0 >= x - arrowW2 && mpos0 <= x + arrowW2) {
				activeSl = i; //pressed on an existing slider
			}
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (mpos0 < 0)
			return;
		boolean changed = false;
		if (!isDragging) {
			if (activeSl >= 0) {
				activeSl = -1;
				mpos0 = -1;
				return; //clicked in an existing slider
			}
			if (breaks.size() < maxNBreaks) {
				boolean close = false;
				for (int i = 0; i < slPos.size() && !close; i++) {
					close = Math.abs(mpos0 - slPos.elementAt(i)) < minDist;
				}
				if (!close) {
					addBreak(scr2Abs(mpos0));
					changed = true;
				}
			} else {
				int idx = -1;
				for (int i = 0; i < slPos.size() && idx < 0; i++)
					if (slPos.elementAt(i) > mpos0) {
						idx = i;
					}
				if (idx < 0) {
					idx = slPos.size() - 1;
				}
				setBreakValue(scr2Abs(mpos0), idx);
				changed = true;
			}
		} else if (activeSl >= 0) {
			int lowLim = x0, upLim = x1;
			if (activeSl > 0) {
				lowLim = slPos.elementAt(activeSl - 1);
			}
			if (activeSl < slPos.size() - 1) {
				upLim = slPos.elementAt(activeSl + 1);
			}
			int mpos = (isHorisontal) ? e.getX() : bounds.y + x1 - e.getY();
			if (mpos - lowLim < minDist || upLim - mpos < minDist) {
				if (maxNBreaks == 1) {
					if (mpos - lowLim < minDist) {
						cmpTo = min;
					} else {
						cmpTo = max;
					}
					setBreakValue(cmpTo, activeSl);
				} else {
					removeBreak(activeSl);
				}
				changed = true;
			} else {
				setBreakValue(scr2Abs(mpos), activeSl);
				changed = true;
			}
		}
		isDragging = false;
		activeSl = -1;
		mpos0 = -1;
		if (changed) {
			redraw();
			notifyBreaksChange();
		}
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if (activeSl < 0)
			return;
		isDragging = true;
		int lowLim = x0, upLim = x1;
		if (activeSl > 0) {
			lowLim = slPos.elementAt(activeSl - 1);
		}
		if (activeSl < slPos.size() - 1) {
			upLim = slPos.elementAt(activeSl + 1);
		}
		//lowLim+=arrowW; upLim-=arrowW;
		int mpos = (isHorisontal) ? e.getX() : bounds.y + x1 - e.getY();
		if (mpos < lowLim) {
			mpos = lowLim;
		}
		if (mpos > upLim) {
			mpos = upLim;
		}
		if (Math.abs(mpos - slPos.elementAt(activeSl)) > 0) {
			setBreakValue(scr2Abs(mpos), activeSl);
			redraw();
			notifyBreakMoving(activeSl, breaks.elementAt(activeSl));
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
		restoreCursor();
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		checkChangeCursor(e.getX(), e.getY());
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		checkChangeCursor(e.getX(), e.getY());
	}

	public void setAligner(Aligner al) {
		aligner = al;
		aligner.addPropertyChangeListener(this);
	}

	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		if (pce.getSource() == aligner) {
			if (origBounds != null) {
				setBounds(origBounds);
			}
			if (canvas != null) {
				canvas.repaint();
			}
		}
	}

	/**
	* Reacts to user changing the break value(s) through the TextField
	*/
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == breakTF) {
			DoubleArray nbr = new DoubleArray(10, 10);
			String str = breakTF.getText().trim();
			Vector strings = null;
			if (maxNBreaks > 1) {
				strings = StringUtil.getNames(str, " ", false);
			} else {
				strings = new Vector(1, 1);
				strings.addElement(str);
			}
			if (strings != null) {
				for (int i = 0; i < strings.size(); i++) {
					str = (String) strings.elementAt(i);
					double f = Double.NaN;
					if (str.length() > 0) {
						if (minTime != null) {
							TimeMoment t = minTime.getCopy();
							if (t.setMoment(str)) {
								f = t.toNumber();
							}
						}
						if (Double.isNaN(f)) {
							try {
								f = Double.valueOf(str).doubleValue();
							} catch (NumberFormatException nfe) {
							}
						}
						if (!Double.isNaN(f)) {
							if (maxNBreaks == 1 && nbr.size() == 0) {
								nbr.addElement(f);
							} else if (maxNBreaks > 1 && f > getMin() && f < getMax() && (nbr.size() == 0 || f > nbr.elementAt(nbr.size() - 1))) {
								nbr.addElement(f);
							}
						}
					}
				}
			}
			if (nbr.size() > 0) {
				if (maxNBreaks > 1) {
					removeAllBreaks();
					for (int i = 0; i < nbr.size(); i++) {
						addBreak(nbr.elementAt(i));
					}
				} else {
					setMidPoint(nbr.elementAt(0));
				}
				redraw();
				notifyBreaksChange();
			} else {
				fillTextField();
			}
		}
	}

}
