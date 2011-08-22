package spade.analysis.tools;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.TextField;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.classification.QualitativeClassifier;
import spade.analysis.system.DisplayProducer;
import spade.analysis.system.ESDACore;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.NotificationLine;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.TImgButton;
import spade.lib.basicwin.TriangleDrawer;
import spade.lib.color.CS;
import spade.lib.color.ColorCanvas;
import spade.lib.color.ColorDlg;
import spade.lib.color.ColorListener;
import spade.lib.lang.Language;
import spade.lib.util.IntArray;
import spade.lib.util.StringUtil;
import spade.vis.action.HighlightListener;
import spade.vis.action.Highlighter;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.dmap.DGeoLayer;
import spade.vis.mapvis.ClassDrawer;
import spade.vis.mapvis.Visualizer;

/**
* The UI of the interactive freehand classifier of objects in a table.
* Constantly updates a dynamic attribute with the values corresponding to the
* classes. This attribute can be visualized and analysed using usual system
* functions.
* ==========================================================================
* last updates:
* ==========================================================================
* => hdz, 03.2004:
*   ---------------
*   - constructor: new (integer) Parameter added at the end to get attribute type
*   - gui modification:
*      (1) Notification line added at the bottom of the gui
*      (2) Function checkInputs() to control the user input against the attribute type
*      (3) apply and close buttons added at the bottom of the gui
*   - changes in add attribute, to make the classifier also construct numerical attributes
* ==========================================================================

*/
public class FreeClassifierUI extends Panel implements HighlightListener, ActionListener, ItemListener, ColorListener, PropertyChangeListener, FocusListener, Destroyable {
	static ResourceBundle res = Language.getTextResource("spade.analysis.tools.Res");
	/**
	* The core, in particular, provides a reference to the supervisor, which
	* propagates object selection events
	*/
	protected ESDACore core = null;
	/**
	* The highlighter corresponding to the object set being classified
	*/
	protected Highlighter hlit = null;
	/**
	* The table containing the dynamic attribute representing the classification
	* results
	*/
	protected DataTable table = null;
	/**
	* The index of the dynamic attribute in the table
	*/
	protected int attrIdx = -1;
	/**
	* The layer corresponding to the table
	*/
	protected DGeoLayer layer = null;
	/**
	* The index of the map the layer belongs to
	*/
	protected int layerMapN = -1;
	/**
	* The main panel, which changes depending on the current number of classes
	*/
	protected Panel mainP = null;
	/**
	* If there are too many classes, the main panel may be included in a scrollpane
	*/
	protected ScrollPane scp = null;
	/**
	* The radio buttons showing what class is currently active
	*/
	protected Vector rbuts = null;
	/**
	* The color canvases showing the class colors
	*/
	protected Vector ccans = null;
	/**
	* The text edit fields for editing class names
	*/
	protected Vector tfields = null;
	/**
	* The labels showing the number of objects in each class
	*/
	protected Vector numLabs = null;
	/**
	* The label for the number of remaining (non-classified) objects
	*/
	protected Label remL = null;
	/**
	* The checkbox for switching on/off class broadcasting
	*/
	protected Checkbox broadcb = null;

	protected CheckboxGroup cbgroup = null;
	/**
	* The button for attaching selected objects to the currently active class
	*/
	protected Button attachBt = null;
	/**
	* The button for attaching remaining (i.e. unclassified) objects to the
	* currently active class
	*/
	protected Button remBt = null;
	/**
	* The values of the dynamic attribute, i.e. class names
	*/
	protected Vector values = null;
	/**
	* Frequencies of the values of the dynamic attribute, i.e. number of objects
	* in each class
	*/
	protected IntArray freq = null;
	/**
	* The number of the unclassified objects
	*/
	protected int remainder = 0;
	/**
	* The classifier used for assigning colors to classes, showing objects
	* on a map, and class broadcasting
	*/
	protected QualitativeClassifier qClassifier = null;
	/**
	* Indicates whether the classifier is currently used for visualization
	* on a map
	*/
	protected boolean usedOnMap = false;
	/**
	* The list of objects of a user-selected class shown to the user
	*/
	protected List objList = null;
	/**
	* The record numbers corresponding to the elements of the list of objects
	* shown to the user
	*/
	protected IntArray objNums = null;
	/**
	* The dialog used for showing class objects to the user
	*/
	protected OKDialog objShowDlg = null;
	/**
	* the active class
	*/
	protected int classIdx = -1;
	/**
	* Indicates the "destroyed" state
	*/
	protected boolean destroyed = false;
	/**
	* Notification line for user messages  *
	*/
	protected NotificationLine lStatus = null;

	protected int attributeType = 0;
	public static final int NEW_ATTRIBUTE = 0;
	public static final int USE_ATTRIBUTE = 1;
	public static final int EDIT_ATTRIBUTE = 2;
	public static final int NEW_ATTRIBUTE_INT = 10;
	public static final int USE_ATTRIBUTE_INT = 11;
	public static final int EDIT_ATTRIBUTE_INT = 12;

	/**
	* Constructs the UI. Arguments:
	* @param table - the table with the objects to classify
	* @param attrIdx - the index of the resulting dynamic attribute in the table
	* @param layer - the map layer corresponding to the table
	* @param layerMapN - the index of the map the layer belongs to
	* @param core - the system's core. In particular, it provides a reference to
	*               the supervisor, which propagates object selection events
	*/
	/* hdz a new parameter for setting the type of attributes is added, default is true
	 public FreeClassifierUI (DataTable table, int attrIdx,
	                          DGeoLayer layer, int layerMapN,
	                          ESDACore core) {*/
	public FreeClassifierUI(DataTable table, int attrIdx, DGeoLayer layer, int layerMapN, ESDACore core, int attributeType) {
		this.attributeType = attributeType;
		this.table = table;
		this.attrIdx = attrIdx;
		this.layer = layer;
		this.layerMapN = layerMapN;
		this.core = core;
		if (table == null || attrIdx < 0 || attrIdx >= table.getAttrCount() || core == null)
			return;
		hlit = core.getHighlighterForSet(table.getEntitySetIdentifier());
		if (hlit == null)
			return;
		values = new Vector(20, 10);
		freq = new IntArray(20, 10);
		for (int i = 0; i < table.getDataItemCount(); i++) {
			String val = table.getAttrValueAsString(attrIdx, i);
			if (val == null) {
				continue;
			}
			int idx = StringUtil.indexOfStringInVectorIgnoreCase(val, values);
			if (idx >= 0) {
				freq.setElementAt(freq.elementAt(idx) + 1, idx);
			} else {
				values.addElement(val);
				freq.addElement(1);
			}
		}
		if (values.size() < 1) {
			String attributeName = "1";
			if (attributeType < 10) {
				attributeName = res.getString("class") + " " + attributeName;
			}
			values.addElement(attributeName);
			//values.addElement(res.getString("class")+" 1");
			freq.addElement(0);
		} else {
			qClassifier = new QualitativeClassifier();
			qClassifier.setTable(table);
			Vector attr = new Vector(1, 1);
			attr.addElement(table.getAttributeId(attrIdx));
			qClassifier.setAttributes(attr);
			qClassifier.setAttributeTypeNumerical(attributeType > 9);
			if (layer != null) {
				DisplayProducer dpr = core.getDisplayProducer();
				dpr.displayOnMap(qClassifier, "qualitative_colour", table, attr, layer, core.getUI().getMapViewer(layerMapN));
				usedOnMap = true;
			}
			//ensure that the order of the classes is the same as in the classifier
			Vector vals1 = new Vector(qClassifier.getNClasses(), 1);
			IntArray freq1 = new IntArray(qClassifier.getNClasses(), 1);
			for (int i = 0; i < qClassifier.getNClasses(); i++) {
				String name = qClassifier.getClassName(i);
				vals1.addElement(name);
				int idx = StringUtil.indexOfStringInVectorIgnoreCase(name, values);
				if (idx >= 0) {
					freq1.addElement(freq.elementAt(idx));
				} else {
					freq1.addElement(0);
				}
			}
			values = vals1;
			freq = freq1;
		}
		int ncl = values.size() + 5;
		if (ncl < 10) {
			ncl = 10;
		}
		mainP = new Panel(new ColumnLayout());
		rbuts = new Vector(ncl, 10);
		ccans = new Vector(ncl, 10);
		tfields = new Vector(ncl, 10);
		numLabs = new Vector(ncl, 10);
		cbgroup = new CheckboxGroup();
		remainder = table.getDataItemCount();
		for (int i = 0; i < values.size(); i++) {
			Panel p = new Panel(new BorderLayout());
			Panel pp = new Panel(new GridLayout(1, 2));
			Checkbox cb = new Checkbox("", i == 0, cbgroup);
			rbuts.addElement(cb);
			pp.add(cb);
			ColorCanvas cc = new ColorCanvas();
			if (qClassifier != null) {
				cc.setColor(qClassifier.getClassColor(i));
			} else {
				cc.setColor(CS.getNiceColor(0));
			}
			cc.setActionCommand(String.valueOf(i));
			cc.setActionListener(this);
			ccans.addElement(cc);
			pp.add(cc);
			p.add(pp, BorderLayout.WEST);
			TextField tf = new TextField((String) values.elementAt(i), 20);
			tf.addActionListener(this);
			tf.addFocusListener(this);
			p.add(tf, BorderLayout.CENTER);
			tfields.addElement(tf);
			Label l = new Label(freq.elementAt(i) + " " + res.getString("objects"));
			p.add(l, BorderLayout.EAST);
			numLabs.addElement(l);
			mainP.add(p);
			remainder -= freq.elementAt(i);
		}
		setLayout(new BorderLayout());
		add(new Label(table.getAttributeName(attrIdx), Label.CENTER), BorderLayout.NORTH);
		if (values.size() > 10) {
			scp = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
			scp.add(mainP);
			add(scp, BorderLayout.CENTER);
		} else {
			add(mainP, BorderLayout.CENTER);
		}
		Panel bp = new Panel(new ColumnLayout());
		bp.setBackground(Color.lightGray);
		add(bp, BorderLayout.SOUTH);
		bp.add(new Line(false));
		Panel p = new Panel(new BorderLayout());
		p.add(new Label(res.getString("remainder")), BorderLayout.WEST);
		remBt = new Button(res.getString("rem_to_class"));
		remBt.setActionCommand("rem_to_class");
		remBt.addActionListener(this);
		remBt.setEnabled(remainder > 0);
		Panel fp = new Panel(new FlowLayout(FlowLayout.CENTER));
		fp.add(remBt);
		p.add(fp, BorderLayout.CENTER);
		remL = new Label(remainder + " " + res.getString("objects"));
		p.add(remL, BorderLayout.EAST);
		bp.add(p);
		Panel pp = new Panel(new FlowLayout(FlowLayout.CENTER, 5, 0));
		TriangleDrawer td = new TriangleDrawer(TriangleDrawer.N);
		td.setPreferredSize(14, 14);
		td.setMargins(2, 1);
		TImgButton ib = new TImgButton(td);
		pp.add(ib);
		ib.setActionCommand("up");
		ib.addActionListener(this);
		td = new TriangleDrawer(TriangleDrawer.S);
		td.setPreferredSize(14, 14);
		td.setMargins(2, 1);
		ib = new TImgButton(td);
		pp.add(ib);
		ib.setActionCommand("down");
		ib.addActionListener(this);
		bp.add(new Label(""));
		bp.add(pp);
		p = new Panel(new GridLayout(2, 2, 20, 5));
		attachBt = new Button(res.getString("sel_to_class"));
		attachBt.setActionCommand("sel_to_class");
		attachBt.addActionListener(this);
		Vector selected = hlit.getSelectedObjects();
		if (selected == null || selected.size() < 1) {
			attachBt.setEnabled(false);
		}
		p.add(attachBt);
		Button b = new Button(res.getString("show_objects"));
		b.setActionCommand("show_objects");
		b.addActionListener(this);
		p.add(b);
		b = new Button(res.getString("add_class"));
		b.setActionCommand("add_class");
		b.addActionListener(this);
		p.add(b);
		b = new Button(res.getString("remove_class"));
		b.setActionCommand("remove_class");
		b.addActionListener(this);
		p.add(b);
		pp = new Panel(new FlowLayout(FlowLayout.CENTER));
		pp.add(p);
		bp.add(pp);
		broadcb = new Checkbox(res.getString("broadcast"), false);
		broadcb.addItemListener(this);
		bp.add(broadcb);
		//start listening to events
		hlit.addHighlightListener(this);
		core.getSupervisor().addPropertyChangeListener(this);
		if (qClassifier != null) {
			qClassifier.addPropertyChangeListener(this);
		}
		if (layer != null) {
			layer.addPropertyChangeListener(this);
		}
		table.addPropertyChangeListener(this);
		Panel pfl = new Panel(new GridLayout(1, 2, 20, 5));
		b = new Button(res.getString("apply"));
		b.setActionCommand("apply");
		b.addActionListener(this);
		pfl.add(b);
		b = new Button(res.getString("close"));
		b.setActionCommand("close");
		b.addActionListener(this);
		pfl.add(b);
		pp = new Panel(new FlowLayout(FlowLayout.CENTER));
		pp.add(pfl);
		bp.add(pp);

		lStatus = new NotificationLine("");
		bp.add(lStatus);
		checkInputs();
	}

	/**
	 * default constructor with textual attributes
	 * **/
	public FreeClassifierUI(DataTable table, int attrIdx, DGeoLayer layer, int layerMapN, ESDACore core) {
		this(table, attrIdx, layer, layerMapN, core, 0);
	}

	/**
	* Returns the entity set identifier of its table
	*/
	public String getEntitySetIdentifier() {
		return table.getEntitySetIdentifier();
	}

	/**
	* Notification about change of the set of objects to be transiently
	* highlighted. Does nothing.
	*/
	@Override
	public void highlightSetChanged(Object source, String setId, Vector highlighted) {
	}

	/**
	* Notification about change of the set of objects to be selected (durably
	* highlighted). Enables or disables the button for attaching objects to the
	* currently active class.
	*/
	@Override
	public void selectSetChanged(Object source, String setId, Vector selected) {
		attachBt.setEnabled(selected != null && selected.size() > 0);
	}

	/**
	* Reacts to buttons and text fields
	*/
	@Override
	public void actionPerformed(ActionEvent e) {
		checkClassNames();
		if (e.getSource() instanceof TextField) {
			checkInputs();
			return;
		}
		String cmd = e.getActionCommand();
		if (cmd == null)
			return;

		if (e.getSource() instanceof ColorCanvas) {
			int idx = -1;
			try {
				idx = Integer.valueOf(cmd).intValue();
			} catch (NumberFormatException nfe) {
				return;
			}
			if (idx < 0)
				return;
			((Checkbox) rbuts.elementAt(idx)).setState(true);
			((TextField) tfields.elementAt(idx)).requestFocus();
			classIdx = idx;
			ColorDlg cDlg = new ColorDlg(CManager.getAnyFrame(this));
			cDlg.setTitle((String) values.elementAt(idx));
			cDlg.selectColor(this, this, ((ColorCanvas) ccans.elementAt(idx)).getColor(), false);
		} else {
			// close button
			if (cmd.equals("close")) {
				if (!checkInputs())
					return;
				Window win = CManager.getWindow(this);
				win.dispose();
				win = null;
				return;
			}
			if (cmd.equals("apply")) {
				if (!checkInputs())
					return;
				if (qClassifier != null) {
					qClassifier.notifyClassesChange();
					qClassifier.notifyColorsChange();
				}
			}
			if (cmd.equals("sel_to_class")) {
				attachSelectionToActiveClass();
			} else if (cmd.equals("rem_to_class")) {
				attachRemainderToActiveClass();
			} else if (cmd.equals("add_class")) {
				addClass();
			} else if (cmd.equals("remove_class")) {
				if (!checkInputs())
					return;
				removeActiveClass();
			} else if (cmd.equals("show_objects")) {
				showClassObjects();
			} else if (cmd.equals("remove_object")) {
				removeObjectFromClass();
			} else if (cmd.equals("remove_all")) {
				clearClass();
			} else if (cmd.equals("up") || cmd.equals("down")) {
				if (getActiveClassIndex() < 0)
					return;
				if (cmd.equals("up")) {
					classUp(classIdx);
				} else {
					classUp(classIdx + 1);
				}
			}
		}
	}

	/**
	* Makes the table notify about changes of values in the column with the
	* classification results
	*/
	protected void notifyTableChange() {
		Vector attr = new Vector(1, 1);
		attr.addElement(table.getAttributeId(attrIdx));
		table.notifyPropertyChange("values", null, attr);
	}

	/**
	* Checks whether the user has changed any of the class names
	*/
	protected void checkClassNames() {
		int nNewNames = 0;
		String newNames[] = new String[values.size()];
		for (int i = 0; i < values.size(); i++) {
			String oldVal = (String) values.elementAt(i);
			newNames[i] = null;
			TextField tf = (TextField) tfields.elementAt(i);
			String newVal = tf.getText();
			if (newVal != null) {
				newVal = newVal.trim();
			}
			if (newVal == null || newVal.length() < 1) {
				tf.setText(oldVal);
				continue;
			}
			if (newVal.equals(oldVal)) {
				continue;
			}
			newNames[i] = newVal;
			++nNewNames;
		}
		if (nNewNames < 1)
			return;
		//ensure that there are no repeated names
		for (int i = values.size() - 1; i >= 0; i--)
			if (newNames[i] != null) {
				boolean same = false;
				for (int j = 0; j < values.size() && !same; j++)
					if (j != i)
						if (newNames[j] != null) {
							same = newNames[i].equalsIgnoreCase(newNames[j]);
						} else {
							same = newNames[i].equalsIgnoreCase((String) values.elementAt(j));
						}
				if (same) {
					newNames[i] = null;
					TextField tf = (TextField) tfields.elementAt(i);
					tf.setText((String) values.elementAt(i));
					--nNewNames;
				}
			}
		if (nNewNames < 1)
			return;
		boolean dataChanged = false;
		for (int i = 0; i < table.getDataItemCount(); i++) {
			String val = table.getAttrValueAsString(attrIdx, i);
			if (val == null) {
				continue;
			}
			int idx = StringUtil.indexOfStringInVectorIgnoreCase(val, values);
			if (idx < 0) {
				continue;
			}
			if (newNames[idx] == null) {
				continue;
			}
			DataRecord rec = table.getDataRecord(i);
			rec.setAttrValue(newNames[idx], attrIdx);
			dataChanged = true;
		}
		if (qClassifier != null) {
			for (int i = 0; i < values.size(); i++)
				if (newNames[i] != null) {
					qClassifier.setClassName(newNames[i], i);
				}
		}
		for (int i = 0; i < values.size(); i++)
			if (newNames[i] != null) {
				values.setElementAt(newNames[i], i);
			}
		if (dataChanged) {
			notifyTableChange();
		} else if (qClassifier != null) {
			qClassifier.notifyClassesChange();
		}
	}

	/**
	* Detects the active class and assigns its index to the variable classIdx
	*/
	protected int getActiveClassIndex() {
		classIdx = -1;
		for (int i = 0; i < rbuts.size() && classIdx < 0; i++) {
			Checkbox cb = (Checkbox) rbuts.elementAt(i);
			if (cb.getState()) {
				classIdx = i;
			}
		}
		return classIdx;
	}

	/**
	* Creates the qualitative classifier.
	*/
	protected void constructClassifier(boolean showOnMap) {
		if (qClassifier != null)
			return;
		Vector attr = new Vector(1, 1);
		attr.addElement(table.getAttributeId(attrIdx));
		if (showOnMap && layer != null) {
			qClassifier = new QualitativeClassifier();
			qClassifier.setAttributeTypeNumerical(attributeType > 9);
			DisplayProducer dpr = core.getDisplayProducer();
			dpr.displayOnMap(qClassifier, "qualitative_colour", table, attr, layer, core.getUI().getMapViewer(layerMapN));
			usedOnMap = true;
		} else {
			qClassifier = new QualitativeClassifier(table, (String) attr.elementAt(0));
			qClassifier.setAttributeTypeNumerical(attributeType > 9);
		}
		Vector colors = new Vector(values.size(), 10);
		for (int i = 0; i < ccans.size(); i++) {
			ColorCanvas cc = (ColorCanvas) ccans.elementAt(i);
			colors.addElement(cc.getColor());
		}
		qClassifier.setClasses(values, colors);
		qClassifier.addPropertyChangeListener(this);
	}

	/**
	* Attaches the current selection to the active class
	*/
	protected void attachSelectionToActiveClass() {
		//find the active class
		if (getActiveClassIndex() < 0)
			return;
		Vector selected = hlit.getSelectedObjects();
		if (selected == null || selected.size() < 1)
			return;
		hlit.clearSelection(this);
		boolean showOnMap = qClassifier == null && remainder >= table.getDataItemCount();
		boolean changed = false;
		String className = (String) values.elementAt(classIdx);
		for (int i = 0; i < table.getDataItemCount(); i++)
			if (StringUtil.isStringInVectorIgnoreCase(table.getDataItemId(i), selected)) {
				String val = table.getAttrValueAsString(attrIdx, i);
				int idx = -1;
				if (val != null) {
					idx = StringUtil.indexOfStringInVectorIgnoreCase(val, values);
					if (idx == classIdx) {
						continue;
					}
				}
				DataRecord rec = table.getDataRecord(i);
				rec.setAttrValue(className, attrIdx);
				if (idx >= 0) {
					freq.setElementAt(freq.elementAt(idx) - 1, idx);
				}
				freq.setElementAt(freq.elementAt(classIdx) + 1, classIdx);
				changed = true;
			}
		if (!changed)
			return;
		notifyTableChange();
		remainder = table.getDataItemCount();
		for (int i = 0; i < values.size(); i++) {
			Label l = (Label) numLabs.elementAt(i);
			int n = freq.elementAt(i);
			remainder -= n;
			l.setText(n + " " + res.getString("objects"));
			CManager.validateAll(l);
		}
		remL.setText(remainder + " " + res.getString("objects"));
		remBt.setEnabled(remainder > 0);
		CManager.validateAll(remL);
		if (showOnMap) {
			constructClassifier(showOnMap);
		}
	}

	/**
	* Class reordering: moves the class with the specified index up
	*/
	protected void classUp(int idx) {
		if (idx < 1 || idx >= values.size())
			return;
		mainP.setVisible(false);
		Component comp = mainP.getComponent(idx);
		mainP.remove(idx);
		mainP.add(comp, idx - 1);
		mainP.setVisible(true);
		CManager.validateAll(comp);
		Object obj = values.elementAt(idx);
		values.removeElementAt(idx);
		values.insertElementAt(obj, idx - 1);
		obj = rbuts.elementAt(idx);
		rbuts.removeElementAt(idx);
		rbuts.insertElementAt(obj, idx - 1);
		obj = ccans.elementAt(idx);
		ccans.removeElementAt(idx);
		ccans.insertElementAt(obj, idx - 1);
		obj = tfields.elementAt(idx);
		tfields.removeElementAt(idx);
		tfields.insertElementAt(obj, idx - 1);
		obj = numLabs.elementAt(idx);
		numLabs.removeElementAt(idx);
		numLabs.insertElementAt(obj, idx - 1);
		int k = freq.elementAt(idx);
		freq.removeElementAt(idx);
		freq.insertElementAt(k, idx - 1);
		Vector colors = new Vector(values.size(), 10);
		for (int i = 0; i < ccans.size(); i++) {
			ColorCanvas cc = (ColorCanvas) ccans.elementAt(i);
			colors.addElement(cc.getColor());
		}
		qClassifier.removePropertyChangeListener(this);
		qClassifier.setClasses(values, colors);
		qClassifier.addPropertyChangeListener(this);
	}

	/**
	* Attaches the remaining (i.e. unclassified) objects to the active class
	*/
	protected void attachRemainderToActiveClass() {
		if (remainder < 1)
			return;
		//find the active class
		if (getActiveClassIndex() < 0)
			return;
		boolean showOnMap = qClassifier == null && remainder >= table.getDataItemCount();
		boolean changed = false;
		String className = (String) values.elementAt(classIdx);
		for (int i = 0; i < table.getDataItemCount() && remainder > 0; i++) {
			String val = table.getAttrValueAsString(attrIdx, i);
			if (val != null) {
				continue;
			}
			DataRecord rec = table.getDataRecord(i);
			rec.setAttrValue(className, attrIdx);
			freq.setElementAt(freq.elementAt(classIdx) + 1, classIdx);
			--remainder;
			changed = true;
		}
		if (!changed)
			return;
		notifyTableChange();
		Label l = (Label) numLabs.elementAt(classIdx);
		l.setText(freq.elementAt(classIdx) + " " + res.getString("objects"));
		remL.setText("0 " + res.getString("objects"));
		remBt.setEnabled(false);
		CManager.validateAll(remL);
		if (showOnMap) {
			constructClassifier(showOnMap);
		}
	}

	protected void addClass() {
		String name = String.valueOf(values.size() + 1);
		if (attributeType < 10) {
			name = res.getString("class") + " " + name;
		}
		if (attributeType < 10) { //for numerical
			while (StringUtil.isStringInVectorIgnoreCase(name, values)) {
				name += "x";
			}
		} else {
			while (StringUtil.isStringInVectorIgnoreCase(name, values)) {
				name += "0";
			}
		}
		values.addElement(name);
		freq.addElement(0);
		if (qClassifier != null) {
			qClassifier.addClass(name);
			qClassifier.notifyClassesChange();
		}
		int idx = values.size() - 1;
		Panel p = new Panel(new BorderLayout());
		Panel pp = new Panel(new GridLayout(1, 2));
		Checkbox cb = new Checkbox("", true, cbgroup);
		rbuts.addElement(cb);
		pp.add(cb);
		ColorCanvas cc = new ColorCanvas();
		cc.setColor((qClassifier != null) ? qClassifier.getClassColor(idx) : CS.getNiceColorExt(idx));
		cc.setActionCommand(String.valueOf(idx));
		cc.setActionListener(this);
		ccans.addElement(cc);
		pp.add(cc);
		p.add(pp, BorderLayout.WEST);
		TextField tf = new TextField(name, 20);
		tf.addActionListener(this);
		tf.addFocusListener(this);
		p.add(tf, BorderLayout.CENTER);
		tfields.addElement(tf);
		Label l = new Label("0 " + res.getString("objects"));
		p.add(l, BorderLayout.EAST);
		numLabs.addElement(l);
		int oldH = mainP.getPreferredSize().height;
		setVisible(false);
		mainP.add(p);
		if (values.size() > 10 && scp == null) {
			scp = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
			remove(mainP);
			scp.add(mainP);
			add(scp, BorderLayout.CENTER);
		}
		setVisible(true);
		CManager.validateAll(mainP);
		int dh = mainP.getPreferredSize().height - oldH;
		if (dh > 0) {
			Window win = CManager.getWindow(this);
			if (win != null) {
				Dimension d = win.getSize(), ss = getToolkit().getScreenSize();
				if (d.height + dh < ss.height * 3 / 4) {
					win.setSize(d.width, d.height + dh);
					CManager.validateFully(p);
				}
			}
		}
	}

	protected void removeActiveClass() {
		if (values.size() < 2)
			return; //the last class cannot be removed
		//find the active class
		if (getActiveClassIndex() < 0)
			return;
		if (freq.elementAt(classIdx) > 0) {
			String valToRemove = (String) values.elementAt(classIdx);
			for (int i = 0; i < table.getDataItemCount(); i++) {
				String val = table.getAttrValueAsString(attrIdx, i);
				if (val == null) {
					continue;
				}
				if (!val.equalsIgnoreCase(valToRemove)) {
					continue;
				}
				DataRecord rec = table.getDataRecord(i);
				rec.setAttrValue(null, attrIdx);
			}
			remainder += freq.elementAt(classIdx);
			freq.setElementAt(0, classIdx);
			notifyTableChange();
		}
		values.removeElementAt(classIdx);
		freq.removeElementAt(classIdx);
		rbuts.removeElementAt(classIdx);
		ccans.removeElementAt(classIdx);
		tfields.removeElementAt(classIdx);
		numLabs.removeElementAt(classIdx);
		int oldH = mainP.getPreferredSize().height;
		setVisible(false);
		mainP.remove(classIdx);
		remBt.setEnabled(remainder > 0);
		remL.setText(remainder + " " + res.getString("objects"));
		CManager.invalidateAll(remL);
		setVisible(true);
		CManager.validateAll(mainP);
		int dh = oldH - mainP.getPreferredSize().height;
		if (dh > 0) {
			Window win = CManager.getWindow(this);
			if (win != null) {
				Dimension d = win.getSize();
				win.setSize(d.width, d.height - dh);
				CManager.validateFully(mainP);
			}
		}
		cbgroup.setSelectedCheckbox((Checkbox) rbuts.elementAt(0));
		for (int i = 0; i < ccans.size(); i++) {
			ColorCanvas cc = (ColorCanvas) ccans.elementAt(i);
			cc.setActionCommand(String.valueOf(i));
		}
		if (qClassifier != null) {
			qClassifier.removeClass(classIdx);
			qClassifier.notifyClassesChange();
		}
	}

	protected void showClassObjects() {
		//find the active class
		if (getActiveClassIndex() < 0)
			return;
		String name = (String) values.elementAt(classIdx);
		Frame fr = CManager.getAnyFrame(this);
		if (freq.elementAt(classIdx) < 1) {
			OKDialog okd = new OKDialog(fr, res.getString("no_objects"), false);
			okd.addContent(new Label(res.getString("no_objects_in_class") + " " + name + "!"));
			okd.show();
			return;
		}
		objList = new List(10);
		objNums = new IntArray(freq.elementAt(classIdx), 1);
		for (int i = 0; i < table.getDataItemCount(); i++) {
			String val = table.getAttrValueAsString(attrIdx, i);
			if (val == null) {
				continue;
			}
			if (val.equalsIgnoreCase(name)) {
				DataRecord rec = table.getDataRecord(i);
				String str = rec.getId(), str1 = rec.getName();
				if (str1 != null && !str1.equals(str)) {
					str += " " + str1;
				}
				objList.add(str);
				objNums.addElement(i);
			}
		}
		Panel p = new Panel(new BorderLayout());
		p.add(objList, BorderLayout.CENTER);
		Panel pp = new Panel(new ColumnLayout());
		Button b = new Button(res.getString("remove_object"));
		b.setActionCommand("remove_object");
		b.addActionListener(this);
		Panel fp = new Panel(new FlowLayout(FlowLayout.CENTER, 0, 2));
		fp.add(b);
		pp.add(fp);
		b = new Button(res.getString("remove_all"));
		b.setActionCommand("remove_all");
		b.addActionListener(this);
		fp = new Panel(new FlowLayout(FlowLayout.CENTER, 0, 2));
		fp.add(b);
		pp.add(fp);
		pp.add(new Line(false));
		p.add(pp, BorderLayout.SOUTH);
		objShowDlg = new OKDialog(fr, name, false);
		objShowDlg.addContent(p);
		objShowDlg.show();
		objShowDlg = null;
		objList = null;
		objNums = null;
	}

	/**
	* Reacts to pressing button "Remove object" in the object viewing dialog
	*/
	protected void removeObjectFromClass() {
		if (objShowDlg == null || objList == null || objNums == null || objNums.size() < 1 || classIdx < 0)
			return;
		int objIdx = objList.getSelectedIndex();
		if (objIdx < 0 || objIdx >= objNums.size())
			return;
		DataRecord rec = table.getDataRecord(objNums.elementAt(objIdx));
		rec.setAttrValue(null, attrIdx);
		notifyTableChange();
		objNums.removeElementAt(objIdx);
		objList.remove(objIdx);
		if (objNums.size() < 1) {
			objShowDlg.dispose();
		}
		int n = freq.elementAt(classIdx) - 1;
		freq.setElementAt(n, classIdx);
		++remainder;
		remBt.setEnabled(true);
		Label l = (Label) numLabs.elementAt(classIdx);
		l.setText(n + " " + res.getString("objects"));
		remL.setText(remainder + " " + res.getString("objects"));
		CManager.validateAll(remL);
	}

	/**
	* Reacts to pressing button "Remove all objects" in the object viewing dialog
	*/
	protected void clearClass() {
		if (objShowDlg == null || objList == null || objNums == null || objNums.size() < 1 || classIdx < 0)
			return;
		objShowDlg.dispose();
		for (int i = 0; i < objNums.size(); i++) {
			DataRecord rec = table.getDataRecord(objNums.elementAt(i));
			rec.setAttrValue(null, attrIdx);
		}
		notifyTableChange();
		remainder += freq.elementAt(classIdx);
		freq.setElementAt(0, classIdx);
		remBt.setEnabled(remainder > 0);
		Label l = (Label) numLabs.elementAt(classIdx);
		l.setText("0 " + res.getString("objects"));
		remL.setText(remainder + " " + res.getString("objects"));
		CManager.validateAll(remL);
	}

	/**
	* Reacts to changing the current color in the color selection dialog
	*/
	public void colorChanged(Color color, Object selector) {
		if (classIdx < 0 || color == null)
			return;
		((ColorCanvas) ccans.elementAt(classIdx)).setColor(color);
		if (qClassifier != null) {
			qClassifier.removePropertyChangeListener(this);
			qClassifier.setColorForClass(classIdx, color, true);
			qClassifier.addPropertyChangeListener(this);
		}
	}

	/**
	* Reacts to the "broadcast" checkbox
	*/
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource().equals(broadcb))
			if (broadcb.getState()) {
				//check if there is at least one classified object
				if (remainder == table.getDataItemCount()) {
					broadcb.setState(false);
					return;
				}
				if (qClassifier == null) {
					constructClassifier(false);
				}
				core.getSupervisor().setObjectColorer(qClassifier);
			} else {
				if (qClassifier == null)
					return;
				core.getSupervisor().removeObjectColorer(qClassifier);
			}
	}

	/**
	* 1) Reacts to supervisor's notifications about changing colors. In particular,
	* checks whether the source of the colors (i.e. the current classifier in the
	* supervisor) has changed. If so, sets the state of the checkbox depending
	* on whether or not this classifier is currently used in the supervisor.
	* 2) Reacts to changes of colors in the classifier, which occurred not in
	* this UI but through other controls
	* 3) Reacts to the classifier being destroyed, e.g. when the visualizer is
	* removed from the layer.
	* 3) Reacts to the layer's visualization changes. If the same attribute
	* is visualized again using the qualitative classification method, replaces
	* its old classifier with the new classifier received from the layer.
	* 4) Reacts to data changes in the table, in particular, in the column used
	* for the classification results.
	*/
	public void propertyChange(PropertyChangeEvent pce) {
		if (pce.getSource().equals(core.getSupervisor())) {
			if (qClassifier != null && pce.getPropertyName().equals(spade.analysis.system.Supervisor.eventObjectColors)) {
				broadcb.setState(qClassifier.equals(core.getSupervisor().getObjectColorer()));
			}
		} else if (pce.getSource().equals(qClassifier)) {
			if (pce.getPropertyName().equals("colors")) {
				for (int i = 0; i < qClassifier.getNClasses(); i++) {
					((ColorCanvas) ccans.elementAt(i)).setColor(qClassifier.getClassColor(i));
				}
			} else if (pce.getPropertyName().equals("destroyed")) {
				usedOnMap = false;
				qClassifier.removePropertyChangeListener(this);
				qClassifier = null;
			}
		} else if (pce.getSource().equals(layer)) {
			if (pce.getPropertyName().equals("Visualization")) {
				if (!layer.hasThematicData(table))
					return;
				Visualizer vis = (Visualizer) pce.getNewValue();
				if (vis == null || !(vis instanceof ClassDrawer))
					return;
				ClassDrawer cld = (ClassDrawer) vis;
				if (cld.getClassifier() == null || cld.getClassifier().equals(qClassifier))
					return;
				if (!(cld.getClassifier() instanceof QualitativeClassifier))
					return;
				QualitativeClassifier qc = (QualitativeClassifier) cld.getClassifier();
				if (!qc.getAttrId().equals(table.getAttributeId(attrIdx)))
					return;
				if (qClassifier != null) {
					qClassifier.removePropertyChangeListener(this);
					if (!usedOnMap) {
						qClassifier.destroy();
					}
					if (broadcb.getState()) {
						core.getSupervisor().removeObjectColorer(qClassifier);
						broadcb.setState(false);
					}
				}
				qClassifier = qc;
				usedOnMap = true;
				boolean changed = values.size() != qClassifier.getNClasses();
				for (int i = 0; i < values.size() && !changed; i++) {
					changed = !qClassifier.getClassName(i).equals(values.elementAt(i)) || !qClassifier.getClassColor(i).equals(((ColorCanvas) ccans.elementAt(i)).getColor());
				}
				if (changed) {
					Vector colors = new Vector(values.size(), 10);
					for (int i = 0; i < ccans.size(); i++) {
						ColorCanvas cc = (ColorCanvas) ccans.elementAt(i);
						colors.addElement(cc.getColor());
					}
					qClassifier.setClasses(values, colors);
				}
				qClassifier.addPropertyChangeListener(this);
			}
		} else if (pce.getSource().equals(table)) {
			if (pce.getPropertyName().equals("destroyed") || pce.getPropertyName().equals("structure_complete")) {
				destroy();
				return;
			}
			if (pce.getPropertyName().equals("names") || pce.getPropertyName().equals("new_attributes"))
				return;
			boolean changed = false;
			if (pce.getPropertyName().equals("data_added") || pce.getPropertyName().equals("data_removed") || pce.getPropertyName().equals("data_updated")) {
				changed = true;
			} else if (pce.getPropertyName().equals("values")) {
				Vector attr = (Vector) pce.getNewValue();
				if (attr == null || attr.size() < 1)
					return;
				changed = attr.contains(table.getAttributeId(attrIdx));
			}
			if (!changed)
				return;
			remainder = table.getDataItemCount();
			for (int i = 0; i < freq.size(); i++) {
				freq.setElementAt(0, i);
			}
			for (int i = 0; i < table.getDataItemCount(); i++) {
				String val = table.getAttrValueAsString(attrIdx, i);
				if (val == null) {
					continue;
				}
				int idx = StringUtil.indexOfStringInVectorIgnoreCase(val, values);
				if (idx >= 0) {
					freq.setElementAt(freq.elementAt(idx) + 1, idx);
				} else {
					values.addElement(val);
					freq.addElement(1);
				}
				--remainder;
			}
			for (int i = 0; i < numLabs.size(); i++) {
				Label l = (Label) numLabs.elementAt(i);
				l.setText(freq.elementAt(i) + " " + res.getString("objects"));
				l.invalidate();
				l.getParent().invalidate();
			}
			mainP.invalidate();
			remL.setText(remainder + " " + res.getString("objects"));
			remBt.setEnabled(remainder > 0);
			CManager.validateAll(remL);
			if (tfields.size() == values.size())
				return;
			int oldH = mainP.getPreferredSize().height;
			setVisible(false);
			for (int i = tfields.size(); i < values.size(); i++) {
				if (qClassifier != null && qClassifier.getNClasses() <= i) {
					qClassifier.addClass((String) values.elementAt(i));
				}
				Panel p = new Panel(new BorderLayout());
				Panel pp = new Panel(new GridLayout(1, 2));
				Checkbox cb = new Checkbox("", true, cbgroup);
				rbuts.addElement(cb);
				pp.add(cb);
				ColorCanvas cc = new ColorCanvas();
				cc.setColor((qClassifier != null) ? qClassifier.getClassColor(i) : CS.getNiceColorExt(i));
				cc.setActionCommand(String.valueOf(i));
				cc.setActionListener(this);
				ccans.addElement(cc);
				pp.add(cc);
				p.add(pp, BorderLayout.WEST);
				TextField tf = new TextField((String) values.elementAt(i), 20);
				tf.addActionListener(this);
				tf.addFocusListener(this);
				p.add(tf, BorderLayout.CENTER);
				tfields.addElement(tf);
				Label l = new Label(freq.elementAt(i) + " " + res.getString("objects"));
				p.add(l, BorderLayout.EAST);
				numLabs.addElement(l);
				mainP.add(p);
			}
			if (values.size() > 10 && scp == null) {
				scp = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
				remove(mainP);
				scp.add(mainP);
				add(scp, BorderLayout.CENTER);
			}
			setVisible(true);
			CManager.validateAll(mainP);
			int dh = mainP.getPreferredSize().height - oldH;
			if (dh > 0) {
				Window win = CManager.getWindow(this);
				if (win != null) {
					Dimension d = win.getSize(), ss = getToolkit().getScreenSize();
					if (d.height + dh < ss.height * 3 / 4) {
						win.setSize(d.width, d.height + dh);
						CManager.validateFully(mainP);
					}
				}
			}
		}
	}

	/**
	* Invoked when a text field with a class name gains the keyboard focus.
	* Makes the corresponding class active, i.e. sets the corresponding radio
	* button in the "checked" state
	*/
	public void focusGained(FocusEvent e) {
		if (e.getSource() instanceof TextField) {
			TextField tf = (TextField) e.getSource();
			int idx = tfields.indexOf(tf);
			if (idx >= 0) {
				((Checkbox) rbuts.elementAt(idx)).setState(true);
				classIdx = idx;
			}
		}
	}

	/**
	 * Invoked when a component loses the keyboard focus.
	 */
	public void focusLost(FocusEvent e) {
	}

	/**
	* Makes necessary operations for destroying, e.g. unregisters from
	* listening highlighting and other events.
	*/
	public void destroy() {
		//stop listening to events
		hlit.removeHighlightListener(this);
		core.getSupervisor().removePropertyChangeListener(this);
		if (qClassifier != null) {
			qClassifier.removePropertyChangeListener(this);
			core.getSupervisor().removeObjectColorer(qClassifier);
			if (!usedOnMap) {
				qClassifier.destroy();
			}
		}
		if (layer != null) {
			layer.removePropertyChangeListener(this);
		}
		table.removePropertyChangeListener(this);
		destroyed = true;
	}

	/**
	* Replies whether is destroyed or not
	*/
	public boolean isDestroyed() {
		return destroyed;
	}

	/**
	 * check of all Text attributes if they should be numerical
	 * @return boolean
	 */
	protected boolean checkInputs() {
		if (attributeType < 10)
			return true;
		if (mainP.getComponents() == null)
			return true;
		TextField tf = null;
		for (int i = 0; i < mainP.getComponents().length; ++i) {
			try {
				if (mainP.getComponent(i) instanceof TextField) {
					tf = ((TextField) mainP.getComponent(i));
					new Float(tf.getText());
				} else if (mainP.getComponent(i) instanceof Panel) {
					Panel p = (Panel) mainP.getComponent(i);
					for (int j = 0; j < p.getComponents().length; ++j) {
						if (p.getComponent(j) instanceof TextField) {
							tf = ((TextField) p.getComponent(j));
							new Float(tf.getText());
						}
					}
				}
				this.lStatus.showDefaultMessage();
			} catch (NumberFormatException nex) {
				this.lStatus.showMessage(res.getString("wrong_classvalue_format"), true);
				/*OKDialog okd=new OKDialog(CManager.getAnyFrame(),res.getString("wrong_input"),false);
				Panel fp=new Panel(new FlowLayout(FlowLayout.CENTER));
				Label label1 = new Label (res.getString("wrong_classvalue_format"));
				fp.add(label1);
				okd.addContent(fp);
				okd.show();*/
				try {
					if (tf != null) {
						tf.requestFocus();
					}
				} catch (Exception ex) {
				}
				return false;
			}
		} //for i
		return true;
	}
}
