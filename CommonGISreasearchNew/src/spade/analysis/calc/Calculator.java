package spade.analysis.calc;

import java.util.Vector;

import spade.vis.database.DataTable;

/**
* A common interface for all components performing calculations in a table
* and adding new (derived) attributes to it.
*/
public interface Calculator {
	/**
	* Sets the table in which to do calculations
	*/
	public void setTable(DataTable table);

	/**
	* Returns the table in which the calculations are done
	*/
	public DataTable getTable();

	/**
	* Sets the numbers of the source attributes for calculations
	*/
	public void setAttrNumbers(int attrNumbers[]);

	/**
	 * Returns the numbers of the source attributes for calculations
	 */
	public int[] getAttrNumbers();

	/**
	* Sets the descriptors of the source attributes for calculations. A descriptor
	* contains a reference to an attribute and, possibly, a list of selected
	* values of relevant parameters. The elements of the vector are instances of
	* the class spade.vis.database.AttrDescriptor.
	*/
	public void setAttrDescriptors(Vector attrDescr);

	/**
	 * Returns false if the Calculator only modifies the values of the selected attributes
	 * but does not create any new attributes.
	 */
	public boolean doesCreateNewAttributes();

	/**
	* Performs the calculation and returns the list of identifiers of attributes
	* added to the table
	*/
	public Vector doCalculation();

	/**
	* If there was an error in computation, returns the error message
	*/
	public String getErrorMessage();

	/**
	* Returns an explanation about this calculation method
	*/
	public String getExplanation();

	/**
	* Returns an instruction for the user about what and how many attributes to
	* select.
	*/
	public String getAttributeSelectionPrompt();

	/**
	* Returns the minimum number of attributes needed for this computation
	*/
	public int getMinAttrNumber();

	/**
	* Returns the maximum number of attributes needed for this computation.
	* If the maximum number is unlimited, returns -1.
	*/
	public int getMaxAttrNumber();
}