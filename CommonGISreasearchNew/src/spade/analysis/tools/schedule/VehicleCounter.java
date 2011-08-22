package spade.analysis.tools.schedule;

import java.awt.Color;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.lib.lang.Language;
import spade.lib.util.StringUtil;
import spade.time.TimeMoment;
import spade.vis.database.DataAggregator;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.spec.DataSourceSpec;
import spade.vis.spec.LinkDataDescription;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 08-Mar-2007
 * Time: 16:10:24
 * Using a table with a transportation schedule as an input, extracts
 * time-dependent information about the vehicles: state at each time (loaded,
 * empty but moving to take a new load, idle), number of items on board,
 * percentage of the capacity used (if the total capacity is known from somewhere).
 */
public class VehicleCounter {
	static ResourceBundle res = Language.getTextResource("spade.analysis.tools.schedule.SchedulerTexts_tools_schedule");

	/**
	 * The possible states (activities) of the vehicles
	 */
	public static final int vehicle_idle = 0, vehicle_empty_move = 1, vehicle_loaded_move = 2;
	/**
	 * The names of these activities
	 */
	public static final String activity_names[] = { res.getString("Idle"), res.getString("Moving_without_load"), res.getString("Loaded") };
	/**
	 * Colors to symbolize these activities
	 */
	public static final Color activity_colors[] = { new Color(255, 255, 178), new Color(146, 197, 222), new Color(240, 59, 32) };
	/**
	 * A part of the output: the matrix with the information about the activities
	 * of the vehicles
	 */
	public int vehicleActivity[][] = null;
	/**
	 * A part of the output: the list of the identifiers of the vehicles,
	 * which correspond to the rows of the activity matrix
	 */
	public Vector vehicleIds = null;
	/**
	 * The types of all vehicles listed in the vehicleIds
	 */
	public Vector vehicleTypes = null;
	/**
	 * The number of actually used vehicles
	 */
	public int nUsedVehicles = 0;
	/**
	 * Another part of the output: ordered list of the time moments corresponding
	 * to the columns of the matrix. The elements are instances of TimeMoment
	 * and indicate the beginnings of the time intervals. The last element in the
	 * list is the end of the last time interval and does not correspond to any
	 * column.
	 */
	public Vector times = null;
	/**
	 * Extracted static data about the vehicles including type, identifier and
	 * name of the home base (initial location) and, possibly, something else.
	 * The table is created only when it is known from which columns of the table
	 * with the schedule the corresponding data may be taken.
	 */
	public DataTable vehicleInfo = null;
	/**
	 * Extracted categories of items transported by the vehicles. If this vector
	 * is null, all items are the same.
	 */
	public Vector itemCategories = null;
	/**
	 * For each item category, a matrix of vehicle loads by time (matrix columns)
	 * for all vehicles (matrix rows)
	 */
	public Vector loads = null;
	/**
	 * For each vehicle type contains the item categories it is suitable for,
	 * capacities for these item categories and, possibly, loading times per item.
	 * Structure:
	 * vehicle type,item type,capacity,loading time
	 * There may be several records for one vehicle type if it is suitable for
	 * more than one item categories.
	 * These data must be provided externally.
	 */
	public DataTable vehicleCap = null;
	/**
	 * For each item category, an instance of VehicleLoadData containing data
	 * about the use of capacities in the vehicles suitable for this item
	 * category.
	 */
	public Vector capUses = null;
	/**
	 * The use of vehicle capacities for all item categories together
	 */
	public VehicleLoadData capUseAllCat = null;
	/**
	 * Keeps and aggregates data about the visits of the different places by the vehicles
	 */
	public DataAggregator vehicleVisitsAggregator = null;

	/**
	 * Sets the list of item categories (should not include "LEER" or "EMPTY")
	 */
	public void setItemCategories(Vector itemCat) {
		itemCategories = itemCat;
	}

	/**
	 * Supplies data about the capacities by vehicle classes.
	 * Structure of the table:
	 * vehicle type,item type,capacity,loading time
	 * There may be several records for one vehicle type if it is suitable for
	 * more than one item categories.
	 */
	public void setVehicleCapacityTable(DataTable vehicleCap) {
		this.vehicleCap = vehicleCap;
	}

	/**
	 * Analyses the input table with the transportation schedule and constructs
	 * the output matrix and list of time moments. Returns true if successful
	 * and false in case of inappropriate input data.
	 * vehicleSources contains data about all available vehicles (not all of them
	 * may be used in a schedule)
	 */
	public boolean countVehicles(DataTable schedule, LinkDataDescription ldd, int vehicleIdColIdx, int vehicleTypeColIdx, int vehicleHomeIdColIdx, int vehicleHomeNameColIdx, int itemNumColIdx, int itemCatColIdx, DataTable vehicleSources) {
		if (schedule == null || schedule.getDataItemCount() < 1)
			return false;
		if (vehicleIdColIdx < 0 || itemNumColIdx < 0)
			return false;
		if (ldd == null || ldd.souTimeColIdx < 0 || ldd.destTimeColIdx < 0)
			return false;
		//Retrieve all vehicle identifiers from the table
		vehicleIds = schedule.getAllAttrValuesAsStrings(schedule.getAttributeId(vehicleIdColIdx));
		if (vehicleIds == null || vehicleIds.size() < 1)
			return false;
		nUsedVehicles = vehicleIds.size();
		Vector vHomeIds = null, vHomeNames = null;
		if (vehicleTypeColIdx >= 0 || vehicleHomeIdColIdx >= 0) {
			if (vehicleTypeColIdx >= 0) {
				vehicleTypes = new Vector(vehicleIds.size(), 50);
				for (int i = 0; i < vehicleIds.size(); i++) {
					vehicleTypes.addElement("0");
				}
			}
			if (vehicleHomeIdColIdx >= 0) {
				vHomeIds = new Vector(vehicleIds.size(), 50);
				for (int i = 0; i < vehicleIds.size(); i++) {
					vHomeIds.addElement(null);
				}
			}
			if (vehicleHomeNameColIdx >= 0) {
				vHomeNames = new Vector(vehicleIds.size(), 50);
				for (int i = 0; i < vehicleIds.size(); i++) {
					vHomeNames.addElement(null);
				}
			}
			for (int i = 0; i < schedule.getDataItemCount(); i++) {
				DataRecord rec = schedule.getDataRecord(i);
				String vId = rec.getAttrValueAsString(vehicleIdColIdx);
				int vIdx = StringUtil.indexOfStringInVectorIgnoreCase(vId, vehicleIds);
				if (vIdx < 0) {
					continue; //this should not happen!
				}
				if (vehicleTypes != null && ((String) vehicleTypes.elementAt(vIdx)).equals("0")) {
					String val = rec.getAttrValueAsString(vehicleTypeColIdx);
					if (val != null) {
						vehicleTypes.setElementAt(val, vIdx);
					}
				}
				if (vHomeIds != null && vHomeIds.elementAt(vIdx) == null) {
					String val = rec.getAttrValueAsString(vehicleHomeIdColIdx);
					if (val != null) {
						vHomeIds.setElementAt(val, vIdx);
					}
				}
				if (vHomeNames != null && vHomeNames.elementAt(vIdx) == null) {
					String val = rec.getAttrValueAsString(vehicleHomeNameColIdx);
					if (val != null) {
						vHomeNames.setElementAt(val, vIdx);
					}
				}
			}
		}
		if (vehicleSources != null && vehicleSources.getDataItemCount() > 0 && vehicleHomeIdColIdx >= 0 && vehicleTypes != null && vHomeIds != null) {
			DataSourceSpec spec = (DataSourceSpec) vehicleSources.getDataSource();
			if (spec != null && spec.extraInfo != null && spec.extraInfo.get("SOURCE_ID_FIELD_NAME") != null && spec.extraInfo.get("VEHICLE_CLASS_ID_FIELD_NAME") != null && spec.extraInfo.get("VEHICLE_NUMBER_FIELD_NAME") != null) {
				int souIdColN = vehicleSources.findAttrByName((String) spec.extraInfo.get("SOURCE_ID_FIELD_NAME")), vClIdColN = vehicleSources.findAttrByName((String) spec.extraInfo.get("VEHICLE_CLASS_ID_FIELD_NAME")), vNumColN = vehicleSources
						.findAttrByName((String) spec.extraInfo.get("VEHICLE_NUMBER_FIELD_NAME")), souNameColN = vehicleSources.findAttrByName((String) spec.extraInfo.get("SOURCE_NAME_FIELD_NAME"));
				if (souIdColN >= 0 && vClIdColN >= 0 && vNumColN >= 0) {
					for (int i = 0; i < vehicleSources.getDataItemCount(); i++) {
						DataRecord rec = vehicleSources.getDataRecord(i);
						String souId = rec.getAttrValueAsString(souIdColN);
						if (souId == null) {
							continue;
						}
						String vType = rec.getAttrValueAsString(vClIdColN);
						if (vType == null) {
							continue;
						}
						double fval = rec.getNumericAttrValue(vNumColN);
						if (Double.isNaN(fval)) {
							continue;
						}
						int num = (int) Math.round(fval);
						if (num < 1) {
							continue;
						}
						String souName = null;
						if (souNameColN >= 0) {
							souName = rec.getAttrValueAsString(souNameColN);
						}
						for (int j = 0; j < nUsedVehicles && num > 0; j++)
							if (souId.equalsIgnoreCase((String) vHomeIds.elementAt(j)) && vType.equalsIgnoreCase((String) vehicleTypes.elementAt(j))) {
								--num;
								if (souName == null && vHomeNames != null && vHomeNames.elementAt(j) != null) {
									souName = (String) vHomeNames.elementAt(j);
								}
							}
						if (num < 1) {
							continue;
						}
						//there are unused vehicles in this source location!
						for (int j = 0; j < num; j++) {
							vehicleIds.addElement("i_" + souId + "_" + vType + "_" + (j + 1));
							vehicleTypes.addElement(vType);
							vHomeIds.addElement(souId);
							if (vHomeNames != null) {
								vHomeNames.addElement(souName);
							}
						}
					}
				}
			}
		}
		//Retrieve all the start times of the orders
		Vector startTimes = schedule.getAllAttrValues(schedule.getAttributeId(ldd.souTimeColIdx));
		if (startTimes == null || startTimes.size() < 1)
			return false;
		if (!(startTimes.elementAt(0) instanceof TimeMoment))
			return false;
		//Retrieve all the end times of the orders
		Vector endTimes = schedule.getAllAttrValues(schedule.getAttributeId(ldd.destTimeColIdx));
		if (endTimes == null || endTimes.size() < 1)
			return false;
		if (!(endTimes.elementAt(0) instanceof TimeMoment))
			return false;
		//Make a joint ordered list of all time moments
		int nStartTimes = startTimes.size(), nTimes = nStartTimes + endTimes.size();
		times = new Vector(nTimes, 10);
		for (int i = 0; i < nTimes; i++) {
			TimeMoment t = (TimeMoment) ((i < nStartTimes) ? startTimes.elementAt(i) : endTimes.elementAt(i - nStartTimes));
			boolean found = false;
			for (int j = 0; j < times.size() && !found; j++) {
				int cmp = t.compareTo((TimeMoment) times.elementAt(j));
				if (cmp > 0) {
					continue;
				}
				found = true;
				if (cmp < 0) {
					times.insertElementAt(t, j);
				}
			}
			if (!found) {
				times.addElement(t);
			}
		}
		vehicleActivity = new int[vehicleIds.size()][times.size() - 1];
		for (int i = 0; i < vehicleIds.size(); i++) {
			for (int j = 0; j < times.size() - 1; j++) {
				vehicleActivity[i][j] = vehicle_idle;
			}
		}
		loads = new Vector(10, 10);
		loads.addElement(makeEmptyLoadMatrix());
		if (vehicleTypes != null || vHomeIds != null || vHomeNames != null) {
			vehicleInfo = new DataTable();
			vehicleInfo.setContainerIdentifier("vehicles_" + schedule.getContainerIdentifier());
			vehicleInfo.setEntitySetIdentifier("vehicles");
			vehicleInfo.setName("Vehicles");
			if (vehicleTypeColIdx >= 0) {
				vehicleInfo.addAttribute(schedule.getAttributeName(vehicleTypeColIdx), "vehicleType", schedule.getAttributeType(vehicleTypeColIdx));
			}
			if (vehicleHomeIdColIdx >= 0) {
				vehicleInfo.addAttribute(schedule.getAttributeName(vehicleHomeIdColIdx), "vehicleHomeId", schedule.getAttributeType(vehicleHomeIdColIdx));
			}
			if (vehicleHomeNameColIdx >= 0) {
				vehicleInfo.addAttribute(schedule.getAttributeName(vehicleHomeNameColIdx), "vehicleHomeName", schedule.getAttributeType(vehicleHomeNameColIdx));
			}
			for (int i = 0; i < vehicleIds.size(); i++) {
				DataRecord vRec = new DataRecord((String) vehicleIds.elementAt(i));
				vehicleInfo.addDataRecord(vRec);
				if (vehicleTypes != null) {
					vRec.addAttrValue(vehicleTypes.elementAt(i));
				}
				if (vHomeIds != null) {
					vRec.addAttrValue(vHomeIds.elementAt(i));
				}
				if (vHomeNames != null) {
					vRec.addAttrValue(vHomeNames.elementAt(i));
				}
			}
		}
		int vNs[] = new int[schedule.getDataItemCount()]; //indexes of the vehicles
		//corresponding to the table records
		for (int i = 0; i < schedule.getDataItemCount(); i++) {
			DataRecord rec = schedule.getDataRecord(i);
			String vId = rec.getAttrValueAsString(vehicleIdColIdx);
			int vIdx = StringUtil.indexOfStringInVectorIgnoreCase(vId, vehicleIds);
			vNs[i] = vIdx;
			if (vIdx < 0) {
				continue; //this should not happen!
			}
			Object val = rec.getAttrValue(ldd.souTimeColIdx);
			if (val == null || !(val instanceof TimeMoment)) {
				continue;
			}
			TimeMoment t1 = (TimeMoment) val;
			val = rec.getAttrValue(ldd.destTimeColIdx);
			if (val == null || !(val instanceof TimeMoment)) {
				continue;
			}
			TimeMoment t2 = (TimeMoment) val;
			int t1Idx = times.indexOf(t1), t2Idx = times.indexOf(t2);
			if (t1Idx < 0 || t2Idx < 0 || t1Idx >= t2Idx) {
				continue; //this should not happen!
			}
			int nItems = (int) Math.round(rec.getNumericAttrValue(itemNumColIdx));
			if (nItems < 1) {
				for (int j = t1Idx; j < t2Idx; j++) {
					vehicleActivity[vIdx][j] = vehicle_empty_move;
				}
			} else {
				int load[][] = (int[][]) loads.elementAt(0);
				if (itemCatColIdx >= 0) {
					String itemCat = rec.getAttrValueAsString(itemCatColIdx);
					if (itemCat != null)
						if (itemCategories == null) {
							itemCategories = new Vector(10, 10);
							itemCategories.addElement(itemCat);
						} else {
							int idx = StringUtil.indexOfStringInVectorIgnoreCase(itemCat, itemCategories);
							if (idx < 0) {
								itemCategories.addElement(itemCat);
								idx = itemCategories.size() - 1;
							}
							while (idx >= loads.size()) {
								loads.addElement(makeEmptyLoadMatrix());
							}
							load = (int[][]) loads.elementAt(idx);
						}
				}
				for (int j = t1Idx; j < t2Idx; j++) {
					vehicleActivity[vIdx][j] = vehicle_loaded_move;
					load[vIdx][j] += nItems;
				}
			}
		}
		if (vehicleInfo != null) {
			int hIdCN = vehicleInfo.getAttrIndex("vehicleHomeId");
			vehicleVisitsAggregator = new DataAggregator();
			vehicleVisitsAggregator.setItemContainer(vehicleInfo);
			vehicleVisitsAggregator.setItemsName("vehicles");
			vehicleVisitsAggregator.setIncludeOrigItemsIds(true);
			vehicleVisitsAggregator.setIncludeOrigItemsNames(false);
			Vector records = new Vector(times.size(), 1);
			Vector tripEndTimes = new Vector(times.size(), 1), tripStartTimes = new Vector(times.size(), 1);
			for (int i = 0; i < vehicleInfo.getDataItemCount(); i++) {
/*
        boolean debugVehicle=false;
        if (vehicleIds.elementAt(i).equals("5")) {
          System.out.println("---- Vehicle "+vehicleIds.elementAt(i)+" -----");
          debugVehicle=true;
        }
*/
				String siteId = vehicleInfo.getAttrValueAsString(hIdCN, i);
				//extract all records from the schedule corresponding to this vehicle
				//and order them by time
				records.removeAllElements();
				tripStartTimes.removeAllElements();
				tripEndTimes.removeAllElements();
				TimeMoment tLeave = null;
				for (int j = 0; j < times.size() - 1 && tLeave == null; j++)
					if (vehicleActivity[i][j] != vehicle_idle) {
						tLeave = (TimeMoment) times.elementAt(j);
					}
				if (tLeave == null) {
					vehicleVisitsAggregator.addItem(vehicleInfo.getDataRecord(i), siteId);
					continue;
				}
				vehicleVisitsAggregator.addItem(vehicleInfo.getDataRecord(i), siteId, null, tLeave);
				for (int j = 0; j < vNs.length; j++)
					if (vNs[j] == i) {
						DataRecord rec = schedule.getDataRecord(j);
						Object val = rec.getAttrValue(ldd.destTimeColIdx);
						if (val == null || !(val instanceof TimeMoment)) {
							continue;
						}
						TimeMoment t = (TimeMoment) val, t0 = null;
						val = rec.getAttrValue(ldd.souTimeColIdx);
						if (val != null && (val instanceof TimeMoment)) {
							t0 = (TimeMoment) val;
						} else {
							t0 = tLeave;
						}
						int idx = -1;
						for (int k = 0; k < tripEndTimes.size() && idx < 0; k++)
							if (t.compareTo((TimeMoment) tripEndTimes.elementAt(k)) < 0) {
								idx = k;
							}
						if (idx < 0) {
							records.addElement(rec);
							tripEndTimes.addElement(t);
							tripStartTimes.addElement(t0);
						} else {
							records.insertElementAt(rec, idx);
							tripEndTimes.insertElementAt(t, idx);
							tripStartTimes.insertElementAt(t0, idx);
						}
					}
				for (int j = 0; j < records.size(); j++) {
					DataRecord rec = (DataRecord) records.elementAt(j);
					siteId = rec.getAttrValueAsString(ldd.destColIdx);
					TimeMoment tCome = (TimeMoment) tripEndTimes.elementAt(j);
					tLeave = null;
					if (j < records.size() - 1) {
						TimeMoment t = (TimeMoment) tripStartTimes.elementAt(j + 1);
						if (t != null && t.compareTo(tCome) >= 0) {
							tLeave = t;
						} else {
							tLeave = tCome;
						}
					}
					vehicleVisitsAggregator.addItem(vehicleInfo.getDataRecord(i), siteId, tCome, tLeave);
/*
          if (debugVehicle && siteId.equals("24")) {
            System.out.println("Site "+siteId+": from "+tCome+" till "+tLeave);
          }
*/
				}
			}
			vehicleVisitsAggregator.fillStaticAggregateData();
		}
		return true;
	}

	/**
	 * From the table vehicleInfo (if available) retrieves the types of all
	 * vehicles and fills in the vector vehicleTypes. Returns the reference
	 * to this vector or null if failed.
	 */
	public Vector getVehicleTypes() {
		if (vehicleIds == null)
			return null;
		if (vehicleTypes != null && vehicleTypes.size() == vehicleIds.size())
			return vehicleTypes;
		if (vehicleInfo == null || vehicleInfo.getDataItemCount() < 1)
			return null;
		vehicleTypes = new Vector(vehicleIds.size(), 50);
		for (int i = 0; i < vehicleIds.size(); i++) {
			vehicleTypes.addElement("0");
		}
		for (int i = 0; i < vehicleInfo.getDataItemCount(); i++) {
			DataRecord rec = vehicleInfo.getDataRecord(i);
			String vId = rec.getId();
			int idx = StringUtil.indexOfStringInVectorIgnoreCase(vId, vehicleIds);
			if (idx < 0) {
				continue;
			}
			String vType = rec.getAttrValueAsString("vehicleType");
			vehicleTypes.setElementAt(vType, idx);
		}
		return vehicleTypes;
	}

	/**
	 * Returns an instance of VehicleActivityData with the identifiers of all
	 * the vehicles and the activity matrix for all the vehicles.
	 */
	public VehicleActivityData getFullActivityData() {
		if (vehicleIds == null || vehicleActivity == null)
			return null;
		VehicleActivityData vad = new VehicleActivityData();
		vad.vehicleIds = vehicleIds;
		vad.vehicleActivity = vehicleActivity;
		vad.statesBefore = vad.statesAfter = new int[vad.vehicleIds.size()];
		for (int i = 0; i < vad.vehicleIds.size(); i++) {
			vad.statesBefore[i] = vehicle_idle;
		}
		return vad;
	}

	/**
	 * Returns the identifiers of the vehicles suitable for the given
	 * item category
	 */
	public Vector getSuitableVehicleIds(String catName) {
		if (vehicleIds == null || vehicleActivity == null)
			return null;
		if (catName == null)
			return vehicleIds;
		Vector suitable = new Vector(vehicleIds.size(), 1);
		for (int i = 0; i < vehicleCap.getDataItemCount(); i++) {
			DataRecord rec = vehicleCap.getDataRecord(i);
			String iType = rec.getAttrValueAsString("item_type");
			if (iType != null && !iType.equalsIgnoreCase(catName)) {
				continue;
			}
			String vType = rec.getAttrValueAsString("vehicle_type");
			double cap = rec.getNumericAttrValue("capacity");
			if (Double.isNaN(cap)) {
				continue;
			}
			int icap = (int) Math.round(cap);
			if (icap < 1) {
				continue;
			}
			for (int j = 0; j < vehicleTypes.size(); j++)
				if (vType.equalsIgnoreCase((String) vehicleTypes.elementAt(j))) {
					suitable.addElement(vehicleIds.elementAt(j));
				}
		}
		if (suitable.size() < 1)
			return null;
		return suitable;
	}

	/**
	 * From the overall vehicle activity matrix, selects data about vehicles
	 * suitable for the specified item category. Returns an instance of
	 * VehicleActivityData containing the identifiers of the suitable vehicles and
	 * a reduced activity matrix. If the argument is null or the information about
	 * vehicle suitability for the item categories (table vehicleCap) is not
	 * available, returns the full list of vehicle identifiers and the full
	 * activity matrix.
	 */
	public VehicleActivityData getSuitableVehicleActivities(String catName) {
		if (vehicleIds == null || vehicleActivity == null)
			return null;
		if (catName == null)
			return getFullActivityData();
		if (vehicleCap == null || vehicleCap.getDataItemCount() < 1)
			return getFullActivityData();
		if (vehicleTypes == null) {
			getVehicleTypes();
			if (vehicleTypes == null)
				return getFullActivityData();
		}
		boolean suit[] = new boolean[vehicleIds.size()];
		for (int i = 0; i < vehicleIds.size(); i++) {
			suit[i] = false;
		}
		int nSuitable = 0;
		for (int i = 0; i < vehicleCap.getDataItemCount(); i++) {
			DataRecord rec = vehicleCap.getDataRecord(i);
			String iType = rec.getAttrValueAsString("item_type");
			if (iType != null && !iType.equalsIgnoreCase(catName)) {
				continue;
			}
			String vType = rec.getAttrValueAsString("vehicle_type");
			double cap = rec.getNumericAttrValue("capacity");
			if (Double.isNaN(cap)) {
				continue;
			}
			int icap = (int) Math.round(cap);
			if (icap < 1) {
				continue;
			}
			for (int j = 0; j < vehicleTypes.size(); j++)
				if (vType.equalsIgnoreCase((String) vehicleTypes.elementAt(j))) {
					suit[j] = true;
					++nSuitable;
				}
		}
		if (nSuitable < 1)
			return null;
		VehicleActivityData vad = new VehicleActivityData();
		vad.itemCat = catName;
		vad.vehicleIds = new Vector(nSuitable, 5);
		for (int i = 0; i < vehicleIds.size(); i++)
			if (suit[i]) {
				vad.vehicleIds.addElement(vehicleIds.elementAt(i));
			}
		vad.vehicleActivity = new int[vad.vehicleIds.size()][times.size() - 1];
		int k = 0;
		for (int i = 0; i < vehicleIds.size(); i++)
			if (suit[i]) {
				vad.vehicleActivity[k++] = vehicleActivity[i];
			}
		vad.statesBefore = vad.statesAfter = new int[vad.vehicleIds.size()];
		for (int i = 0; i < vad.vehicleIds.size(); i++) {
			vad.statesBefore[i] = vehicle_idle;
		}
		return vad;
	}

	/**
	 * Uses already retrieved data about vehicle loading by item categories
	 * (i.e. must be called after method countVehicles(...)).
	 * In the result, fills the vector capUses, which contains for each item
	 * category an instance of VehicleLoadData with data about the use of
	 * capacities in the vehicles suitable for this item category or
	 * actually used for this item category.
	 * @return true if successfully computed
	 */
	public boolean computeCapacityUse() {
		if (vehicleCap == null || vehicleCap.getDataItemCount() < 1 || vehicleIds == null || times == null || vehicleActivity == null || loads == null)
			return false;
		if (vehicleTypes == null) {
			getVehicleTypes();
			if (vehicleTypes == null)
				return false;
		}
		int nCat = 0;
		if (itemCategories != null) {
			nCat = itemCategories.size();
		}
		int capacities[][] = new int[nCat + 1][vehicleIds.size()];
		for (int i = 0; i < nCat + 1; i++) {
			for (int j = 0; j < vehicleIds.size(); j++) {
				capacities[i][j] = 0;
			}
		}
		for (int i = 0; i < vehicleCap.getDataItemCount(); i++) {
			DataRecord rec = vehicleCap.getDataRecord(i);
			String vType = rec.getAttrValueAsString("vehicle_type");
			String iType = rec.getAttrValueAsString("item_type");
			int catIdx = -1;
			if (iType != null) {
				catIdx = StringUtil.indexOfStringInVectorIgnoreCase(iType, itemCategories);
			}
			if (iType == null || catIdx >= 0) {
				double cap = rec.getNumericAttrValue("capacity");
				if (!Double.isNaN(cap) && cap > 0) {
					int icap = (int) Math.round(cap);
					for (int j = 0; j < vehicleTypes.size(); j++)
						if (vType.equalsIgnoreCase((String) vehicleTypes.elementAt(j))) {
							if (catIdx >= 0) {
								capacities[catIdx + 1][j] = icap; //capacity for category catIdx
							}
							if (icap > capacities[0][j]) {
								capacities[0][j] = icap; //maximum capacity
							}
						}
				}
			}
		}
		capUseAllCat = new VehicleLoadData();
		capUseAllCat.vehicleIds = new Vector(vehicleIds.size() * 2, 10);
		for (int i = 0; i < vehicleIds.size(); i++) {
			for (int k = 0; k < 2; k++) {
				capUseAllCat.vehicleIds.addElement(vehicleIds.elementAt(i));
			}
		}
		capUseAllCat.capCats = makeEmptyLoadMatrix(capUseAllCat.vehicleIds);
		for (int j = 0; j < times.size() - 1; j++) {
			for (int i = 0; i < capUseAllCat.vehicleIds.size(); i += 2) {
				capUseAllCat.capCats[i][j] = VehicleLoadData.capacity_idle;
				capUseAllCat.capCats[i + 1][j] = VehicleLoadData.capacity_free;
			}
		}
		capUseAllCat.capSizes = makeEmptyLoadMatrix(capUseAllCat.vehicleIds);
		capUseAllCat.capCatsBefore = capUseAllCat.capCatsAfter = makeIdleCapacityTypesArray(capUseAllCat.vehicleIds);
		capUseAllCat.capSizesBefore = capUseAllCat.capSizesAfter = makeIdleCapacitySizesArray(capUseAllCat.vehicleIds, capacities[0]);
		for (int i = 0; i < vehicleIds.size(); i++) {
			for (int j = 0; j < times.size() - 1; j++)
				if (vehicleActivity[i][j] == vehicle_idle) {
					capUseAllCat.capSizes[2 * i][j] += capacities[0][i];
				} else if (vehicleActivity[i][j] == vehicle_empty_move) {
					continue;
				} else {
					capUseAllCat.capCats[2 * i][j] = VehicleLoadData.capacity_used;
					for (int l = 0; l < loads.size(); l++) {
						int load[][] = (int[][]) loads.elementAt(l);
						if (load[i][j] > 0) {
							int num = load[i][j];
							if (capacities[l + 1][i] > 0 && capacities[l + 1][i] < capacities[0][i]) {
								num = Math.round(num * 1.0f * capacities[0][i] / capacities[l + 1][i]);
							}
							capUseAllCat.capSizes[2 * i][j] += num;
						}
					}
					if (capUseAllCat.capSizes[2 * i][j] < capacities[0][i]) {
						capUseAllCat.capSizes[2 * i + 1][j] = capacities[0][i] - capUseAllCat.capSizes[2 * i][j];
					} else if (capUseAllCat.capSizes[2 * i][j] > capacities[0][i]) {
						capUseAllCat.capCats[2 * i][j] = VehicleLoadData.capacity_overload;
					}
				}
		}

		capUses = new Vector(loads.size(), 10);
		for (int l = 0; l < loads.size(); l++) {
			int nSuitable = 0;
			int cap[] = new int[vehicleIds.size()];
			for (int i = 0; i < vehicleIds.size(); i++) {
				cap[i] = capacities[l + 1][i];
				if (cap[i] > 0) {
					++nSuitable;
				}
			}
			int load[][] = (int[][]) loads.elementAt(l);
			if (nSuitable < capacities.length) {
				//check if any unsuitable vehicles are actually used for this item category
				for (int i = 0; i < capacities.length; i++)
					if (cap[i] == 0) {
						for (int t = 0; t < times.size() - 1; t++)
							if (load[i][t] > cap[i]) {
								cap[i] = load[i][t];
							}
						if (cap[i] > 0) {
							++nSuitable;
						}
					}
			}
			if (nSuitable < 1) {
				capUses.addElement(null);
				continue;
			}
			nSuitable = 0;
			for (int i = 0; i < capacities.length; i++)
				if (cap[i] > 0) {
					++nSuitable;
				}
			VehicleLoadData vld = new VehicleLoadData();
			capUses.addElement(vld);
			vld.vehicleIds = new Vector(nSuitable * 2, 10);
			for (int i = 0; i < vehicleIds.size(); i++)
				if (cap[i] > 0) {
					for (int k = 0; k < 2; k++) {
						vld.vehicleIds.addElement(vehicleIds.elementAt(i));
					}
				}
			vld.capCats = makeEmptyLoadMatrix(vld.vehicleIds);
			for (int j = 0; j < times.size() - 1; j++) {
				for (int i = 0; i < vld.vehicleIds.size(); i += 2) {
					vld.capCats[i][j] = VehicleLoadData.capacity_idle;
					vld.capCats[i + 1][j] = VehicleLoadData.capacity_free;
				}
			}
			vld.capSizes = makeEmptyLoadMatrix(vld.vehicleIds);
			vld.capCatsBefore = vld.capCatsAfter = makeIdleCapacityTypesArray(vld.vehicleIds);
			vld.capSizesBefore = vld.capSizesAfter = makeIdleCapacitySizesArray(vld.vehicleIds, cap);
			for (int i = 0; i < vld.vehicleIds.size(); i += 2) {
				String vId = (String) vld.vehicleIds.elementAt(i);
				int vIdx = StringUtil.indexOfStringInVectorIgnoreCase(vId, vehicleIds);
				for (int j = 0; j < times.size() - 1; j++)
					if (vehicleActivity[vIdx][j] == vehicle_idle) {
						vld.capSizes[i][j] += cap[vIdx];
					} else if (vehicleActivity[vIdx][j] == vehicle_empty_move) {
						continue;
					} else {
						vld.capCats[i][j] = VehicleLoadData.capacity_used;
						if (load[vIdx][j] > 0) {
							vld.capSizes[i][j] += load[vIdx][j];
							//check if items of other categories are transported together with these items
							int numOther = 0;
							for (int k = 0; k < loads.size(); k++)
								if (k != l) {
									int otherLoad[][] = (int[][]) loads.elementAt(k);
									if (otherLoad[vIdx][j] > 0) {
										int num = otherLoad[vIdx][j];
										if (capacities[k + 1][vIdx] > 0 && capacities[k + 1][vIdx] != cap[vIdx]) {
											num = Math.round(num * 1.0f * cap[vIdx] / capacities[k + 1][vIdx]);
										}
										numOther += num;
									}
								}
							vld.capSizes[i][j] += numOther;
							if (vld.capSizes[i][j] < cap[vIdx]) {
								vld.capSizes[i + 1][j] += (cap[vIdx] - vld.capSizes[i][j]);
							} else if (vld.capSizes[i][j] > cap[vIdx]) {
								vld.capCats[i][j] = VehicleLoadData.capacity_overload;
							}
						}
					}
			}
		}
		return true;
	}

	private int[][] makeEmptyLoadMatrix() {
		return makeEmptyLoadMatrix(vehicleIds);
	}

	private int[][] makeEmptyLoadMatrix(Vector ids) {
		if (times == null || times.size() < 2 || ids == null || ids.size() < 1)
			return null;
		int load[][] = new int[ids.size()][times.size() - 1];
		for (int i = 0; i < ids.size(); i++) {
			for (int j = 0; j < times.size() - 1; j++) {
				load[i][j] = 0;
			}
		}
		return load;
	}

	private int[] makeIdleCapacitySizesArray(Vector ids, int capacities[]) {
		if (ids == null || ids.size() < 1)
			return null;
		int load[] = new int[ids.size()];
		for (int i = 0; i < ids.size(); i += 2) {
			int idx = StringUtil.indexOfStringInVectorIgnoreCase((String) ids.elementAt(i), vehicleIds);
			if (idx < 0) {
				continue;
			}
			load[i] = capacities[idx];
			load[i + 1] = 0;
		}
		return load;
	}

	private int[] makeIdleCapacityTypesArray(Vector ids) {
		if (ids == null || ids.size() < 1)
			return null;
		int capCategories[] = new int[ids.size()];
		for (int i = 0; i < ids.size(); i += 2) {
			capCategories[i] = VehicleLoadData.capacity_idle;
			capCategories[i + 1] = VehicleLoadData.capacity_free;
		}
		return capCategories;
	}

	protected DataTable tItemData = null;

	public boolean hasItemData() {
		return tItemData != null && tItemData.getDataItemCount() > 0;
	}

	public void setItemData(DataTable tItemData) {
		this.tItemData = tItemData;
		System.out.println("* vehicleCounter received itemtable!");
	}

	public Color[] getItemColors() {
		return new Color[] { Color.pink, Color.red.darker(), new Color(179, 205, 227), new Color(49, 163, 84), new Color(221, 28, 119), new Color(122, 1, 119) };
	}

	public String[] getItemComments() {
		return new String[] { ItemCollector.itemsInSource, ItemCollector.itemsDelayInSource, ItemCollector.itemsOnTheWay, ItemCollector.itemsInDestination, ItemCollector.itemsInUnsuitableVehicle, ItemCollector.itemsInUnsuitableDestination };
	}

	/**
	 * Returns the identifiers of the item (people) groups 
	 */
	public Vector getItemIDs(String catName) {
		if (!hasItemData())
			return null;
		int icn = tItemData.getAttrIndex("class_name");
		Vector v = new Vector(tItemData.getDataItemCount(), 100);
		for (int i = 0; i < tItemData.getDataItemCount(); i++) {
			String st = tItemData.getAttrValueAsString(icn, i);
			if (catName == null || st != null && st.equals(catName)) {
				v.addElement(tItemData.getDataItem(i).getId());
			}
		}
		return v;
	}

	/**
	 * Returns the states of the item (people) groups at different time
	 * moments in the course of the schedule execution
	 */
	public int[][] getItemStates(String catName) {
		if (!hasItemData())
			return null;
		Vector v = getItemIDs(catName);
		int s[][] = new int[v.size()][];
		int icn = tItemData.getAttrIndex("class_name");
		int idx[] = new int[3];
		idx[0] = tItemData.getAttrIndex("dep_time");
		idx[1] = tItemData.getAttrIndex("arr_time");
		idx[2] = tItemData.getAttrIndex("max_dep_time");
		int n = -1;
		for (int i = 0; i < tItemData.getDataItemCount(); i++) {
			String st = tItemData.getAttrValueAsString(icn, i);
			if (catName == null || (st != null && st.equals(catName))) {
				n++;
				s[n] = new int[times.size() - 1];
				TimeMoment tm[] = new TimeMoment[3];
				for (int tidx = 0; tidx < 3; tidx++)
					if (idx[tidx] >= 0) {
						Object o = tItemData.getAttrValue(idx[tidx], i);
						if (o instanceof TimeMoment) {
							tm[tidx] = (TimeMoment) o;
						}
					}
				for (int t = 0; t < s[n].length; t++) {
					TimeMoment tmt = (TimeMoment) times.elementAt(t);
					if (tm[1] != null && tmt.compareTo(tm[1]) >= 0) {
						s[n][t] = 3; // arrived
					} else if (tm[0] != null && tmt.compareTo(tm[0]) >= 0) {
						s[n][t] = 2; // on the way
					} else if (tm[2] != null && tmt.compareTo(tm[2]) >= 0) {
						s[n][t] = 1; // at source (delayed)
					} else {
						s[n][t] = 0; // at source
					}
				}
			}
		}
		return s;
	}

	/**
	 * Returns the sizes of the groups of items (people) at different time moments.
	 * Although the sizes of the groups do not change over time, this method
	 * is needed for compatibility with the data formats for TimeSegmentedBarCanvas.
	 */
	public int[][] getItemCountsByTime(String catName) {
		if (!hasItemData())
			return null;
		Vector v = getItemIDs(catName);
		int c[][] = new int[v.size()][];
		int icn = tItemData.getAttrIndex("class_name");
		int idx = tItemData.getAttrIndex("number");
		int n = -1;
		for (int i = 0; i < tItemData.getDataItemCount(); i++) {
			String st = tItemData.getAttrValueAsString(icn, i);
			if (catName == null || st != null && st.equals(catName)) {
				n++;
				c[n] = new int[times.size() - 1];
				int iv = (int) tItemData.getNumericAttrValue(idx, i);
				for (int t = 0; t < c[n].length; t++) {
					c[n][t] = iv;
				}
			}
		}
		return c;
	}

	/**
	 * Returns the states of the item (people) groups before the
	 * beginning of the schedule execution
	 */
	public int[] getItemStatesBefore(String catName) {
		if (!hasItemData())
			return null;
		Vector v = getItemIDs(catName);
		int s[] = new int[v.size()];
		for (int i = 0; i < s.length; i++) {
			s[i] = 0; // at source
		}
		return s;
	}

	/**
	 * Returns the states of the item (people) groups after the
	 * end of the schedule execution
	 */
	public int[] getItemStatesAfter(String catName) {
		if (!hasItemData())
			return null;
		Vector v = getItemIDs(catName);
		int s[] = new int[v.size()];
		int icn = tItemData.getAttrIndex("class_name");
		int idx[] = new int[3];
		idx[0] = tItemData.getAttrIndex("dep_time");
		idx[1] = tItemData.getAttrIndex("arr_time");
		idx[2] = tItemData.getAttrIndex("max_dep_time");
		int n = -1;
		for (int i = 0; i < tItemData.getDataItemCount(); i++) {
			String st = tItemData.getAttrValueAsString(icn, i);
			if (catName == null || st != null && st.equals(catName)) {
				n++;
				TimeMoment tm[] = new TimeMoment[3];
				for (int tidx = 0; tidx < 3; tidx++)
					if (idx[tidx] >= 0) {
						Object o = tItemData.getAttrValue(idx[tidx], i);
						if (o instanceof TimeMoment) {
							tm[tidx] = (TimeMoment) o;
						}
					}
				TimeMoment tmt = (TimeMoment) times.elementAt(times.size() - 1);
				if (tm[1] != null && tmt.compareTo(tm[1]) >= 0) {
					s[n] = 3; // arrived
				} else if (tm[0] != null && tmt.compareTo(tm[0]) >= 0) {
					s[n] = 2; // on the way
				} else if (tm[2] != null && tmt.compareTo(tm[2]) >= 0) {
					s[n] = 1; // at source (delayed)
				} else {
					s[n] = 0; // at source
				}
			}
		}
		return s;
	}

	/**
	 * Returns the sizes of the groups of items (people), which do not
	 * depend on time
	 */
	public int[] getItemCounts(String catName) {
		if (!hasItemData())
			return null;
		Vector v = getItemIDs(catName);
		int c[] = new int[v.size()];
		int icn = tItemData.getAttrIndex("class_name");
		int idx = tItemData.getAttrIndex("number");
		int n = -1;
		for (int i = 0; i < tItemData.getDataItemCount(); i++) {
			String st = tItemData.getAttrValueAsString(icn, i);
			if (catName == null || st != null && st.equals(catName)) {
				n++;
				int iv = (int) tItemData.getNumericAttrValue(idx, i);
				c[n] = iv;
			}
		}
		return c;
	}

	public TimeMoment getStartTime() {
		if (times == null || times.size() < 1)
			return null;
		return (TimeMoment) times.elementAt(0);
	}

}
