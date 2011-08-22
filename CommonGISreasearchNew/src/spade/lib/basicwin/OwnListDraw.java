package spade.lib.basicwin;

// no texts

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.ItemSelectable;
import java.awt.Point;
import java.awt.ScrollPane;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Vector;

public class OwnListDraw extends Canvas implements MouseListener, ItemListener, ItemSelectable {
	ItemPainter painter = null;
	int active = 0, nitems = 0;
	Vector AList = null, IList = null;

	public OwnListDraw(ItemPainter ip) {
		super();
		painter = ip;
		addMouseListener(this);
	}

	public void addActionListener(ActionListener actL) {
		if (actL == null)
			return;
		if (AList == null) {
			AList = new Vector(5, 5);
		}
		AList.addElement(actL);
	}

	public void removeActionListener(ActionListener actL) {
		if (actL != null && AList != null) {
			AList.removeElement(actL);
		}
	}

	@Override
	public void addItemListener(ItemListener itemL) {
		if (itemL == null)
			return;
		if (IList == null) {
			IList = new Vector(5, 5);
		}
		IList.addElement(itemL);
	}

	@Override
	public void removeItemListener(ItemListener itemL) {
		if (itemL != null && IList != null) {
			IList.removeElement(itemL);
		}
	}

	@Override
	public Object[] getSelectedObjects() {
		return null;
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (IList != null && IList.size() > 0) {
			for (int i = 0; i < IList.size(); i++) {
				((ItemListener) IList.elementAt(i)).itemStateChanged(e);
			}
		}
	}

	@Override
	public Dimension getPreferredSize() {
		int w = painter.maxItemW();
		if (w <= 0) {
			w = getSize().width;
		}
		if (w <= 0) {
			w = 100;
		}
		int n = nitems;
		if (n < 3) {
			n = 3;
		}
		return new Dimension(w, painter.itemH() * n + 4);
	}

	@Override
	public void paint(Graphics g) {
		if (g == null || painter == null)
			return;
		if (painter.maxItemW() <= 0) { //needs to redefine sizes and validate the container
			CManager.validateAll(this);
			return;
		}
		Dimension d = getSize();
		//draw the frame
		g.setColor(Color.darkGray);
		g.drawLine(0, 0, d.width, 0);
		g.drawLine(0, 0, 0, d.height);
		g.setColor(Color.black);
		g.drawLine(1, 1, d.width, 1);
		g.drawLine(1, 1, 1, d.height);
		g.setColor(Color.lightGray);
		g.drawLine(0, d.height, d.width, d.height);
		g.drawLine(d.width, 0, d.width, d.height);
		g.setColor(Color.white);
		g.drawLine(1, d.height - 1, d.width - 1, d.height - 1);
		g.drawLine(d.width - 1, 2, d.width - 1, d.height - 1);

		if (nitems <= 0) {
			painter.drawEmptyList(g, 2, 2, d.width - 4, d.height - 4);
			return;
		}

		//draw the items
		int h = painter.itemH(), y = 0;
		for (int i = 0; i < nitems; i++) {
			painter.drawItem(g, i, 0, y, d.width, i == active);
			y += h;
		}
	}

	public void changeActive(int itemN) {
		if (itemN != active && itemN >= 0 && itemN < nitems) {
			Graphics g = getGraphics();
			if (g != null) {
				if (active >= 0) {
					painter.drawItem(g, active, 0, getItemPos(active), getSize().width, false);
				}
				painter.drawItem(g, itemN, 0, getItemPos(itemN), getSize().width, true);
				g.dispose();
			}
			active = itemN;
			itemStateChanged(new ItemEvent(this, active, null, 1));
		}
	}

	public void repaintItem(int itemN) {
		if (itemN >= 0 && itemN < nitems) {
			Graphics g = getGraphics();
			if (g == null)
				return;
			painter.drawItem(g, itemN, 0, getItemPos(itemN), getSize().width, itemN == active);
			g.dispose();
		}
	}

	public int getItemPos(int itemN) {
		return itemN * painter.itemH();
	}

	public int getItemHeight() {
		return painter.itemH();
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
		int x = e.getX(), y = e.getY();
		int h = painter.itemH();
		int itemN = y / h;
		changeActive(itemN);
		if (e.getClickCount() == 2 && AList != null && AList.size() > 0) {
			ActionEvent ae = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, String.valueOf(active));
			for (int i = 0; i < AList.size(); i++) {
				((ActionListener) AList.elementAt(i)).actionPerformed(ae);
			}
		}
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

	public boolean next(int y0, int H) { //returns true if scroll is needed
		//y0 is the current scroller position, H is the viewport height
		changeActive(active + 1);
		int y = getItemPos(active);
		return y < y0 || y + painter.itemH() > y0 + H;
	}

	public boolean previous(int y0, int H) { //returns true if scroll is needed
		//y0 is the current scroller position, H is the viewport height
		changeActive(active - 1);
		int y = getItemPos(active);
		return y < y0 || y + painter.itemH() > y0 + H;
	}

	public int countItems() {
		return nitems;
	}

	public synchronized int getSelectedIndex() {
		return active;
	}

	public synchronized void delItem(int position) {
		if (nitems > position) {
			--nitems;
		}
		if (active > position) {
			--active;
		} else if (active == position) {
			active = 0;
		}
	}

	public synchronized void clear() {
		nitems = 0;
		active = 0;
	}

	public synchronized void addItem(String item) {
		++nitems;
	}

	public synchronized void addItem(String item, int index) {
		++nitems;
	}

	public synchronized void setNItems(int N) {
		nitems = N;
		active = 0;
	}

	/**
	* If the component is inserted in a ScrollPane, makes this ScrollPane scroll
	* so that the specified element is visible
	*/
	public void makeVisible(int idx) {
		if (getParent() == null || !(getParent() instanceof ScrollPane))
			return;
		ScrollPane scp = (ScrollPane) getParent();
		if (idx < 0) {
			idx = 0;
		}
		if (idx >= nitems) {
			idx = nitems - 1;
		}
		int y = getItemPos(idx);
		Point p = scp.getScrollPosition();
		if (y < p.y) {
			scp.setScrollPosition(0, y);
		} else {
			Dimension d = scp.getViewportSize();
			if (y + painter.itemH() > p.y + d.height) {
				scp.setScrollPosition(0, y + painter.itemH() - d.height);
			}
		}
		if (isShowing() && scp.getScrollPosition().y != p.y) {
			paint(getGraphics());
		}
	}

}
