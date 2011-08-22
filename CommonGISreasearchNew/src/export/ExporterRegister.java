package export;

/**
* Contains a register of all known classes for exporting data from Descartes.
*/
public class ExporterRegister {
	/**
	* The list of the exporter classes (full names must be specified).
	* All the classes must implement the interface export.DataExporter.
	* The classes for exporting geographical data must implement the interface
	* export.LayerExporter
	*/
	protected String exporters[] = { "export.TableToCSV", "export.TableToCSVnoID", "export.TableToJDBC", "export.TableToOracle", "export.TableToXML", "export.MakeFeatureVectors", "export.LayerToCSV", "export.LinkLayerToCSV",
			"export.TrajectoriesToCSV", "export.TrajectoriesToCSV_Extended", "export.TrajectoriesToOracle", "export.LayerToOVL", "export.LayerToXML", "export.LayerToOraSpatial", "export.RasterToADF", "export.RasterToFLT", "export.RasterToESR",
			"export.GridToASCII" };

	/**
	* Returns its list of exporter classes
	*/
	public String[] getExporterClassNames() {
		return exporters;
	}
}