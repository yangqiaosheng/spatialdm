package export;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Vector;

import spade.analysis.system.SystemUI;
import spade.time.Date;
import spade.time.TimeMoment;
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

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: 27-Aug-2007
 * Time: 15:03:29
 * Special export method for producing files suitable for import into the
 * moving object database.
 */
public class TrajectoriesToCSV_Extended extends TrajectoriesToCSV {
	/**
	* Returns the name of the output format suitable for showing to the user.
	* For example, "Shape (ArcView)", "csv (comma-separated values)"
	*/
	@Override
	public String getFormatName() {
		return "semicolon-separated values";
	}

	/**
	* Returns a short text to explain the user what kind of layer may be saved
	* in this format. For example, "vector layer", "raster layer", "point objects"
	*/
	@Override
	public String getDataChar() {
		return "trajectories for MOD";
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
		String attrStr = "N;E_ID;point_N;T_ID;year;month;day;hour;min;sec;lat;lon";
		int nAttr = 0;
		SpatialEntity spe = (SpatialEntity) mobj.getTrack().elementAt(0);
		ThematicDataItem themData = spe.getThematicData();
		if (themData != null) {
			for (int i = 0; i < themData.getAttrCount(); i++) {
				String aName = themData.getAttributeName(i);
				aName = aName.replace(';', ',');
				attrStr += ";" + aName;
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
					String eId = mobj.getEntityId();
					if (eId == null) {
						eId = "";
					}
					for (int j = 0; j < track.size(); j++) {
						spe = (SpatialEntity) track.elementAt(j);
						TimeReference tref = spe.getTimeReference();
						if (tref == null || tref.getValidFrom() == null || !(tref.getValidFrom() instanceof Date)) {
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
						Date d = (Date) tref.getValidFrom();
						++nrows;
						String str = nrows + ";" + eId + ";" + (j + 1) + ";" + nTr + ";" + d.getElementValue('y') + ";" + d.getElementValue('m') + ";" + d.getElementValue('d') + ";" + d.getElementValue('h') + ";" + d.getElementValue('t') + ";"
								+ d.getElementValue('s') + ";" + pt.y + ";" + pt.x;
						if (nAttr > 0) {
							themData = spe.getThematicData();
							if (themData == null) {
								for (int n = 0; n < nAttr; n++) {
									str += ";";
								}
							} else {
								for (int n = 0; n < nAttr; n++) {
									String sv = themData.getAttrValueAsString(n);
									if (sv == null) {
										sv = "";
									}
									str += ";" + sv;
								}
							}
						}
						dos.writeBytes(str + "\n");
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

	@Override
	protected void makeSourceSpecification(Object data, String source) {
		if (!(data instanceof DGeoLayer))
			return;
		DGeoLayer layer = (DGeoLayer) data;
		DataSourceSpec spec = null;
		if (layer.getDataSource() != null && (layer.getDataSource() instanceof DataSourceSpec)) {
			spec = (DataSourceSpec) layer.getDataSource();
		}
		if (spec == null || spec.source == null || spec.source.equalsIgnoreCase("_derived")) {
			if (spec == null) {
				spec = new DataSourceSpec();
				layer.setDataSource(spec);
			}
			spec.source = source;
			spec.format = "ASCII";
			spec.delimiter = ",";
			spec.nRowWithFieldNames = 0;
			spec.idFieldName = "T_ID";
			spec.nameFieldName = null;
			spec.name = layer.getName();
			spec.toBuildMapLayer = true;
			spec.xCoordFieldName = "lon";
			spec.yCoordFieldName = "lat";
			spec.objType = Geometry.line;
			spec.objSubType = Geometry.movement;
			spec.multipleRowsPerObject = true;
			TimeMoment t = null;
			for (int i = 0; i < layer.getObjectCount() && t == null; i++) {
				DMovingObject mobj = (DMovingObject) layer.getObject(i);
				t = mobj.getStartTime();
			}
		}
	}
}
