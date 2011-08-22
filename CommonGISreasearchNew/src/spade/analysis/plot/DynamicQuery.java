package spade.analysis.plot;

import java.awt.Canvas;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Vector;

import spade.analysis.calc.AttrNameEditor;
import spade.analysis.system.Supervisor;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Focuser;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.Metrics;
import spade.lib.basicwin.OKDialog;
import spade.lib.lang.Language;
import spade.time.TimeMoment;
import spade.vis.action.ObjectEvent;
import spade.vis.action.ObjectEventHandler;
import spade.vis.database.AttrCondition;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.AttributeTypes;
import spade.vis.database.CombinedFilter;
import spade.vis.database.Condition;
import spade.vis.database.ConditionTypeRegister;
import spade.vis.database.DataTable;
import spade.vis.database.NumAttrCondition;
import spade.vis.database.TableFilter;
import spade.vis.spec.ConditionSpec;
import core.ActionDescr;

/**
* A class that allows to set dynamically constraints on values of several
* numeric attributes (i.e. minimum and maximum values). In response, the
* system removes from all the currently visible displays the objects with
* attribute values that do not satisfy these constraints.
* The data to represent are taken from an AttributeDataPortion.
*/

public class DynamicQuery extends Plot implements ActionListener, ObjectEventHandler {
	/**
	* Used to generate unique identifiers of instances of DynamicQuery
	*/
	static ResourceBundle res = Language.getTextResource("spade.analysis.plot.Res");
	protected static int nInstances = 0;

	protected int nSaved = 0;
	protected String idSaved = null;

	public static String saveQueryCmd = "saveQuery";

	protected TableFilter filter = null;

	protected int fn[] = null; // field numbers

	public int[] getFn() {
		return fn;
	}

	public int getFieldCount() {
		if (fn == null)
			return 0;
		return fn.length;
	}

	protected double AbsMin[] = null, AbsMax[] = null;
	/**
	 * Some attributes may be temporal. For such attributes, these arrays
	 * contain the absolute minimum and maximum time moments, respectively.
	 * These arrays are created once for the whole table. The length of each
	 * array equals the number of attributes in the table. This allows to avoid
	 * re-setting the minimum and maximum times when the attributes used in the
	 * query change.
	 */
	protected TimeMoment timeAbsMin[] = null, timeAbsMax[] = null;

	protected boolean isHorisontal = true;

	//protected DotPlot dp[]=null;
	protected DensityPlot dp[] = null;
	protected TextField tfmin[] = null, tfmax[] = null;
	protected Label lmin[] = null, lmax[] = null;

	public TextField[] getTFMin() {
		return tfmin;
	}

	public TextField[] getTFMax() {
		return tfmax;
	}

	public Label[] getLMin() {
		return lmin;
	}

	public Label[] getLMax() {
		return lmax;
	}

	/**
	 * A display of statistics of query satisfaction
	 */
	protected DynamicQueryStat dqs = null;
	/**
	 * The index of the first item of the DynamicQueryStat corresponding to this
	 * DynamicQuery (the other items may correspond to other query conditions)
	 */
	protected int dqsStartIdx = 0;

	/**
	 * Sets a reference to a display of statistics of query satisfaction
	 */
	public void setDQS(DynamicQueryStat dqs) {
		this.dqs = dqs;
	}

	/**
	 * Sets the index of the first item of the DynamicQueryStat corresponding to
	 * this DynamicQuery (the other items may correspond to other query conditions)
	 */
	public void setDQSStartIndex(int idx) {
		dqsStartIdx = idx;
	}

	protected boolean FOutMV = false; // interpretation of missing values: true -> filter out

	public void setFOutMV(boolean FOutMV) {
		if (this.FOutMV == FOutMV)
			return;
		this.FOutMV = FOutMV;
		filter.setFilterOutMissingValues(FOutMV);
		filter.notifyFilterChange();
		if (supervisor != null && supervisor.getActionLogger() != null) {
			ActionDescr aDescr = new ActionDescr();
			aDescr.aName = "Dynamic Query: " + ((FOutMV) ? "filter out missing values" : "ignore missing values");
			aDescr.addParamValue("Table id", dataTable.getContainerIdentifier());
			aDescr.addParamValue("Table name", dataTable.getName());
			aDescr.startTime = System.currentTimeMillis();
			supervisor.getActionLogger().logAction(aDescr);
		}
		updateStatistics();
	}

	public boolean getFOutMV() {
		return FOutMV;
	}

	protected void updateStatistics() {
		if (dqs == null || dataTable == null || filter == null)
			return;
		if (idSaved != null) {
			DataTable dTable = (DataTable) dataTable;
			int idx = dTable.getAttrIndex(idSaved);
			for (int i = 0; i < dTable.getDataItemCount(); i++) {
				dTable.getDataRecord(i).setAttrValue((filter.isActive(i)) ? "y" : "n", idx);
			}
			Vector resultAttr = new Vector(1, 1);
			resultAttr.addElement(new String(idSaved));
			// inform all displays about change of values
			dTable.notifyPropertyChange("values", null, resultAttr);
		}
		dqs.setObjectNumber(dataTable.getDataItemCount());
		if (fn != null) {
			for (int i = 0; i < fn.length; i++) {
				dqs.setNumbers(dqsStartIdx + i, filter.getNSatisfying(fn[i]), filter.getNMissingValues(fn[i]));
			}
		}
		dqs.setNumbers(filter.getNQueryAttributes(), filter.getNSatisfying(), filter.getNMissingValues());
	}

	/**
	* Constructs a DynamicQuery.
	* The variable allowSelection shows whether the plot should listen to
	* mouse events and transform them to object selection events.
	* Supervisor provides access of a plot to the Highlighter (common for
	* all data displays) and in this way links together all displays
	*/
	public DynamicQuery(boolean allowSelection, Supervisor sup) {
		super(true, allowSelection, sup, sup);
	}

	@Override
	public void setDataSource(AttributeDataPortion tbl) {
		super.setDataSource(tbl);
		if (tf instanceof TableFilter) {
			filter = (TableFilter) tf;
		} else if (tf instanceof CombinedFilter) {
			CombinedFilter cFilter = (CombinedFilter) tf;
			for (int i = 0; i < cFilter.getFilterCount(); i++)
				if (cFilter.getFilter(i) instanceof TableFilter) {
					filter = (TableFilter) cFilter.getFilter(i);
				}
		}
	}

	public void setFn(int fn[], TextField tfmin[], TextField tfmax[], Label lmin[], Label lmax[]) {
		this.fn = fn;
		this.tfmin = tfmin;
		this.tfmax = tfmax;
		this.lmin = lmin;
		this.lmax = lmax;
	}

	/**
	* Resets its internal data. * Called when records are added to or removed from
	* the table.
	*/
	@Override
	public void reset() {
		setup();
		redraw();
	}

	public void setup() {
		if (dataTable == null || fn.length == 0)
			return;

		if (dots == null || dots.length != dataTable.getDataItemCount()) {
			dots = new DotPlotObject[dataTable.getDataItemCount()];
			for (int i = 0; i < dots.length; i++) {
				dots[i] = new DotPlotObject();
			}
		}
		if (AbsMin == null || AbsMin.length != fn.length || AbsMax == null || AbsMax.length != fn.length) {
			AbsMin = new double[fn.length];
			AbsMax = new double[fn.length];
			for (int j = 0; j < fn.length; j++) {
				AbsMin[j] = Double.NaN;
				AbsMax[j] = Double.NaN;
			}
		}
		for (int i = 0; i < dots.length; i++) {
			dots[i].reset();
			dots[i].id = dataTable.getDataItemId(i);
			for (int j = 0; j < fn.length; j++) {
				double v = dataTable.getNumericAttrValue(fn[j], i);
				if (!Double.isNaN(v)) {
					if (Double.isNaN(AbsMin[j]) || v < AbsMin[j]) {
						AbsMin[j] = v;
					}
					if (Double.isNaN(AbsMax[j]) || v > AbsMax[j]) {
						AbsMax[j] = v;
					}
				}
			}
		}
		setupTimes();
		if (dp == null || dp.length < 1) {
			constructDotPlots();
		} else {
			for (int j = 0; j < fn.length; j++) {
				if (AbsMin[j] != dp[j].getFocuser().getAbsMin() || AbsMax[j] != dp[j].getFocuser().getAbsMax()) {
					dp[j].getFocuser().setAbsMinMax(AbsMin[j], AbsMax[j]);
				}
				if (timeAbsMin != null && timeAbsMin[fn[j]] != null) {
					dp[j].getFocuser().setAbsMinMaxTime(timeAbsMin[fn[j]], timeAbsMax[fn[j]]);
				}
			}
		}
		for (int j = 0; j < fn.length; j++) {
			lmin[j].setText(getAbsMinText(j));
			tfmin[j].setText(getCurrMinText(j));
			lmax[j].setText(getAbsMaxText(j));
			tfmax[j].setText(getCurrMaxText(j));
		}
		for (int element : fn) {
			filter.addQueryAttribute(element);
		}
		updateStatistics();
	}

	/**
	 * Some attributes may be temporal. For such attributes, constructs the arrays
	 * timeAbsMin and timeAbsMax, which contain the absolute minimum and maximum
	 * time moments, respectively.
	 * These arrays are created once for the whole table. The length of each
	 * array equals the number of attributes in the table. This allows to avoid
	 * re-setting the minimum and maximum times when the attributes used in the
	 * query change.
	 */
	private void setupTimes() {
		if (fn == null || fn.length < 1 || dataTable == null)
			return;
		for (int j = 0; j < fn.length; j++)
			if (dataTable.isAttributeTemporal(fn[j]) && (timeAbsMin == null || timeAbsMin[fn[j]] == null)) {
				//try to find the minimum and maximum times for this attribute
				TimeMoment minTime = null, maxTime = null;
				for (int i = 0; i < dataTable.getDataItemCount(); i++) {
					Object val = dataTable.getAttrValue(fn[j], i);
					if (val == null || !(val instanceof TimeMoment)) {
						continue;
					}
					TimeMoment t = (TimeMoment) val;
					if (minTime == null || minTime.compareTo(t) > 0) {
						minTime = t;
					}
					if (maxTime == null || maxTime.compareTo(t) < 0) {
						maxTime = t;
					}
				}
				if (minTime != null && maxTime != null) {
					if (timeAbsMin == null) {
						int len = dataTable.getAttrCount();
						timeAbsMin = new TimeMoment[len];
						timeAbsMax = new TimeMoment[len];
						for (int k = 0; k < len; k++) {
							timeAbsMin[k] = null;
							timeAbsMax[k] = null;
						}
					}
					timeAbsMin[fn[j]] = minTime.getCopy();
					timeAbsMax[fn[j]] = maxTime.getCopy();
				}
			}
	}

	private String getAbsMinText(int idx) {
		if (timeAbsMin == null || timeAbsMin[fn[idx]] == null)
			return String.valueOf(AbsMin[idx]);
		return timeAbsMin[fn[idx]].toString();
	}

	private String getAbsMaxText(int idx) {
		if (timeAbsMax == null || timeAbsMax[fn[idx]] == null)
			return String.valueOf(AbsMax[idx]);
		return timeAbsMax[fn[idx]].toString();
	}

	private TimeMoment getAttrValAsTime(double value, int idx) {
		if (timeAbsMin == null || timeAbsMin[fn[idx]] == null)
			return null;
		return timeAbsMin[fn[idx]].valueOf((long) value);
	}

	private String getCurrMinText(int idx) {
		double min = dp[idx].getFocuser().getCurrMin();
		TimeMoment minT = getAttrValAsTime(min, idx);
		if (minT == null)
			return String.valueOf(min);
		return minT.toString();
	}

	private String getCurrMaxText(int idx) {
		double max = dp[idx].getFocuser().getCurrMax();
		TimeMoment maxT = getAttrValAsTime(max, idx);
		if (maxT == null)
			return String.valueOf(max);
		return maxT.toString();
	}

	public TableFilter getFilter() {
		return filter;
	}

	public void clearAllFilters() {
		if (filter == null)
			return;
		filter.clearFilter();
		filter.notifyFilterChange();
		if (supervisor != null && supervisor.getActionLogger() != null) {
			ActionDescr aDescr = new ActionDescr();
			aDescr.aName = "Dynamic Query: clear filter";
			aDescr.addParamValue("Table id", dataTable.getContainerIdentifier());
			aDescr.addParamValue("Table name", dataTable.getName());
			aDescr.startTime = System.currentTimeMillis();
			supervisor.getActionLogger().logAction(aDescr);
		}
		if (fn != null && dp != null) {
			for (int j = 0; j < fn.length; j++)
				if (dp[j].getFocuser().getCurrMin() != AbsMin[j] || dp[j].getFocuser().getCurrMax() != AbsMax[j]) {
					dp[j].getFocuser().setCurrMinMax(AbsMin[j], AbsMax[j]);
				}
		}
		updateStatistics();
	}

	/**
	* Sets query conditions according to the given specification. The elements of
	* the vector are instances of spade.vis.spec.ConditionSpec.
	*/
	public void setConditions(Vector condSpecs) {
		if (condSpecs == null || condSpecs.size() < 1)
			return;
		if (filter == null || dataTable == null)
			return;
		Vector conditions = new Vector(condSpecs.size(), 1);
		boolean missingValOK = true;
		for (int i = 0; i < condSpecs.size(); i++) {
			ConditionSpec csp = (ConditionSpec) condSpecs.elementAt(i);
			if (csp != null && csp.type != null && csp.description != null && csp.description.size() > 0) {
				Condition cond = ConditionTypeRegister.constructCondition(csp.type);
				if (cond != null && (cond instanceof AttrCondition)) {
					cond.setTable(dataTable);
					cond.setup(csp.description);
					int attrIdx = ((AttrCondition) cond).getAttributeIndex();
					if (attrIdx >= 0) {
						conditions.addElement(cond);
						missingValOK = missingValOK && cond.getMissingValuesOK();
					}
				}
			}
		}
		if (conditions.size() < 1)
			return;
		Vector toAdd = new Vector(conditions.size(), 1); //if the attributes are not in the query yet
		for (int i = 0; i < conditions.size(); i++) {
			AttrCondition cond = (AttrCondition) conditions.elementAt(i);
			int attrIdx = cond.getAttributeIndex();
			//check if there is already a control for this attribute in the query
			if (!hasAttribute(attrIdx)) {
				toAdd.addElement(cond);
			}
		}
		if (toAdd.size() > 0) {
			int oldN = 0;
			if (fn != null) {
				oldN = fn.length;
			}
			//add new attributes to the query and create corresponding UI controls
			int newFn[] = new int[oldN + toAdd.size()];
			double newAbsMin[] = new double[oldN + toAdd.size()], newAbsMax[] = new double[oldN + toAdd.size()];
			Label newLMin[] = new Label[oldN + toAdd.size()], newLMax[] = new Label[oldN + toAdd.size()];
			TextField newTFMin[] = new TextField[oldN + toAdd.size()], newTFMax[] = new TextField[oldN + toAdd.size()];
//      DotPlot newDP[]=new DotPlot[oldN+toAdd.size()];
			DensityPlot newDP[] = new DensityPlot[oldN + toAdd.size()];
			for (int i = 0; i < oldN; i++) {
				newFn[i] = fn[i];
				newAbsMin[i] = AbsMin[i];
				newAbsMax[i] = AbsMax[i];
				newLMin[i] = lmin[i];
				newLMax[i] = lmax[i];
				newTFMin[i] = tfmin[i];
				newTFMax[i] = tfmax[i];
				newDP[i] = dp[i];
			}
			for (int i = oldN; i < newFn.length; i++) {
				AttrCondition cond = (AttrCondition) conditions.elementAt(i - oldN);
				newFn[i] = cond.getAttributeIndex();
				newAbsMin[i] = Double.NaN;
				newAbsMax[i] = Double.NaN;
				newLMin[i] = new Label("", Label.CENTER);
				newLMax[i] = new Label("", Label.CENTER);
				newTFMin[i] = new TextField("", 7);
				newTFMax[i] = new TextField("", 7);
			}
			for (int i = 0; i < dots.length; i++) {
				for (int j = oldN; j < newFn.length; j++) {
					double v = dataTable.getNumericAttrValue(newFn[j], i);
					if (!Double.isNaN(v)) {
						if (Double.isNaN(newAbsMin[j]) || v < newAbsMin[j]) {
							newAbsMin[j] = v;
						}
						if (Double.isNaN(newAbsMax[j]) || v > newAbsMax[j]) {
							newAbsMax[j] = v;
						}
					}
				}
			}
			fn = newFn;
			AbsMin = newAbsMin;
			AbsMax = newAbsMax;
			setupTimes();
			lmin = newLMin;
			lmax = newLMax;
			tfmin = newTFMin;
			tfmax = newTFMax;
			dp = newDP;
			for (int j = oldN; j < newFn.length; j++) {
				String str = getAbsMinText(j);
				tfmin[j].setText(str);
				lmin[j].setText(str);
				str = getAbsMaxText(j);
				tfmax[j].setText(str);
				lmax[j].setText(str);
				//constructDotPlot(j);
				constructDensityPlot(j);
				dp[j].setCanvas(canvas);
				dqs.addAttr();
			}
			if (supervisor != null) {
				supervisor.notifyGlobalPropertyChange(Supervisor.eventDisplayedAttrs);
			}
		}
		filter.clearFilter();
		for (int i = 0; i < conditions.size(); i++) {
			AttrCondition cond = (AttrCondition) conditions.elementAt(i);
			int attrIdx = cond.getAttributeIndex();
			int n = -1;
			for (int j = 0; j < fn.length && n < 0; j++)
				if (fn[j] == attrIdx) {
					n = j;
				}
			cond.setMissingValuesOK(missingValOK);
			filter.addAttrCondition(cond);
			if (conditions.elementAt(i) instanceof NumAttrCondition) {
				NumAttrCondition ncond = (NumAttrCondition) conditions.elementAt(i);
				double low = ncond.getMinLimit(), high = ncond.getMaxLimit();
				if (!Double.isNaN(low) || !Double.isNaN(high)) {
					dp[n].getFocuser().setCurrMinMax(low, high);
				}
			}
		}
		FOutMV = !missingValOK;
		filter.setFilterOutMissingValues(FOutMV);
		filter.notifyFilterChange();
		if (supervisor != null && supervisor.getActionLogger() != null) {
			ActionDescr aDescr = new ActionDescr();
			aDescr.aName = "Dynamic Query";
			aDescr.addParamValue("Table id", dataTable.getContainerIdentifier());
			aDescr.addParamValue("Table name", dataTable.getName());
			aDescr.addParamValue("Filter out missing values", new Boolean(FOutMV));
			for (int i = 0; i < conditions.size(); i++) {
				AttrCondition cond = (AttrCondition) conditions.elementAt(i);
				int attrIdx = cond.getAttributeIndex();
				Hashtable params = cond.getDescription();
				if (params != null && !params.isEmpty()) {
					Set keys = params.keySet();
					if (keys != null) {
						for (Iterator it = keys.iterator(); it.hasNext();) {
							Object key = it.next(), value = params.get(key);
							if (key != null && value != null) {
								aDescr.addParamValue(key.toString(), value);
							}
						}
					}
				}
			}
			aDescr.startTime = System.currentTimeMillis();
			supervisor.getActionLogger().logAction(aDescr);
		}
		updateStatistics();
	}

	public boolean removeAttributeFromQuery(int n) {
		if (n < 0 || n >= fn.length)
			return false;
		filter.removeQueryAttribute(fn[n]);
		filter.notifyFilterChange();
		if (supervisor != null && supervisor.getActionLogger() != null) {
			ActionDescr aDescr = new ActionDescr();
			aDescr.aName = "Dynamic Query: remove attribute";
			aDescr.addParamValue("Table id", dataTable.getContainerIdentifier());
			aDescr.addParamValue("Table name", dataTable.getName());
			aDescr.addParamValue("Attribute", dataTable.getAttributeName(fn[n]));
			aDescr.startTime = System.currentTimeMillis();
			supervisor.getActionLogger().logAction(aDescr);
		}
		int newFn[] = new int[fn.length - 1];
		double newAbsMin[] = new double[fn.length - 1], newAbsMax[] = new double[fn.length - 1];
		Label newLMin[] = new Label[fn.length - 1], newLMax[] = new Label[fn.length - 1];
		TextField newTFMin[] = new TextField[fn.length - 1], newTFMax[] = new TextField[fn.length - 1];
//    DotPlot newDP[]=new DotPlot[fn.length-1];
		DensityPlot newDP[] = new DensityPlot[fn.length - 1];
		dp[n].destroy();
		for (int i = 0; i < fn.length; i++)
			if (i == n) {
				continue;
			} else if (i < n) {
				newFn[i] = fn[i];
				newAbsMin[i] = AbsMin[i];
				newAbsMax[i] = AbsMax[i];
				newLMin[i] = lmin[i];
				newLMax[i] = lmax[i];
				newTFMin[i] = tfmin[i];
				newTFMax[i] = tfmax[i];
				newDP[i] = dp[i];
			} else {
				newFn[i - 1] = fn[i];
				newAbsMin[i - 1] = AbsMin[i];
				newAbsMax[i - 1] = AbsMax[i];
				newLMin[i - 1] = lmin[i];
				newLMax[i - 1] = lmax[i];
				newTFMin[i - 1] = tfmin[i];
				newTFMax[i - 1] = tfmax[i];
				newDP[i - 1] = dp[i];
			}
		fn = newFn;
		AbsMin = newAbsMin;
		AbsMax = newAbsMax;
		setupTimes();
		lmin = newLMin;
		lmax = newLMax;
		tfmin = newTFMin;
		tfmax = newTFMax;
		dp = newDP;
		dqs.removeAttr();
		updateStatistics();
		if (supervisor != null) {
			supervisor.notifyGlobalPropertyChange(Supervisor.eventDisplayedAttrs);
		}
		return true;
	}

	public boolean hasAttribute(int n) {
		if (fn == null)
			return false;
		for (int element : fn)
			if (element == n)
				return true;
		return false;
	}

	public boolean addAttributeToQuery(int n) {
		int oldN = (fn == null) ? 0 : fn.length;
		if (oldN < 1) {
			fn = new int[1];
			fn[0] = n;
			lmin = new Label[1];
			lmin[0] = new Label("");
			lmax = new Label[1];
			lmax[0] = new Label("");
			tfmin = new TextField[1];
			tfmin[0] = new TextField(7);
			tfmax = new TextField[1];
			tfmax[0] = new TextField(7);
			setup();
			filter.addQueryAttribute(fn[0]);
			dqs.addAttr();
			updateStatistics();
			if (supervisor != null) {
				supervisor.notifyGlobalPropertyChange(Supervisor.eventDisplayedAttrs);
			}
			return true;
		}
		int newFn[] = new int[oldN + 1];
		double newAbsMin[] = new double[oldN + 1], newAbsMax[] = new double[oldN + 1];
		Label newLMin[] = new Label[oldN + 1], newLMax[] = new Label[oldN + 1];
		TextField newTFMin[] = new TextField[oldN + 1], newTFMax[] = new TextField[oldN + 1];
//    DotPlot newDP[]=new DotPlot[oldN+1];
		DensityPlot newDP[] = new DensityPlot[oldN + 1];
		for (int i = 0; i < oldN; i++) {
			newFn[i] = fn[i];
			newAbsMin[i] = AbsMin[i];
			newAbsMax[i] = AbsMax[i];
			newLMin[i] = lmin[i];
			newLMax[i] = lmax[i];
			newTFMin[i] = tfmin[i];
			newTFMax[i] = tfmax[i];
			newDP[i] = dp[i];
		}
		newFn[newFn.length - 1] = n;
		fn = newFn;
		newAbsMin[newFn.length - 1] = Double.NaN;
		newAbsMax[newFn.length - 1] = Double.NaN;
		for (int i = 0; i < dots.length; i++) {
			double v = dataTable.getNumericAttrValue(n, i);
			if (!Double.isNaN(v)) {
				if (Double.isNaN(newAbsMin[newFn.length - 1]) || v < newAbsMin[newFn.length - 1]) {
					newAbsMin[newFn.length - 1] = v;
				}
				if (Double.isNaN(newAbsMax[newFn.length - 1]) || v > newAbsMax[newFn.length - 1]) {
					newAbsMax[newFn.length - 1] = v;
				}
			}
		}
		AbsMin = newAbsMin;
		AbsMax = newAbsMax;
		setupTimes();
		newLMin[newFn.length - 1] = new Label("", Label.CENTER);
		newLMax[newFn.length - 1] = new Label("", Label.CENTER);
		lmin = newLMin;
		lmax = newLMax;
		newTFMin[newFn.length - 1] = new TextField("", 7);
		newTFMax[newFn.length - 1] = new TextField("", 7);
		tfmin = newTFMin;
		tfmax = newTFMax;
		int j = newFn.length - 1;
		//String str=spade.lib.util.StringUtil.floatToStr(AbsMin[j],AbsMin[j],AbsMax[j]);
		String str = getAbsMinText(j);
		tfmin[j].setText(str);
		lmin[j].setText(str);
		//str=spade.lib.util.StringUtil.floatToStr(AbsMax[j],AbsMin[j],AbsMax[j]);
		str = getAbsMaxText(j);
		tfmax[j].setText(str);
		lmax[j].setText(str);
		dp = newDP;
		//constructDotPlot(j);
		constructDensityPlot(j);
		dp[j].setCanvas(canvas);
		filter.addQueryAttribute(fn[j]);
		dqs.addAttr();
		updateStatistics();
		if (supervisor != null) {
			supervisor.notifyGlobalPropertyChange(Supervisor.eventDisplayedAttrs);
		}
		return true;
	}

	@Override
	public boolean reloadAttributeData(Vector v) {
		return false;
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(100 * Metrics.mm(), (fn.length + 1) * (6 + 3) * Metrics.mm());
	}

	@Override
	public void setCanvas(Canvas c) {
		canvas = c;
		if (dp != null && canvas != null && selectionEnabled) {
			for (DensityPlot element : dp) {
				element.setCanvas(c);
			}
		}
	}

	protected void constructDotPlots() {
		if (dp == null) {
//      dp=new DotPlot[fn.length];
			dp = new DensityPlot[fn.length];
			for (int i = 0; i < dp.length; i++) {
				//        constructDotPlot(i);
				constructDensityPlot(i);
			}
		}
	}

	protected void constructDensityPlot(int i) {
		dp[i] = new DensityPlot();
		dp[i].setDataSource(dataTable);
		dp[i].setFieldNumber(fn[i]);
		dp[i].setup();
		dp[i].getFocuser().addFocusListener(this);
		dp[i].getFocuser().setIsUsedForQuery(true);
		dp[i].getFocuser().setTextFields(tfmin[i], tfmax[i]);
		dp[i].getFocuser().setTextDrawing(false);
		dp[i].getFocuser().setToDrawCurrMinMax(false);
		if (timeAbsMin != null && timeAbsMin[fn[i]] != null) {
			dp[i].getFocuser().setAbsMinMaxTime(timeAbsMin[fn[i]], timeAbsMax[fn[i]]);
		}
		if (canvas != null) {
			dp[i].setCanvas(canvas);
		}
	}

/*
  protected void constructDotPlot (int i) {
    dp[i]=new DotPlot(true,false,selectionEnabled,supervisor,this);
    dp[i].setDataSource(dataTable);
    dp[i].setFieldNumber(fn[i]);
    dp[i].setIsZoomable(true);
    dp[i].setup();
    dp[i].getFocuser().addFocusListener(this);
    dp[i].getFocuser().setIsUsedForQuery(true);
    dp[i].checkWhatSelected();
    dp[i].setTextDrawing(false);
    dp[i].getFocuser().setTextFields(tfmin[i],tfmax[i]);
    dp[i].getFocuser().setTextDrawing(false);
    dp[i].getFocuser().setToDrawCurrMinMax(false);
    if (timeAbsMin!=null && timeAbsMin[fn[i]]!=null)
      dp[i].getFocuser().setAbsMinMaxTime(timeAbsMin[fn[i]],timeAbsMax[fn[i]]);
    if (canvas!=null)
      dp[i].setCanvas(canvas);
  }
*/

	protected void drawDotPlots(Graphics g, int dy, int fh) {
		if (isZoomable) {
			if (isHorisontal) {
				int width = bounds.width, height = bounds.height;
				for (int j = 0; j < fn.length; j++) {
					dp[j].setBounds(new Rectangle(10, fh + dy * j, width - 20, dy - fh));
					//focuser.setAlignmentParameters(bounds.x+mx1,bounds.y+my1,width); //top position
					//focuser.setAlignmentParameters(bounds.x+mx1,
					//  bounds.y+bounds.height-horFocuser.getRequiredWidth(g),width); //bottom position
				}
			} else {
				//focuser.setAlignmentParameters(bounds.x+mx1+width,bounds.y+my1+height,height); //right position
				//focuser.setAlignmentParameters(bounds.x+mx1+width,bounds.y+my1+height,height); //right position
				//focuser.setAlignmentParameters(bounds.x+mx1,bounds.y+my1+height,height); //left position
			}
			for (DensityPlot element : dp) {
				element.draw(g);
			}
		}
	}

	@Override
	public void draw(Graphics g) {
		FontMetrics fm = g.getFontMetrics();
		int fh = fm.getHeight(), asc = fm.getAscent();
		int dy = bounds.height / (fn.length + 1);
		if (dp == null) {
			constructDotPlots();
		}
		drawDotPlots(g, dy, fh);
		g.setColor(Color.black);
		for (int j = 0; j < fn.length; j++) {
			g.drawString(dataTable.getAttributeName(fn[j]), 10, asc + dy * j);
		}
	}

	@Override
	public void redraw() {
		if (canvas == null || dp == null)
			return;
		Graphics g = canvas.getGraphics();
		if (g != null) {
			for (DensityPlot element : dp) {
				element.draw(g);
			}
		}
	}

	@Override
	public int mapX(double v) {
		return 0;
	}

	@Override
	public int mapY(double v) {
		return 0;
	}

	@Override
	public double absX(int x) {
		return 0;
	}

	@Override
	public double absY(int y) {
		return 0;
	}

	@Override
	public void focusChanged(Object source, double lowerLimit, double upperLimit) {
		if (!(source instanceof Focuser))
			return;
		if (filter == null)
			return;
		for (int i = 0; i < dp.length; i++)
			if (source == dp[i].getFocuser()) {
				filter.setLowLimit(fn[i], (lowerLimit <= AbsMin[i]) ? Double.NaN : lowerLimit);
				filter.setUpLimit(fn[i], (upperLimit >= AbsMax[i]) ? Double.NaN : upperLimit);
				filter.notifyFilterChange();
				if (supervisor != null && supervisor.getActionLogger() != null) {
					ActionDescr aDescr = new ActionDescr();
					aDescr.aName = "Dynamic Query: limit attribute range";
					aDescr.addParamValue("Table id", dataTable.getContainerIdentifier());
					aDescr.addParamValue("Table name", dataTable.getName());
					aDescr.addParamValue("Attribute", dataTable.getAttributeName(fn[i]));
					if (lowerLimit > AbsMin[i]) {
						aDescr.addParamValue("min", new Float(lowerLimit));
					}
					if (upperLimit < AbsMax[i]) {
						aDescr.addParamValue("max", new Float(upperLimit));
					}
					aDescr.startTime = System.currentTimeMillis();
					supervisor.getActionLogger().logAction(aDescr);
				}
				updateStatistics();
				return;
			}
	}

	protected boolean dynamicUpdate = false;

	public void setDynamicUpdate(boolean dynamicUpdate) {
		this.dynamicUpdate = dynamicUpdate;
	}

	@Override
	public void limitIsMoving(Object source, int n, double currValue) {
		if (!dynamicUpdate)
			return;
		if (!(source instanceof Focuser))
			return;
		Focuser f = (Focuser) source;
		if (n == 0) {
			focusChanged(source, currValue, f.getCurrMax());
		} else {
			focusChanged(source, f.getCurrMin(), currValue);
		}
	}

	@Override
	public void destroy() {
		idSaved = null;
		if (filter != null && fn != null && fn.length > 0) {
			filter.clearFilter();
			for (int element : fn) {
				filter.removeQueryAttribute(element);
			}
			filter.notifyFilterChange();
			if (supervisor != null && supervisor.getActionLogger() != null) {
				ActionDescr aDescr = new ActionDescr();
				aDescr.aName = "Dynamic Query: filter destroyed";
				aDescr.addParamValue("Table id", dataTable.getContainerIdentifier());
				aDescr.addParamValue("Table name", dataTable.getName());
				aDescr.startTime = System.currentTimeMillis();
				supervisor.getActionLogger().logAction(aDescr);
			}
		}
		if (dp != null) {
			for (DensityPlot element : dp) {
				element.destroy();
			}
		}
		super.destroy();
	}

	@Override
	public Vector getAttributeList() {
		Vector a = null;
		if (dataTable != null && fn != null && fn.length > 0) {
			a = new Vector(fn.length, 2);
			for (int element : fn) {
				a.addElement(dataTable.getAttributeId(element));
			}
		}
		return a;
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		if (saveQueryCmd.equals(ae.getActionCommand())) { // save query results in the table
			if (!(dataTable instanceof DataTable))
				return;
			DataTable dTable = (DataTable) dataTable;
			//System.out.println("* save query...");
			Panel p = new Panel();
			p.setLayout(new ColumnLayout());
			CheckboxGroup cbg = new CheckboxGroup();
			// following text:"Statically save current classification"
			Checkbox cbStat = new Checkbox(res.getString("Statically_save"), false, cbg),
			// following text:"Start dynamic saving of the classification"
			cbDyn = new Checkbox(res.getString("Start_dynamic_saving"), true, cbg),
			// following text:"Visualize on map"
			cbVis = new Checkbox(res.getString("Visualize_on_map"), true);
			p.add(cbStat);
			p.add(cbDyn);
			if (idSaved != null) {
				cbStat.setState(true);
				cbDyn.setEnabled(false);
			}
			p.add(new Line(false));
			p.add(cbVis);
			p.add(new Line(false));
			// following text: "Saving classification as attribute"
			OKDialog dlg = new OKDialog(CManager.getAnyFrame(canvas), res.getString("Saving_classification"), true);
			dlg.addContent(p);
			dlg.show();
			if (dlg.wasCancelled())
				return;
			if (cbStat.getState()) {
				nSaved++;
			}
			String name = "Dynamic query: " + ((cbStat.getState()) ? "state " + nSaved : "dynamics");
			Vector sourceAttr = new Vector((fn == null) ? 5 : fn.length, 5);
			if (fn != null) {
				for (int element : fn) {
					sourceAttr.addElement(dataTable.getAttributeId(element));
				}
			}
			// adding the column to the table
			int idx = dTable.addDerivedAttribute(name, AttributeTypes.character, AttributeTypes.logical, sourceAttr);
			for (int i = 0; i < dTable.getDataItemCount(); i++) {
				dTable.getDataRecord(i).setAttrValue((filter.isActive(i)) ? "y" : "n", idx);
			}
			// dynamic saving ?
			if (cbDyn.getState()) {
				idSaved = dTable.getAttributeId(idx);
			}
			// edit attr.name
			Vector resultAttr = new Vector(1, 1);
			resultAttr.addElement(new String(dTable.getAttributeId(idx)));
			AttrNameEditor.attrAddedToTable(dTable, resultAttr);
			// inform all displays about change of values
			dTable.notifyPropertyChange("values", null, resultAttr);
			// map visualziation ?

			/*
			AttrNameEditor.attrAddedToTable(dTable,resultAttrs);
			if (themLayer!=null && mapView!=null) //show on map
			  if (methodId==Normalization && resultAttrs.size()>1)
			    displayProducer.displayOnMap("parallel_bars",dTable,tfilter,resultAttrs,themLayer,mapView);
			  else
			    displayProducer.displayOnMap(dTable,tfilter,resultAttrs,themLayer,mapView);
			if (cbVis.getState()) {
			}
			*/
		}
	}

	/**
	* A method from the ObjectEventHandler interface.
	* The DynamicQuery receives object events from its dot plots and tranferres
	* them to the supervisor.
	*/
	@Override
	public void processObjectEvent(ObjectEvent oevt) {
		if (supervisor != null) {
			supervisor.processObjectEvent(new ObjectEvent(this, oevt.getType(), oevt.getSourceMouseEvent(), oevt.getSetIdentifier(), oevt.getAffectedObjects()));
		}
	}

	/**
	* Used to generate unique identifiers of instances of Plot's descendants.
	* The base class Plot calls this method in the constructor.
	*/
	@Override
	protected void countInstance() {
		instanceN = ++nInstances;
	}

	/**
	* Returns "Dynamic_Query".
	* Used to generate unique identifiers of instances of Plot's descendants.
	* Plot uses this method in the method getIdentifier().
	*/
	@Override
	public String getPlotTypeName() {
		return "Dynamic_Query";
	}
}
