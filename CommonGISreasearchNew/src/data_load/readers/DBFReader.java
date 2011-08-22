package data_load.readers;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.lib.lang.Language;
import spade.lib.util.Formats;
import spade.lib.util.StringUtil;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.spec.DataSourceSpec;

/**
* A class that reads a DBF file with data
*/

public class DBFReader extends TableReader {
	static ResourceBundle res = Language.getTextResource("data_load.readers.Res");
	/**
	* Data from the DBF file header: N of rows and columns, field names and
	* types, sizes of the header and a record
	*/
	protected int nRows = 0, nCols = 0;
	protected Vector fieldNames = null;
	protected char types[] = null;
	protected int lengths[] = null;
	protected int HeadSize = 0; // size of the header
	protected int RecSize = 0; // size of a record
	protected boolean headerGot = false;

	/**
	* Loads the data. When allowed (the argument mayAskUser is true) and necessary,
	* starts a dialog or a wizard for the user
	* to specify the data source, constraints, etc. Returns true if the data
	* have been successfully loaded.
	*/
	@Override
	public boolean loadData(boolean mayAskUser) {
		if (dataError)
			return false;
		if (dataReadingInProgress) {
			waitDataReadingFinish();
			return !dataError;
		}
		if (!mayAskUser) {
			setDataReadingInProgress(true);
		}
		if (spec == null)
			if (mayAskUser) {
				//following text:"Select the file with the table","
				String path = browseForFile(res.getString("Select_the_file_with1"), "*.dbf");
				if (path == null) {
					setDataReadingInProgress(false);
					return false;
				}
				spec = new DataSourceSpec();
				spec.source = path;
			} else {
				//following text:"The table data source is not specified!"
				showMessage(res.getString("The_table_data_source"), true);
				setDataReadingInProgress(false);
				return false;
			}
		if (spec.idFieldN < 0 && spec.idFieldName == null && mayAskUser) {
			//start a dialog for getting information about the data from the user
			DataSample sample = getDataSample(20);
			if (sample == null) {
				dataError = true;
				setDataReadingInProgress(false);
				return false;
			}
			DataPreviewDlg dpd = new DataPreviewDlg(getFrame(), sample);
			dpd.show();
			if (dpd.wasCancelled()) {
				setDataReadingInProgress(false);
				return false;
			}
			spec.idFieldN = dpd.getIdFieldN();
			spec.nameFieldN = dpd.getNameFieldN();
			spec.xCoordFieldName = dpd.getXCoordFName();
			spec.yCoordFieldName = dpd.getYCoordFName();
		}
		//following text:"Start reading data from "
		showMessage(res.getString("Start_reading_data") + spec.source, false);
		closeStream();
		openStream(); //to be at the beginning
		if (stream == null) {
			dataError = true;
			setDataReadingInProgress(false);
			return false;
		}
		constructTable();
		dataError = !readSpecific();
		closeStream();
		if (dataError) {
			setDataReadingInProgress(false);
			return false;
		}
		setDataReadingInProgress(false);
		table.finishedDataLoading();
		if (data == null) {
			data = tryGetGeoObjects();
			if (data != null && layer != null) {
				layer.receiveSpatialData(data);
			}
		}
		return true;
	}

	/**
	* Reads the header of the DBF file
	*/
	protected boolean readFileHeader(DataInputStream f) {
		if (f == null)
			return false;
		headerGot = false;
		try { //reading file header
			/*
			if (f.length()<32) {
			  notifyProcessState("No data in "+filename,true);
			  return null;
			}
			*/
			int Key = f.read();
			if (Key != 0x03 && Key != 0x83) {
				//followimg text:"Wrong file format: "
				showMessage(res.getString("Wrong_file_format_") + Key, true);
				return false;
			}
			byte b4[] = new byte[4];
			f.readFully(b4, 0, 3); //just to skip 3 bytes
			f.readFully(b4, 0, 4);
			nRows = Formats.getInt(b4);
			//System.out.println("nRows="+nRows);
			byte b2[] = new byte[2];
			f.readFully(b2);
			HeadSize = Formats.getUnsignedShort(b2);
			nCols = (HeadSize >> 5) - 1;
			//System.out.println("HeadSize="+HeadSize+" nCols="+nCols);
			f.readFully(b2);
			RecSize = Formats.getUnsignedShort(b2);
			//System.out.println("RecSize="+RecSize);
			byte b20[] = new byte[20];
			f.readFully(b20); //just to skip these 20 bytes
			types = new char[nCols];
			lengths = new int[nCols];
			fieldNames = new Vector(nCols, 10);
			for (int i = 0; i < nCols; i++) {
				byte b11[] = new byte[11];
				f.readFully(b11);
				fieldNames.addElement(new String(b11).trim());
				f.readFully(b2, 0, 1);
				types[i] = (new String(b2, 0, 1)).charAt(0);
				f.readFully(b4); //to skip 4 bytes
				lengths[i] = f.readUnsignedByte();
				int prec = f.readUnsignedByte();
				f.readFully(b20, 0, 14); //to skip 14 bytes
				//System.out.println(fieldNames.elementAt(i).toString()+" "+types[i]+" "+
				//                   lengths[i]+"."+prec);
				switch (types[i]) {
				case 'D':
					types[i] = AttributeTypes.time;
					break;
				case 'L':
					types[i] = AttributeTypes.logical;
					break;
				case 'N':
					if (prec == 0) {
						types[i] = AttributeTypes.integer;
					} else {
						types[i] = AttributeTypes.real;
					}
					break;
				default:
					types[i] = AttributeTypes.character;
				}
			} //for (int i=0; i<nCols; i++)
			f.read(); //end of header
		} catch (IOException ioe) {
			//following text:"Exception reading file header: "
			showMessage(res.getString("Exception_reading2") + ioe, true);
			return false;
		}
		headerGot = true;
		return true;
	}

	protected boolean skipFileHeader(DataInputStream f) {
		if (headerGot && HeadSize > 0) {
			byte bb[] = new byte[HeadSize];
			try {
				f.readFully(bb); //to skip HeadSize bytes
			} catch (IOException ioe) {
				//followimg text:"Exception reading file header: "
				showMessage(res.getString("Exception_reading2") + ioe, true);
				return false;
			}
			return true;
		}
		return false;
	}

	/**
	* Gets DataRecords from an DBF file
	*/
	protected boolean readSpecific() {
		if (stream == null)
			return false;
		DataInputStream f = new DataInputStream(stream);
		if (!headerGot)
			if (!readFileHeader(f))
				return false;
			else {
				;
			}
		else if (!skipFileHeader(f))
			return false;
		if (spec.idFieldName != null) {
			spec.idFieldN = StringUtil.indexOfStringInVectorIgnoreCase(spec.idFieldName, fieldNames);
		}
		if (spec.nameFieldName != null) {
			spec.nameFieldN = StringUtil.indexOfStringInVectorIgnoreCase(spec.nameFieldName, fieldNames);
		}
		for (int i = 0; i < nCols; i++) {
			if (i != spec.idFieldN && i != spec.nameFieldN) {
				table.addAttribute((String) fieldNames.elementAt(i), types[i]);
			}
		} //for (int i=0; i<nCols; i++)
		try {
			int rowNdx = 0; //reading data
			while (rowNdx < nRows) { //reading data
				Vector values = new Vector();
				String id = null, name = null, s = null;
				byte flag = f.readByte();
				//System.out.print((rowNdx+1)+")");
				if (flag == '\u0020') { //record not deleted
					for (int j = 0; j < nCols; j++) {
						byte b[] = new byte[lengths[j]];
						f.readFully(b);
						s = (new String(b)).trim();
						//System.out.print(" "+s);
						if (j == spec.idFieldN) {
							id = s;
						} else if (j == spec.nameFieldN) {
							name = s;
						} else {
							values.addElement(s);
						}
					} //for (int j=0; j<nCols; j++)
						//System.out.println();
					if (id == null || id.length() < 1) {
						id = String.valueOf(rowNdx + 1);
					}
					DataRecord drec = new DataRecord(id, name);
					for (int j = 0; j < values.size(); j++) {
						drec.addAttrValue(values.elementAt(j));
					}
					table.addDataRecord(drec);
				} //if (flag=='\u0020')
					//else
					//System.out.println(" deleted");
				rowNdx++;
				if (rowNdx % 50 == 0) {
					//followimg text:"Got "
					//followimg text:" records  of "
					showMessage(res.getString("Got") + rowNdx + res.getString("records_of") + nRows, false);
				}
			} //while (rowNdx<nRows)
				//followimg text:"Got "
				//followimg text:" records  of "
			showMessage(res.getString("Got") + rowNdx + res.getString("records_of") + nRows, false);
		} catch (IOException ioe) {
			//followimg text:"Exception: "
			showMessage(res.getString("Exception_") + ioe, true);
			return false;
		}
		return table.hasData();
	}

	/**
	* Reading of data sample - some portion of data records for preview.
	*/
	protected DataSample getDataSample(int maxNRows) {
		closeStream();
		openStream(); //to be at the beginning
		if (stream == null)
			return null;
		DataInputStream f = new DataInputStream(stream);
		if (!headerGot)
			if (!readFileHeader(f))
				return null;
			else {
				;
			}
		else if (!skipFileHeader(f))
			return null;
		if (maxNRows > nRows) {
			maxNRows = nRows;
		}
		DataSample sample = new DataSample();
		for (int i = 0; i < nCols; i++) {
			sample.addField((String) fieldNames.elementAt(i), types[i]);
		}
		try {
			int rowNdx = 0; //reading data
			while (rowNdx < maxNRows) { //reading data
				byte flag = f.readByte();
				if (flag == '\u0020') { //record not deleted
					Vector values = new Vector();
					for (int j = 0; j < nCols; j++) {
						byte b[] = new byte[lengths[j]];
						f.readFully(b);
						values.addElement((new String(b)).trim());
					} //for (int j=0; j<nCols; j++)
					sample.addDataRecord(values);
				} //if (flag=='\u0020')
				rowNdx++;
				if (rowNdx % 50 == 0) {
					//followimg text:"Got "
					//followimg text:" sample records  of "
					showMessage(res.getString("Got") + rowNdx + res.getString("sample_records_of") + nRows, false);
				}
			} //while (rowNdx<nRows)
				//followimg text:"Got "
				//followimg text:" sample records  of "
			showMessage(res.getString("Got") + rowNdx + res.getString("sample_records_of") + nRows, false);
		} catch (IOException ioe) {
			//followimg text:"Exception: "
			showMessage(res.getString("Exception_") + ioe, true);
			return null;
		}
		return sample;
	}
}
