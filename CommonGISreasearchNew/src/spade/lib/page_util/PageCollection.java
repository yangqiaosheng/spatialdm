package spade.lib.page_util;

import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Mar 11, 2009
 * Time: 11:58:58 AM
 * Defines the contents of a collection of HTML pages, which may be
 * referenced from an index page
 */
public class PageCollection {
	/**
	 * The contents of the pages. The first page must be the root page.
	 * May also be a single page.
	 */
	public Vector<PageStructure> pages = null;
	/**
	 * A text to be added to the index page for referencing to the root page
	 * of the collection. If this text is null, no reference is added to the
	 * index page. 
	 */
	public String refText = null;

	public void addPage(PageStructure page) {
		if (page == null)
			return;
		if (pages == null) {
			pages = new Vector<PageStructure>(20, 20);
		}
		pages.addElement(page);
	}

	public int getPageCount() {
		if (pages == null)
			return 0;
		return pages.size();
	}

}
