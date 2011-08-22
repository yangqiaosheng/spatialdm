package spade.vis.mapvis;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.transform.AttributeTransformer;
import spade.analysis.transform.TransformerOwner;
import spade.lib.basicwin.Metrics;
import spade.lib.basicwin.StringInRectangle;
import spade.lib.lang.Language;
import spade.lib.util.Comparable;
import spade.lib.util.NumRange;
import spade.lib.util.QSortAlgorithm;
import spade.lib.util.StringUtil;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.DataItem;
import spade.vis.database.DataTreater;
import spade.vis.database.ThematicDataItem;
import spade.vis.database.ThematicDataOwner;
import spade.vis.dataview.DataViewRegulator;
import spade.vis.dataview.TransformedDataPresenter;
import spade.vis.map.MapContext;
import spade.vis.spec.ToolSpec;

/**
* An implementation of Visualizer: specifies how to present thematic data in
* a map. This is the class to be extended by all classes realizing
* various presentation methods such as painting or bar charts.
* Visualizer implements the LegendDrawer interface. This means that it should
* be able to draw the part of the legend explaining this presentation method.
* Visualizer may need statistics about data to setup its parameters.
* Visualizer may also show the statistics in the legend.
* Visualizer implements the PropertyChangeListener interface in order to
* listen to changes of statistics.
*/

public abstract class DataPresenter extends BaseVisualizer implements DataTreater, DataViewRegulator, TransformerOwner, TransformedDataPresenter, PropertyChangeListener {
	static ResourceBundle res = Language.getTextResource("spade.vis.mapvis.Res");
	public static final String errors[] =
	// following string:   "No attributes specified!"
	{ res.getString("No_attributes"), //0
			// following string: "No information about attribute values available!"
			res.getString("No_information_about"), //1
			// following string: "The attribute is non-numeric!"
			res.getString("The_attribute_is_non"), //2
			// following string: "No values of the attribute found!"
			res.getString("No_values_of_the"), //3
			// following string: "Too few values or all values are the same!"
			res.getString("Too_few_values_or_all"), //4
			// following string: "Illegal number of attributes!"
			res.getString("Illegal_number_of"), //5
			// following string: "The method is only applicable to a single attribute!"
			res.getString("The_method_is_only"), //6
			// following string: "Too few attributes!"
			res.getString("Too_few_attributes_"), //7
			// following string: "Too many attributes!"
			res.getString("Too_many_attributes_"), //8
			// following string: "Illegal attribute values!"
			res.getString("Illegal_attribute"), //9
			// following string: "Illegal attribute type!"
			res.getString("Illegal_attribute1"), //10
			res.getString("no_semantics") //11
	};
	/**
	* The table with the data to be visualized
	*/
	protected AttributeDataPortion table = null;
	/**
	* A DataPresenter may optionally be connected to a transformer of attribute
	* values. In this case, it represents transformed attribute values.
	*/
	protected AttributeTransformer aTrans = null;
	/**
	* The vector of identifiers of attributes to be visualized.
	*/
	protected Vector attr = null;
	/**
	* The vector of names of attributes to be visualized (if different from the
	* identifiers).
	*/
	protected Vector attrNames = null;
	/**
	* Some attributes may be dependent on a temporal parameter, and the visualizer
	* may be animated. In this case, the vector attr contains identifiers of
	* super-attributes, and the vector subAttr contains lists of identifiers of
	* the children of these super-attributes for different values of the temporal
	* parameter.
	*/
	protected Vector subAttr = null;
	/**
	* If there are time-dependent attributes, they may also refer to different
	* values of other, non-temporal parameters. For such attributes, this
	* vector contains "invariants" - strings indicating the values of the
	* additional parameters.
	*/
	protected Vector invariants = null;
	/**
	* If at least one of the visualized attributes has sub-attributes (i.e.
	* depends on a parameter), this variable specifies the index of the
	* subattribute to be currently taken for the visualization.
	*/
	protected int subAttrIdx = 0;
	/**
	* For performance optimization, keeps attribute indices in the table (column
	* numbers)
	*/
	protected int colNs[] = null;
	/**
	* The AttrColorHandler provides colors to be used to represent attributes
	* on a map
	*/
	protected AttrColorHandler colorHandler = null;

	/**
	* Sets a reference to the table with the data to visualize.
	*/
	public void setDataSource(AttributeDataPortion table) {
		if (this.table != null) {
			if (this.table.equals(table))
				return;
			this.table.removePropertyChangeListener(this);
		}
		this.table = table;
		if (table != null && aTrans == null) {
			table.addPropertyChangeListener(this);
		}
	}

	/**
	* Returns the reference to the table with the data to visualize.
	*/
	public AttributeDataPortion getDataSource() {
		return table;
	}

	/**
	* Informs whether this visualizer allows the visualized attributes to
	* be transformed. By default, returns true.
	*/
	public boolean getAllowTransform() {
		return true;
	}

	/**
	* Connects the DataPresenter to a transformer of attribute values. After this,
	* it must represent transformed attribute values. The argument listenChanges
	* determines whether the visualizer will listen to the changes of the transformed
	* values and appropriately reset itself. This is not always desirable; for
	* example, a visualizer may be a part of another visualizer, which makes
	* all necessary changes.
	*/
	public void setAttributeTransformer(AttributeTransformer transformer, boolean listenChanges) {
		if (aTrans != null) {
			aTrans.removePropertyChangeListener(this);
		}
		aTrans = transformer;
		if (listenChanges && aTrans != null) {
			aTrans.addPropertyChangeListener(this);
		}
	}

	/**
	* Returns its transformer of attribute values (if exists)
	*/
	@Override
	public AttributeTransformer getAttributeTransformer() {
		return aTrans;
	}

	/**
	* Informs whether this visualizer allows each of the visualized attributes to
	* be transformed individually (returns true) or requires all attributes to
	* be transformed in the same way (returns false). By default, returns true.
	*/
	public boolean getAllowTransformIndividually() {
		return true;
	}

	/**
	* Destroys the visualizer when it is no more used.
	*/
	@Override
	public void destroy() {
		if (aTrans != null) {
			aTrans.removePropertyChangeListener(this);
		}
		if (table != null) {
			table.removePropertyChangeListener(this);
		}
		colNs = null;
		super.destroy();
	}

	/**
	* The Visualizer sets its parameters. For this purpose it may need
	* data statistics. Statistics are received directly from the table.
	*/
	@Override
	public abstract void setup();

	/**
	* Called after data change. Calls "setup" method, which changes its parameters
	* in accord with the actual statistics. Notifies its listeners about
	* visualization change.
	*/
	public void actualizeParameters() {
		colNs = null;
		if (table == null)
			return;
		setup();
		notifyVisChange();
	}

	/**
	* Sets a provider of colors for attributes
	*/
	public void setAttrColorHandler(AttrColorHandler handler) {
		colorHandler = handler;
	}

	/**
	* Returns its provider of colors for attributes
	*/
	public AttrColorHandler getAttrColorHandler() {
		return colorHandler;
	}

	/**
	* This method is called by a GeoObject. The GeoObject sends its DataItem
	* to the Visualizer, and the Visualizer returns an Object specifying the
	* presentation of data contained in this DataItem. This may be, in
	* particular, Color or Diagram. Typically the argument DataItem is
	* a ThematicDataItem, although in some cases (e.g. manual classification)
	* this is not required.
	*/
	@Override
	public Object getPresentation(DataItem dit, MapContext mc) {
		if (dit == null)
			return null;
		if (dit instanceof ThematicDataItem)
			return getPresentation((ThematicDataItem) dit);
		if (dit instanceof ThematicDataOwner) {
			ThematicDataItem thema = ((ThematicDataOwner) dit).getThematicData();
			if (thema == null)
				return null;
			return getPresentation(thema);
		}
		return null;
	}

	/**
	* All descendants of the DataPresenter class visualize ThematicDataItems
	*/
	public abstract Object getPresentation(ThematicDataItem dit);

	/**
	* This method informs whether the Visualizer produces diagrams.
	* This is important for defining the order of drawing of GeoLayers on the
	* map: the diagrams should be drawn on top of all geography.
	*/
	@Override
	public abstract boolean isDiagramPresentation();

	/**
	* Checks if this visualization method is applicable to the given set of
	* attributes. May use the DataInformer to check types and values
	* of the attributes.
	*/
	public abstract boolean isApplicable(Vector attr);

	/**
	* Checks semantic applicability of this visualization method to the
	* attributes previously set in the visualizer. May use the DataInformer to
	* get semantic information about the attributes.
	*/
	public abstract boolean checkSemantics();

	/**
	* Sets the list of identifiers of attributes to be visualized.
	*/
	public void setAttributes(Vector attributes) {
		setAttributes(attributes, null);
	}

	/**
	* Sets the list of identifiers of attributes to be visualized.
	* Additionally, full names of the attributes (if different from the
	* identifiers) may be specified.
	*/
	public void setAttributes(Vector attributes, Vector attrNames) {
		boolean changed = false;
		this.attrNames = attrNames;
		if (attr == null)
			if (attributes == null)
				return;
			else {
				changed = true;
			}
		else if (attributes == null || attr.size() != attributes.size()) {
			changed = true;
		} else {
			for (int i = 0; i < attr.size() && !changed; i++) {
				changed = !StringUtil.sameStringsIgnoreCase((String) attr.elementAt(i), (String) attributes.elementAt(i));
			}
		}
		if (changed) {
			if (subAttr != null)
				if (attr == null || attr.size() < 1 || attributes == null || attributes.size() < 1) {
					subAttr = null;
				} else {
					boolean found = false;
					for (int i = 0; i < subAttr.size() && !found; i++) {
						found = subAttr.elementAt(i) != null && attributes.contains(attr.elementAt(i));
					}
					if (!found) {
						subAttr = null;
					} else {
						Vector v = new Vector(attributes.size(), 1);
						for (int i = 0; i < attributes.size(); i++) {
							v.addElement(null);
						}
						for (int i = 0; i < subAttr.size(); i++)
							if (subAttr.elementAt(i) != null) {
								int idx = attributes.indexOf(attr.elementAt(i));
								if (idx >= 0) {
									v.insertElementAt(subAttr.elementAt(i), idx);
								}
							}
						subAttr = v;
					}
				}
			attr = attributes;
			colNs = null;
			notifyVisChange();
		}
	}

	/**
	* Sets the name of the attribute with the given index in the list of the
	* attributes handled by this visualizer.
	*/
	public void setAttrName(String name, int attrIdx) {
		if (name == null || attrIdx < 0 || attr == null || attrIdx >= attr.size())
			return;
		if (attrNames == null) {
			attrNames = new Vector(attr.size(), 1);
		}
		for (int i = attrNames.size(); i < attr.size(); i++) {
			attrNames.addElement(null);
		}
		attrNames.setElementAt(name, attrIdx);
	}

	/**
	* If there are time-dependent attributes, they may also refer to different
	* values of other, non-temporal parameters. For such attributes, this
	* vector contains "invariants" - strings indicating the values of the
	* additional parameters. This method sets the invariant for a group of
	* "sub-attributes" corresponding to the attribute with the given index.
	*/
	public void setInvariant(String inv, int attrIdx) {
		if (inv == null || attrIdx < 0 || attr == null || attrIdx >= attr.size())
			return;
		if (invariants == null) {
			invariants = new Vector(attr.size(), 1);
		}
		for (int i = invariants.size(); i < attr.size(); i++) {
			invariants.addElement(null);
		}
		invariants.setElementAt(inv, attrIdx);
	}

	/**
	* Returns the invariant of the attribute with the given index (an invariant
	* indicates attribute's reference to particular parameter values).
	*/
	public String getInvariant(int attrIdx) {
		if (attr == null || attrIdx < 0 || attrIdx >= attr.size())
			return null;
		if (invariants != null && invariants.size() > attrIdx)
			return (String) invariants.elementAt(attrIdx);
		return null;
	}

	/**
	* Returns the identifier of the attribute with the given index. If this is
	* a parameter-dependent attribute, returns its identifier extended with the
	* attribute's invariant (an invariant indicates attribute's reference to
	* particular parameter values).
	*/
	public String getInvariantAttrId(int attrIdx) {
		if (attr == null || attrIdx < 0 || attrIdx >= attr.size())
			return null;
		String id = (String) attr.elementAt(attrIdx);
		if (id == null)
			return null;
		if (invariants != null && invariants.size() > attrIdx && invariants.elementAt(attrIdx) != null) {
			id += (String) invariants.elementAt(attrIdx);
		}
		return id;
	}

	/**
	* Returns the list of attributes being visualized.
	*/
	public Vector getAttributes() {
		return attr;
	}

	/**
	* Some attributes may be dependent on parameters. This method associates such
	* an attribute with a list of identifiers of the children attributes. The
	* attribute is specified through its index in the list of the attributes
	* handled by this visualizer.
	*/
	public void setSubAttributes(Vector sub, int attrIdx) {
		if (attr == null || attrIdx < 0 || attrIdx >= attr.size())
			return;
		if (subAttr == null) {
			subAttr = new Vector(attr.size(), 5);
		}
		while (subAttr.size() <= attrIdx) {
			subAttr.addElement(null);
		}
		subAttr.setElementAt(sub, attrIdx);
		colNs = null;
	}

	/**
	* Some attributes may be dependent on parameters. This method returns
	* a list of identifiers of the children attributes of a parameter-dependent
	* attribute specified through its index in the list of the attributes
	* handled by this visualizer.
	*/
	public Vector getSubAttributes(int attrIdx) {
		if (attr == null || subAttr == null || attrIdx < 0 || attrIdx >= attr.size() || attrIdx >= subAttr.size())
			return null;
		return (Vector) subAttr.elementAt(attrIdx);
	}

	/**
	* Informs if there are any attributes with subattributes, i.e. depending on
	* parameters.
	*/
	public boolean hasSubAttributes() {
		if (subAttr == null)
			return false;
		for (int i = 0; i < subAttr.size(); i++)
			if (subAttr.elementAt(i) != null)
				return true;
		return false;
	}

	/**
	* Sets the index of the subattribute to be currently taken for the
	* visualization.
	*/
	public void setCurrentSubAttrIndex(int idx) {
		subAttrIdx = idx;
		colNs = null;
	}

	/**
	* Returns the index of the subattribute currently used for the
	* visualization.
	*/
	public int getCurrentSubAttrIndex() {
		return subAttrIdx;
	}

	public String getCurrentSubAttrId(int attrIdx) {
		if (attr == null || subAttr == null || attrIdx < 0 || attrIdx >= attr.size() || attrIdx >= subAttr.size())
			return null;
		if (subAttrIdx < 0)
			return null;
		return ((Vector) subAttr.elementAt(attrIdx)).elementAt(subAttrIdx).toString();
	}

	/**
	* A method from the DataTreater interface.
	* Returns the list of attributes being visualized. If there are no
	* parameter-dependent attributes, returns the same as getAttributes().
	* Otherwise, takes the identifiers of the current sub-attributes,
	* depending of the value of the variable subAttrIdx.
	*/
	@Override
	public Vector getAttributeList() {
		if (attr == null)
			return null;
		Vector v = (Vector) attr.clone();
		if (subAttr != null) {
			for (int i = 0; i < subAttr.size(); i++)
				if (subAttr.elementAt(i) != null) {
					Vector sub = (Vector) subAttr.elementAt(i);
					if (sub.size() > subAttrIdx) {
						if (sub.elementAt(subAttrIdx) != null) {
							v.setElementAt(sub.elementAt(subAttrIdx), i);
						} else {
							for (int j = subAttrIdx - 1; j >= 0; j--)
								if (sub.elementAt(j) != null) {
									v.setElementAt(sub.elementAt(j), i);
									break;
								}
						}
					} else if (sub.size() > 0) {
						v.setElementAt(sub.elementAt(0), i);
					}
				}
		}
		return v;
	}

	/**
	* A method from the DataTreater interface.
	* Replies whether it is linked to the data set (table) with the given identifier
	*/
	@Override
	public boolean isLinkedToDataSet(String setId) {
		if (table != null)
			return table.getEntitySetIdentifier().equals(setId);
		return tableId != null && tableId.equals(setId);
	}

	/**
	* Returns the name of the attribute with the given index in the list of the
	* attributes handled by this visualizer. If the name is unknown, returns the
	* identifier.
	*/
	public String getAttrName(int attrIdx) {
		if (attrIdx < 0 || attrIdx >= attr.size())
			return null;
		String name = null;
		if (attrNames != null && attrIdx < attrNames.size()) {
			name = (String) attrNames.elementAt(attrIdx);
		}
		if (name == null) {
			name = (String) attr.elementAt(attrIdx);
		}
		return name;
	}

	/**
	* Returns the name of the attribute with the given identifier. If the name
	* is unknown, returns the identifier.
	*/
	public String getAttrName(String attrId) {
		if (attr == null || attrNames == null)
			return attrId;
		int idx = StringUtil.indexOfStringInVectorIgnoreCase(attrId, attr);
		if (idx >= 0 && idx < attrNames.size())
			return (String) attrNames.elementAt(idx);
		if (subAttr == null)
			return attrId;
		for (int i = 0; i < subAttr.size(); i++)
			if (subAttr.elementAt(i) != null) {
				Vector sub = (Vector) subAttr.elementAt(i);
				idx = StringUtil.indexOfStringInVectorIgnoreCase(attrId, sub);
				if (idx >= 0)
					if (i < attrNames.size())
						return (String) attrNames.elementAt(i);
					else
						return attrId;
			}
		return attrId;
	}

	/**
	* Returns the list of names of the attributes or null if not specified.
	*/
	public Vector getAttrNames() {
		return attrNames;
	}

	/**
	* Returns the identifier of the attribute with the given index. If this is
	* a parameter-dependent attribute, returns the identifier of the current
	* sub-attribute.
	*/
	public String getAttrId(int attrIdx) {
		if (attr == null || attrIdx < 0 || attrIdx >= attr.size())
			return null;
		if (subAttr == null || attrIdx >= subAttr.size() || subAttr.elementAt(attrIdx) == null)
			return (String) attr.elementAt(attrIdx);
		Vector sub = (Vector) subAttr.elementAt(attrIdx);
		if (sub.size() > subAttrIdx)
			return (String) sub.elementAt(subAttrIdx);
		if (sub.size() > 0)
			return (String) sub.elementAt(0);
		return (String) attr.elementAt(attrIdx);
	}

	/**
	* Returns the index of the given attribute identifier in the list of identifiers
	*/
	public int getAttrIndex(String attrId) {
		if (attr == null || attr.size() < 1)
			return -1;
		if (attrId == null)
			if (attr.elementAt(0) == null)
				return 0;
			else
				return -1;
		int idx = StringUtil.indexOfStringInVectorIgnoreCase(attrId, attr);
		if (idx >= 0)
			return idx;
//ID
		// workaround for ids with ##table## at the beginning
		if (attrId.startsWith("##")) {
			attrId = attrId.substring(attrId.lastIndexOf("##") + 2);
		}
		idx = StringUtil.indexOfStringInVectorIgnoreCase(attrId, attr);
		if (idx >= 0)
			return idx;
//~ID
		//possibly, this identifier has been modified using an invariant
		if (invariants != null) {
			for (int i = 0; i < invariants.size(); i++) {
				String inv = (String) invariants.elementAt(i);
				if (inv != null && attrId.equalsIgnoreCase((String) attr.elementAt(i) + inv))
					return i;
			}
		}
		if (subAttr == null)
			return -1;
		for (int i = 0; i < subAttr.size(); i++)
			if (subAttr.elementAt(i) != null) {
				int k = StringUtil.indexOfStringInVectorIgnoreCase(attrId, (Vector) subAttr.elementAt(i));
				if (k >= 0)
					if (attr.elementAt(0) == null)
						return i + 1;
					else
						return i;
			}
		return -1;
	}

	/**
	* A convenience method for determining the number of the column corresponding
	* to the attribute with the given index. If attrIdx is an index of a
	* parameter-dependent attribute, i.e. the one having sub-attributes, the
	* column number of the current sub-attribute is returned, depending on the
	* value of the variable subAttrIdx.
	*/
	public int getColumnN(int attrIdx) {
		if (table == null || attr == null || attrIdx < 0 || attrIdx >= attr.size())
			return -1;
		if (colNs != null)
			return colNs[attrIdx];
		colNs = new int[attr.size()];
		int start = 0;
		if (attr.elementAt(0) == null) {
			colNs[0] = -1;
			start = 1;
		}
		for (int i = start; i < attr.size(); i++)
			if (subAttr == null || i >= subAttr.size() || subAttr.elementAt(i) == null) {
				colNs[i] = table.getAttrIndex((String) attr.elementAt(i));
			} else {
				Vector sub = (Vector) subAttr.elementAt(i);
				if (sub.size() > subAttrIdx)
					if (sub.elementAt(subAttrIdx) != null) {
						colNs[i] = table.getAttrIndex((String) sub.elementAt(subAttrIdx));
					} else {
						colNs[i] = -1;
					}
				else if (sub.size() > 0) {
					colNs[i] = table.getAttrIndex((String) sub.elementAt(0));
				} else {
					colNs[i] = table.getAttrIndex((String) attr.elementAt(i));
				}
			}
		return colNs[attrIdx];
	}

	/**
	* A convenience method for retrieving a value of a numeric attribute with the
	* given index from the given ThematicDataItem. If attrIdx is an index of a
	* parameter-dependent attribute, i.e. the one having sub-attributes, the
	* value of the current sub-attribute is retrieved, depending on the value of
	* the variable subAttrIdx.
	*/
	public double getNumericAttrValue(ThematicDataItem data, int attrIdx) {
		if (data == null)
			return Float.NaN;
		int colN = getColumnN(attrIdx);
		if (colN < 0)
			return Float.NaN;
		if (aTrans != null)
			return aTrans.getNumericAttrValue(colN, data);
		return data.getNumericAttrValue(colN);
	}

	/**
	* A convenience method for getting the value range of a numeric attribute
	* from the table or attribute transformer (if exists).
	*/
	public NumRange getAttrValueRange(String attrId) {
		if (aTrans != null)
			return aTrans.getAttrValueRange(attrId);
		if (table != null)
			return table.getAttrValueRange(attrId);
		return null;
	}

	/**
	* A convenience method for getting the value range of a set of numeric
	* attributes from the table or attribute transformer (if exists).
	*/
	public NumRange getAttrValueRange(Vector attrIds) {
		if (aTrans != null)
			return aTrans.getAttrValueRange(attrIds);
		if (table != null)
			return table.getAttrValueRange(attrIds);
		return null;
	}

	/**
	* A convenience method for retrieving a value of a non-numeric attribute with
	* the given index from the given ThematicDataItem. If attrIdx is an index of a
	* parameter-dependent attribute, i.e. the one having sub-attributes, the
	* value of the current sub-attribute is retrieved, depending on the value of
	* the variable subAttrIdx.
	*/
	public String getStringAttrValue(ThematicDataItem data, int attrIdx) {
		if (data == null)
			return null;
		int colN = getColumnN(attrIdx);
		if (colN < 0)
			return null;
		return data.getAttrValueAsString(colN);
	}

	/**
	* A convenience method for retrieving a value of the attribute with
	* the given index from the given ThematicDataItem. If attrIdx is an index of a
	* parameter-dependent attribute, i.e. the one having sub-attributes, the
	* value of the current sub-attribute is retrieved, depending on the value of
	* the variable subAttrIdx.
	*/
	public Object getAttrValue(ThematicDataItem data, int attrIdx) {
		if (data == null)
			return null;
		int colN = getColumnN(attrIdx);
		if (colN < 0)
			return null;
		return data.getAttrValue(colN);
	}

	/**
	* A method from the DataTreater interface.
	* Must return a vector of colors used for representation of the attributes
	* this Data Treater deals with. By default, returns null.
	*/
	@Override
	public Vector getAttributeColors() {
		if (attr == null || attr.size() < 1)
			return null;
		if (this instanceof AttrColorHandler) {
			Vector colors = new Vector(attr.size(), 1);
			for (int i = 0; i < attr.size(); i++) {
				colors.addElement(getColorForAttribute(i));
			}
			return colors;
		}
		return null;
	}

	public Color getColorForAttribute(int idx) {
		if (colorHandler == null || idx < 0 || attr == null || idx >= attr.size())
			return null;
		String id = getInvariantAttrId(idx);
		if (id == null)
			return null;
		return colorHandler.getColorForAttribute(id);
	}

	public void setColorForAttribute(Color color, int idx) {
		if (color == null || colorHandler == null || idx < 0 || attr == null || idx >= attr.size())
			return;
		String id = getInvariantAttrId(idx);
		if (id != null && !color.equals(colorHandler.getColorForAttribute(id))) {
			colorHandler.setColorForAttribute(color, id);
		}
	}

	/**
	* The method from the LegendDrawer interface.
	* Draws the common part of the legend irrespective of the presentation method
	* (e.g. the name of the map), then calls drawMethodSpecificLegend(...)
	*/
	@Override
	public Rectangle drawLegend(Component c, Graphics g, int startY, int leftMarg, int prefW) {
		if (!enabled)
			return drawReducedLegend(c, g, startY, leftMarg, prefW);
		drawCheckbox(c, g, startY, leftMarg);
		int w = switchSize + Metrics.mm(), y = startY;
		g.setColor(Color.black);
		String name = getVisualizationName();
		if (name != null) {
			Point p = StringInRectangle.drawText(g, res.getString("vis_method") + ": " + name, leftMarg + w, y, prefW - w, false);
			y = p.y;
			w = p.x - leftMarg;
		}
		if (y < startY + switchSize + Metrics.mm()) {
			y = startY + switchSize + Metrics.mm();
		}
		if (table != null) {
			Point p = StringInRectangle.drawText(g, table.getName(), leftMarg, y, prefW, true);
			y = p.y;
			if (w < p.x - leftMarg) {
				w = p.x - leftMarg;
			}
		}
		if (aTrans != null) {
			String descr = aTrans.getDescription();
			if (descr != null) {
				descr = res.getString("Data_transformation") + ": " + descr;
				Point p = StringInRectangle.drawText(g, descr, leftMarg, y, prefW, true);
				y = p.y;
				if (w < p.x - leftMarg) {
					w = p.x - leftMarg;
				}
			}
		}
		Rectangle r = drawMethodSpecificLegend(g, y, leftMarg, prefW);
		if (r != null) {
			if (r.width < w) {
				r.width = w;
			}
			r.height += r.y - startY;
			r.y = startY;
			return r;
		}
		return new Rectangle(leftMarg, startY, w, y - startY);
	}

	/**
	* Draws the list of attribute names used in the visualization. If there are
	* many attributes, tries to find common parents and use them for shortening
	* the text.
	*/
	protected Rectangle drawAttrNames(Graphics g, int startY, int leftMarg, int prefW, boolean centered) {
		if (attr == null || attr.size() < 1)
			return null;
		g.setColor(Color.black);
		Rectangle r = new Rectangle(leftMarg, startY, 0, 0);
		int y = startY;
		Vector strings = new Vector(attr.size(), 1);
		if (attr.size() > 5 && table != null) {
			Vector simpleAttr = new Vector(attr.size(), 1);
			Vector supAttr = new Vector(10, 10);
			Vector params = new Vector(10, 10);
			Vector parValues = new Vector(10, 10);
			for (int i = 0; i < attr.size(); i++) {
				Attribute at = table.getAttribute((String) attr.elementAt(i));
				if (at == null || at.hasChildren()) {
					continue;
				}
				if (at.getParent() != null) {
					if (!supAttr.contains(at.getParent())) {
						supAttr.addElement(at.getParent());
					}
					for (int j = 0; j < at.getParameterCount(); j++) {
						String pname = at.getParamName(j);
						Vector v = null;
						int idx = params.indexOf(pname);
						if (idx < 0) {
							params.addElement(pname);
							v = new Vector(50, 10);
							parValues.addElement(v);
						} else {
							v = (Vector) parValues.elementAt(idx);
						}
						Object pv = at.getParamValue(j);
						if (!v.contains(pv)) {
							v.addElement(pv);
						}
					}
				} else {
					simpleAttr.addElement(at);
				}
			}
			if (supAttr.size() > 0 && params.size() > 0 && supAttr.size() + simpleAttr.size() + params.size() + 2 < attr.size()) {
				centered = false;
				if (supAttr.size() == 1) {
					strings.addElement(res.getString("Attribute") + ": " + ((Attribute) supAttr.elementAt(0)).getName());
				} else {
					strings.addElement(res.getString("Attributes") + ":");
					for (int i = 0; i < supAttr.size(); i++) {
						strings.addElement(((Attribute) supAttr.elementAt(i)).getName());
					}
				}
				strings.addElement(((params.size() > 1) ? res.getString("Parameters") : res.getString("Parameter")) + ":");
				for (int i = 0; i < params.size(); i++) {
					String str = (String) params.elementAt(i) + ": ";
					Vector v = (Vector) parValues.elementAt(i);
					if (v.size() > 5 && (v.elementAt(0) instanceof Comparable)) {
						QSortAlgorithm.sort(v);
						str += res.getString("from") + v.elementAt(0).toString() + " " + res.getString("to") + v.elementAt(v.size() - 1).toString();
					} else {
						for (int j = 0; j < v.size(); j++) {
							if (j > 0) {
								str += ", ";
							}
							str += v.elementAt(j).toString();
						}
					}
					strings.addElement(str);
				}
				if (simpleAttr.size() > 0) {
					strings.addElement(res.getString("Other_attributes" + ":"));
					for (int i = 0; i < simpleAttr.size(); i++) {
						strings.addElement(((Attribute) simpleAttr.elementAt(i)).getName());
					}
				}
			}
		}
		if (strings.size() < 1) {
			for (int i = 0; i < attr.size(); i++) {
				strings.addElement(getAttrName(i));
			}
		}
		for (int i = 0; i < strings.size(); i++) {
			Point p = StringInRectangle.drawText(g, (String) strings.elementAt(i), leftMarg, y, prefW, centered);
			y = p.y + 3;
			p.x -= leftMarg;
			if (r.width < p.x) {
				r.width = p.x;
			}
		}
		r.height = y - startY;
		return r;
	}

	/**
	* A method from the DataViewRegulator interface.
	* Replies whether attributes with null values should be shown in data popups.
	* Returns true if the number of the attributes is more than 5
	*/
	@Override
	public boolean getShowAttrsWithNullValues() {
		return attr == null || attr.size() < 6;
	}

	/**
	* A method from the DataViewRegulator interface.
	* Must return the class number for the data record with the given index.
	* Returns -1.
	*/
	@Override
	public int getRecordClassN(int recN) {
		return -1;
	}

	/**
	* A method from the DataViewRegulator interface.
	* Must return the name of the class with the given number. Returns null.
	*/
	@Override
	public String getClassName(int classN) {
		return null;
	}

	/**
	* A method from the DataViewRegulator interface.
	* Must return the color of the class with the given number. Returns null.
	*/
	@Override
	public Color getClassColor(int classN) {
		return null;
	}

	/**
	* Reacts to changes of the transformed data in its AttributeTransformer
	*/
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		//System.out.println("Visualizer "+getClass()+" received event from "+e.getSource()+":\n"+
		//e.getPropertyName()+" "+e.getOldValue()+" "+e.getNewValue());
		boolean eventFromTransformer = e.getSource().equals(aTrans) && e.getPropertyName().equals("values");
		if (eventFromTransformer || (e.getSource().equals(table) && (e.getPropertyName().equals("values") || e.getPropertyName().equals("data_added") || e.getPropertyName().equals("data_removed") || e.getPropertyName().equals("data_updated")))) {
			boolean valuesTransformed = eventFromTransformer;
			if (valuesTransformed) {
				Object nv = e.getNewValue();
				if (nv != null && (nv instanceof String) && ((String) nv).equalsIgnoreCase("original_data")) {
					valuesTransformed = false;
				}
			}
			if (adjustToDataChange(valuesTransformed)) {
				notifyVisChange();
			}
		}
	}

	/**
	 * Should return true if the visualization changes.
	 * By default calls setup() and returns true.
	 * The argument "valuesTransformed" indicates whether the values have been
	 * transformed (e.g. by an attribute transformer), which means that the
	 * value range might completely change. In this case, the visualiser may
	 * need to reset its parameters. Otherwise, a slight adaptation may be
	 * sufficient, if needed at all.
	 */
	public boolean adjustToDataChange(boolean valuesTransformed) {
		setup();
		return true;
	}

	/**
	* A method from the TransformedDataPresenter interface.
	* Returns the transformed value corresponding to the given row and column of
	* the original table. If the value is not transformed, returns null.
	*/
	@Override
	public String getTransformedValue(int rowN, int colN) {
		if (aTrans != null)
			return aTrans.getTransformedValueAsString(rowN, colN);
		return null;
	}

//ID
	@Override
	public ToolSpec getVisSpec() {
		ToolSpec spec = super.getVisSpec();
		if (spec == null) {
			spec = new ToolSpec();
		}
		spec.tagName = getTagName();
		spec.methodId = visId;
		spec.table = tableId;
		spec.attributes = getAttributeList();
		if (aTrans != null) {
			spec.transformSeqSpec = aTrans.getSpecSequence();
		}
		Hashtable prop = getVisProperties();
		if (prop != null)
			if (spec.properties == null) {
				spec.properties = prop;
			} else {
				for (Enumeration e = prop.keys(); e.hasMoreElements();) {
					Object key = e.nextElement();
					spec.properties.put(key, prop.get(key));
				}
			}
		return spec;
	}
//~ID
}
