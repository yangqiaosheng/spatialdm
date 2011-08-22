package spade.lib.basicwin;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Panel;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Vector;

/**
* Implements the behaviour of a menu bar. Unlike a standard MenuBar, MyMenuBar
* can be put anywhere, not only in a frame
*/
public class MyMenuBar extends Panel implements MouseListener, MouseMotionListener {
	/**
	* The menu items
	*/
	protected Vector items = null;
	/**
	* The index of the currently active item
	*/
	protected int activeIdx = -1;

	public MyMenuBar() {
		setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		setBackground(MyMenuItem.defBgColor);
		addMouseListener(this);
		addMouseMotionListener(this);
	}

	@Override
	public Dimension getPreferredSize() {
		if (items == null || items.size() < 1)
			return super.getPreferredSize();
		Dimension psize = null;
		if (getParent() != null) {
			psize = getParent().getSize();
		}
		if (psize == null || psize.width <= 0)
			return super.getPreferredSize();
		Dimension d = new Dimension(0, 0);
		int w = 0, h = 0;
		for (int i = 0; i < items.size(); i++) {
			Dimension dc = ((Component) items.elementAt(i)).getPreferredSize();
			if (dc == null) {
				continue;
			}
			if (h <= 0) {
				h = dc.height;
			}
			if (w > 0 && w + dc.width > psize.width) {
				if (d.width < w) {
					d.width = w;
				}
				d.height += h;
				w = 0;
			}
			w += dc.width;
		}
		if (d.width < w) {
			d.width = w;
		}
		d.height += h;
		return d;
	}

	public void addItem(MyMenuItem mit) {
		if (mit == null)
			return;
		if (items == null) {
			items = new Vector(10, 5);
		}
		items.addElement(mit);
		super.add(mit);
		mit.addMouseListener(this);
		mit.addMouseMotionListener(this);
	}

	public int getItemCount() {
		if (items == null)
			return 0;
		return items.size();
	}

	public MyMenuItem getItem(int i) {
		if (i < 0 || i >= getItemCount())
			return null;
		return (MyMenuItem) items.elementAt(i);
	}

	@Override
	public void remove(int idx) {
		if (idx < 0 || idx >= getItemCount())
			return;
		items.removeElementAt(idx);
		super.remove(idx);
	}

	public void remove(MyMenuItem mit) {
		if (mit == null || items == null)
			return;
		int idx = items.indexOf(mit);
		if (idx < 0)
			return;
		items.removeElementAt(idx);
		super.remove(idx);
	}

	/**
	* Activates (highlights) or deactivates (dehighlights) the item with the given
	* index, depending on the value of the argument active
	*/
	protected void setItemState(int idx, boolean active) {
		if (idx < 0 || idx >= getItemCount())
			return;
		if (active && activeIdx >= 0 && activeIdx != idx) {
			getItem(activeIdx).setActive(false);
		}
		getItem(idx).setActive(active);
		if (active) {
			activeIdx = idx;
		} else if (activeIdx == idx) {
			activeIdx = -1;
		}
	}

	/**
	* Invoked when the mouse has been clicked on a component.
	*/
	@Override
	public void mouseClicked(MouseEvent e) {
		/*
		if (e.getSource() instanceof MyMenuItem) {
		  MyMenuItem mit=(MyMenuItem)e.getSource();
		  if (mit.isEnabled()) {
		    int idx=items.indexOf(mit);
		    setItemState(idx,false);
		    mit.sendCommandToListeners();
		  }
		}
		else
		  setItemState(activeIdx,false);
		*/
	}

	/**
	* Invoked when a mouse button has been pressed on a component.
	*/
	@Override
	public void mousePressed(MouseEvent e) {
		//System.out.println(e.toString());
		if (e.getSource() instanceof MyMenuItem) {
			MyMenuItem mit = (MyMenuItem) e.getSource();
			int idx = items.indexOf(mit);
			if (idx != activeIdx)
				if (mit.isEnabled()) {
					setItemState(idx, true);
				} else {
					setItemState(activeIdx, false);
				}
		} else {
			setItemState(activeIdx, false);
		}
	}

	/**
	* Invoked when a mouse button has been released on a component.
	*/
	@Override
	public void mouseReleased(MouseEvent e) {
		//setItemState(activeIdx,false);
		if (e.getSource() instanceof MyMenuItem) {
			MyMenuItem mit = (MyMenuItem) e.getSource();
			if (mit.isEnabled()) {
				int idx = items.indexOf(mit);
				setItemState(idx, false);
				mit.sendCommandToListeners();
			}
		} else {
			setItemState(activeIdx, false);
		}
	}

	/**
	* Invoked when the mouse enters a component.
	*/
	@Override
	public void mouseEntered(MouseEvent e) {
		if (e.getSource() instanceof MyMenuItem) {
			boolean buttonPressed = (e.getModifiers() & InputEvent.BUTTON1_MASK) == InputEvent.BUTTON1_MASK;
			if (!buttonPressed)
				return;
			MyMenuItem mit = (MyMenuItem) e.getSource();
			if (mit.isEnabled()) {
				int idx = items.indexOf(mit);
				setItemState(idx, true);
			}
		}
	}

	/**
	* Invoked when the mouse exits a component.
	*/
	@Override
	public void mouseExited(MouseEvent e) {
		setItemState(activeIdx, false);
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		int x = e.getX(), y = e.getY();
		if (e.getSource() instanceof MyMenuItem) {
			MyMenuItem mit = (MyMenuItem) e.getSource();
			Point p = mit.getLocation();
			x += p.x;
			y += p.y;
		}
		Component c = this.getComponentAt(x, y);
		if (c != null && (c instanceof MyMenuItem)) {
			MyMenuItem mit = (MyMenuItem) c;
			int idx = items.indexOf(mit);
			if (idx != activeIdx)
				if (mit.isEnabled()) {
					setItemState(idx, true);
				} else {
					setItemState(activeIdx, false);
				}
		} else {
			setItemState(activeIdx, false);
		}
	}

	@Override
	public void mouseMoved(MouseEvent e) {
	}

}
