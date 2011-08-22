package spade.analysis.plot;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.Supervisor;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.Dialogs;
import spade.lib.lang.Language;
import spade.lib.util.IntArray;
import spade.lib.util.StringUtil;
import spade.vis.action.HighlightListener;
import spade.vis.action.Highlighter;
import spade.vis.database.DataItem;
import spade.vis.database.ObjectContainer;
import spade.vis.database.ObjectFilter;
import spade.vis.database.ObjectFilterBySelection;
import spade.vis.spec.SaveableTool;
import spade.vis.spec.ToolSpec;

/**
* A list containing names of objects from a table. The user can select
* objects in this list and look where they are situated on a map or other
* garphical display.
*/
public class ObjectList extends Panel implements QueryOrSearchTool, AttributeFreeTool, SaveableTool, ItemListener, HighlightListener, PropertyChangeListener, Destroyable {
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
	* The ObjectFilter is associated with a table and contains results of data
	* querying. Only objects satisfying the current query (if any) are displayed
	*/
	protected ObjectFilter filter = null;
	/**
	 * The list of objects may be used as an interface to a filter.
	 * The filtering is done by an ObjectFilterBySelection.
	 */
	protected ObjectFilterBySelection selFilter = null;
	/**
	* The objects in the list are sorted. This array specifies the order of the
	* objects, i.e. which record corresponds to each position in the list.
	*/
	protected IntArray order = null;

	protected Supervisor supervisor = null;
	/**
	* The currently shown list containing object names
	*/
	protected List nameList = null;
	/**
	* Regulates the inclusion of object identifiers in the object list.
	*/
	protected Checkbox showIdsCB = null;
	/**
	 * When checked, the object list works as a filter
	 */
	protected Checkbox useAsFilterCB = null;

	protected TextField activeCountTF = null, selCountTF = null;
	/**
	* The desired number of rows
	*/
	protected int rows = 10;
	/**
	* Indicates "destroyed" state. Initially is false.
	*/
	protected boolean destroyed = false;
	/**
	* The error message
	*/
	protected String err = null;
	/**
	* The name of the component (used as the name of the corresponding tab)
	*/
	protected String name = null;
	/**
	* As a SaveableTool, a plot may be registered somewhere and, hence, must
	* notify the component where it is registered about its destroying. This vector
	* contains the listeners to be notified about destroying of the visualizer.
	*/
	protected Vector destroyListeners = null;

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

	public void setNRows(int nRows) {
		rows = nRows;
	}

	/**
	* Must set the identifiers of the attributes to be used in the tool.
	* Does not do anything in this class.
	*/
	@Override
	public void setAttributeList(Vector attr) {
	}

	/**
	* Constructs the tool using the specified references to the supervisor
	* and the object container. Returns true if successfully constructed.
	*/
	public boolean construct(Supervisor sup, int nRows, ObjectContainer oCont) {
		setSupervisor(sup);
		setNRows(nRows);
		setObjectContainer(oCont);
		return construct();
	}

	protected boolean namesDifferFromIds = false;

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
		filter = objCont.getObjectFilter();

		setLayout(new BorderLayout());
		Panel p = new Panel(new ColumnLayout());
		add(p, BorderLayout.NORTH);
		String txt = objCont.getName() + " (" + objCont.getObjectCount() + " " + res.getString("objects") + ")";
		p.add(new Label(txt));
		Panel pp = new Panel(new GridLayout(1, 2));
		p.add(pp);
		p = new Panel(new FlowLayout());
		pp.add(p);
		p.add(new Label("Active:"));
		activeCountTF = new TextField(3);
		activeCountTF.setEditable(false);
		p.add(activeCountTF);
		p = new Panel(new FlowLayout());
		pp.add(p);
		p.add(new Label("Selected:"));
		selCountTF = new TextField(3);
		selCountTF.setEditable(false);
		p.add(selCountTF);

		nameList = new List(rows, true);
		add(nameList, BorderLayout.CENTER);
		//setup();
		nameList.addItemListener(this);
		for (int i = 0; i < objCont.getObjectCount() && !namesDifferFromIds; i++) {
			DataItem dit = objCont.getObjectData(i);
			String str = dit.getName();
			namesDifferFromIds = str == null || str.length() < 1 || !str.equals(dit.getId());
		}
		p = new Panel(new ColumnLayout());
		add(p, BorderLayout.SOUTH);
		if (namesDifferFromIds) {
			showIdsCB = new Checkbox(res.getString("show_ids"), true);
			p.add(showIdsCB);
			showIdsCB.addItemListener(this);
		}
		useAsFilterCB = new Checkbox(res.getString("Use_as_filter"), false);
		p.add(useAsFilterCB);
		useAsFilterCB.addItemListener(this);
		setup();
		objCont.addPropertyChangeListener(this);
		if (filter != null) {
			filter.addPropertyChangeListener(this);
		}
		if (supervisor != null) {
			supervisor.registerHighlightListener(this, objCont.getEntitySetIdentifier());
		}
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
		if (objCont == null)
			return res.getString("Object_list") + " (null)";
		name = objCont.getName();
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	/**
	* Returns the reference to the ObjectContainer this tool works with
	*/
	@Override
	public ObjectContainer getObjectContainer() {
		return objCont;
	}

	/**
	 * Returns true if the object list is used as an interface to an ObjectFilterBySelection
	 */
	public boolean usedAsFilter() {
		return useAsFilterCB != null && useAsFilterCB.getState();
	}

	public boolean isActive(int n) {
		if (filter == null)
			return true;
		return filter.isActive(n);
	}

	/**
	* (Re)creates its contents, in particular, after changing the filter or
	* adding/removing records to/from the table
	*/
	protected void setup() {
		if (order == null) {
			order = new IntArray(objCont.getObjectCount(), 10);
		} else {
			order.removeAllElements();
		}
		if (isShowing()) {
			nameList.setVisible(false);
		}
		nameList.removeAll();
		boolean showIds = showIdsCB != null && showIdsCB.getState();
		Highlighter highlighter = null;
		Vector sel = null;
		if (supervisor != null) {
			highlighter = supervisor.getHighlighter(objCont.getEntitySetIdentifier());
		}
		if (highlighter != null) {
			sel = highlighter.getSelectedObjects();
		}
		int nSelected = 0;
		for (int i = 0; i < objCont.getObjectCount(); i++)
			if (isActive(i)) {
				String str = objCont.getObjectId(i);
				boolean selected = sel != null && StringUtil.isStringInVectorIgnoreCase(str, sel);
				if (namesDifferFromIds) {
					DataItem dit = objCont.getObjectData(i);
					String str1 = dit.getName();
					if (str1 != null && str.length() > 0)
						if (showIds) {
							str = str + "   " + str1;
						} else {
							str = str1;
						}
				}
				if (str == null || str.length() == 0) {
					str = "[" + i + "]";
				}
				int k = nameList.getItemCount();
				for (int j = 0; j < nameList.getItemCount(); j++)
					if (StringUtil.compareStrings(str, nameList.getItem(j)) < 0) {
						k = j;
						break;
					}
				nameList.add(str, k);
				if (selected) {
					nameList.select(k);
					++nSelected;
				}
				order.insertElementAt(i, k);
			}
		activeCountTF.setText(String.valueOf(nameList.getItemCount()));
		selCountTF.setText(String.valueOf(nSelected));
		nameList.setEnabled(!usedAsFilter());
		if (isShowing()) {
			nameList.setVisible(true);
		}
	}

	@Override
	public void highlightSetChanged(Object source, String setId, Vector hlObj) {
	}

	@Override
	public void selectSetChanged(Object source, String setId, Vector hlObj) {
		if (supervisor == null || objCont == null)
			return;
		if (source.equals(this))
			return;
		if (!StringUtil.sameStrings(setId, objCont.getEntitySetIdentifier()))
			return;
		Highlighter highlighter = supervisor.getHighlighter(setId);
		if (highlighter == null)
			return;
		Vector sel = highlighter.getSelectedObjects();
		int nSelected = 0;
		if (sel == null || sel.size() < 1) {
			for (int i = 0; i < nameList.getItemCount(); i++) {
				nameList.deselect(i);
			}
		} else {
			for (int i = 0; i < nameList.getItemCount(); i++) {
				String id = objCont.getObjectId(order.elementAt(i));
				if (StringUtil.isStringInVectorIgnoreCase(id, sel)) {
					nameList.select(i);
					++nSelected;
				} else {
					nameList.deselect(i);
				}
			}
		}
		selCountTF.setText(String.valueOf(nSelected));
	}

	protected boolean getFilterBySelection() {
		if (selFilter != null)
			return true;
		if (objCont == null || objCont.getObjectCount() < 1)
			return false;
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
		if (e.getSource().equals(nameList)) {
			if (usedAsFilter() || supervisor == null)
				return;
			Highlighter highlighter = supervisor.getHighlighter(objCont.getEntitySetIdentifier());
			if (highlighter == null)
				return;
			int selNums[] = nameList.getSelectedIndexes();
			Vector v = new Vector(selNums.length, 10);
			for (int selNum : selNums) {
				v.addElement(new String(objCont.getObjectId(order.elementAt(selNum))));
			}
			highlighter.replaceSelectedObjects(this, v);
		} else if (e.getSource().equals(showIdsCB)) {
			setup();
		} else if (e.getSource().equals(useAsFilterCB)) {
			if (selFilter == null && !getFilterBySelection()) {
				useAsFilterCB.setState(false);
				nameList.setEnabled(true);
				return;
			}
			if (useAsFilterCB.getState()) {
				int selNums[] = nameList.getSelectedIndexes();
				if (selNums == null || selNums.length < 1) {
					Dialogs.showMessage(CManager.getAnyFrame(this), "First select objects in the list and then switch to the filter mode", "Nothing is selected!");
					useAsFilterCB.setState(false);
					nameList.setEnabled(true);
					return;
				}
				if (supervisor != null) {
					Highlighter highlighter = supervisor.getHighlighter(objCont.getEntitySetIdentifier());
					if (highlighter != null) {
						highlighter.clearSelection(this);
					}
				}
				IntArray idxs = new IntArray(selNums.length, 10);
				for (int selNum : selNums) {
					idxs.addElement(order.elementAt(selNum));
				}
				selFilter.setActiveObjectIndexes(idxs);
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
			setup();
		}
	}

	@Override
	public void destroy() {
		if (filter != null) {
			filter.removePropertyChangeListener(this);
		}
		if (objCont != null) {
			objCont.removePropertyChangeListener(this);
		}
		if (supervisor != null && objCont != null) {
			supervisor.removeHighlightListener(this, objCont.getEntitySetIdentifier());
		}
		if (selFilter != null) {
			objCont.removeObjectFilter(selFilter);
			selFilter.destroy();
			selFilter = null;
		}
		destroyed = true;
		if (destroyListeners != null) {
			PropertyChangeEvent pce = new PropertyChangeEvent(this, "destroyed", null, null);
			for (int i = 0; i < destroyListeners.size(); i++) {
				((PropertyChangeListener) destroyListeners.elementAt(i)).propertyChange(pce);
			}
		}
	}

	/**
	* Replies whether is destroyed or not
	*/
	@Override
	public boolean isDestroyed() {
		return destroyed;
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
				setup();
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
				if (selFilter != null) {
					selFilter = null;
					getFilterBySelection();
				}
				setup();
			} else if (pce.getPropertyName().equals("data_added") || pce.getPropertyName().equals("data_removed") || pce.getPropertyName().equals("ObjectSet")) {
				setup();
			}
	}

//---------------- implementation of the SaveableTool interface ------------
	/**
	* Adds a listener to be notified about destroying of the visualize.
	* As a SaveableTool, a visualizer may be registered somewhere and, hence, must
	* notify the component where it is registered about its destroying.
	*/
	@Override
	public void addDestroyingListener(PropertyChangeListener lst) {
		if (lst == null)
			return;
		if (destroyListeners == null) {
			destroyListeners = new Vector(5, 5);
		}
		if (!destroyListeners.contains(lst)) {
			destroyListeners.addElement(lst);
		}
	}

	/**
	* Returns the keyword used in the opening tag of a stored state description
	* of this tool. A visualizer description (specification) is usually stored as
	* a sequence of lines starting with <map> and ending with </map>. Hence,
	* this method returns "map".
	*/
	@Override
	public String getTagName() {
		return "chart";
	}

	/**
	* Returns the specification of this visualizer (normally an instance of the
	* class spade.vis.spec.ToolSpec) for storing in a file.
	*/
	@Override
	public Object getSpecification() {
		ToolSpec spec = new ToolSpec();
		spec.tagName = getTagName();
		spec.methodId = getMethodId();
		if (objCont != null) {
			spec.table = objCont.getContainerIdentifier();
		}
		spec.properties = getProperties();
		return spec;
	}

	/**
	* Returns custom properties of the tool: String -> String
	* By default, returns null.
	*/
	public Hashtable getProperties() {
		return null;
	}

	/**
	* After the plot is constructed, it may be requested to setup its individual
	* properties according to the given list of stored properties.
	* The base Plot class does nothing in this method.
	*/
	@Override
	public void setProperties(Hashtable properties) {
	}

	/**
	* Through this function the component constructing the plot can set the
	* identifier of the visualization method.
	*/
	public void setMethodId(String id) {
		methodId = id;
	}

	/**
	* Returns the identifier of the visualization method implemented by this
	* class.
	*/
	public String getMethodId() {
		return methodId;
	}
}
