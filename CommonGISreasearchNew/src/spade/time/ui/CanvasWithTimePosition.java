package spade.time.ui;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import spade.time.TimeMoment;
import spade.time.TimePositionNotifier;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: Aug 19, 2009
 * Time: 3:52:46 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class CanvasWithTimePosition extends Canvas implements PropertyChangeListener, MouseListener, MouseMotionListener {
	/**
	 * The shift in the horizontal dimension (the main canvas may be scrolled)
	 */
	protected int shift = 0;
	/**
	 * The difference between the screen positions of the plot canvas and the
	 * label canvas, which is used for correct alignment of the grid lines/ticks
	 */
	protected int alignDiff = 0;
	protected boolean alignDiffKnown = false;

	/**
	 * Used to notify other components about the relative mouse position and
	 * the corresponding time moment.
	 */
	protected TimePositionNotifier tpn = null;
	/**
	 * The last relative position of the mouse.
	 * Used for erasing the previously drawn line.
	 */
	protected int lastXPos = -1;

	public CanvasWithTimePosition() {
		super();
		addMouseListener(this);
		addMouseMotionListener(this);
	}

	public TimePositionNotifier getTimePositionNotifier() {
		return tpn;
	}

	public void setTimePositionNotifier(TimePositionNotifier tpn) {
		this.tpn = tpn;
		if (tpn != null) {
			tpn.addPropertyChangeListener(this);
		}
	}

	/**
	 * Sets the shift in the horizontal dimension (the main canvas may be scrolled)
	 */
	public void setShift(int shift) {
		this.shift = shift;
	}

	public int getShift() {
		return shift;
	}

	/**
	 * Sets the difference between the screen positions of the plot canvas and the
	 * label canvas, which is used for correct alignment of the grid lines/ticks
	 */
	public void setAlignmentDiff(int diff) {
		alignDiff = diff;
		alignDiffKnown = true;
	}

	protected void drawLine(Graphics g) {
		if (lastXPos < 0)
			return;
		if (g == null)
			return;
		int x = lastXPos - shift + alignDiff;
		//g.setXORMode(Color.gray);
		g.setColor(Color.yellow);
		g.drawLine(x, 0, x, getSize().height);
		//g.setPaintMode();
	}

	protected void drawLine() {
		if (lastXPos < 0)
			return;
		Graphics g = getGraphics();
		if (g != null) {
			drawLine(g);
			g.dispose();
		}
	}

	public void draw(Graphics g) {
		//to be implemented in the descendants
		//...
		return;
	}

	protected void restore() {
		Graphics g = getGraphics();
		if (g == null)
			return;
		int x = lastXPos - shift + alignDiff;
		lastXPos = -1;
		int h = getSize().height;
		g.setClip(x, 0, 1, h);
		draw(g);
		g.setClip(null);
		g.dispose();
	}

	@Override
	public void paint(Graphics g) {
		draw(g);
		if (lastXPos >= 0) {
			drawLine(g);
		}
	}

	public synchronized void redraw() {
		Graphics g = getGraphics();
		if (g == null)
			return;
		draw(g);
		if (lastXPos >= 0) {
			drawLine(g);
		}
		g.dispose();
	}

	protected abstract int getTimeAxisPos(TimeMoment t);

	public abstract TimeMoment getTimeForAxisPos(int pos);

	public abstract void setPositionInfoInTPN(TimePositionNotifier tpn);

	// ---------- PropertyChangeListener ---------------------------------
	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		if (pce.getPropertyName().equals("current_moment") && (pce.getNewValue() instanceof TimePositionNotifier)) {
			TimePositionNotifier tPN = (TimePositionNotifier) pce.getNewValue();
			int xPos = tPN.getMouseX();
			if (xPos < 0 && tPN.getMouseTime() != null) {
				xPos = getTimeAxisPos(tPN.getMouseTime());
			}
			if (lastXPos == xPos)
				return;
			if (lastXPos >= 0) {
				restore();
			}
			lastXPos = xPos;
			if (lastXPos >= 0) {
				drawLine();
			}
		}
	}

	// ---------- MouseListener ---------------------------------
	@Override
	public void mouseMoved(MouseEvent e) {
		if (tpn != null) {
			tpn.setMouseX(e.getX() + shift - alignDiff);
			tpn.setMouseTime(getTimeForAxisPos(tpn.getMouseX()));
			setPositionInfoInTPN(tpn);
			tpn.notifyPositionChange(this);
		}
	}

	@Override
	public void mouseExited(MouseEvent e) {
		if (tpn != null) {
			tpn.setMouseX(-1);
			tpn.setMouseTime(null);
			tpn.notifyPositionChange(this);
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if (tpn != null && checkRightButtonPressed(e)) {
			int pos = e.getX() + shift - alignDiff;
			TimeMoment t = getTimeForAxisPos(pos);
			if (t != null) {
				tpn.notifyTimeSelection(t);
			}
		}
	}

	public boolean checkRightButtonPressed(MouseEvent e) {
		if (e == null)
			return false;
		if ((e.getModifiers() & InputEvent.BUTTON2_MASK) == InputEvent.BUTTON2_MASK)
			return true;
		if ((e.getModifiers() & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK)
			return true;
		return false;
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

}
