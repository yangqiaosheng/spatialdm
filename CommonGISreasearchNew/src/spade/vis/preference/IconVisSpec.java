package spade.vis.preference;

import java.awt.Color;
import java.awt.Component;
import java.awt.Image;
import java.awt.MediaTracker;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;

import spade.lib.util.IdUtil;
import spade.vis.geometry.GeomSign;

/**
* Specifies an icon to be used for presenting on a map geographical objects
* depending on values of one or more qualitative attributes associated with
* these objects. Sets a correspondence between a value of a single attribute
* or a combination of values of several attributes and an icon (image).
*/
public class IconVisSpec {
	/**
	* This Vector consists of 2-element arrays where the 1st element is an
	* attribute identifier and the 2nd is one of the values of this attribute.
	* The attributes in the vector must not repeat.
	* Stores "pure" rather than "extended" attribute identifiers, i.e. attribute
	* identifiers without table identifiers attached.
	* If the vector is empty, the icon may be used as default.
	*/
	protected Vector pairs = null;
	/**
	* The path to the icon (image) corresponding to this combination of
	* attribute values (may be also a URL).
	*/
	protected String path = null;
	/**
	* The icon. This may be an image (if already loaded) or a GeomSign
	* (spade.vis.geometry) that draws itself
	*/
	protected Object icon = null;
	/**
	 * The scaling factor for the icon (e.g. some values may be represented by
	 * reduced icon sizes). The scaling factor is applied individually to the
	 * width and to the height of the icon (not to the area of the symbol!).
	 */
	protected float scaleFactor = Float.NaN;
	/**
	 * The icon may have a frame. In this case, this field specifies the width
	 * of the frame. If 0, no frame is drawn.
	 */
	protected int frameWidth = 0;
	/**
	 * If the icon has a frame, this is the color of the frame
	 */
	protected Color frameColor = null;

	/**
	* Sets the path to the image (may be a URL, i.e. contain protocol, host, etc.)
	*/
	public void setPathToImage(String path) {
		this.path = path;
	}

	/**
	* Returns the path to the image (may be a URL, i.e. contain protocol, host, etc.)
	*/
	public String getPathToImage() {
		return path;
	}

	/**
	* Sets the image to be used as the icon.
	*/
	public void setImage(Image image) {
		icon = image;
	}

	/**
	* Sets the icon (may be an image or a GeomSign)
	*/
	public void setIcon(Object icon) {
		this.icon = icon;
	}

	/**
	* Returns the icon that may be an image or a GeomSign
	*/
	public Object getIcon() {
		return icon;
	}

	/**
	 * Returns the width of the icon
	 */
	public int getIconWidth() {
		if (icon == null)
			return 0;
		if (icon instanceof GeomSign)
			return ((GeomSign) icon).getWidth();
		if (icon instanceof Image) {
			Image img = (Image) icon;
			int w = img.getWidth(null);
			if (Float.isNaN(scaleFactor) || scaleFactor <= 0)
				return w;
			return Math.round(scaleFactor * w);
		}
		return 0;
	}

	/**
	 * Returns the height of the icon
	 */
	public int getIconHeight() {
		if (icon == null)
			return 0;
		if (icon instanceof GeomSign)
			return ((GeomSign) icon).getHeight();
		if (icon instanceof Image) {
			Image img = (Image) icon;
			int h = img.getHeight(null);
			if (Float.isNaN(scaleFactor) || scaleFactor <= 0)
				return h;
			return Math.round(scaleFactor * h);
		}
		return 0;
	}

	/**
	 * Returns the scaling factor for the icon (e.g. some values may be
	 * represented by reduced icon sizes).
	 */
	public float getScaleFactor() {
		return scaleFactor;
	}

	/**
	 * Sets the scaling factor for the icon (e.g. some values may be represented
	 * by reduced icon sizes). The scaling factor is applied individually to the
	 * width and to the height of the icon (not to the area of the symbol!).
	 */
	public void setScaleFactor(float scaleFactor) {
		this.scaleFactor = scaleFactor;
	}

	/**
	 * Returns the width of the frame. If 0, no frame is drawn.
	 */
	public int getFrameWidth() {
		return frameWidth;
	}

	/**
	 * Sets the width of the frame. If 0, no frame is drawn.
	 */
	public void setFrameWidth(int frameWidth) {
		this.frameWidth = frameWidth;
	}

	/**
	 * Returns the color of the frame. If null, no frame is drawn.
	 */
	public Color getFrameColor() {
		return frameColor;
	}

	/**
	 * Sets the color of the frame. If null, no frame is drawn.
	 */
	public void setFrameColor(Color frameColor) {
		this.frameColor = frameColor;
	}

	/**
	* Loads the image from the URL that was set earlier.
	*/
	public Image loadImage(Component comp) {
		if (icon != null && (icon instanceof Image))
			return (Image) icon;
		if (path == null)
			return null;
		Image img = null;
		if (path.startsWith("http:") || path.startsWith("file:") || path.startsWith("ftp:")) {
			URL url = null;
			try {
				url = new URL(path);
			} catch (MalformedURLException e) {
				System.out.println("URL ERROR: " + e);
			}
			if (url == null)
				return null;
			img = comp.getToolkit().getImage(url);
		} else {
			img = comp.getToolkit().getImage(path);
		}
		MediaTracker mt = new MediaTracker(comp);
		mt.addImage(img, 0);
		try {
			mt.waitForID(0);
		} catch (InterruptedException e) {
			System.out.println("Could not load the image " + path + ":\n  " + e);
		}
		if (img != null) {
			icon = img;
		}
		return img;
	}

	/**
	* Adds a pair <attribute ID, value> to the specification of the value
	* combination. Null value means that the correspondence is valid for any value
	* of the specified attribute.
	*/
	public void addAttrValuePair(String attrId, String value) {
		if (attrId == null)
			return;
		String p[] = new String[2];
		p[0] = IdUtil.getPureAttrId(attrId);
		p[1] = value;
		if (pairs == null) {
			pairs = new Vector(3, 2);
		}
		pairs.addElement(p);
	}

	/**
	* Sets all attribute-value pairs for this icon at once. The previous pairs,
	* if any, are lost.
	*/
	public void setAttrValuePairs(Vector pairs) {
		if (pairs != null) {
			for (int i = 0; i < pairs.size(); i++) {
				String p[] = (String[]) pairs.elementAt(i);
				if (p != null) {
					p[0] = IdUtil.getPureAttrId(p[0]);
				}
			}
		}
		this.pairs = pairs;
	}

	/**
	* Returns the number of attribute-value pairs
	*/
	public int getPairCount() {
		if (pairs == null)
			return 0;
		return pairs.size();
	}

	/**
	* Checks if there is an attribute-value pair for the specified attribute
	*/
	public boolean hasAttribute(String attrId) {
		if (attrId == null || pairs == null)
			return false;
		attrId = IdUtil.getPureAttrId(attrId);
		for (int i = 0; i < pairs.size(); i++) {
			String p[] = (String[]) pairs.elementAt(i);
			if (p[0].equalsIgnoreCase(attrId))
				return true;
		}
		return false;
	}

	/**
	* Returns the value of the specified attribute if it occurs in the combination,
	* otherwise returns null.
	*/
	public String getAttrValue(String attrId) {
		if (attrId == null || pairs == null)
			return null;
		attrId = IdUtil.getPureAttrId(attrId);
		for (int i = 0; i < pairs.size(); i++) {
			String p[] = (String[]) pairs.elementAt(i);
			if (p[0].equalsIgnoreCase(attrId))
				return p[1];
		}
		return null;
	}

	/**
	* Checks if the specified attribute-value pair occurs in the combination.
	*/
	public boolean hasPair(String attrId, String value) {
		if (attrId == null || pairs == null)
			return false;
		String v = getAttrValue(attrId);
		if (v == null)
			return value == null && hasAttribute(attrId);
		return v.equalsIgnoreCase(value);
	}

	/**
	* Returns true if either the specified attribute-value pair occurs in the
	* combination or there is no such attribute in the combination.
	*/
	public boolean isApplicable(String attrId, String value) {
		String v = getAttrValue(attrId);
		if (v == null)
			return true;
		return v.equalsIgnoreCase(value);
	}

	/**
	* Returns true if there is no restriction on attribute values for that this
	* icon must be used, i.e. the vector pairs is empty or null.
	*/
	public boolean isDefault() {
		if (pairs == null || pairs.size() < 1)
			return true;
		for (int i = 0; i < pairs.size(); i++) {
			String p[] = (String[]) pairs.elementAt(i);
			if (p[1] != null)
				return false;
		}
		return true;
	}
}