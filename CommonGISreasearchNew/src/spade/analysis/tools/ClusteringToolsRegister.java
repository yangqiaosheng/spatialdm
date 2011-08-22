package spade.analysis.tools;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Oct 23, 2009
 * Time: 11:51:15 AM
 * Contains descriptions of available tools for clustering.
 */
public class ClusteringToolsRegister implements ToolDescriptor {
	/**
	* The list of available tools.
	* For each tool specifies its identifier, name, and the full name of
	* the class implementing it. Each tool must implement the interface
	* spade.analysis.tools.DataAnalyser.
	*/
	protected static final String tools[][] = { { "OPTICS", "Cluster analysis with OPTICS", "spade.analysis.tools.clustering.ClusterAnalysis" },
			{ "KMedoidsTblClust", "KMedoids Table Clustering", "spade.analysis.tools.tableClustering.KMedoidsTableClustering" }, { "WekaExplorer", "WeKa Knowledge Explorer", "spade.analysis.tools.WekaKnowledgeExplorer" },
			{ "SOM", "SOM (self-organising map)", "spade.analysis.tools.somlink.SOMConnector" } };

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
