package spade.lib.page_util;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Mar 11, 2009
 * Time: 10:34:28 AM
 * Defines the content of an element of an HTML page.
 * This is an abstract class; subclasses need to define the main content
 * of the page element.
 */
public abstract class PageElement {
	/**
	 * An optional text (label) to be placed above the main content
	 */
	public String header = null;
	/**
	 * An optional text (label) to be placed below the main content
	 */
	public String footer = null;
	/**
	 * An optional reference to some page, to be transformed into a hyperlink
	 */
	public PageStructure pageRef = null;
}
