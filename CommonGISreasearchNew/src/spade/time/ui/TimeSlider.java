package spade.time.ui;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Hashtable;
import java.util.Vector;

import spade.analysis.system.Supervisor;
import spade.lib.basicwin.Destroyable;
import spade.time.FocusInterval;
import spade.time.TimeMoment;
import spade.time.TimeUtil;

/**
* A UI component for displaying and controlling currently selected time moments
* and intervals.
*/
public class TimeSlider extends Canvas implements MouseListener, MouseMotionListener, PropertyChangeListener, Destroyable {
	public static int mm = Math.round(Toolkit.getDefaultToolkit().getScreenResolution() / 25.33f), prefH = 6 * mm, minSlW = mm, colorBarH = 2 * mm;
	/**
	* The focus time interval manipulated by this slider
	*/
	protected FocusInterval focusInt = null;
	/**
	 * The supervisor can send notifications about coloring of time moments
	 */
	protected Supervisor supervisor = null;
	/**
	* The step between successive time moments
	*/
	protected int step = 1;
	/**
	* Current screen width of the slider
	*/
	protected int scrW = 0;
	/**
	* Current screen positions of the start and the end of the selected interval
	*/
	protected int scrStartPos = 0, scrEndPos = 0;
	/**
	 * Assignments of colors to time intervals.
	 * Contains arrays Object[2] = [TimeMoment,Color],
	 * where the time moments are the starting moments of the intervals.
	 * The intervals are chronologically sorted.
	 */
	protected Vector<Object[]> colorsForTimes = null;

	protected boolean drag = false;
	protected boolean enabled = true;
	protected boolean destroyed = false;

	public TimeSlider(FocusInterval fint) {
		this(fint, 1);
	}

	public TimeSlider(FocusInterval fint, int step) {
		focusInt = fint;
		this.step = step;
		if (focusInt != null) {
			focusInt.addPropertyChangeListener(this);
			addMouseListener(this);
			addMouseMotionListener(this);
		}
	}

	/**
	 * Returns the supervisor if available
	 */
	public Supervisor getSupervisor() {
		return supervisor;
	}

	/**
	 * The supervisor can send notifications about coloring of time moments
	 */
	public void setSupervisor(Supervisor supervisor) {
		this.supervisor = supervisor;
		if (supervisor != null) {
			supervisor.addPropertyChangeListener(this);
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
				TimeMoment t0 = focusInt.getDataIntervalStart(), tEnd = focusInt.getDataIntervalEnd();
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
	}

	/**
	* Returns the focus time interval manipulated by this slider
	*/
	public FocusInterval getFocusInterval() {
		return focusInt;
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(20 * mm, prefH);
	}

	protected int absToScr(long absPos) {
		return 1 + Math.round(1.0f * absPos * (scrW - 2) / focusInt.getDataIntervalLength());
	}

	protected long scrToAbs(int scrPos) {
		double val = 1.0 * (scrPos - 1) * focusInt.getDataIntervalLength() / (scrW - 2);
		if (step <= 1)
			return (long) Math.floor(val);
		return step * Math.round(val / step);
	}

	protected int getTimeAxisPos(TimeMoment t) {
		if (t == null || focusInt == null)
			return -1;
		TimeMoment start = focusInt.getDataIntervalStart();
		if (start == null)
			return -1;
		char c = t.getPrecision();
		t.setPrecision(start.getPrecision());
		long absPos = t.subtract(start, start.getPrecision());
		t.setPrecision(c);
		return absToScr(absPos);
	}

	@Override
	public void setEnabled(boolean flag) {
		enabled = flag;
		repaint();
	}

	@Override
	public void paint(Graphics g) {
		draw(g);
	}

	public void draw(Graphics g) {
		if (g == null || focusInt == null)
			return;
		Dimension d = getSize();
		scrW = d.width;
		g.setColor(getBackground());
		g.fillRect(0, 0, scrW, d.height);
		int slH = d.height - 1, slY0 = 1;
		if (colorsForTimes != null) {
			slY0 += colorBarH;
			slH -= colorBarH;
			for (int i = 0; i < colorsForTimes.size(); i++) {
				Object pair[] = colorsForTimes.elementAt(i);
				if (pair[0] instanceof TimeMoment) {
					int x0 = getTimeAxisPos((TimeMoment) pair[0]);
					int x1 = scrW - 1;
					if (i < colorsForTimes.size() - 1) {
						Object pair1[] = colorsForTimes.elementAt(i + 1);
						if (pair1[0] instanceof TimeMoment) {
							x1 = getTimeAxisPos((TimeMoment) pair1[0]);
						}
					}
					if (pair[1] != null && (pair[1] instanceof Color)) {
						g.setColor((Color) pair[1]);
					} else {
						g.setColor(getBackground());
					}
					g.fillRect(x0, 1, x1 - x0, colorBarH - 1);
				}
			}
		}
		scrStartPos = absToScr(focusInt.getCurrStartPos());
		scrEndPos = absToScr(focusInt.getCurrEndPos());
		int slW = scrEndPos - scrStartPos + 1;
		if (slW < minSlW) {
			slW = minSlW;
			scrEndPos = scrStartPos + slW;
		}
		if (scrStartPos + slW > scrW) {
			scrStartPos = scrW - slW;
		}
		g.setColor(Color.blue);
		g.fillRect(scrStartPos, slY0, slW, slH);
		g.setColor(Color.black);
		g.drawRect(0, 1, scrW - 1, d.height - 2);
	}

	public int getStartPosScr() {
		return scrStartPos;
	}

	public int getEndPosScr() {
		if (scrW < 10 || scrEndPos - scrStartPos + 1 < minSlW) {
			Dimension d = getSize();
			scrW = d.width;
			scrStartPos = absToScr(focusInt.getCurrStartPos());
			scrEndPos = absToScr(focusInt.getCurrEndPos());
		}
		return scrEndPos;
	}

	/**
	* Reacts to changes of the current focus interval. Repaints itself to reflect
	* the changes.
	*/
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getSource().equals(focusInt) && e.getPropertyName().equals("current_interval")) {
			draw(getGraphics());
		} else if (e.getSource().equals(supervisor) && e.getPropertyName().equals(Supervisor.eventTimeColors)) {
			if (e.getNewValue() == null) {
				colorsForTimes = null;
			} else {
				tryGetColorsForTimes();
			}
			draw(getGraphics());
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (!enabled || !focusInt.hasValidDataInterval())
			return;
		int x = e.getX();
		int slW = scrEndPos - scrStartPos + 1;
		if (slW < mm) {
			slW = mm;
		}
		if (x >= scrStartPos - mm && x <= scrStartPos + slW + mm) {
			drag = true;
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		drag = false;
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if (!enabled || !focusInt.hasValidDataInterval())
			return;
		long absx = scrToAbs(e.getX());
		if (absx < 0) {
			absx = 0;
		}
		if (absx > focusInt.getDataIntervalLength()) {
			absx = focusInt.getDataIntervalLength();
		}
		long currStartPos = focusInt.getCurrStartPos(), currEndPos = focusInt.getCurrEndPos();
		switch (focusInt.getWhatIsFixed()) {
		case FocusInterval.START:
			focusInt.moveEndBy(absx - currEndPos);
			break;
		case FocusInterval.END:
			focusInt.moveStartBy(absx - currStartPos);
			break;
		case FocusInterval.LENGTH:
			focusInt.moveIntervalBy(absx - (currStartPos + currEndPos) / 2);
			break;
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if (!enabled || !focusInt.hasValidDataInterval())
			return;
		long absx = scrToAbs(e.getX());
		if (absx < 0 || absx > focusInt.getDataIntervalLength())
			return;
		long currStartPos = focusInt.getCurrStartPos(), currEndPos = focusInt.getCurrEndPos();
		switch (focusInt.getWhatIsFixed()) {
		case FocusInterval.START:
			focusInt.moveEndBy(absx - currEndPos);
			break;
		case FocusInterval.END:
			focusInt.moveStartBy(absx - currStartPos);
			break;
		case FocusInterval.LENGTH:
			focusInt.moveIntervalBy(absx - (currStartPos + currEndPos) / 2);
			break;
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void mouseMoved(MouseEvent e) {
	}

	@Override
	public void destroy() {
		if (!destroyed) {
			focusInt.removePropertyChangeListener(this);
			if (supervisor != null) {
				supervisor.removePropertyChangeListener(this);
			}
			destroyed = true;
		}
	}

	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

	/**
	* Returns the properties which can be stored in an ASCII file. Each property
	* has its unique key and a value, both are strings.
	*/
	public Hashtable getProperties() {
		Hashtable prop = null;
		if (focusInt != null) {
			prop = focusInt.getProperties();
		}
		if (prop == null) {
			prop = new Hashtable();
		}
		prop.put("step", String.valueOf(step));
		return prop;
	}

	/**
	* Restores the properties, which could be, for example, retrieved from a file.
	* Each property has its unique key and a value, both are strings.
	*/
	public void setProperties(Hashtable param) {
		if (param == null || param.isEmpty())
			return;
		String str = (String) param.get("step");
		if (str != null) {
			try {
				int s = Integer.valueOf(str).intValue();
				if (s > 0) {
					step = s;
				}
			} catch (NumberFormatException e) {
			}
		}
		if (focusInt != null) {
			focusInt.setProperties(param);
		}
	}
}