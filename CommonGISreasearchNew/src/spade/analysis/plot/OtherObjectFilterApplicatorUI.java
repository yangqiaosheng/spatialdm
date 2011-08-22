package spade.analysis.plot;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Vector;

import spade.analysis.system.DataKeeper;
import spade.analysis.system.ESDACore;
import spade.analysis.system.Supervisor;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.TextCanvas;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.ObjectContainer;
import spade.vis.database.OtherObjectFilterApplicator;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Oct 22, 2010
 * Time: 1:50:51 PM
 * A UI for applying filtering to an object set from another object set.
 * That another object set (second object set) must have a table with a column
 * containing identifiers of objects from the other dataset.
 */
public class OtherObjectFilterApplicatorUI extends Panel implements QueryOrSearchTool, AttributeFreeTool, Destroyable, ItemListener {
	/**
	 * The first object container
	 */
	protected ObjectContainer oCont1 = null;
	/**
	 * The second object container
	 */
	protected ObjectContainer oCont2 = null;
	/**
	 * The filter that applies the filtering of one set to the other one
	 */
	protected OtherObjectFilterApplicator fAppl = null;
	/**
	 * The error message
	 */
	protected String err = null;
	/**
	* The name of the component (used as the name of the corresponding tab)
	*/
	protected String name = null;
	/**
	 * Propagates highlighting and selection of objects
	 */
	protected Supervisor supervisor = null;
	/**
	 * UI elements
	 */
	protected Checkbox cb1to2 = null, cb2to1 = null, cbNoFilter = null;

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
		oCont1 = oCont;
	}

	/**
	* Returns the reference to the ObjectContainer this tool works with
	*/
	@Override
	public ObjectContainer getObjectContainer() {
		return oCont1;
	}

	/**
	* Sets the identifiers of the attributes to be used in the tool.
	*/
	@Override
	public void setAttributeList(Vector attr) {
	}

	/**
	* Constructs the tool using the earlier provided references to the supervisor
	* and the object container. Returns true if successfully constructed.
	*/
	@Override
	public boolean construct() {
		if (oCont1 == null)
			return false;
		ESDACore core = (ESDACore) supervisor.getSystemSettings().getParameter("core");
		if (core == null) {
			err = "Cannot get access to the system's core!";
			return false;
		}
		DataKeeper dk = core.getDataKeeper();
		Vector<ObjectContainer> otherContainers = new Vector<ObjectContainer>(10, 10);
		LayerManager lman = dk.getMap(0);
		String setId1 = oCont1.getEntitySetIdentifier();
		if (lman != null) {
			for (int i = 0; i < lman.getLayerCount(); i++) {
				GeoLayer layer = lman.getGeoLayer(i);
				if (!(layer instanceof ObjectContainer)) {
					continue;
				}
				if (layer.getObjectCount() < 2) {
					continue;
				}
				String setId = layer.getEntitySetIdentifier();
				if (setId == null || setId.equals(setId1)) {
					continue;
				}
				boolean sameSetId = false;
				for (int j = 0; j < otherContainers.size() && !sameSetId; j++) {
					sameSetId = setId.equals(otherContainers.elementAt(j).getEntitySetIdentifier());
				}
				if (!sameSetId) {
					otherContainers.addElement((ObjectContainer) layer);
				}
			}
		}
		for (int i = 0; i < dk.getTableCount(); i++) {
			AttributeDataPortion table = dk.getTable(i);
			if (!(table instanceof ObjectContainer)) {
				continue;
			}
			if (table.getDataItemCount() < 2) {
				continue;
			}
			String setId = table.getEntitySetIdentifier();
			if (setId == null || setId.equals(setId1)) {
				continue;
			}
			boolean sameSetId = false;
			for (int j = 0; j < otherContainers.size() && !sameSetId; j++) {
				sameSetId = setId.equals(otherContainers.elementAt(j).getEntitySetIdentifier());
			}
			if (!sameSetId) {
				otherContainers.addElement((ObjectContainer) table);
			}
		}
		if (otherContainers.size() < 1) {
			err = "No other object sets found!";
			return false;
		}
		Panel mainP = new Panel(new BorderLayout());
		Panel p = new Panel(new ColumnLayout());
		mainP.add(p, BorderLayout.NORTH);
		TextCanvas tc = new TextCanvas();
		tc.addTextLine("This filter applies filtering of objects of one set to another object set.\n ");
		tc.addTextLine("One of the two object sets must have a table with a column containing identifiers.");
		p.add(tc);
		p.add(new Label("You have selected the set:"));
		p.add(new Label(oCont1.getName()));
		p.add(new Line(false));
		p.add(new Label("Select the second object set:"));
		List list = new List(Math.min(10, otherContainers.size()));
		for (int i = 0; i < otherContainers.size(); i++) {
			list.add(otherContainers.elementAt(i).getName());
		}
		list.select(0);
		mainP.add(list, BorderLayout.CENTER);
		p = new Panel(new ColumnLayout());
		CheckboxGroup cbg = new CheckboxGroup();
		Checkbox cbRef1 = new Checkbox("Set 1 has a table column with identifiers of objects of set 2", true, cbg);
		Checkbox cbRef2 = new Checkbox("Set 2 has a table column with identifiers of objects of set 1", false, cbg);
		p.add(cbRef1);
		p.add(cbRef2);
		mainP.add(p, BorderLayout.SOUTH);
		OKDialog dia = new OKDialog(supervisor.getUI().getMainFrame(), "Filter of 2 related sets", true);
		dia.addContent(mainP);
		dia.show();
		if (dia.wasCancelled())
			return false;
		int idx = list.getSelectedIndex();
		if (idx < 0) {
			err = "The second object set was not selected!";
			return false;
		}
		oCont2 = otherContainers.elementAt(idx);
		if (cbRef1.getState()) {
			ObjectContainer oCont = oCont1;
			oCont1 = oCont2;
			oCont2 = oCont;
		}
		AttributeDataPortion table2 = null;
		if (oCont2 instanceof AttributeDataPortion) {
			table2 = (AttributeDataPortion) oCont2;
		} else if (oCont2 instanceof GeoLayer) {
			table2 = ((GeoLayer) oCont2).getThematicData();
		}
		if (table2 == null) {
			err = "Object set " + oCont2.getName() + " has no table!";
			return false;
		}
		list = new List(10);
		for (int i = 0; i < table2.getAttrCount(); i++) {
			list.add(table2.getAttributeName(i));
		}
		list.select(0);
		mainP = new Panel(new BorderLayout());
		p = new Panel(new ColumnLayout());
		mainP.add(p, BorderLayout.NORTH);
		p.add(new Label("Which column of the table"));
		p.add(new Label(table2.getName(), Label.CENTER));
		p.add(new Label("contains identifiers of objects from"));
		p.add(new Label(oCont1.getName() + "?", Label.CENTER));
		p.add(new Label("Select the column:"));
		mainP.add(list, BorderLayout.CENTER);
		dia = new OKDialog(supervisor.getUI().getMainFrame(), "Object identifiers", true);
		dia.addContent(mainP);
		dia.show();
		if (dia.wasCancelled())
			return false;
		int colN = list.getSelectedIndex();
		if (colN < 0)
			return false;
		fAppl = new OtherObjectFilterApplicator();
		fAppl.setObjectContainer(oCont1);
		fAppl.setSecondObjectContainer(oCont2);
		fAppl.setTableAndColumnWithObjectIds(table2, colN);
		if (!fAppl.hasDependencies()) {
			err = "Dependencies between the two sets could not been retrieved!";
			return false;
		}
		name = "2 sets filter: " + oCont1.getName() + " & " + oCont2.getName();
		setLayout(new ColumnLayout());
		add(new Label("1) " + oCont1.getName()));
		add(new Label("2) " + oCont2.getName()));
		add(new Label("Filtering direction:", Label.CENTER));
		p = new Panel(new GridLayout(1, 3));
		cbg = new CheckboxGroup();
		cb1to2 = new Checkbox("from set 1 to set 2", false, cbg);
		cb1to2.addItemListener(this);
		cb2to1 = new Checkbox("from set 2 to set 1", false, cbg);
		cb2to1.addItemListener(this);
		cbNoFilter = new Checkbox("none", true, cbg);
		cbNoFilter.addItemListener(this);
		p.add(cb1to2);
		p.add(cb2to1);
		p.add(cbNoFilter);
		add(p);
		return true;
	}

	/**
	* If could not construct itself, returns the error message explaining the
	* reason of the failure
	*/
	@Override
	public String getErrorMessage() {
		return err;
	}

	@Override
	public String getName() {
		if (name != null)
			return name;
		return "Filter of 2 related sets (invalid)";
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (cb1to2.getState()) {
			fAppl.setFilteringDirection(OtherObjectFilterApplicator.Filter1ToSet2);
		} else if (cb2to1.getState()) {
			fAppl.setFilteringDirection(OtherObjectFilterApplicator.Filter2ToSet1);
		} else {
			fAppl.setFilteringDirection(OtherObjectFilterApplicator.NoFilter);
		}
	}

	protected boolean destroyed = false;

	@Override
	public void destroy() {
		if (destroyed)
			return;
		if (fAppl != null) {
			fAppl.destroy();
		}
		destroyed = true;
	}

	@Override
	public boolean isDestroyed() {
		return destroyed;
	}
}
