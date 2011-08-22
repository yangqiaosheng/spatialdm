package spade.vis.database;

import java.util.Vector;

import spade.lib.util.Named;

/**
* Description of an attribute selected for visualisation, calculation, etc.
*/
public class AttrDescriptor implements Named {
	/**
	* The selected (top-level) attribute. 2 cases are possible:
	* 1) a single low-level attribute is selected, i.e. not depending on any
	*    parameters or with fixed values of all parameters. In this case, attr
	*    refers to this attribute, and all other data fields are null.
	* 2) an attribute depending on parameters is selected, and at least two
	*    combinations of parameter values are chosen. In this case, attr refers
	*    to the top-level attribute (parent). Additional information about
	*    parameter values must be provided in parVals, and the relevant
	*    low-level attributes listed in children.
	*/
	public Attribute attr = null;
	/**
	* Relevant parameters and their values. The elements of the array are vectors
	* where the first element is the parameter name and the remaining elements
	* are the selected parameter values (not necessarily all values of the
	* parameters). If a single low-level attribute is selected, this vector is
	* null.
	*/
	public Vector parVals[] = null;
	/**
	* Vector of low-level attributes corresponding to all selected combinations
	* of parameter values. The elements of the vector are instances of the class
	* Attribute.
	*/
	public Vector children = null;

	/**
	* Returns the name of the attribute, possibly, modified by adding values of
	* invariant parameters
	*/
	@Override
	public String getName() {
		if (attr == null)
			return null;
		String name = attr.getName();
		if (parVals == null)
			return name;
		for (Vector parVal : parVals)
			if (parVal.size() == 2) {
				name += " " + parVal.elementAt(0).toString() + " = " + parVal.elementAt(1).toString();
			}
		return name;
	}

	/**
	* Returns the number of varying parameters, i.e. those which have 2 or more
	* values in parVals
	*/
	public int getNVaryingParams() {
		if (parVals == null)
			return 0;
		int n = 0;
		for (Vector parVal : parVals)
			if (parVal.size() > 2) {
				++n;
			}
		return n;
	}

	/**
	* Returns true if the specified parameter is invariant, i.e. has only one
	* value for the columns described by this descriptors. If this parameter
	* is absent in ther parameter list, returns also true.
	*/
	public boolean isInvariantParameter(String parName) {
		if (parVals == null)
			return true;
		for (Vector parVal : parVals)
			if (parVal.elementAt(0).equals(parName))
				if (parVal.size() <= 2)
					return true;
				else
					return false;
		return true; //this parameter is not used at all!!!
	}

	/**
	* Finds among its "children" a child corresponding to the specified attribute
	* with regard to parameters and their values. If there are no children,
	* returns the top attribute.
	*/
	public Attribute findCorrespondingAttribute(Attribute sample) {
		if (sample == null || sample.hasChildren())
			return null;
		if (children == null || children.size() < 1)
			return attr;
		if (children.size() == 1)
			return (Attribute) children.elementAt(0);
		if (sample.getParameterCount() < 1)
			return attr;
		Vector pairs = new Vector(sample.getParameterCount(), 1);
		for (int j = 0; j < sample.getParameterCount(); j++) {
			Object pair[] = sample.getParamValPair(j);
			//if this is an invariant parameter, do not take it into account
			for (Vector parVal : parVals)
				if (parVal.elementAt(0).equals(pair[0])) {
					if (parVal.size() > 2) {
						pairs.addElement(pair);
					}
					break;
				}
		}
		if (pairs.size() < 1)
			return attr;
		for (int i = 0; i < children.size(); i++) {
			Attribute child = (Attribute) children.elementAt(i);
			boolean ok = true;
			for (int j = 0; j < pairs.size() && ok; j++) {
				Object pair[] = (Object[]) pairs.elementAt(j);
				ok = child.hasParamValue((String) pair[0], pair[1]);
			}
			if (ok)
				return child;
		}
		return null;
	}
}