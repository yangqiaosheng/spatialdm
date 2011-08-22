package spade.analysis.plot;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import spade.analysis.system.Supervisor;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.MultiSelector;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.TextCanvas;
import spade.lib.util.StringUtil;
import spade.vis.database.QualAttrCondition;
import spade.vis.database.TableFilter;
import core.ActionDescr;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 09-Jan-2007
 * Time: 11:22:03
 * A UI element allowing the user to specify a query condition for a qualitative
 * attribute.
 */
public class QualAttrConditionUI extends Panel implements ActionListener, Destroyable {
	/**
	 * The condition controlled by this UI element
	 */
	protected QualAttrCondition cond = null;
	/**
	 * The filter in which this condition is included
	 */
	protected TableFilter filter = null;

	protected Supervisor supervisor = null;
	/**
	 * The field where the user may enter the values he/she looks for.
	 */
	protected TextField valueTF = null;
	/**
	 * A display of statistics of query satisfaction
	 */
	protected DynamicQueryStat dqs = null;
	/**
	 * The index of the item in the DynamicQueryStat corresponding to this
	 * condition
	 */
	protected int dqsIdx = 0;

	/**
	 * StringUI and QualUI exist in parallel, but only one of them is active.
	 * Both are activated when <clear filter> button is pressed etc.
	 * They update their text fields.
	 * Only the active UI updates statistics
	 */
	public boolean isActive = false;

	/**
	 * Constructs the UI. The condition cond must be previously constructed,
	 * i.e. the table and attribute must be specified.
	 */
	public QualAttrConditionUI(QualAttrCondition cond, TableFilter filter, Supervisor supervisor) {
		this.cond = cond;
		this.filter = filter;
		this.supervisor = supervisor;
		if (cond == null || filter == null || cond.getTable() == null || cond.getAttributeIndex() < 0)
			return;
		//filter.addAttrCondition(cond);
		Panel p = new Panel(new ColumnLayout());
		valueTF = new TextField("<not limited>", 50);
		valueTF.addActionListener(this);
		p.add(valueTF);
		setLayout(new BorderLayout());
		add(p, BorderLayout.CENTER);
		Button b = new Button("Change");
		b.setActionCommand("change");
		b.addActionListener(this);
		p = new Panel(new BorderLayout());
		p.add(b, BorderLayout.SOUTH);
		add(p, BorderLayout.EAST);
	}

	public QualAttrCondition getCondition() {
		return cond;
	}

	public int getAttrIndex() {
		return cond.getAttributeIndex();
	}

	/**
	 * Sets a reference to a display of statistics of query satisfaction
	 */
	public void setDQS(DynamicQueryStat dqs) {
		this.dqs = dqs;
	}

	/**
	 * Sets the index of the item in the DynamicQueryStat corresponding to this
	 * condition
	 */
	public void setDQSIndex(int idx) {
		dqsIdx = idx;
		updateStatistics();
	}

	/**
	 * Sends the up-to-date statistics of satisfaction of this condition to the
	 * DynamicQueryStat
	 */
	public void updateStatistics() {
		if (dqs == null || filter == null || !isActive)
			return;
		dqs.setObjectNumber(cond.getTable().getDataItemCount());
		int attrIdx = cond.getAttributeIndex();
		dqs.setNumbers(dqsIdx, filter.getNSatisfying(attrIdx), filter.getNMissingValues(attrIdx));
	}

	protected void updateOverallStatistics() {
		if (dqs != null && filter != null && isActive) {
			dqs.setNumbers(filter.getNQueryAttributes(), filter.getNSatisfying(), filter.getNMissingValues());
		}
	}

	public void setFilterOutMissingValues(boolean toFilter) {
		if (toFilter != cond.getMissingValuesOK()) {
			cond.setMissingValuesOK(!toFilter);
			updateStatistics();
		}
	}

	protected void clearTF() {
		valueTF.setText("<not limited>");
	}

	protected void clearFilterCondition() {
		clearTF();
		cond.clearLimits();
		filter.notifyFilterChange();
		if (supervisor != null && supervisor.getActionLogger() != null) {
			ActionDescr aDescr = new ActionDescr();
			aDescr.aName = "Dynamic Query: clear filter condition";
			aDescr.addParamValue("Table id", cond.getTable().getContainerIdentifier());
			aDescr.addParamValue("Table name", cond.getTable().getName());
			aDescr.addParamValue("Attribute", cond.getTable().getAttributeName(cond.getAttributeIndex()));
			aDescr.startTime = System.currentTimeMillis();
			supervisor.getActionLogger().logAction(aDescr);
		}
		updateStatistics();
		updateOverallStatistics();
	}

	protected void displayValuesInTF(Vector rightValues) {
		if (rightValues != null && rightValues.size() > 0) {
			String txt = "\"" + (String) rightValues.elementAt(0) + "\"";
			for (int i = 1; i < rightValues.size(); i++) {
				txt += "; \"" + (String) rightValues.elementAt(i) + "\"";
			}
			valueTF.setText(txt);
		} else {
			clearTF();
		}
	}

	protected void showError(String errMsg) {
		TextCanvas tc = new TextCanvas();
		tc.setText(errMsg);
		OKDialog okd = new OKDialog(CManager.getAnyFrame(this), "Error!", false);
		okd.addContent(tc);
		okd.show();
	}

	protected boolean sameValues(Vector values1, Vector values2) {
		if (values1 == null)
			return values2 == null;
		if (values2 == null)
			return false;
		if (values1.size() < 1)
			return values2.size() < 1;
		if (values2.size() < 1)
			return false;
		boolean found[] = new boolean[values2.size()];
		for (int i = 0; i < found.length; i++) {
			found[i] = false;
		}
		for (int i = 0; i < values1.size(); i++) {
			int idx = StringUtil.indexOfStringInVectorIgnoreCase((String) values1.elementAt(i), values2);
			if (idx >= 0) {
				found[idx] = true;
			} else
				return false;
		}
		for (int i = 0; i < found.length; i++)
			if (!found[i] && !StringUtil.isStringInVectorIgnoreCase((String) values2.elementAt(i), values1))
				return false;
		return true;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource().equals(valueTF)) {
			Vector allValues = cond.getAllUniqueValues();
			if (allValues == null) {
				showError("No attribute values found!");
				return;
			}
			String txt = valueTF.getText();
			if (txt == null || txt.trim().length() < 1) {
				clearFilterCondition();
				return;
			}
			Vector selValues = StringUtil.getNames(txt, ";, ", false);
			if (selValues == null || selValues.size() < 1) {
				showError("Illegal format! You are expected to enter strings " + "(possibly, in quotes) separated by ';' or ','.");
				displayValuesInTF(cond.getRightValues());
				return;
			}
			Vector rightValues = cond.getRightValues();
			if (sameValues(selValues, rightValues)) {
				displayValuesInTF(rightValues);
				return;
			}
			for (int i = selValues.size() - 1; i >= 0; i--)
				if (!StringUtil.isStringInVectorIgnoreCase((String) selValues.elementAt(i), allValues)) {
					showError("Unknown value: [" + (String) selValues.elementAt(i) + "]");
					selValues.removeElementAt(i);
				}
			if (selValues.size() < 1) {
				displayValuesInTF(rightValues);
				return;
			}
			displayValuesInTF(selValues);
			cond.setRightValues(selValues);
			filter.notifyFilterChange();
			if (supervisor != null && supervisor.getActionLogger() != null) {
				ActionDescr aDescr = new ActionDescr();
				aDescr.aName = "Dynamic Query: selected attribute values";
				aDescr.addParamValue("Table id", cond.getTable().getContainerIdentifier());
				aDescr.addParamValue("Table name", cond.getTable().getName());
				aDescr.addParamValue("Attribute", cond.getTable().getAttributeName(cond.getAttributeIndex()));
				aDescr.addParamValue("Values", selValues);
				aDescr.startTime = System.currentTimeMillis();
				supervisor.getActionLogger().logAction(aDescr);
			}
			updateStatistics();
			updateOverallStatistics();
		} else if (e.getActionCommand().equals("change")) {
			Vector values = cond.getAllUniqueValues();
			if (values == null) {
				showError("No attribute values found!");
				return;
			}
			MultiSelector msel = new MultiSelector(values, false);
			Vector rightValues = cond.getRightValues();
			if (rightValues != null && rightValues.size() > 0) {
				int selIdxs[] = new int[rightValues.size()];
				int k = 0;
				for (int i = 0; i < values.size(); i++)
					if (StringUtil.isStringInVectorIgnoreCase((String) values.elementAt(i), rightValues)) {
						selIdxs[k++] = i;
					}
				msel.selectItems(selIdxs);
			}
			OKDialog okd = new OKDialog(CManager.getAnyFrame(this), "Select attribute values", true);
			okd.addContent(msel);
			okd.show();
			if (!okd.wasCancelled()) {
				String result[] = msel.getSelectedItems();
				if (result == null || result.length < 1) {
					rightValues = null;
				} else {
					rightValues = new Vector(result.length, 1);
					for (String element : result) {
						rightValues.addElement(element);
					}
				}
			}
			displayValuesInTF(rightValues);
			if (okd.wasCancelled())
				return;
			cond.setRightValues(rightValues);
			filter.notifyFilterChange();
			if (supervisor != null && supervisor.getActionLogger() != null) {
				ActionDescr aDescr = new ActionDescr();
				aDescr.aName = "Dynamic Query: selected attribute values";
				aDescr.addParamValue("Table id", cond.getTable().getContainerIdentifier());
				aDescr.addParamValue("Table name", cond.getTable().getName());
				aDescr.addParamValue("Attribute", cond.getTable().getAttributeName(cond.getAttributeIndex()));
				aDescr.addParamValue("Values", rightValues);
				aDescr.startTime = System.currentTimeMillis();
				supervisor.getActionLogger().logAction(aDescr);
			}
			updateStatistics();
			updateOverallStatistics();
		}
	}

	/**
	 * Since DynamicQuery always does complete filter clearing (even if
	 * numeric attributes are not used in a query), QualAttrConditionUI
	 * is only notified about the filter being cleared, in order to
	 * reflect this in the UI.
	 */
	public void filterCleared() {
		clearTF();
		updateStatistics();
	}

	protected boolean destroyed = false;

	@Override
	public void destroy() {
		filter.removeQueryAttribute(cond.getAttributeIndex());
		destroyed = true;
	}

	@Override
	public boolean isDestroyed() {
		return destroyed;
	}
}
