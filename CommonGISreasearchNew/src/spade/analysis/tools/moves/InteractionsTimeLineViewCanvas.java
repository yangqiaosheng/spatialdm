package spade.analysis.tools.moves;

import java.awt.Canvas;
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

import spade.analysis.aggregates.Aggregate;
import spade.analysis.aggregates.AggregateContainer;
import spade.analysis.aggregates.AggregateMember;
import spade.analysis.system.Supervisor;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.Metrics;
import spade.lib.util.BubbleSort;
import spade.lib.util.Comparator;
import spade.lib.util.IntArray;
import spade.lib.util.StringUtil;
import spade.time.Date;
import spade.time.TimeMoment;
import spade.time.TimeReference;
import spade.time.TimeUtil;
import spade.time.vis.TimeGrid;
import spade.time.vis.TimeLineInteractionObject;
import spade.time.vis.TimeLineLabelsCanvas;
import spade.time.vis.TimeLineObject;
import spade.vis.action.HighlightListener;
import spade.vis.action.Highlighter;
import spade.vis.action.ObjectEvent;
import spade.vis.database.AttributeTypes;
import spade.vis.database.CombinedFilter;
import spade.vis.database.DataItem;
import spade.vis.database.DataTreater;
import spade.vis.database.ObjectContainer;
import spade.vis.database.ObjectFilter;
import spade.vis.database.ThematicDataItem;
import spade.vis.database.ThematicDataOwner;
import spade.vis.database.TimeFilter;
import spade.vis.event.EventSource;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Apr 21, 2008
 * Time: 11:21:13 AM
 * A time line (Gantt chart) display of trajectories and interactions between them
 */
public class InteractionsTimeLineViewCanvas extends Canvas implements PropertyChangeListener, HighlightListener, MouseListener, MouseMotionListener, EventSource, DataTreater, Comparator, Destroyable {
	protected static Color normalColor = new Color(96, 96, 96), activeColor = Color.white, selectedColor = Color.black;
	protected static final int unitSize = /*Math.round(1f* Metrics.mm())*/3, maxUnitHeight = Math.round(4 * Metrics.mm());
	/**
	 * Colors of the background of the canvas and of the plot area
	 */
	protected Color bkgColor = new Color(192, 192, 192), outOfFilterBkgColor = new Color(150, 150, 150);
	/**
	 * The container (layer) with the members of the interactions
	 */
	protected ObjectContainer memberContainer = null;
	/**
	 * The layer with the interactions
	 */
	protected AggregateContainer interContainer = null;
	/**
	 * The filter of the interactions.
	 */
	protected ObjectFilter interFilter = null;
	/**
	 * The filter of the members of the interactions.
	 */
	protected ObjectFilter memberFilter = null;
	/**
	 * The TimeFilter associated with the data
	 */
	protected TimeFilter timeFilter = null;
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
	 * members of the interactions.
	 */
	protected Vector memberObjects = null;
	/**
	 * The graphical objects, instances of TimeLineObject, representing the
	 * interactions.
	 */
	protected Vector interObjects = null;
	/**
	* Indicates whether the interaction members on the plot are currently colored according
	* to some propagated classification.
	*/
	protected boolean membersAreColored = false;
	/**
	* Indicates whether the interactions on the plot are currently colored according
	* to some propagated classification.
	*/
	protected boolean interactionsAreColored = false;
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

	public InteractionsTimeLineViewCanvas() {
		super();
		++instanceN;
		plotId = "interactions_time_line_canvas_" + instanceN;
		setBackground(bkgColor);
	}

	public void setData(AggregateContainer interContainer, ObjectContainer memberContainer) {
		this.interContainer = interContainer;
		this.memberContainer = memberContainer;
		if (memberContainer == null || interContainer == null)
			return;
		if (interContainer != null) {
			interContainer.addPropertyChangeListener(this);
			interFilter = interContainer.getObjectFilter();
			if (interFilter != null) {
				interFilter.addPropertyChangeListener(this);
				findTimeFilter();
			}
		}
		if (memberContainer != null) {
			memberContainer.addPropertyChangeListener(this);
			memberFilter = memberContainer.getObjectFilter();
			if (memberFilter != null) {
				memberFilter.addPropertyChangeListener(this);
			}
		}
		if (supervisor != null) {
			supervisor.registerHighlightListener(this, interContainer.getEntitySetIdentifier());
			supervisor.registerHighlightListener(this, memberContainer.getEntitySetIdentifier());
			supervisor.addPropertyChangeListener(this);
			supervisor.registerObjectEventSource(this);
			addMouseListener(this);
			addMouseMotionListener(this);
		}
	}

	/**
	 * Initial setup; also called when the data change
	 */
	public void setup() {
		if (memberObjects != null) {
			memberObjects.removeAllElements();
		}
		if (interObjects != null) {
			interObjects.removeAllElements();
		}
		dataStart = null;
		dataEnd = null;
		timeLen = 0;
		if (memberContainer == null || memberContainer.getObjectCount() < 1 || interContainer == null || interContainer.getObjectCount() < 1)
			return;
		if (memberObjects == null) {
			memberObjects = new Vector(memberContainer.getObjectCount(), 100);
		}
		for (int i = 0; i < memberContainer.getObjectCount(); i++) {
			DataItem dit = memberContainer.getObjectData(i);
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
			memberObjects.addElement(tlObj);
		}
		Vector inter = interContainer.getAggregates();
		if (interObjects == null) {
			interObjects = new Vector(inter.size() * 2, 100);
		}
		for (int i = 0; i < inter.size(); i++) {
			Aggregate aggr = (Aggregate) inter.elementAt(i);
			if (aggr == null) {
				continue;
			}
			Vector members = aggr.getAggregateMembers();
			if (members == null || members.size() < 1) {
				continue;
			}
			for (int j = 0; j < members.size(); j++) {
				AggregateMember mb = (AggregateMember) members.elementAt(j);
				TimeLineInteractionObject tliObj = new TimeLineInteractionObject();
				tliObj.id = aggr.getIdentifier();
				tliObj.idx = i;
				tliObj.tloPID = mb.id;
				//tliObj.tloPIdx=-1;
				tliObj.t1 = mb.enterTime;
				tliObj.t2 = mb.exitTime;
				if (tliObj.t1 != null) {
					if (dataStart == null || tliObj.t1.compareTo(dataStart) < 0) {
						dataStart = tliObj.t1;
					}
					if (dataEnd == null || tliObj.t1.compareTo(dataEnd) > 0) {
						dataEnd = tliObj.t1;
					}
				}
				if (tliObj.t2 != null) {
					if (dataEnd == null || tliObj.t2.compareTo(dataEnd) > 0) {
						dataEnd = tliObj.t2;
					}
					tliObj.instant = false;
				} else {
					tliObj.instant = true;
				}
				if (members.size() > 1) {
					tliObj.tloIDs = new String[members.size() - 1];
					//tliObj.tloIdxs=new int[members.size()-1];
					int k = -1;
					for (int n = 0; n < members.size(); n++)
						if (n != j) {
							AggregateMember mb_n = (AggregateMember) members.elementAt(n);
							k++;
							tliObj.tloIDs[k] = mb_n.id;
							//tliObj.tloIdxs[k]=-1; // will be found later, perhaps
						}
				}
				interObjects.addElement(tliObj);
			}
		}
		if (dataStart != null) {
			dataStart = dataStart.getCopy();
		}
		if (dataEnd != null) {
			dataEnd = dataEnd.getCopy();
		}
		BubbleSort.sort(memberObjects, this);
		accountForQuery();
		checkObjectColorPropagation();
		visSetup();
	}

	public void visSetup() {
		timeLen = 0;
		if (dataStart == null || dataEnd == null)
			return;
		if (start == null || start.compareTo(dataStart) < 0) {
			start = dataStart.getCopy();
		}
		if (end == null || end.compareTo(dataEnd) > 0) {
			end = dataEnd.getCopy();
		}
		if (start instanceof Date) {
			int sw = Toolkit.getDefaultToolkit().getScreenSize().width;
			Date startDate = (Date) start.getCopy(), endDate = (Date) end.getCopy();
			char prec = startDate.getPrecision(), prec1 = prec;
			for (char time_symbol : Date.time_symbols) {
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
		if (interFilter != null)
			if (interFilter instanceof TimeFilter) {
				timeFilter = (TimeFilter) interFilter;
			} else if (interFilter instanceof CombinedFilter) {
				CombinedFilter cf = (CombinedFilter) interFilter;
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
		if (supervisor != null && memberContainer != null && interContainer != null) {
			supervisor.registerHighlightListener(this, interContainer.getEntitySetIdentifier());
			supervisor.registerHighlightListener(this, memberContainer.getEntitySetIdentifier());
			supervisor.addPropertyChangeListener(this);
			supervisor.registerObjectEventSource(this);
			addMouseListener(this);
			addMouseMotionListener(this);
			tryGetColorsForTimes();
			if (isShowing()) {
				repaint();
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

	@Override
	public Dimension getPreferredSize() {
		if (memberObjects == null || interObjects == null) {
			setup();
		}
		if (memberObjects == null || memberObjects.size() < 1 || timeLen < 1)
			return new Dimension(100, 100);
		int nObjects = countActiveMembers();
		int w = unitSize * timeLen, h = (unitSize + 2) * nObjects + 10;
		if (w < unitSize) {
			w = 100;
		}
		if (h < unitSize) {
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
		if (memberObjects == null || memberObjects.size() < 1)
			return;
		if (memberFilter == null) {
			for (int i = 0; i < memberObjects.size(); i++) {
				((TimeLineObject) memberObjects.elementAt(i)).active = true;
			}
		} else {
			for (int i = 0; i < memberObjects.size(); i++) {
				TimeLineObject obj = (TimeLineObject) memberObjects.elementAt(i);
				obj.active = memberFilter.isActive(obj.idx);
			}
		}
		if (interObjects == null || interObjects.size() < 1)
			return;
		if (interFilter == null) {
			for (int i = 0; i < interObjects.size(); i++) {
				((TimeLineObject) interObjects.elementAt(i)).active = true;
			}
		} else {
			for (int i = 0; i < interObjects.size(); i++) {
				TimeLineObject obj = (TimeLineObject) interObjects.elementAt(i);
				obj.active = interFilter.isActive(obj.idx);
			}
		}
	}

	protected int countActiveMembers() {
		int nObjects = 0;
		for (int i = 0; i < memberObjects.size(); i++) {
			TimeLineObject obj = (TimeLineObject) memberObjects.elementAt(i);
			if (obj.active) {
				++nObjects;
			}
		}
		return nObjects;
	}

	protected int getTimeAxisPos(TimeMoment t) {
		if (t == null || start == null || timeLen < 1 || drWidth < 1)
			return -1;
		char c = t.getPrecision();
		t.setPrecision(start.getPrecision());
		int pos = (int) Math.round(1.0 * t.subtract(start) * drWidth / timeLen);
		t.setPrecision(c);
		return pos;
	}

	/**
	* Determines the color in which the object (interaction member) with the given index should be
	* drawn. The color depends on the highlighting/selection state.
	* If multi-color brushing is used (e.g. broadcasting of classification),
	* the supervisor knows about the appropriate color.
	*/
	protected Color getColorForMember(TimeLineObject obj) {
		if (!obj.active)
			return null;
		if (obj.isHighlighted)
			return activeColor;
		if (obj.isSelected)
			return selectedColor;
		if (!membersAreColored)
			return normalColor;
		return supervisor.getColorForDataItem(memberContainer.getObjectData(obj.idx), memberContainer.getEntitySetIdentifier(), memberContainer.getContainerIdentifier(), normalColor);
	}

	/**
	* Determines the color in which the interaction with the given index should be
	* drawn. The color depends on the highlighting/selection state.
	* If multi-color brushing is used (e.g. broadcasting of classification),
	* the supervisor knows about the appropriate color.
	*/
	protected Color getColorForInteraction(TimeLineObject obj) {
		if (!obj.active)
			return null;
		if (obj.isHighlighted)
			return Color.WHITE; // activeColor.darker();
		if (obj.isSelected)
			return Color.RED; // selectedColor.brighter();
		if (!interactionsAreColored)
			return Color.ORANGE; // normalColor.brighter();
		return supervisor.getColorForDataItem(interContainer.getObjectData(obj.idx), interContainer.getEntitySetIdentifier(), interContainer.getContainerIdentifier(), normalColor);
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
		BubbleSort.sort(memberObjects, this);
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
		BubbleSort.sort(memberObjects, this);
		if (isShowing()) {
			redraw();
		}
	}

	/**
	 * Gets the ThematicDataItem associated with the member having the given index
	 * in the container
	 */
	protected ThematicDataItem getThematicDataForMember(int objIdx) {
		DataItem data = memberContainer.getObjectData(objIdx);
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
		if (sortAttrIds != null && sortAttrIds.size() > 0) {
			ThematicDataItem d1 = getThematicDataForMember(tlObj1.idx), d2 = getThematicDataForMember(tlObj2.idx);
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

	public void draw(Graphics g) {
		Dimension size = getSize();
		int w = size.width, h = size.height;
		g.setColor((colorsForTimes == null) ? bkgColor : Color.white);
		g.fillRect(0, 0, w + 1, h + 1);

		if (memberObjects == null) {
			setup();
		}
		if (memberObjects == null || memberObjects.size() < 1)
			return;

		int nObjects = countActiveMembers();
		if (nObjects < 1)
			return;
		float stepX = 1.0f * w / timeLen;
		int stepY = unitSize + 2; /*h/nObjects;*/
		//if (stepX<unitSize) stepX=unitSize;
/*
    if (stepY<unitSize) stepY=unitSize;
    if (stepY>maxUnitHeight) stepY=maxUnitHeight;
*/
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
			tLabCanvas.setRequiredSize(drWidth, Metrics.fh + 2);
			int xScrollPos = 0;
			if (getParent() != null && (getParent() instanceof ScrollPane)) {
				ScrollPane scp = (ScrollPane) getParent();
				xScrollPos = scp.getScrollPosition().x;
			}
			tLabCanvas.setShift(xScrollPos);
			if (gridTimes != null) {
				tLabCanvas.setGridParameters(gridPos, gridTimes);
			} else {
				tLabCanvas.setGridParameters(null, null);
			}
			if (isShowing() && tLabCanvas.isShowing()) {
				Point p = getLocationOnScreen();
				if (p != null) {
					Point p1 = tLabCanvas.getLocationOnScreen();
					tLabCanvas.setAlignmentDiff(p.x + xScrollPos - p1.x);
				}
			}
			tLabCanvas.redraw();
		}
		int y = 0;
		for (int i = 0; i < memberObjects.size(); i++) {
			TimeLineObject tlo = (TimeLineObject) memberObjects.elementAt(i);
			if (!tlo.active) {
				tlo.tPos1 = tlo.tPos2 = tlo.oPos1 = tlo.oPos2 = -1;
				continue;
			}
			tlo.tPos1 = getTimeAxisPos(tlo.t1);
			if (tlo.instant) {
				tlo.tPos2 = tlo.tPos1 + unitSize;
			} else {
				tlo.tPos2 = getTimeAxisPos(tlo.t2);
			}
			tlo.oPos1 = y;
			tlo.oPos2 = y + unitSize;
			g.setColor(getColorForMember(tlo));
			tlo.draw(g);
			y += stepY;
		}
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
		if (interObjects == null || interObjects.size() < 1)
			return;
		for (int i = 0; i < interObjects.size(); i++) {
			TimeLineInteractionObject tlio = (TimeLineInteractionObject) interObjects.elementAt(i);
			int idxTr = getMemberIdxById(tlio.tloPID);
			if (idxTr < 0) {
				continue;
			}
			if (!tlio.active) {
				tlio.tPos1 = tlio.tPos2 = tlio.oPos1 = tlio.oPos2 = -1;
				continue;
			}
			tlio.tPos1 = getTimeAxisPos(tlio.t1);
			if (tlio.instant) {
				tlio.tPos2 = tlio.tPos1 + unitSize;
			} else {
				tlio.tPos2 = getTimeAxisPos(tlio.t2);
			}
			TimeLineObject tlo = (TimeLineObject) memberObjects.elementAt(idxTr);
			tlio.oPos1 = tlo.oPos1 + 1;
			tlio.oPos2 = tlo.oPos2 - 1;
			g.setColor(getColorForInteraction(tlio));
			tlio.draw(g);
		}
	}

	@Override
	public void paint(Graphics g) {
		draw(g);
	}

	public void redraw() {
		if (memberContainer == null || interContainer == null)
			return;
		Graphics g = getGraphics();
		if (g != null) {
			checkObjectColorPropagation();
			draw(g);
			g.dispose();
		}
	}

	/**
	* Checks whether object coloring (classes) is propagated among the system's
	* component.
	*/
	protected void checkObjectColorPropagation() {
		membersAreColored = memberContainer != null && supervisor != null && supervisor.getObjectColorer() != null && supervisor.getObjectColorer().getEntitySetIdentifier().equals(memberContainer.getEntitySetIdentifier());
		interactionsAreColored = !membersAreColored && interContainer != null && supervisor != null && supervisor.getObjectColorer() != null && supervisor.getObjectColorer().getEntitySetIdentifier().equals(interContainer.getEntitySetIdentifier());
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
		if (memberObjects == null || memberObjects.size() < 1)
			return;
		Vector grObjects = null;
		boolean inter = false;
		if (setId.equals(memberContainer.getEntitySetIdentifier())) {
			grObjects = memberObjects;
		} else if (setId.equals(interContainer.getEntitySetIdentifier())) {
			grObjects = interObjects;
			inter = true;
		}
		if (grObjects == null)
			return;
		Graphics g = getGraphics();
		if (highlighted == null || highlighted.size() < 1) {
			for (int i = 0; i < grObjects.size(); i++) {
				TimeLineObject obj = (TimeLineObject) grObjects.elementAt(i);
				if (obj.isHighlighted) {
					obj.isHighlighted = false;
					if (obj.active && g != null) {
						g.setColor((inter) ? getColorForInteraction(obj) : getColorForMember(obj));
						obj.draw(g);
						if (!inter && interObjects != null) {
							for (int io = 0; io < interObjects.size(); io++) {
								TimeLineInteractionObject iobj = (TimeLineInteractionObject) interObjects.elementAt(io);
								if (iobj.tloPID.equals(obj.id)) {
									g.setColor(getColorForInteraction(iobj));
									iobj.draw(g);
								}
							}
						}
					}
				}
			}
		} else {
			for (int i = 0; i < grObjects.size(); i++) {
				TimeLineObject obj = (TimeLineObject) grObjects.elementAt(i);
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
					g.setColor((inter) ? getColorForInteraction(obj) : getColorForMember(obj));
					obj.draw(g);
					if (!inter && interObjects != null) {
						for (int io = 0; io < interObjects.size(); io++) {
							TimeLineInteractionObject iobj = (TimeLineInteractionObject) interObjects.elementAt(io);
							if (iobj.tloPID.equals(obj.id)) {
								g.setColor(getColorForInteraction(iobj));
								iobj.draw(g);
							}
						}
					}
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
		if (memberObjects == null || memberObjects.size() < 1)
			return;
		Vector grObjects = null;
		boolean inter = false;
		if (setId.equals(memberContainer.getEntitySetIdentifier())) {
			grObjects = memberObjects;
		} else if (setId.equals(interContainer.getEntitySetIdentifier())) {
			grObjects = interObjects;
			inter = true;
		}
		if (grObjects == null)
			return;
		Graphics g = getGraphics();
		if (selected == null || selected.size() < 1) {
			for (int i = 0; i < grObjects.size(); i++) {
				TimeLineObject obj = (TimeLineObject) grObjects.elementAt(i);
				if (obj.isSelected) {
					obj.isSelected = false;
					if (obj.active && g != null) {
						g.setColor((inter) ? getColorForInteraction(obj) : getColorForMember(obj));
						obj.draw(g);
						if (!inter && interObjects != null) {
							for (int io = 0; io < interObjects.size(); io++) {
								TimeLineInteractionObject iobj = (TimeLineInteractionObject) interObjects.elementAt(io);
								if (iobj.tloPID.equals(obj.id)) {
									g.setColor(getColorForInteraction(iobj));
									iobj.draw(g);
								}
							}
						}
					}
				}
			}
		} else {
			int h = getSize().height;
			int minPos = h, maxPos = 0;
			for (int i = 0; i < grObjects.size(); i++) {
				TimeLineObject obj = (TimeLineObject) grObjects.elementAt(i);
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
					g.setColor((inter) ? getColorForInteraction(obj) : getColorForMember(obj));
					obj.draw(g);
					if (!inter && interObjects != null) {
						for (int io = 0; io < interObjects.size(); io++) {
							TimeLineInteractionObject iobj = (TimeLineInteractionObject) interObjects.elementAt(io);
							if (iobj.tloPID.equals(obj.id)) {
								g.setColor(getColorForInteraction(iobj));
								iobj.draw(g);
							}
						}
					}
				}
			}
			if (getParent() != null && (getParent() instanceof ScrollPane)) {
				//scroll to make the selected item(s) visible
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
		if (pce.getSource().equals(supervisor)) {
			if (pce.getPropertyName().equals(Supervisor.eventObjectColors) && (pce.getNewValue().equals(memberContainer.getEntitySetIdentifier()) || pce.getNewValue().equals(interContainer.getEntitySetIdentifier()))) {
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
		} else if (pce.getSource().equals(interFilter)) {
			if (pce.getPropertyName().equals("destroyed")) {
				interFilter.removePropertyChangeListener(this);
				interFilter = null;
				if (timeFilter != null) {
					timeFilter.removeTimeIntervalListener(this);
				}
				timeFilter = null;
			}
			if (!destroyed) {
				accountForQuery();
				redraw();
			}
		} else if (pce.getSource().equals(memberFilter)) {
			if (pce.getPropertyName().equals("destroyed")) {
				memberFilter.removePropertyChangeListener(this);
				memberFilter = null;
			}
			if (!destroyed) {
				accountForQuery();
				redraw();
			}
		} else if (pce.getSource().equals(timeFilter)) {
			if (pce.getPropertyName().equals("current_interval")) {
				redraw();
			}
		} else if (pce.getSource().equals(memberContainer) || pce.getSource().equals(interContainer)) {
			if (pce.getPropertyName().equals("destroyed") || pce.getPropertyName().equals("ThematicDataRemoved") || pce.getPropertyName().equals("structure_complete")) {
				destroy();
			} else if (pce.getPropertyName().equals("filter") || pce.getPropertyName().equals("ObjectFilter")) {
				if (pce.getSource().equals(memberContainer)) {
					if (memberFilter != null) {
						memberFilter.removePropertyChangeListener(this);
					}
					memberFilter = memberContainer.getObjectFilter();
				} else {
					if (timeFilter != null) {
						timeFilter.removeTimeIntervalListener(this);
					}
					timeFilter = null;
					if (interFilter != null) {
						interFilter.removePropertyChangeListener(this);
					}
					interFilter = interContainer.getObjectFilter();
					if (interFilter != null) {
						interFilter.addPropertyChangeListener(this);
						findTimeFilter();
					}
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
		}
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
			if (memberContainer != null) {
				supervisor.removeHighlightListener(this, memberContainer.getEntitySetIdentifier());
			}
			supervisor.removePropertyChangeListener(this);
		}
		if (interFilter != null) {
			interFilter.removePropertyChangeListener(this);
		}
		if (memberFilter != null) {
			memberFilter.removePropertyChangeListener(this);
		}
		if (timeFilter != null) {
			timeFilter.removeTimeIntervalListener(this);
		}
		if (interContainer != null) {
			interContainer.removePropertyChangeListener(this);
		}
		if (memberContainer != null) {
			memberContainer.removePropertyChangeListener(this);
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

	protected TimeLineObject getMemberById(String id) {
		if (id == null || memberObjects == null || memberObjects.size() < 1)
			return null;
		for (int i = 0; i < memberObjects.size(); i++) {
			TimeLineObject obj = (TimeLineObject) memberObjects.elementAt(i);
			if (id.equals(obj.id))
				return obj;
		}
		return null;
	}

	protected int getMemberIdxById(String id) {
		if (id == null || memberObjects == null || memberObjects.size() < 1)
			return -1;
		for (int i = 0; i < memberObjects.size(); i++) {
			TimeLineObject obj = (TimeLineObject) memberObjects.elementAt(i);
			if (id.equals(obj.id))
				return i;
		}
		return -1;
	}

	protected TimeLineObject getMemberAt(int x, int y) {
		if (memberObjects == null || memberObjects.size() < 1)
			return null;
		for (int i = 0; i < memberObjects.size(); i++) {
			TimeLineObject obj = (TimeLineObject) memberObjects.elementAt(i);
			if (obj.active && obj.contains(x, y))
				return obj;
		}
		return null;
	}

	protected TimeLineObject getInteractionAt(int x, int y) {
		if (interObjects == null || interObjects.size() < 1)
			return null;
		for (int i = 0; i < interObjects.size(); i++) {
			TimeLineObject obj = (TimeLineObject) interObjects.elementAt(i);
			if (obj.active && obj.contains(x, y))
				return obj;
		}
		return null;
	}

	/**
	 * Checks if any of the members is highlighted
	 */
	protected boolean hasHighlightedMembers() {
		if (memberObjects == null || memberObjects.size() < 1)
			return false;
		for (int i = 0; i < memberObjects.size(); i++) {
			TimeLineObject obj = (TimeLineObject) memberObjects.elementAt(i);
			if (obj.active && obj.isHighlighted)
				return true;
		}
		return false;
	}

	/**
	 * Checks if any of the members is selected
	 */
	protected boolean hasSelectedMembers() {
		if (memberObjects == null || memberObjects.size() < 1)
			return false;
		for (int i = 0; i < memberObjects.size(); i++) {
			TimeLineObject obj = (TimeLineObject) memberObjects.elementAt(i);
			if (obj.active && obj.isSelected)
				return true;
		}
		return false;
	}

	/**
	 * Checks if any of the interactions is highlighted
	 */
	protected boolean hasHighlightedInteractions() {
		if (interObjects == null || interObjects.size() < 1)
			return false;
		for (int i = 0; i < interObjects.size(); i++) {
			TimeLineObject obj = (TimeLineObject) interObjects.elementAt(i);
			if (obj.active && obj.isHighlighted)
				return true;
		}
		return false;
	}

	/**
	 * Checks if any of the interactions is selected
	 */
	protected boolean hasSelectedInteractions() {
		if (interObjects == null || interObjects.size() < 1)
			return false;
		for (int i = 0; i < interObjects.size(); i++) {
			TimeLineObject obj = (TimeLineObject) interObjects.elementAt(i);
			if (obj.active && obj.isSelected)
				return true;
		}
		return false;
	}

	@Override
	public void mouseExited(MouseEvent e) { //dehighlight all highlighted objects
		if (supervisor == null)
			return;
		ObjectEvent oevt = new ObjectEvent(this, ObjectEvent.point, e, memberContainer.getEntitySetIdentifier()); //no objects to highlight
		supervisor.processObjectEvent(oevt);
		oevt = new ObjectEvent(this, ObjectEvent.point, e, interContainer.getEntitySetIdentifier()); //no objects to highlight
		supervisor.processObjectEvent(oevt);
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		if (supervisor == null || memberObjects == null || memberObjects.size() < 1)
			return;
		int x = e.getX(), y = e.getY();
		TimeLineObject obj = getInteractionAt(x, y);
		if (obj != null) {
			if (hasHighlightedInteractions()) { // dehighlight all the interactions
				ObjectEvent oevt = new ObjectEvent(this, ObjectEvent.point, e, interContainer.getEntitySetIdentifier());
				supervisor.processObjectEvent(oevt);
			}
			if (hasHighlightedMembers()) { // dehighlight all the trajectories
				ObjectEvent oevt = new ObjectEvent(this, ObjectEvent.point, e, memberContainer.getEntitySetIdentifier());
				supervisor.processObjectEvent(oevt);
			}
			//if (obj.isHighlighted) return;
			// to ensure popup with information
			ObjectEvent oevt = new ObjectEvent(this, ObjectEvent.point, e, interContainer.getEntitySetIdentifier(), obj.id);
			supervisor.processObjectEvent(oevt);
		} else {
			obj = getMemberAt(x, y);
			if (obj == null) {
				if (hasHighlightedMembers()) { // dehighlight all the trajectories
					ObjectEvent oevt = new ObjectEvent(this, ObjectEvent.point, e, memberContainer.getEntitySetIdentifier());
					supervisor.processObjectEvent(oevt);
				}
				return;
			}
			if (hasHighlightedInteractions()) { // dehighlight all the interactions
				ObjectEvent oevt = new ObjectEvent(this, ObjectEvent.point, e, interContainer.getEntitySetIdentifier());
				supervisor.processObjectEvent(oevt);
			}
			///if (obj.isHighlighted) return;
			// to ensure popup with information
			ObjectEvent oevt = new ObjectEvent(this, ObjectEvent.point, e, memberContainer.getEntitySetIdentifier(), obj.id);
			supervisor.processObjectEvent(oevt);
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if (e.getClickCount() == 2) {
			//deselect all selected objects
			if (hasSelectedMembers()) {
				ObjectEvent oevt = new ObjectEvent(this, ObjectEvent.dblClick, e, memberContainer.getEntitySetIdentifier());
				supervisor.processObjectEvent(oevt);
			}
			if (hasSelectedInteractions()) {
				ObjectEvent oevt = new ObjectEvent(this, ObjectEvent.dblClick, e, interContainer.getEntitySetIdentifier());
				supervisor.processObjectEvent(oevt);
			}
		} else if (e.getClickCount() == 1) {
			//select or deselect the object at the cursor
			int x = e.getX(), y = e.getY();
			TimeLineObject obj = getInteractionAt(x, y);
			if (obj != null) { // click to interaction
				ObjectEvent oevt = new ObjectEvent(this, ObjectEvent.click, e, interContainer.getEntitySetIdentifier(), obj.id);
				supervisor.processObjectEvent(oevt);
				// selecting all trajectories involved in the selected interaction
				Highlighter highlighter = supervisor.getHighlighter(memberContainer.getEntitySetIdentifier());
				if (highlighter != null) {
					TimeLineInteractionObject tlio = (TimeLineInteractionObject) obj;
					Vector v = new Vector(1 + tlio.tloIDs.length, 10);
					v.addElement(tlio.tloPID);
					for (String tloID : tlio.tloIDs) {
						v.addElement(tloID);
					}
					if (tlio.isSelected) {
						highlighter.clearSelection(this);
					} else {
						highlighter.replaceSelectedObjects(this, v);
					}
				}
			} else { // click to trajectory
				obj = getMemberAt(x, y);
				if (obj == null)
					return;
				ObjectEvent oevt = new ObjectEvent(this, ObjectEvent.click, e, memberContainer.getEntitySetIdentifier(), obj.id);
				supervisor.processObjectEvent(oevt);
			}
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
		return null;
		/* // this code confuses popup manager which should handle both trajectories and interactions
		if (attributes!=null && attributes.size()>0) return attributes;
		makeAttributeList();
		return attributes;
		*/
	}

	protected void makeAttributeList() {
		attributes = null;
		if (memberContainer == null || memberContainer.getObjectCount() < 1)
			return;
		ThematicDataItem td = null;
		for (int i = 0; i < memberContainer.getObjectCount() && td == null; i++) {
			td = getThematicDataForMember(i);
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
	* Replies whether it is linked to the data set (table) with the given identifier
	*/
	@Override
	public boolean isLinkedToDataSet(String setId) {
		return memberContainer != null && memberContainer.getEntitySetIdentifier().equals(setId);
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
