package spade.analysis.tools;

import java.util.ResourceBundle;

import spade.lib.lang.Language;

/**
* Contains descriptions of available tools to appear in the menu "tools".
* Extensions of Descartes may be added to this register.
*/
public class ExtraToolRegister implements ToolDescriptor {
	static ResourceBundle res = Language.getTextResource("spade.analysis.tools.Res");
	/**
	* The list of available tools.
	* For each tool specifies its identifier, name, and the full name of
	* the class implementing it. Each tool must implement the interface
	* spade.analysis.tools.DataAnalyser.
	*/
	protected static final String tools[][] = { { "space_time_cube", "Vew temporal data in a space-time cube", "spade.analysis.space_time_cube.SpaceTimeCubeTool" },
			{ "DB_analysis", "Analyse data using database operations", "spade.analysis.tools.db_tools.DatabaseAnalyser" }, { "movement", "Explore movement data", "spade.analysis.tools.moves.MovementExplorer" },
			{ "events", "Explore event data", "spade.analysis.tools.events.EventExplorer" }, { "cluster", "Cluster analysis", "spade.analysis.tools.ClusteringTools" },
			{ "similarity", "Similarity analysis", "spade.analysis.tools.similarity.SimilarityExplorer" }, { "process_table", "Table data processing", "spade.analysis.tools.table_processing.TableProcessor" },
			{ "google", "Export objects for viewing in Google", "spade.analysis.tools.GeoObjectsToGoogle" }, { "classify", res.getString("classify"), "spade.analysis.tools.FreeClassifier" },
			{ "classes_by_small_multiples", "Represent classes of objects by small multiples", "spade.analysis.tools.ShowClassesBySmallMultiples" },
			{ "anim_by_small_multiples", "Represent the animated map by small multiples", "spade.analysis.tools.MakeSmallMultiplesFromAnimation" }, { "build_layer", res.getString("Edit_or_construct_map"), "spade.analysis.tools.LayerBuilder" },
			{ "build_grid", "Build a grid as a map layer", "spade.analysis.tools.GridBuilder" }, { "spatial_smoothing", "Do spatial smoothing for a numeric attribute", "spade.analysis.tools.SpatialSmoothing" },
			{ "decision map", res.getString("Decision_map"), "spade.analysis.tools.DecisionMap" }, { "3DView", res.getString("Perspective_view"), "spade.analysis.vis3d.Run3DView" },
			{ "data_pipe", res.getString("Data_pipelines"), "connection.zeno.DataPipesManager" }, { "distance", res.getString("Measure_distances"), "spade.analysis.tools.DistanceMeasurer" },
			{ "processors", "Store or load previously created data processors", "spade.analysis.tools.processors.ProcessorsManager" }, { "patterns", "Load and explore previously extracted patterns", "spade.analysis.tools.patterns.PatternExplorer" },
			{ "schedule", "Explore a transportation schedule", "spade.analysis.tools.schedule.ScheduleExplorer" }
	//{"ora_spatial","Spatial Analysis","spade.analysis.oraspa.SpatialAnalysis"}
	};

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
