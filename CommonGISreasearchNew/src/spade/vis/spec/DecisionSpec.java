package spade.vis.spec;

/**
* Contains settings for decision making, e.g. type of decision, where to store
* the decisions, etc.
*/
public class DecisionSpec implements java.io.Serializable {
	/**
	* The file in which to store decision results
	*/
	public String resultFile = null;
	/**
	* The directory in which to store decision results
	*/
	public String resultDir = null;
	/**
	* The script to be used for storing decision results
	*/
	public String resultScript = null;
	/**
	* The type of decision: either "classification" or "ranking"
	*/
	public String decisionType = "classification";
}