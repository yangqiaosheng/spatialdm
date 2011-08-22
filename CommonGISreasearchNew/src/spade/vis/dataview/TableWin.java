package spade.vis.dataview;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.plot.QueryOrSearchTool;
import spade.analysis.system.Supervisor;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.Destroyable;
import spade.lib.lang.Language;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.ObjectContainer;
import ui.AttributeChooser;

//Window for visual table (TableViewer)
public class TableWin extends Panel implements QueryOrSearchTool, ItemListener, ActionListener, PropertyChangeListener, Destroyable {
	static ResourceBundle res = Language.getTextResource("spade.vis.dataview.Res");
	protected TableViewer tableViewer = null; //visual table
	protected Choice sortChoice; //sorting attribute choice
	protected Choice dirChoice; //sorting direction choice
	protected Checkbox tableLens = null, condensedCB = null;
	protected Button setShowingAttribute;
	protected AttributeDataPortion table = null; //Table
	/**
	* Contains the identifiers of the attributes to be shown in the table view.
	*/
	protected Vector attributes = null;
	/**
	* Supervisor provides access to the Highlighter (common for
	* all data displays) and in this way links together all displays
	*/
	protected Supervisor supervisor = null;
	/**
	* This checkbox allows to switch on and off grouping of table rows by classes
	*/
	protected Checkbox groupCB = null;
	/**
	* The panel with the controls for manilulating the table view
	*/
	protected Panel toolBar = null;
	/**
	* The panel with the controls for manipulating "table lens" view
	*/
	protected Panel tableLensPan = null;
	/**
	* Indicates whether the table was destroyed
	*/
	protected boolean destroyed = false;
	/**
	* The error message
	*/
	protected String err = null;

	@Override
	public String getName() {
		if (table == null)
			return res.getString("table_view") + " (null)";
		return res.getString("table_view") + ": " + table.getName();
	}

	/**
	* Sets a reference to the system's supervisor used for propagating events
	* among system's components.
	*/
	@Override
	public void setSupervisor(Supervisor sup) {
		supervisor = sup;
	}

	/**
	* Sets a reference to the ObjectContainer this tool must work with
	*/
	@Override
	public void setObjectContainer(ObjectContainer oCont) {
		if (oCont != null && (oCont instanceof AttributeDataPortion)) {
			table = (AttributeDataPortion) oCont;
		}
	}

	/**
	* Returns the reference to the ObjectContainer this tool works with
	*/
	@Override
	public ObjectContainer getObjectContainer() {
		if (table != null && (table instanceof ObjectContainer))
			return (ObjectContainer) table;
		return null;
	}

	/**
	* If could not construct itself, returns the error message explaining the
	* reason of the failure
	*/
	@Override
	public String getErrorMessage() {
		return err;
	}

	/**
	* Sets the identifiers of the attributes to be shown in the table view.
	*/
	@Override
	public void setAttributeList(Vector attr) {
		attributes = attr;
	}

	/**
	* Constructs the tool using the earlier provided references to the supervisor
	* and the object container. Returns true if successfully constructed.
	*/
	@Override
	public boolean construct() {
		if (table == null) {
			err = res.getString("no_data");
			return false;
		}
		if (!table.hasData()) {
			table.loadData();
		}
		if (!table.hasData()) {
			err = res.getString("no_data");
			return false;
		}
		Vector colIds = attributes;
		if (colIds == null || colIds.size() < 1) {
			AttributeChooser attrSel = new AttributeChooser();
			if (attrSel.selectColumns(table, null, null, false, res.getString("Select_attributes_to"), supervisor.getUI()) != null) {
				colIds = attrSel.getSelectedColumnIds();
			}
		}
		if (colIds == null) {
			err = res.getString("no_attributes");
			return false;
		}
		table.addPropertyChangeListener(this);
		if (supervisor != null) {
			supervisor.addPropertyChangeListener(this);
		}

		setLayout(new BorderLayout());
		tableViewer = new TableViewer(table, supervisor, this);
		add(tableViewer, "Center");
		tableViewer.setVisibleAttributes(colIds);
		tableViewer.setSortAttr(null);
		sortChoice = new Choice();
		fillingOfsortChoice(colIds);
		dirChoice = new Choice();
		// following string: "Ascending"
		dirChoice.add(res.getString("Ascending"));
		// following string: "Descending"
		dirChoice.add(res.getString("Descending"));
		dirChoice.select((tableViewer.getDescending()) ? 1 : 0);
		dirChoice.setEnabled(false);
		dirChoice.addItemListener(this);
		// following string: "TableLens"
		tableLens = new Checkbox(res.getString("TableLens"));
		// following string: "Attribute..."
		setShowingAttribute = new Button(res.getString("Attribute_"));
		tableViewer.setTableLens(tableLens.getState());
		sortChoice.addItemListener(this);
		tableLens.addItemListener(this);
		setShowingAttribute.addActionListener(this);
		toolBar = new Panel(new BorderLayout());
		toolBar.add(setShowingAttribute, BorderLayout.EAST);
		Panel p1 = new Panel(new BorderLayout(5, 0));
		p1.add(new Label(res.getString("sort_by"), Label.RIGHT), BorderLayout.WEST);
		p1.add(sortChoice, BorderLayout.CENTER);
		Panel p2 = new Panel(new BorderLayout());
		p2.add(dirChoice, BorderLayout.WEST);
		tableLensPan = new Panel(new BorderLayout());
		tableLensPan.add(tableLens, BorderLayout.WEST);
		p2.add(tableLensPan, BorderLayout.EAST);
		p1.add(p2, BorderLayout.EAST);
		toolBar.add(p1, BorderLayout.CENTER);
		add(toolBar, "South");
		return true;
	}

//initial or after property changing of table filling of sortChoice
	private void fillingOfsortChoice(Vector colIds) {
		if (sortChoice.getItemCount() > 0) {
			sortChoice.removeAll();
		}
		// following string: "No selection"
		sortChoice.addItem(res.getString("No_selection"));
		// following string: "Name (1-st column)"
		sortChoice.addItem(res.getString("Name_1_st_column_"));
		for (int i = 0; i < colIds.size(); i++) {
			sortChoice.addItem(table.getAttributeName((String) colIds.elementAt(i)));
		}
		String sortAttr = tableViewer.getSortAttr();
		if (sortAttr == null) {
			sortChoice.select(0);
		} else if (sortAttr.equals("_NAME_")) {
			sortChoice.select(1);
		} else {
			sortChoice.select(colIds.indexOf(sortAttr) + 2);
		}
	}

	private void setParameters() {
		switch (sortChoice.getSelectedIndex()) {
//if no selection
		case 0:
			tableViewer.setSortAttr(null);
			dirChoice.setEnabled(false);
			break;
//if selection by name
		case 1:
			tableViewer.setSortAttr("_NAME_");
			dirChoice.setEnabled(true);
			tableViewer.setDescending(dirChoice.getSelectedIndex() == 1);
			break;
//if selection by some attribute
		default:
			int aidx = sortChoice.getSelectedIndex() - 2;
			tableViewer.setSortAttr(aidx);
			dirChoice.setEnabled(true);
			tableViewer.setDescending(dirChoice.getSelectedIndex() == 1);
//Set horizontal scrollbar to show sorting attribute if it is not visible
			if (aidx < tableViewer.getHScrollValue() || aidx > (tableViewer.getHScrollValue() + tableViewer.getHScroll().getVisibleAmount() - 1)) {
				tableViewer.getHScroll().setValue(aidx);
//-----------------------------------------------------------------------------
			}
		}
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource().equals(sortChoice) || e.getSource().equals(dirChoice)) {
			setParameters();
		} else if (e.getSource().equals(tableLens)) {
			boolean lens = tableLens.getState();
			tableViewer.setTableLens(tableLens.getState());
			if (lens) {
				if (condensedCB == null) {
					condensedCB = new Checkbox(res.getString("condensed"), false);
					condensedCB.addItemListener(this);
				}
				tableLensPan.add(condensedCB, BorderLayout.EAST);
				tableLensPan.invalidate();
				adjustSizes();
			} else if (condensedCB != null && condensedCB.isShowing()) {
				tableLensPan.remove(condensedCB);
				condensedCB.setState(false);
				tableLensPan.invalidate();
				toolBar.invalidate();
				invalidate();
				getParent().validate();
			}
		} else if (e.getSource().equals(condensedCB)) {
			tableViewer.setCondensedMode(condensedCB.getState());
		} else if (e.getSource().equals(groupCB)) {
			tableViewer.setGroupByClasses(groupCB.getState());
		}
		tableViewer.tablerepaint();
		tableViewer.repaint();
	}

//----------------------- ActionListener interface -----------------------------
	@Override
	public void actionPerformed(ActionEvent e) {
		AttributeChooser attrSel = new AttributeChooser();
		Vector colIds = null, selected = null;
		int nAttr = (tableViewer.getVisibleAttrNs() == null) ? 0 : tableViewer.getVisibleAttrNs().size();
		if (nAttr > 0) {
			selected = new Vector(nAttr, 1);
			for (int i = 0; i < nAttr; i++) {
				selected.addElement(tableViewer.getVisibleAttrId(i));
			}
		}
		if (attrSel.selectColumns(table, selected, null, false, res.getString("Select_attributes_to"), supervisor.getUI()) != null) {
			colIds = attrSel.getSelectedColumnIds();
		}
		if (colIds == null)
			return;
		tableViewer.setVisibleAttributes(colIds);
		fillingOfsortChoice(colIds);
		setParameters();
		tableViewer.tablerepaint();
		tableViewer.repaint();
	}

	protected void adjustSizes() {
		toolBar.invalidate();
		Frame fr = CManager.getFrame(this);
		if (fr != null) {
			Dimension size = fr.getSize(), prefSize = fr.getPreferredSize();
			if (prefSize.width > size.width) {
				size.width = prefSize.width;
				Point p = fr.getLocation();
				Dimension scrSize = getToolkit().getScreenSize();
				if (p.x + size.width > scrSize.width) {
					p.x = scrSize.width - size.width;
					fr.setLocation(p.x, p.y);
				}
				fr.setSize(size.width, size.height);
			}
			invalidate();
			fr.validate();
		} else {
			invalidate();
			getParent().validate();
		}
	}

//----------------------- PropertyChangeListener interface --------------------
	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		if (pce.getSource() == table) {
			if (pce.getPropertyName().equals("destroyed") || pce.getPropertyName().equals("structure_complete")) {
				destroy();
			}
			return;
		}
		if (pce.getPropertyName().equals(Supervisor.eventObjectColors) && table.getEntitySetIdentifier().equals(pce.getNewValue())) { //propagated classification of objects
			boolean hasGroupCB = groupCB != null && groupCB.isShowing();
			if (hasGroupCB)
				if (supervisor.getObjectColorer() == null || !supervisor.getObjectColorer().getEntitySetIdentifier().equals(table.getEntitySetIdentifier())) {
					//if there is the "group" checkbox in the layout, remove it
					toolBar.remove(groupCB);
					toolBar.invalidate();
					invalidate();
					getParent().validate();
				} else {
					;
				}
			else if (supervisor.getObjectColorer() != null && supervisor.getObjectColorer().getEntitySetIdentifier().equals(table.getEntitySetIdentifier())) {
				//if there is no "group by classes" checkbox, create it and include in the layout
				if (groupCB != null && groupCB.isShowing())
					return;
				if (groupCB == null) {
					groupCB = new Checkbox(res.getString("group"), tableViewer.getGroupByClasses());
					groupCB.addItemListener(this);
				}
				toolBar.add(groupCB, BorderLayout.WEST);
				adjustSizes();
			}
		} else if (pce.getSource().equals(tableViewer) && pce.getPropertyName().equals("sorting")) {
			dirChoice.select((tableViewer.getDescending()) ? 1 : 0);
			sortChoice.select(tableViewer.getSortAttrIdx() + 2);
			dirChoice.setEnabled(tableViewer.getSortAttrIdx() >= -1);
		}
	}

//---------------------- Destroyable interface -------------------------------
	/**
	* Makes necessary operations for destroying, in particular, unregisters from
	* listening table changes.
	*/
	@Override
	public void destroy() {
		if (table != null) {
			table.removePropertyChangeListener(this);
		}
		if (supervisor != null) {
			supervisor.removePropertyChangeListener(this);
		}
		destroyed = true;
	}

	/**
	* Replies whether is destroyed or not
	*/
	@Override
	public boolean isDestroyed() {
		return destroyed;
	}
}
