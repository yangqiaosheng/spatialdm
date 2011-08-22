package core;


public class Res extends java.util.ListResourceBundle {
	static final Object[][] contents = new String[][] { { "No_tables_loaded_", "No tables loaded!" }, { "is_not_available_", " is not available!" }, { "The_tool", "The tool " }, { "Graphical_analysis", "Graphical analysis tools" },
			{ "Query_search", "Query and search tools" }, { "Unknown_analysis_tool", "Unknown analysis tool requested: " }, { "_unknown_tool_", ": unknown tool!" }, { "select_url", "Select URL to open" }, { "Geobrowse_ON", "Geo-browsing mode" },
			{ "Geobrowse_OFF", "Object selection mode" }, { "Level_Up", "Go to upper level" }, { "Areas", "Areas" }, { "Cannot_open_the_URL", "Cannot open the URL " },
			{ "already_open", "The tool is already open; see \"Query and search tools\" window" }, { "Available_windows", "Available windows" }, { "Windows", "Windows" }, { "Rename", "Rename" }, { "Rename_window", "Rename window" },
			{ "New_name", "New name" }, { "To_front", "To front" } };

	@Override
	public Object[][] getContents() {
		return contents;
	}
}
