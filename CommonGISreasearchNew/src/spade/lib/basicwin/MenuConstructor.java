package spade.lib.basicwin;

import java.awt.Menu;
import java.awt.event.ActionListener;

/**
* This interface allows a component to construct a menu bar and react to its
* commands irrespective of whether this is a "normal" menu bar attached to a
* frame or MyMenuBar (i.e. a panel that may be included anywhere).
*/
public interface MenuConstructor {
	/**
	* Replies if simple items (not menus) are allowed in the topmost menu (menu
	* bar)
	*/
	public boolean allowSimpleItems();

	/**
	* Replies if shortcuts are allowed
	*/
	public boolean allowShortcuts();

	/**
	* Returns the number of items in the menu
	*/
	public int getMenuItemCount();

	/**
	* If the item with the given index is a menu, returns this item.
	* If this is MyMenuItem, returns its popup menu (if exists).
	* May return null; 
	*/
	public Menu getMenu(int idx);

	/**
	* Adds a new menu item with the given label, command (optional), and
	* ActionListener. Returns the index of this item.
	* The argument hasSubMenu indicates whether this item must be a simple
	* item or a menu.
	*/
	public int addMenuItem(String label, String command, ActionListener list, boolean hasSubMenu);

	/**
	* Removes the menu item with the given index
	*/
	public void removeMenuItem(int idx);
}