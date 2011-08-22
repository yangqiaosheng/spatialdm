package spade.vis.dataview;

public class Res extends java.util.ListResourceBundle {
	static final Object[][] contents = new String[][] { { "Descending", "Descending" }, { "Ascending", "Ascending" }, { "TableLens", "TableLens" }, { "Attribute_", "Attribute..." }, { "No_selection", "No selection" },
			{ "Name_1_st_column_", "Name (1-st column)" }, { "Select_attributes_to", "Select attributes to be shown in the table" }, { "object_view", "Object view" }, { "table_view", "Table view" },
			{ "no_selection", "No objects are currently selected." }, { "when_select", "When you select one or more objects from the table " },
			{ "see_obj_info", ", you will see here their names, identifiers, and " + "corresponding values of attributes." },
			{ "default_attr", "By default, these will be the attributes currently " + "represented on the map or other graphical displays." }, { "show_ids", "identifiers" }, { "click_to_switch", "click to switch on or off" },
			{ "group", "group by classes" }, { "sort_by", "Sort by:" }, { "condensed", "condensed" }, { "no_data", "No data provided for the table view!" }, { "no_attributes", "No attributes selected for the table view!" },
			{ "Select_attributes_to", "Select attributes to be shown" }, { "class", "class" } };

	@Override
	public Object[][] getContents() {
		return contents;
	}
}