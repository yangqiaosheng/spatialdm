package spade.analysis.tools.schedule;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.TextField;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.ESDACore;
import spade.analysis.tools.EnterPointOnMapUI;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.Dialogs;
import spade.lib.basicwin.FoldablePanel;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.NotificationLine;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.OKFrame;
import spade.lib.basicwin.TextCanvas;
import spade.lib.lang.Language;
import spade.lib.util.IntArray;
import spade.lib.util.StringUtil;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.database.SpatialEntity;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DLayerManager;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RealPoint;
import spade.vis.map.MapContext;
import spade.vis.map.PlaceMarker;
import spade.vis.space.GeoObject;
import spade.vis.spec.DataSourceSpec;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: 14-Nov-2007
 * Time: 15:20:02
 * A UI for entering and editing data about the numbers of vehicles
 * suitable for evacuation and their original locations
 */
public class VehiclesInSourcesEditUI extends Frame implements ActionListener, ItemListener, Destroyable {
	static ResourceBundle res = Language.getTextResource("spade.analysis.tools.schedule.SchedulerTexts_tools_schedule");

	protected ESDACore core = null;
	/**
	 * The geographical layer with the source locations of the vehicles
	 */
	public DGeoLayer vehicleLocLayer = null;
	/**
	 * The geographical layer with the source locations (which may also be vehicle sources)
	 */
	public DGeoLayer souLayer = null;
	/**
	 * The geographical layer with the destination locations (which may also be vehicle sources)
	 */
	public DGeoLayer destLayer = null;
	/**
	 * The table with data about the sources, in particular, numbers of
	 * vehicles, possibly, belonging to different categories
	 */
	public DataTable vehicleLocTable = null;
	/**
	 * Indices of relevant table columns
	 */
	protected int siteIdCN = -1, siteNameCN = -1, vehicleTypeCN = -1, vehicleNumCN = -1, readyTimeCN = -1;
	/**
	 * Keeps information about classes of vehicles used for transportation.
	 */
	public VehicleTypesInfo vehicleTypesInfo = null;
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
	 * A matrix of vehicle capacities by item categories
	 */
	protected int capacities[][];
	/**
	 * The layer with locations which may (or may not) contain source
	 * locations and/or destination locations and/or locations of vehicles
	 */
	public DGeoLayer locLayer = null;
	/**
	 * For each category, contains the total number of items to evacuate.
	 */
	public int itemTotalNumbers[] = null;
	/**
	 * The owner must be notified when the editing is finished
	 */
	protected ActionListener owner = null;
	/**
	 * UI elements
	 */
	protected Panel mainP = null;
	protected GridBagLayout gridbag = null;
	protected GridBagConstraints gbc = null;
	protected CheckboxGroup cbg = null;
	/**
	 * Contains radio buttons corresponding to the sites
	 */
	protected Vector siteCheckers = null;
	/**
	 * Contains the identifiers of the sites corresponding to the radio buttons
	 */
	protected Vector siteIdsOfCheckers = null;
	/**
	 * Contains text fields for item numbers in the sites.
	 * There may be several fields per site depending on the item categories
	 * present in a site.
	 */
	protected Vector itemNumTextFields = null;
	/**
	 * Contains text fields for the available times.
	 * There may be several fields per site depending on the item categories
	 * present in a site.
	 */
	protected Vector maxTimeTextFields = null;
	/**
	 * Contains the identifiers of the sites corresponding to the text fields
	 */
	protected Vector siteIdsOfTextFields = null;
	/**
	 * Contains the numbers of the table records corresponding to the text fields
	 */
	protected IntArray recNums = null;
	/**
	 * The index of the currently selected site
	 */
	protected int currSiteIdx = -1;
	/**
	 * These text fields show the total available capacities by item categories
	 */
	protected TextField totalCapTextFields[] = null;

	protected NotificationLine notLine = null;
	/**
	 * Used for non-modal dialogs
	 */
	protected OKFrame okFrame = null;

	protected EnterPointOnMapUI enterPointUI = null;

	protected PlaceMarker placeMarker = null;

	protected DLayerManager lman = null;
	protected boolean layerWasAdded = false;
	protected boolean destroyed = false;

	/**
	 * @param vehicleLocLayer - The geographical layer with the locations of the available vehicles
	 * @param vehicleLocTable - The table with data about the availability of different types
	 *                          of vehicles in the locations specified in vehicleLocLayer
	 * @param vehicleTypesInfo - Keeps information about classes of vehicles used for transportation.
	 * @param itemCatNames - the names of the categories of the transported items
	 * @param itemCatCodes - the codes of the categories of the transported items
	 * @param itemTotalNumbers - for each category, contains the total number of items to evacuate
	 * @param locLayer - a layer with locations from which additional source
	 *                   locations may be taken
	 */
	public VehiclesInSourcesEditUI(DGeoLayer vehicleLocLayer, DataTable vehicleLocTable, VehicleTypesInfo vehicleTypesInfo, Vector itemCatNames, Vector itemCatCodes, int itemTotalNumbers[], DGeoLayer locLayer, DGeoLayer souLayer, DGeoLayer destLayer) {
		super(res.getString("vehicles_UI_title"));
		if (vehicleLocTable == null || vehicleTypesInfo == null)
			return;
		DataSourceSpec spec = (DataSourceSpec) vehicleLocTable.getDataSource();
		if (spec == null || spec.extraInfo == null)
			return;
		if (spec.extraInfo.get("SOURCE_ID_FIELD_NAME") != null) {
			siteIdCN = vehicleLocTable.findAttrByName((String) spec.extraInfo.get("SOURCE_ID_FIELD_NAME"));
		}
		if (siteIdCN < 0)
			return;
		if (spec.extraInfo.get("SOURCE_NAME_FIELD_NAME") != null) {
			siteNameCN = vehicleLocTable.findAttrByName((String) spec.extraInfo.get("SOURCE_NAME_FIELD_NAME"));
		}
		if (spec.extraInfo.get("VEHICLE_CLASS_ID_FIELD_NAME") != null) {
			vehicleTypeCN = vehicleLocTable.findAttrByName((String) spec.extraInfo.get("VEHICLE_CLASS_ID_FIELD_NAME"));
		}
		if (spec.extraInfo.get("VEHICLE_NUMBER_FIELD_NAME") != null) {
			vehicleNumCN = vehicleLocTable.findAttrByName((String) spec.extraInfo.get("VEHICLE_NUMBER_FIELD_NAME"));
		}
		if (vehicleNumCN < 0)
			return;
		if (spec.extraInfo.get("READY_TIME_FIELD_NAME") != null) {
			readyTimeCN = vehicleLocTable.findAttrByName((String) spec.extraInfo.get("READY_TIME_FIELD_NAME"));
		}

		this.vehicleLocLayer = vehicleLocLayer;
		this.vehicleLocTable = vehicleLocTable;
		this.vehicleTypesInfo = vehicleTypesInfo;
		this.itemCatNames = itemCatNames;
		this.itemCatCodes = itemCatCodes;
		this.locLayer = locLayer;
		this.souLayer = souLayer;
		this.destLayer = destLayer;
		this.itemTotalNumbers = itemTotalNumbers;
		vehicleTypesInfo.removeVirtualType();
		retrieveCapacities();

		setLayout(new BorderLayout());
		Panel p = new Panel(new ColumnLayout());
		notLine = new NotificationLine(null);
		p.add(notLine);
		if (itemTotalNumbers != null && itemTotalNumbers.length > 0) {
			totalCapTextFields = new TextField[itemTotalNumbers.length];
			GridBagLayout gb = new GridBagLayout();
			GridBagConstraints c = new GridBagConstraints();
			c.weightx = 1.0f;
			c.weighty = 1.0f;
			c.fill = GridBagConstraints.HORIZONTAL;
			Panel pp = new Panel(gb);
			if (itemTotalNumbers.length > 1) {
				Label l = new Label(res.getString("item_cat"));
				c.gridwidth = 3;
				gb.setConstraints(l, c);
				pp.add(l);
			}
			Label l = new Label(res.getString("Number_of_items"));
			c.gridwidth = 1;
			gb.setConstraints(l, c);
			pp.add(l);
			l = new Label(res.getString("Sum_of_capacities"));
			gb.setConstraints(l, c);
			pp.add(l);
			l = new Label("");
			c.gridwidth = GridBagConstraints.REMAINDER;
			gb.setConstraints(l, c);
			pp.add(l);
			for (int i = 0; i < itemTotalNumbers.length; i++) {
				if (itemTotalNumbers.length > 1) {
					String cat = (itemCatNames != null) ? (String) itemCatNames.elementAt(i) : (itemCatCodes != null) ? (String) itemCatCodes.elementAt(i) : "";
					l = new Label(cat);
					c.gridwidth = 3;
					gb.setConstraints(l, c);
					pp.add(l);
				}
				l = new Label(String.valueOf(itemTotalNumbers[i]), Label.CENTER);
				c.gridwidth = 1;
				gb.setConstraints(l, c);
				pp.add(l);
				totalCapTextFields[i] = new TextField("0");
				totalCapTextFields[i].setEditable(false);
				c.gridwidth = 1;
				gb.setConstraints(totalCapTextFields[i], c);
				pp.add(totalCapTextFields[i]);
				l = new Label("");
				c.gridwidth = GridBagConstraints.REMAINDER;
				gb.setConstraints(l, c);
				pp.add(l);
			}
			FoldablePanel fp = new FoldablePanel(pp, new Label(res.getString("Items_to_evacuate_")));
			p.add(fp);
			p.add(new Line(false));
		}
		p.add(new Label(res.getString("Available_vehicles_and_their_locations"), Label.CENTER));
		add(p, BorderLayout.NORTH);

		p = new Panel(new GridLayout(2, 1));
		Panel p1 = new Panel(new FlowLayout(FlowLayout.CENTER, 5, 1));
		p1.add(new Label(res.getString("curr_site_")));

		boolean hasVehicleTypes = vehicleTypesInfo.getNofVehicleClasses() > 1;
		if (hasVehicleTypes) {
			Button b = new Button(res.getString("add_vehicle_type"));
			b.setActionCommand("add_vehicle_type");
			b.addActionListener(this);
			p1.add(b);
		}

		Panel p2 = new Panel(new BorderLayout());
		p2.add(p1, BorderLayout.WEST);
		p1 = new Panel(new FlowLayout(FlowLayout.CENTER, 5, 1));
		Button b = new Button(res.getString("remove_site"));
		b.setActionCommand("remove_site");
		b.addActionListener(this);
		p1.add(b);
		p2.add(p1, BorderLayout.EAST);
		p.add(p2);
		p2 = new Panel(new GridLayout(1, 3));
		p1 = new Panel(new FlowLayout(FlowLayout.CENTER, 5, 1));
		b = new Button(res.getString("add_site"));
		b.setActionCommand("add_site");
		b.addActionListener(this);
		p1.add(b);
		p2.add(p1);
		p1 = new Panel(new FlowLayout(FlowLayout.CENTER, 5, 1));
		b = new Button(res.getString("Done"));
		b.setActionCommand("done");
		b.addActionListener(this);
		p1.add(b);
		p2.add(p1);
		p2.add(new Label(""));
		p.add(p2);
		add(p, BorderLayout.SOUTH);

		makeMainPanel();
		add(mainP, BorderLayout.CENTER);
		pack();
		Dimension s = getSize(), ss = getToolkit().getScreenSize();
		int w = s.width, h = s.height;

		remove(mainP);
		ScrollPane scp = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
		scp.add(mainP);
		add(scp, BorderLayout.CENTER);
		w += scp.getVScrollbarWidth() + 10;
		h += 10;
		if (itemNumTextFields.size() < 5) {
			h += 100;
		}
		if (w > ss.width / 2) {
			w = ss.width / 2;
		}
		if (h > ss.height * 2 / 3) {
			h = ss.height * 2 / 3;
		}

		setBounds(ss.width - w, 0, w, h);
		setVisible(true);
	}

	protected void makeMainPanel() {
		if (mainP == null) {
			siteCheckers = new Vector(50, 50);
			siteIdsOfCheckers = new Vector(50, 50);
			itemNumTextFields = new Vector(100, 50);
			maxTimeTextFields = new Vector(100, 50);
			siteIdsOfTextFields = new Vector(100, 50);
			recNums = new IntArray(100, 50);

			gridbag = new GridBagLayout();
			gbc = new GridBagConstraints();
			gbc.weightx = 1.0f;
			gbc.weighty = 1.0f;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			mainP = new Panel(gridbag);
			cbg = new CheckboxGroup();
		} else {
			mainP.removeAll();
			siteCheckers.removeAllElements();
			siteIdsOfCheckers.removeAllElements();
			itemNumTextFields.removeAllElements();
			maxTimeTextFields.removeAllElements();
			siteIdsOfTextFields.removeAllElements();
			recNums.removeAllElements();
		}

		boolean hasVehicleTypes = vehicleTypesInfo.getNofVehicleClasses() > 1;

		Label l = new Label("");
		gbc.gridwidth = 1;
		gridbag.setConstraints(l, gbc);
		mainP.add(l);
		l = new Label(res.getString("ID"));
		gbc.gridwidth = 1;
		gridbag.setConstraints(l, gbc);
		mainP.add(l);
		l = new Label(res.getString("Name"));
		gbc.gridwidth = 3;
		gridbag.setConstraints(l, gbc);
		mainP.add(l);
		if (hasVehicleTypes) {
			l = new Label(res.getString("Type_of_vehicle"));
			gbc.gridwidth = 4;
			gridbag.setConstraints(l, gbc);
			mainP.add(l);
		}
		l = new Label(res.getString("Number"));
		gbc.gridwidth = 2;
		gridbag.setConstraints(l, gbc);
		mainP.add(l);
		l = new Label(res.getString("Ready_time"));
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(l, gbc);
		mainP.add(l);

		boolean hasTotalCapFields = totalCapTextFields != null;
		int capAvail[] = null;
		if (hasTotalCapFields) {
			capAvail = new int[totalCapTextFields.length];
			for (int i = 0; i < capAvail.length; i++) {
				capAvail[i] = 0;
			}
		}

		if (vehicleLocTable.getDataItemCount() > 0) {
			for (int i = 0; i < vehicleLocTable.getDataItemCount(); i++) {
				DataRecord rec = vehicleLocTable.getDataRecord(i);
				String siteId = rec.getAttrValueAsString(siteIdCN);
				if (siteId == null || StringUtil.isStringInVectorIgnoreCase(siteId, siteIdsOfCheckers)) {
					continue;
				}
				siteIdsOfCheckers.addElement(siteId);
				Checkbox cb = new Checkbox("", false, cbg);
				cb.addItemListener(this);
				siteCheckers.addElement(cb);
				gbc.gridwidth = 1;
				gridbag.setConstraints(cb, gbc);
				mainP.add(cb);
				l = new Label(siteId);
				gbc.gridwidth = 1;
				gridbag.setConstraints(l, gbc);
				mainP.add(l);
				l = new Label(rec.getAttrValueAsString(siteNameCN));
				gbc.gridwidth = 3;
				gridbag.setConstraints(l, gbc);
				mainP.add(l);
				int vtIdx = 0;
				if (hasVehicleTypes) {
					vtIdx = getVehicleTypeIdx(rec.getAttrValueAsString(vehicleTypeCN));
					String cat = vehicleTypesInfo.getVehicleClassName(vtIdx);
					if (cat == null) {
						cat = "";
					}
					l = new Label(cat);
					gbc.gridwidth = 4;
					gridbag.setConstraints(l, gbc);
					mainP.add(l);
				}
				TextField tf = new TextField(rec.getAttrValueAsString(vehicleNumCN));
				if (hasTotalCapFields) {
					tf.addActionListener(this);
				}
				itemNumTextFields.addElement(tf);
				gbc.gridwidth = 2;
				gridbag.setConstraints(tf, gbc);
				mainP.add(tf);
				tf = new TextField(rec.getAttrValueAsString(readyTimeCN));
				maxTimeTextFields.addElement(tf);
				gbc.gridwidth = GridBagConstraints.REMAINDER;
				gridbag.setConstraints(tf, gbc);
				mainP.add(tf);
				siteIdsOfTextFields.addElement(siteId);
				recNums.addElement(i);
				if (capAvail != null) {
					double val = rec.getNumericAttrValue(vehicleNumCN);
					if (!Double.isNaN(val) && val > 0) {
						int num = (int) Math.round(val);
						for (int k = 0; k < capAvail.length; k++) {
							capAvail[k] += getCapacity(vtIdx, k) * num;
						}
					}
				}
				for (int j = i + 1; j < vehicleLocTable.getDataItemCount(); j++) {
					rec = vehicleLocTable.getDataRecord(j);
					if (siteId.equalsIgnoreCase(rec.getAttrValueAsString(siteIdCN))) {
						l = new Label("");
						gbc.gridwidth = 5;
						gridbag.setConstraints(l, gbc);
						mainP.add(l);
						int vtidx = 0;
						if (hasVehicleTypes) {
							vtIdx = getVehicleTypeIdx(rec.getAttrValueAsString(vehicleTypeCN));
							String cat = vehicleTypesInfo.getVehicleClassName(vtIdx);
							if (cat == null) {
								cat = "";
							}
							l = new Label(cat);
							gbc.gridwidth = 4;
							gridbag.setConstraints(l, gbc);
							mainP.add(l);
						}
						tf = new TextField(rec.getAttrValueAsString(vehicleNumCN));
						if (hasTotalCapFields) {
							tf.addActionListener(this);
						}
						itemNumTextFields.addElement(tf);
						gbc.gridwidth = 2;
						gridbag.setConstraints(tf, gbc);
						mainP.add(tf);
						tf = new TextField(rec.getAttrValueAsString(readyTimeCN));
						maxTimeTextFields.addElement(tf);
						gbc.gridwidth = GridBagConstraints.REMAINDER;
						gridbag.setConstraints(tf, gbc);
						mainP.add(tf);
						siteIdsOfTextFields.addElement(siteId);
						recNums.addElement(j);
						if (capAvail != null) {
							double val = rec.getNumericAttrValue(vehicleNumCN);
							if (!Double.isNaN(val) && val > 0) {
								int num = (int) Math.round(val);
								for (int k = 0; k < capAvail.length; k++) {
									capAvail[k] += getCapacity(vtIdx, k) * num;
								}
							}
						}
					}
				}
			}
		}
		if (capAvail != null) {
			for (int i = 0; i < capAvail.length; i++) {
				totalCapTextFields[i].setText(String.valueOf(capAvail[i]));
				boolean notEnough = itemTotalNumbers[i] > 0 && capAvail[i] < 1;
				totalCapTextFields[i].setForeground((notEnough) ? Color.red.darker() : getForeground());
				totalCapTextFields[i].setBackground((notEnough) ? Color.pink : getBackground());
			}
		}
	}

	public void setCore(ESDACore core) {
		this.core = core;
		if (core != null) {
			lman = (DLayerManager) core.getUI().getCurrentMapViewer().getLayerManager();
			if (lman != null && vehicleLocLayer != null) {
				int layerIdx = lman.getIndexOfLayer(vehicleLocLayer.getContainerIdentifier());
				if (layerIdx < 0) {
					lman.addGeoLayer(vehicleLocLayer);
					layerIdx = lman.getLayerCount() - 1;
					layerWasAdded = true;
				}
				lman.activateLayer(layerIdx);
			}
		}
	}

	/**
	 * The owner must be notified when the editing is finished
	 */
	public void setOwner(ActionListener owner) {
		this.owner = owner;
	}

	protected int getVehicleTypeIdx(String type) {
		return vehicleTypesInfo.getVehicleClassIndex(type);
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

	protected int getCapacity(int vehicleTypeIdx, int itemTypeIdx) {
		if (capacities == null)
			return 0;
		if (vehicleTypeIdx < 0 || vehicleTypeIdx >= capacities.length)
			return 0;
		if (itemTypeIdx < 0 || itemTypeIdx >= capacities[vehicleTypeIdx].length)
			return 0;
		return capacities[vehicleTypeIdx][itemTypeIdx];
	}

	/**
	 * Retrieves vehicle capacities from the table in vehicleTypesInfo
	 */
	protected void retrieveCapacities() {
		if (capacities != null)
			return;
		DataTable table = vehicleTypesInfo.vehicleCap;
		if (table == null)
			return;
		DataSourceSpec spec = (DataSourceSpec) table.getDataSource();
		Hashtable info = (spec != null) ? spec.extraInfo : null;
		String aName = (info != null) ? (String) info.get("VEHICLE_CAPACITY_FIELD_NAME") : null;
		if (aName == null) {
			aName = "capacity";
		}
		int capCN = table.findAttrByName(aName);
		if (capCN < 0)
			return;
		aName = (info != null) ? (String) info.get("VEHICLE_CLASS_ID_FIELD_NAME") : null;
		if (aName == null) {
			aName = "vehicle type";
		}
		int vTypeCN = table.findAttrByName(aName);
		aName = (info != null) ? (String) info.get("ITEM_CLASS_FIELD_NAME") : null;
		if (aName == null) {
			aName = "item type";
		}
		int iTypeCN = table.findAttrByName(aName);

		int nItemTypes = 1;
		if (itemCatCodes != null && itemCatCodes.size() > 1) {
			nItemTypes = itemCatCodes.size();
		} else if (itemCatNames != null && itemCatNames.size() > 1) {
			nItemTypes = itemCatNames.size();
		}
		if (nItemTypes > 1 && iTypeCN < 0)
			return;
		int nVehicleTypes = (vehicleTypesInfo != null) ? vehicleTypesInfo.getNofVehicleClasses() : 1;
		if (nVehicleTypes < 1) {
			nVehicleTypes = 1;
		}
		if (nVehicleTypes > 1 && vTypeCN < 0)
			return;

		capacities = new int[nVehicleTypes][nItemTypes];
		for (int i = 0; i < nVehicleTypes; i++) {
			for (int j = 0; j < nItemTypes; j++) {
				capacities[i][j] = 0;
			}
		}

		for (int i = 0; i < table.getDataItemCount(); i++) {
			DataRecord rec = table.getDataRecord(i);
			double val = rec.getNumericAttrValue(capCN);
			if (Double.isNaN(val) || val < 1) {
				continue;
			}
			int cap = (int) Math.round(val);
			int vtIdx = 0;
			if (nVehicleTypes > 1) {
				vtIdx = getVehicleTypeIdx(rec.getAttrValueAsString(vTypeCN));
				if (vtIdx < 0) {
					continue;
				}
			}
			int itIdx = 0;
			if (nItemTypes > 1) {
				itIdx = getItemCatIdx(rec.getAttrValueAsString(iTypeCN));
				if (itIdx < 0) {
					continue;
				}
			}
			capacities[vtIdx][itIdx] = cap;
		}
	}

	/**
	 * Writes the data entered or edited by the user to the table
	 */
	protected void putDataToTable(boolean removeEmpty) {
		if (vehicleLocTable == null)
			return;
		IntArray recNumsToRemove = (removeEmpty) ? new IntArray(recNums.size(), 1) : null;
		Vector objIdsHaveItems = new Vector(siteIdsOfCheckers.size(), 1);
		boolean hasTotalCapFields = totalCapTextFields != null;
		int capAvail[] = null;
		if (hasTotalCapFields) {
			capAvail = new int[totalCapTextFields.length];
			for (int i = 0; i < capAvail.length; i++) {
				capAvail[i] = 0;
			}
		}
		boolean hasVehicleTypes = vehicleTypesInfo.getNofVehicleClasses() > 1;

		for (int i = 0; i < itemNumTextFields.size(); i++) {
			TextField tf = (TextField) itemNumTextFields.elementAt(i);
			String str = tf.getText();
			int num = 0;
			if (str != null) {
				str = str.trim();
				if (str.length() > 0) {
					try {
						num = Integer.parseInt(str);
					} catch (NumberFormatException nfe) {
						num = 0;
					}
				}
			}
			if (removeEmpty && num < 1) {
				recNumsToRemove.addElement(recNums.elementAt(i));
				continue;
			}
			int time = 0;
			tf = (TextField) maxTimeTextFields.elementAt(i);
			str = tf.getText();
			if (str != null) {
				str = str.trim();
				if (str.length() > 0) {
					try {
						time = Integer.parseInt(str);
					} catch (NumberFormatException nfe) {
						time = 0;
					}
				}
			}
			DataRecord rec = vehicleLocTable.getDataRecord(recNums.elementAt(i));
			String siteId = rec.getAttrValueAsString(siteIdCN);
			if (!StringUtil.isStringInVectorIgnoreCase(siteId, objIdsHaveItems)) {
				objIdsHaveItems.addElement(siteId);
			}
			rec.setNumericAttrValue(num, String.valueOf(num), vehicleNumCN);
			rec.setNumericAttrValue(time, String.valueOf(time), readyTimeCN);
			if (num > 0 && capAvail != null) {
				int vtIdx = 0;
				if (hasVehicleTypes) {
					vtIdx = getVehicleTypeIdx(rec.getAttrValueAsString(vehicleTypeCN));
				}
				for (int k = 0; k < capAvail.length; k++) {
					capAvail[k] += getCapacity(vtIdx, k) * num;
				}
			}
		}
		if (capAvail != null) {
			for (int i = 0; i < capAvail.length; i++) {
				totalCapTextFields[i].setText(String.valueOf(capAvail[i]));
				boolean notEnough = itemTotalNumbers[i] > 0 && capAvail[i] < 1;
				totalCapTextFields[i].setForeground((notEnough) ? Color.red.darker() : getForeground());
				totalCapTextFields[i].setBackground((notEnough) ? Color.pink : getBackground());
			}
		}
		if (removeEmpty && recNumsToRemove.size() > 0) {
			for (int i = recNumsToRemove.size() - 1; i >= 0; i--) {
				vehicleLocTable.removeDataItem(recNumsToRemove.elementAt(i));
			}
		}
		if (removeEmpty && vehicleLocLayer != null) {
			for (int i = vehicleLocLayer.getObjectCount() - 1; i >= 0; i--)
				if (!StringUtil.isStringInVectorIgnoreCase(vehicleLocLayer.getObjectId(i), objIdsHaveItems)) {
					vehicleLocLayer.removeGeoObject(i);
				}
		}
	}

	public void actionPerformed(ActionEvent e) {
		notLine.showMessage(null, false);
		if (e.getSource() instanceof TextField && okFrame == null) {
			putDataToTable(false);
			return;
		}
		String cmd = e.getActionCommand();
		if (cmd == null)
			return;
		if (okFrame != null) {
			if (e.getSource().equals(okFrame) && cmd.equalsIgnoreCase("closed")) {
				Component comp = okFrame.getMainComponent();
				boolean cancelled = okFrame.wasCancelled();
				okFrame = null;
				if (comp instanceof SiteSelectionUI) {
					SiteSelectionUI siteSelUI = (SiteSelectionUI) comp;
					if (cancelled) {
						siteSelUI.destroy();
						markCurrentSite();
					} else {
						finishSiteSelection(siteSelUI);
					}
				}
			}
			return;
		}
		if (cmd.equals("done")) {
			//check if all capacities have been correctly specified
			boolean hasTotalCapFields = totalCapTextFields != null;
			int capAvail[] = null;
			if (hasTotalCapFields) {
				capAvail = new int[totalCapTextFields.length];
				for (int i = 0; i < capAvail.length; i++) {
					capAvail[i] = 0;
				}
			}
			boolean hasVehicleTypes = vehicleTypesInfo.getNofVehicleClasses() > 1;

			for (int i = 0; i < itemNumTextFields.size(); i++) {
				TextField tf = (TextField) itemNumTextFields.elementAt(i);
				String str = tf.getText();
				if (str == null) {
					continue;
				}
				str = str.trim();
				if (str.length() < 1) {
					continue;
				}
				int num = 0;
				String errMsg = null;
				try {
					num = Integer.parseInt(str);
				} catch (NumberFormatException nfe) {
					errMsg = res.getString("illegal_string") + ": [" + str + "]";
				}
				if (errMsg == null && num < 0) {
					errMsg = res.getString("illegal_number") + " (" + num + "); " + res.getString("must_be_positive_or_0");
				}
				if (errMsg != null) {
					tf.requestFocus();
					tf.selectAll();
					str = res.getString("Illegal_number_of_vehicles_in_site") + " " + siteIdsOfTextFields.elementAt(i);
					if (siteNameCN >= 0) {
						str += " \"" + vehicleLocTable.getAttrValueAsString(siteNameCN, recNums.elementAt(i)) + "\"";
					}
					str += ": " + errMsg;
					notLine.showMessage(str, true);
					return;
				}
				if (num < 1) {
					continue;
				}
				if (capAvail != null) {
					int vtIdx = 0;
					if (hasVehicleTypes) {
						vtIdx = getVehicleTypeIdx(vehicleLocTable.getAttrValueAsString(vehicleTypeCN, recNums.elementAt(i)));
					}
					for (int k = 0; k < capAvail.length; k++) {
						capAvail[k] += getCapacity(vtIdx, k) * num;
					}
				}
				int time = 0;
				tf = (TextField) maxTimeTextFields.elementAt(i);
				str = tf.getText();
				if (str == null) {
					continue;
				}
				str = str.trim();
				if (str.length() < 1) {
					continue;
				}
				try {
					time = Integer.parseInt(str);
				} catch (NumberFormatException nfe) {
					errMsg = res.getString("illegal_string") + ": [" + str + "]";
				}
				if (errMsg == null && time < 0) {
					errMsg = res.getString("illegal_number") + " (" + time + "); " + res.getString("must_be_positive_or_0");
				}
				if (errMsg != null) {
					tf.requestFocus();
					tf.selectAll();
					str = res.getString("Illegal_readiness_time_in_site") + " " + siteIdsOfTextFields.elementAt(i);
					if (siteNameCN >= 0) {
						str += " \"" + vehicleLocTable.getAttrValueAsString(siteNameCN, recNums.elementAt(i)) + "\"";
					}
					str += ": " + errMsg;
					notLine.showMessage(str, true);
					return;
				}
			}
			String errMsg = null;
			if (capAvail != null) {
				for (int i = 0; i < capAvail.length; i++) {
					totalCapTextFields[i].setText(String.valueOf(capAvail[i]));
					boolean notEnough = itemTotalNumbers[i] > 0 && capAvail[i] < 1;
					totalCapTextFields[i].setForeground((notEnough) ? Color.red.darker() : getForeground());
					totalCapTextFields[i].setBackground((notEnough) ? Color.pink : getBackground());
					if (notEnough) {
						if (errMsg == null) {
							errMsg = res.getString("No_vehicles_for_transporting") + " ";
						} else {
							errMsg += ", ";
						}
						if (itemCatNames != null) {
							errMsg += (String) itemCatNames.elementAt(i);
						} else if (itemCatCodes != null) {
							errMsg += (String) itemCatCodes.elementAt(i);
						} else {
							errMsg += res.getString("any_items");
						}
					}
				}
			}
			if (errMsg != null) {
				notLine.showMessage(errMsg, true);
				return;
			}
			dispose();
			putDataToTable(true);
			if (owner != null) {
				owner.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "vehicles_in_sources"));
			}
			return;
		}
		if (cmd.equals("add_vehicle_type")) {
			if (currSiteIdx < 0)
				return;
			String siteId = (String) siteIdsOfCheckers.elementAt(currSiteIdx);
			String siteName = null;
			int nTypes = vehicleTypesInfo.getNofVehicleClasses();
			IntArray exTypeIdxs = new IntArray(nTypes, 1);
			int firstIdx = -1, lastIdx = -1;
			for (int i = 0; i < siteIdsOfTextFields.size(); i++)
				if (siteId.equals(siteIdsOfTextFields.elementAt(i))) {
					if (firstIdx < 0) {
						firstIdx = i;
					}
					lastIdx = i;
					DataRecord rec = vehicleLocTable.getDataRecord(recNums.elementAt(i));
					if (siteName == null) {
						siteName = rec.getAttrValueAsString(siteNameCN);
					}
					int vtIdx = getVehicleTypeIdx(rec.getAttrValueAsString(vehicleTypeCN));
					if (vtIdx >= 0) {
						exTypeIdxs.addElement(vtIdx);
					}
				} else if (lastIdx >= 0) {
					break;
				}
			if (exTypeIdxs.size() >= nTypes) {
				Dialogs.showMessage(this, "There_are_no_more_vehicle_types_", res.getString("No_more_types_"));
				return;
			}
			int catIdxs[] = new int[nTypes - exTypeIdxs.size()];
			Panel p = new Panel(new GridLayout(catIdxs.length, 1));
			int k = 0;
			Checkbox cb[] = new Checkbox[catIdxs.length];
			Vector types = vehicleTypesInfo.vehicleClassNames;
			if (types == null) {
				types = vehicleTypesInfo.vehicleClassIds;
			}
			for (int i = 0; i < types.size(); i++)
				if (exTypeIdxs.indexOf(i) < 0) {
					cb[k] = new Checkbox((String) types.elementAt(i), false);
					p.add(cb[k]);
					catIdxs[k++] = i;
				}
			OKDialog okDia = new OKDialog(this, res.getString("Select_the_types_to_add"), true);
			okDia.addContent(p);
			okDia.show();
			if (okDia.wasCancelled())
				return;
			putDataToTable(false);
			boolean added = false;
			for (int i = 0; i < cb.length; i++)
				if (cb[i].getState()) {
					DataRecord rec = new DataRecord(siteId + (vehicleLocTable.getDataItemCount() + 1), siteName);
					vehicleLocTable.addDataRecord(rec);
					rec.setAttrValue(siteId, siteIdCN);
					rec.setAttrValue(siteName, siteNameCN);
					rec.setAttrValue(vehicleTypesInfo.getVehicleClassId(catIdxs[i]), vehicleTypeCN);
					rec.setNumericAttrValue(0, "0", vehicleNumCN);
					added = true;
				}
			if (!added)
				return;
			Dimension d0 = mainP.getPreferredSize();
			mainP.setVisible(false);
			makeMainPanel();
			((Checkbox) siteCheckers.elementAt(currSiteIdx)).setState(true);
			Dimension d1 = mainP.getPreferredSize();
			int dh = d1.height - d0.height;
			d0 = getSize();
			d1 = getToolkit().getScreenSize();
			if (d0.height + dh <= d1.height * 2 / 3) {
				setSize(d0.width, d0.height + dh);
			}
			mainP.invalidate();
			mainP.setVisible(true);
			validate();
			return;
		}
		if (cmd.equals("remove_site")) {
			if (currSiteIdx < 0)
				return;
			String siteId = (String) siteIdsOfCheckers.elementAt(currSiteIdx);
			String siteName = null;
			int firstIdx = -1, lastIdx = -1;
			for (int i = 0; i < siteIdsOfTextFields.size(); i++)
				if (siteId.equals(siteIdsOfTextFields.elementAt(i))) {
					if (firstIdx < 0) {
						firstIdx = i;
					}
					lastIdx = i;
					DataRecord rec = vehicleLocTable.getDataRecord(recNums.elementAt(i));
					if (siteName == null) {
						siteName = rec.getAttrValueAsString(siteNameCN);
					}
				} else if (lastIdx >= 0) {
					break;
				}
			if (firstIdx < 0)
				return;
			if (!Dialogs.askYesOrNo(this, res.getString("wish_remove_site") + " \"" + siteName + "\" (ID = " + siteId + ")?", res.getString("Confirm")))
				return;
			erasePlaceMarker();
			putDataToTable(false);
			if (vehicleLocLayer != null) {
				int layerIdx = lman.getIndexOfLayer(vehicleLocLayer.getContainerIdentifier());
				if (layerIdx >= 0) {
					lman.activateLayer(layerIdx);
					DGeoLayer layer = (DGeoLayer) lman.getGeoLayer(layerIdx);
					Graphics g = core.getUI().getCurrentMapViewer().getMapDrawer().getGraphics();
					MapContext mc = core.getUI().getCurrentMapViewer().getMapDrawer().getMapContext();
					layer.highlightObject(siteId, false, g, mc);
				}
				int idx = vehicleLocLayer.getObjectIndex(siteId);
				if (idx >= 0) {
					vehicleLocLayer.removeGeoObject(idx);
				}
			}
			currSiteIdx = -1;
			for (int i = lastIdx; i >= firstIdx; i--) {
				int recN = recNums.elementAt(i);
				vehicleLocTable.removeDataItem(recN);
			}
			mainP.setVisible(false);
			makeMainPanel();
			mainP.invalidate();
			mainP.setVisible(true);
			validate();
			return;
		}
		if (cmd.equals("add_site")) {
			erasePlaceMarker();
			addSites();
			return;
		}
		if (cmd.equals("make_point") && e.getSource().equals(enterPointUI)) {
			RealPoint point = enterPointUI.getPoint();
			if (point == null) {
				markCurrentSite();
				return;
			}
			enterPointUI.restorePoint();
			GridBagLayout gb = new GridBagLayout();
			GridBagConstraints c = new GridBagConstraints();
			c.weightx = 1.0f;
			c.weighty = 1.0f;
			c.fill = GridBagConstraints.HORIZONTAL;
			Panel p = new Panel(gb);
			Label l = new Label(res.getString("Site_ID") + ":", Label.RIGHT);
			c.gridwidth = 1;
			gb.setConstraints(l, c);
			p.add(l);
			TextField siteIdTF = new TextField(10);
			c.gridwidth = GridBagConstraints.REMAINDER;
			gb.setConstraints(siteIdTF, c);
			p.add(siteIdTF);
			l = new Label(res.getString("Site_name") + ":", Label.RIGHT);
			c.gridwidth = 1;
			gb.setConstraints(l, c);
			p.add(l);
			TextField siteNameTF = new TextField(20);
			c.gridwidth = GridBagConstraints.REMAINDER;
			gb.setConstraints(siteNameTF, c);
			p.add(siteNameTF);
			Checkbox cb[] = null;
			Vector types = vehicleTypesInfo.vehicleClassNames;
			if (types == null) {
				types = vehicleTypesInfo.vehicleClassIds;
			}
			if (types != null && types.size() < 1) {
				types = null;
			}
			if (types != null) {
				l = new Label(res.getString("Vehicle_types") + ":", Label.CENTER);
				c.gridwidth = GridBagConstraints.REMAINDER;
				gb.setConstraints(l, c);
				p.add(l);
				cb = new Checkbox[types.size()];
				for (int i = 0; i < types.size(); i++) {
					cb[i] = new Checkbox((String) types.elementAt(i), false);
					c.gridwidth = GridBagConstraints.REMAINDER;
					gb.setConstraints(cb[i], c);
					p.add(cb[i]);
				}
			}
			OKDialog okDia = new OKDialog(this, res.getString("New_site"), true);
			okDia.addContent(p);
			String siteId = null, siteName = null, siteType = null;
			do {
				okDia.show();
				if (!okDia.wasCancelled()) {
					siteId = siteIdTF.getText();
					if (siteId != null) {
						siteId = siteId.trim();
						if (siteId.length() < 1) {
							siteId = null;
						}
					}
					siteName = siteNameTF.getText();
					if (siteName != null) {
						siteName = siteName.trim();
						if (siteName.length() < 1) {
							siteName = null;
						}
					}
					if (siteId == null) {
						Dialogs.showMessage(this, res.getString("No_identifier_specified"), res.getString("Error") + "!");
					} else if (siteName == null) {
						Dialogs.showMessage(this, res.getString("No_name_specified"), res.getString("Error") + "!");
					}
				}
			} while (!okDia.wasCancelled() && (siteId == null || siteName == null));
			enterPointUI.erasePoint();
			if (okDia.wasCancelled()) {
				markCurrentSite();
				return;
			}
			putDataToTable(false);
			if (siteType == null) {
				siteType = "unknown";
			}
			boolean added = false;
			for (int i = 0; i < cb.length; i++)
				if (cb[i].getState()) {
					DataRecord rec = new DataRecord(siteId + (vehicleLocTable.getDataItemCount() + 1), siteName);
					vehicleLocTable.addDataRecord(rec);
					rec.setAttrValue(siteId, siteIdCN);
					rec.setAttrValue(siteName, siteNameCN);
					if (types != null) {
						rec.setAttrValue(vehicleTypesInfo.getVehicleClassId(i), vehicleTypeCN);
					}
					rec.setNumericAttrValue(0, "0", vehicleNumCN);
					added = true;
				}
			if (!added)
				return;
			Dimension d0 = mainP.getPreferredSize();
			mainP.setVisible(false);
			makeMainPanel();
			Dimension d1 = mainP.getPreferredSize();
			int dh = d1.height - d0.height;
			d0 = getSize();
			d1 = getToolkit().getScreenSize();
			if (d0.height + dh <= d1.height * 2 / 3) {
				setSize(d0.width, d0.height + dh);
			}
			mainP.invalidate();
			mainP.setVisible(true);
			validate();
			if (vehicleLocLayer != null) {
				SpatialEntity spe = new SpatialEntity(siteId, siteName);
				spe.setGeometry(point);
				DGeoObject gobj = new DGeoObject();
				gobj.setup(spe);
				gobj.setLabel(siteName);
				vehicleLocLayer.addGeoObject(gobj);
			}
			Checkbox lastCB = (Checkbox) siteCheckers.elementAt(siteCheckers.size() - 1);
			lastCB.setState(true);
			findSelectedSite();
			return;
		}
	}

	public void addSites() {
		Vector suitableLayers = null, locations = null;
		if (locLayer != null && locLayer.getObjectCount() > 0) {
			locations = new Vector(locLayer.getObjectCount(), 1);
			for (int i = 0; i < locLayer.getObjectCount(); i++) {
				DGeoObject gobj = locLayer.getObject(i);
				if (!StringUtil.isStringInVectorIgnoreCase(gobj.getIdentifier(), siteIdsOfCheckers)) {
					locations.addElement(gobj);
				}
			}
			if (locations.size() < 1) {
				locations = null;
			}
		}
		if (lman != null) {
			suitableLayers = new Vector(lman.getLayerCount(), 1);
			if (souLayer != null) {
				suitableLayers.addElement(souLayer);
			}
			if (destLayer != null) {
				suitableLayers.addElement(destLayer);
			}
			for (int i = 0; i < lman.getLayerCount(); i++) {
				DGeoLayer layer = lman.getLayer(i);
				if (!layer.equals(locLayer) && !layer.equals(vehicleLocLayer) && !layer.equals(souLayer) && !layer.equals(destLayer) && layer.getType() == Geometry.point && layer.getObjectCount() > 0) {
					suitableLayers.addElement(layer);
				}
			}
			if (suitableLayers.size() < 1) {
				suitableLayers = null;
			}
		}
		if (suitableLayers != null || locations != null) {
			SiteSelectionUI siteSelUI = new SiteSelectionUI(suitableLayers, locations, locLayer, lman, core.getSupervisor(), core.getUI().getCurrentMapViewer());
			okFrame = new OKFrame(this, res.getString("new_sites"), true);
			okFrame.addContent(siteSelUI);
			okFrame.start();
			return;
		}
		lman.activateLayer(vehicleLocLayer.getContainerIdentifier());
		startEnterPointOnMap();
	}

	protected void startEnterPointOnMap() {
		if (enterPointUI != null) {
			enterPointUI.destroy();
		}
		enterPointUI = new EnterPointOnMapUI();
		enterPointUI.setOwner(this);
		enterPointUI.setPromptText(res.getString("Enter_point_on_map"));
		enterPointUI.setWindowTitle(res.getString("Enter_point"));
		enterPointUI.run(core);
	}

	protected void finishSiteSelection(SiteSelectionUI siteSelUI) {
		if (siteSelUI == null)
			return;
		if (siteSelUI.wishClickMap()) {
			siteSelUI.destroy();
			lman.activateLayer(vehicleLocLayer.getContainerIdentifier());
			startEnterPointOnMap();
			return;
		}
		if (siteSelUI.getSuitableLayers() == null || siteSelUI.sitesSelectedFromLocLayer()) {
			addSitesFromList(siteSelUI.getLocList().getSelectedObjects(), siteSelUI.getLocLayer());
		} else {
			siteSelUI.destroy();
			if (siteSelUI.getSelectedLayer() != null) {
				DGeoLayer layer = siteSelUI.getSelectedLayer();
				siteSelUI = new SiteSelectionUI(layer, lman, core.getSupervisor(), core.getUI().getCurrentMapViewer());
				okFrame = new OKFrame(this, res.getString("Select_the_sites"), true);
				okFrame.addContent(siteSelUI);
				okFrame.start();
				return;
			}
		}
		siteSelUI.destroy();
		lman.activateLayer(vehicleLocLayer.getContainerIdentifier());
		markCurrentSite();
	}

	public void itemStateChanged(ItemEvent e) {
		findSelectedSite();
	}

	/**
	 * Determines the index of the currently selected site according
	 * to the states of the radio buttons
	 */
	protected void findCurrSiteIndex() {
		currSiteIdx = -1;
		if (siteCheckers == null || siteCheckers.size() < 1)
			return;
		for (int i = 0; i < siteCheckers.size(); i++)
			if (((Checkbox) siteCheckers.elementAt(i)).getState()) {
				currSiteIdx = i;
				break;
			}
	}

	protected void markCurrentSite() {
		erasePlaceMarker();
		if (currSiteIdx < 0)
			return;
		String id = (String) siteIdsOfCheckers.elementAt(currSiteIdx);
		GeoObject obj = vehicleLocLayer.findObjectById(id);
		if (obj != null && obj.getGeometry() != null) {
			placeMarker = new PlaceMarker(obj.getGeometry(), core.getUI().getCurrentMapViewer().getMapDrawer(), 2, 5);
		}
	}

	protected void erasePlaceMarker() {
		if (placeMarker != null) {
			placeMarker.destroy();
			placeMarker = null;
		}
	}

	protected void findSelectedSite() {
		int prevIdx = currSiteIdx;
		findCurrSiteIndex();
		if (currSiteIdx == prevIdx)
			return;
		markCurrentSite();
	}

	protected void addSitesFromList(Vector sites, DGeoLayer layer) {
		if (sites == null || sites.size() < 1)
			return;
		Vector types = vehicleTypesInfo.vehicleClassNames;
		if (types == null) {
			types = vehicleTypesInfo.vehicleClassIds;
		}
		if (types.size() < 1) {
			types = null;
		}
		boolean hasVehicleTypes = types != null && types.size() > 1;
		DataTable table = null;
		GridBagLayout gb = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 1.0f;
		c.weighty = 1.0f;
		c.fill = GridBagConstraints.HORIZONTAL;
		Panel p = new Panel(gb);
		Label l = new Label(res.getString("Site_ID") + ":", Label.RIGHT);
		c.gridwidth = 1;
		gb.setConstraints(l, c);
		p.add(l);
		TextField siteIdTF = new TextField(10);
		siteIdTF.setEditable(false);
		c.gridwidth = GridBagConstraints.REMAINDER;
		gb.setConstraints(siteIdTF, c);
		p.add(siteIdTF);
		l = new Label(res.getString("Site_name") + ":", Label.RIGHT);
		c.gridwidth = 1;
		gb.setConstraints(l, c);
		p.add(l);
		TextField siteNameTF = new TextField(20);
		siteNameTF.setEditable(false);
		c.gridwidth = GridBagConstraints.REMAINDER;
		gb.setConstraints(siteNameTF, c);
		p.add(siteNameTF);
		Checkbox cb[] = null;
		if (hasVehicleTypes) {
			l = new Label(res.getString("Types_of_vehicles") + ":", Label.CENTER);
			c.gridwidth = GridBagConstraints.REMAINDER;
			gb.setConstraints(l, c);
			p.add(l);
			cb = new Checkbox[types.size()];
			for (int i = 0; i < types.size(); i++) {
				cb[i] = new Checkbox((String) types.elementAt(i), false);
				c.gridwidth = GridBagConstraints.REMAINDER;
				gb.setConstraints(cb[i], c);
				p.add(cb[i]);
			}
		}
		boolean added = false;
		for (int i = 0; i < sites.size(); i++) {
			DGeoObject gobj = (DGeoObject) sites.elementAt(i);
			String siteId = gobj.getIdentifier();
			String siteName = gobj.getName();
			while (siteId != null && StringUtil.isStringInVectorIgnoreCase(siteId, siteIdsOfCheckers)) {
				String str = res.getString("The_site") + " " + siteId;
				if (siteName != null) {
					str += " \"" + siteName + "\"";
				}
				DGeoObject obj = (DGeoObject) vehicleLocLayer.findObjectById(siteId);
				str += "\n" + res.getString("has_same_identifier_as_site") + " \"" + obj.getName() + "\", " + res.getString("which_present_in_list") + ".\n" + res.getString("wish_modify_identifier_");
				TextCanvas tc = new TextCanvas();
				tc.setText(str);
				TextField tf = new TextField(siteId);
				Panel pp = new Panel(new BorderLayout());
				pp.add(tc, BorderLayout.CENTER);
				Panel p1 = new Panel(new BorderLayout());
				p1.add(new Label(res.getString("Identifier") + ":"), BorderLayout.WEST);
				p1.add(tf, BorderLayout.CENTER);
				pp.add(p1, BorderLayout.SOUTH);
				OKDialog dia = new OKDialog(this, res.getString("Duplicate_site_identifier"), true);
				dia.addContent(pp);
				dia.show();
				if (dia.wasCancelled()) {
					siteId = null;
				} else {
					siteId = tf.getText();
					if (siteId != null) {
						siteId = siteId.trim();
						if (siteId.length() < 1) {
							siteId = null;
						}
					}
				}
			}
			if (siteId == null) {
				continue;
			}
			siteIdTF.setText(siteId);
			siteNameTF.setText((siteName == null) ? "" : siteName);
			siteNameTF.setEditable(siteName == null);
			if (cb != null) {
				for (Checkbox element : cb) {
					element.setState(false);
				}
			}
			OKDialog okDia = new OKDialog(this, res.getString("add_site"), true);
			okDia.addContent(p);
			okDia.show();
			if (okDia.wasCancelled()) {
				continue;
			}
			putDataToTable(false);
			siteName = siteNameTF.getText();
			if (siteName != null) {
				siteName = siteName.trim();
				if (siteName.length() < 1) {
					siteName = null;
				}
			}
			boolean siteAdded = false;
			for (int j = 0; j < cb.length; j++)
				if (cb[j].getState()) {
					DataRecord rec = new DataRecord(siteId + (vehicleLocTable.getDataItemCount() + 1), siteName);
					vehicleLocTable.addDataRecord(rec);
					rec.setAttrValue(siteId, siteIdCN);
					rec.setAttrValue(siteName, siteNameCN);
					if (types != null) {
						rec.setAttrValue(vehicleTypesInfo.getVehicleClassId(j), vehicleTypeCN);
					}
					rec.setNumericAttrValue(0, "0", vehicleNumCN);
					added = true;
					siteAdded = true;
				}
			if (siteAdded && vehicleLocLayer != null) {
				SpatialEntity spe = new SpatialEntity(siteId, siteName);
				spe.setGeometry(gobj.getGeometry());
				DGeoObject gobj1 = new DGeoObject();
				gobj1.setup(spe);
				gobj1.setLabel(siteName);
				vehicleLocLayer.addGeoObject(gobj1);
			}
		}
		if (!added)
			return;
		Dimension d0 = mainP.getSize();
		mainP.setVisible(false);
		makeMainPanel();
		Dimension d1 = mainP.getPreferredSize();
		int dh = d1.height - d0.height, dw = d1.width - d0.width;
		if (dh < 0) {
			dh = 0;
		}
		if (dw < 0) {
			dw = 0;
		}
		Window win = CManager.getWindow(this);
		if (win != null) {
			CManager.enlargeWindow(win, dw, dh, 0, 50);
		} else {
			d0 = getSize();
			setSize(d0.width + dw, d0.height + dh);
		}
		mainP.invalidate();
		mainP.setVisible(true);
		validate();
		Checkbox lastCB = (Checkbox) siteCheckers.elementAt(siteCheckers.size() - 1);
		lastCB.setState(true);
		lman.activateLayer(vehicleLocLayer.getContainerIdentifier());
		findSelectedSite();
	}

	public void dispose() {
		super.dispose();
		destroy();
	}

	public void destroy() {
		if (destroyed)
			return;
		erasePlaceMarker();
		if (enterPointUI != null) {
			enterPointUI.destroy();
			enterPointUI = null;
		}
		if (okFrame != null) {
			okFrame.dispose();
			Component comp = okFrame.getMainComponent();
			if (comp instanceof Destroyable) {
				((Destroyable) comp).destroy();
			}
			okFrame = null;
		}
		if (layerWasAdded) {
			int layerIdx = lman.getIndexOfLayer(vehicleLocLayer.getContainerIdentifier());
			if (layerIdx >= 0) {
				lman.removeGeoLayer(layerIdx);
			}
			layerWasAdded = false;
		}
		destroyed = true;
	}

	public boolean isDestroyed() {
		return destroyed;
	}
}
