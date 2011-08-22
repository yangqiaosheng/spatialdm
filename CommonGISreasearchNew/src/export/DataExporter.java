package export;

import java.io.OutputStream;
import java.util.Vector;

import spade.analysis.system.SystemUI;
import spade.vis.database.ObjectFilter;
import spade.vis.spec.DataSourceSpec;

/**
* The interface to be implemented by classes for exporting data
* (tables or map layers) from Descartes into files of various formats.
*/
public interface DataExporter {
	/**
	* Returns the name of the output format suitable for showing to the user.
	* For example, "Shape (ArcView)", "csv (Excel-compatible)"
	*/
	public String getFormatName();

	/**
	* Returns the usual file extension corresponding to this export format
	*/
	public String getFileExtension();

	/**
	* Returns true if this data exporter can store attributes from a table
	*/
	public boolean canWriteAttributes();

	/**
	* Returns true if the format requires creation of more than one files
	*/
	public boolean needsMultipleFiles();

	/**
	* Writes the data to the given stream. The SystemUI provided may be used for
	* displaying diagnostic messages. The exporter must check if the object passed 
	* to it has the required type. Returns true if the data have been successfully
	* stored. Arguments:
	* data:          the table or layer to be stored
	* filter:        filter of records or objects. May be null. If not null, only
	*                the records (objects) satisfying the filter must be stored.
	* selAttr:       selected attributes to be stored. If null, no attributes
	*                are stored. Not appropriate for exporters that only store
	*                geographic data.
	* stream:        the stream in which to put the data (not necessarily a file,
	*                may be, for example, a script.
	* This method is not suitable for exporters that need to write to several files!
	*/
	public boolean storeData(Object data, ObjectFilter filter, Vector selAttr, OutputStream stream, SystemUI ui);

	/**
	* Some formats, for example, Shape or ADF, require data to be stored in several
	* files. In this case the data cannot be written in just one stream and,
	* hence, the previous storeData method is not applicable. For this case
	* the method with arguments directory and file name insead of a stream
	* must be defined. Exporters that save data in just one file should in this
	* method open the specified file as a stream and call the method with the
	* stream argument.
	*/
	public boolean storeData(Object data, ObjectFilter filter, Vector selAttr, String dir, String filename, SystemUI ui);

	/**
	 * Returns the description of the file or database table to which the data
	 * were stored
	 */
	public DataSourceSpec getStoredDataDescriptor();
}