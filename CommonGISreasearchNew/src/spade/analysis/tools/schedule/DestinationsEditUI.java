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
import java.awt.Rectangle;
import java.awt.ScrollPane;
import java.awt.TextField;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
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
 * Date: 12-Nov-2007
 * Time: 16:35:59
 * A UI for entering and editing data about the destinations for items
 * sunject to evacuation
 */
public class DestinationsEditUI extends Frame implements ActionListener, ItemListener, Destroyable {
	static ResourceBundle res = Language.getTextResource("spade.analysis.tools.schedule.SchedulerTexts_tools_schedule");

	protected ESDACore core = null;
	/**
	 * Contains data about the available destinations and their capacities
	 */
	public DataTable destTable = null;
	/**
	 * Indices of relevant table columns in destCap
	 */
	protected int siteIdCN = -1, siteNameCN = -1, siteTypeCN = -1, itemCatCN = -1, itemCatCodeCN = -1, capacityCN = -1;
	/**
	 * The layer with the destinations
	 */
	public DGeoLayer destLayer = null;
	/**
	 * The geographical layer with the source locations (to show on the map
	 * as a remainder)
	 */
	public DGeoLayer souLayer = null;
	protected boolean souLayerWasAdded = false;
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
	 * For each category, contains the total number of items to evacuate.
	 */
	public int itemTotalNumbers[] = null;
	/**
	 * The layer with locations which may (or may not) contain source
	 * locations and/or destination locations and/or locations of vehicles
	 */
	public DGeoLayer locLayer = null;
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
	 * Contains text fields for the capacities of the sites.
	 * There may be several fields per site depending on the item categories
	 * present in a site.
	 */
	protected Vector capacityTextFields = null;
	/**
	 * Contains the identifiers of the sites corresponding to the text fields
	 */
	protected Vector siteIdsOfTextFields = null;
	/**
	 * Contains the numbers of the table records corresponding to the text fields
	 */
	protected IntArray recNums = null;
	/**
	 * These text fields show the total available capacities by item categories
	 */
	protected TextField totalCapTextFields[] = null;
	/**
	 * The index of the currently selected site
	 */
	protected int currSiteIdx = -1;

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
	 * @param destLayer - the geographical layer with the destination sites
	 * @param destTable - the table with data about the destinations to be edited
	 * @param itemCatNames - the names of the categories of the transported items
	 * @param itemCatCodes - the codes of the categories of the transported items
	 * @param itemTotalNumbers - for each category, contains the total number of items to evacuate
	 * @param locLayer - a layer with locations from which additional source
	 *                   locations may be taken
	 */
	public DestinationsEditUI(DGeoLayer destLayer, DataTable destTable, Vector itemCatNames, Vector itemCatCodes, int itemTotalNumbers[], DGeoLayer souLayer, DGeoLayer locLayer) {
		super(res.getString("Destinations_UI_title"));
		if (destTable == null)
			return;
		DataSourceSpec spec = (DataSourceSpec) destTable.getDataSource();
		if (spec == null || spec.extraInfo == null)
			return;
		String aName = (String) spec.extraInfo.get("ID_FIELD_NAME");
		if (aName == null) {
			aName = spec.idFieldName;
		}
		if (aName == null) {
			aName = "id";
		}
		siteIdCN = destTable.findAttrByName(aName);
		aName = (String) spec.extraInfo.get("NAME_FIELD_NAME");
		if (aName == null) {
			aName = spec.nameFieldName;
		}
		if (aName == null) {
			aName = "name";
		}
		siteNameCN = destTable.findAttrByName(aName);
		siteTypeCN = destTable.findAttrByName((String) spec.extraInfo.get("SITE_TYPE_FIELD_NAME"));
		itemCatCN = destTable.findAttrByName((String) spec.extraInfo.get("ITEM_CLASS_FIELD_NAME"));
		itemCatCodeCN = destTable.findAttrByName((String) spec.extraInfo.get("ITEM_CLASS_CODE_FIELD_NAME"));
		capacityCN = destTable.findAttrByName((String) spec.extraInfo.get("CAPACITY_FIELD_NAME"));
		if (siteIdCN < 0 || capacityCN < 0)
			return;

		this.destLayer = destLayer;
		this.destTable = destTable;
		this.souLayer = souLayer;
		this.itemCatNames = itemCatNames;
		this.itemCatCodes = itemCatCodes;
		this.itemTotalNumbers = itemTotalNumbers;
		this.locLayer = locLayer;

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
			l = new Label(res.getString("Available_capacity"));
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
			fp.open();
			p.add(fp);
			p.add(new Line(false));
		}
		p.add(new Label(res.getString("possible_destinations_and_capacities"), Label.CENTER));
		add(p, BorderLayout.NORTH);

		p = new Panel(new GridLayout(2, 1));
		Panel p1 = new Panel(new FlowLayout(FlowLayout.CENTER, 5, 1));
		p1.add(new Label(res.getString("curr_site_")));

		boolean hasItemCategories = (itemCatNames != null && itemCatNames.size() > 0) || (itemCatCodes != null && itemCatCodes.size() > 0);
		if (hasItemCategories) {
			Button b = new Button(res.getString("add_item_cat"));
			b.setActionCommand("add_item_cat");
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
		if (capacityTextFields.size() < 5) {
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

	private void makeMainPanel() {
		if (mainP == null) {
			siteCheckers = new Vector(50, 50);
			siteIdsOfCheckers = new Vector(50, 50);
			capacityTextFields = new Vector(100, 50);
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
			capacityTextFields.removeAllElements();
			siteIdsOfTextFields.removeAllElements();
			recNums.removeAllElements();
		}

		boolean hasItemCategories = (itemCatNames != null && itemCatNames.size() > 0) || (itemCatCodes != null && itemCatCodes.size() > 0);

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
		if (hasItemCategories) {
			l = new Label(res.getString("item_cat"));
			gbc.gridwidth = 4;
			gridbag.setConstraints(l, gbc);
			mainP.add(l);
		}
		l = new Label(res.getString("Capacity"));
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

		if (destTable.getDataItemCount() > 0) {
			for (int i = 0; i < destTable.getDataItemCount(); i++) {
				DataRecord rec = destTable.getDataRecord(i);
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
				int cap = 0;
				if (capAvail != null) {
					double val = rec.getNumericAttrValue(capacityCN);
					if (!Double.isNaN(val) && val > 0) {
						cap = (int) Math.round(val);
					}
				}
				if (hasItemCategories) {
					String cat = rec.getAttrValueAsString(itemCatCN);
					if (cat == null) {
						cat = getCatName(rec.getAttrValueAsString(itemCatCodeCN));
						if (cat == null) {
							cat = "";
						}
					}
					l = new Label(cat);
					gbc.gridwidth = 4;
					gridbag.setConstraints(l, gbc);
					mainP.add(l);
					if (cap > 0 && capAvail != null && cat.length() > 0) {
						int idx = StringUtil.indexOfStringInVectorIgnoreCase(cat, itemCatNames);
						if (idx < 0) {
							cat = rec.getAttrValueAsString(itemCatCodeCN);
							idx = StringUtil.indexOfStringInVectorIgnoreCase(cat, itemCatCodes);
						}
						if (idx >= 0) {
							capAvail[idx] += cap;
						}
					}
				} else if (capAvail != null) {
					capAvail[0] += cap;
				}
				TextField tf = new TextField(rec.getAttrValueAsString(capacityCN));
				if (hasTotalCapFields) {
					tf.addActionListener(this);
				}
				capacityTextFields.addElement(tf);
				gbc.gridwidth = GridBagConstraints.REMAINDER;
				gridbag.setConstraints(tf, gbc);
				mainP.add(tf);
				siteIdsOfTextFields.addElement(siteId);
				recNums.addElement(i);
				for (int j = i + 1; j < destTable.getDataItemCount(); j++) {
					rec = destTable.getDataRecord(j);
					if (siteId.equalsIgnoreCase(rec.getAttrValueAsString(siteIdCN))) {
						l = new Label("");
						gbc.gridwidth = 5;
						gridbag.setConstraints(l, gbc);
						mainP.add(l);
						cap = 0;
						if (capAvail != null) {
							double val = rec.getNumericAttrValue(capacityCN);
							if (!Double.isNaN(val) && val > 0) {
								cap = (int) Math.round(val);
							}
						}
						if (hasItemCategories) {
							String cat = rec.getAttrValueAsString(itemCatCN);
							if (cat == null) {
								cat = getCatName(rec.getAttrValueAsString(itemCatCodeCN));
							}
							if (cat == null) {
								cat = "";
							}
							l = new Label(cat);
							gbc.gridwidth = 4;
							gridbag.setConstraints(l, gbc);
							mainP.add(l);
							if (cap > 0 && capAvail != null && cat.length() > 0) {
								int idx = StringUtil.indexOfStringInVectorIgnoreCase(cat, itemCatNames);
								if (idx < 0) {
									cat = rec.getAttrValueAsString(itemCatCodeCN);
									idx = StringUtil.indexOfStringInVectorIgnoreCase(cat, itemCatCodes);
								}
								if (idx >= 0) {
									capAvail[idx] += cap;
								}
							}
						} else if (capAvail != null) {
							capAvail[0] += cap;
						}
						tf = new TextField(rec.getAttrValueAsString(capacityCN));
						if (hasTotalCapFields) {
							tf.addActionListener(this);
						}
						capacityTextFields.addElement(tf);
						gbc.gridwidth = GridBagConstraints.REMAINDER;
						gridbag.setConstraints(tf, gbc);
						mainP.add(tf);
						siteIdsOfTextFields.addElement(siteId);
						recNums.addElement(j);
					}
				}
			}
		}
		if (capAvail != null) {
			for (int i = 0; i < capAvail.length; i++) {
				totalCapTextFields[i].setText(String.valueOf(capAvail[i]));
				boolean notEnough = capAvail[i] < itemTotalNumbers[i];
				totalCapTextFields[i].setForeground((notEnough) ? Color.red.darker() : getForeground());
				totalCapTextFields[i].setBackground((notEnough) ? Color.pink : getBackground());
			}
		}
	}

	public void setCore(ESDACore core) {
		this.core = core;
		if (core != null) {
			lman = (DLayerManager) core.getUI().getCurrentMapViewer().getLayerManager();
			if (lman != null) {
				if (souLayer != null) {
					int souLayerIdx = lman.getIndexOfLayer(souLayer.getContainerIdentifier());
					if (souLayerIdx < 0) {
						lman.addGeoLayer(souLayer);
						souLayerWasAdded = true;
					}
				}
				if (destLayer != null) {
					int layerIdx = lman.getIndexOfLayer(destLayer.getContainerIdentifier());
					if (layerIdx < 0) {
						lman.addGeoLayer(destLayer);
						layerIdx = lman.getLayerCount() - 1;
						layerWasAdded = true;
					}
					lman.activateLayer(layerIdx);
				}
			}
		}
	}

	/**
	 * The owner must be notified when the editing is finished
	 */
	public void setOwner(ActionListener owner) {
		this.owner = owner;
	}

	/**
	 * Returns the name of the item category for the given code
	 */
	protected String getCatName(String catCode) {
		if (catCode == null || itemCatCodes == null)
			return catCode;
		if (itemCatNames == null || itemCatNames.size() < 1)
			return catCode;
		int idx = StringUtil.indexOfStringInVectorIgnoreCase(catCode, itemCatCodes);
		if (idx < 0 || idx >= itemCatNames.size())
			return catCode;
		return (String) itemCatNames.elementAt(idx);
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

	/**
	 * Writes the data entered or edited by the user to the table
	 */
	protected void putDataToTable(boolean removeEmpty) {
		if (destTable == null)
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
		boolean hasItemCategories = (itemCatNames != null && itemCatNames.size() > 0) || (itemCatCodes != null && itemCatCodes.size() > 0);
		for (int i = 0; i < capacityTextFields.size(); i++) {
			TextField tf = (TextField) capacityTextFields.elementAt(i);
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
			DataRecord rec = destTable.getDataRecord(recNums.elementAt(i));
			String siteId = rec.getAttrValueAsString(siteIdCN);
			if (!StringUtil.isStringInVectorIgnoreCase(siteId, objIdsHaveItems)) {
				objIdsHaveItems.addElement(siteId);
			}
			rec.setNumericAttrValue(num, String.valueOf(num), capacityCN);
			if (num > 0 && capAvail != null)
				if (hasItemCategories) {
					String cat = rec.getAttrValueAsString(itemCatCodeCN);
					int idx = StringUtil.indexOfStringInVectorIgnoreCase(cat, itemCatCodes);
					if (idx < 0) {
						cat = rec.getAttrValueAsString(itemCatCN);
						idx = StringUtil.indexOfStringInVectorIgnoreCase(cat, itemCatNames);
					}
					if (idx >= 0) {
						capAvail[idx] += num;
					}
				} else {
					capAvail[0] += num;
				}
		}
		if (capAvail != null) {
			for (int i = 0; i < capAvail.length; i++) {
				totalCapTextFields[i].setText(String.valueOf(capAvail[i]));
				boolean notEnough = capAvail[i] < itemTotalNumbers[i];
				totalCapTextFields[i].setForeground((notEnough) ? Color.red.darker() : getForeground());
				totalCapTextFields[i].setBackground((notEnough) ? Color.pink : getBackground());
			}
		}
		if (removeEmpty && recNumsToRemove.size() > 0) {
			for (int i = recNumsToRemove.size() - 1; i >= 0; i--) {
				destTable.removeDataItem(recNumsToRemove.elementAt(i));
			}
		}
		if (removeEmpty && destLayer != null) {
			for (int i = destLayer.getObjectCount() - 1; i >= 0; i--)
				if (!StringUtil.isStringInVectorIgnoreCase(destLayer.getObjectId(i), objIdsHaveItems)) {
					destLayer.removeGeoObject(i);
				}
		}
	}

	protected OKFrame infoWin = null;

	/**
	 * Returns true if there are enough capacities.
	 */
	protected boolean compareCapacities(int totalCap[]) {
		if (infoWin != null) {
			infoWin.dispose();
			infoWin = null;
		}
		if (itemTotalNumbers == null || itemTotalNumbers.length < 1)
			return true;
		if (itemTotalNumbers.length == 1) {
			int deficit = itemTotalNumbers[0] - totalCap[0];
			if (deficit <= 0)
				return true;
			notLine.showMessage(res.getString("Insufficient_capacity") + ": " + itemTotalNumbers[0] + " " + res.getString("needed") + "; " + totalCap[0] + " " + res.getString("available") + "; " + res.getString("deficit") + " = " + deficit, true);
			return false;
		}
		IntArray deficient = new IntArray(itemTotalNumbers.length, 1);
		for (int i = 0; i < itemTotalNumbers.length; i++)
			if (itemTotalNumbers[i] > 0 && (i >= totalCap.length || totalCap[i] < itemTotalNumbers[i])) {
				deficient.addElement(i);
			}
		if (deficient.size() < 1)
			return true;
		if (deficient.size() == 1) {
			int idx = deficient.elementAt(0);
			String cat = (itemCatNames != null) ? (String) itemCatNames.elementAt(idx) : (itemCatCodes != null) ? (String) itemCatCodes.elementAt(idx) : null;
			String str = res.getString("Insufficient_capacity") + " ";
			if (cat != null) {
				str += "for " + cat;
			}
			int deficit = itemTotalNumbers[idx] - totalCap[idx];
			str += ": " + itemTotalNumbers[idx] + " " + res.getString("needed") + "; " + totalCap[idx] + " " + res.getString("available") + "; " + res.getString("deficit") + " = " + deficit;
			notLine.showMessage(str, true);
		} else {
			GridBagLayout gb = new GridBagLayout();
			GridBagConstraints c = new GridBagConstraints();
			c.weightx = 1.0f;
			c.weighty = 1.0f;
			c.fill = GridBagConstraints.HORIZONTAL;
			Panel p = new Panel(gb);
			Label l = new Label(res.getString("item_cat"), Label.RIGHT);
			c.gridwidth = 3;
			gb.setConstraints(l, c);
			p.add(l);
			l = new Label(res.getString("needed"), Label.RIGHT);
			c.gridwidth = 1;
			gb.setConstraints(l, c);
			p.add(l);
			l = new Label(res.getString("available"), Label.RIGHT);
			c.gridwidth = 1;
			gb.setConstraints(l, c);
			p.add(l);
			l = new Label(res.getString("deficit"), Label.RIGHT);
			c.gridwidth = GridBagConstraints.REMAINDER;
			gb.setConstraints(l, c);
			p.add(l);
			for (int i = 0; i < deficient.size(); i++) {
				int idx = deficient.elementAt(i);
				String cat = (itemCatNames != null) ? (String) itemCatNames.elementAt(idx) : (itemCatCodes != null) ? (String) itemCatCodes.elementAt(idx) : "";
				int deficit = itemTotalNumbers[idx] - totalCap[idx];
				l = new Label(cat, Label.RIGHT);
				c.gridwidth = 3;
				gb.setConstraints(l, c);
				p.add(l);
				l = new Label(String.valueOf(itemTotalNumbers[idx]), Label.RIGHT);
				c.gridwidth = 1;
				gb.setConstraints(l, c);
				p.add(l);
				l = new Label(String.valueOf(totalCap[idx]), Label.RIGHT);
				c.gridwidth = 1;
				gb.setConstraints(l, c);
				p.add(l);
				l = new Label(String.valueOf(deficit), Label.RIGHT);
				c.gridwidth = GridBagConstraints.REMAINDER;
				gb.setConstraints(l, c);
				p.add(l);
			}
			infoWin = new OKFrame(this, res.getString("Insufficient_capacities"), false);
			Panel pp = new Panel(new BorderLayout());
			pp.add(new Label(res.getString("Insufficient_capacities_in_destinations")), BorderLayout.NORTH);
			pp.add(p, BorderLayout.CENTER);
			infoWin.addContent(p);
			infoWin.pack();
			Rectangle b = getBounds();
			Dimension d = infoWin.getSize();
			infoWin.setLocation(b.x + b.width / 2 - d.width / 2, b.y + b.height / 2 - d.height / 2);
			infoWin.setVisible(true);
		}
		return false;
	}

	public void actionPerformed(ActionEvent e) {
		notLine.showMessage(null, false);
		if ((e.getSource() instanceof TextField) && okFrame == null) {
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
		if (e.getSource().equals(infoWin) && cmd.equals("closed")) {
			infoWin = null;
			return;
		}
		if (cmd.equals("done")) {
			//check if all capacities have been correctly specified
			int totalCap[] = null;
			int nCat = (itemCatCodes != null) ? itemCatCodes.size() : (itemCatNames != null) ? itemCatNames.size() : 1;
			if (itemTotalNumbers != null && itemTotalNumbers.length > 0) {
				totalCap = new int[nCat];
				for (int i = 0; i < nCat; i++) {
					totalCap[i] = 0;
				}
			}
			for (int i = 0; i < capacityTextFields.size(); i++) {
				TextField tf = (TextField) capacityTextFields.elementAt(i);
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
					str = res.getString("Illegal_capacity_for_site") + " " + siteIdsOfTextFields.elementAt(i);
					if (siteNameCN >= 0) {
						str += " \"" + destTable.getAttrValueAsString(siteNameCN, recNums.elementAt(i)) + "\"";
					}
					str += ": " + errMsg;
					notLine.showMessage(str, true);
					return;
				} else if (totalCap != null)
					if (nCat == 1) {
						totalCap[0] += num;
					} else {
						//determine the item category
						DataRecord rec = destTable.getDataRecord(recNums.elementAt(i));
						int catIdx = -1;
						String cat = rec.getAttrValueAsString(itemCatCodeCN);
						if (cat == null) {
							cat = rec.getAttrValueAsString(itemCatCN);
						}
						if (cat != null) {
							catIdx = StringUtil.indexOfStringInVectorIgnoreCase(cat, itemCatCodes);
							if (catIdx < 0) {
								catIdx = StringUtil.indexOfStringInVectorIgnoreCase(cat, itemCatNames);
							}
						}
						if (catIdx >= 0) {
							totalCap[catIdx] += num;
						}
					}
			}
			if (!compareCapacities(totalCap))
				return;
			dispose();
			putDataToTable(true);
			if (owner != null) {
				owner.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "destinations"));
			}
			return;
		}
		if (cmd.equals("add_item_cat")) {
			if (currSiteIdx < 0)
				return;
			String siteId = (String) siteIdsOfCheckers.elementAt(currSiteIdx);
			String siteName = null, siteType = null;
			IntArray exCatIdxs = new IntArray(10, 10);
			int firstIdx = -1, lastIdx = -1;
			for (int i = 0; i < siteIdsOfTextFields.size(); i++)
				if (siteId.equals(siteIdsOfTextFields.elementAt(i))) {
					if (firstIdx < 0) {
						firstIdx = i;
					}
					lastIdx = i;
					DataRecord rec = destTable.getDataRecord(recNums.elementAt(i));
					if (siteName == null) {
						siteName = rec.getAttrValueAsString(siteNameCN);
					}
					if (siteType == null) {
						siteType = rec.getAttrValueAsString(siteTypeCN);
					}
					int catIdx = -1;
					if (itemCatCodeCN >= 0 && itemCatCodes != null && itemCatCodes.size() > 0) {
						String cat = rec.getAttrValueAsString(itemCatCodeCN);
						catIdx = StringUtil.indexOfStringInVectorIgnoreCase(cat, itemCatCodes);
					}
					if (catIdx < 0) {
						String cat = rec.getAttrValueAsString(itemCatCN);
						catIdx = StringUtil.indexOfStringInVectorIgnoreCase(cat, itemCatNames);
					}
					if (catIdx >= 0) {
						exCatIdxs.addElement(catIdx);
					}
				} else if (lastIdx >= 0) {
					break;
				}
			if (exCatIdxs.size() >= itemCatNames.size()) {
				Dialogs.showMessage(this, res.getString("there_are_no_more_categories"), res.getString("no_more_categories"));
				return;
			}
			int catIdxs[] = new int[itemCatNames.size() - exCatIdxs.size()];
			Panel p = new Panel(new GridLayout(catIdxs.length, 1));
			int k = 0;
			Checkbox cb[] = new Checkbox[catIdxs.length];
			for (int i = 0; i < itemCatNames.size(); i++)
				if (exCatIdxs.indexOf(i) < 0) {
					cb[k] = new Checkbox((String) itemCatNames.elementAt(i), false);
					p.add(cb[k]);
					catIdxs[k++] = i;
				}
			OKDialog okDia = new OKDialog(this, res.getString("Select_categories_to_add"), true);
			okDia.addContent(p);
			okDia.show();
			if (okDia.wasCancelled())
				return;
			putDataToTable(false);
			boolean added = false;
			for (int i = 0; i < cb.length; i++)
				if (cb[i].getState()) {
					DataRecord rec = new DataRecord(siteId + (destTable.getDataItemCount() + 1), siteName);
					destTable.addDataRecord(rec);
					rec.setAttrValue(siteId, siteIdCN);
					rec.setAttrValue(siteName, siteNameCN);
					rec.setAttrValue(siteType, siteTypeCN);
					int catIdx = catIdxs[i];
					if (itemCatCodes != null) {
						rec.setAttrValue(itemCatCodes.elementAt(catIdx), itemCatCodeCN);
					}
					if (itemCatCN >= 0) {
						rec.setAttrValue(itemCatNames.elementAt(catIdx), itemCatCN);
					}
					rec.setNumericAttrValue(0, "0", capacityCN);
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
					DataRecord rec = destTable.getDataRecord(recNums.elementAt(i));
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
			if (destLayer != null) {
				int layerIdx = lman.getIndexOfLayer(destLayer.getContainerIdentifier());
				if (layerIdx >= 0) {
					lman.activateLayer(layerIdx);
					DGeoLayer layer = (DGeoLayer) lman.getGeoLayer(layerIdx);
					Graphics g = core.getUI().getCurrentMapViewer().getMapDrawer().getGraphics();
					MapContext mc = core.getUI().getCurrentMapViewer().getMapDrawer().getMapContext();
					layer.highlightObject(siteId, false, g, mc);
				}
				int idx = destLayer.getObjectIndex(siteId);
				if (idx >= 0) {
					destLayer.removeGeoObject(idx);
				}
			}
			currSiteIdx = -1;
			for (int i = lastIdx; i >= firstIdx; i--) {
				int recN = recNums.elementAt(i);
				destTable.removeDataItem(recN);
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
			TextField siteTypeTF = null;
			if (siteTypeCN >= 0) {
				l = new Label(res.getString("Site_type") + ":", Label.RIGHT);
				c.gridwidth = 1;
				gb.setConstraints(l, c);
				p.add(l);
				siteTypeTF = new TextField(10);
				c.gridwidth = GridBagConstraints.REMAINDER;
				gb.setConstraints(siteTypeTF, c);
				p.add(siteTypeTF);
			}
			Checkbox cb[] = null;
			if (itemCatNames != null) {
				l = new Label(res.getString("Item_categories") + ":", Label.CENTER);
				c.gridwidth = GridBagConstraints.REMAINDER;
				gb.setConstraints(l, c);
				p.add(l);
				cb = new Checkbox[itemCatNames.size()];
				for (int i = 0; i < itemCatNames.size(); i++) {
					cb[i] = new Checkbox((String) itemCatNames.elementAt(i), false);
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
					siteType = (siteTypeTF != null) ? siteTypeTF.getText() : null;
					if (siteType != null) {
						siteType = siteType.trim();
						if (siteType.length() < 1) {
							siteType = null;
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
					DataRecord rec = new DataRecord(siteId + (destTable.getDataItemCount() + 1), siteName);
					destTable.addDataRecord(rec);
					rec.setAttrValue(siteId, siteIdCN);
					rec.setAttrValue(siteName, siteNameCN);
					rec.setAttrValue(siteType, siteTypeCN);
					if (itemCatCodes != null) {
						rec.setAttrValue(itemCatCodes.elementAt(i), itemCatCodeCN);
					}
					if (itemCatCN >= 0) {
						rec.setAttrValue(itemCatNames.elementAt(i), itemCatCN);
					}
					rec.setNumericAttrValue(0, "0", capacityCN);
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
			if (destLayer != null) {
				SpatialEntity spe = new SpatialEntity(siteId, siteName);
				spe.setGeometry(point);
				DGeoObject gobj = new DGeoObject();
				gobj.setup(spe);
				gobj.setLabel(siteName);
				destLayer.addGeoObject(gobj);
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
			for (int i = 0; i < lman.getLayerCount(); i++) {
				DGeoLayer layer = lman.getLayer(i);
				if (!layer.equals(locLayer) && !layer.equals(souLayer) && !layer.equals(destLayer) && layer.getType() == Geometry.point && layer.getObjectCount() > 0) {
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
		lman.activateLayer(destLayer.getContainerIdentifier());
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
			lman.activateLayer(destLayer.getContainerIdentifier());
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
		lman.activateLayer(destLayer.getContainerIdentifier());
		markCurrentSite();
	}

	protected void addSitesFromList(Vector sites, DGeoLayer layer) {
		if (sites == null || sites.size() < 1)
			return;
		DataTable table = null;
		int typeColN = -1;
		if (siteTypeCN >= 0 && layer.getThematicData() != null && (layer.getThematicData() instanceof DataTable)) {
			table = (DataTable) layer.getThematicData();
			typeColN = table.getAttrIndex("type");
			if (typeColN < 0) {
				typeColN = table.findAttrByName("type");
			}
		}
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
		TextField siteTypeTF = null;
		if (siteTypeCN >= 0) {
			l = new Label(res.getString("Site_type") + ":", Label.RIGHT);
			c.gridwidth = 1;
			gb.setConstraints(l, c);
			p.add(l);
			siteTypeTF = new TextField(10);
			siteTypeTF.setEditable(typeColN < 0);
			c.gridwidth = GridBagConstraints.REMAINDER;
			gb.setConstraints(siteTypeTF, c);
			p.add(siteTypeTF);
		}
		Checkbox cb[] = null;
		if (itemCatNames != null) {
			l = new Label(res.getString("Item_categories") + ":", Label.CENTER);
			c.gridwidth = GridBagConstraints.REMAINDER;
			gb.setConstraints(l, c);
			p.add(l);
			cb = new Checkbox[itemCatNames.size()];
			for (int i = 0; i < itemCatNames.size(); i++) {
				cb[i] = new Checkbox((String) itemCatNames.elementAt(i), false);
				c.gridwidth = GridBagConstraints.REMAINDER;
				gb.setConstraints(cb[i], c);
				p.add(cb[i]);
			}
		}
		boolean added = false;
		putDataToTable(false);
		for (int i = 0; i < sites.size(); i++) {
			DGeoObject gobj = (DGeoObject) sites.elementAt(i);
			String siteId = gobj.getIdentifier();
			String siteName = gobj.getName();
			while (siteId != null && StringUtil.isStringInVectorIgnoreCase(siteId, siteIdsOfCheckers)) {
				String str = res.getString("The_site") + " " + siteId;
				if (siteName != null) {
					str += " \"" + siteName + "\"";
				}
				DGeoObject obj = (DGeoObject) destLayer.findObjectById(siteId);
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
			if (siteTypeTF != null) {
				String type = null;
				if (typeColN >= 0 && gobj.getData() != null) {
					type = gobj.getData().getAttrValueAsString(typeColN);
				}
				siteTypeTF.setText((type == null) ? "" : type);
			}
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
			siteName = siteNameTF.getText();
			if (siteName != null) {
				siteName = siteName.trim();
				if (siteName.length() < 1) {
					siteName = null;
				}
			}
			String siteType = (siteTypeTF != null) ? siteTypeTF.getText() : null;
			if (siteType != null) {
				siteType = siteType.trim();
				if (siteType.length() < 1) {
					siteType = null;
				}
			}
			if (siteType == null) {
				siteType = "unknown";
			}
			boolean siteAdded = false;
			for (int j = 0; j < cb.length; j++)
				if (cb[j].getState()) {
					DataRecord rec = new DataRecord(siteId + (destTable.getDataItemCount() + 1), siteName);
					destTable.addDataRecord(rec);
					rec.setAttrValue(siteId, siteIdCN);
					rec.setAttrValue(siteName, siteNameCN);
					rec.setAttrValue(siteType, siteTypeCN);
					if (itemCatCodes != null) {
						rec.setAttrValue(itemCatCodes.elementAt(j), itemCatCodeCN);
					}
					if (itemCatCN >= 0) {
						rec.setAttrValue(itemCatNames.elementAt(j), itemCatCN);
					}
					rec.setNumericAttrValue(0, "0", capacityCN);
					added = true;
					siteAdded = true;
				}
			if (siteAdded && destLayer != null) {
				SpatialEntity spe = new SpatialEntity(siteId, siteName);
				spe.setGeometry(gobj.getGeometry());
				DGeoObject gobj1 = new DGeoObject();
				gobj1.setup(spe);
				gobj1.setLabel(siteName);
				destLayer.addGeoObject(gobj1);
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
		if (destLayer != null && lman != null) {
			lman.activateLayer(destLayer.getContainerIdentifier());
			findSelectedSite();
		}
	}

	public void itemStateChanged(ItemEvent e) {
		findSelectedSite();
	}

	protected void markCurrentSite() {
		erasePlaceMarker();
		if (currSiteIdx < 0)
			return;
		String id = (String) siteIdsOfCheckers.elementAt(currSiteIdx);
		GeoObject obj = destLayer.findObjectById(id);
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

	public void dispose() {
		super.dispose();
		destroy();
	}

	public void destroy() {
		if (destroyed)
			return;
		erasePlaceMarker();
		if (infoWin != null) {
			infoWin.dispose();
			infoWin = null;
		}
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
			int layerIdx = lman.getIndexOfLayer(destLayer.getContainerIdentifier());
			lman.removeGeoLayer(layerIdx);
			layerWasAdded = false;
		}
		if (souLayerWasAdded) {
			int souLayerIdx = lman.getIndexOfLayer(souLayer.getContainerIdentifier());
			if (souLayerIdx >= 0) {
				lman.removeGeoLayer(souLayerIdx);
			}
			souLayerWasAdded = false;
		}
		destroyed = true;
	}

	public boolean isDestroyed() {
		return destroyed;
	}
}
