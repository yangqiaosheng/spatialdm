package data_load.cache;


public class Res extends java.util.ListResourceBundle {
	static final Object[][] contents = new String[][] { { "could_not_create", "could not create cache cleaner " }, { "No_data_to_be_loaded", "No data to be loaded" }, { "Selecting_relevant", "Selecting relevant objects in DataBroker..." },
			{ "Selecting_relevant1", "Selecting relevant objects in DataBroker: " }, { "objects_selected", " objects selected" }, { "Data_Broker_failed_to", "Data Broker failed to load data; clearing the cache..." },
			{ "Data_Broker_failed_to1", "Data Broker failed to load data" }, { "Cache_clearing", "Cache clearing" }, { "Cache_cleaning_is", "Cache cleaning is finished" } };

	@Override
	public Object[][] getContents() {
		return contents;
	}
}