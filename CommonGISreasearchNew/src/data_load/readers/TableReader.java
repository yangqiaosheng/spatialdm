package data_load.readers;

import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.AttrDataReader;
import spade.analysis.system.GeoDataReader;
import spade.lib.lang.Language;
import spade.lib.util.CopyFile;
import spade.time.TimeMoment;
import spade.time.TimeRefDescription;
import spade.time.TimeReference;
import spade.vis.database.DataPortion;
import spade.vis.database.DataSupplier;
import spade.vis.database.DataTable;
import spade.vis.database.LayerData;
import spade.vis.database.SpatialEntity;
import spade.vis.database.TableContentSupplier;
import spade.vis.database.ThematicDataItem;
import spade.vis.dmap.DGeoLayer;
import spade.vis.geometry.RealCircle;
import spade.vis.geometry.RealPoint;

/**
* A class to be used as a basis for classes reading attribute data (tables) from
* streams in various formats. A table may also contain coordinates of point
* objects. In this case the reader produces, besides a table, also a map layer.
*/

public abstract class TableReader extends DataStreamReader implements AttrDataReader, GeoDataReader, TableContentSupplier, DataSupplier {
	static ResourceBundle res = Language.getTextResource("data_load.readers.Res");
	/**
	* The table loaded
	*/
	protected DataTable table = null;
	/**
	* The geo layer created, if the table contains coordinates
	*/
	protected DGeoLayer layer = null;
	/**
	* The spatial data for the layer (point entities)
	*/
	protected LayerData data = null;

	/**
	* Constructs an empty table. Sets its name, identifier, etc. on the basis of
	* the data source specification provided.
	*/
	protected void constructTable() {
		if (table != null)
			return;
		table = new DataTable();
		if (spec != null) {
			table.setDataSource(spec);
			if (spec.name != null) {
				table.setName(spec.name);
			} else {
				table.setName(CopyFile.getName(spec.source));
			}
		}
	}

	/**
	* Returns the table with the attribute (thematic) data loaded (if any)
	*/
	@Override
	public DataTable getAttrData() {
		if (table == null && data == null && (layer == null || !layer.hasData())) {
			constructTable();
			table.setTableContentSupplier(this);
		}
		return table;
	}

	/**
	* Returns the map layer constructed from the coordinates contained in the
	* table (if any). If the table contains no coordinates, returns null.
	* If the table has not been loaded yet, the reader constructs a layer without
	* objects and sets itself as the  data supplier of the layer. Then the data
	* will be loaded when the layer is first drawn.
	*/
	@Override
	public DGeoLayer getMapLayer() {
		if (layer != null)
			return layer;
		if (spec == null || spec.xCoordFieldName == null || spec.yCoordFieldName == null || spec.multipleRowsPerObject)
			return null;
		layer = new DGeoLayer();
		layer.setDataSource(spec);
		if (spec.name != null) {
			layer.setName(spec.name);
		} else if (spec.source != null) {
			layer.setName(CopyFile.getName(spec.source));
		}
		if (data != null) {
			layer.receiveSpatialData(data);
		} else {
			layer.setDataSupplier(this);
		}
		return layer;
	}

	/**
	* Loads the data. When allowed (the argument mayAskUser is true) and necessary,
	* starts a dialog or a wizard for the user to specify the data source,
	* constraints, etc. Returns true if the data have been successfully loaded.
	*/
	@Override
	abstract public boolean loadData(boolean mayAskUser);

	/**
	* If the table contains coordinates of geographical objects, this method
	* constructs the objects and links them to the corresponding thematic data.
	*/
	protected LayerData tryGetGeoObjects() {
		if (table == null)
			return null;
		if ((spec.xCoordFieldName != null && spec.yCoordFieldName != null) && !spec.multipleRowsPerObject) {
			//form a geo layer with point objects
			int xfn = table.findAttrByName(spec.xCoordFieldName), yfn = table.findAttrByName(spec.yCoordFieldName);
			if (xfn < 0 || yfn < 0) {
				showMessage(res.getString("Could_not_get_points") + spec.source + res.getString("_no_coord_"), true);
				return null;
			}
			int radiusFN = -1;
			if (spec.radiusFieldName != null) {
				radiusFN = table.findAttrByName(spec.radiusFieldName);
			}
			//check if the data contain time references
			int occurTimeFN = -1, validFromFN = -1, validUntilFN = -1;
			if (spec.descriptors != null) {
				for (int i = 0; i < spec.descriptors.size(); i++)
					if (spec.descriptors.elementAt(i) != null && (spec.descriptors.elementAt(i) instanceof TimeRefDescription)) {
						TimeRefDescription td = (TimeRefDescription) spec.descriptors.elementAt(i);
						if (td.attrBuilt) {
							int timeFN = table.findAttrByName(td.attrName);
							if (timeFN >= 0) {
								switch (td.meaning) {
								case TimeRefDescription.OCCURRED_AT:
									occurTimeFN = timeFN;
									break;
								case TimeRefDescription.VALID_FROM:
									validFromFN = timeFN;
									break;
								case TimeRefDescription.VALID_UNTIL:
									validUntilFN = timeFN;
									break;
								}
							}
						}
					}
			}
			//construct geographical objects, possibly, with time references
			LayerData ld = new LayerData();
			for (int i = 0; i < table.getDataItemCount(); i++) {
				double x = table.getNumericAttrValue(xfn, i), y = table.getNumericAttrValue(yfn, i);
				if (Double.isNaN(x) || Double.isNaN(y)) {
					continue;
				}
				SpatialEntity spe = new SpatialEntity(table.getDataItemId(i));
				if (radiusFN < 0) {
					RealPoint rp = new RealPoint();
					rp.x = (float) x;
					rp.y = (float) y;
					spe.setGeometry(rp);
				} else {
					RealCircle rc = new RealCircle((float) x, (float) y, (float) table.getNumericAttrValue(radiusFN, i));
					spe.setGeometry(rc);
				}
				spe.setThematicData((ThematicDataItem) table.getDataItem(i));
				spe.setName(table.getDataItemName(i));
				TimeMoment validFrom = null, validUntil = null;
				if (occurTimeFN >= 0) {
					Object value = table.getAttrValue(occurTimeFN, i);
					if (value != null && (value instanceof TimeMoment)) {
						validFrom = (TimeMoment) value;
						validUntil = validFrom;
					}
				} else if (validFromFN >= 0) {
					Object value = table.getAttrValue(validFromFN, i);
					if (value != null && (value instanceof TimeMoment)) {
						validFrom = (TimeMoment) value;
					}
					if (validUntilFN >= 0) {
						value = table.getAttrValue(validUntilFN, i);
						if (value != null && (value instanceof TimeMoment)) {
							validUntil = (TimeMoment) value;
						}
					}
				}
				if (validFrom != null) {
					TimeReference tref = new TimeReference();
					tref.setValidFrom(validFrom);
					tref.setValidUntil(validUntil);
					spe.setTimeReference(tref);
					//System.out.println(spe.getId()+": from "+validFrom+" until "+validUntil);
				}
				ld.addItemSimple(spe);
				if ((i + 1) % 50 == 0) {
					showMessage(res.getString("constr_point") + (i + 1) + res.getString("_obj_constructed"), false);
				}
			}
			ld.setHasAllData(true);
			return ld;
		}
		return null;
	}

//----------------- TableContentSupplier interface ---------------------------
//----------------- (used for "delayed" loading of table data) ---------------
	/**
	* Using this method the table asks its supplier to fill it with the data.
	* The method returns true if the data have been successfully loaded.
	*/
	@Override
	public boolean fillTable() {
		if (dataReadingInProgress) {
			waitDataReadingFinish();
			return true;
		}
		return loadData(false);
	}

//----------------- DataSupplier interface -----------------------------------
//----------------- (used for "delayed" loading of map layers) ---------------
	/**
	* Returns the SpatialDataPortion containing all DataItems available
	*/
	@Override
	public DataPortion getData() {
		if (data != null)
			return data;
		if (dataError)
			return null;
		if (table == null || !table.hasData()) {
			if (dataReadingInProgress) {
				waitDataReadingFinish();
			} else {
				loadData(false);
			}
			if (data != null)
				return data;
		}
		return tryGetGeoObjects();
	}

	/**
	* Constructs and returns a DataPortion containing DataItems fitting in at
	* least one of the specified bounding rectangles (listed in the vector).
	* The elements of the vector are instances of
	* @see spade.vis.geometry.RealRectangle
	* Readers from files do not filter data according to any query,
	* therefore the method getData() without arguments is called
	*/
	@Override
	public DataPortion getData(Vector bounds) {
		return getData();
	}

	/**
	* When no more data from the DataSupplier are needed, this method is
	* called. Here the DataSupplier can clear its internal structures
	*/
	@Override
	public void clearAll() {
		data = null;
	}
}
