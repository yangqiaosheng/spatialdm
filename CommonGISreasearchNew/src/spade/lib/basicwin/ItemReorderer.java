package spade.lib.basicwin;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.lib.lang.Language;

public class ItemReorderer extends Canvas implements MouseListener, MouseMotionListener {
	static ResourceBundle res = Language.getTextResource("spade.lib.basicwin.Res");
	/**
	* Listeners of changes of the order - instances of ActionListener
	*/
	protected Vector listeners = null;
	/**
	* The names of the items to reorder
	*/
	protected Vector itemNames = null;
	/**
	* The identifiers of the items to reorder
	*/
	protected Vector itemIds = null;

	protected int yy[] = null, fh = -1;
	protected int Y0 = -1, DY = 0;

	/**
	* Adds a reorderable item. Each item has its name and identifier.
	*/
	public void addItem(String itemId, String itemName) {
		if (itemName == null) {
			itemName = itemId;
		}
		if (itemIds == null) {
			itemIds = new Vector(10, 10);
			addMouseListener(this);
			addMouseMotionListener(this);
		}
		if (itemNames == null) {
			itemNames = new Vector(10, 10);
		}
		if (itemIds.contains(itemId))
			return;
		itemIds.addElement(itemId);
		itemNames.addElement(itemName);
	}

	/**
	* Removes the item with the given identifier.
	*/
	public void removeItem(String itemId) {
		int idx = itemIds.indexOf(itemId);
		if (idx >= 0) {
			itemIds.removeElementAt(idx);
			itemNames.removeElementAt(idx);
		}
	}

	/**
	* Returns the ordered list of identifiers of the items
	*/
	public Vector getItemList() {
		return itemIds;
	}

	/**
	* Adds a listener of changes of the order.
	*/
	public void addActionListener(ActionListener alist) {
		if (alist == null)
			return;
		if (listeners == null) {
			listeners = new Vector(5, 5);
		}
		if (!listeners.contains(alist)) {
			listeners.addElement(alist);
		}
	}

	/**
	* Removes the listener of changes of the order.
	*/
	public void removeActionListener(ActionListener alist) {
		if (alist == null || listeners == null)
			return;
		int idx = listeners.indexOf(alist);
		if (idx >= 0) {
			listeners.removeElementAt(idx);
		}
	}

	/**
	* Sets the vertical offset for drawing the items
	*/
	public void setY0(int y0) {
		Y0 = y0;
	}

	/**
	* Sets the height of each item
	*/
	public void setDY(int dy) {
		DY = dy;
	}

	@Override
	public Dimension getPreferredSize() {
		if (itemIds == null)
			return new Dimension(30 * Metrics.mm(), 20);
		return new Dimension(30 * Metrics.mm(), (DY > 0) ? DY * (itemIds.size() + 1) : 60 * Metrics.mm());
	}

	protected void drawText(Graphics g, int n) {
		g.drawString((String) itemNames.elementAt(n), 5, yy[n]);
	}

	@Override
	public void paint(Graphics g) {
		if (g == null)
			return;
		g.setColor(Color.white);
		Dimension d = getSize();
		g.fillRect(0, 0, d.width, d.height);
		if (itemIds == null)
			return;
		g.setColor(Color.black);
		FontMetrics fm = g.getFontMetrics();
		fh = fm.getHeight();
		int asc = fm.getAscent();
		int dy = DY;
		if (dy <= 0) {
			dy = (d.height - 3 * fh - asc) / (itemNames.size() - 1);
		}
		if (Y0 < 0) {
			Y0 = fh;
		}
		if (yy == null || yy.length != itemNames.size()) {
			yy = new int[itemNames.size()];
		}
		for (int i = 0; i < itemNames.size(); i++) {
			yy[i] = Y0 + dy * i + asc;
			drawText(g, i);
			//g.drawString(dataTable.getAttributeName(fn[i]),5,yy[i]);
		}
		g.setColor(Color.magenta);
		g.drawString(res.getString("drag_to_reorder"), 3, d.height - fh + asc);
	}

	protected void drawArrow(int x1, int y1, int x2, int y2, boolean OK) {
		Graphics g = getGraphics();
		if (g == null)
			return;
		g.setColor((OK) ? Color.green : Color.red);
		g.setXORMode(Color.lightGray);
		g.drawLine(x1, y1, x2, y2);
		g.setPaintMode();
		g.dispose();
	}

	protected int dragX1 = -1, dragY1 = -1, dragX2 = dragX1, dragY2 = dragY1, dragged = -1, draggedTo = -1;
	protected boolean dragging = false;

	public int getDragged() {
		return dragged;
	}

	public int getDraggedTo() {
		return draggedTo;
	}

	protected void drawTarget(boolean OK) {
		Graphics g = getGraphics();
		if (g == null)
			return;
		if (draggedTo == -1)
			return;
		int y = 0;
		if (draggedTo < dragged) {
			y = yy[draggedTo] - fh;
		}
		if (draggedTo > dragged) {
			y = yy[draggedTo] + fh / 2;
		}
		g.setColor((OK) ? Color.green : Color.red);
		g.setXORMode(Color.lightGray);
		if (OK) {
			g.drawLine(5, y, 15, y);
			g.drawLine(10, y - 5, 15, y);
			g.drawLine(10, y + 5, 15, y);
		} else {
			g.drawLine(5, y - 5, 15, y + 5);
			g.drawLine(5, y + 5, 15, y - 5);
		}
		g.setPaintMode();
		g.dispose();
	}

	protected boolean isPointInPlotArea(int x, int y) {
		Dimension d = getSize();
		if (d == null)
			return false;
		return x >= 0 && x <= d.width && y >= 0 && y <= d.height;
	}

	protected Cursor savedCursor = null;

	@Override
	public void mouseDragged(MouseEvent e) {
		int x = e.getX(), y = e.getY();
		if (!dragging) {
			if (!isPointInPlotArea(x, y))
				return;
			dragging = true;
			dragX1 = dragX2 = x;
			dragY1 = dragY2 = y;
			// finding an item to be dragged
			dragged = -1;
			int dy = (yy[1] - yy[0] - fh) / 2;
			for (int i = 0; dragged == -1 && i < yy.length; i++)
				if ((i == 0 && y <= yy[0] + dy) || (i > 0 && i < yy.length - 1 && y > yy[i - 1] + dy && y <= yy[i] + dy) || (i == yy.length - 1 && y > yy[i - 1] + dy)) {
					dragged = i;
				}
			draggedTo = -1;
			//System.out.println(" dragged="+dragged+", y="+y+", dy="+dy+", yy[dragged]="+yy[dragged]);
			Graphics g = getGraphics();
			if (g != null) {
				g.setColor(Color.red);
				drawText(g, dragged);
				g.setColor(Color.magenta);
				g.setXORMode(Color.lightGray);
				g.fillOval(x - 2, y - 2, 5, 5);
				g.setPaintMode();
				g.dispose();
			}
			savedCursor = getCursor();
			setCursor(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
			return;
		}
		// hiding old arrow
		if (x == dragX2 && y == dragY2)
			return;
		drawArrow(dragX1, dragY1, dragX2, dragY2, true);
		drawTarget(true);
		// finding the target
		draggedTo = -1;
		if (y < dragY1 && dragged > 0) { // dragging up
			for (int i = 0; i < dragged; i++)
				if (y < yy[i]) {
					draggedTo = i;
					break;
				}
		} else if (y > dragY1 && dragged < yy.length - 1) { // dragging down
			for (int i = yy.length - 1; i > dragged; i--)
				if (y > yy[i] - fh) {
					draggedTo = i;
					break;
				}
		}
		//System.out.println("* dragged="+dragged+", To="+draggedTo);
		// drawing new arrow
		dragX2 = x;
		dragY2 = y;
		drawArrow(dragX1, dragY1, dragX2, dragY2, true);
		drawTarget(true);
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (dragged >= 0 && draggedTo >= 0 && dragged != draggedTo) {
			Object id = itemIds.elementAt(dragged), name = itemNames.elementAt(dragged);
			itemIds.removeElementAt(dragged);
			itemNames.removeElementAt(dragged);
			//if (draggedTo>dragged) --draggedTo;
			itemIds.insertElementAt(id, draggedTo);
			itemNames.insertElementAt(name, draggedTo);
			if (listeners != null) {
				ActionEvent ae = new ActionEvent(this, 0, "order_changed");
				for (int i = 0; i < listeners.size(); i++) {
					ActionListener al = (ActionListener) listeners.elementAt(i);
					al.actionPerformed(ae);
				}
			}
			repaint();
		}
		dragging = false;
		dragX1 = dragY1 = dragX2 = dragY2 = dragged = -1;
		if (savedCursor != null) {
			setCursor(savedCursor);
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mouseMoved(MouseEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

}