package spade.lib.basicwin;

import java.awt.Canvas;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class FocuserCanvas extends Canvas implements MouseListener, MouseMotionListener {
	protected static int height = Focuser.mainWidth;
	protected int lMarg = 10, rMarg = 10;

	protected boolean isVertical = false;
	protected Focuser focuser = null;

	public FocuserCanvas(Focuser focuser, boolean isVertical) {
		super();
		this.focuser = focuser;
		this.isVertical = isVertical;
		focuser.setCanvas(this);
		addMouseListener(this);
		addMouseMotionListener(this);
		focuser.setSpacingFromAxis(0);
		if (height <= Focuser.mainWidth) {
			height += Metrics.asc;
		}
	}

	public void setMargins(int leftMargin, int rightMargin) {
		lMarg = leftMargin;
		rMarg = rightMargin;
	}

	@Override
	public void paint(Graphics g) {
		focuser.setAlignmentParameters(lMarg, height, getSize().width - (lMarg + rMarg));
		focuser.draw(g);
	}

	@Override
	public Dimension getPreferredSize() {
		if (isVertical)
			return new Dimension(height, 100);
		return new Dimension(100, height);
	}

	protected boolean dragging = false;

	@Override
	public void mouseDragged(MouseEvent e) {
		if (focuser.isMouseCaptured()) {
			Graphics g = getGraphics();
			focuser.mouseDragged(e.getX(), e.getY(), g);
			g.dispose();
			return;
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {
		focuser.captureMouse(e.getX(), e.getY());
		if (focuser.isMouseCaptured()) {
			setCursor(new Cursor((isVertical) ? Cursor.N_RESIZE_CURSOR : Cursor.W_RESIZE_CURSOR));
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (focuser.isMouseCaptured()) {
			focuser.releaseMouse();
			setCursor(Cursor.getDefaultCursor());
			return;
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mouseMoved(MouseEvent e) {
	}
}