package spade.analysis.transform;

import java.awt.Component;
import java.awt.Panel;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Hashtable;
import java.util.Vector;

import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.Line;
import spade.lib.util.DoubleArray;
import spade.lib.util.IntArray;
import spade.lib.util.NumRange;
import spade.lib.util.NumStat;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.ThematicDataItem;
import spade.vis.spec.TransformSequenceSpec;
import spade.vis.spec.TransformSpec;

/**
* This is the base class for classes that somehow transform numeric attribute
* values before visualizing them. For example, this class may implement
* various comparison modes for time-dependent attributes.
*/
public abstract class BaseAttributeTransformer implements AttributeTransformer, PropertyChangeListener, Destroyable {
	/**
	* The AttributeDataPortion (table) this transformer is attached to
	*/
	protected AttributeDataPortion table = null;

	/**
	* Several transformers may be combined. In this case, the second transformer
	* in a row must receive data from the first transformer rather than directly
	* from the table, the third transformer - from the second, and so on. Here
	* is a reference to the previous transformer in the row, which serves as
	* a data source for this transformer.
	*/
	protected BaseAttributeTransformer prevTrans = null;

	/**
	* A reference to the next transformer in a row (if any). This reference is
	* needed for combining the UIs of the transformers.
	*/
	protected BaseAttributeTransformer nextTrans = null;

	/**
	* The array of transformed values. The first index corresponds to the data
	* records in the source table and the second to the table columns to transform.
	*/
	protected double data[][] = null;

	/**
	* Contains the indexes of the original table columns to be transformed
	*/
	protected IntArray transColList = null;

	/**
	* The correspondence between the column numbers in the source table and the
	* columns of the array of the transformed values. The length of this array
	* normally equals the number of columns in the table, but may become less
	* if some columns have been added to the table after the transformer was
	* generated. If some column is not transformed, the corresponding array
	* element equals -1. This array is used as a complement to transColList for
	* efficiency reasons: it allows to avoid repeated searches in the list of
	* column numbers.
	*/
	protected int transColNs[] = null;

	/**
	* Allows or disallows individual transformation modes for each attribute.
	* By default is false, i.e. individual transformation is not allowed.
	*/
	protected boolean allowIndividualTransformation = false;

	/**
	* If individual transformation is allowed, this variable shows whether
	* this mode is currently on or off.
	*/
	protected boolean transformIndividually = false;

	/**
	* Indicates "destroyed" state
	*/
	protected boolean destroyed = false;

	/**
	* An AttributeTransformer may have listeners of changes of the transformed
	* values. The listeners should implement the PropertyChangeListener interface.
	* To handle the list of listeners and notify them about changes of the
	* object set or object data, the DataTable uses a PropertyChangeSupport.
	*/
	protected PropertyChangeSupport pcSupport = null;

	/**
	* Attaches the transformer to the AttributeDataPortion (table) with the data
	* to transform
	*/
	@Override
	public void setDataTable(AttributeDataPortion table) {
		setDataTable(table, true);
	}

	/**
	 * Attaches the transformer to the AttributeDataPortion (table) with the data
	 * to transform.
	 * The argument mustListen specifies whether the transformer must listen to changes
	 * of data in the table (this is necessary only for the first transformer in a row).
	 */
	public void setDataTable(AttributeDataPortion table, boolean mustListen) {
		if (this.table != null && this.table.equals(table))
			return;
		this.table = table;
		if (table != null && mustListen) {
			table.addPropertyChangeListener(this);
		}
		if (nextTrans != null) {
			nextTrans.setDataTable(table, false);
		}
	}

	/**
	* Returns the AttributeDataPortion (table) with the data it transforms
	*/
	@Override
	public AttributeDataPortion getDataTable() {
		return table;
	}

	/**
	* Attaches the transformer to the previous transformer in a row. The previous
	* transformer will be actually used as the data source for this transformer.
	* This allows to combine several transformers.
	*/
	@Override
	public void setPreviousTransformer(AttributeTransformer transformer) {
		if (prevTrans != null) {
			prevTrans.setNextTransformer(null);
		}
		prevTrans = (BaseAttributeTransformer) transformer;
		if (prevTrans != null) {
			prevTrans.setNextTransformer(this);
			if (table != null) {
				table.removePropertyChangeListener(this);
			}
		}
	}

	/**
	* Returns its reference to the previous transformer in a row of combined
	* transformers (if any).
	*/
	@Override
	public AttributeTransformer getPreviousTransformer() {
		return prevTrans;
	}

	/**
	* Sets a reference to the next transformer in a row (if any). This reference is
	* needed for combining the UIs of the transformers.
	*/
	public void setNextTransformer(AttributeTransformer transformer) {
		if (nextTrans != null) {
			nextTrans.removePropertyChangeListener(this);
		}
		nextTrans = (BaseAttributeTransformer) transformer;
		if (nextTrans != null) {
			nextTrans.addPropertyChangeListener(this);
		}
	}

	/**
	* Returns its reference to the next transformer in a row (if any).
	*/
	@Override
	public AttributeTransformer getNextTransformer() {
		return nextTrans;
	}

	/**
	* Returns the identifier (unique name) of this transformation method.
	*/
	@Override
	public abstract String getMethodName();

	/**
	* Returns the parameters (properties) of this transformation method so that
	* the state of the transformer may be completely restored.
	*/
	@Override
	public Hashtable getProperties() {
		Hashtable prop = new Hashtable();
		prop.put("allow_transform_individually", String.valueOf(getAllowIndividualTransformation()));
		if (getAllowIndividualTransformation()) {
			prop.put("transform_individually", String.valueOf(getTransformIndividually()));
		}
		return prop;
	}

	/**
	* Sets the parameters (properties) of this transformation method.
	* In the base transformer class, this method does nothing.
	*/
	@Override
	public void setProperties(Hashtable prop) {
		if (prop == null)
			return;
		Object obj = prop.get("allow_transform_individually");
		if (obj != null) {
			allowIndividualTransformation = Boolean.valueOf(obj.toString()).booleanValue();
		}
		if (allowIndividualTransformation) {
			obj = prop.get("transform_individually");
			if (obj != null) {
				transformIndividually = Boolean.valueOf(obj.toString()).booleanValue();
			}
		}
	}

	/**
	* Returns the specification of this single attribute transformer, with no
	* regard of the other transformers in the sequence.
	*/
	@Override
	public TransformSpec getSpecification() {
		TransformSpec tsp = new TransformSpec();
		tsp.methodId = getMethodName();
		if (tsp.methodId == null)
			return null;
		tsp.properties = getProperties();
		return tsp;
	}

	/**
	* Returns the specification of the sequence of transformers including
	* this transformer and all the transformers following it.
	*/
	@Override
	public TransformSequenceSpec getSpecSequence() {
		Vector specList = addSpecToList(null);
		if (specList == null || specList.size() < 1)
			return null;
		TransformSequenceSpec tss = new TransformSequenceSpec();
		tss.transSp = specList;
		return tss;
	}

	/**
	* If the argument specSequence is not null, adds the specification of this
	* transformer to the end of the vector. If the argument is null, creates
	* a new vector and attaches its specification to this vector. Then, if there
	* is a reference to the next transformer in the sequence, requests the
	* next transformer to complete the vector of transformer specifications.
	* Returns the resulting vector. If the argument specSequence was not null,
	* it is returned as the result of the method.
	*/
	protected Vector addSpecToList(Vector specSequence) {
		TransformSpec tsp = getSpecification();
		if (tsp != null) {
			if (specSequence == null) {
				specSequence = new Vector(10, 10);
			}
			specSequence.addElement(tsp);
		}
		if (nextTrans != null)
			return nextTrans.addSpecToList(specSequence);
		else
			return specSequence;
	}

	/**
	 * Returns the description of the data transformation made. The description may be
	 * shown to the user. If no transformation has been made, returns null.
	 */
	@Override
	public String getDescription() {
		if (nextTrans != null)
			return nextTrans.getDescription();
		return null;
	}

	/**
	* The transformer is given a list of attribute identifiers to be transformed.
	* Possibly, not all attributes can be transformed by a specific Transformer.
	*/
	@Override
	public abstract void setAttributes(Vector attrIds);

	/**
	* Sets the columns (specified by their numbers) to be transformed by the given
	* transformer. The descendants must check, if necessary, whether each of the
	* columns is suitable for this particular kind of transformation. The base
	* class only checks whether all the columns are numeric.
	*/
	@Override
	public void setColumnNumbers(IntArray colNs) {
		transColList = null;
		transColNs = null;
		if (colNs == null || colNs.size() < 1 || table == null)
			return;
		transColList = new IntArray(colNs.size(), 1);
		for (int i = 0; i < colNs.size(); i++)
			if (table.isAttributeNumeric(colNs.elementAt(i))) {
				transColList.addElement(colNs.elementAt(i));
			}
		if (transColList.size() < 1) {
			transColList = null;
		} else {
			transColNs = new int[table.getAttrCount()];
			for (int i = 0; i < transColNs.length; i++) {
				transColNs[i] = -1;
			}
			for (int i = 0; i < transColList.size(); i++) {
				int idx = transColList.elementAt(i);
				transColNs[idx] = i;
			}
		}
		if (nextTrans != null) {
			nextTrans.setColumnNumbers(colNs);
		}
	}

	/**
	* Returns the identifiers of the attributes under transformation. If the
	* attributes have super-attributes, returns the identifiers of the
	* super-attributes.
	*/
	@Override
	public Vector getAttrIds() {
		if (table == null || transColList == null || transColList.size() < 1)
			return null;
		Vector v = new Vector(transColList.size(), 1);
		for (int i = 0; i < transColList.size(); i++) {
			Attribute at = table.getAttribute(transColList.elementAt(i));
			if (at != null) {
				if (at.getParent() != null) {
					at = at.getParent();
				}
				if (!v.contains(at.getIdentifier())) {
					v.addElement(at.getIdentifier());
				}
			}
		}
		if (v.size() < 1)
			return null;
		v.trimToSize();
		return v;
	}

	/**
	* Returns the column numbers corresponding to the attributes under
	* transformation.
	*/
	@Override
	public IntArray getColumnNumbers() {
		return (IntArray) transColList.clone();
	}

	/**
	* Returns the name of the attribute with the given index
	*/
	@Override
	public String getAttrName(String attrId) {
		if (table == null)
			return null;
		return table.getAttributeName(attrId);
	}

	/**
	* Informs whether the transformer has all necessary settings for the
	* transformation and whether the data given to it can be transformed.
	*/
	@Override
	public abstract boolean isValid();

	/**
	* Allows or disallows individual transformation modes for each attribute.
	* By default, individual transformation is not allowed.
	*/
	@Override
	public void setAllowIndividualTransformation(boolean value) {
		allowIndividualTransformation = value;
	}

	/**
	* Informs whether the attributes may be (potentially) transformed
	* individually.
	*/
	@Override
	public boolean getAllowIndividualTransformation() {
		return allowIndividualTransformation;
	}

	/**
	* If individual transformation is allowed, this method switches it on or off.
	*/
	@Override
	public void setTransformIndividually(boolean value) {
		transformIndividually = value;
	}

	/**
	* Informs whether the individual transformation mode is on or off.
	*/
	@Override
	public boolean getTransformIndividually() {
		return transformIndividually;
	}

	/**
	* Performs the necessary data transformation and puts the results in the
	* array data.
	*/
	@Override
	public abstract void doTransformation();

	/**
	* Returns the UI for setting the transformation parameters in this transformer.
	* If this is the first transformer in a row, combines the UIs of all the
	* transformers, otherwise returns its individual UI.
	*/
	@Override
	public Component getUI() {
		if (prevTrans != null || nextTrans == null)
			return getIndividualUI();
		Panel p = new Panel(new ColumnLayout());
		Component c = getIndividualUI();
		if (c != null) {
			p.add(c);
		}
		AttributeTransformer aTrans = nextTrans;
		while (aTrans != null) {
			p.add(new Line(false));
			c = aTrans.getIndividualUI();
			if (c != null) {
				p.add(c);
			}
			aTrans = aTrans.getNextTransformer();
		}
		return p;
	}

	/**
	* Returns the UI for setting the transformation parameters in this transformer.
	* Does not take into account the other transformers in a row..
	*/
	@Override
	public abstract Component getIndividualUI();

	/**
	* Gets the value of the attribute specified by its index (column number) either
	* from the table or from the previous transformer in a row of combined
	* transformers.
	*/
	protected double getOrigAttrValue(int attrN, int recN) {
		if (prevTrans != null)
			return prevTrans.getNumericAttrValue(attrN, recN, false);
		return table.getNumericAttrValue(attrN, recN);
	}

	/**
	* Gets the value of the attribute specified by its index (column number) either
	* from the table or from the previous transformer in a row of combined
	* transformers.
	*/
	protected double getOrigAttrValue(int attrN, ThematicDataItem dit) {
		if (prevTrans != null)
			return prevTrans.getNumericAttrValue(attrN, dit, false);
		return dit.getNumericAttrValue(attrN);
	}

	/**
	* Returns the numeric value corresponding to the specified table row (recN)
	* and column (attrN). This may be either a result of transformation or the
	* initial attribute value, if this column is not transformed. The names and
	* order of the arguments are kept for the compatibility with the corresponding
	* method in the interface AttributeDataPortion.
	*/
	@Override
	public double getNumericAttrValue(int attrN, int recN) {
		return getNumericAttrValue(attrN, recN, true);
	}

	/**
	* Returns the numeric value corresponding to the specified table row (recN)
	* and column (attrN).
	* The argument useNext determines whether this transformer must get the
	* value from the next transformer in the row.
	*/
	protected double getNumericAttrValue(int attrN, int recN, boolean useNext) {
		if (useNext && nextTrans != null)
			return nextTrans.getNumericAttrValue(attrN, recN, true);
		if (table == null)
			return Double.NaN;
		if (data == null || transColNs == null || attrN >= transColNs.length || transColNs[attrN] < 0)
			return getOrigAttrValue(attrN, recN);
		if (recN >= data.length) {
			doTransformation();
		}
		if (recN >= data.length)
			return getOrigAttrValue(attrN, recN);
		return data[recN][transColNs[attrN]];
	}

	/**
	* Returns the string representation of the transformed value corresponding to
	* the given row and column of the original table. If the value is not
	* transformed, returns the value returned by the next transformer or null.
	*/
	@Override
	public abstract String getTransformedValueAsString(int rowN, int colN);

	/**
	* Returns the numeric value corresponding to the specified column (attrN) of
	* the given data record from the table. This may be either a result of
	* transformation or the initial attribute value fronm the record, if this
	* column is not transformed.
	*/
	@Override
	public double getNumericAttrValue(int attrN, ThematicDataItem dit) {
		return getNumericAttrValue(attrN, dit, true);
	}

	/**
	* Returns the numeric value corresponding to the specified column (attrN) of
	* the given data record from the table.
	* The argument useNext determines whether this transformer must get the
	* value from the next transformer in the row.
	*/
	protected double getNumericAttrValue(int attrN, ThematicDataItem dit, boolean useNext) {
		if (useNext && nextTrans != null)
			return nextTrans.getNumericAttrValue(attrN, dit, true);
		if (dit == null || attrN < 0)
			return Double.NaN;
		if (data == null || transColNs == null || attrN >= transColNs.length || transColNs[attrN] < 0)
			return getOrigAttrValue(attrN, dit);
		int idx = dit.getIndexInContainer();
		if (idx < 0 || idx >= data.length)
			return Double.NaN;
		return data[idx][transColNs[attrN]];
	}

	/**
	* Determines the common value range in the columns with the specified numbers.
	*/
	@Override
	public NumRange getValueRangeInColumns(IntArray colNs) {
		return getValueRangeInColumns(colNs, true);
	}

	/**
	* Determines the common value range in the columns with the specified numbers.
	* The argument useNext determines whether this transformer must get the
	* value range from the next transformer in the row.
	*/
	protected NumRange getValueRangeInColumns(IntArray colNs, boolean useNext) {
		if (useNext && nextTrans != null)
			return nextTrans.getValueRangeInColumns(colNs, true);
		if (table == null || colNs == null || colNs.size() < 1)
			return null;
		if (data == null || transColNs == null)
			if (prevTrans != null)
				return prevTrans.getValueRangeInColumns(colNs, false);
			else
				return table.getValueRangeInColumns(colNs);
		NumRange r = new NumRange();
		int I = Math.min(data.length, table.getDataItemCount());
		for (int i = 0; i < I; i++) {
			for (int j = 0; j < colNs.size(); j++) {
				int cn = colNs.elementAt(j), k = (cn < transColNs.length) ? transColNs[cn] : -1;
				double val = (k < 0) ? getOrigAttrValue(cn, i) : data[i][k];
				if (!Double.isNaN(val)) {
					if (Double.isNaN(r.minValue) || r.minValue > val) {
						r.minValue = val;
					}
					if (Double.isNaN(r.maxValue) || r.maxValue < val) {
						r.maxValue = val;
					}
				}
			}
		}
		if (Double.isNaN(r.maxValue))
			return null;
		return r;
	}

	/**
	* Determines the value range of the numeric attribute with the given identifier.
	* If this is a super-attribute, processes all its children.
	*/
	@Override
	public NumRange getAttrValueRange(String attrId) {
		return getAttrValueRange(attrId, true);
	}

	/**
	* Determines the value range of the numeric attribute with the given identifier.
	* If this is a super-attribute, processes all its children.
	* The argument useNext determines whether this transformer must get the
	* value range from the next transformer in the row.
	*/
	protected NumRange getAttrValueRange(String attrId, boolean useNext) {
		if (useNext && nextTrans != null)
			return nextTrans.getAttrValueRange(attrId, true);
		if (attrId == null || table == null)
			return null;
		Vector v = new Vector(1, 1);
		v.addElement(attrId);
		return getAttrValueRange(v, false);
	}

	/**
	* Determines the common value range of the numeric attributes with the
	* specified identifiers.
	*/
	@Override
	public NumRange getAttrValueRange(Vector attrIds) {
		return getAttrValueRange(attrIds, true);
	}

	/**
	* Determines the common value range of the numeric attributes with the
	* specified identifiers.
	* The argument useNext determines whether this transformer must get the
	* value range from the next transformer in the row.
	*/
	protected NumRange getAttrValueRange(Vector attrIds, boolean useNext) {
		if (useNext && nextTrans != null)
			return nextTrans.getAttrValueRange(attrIds, true);
		if (attrIds == null || table == null)
			return null;
		return getValueRangeInColumns(table.getRelevantColumnNumbers(attrIds), false);
	}

	/**
	* Returns statistics for the numeric attribute specified through its column
	* index.
	*/
	@Override
	public NumStat getNumAttrStatistics(int attrN) {
		return getNumAttrStatistics(attrN, true);
	}

	/**
	* Returns statistics for the numeric attribute specified through its column
	* index.
	* The argument useNext determines whether this transformer must get the
	* statistics from the next transformer in the row.
	*/
	protected NumStat getNumAttrStatistics(int attrN, boolean useNext) {
		if (useNext && nextTrans != null)
			return nextTrans.getNumAttrStatistics(attrN, true);
		if (table == null)
			return null;
		if (data == null || transColNs == null || attrN >= transColNs.length || transColNs[attrN] < 0)
			if (prevTrans != null)
				return prevTrans.getNumAttrStatistics(attrN, false);
			else
				return table.getNumAttrStatistics(attrN);
		DoubleArray values = new DoubleArray(table.getDataItemCount(), 1);
		NumStat ns = new NumStat();
		ns.sum = 0.0f;
		for (int i = 0; i < table.getDataItemCount(); i++) {
			double val = data[i][transColNs[attrN]];
			if (Double.isNaN(val)) {
				continue;
			}
			++ns.nValues;
			ns.sum += val;
			if (Double.isNaN(ns.minValue) || ns.minValue > val) {
				ns.minValue = val;
			}
			if (Double.isNaN(ns.maxValue) || ns.maxValue < val) {
				ns.maxValue = val;
			}
			boolean inserted = false;
			for (int j = 0; j < values.size() && !inserted; j++)
				if (val < values.elementAt(j)) {
					values.insertElementAt(val, j);
					inserted = true;
				}
			if (!inserted) {
				values.addElement(val);
			}
		}
		if (ns.nValues < 1)
			return null;
		ns.mean = ns.sum / ns.nValues;
		if (ns.nValues < 3) {
			ns.median = (ns.minValue + ns.maxValue) / 2;
		} else {
			int k = ns.nValues / 2;
			if (ns.nValues % 2 == 0) {
				//System.out.println("N="+ns.nValues+", median between "+(k-1)+" and "+k);
				ns.median = (values.elementAt(k - 1) + values.elementAt(k)) / 2;
				--k;
			} else {
				//System.out.println("N="+ns.nValues+", median at "+k);
				ns.median = values.elementAt(k);
			}
			if (ns.nValues > 4) {
				int k1 = k / 2, k2 = ns.nValues - k1 - 1;
				if (k % 2 == 0) {
					ns.lowerQuart = values.elementAt(k1);
					ns.upperQuart = values.elementAt(k2);
				} else {
					ns.lowerQuart = (values.elementAt(k1) + values.elementAt(k1 + 1)) / 2;
					ns.upperQuart = (values.elementAt(k2 - 1) + values.elementAt(k2)) / 2;
				}
			}
		}
		return ns;
	}

	/**
	 * Indicates that the original data in the table changed, which required
	 * re-application of the data transformations
	 */
	protected boolean origDataChanged = false;

	/**
	* Reacts to changes of the data in the table
	*/
	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		if (pce.getSource().equals(table)) {
			if (pce.getPropertyName().equals("destroyed") || pce.getPropertyName().equals("structure_complete")) {
				destroy();
				return;
			}
			if (transColNs == null)
				return;
			if (pce.getPropertyName().equals("names") || pce.getPropertyName().equals("new_attributes"))
				return;
			boolean changed = false;
			if (pce.getPropertyName().equals("data_added") || pce.getPropertyName().equals("data_removed") || pce.getPropertyName().equals("data_updated")) {
				changed = true;
			} else if (pce.getPropertyName().equals("values")) {
				Vector attr = (Vector) pce.getNewValue();
				if (attr == null || attr.size() < 1)
					return;
				for (int i = 0; i < transColNs.length && !changed; i++) {
					changed = transColNs[i] >= 0 && attr.contains(table.getAttributeId(i));
				}
			}
			if (!changed)
				return;
			origDataChanged = true;
			doTransformation();
		} else if (pce.getSource().equals(nextTrans) && pce.getPropertyName().equals("values")) {
			notifyValuesChange();
		}
	}

	/**
	* Registeres a listener of changes of the transformed values. The
	* listener must implement the PropertyChangeListener interface.
	*/
	@Override
	public synchronized void addPropertyChangeListener(PropertyChangeListener l) {
		if (l == null)
			return;
		if (pcSupport == null) {
			pcSupport = new PropertyChangeSupport(this);
		}
		pcSupport.addPropertyChangeListener(l);
	}

	/**
	* Unregisteres a listener of changes of the transformed values.
	*/
	@Override
	public synchronized void removePropertyChangeListener(PropertyChangeListener l) {
		if (l == null || pcSupport == null)
			return;
		pcSupport.removePropertyChangeListener(l);
	}

	/**
	* The method used to notify all the listeners about changes of the transformed
	* values. The property name is "values".
	*/
	@Override
	public void notifyValuesChange() {
		if (pcSupport == null)
			return;
		Object newValue = (origDataChanged) ? "original_data" : null;
		origDataChanged = false;
		pcSupport.firePropertyChange("values", null, newValue);
	}

	/**
	* Stops listening to events from the table. Sends the property change event
	* with the name "destroyed" to its listeners.
	*/
	@Override
	public void destroy() {
		if (table != null) {
			table.removePropertyChangeListener(this);
		}
		if (prevTrans != null) {
			prevTrans.removePropertyChangeListener(this);
		}
		destroyed = true;
		if (pcSupport != null) {
			pcSupport.firePropertyChange("destroyed", null, null);
		}
	}

	@Override
	public boolean isDestroyed() {
		return destroyed;
	}
}
