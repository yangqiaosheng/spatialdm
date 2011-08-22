package spade.analysis.tools.moves;

import spade.analysis.tools.ToolDescriptor;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: 22-Aug-2007
 * Time: 12:43:49
 * Contains descriptions of available tools for movement data (trajectories or vectors).
 */
public class MovementToolRegister implements ToolDescriptor {
	/**
	* The list of available tools.
	* For each tool specifies its identifier, name, and the full name of
	* the class implementing it. Each tool must implement the interface
	* spade.analysis.tools.DataAnalyser.
	*/
	protected static final String tools[][] = { { "trajectories_Google", "Export trajectories to Google Earth or Google Maps", "spade.analysis.tools.moves.TrajectoriesToKML" },
			{ "generalise_trajectories", "Generalise and summarise trajectories", "spade.analysis.tools.moves.TrajectoriesGeneraliser" },
			{ "analyse_sum_quality", "Analyze and improve the quality of the generalization", "spade.analysis.tools.moves.GeneralizationQualityManager" },
			{ "vary_gen_level", "Vary the generalization level depending on data density", "spade.analysis.tools.moves.GeneralizerByVariableAreas" },
			{ "itemize_aggregates", "Itemize aggregates by classes (clusters) or by time intervals", "spade.analysis.tools.moves.AggregatesItemizer" },
			{ "summarize_classes", "Summarise classes or clusters of trajectories", "spade.analysis.tools.moves.TrajectoryClassSummarizer" },
			{ "summarise_traj_by_areas", "Summarise trajectories as moves between existing areas", "spade.analysis.tools.moves.TrajectoriesByAreasSummariser" },
			{ "filter_traj_by_areas", "Filter trajectories by visited areas", "spade.analysis.tools.moves.TrajectoriesByAreasFilter" }, { "smooth_trajectories", "Smooth trajectories", "spade.analysis.tools.moves.TrajectoriesSmoother" },
			{ "simplify_trajectories", "Simplify trajectories", "spade.analysis.tools.moves.TrajectoriesSimplifier" },
			{ "simplify_trajectories_by_areas", "Simplify trajectories by existing areas", "spade.analysis.tools.moves.TrajectoriesByAreasSimplifier" },
			{ "split_trajectories", "Split trajectories by selected places", "spade.analysis.tools.moves.TrajectorySplitter" }, { "extract_points", "Extract points from trajectories", "spade.analysis.tools.moves.PointExtractor" },
			{ "extract_events", "Extract events from trajectories", "spade.analysis.tools.moves.EventExtractor" }, { "find_interactions", "Detect interactions between trajectories", "spade.analysis.tools.moves.InteractionsSearchTool" },
			{ "view_interactions", "Visualise interactions between trajectories", "spade.analysis.tools.moves.InteractionsViewTool" }, { "compute_trajectories", "Do computations on trajectories", "spade.analysis.tools.moves.MovementComputer" },
			{ "movement_matrix", "Display a matrix of summarised moves", "spade.analysis.tools.moves.MovementMatrixBuilder" }, { "filter_vectors", "Filter moves (vectors) by starts/ends", "spade.analysis.tools.moves.MovesByStartsEndsFilter" },
			{ "vectors", "Summarise moves (vectors)", "spade.analysis.tools.moves.MovesSummariser" },
/*
    {"aggr_moves_Google","Export aggregated moves to Google Earth",
            "spade.analysis.tools.moves.AggregatedMovesToKML"},
*/
			{ "build_lines_or_trajectories", "Build a map layer with lines or trajectories of movement", "spade.analysis.tools.moves.MovementLayerBuilder" },
			{ "build_links", "Build a map layer with moves (vectors) between locations", "spade.analysis.tools.LinkLayerBuilder" },
			{ "processor", "Apply a processor (e.g. classifier based on clustering)", "spade.analysis.tools.moves.ApplyTrajectoriesProcessor" } };

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
