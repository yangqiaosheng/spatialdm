package spade.analysis.transform;

import java.awt.Component;
import java.beans.PropertyChangeListener;
import java.util.Hashtable;
import java.util.Vector;

import spade.lib.util.IntArray;
import spade.lib.util.NumRange;
import spade.lib.util.NumStat;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.ThematicDataItem;
import spade.vis.spec.TransformSequenceSpec;
import spade.vis.spec.TransformSpec;

/**
* To be implemented by classes that somehow transform numeric attribute
* values before visualizing them. For example, a transformer may implement
* various comparison modes for time-dependent attributes.
*/
public interface AttributeTransformer {
	/**
	* Attaches the transformer to the AttributeDataPortion (table) with the data
	* to transform
	*/
	public void setDataTable(AttributeDataPortion table);

	/**
	* Returns the AttributeDataPortion (table) with the data it transforms
	*/
	public AttributeDataPortion getDataTable();

	/**
	* Attaches the transformer to the previous transformer in a row. The previous
	* transformer will be actually used as the data source for this transformer.
	* This allows to combine several transformers.
	*/
	public void setPreviousTransformer(AttributeTransformer transformer);

	/**
	* Returns its reference to the previous transformer in a row of combined
	* transformers (if any).
	*/
	public AttributeTransformer getPreviousTransformer();

	/**
	* Returns its reference to the next transformer in a row (if any).
	*/
	public AttributeTransformer getNextTransformer();

	/**
	* The transformer is given a list of attribute identifiers to be transformed.
	* Possibly, not all attributes can be transformed by a specific Transformer.
	*/
	public void setAttributes(Vector attrIds);

	/**
	* Returns the identifiers of the attributes under transformation. If the
	* attributes have super-attributes, returns the identifiers of the
	* super-attributes.
	*/
	public Vector getAttrIds();

	/**
	* Sets the columns (specified by their numbers) to be transformed by the given
	* transformer. The descendants must check, if necessary, whether each of the
	* columns is suitable for this particular kind of transformation. The base
	* class only checks whether all the columns are numeric.
	*/
	public void setColumnNumbers(IntArray colNs);

	/**
	* Returns the column numbers corresponding to the attributes under
	* transformation.
	*/
	public IntArray getColumnNumbers();

	/**
	* Returns the name of the (super)attribute with the given identifier
	*/
	public String getAttrName(String attrId);

	/**
	* Informs whether the transformer has all necessary settings for the
	* transformation and whether the data given to it can be transformed.
	*/
	public boolean isValid();

	/**
	* Allows or disallows individual transformation modes for each attribute.
	* By default, individual transformation is not allowed.
	*/
	public void setAllowIndividualTransformation(boolean value);

	/**
	* Informs whether the attributes may be (potentially) transformed
	* individually.
	*/
	public boolean getAllowIndividualTransformation();

	/**
	* If individual transformation is allowed, this method switches it on or off.
	*/
	public void setTransformIndividually(boolean value);

	/**
	* Informs whether the individual transformation mode is on or off.
	*/
	public boolean getTransformIndividually();

	/**
	* Performs the necessary data transformation and puts the results in the
	* array data.
	*/
	public void doTransformation();

	/**
	* Returns the UI for setting the transformation parameters in this transformer.
	* If this is the first transformer in a row, combines the UIs of all the
	* transformers, otherwise returns its individual UI.
	*/
	public Component getUI();

	/**
	* Returns the UI for setting the transformation parameters in this transformer.
	* Does not take into account the other transformers in a row..
	*/
	public Component getIndividualUI();

	/**
	* Returns the numeric value corresponding to the specified table row (recN)
	* and column (attrN). This may be either a result of transformation or the
	* initial attribute value, if this column is not transformed. The names and
	* order of the arguments are kept for the compatibility with the corresponding
	* method in the interface AttributeDataPortion.
	*/
	public double getNumericAttrValue(int attrN, int recN);

	/**
	* Returns the numeric value corresponding to the specified column (attrN) of
	* the given data record from the table. This may be either a result of
	* transformation or the initial attribute value fronm the record, if this
	* column is not transformed.
	*/
	public double getNumericAttrValue(int attrN, ThematicDataItem dit);

	/**
	* Determines the common value range in the columns with the specified numbers.
	*/
	public NumRange getValueRangeInColumns(IntArray colNs);

	/**
	* Determines the value range of the numeric attribute with the given identifier.
	* If this is a super-attribute, processes all its children.
	*/
	public NumRange getAttrValueRange(String attrId);

	/**
	* Determines the common value range of the numeric attributes with the
	* specified identifiers.
	*/
	public NumRange getAttrValueRange(Vector attrIds);

	/**
	* Returns statistics for the numeric attribute specified through its column
	* index.
	*/
	public NumStat getNumAttrStatistics(int attrN);

	/**
	* Registeres a listener of changes of the transformed values. The
	* listener must implement the PropertyChangeListener interface.
	*/
	public void addPropertyChangeListener(PropertyChangeListener l);

	/**
	* Unregisteres a listener of changes of the transformed values.
	*/
	public void removePropertyChangeListener(PropertyChangeListener l);

	/**
	* The method used to notify all the listeners about changes of the transformed
	* values. The property name is "values".
	*/
	public void notifyValuesChange();

	/**
	* Returns the string representation of the transformed value corresponding to
	* the given row and column of the original table. If the value is not
	* transformed, returns null.
	*/
	public String getTransformedValueAsString(int rowN, int colN);

	/**
	* Returns the identifier (unique name) of this transformation method.
	*/
	public String getMethodName();

	/**
	* Returns the parameters (properties) of this transformation method so that
	* the state of the transformer may be completely restored.
	*/
	public Hashtable getProperties();

	/**
	* Sets the parameters (properties) of this transformation method.
	* In the base transformer class, this method does nothing.
	*/
	public void setProperties(Hashtable properties);

	/**
	* Returns the specification of this single attribute transformer, with no
	* regard of the other transformers in the sequence.
	*/
	public TransformSpec getSpecification();

	/**
	* Returns the specification of the sequence of transformers including
	* this transformer and all the transformers following it.
	*/
	public TransformSequenceSpec getSpecSequence();

	/**
	 * Returns the description of the data transformation made. The description may be
	 * shown to the user. If no transformation has been made, returns null.
	 */
	public String getDescription();
}