package spade.analysis.tools.schedule;

import java.awt.Color;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.AttrDataReader;
import spade.analysis.system.DataLoader;
import spade.analysis.system.DataReader;
import spade.analysis.system.DataReaderFactory;
import spade.analysis.system.ESDACore;
import spade.analysis.system.GeoDataReader;
import spade.lib.basicwin.Dialogs;
import spade.lib.basicwin.GetPathDlg;
import spade.lib.lang.Language;
import spade.lib.util.CopyFile;
import spade.lib.util.Matrix;
import spade.lib.util.StringUtil;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DLayerManager;
import spade.vis.dmap.DrawingParameters;
import spade.vis.geometry.GeoComp;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RealPoint;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;
import spade.vis.spec.AttrValueOrderPrefSpec;
import spade.vis.spec.DataSourceSpec;
import spade.vis.spec.LinkDataDescription;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: 01-Nov-2007
 * Time: 11:08:11
 * Prepares input data for the transportation scheduler
 */
public class SchedulerInputPreparer implements ActionListener, WindowListener {

	static ResourceBundle res = Language.getTextResource("spade.analysis.tools.schedule.SchedulerTexts_tools_schedule");

	protected ESDACore core = null;
	/**
	 * Path to the directory with the metadata files
	 */
	public String metadataPath = null;
	/**
	 * Path to the directory with the static data files (vehicle types, suitability, ...)
	 */
	public String staticDataPath = null;
	/**
	 * Path to the directory with the scheduler input files
	 */
	public String schedulerInputPath = null;
	/**
	 * Path to the directory with the scheduler output files
	 */
	public String schedulerOutputPath = null;
	/**
	 * The names of the input files for the scheduler
	 */
	public String sourcesFName = "sources.csv", sourcesExtFName = "sources_ext.csv", destFName = "destinations.csv", vehiclesFName = "vehicles.csv", distancesFName = "distances.csv", allPlacesFName = "places.csv",
			itemClassesFName = "item_classes.csv", vehicleClassesFName = "vehicle_classes.csv", loadTimesFName = "loading_time.csv", vehiclesSuitFName = "vehicle_suitability.csv";
	/**
	 * A layer with endangered objects received from IDAS. The objects may contain
	 * people or items subject to evacuation.
	 */
	protected DGeoLayer objToSave = null;
	/**
	 * A layer with potentially endangered objects received from IDAS.
	 * The objects may contain people or items subject to evacuation.
	 */
	protected DGeoLayer objPossiblyToSave = null;
	/**
	 * A layer with potential shelters received from IDAS.
	 */
	protected DGeoLayer shelters = null;
	/**
	 * The layer with locations which may (or may not) contain source
	 * locations and/or destination locations and/or locations of vehicles
	 */
	public DGeoLayer locLayer = null;
	/**
	 * Indicates whether a new locLayer has been produced and added
	 * to the map
	 */
	public boolean locLayerWasAdded = false;
	/**
	 * Contains data about the original numbers of items in the sources,
	 * which may (or may not) be available from a previous run of the scheduler
	 */
	public DataTable itemsInSources = null;
	/**
	 * Contains data about the available destinations and their capacities,
	 * which may (or may not) be available from a previous run of the scheduler
	 */
	public DataTable destCap = null;
	/**
	 * Contains data about the original numbers of vehicles at their sources,
	 * which may (or may not) be available from a previous run of the scheduler
	 */
	protected DataTable vehiclesInSources = null;
	/**
	 * The geographical layer with the source locations
	 */
	public DGeoLayer souLayer = null;
	/**
	 * The table with data about the sources, in particular, numbers of
	 * items, possibly, belonging to different categories
	 */
	public DataTable souTable = null;
	/**
	 * The geographical layer with the destination locations
	 */
	public DGeoLayer destLayer = null;
	/**
	 * The table with data about the destinations, in particular, capacities,
	 * possibly, several capacities for different categories of items
	 */
	public DataTable destTable = null;
	/**
	 * The geographical layer with the locations of the available vehicles
	 */
	public DGeoLayer vehicleLocLayer = null;
	/**
	 * The table with data about the availability of different types
	 * of vehicles in the locations specified in vehicleLocLayer
	 */
	public DataTable vehicleLocTable = null;
	/**
	 * The matrix with the distances and/or travel times between the source and
	 * destination locations
	 */
	public Matrix distancesMatrix = null;
	/**
	 * Contains the names of the categories of the transported items. Does not
	 * contain "LEER" or "EMPTY"!
	 */
	public Vector itemCatNames = null;
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
	public Vector itemCatCodes = null;
	/**
	 * Contains information about the preferred order of the item categories
	 */
	public AttrValueOrderPrefSpec catOrder = null;
	/**
	 * Contains the total numbers of the items in the sources, by item categories.
	 */
	public int itemCountsByCat[] = null;
	/**
	 * Keeps information about classes of vehicles used for transportation.
	 */
	public VehicleTypesInfo vehicleTypesInfo = null;
	/**
	 * The currently running UI for data editing
	 */
	protected Frame editingUI = null;
	/**
	 * The owner must be notified when the data preparation is finished
	 */
	protected ActionListener owner = null;

	public void setCore(ESDACore core) {
		this.core = core;
	}

	/**
	 * The owner must be notified when the data preparation is finished
	 */
	public void setOwner(ActionListener owner) {
		this.owner = owner;
	}

	/**
	 * Sets a layer with endangered objects, which may contain
	 * people or items subject to evacuation.
	 */
	public void setObjectsToSave(DGeoLayer objToSave) {
		this.objToSave = objToSave;
	}

	/**
	 * Sets a layer with potentially endangered objects, which may contain
	 * people or items subject to evacuation.
	 */
	public void setObjectsPossiblyToSave(DGeoLayer objPossiblyToSave) {
		this.objPossiblyToSave = objPossiblyToSave;
	}

	/**
	 * Sets a layer with locations that may be used as shelters
	 */
	public void setShelters(DGeoLayer shelters) {
		this.shelters = shelters;
	}

	/**
	 * Sets the paths to the directories with the metadata, static data,
	 * scheduler input, and scheduler output
	 */
	public void setPaths(String metadataPath, String staticDataPath, String schedulerInputPath, String schedulerOutputPath) {
		this.metadataPath = metadataPath;
		this.staticDataPath = staticDataPath;
		this.schedulerInputPath = schedulerInputPath;
		this.schedulerOutputPath = schedulerOutputPath;
	}

	/**
	 * Sets the codes and names of the item categories
	 */
	public void setItemCategories(Vector itemCatCodes, Vector itemCatNames) {
		this.itemCatCodes = itemCatCodes;
		this.itemCatNames = itemCatNames;
	}

	/**
	 * Sets a reference to a layer which may (or may not) contain source
	 * locations and/or destination locations and/or locations of vehicles
	 */
	public void setLayerWithPotentialPlaces(DGeoLayer locLayer) {
		this.locLayer = locLayer;
	}

	/**
	 * Sets a reference to a table with the original numbers of items in the sources,
	 * which may be available from a previous run of the scheduler
	 */
	public void setItemsInSources(DataTable itemsInSources) {
		this.itemsInSources = itemsInSources;
	}

	/**
	 * Sets a reference to a table with data about the available destinations and
	 * their capacities, which may be available from a previous run of the scheduler
	 */
	public void setDestinationCapacitiesTable(DataTable destCap) {
		this.destCap = destCap;
	}

	/**
	 * Sets a reference to a table with classes of vehicles used for transportation.
	 */
	public void setVehicleTypesInfo(VehicleTypesInfo vehicleTypesInfo) {
		this.vehicleTypesInfo = vehicleTypesInfo;
	}

	/**
	 * Sets a reference to a table with the original numbers of vehicles at their sources
	 */
	public void setVehiclesInSources(DataTable vehiclesInSources) {
		this.vehiclesInSources = vehiclesInSources;
	}

	/**
	 * The matrix with the distances and/or travel times between the source and
	 * destination locations
	 */
	public void setDistancesMatrix(Matrix distancesMatrix) {
		this.distancesMatrix = distancesMatrix;
	}

	/**
	 * Tries to get the input data left from a previous run of the scheduler.
	 * Returns true if successful.
	 */
	public boolean tryGetPreviousData(String inputPath) {
		if (metadataPath == null) {
			metadataPath = "";
		}
		if (schedulerInputPath == null) {
			schedulerInputPath = "";
		}
		if (staticDataPath == null) {
			staticDataPath = "";
		}
		if (inputPath == null) {
			inputPath = schedulerInputPath;
		}

		ScheduleLoader schLoader = new ScheduleLoader();
		schLoader.setCore(core);
		schLoader.setMetadataPath(metadataPath);
		boolean anythingGot = false;
		String applPath = core.getDataLoader().getApplicationPath();

		DataSourceSpec spec = DataSourceSpec.readMetadata(metadataPath + "orders.descr", applPath);
		if (spec == null) {
			showMessage(DataSourceSpec.getErrorMessage(), true);
		}
		LinkDataDescription ldd = null;
		if (spec != null && spec.descriptors != null && spec.descriptors.size() > 0) {
			for (int i = 0; i < spec.descriptors.size() && ldd == null; i++)
				if (spec.descriptors.elementAt(i) instanceof LinkDataDescription) {
					ldd = (LinkDataDescription) spec.descriptors.elementAt(i);
				}
		}

		if (locLayer == null && core != null && core.getUI().getCurrentMapViewer() != null) {
			//try to find the layer with potential places
			LayerManager lman = core.getUI().getCurrentMapViewer().getLayerManager();
			if (ldd != null && ldd.layerRef != null) {
				GeoLayer layer = null;
				for (int i = 0; i < lman.getLayerCount() && layer == null; i++)
					if (lman.getGeoLayer(i).getContainerIdentifier().equalsIgnoreCase(ldd.layerRef)) {
						layer = lman.getGeoLayer(i);
					}
				if (layer != null && (layer instanceof DGeoLayer)) {
					locLayer = (DGeoLayer) layer;
					anythingGot = true;
				}
			}
			if (locLayer == null && CopyFile.fileExists(inputPath + allPlacesFName)) {
				DataLoader dataLoader = core.getDataLoader();
				DataReaderFactory rfac = dataLoader.getDataReaderFactory();
				DataReader reader = rfac.getReaderOfFormat("CSV");
				if (reader != null && (reader instanceof GeoDataReader) && (reader instanceof AttrDataReader)) {
					DataSourceSpec lspec = new DataSourceSpec();
					lspec.source = inputPath + allPlacesFName;
					lspec.format = "CSV";
					lspec.delimiter = ",";
					lspec.nRowWithFieldNames = 0;
					lspec.idFieldName = "id";
					lspec.nameFieldName = "Name";
					lspec.xCoordFieldName = "X";
					lspec.yCoordFieldName = "Y";
					lspec.extraInfo = new Hashtable();
					lspec.extraInfo.put("SITE_TYPE_FIELD_NAME", "type");
					reader.setDataSource(lspec);
					reader.setUI(core.getUI());
					if (reader.loadData(false)) {
						DGeoLayer layer = ((GeoDataReader) reader).getMapLayer();
						if (layer != null) {
							DrawingParameters dp = layer.getDrawingParameters();
							if (dp == null) {
								dp = new DrawingParameters();
								layer.setDrawingParameters(dp);
							}
							dp.lineColor = Color.yellow;
							dp.lineWidth = 1;
							dp.fillColor = Color.blue;
							dp.fillContours = true;
							dp.drawLabels = true;
							dp.labelColor = Color.gray;
							dp.transparency = 40;
							layer.setContainerIdentifier("_places_");
							layer.setName("Places");
							int idx = lman.getIndexOfLayer(layer.getContainerIdentifier());
							if (idx >= 0) {
								core.removeMapLayer(layer.getContainerIdentifier(), true);
							}
							dataLoader.addMapLayer(layer, -1);
							DataTable table = ((AttrDataReader) reader).getAttrData();
							if (table != null) {
								table.setName(layer.getName());
								dataLoader.setLink(layer, dataLoader.addTable(table));
								layer.setLinkedToTable(true);
							}
							locLayer = layer;
						}
					}
				}
			}
		}
		if (distancesMatrix == null) {
			String dfName = distancesFName;
			if (ldd != null && ldd.distancesFilePath != null && CopyFile.fileExists(inputPath, ldd.distancesFilePath)) {
				dfName = distancesFName = ldd.distancesFilePath;
			}
			ldd.distancesFilePath = inputPath + dfName;
			showMessage(res.getString("try_load_distances_from") + " " + ldd.distancesFilePath, false);
			DistancesLoader distLoader = new DistancesLoader();
			distancesMatrix = distLoader.loadDistances(ldd.distancesFilePath, applPath);
			anythingGot = anythingGot || distancesMatrix != null;
			if (distancesMatrix != null) {
				showMessage(res.getString("loaded_distances_from") + " " + ldd.distancesFilePath, false);
				//correctDistanceMatrix();
			}
		}
		if (itemsInSources == null || catOrder == null) {
			DataSourceSpec ispec = DataSourceSpec.readMetadata(metadataPath + "item_sources.descr", applPath);
			if (ispec != null) {
				if (itemsInSources == null) {
					if (ispec.source != null) {
						sourcesExtFName = ispec.source;
					}
					ispec.source = inputPath + sourcesExtFName;
					itemsInSources = schLoader.loadTableData(ispec);
					anythingGot = anythingGot || itemsInSources != null;
				}
				if (catOrder == null && ispec.descriptors != null && ispec.extraInfo != null) {
					String attrName1 = (String) ispec.extraInfo.get("ITEM_CLASS_FIELD_NAME"), attrName2 = (String) ispec.extraInfo.get("ITEM_CLASS_CODE_FIELD_NAME");
					for (int i = 0; i < ispec.descriptors.size() && catOrder == null; i++)
						if (ispec.descriptors.elementAt(i) instanceof AttrValueOrderPrefSpec) {
							AttrValueOrderPrefSpec aOrdPref = (AttrValueOrderPrefSpec) ispec.descriptors.elementAt(i);
							if (aOrdPref.attrName != null && (aOrdPref.attrName.equalsIgnoreCase(attrName1) || aOrdPref.attrName.equalsIgnoreCase(attrName2))) {
								catOrder = aOrdPref;
							}
						}
					anythingGot = anythingGot || catOrder != null;
				}
			} else {
				showMessage(DataSourceSpec.getErrorMessage(), true);
			}
		}
		boolean gotItemCategories = false;
		if (itemsInSources != null) {
			if (itemCatNames == null) {
				itemCatNames = new Vector(10, 10);
			}
			if (itemCatCodes == null) {
				itemCatCodes = new Vector(10, 10);
			}
			if (getAllItemCategories(itemsInSources, itemCatNames, itemCatCodes) && itemCatNames.size() > 1) {
				anythingGot = gotItemCategories = true;
			}
		}
		if (catOrder == null && spec != null) {
			String attrName = spec.getExtraInfoByKeyAsString("ITEM_CLASS_FIELD_NAME");
			if (attrName != null) {
				catOrder = ScheduleExplorer.findOrderPrefSpec(spec, attrName);
			}
		}
		if (catOrder != null) {
			ScheduleExplorer.orderCategoriesByPreference(catOrder, itemCatNames, itemCatCodes);
		}
		boolean gotDestCap = false;
		if (destCap == null) {
			DataSourceSpec dspec = DataSourceSpec.readMetadata(metadataPath + "destinations.descr", applPath);
			if (dspec != null) {
				if (dspec.source != null) {
					destFName = dspec.source;
				}
				dspec.source = inputPath + destFName;
				destCap = schLoader.loadTableData(dspec);
				anythingGot = gotDestCap = destCap != null;
			} else {
				showMessage(DataSourceSpec.getErrorMessage(), true);
			}
		}
		if (destCap != null && itemCatNames != null && (gotDestCap || gotItemCategories)) {
			ScheduleExplorer.fillItemClassNames(destCap, itemCatNames, itemCatCodes);
		}
		if (vehiclesInSources == null) {
			DataSourceSpec vspec = DataSourceSpec.readMetadata(metadataPath + "vehicle_sources.descr", applPath);
			if (vspec != null) {
				if (vspec.source != null) {
					vehiclesFName = vspec.source;
				}
				vspec.source = inputPath + vehiclesFName;
				vehiclesInSources = schLoader.loadTableData(vspec);
				anythingGot = vehiclesInSources != null;
			} else {
				showMessage(DataSourceSpec.getErrorMessage(), true);
			}
		}
		if (vehicleTypesInfo == null) {
			vehicleTypesInfo = ScheduleExplorer.loadVehicleTypesInfo(core, staticDataPath);
			if (vehicleTypesInfo == null) {
				showMessage(res.getString("failed_load_vehicle_types"), true);
			} else {
				anythingGot = true;
				showMessage(res.getString("Got") + " " + vehicleTypesInfo.vehicleClassIds.size() + " " + res.getString("vehicle_classes"), false);
				if (vehicleTypesInfo.vehicleClassNames != null) {
					showMessage(res.getString("Got") + " " + vehicleTypesInfo.vehicleClassNames.size() + " " + res.getString("vehicle_class_names"), false);
				} else {
					showMessage(res.getString("failed_get_vehicle_class_names"), true);
				}
				showMessage(res.getString("loaded_veh_cap_table_with") + " " + vehicleTypesInfo.vehicleCap.getDataItemCount() + " " + res.getString("records"), false);
			}
		}

		return anythingGot;
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
	 * Prepares input data for the scheduler
	 */
	public boolean prepareInputData() {
		if (locLayer != null && !locLayer.hasData()) {
			locLayer.loadData();
		}
		if ((souLayer == null || souTable == null) && itemsInSources != null && locLayer != null) {
			setSources(itemsInSources, locLayer);
		}
		if ((destLayer == null || destTable == null) && destCap != null && locLayer != null) {
			setDestinations(destCap, locLayer);
		}
		if ((vehicleLocLayer == null || vehicleLocTable == null) && vehiclesInSources != null && locLayer != null) {
			setVehicleLocations(vehiclesInSources, locLayer);
		}
		editSouTable();
		return souLayer != null && souTable != null && destLayer != null && destTable != null && vehicleTypesInfo != null && vehicleLocLayer != null && vehicleLocTable != null;
	}

	/**
	 * Asks the user to check and edit the data about the items in the sources
	 */
	protected void editSouTable() {
		if (souTable == null) {
			//create the table
			DataSourceSpec ispec = DataSourceSpec.readMetadata(metadataPath + "item_sources.descr", core.getDataLoader().getApplicationPath());
			if (ispec == null) {
				ispec = new DataSourceSpec();
				ispec.format = "CSV";
				ispec.delimiter = ",";
				ispec.nRowWithFieldNames = 0;
			}
			souTable = new DataTable();
			souTable.setName(res.getString("Item_sources"));
			if (ispec.extraInfo == null) {
				ispec.extraInfo = new Hashtable();
			}
			ispec.name = souTable.getName();
			ispec.source = null;
			souTable.setDataSource(ispec);
			String aName = (String) ispec.extraInfo.get("SOURCE_ID_FIELD_NAME");
			if (aName == null) {
				aName = "id";
				ispec.extraInfo.put("SOURCE_ID_FIELD_NAME", aName);
			}
			souTable.addAttribute(aName, "source_id", AttributeTypes.character);
			aName = (String) ispec.extraInfo.get("SOURCE_NAME_FIELD_NAME");
			if (aName == null) {
				aName = "Name";
				ispec.extraInfo.put("SOURCE_NAME_FIELD_NAME", aName);
			}
			souTable.addAttribute(aName, "source_name", AttributeTypes.character);
			aName = (String) ispec.extraInfo.get("SITE_TYPE_FIELD_NAME");
			if (aName == null) {
				aName = "type";
				ispec.extraInfo.put("SITE_TYPE_FIELD_NAME", aName);
			}
			souTable.addAttribute(aName, "source_type", AttributeTypes.character);
			aName = (String) ispec.extraInfo.get("ITEM_CLASS_CODE_FIELD_NAME");
			if (aName == null) {
				aName = "item class code";
				ispec.extraInfo.put("ITEM_CLASS_CODE_FIELD_NAME", aName);
			}
			souTable.addAttribute(aName, "item_class_code", AttributeTypes.character);
			aName = (String) ispec.extraInfo.get("ITEM_CLASS_FIELD_NAME");
			if (aName == null) {
				aName = "item class name";
				ispec.extraInfo.put("ITEM_CLASS_FIELD_NAME", aName);
			}
			souTable.addAttribute(aName, "item_class_name", AttributeTypes.character);
			aName = (String) ispec.extraInfo.get("ITEM_NUMBER_FIELD_NAME");
			if (aName == null) {
				aName = "number of items";
				ispec.extraInfo.put("ITEM_NUMBER_FIELD_NAME", aName);
			}
			souTable.addAttribute(aName, "item_number", AttributeTypes.integer);
			aName = (String) ispec.extraInfo.get("TIME_LIMIT_FIELD_NAME");
			if (aName == null) {
				aName = "available time (min.)";
				ispec.extraInfo.put("TIME_LIMIT_FIELD_NAME", aName);
			}
			souTable.addAttribute(aName, "avail_time", AttributeTypes.integer);

			souLayer = new DGeoLayer();
			souLayer.setName(souTable.getName());
			souLayer.setContainerIdentifier("_item_sources_");
			souLayer.setType(Geometry.point);
			DrawingParameters dp = souLayer.getDrawingParameters();
			if (dp == null) {
				dp = new DrawingParameters();
				souLayer.setDrawingParameters(dp);
			}
			dp.lineColor = Color.red;
			dp.lineWidth = 1;
			dp.fillColor = Color.red;
			dp.fillContours = true;
			dp.drawLabels = true;
			dp.labelColor = Color.gray;
			dp.hlWidth = 5;
		}

		if (staticDataPath == null) {
			staticDataPath = "";
		}
		if (CopyFile.fileExists(staticDataPath + itemClassesFName)) {
			ScheduleLoader schLoader = new ScheduleLoader();
			schLoader.setCore(core);
			schLoader.setMetadataPath(metadataPath);
			DataSourceSpec spec = new DataSourceSpec();
			spec.source = staticDataPath + itemClassesFName;
			spec.format = "CSV";
			spec.delimiter = ",";
			spec.nRowWithFieldNames = 0;
			DataTable table = schLoader.loadTableData(spec);
			if (table != null && table.getDataItemCount() > 0 && table.getAttrCount() >= 2) {
				int idCN = table.findAttrByName("id"), nameCN = table.findAttrByName("description");
				if (idCN < 0) {
					table.findAttrByName("code");
				}
				if (idCN < 0) {
					table.findAttrByName("class code");
				}
				if (idCN < 0) {
					table.findAttrByName("category code");
				}
				if (idCN < 0) {
					idCN = 0;
				}
				if (nameCN < 0) {
					table.findAttrByName("name");
				}
				if (nameCN < 0) {
					table.findAttrByName("class name");
				}
				if (nameCN < 0) {
					table.findAttrByName("category name");
				}
				if (nameCN < 0) {
					nameCN = 1;
				}
				if (itemCatNames == null) {
					itemCatNames = new Vector(10, 10);
				}
				if (itemCatCodes == null) {
					itemCatCodes = new Vector(10, 10);
				}
				for (int i = 0; i < table.getDataItemCount(); i++) {
					String code = table.getAttrValueAsString(idCN, i), name = table.getAttrValueAsString(nameCN, i);
					if (code == null || name == null || code.equals("0") || name.equalsIgnoreCase("LEER") || name.equalsIgnoreCase("EMPTY")) {
						continue;
					}
					if (StringUtil.isStringInVectorIgnoreCase(code, itemCatCodes)) {
						continue;
					}
					itemCatCodes.addElement(code);
					itemCatNames.addElement(name);
				}
			}
			if (catOrder == null) {
				DataSourceSpec ospec = DataSourceSpec.readMetadata(metadataPath + "orders.descr", core.getDataLoader().getApplicationPath());
				if (ospec != null) {
					String attrName = ospec.getExtraInfoByKeyAsString("ITEM_CLASS_FIELD_NAME");
					if (attrName != null) {
						catOrder = ScheduleExplorer.findOrderPrefSpec(ospec, attrName);
					}
				}
			}
			if (catOrder != null) {
				ScheduleExplorer.orderCategoriesByPreference(catOrder, itemCatNames, itemCatCodes);
			}
		}
		DGeoLayer addLayer1 = null, addLayer2 = null;
		if (objToSave != null && objToSave.getObjectCount() < 1) {
			objToSave = null;
		}
		if (objPossiblyToSave != null && objPossiblyToSave.getObjectCount() < 1) {
			objPossiblyToSave = null;
		}
		if (objToSave != null || objPossiblyToSave != null) {
			int nNewObjToSave = (objToSave != null) ? objToSave.getObjectCount() : 0;
			int nNewObjPossToSave = (objPossiblyToSave != null) ? objPossiblyToSave.getObjectCount() : 0;
			int siteIdCN = -1;
			DataSourceSpec spec = (DataSourceSpec) souTable.getDataSource();
			if (spec != null && spec.extraInfo != null) {
				siteIdCN = souTable.findAttrByName((String) spec.extraInfo.get("SOURCE_ID_FIELD_NAME"));
			}
			for (int i = souLayer.getObjectCount() - 1; i >= 0; i--) {
				//remove objects that do not occur in objToSave or objPossiblyToSave
				String id = souLayer.getObjectId(i);
				boolean save = objToSave != null && objToSave.findObjectById(id) != null;
				boolean possiblySave = !save && objPossiblyToSave != null && objPossiblyToSave.findObjectById(id) != null;
				if (!save && !possiblySave) {
					souLayer.removeGeoObject(i);
					if (siteIdCN >= 0) {
						for (int j = souTable.getDataItemCount() - 1; j >= 0; j--)
							if (id.equalsIgnoreCase(souTable.getAttrValueAsString(siteIdCN, j))) {
								souTable.removeDataItem(j);
							}
					}
				} else {
					if (save) {
						--nNewObjToSave;
					}
					if (possiblySave) {
						--nNewObjPossToSave;
					}
				}
			}
			if (nNewObjToSave > 0) {
				addLayer1 = objToSave;
			}
			if (nNewObjPossToSave > 0) {
				addLayer2 = objPossiblyToSave;
			}
		}

		itemCountsByCat = null;

		ItemsInSourcesEditUI eui = new ItemsInSourcesEditUI(souLayer, souTable, itemCatNames, itemCatCodes, locLayer, addLayer1, addLayer2);
		eui.setCore(core);
		eui.setOwner(this);
		eui.addWindowListener(this);
		editingUI = eui;
		core.getWindowManager().registerWindow(editingUI);
		if (souTable.getDataItemCount() < 1) {
			eui.addSites();
		}
	}

	/**
	 * Computes the numbers of the items to evacuate by item categories.
	 */
	public int[] countItemsInSources() {
		if (souTable == null || souTable.getDataItemCount() < 1)
			return null;
		DataSourceSpec spec = (DataSourceSpec) souTable.getDataSource();
		if (spec == null || spec.extraInfo == null)
			return null;
		int itemNumCN = souTable.findAttrByName((String) spec.extraInfo.get("ITEM_NUMBER_FIELD_NAME"));
		if (itemNumCN < 0)
			return null;
		int itemCatCN = souTable.findAttrByName((String) spec.extraInfo.get("ITEM_CLASS_FIELD_NAME"));
		int itemCatCodeCN = souTable.findAttrByName((String) spec.extraInfo.get("ITEM_CLASS_CODE_FIELD_NAME"));
		int ncat = 1;
		if (itemCatCodes != null && itemCatCodes.size() > 1) {
			ncat = itemCatCodes.size();
		} else if (itemCatNames != null && itemCatNames.size() > 1) {
			ncat = itemCatNames.size();
		}
		int counts[] = new int[ncat];
		for (int i = 0; i < ncat; i++) {
			counts[i] = 0;
		}
		for (int i = 0; i < souTable.getDataItemCount(); i++) {
			DataRecord rec = souTable.getDataRecord(i);
			double val = rec.getNumericAttrValue(itemNumCN);
			if (Double.isNaN(val) || val < 1) {
				continue;
			}
			if (ncat == 1) {
				counts[0] += (int) Math.round(val);
			} else {
				int catIdx = -1;
				String cat = rec.getAttrValueAsString(itemCatCodeCN);
				if (cat != null && itemCatCodes != null) {
					catIdx = StringUtil.indexOfStringInVectorIgnoreCase(cat, itemCatCodes);
				}
				if (catIdx < 0) {
					cat = rec.getAttrValueAsString(itemCatCN);
					if (cat != null && itemCatNames != null) {
						catIdx = StringUtil.indexOfStringInVectorIgnoreCase(cat, itemCatNames);
					}
				}
				if (catIdx < 0) {
					showMessage("Unrecognised category in the table with the item sources: " + cat, true);
				} else {
					counts[catIdx] += (int) Math.round(val);
				}
			}
		}
		return counts;
	}

	/**
	 * Asks the user to check and edit the data about the destinations
	 */
	protected void editDestinationsTable() {
		if (destTable == null) {
			//create the table
			DataSourceSpec spec = DataSourceSpec.readMetadata(metadataPath + "destinations.descr", core.getDataLoader().getApplicationPath());
			if (spec == null) {
				spec = new DataSourceSpec();
				spec.format = "CSV";
				spec.delimiter = ",";
				spec.nRowWithFieldNames = 0;
			}
			destTable = new DataTable();
			destTable.setName(res.getString("Destinations"));
			if (spec.extraInfo == null) {
				spec.extraInfo = new Hashtable();
			}
			spec.name = destTable.getName();
			spec.source = null;
			destTable.setDataSource(spec);
			String aName = (String) spec.extraInfo.get("ID_FIELD_NAME");
			if (aName == null) {
				aName = "id";
				spec.extraInfo.put("ID_FIELD_NAME", aName);
			}
			destTable.addAttribute(aName, "site_id", AttributeTypes.character);
			aName = (String) spec.extraInfo.get("NAME_FIELD_NAME");
			if (aName == null) {
				aName = "Name";
				spec.extraInfo.put("NAME_FIELD_NAME", aName);
			}
			destTable.addAttribute(aName, "site_name", AttributeTypes.character);
			aName = (String) spec.extraInfo.get("SITE_TYPE_FIELD_NAME");
			if (aName == null) {
				aName = "type";
				spec.extraInfo.put("SITE_TYPE_FIELD_NAME", aName);
			}
			destTable.addAttribute(aName, "site_type", AttributeTypes.character);
			aName = (String) spec.extraInfo.get("ITEM_CLASS_CODE_FIELD_NAME");
			if (aName == null) {
				aName = "item class";
				spec.extraInfo.put("ITEM_CLASS_CODE_FIELD_NAME", aName);
			}
			destTable.addAttribute(aName, "item_class_code", AttributeTypes.character);
			aName = (String) spec.extraInfo.get("ITEM_CLASS_FIELD_NAME");
			if (aName != null) {
				aName = "item class name";
				spec.extraInfo.put("ITEM_CLASS_FIELD_NAME", aName);
				destTable.addAttribute(aName, "item_class_name", AttributeTypes.character);
			}
			aName = (String) spec.extraInfo.get("CAPACITY_FIELD_NAME");
			if (aName == null) {
				aName = "capacity";
				spec.extraInfo.put("CAPACITY_FIELD_NAME", aName);
			}
			destTable.addAttribute(aName, "capacity", AttributeTypes.integer);

			destLayer = new DGeoLayer();
			destLayer.setName(destTable.getName());
			destLayer.setContainerIdentifier("_destinations_");
			destLayer.setType(Geometry.point);
			DrawingParameters dp = destLayer.getDrawingParameters();
			if (dp == null) {
				dp = new DrawingParameters();
				destLayer.setDrawingParameters(dp);
			}
			dp.lineColor = Color.blue;
			dp.lineWidth = 1;
			dp.fillColor = Color.orange;
			dp.fillContours = true;
			dp.drawLabels = true;
			dp.labelColor = Color.gray;
			dp.hlWidth = 5;
		}
		if (itemCountsByCat == null) {
			itemCountsByCat = countItemsInSources();
		}
		int siteIdCN = -1;
		DataSourceSpec spec = (DataSourceSpec) destTable.getDataSource();
		if (spec != null) {
			String aName = null;
			if (spec.extraInfo != null) {
				aName = (String) spec.extraInfo.get("ID_FIELD_NAME");
			}
			if (aName == null) {
				aName = spec.idFieldName;
			}
			if (aName == null) {
				aName = "id";
			}
			siteIdCN = destTable.findAttrByName(aName);
		}
		int nNewShelters = 0;
		if (shelters != null && shelters.getObjectCount() > 0) {
			nNewShelters = shelters.getObjectCount();
			for (int i = destLayer.getObjectCount() - 1; i >= 0; i--) {
				String id = destLayer.getObjectId(i);
				if (shelters.findObjectById(id) != null) {
					--nNewShelters;
				}
			}
		}
		if (souLayer != null && souLayer.getObjectCount() > 0) {
			for (int i = destLayer.getObjectCount() - 1; i >= 0; i--) {
				//remove objects that occur in souLayer
				String id = destLayer.getObjectId(i);
				if (souLayer.findObjectById(id) != null) {
					destLayer.removeGeoObject(i);
					if (siteIdCN >= 0) {
						for (int j = destTable.getDataItemCount() - 1; j >= 0; j--)
							if (id.equalsIgnoreCase(destTable.getAttrValueAsString(siteIdCN, j))) {
								destTable.removeDataItem(j);
							}
					}
				}
			}
		}
		DestinationsEditUI eui = new DestinationsEditUI(destLayer, destTable, itemCatNames, itemCatCodes, itemCountsByCat, souLayer, (nNewShelters > 0) ? shelters : locLayer);
		eui.setCore(core);
		eui.setOwner(this);
		eui.addWindowListener(this);
		editingUI = eui;
		core.getWindowManager().registerWindow(editingUI);
		if (destTable.getDataItemCount() < 1) {
			eui.addSites();
		}
	}

	/**
	 * Asks the user to check and edit the data about the sources of the vehicles
	 */
	protected void editVehicleLocTable() {
		if (vehicleLocTable == null) {
			//create the table
			DataSourceSpec spec = DataSourceSpec.readMetadata(metadataPath + "vehicle_sources.descr", core.getDataLoader().getApplicationPath());
			if (spec == null) {
				spec = new DataSourceSpec();
				spec.format = "CSV";
				spec.delimiter = ",";
				spec.nRowWithFieldNames = 0;
			}
			vehicleLocTable = new DataTable();
			vehicleLocTable.setName(res.getString("Vehicle_locations"));
			if (spec.extraInfo == null) {
				spec.extraInfo = new Hashtable();
			}
			spec.name = vehicleLocTable.getName();
			spec.source = null;
			vehicleLocTable.setDataSource(spec);
			String aName = (String) spec.extraInfo.get("SOURCE_ID_FIELD_NAME");
			if (aName == null) {
				aName = "id";
				spec.extraInfo.put("SOURCE_ID_FIELD_NAME", aName);
			}
			vehicleLocTable.addAttribute(aName, "site_id", AttributeTypes.character);
			aName = (String) spec.extraInfo.get("SOURCE_NAME_FIELD_NAME");
			if (aName == null) {
				aName = "Name";
				spec.extraInfo.put("SOURCE_NAME_FIELD_NAME", aName);
			}
			vehicleLocTable.addAttribute(aName, "site_name", AttributeTypes.character);
			aName = (String) spec.extraInfo.get("VEHICLE_CLASS_ID_FIELD_NAME");
			if (aName == null) {
				aName = "vehicle class";
				spec.extraInfo.put("VEHICLE_CLASS_ID_FIELD_NAME", aName);
			}
			vehicleLocTable.addAttribute(aName, "vehicle_type", AttributeTypes.character);
			aName = (String) spec.extraInfo.get("VEHICLE_NUMBER_FIELD_NAME");
			if (aName == null) {
				aName = "number of vehicles";
				spec.extraInfo.put("VEHICLE_NUMBER_FIELD_NAME", aName);
			}
			vehicleLocTable.addAttribute(aName, "vehicle_number", AttributeTypes.integer);
			aName = (String) spec.extraInfo.get("READY_TIME_FIELD_NAME");
			if (aName == null) {
				aName = "ready time";
				spec.extraInfo.put("READY_TIME_FIELD_NAME", aName);
			}
			vehicleLocTable.addAttribute(aName, "ready_time", AttributeTypes.integer);

			vehicleLocLayer = new DGeoLayer();
			vehicleLocLayer.setName(vehicleLocTable.getName());
			vehicleLocLayer.setContainerIdentifier("_vehicle_locations_");
			vehicleLocLayer.setType(Geometry.point);
			DrawingParameters dp = vehicleLocLayer.getDrawingParameters();
			if (dp == null) {
				dp = new DrawingParameters();
				vehicleLocLayer.setDrawingParameters(dp);
			}
			dp.lineColor = Color.magenta;
			dp.lineWidth = 1;
			dp.fillColor = Color.magenta;
			dp.fillContours = true;
			dp.drawLabels = true;
			dp.labelColor = Color.gray;
			dp.hlWidth = 5;
		}

		if (itemCountsByCat == null) {
			itemCountsByCat = countItemsInSources();
		}
		VehiclesInSourcesEditUI eui = new VehiclesInSourcesEditUI(vehicleLocLayer, vehicleLocTable, vehicleTypesInfo, itemCatNames, itemCatCodes, itemCountsByCat, locLayer, souLayer, destLayer);
		eui.setCore(core);
		eui.setOwner(this);
		eui.addWindowListener(this);
		editingUI = eui;
		core.getWindowManager().registerWindow(editingUI);
		if (vehicleLocTable.getDataItemCount() < 1) {
			eui.addSites();
		}
	}

	/**
	 * Uses the given table with data about sources (which may be e.g. left from a previous
	 * run of the scheduler) and layer with locations (suposedly containing the source
	 * locations), builds the souLayer and souTable. Returns true if successful.
	 */
	public boolean setSources(DataTable sourceData, DGeoLayer locLayer) {
		if (sourceData == null || locLayer == null || !sourceData.hasData() || !locLayer.hasData())
			return false;
		DataSourceSpec spec = (DataSourceSpec) sourceData.getDataSource();
		if (spec == null || spec.extraInfo == null)
			return false;
		int souIdCN1 = -1, souNameCN1 = -1, souTypeCN1 = -1, itemCatCN1 = -1, itemCatCodeCN1 = -1, itemNumCN1 = -1, timeLimitCN1 = -1;
		if (spec.extraInfo.get("SOURCE_ID_FIELD_NAME") != null) {
			souIdCN1 = sourceData.findAttrByName((String) spec.extraInfo.get("SOURCE_ID_FIELD_NAME"));
		}
		if (spec.extraInfo.get("SOURCE_NAME_FIELD_NAME") != null) {
			souNameCN1 = sourceData.findAttrByName((String) spec.extraInfo.get("SOURCE_NAME_FIELD_NAME"));
		}
		if (spec.extraInfo.get("SITE_TYPE_FIELD_NAME") != null) {
			souTypeCN1 = sourceData.findAttrByName((String) spec.extraInfo.get("SITE_TYPE_FIELD_NAME"));
		}
		if (spec.extraInfo.get("ITEM_CLASS_FIELD_NAME") != null) {
			itemCatCN1 = sourceData.findAttrByName((String) spec.extraInfo.get("ITEM_CLASS_FIELD_NAME"));
		}
		if (spec.extraInfo.get("ITEM_CLASS_CODE_FIELD_NAME") != null) {
			itemCatCodeCN1 = sourceData.findAttrByName((String) spec.extraInfo.get("ITEM_CLASS_CODE_FIELD_NAME"));
		}
		if (spec.extraInfo.get("ITEM_NUMBER_FIELD_NAME") != null) {
			itemNumCN1 = sourceData.findAttrByName((String) spec.extraInfo.get("ITEM_NUMBER_FIELD_NAME"));
		}
		if (spec.extraInfo.get("TIME_LIMIT_FIELD_NAME") != null) {
			timeLimitCN1 = sourceData.findAttrByName((String) spec.extraInfo.get("TIME_LIMIT_FIELD_NAME"));
		}
		if (itemNumCN1 < 0 || souIdCN1 < 0)
			return false;

		Vector souIds = new Vector(sourceData.getDataItemCount(), 1);
		Vector souPlaces = new Vector(sourceData.getDataItemCount(), 1);

		for (int i = 0; i < sourceData.getDataItemCount(); i++) {
			DataRecord rec1 = sourceData.getDataRecord(i);
			String souId = rec1.getAttrValueAsString(souIdCN1);
			if (souId == null) {
				continue;
			}
			if (StringUtil.isStringInVectorIgnoreCase(souId, souIds)) {
				continue;
			}
			DGeoObject geo1 = (DGeoObject) locLayer.findObjectById(souId);
			if (geo1 == null) {
				continue;
			}
			String souName = null;
			if (souNameCN1 >= 0) {
				souName = rec1.getAttrValueAsString(souNameCN1);
			}
			if (souName == null) {
				souName = geo1.getName();
			}
			DGeoObject geo2 = (DGeoObject) geo1.makeCopy();
			geo2.setIdentifier(souId);
			if (souName != null) {
				geo2.setLabel(souName);
			}
			souPlaces.addElement(geo2);
			souIds.addElement(souId);
		}
		if (souIds.size() < 1)
			return false;

		souLayer = new DGeoLayer();
		souLayer.setName(res.getString("Item_sources"));
		souLayer.setContainerIdentifier("_item_sources_");
		souLayer.setType(Geometry.point);
		souLayer.setGeoObjects(souPlaces, true);
		DrawingParameters dp = souLayer.getDrawingParameters();
		if (dp == null) {
			dp = new DrawingParameters();
			souLayer.setDrawingParameters(dp);
		}
		dp.lineColor = Color.red;
		dp.lineWidth = 1;
		dp.fillColor = Color.red;
		dp.fillContours = true;
		dp.drawLabels = true;
		dp.labelColor = Color.gray;
		dp.hlWidth = 5;

		souTable = new DataTable();
		souTable.setName(res.getString("Item_sources"));
		//the structure of the table must be the same as the structure of the input table
		DataSourceSpec spec2 = (DataSourceSpec) spec.clone();
		spec2.source = null;
		souTable.setDataSource(spec2);
		int souIdCN2 = -1, souNameCN2 = -1, souTypeCN2 = -1, itemCatCN2 = -1, itemCatCodeCN2 = -1, itemNumCN2 = -1, timeLimitCN2 = -1;
		souTable.addAttribute((String) spec2.extraInfo.get("SOURCE_ID_FIELD_NAME"), "source_id", AttributeTypes.character);
		souIdCN2 = souTable.getAttrCount() - 1;
		String aName = (String) spec2.extraInfo.get("SOURCE_NAME_FIELD_NAME");
		if (aName == null) {
			aName = "Name";
			spec2.extraInfo.put("SOURCE_NAME_FIELD_NAME", aName);
		}
		souTable.addAttribute(aName, "source_name", AttributeTypes.character);
		souNameCN2 = souTable.getAttrCount() - 1;
		aName = (String) spec2.extraInfo.get("SITE_TYPE_FIELD_NAME");
		if (aName != null) {
			souTable.addAttribute(aName, "source_type", AttributeTypes.character);
			souTypeCN2 = souTable.getAttrCount() - 1;
		}
		aName = (String) spec2.extraInfo.get("ITEM_CLASS_CODE_FIELD_NAME");
		if (aName == null && itemCatCodes != null && itemCatCodes.size() > 0) {
			aName = "item class code";
			spec2.extraInfo.put("ITEM_CLASS_CODE_FIELD_NAME", aName);
		}
		if (aName != null) {
			souTable.addAttribute(aName, "item_class_code", AttributeTypes.character);
			itemCatCodeCN2 = souTable.getAttrCount() - 1;
		}
		aName = (String) spec2.extraInfo.get("ITEM_CLASS_FIELD_NAME");
		if (aName == null && itemCatNames != null && itemCatNames.size() > 0) {
			aName = "item class name";
			spec2.extraInfo.put("ITEM_CLASS_FIELD_NAME", aName);
		}
		if (aName != null) {
			souTable.addAttribute(aName, "item_class_name", AttributeTypes.character);
			itemCatCN2 = souTable.getAttrCount() - 1;
		}
		aName = (String) spec2.extraInfo.get("ITEM_NUMBER_FIELD_NAME");
		souTable.addAttribute(aName, "item_number", AttributeTypes.integer);
		itemNumCN2 = souTable.getAttrCount() - 1;
		aName = (String) spec2.extraInfo.get("TIME_LIMIT_FIELD_NAME");
		if (aName == null) {
			aName = "available time (min.)";
			spec2.extraInfo.put("TIME_LIMIT_FIELD_NAME", aName);
		}
		souTable.addAttribute(aName, "avail_time", AttributeTypes.integer);
		timeLimitCN2 = souTable.getAttrCount() - 1;

		for (int i = 0; i < sourceData.getDataItemCount(); i++) {
			DataRecord rec1 = sourceData.getDataRecord(i);
			String souId = rec1.getAttrValueAsString(souIdCN1);
			if (souId == null) {
				continue;
			}
			DGeoObject geo = (DGeoObject) souLayer.findObjectById(souId);
			if (geo == null) {
				continue;
			}
			DataRecord rec2 = new DataRecord(souId + "_" + (i + 1), geo.getName());
			souTable.addDataRecord(rec2);
			rec2.setAttrValue(souId, souIdCN2);
			rec2.setAttrValue(geo.getName(), souNameCN2);
			if (souTypeCN1 >= 0 && souTypeCN2 >= 0) {
				rec2.setAttrValue(rec1.getAttrValue(souTypeCN1), souTypeCN2);
			}
			if (itemCatCodeCN1 >= 0 && itemCatCodeCN2 >= 0) {
				rec2.setAttrValue(rec1.getAttrValue(itemCatCodeCN1), itemCatCodeCN2);
			}
			if (itemCatCN1 >= 0 && itemCatCN2 >= 0) {
				rec2.setAttrValue(rec1.getAttrValue(itemCatCN1), itemCatCN2);
			}
			double val = rec1.getNumericAttrValue(itemNumCN1);
			int num = (Double.isNaN(val)) ? 0 : (int) Math.round(val);
			if (num < 0) {
				num = 0;
			}
			rec2.setNumericAttrValue(num, String.valueOf(num), itemNumCN2);
			if (timeLimitCN1 >= 0) {
				val = rec1.getNumericAttrValue(timeLimitCN1);
				if (!Double.isNaN(val) && val > 0) {
					num = (int) Math.round(val);
					rec2.setNumericAttrValue(num, String.valueOf(num), timeLimitCN2);
				}
			}
		}

		return true;
	}

	/**
	 * Uses the given table with data about destinations (which may be e.g. left from a previous
	 * run of the scheduler) and layer with locations (suposedly containing the destination
	 * locations), builds the destLayer and destTable. Returns true if successful.
	 */
	public boolean setDestinations(DataTable destData, DGeoLayer locLayer) {
		if (destData == null || locLayer == null || !destData.hasData() || !locLayer.hasData())
			return false;
		DataSourceSpec spec = (DataSourceSpec) destData.getDataSource();
		if (spec == null || spec.extraInfo == null)
			return false;
		int siteIdCN1 = -1, siteNameCN1 = -1, siteTypeCN1 = -1, itemCatCN1 = -1, itemCatCodeCN1 = -1, capacityCN1 = -1;
		String aName = (String) spec.extraInfo.get("ID_FIELD_NAME");
		if (aName == null) {
			aName = spec.idFieldName;
		}
		if (aName == null) {
			aName = "id";
		}
		siteIdCN1 = destData.findAttrByName(aName);
		aName = (String) spec.extraInfo.get("NAME_FIELD_NAME");
		if (aName == null) {
			aName = spec.nameFieldName;
		}
		if (aName == null) {
			aName = "name";
		}
		siteNameCN1 = destData.findAttrByName(aName);
		aName = (String) spec.extraInfo.get("SITE_TYPE_FIELD_NAME");
		if (aName == null) {
			aName = "type";
		}
		siteTypeCN1 = destData.findAttrByName(aName);
		aName = (String) spec.extraInfo.get("ITEM_CLASS_CODE_FIELD_NAME");
		if (aName == null) {
			aName = "item class";
		}
		itemCatCodeCN1 = destData.findAttrByName(aName);
		aName = (String) spec.extraInfo.get("ITEM_CLASS_FIELD_NAME");
		if (aName == null) {
			aName = "item class name";
		}
		itemCatCN1 = destData.findAttrByName(aName);
		aName = (String) spec.extraInfo.get("CAPACITY_FIELD_NAME");
		if (aName == null) {
			aName = "capacity";
		}
		capacityCN1 = destData.findAttrByName(aName);
		if (capacityCN1 < 0)
			return false;

		Vector siteIds = new Vector(destData.getDataItemCount(), 1);
		Vector sites = new Vector(destData.getDataItemCount(), 1);

		for (int i = 0; i < destData.getDataItemCount(); i++) {
			DataRecord rec1 = destData.getDataRecord(i);
			String siteId = (siteIdCN1 >= 0) ? rec1.getAttrValueAsString(siteIdCN1) : rec1.getId();
			if (siteId == null) {
				continue;
			}
			if (rec1.getNumericAttrValue(capacityCN1) < 1) {
				continue;
			}
			if (StringUtil.isStringInVectorIgnoreCase(siteId, siteIds)) {
				continue;
			}
			DGeoObject geo1 = (DGeoObject) locLayer.findObjectById(siteId);
			if (geo1 == null) {
				continue;
			}
			String siteName = (siteNameCN1 >= 0) ? rec1.getAttrValueAsString(siteNameCN1) : rec1.getName();
			if (siteName == null) {
				siteName = geo1.getName();
			}
			DGeoObject geo2 = (DGeoObject) geo1.makeCopy();
			geo2.setIdentifier(siteId);
			if (siteName != null) {
				geo2.setLabel(siteName);
			}
			sites.addElement(geo2);
			siteIds.addElement(siteId);
		}
		if (siteIds.size() < 1)
			return false;

		destLayer = new DGeoLayer();
		destLayer.setName(res.getString("Destinations"));
		destLayer.setContainerIdentifier("_destinations_");
		destLayer.setType(Geometry.point);
		destLayer.setGeoObjects(sites, true);
		DrawingParameters dp = destLayer.getDrawingParameters();
		if (dp == null) {
			dp = new DrawingParameters();
			destLayer.setDrawingParameters(dp);
		}
		dp.lineColor = Color.blue;
		dp.lineWidth = 1;
		dp.fillColor = Color.orange;
		dp.fillContours = true;
		dp.drawLabels = true;
		dp.labelColor = Color.gray;
		dp.hlWidth = 5;

		destTable = new DataTable();
		destTable.setName(res.getString("Destinations"));
		//the structure of the table must be the same as the structure of the input table
		DataSourceSpec spec2 = (DataSourceSpec) spec.clone();
		spec2.source = null;
		spec2.idFieldN = -1;
		spec2.idFieldName = null;
		spec2.nameFieldN = -1;
		spec2.nameFieldName = null;
		destTable.setDataSource(spec2);
		int siteIdCN2 = -1, siteNameCN2 = -1, siteTypeCN2 = -1, itemCatCN2 = -1, itemCatCodeCN2 = -1, capacityCN2 = -1;
		aName = (String) spec2.extraInfo.get("ID_FIELD_NAME");
		if (aName == null) {
			aName = spec.idFieldName;
		}
		if (aName == null) {
			aName = "id";
			spec2.extraInfo.put("ID_FIELD_NAME", aName);
		}
		destTable.addAttribute(aName, "site_id", AttributeTypes.character);
		siteIdCN2 = destTable.getAttrCount() - 1;
		aName = (String) spec2.extraInfo.get("NAME_FIELD_NAME");
		if (aName == null) {
			aName = spec.nameFieldName;
		}
		if (aName == null) {
			aName = "Name";
			spec2.extraInfo.put("NAME_FIELD_NAME", aName);
		}
		destTable.addAttribute(aName, "site_name", AttributeTypes.character);
		siteNameCN2 = destTable.getAttrCount() - 1;
		aName = (String) spec2.extraInfo.get("SITE_TYPE_FIELD_NAME");
		if (aName != null) {
			destTable.addAttribute(aName, "site_type", AttributeTypes.character);
			siteTypeCN2 = destTable.getAttrCount() - 1;
		}
		aName = (String) spec2.extraInfo.get("ITEM_CLASS_CODE_FIELD_NAME");
		if (aName == null && itemCatCodes != null && itemCatCodes.size() > 0) {
			aName = "item class code";
			spec2.extraInfo.put("ITEM_CLASS_CODE_FIELD_NAME", aName);
		}
		if (aName != null) {
			destTable.addAttribute(aName, "item_class_code", AttributeTypes.character);
			itemCatCodeCN2 = destTable.getAttrCount() - 1;
		}
		aName = (String) spec2.extraInfo.get("ITEM_CLASS_FIELD_NAME");
		if (aName != null) {
			destTable.addAttribute(aName, "item_class_name", AttributeTypes.character);
			itemCatCN2 = destTable.getAttrCount() - 1;
		}
		aName = (String) spec2.extraInfo.get("CAPACITY_FIELD_NAME");
		destTable.addAttribute(aName, "capacity", AttributeTypes.integer);
		capacityCN2 = destTable.getAttrCount() - 1;

		for (int i = 0; i < destData.getDataItemCount(); i++) {
			DataRecord rec1 = destData.getDataRecord(i);
			String siteId = (siteIdCN1 >= 0) ? rec1.getAttrValueAsString(siteIdCN1) : rec1.getId();
			if (siteId == null) {
				continue;
			}
			if (rec1.getNumericAttrValue(capacityCN1) < 1) {
				continue;
			}
			DGeoObject geo = (DGeoObject) destLayer.findObjectById(siteId);
			if (geo == null) {
				continue;
			}
			DataRecord rec2 = new DataRecord(siteId + "_" + (i + 1), geo.getName());
			destTable.addDataRecord(rec2);
			rec2.setAttrValue(siteId, siteIdCN2);
			rec2.setAttrValue(geo.getName(), siteNameCN2);
			if (siteTypeCN1 >= 0 && siteTypeCN2 >= 0) {
				rec2.setAttrValue(rec1.getAttrValue(siteTypeCN1), siteTypeCN2);
			}
			if (itemCatCodeCN1 >= 0 && itemCatCodeCN2 >= 0) {
				rec2.setAttrValue(rec1.getAttrValue(itemCatCodeCN1), itemCatCodeCN2);
			}
			if (itemCatCN1 >= 0 && itemCatCN2 >= 0) {
				rec2.setAttrValue(rec1.getAttrValue(itemCatCN1), itemCatCN2);
			}
			double val = rec1.getNumericAttrValue(capacityCN1);
			int num = (Double.isNaN(val)) ? 0 : (int) Math.round(val);
			if (num < 0) {
				num = 0;
			}
			rec2.setNumericAttrValue(num, String.valueOf(num), capacityCN2);
		}

		return true;
	}

	/**
	 * Uses the given table with data about vehicle locations (which may be e.g. left from
	 * a previous run of the scheduler) and layer with locations (suposedly containing the
	 * vehicle locations), builds the vehicleLocLayer and vehicleLocTable. Returns true if successful.
	 */
	public boolean setVehicleLocations(DataTable vehicleLocData, DGeoLayer locLayer) {
		if (vehicleLocData == null || locLayer == null || !vehicleLocData.hasData() || !locLayer.hasData())
			return false;
		DataSourceSpec spec = (DataSourceSpec) vehicleLocData.getDataSource();
		if (spec == null || spec.extraInfo == null)
			return false;
		int siteIdCN1 = -1, siteNameCN1 = -1, vehicleTypeCN1 = -1, vehicleNumCN1 = -1, readyTimeCN1 = -1;
		if (spec.extraInfo.get("SOURCE_ID_FIELD_NAME") != null) {
			siteIdCN1 = vehicleLocData.findAttrByName((String) spec.extraInfo.get("SOURCE_ID_FIELD_NAME"));
		}
		if (spec.extraInfo.get("SOURCE_NAME_FIELD_NAME") != null) {
			siteNameCN1 = vehicleLocData.findAttrByName((String) spec.extraInfo.get("SOURCE_NAME_FIELD_NAME"));
		}
		if (spec.extraInfo.get("VEHICLE_CLASS_ID_FIELD_NAME") != null) {
			vehicleTypeCN1 = vehicleLocData.findAttrByName((String) spec.extraInfo.get("VEHICLE_CLASS_ID_FIELD_NAME"));
		}
		if (spec.extraInfo.get("VEHICLE_NUMBER_FIELD_NAME") != null) {
			vehicleNumCN1 = vehicleLocData.findAttrByName((String) spec.extraInfo.get("VEHICLE_NUMBER_FIELD_NAME"));
		}
		if (spec.extraInfo.get("READY_TIME_FIELD_NAME") != null) {
			readyTimeCN1 = vehicleLocData.findAttrByName((String) spec.extraInfo.get("READY_TIME_FIELD_NAME"));
		}
		if (vehicleNumCN1 < 0 || siteIdCN1 < 0)
			return false;

		Vector siteIds = new Vector(vehicleLocData.getDataItemCount(), 1);
		Vector sites = new Vector(vehicleLocData.getDataItemCount(), 1);

		for (int i = 0; i < vehicleLocData.getDataItemCount(); i++) {
			DataRecord rec1 = vehicleLocData.getDataRecord(i);
			String siteId = rec1.getAttrValueAsString(siteIdCN1);
			if (siteId == null) {
				continue;
			}
			if (StringUtil.isStringInVectorIgnoreCase(siteId, siteIds)) {
				continue;
			}
			DGeoObject geo1 = (DGeoObject) locLayer.findObjectById(siteId);
			if (geo1 == null) {
				continue;
			}
			String siteName = null;
			if (siteNameCN1 >= 0) {
				siteName = rec1.getAttrValueAsString(siteNameCN1);
			}
			if (siteName == null) {
				siteName = geo1.getName();
			}
			DGeoObject geo2 = (DGeoObject) geo1.makeCopy();
			geo2.setIdentifier(siteId);
			if (siteName != null) {
				geo2.setLabel(siteName);
			}
			sites.addElement(geo2);
			siteIds.addElement(siteId);
		}
		if (siteIds.size() < 1)
			return false;

		vehicleLocLayer = new DGeoLayer();
		vehicleLocLayer.setName(res.getString("Vehicle_locations"));
		vehicleLocLayer.setContainerIdentifier("_vehicle_locations_");
		vehicleLocLayer.setType(Geometry.point);
		vehicleLocLayer.setGeoObjects(sites, true);
		DrawingParameters dp = vehicleLocLayer.getDrawingParameters();
		if (dp == null) {
			dp = new DrawingParameters();
			vehicleLocLayer.setDrawingParameters(dp);
		}
		dp.lineColor = Color.magenta;
		dp.lineWidth = 1;
		dp.fillColor = Color.magenta;
		dp.fillContours = true;
		dp.drawLabels = true;
		dp.labelColor = Color.gray;
		dp.hlWidth = 5;

		vehicleLocTable = new DataTable();
		vehicleLocTable.setName(res.getString("Vehicle_locations"));
		//the structure of the table must be the same as the structure of the input table
		DataSourceSpec spec2 = (DataSourceSpec) spec.clone();
		spec2.source = null;
		vehicleLocTable.setDataSource(spec2);
		int siteIdCN2 = -1, siteNameCN2 = -1, vehicleTypeCN2 = -1, vehicleNumCN2 = -1, readyTimeCN2 = -1;
		vehicleLocTable.addAttribute((String) spec2.extraInfo.get("SOURCE_ID_FIELD_NAME"), "site_id", AttributeTypes.character);
		siteIdCN2 = vehicleLocTable.getAttrCount() - 1;
		String aName = (String) spec2.extraInfo.get("SOURCE_NAME_FIELD_NAME");
		if (aName == null) {
			aName = "Name";
			spec2.extraInfo.put("SOURCE_NAME_FIELD_NAME", aName);
		}
		vehicleLocTable.addAttribute(aName, "site_name", AttributeTypes.character);
		siteNameCN2 = vehicleLocTable.getAttrCount() - 1;
		aName = (String) spec2.extraInfo.get("VEHICLE_CLASS_ID_FIELD_NAME");
		if (aName == null) {
			aName = "vehicle class";
			spec2.extraInfo.put("VEHICLE_CLASS_ID_FIELD_NAME", aName);
		}
		vehicleLocTable.addAttribute(aName, "vehicle_type", AttributeTypes.character);
		vehicleTypeCN2 = vehicleLocTable.getAttrCount() - 1;
		aName = (String) spec2.extraInfo.get("VEHICLE_NUMBER_FIELD_NAME");
		vehicleLocTable.addAttribute(aName, "vehicle_number", AttributeTypes.integer);
		vehicleNumCN2 = vehicleLocTable.getAttrCount() - 1;
		aName = (String) spec2.extraInfo.get("READY_TIME_FIELD_NAME");
		if (aName == null) {
			aName = "ready time";
			spec2.extraInfo.put("READY_TIME_FIELD_NAME", aName);
		}
		vehicleLocTable.addAttribute(aName, "ready_time", AttributeTypes.integer);
		readyTimeCN2 = vehicleLocTable.getAttrCount() - 1;

		for (int i = 0; i < vehicleLocData.getDataItemCount(); i++) {
			DataRecord rec1 = vehicleLocData.getDataRecord(i);
			String siteId = rec1.getAttrValueAsString(siteIdCN1);
			if (siteId == null) {
				continue;
			}
			DGeoObject geo = (DGeoObject) vehicleLocLayer.findObjectById(siteId);
			if (geo == null) {
				continue;
			}
			DataRecord rec2 = new DataRecord(siteId + "_" + (i + 1), geo.getName());
			vehicleLocTable.addDataRecord(rec2);
			rec2.setAttrValue(siteId, siteIdCN2);
			rec2.setAttrValue(geo.getName(), siteNameCN2);
			if (vehicleTypeCN1 >= 0 && vehicleTypeCN2 >= 0) {
				rec2.setAttrValue(rec1.getAttrValue(vehicleTypeCN1), vehicleTypeCN2);
			}
			double val = rec1.getNumericAttrValue(vehicleNumCN1);
			int num = (Double.isNaN(val)) ? 0 : (int) Math.round(val);
			if (num < 0) {
				num = 0;
			}
			rec2.setNumericAttrValue(num, String.valueOf(num), vehicleNumCN2);
			num = 1;
			if (readyTimeCN1 >= 0) {
				val = rec1.getNumericAttrValue(readyTimeCN1);
				if (!Double.isNaN(val)) {
					num = (int) Math.round(val);
					if (num < 1) {
						num = 1;
					}
				}
			}
			rec2.setNumericAttrValue(num, String.valueOf(num), readyTimeCN2);
		}

		return true;
	}

	/**
	 * Composes a layer from all locations: item sources, destinations, vehicle sources
	 */
	public DGeoLayer makeLayerWithAllPlaces() {
		//create the table
		DataTable table = new DataTable();
		table.setName(res.getString("Places"));
		table.setContainerIdentifier("_t_places_");
		DataSourceSpec spec = new DataSourceSpec();
		spec.name = table.getName();
		spec.format = "CSV";
		spec.delimiter = ",";
		spec.nRowWithFieldNames = 0;
		spec.idFieldName = "id";
		spec.nameFieldName = "name";
		spec.extraInfo = new Hashtable();
		spec.source = null;
		table.setDataSource(spec);
		String aName = "type";
		spec.extraInfo.put("SITE_TYPE_FIELD_NAME", aName);
		table.addAttribute(aName, "type", AttributeTypes.character);
		int typeCN = table.getAttrCount() - 1;

		Vector geoObj = new Vector(100, 100);
		Vector siteIds = new Vector(100, 100);

		if (souTable != null && souLayer != null) {
			spec = (DataSourceSpec) souTable.getDataSource();
			Hashtable info = (spec != null) ? spec.extraInfo : null;
			aName = (info != null) ? (String) info.get("SOURCE_ID_FIELD_NAME") : null;
			if (aName == null) {
				aName = spec.idFieldName;
			}
			if (aName == null) {
				aName = "id";
			}
			int siteIdCN = souTable.findAttrByName(aName);
			if (siteIdCN >= 0) {
				aName = (info != null) ? (String) info.get("SOURCE_NAME_FIELD_NAME") : null;
				if (aName == null) {
					aName = spec.nameFieldName;
				}
				if (aName == null) {
					aName = "Name";
				}
				int siteNameCN = souTable.findAttrByName(aName);
				aName = (info != null) ? (String) info.get("SITE_TYPE_FIELD_NAME") : "type";
				if (aName == null) {
					aName = "type";
				}
				int siteTypeCN = souTable.findAttrByName(aName);
				for (int i = 0; i < souTable.getDataItemCount(); i++) {
					DataRecord rec1 = souTable.getDataRecord(i);
					String siteId = rec1.getAttrValueAsString(siteIdCN);
					if (siteId == null || StringUtil.isStringInVectorIgnoreCase(siteId, siteIds)) {
						continue;
					}
					DGeoObject geo1 = (DGeoObject) souLayer.findObjectById(siteId);
					if (geo1 == null) {
						continue;
					}
					siteIds.addElement(siteId);
					String siteName = (siteNameCN >= 0) ? rec1.getAttrValueAsString(siteNameCN) : geo1.getName();
					DataRecord rec2 = new DataRecord(siteId, siteName);
					table.addDataRecord(rec2);
					String type = null;
					if (siteTypeCN >= 0) {
						type = rec1.getAttrValueAsString(siteTypeCN);
					}
					if (type == null) {
						type = "unknown";
					}
					rec2.setAttrValue(type, typeCN);
					DGeoObject geo2 = (DGeoObject) geo1.makeCopy();
					geo2.setThematicData(rec2);
					geo2.setLabel(siteName);
					geoObj.addElement(geo2);
				}
			}
		}
		if (destTable != null && destLayer != null) {
			spec = (DataSourceSpec) destTable.getDataSource();
			Hashtable info = (spec != null) ? spec.extraInfo : null;
			aName = (info != null) ? (String) info.get("ID_FIELD_NAME") : null;
			if (aName == null) {
				aName = spec.idFieldName;
			}
			if (aName == null) {
				aName = "id";
			}
			int siteIdCN = destTable.findAttrByName(aName);
			if (siteIdCN >= 0) {
				aName = (info != null) ? (String) info.get("NAME_FIELD_NAME") : null;
				if (aName == null) {
					aName = spec.nameFieldName;
				}
				if (aName == null) {
					aName = "Name";
				}
				int siteNameCN = destTable.findAttrByName(aName);
				aName = (info != null) ? (String) info.get("SITE_TYPE_FIELD_NAME") : "type";
				if (aName == null) {
					aName = "type";
				}
				int siteTypeCN = destTable.findAttrByName(aName);
				for (int i = 0; i < destTable.getDataItemCount(); i++) {
					DataRecord rec1 = destTable.getDataRecord(i);
					String siteId = rec1.getAttrValueAsString(siteIdCN);
					if (siteId == null || StringUtil.isStringInVectorIgnoreCase(siteId, siteIds)) {
						continue;
					}
					DGeoObject geo1 = (DGeoObject) destLayer.findObjectById(siteId);
					if (geo1 == null) {
						continue;
					}
					siteIds.addElement(siteId);
					String siteName = (siteNameCN >= 0) ? rec1.getAttrValueAsString(siteNameCN) : geo1.getName();
					DataRecord rec2 = new DataRecord(siteId, siteName);
					table.addDataRecord(rec2);
					String type = null;
					if (siteTypeCN >= 0) {
						type = rec1.getAttrValueAsString(siteTypeCN);
					}
					if (type == null) {
						type = "unknown";
					}
					rec2.setAttrValue(type, typeCN);
					DGeoObject geo2 = (DGeoObject) geo1.makeCopy();
					geo2.setThematicData(rec2);
					geo2.setLabel(siteName);
					geoObj.addElement(geo2);
				}
			}
		}
		if (vehicleLocTable != null && vehicleLocLayer != null) {
			spec = (DataSourceSpec) vehicleLocTable.getDataSource();
			Hashtable info = (spec != null) ? spec.extraInfo : null;
			aName = (info != null) ? (String) info.get("SOURCE_ID_FIELD_NAME") : null;
			if (aName == null) {
				aName = spec.idFieldName;
			}
			if (aName == null) {
				aName = "id";
			}
			int siteIdCN = vehicleLocTable.findAttrByName(aName);
			if (siteIdCN >= 0) {
				aName = (info != null) ? (String) info.get("SOURCE_NAME_FIELD_NAME") : null;
				if (aName == null) {
					aName = spec.nameFieldName;
				}
				if (aName == null) {
					aName = "Name";
				}
				int siteNameCN = vehicleLocTable.findAttrByName(aName);
				aName = (info != null) ? (String) info.get("SITE_TYPE_FIELD_NAME") : "type";
				if (aName == null) {
					aName = "type";
				}
				int siteTypeCN = vehicleLocTable.findAttrByName(aName);
				for (int i = 0; i < vehicleLocTable.getDataItemCount(); i++) {
					DataRecord rec1 = vehicleLocTable.getDataRecord(i);
					String siteId = rec1.getAttrValueAsString(siteIdCN);
					if (siteId == null || StringUtil.isStringInVectorIgnoreCase(siteId, siteIds)) {
						continue;
					}
					DGeoObject geo1 = (DGeoObject) vehicleLocLayer.findObjectById(siteId);
					if (geo1 == null) {
						continue;
					}
					siteIds.addElement(siteId);
					String siteName = (siteNameCN >= 0) ? rec1.getAttrValueAsString(siteNameCN) : geo1.getName();
					DataRecord rec2 = new DataRecord(siteId, siteName);
					table.addDataRecord(rec2);
					String type = null;
					if (siteTypeCN >= 0) {
						type = rec1.getAttrValueAsString(siteTypeCN);
					}
					if (type == null) {
						type = "vehicle source";
					}
					rec2.setAttrValue(type, typeCN);
					DGeoObject geo2 = (DGeoObject) geo1.makeCopy();
					geo2.setThematicData(rec2);
					geo2.setLabel(siteName);
					geoObj.addElement(geo2);
				}
			}
		}

		if (geoObj.size() < 1)
			return null;

		DGeoLayer layer = new DGeoLayer();
		layer.setName(table.getName());
		layer.setContainerIdentifier("_places_");
		layer.setType(Geometry.point);
		layer.setGeoObjects(geoObj, true);
		DrawingParameters dp = layer.getDrawingParameters();
		if (dp == null) {
			dp = new DrawingParameters();
			layer.setDrawingParameters(dp);
		}
		dp.lineColor = Color.yellow;
		dp.lineWidth = 1;
		dp.fillColor = Color.blue;
		dp.fillContours = true;
		dp.drawLabels = true;
		dp.labelColor = Color.gray;
		dp.transparency = 50;
		layer.setDataTable(table);
		layer.setDataSource(table.getDataSource());
		return layer;
	}

	/**
	 * Writes the prepared data to appropriate files and
	 * notifies the owner about the process having been done
	 */
	protected void writeDataAndFinish() {
		String applPath = core.getDataKeeper().getApplicationPath();
		if (applPath != null) {
			applPath = CopyFile.getDir(applPath);
		}
		if (applPath == null) {
			applPath = "";
		}
		if (!CopyFile.isAbsolutePath(schedulerInputPath)) {
			schedulerInputPath = applPath + schedulerInputPath;
		}
		File ipf = new File(schedulerInputPath);
		if (!ipf.exists() || !ipf.isDirectory()) {
			ipf.mkdir();
		}
		writeItemClasses(schedulerInputPath + itemClassesFName);
		writeVehicleClasses(schedulerInputPath + vehicleClassesFName);
		writeVehicleSuitAndLoadTimes(schedulerInputPath + vehiclesSuitFName, schedulerInputPath + loadTimesFName);
		writeItemSourcesData(schedulerInputPath + sourcesFName, schedulerInputPath + sourcesExtFName);
		writeDestinationsData(schedulerInputPath + destFName);
		writeVehiclesInSourcesData(schedulerInputPath + vehiclesFName);
		DGeoLayer layer = makeLayerWithAllPlaces();
		if (layer != null) {
			DLayerManager lman = (DLayerManager) core.getUI().getCurrentMapViewer().getLayerManager();
			if (lman != null && locLayer != null) {
				int layerIdx = lman.getIndexOfLayer(locLayer.getContainerIdentifier());
				if (layerIdx >= 0)
					if (locLayerWasAdded || locLayer.getContainerIdentifier().equals(layer.getContainerIdentifier())) {
						core.removeMapLayer(locLayer.getContainerIdentifier(), true);
					} else {
						locLayer.setLayerDrawn(false);
					}
			}
			locLayer = layer;
			if (locLayer != null) {
				DataLoader loader = core.getDataLoader();
				int tn = loader.addTable((DataTable) locLayer.getThematicData());
				loader.addMapLayer(locLayer, -1);
				loader.setLink(locLayer, tn);
				locLayer.setLinkedToTable(true);
				locLayerWasAdded = true;
				writeAllPlaces(schedulerInputPath + allPlacesFName);
			}
		}
		Matrix matr = makeDistanceMatrix();
		if (matr != null) {
			this.distancesMatrix = matr;
			writeDistanceMatrix(schedulerInputPath + distancesFName);
		}
		if (owner != null) {
			owner.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "done"));
		}
	}

	/**
	 * Makes a distance matrix: allows the user to specify a file with the
	 * distances and computes the lacking distances
	 */
	public Matrix makeDistanceMatrix() {
		if (locLayer == null || locLayer.getObjectCount() < 2)
			return null;
		Matrix matr0 = null;
		if (Dialogs.askYesOrNo(core.getUI().getMainFrame(), res.getString("Load_dist_from_file_"), res.getString("Use_pre_comp_dist_"))) {
			boolean cancelled = false;
			do {
				GetPathDlg fd = new GetPathDlg(core.getUI().getMainFrame(), res.getString("Spec_file_distances"));
				fd.setDirectory(schedulerInputPath);
				fd.setFileMask("*.txt;*.csv");
				fd.show();
				String path = fd.getPath();
				if (path != null) {
					DistancesLoader distLoader = new DistancesLoader();
					matr0 = distLoader.loadDistances(path, null);
					if (matr0 == null && !Dialogs.askYesOrNo(core.getUI().getMainFrame(), res.getString("failed_load_distances_from") + " " + path + ". " + res.getString("Try_another_file_"), res.getString("Try_another_file_"))) {
						cancelled = true;
					}
				} else {
					cancelled = true;
				}
			} while (!cancelled && matr0 == null);
		}
		Matrix matr = new Matrix(locLayer.getObjectCount(), locLayer.getObjectCount(), 10);
		matr.addAttribute("distance");
		matr.addAttribute("driving time");
		for (int i = 0; i < locLayer.getObjectCount(); i++) {
			DGeoObject obj = locLayer.getObject(i);
			matr.addColumnId(obj.getIdentifier());
			matr.addRowId(obj.getIdentifier());
		}
		float aveSpeed = Float.NaN;
		Float zero = new Float(0);
		for (int i = 0; i < locLayer.getObjectCount(); i++) {
			DGeoObject obj1 = locLayer.getObject(i);
			Geometry geom = obj1.getGeometry();
			RealPoint pt1 = (geom instanceof RealPoint) ? (RealPoint) geom : null;
			if (pt1 == null) {
				float r[] = geom.getBoundRect();
				pt1 = new RealPoint((r[0] + r[2]) / 2, (r[1] + r[3]) / 2);
			}
			for (int j = 0; j < locLayer.getObjectCount(); j++)
				if (i == j) {
					matr.put(zero, 0, i, j);
					matr.put(zero, 1, i, j);
				} else {
					DGeoObject obj2 = locLayer.getObject(j);
					boolean haveDist = false, haveTime = false;
					double dist = Double.NaN;
					if (matr0 != null) {
						Float val = (Float) matr0.get(0, obj1.getIdentifier(), obj2.getIdentifier());
						if (val != null && !Float.isNaN(val)) {
							matr.put(val, 0, i, j);
							haveDist = true;
							dist = val.doubleValue();
						}
						val = (Float) matr0.get(1, obj1.getIdentifier(), obj2.getIdentifier());
						if (val != null && !Float.isNaN(val)) {
							matr.put(val, 1, i, j);
							haveTime = true;
						}
					}
					if (!haveDist) {
						geom = obj2.getGeometry();
						RealPoint pt2 = (geom instanceof RealPoint) ? (RealPoint) geom : null;
						if (pt2 == null) {
							float r[] = geom.getBoundRect();
							pt2 = new RealPoint((r[0] + r[2]) / 2, (r[1] + r[3]) / 2);
						}
						dist = GeoComp.getManhattanDistance(pt1.x, pt1.y, pt2.x, pt2.y, locLayer.isGeographic());
						Float val = new Float(dist);
						matr.put(val, 0, i, j);
						haveDist = true;
					}
					if (!haveTime) {
						if (Float.isNaN(aveSpeed)) {
							aveSpeed = Dialogs.askForIntValue(core.getUI().getMainFrame(), res.getString("av_speed_request"), 30, 5, 150, res.getString("av_speed_expl"), res.getString("Average_speed"), false);
							aveSpeed *= 1000; //m per hour
							aveSpeed /= 60; //m per minute
						}
						double time = Math.ceil(dist / aveSpeed) + 2;
						Float val = new Float(time);
						matr.put(val, 1, i, j);
						haveTime = true;
					}
				}
		}
		return matr;
	}

	protected void showMessage(String msg, boolean error) {
		if (core != null && core.getUI() != null) {
			core.getUI().showMessage(msg, error);
		}
		if (error) {
			System.out.println("!--> " + msg);
		}
	}

	/**
	 * Reacts to notifications about finishing of data editing from
	 * the corresponding windows
	 */
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if (cmd == null)
			return;
		if (e.getSource().equals(editingUI)) {
			editingUI.removeWindowListener(this);
			editingUI = null;
			if (cmd.equals("items_in_sources")) {
				itemCountsByCat = countItemsInSources();
				int ncat = 1;
				if (itemCatCodes != null && itemCatCodes.size() > 1) {
					ncat = itemCatCodes.size();
				} else if (itemCatNames != null && itemCatNames.size() > 1) {
					ncat = itemCatNames.size();
				}
				if (ncat > 1) {
					int nCatPresent = 0;
					for (int element : itemCountsByCat)
						if (element > 0) {
							++nCatPresent;
						}
					if (nCatPresent < ncat) {
						int counts[] = new int[nCatPresent];
						int k = 0;
						for (int element : itemCountsByCat)
							if (element > 0) {
								counts[k++] = element;
							} else {
								if (itemCatCodes != null && itemCatCodes.size() > k) {
									itemCatCodes.removeElementAt(k);
								}
								if (itemCatNames != null && itemCatNames.size() > k) {
									itemCatNames.removeElementAt(k);
								}
							}
						itemCountsByCat = counts;
					}
				}
				editDestinationsTable();
				return;
			}
			if (cmd.equals("destinations")) {
				editVehicleLocTable();
				return;
			}
			if (cmd.equals("vehicles_in_sources")) {
				//distances!
				//...
			}
			//no steps left!
			writeDataAndFinish();
		}
	}

	/**
	 * Replies if data preparation is in progress
	 */
	public boolean isRunning() {
		return editingUI != null;
	}

	/**
	 * Reacts to the data editing window being closed
	 */
	public void windowClosed(WindowEvent e) {
		if (e.getSource().equals(editingUI)) {
			editingUI.removeWindowListener(this);
			editingUI = null;
			if (owner != null) {
				owner.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "cancel"));
			}
		}
	}

	public void windowOpened(WindowEvent e) {
	}

	public void windowClosing(WindowEvent e) {
	}

	public void windowIconified(WindowEvent e) {
	}

	public void windowDeiconified(WindowEvent e) {
	}

	public void windowActivated(WindowEvent e) {
	}

	public void windowDeactivated(WindowEvent e) {
	}

	/**
	 * Using the given classification of locations, modifies the distances matrix
	 * so that the distances and/or travel times between sites from different classes
	 * are replaced by very high values indicating that there is no way between
	 * these locations.
	 * @param siteIds - identifiers of the locations
	 * @param classes -  the classes these locations belong to (strings)
	 * @param distValue - the value to put for the distance when two sites are
	 *                    from different classes
	 * @param timeValue - the value to put for the travel time when two sites are
	 *                    from different classes
	 */
	public void separateSitesInDistanceMatrix(Vector siteIds, Vector classes, Object distValue, Object timeValue) {
		if (distancesMatrix == null || siteIds == null || classes == null || siteIds.size() < 2 && classes.size() < 2)
			return;
		for (int i = 0; i < siteIds.size() - 1; i++) {
			String cl1 = (String) classes.elementAt(i);
			if (cl1 == null) {
				continue;
			}
			String id1 = (String) siteIds.elementAt(i);
			int ir1 = distancesMatrix.getRowIndex(id1), ic1 = distancesMatrix.getColumnIndex(id1);
			if (ir1 < 0 && ic1 < 0) {
				continue;
			}
			for (int j = i + 1; j < siteIds.size(); j++) {
				String cl2 = (String) classes.elementAt(j);
				if (cl2.equals(cl1)) {
					continue;
				}
				String id2 = (String) siteIds.elementAt(j);
				int ir2 = distancesMatrix.getRowIndex(id2), ic2 = distancesMatrix.getColumnIndex(id2);
				if (ir2 < 0 && ic2 < 0) {
					continue;
				}
				distancesMatrix.put(distValue, 0, ir1, ic2);
				distancesMatrix.put(distValue, 0, ir2, ic1);
				distancesMatrix.put(timeValue, 1, ir1, ic2);
				distancesMatrix.put(timeValue, 1, ir2, ic1);
			}
		}
	}

	/**
	 * Using the given classification of locations, modifies the distances matrix
	 * so that the distances and/or travel times between sites from different classes
	 * are multiplied by the given factor.
	 * @param siteIds - identifiers of the locations
	 * @param classes - the classes these locations belong to (strings)
	 * @param factor - the factor to multiply the values in the distance matrix
	 */
	public void modifyReachabilityInDistanceMatrix(Vector siteIds, Vector classes, float factor) {
		if (distancesMatrix == null || siteIds == null || classes == null || siteIds.size() < 2 && classes.size() < 2)
			return;
		for (int i = 0; i < siteIds.size() - 1; i++) {
			String cl1 = (String) classes.elementAt(i);
			if (cl1 == null) {
				continue;
			}
			String id1 = (String) siteIds.elementAt(i);
			int ir1 = distancesMatrix.getRowIndex(id1), ic1 = distancesMatrix.getColumnIndex(id1);
			if (ir1 < 0 && ic1 < 0) {
				continue;
			}
			for (int j = i + 1; j < siteIds.size(); j++) {
				String cl2 = (String) classes.elementAt(j);
				if (cl2.equals(cl1)) {
					continue;
				}
				String id2 = (String) siteIds.elementAt(j);
				int ir2 = distancesMatrix.getRowIndex(id2), ic2 = distancesMatrix.getColumnIndex(id2);
				if (ir2 < 0 && ic2 < 0) {
					continue;
				}
				if (ir1 >= 0 && ic2 >= 0) {
					Float fval = (Float) distancesMatrix.get(0, ir1, ic2);
					if (fval != null && !fval.isNaN()) {
						float val = fval.floatValue() * factor;
						distancesMatrix.put(new Float(val), 0, ir1, ic2);
					}
					fval = (Float) distancesMatrix.get(1, ir1, ic2);
					if (fval != null && !fval.isNaN()) {
						float val = fval.floatValue() * factor;
						distancesMatrix.put(new Float(val), 1, ir1, ic2);
					}
				}
				if (ir2 >= 0 && ic1 >= 0) {
					Float fval = (Float) distancesMatrix.get(0, ir2, ic1);
					if (fval != null && !fval.isNaN()) {
						float val = fval.floatValue() * factor;
						distancesMatrix.put(new Float(val), 0, ir2, ic1);
					}
					fval = (Float) distancesMatrix.get(1, ir2, ic1);
					if (fval != null && !fval.isNaN()) {
						float val = fval.floatValue() * factor;
						distancesMatrix.put(new Float(val), 1, ir2, ic1);
					}
				}
			}
		}
	}

	/**
	 * Temporary!
	 * Reads a table with site classification and calls separateSitesInDistanceMatrix(...)
	 */
	public void correctDistanceMatrix() {
		if (distancesMatrix == null)
			return;
		String applPath = core.getDataKeeper().getApplicationPath();
		if (applPath != null) {
			applPath = CopyFile.getDir(applPath);
		} else {
			applPath = "";
		}
		FileInputStream stream = null;
		try {
			stream = new FileInputStream(applPath + "river_sides.csv");
		} catch (IOException e) {
		}
		if (stream == null)
			return;
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		Vector siteIds = new Vector(100, 100), classes = new Vector(100, 100);
		while (true) {
			String str = null;
			try {
				str = reader.readLine();
			} catch (EOFException eof) {
				break;
			} catch (IOException ioe) {
				System.out.println("Error reading site classes: " + ioe);
				return;
			}
			if (str == null) {
				break;
			}
			Vector v = StringUtil.getNames(str, ",;\t", true);
			if (v == null || v.size() < 2) {
				continue;
			}
			siteIds.addElement(v.elementAt(0));
			classes.addElement(v.elementAt(v.size() - 1));
		}
		if (siteIds.size() < 2)
			return;
		//Float distValue=new Float(Float.NaN), timeValue=new Float(100f*Short.MAX_VALUE);
		//separateSitesInDistanceMatrix(siteIds,classes,distValue,timeValue);
		modifyReachabilityInDistanceMatrix(siteIds, classes, 2);
		distancesFName = "distances_1.csv";
		writeDistanceMatrix(schedulerInputPath + distancesFName);
	}

	protected boolean writeItemSourcesData(String fname, String extfname) {
		if (souTable == null || souTable.getDataItemCount() < 1) {
			showMessage("No data about item sources!", true);
			return false;
		}
		DataSourceSpec spec = (DataSourceSpec) souTable.getDataSource();
		if (spec == null || spec.extraInfo == null) {
			showMessage("Unknown meanings of the columns in the table with the item sources!", true);
			return false;
		}
		int siteIdCN = souTable.findAttrByName((String) spec.extraInfo.get("SOURCE_ID_FIELD_NAME"));
		if (siteIdCN < 0) {
			showMessage("Table with item sources: no column with the site identifiers found!", true);
			return false;
		}
		int siteNameCN = souTable.findAttrByName((String) spec.extraInfo.get("SOURCE_NAME_FIELD_NAME"));
		int siteTypeCN = souTable.findAttrByName((String) spec.extraInfo.get("SITE_TYPE_FIELD_NAME"));
		int itemCatCN = souTable.findAttrByName((String) spec.extraInfo.get("ITEM_CLASS_FIELD_NAME"));
		int itemCatCodeCN = souTable.findAttrByName((String) spec.extraInfo.get("ITEM_CLASS_CODE_FIELD_NAME"));
		int itemNumCN = souTable.findAttrByName((String) spec.extraInfo.get("ITEM_NUMBER_FIELD_NAME"));
		if (itemNumCN < 0) {
			showMessage("Table with item sources: no column with the numbers of items found!", true);
			return false;
		}
		int timeLimitCN = souTable.findAttrByName((String) spec.extraInfo.get("TIME_LIMIT_FIELD_NAME"));
		if (timeLimitCN < 0) {
			showMessage("Table with item sources: no column with the time limits found!", true);
			return false;
		}
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(fname);
		} catch (IOException ioe) {
			showMessage(res.getString("Could_not_create_file") + " " + fname, true);
			System.out.println(ioe);
		}
		if (out == null)
			return false;
		DataOutputStream dos = new DataOutputStream(out);
		try {
			dos.writeBytes("id,Name,type,item class,number of items,available time\n");
			for (int i = 0; i < souTable.getDataItemCount(); i++) {
				DataRecord rec = souTable.getDataRecord(i);
				String id = rec.getAttrValueAsString(siteIdCN);
				String name = rec.getAttrValueAsString(siteNameCN);
				if (name == null) {
					name = id;
				}
				String type = rec.getAttrValueAsString(siteTypeCN);
				if (type == null) {
					type = "unknown";
				}
				String cat = rec.getAttrValueAsString(itemCatCodeCN);
				if (cat == null) {
					cat = rec.getAttrValueAsString(itemCatCN);
				}
				if (cat == null) {
					cat = "1";
				}
				String numStr = rec.getAttrValueAsString(itemNumCN);
				String timeStr = rec.getAttrValueAsString(timeLimitCN);
				dos.writeBytes(id + "," + name + "," + type + "," + cat + "," + numStr + "," + timeStr + "\n");
			}
		} catch (IOException ioe) {
			showMessage(res.getString("Error_writing_file") + " " + fname, true);
			System.out.println(ioe);
			return false;
		}
		try {
			out.close();
		} catch (IOException e) {
		}

		if (extfname == null || itemCatCN < 0)
			return true;
		try {
			out = new FileOutputStream(extfname);
		} catch (IOException ioe) {
			showMessage(res.getString("Could_not_create_file") + " " + extfname, true);
			System.out.println(ioe);
		}
		if (out == null)
			return false;
		dos = new DataOutputStream(out);
		try {
			dos.writeBytes("id,Name,type,item class code,item class name,number of items,available time (min.)\n");
			for (int i = 0; i < souTable.getDataItemCount(); i++) {
				DataRecord rec = souTable.getDataRecord(i);
				String id = rec.getAttrValueAsString(siteIdCN);
				String name = rec.getAttrValueAsString(siteNameCN);
				if (name == null) {
					name = id;
				}
				String type = rec.getAttrValueAsString(siteTypeCN);
				if (type == null) {
					type = "unknown";
				}
				String catCode = rec.getAttrValueAsString(itemCatCodeCN);
				if (catCode == null) {
					catCode = "1";
				}
				String catName = rec.getAttrValueAsString(itemCatCN);
				if (catName == null) {
					catName = catCode;
				}
				String numStr = rec.getAttrValueAsString(itemNumCN);
				String timeStr = rec.getAttrValueAsString(timeLimitCN);
				dos.writeBytes(id + "," + name + "," + type + "," + catCode + "," + catName + "," + numStr + "," + timeStr + "\n");
			}
		} catch (IOException ioe) {
			showMessage(res.getString("Error_writing_file") + " " + fname, true);
			System.out.println(ioe);
			return false;
		}
		try {
			out.close();
		} catch (IOException e) {
		}
		return true;
	}

	protected boolean writeDestinationsData(String fname) {
		if (destTable == null || destTable.getDataItemCount() < 1) {
			showMessage("No data about destinations!", true);
			return false;
		}
		DataSourceSpec spec = (DataSourceSpec) destTable.getDataSource();
		if (spec == null || spec.extraInfo == null) {
			showMessage("Unknown meanings of the columns in the table with the destinations!", true);
			return false;
		}
		String aName = (String) spec.extraInfo.get("ID_FIELD_NAME");
		if (aName == null) {
			aName = spec.idFieldName;
		}
		if (aName == null) {
			aName = "id";
		}
		int siteIdCN = destTable.findAttrByName(aName);
		if (siteIdCN < 0) {
			showMessage("Table with destinations: no column with the site identifiers found!", true);
			return false;
		}
		aName = (String) spec.extraInfo.get("NAME_FIELD_NAME");
		if (aName == null) {
			aName = spec.nameFieldName;
		}
		if (aName == null) {
			aName = "name";
		}
		int siteNameCN = destTable.findAttrByName(aName);
		int siteTypeCN = destTable.findAttrByName((String) spec.extraInfo.get("SITE_TYPE_FIELD_NAME"));
		int itemCatCodeCN = destTable.findAttrByName((String) spec.extraInfo.get("ITEM_CLASS_CODE_FIELD_NAME"));
		int itemCatCN = destTable.findAttrByName((String) spec.extraInfo.get("ITEM_CLASS_FIELD_NAME"));
		int capacityCN = destTable.findAttrByName((String) spec.extraInfo.get("CAPACITY_FIELD_NAME"));
		if (capacityCN < 0) {
			showMessage("Table with destinations: no column with the capacities found!", true);
			return false;
		}
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(fname);
		} catch (IOException ioe) {
			showMessage(res.getString("Could_not_create_file") + " " + fname, true);
			System.out.println(ioe);
		}
		if (out == null)
			return false;
		DataOutputStream dos = new DataOutputStream(out);
		Vector siteIds = new Vector(destTable.getDataItemCount(), 10);
		try {
			dos.writeBytes("id,Name,type,item class,capacity\n");
			for (int i = 0; i < destTable.getDataItemCount(); i++) {
				DataRecord rec = destTable.getDataRecord(i);
				String id = rec.getAttrValueAsString(siteIdCN);
				if (!StringUtil.isStringInVectorIgnoreCase(id, siteIds)) {
					siteIds.addElement(id);
				}
				String name = rec.getAttrValueAsString(siteNameCN);
				if (name == null) {
					name = id;
				}
				String type = rec.getAttrValueAsString(siteTypeCN);
				if (type == null) {
					type = "unknown";
				}
				String cat = rec.getAttrValueAsString(itemCatCodeCN);
				if (cat == null) {
					cat = rec.getAttrValueAsString(itemCatCN);
				}
				if (cat == null) {
					cat = "1";
				}
				String numStr = rec.getAttrValueAsString(capacityCN);
				dos.writeBytes(id + "," + name + "," + type + "," + cat + "," + numStr + "\n");
			}
			if (vehicleLocTable != null) {
				for (int i = 0; i < vehicleLocTable.getDataItemCount(); i++) {
					DataRecord rec = vehicleLocTable.getDataRecord(i);
					String id = rec.getAttrValueAsString(siteIdCN);
					if (!StringUtil.isStringInVectorIgnoreCase(id, siteIds)) {
						String name = rec.getAttrValueAsString(siteNameCN);
						if (name == null) {
							name = id;
						}
						dos.writeBytes(id + "," + name + ",vehicle source,0,0\n");
					}
				}
			}
		} catch (IOException ioe) {
			showMessage(res.getString("Error_writing_file") + " " + fname, true);
			System.out.println(ioe);
			return false;
		}
		try {
			out.close();
		} catch (IOException e) {
		}
		/*
		if (destCap==null) {
		  DataSourceSpec dspec=DataSourceSpec.readMetadata(metadataPath+"destinations.descr",
		                          core.getDataLoader().getApplicationPath());
		  if (dspec==null) {
		    dspec=new DataSourceSpec();
		    dspec.name="Destinations";
		  }
		  dspec.format="CSV"; dspec.delimiter=","; dspec.nRowWithFieldNames=0;
		  dspec.idFieldName="id"; dspec.nameFieldName="Name";
		  dspec.source=fname;
		  if (dspec.extraInfo==null)
		    dspec.extraInfo=new Hashtable();
		  dspec.extraInfo.put("SITE_TYPE_FIELD_NAME","type");
		  dspec.extraInfo.put("ITEM_CLASS_CODE_FIELD_NAME","item class");
		  dspec.extraInfo.put("ITEM_CLASS_FIELD_NAME","item class name");
		  dspec.extraInfo.put("CAPACITY_FIELD_NAME","capacity");
		  destCap=new DataTable();
		  destCap.setName(dspec.name);
		  destCap.setDataSource(dspec);
		  destCap.addAttribute((String)dspec.extraInfo.get("SITE_TYPE_FIELD_NAME"),"type",AttributeTypes.character);
		  destCap.addAttribute((String)dspec.extraInfo.get("ITEM_CLASS_CODE_FIELD_NAME"),"itemClass",AttributeTypes.character);
		  destCap.addAttribute((String)dspec.extraInfo.get("ITEM_CLASS_FIELD_NAME"),"itemClassName",AttributeTypes.character);
		  destCap.addAttribute((String)dspec.extraInfo.get("CAPACITY_FIELD_NAME"),"capacity",AttributeTypes.integer);
		  for (int i=0; i<destTable.getDataItemCount(); i++) {
		    DataRecord rec=destTable.getDataRecord(i);
		    String id=rec.getAttrValueAsString(siteIdCN);
		    String name=rec.getAttrValueAsString(siteNameCN);
		    if (name==null) name=id;
		    DataRecord r1=new DataRecord(id,name);
		    destCap.addDataRecord(r1);
		    String type=rec.getAttrValueAsString(siteTypeCN);
		    if (type==null) type="unknown";
		    r1.setAttrValue(type,0);
		    String cat=rec.getAttrValueAsString(itemCatCodeCN);
		    if (cat==null ) cat=rec.getAttrValueAsString(itemCatCN);
		    if (cat==null) cat="1";
		    r1.setAttrValue(cat,1);
		    if (itemCatCodeCN>=0)
		      r1.setAttrValue(rec.getAttrValueAsString(itemCatCodeCN),2);
		    r1.setAttrValue(rec.getAttrValueAsString(capacityCN),3);
		  }
		  if (destCap!=null && itemCatNames!=null)
		    ScheduleExplorer.fillItemClassNames(destCap,itemCatNames,itemCatCodes);
		}
		*/
		return true;
	}

	protected boolean writeVehiclesInSourcesData(String fname) {
		if (vehicleLocTable == null || vehicleLocTable.getDataItemCount() < 1) {
			showMessage("No data about vehicle sources!", true);
			return false;
		}
		DataSourceSpec spec = (DataSourceSpec) vehicleLocTable.getDataSource();
		if (spec == null || spec.extraInfo == null) {
			showMessage("Unknown meanings of the columns in the table with the vehicle sources!", true);
			return false;
		}
		int vehicleNumCN = vehicleLocTable.findAttrByName((String) spec.extraInfo.get("VEHICLE_NUMBER_FIELD_NAME"));
		if (vehicleNumCN < 0) {
			showMessage("Table with vehicle sources: no column with the numbers of vehicles found!", true);
			return false;
		}
		String aName = (String) spec.extraInfo.get("SOURCE_ID_FIELD_NAME");
		if (aName == null) {
			aName = spec.idFieldName;
		}
		if (aName == null) {
			aName = "id";
		}
		int siteIdCN = vehicleLocTable.findAttrByName(aName);
		if (siteIdCN < 0) {
			showMessage("Table with vehicle sources: no column with the site identifiers found!", true);
			return false;
		}
		aName = (String) spec.extraInfo.get("SOURCE_NAME_FIELD_NAME");
		if (aName == null) {
			aName = spec.nameFieldName;
		}
		if (aName == null) {
			aName = "name";
		}
		int siteNameCN = vehicleLocTable.findAttrByName(aName);
		int vehicleTypeCN = vehicleLocTable.findAttrByName((String) spec.extraInfo.get("VEHICLE_CLASS_ID_FIELD_NAME"));
		int readyTimeCN = vehicleLocTable.findAttrByName((String) spec.extraInfo.get("READY_TIME_FIELD_NAME"));
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(fname);
		} catch (IOException ioe) {
			showMessage(res.getString("Could_not_create_file") + " " + fname, true);
			System.out.println(ioe);
		}
		if (out == null)
			return false;
		DataOutputStream dos = new DataOutputStream(out);
		try {
			dos.writeBytes("id,Name,vehicle class,number of vehicles,ready time\n");
			for (int i = 0; i < vehicleLocTable.getDataItemCount(); i++) {
				DataRecord rec = vehicleLocTable.getDataRecord(i);
				String id = rec.getAttrValueAsString(siteIdCN);
				String name = rec.getAttrValueAsString(siteNameCN);
				if (name == null) {
					name = id;
				}
				String type = rec.getAttrValueAsString(vehicleTypeCN);
				if (type == null) {
					type = "1";
				}
				String numStr = rec.getAttrValueAsString(vehicleNumCN);
				String timeStr = rec.getAttrValueAsString(readyTimeCN);
				if (timeStr == null) {
					timeStr = "1";
				} else {
					try {
						int t = Integer.parseInt(timeStr);
						if (t < 1) {
							t = 1;
						}
						timeStr = String.valueOf(t);
					} catch (NumberFormatException e) {
						timeStr = "1";
					}
				}
				dos.writeBytes(id + "," + name + "," + type + "," + numStr + "," + timeStr + "\n");
			}
		} catch (IOException ioe) {
			showMessage(res.getString("Error_writing_file") + " " + fname, true);
			System.out.println(ioe);
			return false;
		}
		try {
			out.close();
		} catch (IOException e) {
		}

		return true;
	}

	public boolean writeAllPlaces(String fname) {
		if (locLayer == null || locLayer.getObjectCount() < 1) {
			showMessage("No locations!", true);
			return false;
		}
		int siteTypeCN = -1;
		DataTable table = (DataTable) locLayer.getThematicData();
		if (table != null) {
			DataSourceSpec spec = (DataSourceSpec) table.getDataSource();
			String aName = null;
			if (spec != null && spec.extraInfo != null) {
				aName = (String) spec.extraInfo.get("SITE_TYPE_FIELD_NAME");
			}
			if (aName == null) {
				aName = "type";
			}
			siteTypeCN = table.findAttrByName((String) spec.extraInfo.get("SITE_TYPE_FIELD_NAME"));
		}
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(fname);
		} catch (IOException ioe) {
			showMessage(res.getString("Could_not_create_file") + " " + fname, true);
			System.out.println(ioe);
		}
		if (out == null)
			return false;
		DataOutputStream dos = new DataOutputStream(out);
		Vector siteIds = new Vector(destTable.getDataItemCount(), 10);
		try {
			dos.writeBytes("id,Name,X,Y,type\n");
			for (int i = 0; i < locLayer.getObjectCount(); i++) {
				DGeoObject loc = locLayer.getObject(i);
				if (loc == null || loc.getGeometry() == null) {
					continue;
				}
				Geometry geom = loc.getGeometry();
				RealPoint pt = (geom instanceof RealPoint) ? (RealPoint) geom : null;
				if (pt == null) {
					float r[] = geom.getBoundRect();
					if (r != null) {
						pt = new RealPoint((r[0] + r[2]) / 2, (r[1] + r[3]) / 2);
					} else {
						continue;
					}
				}
				String type = "";
				if (siteTypeCN >= 0) {
					DataRecord rec = (DataRecord) loc.getData();
					type = rec.getAttrValueAsString(siteTypeCN);
					if (type == null) {
						type = "";
					}
				}
				dos.writeBytes(loc.getIdentifier() + "," + loc.getLabel() + "," + pt.x + "," + pt.y + "," + type + "\n");
			}
		} catch (IOException ioe) {
			showMessage(res.getString("Error_writing_file") + " " + fname, true);
			System.out.println(ioe);
			return false;
		}
		try {
			out.close();
		} catch (IOException e) {
		}

		return true;
	}

	public void writeDistanceMatrix(String fname) {
		if (distancesMatrix == null)
			return;
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(fname);
		} catch (IOException ioe) {
			showMessage(res.getString("Could_not_create_file") + " " + fname, true);
			System.out.println(ioe);
		}
		if (out == null)
			return;
		DataOutputStream dos = new DataOutputStream(out);
		try {
			dos.writeBytes("From,To,distance,Driving_Time\n");
			for (int i = 0; i < distancesMatrix.getNRows(); i++) {
				String id1 = distancesMatrix.getRowIdentifier(i);
				if (id1 != null) {
					for (int j = 0; j < distancesMatrix.getNColumns(); j++) {
						String id2 = distancesMatrix.getColumnIdentifier(j);
						if (id2 == null || id2.equalsIgnoreCase(id1)) {
							continue;
						}
						String str = id1 + "," + id2 + ",";
						Object val = distancesMatrix.get(0, i, j);
						if (val != null && (val instanceof Float)) {
							Float f = (Float) val;
							if (f.isNaN()) {
								str += "NaN";
							} else {
								str += f.toString();
							}
						}
						str += ",";
						val = distancesMatrix.get(1, i, j);
						if (val != null && (val instanceof Float)) {
							Float f = (Float) val;
							if (f.isNaN()) {
								str += String.valueOf(Short.MAX_VALUE);
							} else {
								int t = f.intValue();
								if (t < 1) {
									t = 1;
								}
								str += String.valueOf(t);
							}
						}
						dos.writeBytes(str + "\n");
					}
				}
			}
		} catch (IOException ioe) {
			showMessage(res.getString("Error_writing_file") + " " + fname, true);
			System.out.println(ioe);
		}
		try {
			out.close();
		} catch (IOException e) {
		}
	}

	public boolean writeItemClasses(String fname) {
		if ((itemCatCodes == null || itemCatCodes.size() < 1) && (itemCatNames == null || itemCatNames.size() < 1)) {
			showMessage("No item classes!", true);
			return false;
		}
		FileWriter out = null;
		try {
			out = new FileWriter(fname);
		} catch (IOException ioe) {
			showMessage("Could not create file " + fname, true);
			System.out.println(ioe);
		}
		if (out == null)
			return false;
		try {
			out.write("id,description\n");
			out.write("0,LEER\n");
			int ncodes = (itemCatCodes != null) ? itemCatCodes.size() : itemCatNames.size();
			for (int i = 0; i < ncodes; i++) {
				String code = (itemCatCodes != null) ? (String) itemCatCodes.elementAt(i) : (String) itemCatNames.elementAt(i);
				String name = (itemCatNames != null) ? (String) itemCatNames.elementAt(i) : (String) itemCatCodes.elementAt(i);
				out.write(code + "," + name + "\n");
			}
		} catch (IOException ioe) {
			showMessage(res.getString("Error_writing_file") + " " + fname, true);
			System.out.println(ioe);
			return false;
		}
		try {
			out.close();
		} catch (IOException e) {
		}
		return true;
	}

	public boolean writeVehicleClasses(String fname) {
		if (vehicleTypesInfo == null || vehicleTypesInfo.getNofVehicleClasses() < 1) {
			showMessage("No vehicle classes!", true);
			return false;
		}
		FileWriter out = null;
		try {
			out = new FileWriter(fname);
		} catch (IOException ioe) {
			showMessage(res.getString("Could_not_create_file") + " " + fname, true);
			System.out.println(ioe);
		}
		if (out == null)
			return false;
		try {
			out.write("id,description,speed_factor\n");
			out.write("0,virtual,1.0\n");
			for (int i = 0; i < vehicleTypesInfo.getNofVehicleClasses(); i++) {
				String sf = "1.0";
				if (vehicleTypesInfo.speedFactors != null && vehicleTypesInfo.speedFactors.size() > i) {
					sf = String.valueOf(vehicleTypesInfo.speedFactors.elementAt(i));
				}
				out.write(vehicleTypesInfo.getVehicleClassId(i) + "," + vehicleTypesInfo.getVehicleClassName(i) + "," + sf + "\n");
			}
		} catch (IOException ioe) {
			showMessage(res.getString("Error_writing_file") + " " + fname, true);
			System.out.println(ioe);
			return false;
		}
		try {
			out.close();
		} catch (IOException e) {
		}
		return true;
	}

	public boolean writeVehicleSuitAndLoadTimes(String suitFName, String loadTimeFName) {
		if (vehicleTypesInfo == null || vehicleTypesInfo.getNofVehicleClasses() < 1) {
			showMessage("No vehicle classes!", true);
			return false;
		}
		DataTable table = vehicleTypesInfo.vehicleCap;
		if (table == null || table.getDataItemCount() < 1) {
			showMessage("No data about the suitability of vehicles!", true);
			return false;
		}
		DataSourceSpec spec = (DataSourceSpec) table.getDataSource();
		Hashtable info = (spec != null) ? spec.extraInfo : null;
		String aName = (info != null) ? (String) info.get("VEHICLE_CAPACITY_FIELD_NAME") : null;
		if (aName == null) {
			aName = "capacity";
		}
		int capCN = table.findAttrByName(aName);
		if (capCN < 0) {
			capCN = table.getAttrIndex("capacity");
		}
		if (capCN < 0) {
			showMessage("Unknown capacities of vehicles!", true);
			return false;
		}
		aName = (info != null) ? (String) info.get("VEHICLE_CLASS_ID_FIELD_NAME") : null;
		if (aName == null) {
			aName = "vehicle type";
		}
		int vTypeCN = table.findAttrByName(aName);
		if (vTypeCN < 0) {
			vTypeCN = table.getAttrIndex("vehicle_type");
		}
		aName = (info != null) ? (String) info.get("ITEM_CLASS_FIELD_NAME") : null;
		if (aName == null) {
			aName = "item type";
		}
		int iTypeCN = table.findAttrByName(aName);
		if (iTypeCN < 0) {
			vTypeCN = table.getAttrIndex("item_type");
		}
		aName = (info != null) ? (String) info.get("VEHICLE_LOAD_TIME_FIELD_NAME") : null;
		if (aName == null) {
			aName = "loading time";
		}
		int loadTimeCN = table.findAttrByName(aName);
		if (loadTimeCN < 0) {
			loadTimeCN = table.getAttrIndex("load_time");
		}

		FileWriter outSuit = null, outLoad = null;
		try {
			outSuit = new FileWriter(suitFName);
		} catch (IOException ioe) {
			showMessage(res.getString("Could_not_create_file") + " " + suitFName, true);
			System.out.println(ioe);
		}
		try {
			outLoad = new FileWriter(loadTimeFName);
		} catch (IOException ioe) {
			showMessage(res.getString("Could_not_create_file") + " " + loadTimeFName, true);
			System.out.println(ioe);
		}
		if (outSuit == null)
			return false;
		try {
			outSuit.write("vehicle type,item type,capacity\n");
			outSuit.write("0,0,1\n");
			//add records for the "virtual" vehicle
			Vector iCodes = itemCatCodes;
			if (iCodes == null) {
				iCodes = itemCatNames;
			}
			for (int i = 0; i < iCodes.size(); i++) {
				outSuit.write("0," + iCodes.elementAt(i) + ",1\n");
			}
			outLoad.write("vehicle type,item type,loading time\n");
			for (int i = 0; i < table.getDataItemCount(); i++) {
				DataRecord rec = table.getDataRecord(i);
				double val = rec.getNumericAttrValue(capCN);
				if (Double.isNaN(val) || val < 1) {
					continue;
				}
				int cap = (int) Math.round(val);
				int vtIdx = vehicleTypesInfo.getVehicleClassIndex(rec.getAttrValueAsString(vTypeCN));
				if (vtIdx < 0) {
					continue;
				}
				int itIdx = getItemCatIdx(rec.getAttrValueAsString(iTypeCN));
				if (itIdx < 0) {
					continue;
				}
				float loadTime = 1.0f;
				if (loadTimeCN >= 0) {
					val = rec.getNumericAttrValue(loadTimeCN);
					if (!Double.isNaN(val) && val > 0) {
						loadTime = (float) val;
					}
				}
				outSuit.write(vehicleTypesInfo.getVehicleClassId(vtIdx) + "," + iCodes.elementAt(itIdx) + "," + cap + "\n");
				outLoad.write(vehicleTypesInfo.getVehicleClassId(vtIdx) + "," + iCodes.elementAt(itIdx) + "," + loadTime + "\n");
			}
		} catch (IOException ioe) {
			showMessage("Error: " + ioe.toString(), true);
			return false;
		}
		try {
			outSuit.close();
		} catch (IOException e) {
		}
		try {
			outLoad.close();
		} catch (IOException e) {
		}
		return true;
	}

	protected int getItemCatIdx(String cat) {
		if (cat == null)
			return -1;
		int idx = -1;
		if (itemCatCodes != null) {
			idx = StringUtil.indexOfStringInVectorIgnoreCase(cat, itemCatCodes);
		}
		if (idx < 0 && itemCatNames != null) {
			idx = StringUtil.indexOfStringInVectorIgnoreCase(cat, itemCatNames);
		}
		return idx;
	}

}
