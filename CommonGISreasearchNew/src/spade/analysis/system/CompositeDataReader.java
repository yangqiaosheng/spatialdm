package spade.analysis.system;

/**
* This interface is to be implemented by data readers that load associated
* geographic and thematic data from different sources, for example, geography
* from a *.SHP file and object identifiers, names, and attributes from a
* *.DBF file. Such readers need an access to the system's DataReaderFactory
* in order to load thematic data with the use of the appropriate reader.
*/
public interface CompositeDataReader {
	/**
	* Sets the DataReaderFactory that can produce an appropriate reader for
	* the additional data source
	*/
	public void setDataReaderFactory(DataReaderFactory factory);
}