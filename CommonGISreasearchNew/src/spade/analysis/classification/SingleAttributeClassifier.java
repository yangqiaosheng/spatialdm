package spade.analysis.classification;

import java.awt.Color;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: 19.01.2007
 * Time: 11:05:51
 * For a classifier doing the classification on the basis of values of a single
 * attribute, allows to get the color for a specific attribute value
 */
public interface SingleAttributeClassifier {
	/**
	* Returns the identifier of the attribute used for
	* the classification
	*/
	public String getAttrId();

	/**
	* Returns the index (column number) in the table of the attribute used for
	* the classification
	*/
	public int getAttrColumnN();

	/**
	 * Returns the color corresponding to the given attribute value
	 */
	public Color getColorForValue(Object value);
}
