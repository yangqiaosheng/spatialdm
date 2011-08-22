package spade.vis.mapvis;

import java.awt.Color;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Date;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.lib.help.Helper;
import spade.lib.lang.Language;
import spade.lib.util.StringUtil;
import spade.vis.database.AttributeTypes;

/**
* The base class for visualizers representing qualitative attributes that may
* have multiple values associated with a single object. Alternatively, such
* visualizers may represent simultaneously several logical attributes.
*/
public abstract class ListPresenter extends DataPresenter {
	/**
	* Indicates whether the visualizer represents different items by different
	* colors. By default is false.
	*/
	static ResourceBundle res = Language.getTextResource("spade.vis.mapvis.Res");
	protected boolean useColors = true;
	/**
	* The default color used when items are not distinguished by colors
	*/
	protected Color defaultColor = Color.darkGray;
	/**
	* Stores names of items for assignment of colors
	*/
	protected Vector itemNames = null;
	/**
	* Specifies colors for the items listed in itemNames.
	*/
	protected Vector itemColors = null;
	/**
	* Used to generate default colors
	*/
	protected Random random = null;
	/**
	* Used for remembering the assignment of colors to items to be reused next
	* time when this visualizer is activated.
	*/
	private static Vector storedNames = null, storedColors = null;
	/**
	* "Hidden" items, i.e. no symbols are drawn for them
	*/
	protected Vector hiddenItems = null;
	/**
	* Used for registered listeners of property changes. The ListPresenter may
	* notify about appearance of new items (when data are updated)
	*/
	protected PropertyChangeSupport pcSupport = null;

	/**
	* Replies whether the visualizer represents different items by different
	* colors.
	*/
	public boolean getUseColors() {
		return useColors;
	}

	/**
	* Sets the visualizer to represent different items by different colors (if
	* the argument is true) or by the same color (if false).
	*/
	public void setUseColors(boolean value) {
		useColors = value;
		notifyVisChange();
	}

	/**
	* This method informs whether the Visualizer produces diagrams.
	* This is important for defining the order of drawing of GeoLayers on the
	* map: the diagrams should be drawn on top of all geography.
	*/
	@Override
	public boolean isDiagramPresentation() {
		return true;
	}

	/**
	* Informs whether this visualizer allows the visualized attributes to
	* be transformed. This visualizer returns false.
	*/
	@Override
	public boolean getAllowTransform() {
		return false;
	}

	/**
	* Checks if this visualization method is applicable to the given set of
	* attributes. May use the DataInformer to check types and values
	* of the attributes.
	**/
	@Override
	public boolean isApplicable(Vector attr) {

		err = null;
		if (attr == null || attr.size() < 1) {
			err = errors[0];
			return false;
		}
		if (table == null) {
			err = errors[1];
			return false;
		}
		boolean testNumerical = table.isValuesCountBelow(attr, Helper.getMaxNumValuesForQualitativeVis());
		for (int i = 0; i < attr.size(); i++) {
			char at = table.getAttributeType((String) attr.elementAt(i));
			if (!((testNumerical && AttributeTypes.isIntegerType(at)) || (AttributeTypes.isNominalType(at)))) {
				err = attr.elementAt(i) + ": " + errors[10];
				return false;
			}
		}
		for (int i = 0; i < attr.size(); i++) {
			String id = (String) attr.elementAt(i);
			Vector values = table.getAllAttrValuesAsStrings(id);
			if (values == null) {
				err = id + ": " + errors[3];
				return false;
			}
			if (attr.size() > 1) { //check if all the attributes are logical
				if (values.size() > 2) {
					// following string: Attributes " (must be logical)"
					err = id + ": " + errors[10] + res.getString("_must_be_logical_");
					return false;
				}
				for (int j = 0; j < values.size(); j++) {
					String val = (String) values.elementAt(j);
					if (!val.equalsIgnoreCase("T") && !val.equalsIgnoreCase("F") && !val.equalsIgnoreCase("true") && !val.equalsIgnoreCase("false") && !val.equalsIgnoreCase("1") && !val.equalsIgnoreCase("0") && !val.equalsIgnoreCase("yes")
							&& !val.equalsIgnoreCase("no")) {
						// following string: Attributes " (must be logical)"
						err = id + ": " + errors[10] + res.getString("_must_be_logical_");
						return false;
					}
				}
			} else { //check if at least one value is a list of strings separated by ";"
				boolean hasMultipleValues = false;
				for (int j = 0; j < values.size() && !hasMultipleValues; j++) {
					String val = (String) values.elementAt(j);
					if (val.indexOf(';') < 1) {
						continue;
					}
					Vector v = StringUtil.getNames(val, ";");
					hasMultipleValues = (v != null && v.size() > 1);
				}
				if (!hasMultipleValues) {
					// following string: " (must be list)"
					err = id + ": " + errors[10] + res.getString("_must_be_list_");
					return false;
				}
			}
		}
		return true;
	}

	/**
	* Must check semantic applicability of this visualization method to the
	* attributes previously set in the visualizer.
	*/
	@Override
	public boolean checkSemantics() {
		return true;
	}

	/**
	* Returns the default color used when items are not distinguished by colors
	*/
	public Color getDefaultColor() {
		return defaultColor;
	}

	/**
	* Sets the default color to be used when items are not distinguished by colors
	*/
	public void setDefaultColor(Color color) {
		defaultColor = color;
	}

	/**
	* Returns the color for the given item. If the item is the identifier of an
	* attribute, uses the colorHandler for attributes.
	*/
	public Color getColorForItem(String item) {
		if (!useColors || attr == null || item == null)
			return null;
		if (attr.size() > 1 && colorHandler != null) //colors are used for attributes
			return getColorForAttribute(getAttrIndex(item));
		if (itemNames == null) {
			getAllItems();
		}
		int idx = StringUtil.indexOfStringInVectorIgnoreCase(item, itemNames);
		if (idx >= 0)
			return (Color) itemColors.elementAt(idx);
		if (itemNames == null) {
			itemNames = new Vector(20, 10);
			itemColors = new Vector(20, 10);
		}
		if (storedNames == null) {
			storedNames = new Vector(20, 20);
		}
		if (storedColors == null) {
			storedColors = new Vector(20, 20);
		}
		Color c = null;
		idx = StringUtil.indexOfStringInVectorIgnoreCase(item, storedNames);
		if (idx >= 0) {
			c = (Color) storedColors.elementAt(idx);
		} else {
			c = Color.getHSBColor(random.nextFloat(), 0.9f, 1.0f);
			storedNames.addElement(item);
			storedColors.addElement(c);
		}
		itemNames.addElement(item);
		itemColors.addElement(c);
		notifyPropertyChange("item_added", item);
		return c;
	}

	/**
	* Sets the color for the given item. If the item is the identifier of an
	* attribute, uses the colorHandler for attributes.
	*/
	public void setColorForItem(Color color, String item) {
		if (attr.size() > 1 && StringUtil.isStringInVectorIgnoreCase(item, attr) && colorHandler != null) {
			colorHandler.setColorForAttribute(color, item);
		} else {
			if (itemNames == null) {
				getAllItems();
			}
			int idx = StringUtil.indexOfStringInVectorIgnoreCase(item, itemNames);
			if (idx < 0)
				return;
			itemColors.setElementAt(color, idx);
		}
	}

	/**
	* Returns a vector of all items (without repetitions). The vector elements are
	* strings.
	*/
	public Vector getAllItems() {
		if (attr.size() > 1)
			if (colorHandler != null)
				return attr;
			else {
				itemNames = (Vector) attr.clone();
				itemColors = new Vector(attr.size(), 1);
				random = new Random((new Date()).getTime());
				for (int i = 0; i < attr.size(); i++) {
					itemColors.addElement(Color.getHSBColor(random.nextFloat(), 0.9f, 1.0f));
				}
				return itemNames;
			}
		if (itemNames != null && itemNames.size() > 0)
			return itemNames;
		itemNames = new Vector(20, 10);
		itemColors = new Vector(20, 10);
		if (storedNames == null) {
			storedNames = new Vector(20, 20);
		}
		if (storedColors == null) {
			storedColors = new Vector(20, 20);
		}
		random = new Random((new Date()).getTime());
		Vector values = (subAttr == null || subAttr.elementAt(0) == null) ? table.getAllAttrValuesAsStrings((String) attr.elementAt(0)) : table.getAllAttrValuesAsStrings((Vector) subAttr.elementAt(0));
		for (int i = 0; i < values.size(); i++) {
			String val = (String) values.elementAt(i);
			if (val == null) {
				continue;
			}
			Vector v = StringUtil.getNames(val, ";");
			if (v == null || v.size() < 1) {
				continue;
			}
			for (int j = 0; j < v.size(); j++) {
				val = (String) v.elementAt(j);
				if (StringUtil.isStringInVectorIgnoreCase(val, itemNames)) {
					continue;
				}
				int idx = StringUtil.indexOfStringInVectorIgnoreCase(val, storedNames);
				Color c = null;
				if (idx >= 0) {
					c = (Color) storedColors.elementAt(idx);
				} else {
					c = Color.getHSBColor(random.nextFloat(), 0.9f, 1.0f);
					storedNames.addElement(val);
					storedColors.addElement(c);
				}
				itemNames.addElement(val);
				itemColors.addElement(c);
			}
		}
		return itemNames;
	}

	/**
	* Returns a vector of colors used to represent items
	*/
	public Vector getItemColors() {
		if (attr.size() > 1 && colorHandler != null) {
			Vector colors = new Vector(attr.size(), 1);
			for (int i = 0; i < attr.size(); i++) {
				colors.addElement(getColorForAttribute(i));
			}
			return colors;
		}
		if (itemNames == null) {
			getAllItems();
		}
		return itemColors;
	}

	/**
	* Checks if the item is active or hidden
	*/
	public boolean isItemActive(String item) {
		if (hiddenItems == null)
			return true;
//ID
		return !StringUtil.isStringInVectorIgnoreCase(item, hiddenItems);
//~ID
	}

	/**
	* Makes the item active or hidden
	*/
	public void setItemActive(String item, boolean value) {
		if (item == null)
			return;
		if (value) {
			int idx = StringUtil.indexOfStringInVectorIgnoreCase(item, hiddenItems);
			if (idx < 0)
				return;
			hiddenItems.removeElementAt(idx);
			notifyVisChange();
		} else {
			if (hiddenItems == null) {
				hiddenItems = new Vector(20, 10);
			}
			if (StringUtil.isStringInVectorIgnoreCase(item, hiddenItems))
				return;
			hiddenItems.addElement(item);
			notifyVisChange();
		}
	}

	/**
	* Returns the name of the item with the given identifier. If the item is an
	* attribute value, it is returned unchanged. If this is an attribute
	* identifier, the name of the attribute is returned.
	*/
	public String getItemName(String itemId) {
		if (attr.size() > 1)
			return this.getAttrName(itemId);
		return itemId;
	}

	/**
	* Registers a listener of property changes. The ListPresenter may
	* notify about appearance of new items (when data are updated). This makes
	* sense only when values of a single list-type attribute are represented
	* rather than multiple logical attributes. In the latter case the listener
	* is not registered and, hence, not notified (because a new attribute
	* cannot be added).
	*/
	public void addPropertyChangeListener(PropertyChangeListener list) {
		if (list == null || (attr != null && attr.size() > 1))
			return;
		if (pcSupport == null) {
			pcSupport = new PropertyChangeSupport(this);
		}
		pcSupport.addPropertyChangeListener(list);
	}

	/**
	* Removes the listener of property changes.
	*/
	public void removePropertyChangeListener(PropertyChangeListener list) {
		if (list == null || pcSupport == null)
			return;
		pcSupport.removePropertyChangeListener(list);
	}

	/**
	* Notifies about a change of the property with the given name.
	*/
	protected void notifyPropertyChange(String propName, Object value) {
		if (pcSupport != null) {
			pcSupport.firePropertyChange(propName, null, value);
		}
	}
}
