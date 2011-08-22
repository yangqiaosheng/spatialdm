package spade.analysis.calc;

import java.util.Vector;

import spade.analysis.system.DisplayProducer;
import spade.analysis.system.Supervisor;
import spade.analysis.system.ToolManager;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.DataTable;

public interface CalcManager extends ToolManager {
	/**
	* Returns the name of the method with the given identifier
	*/
	public String getMethodName(int methodId);

	/**
	* Returns the name of the group for the method with the given index,
	* e.g. "statistics" or "decision support"
	*/
	public String getMethodGroupName(int methodId);

	/**
	* Returns an explanation about the method with the given identifier
	*/
	public String getMethodExplanation(int methodId);

	/**
	* Returns an attribute selection prompt about the method with the given identifier
	*/
	public String getAttrSelectionPrompt(int methodId);

	/**
	* Returns the minimum number of attributes needed for the method with
	* the given identifier
	*/
	public int getMinAttrNumber(int methodId);

	/**
	* Returns the maximum number of attributes needed for the method with
	* the given identifier. If the maximum number is unlimited, returns -1.
	*/
	public int getMaxAttrNumber(int methodId);

	/**
	* Checks whether the method with the given index is applicable to the specified
	* attributes. The vector attrDescr contains descriptors of the source
	* attributes for calculations. A descriptor contains a reference to an
	* attribute and, possibly, a list of selected values of relevant parameters.
	* The elements of the vector are instances of the class
	* spade.vis.database.AttrDescriptor.
	* fn is an array of column numbers.
	*/
	public boolean isApplicable(int methodId, AttributeDataPortion dTable, int fn[], Vector attrDescr);

	/**
	* Checks whether the method with the given index is applicable to the specified
	* attributes. The vector attrDescr contains descriptors of the source
	* attributes for calculations. A descriptor contains a reference to an
	* attribute and, possibly, a list of selected values of relevant parameters.
	* The elements of the vector are instances of the class
	* spade.vis.database.AttrDescriptor.
	* Vector attr contains low-level attribute identifiers, i.e. attributes
	* directly corresponding to table columns (1:1). The vector attrDescr may
	* contain parents of these attributes; hence, its size may be less than the
	* size of the vector attr.
	*/
	public boolean isApplicable(int methodId, AttributeDataPortion dTable, Vector attr, Vector attrDescr);

	/**
	* Sets the supervisor used for dynamic linking between displays
	*/
	public void setSupervisor(Supervisor sup);

	/**
	* Sets the display producer used for visual representation of data on maps
	* and other graphics
	*/
	public void setDisplayProducer(DisplayProducer dprod);

	/**
	* Returns the number of methods that are available in the current system
	* configuration
	*/
	public int getNAvailableMethods();

	/**
	* Checks if the specified calculation method is available
	*/
	public boolean isMethodAvailable(int methodId);

	/**
	* Returns the index of an available method by its index in the list of
	* available methods
	*/
	public int getAvailableMethodId(int idx);

	/**
	* Applies the specified calculation method to the given data. The method is
	* specified by its identifier (not the index!!!). The vector attrDescr contains
	* descriptors of the source attributes selected for calculations. A descriptor
	* contains a reference to an attribute and, possibly, a list of selected values
	* of relevant parameters. The elements of the vector are instances of the class
	* spade.vis.database.AttrDescriptor.
	* fn is an array of column numbers.
	*/
	public Object applyCalcMethod(int methodId, DataTable dTable, int fn[], Vector attrDescr, String layerId);

	/**
	* Applies the specified calculation method to the given data. The method is
	* specified by its identifier (not the index!!!). The vector attrDescr contains
	* descriptors of the source attributes selected for calculations. A descriptor
	* contains a reference to an attribute and, possibly, a list of selected values
	* of relevant parameters. The elements of the vector are instances of the class
	* spade.vis.database.AttrDescriptor.
	* Vector attr contains low-level attribute identifiers, i.e. attributes
	* directly corresponding to table columns (1:1). The vector attrDescr may
	* contain parents of these attributes; hence, its size may be less than the
	* size of the vector attr.
	*/
	public Object applyCalcMethod(int methodId, DataTable dTable, Vector attr, Vector attrDescr, String layerId);

	/**
	* When a table is removed, the CalcManager closes all calculation dialogs that
	* are linked to this table. The table is specified by its identifier.
	*/
	public void tableIsRemoved(String tableId);
}
