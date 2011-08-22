package data_load.read_oracle;


public class Res_de extends java.util.ListResourceBundle {
	static final Object[][] contents = new String[][] { { "whole_layer_extent", "Volle Layerausdehnung" }, { "current_map_extent", "Momentane Kartenausdehnung" }, { "other", "andere" },
			{ "Select_columns_to", "W�hle die Spalten, welche geladen werden sollen:" }, { "Select_all", "Selektiere alles" }, { "Deselect_all", "Selektiere nichts" }, { "Identifiers_of", "Identifier der Objekte sind in den Spalten:" },
			{ "Names_of_objects_are", "Namen der Objekte sind in der Spalte:" }, { "Take_geographic_data", "�bertrage die geographischen Daten aus der Spalte:" }, { "Cannot_get_the_list", "Fehler beim Laden der Spaltenliste: " },
			{ "Failed_to_get_row", "Laden der Reihennummer f�r die Tabelle fehlgeschlagen " }, { "Failed_to_get_layer", "Laden der gr��ten Layerausgehnung fehlgeschlagen: " }, { "Select_columns_to1", "W�hle zu ladende Spalten" },
			{ "Set_territory_limits", "Setzte Gebietsgrenzen fest" }, { "Failed_to_get_data", "Fehler bei der Daten�bertragung von(m) " }, { "No_columns_with_data", "Keine Spalten mit Daten bekommen von(m) " }, { "rows_got", " Reihen bekommen" },
			{ "Reading", "Lese " }, { "database_records_from", " Datenbankordner von(m) " }, { "Got", "Bekomme " }, { "Exception_while", "Ausnahme w�hrend des lesens der Daten von(m) " },
			{ "Error_in_OracleReader", "Fehler im OracleReader: umrechnen der Geometrie nicht m�glich " }, { "Error_converting", "Fehler beim umrechen der Geometrie: " } };

	@Override
	public Object[][] getContents() {
		return contents;
	}
}