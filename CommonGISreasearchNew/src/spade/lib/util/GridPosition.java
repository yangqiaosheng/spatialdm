package spade.lib.util;

/**
* Used for drawing grids on graphs and plots. Contains a "rounded" number
* ans its string representation with an appropriate precision.
*/
public class GridPosition {
	/**
	* A "rounded" number for a grid position.
	*/
	public float value = 0f;
	/**
	* The offset of this grid position from the beginning of the axis.
	*/
	public int offset = 0;
	/**
	* The string representation of the grid position.
	*/
	public String strVal = null;
}