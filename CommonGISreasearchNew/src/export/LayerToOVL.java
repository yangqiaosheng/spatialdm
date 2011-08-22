package export;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.SystemUI;
import spade.lib.lang.Language;
import spade.vis.database.ObjectFilter;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.MultiGeometry;
import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealPolyline;
import spade.vis.geometry.RealRectangle;
import spade.vis.space.GeoObject;
import spade.vis.spec.DataSourceSpec;

public class LayerToOVL implements LayerExporter {
	static ResourceBundle res = Language.getTextResource("export.Res");
	private static byte b12[] = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

	/**
	* Returns the name of the output format suitable for showing to the user.
	* For example, "Shape (ArcView)", "csv (comma-separated values)"
	*/
	@Override
	public String getFormatName() {
		// following string: "OVL (Descartes-specific)"
		return res.getString("OVL_Descartes");
	}

	/**
	* Returns the usual file extension corresponding to this export format
	*/
	@Override
	public String getFileExtension() {
		return "ovl";
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
	* Returns a short text to explain the user what kind of layer may be saved
	* in this format. For example, "vector layer", "raster layer", "point objects"
	*/
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
		if (layer.getType() != Geometry.point && layer.getType() != Geometry.line && layer.getType() != Geometry.area) {
			// following string: "Illegal type of objects: vector objects expected!"
			if (ui != null) {
				ui.showMessage(res.getString("Illegal_type_of1"), true);
			}
			return false;
		}
		//find the exact layer boundaries taking into account the filter
		//simultaneousl, remember which objects are active
		float x1 = Float.NaN, x2 = x1, y1 = x1, y2 = x1;
		boolean active[] = new boolean[layer.getObjectCount()];
		if (filter == null) {
			for (int i = 0; i < active.length; i++) {
				active[i] = true;
			}
			RealRectangle r = layer.getWholeLayerBounds();
			x1 = r.rx1;
			x2 = r.rx2;
			y1 = r.ry1;
			y2 = r.ry2;
		} else {
			for (int i = 0; i < active.length; i++) {
				active[i] = false;
				GeoObject obj = layer.getObjectAt(i);
				if (obj == null || !(obj instanceof DGeoObject)) {
					continue;
				}
				DGeoObject gobj = (DGeoObject) obj;
				if (gobj.getGeometry() == null) {
					continue;
				}
				if (filter.isAttributeFilter())
					if (gobj.getData() != null) {
						active[i] = filter.isActive(gobj.getData());
					} else {
						active[i] = false;
					}
				else {
					active[i] = filter.isActive(gobj.getIdentifier());
				}
				if (active[i]) {
					RealRectangle r = gobj.getBounds();
					if (Float.isNaN(x1)) {
						x1 = r.rx1;
						x2 = r.rx2;
						y1 = r.ry1;
						y2 = r.ry2;
					} else {
						if (x1 > r.rx1) {
							x1 = r.rx1;
						}
						if (x2 < r.rx2) {
							x2 = r.rx2;
						}
						if (y1 > r.ry1) {
							y1 = r.ry1;
						}
						if (y2 < r.ry2) {
							y2 = r.ry2;
						}
					}
				}
			}
		}
		if (Float.isNaN(x1)) {
			// following string: "No objects to store!"
			if (ui != null) {
				ui.showMessage(res.getString("No_objects_to_store_"), true);
			}
			return false;
		}
		DataOutputStream dos = new DataOutputStream(stream);
		int nobj = 0;
		try {
			// following string: "Writing data..."
			if (ui != null) {
				ui.showMessage(res.getString("Writing_data_"), false);
			}
			dos.writeFloat(x1);
			dos.writeFloat(y1);
			dos.writeFloat(x2);
			dos.writeFloat(y2);
			for (int i = 0; i < active.length; i++)
				if (active[i]) {
					DGeoObject gobj = (DGeoObject) layer.getObjectAt(i);
					writeGeometry(gobj.getGeometry(), gobj.getIdentifier(), dos);
					++nobj;
					if (ui != null && nobj % 100 == 0) {
						// following string: " objects stored"
						ui.showMessage(nobj + res.getString("objects_stored"), false);
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
			// following string: " objects stored"
			if (nobj > 0) {
				ui.showMessage(nobj + res.getString("objects_stored"), false);
			} else {
				ui.showMessage(res.getString("No_objects_actually"), true);
			}
		return nobj > 0;
	}

	protected void writeGeometry(Geometry geom, String id, DataOutputStream f) throws IOException {
		if (geom == null || f == null)
			return;
		if (geom instanceof RealPoint) {
			RealPoint rp = (RealPoint) geom;
			writePointCountAndId(1, id, f);
			f.writeFloat(rp.x);
			f.writeFloat(rp.y);
		} else if (geom instanceof RealPolyline) {
			RealPolyline rl = (RealPolyline) geom;
			if (rl.p == null)
				return;
			writePointCountAndId(rl.p.length, id, f);
			for (RealPoint element : rl.p) {
				f.writeFloat(element.x);
				f.writeFloat(element.y);
			}
		} else if (geom instanceof MultiGeometry) {
			MultiGeometry mg = (MultiGeometry) geom;
			for (int i = 0; i < mg.getPartsCount(); i++) {
				writeGeometry(mg.getPart(i), id, f);
			}
		}
	}

	protected void writePointCountAndId(int nPoints, String id, DataOutputStream f) throws IOException {
		f.writeFloat(nPoints);
		//writing the identifier
		if (id == null) {
			f.write(b12, 0, 12);
		} else {
			id = id.trim();
			if (id.length() > 12) {
				id = id.substring(0, 12).trim();
			}
			if (id.length() < 1) {
				f.write(b12, 0, 12);
			} else {
				f.writeBytes(id);
				if (id.length() < 12) {
					f.write(b12, 0, 12 - id.length());
				}
			}
		}
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
			return result;
		}
		// following string: "Could not open the file "
		if (ui != null) {
			ui.showMessage(res.getString("Could_not_open_the") + dir + filename, true);
		}
		return false;
	}

	/**
	 * Returns the description of the file or database table to which the data
	 * were stored
	 */
	@Override
	public DataSourceSpec getStoredDataDescriptor() {
		return null;
	}
}
