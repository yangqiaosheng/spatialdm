package guide_tools.guide;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Vector;

import spade.analysis.system.SystemManager;
import spade.kbase.scenarios.ContextElement;
import spade.kbase.scenarios.Restriction;

/**
* Keeps information about currently defined context of a task or scenario.
* A context is specified by a number of ContextItems.
* Notifies registered listeners about changes of the context.
*/
public class Context {
	protected Vector items = null;
	protected SystemManager sysMan = null;
	/**
	* This variable contains the error message that can be generated in some
	* of the methods
	*/
	protected String err = null;

	/**
	* Returns the error message generated if some method could not be normally
	* performed
	*/
	public String getErrorMessage() {
		return err;
	}

	/**
	* Sets the System Manager - the source of necessary information about the
	* current system session
	*/
	public void setSystemManager(SystemManager sm) {
		sysMan = sm;
	}

	/**
	* Adds a new (defined) context item to the specification of the context.
	* May also add context items with null content (for example, when a context
	* element is optional, and the user did not select anything for it).
	* If an item with the same identifier was added earlier, the new item
	* replaces the old one.
	*/
	public void addContextItem(ContextItem cit) {
		if (cit == null || cit.getTemplate() == null)
			return;
		if (items == null) {
			items = new Vector(10, 10);
		}
		int idx = getContextItemIndex(cit.getId());
		if (idx >= 0) {
			items.removeElementAt(idx);
			//remove all the context elements that are dependent on the element to
			//be replaced
			Vector dependent = getDependentItems(cit.getId(), null);
			if (dependent != null) {
				for (int i = 0; i < dependent.size(); i++) {
					items.removeElement(dependent.elementAt(i));
				}
			}
		}
		items.addElement(cit);
		notifyContextChange();
	}

	/**
	* Returns the number of currently defined context items
	*/
	public int getItemCount() {
		if (items == null)
			return 0;
		return items.size();
	}

	/**
	* Returns the context item with the given index
	*/
	public ContextItem getContextItem(int idx) {
		if (idx < 0 || idx >= getItemCount())
			return null;
		return (ContextItem) items.elementAt(idx);
	}

	/**
	* Returns the index of the context item with the given identifier or -1
	* if there is no such item
	*/
	public int getContextItemIndex(String id) {
		if (id == null)
			return -1;
		for (int i = 0; i < getItemCount(); i++)
			if (id.equals(getContextItem(i).getId()))
				return i;
		return -1;
	}

	/**
	* Returns the context item with the given identifier or null if there is no
	* such item
	*/
	public ContextItem getContextItem(String id) {
		int idx = getContextItemIndex(id);
		if (idx < 0)
			return null;
		return getContextItem(idx);
	}

	/**
	* Check if the context item with the given identifier is already defined
	*/
	public boolean isContextElementDefined(String elementId) {
		if (elementId == null)
			return false;
		ContextItem cit = getContextItem(elementId);
		return cit != null && cit.getContent() != null;
	}

	/**
	* Returns a vector of context items dependent on the context item with the
	* given identifier (i.e. referring to it) or null.
	*/
	public Vector getDependentItems(String refItem, Vector dependent) {
		if (refItem == null)
			return dependent;
		for (int i = 0; i < getItemCount(); i++) {
			ContextItem cit = getContextItem(i);
			if (refItem.equals(cit.getRefersTo())) {
				if (dependent == null) {
					dependent = new Vector(10, 10);
				}
				dependent.addElement(cit);
				getDependentItems(cit.getId(), dependent);
			}
		}
		return dependent;
	}

	/**
	* Makes the context elements referring to the given task "active" or
	* "inactive", depending on the value of the argument active
	*/
	public void setTaskContextState(String taskId, boolean active) {
		if (taskId == null)
			return;
		boolean changed = false;
		Vector dependent = null;
		for (int i = 0; i < getItemCount(); i++) {
			ContextItem cit = getContextItem(i);
			if (cit.isActive() != active && taskId.equals(cit.getTaskId())) {
				cit.setIsActive(active);
				changed = true;
				if (!active) {
					dependent = getDependentItems(cit.getId(), dependent);
				}
			}
		}
		if (dependent != null) {
			for (int i = 0; i < dependent.size(); i++) {
				ContextItem cit = (ContextItem) dependent.elementAt(i);
				cit.setIsActive(active);
			}
		}
		if (changed) {
			notifyContextChange();
		}
	}

	/**
	* Returns the text corresponding to the given object from the definition
	* of a context item. The text is supposed to be shown to the user. For
	* example, for a given identifier of a layer returns the name of the
	* layer, for an identifier of an attribute - the name of the attribute.
	*/
	public String getTextForItemValue(ContextItem item, Object value) {
		if (value == null)
			return null;
		if (item == null || sysMan == null)
			return value.toString();
		ContextElement cel = item.getTemplate();
		if (cel == null)
			return value.toString();
		String id = null;
		if (value instanceof String) {
			id = (String) value;
		} else if (value instanceof Vector) {
			Vector v = (Vector) value;
			if (v.size() > 0 && (v.elementAt(0) instanceof String)) {
				id = (String) v.elementAt(0);
			}
		}
		if (id == null)
			return value.toString();
		String txt = null;
		if (cel.type.equals("layer")) { //"value" is the identifier of a layer
			txt = sysMan.getLayerName(item.getMapNumber(), id);
		} else if (cel.type.equals("attributes")) { //"value" is the identifier of an attribute
			ContextItem cit = getContextItem(cel.refersTo);
			if (cit != null && cit.getContent() != null && (cit.getContent() instanceof String)) {
				txt = sysMan.getAttributeName(id, item.getMapNumber(), (String) cit.getContent());
			}
		}
		if (txt == null)
			return value.toString();
		return txt;
	}

	/**
	* In the given text fills the "slots" (if any), i.e. replaces references to
	* context elements like #options# by actual names of these context elements
	*/
	public String fillNameSlots(String txt) {
		if (txt == null)
			return null;
		if (txt.indexOf('#') < 0)
			return txt;
		int i = 0;
		String result = "";
		while (i < txt.length()) {
			int j = txt.indexOf('#', i);
			if (j < 0) {
				j = txt.length();
			}
			result += txt.substring(i, j);
			i = j + 1;
			if (i < txt.length()) {
				j = txt.indexOf('#', i);
				if (j > 0) {
					String ident = txt.substring(i, j);
					//find the context element with this identifier
					ContextItem cit = getContextItem(ident);
					if (cit == null || cit.getContent() == null) {
						result += "#" + ident + "#";
					} else {
						result += "\"" + getTextForItemValue(cit, cit.getContent()) + "\"";
					}
					i = j + 1;
				}
			}
		}
		return result;
	}

	/**
	* If the context element is defined by means of selection from some options,
	* returns a vector of appropriate options to select from. If the options
	* are not available, generates an error message and returns null.
	*/
	public Vector getOptions(ContextElement cel, int mapN) {
		Vector options = null;
		if (cel.type.equals("layer")) {
			options = sysMan.getLayerList(mapN);
			if (options == null || options.size() < 1) {
				err = "No map layers available!";
				return null;
			}
			//check layer-specific restrictions
			if (cel.restrictions != null) {
				for (int i = 0; i < cel.restrictions.size() && options.size() > 0; i++) {
					Restriction r = (Restriction) cel.restrictions.elementAt(i);
					if (!r.isValid()) {
						continue;
					}
					if (r.type.equals("layer_content")) {
						//not yet supported
						//...
					} else if (r.type.equals("data_presence")) {
						for (int j = 0; j < r.getValuesCount(); j++) {
							String val = r.getValue(j);
							if (val.equals("thematic_data")) {
								//remove the layers that have no associated thematic data
								for (int k = options.size() - 1; k >= 0; k--)
									if (!sysMan.getLayerHasAttrData(mapN, (String) options.elementAt(k))) {
										options.removeElementAt(k);
									}
							}
						}
					}
				}
			}
			if (options.size() < 1) {
				err = "No appropriate map layers available!";
				return null;
			}
		} else if (cel.type.equals("attributes")) {
			//find out what layer the attributes must refer to
			String ref = cel.refersTo, layerId = null;
			if (ref == null) {
				err = "Knowledge base error: undefined reference in a context element!";
				return null;
			}
			ContextItem cit = getContextItem(ref);
			if (cit == null || cit.getContent() == null) {
				err = "The layer the attributes must refer to is undefined";
				return null;
			}
			Object content = cit.getContent();
			if (content instanceof String) {
				layerId = (String) content;
			}
			if (layerId == null) {
				err = "The layer the attributes must refer to is undefined";
				return null;
			}
			options = sysMan.getAttributesForLayer(mapN, layerId);
			if (options == null || options.size() < 1) {
				err = "No attributes referring to " + sysMan.getLayerName(mapN, layerId) + "!";
				return null;
			}
			//checks attribute-specific restrictions
			//...
		}
		//other types of context elements are currently not selected
		if (options == null || options.size() < 1)
			return null;
		//check type-independent restrictions, e.g. "exclude_items"
		if (cel.restrictions != null) {
			for (int i = 0; i < cel.restrictions.size() && options.size() > 0; i++) {
				Restriction r = (Restriction) cel.restrictions.elementAt(i);
				if (!r.isValid()) {
					continue;
				}
				if (r.type.equals("exclude_items")) {
					for (int j = 0; j < r.getValuesCount() && options.size() > 0; j++) {
						ContextItem cit = getContextItem(r.getValue(j));
						if (cit != null && cit.getContent() != null) {
							for (int k = options.size() - 1; k >= 0; k--) {
								String id = (String) options.elementAt(k);
								if (cit.getContent() instanceof String)
									if (id.equals(cit.getContent())) {
										options.removeElementAt(k);
									} else {
										;
									}
								else if (cit.getContent() instanceof Vector) {
									Vector v = (Vector) cit.getContent();
									if (v.contains(id)) {
										options.removeElementAt(k);
									}
								}
							}
						}
					}
				}
			}
		}
		if (options.size() < 1)
			return null;
		return options;
	}

	/**
	* Checks if the system supports specification of this type of context element
	*/
	public boolean isContextTypeSupported(String type) {
		if (type.equals("territory"))
			return sysMan.canSelectTerritory();
		if (type.equals("bound_rect"))
			return false; //not implemented yet
		if (type.equals("attr_filter"))
			return sysMan.canProcessQueries();
		return true;
	}

	/**
	* Checks if the context element can be defined:
	* 1) the context element is valid (has type and method);
	* 2) if definition involves selection from a set of options, there are
	*    some suitable options available (according to the type and restrictions)
	*/
	public boolean canBeDefined(ContextElement cel, int mapN) {
		if (cel == null)
			return false;
		if (cel.type == null || cel.method == null) {
			err = "Invalid context element: no type or method!";
			return false;
		}
		if (cel.refersTo != null)
			if (!isContextElementDefined(cel.refersTo))
				return true; //actually, it is unknown, but we should return boolean
		if (cel.method.equals("select_one") || cel.method.equals("select_many"))
			return getOptions(cel, mapN) != null;
		return true;
	}

//----------- notification about changes of the context ---------
	/**
	* A Context may have listeners of changes of the context.
	* The listeners should implement the PropertyChangeListener interface.
	* To handle the list of listeners and notify them about changes of the
	* object set or object data, the Context uses a PropertyChangeSupport.
	*/
	protected PropertyChangeSupport pcSupport = null;

	/**
	* Registeres a listener of changes of the context. The
	* listener must implement the PropertyChangeListener interface.
	*/
	public synchronized void addPropertyChangeListener(PropertyChangeListener l) {
		if (l == null)
			return;
		if (pcSupport == null) {
			pcSupport = new PropertyChangeSupport(this);
		}
		pcSupport.addPropertyChangeListener(l);
	}

	/**
	* Unregisteres a listener of changes of the context.
	*/
	public synchronized void removePropertyChangeListener(PropertyChangeListener l) {
		if (l == null || pcSupport == null)
			return;
		pcSupport.removePropertyChangeListener(l);
	}

	/**
	* An internal method used to notify all the listeners about changes of
	* the context.
	*/
	protected void notifyContextChange() {
		if (pcSupport == null)
			return;
		pcSupport.firePropertyChange("context", null, null);
	}
}
