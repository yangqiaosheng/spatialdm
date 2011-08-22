package spade.vis.spec;

import java.util.Vector;

public abstract class ProtoParam implements ParamSpec, java.io.Serializable {
	/**
	* Indicates whether the values of this parameter must be ordered
	*/
	protected boolean ordered = false;
	/**
	* For a parameter with ordered values, the order may be explicitly specified.
	* In this case, this vector contains the values of the parameter in the
	* prescribed order.
	*/
	protected Vector order = null;
	/**
	* Indicates whether the parameter is temporal
	*/
	public boolean isTemporal = false;
	/**
	 * For a temporal parameter, indicates whether missing values of parameter-
	 * dependent attributes should be filled with the last known values
	 */
	public boolean protractKnownValues = false;

	/**
	* Replies whether this is a temporal parameter
	*/
	@Override
	public boolean isTemporalParameter() {
		return isTemporal;
	}

	/**
	* Replies whether the values of this parameter must be ordered
	*/
	@Override
	public boolean mustBeOrdered() {
		return ordered;
	}

	/**
	* Sets whether the values of this parameter must be ordered
	*/
	public void setMustBeOrdered(boolean toBeOrdered) {
		ordered = toBeOrdered;
	}

	/**
	* For a parameter with ordered values, the order may be explicitly specified.
	* In this case, this method returns the values of the parameter in the
	* prescribed order.
	*/
	@Override
	public Vector getValueOrder() {
		return order;
	}

	/**
	* For a parameter with ordered values, the order may be explicitly specified.
	* This method sets the desired order of the values of the parameter.
	*/
	public void setValueOrder(Vector values) {
		order = values;
	}
}