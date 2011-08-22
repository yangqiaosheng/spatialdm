package data_load.readers;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.Vector;

import spade.lib.lang.Language;
import spade.lib.util.CopyFile;
import spade.lib.util.IntArray;
import spade.lib.util.StringUtil;
import spade.time.Date;
import spade.time.TimeCount;
import spade.time.TimeMoment;
import spade.time.TimeRefDescription;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.LayerData;
import spade.vis.database.SpatialEntity;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DMovingObject;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealRectangle;
import spade.vis.spec.DataSourceSpec;

/**
* A class that reads an ASCII file with data where fields are separated by
* delimiters such as commas, semicolons etc. In particular, can read data
* from the CSV (comma-separated values) format. By default, assumes that the
* spec.delimiter is ','. If another spec.delimiter is used in the data file to be read,
* the default value should be changed.
* Each line of data in the file must contain the identifier of the object the
* data from the line refer to.
* The lines may contain also full names of the objects. In this case the
* ASCIIReader should be informed the contents of what field is to be interpreted
* as object names.
* If the file with data contains a specially prepared header with data
* description (metadata), the ASCIIReader can itself get the information
* about the spec.delimiter, the numbers of the fields with object identifiers
* and names, and attribute names and types. If the data file does not contain
* this information, one should make all necessary settings in the DataSourceSpec
* that is given to the reader
*/

public class ASCIIReader extends TableReader {
	static ResourceBundle res = Language.getTextResource("data_load.readers.Res");
	/**
	 * maximal lines will be loaded, if -1 then  all lines
	 * for sample read in MIFReader. ~MO
	 */
	public int maxLines = -1;

	/**
	* Loads the data. When allowed (the argument mayAskUser is true) and necessary,
	* starts a dialog or a wizard for the user
	* to specify the data source, constraints, etc. Returns true if the data
	* have been successfully loaded.
	*/
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
				//following text:"Select the file with the table"
				String path = browseForFile(res.getString("Select_the_file_with1"), "*.csv;*.txt;*.dat");
				System.out.println("Path=" + path);
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
		Vector header = getNDataLines(20);
		if (header == null || header.size() < 1) {
			dataError = true;
			setDataReadingInProgress(false);
			return false;
		}
		String line = (String) header.elementAt(0);
		if (line.equalsIgnoreCase("<REGIONS>")) {
			table = null;
			closeStream();
			openStream(); //to be at the beginning
			if (stream == null) {
				dataError = true;
				setDataReadingInProgress(false);
				return false;
			}
			boolean ok = loadRegions(new BufferedReader(new InputStreamReader(stream)));
			closeStream();
			setDataReadingInProgress(false);
			return ok;
		}
		if (mayAskUser && (spec.delimiter == null || (spec.idFieldN < 0 && spec.idFieldName == null))) {
			//try to get metadata
			//start a dialog for getting information about the data from the user
			ASCIIReadDlg dlg = new ASCIIReadDlg(getFrame(), spec, header);
			dlg.show();
			if (dlg.wasCancelled()) {
				setDataReadingInProgress(false);
				return false;
			}
		}
		if (spec.delimiter == null) {
			//following text:"The delimiter is not specified for "
			showMessage(res.getString("The_delimiter_is_not") + spec.source, true);
			setDataReadingInProgress(false);
			return false;
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
		dataError = !readSpecific(new BufferedReader(new InputStreamReader(stream)), true);
		closeStream();
		if (dataError) {
			setDataReadingInProgress(false);
			return false;
		}
		setDataReadingInProgress(false);
		if (table != null)
			if (table.hasData()) {
				table.finishedDataLoading();
			} else {
				table = null;
			}
		if (data == null && layer == null) {
			data = tryGetGeoObjects();
			if (data != null && layer != null) {
				layer.receiveSpatialData(data);
			}
		}
		return true;
	}

	/**
	* Reads metadata contained in the header of the stream with data between the
	* lines METADATA_START and METADATA_END. Returns true if all the metadata
	* have been normally received, i.e. the indicator METADATA_END occurred.
	* The metadata got are used to setup parameters of the GeoDataBase passed
	* as an argument.
	*/
	protected boolean getMetaData(BufferedReader reader) {
		//following text:"reading metadata from "
		showMessage(res.getString("reading_metadata_from") + spec.source, false);
		while (true) {
			try {
				String s = reader.readLine();
				if (s == null) {
					break;
				}
				s = s.trim();
				System.err.println(s);
				if (s.length() < 1) {
					continue;
				}
				if (s.equalsIgnoreCase("METADATA_END")) {
					//following text:"Metadata got from "
					showMessage(res.getString("Metadata_got_from") + spec.source, false);
					return true;
				}
				StringTokenizer st = new StringTokenizer(s, " =;\r\n");
				if (st.countTokens() < 2) {
					continue;
				}
				String key = st.nextToken();
				if (key.equalsIgnoreCase("DELIMITER")) {
					String delim = StringUtil.removeQuotes(st.nextToken(" =\r\n"));
					spec.setDelimiter(delim);
				} else if (key.equalsIgnoreCase("FIELD_NAMES_IN_ROW")) {
					String str = st.nextToken();
					if (str != null) {
						try {
							int k = Integer.valueOf(str).intValue();
							spec.nRowWithFieldNames = k - 1;
						} catch (NumberFormatException nfe) {
							//following text:"Unrecognizable number of row with field names!"
							showMessage(res.getString("Unrecognizable_number"), true);
						}
					}
				} else if (key.equalsIgnoreCase("FIELD_TYPES_IN_ROW")) {
					String str = st.nextToken();
					if (str != null) {
						try {
							int k = Integer.valueOf(str).intValue();
							spec.nRowWithFieldTypes = k - 1;
						} catch (NumberFormatException nfe) {
							//following text:"Unrecognizable number of row with field types!"
							showMessage(res.getString("Unrecognizable_number1"), true);
						}
					}
				} else if (key.equalsIgnoreCase("ID_FIELD_N")) {
					String str = st.nextToken();
					if (str != null) {
						try {
							int k = Integer.valueOf(str).intValue();
							spec.idFieldN = k - 1;
						} catch (NumberFormatException nfe) {
							//following text:"Unrecognizable number of column with identifiers!"
							showMessage(res.getString("Unrecognizable_number2"), true);
						}
					}
				} else if (key.equalsIgnoreCase("ID_FIELD_NAME")) {
					String fname = StringUtil.removeQuotes(s.substring(key.length() + 1).trim());
					spec.idFieldName = fname;
				} else if (key.equalsIgnoreCase("NAME_FIELD_N")) {
					String str = st.nextToken();
					if (str != null) {
						try {
							int k = Integer.valueOf(str).intValue();
							spec.nameFieldN = k - 1;
						} catch (NumberFormatException nfe) {
							//following text:"Unrecognizable number of column with names!"
							showMessage(res.getString("Unrecognizable_number3"), true);
						}
					}
				} else if (key.equalsIgnoreCase("NAME_FIELD_NAME")) {
					String fname = StringUtil.removeQuotes(s.substring(key.length() + 1).trim());
					spec.nameFieldName = fname;
				}
			} catch (IOException ioe) {
				//following text:"Exception reading metadata: "
				showMessage(res.getString("Exception_reading1") + ioe, true);
				return false;
			}
		}
		//following text:"No METADATA_END indicator occurred!"
		showMessage(res.getString("No_METADATA_END"), true);
		return false; //no METADATA_END indicator occurred
	}

	protected boolean getMetaData(InputStream stream) {
		return getMetaData(new BufferedReader(new InputStreamReader(stream)));
	}

	/**
	* Skips the metadata part of the stream, i.e. everything that comes between
	* the lines METADATA_START and METADATA_END. Returns true if the indicator
	* METADATA_END occurred.
	*/
	protected boolean skipMetaData(BufferedReader reader) {
		//following text:"skipping the header of the file "
		showMessage(res.getString("skipping_the_header") + spec.source, false);
		while (true) {
			try {
				String s = reader.readLine();
				if (s == null) {
					break;
				}
				if (s.length() < 1) {
					continue;
				}
				if (s.equalsIgnoreCase("METADATA_END")) {
					//following text:"Metadata end found in "
					showMessage(res.getString("Metadata_end_found_in") + spec.source, false);
					return true;
				}
			} catch (IOException ioe) {
				//following text:"Exception reading metadata: "
				showMessage(res.getString("Exception_reading1") + ioe, true);
				return false;
			}
		}
		//following text:"No METADATA_END indicator occurred!"
		showMessage(res.getString("No_METADATA_END"), true);
		return false; //no METADATA_END indicator occurred
	}

	/**
	* Retrieves substrings from a string with separators. Puts the substrings
	* retrieved to the vector supplied as an argument. If it is null, creates
	* this vector and returns it as the result. If the argument is not null,
	* clears the vector (removes all elements).
	* Correctly handles the situation with several separators without strings
	* between them (adds empty strings to the vector).
	*/
	protected Vector getStrings(String str, Vector strings) {
		if (str == null)
			return null;
		if (strings == null) {
			strings = new Vector(50, 20);
		} else {
			strings.removeAllElements();
		}
		int p1 = 0, p2 = 0;
		boolean getTextsFromQuotes = spec.delimiter.charAt(0) != '\"';
		do {
			if (spec.delimiter.charAt(0) != ' ') {
				while (str.charAt(p1) == ' ') {
					++p1;
				}
			}
			boolean haveQuotes = false;
			if (getTextsFromQuotes && str.charAt(p1) == '\"') {
				p2 = str.indexOf('\"', p1 + 1);
				if (p2 > p1) {
					++p1;
					haveQuotes = true;
				}
			} else {
				p2 = str.indexOf(spec.delimiter.charAt(0), p1);
			}
			if (p2 < 0) {
				p2 = str.length();
			}
			strings.addElement(str.substring(p1, p2).trim());
			if (getTextsFromQuotes && haveQuotes) {
				p1 = str.indexOf(spec.delimiter.charAt(0), p2 + 1);
				if (p1 < 0) {
					p1 = str.length();
				} else {
					++p1;
				}
			} else {
				p1 = p2 + 1;
			}
		} while (p1 < str.length());
		/*
		for (int i=0; i<strings.size(); i++)
		  System.err.print("["+strings.elementAt(i)+"]");
		System.err.println();
		*/
		return strings;
	}

	/**
	* Equivalent to getStrings(str,null)
	*/
	protected Vector getStrings(String str) {
		return getStrings(str, null);
	}

	/**
	* Gets DataRecords from a BufferedReader
	*/
	protected boolean readSpecific(BufferedReader reader, boolean ignoreMetaData) {
		if (reader == null)
			return false;
		if (describesTrajectories(spec))
			return getTrajectories(reader, ignoreMetaData);
		int k = 0;
		Vector fieldNames = null, fieldTypes = null, values = null;
		boolean start = true, attributesGot = false;
		String header = null;
		while (true) {
			try {
				String s = reader.readLine();
				if (s == null) {
					break;
				}
				s = s.trim();
				if (s.length() < 1) {
					continue;
				}
				if (start) { //at the beginning of the stream some header may
					//be present. The stream can also contain metadata.
					StringTokenizer st = new StringTokenizer(s, " \n\r");
					if (st.hasMoreTokens()) {
						String tok = st.nextToken().toLowerCase();
						if (tok.startsWith("http") || tok.startsWith("accept-") || tok.startsWith("server") || tok.startsWith("date:") || tok.startsWith("last-modified:") || tok.startsWith("etag:") || tok.startsWith("content-")
								|| tok.startsWith("connection")) {
							continue;
						}
						if (tok.equals("metadata_start")) {
							if (ignoreMetaData)
								if (!skipMetaData(reader)) {
									break;
								} else {
									;
								}
							else if (!getMetaData(reader)) {
								break;
							}
							continue;
						}
					}
				}
				if (k == spec.nRowWithFieldNames) {
					start = false;
					header = s;
					fieldNames = getStrings(s);
					if (spec.idFieldName != null) {
						spec.idFieldN = StringUtil.indexOfStringInVectorIgnoreCase(spec.idFieldName, fieldNames);
					}
					if (spec.nameFieldName != null) {
						spec.nameFieldN = StringUtil.indexOfStringInVectorIgnoreCase(spec.nameFieldName, fieldNames);
					}
					//following text:"Got "
					//following text:" attribute names"
					showMessage(res.getString("Got") + fieldNames.size() + res.getString("attribute_names"), false);
				} else if (k == spec.nRowWithFieldTypes) {
					start = false;
					fieldTypes = getStrings(s);
					//following text:"Got "
					//following text:" attribute types"
					showMessage(res.getString("Got") + fieldTypes.size() + res.getString("attribute_types"), false);
				} else { //this is a row with data values
					if (header != null && header.equalsIgnoreCase(s)) {
						continue;
					}
					start = false;
					if (table == null) {
						constructTable();
					}
					if (!attributesGot) {
						//construct the list of attributes
						if (fieldNames != null && fieldNames.size() > 0) {
							for (int i = 0; i < fieldNames.size(); i++)
								if (spec.multipleRowsPerObject || (i != spec.idFieldN && i != spec.nameFieldN))
									if (fieldTypes == null || fieldTypes.size() <= i) {
										table.addAttribute((String) fieldNames.elementAt(i), AttributeTypes.character);
									} else {
										String typestr = (String) fieldTypes.elementAt(i);
										if (typestr.length() > 0) {
											table.addAttribute((String) fieldNames.elementAt(i), typestr.charAt(0));
										} else {
											table.addAttribute((String) fieldNames.elementAt(i), AttributeTypes.character);
										}
									}
						} else { //there are no field names. We should make them artificially
							//the number of fields is equal to the number of data values
							//in the string
							values = getStrings(s, values);
							if (values == null || values.size() < 1) {
								continue;
							}
							for (int i = 0; i < values.size(); i++)
								if (spec.multipleRowsPerObject || (i != spec.idFieldN && i != spec.nameFieldN))
									if (fieldTypes == null || fieldTypes.size() <= i) {
										table.addAttribute("attr" + (i + 1), AttributeTypes.character);
									} else {
										String typestr = (String) fieldTypes.elementAt(i);
										if (typestr.length() > 0) {
											table.addAttribute("attr" + (i + 1), typestr.charAt(0));
										} else {
											table.addAttribute("attr" + (i + 1), AttributeTypes.character);
										}
									}
						}
						attributesGot = true;
					}
					values = getStrings(s, values);
					if (values != null && values.size() > 0) {
						String id = null, name = null;
						if (!spec.multipleRowsPerObject && spec.idFieldN >= 0 && spec.idFieldN < values.size()) {
							id = (String) values.elementAt(spec.idFieldN);
						}
						if (id == null || id.length() < 1) {
							id = String.valueOf(k);
						}
						if (!spec.multipleRowsPerObject && spec.nameFieldN >= 0 && spec.nameFieldN < values.size()) {
							name = (String) values.elementAt(spec.nameFieldN);
						}
						DataRecord drec = new DataRecord(id, name);
						for (int i = 0; i < values.size(); i++)
							if (spec.multipleRowsPerObject || (i != spec.idFieldN && i != spec.nameFieldN)) {
								drec.addAttrValue((String) values.elementAt(i));
							}
						table.addDataRecord(drec);
					}
				}
				++k;
				if (k % 50 == 0) {
					//following text:" data lines read"
					showMessage(k + res.getString("data_lines_read"), false);
				}
				//~MO
				if (k > maxLines && maxLines > 0) {
					break;
				}
			} catch (EOFException ioe) {
				if (attributesGot) {
					//following text:"Data loaded; N of rows:
					//following text:"; N of fields: "
					showMessage(res.getString("Data_loaded_N_of_rows") + table.getDataItemCount() + res.getString("_N_of_fields_") + table.getAttrCount(), false);
				} else {
					//following text:"Unexpected end of file "
					//following text:"; no data loaded"
					showMessage(res.getString("Unexpected_end_of") + spec.source + res.getString("_no_data_loaded"), true);
				}
			} catch (IOException ioe) {
				//following text:"Error reading data: "
				showMessage(res.getString("Error_reading_data_") + ioe, true);
				break;
			}
		}
		if (attributesGot && (fieldTypes == null || fieldTypes.size() < table.getAttrCount())) {
			//following text:"Determining attribute types..."
			showMessage(res.getString("Determining_attribute"), false);
			table.determineAttributeTypes();
			//following text:"Finished determining attribute types in "
			showMessage(res.getString("Finished_determining") + spec.source, false);
		}
		return table.hasData();
	}

	/**
	* Gets at most N lines from an ASCII stream. Tries to find and interpret
	* metadata. The lines are not interpreted, simply stored in a vector.
	* Creates a BufferedReader and calls getNDataLines(BufferedReader,int)
	*/
	public Vector getNDataLines(int N) {
		closeStream();
		openStream();
		if (stream == null)
			return null;
		return getNDataLines(new BufferedReader(new InputStreamReader(stream)), N);
	}

	/**
	* Gets at most N lines from a BufferedReader. Tries to find and interpret
	* metadata. The lines are not interpreted, simply stored in a vector.
	*/
	public Vector getNDataLines(BufferedReader reader, int N) {
		if (reader == null)
			return null;
		Vector fileHead = new Vector(N, 10);
		boolean gotMetaData = false;
		while (fileHead.size() < N) {
			try {
				String s = reader.readLine();
				if (s == null) {
					break;
				}
				s = s.trim();
				if (s.length() < 1) {
					continue;
				}
				if (fileHead.size() < 1) { //at the beginning of the stream some header may
					//be present. The stream can also contain metadata.
					StringTokenizer st = new StringTokenizer(s, " \n\r");
					if (st.hasMoreTokens()) {
						String tok = st.nextToken().toLowerCase();
						if (tok.startsWith("http") || tok.startsWith("accept-") || tok.startsWith("server") || tok.startsWith("date:") || tok.startsWith("last-modified:") || tok.startsWith("etag:") || tok.startsWith("content-")
								|| tok.startsWith("connection")) {
							continue;
						}
						if (tok.equals("metadata_start")) {
							if (!getMetaData(reader)) {
								break;
							}
							gotMetaData = true;
							continue;
						}
					}
				}
				if (fileHead.size() == 0 && !gotMetaData && s.indexOf('\t') >= 0) {
					spec.setDelimiter("\t");
				}
				fileHead.addElement(s);
			} catch (IOException ioe) {
				break;
			}
		}
		closeStream();
		return fileHead;
	}

	/**
	 * Checks if the specification describes a file with trajectories
	 */
	public static boolean describesTrajectories(DataSourceSpec spec) {
		if (spec == null)
			return false;
		if (spec.objType != Geometry.line || spec.objSubType != Geometry.movement)
			return false;
		if (spec.idFieldN < 0 && spec.idFieldName == null)
			return false;
		if (spec.xCoordFieldName == null)
			return false;
		if (spec.yCoordFieldName == null)
			return false;
		if (spec.descriptors == null)
			return false; //must contain column with times
		TimeRefDescription td = null;
		for (int i = 0; i < spec.descriptors.size() && td == null; i++)
			if (spec.descriptors.elementAt(i) != null && (spec.descriptors.elementAt(i) instanceof TimeRefDescription)) {
				td = (TimeRefDescription) spec.descriptors.elementAt(i);
			}
		if (td == null || td.sourceColumns == null || td.sourceColumns.length < 1 || td.sourceColumns[0] == null)
			return false;
		return true;
	}

	/**
	* Gets data from a BufferedReader and builds trajectories from them
	*/
	protected boolean getTrajectories(BufferedReader reader, boolean ignoreMetaData) {
		if (reader == null || spec == null)
			return false;
		if (!describesTrajectories(spec)) {
			showMessage("The metadata of " + spec.source + " are inadequate for building trajectories!", true);
			return false;
		}
		TimeRefDescription td = null;
		for (int i = 0; i < spec.descriptors.size() && td == null; i++)
			if (spec.descriptors.elementAt(i) != null && (spec.descriptors.elementAt(i) instanceof TimeRefDescription)) {
				td = (TimeRefDescription) spec.descriptors.elementAt(i);
			}
		boolean hasDates = td.schemes != null && td.schemes.length > 0 && td.schemes[0] != null && !td.schemes[0].equalsIgnoreCase("a");
		int k = 0;
		Vector fieldNames = null, fieldTypes = null, values = null;
		boolean start = true;
		String header = null;
		Vector attrList = null;
		IntArray attrCNs = null;
		int xFN = -1, yFN = -1, timeFN = -1;
		boolean structureGot = false;
		Vector mObjects = new Vector(200, 100); //trajectories
		DMovingObject mObj = null;
		int np = 0;
		while (true) {
			try {
				String s = reader.readLine();
				if (s == null) {
					break;
				}
				s = s.trim();
				if (s.length() < 1) {
					continue;
				}
				if (start) { //at the beginning of the stream some header may
					//be present. The stream can also contain metadata.
					StringTokenizer st = new StringTokenizer(s, " \n\r");
					if (st.hasMoreTokens()) {
						String tok = st.nextToken().toLowerCase();
						if (tok.startsWith("http") || tok.startsWith("accept-") || tok.startsWith("server") || tok.startsWith("date:") || tok.startsWith("last-modified:") || tok.startsWith("etag:") || tok.startsWith("content-")
								|| tok.startsWith("connection")) {
							continue;
						}
						if (tok.equals("metadata_start")) {
							if (ignoreMetaData)
								if (!skipMetaData(reader)) {
									break;
								} else {
									;
								}
							else if (!getMetaData(reader)) {
								break;
							}
							continue;
						}
					}
				}
				if (k == spec.nRowWithFieldNames) {
					start = false;
					header = s;
					fieldNames = getStrings(s);
					if (spec.idFieldName != null) {
						spec.idFieldN = StringUtil.indexOfStringInVectorIgnoreCase(spec.idFieldName, fieldNames);
					}
					if (spec.nameFieldName != null) {
						spec.nameFieldN = StringUtil.indexOfStringInVectorIgnoreCase(spec.nameFieldName, fieldNames);
					}
					//following text:"Got "
					//following text:" attribute names"
					showMessage(res.getString("Got") + fieldNames.size() + res.getString("attribute_names"), false);
				} else if (k == spec.nRowWithFieldTypes) {
					start = false;
					fieldTypes = getStrings(s);
					//following text:"Got "
					//following text:" attribute types"
					showMessage(res.getString("Got") + fieldTypes.size() + res.getString("attribute_types"), false);
				} else { //this is a row with data values
					if (header != null && header.equalsIgnoreCase(s)) {
						continue;
					}
					start = false;
					if (!structureGot) {
						if (fieldNames == null || fieldNames.size() < 4) { //id,x,y,time
							showMessage("No appropriate column names found in file " + spec.source + "!", true);
							return false;
						}
						if (spec.idFieldN < 0) {
							showMessage("Column " + spec.idFieldName + " not found in file " + spec.source + "!", true);
							return false;
						}
						xFN = StringUtil.indexOfStringInVectorIgnoreCase(spec.xCoordFieldName, fieldNames);
						if (xFN < 0) {
							showMessage("Column " + spec.xCoordFieldName + " not found in file " + spec.source + "!", true);
							return false;
						}
						yFN = StringUtil.indexOfStringInVectorIgnoreCase(spec.yCoordFieldName, fieldNames);
						if (yFN < 0) {
							showMessage("Column " + spec.yCoordFieldName + " not found in file " + spec.source + "!", true);
							return false;
						}
						timeFN = StringUtil.indexOfStringInVectorIgnoreCase(td.sourceColumns[0], fieldNames);
						if (timeFN < 0) {
							showMessage("Column " + td.sourceColumns[0] + " not found in file " + spec.source + "!", true);
							return false;
						}
						structureGot = true;
						if (fieldNames.size() > 4) {
							attrList = new Vector(fieldNames.size());
							attrCNs = new IntArray(fieldNames.size(), 1);
							//construct the list of attributes
							for (int i = 0; i < fieldNames.size(); i++)
								if (i != spec.idFieldN && i != spec.nameFieldN && i != xFN && i != yFN && i != timeFN) {
									String aName = (String) fieldNames.elementAt(i);
									if (aName.equalsIgnoreCase("trN") || aName.equalsIgnoreCase("pIdx")) {
										continue;
									}
									Attribute attr = new Attribute(aName, AttributeTypes.real); //may be changed if non-numeric value appears
									attrList.addElement(attr);
									attrCNs.addElement(i);
								}
							if (attrList.size() < 1) {
								attrList = null;
								attrCNs = null;
							}
						}
					}
					values = getStrings(s, values);
					if (values != null && values.size() >= 4) { //id,x,y,time
						String id = (String) values.elementAt(spec.idFieldN);
						float x = getFloat((String) values.elementAt(xFN)), y = getFloat((String) values.elementAt(yFN));
						String timeStr = (String) values.elementAt(timeFN);
						if (id != null && !Float.isNaN(x) && !Float.isNaN(y) && timeStr != null) {
							TimeMoment t = null;
							if (hasDates) {
								Date dt = new Date();
								dt.setDateScheme(td.schemes[0]);
								t = dt;
							} else {
								t = new TimeCount();
							}
							t.setMoment(timeStr);
							if (t.isValid()) {
								DataRecord pRec = null;
								if (attrList != null) {
									pRec = new DataRecord(id + "_" + np);
									pRec.setAttrList(attrList);
									for (int i = 0; i < attrCNs.size(); i++) {
										int cN = attrCNs.elementAt(i);
										String val = null;
										if (cN >= 0 && cN < values.size()) {
											val = (String) values.elementAt(cN);
										}
										if (val == null || val.length() < 1) {
											continue;
										}
										Attribute attr = (Attribute) attrList.elementAt(i);
										if (attr.isNumeric()) {
											float fval = getFloat(val);
											if (Float.isNaN(fval)) {
												attr.setType(AttributeTypes.character);
											} else {
												pRec.setNumericAttrValue(fval, val, i);
											}
										}
										if (!attr.isNumeric()) {
											pRec.setAttrValue(val, i);
										}
									}
								}
								if (mObj == null || !id.equalsIgnoreCase(mObj.getIdentifier())) {
									np = 1;
									mObj = new DMovingObject();
									mObj.setIdentifier(id);
									mObjects.addElement(mObj);
									if (mObjects.size() % 100 == 0) {
										showMessage(mObjects.size() + " trajectories constructed", false);
									}
								}
								mObj.addPosition(new RealPoint(x, y), t, t, pRec);
							}
						}
					}
				}
				++k;
				if (k % 100 == 0) {
					//following text:" data lines read"
					showMessage(k + res.getString("data_lines_read"), false);
					//~MO
				}
			} catch (EOFException ioe) {
				if (mObjects.size() > 0) {
					showMessage(mObjects.size() + " trajectories constructed", false);
				} else {
					showMessage(res.getString("Unexpected_end_of") + spec.source + res.getString("_no_data_loaded"), true);
				}
			} catch (IOException ioe) {
				//following text:"Error reading data: "
				showMessage(res.getString("Error_reading_data_") + ioe, true);
				break;
			}
		}
		for (int i = mObjects.size() - 1; i >= 0; i--) {
			DMovingObject mobj = (DMovingObject) mObjects.elementAt(i);
			if (mobj.getTrack() == null || mobj.getTrack().size() < 2) {
				mObjects.removeElementAt(i);
			}
		}
		if (mObjects.size() < 1)
			return false;
		layer = new DGeoLayer();
		layer.setDataSource(spec);
		if (spec.name != null) {
			layer.setName(spec.name);
		} else if (spec.source != null) {
			layer.setName(CopyFile.getName(spec.source));
		}
		layer.setType(Geometry.line);
		layer.setGeoObjects(mObjects, true);
		layer.setHasMovingObjects(true);
		return true;
	}

	/**
	* Returns the map layer constructed from the coordinates contained in the
	* table (if any). If the table contains no coordinates, returns null.
	* If the table has not been loaded yet, the reader constructs a layer without
	* objects and sets itself as the  data supplier of the layer. Then the data
	* will be loaded when the layer is first drawn.
	*/
	public DGeoLayer getMapLayer() {
		if (layer != null)
			return layer;
		if (spec == null)
			return null;
		if (!describesTrajectories(spec)) {
			layer = super.getMapLayer();
			if (layer != null)
				return layer;
		}
		if (table != null && table.hasData())
			return null;
		if (loadData(false))
			return layer;
		return null;
	}

	protected static float getFloat(String str) {
		if (str == null)
			return Float.NaN;
		try {
			float f = Float.parseFloat(str);
			return f;
		} catch (NumberFormatException nfe) {
		}
		return Float.NaN;
	}

	protected boolean loadRegions(BufferedReader reader) {
		if (reader == null)
			return false;
		int k = 0;
		boolean start = true, openingTagFound = false;
		LayerData data = new LayerData();
		while (true) {
			try {
				String s = reader.readLine();
				if (s == null) {
					break;
				}
				s = s.trim();
				if (s.length() < 1) {
					continue;
				}
				if (start) { //at the beginning of the stream some header may
					//be present. The stream can also contain metadata.
					StringTokenizer st = new StringTokenizer(s, " \n\r");
					if (st.hasMoreTokens()) {
						String tok = st.nextToken().toLowerCase();
						if (tok.startsWith("http") || tok.startsWith("accept-") || tok.startsWith("server") || tok.startsWith("date:") || tok.startsWith("last-modified:") || tok.startsWith("etag:") || tok.startsWith("content-")
								|| tok.startsWith("connection")) {
							continue;
						}
					}
				}
				if (s.equalsIgnoreCase("<REGIONS>")) {
					start = false;
					openingTagFound = true;
					continue;
				}
				if (s.equalsIgnoreCase("</REGIONS>")) {
					break;
				}
				if (!openingTagFound) {
					continue;
				}
				s = s.toUpperCase();
				if (s.startsWith("REGION")) {
					StringTokenizer st = new StringTokenizer(s, " =,;\r\n");
					//if (st.countTokens()<7) continue;
					st.nextToken(); //the 1st token is the keyword REGION
					String id = st.nextToken();
					float coords[] = { Float.NaN, Float.NaN, Float.NaN, Float.NaN };
					int n = 0;
					boolean ok = true;
					while (st.hasMoreTokens() && ok && n < 4) {
						float val = Float.NaN;
						try {
							val = Float.parseFloat(st.nextToken());
						} catch (NumberFormatException e) {
						}
						coords[n++] = val;
						ok = ok && !Float.isNaN(val);
					}
					if (ok && n >= 4) {
						SpatialEntity spe = new SpatialEntity(id);
						RealRectangle rr = new RealRectangle(coords);
						spe.setGeometry(rr);
						data.addDataItem(spe);
					}
				}
				++k;
				if (k % 100 == 0) {
					showMessage(k + res.getString("data_lines_read"), false);
				}
			} catch (EOFException ioe) {
				if (data.getDataItemCount() > 0) {
					showMessage(data.getDataItemCount() + " regions constructed", false);
				} else {
					showMessage(res.getString("Unexpected_end_of") + spec.source + res.getString("_no_data_loaded"), true);
				}
			} catch (IOException ioe) {
				showMessage(res.getString("Error_reading_data_") + ioe, true);
				break;
			}
		}
		if (!openingTagFound) {
			showMessage("The opening tag <REGIONS> has not been found!", true);
			return false;
		}
		if (data.getDataItemCount() < 1)
			return false;
		layer = new DGeoLayer();
		layer.setDataSource(spec);
		if (spec.name != null) {
			layer.setName(spec.name);
		} else if (spec.source != null) {
			layer.setName(CopyFile.getName(spec.source));
		}
		layer.setType(Geometry.area);
		layer.receiveSpatialData(data);
		return true;
	}
}
