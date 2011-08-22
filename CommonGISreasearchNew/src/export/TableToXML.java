package export;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 04-Nov-2005
 * Time: 15:36:07
 * This is a class for writing the contents of a Descartes table into XML format
 */

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.SystemUI;
import spade.lib.lang.Language;
import spade.lib.util.IntArray;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.DataTable;
import spade.vis.database.ObjectFilter;
import spade.vis.spec.DataSourceSpec;
import spade.vis.spec.ParamSpec;

public class TableToXML implements DataExporter {
	static ResourceBundle res = Language.getTextResource("export.Res");
	/**
	 * Descriptor of the file to which the  data were stored
	 */
	protected DataSourceSpec spec = null;

	/**
	* Returns the name of the output format suitable for showing to the user.
	* For example, "Shape (ArcView)", "csv (comma-separated values)"
	*/
	@Override
	public String getFormatName() {
		return "XML";
	}

	/**
	* Returns the usual file extension corresponding to this export format
	*/
	@Override
	public String getFileExtension() {
		return "xml";
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
		int nObj = 0;
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
			dos.writeBytes("<?xml version='1.0'?>\n\n<ObjectData>\n\n");
			for (int i = 0; i < table.getDataItemCount(); i++) {
				boolean active = true;
				if (filter != null) {
					active = filter.isActive(i);
				} else if (filter != null) {
					active = filter.isActive(table.getDataItem(i));
				}
				if (active) {
					dos.writeBytes("<Object>\n");
					dos.writeBytes("\t<id>" + table.getDataItemId(i) + "</id>\n");
					dos.writeBytes("\t<name>" + table.getDataItemName(i) + "</name>\n");
					if (attrNumbers != null) {
						for (int j = 0; j < attrNumbers.size(); j++) {
							int idx = attrNumbers.elementAt(j);
							String val = table.getAttrValueAsString(idx, i);
							if (val != null) {
								dos.writeBytes("\t<property name=\"" + table.getAttributeName(idx) + "\" type=\"" + getTypeAsString(table.getAttributeType(idx)) + "\">" + val + "</property>\n");
							}
						}
					}
					dos.writeBytes("</Object>\n\n");
					++nObj;
					if (ui != null && nObj % 100 == 0) {
						// following string: " rows stored"
						ui.showMessage(nObj + res.getString("rows_stored"), false);
					}
				}
			}
			dos.writeBytes("</ObjectData>\n");
		} catch (IOException ioe) {
			if (ui != null) {
				// following string: "Error writing to the file: "
				ui.showMessage(res.getString("Error_writing_to_the") + ioe.toString(), true);
			}
			return false;
		}
		if (ui != null)
			// following string: " rows stored"
			if (nObj > 0) {
				ui.showMessage(nObj + res.getString("rows_stored"), false);
			} else {
				ui.showMessage(res.getString("No_records_actually"), true);
			}
		return nObj > 0;
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
			if (result) {
				makeSourceSpecification(data, dir + filename);
			}
			return result;
		}
		// following string: "Could not open the file "
		if (ui != null) {
			ui.showMessage(res.getString("Could_not_open_the") + dir + filename, true);
		}
		return false;
	}

	protected void makeSourceSpecification(Object data, String source) {
		if (!(data instanceof DataTable))
			return;
		DataTable dtab = (DataTable) data;
		if (spec == null) {
			spec = new DataSourceSpec();
		}
		spec.source = source;
		spec.format = "XML";
		spec.idFieldName = "id";
		spec.nameFieldName = "Name";
		spec.name = dtab.getName();

		DataSourceSpec dss = null;
		if (dtab.getDataSource() != null && (dtab.getDataSource() instanceof DataSourceSpec)) {
			dss = (DataSourceSpec) dtab.getDataSource();
		}
		if (dss != null && dss.descriptors != null) {
			for (int i = 0; i < dss.descriptors.size(); i++)
				if (!(dss.descriptors.elementAt(i) instanceof ParamSpec)) {
					if (spec.descriptors == null) {
						spec.descriptors = new Vector(5, 5);
					}
					spec.descriptors.addElement(dss.descriptors.elementAt(i));
				}
		}
		if (dss == null || dss.source == null || dss.source.equalsIgnoreCase("_derived")) {
			dtab.setDataSource(spec);
		}
	}

	public static String getTypeAsString(char attrType) {
		switch (attrType) {
		case 'I':
		case 'i':
		case 'R':
		case 'r':
			return "numeric";
		case 'C':
		case 'c':
			return "string";
		case 'L':
		case 'l':
			return "boolean";
		case 'T':
		case 't':
			return "time";
		case 'G':
		case 'g':
			return "geometry";
		}
		return "string";
	}

	/**
	 * Returns the description of the file or database table to which the data
	 * were stored
	 */
	@Override
	public DataSourceSpec getStoredDataDescriptor() {
		return spec;
	}
}
