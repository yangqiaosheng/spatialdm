package spade.lib.page_util;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Mar 11, 2009
 * Time: 11:11:27 AM
 * The content of this type of page element is a table (matrix) consisting of text strings.
 */
public class PageElementTable extends PageElement {
	/**
	 * 1st dimension: rows; 2nd dimension: columns
	 */
	public String texts[][] = null;

	public int getNRows() {
		return (texts == null) ? 0 : texts.length;
	}

	public int getNColumns() {
		return (texts == null || texts[0] == null) ? 0 : texts[0].length;
	}
}
