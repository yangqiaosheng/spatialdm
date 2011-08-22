package spade.vis.spec;

import java.util.Vector;

/**
* Used for saving and restoring "snapshots", i.e. system states. Contains
* information about a single tool dealing with time-dependent data that is (was)
* working at the moment of state saving.
*/
public class TemporalToolSpec extends ToolSpec {
	/**
	* Specifications of the attributes the tool deals with. Each element of
	* the vector is an instance of spade.vis.spec.AnimationAttrSpec.
	*/
	public Vector attrSpecs = null;
}
