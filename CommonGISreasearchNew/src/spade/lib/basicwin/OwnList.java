package spade.lib.basicwin;

// no texts

import java.awt.Adjustable;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Panel;
import java.awt.Point;
import java.awt.ScrollPane;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;

public class OwnList extends Panel {
	protected ItemPainter painter = null;
	protected OwnListDraw ld = null;
	protected ScrollPane scp = null;

	public OwnList(ItemPainter ip) {
		super();
		initialize(ip);
	}

	public OwnList() {
		super();
	}

	public void addActionListener(ActionListener actL) {
		ld.addActionListener(actL);
	}

	public void addItemListener(ItemListener itemL) {
		ld.addItemListener(itemL);
	}

	public OwnListDraw getListDrawCanvas() {
		return ld;
	}

	@Override
	public void repaint() {
		if (ld != null) {
			ld.repaint();
		}
	}

	public void initialize(ItemPainter ip) {
		ld = new OwnListDraw(ip);
		scp = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
		scp.add(ld);
		painter = ip;
		Adjustable adj = scp.getVAdjustable();
		if (adj != null) {
			adj.setUnitIncrement(painter.itemH());
		}
		setLayout(new BorderLayout());
		add(scp, "Center");
	}

	public int countItems() {
		return ld.countItems();
	}

	protected void adjustScrollPos() {
		//scroll to active, if needed
		int y = ld.getItemPos(ld.getSelectedIndex());
		Point p = scp.getScrollPosition();
		if (y < p.y) {
			scp.setScrollPosition(0, y);
			return;
		}
		int h = ld.getItemHeight();
		Dimension d = getVisibleListSize();
		if (y + h > p.y + d.height) {
			scp.setScrollPosition(0, y + h - d.height);
		}
	}

	public void setSelected(int n) {
		ld.changeActive(n);
		adjustScrollPos();
	}

	public synchronized int getSelectedIndex() {
		return ld.getSelectedIndex();
	}

	public synchronized void delItem(int position) {
		if (ld.countItems() > position) {
			ld.delItem(position);
			ld.invalidate();
			validate();
			ld.repaint();
		}
	}

	public synchronized void clear() {
		if (ld.countItems() > 0) {
			ld.clear();
			ld.invalidate();
			validate();
			ld.repaint();
		}
	}

	public synchronized void addItem(String item) {
		ld.addItem(item);
		ld.invalidate();
		validate();
		ld.repaint();
	}

	public synchronized void addItem(String item, int index) {
		ld.addItem(item, index);
		ld.invalidate();
		validate();
		ld.repaint();
	}

	public synchronized void setNItems(int N) {
		ld.setNItems(N);
		ld.invalidate();
		validate();
		ld.repaint();
	}

	public void repaintItem(int itemN) {
		ld.repaintItem(itemN);
	}

	public Dimension getVisibleListSize() {
		return scp.getViewportSize();
	}

	@Override
	public Dimension getPreferredSize() {
		Dimension size = ld.getPreferredSize();
		if (ld.countItems() > 10) {
			size.height = 10 * painter.itemH() + 4;
		}
		size.width += scp.getVScrollbarWidth() + 5;
		return size;
	}

}
