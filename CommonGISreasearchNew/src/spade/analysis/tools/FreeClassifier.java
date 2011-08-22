package spade.analysis.tools;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Choice;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.DataKeeper;
import spade.analysis.system.ESDACore;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.SelectDialog;
import spade.lib.lang.Language;
import spade.lib.util.IntArray;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.geometry.Geometry;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;
import spade.vis.spec.DataSourceSpec;

/**
* Interactive freehand classifier of objects in a table or/and map layer.
* Creates a dynamic attribute with the values corresponding to the classes.
* This attribute can be visualized and analysed using usual system functions.
* ==========================================================================
* * last updates:
* ==========================================================================
* => hdz, 03.2004:
*   ---------------
*   - extension of the selectable attributes for classification (integers)
*   - gui modification:
*      (1) options for create new, create using existing and edit existing attributes
*      (2) options for modifying the type of the attribute (string <-> numerical)
*      (3) event handling and definition of global variable for event handling to
*          update the gui according user entries
*   - functionality for copying attributes and changing their type
*   - constructorcall for freeeclassifier changes to pass attributetype
* ==========================================================================
*/
public class FreeClassifier implements DataAnalyser, SingleInstanceTool, WindowListener, ItemListener {
	static ResourceBundle res = Language.getTextResource("spade.analysis.tools.Res");
	/**
	* The FreeClassifier remembers the classification windows created in order
	* to prevent multiple concurrent classifications of the same object set
	*/
	protected Vector classWins = null;
	private Checkbox addNewCB = null; //global for eventhandling
	private Checkbox useOldCB = null; //global for eventhandling
	private Checkbox editOldCB = null; //global for eventhandling
	private TextField newAttrTF = null; //global for eventhandling
	private Checkbox numAttrCB = null; //global for eventhandling
	private Checkbox texAttrCB = null; //global for eventhandling
	private Choice attrCh = null; //global for eventhandling
	private Frame frame = null;
	private Panel typeOptionsPanel = null; //for global eventhandling
	private AttributeDataPortion selectedTable = null;
	private IntArray qualAttrNs = null;

	/**
	* Returns true.
	*/
	@Override
	public boolean isValid(ESDACore core) {
		return true;
	}

	/**
	* This method constructs and starts the classification tool.
	*/
	@Override
	public void run(ESDACore core) {
		if (core == null)
			return;
		DataKeeper dk = core.getDataKeeper();
		if (dk == null)
			return;
		if (dk.getMapCount() < 1 && dk.getTableCount() < 1)
			return; //no data
		Vector layers = new Vector(20, 10);
		IntArray layerMapNs = new IntArray(20, 10);
		for (int i = 0; i < dk.getMapCount(); i++) {
			LayerManager lm = dk.getMap(i);
			if (lm == null || lm.getLayerCount() < 1) {
				continue;
			}
			for (int j = 0; j < lm.getLayerCount(); j++) {
				GeoLayer gl = lm.getGeoLayer(j);
				if (gl != null && gl.getType() != Geometry.image && gl.getType() != Geometry.raster) {
					layers.addElement(gl);
					layerMapNs.addElement(i);
				}
			}
		}
		if (layers.size() < 1 && dk.getTableCount() < 1)
			return;
		Vector tables = null;
		IntArray layerIdxs = null;
		if (dk.getTableCount() > 0) {
			tables = new Vector(dk.getTableCount(), 1);
			layerIdxs = new IntArray(dk.getTableCount(), 1);
		}
		for (int i = 0; i < dk.getTableCount(); i++) {
			AttributeDataPortion table = dk.getTable(i);
			if (table == null) {
				continue;
			}
			tables.addElement(table);
			String layerName = null;
			if (table.getDataSource() != null && (table.getDataSource() instanceof DataSourceSpec)) {
				DataSourceSpec dss = (DataSourceSpec) table.getDataSource();
				layerName = dss.layerName;
			}
			int layerIdx = -1;
			if (layerName != null && layers.size() > 0) {
				layerName = layerName.toUpperCase();
				for (int j = 0; j < layers.size() && layerIdx < 0; j++) {
					GeoLayer gl = (GeoLayer) layers.elementAt(j);
					if (gl.getDataSource() != null && (gl.getDataSource() instanceof DataSourceSpec)) {
						DataSourceSpec lSpec = (DataSourceSpec) gl.getDataSource();
						if (lSpec.source == null) {
							continue;
						}
						String s1 = lSpec.source.toUpperCase();
						if (s1.endsWith(layerName)) {
							layerIdx = j;
						}
					}
				}
			}
			if (layerIdx < 0 && layers.size() > 0) {
				for (int j = 0; j < layers.size() && layerIdx < 0; j++) {
					GeoLayer gl = (GeoLayer) layers.elementAt(j);
					if (table.equals(gl.getThematicData())) {
						layerIdx = j;
					} else if (table.getEntitySetIdentifier().equals(gl.getEntitySetIdentifier())) {
						layerIdx = j;
					}
				}
			}
			layerIdxs.addElement(layerIdx);
		}
		if (tables != null && tables.size() < 1) {
			tables = null;
			layerIdxs = null;
		}
		if (layers.size() < 1 && tables == null)
			return;
		IntArray pureLayerNs = null; //layers without tables
		if (layers.size() > 0) {
			pureLayerNs = new IntArray(layers.size(), 1);
			for (int i = 0; i < layers.size(); i++)
				if (layerIdxs == null || layerIdxs.indexOf(i) < 0) {
					pureLayerNs.addElement(i);
				}
			if (pureLayerNs.size() < 1) {
				pureLayerNs = null;
			}
		}
		String prompt = (tables == null) ? res.getString("Select_layer") : (pureLayerNs != null) ? res.getString("Select_table_or_layer") : res.getString("Select_table");
		// for testing Frame frame=(core.getUI()!=null)?core.getUI().getMainFrame():null;
		frame = (core.getUI() != null) ? core.getUI().getMainFrame() : null;
		if (frame == null) {
			frame = CManager.getAnyFrame();
		}
		SelectDialog selDia = new SelectDialog(frame, res.getString("classify"), prompt);
		if (tables != null) {
			if (pureLayerNs != null) {
				selDia.addLabel(res.getString("tables") + ":");
			}
			for (int i = 0; i < tables.size(); i++) {
				AttributeDataPortion table = (AttributeDataPortion) tables.elementAt(i);
				String str = table.getName();
				if (layerIdxs.elementAt(i) >= 0) {
					GeoLayer gl = (GeoLayer) layers.elementAt(layerIdxs.elementAt(i));
					str += " (" + gl.getName() + ")";
				}
				selDia.addOption(str, table.getContainerIdentifier(), false);
			}
		}
		if (pureLayerNs != null) {
			if (tables != null) {
				selDia.addSeparator();
				selDia.addLabel(res.getString("layers") + ":");
			}
			for (int i = 0; i < pureLayerNs.size(); i++) {
				GeoLayer gl = (GeoLayer) layers.elementAt(pureLayerNs.elementAt(i));
				selDia.addOption(gl.getName(), gl.getContainerIdentifier(), false);
			}
		}
		selDia.show();
		if (selDia.wasCancelled())
			return;
		int idx = selDia.getSelectedOptionN();
		if (idx < 0)
			return;
		AttributeDataPortion table = null;
		GeoLayer layer = null;
		int mapN = -1;
		if (tables != null)
			if (idx < tables.size()) {
				table = (AttributeDataPortion) tables.elementAt(idx);
				int lIdx = layerIdxs.elementAt(idx);
				if (lIdx >= 0) {
					layer = (GeoLayer) layers.elementAt(lIdx);
					mapN = layerMapNs.elementAt(lIdx);
				}
			} else {
				idx -= tables.size();
			}
		if (table == null && pureLayerNs != null) {
			int lIdx = pureLayerNs.elementAt(idx);
			layer = (GeoLayer) layers.elementAt(lIdx);
			mapN = layerMapNs.elementAt(lIdx);
		}
		if (classWins != null && classWins.size() > 0) {
			String setId = (table != null) ? table.getEntitySetIdentifier() : layer.getEntitySetIdentifier();
			Frame win = null;
			for (int i = 0; i < classWins.size() && win == null; i++) {
				Frame fr = (Frame) classWins.elementAt(i);
				for (int j = 0; j < fr.getComponentCount(); j++)
					if (fr.getComponent(j) instanceof FreeClassifierUI) {
						FreeClassifierUI clUi = (FreeClassifierUI) fr.getComponent(j);
						if (setId.equals(clUi.getEntitySetIdentifier())) {
							win = fr;
						}
					}
			}
			if (win != null) {
				if (core.getUI() != null) {
					core.getUI().showMessage(res.getString("already_classified"), true);
				}
				win.toFront();
				return;
			}
		}
		if (layer != null && layer.getObjectCount() < 1) {
			layer.loadGeoObjects();
		}
		if (table != null && !table.hasData()) {
			table.loadData();
		}
		int nObj = 0;
		if (table != null) {
			nObj = table.getDataItemCount();
		} else {
			nObj = layer.getObjectCount();
		}
		if (nObj < 2) {
			if (core.getUI() != null) {
				core.getUI().showMessage(res.getString("too_few_objects"), true);
			}
			return;
		}
		//check whether the table has any qualitative attributes

		if (table != null) {
			selectedTable = table;
			qualAttrNs = null;
			for (int i = 0; i < table.getAttrCount(); i++)
				//if (AttributeTypes.isNominalType(table.getAttributeType(i))) { //hdz extended for integers
				if (AttributeTypes.isNominalType(table.getAttributeType(i)) || table.getAttributeType(i) == AttributeTypes.integer) {
					if (qualAttrNs == null) {
						qualAttrNs = new IntArray(50, 10);
					}
					qualAttrNs.addElement(i);
				}
		}

		Panel p = new Panel(new ColumnLayout());
		Label label1 = new Label(res.getString("give_attr_text"));
		p.add(label1);

		/* Fields for new Attribute name */
		Panel pfl = new Panel(new BorderLayout());
		Panel p1 = new Panel(new ColumnLayout());
		label1 = new Label(res.getString("give_new_name"));
		label1.setAlignment(Label.RIGHT);
		p1.add(label1);

		Panel p2 = new Panel(new ColumnLayout());
		String str = res.getString("classification"), name = str;
		if (table != null) {
			for (int i = 1; table.findAttrByName(name) >= 0; i++) {
				name = str + " " + i;
			}
		}
		newAttrTF = new TextField(name, 30);
		newAttrTF.setEnabled(true);
		p2.add(newAttrTF);

		/* Fields for existing Attribute selection */
		label1 = new Label(res.getString("give_old_name"));
		label1.setAlignment(Label.RIGHT);
		p1.add(label1);

		if (qualAttrNs != null) {
			attrCh = new Choice();
			attrCh.setEnabled(false);
			attrCh.addItemListener(this);

			p2.add(attrCh);
			for (int i = 0; i < qualAttrNs.size(); i++) {
				attrCh.add(table.getAttributeName(qualAttrNs.elementAt(i)));
			}
		}
		pfl.add(p1, "West");
		pfl.add(p2, "Center");
		p.add(pfl);

		/* Options */
		CheckboxGroup cbGroupAttribute = new CheckboxGroup();
		pfl = new Panel(new FlowLayout(FlowLayout.LEFT, 25, 0));
		addNewCB = new Checkbox(res.getString("cb_class_createneu"), true);
		addNewCB.addItemListener(this);
		//addNewCB.setFocusable(false);
		pfl.add(addNewCB);
		if (qualAttrNs != null) {
			addNewCB.setCheckboxGroup(cbGroupAttribute);
			//corrected by G.A.: at least one checkbox should be selected at start, otherwise
			//it causes errors
			//addNewCB.setState(false);
			//addNewCB.setFocusable(true);
			useOldCB = new Checkbox(res.getString("cb_class_createold"), false);
			useOldCB.setCheckboxGroup(cbGroupAttribute);
			useOldCB.addItemListener(this);
			pfl.add(useOldCB);
			editOldCB = new Checkbox(res.getString("cb_class_editold"), false);
			editOldCB.setCheckboxGroup(cbGroupAttribute);
			editOldCB.addItemListener(this);
			pfl.add(editOldCB);
		}
		Panel pp = new Panel(new FlowLayout(FlowLayout.LEFT, 5, 2));
		pp.add(pfl);
		p.add(pp);

		/* Option to set the type for new or editable Attribute */
		typeOptionsPanel = new Panel(new BorderLayout());
		Label typeOptionsLb = new Label(res.getString("class_attr_type"));
		typeOptionsPanel.add(typeOptionsLb, BorderLayout.WEST);
		pfl = new Panel(new FlowLayout(FlowLayout.LEFT, 15, 3));
		CheckboxGroup cbGroupType = new CheckboxGroup();
		texAttrCB = new Checkbox(res.getString("class_attr_tex"), true);
		texAttrCB.setCheckboxGroup(cbGroupType);
		pfl.add(texAttrCB, "West");

		numAttrCB = new Checkbox(res.getString("class_attr_num"), false);
		numAttrCB.setCheckboxGroup(cbGroupType);
		pfl.add(numAttrCB);
		//Label label2 = new Label("                                 ");
		//pfl.add(label2);
		pp = new Panel(new FlowLayout(FlowLayout.LEFT, 5, 0));
		//pp.setBackground(Color.GRAY);
		//pfl.setBackground(Color.lightGray);
		pp.add(pfl);
		typeOptionsPanel.add(pp, BorderLayout.SOUTH);
		p.add(typeOptionsPanel);

		/* Create an ok-cancel dialog */
		OKDialog okd = new OKDialog(frame, res.getString("class_attribute"), true);
		okd.addContent(p);
		newAttrTF.addActionListener(okd);

		okd.show();
		if (okd.wasCancelled())
			return;
		int attrIdx = -1;
		if ((editOldCB != null && editOldCB.getState()) || (useOldCB != null && useOldCB.getState())) {
			attrIdx = qualAttrNs.elementAt(attrCh.getSelectedIndex());
		}
		if ((addNewCB != null && addNewCB.getState()) || (useOldCB != null && useOldCB.getState())) {
			String attrName = newAttrTF.getText();
			if (attrName != null) {
				attrName = attrName.trim();
			}
			if (attrName == null || attrName.length() < 1)
				return;
			str = attrName;
			if (table != null) {
				for (int i = 1; table.findAttrByName(attrName) >= 0; i++) {
					attrName = str + " " + i;
				}
			}
			if (layer != null) {
				layer.setLayerDrawn(true);
				dk.getMap(mapN).activateLayer(layer.getContainerIdentifier());
			}
			if (table == null) {
				DataTable dTable = new DataTable();
				table = dTable;
				dTable.setName(layer.getName());
				dTable.setEntitySetIdentifier(layer.getEntitySetIdentifier());
				boolean link = layer.getThematicData() == null;
				DGeoLayer gl = (DGeoLayer) layer;
				for (int i = 0; i < nObj; i++) {
					DGeoObject gobj = gl.getObject(i);
					DataRecord drec = new DataRecord(gobj.getIdentifier());
					dTable.addDataRecord(drec);
					if (link) {
						gobj.setThematicData(drec);
					}
				}
				if (link) {
					gl.setDataTable(dTable);
					gl.setLinkedToTable(true);
				}
				int tblN = core.getDataLoader().addTable(dTable);
				core.getDataLoader().setLink(gl, tblN);
			}
			if (this.useOldCB != null && this.useOldCB.getState()) { //copy the content
				if (table instanceof DataTable && attrIdx >= 0) {
					DataTable myTable = (DataTable) table;
					Attribute selAttribute = myTable.getAttribute(attrIdx);
					char atype = AttributeTypes.character;
					Hashtable textHash = null;
					int keys = 0;
					Integer valint = null;
					if (this.numAttrCB.getState()) {
						atype = AttributeTypes.integer;
						textHash = new Hashtable();
					}
					int newAttrIdx = myTable.addDerivedAttribute(attrName, atype, selAttribute.origin, myTable.getAllAttrValues(selAttribute.getIdentifier()));
					for (int i = 0; i < myTable.getDataItemCount(); i++) {
						DataRecord dt = myTable.getDataRecord(i);
						Object value = dt.getAttrValue(attrIdx);
						if (value != null & this.numAttrCB.getState()) {
							try {
								if (value instanceof String) {
									if (((String) value).equalsIgnoreCase("false")) {
										valint = new Integer(0);
									} else if (((String) value).equalsIgnoreCase("true")) {
										valint = new Integer(1);
									} else {
										valint = new Integer((String) value);
									}
								}
								dt.setAttrValue(valint.toString(), newAttrIdx);
							} catch (NumberFormatException nex) {
								valint = (Integer) textHash.get(value);
								if (valint == null) {
									valint = new Integer(++keys);
									textHash.put(value, valint);
								}
								dt.setAttrValue(valint.toString(), newAttrIdx);
							}
						} else {
							dt.setAttrValue(value, newAttrIdx);
						}
					}
				}
			} else {
				table.addAttribute(attrName, null, AttributeTypes.character);
			}
			attrIdx = table.getAttrCount() - 1;
		}
		/* hdz, changed for sending information about type of attributes
		FreeClassifierUI clUI=new FreeClassifierUI((DataTable)table,attrIdx,
		                                           (DGeoLayer)layer,mapN,core);*/
		FreeClassifierUI clUI = new FreeClassifierUI((DataTable) table, attrIdx, (DGeoLayer) layer, mapN, core, getCheckboxChoice());
		Frame win = core.getDisplayProducer().makeWindow(clUI, res.getString("classify"));
		win.addWindowListener(this);
		if (classWins == null) {
			classWins = new Vector(10, 10);
		}
		classWins.addElement(win);
	}

	/** hdz for listening to dialog item change events
	 * @param ItemEvent
	 */
	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() instanceof Choice) {
			attrCh_itemStateChanged(e);
			return;
		}
		addNewCB_itemStateChanged(e);
		useOldCB_itemStateChanged(e);
		editOldCB_itemStateChanged(e);
	}

	void addNewCB_itemStateChanged(ItemEvent e) {
		try {
			newAttrTF.setEnabled(addNewCB.getState() || useOldCB.getState());
			//((Container)addNewCB.getParent().getParent()).getComponent(2).getComponent(1).setEnabled(addNewCB.getState() || useOldCB.getState());
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
	}

	void useOldCB_itemStateChanged(ItemEvent e) {
		try {
			if (useOldCB != null) {
				attrCh.setEnabled(useOldCB.getState() || editOldCB.getState());
				if (useOldCB.getState()) {
					attrCh_itemStateChanged(e);
				}
			}
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
	}

	void editOldCB_itemStateChanged(ItemEvent e) {
		try {
			if (editOldCB != null) {
				typeOptionsPanel.setVisible(!editOldCB.getState());
				if (editOldCB.getState()) {
					attrCh_itemStateChanged(e);
				}
			}
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}

	}

	void attrCh_itemStateChanged(ItemEvent e) {
		try {
			if (selectedTable == null || qualAttrNs == null)
				return;
			char atype = selectedTable.getAttributeType(qualAttrNs.elementAt(attrCh.getSelectedIndex()));
			boolean num = AttributeTypes.isNumericType(atype);
			numAttrCB.setState(num);
			texAttrCB.setState(!num);
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
	}

	/**
	 * Method to obtain the state of the selected checkbox
	 * @param e none
	 */
	private int getCheckboxChoice() {
		int val = 0;
		if (this.numAttrCB.getState()) {
			val = 10;
		}
		if (addNewCB.getState())
			return val + FreeClassifierUI.NEW_ATTRIBUTE; //  =0
		if (useOldCB.getState())
			return val + FreeClassifierUI.USE_ATTRIBUTE; //  =1
		if (editOldCB.getState())
			return FreeClassifierUI.EDIT_ATTRIBUTE; //  =2
		return val;
	}

	@Override
	public void windowClosed(WindowEvent e) {
		if (classWins != null && classWins.size() > 0) {
			int idx = classWins.indexOf(e.getWindow());
			if (idx >= 0) {
				classWins.removeElementAt(idx);
			}
		}
	}

	@Override
	public void windowOpened(WindowEvent e) {
	}

	@Override
	public void windowClosing(WindowEvent e) {
	}

	@Override
	public void windowIconified(WindowEvent e) {
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
	}

	@Override
	public void windowActivated(WindowEvent e) {
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
	}
}
