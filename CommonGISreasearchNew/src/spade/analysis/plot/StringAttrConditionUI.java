package spade.analysis.plot;

import java.awt.BorderLayout;
import java.awt.Choice;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import spade.analysis.system.Supervisor;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.Destroyable;
import spade.vis.database.AttrCondition;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.QualAttrCondition;
import spade.vis.database.SubstringAttrCondition;
import spade.vis.database.TableFilter;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: Jan 19, 2010
 * Time: 4:59:51 PM
 * To change this template use File | Settings | File Templates.
 */
public class StringAttrConditionUI extends Panel implements ItemListener, Destroyable {

	protected Supervisor supervisor = null;
	protected AttributeDataPortion table = null;
	protected int colN = -1;
	protected TableFilter filter = null;

	/**
	 * A display of statistics of query satisfaction
	 */
	protected DynamicQueryStat dqs = null;
	/**
	 * The index of the item in the DynamicQueryStat corresponding to this
	 * condition
	 */
	protected int dqsIdx = 0;

	SubstringAttrCondition sac = null;
	QualAttrCondition qac = null;
	SubstringAttrConditionUI sacUI = null;
	QualAttrConditionUI qacUI = null;

	protected Choice chCondType = null;

	public StringAttrConditionUI(AttributeDataPortion table, int colN, TableFilter filter, Supervisor supervisor) {
		this.table = table;
		this.colN = colN;
		this.filter = filter;
		this.supervisor = supervisor;

		setLayout(new BorderLayout());
		chCondType = new Choice();
		chCondType.addItem("substr");
		chCondType.addItem("list");
		chCondType.select(0);
		chCondType.addItemListener(this);

		setupCondition();
	}

	public AttrCondition getCondition() {
		if (chCondType.getSelectedIndex() == 0)
			return sac;
		else
			return qac;
	}

	public int getAttrIndex() {
		return colN;
	}

	/**
	 * Sets a reference to a display of statistics of query satisfaction
	 */
	public void setDQS(DynamicQueryStat dqs) {
		this.dqs = dqs;
		if (sacUI != null) {
			sacUI.setDQS(dqs);
		}
		if (qacUI != null) {
			qacUI.setDQS(dqs);
		}
	}

	/**
	 * Sets the index of the item in the DynamicQueryStat corresponding to this
	 * condition
	 */
	public void setDQSIndex(int idx) {
		dqsIdx = idx;
		if (sacUI != null) {
			sacUI.setDQSIndex(dqsIdx);
		}
		if (qacUI != null) {
			qacUI.setDQSIndex(dqsIdx);
		}
	}

	public void setFilterOutMissingValues(boolean toFilter) {
		if (sacUI != null) {
			sacUI.updateStatistics();
		}
		if (qac != null) {
			qacUI.updateStatistics();
		}
	}

	public void setupCondition() {
		removeAll();
		add(new Label(table.getAttributeName(colN)), BorderLayout.NORTH);
		add(chCondType, BorderLayout.WEST);
		if (chCondType.getSelectedIndex() == 0) {
			if (sac == null) { // create SubstringAttrCondition
				sac = new SubstringAttrCondition();
				sac.setTable(table);
				sac.setAttributeIndex(colN);
				sacUI = new SubstringAttrConditionUI(sac, filter, supervisor);
				sacUI.setDQS(dqs);
				sacUI.setDQSIndex(dqsIdx);
			}
			// make SubstringAttrCondition active
			if (filter.getConditionIndex(colN) >= 0) {
				filter.removeAttrCondition(filter.getConditionIndex(colN));
			}
			filter.addAttrCondition(sac);
			add(sacUI, BorderLayout.CENTER); // activate SubstringAttrConditionUI
			sacUI.isActive = true;
			if (qacUI != null) {
				qacUI.isActive = false;
			}
		} else {
			if (qac == null) { // create QualAttrCondition
				qac = new QualAttrCondition();
				qac.setTable(table);
				qac.setAttributeIndex(colN);
				qacUI = new QualAttrConditionUI(qac, filter, supervisor);
				qacUI.setDQS(dqs);
				qacUI.setDQSIndex(dqsIdx);
			}
			// make SubstringAttrCondition active
			if (filter.getConditionIndex(colN) >= 0) {
				filter.removeAttrCondition(filter.getConditionIndex(colN));
			}
			filter.addAttrCondition(qac);
			add(qacUI, BorderLayout.CENTER);//activate QualAttrConditionUI
			if (sacUI != null) {
				sacUI.isActive = false;
			}
			qacUI.isActive = true;
		}
		CManager.invalidateAll(this);
		CManager.validateAll(this);
	}

	@Override
	public void itemStateChanged(ItemEvent ie) {
		setupCondition();
	}

	/**
	 * Since DynamicQuery always does complete filter clearing (even if
	 * numeric attributes are not used in a query), QualAttrConditionUI
	 * is only notified about the filter being cleared, in order to
	 * reflect this in the UI.
	 */
	public void filterCleared() {
		if (sacUI != null) {
			sacUI.filterCleared();
		}
		if (qacUI != null) {
			qacUI.filterCleared();
		}
	}

	protected boolean destroyed = false;

	@Override
	public void destroy() {
		if (sac != null) {
			filter.removeQueryAttribute(sac.getAttributeIndex());
		}
		destroyed = true;
	}

	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

}
