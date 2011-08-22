package core;

import java.applet.Applet;
import java.awt.Component;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.plot.QueryOrSearchTool;
import spade.analysis.system.Supervisor;
import spade.lib.basicwin.Destroyable;
import spade.lib.lang.Language;
import spade.vis.action.HighlightListener;
import spade.vis.database.DataItem;
import spade.vis.database.ObjectContainer;
import spade.vis.database.SpatialDataItem;
import spade.vis.geometry.RealRectangle;

/**
* Processes object selection events and puts information about selected objects
* into an external HTML form using Javascript functions.
*/
public class ObjSelectorHTML implements HighlightListener, Destroyable {
	static ResourceBundle res = Language.getTextResource("core.Res");
	/**
	*  Used for communication with javascript objects
	*/
	protected FormFiller formFiller = null;
	/**
	* Vector of currenly selected objects. May be empty.
	*/
	protected Vector objects = new Vector(10, 5);
	/**
	* The identifier of the object set, in which objects are selected.
	*/
	protected String objSetId = null;
	/**
	* A container with the objects that may be selected. May be, in principle,
	* null. If the container is available, it may be used for getting object
	* names in addition to object identifiers.
	*/
	protected ObjectContainer objCont = null;
	/**
	* The supervisor propagates object selection events.
	*/
	protected Supervisor supervisor = null;
	/**
	* Indicates whether the bounding rectangle of the selection must be shown in
	* the HTML form or not
	*/
	protected boolean showBounds = false;
	/**
	* Indicates whether is destroyed or not
	*/
	protected boolean destroyed = false;

	/**
	* Sets the identifier of the object set, in which objects will be selected.
	*/
	public void setObjectSetId(String id) {
		if (supervisor != null && objSetId != null) {
			if (objSetId.equalsIgnoreCase(id))
				return;
			supervisor.removeHighlightListener(this, objSetId);
		}
		objSetId = id;
		if (supervisor != null && objSetId != null) {
			supervisor.registerHighlightListener(this, objSetId);
		}
	}

	/**
	* Returns the identifier of the object set, in which objects will be selected.
	*/
	public String getObjectSetId() {
		return objSetId;
	}

	/**
	* Sets the container with the objects that may be selected. The container
	* is, in principle, not strictly required. If the container is available, it
	* may be used for getting object names in addition to object identifiers.
	*/
	public void setObjectContainer(ObjectContainer cont) {
		objCont = cont;
		if (objCont != null) {
			setObjectSetId(objCont.getEntitySetIdentifier());
		}
	}

	/**
	* Sets the supervisor, which propagates object selection events.
	*/
	public void setSupervisor(Supervisor sup) {
		supervisor = sup;
		if (supervisor != null && objSetId != null) {
			supervisor.registerHighlightListener(this, objSetId);
		}
	}

	/**
	* Indicates whether the bounding rectangle of the selection must be shown in
	* the HTML form or not
	*/
	public void setShowBounds(boolean show) {
		showBounds = show;
	}

	public void prepareToWork() {
		checkMakeObjectList();
		initLinkToHTML();
	}

	/**
	* Creates a list of the names of the areas to select from and puts it in the
	* map window.
	*/
	protected void checkMakeObjectList() {
		if (supervisor == null || objCont == null)
			return;
		if (!supervisor.getSystemSettings().checkParameterValue("Enable_Object_List", "true"))
			return;
		if (objCont.getObjectCount() < 1) {
			objCont.loadData();
			if (objCont.getObjectCount() < 1)
				return;
		}
		try {
			Object tool = Class.forName("spade.analysis.plot.ObjectList").newInstance();
			if (tool != null && (tool instanceof QueryOrSearchTool)) {
				QueryOrSearchTool qst = (QueryOrSearchTool) tool;
				qst.setObjectContainer(objCont);
				qst.setSupervisor(supervisor);
				if (!qst.construct()) {
					System.out.println(qst.getErrorMessage());
				} else {
					Component c = (Component) qst;
					c.setName(res.getString("Areas"));
					supervisor.getUI().placeComponent(c);
				}
			}
		} catch (Exception e) {
			System.out.println("The class spade.analysis.plot.ObjectList is absent in the system configuration");
		}
	}

	protected void initLinkToHTML() {
		//System.out.println("Trying to link to HTML page");
		Object objApplet = supervisor.getSystemSettings().getParameter("APPLET");
		if (objApplet == null || !(objApplet instanceof Applet)) {
			System.out.println("ERROR: No applet found!!!");
			return;
		}
		try {
			formFiller = (FormFiller) Class.forName("core.HTMLFormFiller").newInstance();
		} catch (Throwable ex) {
			System.out.println("No class supporting an access to HTML forms found!!!");
		}
		if (formFiller == null)
			return;
		formFiller.setApplet((Applet) objApplet);
		if (!formFiller.initLinkToForm("document", "search-form")) {
			formFiller = null;
			return;
		}
		refreshFields();
	}

	protected void showVisibleTerrBounds() {
		if (showBounds && supervisor != null && supervisor.getUI() != null && supervisor.getUI().getCurrentMapViewer() != null && supervisor.getUI().getCurrentMapViewer().getMapDrawer() != null
				&& supervisor.getUI().getCurrentMapViewer().getMapDrawer().getMapContext() != null) {
			setGeometryFields(supervisor.getUI().getCurrentMapViewer().getMapDrawer().getMapContext().getVisibleTerritory());
		}
	}

	public void refreshFields() {
		setField("selection", "no objects selected");
		showVisibleTerrBounds();
	}

	public void clearFields() {
		setField("selection", "no objects selected");
		if (showBounds) {
			setField("west-c", "");
			setField("east-c", "");
			setField("south-c", "");
			setField("north-c", "");
		}
	}

	public void setGeometryFields(RealRectangle rr) {
		if (!showBounds)
			return;
		//System.out.println(rr);
		if (rr == null) {
			clearFields();
			return;
		}
		setField("west-c", Float.toString(rr.rx1));
		setField("east-c", Float.toString(rr.rx2));
		setField("south-c", Float.toString(rr.ry2));
		setField("north-c", Float.toString(rr.ry1));
	}

	public void setField(String field, String value) {
		if (formFiller != null) {
			formFiller.setField(field, value);
		} else {
			System.out.println("===> FORM: field " + field);
			System.out.println(value);
			return;
		}
	}

	/**
	 * Notification about change of the set of objects to be transiently
	 * highlighted. This event is ignored.
	 */
	@Override
	public void highlightSetChanged(Object source, String setId, Vector highlighted) {
	}

	/**
	* Notification about change of the set of objects to be selected (durably
	* highlighted). The argument setId is the identifier of the set the
	* highlighted objects belong to (e.g. map layer or table).
	* The argument selected is a vector of identifiers of currently selected
	* objects.
	*/
	@Override
	public void selectSetChanged(Object source, String setId, Vector selected) {
		processSelection(selected);
	}

	/**
	* Puts identifiers and names of the selected objects in the corresponding
	* field of the HTML form.
	* @param objList Vector - the selected objects
	*/
	protected void processSelection(Vector objList) {
		if (objList == null) {
			//System.out.println("objList==null, no objects selected");
			clearFields();
			return;
		}
		//System.out.println("Selected:");
		// for (int i=0; i<objList.size(); i++) System.out.println((String)objList.elementAt(i));
		objects = (Vector) objList.clone();
		if (objects.size() < 1) {
			clearFields();
			return;
		}
		String selected = "";
		RealRectangle rrSelected = null;
		for (int j = 0; j < objects.size(); j++) {
			String sId = (String) objects.elementAt(j);
			String sName = null;
			if (objCont != null) {
				int objectN = objCont.getObjectIndex(sId);
				if (objectN >= 0) {
					DataItem dit = objCont.getObjectData(objectN);
					if (dit != null) {
						sName = dit.getName();
						if (showBounds && (dit instanceof SpatialDataItem)) {
							SpatialDataItem spdit = (SpatialDataItem) dit;
							if (spdit.getGeometry() != null) {
								float rr[] = spdit.getGeometry().getBoundRect();
								if (rr != null) {
									if (rrSelected == null) {
										rrSelected = new RealRectangle(rr);
									} else {
										if (rrSelected.rx1 > rr[0]) {
											rrSelected.rx1 = rr[0];
										}
										if (rrSelected.ry1 > rr[1]) {
											rrSelected.ry1 = rr[1];
										}
										if (rrSelected.rx2 > rr[2]) {
											rrSelected.rx2 = rr[2];
										}
										if (rrSelected.ry2 > rr[3]) {
											rrSelected.ry2 = rr[3];
										}
									}
								}
							}
						}
					}
				}
			}
			if (sName == null || sName.length() < 1) {
				selected += (" " + sId + "\n");
			} else {
				selected += (" " + sId + " " + sName + "\n");
			}
		}
		setField("selection", selected);
		if (showBounds) {
			setGeometryFields(rrSelected);
		}
	}

	/**
	* Makes necessary operations for destroying, e.g. unregisters from
	* listening highlighting and other events.
	*/
	@Override
	public void destroy() {
		if (isDestroyed())
			return;
		if (supervisor != null && objSetId != null) {
			supervisor.removeHighlightListener(this, objSetId);
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

	protected void showMessage(String msg, boolean error) {
		if (supervisor != null && supervisor.getUI() != null) {
			supervisor.getUI().showMessage(msg, error);
		}
	}
}
