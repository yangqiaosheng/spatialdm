package spade.analysis.plot;

import java.awt.Checkbox;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.Supervisor;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.Dialogs;
import spade.lib.basicwin.TextCanvas;
import spade.lib.lang.Language;
import spade.lib.util.StringUtil;
import spade.vis.action.HighlightListener;
import spade.vis.action.Highlighter;
import spade.vis.database.ObjectContainer;
import spade.vis.database.ObjectFilter;
import spade.vis.database.ObjectFilterBySelection;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Jul 2, 2009
 * Time: 11:43:39 AM
 * Privides a simple UI for switching on/off the filter of objects by direct selection.
 */
public class FilterBySelectionUI extends Panel implements QueryOrSearchTool, AttributeFreeTool, ItemListener, HighlightListener, PropertyChangeListener, Destroyable {
	static ResourceBundle res = Language.getTextResource("spade.analysis.plot.Res");
	/**
	* Identifier of this tool
	*/
	protected String methodId = null;
	/**
	* The container with the objects to be shown
	*/
	protected ObjectContainer objCont = null;
	/**
	 * The list of objects may be used as an interface to a filter.
	 * The filtering is done by an ObjectFilterBySelection.
	 */
	protected ObjectFilterBySelection selFilter = null;
	/**
	* The ObjectFilter is associated with a table or layer and contains results of data
	* querying. Used to display the number of active objects.
	*/
	protected ObjectFilter filter = null;

	protected Supervisor supervisor = null;
	/**
	* Indicates "destroyed" state. Initially is false.
	*/
	protected boolean destroyed = false;
	/**
	* The error message
	*/
	protected String err = null;
	/**
	 * When checked, the selection of objects (in any display) works as a filter
	 */
	protected Checkbox useAsFilterCB = null;

	protected TextField activeCountTF = null, selCountTF = null;
	/**
	* The name of the component (used as the name of the corresponding tab)
	*/
	protected String name = null;

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
		objCont = oCont;
	}

	/**
	* Returns the reference to the ObjectContainer this tool works with
	*/
	@Override
	public ObjectContainer getObjectContainer() {
		return objCont;
	}

	/**
	* Must set the identifiers of the attributes to be used in the tool.
	* Does not do anything in this class.
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
		if (objCont == null || objCont.getObjectCount() < 1) {
			err = res.getString("no_objects");
			return false;
		}
		setLayout(new ColumnLayout());
		TextCanvas tc = new TextCanvas();
		tc.setText(objCont.getName());
		add(tc);
		add(new Label(objCont.getObjectCount() + " objects"));
		Panel pp = new Panel(new GridLayout(1, 2));
		add(pp);
		Panel p = new Panel(new FlowLayout());
		pp.add(p);
		p.add(new Label("Active:"));
		activeCountTF = new TextField(5);
		activeCountTF.setEditable(false);
		p.add(activeCountTF);
		p = new Panel(new FlowLayout());
		pp.add(p);
		p.add(new Label("Selected:"));
		selCountTF = new TextField(5);
		selCountTF.setEditable(false);
		p.add(selCountTF);
		useAsFilterCB = new Checkbox("show only selected objects", false);
		add(useAsFilterCB);
		useAsFilterCB.addItemListener(this);
		if (supervisor != null) {
			supervisor.registerHighlightListener(this, objCont.getEntitySetIdentifier());
		}
		filter = objCont.getObjectFilter();
		objCont.addPropertyChangeListener(this);
		if (filter != null) {
			filter.addPropertyChangeListener(this);
		}
		showNumbers();

		return true;
	}

	protected void showNumbers() {
		if (objCont == null)
			return;
		if (filter == null || !filter.areObjectsFiltered()) {
			activeCountTF.setText(String.valueOf(objCont.getObjectCount()));
		} else {
			int nActive = 0;
			for (int i = 0; i < objCont.getObjectCount(); i++)
				if (filter.isActive(i)) {
					++nActive;
				}
			activeCountTF.setText(String.valueOf(nActive));
		}
		Vector sel = null;
		Highlighter highlighter = supervisor.getHighlighter(objCont.getEntitySetIdentifier());
		if (highlighter != null) {
			sel = highlighter.getSelectedObjects();
		}
		selCountTF.setText((sel == null) ? "0" : String.valueOf(sel.size()));
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
		if (objCont == null)
			return "Filter by selection (null)";
		name = "Filter by selection in " + objCont.getName();
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Returns true if the object list is used as an interface to an ObjectFilterBySelection
	 */
	public boolean usedAsFilter() {
		return useAsFilterCB != null && useAsFilterCB.getState();
	}

	@Override
	public void highlightSetChanged(Object source, String setId, Vector hlObj) {
	}

	@Override
	public void selectSetChanged(Object source, String setId, Vector hlObj) {
		if (supervisor == null || objCont == null)
			return;
		if (!StringUtil.sameStrings(setId, objCont.getEntitySetIdentifier()))
			return;
		Highlighter highlighter = supervisor.getHighlighter(setId);
		if (highlighter == null)
			return;
		Vector sel = highlighter.getSelectedObjects();
		selCountTF.setText((sel == null) ? "0" : String.valueOf(sel.size()));
		if (!usedAsFilter() || sel == null || sel.size() < 1)
			return;
		if (!getFilterBySelection())
			return;
		selFilter.setActiveObjects(sel);
	}

	protected boolean getFilterBySelection() {
		if (selFilter != null)
			return true;
		selFilter = new ObjectFilterBySelection();
		selFilter.setObjectContainer(objCont);
		selFilter.setEntitySetIdentifier(objCont.getEntitySetIdentifier());
		objCont.removePropertyChangeListener(this); //to avoid reacting to filter change
		objCont.setObjectFilter(selFilter);
		if (filter != null) {
			filter.removePropertyChangeListener(this);
		}
		filter = objCont.getObjectFilter();
		if (filter != null) {
			filter.addPropertyChangeListener(this);
		}
		objCont.addPropertyChangeListener(this);
		return true;
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (objCont == null || objCont.getObjectCount() < 1)
			return;
		if (e.getSource().equals(useAsFilterCB)) {
			if (selFilter == null && !getFilterBySelection()) {
				useAsFilterCB.setState(false);
				return;
			}
			if (useAsFilterCB.getState()) {
				Vector sel = null;
				Highlighter highlighter = supervisor.getHighlighter(objCont.getEntitySetIdentifier());
				if (highlighter != null) {
					sel = highlighter.getSelectedObjects();
				}
				if (sel == null || sel.size() < 1) {
					Dialogs.showMessage(CManager.getAnyFrame(this), "First select objects in some of the displays and then try again.", "Nothing is selected!");
					useAsFilterCB.setState(false);
					return;
				}
				selFilter.setActiveObjects(sel);
				if (highlighter != null) {
					highlighter.clearSelection(this);
				}
			} else {
				if (supervisor != null) {
					Highlighter highlighter = supervisor.getHighlighter(objCont.getEntitySetIdentifier());
					if (highlighter != null) {
						Vector active = selFilter.getActiveObjects();
						if (active != null && active.size() > 0) {
							highlighter.makeObjectsSelected(this, active);
						}
					}
				}
				selFilter.clearFilter();
			}
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		if (destroyed)
			return;
		if (pce.getSource().equals(filter))
			if (pce.getPropertyName().equals("destroyed")) {
				filter.removePropertyChangeListener(this);
				filter = null;
			} else {
				showNumbers();
			}
		else if (pce.getSource().equals(objCont))
			if (pce.getPropertyName().equals("destroyed")) {
				destroy();
			} else if (pce.getPropertyName().equals("filter")) {
				if (filter != null) {
					filter.removePropertyChangeListener(this);
				}
				filter = objCont.getObjectFilter();
				if (filter != null) {
					filter.addPropertyChangeListener(this);
				}
				showNumbers();
			} else if (pce.getPropertyName().equals("data_added") || pce.getPropertyName().equals("data_removed") || pce.getPropertyName().equals("ObjectSet")) {
				showNumbers();
			}
	}

	@Override
	public void destroy() {
		if (supervisor != null && objCont != null) {
			supervisor.removeHighlightListener(this, objCont.getEntitySetIdentifier());
		}
		if (objCont != null) {
			objCont.removePropertyChangeListener(this);
		}
		if (filter != null) {
			filter.removePropertyChangeListener(this);
		}
		if (selFilter != null) {
			objCont.removeObjectFilter(selFilter);
			selFilter.destroy();
			selFilter = null;
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
