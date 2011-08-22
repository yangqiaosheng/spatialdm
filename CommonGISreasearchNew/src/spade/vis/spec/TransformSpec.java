package spade.vis.spec;

import java.io.Serializable;
import java.util.Hashtable;

/**
* Used for saving and restoring states of attribute transformers, which may be
* attached to maps or graphs. Contains a specification of a single transformer,
* with its specific properties (e.g. what transformation is applied and with
* what parameters).
*/
public class TransformSpec implements Serializable {
	/**
	* The identifier of the type of the transformer. It is necessary for the
	* re-construction of the transformer.
	*/
	public String methodId = null;
	/**
	* Custom properties of the transformer (depend on the type of the transformer).
	* String -> String
	*/
	public Hashtable properties = null;
}
