package spade.analysis.tools.schedule;

import java.util.Vector;

import spade.lib.util.FloatArray;
import spade.lib.util.StringUtil;
import spade.vis.database.DataTable;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: 18-Oct-2007
 * Time: 16:00:45
 * Keeps information about classes of vehicles used for transportation.
 */
public class VehicleTypesInfo {
	/**
	 * Contains identifiers of known vehicle types
	 */
	public Vector vehicleClassIds = null;
	/**
	 * Contains names or textual descriptions of the known vehicle types in the
	 * same order as in vehicleClassIds
	 */
	public Vector vehicleClassNames = null;
	/**
	 * Speed factors for the vehicle classes
	 */
	public FloatArray speedFactors = null;
	/**
	 * For each vehicle type contains the item categories it is suitable for,
	 * capacities for these item categories and, possibly, loading times per item.
	 * Structure:
	 * vehicle type,item type,capacity,loading time
	 * There may be several records for one vehicle type if it is suitable for
	 * more than one item categories.
	 */
	public DataTable vehicleCap = null;

	/**
	 * Removes the "virtual" vehicle type, if occurs among the types
	 */
	public void removeVirtualType() {
		int idx = -1;
		if (vehicleClassIds != null) {
			idx = vehicleClassIds.indexOf("0");
		}
		if (idx < 0 && vehicleClassNames != null) {
			idx = StringUtil.indexOfStringInVectorIgnoreCase("virtual", vehicleClassNames);
			if (idx < 0) {
				idx = vehicleClassNames.indexOf("0");
			}
		}
		if (idx >= 0) {
			if (vehicleClassIds != null && vehicleClassIds.size() > idx) {
				vehicleClassIds.removeElementAt(idx);
			}
			if (vehicleClassNames != null && vehicleClassNames.size() > idx) {
				vehicleClassNames.removeElementAt(idx);
			}
			if (speedFactors != null && speedFactors.size() > idx) {
				speedFactors.removeElementAt(idx);
			}
		}
	}

	public int getNofVehicleClasses() {
		if (vehicleClassIds != null && vehicleClassIds.size() > 0)
			return vehicleClassIds.size();
		if (vehicleClassNames != null && vehicleClassNames.size() > 0)
			return vehicleClassNames.size();
		return 0;
	}

	/**
	 * For the given vehicle class identifier (code), returns its index.
	 */
	public int getVehicleClassIndex(String code) {
		if (code == null)
			return -1;
		int idx = -1;
		if (vehicleClassIds != null) {
			idx = StringUtil.indexOfStringInVectorIgnoreCase(code, vehicleClassIds);
		}
		if (idx < 0 && vehicleClassNames != null) {
			idx = StringUtil.indexOfStringInVectorIgnoreCase(code, vehicleClassNames);
		}
		return idx;
	}

	/**
	 * Returns the class identifier (code) with the given index
	 */
	public String getVehicleClassId(int idx) {
		if (idx < 0)
			return null;
		if (vehicleClassIds != null && idx < vehicleClassIds.size())
			return (String) vehicleClassIds.elementAt(idx);
		if (vehicleClassNames != null && idx < vehicleClassNames.size())
			return (String) vehicleClassNames.elementAt(idx);
		return null;
	}

	/**
	 * Returns the class name with the given index
	 */
	public String getVehicleClassName(int idx) {
		if (idx < 0)
			return null;
		if (vehicleClassNames != null && idx < vehicleClassNames.size())
			return (String) vehicleClassNames.elementAt(idx);
		if (vehicleClassIds != null && idx < vehicleClassIds.size())
			return (String) vehicleClassIds.elementAt(idx);
		return null;
	}

	/**
	 * For the given vehicle class identifier (code), returns its name.
	 */
	public String getVehicleClassName(String code) {
		return getVehicleClassName(getVehicleClassIndex(code));
	}
}
