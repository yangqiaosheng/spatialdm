package export;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.SystemUI;
import spade.lib.lang.Language;
import spade.time.Date;
import spade.time.TimeMoment;
import spade.time.TimeRefDescription;
import spade.time.TimeReference;
import spade.vis.database.ObjectFilter;
import spade.vis.database.SpatialEntity;
import spade.vis.database.ThematicDataItem;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DMovingObject;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RealPoint;
import spade.vis.spec.DataSourceSpec;
import spade.vis.spec.ParamSpec;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 19-Apr-2007
 * Time: 10:41:29
 */
public class TrajectoriesToCSV implements LayerExporter, RecordNumberSaver {
	static ResourceBundle res = Language.getTextResource("export.Res");
	/**
	 * Descriptor of the file to which the  data were stored
	 */
	protected DataSourceSpec spec = null;
	/**
	 * Indicates whether the exporter saves record numbers.
	 * By default is true.
	 */
	protected boolean saveRecordNumbers = true;
	/**
	 * The name of the attribute (column) in which the record numbers are put
	 */
	protected String recNumColName = "trN";

	/**
	 * Indicates whether the exporter saves record numbers
	 */
	@Override
	public boolean getSaveRecordNumbers() {
		return saveRecordNumbers;
	}

	/**
	 * Requires the exporter to save or not to save record numbers
	 */
	@Override
	public void setSaveRecordNumbers(boolean toSave) {
		saveRecordNumbers = toSave;
	}

	/**
	 * Returns the name of the attribute (column) in which the record
	 * numbers are put
	 */
	@Override
	public String getRecNumColName() {
		return recNumColName;
	}

	/**
	 * Sets the name of the attribute (column) in which the record
	 * numbers will be put
	 */
	@Override
	public void setRecNumColName(String colName) {
		recNumColName = colName;
	}

	/**
	* Returns the name of the output format suitable for showing to the user.
	* For example, "Shape (ArcView)", "csv (comma-separated values)"
	*/
	@Override
	public String getFormatName() {
		return "csv (comma-separated values)";
	}

	/**
	* Returns a short text to explain the user what kind of layer may be saved
	* in this format. For example, "vector layer", "raster layer", "point objects"
	*/
	@Override
	public String getDataChar() {
		return "trajectories";
	}

	/**
	* Returns the usual file extension corresponding to this export format
	*/
	@Override
	public String getFileExtension() {
		return "csv";
	}

	/**
	* Returns true if this data exporter can store attributes from a table
	*/
	@Override
	public boolean canWriteAttributes() {
		return false;
	}

	/**
	* Returns true if the format requires creation of more than one files
	*/
	@Override
	public boolean needsMultipleFiles() {
		return false;
	}

	/**
	 * Replies if this exporter is applicable to a layer of the given type and,
	 * possibly, subtype.
	 */
	@Override
	public boolean isApplicable(char layerType, char subType) {
		return layerType == Geometry.line && subType == Geometry.movement;
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
		if (!(data instanceof DGeoLayer)) {
			// following string: "Illegal data type: DGeoLayer expected!"
			if (ui != null) {
				ui.showMessage(res.getString("Illegal_data_type1"), true);
			}
			return false;
		}
		DGeoLayer layer = (DGeoLayer) data;
		if (layer.getObjectCount() < 1) {
			// following string: "No data in the layer!"
			if (ui != null) {
				ui.showMessage(res.getString("No_data_in_the_layer_"), true);
			}
			return false;
		}
		DGeoObject obj = layer.getObject(0);
		if (!(obj instanceof DMovingObject)) {
			if (ui != null) {
				ui.showMessage("Illegal object type; DMovingObject required!", true);
			}
			return false;
		}
		DMovingObject mobj = (DMovingObject) obj;
		int k = 0;
		while ((mobj.getTrack() == null || mobj.getTrack().size() < 1) && k < layer.getObjectCount() - 1) {
			mobj = (DMovingObject) layer.getObject(++k);
		}
		if (mobj.getTrack() == null || mobj.getTrack().size() < 1) {
			if (ui != null) {
				ui.showMessage("No trajectories found!", true);
			}
			return false;
		}
		String attrStr = (saveRecordNumbers) ? "trID," + recNumColName + ",pIdx,X,Y,time" : "trID,pIdx,X,Y,time";
		int nAttr = 0;
		SpatialEntity spe = (SpatialEntity) mobj.getTrack().elementAt(0);
		ThematicDataItem themData = spe.getThematicData();
		if (themData != null) {
			for (int i = 0; i < themData.getAttrCount(); i++) {
				String aName = themData.getAttributeName(i);
				aName = aName.replace(',', ';');
				attrStr += "," + aName;
				++nAttr;
			}
		}
		DataOutputStream dos = new DataOutputStream(stream);
		int nrows = 0, nTr = 0;
		try {
			if (ui != null) {
				ui.showMessage(res.getString("Writing_data_"), false);
			}
			dos.writeBytes(attrStr + "\n");
			for (int i = 0; i < layer.getObjectCount(); i++)
				if (layer.isObjectActive(i)) {
					mobj = (DMovingObject) layer.getObject(i);
					Vector track = mobj.getTrack();
					if (track == null || track.size() < 1) {
						continue;
					}
					++nTr;
					String trId = mobj.getIdentifier();
					for (int j = 0; j < track.size(); j++) {
						spe = (SpatialEntity) track.elementAt(j);
						TimeReference tref = spe.getTimeReference();
						if (tref == null || tref.getValidFrom() == null) {
							continue;
						}
						Geometry geom = spe.getGeometry();
						if (geom == null) {
							continue;
						}
						RealPoint pt = null;
						if (geom instanceof RealPoint) {
							pt = (RealPoint) geom;
						} else {
							float bounds[] = geom.getBoundRect();
							if (bounds != null) {
								pt = new RealPoint((bounds[0] + bounds[2]) / 2, (bounds[1] + bounds[3]) / 2);
							}
						}
						if (pt == null) {
							continue;
						}
						String str = trId + ",";
						if (saveRecordNumbers) {
							str += nTr + ",";
						}
						str += (j + 1) + "," + pt.x + "," + pt.y + "," + tref.getValidFrom().toString();
						if (nAttr > 0) {
							themData = spe.getThematicData();
							if (themData == null) {
								for (int n = 0; n < nAttr; n++) {
									str += ",";
								}
							} else {
								for (int n = 0; n < nAttr; n++) {
									String sv = themData.getAttrValueAsString(n);
									if (sv == null) {
										sv = "";
									}
									str += "," + sv;
								}
							}
						}
						dos.writeBytes(str + "\n");
						++nrows;
						if (ui != null && nrows % 100 == 0) {
							ui.showMessage(nrows + res.getString("rows_stored") + " for " + nTr + " trajectories", false);
						}
					}
				}
		} catch (IOException ioe) {
			if (ui != null) {
				ui.showMessage(res.getString("Error_writing_to_the") + ioe.toString(), true);
			}
			return false;
		}
		if (ui != null)
			if (nrows > 0) {
				ui.showMessage(nrows + res.getString("rows_stored") + " for " + nTr + " trajectories", false);
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
		if (!(data instanceof DGeoLayer))
			return;
		DGeoLayer layer = (DGeoLayer) data;
		if (spec == null) {
			spec = new DataSourceSpec();
		}
		spec.source = source;
		spec.format = "ASCII";
		spec.delimiter = ",";
		spec.nRowWithFieldNames = 0;
		spec.idFieldName = "trID";
		spec.nameFieldName = null;
		spec.name = layer.getName();
		spec.toBuildMapLayer = true;
		spec.xCoordFieldName = "X";
		spec.yCoordFieldName = "Y";
		spec.objType = Geometry.line;
		spec.objSubType = Geometry.movement;
		spec.multipleRowsPerObject = true;
		TimeMoment t = null;
		for (int i = 0; i < layer.getObjectCount() && t == null; i++) {
			DMovingObject mobj = (DMovingObject) layer.getObject(i);
			t = mobj.getStartTime();
		}
		if (t != null) {
			TimeRefDescription trefD = new TimeRefDescription();
			trefD.attrName = "time";
			if (t instanceof Date) {
				trefD.attrScheme = ((Date) t).scheme;
			} else {
				trefD.attrScheme = "a";
			}
			trefD.sourceColumns = new String[1];
			trefD.sourceColumns[0] = "time";
			trefD.schemes = new String[1];
			trefD.schemes[0] = trefD.attrScheme;
			trefD.isParameter = false;
			trefD.meaning = TimeRefDescription.OCCURRED_AT;
			if (spec.descriptors == null) {
				spec.descriptors = new Vector(5, 5);
			}
			spec.descriptors.addElement(trefD);
		}
		DataSourceSpec dss = (DataSourceSpec) layer.getDataSource();
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
			layer.setDataSource(spec);
		}
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
