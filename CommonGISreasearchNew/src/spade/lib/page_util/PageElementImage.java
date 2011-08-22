package spade.lib.page_util;

import java.awt.Image;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Mar 11, 2009
 * Time: 11:08:19 AM
 * The content of this type of page element is an image.
 */
public class PageElementImage extends PageElement {
	/**
	 * image should be set to null after saving it
	 */
	public Image image = null;
	/**
	 * if image is saved, its width should be set, if image is set to null
	 */
	public int width = 0;
	/**
	 * file name for storing the image
	 */
	public String fname = null;
	/**
	 * Geographic reference of the image (when makes sense)
	 */
	public float x1 = Float.NaN, y1 = Float.NaN, x2 = Float.NaN, y2 = Float.NaN;
}
