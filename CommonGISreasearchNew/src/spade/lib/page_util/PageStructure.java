package spade.lib.page_util;

import java.util.Vector;

import spade.analysis.system.Supervisor;
import ui.ImagePrinter;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Mar 11, 2009
 * Time: 10:34:43 AM
 * Defines the structure of an HTML page
 */
public class PageStructure {
	/**
	 * Possible types of page layout
	 */
	public static final int LAYOUT_COLUMN = 0, // the elements are put one below another
			LAYOUT_2_COLUMNS = 1, // upper left - lower left - (upper) right - (lower right)
			LAYOUT_TABLE = 2; // the elements are organised in a table; the number of table columns must be specified
	/**
	 * file name, more precisely, its individual part
	 */
	public String fname = null;
	/**
	 * The title of the page
	 */
	public String title = null;
	/**
	 * An optional text to be placed below the title
	 */
	public String header = null;
	/**
	 * An optional text to be placed at the bottom of the page, below
	 * all other elements
	 */
	public String footer = null;
	/**
	 * The remaining elements of the page content
	 */
	public Vector<PageElement> elements = null;
	/**
	 * The type of the layout to be used for organising the elements
	 */
	public int layout = LAYOUT_2_COLUMNS;
	/**
	 * If the required layout is LAYOUT_TABLE, this is the number of table columns
	 */
	public int nColumns = 2;

	/**
	 * Pages may be organised in a sequence. If so, these are references to the
	 * previous and next pages in the sequence, to be transformed into hyperlinks.
	 */
	//public PageStructure prevPage=null, nextPage=null;
	/**
	 * The texts to be used for making hyperlinks to the previous and next pages
	 */
	//public String prevPageLinkText=null, nextPageLinkText=null;

	public void addElement(PageElement element) {
		if (element == null)
			return;
		if (elements == null) {
			elements = new Vector<PageElement>(20, 20);
		}
		elements.addElement(element);
	}

	public int getElementCount() {
		if (elements == null)
			return 0;
		return elements.size();
	}

	public PageElement getElement(int idx) {
		if (idx < 0 || elements == null || idx >= elements.size())
			return null;
		return elements.elementAt(idx);
	}

	private void addImagesToVectors(PageElementMultiple pem, Vector vImages, Vector vFnames) {
		for (int i = 0; i < pem.getItemCount(); i++)
			if (pem.getItem(i) instanceof PageElementImage && ((PageElementImage) pem.getItem(i)).image != null) {
				PageElementImage pei = (PageElementImage) pem.getItem(i);
				vImages.addElement(pei.image);
				vFnames.addElement(pei.fname);
				pei.width = pei.image.getWidth(null);
				pei.image = null;
			} else if (pem.getItem(i) instanceof PageElementMultiple) {
				addImagesToVectors((PageElementMultiple) pem.getItem(i), vImages, vFnames);
			}
	}

	public void saveAllImages(Supervisor supervisor, String fnPrefix) {
		// create all needed images, if any
		ImagePrinter impr = new ImagePrinter(supervisor);
		Vector vImages = new Vector(10, 10), vFnames = new Vector(10, 10);
		for (int i = 0; i < getElementCount(); i++)
			if (getElement(i) instanceof PageElementImage && ((PageElementImage) getElement(i)).image != null) {
				PageElementImage pei = (PageElementImage) getElement(i);
				vImages.addElement(pei.image);
				vFnames.addElement(pei.fname);
				pei.width = pei.image.getWidth(null);
				pei.image = null;
			} else if (getElement(i) instanceof PageElementMultiple) {
				addImagesToVectors((PageElementMultiple) getElement(i), vImages, vFnames);
			}
		if (vImages.size() > 0) {
			impr.setImages(vImages, vFnames);
			impr.saveImages(false, "png", fnPrefix + "_");
		}
	}
}
