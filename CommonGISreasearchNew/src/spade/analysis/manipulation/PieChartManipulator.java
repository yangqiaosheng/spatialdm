package spade.analysis.manipulation;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Choice;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.Supervisor;
import spade.analysis.transform.AttributeTransformer;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.FocusListener;
import spade.lib.basicwin.Focuser;
import spade.lib.basicwin.FocuserCanvas;
import spade.lib.basicwin.ItemReorderer;
import spade.lib.basicwin.Line;
import spade.lib.color.ColorCanvas;
import spade.lib.color.ColorDlg;
import spade.lib.color.ColorListener;
import spade.lib.lang.Language;
import spade.lib.util.StringUtil;
import spade.vis.action.ObjectEvent;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.ThematicDataItem;
import spade.vis.event.DEvent;
import spade.vis.event.EventReceiver;
import spade.vis.mapvis.PieChartDrawer;

public class PieChartManipulator extends Panel implements Manipulator, Destroyable, PropertyChangeListener, FocusListener, ActionListener, ItemListener, EventReceiver, ObjectEventReactor, ColorListener {
	static ResourceBundle res = Language.getTextResource("spade.analysis.manipulation.Res");
	/**
	* Used to generate unique identifiers of instances of MultiVisComparison
	*/
	protected static int nInstances = 0;
	protected int instanceN = 0;

	protected Supervisor supervisor = null;
	protected AttributeDataPortion dTable = null;
	protected PieChartDrawer vis = null;
	protected Focuser f = null;
	protected int nattrs = -1;
	protected ColorCanvas ccs[] = null, ccscopy[] = null;
	protected int order[] = null;
	protected Panel pcc = null, pCmp = null;
	protected Choice ch = null;
	protected Checkbox useSize = null;
	protected ColorDlg cDlg = null;
	protected Panel pOptions = null;
	/**
	* Used to switch the interpretation of mouse click to object comparison
	*/
	protected ClickManagePanel cmpan = null;
	/**
	* The identifier of the selected object for the visual comparison.
	*/
	protected String selObj = null;
	/**
	* The index of the "subattributes" of the time-dependent attributes which
	* were last used for the visual comparison
	*/
	protected int subAttrIdx = 0;
	/**
	* Indicates "destroyed" state. Initially is false.
	*/
	protected boolean destroyed = false;

	/**
	* Construction of map manipulator. Returns true if successfully constructed.
	* Arguments: 1) supervisor responsible for dynamic linking of displays;
	* 2) visualizer or classifier to be manipulated; 3) table with source data;
	* 4) table filter
	* The Manipulator must check if the visualizer has the appropriate type.
	* For PieChartManipulator visualizer should be an instance of PieChartDrawer.
	*/
	@Override
	public boolean construct(Supervisor sup, Object visualizer, AttributeDataPortion dataTable) {
		if (visualizer == null || dataTable == null)
			return false;
		if (!(visualizer instanceof PieChartDrawer))
			return false;
		this.vis = (PieChartDrawer) visualizer;
		if (vis.hasSubAttributes()) {
			vis.addVisChangeListener(this);
		}
		supervisor = sup;
		this.dTable = dataTable;
		AttributeTransformer aTrans = vis.getAttributeTransformer();
		if (aTrans != null) {
			aTrans.addPropertyChangeListener(this);
		} else {
			dTable.addPropertyChangeListener(this);
		}

		instanceN = ++nInstances;
		order = vis.getOrder();
		if (order == null)
			return false;
		supervisor.addPropertyChangeListener(this);
		/* System.out.print("*Order ");
		for (int i=0; i<order.length; i++) System.out.print(" "+order[i]);
		System.out.println(); */
		nattrs = order.length;
		setLayout(new ColumnLayout());
		// focuser for total value
		if (vis.getSumMAX() - vis.getSumMIN() > Math.abs(0.001 * vis.getSumMAX())) {
			// following text:"Diameter proportional to sum"
			useSize = new Checkbox(res.getString("Area_proportional"), true);
			useSize.addItemListener(this);
			add(useSize);
			vis.setUseSize(useSize.getState());
			f = new Focuser();
			f.setIsVertical(false);
			f.addFocusListener(this);
			// was: f.setAbsMinMax(0f,vis.getSumMAX());  why was "0f" for MIN ?
			f.setAbsMinMax(vis.getSumMIN(), vis.getSumMAX());
//ID
			f.setCurrMinMax(vis.getFocuserMin(), vis.getFocuserMax());
//~ID
			f.setIsUsedForQuery(true);
			TextField tfmin = new TextField(StringUtil.doubleToStr(vis.getSumMIN(), vis.getSumMIN(), vis.getSumMAX(), false)), tfmax = new TextField(StringUtil.doubleToStr(vis.getSumMAX(), vis.getSumMIN(), vis.getSumMAX(), true));
			f.setTextFields(tfmin, tfmax);
			add(new FocuserCanvas(f, false));
			Panel p = new Panel();
			p.setLayout(new GridLayout(1, 2));
			add(p);
			p.add(tfmin);
			p.add(tfmax);
			add(new Line(false));
		}
		// visual comparison
		// following text:"Select object for comparison"
		add(new Label(res.getString("Select_object_for"), Label.CENTER));
		ch = new Choice();
		// following text:"NO comparison"
		ch.add(res.getString("NO_comparison"));
//ID
		String cmpId = "";
		String currName;
		for (int i = 0; i < dTable.getDataItemCount(); i++) {
			currName = dTable.getDataItemName(i);
			ch.add(currName);
			if (vis.visCompObjName.equals(currName)) {
				cmpId = dTable.getDataItemId(i);
			}
		}
//~ID
		add(ch);
		cmpan = new ClickManagePanel(supervisor);
		cmpan.setComparer(this);
		add(cmpan);
		ch.addItemListener(this);
		pCmp = new Panel();
		add(pCmp);
		add(new Line(false));
		// reordering of segments
		Panel p = new Panel();
		p.setLayout(new BorderLayout());
		add(p);
		int nlines = nattrs;
		if (vis.getAttrId(0) != null) {
			++nlines;
		}
		pcc = new Panel(new GridLayout(nlines, 1));
		ccs = new ColorCanvas[nattrs];
		ccscopy = new ColorCanvas[nattrs];
		for (int i = 0; i < nattrs; i++) {
			int n = order[i];
			ccs[n] = new ColorCanvas();
			ccs[n].setColor(vis.getColorForAttribute(n));
			ccs[n].setActionListener(this);
			pcc.add(ccs[n]);
			ccscopy[n] = new ColorCanvas();
			ccscopy[n].setColor(vis.getColorForAttribute(n));
			ccscopy[n].setActionListener(this);
		}
		p.add(pcc, "West");

		ItemReorderer reord = new ItemReorderer();
		reord.setY0(0);
		reord.setDY(20);
		for (int element : order) {
			String id = vis.getInvariantAttrId(element);
			if (id == null) {
				continue;
			}
			String name = vis.getAttrName(element);
			if (element == 0) {
				name += res.getString("_the_rest_of_");
			}
			reord.addItem(id, name);
		}
		reord.addActionListener(this);
		p.add(reord, "Center");
		add(new Line(false));

		// 2 options for radius of pie
		if (useSize != null) {
			pOptions = new Panel(new GridLayout(2, 1, 0, 0));
			pOptions.setBackground(Color.white);
			Panel p1 = new Panel(new FlowLayout());
			CheckboxGroup cbgOptions = new CheckboxGroup();
//ID
			Checkbox useMinSize = new Checkbox(null, vis.useMinSize, cbgOptions);
			Checkbox useMaxSizeOnly = new Checkbox(null, !vis.useMinSize, cbgOptions); // default: minSize->0
//~ID
			useMinSize.setName("useMinSize");
			useMaxSizeOnly.setName("useMaxSizeOnly");
			useMinSize.addItemListener(this);
			useMaxSizeOnly.addItemListener(this);
			PieOption option = new PieOption(false);
			option.setDrawBorder(false);
			p1.add(useMaxSizeOnly);
			p1.add(option);
			pOptions.add(p1);
			p1 = new Panel(new FlowLayout());
			p1.add(useMinSize);
			option = new PieOption(true);
			option.setDrawBorder(false);
			p1.add(option);
			pOptions.add(p1);
			add(pOptions);
		}
		// ~2 options
		fillPCmp();
//ID
		if (vis.visCompObjName.length() > 0) {
			cmpToObj(cmpId);
		}
//~ID
		return true;
	}

	protected double cmpVals[] = null;

	public void fillPCmp() {
		pCmp.removeAll();
		Vector attrs = vis.getAttributes();
		int n = attrs.size();
		if (attrs.elementAt(0) == null) {
			n--;
		}
		pCmp.setLayout(new GridLayout(1, n));
		for (int i = 0; i < n; i++) {
			Panel p = new Panel();
			p.setLayout(new BorderLayout());
			pCmp.add(p);
			p.add(ccscopy[order[i]], "West");
			String txt = "?";
			if (cmpVals != null) {
				txt = Math.round(cmpVals[order[i]]) + "%";
			}
			p.add(new Label(txt), "Center");
		}
		pCmp.invalidate();
		pCmp.validate();
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		String cmd = ae.getActionCommand();
		if (cmd != null && cmd.equals("order_changed")) {
			ItemReorderer reord = (ItemReorderer) ae.getSource();
			Vector ids = reord.getItemList();
			for (int i = 0; i < ids.size(); i++)
				if (ids.elementAt(i) != null) {
					order[i] = vis.getAttrIndex((String) ids.elementAt(i));
				} else {
					order[i] = 0;
				}
			pcc.setVisible(false);
			pcc.removeAll();
			for (int element : order) {
				pcc.add(ccs[element]);
			}
			pcc.invalidate();
			pcc.setVisible(true);
			pcc.validate();
			vis.setOrder(order);
			cmpToObj(ch.getSelectedIndex() - 1);
			return;
		}
		if (ae.getSource() instanceof ColorCanvas) {
			// finding the ColorCanvas clicked
			ColorCanvas cc = null;
			String name = null;
			for (int i = 0; i < ccs.length && cc == null; i++)
				if (ccs[i] == ae.getSource() || ccscopy[i] == ae.getSource()) {
					cc = ccs[i];
					name = vis.getAttrName(i);
				}
			if (cc.getColor() == null)
				return;
			// getting new color for it
			if (cDlg == null) {
				cDlg = new ColorDlg(CManager.getAnyFrame(this), "");
			}
			// following text: "Color for: "
			cDlg.setTitle(res.getString("Color_for_") + name);
			cDlg.selectColor(this, cc, cc.getColor());
			return;
		}
	}

	/*
	* color change through the dialog
	*/
	@Override
	public void colorChanged(Color c, Object sel) {
		// find a ColorCanvas produced the message
		int n = -1;
		for (int i = 0; i < ccs.length && n == -1; i++)
			if (ccs[i] == sel || ccscopy[i] == sel) {
				n = i;
			}
		// save a color
		ccs[n].setColor(c);
		ccscopy[n].setColor(c);
		vis.setColorForAttribute(c, n);
		fillPCmp();
		// hide a dialog
		cDlg.setVisible(false);
	}

	public void cmpToObj(String objId) {
		int recN = -1;
		if (objId != null) {
			recN = dTable.indexOf(objId);
		}
		cmpToObj(recN);
		ch.select(1 + recN);
	}

	public void cmpToObj(int objN) {
		double visCompVals[] = null;
		if (objN >= 0) {
			visCompVals = new double[order.length];
			for (int i = 0; i < visCompVals.length; i++) {
				visCompVals[i] = 0f;
			}
			double sum = 0f;
			int start = 0;
			if (vis.getAttrId(0) == null) {
				++start;
			}
			ThematicDataItem item = (ThematicDataItem) dTable.getDataItem(objN);
			for (int i = start; i < visCompVals.length; i++) {
				double f = vis.getNumericAttrValue(item, i);
				if (!Double.isNaN(f)) {
					visCompVals[i] = f;
					if (i != 0) {
						sum += f;
					}
				}
			}
			if (start < 1) {
				visCompVals[0] -= sum;
			}
			cmpVals = new double[visCompVals.length];
			for (int i = 0; i < cmpVals.length; i++) {
				cmpVals[i] = 100f * visCompVals[i] / sum;
			}
		} else {
			cmpVals = null;
		}
		if (objN >= 0) {
			selObj = dTable.getDataItemId(objN);
		} else {
			selObj = null;
		}
		subAttrIdx = vis.getCurrentSubAttrIndex();
		vis.setVisCompObj(objN, dTable.getDataItemName(objN), visCompVals, cmpVals);
		fillPCmp();
	}

	@Override
	public void itemStateChanged(ItemEvent ie) {
		Object objSrc = ie.getSource();
		if (objSrc == ch) {
			int n = ch.getSelectedIndex() - 1;
			cmpToObj(n);
			return;
		}
		if (objSrc == useSize) {
			vis.setUseSize(useSize.getState());
			pOptions.setVisible(useSize.getState());
			return;
		}
		if (objSrc instanceof Checkbox) {
			Checkbox cb = (Checkbox) objSrc;
			if (cb.getName().equalsIgnoreCase("useMinSize")) {
				vis.setUseMinSize(true);
			}
			if (cb.getName().equalsIgnoreCase("useMaxSizeOnly")) {
				vis.setUseMinSize(false);
			}
			return;
		}

	}

	@Override
	public void focusChanged(Object source, double lowerLimit, double upperLimit) {
		vis.setFocuserMinMax(lowerLimit, upperLimit);
	}

	@Override
	public void limitIsMoving(Object source, int n, double currValue) { // n==0 -> min, n==1 -> max
		vis.setFocuserMinMax(f.getCurrMin(), f.getCurrMax());
	}

	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getSource() == supervisor) {
			if (e.getPropertyName().equals(Supervisor.eventAttrColors)) {
				for (int i = 0; i < ccs.length; i++) {
					String id = vis.getInvariantAttrId(i);
					Color newColor = supervisor.getColorForAttribute(id);
					if (ccs[i].getColor() != null && !ccs[i].getColor().equals(newColor)) {
						ccs[i].setColor(newColor);
						ccscopy[i].setColor(newColor);
						if (!newColor.equals(vis.getColorForAttribute(i))) {
							vis.setColorForAttribute(newColor, i);
						}
					}
				}
			}
			return;
		}
		if (e.getSource().equals(vis) && selObj != null && subAttrIdx != vis.getCurrentSubAttrIndex()) {
			cmpToObj(selObj);
		}
		if (((e.getSource() instanceof AttributeTransformer) && e.getPropertyName().equals("values"))
				|| (e.getSource().equals(dTable) && (e.getPropertyName().equals("values") || e.getPropertyName().equals("data_added") || e.getPropertyName().equals("data_removed") || e.getPropertyName().equals("data_updated")))) {
			if (f != null) {
				f.setMinMax(vis.getSumMIN(), vis.getSumMAX(), vis.getSumMIN(), vis.getSumMAX());
				f.refresh();
			}
			cmpToObj(selObj);
		}
	}

//---------------- implementation of the EventReceiver interface ------------
	/**
	* The EventReceiver answers whether it is interested in getting the specified
	* kind of events. A PieChartManipulator device is interested in receiving
	* object clicks events.
	* It uses them for setting reference values for visual comparison.
	*/
	@Override
	public boolean doesListenToEvent(String eventId) {
		return eventId.equals(ObjectEvent.click);
	}

	/**
	* This method is used for delivering events to the Event Receiver.
	*/
	@Override
	public void eventOccurred(DEvent evt) {
		if (evt instanceof ObjectEvent) {
			ObjectEvent oe = (ObjectEvent) evt;
			if (StringUtil.sameStrings(oe.getSetIdentifier(), dTable.getEntitySetIdentifier()) && oe.getType().equals(ObjectEvent.click)) {
				for (int i = 0; i < oe.getAffectedObjectCount(); i++) {
					String objId = oe.getObjectIdentifier(i);
					int recN = dTable.indexOf(objId);
					if (recN >= 0) {
						cmpToObj(recN);
						ch.select(1 + recN);
						return;
					}
				}
				cmpToObj(-1);
				ch.select(0);
			}
		}
	}

	/**
	* Removes itself from receivers of object events
	*/
	@Override
	public void destroy() {
		if (cmpan != null) {
			cmpan.destroy();
		}
		supervisor.removePropertyChangeListener(this);
		destroyed = true;
	}

	/**
	* Replies whether is destroyed or not
	*/
	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

	/**
	* Returns a unique identifier of the component
	* (used only internally, not shown to the user).
	* The identifier is used for explicit linking of producers and recipients of
	* object events.
	*/
	@Override
	public String getIdentifier() {
		return "Pie_Comparison_" + instanceN;
	}

//------------- implementation of the ObjectEventReactor interface ------------
	/**
	* An ObjectEventReactor may process object events either from all displays
	* or from the component (e.g. map) it is attached to. This component is a
	* primary event source for the ObjectEventReactor. A reference to the
	* primary event source is set using this method.
	*/
	@Override
	public void setPrimaryEventSource(Object evtSource) {
		if (cmpan != null) {
			cmpan.setPrimaryEventSource(evtSource);
		}
	}
}
