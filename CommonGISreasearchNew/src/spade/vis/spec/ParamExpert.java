package spade.vis.spec;

import java.util.Vector;

import spade.vis.database.AttributeDataPortion;

/**
* The interface of a component that can create a description of table parameters
* (e.g. for storing in a project specification file)
*/
public interface ParamExpert {
	/**
	* For the given table, creates a description of its parameters relevant for
	* the specified columns. The columns are specified through their identifiers.
	* If no column identifiers are given, all table columns are considered.
	* Returns a vector of descriptions (instances of spade.vis.spec.CaptionParamDescription).
	*/
	public Vector describeParameters(AttributeDataPortion table, Vector colIds);
}