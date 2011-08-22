package spade.vis.dmap;

import java.awt.Color;

/**
* Colors, line width, and, probably, other parameters determining the
* appearance of Geo Objects in a map. These parameters are common for all
* Geo Objects belonging to the same Geo Layer.
*/

public class DrawingParameters implements java.io.Serializable {
	// style of font for labels
	public static final int NORMAL = 0, UNDERLINED = 1, SHADOWED = 2;

	public Color lineColor = Color.blue, fillColor = Color.lightGray, labelColor = Color.black, patternColor = Color.gray;
	//information for drawing labels
	public String fontName = null;
	public int fontStyle = 1, fontSize = 12, labelStyle = NORMAL;

	public int lineWidth = 1;
	/**
	* For a QuadTree layer - the uper limit of the allowed number of levels;
	* -1 means undefined
	*/
	public int maxLevels = -1;
	/**
	* width of lines used for highlighting and selection
	*/
	public int hlWidth = 3, selWidth = 3;
	/**
	* whether to draw circles for highlighting (only for areas)
	*/
	public boolean hlDrawCircles = false;
	/**
	* the color of the circles used for highlighting
	*/
	public Color hlCircleColor = Color.red;
	/**
	* the size (diameter) of the circles used for highlighting
	*/
	public int hlCircleSize = 5;
	/**
	* fill pattern (not used)
	*/
	public int patternN = 1;
	public boolean drawLayer = true, drawBorders = true, fillContours = true, isTransparent = false, drawLabels = false,
	// conditional drawing (if scale is between min and max)
			drawCondition = false;
	// min and max scales for condition drawing
	public float minScaleDC = Float.NaN, maxScaleDC = Float.NaN;
//ID

	// Parameters of raster layers or area ~MO:
	// transparency, %
	public int transparency = 0;
	// color scale (class name without package)
	public String colorScale = "";
	// color scale parameters ()
	public String csParameters = "";

//~ID
	public boolean drawHoles = false;
	/**
	* Indicates whether objects in the layer must be filled by "default" (gray)
	* color when a visualizer does not find an appropriate color (in particular,
	* in cases of missing values)
	*/
	public boolean useDefaultFilling = true;
	/**
	 * Indicates whether the layer must react to a spatial filter
	 */
	public boolean allowSpatialFilter = true;

	public DrawingParameters makeCopy() {
		DrawingParameters dp = new DrawingParameters();
		copyTo(dp);
		return dp;
	}

	public void copyTo(DrawingParameters dp) {
		if (dp == null)
			return;
		dp.lineColor = lineColor;
		dp.fillColor = fillColor;
		dp.patternColor = patternColor;
		dp.lineWidth = lineWidth;
		dp.hlWidth = hlWidth;
		dp.selWidth = selWidth;
		dp.patternN = patternN;
		dp.drawLayer = drawLayer;
		dp.drawBorders = drawBorders;
		dp.fillContours = fillContours;
		dp.isTransparent = isTransparent;
		dp.drawLabels = drawLabels;
		dp.labelColor = labelColor;
		dp.fontName = fontName;
		dp.fontSize = fontSize;
		dp.fontStyle = fontStyle;
		dp.labelStyle = labelStyle;
		dp.hlDrawCircles = hlDrawCircles;
		dp.hlCircleColor = hlCircleColor;
		dp.hlCircleSize = hlCircleSize;
		dp.allowSpatialFilter = allowSpatialFilter;
//ID
		dp.transparency = transparency;
		dp.colorScale = colorScale;
		dp.csParameters = csParameters;
		dp.drawCondition = drawCondition;
		dp.minScaleDC = minScaleDC;
		dp.maxScaleDC = maxScaleDC;
		dp.maxLevels = maxLevels;
		dp.drawHoles = drawHoles;
//~ID
	}

	@Override
	public boolean equals(Object o) {
		if (o == null)
			return false;
		if (!(o instanceof DrawingParameters))
			return false;
		DrawingParameters dp = (DrawingParameters) o;
		return dp.lineColor == lineColor && dp.fillColor == fillColor && dp.labelColor == labelColor && dp.patternColor == patternColor && dp.lineWidth == lineWidth && dp.hlWidth == hlWidth && dp.selWidth == selWidth && dp.patternN == patternN
				&& dp.drawLayer == drawLayer && dp.drawBorders == drawBorders && dp.drawLabels == drawLabels && dp.fontSize == fontSize && dp.fontStyle == fontStyle && dp.labelStyle == labelStyle && dp.fillContours == fillContours
				&& dp.isTransparent == isTransparent && dp.hlDrawCircles == hlDrawCircles && dp.hlCircleColor == hlCircleColor && dp.hlCircleSize == hlCircleSize && dp.allowSpatialFilter == allowSpatialFilter &&
//ID
				dp.transparency == transparency && dp.colorScale == colorScale && dp.csParameters == csParameters &&
//~ID
				dp.drawHoles == drawHoles && dp.drawCondition == drawCondition && dp.minScaleDC == minScaleDC && dp.maxScaleDC == maxScaleDC && ((dp.fontName == null && fontName == null) || (fontName != null && fontName.equals(dp.fontName)));
	}
}
