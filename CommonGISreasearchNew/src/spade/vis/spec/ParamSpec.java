package spade.vis.spec;

import java.util.Vector;

/**
* The interface is introduced for distinguishing temporal parameters
*/
public interface ParamSpec {
	/**
	* Replies whether this is a temporal parameter
	*/
	public boolean isTemporalParameter();

	/**
	* Replies whether the values of this parameter must be ordered
	*/
	public boolean mustBeOrdered();

	/**
	* For a parameter with ordered values, the order may be explicitly specified.
	* In this case, this method returns the values of the parameter in the
	* prescribed order.
	*/
	public Vector getValueOrder();
}