package configstart;

public class Res extends java.util.ListResourceBundle {
	static final Object[][] contents = new String[][] { { "table_not_found", "Table not found" }, { "attr_not_found", "Attribute not found" }, { "The_table", "The table" }, { "has_no_data_", "has no data; trying to load..." },
			{ "Failed_load_table", "Failed to load data in the table" }, { "Failed_find_map", "Failed to find a map for the table" }, { "Failed_find_layer", "Failed to find a map layer for the table" }, { "The_layer", "The layer" },
			{ "has_no_objects_", "has no objects; trying to load..." }, { "Failed_load_layer", "Failed to load data in the layer" }, { "ill_vis_method", "Illegal visualization method for animation" },
			{ "failed_make_visualizer", "Failed to construct the visualizer" }, { "no_map_view", "No map view found!" }, { "no_ui", "No system\'s UI found!" }, { "Unknown_tool", "Unknown tool" }, { "Failed_make_display", "Could not make display" } };

	@Override
	public Object[][] getContents() {
		return contents;
	}
}