package export;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 04-Nov-2005
 * Time: 16:12:51
 * This is a class for writing the contents of a Descartes layer with associated
 * thematic data into XML format
 */

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.SystemUI;
import spade.lib.lang.Language;
import spade.lib.util.IntArray;
import spade.lib.util.XMLUtil;
import spade.time.Date;
import spade.time.TimeCount;
import spade.time.TimeMoment;
import spade.time.TimeReference;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.ObjectFilter;
import spade.vis.database.SpatialEntity;
import spade.vis.database.ThematicDataItem;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DMovingObject;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.MultiGeometry;
import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealPolyline;
import spade.vis.space.GeoLayer;
import spade.vis.space.GeoObject;
import spade.vis.spec.DataSourceSpec;

public class LayerToXML extends TableToXML implements LayerExporter {
	static ResourceBundle res = Language.getTextResource("export.Res");
	/**
	 * Descriptor of the file to which the  data were stored
	 */
	protected DataSourceSpec spec = null;

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
		return (layerType == Geometry.point || layerType == Geometry.line || layerType == Geometry.area) && (subType == Geometry.undefined || subType == Geometry.movement);
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
		if (layer.getType() != Geometry.point && layer.getType() != Geometry.line && layer.getType() != Geometry.area) {
			// following string: "Illegal type of objects: vector objects expected!"
			if (ui != null) {
				ui.showMessage(res.getString("Illegal_type_of1"), true);
			}
			return false;
		}
		AttributeDataPortion table = null;
		if (layer.hasThematicData()) {
			table = layer.getThematicData();
		}
		DataOutputStream dos = new DataOutputStream(stream);
		int nObj = 0;
		IntArray attrNumbers = null;
		if (selAttr != null && selAttr.size() > 0 && table != null && table.getAttrCount() > 0) {
			attrNumbers = new IntArray(selAttr.size(), 1);
			for (int i = 0; i < selAttr.size(); i++) {
				int idx = table.getAttrIndex((String) selAttr.elementAt(i));
				if (idx >= 0) {
					attrNumbers.addElement(idx);
				}
			}
		}
		try {
			// following string: "Writing data..."
			if (ui != null) {
				ui.showMessage(res.getString("Writing_data_"), false);
			}
			dos.writeBytes("<?xml version='1.0'?>\r\n\r\n<ObjectData>\r\n\r\n");
			for (int i = 0; i < layer.getObjectCount(); i++) {
				GeoObject obj = layer.getObjectAt(i);
				if (obj == null || !(obj instanceof DGeoObject)) {
					continue;
				}
				DGeoObject gobj = (DGeoObject) obj;
				if (gobj.getGeometry() == null) {
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
				if (active && gobj.hasData()) {
					String name = gobj.getLabel();
					if (name != null && name.length() < 1) {
						name = null;
					}
					dos.writeBytes("<Object>\r\n");
					dos.writeBytes("\t<id>" + XMLUtil.encodeSpecChars(id) + "</id>\r\n");
					if (name != null) {
						dos.writeBytes("\t<name>" + XMLUtil.encodeSpecChars(name) + "</name>\r\n");
					}
					writeGeometry(gobj, dos);
					if (gobj.neighbours != null) {
						writeNeighbors(gobj.neighbours, dos);
					}
					writeTimeReference(gobj.getTimeReference(), dos);
					if (table != null && attrNumbers != null) {
						ThematicDataItem dit = gobj.getData();
						if (dit != null) {
							if (name == null) {
								name = dit.getName();
								if (name != null) {
									dos.writeBytes("\t<name>" + XMLUtil.encodeSpecChars(name) + "</name>\r\n");
								}
							}
							for (int j = 0; j < attrNumbers.size(); j++) {
								int idx = attrNumbers.elementAt(j);
								String val = dit.getAttrValueAsString(idx);
								if (val != null) {
									dos.writeBytes("\t<property name=\"" + XMLUtil.encodeSpecChars(table.getAttributeName(idx)) + "\" type=\"" + getTypeAsString(table.getAttributeType(idx)) + "\">" + val + "</property>\r\n");
								}
							}
						}
					}
					dos.writeBytes("</Object>\r\n\r\n");
					++nObj;
					if (ui != null && nObj % 100 == 0) {
						// following string: " rows stored"
						ui.showMessage(nObj + res.getString("rows_stored"), false);
					}
				}
			}
			dos.writeBytes("</ObjectData>\r\n");

		} catch (IOException ioe) {
			if (ui != null) {
				// following string: "Error writing to the file: "
				ui.showMessage(res.getString("Error_writing_to_the") + ioe.toString(), true);
			}
			return false;
		}
		if (ui != null)
			// following string: " rows stored"
			if (nObj > 0) {
				ui.showMessage(nObj + res.getString("rows_stored"), false);
			} else {
				ui.showMessage(res.getString("No_records_actually"), true);
			}
		return nObj > 0;
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
		spec.format = "XML";
		spec.idFieldName = "id";
		spec.nameFieldName = "Name";
		spec.name = layer.getName();
		DataSourceSpec dss = null;
		if (layer.getDataSource() != null && (layer.getDataSource() instanceof DataSourceSpec)) {
			dss = (DataSourceSpec) layer.getDataSource();
		}
		if (dss == null || dss.source == null || dss.source.equalsIgnoreCase("_derived")) {
			layer.setDataSource(spec);
		}
	}

	protected String dateToString(Date d) {
		if (d == null)
			return "";
		String str = "<date";
		if (d.useElement('y')) {
			str += " year=\"" + d.getElementValue('y') + "\"";
		}
		if (d.useElement('m')) {
			str += " month=\"" + d.getElementValue('m') + "\"";
		}
		if (d.useElement('d')) {
			str += " day=\"" + d.getElementValue('d') + "\"";
		}
		if (d.useElement('h')) {
			str += " hour=\"" + d.getElementValue('h') + "\"";
		}
		if (d.useElement('t')) {
			str += " minute=\"" + d.getElementValue('t') + "\"";
		}
		if (d.useElement('s')) {
			str += " second=\"" + d.getElementValue('s') + "\"";
		}
		str += " />";
		return str;
	}

	protected String timeCountToString(TimeCount tc) {
		if (tc == null)
			return "";
		return "<moment value=\"" + tc.toNumber() + "\" />";
	}

	protected String timeToString(TimeMoment t) {
		if (t == null)
			return "";
		if (t instanceof Date)
			return dateToString((Date) t);
		if (t instanceof TimeCount)
			return timeCountToString((TimeCount) t);
		return "";
	}

	protected void writeTimeReference(TimeReference tref, DataOutputStream f) throws IOException {
		if (tref == null || tref.getValidFrom() == null || f == null)
			return;
		f.writeBytes("\t<existenceTime>\r\n");
		f.writeBytes("\t\t<begin> " + timeToString(tref.getValidFrom()) + " </begin>\r\n");
		if (tref.getValidUntil() != null) {
			f.writeBytes("\t\t<end> " + timeToString(tref.getValidUntil()) + " </end>\r\n");
		}
		f.writeBytes("\t</existenceTime>\r\n");
	}

	/**
	 * @param neighbors - identifiers of the neighbours
	 */
	protected void writeNeighbors(Vector<String> neighbors, DataOutputStream f) throws IOException {
		if (neighbors == null || neighbors.size() < 1 || f == null)
			return;
		for (int i = 0; i < neighbors.size(); i++) {
			f.writeBytes("\t<neighbor>" + XMLUtil.encodeSpecChars(neighbors.elementAt(i)) + "</neighbor>\r\n");
		}
	}

	protected String trajectoryPositionToString(SpatialEntity trPosition) {
		TimeReference tref = trPosition.getTimeReference();
		if (tref == null || tref.getValidFrom() == null)
			return null;
		RealPoint rp = trPosition.getCentre();
		if (rp == null)
			return null;
		String str = "<point> <position>" + pointToString(rp) + "</position>";
		str += " <time>" + timeToString(tref.getValidFrom()) + "</time>";
		if (tref.getValidUntil() != null && !tref.getValidUntil().equals(tref.getValidFrom())) {
			str += " <lastTime>" + timeToString(tref.getValidUntil()) + "</lastTime>";
		}
		str += " </point>";
		return str;
	}

	protected void writeGeometry(DGeoObject gobj, DataOutputStream f) throws IOException {
		if (gobj == null || f == null)
			return;
		if (!(gobj instanceof DMovingObject)) {
			writeGeometry(gobj.getGeometry(), f);
			return;
		}
		DMovingObject mobj = (DMovingObject) gobj;
		Vector track = mobj.getTrack();
		if (track == null || track.size() < 1)
			return;
		f.writeBytes("\t<trajectory>\r\n");
		for (int j = 0; j < track.size(); j++) {
			String str = trajectoryPositionToString((SpatialEntity) track.elementAt(j));
			if (str == null) {
				continue;
			}
			f.writeBytes("\t\t" + str + "\r\n");
		}
		f.writeBytes("\t</trajectory>\r\n");
	}

	protected void writeGeometry(Geometry geom, DataOutputStream f) throws IOException {
		if (geom == null || f == null)
			return;
		boolean mayHaveCentroid = false;
		if (geom instanceof RealPoint) {
			RealPoint rp = (RealPoint) geom;
			f.writeBytes("\t<location>" + pointToString(rp) + "</location>\r\n");
		} else if (geom instanceof RealPolyline) {
			RealPolyline rl = (RealPolyline) geom;
			if (rl.p == null)
				return;
			mayHaveCentroid = rl.isClosed;
			f.writeBytes("\t<shape closed=\"" + rl.isClosed + "\">\r\n");
			for (RealPoint element : rl.p) {
				f.writeBytes("\t\t<point>" + pointToString(element) + "</point>\r\n");
			}
			f.writeBytes("\t</shape>\r\n");
		} else if (geom instanceof MultiGeometry) {
			MultiGeometry mg = (MultiGeometry) geom;
			if (mg.getPartsCount() < 2) {
				writeGeometry(mg.getPart(0), f);
			} else {
				f.writeBytes("\t<parts>\r\n");
				for (int i = 0; i < mg.getPartsCount(); i++)
					if (mg.getPart(i) instanceof RealPolyline) {
						RealPolyline rl = (RealPolyline) mg.getPart(i);
						if (rl.p == null) {
							continue;
						}
						f.writeBytes("\t\t<part closed=\"" + rl.isClosed + "\">\r\n");
						for (RealPoint element : rl.p) {
							f.writeBytes("\t\t\t<point>" + pointToString(element) + "</point>\r\n");
						}
						f.writeBytes("\t\t</part>\r\n");
					} else if (mg.getPart(i) instanceof RealPoint) {
						RealPoint rp = (RealPoint) mg.getPart(i);
						f.writeBytes("\t\t<part closed=\"false\">\r\n");
						f.writeBytes("\t\t\t<point>" + pointToString(rp) + "</point>\r\n");
						f.writeBytes("\t\t</part>\r\n");
					}
				f.writeBytes("\t</parts>\r\n");
			}
		}
		if (mayHaveCentroid) {
			float c[] = geom.getCentroid();
			if (c != null) {
				f.writeBytes("\t<centroid><xCoord>" + c[0] + "</xCoord><yCoord>" + c[1] + "</yCoord></centroid>\r\n");
			}
		}
	}

	protected String pointToString(RealPoint rp) {
		if (rp == null)
			return null;
		return "<xCoord>" + rp.x + "</xCoord><yCoord>" + rp.y + "</yCoord>";
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
