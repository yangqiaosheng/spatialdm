package core;

import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.DataKeeper;
import spade.analysis.system.DataLoader;
import spade.analysis.system.MapToolbar;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.PopupManager;
import spade.lib.basicwin.TImgButton;
import spade.lib.basicwin.TImgButtonGroup;
import spade.lib.lang.Language;
import spade.lib.util.CopyFile;
import spade.lib.util.Parameters;
import spade.vis.action.ObjectEvent;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataTable;
import spade.vis.database.ObjectContainer;
import spade.vis.event.DEvent;
import spade.vis.event.EventConsumer;
import spade.vis.mapvis.SimpleDataMapper;
import spade.vis.mapvis.Visualizer;
import spade.vis.space.GeoLayer;
import configstart.MapVisInitiator;

/**
* Suports hierarchical selection of territories: allows the user to select
* territories (as areas in some map) and loads projects corresponding to these
* territories. In a new map loaded, the user can again select some sub-territory,
* and so on. Return to the previous level of the hierarchy is supported.
*/
public class TerritorySelector extends ObjSelectorHTML implements ActionListener, EventConsumer {
	static ResourceBundle res = Language.getTextResource("core.Res");
	/**
	* Used for loading projects (*.APP files) describing territories of different
	* levels
	*/
	private DataLoader dataLoader = null;
	protected String sDataLocation = "";
	/**
	* The table with data availability attributes and geometry files of different
	* levels
	*/
	protected AttributeDataPortion dTable = null;
	/**
	* The indexes and namees of the attributes containing the names of app-files
	* for uper and lower levels in the hierarchy of territories.
	*/
	protected int levelUpAttrIdx = -1, levelDownAttrIdx = -1;
	protected String partOfAttrName = null, levelDownAttrName = null;

	protected String currentEventMeaning = null;
	protected String appFile = null;
	/**
	* System parameters, source of necessary information
	*/
	private Parameters sysParams = null;
	/**
	* Specific for GIMMI: names and indexes of attributes showing data availability
	*/
	protected String soilAttrName = null, climateAttrName = null, cropAttrName = null;
	protected int soilAttrIdx = -1, climateAttrIdx = -1, cropAttrIdx = -1;
	/**
	* UI elements
	*/
	private MapToolbar mtb = null;
	protected TImgButton bSelect = null, bGeobrowse = null, bGeobrowseUp = null, bSubmit = null;
	protected TImgButtonGroup geobrowseBGr = null;
	public static String cmdGeobrowseUp = "GeobrowseUp", cmdSelect = "Select", cmdGeobrowse = "Geobrowse", cmdSubmit = "Submit";
	protected boolean consumesEvents = false;

	/**
	* Finds the container (table or layer) in which to select territories.
	* For this purpose, uses the parameter OBJECT_SELECTION_IN in the system
	* settings (this parameter comes from an *.APP file). If this parameter is
	* not set, takes the active map layer or the first table available.
	*/
	public static ObjectContainer findObjContainerForSelection(DataKeeper dKeeper, Parameters params) {
		if (dKeeper == null)
			return null;
		ObjectContainer cont = null;
		if (params != null) {
			String objContId = params.getParameterAsString("OBJECT_SELECTION_IN");
			System.out.println(">>AS: objContId=" + objContId);
			if (objContId != null) {
				if (CopyFile.hasSeparator(objContId)) {
					objContId = CopyFile.getName(objContId);
					System.out.println(">>AS: modified objContId=" + objContId);
				}
				if (dKeeper.getMap(0) != null) {
					int idx = dKeeper.getMap(0).getIndexOfLayer(objContId);
					if (idx >= 0) {
						GeoLayer layer = dKeeper.getMap(0).getGeoLayer(idx);
						if (layer.getObjectCount() < 1) {
							layer.loadGeoObjects();
						}
						cont = (ObjectContainer) layer;
					}
				}
				if (cont == null) {
					int idx = dKeeper.getTableIndex(objContId);
					if (idx >= 0) {
						AttributeDataPortion table = dKeeper.getTable(idx);
						if (table != null) {
							GeoLayer layer = dKeeper.getTableLayer(table);
							if (layer != null) {
								cont = (ObjectContainer) layer;
							} else {
								cont = (ObjectContainer) table;
							}
						}
					}
				}
			}
		}
		if (cont == null) {
			GeoLayer layer = dKeeper.getMap(0).getActiveLayer();
			if (layer == null) {
				layer = dKeeper.getMap(0).getGeoLayer(0);
			}
			if (layer != null) {
				if (layer.getObjectCount() < 1) {
					layer.loadGeoObjects();
				}
				cont = (ObjectContainer) layer;
			} else {
				cont = (ObjectContainer) dKeeper.getTable(0);
			}
		}
		if (cont == null)
			return null;
		if (params == null)
			return cont;
		String partOfAttrName = params.getParameterAsString("PART_OF_ATTR_NAME"), levelDownAttrName = params.getParameterAsString("LEVEL_DOWN_ATTR_NAME");
		if (partOfAttrName == null && levelDownAttrName == null)
			return cont;
		boolean hierarchy = false;
		AttributeDataPortion table = null;
		if (cont instanceof AttributeDataPortion) {
			table = (AttributeDataPortion) cont;
		} else if (cont instanceof GeoLayer) {
			table = ((GeoLayer) cont).getThematicData();
		}
		if (table != null) {
			hierarchy = table.findAttrByName(partOfAttrName) >= 0 || table.findAttrByName(levelDownAttrName) >= 0;
		}
		if (!hierarchy) {
			for (int i = 0; i < dKeeper.getTableCount() && !hierarchy; i++) {
				AttributeDataPortion t = dKeeper.getTable(i);
				if (!t.equals(table) && (t.findAttrByName(partOfAttrName) >= 0 || t.findAttrByName(levelDownAttrName) >= 0)) {
					hierarchy = true;
					table = t;
				}
			}
		}
		if (!hierarchy)
			return cont;
		GeoLayer layer = dKeeper.getTableLayer(table);
		if (layer != null) {
			if (!layer.hasThematicData(table)) {
				if (layer.getObjectCount() < 1) {
					layer.loadGeoObjects();
				}
				layer.receiveThematicData(table);
			}
			cont = (ObjectContainer) layer;
		} else {
			cont = (ObjectContainer) table;
		}
		return cont;
	}

	/**
	* Sets the container with the objects that may be selected. The container
	* is, in principle, not strictly required. If the container is available, it
	* may be used for getting object names in addition to object identifiers.
	*/
	@Override
	public void setObjectContainer(ObjectContainer cont) {
		super.setObjectContainer(cont);
		dTable = null;
		if (objCont != null) {
			if (objCont instanceof AttributeDataPortion) {
				dTable = (AttributeDataPortion) objCont;
			} else if (objCont instanceof GeoLayer) {
				GeoLayer layer = (GeoLayer) objCont;
				dTable = layer.getThematicData();
			}
			if (dTable != null) {
				if (levelDownAttrName != null) {
					levelDownAttrIdx = dTable.findAttrByName(levelDownAttrName);
				}
				if (partOfAttrName != null) {
					levelUpAttrIdx = dTable.findAttrByName(partOfAttrName);
				}
			}
		}
		startEventConsuming();
	}

	/**
	* Sets the data loader to be used for loading projects (*.APP files)
	* describing territories of different levels.
	*/
	public void setDataLoader(DataLoader dLoad) {
		dataLoader = dLoad;
		//System.out.println("Set data loader = "+dataLoader.toString());
	}

	/**
	* Checks if everything that is needed for loading application is available:
	* 1) supervisor;
	* 2) table;
	* 3) index of the attribute containing app-file
	*/
	protected boolean canOpen() {
		return supervisor != null && dTable != null && (levelDownAttrIdx >= 0 || levelUpAttrIdx >= 0);
	}

	protected void startEventConsuming() {
		if (consumesEvents)
			return;
		if (!canOpen())
			return;
		currentEventMeaning = supervisor.getObjectEventMeaningManager().getCurrentEventMeaning(ObjectEvent.click);
		supervisor.getObjectEventMeaningManager().addEventMeaning(ObjectEvent.click, "browse", "Geo-browse");
		supervisor.registerObjectEventConsumer(this, ObjectEvent.click, "browse", "Geo-Browse");
		supervisor.getObjectEventMeaningManager().setCurrentEventMeaning(ObjectEvent.click, (checkDownLevel() > -1) ? "browse" : currentEventMeaning);
		consumesEvents = true;
	}

	protected void checkEventConsuming() {
		if (consumesEvents) {
			supervisor.getObjectEventMeaningManager().setCurrentEventMeaning(ObjectEvent.click, (checkDownLevel() > -1) ? "browse" : currentEventMeaning);
		} else {
			startEventConsuming();
		}
	}

	protected void restoreEventMeaning() {
		if (currentEventMeaning != null) {
			supervisor.getObjectEventMeaningManager().setCurrentEventMeaning(ObjectEvent.click, currentEventMeaning);
		}
	}

	/**
	* Sets the name of the attribute with app-file name
	*/
	public void setNavigationAttributes(int attrUpIdx, int attrDownIdx) {
		levelUpAttrIdx = attrUpIdx;
		levelDownAttrIdx = attrDownIdx;
		if (levelUpAttrIdx > -1) {
			partOfAttrName = dTable.getAttributeName(levelUpAttrIdx);
		}
		if (levelDownAttrIdx > -1) {
			levelDownAttrName = dTable.getAttributeName(levelDownAttrIdx);
		}
		//System.out.println("Attribute with level-up applications = "+partOfAttrName+" N="+levelUpAttrIdx);
		//System.out.println("Attribute with level-down applications = "+levelDownAttrName+" N="+levelDownAttrIdx);
		startEventConsuming();
	}

	private boolean setDataAvailabilityAttributes() {
		if (sysParams == null)
			return false;
		soilAttrName = sysParams.getParameterAsString("soil_attr");
		climateAttrName = sysParams.getParameterAsString("climate_attr");
		cropAttrName = sysParams.getParameterAsString("crop_attr");
		if (dTable == null)
			return false;
		soilAttrIdx = dTable.findAttrByName(soilAttrName);
		climateAttrIdx = dTable.findAttrByName(climateAttrName);
		cropAttrIdx = dTable.findAttrByName(cropAttrName);
		//System.out.println("soilAttrIdx="+soilAttrIdx);
		//System.out.println("climateAttrIdx="+climateAttrIdx);
		//System.out.println("cropAttrIdx="+cropAttrIdx);
		return soilAttrIdx > -1 && climateAttrIdx > -1 && cropAttrIdx > -1;
	}

	protected String getCurrGeomId() {
		if (objCont == null)
			return null;
		String id = objCont.getContainerIdentifier();
		if (id == null)
			return null;
		if (CopyFile.hasSeparator(id)) {
			id = CopyFile.getName(id);
		}
		return id;
	}

	private int checkTopLevel() {
		if (dTable == null)
			return -1;
		System.out.println("Try to check existence of top level geometry...");
		int rowN = -1;
		String sCurrentArea = getCurrGeomId();
		System.out.println("Current geometry=" + sCurrentArea);
		if (sCurrentArea == null)
			return -1;
		for (int i = 0; i < dTable.getDataItemCount() && rowN < 0; i++) {
			String sValue = dTable.getAttrValueAsString(levelDownAttrIdx, i);
			if (sValue != null && sValue.equalsIgnoreCase(sCurrentArea)) {
				rowN = i;
			}
		}
		if (rowN < 0) {
			System.out.println("...not found!");
		}
		return rowN;
	}

	private int checkDownLevel() {
		if (dTable == null)
			return -1;
		System.out.println("Try to check existence of down level geometry...");
		int rowN = -1;
		String sCurrentArea = getCurrGeomId();
		System.out.println("Current geometry=" + sCurrentArea);
		if (sCurrentArea == null)
			return -1;
		for (int i = 0; i < dTable.getDataItemCount() && rowN < 0; i++) {
			String sValueDown = dTable.getAttrValueAsString(levelDownAttrIdx, i);
			String sValueUp = dTable.getAttrValueAsString(levelUpAttrIdx, i);
			if (sValueDown != null && sValueDown.length() > 0 && sValueUp.equalsIgnoreCase(sCurrentArea)) {
				rowN = i;
			}
		}
		if (rowN < 0) {
			System.out.println("...not found!");
		}
		//System.out.println("rowN="+rowN);
		return rowN;
	}

	private boolean findDataAvailable(String sTerritory) {
		boolean data_available = false;
		if (dTable != null && sTerritory != null) {
			sTerritory.trim();
			if (sTerritory.length() < 1)
				return false;
			String sCurrentTerr = null;
			for (int j = 0; j < dTable.getDataItemCount() && !data_available; j++) {
				sCurrentTerr = dTable.getAttrValueAsString(levelUpAttrIdx, j);
				if (sCurrentTerr != null && sCurrentTerr.equalsIgnoreCase(sTerritory)) {
					String soil = dTable.getAttrValueAsString(soilAttrIdx, j);
					String climate = dTable.getAttrValueAsString(climateAttrIdx, j);
					String crop = dTable.getAttrValueAsString(cropAttrIdx, j);

					data_available = data_available || (soil != null && soil.equalsIgnoreCase("yes")) || (climate != null && climate.equalsIgnoreCase("yes")) || (crop != null && crop.equalsIgnoreCase("yes"));
					if (data_available) {
						/*
						System.out.println("Territory: "+sCurrentTerr+" ");
						System.out.println("Soil: "+soil);
						System.out.println("Climate: "+climate);
						System.out.println("Crop: "+crop);
						System.out.println("Name: "+dTable.getDataItem(j).getName());
						System.out.println("******************************************");
						*/
					}
				}
			}
		}
		return data_available;
	}

	private void calculateDataAvailability() {
		if (objCont == null || sysParams == null)
			return;

		DataTable metaData = null;
		if (dTable instanceof DataTable) {
			metaData = (DataTable) dTable;
		}
		if (metaData == null)
			return;

		int nldaAttrN = metaData.findAttrByName("NEXT_LEVEL_DATA_AVAILABLE");
		if (nldaAttrN < 0) {
			Vector srcAttrs = new Vector(3);
			srcAttrs.addElement(dTable.getAttributeId(soilAttrIdx));
			srcAttrs.addElement(dTable.getAttributeId(climateAttrIdx));
			srcAttrs.addElement(dTable.getAttributeId(cropAttrIdx));

			nldaAttrN = metaData.addDerivedAttribute("NEXT_LEVEL_DATA_AVAILABLE", AttributeTypes.character, AttributeTypes.compute, srcAttrs);
		}

		//System.out.println("levelUpAttrIdx="+levelUpAttrIdx);
		//System.out.println("soilAttrIdx="+soilAttrIdx);
		//System.out.println("climateAttrIdx="+climateAttrIdx);
		//System.out.println("cropAttrIdx="+cropAttrIdx);
		for (int i = 0; i < dTable.getDataItemCount(); i++) {
			String str = dTable.getAttrValueAsString(levelDownAttrIdx, i);
			//System.out.println("Call findDataAvailable for "+str);
			boolean dataFound = str != null && findDataAvailable(str);
			//System.out.println("dataFound="+dataFound);
			metaData.getDataRecord(i).setAttrValue((dataFound ? "yes" : "no"), nldaAttrN);
		}

		if (!(objCont instanceof GeoLayer))
			return;
		GeoLayer layer = (GeoLayer) objCont;
		SimpleDataMapper mapper = new SimpleDataMapper();

		Vector attr = new Vector(1);
		attr.addElement(dTable.getAttributeId(nldaAttrN));
		Vector attr_all = dTable.getAllAttrValues(attr);
		//System.out.println("Next level data availability: "+attr_all);
		if (attr_all != null && attr_all.size() > 1) {
			String methodId = "qualitative_colour";
			Object vis = mapper.constructVisualizer(methodId, layer.getType());
			if (vis != null) {
				vis = mapper.visualizeAttributes(vis, methodId, dTable, attr, layer.getType());
			}
			if (vis != null && (vis instanceof Visualizer)) {
				MapVisInitiator.displayOnMap((Visualizer) vis, methodId, dTable, attr, layer, false, supervisor, supervisor.getUI().getCurrentMapViewer());
			}
		}
		attr = new Vector(3);
		if (soilAttrIdx >= 0) {
			attr.addElement(dTable.getAttributeId(soilAttrIdx));
		}
		if (climateAttrIdx >= 0) {
			attr.addElement(dTable.getAttributeId(climateAttrIdx));
		}
		if (cropAttrIdx >= 0) {
			attr.addElement(dTable.getAttributeId(cropAttrIdx));
		}
		if (attr.size() > 0) {
			String methodId = "stacks";
			Object vis = mapper.constructVisualizer(methodId, layer.getType());
			if (vis != null) {
				vis = mapper.visualizeAttributes(vis, methodId, dTable, attr, layer.getType());
			}
			if (vis != null && (vis instanceof Visualizer)) {
				MapVisInitiator.displayOnMap((Visualizer) vis, methodId, dTable, attr, layer, true, supervisor, supervisor.getUI().getCurrentMapViewer());
				MapVisInitiator.makeMapManipulator((Visualizer) vis, methodId, dTable, layer, supervisor, supervisor.getUI().getCurrentMapViewer(), mapper);
			}
		}
	}

	@Override
	public void prepareToWork() {
		super.prepareToWork();
		if (supervisor == null) {
			System.out.println("AppOpener has no access to system's supervisor!");
			return;
		}
		sysParams = supervisor.getSystemSettings();
		if (sysParams == null) {
			System.out.println("AppOpener has no access to system parameters!");
			return;
		}
		getParameterValues();
		if (dTable != null) {
			if (levelDownAttrName != null) {
				levelDownAttrIdx = dTable.findAttrByName(levelDownAttrName);
			}
			if (partOfAttrName != null) {
				levelUpAttrIdx = dTable.findAttrByName(partOfAttrName);
			}
		}
		makeButtonsInToolbar();
		initLinkToHTML();
		boolean attrsOK = setDataAvailabilityAttributes();
		if (attrsOK) {
			calculateDataAvailability();
		}
		startEventConsuming();
	}

	protected void getParameterValues() {
		if (sysParams == null)
			return;
		appFile = sysParams.getParameterAsString("Application");
		partOfAttrName = sysParams.getParameterAsString("PART_OF_ATTR_NAME");
		levelDownAttrName = sysParams.getParameterAsString("LEVEL_DOWN_ATTR_NAME");
		sDataLocation = sysParams.getParameterAsString("Data_From");
		System.out.println("partOfAttrName=" + partOfAttrName);
		System.out.println("levelDownAttrName=" + levelDownAttrName);
		System.out.println("data_from=" + sDataLocation);
		System.out.println("appFile=" + appFile);
	}

	protected void makeButtonsInToolbar() {
		if (supervisor.getUI().getCurrentMapViewer() == null) {
			supervisor.getUI().openMapView(0);
		}
		if (supervisor.getUI().getCurrentMapViewer() == null)
			return;
		mtb = supervisor.getUI().getCurrentMapViewer().getMapToolbar();
		if (mtb == null)
			return;
		//System.out.println("Making interface buttons...");
		boolean geoLevelsDownAvailable = (checkDownLevel() > -1), topLevelAppAvailable = (checkTopLevel() > -1);
		//System.out.println("topLevelAppAvailable="+topLevelAppAvailable);
		//System.out.println("geoLevelsDownAvailable="+geoLevelsDownAvailable);
		Panel p = new Panel();
		p.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 1));
		if (geoLevelsDownAvailable) {
			//System.out.println("add DOWN button");
			p.add(new Label(" "));
			geobrowseBGr = new TImgButtonGroup();
			geobrowseBGr.addActionListener(this);
			p.add(geobrowseBGr);
			geobrowseBGr.addButton(bGeobrowse = new TImgButton("/icons/geobrowse.gif"));
			new PopupManager(bGeobrowse, res.getString("Geobrowse_ON"), true);
			//new PopupManager(bGeobrowse,"Browse objects mode",true);
			bGeobrowse.setActionCommand(cmdGeobrowse);
			geobrowseBGr.addButton(bSelect = new TImgButton("/icons/Check.gif"));
			new PopupManager(bSelect, res.getString("Geobrowse_OFF"), true);
			//new PopupManager(bSelect,"Select objects mode",true);
			bSelect.setActionCommand(cmdSelect);
			//geobrowseBGr.setVisible(geoLevelsDownAvailable);
		}
		if (topLevelAppAvailable) {
			//System.out.println("add UP button");
			p.add(new Label(" "));
			p.add(bGeobrowseUp = new TImgButton("/icons/LevelUp.gif"));
			new PopupManager(bGeobrowseUp, res.getString("Level_Up"), true);
			//new PopupManager(bGeobrowseUp,"Level Up",true);
			bGeobrowseUp.setActionCommand(cmdGeobrowseUp);
			bGeobrowseUp.addActionListener(this);
			//bGeobrowseUp.setVisible(topLevelAppAvailable);
		}
		mtb.addToolbarElement(p);
		CManager.validateAll(p);
	}

	/**
	* Opens the specified app-file.
	*/
	protected void openApplication(String path) {
		clearFields();
		if (path == null) {
			System.out.println("ERROR: Cannot open application: path was not specified!");
			showMessage("ERROR: Cannot open application: path was not specified!", true);
			return;
		}
		if (dataLoader == null) {
			System.out.println("ERROR: Cannot open application: DataLoader is not available!");
			showMessage("ERROR: Cannot open application: DataLoader is not available!", true);
			return;
		}
		if (supervisor.getUI() == null) {
			System.out.println("ERROR: No UI found!");
			showMessage("ERROR: No UI found!", true);
			return;
		}
		restoreEventMeaning();
		//String sDocBase=supervisor.getSystemSettings().getParameterAsString("DocumentBase");
		System.out.println("Trying to open application: path=" + path);

		supervisor.getUI().closeMapView(0);
		dataLoader.removeMap(0);
		for (int i = dataLoader.getTableCount() - 1; i >= 0; i--) {
			String tableId = dataLoader.getTable(i).getContainerIdentifier();
			dataLoader.removeTable(i);
			supervisor.tableIsRemoved(tableId);
		}
		partOfAttrName = null;
		levelDownAttrName = null;
		levelUpAttrIdx = -1;
		levelDownAttrIdx = -1;
		dTable = null;
		objCont = null;
		if (!dataLoader.loadApplication(path, null))
			return;
		supervisor.getUI().openMapView(0);
		refreshFields();
		getParameterValues();
		ObjectContainer oc = findObjContainerForSelection(dataLoader, sysParams);
		if (oc == null)
			return;
		setObjectContainer(oc);
		makeButtonsInToolbar();
		boolean attrsOK = setDataAvailabilityAttributes();
		if (attrsOK) {
			calculateDataAvailability();
		}
		checkEventConsuming();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals(cmdGeobrowse)) {
			supervisor.getObjectEventMeaningManager().setCurrentEventMeaning(ObjectEvent.click, "browse");
			return;
		}
		if (e.getActionCommand().equals(cmdSelect)) {
			supervisor.getObjectEventMeaningManager().setCurrentEventMeaning(ObjectEvent.click, "highlight");
			return;
		}
		if (e.getActionCommand().equals(cmdGeobrowseUp)) {
			goTopLevel();
			return;
		}
	}

	private void goTopLevel() {
		String sTopArea = null;

		sTopArea = dTable.getAttrValueAsString(levelUpAttrIdx, checkTopLevel());
		if (sTopArea != null && sTopArea.length() > 0) {
			if (sDataLocation != null) {
				sTopArea = sDataLocation + sTopArea;
			}
			System.out.println("Top geometry= " + sTopArea);
			sysParams.setParameter("%1", sTopArea);
			sysParams.setParameter("%2", sTopArea.substring(0, sTopArea.lastIndexOf(".")) + ".dbf");
			System.out.println("Top geometry file = " + sysParams.getParameterAsString("%1"));
			System.out.println("Top geometry data = " + sysParams.getParameterAsString("%2"));
			openApplication(appFile);
		}
	}

	//--------------------------- EventConsumer interface ---------------------
	/**
	* The EventReceiver answers whether it is interested in getting the specified
	* kind of events.
	*/
	@Override
	public boolean doesListenToEvent(String eventId) {
		return eventId != null && (eventId.equals(ObjectEvent.select) || eventId.equals(ObjectEvent.click));
	}

	/**
	* This method is used for delivering events to the Event Receiver.
	*/
	@Override
	public void eventOccurred(DEvent evt) {
		if (!(evt instanceof ObjectEvent))
			return;
		String sMeaning = supervisor.getObjectEventMeaningManager().getCurrentEventMeaning(evt.getId());
		if (!sMeaning.equalsIgnoreCase("browse"))
			return;
		ObjectEvent oe = (ObjectEvent) evt;
		Vector obj = oe.getAffectedObjects();
		if (obj == null || obj.size() < 1) {
			objects.removeAllElements();
			clearFields();
			return;
		}
		if (oe.getSetIdentifier() == null || !oe.getSetIdentifier().equals(dTable.getEntitySetIdentifier()))
			return;

		String levelDownGeometry = null;
		for (int i = 0; i < obj.size() && levelDownGeometry == null; i++) {
			int recN = dTable.indexOf((String) obj.elementAt(i));

			if (recN < 0) {
				continue;
			}
			levelDownGeometry = dTable.getAttrValueAsString(levelDownAttrIdx, recN);
			if (levelDownGeometry != null)
				if (levelDownGeometry.length() < 1 || levelDownGeometry.equalsIgnoreCase("no")) {
					levelDownGeometry = null;
				}
		}
		if (levelDownGeometry == null)
			return;
		//System.out.println("levelDownGeometry = "+levelDownGeometry);
		String sGeomFileFull = (sDataLocation != null) ? sDataLocation + levelDownGeometry : levelDownGeometry;
		sysParams.setParameter("%1", sGeomFileFull);
		sysParams.setParameter("%2", sGeomFileFull.substring(0, sGeomFileFull.lastIndexOf(".")) + ".dbf");
		System.out.println("Top geometry file = " + sysParams.getParameterAsString("%1"));
		System.out.println("Top geometry data = " + sysParams.getParameterAsString("%2"));
		openApplication(appFile);
	}

	/**
	* Returns a unique identifier of the event receiver (may be produced
	* automatically, used only internally, not shown to the user).
	*/
	@Override
	public String getIdentifier() {
		if (dTable != null)
			return "TerrSelector_" + dTable.getContainerIdentifier();
		return "TerrSelector";
	}

	/**
	* The Event Consumer answers whether it consumes the specified event
	* when it has the given meaning.
	*/
	@Override
	public boolean doesConsumeEvent(String evtType, String eventMeaning) {
		return (evtType.equalsIgnoreCase(ObjectEvent.click) || evtType.equalsIgnoreCase(ObjectEvent.select)) && eventMeaning.endsWith("browse");
	}

	/**
	* The Event Consumer answers whether it consumes the specified event
	* when it has the given meaning.
	*/
	@Override
	public boolean doesConsumeEvent(DEvent evt, String eventMeaning) {
		return doesConsumeEvent(evt.getId(), eventMeaning);
	}

	/**
	* Makes necessary operations for destroying, e.g. unregisters from
	* listening highlighting and other events.
	*/
	@Override
	public void destroy() {
		if (isDestroyed())
			return;
		if (supervisor != null) {
			restoreEventMeaning();
			supervisor.removeObjectEventConsumer(this, ObjectEvent.click, "browse");
			supervisor.removeObjectEventReceiver(this);
		}
		super.destroy();
	}
}
