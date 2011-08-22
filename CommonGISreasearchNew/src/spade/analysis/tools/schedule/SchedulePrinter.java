package spade.analysis.tools.schedule;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.lib.lang.Language;
import spade.lib.util.BubbleSort;
import spade.lib.util.Comparator;
import spade.lib.util.IntArray;
import spade.lib.util.StringUtil;
import spade.time.TimeMoment;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataTable;
import spade.vis.database.ThematicDataItem;
import spade.vis.dmap.DGeoLayer;
import spade.vis.space.GeoObject;
import spade.vis.spec.DataSourceSpec;
import spade.vis.spec.LinkDataDescription;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: 29-Feb-2008
 * Time: 10:43:49
 * Writes a transportation schedule to a text file
 */
public class SchedulePrinter implements Comparator {
	static ResourceBundle res = Language.getTextResource("spade.analysis.tools.schedule.SchedulerTexts_tools_schedule");

	public static String metaKeys[] = { "ITEM_CLASS_FIELD_NAME", "ITEM_NUMBER_FIELD_NAME", "VEHICLE_ID_FIELD_NAME", "VEHICLE_HOME_ID_FIELD_NAME", "SOURCE_NAME_FIELD_NAME", "DESTIN_NAME_FIELD_NAME", "VEHICLE_CLASS_FIELD_NAME",
			"VEHICLE_HOME_NAME_FIELD_NAME" };
	/**
	 * The number of mandatory attributes listed in the metaKeys
	 * at the beginning
	 */
	public static int nMandatory = 4;
	/**
	 * The table with the orders
	 */
	public DataTable tableOrders = null;
	/**
	 * The layer with all sites
	 */
	public DGeoLayer locLayer = null;
	/**
	 * A table with static data about the vehicles including type, identifier and
	 * name of the home base (initial location) and, possibly, something else.
	 */
	public DataTable vehicleInfo = null;
	/**
	 * The indexes of the columns with the trip sources and destinations
	 */
	public int souColIdx = -1, destColIdx = -1;
	/**
	 * The indexes of the columns with the trip start and end times
	 */
	public int souTimeColIdx = -1, destTimeColIdx = -1;
	/**
	 * The column numbers corresponding to the metaKeys
	 */
	public int keyColNs[] = null;
	/**
	 * The indexes (column numbers) of the attributes used to sort and group the orders
	 */
	public IntArray sortColNs = null;
	/**
	 * The records of the table with the orders in the sequence prepared for writing
	 */
	public Vector orders = null;

	/**
	 * The table with the orders
	 */
	public boolean setOrders(DataTable table) {
		this.tableOrders = table;
		if (table == null || table.getDataItemCount() < 1)
			return false;
		DataSourceSpec spec = (DataSourceSpec) table.getDataSource();
		if (spec == null || spec.descriptors == null || spec.extraInfo == null)
			return false;
		LinkDataDescription ldd = null;
		for (int i = 0; i < spec.descriptors.size() && ldd == null; i++)
			if (spec.descriptors.elementAt(i) instanceof LinkDataDescription) {
				ldd = (LinkDataDescription) spec.descriptors.elementAt(i);
			}
		if (ldd == null)
			return false;
		if (ldd.souColIdx >= 0) {
			souColIdx = ldd.souColIdx;
		} else if (ldd.souColName != null) {
			souColIdx = table.findAttrByName(ldd.souColName);
		}
		if (souColIdx < 0)
			return false;
		if (ldd.destColIdx >= 0) {
			destColIdx = ldd.destColIdx;
		} else if (ldd.destColName != null) {
			destColIdx = table.findAttrByName(ldd.destColName);
		}
		if (destColIdx < 0)
			return false;
		if (ldd.souTimeColIdx >= 0) {
			souTimeColIdx = ldd.souTimeColIdx;
		} else if (ldd.souTimeColName != null) {
			souTimeColIdx = table.findAttrByName(ldd.souTimeColName);
		}
		if (souTimeColIdx < 0)
			return false;
		if (ldd.destTimeColIdx >= 0) {
			destTimeColIdx = ldd.destTimeColIdx;
		} else if (ldd.destTimeColName != null) {
			destTimeColIdx = table.findAttrByName(ldd.destTimeColName);
		}
		if (destTimeColIdx < 0)
			return false;
		keyColNs = new int[metaKeys.length];
		for (int i = 0; i < metaKeys.length; i++) {
			Object val = spec.extraInfo.get(metaKeys[i]);
			if (val != null && (val instanceof String)) {
				keyColNs[i] = table.findAttrByName((String) val);
				if (i < nMandatory && keyColNs[i] < 0)
					return false;
			}
		}
		sortColNs = new IntArray(5, 5);
		if (keyColNs[7] >= 0) {
			sortColNs.addElement(keyColNs[7]); //vehicle home name
		} else {
			sortColNs.addElement(keyColNs[3]); //vehicle home id
		}
		if (keyColNs[6] >= 0) {
			sortColNs.addElement(keyColNs[6]); //vehicle type
		}
		sortColNs.addElement(keyColNs[2]); //vehicle id
		sortColNs.addElement(souTimeColIdx);
		orders = (Vector) table.getData().clone();
		BubbleSort.sort(orders, this);
		return true;
	}

	/**
	 * Compares two objects if they are instances of ThematicDataItem.
	 * Returns 0 if the objects are equal, <0 if the first object is less than the
	 * second one, >0 otherwise
	 */
	@Override
	public int compare(Object obj1, Object obj2) {
		if (obj1 == null)
			if (obj2 == null)
				return 0;
			else
				return 1;
		if (obj2 == null)
			return -1;
		if (!(obj1 instanceof ThematicDataItem) || !(obj2 instanceof ThematicDataItem))
			return 0;
		ThematicDataItem d1 = (ThematicDataItem) obj1, d2 = (ThematicDataItem) obj2;
		if (sortColNs != null && sortColNs.size() > 0) {
			for (int i = 0; i < sortColNs.size(); i++) {
				int idx = sortColNs.elementAt(i);
				char type = d1.getAttrType(idx);
				if (AttributeTypes.isNumericType(type)) {
					double v1 = d1.getNumericAttrValue(idx), v2 = d2.getNumericAttrValue(idx);
					if (!Double.isNaN(v1))
						if (!Double.isNaN(v2)) {
							if (v1 < v2)
								return -1;
							if (v1 > v2)
								return 1;
						} else
							return -1;
					else if (!Double.isNaN(v2))
						return 1;
				} else {
					Object val1 = d1.getAttrValue(idx), val2 = d2.getAttrValue(idx);
					if (val1 == null && val2 == null) {
						continue;
					}
					if (val1 == null)
						return 1;
					if (val2 == null)
						return -1;
					if (AttributeTypes.isTemporal(type) && (val1 instanceof TimeMoment) && (val2 instanceof TimeMoment)) {
						TimeMoment t1 = (TimeMoment) val1, t2 = (TimeMoment) val2;
						int diff = t1.compareTo(t2);
						if (diff != 0)
							return diff;
					} else {
						int diff = BubbleSort.compare(val1, val2);
						if (diff != 0)
							return diff;
					}
				}
			}
		}
		return 1;
	}

	/**
	 * The layer with all sites
	 */
	public void setLocLayer(DGeoLayer locLayer) {
		this.locLayer = locLayer;
	}

	/**
	 * A table with static data about the vehicles including type, identifier and
	 * name of the home base (initial location) and, possibly, something else.
	 */
	public void setVehicleInfo(DataTable vehicleInfo) {
		this.vehicleInfo = vehicleInfo;
	}

	/**
	 * Writes the orders to the specified file. If "itemCatNames" or "itemCatCodes"
	 * is not null, writes only orders relevant to the specified item categories,
	 * that is, orders for the vehicles which are used for transporting items of
	 * these categories. If those vehicles are also used for other item categories,
	 * all their orders will be written.
	 */
	public boolean writeOrdersToFile(String path, Vector itemCatNames, Vector itemCatCodes) {
		if (orders == null)
			return false;
		if (path == null)
			return false;
		FileWriter fwr = null;
		try {
			fwr = new FileWriter(path, false);
		} catch (IOException ioe) {
			System.out.println(ioe.toString());
			return false;
		}
		int vhTypeNameColN = -1;
		if (vehicleInfo != null) {
			vhTypeNameColN = vehicleInfo.getAttrIndex("vehicleTypeName");
			if (vhTypeNameColN < 0) {
				vhTypeNameColN = vehicleInfo.findAttrByName("Vehicle type description");
			}
		}
		boolean hasCatNames = itemCatNames != null && itemCatNames.size() > 0;
		boolean hasCatCodes = itemCatCodes != null && itemCatCodes.size() > 0;
		String lastVhId = null;
		int tripN = 0;
		int ordN = 0;
		try {
			while (ordN < orders.size()) {
				ThematicDataItem rec = (ThematicDataItem) orders.elementAt(ordN);
				String vhId = rec.getAttrValueAsString(keyColNs[2]);
				if (vhId == null) {
					++ordN;
					continue;
				}
				if (!vhId.equals(lastVhId)) {
					//check if this vehicle is used for transporting items of the selected categories
					if (hasCatNames || hasCatCodes) {
						boolean relevant = false;
						int rN = ordN;
						for (rN = ordN; rN < orders.size() && !relevant; rN++) {
							ThematicDataItem r = (ThematicDataItem) orders.elementAt(rN);
							if (rN > ordN) {
								String vid = r.getAttrValueAsString(keyColNs[2]);
								if (!vid.equals(vhId)) {
									break;
								}
							}
							String loadTypeStr = r.getAttrValueAsString(keyColNs[0]);
							if (loadTypeStr == null || loadTypeStr.equalsIgnoreCase("LEER") || loadTypeStr.equalsIgnoreCase("EMPTY")) {
								continue;
							}
							relevant = (hasCatNames && StringUtil.isStringInVectorIgnoreCase(loadTypeStr, itemCatNames)) || (hasCatCodes && StringUtil.isStringInVectorIgnoreCase(loadTypeStr, itemCatCodes));
						}
						if (!relevant) {
							ordN = rN;
							continue;
						}
					}
					if (tripN > 0) {
						fwr.write("\r\n");
					}
					String vhTypeCode = null, vhTypeName = null;
					if (keyColNs[6] >= 0) {
						vhTypeCode = rec.getAttrValueAsString(keyColNs[6]);
					}
					if (vhTypeNameColN >= 0) {
						int idx = vehicleInfo.indexOf(vhId);
						if (idx >= 0) {
							vhTypeName = vehicleInfo.getAttrValueAsString(vhTypeNameColN, idx);
						}
					}
					String vhHomeId = rec.getAttrValueAsString(keyColNs[3]);
					String vhHomeName = null;
					if (keyColNs[7] >= 0) {
						vhHomeName = rec.getAttrValueAsString(keyColNs[7]);
					}
					float lat = Float.NaN, lon = Float.NaN;
					if (locLayer != null) {
						GeoObject gobj = locLayer.findObjectById(vhHomeId);
						if (gobj != null && gobj.getGeometry() != null) {
							float b[] = gobj.getGeometry().getBoundRect();
							if (b != null) {
								lon = (b[0] + b[2]) / 2;
								lat = (b[1] + b[3]) / 2;
							}
							if (vhHomeName == null && gobj.getName() != null && !gobj.getName().equalsIgnoreCase(vhHomeId)) {
								vhHomeName = gobj.getName();
							}
						}
					}
					fwr.write(res.getString("Vehicle") + " " + vhId + "\r\n");
					fwr.write(res.getString("Type") + ": ");
					if (vhTypeName != null) {
						fwr.write(vhTypeName + " (" + vhTypeCode + ")\r\n");
					} else {
						fwr.write(vhTypeCode + "\r\n");
					}
					fwr.write(res.getString("Initial_position") + ": ");
					if (vhHomeName != null) {
						fwr.write(vhHomeName + " (" + vhHomeId + ")");
					} else {
						fwr.write(vhHomeId);
					}
					if (!Float.isNaN(lat)) {
						fwr.write("; " + res.getString("latitude") + "=" + lat + "; " + res.getString("longitude") + "=" + lon);
					}
					fwr.write("\r\n\r\n");
					fwr.write(res.getString("Trip_N") + "\t" + res.getString("Time") + "\t" + res.getString("Load") + "\t" + res.getString("Amount"));
					fwr.write("\t" + res.getString("Origin"));
					if (keyColNs[4] >= 0) {
						fwr.write("\t" + res.getString("Identifier"));
					}
					if (locLayer != null) {
						fwr.write("\t" + res.getString("latitude") + "\t" + res.getString("longitude"));
					}
					fwr.write("\t" + res.getString("Destination"));
					if (keyColNs[5] >= 0) {
						fwr.write("\t" + res.getString("Identifier"));
					}
					if (locLayer != null) {
						fwr.write("\t" + res.getString("latitude") + "\t" + res.getString("longitude"));
					}
					fwr.write("\t" + res.getString("End_time"));
					fwr.write("\r\n");
					tripN = 0;
					lastVhId = vhId;
				}
				++tripN;
				String loadTypeStr = rec.getAttrValueAsString(keyColNs[0]);
				if (loadTypeStr == null || loadTypeStr.equalsIgnoreCase("LEER") || loadTypeStr.equalsIgnoreCase("EMPTY")) {
					loadTypeStr = "-";
				}
				fwr.write(tripN + "\t" + rec.getAttrValueAsString(souTimeColIdx) + "\t" + loadTypeStr + "\t" + rec.getAttrValueAsString(keyColNs[1]));
				if (keyColNs[4] >= 0) {
					fwr.write("\t" + rec.getAttrValueAsString(keyColNs[4]));
				}
				String siteId = rec.getAttrValueAsString(souColIdx);
				fwr.write("\t" + siteId);
				if (locLayer != null) {
					GeoObject gobj = locLayer.findObjectById(siteId);
					float lat = Float.NaN, lon = Float.NaN;
					if (gobj != null && gobj.getGeometry() != null) {
						float b[] = gobj.getGeometry().getBoundRect();
						if (b != null) {
							lon = (b[0] + b[2]) / 2;
							lat = (b[1] + b[3]) / 2;
						}
					}
					if (Float.isNaN(lat)) {
						fwr.write("\t \t ");
					} else {
						fwr.write("\t" + lat + "\t" + lon);
					}
				}
				if (keyColNs[5] >= 0) {
					fwr.write("\t" + rec.getAttrValueAsString(keyColNs[5]));
				}
				siteId = rec.getAttrValueAsString(destColIdx);
				fwr.write("\t" + siteId);
				if (locLayer != null) {
					GeoObject gobj = locLayer.findObjectById(siteId);
					float lat = Float.NaN, lon = Float.NaN;
					if (gobj != null && gobj.getGeometry() != null) {
						float b[] = gobj.getGeometry().getBoundRect();
						if (b != null) {
							lon = (b[0] + b[2]) / 2;
							lat = (b[1] + b[3]) / 2;
						}
					}
					if (Float.isNaN(lat)) {
						fwr.write("\t \t ");
					} else {
						fwr.write("\t" + lat + "\t" + lon);
					}
				}
				fwr.write("\t" + rec.getAttrValueAsString(destTimeColIdx) + "\r\n");
				++ordN;
			}
		} catch (IOException ioe) {
			System.out.println(ioe.toString());
			return false;
		}
		try {
			fwr.close();
		} catch (IOException ioe) {
		}
		return true;
	}
}
