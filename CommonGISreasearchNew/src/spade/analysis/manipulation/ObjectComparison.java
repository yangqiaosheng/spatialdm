package spade.analysis.manipulation;

import java.awt.BorderLayout;
import java.awt.Choice;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.Supervisor;
import spade.analysis.transform.AttributeTransformer;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.DialogContent;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.PopupManager;
import spade.lib.lang.Language;
import spade.lib.util.StringUtil;
import spade.vis.action.ObjectEvent;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.ThematicDataItem;
import spade.vis.event.DEvent;
import spade.vis.event.EventReceiver;
import spade.vis.mapvis.UtilitySignDrawer;

class cmpValsPanel extends Panel implements DialogContent {
	static ResourceBundle res = Language.getTextResource("spade.analysis.manipulation.Res");

	protected float cmpVals[] = null, minVals[] = null, maxVals[] = null;
	protected TextField tf[] = null;

	public cmpValsPanel(UtilitySignDrawer usd, AttributeDataPortion dataTable) {
		super();
		Vector attrs = usd.getAttributes();
		setLayout(new GridLayout(2 * attrs.size(), 1));
		cmpVals = new float[attrs.size()];
		minVals = new float[attrs.size()];
		maxVals = new float[attrs.size()];
		tf = new TextField[attrs.size()];
		for (int i = 0; i < attrs.size(); i++) {
			minVals[i] = (float) usd.getDataMin(i);
			maxVals[i] = (float) usd.getDataMax(i);
			cmpVals[i] = (minVals[i] + maxVals[i]) / 2.0f;
			add(new Label(dataTable.getAttributeName(dataTable.getAttrIndex((String) attrs.elementAt(i)))));
			Panel pp = new Panel();
			pp.setLayout(new GridLayout(1, 3));
			pp.add(new Label(StringUtil.floatToStr(minVals[i])));
			pp.add(tf[i] = new TextField(StringUtil.floatToStr(cmpVals[i])));
			pp.add(new Label(StringUtil.floatToStr(maxVals[i])));
			add(pp);
		}
	}

	public float[] getCmpVals() {
		return cmpVals;
	}

	protected String err = null;

	@Override
	public String getErrorMessage() {
		return err;
	}

	@Override
	public boolean canClose() {
		for (int i = 0; i < cmpVals.length; i++) {
			try {
				cmpVals[i] = Float.valueOf(tf[i].getText()).floatValue();
				if (cmpVals[i] < minVals[i]) {
					cmpVals[i] = minVals[i];
				}
				if (cmpVals[i] > maxVals[i]) {
					cmpVals[i] = maxVals[i];
				}
			} catch (NumberFormatException nfe) {
				cmpVals[i] = Float.NaN;
				tf[i].requestFocus();
				// following text: "Wrong number in the field"
				err = res.getString("Wrong_number_in_the");
				return false;
			}
		}
		return true;
	}
}

/**
* ObjectComparison is a manipulator for the "utility bars" map visualisation
* method that allows to compare visually  utility of all objects represented on
* the map with utility of a selected object. The comparison is supported by
* appropriate changing of signs on the map. The ObjectComparison component
* allows the user to select the reference object for comparison. It also reacts
* to object selection events occurring on the map (and, optionally, other
* graphical displays).
*/
public class ObjectComparison extends Panel implements PropertyChangeListener, ItemListener, EventReceiver, Destroyable {
	/**
	* Used to generate unique identifiers of instances of ObjectComparison
	*/
	static ResourceBundle res = Language.getTextResource("spade.analysis.manipulation.Res");
	protected static int nInstances = 0;
	protected int instanceN = 0;

	protected UtilitySignDrawer usd = null;
	protected AttributeDataPortion dataTable = null;
	protected Supervisor supervisor = null;
	protected Choice chObj = null;
	/**
	* The table row number of the selected object for the visual comparison.
	*/
	protected int cmpRowN = -1;
	/**
	* The index of the "subattributes" of the time-dependent attributes which
	* were last used for the visual comparison
	*/
	protected int subAttrIdx = 0;
	/**
	* Indicates "destroyed" state. Initially is false.
	*/
	protected boolean destroyed = false;

	protected PopupManager pm = null;

	public ObjectComparison(Supervisor sup, UtilitySignDrawer usd, AttributeDataPortion dataTable) {
		this.dataTable = dataTable;
		this.usd = usd;
		this.supervisor = sup;
		if (usd == null)
			return;
		usd.addVisChangeListener(this);
		AttributeTransformer aTrans = usd.getAttributeTransformer();
		if (aTrans != null) {
			aTrans.addPropertyChangeListener(this);
		}
		instanceN = ++nInstances;
		setLayout(new BorderLayout());
		chObj = new Choice();
		// following text: "Select object for comparison:"
		chObj.add(res.getString("Select_object_for"));
//ID
		int cmpN = 0;
		String currName;
		for (int i = 0; i < dataTable.getDataItemCount(); i++) {
			currName = dataTable.getDataItemName(i);
			chObj.add(currName);
			if (usd.cmpObjName.equals(currName)) {
				cmpN = i;
			}
		}
//~ID
		// following text: "*** arbitrary values ***"
		chObj.add(res.getString("_arbitrary_values_"));
		add(chObj, "Center");
		chObj.addItemListener(this);
		pm = new PopupManager(chObj, getPMText(), true);
//ID
		if (usd.cmpObjName.length() > 0) {
			compareToRowN(cmpN);
		} else if (usd.getCmpVals() != null) {
			usd.setCmpModeOn(-1, "specified values", usd.getCmpVals());
			cmpRowN = dataTable.getDataItemCount();
		}
//~ID
	}

	public String getPMText() {
		int n = chObj.getSelectedIndex();
		// following text: "Select object\nto compare to"
		if (n == 0)
			return res.getString("Select_object_to");
		// following text: "Comparison to:\n"+chObj.getSelectedItem()+"\nValues:\n"
		String str = res.getString("Comparison_to_") + chObj.getSelectedItem() + res.getString("Values_");
		Vector attrs = usd.getAttributes();
		float cmpVals[] = usd.getCmpVals();
		for (int i = 0; i < attrs.size(); i++) {
			str += ((i == 0) ? "" : "\n") + String.valueOf(cmpVals[i]) + " : " + usd.getAttrName(i);
		}
		return str;
	}

	boolean ignoreVisChange = false;

	/**
	* Reacts to changes of reference values
	*/
	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		if (pce.getSource().equals(usd)) {
			if (cmpRowN >= 0 && cmpRowN < dataTable.getDataItemCount() && subAttrIdx != usd.getCurrentSubAttrIndex()) {
				compareToRowN(cmpRowN);
				return;
			}
			subAttrIdx = usd.getCurrentSubAttrIndex();
			//System.out.println("* property changed");
			if (ignoreVisChange) {
				ignoreVisChange = false;
				return;
			}
			int idx = 0;
			if (usd.isInCmpMode())
				if (usd.getCmpObjNum() < 0) {
					idx = chObj.getItemCount() - 1;
				} else {
					idx = usd.getCmpObjNum() + 1;
				}
			if (idx != chObj.getSelectedIndex()) {
				chObj.select(idx);
				pm.setText(getPMText());
			}
		} else if ((pce.getSource() instanceof AttributeTransformer) && pce.getPropertyName().equals("values")) {
			compareToRowN(-1);
		}
	}

	@Override
	public void itemStateChanged(ItemEvent ie) {
		int n = chObj.getSelectedIndex();
		if (n == 0) {
			compareToRowN(-1);
		} else if (n == chObj.getItemCount() - 1) {
			cmpValsPanel cvp = new cmpValsPanel(usd, dataTable);
			// following text:"set comparison values"
			OKDialog okDlg = new OKDialog(supervisor.getUI().getMainFrame(), res.getString("set_comparison_values"), true);
			okDlg.addContent(cvp);
			okDlg.show();
			if (okDlg.wasCancelled()) {
				compareToRowN(-1);
			} else {
				usd.setCmpModeOn(-1, "specified values", cvp.getCmpVals());
				cmpRowN = dataTable.getDataItemCount();
			}
		} else {
			compareToRowN(n - 1);
		}
	}

	protected void compareToRowN(int rowN) {
		if (cmpRowN == rowN && subAttrIdx == usd.getCurrentSubAttrIndex())
			return;
		cmpRowN = rowN;
		subAttrIdx = usd.getCurrentSubAttrIndex();
		ignoreVisChange = true;
		if (cmpRowN < 0) {
			usd.setCmpModeOff();
		} else if (cmpRowN < dataTable.getDataItemCount()) {
			ThematicDataItem dit = (ThematicDataItem) dataTable.getDataItem(cmpRowN);
			float cmpVals[] = new float[usd.getAttributes().size()];
			for (int i = 0; i < cmpVals.length; i++) {
				cmpVals[i] = (float) usd.getNumericAttrValue(dit, i);
			}
			usd.setCmpModeOn(cmpRowN, dit.getName(), cmpVals);
		}
		chObj.select(cmpRowN + 1);
		pm.setText(getPMText());
	}

	/**
	* The EventReceiver answers whether it is interested in getting the specified
	* kind of events. An ObjectComparison device is interested in receiving
	* object clicks events. It uses them for setting reference values for
	* visual comparison.
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
		if (usd == null || dataTable == null)
			return;
		if (evt instanceof ObjectEvent) {
			ObjectEvent oe = (ObjectEvent) evt;
			if (StringUtil.sameStrings(oe.getSetIdentifier(), dataTable.getEntitySetIdentifier()) && oe.getType().equals(ObjectEvent.click)) {
				int recN = -1;
				for (int i = 0; i < oe.getAffectedObjectCount() && recN < 0; i++) {
					String objId = oe.getObjectIdentifier(i);
					recN = dataTable.indexOf(objId);
				}
				compareToRowN(recN);
			}
		}
	}

	/*
	* Removes itself from consumers of object events
	*/
	@Override
	public void destroy() {
		if (usd != null) {
			usd.removeVisChangeListener(this);
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

	/**
	* Returns a unique identifier of the component
	* (used only internally, not shown to the user).
	* The identifier is used for explicit linking of producers and recipients of
	* object events.
	*/
	@Override
	public String getIdentifier() {
		return "Utility_Comparison_" + instanceN;
	}
}
