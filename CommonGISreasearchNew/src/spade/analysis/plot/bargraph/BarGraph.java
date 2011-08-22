package spade.analysis.plot.bargraph;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Vector;

import spade.lib.basicwin.Drawable;
import spade.lib.basicwin.Metrics;

/** The abstract baseclass of all bargraph objects.
 * See its descendant {@link HorizontalBarGraph HorizontalBarGraph} for example
 * code.
 *
 * @author Mario Boley
 * @version 1.0
 */
public abstract class BarGraph implements Drawable {

	/** A class representing a single bar within a BarGraph.
	 *
	 */
	protected class Bar {
		/**
		 * Value the bar represents.
		 */
		float value = Float.NaN;
		/**
		 * Description of the bar.
		 */
		String description = "";
		/**
		 * The X-coordinate of the bar.
		 */
		int x = 0;
		/**
		 * The Y-coordinate of the bar.
		 */
		int y = 0;
		/**
		 * The height of the bar.
		 */
		int height = 0;
		/**
		 * The width of the bar.
		 */
		int width = 0;
		/**
		 * The X-coordinate of the bar's description-String.
		 */
		int descriptionX = 0;
		/** The Y-coordinate of the bar's description-String.
		 */
		int descriptionY = 0;

		/** Draws the Bar. Fillcolor is {@link BarGraph#barColor
		     *  BarGraph.barColor} and bordercolor {@link BarGraph#baselineColor
		     *  BarGraph.baselineColor}.
		 *
		 * @param g the Graphics-context on which the Bar is to be painted
		 */
		void drawBar(Graphics g) {
			g.setColor(barColor);
			g.fillRect(x, y, width, height);
			g.setColor(baselineColor);
			g.drawRect(x, y, width, height);
		}

		/** Draws the bar's description String.
		 *
		 * @param g the Graphics-context on which the Bar is to be painted
		 */
		void drawDescription(Graphics g) {
			g.setColor(textOutsideBarColor);
			g.drawString(description, descriptionX, descriptionY);
		}

		/** Draws bar and description.
		 *
		 * @param g the Graphics-context on which the Bar is to be painted
		 */
		void draw(Graphics g) {
			drawBar(g);
			drawDescription(g);
		}

		/** Checks if a given point is inside the Bar.
		 *
		 * @param ax the Y-Coordinate of the Point to be checked
		 * @param ay the Y-Coordinate of the Point to be checked
		 * @return true if point is inside bar - false in other cases
		 */
		boolean contains(int ax, int ay) {
			return ((ax >= x && ax <= x + width) && (ay >= y && ay <= y + height));
		}

	}

	/** The data of each bar. It can be accessed via {@link #setValues(Vector v)
	 * setValues} and {@link #setDescriptions(Vector v) setDescriptions}.
	 */
	protected Bar[] bars = null;
	/** The canvas, on which the bargraph draws itself.
	 */
	protected Canvas canvas = null;
	/** The bounds, into which the bargraph must fit.
	 */
	protected Rectangle bounds = null;
	/** Indicates destroyed state.
	 */
	protected boolean destroyed = false;

	/** The margin on the left of the bargraph not to be painted with any content.
	 */
	protected int leftMargin = 0, rightMargin = 0, topMargin = 0, bottomMargin = 0;

	/** The outermost value, which is completely visible. The value of this field
	 *  should be used in the method {@link #setup() setup()}.
	 */
	protected float borderValue;

	public void setBorderValue(float borderValue) {
		this.borderValue = borderValue;
	}

	/** The minimum length of the bars. The value of this field should be used in
	 *  the method {@link #setup() setup()}.
	 */
	protected int minBarLength;
	/** The maximum width of the bars. The value of this field should be used in the
	 *  method {@link #setup() setup()}.
	 */
	protected int maxBarWidth;
	/** The minimum length of the bars. The value of this field should be used in
	 *  the method {@link #setup() setup()}.
	 */
	protected int minBarWidth;
	/** The maximum width of the spaces between the bars. The value of this field
	 *  should be used in the method {@link #setup() setup()}.
	 */
	protected int maxSpaceWidth;
	/** The minimum width of the spaces between the bars. The value of this field
	 *  should be used in the method {@link #setup() setup()}.
	 */
	protected int minSpaceWidth;
	/** The percentage of room of each space/bar unit , which is used for spaces.
	 *  The value of this field should be used in the method {@link #setup()
	 *  setup()}.
	 */
	protected int spacePercentage;

	/** The background color of the bargraph. The method {@link #draw() draw()}
	 *  fills the background with this color.
	 */
	protected Color backgroundColor;
	/** The color intended to fill the interior of the bars. Its actual usage is
	 *  determined by the heirs of BarGraph.
	 */
	protected Color barColor;
	/** Intended to be the color of the text that appears outside the bars. (e.g.
	 *  descriptions/absolute values in {@link CorrelationBarGraph
	 *  CorrelationBarGraph}). Its actual usage is determined by the heirs of
	 *  BarGraph.
	 */
	protected Color textOutsideBarColor;
	/** Is intended to be the color of the zeroline and the line at the outer left
	 *  of the bargraph. {@link Bar#drawBar(Graphics g) Bar.drawBar} uses it for
	 *  the borders of the bar. Beyond this its actual usage is determined by the
	 *  descendants of BarGraph.
	 */
	protected Color baselineColor;

	/** Sets appereance of the bargraph to some standard values.
	 * <P>
	 * <UL>
	 * <LI> {@link #backgroundColor backgroundColor} to lightGray, <BR>
	 * <LI> {@link #barColor barColor} to blue, <BR>
	 * <LI> {@link #textOutsideBarColor textOutsideBarColor} to black, <BR>
	 * <LI> {@link #baselineColor baselineColor} to black, <BR>
	 * <LI> {@link #minBarLength minBarLength} to 25mm, <BR>
	 * <LI> {@link #minBarWidth minBarWidth} to FontHeigth <BR>
	 * <LI> {@link #maxBarWidth maxBarWidth} to 10mm, <BR>
	 * <LI> {@link #maxSpaceWidth maxSpaceWidth} to 5mm, <BR>
	 * <LI> {@link #minSpaceWidth minSpaceWidth} to 0mm, <BR>
	 * <LI> {@link #spacePercentage spacePercentage} to 37%, <BR>
	 * <LI> {@link #borderValue borderValue} to 0, <BR>
	 * </UL>
	 */
	public BarGraph() {
		backgroundColor = Color.lightGray;
		barColor = Color.blue;
		textOutsideBarColor = Color.black;
		baselineColor = Color.black;
		minBarLength = Metrics.mm() * 25;
		minBarWidth = Metrics.getFontMetrics().getHeight();
		maxBarWidth = Metrics.mm() * 10;
		maxSpaceWidth = Metrics.mm() * 5;
		minSpaceWidth = 0;
		spacePercentage = 37;
		borderValue = 0;
	}

	/** Calls {@link #BarGraph() BarGraph()} and sets the bars to initial values
	 * .
	 *
	 * @param val the initial values of the bars
	 */
	public BarGraph(Vector val) {
		this();
		setValues(val);
	}

	/** Calls {@link #BarGraph() BarGraph()} and initializes values and descriptions
	 *  of the bars.
	 *
	 * @param val a vector of Float-Objects containing the initial values of the
	 *     bars
	 * @param des a vector of String-Objects containing the initial
	 *     descriptions of each bar
	 */
	public BarGraph(Vector val, Vector des) {
		this(val);
		setDescriptions(des);
	}

	/** Sets the values of the bars. Also sets {@link #borderValue borderValue}
	 * to the maximum absolute value.
	 * Creates a new array {@link #bars bars} and creates a corresponding amount of
	 * {@link Bar Bar}-Objects if the old one does not fit the number of Floats
	 * delivered. note that the method {@link BarGraph#setDescriptions()
	 * setDescriptions()} neither changes array-bounds nor creates objects. so when
	 * passing data to a BarGraph, this method has to be called always first.
	 *
	 * @param v a vector of Float-objects containing the values of the bars
	 */
	public void setValues(Vector v) {
		if (v == null)
			return;
		if (bars == null || bars.length != v.size()) {
			bars = new Bar[v.size()];
		}
		if ((v.size() > 0) && (v.firstElement() instanceof Float)) {
			borderValue = Math.abs(((Float) v.firstElement()).floatValue());
		} else {
			borderValue = 0;
		}
		for (int i = 0; i < v.size(); i++) {
			if (bars[i] == null) {
				bars[i] = new Bar();
			}
			if (v.elementAt(i) instanceof Float) {
				bars[i].value = ((Float) v.elementAt(i)).floatValue();
			} else {
				bars[i].value = 0;
			}
			borderValue = Math.max(borderValue, Math.abs(bars[i].value));
		}
	}

	/** Method for setting a description for each bar. It saves the Strings in the
	 *  description-field of every {@link Bar Bar} of the array {@link #bars bars}
	 *  but neither changes the bounds of this array nor creates new Objects. This
	 *  can be achieved by using {@link #setValues() setValues()} or the constructor
	 *  {@link #BarGraph(Vector val, Vector des) BarGraph(Vector val, Vector des)}.
	 *
	 * @param v a vector of String-Objects containing the descriptions of each bar
	 */
	public void setDescriptions(Vector v) {
		if (v != null && bars != null && v.size() > 0) {
			for (int i = 0; i < Math.min(v.size(), bars.length); i++)
				if (v.elementAt(i) instanceof String) {
					bars[i].description = (String) v.elementAt(i);
				}
		}
	}

	/** Selector of the field {@link #baselineColor baselineColor}.
	 * This color is supposed to be used for the baselines of the BarGraph. Its
	 * actual usage is determined by the heirs of BarGraph.
	 *
	 * @param c a java.awt.Color-Object
	 */
	public void setBaselineColor(Color c) {
		baselineColor = c;
	}

	/** Selector of the field {@link #backgroundColor backgroundColor}. The method
	 * {@link #draw() draw()} fills the background with this color.
	 *
	 * @param c a java.awt.Color-Object
	 */
	public void setBackgroundColor(Color c) {
		backgroundColor = c;
	}

	/** Selector of the field {@link #barColor barColor}. It is intended to fill the
	 * interior of the bars. Its actual usage is determined by the heirs of
	 * BarGraph.
	 *
	 * @param c a java.awt.Color-Object
	 */
	public void setBarColor(Color c) {
		barColor = c;
	}

	/** Sets the field {@link textOutsideBarColor textOutsideBarColor} The text
	 *  outside the bars is intended to appear in this color. Its actual usage is
	 *  determined by the heirs of BarGraph.
	 *
	 * @param c a java.awt.Color-Object
	 */
	public void setTextOutsideBarColor(Color c) {
		textOutsideBarColor = c;
	}

	/** Current color of baselines and barborders.
	 *
	 * @return content of the field {@link #baselineColor baselineColor}
	 */
	public Color getBaselineColor() {
		return baselineColor;
	}

	/**  Current backgroundcolor.
	 *
	 * @return content of the field {@link #backgroundColor backgroundColor}
	 */
	public Color getBackgroundColor() {
		return backgroundColor;
	}

	/** Current fillcolor of the bars.
	 *
	 * @return content of the field {@link #barColor barColor}
	 */
	public Color getBarColor() {
		return barColor;
	}

	/** Current color for texts outside of the bars.
	 *
	 * @return content of the field {@link #textOutsideBarColor textOutsideBarColor}
	 */
	public Color getTextOutsideBarColor() {
		return textOutsideBarColor;
	}

	/** Sets the field {@link #maxSpaceWidth maxSpaceWidth}.
	 *
	 * @param max int - negative values are treated as 0, values lower than
	 *     minSpaceWidth as minSpaceWidth
	 */
	public void setMaxSpaceWidth(int max) {
		maxSpaceWidth = Math.max(0, max);
		minSpaceWidth = Math.min(maxSpaceWidth, minSpaceWidth);
	}

	/** Sets the field {@link #minSpaceWidth minSpaceWidth}.
	 *
	 * @param min int - negative values are treated as 0, values higher than
	 *     maxSpaceWidth as maxSpaceWidth
	 */
	public void setMinSpaceWidth(int min) {
		minSpaceWidth = Math.max(0, min);
		maxSpaceWidth = Math.max(maxSpaceWidth, minSpaceWidth);
	}

	/** Combines the selectors {@link #setSpacePercentage(int percent)
	 *  setSpacePercentage}, {@link #setMaxSpaceWidth(int max) setMaxSpaceWidth} and
	 *  {@link #setMinSpaceWidth(int min) setMinSpaceWidth} by calling each of them.
	 *
	 * @param percent int - values higher than 100 or lower than 0 are treated as
	 *     100 respective 0
	 * @param max int - values lower than 0 or minSpaceWidth are treated as 0
	 *      respective minSpaceWidth
	 * @param min int - negative values are treated as 0, values higher than
	 *      maxSpaceWidth as maxSpaceWidth
	 */
	public void setSpace(int percent, int max, int min) {
		setSpacePercentage(percent);
		setMaxSpaceWidth(max);
		setMinSpaceWidth(min);
	}

	/** Sets the field {@link #spacePercentage spacePercentage}.
	 *
	 * @param percent int - values higher than 100 or lower than 0 are treated as
	 *     100 respective 0
	 */
	public void setSpacePercentage(int percent) {
		spacePercentage = Math.max(0, Math.min(100, percent));
	}

	/** Sets the field {@link #maxBarWidth maxBarWidth}.
	 *
	 * @param w int - values lower than 0 or minBarWidth are treated as 0
	 *     respective minBarWidth
	 */
	public void setMaxBarWidth(int w) {
		if (w < 1) {
			w = 1;
		}
		if (w < minBarWidth) {
			w = minBarWidth;
		}
		maxBarWidth = w;
	}

	/** Sets the field {@link #minBarWidth minBarWidth}.
	 *
	 * @param w int - negative values are treated as 0, values higher than
	 *     maxBarWidth as maxBarWidth
	 */
	public void setMinBarWidth(int w) {
		if (w < 0) {
			w = 0;
		}
		if (w > maxBarWidth) {
			w = maxBarWidth;
		}
		minBarWidth = w;
	}

	/** Sets the 4 fields {@link #leftMargin leftMargin}, {@link #rightMargin
	 *  rightMargin}, {@link #bottomMargin bottomMargin} and {@link #topMargin
	 *  topMargin}. All margins are intended not to be painted with any content.
	 *
	 * @param m int-value for all 4 margins - negative values are treated as 0
	 */
	public void setMargin(int m) {
		if (m < 0) {
			m = 0;
		}
		leftMargin = rightMargin = topMargin = bottomMargin = m;
	}

	/** Sets the field {@link #leftMargin leftMargin}. All margins are intended not
	 *  to be painted with any content.
	 *
	 * @param m int - negative values are treated as 0
	 */
	public void setLeftMargin(int m) {
		if (m < 0) {
			m = 0;
		}
		leftMargin = m;
	}

	/** Sets the field {@link #rightMargin rightMargin}. All margins are intended
	 *  not to be painted with any content.
	 *
	 * @param m int - negative values are treated as 0
	 */
	public void setRightMargin(int m) {
		if (m < 0) {
			m = 0;
		}
		rightMargin = m;
	}

	/** Sets the field {@link #topMargin topMargin}. All margins are intended not to
	 *  be painted with any content.
	 *
	 * @param m int - negative values are treated as 0
	 */
	public void setTopMargin(int m) {
		if (m < 0) {
			m = 0;
		}
		topMargin = m;
	}

	/** Sets the field {@link #bottomMargin bottomMargin}. All margins are intended
	 *  not to be painted with any content.
	 *
	 * @param m int - negative values are treated as 0
	 */
	public void setBottomMargin(int m) {
		if (m < 0) {
			m = 0;
		}
		bottomMargin = m;
	}

	/** Returns the content of the field {@link #leftMargin leftMargin}.
	 *
	 * @return a value >= 0
	 */
	public int getLeftMargin() {
		return leftMargin;
	}

	/** Returns the content of the field {@link #rightMargin rightMargin}.
	 *
	 * @return a value >= 0
	 */
	public int getRightMargin() {
		return rightMargin;
	}

	/** Returns the content of the field {@link #topMargin topMargin}.
	 *
	 * @return a value >= 0
	 */
	public int getTopMargin() {
		return topMargin;
	}

	/** Returns the content of the field {@link #bottomMargin bottomMargin}.
	 *
	 * @return a value >= 0
	 */
	public int getBottomMargin() {
		return bottomMargin;
	}

	/** Is intended to set up the {@link Bar Bar-objects}. Giving them concrete
	 *  x/y-coordinates, heigth and width should be done here.
	 */
	public abstract void setup();

	/** Is intended to calculate local and global bar-data depending on {@link
	 *  #bounds bounds}.
	 */
	protected abstract void calcBoundsDependentData();

	/** Intended to draw the customized background of BarGraph descendants. Is
	 *  called by {@link #draw draw} after filling the background with {@link
	 *  #backgroundColor backgroundColor}. Thereby this is not neccesary to be done
	 *  again her.
	 *
	 * @param g the Graphics-context on which the background shall be painted
	 */
	protected abstract void drawBackground(Graphics g);

	/** Sets the field {@link #canvas canvas}.
	 *
	 * @param c the Canvas, associated to the BarGraph
	 */
	@Override
	public void setCanvas(Canvas c) {
		canvas = c;
	}

	/** Returns the constant value of 6cm x 6cm. Is required by the interface
	 * drawable. Descendants of BarGraph must overwrite this method and calculate
	 * the space virtually occupied by the BarGraph according to given amount of
	 * bars and screen-proportions (needed e.g. for calculation of scrollbars).
	 *
	 * @return the Dimension of 6cm x 6cm
	 */
	@Override
	public Dimension getPreferredSize() {
		return new Dimension(60 * Metrics.mm(), 60 * Metrics.mm());
	}

	/** Sets the field {@link #bounds bounds}.
	 * Afterwards it calls {@link #calcBoundsDependentData()
	 * calcBoundsDependentData} and {@link #setup() setup}.
	 *
	 * @param b the rectangle, in which the BarGraph has to fit
	 */
	@Override
	public void setBounds(Rectangle b) {
		bounds = b;
		if (bounds == null)
			return;
		calcBoundsDependentData();
		setup();
	}

	/** Required by the interface drawable.
	 *
	 * @return the content of {@link #bounds bounds}
	 */
	@Override
	public Rectangle getBounds() {
		return bounds;
	}

	/** Draws the BarGraph. It fills the {@link #bounds bounds} with the {@link
	 * #backgroundColor backgroundColor}. Then it calls the method {@link
	 * #drawBackground(Graphics g) drawBackground} and the method {@link
	 * Bar#draw(Graphics g) draw of Bar} for each entry of the array {@link #bars
	 * bars}.
	 *
	 * @param g the Graphics-context on which the BarGraph shall be painted
	 */
	@Override
	public void draw(Graphics g) {
		if (bounds == null || g == null)
			return;
		g.setColor(backgroundColor);
		g.fillRect(bounds.x, bounds.y, bounds.width + 1, bounds.height + 1);
		drawBackground(g);
		if (bars == null)
			return;
		for (Bar bar : bars) {
			bar.draw(g);
		}
	}

	/** Is required by the interface drawable.
	 */
	@Override
	public void destroy() {
		destroyed = true;
	}

	/** Returns destroyed state. Is required by the interface drawable.
	 *
	 * @return content of the field {@link #destroyed destroyed}
	 */
	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

}
