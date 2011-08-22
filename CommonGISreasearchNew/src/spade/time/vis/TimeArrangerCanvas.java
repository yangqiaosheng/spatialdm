package spade.time.vis;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import spade.lib.basicwin.PopupManager;
import spade.time.Date;
import spade.time.TimeLimits;
import spade.time.TimeMoment;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Oct 27, 2009
 * Time: 2:12:44 PM
 * Draws a display representing colored time moments
 */
public class TimeArrangerCanvas extends Canvas implements MouseMotionListener {

	/**
	 * The ordered sequence of time moments. These may be starting moments of
	 * time intervals; pay attention to the distance (difference) between the consecutive
	 * moments!
	 * If these are instances of spade.time.Date, pay also attention to the precision!
	 * E.g. if the precision is 'm', these are months.
	 */
	protected TimeMoment times[] = null;
	/**
	 * The list of classes of time moments
	 */
	protected String allClassLabels[] = null;
	/**
	 * The colors assigned to the classes time moments
	 */
	protected Color allClassColors[] = null;
	/**
	 * The indexes of the classes of the time moments in the list of the classes
	 */
	protected int timeClassIdxs[] = null;
	/**
	 * Time limits may be propagated to interested listeners, e.g. for event selection
	 */
	protected TimeLimits timeLimits[] = null;
	/**
	 * Listeners of the selection of time intervals. The time limits are
	 * passed to them.
	 */
	protected Vector<PropertyChangeListener> timeLimitsListeners = null;

	protected int nColumns = 10, offset = 0, nRows = 1;
	protected char timeBreak = ' '; // if not blank, set's time precision for a new row, e.g. 'y' means new year

	public int getNRows() {
		return nRows;
	}

	public int getCellSizeX() {
		return cellSizeX;
	}

	public int getCellSizeY() {
		return cellSizeY;
	}

	protected int cellSizeX = 20, cellSizeY = 20;

	public int getNColumns() {
		return nColumns;
	}

	public int getOffset() {
		return offset;
	}

	/**
	 * x,y coordinates in the matrix
	 */
	protected int x[] = null, y[] = null;

	public int getX(int idx) {
		return (x != null && idx < x.length) ? x[idx] : 0;
	}

	public int getY(int idx) {
		return (y != null && idx < y.length) ? y[idx] : 0;
	}

	public TimeMoment getTime(int idx) {
		if (times == null)
			return null;
		if (idx < 0 || idx >= times.length)
			return null;
		return times[idx];
	}

	// Data structures for popups showing mouse-over details
	protected PopupManager popM = null;
	protected Rectangle hotspots[] = null;

	public TimeArrangerCanvas(int nColumns, TimeMoment times[], String allClassLabels[], Color allClassColors[], int timeClassIdxs[]) {
		this.times = times;
		this.timeClassIdxs = timeClassIdxs;
		this.allClassLabels = allClassLabels;
		this.allClassColors = allClassColors;
		this.nColumns = nColumns;
		computeDefaultNColumns();
		if (nColumns > 0) {
			setupXY();
		} else {
			setupXY('y');
		}
		addMouseMotionListener(this);
	}

	public void setClasses(String allClassLabels[], Color allClassColors[], int timeClassIdxs[]) {
		this.allClassLabels = allClassLabels;
		this.allClassColors = allClassColors;
		this.timeClassIdxs = timeClassIdxs;
		if (isShowing()) {
			repaint();
		}
	}

	protected void setupXY() {
		if (x == null) {
			x = new int[times.length];
			y = new int[times.length];
		}
		int xPos = offset - 1, yPos = 0;
		for (int i = 0; i < times.length; i++) {
			xPos++;
			if (xPos >= nColumns) {
				xPos = 0;
				yPos++;
			}
			x[i] = xPos;
			y[i] = yPos;
			if (yPos + 1 > nRows) {
				nRows = yPos + 1;
			}
		}
	}

	protected void setupXY(char precision) {
		if (!(times[0] instanceof Date) || ((Date) times[0]).getPrecision() == 'y') {
			setupXY();
		}
		if (x == null) {
			x = new int[times.length];
			y = new int[times.length];
		}
		int xPos = -1, yPos = 0, prev = -1;
		nColumns = 0;
		for (int i = 0; i < times.length; i++) {
			xPos++;
			int val = times[i].getElementValue(precision);
			if (i > 0 && val != prev) {
				xPos = 0;
				yPos++;
			}
			x[i] = xPos;
			y[i] = yPos;
			prev = val;
			if (xPos + 1 > nColumns) {
				nColumns = xPos + 1;
			}
			nRows = yPos + 1;
		}
		if (nRows > 1) { // check if we need to adjust right the 1st row
			boolean adjustRight = false;
			int offset = 0;
			if (precision == 'y') {
				switch ((times[0]).getMinPrecision()) {
				case 'd':
					long dt = (times[1]).subtract(times[0], 'd');
					if (dt == 7) {
						offset = ((Date) times[0]).getWeekOfYear() - 1;
					} else if (dt == 1) {
						offset = ((Date) times[0]).getDayOfYear() - 1;
					}
					break;
				case 'm': // these codes are not tested - no data examples
					offset = ((Date) times[0]).month - 1;
				default:
					adjustRight = true;
				}
			}
			if (precision == 'm') { // these codes are not tested - no data examples
				switch ((times[0]).getMinPrecision()) {
				case 'd':
					offset = ((Date) times[0]).day - 1;
					break;
				default:
					adjustRight = true;
				}
			}
			if (precision == 'd') { // these codes are not tested - no data examples
				switch ((times[0]).getMinPrecision()) {
				case 'h':
					offset = ((Date) times[0]).hour;
					break;
				default:
					adjustRight = true;
				}
			}
			if (precision == 'h') { // these codes are not tested - no data examples
				switch ((times[0]).getMinPrecision()) {
				case 't':
					offset = ((Date) times[0]).min;
					break;
				default:
					adjustRight = true;
				}
			}
			if (adjustRight) {
				int nColumnsIn1stRow = 0;
				for (int i = 0; i < times.length && y[i] == 0; i++) {
					nColumnsIn1stRow = x[i] + 1;
				}
				if (nColumnsIn1stRow < nColumns) {
					for (int i = 0; i < times.length && y[i] == 0; i++) {
						x[i] += nColumns - nColumnsIn1stRow;
					}
				}
			} else if (offset > 0) {
				for (int i = 0; i < times.length && y[i] == 0; i++) {
					x[i] += offset;
				}
			}
		}
	}

	public void setNColumns(int nColumns, int offset) {
		if (this.nColumns != nColumns || this.offset != offset) {
			this.nColumns = nColumns;
			this.offset = offset;
			if (nColumns > 0) {
				setupXY();
				timeBreak = ' ';
			} else {
				setupXY('y');
			}
			redraw();
		}
	}

	public void setNColumns(char precision) {
		timeBreak = precision;
		setupXY(timeBreak);
		redraw();
	}

	public void setCellSizeX(int cellSizeX) {
		this.cellSizeX = cellSizeX;
		redraw();
	}

	public void setCellSizeY(int cellSizeY) {
		this.cellSizeY = cellSizeY;
		redraw();
	}

	protected void computeDefaultNColumns() { // ToDo

	}

	protected void drawSingleCell(int n, Graphics g) {
		int xPos = x[n], yPos = y[n];
		hotspots[n] = null;
		if (n < 0 || n >= timeClassIdxs.length || timeClassIdxs[n] < 0 || timeClassIdxs[n] >= allClassColors.length || allClassColors[timeClassIdxs[n]] == null)
			return;
		g.setColor(allClassColors[timeClassIdxs[n]]);
		int x = 2 + xPos * cellSizeX, y = 2 + yPos * (cellSizeY + 1);
		g.fillRect(x, y, cellSizeX, cellSizeY);
		hotspots[n] = new Rectangle(x, y, cellSizeX, cellSizeY);
	}

	public void draw(Graphics g) {
		if (popM == null) {
			popM = new PopupManager(this, "", true);
			popM.setOnlyForActiveWindow(false);
			hotspots = new Rectangle[times.length];
		}
		for (int i = 0; i < times.length; i++) {
			drawSingleCell(i, g);
		}
		if (nRows < 2 && hotspots != null && hotspots[0] != null) {
			g.setColor(Color.gray);
			int x1 = hotspots[0].x, x2 = hotspots[hotspots.length - 1].x + hotspots[hotspots.length - 1].width;
			g.drawLine(x1, 2, x2, 2);
			g.drawLine(x1, 1 + cellSizeY, x2, 1 + cellSizeY);
			g.drawLine(x1, 2, x1, 1 + cellSizeY);
			g.drawLine(x2, 2, x2, 1 + cellSizeY);
		}
	}

	@Override
	public void paint(Graphics g) {
		draw(g);
	}

	public void redraw() {
		Graphics g = getGraphics();
		if (g != null) {
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, getSize().width, getSize().height);
			draw(g);
			g.dispose();
		}
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		if (popM == null)
			return;
		int found = -1;
		for (int i = 0; i < hotspots.length && found == -1; i++)
			if (hotspots[i] != null && hotspots[i].contains(e.getX(), e.getY())) {
				found = i;
			}
		if (found >= 0) {
			String str = "* " + found + "\n" + times[found].toString() + "\n";
			if (timeClassIdxs[found] >= 0 && allClassLabels[timeClassIdxs[found]] != null) {
				str += "\n" + allClassLabels[timeClassIdxs[found]];
			}
			popM.setText(str);
			popM.setKeepHidden(false);
			popM.startShow(e.getX(), e.getY());
		} else {
			String str = "";
			popM.setText(str);
		}
	}

	@Override
	public void mouseDragged(MouseEvent e) {
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(300, 300);
	}

//---------------------- propagation of time limits corresponding to the selected cells ----------
	public void setTimeLimits(TimeLimits limits[]) {
		this.timeLimits = limits;
	}

	public void makeTimeLimits() {
		if (times == null || times.length < 2)
			return;
		timeLimits = new TimeLimits[times.length];
		for (int i = 0; i < times.length; i++) {
			timeLimits[i] = new TimeLimits();
			timeLimits[i].init(1, 1);
			TimeMoment t2 = (i + 1 < times.length) ? times[i + 1] : times[i];
			t2 = t2.getCopy();
			if (i + 1 < times.length) {
				t2.add(-1);
			} else {
				t2.add(times[1].subtract(times[0]) - 1);
			}
			timeLimits[i].addLimit(times[i], t2);
		}
	}

	public TimeLimits getTimeLimit(int idx) {
		if (timeLimits == null || idx < 0 || idx >= timeLimits.length)
			return null;
		return timeLimits[idx];
	}

	public void addTimeLimitsListener(PropertyChangeListener list) {
		if (list == null)
			return;
		if (timeLimitsListeners == null) {
			timeLimitsListeners = new Vector<PropertyChangeListener>(5, 5);
		}
		timeLimitsListeners.addElement(list);
	}

	public void removeTimeLimitsListener(PropertyChangeListener list) {
		if (list == null || timeLimitsListeners == null)
			return;
		int idx = timeLimitsListeners.indexOf(list);
		if (idx >= 0) {
			timeLimitsListeners.removeElementAt(idx);
		}
	}

	public void notifyTimeLimitsChange(int cellIdx) {
		if (timeLimitsListeners == null || timeLimitsListeners.size() < 1)
			return;
		if (timeLimits == null) {
			makeTimeLimits();
		}
		TimeLimits lim = null;
		if (timeLimits != null && cellIdx >= 0 && cellIdx < timeLimits.length) {
			lim = timeLimits[cellIdx];
		}
		PropertyChangeEvent e = new PropertyChangeEvent(this, "time_limits", null, lim);
		for (int i = 0; i < timeLimitsListeners.size(); i++) {
			timeLimitsListeners.elementAt(i).propertyChange(e);
		}
	}
}
