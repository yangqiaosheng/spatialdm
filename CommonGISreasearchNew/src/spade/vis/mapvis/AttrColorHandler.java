package spade.vis.mapvis;

import java.awt.Color;
import java.beans.PropertyChangeListener;
import java.util.Vector;

/**
* This interface is to be implemented by classes that put colors in
* correspondence to attributes. For example, such a class may support selection
* of colors of bars in bar charts or sectors in pie charts. 
*/

public interface AttrColorHandler {
	/**
	* Returns the color used to represent the specified attribute.
	* May return null.
	*/
	public Color getColorForAttribute(String attrId);

	/**
	* Sets the color to be used to represent the specified attribute.
	*/
	public void setColorForAttribute(Color color, String attrId);

	/**
	* Sets the colors for the specified group of attributes.
	*/
	public void setColorsForAttributes(Vector colors, Vector attrIds);

	/**
	* Returns the identifiers of the attributes the AttrColorHandler currently
	* deals with. May return null.
	*/
	public Vector getAttributeList();

	/**
	* Returns the name of the attribute with the given identifier. If the name
	* is unknown, returns the identifier.
	*/
	public String getAttrName(String attrId);

	/**
	* notification about changes in attribute colors
	*/
	public void addPropertyChangeListener(PropertyChangeListener list);

	public void removePropertyChangeListener(PropertyChangeListener l);
}