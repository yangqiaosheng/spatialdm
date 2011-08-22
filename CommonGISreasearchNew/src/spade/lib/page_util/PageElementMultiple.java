package spade.lib.page_util;

import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Mar 11, 2009
 * Time: 11:14:04 AM
 * This type of page element consists of several elements.
 * The elements are organised in a table layout with the
 * specified number of columns
 */
public class PageElementMultiple extends PageElement {
	/**
	 * The elements from which this page element consists
	 */
	public Vector<PageElement> items = null;
	/**
	 * The number of table columns
	 */
	public int nColumns = 3;

	public void addItem(PageElement item) {
		if (item == null)
			return;
		if (items == null) {
			items = new Vector<PageElement>(20, 20);
		}
		items.addElement(item);
	}

	public int getItemCount() {
		if (items == null)
			return 0;
		return items.size();
	}

	public PageElement getItem(int idx) {
		if (idx < 0 || items == null || idx >= items.size())
			return null;
		return items.elementAt(idx);
	}
}
