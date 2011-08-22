package spade.vis.spec;

import java.io.Serializable;
import java.util.Vector;

/**
* Contains information about an attribute used in an animated representation.
* Used for saving and restoring "snapshots", i.e. system states.
*/
public class AnimationAttrSpec implements Serializable {
	/**
	* The identifier of the attribute
	*/
	public String attribute = null;
	/**
	* The identifier of the super-attribute of this attribute, if exists
	*/
	public String parent = null;
	/**
	* Indicates whether the parent is time-dependent
	*/
	public boolean isTimeDependent = false;
	/**
	* For a time-dependent attribute, specifies the offset in relation to the
	* current time moment
	*/
	public int offset = 0;
	/**
	* Names of the non-temporal parameters with fixed user-selected values
	* this attribute refers to.
	*/
	public Vector fixedParams = null;
	/**
	* The fixed (user-selected) values of the non-temporal parameters
	* this attribute refers to.
	*/
	public Vector fixedParamVals = null;
}