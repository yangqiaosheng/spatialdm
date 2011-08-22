package spade.vis.spec;

import java.io.Serializable;
import java.util.Vector;

/**
* Contains information about a single animated representation that is (was)
* working at the moment of state saving. Used for saving and restoring
* "snapshots", i.e. system states.
*/
public class AnimatedVisSpec implements Serializable {
	/**
	* The identifier of the table supplying data for the animated representation.
	*/
	public String table = null;
	/**
	* Specifications of the attributes used in the animation. Each element of
	* the vector is an instance of spade.vis.spec.AnimationAttrSpec.
	*/
	public Vector attrSpecs = null;
	/**
	* The specification describing animation-irrelevant features of the
	* visualization.
	*/
	public ToolSpec visSpec = null;
}