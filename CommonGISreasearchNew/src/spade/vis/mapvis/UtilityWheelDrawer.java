package spade.vis.mapvis;

/**
* A convenience method: the same as UtilitySignDrawer, only the type of the
* sign is set to "pie" in the constructor. This class is needed to avoid
* direct references to visualizer classes in the DataMapper class. This
* simplifies exclusion of classes from configurations where they are
* not needed.
*/

public class UtilityWheelDrawer extends UtilitySignDrawer {
	/**
	* Sets the type of the sign to PieSign
	*/
	public UtilityWheelDrawer() {
		signType = PieSign;
	}
}
