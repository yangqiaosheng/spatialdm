package spade.analysis.manipulation;

import spade.lib.util.FloatArray;

/**
* An interface to be implemented by classes that somehow use attribute
* weights assigned by the user in order to represent relative importance
* of the attributes. Thus, assignment of weights to attributes is common
* for many decision support methods. The interface allows to develop a reusable
* UI component for setting the weights.
*/
public interface AttrWeightUser {
	/**
	* Adds one more attribute to be manipulated to the current set
	* of the manipulated attributes.
	*/
	public void addAttribute(String attrId, String attrName);

	/**
	* Removes one of the attributes being currently manipulated.
	*/
	public void removeAttribute(String attrId);

	/**
	* Removes one of the attributes being currently manipulated.
	*/
	public void removeAttribute(int attrIdx);

	/**
	* Sets the weights of the attributes. A negative weight means a criterion
	* to be minimized
	*/
	public void setWeights(FloatArray weights);
}
