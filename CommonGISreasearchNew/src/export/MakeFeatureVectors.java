package export;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.SystemUI;
import spade.lib.lang.Language;
import spade.lib.util.IntArray;
import spade.lib.util.StringUtil;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.ObjectFilter;
import spade.vis.spec.DataSourceSpec;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Jul 10, 2009
 * Time: 1:33:54 PM
 * From a given table produces a file with feature vectors in
 * FeatureVector2 format (Darmstadt).
 */
public class MakeFeatureVectors implements DataExporter {
	static ResourceBundle res = Language.getTextResource("export.Res");

	/**
	* Returns the name of the output format suitable for showing to the user.
	* For example, "Shape (ArcView)", "csv (comma-separated values)"
	*/
	@Override
	public String getFormatName() {
		return "fv2 (feature vectors, tab-separated)";
	}

	/**
	* Returns the usual file extension corresponding to this export format
	*/
	@Override
	public String getFileExtension() {
		return "fv2";
	}

	/**
	* Returns true if this data exporter can store attributes from a table
	*/
	@Override
	public boolean canWriteAttributes() {
		return true;
	}

	/**
	* Returns true if the format requires creation of more than one files
	*/
	@Override
	public boolean needsMultipleFiles() {
		return false;
	}

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
	@Override
	public boolean storeData(Object data, ObjectFilter filter, Vector selAttr, OutputStream stream, SystemUI ui) {
		if (data == null || stream == null)
			return false;
		if (!(data instanceof AttributeDataPortion)) {
			// following string: "Illegal data type: AttributeDataPortion expected!"
			if (ui != null) {
				ui.showMessage(res.getString("Illegal_data_type3"), true);
			}
			return false;
		}
		AttributeDataPortion table = (AttributeDataPortion) data;
		if (!table.hasData() && table.getAttrCount() < 1) {
			// following string: "No data in the table!"
			if (ui != null) {
				ui.showMessage(res.getString("No_data_in_the_table_"), true);
			}
			return false;
		}
		DataOutputStream dos = new DataOutputStream(stream);
		int nrows = 0;
		IntArray attrNumbers = null;
		if (selAttr != null && selAttr.size() > 0 && table != null && table.getAttrCount() > 0) {
			attrNumbers = new IntArray(selAttr.size(), 1);
			for (int i = 0; i < selAttr.size(); i++) {
				int idx = table.getAttrIndex((String) selAttr.elementAt(i));
				if (idx >= 0) {
					attrNumbers.addElement(idx);
				}
			}
		}
		try {
			// following string: "Writing data..."
			if (ui != null) {
				ui.showMessage(res.getString("Writing_data_"), false);
			}
			String str = "TimeID\tName\tObjectID";
			if (attrNumbers != null) {
				for (int j = 0; j < attrNumbers.size(); j++) {
					str += "\t" + table.getAttributeName(attrNumbers.elementAt(j));
				}
			}
			dos.writeBytes(str + "\r\n");
			for (int i = 0; i < table.getDataItemCount(); i++) {
				boolean active = true;
				if (filter != null) {
					active = filter.isActive(i);
				} else if (filter != null) {
					active = filter.isActive(table.getDataItem(i));
				}
				if (active) {
					str = "1\t" + StringUtil.eliminateCommas(table.getDataItemName(i)) + "\t" + String.valueOf(i + 1);
					if (attrNumbers != null) {
						for (int j = 0; j < attrNumbers.size(); j++) {
							String val = table.getAttrValueAsString(attrNumbers.elementAt(j), i);
							if (val == null) {
								val = "0";
							}
							str += "\t" + val;
						}
					}
					dos.writeBytes(str + "\r\n");
					++nrows;
					if (ui != null && nrows % 100 == 0) {
						// following string: " rows stored"
						ui.showMessage(nrows + res.getString("rows_stored"), false);
					}
				}
			}
		} catch (IOException ioe) {
			if (ui != null) {
				// following string: "Error writing to the file: "
				ui.showMessage(res.getString("Error_writing_to_the") + ioe.toString(), true);
			}
			return false;
		}
		if (ui != null)
			// following string: " rows stored"
			if (nrows > 0) {
				ui.showMessage(nrows + res.getString("rows_stored"), false);
			} else {
				ui.showMessage(res.getString("No_records_actually"), true);
			}
		return nrows > 0;
	}

	/**
	* Some formats, for example, Shape or ADF, require data to be stored in several
	* files. In this case the data cannot be written in just one stream and,
	* hence, the previous storeData method is not applicable. For this case
	* the method with arguments directory and file name insead of a stream
	* must be defined. Exporters that save data in just one file should in this
	* method open the specified file as a stream and call the method with the
	* stream argument.
	*/
	@Override
	public boolean storeData(Object data, ObjectFilter filter, Vector selAttr, String dir, String filename, SystemUI ui) {
		if (data == null || filename == null)
			return false;
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(dir + filename);
		} catch (IOException ioe) {
			if (ui != null) {
				ui.showMessage(ioe.toString(), true);
			}
			return false;
		}
		boolean result = false;
		if (out != null) {
			result = storeData(data, filter, selAttr, out, ui);
			try {
				out.close();
			} catch (IOException e) {
			}
			return result;
		}
		// following string: "Could not open the file "
		if (ui != null) {
			ui.showMessage(res.getString("Could_not_open_the") + dir + filename, true);
		}
		return false;
	}

	/**
	 * Returns the description of the file or database table to which the data
	 * were stored
	 */
	@Override
	public DataSourceSpec getStoredDataDescriptor() {
		return null;
	}
}
