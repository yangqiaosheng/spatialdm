package spade.analysis.tools.schedule;

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Vector;

import spade.analysis.system.ESDACore;
import spade.analysis.tools.DataAnalyser;
import spade.analysis.tools.SingleInstanceTool;
import spade.analysis.tools.moves.MovementMatrixPanel;
import spade.analysis.tools.moves.MovementToolRegister;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.OKDialog;
import spade.lib.util.CopyFile;
import spade.lib.util.FloatArray;
import spade.lib.util.IntArray;
import spade.lib.util.StringUtil;
import spade.time.Date;
import spade.time.vis.TimeLineView;
import spade.vis.action.Highlighter;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.database.Parameter;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DLinkObject;
import spade.vis.geometry.Geometry;
import spade.vis.space.LayerManager;
import spade.vis.spec.AttrValueOrderPrefSpec;
import spade.vis.spec.DataSourceSpec;
import spade.vis.spec.LinkDataDescription;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 22-Feb-2007
 * Time: 14:34:26
 * Provides a set of tools to load, visualise, and explore transportation
 * schedules.
 */
public class ScheduleExplorer implements DataAnalyser, SingleInstanceTool, ActionListener, PropertyChangeListener {

	protected ESDACore core = null;
	/**
	 * For each schedule under exploration, contains the ScheduleData structure
	 * with all relevant data. The user may open several views to explore one and
	 * the same schedule. All the views will work with one and the same copy of
	 * ScheduleData.
	 */
	protected Vector scheduleData = null;
	/**
	 * Contains the names of the categories of the transported items. Does not
	 * contain "LEER" or "EMPTY"!
	 */
	protected Vector itemCategories = null;
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
	 * For each schedule, contains an instance of ItemCategorySelector. This is
	 * a non-UI object supporting the selection of the item category for viewing
	 * corresponding transportation orders and other relevant data in course of
	 * exploring a transportation schedule. The selection affects all currently
	 * existing displays of the data relevant to the schedule.
	 */
	protected Vector icatSelectors = null;
	/**
	 * Keeps information about classes of vehicles used for transportation.
	 */
	protected VehicleTypesInfo vehicleTypesInfo = null;
	/**
	 * Contains data about the original numbers of vehicles at their sources
	 */
	protected DataTable vehicleSources = null;
	/**
	 * Contains data about the available destinations and their original capacities
	 */
	protected DataTable destCap = null;

	protected OKDialog dia = null;

	/**
	 * Returns true when the tool has everything necessary for its operation.
	 * For example, if the tool manages a number of analysis methods, it should
	 * check whether the class for at least one method is available.
	 * A ScheduleExplorer returns true if the class LinkLayerBuilder is available.
	 */
	@Override
	public boolean isValid(ESDACore core) {
		String linkBuilderClassName = MovementToolRegister.getToolClassName("build_links");
		if (linkBuilderClassName == null)
			return false;
		try {
			Class.forName(linkBuilderClassName);
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	/**
	 * Looks for (new) schedules loaded in the system and, if finds, puts the
	 * relevant data in the vector scheduleData
	 */
	protected void lookForSchedules() {
		LayerManager lman = core.getUI().getCurrentMapViewer().getLayerManager();
		if (lman.getLayerCount() < 1) {
			showMessage("No map layers available!", true);
			return;
		}
		// find appropriate map layers, i.e. layers containing time-referenced
		// DLinkObjects with thematic data attached to them
		if (scheduleData == null) {
			scheduleData = new Vector(lman.getLayerCount(), 10);
		}
		for (int i = 0; i < lman.getLayerCount(); i++)
			if (lman.getGeoLayer(i) instanceof DGeoLayer) {
				DGeoLayer layer = (DGeoLayer) lman.getGeoLayer(i);
				if (layer.getObjectCount() < 1 || (layer.getType() != Geometry.line) || !layer.hasTimeReferences() || layer.getThematicData() == null || !(layer.getObjectAt(0) instanceof DLinkObject)) {
					continue;
				}
				//check if this layer is already registered in scheduleData
				boolean found = false;
				for (int j = 0; j < scheduleData.size() && !found; j++) {
					ScheduleData sd = (ScheduleData) scheduleData.elementAt(j);
					found = sd.linkLayer.getContainerIdentifier().equals(layer.getContainerIdentifier());
				}
				if (!found) {
					scheduleData.addElement(makeScheduleData(layer, lman));
					if (icatSelectors != null) {
						while (icatSelectors.size() < scheduleData.size()) {
							icatSelectors.addElement(null);
						}
					}
				}
			}
	}

	/**
	 * Creates and fills a ScheduleData structure on the basis of the given layer
	 * with transportations or movements between source and destination locations
	 */
	public static ScheduleData makeScheduleData(DGeoLayer layer, LayerManager lman) {
		if (layer == null)
			return null;
		ScheduleData sd = new ScheduleData();
		sd.linkLayer = layer;
		sd.souTbl = (DataTable) layer.getThematicData();
		if (layer.getDataSource() != null && (layer.getDataSource() instanceof DataSourceSpec)) {
			sd.spec = (DataSourceSpec) layer.getDataSource();
		}
		if ((sd.spec == null || sd.spec.descriptors == null) && sd.souTbl.getDataSource() != null && (sd.souTbl.getDataSource() instanceof DataSourceSpec)) {
			sd.spec = (DataSourceSpec) sd.souTbl.getDataSource();
		}
		if (sd.spec.descriptors != null) {
			for (int i = 0; i < sd.spec.descriptors.size() && sd.ldd == null; i++)
				if (sd.spec.descriptors.elementAt(i) instanceof LinkDataDescription) {
					sd.ldd = (LinkDataDescription) sd.spec.descriptors.elementAt(i);
				}
		}
		if (sd.ldd != null && sd.ldd.layerRef != null) {
			for (int i = 0; i < lman.getLayerCount() && sd.locLayer == null; i++)
				if ((lman.getGeoLayer(i) instanceof DGeoLayer) && lman.getGeoLayer(i).getContainerIdentifier().equalsIgnoreCase(sd.ldd.layerRef)) {
					sd.locLayer = (DGeoLayer) lman.getGeoLayer(i);
				}
		}
		if (sd.ldd != null && sd.ldd.souTimeColIdx >= 0 && sd.ldd.destTimeColIdx >= 0) {
			for (int i = 0; i < sd.souTbl.getDataItemCount(); i++) {
				DataRecord rec = sd.souTbl.getDataRecord(i);
				Object val = rec.getAttrValue(sd.ldd.souTimeColIdx);
				if (val != null && (val instanceof Date)) {
					Date d = (Date) val;
					if (d.getPrecision() == 's') {
						d.setPrecision('t');
						d.roundUp();
					}
				}
				val = rec.getAttrValue(sd.ldd.destTimeColIdx);
				if (val != null && (val instanceof Date)) {
					Date d = (Date) val;
					if (d.getPrecision() == 's') {
						d.setPrecision('t');
						d.roundUp();
					}
				}
			}
		}
		if (sd.spec.extraInfo != null) {
			if (sd.spec.extraInfo.get("SOURCE_NAME_FIELD_NAME") != null) {
				sd.souNameColIdx = sd.souTbl.findAttrByName((String) sd.spec.extraInfo.get("SOURCE_NAME_FIELD_NAME"));
			}
			if (sd.spec.extraInfo.get("DESTIN_NAME_FIELD_NAME") != null) {
				sd.destNameColIdx = sd.souTbl.findAttrByName((String) sd.spec.extraInfo.get("DESTIN_NAME_FIELD_NAME"));
			}
			if (sd.spec.extraInfo.get("ITEM_NUMBER_FIELD_NAME") != null) {
				sd.itemNumColIdx = sd.souTbl.findAttrByName((String) sd.spec.extraInfo.get("ITEM_NUMBER_FIELD_NAME"));
			}
			if (sd.spec.extraInfo.get("ITEM_CLASS_FIELD_NAME") != null) {
				sd.itemCatColIdx = sd.souTbl.findAttrByName((String) sd.spec.extraInfo.get("ITEM_CLASS_FIELD_NAME"));
			}
			if (sd.spec.extraInfo.get("VEHICLE_ID_FIELD_NAME") != null) {
				sd.vehicleIdColIdx = sd.souTbl.findAttrByName((String) sd.spec.extraInfo.get("VEHICLE_ID_FIELD_NAME"));
			}
			if (sd.spec.extraInfo.get("VEHICLE_CLASS_FIELD_NAME") != null) {
				sd.vehicleTypeColIdx = sd.souTbl.findAttrByName((String) sd.spec.extraInfo.get("VEHICLE_CLASS_FIELD_NAME"));
			}
			if (sd.spec.extraInfo.get("VEHICLE_HOME_ID_FIELD_NAME") != null) {
				sd.vehicleHomeIdColIdx = sd.souTbl.findAttrByName((String) sd.spec.extraInfo.get("VEHICLE_HOME_ID_FIELD_NAME"));
			}
			if (sd.spec.extraInfo.get("VEHICLE_HOME_NAME_FIELD_NAME") != null) {
				sd.vehicleHomeNameColIdx = sd.souTbl.findAttrByName((String) sd.spec.extraInfo.get("VEHICLE_HOME_NAME_FIELD_NAME"));
			}
		}
		return sd;
	}

	/**
	* This method constructs and starts the tool. Everything the tool may need
	* for integration with other components of the system can be received
	* from the system's core passed as an argument.
	*/
	@Override
	public void run(ESDACore core) {
		if (core == null || core.getUI() == null)
			return;
		this.core = core;
		if (core.getUI().getCurrentMapViewer() == null || core.getUI().getCurrentMapViewer().getLayerManager() == null) {
			showMessage("No map exists!", true);
			return;
		}
		lookForSchedules();
		if (scheduleData == null || scheduleData.size() < 1) {
			ScheduleLoader schLoader = new ScheduleLoader();
			schLoader.run(core);
			return;
		}
		Panel p = new Panel(new ColumnLayout());
		p.add(new Label("Schedule to explore:"));
		List list = new List(Math.max(scheduleData.size() + 1, 5));
		for (int i = 0; i < scheduleData.size(); i++) {
			ScheduleData sd = (ScheduleData) scheduleData.elementAt(i);
			list.add(sd.linkLayer.getName());
		}
		list.select(0);
		p.add(list);
		Button b = new Button("Load new schedule");
		b.setActionCommand("load_schedule");
		b.addActionListener(this);
		Panel pp = new Panel(new FlowLayout(FlowLayout.CENTER, 0, 2));
		pp.add(b);
		p.add(pp);
		p.add(new Label("Visualisation method:"));
		Checkbox timeLineCB = new Checkbox("Time line", true);
		p.add(timeLineCB);
		Checkbox matrixCB = new Checkbox("Movement matrix", true);
		p.add(matrixCB);
		Checkbox vehicleUseCB = new Checkbox("Vehicle use display", true);
		p.add(vehicleUseCB);
		dia = new OKDialog(core.getUI().getMainFrame(), "Schedule exploration", true);
		dia.addContent(p);
		dia.show();
		if (dia == null || dia.wasCancelled()) {
			dia = null;
			return;
		}
		int idx = list.getSelectedIndex();
		if (idx < 0)
			return;
		ScheduleData sd = (ScheduleData) scheduleData.elementAt(idx);
		if (vehicleSources == null || destCap == null) {
			ScheduleLoader schLoader = new ScheduleLoader();
			schLoader.setCore(core);
			if (vehicleSources == null) {
				DataSourceSpec spec = DataSourceSpec.readMetadata("vehicle_sources.descr", core.getDataKeeper().getApplicationPath());
				if (spec != null) {
					vehicleSources = schLoader.loadTableData(spec);
					if (vehicleSources != null && vehicleSources.getDataItemCount() > 0) {
						core.getDataLoader().addTable(vehicleSources);
						if (sd.locLayer != null) {
							vehicleSources.setEntitySetIdentifier(sd.locLayer.getEntitySetIdentifier());
						}
					}
				} else {
					showMessage(DataSourceSpec.getErrorMessage(), true);
				}
			}
			if (destCap == null) {
				DataSourceSpec spec = DataSourceSpec.readMetadata("destinations.descr", core.getDataKeeper().getApplicationPath());
				if (spec != null) {
					destCap = schLoader.loadTableData(spec);
					if (destCap != null && destCap.getDataItemCount() > 0) {
						core.getDataLoader().addTable(destCap);
						if (sd.locLayer != null) {
							destCap.setEntitySetIdentifier(sd.locLayer.getEntitySetIdentifier());
						}
					}
				} else {
					showMessage(DataSourceSpec.getErrorMessage(), true);
				}
			}
		}
		if (sd.itemData == null) {
			ItemCollector iCollector = new ItemCollector();
			if (iCollector.collectItems(sd) && sd.itemData != null) {
				core.getDataLoader().addTable(sd.itemData);
				//try to load data about the item sources: how many items are there
				//at the beginning
				ScheduleLoader schLoader = new ScheduleLoader();
				schLoader.setCore(core);
				DataSourceSpec spec = DataSourceSpec.readMetadata("item_sources.descr", core.getDataLoader().getApplicationPath());
				if (spec != null) {
					DataTable sTable = schLoader.loadTableData(spec);
					if (sTable != null && sTable.getDataItemCount() > 0) {
						iCollector.completeItemData(sd.itemData, sTable);
						if (itemCategories == null) {
							itemCategories = new Vector(10, 10);
						}
						if (itemCatCodes == null) {
							itemCatCodes = new Vector(10, 10);
						}
						if (iCollector.getAllItemCategories(sTable, itemCategories, itemCatCodes) && itemCategories.size() > 1) {
							String attrName = spec.getExtraInfoByKeyAsString("ITEM_CLASS_FIELD_NAME");
							if (attrName != null) {
								orderCategoriesByPreference(findOrderPrefSpec((DataSourceSpec) sd.souTbl.getDataSource(), attrName), itemCategories, itemCatCodes);
							}
							if (destCap != null) {
								fillItemClassNames(destCap, itemCategories, itemCatCodes);
							}
						}
					}
				} else {
					showMessage(DataSourceSpec.getErrorMessage(), true);
				}
			}
		}
		if (sd.souTbl != null && sd.itemCatColIdx >= 0 && (itemCategories == null || itemCategories.size() < 1)) {
			IntArray iar = new IntArray(1, 1);
			iar.addElement(sd.itemCatColIdx);
			itemCategories = sd.souTbl.getAllValuesInColumnsAsStrings(iar);
			if (itemCategories != null && itemCategories.size() > 1) {
				int k = StringUtil.indexOfStringInVectorIgnoreCase("LEER", itemCategories);
				if (k < 0) {
					k = StringUtil.indexOfStringInVectorIgnoreCase("EMPTY", itemCategories);
				}
				if (k >= 0) {
					itemCategories.removeElementAt(k);
				}
				if (itemCategories.size() > 1) {
					orderCategoriesByPreference(findOrderPrefSpec((DataSourceSpec) sd.souTbl.getDataSource(), sd.souTbl.getAttributeName(sd.itemCatColIdx)), itemCategories, itemCatCodes);
				}
			}
		}
		ItemCategorySelector iCatSelector = null;
		if (icatSelectors != null && idx < icatSelectors.size()) {
			iCatSelector = (ItemCategorySelector) icatSelectors.elementAt(idx);
		} else if (itemCategories.size() > 1) {
			iCatSelector = new ItemCategorySelector();
			iCatSelector.setItemCategories(itemCategories);
			iCatSelector.setScheduleData(sd);
			//iCatSelector.doCategorySelection(0);
			if (icatSelectors == null) {
				icatSelectors = new Vector(5, 5);
				for (int i = 0; i < scheduleData.size(); i++) {
					icatSelectors.addElement(null);
				}
			}
			icatSelectors.setElementAt(iCatSelector, idx);
		}
		if (timeLineCB.getState()) {
			TimeLineView tlv = new TimeLineView(sd.souTbl);
			tlv.setName("Time line: " + sd.linkLayer.getName());
			tlv.setSupervisor(core.getSupervisor());
			if (iCatSelector != null) {
				tlv.setListSelector(iCatSelector);
				tlv.addUIElement(new ItemCategorySelectUI(iCatSelector));
			}
			core.getDisplayProducer().showGraph(tlv);
			if (sd.vehicleTypeColIdx >= 0 || sd.vehicleIdColIdx >= 0 || sd.vehicleHomeIdColIdx >= 0 || sd.vehicleHomeNameColIdx >= 0) {
				Vector sortAttrIds = new Vector(5, 10);
				if (sd.vehicleTypeColIdx >= 0) {
					sortAttrIds.addElement(sd.souTbl.getAttributeId(sd.vehicleTypeColIdx));
				}
				if (sd.vehicleHomeNameColIdx >= 0) {
					sortAttrIds.addElement(sd.souTbl.getAttributeId(sd.vehicleHomeNameColIdx));
				} else if (sd.vehicleHomeIdColIdx >= 0) {
					sortAttrIds.addElement(sd.souTbl.getAttributeId(sd.vehicleHomeIdColIdx));
				}
				if (sd.vehicleIdColIdx >= 0) {
					sortAttrIds.addElement(sd.souTbl.getAttributeId(sd.vehicleIdColIdx));
				}
				if (sortAttrIds.size() > 0) {
					(tlv).sortBy(sortAttrIds);
				}
			}
		}
		if (matrixCB.getState()) {
			if (sd.moveAggregator == null) {
				showMessage("Wait... Data preparation (aggregation) is in progress", false);
				sd.moveAggregator = new MovementAggregator(sd.souTbl, sd.ldd, sd.souNameColIdx, sd.destNameColIdx, sd.itemNumColIdx, sd.vehicleIdColIdx);
				sd.moveAggregator.addPropertyChangeListener(this);
				showMessage(null, false);
				sd.linkLayer.setLayerDrawn(false);
				//build a table and a layer on the basis of the aggregated data
				AggrScheduleLayerBuilder agb = new AggrScheduleLayerBuilder(core, sd);
				agb.buildAggregateLinks();
				if (sd.distancesMatrix == null && sd.ldd.distancesFilePath != null) {
					showMessage("Trying to load distances from " + sd.ldd.distancesFilePath, false);
					DistancesLoader distLoader = new DistancesLoader();
					sd.distancesMatrix = distLoader.loadDistances(sd.ldd.distancesFilePath, core.getDataLoader().getApplicationPath());
/*
          if (sd.distancesMatrix!=null) {
            System.out.println("Distance matrix: N attributes = "+sd.distancesMatrix.getNAttributes()+
            "; N rows = "+sd.distancesMatrix.getNRows()+"; N Columns = "+
            sd.distancesMatrix.getNColumns());
          }
*/
				}
			}
			MovementMatrixPanel matr = new MovementMatrixPanel(core.getSupervisor(), (sd.locLayer == null) ? null : sd.locLayer.getEntitySetIdentifier(), (sd.aggLinkLayer == null) ? null : sd.aggLinkLayer.getEntitySetIdentifier(), sd.souTbl,
					sd.moveAggregator, sd.distancesMatrix, true);
			matr.setName("Movement matrix: " + sd.linkLayer.getName());
			if (iCatSelector != null) {
				matr.addUIElement(new ItemCategorySelectUI(iCatSelector));
			}
			core.getDisplayProducer().showGraph(matr);
		}
		if (vehicleUseCB.getState()) {
			if (vehicleTypesInfo == null) {
				vehicleTypesInfo = loadVehicleTypesInfo(core, null);
				if (vehicleTypesInfo == null) {
					showMessage("Failed to load information about the types of vehicles!", true);
				} else {
					showMessage("Got " + vehicleTypesInfo.vehicleClassIds.size() + " vehicle classes", false);
					if (vehicleTypesInfo.vehicleClassNames != null) {
						showMessage("Got " + vehicleTypesInfo.vehicleClassNames.size() + " vehicle class names", false);
					} else {
						showMessage("Failed to get vehicle class names!", true);
					}
					showMessage("Loaded the vehicle capacity table with " + vehicleTypesInfo.vehicleCap.getDataItemCount() + " records", false);
				}
				if (sd.vehicleInfo != null) {
					fillVehicleClassNames(sd.vehicleInfo, vehicleTypesInfo, vehicleSources);
					fillVehicleCapacities(sd.vehicleInfo, itemCategories, vehicleTypesInfo);
				}
			}
			VehicleCounter vc = new VehicleCounter();
			if (vehicleTypesInfo.vehicleCap != null) {
				vc.setVehicleCapacityTable(vehicleTypesInfo.vehicleCap);
			}
			if (itemCategories != null) {
				vc.setItemCategories(itemCategories);
			}
			if (sd.itemData != null) {
				vc.setItemData(sd.itemData);
			}
			if (vc.countVehicles(sd.souTbl, sd.ldd, sd.vehicleIdColIdx, sd.vehicleTypeColIdx, sd.vehicleHomeIdColIdx, sd.vehicleHomeNameColIdx, sd.itemNumColIdx, sd.itemCatColIdx, vehicleSources)) {
				showMessage("Successfully got data for the vehicle use display!", false);
				if (sd.vehicleInfo == null && vc.vehicleInfo != null) {
					sd.vehicleInfo = vc.vehicleInfo;
					core.getDataLoader().addTable(sd.vehicleInfo);
					fillVehicleClassNames(sd.vehicleInfo, vehicleTypesInfo, vehicleSources);
					fillVehicleCapacities(sd.vehicleInfo, itemCategories, vehicleTypesInfo);
				}
				if (vehicleTypesInfo.vehicleCap != null) {
					vc.computeCapacityUse();
				}
				DestinationUseCounter destUseCounter = new DestinationUseCounter();
				destUseCounter.setItemCategories(itemCategories);
				destUseCounter.setItemCategoriesCodes(itemCatCodes);
				destUseCounter.setDestinationCapacitiesTable(destCap);
				if (!destUseCounter.countDestinationUse(sd.souTbl, sd.ldd, sd.itemNumColIdx, sd.itemCatColIdx, vc.getStartTime())) {
					destUseCounter = null;
				}

				TimeSegmentedBarPanel tsbp = new TimeSegmentedBarPanel(vc, destUseCounter, iCatSelector);
				tsbp.setName("Resource use chart");
				Highlighter hl = null;
				if (sd.vehicleInfo != null) {
					String setId = sd.vehicleInfo.getEntitySetIdentifier();
					if (setId == null) {
						setId = "vehicles";
						sd.vehicleInfo.setEntitySetIdentifier(setId);
					}
					hl = core.getSupervisor().getHighlighter(setId);
				}
				if (hl != null) {
					tsbp.setHighlighterForVehicles(hl);
				}
				hl = core.getSupervisor().getHighlighter(sd.souTbl.getEntitySetIdentifier());
				if (hl != null) {
					tsbp.setHighlighterForItems(hl);
					if (sd.itemData != null) {
						sd.itemData.setEntitySetIdentifier(hl.getEntitySetIdentifier());
					}
				}
				if (destCap != null) {
					hl = core.getSupervisor().getHighlighter(destCap.getEntitySetIdentifier());
					if (hl != null) {
						tsbp.setHighlighterForDestinations(hl);
					}
				}
				VehicleSelector vSel = new VehicleSelector();
				vSel.setScheduleData(sd);
				vSel.setHighlighter(core.getSupervisor().getHighlighter(sd.linkLayer.getEntitySetIdentifier()));
				tsbp.addTimeAndItemsSelectListener(vSel);
				core.getDisplayProducer().showGraph(tsbp);
			} else {
				showMessage("Failed to get data for the vehicle use display...", true);
			}
		}
	}

	public static AttrValueOrderPrefSpec findOrderPrefSpec(DataSourceSpec spec, String attrName) {
		if (spec == null || attrName == null || spec.descriptors == null || spec.descriptors.size() < 1)
			return null;
		for (int i = 0; i < spec.descriptors.size(); i++)
			if (spec.descriptors.elementAt(i) instanceof AttrValueOrderPrefSpec) {
				AttrValueOrderPrefSpec aOrdPref = (AttrValueOrderPrefSpec) spec.descriptors.elementAt(i);
				if (aOrdPref.attrName != null && aOrdPref.attrName.equalsIgnoreCase(attrName))
					return aOrdPref;
			}
		return null;
	}

	public static void orderCategoriesByPreference(AttrValueOrderPrefSpec aOrdPref, Vector itemCategories, Vector itemCatCodes) {
		if (itemCategories == null || itemCategories.size() < 2 || aOrdPref == null)
			return;
		if (aOrdPref.values == null || aOrdPref.values.size() < 2)
			return;
		Vector iCat = (Vector) itemCategories.clone(), iCatCodes = null;
		itemCategories.removeAllElements();
		if (itemCatCodes != null) {
			while (itemCatCodes.size() < iCat.size()) {
				itemCatCodes.addElement(null);
			}
			iCatCodes = (Vector) itemCatCodes.clone();
			itemCatCodes.removeAllElements();
		}
		for (int i = 0; i < aOrdPref.values.size(); i++) {
			int idx = StringUtil.indexOfStringInVectorIgnoreCase((String) aOrdPref.values.elementAt(i), iCat);
			if (idx >= 0) {
				itemCategories.addElement(iCat.elementAt(idx));
				if (itemCatCodes != null) {
					itemCatCodes.addElement(iCatCodes.elementAt(idx));
				}
			}
		}
		if (itemCategories.size() < iCat.size()) {
			for (int i = 0; i < iCat.size(); i++)
				if (!StringUtil.isStringInVectorIgnoreCase((String) iCat.elementAt(i), itemCategories)) {
					itemCategories.addElement(iCat.elementAt(i));
					if (itemCatCodes != null) {
						itemCatCodes.addElement(iCatCodes.elementAt(i));
					}
				}
		}
	}

	protected void showMessage(String msg, boolean error) {
		if (core != null && core.getUI() != null) {
			core.getUI().showMessage(msg, error);
		} else if (error) {
			System.out.println("!--> " + msg);
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (dia != null) {
			dia.dispose();
			dia = null;
		}
		String cmd = e.getActionCommand();
		if (cmd == null)
			return;
		if (cmd.equals("load_schedule")) {
			ScheduleLoader schLoader = new ScheduleLoader();
			schLoader.run(core);
			return;
		}
	}

	/**
	 * Reacts to changes of the aggregated data in a MovementAggregator.
	 * A MovementAggregator changes the aggregated data when the filter of the
	 * source table changes.
	 */
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getSource() instanceof MovementAggregator) {
			if (scheduleData == null)
				return; //seems impossible but it's better to check
			for (int i = 0; i < scheduleData.size(); i++) {
				ScheduleData sd = (ScheduleData) scheduleData.elementAt(i);
				if (sd.moveAggregator != null && e.getSource().equals(sd.moveAggregator)) {
					sd.moveAggregator = null;
					return;
				}
			}
		}
	}

	/**
	 * Loads static data about vehicle types, suitability, and capacities
	 */
	public static VehicleTypesInfo loadVehicleTypesInfo(ESDACore core, String path) {
		if (path == null) {
			path = core.getDataLoader().getApplicationPath();
			if (path != null) {
				path = CopyFile.getDir(path);
			}
		}
		String fileName = "vehicle_classes.csv";
		if (path != null) {
			fileName = path + fileName;
		}
		InputStream stream = openStream(fileName);
		boolean headerGot = false;
		VehicleTypesInfo vti = new VehicleTypesInfo();
		if (stream != null) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
			while (true) {
				String str = null;
				try {
					str = reader.readLine();
				} catch (EOFException eof) {
					break;
				} catch (IOException ioe) {
					core.getUI().showMessage("Error reading vehicle type descriptions: " + ioe, true);
					break;
				}
				if (str == null) {
					break;
				}
				Vector v = StringUtil.getNames(str, ",;\t", true);
				if (v == null || v.size() < 2) {
					continue;
				}
				if (!headerGot) { //skip line with field names
					headerGot = true;
					continue;
				}
				if (vti.vehicleClassIds == null) {
					vti.vehicleClassIds = new Vector(10, 10);
					vti.vehicleClassNames = new Vector(10, 10);
				}
				vti.vehicleClassIds.addElement(v.elementAt(0));
				vti.vehicleClassNames.addElement(v.elementAt(1));
				if (v.size() > 2) { //the line contains speed factor
					String sf = (String) v.elementAt(2);
					try {
						float f = Float.parseFloat(sf);
						if (!Float.isNaN(f) && f > 0) {
							if (vti.speedFactors == null) {
								vti.speedFactors = new FloatArray(10, 10);
							}
							while (vti.speedFactors.size() < vti.vehicleClassIds.size() - 1) {
								vti.speedFactors.addElement(1f);
							}
							vti.speedFactors.addElement(f);
						}
					} catch (NumberFormatException e) {
					}
				}
			}
		}
		closeStream(stream);
		if (vti.speedFactors != null) {
			while (vti.speedFactors.size() < vti.vehicleClassIds.size()) {
				vti.speedFactors.addElement(1f);
			}
		}
		fileName = "vehicle_suit+loading.csv";
		if (path != null) {
			fileName = path + fileName;
		}
		stream = openStream(fileName);
		if (stream == null) {
			fileName = "vehicle_suitability.csv";
			if (path != null) {
				fileName = path + fileName;
			}
			stream = openStream(fileName);
		}
		if (stream != null) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
			while (true) {
				String str = null;
				try {
					str = reader.readLine();
				} catch (EOFException eof) {
					break;
				} catch (IOException ioe) {
					core.getUI().showMessage("Error reading vehicle type descriptions: " + ioe, true);
					break;
				}
				if (str == null) {
					break;
				}
				Vector v = StringUtil.getNames(str, ",;\t", true);
				if (v == null || v.size() < 3) {
					continue;
				}
				if (vti.vehicleCap == null) { //line with field names
					vti.vehicleCap = new DataTable();
					vti.vehicleCap.addAttribute((String) v.elementAt(0), "vehicle_type", AttributeTypes.character);
					vti.vehicleCap.addAttribute((String) v.elementAt(1), "item_type", AttributeTypes.character);
					vti.vehicleCap.addAttribute((String) v.elementAt(2), "capacity", AttributeTypes.integer);
					if (v.size() > 3) {
						vti.vehicleCap.addAttribute((String) v.elementAt(3), "load_time", AttributeTypes.real);
					}
				} else { //line with values
					String vType = (String) v.elementAt(0);
					if (vti.vehicleClassIds == null) {
						vti.vehicleClassIds = new Vector(10, 10);
					}
					if (!StringUtil.isStringInVectorIgnoreCase(vType, vti.vehicleClassIds)) {
						vti.vehicleClassIds.addElement(vType);
						if (vti.vehicleClassNames != null) {
							vti.vehicleClassNames.addElement(vType);
						}
					}
					DataRecord rec = new DataRecord(String.valueOf(vti.vehicleCap.getDataItemCount() + 1));
					vti.vehicleCap.addDataRecord(rec);
					rec.addAttrValue(vType);
					for (int i = 1; i < v.size(); i++) {
						rec.addAttrValue(v.elementAt(i));
					}
				}
			}
		}
		closeStream(stream);
		if (vti.vehicleClassIds == null || vti.vehicleCap == null)
			return null;
		return vti;
	}

	/**
	 * After loading item class names and item class codes, inserts the names in
	 * the table where only codes are initially present
	 */
	public static void fillItemClassNames(DataTable table, Vector itemCategories, Vector itemCatCodes) {
		if (table == null || table.getDataSource() == null || table.getDataItemCount() < 1)
			return;
		if (itemCategories == null || itemCatCodes == null || itemCategories.size() < 1)
			return;
		boolean hasNotNull = false;
		for (int i = 0; i < itemCatCodes.size() && !hasNotNull; i++) {
			hasNotNull = itemCatCodes.elementAt(i) != null;
		}
		if (!hasNotNull)
			return;
		DataSourceSpec dss = (DataSourceSpec) table.getDataSource();
		String attrName = dss.getExtraInfoByKeyAsString("ITEM_CLASS_CODE_FIELD_NAME");
		if (attrName == null)
			return;
		int classCodeColN = table.findAttrByName(attrName);
		if (classCodeColN < 0)
			return;
		attrName = dss.getExtraInfoByKeyAsString("ITEM_CLASS_FIELD_NAME");
		if (attrName != null) {
			int classNameColN = table.findAttrByName(attrName);
			if (classNameColN >= 0)
				return; //already exists
		}
		table.addAttribute("Item class name", "item_class_name", AttributeTypes.character);
		int classNameColN = table.getAttrCount() - 1;
		dss.extraInfo.put("ITEM_CLASS_FIELD_NAME", table.getAttributeName(classNameColN));
		for (int i = 0; i < table.getDataItemCount(); i++) {
			DataRecord rec = table.getDataRecord(i);
			String code = rec.getAttrValueAsString(classCodeColN);
			if (code == null) {
				continue;
			}
			int idx = StringUtil.indexOfStringInVectorIgnoreCase(code, itemCatCodes);
			if (idx < 0) {
				continue; //unknown code
			}
			rec.setAttrValue(itemCategories.elementAt(idx), classNameColN);
		}
	}

	/**
	 * After loading vehicle class names, inserts these names in the table with
	 * the data about the individual vehicles
	 */
	public static void fillVehicleClassNames(DataTable vehicleInfo, VehicleTypesInfo vti, DataTable vehicleSources) {
		if (vehicleInfo == null || vehicleInfo.getDataItemCount() < 1 || vti == null || vti.vehicleClassIds == null || vti.vehicleClassNames == null || vti.vehicleClassIds.size() < 1 || vti.vehicleClassNames.size() < 1)
			return;
		int vTypeAttrIdx = vehicleInfo.getAttrIndex("vehicleType");
		if (vTypeAttrIdx < 0)
			return;
		int vTypeNameAttrIdx = vehicleInfo.getAttrIndex("vehicleTypeName");
		if (vTypeNameAttrIdx >= 0)
			return; //already exists
		fillVehicleClassNames(vehicleInfo, vTypeAttrIdx, vti);
		if (vehicleSources != null && vehicleSources.getDataItemCount() > 0) {
			vTypeNameAttrIdx = vehicleSources.getAttrIndex("vehicleTypeName");
			if (vTypeNameAttrIdx < 0) {
				String attrName = ((DataSourceSpec) vehicleSources.getDataSource()).getExtraInfoByKeyAsString("VEHICLE_CLASS_ID_FIELD_NAME");
				if (attrName != null) {
					int aIdx = vehicleSources.findAttrByName(attrName);
					if (aIdx >= 0) {
						fillVehicleClassNames(vehicleSources, aIdx, vti);
					}
				}
			}
		}
	}

	/**
	 * After loading vehicle class names, inserts these names in the table with
	 * the data about vehicles or vehicle types. The agruments specify the
	 * number of the table column with the attribute type.
	 */
	public static void fillVehicleClassNames(DataTable vehicleInfo, int vTypeAttrIdx, VehicleTypesInfo vti) {
		if (vehicleInfo == null || vehicleInfo.getDataItemCount() < 1 || vti == null || vti.vehicleClassIds == null || vti.vehicleClassNames == null || vti.vehicleClassIds.size() < 1 || vti.vehicleClassNames.size() < 1)
			return;
		if (vTypeAttrIdx < 0)
			return;
		vehicleInfo.addAttribute("Vehicle type description", "vehicleTypeName", AttributeTypes.character);
		int vTypeNameAttrIdx = vehicleInfo.getAttrCount() - 1;
		for (int i = 0; i < vehicleInfo.getDataItemCount(); i++) {
			DataRecord rec = vehicleInfo.getDataRecord(i);
			String typeId = rec.getAttrValueAsString(vTypeAttrIdx);
			if (typeId == null) {
				continue;
			}
			int idx = StringUtil.indexOfStringInVectorIgnoreCase(typeId, vti.vehicleClassIds);
			if (idx < 0 || idx >= vti.vehicleClassNames.size() || vti.vehicleClassNames.elementAt(idx) == null) {
				continue;
			}
			rec.setAttrValue(vti.vehicleClassNames.elementAt(idx), vTypeNameAttrIdx);
		}
	}

	/**
	 * Inserts vehicle capacities in the table with the data about the individual
	 * vehicles
	 */
	public static void fillVehicleCapacities(DataTable vehicleInfo, Vector itemCategories, VehicleTypesInfo vti) {
		if (vehicleInfo == null || vehicleInfo.getDataItemCount() < 1 || vti == null || vti.vehicleCap == null || vti.vehicleCap.getDataItemCount() < 1 || itemCategories == null || itemCategories.size() < 1)
			return;
		if (vehicleInfo.getAttribute("capacities") != null)
			return; //already exists
		int vTypeIdxCap = vti.vehicleCap.getAttrIndex("vehicle_type"), iTypeIdxCap = vti.vehicleCap.getAttrIndex("item_type"), capIdxCap = vti.vehicleCap.getAttrIndex("capacity"), vTypeIdxVeh = vehicleInfo.getAttrIndex("vehicleType");
		if (vTypeIdxCap < 0 || iTypeIdxCap < 0 || capIdxCap < 0 || vTypeIdxVeh < 0)
			return;
		int capIdxVeh = vehicleInfo.getAttrCount();
		if (itemCategories.size() > 1) {
			Parameter par = new Parameter();
			par.setName("item category");
			for (int i = 0; i < itemCategories.size(); i++) {
				par.addValue(itemCategories.elementAt(i));
			}
			vehicleInfo.addParameter(par);
			Attribute parent = new Attribute("capacities", AttributeTypes.integer);
			parent.setName("Capacity");
			for (int i = 0; i < itemCategories.size(); i++) {
				Attribute attr = new Attribute("capacities_" + (i + 1), AttributeTypes.integer);
				attr.addParamValPair(par.getName(), par.getValue(i));
				parent.addChild(attr);
				vehicleInfo.addAttribute(attr);
			}
		} else {
			vehicleInfo.addAttribute("Capacity", "capacities", AttributeTypes.integer);
		}
		for (int i = 0; i < vehicleInfo.getDataItemCount(); i++) {
			DataRecord rec = vehicleInfo.getDataRecord(i);
			for (int k = capIdxVeh; k < vehicleInfo.getAttrCount(); k++) {
				rec.setNumericAttrValue(0f, "0", k);
			}
			String typeId = rec.getAttrValueAsString(vTypeIdxVeh);
			if (typeId == null) {
				continue;
			}
			for (int j = 0; j < vti.vehicleCap.getDataItemCount(); j++)
				if (typeId.equalsIgnoreCase(vti.vehicleCap.getAttrValueAsString(vTypeIdxCap, j))) {
					double cap = vti.vehicleCap.getNumericAttrValue(capIdxCap, j);
					if (Double.isNaN(cap)) {
						continue;
					}
					int icap = (int) Math.round(cap);
					if (icap < 1) {
						continue;
					}
					String itemCat = vti.vehicleCap.getAttrValueAsString(iTypeIdxCap, j);
					int itemCatIdx = 0;
					if (itemCategories.size() > 1) {
						if (itemCat == null) {
							continue;
						} else {
							itemCatIdx = StringUtil.indexOfStringInVectorIgnoreCase(itemCat, itemCategories);
						}
						if (itemCatIdx < 0) {
							continue;
						}
					}
					rec.setNumericAttrValue(icap, String.valueOf(icap), capIdxVeh + itemCatIdx);
				}
		}
	}

	/**
	* Opens the stream on the specified data source. If the source
	* string starts with HTTP or FILE, accesses the source as a URL, otherwise
	* opens it as a local file
	*/
	protected static InputStream openStream(String dataSource) {
		if (dataSource == null)
			return null;
		int idx = dataSource.indexOf(':');
		boolean isURL = false;
		InputStream stream = null;
		if (idx > 0) {
			String pref = dataSource.substring(0, idx);
			if (pref.equalsIgnoreCase("HTTP") || pref.equalsIgnoreCase("FILE")) {
				isURL = true;
			}
		}
		try {
			if (isURL) { //try to access the source as a URL
				URL url = new URL(dataSource);
				stream = url.openStream();
			} else {
				stream = new FileInputStream(dataSource);
			}
		} catch (IOException ioe) {
			System.out.println("Error accessing " + dataSource + ": " + ioe);
			return null;
		}
		return stream;
	}

	protected static void closeStream(InputStream stream) {
		if (stream == null)
			return;
		try {
			stream.close();
		} catch (IOException ioe) {
		}
	}
}
