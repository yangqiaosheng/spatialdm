package spade.analysis.plot;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Hashtable;
import java.util.Vector;

import observer.annotation.AnnotationSurfaceInterface;
import observer.annotation.Markable;
import spade.analysis.generators.PlotGeneratorsDescriptor;
import spade.analysis.system.Supervisor;
import spade.analysis.transform.AttributeTransformer;
import spade.lib.basicwin.Drawable;
import spade.lib.basicwin.FocusListener;
import spade.lib.basicwin.Focuser;
import spade.lib.basicwin.Metrics;
import spade.lib.basicwin.StringInRectangle;
import spade.lib.util.Aligner;
import spade.lib.util.NumRange;
import spade.lib.util.StringUtil;
import spade.vis.action.HighlightListener;
import spade.vis.action.Highlighter;
import spade.vis.action.ObjectEvent;
import spade.vis.action.ObjectEventHandler;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.DataTreater;
import spade.vis.database.ObjectFilter;
import spade.vis.dataview.TransformedDataPresenter;
import spade.vis.event.EventSource;
import spade.vis.spec.SaveableTool;
import spade.vis.spec.ToolSpec;
import core.InstanceCounts;

/**
* A class that represents values of numeric attribute(s) on a plot.
* The base class for all types of graphical data displays.
* The data to represent are taken from an AttributeDataPortion.
*/

public abstract class Plot implements Drawable, DataTreater, TransformedDataPresenter, EventSource, SaveableTool, HighlightListener, FocusListener, MouseListener, MouseMotionListener, PropertyChangeListener, PrintableImage, Markable {
	protected static int minMarg = Focuser.getRequiredMargin();
	protected static Color normalColor = new Color(128, 128, 128), filteredColor = new Color(168, 168, 168), activeColor = Color.white, selectedColor = Color.black;

	protected int width = 0, height = 0, mx1 = minMarg, mx2 = minMarg, my1 = minMarg, my2 = minMarg;
	protected PlotObject dots[] = null;

	protected boolean selectionEnabled = false, isZoomable = false;

	/**
	* In some cases it may be necessary to hide a plot
	*/
	protected boolean isHidden = false;

	public boolean getIsHidden() {
		return isHidden;
	}

	/**
	* The canvas in which this object should draw itself
	*/
	protected Canvas canvas = null;
	/**
	* The boundaries in which the object should fit itself
	*/
	protected Rectangle bounds = null;

	/**
	* The source of the data to be shown on the plot
	*/
	protected AttributeDataPortion dataTable = null;
	/**
	* ObjectFilter may be associated with the table and contain results of data
	* querying. Only data satisfying the current query (if any) are displayed
	*/
	protected ObjectFilter tf = null;
	/**
	* A Plot may optionally be connected to a transformer of attribute
	* values. In this case, it represents transformed attribute values.
	*/
	protected AttributeTransformer aTrans = null;
	/**
	 * Indicates whether the plot should react to data change events from the table
	 * (this may be undesirable e.g. in case of visualisation of dynamic aggregators)
	 */
	protected boolean reactToTableDataChange = true;
	/**
	* Supervisor provides access of a plot to the Highlighter (common for
	* all data displays) and in this way links together all displays
	*/
	protected Supervisor supervisor = null;
	/**
	* Identifier of this visualization method
	*/
	protected String methodId = null;
	/**
	* This variable shows whether this plot is an independent event source
	* and, hence, should be registered at the supervisor as an event source
	* or it is a part of some larger plot. For example, a dot plot may be a part
	* of a dynamic query device or a map manipulator. In this case the larger
	* plot acts as an event source and should register itself at the supervisor.
	* Object events should be sent from the smaller plot not
	* directly to the supervisor but to the larger plot that must implement
	* the ObjectEventHandler interface.
	*/
	protected boolean isIndependentEventSource = false;
	/**
	* A plot sends object events that occur in it to an appropriate
	* ObjectEventHandler. In a case when the plot is an independent event source
	* (see the variable isIndependentEventSource), the ObjectEventHandler
	* is the supervisor (the supervisor implements this interface). Otherwise,
	* the handler is the larger plot in which this plot is included as a part.
	* The larger plot should implement the ObjectEventHandler interface.
	*/
	protected ObjectEventHandler objEvtHandler = null;
	/**
	* A variable indicating whether there is at least one highlighted object.
	* Intended to avoid unnecessary messages to the highlighter upon mouse
	* movements in an object-free area of the plot
	*/
	protected boolean hasHighlighted = false;
	/**
	* A variable indicating whether there is at least one selected object.
	* Intended as well as hasHighlighted for optimisation purposes
	*/
	protected boolean hasSelected = false;
	/**
	* Colors of the background of the canvas and of the plot area
	*/
	public static Color bkgColor = Color.white, plotAreaColor = Color.lightGray;
	/**
	* Aligner is used to align horisontally or vertically several plots
	*/
	protected Aligner aligner = null;
	/**
	* This variable shows whether this plot may define alignment for other plots
	*/
	protected boolean mayDefineAlignment = false;
	/**
	* Indicates "destroyed" state. Initially is false.
	*/
	protected boolean destroyed = false;
	/**
	* Used to generate unique identifiers of instances of Plot descendants
	*/
	protected int instanceN = 0;
	/**
	* The unique identifier of this plot. The identifier is used
	* 1) for explicit linking of producers and recipients of object events;
	* 2) for correct restoring of system states with multiple plots.
	*/
	protected String plotId = null;
	/**
	* Indicates whether the objects on the plot are currently colored according
	* to some propagated classification.
	*/
	protected boolean objectsAreColored = false;
	/**
	* As a SaveableTool, a plot may be registered somewhere and, hence, must
	* notify the component where it is registered about its destroying. This vector
	* contains the listeners to be notified about destroying of the visualizer.
	*/
	protected Vector destroyListeners = null;

	/**
	* Used to generate unique identifiers of instances of Plot's descendants.
	* The method should be defined in the descendants. It should count the
	* number of constructed instances of the class and set the variable
	* instanceN. Plot calls this method in the constructor. The variable
	* instanceN is used in the method getIdentifier().
	*/
	protected abstract void countInstance();

	/**
	* Used to generate unique identifiers of instances of Plot's descendants.
	* The method should be defined in the descendants.
	* Plot uses this method in the method getIdentifier().
	*/
	protected abstract String getPlotTypeName();

	/**
	* Constructs a Plot. The argument isIndependent shows whether
	* this plot is displayed separately and, hence, should be registered at the
	* supervisor as an event source or it is a part of some larger plot.
	* The variable allowSelection shows whether the plot should listen to
	* mouse events and transform them to object selection events.
	* Supervisor provides access of a plot to the Highlighter (common for
	* all data displays) and in this way links together all displays
	* The argument handler is a reference to the component the plot
	* should send object events to. In a case when the plot is displayed
	* independently, the ObjectEventHandler is the supervisor (the supervisor
	* implements this interface). Otherwise, the handler is the larger plot in
	* which this plot is included as a part.
	* The larger plot should implement the ObjectEventHandler interface.
	*/
	public Plot(boolean isIndependent, boolean allowSelection, Supervisor sup, ObjectEventHandler handler) {
		supervisor = sup;
		isIndependentEventSource = isIndependent;
		if (isIndependentEventSource) //countInstance();
		{
			instanceN = InstanceCounts.incPlotInstanceCount();
			String id = "Plot_" + instanceN;
			if (supervisor != null && supervisor.getUI() != null) {
				while (supervisor.getUI().findPlot(id) != null) {
					instanceN = InstanceCounts.incPlotInstanceCount();
					id = "Plot_" + instanceN;
				}
			}
			setIdentifier(id);
		}
		if (handler != null) {
			objEvtHandler = handler;
		} else {
			objEvtHandler = sup;
		}
		if (supervisor != null) {
			supervisor.addPropertyChangeListener(this);
			if (isIndependentEventSource) {
				supervisor.registerObjectEventSource(this);
				supervisor.registerDataDisplayer(this);
			}
		}
		enableSelection(allowSelection);
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

	/**
	 * @return reference to the supervisor
	 */
	public Supervisor getSupervisor() {
		return supervisor;
	}

	public void setDataSource(AttributeDataPortion tbl) {
		this.dataTable = tbl;
		if (dataTable != null) {
			dataTable.addPropertyChangeListener(this);
			if (selectionEnabled && supervisor != null) {
				supervisor.registerHighlightListener(this, dataTable.getEntitySetIdentifier());
			}
			tf = dataTable.getObjectFilter();
			if (tf != null) {
				tf.addPropertyChangeListener(this);
			}
		}
		checkObjectColorPropagation();
	}

	/**
	 * Sets whether the plot should react to data change events from the table
	 * (this may be undesirable e.g. in case of visualisation of dynamic aggregators)
	 */
	public void setReactToTableDataChange(boolean value) {
		reactToTableDataChange = value;
	}

	/**
	* Connects the Plot to a transformer of attribute values. After this,
	* it must represent transformed attribute values. The argument listenChanges
	* determines whether the plot will listen to the changes of the transformed
	* values and appropriately reset itself. This is not always desirable; for
	* example, a plot may be a part of a map manipulator, which determines
	* itself when and how the plot must change.
	*/
	public void setAttributeTransformer(AttributeTransformer transformer, boolean listenChanges) {
		if (aTrans != null) {
			aTrans.removePropertyChangeListener(this);
		}
		aTrans = transformer;
		if (aTrans != null) {
			if (listenChanges) {
				aTrans.addPropertyChangeListener(this);
			}
		}
	}

	/**
	* Returns its transformer of attribute values (if exists)
	*/
	public AttributeTransformer getAttributeTransformer() {
		return aTrans;
	}

	/**
	* Depending on the presence of an attribute transformer, gets attribute values
	* either from the transformer or the table
	*/
	public double getNumericAttrValue(int attrN, int recN) {
		if (aTrans != null)
			return aTrans.getNumericAttrValue(attrN, recN);
		if (dataTable != null)
			return dataTable.getNumericAttrValue(attrN, recN);
		return Double.NaN;
	}

	/**
	* A method from the TransformedDataPresenter interface.
	* Returns the transformed value corresponding to the given row and column of
	* the original table. If the value is not transformed, returns null.
	*/
	@Override
	public String getTransformedValue(int rowN, int colN) {
		if (aTrans != null)
			return aTrans.getTransformedValueAsString(rowN, colN);
		return null;
	}

	/**
	* Depending on the presence of an attribute transformer, gets the value range
	* of the numeric attribute with the specified identifier
	* either from the transformer or the table
	*/
	public NumRange getAttrValueRange(String attrId) {
		if (aTrans != null)
			return aTrans.getAttrValueRange(attrId);
		if (dataTable != null)
			return dataTable.getAttrValueRange(attrId);
		return null;
	}

	public boolean selectedByQuery(int n) {
		if (tf == null)
			return true;
		return tf.isActive(n);
	}

	public void applyFilter() {
		if (dots != null)
			if (tf == null || !tf.areObjectsFiltered()) {
				for (int i = 0; i < dots.length; i++) {
					dots[i].isActive = true;
				}
			} else {
				for (int i = 0; i < dots.length; i++) {
					dots[i].isActive = tf.isActive(i);
				}
			}
	}

	/**
	* Sets whether the plot allows for its zooming. If yes, the plot
	* includes a pair of Focusers used for selection of value subranges of
	* the attributes. If a scatter plot is non-zoomable, the Focusers
	* are absent.
	*/
	public void setIsZoomable(boolean value) {
		isZoomable = value;
	}

	public void setIsHidden(boolean value) {
		if (isHidden != value) {
			isHidden = value;
			redraw();
		}
	}

	public boolean hasData() {
		return dots != null;
	}

	public void resetDotCoordinates() {
		for (PlotObject dot : dots) {
			dot.reset();
		}
	}

	public void enableSelection(boolean flag) {
		selectionEnabled = flag;
		if (selectionEnabled) {
			if (canvas != null) {
				if (annotationSurfacePresent()) {
					canvas.addMouseListener(getAnnotationSurface().addMouseRedirector(this));
					canvas.addMouseMotionListener(getAnnotationSurface().addMouseMotionRedirector(this));
				} else {
					canvas.addMouseListener(this);
					canvas.addMouseMotionListener(this);
				}
			}
			if (supervisor != null && dataTable != null) {
				supervisor.registerHighlightListener(this, dataTable.getEntitySetIdentifier());
			}
		} else {
			if (canvas != null) {
				if (annotationSurfacePresent()) {
					canvas.removeMouseListener(getAnnotationSurface().removeMouseRedirector(this));
					canvas.removeMouseMotionListener(getAnnotationSurface().removeMouseMotionRedirector(this));
				} else {
					canvas.removeMouseListener(this);
					canvas.removeMouseMotionListener(this);
				}
			}
			if (supervisor != null && dataTable != null) {
				supervisor.removeHighlightListener(this, dataTable.getEntitySetIdentifier());
			}
		}
	}

//------------- Functions from the Drawable interface -------------------
	/**
	* Sets the canvas in which this object should draw itself
	*/
	@Override
	public void setCanvas(Canvas c) {
		canvas = c;
		if (canvas != null && selectionEnabled) {
			if (annotationSurfacePresent()) {
				canvas.addMouseListener(getAnnotationSurface().addMouseRedirector(this));
				canvas.addMouseMotionListener(getAnnotationSurface().addMouseMotionRedirector(this));
			} else {
				canvas.addMouseListener(this);
				canvas.addMouseMotionListener(this);
			}
		}
	}

	public Canvas getCanvas() {
		return canvas;
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(60 * Metrics.mm(), 60 * Metrics.mm());
	}

	@Override
	public void setBounds(Rectangle bounds) {
		this.bounds = bounds;
		if (bounds != null) {
			width = bounds.width - mx1 - mx2;
			height = bounds.height - my1 - my2;
		}
	}

	@Override
	public Rectangle getBounds() {
		return bounds;
	}

	public boolean checkWhatSelected() {
		if (supervisor == null || dataTable == null || dots == null)
			return false;
		Highlighter highlighter = supervisor.getHighlighter(dataTable.getEntitySetIdentifier());
		if (highlighter == null)
			return false;
		hasSelected = false;
		for (int i = 0; i < dots.length; i++) {
			if (highlighter.isObjectSelected(dots[i].id)) {
				dots[i].isSelected = true;
				hasSelected = true;
			}
			if (highlighter.isObjectHighlighted(dots[i].id)) {
				dots[i].isHighlighted = true;
			}
		}
		return hasSelected;
	}

	/**
	 * The background image used for the optimisation of the drawing (helps in case
	 * of many objects).
	 */
	protected Image bkgImage = null;
	/**
	 * Indicates whether the background image is valid
	 */
	protected boolean bkgImageValid = false;
	/**
	 * The image with the whole plot, which is used for the optimisation of the drawing
	 * (helps in case of many objects).
	 */
	protected Image plotImage = null;
	/**
	 * Indicates whether the full image is valid
	 */
	protected boolean plotImageValid = false;

	/**
	 * Must be defined in descendants!
	 */
	public void drawReferenceFrame(Graphics g) {
	}

	/**
	 * Must be defined in descendants!
	 */
	public void countScreenCoordinates() {
	}

	public void drawAllInInactiveMode(Graphics g) {
		g.setColor(filteredColor);
		for (PlotObject dot : dots)
			if (isPointInPlotArea(dot.x, dot.y)) {
				dot.draw(g);
			}
	}

	public void drawOnlyActive(Graphics g) {
		for (int i = 0; i < dots.length; i++) {
			if (dots[i].isActive && isPointInPlotArea(dots[i].x, dots[i].y)) {
				g.setColor(getColorForPlotObject(i));
				dots[i].draw(g);
			}
		}
	}

	@Override
	public void draw(Graphics g) {
		if (bounds == null || !hasData())
			return;
		//System.out.println("1. "+System.currentTimeMillis());
		if (isHidden) {
			g.setColor(bkgColor);
			g.fillRect(bounds.x, bounds.y, bounds.width + 1, bounds.height + 1);
			return;
		}
		prepareImages();
		if (!plotImageValid) {
			if (!bkgImageValid) {
				Graphics ig = (bkgImage != null) ? bkgImage.getGraphics() : null;
				if (ig == null) {
					ig = g;
				}
				drawReferenceFrame(ig);
				countScreenCoordinates();
				// draw points filtered out
				drawAllInInactiveMode(ig);
				if (bkgImage != null) {
					bkgImageValid = true;
				}
			}
			Graphics ig = (plotImage != null) ? plotImage.getGraphics() : null;
			if (ig == null) {
				ig = g;
			}
			if (bkgImageValid) {
				ig.setClip(bounds.x, bounds.y, bounds.width, bounds.height);
				ig.drawImage(bkgImage, 0, 0, null);
				ig.setClip(0, 0, canvas.getWidth() + 1, canvas.getHeight() + 1);
			}
			// draw not-filtered points
			drawOnlyActive(ig);
			if (plotImage != null) {
				plotImageValid = true;
			}
		}
		if (plotImageValid) {
			g.setClip(bounds.x, bounds.y, bounds.width, bounds.height);
			g.drawImage(plotImage, 0, 0, null);
			g.setClip(0, 0, canvas.getWidth() + 1, canvas.getHeight() + 1);
		}
		// draw selected points
		drawAllSelectedObjects(g); //selected objects should not be covered by others
		//System.out.println("7. "+System.currentTimeMillis());
		if (annotationSurfacePresent()) {
			getAnnotationSurface().paint(g);
			//System.out.println("8. "+System.currentTimeMillis());
		}
	}

	public void invalidateImages() {
		plotImageValid = false;
		bkgImageValid = false;
	}

	/**
	* Determines the color in which the plot object with the given index should be
	* drawn. The color depends on the highlighting/selection state.
	* If multi-color brushing is used (e.g. broadcasting of classification),
	* the supervisor knows about the appropriate color.
	*/
	protected Color getColorForPlotObject(int idx) {
		if (selectionEnabled) {
			if (dots[idx].isHighlighted)
				return activeColor;
			if (dots[idx].isSelected)
				return selectedColor;
		}
		if (!objectsAreColored)
			return normalColor;
		return supervisor.getColorForDataItem(dataTable.getDataItem(idx), dataTable.getEntitySetIdentifier(), dataTable.getContainerIdentifier(), normalColor);
	}

	/**
	* Transforms a real x-coordinate into a screen x-coordinate
	*/
	public abstract int mapX(double v);

	/**
	* Transforms a real y-coordinate into a screen y-coordinate
	*/
	public abstract int mapY(double v);

	/**
	* Transforms a screen x-coordinate into a real x-coordinate
	*/
	public abstract double absX(int x);

	/**
	* Transforms a screen y-coordinate into a real y-coordinate
	*/
	public abstract double absY(int y);

	/**
	* Notification from the highlighter about change of the set of objects to be
	* transiently highlighted.
	*/
	@Override
	public void highlightSetChanged(Object source, String setId, Vector hlObj) {
		if (canvas == null)
			return;
		if (!selectionEnabled || !hasData())
			return;
		if (!StringUtil.sameStrings(setId, dataTable.getEntitySetIdentifier()))
			return;
		boolean needRedrawSelected = false;
		hasHighlighted = false;
		Graphics g = canvas.getGraphics();
		for (int i = 0; i < dots.length; i++)
			if (dots[i].isActive) {
				if (dots[i].isHighlighted)
					if (hlObj == null || !StringUtil.isStringInVectorIgnoreCase(dots[i].id, hlObj)) {
						dots[i].isHighlighted = false;
						if (!isHidden && g != null && isPointInPlotArea(dots[i].x, dots[i].y)) {
							g.setColor(getColorForPlotObject(i));
							dots[i].draw(g);
							needRedrawSelected = true;
						}
					} else {
						;
					}
				else if (hlObj != null && StringUtil.isStringInVectorIgnoreCase(dots[i].id, hlObj)) {
					dots[i].isHighlighted = true;
					if (!isHidden && g != null && isPointInPlotArea(dots[i].x, dots[i].y)) {
						g.setColor(activeColor);
						dots[i].draw(g);
					}
				}
				hasHighlighted = hasHighlighted || dots[i].isHighlighted;
			}
		if (!isHidden && needRedrawSelected) {
			drawAllSelectedObjects(g);
		}
		if (g != null) {
			g.dispose();
		}
	}

	/**
	* Notification from the highlighter about change of the set of objects to be
	* selected (durably highlighted).
	*/
	@Override
	public void selectSetChanged(Object source, String setId, Vector hlObj) {
		if (canvas == null)
			return;
		if (!selectionEnabled || !hasData())
			return;
		if (!StringUtil.sameStrings(setId, dataTable.getEntitySetIdentifier()))
			return;
		//System.out.print("selectSetChanged:");
		boolean needRedrawSelected = false;
		hasSelected = false;
		Graphics g = canvas.getGraphics();
		for (int i = 0; i < dots.length; i++)
			if (dots[i].isActive) {
				if (dots[i].isSelected)
					if (hlObj == null || !StringUtil.isStringInVectorIgnoreCase(dots[i].id, hlObj)) {
						dots[i].isSelected = false;
						if (!isHidden && g != null && isPointInPlotArea(dots[i].x, dots[i].y)) {
							g.setColor(getColorForPlotObject(i));
							dots[i].draw(g);
							needRedrawSelected = true;
						}
					} else {
						;
					}
				else if (hlObj != null && StringUtil.isStringInVectorIgnoreCase(dots[i].id, hlObj)) {
					dots[i].isSelected = true;
					if (!isHidden && g != null && isPointInPlotArea(dots[i].x, dots[i].y)) {
						g.setColor(selectedColor);
						dots[i].draw(g);
					}
					//System.out.print(" "+dots[i].id);
				}
				hasSelected = hasSelected || dots[i].isSelected;
			}
		//System.out.println();
		if (!isHidden && needRedrawSelected) {
			drawAllSelectedObjects(g);
		}
		if (g != null) {
			g.dispose();
		}
	}

	protected void drawAllSelectedObjects(Graphics g) {
		if (isHidden || g == null || !selectionEnabled || !hasData())
			return;
		if (hasSelected) {
			g.setColor(selectedColor);
			for (PlotObject dot : dots)
				if (dot.isSelected && isPointInPlotArea(dot.x, dot.y))
					if (dot.isActive) {
						dot.draw(g);
					}
		}
		if (hasHighlighted) {
			g.setColor(activeColor);
			for (PlotObject dot : dots)
				if (dot.isHighlighted && isPointInPlotArea(dot.x, dot.y))
					if (dot.isActive) {
						dot.draw(g);
					}
		}
	}

	protected boolean allowClearSelection = true; // click to empty space means
													// clearing the selection
													// to be set to false in cross-classification
													// for supporting class hiding

	/**
	* Method for selecting object(s) located at a given point.
	* returns true if at least one object is selected, otherwise false
	*/
	protected boolean selectObjectAt(int x, int y, MouseEvent sourceME) { //select the clicked object
		if (isHidden || !selectionEnabled || objEvtHandler == null || !hasData() || !isPointInPlotArea(x, y))
			return true;
		ObjectEvent oevt = new ObjectEvent(this, (sourceME.getClickCount() > 1) ? ObjectEvent.dblClick : ObjectEvent.click, sourceME, dataTable.getEntitySetIdentifier());
		//System.out.print("mouse clicked:");
		for (PlotObject dot : dots)
			if (dot.isActive && dot.contains(x, y)) {
				oevt.addEventAffectedObject(dot.id);
				//System.out.print(" "+dots[i].id);
			}
		//System.out.println();
		if (oevt.getAffectedObjectCount() < 1 && (!allowClearSelection || !hasSelected))
			return false; //to avoid unnecessary messages to highlighter about mouse clicks
		//in the object-free area
		objEvtHandler.processObjectEvent(oevt);
		return true;
	}

	protected boolean isPointInPlotArea(int x, int y) {
		if (isHidden || bounds == null)
			return false;
		return x >= bounds.x + mx1 && x <= bounds.x + mx1 + width + 1 && y >= bounds.y + my1 && y <= bounds.y + my1 + height + 1;
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	} //see mouseReleased

	@Override
	public void mouseExited(MouseEvent e) { //dehighlight all highlighted objects
		if (isHidden || canvas == null || !canvas.isShowing() || !selectionEnabled || objEvtHandler == null || !hasData())
			return;
		//if (sRec!=null) sRec.setActive(false);
		ObjectEvent oevt = new ObjectEvent(this, ObjectEvent.point, e, dataTable.getEntitySetIdentifier()); //no objects to highlight
		objEvtHandler.processObjectEvent(oevt);
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		if (isHidden || canvas == null || !canvas.isShowing() || !selectionEnabled || objEvtHandler == null || !hasData())
			return;
		int x = e.getX(), y = e.getY();
		if (!isPointInPlotArea(x, y)) {
			if (hasHighlighted) {
				ObjectEvent oevt = new ObjectEvent(this, ObjectEvent.point, e, dataTable.getEntitySetIdentifier());
				//to dehighlight all the objects
				objEvtHandler.processObjectEvent(oevt);
			}
			return;
		}
		ObjectEvent oevt = new ObjectEvent(this, ObjectEvent.point, e, dataTable.getEntitySetIdentifier());
		int count = 0;
		for (int i = 0; i < dots.length && count < 20; i++)
			if (dots[i].isActive && dots[i].contains(x, y)) {
				oevt.addEventAffectedObject(dots[i].id);
				++count;
			}
		if (oevt.getAffectedObjectCount() < 1 && !hasHighlighted)
			return;
		//to avoid unnecessary messages to highlighter about mouse
		//movements in the object-free area

		objEvtHandler.processObjectEvent(oevt);
	}

	@Override
	public void mouseDragged(MouseEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	}

	protected void drawFrame(int x0, int y0, int x, int y) {
		if (isHidden || canvas == null)
			return;
		if (x - x0 != 0 || y - y0 != 0) {
			Graphics gr = canvas.getGraphics();
			gr.setColor(Color.magenta);
			gr.setXORMode(plotAreaColor);
			gr.drawLine(x0, y0, x, y0);
			gr.drawLine(x, y0, x, y);
			gr.drawLine(x, y, x0, y);
			gr.drawLine(x0, y, x0, y0);
			gr.setPaintMode();
			gr.dispose();
		}
	}

	protected void selectInFrame(int x0, int y0, int x, int y, MouseEvent sourceME) {
		if (isHidden || !selectionEnabled || objEvtHandler == null || !hasData())
			return;
		if (x < x0) {
			int x1 = x0;
			x0 = x;
			x = x1;
		}
		if (y < y0) {
			int y1 = y0;
			y0 = y;
			y = y1;
		}
		Rectangle r = new Rectangle(x0, y0, x - x0 + 1, y - y0 + 1);
		ObjectEvent oevt = new ObjectEvent(this, ObjectEvent.frame, sourceME, dataTable.getEntitySetIdentifier());
		for (PlotObject dot : dots)
			if (dot.isActive && r.contains(dot.x, dot.y)) {
				oevt.addEventAffectedObject(dot.id);
			}
		objEvtHandler.processObjectEvent(oevt);
	}

	@Override
	public void focusChanged(Object source, double lowerLimit, double upperLimit) {
	}

	@Override
	public void limitIsMoving(Object source, int n, double currValue) {
	}

	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		//System.out.println("Plot "+toString()+"\n  received event from "+pce.getSource()+":\n"+
		//                   pce.getPropertyName()+" "+pce.getOldValue()+" "+pce.getNewValue());
		//long t0=System.currentTimeMillis();
		if (pce.getSource().equals(tf)) {
			if (pce.getPropertyName().equals("destroyed")) {
				tf.removePropertyChangeListener(this);
				tf = null;
			}
			if (!destroyed) {
				applyFilter();
				plotImageValid = false;
				redraw();
			}
		} else if (pce.getSource().equals(aligner)) {
			redraw();
		} else if (pce.getPropertyName().equals(Supervisor.eventObjectColors)) {
			if (dataTable.getEntitySetIdentifier().equals(pce.getNewValue())) {
				plotImageValid = false;
				redraw();
			}
		} else if (pce.getSource().equals(dataTable)) {
			if (pce.getPropertyName().equals("destroyed") || pce.getPropertyName().equals("structure_complete")) {
				destroy();
			} else if (pce.getPropertyName().equals("filter")) {
				if (tf != null) {
					tf.removePropertyChangeListener(this);
				}
				tf = dataTable.getObjectFilter();
				if (tf != null) {
					tf.addPropertyChangeListener(this);
				}
				applyFilter();
				plotImageValid = false;
				redraw();
			} else if (pce.getPropertyName().equals("values")) {
				applyFilter();
				plotImageValid = false;
				if (reactToTableDataChange) {
					Vector v = (Vector) pce.getNewValue(); // list of changed attributes
					if (reloadAttributeData(v)) {
						redraw();
					}
				}
			} else if (pce.getPropertyName().equals("data_added") || pce.getPropertyName().equals("data_removed") || pce.getPropertyName().equals("data_updated")) {
				reset();
				if (checkWhatSelected()) {
					redraw();
				}
			}
		} else if (pce.getSource().equals(aTrans)) {
			if (pce.getPropertyName().equals("values")) {
				reset();
			} else if (pce.getPropertyName().equals("destroyed")) {
				destroy();
			}
		}
		//long t=System.currentTimeMillis();
		//System.out.println("Event processed during "+(t-t0)+" msec");
	}

	/**
	* Resets its internal data, including the array of screen objects.
	* Called when records are added to or removed from the table.
	*/
	public abstract void reset();

	/*
	* reloads new data for attributes specified in the Vector which
	* contains identifiers of attributes.
	* returns TRUE if repainting is needed, otherwise FALSE
	* (if attributes are not involved in the plot or
	* if repainting is already done
	*/
	public abstract boolean reloadAttributeData(Vector v);

	/**
	* Makes necessary operations for destroying, e.g. unregisters from
	* listening highlighting and other events.
	*/
	@Override
	public void destroy() {
		if (canvas != null) {
			if (annotationSurfacePresent()) {
				canvas.removeMouseListener(getAnnotationSurface().removeMouseRedirector(this));
				canvas.removeMouseMotionListener(getAnnotationSurface().removeMouseMotionRedirector(this));
			} else {
				canvas.removeMouseListener(this);
				canvas.removeMouseMotionListener(this);
			}
		}
		if (supervisor != null) {
			if (dataTable != null) {
				supervisor.removeHighlightListener(this, dataTable.getEntitySetIdentifier());
			}
			supervisor.removePropertyChangeListener(this);
			if (isIndependentEventSource) {
				supervisor.removeObjectEventSource(this);
				supervisor.removeDataDisplayer(this);
			}
		}
		if (tf != null) {
			tf.removePropertyChangeListener(this);
		}
		if (dataTable != null) {
			dataTable.removePropertyChangeListener(this);
		}
		if (aTrans != null) {
			aTrans.removePropertyChangeListener(this);
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

	/**
	* Checks whether object coloring (classes) is propagated among the system's
	* component.
	*/
	protected void checkObjectColorPropagation() {
		objectsAreColored = dataTable != null && supervisor != null && supervisor.getObjectColorer() != null && !supervisor.getObjectColorer().equals(this)
				&& supervisor.getObjectColorer().getEntitySetIdentifier().equals(dataTable.getEntitySetIdentifier());
	}

	public void prepareImages() {
		if (canvas == null || bounds == null)
			return;
		if ((plotImage != null && (plotImage.getWidth(null) != bounds.x + bounds.width || plotImage.getHeight(null) != bounds.y + bounds.height))
				|| (bkgImage != null && (bkgImage.getWidth(null) != bounds.x + bounds.width || bkgImage.getHeight(null) != bounds.y + bounds.height))) {
			plotImage = null;
			bkgImage = null;
			plotImageValid = bkgImageValid = false;
		}
		if (plotImage == null) {
			plotImage = canvas.createImage(bounds.x + bounds.width, bounds.y + bounds.height);
			plotImageValid = false;
		}
		if (bkgImage == null) {
			bkgImage = canvas.createImage(bounds.x + bounds.width, bounds.y + bounds.height);
			bkgImageValid = false;
		}
	}

	public void redraw() {
		if (canvas == null || bounds == null || !hasData())
			return;
		checkObjectColorPropagation();
		Graphics g = canvas.getGraphics();
		if (g == null)
			return;
		if (isHidden) {
			g.setColor(bkgColor);
			g.fillRect(bounds.x, bounds.y, bounds.width + 1, bounds.height + 1);
		} else {
			if (plotImageValid && plotImage != null) {
				g.setClip(bounds.x, bounds.y, bounds.width, bounds.height);
				g.drawImage(plotImage, 0, 0, null);
				g.setClip(0, 0, canvas.getWidth() + 1, canvas.getHeight() + 1);
			} else {
				draw(g);
			}
		}
		g.dispose();
	}

	public void setAligner(Aligner al) {
		aligner = al;
		if (!mayDefineAlignment) {
			aligner.addPropertyChangeListener(this);
		}
	}

	public void setMayDefineAlignment(boolean value) {
		if (mayDefineAlignment == value)
			return;
		mayDefineAlignment = value;
		if (aligner != null && mayDefineAlignment) {
			aligner.removePropertyChangeListener(this);
		}
	}

//--------------------- DataTreater interface -----------------------------
	/**
	* Returns vector of IDs of attribute(s) on this display
	*/
	@Override
	public abstract Vector getAttributeList();

	/**
	* Replies whether it is linked to the data set (table) with the given identifier
	*/
	@Override
	public boolean isLinkedToDataSet(String setId) {
		return setId != null && dataTable != null && setId.equals(dataTable.getContainerIdentifier());
	}

	/**
	* A method from the DataTreater interface.
	* Must return a vector of colors used for representation of the attributes
	* this Data Treater deals with. By default, returns null.
	*/
	@Override
	public Vector getAttributeColors() {
		return null;
	}

//---------------- implementation of the EventSource interface ------------
	/**
	* The EventSource answers whether it can produce the specified
	* type of events.
	*/
	@Override
	public boolean doesProduceEvent(String eventId) {
		return isIndependentEventSource && eventId != null && (eventId.equals(ObjectEvent.click) || eventId.equals(ObjectEvent.dblClick) || eventId.equals(ObjectEvent.point) || eventId.equals(ObjectEvent.frame));
	}

	/**
	* Returns a unique identifier of the event source (may be produced
	* automatically, used only internally, not shown to the user).
	* The identifier is used for explicit linking of producers and recipients of
	* object events.
	* To produce an identifier, the base class Plot uses the methods
	* getPlotTypeName() and getInstanceN() that are to be defined in the
	* descendants
	*/
	@Override
	public String getIdentifier() {
		if (isIndependentEventSource)
			return plotId;
		return null;
	}

	/**
	* Sets the unique identifier of the map view.
	*/
	public void setIdentifier(String id) {
		if (id != null) {
			plotId = id;
		}
	}

	public int getInstanceN() {
		return instanceN;
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
	* Returns the specification of this plot (normally an instance of the
	* class spade.vis.spec.ToolSpec) for storing in a file.
	*/
	@Override
	public Object getSpecification() {
		ToolSpec spec = new ToolSpec();
		spec.tagName = getTagName();
		spec.methodId = getMethodId();
		spec.chartId = getIdentifier();
		if (dataTable != null) {
			spec.table = dataTable.getContainerIdentifier();
		}
		spec.attributes = getAttributeList();
		if (aTrans != null) {
			spec.transformSeqSpec = aTrans.getSpecSequence();
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

//ID
	@Override
	public Image getImage() {
		int gap = 2, indent = 20;
		StringBuffer sb = new StringBuffer();
		StringInRectangle str = new StringInRectangle();
		str.setPosition(StringInRectangle.Left);
		for (int i = 0; i < getAttributeList().size(); i++) {
			if (getAttributeList().size() > 1)
				if (getAttributeList().size() == 2) {
					sb.append((char) ('X' + i) + ": ");
				} else {
					sb.append((i + 1) + ": ");
				}
			else {
				str.setPosition(StringInRectangle.Center);
				indent = 0;
			}
			sb.append(dataTable.getAttributeName(dataTable.getAttrIndex((String) getAttributeList().elementAt(i))));
			if (i < getAttributeList().size() - 1) {
				sb.append("\n");
			}
		}
		str.setString(sb.toString());
		Dimension lsize = str.countSizes(getCanvas().getGraphics());
		int w = getCanvas().getBounds().width;
		int h = getCanvas().getBounds().height;
		str.setRectSize(w - indent, lsize.width * lsize.height / w * 2);
		lsize = str.countSizes(getCanvas().getGraphics());
		Image li = getCanvas().createImage(w - indent, lsize.height);
		str.draw(li.getGraphics());
		Image img = getCanvas().createImage(w, h + lsize.height + 2 * gap);
		Image pi = getCanvas().createImage(w, h);
		draw(pi.getGraphics());
		img.getGraphics().drawImage(li, indent, gap, null);
		img.getGraphics().drawImage(pi, 0, lsize.height + 2 * gap, null);
		return img;
	}

	@Override
	public String getName() {
		String name = PlotGeneratorsDescriptor.getToolName(getMethodId()) + " " + getInstanceN();
		if (name == null) {
			name = getMethodId() + " " + getInstanceN();
		}
		return name;
	}

//~ID
//ID
	// IDbad - dirty hacks
	public PlotObject[] getPlotObjects() {
		return dots;
	}

	public AttributeDataPortion getDataTable() {
		return dataTable;
	}

	// --------------------  annotation support  --------------------------
	private String surfaceClass = "connection.observer.annotation.PlotAnnotationSurface";

	protected AnnotationSurfaceInterface annotationSurface;
	protected boolean presenceChecked = false;

	@Override
	public boolean annotationSurfacePresent() {
		if (presenceChecked)
			return annotationSurface != null;
		return ScatterPlot.class.isInstance(this) && getAnnotationSurface() != null; // IDdebug - now it works only for scatter plot 
	}

	@Override
	public AnnotationSurfaceInterface getAnnotationSurface() {
		if (annotationSurface == null) {
			try {
				annotationSurface = (AnnotationSurfaceInterface) Class.forName(surfaceClass).newInstance();
				annotationSurface.connect(this);
			} catch (Exception ex) {
			}
		}
		presenceChecked = true;
		return annotationSurface;
	}
//~ID
}
