package spade.vis.dataview;

import java.awt.Frame;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import spade.analysis.system.Supervisor;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.PopupWindow;
import spade.lib.util.StringUtil;
import spade.vis.action.HighlightListener;
import spade.vis.action.Highlighter;
import spade.vis.action.ObjectEvent;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.DataTable;
import spade.vis.event.DEvent;
import spade.vis.event.EventReceiver;

class EventHandlerThread extends Thread {
	public static long popupDelay = 500; // in millis
	boolean mustStop = false;
	boolean isRunning = false;
	protected ShowRec popupSR = null;
	protected PopupWindow win = null;
	protected ObjectEvent oevt = null;
	/**
	 * Additional attributes to include in the popup record view
	 */
	protected Vector popupAddAttr = null;

	public EventHandlerThread(ShowRec popupSR, PopupWindow win, ObjectEvent oevt, Vector popupAddAttr) {
		this.popupSR = popupSR;
		this.win = win;
		this.oevt = oevt;
		this.popupAddAttr = popupAddAttr;
	}

	@Override
	public void run() {
		if (mustStop)
			return;
		if (!oevt.getType().equals(ObjectEvent.point) || oevt.getAffectedObjectCount() < 1 || oevt.getX() < 0 || oevt.getY() < 0)
			return;
		synchronized (this) {
			isRunning = true;
		}
		try {
			sleep(popupDelay);
		} catch (InterruptedException e) {
		}
		if (!mustStop) {
			processEvent();
		}
		synchronized (this) {
			isRunning = false;
		}
		if (mustStop) {
			win.dispose();
		}
	}

	protected void processEvent() {
		Vector av = oevt.getDataTreater().getAttributeList(), cv = oevt.getDataTreater().getAttributeColors();
		if (popupAddAttr == null || popupAddAttr.size() < 1) {
			popupSR.setShownAttrs(av, cv);
		} else {
			int avSize = (av != null) ? av.size() : 0;
			if (avSize < 1) {
				popupSR.setShownAttrs(popupAddAttr, null);
			} else {
				Vector aav = new Vector(avSize + popupAddAttr.size(), 10), acv = new Vector(avSize + popupAddAttr.size(), 10);
				for (int i = 0; i < avSize; i++) {
					aav.addElement(av.elementAt(i));
					acv.addElement((cv != null && cv.size() > i) ? cv.elementAt(i) : null);
				}
				for (int i = 0; i < popupAddAttr.size(); i++)
					if (!aav.contains(popupAddAttr.elementAt(i))) {
						aav.addElement(popupAddAttr.elementAt(i));
						acv.addElement(null);
					}
				popupSR.setShownAttrs(aav, acv);
			}
		}
		if (mustStop)
			return;
		popupSR.showObjects(oevt.getAffectedObjects(), oevt.getTimeRefs());
		if (mustStop)
			return;
		showRecordPopup(oevt.getX(), oevt.getY());
	}

	/**
	* Displays the popup window with the content of the record
	*/
	protected void showRecordPopup(int x, int y) {
		if (mustStop)
			return;
		if (popupSR.hasContent) {
			win.setLocation(x, y);
			win.setPosition();
			win.setContent(popupSR);
			popupSR.setExtContainer(win);
			if (!mustStop) {
				win.show();
//        win.toFront(); // IDbug - this call sometimes causes VM crash
				//System.out.println("object popup: visible="+win.isShowing());
			}
		}
	}
}

public class ShowRecManager implements EventReceiver, PropertyChangeListener, HighlightListener, Destroyable {
	public static final int showDisplayedAttr = 1, showSelectedAttr = 2, firstMode = showDisplayedAttr, lastMode = showSelectedAttr;

	protected AttributeDataPortion dTable = null;

	public boolean isPopupEnabled = false;
	protected static PopupWindow oiw = null;
	protected static EventHandlerThread thr = null;
	protected ShowRec popupSR = null, persistSR = null;
	protected int maxPopupLines = 30; //-1 will mean unlimited number
	/**
	* Settings for the persistent record show
	*/
	public int mode = showDisplayedAttr;
	public boolean addNew = false, addParents = false;
	/**
	 * Attributes to show in the persistent record view
	 */
	protected Vector shownAttr = null;
	/**
	 * Additional attributes to include in the popup record view
	 */
	protected Vector popupAddAttr = null;

	/**
	* The supervisor helps in propagating object selection events the
	* record show component should listen to
	*/
	protected Supervisor supervisor = null;
	/**
	* Indicates "destroyed" state. Initially is false.
	*/
	protected boolean destroyed = false;

	public ShowRecManager(Supervisor sup, AttributeDataPortion dTable) {
		supervisor = sup;
		this.dTable = dTable;
		if (supervisor != null) {
			supervisor.addPropertyChangeListener(this);
			supervisor.registerObjectEventReceiver(this);
		}
	}

	public Supervisor getSupervisor() {
		return supervisor;
	}

	public AttributeDataPortion getDataTable() {
		return dTable;
	}

	protected ShowRec constructShowRecInstance(boolean persistent) {
		if (dTable == null)
			return null;
		ShowRec sRec = new ShowRec(dTable);
		sRec.setLinesLimit((persistent) ? -1 : maxPopupLines);
		return sRec;
	}

	public void setEnabled(boolean value) {
		isPopupEnabled = value;
		if (isPopupEnabled) {
			makePopup();
		} else {
			popupSR = null;
		}
	}

	public boolean getEnabled() {
		return isPopupEnabled;
	}

	public void setLinesLimit(int limit) {
		if (limit != maxPopupLines) {
			maxPopupLines = limit;
			if (popupSR != null) {
				popupSR.setLinesLimit(limit);
			}
		}
	}

	public int getLinesLimit() {
		return maxPopupLines;
	}

	protected void makePopup() {
		if (popupSR == null) {
			popupSR = constructShowRecInstance(false);
		}
		boolean secure_flag = true;
	}

	/**
	* Hides the popup window with the content of the record
	*/
	protected void hideRecordPopup() {
		if (thr != null) {
			thr.mustStop = true;
		}
		if (oiw != null && oiw.isShowing()) {
			oiw.dispose();
		}
		oiw = null;
	}

	/**
	* The EventReceiver answers whether it is interested in getting the specified
	* kind of events.
	*/
	@Override
	public boolean doesListenToEvent(String eventId) {
		return dTable != null && dTable.hasData() && eventId != null && isPopupEnabled;
	}

	/**
	* When an object pointing event occurs, the popup window should be moved to
	* the origin of the event.
	* When an object clicking event occurs, the persistent record show should
	* display data about the selected objects
	*/
	@Override
	public void eventOccurred(DEvent evt) {
		if (dTable == null || !dTable.hasData() || !isPopupEnabled)
			return;
		if ((evt instanceof ObjectEvent) && ((ObjectEvent) evt).getDataTreater() != null)
			if (evt.getId().equals(ObjectEvent.point)) {
				ObjectEvent oe = (ObjectEvent) evt;
				if (!StringUtil.sameStrings(oe.getSetIdentifier(), dTable.getEntitySetIdentifier()))
					return;
				if (oe.getAffectedObjectCount() > 0) {
					Vector attr = oe.getDataTreater().getAttributeList();
					if (attr != null && attr.size() > 0) {
						for (int i = 0; i < attr.size(); i++)
							if (attr.elementAt(i) != null)
								if (dTable.getAttrIndex((String) attr.elementAt(i)) < 0)
									return;
					}
				}
				hideRecordPopup();
				DataViewRegulator reg = null;
				if (oe.getDataTreater() instanceof DataViewRegulator) {
					reg = (DataViewRegulator) oe.getDataTreater();
				} else if (evt.getSource() instanceof DataViewInformer) {
					reg = ((DataViewInformer) evt.getSource()).getDataViewRegulator();
				}
				popupSR.setDataViewRegulator(reg);
				TransformedDataPresenter tPres = null;
				if (oe.getDataTreater() instanceof TransformedDataPresenter) {
					tPres = (TransformedDataPresenter) oe.getDataTreater();
				} else if (evt.getSource() instanceof DataViewInformer) {
					tPres = ((DataViewInformer) evt.getSource()).getTransformedDataPresenter();
				}
				popupSR.setTransformedDataPresenter(tPres);
				Frame fr = null;
				if (evt.getSourceMouseEvent() != null) {
					fr = CManager.getAnyFrame(evt.getSourceMouseEvent().getComponent());
				}
				if (fr == null) {
					fr = CManager.getAnyFrame();
				}
				oiw = new PopupWindow(fr);
				oiw.setEnabled(false);
				thr = new EventHandlerThread(popupSR, oiw, oe, popupAddAttr);
				thr.start();
			} else {
				hideRecordPopup();
			}
	}

	/**
	* Notification about change of the set of objects to be transiently
	* highlighted. The ShowRecManager does not process this event.
	*/
	@Override
	public void highlightSetChanged(Object source, String setId, Vector highlighted) {
	}

	/**
	* Notification about change of the set of objects to be selected (durably
	* highlighted). The argument setId is the identifier of the set the
	* highlighted objects belong to (e.g. map layer or table).
	* The argument selected is a vector of identifiers of
	* currently selected objects.
	*/
	@Override
	public void selectSetChanged(Object source, String setId, Vector selected) {
		if (dTable == null || !dTable.hasData())
			return;
		if (persistSR == null || setId == null || !setId.equals(dTable.getEntitySetIdentifier()))
			return;
		DataViewRegulator reg = null;
		if (source instanceof DataViewRegulator) {
			reg = (DataViewRegulator) source;
		} else if (source instanceof DataViewInformer) {
			reg = ((DataViewInformer) source).getDataViewRegulator();
		}
		persistSR.setDataViewRegulator(reg);
		TransformedDataPresenter tPres = null;
		if (source instanceof TransformedDataPresenter) {
			tPres = (TransformedDataPresenter) source;
		} else if (source instanceof DataViewInformer) {
			tPres = ((DataViewInformer) source).getTransformedDataPresenter();
		}
		popupSR.setTransformedDataPresenter(tPres);
		persistSR.showObjects(selected);
	}

	/**
	* Returns the list of attributes that are shown persistently
	*/
	public Vector getPersistentlyShownAttrs() {
		return shownAttr;
	}

	/**
	 * Returns the additional attributes included in the popup record view
	 */
	public Vector getPopupAddAttrs() {
		return popupAddAttr;
	}

	/**
	* Sets the list of attributes to be shown persistently
	*/
	public void setPersistentlyShownAttrs(Vector attr) {
		shownAttr = attr;
		if (mode == showSelectedAttr && persistSR != null) {
			sendAttrsToPersistentView(attr);
		}
	}

	/**
	 * Sets the list of additional attributes to include in the popup record view
	 */
	public void setPopupAddAttrs(Vector attr) {
		popupAddAttr = attr;
	}

	/**
	* Sends the list of attributes to the persistent record view
	*/
	protected void sendAttrsToPersistentView(Vector attr) {
		Vector colors = null;
		if (attr != null && attr.size() > 0) {
			colors = new Vector(attr.size(), 1);
			for (int i = 0; i < attr.size(); i++) {
				colors.addElement(supervisor.getColorForAttribute((String) attr.elementAt(i)));
			}
		}
		persistSR.setShownAttrs(attr, colors);
	}

	/**
	* Creates a component (instance of ShowRec) for persistent viewing of
	* attribute values associated with selected objects
	*/
	public ShowRec getPersistentRecordShow() {
		if (persistSR == null) {
			persistSR = constructShowRecInstance(true);
			persistSR.addPropertyChangeListener(this);
			Vector attr = shownAttr;
			if (mode == showDisplayedAttr) {
				attr = getRelevantDisplayedAttributes();
			}
			sendAttrsToPersistentView(attr);
			if (supervisor != null) {
				Highlighter hl = supervisor.getHighlighter(dTable.getEntitySetIdentifier());
				if (hl != null) {
					persistSR.showObjects(hl.getSelectedObjects());
				}
				supervisor.registerHighlightListener(this, dTable.getEntitySetIdentifier());
			}
		}
		return persistSR;
	}

	/**
	* Destroys the component for persistent viewing of attribute values.
	* Returns the old reference (in order to be able to remove the component
	* from where it has been placed)
	*/
	public ShowRec destroyPersistentRecordShow() {
		ShowRec srOld = persistSR;
		if (persistSR != null) {
			persistSR.removePropertyChangeListener(this);
			if (supervisor != null) {
				supervisor.removeHighlightListener(this, dTable.getEntitySetIdentifier());
			}
			persistSR.destroy();
			persistSR = null;
		}
		return srOld;
	}

	/**
	* Replies whether a persistent recors viewer currently exists
	*/
	public boolean hasPersistentRecordView() {
		return persistSR != null;
	}

	/**
	* Sets the mode: which attributes are shown in the persistent record view
	* (one of the constants showDisplayedAttr=1, showSelectedAttr=2)
	*/
	public void setShowAttrMode(int mode) {
		if (mode < firstMode || mode > lastMode)
			return;
		this.mode = mode;
		if (persistSR == null)
			return;
		switch (mode) {
		case showDisplayedAttr:
			if (supervisor != null) {
				sendAttrsToPersistentView(getRelevantDisplayedAttributes());
			}
			break;
		case showSelectedAttr:
			sendAttrsToPersistentView(shownAttr);
			break;
		}
	}

	public int getShowAttrMode() {
		return mode;
	}

	public void addAttrToShow(String attrId, boolean showImmediately) {
		if (attrId == null)
			return;
		if (shownAttr == null) {
			shownAttr = new Vector(20, 10);
		}
		if (shownAttr.contains(attrId))
			return;
		shownAttr.addElement(attrId);
		if (addParents) {
			Vector p = getParents(attrId);
			if (p != null) {
				for (int i = 0; i < p.size(); i++) {
					if (p.elementAt(i) != null) {
						attrId = p.elementAt(i).toString();
					} else {
						attrId = null;
					}
					if (attrId != null) {
						if (shownAttr.indexOf(attrId) < 0) {
							shownAttr.addElement(attrId); // add to ShowRec
						}
					}
				}
			}
		}
		if (showImmediately && mode == showSelectedAttr && persistSR != null) {
			sendAttrsToPersistentView(shownAttr);
		}
	}

	public void removeShownAttr(String attrId, boolean showImmediately) {
		if (attrId == null || shownAttr == null || !shownAttr.contains(attrId))
			return;
		shownAttr.removeElement(attrId);
		if (showImmediately && mode == showSelectedAttr && persistSR != null) {
			sendAttrsToPersistentView(shownAttr);
		}
	}

	public Vector getParents(Vector aList) {
		if (aList == null || dTable == null || !(dTable instanceof DataTable))
			return null;
		String attrId = null;
		Vector vParents = null;
		DataTable dt = (DataTable) dTable;
		Attribute a = null;
		for (int i = 0; i < aList.size(); i++) {
			if (aList.elementAt(i) != null) {
				attrId = aList.elementAt(i).toString();
			} else {
				attrId = null;
			}
			if (attrId != null) {
				if (vParents == null) {
					vParents = new Vector(2, 2);
				}
				a = dt.getAttribute(dt.getAttrIndex(attrId));
				for (int j = 0; j < a.getDerivedFromSize(); j++) {
					vParents.addElement(a.getDerivedFromID(j));
				}
			}
		}
		if (vParents != null && vParents.size() > 0) {
			String parentId = null;
			for (int i = 0; i < vParents.size() - 1; i++) {
				parentId = vParents.elementAt(i).toString();
				if (parentId == null) {
					continue;
				}
				for (int j = i + 1; j < vParents.size(); j++)
					if (parentId.equalsIgnoreCase(vParents.elementAt(j).toString())) {
						vParents.removeElementAt(j);
					}
			}
		}
		return vParents;
	}

	public Vector getParents(String attrId) {
		if (attrId == null || dTable == null || !(dTable instanceof DataTable))
			return null;
		Vector vParents = null;
		DataTable dt = (DataTable) dTable;
		Attribute a = null;
		if (attrId != null) {
			a = dt.getAttribute(dt.getAttrIndex(attrId));
			vParents = new Vector(2, 2);
			for (int j = 0; j < a.getDerivedFromSize(); j++) {
				vParents.addElement(a.getDerivedFromID(j));
			}
		}
		return vParents;
	}

	protected Vector getRelevantDisplayedAttributes() {
		if (dTable == null || supervisor == null)
			return null;
		Vector vAttrs = supervisor.getAllPresentedAttributes();
		//remove the attributes that are absent in the table
		if (vAttrs != null && vAttrs.size() > 0) {
			for (int i = vAttrs.size() - 1; i >= 0; i--)
				if (dTable.getAttrIndex((String) vAttrs.elementAt(i)) < 0) {
					vAttrs.removeElementAt(i);
				}
			if (vAttrs.size() < 1) {
				vAttrs = null;
			}
		}
		return vAttrs;
	}

	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getSource() == persistSR && e.getPropertyName().equals("destroy")) {
			destroyPersistentRecordShow();
		} else if (e.getPropertyName().equals(Supervisor.eventDisplayedAttrs) && persistSR != null && supervisor != null) {
			Vector displayed = getRelevantDisplayedAttributes();
			if (mode == showDisplayedAttr) {
				sendAttrsToPersistentView(displayed);
			} else if (displayed != null && displayed.size() > 0) {
				boolean changed = false;
				for (int i = 0; i < displayed.size(); i++)
					if (shownAttr == null || !shownAttr.contains(displayed.elementAt(i))) {
						addAttrToShow((String) displayed.elementAt(i), false);
						changed = true;
					}
				if (changed && persistSR != null) {
					sendAttrsToPersistentView(shownAttr);
				}
			}
		} else if (e.getPropertyName().equals(Supervisor.eventAttrColors) && persistSR != null) {
			if (mode == showDisplayedAttr) {
				sendAttrsToPersistentView(getRelevantDisplayedAttributes());
			} else {
				sendAttrsToPersistentView(shownAttr);
			}
		}
	}

	/**
	* Returns a unique identifier of the component
	* (used only internally, not shown to the user).
	* The identifier is used for explicit linking of producers and recipients of
	* object events.
	*/
	@Override
	public String getIdentifier() {
		return "Record_Show_" + dTable.getEntitySetIdentifier();
	}

	/**
	* Removes itself from listeners of various events
	*/
	@Override
	public void destroy() {
		destroyPersistentRecordShow();
		if (supervisor != null) {
			supervisor.removePropertyChangeListener(this);
			supervisor.removeObjectEventReceiver(this);
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
