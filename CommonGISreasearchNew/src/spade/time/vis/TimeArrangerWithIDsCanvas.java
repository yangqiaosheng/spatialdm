package spade.time.vis;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Label;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import spade.lib.basicwin.Destroyable;
import spade.time.FocusInterval;
import spade.time.TimeMoment;
import spade.vis.action.HighlightListener;
import spade.vis.action.Highlighter;
import spade.vis.database.DataTable;
import spade.vis.database.ObjectFilter;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: Feb 19, 2010
 * Time: 3:05:01 PM
 * Canvas with time moments colored by events presence.
 * Reacts to mouse click by selecting corresponding objects
 */
public class TimeArrangerWithIDsCanvas extends TimeArrangerCanvas implements PropertyChangeListener, MouseListener, ComponentListener, HighlightListener, Destroyable {

	protected Highlighter highlighter = null;
	protected TimeGraph tigr = null;
	protected Label lResults = null;
	protected DataTable table = null;
	protected ObjectFilter tf = null;
	protected Vector<Vector<String>> vvIds = null;
	protected Vector<Vector<Integer>> vvEvtNums = null;

	protected DataTable tblEvents = null;
	protected ObjectFilter tblEventsObjFilter = null;

	protected int nEvents = 0, nEventsAfterFilter = 0, nTimeMoments = 0, nTimeMomentsAfterFilter = 0, nObjects = 0, nObjectsAfterFilter = 0;

	protected int idxClicked = -1;
	protected int xx[] = null;

	public int desiredWidth = 1000, desiredHeight = 20;

	/**
	 *
	 * table is used for reaction to query
	 * tigr is for horisontal aligning (if single row) and setting current time
	 * lResults text is set by this class
	 * highlighter if selection
	 * times: time references of the cells
	 * vvIds: Ids of objects (time series) having events in each time moment
	 * vvEvtNums: Integers representing record numbbers of events in the event table
	 */
	public TimeArrangerWithIDsCanvas(DataTable table, TimeGraph tigr, DataTable tblEvents, Label lResults, Highlighter highlighter, TimeMoment times[], Vector<Vector<String>> vvIds, Vector<Vector<Integer>> vvEvtNums) {
		super(times.length, times, null, null, null);
		this.tigr = tigr;
		this.table = table;
		this.lResults = lResults;
		this.highlighter = highlighter;
		this.vvIds = vvIds;
		this.vvEvtNums = vvEvtNums;
		this.tblEvents = tblEvents;
		if (tblEvents != null) {
			tblEvents.addPropertyChangeListener(this);
			tblEventsObjFilter = tblEvents.getObjectFilter();
		}
		if (tblEventsObjFilter != null) {
			tblEventsObjFilter.addPropertyChangeListener(this);
		}
		updateStatistics();
		Color col[] = getAllColors();
		String str[] = getAllLabels();
		super.setClasses(getAllLabels(), getAllColors(), getTimeClassIdxs());
		cellSizeX = 5;
		addMouseListener(this);
		addComponentListener(this);
		if (table != null) {
			table.addPropertyChangeListener(this);
			tf = table.getObjectFilter();
			if (tf != null) {
				tf.addPropertyChangeListener(this);
			}
		}
		if (highlighter != null) {
			highlighter.addHighlightListener(this);
		}
		if (tigr != null) {
			this.addTimeLimitsListener(tigr);
		}
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(desiredWidth, desiredHeight);
	}

	protected int[] getTimeClassIdxs() {
		int idx[] = new int[vvIds.size()];
		for (int i = 0; i < idx.length; i++) {
			idx[i] = i;
		}
		return idx;
	}

	protected int getNsatisfyingQuery(Vector<String> v) {
		if (v == null)
			return 0;
		int n = 0;
		for (int i = 0; i < v.size(); i++)
			if (tf == null || tf.isActive(v.elementAt(i))) {
				n++;
			}
		return n;
	}

	protected Color[] getAllColors() {
		if (vvIds == null || vvIds.size() == 0)
			return null;
		Color color[] = new Color[vvIds.size()];
		int max = 0;
		for (int vIdx = 0; vIdx < vvIds.size(); vIdx++) {
			Vector<String> v = vvIds.elementAt(vIdx);
			if (v == null || v.size() == 0) {
				continue;
			}
			Vector<String> vv = new Vector<String>(v.size(), 10);
			for (int i = 0; i < v.size(); i++)
				if (tblEventsObjFilter == null || (vvEvtNums.elementAt(vIdx) != null && tblEventsObjFilter.isActive(vvEvtNums.elementAt(vIdx).elementAt(i)))) {
					vv.addElement(v.elementAt(i));
				}
			int size = getNsatisfyingQuery(vv);
			if (size > max) {
				max = size;
			}
		}
		for (int vIdx = 0; vIdx < vvIds.size(); vIdx++) {
			Vector<String> v = vvIds.elementAt(vIdx);
			Vector<String> vv = null;
			if (v != null && v.size() > 0) {
				vv = new Vector<String>(Math.max(1, v.size()), 10);
				for (int i = 0; i < v.size(); i++)
					if (tblEventsObjFilter == null || (vvEvtNums.elementAt(vIdx) != null && tblEventsObjFilter.isActive(vvEvtNums.elementAt(vIdx).elementAt(i)))) {
						vv.addElement(v.elementAt(i));
					}
			}
			if (vv == null || max == 0) {
				color[vIdx] = Color.WHITE;
			} else {
				if (vv.size() > 0) {
					nTimeMoments++;
					nEvents += vv.size();
				}
				int size = getNsatisfyingQuery(vv);
				if (size > 0) {
					nTimeMomentsAfterFilter++;
					nEventsAfterFilter += size;
				}
				int rgb = (int) (255 * (1 - (float) size / max));
				color[vIdx] = new Color(rgb, rgb, rgb);
			}
		}
		updateStatistics();
		return color;
	}

	protected String[] getAllLabels() {
		String str[] = new String[vvIds.size()];
		for (int vIdx = 0; vIdx < vvIds.size(); vIdx++) {
			Vector<String> v = vvIds.elementAt(vIdx);
			Vector<String> vv = null;
			if (v != null && v.size() > 0) {
				vv = new Vector<String>(Math.max(1, v.size()), 10);
				for (int i = 0; i < v.size(); i++) {
					//if (tblEventsObjFilter==null || tblEventsObjFilter.isActive(vvEvtNums.elementAt(vIdx).elementAt(i)))
					//vv.addElement(v.elementAt(i));
					Vector<Integer> vi = vvEvtNums.elementAt(vIdx);
					int j = vi.elementAt(i);
					if (tblEventsObjFilter == null || tblEventsObjFilter.isActive(j)) {
						vv.addElement(v.elementAt(i));
					}
				}

			}
			if (v == null || v.size() == 0) {
				str[vIdx] = "no events";
			} else {
				if (v.size() == 1) {
					str[vIdx] = "1 event";
				} else {
					str[vIdx] = (v.size()) + " events";
					Vector<String> v_pl = new Vector<String>(v.size(), 10);
					for (int i = 0; i < v.size(); i++)
						if (!v_pl.contains(v.elementAt(i))) {
							v_pl.addElement(v.elementAt(i));
						}
					if (v_pl.size() < v.size()) {
						str[vIdx] += " in " + v_pl.size() + " places";
					}
				}
				if (nEvents > nEventsAfterFilter) {
					str[vIdx] += "; after query: " + getNsatisfyingQuery(vv) + " event(s)";
				}
			}
		}
		return str;
	}

	@Override
	protected void drawSingleCell(int n, Graphics g) {
		int xPos = x[n], yPos = y[n];
		hotspots[n] = null;
		int x = (xx == null) ? 2 + cellSizeX * xPos : xx[xPos], y = 2 + yPos * (cellSizeY + 1), dx = (xx == null) ? cellSizeX : (xPos < xx.length - 1) ? xx[xPos + 1] - xx[xPos] : cellSizeX;
		if (dx <= 0) {
			dx = cellSizeX;
		}
		if (n < 0 || n >= timeClassIdxs.length || timeClassIdxs[n] < 0 || timeClassIdxs[n] >= allClassColors.length || allClassColors[timeClassIdxs[n]] == null) {
			g.setColor(Color.white);
			g.fillRect(x + 1, y, dx - 1, cellSizeY);
			return;
		}
		g.setColor(allClassColors[timeClassIdxs[n]]);
		g.fillRect(x + 1, y, dx - 1, cellSizeY);
		if (!allClassColors[timeClassIdxs[n]].equals(Color.white)) {
			g.setColor(Color.gray);
			g.drawRect(x, y, dx, cellSizeY - 1);
		}
		hotspots[n] = new Rectangle(x, y, dx, cellSizeY);

		boolean allSelected = vvIds.elementAt(n) != null && selected != null && selected.size() > 0;
		if (allSelected) {
			for (int i = 0; i < vvIds.elementAt(n).size() && allSelected; i++) {
				allSelected = selected.contains(vvIds.elementAt(n).elementAt(i));
			}
		}
		if (allSelected) { // idxClicked==n && tigr!=null
			g.setColor(Color.WHITE);
			g.drawLine(hotspots[n].x, hotspots[n].y + 1, hotspots[n].x + hotspots[n].width, hotspots[n].y + 1);
			g.drawLine(hotspots[n].x, hotspots[n].y + hotspots[n].height - 2, hotspots[n].x + hotspots[n].width, hotspots[n].y + hotspots[n].height - 2);
			g.setColor(Color.BLACK);
			g.drawLine(hotspots[n].x, hotspots[n].y + 2, hotspots[n].x + hotspots[n].width, hotspots[n].y + 2);
			g.drawLine(hotspots[n].x, hotspots[n].y + hotspots[n].height - 3, hotspots[n].x + hotspots[n].width, hotspots[n].y + hotspots[n].height - 3);
		}

	}

	protected void updateStatistics() {
		//ToDo: debug
		Vector<String> allIds = new Vector(100, 100); // IDs of all time series involved in events
		for (int i = 0; i < vvIds.size(); i++)
			if (vvIds.elementAt(i) != null) {
				for (int j = 0; j < vvIds.elementAt(i).size(); j++)
					if (allIds.indexOf(vvIds.elementAt(i).elementAt(j)) == -1) {
						allIds.addElement(vvIds.elementAt(i).elementAt(j));
					}
			}
		nObjects = allIds.size();
		allIds = new Vector(100, 100); // now - only IDs of time series with active events
		nEvents = nEventsAfterFilter = 0;
		nObjectsAfterFilter = 0;
		nTimeMoments = nTimeMomentsAfterFilter = 0;
		for (int vIdx = 0; vIdx < vvIds.size(); vIdx++) {
			Vector<Integer> ven = vvEvtNums.elementAt(vIdx);
			if (ven != null && ven.size() > 0) {
				Vector<String> vidx = vvIds.elementAt(vIdx);
				nTimeMoments++;
				nEvents += vidx.size();
				Vector<String> v = new Vector<String>(vidx.size(), 10);
				for (int n = 0; n < ven.size(); n++)
					if (tblEventsObjFilter == null || tblEventsObjFilter.isActive(ven.elementAt(n))) {
						String id = vidx.elementAt(n);
						v.addElement(id);
						if (allIds.indexOf(id) == -1) {
							allIds.addElement(id);
						}
					}
				int size = getNsatisfyingQuery(v);
				if (size > 0) {
					nTimeMomentsAfterFilter++;
					nEventsAfterFilter += size;
				}
			}
		}
		nObjectsAfterFilter = getNsatisfyingQuery(allIds);
		updateResultsLabel();
	}

	protected void updateResultsLabel() {
		if (lResults == null)
			return;
		if (nEvents == 0) {
			lResults.setText("no events detected");
		} else {
			String str = nEvents + " events occured in " + nObjects + " time series at " + nTimeMoments + " time moments";
			if (nEventsAfterFilter < nEvents) {
				str += "; after query: " + nEventsAfterFilter + " events, " + nObjectsAfterFilter + " time series, " + nTimeMomentsAfterFilter + " time moments";
			}
			lResults.setText(str);
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		if (pce.getSource().equals(table)) {
			if (pce.getPropertyName().equalsIgnoreCase("filter")) {
				if (tf != null) {
					tf.removePropertyChangeListener(this);
				}
				tf = table.getObjectFilter();
				if (tf != null) {
					tf.addPropertyChangeListener(this);
				}
			}
		}
		if (pce.getSource().equals(tf) && pce.getPropertyName().equals("destroyed")) {
			tf.removePropertyChangeListener(this);
			tf = null;
		}
		if (pce.getSource().equals(tblEvents)) {
			if (pce.getPropertyName().equalsIgnoreCase("filter")) {
				if (tblEventsObjFilter != null) {
					tblEventsObjFilter.removePropertyChangeListener(this);
				}
				tblEventsObjFilter = tblEvents.getObjectFilter();
				if (tblEventsObjFilter != null) {
					tblEventsObjFilter.addPropertyChangeListener(this);
				}
			}
		}
		if (pce.getSource().equals(tblEventsObjFilter)) {
			//System.out.println("* filter");
		}
		updateStatistics();
		super.setClasses(getAllLabels(), getAllColors(), getTimeClassIdxs());
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		int vIdx = -1;
		for (int i = 0; i < hotspots.length && vIdx == -1; i++)
			if (hotspots[i] != null && hotspots[i].contains(e.getX(), e.getY())) {
				vIdx = i;
			}
		notifyTimeLimitsChange(vIdx);
		if (tigr != null && vIdx >= 0 && vIdx < times.length) {
			FocusInterval fint = tigr.getFocusInterval();
			if (fint != null) {
				fint.setCurrIntervalStart(times[vIdx]);
			}
		}
		if (vIdx >= 0) {
			Vector<String> v = vvIds.elementAt(vIdx), vv = new Vector<String>((v == null) ? 10 : v.size(), 100);
			if (v != null && v.size() > 0) {
				for (int i = 0; i < v.size(); i++)
					if (tf == null || tf.isActive(v.elementAt(i))) {
						vv.addElement(v.elementAt(i));
					}
			}
			if (vv.size() > 0) {
				highlighter.replaceSelectedObjects(this, vv);
			} else {
				highlighter.clearSelection(this);
			}
		}
		idxClicked = vIdx;
		repaint();
	}

	@Override
	public void mousePressed(MouseEvent e) {
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void componentResized(ComponentEvent ce) {
		if (tigr != null && lResults != null) { // within RStatisticsPanel, needs to be adjusted with time graph
			tigr.updateX0andWidth();
			if (xx == null) {
				xx = new int[times.length];
			}
			for (int i = 0; i < times.length; i++) {
				xx[i] = tigr.getScrX(times[i]);
			}
		} else {
			cellSizeX = getSize().width / nColumns;
			cellSizeY = (int) Math.floor((getSize().height - 5) / nRows);
		}
	}

	@Override
	public void componentShown(ComponentEvent ce) {
	}

	@Override
	public void componentHidden(ComponentEvent ce) {
	}

	@Override
	public void componentMoved(ComponentEvent ce) {
	}

	protected boolean destroyed = false;

	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

	@Override
	public void destroy() {
		if (destroyed)
			return;
		if (table != null) {
			table.removePropertyChangeListener(this);
		}
		if (tf != null) {
			tf.removePropertyChangeListener(this);
		}
		if (highlighter != null) {
			highlighter.removeHighlightListener(this);
		}
		if (tblEvents != null) {
			tblEvents.removePropertyChangeListener(this);
		}
		if (tblEventsObjFilter != null) {
			tblEventsObjFilter.removePropertyChangeListener(this);
		}
		destroyed = true;
		//System.out.println("* event bar is destroyed");
	}

	Vector selected = null;

	@Override
	public void selectSetChanged(Object source, String setId, Vector selected) {
		if (table != null && setId.equals(table.getEntitySetIdentifier())) {
			this.selected = selected;
			repaint();
		}
	}

	@Override
	public void highlightSetChanged(Object source, String setId, Vector highlighted) {
	}

}
