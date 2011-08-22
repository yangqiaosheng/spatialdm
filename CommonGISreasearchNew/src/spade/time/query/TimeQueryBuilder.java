package spade.time.query;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.transform.AttributeTransformer;
import spade.lib.basicwin.MouseDragEventConsumer;
import spade.lib.lang.Language;
import spade.lib.util.IntArray;
import spade.lib.util.NumRange;
import spade.time.vis.TimeGraph;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.ObjectFilter;
import spade.vis.database.Parameter;
import spade.vis.geometry.RealRectangle;

/**
* Suports the formulation of queries concerning time-series data. The user
* formulates the queries graphically by drawing and manipulating boxes on a time
* graph. Hence, this component is linked to the time graph. In particular,
* it listens to mouse events occurring within the time graph and can draw
* boxes in its graphical context.
*/
public class TimeQueryBuilder implements PropertyChangeListener, ItemListener, ActionListener, MouseDragEventConsumer {
	/**
	* All texts that appear in the user interface must be defined in resource
	* classes in two languages: english and german. Typically, there is a pair
	* of such resource classes in each package. English texts are defined in
	* the class <package_name>.Res, and german texts in the class
	* <package_name>.Res_de. The class spade.lib.lang.Language cares about the
	* selection of the apropriate resources depending on the selected user
	* interface language.
	*/
	static ResourceBundle res = Language.getTextResource("spade.time.query.Res");
	/**
	* Possible logical operations for combining multiple query conditions
	*/
	static public final int AND = 0, OR = 1;
	/**
	* The time graph this query builder is connected to.
	*/
	protected TimeGraph timeGraph = null;
	/**
	* The table with time-dependent data. One of the time-dependent attributes
	* of this table is represented on the time graph.
	*/
	protected AttributeDataPortion table = null;
	/**
	* The filter in which the query builder sets temporal query conditions
	*/
	protected ObjectFilter filter = null;
	/**
	* The time-dependent super-attribute represented on the time graph, i.e. an
	* attribute having references to child attributes (table columns) corresponding
	* to different values of a temporal parameter.
	*/
	protected Attribute supAttr = null;
	/**
	* The temporal parameter the attribute depends on
	*/
	protected Parameter par = null;
	/**
	* The attribute transformer transforms the data displayed on the graph:
	* aggregates, computes changes, compares to mean, median, or selected object,
	* etc. When the time graph represents transformed data, query conditions
	* entered by the user also refer to the transformed data. The TimeQueryBuilder
	* must listen to data change events from the transformer. When the
	* transformation procedure or transformation parameters change, the transformer
	* sends to its listeners a PropertyChangeEvent with the property name "values".
	* After receiving this event, the TimeQueryBuilder must check whether previously
	* specified query conditions still make sense (i.e. the limits lie within the
	* new value range resulting from the transformation). Conditions which are
	* inconsistent with the new data must be deleted.
	*/
	protected AttributeTransformer aTrans = null;
	/**
	* Indicates "enabled" or "disabled" state. In the "enabled" state, the
	* TimeQueryBuilder listens to mouse dragging events occurring within the
	* time graph.
	*/
	protected boolean enabled = false;
	/**
	* Currently used logical operation for combining multiple query conditions
	* (by default, this is AND)
	*/
	protected int operation = AND;
	/**
	* A panel with UI elements for controlling possible parameters of the
	* query builder, in particular, the method of combining query conditions
	* (AND or OR)
	*/
	protected TimeQueryControls controlPanel = null;
	/**
	* The vector of "boxes" built by the user. For each box, there must be a
	* corresponding query component (condition). The boxes are instances of
	* RealRectangle. Storing of "real" coordinates provides the independence
	* on the current scale on the time graph.
	*/
	protected Vector boxes = null;

	/**
	* Connects the TimeQueryBuilder to a time graph.
	*/
	public void setTimeGraph(TimeGraph tigr) {
		timeGraph = tigr;
		if (timeGraph == null)
			return;
		timeGraph.addPropertyChangeListener(this); //for listening to drawing events
		table = timeGraph.getTable();
		if (table != null) {
			table.addPropertyChangeListener(this);
			filter = table.getObjectFilter();
		}
		supAttr = timeGraph.getAttribute();
		par = timeGraph.getTemporalParameter();
	}

	/**
	* Sets the data transformer, which may transform data in various ways before
	* representing them on the time graph. When the time graph represents
	* transformed data, query conditions entered by the user also refer to the
	* transformed data. The TimeQueryBuilder must register as a listeners of data
	* change events produced by the transformer.
	*/
	public void setAttributeTransformer(AttributeTransformer transformer) {
		aTrans = transformer;
		if (aTrans != null) {
			aTrans.addPropertyChangeListener(this);
		}
	}

	/**
	* Sets whether the TimeQueryBuilder is currently enabled or disabled state.
	* In the "enabled" state, the TimeQueryBuilder requests the time graph to
	* pass to it all mouse dragging events occurring within the time graph.
	*/
	public void setEnabled(boolean value) {
		if (timeGraph == null) {
			enabled = false;
			return;
		}
		if (enabled == value)
			return;
		enabled = value;
		if (enabled) {
			timeGraph.setMouseDragConsumer(this);
		} else {
			timeGraph.setMouseDragConsumer(null);
		}
	}

	/**
	* Sets the panel with UI elements for controlling possible parameters of the
	* query builder, in particular, the method of combining query conditions
	* (AND or OR). If there are multiple time graphs in one panel, each graph
	* is linked to its own query builder, but the control panel is common
	* for all these query builders.
	*/
	public void setControlPanel(TimeQueryControls queryControls) {
		controlPanel = queryControls;
		if (controlPanel != null) {
			controlPanel.operationChoice.addItemListener(this);
			controlPanel.eraseButton.addActionListener(this);
		}
	}

	/**
	* Reacts to changes of the condition combination operation in the
	* corresponding choice control.
	*/
	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource().equals(controlPanel.operationChoice)) {
			operation = controlPanel.operationChoice.getSelectedIndex();
			System.out.println("TimeQueryBuilder: operation=" + operation);
			//do necessary actions with existing query conditions
			//...
		}
	}

	/**
	* Reacts to pressing the button "Erase all": deletes all boxes and the
	* corresponding query conditions
	*/
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("erase_all")) {
			if (boxes == null || boxes.size() < 1)
				return;
			boxes.removeAllElements();
			//now remove all query conditions
			//... (to be implemented)
			timeGraph.repaint();
		}
	}

	/**
	* In this method, the TimeQueryBuilder reacts to data change events from the
	* data transformer or the data table. After receiving such an event, the
	* TimeQueryBuilder must check whether previously specified query conditions
	* still make sense (i.e. the limits lie within the new value range resulting
	* from the transformation). Conditions which are inconsistent with the new
	* data must be deleted.
	*/
	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		if (pce.getSource().equals(timeGraph) && pce.getPropertyName().equals("drawn")) {
			drawBoxes();
		}
		if (pce.getSource().equals(aTrans) && pce.getPropertyName().equals("values")) {
			checkConditions();
		} else if (pce.getSource().equals(table)) {
			if (pce.getPropertyName().equals("destroyed") || pce.getPropertyName().equals("structure_complete")) {
				destroy();
			} else if (pce.getPropertyName().equals("data_added") || pce.getPropertyName().equals("data_removed") || pce.getPropertyName().equals("data_updated")) {
				checkConditions();
			} else if (pce.getPropertyName().equals("values"))
				if (timeGraph.showsAttributes((Vector) pce.getNewValue())) {
					checkConditions();
				}
		}
	}

	/**
	* Checks whether previously specified query conditions still make sense
	* after the data have changed (i.e. the limits lie within the new value range).
	* Conditions which are inconsistent with the new data must be deleted.
	*/
	public void checkConditions() {
		if (timeGraph == null || boxes == null || boxes.size() < 1)
			return;
		//obtaining new minimum and maximum values
		double minValue = timeGraph.getAbsMin(), maxValue = timeGraph.getAbsMax();
		for (int i = boxes.size() - 1; i >= 0; i--) {
			RealRectangle box = (RealRectangle) boxes.elementAt(i);
			if (box.ry1 >= minValue && box.ry2 <= maxValue) {
				continue; //still valid
			}
			if (box.ry1 >= maxValue || box.ry2 <= minValue) {
				deleteBoxAndCondition(i);
			} else {
				boolean changed = false;
				if (box.ry1 < minValue) {
					box.ry1 = (float) minValue;
					changed = true;
				}
				if (box.ry2 > maxValue) {
					box.ry2 = (float) maxValue;
					changed = true;
				}
				if (box.ry1 == minValue && box.ry2 == maxValue) {
					deleteBoxAndCondition(i); //actually puts no limitation!
				} else if (changed) {
					//modify the corresponding query condition
					//...(to be implemented)
				}
			}
		}
	}

	/**
	* Deletes the box with the specified index and the associated query condition
	*/
	protected void deleteBoxAndCondition(int idx) {
		if (boxes == null || idx < 0 || idx >= boxes.size())
			return;
		boxes.removeElementAt(idx);
		//delete the corresponding query condition
		//... (to be implemented)
	}

	/**
	* Draws all the boxes defined so far inside the time graph area
	*/
	protected void drawBoxes() {
		if (boxes == null || boxes.size() < 1)
			return;
		Graphics gr = timeGraph.getGraphics();
		gr.setXORMode(timeGraph.getPlotAreaColor());
		for (int i = 0; i < boxes.size(); i++) {
			RealRectangle r = (RealRectangle) boxes.elementAt(i);
			int sx1 = timeGraph.getScrX(r.rx1), sx2 = timeGraph.getScrX(r.rx2), sy1 = timeGraph.getScrY(r.ry1), sy2 = timeGraph.getScrY(r.ry2);
			int w = sx2 - sx1, h = sy1 - sy2;
			gr.setColor(Color.red);
			gr.drawRect(sx1, sy2, w, h);
			gr.setColor(Color.pink);
			gr.fillRect(sx1 + 1, sy2 + 1, w - 1, h - 1);
		}
		gr.setPaintMode();
		gr.dispose();
	}

	/**
	* Stops listening to all events
	*/
	public void destroy() {
		if (aTrans != null) {
			aTrans.removePropertyChangeListener(this);
		}
		if (table != null) {
			table.removePropertyChangeListener(this);
		}
	}

	/**
	* The mouse coordinates at the moment of dragging start
	*/
	protected int dragStartX = -1, dragStartY = -1;

	/**
	* Reacts to a start of mouse dragging in the time graph.
	* x0 and y0 are the mouse coordinates at the moment when the mouse button
	* was pressed, x and y are the current mouse coordinates.
	* At the moment, we do not consider the case when the user wants to change
	* one of earlier built boxes. We assume that the user always enters a new box.
	* This should be later changed.
	*/
	@Override
	public void mouseDragBegin(int x0, int y0, int x, int y) {
		drawFrame(x0, y0, x, y);
		dragStartX = x0;
		dragStartY = y0;
	}

	/**
	* Reacts to a continuation of mouse dragging in the time graph.
	* x1 and y1 are the old mouse coordinates, x2 and y2 are the new coordinates.
	*/
	@Override
	public void mouseDragging(int x1, int y1, int x2, int y2) {
		if (x1 == x2 && y1 == y2)
			return;
		//erase old frame
		drawFrame(dragStartX, dragStartY, x1, y1);
		//draw new frame
		drawFrame(dragStartX, dragStartY, x2, y2);
	}

	/**
	* Reacts to the end of mouse dragging in the time graph.
	* x and y are the mouse coordinates at the moment when the mouse button
	* was released.
	*/
	@Override
	public void mouseDragEnd(int x, int y) {
		//Coordinates of the box are dragStartX,dragStartY,x,y
		if (dragStartX == x || dragStartY == y)
			return; //nothing is inside!
		//Find the minimum and maximum (transformed) attribute values corresponding
		//to this box.
		NumRange limits = timeGraph.getValueRangeBetween(dragStartY, y);
		if (limits == null)
			return;
		System.out.println("min=" + limits.minValue + " max=" + limits.maxValue);
		//Find the numbers of the table columns corresponding to this box
		IntArray columns = timeGraph.getColumnsBetween(dragStartX, x);
		if (columns == null)
			return;
		System.out.print("Selected columns:");
		for (int i = 0; i < columns.size(); i++) {
			System.out.print(" " + columns.elementAt(i));
		}
		System.out.println();
		//Transform the screen coordinates of the box into "real" coordinates
		//and store the box in the vector of all boxes
		int x0 = dragStartX, y0 = dragStartY;
		if (x < x0) {
			int k = x0;
			x0 = x;
			x = k;
		}
		if (y < y0) {
			int k = y0;
			y0 = y;
			y = k;
		}
		RealRectangle box = new RealRectangle(timeGraph.getAbsX(x0), (float) timeGraph.getAbsY(y0), timeGraph.getAbsX(x), (float) timeGraph.getAbsY(y));
		if (boxes == null) {
			boxes = new Vector(10, 10);
		}
		boxes.addElement(box);
		//Build the query condition and combine it with the other query conditions
		//defined earlier
		//... (to be implemented)
		//clear internal variables
		dragStartX = dragStartY = -1;
	}

	/**
	* Draws a rectangular frame in XOR mode. In this mode, second drawing of the
	* same shape erases the previous drawing.
	*/
	protected void drawFrame(int x0, int y0, int x, int y) {
		if (x != x0 || y != y0) {
			if (x < x0) {
				int k = x0;
				x0 = x;
				x = k;
			}
			if (y < y0) {
				int k = y0;
				y0 = y;
				y = k;
			}
			int w = x - x0, h = y - y0;
			Graphics gr = timeGraph.getGraphics();
			gr.setXORMode(timeGraph.getPlotAreaColor());
			gr.setColor(Color.red);
			gr.drawRect(x0, y0, w, h);
			if (w > 0 && h > 0) {
				gr.setColor(Color.pink);
				gr.fillRect(x0 + 1, y0 + 1, w - 1, h - 1);
			}
			gr.setPaintMode();
			gr.dispose();
		}
	}
}