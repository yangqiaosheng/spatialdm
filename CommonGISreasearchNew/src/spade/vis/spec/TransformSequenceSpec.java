package spade.vis.spec;

import java.io.Serializable;
import java.util.Vector;

/**
* Used for saving and restoring states of sequences of attribute transformers,
* which may be attached to maps or graphs. Contains specifications of
* transformers arranged in a sequence.
*/
public class TransformSequenceSpec implements Serializable {
	/**
	* The sequence of descriptors of transformers, in the order as they are applied.
	*/
	public Vector transSp = null;
}
