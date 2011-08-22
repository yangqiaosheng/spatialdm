package spade.analysis.plot;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Choice;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import spade.analysis.system.Supervisor;
import spade.vis.database.SubstringAttrCondition;
import spade.vis.database.TableFilter;
import core.ActionDescr;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: Jan 19, 2010
 * Time: 4:12:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class SubstringAttrConditionUI extends Panel implements ActionListener, ItemListener {
	/**
	 * The condition controlled by this UI element
	 */
	protected SubstringAttrCondition cond = null;
	/**
	 * The filter in which this condition is included
	 */
	protected TableFilter filter = null;

	protected Supervisor supervisor = null;

	/**
	 * StringUI and QualUI exist in parallel, but only one of them is active.
	 * Both are activated when <clear filter> button is pressed etc.
	 * They update their text fields.
	 * Only the active UI updates statistics
	 */
	public boolean isActive = true;

	/**
	 * Choice for selecting text matching operations
	 */
	protected Choice chOper = null;
	/**
	 * The field where the user may enter the values he/she looks for.
	 */
	protected TextField tfValue = null;

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
	 * Constructs the UI. The condition cond must be previously constructed,
	 * i.e. the table and attribute must be specified.
	 */
	public SubstringAttrConditionUI(SubstringAttrCondition cond, TableFilter filter, Supervisor supervisor) {
		this.cond = cond;
		this.filter = filter;
		this.supervisor = supervisor;
		if (cond == null || filter == null || cond.getTable() == null || cond.getAttributeIndex() < 0)
			return;
		//filter.addAttrCondition(cond);
		setLayout(new BorderLayout());
		chOper = new Choice();
		chOper.addItemListener(this);
		chOper.addItem("contains");
		chOper.addItem("is equal to");
		chOper.addItem("starts with");
		chOper.addItem("ends with");
		chOper.addItem("contains list (comma-separated)");
		chOper.addItem("contains ordered list");
		tfValue = new TextField("<query string>", 50);
		tfValue.addActionListener(this);
		Button b = new Button("Apply");
		add(chOper, BorderLayout.WEST);
		add(tfValue, BorderLayout.CENTER);
		add(b, BorderLayout.EAST);
		b.addActionListener(this);
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
		updateOverallStatistics();
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

	protected void doFiltering() {
		filter.notifyFilterChange();
		if (supervisor != null && supervisor.getActionLogger() != null) {
			ActionDescr aDescr = new ActionDescr();
			aDescr.aName = "Dynamic Query: selected attribute values";
			aDescr.addParamValue("Table id", cond.getTable().getContainerIdentifier());
			aDescr.addParamValue("Table name", cond.getTable().getName());
			aDescr.addParamValue("Attribute", cond.getTable().getAttributeName(cond.getAttributeIndex()));
			aDescr.addParamValue("String operation", cond.getConditionsAsString());
			aDescr.startTime = System.currentTimeMillis();
			supervisor.getActionLogger().logAction(aDescr);
		}
		updateStatistics();
		updateOverallStatistics();
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		cond.setCondParams(tfValue.getText(), chOper.getSelectedIndex());
		doFiltering();
	}

	@Override
	public void itemStateChanged(ItemEvent ie) {
		cond.setCondParams(tfValue.getText(), chOper.getSelectedIndex());
		doFiltering();
	}

	/**
	 * Since DynamicQuery always does complete filter clearing (even if
	 * numeric attributes are not used in a query), QualAttrConditionUI
	 * is only notified about the filter being cleared, in order to
	 * reflect this in the UI.
	 */
	public void filterCleared() {
		tfValue.setText("");
		updateStatistics();
		updateOverallStatistics();
	}

}
