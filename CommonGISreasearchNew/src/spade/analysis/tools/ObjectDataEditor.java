package spade.analysis.tools;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Choice;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.TextField;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.DialogContent;
import spade.lib.basicwin.OKDialog;
import spade.lib.lang.Language;
import spade.vis.database.AttributeTypes;

/**
* A structure describing an attribute: its name, identifier, and type
*/
class AttrDescr {
	public String name = null, id = null;
	public char type = AttributeTypes.character;
}

/**
* A UI component allowing the user to enter or edit data about an object,
* e.g. a geographical object.
*/
public class ObjectDataEditor extends Panel implements ActionListener, DialogContent {
	static ResourceBundle res = Language.getTextResource("spade.analysis.tools.Res");
	/**
	* The main panel (if necessary, included in a scrollpane)
	*/
	protected Panel mainP = null;
	/**
	* The panel with the button "Add attribute". May be absent initially or
	* removed later on.
	*/
	protected Panel buttonPanel = null;
	/**
	* The text field for the object identifier
	*/
	protected TextField idTF = null;
	/**
	* The text field for the object name
	*/
	protected TextField nameTF = null;
	/**
	* The text fields for attribute values. The number of such fields may be
	* arbitrary. The user may add new attributes. In this case new text fields
	* will be added.
	*/
	protected Vector attrTF = null;
	/**
	* The information about the attributes. The elements of the vector are
	* instances of AttrDescr
	*/
	protected Vector attrInfo = null;
	/**
	* The error message (needed for implementation of the DialogContent interface)
	*/
	protected String err = null;

	/**
	* Constructs the panel with 2 input fields: for the identifier and for
	* the name. The argument mayEditId indicates whether the user is allowed to
	* change the given object identifier.
	*/
	public ObjectDataEditor() {
		mainP = new Panel(new GridLayout(3, 1));
		// following string: "Enter information about the object"
		mainP.add(new Label(res.getString("Enter_information"), Label.CENTER));
		Panel pp = new Panel(new BorderLayout());
		// following string:"Identifier:"
		pp.add(new Label(res.getString("Identifier_")), "West");
		idTF = new TextField(20);
		pp.add(idTF, "East");
		mainP.add(pp);
		pp = new Panel(new BorderLayout());
		pp.add(new Label(res.getString("Name_")), "West");
		nameTF = new TextField(20);
		pp.add(nameTF, "East");
		mainP.add(pp);
	}

	/**
	* Enables or disables the text field for entering/editing object identifier
	*/
	public void setMayEditId(boolean value) {
		idTF.setEnabled(value);
	}

	/**
	* Puts the object identifier and name in the corresponding text input fields.
	* If there are other fields, clears them.
	*/
	public void setObject(String objId, String objName) {
		if (objId != null) {
			idTF.setText(objId);
		}
		if (objName != null) {
			nameTF.setText(objName);
		}
	}

	/**
	* Adds a field for entering or editing the value of the specified attribute.
	* Remembers the identifier and the type of the attribute
	*/
	public void addAttribute(String attrName, String attrId, char attrType) {
		if (attrName == null)
			return;
		AttrDescr ad = new AttrDescr();
		ad.name = attrName;
		ad.id = attrId;
		if (AttributeTypes.isValidType(attrType)) {
			ad.type = attrType;
		}
		if (attrInfo == null) {
			attrInfo = new Vector(10, 10);
		}
		attrInfo.addElement(ad);
		if (attrTF == null) {
			attrTF = new Vector(10, 10);
		}
		Panel pp = new Panel(new BorderLayout());
		pp.add(new Label(attrName), "West");
		TextField tf = new TextField(20);
		pp.add(tf, "East");
		mainP.setLayout(new GridLayout(mainP.getComponentCount() + 1, 1));
		mainP.add(pp);
		attrTF.addElement(tf);
		if (isShowing()) {
			boolean mustPack = true;
			if (mainP.getComponentCount() > 10)
				if (!(mainP.getParent() instanceof ScrollPane)) {
					remove(mainP);
					ScrollPane scp = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
					scp.add(mainP);
					add(scp, "Center");
				} else {
					mustPack = false;
				}
			if (mustPack) {
				Window win = null;
				Component c = getParent();
				while (c != null && win == null) {
					if (c instanceof Window) {
						win = (Window) c;
					} else {
						c = c.getParent();
					}
				}
				if (win != null) {
					win.pack();
				}
			}
		}
	}

	@Override
	public Dimension getPreferredSize() {
		Dimension d = super.getPreferredSize(), dpref = mainP.getPreferredSize(), dsc = Toolkit.getDefaultToolkit().getScreenSize();
		if (mainP.getParent() instanceof ScrollPane) {
			ScrollPane scp = (ScrollPane) mainP.getParent();
			dpref.width += scp.getVScrollbarWidth() + 5;
			dpref.height += 50;
		}
		if (dpref.width > dsc.width - 50) {
			dpref.width = dsc.width - 50;
		}
		if (d.width < dpref.width) {
			d.width = dpref.width;
		}
		return d;
	}

	/**
	* Sets the specified value in the text field corresponding to the attribute
	* with the given index
	*/
	public void setAttrValue(int idx, String value) {
		if (idx < 0 || idx >= getAttrCount())
			return;
		TextField tf = (TextField) attrTF.elementAt(idx);
		if (value == null) {
			tf.setText("");
		} else {
			tf.setText(value);
		}
	}

	/**
	* After all input fields have been added, finishes panel construction.
	* In particular, adds a button "Add attribute" if permitted by the argument
	* mayAddAttributes.
	*/
	public void finishConstruction(boolean mayAddAttributes) {
		setLayout(new BorderLayout());
		if (mainP.getComponentCount() > 10) {
			ScrollPane scp = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
			scp.add(mainP);
			add(scp, "Center");
		} else {
			add(mainP, "Center");
		}
		if (mayAddAttributes) {
			makeButtonPanel();
			add(buttonPanel, "South");
		}
	}

	/**
	* Constructs the panel with the button "Add attribute"
	*/
	protected void makeButtonPanel() {
		if (buttonPanel != null)
			return;
		buttonPanel = new Panel(new FlowLayout(FlowLayout.CENTER));
		// following string: "Add attribute"
		Button b = new Button(res.getString("Add_attribute"));
		b.setActionCommand("add");
		b.addActionListener(this);
		buttonPanel.add(b);
	}

	/**
	* According to the value of the argument mayAddAttributes, adds or removes
	* the panel with the button "Add attribute"
	*/
	public void setMayAddAttributes(boolean mayAddAttributes) {
		boolean mustPack = false;
		if (mayAddAttributes) {
			makeButtonPanel();
			if (!isAncestorOf(buttonPanel)) {
				add(buttonPanel, "South");
				mustPack = isShowing();
			}
		} else if (buttonPanel != null && isAncestorOf(buttonPanel)) {
			remove(buttonPanel);
			mustPack = isShowing();
		}
		if (mustPack) {
			Window win = null;
			Component c = getParent();
			while (c != null && win == null) {
				if (c instanceof Window) {
					win = (Window) c;
				} else {
					c = c.getParent();
				}
			}
			if (win != null) {
				win.pack();
			}
		}
	}

	/**
	* Gets the text from the specified text field
	*/
	protected String getTextFromField(TextField tf) {
		if (tf == null)
			return null;
		String str = tf.getText();
		if (str == null)
			return null;
		str = str.trim();
		if (str.length() < 1)
			return null;
		return str;
	}

	/**
	* Returns the identifier of the object taken from the corresponding text field
	*/
	public String getObjectId() {
		return getTextFromField(idTF);
	}

	/**
	* Returns the name of the object taken from the corresponding text field
	*/
	public String getObjectName() {
		return getTextFromField(nameTF);
	}

	/**
	* Returns the current number of attributes
	*/
	public int getAttrCount() {
		if (attrTF == null)
			return 0;
		return attrTF.size();
	}

	/**
	* Returns the value of the attribute with the given index
	*/
	public String getAttrValue(int idx) {
		if (idx >= 0 && idx < getAttrCount())
			return getTextFromField((TextField) attrTF.elementAt(idx));
		return null;
	}

	/**
	* Returns the AttrDescr structure with the given index
	*/
	public AttrDescr getAttrDescr(int idx) {
		if (idx >= 0 && idx < getAttrCount())
			return (AttrDescr) attrInfo.elementAt(idx);
		return null;
	}

	/**
	* Reacts to the button "Add attribute"
	*/
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand() != null && e.getActionCommand().equals("add")) {
			Panel p = new Panel(new ColumnLayout());
			// following string:"Adding a new attribute"
			p.add(new Label(res.getString("Adding_a_new"), Label.CENTER));
			Panel pp = new Panel(new BorderLayout());
			pp.add(new Label("Name:"), "West");
			TextField tf = new TextField(20);
			pp.add(tf, "Center");
			p.add(pp);
			pp = new Panel(new BorderLayout());
			// following string:"Type:"
			pp.add(new Label(res.getString("Type_")), "West");
			Choice ch = new Choice();
			// following string:"real number"
			ch.addItem(res.getString("real_number"));
			// following string:"integer number"
			ch.addItem(res.getString("integer_number"));
			// following string:"string"
			ch.addItem(res.getString("string"));
			// following string:"logical"
			ch.addItem(res.getString("logical"));
			// following string:"date/time"
			ch.addItem(res.getString("date_time"));
			char types[] = { 'R', 'I', 'C', 'L', 'T' };
			pp.add(ch, "Center");
			p.add(pp);
			//find a frame for dialog construction
			Frame fr = null;
			Component c = getParent();
			while (fr == null && c != null)
				if (c instanceof Frame) {
					fr = (Frame) c;
				} else if (c instanceof Dialog) {
					Dialog d = (Dialog) c;
					if (d.getParent() != null && (d.getParent() instanceof Frame)) {
						fr = (Frame) d.getParent();
					} else {
						c = null;
					}
				} else {
					c = c.getParent();
				}
			if (fr == null) {
				fr = new Frame();
			}
			// following string:"Define a new attribute"
			OKDialog okd = new OKDialog(fr, res.getString("Define_a_new"), true);
			okd.addContent(p);
			okd.show();
			if (okd.wasCancelled())
				return;
			String name = getTextFromField(tf);
			if (name == null)
				return;
			addAttribute(name, null, types[ch.getSelectedIndex()]);
		}
	}

	/**
	* Checks if attribute values entered by the user correspond to attribute types
	*/
	@Override
	public boolean canClose() {
		err = null;
		if (getAttrCount() < 1)
			return true;
		for (int i = 0; i < getAttrCount(); i++) {
			AttrDescr ad = getAttrDescr(i);
			if (ad.type == AttributeTypes.character || ad.type == AttributeTypes.time) {
				continue;
			}
			String value = getTextFromField((TextField) attrTF.elementAt(i));
			if (value == null) {
				continue;
			}
			if (AttributeTypes.isNumericType(ad.type)) {
				if (value.indexOf(',') >= 0) {
					value = value.replace(',', '.');
					((TextField) attrTF.elementAt(i)).setText(value);
				}
				try {
					float v = Float.valueOf(value).floatValue();
				} catch (NumberFormatException nfe) {
					err = "Illegal value of \"" + ad.name + "\": must be a number!";
					return false;
				}
				if (ad.type == AttributeTypes.integer && value.indexOf('.') >= 0) {
					err = "Illegal value of \"" + ad.name + "\": must be integer!";
					return false;
				}
			} else if (ad.type == AttributeTypes.logical) {
				value = value.toUpperCase();
				if (!value.equals("T") && !value.equals("F")) {
					err = "Illegal value of \"" + ad.name + "\": must be either T or F!";
					return false;
				}
				((TextField) attrTF.elementAt(i)).setText(value);
			}
		}
		return true;
	}

	@Override
	public String getErrorMessage() {
		return err;
	}
}
