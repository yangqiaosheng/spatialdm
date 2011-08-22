package data_update;


public class Res extends java.util.ListResourceBundle {
	static final Object[][] contents = new String[][] { { "finished", "finished" }, { "No_file_for_the", "No file for the thematic data specified!" }, { "No_data_of", "No data of appropriate type (SpatialEntity or DataRecord) found!" },
			{ "Error_reading", "Error reading " }, { "for_writing_", " for writing: " }, { "Error_opening_the", "Error opening the file " }, { "Error_writing_to_the", "Error writing to the file " },
			{ "_update_for_writing_", ".update for writing: " }, { "Error_reading_the", "Error reading the header of the file " }, { "for_rewriting_the", " for rewriting the header: " },
			{ "Error_rewriting_the", "Error rewriting the header of the file " } };

	@Override
	public Object[][] getContents() {
		return contents;
	}
}