package spade.vis.mapvis;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.beans.PropertyChangeListener;

import spade.analysis.classification.ObjectColorer;
import spade.vis.database.DataItem;
import spade.vis.map.LegendDrawer;
import spade.vis.map.MapContext;
import spade.vis.spec.SaveableTool;

/**
* Visualizer specifies how to present thematic data in a map.
* This is the class to be extended by all classes realizing
* various presentation methods such as painting or bar charts.
* Visualizer implements the LegendDrawer interface. This means that it should
* be able to draw the part of the legend explaining this presentation method.
* Visualizer may need statistics about data to setup its parameters.
* Visualizer may also show the statistics in the legend.
* Visualizer implements the PropertyChangeListener interface in order to
* listen to changes of statistics.
*/

public interface Visualizer extends LegendDrawer, SaveableTool {
	/**
	* Returns ID of the visualization method implemented by this
	* Visualizer.
	*/
	public String getVisualizationId();

	/**
	* Through this function the DataMapper can set ID of the
	* visualization method according to its list of names.
	*/
	public void setVisualizationId(String id);

	/**
	* Returns the name of the visualization method implemented by this
	* Visualizer.
	*/
	public String getVisualizationName();

	/**
	* Through this function the DataMapper can set the name of the
	* visualization method according to its list of names.
	*/
	public void setVisualizationName(String name);

	/**
	 * If this visualiser defines object colors using some ObjectColorer, returns
	 * a reference to this ObjectColorer.
	 */
	public ObjectColorer getObjectColorer();

	/**
	* Sets the identifier of the table this Visualizer is linked with (if any).
	* A Visualizer may be used to represent data from a table. In this case it may
	* keep the identifier of the table.
	*/
	public void setTableIdentifier(String tblId);

	/**
	* Returns the identifier of the table this Visualizer is linked with (if any).
	* A Visualizer may be used to represent data from a table. In this case it may
	* keep the identifier of the table.
	*/
	public String getTableIdentifier();

	/**
	* Returns the specification of this visualizer (normally an instance of the
	* class spade.vis.spec.MapVisSpec) for storing in a file.
	*/
	@Override
	public Object getSpecification();

	/**
	* Returns the keyword used in the opening tag of a stored state description
	* of this tool. A visualizer description (specification) is usually stored as
	* a sequence of lines starting with <map> and ending with </map>. Hence,
	* this method returns "map".
	*/
	@Override
	public String getTagName();

	public void addVisChangeListener(PropertyChangeListener l);

	public void removeVisChangeListener(PropertyChangeListener l);

	/**
	* Notifies the listeners about a change of the visualization parameters
	*/
	public void notifyVisChange();

	/**
	* "Disables" or "enables" the visualizer. If a visualizer is
	* disabled, the geographical layer does not use it for drawing its objects but
	* uses instead the drawing parameters common for all objects.
	*/
	public void setEnabled(boolean value);

	/**
	* Informs about the "enabled" or  "disabled" state of the visualizer. If a
	* visualizer is disabled, the geographical layer does not use it for drawing
	* its objects but uses instead the drawing parameters common for all objects.
	*/
	public boolean isEnabled();

	/**
	* This method is called by a GeoObject. The GeoObject sends its DataItem
	* to the Visualizer, and the Visualizer returns an Object specifying the
	* presentation of data contained in this DataItem. This may be, in
	* particular, Color or Diagram. Typically the argument DataItem is
	* a ThematicDataItem, although in some cases (e.g. manual classification)
	* this is not required.
	*/
	public Object getPresentation(DataItem dit, MapContext mc);

	/**
	* This method informs whether the Visualizer produces diagrams.
	* This is important for defining the order of drawing of GeoLayers on the
	* map: the diagrams should be drawn on top of all geography.
	*/
	public boolean isDiagramPresentation();

	/**
	* The method from the LegendDrawer interface.
	*/
	@Override
	public Rectangle drawLegend(Component c, Graphics g, int startY, int leftMarg, int prefW);

	/**
	* Draws a small picture symbolizing this presentation technique
	*/
	public void drawIcon(Graphics g, int x, int y, int w, int h);

	/**
	* If the visualizer is not applicable to given data or any other error
	* occurs, an error message is generated. This method returns the error
	* message or null if everything is OK.
	*/
	public String getErrorMessage();

	/**
	* Stores, for further use, the location of the visualizer.
	* This is usually the identifier of the map window in which this visualizer
	* is used. The location is important for correct restoring of tool states.
	*/
	public void setLocation(String location);

	/**
	* Returns the previously stored location of the visualizer.
	* This is usually the identifier of the map window in which this visualizer
	* is used. The location is important for correct restoring of tool states.
	*/
	public String getLocation();

	/**
	* Replies whether any parameters of this visualization method may be
	* changed, e.g. colors or sizes of signs. This does not include interactive
	* analytical manipulation
	*/
	public boolean canChangeParameters();

	/**
	* Constructs and displays a dialog for changing parameters of this
	* visualization method (if possible). Changing parameters (colors or sizes
	* of signs) is different from interactive analytical manipulation!
	*/
	public void startChangeParameters();

	/**
	* Replies whether the color scale used by this visualization method may be
	* changed. This does not include interactive analytical manipulation.
	*/
	public boolean canChangeColors();

	/**
	* Constructs and displays a dialog for changing colors used in this
	* visualization method (if possible). Changing colors is different from
	* interactive analytical manipulation!
	*/
	public void startChangeColors();

	/**
	* As a SaveableTool, a visualizer may be registered somewhere and, hence, must
	* notify the component where it is registered about its destroying.
	*/
	@Override
	public void addDestroyingListener(PropertyChangeListener lst);

	/**
	* Destroys the visualizer when it is no more used. If the visualizer listened
	* itself to any events or used some other objects listening to events, it
	* must stop listening.
	* Besides, must send a PropertyChangeEvent with the name "destroyed" to its
	* destroying listener(s), @see addDestroyingListener.
	*/
	@Override
	public void destroy();

	/**
	* Returns the size of the "checkbox" used to enable and disable the visualizer
	*/
	public int getSwitchSize();
}
