package spade.analysis.system;

import java.util.Hashtable;
import java.util.Vector;

import spade.vis.database.AttributeDataPortion;
import spade.vis.spec.ToolSpec;

/**
* An interface to be implememnted by classes supporting generation of various
* graphical displays and calculations over table columns. The reason for
* introducing the interface is to keep the components that could require
* such tools (e.g. task support guide) apart from specific
* implementation peculiarities.
*/
public interface ToolManager {
	/**
	* Here not all possible data analysis tools are listed but rather those that
	* may be required for the task support. This list may be extended in the
	* future
	*/
	public static final String tools[] = {
			//graphical display types
			"dot_plot", "scatter_plot", "parallel_coordinates",
			//query, search, classification
			"attribute_query", "dynamic_query", "find_by_name", "object_index", "classify",
			//analysis tools
			"similarity", "similarity_class", "rank", "evaluate" };

	/**
	* Replies whether the specified analysis tool is available. The tool
	* should be one of those listed in the array of tools above.
	*/
	public boolean isToolAvailable(String toolId);

	/**
	* Replies whether help about the specified tool is available in the system
	*/
	public boolean canHelpWithTool(String toolId);

	/**
	* Displays help about the specified tool
	*/
	public void helpWithTool(String toolId);

	/**
	* Checks whether the specified analysis tool is applicable to the
	* given table and the given attributes
	*/
	public boolean isToolApplicable(String toolId, AttributeDataPortion table, Vector attrs);

	/**
	* Applies the specified analysis tool to the given table and the given
	* attributes. Returns a reference to the tool constructed or null if failed.
	* The argument properties may specify individual properties for the
	* tool to be constructed.
	*/
	public Object applyTool(String toolId, AttributeDataPortion table, Vector attrs, Hashtable properties);

	/**
	* Applies the specified analysis tool to the given table and the given
	* attributes. Returns a reference to the tool constructed or null if failed.
	* The tool may show its results on a map if the layer identifier is provided.
	* The argument properties may specify individual properties for the
	* tool to be constructed.
	*/
	public Object applyTool(String toolId, AttributeDataPortion table, Vector attrs, String geoLayerId, Hashtable properties);

	/**
	* Constructs and applies the tool according to the given specification.
	*/
	public Object applyTool(ToolSpec spec, AttributeDataPortion table);

	/**
	* Constructs and applies the tool according to the given specification.
	*/
	public Object applyTool(ToolSpec spec, AttributeDataPortion table, String geoLayerId);

	/**
	* If failed to apply the requested tool, the error message explains the
	* reason
	*/
	public String getErrorMessage();

	/**
	* Closes all tools that are currently open
	*/
	public void closeAllTools();
}
