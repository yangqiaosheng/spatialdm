package spade.vis.mapvis;

/**
* Implements multiple choropleth maps, i.e. filling contours according to values
* of numeric attributes.
*/
public class MultiMapNumPainter extends MultiMapNumDrawer {
	public MultiMapNumPainter() {
		drawer = new NumValuePainter();
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
	* visualization method: color hues.
	*/
	@Override
	public void startChangeParameters() {
		startChangeColors();
	}
}