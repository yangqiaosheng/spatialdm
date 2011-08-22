package spade.analysis.calc;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.lib.basicwin.Arrow;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.PopupManager;
import spade.lib.basicwin.Slider;
import spade.lib.color.ColorCanvas;
import spade.lib.color.ColorDlg;
import spade.lib.color.ColorListener;
import spade.lib.lang.Language;
import spade.lib.util.StringUtil;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.DataTable;
import spade.vis.database.SemanticsManager;
import spade.vis.mapvis.AttrColorHandler;
import ui.AttributeChooser;

/**
* Control Panel for a group of weighted attributes
* allows to manipulate only numeric attributes
* prohibited Attributes are listed in <prohibitedAttributes>
* sends 2 messages:
*   weightsChanged - weights of existing attributes has been changed
*   fnChanged      - set of attributes has been changed
*/
public class WAPanel extends Panel implements ActionListener, ItemListener, PropertyChangeListener, Destroyable, ColorListener {

	static ResourceBundle res = Language.getTextResource("spade.analysis.calc.Res");

	protected AttributeDataPortion dTable = null;
	protected SemanticsManager sm = null;
	/**
	* This vector may contain either identifiers of attributes or the attributes
	* themselves, i.e. instances of spade.vis.database.Attribute
	*/
	protected Vector attr = null;

	protected ActionListener al = null;

	protected Choice delCh = null;
	protected Panel upP = null;
	protected ScrollPane scp = null;
	protected Vector sliders = null, labelsN = null, labelsV = null, arrows = null, ccs = null; // color canvases
	/*
	* preferred slider width
	*/
	protected int prefSlW = 0;
	/**
	* An object that handles colors used to represent attributes
	*/
	protected AttrColorHandler colorHandler = null;
	/**
	* Indicates "destroyed" state. Initially is false.
	*/
	protected boolean destroyed = false;

	public void setAttrColorHandler(AttrColorHandler handler) {
		colorHandler = handler;
		colorHandler.addPropertyChangeListener(this);
		setCanvasColors();
	}

	protected void setCanvasColors() {
		if (colorHandler != null) {
			for (int i = 0; i < attr.size(); i++)
				if (attr.elementAt(i) instanceof String) {
					((ColorCanvas) ccs.elementAt(i)).setColor(colorHandler.getColorForAttribute((String) attr.elementAt(i)));
				} else if (attr.elementAt(i) instanceof Attribute) {
					((ColorCanvas) ccs.elementAt(i)).setColor(colorHandler.getColorForAttribute(((Attribute) attr.elementAt(i)).getIdentifier()));
				}
		}
	}

	protected String[] prohibitedAttributes = null;
	protected ColorDlg cDlg = null;

	protected boolean usedForIP = false;
	protected boolean allowAddRemoveAttr = false;
	protected Checkbox dynCh = null;

	// CR: added
	public WAPanel(ActionListener al, AttributeDataPortion dTable, Vector attr) {
		this(al, dTable, attr, false);
	}

	// CR: added
	public WAPanel(ActionListener al, AttributeDataPortion dTable, Vector attr, boolean usedForIP) {
		this(al, dTable, attr, usedForIP, true);
	}

	// CR: modified to consider <allowAddRemoveAttr>
	public WAPanel(ActionListener al, AttributeDataPortion dTable, Vector attr, boolean usedForIP, boolean allowAddRemoveAttr) {
		this.al = al;
		this.dTable = dTable;
		this.attr = attr;
		this.usedForIP = usedForIP;
		this.allowAddRemoveAttr = allowAddRemoveAttr; // CR: (dis)allow adding choice menu to GUI

		if (dTable instanceof DataTable) {
			sm = ((DataTable) dTable).getSemanticsManager();
		}
		dTable.addPropertyChangeListener(this);
		sliders = new Vector(attr.size(), 5);
		labelsN = new Vector(attr.size(), 5);
		labelsV = new Vector(attr.size(), 5);
		arrows = new Vector(attr.size(), 5);
		ccs = new Vector(attr.size(), 5);

		setLayout(new BorderLayout());
		for (int i = 0; i < attr.size(); i++) {
			Attribute a = (attr.elementAt(i) instanceof Attribute) ? (Attribute) attr.elementAt(i) : dTable.getAttribute((String) attr.elementAt(i));
			labelsN.addElement(new Label(a.getName()));
			labelsV.addElement(new Label(StringUtil.floatToStr(1f / attr.size(), 2)));
			String ID = a.getName();
			boolean b = true;
			if (sm != null && sm.isAttributeCostCriterion(ID)) {
				b = false;
			}
			arrows.addElement(new Arrow(this, b, i));
			ColorCanvas cc = new ColorCanvas();
			cc.setActionListener(this);
			ccs.addElement(cc);
			Slider sl = new Slider(this, 0f, 1f, 1f / attr.size());

			/*
			 * A.O. 2004-07-30 Don't generate drag events by default.
			 *
			 */
			sl.setNAD(true);
			sliders.addElement(sl);
		}
		upP = new Panel();
		fillUpP();
		if (usedForIP) {
			add(upP, "Center");
		} else {
			scp = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
			scp.add(upP);
			add(scp, "Center");
		}

		Panel loP = new Panel();
		loP.setLayout(new ColumnLayout());
		((ColumnLayout) loP.getLayout()).setAlignment(ColumnLayout.Hor_Left);
		add(loP, "South");
		// following text:"Set equal weights"
		Button b = new Button(res.getString("Set_equal_weights"));
		Panel p = new Panel();
		p.setLayout(new FlowLayout());
		p.add(b);

		dynCh = new Checkbox(res.getString("Dynamic update"), true);
		dynCh.addItemListener(this);
		loP.add(dynCh);
		loP.add(p);
		b.addActionListener(this);
		b.setActionCommand("EqWeights");

		// add choice menus to add and remove attributes/criteria
		// CR:
		if (allowAddRemoveAttr) {
			loP.add(new Line(false));
			b = new Button(res.getString("Add_criterion"));
			b.setActionCommand("add");
			b.addActionListener(this);
			loP.add(b);
			//loP.add(new Line(false));
			delCh = new Choice();
			delCh.addItemListener(this);
			loP.add(delCh);
			// following text: "You can always remove any criterion using this drop-down list"
			new PopupManager(delCh, res.getString("Ycara"), true);
			loP.add(new Line(false));
			fillDelCh();
		}

		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				checkUpPSize();
			}
		});
	}

	@Override
	public void destroy() {
		if (sm != null) {
			boolean isMax[] = getIsMax();
			for (int i = 0; i < attr.size(); i++) {
				Attribute a = (attr.elementAt(i) instanceof Attribute) ? (Attribute) attr.elementAt(i) : dTable.getAttribute((String) attr.elementAt(i));
				String ID = a.getIdentifier();
				if (isMax[i]) {
					sm.setAttributeIsBenefitCriterion(ID);
				} else {
					sm.setAttributeIsCostCriterion(ID);
				}
			}
		}
		if (dTable != null) {
			dTable.removePropertyChangeListener(this);
		}
		if (colorHandler != null) {
			colorHandler.removePropertyChangeListener(this);
		}
		if (cDlg != null) {
			cDlg.dispose();
		}
		destroyed = true;
	}

	/**
	* Replies whether is destroyed or not
	*/
	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

	public void setProhibitedAttributes(String prohibitedAttributes[]) {
		this.prohibitedAttributes = prohibitedAttributes;
	}

	public boolean[] getIsMax() {
		boolean isMax[] = new boolean[arrows.size()];
		for (int i = 0; i < isMax.length; i++) {
			Arrow ar = (Arrow) arrows.elementAt(i);
			isMax[i] = ar.isMax();
		}
		return isMax;
	}

	public void hideArrows() {
		for (int i = 0; i < arrows.size(); i++) {
			Arrow ar = (Arrow) arrows.elementAt(i);
			ar.setVisible(false);
		}
	}

	public float[] getWeights() {
		float W[] = new float[arrows.size()];
		for (int i = 0; i < W.length; i++) {
			Slider sl = (Slider) sliders.elementAt(i);
			W[i] = (float) sl.getValue();
		}
		return W;
	}

	protected void checkUpPSize() {
		if (scp == null)
			return;
		Dimension d = scp.getViewportSize(), dp = upP.getSize();
		if (d.width < 10)
			return;
		//if (dp.height>d.height) d.width-=scp.getVScrollbarWidth();
		if (dp.width != d.width - 2) {
			prefSlW = 0;
			dp.width = d.width - 2;
			if (sliders != null) {
				for (int i = 0; i < sliders.size(); i++) {
					if (prefSlW == 0) {
						ColorCanvas cc = (ColorCanvas) ccs.elementAt(i);
						prefSlW = dp.width - cc.getPreferredSize().width;
						Label l = (Label) labelsV.elementAt(i);
						prefSlW -= l.getPreferredSize().width;
					}
					Slider sl = (Slider) sliders.elementAt(i);
					sl.setPreferredSize(prefSlW, sl.getPreferredSize().height);
					CManager.invalidateAll(sl);
				}
			}
			upP.setSize(dp);
			upP.validate();
			scp.invalidate();
			scp.validate();
		}
	}

	protected void fillUpP() {
		upP.removeAll();
		upP.setLayout(new GridLayout(attr.size() + ((usedForIP) ? 1 : 0), 1));
		for (int i = 0; i < attr.size(); i++) {
			Panel pp = new Panel();
			pp.setLayout(new ColumnLayout());
			Panel p = new Panel();
			p.setLayout(new BorderLayout());
			p.add((Label) labelsN.elementAt(i), "Center");
			p.add((ColorCanvas) ccs.elementAt(i), "West");
			// following text: "Click here to change color"
			new PopupManager((ColorCanvas) ccs.elementAt(i), res.getString("Chtocc"), true);
			pp.add(p);
			p = new Panel();
			p.setLayout(new BorderLayout());
			p.add((Arrow) arrows.elementAt(i), "West");
			// following text: "Cost (to be minimized) and benefit (to be maximized) criteria are distinquished. Click the arrow to change type of the criterion"
			new PopupManager((Arrow) arrows.elementAt(i), res.getString("Costtbm"), true);
			if (attr.size() > 1) {
				p.add((Slider) sliders.elementAt(i), "Center");
				// following text: "Use sliders to change weights of attributes"
				new PopupManager((Slider) sliders.elementAt(i), res.getString("Ustcw"), true);
				p.add((Label) labelsV.elementAt(i), "East");
				// following text: "Use sliders to change weights of attributes"
				new PopupManager((Label) labelsV.elementAt(i), res.getString("Ustcw"), true);
			}
			p.add(new Line(false), "South");
			pp.add(p);
			upP.add(pp);
		}
		checkUpPSize();
	}

	protected void fillDelCh() {
		if (delCh == null)
			return;
		delCh.removeAll();
		// following text: "Remove criterion"
		delCh.addItem(res.getString("Remove_criterion"));
		for (int i = 0; i < attr.size(); i++) {
			Attribute a = (attr.elementAt(i) instanceof Attribute) ? (Attribute) attr.elementAt(i) : dTable.getAttribute((String) attr.elementAt(i));
			delCh.addItem(a.getName());
		}
	}

	public void fnReordered(int dragged, int draggedTo) {
		Object elem = attr.elementAt(dragged);
		attr.removeElementAt(dragged);
		attr.insertElementAt(elem, draggedTo);

		Object o = sliders.elementAt(dragged);
		sliders.removeElementAt(dragged);
		sliders.insertElementAt(o, draggedTo);
		o = arrows.elementAt(dragged);
		arrows.removeElementAt(dragged);
		arrows.insertElementAt(o, draggedTo);
		o = labelsN.elementAt(dragged);
		labelsN.removeElementAt(dragged);
		labelsN.insertElementAt(o, draggedTo);
		o = labelsV.elementAt(dragged);
		labelsV.removeElementAt(dragged);
		labelsV.insertElementAt(o, draggedTo);
		o = ccs.elementAt(dragged);
		ccs.removeElementAt(dragged);
		ccs.insertElementAt(o, draggedTo);

		fillUpP();

		upP.invalidate();
		upP.validate();
		fillDelCh();
	}

	public Vector getAttributes() {
		return attr;
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		if (ae.getSource() instanceof ColorCanvas) {
			if (colorHandler != null) {
				// finding the ColorCanvas clicked
				ColorCanvas cc = null;
				String name = null;
				for (int i = 0; i < ccs.size() && cc == null; i++)
					if (ccs.elementAt(i) == ae.getSource()) {
						cc = (ColorCanvas) ccs.elementAt(i);
						Attribute a = (attr.elementAt(i) instanceof Attribute) ? (Attribute) attr.elementAt(i) : dTable.getAttribute((String) attr.elementAt(i));
						name = a.getName();
					}
				// getting new color for it
				if (cDlg == null) {
					cDlg = new ColorDlg(CManager.getAnyFrame(this), "");
				}
				//cDlg.setVisible(true);
				// following text: "Color for: "+name
				cDlg.setTitle("Color for: " + name);
				cDlg.selectColor(this, cc, cc.getColor());
			}
			return;
		}
		if (ae.getSource() instanceof Button) {
			String cmd = ae.getActionCommand();
			if (cmd.equals("add")) {
				int n = attr.size();
				if (prohibitedAttributes != null) {
					n += prohibitedAttributes.length;
				}
				Vector excludeAttrIds = new Vector(n, 10);
				for (int i = 0; i < attr.size(); i++)
					if (attr.elementAt(i) instanceof Attribute) {
						excludeAttrIds.addElement(((Attribute) attr.elementAt(i)).getIdentifier());
					} else {
						excludeAttrIds.addElement(attr.elementAt(i));
					}
				if (prohibitedAttributes != null) {
					for (String prohibitedAttribute : prohibitedAttributes) {
						excludeAttrIds.addElement(prohibitedAttribute);
					}
				}
				AttributeChooser attrSel = new AttributeChooser();
				Vector attributes = attrSel.selectColumns(dTable, null, excludeAttrIds, true, res.getString("Select_one_or_more"), null);
				if (addAttributes(attributes)) {
					attributesChanged();
				}
			} else {
				for (int i = 0; i < sliders.size(); i++) {
					Slider sl = (Slider) sliders.elementAt(i);
					sl.setValue(1f / attr.size());
					Label l = (Label) labelsV.elementAt(i);
					l.setText(StringUtil.floatToStr(1f / attr.size(), 2));
				}
			}
		}
		if (ae.getSource() instanceof Arrow) {
		}
		if (ae.getSource() instanceof Slider) {
			int n = -1;
			float newW = Float.NaN;
			for (int i = 0; i < sliders.size(); i++)
				if (sliders.elementAt(i) == ae.getSource()) {
					Slider sl = (Slider) ae.getSource();
					n = i;
					newW = (float) sl.getValue();
				}
			adjustWeights(n, newW);
		}
		weightsChanged();
	}

	protected void weightsChanged() {
		if (al != null) {
			al.actionPerformed(new ActionEvent(this, 0, "weightsChanged"));
		}
	}

	protected void attributesChanged() {
		if (al != null) {
			al.actionPerformed(new ActionEvent(this, 0, "fnChanged"));
		}
	}

	protected void removeAttribute(int n) {
		if (n < 0 || n >= attr.size())
			return;
		attr.removeElementAt(n);
		arrows.removeElementAt(n);
		sliders.removeElementAt(n);
		labelsN.removeElementAt(n);
		labelsV.removeElementAt(n);
		ccs.removeElementAt(n);
		fillUpP();
		upP.invalidate();
		upP.validate();
		delCh.remove(n + 1);
		delCh.select(0);
		adjustWeights(-1, Float.NaN);
	}

	protected boolean addAttributes(Vector attributes) {
		if (attributes == null || attributes.size() < 1)
			return false;
		//remove the attributes that are already selected
		for (int i = attributes.size() - 1; i >= 0; i--) {
			boolean found = false;
			Attribute at = (Attribute) attributes.elementAt(i);
			if (attr.size() > 0 && (attr.elementAt(0) instanceof Attribute)) {
				found = attr.contains(at);
			} else {
				found = attr.contains(at.getIdentifier());
			}
			if (found) {
				attributes.removeElementAt(i);
			}
		}
		if (attributes.size() < 1)
			return false;
		for (int i = 0; i < attributes.size(); i++) {
			Attribute a = (Attribute) attributes.elementAt(i);
			if (attr.size() > 0 && (attr.elementAt(0) instanceof Attribute)) {
				attr.addElement(a);
			} else {
				attr.addElement(a.getIdentifier());
			}
			int newPos = attr.size() - 1;
			labelsN.insertElementAt(new Label(a.getName()), newPos);
			labelsV.insertElementAt(new Label(StringUtil.floatToStr(1f / attr.size(), 2)), newPos);
			boolean b = true;
			if (sm != null && sm.isAttributeCostCriterion(a.getIdentifier())) {
				b = false;
			}
			arrows.insertElementAt(new Arrow(this, b, newPos), newPos);
			ColorCanvas cc = new ColorCanvas();
			cc.setActionListener(this);
			if (colorHandler != null) {
				cc.setColor(colorHandler.getColorForAttribute(a.getIdentifier()));
			}
			ccs.insertElementAt(cc, newPos);
			Slider sl = new Slider(this, 0f, 1f, 1f / attr.size());
			sl.setPreferredSize(prefSlW, sl.getPreferredSize().height);
			/*
			 * A.O. 2004-07-30 Don't generate drag events by default.
			 *
			 */
			sl.setNAD(true);
			sliders.insertElementAt(sl, newPos);
			adjustWeights(newPos, 1f / attr.size());
		}
		fillUpP();
		upP.invalidate();
		upP.validate();
		fillDelCh();
		return true;
	}

	@Override
	public void itemStateChanged(ItemEvent ie) {
		if (ie.getSource() == delCh) {
			int n = delCh.getSelectedIndex();
			if (n == 0)
				return;
			if (delCh.getItemCount() <= 2) {
				delCh.select(0);
				return;
			}
			removeAttribute(n - 1);
			attributesChanged();
			return;
		} else if (ie.getSource() == dynCh) {
			/*
			 * A.O. 2004-07-30
			 * en-/disable drag events for the sliders
			 */
			boolean state = dynCh.getState();
			for (int i = 0; i < sliders.size(); i++) {
				Slider slider = (Slider) sliders.elementAt(i);
				slider.setNAD(state);
			}
		}
	}

	public void adjustWeights(int n, float newW) {
		float newSum = 1f;
		if (n >= 0) {
			Label l = (Label) labelsV.elementAt(n);
			l.setText(StringUtil.floatToStr(newW, 2));
			Slider sl = (Slider) sliders.elementAt(n);
			sl.setValue(newW);
			newSum = 1 - newW;
		}
		float oldSum = 0;
		for (int i = 0; i < sliders.size(); i++)
			if (i != n) {
				oldSum += ((Slider) sliders.elementAt(i)).getValue();
			}
		for (int i = 0; i < sliders.size(); i++)
			if (i != n) {
				Slider sl = (Slider) sliders.elementAt(i);
				float val = (newSum == 0) ? 0 : ((oldSum == 0) ? 1f / (sliders.size() - 1) : (float) sl.getValue() * newSum / oldSum);
				sl.setValue(val);
				Label l = (Label) labelsV.elementAt(i);
				l.setText(StringUtil.floatToStr(val, 2));
			}
	}

	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		if (colorHandler != null && pce.getPropertyName().equals("colors")) {
			//System.out.println("* WAPanel PropertyChangeEvent received");
			for (int i = 0; i < ccs.size(); i++) {
				ColorCanvas cc = (ColorCanvas) ccs.elementAt(i);
				Attribute a = (attr.elementAt(i) instanceof Attribute) ? (Attribute) attr.elementAt(i) : dTable.getAttribute((String) attr.elementAt(i));
				Color newColor = colorHandler.getColorForAttribute(a.getIdentifier());
				if (newColor == null) {
					continue;
				}
				if (cc.getColor() == null || !cc.getColor().equals(newColor)) {
					cc.setColor(newColor);
				}
			}
		}
	}

	@Override
	public void colorChanged(Color c, Object sel) {
		// find a ColorCanvas produced the message
		ColorCanvas cc = null;
		String ID = null;
		for (int i = 0; i < ccs.size() && cc == null; i++)
			if (ccs.elementAt(i) == sel) {
				cc = (ColorCanvas) ccs.elementAt(i);
				Attribute a = (attr.elementAt(i) instanceof Attribute) ? (Attribute) attr.elementAt(i) : dTable.getAttribute((String) attr.elementAt(i));
				ID = a.getIdentifier();
			}
		// save a color
		cc.setColor(c);
		colorHandler.setColorForAttribute(c, ID);
		// hide a dialog
		cDlg.dispose();
		cDlg = null;
	}

}
