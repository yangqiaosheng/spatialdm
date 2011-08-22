package spade.analysis.manipulation;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.Supervisor;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Line;
import spade.lib.lang.Language;
import spade.lib.util.FloatArray;
import spade.lib.util.IntArray;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataTable;
import spade.vis.database.ThematicDataItem;
import spade.vis.mapvis.UtilitySignDrawer;

/**
* This is a manipulator for "utility signs" map visualisation method.
* It contains a UtilitySignsFilter and a component for setting weits of
* the attributes (decision criteria).
*/
public class UtilitySignsManipulator extends Panel implements Manipulator, ActionListener, PropertyChangeListener, ObjectEventReactor {
	static ResourceBundle res = Language.getTextResource("spade.analysis.manipulation.Res");

	protected UtilitySignDrawer usd = null;
	protected AttributeDataPortion dataTable = null;
	protected Supervisor supervisor = null;
	protected ObjectComparison objCmp = null;
	/**
	* Used to switch the interpretation of mouse click to object comparison
	*/
	protected ClickManagePanel cmpan = null;

	/**
	* Construction of map manipulator. Returns true if successfully constructed.
	* Arguments: 1) supervisor responsible for dynamic linking of displays;
	* 2) visualizer or classifier to be manipulated; 3) table with source data;
	* 4) table filter
	* The Manipulator must check if the visualizer has the appropriate type.
	* For UtilitySignsManipulator visualizer should be an instance of UtilitySignDrawer.
	*/
	@Override
	public boolean construct(Supervisor sup, Object visualizer, AttributeDataPortion dataTable) {
		if (visualizer == null || dataTable == null)
			return false;
		if (!(visualizer instanceof UtilitySignDrawer))
			return false;
		this.usd = (UtilitySignDrawer) visualizer;
		this.dataTable = dataTable;

		Vector attr = usd.getAttributes();
		if (attr == null || attr.size() < 1)
			return false;
		supervisor = sup;
		if (supervisor != null) {
			supervisor.addPropertyChangeListener(this);
		}
		setLayout(new BorderLayout());
		Panel pp = new Panel();
		pp.setLayout(new ColumnLayout());
		if (usd.getSignType() == UtilitySignDrawer.BarSign) {
			objCmp = new ObjectComparison(sup, usd, dataTable);
			pp.add(objCmp);
			pp.add(new Line(false));
			cmpan = new ClickManagePanel(supervisor);
			cmpan.setComparer(objCmp);
			pp.add(cmpan);
			pp.add(new Line(false));
		}
		pp.add(new UtilitySignsFilter(usd));
		add(pp, "North");
		Vector weightedAttr = new Vector(attr.size(), 1);
		for (int i = 0; i < attr.size(); i++) {
			Vector subAttrs = usd.getSubAttributes(i);
			Attribute a = dataTable.getAttribute((String) attr.elementAt(i));
			if (subAttrs == null || subAttrs.size() < 1) {
				weightedAttr.addElement(a);
			} else {
				Attribute wa = new Attribute(usd.getInvariantAttrId(i), a.getType());
				wa.setName(usd.getAttrName(i));
				for (int j = 0; j < subAttrs.size(); j++) {
					Attribute child = dataTable.getAttribute((String) subAttrs.elementAt(j)), copy = new Attribute(child.getIdentifier(), child.getType());
					wa.addChild(copy);
				}
				weightedAttr.addElement(wa);
			}
		}
		WeightManipulator wm = new WeightManipulator(this, dataTable, weightedAttr);
//ID
		wm.setWeightsList(usd.getWeights());
		wm.addWeightUser(usd);
//    if (supervisor!=null) wm.setAttrColorHandler(supervisor.getAttrColorHandler());
		wm.setAttrColorHandler(usd);
//~ID
		add(wm, "Center");
		if (dataTable instanceof DataTable) {
			DataTable dTable = (DataTable) dataTable;
			if (dTable.hasDecisionSupporter()) {
				// following text: "Make decision"
				Button b = new Button(res.getString("Make_decision"));
				b.addActionListener(this);
				b.setActionCommand("decision");
				add(b, "South");
			}
		}
		return true;
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		if (ae.getActionCommand().equals("decision")) {
			makeAttributeForDecision();
		} else if ((ae.getSource() instanceof WeightManipulator) && ae.getActionCommand().equals("fnChanged")) {
			supervisor.notifyGlobalPropertyChange(Supervisor.eventDisplayedAttrs);
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		if (pce.getSource() == supervisor) {
			if (pce.getPropertyName().equals(Supervisor.eventAttrColors)) {
				usd.checkAttrColorsChange();
			}
			return;
		}
	}

	protected void makeAttributeForDecision() {
		int nobj = dataTable.getDataItemCount();
		FloatArray util = new FloatArray(nobj, 5);
		IntArray order = new IntArray(nobj, 5);
		for (int i = 0; i < nobj; i++) {
			float u = usd.getUtility((ThematicDataItem) dataTable.getDataItem(i));
			boolean inserted = false;
			for (int j = 0; j < util.size() && !inserted; j++)
				if (u > util.elementAt(j)) {
					util.insertElementAt(u, j);
					order.insertElementAt(i, j);
					inserted = true;
				}
			if (!inserted) {
				util.addElement(u);
				order.addElement(i);
			}
		}
		DataTable dTable = (DataTable) dataTable;
		int idx = dTable.addDerivedAttribute("temporary", AttributeTypes.integer, AttributeTypes.evaluate_rank, (Vector) usd.getAttributes().clone());
		for (int j = 0; j < nobj; j++) {
			dTable.getDataRecord(j).setAttrValue(String.valueOf(order.indexOf(j) + 1), idx);
		}
		dTable.notifyDecisionColumnAdded(dTable.getAttributeId(idx));
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
