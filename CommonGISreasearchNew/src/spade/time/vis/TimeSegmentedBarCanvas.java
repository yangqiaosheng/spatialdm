package spade.time.vis;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.Metrics;
import spade.lib.basicwin.PopupManager;
import spade.lib.lang.Language;
import spade.lib.ui.RulerController;
import spade.time.TimeIntervalSelector;
import spade.time.TimeMoment;
import spade.vis.action.HighlightListener;
import spade.vis.action.Highlighter;

class SingleSegmentInfo {
	Rectangle r = null; // segment outline
	String timeLabel = null;
	TimeMoment t1 = null, t2 = null;
	Vector IDs = null;
	int total = 0;
	int status = 0;
}

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: 09-Mar-2007
 * Time: 10:23:04
 * To change this template use File | Settings | File Templates.
 */
public class TimeSegmentedBarCanvas extends Canvas implements MouseMotionListener, MouseListener, ComponentListener, PropertyChangeListener, HighlightListener, RulerController, Destroyable {
	static ResourceBundle res = Language.getTextResource("spade.time.vis.SchedulerTexts_time_vis");

	protected static Color
	/*
	normalColor=new Color(96,96,96),
	filteredColor=new Color(168,168,168),
	activeColor=Color.white,
	selectedColor=Color.black,
	*/
	bkgColor = new Color(192, 192, 192);

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
	 * The width and height of the plot area
	 */
	protected int plotW = 0, plotH = 0;

	// Data structures for popups showing mouse-over details
	protected PopupManager popM = null;
	protected Vector hotspots = null;

	protected boolean bDrawUp = false;
	protected Color colors[] = null;
	protected String commonComment = null, comments[] = null;
	/**
	 * Identifiers of the objects (e.g. vehicles, sources, destinations)
	 * to be summarized in the bar segments
	 */
	protected Vector objIDs = null;
	/**
	 * Ordered list of the time moments (instances of TimeMoment),
	 * the beginnings of the time intervals the bars correspond to. The last
	 * element in the list is the end of the last time interval.
	 */
	protected Vector times = null;
	/**
	 * For each object and time interval, specifies the state of this object
	 * at this time interval. 1st index: object; 2nd index: time interval
	 */
	protected int[][] states = null;
	/**
	 * Optional
	 * For each object and time interval, contains some quantitative
	 * characteristic (must be >= 0!), e.g. load or capacity.
	 * 1st index: object; 2nd index: time interval
	 */
	protected int[][] counts = null;
	/**
	 * Optional
	 * For each object, specifies the state before the beginning of the
	 * first time interval
	 */
	protected int[] statesBefore = null;
	/**
	 * Optional
	 * For each object, specifies the state after the end of the
	 * last time interval
	 */
	protected int[] statesAfter = null;
	/**
	 * Optional
	 * For each object, specifies the quantitative characteristic (same as
	 * in "states") before the beginning of the first time interval
	 */
	protected int[] countsBefore = null;
	/**
	 * Optional
	 * For each object, specifies the quantitative characteristic (same as
	 * in "states") after the end of the last time interval
	 */
	protected int[] countsAfter = null;

	protected int nTimes = -1;
	protected long minTime, maxTime;
	protected int maxSum = -1;

	public int getMaxSum() {
		return maxSum;
	}

	// "coordinates" of the selected bar (if any)
	protected TimeMoment tmSel = null; // ssi.t1
	protected int iSel = -1; // ssi.status

	/**
	 * Propagates item selection events when the user clicks on bar segments
	 */
	protected Highlighter highlighter = null;
	/**
	 * Used for the selection of time intervals by clicking on bars of the plot
	 */
	protected TimeIntervalSelector timeIntSel = null;
	/**
	 * A list of instances of TimeAndItemsSelectListener. A TimeAndItemsSelectListener
	 * listens to simultaneous selections of subsets of items (specified by their
	 * identifiers) and time intervals (specified by the start and end time moments)
	 */
	protected Vector tiseList = null;
	/**
	 * Indicates whether to show object identifiers in the popup window appearing
	 * on mouse move
	 */
	protected boolean showObjectIDsInPopup = false;

	/**
	 * @param bDrawUp - the direction of drawing segments in a bar: bottom to top or top to bottom
	 * @param colors - colors corresponding to the object states
	 * @param commonComment - to show in popups
	 * @param comments - names of the object states
	 * @param objIDs - Identifiers of the objects (e.g. vehicles, sources, destinations)
	 * to be summarized in the bar segments
	 * @param times - Ordered list of the time moments (instances of TimeMoment),
	 * the beginnings of the time intervals the bars correspond to. The last
	 * element in the list is the end of the last time interval.
	 * @param states - For each object and time interval, specifies the state of this object
	 * at this time interval. 1st index: object; 2nd index: time interval
	 * @param counts - Optional; For each object and time interval, contains some quantitative
	 * characteristic (must be >= 0!), e.g. load or capacity.
	 * 1st index: object; 2nd index: time interval
	 */
	public TimeSegmentedBarCanvas(boolean bDrawUp, Color colors[], String commonComment, String comments[], Vector objIDs, Vector times, int states[][], int counts[][]) {
		this(bDrawUp, colors, commonComment, comments, objIDs, times, states, counts, null, null, null, null);
	}

	/**
	 * @param bDrawUp - the direction of drawing segments in a bar: bottom to top or top to bottom
	 * @param colors - colors corresponding to the object states
	 * @param commonComment - to show in popups
	 * @param comments - names of the object states
	 * @param objIDs - Identifiers of the objects (e.g. vehicles, sources, destinations)
	 * to be summarized in the bar segments
	 * @param times - Ordered list of the time moments (instances of TimeMoment),
	 * the beginnings of the time intervals the bars correspond to. The last
	 * element in the list is the end of the last time interval.
	 * @param states - For each object and time interval, specifies the state of this object
	 * at this time interval. 1st index: object; 2nd index: time interval
	 * @param counts - Optional; For each object and time interval, contains some quantitative
	 * characteristic (must be >= 0!), e.g. load or capacity.
	 * 1st index: object; 2nd index: time interval
	 * @param statesBefore - Optional; For each object, specifies the state before the beginning of the
	 * first time interval
	 * @param countsBefore - Optional; For each object, specifies the quantitative characteristic (same as
	 * in "states") before the beginning of the first time interval
	 * @param statesAfter - Optional; For each object, specifies the state after the end of the
	 * last time interval
	 * @param countsAfter - Optional; For each object, specifies the quantitative characteristic (same as
	 * in "states") after the end of the last time interval
	 */
	public TimeSegmentedBarCanvas(boolean bDrawUp, Color colors[], String commonComment, String comments[], Vector objIDs, Vector times, int states[][], int counts[][], int statesBefore[], int countsBefore[], int statesAfter[], int countsAfter[]) {
		super();
		this.bDrawUp = bDrawUp;
		this.colors = colors;
		this.commonComment = commonComment;
		this.comments = comments;
		this.objIDs = objIDs;
		this.times = times;
		this.states = states;
		this.counts = counts;
		this.statesBefore = statesBefore;
		this.countsBefore = countsBefore;
		this.statesAfter = statesAfter;
		this.countsAfter = countsAfter;
		++instanceN;
		plotId = "schedule_summary_" + instanceN;
		if (popM == null) {
			popM = new PopupManager(this, "", true);
			popM.setOnlyForActiveWindow(false);
		}
		addComponentListener(this);
		addMouseListener(this);
		addMouseMotionListener(this);
		setup();
	}

	/**
	 * Initial setup; also called when the data change
	 */
	public void setup() {
		nTimes = times.size();
		minTime = ((TimeMoment) times.elementAt(0)).toNumber();
		maxTime = ((TimeMoment) times.elementAt(nTimes - 1)).toNumber();
		maxSum = -1;
		if (counts == null) {
			maxSum = states.length;
		} else {
			for (int ix = 0; ix < counts[0].length; ix++) {
				int sum = 0;
				for (int[] count : counts) {
					sum += count[ix];
				}
				if (sum > maxSum) {
					maxSum = sum;
				}
			}
		}
	}

	/**
	 * Changes the data (in particular, when another item category is selected).
	 * However, the time intervals are assumed to preserve.
	 */
	public void setData(Vector objIDs, int states[][], int counts[][], String commonComment) {
		setData(objIDs, states, counts, null, null, null, null, commonComment);
	}

	/**
	 * Changes the data (in particular, when another item category is selected).
	 * However, the time intervals are assumed to preserve.
	 */
	public void setData(Vector objIDs, int states[][], int counts[][], int statesBefore[], int countsBefore[], int statesAfter[], int countsAfter[], String commonComment) {
		this.objIDs = objIDs;
		this.states = states;
		this.counts = counts;
		this.statesBefore = statesBefore;
		this.countsBefore = countsBefore;
		this.statesAfter = statesAfter;
		this.countsAfter = countsAfter;
		this.commonComment = commonComment;
		iSel = -1;
		tmSel = null;
		maxSum = -1;
		if (objIDs != null && states != null)
			if (counts == null) {
				maxSum = states.length;
			} else {
				for (int ix = 0; ix < counts[0].length; ix++) {
					int sum = 0;
					for (int[] count : counts) {
						sum += count[ix];
					}
					if (sum > maxSum) {
						maxSum = sum;
					}
				}
			}
		redraw();
	}

	/**
	* Returns a unique identifier of the event source (may be produced
	* automatically, used only internally, not shown to the user).
	* The identifier is used for explicit linking of producers and recipients of
	* object events.
	*/
	public String getIdentifier() {
		return plotId;
	}

	@Override
	public Dimension getPreferredSize() {
		if (plotW == 0 || plotH == 0)
			return new Dimension(500, 200);
		return new Dimension(plotW, plotH);
	}

	@Override
	public void paint(Graphics g) {
		draw(g);
	}

	public void redraw() {
		//if (ma==null) return;
		Graphics g = getGraphics();
		if (g != null) {
			draw(g);
			g.dispose();
		}
	}

	private int wPrev = -1, hPrev = -1, x[] = null;
	private int wBeforeAfter = 0, x0 = 0, xEnd = 0;

	private void recountPositions() {
		Dimension size = getSize();
		int w = size.width, h = size.height;
		plotW = w;
		plotH = h;
		w--;
		h--;
		if (wBeforeAfter < 1 && statesBefore != null && statesAfter != null) {
			wBeforeAfter = Metrics.mm() + 1;
			x0 = wBeforeAfter;
		}
		w -= wBeforeAfter * 2;
		if (w != wPrev) {
			x = new int[nTimes];
			for (int ix = 0; ix < x.length; ix++) {
				x[ix] = x0 + (int) (w * (((TimeMoment) times.elementAt(ix)).toNumber() - minTime) / (maxTime - minTime));
				if (ix > 0 && x[ix] == x[ix - 1]) {
					x[ix]++;
				}
			}
			xEnd = x[x.length - 1];
			wPrev = w;
		}
	}

	private int getPosition(TimeMoment t) {
		if (t == null)
			return -1;
		int w = xEnd - x0;
		return x0 + (int) (w * (t.toNumber() - minTime) / (maxTime - minTime));
	}

	public void draw(Graphics g) {
		recountPositions();
		Dimension size = getSize();
		g.setColor(bkgColor);
		g.fillRect(0, 0, size.width + 1, size.height + 1);
		int selX = -1, selY = -1, selW = -1, selH = -1;
		hotspots = new Vector(100, 100);
		g.setColor(Color.gray);
		for (int element : x) {
			g.drawLine(element, 0, element, size.height);
		}
		if (states == null || maxSum <= 0)
			return;
		int first = 0, last = x.length - 1;
		if (statesBefore != null && statesAfter != null) {
			--first;
			++last;
		}
		int y0 = 0, barH = size.height, th = 0;
		if (timeIntSel != null) {
			th = Metrics.mm() / 2;
			if (th < 2) {
				th = 2;
			}
			y0 += th;
			barH -= th * 2;
		}
		for (int ix = first; ix < last; ix++) {
			String timeLabel = null;
			if (ix < 0) {
				timeLabel = res.getString("before") + " " + times.elementAt(0).toString();
			} else if (ix >= x.length - 1) {
				timeLabel = res.getString("after") + " " + times.elementAt(x.length - 1).toString();
			} else {
				timeLabel = times.elementAt(ix).toString() + ".." + times.elementAt(ix + 1).toString();
			}
			timeLabel += "\n";
			int sums[] = new int[colors.length];
			Vector IDs[] = new Vector[colors.length];
			for (int c = 0; c < colors.length; c++) {
				sums[c] = 0;
				IDs[c] = new Vector(10, 10);
			}
			if (ix >= 0 && ix < x.length - 1) {
				for (int iy = 0; iy < states.length; iy++) {
					for (int c = 0; c < colors.length; c++)
						if (states[iy][ix] == c) {
							if (counts == null) {
								sums[c]++;
								IDs[c].addElement(objIDs.elementAt(iy));
							} else if (counts[iy][ix] > 0) {
								sums[c] += counts[iy][ix];
								IDs[c].addElement(objIDs.elementAt(iy));
							}
						}
				}
			} else {
				int st[] = (ix < 0) ? statesBefore : statesAfter;
				int cnt[] = (ix < 0) ? countsBefore : countsAfter;
				for (int iy = 0; iy < st.length; iy++) {
					for (int c = 0; c < colors.length; c++)
						if (st[iy] == c) {
							if (cnt == null) {
								sums[c]++;
								IDs[c].addElement(objIDs.elementAt(iy));
							} else if (cnt[iy] > 0) {
								sums[c] += cnt[iy];
								IDs[c].addElement(objIDs.elementAt(iy));
							}
						}
				}
			}
			int y[] = new int[colors.length + 1];
			y[0] = (bDrawUp) ? y0 + barH : y0;
			int accumulation = 0;
			for (int c = 0; c < colors.length; c++)
				if (sums[c] == 0) {
					y[c + 1] = y[c];
				} else {
					accumulation += sums[c];
					int hh = barH * accumulation / maxSum;
					y[c + 1] = (bDrawUp) ? y0 + barH - hh : y0 + hh;
					if (y[c + 1] == y[c])
						if (c + 1 == colors.length)
							if (bDrawUp) {
								y[c]++;
							} else {
								y[c]--;
							}
						else if (bDrawUp) {
							y[c + 1]--;
						} else {
							y[c + 1]++;
						}
				}
			int xr = (ix < 0) ? 0 : x[ix], ww = (ix >= 0 && ix < x.length - 1) ? x[ix + 1] - x[ix] : wBeforeAfter;
			for (int c = 0; c < colors.length; c++)
				if (sums[c] > 0) {
					int yy = (bDrawUp) ? y[c + 1] : y[c], hh = Math.abs(y[c + 1] - y[c]);
					SingleSegmentInfo ssi = new SingleSegmentInfo();
					ssi.timeLabel = timeLabel;
					ssi.IDs = IDs[c];
					if (ix >= 0 && ix < x.length - 1) {
						ssi.t1 = (TimeMoment) times.elementAt(ix);
						ssi.t2 = (TimeMoment) times.elementAt(ix + 1);
					} else if (ix < 0) {
						ssi.t2 = (TimeMoment) times.elementAt(0);
					} else {
						ssi.t1 = (TimeMoment) times.elementAt(times.size() - 1);
					}
					ssi.total = sums[c];
					ssi.status = c;
					ssi.r = new Rectangle(xr, yy, 1 + ww, 1 + hh);
					hotspots.addElement(ssi);
					g.setColor(colors[c]);
					g.fillRect(xr, yy, ww, hh);
					g.setColor(colors[c].darker());
					g.drawRect(xr, yy, ww, hh);
					if (ssi.status == iSel && ssi.t1 != null && ssi.t1.equals(tmSel)) {
						selX = xr;
						selY = yy;
						selW = ww;
						selH = hh;
					}
				}
		}
		if (wBeforeAfter > 0) {
			g.setColor(Color.gray);
			g.drawLine(x0 - 1, 0, x0 - 1, size.height);
			g.drawLine(xEnd + 1, 0, xEnd + 1, size.height);
		}
		if (selX >= 0) {
			g.setColor(Color.black);
			g.drawRect(selX, selY, selW, selH);
		}
		if (timeIntSel != null) {
			int x1 = getPosition(timeIntSel.getCurrIntervalStart()), x2 = getPosition(timeIntSel.getCurrIntervalEnd());
			if (x1 >= 0 || x2 >= 0) {
				if (x1 < 0) {
					x1 = 0;
				}
				if (x2 < 0) {
					x2 = size.width;
				}
				g.setColor(Color.darkGray);
				g.fillRect(x1, 0, x2 - x1 + 1, th + 1);
				g.fillRect(x1, size.height - th, x2 - x1 + 1, th + 1);
				if (x1 > 0) {
					g.drawLine(x1, 0, x1, size.height);
				}
				if (x2 < size.width) {
					g.drawLine(x2, 0, x2, size.height);
				}
			}
		}
	}

	/**
	 * Returns the position of the starting point
	 */
	@Override
	public int getStartPos() {
		return x0;
	}

	/**
	 * Returns the position of the end point
	 */
	@Override
	public int getEndPos() {
		if (xEnd > 0)
			return xEnd;
		Dimension size = getSize();
		if (size == null) {
			size = getPreferredSize();
		}
		return size.width;
	}

	public void setHighlighter(Highlighter highlighter) {
		this.highlighter = highlighter;
		highlighter.addHighlightListener(this);
	}

	@Override
	public void highlightSetChanged(Object source, String setId, Vector selected) {
	}

	@Override
	public void selectSetChanged(Object source, String setId, Vector selected) {
		if (source == this)
			return;
		tmSel = null;
		iSel = -1;
		redraw();
	}

	/**
	 * Used for the selection of time intervals by clicking on bars of the plot
	 */
	public void setTimeIntervalSelector(TimeIntervalSelector timeIntSel) {
		this.timeIntSel = timeIntSel;
		if (timeIntSel != null) {
			timeIntSel.addTimeSelectionListener(this);
		}
	}

	/**
	 * Reacts to changes of the selection of the current time interval
	 */
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getSource().equals(timeIntSel)) {
			redraw();
		}
	}

	/**
	 * Adds a TimeAndItemsSelectListener, which listens to simultaneous selections
	 * of subsets of items (specified by their identifiers) and time intervals
	 * (specified by the start and end time moments).
	 * Each TimeAndItemsSelectListener receives events when the user clicks on
	 * bar segments
	 */
	public void addTimeAndItemsSelectListener(TimeAndItemsSelectListener listener) {
		if (listener == null)
			return;
		if (tiseList == null) {
			tiseList = new Vector(10, 10);
		}
		if (!tiseList.contains(listener)) {
			tiseList.addElement(listener);
		}
	}

	/**
	 * Removes the listener of the time and item selection events
	 */
	public void removeTimeAndItemsSelectListener(TimeAndItemsSelectListener listener) {
		if (listener == null || tiseList == null || tiseList.size() < 1)
			return;
		int idx = tiseList.indexOf(listener);
		if (idx >= 0) {
			tiseList.removeElementAt(idx);
		}
	}

	//===Mouse===

	public void setShowObjectIDsInPopup(boolean showObjectIDsInPopup) {
		this.showObjectIDsInPopup = showObjectIDsInPopup;
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		if (popM == null || hotspots == null || hotspots.size() == 0)
			return;
		SingleSegmentInfo ssi = null;
		boolean found = false;
		for (int i = 0; i < hotspots.size() && !found; i++) {
			ssi = (SingleSegmentInfo) hotspots.elementAt(i);
			if (ssi.r.contains(e.getX(), e.getY())) {
				found = true;
			}
		}
		if (found) {
			String str = ssi.timeLabel;
			if (commonComment != null) {
				str += "\n" + commonComment;
			}
			str += "\n" + res.getString("Status") + ": " + comments[ssi.status];
			if (showObjectIDsInPopup && ssi.IDs != null) {
				for (int i = 0; i < ssi.IDs.size(); i++) {
					str += ((i == 0) ? "id: " : ", ") + (String) ssi.IDs.elementAt(i);
				}
			}
			str += "\n" + res.getString("Amount") + ": " + ssi.total;
			popM.setText(str);
			popM.setKeepHidden(false);
			popM.startShow(e.getX(), e.getY());
		} else {
			popM.setText("");
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if (e.getClickCount() > 1) {
			if (timeIntSel != null) {
				timeIntSel.selectTimeInterval(null, null);
			}
			return;
		}
		if (timeIntSel != null) {
			if (x == null)
				return;
			int idx = -1;
			for (int i = 0; i < x.length && idx < 0; i++)
				if (e.getX() < x[i]) {
					idx = i;
				}
			TimeMoment t1 = null, t2 = null;
			if (idx < 0) {
				t1 = (TimeMoment) times.elementAt(times.size() - 1);
			} else {
				t1 = (idx < 1) ? null : (TimeMoment) times.elementAt(idx - 1);
				t2 = (TimeMoment) times.elementAt(idx);
			}
			timeIntSel.selectTimeInterval(t1, t2);
			return;
		}
		if (timeIntSel == null && highlighter == null && (tiseList == null || tiseList.size() < 1))
			return;
		if (popM == null || hotspots == null || hotspots.size() == 0)
			return;
		SingleSegmentInfo ssi = null;
		boolean found = false;
		for (int i = 0; i < hotspots.size() && !found; i++) {
			ssi = (SingleSegmentInfo) hotspots.elementAt(i);
			if (ssi.r.contains(e.getX(), e.getY())) {
				found = true;
			}
		}
		if (found && ssi.IDs != null && ssi.IDs.size() > 0) {
			tmSel = ssi.t1;
			iSel = ssi.status;
			if (e.getClickCount() == 1) {
				if (highlighter != null) {
					highlighter.replaceSelectedObjects(this, ssi.IDs);
				}
				if (tiseList != null) {
					for (int i = 0; i < tiseList.size(); i++) {
						((TimeAndItemsSelectListener) tiseList.elementAt(i)).selectionOccurred(ssi.IDs, ssi.t1, ssi.t2);
					}
				}
			}
		} else {
			tmSel = null;
			iSel = -1;
			if (highlighter != null) {
				highlighter.clearSelection(this);
			}
			if (tiseList != null) {
				for (int i = 0; i < tiseList.size(); i++) {
					((TimeAndItemsSelectListener) tiseList.elementAt(i)).cancelSelection();
				}
			}
		}
		redraw();
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void mouseDragged(MouseEvent e) {
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
	}

	//===Mouse===

	/**
	 * Invoked when the size changes.
	 */
	@Override
	public void componentResized(ComponentEvent e) {
		recountPositions();
	}

	/**
	 * Invoked when the position changes.
	 */
	@Override
	public void componentMoved(ComponentEvent e) {
	}

	/**
	 * Invoked when the component has been made visible.
	 */
	@Override
	public void componentShown(ComponentEvent e) {
		recountPositions();
	}

	/**
	 * Invoked when the component has been made invisible.
	 */
	@Override
	public void componentHidden(ComponentEvent e) {
	}

	protected boolean destroyed = false;

	@Override
	public void destroy() {
		if (destroyed)
			return;
		if (highlighter != null) {
			highlighter.removeHighlightListener(this);
		}
		if (timeIntSel != null) {
			timeIntSel.removeTimeSelectionListener(this);
		}
		destroyed = true;
	}

	@Override
	public boolean isDestroyed() {
		return destroyed;
	}
}
