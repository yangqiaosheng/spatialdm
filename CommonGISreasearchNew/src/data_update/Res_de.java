package data_update;


public class Res_de extends java.util.ListResourceBundle {
	static final Object[][] contents = new String[][] { { "finished", "Beendet" }, { "No_file_for_the", "Keine Datei f�r die thematischen Daten spezifiziert!" },
			{ "No_data_of", "Keine Daten des entsprechenden Typus (SpatialEntity oder DataRecord) gefunden!" }, { "Error_reading", "Lesefehler " }, { "for_writing_", " zum schreiben: " }, { "Error_opening_the", "Fehler beim �ffnen der Datei " },
			{ "Error_writing_to_the", "Fehler beim schreiben der Datei " }, { "_update_for_writing_", ".update zum schreiben: " }, { "Error_reading_the", "Fehler beim schreiben des Dateikopfes (Header) " },
			{ "for_rewriting_the", " zum wiederbeschreiben des Dateikopfes (Header): " }, { "Error_rewriting_the", "Fehler beim wiederbeschreiben des Dateikopfes " } };

	@Override
	public Object[][] getContents() {
		return contents;
	}
}