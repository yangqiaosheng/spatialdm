package data_load.connect_server;


public class Res extends java.util.ListResourceBundle {
	static final Object[][] contents = new String[][] { { "Specify_information", "Specify information for the ASCII file" }, { "Delimiter_", "Delimiter:" }, { "comma_", "comma (,)" }, { "semicolon_", "semicolon (;)" },
			{ "TAB_character", "TAB character" }, { "space", "space" }, { "other_character", "other character" }, { "specify_", "specify:" }, { "Take_field_names_from", "Take field names from row N" },
			{ "Take_field_types_from", "Take field types from row N" }, { "The_delimiter_must_be", "The delimiter must be a single character!" }, { "Illegal_row_number", "Illegal row number for field " }, { "types_", "types!" },
			{ "names_", "names!" }, { "No_delimiter", "No delimiter specified!" }, { "The_number_of_the_row", "The number of the row with field names is not specified!" }, { "Illegal_row_number1", "Illegal row number for field names!" },
			{ "The_number_of_the_row1", "The number of the row with field types is not specified!" }, { "Illegal_row_number2", "Illegal row number for field types!" }, { "Reader", "Reader " },
			{ "actually_used_instead", " actually used instead of data server for loading " }, { "Could_not_form_the", "Could not form the URL of the servlet from " }, { "Connecting_to_the", "Connecting to the server " },
			{ "Failed_to_open", "Failed to open connection with server: " }, { "Failed_to_open_output", "Failed to open output stream to server: " }, { "Failed_to_send_data", "Failed to send data specification to server: " },
			{ "Failed_to_close", "Failed to close output stream: " }, { "Failed_to_open_input", "Failed to open input stream from server: " }, { "Failed_to_construct", "Failed to construct an ObjectInputStream: " },
			{ "Getting_data_from_the", "Getting data from the server " }, { "Error_when_getting", "Error when getting data from " }, { "Got_table_N", "Got table: N attributes = " }, { "_N_records_", "; N records = " },
			{ "Got_layer_N_objects_", "Got layer: N objects = " }, { "Error_in_reading_data", "Error in reading data server\'s output: " }, { "Error_in_getting", "Error in getting object from data server: " },
			{ "Failed_to_close_input", "Failed to close input stream: " }, { "Could_not_construct", "Could not construct Data Broker: " }, { "Constructed_Data", "Constructed Data Broker" }, { "Data_Server_URL_", "Data Server URL:" },
			{ "Load_data_from", "Load data from " }, { "file", "file" }, { "Oracle_database", "Oracle database" }, { "JDBC_ODBC_database", "JDBC/ODBC database" }, { "Specify_data_server", "Specify data server and source" },
			{ "The_Data_Server_URL", "The Data Server URL is not specified!" }, { "Illegal_URL_", "Illegal URL: " }, { "Specify_information1", "Specify information for getting data from a FILE:" }, { "Path_or_URL_", "Path or URL:" },
			{ "Format_", "Format:" }, { "ASCII_with_delimiters", "ASCII with delimiters (comma, TAB, ...)" }, { "binary_table_format", "binary table format (DBF, ...)" }, { "specific_format_for", "specific format for VECTOR geographical " },
			{ "objects_SHAPE_MIF_MID", "objects (SHAPE, MIF/MID, GML, WKB, ...)" }, { "specific_format_for1", "specific format for GRID (RASTER) geographical " }, { "data_BIL_ADF_", "data (BIL, ADF, ...)" },
			{ "image_GIF_JPEG_", "image (GIF, JPEG, ...)" }, { "Specify_file_path_and", "Specify file path and format" }, { "The_file_path_is_not", "The file path is not specified!" }, { "Specify_delimiter", "Specify delimiter" },
			{ "Specify_information2", "Specify information about fields for the file" }, { "Column_with_object", "Column with object identifiers:" }, { "enter_name_or_number", "enter name or number or leave empty" },
			{ "Column_with_object1", "Column with object names:" }, { "Column_with_X", "Column with X-coordinates:" }, { "enter_name_or_leave", "enter name or leave empty" }, { "Column_with_Y", "Column with Y-coordinates:" },
			{ "Specify_column", "Specify column information" }, { "Illegal_number_of", "Illegal number of column with identifiers!" }, { "Illegal_number_of1", "Illegal number of column with names!" },
			{ "The_column_with_Y", "The column with Y-coordinates is not specified!" }, { "The_column_with_X", "The column with X-coordinates is not specified!" },
			{ "Specify_geographical", "Specify geographical boundaries (georeference) for the file" }, { "start_X_", "start X:" }, { "start_Y_", "start Y:" }, { "end_X_", "end X:" }, { "end_Y_", "end Y:" },
			{ "Specify_boundaries", "Specify boundaries" }, { "Starting_X_coordinate", "Starting X-coordinate" }, { "Starting_Y_coordinate", "Starting Y-coordinate" }, { "Ending_X_coordinate", "Ending X-coordinate" },
			{ "Ending_Y_coordinate", "Ending Y-coordinate" }, { "is_not_specified_", " is not specified!" }, { "_illegal_number", ": illegal number entered!" }, { "Starting_X_must_be", "Starting X must be less than ending X!" },
			{ "Starting_Y_must_be", "Starting Y must be less than ending Y!" }, { "Specify_information3", "Specify information for getting data from a database" }, { "Database_URL_", "Database URL:" }, { "Driver_", "Driver:" },
			{ "User_name_", "User name:" }, { "Password_", "Password:" }, { "Table_", "Table:" }, { "Enter_appropriate", "Enter appropriate NAMES of table columns:" }, { "Identifiers_are_in", "Identifiers are in column" },
			{ "Object_names_are_in", "Object names are in column" }, { "Object_geometries_are", "Object geometries are in column" }, { "Xcoordinates_are_in", "X-coordinates are in column" }, { "Ycoordinates_are_in", "Y-coordinates are in column" },
			{ "Load_following", "Load following columns (enter single column name per row):" }, { "Specify_database_info", "Specify database info" }, { "The_database_URL_is", "The database URL is not specified!" },
			{ "Illegal_database_URL_", "Illegal database URL!" }, { "The_database_driver", "The database driver is not specified!" }, { "oracle", "oracle" }, { "The_user_name_is_not", "The user name is not specified!" },
			{ "The_password_is_not", "The password is not specified!" }, { "The_table_name_is_not", "The table name is not specified!" } };

	public Object[][] getContents() {
		return contents;
	}
}