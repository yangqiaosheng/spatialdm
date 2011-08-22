package spade.vis.mapvis;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.classification.ObjectColorer;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.HotSpot;
import spade.lib.basicwin.Icons;
import spade.lib.basicwin.Metrics;
import spade.lib.basicwin.StringInRectangle;
import spade.lib.lang.Language;
import spade.vis.database.DataItem;
import spade.vis.map.MapContext;
import spade.vis.spec.ToolSpec;

/**
* An implementation of Visualizer: specifies how to present thematic data in
* a map. This is the class to be extended by all classes realizing
* various presentation methods such as painting or bar charts.
* Visualizer implements the LegendDrawer interface. This means that it should
* be able to draw the part of the legend explaining this presentation method.
*/

public abstract class BaseVisualizer implements Visualizer, Destroyable, ActionListener {
	static ResourceBundle res = Language.getTextResource("spade.vis.mapvis.Res");
	/**
	* The vector of listeners of visualization changes.
	*/
	protected Vector visList = null;
	/**
	* ID of this visualization
	*/
	protected String visId = null;
	/**
	* The name of this visualization
	*/
	protected String visName = null;
	/**
	* The identifier of the table with data this visualizer represents (if any)
	*/
	protected String tableId = null;
	/**
	* The error message
	*/
	protected String err = null;
	/**
	* This is usually the identifier of the map window in which this visualizer
	* is used. The location is important for correct restoring of tool states.
	*/
	protected String location = null;
	/**
	* Indicates the "enabled" or  "disabled" state of the visualizer. If a
	* visualizer is disabled, the geographical layer does not use it for drawing
	* its objects but uses instead the drawing parameters common for all objects.
	*/
	protected boolean enabled = true;
	/**
	* The controller of selective drawing, i.e. when the visualizer
	* generates representations not for all objects but only for selected ones.
	* The presence of the controller is required for sign drawers, for other
	* visualizers it is optional.
	*/
	protected SelectiveDrawingController sdController = null;
	/**
	* The size of the checkbox for enabling and disabling this visualizer
	*/
	protected int switchSize = 0;
	/**
	* Used for the reaction to clicking on the checkbox
	*/
	protected HotSpot hsp = null;

	protected boolean destroyed = false;
	/**
	* As a SaveableTool, a visualizer may be registered somewhere and, hence, must
	* notify the component where it is registered about its destroying. This vector
	* contains the listeners to be notified about destroying of the visualizer.
	*/
	protected Vector destroyListeners = null;

//-------------- methods for notification about visualization changes ---------
	protected PropertyChangeSupport pcSupport = null;

	@Override
	public void addVisChangeListener(PropertyChangeListener l) {
		if (l == null)
			return;
		if (pcSupport == null) {
			pcSupport = new PropertyChangeSupport(this);
		}
		pcSupport.addPropertyChangeListener(l);
	}

	@Override
	public void removeVisChangeListener(PropertyChangeListener l) {
		if (l == null || pcSupport == null)
			return;
		pcSupport.removePropertyChangeListener(l);
	}

	@Override
	public void notifyVisChange() {
		if (!enabled || pcSupport == null)
			return;
		pcSupport.firePropertyChange("Visualization", null, null);
	}

//---------END-- methods for notification about visualization changes ---------
	/**
	* Returns the controller of selective sign drawing (i.e. when the visualizer
	* generates representations not for all objects but only for selected ones).
	*/
	public SelectiveDrawingController getSelectiveDrawingController() {
		if (sdController == null) {
			sdController = new SelectiveDrawingController();
			sdController.setVisualizer(this);
		}
		return sdController;
	}

	/**
	* Sets the identifier of the table this Visualizer is linked with (if any).
	* A Visualizer may be used to represent data from a table. In this case it may
	* keep the identifier of the table.
	*/
	@Override
	public void setTableIdentifier(String tblId) {
		tableId = tblId;
	}

	/**
	* Returns the identifier of the table this Visualizer is linked with (if any).
	* A Visualizer may be used to represent data from a table. In this case it may
	* keep the identifier of the table.
	*/
	@Override
	public String getTableIdentifier() {
		return tableId;
	}

	/**
	* Here the Visualizer sets its parameters. This method should be redefined
	* in descendants.
	*/
	public abstract void setup();

	/**
	* "Disables" or "enables" the visualizer. If a visualizer is
	* disabled, the geographical layer does not use it for drawing its objects but
	* uses instead the drawing parameters common for all objects.
	*/
	@Override
	public void setEnabled(boolean value) {
		if (value != enabled) {
			enabled = value;
			if (pcSupport != null) {
				pcSupport.firePropertyChange("Visualization", null, null);
			}
		}
	}

	/**
	* Informs about the "enabled" or  "disabled" state of the visualizer. If a
	* visualizer is disabled, the geographical layer does not use it for drawing
	* its objects but uses instead the drawing parameters common for all objects.
	*/
	@Override
	public boolean isEnabled() {
		return enabled;
	}

	//------------------ main functionality ------------------------------
	/**
	* This method is called by a GeoObject. The GeoObject sends its DataItem
	* to the Visualizer, and the Visualizer returns an Object specifying the
	* presentation of data contained in this DataItem. This may be, in
	* particular, Color or Diagram. Typically the argument DataItem is
	* a ThematicDataItem, although in some cases (e.g. manual classification)
	* this is not required.
	*/
	@Override
	public abstract Object getPresentation(DataItem dit, MapContext mc);

	/**
	* This method informs whether the Visualizer produces diagrams.
	* This is important for defining the order of drawing of GeoLayers on the
	* map: the diagrams should be drawn on top of all geography.
	*/
	@Override
	public abstract boolean isDiagramPresentation();

	/**
	* The method from the LegendDrawer interface.
	* Draws the common part of the legend irrespective of the presentation method
	* (e.g. the name of the map), then calls drawMethodSpecificLegend(...)
	*/
	@Override
	public Rectangle drawLegend(Component c, Graphics g, int startY, int leftMarg, int prefW) {
		if (!enabled)
			return drawReducedLegend(c, g, startY, leftMarg, prefW);
		drawCheckbox(c, g, startY, leftMarg);
		int w = switchSize + Metrics.mm(), y = startY;
		g.setColor(Color.black);
		String name = getVisualizationName();
		if (name != null) {
			Point p = StringInRectangle.drawText(g, res.getString("vis_method") + ": " + name, leftMarg + w, y, prefW - w, false);
			y = p.y;
			w = p.x - leftMarg;
		}
		if (y < startY + switchSize + Metrics.mm()) {
			y = startY + switchSize + Metrics.mm();
		}
		Rectangle r = drawMethodSpecificLegend(g, y, leftMarg, prefW);
		if (r != null) {
			if (r.width < w) {
				r.width = w;
			}
			r.height += r.y - startY;
			r.y = startY;
			return r;
		}
		return new Rectangle(leftMarg, startY, w, y - startY);
	}

	/**
	* Draws a checkbox in the legend indicating whether this visualizer is enabled
	* or disabled.
	*/
	protected void drawCheckbox(Component c, Graphics g, int startY, int leftMarg) {
		if (switchSize <= 0) {
			switchSize = Metrics.mm() * 4;
		}
		g.setColor(Color.gray);
		if (enabled) {
			Icons.drawChecked(g, leftMarg, startY, switchSize, switchSize);
		} else {
			Icons.drawUnchecked(g, leftMarg, startY, switchSize, switchSize);
		}
		if (hsp != null) {
			hsp.setSize(0, 0);
		}
		if (c != null) {
			if (hsp == null || !c.equals(hsp.getOwner())) {
				hsp = new HotSpot(c);
				hsp.addActionListener(this);
			}
			hsp.setLocation(leftMarg, startY);
			hsp.setSize(switchSize, switchSize);
			if (enabled) {
				hsp.setActionCommand("disable");
			} else {
				hsp.setActionCommand("enable");
			}
		}
	}

	/**
	* Returns the size of the "checkbox" used to enable and disable the visualizer
	*/
	@Override
	public int getSwitchSize() {
		if (hsp == null)
			return 0;
		return switchSize;
	}

	/**
	* Draws a "reduced" legend if the visualizer is disabled.
	*/
	protected Rectangle drawReducedLegend(Component c, Graphics g, int startY, int leftMarg, int prefW) {
		drawCheckbox(c, g, startY, leftMarg);
		int w = switchSize + Metrics.mm();
		int iconH = Metrics.mm() * 5, iconW = Metrics.mm() * 8;
		drawIcon(g, leftMarg + w, startY, iconW, iconH);
		w += iconW + Metrics.mm();
		g.setColor(Color.black);
		String name = getVisualizationName();
		if (name == null)
			return new Rectangle(leftMarg, startY, w, iconH + Metrics.mm());
		int x = leftMarg, y = startY + iconH + Metrics.mm(), prw = prefW;
		boolean centered = true;
		if (prefW - w > 50) {
			StringInRectangle sr = new StringInRectangle(name);
			sr.setRectSize(prefW - w, 10);
			Dimension d = sr.countSizes(g);
			if (d != null && w + d.width <= prefW) {
				x += w;
				y = startY;
				prw = prefW - w;
				centered = false;
			}
		}
		Point p = StringInRectangle.drawText(g, name, x, y, prw, centered);
		y = p.y;
		if (y < startY + iconH + Metrics.mm()) {
			y = startY + iconH + Metrics.mm();
		}
		if (w < p.x - leftMarg) {
			w = p.x - leftMarg;
		}
		return new Rectangle(leftMarg, startY, w, y - startY);
	}

	/**
	* Draws the part of the legend explaining this specific presentation method.
	*/
	public abstract Rectangle drawMethodSpecificLegend(Graphics g, int startY, int leftmarg, int prefW);

	/**
	* Through this function the DataMapper can set the name of the
	* visualization method according to its list of names.
	*/
	@Override
	public void setVisualizationName(String name) {
		visName = name;
	}

	/**
	* Returns the name of the visualization method implemented by this
	* Visualizer.
	*/
	@Override
	public String getVisualizationName() {
		return visName;
	}

	/**
	* Through this function the DataMapper can set ID of the
	* visualization method according to its list of names.
	*/
	@Override
	public void setVisualizationId(String id) {
		visId = id;
	}

	/**
	* Returns ID of the visualization method implemented by this
	* Visualizer.
	*/
	@Override
	public String getVisualizationId() {
		return visId;
	}

	/**
	 * If this visualiser defines object colors using some ObjectColorer, returns
	 * a reference to this ObjectColorer.
	 * By default, returns null.
	 */
	@Override
	public ObjectColorer getObjectColorer() {
		return null;
	}

	/**
	* Draws a small picture symbolizing this presentation technique
	*/
	@Override
	public abstract void drawIcon(Graphics g, int x, int y, int w, int h);

	/**
	* If the visualizer is not applicable to given data or any other error
	* occurs, an error message is generated. This method returns the error
	* message or null if everything is OK.
	*/
	@Override
	public String getErrorMessage() {
		return err;
	}

	/**
	* Stores, for further use, the location of the visualizer.
	* This is usually the identifier of the map window in which this visualizer
	* is used. The location is important for correct restoring of tool states.
	*/
	@Override
	public void setLocation(String location) {
		this.location = location;
	}

	/**
	* Returns the previously stored location of the visualizer.
	* This is usually the identifier of the map window in which this visualizer
	* is used. The location is important for correct restoring of tool states.
	*/
	@Override
	public String getLocation() {
		return location;
	}

	/**
	* Replies whether any parameters of this visualization method may be
	* changed, e.g. colors or sizes of signs. This does not include interactive
	* analytical manipulation.
	* By default, returns false.
	*/
	@Override
	public boolean canChangeParameters() {
		return false;
	}

	/**
	* Constructs and displays a dialog for changing parameters of this
	* visualization method (if possible). Changing parameters (colors or sizes
	* of signs) is different from interactive analytical manipulation!
	* By default, does nothing.
	*/
	@Override
	public void startChangeParameters() {
	}

	/**
	* Replies whether the color scale used by this visualization method may be
	* changed. This does not include interactive analytical manipulation.
	* By default, returns false.
	*/
	@Override
	public boolean canChangeColors() {
		return false;
	}

	/**
	* Constructs and displays a dialog for changing colors used in this
	* visualization method (if possible). Changing colors is different from
	* interactive analytical manipulation!
	* By default, does nothing.
	*/
	@Override
	public void startChangeColors() {
	}

	/**
	* Reacts to clicking on the "hot spot" with the checkbox used for enabling and
	* disabling the visualizer.
	*/
	@Override
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if (cmd == null)
			return;
		if (cmd.equals("disable")) {
			setEnabled(false);
		} else if (cmd.equals("enable")) {
			setEnabled(true);
		}
	}

//ID
	public Hashtable getVisProperties() {
		return null;
	}

	public void setVisProperties(Hashtable param) {
//    notifyVisChange();
	}

	public ToolSpec getVisSpec() {
		ToolSpec spec = new ToolSpec();
		spec.tagName = getTagName();
		spec.methodId = visId;
		spec.location = getLocation();
		spec.properties = getVisProperties();
		return spec;
	}

//~ID
	//---------------- implementation of the SaveableTool interface ------------
	/**
	* Returns the specification of this visualizer (normally an instance of the
	* class spade.vis.spec.MapVisSpec) for storing in a file.
	*/
	@Override
	public Object getSpecification() {
		return getVisSpec();
	}

	/**
	* Returns the keyword used in the opening tag of a stored state description
	* of this tool. A visualizer description (specification) is usually stored as
	* a sequence of lines starting with <map> and ending with </map>. Hence,
	* this method returns "map".
	*/
	@Override
	public String getTagName() {
		return "map";
	}

	/**
	* After the visualizer is constructed, it may be requested to setup its individual
	* properties according to the given list of stored properties.
	*/
	@Override
	public void setProperties(Hashtable properties) {
		setVisProperties(properties);
	}

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
	* Destroys the visualizer when it is no more used. If the visualizer listened
	* itself to any events or used some other objects listening to events, it
	* must stop listening.
	* Besides, must send a PropertyChangeEvent with the name "destroyed" to its
	* destroying listener(s), @see addDestroyingListener.
	* Here, only destroying listeners are notified, and nothing more is done.
	*/
	@Override
	public void destroy() {
		if (isDestroyed())
			return;
		if (destroyListeners != null) {
			PropertyChangeEvent pce = new PropertyChangeEvent(this, "destroyed", null, null);
			for (int i = 0; i < destroyListeners.size(); i++) {
				((PropertyChangeListener) destroyListeners.elementAt(i)).propertyChange(pce);
			}
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
