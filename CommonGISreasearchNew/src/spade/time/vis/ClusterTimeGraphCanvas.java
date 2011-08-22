package spade.time.vis;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
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
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.Metrics;
import spade.lib.util.BubbleSort;
import spade.lib.util.Comparator;
import spade.lib.util.IntArray;
import spade.lib.util.ListSelector;
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

/**
 * Created by IntelliJ IDEA. Creator: N.Andrienko Date: 01-Feb-2007 Time:
 * 16:49:27 Represents a set of events (time-referenced items in a
 * ObjectContainer) in a plane where the horizontal dimension represents time
 * and the vertical dimension is used to position the events. Instant events are
 * represented as small circles and prolonged as horizontal bars.
 */
public class ClusterTimeGraphCanvas extends CanvasWithTimePosition implements PropertyChangeListener, HighlightListener, MouseListener, MouseMotionListener, EventSource, DataTreater, Comparator, Destroyable {
	protected static Color normalColor = new Color(96, 96, 96), filteredColor = new Color(168, 168, 168), activeColor = Color.white, selectedColor = Color.black;
	protected int unitSize = /* Math.round(1f* Metrics.mm()) */5, maxUnitHeight = Math.round(4 * Metrics.mm());

	public void setUnitSize(int unitSize) {
		this.unitSize = unitSize;
		if (isShowing()) {
			resize();
		}
	}

	String tableRef = "";

	public void setTableRef(String ref) {
		tableRef = ref;
	}

	/**
	 * visual appearance: time graph or time line
	 */
	protected boolean isTGraphMode = true;

	public void setIsTGraphMode(boolean TGraphMode) {
		isTGraphMode = TGraphMode;
		if (grObjects != null) {
			for (int i = 0; i < grObjects.size(); i++) {
				ClusterTimeLineObject ttlo = grObjects.get(i);
				ttlo.setIsTGraphMode(TGraphMode);
				if (isTGraphMode && ttlo.vPos == null) {
					ttlo.vPos = (isTGraphMode && dAttrVals != null && dAttrVals[ttlo.idx] != null) ? new int[1 + dAttrVals[ttlo.idx].length] : null;
				}
			}
		}
		resize();
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
	 * ObjectFilter may be associated with the set of objects and contain
	 * results of data querying. Data not satisfying the current query are
	 * displayed in a "neutral" grey and to not react to mouse events.
	 */
	protected ObjectFilter filter = null;
	/**
	 * The TimeFilter associated with the data
	 */
	protected TimeFilter timeFilter = null;
	/**
	 * Supervisor provides access of a plot to the Highlighter (common for all
	 * data displays) and in this way links together all displays
	 */
	protected Supervisor supervisor = null;
	/**
	 * Assignments of colors to time intervals. Contains arrays Object[2] =
	 * [TimeMoment,Color], where the time moments are the starting moments of
	 * the intervals. The intervals are chronologically sorted.
	 */
	protected Vector<Object[]> colorsForTimes = null;
	/**
	 * The graphical objects, instances of TimeLineObject, representing the
	 * members of the container.
	 */
	protected Vector<ClusterTimeLineObject> grObjects = null;
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
	 * sort type: asc or desc
	 */
	protected boolean isSortAsc = true;
	/**
	 * If the objects have attributes, this is the list of attributes to be
	 * shown in popup windows
	 */
	protected Vector attributes = null;
	/**
	 * Indicates that the display must show only active events (i.e. satisfying
	 * all filters)
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
	 * The unique identifier of this plot. The identifier is used 1) for
	 * explicit linking of producers and recipients of object events; 2) for
	 * correct restoring of system states with multiple plots.
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
	/**
	 * class breaks and corresponding colors
	 */
	protected float breaks[] = null;
	protected Color colors[] = null;

	protected boolean highlightingIsActive = true;

	public void setHighlightingIsActive(boolean highlightingIsActive) {
		this.highlightingIsActive = highlightingIsActive;
	}

	public ClusterTimeGraphCanvas() {
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
				filter.addPropertyChangeListener(this);
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
		if (grObjects != null) {
			grObjects.removeAllElements();
		}
		dataStart = null;
		dataEnd = null;
		timeLen = 0;
		if (oCont == null || oCont.getObjectCount() < 1)
			return;
		if (grObjects == null) {
			grObjects = new Vector<ClusterTimeLineObject>(oCont.getObjectCount(), 100);
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
			ClusterTimeLineObject tlObj = new ClusterTimeLineObject();
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
		BubbleSort.sort(grObjects, this);
		visSetup();
	}

	protected void visSetup() {
		timeLen = 0;
		if (dataStart == null || dataEnd == null)
			return;
		if (start == null || start.compareTo(dataStart) < 0) {
			start = dataStart.getCopy();
		}
		if (end == null || end.compareTo(dataEnd) > 0) {
			end = dataEnd.getCopy();
		}
		if (start != null && end != null) {
			if (dataStart instanceof Date) {
				int sw = Toolkit.getDefaultToolkit().getScreenSize().width;
				Date startDate = (Date) start.getCopy(), endDate = (Date) end.getCopy();
				char prec = startDate.getPrecision(), prec1 = prec;
				for (char time_symbol : Date.time_symbols)
					if (startDate.hasElement(time_symbol)) {
						prec1 = time_symbol;
						startDate.setPrecision(prec1);
						endDate.setPrecision(prec1);
						long diff = endDate.subtract(startDate);
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
			ClusterTimeLineObject obj = grObjects.elementAt(i);
			if (obj == null) {
				continue;
			}
			obj.tPos = (dAttrVals[obj.idx] == null) ? null : new int[dAttrVals[obj.idx].length];
			obj.vPos = (dAttrVals[obj.idx] == null) ? null : new int[dAttrVals[obj.idx].length];
		}
	}

	public void setBreaksAndFocuserMinMax(float breaks[], Color colors[], double min, double max) {
		this.breaks = breaks;
		this.colors = colors;
		this.dMin = min;
		this.dMax = max;
		setupBreaks();
	}

	protected void setupBreaks() {
		if (grObjects == null)
			return;
		if (!isTGraphMode) {
			for (int i = 0; i < grObjects.size(); i++) {
				ClusterTimeLineObject obj = grObjects.elementAt(i);
				if (obj == null || dAttrVals == null || obj.idx < 0 || dAttrVals[obj.idx] == null) {
					obj.colors = null;
					continue;
				} else {
					if (obj.colors == null) {
						obj.colors = new Color[dAttrVals[obj.idx].length];
					}
					for (int j = 0; j < dAttrVals[obj.idx].length; j++) {
						if (!Double.isNaN(dMin) && !Double.isNaN(dMax) && (Double.isNaN(dAttrVals[obj.idx][j]) || dAttrVals[obj.idx][j] < dMin || dAttrVals[obj.idx][j] > dMax)) {
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
		}
		redraw();
	}

	public void setFocusInterval(TimeMoment t1, TimeMoment t2) {
		if (t1 == null || t2 == null)
			return;
		if (t2.subtract(t1) < 1)
			return;
		start = t1.getCopy();
		end = t2.getCopy();
		start.setPrecision(dataStart.getPrecision());
		end.setPrecision(dataStart.getPrecision());
		visSetup();
		redraw();
	}

	protected void findTimeFilter() {
		if (timeFilter != null) {
			timeFilter.removeTimeIntervalListener(this);
		}
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
		if (timeFilter != null) {
			timeFilter.addTimeIntervalListener(this);
		}
	}

	public void setSupervisor(Supervisor supervisor) {
		this.supervisor = supervisor;
		if (supervisor != null && oCont != null) {
			supervisor.registerHighlightListener(this, oCont.getEntitySetIdentifier());
			supervisor.addPropertyChangeListener(this);
			supervisor.registerObjectEventSource(this);
			tryGetColorsForTimes();
			if (isShowing()) {
				repaint();
			}
		}
	}

	public void setColorsForTimes(Vector<Object[]> c) {
		colorsForTimes = c;
	}

	/**
	 * Tries to get from the supervisor the assignment of colors to times
	 */
	protected void tryGetColorsForTimes() {
		//
		if (colorsForTimes != null)
			return;

		colorsForTimes = null;
		if (supervisor != null && supervisor.coloredObjectsAreTimes()) {
			Vector assignment = supervisor.getColorsForTimes();
			if (assignment != null) {
				colorsForTimes = TimeUtil.getColorsForTimeIntervals(assignment);
			}
			if (colorsForTimes != null) {
				// check the validity of the time intervals
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
	 * Sets whether the display must show only active events (i.e. satisfying
	 * all filters)
	 */
	public void setShowOnlyActiveEvents(boolean showOnlyActiveEvents) {
		this.showOnlyActiveEvents = showOnlyActiveEvents;
		if (isShowing()) {
			if (getParent() != null && (getParent() instanceof ScrollPane)) {
				resize();
			} else {
				redraw();
			}
		}
	}

	@Override
	public Dimension getPreferredSize() {
		if (grObjects == null) {
			setup();
		}
		if (grObjects == null || grObjects.size() < 1 || timeLen < 1)
			return new Dimension(100, 100);
		Dimension ss = Toolkit.getDefaultToolkit().getScreenSize();
		int nObjects = countActiveObjects();
		int w = unitSize * timeLen, h = (this.isTGraphMode) ? ss.height / 2 : (unitSize + 2) * nObjects + 10;
		if (w < unitSize) {
			w = 100;
		}
		if (h < unitSize) {
			h = 100;
		}
		Dimension sz = getSize();
		if (sz != null && sz.width > 100)
			return new Dimension(sz.width, h);
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
				ClusterTimeLineObject obj = grObjects.elementAt(i);
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
			ClusterTimeLineObject obj = grObjects.elementAt((sel == null) ? i : sel.elementAt(i));
			if (obj.active) {
				++nObjects;
			}
		}
		return nObjects;
	}

	@Override
	protected int getTimeAxisPos(TimeMoment t) {
		if (t == null || start == null || timeLen < 1 || drWidth < 1)
			return -1;
		char c = t.getPrecision();
		t.setPrecision(start.getPrecision());
		int pos = (int) Math.round(1.0 * t.subtract(start) * drWidth / timeLen);
		t.setPrecision(c);
		return pos;
	}

	@Override
	public TimeMoment getTimeForAxisPos(int pos) {
		if (drWidth < 10)
			return null;
		long timePos = Math.round(1.0 * pos * timeLen / drWidth);
		TimeMoment t = start.getCopy();
		t.add(timePos);
		return t;
	}

	/**
	 * Determines the color in which the object with the given index should be
	 * drawn. The color depends on the highlighting/selection state. If
	 * multi-color brushing is used (e.g. broadcasting of classification), the
	 * supervisor knows about the appropriate color.
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
	 * Returns the identifiers of the attributes used to sort and group the
	 * objects before drawing
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
		BubbleSort.sort(grObjects, this);
		if (isShowing()) {
			redraw();
		}
	}

	/**
	 * Sets ascending/descending order for the attributes used to sort and group
	 * the objects before drawing
	 */
	public void setSortType(boolean isSortAsc) {
		this.isSortAsc = isSortAsc;
		BubbleSort.sort(grObjects, this);
		if (isShowing()) {
			redraw();
		}
	}

	/**
	 * Gets the ThematicDataItem associated with the object having the given
	 * index in the container
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
	 * Compares two objects if they are instances of TimeLineObject. Returns 0
	 * if the objects are equal, <0 if the first object is less than the second
	 * one, >0 otherwise
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
		if (sortAttrIds != null && sortAttrIds.size() > 0) {
			ThematicDataItem d1 = getThematicDataForObject(tlObj1.idx), d2 = getThematicDataForObject(tlObj2.idx);
			if (d1 != null && d2 != null) {
				for (int i = 0; i < sortAttrIds.size(); i++) {
					String attrId = (String) sortAttrIds.elementAt(i);
					int idx1 = d1.getAttrIndex(attrId), idx2 = d2.getAttrIndex(attrId);
					char type = d1.getAttrType(idx1);
					if (AttributeTypes.isNumericType(type)) {
						double v1 = d1.getNumericAttrValue(idx1), v2 = d2.getNumericAttrValue(idx2);
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
						int diff = BubbleSort.compare(d1.getAttrValue(idx1), d2.getAttrValue(idx2));
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

	@Override
	public void draw(Graphics g) {
		Dimension size = getSize();
		int w = size.width, h = size.height;
		g.setColor((colorsForTimes == null) ? bkgColor : Color.white);
		g.fillRect(0, 0, w + 1, h + 1);

		if (grObjects == null) {
			setup();
		}
		if (grObjects == null || grObjects.size() < 1)
			return;

		int nObjects = countActiveObjects();
		if (nObjects < 1)
			return;
		IntArray sel = null;
		if (listSelector != null && listSelector.isSelectionApplied()) {
			sel = listSelector.getSelectedIndexes();
		}

		float stepX = 1.0f * w / timeLen;
		int stepY = unitSize + 2;
		drWidth = Math.round(stepX * timeLen);
		drHeight = stepY * nObjects;

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

		int tfx1 = -1, tfx2 = -1;
		if (timeFilter != null) {
			TimeMoment t1 = timeFilter.getFilterPeriodStart();
			if (t1 != null) {
				tfx1 = getTimeAxisPos(t1);
			}
			TimeMoment t2 = timeFilter.getFilterPeriodEnd();
			if (t2 != null) {
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

		TimeGrid tGrid = new TimeGrid();
		tGrid.setTimeInterval(start, end);
		tGrid.setTimeAxisLength(drWidth);
		tGrid.computeGrid();
		IntArray gridPos = tGrid.getGridPositions();
		Vector gridTimes = null;
		if (gridPos != null && gridPos.size() > 0) {
			g.setColor(Color.gray);
			for (int i = 0; i < gridPos.size(); i++) {
				int x = gridPos.elementAt(i);
				g.drawLine(x, 0, x, h);
			}
			gridTimes = tGrid.getGridTimeMoments();
		}

		if (tLabCanvas != null) {
			for (TimeLineLabelsCanvas tLabCanva : tLabCanvas) {
				tLabCanva.setRequiredSize(drWidth, Metrics.fh + 2);
				int xScrollPos = 0;
				if (getParent() != null && (getParent() instanceof ScrollPane)) {
					ScrollPane scp = (ScrollPane) getParent();
					xScrollPos = scp.getScrollPosition().x;
				}
				tLabCanva.setShift(xScrollPos);
				if (gridTimes != null) {
					tLabCanva.setGridParameters(gridPos, gridTimes);
				} else {
					tLabCanva.setGridParameters(null, null);
				}
				if (isShowing() && tLabCanva.isShowing()) {
					Point p = getLocationOnScreen();
					if (p != null) {
						Point p1 = tLabCanva.getLocationOnScreen();
						tLabCanva.setAlignmentDiff(p.x + xScrollPos - p1.x);
					}
				}
				tLabCanva.redraw();
			}
		}

		// long tstart=System.currentTimeMillis();
		int y = 0;
		for (int i = 0; i < grObjects.size(); i++) {
			ClusterTimeLineObject obj = grObjects.elementAt(i);
			if (sel != null && sel.indexOf(obj.idx) < 0) {
				obj.tPos1 = obj.tPos2 = obj.oPos1 = obj.oPos2 = -1;
				continue;
			}
			if (showOnlyActiveEvents && !obj.active) {
				obj.tPos1 = obj.tPos2 = obj.oPos1 = obj.oPos2 = -1;
				continue;
			}
			obj.tPos1 = getTimeAxisPos(obj.t1);
			if (obj.instant) {
				obj.tPos2 = obj.tPos1 + unitSize;
			} else {
				obj.tPos2 = getTimeAxisPos(obj.t2);
			}
			obj.oPos1 = y;
			obj.oPos2 = y + unitSize;
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
			obj.draw(g);
			y += stepY;
		}
		// System.out.println("* "+((isTGraphMode)?"graph":"line")+": elapsed time "+StringUtil.floatToStr((System.currentTimeMillis()-tstart)/1000f,3)+" (s)");

		if (tfx1 >= 0 && tfx2 > tfx1) {
			g.setColor(Color.green);
			g.drawLine(tfx1, 0, tfx1, h);
			g.setColor(Color.red);
			g.drawLine(tfx2, 0, tfx2, h);
		}
		if (getParent() != null && (getParent() instanceof ScrollPane)) {
			ScrollPane scp = (ScrollPane) getParent();
			if (scp.getVAdjustable().getUnitIncrement() != stepY) {
				scp.getVAdjustable().setUnitIncrement(stepY);
			}
		}
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
		objectsAreColored = oCont != null && supervisor != null && supervisor.getObjectColorer() != null && supervisor.getObjectColorer().getEntitySetIdentifier().equals(oCont.getEntitySetIdentifier());
	}

	/**
	 * Notification about change of the set of objects to be transiently
	 * highlighted. The argument "source" is usually a reference to a
	 * Highlighter. The argument setId is the identifier of the set the
	 * highlighted objects belong to (e.g. map layer or table). The argument
	 * highlighted is a vector of identifiers of currently highlighted objects.
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
				ClusterTimeLineObject obj = grObjects.elementAt(i);
				if (obj.isHighlighted) {
					obj.isHighlighted = false;
					if (obj.active && g != null) {
						g.setColor(getColorForObject(obj));
						obj.draw(g);
					}
				}
			}
		} else {
			for (int i = 0; i < grObjects.size(); i++) {
				ClusterTimeLineObject obj = grObjects.elementAt(i);
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
					obj.draw(g);
				}
			}
		}
	}

	/**
	 * Notification about change of the set of objects to be selected (durably
	 * highlighted). The argument "source" is usually a reference to a
	 * Highlighter. The argument setId is the identifier of the set the
	 * highlighted objects belong to (e.g. map layer or table). The argument
	 * selected is a vector of identifiers of currently selected objects.
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
				ClusterTimeLineObject obj = grObjects.elementAt(i);
				if (obj.isSelected) {
					obj.isSelected = false;
					if (obj.active && g != null) {
						g.setColor(getColorForObject(obj));
						obj.draw(g);
					}
				}
			}
		} else {
			int h = getSize().height;
			int minPos = h, maxPos = 0;
			for (int i = 0; i < grObjects.size(); i++) {
				ClusterTimeLineObject obj = grObjects.elementAt(i);
				if (selected.contains(obj.id)) {
					if (obj.isSelected) {
						continue;
					} else {
						obj.isSelected = true;
					}
					if (obj.oPos1 < minPos) {
						minPos = obj.oPos1;
					}
					if (obj.oPos2 > maxPos) {
						maxPos = obj.oPos2;
					}
				} else if (!obj.isSelected) {
					continue;
				} else {
					obj.isSelected = false;
				}
				if (obj.active && g != null) {
					g.setColor(getColorForObject(obj));
					obj.draw(g);
				}
			}
			if (getParent() != null && (getParent() instanceof ScrollPane)) {
				// scroll to make the selected item(s) visible
				ScrollPane scp = (ScrollPane) getParent();
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
		if (pce.getSource().equals(supervisor)) {
			if (pce.getPropertyName().equals(Supervisor.eventObjectColors) && pce.getNewValue().equals(oCont.getEntitySetIdentifier())) {
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
				redraw();
			}
		} else if (pce.getSource().equals(filter)) {
			if (pce.getPropertyName().equals("destroyed")) {
				filter.removePropertyChangeListener(this);
				filter = null;
				if (timeFilter != null) {
					timeFilter.removeTimeIntervalListener(this);
				}
				timeFilter = null;
			}
			if (!destroyed) {
				accountForQuery();
				redraw();
			}
		} else if (pce.getSource().equals(timeFilter)) {
			if (pce.getPropertyName().equals("current_interval")) {
				redraw();
			}
		} else if (pce.getSource().equals(oCont)) {
			if (pce.getPropertyName().equals("destroyed") || pce.getPropertyName().equals("time_references") || pce.getPropertyName().equals("ThematicDataRemoved") || pce.getPropertyName().equals("structure_complete")) {
				destroy();
			} else if (pce.getPropertyName().equals("filter") || pce.getPropertyName().equals("ObjectFilter")) {
				if (timeFilter != null) {
					timeFilter.removeTimeIntervalListener(this);
				}
				timeFilter = null;
				if (filter != null) {
					filter.removePropertyChangeListener(this);
				}
				filter = oCont.getObjectFilter();
				if (filter != null) {
					filter.addPropertyChangeListener(this);
					findTimeFilter();
				}
				accountForQuery();
				redraw();
			} else if (pce.getPropertyName().equals("values") || pce.getPropertyName().equals("ObjectData")) {
				redraw();
			} else if (pce.getPropertyName().equals("data_added") || pce.getPropertyName().equals("data_removed") || pce.getPropertyName().equals("data_updated") || pce.getPropertyName().equals("update")
					|| pce.getPropertyName().equals("got_time_references") || pce.getPropertyName().equals("ObjectSet") || pce.getPropertyName().equals("got_data")) {
				setup();
				redraw();
			}
		} else if (pce.getSource().equals(listSelector)) {
			if (getParent() != null && (getParent() instanceof ScrollPane)) {
				resize();
			} else {
				redraw();
			}
		}
	}

	protected void resize() {
		ScrollPane scp = (ScrollPane) getParent();
		Dimension size = getSize(), prefSize = getPreferredSize();
		if (size.height != prefSize.height) {
			setSize(size.width, prefSize.height);
			scp.invalidate();
			scp.validate();
			redraw();
		}
		scp.setScrollPosition(0, 0);
	}

	// ---------------- implementation of the EventSource interface ------------
	/**
	 * The EventSource answers whether it can produce the specified type of
	 * events.
	 */
	@Override
	public boolean doesProduceEvent(String eventId) {
		return eventId != null && (eventId.equals(ObjectEvent.click) || eventId.equals(ObjectEvent.dblClick) || eventId.equals(ObjectEvent.point) || eventId.equals(ObjectEvent.frame));
	}

	/**
	 * Returns a unique identifier of the event source (may be produced
	 * automatically, used only internally, not shown to the user). The
	 * identifier is used for explicit linking of producers and recipients of
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
		if (filter != null) {
			filter.removePropertyChangeListener(this);
		}
		if (timeFilter != null) {
			timeFilter.removeTimeIntervalListener(this);
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
			ClusterTimeLineObject obj = grObjects.elementAt(i);
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
			ClusterTimeLineObject obj = grObjects.elementAt(i);
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
			ClusterTimeLineObject obj = grObjects.elementAt(i);
			if (obj.active && obj.isSelected)
				return true;
		}
		return false;
	}

	@Override
	public void mouseExited(MouseEvent e) { // dehighlight all highlighted
											// objects
		super.mouseExited(e);
		if (supervisor == null)
			return;
		ObjectEvent oevt = new ObjectEvent(this, ObjectEvent.point, e, oCont.getEntitySetIdentifier()); // no objects to highlight
		supervisor.processObjectEvent(oevt);
	}

	// attribute values of the cluster corresponding to current line segment
	protected String lastId = null;
	protected TimeMoment lastDataTime = null;
	protected double lastMapX, lastMapY, lastValue;
	protected String lastC_id;

	@Override
	public void setPositionInfoInTPN(TimePositionNotifier tpn) {
		tpn.lastId = lastId;
		tpn.lastDataTime = lastDataTime;
		tpn.lastMapX = lastMapX;
		tpn.lastMapY = lastMapY;
		tpn.lastValue = lastValue;
		tpn.lastC_id = lastC_id;
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		lastId = null;
		if (!highlightingIsActive) {
			super.mouseMoved(e); // drawing vertical line (time reference)
			return;
		}
		if (supervisor == null || grObjects == null || grObjects.size() < 1)
			return;
		int x = e.getX(), y = e.getY();
		ClusterTimeLineObject obj = (ClusterTimeLineObject) getObjectAt(x, y);
		if (obj == null) {
			if (hasHighlightedObjects()) {
				ObjectEvent oevt = new ObjectEvent(this, ObjectEvent.point, e, oCont.getEntitySetIdentifier());
				// to dehighlight all the objects
				supervisor.processObjectEvent(oevt);
			}
			setPositionInfoInTPN(tpn);
			tpn.notifyPositionChange(this);
			return;
		}
		// if (obj.isHighlighted) return;
		lastId = obj.id;

		int idx = oCont.getObjectIndex(obj.id);
		if (idx >= 0 && obj.idxPoint >= 0) {
			lastValue = this.dAttrVals[obj.idx][obj.idxPoint];
			DMovingObject dmo = (DMovingObject) oCont.getObject(idx);
			lastDataTime = dmo.getPositionTime(obj.idxPoint).getValidFrom();
			lastMapX = dmo.getPositionAsPoint(obj.idxPoint).getX();
			lastMapY = dmo.getPositionAsPoint(obj.idxPoint).getY();
			lastC_id = dmo.getPosition(obj.idxPoint).getThematicData().getAttrValueAsString(0);
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
			// deselect all selected objects
			lastId = null;
			setPositionInfoInTPN(tpn);
			tpn.notifyPositionChange(this);
			if (!hasSelectedObjects())
				return;
			ObjectEvent oevt = new ObjectEvent(this, ObjectEvent.dblClick, e, oCont.getEntitySetIdentifier());
			supervisor.processObjectEvent(oevt);
		} else if (e.getClickCount() == 1) {
			// select or deselect the object at the cursor
			int x = e.getX(), y = e.getY();
			ClusterTimeLineObject obj = (ClusterTimeLineObject) getObjectAt(x, y);
			if (obj == null)
				return;
			((HighlightListener) supervisor.getHighlighter(tableRef).getListeners().get(1)).highlightSetChanged(this, String.valueOf(tpn.lastC_id), null);
			ObjectEvent oevt = new ObjectEvent(this, ObjectEvent.click, e, oCont.getEntitySetIdentifier(), obj.id);
			supervisor.processObjectEvent(oevt);
		}
	}

	// ----------------- DataTreater interface ---------------------------
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
		attributes = null;
		if (oCont == null || oCont.getObjectCount() < 1)
			return;
		ThematicDataItem td = null;
		for (int i = 0; i < oCont.getObjectCount() && td == null; i++) {
			td = getThematicDataForObject(i);
		}
		if (td == null || td.getAttrCount() < 1)
			return;
		attributes = new Vector(td.getAttrCount(), 10);
		if (sortAttrIds != null) {
			for (int i = 0; i < sortAttrIds.size(); i++) {
				attributes.addElement(sortAttrIds.elementAt(i));
			}
		}
		for (int i = 0; i < td.getAttrCount(); i++)
			if (td.getAttrType(i) == AttributeTypes.time && !StringUtil.isStringInVectorIgnoreCase(td.getAttributeId(i), attributes)) {
				attributes.addElement(td.getAttributeId(i));
			}
		if (attributes.size() < 1) {
			attributes = null;
		}
	}

	/**
	 * Replies whether it is linked to the data set (table) with the given
	 * identifier
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
