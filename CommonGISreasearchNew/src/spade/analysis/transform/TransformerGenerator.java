package spade.analysis.transform;

import java.util.Vector;

import spade.analysis.classification.TableClassifier;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.Parameter;
import spade.vis.mapvis.DataPresenter;

/**
* Given an identifier of an attribute transformer class, tries to generate
* a corresponding instance. This class is introduced to enable removing
* particular transformers from the system's configuration when they are
* not needed.
*/
public class TransformerGenerator {
	/**
	* The available attribute transformers. The first element in each row is
	* an identifier, the second is the full class name. The class must extend
	* the abstract class AttributeTransformer.
	*/
	protected static String transDescr[][] = { { "time_smooth", "spade.time.transform.TimeSmoother" }, { "time_change", "spade.time.transform.ChangeCounter" }, { "compare", "spade.analysis.transform.CompareSupport" },
			{ "math", "spade.analysis.transform.MathTransformer" } };
	/**
	* A list of identifiers of the transformers which can be only applied to
	* time-dependent attributes
	*/
	protected static String timeTrans[] = { "time_smooth", "time_change" };

	/**
	* Returns the total number of the known transformers
	*/
	public static int getTransformerCount() {
		return transDescr.length;
	}

	/**
	* Returns the identifier of the transformer with the given index
	*/
	public static String getTransformerId(int idx) {
		if (idx < 0 || idx >= transDescr.length)
			return null;
		return transDescr[idx][0];
	}

	/**
	* Returns the class name of the transformer with the given index
	*/
	public static String getTransformerClassName(int idx) {
		if (idx < 0 || idx >= transDescr.length)
			return null;
		return transDescr[idx][1];
	}

	/**
	* Replies whether the transformer with the given index is used only for
	* time-dependent data
	*/
	public static boolean requiresTimeDependentData(int idx) {
		if (idx < 0 || idx >= transDescr.length)
			return false;
		return requiresTimeDependentData(transDescr[idx][0]);
	}

	/**
	* Replies whether the transformer with the given identifier is used only for
	* time-dependent data
	*/
	public static boolean requiresTimeDependentData(String transId) {
		if (transId == null || timeTrans == null || timeTrans.length < 1)
			return false;
		for (String timeTran : timeTrans)
			if (timeTran.equalsIgnoreCase(transId))
				return true;
		return false;
	}

	/**
	* Tries to generate an instance of the transformer with the given identifier.
	* May return null if fails.
	*/
	public static AttributeTransformer makeTransformer(int idx) {
		String className = getTransformerClassName(idx);
		if (className == null)
			return null; //unknown identifier
		try {
			Object obj = Class.forName(className).newInstance();
			if (obj != null && (obj instanceof AttributeTransformer))
				return (AttributeTransformer) obj;
		} catch (Exception e) {
		}
		return null;
	}

	/**
	* Given an identifier of an attribute transformer, tries to generate
	* a corresponding instance. May return null if the class is not available.
	*/
	public static AttributeTransformer makeTransformer(String id) {
		if (id == null)
			return null;
		for (int i = 0; i < transDescr.length; i++)
			if (id.equalsIgnoreCase(transDescr[i][0]))
				return makeTransformer(i);
		return null;
	}

	/**
	 * For the given table and vector of column identifiers, produces a chain of
	 * transformers.
	 * @param table - the table with the attributes to be transformed
	 * @param columns - the identifiers of the columns to be transformed
	 * @param timeDependent - whether the columns depend on the temporal parameter
	 * @param mayTransformIndividually - whether each of the attributes may be
	 *                                   transformed independently from the others
	 * @return the first transformer in the chain
	 */
	public static AttributeTransformer makeTransformerChain(AttributeDataPortion table, Vector columns, boolean timeDependent, boolean mayTransformIndividually) {
		if (table == null || columns == null || columns.size() < 1)
			return null;
		AttributeTransformer prevTrans = null, firstTrans = null;
		for (int i = 0; i < getTransformerCount(); i++)
			if (timeDependent || !requiresTimeDependentData(i)) {
				AttributeTransformer mtrans = makeTransformer(i);
				if (mtrans != null) {
					mtrans.setDataTable(table);
					mtrans.setAllowIndividualTransformation(mayTransformIndividually);
					if (prevTrans != null) {
						mtrans.setColumnNumbers(prevTrans.getColumnNumbers());
						mtrans.setPreviousTransformer(prevTrans);
					} else {
						mtrans.setAttributes(columns);
					}
					if (mtrans.isValid()) {
						prevTrans = mtrans;
						if (firstTrans == null) {
							firstTrans = mtrans;
						}
					} else {
						mtrans = null;
					}
				}
			}
		return firstTrans;
	}

	/**
	 * Constructs a transformer for the given visualiser, which must be either
	 * a DataPresenter (instance of spade.vis.mapvis.DataPresenter) or
	 * a TableClassifier (instance of spade.analysis.classification.TableClassifier).
	 * @param visualiser - the visualiser that must be suplied with transformers
	 * @param table - the table with the attributes to be transformed
	 * @param columns - the identifiers of the columns to be transformed
	 * @param timeDependent - whether the columns depend on the temporal parameter
	 * @return the first transformer in the chain
	 */
	public static AttributeTransformer makeTransformerChain(Object visualiser, AttributeDataPortion table, Vector columns, boolean timeDependent) {
		if (visualiser == null || table == null || columns == null || columns.size() < 1)
			return null;
		boolean allowTransform = false, mayTransformIndividually = false;
		DataPresenter dpres = null;
		TableClassifier tcl = null;
		if (visualiser instanceof DataPresenter) {
			dpres = (DataPresenter) visualiser;
			allowTransform = dpres.getAllowTransform();
			if (allowTransform) {
				mayTransformIndividually = dpres.getAllowTransformIndividually();
			}
		} else if (visualiser instanceof TableClassifier) {
			tcl = (TableClassifier) visualiser;
			allowTransform = tcl.getAllowTransform();
			if (allowTransform) {
				mayTransformIndividually = tcl.getAllowTransformIndividually();
			}
		}
		if (!allowTransform)
			return null;
		AttributeTransformer trans = makeTransformerChain(table, columns, timeDependent, mayTransformIndividually);
		if (trans == null)
			return null;
		if (dpres != null) {
			dpres.setAttributeTransformer(trans, true);
		} else {
			tcl.setAttributeTransformer(trans, true);
		}
		return trans;
	}

	/**
	 * Same as previous, but tries to detect whether any of the attributes passed
	 * to it is time-dependent.
	 * @param visualiser - the visualiser that must be suplied with transformers
	 * @param table - the table with the attributes to be transformed
	 * @param columns - the identifiers of the columns to be transformed
	 * @return the first transformer in the chain
	 */
	public static AttributeTransformer makeTransformerChain(Object visualiser, AttributeDataPortion table, Vector columns) {
		if (visualiser == null || table == null || columns == null || columns.size() < 1)
			return null;
		boolean timeDep = false;
		if (table.hasTemporalParameter()) {
			Parameter tpar = table.getTemporalParameter();
			for (int i = 0; i < columns.size() && !timeDep; i++) {
				Attribute at = table.getAttribute((String) columns.elementAt(i));
				if (at == null) {
					continue;
				}
				if (at.getParent() != null) {
					at = at.getParent();
				}
				timeDep = at.dependsOnParameter(tpar);
			}
		}
		return makeTransformerChain(visualiser, table, columns, timeDep);
	}
}