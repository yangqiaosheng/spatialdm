package spade.time.vis;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.Vector;

import spade.lib.basicwin.CManager;
import spade.lib.basicwin.Metrics;
import spade.lib.ui.RulerController;
import spade.lib.util.IntArray;
import spade.time.TimeMoment;
import spade.time.TimePositionNotifier;
import spade.time.ui.CanvasWithTimePosition;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 02-Feb-2007
 * Time: 17:24:17
 */
public class TimeLineLabelsCanvas extends CanvasWithTimePosition implements ComponentListener {
	/**
	 * The start and end moments of the time line.
	 */
	protected TimeMoment start = null, end = null;
	/**
	 * The length of the time interval
	 */
	protected int timeLen = 0;
	/**
	 * The width and height of the required drawing (which may differ from the
	 * width and height of the canvas)
	 */
	protected int drWidth = 0, drHeight = 0;
	/**
	 * The time intervals represented by grid lines or ticks (this does not
	 * necessarily include the starting and ending moments)
	 */
	protected Vector gridTimes = null;
	/**
	 * The positions of the grid lines (this does not necessarily include the
	 * positions of the starting and ending moments)
	 */
	protected IntArray gridPos = null;
	/**
	 * In a case when gridTimes and gridPos are not imposed by another
	 * component, a TimeGrid is used for drawing ruler ticks
	 */
	protected TimeGrid timeGrid = null;
	/**
	 * A TimeLineLabelsCanvas may be aligned with a component implementing the
	 * interface RulerController and informing about the required positions
	 * of the start and the end of the time line
	 */
	protected RulerController rulerController = null;
	/**
	 * Assignments of colors to time intervals.
	 * Contains arrays Object[2] = [TimeMoment,Color],
	 * where the time moments are the starting moments of the intervals.
	 * The intervals are chronologically sorted.
	 */
	protected Vector<Object[]> colorsForTimes = null;

	@Override
	public Dimension getPreferredSize() {
		if (drWidth < 1) {
			drWidth = 100;
		}
		if (drHeight < 1) {
			drHeight = Metrics.fh;
		}
		if (drHeight < 1) {
			drHeight = 15;
		}
		return new Dimension(drWidth, drHeight);
	}

	public int getCurrentWidth() {
		return drWidth;
	}

	public int getCurrentHeight() {
		return drHeight;
	}

	public void setTimeInterval(TimeMoment t1, TimeMoment t2) {
		start = t1;
		end = t2;
		if (start != null && end != null) {
			timeLen = (int) end.subtract(start);
		} else {
			timeLen = 0;
		}
	}

	/**
	 * Sets the width and height of the required drawing (which may differ from the
	 * width and height of the canvas)
	 */
	public void setRequiredSize(int width, int height) {
		drWidth = width;
		drHeight = height;
	}

	/**
	 * Sets the grid parameters: positions of the grid lines or ticks and the
	 * values of the corresponding time moments
	 */
	public void setGridParameters(IntArray gridPos, Vector gridTimes) {
		this.gridPos = gridPos;
		this.gridTimes = gridTimes;
	}

	/**
	 * A TimeLineLabelsCanvas may be aligned with a component implementing the
	 * interface RulerController and informing about the required positions
	 * of the start and the end of the time line
	 */
	public void setRulerController(RulerController rulerController) {
		this.rulerController = rulerController;
		if (rulerController instanceof Component) {
			((Component) rulerController).addComponentListener(this);
		}
	}

	public boolean hasRulerController() {
		return rulerController != null;
	}

	public void setColorsForTimes(Vector<Object[]> colorsForTimes) {
		this.colorsForTimes = colorsForTimes;
		if (isShowing()) {
			redraw();
		}
	}

	@Override
	protected int getTimeAxisPos(TimeMoment t) {
		if (t == null || start == null || timeLen < 1 || drWidth < 1)
			return -1;
		char c = t.getPrecision();
		t.setPrecision(start.getPrecision());
		int pos = (int) t.subtract(start) * drWidth / timeLen;
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

	protected void findAlignDiff() {
		if (alignDiffKnown)
			return;
		if (rulerController != null) {
			alignDiff = rulerController.getStartPos();
			if (rulerController instanceof Component) {
				Container parent = CManager.getWindow(this);
				if (parent == null) {
					parent = getParent();
				}
				Component c = (Component) rulerController;
				int x = c.getLocation().x;
				c = c.getParent();
				while (c != null && !parent.equals(c)) {
					x += c.getLocation().x;
					c = c.getParent();
				}
				int x0 = getLocation().x;
				c = getParent();
				while (c != null && !parent.equals(c)) {
					x0 += c.getLocation().x;
					c = c.getParent();
				}
				alignDiff += (x - x0);
			}
			alignDiffKnown = true;
		}
	}

	public void drawPlotBackground(Graphics g) {
		if (rulerController != null) {
			findAlignDiff();
			drWidth = rulerController.getEndPos() - rulerController.getStartPos();
		}
		Dimension size = getSize();
		int w = size.width, h = size.height;
		Color bkgColor = getBackground();
		if (colorsForTimes == null) {
			g.setColor(bkgColor);
			g.fillRect(0, 0, w + 1, h + 1);
		} else {
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
						cBkg = (Color) pair[1];
					}
					g.setColor(cBkg);
					g.fillRect(p0, 0, p1 - p0, h + 1);
				}
			}
		}
	}

	public void getGrid() {
		if ((gridPos == null || gridTimes == null) && timeGrid == null) {
			timeGrid = new TimeGrid();
			timeGrid.setTimeInterval(start, end);
		}
		if ((gridPos == null || gridTimes == null) && timeGrid != null) {
			if (drWidth != timeGrid.getTimeAxisLength()) {
				timeGrid.setTimeAxisLength(drWidth);
				timeGrid.computeGrid();
			}
			gridPos = timeGrid.getGridPositions();
			gridTimes = timeGrid.getGridTimeMoments();
		}
	}

	@Override
	public void draw(Graphics g) {
		if (g == null)
			return;
		drawPlotBackground(g);
		FontMetrics fm = g.getFontMetrics();
		if (Metrics.fh < 1 && fm.getHeight() > 0) {
			Metrics.fh = fm.getHeight();
		}
		Dimension size = getSize();
		int w = size.width, h = size.height;
		if (shift == 0) {
			g.drawLine(alignDiff, h + 1, alignDiff, h - 2);
		}
		int asc = fm.getAscent();
		int lastPos = drWidth - shift + alignDiff;
		if (lastPos <= w) {
			g.drawLine(lastPos, h + 1, lastPos, h - 2);
		}
		getGrid();
		int strLen = 0;
		if (gridPos != null && gridTimes != null && gridTimes.size() > 0) {
			g.setColor(Color.darkGray);
			drawGrid(g, h);
			for (int i = 0; i < gridTimes.size(); i++) {
				int len = fm.stringWidth(gridTimes.elementAt(i).toString());
				if (strLen < len) {
					strLen = len;
				}
			}
			drawTextLabels(g, fm, strLen, lastPos, h, asc);
		}
		if (strLen == 0) {
			String str1 = (start == null) ? "" : start.toString(), str2 = (end == null) ? "" : end.toString();
			strLen = Math.max(fm.stringWidth(str1), fm.stringWidth(str2));
			g.setColor(Color.darkGray);
			lastPos = drawTextLabelsStartEnd(g, str1, str2, fm, strLen, lastPos, w, h, asc);
		}
	}

	protected void drawGrid(Graphics g, int h) {
		for (int i = 0; i < gridPos.size(); i++) {
			int x = gridPos.elementAt(i) - shift + alignDiff;
			g.drawLine(x, h + 1, x, h - 2);
			if (colorsForTimes != null) {
				g.setColor(Color.white);
				g.drawLine(x + 1, h + 1, x + 1, h - 2);
				g.setColor(Color.darkGray);
			}
		}
	}

	protected void drawTextLabels(Graphics g, FontMetrics fm, int strLen, int lastPos, int h, int asc) {
		int pos1 = -shift;
		//if (strLen>shift) pos1+=strLen+10;
		if (pos1 < 0) {
			pos1 = 0;
		}
		int textStep = strLen + 10, gridStep = textStep;
		for (int i = 1; i < gridPos.size(); i++) {
			int diff = gridPos.elementAt(i) - gridPos.elementAt(i - 1);
			if (diff < gridStep) {
				gridStep = diff;
			}
		}
		if (gridStep >= textStep) {
			for (int i = 0; i < gridPos.size() && pos1 + strLen <= lastPos; i++) {
				int x = gridPos.elementAt(i) - shift + alignDiff;
				if (x >= pos1 - 3 && x + strLen <= lastPos) {
					String str = gridTimes.elementAt(i).toString();
					if (colorsForTimes != null) {
						g.setColor(Color.white);
						g.drawString(str, x + 2, asc + 2);
						g.setColor(Color.darkGray);
					}
					g.drawString(str, x + 1, asc + 1);
					pos1 = x + strLen;
				}
			}
		} else {
			int inc = 1;
			int step = gridStep;
			while (step < textStep) {
				step += gridStep;
				++inc;
			}
			int i0 = -1, pos = pos1;
			for (int i = 0; i < gridPos.size() && i0 < 0; i++) {
				int x = gridPos.elementAt(i) - shift + alignDiff;
				if (x >= pos1 - 3 && x + strLen <= lastPos && ((TimeMoment) gridTimes.elementAt(i)).isNice()) {
					i0 = i;
					pos = x;
					break;
				}
			}
			if (i0 < 0) {
				i0 = 0;
			}
			int pos0 = pos;
			for (int i = i0; i < gridPos.size() && pos + strLen < lastPos; i += inc) {
				int x = gridPos.elementAt(i) - shift + alignDiff;
				if (x >= pos - 3 && x + strLen <= lastPos) {
					String str = gridTimes.elementAt(i).toString();
					if (colorsForTimes != null) {
						g.setColor(Color.white);
						g.drawString(str, x + 2, asc + 2);
						g.setColor(Color.darkGray);
					}
					g.drawString(str, x + 1, asc + 1);
					pos = x + strLen;
				}
			}
			for (int i = i0 - inc; i >= 0 && pos - step >= pos1; i -= inc) {
				int x = gridPos.elementAt(i) - shift + alignDiff;
				if (x >= pos1 - 3) {
					String str = gridTimes.elementAt(i).toString();
					if (colorsForTimes != null) {
						g.setColor(Color.white);
						g.drawString(str, x + 2, asc + 2);
						g.setColor(Color.darkGray);
					}
					g.drawString(str, x + 1, asc + 1);
					pos = x;
				}
			}
		}
	}

	protected int drawTextLabelsStartEnd(Graphics g, String str1, String str2, FontMetrics fm, int strLen, int lastPos, int w, int h, int asc) {
		if (strLen > shift) {
			g.drawString(str1, 1 - shift + alignDiff, asc + 1);
		}
		if (lastPos <= w) {
			//g.drawLine(lastPos,h+1,lastPos,h-2);
			if (lastPos + strLen > w) {
				lastPos = w - strLen;
			}
			g.drawString(str2, lastPos, asc + 1);
			lastPos -= 10;
		}
		return lastPos;
	}

	/**
	 * Invoked when the RulerController's size changes.
	 */
	@Override
	public void componentResized(ComponentEvent e) {
		redraw();
	}

	/**
	 * Invoked when the RulerController's position changes.
	 */
	@Override
	public void componentMoved(ComponentEvent e) {
		alignDiffKnown = false;
		redraw();
	}

	/**
	 * Invoked when the RulerController has been made visible.
	 */
	@Override
	public void componentShown(ComponentEvent e) {
		redraw();
	}

	/**
	 * Invoked when the RulerController has been made invisible.
	 */
	@Override
	public void componentHidden(ComponentEvent e) {
		redraw();
	}

	@Override
	public void setPositionInfoInTPN(TimePositionNotifier tpn) {
		tpn.lastId = null;
		tpn.lastDataTime = null;
		tpn.lastMapX = tpn.lastMapY = tpn.lastValue = Double.NaN;
	}

}
