package spade.analysis.tools.similarity;

import spade.analysis.tools.ToolDescriptor;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Aug 4, 2009
 * Time: 5:33:29 PM
 * Contains descriptors of tools for similarity analysis.
 */
public class SimilarityToolsRegister implements ToolDescriptor {
	/**
	* The list of available tools.
	* For each tool specifies its identifier, name, and the full name of
	* the class implementing it. Each tool must implement the interface
	* spade.analysis.tools.DataAnalyser.
	*/
	protected static final String tools[][] = { { "dist_matrix_table", "Compute similarities between records in a table", "spade.analysis.tools.similarity.ComputeDistanceMatrixForTable" },
			{ "dist_to_sel_object", "Compute distances to a selected geographical object", "spade.analysis.tools.similarity.ComputeDistancesToSingleObject" },
			{ "dist_matrix_geo", "Compute matrix of distances among geographical objects", "spade.analysis.tools.similarity.ComputeDistanceMatrixForGeoObjects" },
			{ "sammons_projection", "Apply Sammon's projection to a pre-computed distance matrix", "spade.analysis.tools.similarity.SammonProjectionBuilder" } };

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
