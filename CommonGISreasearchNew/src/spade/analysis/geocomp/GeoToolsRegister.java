package spade.analysis.geocomp;

import java.util.ResourceBundle;

import spade.analysis.tools.ToolDescriptor;
import spade.lib.lang.Language;

/**
* Contains descriptions of available tools for computations or other
* operations with map layers
*/
public class GeoToolsRegister implements ToolDescriptor {
	static ResourceBundle res = Language.getTextResource("spade.analysis.geocomp.Res");
	/**
	* The list of available multi-table tools for computations or other
	* operations with map layers.
	* For each tool specifies its identifier, name, and the full name of
	* the class implementing it. Each tool must extend the abstract class
	* spade.analysis.geocomp.GeoCalculator.
	*/
	protected String geoTools[][] = {
			// following text: "statistics of value distribution in a raster"
			{ "statistics", res.getString("statistics_of_value"), "spade.analysis.geocomp.StatisticsMaker" },
			// following text:"histogram of value distribution in a raster"
			{ "histogram", res.getString("histogram_of_value"), "spade.analysis.geocomp.HistogramMaker" },
			// following text: "illumination model for a raster"
			{ "topo", res.getString("illumination_model"), "spade.analysis.geocomp.TopoModel" },
			// following text: "query raster data"
			{ "query", res.getString("query_raster_data"), "spade.analysis.geocomp.RasterQuery" },
			// following text: "transform or combine rasters"
			{ "combine", res.getString("transform_or_combine"), "spade.analysis.geocomp.RasterCombiner" },
			{ "filter", res.getString("filter_or_smooth_a"), "spade.analysis.geocomp.RasterFilter" },
			// following text: "filter a raster using a free-form matrix"
			{ "freeform", res.getString("filter_a_raster_using"), "spade.analysis.geocomp.FreeformFilter" },
			// following text: "compute derivatives for a raster"
			{ "derive", res.getString("compute_derivatives"), "spade.analysis.geocomp.RasterDerivative" },
			// following text: "build a raster from points"
			{ "points_to_raster", res.getString("build_a_raster_from"), "spade.analysis.geocomp.RasterFromPoints" },
			// following text: build a raster from lines"
			{ "lines_to_raster", res.getString("build_a_raster_from1"), "spade.analysis.geocomp.RasterFromLines" },
//ID
			// following text: build a raster from lines"
			{ "polygons_to_raster", res.getString("build_a_raster_from2"), "spade.analysis.geocomp.RasterFromPolygons" },
//~ID
			// following text: "derive an attribute of vector objects from raster data"
			{ "attr_from_raster", res.getString("derive_an_attribute"), "spade.analysis.geocomp.AttrFromRaster" },
			// following text: "derive an attribute of area objects from point data"
			{ "attr_from_points", res.getString("derive_an_attribute_"), "spade.analysis.geocomp.AttrFromPoints" },
			// following text: "derive an attribute of point objects from polygon data"
			{ "attr_from_polygons", res.getString("derive_an_attribute__"), "spade.analysis.geocomp.AttrFromPolygons" },
			{ "line_ends_to_areas", "refer line ends in a linear layer to areas in an area layer", "spade.analysis.geocomp.LineEndsToAreas" },
			// following text: "clone a raster layer"
//    {"clone",res.getString("clone_a_raster_layer"),"spade.analysis.geocomp.RasterCloner"},
			// following text: "change parameters of a raster"
			{ "parameters", res.getString("change_parameters_of"), "spade.analysis.geocomp.RasterParameters" },
			// following text: "cut a part of raster layer"
			{ "cut", res.getString("cut_a_part_of_raster"), "spade.analysis.geocomp.CutRaster" },
			// following text: "make a table from raster"
			{ "table_from_raster", res.getString("make_a_table_from_raster"), "spade.analysis.geocomp.TableFromRaster" }, { "circles_from_points", "make circles around point objects", "spade.analysis.geocomp.CirclesFromPoints" },
			{ "voronoi_from_points", "make Voronoi polygons around point objects", "spade.analysis.geocomp.VoronoiPolygonsFromPoints" }, { "group_points", "group point objects", "spade.analysis.geocomp.PointGrouper" },
			{ "voronoi_from_point_clusters", "make Voronoi polygons around groups of points", "spade.analysis.geocomp.VoronoiPolygonsFromPointClusters" },
			{ "coords_from_vectors", "place coordinates of objects into table", "spade.analysis.geocomp.CoordsFromVectors" },
			{ "sp_prop_point_groups", "compute spatial properties of groups of points", "spade.analysis.geocomp.PointGroupsPropertiesComputer" },
			{ "circles_point_groups", "build circles around groups of points", "spade.analysis.geocomp.CirclesBuilder" }, { "hulls_point_groups", "build convex hulls or buffers around groups of points", "spade.analysis.geocomp.HullBuilder" },
			{ "buffers_vector_objects", "build buffers around vector objects", "spade.analysis.geocomp.BufferBuilder" } };

	/**
	* Returns the description of the known multi-table tools
	*/
	@Override
	public String[][] getToolDescription() {
		return geoTools;
	}
}
