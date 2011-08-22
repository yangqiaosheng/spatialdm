package data_load.cache;

import java.util.Vector;

import spade.vis.database.Attribute;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.database.LayerData;
import spade.vis.database.SpatialDataItem;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.MultiGeometry;
import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealPolyline;
import spade.vis.geometry.RealRectangle;

public class MemoryUtil {

	private MemoryUtil() {
	}

	public static int sizeOf(Attribute data) {
		return data != null ? sizeOf(data.getIdentifier()) + sizeOf(data.getName()) + 2/*type*/: 0;
	}

	public static int sizeOf(DataRecord data) {
		if (data == null)
			return 0;
		int size = sizeOf(data.getId()) + (data.hasName() ? sizeOf(data.getName()) : 0) + data.getAttrCount() * 4 + 4/*link to attribute list*/;
		Vector vals = data.getAttrValues();
		if (vals != null) {
			for (int i = 0; i < vals.size(); i++) {
				size += sizeOf((String) vals.elementAt(i));
			}
		}
		return size;
	}

	public static int sizeOf(DataTable data) {
		if (data == null)
			return 0;
		int size = 18/*version, sm*/;
		Vector attrlist = data.getAttrList();
		if (attrlist != null) {
			for (int i = 0; i < attrlist.size(); i++) {
				size += sizeOf((Attribute) attrlist.elementAt(i));
			}
		}
		for (int i = 0; i < data.getDataItemCount(); i++) {
			size += sizeOf(data.getDataRecord(i));
		}
		return size;
	}

	public static int sizeOf(SpatialDataItem data) {
		if (data == null)
			return 0;
		int size = sizeOf(data.getId()) + sizeOf(data.getGeometry()) + sizeOf((DataRecord) data.getThematicData());
		if (data.hasName()) {
			size += sizeOf(data.getName());
		}
		return size;
	}

	public static int sizeOf(LayerData data) {
		if (data == null)
			return 0;
		int size = sizeOf(data.getBoundingRectangle()) + sizeOf(data.hasAllData());
		for (int i = 0; i < data.getDataItemCount(); i++) {
			size += sizeOf((SpatialDataItem) data.getDataItem(i));
		}
		return size;
	}

	public static int sizeOf(Geometry geom) {
		if (geom == null)
			return 0;
		if (geom instanceof RealPoint)
			return sizeOf((RealPoint) geom);
		else if (geom instanceof RealPolyline)
			return sizeOf((RealPolyline) geom);
		else if (geom instanceof RealRectangle)
			return sizeOf((RealRectangle) geom);
		else if (geom instanceof MultiGeometry)
			return sizeOf((MultiGeometry) geom);
		else
			return 0;
	}

	private static int sizeOf(RealPoint geom) {
		if (geom == null)
			return 0;
		return sizeOf(geom.getBoundRect()) + sizeOf(geom.x) + sizeOf(geom.y);
	}

	private static int sizeOf(RealPolyline geom) {
		if (geom == null)
			return 0;
		int size = 1/*isClosed*/+ sizeOf(geom.getBoundRect()) + sizeOf(geom.boundRect) + sizeOf(geom.labelRect);
		if (geom.p != null) {
			for (RealPoint element : geom.p) {
				size += sizeOf(element);
			}
		}
		return size;
	}

	private static int sizeOf(RealRectangle geom) {
		if (geom == null)
			return 0;
		return sizeOf(geom.getBoundRect()) + sizeOf(geom.rx1) + sizeOf(geom.ry1) + sizeOf(geom.rx2) + sizeOf(geom.ry2);
	}

	private static int sizeOf(MultiGeometry geom) {
		if (geom == null)
			return 0;
		int size = sizeOf(geom.getBoundRect());
		for (int i = 0; i < geom.getPartsCount(); i++) {
			size += sizeOf(geom.getPart(i));
		}
		return size;
	}

	public static int sizeOf(boolean b) {
		return 1;
	}

	public static int sizeOf(byte b) {
		return 1;
	}

	public static int sizeOf(char c) {
		return 2;
	}

	public static int sizeOf(short s) {
		return 2;
	}

	public static int sizeOf(int i) {
		return 4;
	}

	public static int sizeOf(long l) {
		return 8;
	}

	public static int sizeOf(float f) {
		return 4;
	}

	public static int sizeOf(double d) {
		return 8;
	}

	public static int sizeOf(String s) {
		return s != null ? s.length() * 2 : 0;
	}

	public static int sizeOf(boolean b[]) {
		return b != null ? b.length : 0;
	}

	public static int sizeOf(byte b[]) {
		return b != null ? b.length : 0;
	}

	public static int sizeOf(char c[]) {
		return c != null ? c.length * 2 : 0;
	}

	public static int sizeOf(short s[]) {
		return s != null ? s.length * 2 : 0;
	}

	public static int sizeOf(int i[]) {
		return i != null ? i.length * 4 : 0;
	}

	public static int sizeOf(long l[]) {
		return l != null ? l.length * 8 : 0;
	}

	public static int sizeOf(float f[]) {
		return f != null ? f.length * 4 : 0;
	}

	public static int sizeOf(double d[]) {
		return d != null ? d.length * 8 : 0;
	}

	public static int sizeOf(String s[]) {
		int size = 0;
		if (s != null) {
			for (String element : s) {
				size += sizeOf(element);
			}
		}
		return size;
	}

}
