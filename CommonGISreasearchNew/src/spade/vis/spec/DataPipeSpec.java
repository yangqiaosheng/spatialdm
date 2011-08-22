package spade.vis.spec;

/**
* Contains the specification of a "datapipe": the system may get data from some
* external source and ask the user to provide georeferences for the new data.
* Then the data can be stored in a specified file, in particular, on a server.
* In the latter case a script or servlet must be used for storing the data.
*/
public class DataPipeSpec implements java.io.Serializable {
	/**
	* The URL or file from which the data come. The system should detect when
	* new data appear in this source.
	*/
	public String dataSource = null;
	/**
	* The identifier of the map layer to which the new data must be added
	* (after the user has georeferenced them).
	*/
	public String layerId = null;
	/**
	* The URL of the script or servlet used for storing data on a server. A
	* local variant of the system may store the data directly in a file, without
	* using any script. In this case "updater" may be null.
	*/
	public String updater = null;
	/**
	* The path to the table file (on the server) in which the new data must be
	* stored. The data are normally added to the end of the file. For line and
	* area objects, thematic data are written to an ASCII file while the
	* geometries are stored in an *.ovl file. The latter normally has the same
	* name as the table file, only the extension is different.
	*/
	public String tableFileName = null;
	/**
	* The delimiter to be used in the table with object identifiers, names, and
	* thematic data (for point objects - also coordinates). By default, comma
	* is used. The delimiter must be a 1-symbol string.
	*/
	public String delimiter = ",";
}