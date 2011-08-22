package spade.analysis.tools.schedule;

import java.awt.Color;
import java.util.Vector;

import spade.lib.util.IntArray;
import spade.lib.util.StringUtil;
import spade.time.TimeMoment;
import spade.time.TimeReference;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DrawingParameters;
import spade.vis.geometry.Geometry;
import spade.vis.spec.DataSourceSpec;
import spade.vis.spec.LinkDataDescription;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 16-Mar-2007
 * Time: 16:31:53
 * Computes the dynamics of using the capacities in the destination sites.
 */
public class DestinationUseCounter {
	/**
	 * The list of the identifiers of the sites
	 */
	public Vector siteIds = null;
	/**
	 * The list of the names of the sites
	 */
	public Vector siteNames = null;
	/**
	 * Contains the names of the categories of the transported items. Does not
	 * contain "LEER" or "EMPTY"!
	 */
	public Vector itemCategories = null;
	/**
	 * Contains the codes of the categories of the transported items, for example:
	 * 10 - general people or children
	 * 12 - infants
	 * 20 - invalids who can seat
	 * 21 - invalids who cannot seat
	 * 22 - disabled people using wheelchairs
	 * 23 - critically sick or injured people
	 * 30 - prisoners
	 */
	protected Vector itemCatCodes = null;
	/**
	 * Original capacities of the destinations for the item categories.
	 * The rows (1st dimension) correspond to the sites and the columns (2nd
	 * dimension) - to the categories.
	 */
	public int[][] siteCapacities = null;
	/**
	 * Ordered list of the time moments. The elements are instances of TimeMoment
	 * and indicate the beginnings of the time intervals. The last element in the
	 * list is the end of the last time interval.
	 */
	public Vector times = null;
	/**
	 * For each item category, a matrix of the numbers of items
	 * delivered to each destination by time (matrix columns) for all
	 * destinations (matrix rows).
	 * The first matrix (index==0) corresponds to all categories taken together.
	 */
	public Vector deliveries = null;
	/**
	 * Contains data about the available destinations and their original capacities
	 */
	public DataTable destCap = null;

	/**
	 * Sets the list of item categories (should not include "LEER" or "EMPTY")
	 */
	public void setItemCategories(Vector itemCat) {
		itemCategories = itemCat;
	}

	/**
	 * Sets the list of codes of the categories
	 */
	public void setItemCategoriesCodes(Vector itemCatCodes) {
		this.itemCatCodes = itemCatCodes;
	}

	/**
	 * Sets a reference to a table with data about the available destinations and
	 * their original capacities
	 */
	public void setDestinationCapacitiesTable(DataTable destCap) {
		this.destCap = destCap;
	}

	/**
	 * Analyses the input table with the transportation schedule and constructs
	 * the output matrices and list of time moments. Returns true if successful
	 * and false in case of inappropriate input data.
	 */
	public boolean countDestinationUse(DataTable schedule, LinkDataDescription ldd, int itemNumColIdx, int itemCatColIdx, TimeMoment startTime) {
		if (schedule == null || schedule.getDataItemCount() < 1)
			return false;
		if (destCap == null || destCap.getDataItemCount() < 1 || destCap.getDataSource() == null)
			return false;
		if (itemNumColIdx < 0)
			return false;
		if (ldd == null || ldd.destColIdx < 0 || ldd.destTimeColIdx < 0)
			return false;
		DataSourceSpec spec = (DataSourceSpec) destCap.getDataSource();
		if (spec.extraInfo == null)
			return false; //no metadata
		String attrName = (String) spec.extraInfo.get("CAPACITY_FIELD_NAME");
		if (attrName == null) {
			attrName = "capacity";
		}
		int capColN = destCap.findAttrByName(attrName);
		if (capColN < 0)
			return false;
		//retrieve the site identifiers
		siteIds = new Vector(destCap.getDataItemCount(), 10);
		siteNames = new Vector(destCap.getDataItemCount(), 10);
		for (int i = 0; i < destCap.getDataItemCount(); i++) {
			String id = destCap.getDataItemId(i);
			if (!StringUtil.isStringInVectorIgnoreCase(id, siteIds)) {
				siteIds.addElement(id);
				siteNames.addElement(destCap.getDataItemName(i));
			}
		}
		if (siteIds.size() < 1)
			return false;
		boolean gotNames = false;
		for (int i = 0; i < siteNames.size() && !gotNames; i++) {
			gotNames = siteNames.elementAt(i) != null;
		}
		if (!gotNames) {
			siteNames = null;
		}
		int nCat = 0;
		int destItemCatColIdx = -1;
		if (itemCategories != null && itemCategories.size() > 0) {
			attrName = (String) spec.extraInfo.get("ITEM_CLASS_FIELD_NAME");
			if (attrName == null) {
				attrName = "Item class name";
			}
			destItemCatColIdx = destCap.findAttrByName(attrName);
			if (destItemCatColIdx >= 0) {
				nCat = itemCategories.size();
			}
		}
		siteCapacities = new int[siteIds.size()][nCat + 1];
		for (int i = 0; i < siteIds.size(); i++) {
			for (int j = 0; j < nCat + 1; j++) {
				siteCapacities[i][j] = 0;
			}
		}
		for (int i = 0; i < destCap.getDataItemCount(); i++) {
			DataRecord rec = destCap.getDataRecord(i);
			double fval = rec.getNumericAttrValue(capColN);
			if (Double.isNaN(fval)) {
				continue;
			}
			int cap = (int) Math.round(fval);
			if (cap < 1) {
				continue;
			}
			int catIdx = 0;
			if (destItemCatColIdx >= 0) {
				String iCat = rec.getAttrValueAsString(destItemCatColIdx);
				if (iCat != null) {
					catIdx = StringUtil.indexOfStringInVectorIgnoreCase(iCat, itemCategories);
				}
				if (catIdx < 0) {
					continue;
				}
			}
			int siteIdx = StringUtil.indexOfStringInVectorIgnoreCase(rec.getId(), siteIds);
			siteCapacities[siteIdx][catIdx + 1] = cap;
			siteCapacities[siteIdx][0] += cap; //total capacity for all items
		}
		//retrieve the end times of the orders and arrange then chronologically
		times = new Vector(100, 100);
		for (int i = 0; i < schedule.getDataItemCount(); i++) {
			DataRecord rec = schedule.getDataRecord(i);
			double fval = rec.getNumericAttrValue(itemNumColIdx);
			if (Double.isNaN(fval)) {
				continue;
			}
			int num = (int) Math.round(fval);
			if (num < 1) {
				continue;
			}
			Object val = rec.getAttrValue(ldd.destTimeColIdx);
			if (val == null || !(val instanceof TimeMoment)) {
				continue;
			}
			TimeMoment t = (TimeMoment) val;
			int idx = -1;
			boolean found = false;
			for (int j = 0; j < times.size() && !found && idx < 0; j++) {
				int cmp = t.compareTo((TimeMoment) times.elementAt(j));
				found = cmp == 0;
				if (!found && cmp < 0) {
					idx = j;
				}
			}
			if (found) {
				continue;
			}
			if (idx >= 0) {
				times.insertElementAt(t, idx);
			} else {
				times.addElement(t);
			}
		}
		if (times.size() < 1)
			return false;
		if (startTime != null && startTime.compareTo((TimeMoment) times.elementAt(0)) < 0) {
			times.insertElementAt(startTime, 0);
		}
		deliveries = new Vector(nCat + 1, 1);
		for (int i = 0; i < nCat + 1; i++) {
			deliveries.addElement(makeEmptyMatrix(siteIds));
		}
		int delivAll[][] = (int[][]) deliveries.elementAt(0);
		for (int i = 0; i < schedule.getDataItemCount(); i++) {
			DataRecord rec = schedule.getDataRecord(i);
			double fval = rec.getNumericAttrValue(itemNumColIdx);
			if (Double.isNaN(fval)) {
				continue;
			}
			int num = (int) Math.round(fval);
			if (num < 1) {
				continue;
			}
			String siteId = rec.getAttrValueAsString(ldd.destColIdx);
			if (siteId == null) {
				continue;
			}
			int siteIdx = StringUtil.indexOfStringInVectorIgnoreCase(siteId, siteIds);
			if (siteIdx < 0) {
				continue;
			}
			Object val = rec.getAttrValue(ldd.destTimeColIdx);
			if (val == null || !(val instanceof TimeMoment)) {
				continue;
			}
			TimeMoment t = (TimeMoment) val;
			int tIdx = times.indexOf(t);
			int catIdx = -1;
			if (itemCatColIdx >= 0) {
				String iCat = rec.getAttrValueAsString(itemCatColIdx);
				if (iCat != null) {
					catIdx = StringUtil.indexOfStringInVectorIgnoreCase(iCat, itemCategories);
				}
				if (catIdx < 0) {
					continue;
				}
			}
			for (int j = tIdx; j < times.size(); j++) {
				delivAll[siteIdx][j] += num;
			}
			if (catIdx >= 0) {
				int deliv[][] = (int[][]) deliveries.elementAt(catIdx + 1);
				for (int j = tIdx; j < times.size(); j++) {
					deliv[siteIdx][j] += num;
				}
			}
		}
		return true;
	}

	/**
	 * Returns capacity use data for the given item category. If the category
	 * is null, returns the data for all categories in total.
	 */
	public CapacityUseData getCapacityUseData(String itemCat) {
		if (siteIds == null || times == null || deliveries == null)
			return null;
		int catIdx = -1;
		int deliv[][] = null;
		CapacityUseData cd = new CapacityUseData();
		cd.times = times;
		cd.ids = new Vector(siteIds.size() * 2, 5);
		if (itemCat == null || itemCategories == null) {
			for (int i = 0; i < siteIds.size(); i++) {
				for (int k = 0; k < 2; k++) {
					cd.ids.addElement(siteIds.elementAt(i));
				}
			}
			deliv = (int[][]) deliveries.elementAt(0);
		} else {
			catIdx = StringUtil.indexOfStringInVectorIgnoreCase(itemCat, itemCategories);
			if (catIdx < 0)
				return null;
			deliv = (int[][]) deliveries.elementAt(catIdx + 1);
			cd.ids = new Vector(siteIds.size(), 1);
			for (int i = 0; i < siteIds.size(); i++) {
				if (siteCapacities[i][catIdx + 1] > 0) {
					for (int k = 0; k < 2; k++) {
						cd.ids.addElement(siteIds.elementAt(i));
					}
				} else {
					boolean used = false;
					for (int j = 0; j < times.size() && !used; j++) {
						used = deliv[i][j] > 0;
					}
					if (used) {
						for (int k = 0; k < 2; k++) {
							cd.ids.addElement(siteIds.elementAt(i));
						}
					}
				}
			}
			if (cd.ids.size() < 1)
				return null;
		}
		cd.states = makeEmptyMatrix(cd.ids);
		cd.fills = makeEmptyMatrix(cd.ids);
		cd.statesBefore = makeEmptyArray(cd.ids);
		cd.fillsBefore = makeEmptyArray(cd.ids);
		cd.statesAfter = makeEmptyArray(cd.ids);
		cd.fillsAfter = makeEmptyArray(cd.ids);
		for (int i = 0; i < cd.ids.size(); i += 2) {
			int idx = siteIds.indexOf(cd.ids.elementAt(i));
			int cap = siteCapacities[idx][catIdx + 1];
			cd.statesBefore[i] = CapacityUseData.capacity_fully_unused;
			cd.fillsBefore[i] = cap;
			for (int j = 0; j < times.size(); j++)
				if (deliv[idx][j] < 1) {
					cd.states[i][j] = CapacityUseData.capacity_fully_unused;
					cd.fills[i][j] = cap;
				} else {
					cd.fills[i][j] = deliv[idx][j];
					if (deliv[idx][j] > cap) {
						cd.states[i][j] = CapacityUseData.capacity_overloaded;
					} else if (deliv[idx][j] == cap) {
						cd.states[i][j] = CapacityUseData.capacity_full;
					} else {
						cd.states[i][j] = CapacityUseData.capacity_partly_filled;
						cd.states[i + 1][j] = CapacityUseData.capacity_partly_unused;
						cd.fills[i + 1][j] = cap - cd.fills[i][j];
					}
				}
			int j = times.size() - 1;
			cd.statesAfter[i] = cd.states[i][j];
			cd.statesAfter[i + 1] = cd.states[i + 1][j];
			cd.fillsAfter[i] = cd.fills[i][j];
			cd.fillsAfter[i + 1] = cd.fills[i + 1][j];
		}
		return cd;
	}

	private int[][] makeEmptyMatrix(Vector ids) {
		if (times == null || times.size() < 1 || ids == null || ids.size() < 1)
			return null;
		int load[][] = new int[ids.size()][times.size()];
		for (int i = 0; i < ids.size(); i++) {
			for (int j = 0; j < times.size(); j++) {
				load[i][j] = 0;
			}
		}
		return load;
	}

	private int[] makeEmptyArray(Vector ids) {
		if (ids == null || ids.size() < 1)
			return null;
		int load[] = new int[ids.size()];
		for (int i = 0; i < ids.size(); i++) {
			load[i] = 0;
		}
		return load;
	}

	/**
	 * Using the given layer containing the destination sites, constructs a
	 * layer with time-referenced objects showing the numbers of items at the
	 * destination sites at different time moments.
	 */
	public DGeoLayer makeDestinationUseLayer(DGeoLayer locLayer) {
		if (locLayer == null || siteIds == null || siteIds.size() < 1 || times == null || times.size() < 0 || deliveries == null)
			return null;

		DataTable destUseTbl = new DataTable();
		destUseTbl.setContainerIdentifier("dest_use");
		destUseTbl.setName("Use of destinations");
		int icn2 = -1, icc2 = -1, icap2 = -1, iN2 = -1, iCapRest = -1, iDestId2 = -1, iDestName2 = -1, iFrom2 = -1, iUntil2 = -1;
		if (itemCategories != null) {
			destUseTbl.addAttribute("Item class name", "class_name", AttributeTypes.character);
			icn2 = destUseTbl.getAttrCount() - 1;
			if (itemCatCodes != null) {
				destUseTbl.addAttribute("Item class code", "class_code", AttributeTypes.character);
				icc2 = destUseTbl.getAttrCount() - 1;
			}
		}
		destUseTbl.addAttribute("Capacity", "capacity", AttributeTypes.integer);
		icap2 = destUseTbl.getAttrCount() - 1;
		destUseTbl.addAttribute("Number of items", "number", AttributeTypes.integer);
		iN2 = destUseTbl.getAttrCount() - 1;
		destUseTbl.addAttribute("Remaining capacity", "rest_capacity", AttributeTypes.integer);
		iCapRest = destUseTbl.getAttrCount() - 1;
		destUseTbl.addAttribute("Destination ID", "dest_id", AttributeTypes.character);
		iDestId2 = destUseTbl.getAttrCount() - 1;
		if (siteNames != null) {
			destUseTbl.addAttribute("Destination name", "dest_name", AttributeTypes.character);
			iDestName2 = destUseTbl.getAttrCount() - 1;
		}
		destUseTbl.addAttribute("Period: from", "valid_from", AttributeTypes.time);
		iFrom2 = destUseTbl.getAttrCount() - 1;
		Attribute timeAttr = destUseTbl.getAttribute(iFrom2);
		timeAttr.timeRefMeaning = Attribute.VALID_FROM;
		destUseTbl.addAttribute("Period: till", "valid_until", AttributeTypes.time);
		iUntil2 = destUseTbl.getAttrCount() - 1;
		timeAttr = destUseTbl.getAttribute(iUntil2);
		timeAttr.timeRefMeaning = Attribute.VALID_UNTIL;

		Vector locStates = new Vector(siteIds.size() * 3 * ((itemCategories == null) ? 1 : itemCategories.size()), 50);

		for (int iSite = 0; iSite < siteIds.size(); iSite++) {
			String siteId = (String) siteIds.elementAt(iSite);
			DGeoObject geo1 = (DGeoObject) locLayer.findObjectById(siteId);
			if (geo1 == null) {
				continue;
			}
			String siteName = (siteNames == null) ? null : (String) siteNames.elementAt(iSite);
			if (siteName == null) {
				siteName = geo1.getLabel();
			}
			int nCat = 0;
			if (itemCategories != null) {
				nCat = itemCategories.size();
			}
			for (int iCat = -1; iCat < nCat; iCat++) {
				int deliv[][] = (int[][]) deliveries.elementAt(iCat + 1);
				int cap = siteCapacities[iSite][iCat + 1];
				if (cap <= 0) {
					boolean used = false;
					for (int j = 0; j < times.size() && !used; j++) {
						used = deliv[iSite][j] > 0;
					}
					if (!used) {
						continue;
					}
				}
				IntArray tIdxs = new IntArray(10, 10);
				for (int j = 0; j < times.size(); j++)
					if (deliv[iSite][j] > 0 && (j == 0 || deliv[iSite][j] > deliv[iSite][j - 1])) {
						tIdxs.addElement(j);
					}
				if (tIdxs.size() < 1) {
					tIdxs.addElement(0);
					tIdxs.addElement(times.size() - 1);
				}
				String catStr = (iCat < 0) ? "all" : (String) itemCategories.elementAt(iCat);
				for (int j = -1; j < tIdxs.size(); j++) {
					DGeoObject geo2 = (DGeoObject) geo1.makeCopy();
					geo2.setIdentifier(geo1.getIdentifier() + "_" + (locStates.size() + 1));
					if (siteName != null) {
						geo2.setLabel(siteName);
					}
					locStates.addElement(geo2);
					DataRecord rec2 = new DataRecord(geo2.getIdentifier(), siteName);
					destUseTbl.addDataRecord(rec2);
					rec2.setAttrValue(siteId, iDestId2);
					if (iDestName2 >= 0 && siteName != null) {
						rec2.setAttrValue(siteName, iDestName2);
					}
					if (icn2 >= 0) {
						rec2.setAttrValue(catStr, icn2);
					}
					if (icc2 >= 0 && iCat >= 0) {
						rec2.setAttrValue(itemCatCodes.elementAt(iCat), icc2);
					}
					rec2.setNumericAttrValue(cap, String.valueOf(cap), icap2);
					int num = (j < 0) ? 0 : (j < tIdxs.size()) ? deliv[iSite][tIdxs.elementAt(j)] : deliv[iSite][tIdxs.elementAt(j - 1)];
					rec2.setNumericAttrValue(num, String.valueOf(num), iN2);
					int rest = cap - num;
					rec2.setNumericAttrValue(rest, String.valueOf(rest), iCapRest);
					TimeReference tref = new TimeReference();
					if (j >= 0) {
						tref.setValidFrom((TimeMoment) times.elementAt(tIdxs.elementAt(j)));
					}
					if (j < tIdxs.size() - 1) {
						tref.setValidUntil((TimeMoment) times.elementAt(tIdxs.elementAt(j + 1)));
					}
					rec2.setAttrValue(tref.getValidFrom(), iFrom2);
					rec2.setAttrValue(tref.getValidUntil(), iUntil2);
					rec2.setTimeReference(tref);
					geo2.setThematicData(rec2);
					geo2.getSpatialData().setTimeReference(rec2.getTimeReference());
				}
				DGeoObject geo2 = (DGeoObject) geo1.makeCopy();
				geo2.setIdentifier(geo1.getIdentifier() + "_" + (locStates.size() + 1));
				if (siteName != null) {
					geo2.setLabel(siteName);
				}
				locStates.addElement(geo2);
				DataRecord rec2 = new DataRecord(geo2.getIdentifier(), siteName);
				destUseTbl.addDataRecord(rec2);
				rec2.setAttrValue(siteId, iDestId2);
				if (iDestName2 >= 0 && siteName != null) {
					rec2.setAttrValue(siteName, iDestName2);
				}
				if (icn2 >= 0) {
					rec2.setAttrValue(catStr, icn2);
				}
				if (icc2 >= 0 && iCat >= 0) {
					rec2.setAttrValue(itemCatCodes.elementAt(iCat), icc2);
				}
				rec2.setNumericAttrValue(cap, String.valueOf(cap), icap2);
				int num = deliv[iSite][times.size() - 1];
				rec2.setNumericAttrValue(num, String.valueOf(num), iN2);
				int rest = cap - num;
				rec2.setNumericAttrValue(rest, String.valueOf(rest), iCapRest);
				geo2.setThematicData(rec2);
			}
		}
		DGeoLayer dgl = new DGeoLayer();
		dgl.setName(destUseTbl.getName());
		dgl.setType(Geometry.point);
		dgl.setGeoObjects(locStates, true);
		DrawingParameters dp = dgl.getDrawingParameters();
		if (dp == null) {
			dp = new DrawingParameters();
			dgl.setDrawingParameters(dp);
		}
		dp.lineColor = Color.orange;
		dp.lineWidth = 2;
		dp.fillColor = Color.orange;
		dp.fillContours = false;
		dp.drawLabels = true;
		dp.labelColor = Color.gray;
		dgl.setDataTable(destUseTbl);

		return dgl;
	}
}
