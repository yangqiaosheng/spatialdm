package export;

import java.io.OutputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;

import spade.analysis.system.SystemUI;
import spade.lib.util.StringUtil;
import spade.time.TimeCount;
import spade.time.TimeReference;
import spade.vis.database.ObjectFilter;
import spade.vis.database.SpatialEntity;
import spade.vis.database.ThematicDataItem;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DMovingObject;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RealPoint;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: Jul 28, 2008
 * Time: 3:07:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class TrajectoriesToOracle extends TableToOracle implements LayerExporter {

	/**
	* Returns a short text to explain the user what kind of layer may be saved
	* in this format. For example, "vector layer", "raster layer", "point objects"
	*/
	@Override
	public String getDataChar() {
		return "trajectories";
	}

	/**
	* Returns true if this data exporter can store attributes from a table
	*/
	@Override
	public boolean canWriteAttributes() {
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
		this.ui = ui;
		if (data == null)
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

		//connecting to database
		if (!openConnection())
			return false;
		boolean goodName = false;
		while (!goodName) {
			try {
				Statement stat = connection.createStatement();
				stat.setMaxRows(1);
				ResultSet result = stat.executeQuery("SELECT * FROM " + spec.source);
				result.close();
				stat.close();
				spec.source = StringUtil.modifyId(spec.source, getMaxTableNameLength());
			} catch (SQLException se) {
				goodName = true;
			}
		}

		// generate table
		boolean bTimeIsAbstract = false;
		Vector track = (mobj == null) ? null : mobj.getTrack();
		SpatialEntity spe = (track == null) ? null : (SpatialEntity) track.elementAt(0);
		TimeReference tref = (spe == null) ? null : spe.getTimeReference();
		bTimeIsAbstract = tref == null || tref.getValidFrom() instanceof TimeCount;
		String sStructure = "TRAJECTORY_ID CHAR(32), ENTITY_ID CHAR(32), X FLOAT(32), Y FLOAT(32), DT " + ((bTimeIsAbstract) ? "NUMBER(32)" : "DATE"), columns = "TRAJECTORY_ID, ENTITY_ID, X, Y, DT", values = "?,?,?,?,?";
		ThematicDataItem themData = spe.getThematicData();
		if (themData != null) {
			for (int j = 0; j < themData.getAttrCount(); j++) {
				String aName = themData.getAttributeName(j);
				aName = aName.replace(',', '_');
				sStructure += ", " + aName + " ";
				columns += ", " + aName;
				values += ",?";
				switch (themData.getAttrType(j)) {
				case 'I':
					sStructure += "INTEGER(32)";
					break;
				case 'R':
					sStructure += "FLOAT(32)";
					break;
				case 'T':
					sStructure += "DATE";
					break;
				case 'C':
				default:
					sStructure += "CHAR(32)";
					break;
				}
			}
		}

		long tStart = System.currentTimeMillis();
		int nrows = 0, nTr = 0;
		String sErrorMessage = "";

		try {
			if (ui != null) {
				ui.showMessage(res.getString("Writing_data_"), false);
			}

			sErrorMessage = res.getString("Cannot_create_a_table");
			createTable(sStructure);

			sErrorMessage = "Can not prepare SQL statement\n" + "INSERT INTO " + spec.source + " (" + columns + ") VALUES (" + values + ")";
			PreparedStatement ps = connection.prepareStatement("INSERT INTO " + spec.source + " (" + columns + ") VALUES (" + values + ")");

			for (int i = 0; i < layer.getObjectCount(); i++)
				if (layer.isObjectActive(i)) {
					mobj = (DMovingObject) layer.getObject(i);
					track = mobj.getTrack();
					if (track == null || track.size() < 1) {
						continue;
					}
					++nTr;
					String trId = mobj.getIdentifier();
					for (int j = 0; j < track.size(); j++) {
						spe = (SpatialEntity) track.elementAt(j);
						tref = spe.getTimeReference();
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
						ps.setString(1, trId);
						ps.setString(2, mobj.getEntityId());
						ps.setFloat(3, pt.getX());
						ps.setFloat(4, pt.getY());
						if (tref.getValidFrom() instanceof spade.time.Date) {
							ps.setTimestamp(5, getTimestamp((spade.time.Date) tref.getValidFrom()));
						} else {
							ps.setLong(5, ((TimeCount) tref.getValidFrom()).toNumber());
						}
						themData = spe.getThematicData();
						if (themData != null) {
							for (int a = 0; a < themData.getAttrCount(); a++) {
								String sv = themData.getAttrValueAsString(a);
								ps.setString(6 + a, sv);
							}
						}

						sErrorMessage = "Error by storing data: trId=" + trId;
						ps.executeUpdate();

						++nrows;
						if (ui != null && nrows % 100 == 0) {
							ui.showMessage(nrows + res.getString("rows_stored") + " for " + nTr + " trajectories", false);
						}
					}
				}
		} catch (SQLException se) {
			if (ui != null) {
				ui.showMessage(sErrorMessage + " " + se.toString(), true);
			}
			System.out.println("ERROR: " + sErrorMessage + " " + se.toString());
			return false;
		}
		long tEnd = System.currentTimeMillis() - tStart;
		System.out.println("* Ready. Elapsed time " + StringUtil.floatToStr(tEnd / 1000f, 3) + " (s)");
		if (ui != null)
			if (nrows > 0) {
				ui.showMessage(nrows + res.getString("rows_stored") + " for " + nTr + " trajectories", false);
			} else {
				ui.showMessage(res.getString("No_records_actually"), true);
			}
		return nrows > 0;
	}
}
