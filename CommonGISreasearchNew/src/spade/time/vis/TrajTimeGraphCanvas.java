package spade.time.vis;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.ScrollPane;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import spade.analysis.system.Supervisor;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.Metrics;
import spade.lib.util.BubbleSort;
import spade.lib.util.Comparator;
import spade.lib.util.IntArray;
import spade.lib.util.ListSelector;
import spade.lib.util.QSortAlgorithm;
import spade.lib.util.StringUtil;
import spade.time.Date;
import spade.time.TimeMoment;
import spade.time.TimePositionNotifier;
import spade.time.TimeReference;
import spade.time.TimeUtil;
import spade.time.ui.CanvasWithTimePosition;
import spade.vis.action.HighlightListener;
import spade.vis.action.ObjectEvent;
import spade.vis.database.AttributeTypes;
import spade.vis.database.CombinedFilter;
import spade.vis.database.DataItem;
import spade.vis.database.DataTreater;
import spade.vis.database.ObjectContainer;
import spade.vis.database.ObjectFilter;
import spade.vis.database.SpatialEntity;
import spade.vis.database.ThematicDataItem;
import spade.vis.database.ThematicDataOwner;
import spade.vis.database.TimeFilter;
import spade.vis.dmap.DMovingObject;
import spade.vis.event.EventSource;
import spade.vis.geometry.RealPoint;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 01-Feb-2007
 * Time: 16:49:27
 * Represents a set of events (time-referenced items in a ObjectContainer)
 * in a plane where the horizontal dimension represents time and the vertical
 * dimension is used to position the events. Instant events are represented as
 * small circles and prolonged as horizontal bars.
 */
public class TrajTimeGraphCanvas extends CanvasWithTimePosition implements PropertyChangeListener, HighlightListener, MouseListener, MouseMotionListener, EventSource, DataTreater, Comparator, Destroyable {
	protected static Color normalColor = new Color(120, 120, 120), filteredColor = new Color(168, 168, 168), activeColor = Color.white, selectedColor = Color.black;
	protected int unitSize = 5;

	public void setUnitSize(int unitSize) {
		if (this.unitSize == unitSize)
			return;
		this.unitSize = unitSize;
		plotImageValid = false;
		if (isShowing()) {
			resize();
		}
	}

	/**
	 * visual appearance: time graph or time line
	 */
	protected boolean isTGraphMode = true;

	public void setIsTGraphMode(boolean TGraphMode) {
		if (isTGraphMode == TGraphMode)
			return;
		isTGraphMode = TGraphMode;
		if (grObjects != null) {
			for (int i = 0; i < grObjects.size(); i++) {
				TrajTimeLineObject ttlo = grObjects.get(i);
				ttlo.setIsTGraphMode(TGraphMode);
				if (isTGraphMode && ttlo.vPos == null) {
					ttlo.vPos = (isTGraphMode && dAttrVals != null && dAttrVals[ttlo.idx] != null) ? new int[1 + dAttrVals[ttlo.idx].length] : null;
				}
			}
		}
		plotImageValid = false;
		//resize();
		redraw();
	}

	/**
	 * Colors of the background of the canvas and of the plot area
	 */
	protected Color bkgColor = new Color(192, 192, 192), outOfFilterBkgColor = new Color(150, 150, 150);
	/**
	 * The container with time-referenced items (events), which are visualised
	 */
	protected ObjectContainer oCont = null;
	/**
	 * The earliers and the latest time references among the objects in the
	 * container.
	 */
	protected TimeMoment dataStart = null, dataEnd = null;
	/**
	 * The currently shown time interval (selected by time focuser)
	 */
	protected TimeMoment start = null, end = null;
	/**
	 * The length of the time interval
	 */
	protected int timeLen = 0;
	/**
	 * ObjectFilter may be associated with the set of objects and contain results
	 * of data querying. Data not satisfying the current query are displayed
	 * in a "neutral" grey and to not react to mouse events.
	 */
	protected ObjectFilter filter = null;
	/**
	 * The TimeFilter associated with the data
	 */
	protected TimeFilter timeFilter = null;
	/**
	 * Supervisor provides access of a plot to the Highlighter (common for
	 * all data displays) and in this way links together all displays
	 */
	protected Supervisor supervisor = null;
	/**
	 * Assignments of colors to time intervals.
	 * Contains arrays Object[2] = [TimeMoment,Color],
	 * where the time moments are the starting moments of the intervals.
	 * The intervals are chronologically sorted.
	 */
	protected Vector<Object[]> colorsForTimes = null;
	/**
	 * The graphical objects, instances of TimeLineObject, representing the
	 * members of the container.
	 */
	protected Vector<TrajTimeLineObject> grObjects = null;
	/**
	* Indicates whether the objects on the plot are currently colored according
	* to some propagated classification.
	*/
	protected boolean objectsAreColored = false;
	/**
	 * A ListSelector specifies which objects should be drawn
	 */
	protected ListSelector listSelector = null;
	/**
	 * The identifiers of the attributes used to sort and group the objects
	 * before drawing
	 */
	protected Vector sortAttrIds = null;
	/**
	 * The indexes of the attributes used for sorting and grouping
	 */
	protected int sortAttrIdxs[] = null;
	/**
	 * Indicates whether the values of the attributes used for sorting are
	 * numbers (true) or objects (false)
	 */
	protected boolean sortAttrNumeric[] = null;
	/**
	 * sort type: asc or desc
	 */
	protected boolean isSortAsc = true;
	/**
	 * If the objects have attributes, this is the list of attributes to be
	 * shown in popup windows
	 */
	protected Vector attributes = null;
	/**
	 * Indicates that the display must show only active events
	 * (i.e. satisfying all filters)
	 */
	protected boolean showOnlyActiveEvents = false;
	/**
	 * The width and height of the drawing (which may differ from the width and
	 * height of the canvas)
	 */
	protected int drWidth = 0, drHeight = 0;
	/**
	 * The canvas in which labels are drawn (to remain fixed independently of
	 * the scroller position)
	 */
	protected TimeLineLabelsCanvas tLabCanvas[] = null;
	/**
	* Indicates "destroyed" state. Initially is false.
	*/
	protected boolean destroyed = false;
	/**
	* The unique identifier of this plot. The identifier is used
	* 1) for explicit linking of producers and recipients of object events;
	* 2) for correct restoring of system states with multiple plots.
	*/
	protected String plotId = null;
	/**
	* Used to generate unique identifiers of instances of TimeLineCanvas
	*/
	protected static int instanceN = 0;
	/**
	 * range of values of the displayed attribute
	 */
	protected double dMin = Float.NaN, dMax = Float.NaN;
	/**
	 * values of the attriobutes along the trajectories
	 */
	protected double dAttrVals[][] = null;

	public double[] getAttrVals(int dmoIdx) {
		if (dAttrVals == null || dmoIdx >= dAttrVals.length)
			return null;
		else
			return dAttrVals[dmoIdx];
	}

	/**
	 * class breaks and corresponding colors
	 */
	protected float breaks[] = null;
	protected Color colors[] = null;
	/**
	 * Maximum length of time interval of value validity.
	 * If negative, ignored.
	 */
	protected long maxValueValidityTime = -1;

	protected boolean highlightingIsActive = true;

	public void setHighlightingIsActive(boolean highlightingIsActive) {
		this.highlightingIsActive = highlightingIsActive;
	}

	public TrajTimeGraphCanvas() {
		super();
		++instanceN;
		plotId = "traj_time_graph_canvas_" + instanceN;
		setBackground(bkgColor);
	}

	public void setObjectContainer(ObjectContainer oCont) {
		this.oCont = oCont;
		if (oCont != null) {
			oCont.addPropertyChangeListener(this);
			filter = oCont.getObjectFilter();
			if (filter != null) {
				findTimeFilter();
			}
			if (supervisor != null) {
				supervisor.registerHighlightListener(this, oCont.getEntitySetIdentifier());
				supervisor.addPropertyChangeListener(this);
				supervisor.registerObjectEventSource(this);
			}
		}
	}

	/**
	 * Initial setup; also called when the data change
	 */
	public void setup() {
		plotImageValid = false;
		if (grObjects != null) {
			grObjects.removeAllElements();
		}
		dataStart = null;
		dataEnd = null;
		timeLen = 0;
		if (oCont == null || oCont.getObjectCount() < 1)
			return;
		if (grObjects == null) {
			grObjects = new Vector<TrajTimeLineObject>(oCont.getObjectCount(), 100);
		}
		for (int i = 0; i < oCont.getObjectCount(); i++) {
			DataItem dit = oCont.getObjectData(i);
			if (dit == null) {
				continue;
			}
			TimeReference tref = dit.getTimeReference();
			if (tref == null) {
				continue;
			}
			TimeMoment t1 = tref.getValidFrom();
			if (t1 == null) {
				continue;
			}
			TrajTimeLineObject tlObj = new TrajTimeLineObject();
			tlObj.id = dit.getId();
			tlObj.idx = i;
			tlObj.t1 = t1;
			if (dataStart == null || t1.compareTo(dataStart) < 0) {
				dataStart = t1;
			}
			if (dataEnd == null || t1.compareTo(dataEnd) > 0) {
				dataEnd = t1;
			}
			TimeMoment t2 = tref.getValidUntil();
			if (t2 != null && t2.compareTo(t1) > 0) {
				tlObj.t2 = t2;
				tlObj.instant = false;
				if (t2.compareTo(dataEnd) > 0) {
					dataEnd = t2;
				}
			} else {
				tlObj.instant = true;
			}
			grObjects.addElement(tlObj);
		}
		if (dataStart != null) {
			dataStart = dataStart.getCopy();
		}
		if (dataEnd != null) {
			dataEnd = dataEnd.getCopy();
		}
		accountForQuery();
		checkObjectColorPropagation();
		QSortAlgorithm.sort(grObjects, this, false);
		visSetup();
	}

	protected void visSetup() {
		plotImageValid = false;
		timeLen = 0;
		if (dataStart == null || dataEnd == null)
			return;
		if (start == null || start.compareTo(dataStart) < 0) {
			start = dataStart.getCopy();
			if (start instanceof Date) {
				((Date) start).setHighestPossiblePrecision();
			}
		}
		if (end == null || end.compareTo(dataEnd) > 0) {
			end = dataEnd.getCopy();
			if (end instanceof Date) {
				((Date) end).setHighestPossiblePrecision();
			}
		}
		if (start != null && end != null) {
			if (dataStart instanceof Date) {
				int sw = Toolkit.getDefaultToolkit().getScreenSize().width;
				Date startDate = (Date) start.getCopy(), endDate = (Date) end.getCopy();
				char prec = startDate.getPrecision(), prec1 = prec;
				for (char time_symbol : Date.time_symbols)
					if (startDate.hasElement(time_symbol)) {
						prec1 = time_symbol;
						//startDate.setPrecision(prec1);
						//endDate.setPrecision(prec1);
						long diff = endDate.subtract(startDate, prec1);
						if (diff > sw / 2) {
							break;
						}
					}
				if (prec1 != prec) {
					startDate = (Date) start;
					endDate = (Date) end;
					startDate.setPrecision(prec1);
					endDate.setPrecision(prec1);
					startDate.roundDown();
					endDate.roundUp();
				}
			}
			timeLen = (int) end.subtract(start);
			//System.out.println("timeLen="+timeLen+"; precision="+start.getPrecision());
		}
		if (tLabCanvas != null) {
			for (TimeLineLabelsCanvas tLabCanva : tLabCanvas) {
				tLabCanva.setTimeInterval(start, end);
			}
		}
	}

	public TimeMoment getDataStart() {
		return dataStart;
	}

	public TimeMoment getDataEnd() {
		return dataEnd;
	}

	public TimeMoment getStart() {
		return start;
	}

	public TimeMoment getEnd() {
		return end;
	}

	public void setAttrVals(double dAttrVals[][]) {
		this.dAttrVals = dAttrVals;
		for (int i = 0; i < grObjects.size(); i++) {
			TrajTimeLineObject obj = grObjects.elementAt(i);
			if (obj == null) {
				continue;
			}
			obj.tPos = (dAttrVals[obj.idx] == null) ? null : new int[dAttrVals[obj.idx].length];
			obj.vPos = (dAttrVals[obj.idx] == null) ? null : new int[dAttrVals[obj.idx].length];
		}
	}

	public double[][] getAttrVals() {
		return dAttrVals;
	}

	public void setBreaksAndFocuserMinMax(float breaks[], Color colors[], double min, double max) {
		this.breaks = breaks;
		this.colors = colors;
		this.dMin = min;
		this.dMax = max;
		setupBreaks();
	}

	protected void setupBreaks() {
		plotImageValid = false;
		if (grObjects == null)
			return;
		//if (!isTGraphMode)
		for (int i = 0; i < grObjects.size(); i++) {
			TrajTimeLineObject obj = grObjects.elementAt(i);
			DMovingObject dmo = (DMovingObject) oCont.getObject(obj.idx);
			if (obj == null || dAttrVals == null || obj.idx < 0 || dAttrVals[obj.idx] == null) {
				obj.colors = null;
				continue;
			} else {
				if (obj.colors == null) {
					obj.colors = new Color[dAttrVals[obj.idx].length];
				}
				for (int j = 0; j < dAttrVals[obj.idx].length; j++) {
					if ((!Double.isNaN(dMin) && !Double.isNaN(dMax) && (Double.isNaN(dAttrVals[obj.idx][j]) || dAttrVals[obj.idx][j] < dMin || dAttrVals[obj.idx][j] > dMax)) || !dmo.isSegmActive(j)) {
						obj.colors[j] = null;
					} else if (colors == null || breaks == null) {
						obj.colors[j] = (Double.isNaN(dAttrVals[obj.idx][j])) ? null : Color.GRAY;
					} else {
						obj.colors[j] = null;
						for (int c = 0; c < colors.length && obj.colors[j] == null; c++)
							if (c == colors.length - 1) {
								obj.colors[j] = colors[colors.length - 1];
							} else if (dAttrVals[obj.idx][j] < breaks[c]) {
								obj.colors[j] = colors[c];
							}
					}
				}
			}
		}
		if (isShowing()) {
			redraw();
		}
	}

	public void setFocusInterval(TimeMoment t1, TimeMoment t2) {
		if (t1 == null || t2 == null)
			return;
		if (t2.subtract(t1) < 1)
			return;
		start = t1.getCopy();
		end = t2.getCopy();
		if (start instanceof Date) {
			((Date) start).setHighestPossiblePrecision();
			((Date) end).setHighestPossiblePrecision();
		}
/*
    start.setPrecision(dataStart.getPrecision());
    end.setPrecision(dataStart.getPrecision());
*/
		visSetup();
		plotImageValid = false;
		if (isShowing()) {
			redraw();
		}
	}

	protected void findTimeFilter() {
		timeFilter = null;
		if (filter != null)
			if (filter instanceof TimeFilter) {
				timeFilter = (TimeFilter) filter;
			} else if (filter instanceof CombinedFilter) {
				CombinedFilter cf = (CombinedFilter) filter;
				for (int i = 0; i < cf.getFilterCount() && timeFilter == null; i++)
					if (cf.getFilter(i) instanceof TimeFilter) {
						timeFilter = (TimeFilter) cf.getFilter(i);
					}
			}
	}

	public void setSupervisor(Supervisor supervisor) {
		this.supervisor = supervisor;
		if (supervisor != null && oCont != null) {
			supervisor.registerHighlightListener(this, oCont.getEntitySetIdentifier());
			supervisor.addPropertyChangeListener(this);
			supervisor.registerObjectEventSource(this);
			tryGetColorsForTimes();
			if (isShowing() && !plotImageValid) {
				redraw();
			}
		}
	}

	/**
	 * Tries to get from the supervisor the assignment of colors to times
	 */
	protected void tryGetColorsForTimes() {
		Vector cftOld = colorsForTimes;
		colorsForTimes = null;
		if (supervisor != null && supervisor.coloredObjectsAreTimes()) {
			Vector assignment = supervisor.getColorsForTimes();
			if (assignment != null) {
				colorsForTimes = TimeUtil.getColorsForTimeIntervals(assignment);
			}
			if (colorsForTimes != null) {
				//check the validity of the time intervals
				TimeMoment t0 = dataStart, tEnd = dataEnd;
				for (int i = colorsForTimes.size() - 1; i >= 0; i--) {
					TimeMoment t = (TimeMoment) colorsForTimes.elementAt(i)[0];
					boolean valid = t != null && t.getClass().getName().equals(t0.getClass().getName()) && t.compareTo(t0) >= 0 && t.compareTo(tEnd) <= 0;
					if (!valid) {
						colorsForTimes.removeElementAt(i);
					}
				}
				if (colorsForTimes.size() < 1) {
					colorsForTimes = null;
				}
			}
		}
		if (colorsForTimes != null || cftOld != null) {
			plotImageValid = false;
		}
		if (tLabCanvas != null) {
			for (TimeLineLabelsCanvas tLabCanva : tLabCanvas) {
				tLabCanva.setColorsForTimes(colorsForTimes);
			}
		}
	}

	public void setTimeLineLabelsCanvas(TimeLineLabelsCanvas tLabCanvas[]) {
		this.tLabCanvas = tLabCanvas;
		if (tLabCanvas != null) {
			for (TimeLineLabelsCanvas tLabCanva : tLabCanvas) {
				tLabCanva.setBackground(getBackground());
			}
		}
	}

	/**
	 * A ListSelector specifies which objects should be drawn
	 */
	public void setListSelector(ListSelector listSelector) {
		this.listSelector = listSelector;
		if (listSelector != null) {
			listSelector.addSelectListener(this);
		}
	}

	/**
	 * Sets whether the display must show only active events
	 * (i.e. satisfying all filters)
	 */
	public void setShowOnlyActiveEvents(boolean showOnlyActiveEvents) {
		if (this.showOnlyActiveEvents == showOnlyActiveEvents)
			return;
		this.showOnlyActiveEvents = showOnlyActiveEvents;
		plotImageValid = false;
		if (isShowing()) {
			if (getParent() != null && (getParent() instanceof ScrollPane)) {
				resize();
			} else {
				redraw();
			}
		}
	}

	protected ScrollPane getScrollPane() {
		Component p = getParent();
		while (p != null && !(p instanceof ScrollPane)) {
			p = p.getParent();
		}
		if (p != null && (p instanceof ScrollPane))
			return (ScrollPane) p;
		return null;
	}

	protected int prefH = -1;

	protected int getPreferredWidth() {
		if (!isShowing())
			return drWidth;
		Dimension size = getSize();
		if (size == null)
			return drWidth;
		int w = size.width;
		ScrollPane scp = getScrollPane();
		if (scp == null)
			return w;
		Dimension ss = scp.getViewportSize();
		if (ss == null)
			return w;
		w = ss.width;
		if (ss.height >= prefH) {
			w -= scp.getVScrollbarWidth() - 3;
		}
		return w;
	}

	protected int getPreferredHeight() {
		if (!isShowing())
			return prefH;
		Dimension size = getSize();
		if (size == null)
			return prefH;
		int h = size.height;
		ScrollPane scp = getScrollPane();
		if (scp == null)
			return h;
		Dimension ss = scp.getViewportSize();
		if (ss == null)
			return h;
		return ss.height;
	}

	@Override
	public Dimension getPreferredSize() {
		if (grObjects == null) {
			setup();
		}
		if (grObjects == null || grObjects.size() < 1 || timeLen < 1)
			return new Dimension(100, 100);
		int prefW = getPreferredWidth();
		if (prefW > 10 && prefH >= unitSize)
			return new Dimension(prefW, prefH);
		Dimension ss = Toolkit.getDefaultToolkit().getScreenSize();
		int nObjects = countActiveObjects();
		int w = 2 * timeLen, h = (this.isTGraphMode) ? ss.height / 2 : (unitSize + 2) * nObjects + 10;
		if (w < 10) {
			w = 100;
		}
		if (h < unitSize) {
			h = 100;
		}
		if (isShowing()) {
			Dimension sz = getSize();
			if (sz != null && sz.width > 100)
				return new Dimension(sz.width, h);
		}
		if (w > ss.width * 2 / 3) {
			w = ss.width * 2 / 3;
		}
		return new Dimension(w, h);
	}

	protected void accountForQuery() {
		if (grObjects == null || grObjects.size() < 1)
			return;
		if (filter == null) {
			for (int i = 0; i < grObjects.size(); i++) {
				grObjects.elementAt(i).active = true;
			}
		} else {
			for (int i = 0; i < grObjects.size(); i++) {
				TrajTimeLineObject obj = grObjects.elementAt(i);
				obj.active = filter.isActive(obj.idx);
			}
		}
	}

	protected int countActiveObjects() {
		IntArray sel = null;
		if (listSelector != null && listSelector.isSelectionApplied()) {
			sel = listSelector.getSelectedIndexes();
			if (sel == null)
				return 0;
		}
		if (!showOnlyActiveEvents)
			if (sel == null)
				return grObjects.size();
			else
				return sel.size();
		int nObjects = 0;
		for (int i = 0; i < ((sel == null) ? grObjects.size() : sel.size()); i++) {
			TrajTimeLineObject obj = grObjects.elementAt((sel == null) ? i : sel.elementAt(i));
			if (obj.active && ((DMovingObject) oCont.getObject(obj.idx)).isAnySegmActive()) {
				++nObjects;
			}
		}
		return nObjects;
	}

	@Override
	protected int getTimeAxisPos(TimeMoment t) {
		if (t == null || start == null || timeLen < 1 || drWidth < 10)
			return -1;
		int pos = 0;
		synchronized (this) {
			pos = (int) Math.round(((double) t.subtract(start, start.getPrecision())) * drWidth / timeLen);
		}
		return pos;
	}

	@Override
	public TimeMoment getTimeForAxisPos(int pos) {
		if (drWidth < 10)
			return null;
		IntArray gridPos = tGrid.getGridPositions();
		if (gridPos != null && gridPos.size() > 1) {
			int step = gridPos.elementAt(1) - gridPos.elementAt(0);
			for (int i = 0; i < gridPos.size(); i++) {
				long diff = pos - gridPos.elementAt(i);
				if (Math.abs(diff) <= step / 2) {
					TimeMoment t = tGrid.getGridTimeMoment(i).getCopy();
					diff = Math.round(((double) diff) * timeLen / drWidth);
					t.setPrecision(start.getPrecision());
					t.add(diff);
					if (t instanceof Date) {
						((Date) t).setDateScheme(((Date) start).scheme);
					}
					return t;
				}
			}
		}
		long timePos = Math.round(((double) pos) * timeLen / drWidth);
		TimeMoment t = start.getCopy();
		t.add(timePos);
		return t;
	}

	/**
	* Determines the color in which the object with the given index should be
	* drawn. The color depends on the highlighting/selection state.
	* If multi-color brushing is used (e.g. broadcasting of classification),
	* the supervisor knows about the appropriate color.
	*/
	protected Color getColorForObject(TimeLineObject obj) {
		if (!obj.active)
			return filteredColor;
		if (obj.isHighlighted)
			return activeColor;
		if (obj.isSelected)
			return selectedColor;
		if (!objectsAreColored)
			return normalColor;
		return supervisor.getColorForDataItem(oCont.getObjectData(obj.idx), oCont.getEntitySetIdentifier(), oCont.getContainerIdentifier(), normalColor);
	}

	/**
	 * Returns the identifiers of the attributes used to sort and group the objects
	 * before drawing
	 */
	public Vector getSortAttrIds() {
		return sortAttrIds;
	}

	/**
	 * Sets the identifiers of the attributes used to sort and group the objects
	 * before drawing
	 */
	public void setSortAttrIds(Vector sortAttrIds) {
		this.sortAttrIds = sortAttrIds;
		makeAttributeList();
		QSortAlgorithm.sort(grObjects, this, false);
		plotImageValid = false;
		if (isShowing()) {
			redraw();
		}
	}

	/**
	 * Sets ascending/descending order for the attributes used to sort and group the objects
	 * before drawing
	 */
	public void setSortType(boolean isSortAsc) {
		this.isSortAsc = isSortAsc;
		QSortAlgorithm.sort(grObjects, this, false);
		plotImageValid = false;
		if (isShowing()) {
			redraw();
		}
	}

	/**
	 * Gets the ThematicDataItem associated with the object having the given index
	 * in the container
	 */
	protected ThematicDataItem getThematicDataForObject(int objIdx) {
		DataItem data = oCont.getObjectData(objIdx);
		if (data == null)
			return null;
		if (data instanceof ThematicDataItem)
			return (ThematicDataItem) data;
		if (data instanceof ThematicDataOwner)
			return ((ThematicDataOwner) data).getThematicData();
		return null;
	}

	/**
	 * Compares two objects if they are instances of TimeLineObject.
	 * Returns 0 if the objects are equal, <0 if the first object is less than the
	 * second one, >0 otherwise
	 */
	@Override
	public int compare(Object obj1, Object obj2) {
		if (isSortAsc)
			return compareAscending(obj1, obj2);
		else
			return compareAscending(obj2, obj1);
	}

	public int compareAscending(Object obj1, Object obj2) {
		if (obj1 == null)
			if (obj2 == null)
				return 0;
			else
				return 1;
		if (obj2 == null)
			return -1;
		if (!(obj1 instanceof TimeLineObject) || !(obj2 instanceof TimeLineObject))
			return 0;
		TimeLineObject tlObj1 = (TimeLineObject) obj1, tlObj2 = (TimeLineObject) obj2;
		if (sortAttrIdxs != null && sortAttrIdxs.length > 0) {
			ThematicDataItem d1 = getThematicDataForObject(tlObj1.idx), d2 = getThematicDataForObject(tlObj2.idx);
			if (d1 != null && d2 != null) {
				for (int i = 0; i < sortAttrIdxs.length; i++) {
					if (sortAttrNumeric[i]) {
						double v1 = d1.getNumericAttrValue(sortAttrIdxs[i]), v2 = d2.getNumericAttrValue(sortAttrIdxs[i]);
						if (!Double.isNaN(v1))
							if (!Double.isNaN(v2)) {
								if (v1 < v2)
									return -1;
								if (v1 > v2)
									return 1;
							} else
								return -1;
						else if (!Double.isNaN(v2))
							return 1;
					} else {
						int diff = BubbleSort.compare(d1.getAttrValue(sortAttrIdxs[i]), d2.getAttrValue(sortAttrIdxs[i]));
						if (diff != 0)
							return diff;
					}
				}
			}
		}
		if (tlObj1.t1 != null && tlObj2.t1 != null) {
			int diff = tlObj1.t1.compareTo(tlObj2.t1);
			if (diff != 0)
				return diff;
		}
		if (tlObj1.t2 != null && tlObj2.t2 != null) {
			int diff = tlObj1.t2.compareTo(tlObj2.t2);
			if (diff != 0)
				return diff;
		}
		if (tlObj1.idx < tlObj2.idx)
			return -1;
		return 1;
	}

	/**
	 * Sets the maximum length of time interval of value validity.
	 * If this value is negative, it is ignored.
	 */
	public void setMaxValueValidityTime(long maxValueValidityTime, char prec) {
		if (maxValueValidityTime <= 0 || !(start instanceof Date) || prec == 0 || prec == start.getPrecision()) {
			this.maxValueValidityTime = maxValueValidityTime;
		} else {
			//char p=start.getPrecision();
			//start.setPrecision(prec); end.setPrecision(prec);
			long tl = end.subtract(start, prec);
			//start.setPrecision(p); end.setPrecision(p);
			this.maxValueValidityTime = Math.round(1.0 * maxValueValidityTime * timeLen / tl);
			if (this.maxValueValidityTime < 1) {
				this.maxValueValidityTime = 1;
			}
		}
		plotImageValid = false;
		if (isShowing()) {
			redraw();
		}
	}

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
	 * The scrollpane position for which the image was drawn
	 */
	protected int scrollPosY = -1;

	/**
	 * Finds the size of the display (in case of using a scrollpane, this is
	 * the case of the viewport)
	 */
	public Dimension getVisibleSize() {
		ScrollPane scp = getScrollPane();
		if (scp != null)
			return scp.getViewportSize();
		return getSize();
	}

	public void prepareImage() {
		Dimension size = getVisibleSize();
		if (size == null)
			return;
		int w = size.width, h = size.height;
		if (w < 20 && h < 20)
			return;
		int y0 = 0;
		ScrollPane scp = getScrollPane();
		if (scp != null) {
			y0 = scp.getScrollPosition().y;
		}
		if (plotImage != null)
			if (plotImage.getWidth(null) != w || plotImage.getHeight(null) != h) {
				plotImage = null;
				plotImageValid = false;
			} else if (y0 != scrollPosY) {
				plotImageValid = false;
			}
		if (plotImage == null) {
			plotImage = createImage(w, h);
			plotImageValid = false;
		}
		scrollPosY = y0;
	}

	protected int tfx1 = -1, tfx2 = -1;

	public void paintBackground(Graphics g, int w, int h) {
		g.setColor((colorsForTimes == null) ? bkgColor : Color.white);
		g.fillRect(0, 0, w + 1, h + 1);
		if (colorsForTimes != null) {
			g.setColor(Color.white);
			g.fillRect(0, 0, w + 1, h + 1);
			for (int i = 0; i < colorsForTimes.size(); i++) {
				Object pair[] = colorsForTimes.elementAt(i);
				if (pair[0] instanceof TimeMoment) {
					TimeMoment t0 = (TimeMoment) pair[0];
					int p0 = getTimeAxisPos(t0);
					int p1 = w + 1;
					if (i < colorsForTimes.size() - 1) {
						Object pair1[] = colorsForTimes.elementAt(i + 1);
						if (pair1[0] instanceof TimeMoment) {
							TimeMoment t1 = (TimeMoment) pair1[0];
							p1 = getTimeAxisPos(t1);
						}
					}
					Color cBkg = bkgColor;
					if (pair[1] != null && (pair[1] instanceof Color)) {
						Color c = (Color) pair[1];
						cBkg = new Color(c.getRed(), c.getGreen(), c.getBlue(), 100);
					}
					g.setColor(cBkg);
					g.fillRect(p0, 0, p1 - p0, h + 1);
				}
			}
		}
		tfx1 = -1;
		tfx2 = -1;
		if (timeFilter != null) {
			TimeMoment t1 = timeFilter.getFilterPeriodStart();
			if (t1 != null) {
				t1 = t1.getCopy();
				t1.setPrecision(start.getPrecision());
				tfx1 = getTimeAxisPos(t1);
			}
			TimeMoment t2 = timeFilter.getFilterPeriodEnd();
			if (t2 != null) {
				t2 = t2.getCopy();
				t2.setPrecision(start.getPrecision());
				tfx2 = getTimeAxisPos(t2);
			}
			if (tfx1 >= 0 || tfx2 >= 0) {
				if (tfx1 < 0) {
					tfx1 = 0;
				}
				if (tfx2 < 0) {
					tfx2 = w + 1;
				}
				if (tfx2 == tfx1) {
					++tfx2;
				}
				g.setColor(outOfFilterBkgColor);
				if (tfx1 > 0) {
					g.fillRect(0, 0, tfx1, h + 1);
				}
				if (tfx2 < w) {
					g.fillRect(tfx2, 0, w - tfx2 + 1, h + 1);
				}
			}
		}
	}

	protected TimeGrid tGrid = null;

	public void makeTimeGrid(Graphics g, int w, int h) {
		boolean changeTimeGrid = tGrid == null || !start.equals(tGrid.getStart()) || !end.equals(tGrid.getEnd()) || tGrid.getTimeAxisLength() != drWidth;
		if (changeTimeGrid) {
			if (tGrid == null) {
				tGrid = new TimeGrid();
			}
			tGrid.setTimeInterval(start, end);
			tGrid.setTimeAxisLength(drWidth);
			tGrid.computeGrid();
		}
		IntArray gridPos = tGrid.getGridPositions();
		Vector gridTimes = null;
		if (gridPos != null && gridPos.size() > 0) {
			g.setColor(Color.gray);
			for (int i = 0; i < gridPos.size(); i++) {
				int x = gridPos.elementAt(i);
				if (x >= 0) {
					g.drawLine(x, 0, x, h);
				}
			}
			gridTimes = tGrid.getGridTimeMoments();
		}

		ScrollPane scp = getScrollPane();
		if (tLabCanvas != null) {
			for (TimeLineLabelsCanvas tLabCanva : tLabCanvas) {
				boolean changed = drWidth != tLabCanva.getCurrentWidth() || tLabCanva.getCurrentHeight() != Metrics.fh + 2;
				if (changed) {
					tLabCanva.setRequiredSize(drWidth, Metrics.fh + 2);
				}
				int xScrollPos = 0;
				if (scp != null) {
					xScrollPos = scp.getScrollPosition().x;
				}
				if (tLabCanva.getShift() != xScrollPos) {
					tLabCanva.setShift(xScrollPos);
					changed = true;
				}
				if (changeTimeGrid) {
					if (gridTimes != null) {
						tLabCanva.setGridParameters(gridPos, gridTimes);
					} else {
						tLabCanva.setGridParameters(null, null);
					}
					changed = true;
				}
				if (changed && isShowing() && tLabCanva.isShowing()) {
					Point p = getLocationOnScreen();
					if (p != null) {
						Point p1 = tLabCanva.getLocationOnScreen();
						tLabCanva.setAlignmentDiff(p.x + xScrollPos - p1.x);
					}
					tLabCanva.redraw();
				}
			}
		}

	}

	@Override
	public synchronized void draw(Graphics gr) {
		prepareImage();
		if (plotImageValid) {
			gr.drawImage(plotImage, 0, scrollPosY, null);
			drawHighlightedObjects(gr);
			return;
		}
		Graphics g = null;
		if (plotImage != null) {
			g = plotImage.getGraphics();
		}
		if (g == null) {
			g = gr;
		}
		int drHeight_old = drHeight;
		Dimension size = getVisibleSize();
		int w = size.width, h = size.height;

		if (grObjects == null) {
			setup();
		}
		if (grObjects == null || grObjects.size() < 1)
			return;

		int nObjects = countActiveObjects();
		if (nObjects < 1) {
			paintBackground(g, w, h);
			makeTimeGrid(g, w, h);
			if (plotImage != null && !g.equals(gr)) {
				//everything has been drawn to the image
				plotImageValid = true;
				// copy the image to the screen
				gr.drawImage(plotImage, 0, scrollPosY, null);
			}
			return;
		}
		IntArray sel = null;
		if (listSelector != null && listSelector.isSelectionApplied()) {
			sel = listSelector.getSelectedIndexes();
		}

		float stepX = 1.0f * w / timeLen;
		int stepY = unitSize + 2;
		drWidth = Math.round(stepX * timeLen);
		prefH = getPreferredHeight();
		drHeight = (isTGraphMode) ? prefH : stepY * nObjects;

		if (!isTGraphMode) {
			//compute required height
			int y = 0;
			for (int i = 0; i < grObjects.size(); i++) {
				TrajTimeLineObject obj = grObjects.elementAt(i);
				if (sel != null && sel.indexOf(obj.idx) < 0) {
					continue;
				}
				if (showOnlyActiveEvents && (!obj.active || !((DMovingObject) oCont.getObject(obj.idx)).isAnySegmActive())) {
					continue;
				}
				y += stepY;
			}
			prefH = drHeight = y;
			if (drHeight_old != drHeight) {
				if (drHeight > 0) {
					resize();
					return;
				}
			}
		}

		paintBackground(g, w, h);
		makeTimeGrid(g, w, h);

		if (!isTGraphMode && drHeight <= 0) {
			if (plotImage != null && !g.equals(gr)) {
				//everything has been drawn to the image
				plotImageValid = true;
				// copy the image to the screen
				gr.drawImage(plotImage, 0, scrollPosY, null);
			}
			return;
		}
		//long tstart=System.currentTimeMillis();
		int maxSegmLength = -1;
		if (maxValueValidityTime > 0) {
			maxSegmLength = (int) Math.round(1.0 * maxValueValidityTime * drWidth / timeLen);
			if (maxSegmLength < 1) {
				maxSegmLength = 1;
			}
		}
		int yMax = scrollPosY + h;
		int oIdx = 0;
		int y = 0;
		if (!isTGraphMode) {
			for (; oIdx < grObjects.size() && y + stepY < scrollPosY; oIdx++) {
				TrajTimeLineObject obj = grObjects.elementAt(oIdx);
				obj.tPos1 = obj.tPos2 = obj.oPos1 = obj.oPos2 = -1;
				if (sel != null && sel.indexOf(obj.idx) < 0) {
					continue;
				}
				if (showOnlyActiveEvents && (!obj.active || !((DMovingObject) oCont.getObject(obj.idx)).isAnySegmActive())) {
					continue;
				}
				obj.oPos1 = y - scrollPosY;
				obj.oPos2 = obj.oPos1 + unitSize;
				y += stepY;
			}
		}
		for (; oIdx < grObjects.size() && y < yMax; oIdx++) {
			TrajTimeLineObject obj = grObjects.elementAt(oIdx);
			obj.tPos1 = obj.tPos2 = obj.oPos1 = obj.oPos2 = -1;
			obj.setMaxSegmentLength(maxSegmLength);
			if (sel != null && sel.indexOf(obj.idx) < 0) {
				continue;
			}
			if (showOnlyActiveEvents && (!obj.active || !((DMovingObject) oCont.getObject(obj.idx)).isAnySegmActive())) {
				continue;
			}
			obj.tPos1 = getTimeAxisPos(obj.t1);
			if (obj.instant) {
				obj.tPos2 = obj.tPos1 + 2;
			} else {
				obj.tPos2 = getTimeAxisPos(obj.t2);
			}
			obj.oPos1 = y - scrollPosY;
			obj.oPos2 = obj.oPos1 + unitSize;
			if (obj.tPos != null && (isTGraphMode || obj.colors != null)) {
				Object o = oCont.getObject(obj.idx);
				if (o != null && o instanceof DMovingObject) {
					DMovingObject dmo = (DMovingObject) o;
					Vector v = dmo.getTrack();
					for (int iv = 0; iv < v.size(); iv++) {
						SpatialEntity se = (SpatialEntity) v.elementAt(iv);
						TimeReference tr = se.getTimeReference();
						TimeMoment tm = tr.getValidFrom();
						if (isTGraphMode) {
							obj.vPos[iv] = (Double.isNaN(dAttrVals[obj.idx][iv])) ? -1000 : (int) Math.round(h * (1 - (dAttrVals[obj.idx][iv] - dMin) / (dMax - dMin)));
						}
						obj.tPos[iv] = getTimeAxisPos(tm);
					}
				}
			}
			g.setColor(getColorForObject(obj));
			obj.draw(g, 0);
			if (!isTGraphMode) {
				y += stepY;
			}
		}
		if (tfx1 >= 0 && tfx2 > tfx1) {
			g.setColor(Color.green);
			g.drawLine(tfx1, 0, tfx1, h);
			g.setColor(Color.red);
			g.drawLine(tfx2, 0, tfx2, h);
		}
		if (plotImage != null && !g.equals(gr)) {
			//everything has been drawn to the image
			plotImageValid = true;
			// copy the image to the screen
			gr.drawImage(plotImage, 0, scrollPosY, null);
		}
		if (!isTGraphMode) {
			for (; oIdx < grObjects.size(); oIdx++) {
				TrajTimeLineObject obj = grObjects.elementAt(oIdx);
				obj.tPos1 = obj.tPos2 = obj.oPos1 = obj.oPos2 = -1;
				if (sel != null && sel.indexOf(obj.idx) < 0) {
					continue;
				}
				if (showOnlyActiveEvents && (!obj.active || !((DMovingObject) oCont.getObject(obj.idx)).isAnySegmActive())) {
					continue;
				}
				obj.oPos1 = y - scrollPosY;
				obj.oPos2 = obj.oPos1 + unitSize;
				y += stepY;
			}
			ScrollPane scp = getScrollPane();
			if (scp != null && scp.getVAdjustable().getUnitIncrement() != stepY) {
				scp.getVAdjustable().setUnitIncrement(stepY);
			}
		}
		drawHighlightedObjects(gr);
	}

	@Override
	public void redraw() {
		checkObjectColorPropagation();
		super.redraw();
	}

	/**
	* Checks whether object coloring (classes) is propagated among the system's
	* component.
	*/
	protected void checkObjectColorPropagation() {
		boolean ocOld = objectsAreColored;
		objectsAreColored = oCont != null && supervisor != null && supervisor.getObjectColorer() != null && supervisor.getObjectColorer().getEntitySetIdentifier().equals(oCont.getEntitySetIdentifier());
		if (objectsAreColored || ocOld) {
			plotImageValid = false;
		}
	}

	/**
	* Notification about change of the set of objects to be transiently
	* highlighted. The argument "source" is usually a reference to a
	* Highlighter. The argument setId is the identifier of the set the
	* highlighted objects belong to (e.g. map layer or table).
	* The argument highlighted is a vector of identifiers of
	* currently highlighted objects.
	*/
	@Override
	public void highlightSetChanged(Object source, String setId, Vector highlighted) {
		if (grObjects == null || grObjects.size() < 1)
			return;
		if (!setId.equals(oCont.getEntitySetIdentifier()))
			return;
		Graphics g = getGraphics();
		if (highlighted == null || highlighted.size() < 1) {
			for (int i = 0; i < grObjects.size(); i++) {
				TrajTimeLineObject obj = grObjects.elementAt(i);
				if (obj.isHighlighted) {
					obj.isHighlighted = false;
					if (obj.active && g != null) {
						g.setColor(getColorForObject(obj));
						obj.draw(g, scrollPosY);
					}
				}
			}
		} else {
			for (int i = 0; i < grObjects.size(); i++) {
				TrajTimeLineObject obj = grObjects.elementAt(i);
				if (highlighted.contains(obj.id))
					if (obj.isHighlighted) {
						continue;
					} else {
						obj.isHighlighted = true;
					}
				else if (!obj.isHighlighted) {
					continue;
				} else {
					obj.isHighlighted = false;
				}
				if (obj.active && g != null) {
					g.setColor(getColorForObject(obj));
					obj.draw(g, scrollPosY);
				}
			}
		}
	}

	protected void drawHighlightedObjects(Graphics g) {
		for (int i = 0; i < grObjects.size(); i++) {
			TrajTimeLineObject obj = grObjects.elementAt(i);
			if (obj.oPos1 < 0 && obj.oPos2 < 0) {
				continue;
			}
			if (obj.isHighlighted) {
				g.setColor(activeColor);
				obj.draw(g, scrollPosY);
			} else if (obj.isSelected) {
				g.setColor(selectedColor);
				obj.draw(g, scrollPosY);
			}
		}
	}

	/**
	* Notification about change of the set of objects to be selected (durably
	* highlighted). The argument "source" is usually a reference to a
	* Highlighter. The argument setId is the identifier of the set the
	* highlighted objects belong to (e.g. map layer or table).
	* The argument selected is a vector of identifiers of
	* currently selected objects.
	*/
	@Override
	public void selectSetChanged(Object source, String setId, Vector selected) {
		if (grObjects == null || grObjects.size() < 1)
			return;
		if (!setId.equals(oCont.getEntitySetIdentifier()))
			return;
		Graphics g = getGraphics();
		if (selected == null || selected.size() < 1) {
			for (int i = 0; i < grObjects.size(); i++) {
				TrajTimeLineObject obj = grObjects.elementAt(i);
				if (obj.isSelected) {
					obj.isSelected = false;
					if (obj.active && g != null) {
						g.setColor(getColorForObject(obj));
						obj.draw(g, scrollPosY);
					}
				}
			}
		} else {
			int h = getSize().height;
			int minPos = h + scrollPosY, maxPos = 0;
			for (int i = 0; i < grObjects.size(); i++) {
				TrajTimeLineObject obj = grObjects.elementAt(i);
				if (selected.contains(obj.id)) {
					if (obj.isSelected) {
						continue;
					} else {
						obj.isSelected = true;
					}
					if (obj.oPos1 + scrollPosY < minPos) {
						minPos = obj.oPos1 + scrollPosY;
					}
					if (obj.oPos2 + scrollPosY > maxPos) {
						maxPos = obj.oPos2 + scrollPosY;
					}
				} else if (!obj.isSelected) {
					continue;
				} else {
					obj.isSelected = false;
				}
				if (obj.active && g != null) {
					g.setColor(getColorForObject(obj));
					obj.draw(g, scrollPosY);
				}
			}
			ScrollPane scp = getScrollPane();
			if (scp != null) {
				//scroll to make the selected item(s) visible
				Dimension vpSize = scp.getViewportSize();
				Point scPos = scp.getScrollPosition();
				if (minPos < scPos.y || maxPos > scPos.y + vpSize.height) {
					int y = scPos.y;
					if (maxPos > y + vpSize.height) {
						y = maxPos - vpSize.height + unitSize;
						if (y + vpSize.height > h) {
							y = h - vpSize.height;
						}
					}
					if (minPos < y) {
						y = minPos - unitSize;
						if (y < 0) {
							y = 0;
						}
					}
					if (y != scPos.y) {
						scp.setScrollPosition(scPos.x, y);
					}
				}
			}
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		super.propertyChange(pce);
		//System.out.println(pce+": "+pce.getPropertyName());
		if (pce.getSource().equals(supervisor)) {
			if (pce.getPropertyName().equals(Supervisor.eventObjectColors) && pce.getNewValue().equals(oCont.getEntitySetIdentifier())) {
				plotImageValid = false;
				redraw();
			} else if (pce.getPropertyName().equals(Supervisor.eventTimeColors)) {
				if (pce.getNewValue() == null) {
					colorsForTimes = null;
				} else {
					tryGetColorsForTimes();
				}
				if (tLabCanvas != null) {
					for (TimeLineLabelsCanvas tLabCanva : tLabCanvas) {
						tLabCanva.setColorsForTimes(colorsForTimes);
					}
				}
				plotImageValid = false;
				redraw();
			}
		} else if (pce.getSource().equals(oCont)) {
			if (pce.getPropertyName().equals("destroyed") || pce.getPropertyName().equals("time_references") || pce.getPropertyName().equals("ThematicDataRemoved") || pce.getPropertyName().equals("structure_complete")) {
				destroy();
			} else if (pce.getPropertyName().equals("filter") || pce.getPropertyName().equals("ObjectFilter")) {
				timeFilter = null;
				if (filter != null) {
					filter.removePropertyChangeListener(this);
				}
				filter = oCont.getObjectFilter();
				if (filter != null) {
					findTimeFilter();
				}
				accountForQuery();
				plotImageValid = false;
				redraw();
			} else if (pce.getPropertyName().equals("values") || pce.getPropertyName().equals("ObjectData")) {
				plotImageValid = false;
				redraw();
			} else if (pce.getPropertyName().equals("data_added") || pce.getPropertyName().equals("data_removed") || pce.getPropertyName().equals("data_updated") || pce.getPropertyName().equals("update")
					|| pce.getPropertyName().equals("got_time_references") || pce.getPropertyName().equals("ObjectSet") || pce.getPropertyName().equals("got_data")) {
				setup();
				plotImageValid = false;
				redraw();
			}
		} else if (pce.getSource().equals(listSelector)) {
			if (getScrollPane() != null) {
				resize();
			} else {
				plotImageValid = false;
				redraw();
			}
		}
	}

	protected void resize() {
		if (!isShowing())
			return;
		Dimension size = getSize();
		ScrollPane scp = getScrollPane();
		if (scp == null) {
			setSize(size.width, (prefH > 0) ? prefH : size.height);
			CManager.validateAll(this);
			//redraw();
			return;
		}
		int w = size.width;
		Dimension ss = scp.getViewportSize();
		if (prefH > ss.height) {
			w = ss.width;
			if (ss.height >= size.height) {
				w -= scp.getVScrollbarWidth() - 5;
			}
		}
		if (w == size.width && (prefH == size.height || prefH <= 0))
			return;
		setSize(w, prefH);
		scp.setScrollPosition(0, 0);
		CManager.validateAll(this);
		//redraw();
	}

//---------------- implementation of the EventSource interface ------------
	/**
	* The EventSource answers whether it can produce the specified
	* type of events.
	*/
	@Override
	public boolean doesProduceEvent(String eventId) {
		return eventId != null && (eventId.equals(ObjectEvent.click) || eventId.equals(ObjectEvent.dblClick) || eventId.equals(ObjectEvent.point) || eventId.equals(ObjectEvent.frame));
	}

	/**
	* Returns a unique identifier of the event source (may be produced
	* automatically, used only internally, not shown to the user).
	* The identifier is used for explicit linking of producers and recipients of
	* object events.
	*/
	@Override
	public String getIdentifier() {
		return plotId;
	}

	/**
	* Makes necessary operations for destroying, e.g. unregisters from
	* listening highlighting and other events.
	*/
	@Override
	public void destroy() {
		if (supervisor != null) {
			if (oCont != null) {
				supervisor.removeHighlightListener(this, oCont.getEntitySetIdentifier());
			}
			supervisor.removePropertyChangeListener(this);
		}
		if (oCont != null) {
			oCont.removePropertyChangeListener(this);
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

	protected TimeLineObject getObjectAt(int x, int y) {
		if (grObjects == null || grObjects.size() < 1)
			return null;
		for (int i = 0; i < grObjects.size(); i++) {
			TrajTimeLineObject obj = grObjects.elementAt(i);
			if (obj.active && obj.contains(x, y))
				return obj;
		}
		return null;
	}

	/**
	 * Checks if any of the objects is highlighted
	 */
	protected boolean hasHighlightedObjects() {
		if (grObjects == null || grObjects.size() < 1)
			return false;
		for (int i = 0; i < grObjects.size(); i++) {
			TrajTimeLineObject obj = grObjects.elementAt(i);
			if (obj.active && obj.isHighlighted)
				return true;
		}
		return false;
	}

	/**
	 * Checks if any of the objects is selected
	 */
	protected boolean hasSelectedObjects() {
		if (grObjects == null || grObjects.size() < 1)
			return false;
		for (int i = 0; i < grObjects.size(); i++) {
			TrajTimeLineObject obj = grObjects.elementAt(i);
			if (obj.active && obj.isSelected)
				return true;
		}
		return false;
	}

	@Override
	public void mouseExited(MouseEvent e) { //dehighlight all highlighted objects
		super.mouseExited(e);
		if (supervisor == null)
			return;
		ObjectEvent oevt = new ObjectEvent(this, ObjectEvent.point, e, oCont.getEntitySetIdentifier()); //no objects to highlight
		supervisor.processObjectEvent(oevt);
	}

	protected String lastId = null;
	protected TimeMoment lastDataTime = null;
	protected double lastMapX, lastMapY, lastValue;

	@Override
	public void setPositionInfoInTPN(TimePositionNotifier tpn) {
		tpn.lastId = lastId;
		tpn.lastDataTime = lastDataTime;
		tpn.lastMapX = lastMapX;
		tpn.lastMapY = lastMapY;
		tpn.lastValue = lastValue;
	}

	boolean drag = false;
	int dragX0 = -1, dragY0 = -1, dragX = -1, dragY = -1;

	@Override
	public void mousePressed(MouseEvent e) {
		dragX0 = e.getX();
		dragY0 = e.getY();
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if (!drag) {
			drag = true;
			dragX = e.getX();
			dragY = e.getY();
		} else {
			int x = e.getX(), y = e.getY() - scrollPosY;
			Dimension size = getVisibleSize();
			if (x < 0) {
				x = 0;
			} else if (x > size.width) {
				x = size.width;
			}
			if (y < 0) {
				y = 0;
			} else if (y > size.height) {
				y = size.height;
			}
			y += scrollPosY;
			if (x == dragX && y == dragY)
				return;
			drawRectXOR(dragX0, dragY0, dragX, dragY);
			dragX = x;
			dragY = y;
			drawRectXOR(dragX0, dragY0, dragX, dragY);
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (drag) {
			drawRectXOR(dragX0, dragY0, dragX, dragY);
			drag = false;
			int x1 = dragX0, x2 = dragX;
			if (x1 > x2) {
				x1 = dragX;
				x2 = dragX0;
			}
			int y1 = dragY0, y2 = dragY;
			if (y1 > y2) {
				y1 = dragY;
				y2 = dragY0;
			}
			dragX0 = dragY0 = dragX = dragY = -1;
			if (x1 == x2 && y1 == y2)
				return;
			y1 -= scrollPosY;
			y2 -= scrollPosY;
			Vector selObj = new Vector(50, 50);
			for (int i = 0; i < grObjects.size(); i++) {
				TrajTimeLineObject obj = grObjects.elementAt(i);
				if (obj.active && obj.hasPointInRectangle(x1, y1, x2, y2)) {
					selObj.addElement(obj.id);
				}
			}
			if (selObj.size() < 1)
				return;
			ObjectEvent oevt = new ObjectEvent(this, ObjectEvent.frame, e, oCont.getEntitySetIdentifier(), selObj);
			supervisor.processObjectEvent(oevt);
		} else {
			dragX0 = dragY0 = dragX = dragY = -1;
		}
	}

	protected void drawRectXOR(int x1, int y1, int x2, int y2) {
		if (x1 < 0 || x2 < 0 || y1 < 0 || y2 < 0)
			return;
		if (x1 == x2 && y1 == y2)
			return;
		Graphics g = getGraphics();
		if (g == null)
			return;
		g.setXORMode(Color.gray);
		g.setColor(Color.magenta);
		if (x1 > x2) {
			int x = x1;
			x1 = x2;
			x2 = x;
		}
		if (y1 > y2) {
			int y = y1;
			y1 = y2;
			y2 = y;
		}
		g.drawRect(x1, y1, x2 - x1, y2 - y1);
		g.setPaintMode();
		g.dispose();
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		lastId = null;
		if (supervisor == null || grObjects == null || grObjects.size() < 1)
			return;
		int x = e.getX(), y = e.getY() - scrollPosY;
		//System.out.println("x="+x+"; y="+y+"; scrollPosY="+scrollPosY);
		TrajTimeLineObject obj = (TrajTimeLineObject) getObjectAt(x, y);
		if (obj == null) {
			if (hasHighlightedObjects()) {
				ObjectEvent oevt = new ObjectEvent(this, ObjectEvent.point, e, oCont.getEntitySetIdentifier());
				//to dehighlight all the objects
				supervisor.processObjectEvent(oevt);
			}
			super.mouseMoved(e);
			return;
		}
		//if (obj.isHighlighted) return;
		lastId = obj.id;
		int idx = oCont.getObjectIndex(obj.id);
		if (idx >= 0 && obj.idxPoint >= 0) {
			lastValue = this.dAttrVals[obj.idx][obj.idxPoint];
			DMovingObject dmo = (DMovingObject) oCont.getObject(idx);
			lastDataTime = dmo.getPositionTime(obj.idxPoint).getValidFrom();
			RealPoint pt = dmo.getPositionAsPoint(obj.idxPoint);
			if (pt != null) {
				lastMapX = pt.getX();
				lastMapY = pt.getY();
			} else {
				lastMapX = lastMapY = Double.NaN;
			}
		}
		super.mouseMoved(e);
		ObjectEvent oevt = new ObjectEvent(this, ObjectEvent.point, e, oCont.getEntitySetIdentifier(), obj.id);
		supervisor.processObjectEvent(oevt);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if (checkRightButtonPressed(e)) {
			super.mouseClicked(e);
		} else if (e.getClickCount() == 2) {
			//deselect all selected objects
			lastId = null;
			setPositionInfoInTPN(tpn);
			tpn.notifyPositionChange(this);
			if (!hasSelectedObjects())
				return;
			ObjectEvent oevt = new ObjectEvent(this, ObjectEvent.dblClick, e, oCont.getEntitySetIdentifier());
			supervisor.processObjectEvent(oevt);
		} else if (e.getClickCount() == 1) {
			//select or deselect the object at the cursor
			int x = e.getX(), y = e.getY() - scrollPosY;
			TimeLineObject obj = getObjectAt(x, y);
			if (obj == null)
				return;
			ObjectEvent oevt = new ObjectEvent(this, ObjectEvent.click, e, oCont.getEntitySetIdentifier(), obj.id);
			supervisor.processObjectEvent(oevt);
		}
	}

	//----------------- DataTreater interface ---------------------------
	/**
	* Returns a vector of IDs of the attributes this Data Treater deals with
	*/
	@Override
	public Vector getAttributeList() {
		if (attributes != null && attributes.size() > 0)
			return attributes;
		makeAttributeList();
		return attributes;
	}

	protected void makeAttributeList() {
		sortAttrIdxs = null;
		attributes = null;
		if (oCont == null || oCont.getObjectCount() < 1)
			return;
		ThematicDataItem td = null;
		for (int i = 0; i < oCont.getObjectCount() && td == null; i++) {
			td = getThematicDataForObject(i);
		}
		if (td == null || td.getAttrCount() < 1)
			return;
		if (sortAttrIds != null) {
			sortAttrIdxs = new int[sortAttrIds.size()];
			sortAttrNumeric = new boolean[sortAttrIds.size()];
			for (int i = 0; i < sortAttrIdxs.length; i++) {
				sortAttrIdxs[i] = td.getAttrIndex((String) sortAttrIds.elementAt(i));
				sortAttrNumeric[i] = AttributeTypes.isNumericType(td.getAttrType(sortAttrIdxs[i]));
			}
		}
		attributes = new Vector(td.getAttrCount(), 10);
		if (sortAttrIds != null) {
			for (int i = 0; i < sortAttrIds.size(); i++) {
				attributes.addElement(sortAttrIds.elementAt(i));
			}
		}
		int nAdded = 0;
		for (int i = 0; i < td.getAttrCount() && nAdded < 2; i++)
			if (td.getAttrType(i) == AttributeTypes.time && !StringUtil.isStringInVectorIgnoreCase(td.getAttributeId(i), attributes)) {
				attributes.addElement(td.getAttributeId(i));
				++nAdded;
			}
		if (attributes.size() < 1) {
			attributes = null;
		}
	}

	/**
	* Replies whether it is linked to the data set (table) with the given identifier
	*/
	@Override
	public boolean isLinkedToDataSet(String setId) {
		return oCont != null && oCont.getEntitySetIdentifier().equals(setId);
	}

	/**
	* Returns a vector of colors used for representation of the attributes this
	* Data Treater deals with. May return null if no colors are used.
	*/
	@Override
	public Vector getAttributeColors() {
		return null;
	}

}
