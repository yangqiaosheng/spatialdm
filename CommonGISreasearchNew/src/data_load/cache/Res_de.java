package data_load.cache;


public class Res_de extends java.util.ListResourceBundle {
	static final Object[][] contents = new String[][] { { "could_not_create", "Kann den Cachecleaner nicht erstellen " }, { "No_data_to_be_loaded", "Keine Daten zum Laden vorhanden" },
			{ "Selecting_relevant", "Markiere relevante Objekte im DataBroker..." },
			// Die n�chsten beiden Strings geh�ren zusammen
			{ "Selecting_relevant1", "Markiere relevante Objekte im DataBroker: " }, { "objects_selected", " Objekte markiert" }, { "Data_Broker_failed_to", "DataBroker konnte Daten nicht laden; leere den Cache..." },
			{ "Data_Broker_failed_to1", "DataBroker konnte Daten nicht laden" }, { "Cache_clearing", "Cache wird gel�scht" }, { "Cache_cleaning_is", "Cache wurde gel�scht" } };

	@Override
	public Object[][] getContents() {
		return contents;
	}
}