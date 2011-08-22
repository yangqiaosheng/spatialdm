package spade.analysis.classification;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import spade.analysis.transform.AttributeTransformer;
import spade.analysis.transform.TransformerOwner;
import spade.lib.util.NumRange;
import spade.lib.util.StringUtil;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.DataItem;
import spade.vis.database.DataTreater;
import spade.vis.database.ObjectContainer;
import spade.vis.database.ThematicDataItem;
import spade.vis.dataview.TransformedDataPresenter;
import spade.vis.spec.ToolSpec;

/**
* This is a basic class for all specific classification methods that do
* classification on the basis of data stored in a table (AttributeDataPortion).
* Listens to changes in the table: data editing, adding, and removing.
*/

public abstract class TableClassifier extends Classifier implements DataTreater, PropertyChangeListener, TransformerOwner, TransformedDataPresenter {
	protected AttributeDataPortion data = null;
	/**
	* A TableClassifier may optionally be connected to a transformer of attribute
	* values. In this case, it classifies objects according to transformed
	* attribute values.
	*/
	protected AttributeTransformer aTrans = null;

	/**
	* The vector of identifiers of attributes used for the classification.
	*/
	protected Vector attr = null;
	/**
	* Some attributes may be dependent on a temporal parameter, and the visualizer
	* may be animated. In this case, the vector attr contains identifiers of
	* super-attributes, and the vector subAttr contains lists of identifiers of
	* the children of these super-attributes for different values of the temporal
	* parameter.
	*/
	protected Vector subAttr = null;
	/**
	* If there are time-dependent attributes, they may also refer to different
	* values of other, non-temporal parameters. For such attributes, this
	* vector contains "invariants" - strings indicating the values of the
	* additional parameters.
	*/
	protected Vector invariants = null;
	/**
	* If at least one of the visualized attributes has sub-attributes (i.e.
	* depends on a parameter), this variable specifies the index of the
	* subattribute to be currently taken for the visualization.
	*/
	protected int subAttrIdx = 0;
	/**
	* For performance optimization, keeps attribute indices in the table (column
	* numbers)
	*/
	protected int colNs[] = null;

	public void setTable(AttributeDataPortion table) {
		if (data != null) {
			if (data.equals(table))
				return;
			data.removePropertyChangeListener(this);
		}
		data = table;
		if (data != null) {
			data.addPropertyChangeListener(this);
			if (data instanceof ObjectContainer) {
				setObjectContainer((ObjectContainer) data);
			}
		}
	}

	public AttributeDataPortion getTable() {
		return data;
	}

	/**
	* Checks if this visualization method is applicable to the specified number
	* of attributes having the specified types. The possible types see in
	* @see spade.vis.database.AttributeTypes
	*/
	public abstract boolean isApplicable(int attrNumber, char attrTypes[]);

	/**
	* Sets the attributes to be used for classification
	*/
	public void setAttributes(Vector attributes) {
		attr = attributes;
		subAttr = null;
		invariants = null;
		colNs = null;
	}

	/**
	* Returns the list of attributes being used for the classification.
	*/
	public Vector getAttributes() {
		return attr;
	}

	/**
	 * Returns the collective name of the classes (may be shown in the class manipulation field)
	 */
	@Override
	public String getName() {
		if (name != null)
			return name;
		if (attr == null || attr.size() < 1)
			return null;
		if (attr.size() == 1) {
			name = data.getAttributeName((String) attr.elementAt(0));
		} else {
			name = "Classes by " + data.getAttributeName((String) attr.elementAt(0));
			for (int i = 1; i < attr.size(); i++) {
				name += "; " + data.getAttributeName((String) attr.elementAt(i));
			}
		}
		return name;
	}

	/**
	* There may bet attributes, including temporal, depending on different
	* values of (non-temporal) parameters. For such attributes, this
	* vector contains "invariants" - strings indicating the values of the
	* additional parameters. This method sets the invariant for a group of
	* "sub-attributes" corresponding to the attribute with the given index.
	*/
	public void setInvariant(String inv, int attrIdx) {
		if (inv == null || attrIdx < 0 || attr == null || attrIdx >= attr.size())
			return;
		if (invariants == null) {
			invariants = new Vector(attr.size(), 1);
		}
		for (int i = invariants.size(); i < attr.size(); i++) {
			invariants.addElement(null);
		}
		invariants.setElementAt(inv, attrIdx);
	}

	/**
	* Returns the identifier of the attribute with the given index. If this is
	* a parameter-dependent attribute, returns its identifier extended with the
	* attribute's invariant (an invariant indicates attribute's reference to
	* particular parameter values).
	*/
	public String getInvariantAttrId(int attrIdx) {
		if (attr == null || attrIdx < 0 || attrIdx >= attr.size())
			return null;
		String id = (String) attr.elementAt(attrIdx);
		if (id == null)
			return null;
		if (invariants != null && invariants.size() > attrIdx && invariants.elementAt(attrIdx) != null) {
			id += (String) invariants.elementAt(attrIdx);
		}
		return id;
	}

	/**
	* Some attributes may be dependent on parameters. This method associates such
	* an attribute with a list of identifiers of the children attributes. The
	* attribute is specified through its index in the list of the attributes
	* handled by this visualizer.
	*/
	public void setSubAttributes(Vector sub, int attrIdx) {
		if (attr == null || attrIdx < 0 || attrIdx >= attr.size())
			return;
		if (subAttr == null) {
			subAttr = new Vector(attr.size(), 5);
		}
		while (subAttr.size() <= attrIdx) {
			subAttr.addElement(null);
		}
		subAttr.setElementAt(sub, attrIdx);
		colNs = null;
	}

	/**
	* Some attributes may be dependent on parameters. This method returns
	* a list of identifiers of the children attributes of a parameter-dependent
	* attribute specified through its index in the list of the attributes
	* handled by this visualizer.
	*/
	public Vector getSubAttributes(int attrIdx) {
		if (attr == null || subAttr == null || attrIdx < 0 || attrIdx >= attr.size() || attrIdx >= subAttr.size())
			return null;
		return (Vector) subAttr.elementAt(attrIdx);
	}

	/**
	* Informs if there are any attributes with subattributes, i.e. depending on
	* parameters.
	*/
	public boolean hasSubAttributes() {
		if (subAttr == null)
			return false;
		for (int i = 0; i < subAttr.size(); i++)
			if (subAttr.elementAt(i) != null)
				return true;
		return false;
	}

	/**
	* Sets the index of the subattribute to be currently taken for the
	* visualization.
	*/
	public void setCurrentSubAttrIndex(int idx) {
		subAttrIdx = idx;
		colNs = null;
		objClassNumbers = null;
	}

	/**
	* Returns the index of the subattribute currently used for the
	* visualization.
	*/
	public int getCurrentSubAttrIndex() {
		return subAttrIdx;
	}

	/**
	* A method from the DataTreater interface.
	* Returns the list of attributes being visualized. If there are no
	* parameter-dependent attributes, returns the same as getAttributes().
	* Otherwise, takes the identifiers of the current sub-attributes,
	* depending of the value of the variable subAttrIdx.
	*/
	@Override
	public Vector getAttributeList() {
		if (attr == null)
			return null;
		Vector v = (Vector) attr.clone();
		if (subAttr != null) {
			for (int i = 0; i < subAttr.size(); i++)
				if (subAttr.elementAt(i) != null) {
					Vector sub = (Vector) subAttr.elementAt(i);
					if (sub.size() > subAttrIdx) {
						v.setElementAt(sub.elementAt(subAttrIdx), i);
					} else if (sub.size() > 0) {
						v.setElementAt(sub.elementAt(0), i);
					}
				}
		}
		return v;
	}

	/**
	* Returns the identifier of the set of objects this classifier deals with
	*/
	@Override
	public String getEntitySetIdentifier() {
		if (data == null)
			return null;
		return data.getEntitySetIdentifier();
	}

	/**
	 * Returns the identifier of the container this classifier deals with
	 */
	@Override
	public String getContainerIdentifier() {
		if (data == null)
			return null;
		return data.getContainerIdentifier();
	}

	/**
	 * Returns the container in which the objects are classified
	 */
	@Override
	public ObjectContainer getObjectContainer() {
		if (data instanceof ObjectContainer)
			return (ObjectContainer) data;
		return super.getObjectContainer();
	}

	/**
	* Returns the identifier of the attribute with the given index. If this is
	* a parameter-dependent attribute, returns the identifier of the current
	* sub-attribute.
	*/
	public String getAttrId(int attrIdx) {
		if (attr == null || attrIdx < 0 || attrIdx >= attr.size())
			return null;
		if (subAttr == null || attrIdx >= subAttr.size() || subAttr.elementAt(attrIdx) == null)
			return (String) attr.elementAt(attrIdx);
		Vector sub = (Vector) subAttr.elementAt(attrIdx);
		if (sub.size() > subAttrIdx)
			return (String) sub.elementAt(subAttrIdx);
		if (sub.size() > 0)
			return (String) sub.elementAt(0);
		return (String) attr.elementAt(attrIdx);
	}

	public String getAttributeName(int attrIdx) {
		if (attr == null || attrIdx < 0 || attrIdx >= attr.size())
			return null;
		String name = data.getAttributeName((String) attr.elementAt(attrIdx));
		if (name != null && invariants != null && invariants.size() > attrIdx && invariants.elementAt(attrIdx) != null) {
			name += (String) invariants.elementAt(attrIdx);
		}
		return name;
	}

	/**
	* Returns the index of the given attribute identifier in the list of identifiers
	*/
	public int getAttrIndex(String attrId) {
		if (attr == null || attr.size() < 1)
			return -1;
		if (attrId == null)
			if (attr.elementAt(0) == null)
				return 0;
			else
				return -1;
		int idx = StringUtil.indexOfStringInVectorIgnoreCase(attrId, attr);
		if (idx >= 0)
			return idx;
		//possibly, this identifier has been modified using an invariant
		if (invariants != null) {
			for (int i = 0; i < invariants.size(); i++) {
				String inv = (String) invariants.elementAt(i);
				if (inv != null && attrId.equalsIgnoreCase((String) attr.elementAt(i) + inv))
					return i;
			}
		}
		if (subAttr == null)
			return -1;
		for (int i = 0; i < subAttr.size(); i++)
			if (subAttr.elementAt(i) != null) {
				int k = StringUtil.indexOfStringInVectorIgnoreCase(attrId, (Vector) subAttr.elementAt(i));
				if (k >= 0)
					if (attr.elementAt(0) == null)
						return i + 1;
					else
						return i;
			}
		return -1;
	}

	/**
	* A convenience method for determining the number of the column corresponding
	* to the attribute with the given index. If attrIdx is an index of a
	* parameter-dependent attribute, i.e. the one having sub-attributes, the
	* column number of the current sub-attribute is returned, depending on the
	* value of the variable subAttrIdx.
	*/
	public int getAttrColumnN(int attrIdx) {
		if (data == null || attr == null || attrIdx < 0 || attrIdx >= attr.size())
			return -1;
		if (colNs != null)
			return colNs[attrIdx];
		colNs = new int[attr.size()];
		int start = 0;
		if (attr.elementAt(0) == null) {
			colNs[0] = -1;
			start = 1;
		}
		for (int i = start; i < attr.size(); i++)
			if (subAttr == null || i >= subAttr.size() || subAttr.elementAt(i) == null) {
				colNs[i] = data.getAttrIndex((String) attr.elementAt(i));
			} else {
				Vector sub = (Vector) subAttr.elementAt(i);
				if (sub.size() > subAttrIdx) {
					colNs[i] = data.getAttrIndex((String) sub.elementAt(subAttrIdx));
				} else if (sub.size() > 0) {
					colNs[i] = data.getAttrIndex((String) sub.elementAt(0));
				} else {
					colNs[i] = data.getAttrIndex((String) attr.elementAt(i));
				}
			}
		return colNs[attrIdx];
	}

	/**
	* Informs whether this visualizer allows the visualized attributes to
	* be transformed. By default, returns true.
	*/
	public boolean getAllowTransform() {
		return true;
	}

	/**
	* Connects the DataPresenter to a transformer of attribute values. After this,
	* it must represent transformed attribute values. The argument listenChanges
	* determines whether the visualizer will listen to the changes of the transformed
	* values and appropriately reset itself. This is not always desirable; for
	* example, a visualizer may be a part of another visualizer, which makes
	* all necessary changes.
	*/
	public void setAttributeTransformer(AttributeTransformer transformer, boolean listenChanges) {
		if (aTrans != null) {
			aTrans.removePropertyChangeListener(this);
		}
		aTrans = transformer;
		if (aTrans != null) {
			if (listenChanges) {
				aTrans.addPropertyChangeListener(this);
			}
		}
	}

	/**
	* Returns its transformer of attribute values (if exists)
	*/
	@Override
	public AttributeTransformer getAttributeTransformer() {
		return aTrans;
	}

	/**
	* Informs whether this visualizer allows each of the visualized attributes to
	* be transformed individually (returns true) or requires all attributes to
	* be transformed in the same way (returns false). By default, returns true.
	*/
	public boolean getAllowTransformIndividually() {
		return true;
	}

	/**
	* A convenience method for retrieving a value of a numeric attribute with the
	* given index from the given ThematicDataItem. If attrIdx is an index of a
	* parameter-dependent attribute, i.e. the one having sub-attributes, the
	* value of the current sub-attribute is retrieved, depending on the value of
	* the variable subAttrIdx.
	*/
	public double getNumericAttrValue(ThematicDataItem data, int attrIdx) {
		if (data == null)
			return Double.NaN;
		int colN = getAttrColumnN(attrIdx);
		if (colN < 0)
			return Double.NaN;
		if (aTrans != null)
			return aTrans.getNumericAttrValue(colN, data);
		return data.getNumericAttrValue(colN);
	}

	/**
	* A convenience method for retrieving a numeric value from the specified row
	* and column of the table. If the classifier is attached to an attribute
	* transformer, the value is taken from the transformer.
	*/
	public double getNumericAttrValue(int colN, int rowN) {
		if (aTrans != null)
			return aTrans.getNumericAttrValue(colN, rowN);
		if (data != null)
			return data.getNumericAttrValue(colN, rowN);
		return Double.NaN;
	}

	/**
	* A convenience method for retrieving a value of a non-numeric attribute with
	* the given index from the given ThematicDataItem. If attrIdx is an index of a
	* parameter-dependent attribute, i.e. the one having sub-attributes, the
	* value of the current sub-attribute is retrieved, depending on the value of
	* the variable subAttrIdx.
	*/
	public String getStringAttrValue(ThematicDataItem data, int attrIdx) {
		if (data == null)
			return null;
		int colN = getAttrColumnN(attrIdx);
		if (colN < 0)
			return null;
		return data.getAttrValueAsString(colN);
	}

	/**
	* A convenience method for retrieving a value of the attribute with
	* the given index from the given ThematicDataItem. If attrIdx is an index of a
	* parameter-dependent attribute, i.e. the one having sub-attributes, the
	* value of the current sub-attribute is retrieved, depending on the value of
	* the variable subAttrIdx.
	*/
	public Object getAttrValue(ThematicDataItem data, int attrIdx) {
		if (data == null)
			return null;
		int colN = getAttrColumnN(attrIdx);
		if (colN < 0)
			return null;
		return data.getAttrValue(colN);
	}

	/**
	* Determines the value range of the attribute with the given index. If
	* the attribute is dependent on parameters, determines the value range for all
	* parameter values.
	*/
	public NumRange getAttrValueRange(int attrIdx) {
		if (data == null || attr == null || attrIdx < 0 || attrIdx >= attr.size())
			return null;
		if (subAttr == null || subAttr.size() <= attrIdx || subAttr.elementAt(attrIdx) == null)
			if (aTrans != null)
				return aTrans.getAttrValueRange((String) attr.elementAt(attrIdx));
			else
				return data.getAttrValueRange((String) attr.elementAt(attrIdx));
		if (aTrans != null)
			return aTrans.getAttrValueRange((Vector) subAttr.elementAt(attrIdx));
		return data.getAttrValueRange((Vector) subAttr.elementAt(attrIdx));
	}

	/**
	* Retrieves all (different and not null) values of the attribute with the
	* given identifier. If the attribute is dependent on parameters, retrieves the
	* attribute values for all parameter values.
	*/
	public Vector getAllAttrValues(int attrIdx) {
		if (data == null || attr == null || attrIdx < 0 || attrIdx >= attr.size())
			return null;
		if (subAttr == null || subAttr.size() <= attrIdx || subAttr.elementAt(attrIdx) == null)
			return data.getAllAttrValuesAsStrings((String) attr.elementAt(attrIdx));
		return data.getAllAttrValuesAsStrings((Vector) subAttr.elementAt(attrIdx));
	}

	/**
	* Prepares its internal variables to the classification.
	*/
	public abstract void setup();

	/**
	* Determines the number of the table record referring to the object with the
	* given identifier
	*/
	public int getObjectRecordIdx(String objId) {
		if (data == null)
			return -1;
		return data.indexOf(objId);
	}

	public int getNRecords() {
		if (data == null)
			return 0;
		return data.getDataItemCount();
	}

	/**
	* Determines the class the record with the given number belongs to
	*/
	public int getRecordClass(int recN) {
		if (recN < 0)
			return -1;
		if (objClassNumbers == null || objClassNumbers.length != oCont.getObjectCount()) {
			int oClNum[] = new int[oCont.getObjectCount()];
			for (int i = 0; i < oClNum.length; i++) {
				oClNum[i] = getRecordClass((ThematicDataItem) data.getDataItem(i));
			}
			objClassNumbers = oClNum;
		}
		if (recN >= objClassNumbers.length)
			return -1;
		return objClassNumbers[recN];
	}

	/**
	* Determines the class for the given ThematicDataItem
	*/
	public abstract int getRecordClass(ThematicDataItem dit);

	/**
	* Returns the color for the given ThematicDataItem.
	*/
	public Color getColorForRecord(ThematicDataItem dit) {
		if (dit == null)
			return null;
		if (dit.getIndexInContainer() >= 0)
			return getColorForObject(dit.getIndexInContainer());
		int classN = getRecordClass(dit);
		if (classN < 0)
			return null;
		if (isClassHidden(classN))
			return hiddenClassColor;
		return getClassColor(classN);
	}

	/**
	* Returns the color for the given data item, depending on the current
	* classification. The data item belongs to the container with the identifier
	* passed as the argument containerId. The Classifier can check whether
	* the given data item is relevant for it, i.e. belongs to the table
	* this classifier works with.
	*/
	@Override
	public Color getColorForDataItem(DataItem dit, String containerId) {
		if (dit == null)
			return null;
		if (containerId != null && !containerId.equals(data.getContainerIdentifier()))
			return getColorForObject(dit.getId());
		if (dit.getIndexInContainer() >= 0)
			return getColorForObject(dit.getIndexInContainer());
		if (!(dit instanceof ThematicDataItem))
			return getColorForObject(dit.getId());
		return getColorForRecord((ThematicDataItem) dit);
	}

	/**
	* Returns the color for the given data item, depending on the current
	* classification. Does not check whether the item belongs to the table
	* this classifier works with.
	*/
	@Override
	public Color getColorForDataItem(DataItem dit) {
		if (dit == null)
			return null;
		if (dit.getIndexInContainer() >= 0)
			return getColorForObject(dit.getIndexInContainer());
		if (!(dit instanceof ThematicDataItem))
			return getColorForObject(dit.getId());
		return getColorForRecord((ThematicDataItem) dit);
	}

	/**
	* Returns the color for the record with the given number.
	*/
	public Color getColorForRecord(int recN) {
		return getColorForObject(recN);
	}

	/**
	* Determines the class the object with the given identifier belongs to
	*/
	@Override
	public int getObjectClass(String objId) {
		int recN = getObjectRecordIdx(objId);
		if (recN < 0)
			return -1;
		return getRecordClass(recN);
	}

	/**
	 * Returns the number of the class for the object with the given index in the container.
	 */
	@Override
	public int getObjectClass(int objIdx) {
		if (objIdx < 0)
			return -1;
		return getRecordClass(objIdx);
	}

	/**
	* A method from the DataTreater interface.
	* Replies whether it is linked to the data set (table) with the given identifier
	*/
	@Override
	public boolean isLinkedToDataSet(String setId) {
		return setId != null && data != null && setId.equals(data.getContainerIdentifier());
	}

	/**
	* A method from the DataTreater interface.
	* Must return a vector of colors used for representation of the attributes
	* this Data Treater deals with. By default, returns null.
	*/
	@Override
	public Vector getAttributeColors() {
		return null;
	}

	/**
	* Returns the total number of objects (table records) being classified
	*/
	@Override
	public int getSetSize() {
		return getNRecords();
	}

	/**
	* Returns true if the attribute with the given identifier is used for
	* classification
	*/
	protected boolean isAttributeUsed(String attrId) {
		int idx = this.getAttrIndex(attrId);
		return idx >= 0;
	}

	/**
	* Replies whether attributes with null values should be shown in data popups.
	*/
	public boolean getShowAttrsWithNullValues() {
		return true;
	}

	/**
	* Checks if new values appeared in data and if this affects the classes
	*/
	protected abstract void checkValues();

	/**
	 * Must check if the colors corresponding to attribute values have been
	 * changed by an external actor. By default, does nothing.
	 */
	public void getValueColorsFromAttribute() {
	}

	/**
	* Reacts to changes in the table: data editing, adding, and removing.
	*/
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		/*
		System.out.println("TableClassifier received event from "+e.getSource()+":\n"+
		                   e.getPropertyName()+" "+e.getOldValue()+" "+e.getNewValue());
		*/
		if (e.getSource().equals(data)) {
			if (e.getPropertyName().equals("destroyed") || e.getPropertyName().equals("structure_complete")) {
				destroy();
				return;
			}
			if (e.getPropertyName().equals("names") || e.getPropertyName().equals("new_attributes"))
				return;
			if (e.getPropertyName().equals("data_added") || e.getPropertyName().equals("data_removed") || e.getPropertyName().equals("data_updated")) {
				objClassNumbers = null;
				checkValues();
				notifyChange("classes");
			} else if (e.getPropertyName().equals("values") || e.getPropertyName().equals("value_colors")) {
				Vector attr = (Vector) e.getNewValue();
				if (attr == null || attr.size() < 1)
					return;
				boolean changed = false;
				for (int i = 0; i < attr.size() && !changed; i++) {
					changed = isAttributeUsed((String) attr.elementAt(i));
				}
				if (changed) {
					if (e.getPropertyName().equals("values")) {
						checkValues();
						notifyChange("classes");
					} else {
						getValueColorsFromAttribute();
						objClassNumbers = null;
						notifyColorsChange();
					}
				}
			}
		} else if (e.getSource().equals(aTrans)) {
			if (e.getPropertyName().equals("values")) {
				checkValues();
				notifyChange("classes");
			} else if (e.getPropertyName().equals("destroyed")) {
				destroy();
				return;
			}
		}
	}

	/**
	* Makes necessary operations for destroying, in particular, unregisters from
	* listening table change events. Must be destroyed!
	*/
	@Override
	public void destroy() {
		if (isDestroyed())
			return;
		if (data != null) {
			data.removePropertyChangeListener(this);
		}
		if (aTrans != null) {
			aTrans.removePropertyChangeListener(this);
		}
		super.destroy();
	}

	/**
	* A method from the TransformedDataPresenter interface.
	* Returns the transformed value corresponding to the given row and column of
	* the original table. If the value is not transformed, returns null.
	*/
	@Override
	public String getTransformedValue(int rowN, int colN) {
		if (aTrans != null)
			return aTrans.getTransformedValueAsString(rowN, colN);
		return null;
	}

	/**
	* Returns the specification of this classifier to be used for saving the
	* system's state.
	*/
	@Override
	public ToolSpec getSpecification() {
		ToolSpec spec = super.getSpecification();
		spec.table = data.getContainerIdentifier();
		spec.attributes = getAttributeList();
		if (aTrans != null) {
			spec.transformSeqSpec = aTrans.getSpecSequence();
		}
		return spec;
	}
}
