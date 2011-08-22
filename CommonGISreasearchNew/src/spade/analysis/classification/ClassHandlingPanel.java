package spade.analysis.classification;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.Supervisor;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.OKDialog;
import spade.lib.color.ColorCanvas;
import spade.lib.lang.Language;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataTable;

/**
* This panel may be added to any class manipulator. It contains controls for
* broadcasting of the classes (i.e. propagating colors of objects among
* the graphical displays) and for saving the classification in the table as
* a new attribute.
*/

public class ClassHandlingPanel extends Panel implements ClassOperator, ActionListener {
	static ResourceBundle res = Language.getTextResource("spade.analysis.classification.Res");
	protected Classifier classifier = null;
	protected Supervisor supervisor = null;
	protected AttributeDataPortion data = null;
	protected int fn[] = null;
	protected String prevClassNames[] = null;
	protected boolean ClassNamesWereEdited = false;

	/**
	* Constructs a ClassHandlingPanel. Needs 1) a reference to the classifier which
	* produces the classes - this must be a TableClassifier; 2) a reference to the
	* supervisor that can be used for event propagation.
	* Returns true if successfully constructed.
	*/
	@Override
	public boolean construct(Classifier classifier, Supervisor supervisor) {
		if (classifier == null || supervisor == null)
			return false;
		if (!(classifier instanceof TableClassifier))
			return false;
		TableClassifier tcl = (TableClassifier) classifier;
		data = tcl.getTable();
		if (data == null)
			return false;
		Vector attr = tcl.getAttributeList();
		if (attr == null || attr.size() < 1)
			return false;
		fn = new int[attr.size()];
		for (int i = 0; i < attr.size(); i++) {
			fn[i] = data.getAttrIndex((String) attr.elementAt(i));
			if (fn[i] < 0)
				return false;
		}
		this.classifier = classifier;
		this.supervisor = supervisor;
		setLayout(new ColumnLayout());
		try {
			Object obj = Class.forName("spade.analysis.classification.ClassBroadcastPanel").newInstance();
			if (obj != null && (obj instanceof ClassOperator) && (obj instanceof Component)) {
				ClassOperator cop = (ClassOperator) obj;
				if (cop.construct(classifier, supervisor)) {
					add((Component) obj);
				}
			}
		} catch (Exception e) {
		}
		// following text: "Add to table"
		Button b = new Button(res.getString("Add_to_table"));
		b.setActionCommand("save");
		b.addActionListener(this);
		Panel p = new Panel(new FlowLayout(FlowLayout.CENTER, 0, 2));
		p.add(b);
		add(p);
		if (data instanceof DataTable) {
			DataTable dTable = (DataTable) data;
			if (dTable.hasDecisionSupporter()) {
				// following text: "Make decision"
				b = new Button(res.getString("Make_decision"));
				p = new Panel(new FlowLayout(FlowLayout.CENTER, 0, 2));
				p.add(b);
				add(p);
				b.addActionListener(this);
				b.setActionCommand("decision");
			}
		}
		return true;
	}

	// save the classification
	@Override
	public void actionPerformed(ActionEvent ae) {
		if (ae.getActionCommand().equals("decision")) {
			if (classifier.getNClasses() < 2)
				return;
			int ncl = classifier.getNClasses();
			String values[] = new String[ncl];
			if (fn != null && fn.length == 1 && data.getAttributeOrigin(fn[0]) == AttributeTypes.evaluate_score) {
				//reverse class order
				for (int i = 0; i < values.length; i++) {
					values[i] = String.valueOf(ncl - i);
				}
			} else {
				for (int i = 0; i < values.length; i++) {
					values[i] = String.valueOf(i + 1);
				}
			}
			String decisionAttr = saveClassesAsAttribute("temporary", values, true);
			((DataTable) data).notifyDecisionColumnAdded(decisionAttr);
		} else if (ae.getActionCommand().equals("save")) {
			saveClasses();
		}
	}

	protected void saveClasses() {
		if (prevClassNames == null || prevClassNames.length != classifier.getNClasses() || !ClassNamesWereEdited) {
			prevClassNames = new String[classifier.getNClasses()];
			for (int i = 0; i < prevClassNames.length; i++) {
				prevClassNames[i] = classifier.getClassName(i);
			}
		}
		Panel p = new Panel();
		p.setLayout(new ColumnLayout());
		if (fn != null) {
			for (int element : fn) {
				p.add(new Label(data.getAttributeName(element), Label.CENTER));
			}
			p.add(new Line(false));
		}
		// following text: "Name of the classification attribute"
		p.add(new Label(res.getString("Name_of_the"), Label.CENTER));
		String attrName = "Classes by ";
		if (fn != null) {
			for (int i = 0; i < fn.length; i++) {
				attrName += ((i == 0) ? "" : ", ") + data.getAttributeName(fn[i]);
			}
		}
		TextField tfn = new TextField(attrName);
		p.add(tfn);
		p.add(new Line(false));
		//following text:  "Colors and names of classes"
		p.add(new Label(res.getString("Colors_and_names_of"), Label.CENTER));
		TextField tf[] = new TextField[classifier.getNClasses()];
		ColorCanvas ccs[] = new ColorCanvas[tf.length];
		for (int i = 0; i < tf.length; i++) {
			Panel pp = new Panel();
			pp.setLayout(new BorderLayout());

			pp.add(ccs[i] = new ColorCanvas(), "West");
			ccs[i].setColor(classifier.getClassColor(i));

			pp.add(tf[i] = new TextField(prevClassNames[i]), "Center");
			p.add(pp);
		}
		p.add(new Line(false));
		boolean OK = true;
		do {
			// following text: "Save classification"
			OKDialog dlg = new OKDialog(CManager.getAnyFrame(this), res.getString("Save_classification"), true);
			dlg.addContent(p);
			dlg.show();
			if (dlg.wasCancelled())
				return;
			OK = true;
			String str = tfn.getText().trim();
			if (str == null || str.length() == 0) {
				tfn.setText(attrName);
				OK = false;
			}
			for (int i = 0; i < tf.length && OK; i++) {
				str = tf[i].getText().trim();
				if (str == null || str.length() == 0) {
					tf[i].setText(prevClassNames[i]);
					OK = false;
				}
			}
		} while (!OK);
		if (!attrName.equals(tfn.getText().trim())) {
			attrName = tfn.getText().trim();
			ClassNamesWereEdited = true;
		}
		for (int i = 0; i < tf.length; i++)
			if (!prevClassNames[i].equals(tf[i].getText().trim())) {
				prevClassNames[i] = tf[i].getText().trim();
				ClassNamesWereEdited = true;
			}
		// now check if all names are numbers
		boolean allNumbers = true;
		for (int i = 0; i < prevClassNames.length && allNumbers; i++)
			if (prevClassNames[i].indexOf('.') >= 0) {
				allNumbers = false;
			} else {
				try {
					int val = Integer.valueOf(prevClassNames[i]).intValue();
				} catch (NumberFormatException nfe) {
					allNumbers = false;
				}
			}
		// ... ask to save as numbers or as strings
		// ...
		saveClassesAsAttribute(attrName, prevClassNames, allNumbers);
	}

	/**
	* Returns the identifier of the new attribute
	*/
	protected String saveClassesAsAttribute(String attrName, String valueNames[], boolean storeAsNumbers) {
		Vector sourceAttr = null;
		if (fn != null) {
			sourceAttr = new Vector(fn.length, 5);
			for (int element : fn) {
				sourceAttr.addElement(data.getAttributeId(element));
			}
		}
		DataTable dTable = (DataTable) data;
		int idx = dTable.addDerivedAttribute(attrName, (storeAsNumbers) ? AttributeTypes.integer : AttributeTypes.character, (storeAsNumbers) ? AttributeTypes.classify_order : AttributeTypes.classify, sourceAttr);
		for (int j = 0; j < data.getDataItemCount(); j++) {
			int n = classifier.getObjectClass(j);
			if (n >= 0) {
				dTable.getDataRecord(j).setAttrValue(valueNames[n], idx);
			}
		}
		// save colors ...
		Attribute attr = dTable.getAttribute(idx);
		Color valueColors[] = new Color[classifier.getNClasses()];
		for (int i = 0; i < valueColors.length; i++) {
			valueColors[i] = classifier.getClassColor(i);
		}
		attr.setValueListAndColors(prevClassNames, valueColors);
		Vector resultAttrs = new Vector(1, 1);
		resultAttrs.addElement(attr.getIdentifier());
		dTable.notifyPropertyChange("new_attributes", null, resultAttrs);
		return attr.getIdentifier();
	}

}
