package spade.vis.mapvis;

import java.awt.Component;

import spade.lib.color.ColorScale;
import spade.vis.dmap.DrawingParameters;

/**
* Used for the representation of grid (raster) data, i.e. matrices of real numbers.
*/
public interface GridVisualizer {
	/**
	* Sets the minimum and maximum values to be encoded
	*/
	public void setMinMax(float min, float max);

	/**
	* Sets the parameters determining the appearance of the layer in a map.
	*/
	public void setDrawingParameters(DrawingParameters dp);

	/**
	* Returns the parameters determining the appearance of the layer in a map.
	*/
	public DrawingParameters getDrawingParameters();

	/**
	* Sets the color scale to be used to encode grid data
	*/
	public void setColorScale(ColorScale cs);

	/**
	* Constructs and returns a manipulator for this visualizer.
	*/
	public Component getGridManipulator();
}
