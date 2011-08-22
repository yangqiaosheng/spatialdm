package spade.analysis.transform;

import spade.vis.spec.TransformSequenceSpec;
import spade.vis.spec.TransformSpec;

/**
* Created by IntelliJ IDEA.
* Creator: N.Andrienko
* Date: 19-Apr-2004
* Time: 12:58:17
* Restores sequences of attribute transformers according to given specifications.
*/
public class TransformRestorer {
	/**
	* Specifies the correspondence between available types of transformation and the classes
	* implementing them.
	*/
	protected static final String transCorr[][] = { { "compare", "spade.analysis.transform.CompareSupport" }, { "arithmetic", "spade.analysis.transform.MathTransformer" }, { "time_compare", "spade.time.transform.ChangeCounter" },
			{ "time_smooth", "spade.time.transform.TimeSmoother" } };

	/**
	 * For the given identifier (unique name) of a transformation method,
	 * returns the name of the class implementing it.
	 */
	public static String getClassNameForMethod(String methodId) {
		if (methodId == null)
			return null;
		for (String[] element : transCorr)
			if (methodId.equalsIgnoreCase(element[0]))
				return element[1];
		return null;
	}

	/**
	 * Restores the sequence of transfromers according to the given specification.
	 */
	public static AttributeTransformer restoreTransformerSequence(TransformSequenceSpec tss) {
		if (tss == null || tss.transSp == null || tss.transSp.size() < 1)
			return null;
		AttributeTransformer first = null, last = null;
		for (int i = 0; i < tss.transSp.size(); i++)
			if (tss.transSp.elementAt(i) != null && (tss.transSp.elementAt(i) instanceof TransformSpec)) {
				TransformSpec tsp = (TransformSpec) tss.transSp.elementAt(i);
				String className = getClassNameForMethod(tsp.methodId);
				if (className == null) {
					continue;
				}
				AttributeTransformer trans = null;
				try {
					trans = (AttributeTransformer) Class.forName(className).newInstance();
				} catch (Exception e) {
				}
				if (trans == null) {
					continue;
				}
				if (tsp.properties != null) {
					trans.setProperties(tsp.properties);
				}
				if (first == null) {
					first = trans;
					last = trans;
				} else {
					trans.setPreviousTransformer(last);
					last = trans;
				}
			}
		return first;
	}

}
