package data_load.readers;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.Vector;

import spade.analysis.system.AttrDataReader;
import spade.analysis.system.GeoDataReader;
import spade.lib.lang.Language;
import spade.lib.util.CopyFile;
import spade.lib.util.FloatArray;
import spade.lib.util.StringUtil;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DVectorGridLayer;
import spade.vis.spec.DataSourceSpec;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 09-Oct-2006
 * Time: 10:21:52
 * Builds an instance of DVectorGridLayer (a regular rectangular grid with
 * thematic data attached to the cells) from two files:
 * 1) GRD file specifying the grid parameters, i.e. x-coordinates of the
 *    vertical lines and y-coordinates of the horizontal lines
 * 2) CSV file with the thematic data.
 */
public class GridASCIIReader extends DataStreamReader implements GeoDataReader, AttrDataReader {
	static ResourceBundle res = Language.getTextResource("data_load.readers.Res");
	/**
	 * The x-coordinates of the vertical grid lines
	 */
	protected FloatArray xVertCoord = null;
	/**
	 * The y-coordinates of the horizontal grid lines
	 */
	protected FloatArray yHorzCoord = null;
	/**
	* The attribute data loaded
	*/
	protected DataTable table = null;

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
		setDataReadingInProgress(true);
		if (spec == null || spec.source == null)
			if (mayAskUser) {
				//following text:"Select the file with geographical data"
				String path = browseForFile(res.getString("Select_the_file_with3"), "*.grd");
				if (path == null) {
					setDataReadingInProgress(false);
					return false;
				}
				if (spec == null) {
					spec = new DataSourceSpec();
				}
				spec.source = path;
			} else {
				//following text:"The data source for layer is not specified!"
				showMessage(res.getString("The_data_source_for"), true);
				setDataReadingInProgress(false);
				return false;
			}
		if (spec.name == null) {
			spec.name = CopyFile.getName(spec.source);
		}
		showMessage(res.getString("Start_reading_data") + spec.source, false);
		closeStream();
		openStream(); //to be at the beginning
		if (stream == null) {
			dataError = true;
			setDataReadingInProgress(false);
			return false;
		}
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		int what = 0; //1 - columns (vertical line coordinates), 2 - rows (horizontal)
		int nLine = 0;
		while (!dataError) {
			try {
				String s = reader.readLine();
				if (s == null) {
					break;
				}
				++nLine;
				s = s.trim();
				if (s.length() < 1) {
					continue;
				}
				if (s.startsWith("<") && s.endsWith(">")) {
					if (s.equalsIgnoreCase("<columns>")) {
						what = 1;
						xVertCoord = new FloatArray(100, 100);
					} else if (s.equalsIgnoreCase("<rows>")) {
						what = 2;
						yHorzCoord = new FloatArray(100, 100);
					} else if (s.equalsIgnoreCase("</columns>")) {
						if (what != 1) {
							showMessage("No opening tag for " + s, true);
							dataError = true;
						}
						what = 0;
					} else if (s.equalsIgnoreCase("</rows>")) {
						if (what != 2) {
							showMessage("No opening tag for " + s, true);
							dataError = true;
						}
						what = 0;
					} else {
						showMessage("Unexpected tag " + s + " in line " + nLine, true);
						dataError = true;
						what = 0;
					}
				} else if (what < 1) {
					continue;
				} else {
					//the line must contain one or more numbers
					StringTokenizer st = new StringTokenizer(s, " ,;\t");
					while (st.hasMoreTokens()) {
						String tok = st.nextToken().trim();
						if (tok.length() < 1) {
							continue;
						}
						try {
							float val = Float.valueOf(tok).floatValue();
							if (what == 1) {
								xVertCoord.addElementSorted(val);
							} else {
								yHorzCoord.addElementSorted(val);
							}
						} catch (NumberFormatException nfe) {
							showMessage("Unexpected token [" + s + "] in line " + nLine + "; must be a number!", true);
						}
					}
				}
			} catch (EOFException ioe) {
				if (xVertCoord == null || xVertCoord.size() < 2 || yHorzCoord == null || yHorzCoord.size() < 2) {
					showMessage(res.getString("Unexpected_end_of") + spec.source + res.getString("_no_data_loaded"), true);
				}
			} catch (IOException ioe) {
				showMessage(res.getString("Error_reading_data_") + ioe, true);
				dataError = true;
				break;
			}
		}
		closeStream();
		setDataReadingInProgress(false);
		if (dataError || xVertCoord == null || xVertCoord.size() < 2 || yHorzCoord == null || yHorzCoord.size() < 2)
			return false;
		showMessage("Grid: " + xVertCoord.size() + " vertical lines and " + yHorzCoord.size() + " horizontal lines", false);
		if (spec.objDescrSource == null) {
			//generate a default name of the file with thematic data about the objects
			String str = spec.source;
			if (spec.source.endsWith(".grd") || spec.source.endsWith(".GRD")) {
				str = spec.source.substring(0, spec.source.length() - 4);
			} else if (spec.source.endsWith(".grid") || spec.source.endsWith(".GRID")) {
				str = spec.source.substring(0, spec.source.length() - 5);
			}
			if (CopyFile.checkExistence(str + ".csv")) {
				spec.objDescrSource = str + ".csv";
				if (spec.nRowWithFieldNames < 0) {
					spec.nRowWithFieldNames = 0;
				}
			}
		}
		if (spec.objDescrSource == null)
			return false; //no file with object information exists
		if (spec.delimiter == null) {
			spec.delimiter = ",";
		}
		int max = (xVertCoord.size() > yHorzCoord.size()) ? xVertCoord.size() : yHorzCoord.size();
		int nDigits = 1;
		do {
			max /= 10;
			if (max > 0) {
				++nDigits;
			}
		} while (max > 0);

		showMessage(res.getString("Start_reading_data") + spec.objDescrSource, false);
		String sourcePath = spec.source;
		spec.source = spec.objDescrSource;
		setDataReadingInProgress(true);
		openStream();
		if (stream == null) {
			dataError = true;
			spec.source = sourcePath;
			setDataReadingInProgress(false);
			return false;
		}
		reader = new BufferedReader(new InputStreamReader(stream));
		int k = 0;
		Vector fieldNames = null, fieldTypes = null, values = null;
		int xfn = -1, yfn = -1;
		boolean start = true, attributesGot = false;
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
				if (k == spec.nRowWithFieldNames) {
					start = false;
					fieldNames = getStrings(s, fieldNames);
					showMessage(res.getString("Got") + fieldNames.size() + res.getString("attribute_names"), false);
				} else if (k == spec.nRowWithFieldTypes) {
					start = false;
					fieldTypes = getStrings(s, fieldTypes);
					showMessage(res.getString("Got") + fieldTypes.size() + res.getString("attribute_types"), false);
				} else { //this is a row with data values
					start = false;
					if (table == null) {
						table = new DataTable();
						table.setDataSource(spec.clone());
					}
					if (!attributesGot) {
						//construct the list of attributes
						if (fieldNames != null && fieldNames.size() > 0) {
							if (spec.idFieldN < 0) {
								if (spec.idFieldName != null) {
									spec.idFieldN = StringUtil.indexOfStringInVectorIgnoreCase(spec.idFieldName, fieldNames);
								} else {
									spec.idFieldN = StringUtil.indexOfStringInVectorIgnoreCase("id", fieldNames);
									if (spec.idFieldN >= 0) {
										spec.idFieldName = (String) fieldNames.elementAt(spec.idFieldN);
									}
								}
							}
							if (spec.nameFieldN < 0) {
								if (spec.nameFieldName != null) {
									spec.nameFieldN = StringUtil.indexOfStringInVectorIgnoreCase(spec.nameFieldName, fieldNames);
								} else {
									spec.nameFieldN = StringUtil.indexOfStringInVectorIgnoreCase("name", fieldNames);
									if (spec.nameFieldN >= 0) {
										spec.nameFieldName = (String) fieldNames.elementAt(spec.nameFieldN);
									}
								}
							}
							for (int i = 0; i < fieldNames.size(); i++)
								if (i != spec.idFieldN && i != spec.nameFieldN)
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
							if (spec.xCoordFieldName != null) {
								xfn = table.getAttrIndex(spec.xCoordFieldName);
							}
							if (xfn < 0) {
								xfn = table.getAttrIndex("X");
								if (xfn >= 0) {
									spec.xCoordFieldName = "X";
								}
							}
							if (spec.yCoordFieldName != null) {
								yfn = table.getAttrIndex(spec.yCoordFieldName);
							}
							if (yfn < 0) {
								yfn = table.getAttrIndex("Y");
								if (yfn >= 0) {
									spec.yCoordFieldName = "Y";
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
								if (i != spec.idFieldN && i != spec.nameFieldN)
									if (i != xfn && i != yfn) {
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
									} else {
										table.addAttribute((i == xfn) ? "X" : "Y", AttributeTypes.integer);
									}
						}
						attributesGot = true;
					}
					values = getStrings(s, values);
					if (values != null && values.size() > 0) {
						String id = null, name = null;
						if (spec.idFieldN >= 0) {
							id = (String) values.elementAt(spec.idFieldN);
						}
						if (spec.nameFieldN >= 0) {
							name = (String) values.elementAt(spec.nameFieldN);
						}
						if (id == null && xfn >= 0 && yfn >= 0) {
							int cx = -1, ry = -1;
							try {
								if (xfn < values.size() && values.elementAt(xfn) != null) {
									cx = Integer.valueOf((String) values.elementAt(xfn)).intValue();
								}
								if (yfn < values.size() && values.elementAt(yfn) != null) {
									ry = Integer.valueOf((String) values.elementAt(yfn)).intValue();
								}
							} catch (NumberFormatException nfe) {
							}
							if (cx > 0 && ry > 0) {
								id = StringUtil.padString(String.valueOf(ry), '0', nDigits, true) + "_" + StringUtil.padString(String.valueOf(cx), '0', nDigits, true);
							}
						}
						if (id != null) {
							DataRecord drec = new DataRecord(id, name);
							for (int i = 0; i < values.size(); i++)
								if (i != spec.idFieldN && i != spec.nameFieldN) {
									drec.addAttrValue(values.elementAt(i));
								}
							table.addDataRecord(drec);
						}
					}
				}
				++k;
				if (k % 50 == 0) {
					showMessage(k + res.getString("data_lines_read"), false);
				}
			} catch (EOFException ioe) {
				if (!attributesGot) {
					showMessage(res.getString("Unexpected_end_of") + spec.source + res.getString("_no_data_loaded"), true);
				}
			} catch (IOException ioe) {
				dataError = true;
				showMessage(res.getString("Error_reading_data_") + ioe, true);
				break;
			}
		}
		closeStream();
		setDataReadingInProgress(false);
		if (attributesGot) {
			showMessage(res.getString("Data_loaded_N_of_rows") + table.getDataItemCount() + res.getString("_N_of_fields_") + table.getAttrCount(), false);
		}
		spec.source = sourcePath;
		if (dataError || table == null)
			return false;
		if (attributesGot && (fieldTypes == null || fieldTypes.size() < table.getAttrCount())) {
			//following text:"Determining attribute types..."
			showMessage(res.getString("Determining_attribute"), false);
			table.determineAttributeTypes();
			//following text:"Finished determining attribute types in "
			showMessage(res.getString("Finished_determining") + spec.source, false);
		}
		if (table.hasData()) {
			table.finishedDataLoading();
		}
		return true;
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
		return strings;
	}

	/**
	* Returns the map layer constructed from the geographical data loaded (if any).
	* If the data have not been loaded yet, loads the data.
	*/
	@Override
	public DGeoLayer getMapLayer() {
		if (dataError)
			return null;
		if (xVertCoord == null || yHorzCoord == null)
			if (!loadData(false))
				return null;
		DVectorGridLayer layer = new DVectorGridLayer();
		layer.setDataSource(spec);
		if (spec.id != null) {
			layer.setContainerIdentifier(spec.id);
		}
		if (spec.name != null) {
			layer.setName(spec.name);
		} else if (spec.source != null) {
			layer.setName(CopyFile.getName(spec.source));
		}
		layer.constructObjects(xVertCoord.getTrimmedArray(), yHorzCoord.getTrimmedArray());
		if (table != null && table.hasData()) {
			int xfn = -1, yfn = -1;
			if (spec.xCoordFieldName != null) {
				xfn = table.getAttrIndex(spec.xCoordFieldName);
			}
			if (spec.yCoordFieldName != null) {
				yfn = table.getAttrIndex(spec.yCoordFieldName);
			}
			if (xfn >= 0 && yfn >= 0) {
				layer.attachTable(table, xfn, yfn);
			} else {
				layer.receiveThematicData(table);
			}
		}
		return layer;
	}

	/**
	* Returns the table with the attribute (thematic) data loaded (if any)
	*/
	@Override
	public DataTable getAttrData() {
		if (table == null) {
			loadData(false);
		}
		return table;
	}
}
