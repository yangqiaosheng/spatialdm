package spade.analysis.tools.schedule;

import java.util.Vector;

import spade.lib.util.StringUtil;
import spade.time.TimeMoment;
import spade.time.vis.TimeAndItemsSelectListener;
import spade.vis.action.Highlighter;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 12-Mar-2007
 * Time: 18:07:10
 * A non-UI class, which reacts to selections of vehicles and, possibly, time
 * intervals in various displays and selects corresponding records in a table
 * with transportation orders.
 */
public class VehicleSelector implements TimeAndItemsSelectListener {
	/**
	 * All data relevant to the schedule, in particular, the table with the
	 * transportation orders
	 */
	protected ScheduleData schData = null;
	/**
	 * Used to propagate selection events
	 */
	protected Highlighter highlighter = null;

	public void setHighlighter(Highlighter highlighter) {
		this.highlighter = highlighter;
	}

	public void setScheduleData(ScheduleData schData) {
		this.schData = schData;
	}

	/**
	 * Reacts to a simultaneous selection of a subset of vehicles (specified by their
	 * identifiers) and a time interval (specified by the start and end time moments)
	 */
	@Override
	public void selectionOccurred(Vector selItems, TimeMoment t1, TimeMoment t2) {
		if (selItems == null || selItems.size() < 1) {
			cancelSelection();
		}
		if (schData == null || schData.souTbl == null || highlighter == null)
			return;
		if (schData.vehicleIdColIdx < 0)
			return;
		Vector selRecIds = new Vector(50, 50);
		for (int i = 0; i < schData.souTbl.getDataItemCount(); i++) {
			String id = schData.souTbl.getAttrValueAsString(schData.vehicleIdColIdx, i);
			if (!StringUtil.isStringInVectorIgnoreCase(id, selItems)) {
				continue;
			}
			if (t1 != null && t2 != null && schData.ldd != null && schData.ldd.souTimeColIdx >= 0 && schData.ldd.destTimeColIdx >= 0) {
				Object val = schData.souTbl.getAttrValue(schData.ldd.souTimeColIdx, i);
				if (val == null || !(val instanceof TimeMoment)) {
					continue;
				}
				TimeMoment start = (TimeMoment) val;
				if (start.compareTo(t2) >= 0) {
					continue;
				}
				val = schData.souTbl.getAttrValue(schData.ldd.destTimeColIdx, i);
				if (val == null || !(val instanceof TimeMoment)) {
					continue;
				}
				TimeMoment end = (TimeMoment) val;
				if (end.compareTo(t1) <= 0) {
					continue;
				}
				selRecIds.addElement(schData.souTbl.getDataItemId(i));
			} else {
				selRecIds.addElement(schData.souTbl.getDataItemId(i));
			}
		}
		highlighter.replaceSelectedObjects(this, selRecIds);
	}

	/**
	 * Cancels previously made selection(s)
	 */
	@Override
	public void cancelSelection() {
		if (schData == null || schData.souTbl == null || highlighter == null)
			return;
		if (schData.vehicleIdColIdx < 0)
			return;
		highlighter.clearSelection(this);
	}
}
