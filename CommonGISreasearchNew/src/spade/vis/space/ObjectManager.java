package spade.vis.space;

import java.awt.Color;
import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Vector;

import spade.analysis.system.Supervisor;
import spade.lib.util.StringUtil;
import spade.time.TimeMoment;
import spade.time.TimePositionNotifier;
import spade.time.TimeReference;
import spade.vis.action.HighlightListener;
import spade.vis.action.Highlighter;
import spade.vis.action.ObjectEvent;
import spade.vis.action.ObjectEventHandler;
import spade.vis.database.DataItem;
import spade.vis.database.DataTreater;
import spade.vis.database.ObjectFilter;
import spade.vis.database.ThematicDataItem;
import spade.vis.database.ThematicDataSupplier;
import spade.vis.database.TimeFilter;
import spade.vis.dataview.DataViewInformer;
import spade.vis.dataview.DataViewRegulator;
import spade.vis.dataview.TransformedDataPresenter;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DMovingObject;
import spade.vis.event.DEvent;
import spade.vis.event.DMouseEvent;
import spade.vis.event.EventConsumer;
import spade.vis.map.MapDraw;

/**
* Object Manager
* 1) keeps a list of currently visible Geo Objects;
* 2) notifies about object set changes (objects added/deleted/replaced)
*    and changes of thematic data associated with the objects;
* 3) finds the object selected in a map;
* 4) manages object highlighting in the map;
*/

public class ObjectManager implements ThematicDataSupplier, DataTreater, DataViewInformer, PropertyChangeListener, EventConsumer, HighlightListener {
	/**
	* Used to generate unique identifiers of instances of ObjectManager
	*/
	protected static int nInstances = 0;
	protected int instanceN = 0;

	protected GeoLayer layer = null;
	/**
	* The map, on which the objects are drawn in "normal" or "highlighted" state.
	*/
	protected MapDraw map = null;
	/**
	* The ObjectManager sends the object events it generates to an
	* ObjectEventHandler.
	*/
	protected ObjectEventHandler objEvtHandler = null;
	/**
	* The Supervisor allows the ObjectManager to get a reference to the
	* appropriate highlighter, depending on the active map layer.
	*/
	protected Supervisor supervisor = null;
	/**
	* Vectors of last highlighted and last selected geographical objects of the
	* GeoLayer.
	*/
	protected Vector lastHL = null, lastSel = null;

	public ObjectManager(Supervisor sup) {
		instanceN = ++nInstances;
		supervisor = sup;
	}

	/**
	* Propagates notifications about changes received from the GeoLayer to its
	* own listeners. These listeners are not interested in all changes occurring
	* in the GeoLayer, only the changes of the current object set (objects
	* added/deleted/replaced) and of the thematic data associated with the
	* objects. Accordingly, only these kinds of property changes are transmitted
	* to the Listeners of the Object Manager.
	*/
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getSource() == layer)
			if (e.getPropertyName().equals("ObjectSet")) {
				notifyPropertyChange("ObjectSet", null, layer.getObjects());
			} else if (e.getPropertyName().equals("ObjectData") || e.getPropertyName().equals("ThematicDataRemoved")) {
				notifyPropertyChange("ObjectData", null, null);
			} else if (e.getPropertyName().equals("update")) {
				//restore selection
				if (lastSel != null) {
					lastSel.removeAllElements();
				}
				if (map != null) {
					Highlighter hl = supervisor.getHighlighter(layer.getEntitySetIdentifier());
					if (hl != null) {
						Vector selected = hl.getSelectedObjects();
						if (selected != null && selected.size() > 0) {
							if (lastSel == null) {
								lastSel = new Vector(50, 20);
							}
							Graphics g = map.getGraphics();
							for (int i = 0; i < selected.size(); i++)
								if (layer.selectObject((String) selected.elementAt(i), true, g, map.getMapContext())) {
									lastSel.addElement(selected.elementAt(i));
								}
						}
					}
				}
			}
	}

	/**
	* An Object Manager is always connected to a GeoLayer and has access to its
	* GeoObjects. It listens to changes of properties of the GeoLayer being
	* interested in changes of the set of objects (objects added/deleted/replaced)
	* and of the thematic data associated with the objects.
	* An ObjectManager can be disconnected from the GeoLayer it was previously
	* connected and connected to another layer (for example, when the active layer
	* changes).
	* The ObjectManager gets from the supervisor the Highlighter corresponding
	* to the active layer and registeres itself as a listener of this highlighter.
	* If it was before listening to some other highlighter, it unregisters itself
	* from listeners of the previous highlighter.
	*/
	public void setGeoLayer(GeoLayer gl) {
		if (gl == layer)
			return;
		Graphics g = null;
		if (map != null) {
			g = map.getGraphics();
		}
		if (layer != null) {
			layer.removePropertyChangeListener(this);
			if (map != null && lastHL != null && lastHL.size() > 0) {
				//dehighlight all earlier highlighted objects
				for (int i = 0; i < lastSel.size(); i++) {
					layer.highlightObject((String) lastSel.elementAt(i), false, g, map.getMapContext());
				}
			}
			if (map != null && lastSel != null && lastSel.size() > 0) {
				map.restorePicture();
				//layer.hideSelection(map.getGraphics(),map.getMapContext());
				//deselect all earlier selected objects
				for (int i = 0; i < lastSel.size(); i++) {
					layer.selectObject((String) lastSel.elementAt(i), false, g, map.getMapContext());
				}
			}
			if (supervisor != null) {
				Highlighter highlighter = supervisor.getHighlighter(layer.getEntitySetIdentifier());
				if (highlighter != null) {
					highlighter.removeHighlightListener(this);
				}
			}
		}
		if (lastSel != null) {
			lastSel.removeAllElements();
		}
		if (lastHL != null) {
			lastHL.removeAllElements();
		}
		layer = gl;
		if (layer != null) {
			layer.addPropertyChangeListener(this);
			if (supervisor != null) {
				String setId = layer.getEntitySetIdentifier();
				if (setId != null) {
					Highlighter highlighter = supervisor.getHighlighter(setId);
					if (highlighter != null) {
						//register as listener of highlighting
						highlighter.addHighlightListener(this);
						//mark currently selected objects
						//these objects might be selected in other displays linked to the map
						selectSetChanged(highlighter, setId, highlighter.getSelectedObjects());
					}
				}
			}
		}
		if (g != null) {
			g.dispose();
		}
	}

	public GeoLayer getGeoLayer() {
		return layer;
	}

	/**
	 * Supposed to set a generic name of the entities in the container.
	 * Does nothing.
	 */
	@Override
	public void setGenericNameOfEntity(String name) {
	}

	/**
	 * Returns the generic name of the entities in the geo layer this ObjectManager is attached to.
	 * May return null, if the name was not previously set.
	 */
	@Override
	public String getGenericNameOfEntity() {
		if (layer == null)
			return null;
		return layer.getGenericNameOfEntity();
	}

	//------------- providing access to GeoObjects of the GeoLayer ---------

	/**
	* A method from the ThematicDataSupplier interface.
	* Returns the number of GeoObjects currently available in the GeoLayer.
	*/
	@Override
	public int getObjectCount() {
		if (layer == null)
			return 0;
		return layer.getObjectCount();
	}

	/**
	* Returns the GeoObject at the given index.
	*/
	public GeoObject getObjectAt(int idx) {
		if (layer == null)
			return null;
		return layer.getObjectAt(idx);
	}

	/**
	* A method from the ThematicDataSupplier interface.
	* Returns thematic data associated with the object at the given index.
	*/
	@Override
	public ThematicDataItem getThematicData(int idx) {
		GeoObject obj = getObjectAt(idx);
		if (obj == null)
			return null;
		return obj.getData();
	}

	/**
	* A method from the ThematicDataSupplier interface.
	* Finds GeoObject with the given identifier and returns its indey.
	*/
	@Override
	public int getObjectIndex(String id) {
		if (id == null)
			return -1;
		int n = getObjectCount();
		for (int i = 0; i < n; i++)
			if (getObjectAt(i).getIdentifier().equalsIgnoreCase(id))
				return i;
		return -1;
	}

	/**
	* Finds GeoObject with the given identifier and returns this object.
	* Uses the method findObjectById of its GeoLayer.
	*/
	public GeoObject findObjectById(String id) {
		if (layer == null)
			return null;
		return layer.findObjectById(id);
	}

	/**
	* Returns the object with the given index.
	*/
	@Override
	public Object getObject(int idx) {
		return getObjectAt(idx);
	}

	/**
	* A method from the ThematicDataSupplier interface.
	* Finds GeoObject with the given identifier and returns the data
	* associated with the object.
	* Uses the method findObjectById of its GeoLayer.
	*/
	@Override
	public ThematicDataItem getThematicData(String id) {
		GeoObject obj = findObjectById(id);
		if (obj == null)
			return null;
		return obj.getData();
	}

	/**
	* A method from the ObjectContainer interface.
	* Uses the method getThematicData(id).
	*/
	public DataItem getObjectData(String id) {
		return getThematicData(id);
	}

	/**
	* A method from the ObjectContainer interface.
	* Uses the method getThematicData(idx).
	*/
	@Override
	public DataItem getObjectData(int idx) {
		return getThematicData(idx);
	}

	/**
	* A method from the ObjectContainer interface.
	* Uses the method getThematicData(idx).
	*/
	@Override
	public String getObjectId(int idx) {
		if (layer == null)
			return null;
		return layer.getObjectAt(idx).getIdentifier();
	}

	/**
	* A method from the ObjectContainer interface. Returns the name of the layer.
	*/
	@Override
	public String getName() {
		if (layer != null)
			return layer.getName();
		return null;
	}

	/**
	* A method from the ObjectContainer interface. Returns the identifier of the layer.
	*/
	@Override
	public String getContainerIdentifier() {
		if (layer != null)
			return layer.getContainerIdentifier();
		return null;
	}

	/**
	 * A method from the ObjectContainer interface. Does nothing.
	*/
	@Override
	public void setEntitySetIdentifier(String setId) {
	}

	/**
	* A method from the ObjectContainer interface. Returns the set identifier
	* of the layer.
	*/
	@Override
	public String getEntitySetIdentifier() {
		if (layer != null)
			return layer.getEntitySetIdentifier();
		return null;
	}

	/**
	* Reports whether there are any objects in this container.
	*/
	@Override
	public boolean hasData() {
		if (layer == null)
			return false;
		return layer.getObjectCount() > 0;
	}

	/**
	 * A method from the ObjectContainer interface. Does nothing. Returns true.
	*/
	@Override
	public boolean loadData() {
		return true;
	}

	/**
	 * Reports whether the objects in this container represent entities
	 * changing over time, e.g. moving, growing, shrinking, etc.
	 * The ObjectContainer returns true only if it contains data about
	 * these changes. An ObjectManager returns false.
	 */
	@Override
	public boolean containsChangingObjects() {
		return false;
	}

	//----------------- notification about object changes---------------
	/**
	* An Object Manager may have a number of listeners of changes of the set
	* of visible Geo Objects and of the thematic data associated with them.
	* Eyamples of such listeners are VisManipulators and Linked Displays.
	* The listeners should implement the PropertyChangeListener interface.
	* They are notified about changes of properties "ObjectSet" and "ObjectData".
	* The ObjectManager, in its turn, is notified about these changes
	* by its GeoLayer.
	* To handle the list of listeners and notify them about changes of the
	* object set or object data, the Object Manager uses a PropertyChangeSupport.
	*/
	protected PropertyChangeSupport pcSupport = null;

	/**
	* A method from the ThematicDataSupplier interface.
	* Registeres a listener of changes of object set and object data. The
	* listener must implement the PropertyChangeListener interface.
	*/
	@Override
	public synchronized void addPropertyChangeListener(PropertyChangeListener l) {
		if (l == null)
			return;
		if (pcSupport == null) {
			pcSupport = new PropertyChangeSupport(this);
		}
		pcSupport.addPropertyChangeListener(l);
	}

	/**
	* A method from the ThematicDataSupplier interface.
	* Unregisteres a listener of changes of object set and object data.
	*/
	@Override
	public synchronized void removePropertyChangeListener(PropertyChangeListener l) {
		if (l == null || pcSupport == null)
			return;
		pcSupport.removePropertyChangeListener(l);
	}

	/**
	* An internal method used to notify all the listeners about changes of object
	* set and object data.
	*/
	protected void notifyPropertyChange(String propName, Object oldValue, Object newValue) {
		if (pcSupport == null)
			return;
		pcSupport.firePropertyChange(propName, oldValue, newValue);
	}

	/**
	* A method from the DataTreater interface.
	* Returns a vector of IDs of the attributes that are currently visualized
	* in the map
	*/
	@Override
	public Vector getAttributeList() {
		if (layer == null)
			return null;
		Vector list = null, list1 = null;
		Object vis = layer.getVisualizer();
		if (vis != null && (vis instanceof DataTreater)) {
			list = ((DataTreater) vis).getAttributeList();
		}
		vis = layer.getBackgroundVisualizer();
		if (vis != null && (vis instanceof DataTreater)) {
			list1 = ((DataTreater) vis).getAttributeList();
		}
		if (list1 == null)
			return list;
		if (list == null)
			return list1;
		for (int i = 0; i < list1.size(); i++) {
			list.addElement(list1.elementAt(i));
		}
		return list;
	}

	/**
	* A method from the DataTreater interface.
	* Replies whether it is linked to the data set (table) with the given identifier
	*/
	@Override
	public boolean isLinkedToDataSet(String setId) {
		if (layer == null || setId == null)
			return false;
		Object vis = layer.getVisualizer();
		if (vis != null && (vis instanceof DataTreater) && layer.getThematicData() != null)
			return setId.equals(layer.getThematicData().getContainerIdentifier());
		return false;
	}

	/**
	* A method from the DataTreater interface.
	* Returns a vector of colors used for representation of the attributes that
	* are currently visualized in the map. May return null if no attributes are
	* visualized or no colors are used.
	*/
	@Override
	public Vector getAttributeColors() {
		if (layer == null)
			return null;
		Vector list = null, list1 = null;
		Object vis = layer.getVisualizer(), vis1 = layer.getBackgroundVisualizer();
		if (vis != null && (vis instanceof DataTreater)) {
			list = ((DataTreater) vis).getAttributeList();
		}
		if (vis1 != null && (vis1 instanceof DataTreater)) {
			list1 = ((DataTreater) vis).getAttributeList();
		}
		if (list == null && list1 == null)
			return null;
		if (list == null)
			return ((DataTreater) vis1).getAttributeColors();
		if (list1 == null)
			return ((DataTreater) vis).getAttributeColors();
		Vector colors = ((DataTreater) vis).getAttributeColors(), colors1 = ((DataTreater) vis1).getAttributeColors();
		if (colors == null && colors1 == null)
			return null;
		Vector c = new Vector(list.size() + list1.size());
		for (int i = 0; i < list.size(); i++)
			if (colors != null && i <= colors.size()) {
				c.addElement(colors.elementAt(i));
			} else {
				c.addElement(null);
			}
		for (int i = 0; i < list1.size(); i++)
			if (colors1 != null && i <= colors1.size()) {
				c.addElement(colors1.elementAt(i));
			} else {
				c.addElement(null);
			}
		return c;
	}

	/**
	* A method from the DataViewInformer interface.
	* Checks whether the layer has any visualizer and whether
	* the visualizer, if exists, is a DataViewRegulator. If so, returns this
	* visualizer.
	*/
	@Override
	public DataViewRegulator getDataViewRegulator() {
		if (layer == null)
			return null;
		Object vis = layer.getVisualizer();
		if (vis != null && (vis instanceof DataViewRegulator))
			return (DataViewRegulator) vis;
		vis = layer.getBackgroundVisualizer();
		if (vis != null && (vis instanceof DataViewRegulator))
			return (DataViewRegulator) vis;
		return null;
	}

	/**
	* A method from the DataViewInformer interface.
	* Checks whether the layer has any visualizer and whether
	* the visualizer, if exists, is a TransformedDataPresenter. If so, returns this
	* visualizer.
	*/
	@Override
	public TransformedDataPresenter getTransformedDataPresenter() {
		if (layer == null)
			return null;
		Object vis = layer.getVisualizer();
		if (vis != null && (vis instanceof TransformedDataPresenter))
			return (TransformedDataPresenter) vis;
		vis = layer.getBackgroundVisualizer();
		if (vis != null && (vis instanceof TransformedDataPresenter))
			return (TransformedDataPresenter) vis;
		return null;
	}

//--------------- Listening to mouse events from MapDraw -------------------
//--------------- and highlighting/selection of objects --------------------
	/**
	* Sets the map, on which the objects are drawn. Registers the ObjectManager
	* as MapListener of this map.
	*/
	public void setMap(MapDraw mdraw) {
		if (map == mdraw)
			return;
		if (map != null) {
			map.removeMapListener(this);
		}
		map = mdraw;
		if (map != null) {
			map.addMapListener(this);
			map.addMapEventMeaning(DMouseEvent.mDrag, "select", "select objects");
		}
	}

	public MapDraw getMap() {
		return map;
	}

	/**
	* The ObjectManager sends the object events it generates to an
	* ObjectEventHandler (this may be a supervisor, an object event dispatcher,
	* a highlighter or other component implementing this interface).
	*/
	public void setObjectEventHandler(ObjectEventHandler handler) {
		objEvtHandler = handler;
	}

	/**
	* A method from the EventReceiver interface. Returns true if the
	* argument eventId equals to DMouseEvent.mExited, DMouseEvent.mMove,
	* DMouseEvent.mClicked, or DMouseEvent.mDClicked.
	*/
	@Override
	public boolean doesListenToEvent(String eventId) {
		return eventId != null
				&& (eventId.equals(DMouseEvent.mMove) || eventId.equals(DMouseEvent.mExited) || eventId.equals(DMouseEvent.mDrag) || eventId.equals(DMouseEvent.mClicked) || eventId.equals(DMouseEvent.mDClicked) || eventId
						.equals(DMouseEvent.mLongPressed));
	}

	/**
	* A method from the EventConsumer interface. Returns true if the
	* argument eventId equals to DMouseEvent.mDrag and the meaning is "select"
	*/
	@Override
	public boolean doesConsumeEvent(DEvent evt, String eventMeaning) {
		return evt != null && evt.getSource() == map && doesConsumeEvent(evt.getId(), eventMeaning);
	}

	/**
	* A method from the EventConsumer interface. Returns true if the
	* argument eventId equals to DMouseEvent.mDrag and the meaning is "select"
	*/
	@Override
	public boolean doesConsumeEvent(String evtType, String eventMeaning) {
		return evtType != null && evtType.equals(DMouseEvent.mDrag) && eventMeaning.equals("select");
	}

	/**
	* A method from the EventReceiver interface. If the event comes from
	* the map (is an instance of DMouseEvent), the ObjectManager transforms
	* the map event into an object event and forwards it to its Highlighter.
	*/
	@Override
	public void eventOccurred(DEvent evt) {
		if (objEvtHandler == null)
			return;
		if ((evt instanceof DMouseEvent) && evt.getSource() == map) {
			if (layer == null || supervisor == null)
				return;
			DMouseEvent mevt = (DMouseEvent) evt;
			ObjectEvent oevt = null;
			String mevtId = mevt.getId();
			Vector times = null;
			if (mevtId.equals(DMouseEvent.mExited)) {
				oevt = new ObjectEvent(this, ObjectEvent.point, evt.getSourceMouseEvent(), layer.getEntitySetIdentifier());
				//mouse exit from a map is equivalent to pointing in an object-free area
			} else if (mevtId.equals(DMouseEvent.mDrag)) {
				processMouseDragging(mevt);
			} else if (mevtId.equals(DMouseEvent.mMove) || mevtId.equals(DMouseEvent.mClicked) || mevtId.equals(DMouseEvent.mDClicked) || mevtId.equals(DMouseEvent.mLongPressed)) {
				//find the object(s) at the mouse cursor
				Vector obj = layer.findObjectsAt(mevt.getX(), mevt.getY(), map.getMapContext(), mevtId.equals(DMouseEvent.mMove));
				if (obj != null && obj.size() < 1) {
					obj = null;
				}
				//pointing or clicking in an object-free map area
				if (obj != null && layer.hasTimeReferences()) {
					times = new Vector(obj.size());
					boolean hasTime = false;
					for (int i = 0; i < obj.size(); i++) {
						GeoObject go = layer.findObjectById((String) obj.elementAt(i));
						if (go instanceof DMovingObject) {
							float x = map.getMapContext().absX(mevt.getX()), y = map.getMapContext().absY(mevt.getY());
							TimeFilter tf = null;
							if (layer instanceof DGeoLayer) {
								tf = ((DGeoLayer) layer).getTimeFilter();
							}
							TimeMoment t1 = null, t2 = null;
							if (tf != null && tf.areObjectsFiltered()) {
								t1 = tf.getFilterPeriodStart();
								t2 = tf.getFilterPeriodEnd();
							}
							DMovingObject mobj = (DMovingObject) go;
							int pIdx = mobj.getClosestPointTo(x, y, t1, t2);
							times.addElement(mobj.getPositionTime(pIdx));
						} else if (go instanceof DGeoObject) {
							times.addElement(((DGeoObject) go).getTimeReference());
						} else {
							times.addElement(null);
						}
						hasTime = hasTime || times.elementAt(times.size() - 1) != null;
					}
					if (!hasTime) {
						times = null;
					}
				}
				if (mevtId.equals(DMouseEvent.mMove)) {
					oevt = new ObjectEvent(this, ObjectEvent.point, evt.getSourceMouseEvent(), layer.getEntitySetIdentifier(), obj);
				} else if (mevtId.equals(DMouseEvent.mClicked)) {
					oevt = new ObjectEvent(this, ObjectEvent.click, evt.getSourceMouseEvent(), layer.getEntitySetIdentifier(), obj);
				} else if (mevtId.equals(DMouseEvent.mLongPressed)) {
					oevt = new ObjectEvent(this, ObjectEvent.select, evt.getSourceMouseEvent(), layer.getEntitySetIdentifier(), obj);
				} else {
					if (obj != null) { //the object was selected by the previous click
						//deselect it
						oevt = new ObjectEvent(this, ObjectEvent.click, evt.getSourceMouseEvent(), layer.getEntitySetIdentifier(), obj);
						objEvtHandler.processObjectEvent(oevt);
					}
					oevt = new ObjectEvent(this, ObjectEvent.dblClick, evt.getSourceMouseEvent(), layer.getEntitySetIdentifier(), obj);
				}
				if (times != null) {
					oevt.setTimeRefs(times);
				}
			}
			boolean timeSelection = mevtId.equals(DMouseEvent.mClicked) && mevt.getRightButtonPressed() && times != null && times.size() == 1 && supervisor != null;
			if (!timeSelection) {
				objEvtHandler.processObjectEvent(oevt);
			}
			boolean timeNotified = false;
			if (times != null && times.size() == 1 && supervisor != null) {
				TimeReference tr = (TimeReference) times.elementAt(0);
				if (tr.getValidFrom() != null) {
					if (timeSelection) {
						supervisor.notifyGlobalPropertyChange("time_moment_selection", tr.getValidFrom());
					} else {
						TimePositionNotifier tpn = new TimePositionNotifier();
						tpn.lastId = oevt.getObjectIdentifier(0);
						tpn.setMouseTime(tr.getValidFrom());
						supervisor.notifyGlobalPropertyChange("current_moment", tpn);
					}
					timeNotified = true;
				}
			}
			if (!timeNotified && layer.hasTimeReferences()) {
				TimePositionNotifier tpn = new TimePositionNotifier();
				supervisor.notifyGlobalPropertyChange("current_moment", tpn);
			}
		}
	}

	protected void processMouseDragging(DMouseEvent mevt) {
		if (objEvtHandler == null)
			return;
		Graphics g = map.getGraphics();
		if (g == null)
			return;
		if (lastHL != null && lastHL.size() > 0) {
			layer.hideHighlighting(g, map.getMapContext());
			layer.dehighlightAllObjects();
			lastHL.removeAllElements();
		}
		//erase previous frame
		int x0 = mevt.getDragStartX(), y0 = mevt.getDragStartY(), prx = mevt.getDragPrevX(), pry = mevt.getDragPrevY();
		if (prx >= 0 && pry >= 0) {
			drawFrame(g, x0, y0, prx, pry);
		}
		if (mevt.isDraggingFinished()) {
			// get location of the last mouse event occurred
			int x1 = mevt.getX();
			int y1 = mevt.getY();
			// if the last mouse event occurred outside map area: fix frame bounds
			java.awt.Rectangle viewPort = map.getMapContext().getViewportBounds();
			if (viewPort != null) {
				int xMin = viewPort.x, xMax = viewPort.x + viewPort.width;
				int yMin = viewPort.y, yMax = viewPort.y + viewPort.height;
				if (x1 < xMin) {
					x1 = xMin;
				}
				if (y1 < yMin) {
					y1 = yMin;
				}
				if (x1 >= xMax) {
					x1 = xMax - 1;
				}
				if (y1 >= yMax) {
					y1 = yMax - 1;
				}
			}
			//select objects fitting in the frame
			//System.out.println("x0="+x0+" y0="+y0+" x1="+x1+" y1="+y1);
			Vector obj = layer.findObjectsIn(x0, y0, x1, y1, map.getMapContext());
			ObjectEvent oevt = new ObjectEvent(this, ObjectEvent.frame, mevt.getSourceMouseEvent(), layer.getEntitySetIdentifier(), obj);
			objEvtHandler.processObjectEvent(oevt);
		} else {
			drawFrame(g, x0, y0, mevt.getX(), mevt.getY());
		}
		g.dispose();
	}

	/**
	* Requests the highlighter to clear highlighting of objects
	*/
	public void clearHighlight() {
		if (layer != null && supervisor != null) {
			Highlighter highlighter = supervisor.getHighlighter(layer.getEntitySetIdentifier());
			if (highlighter != null) {
				highlighter.clearHighlighting(this);
			}
		}
	}

	/**
	* Requests the highlighter to clear selection (durable highlighting) of objects
	*/
	public void clearSelection() {
		if (layer != null && supervisor != null) {
			Highlighter highlighter = supervisor.getHighlighter(layer.getEntitySetIdentifier());
			if (highlighter != null) {
				highlighter.clearSelection(this);
			}
		}
	}

	/**
	* A method from the HighlightListener interface.
	* Reaction to a change of the set of objects to be transiently
	* highlighted. The argument "source" is typically a reference to the
	* highlighter.
	*/
	@Override
	public synchronized void highlightSetChanged(Object source, String setId, Vector hlObj) {
		if (layer == null || !StringUtil.sameStrings(setId, layer.getEntitySetIdentifier()))
			return;
		if (map == null)
			return;
		Graphics g = map.getGraphics();
		if (g == null)
			return;
		if (lastHL != null && lastHL.size() > 0) {
			layer.hideHighlighting(g, map.getMapContext());
			layer.dehighlightAllObjects();
			lastHL.removeAllElements();
		}
		if (hlObj != null && hlObj.size() > 0) {//some objects are now highlighted
			if (lastHL == null) {
				lastHL = new Vector(20, 10);
			}
			for (int i = 0; i < hlObj.size(); i++)
				if (layer.setObjectHighlight((String) hlObj.elementAt(i), true)) {
					lastHL.addElement(hlObj.elementAt(i));
				}
			layer.showHighlighting(g, map.getMapContext());
		}
		//map.redraw();
	}

	/**
	* A method from the HighlightListener interface.
	* Reaction to a change of the set of objects to be selected (durably
	* highlighted). The argument "source" is typically a reference to the
	* highlighter.
	*/
	@Override
	public void selectSetChanged(Object source, String setId, Vector selObj) {
		if (layer == null || !StringUtil.sameStrings(setId, layer.getEntitySetIdentifier()))
			return;
		if (map == null)
			return;
		map.eraseAllMarks();
		Graphics g = map.getGraphics();
		if (lastSel == null) {
			lastSel = new Vector(20, 10);
		}
		if (lastHL != null && lastHL.size() > 0) {
			layer.hideHighlighting(g, map.getMapContext());
		}
		if (selObj == null || selObj.size() < 1) {//no objects are now selected
			//deselect all earlier selected objects
			if (lastSel.size() > 0) {
				map.restorePicture();
			}
			for (int i = 0; i < lastSel.size(); i++) {
				layer.selectObject((String) lastSel.elementAt(i), false, g, map.getMapContext());
			}
			lastSel.removeAllElements();
		} else {//some objects are now selected
			//deselect earlier selected objects that are no more selected
			boolean someDeselected = false;
			for (int i = 0; i < lastSel.size(); i++)
				if (!StringUtil.isStringInVectorIgnoreCase((String) lastSel.elementAt(i), selObj)) {
					layer.selectObject((String) lastSel.elementAt(i), false, g, map.getMapContext());
					someDeselected = true;
				}
			if (someDeselected) {
				map.restorePicture();
			}
			lastSel.removeAllElements();
			//select objects that have not been earlier selected
			for (int i = 0; i < selObj.size(); i++)
				if (!StringUtil.isStringInVectorIgnoreCase((String) selObj.elementAt(i), lastSel))
					if (layer.selectObject((String) selObj.elementAt(i), true, g, map.getMapContext())) {
						lastSel.addElement(selObj.elementAt(i));
					}
			if (lastSel.size() > 0 && someDeselected) {
				layer.drawSelectedObjects(g, map.getMapContext());
			}
		}
		if (lastHL != null && lastHL.size() > 0) {
			layer.showHighlighting(g, map.getMapContext());
		}
		if (g != null) {
			g.dispose();
		}
	}

	/**
	* Draws a frame in the map
	*/
	protected void drawFrame(Graphics gr, int x0, int y0, int x, int y) {
		if (x - x0 != 0 || y - y0 != 0) {
			gr.setColor(Color.yellow);
			gr.setXORMode(Color.lightGray);
			gr.drawLine(x0, y0, x, y0);
			gr.drawLine(x, y0, x, y);
			gr.drawLine(x, y, x0, y);
			gr.drawLine(x0, y, x0, y0);
			gr.setPaintMode();
		}
	}

	/**
	* Makes necessary operations for destroying, e.g. unregisters from
	* listening events.
	*/
	public void destroy() {
		setGeoLayer(null);
		setMap(null);
	}

	/**
	* Returns a unique identifier of the component
	* (used only internally, not shown to the user).
	* The identifier is used for explicit linking of producers and recipients of
	* object events.
	*/
	@Override
	public String getIdentifier() {
		return "ObjectManager_" + instanceN;
	}

	/**
	* Does nothing; needed for compliance with the ThematicDataSuplier interface.
	*/
	@Override
	public void setObjectFilter(ObjectFilter oFilter) {
	}

	/**
	* Does nothing; needed for compliance with the ThematicDataSuplier interface.
	*/
	@Override
	public void removeObjectFilter(ObjectFilter filter) {
	}

	/**
	* Reports whether the objects in the geolayer are temporally referenced.
	*/
	@Override
	public boolean hasTimeReferences() {
		if (layer == null)
			return false;
		return layer.hasTimeReferences();
	}

	@Override
	public ObjectFilter getObjectFilter() {
		return null;
	}

	/**
	 * This method is called after a transformation of the time references
	 * of the objects, e.g. from absolute to relative times. The ObjectContainer
	 * may need to change some of its internal settings.
	 */
	@Override
	public void timesHaveBeenTransformed() {
	}

	/**
	 * If the objects in this container are time-referenced, returns the earliest
	 * and the latest times among the time references; otherwise returns null.
	 */
	@Override
	public TimeReference getTimeSpan() {
		return null;
	}

	/**
	 * If the objects in this container are time-referenced, returns the earliest
	 * and the latest times among the original time references irrespective of the
	 * current transformation of the times; otherwise returns null.
	 */
	@Override
	public TimeReference getOriginalTimeSpan() {
		return null;
	}
}
