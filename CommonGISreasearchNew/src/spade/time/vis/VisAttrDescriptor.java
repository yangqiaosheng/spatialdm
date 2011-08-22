package spade.time.vis;

import java.util.Vector;

import spade.lib.util.Named;
import spade.vis.database.Attribute;
import spade.vis.spec.AnimationAttrSpec;

/**
* Describes an attribute selected for an animated visualization: whether it is
* time-dependent, its super-attribute, user-selected values of other,
* non-temporal parameters.
*/
public class VisAttrDescriptor implements Named {
	/**
	* The super-attribute of this attribute, if exists
	*/
	public Attribute parent = null;
	/**
	* The attribute itself, if there is no parent
	*/
	public Attribute attr = null;
	/**
	* Attribute's identifier
	*/
	public String attrId = null;
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
	* Names of the other (non-temporal) parameters with fixed user-selected values
	* this attribute refers to.
	*/
	public Vector fixedParams = null;
	/**
	* Fixed (user-selected) values of the other (non-temporal) parameters
	* this attribute refers to.
	*/
	public Vector fixedParamVals = null;

	/**
	* Returns the name of the attribute. If needed, generates it using the
	* values of the fixed parameters and the offset.
	*/
	@Override
	public String getName() {
		if (parent == null)
			if (attr == null)
				return null;
			else
				return attr.getName();
		String name = parent.getName();
		if (offset == 0 && (fixedParams == null || fixedParams.size() < 1))
			return name;
		name += " (";
		if (offset != 0)
			if (offset > 0) {
				name += "t+" + offset;
			} else {
				name += "t-" + offset;
			}
		if (fixedParams != null) {
			for (int j = 0; j < fixedParams.size(); j++) {
				if (!name.endsWith("(")) {
					name += "; ";
				}
				name += (String) fixedParams.elementAt(j) + "=" + fixedParamVals.elementAt(j).toString();
			}
		}
		name += ")";
		return name;
	}

	/**
	* Returns the specification of this animation-involved attribute to be used
	* for saving the system's state.
	*/
	public AnimationAttrSpec getSpecification() {
		AnimationAttrSpec spec = new AnimationAttrSpec();
		if (attr != null) {
			spec.attribute = attr.getIdentifier();
		} else {
			spec.attribute = attrId;
		}
		if (parent != null) {
			spec.parent = parent.getIdentifier();
		}
		spec.isTimeDependent = isTimeDependent;
		spec.offset = offset;
		spec.fixedParams = fixedParams;
		spec.fixedParamVals = fixedParamVals;
		return spec;
	}
}