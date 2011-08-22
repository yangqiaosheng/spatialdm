package connection.zeno;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.util.Vector;

import spade.lib.util.StringUtil;

/**
* A panel in which the user may view and edit current attribute values
* associated with some object.
*/
public class ObjectDataPanel extends Panel {
	/**
	* The names of the attributes
	*/
	protected Vector attrNames = null;
	/**
	* The text fields for the attribute values
	*/
	protected Vector textFields = null;
	/**
	* The layout used in the panel
	*/
	protected GridBagLayout gbLayout = null;

	public void addAttrValuePair(String attr, String value) {
		if (attr == null)
			return;
		if (attrNames == null) {
			attrNames = new Vector(20, 10);
		}
		attrNames.addElement(attr);
		if (textFields == null) {
			textFields = new Vector(20, 10);
		}
		TextField tf = new TextField(30);
		if (value != null) {
			tf.setText(value);
		}
		textFields.addElement(tf);
		if (gbLayout == null) {
			gbLayout = new GridBagLayout();
			setLayout(gbLayout);
		}
		GridBagConstraints c = new GridBagConstraints();
		c.weighty = 1.0f;
		c.fill = GridBagConstraints.HORIZONTAL;
		Label l = new Label(attr + ":");
		c.gridwidth = 1;
		c.weightx = 0.0f;
		gbLayout.setConstraints(l, c);
		add(l);
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1.0f;
		gbLayout.setConstraints(tf, c);
		add(tf);
	}

	public boolean hasAttribute(String attr) {
		if (attr == null || attrNames == null)
			return false;
		return StringUtil.isStringInVectorIgnoreCase(attr, attrNames);
	}

	public int getAttributeCount() {
		if (attrNames == null)
			return 0;
		return attrNames.size();
	}

	public String getAttributeValue(String attr) {
		if (attr == null || attrNames == null)
			return null;
		return getAttributeValue(StringUtil.indexOfStringInVectorIgnoreCase(attr, attrNames));
	}

	public String getAttributeValue(int idx) {
		if (idx < 0 || idx >= getAttributeCount())
			return null;
		TextField tf = (TextField) textFields.elementAt(idx);
		String value = tf.getText();
		if (value == null)
			return null;
		value = value.trim();
		if (value.length() < 1)
			return null;
		return value;
	}

	public String getAttributeName(int idx) {
		if (idx < 0 || idx >= getAttributeCount())
			return null;
		return (String) attrNames.elementAt(idx);
	}
}
