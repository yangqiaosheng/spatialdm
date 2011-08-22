package spade.analysis.generators;

import java.util.ResourceBundle;

import spade.analysis.tools.ToolDescriptor;
import spade.lib.lang.Language;

/**
* Contains descriptions of available query and search tools such as dynamic query
* or object list
*/
public class QueryAndSearchToolsDescriptor implements ToolDescriptor {
	static ResourceBundle res = Language.getTextResource("spade.analysis.generators.Res");
	/**
	* The list of available query and search tools such as dynamic query and object
	* list. For each tool specifies its identifier, name, and the full name of
	* the class implementing it. Each multi-table tool must implement the interface
	* spade.analysis.plot.QueryOrSearchTool.
	*/
	protected String queryTools[][] = { { "dynamic_query", res.getString("Dynamic_query1"), "spade.analysis.plot.DynamicQueryShell" }, { "object_index", res.getString("Index_of_objects"), "spade.analysis.plot.ObjectList" },
			{ "filter_by_selection", "Filter by selection", "spade.analysis.plot.FilterBySelectionUI" }, { "2_sets_filter", "Filter of two related sets", "spade.analysis.plot.OtherObjectFilterApplicatorUI" },
			{ "aggregate_filter", "Aggregate filter", "spade.analysis.aggregates.AggregateFilterUI" } };

	/**
	* Returns the description of the known query and search tools
	*/
	@Override
	public String[][] getToolDescription() {
		return queryTools;
	}
}
