package spade.analysis.tools.schedule;

import java.awt.Color;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.lib.lang.Language;
import spade.lib.util.IntArray;
import spade.lib.util.StringUtil;
import spade.time.Date;
import spade.time.TimeMoment;
import spade.time.TimeReference;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DrawingParameters;
import spade.vis.geometry.Geometry;
import spade.vis.spec.AttrValueColorPrefSpec;
import spade.vis.spec.AttrValueOrderPrefSpec;
import spade.vis.spec.DataSourceSpec;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 15-Mar-2007
 * Time: 12:47:17
 * On the basis of a transportation schedule, builds a table describing
 * the groups of items transported
 */
public class ItemCollector {
	static ResourceBundle res = Language.getTextResource("spade.analysis.tools.schedule.SchedulerTexts_tools_schedule");

	public static String itemsInSource = res.getString("in_source"), itemsDelayInSource = res.getString("delay_in_source"), itemsOnTheWay = res.getString("on_way"), itemsInDestination = res.getString("in_destination"), itemsInUnsuitableVehicle = res
			.getString("in_unsuitable_vehicle"), itemsInUnsuitableDestination = res.getString("in_unsuitable_destination");

	/**
	 * Returns true if successfull
	 */
	public boolean collectItems(ScheduleData schData) {
		if (schData == null || schData.souTbl == null || schData.itemNumColIdx < 0 || schData.ldd == null || schData.ldd.souColIdx < 0 || schData.ldd.souTimeColIdx < 0 || schData.ldd.destColIdx < 0 || schData.ldd.destTimeColIdx < 0)
			return false;
		DataTable itemData = new DataTable();
		itemData.setContainerIdentifier("items_" + schData.souTbl.getContainerIdentifier());
		itemData.setEntitySetIdentifier(schData.souTbl.getEntitySetIdentifier());
		itemData.setName("Items in " + schData.souTbl.getName());
		itemData.addAttribute("Item class name", "class_name", AttributeTypes.character);
		itemData.addAttribute("Item class code", "class_code", AttributeTypes.character);
		itemData.addAttribute("Number of items", "number", AttributeTypes.integer);
		itemData.addAttribute("Source ID", "source_id", AttributeTypes.character);
		if (schData.ldd.souColIdx >= 0) {
			itemData.addAttribute("Source name", "source_name", AttributeTypes.character);
		}
		itemData.addAttribute("Destination ID", "dest_id", AttributeTypes.character);
		if (schData.ldd.destColIdx >= 0) {
			itemData.addAttribute("Destination name", "dest_name", AttributeTypes.character);
		}
		itemData.addAttribute("Departure time", "dep_time", AttributeTypes.time);
		itemData.addAttribute("Attrival time", "arr_time", AttributeTypes.time);
		itemData.addAttribute("Max departure time", "max_dep_time", AttributeTypes.time);
		if (schData.vehicleIdColIdx >= 0) {
			itemData.addAttribute("Vehicle ID", "vehicle_id", AttributeTypes.character);
		}
		if (schData.vehicleTypeColIdx >= 0) {
			itemData.addAttribute("Vehicle type", "vehicle_type", AttributeTypes.character);
		}
		itemData.addAttribute("Delay", "delay", AttributeTypes.integer);
		schData.itemData = itemData;
		int idx[] = new int[12];
		idx[0] = schData.souTbl.getAttrIndex("ItemClass");
		idx[1] = -1;
		idx[2] = schData.itemNumColIdx;
		idx[3] = schData.ldd.souColIdx;
		idx[4] = schData.souTbl.getAttrIndex("SourceName");
		idx[5] = schData.ldd.destColIdx;
		idx[6] = schData.souTbl.getAttrIndex("DestName");
		idx[7] = schData.ldd.souTimeColIdx;
		idx[8] = schData.ldd.destTimeColIdx;
		idx[9] = -1;
		idx[10] = schData.vehicleIdColIdx;
		idx[11] = schData.vehicleTypeColIdx;
		for (int i = 0; i < schData.souTbl.getDataItemCount(); i++)
			if (schData.souTbl.getNumericAttrValue(schData.itemNumColIdx, i) > 0) {
				DataRecord dr = new DataRecord(schData.souTbl.getDataRecord(i).getId());
				itemData.addDataRecord(dr);
				for (int j = 0; j < idx.length; j++)
					if (idx[j] >= 0) {
						dr.setAttrValue(schData.souTbl.getAttrValue(idx[j], i), j);
						/*
						dr.setAttrValue(schData.souTbl.getAttrValue(schData.souTbl.getAttrIndex("ItemClass"),i),0);
						dr.setAttrValue(schData.souTbl.getAttrValue(schData.itemNumColIdx,i),2);
						dr.setAttrValue(schData.souTbl.getAttrValue(schData.ldd.souColIdx,i),3);
						dr.setAttrValue(schData.souTbl.getAttrValue(schData.souTbl.getAttrIndex("SourceName"),i),4);
						dr.setAttrValue(schData.souTbl.getAttrValue(schData.ldd.destColIdx,i),5);
						dr.setAttrValue(schData.souTbl.getAttrValue(schData.souTbl.getAttrIndex("DestName"),i),6);
						dr.setAttrValue(schData.souTbl.getAttrValue(schData.ldd.souTimeColIdx,i),7);
						dr.setAttrValue(schData.souTbl.getAttrValue(schData.ldd.destTimeColIdx,i),8);
						dr.setAttrValue(schData.souTbl.getAttrValue(schData.souTbl.getAttrIndex("VhclId"),i),10);
						dr.setAttrValue(schData.souTbl.getAttrValue(schData.souTbl.getAttrIndex("VhclType"),i),11);
						*/
					}
			}
		return itemData.getDataItemCount() > 0;
	}

	/**
	 * Completes the table with data about item groups with the information
	 * available in the table of data sources:
	 * 1) time limits;
	 * 2) groups that are not transported according to the schedule
	 */
	protected void completeItemData(DataTable itemData, DataTable sourceData) {
		if (itemData == null || sourceData == null || sourceData.getDataItemCount() < 1)
			return;
		DataSourceSpec spec = (DataSourceSpec) sourceData.getDataSource();
		if (spec == null || spec.extraInfo == null)
			return;
		int souIdColIdx = -1, souNameColIdx = -1, itemCatColIdx = -1, itemCatCodeColIdx = -1, itemNumColIdx = -1, timeLimitColIdx = -1;
		if (spec.extraInfo.get("SOURCE_ID_FIELD_NAME") != null) {
			souIdColIdx = sourceData.findAttrByName((String) spec.extraInfo.get("SOURCE_ID_FIELD_NAME"));
		}
		if (spec.extraInfo.get("SOURCE_NAME_FIELD_NAME") != null) {
			souNameColIdx = sourceData.findAttrByName((String) spec.extraInfo.get("SOURCE_NAME_FIELD_NAME"));
		}
		if (spec.extraInfo.get("ITEM_CLASS_FIELD_NAME") != null) {
			itemCatColIdx = sourceData.findAttrByName((String) spec.extraInfo.get("ITEM_CLASS_FIELD_NAME"));
		}
		if (spec.extraInfo.get("ITEM_CLASS_CODE_FIELD_NAME") != null) {
			itemCatCodeColIdx = sourceData.findAttrByName((String) spec.extraInfo.get("ITEM_CLASS_CODE_FIELD_NAME"));
		}
		if (spec.extraInfo.get("ITEM_NUMBER_FIELD_NAME") != null) {
			itemNumColIdx = sourceData.findAttrByName((String) spec.extraInfo.get("ITEM_NUMBER_FIELD_NAME"));
		}
		if (spec.extraInfo.get("TIME_LIMIT_FIELD_NAME") != null) {
			timeLimitColIdx = sourceData.findAttrByName((String) spec.extraInfo.get("TIME_LIMIT_FIELD_NAME"));
		}
		if (itemNumColIdx < 0 || souIdColIdx < 0)
			return;

		int itemDataSouIdColN = itemData.getAttrIndex("source_id");
		if (itemDataSouIdColN < 0)
			return;
		int itemDataItemNumColN = itemData.getAttrIndex("number");
		if (itemDataItemNumColN < 0)
			return;
		int itemDataItemCatColIdx = itemData.getAttrIndex("class_name");
		int itemDataItemCatCodeColIdx = itemData.getAttrIndex("class_code");
		int itemDataMaxTimeColN = itemData.getAttrIndex("max_dep_time");
		if (itemDataMaxTimeColN < 0 && timeLimitColIdx >= 0) {
			itemData.addAttribute("Max departure time", "max_dep_time", AttributeTypes.time);
			itemDataMaxTimeColN = itemData.getAttrIndex("max_dep_time");
		}
		int depTimeColIdx = itemData.getAttrIndex("dep_time");
		int delayColIdx = itemData.getAttrIndex("delay");
		int itemDataSouNameColN = itemData.getAttrIndex("source_name");
		int nLeft = 0;
		Date zeroDate = new Date();
		zeroDate.setSecond(0);
		zeroDate.setMinute(0);
		zeroDate.setHour(0);
		zeroDate.setDateScheme("hh:tt:ss");
		zeroDate.setPrecision('t');
		for (int i = 0; i < sourceData.getDataItemCount(); i++) {
			DataRecord rec = sourceData.getDataRecord(i);
			double fval = rec.getNumericAttrValue(itemNumColIdx);
			if (Double.isNaN(fval)) {
				continue;
			}
			int origNum = (int) Math.round(fval);
			if (origNum < 1) {
				continue;
			}
			String souId = rec.getAttrValueAsString(souIdColIdx);
			if (souId == null) {
				continue;
			}
			String itemCat = null, itemCatCode = null;
			if (itemCatColIdx >= 0) {
				itemCat = rec.getAttrValueAsString(itemCatColIdx);
			}
			if (itemCatCodeColIdx >= 0) {
				itemCatCode = rec.getAttrValueAsString(itemCatCodeColIdx);
			}
			TimeMoment maxTime = null;
			if (timeLimitColIdx >= 0) {
				fval = rec.getNumericAttrValue(timeLimitColIdx);
				if (!Double.isNaN(fval)) {
					int timeLimit = (int) Math.round(fval);
					if (timeLimit > 0) {
						maxTime = zeroDate.getCopy();
						maxTime.add(timeLimit);
					}
				}
			}
			//in the table itemData find all records with the source souId
			for (int j = 0; j < itemData.getDataItemCount(); j++)
				if (StringUtil.sameStrings(souId, itemData.getAttrValueAsString(itemDataSouIdColN, j))) {
					DataRecord groupRec = itemData.getDataRecord(j);
					if (itemCat != null && itemDataItemCatColIdx >= 0 && !StringUtil.sameStringsIgnoreCase(itemCat, groupRec.getAttrValueAsString(itemDataItemCatColIdx))) {
						continue; //another item category
					}
					int num = 0;
					fval = groupRec.getNumericAttrValue(itemDataItemNumColN);
					if (Double.isNaN(fval)) {
						continue;
					}
					num = (int) Math.round(fval);
					if (num < 1) {
						continue;
					}
					origNum -= num;
					if (itemDataItemCatCodeColIdx >= 0 && itemCatCode != null) {
						groupRec.setAttrValue(itemCatCode, itemDataItemCatCodeColIdx);
					}
					if (maxTime != null) {
						groupRec.setAttrValue(maxTime, itemDataMaxTimeColN);
						if (depTimeColIdx >= 0 && delayColIdx >= 0) {
							Object val = groupRec.getAttrValue(depTimeColIdx);
							if (val != null && (val instanceof TimeMoment)) {
								TimeMoment t = (TimeMoment) val;
								if (t.compareTo(maxTime) > 0) {
									long delay = t.subtract(maxTime);
									groupRec.setNumericAttrValue((float) delay, String.valueOf(delay), delayColIdx);
								} else {
									groupRec.setNumericAttrValue(0f, "0", delayColIdx);
								}
							}
						}
					}
				}
			if (origNum > 0) {
				//some items have not been transported!
				++nLeft;
				DataRecord groupRec = new DataRecord("items_left_" + nLeft, "Items left in " + ((souNameColIdx >= 0) ? rec.getAttrValueAsString(souNameColIdx) : souId));
				itemData.addDataRecord(groupRec);
				groupRec.setAttrValue(souId, itemDataSouIdColN);
				if (souNameColIdx >= 0 && itemDataSouNameColN >= 0) {
					groupRec.setAttrValue(rec.getAttrValueAsString(souNameColIdx), itemDataSouNameColN);
				}
				if (itemCat != null && itemDataItemCatColIdx >= 0) {
					groupRec.setAttrValue(itemCat, itemDataItemCatColIdx);
				}
				if (itemCatCode != null && itemDataItemCatCodeColIdx >= 0) {
					groupRec.setAttrValue(itemCatCode, itemDataItemCatCodeColIdx);
				}
				groupRec.setNumericAttrValue((float) origNum, String.valueOf(origNum), itemDataItemNumColN);
				if (maxTime != null) {
					groupRec.setAttrValue(maxTime, itemDataMaxTimeColN);
				}
			}
		}

		if (itemCatColIdx >= 0 && spec.descriptors != null) {
			DataSourceSpec spec1 = (DataSourceSpec) itemData.getDataSource();
			if (spec1 == null) {
				spec1 = new DataSourceSpec();
				spec.name = itemData.getName();
				itemData.setDataSource(spec1);
			}
			if (spec1.descriptors == null) {
				spec1.descriptors = new Vector(5, 5);
			}
			for (int i = 0; i < spec.descriptors.size(); i++)
				if (spec.descriptors.elementAt(i) instanceof AttrValueColorPrefSpec) {
					AttrValueColorPrefSpec cps = (AttrValueColorPrefSpec) spec.descriptors.elementAt(i);
					if (StringUtil.sameStringsIgnoreCase(cps.attrName, sourceData.getAttributeName(itemCatColIdx))) {
						AttrValueColorPrefSpec cps1 = new AttrValueColorPrefSpec();
						cps1.attrName = itemData.getAttributeName("class_name");
						if (cps1.attrName != null) {
							cps1.colorPrefs = cps.colorPrefs;
							spec1.descriptors.addElement(cps1);
						}
					}
				} else if (spec.descriptors.elementAt(i) instanceof AttrValueOrderPrefSpec) {
					AttrValueOrderPrefSpec aOrdPref = (AttrValueOrderPrefSpec) spec.descriptors.elementAt(i);
					if (StringUtil.sameStringsIgnoreCase(aOrdPref.attrName, sourceData.getAttributeName(itemCatColIdx))) {
						AttrValueOrderPrefSpec pref1 = new AttrValueOrderPrefSpec();
						pref1.attrName = itemData.getAttributeName("class_name");
						if (pref1.attrName != null) {
							pref1.values = aOrdPref.values;
							spec1.descriptors.addElement(pref1);
						}
					}
				}
		}
	}

	/**
	 * Retrieves all data categories from the given table with the data about the
	 * initial number of items in the source locations. Puts the names and
	 * codes (if available) of the item categories in the vectors supplied
	 * as arguments. If the vectors are not empty, completes them with
	 * yet lacking categories. Returns true if any new categories have been
	 * detected and added.
	 */
	public boolean getAllItemCategories(DataTable sourceData, Vector catNames, Vector catCodes) {
		if (sourceData == null || catNames == null)
			return false;
		DataSourceSpec spec = (DataSourceSpec) sourceData.getDataSource();
		if (spec == null || spec.extraInfo == null)
			return false;
		int itemCatColIdx = -1;
		if (spec.extraInfo.get("ITEM_CLASS_FIELD_NAME") != null) {
			itemCatColIdx = sourceData.findAttrByName((String) spec.extraInfo.get("ITEM_CLASS_FIELD_NAME"));
		}
		if (itemCatColIdx < 0)
			return false;
		int itemCatCodeColIdx = -1;
		if (catCodes != null && spec.extraInfo.get("ITEM_CLASS_CODE_FIELD_NAME") != null) {
			itemCatCodeColIdx = sourceData.findAttrByName((String) spec.extraInfo.get("ITEM_CLASS_CODE_FIELD_NAME"));
		}
		int nAdded = 0;
		for (int i = 0; i < sourceData.getDataItemCount(); i++) {
			DataRecord rec = sourceData.getDataRecord(i);
			String itemCat = rec.getAttrValueAsString(itemCatColIdx);
			if (itemCat == null) {
				continue;
			}
			int idx = StringUtil.indexOfStringInVectorIgnoreCase(itemCat, catNames);
			if (idx < 0) {
				catNames.addElement(itemCat);
				++nAdded;
				idx = catNames.size() - 1;
			}
			if (itemCatCodeColIdx >= 0) {
				String itemCatCode = rec.getAttrValueAsString(itemCatCodeColIdx);
				if (itemCatCode != null) {
					while (catCodes.size() <= idx) {
						catCodes.addElement(null);
					}
					catCodes.setElementAt(itemCatCode, idx);
				}
			}
		}
		return nAdded > 0;
	}

	/**
	 * On the basis of the given table with data about the groups of items to be
	 * transported and the layer containing their source locations, constructs a
	 * layer with time-referenced objects showing the numbers of items at the source
	 * locations at different time moments.
	 */
	public DGeoLayer makeSourceDynamicsLayer(DataTable tItemData, DGeoLayer locLayer) {
		if (tItemData == null || locLayer == null || !tItemData.hasData() || !locLayer.hasData())
			return null;
		int iSouId1 = tItemData.getAttrIndex("source_id");
		if (iSouId1 < 0)
			return null;
		int iN1 = tItemData.getAttrIndex("number");
		if (iN1 < 0)
			return null;
		int idt1 = tItemData.getAttrIndex("dep_time");
		int iat1 = tItemData.getAttrIndex("arr_time");
		if (idt1 < 0 || iat1 < 0)
			return null;
		int iMaxDT1 = tItemData.getAttrIndex("max_dep_time");
		int iSouName1 = tItemData.getAttrIndex("source_name");
		int icn1 = tItemData.getAttrIndex("class_name");
		int icc1 = tItemData.getAttrIndex("class_code");

		Vector souIds = tItemData.getAllAttrValuesAsStrings("source_id");
		if (souIds == null || souIds.size() < 1)
			return null;
		IntArray recNsBySou[] = new IntArray[souIds.size()];
		for (int i = 0; i < souIds.size(); i++) {
			recNsBySou[i] = new IntArray(50, 20);
		}
		for (int i = 0; i < tItemData.getDataItemCount(); i++) {
			String souId = tItemData.getAttrValueAsString(iSouId1, i);
			if (souId == null) {
				continue;
			}
			int idx = StringUtil.indexOfStringInVectorIgnoreCase(souId, souIds);
			if (idx >= 0) {
				recNsBySou[idx].addElement(i);
			}
		}

		DataTable souStatesData = new DataTable();
		souStatesData.setContainerIdentifier("sources_" + tItemData.getContainerIdentifier());
		souStatesData.setName("Sources of " + tItemData.getName());
		int icn2 = -1, icc2 = -1, iN2 = -1, iNDelay2 = -1, iSouId2 = -1, iSouName2 = -1, iFrom2 = -1, iUntil2 = -1;
		if (icn1 >= 0) {
			souStatesData.addAttribute("Item class name", "class_name", AttributeTypes.character);
			icn2 = souStatesData.getAttrCount() - 1;
		}
		if (icc1 >= 0) {
			souStatesData.addAttribute("Item class code", "class_code", AttributeTypes.character);
			icc2 = souStatesData.getAttrCount() - 1;
		}
		souStatesData.addAttribute("Number of items", "number", AttributeTypes.integer);
		iN2 = souStatesData.getAttrCount() - 1;
		souStatesData.addAttribute("Number of delayed items", "number_delayed", AttributeTypes.integer);
		iNDelay2 = souStatesData.getAttrCount() - 1;
		souStatesData.addAttribute("Source ID", "source_id", AttributeTypes.character);
		iSouId2 = souStatesData.getAttrCount() - 1;
		if (iSouName1 >= 0) {
			souStatesData.addAttribute("Source name", "source_name", AttributeTypes.character);
			iSouName2 = souStatesData.getAttrCount() - 1;
		}
		souStatesData.addAttribute("Period: from", "valid_from", AttributeTypes.time);
		iFrom2 = souStatesData.getAttrCount() - 1;
		souStatesData.addAttribute("Period: till", "valid_until", AttributeTypes.time);
		iUntil2 = souStatesData.getAttrCount() - 1;

		Vector locStates = new Vector(tItemData.getDataItemCount() * 2, 50);

		for (int iSou = 0; iSou < souIds.size(); iSou++) {
			if (recNsBySou[iSou].size() < 1) {
				continue;
			}
			DGeoObject geo1 = (DGeoObject) locLayer.findObjectById((String) souIds.elementAt(iSou));
			if (geo1 == null) {
				continue;
			}
			String souName = null;
			Vector cat = new Vector(10, 5);
			if (icn1 >= 0 || icc1 >= 0) {
				for (int j = 0; j < recNsBySou[iSou].size(); j++) {
					String catStr = tItemData.getAttrValueAsString((icn1 >= 0) ? icn1 : icc1, recNsBySou[iSou].elementAt(j));
					if (!StringUtil.isStringInVectorIgnoreCase(catStr, cat)) {
						cat.addElement(catStr);
					}
					if (souName == null && iSouName1 >= 0) {
						souName = tItemData.getAttrValueAsString(iSouName1, recNsBySou[iSou].elementAt(j));
					}
				}
			}
			cat.addElement("all");
			if (souName == null) {
				souName = geo1.getLabel();
			}
			for (int iCat = 0; iCat < cat.size(); iCat++) {
				String catStr = (String) cat.elementAt(iCat);
				Vector times = new Vector(recNsBySou[iSou].size(), 1);
				for (int iRec = 0; iRec < recNsBySou[iSou].size(); iRec++) {
					DataRecord rec1 = tItemData.getDataRecord(recNsBySou[iSou].elementAt(iRec));
					if (iCat < cat.size() - 1) {
						String val = rec1.getAttrValueAsString((icn1 >= 0) ? icn1 : icc1);
						if (val == null || !val.equalsIgnoreCase(catStr)) {
							continue;
						}
					}
					TimeMoment depTime = (TimeMoment) rec1.getAttrValue(idt1), maxDepTime = (iMaxDT1 >= 0) ? (TimeMoment) rec1.getAttrValue(iMaxDT1) : null;
					TimeMoment t1 = depTime, t2 = null;
					if (maxDepTime != null)
						if (t1 == null) {
							t1 = maxDepTime;
						} else if (t1.compareTo(maxDepTime) > 0) {
							t2 = t1;
							t1 = maxDepTime;
						}
					if (t1 == null) {
						continue;
					}
					boolean same = false;
					int idx = -1;
					for (int iTime = 0; iTime < times.size() && idx < 0; iTime++) {
						int cmp = t1.compareTo((TimeMoment) times.elementAt(iTime));
						if (cmp <= 0) {
							idx = iTime;
						}
						same = cmp == 0;
					}
					if (!same)
						if (idx >= 0) {
							times.insertElementAt(t1, idx);
						} else {
							times.addElement(t1);
							idx = times.size() - 1;
						}
					if (t2 != null) {
						same = false;
						int idx2 = -1;
						for (int iTime = idx + 1; iTime < times.size() && idx2 < 0; iTime++) {
							int cmp = t2.compareTo((TimeMoment) times.elementAt(iTime));
							if (cmp <= 0) {
								idx2 = iTime;
							}
							same = cmp == 0;
						}
						if (!same)
							if (idx2 >= 0) {
								times.insertElementAt(t2, idx2);
							} else {
								times.addElement(t2);
							}
					}
				}
				int counts[] = new int[times.size() + 1]; //before each moment + after all
				for (int i = 0; i < counts.length; i++) {
					counts[i] = 0;
				}
				int delayed[] = new int[times.size() + 1]; //before each moment + after all
				for (int i = 0; i < delayed.length; i++) {
					delayed[i] = 0;
				}
				String catCode = null;
				for (int iRec = 0; iRec < recNsBySou[iSou].size(); iRec++) {
					DataRecord rec1 = tItemData.getDataRecord(recNsBySou[iSou].elementAt(iRec));
					if (iCat < cat.size() - 1) {
						String val = rec1.getAttrValueAsString((icn1 >= 0) ? icn1 : icc1);
						if (val == null || !val.equalsIgnoreCase(catStr)) {
							continue;
						}
						if (catCode == null && icc1 >= 0) {
							catCode = rec1.getAttrValueAsString(icc1);
						}
					}
					double val = rec1.getNumericAttrValue(iN1);
					if (Double.isNaN(val)) {
						continue;
					}
					int nItems = (int) Math.round(val);
					counts[0] += nItems;
					TimeMoment depTime = (TimeMoment) rec1.getAttrValue(idt1), maxDepTime = (iMaxDT1 >= 0) ? (TimeMoment) rec1.getAttrValue(iMaxDT1) : null;
					if (depTime == null) {
						for (int i = 1; i < counts.length; i++) {
							counts[i] += nItems;
						}
						if (maxDepTime != null) {
							int idx2 = times.indexOf(maxDepTime);
							for (int i = idx2; i < counts.length; i++) {
								delayed[i] += nItems;
							}
						}
					} else {
						int idx = times.indexOf(depTime);
						for (int i = 1; i <= idx; i++) {
							counts[i] += nItems;
						}
						if (maxDepTime != null && depTime.compareTo(maxDepTime) > 0) {
							int idx2 = times.indexOf(maxDepTime);
							for (int i = idx2 + 1; i <= idx; i++) {
								delayed[i] += nItems;
							}
						}
					}
				}
				for (int i = 0; i < counts.length; i++) {
					DGeoObject geo2 = (DGeoObject) geo1.makeCopy();
					geo2.setIdentifier(geo1.getIdentifier() + "_" + (locStates.size() + 1));
					if (souName != null) {
						geo2.setLabel(souName);
					}
					locStates.addElement(geo2);
					DataRecord rec2 = new DataRecord(geo2.getIdentifier(), souName);
					souStatesData.addDataRecord(rec2);
					rec2.setAttrValue(souIds.elementAt(iSou), iSouId2);
					if (iSouName2 >= 0 && souName != null) {
						rec2.setAttrValue(souName, iSouName2);
					}
					if (icn2 >= 0) {
						rec2.setAttrValue(catStr, icn2);
					}
					if (icc2 >= 0) {
						rec2.setAttrValue(catCode, icc2);
					}
					rec2.setNumericAttrValue(counts[i], String.valueOf(counts[i]), iN2);
					rec2.setNumericAttrValue(delayed[i], String.valueOf(delayed[i]), iNDelay2);
					if (counts.length > 1) {
						TimeReference tref = new TimeReference();
						if (i > 0) {
							tref.setValidFrom((TimeMoment) times.elementAt(i - 1));
						}
						if (i < times.size()) {
							tref.setValidUntil((TimeMoment) times.elementAt(i));
						}
						rec2.setAttrValue(tref.getValidFrom(), iFrom2);
						rec2.setAttrValue(tref.getValidUntil(), iUntil2);
						rec2.setTimeReference(tref);
					}
					geo2.setThematicData(rec2);
					geo2.getSpatialData().setTimeReference(rec2.getTimeReference());
				}
				if (counts.length > 1) {
					DGeoObject geo2 = (DGeoObject) geo1.makeCopy();
					geo2.setIdentifier(geo1.getIdentifier() + "_" + (locStates.size() + 1));
					if (souName != null) {
						geo2.setLabel(souName);
					}
					locStates.addElement(geo2);
					DataRecord rec2 = new DataRecord(geo2.getIdentifier(), souName);
					souStatesData.addDataRecord(rec2);
					rec2.setAttrValue(souIds.elementAt(iSou), iSouId2);
					if (iSouName2 >= 0 && souName != null) {
						rec2.setAttrValue(souName, iSouName2);
					}
					if (icn2 >= 0) {
						rec2.setAttrValue(catStr, icn2);
					}
					if (icc2 >= 0) {
						rec2.setAttrValue(catCode, icc2);
					}
					rec2.setNumericAttrValue(counts[0], String.valueOf(counts[0]), iN2);
					rec2.setNumericAttrValue(delayed[counts.length - 1], String.valueOf(delayed[counts.length - 1]), iNDelay2);
					geo2.setThematicData(rec2);
				}
			}
		}
		DGeoLayer dgl = new DGeoLayer();
		dgl.setName(souStatesData.getName());
		dgl.setType(Geometry.point);
		dgl.setGeoObjects(locStates, true);
		DrawingParameters dp = dgl.getDrawingParameters();
		if (dp == null) {
			dp = new DrawingParameters();
			dgl.setDrawingParameters(dp);
		}
		dp.lineColor = Color.pink;
		dp.lineWidth = 2;
		dp.fillColor = Color.pink;
		dp.fillContours = false;
		dp.drawLabels = true;
		dp.labelColor = Color.gray;
		dp.transparency = 25;
		dgl.setDataTable(souStatesData);

		return dgl;
	}
}
