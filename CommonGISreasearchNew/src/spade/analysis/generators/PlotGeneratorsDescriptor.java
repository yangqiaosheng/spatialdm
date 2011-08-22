package spade.analysis.generators;

import java.util.ResourceBundle;

import spade.analysis.tools.ToolDescriptor;
import spade.lib.lang.Language;

/**
* Contains descriptions of available generators of graphical displays
*/
public class PlotGeneratorsDescriptor implements ToolDescriptor {
	static ResourceBundle res = Language.getTextResource("spade.analysis.generators.Res");
	/**
	* The list of all available data display methods. For each method specifies
	* its internal identifier, name, and the full name of the generator, i.e. the
	* class that constructs this display. The generator should descend from the
	* abstract class VisGenerator
	*/
	protected static String generators[][] = { { "statistics", res.getString("Attribute_statistics"), "spade.analysis.generators.StatisticsGenerator" },
			{ "frequencies", "Attribute value frequencies", "spade.analysis.generators.FrequenciesViewGenerator" }, { "histogram", res.getString("histogram"), "spade.analysis.generators.HistogramGenerator" },
			{ "ranged_distrib_graph", res.getString("ranged_distrib_graph"), "spade.analysis.generators.RangedDistViewGenerator" }, { "scatter_plot", res.getString("Scatter_plot"), "spade.analysis.generators.ScatterplotGenerator" },
			{ "2d histogram", "2d histogram", "spade.analysis.generators.Histogram2dGenerator" }, { "scatter_matrix", res.getString("Scatter_plot_matrix"), "spade.analysis.generators.ScatterMatrixGenerator" },
			{ "parallel_coordinates", res.getString("Parallel_coordinates1"), "spade.analysis.generators.PCPGenerator" }, { "sammons_projection", "Sammon's projection", "spade.analysis.generators.Sammons2DPlotGenerator" },
			{ "dot_plot", res.getString("Dot_plot_horizontal_"), "spade.analysis.generators.DotPlotHGenerator" }, { "dot_plot_v", res.getString("Dot_plot_vertical_"), "spade.analysis.generators.DotPlotVGenerator" },
			{ "dispersion_graph", res.getString("Dispersion_graph"), "spade.analysis.generators.DispersionGraphGenerator" }, { "classifier_1_num_attr", res.getString("Classifier"), "spade.analysis.generators.NumClass1DHGenerator" },
			{ "classifier_1_qual_attr", "Qualitative attribute classifier", "spade.analysis.generators.QualClassGenerator" }, { "time_arranger", "Time Arranger", "spade.analysis.generators.TimeArrangerGenerator" },
			{ "object_list", res.getString("Object_list"), "spade.analysis.generators.ObjectListGenerator" }, { "cor2d", res.getString("1_to_n_correlation_graph"), "spade.analysis.generators.CorrelationOneToNGenerator" },
			{ "cor3d", res.getString("n_to_n_correlation_graph"), "spade.analysis.generators.CorrelationNToNGenerator" }, { "disOverview", res.getString("district_overview"), "spade.analysis.generators.DistOverviewGenerator" } };
	/**
	* The list of display types which do not visualize any attributes
	*/
	protected static String attributeFreeDisplays[] = { "object_list" };

	/**
	* Replies if the given display type is attribute-free (i.e. does not visualize
	* any attributes)
	*/
	public static boolean isAttributeFree(String displayType) {
		if (displayType == null || attributeFreeDisplays == null)
			return false;
		for (String attributeFreeDisplay : attributeFreeDisplays)
			if (attributeFreeDisplay.equalsIgnoreCase(displayType))
				return true;
		return false;
	}

	/**
	* Returns the description of the known data display methods and the
	* corresponding display generators
	*/
	@Override
	public String[][] getToolDescription() {
		return generators;
	}

//ID
	public static String getToolName(String methodId) {
		if (generators == null)
			return null;
		for (String[] generator : generators)
			if (generator[0].equalsIgnoreCase(methodId))
				return generator[1];
		return null;
	}
//~ID
}