package spade.lib.basicwin;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Vector;

public class Border extends Canvas implements MouseListener, MouseMotionListener {
	public static int mm = Math.round(java.awt.Toolkit.getDefaultToolkit().getScreenResolution() / 25.33f);
	final static int tHeight = Math.round(0.9f * mm), tWidth = Math.round(2.0f * mm);
	public static int maxW = tHeight + 4;
	public static final int HOR = 0, VERT = 1;

	protected Cursor oldCursor = null;

	int dir = HOR;
	Vector drlist = null;

	/**
	* buttons to move the border:
	* left triangle: to left or to top
	* right triangle: to right or to bottom
	* (depending on <dir> value
	*/
	boolean hasTButtonLeft = true, hasTButtonRight = true, hasTButtonSwap = true;

	public boolean getHasTButtonLeft() {
		return hasTButtonLeft;
	}

	public boolean getHasTButtonRight() {
		return hasTButtonRight;
	}

	public boolean getHasTButtonSwap() {
		return hasTButtonSwap;
	}

	public void setHasTButtonLeft(boolean hasTButtonLeft) {
		this.hasTButtonLeft = hasTButtonLeft;
	}

	public void setHasTButtonRight(boolean hasTButtonRight) {
		this.hasTButtonRight = hasTButtonRight;
	}

	public void setHasTButtonSwap(boolean hasTButtonSwap) {
		this.hasTButtonSwap = hasTButtonSwap;
		if (isVisible()) {
			repaint();
		}
	}

	TriangleDrawer tdl = null, tdr = null;

	public Border(int direction) {
		super();
		tdl = new TriangleDrawer();
		tdl.setColor(Color.black);
		tdr = new TriangleDrawer();
		tdr.setColor(Color.black);
		if (direction == VERT) {
			dir = VERT;
			//setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
			tdl.setPreferredSize(tHeight, tWidth);
			tdr.setPreferredSize(tHeight, tWidth);
			tdl.setDirection(TriangleDrawer.W);
			tdr.setDirection(TriangleDrawer.E);
		} else {
			dir = HOR;
			//setCursor(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
			tdl.setPreferredSize(tWidth, tHeight);
			tdr.setPreferredSize(tWidth, tHeight);
			tdl.setDirection(TriangleDrawer.N);
			tdr.setDirection(TriangleDrawer.S);
		}
		addMouseListener(this);
		addMouseMotionListener(this);
	}

	public void addSplitListener(SplitListener mdl) {
		if (drlist == null) {
			drlist = new Vector();
		}
		if (drlist.indexOf(mdl) < 0) {
			drlist.addElement(mdl);
		}
	}

	public void dragMouse(int x, int y) {
		if (drlist != null && drlist.size() > 0) {
			for (int i = 0; i < drlist.size(); i++) {
				((SplitListener) drlist.elementAt(i)).mouseDragged(this, x, y);
			}
		}
	}

	public void stopDrag() {
		if (drlist != null && drlist.size() > 0) {
			for (int i = 0; i < drlist.size(); i++) {
				((SplitListener) drlist.elementAt(i)).stopDrag();
			}
		}
	}

	public void notifyBulletClicked() {

		if (drlist != null && drlist.size() > 0) {
			for (int i = 0; i < drlist.size(); i++) {
				((SplitListener) drlist.elementAt(i)).bulletClicked(this);
			}
		}
	}

	public void notifyArrowClicked(int where) {
		/*
		System.out.println("arrowClicked: hasTButtonLeft="+hasTButtonLeft+
		  ", hasTButtonRight="+hasTButtonRight+", where="+where);
		*/
		if (drlist != null && drlist.size() > 0) {
			for (int i = 0; i < drlist.size(); i++) {
				((SplitListener) drlist.elementAt(i)).arrowClicked(this, where);
			}
		}
	}

	@Override
	public void paint(Graphics g) {
		Dimension d = getSize();
		int w = d.width, h = d.height;
		if (dir == HOR && h > maxW) {
			h = maxW;
		} else if (dir == VERT && w > maxW) {
			w = maxW;
		}
		g.setColor(Color.lightGray);
		g.fillRect(0, 0, w, h);
		int x = (d.width - w) / 2, y = (d.height - h) / 2;
		g.setColor(Color.white);
		if (dir == HOR) {
			g.drawLine(x, y, x + w, y);
			y += h - 1;
		} else {
			g.drawLine(x, y, x, y + h);
			x += w - 1;
		}
		g.setColor(Color.darkGray);
		if (dir == HOR) {
			g.drawLine(x, y, x + w, y);
		} else {
			g.drawLine(x, y, x, y + h);
		}
		if (dir == HOR) {
			if (this.hasTButtonLeft) {
				tdl.draw(g, 1 * tWidth, 2);
			}
			if (this.hasTButtonRight) {
				tdr.draw(g, 3 * tWidth, 2);
			}
			if (hasTButtonLeft && hasTButtonRight && hasTButtonSwap) {
				int y_coord[] = { 0, h / 2 - 1, h - 2, h / 2 - 1 };
				int x_coord[] = { 5 * tWidth + h / 2, 5 * tWidth, 5 * tWidth + h / 2, 5 * tWidth + h };
				g.fillPolygon(x_coord, y_coord, 4);
				g.drawPolygon(x_coord, y_coord, 4);
				g.setColor(Color.lightGray);
				//g.setColor(getBackground());
				g.drawLine(x_coord[1], y_coord[1], x_coord[3], y_coord[3]);
				//g.fillRect(5*tWidth,1,h-2,h-2);
			}
		} else {
			if (this.hasTButtonLeft) {
				tdl.draw(g, 2, 1 * tWidth);
			}
			if (this.hasTButtonRight) {
				tdr.draw(g, 2, 3 * tWidth);
			}
			if (hasTButtonLeft && hasTButtonRight && hasTButtonSwap) {
				int x_coord[] = { 0, w / 2 - 1, w - 2, w / 2 - 1 };
				int y_coord[] = { 5 * tWidth + w / 2, 5 * tWidth, 5 * tWidth + w / 2, 5 * tWidth + w };
				g.fillPolygon(x_coord, y_coord, 4);
				g.drawPolygon(x_coord, y_coord, 4);
				g.setColor(Color.lightGray);
				//g.setColor(getBackground());
				g.drawLine(x_coord[1], y_coord[1], x_coord[3], y_coord[3]);
				//g.fillRect(1,5*tWidth,w-2,w-2);
			}
		}
	}

	@Override
	public Dimension getPreferredSize() {
		/*
		Dimension d=getSize();
		Component p=this;
		while ((d.height<=0 || d.width<=0) && p!=null) {
		  p=p.getParent();
		  if (p!=null) d=p.getSize();
		}
		if (dir==HOR) {
		  d.height=maxW;
		  if (d.width<=0) d.width=200;
		}
		else {
		  d.width=maxW;
		  if (d.height<=0) d.height=200;
		}
		return d;
		*/
		if (dir == HOR)
			return new Dimension(20, maxW);
		else
			return new Dimension(maxW, 20);
	}

	@Override
	public Dimension getMinimumSize() {
		Dimension d = getSize();
		Component p = this;
		while ((d.height <= 0 || d.width <= 0) && p != null) {
			p = p.getParent();
			if (p != null) {
				d = p.getSize();
			}
		}
		if (dir == HOR) {
			d.height = maxW;
			if (d.width <= 0) {
				d.width = 20;
			}
		} else {
			d.width = maxW;
			if (d.height <= 0) {
				d.height = 20;
			}
		}
		return d;
	}

	boolean isDragging = false;

	@Override
	public void mouseDragged(MouseEvent e) {
		if (drlist == null || drlist.size() < 1)
			return;
		if (!isDragging) {
			isDragging = true;
		}
		dragMouse(e.getX(), e.getY());
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (!isDragging)
			return;
		isDragging = false;
		if (oldCursor != null) {
			setCursor(oldCursor);
			oldCursor = null;
		}
		stopDrag();
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		Dimension d = getSize();
		int w = d.width, h = d.height;
		boolean clickInBullet = hasTButtonLeft && hasTButtonRight && hasTButtonSwap && (dir == VERT ? e.getY() > 4 * tWidth && e.getY() < 7.5 * tWidth + w - 2 : e.getX() > 4 * tWidth && e.getX() < 7.5 * tWidth + h - 2);

		if (clickInBullet) {
			notifyBulletClicked();
			return;
		}
		if (dir == VERT) {
			if (hasTButtonLeft && e.getY() < 2 * tWidth) {
				notifyArrowClicked((hasTButtonRight) ? SplitListener.dragToLeft : SplitListener.dragToCenter);
			}
			if (hasTButtonRight && e.getY() >= 2 * tWidth && e.getY() < 4 * tWidth) {
				notifyArrowClicked((hasTButtonLeft) ? SplitListener.dragToRight : SplitListener.dragToCenter);
			}
		} else {
			if (hasTButtonLeft && e.getX() < 2 * tWidth) {
				notifyArrowClicked((hasTButtonRight) ? SplitListener.dragToLeft : SplitListener.dragToCenter);
			}
			if (hasTButtonRight && e.getX() >= 2 * tWidth && e.getX() < 4 * tWidth) {
				notifyArrowClicked((hasTButtonLeft) ? SplitListener.dragToRight : SplitListener.dragToCenter);
			}
		}
	}

	@Override
	public void mouseMoved(MouseEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		if (oldCursor == null) {
			oldCursor = getCursor();
		}
		if (dir == VERT) {
			setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
		} else {
			setCursor(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
		}
	}

	@Override
	public void mouseExited(MouseEvent e) {
		if (!isDragging && oldCursor != null) {
			setCursor(oldCursor);
			oldCursor = null;
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {
	}

}