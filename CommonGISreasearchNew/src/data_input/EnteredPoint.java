package data_input;

import spade.vis.geometry.RealPoint;

/**
 *   Only for FloraWeb-Project!
*    extends to add fields 'radius' and 'tk_grid'
*    used by DataInputManager as elements in vector of entered points
*
 */

public class EnteredPoint extends RealPoint {
	public float radius = 0.0f;
	public String tk_grid = null;

	public EnteredPoint() {
	}

}
