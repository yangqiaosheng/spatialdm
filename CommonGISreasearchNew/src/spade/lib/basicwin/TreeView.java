package spade.lib.basicwin;

import java.awt.Adjustable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.ItemSelectable;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Vector;

import spade.lib.util.IntArray;

class ListItem {
	String id = null, name = null, parentId = null;
	boolean hasChildren = false, isCollapsed = false;
	int level = 0;

	ListItem(String id, String name, String parentId) {
		this.id = id;
		this.name = name;
		this.parentId = parentId;
	}
}

public class TreeView extends Panel implements ActionListener, ItemListener, ItemPainter, ItemSelectable {
	protected static int itemHeight = Metrics.mm() * 5, space = 4 * Metrics.mm(), iconW = 4 * Metrics.mm(), iconH = 3 * Metrics.mm(), circleD = 2 * Metrics.mm(), marg = 2 * Metrics.mm();
	protected static Color activeBkgColor = Color.blue.darker(), activeFrgColor = Color.white;
	protected Vector items = null;
	/**
	* The array contains numbers of the items that are to be drawn (some folders
	* may be collapsed, and their children not visible). The numbers come in the
	* order they should be drawn.
	*/
	protected IntArray order = null;

	protected OwnListDraw ld = null;
	protected ScrollPane scp = null;
	protected FontMetrics fm = null;
	protected int activeIdx = 0;
	/**
	* Listeners of item and action events
	*/
	protected Vector AList = null, IList = null;

	public TreeView() {
		ld = new OwnListDraw(this);
		ld.addActionListener(this);
		ld.addItemListener(this);
		scp = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
		scp.add(ld);
		Adjustable adj = scp.getVAdjustable();
		if (adj != null) {
			adj.setUnitIncrement(itemH());
		}
		setLayout(new BorderLayout());
		add(scp, "Center");
		fm = Metrics.fmetr;
	}

	public int getItemCount() {
		if (items == null)
			return 0;
		return items.size();
	}

	protected ListItem getItem(int idx) {
		if (idx < 0 || idx >= getItemCount())
			return null;
		return (ListItem) items.elementAt(idx);
	}

	public String getItemId(int idx) {
		ListItem lit = getItem(idx);
		if (lit != null)
			return lit.id;
		return null;
	}

	public String getItemName(int idx) {
		ListItem lit = getItem(idx);
		if (lit != null)
			return lit.name;
		return null;
	}

	public int getItemIndex(String itemId) {
		if (itemId == null)
			return -1;
		for (int i = 0; i < getItemCount(); i++)
			if (itemId.equals(getItemId(i)))
				return i;
		return -1;
	}

	/**
	* Returns the index of the added item
	*/
	public int addItem(String id, String name, String parentId, boolean collapsed) {
		if (id == null)
			return -1; //null identifiers are not allowed
		int idx = getItemIndex(id);
		if (idx >= 0)
			return idx; //the identifier should be unique
		ListItem lit = new ListItem(id, name, parentId);
		lit.isCollapsed = collapsed;
		idx = getItemIndex(parentId);
		if (idx >= 0) {
			getItem(idx).hasChildren = true;
		}
		if (items == null) {
			items = new Vector(20, 10);
		}
		items.addElement(lit);
		if (isShowing()) {
			setup();
		}
		return items.size() - 1;
	}

	/**
	* Returns the index of the added item
	*/
	public int addItem(String id, String name, String parentId) {
		return addItem(id, name, parentId, false);
	}

	public void removeItem(String itemId) {
		removeItemAt(getItemIndex(itemId));
	}

	public void removeItemAt(int idx) {
		if (idx >= 0 && idx < getItemCount()) {
			String parentId = getItem(idx).parentId;
			items.removeElementAt(idx);
			if (parentId != null) { //check if the parent still has children
				boolean hasChildren = false;
				for (int i = 0; i < getItemCount() && !hasChildren; i++)
					if (parentId.equals(getItem(i).parentId)) {
						hasChildren = true;
					}
				if (!hasChildren) {
					int pidx = getItemIndex(parentId);
					if (pidx >= 0) {
						getItem(pidx).hasChildren = false;
					}
				}
			}
			if (isShowing()) {
				setup();
			}
		}
	}

	public void clear() {
		if (items != null) {
			items.removeAllElements();
		}
		if (order != null) {
			order.removeAllElements();
		}
		ld.setNItems(0);
		if (isShowing()) {
			ld.invalidate();
			invalidate();
			validate();
		}
	}

	public void expandAll() {
		boolean changed = false;
		for (int i = 0; i < getItemCount(); i++) {
			ListItem lit = getItem(i);
			if (lit.hasChildren && lit.isCollapsed) {
				lit.isCollapsed = false;
				changed = true;
			}
		}
		if (changed && isShowing()) {
			setup();
		}
	}

	public void expand(int n) {
		if (n >= 0 && n < getItemCount()) {
			ListItem lit = getItem(n);
			if (lit.hasChildren && lit.isCollapsed) {
				lit.isCollapsed = false;
				if (isShowing()) {
					setup();
				}
			}
		}
	}

	public void collapseAll() {
		boolean changed = false;
		for (int i = 0; i < getItemCount(); i++) {
			ListItem lit = getItem(i);
			if (lit.hasChildren && !lit.isCollapsed) {
				lit.isCollapsed = true;
				changed = true;
			}
		}
		if (changed && isShowing()) {
			setup();
		}
	}

	public void setup() {
		defineOrder();
		ld.setNItems((order == null) ? 0 : order.size());
		if (isShowing()) {
			ld.changeActive(getOrderOfItem(activeIdx));
			ld.invalidate();
			scp.invalidate();
			invalidate();
			validate();
			ld.repaint();
		}
	}

	protected void defineOrder() {
		if (getItemCount() < 1)
			return;
		if (order == null) {
			order = new IntArray(getItemCount(), 5);
		} else {
			order.removeAllElements();
		}
		addChildrenOf(null, 0);
	}

	protected boolean isChildOf(ListItem lit, String parentId) {
		if (parentId == null)
			return lit.parentId == null;
		return parentId.equals(lit.parentId);
	}

	protected void addChildrenOf(String parentId, int level) {
		for (int i = 0; i < getItemCount(); i++) {
			ListItem lit = getItem(i);
			if (isChildOf(lit, parentId)) {
				lit.level = level;
				order.addElement(i);
				if (lit.hasChildren && !lit.isCollapsed) {
					addChildrenOf(lit.id, level + 1);
				}
			}
		}
	}

	protected int getOrderOfItem(int itemN) {
		if (order == null)
			return -1;
		return order.indexOf(itemN);
	}

	public int getSelectedIndex() {
		if (order == null || order.size() < 1)
			return -1;
		int n = ld.getSelectedIndex();
		if (n < 0 || n >= order.size())
			return -1;
		activeIdx = order.elementAt(n);
		return activeIdx;
	}

	public void setSelectedIndex(int n) {
		activeIdx = n;
		ld.changeActive(getOrderOfItem(n));
	}

	@Override
	public int itemH() {
		if (fm != null && fm.getHeight() > itemHeight)
			return fm.getHeight();
		return itemHeight;
	}

	@Override
	public int maxItemW() {
		if (fm == null) {
			fm = Metrics.fmetr;
		}
		if (fm == null)
			return 100;
		if (order == null) {
			defineOrder();
		}
		if (order == null || order.size() < 1)
			return 100;
		int maxW = 0;
		for (int i = 0; i < order.size(); i++) {
			ListItem lit = getItem(order.elementAt(i));
			int w = 2 * marg + iconW + lit.level * space + fm.stringWidth(lit.name);
			if (maxW < w) {
				maxW = w;
			}
		}
		return maxW;
	}

	@Override
	public void drawItem(Graphics g, int n, int x, int y, int w, boolean isActive) {
		g.setColor((isActive) ? activeBkgColor : getBackground());
		g.fillRect(x, y, w + 1, itemH() + 1);
		if (order == null || n < 0 || n >= order.size())
			return;
		int idx = order.elementAt(n);
		ListItem lit = getItem(idx);
		g.setColor((isActive) ? activeFrgColor : getForeground());
		if (fm == null) {
			fm = g.getFontMetrics();
			Metrics.fmetr = fm;
		}
		int dx = x + lit.level * space;
		if (lit.hasChildren)
			if (lit.isCollapsed) {
				Icons.drawClosedFolder(g, dx, y, iconW + marg, itemH(), iconW, iconH);
			} else {
				Icons.drawOpenFolder(g, dx, y, iconW + marg, itemH(), iconW, iconH);
			}
		else {
			Icons.drawCircle(g, dx, y, iconW + marg, itemH(), circleD);
		}
		dx += iconW + marg;
		g.drawString(lit.name, dx, y + (itemH() - fm.getHeight()) / 2 + fm.getAscent());
	}

	@Override
	public void drawEmptyList(Graphics g, int x, int y, int w, int h) {
		g.setColor(getBackground());
		g.fillRect(x, y, w + 1, h + 1);
	}

	public Dimension getVisibleListSize() {
		return scp.getViewportSize();
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

	/**
	* Upon "action" the hierarchical list expands or collapses the corresponding
	* tree node (if it has children)
	*/
	@Override
	public void actionPerformed(ActionEvent evt) {
		if (order == null || order.size() < 1)
			return;
		if (evt.getSource() == ld) {
			int n = ld.getSelectedIndex();
			if (n < 0)
				return;
			activeIdx = order.elementAt(n);
			ListItem lit = getItem(activeIdx);
			if (!lit.hasChildren)
				return;
			lit.isCollapsed = !lit.isCollapsed;
			setup();
			sendActionEvent();
		}
	}

	protected void sendActionEvent() {
		if (AList != null && AList.size() > 0) {
			ActionEvent ae = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, getItem(activeIdx).id);
			for (int i = 0; i < AList.size(); i++) {
				((ActionListener) AList.elementAt(i)).actionPerformed(ae);
			}
		}
	}

	@Override
	public void itemStateChanged(ItemEvent evt) {
		if (order == null || order.size() < 1)
			return;
		if (evt.getSource() == ld) {
			int n = ld.getSelectedIndex();
			if (n < 0) {
				activeIdx = -1;
			} else {
				activeIdx = order.elementAt(n);
			}
			if (IList != null && IList.size() > 0) {
				ItemEvent e = new ItemEvent(this, ItemEvent.ITEM_STATE_CHANGED, getItem(activeIdx).id, ItemEvent.SELECTED);
				for (int i = 0; i < IList.size(); i++) {
					((ItemListener) IList.elementAt(i)).itemStateChanged(e);
				}
			}
		}
	}

	@Override
	public Object[] getSelectedObjects() {
		if (activeIdx < 0 || activeIdx >= getItemCount())
			return null;
		Object selObj[] = new Object[1];
		selObj[0] = getItem(activeIdx);
		return selObj;
	}

}