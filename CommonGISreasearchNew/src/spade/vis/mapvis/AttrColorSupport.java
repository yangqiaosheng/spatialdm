package spade.vis.mapvis;

import java.awt.Color;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Random;
import java.util.Vector;

import spade.lib.color.CS;

/**
* This is an implementation of the AttrColorHandler interface. The
* AttrColorSupports put colors in correspondence to attributes. For example,
* such a class may support selection of colors of bars in bar charts or
* sectors in pie charts.
*/

public class AttrColorSupport implements AttrColorHandler {
	/**
	* Maximum number of colors in a palette
	*/
	public static int maxNColors = 15;
	/**
	* Color palette used to select colors for attributes
	*/
	public static Color palette[] = null;
	/**
	* Used to generate random colors
	*/
	private static Random rnd = null;
	/**
	* Vector of identifiers of attributes
	*/
	protected Vector attr = null;
	/**
	* Colors to be used for the attributes
	*/
	protected Vector colors = null;

	/**
	* Creates a color palette used to select colors for attributes
	*/
	public static void createPalette() {
		if (palette == null) {
			palette = new Color[maxNColors];
			int n = maxNColors / 3;
			for (int j = 0; j < 3; j++) {
				for (int i = j * n; i < j * n + n; i++) {
					palette[i] = Color.getHSBColor(CS.getNthHue((i % n) * 3 + j, maxNColors), 1.0f, 0.75f);
				}
			}
		}
	}

	/**
	* Returns the palette color with the given index
	*/
	public static Color getPaletteColor(int idx) {
		if (palette == null) {
			createPalette();
		}
		if (idx < maxNColors)
			return palette[idx];
		if (rnd == null) {
			rnd = new Random(System.currentTimeMillis());
		}
		return new Color(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat());
	}

	/**
	* Returns the color used to represent the specified attribute.
	* If there is not yet any color assigned to this attribute, assigns some color
	* from the default palette and returns it.
	*/
	@Override
	public Color getColorForAttribute(String attrId) {
		if (attrId == null)
			return null;
		if (attr != null) {
			int idx = attr.indexOf(attrId);
			if (idx >= 0)
				return (Color) colors.elementAt(idx);
		}
		addAttribute(attrId);
		return (Color) colors.elementAt(attr.size() - 1);
	}

	/**
	* Sets the color to be used to represent the specified attribute.
	*/
	@Override
	public void setColorForAttribute(Color color, String attrId) {
		if (attrId == null)
			return;
		if (attr != null) {
			int idx = attr.indexOf(attrId);
			if (idx >= 0) {
				colors.setElementAt(color, idx);
				notifyChange("colors", attrId);
				return;
			}
		} else {
			attr = new Vector(10, 10);
			colors = new Vector(10, 10);
		}
		attr.addElement(attrId);
		colors.addElement(color);
		notifyChange("colors", attrId);
	}

	/**
	* Sets the colors for the specified group of attributes.
	*/
	@Override
	public void setColorsForAttributes(Vector attrColors, Vector attrIds) {
		if (attrIds == null || attrColors == null || attrIds.size() < 1 || attrColors.size() < 1)
			return;
		if (attr == null) {
			attr = new Vector(100, 100);
			colors = new Vector(100, 100);
		}
		Vector changed = new Vector(attrIds.size(), 1);
		for (int i = 0; i < attrIds.size() && i < attrColors.size(); i++) {
			String attrId = (String) attrIds.elementAt(i);
			int idx = attr.indexOf(attrId);
			if (idx >= 0) {
				colors.setElementAt(attrColors.elementAt(i), idx);
			} else {
				attr.addElement(attrId);
				colors.addElement(attrColors.elementAt(i));
			}
			changed.addElement(attrId);
		}
		if (changed.size() > 0) {
			notifyChange("colors", changed);
		}
	}

	/**
	* Adds an attribute to its register and assigns a default color to it
	*/
	public void addAttribute(String attrId) {
		if (attrId == null)
			return;
		if (attr != null && attr.contains(attrId))
			return;
		if (attr == null) {
			attr = new Vector(10, 10);
			colors = new Vector(10, 10);
		}
		attr.addElement(attrId);
		colors.addElement(getPaletteColor(attr.size() - 1));
	}

	/**
	* Removes identifiers of all earlier registered attributes and their
	* corresponding colors
	*/
	public void clear() {
		if (attr != null) {
			attr.removeAllElements();
			colors.removeAllElements();
		}
	}

	/**
	* Returns the identifiers of the attributes the AttrColorHandler currently
	* deals with. May return null.
	*/
	@Override
	public Vector getAttributeList() {
		if (attr == null)
			return null;
		return (Vector) attr.clone();
	}

	/**
	* Should return the name of the attribute with the given identifier.
	* However, for AttrColorSupport the name is unknown, therefore it returns
	* the identifier.
	*/
	@Override
	public String getAttrName(String attrId) {
		return attrId;
	}

	//----------------- notification about attribute colors changes---------------
	protected PropertyChangeSupport pcSupport = null;

	@Override
	public synchronized void addPropertyChangeListener(PropertyChangeListener l) {
		if (l == null)
			return;
		if (pcSupport == null) {
			pcSupport = new PropertyChangeSupport(this);
		}
		pcSupport.addPropertyChangeListener(l);
	}

	@Override
	public synchronized void removePropertyChangeListener(PropertyChangeListener l) {
		if (l == null || pcSupport == null)
			return;
		pcSupport.removePropertyChangeListener(l);
	}

	protected void notifyChange(String what, Object value) {
		if (pcSupport == null)
			return;
		pcSupport.firePropertyChange(what, null, value);
	}
}