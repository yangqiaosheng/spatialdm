package spade.vis.geometry;

import java.awt.Color;
import java.awt.Point;

import spade.lib.basicwin.Metrics;

public abstract class Sign implements Diagram {
	public static int mm = 0;
	public static int defaultMaxWidth = 0, defaultMaxHeight = 0;
	public static int defaultMinWidth = 0, defaultMinHeight = 0;
	protected int maxW = 0, maxH = 0;
	protected int minW = 0, minH = 0;
	protected int width = 0, height = 0;
	protected int labelX = 0, labelY = 0;
	protected boolean isRound = false, usesMinSize = false, usesFrame = false;
	protected Color color = Color.white, borderColor = Color.black;
	protected int transparency = 0;
	/**
	* These variables indicate sign properties that may be changed
	*/
	public static final int SIZE = 0, MIN_SIZE = 1, MAX_SIZE = 2, USE_MIN_SIZE = 3, COLOR = 4, BORDER_COLOR = 5, USE_FRAME = 6, SEGMENT_ORDER = 7, PROPERTY_FIRST = SIZE, PROPERTY_LAST = SEGMENT_ORDER;
	/**
	* True stands for changeable properties. By default, none of the sign
	* properties may be changed. The number of elements in the array must
	* correspond to the number of properties.
	*/
	protected boolean mayChangeProperty[] = { false, false, false, false, false, false, false, false };

	public Sign() {
		if (mm <= 0) {
			mm = Metrics.mm();
		}
		if (defaultMaxWidth <= 0) {
			defaultMaxWidth = 10 * mm;
		}
		if (defaultMaxHeight <= 0) {
			defaultMaxHeight = 10 * mm;
		}
		if (defaultMinWidth <= 0) {
			defaultMinWidth = 2 * mm;
		}
		if (defaultMinHeight <= 0) {
			defaultMinHeight = 2 * mm;
		}

		maxW = defaultMaxWidth;
		maxH = defaultMaxHeight;
		minW = defaultMinWidth;
		minH = defaultMinHeight;
		width = maxW;
		height = maxH;
	}

	/**
	* Replies whether the given sign property may be changed
	*/
	public boolean mayChangeProperty(int propertyId) {
		if (propertyId < PROPERTY_FIRST || propertyId > PROPERTY_LAST)
			return false;
		return mayChangeProperty[propertyId];
	}

	/**
	* Allows or prohibits changing the given property of the sign.
	*/
	public void setMayChangeProperty(int propertyId, boolean mayChange) {
		if (propertyId < PROPERTY_FIRST || propertyId > PROPERTY_LAST)
			return;
		mayChangeProperty[propertyId] = mayChange;
	}

	/**
	* Diameter of a round sign is the minimum of the width and the height
	*/
	public int getDiameter() {
		return (width <= height) ? width : height;
	}

	@Override
	public int getWidth() {
		if (isRound)
			return getDiameter();
		return width;
	}

	@Override
	public int getHeight() {
		if (isRound)
			return getDiameter();
		return height;
	}

	/**
	* Returns the position of the label. This method should be called after draw(...).
	*/
	@Override
	public Point getLabelPosition() {
		return new Point(labelX, labelY);
	}

	/**
	* Replies whether this diagram is centered, i.e. the center of the diagram
	* coinsides with the center of the object
	*/
	@Override
	public boolean isCentered() {
		return isRound;
	}

	protected void checkSizes() {
		if (width > maxW) {
			width = maxW;
		}
		if (height > maxH) {
			height = maxH;
		}
		if (usesMinSize) {
			if (width < minW) {
				width = minW;
			}
			if (height < minH) {
				height = minH;
			}
		}
	}

	public void setSizes(int w, int h) {
		width = w;
		height = h;
		checkSizes();
	}

	public void setWidth(int w) {
		width = w;
		checkSizes();
	}

	public void setHeight(int h) {
		height = h;
		checkSizes();
	}

	public int getMaxWidth() {
		return maxW;
	}

	public int getMaxHeight() {
		return maxH;
	}

	public void setMaxSizes(int w, int h) {
		maxW = w;
		maxH = h;
		checkSizes();
	}

	public void setMaxWidth(int w) {
		maxW = w;
		checkSizes();
	}

	public void setMaxHeight(int h) {
		maxH = h;
		checkSizes();
	}

	public boolean getUsesMinSize() {
		return usesMinSize;
	}

	public boolean getIsRound() {
		return isRound;
	}

	public int getMinWidth() {
		return minW;
	}

	public int getMinHeight() {
		return minH;
	}

	public void setMinSizes(int w, int h) {
		minW = w;
		minH = h;
		checkSizes();
	}

	public void setMinWidth(int w) {
		minW = w;
		checkSizes();
	}

	public void setMinHeight(int h) {
		minH = h;
		checkSizes();
	}

	public void setDefaultSizes() {
		setMaxSizes(defaultMaxWidth, defaultMaxHeight);
		setMinSizes(defaultMinWidth, defaultMinHeight);
	}

	/**
	* Setting and getting the current color makes sense for simple signs like
	* triangles or circles but not for structured signs where colors correspond
	* to attributes.
	*/
	public Color getColor() {
		return color;
	}

	public void setColor(Color c) {
		color = c;
	}

	public Color getBorderColor() {
		return borderColor;
	}

	public void setBorderColor(Color c) {
		borderColor = c;
	}

	/**
	* For some types of signs drawing of a "frame" can make sense. A "frame"
	* shows the maximum possible size of the sign.
	*/
	public boolean getMustDrawFrame() {
		return usesFrame;
	}

	public void setMustDrawFrame(boolean value) {
		usesFrame = value;
	}

	public void setUsesMinSize(boolean value) {
		usesMinSize = value;
	}

	/**
	* This method is only relevant for structured signs.
	* Sets the method of ordering segments in the sign (if the sign is structured,
	* i.e. consists of several segments). Three possible methods of segment
	* arrangement are defined in @see spade.vis.geometry.StructSign :
	* OrderPreserved=0 (preserves the order of the attributes), OrderDescending=1
	* (from biggest to smallest), OrderAscending=2 (from smallest to biggest).
	* By default, this method does nothing.
	*/
	public void setSegmentOrderMethod(int order) {
	}

	public void setTransparency(int t) {
		transparency = t;
	}

	public int getTransparency() {
		return transparency;
	}

}
