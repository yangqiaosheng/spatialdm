package spade.vis.database;

import java.awt.Color;
import java.util.Vector;

import spade.lib.color.CS;
import spade.lib.util.Frequencies;
import spade.lib.util.Named;
import spade.lib.util.StringUtil;
import spade.time.TimeMoment;

/**
* The class provides specification of an attribute, first of all its identifier
* and type. A dataset may contain attributes depending on parameters, for
* example, population numbers in different years. In such cases so-called
* "super-attributes" are created. In our example, "population number" is a
* super-attribute, and there are several "child attributes" corresponding to
* different values of the parameter "year". Child attributes have references
* to their parent super-attributes and one or more parameter-value pairs
* specifying which value of which parameter they correspond to. All child
* attributes of the same super-attribute must have one and the same type.
*/

public class Attribute implements Named, java.io.Serializable {

	protected String id = null, name = null; //id is obligatory, name is optional
	protected char type = AttributeTypes.character;
	/**
	* Values of an attribute may be temporal references. Possible meanings of such
	* temporal references are encoded by integer numbers.
	*/
	public static final int OCCURRED_AT = 0, VALID_FROM = 1, VALID_UNTIL = 2;
	/**
	 * If this is a temporal attribute, i.e. its values are time references,
	 * this variable indicates the meaning of these references.
	 */
	public int timeRefMeaning = OCCURRED_AT;
	/**
	* The origin of the attribute, i.e. how it was derived. This should be
	* one of the values defined in the class @see AttributeTypes
	* By default, the attribute is marked as being initially present in the dataset.
	*/
	public int origin = AttributeTypes.original;
	/**
	 * Indicates whether the attribute is periodic.
	 * Examples of periodic attributes:
	 *  - direction, in degrees, from 0 to 359 (360==0)
	 *  - time of the day, from 00:00:00 to 23:59:59
	 *  - day of the week, from 1 to 7
	 *  - month of the year, from 1 to 12
	 * By default, an attribute is assumed to be non-periodic
	 */
	protected boolean periodic = false;
	/**
	 * For a periodic attribute, the range of the period, specified by the start and end values.
	 * The values may be instances of Integer, Double, or TimeMoment
	 */
	protected Object periodStart = null, periodEnd = null;
	/**
	* A reference to the parent "super-attribute", if exists. Super-attributes are
	* created if a dataset contains attributes depending on parameters, for
	* example, population numbers in different years. In this case "population
	* number" is a super-attribute, and there are several "child attributes"
	* corresponding to different values of the parameter "year".
	*/
	protected Attribute parent = null;
	/**
	* One or more parameter-value pairs specifying which value of which parameter
	* they correspond to, for example, {"year","1995"}. The first element of each
	* pair is a string, while the second (i.e. parameter value) may be, in general,
	* any object.
	*/
	protected Vector paramval = null;
	/**
	* If this is a super-attribute, the vector contains references to the child
	* attributes of this super-attribute.
	*/
	protected Vector children = null;

	protected boolean IsClassification = false;
	protected String[] valueList = null;
	protected Color[] valueColors = null;

	/**
	* Constructs a new attribute descriptor with the given identifier and type.
	*/
	public Attribute(String identifier, char attrType) {
		id = identifier;
		setType(attrType);
	}

	/**
	 * Does not make a copy of the parent and children!
	 */
	public Attribute makeCopy() {
		Attribute at = new Attribute(id, type);
		at.setName(name);
		at.timeRefMeaning = timeRefMeaning;
		at.origin = origin;
		if (paramval != null && paramval.size() > 0) {
			for (int i = 0; i < paramval.size(); i++) {
				at.addParamValPair((Object[]) paramval.elementAt(i));
			}
		}
		if (valueList != null && valueColors != null) {
			at.setValueListAndColors(valueList, valueColors);
		}
		return at;
	}

	/**
	 * In the given list of attributes finds an attribute with the
	 * given identifier
	 */
	static public Attribute findAttrById(Vector attrList, String attrId) {
		if (attrList == null || attrList.size() < 1 || attrId == null)
			return null;
		for (int i = 0; i < attrList.size(); i++) {
			Attribute at = (Attribute) attrList.elementAt(i);
			if (attrId.equals(at.getIdentifier()))
				return at;
		}
		return null;
	}

	/**
	 * Creates a new list of attributes with copies of all original attributes.
	 * Reproduces all parent-child relationships, if any.
	 */
	static public Vector makeCopyOfAttributeList(Vector attrList) {
		if (attrList == null || attrList.size() < 1)
			return null;
		Vector copy = new Vector(attrList.size(), 20);
		for (int i = 0; i < attrList.size(); i++) {
			copy.addElement(((Attribute) attrList.elementAt(i)).makeCopy());
		}
		for (int i = 0; i < attrList.size(); i++) {
			Attribute at1 = (Attribute) attrList.elementAt(i);
			if (at1.hasChildren() || at1.getParent() != null) {
				Attribute at2 = (Attribute) copy.elementAt(i);
				if (at1.getParent() != null) {
					Attribute parent = findAttrById(copy, at1.getParent().getIdentifier());
					if (parent != null) {
						at2.setParent(parent);
					}
				}
				if (at1.hasChildren()) {
					for (int j = 0; j < at1.getChildrenCount(); j++) {
						Attribute child = findAttrById(copy, at1.getChild(j).getIdentifier());
						if (child != null) {
							at2.addChild(child);
						}
					}
				}
			}
		}
		return copy;
	}

	public char getType() {
		return type;
	}

	public void setType(char attrType) {
		if (AttributeTypes.isValidType(attrType)) {
			type = attrType;
		} else if (attrType == 'N') {
			type = AttributeTypes.real; //'N' stands for "numeric"
		} else {
			type = AttributeTypes.character;
		}
		if (children != null) {
			for (int i = 0; i < children.size(); i++) {
				((Attribute) children.elementAt(i)).setType(type);
			}
		}
	}

	public String getIdentifier() {
		return id;
	}

	public void setIdentifier(String attrId) {
		if (attrId != null) {
			id = attrId;
		}
	}

	/**
	* Returns the name of this attribute. If the name is not specified,
	* returns the identifier.
	*/
	@Override
	public String getName() {
		if (parent != null && paramval != null && paramval.size() > 0) {
			String namestr = parent.getName();
			for (int i = 0; i < paramval.size(); i++) {
				Object pair[] = (Object[]) paramval.elementAt(i);
				namestr += "; " + pair[0].toString() + "=" + pair[1].toString();
			}
			return namestr;
		}
		if (name == null) {
			name = id;
		}
		return name;
	}

	/**
	* Sets the name of the attribute to be shown to the user. The name is not the
	* same as the identifier. The latter may come, for example, from a database
	* and may be not understandable to the user. However, if the name of the
	* attribute is not specified, its identifier is returned as its name.
	*/
	public void setName(String attrName) {
		name = attrName;
	}

	/**
	* Reports whether the attribute is numeric, i.e. integer or real
	*/
	public boolean isNumeric() {
		return AttributeTypes.isNumericType(type);
	}

	/**
	* Reports whether the attribute is temporal, i.e. its values are time moments
	*/
	public boolean isTemporal() {
		return type == AttributeTypes.time;
	}

	/**
	 * Reports whether the attribute is periodic.
	 * Examples of periodic attributes:
	 *  - direction, in degrees, from 0 to 359 (360==0)
	 *  - time of the day, from 00:00:00 to 23:59:59
	 *  - day of the week, from 1 to 7
	 *  - month of the year, from 1 to 12
	 */
	public boolean isPeriodic() {
		return periodic;
	}

	/**
	 * Sets whether the attribute is periodic.
	 * Examples of periodic attributes:
	 *  - direction, in degrees, from 0 to 359 (360==0)
	 *  - time of the day, from 00:00:00 to 23:59:59
	 *  - day of the week, from 1 to 7
	 *  - month of the year, from 1 to 12
	 */
	public void setPeriodic(boolean periodic) {
		this.periodic = periodic;
	}

	/**
	 * For a periodic attribute, sets the range of the period, specified by the start and end values.
	 * The values may be instances of Integer, Double, or TimeMoment.
	 * Automatically sets the value of the variable 'periodic', which becomes true
	 * when both periodStart and periodEnd are not nulls.
	 */
	public void setPeriod(Object periodStart, Object periodEnd) {
		this.periodStart = periodStart;
		this.periodEnd = periodEnd;
		periodic = periodStart != null && periodEnd != null;
	}

	/**
	 * For a periodic attribute, returns the first (start) value of the period.
	 * The value may be an instance of Integer, Double, or TimeMoment
	 */
	public Object getPeriodStart() {
		return periodStart;
	}

	/**
	 * For a periodic attribute, returns the last (end) value of the period.
	 * The value may be an instance of Integer, Double, or TimeMoment
	 */
	public Object getPeriodEnd() {
		return periodEnd;
	}

	/**
	 * If this is a periodic attribute, computes and returns the period length.
	 * The length is computed by subtracting the start value from the end value
	 * and adding 1.
	 */
	public long getPeriodLength() {
		if (periodStart == null || periodEnd == null)
			return 0;
		if (periodStart instanceof Integer)
			return ((Integer) periodEnd).intValue() - ((Integer) periodStart).intValue() + 1;
		if (periodStart instanceof Double)
			return ((Double) periodEnd).longValue() - ((Double) periodStart).longValue() + 1;
		if (periodStart instanceof TimeMoment)
			return ((TimeMoment) periodEnd).subtract((TimeMoment) periodStart) + 1;
		return 0;
	}

	/**
	* Sets a reference to its parent super-attribute
	*/
	public void setParent(Attribute parent) {
		this.parent = parent;
	}

	/**
	* Returns the reference to its parent super-attribute
	*/
	public Attribute getParent() {
		return parent;
	}

	/**
	* Replies if this attribute has parameters, i.e. has at least one
	* parameter-value pair.
	*/
	public boolean hasParameters() {
		return paramval != null && paramval.size() > 0;
	}

	/**
	* Returns the number of parameters (parameter-value pairs).
	*/
	public int getParameterCount() {
		if (paramval == null)
			return 0;
		return paramval.size();
	}

	/**
	* Returns the parameter-value pair with the given index, or null if such a
	* pair does not exist.
	*/
	public Object[] getParamValPair(int idx) {
		if (paramval == null || idx < 0 || idx >= paramval.size())
			return null;
		return (Object[]) paramval.elementAt(idx);
	}

	/**
	* Returns the name of the parameter with the given index, or null if such a
	* parameter does not exist.
	*/
	public String getParamName(int idx) {
		Object pair[] = getParamValPair(idx);
		if (pair == null)
			return null;
		return pair[0].toString();
	}

	/**
	* Returns the value of the parameter with the given index, or null if such a
	* parameter does not exist.
	*/
	public Object getParamValue(int idx) {
		Object pair[] = getParamValPair(idx);
		if (pair == null)
			return null;
		return pair[1];
	}

	/**
	* Returns the index of the parameter with the given name, or -1 if such a
	* parameter does not exist.
	*/
	public int getParamIndex(String paramName) {
		if (paramval == null || paramval.size() < 1)
			return -1;
		for (int i = 0; i < paramval.size(); i++) {
			Object pair[] = (Object[]) paramval.elementAt(i);
			if (pair[0].toString().equalsIgnoreCase(paramName))
				return i;
		}
		return -1;
	}

	/**
	* Returns the value of the parameter with the given name, or null if such a
	* parameter does not exist.
	*/
	public Object getParamValue(String paramName) {
		if (paramval == null || paramval.size() < 1 || paramName == null)
			return null;
		for (int i = 0; i < paramval.size(); i++) {
			Object pair[] = (Object[]) paramval.elementAt(i);
			if (pair[0].toString().equalsIgnoreCase(paramName))
				return pair[1];
		}
		return null;
	}

	/**
	* Returns true if the parameter with the given name exists and has the
	* specified value.
	*/
	public boolean hasParamValue(String paramName, Object value) {
		if (paramval == null || paramval.size() < 1 || paramName == null || value == null)
			return false;
		for (int i = 0; i < paramval.size(); i++) {
			Object pair[] = (Object[]) paramval.elementAt(i);
			if (pair[0].toString().equalsIgnoreCase(paramName))
				return pair[1].equals(value);
		}
		return false;
	}

	/**
	* Returns true if the parameter with the given name exists
	*/
	public boolean hasParameter(String paramName) {
		if (paramval == null || paramval.size() < 1 || paramName == null)
			return false;
		for (int i = 0; i < paramval.size(); i++) {
			Object pair[] = (Object[]) paramval.elementAt(i);
			if (pair[0].toString().equalsIgnoreCase(paramName))
				return true;
		}
		return false;
	}

	/**
	* Adds the given parameter-value pair to the list of parameters.
	*/
	public void addParamValPair(Object pair[]) {
		if (pair == null || pair.length != 2 || pair[0] == null || pair[1] == null || !(pair[0] instanceof String))
			return;
		addParamValPair((String) pair[0], pair[1]);
	}

	/**
	* Adds the given parameter-value pair to the list of parameters.
	*/
	public void addParamValPair(String paramName, Object value) {
		if (paramName == null || value == null)
			return;
		if (paramval == null) {
			paramval = new Vector(2, 2);
		}
		int idx = getParamIndex(paramName);
		if (idx >= 0) {
			Object pair[] = getParamValPair(idx);
			pair[1] = value;
		} else {
			Object pair[] = { paramName, value };
			paramval.addElement(pair);
		}
	}

	/**
	* Replies if this attribute has children, i.e. is a super-attribute.
	*/
	public boolean hasChildren() {
		return children != null && children.size() > 0;
	}

	/**
	* Returns the number of children if this is a super-attribute, or 0.
	*/
	public int getChildrenCount() {
		if (children == null)
			return 0;
		return children.size();
	}

	/**
	* Returns the vector of children if this is a super-attribute, or null.
	*/
	public Vector getChildren() {
		return children;
	}

	/**
	* Returns the child with the given index, or null if such a child does not exist.
	*/
	public Attribute getChild(int idx) {
		if (children == null || idx < 0 || idx >= children.size())
			return null;
		return (Attribute) children.elementAt(idx);
	}

	/**
	* Returns the indexof the given attribute in the list of children. If it is
	* not there, returns -1.
	*/
	public int getChildIndex(Attribute attr) {
		if (children == null || attr == null)
			return -1;
		return children.indexOf(attr);
	}

	/**
	* Adds a child to the list of children
	*/
	public void addChild(Attribute attr) {
		if (attr == null)
			return;
		if (children == null) {
			children = new Vector(50, 10);
		}
		children.addElement(attr);
		attr.setType(type);
		attr.setParent(this);
	}

	/**
	* Removes the child with the given index, if exists.
	*/
	public void removeChild(int idx) {
		if (children == null || idx < 0 || idx >= children.size())
			return;
		Attribute attr = (Attribute) children.elementAt(idx);
		attr.setParent(null);
		children.removeElementAt(idx);
	}

	/**
	* Removes all the children.
	*/
	public void removeAllChildren() {
		if (children == null || children.size() < 1)
			return;
		for (int i = 0; i < children.size(); i++) {
			Attribute attr = (Attribute) children.elementAt(i);
			attr.setParent(null);
		}
		children.removeAllElements();
	}

	/**
	* Replies whether this attribute is dependent on a given parameter, i.e.
	* whether (1) this is a super-attribute; (2) among its children there are
	* at least 2 attributes referring to different values of this parameter.
	*/
	public boolean dependsOnParameter(Parameter par) {
		if (children == null || children.size() < 2 || par == null || par.getValueCount() < 2)
			return false;
		int nParValues = 0;
		String name = par.getName();
		for (int k = 0; k < par.getValueCount() && nParValues < 2; k++) {
			Object parValue = par.getValue(k);
			boolean found = false;
			for (int j = 0; j < children.size() && !found; j++) {
				found = ((Attribute) children.elementAt(j)).hasParamValue(name, parValue);
			}
			if (found) {
				++nParValues;
			}
		}
		return nParValues > 1;
	}

	/**
	* If this is a super-attribute, retrieves from its children the list of
	* all occurring names and values of parameters. The argument tblParams is a
	* vector of the parameters of the table (instances of the class Parameter)
	* the values of which need to be retrieved (in principle, not all table
	* parameters may be included in the vector).
	* Returns an array of vectors. In each vector the first element is the name of
	* a parameter, the rest are the values of this parameter occurring with
	* the children of this attribute.
	*/
	public Vector[] getAllParametersAndValues(Vector tblParams) {
		return getOtherParametersAndValues(tblParams, null);
	};

	/**
	* If this is a super-attribute, retrieves from its children dependent on the
	* specified parameters the list of all occurring names and values of other
	* parameters. Skips the children that do not depend on the specified
	* parameters. The argument tblParams is a vector of the parameters of the
	* table (instances of the class Parameter) the values of which need to be
	* retrieved (in principle, not all table parameters may be included).
	* Returns an array of vectors. In each vector the first element
	* is the name of a parameter, the rest are the values of this parameter
	* occurring with the children of this attribute.
	* The argument paramsMustHave contains the names of the mandatory parameters.
	*/
	public Vector[] getOtherParametersAndValues(Vector tblParams, Vector paramsMustHave) {
		if (tblParams == null || tblParams.size() < 1 || children == null || children.size() < 1)
			return null;
		if (paramsMustHave != null && paramsMustHave.size() < 1) {
			paramsMustHave = null;
		}
		if (paramsMustHave != null) {
			//remove from tblParams those parameters which are listed in paramsMustHave
			Vector t = new Vector(tblParams.size(), 1);
			for (int i = 0; i < tblParams.size(); i++) {
				Parameter par = (Parameter) tblParams.elementAt(i);
				if (!StringUtil.isStringInVectorIgnoreCase(par.getName(), paramsMustHave)) {
					t.addElement(par);
				}
			}
			if (t.size() < 1)
				return null;
			tblParams = t;
		}
		//for each table parameter, create a boolean array of value occurrences
		Vector vocc = new Vector(tblParams.size(), 1);
		int nOccur[] = new int[tblParams.size()];
		for (int i = 0; i < tblParams.size(); i++) {
			nOccur[i] = 0;
			Parameter par = (Parameter) tblParams.elementAt(i);
			boolean occ[] = new boolean[par.getValueCount()];
			for (int j = 0; j < occ.length; j++) {
				occ[j] = false;
			}
			vocc.addElement(occ);
		}
		//look through the children and register occurrences of parameter values
		for (int i = 0; i < children.size(); i++) {
			Attribute child = (Attribute) children.elementAt(i);
			//if this child noes not depend on the specified parameters (paramsMustHave),
			//it should be skipped
			if (paramsMustHave != null) {
				boolean ok = true;
				for (int j = 0; j < paramsMustHave.size() && ok; j++) {
					ok = child.getParamIndex((String) paramsMustHave.elementAt(j)) >= 0;
				}
				if (!ok) {
					continue;
				}
			}
			for (int j = 0; j < tblParams.size(); j++) {
				Parameter par = (Parameter) tblParams.elementAt(j);
				Object value = child.getParamValue(par.getName());
				if (value != null) {
					int idx = par.getValueIndex(value);
					if (idx >= 0) {
						boolean occ[] = (boolean[]) vocc.elementAt(j);
						if (!occ[idx]) {
							occ[idx] = true;
							++nOccur[j];
						}
					}
				}
			}
		}
		int npar = 0;
		for (int element : nOccur)
			if (element > 0) {
				++npar;
			}
		if (npar < 1)
			return null;
		Vector result[] = new Vector[npar];
		int k = 0;
		for (int i = 0; i < tblParams.size(); i++)
			if (nOccur[i] > 0) {
				result[k] = new Vector(nOccur[i], 1);
				Parameter par = (Parameter) tblParams.elementAt(i);
				result[k].addElement(par.getName());
				boolean occ[] = (boolean[]) vocc.elementAt(i);
				for (int j = 0; j < par.getValueCount(); j++)
					if (occ[j]) {
						result[k].addElement(par.getValue(j));
					}
				++k;
			}
		return result;
	}

	/**
	* Finds among its children the attribute having the same parameters and values
	* as the specified attribute (sample). If this attribute has no children, or
	* if the sample does not refer to any parameter, the method returns a
	* reference to "this".
	*/
	public Attribute findCorrespondingAttribute(Attribute sample) {
		if (sample == null || sample.hasChildren())
			return null;
		if (!hasChildren())
			return this;
		int nParInSample = sample.getParameterCount();
		if (nParInSample < 1)
			return this;
		for (int i = 0; i < children.size(); i++) {
			Attribute child = (Attribute) children.elementAt(i);
			if (child.getParameterCount() != nParInSample) {
				continue;
			}
			boolean ok = true;
			for (int j = 0; j < nParInSample && ok; j++) {
				Object pair[] = sample.getParamValPair(j);
				ok = child.hasParamValue((String) pair[0], pair[1]);
			}
			if (ok)
				return child;
		}
		return null;
	}

	public void setValueListAndColors(String[] valueList, Color[] valueColors) {
		this.valueList = valueList;
		this.valueColors = valueColors;
		IsClassification = true;
	}

	/**
	* Creates default color scheme for classification attribute
	* Attention! In case of boolean attributes can reorder values
	* Therefore it is necessary to use getValueList() afterwards
	*/
	public void setupDefaultColors() {
		if (valueList == null || valueList.length < 1)
			return;
		valueColors = new Color[valueList.length];
		boolean yesno = false;
		if (valueList.length == 2) {
			String s1 = valueList[0], s2 = valueList[1];
			if (s1.equalsIgnoreCase("F") || s1.equalsIgnoreCase("FALSE") || s1.equalsIgnoreCase("NO") || s1.equals("0")) {
				yesno = s2.equalsIgnoreCase("T") || s2.equalsIgnoreCase("TRUE") || s2.equalsIgnoreCase("YES") || s2.equals("1");
				if (yesno) {
					valueList[0] = s2;
					valueList[1] = s1;
				}
			} else {
				yesno = (s1.equalsIgnoreCase("T") || s1.equalsIgnoreCase("TRUE") || s1.equalsIgnoreCase("YES") || s1.equals("1")) && (s2.equalsIgnoreCase("F") || s2.equalsIgnoreCase("FALSE") || s2.equalsIgnoreCase("NO") || s2.equals("0"));
			}
		}
		if (yesno) {
			valueColors[0] = Color.red;
			valueColors[1] = Color.pink.darker();
		} else {
			for (int i = 0; i < valueList.length; i++) {
				valueColors[i] = CS.getNiceColorExt(i);
			}
		}
		IsClassification = true;
	}

	public boolean isClassification() {
		return IsClassification;
	}

	public int getNClasses() {
		return (valueList == null) ? 0 : valueList.length;
	}

	public String[] getValueList() {
		return valueList;
	}

	public Color[] getValueColors() {
		return valueColors;
	}

	public int getValueN(String val) {
		if (IsClassification) {
			for (int i = 0; i < valueList.length; i++)
				if (valueList[i].equals(val))
					return i;
		}
		return -1;
	}

	/*
	* Links derived attributes with source data
	*/
	protected Vector derivedFrom = null;

	/**
	* Stores the list of identifiers of attributes this attribute has been derived from
	*/
	public void setSourceAttributes(Vector attr) {
		derivedFrom = attr;
	}

	/**
	* Returns the list of identifiers of attributes this attribute has been derived from
	*/
	public Vector getSourceAttributes() {
		return derivedFrom;
	}

	public int getDerivedFromSize() {
		if (derivedFrom == null)
			return 0;
		return derivedFrom.size();
	}

	public String getDerivedFromID(int n) {
		if (n > getDerivedFromSize() - 1)
			return null;
		else
			return (String) derivedFrom.elementAt(n);
	}

	public void addDependency(String ID) {
		if (derivedFrom == null) {
			derivedFrom = new Vector(10, 10);
		}
		derivedFrom.addElement(ID);
	}

	/**
	 * Frequencies of attribute values (not necessarily counted in the table).
	 * The frequencies may be set externally when necessary; may also be null.
	 */
	protected Frequencies valueFrequencies = null;

	/**
	 * Returns the frequencies of attribute values if specified.
	 * The frequencies may be set externally when necessary; may also be null.
	 */
	public Frequencies getValueFrequencies() {
		return valueFrequencies;
	}

	/**
	 * Sets the frequencies of attribute values (not necessarily counted in the table).
	 */
	public void setValueFrequencies(Frequencies valueFrequencies) {
		this.valueFrequencies = valueFrequencies;
	}
}
