package ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.ScrollPane;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Vector;

import spade.lib.basicwin.CManager;
import spade.lib.basicwin.TabSelectionListener;
import spade.lib.basicwin.TabbedPanel;
import spade.lib.basicwin.VisIconDrawer;
import spade.vis.mapvis.Visualizer;

/**
* Handles multiple manipulators that can be attached to the same map (but to
* different map layers). At each time moment the user has access to only one
* manipulator. If there are several manipulators, the ManipulatorHandler
* allows the user to switch between them.
*/

public class ManipulatorHandler extends TabbedPanel implements TabSelectionListener {
	/**
	* The identifiers of the manipulators' owners, e.g. map layers the
	* manipulators are attached to
	*/
	protected Vector ownerIds = null;
	/**
	* The names of the manipulators' owners, e.g. map layers the
	* manipulators are attached to
	*/
	protected Vector ownerNames = null;
	/**
	* The references to the visualizers manipulated by each of the manipulators.
	*/
	protected Vector visualizers = null;

	/**
	* Interface elements
	*/

	public ManipulatorHandler() {
		super();
		setTabsAtTheBottom(false);
		setHideTabNamesWhenIcons(true);
		setShowTabSelectorOnDemand(true);
		addTabSelectionListener(this);
	}

	/**
	* Checks whether the given component contains a scrollpane. This is needed
	* to avoid several nestedc scrollpanes in the manipulator area. Only if
	* the manipulator does not contain its own scrollpane, it is inserted in a
	* scrollpane by the ManipulatorHandler.
	*/
	protected boolean hasScrollPane(Component c) {
		if (c == null || !(c instanceof Container))
			return false;
		if (c instanceof ScrollPane)
			return true;
		Container cont = (Container) c;
		for (int i = 0; i < cont.getComponentCount(); i++)
			if (hasScrollPane(cont.getComponent(i)))
				return true;
		return false;
	}

	/**
	 * Activates the manipulator, i.e. brings its panel to front
	 * if the corresponding map layer has been activated in the legend
	 **/
	public void activateManipulator(String ownerId) {
		if (ownerId == null)
			return;
		if (getActiveTabName().startsWith(ownerId))
			return;
		showTab(ownerId);
	}

	/**
	* Adds the given manipulator to the set of existing manipulators.
	* The variable visualizer is a reference to the visualizer being manipulated.
	* ownerId is the identifier of the manipulator's owner, i.e. the layer
	* this manipulator is attached to.
	*/
	public void addManipulator(Component man, Object visualizer, String ownerId, String ownerName) {
		if (man == null || ownerId == null || visualizer == null)
			return;
		//removeManipulator(ownerId);
		int idx = -1;
		if (ownerIds == null) {
			ownerIds = new Vector(5, 5);
			ownerNames = new Vector(5, 5);
			visualizers = new Vector(5, 5);
		} else {
			idx = ownerIds.indexOf(ownerId);
		}
		ownerIds.addElement(ownerId);
		if (ownerName == null) {
			ownerName = ownerId;
		}
		String id = ownerId;
		if (idx >= 0) {
			//The layer has two manipulators (e.g., one for painting and one for signs).
			//The identifier and the label of the second manipulator for the same layer
			//are modified by attaching the symbol "*" at the end.
			id += "*";
			ownerName += "*";
		}
		ownerNames.addElement(ownerName);
		visualizers.addElement(visualizer);
		//add the manipulator to the layout
		if (hasScrollPane(man)) {
			//addComponent(ownerName,man);
			addComponent(id, man);
		} else {
			ScrollPane sp = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
			sp.add(man);
			//addComponent(ownerName,sp);
			addComponent(id, sp);
		}
		makeLayout();
		if (visualizer != null && visualizer instanceof Visualizer) {
			Visualizer vis = (Visualizer) visualizer;
			VisIconDrawer vid = new VisIconDrawer(vis, this);
			setTabIconDrawer(getTabCount() - 1, vid);
		}
		if (!ownerName.equals(ownerId)) {
			setToolTip(getTabCount() - 1, ownerName);
		}
		if (ownerIds.size() > 1) {
			showTab(getTabCount() - 1);
		} else {
			;
		}
		if (isShowing()) {
			CManager.validateAll(this);
		}
		notifyPropertyChange("active_man", null, ownerId);
	}

	/**
	* Removes the manipulator of the specified visualizer belonging to the
	* specified owner.
	*/
	public void removeManipulator(Object visualizer, String ownerId) {
		if (ownerId == null || ownerIds == null || visualizer == null)
			return;
		int idx = ownerIds.indexOf(ownerId);
		while (idx >= 0 && !visualizers.elementAt(idx).equals(visualizer)) {
			idx = ownerIds.indexOf(ownerId, idx + 1);
		}
		if (idx < 0)
			return;
		//remove the references from the lists
		ownerIds.removeElementAt(idx);
		ownerNames.removeElementAt(idx);
		visualizers.removeElementAt(idx);
		//remove the manipulator from the layout
		removeComponentAt(idx);
		String oid = null;
		if (ownerIds.size() > 0) {
			idx = ownerIds.indexOf(ownerId);
			if (idx >= 0) {
				//the layer had two manipulators (e.g., one for painting and one for signs)
				String name = (String) ownerNames.elementAt(idx);
				if (name.endsWith("*")) {
					//the label of the remaining manipulator has a modifier "*"
					//attached to it. It must be removed, i.e. the label must be simply the
					// name of the layer.
					name = name.substring(0, name.length() - 1);
					ownerNames.setElementAt(name, idx);
					Component cmp = getTabContent(idx);
					spade.lib.basicwin.Drawer dr = getTabIcon(idx);
					removeComponentAt(idx);
					//addComponent(name,cmp);
					addComponent(ownerId, cmp);
					setTabIconDrawer(getTabIndex(cmp), dr);
				}
			}
			oid = (String) ownerIds.elementAt(ownerIds.size() - 1);
		}
		if (oid != null) {
			showTab(oid);
		}
		if (isShowing()) {
			CManager.validateAll(this);
		}
		notifyPropertyChange("active_man", null, oid);
	}

	/**
	* Removes all the manipulators
	*/
	public void removeAllManipulators() {
		if (ownerIds == null || ownerIds.size() < 1)
			return;
		removeAllComponents();
		ownerIds.removeAllElements();
		ownerNames.removeAllElements();
		visualizers.removeAllElements();
		if (isShowing()) {
			CManager.validateAll(this);
		}
		notifyPropertyChange("active_man", null, null);
	}

	/**
	* Returns true if there is at least one manipulator
	*/
	public boolean hasManipulators() {
		return ownerIds != null && ownerIds.size() > 0;
	}

	/**
	* Returns the number of currently existing manipulators
	*/
	public int getManipulatorCount() {
		if (ownerIds == null)
			return 0;
		return ownerIds.size();
	}

	/**
	* Returns the manipulator with the given index
	*/
	public Component getManipulator(int idx) {
		if (idx >= 0 && idx < this.getTabCount())
			return getTabContent(idx);
		return null;
	}

	/**
	* Returns the manipulator attached to the owner with the given identifier
	*/
	public Component getManipulator(Object visualizer, String ownerId) {
		if (ownerIds == null || ownerId == null)
			return null;
		int idx = ownerIds.indexOf(ownerId);
		while (idx >= 0 && !visualizers.elementAt(idx).equals(visualizer)) {
			idx = ownerIds.indexOf(ownerId, idx + 1);
		}
		if (idx < 0)
			return null;
		return getTabContent(idx);
	}

	/**
	* Returns the identifier of the owner of the manipulator with the given index
	*/
	public String getOwnerId(int idx) {
		if (ownerIds != null && idx >= 0 && idx < ownerIds.size())
			return (String) ownerIds.elementAt(idx);
		return null;
	}

	/**
	* Returns the visualizer controlled by the manipulator with the given index
	*/
	public Object getVisualizer(int idx) {
		if (visualizers != null && idx >= 0 && idx < visualizers.size())
			return visualizers.elementAt(idx);
		return null;
	}

	/**
	* Returns all visualizers associated with the given layer
	*/
	public Vector getVisualizers(String layerId) {
		if (visualizers == null || visualizers.size() < 1 || layerId == null)
			return null;
		Vector vis = new Vector(2, 1);
		for (int i = 0; i < ownerIds.size(); i++)
			if (layerId.equals(ownerIds.elementAt(i))) {
				vis.addElement(visualizers.elementAt(i));
			}
		if (vis.size() < 1)
			return null;
		return vis;
	}

	/**
	* Reacts to selection of another layer
	*/
	@Override
	public void tabSelected(int idx, Object src) {
		//System.out.println("ManipulatorHandler:: tabSelected "+idx);
		if (ownerIds != null && src != null && src.equals(this)) {
			if (idx >= 0 && idx < ownerIds.size()) {
				String oid = (String) ownerIds.elementAt(idx);
				notifyPropertyChange("active_man", null, oid);
			}
		}
	}

	public void refreshVisualisationIcons() {
		forceRepaintTabSelector();
	}

//------------ notification about change of the active manipulator -----------
	protected PropertyChangeSupport pcSupport = null;

	@Override
	public void addPropertyChangeListener(PropertyChangeListener pcl) {
		if (pcl == null)
			return;
		if (pcSupport == null) {
			pcSupport = new PropertyChangeSupport(this);
		}
		pcSupport.addPropertyChangeListener(pcl);
	}

	@Override
	public void removePropertyChangeListener(PropertyChangeListener pcl) {
		if (pcl == null || pcSupport == null)
			return;
		pcSupport.removePropertyChangeListener(pcl);
	}

	public void notifyPropertyChange(String name, Object oldValue, Object newValue) {
		if (pcSupport != null) {
			pcSupport.firePropertyChange(name, oldValue, newValue);
		}
	}
}
