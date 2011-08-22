package ui;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import spade.lib.basicwin.CManager;
import spade.lib.basicwin.MenuConstructor;
import spade.lib.basicwin.Metrics;
import spade.lib.util.IconUtil;

public class MainWin extends Frame implements WindowListener, MenuConstructor {
	protected MenuBar mb = null;

	public MainWin() {
		super("V-Analytics - Geospatial Visual Analytics");
		CManager.setMainFrame(this);
		setIconImage(IconUtil.loadImage(this.getClass(), "/icons/commongis_icon.gif", 1000));
		setLayout(new BorderLayout());
		setSize(1000, 700);
		setLocation(10, 10);
		show();
		Metrics.setFontMetrics(getGraphics());
		addWindowListener(this);
		mb = new MenuBar();
		this.setMenuBar(mb);
	}

	/**
	* Replies if simple items (not menus) are allowed in the topmost menu (menu
	* bar). In this case simple items are not allowed.
	*/
	@Override
	public boolean allowSimpleItems() {
		return false;
	}

	/**
	* Replies if shortcuts are allowed. Since a standard menu bar is used,
	* shortcuts are allowed in this case.
	*/
	@Override
	public boolean allowShortcuts() {
		return true;
	}

	/**
	* Returns the number of items in the menu
	*/
	@Override
	public int getMenuItemCount() {
		return mb.getMenuCount();
	}

	/**
	* If the item with the given index is a menu, returns this item.
	* If this is MyMenuItem, returns its popup menu (if exists).
	* May return null;
	*/
	@Override
	public Menu getMenu(int idx) {
		return mb.getMenu(idx);
	}

	/**
	* Adds a new menu item with the given label, command (optional), and
	* ActionListener. Returns the index of this item.
	* The argument hasSubMenu indicates whether this item must be a simple
	* item or a menu.
	*/
	@Override
	public int addMenuItem(String label, String command, ActionListener list, boolean hasSubMenu) {
		Menu menu = new Menu(label);
		mb.add(menu);
		if (list != null) {
			menu.addActionListener(list);
		}
		return mb.getMenuCount() - 1;
	}

	/**
	* Removes the menu item with the given index
	*/
	@Override
	public void removeMenuItem(int idx) {
		mb.remove(idx);
	}

	@Override
	public void windowClosing(WindowEvent e) {
		if (e.getSource() == this) {
			dispose();
			return;
		}
	}

	@Override
	public void windowOpened(WindowEvent e) {
	}

	@Override
	public void windowClosed(WindowEvent e) {
	}

	@Override
	public void windowIconified(WindowEvent e) {
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
	}

	@Override
	public void windowActivated(WindowEvent e) {
		CManager.setMainFrame(this);
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
	}

	@Override
	public void dispose() {
		super.dispose();
		CManager.destroyComponent(this);
		if (CManager.getAnyFrame().equals(this)) {
			CManager.setMainFrame(null);
		}
	}

}
