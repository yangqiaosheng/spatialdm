package spade.time.vis;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Label;
import java.awt.Point;
import java.awt.ScrollPane;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;
import java2d.Drawing2D;

import spade.analysis.system.ESDACore;
import spade.analysis.system.Supervisor;
import spade.lib.basicwin.CManager;
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
import spade.vis.database.AttributeDataPortion;
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
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.event.EventSource;
import spade.vis.geometry.RealPoint;
import spade.vis.space.GeoLayer;

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
public class TimeLineCanvas extends CanvasWithTimePosition implements PropertyChangeListener, HighlightListener, MouseListener, MouseMotionListener, EventSource, DataTreater, Comparator, Destroyable {
	protected static Color normalColor = new Color(96, 96, 96), filteredColor = new Color(168, 168, 168), activeColor = Color.white, selectedColor = Color.black;
	protected int unitSize = Math.round(1f * Metrics.mm());
	/**
	 * Colors of the background of the canvas and of the plot area
	 */
	protected Color bkgColor = new Color(192, 192, 192), outOfFilterBkgColor = new Color(150, 150, 150);
	/**
	 * The container with time-referenced items (events), which are visualised
	 */
	protected ObjectContainer oCont = null;
	/**
	 * The geo layer with the events displayed in this canvas
	 */
	protected DGeoLayer geoLayer = null;
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
	protected Vector<TimeLineObject> grObjects = null;
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
	 * The identifiers of the attributes used to sort the objects before drawing
	 */
	protected Vector sortAttrIds = null;
	/**
	 * The identifier of the attribute used to group the objects before drawing
	 */
	protected String groupAttrId = null;
	/**
	 * The identifier of the attribute used to align the objects before drawing
	 */
	protected String alignAttrId = null;
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
	protected TimeLineLabelsCanvas tLabCanvas = null;
	/**
	 * Used for showing information related to the mouse position
	 */
	protected Label infoLabel = null;
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

	public TimeLineCanvas() {
		super();
		++instanceN;
		plotId = "time_line_canvas_" + instanceN;
		setBackground(bkgColor);
	}

	public void setObjectContainer(ObjectContainer oCont) {
		this.oCont = oCont;
		geoLayer = getDGeoLayer();
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
	 * Tries to find the DGeoLayer showing the events on the map
	 */
	protected DGeoLayer getDGeoLayer() {
		if (oCont == null)
			return null;
		if (oCont instanceof DGeoLayer)
			return (DGeoLayer) oCont;
		if (supervisor != null && (oCont instanceof AttributeDataPortion)) {
			ESDACore core = (ESDACore) supervisor.getSystemSettings().getParameter("core");
			if (core != null) {
				GeoLayer gl = core.getDataKeeper().getTableLayer((AttributeDataPortion) oCont);
				if (gl != null && (gl instanceof DGeoLayer))
					return (DGeoLayer) gl;
			}
		}
		return null;
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
			grObjects = new Vector<TimeLineObject>(oCont.getObjectCount(), 100);
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
			TimeLineObject tlObj = new TimeLineObject();
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
		groupAndSort();
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
			tLabCanvas.setTimeInterval(start, end);
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
			geoLayer = getDGeoLayer();
			supervisor.registerHighlightListener(this, oCont.getEntitySetIdentifier());
			supervisor.addPropertyChangeListener(this);
			supervisor.registerObjectEventSource(this);
			tryGetColorsForTimes();
			if (isShowing()) {
				redraw();
			}
		}
	}

	/**
	 * Tries to get from the supervisor the assignment of colors to times
	 */
	protected void tryGetColorsForTimes() {
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
		if (tLabCanvas != null) {
			tLabCanvas.setColorsForTimes(colorsForTimes);
		}
	}

	public void setTimeLineLabelsCanvas(TimeLineLabelsCanvas tLabCanvas) {
		this.tLabCanvas = tLabCanvas;
		if (tLabCanvas != null) {
			tLabCanvas.setBackground(getBackground());
		}
	}

	public void setInfoLabel(Label infoLabel) {
		this.infoLabel = infoLabel;
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
		this.showOnlyActiveEvents = showOnlyActiveEvents;
		if (isShowing()) {
			redraw();
		}
	}

	public int getUnitSize() {
		return unitSize;
	}

	public void setUnitSize(int unitSize) {
		this.unitSize = unitSize;
		if (isShowing()) {
			resize();
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
		int nObjects = countActiveObjects();
		int w = 2 * timeLen, h = (unitSize + 2) * nObjects + 10;
		if (w < 10) {
			w = 100;
		}
		if (h < 10) {
			h = 100;
		}
		Dimension sz = getSize();
		if (sz != null && sz.width > 100)
			return new Dimension(sz.width, h);
		Dimension ss = Toolkit.getDefaultToolkit().getScreenSize();
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
				(grObjects.elementAt(i)).active = true;
			}
		} else {
			for (int i = 0; i < grObjects.size(); i++) {
				TimeLineObject obj = grObjects.elementAt(i);
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
			TimeLineObject obj = grObjects.elementAt((sel == null) ? i : sel.elementAt(i));
			if (obj.active) {
				++nObjects;
			}
		}
		return nObjects;
	}

	@Override
	protected int getTimeAxisPos(TimeMoment t) {
		if (t == null || start == null || timeLen < 1 || drWidth < 10)
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
	}

	/**
	 * Returns the identifier of the attribute used to group the objects before drawing
	 */
	public String getGroupAttrId() {
		return groupAttrId;
	}

	/**
	 * Sets the identifier of the attribute used to group the objects before drawing
	 */
	public void setGroupAttrId(String groupAttrId) {
		this.groupAttrId = groupAttrId;
	}

	/**
	 * Returns the identifier of the attribute used to align the objects before drawing
	 */
	public String getAlignAttrId() {
		return alignAttrId;
	}

	/**
	 * Sets the identifier of the attribute used to align the objects before drawing
	 */
	public void setAlignAttrId(String alignAttrId) {
		this.alignAttrId = alignAttrId;
	}

	/**
	 * Column numbers of the attributes used for grouping and sorting
	 */
	protected IntArray colNs = null;
	/**
	 * The column number of the attribute used for aligning
	 */
	protected int alignColN = -1;
	/**
	 * The information about the attribute used for grouping
	 */
	protected GroupAttrInfo grAInfo = null;

	/**
	 * Sorts and/or groups the objects according to the selected attributes
	 */
	public void groupAndSort() {
		makeAttributeList();
		if (colNs == null) {
			colNs = new IntArray(10, 10);
		} else {
			colNs.removeAllElements();
		}
		ThematicDataItem td = null;
		for (int i = 0; i < oCont.getObjectCount() && td == null; i++) {
			td = getThematicDataForObject(i);
		}
		if (td != null && td.getAttrCount() > 0) {
			if (groupAttrId != null) {
				int idx = td.getAttrIndex(groupAttrId);
				if (idx >= 0) {
					colNs.addElement(idx);
				}
				grAInfo = new GroupAttrInfo();
				grAInfo.id = groupAttrId;
				grAInfo.colN = idx;
				grAInfo.name = td.getAttributeName(idx);
			} else {
				grAInfo = null;
			}
			if (alignAttrId != null) {
				int idx = td.getAttrIndex(alignAttrId);
				if (idx >= 0) {
					colNs.addElement(idx);
				}
				alignColN = idx;
			} else {
				alignColN = -1;
			}
			if (sortAttrIds != null) {
				for (int i = 0; i < sortAttrIds.size(); i++) {
					String attrId = (String) sortAttrIds.elementAt(i);
					int idx = td.getAttrIndex(attrId);
					if (idx >= 0) {
						colNs.addElement(idx);
					}
				}
			}
		}
		BubbleSort.sort(grObjects, this);
		if (grAInfo != null) {
			grAInfo.values = new Vector(grObjects.size() / 2, 50);
			grAInfo.objIdxs = new IntArray(grObjects.size() / 2, 50);
			Object lastValue = null;
			for (int i = 0; i < grObjects.size(); i++) {
				td = getThematicDataForObject(grObjects.elementAt(i).idx);
				Object value = null;
				if (td != null) {
					value = td.getAttrValue(grAInfo.colN);
				}
				if (grAInfo.values.size() < 1 || (lastValue != null && !lastValue.equals(value)) || (lastValue == null && value != null)) {
					grAInfo.values.add(value);
					grAInfo.objIdxs.addElement(i);
					lastValue = value;
				}
			}
		}
/*
    //print the ordered objects for debugging
    if (colNs.size()>0)
      for (int i=0; i<grObjects.size(); i++) {
        String str=i+") "+grObjects.elementAt(i).id+" ("+grObjects.elementAt(i).idx+")";
        td=getThematicDataForObject(grObjects.elementAt(i).idx);
        if (td!=null)
          for (int j=0; j<colNs.size(); j++)
            str+="; "+td.getAttrValue(colNs.elementAt(j));
        System.out.println(str);
      }
    if (grAInfo!=null) {
      System.out.println("Grouping:");
      for (int i=0; i<grAInfo.values.size(); i++)
        System.out.println(i+") "+grAInfo.values.elementAt(i)+": "+
          grAInfo.objIdxs.elementAt(i)+".."+
          ((i<grAInfo.objIdxs.size()-1)?grAInfo.objIdxs.elementAt(i+1)-1:grObjects.size()-1));
    }
*/
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
		if (colNs != null && colNs.size() > 0) {
			ThematicDataItem d1 = getThematicDataForObject(tlObj1.idx), d2 = getThematicDataForObject(tlObj2.idx);
			if (d1 != null && d2 != null) {
				for (int i = 0; i < colNs.size(); i++) {
					int idx = colNs.elementAt(i);
					char type = d1.getAttrType(idx);
					if (AttributeTypes.isNumericType(type)) {
						double v1 = d1.getNumericAttrValue(idx), v2 = d2.getNumericAttrValue(idx);
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
						int diff = BubbleSort.compare(d1.getAttrValue(idx), d2.getAttrValue(idx));
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

	protected TimeGrid tGrid = null;

	@Override
	public void draw(Graphics g) {
		int drHeight_old = drHeight;
		Dimension size = getSize();
		int w = size.width, h = size.height;
		g.setColor((colorsForTimes == null) ? bkgColor : Color.white);
		g.fillRect(0, 0, w + 1, h + 1);
		prefH = h;

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
				g.drawLine(x, 0, x, h);
			}
			gridTimes = tGrid.getGridTimeMoments();
		}

		if (tLabCanvas != null) {
			boolean changed = drWidth != tLabCanvas.getCurrentWidth() || tLabCanvas.getCurrentHeight() != Metrics.fh + 2;
			if (changed) {
				tLabCanvas.setRequiredSize(drWidth, Metrics.fh + 2);
			}
			int xScrollPos = 0;
			ScrollPane scp = getScrollPane();
			if (scp != null) {
				xScrollPos = scp.getScrollPosition().x;
			}
			if (tLabCanvas.getShift() != xScrollPos) {
				tLabCanvas.setShift(xScrollPos);
				changed = true;
			}
			if (changeTimeGrid) {
				if (gridTimes != null) {
					tLabCanvas.setGridParameters(gridPos, gridTimes);
				} else {
					tLabCanvas.setGridParameters(null, null);
				}
				changed = true;
			}
			if (changed && isShowing() && tLabCanvas.isShowing()) {
				Point p = getLocationOnScreen();
				if (p != null) {
					Point p1 = tLabCanvas.getLocationOnScreen();
					tLabCanvas.setAlignmentDiff(p.x + xScrollPos - p1.x);
				}
				tLabCanvas.redraw();
			}
		}
		checkObjectColorPropagation();
		int y = 0;
		Color grLineColor = null;
		if (grAInfo != null && grAInfo.objIdxs != null) {
			grLineColor = Drawing2D.getTransparentColor(Color.gray, 80);
		}
		for (int i = 0; i < grObjects.size(); i++) {
			TimeLineObject obj = grObjects.elementAt(i);
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
				obj.tPos2 = obj.tPos1 + 2;
			} else {
				obj.tPos2 = getTimeAxisPos(obj.t2);
				if (obj.tPos2 <= obj.tPos1) {
					obj.tPos2 = obj.tPos1 + 1;
				}
			}
			obj.oPos1 = y;
			obj.oPos2 = y + unitSize;
			boolean nextInSameGroup = i < grObjects.size() - 1;
			if (grAInfo != null && grAInfo.objIdxs != null) {
				if (i > 0 && grAInfo.objIdxs.indexOf(i) >= 0) {
					g.setColor(grLineColor);
					g.drawLine(0, obj.oPos1, w, obj.oPos1);
				}
				nextInSameGroup = nextInSameGroup && grAInfo.objIdxs.indexOf(i + 1) < 0;
			}
			boolean sameLine = false;
			if (nextInSameGroup && alignColN >= 0) {
				ThematicDataItem td1 = getThematicDataForObject(grObjects.elementAt(i).idx), td2 = getThematicDataForObject(grObjects.elementAt(i + 1).idx);
				Object value1 = (td1 == null) ? null : td1.getAttrValue(alignColN), value2 = (td2 == null) ? null : td2.getAttrValue(alignColN);
				sameLine = (value1 == null) ? value2 == null : value1.equals(value2);
			}
			if (!sameLine) {
				y += stepY;
			}
		}
		if (grAInfo != null && grAInfo.objIdxs != null) {
			g.setColor(grLineColor);
			g.drawLine(0, y, w, y);
		}

		prefH = drHeight = y;
		for (int i = 0; i < grObjects.size(); i++) {
			TimeLineObject obj = grObjects.elementAt(i);
			if (obj.tPos2 < 0 && obj.oPos2 < 0) {
				continue;
			}
			g.setColor(getColorForObject(obj));
			obj.draw(g);
		}
		if (tfx1 >= 0 && tfx2 > tfx1) {
			g.setColor(Color.green);
			g.drawLine(tfx1, 0, tfx1, h);
			g.setColor(Color.red);
			g.drawLine(tfx2, 0, tfx2, h);
		}
		ScrollPane scp = getScrollPane();
		if (scp != null && scp.getVAdjustable().getUnitIncrement() != stepY) {
			scp.getVAdjustable().setUnitIncrement(stepY);
		}
		if (drHeight_old != drHeight) {
			resize();
		}
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
				TimeLineObject obj = grObjects.elementAt(i);
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
				TimeLineObject obj = grObjects.elementAt(i);
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
				TimeLineObject obj = grObjects.elementAt(i);
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
				TimeLineObject obj = grObjects.elementAt(i);
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
					tLabCanvas.setColorsForTimes(colorsForTimes);
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
			ScrollPane scp = getScrollPane();
			if (scp != null) {
				Dimension size = getSize(), prefSize = getPreferredSize();
				if (size.height != prefSize.height) {
					setSize(size.width, prefSize.height);
					invalidate();
					scp.invalidate();
					scp.validate();
				}
				scp.setScrollPosition(0, 0);
			} else {
				redraw();
			}
		}
	}

	protected void resize() {
		if (!isShowing())
			return;
		ScrollPane scp = getScrollPane();
		Dimension size = getSize();
		if (size.height != prefH) {
			int w = size.width;
			if (scp != null) {
				Dimension ss = scp.getViewportSize();
				if (prefH > ss.height) {
					w = ss.width;
					if (ss.height >= size.height) {
						w -= scp.getVScrollbarWidth() - 5;
					}
				}
			}
			setSize(w, prefH);
			CManager.validateAll(this);
		}
		scp.setScrollPosition(0, 0);
		redraw();
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
			TimeLineObject obj = grObjects.elementAt(i);
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
			TimeLineObject obj = grObjects.elementAt(i);
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
			TimeLineObject obj = grObjects.elementAt(i);
			if (obj.active && obj.isSelected)
				return true;
		}
		return false;
	}

	@Override
	public void mouseExited(MouseEvent e) { //dehighlight all highlighted objects
		super.mouseExited(e);
		if (infoLabel != null) {
			infoLabel.setText("");
		}
		if (supervisor == null)
			return;
		ObjectEvent oevt = new ObjectEvent(this, ObjectEvent.point, e, oCont.getEntitySetIdentifier()); //no objects to highlight
		supervisor.processObjectEvent(oevt);
	}

	protected String lastId = null;
	protected TimeMoment lastDataTime = null;
	protected double lastMapX, lastMapY, lastValue;

	protected void clearLastValues() {
		lastId = null;
		lastDataTime = null;
		lastMapX = lastMapY = lastValue = Double.NaN;
	}

	@Override
	public void setPositionInfoInTPN(TimePositionNotifier tpn) {
		tpn.lastId = lastId;
		tpn.lastDataTime = lastDataTime;
		tpn.lastMapX = lastMapX;
		tpn.lastMapY = lastMapY;
		tpn.lastValue = lastValue;
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		clearLastValues();
		if (supervisor == null || grObjects == null || grObjects.size() < 1)
			return;
		int x = e.getX(), y = e.getY();
		TimeLineObject obj = getObjectAt(x, y);
		if (obj == null) {
			if (hasHighlightedObjects()) {
				ObjectEvent oevt = new ObjectEvent(this, ObjectEvent.point, e, oCont.getEntitySetIdentifier());
				//to dehighlight all the objects
				supervisor.processObjectEvent(oevt);
			}
		} else {
			if (!obj.isHighlighted) {
				ObjectEvent oevt = new ObjectEvent(this, ObjectEvent.point, e, oCont.getEntitySetIdentifier(), obj.id);
				supervisor.processObjectEvent(oevt);
			}
			lastId = obj.id;
			lastDataTime = obj.t1;
			if (geoLayer != null) {
				int oIdx = geoLayer.getObjectIndex(obj.id);
				if (oIdx >= 0) {
					DGeoObject gObj = geoLayer.getObject(oIdx);
					TimeMoment t = getTimeForAxisPos(x + shift - alignDiff);
					RealPoint pt = gObj.getGeoPositionAroundTime(t);
					if (pt == null) {
						pt = SpatialEntity.getCentre(gObj.getGeometry());
					}
					if (pt != null) {
						lastMapX = pt.x;
						lastMapY = pt.y;
					}
				}
			}
		}
		super.mouseMoved(e);
		if (infoLabel != null) {
			String txt = "t=" + tpn.getMouseTime();
			if (tpn.lastId != null) {
				txt += "; object id=" + tpn.lastId;
			}
			if (grAInfo != null && grAInfo.objIdxs != null && y <= grObjects.elementAt(grObjects.size() - 1).oPos2) {
				int oIdx = grObjects.size() - 1;
				for (int i = grAInfo.objIdxs.size() - 1; i > 0; i--) {
					int k = grAInfo.objIdxs.elementAt(i);
					if (y < grObjects.elementAt(k).oPos1) {
						oIdx = k - 1;
					} else {
						break;
					}
				}
				ThematicDataItem td = getThematicDataForObject(grObjects.elementAt(oIdx).idx);
				Object value = (td == null) ? null : td.getAttrValue(grAInfo.colN);
				if (value != null) {
					txt += "; " + td.getAttributeName(grAInfo.colN) + " = " + value;
				}
			}
			infoLabel.setText(txt);
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if (checkRightButtonPressed(e)) {
			super.mouseClicked(e);
		} else if (e.getClickCount() == 2) {
			clearLastValues();
			setPositionInfoInTPN(tpn);
			tpn.notifyPositionChange(this);
			//deselect all selected objects
			if (!hasSelectedObjects())
				return;
			ObjectEvent oevt = new ObjectEvent(this, ObjectEvent.dblClick, e, oCont.getEntitySetIdentifier());
			supervisor.processObjectEvent(oevt);
		} else if (e.getClickCount() == 1) {
			//select or deselect the object at the cursor
			int x = e.getX(), y = e.getY();
			TimeLineObject obj = getObjectAt(x, y);
			if (obj == null)
				return;
			ObjectEvent oevt = new ObjectEvent(this, ObjectEvent.click, e, oCont.getEntitySetIdentifier(), obj.id);
			supervisor.processObjectEvent(oevt);
		}
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
		if (groupAttrId != null) {
			attributes.addElement(groupAttrId);
		}
		if (alignAttrId != null) {
			attributes.addElement(alignAttrId);
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
