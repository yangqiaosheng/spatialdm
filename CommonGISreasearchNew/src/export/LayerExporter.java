package export;

/**
* The interface to be implemented by classes for exporting geographical data
* (map layers) from Descartes into files of various formats.
*/
public interface LayerExporter extends DataExporter {
	/**
	* Returns a short text to explain the user what kind of layer may be saved
	* in this format. For example, "vector layer", "raster layer", "point objects"
	*/
	public String getDataChar();

	/**
	 * Replies if this exporter is applicable to a layer of the given type and,
	 * possibly, subtype.
	 */
	public boolean isApplicable(char layerType, char subType);
}
