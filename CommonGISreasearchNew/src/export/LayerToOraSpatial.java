package export;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;

import oracle.sdoapi.OraSpatialManager;
import oracle.sdoapi.adapter.GeometryAdapter;
import oracle.sdoapi.geom.GeometryFactory;
import oracle.sdoapi.geom.InvalidGeometryException;
import oracle.sdoapi.geom.LineString;
import oracle.sql.STRUCT;
import spade.analysis.system.SystemUI;
import spade.lib.util.IntArray;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.ObjectFilter;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.MultiGeometry;
import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealPolyline;
import spade.vis.geometry.RealRectangle;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: Fraunhofer AIS.SPADE</p>
 * @author unascribed
 * @version 1.0
 */

public class LayerToOraSpatial extends TableToOracle implements LayerExporter {

	// For Oracle Spatial 8.1.6 database, change sdo_geom_metadata to user_sdo_geom_metadata
	DGeoLayer layer = null;
	private static String geomMetaDataTable = "user_sdo_geom_metadata";
	String tableName = "TEST";

	public LayerToOraSpatial() {
	}

	@Override
	public String getDataChar() {
		return "vector layer";
	}

	/**
	 * Replies if this exporter is applicable to a layer of the given type and,
	 * possibly, subtype.
	 */
	@Override
	public boolean isApplicable(char layerType, char subType) {
		return (layerType == Geometry.point || layerType == Geometry.line || layerType == Geometry.area) && subType == Geometry.undefined;
	}

	@Override
	public String getFormatName() {
		return "Oracle Spatial";
	}

	@Override
	public String getFileExtension() {
		return null;
	}

	@Override
	public boolean canWriteAttributes() {
		return true;
	}

	@Override
	public boolean needsMultipleFiles() {
		return false;
	}

	/*
	public boolean storeData(Object data, ObjectFilter filter, Vector selAttr, OutputStream stream, SystemUI ui) {
	  return true;
	}
	*/
	@Override
	public boolean storeData(Object data, ObjectFilter filter, Vector selAttr, String dir, String filename, SystemUI ui) {
		if (!(data instanceof DGeoLayer))
			return false;
		layer = (DGeoLayer) data;
		AttributeDataPortion dp = layer.getThematicData();
		if (dp == null) {
			if (ui != null) {
				ui.showMessage("Export without any tables is not yet implemented.", true);
			}
			return false;
		}

		this.ui = ui;
		System.out.println("Version:" + OraSpatialManager.getVersion());
		boolean res = super.storeData(dp, filter, selAttr, dir, filename, ui);
		System.out.println("res:" + res);
		return res;
	}

	/**
	  *  This Methods overrides methods in TableToJDBC (Mark)
	  *
	  */
	@Override
	protected void createTable(String columns) throws SQLException {
		tableName = spec.source;
		Statement stat = null;
		String update = null;

		try {
			update = "CREATE TABLE " + tableName + " (" + columns + ", geom MDSYS.SDO_GEOMETRY)";
			stat = connection.createStatement();
			stat.executeUpdate(update);
			stat.close();
			System.out.println("table '" + tableName + "' is created");
		} catch (SQLException ex) {
			System.out.println("create table:" + ex.getMessage());
		}

		RealRectangle bounds = layer.getCurrentLayerBounds();

		try {
			// add reference to it to meta data table
			stat = connection.createStatement();
			update = "INSERT INTO " + geomMetaDataTable + " VALUES ('" + tableName + "', 'GEOM', MDSYS.SDO_DIM_ARRAY(" + "MDSYS.SDO_DIM_ELEMENT('X', " + bounds.rx1 + ", " + bounds.rx2 + ", 0.0000005), " + "MDSYS.SDO_DIM_ELEMENT('Y', " + bounds.ry1
					+ ", " + bounds.ry2 + ", 0.0000005)), " + ((layer.isGeographic()) ? "8307" : "1") + ")";

			stat.executeUpdate(update);
			stat.close();
		} catch (SQLException de) {
			System.out.println("insert into '" + geomMetaDataTable + "':" + de);
		}

	}

	@Override
	protected void populatePreparedStatement(AttributeDataPortion table, IntArray attrNumbers, int rowNum, int idxGeom) {
		super.populatePreparedStatement(table, attrNumbers, rowNum, idxGeom);
		try {
			GeometryAdapter sdoAdapter = OraSpatialManager.getGeometryAdapter("SDO", "8.1.6", null, STRUCT.class, null, connection);
			GeometryFactory gF = OraSpatialManager.getGeometryFactory();
			if (layer.isGeographic()) {
				gF.setSpatialReference(OraSpatialManager.getSpatialReferenceManager(connection).retrieve(8307));
			}
			oracle.sdoapi.geom.Geometry gm = null;
			int index = layer.getObjectIndex(table.getDataItemId(rowNum));
			DGeoObject dg = layer.getObject(index);
			Geometry gmm = dg.getGeometry();
			gm = convertGeom(gF, gmm);
			if (gm == null)
				return;
			STRUCT sdo = (STRUCT) sdoAdapter.exportGeometry(oracle.sql.STRUCT.class, gm);
			ps.setObject(idxGeom, sdo);
		} catch (Exception ex) {
			System.out.println("insert into '" + tableName + "':" + ex.getMessage());
		}
	}

	/*
	protected void insertIntoTable(String id,String columns,String values) throws SQLException
	{


	  try{
	    GeometryAdapter sdoAdapter = OraSpatialManager.getGeometryAdapter("SDO", "8.1.6",
	                                null, STRUCT.class, null,connection);
	    GeometryFactory gF = OraSpatialManager.getGeometryFactory();
	    gF.setSpatialReference(OraSpatialManager.getSpatialReferenceManager(connection).retrieve(8307));

	    String update = "INSERT INTO " + tableName + " (" +columns+ ", geom) VALUES(" +values+ ",?)";
	    //System.out.println(update);
	    PreparedStatement stat = connection.prepareStatement(update);


	    oracle.sdoapi.geom.Geometry gm = null;
	    int index = layer.getObjectIndex(id);
	    DGeoObject dg = layer.getObject(index);
	    Geometry gmm = dg.getGeometry();



	    gm = convertGeom(gF,gmm);


	    if(gm == null) return;

	    STRUCT sdo = (STRUCT)sdoAdapter.exportGeometry(oracle.sql.STRUCT.class, gm);
	    stat.setObject(1, sdo);
	    stat.executeUpdate();


	    stat.close();
	    //System.out.println("id:"+id);

	  }catch(Exception ex){
	    //ex.printStackTrace();t = true;
	    System.out.println("insert into '"+tableName+"':"+ex.getMessage());
	  }




	}
	*/

	protected oracle.sdoapi.geom.Geometry convertGeom(GeometryFactory gF, Geometry gmm) {
		oracle.sdoapi.geom.Geometry geom = null;
		char gmty = gmm.getType();

		//System.out.println("["+gmty+"] class:" +gmm.getClass().getName());

		try {

			if (layer.isGeographic()) {
				gF.setSpatialReference(OraSpatialManager.getSpatialReferenceManager(connection).retrieve(8307));
			}
			if (gmm instanceof RealPolyline) {

				RealPoint rp[] = ((RealPolyline) gmm).p;
				double outerOrdArray[] = new double[rp.length * 2];
				for (int i = 0; i < rp.length; i++) {
					outerOrdArray[i * 2] = rp[i].x;
					outerOrdArray[i * 2 + 1] = rp[i].y;
				}

				oracle.sdoapi.geom.Geometry outerLineString = gF.createLineString(outerOrdArray);
				if (gmty == 'A') {
					geom = gF.createPolygon((LineString) outerLineString, null);
				} else if (gmty == 'L') {
					geom = outerLineString;
				}

			} else if (gmm instanceof MultiGeometry) {

				int n = ((MultiGeometry) gmm).getPartsCount();

				oracle.sdoapi.geom.Geometry collection[] = new oracle.sdoapi.geom.Geometry[n];
				for (int i = 0; i < n; i++) {
					Geometry gm = ((MultiGeometry) gmm).getPart(i);
					collection[i] = convertGeom(gF, gm);
				}
				geom = gF.createGeometryCollection(collection);
			} else if (gmm instanceof spade.vis.geometry.RealPoint && gmty == 'P') {
				geom = gF.createPoint(((RealPoint) gmm).x, ((RealPoint) gmm).y);
			}

		} catch (InvalidGeometryException ex) {
			System.out.println(ex.getClass().getName());
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return geom;
	}
}