package data_load.read_oracle;


public class Res extends java.util.ListResourceBundle {
	static final Object[][] contents = new String[][] { { "whole_layer_extent", "whole layer extent" }, { "current_map_extent", "current map extent" }, { "other", "other" }, { "Select_columns_to", "Select columns to load:" },
			{ "Select_all", "Select all" }, { "Deselect_all", "Deselect all" }, { "Identifiers_of", "Identifiers of objects are in column:" }, { "Names_of_objects_are", "Names of objects are in column:" },
			{ "Take_geographic_data", "Take geographic data from column:" }, { "Cannot_get_the_list", "Cannot get the list of columns: " }, { "Failed_to_get_row", "Failed to get row count for table " },
			{ "Failed_to_get_layer", "Failed to get layer extent: " }, { "Select_columns_to1", "Select columns to load" }, { "Set_territory_limits", "Set territory limits" }, { "Failed_to_get_data", "Failed to get data from " },
			{ "No_columns_with_data", "No columns with data got from " }, { "rows_got", " rows got" }, { "Reading", "Reading " }, { "database_records_from", " database records from " }, { "Got", "Got " },
			{ "Exception_while", "Exception while reading data from " }, { "Error_in_OracleReader", "Error in OracleReader: unable convert geometry for " }, { "Error_converting", "Error converting geometry: " } };

	@Override
	public Object[][] getContents() {
		return contents;
	}
}