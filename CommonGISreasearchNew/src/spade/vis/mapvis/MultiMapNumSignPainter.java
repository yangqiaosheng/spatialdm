package spade.vis.mapvis;

import spade.vis.geometry.Sign;

/**
* Implements multiple maps with painting signs (circles) according to values
* of numeric attributes.
*/
public class MultiMapNumSignPainter extends MultiMapNumDrawer implements SignDrawer {
	public MultiMapNumSignPainter() {
		drawer = new NumValueSignPainter();
	}

	/**
	* Methods from the interface SignDrawer
	*/
	@Override
	public Sign getSignInstance() {
		return ((NumValueSignPainter) drawer).getSignInstance();
	}

	/**
	* Through this method a SignDrawer is informed that some of the sign properties
	* have changed. The sign drawer must care about repainting the map.
	* The possible identifiers of sign properties are listed in the class
	* @see spade.vis.geometry.Sign.
	*/
	@Override
	public void signPropertyChanged(int propertyId, Sign sgn) {
		((NumValueSignPainter) drawer).signPropertyChanged(propertyId, sgn);
		notifyVisChange();
	}

	/**
	* Must reply whether any parameters of this visualization method may be
	* changed, e.g. colors or sizes of signs. This does not include interactive
	* analytical manipulation.
	* Returns true.
	*/
	@Override
	public boolean canChangeParameters() {
		return true;
	}

	/**
	* Constructs and displays a dialog for changing parameters of this
	* visualization method: minimum and maximum sizes of the signs.
	*/
	@Override
	public void startChangeParameters() {
		SignParamsController pc = new SignParamsController();
		pc.startChangeParameters(this);
	}
}