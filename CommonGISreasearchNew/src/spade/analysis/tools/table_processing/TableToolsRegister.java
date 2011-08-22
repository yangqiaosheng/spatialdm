package spade.analysis.tools.table_processing;

import spade.analysis.tools.ToolDescriptor;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Apr 9, 2010
 * Time: 10:51:27 AM
 * Keeps the list of tools implementing various operations on table data.
 */
public class TableToolsRegister implements ToolDescriptor {
	/**
	* The list of available tools.
	* For each tool specifies its identifier, name, and the full name of
	* the class implementing it. Each tool must implement the interface
	* spade.analysis.tools.DataAnalyser.
	*/
	protected static final String tools[][] = { { "summarize_classes", "Summarize classes or clusters of table records", "spade.analysis.tools.table_processing.ClassSummarizer" },
			{ "transpose_table", "Transpose a table", "spade.analysis.tools.table_processing.TransposeTable" }, { "names_to_column", "Put identifiers and/or names in table columns", "spade.analysis.tools.table_processing.PutNamesInColumns" },
			{ "geo_names_to_table", "Assign names of geo objects to respective table records", "spade.analysis.tools.table_processing.GeoNamesToTable" },
			{ "extract_date_components", "Extract date components from a column with dates", "spade.analysis.tools.table_processing.DateComponentExtractor" },
			{ "process_strings", "Process strings from a table column", "spade.analysis.tools.table_processing.StringProcessor" },
			{ "build_points", "Make a layer with point objects from a table", "spade.analysis.tools.table_processing.PointsFromTable" },
			{ "related_objects_statistics", "Compute statistics from related objects of another set", "spade.analysis.tools.table_processing.RelatedObjectsStatisticsComputer" },
			{ "reformat_trings", "Re-format strings representing numeric values", "spade.analysis.tools.table_processing.NumberFormatter" }, { "remove_records", "Remove selected table records", "spade.analysis.tools.table_processing.RecordRemover" } };

	/**
	* Returns the description of the known tools
	*/
	@Override
	public String[][] getToolDescription() {
		return tools;
	}

	/**
	 * Returns the name of the class implementing the tool with the given identifier
	 */
	public static String getToolClassName(String toolId) {
		if (toolId == null)
			return null;
		for (String[] tool : tools)
			if (toolId.equals(tool[0]))
				return tool[2];
		return null;
	}
}
