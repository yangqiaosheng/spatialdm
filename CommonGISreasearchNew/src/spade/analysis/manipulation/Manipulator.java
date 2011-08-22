package spade.analysis.manipulation;

import spade.analysis.system.Supervisor;
import spade.vis.database.AttributeDataPortion;

/**
* The interface to be implemented by components for map manipulation
*/
public interface Manipulator {
	/**
	* Construction of map manipulator. Returns true if successfully constructed.
	* Arguments: 1) supervisor responsible for dynamic linking of displays;
	* 2) visualizer or classifier to be manipulated; 3) table with source data;
	* 4) table filter
	* The Manipulator must check if the visualizer has the appropriate type.
	*/
	public boolean construct(Supervisor sup, Object visualizer, AttributeDataPortion table);
}
