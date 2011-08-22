package export;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.SystemUI;
import spade.lib.lang.Language;
import spade.lib.util.IntArray;
import spade.lib.util.StringUtil;
import spade.time.Date;
import spade.time.TimeMoment;
import spade.time.TimeRefDescription;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.ObjectFilter;
import spade.vis.database.ThematicDataItem;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RealCircle;
import spade.vis.geometry.RealPoint;
import spade.vis.space.GeoLayer;
import spade.vis.space.GeoObject;
import spade.vis.spec.DataSourceSpec;
import spade.vis.spec.ParamSpec;

public class LayerToCSV extends TableToCSV implements LayerExporter {
	static ResourceBundle res = Language.getTextResource("export.Res");

	/**
	* Returns a short text to explain the user what kind of layer may be saved
	* in this format. For example, "vector layer", "raster layer", "point objects"
	*/
	// following string: "point objects only"
	@Override
	public String getDataChar() {
		return "points or circles";
	}

	/**
	 * Replies if this exporter is applicable to a layer of the given type and,
	 * possibly, subtype.
	 */
	@Override
	public boolean isApplicable(char layerType, char subType) {
		return layerType == Geometry.point || subType == Geometry.circle;
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
		if (!(data instanceof GeoLayer)) {
			// following string: "Illegal data type: GeoLayer expected!"
			if (ui != null) {
				ui.showMessage(res.getString("Illegal_data_type"), true);
			}
			return false;
		}
		GeoLayer layer = (GeoLayer) data;
		if (layer.getObjectCount() < 1) {
			// following string: "No data in the layer!"
			if (ui != null) {
				ui.showMessage(res.getString("No_data_in_the_layer_"), true);
			}
			return false;
		}
		if (layer.getType() != Geometry.point && layer.getSubtype() != Geometry.circle) {
			// following string: "Illegal type of objects: points expected!"
			if (ui != null) {
				ui.showMessage(res.getString("Illegal_type_of"), true);
			}
			return false;
		}
		AttributeDataPortion table = null;
		if (layer.hasThematicData()) {
			table = layer.getThematicData();
		}
		DataOutputStream dos = new DataOutputStream(stream);
		int nrows = 0;
		IntArray attrNumbers = null;
		if (selAttr != null && selAttr.size() > 0 && table != null && table.getAttrCount() > 0) {
			attrNumbers = new IntArray(selAttr.size(), 1);
			for (int i = 0; i < selAttr.size(); i++) {
				int idx = table.getAttrIndex((String) selAttr.elementAt(i));
				if (idx >= 0) {
					attrNumbers.addElement(idx);
					if (table.isAttributeTemporal(idx)) {
						TimeMoment t = null;
						for (int j = 0; j < table.getDataItemCount() && t == null; j++) {
							Object val = table.getAttrValue(idx, j);
							if (val != null && (val instanceof TimeMoment)) {
								t = (TimeMoment) val;
							}
						}
						if (t != null) {
							TimeRefDescription trefD = new TimeRefDescription();
							trefD.attrName = table.getAttributeName(idx);
							if (t instanceof Date) {
								trefD.attrScheme = ((Date) t).scheme;
							} else {
								trefD.attrScheme = "a";
							}
							trefD.sourceColumns = new String[1];
							trefD.sourceColumns[0] = trefD.attrName;
							trefD.schemes = new String[1];
							trefD.schemes[0] = trefD.attrScheme;
							trefD.isParameter = false;
							trefD.meaning = table.getAttribute(idx).timeRefMeaning;
							if (tRefDescr == null) {
								tRefDescr = new Vector(5, 5);
							}
							tRefDescr.addElement(trefD);
						}
					}
				}
			}
		}
		try {
			// following string: "Writing data..."
			if (ui != null) {
				ui.showMessage(res.getString("Writing_data_"), false);
			}
			String str = "id,Name,X,Y";
			if (layer.getSubtype() == Geometry.circle) {
				str += ",radius";
			}
			if (table != null && attrNumbers != null) {
				for (int j = 0; j < attrNumbers.size(); j++) {
					str += "," + StringUtil.eliminateCommas(table.getAttributeName(attrNumbers.elementAt(j)));
				}
			}
			dos.writeBytes(str + "\n");
			for (int i = 0; i < layer.getObjectCount(); i++) {
				GeoObject obj = layer.getObjectAt(i);
				if (obj == null || !(obj instanceof DGeoObject)) {
					continue;
				}
				DGeoObject gobj = (DGeoObject) obj;
				Geometry geom = gobj.getGeometry();
				if (geom == null) {
					continue;
				}
				if (!(geom instanceof RealPoint) && !(geom instanceof RealCircle)) {
					continue;
				}
				String id = gobj.getIdentifier();
				boolean active = true;
				if (filter != null)
					if (filter.isAttributeFilter())
						if (gobj.getData() != null) {
							active = filter.isActive(gobj.getData());
						} else {
							;
						}
					else {
						active = filter.isActive(id);
					}
				if (active) {
					String name = gobj.getLabel();
					if (name == null) {
						name = "";
					}
					str = StringUtil.eliminateCommas(id) + "," + StringUtil.eliminateCommas(name);
					if (geom instanceof RealPoint) {
						RealPoint rp = (RealPoint) geom;
						str += "," + rp.x + "," + rp.y;
					} else {
						RealCircle rc = (RealCircle) geom;
						str += "," + rc.cx + "," + rc.cy + "," + rc.rad;
					}
					if (table != null && attrNumbers != null) {
						ThematicDataItem dit = gobj.getData();
						if (dit == null) {
							for (int j = 0; j < attrNumbers.size(); j++) {
								str += ",";
							}
						} else {
							for (int j = 0; j < attrNumbers.size(); j++) {
								String val = dit.getAttrValueAsString(attrNumbers.elementAt(j));
								if (val == null) {
									val = "";
								}
								str += "," + StringUtil.eliminateCommas(val);
							}
						}
					}
					dos.writeBytes(str + "\n");
					++nrows;
					if (ui != null && nrows % 100 == 0) {
						// following string: " rows stored"
						ui.showMessage(nrows + res.getString("rows_stored"), false);
					}
				}
			}
		} catch (IOException ioe) {
			if (ui != null) {
				// following string: "Error writing to the file: "
				ui.showMessage(res.getString("Error_writing_to_the") + ioe.toString(), true);
			}
			return false;
		}
		if (ui != null)
			// following string: " rows stored"
			if (nrows > 0) {
				ui.showMessage(nrows + res.getString("rows_stored"), false);
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
		if (spec == null) {
			spec = new DataSourceSpec();
		}
		spec.source = source;
		spec.format = "ASCII";
		spec.delimiter = ",";
		spec.nRowWithFieldNames = 0;
		spec.idFieldName = "id";
		spec.nameFieldName = "Name";
		spec.name = layer.getName();
		spec.xCoordFieldName = "X";
		spec.yCoordFieldName = "Y";
		if (layer.getSubtype() == Geometry.circle) {
			spec.radiusFieldName = "radius";
			spec.objType = Geometry.area;
			spec.objSubType = Geometry.circle;
		} else {
			spec.objType = Geometry.point;
		}
		if (tRefDescr != null && tRefDescr.size() > 0) {
			if (spec.descriptors == null) {
				spec.descriptors = new Vector(5, 5);
			}
			for (int i = 0; i < tRefDescr.size(); i++) {
				spec.descriptors.addElement(tRefDescr.elementAt(i));
			}
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
			layer.setDataSource(dss);
		}
	}
}
