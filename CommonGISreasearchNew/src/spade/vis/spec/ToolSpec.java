package spade.vis.spec;

import java.awt.Rectangle;
import java.io.Serializable;
import java.util.Hashtable;
import java.util.Vector;

/**
* Used for saving and restoring "snapshots", i.e. system states. Contains
* information about a single tool that is (was) working at the moment of
* state saving.
*/
public class ToolSpec implements Serializable {
	/**
	* The tag to be used for storing this specification in a file. Every
	* specification is stored as a sequence of lines starting with <tagName>
	* and ending with </tagName>.
	*/
	public String tagName = null;
	/**
	* The identifier of the tool. It is necessary for tool construction.
	*/
	public String methodId = null;
	/**
	* The identifier of the chart.
	*/
	public String chartId = null;
	/**
	* The identifier of the table the tool works with
	*/
	public String table = null;
	/**
	* The list of the attributes from the table the tool works with
	*/
	public Vector attributes = null;
	/**
	* Describes the sequence of attribute transformers attached to the tool (if
	* any). Contains specifications of all transformers arranged in the sequence.
	*/
	public TransformSequenceSpec transformSeqSpec = null;
	/**
	* Where to put the tool, for example, "main_frame" or "graph_frame". May be
	* null - in this case the tool will be put in the default location.
	* For some tools may be irrelevant.
	*/
	public String location = null;
	/**
	* Tool location and size in screen coordinates. May be relevant, for example,
	* when the tool appears in an individual frame.
	*/
	public Rectangle bounds = null;
	/**
	* Custom properties of the tool.
	* String -> String
	*/
	public Hashtable properties = null;
}
