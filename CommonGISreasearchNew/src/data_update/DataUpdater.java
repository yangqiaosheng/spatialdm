package data_update;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.lib.lang.Language;
import spade.lib.util.IdMaker;
import spade.lib.util.IdUtil;
import spade.lib.util.IdentifierUseChecker;
import spade.lib.util.StringUtil;
import spade.vis.database.Attribute;
import spade.vis.database.DataRecord;
import spade.vis.database.SpatialEntity;
import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealPolyline;

/**
* Stores new geographical objects (possibly, with thematic data) created by the
* user. For this purpose adds new records to a table file (in CSV format) and,
* possibly, new geometries to a layer file (OVL format, used for line or area
* objects). If the table file already existed, and a record contains attributes
* that are not present in the table, the data are stored in an additional file.
*/
public class DataUpdater implements IdentifierUseChecker {
	static ResourceBundle res = Language.getTextResource("data_update.Res");
	/**
	* Used for writing object identifiers to an OVL file
	*/
	private static byte b12[] = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
	/**
	* The error message
	*/
	protected String err = null;
	/**
	* Identifiers of the objects previously existing in the table. The DataUpdater
	* tries to avoid identifier duplication in the table.
	*/
	protected Vector objIds = null;

	/**
	* Adds the given SpatialEntities (that may refer to corresponding DataRecords)
	* to the specified table file (argument tableFileName) and, in the case of
	* lines or areas, to the specified geometry file (argument geoFileName).
	* The entities are contained in the argument data.
	* Some of the data items may be simply DataRecords (thematic data) rather than
	* SpatialEntities. These items are used to update existing records in the
	* table (the correspondence is determined through identifiers).
	* The argument delimiter (must be a string of 1 symbol length) specifies the
	* delimiter to be used in the table file.
	* Returns true if successfully stored. Otherwise the method getErrorMessage()
	* will return the error message.
	*/
	public boolean storeSpatialData(Vector data, String tableFileName, String delimiter, String geoFileName) {
		err = null;
		if (data == null || data.size() < 1)
			return true;
		if (tableFileName == null) {
			//following text: "No file for the thematic data specified!"
			err = res.getString("No_file_for_the");
			return false;
		}
		Vector updates = new Vector(data.size(), 1);
		for (int i = data.size() - 1; i >= 0; i--)
			if (data.elementAt(i) == null) {
				data.removeElementAt(i);
			} else if (data.elementAt(i) instanceof SpatialEntity) {
				;
			} else {
				if (data.elementAt(i) instanceof DataRecord) {
					updates.addElement(data.elementAt(i));
				}
				data.removeElementAt(i);
			}
		if (data.size() < 1 && updates.size() < 1) {
			//following text:"No data of appropriate type (SpatialEntity or DataRecord) found!"
			err = res.getString("No_data_of");
			return false;
		}
		Vector attrs = null; //attributes previously existing in the table
		if (objIds == null) {
			objIds = new Vector(50, 50);
		} else {
			objIds.removeAllElements();
		}
		//try to open the table file and read attribute names and object identifiers
		FileReader fr = null;
		boolean tableExists = false;
		try {
			fr = new FileReader(tableFileName);
		} catch (FileNotFoundException e) {
		}
		if (fr != null) {
			tableExists = true;
			DataOutputStream dout = null;
			boolean changed = false;
			if (updates.size() > 0) {
				FileOutputStream fout = null;
				try {
					fout = new FileOutputStream(tableFileName + ".new", false);
				} catch (IOException ioe) {
				}
				if (fout != null) {
					dout = new DataOutputStream(fout);
				}
			}
			BufferedReader reader = null;
			reader = new BufferedReader(fr);
			int idIdx = -1;
			while (true) {
				try {
					String str = reader.readLine();
					if (str == null) {
						break;
					}
					str = str.trim();
					if (str.length() > 0)
						if (attrs == null) {
							attrs = StringUtil.getNames(str, delimiter, true);
							idIdx = StringUtil.indexOfStringInVectorIgnoreCase("identifier", attrs);
							if (idIdx < 0) {
								idIdx = StringUtil.indexOfStringInVectorIgnoreCase("ident", attrs);
							}
							if (idIdx < 0) {
								idIdx = StringUtil.indexOfStringInVectorIgnoreCase("id", attrs);
							}
							if (idIdx < 0) {
								break;
							}
							if (dout != null) {
								dout.writeBytes(str + "\n");
							}
						} else {
							if (idIdx < 0) {
								break;
							}
							Vector v = StringUtil.getNames(str, delimiter, true);
							if (v != null && idIdx < v.size()) {
								String id = (String) v.elementAt(idIdx);
								if (id != null && id.length() > 0) {
									objIds.addElement(id);
									//probably, there is an update for this record
									if (dout != null) {
										for (int i = 0; i < updates.size(); i++)
											if (id.equalsIgnoreCase(((DataRecord) updates.elementAt(i)).getId())) {
												DataRecord rec = (DataRecord) updates.elementAt(i);
												for (int j = 0; j < attrs.size(); j++) {
													int aidx = rec.getAttrIndex((String) attrs.elementAt(j));
													if (aidx >= 0) {
														String oldVal = (String) v.elementAt(j), newVal = rec.getAttrValueAsString(aidx);
														if (StringUtil.sameStrings(oldVal, newVal)) {
															continue;
														}
														if (oldVal == null || oldVal.length() < 1) {
															v.setElementAt(newVal, j);
															changed = true;
														} else {
															Vector vo = StringUtil.getNames(oldVal, ";", false), vn = StringUtil.getNames(newVal, ";", false);
															for (int k = 0; k < vn.size(); k++)
																if (!StringUtil.isStringInVectorIgnoreCase((String) vn.elementAt(k), vo)) {
																	oldVal += ";" + (String) vn.elementAt(k);
																	changed = true;
																}
															v.setElementAt(oldVal, j);
														}
													}
												}
											}
										for (int i = 0; i < v.size(); i++) {
											if (i > 0) {
												dout.writeBytes(delimiter);
											}
											dout.writeBytes((String) v.elementAt(i));
										}
										dout.writeBytes("\n");
									}
								}
							}
						}
				} catch (EOFException eof) {
					break;
				} catch (IOException ioe) {
					err = res.getString("Error_reading") + tableFileName + ": " + ioe.toString();
					break;
				}
			}
			try {
				fr.close();
			} catch (IOException ioe) {
			}
			if (dout != null) {
				try {
					dout.close();
				} catch (IOException ioe) {
				}
				File file = new File(tableFileName + ".new");
				if (file.exists())
					if (!changed) {
						file.delete(); //no changes - erase the new file
					} else { //erase the old file
						File oldFile = new File(tableFileName);
						if (oldFile.delete()) {
							file.renameTo(oldFile);
						}
					}
			}
			if (err != null)
				return false;
		}
		if (data.size() < 1)
			return true;
		boolean newTable = attrs == null || attrs.size() < 0;
		if (newTable) { //compose the list of attributes from
			//the records to be stored
			if (attrs == null) {
				attrs = new Vector(20, 10);
			}
			attrs.addElement("identifier");
			attrs.addElement("name");
			boolean onlyPoints = true;
			for (int i = 0; i < data.size() && onlyPoints; i++) {
				if (data.elementAt(i) == null || !(data.elementAt(i) instanceof SpatialEntity)) {
					continue;
				}
				SpatialEntity spe = (SpatialEntity) data.elementAt(i);
				if (spe.getGeometry() != null) {
					onlyPoints = (spe.getGeometry() instanceof RealPoint);
				}
			}
			if (onlyPoints) {
				attrs.addElement("X");
				attrs.addElement("Y");
			}
			for (int i = 0; i < data.size(); i++) {
				if (data.elementAt(i) == null || !(data.elementAt(i) instanceof SpatialEntity)) {
					continue;
				}
				SpatialEntity spe = (SpatialEntity) data.elementAt(i);
				if (spe.getThematicData() == null || !(spe.getThematicData() instanceof DataRecord)) {
					continue;
				}
				DataRecord rec = (DataRecord) spe.getThematicData();
				for (int j = 0; j < rec.getAttrCount(); j++) {
					Attribute a = rec.getAttribute(j);
					String aId = IdUtil.getPureAttrId(a.getIdentifier());
					if (!StringUtil.isStringInVectorIgnoreCase(aId, attrs) && !StringUtil.isStringInVectorIgnoreCase(a.getName(), attrs)) {
						attrs.addElement(aId);
					}
				}
			}
		}
		FileOutputStream fout = null;
		try {
			fout = new FileOutputStream(tableFileName, tableExists);
		} catch (IOException ioe) {
			err = res.getString("Error_opening_the") + tableFileName + res.getString("for_writing_") + ioe.toString();
			return false;
		}
		DataOutputStream dout = new DataOutputStream(fout);
		if (newTable) { //write the string with attribute names
			try {
				for (int i = 0; i < attrs.size(); i++) {
					if (i > 0) {
						dout.writeBytes(delimiter);
					}
					String attrName = (String) attrs.elementAt(i);
					if (attrName.indexOf(delimiter) < 0) {
						dout.writeBytes(attrName);
					} else {
						dout.writeBytes("\"" + attrName + "\"");
					}
				}
				dout.write('\n');
				dout.flush();
			} catch (IOException ioe) {
				err = res.getString("Error_writing_to_the") + tableFileName + ": " + ioe.toString();
				try {
					fout.close();
				} catch (IOException e) {
				}
				return false;
			}
		}
		boolean hasNewAttr[] = null;
		if (!newTable) {
			hasNewAttr = new boolean[data.size()];
		}
		for (int i = 0; i < data.size(); i++) {
			if (hasNewAttr != null) {
				hasNewAttr[i] = false;
			}
			if (data.elementAt(i) == null || !(data.elementAt(i) instanceof SpatialEntity)) {
				continue;
			}
			SpatialEntity spe = (SpatialEntity) data.elementAt(i);
			DataRecord rec = null;
			if (spe.getThematicData() != null && (spe.getThematicData() instanceof DataRecord)) {
				rec = (DataRecord) spe.getThematicData();
			}
			RealPoint coord = null;
			if (spe.getGeometry() != null && (spe.getGeometry() instanceof RealPoint)) {
				coord = (RealPoint) spe.getGeometry();
			}
			if (rec == null && coord == null) {
				continue; //nothing to store in the table
			}
			//object identifier; may be changed to avoid duplicates
			String id = IdMaker.makeId(rec.getId(), this);
			//if the identifier was duplicate, it is replaced by a new identifier
			if (!id.equals(rec.getId())) {
				rec.setId(id);
				spe.setId(id);
			}
			objIds.addElement(id);
			String txt = "";
			//compose the string to add to the table
			for (int k = 0; k < attrs.size(); k++) {
				String attrName = (String) attrs.elementAt(k);
				String value = null;
				if (attrName.equalsIgnoreCase("identifier") || attrName.equalsIgnoreCase("ident") || attrName.equalsIgnoreCase("id")) {
					value = id;
				} else if (attrName.equalsIgnoreCase("name")) {
					value = rec.getName();
				} else if ((attrName.equalsIgnoreCase("X") || attrName.equalsIgnoreCase("Y")) && coord != null) {
					value = String.valueOf((attrName.equalsIgnoreCase("X")) ? coord.x : coord.y);
				} else {
					for (int j = 0; j < rec.getAttrCount() && value == null; j++) {
						Attribute a = rec.getAttribute(j);
						String aId = IdUtil.getPureAttrId(a.getIdentifier());
						if (attrName.equalsIgnoreCase(aId) || attrName.equalsIgnoreCase(a.getName())) {
							value = rec.getAttrValueAsString(j);
							break;
						}
					}
				}
				if (value == null) {
					value = "";
				}
				if (value.indexOf(delimiter) >= 0) {
					value = "\"" + value + "\"";
				}
				if (k > 0) {
					value = delimiter + value;
				}
				txt += value;
			}
			try {
				dout.writeBytes(txt + "\n");
			} catch (IOException ioe) {
				err = res.getString("Error_writing_to_the") + tableFileName + ": " + ioe.toString();
				break;
			}
			//check if all attributes of the record were written
			if (!newTable && hasNewAttr != null) {
				for (int j = 0; j < rec.getAttrCount() && !hasNewAttr[i]; j++) {
					Attribute a = rec.getAttribute(j);
					String aId = IdUtil.getPureAttrId(a.getIdentifier());
					if (!StringUtil.isStringInVectorIgnoreCase(aId, attrs) && !StringUtil.isStringInVectorIgnoreCase(a.getName(), attrs)) {
						hasNewAttr[i] = true;
					}
				}
			}
		}
		try {
			fout.close();
		} catch (IOException ioe) {
		}
		if (hasNewAttr != null) {
			//write records with additional attributes to a special "update" file
			boolean newAttr = false;
			for (int i = 0; i < hasNewAttr.length && !newAttr; i++) {
				newAttr = hasNewAttr[i];
			}
			if (newAttr) {
				fout = null;
				try {
					fout = new FileOutputStream(tableFileName + ".update", true);
				} catch (IOException ioe) {
					err = res.getString("Error_opening_the") + tableFileName + res.getString("_update_for_writing_") + ioe.toString();
				}
				if (fout != null) {
					dout = new DataOutputStream(fout);
					for (int i = 0; i < hasNewAttr.length; i++)
						if (hasNewAttr[i]) {
							SpatialEntity spe = (SpatialEntity) data.elementAt(i);
							DataRecord rec = (DataRecord) spe.getThematicData();
							String attrStr = "identifier" + delimiter + "name", valStr = rec.getId() + delimiter;
							if (rec.getName() != null) {
								valStr += rec.getName();
							}
							for (int j = 0; j < rec.getAttrCount(); j++) {
								Attribute a = rec.getAttribute(j);
								String name = a.getName();
								if (name == null) {
									name = IdUtil.getPureAttrId(a.getIdentifier());
								}
								if (name.indexOf(delimiter) >= 0) {
									name = "\"" + name + "\"";
								}
								attrStr += delimiter + name;
								String value = rec.getAttrValueAsString(j);
								if (value == null) {
									value = "";
								}
								if (value.indexOf(delimiter) >= 0) {
									value = "\"" + value + "\"";
								}
								valStr += delimiter + value;
							}
							try {
								dout.writeBytes(attrStr + "\n");
								dout.writeBytes(valStr + "\n\n");
							} catch (IOException ioe) {
								err = res.getString("Error_writing_to_the") + tableFileName + ".update : " + ioe.toString();
								break;
							}
						}
					try {
						fout.close();
					} catch (IOException ioe) {
					}
					if (err != null)
						return false;
				}
			}
		}
		if (geoFileName == null)
			return err == null;
		//store the geometries in an OVL file
		float x1 = Float.NaN, x2 = x1, y1 = x1, y2 = x1;
		//check if the file existed before
		FileInputStream fin = null;
		boolean geoFileExists = false, geoError = false;
		try {
			fin = new FileInputStream(geoFileName);
		} catch (FileNotFoundException e) {
		}
		if (fin != null) {
			geoFileExists = true;
			//read the header with the layer extent
			DataInputStream din = new DataInputStream(fin);
			try {
				x1 = din.readFloat();
				y1 = din.readFloat();
				x2 = din.readFloat();
				y2 = din.readFloat();
			} catch (IOException ioe) {
				//following text:"Error reading the header of the file "
				err = res.getString("Error_reading_the") + geoFileName + ": " + ioe.toString();
				geoError = true;
			}
			try {
				fin.close();
			} catch (IOException ioe) {
			}
			if (geoError)
				return false;
		}
		fout = null;
		try {
			fout = new FileOutputStream(geoFileName, geoFileExists);
		} catch (IOException ioe) {
			err = res.getString("Error_opening_the") + geoFileName + res.getString("for_writing_") + ioe.toString();
			return false;
		}
		dout = new DataOutputStream(fout);
		if (!geoFileExists) {
			try {
				for (int i = 0; i < 4; i++) {
					dout.writeFloat(0.0f);
				}
			} catch (IOException ioe) {
				err = res.getString("Error_writing_to_the") + geoFileName + ": " + ioe.toString();
				try {
					fout.close();
				} catch (IOException e) {
				}
				return false;
			}
		}
		for (int i = 0; i < data.size(); i++) {
			if (data.elementAt(i) == null || !(data.elementAt(i) instanceof SpatialEntity)) {
				continue;
			}
			SpatialEntity spe = (SpatialEntity) data.elementAt(i);
			if (spe.getGeometry() == null || (!(spe.getGeometry() instanceof RealPoint) && !(spe.getGeometry() instanceof RealPolyline))) {
				continue;
			}
			RealPoint point = null;
			RealPolyline line = null;
			int npoints = 1;
			if (spe.getGeometry() instanceof RealPoint) {
				point = (RealPoint) spe.getGeometry();
			} else {
				line = (RealPolyline) spe.getGeometry();
				npoints = line.p.length;
			}
			String id = spe.getId();
			try {
				dout.writeFloat(npoints);
				//writing the identifier
				if (id == null) {
					dout.write(b12, 0, 12);
				} else {
					id = id.trim();
					if (id.length() > 12) {
						id = id.substring(0, 12).trim();
					}
					if (id.length() < 1) {
						dout.write(b12, 0, 12);
					} else {
						dout.writeBytes(id);
						if (id.length() < 12) {
							dout.write(b12, 0, 12 - id.length());
						}
					}
				}
				if (point != null) {
					dout.writeFloat(point.x);
					dout.writeFloat(point.y);
					if (Float.isNaN(x1) || point.x < x1) {
						x1 = point.x;
					}
					if (Float.isNaN(x2) || point.x > x2) {
						x2 = point.x;
					}
					if (Float.isNaN(y1) || point.y < y1) {
						y1 = point.y;
					}
					if (Float.isNaN(y2) || point.y > y2) {
						y2 = point.y;
					}
				} else {
					for (RealPoint element : line.p) {
						dout.writeFloat(element.x);
						dout.writeFloat(element.y);
						if (Float.isNaN(x1) || element.x < x1) {
							x1 = element.x;
						}
						if (Float.isNaN(x2) || element.x > x2) {
							x2 = element.x;
						}
						if (Float.isNaN(y1) || element.y < y1) {
							y1 = element.y;
						}
						if (Float.isNaN(y2) || element.y > y2) {
							y2 = element.y;
						}
					}
				}
			} catch (IOException ioe) {
				err = res.getString("Error_writing_to_the") + geoFileName + ": " + ioe.toString();
				geoError = true;
				break;
			}
		}
		try {
			fout.close();
		} catch (IOException ioe) {
		}
		if (geoError)
			return false;
		//rewrite the header
		RandomAccessFile raf = null;
		try {
			raf = new RandomAccessFile(geoFileName, "rw");
		} catch (IOException ioe) {
			//following text:"Error opening the file "
			//following text:" for rewriting the header: "
			err = res.getString("Error_opening_the") + geoFileName + res.getString("for_rewriting_the") + ioe.toString();
			return false;
		}
		try {
			raf.seek(0); //beginning of the file
			raf.writeFloat(x1);
			raf.writeFloat(y1);
			raf.writeFloat(x2);
			raf.writeFloat(y2);
		} catch (IOException ioe) {
			//following text:"Error rewriting the header of the file "
			err = res.getString("Error_rewriting_the") + geoFileName + ": " + ioe.toString();
			geoError = true;
		}
		try {
			raf.close();
		} catch (IOException ioe) {
		}
		return err == null;
	}

	/**
	* Checks if the given identifier was already used in the table (to avoid
	* identifier duplication when the table is updated)
	*/
	@Override
	public boolean isIdentifierUsed(String ident) {
		if (objIds == null || ident == null)
			return false;
		return StringUtil.isStringInVectorIgnoreCase(ident, objIds);
	}

	/**
	* If an error occurred in the process of updating data, returns the error
	* message.
	*/
	public String getErrorMessage() {
		return err;
	}
}