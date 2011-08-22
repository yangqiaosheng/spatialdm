package spade.lib.basicwin;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: 25-Feb-2008
 * Time: 14:36:07
 */
public class Res_rom extends java.util.ListResourceBundle {
	static final Object[][] contents = new String[][] { { "OK", "OK" }, { "Cancel", "Anulare" }, { "Click_to_choose", "Clic pentru a alege o alta optiune" }, { "Back", "Inapoi" }, { "Yes", "Da" }, { "No", "Nu" },
			{ "Expand_window", "Marire fereastra" }, { "Close_window", "Inchidere fereastra" }, { "Enter_path_or_URL_", "Introduceti calea sau URL:" }, { "Browse", "Browse" }, { "Clear_list", "Curata lista" }, { "Select_all", "Selectare tot" },
			{ "go", "Go!" }, { "S_every", "Selectare fiecare" }, { "S_from", "de la" }, { "S_to", "la" }, { "drag_to_reorder", "trage pentru a reordona" } };

	@Override
	public Object[][] getContents() {
		return contents;
	}
}
