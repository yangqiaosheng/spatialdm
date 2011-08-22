package spade.analysis.tools.schedule;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
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
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.ESDACore;
import spade.analysis.tools.EnterPointOnMapUI;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.Dialogs;
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
 * Date: 02-Nov-2007
 * Time: 10:02:58
 * A UI for entering and editing data about the numbers of items
 * subject to evacuation (transportation) in the source locations.
 */
public class ItemsInSourcesEditUI extends Frame implements ActionListener, ItemListener, Destroyable {
	static ResourceBundle res = Language.getTextResource("spade.analysis.tools.schedule.SchedulerTexts_tools_schedule");

	protected ESDACore core = null;
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
	 * Indices of relevant table columns
	 */
	protected int siteIdCN = -1, siteNameCN = -1, siteTypeCN = -1, itemCatCN = -1, itemCatCodeCN = -1, itemNumCN = -1, timeLimitCN = -1;
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
	 * The layer with locations which may (or may not) contain source
	 * locations and/or destination locations and/or locations of vehicles
	 */
	public DGeoLayer locLayer = null;
	/**
	 * The first in rank layer from which to take item sources
	 * (e.g. it may come from IDAS)
	 */
	public DGeoLayer addLayer1 = null;
	/**
	 * The second in rank layer from which to take item sources
	 * (e.g. it may come from IDAS)
	 */
	public DGeoLayer addLayer2 = null;
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
	protected TextField commonTimeTF = null;
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
	 * @param souLayer - the geographical layer with the source location
	 * @param souTable - the table with data about the sources to be edited
	 * @param itemCatNames - the names of the categories of the transported items
	 * @param itemCatCodes - the codes of the categories of the transported items
	 * @param locLayer - a layer with locations from which additional source
	 *                   locations may be taken
	 * @param addLayer1 - the first in rank layer from which to take item sources
	 * @param addLayer2 - the second in rank layer from which to take item sources
	 */
	public ItemsInSourcesEditUI(DGeoLayer souLayer, DataTable souTable, Vector itemCatNames, Vector itemCatCodes, DGeoLayer locLayer, DGeoLayer addLayer1, DGeoLayer addLayer2) {
		super(res.getString("source_UI_title"));
		if (souTable == null)
			return;
		DataSourceSpec spec = (DataSourceSpec) souTable.getDataSource();
		if (spec == null || spec.extraInfo == null)
			return;
		siteIdCN = souTable.findAttrByName((String) spec.extraInfo.get("SOURCE_ID_FIELD_NAME"));
		siteNameCN = souTable.findAttrByName((String) spec.extraInfo.get("SOURCE_NAME_FIELD_NAME"));
		siteTypeCN = souTable.findAttrByName((String) spec.extraInfo.get("SITE_TYPE_FIELD_NAME"));
		itemCatCN = souTable.findAttrByName((String) spec.extraInfo.get("ITEM_CLASS_FIELD_NAME"));
		itemCatCodeCN = souTable.findAttrByName((String) spec.extraInfo.get("ITEM_CLASS_CODE_FIELD_NAME"));
		itemNumCN = souTable.findAttrByName((String) spec.extraInfo.get("ITEM_NUMBER_FIELD_NAME"));
		timeLimitCN = souTable.findAttrByName((String) spec.extraInfo.get("TIME_LIMIT_FIELD_NAME"));
		if (siteIdCN < 0 || itemNumCN < 0)
			return;

		this.souLayer = souLayer;
		this.souTable = souTable;
		this.itemCatNames = itemCatNames;
		this.itemCatCodes = itemCatCodes;
		this.locLayer = locLayer;
		this.addLayer1 = addLayer1;
		this.addLayer2 = addLayer2;

		setLayout(new BorderLayout());
		Panel p = new Panel(new GridLayout(2, 1));
		notLine = new NotificationLine(null);
		p.add(notLine);
		p.add(new Label(res.getString("Items_in_sources_and_time_limits"), Label.CENTER));
		add(p, BorderLayout.NORTH);
		p = new Panel(new GridLayout(3, 1));
		Panel p1 = new Panel(new FlowLayout(FlowLayout.CENTER, 1, 1));
		p1.add(new Label(res.getString("Common_time_limit") + ":"));
		commonTimeTF = new TextField(10);
		commonTimeTF.addActionListener(this);
		p1.add(commonTimeTF);
		p.add(p1);
		p1 = new Panel(new FlowLayout(FlowLayout.CENTER, 5, 1));
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

	private void makeMainPanel() {
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
		l = new Label(res.getString("Number"));
		gbc.gridwidth = 2;
		gridbag.setConstraints(l, gbc);
		mainP.add(l);
		l = new Label(res.getString("Time"));
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(l, gbc);
		mainP.add(l);

		if (souTable.getDataItemCount() > 0) {
			for (int i = 0; i < souTable.getDataItemCount(); i++) {
				DataRecord rec = souTable.getDataRecord(i);
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
				if (hasItemCategories) {
					String cat = rec.getAttrValueAsString(itemCatCN);
					if (cat == null) {
						cat = rec.getAttrValueAsString(itemCatCodeCN);
					}
					if (cat == null) {
						cat = "";
					}
					l = new Label(cat);
					gbc.gridwidth = 4;
					gridbag.setConstraints(l, gbc);
					mainP.add(l);
				}
				TextField tf = new TextField(rec.getAttrValueAsString(itemNumCN));
				itemNumTextFields.addElement(tf);
				gbc.gridwidth = 2;
				gridbag.setConstraints(tf, gbc);
				mainP.add(tf);
				tf = new TextField(rec.getAttrValueAsString(timeLimitCN));
				maxTimeTextFields.addElement(tf);
				gbc.gridwidth = GridBagConstraints.REMAINDER;
				gridbag.setConstraints(tf, gbc);
				mainP.add(tf);
				siteIdsOfTextFields.addElement(siteId);
				recNums.addElement(i);
				for (int j = i + 1; j < souTable.getDataItemCount(); j++) {
					rec = souTable.getDataRecord(j);
					if (siteId.equalsIgnoreCase(rec.getAttrValueAsString(siteIdCN))) {
						l = new Label("");
						gbc.gridwidth = 5;
						gridbag.setConstraints(l, gbc);
						mainP.add(l);
						if (hasItemCategories) {
							String cat = rec.getAttrValueAsString(itemCatCN);
							if (cat == null) {
								cat = rec.getAttrValueAsString(itemCatCodeCN);
							}
							if (cat == null) {
								cat = "";
							}
							l = new Label(cat);
							gbc.gridwidth = 4;
							gridbag.setConstraints(l, gbc);
							mainP.add(l);
						}
						tf = new TextField(rec.getAttrValueAsString(itemNumCN));
						itemNumTextFields.addElement(tf);
						gbc.gridwidth = 2;
						gridbag.setConstraints(tf, gbc);
						mainP.add(tf);
						tf = new TextField(rec.getAttrValueAsString(timeLimitCN));
						maxTimeTextFields.addElement(tf);
						gbc.gridwidth = GridBagConstraints.REMAINDER;
						gridbag.setConstraints(tf, gbc);
						mainP.add(tf);
						siteIdsOfTextFields.addElement(siteId);
						recNums.addElement(j);
					}
				}
			}
		}

	}

	public void setCore(ESDACore core) {
		this.core = core;
		if (core != null) {
			lman = (DLayerManager) core.getUI().getCurrentMapViewer().getLayerManager();
			if (lman != null && souLayer != null) {
				int layerIdx = lman.getIndexOfLayer(souLayer.getContainerIdentifier());
				if (layerIdx < 0) {
					lman.addGeoLayer(souLayer);
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
		if (souTable == null)
			return;
		IntArray recNumsToRemove = (removeEmpty) ? new IntArray(recNums.size(), 1) : null;
		Vector objIdsHaveItems = new Vector(siteIdsOfCheckers.size(), 1);
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
			if (removeEmpty && time < 1) {
				recNumsToRemove.addElement(recNums.elementAt(i));
				continue;
			}
			DataRecord rec = souTable.getDataRecord(recNums.elementAt(i));
			String siteId = rec.getAttrValueAsString(siteIdCN);
			if (!StringUtil.isStringInVectorIgnoreCase(siteId, objIdsHaveItems)) {
				objIdsHaveItems.addElement(siteId);
			}
			rec.setNumericAttrValue(num, String.valueOf(num), itemNumCN);
			rec.setNumericAttrValue(time, String.valueOf(time), timeLimitCN);
		}
		if (removeEmpty && recNumsToRemove.size() > 0) {
			for (int i = recNumsToRemove.size() - 1; i >= 0; i--) {
				souTable.removeDataItem(recNumsToRemove.elementAt(i));
			}
		}
		if (removeEmpty && souLayer != null) {
			for (int i = souLayer.getObjectCount() - 1; i >= 0; i--)
				if (!StringUtil.isStringInVectorIgnoreCase(souLayer.getObjectId(i), objIdsHaveItems)) {
					souLayer.removeGeoObject(i);
				}
		}
	}

	public void actionPerformed(ActionEvent e) {
		notLine.showMessage(null, false);
		String cmd = e.getActionCommand();
		if (cmd == null && !e.getSource().equals(commonTimeTF))
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
		if (e.getSource().equals(commonTimeTF)) {
			String str = commonTimeTF.getText();
			if (str == null)
				return;
			str = str.trim();
			if (str.length() < 1)
				return;
			int num = 0;
			try {
				num = Integer.parseInt(str);
			} catch (NumberFormatException nfe) {
				notLine.showMessage(res.getString("illegal_string") + ": [" + str + "]", true);
				return;
			}
			if (num < 1) {
				notLine.showMessage(res.getString("illegal_number") + " (" + num + "); " + res.getString("must_be_positive"), true);
				return;
			}
			for (int i = 0; i < maxTimeTextFields.size(); i++) {
				TextField tf = (TextField) maxTimeTextFields.elementAt(i);
				str = tf.getText();
				if (str == null) {
					tf.setText(String.valueOf(num));
				} else {
					str = str.trim();
					if (str.length() < 1 || str.equals("0")) {
						tf.setText(String.valueOf(num));
					}
				}
			}
			return;
		}
		if (cmd.equals("done")) {
			int nItemsTotal = 0;
			//check if all time limits have been specified
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
				String infoType = res.getString("number_of_objects"), errMsg = null;
				try {
					num = Integer.parseInt(str);
				} catch (NumberFormatException nfe) {
					errMsg = res.getString("illegal_string") + ": [" + str + "]";
				}
				if (errMsg == null && num < 0) {
					errMsg = res.getString("illegal_number") + " (" + num + "); " + res.getString("must_be_positive_or_0");
				}
				if (errMsg == null) {
					if (num < 1) {
						continue;
					}
					nItemsTotal += num;
					infoType = res.getString("time_limit");
					tf = (TextField) maxTimeTextFields.elementAt(i);
					str = tf.getText();
					if (str != null) {
						str = str.trim();
						if (str.length() < 1) {
							str = null;
						}
					}
					if (str == null) {
						errMsg = res.getString("time_limit_not_specified");
					} else {
						num = 0;
						try {
							num = Integer.parseInt(str);
						} catch (NumberFormatException nfe) {
							errMsg = res.getString("illegal_string") + ": [" + str + "]";
						}
						if (errMsg == null && num < 1) {
							errMsg = res.getString("illegal_number") + " (" + num + "); " + res.getString("must_be_positive");
						}
					}
				}
				if (errMsg != null) {
					tf.requestFocus();
					tf.selectAll();
					str = res.getString("Error_in") + " " + infoType + " " + res.getString("for_site") + " " + siteIdsOfTextFields.elementAt(i);
					if (siteNameCN >= 0) {
						str += " \"" + souTable.getAttrValueAsString(siteNameCN, recNums.elementAt(i)) + "\"";
					}
					str += ": " + errMsg;
					notLine.showMessage(str, true);
					return;
				}
			}
			if (nItemsTotal < 1) {
				notLine.showMessage(res.getString("No_items_to_transport"), true);
				return;
			}
			dispose();
			putDataToTable(true);
			if (owner != null) {
				owner.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "items_in_sources"));
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
					DataRecord rec = souTable.getDataRecord(recNums.elementAt(i));
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
					DataRecord rec = new DataRecord(siteId + (souTable.getDataItemCount() + 1), siteName);
					souTable.addDataRecord(rec);
					rec.setAttrValue(siteId, siteIdCN);
					rec.setAttrValue(siteName, siteNameCN);
					rec.setAttrValue(siteType, siteTypeCN);
					int catIdx = catIdxs[i];
					if (itemCatCodes != null) {
						rec.setAttrValue(itemCatCodes.elementAt(catIdx), itemCatCodeCN);
					}
					rec.setAttrValue(itemCatNames.elementAt(catIdx), itemCatCN);
					rec.setNumericAttrValue(0, "0", itemNumCN);
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
					DataRecord rec = souTable.getDataRecord(recNums.elementAt(i));
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
			if (souLayer != null) {
				int layerIdx = lman.getIndexOfLayer(souLayer.getContainerIdentifier());
				if (layerIdx >= 0) {
					lman.activateLayer(layerIdx);
					DGeoLayer layer = (DGeoLayer) lman.getGeoLayer(layerIdx);
					Graphics g = core.getUI().getCurrentMapViewer().getMapDrawer().getGraphics();
					MapContext mc = core.getUI().getCurrentMapViewer().getMapDrawer().getMapContext();
					layer.highlightObject(siteId, false, g, mc);
				}
				int idx = souLayer.getObjectIndex(siteId);
				if (idx >= 0) {
					souLayer.removeGeoObject(idx);
				}
			}
			currSiteIdx = -1;
			for (int i = lastIdx; i >= firstIdx; i--) {
				int recN = recNums.elementAt(i);
				souTable.removeDataItem(recN);
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
					DataRecord rec = new DataRecord(siteId + (souTable.getDataItemCount() + 1), siteName);
					souTable.addDataRecord(rec);
					rec.setAttrValue(siteId, siteIdCN);
					rec.setAttrValue(siteName, siteNameCN);
					rec.setAttrValue(siteType, siteTypeCN);
					if (itemCatCodes != null) {
						rec.setAttrValue(itemCatCodes.elementAt(i), itemCatCodeCN);
					}
					rec.setAttrValue(itemCatNames.elementAt(i), itemCatCN);
					rec.setNumericAttrValue(0, "0", itemNumCN);
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
			if (souLayer != null) {
				SpatialEntity spe = new SpatialEntity(siteId, siteName);
				spe.setGeometry(point);
				DGeoObject gobj = new DGeoObject();
				gobj.setup(spe);
				gobj.setLabel(siteName);
				souLayer.addGeoObject(gobj);
			}
			Checkbox lastCB = (Checkbox) siteCheckers.elementAt(siteCheckers.size() - 1);
			lastCB.setState(true);
			findSelectedSite();
			return;
		}
	}

	public void addSites() {
		Vector suitableLayers = null, locations = null;
		boolean include[] = { addLayer1 != null, addLayer2 != null, locLayer != null };
		DGeoLayer mainLayer = null;
		for (int k = 0; k < 3 && locations == null; k++)
			if (include[k]) {
				DGeoLayer layer = (k == 0) ? addLayer1 : (k == 1) ? addLayer2 : locLayer;
				if (layer != null && layer.getObjectCount() > 0) {
					locations = new Vector(layer.getObjectCount(), 1);
					for (int i = 0; i < layer.getObjectCount(); i++) {
						DGeoObject gobj = layer.getObject(i);
						if (!StringUtil.isStringInVectorIgnoreCase(gobj.getIdentifier(), siteIdsOfCheckers)) {
							locations.addElement(gobj);
						}
					}
					if (locations.size() < 1) {
						locations = null;
					} else {
						mainLayer = layer;
					}
					include[k] = false;
				}
			}
		if (lman != null) {
			suitableLayers = new Vector(lman.getLayerCount(), 1);
			if (include[0]) {
				suitableLayers.addElement(addLayer1);
			}
			if (include[1]) {
				suitableLayers.addElement(addLayer2);
			}
			if (include[2]) {
				suitableLayers.addElement(locLayer);
			}
			for (int i = 0; i < lman.getLayerCount(); i++) {
				DGeoLayer layer = lman.getLayer(i);
				if (!layer.equals(locLayer) && !layer.equals(addLayer1) && !layer.equals(addLayer2) && !layer.equals(souLayer) && layer.getType() == Geometry.point && layer.getObjectCount() > 0) {
					suitableLayers.addElement(layer);
				}
			}
			if (suitableLayers.size() < 1) {
				suitableLayers = null;
			}
		}
		if (suitableLayers != null || locations != null) {
			SiteSelectionUI siteSelUI = new SiteSelectionUI(suitableLayers, locations, mainLayer, lman, core.getSupervisor(), core.getUI().getCurrentMapViewer());
			okFrame = new OKFrame(this, res.getString("new_sites"), true);
			okFrame.addContent(siteSelUI);
			okFrame.start();
			return;
		}
		lman.activateLayer(souLayer.getContainerIdentifier());
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
			lman.activateLayer(souLayer.getContainerIdentifier());
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
		lman.activateLayer(souLayer.getContainerIdentifier());
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
		for (int i = 0; i < sites.size(); i++) {
			DGeoObject gobj = (DGeoObject) sites.elementAt(i);
			String siteId = gobj.getIdentifier();
			String siteName = gobj.getName();
			while (siteId != null && StringUtil.isStringInVectorIgnoreCase(siteId, siteIdsOfCheckers)) {
				String str = res.getString("The_site") + " " + siteId;
				if (siteName != null) {
					str += " \"" + siteName + "\"";
				}
				DGeoObject obj = (DGeoObject) souLayer.findObjectById(siteId);
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
			putDataToTable(false);
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
					DataRecord rec = new DataRecord(siteId + (souTable.getDataItemCount() + 1), siteName);
					souTable.addDataRecord(rec);
					rec.setAttrValue(siteId, siteIdCN);
					rec.setAttrValue(siteName, siteNameCN);
					rec.setAttrValue(siteType, siteTypeCN);
					if (itemCatCodes != null) {
						rec.setAttrValue(itemCatCodes.elementAt(j), itemCatCodeCN);
					}
					rec.setAttrValue(itemCatNames.elementAt(j), itemCatCN);
					rec.setNumericAttrValue(0, "0", itemNumCN);
					added = true;
					siteAdded = true;
				}
			if (siteAdded && souLayer != null) {
				SpatialEntity spe = new SpatialEntity(siteId, siteName);
				spe.setGeometry(gobj.getGeometry());
				DGeoObject gobj1 = new DGeoObject();
				gobj1.setup(spe);
				gobj1.setLabel(siteName);
				souLayer.addGeoObject(gobj1);
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
		lman.activateLayer(souLayer.getContainerIdentifier());
		findSelectedSite();
	}

	public void itemStateChanged(ItemEvent e) {
		findSelectedSite();
	}

	protected void markCurrentSite() {
		erasePlaceMarker();
		if (currSiteIdx < 0)
			return;
		String id = (String) siteIdsOfCheckers.elementAt(currSiteIdx);
		GeoObject obj = souLayer.findObjectById(id);
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
			int layerIdx = lman.getIndexOfLayer(souLayer.getContainerIdentifier());
			lman.removeGeoLayer(layerIdx);
			layerWasAdded = false;
		}
		destroyed = true;
	}

	public boolean isDestroyed() {
		return destroyed;
	}
}
