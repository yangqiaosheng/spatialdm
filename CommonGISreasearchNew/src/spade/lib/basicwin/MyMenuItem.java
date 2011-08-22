package spade.lib.basicwin;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Menu;
import java.awt.MenuComponent;
import java.awt.PopupMenu;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

/**
* The class implements the behaviour of a menu item. This is needed in order to
* be able to put a menu anywhere, not only in a frame.
*/
public class MyMenuItem extends Canvas {
	public static Color defBgColor = Color.lightGray, defFgColor = Color.black, activeBgColor = Color.blue.darker().darker(), activeFgColor = Color.white;
	/**
	* Action listeners of the menu item
	*/
	protected Vector listeners = null;
	/**
	* Action command
	*/
	protected String command = null;
	/**
	* Indicates "active" (highlighted) state. If the item has a submenu, it opens
	* this submenu in the active state.
	*/
	protected boolean active = false;
	/**
	* The text of the menu item
	*/
	protected String text = null;
	/**
	* The popup menu (optional) that may appear in the active state
	*/
	protected PopupMenu pmenu = null;

	public MyMenuItem() {
		//this.setAlignment(CENTER);
		setFont(new Font("Dialog", 0, 11));
	}

	public MyMenuItem(String label) {
		this();
		setLabel(label);
	}

	public void setText(String label) {
		text = label;
		if (isShowing()) {
			repaint();
		}
	}

	public String getText() {
		return text;
	}

	@Override
	public Dimension getPreferredSize() {
		if (text == null)
			return new Dimension(10, 10);
		FontMetrics fm = getFontMetrics(getFont());
		if (fm == null)
			return new Dimension(50, 10);
		return new Dimension(fm.stringWidth(text) + 10, fm.getHeight() + 4);
	}

	@Override
	public void paint(Graphics g) {
		Dimension d = getSize();
		g.setColor((active) ? activeBgColor : defBgColor);
		g.fillRect(0, 0, d.width + 1, d.height + 1);
		g.setColor((active) ? activeFgColor : defFgColor);
		FontMetrics fm = g.getFontMetrics();
		int w = fm.stringWidth(text), h = fm.getHeight(), asc = fm.getAscent();
		g.drawString(text, (d.width - w) / 2, (d.height - h) / 2 + asc);
	}

	@Override
	public void add(PopupMenu menu) {
		super.add(menu);
		pmenu = menu;
		if (pmenu != null) {
			if (pmenu.getLabel() != null && pmenu.getLabel().length() > 0) {
				setLabel(pmenu.getLabel());
			}
			if (listeners != null) {
				for (int i = 0; i < listeners.size(); i++) {
					pmenu.addActionListener((ActionListener) listeners.elementAt(i));
				}
			}
		}
	}

	@Override
	public void remove(MenuComponent menu) {
		super.remove(menu);
		pmenu = null;
	}

	public String getActionCommand() {
		return command;
	}

	public String getLabel() {
		return getText();
	}

	public Menu getMenu() {
		return pmenu;
	}

	public boolean isActive() {
		return active;
	}

	public void setActionCommand(String cmd) {
		command = cmd;
	}

	public void setActive(boolean value) {
		if (active != value) {
			active = value;
			setAppearance();
		}
	}

	public void setLabel(String label) {
		setText(label);
		if (command == null) {
			command = label;
		}
	}

	public void addActionListener(ActionListener alist) {
		if (alist == null)
			return;
		if (listeners == null) {
			listeners = new Vector(1, 1);
		}
		if (listeners.contains(alist))
			return;
		listeners.addElement(alist);
		if (pmenu != null) {
			pmenu.addActionListener(alist);
		}
	}

	public void removeActionlistener(ActionListener alist) {
		if (alist == null || listeners == null)
			return;
		int idx = listeners.indexOf(alist);
		if (idx >= 0) {
			listeners.removeElementAt(idx);
		}
		if (pmenu != null) {
			pmenu.removeActionListener(alist);
		}
	}

	protected void sendCommandToListeners() {
		if (command == null || pmenu != null || listeners == null || listeners.size() < 1)
			return;
		ActionEvent ae = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, command);
		for (int i = 0; i < listeners.size(); i++) {
			((ActionListener) listeners.elementAt(i)).actionPerformed(ae);
		}
	}

	/**
	* Sets its appearance to "highlighted" (white text on blue background) or
	* default (black text on light gray background), depending on the value of
	* the variable highlighted.
	*/
	protected void setAppearance() {
		if (!isShowing())
			return;
		Graphics g = getGraphics();
		paint(g);
		g.dispose();
		if (active && pmenu != null) {
			pmenu.show(this, 0, getSize().height);
		}
	}
}
