package spade.lib.basicwin;

/**
* Listener to the events of focusing on value subranges of a numeric attribute.
*/

public interface FocusListener {
	public void focusChanged(Object source, double lowerLimit, double upperLimit);

	public void limitIsMoving(Object source, int n, double currValue); // n==0 -> min, n==1 -> max
}