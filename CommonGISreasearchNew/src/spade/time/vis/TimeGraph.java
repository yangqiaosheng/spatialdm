package spade.time.vis;

import java.awt.Canvas;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Point;
import java.awt.TextField;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Vector;

import spade.analysis.classification.BroadcastClassesCP;
import spade.analysis.classification.BroadcastClassesCPinfo;
import spade.analysis.classification.Classifier;
import spade.analysis.classification.TableClassifier;
import spade.analysis.system.Supervisor;
import spade.analysis.transform.AttributeTransformer;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.FocusListener;
import spade.lib.basicwin.Focuser;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.MouseDragEventConsumer;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.PopupManager;
import spade.lib.color.CS;
import spade.lib.util.DoubleArray;
import spade.lib.util.FloatArray;
import spade.lib.util.GraphGridSupport;
import spade.lib.util.GridPosition;
import spade.lib.util.IntArray;
import spade.lib.util.NumRange;
import spade.lib.util.NumValManager;
import spade.lib.util.StringUtil;
import spade.time.FocusInterval;
import spade.time.TimeLimits;
import spade.time.TimeMoment;
import spade.time.TimeReference;
import spade.time.TimeUtil;
import spade.vis.action.HighlightListener;
import spade.vis.action.Highlighter;
import spade.vis.action.ObjectEvent;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.database.ObjectFilter;
import spade.vis.database.Parameter;
import spade.vis.database.ThematicDataItem;
import spade.vis.event.EventSource;
import spade.vis.geometry.RealRectangle;

/**
* Represents temporal variation of values of a numeric attribute. Allows
* various modes of "visual comparison" by transforming the graph. Informs
* possible listeners about changing the reference object or time moment for
* comparison by clicking in the graph.
*/
public class TimeGraph extends Canvas implements MouseListener, MouseMotionListener, HighlightListener, EventSource, PropertyChangeListener, FocusListener, ItemListener, Destroyable {
	/**
	* Possible operations over subsequent selections
	*/
	public static final int SEL_REPLACE = 0, SEL_OR = 1, SEL_AND = 2, SEL_TOGGLE = 3, SEL_OPER_FIRST = 0, SEL_OPER_LAST = 3;
	/**
	* The table with time-dependent data
	*/
	protected AttributeDataPortion table = null;
	protected DataTable dTable = null;
	/**
	* The attribute transformer transforms the data displayed on the graph:
	* aggregates, computes changes, compares to mean, median, or selected object,
	* etc.
	*/
	protected AttributeTransformer aTrans = null;
	/**
	* The time-dependent super-attribute to be represented, i.e. an attribute
	* having references to child attributes corresponding to different values
	* of a temporal parameter.
	*/
	protected Attribute supAttr = null;
	/**
	* The temporal parameter of the table the time graph works with
	*/
	protected Parameter par = null;

	/**
	* Returns the temporal parameter of the table the time graph works with
	*/
	public Parameter getTemporalParameter() {
		return par;
	}

	/**
	* The attribute may additionally depend on some other parameters. The values
	* of these additional parameters must be fixed by the user. These vectors
	* contain the names and the selected values of the additional parameters.
	*/
	protected Vector otherParNames = null, otherParValues = null;

	/**
	* ObjectFilter may be associated with the table and contain results of data
	* querying. Only data satisfying the current query (if any) are displayed
	*/
	protected ObjectFilter tf = null;

	/**
	* Indices of the table columns used in the visualization. The columns are
	* sorted according to their time references. There must be an element for
	* each parameter value. If such a column has not been found, the element is -1.
	*/
	protected IntArray colNs = null;

	/**
	* Returns the indexes of the table columns used in the visualization.
	* The columns are sorted according to their time references. There must be an
	* element for each parameter value. If such a column has not been found, the
	* element is -1.
	*/
	public IntArray getColumnNumbers() {
		if (colNs == null || colNs.size() < 2) {
			makeColumnList();
		}
		return colNs;
	}

	/**
	* Absolute minimum and maximum values
	*/
	protected double absMin = Double.NaN, absMax = Double.NaN;
	/**
	* Minimum and maximum shown values (set by a focuser)
	*/
	protected double focusMin = Double.NaN, focusMax = Double.NaN;

	/**
	* The first and the last time moments the data refer to
	*/
	protected TimeMoment start = null, end = null;
	/*
	* Temporal focus (horisontal focusing of the graph): start and finish
	*/
	protected TimeMoment tFocusStart = null, tFocusEnd = null;

	/**
	* The FocusInterval propagates the events of changing the current time moment
	* in the system.
	*/
	protected FocusInterval fint = null;
	/**
	* Current time moment on other displays
	*/
	protected int currTMidx = -1, prevTMidx = -1;
	/**
	* If this field is not null, all mouse drag events that occur in the plot
	* drawing area (but not in the area of the focuser) are passed to this
	* component.
	*/
	protected MouseDragEventConsumer mouseDragConsumer = null;
	/**
	* Used for registering possible listeners that need to be notified when the
	* time graph is redrawn. This may be needed, for example, in a TimeQueryBuilder.
	*/
	protected PropertyChangeSupport pcSupport = null;
	/**
	* The classifier that propagates its classification. May be null.
	*/
	protected Classifier cl = null;
	/**
	* If the classifier that propagates its classification is a TableClassifier,
	* this variable provides an additional reference to it.
	*/
	protected TableClassifier tcl = null;

	public double getAbsMin() {
		return absMin;
	}

	public double getAbsMax() {
		return absMax;
	}

	public void recalcAbsMinMax() {
		absMin = absMax = Double.NaN;
		for (int i = 0; i < lines.size(); i++) {
			TimeLine line = (TimeLine) lines.elementAt(i);
			for (int j = 0; j < line.getNPoints(); j++) {
				double v = line.getValue(j);
				if (Double.isNaN(v) || Double.isInfinite(v)) {
					continue;
				}
				if (Double.isNaN(absMin) || v < absMin) {
					absMin = v;
				}
				if (Double.isNaN(absMax) || v > absMax) {
					absMax = v;
				}
			}
		}
		boolean needRefocus = false;
		if (Double.isNaN(focusMin) || focusMin < absMin || focusMin > absMax) {
			focusMin = absMin;
			needRefocus = true;
		}
		if (Double.isNaN(focusMax) || focusMax < absMin || focusMax > absMax) {
			focusMax = absMax;
			needRefocus = true;
		}
		if (focuser != null && (needRefocus || absMin != focuser.getAbsMin() || absMax != focuser.getAbsMax())) {
			focuser.setAbsMinMax(absMin, absMax);
			focuser.setCurrMinMax(focusMin, focusMax);
		}
	}

	/**
	* Min and Max for group of time graphs
	*/
	protected boolean useCommonMinMax = false;

	public void setUseCommonMinMax() {
		useCommonMinMax = false;
		if (absMin != focuser.getAbsMin() || absMax != focuser.getAbsMax()) {
			focusMin = absMin;
			focusMax = absMax;
			focuser.setAbsMinMax(absMin, absMax);
			focuser.setCurrMinMax(focusMin, focusMax);
		}
		if (isShowing()) {
			repaint();
		}
	}

	public void setUseCommonMinMax(double commonMin, double commonMax) {
		useCommonMinMax = true;
		absMin = focusMin = commonMin;
		absMax = focusMax = commonMax;
		if (commonMin != focuser.getAbsMin() || commonMax != focuser.getAbsMax()) {
			focuser.setAbsMinMax(commonMin, commonMax);
			focuser.setCurrMinMax(focusMin, focusMax);
		}
		if (isShowing()) {
			repaint();
		}
	}

	/**
	* Lider lines: offset and period; 0 means hide
	*/
	protected int iLiderPeriod = 24, iLiderOffset = 0;

	public int getLiderPeriod() {
		return iLiderPeriod;
	}

	public int getLiderOffset() {
		return iLiderOffset;
	}

	public void setLiderLines(int iLiderPeriod, int iLiderOffset) {
		this.iLiderPeriod = iLiderPeriod;
		this.iLiderOffset = iLiderOffset;
		if (isShowing()) {
			repaint();
		}
	}

	public TimeMoment getFocusStart() {
		return tFocusStart;
	}

	public TimeMoment getFocusEnd() {
		return tFocusEnd;
	}

	/*
	* indices in colNs for focus start and end
	*/
	protected int idxTFstart = 0, idxTFend = 0;

	public int getIdxTFstart() {
		return idxTFstart;
	}

	public int getIdxTFend() {
		return idxTFend;
	}

	/**
	* The length of the interval to be shown
	*/
	protected long iLen = 0l;

	/**
	* Vector of lines to be drawn
	*/
	protected Vector lines = null;

	public Vector getLines() {
		return lines;
	}

	/**
	* lines for average and median
	*/
	protected boolean trendTM[] = null; // true corresponds to TMs for setting trends; 1stand last are always =true
	protected TimeLine lineAvg = null, lineMedian = null;
	protected Vector vLinesAvg = null, vLinesMedian = null;
	protected boolean showLineAvg = false, showLineMedian = false;
	protected boolean showTrend = false, trendFromMedians = false;
	protected boolean subtractTrend = false, subtractFromTransformedData = false;
	// we need to store the values separately because in comparison modes
	// all values are transformed
	// ... to be replaced by srcValues in TimeLine
	protected float lineAvgValues[] = null, lineMedianValues[] = null;

	public boolean getShowTrendLine() {
		return showTrend;
	}

	public void setShowLineAvg(boolean showLineAvg) {
		this.showLineAvg = showLineAvg;
		draw(getGraphics());
	}

	public void setShowLineMedian(boolean showLineMedian) {
		this.showLineMedian = showLineMedian;
		draw(getGraphics());
	}

	/**
	 * The trend line or lines, in case of multiple classes
	 */
	protected TimeLine lineTrend = null;

	public void setShowTrendLine(boolean show) {
		if (showTrend == show)
			return;
		showTrend = show;
		if (showTrend && trendTM == null) {
			trendTM = new boolean[par.getValueCount()];
			trendTM[0] = true;
			trendTM[trendTM.length - 1] = true;
		}
		if (!showTrend) {
			trendTM = null;
			lineTrend = null;
		} else {
			computeCommonTrendLine();
		}
		draw(getGraphics());
	}

	public void setSubtractTrend(boolean subtract, boolean fromTransformedData) {
		if (subtractTrend == subtract && subtractFromTransformedData == fromTransformedData)
			return;
		subtractTrend = subtract;
		subtractFromTransformedData = fromTransformedData;
		subtractTrendFromDataIfNeeded();
		if (isShowing()) {
			draw(getGraphics());
		}
	}

	protected double[] float2double(float values[]) {
		if (values == null)
			return null;
		double dv[] = new double[values.length];
		for (int i = 0; i < values.length; i++) {
			dv[i] = values[i];
		}
		return dv;
	}

	public boolean computeCommonTrendLine() {
		if (trendTM == null) {
			lineTrend = null;
			return false;
		}
		//compute the common trend line
		float valuesToBuildTrend[] = (trendFromMedians) ? lineMedianValues : lineAvgValues;
		if (valuesToBuildTrend == null)
			return false;
		double trend[] = computeTrendLine(float2double(valuesToBuildTrend));
		if (trend == null)
			return false;
		if (lineTrend == null) {
			lineTrend = new TimeLine();
			lineTrend.objIdx = -1;
			lineTrend.setNPoints(colNs.size());
		}
		for (int i = 0; i < trend.length; i++) {
			lineTrend.setValue(trend[i], i);
		}
		return true;
	}

	public double[] computeTrendLine(double valuesToBuildTrend[]) {
		if (trendTM == null || valuesToBuildTrend == null)
			return null;
		int nIntervals = 0;
		for (int i = 1; i < trendTM.length - 1; i++)
			if (trendTM[i]) {
				++nIntervals;
			}
		++nIntervals; //include the last column
		int iEnds[] = new int[nIntervals];
		int k = 0;
		for (int i = 1; i < trendTM.length - 1; i++)
			if (trendTM[i]) {
				iEnds[k++] = i;
			}
		iEnds[nIntervals - 1] = colNs.size() - 1;
		float firstValues[] = new float[nIntervals];
		float lastValues[] = new float[nIntervals];

		for (k = 0; k < nIntervals; k++) {
			int idx0 = (k == 0) ? 0 : iEnds[k - 1], idxEnd = iEnds[k];
			float sum = 0f;
			int num = 0;
			for (int i = idx0; i <= idxEnd; i++) {
				if (Double.isNaN(valuesToBuildTrend[i])) {
					continue;
				}
				int j;
				for (j = i + 1; j <= idxEnd && Double.isNaN(valuesToBuildTrend[j]); j++) {
					;
				}
				if (j > idxEnd) {
					break;
				}
				sum += (valuesToBuildTrend[j] - valuesToBuildTrend[i]) / (j - i);
				num++;
			}
			if (num < 1) {
				firstValues[k] = lastValues[k] = Float.NaN;
			} else {
				float aTrend = sum / num;
				num = 0;
				sum = 0f;
				for (int i = idx0; i <= idxEnd; i++)
					if (!Double.isNaN(valuesToBuildTrend[i])) {
						float trendVal = aTrend * (i - idx0);
						double diff = valuesToBuildTrend[i] - trendVal;
						sum += diff;
						++num;
					}
				float bTrend = sum / num;
				firstValues[k] = bTrend;
				lastValues[k] = bTrend + aTrend * (idxEnd - idx0);
			}
		}
		for (k = 1; k < nIntervals; k++)
			if (!Float.isNaN(lastValues[k - 1]) && !Float.isNaN(firstValues[k])) {
				float ave = (lastValues[k - 1] + firstValues[k]) / 2;
				lastValues[k - 1] = firstValues[k] = ave;
			}
		double result[] = new double[colNs.size()];
		for (int i = 0; i < result.length; i++) {
			result[i] = Double.NaN;
		}
		for (k = 0; k < nIntervals; k++)
			if (!Float.isNaN(firstValues[k]) && !Float.isNaN(lastValues[k])) {
				int idx0 = (k == 0) ? 0 : iEnds[k - 1], idxEnd = iEnds[k], iLen = idxEnd - idx0;
				float aTrend = (lastValues[k] - firstValues[k]) / iLen, bTrend = firstValues[k];
				for (int i = idx0; i <= idxEnd; i++) {
					result[i] = bTrend + aTrend * (i - idx0);
				}
			}
		return result;
	}

	public void subtractTrendFromDataIfNeeded() {
		if (subtractTrend) {
			for (int i = 0; i < lines.size(); i++) {
				TimeLine line = (TimeLine) lines.elementAt(i);
				//compute individual trend line parameters
				double trend[] = computeTrendLine(line.getValues());
				if (trend != null) {
					double trendMax = Double.NaN;
					for (int j = 0; j < trend.length; j++)
						if (!Double.isNaN(trend[j]))
							if (Double.isNaN(trendMax) || trendMax < trend[j]) {
								trendMax = trend[j];
							}
					for (int j = 0; j < colNs.size(); j++) {
						double val = (subtractFromTransformedData) ? line.getValue(j) : table.getNumericAttrValue(colNs.elementAt(j), line.objIdx);
						if (!Double.isNaN(trend[j])) {
							//line.setTransformedValue(val-trend[j],j);
							line.setTransformedValue(val + (trendMax - trend[j]), j);
						} else {
							line.setTransformedValue(val, j);
						}
					}
				} else if (line.hasTransformedValues()) {
					for (int j = 0; j < line.getNPoints(); j++) {
						line.setTransformedValue(line.getValue(j), j);
					}
				}
			}
		} else {
			for (int i = 0; i < lines.size(); i++) {
				TimeLine line = (TimeLine) lines.elementAt(i);
				if (line.hasTransformedValues()) {
					for (int j = 0; j < line.getNPoints(); j++) {
						line.setTransformedValue(line.getValue(j), j);
					}
				}
			}
		}
		resetValueRanges();
	}

	/**
	 * @param useMedians - if false, the trend line is built from the average values (default)
	 */
	public void setBuildTrendFromMedians(boolean useMedians) {
		if (trendFromMedians == useMedians)
			return;
		trendFromMedians = useMedians;
		if (computeCommonTrendLine()) {
			subtractTrendFromDataIfNeeded();
			if (showTrend || subtractTrend) {
				draw(getGraphics());
			}
		}
	}

	/**
	* number of classes and class breaks for each time moment
	*/
	protected int nclasses = 1;
	protected boolean isEqualClassSize = true;

	public void setNClasses(int nclasses, boolean isEqualClassSize) {
		if (this.nclasses == nclasses && this.isEqualClassSize == isEqualClassSize)
			return;
		this.nclasses = nclasses;
		this.isEqualClassSize = isEqualClassSize;
		resetClasses();
		draw(getGraphics());
	}

	protected float br[][] = null;

	/**
	* breaks on the time aggregator - used for drawing on the time graph
	*/
	protected float taBreaks[] = null;
	protected Color taClassColors[] = null;

	public void setTAbreaks(float taBreaks[], Color taClassColors[]) {
		this.taBreaks = taBreaks;
		this.taClassColors = taClassColors;
		draw(getGraphics());
	}

	/**
	* Identifiers of attributes that store average classification and variance
	* of the dynamic of classification
	*/
	protected String attrIdClassAverage = null, attrIdClassVariance = null, attrIdClassIncrease = null, attrIdClassDecrease = null, attrIdClassNumbers[] = null;

	/**
	* Plot area (where the lines are drawn)
	*/
	protected int x0 = -1, y0 = -1, width = 0, height = 0;

	/**
	* The supervisor is used for propagating object events among system components
	*/
	protected Supervisor supervisor = null;
	/**
	 * Assignments of colors to time intervals.
	 * Contains arrays Object[2] = [TimeMoment,Color],
	 * where the time moments are the starting moments of the intervals.
	 * The intervals are chronologically sorted.
	 */
	protected Vector<Object[]> colorsForTimes = null;

	/**
	* Currently highlighted lines
	*/
	protected Vector hLines = null;

	/**
	* The operation over subsequent selections
	*/
	protected int selectMode = SEL_TOGGLE;

	/**
	* Indicates whether the graph draws only selected lines or all lines
	*/
	protected boolean drawOnlySelected = false;

	public boolean getDrawOnlySelected() {
		return drawOnlySelected;
	}

	public void setDrawOnlySelected(boolean value) {
		drawOnlySelected = value;
		if (isShowing()) {
			repaint();
		}
	}

	/**
	* Indicates whether the graph draws values flow(s) and classes
	*/
	protected boolean drawValueFlow = true, drawValueClasses = true;

	public void setDrawValueFlow(boolean value) {
		drawValueFlow = value;
		if (isShowing()) {
			repaint();
		}
	}

	public void setDrawValueClasses(boolean value) {
		drawValueClasses = value;
		if (isShowing()) {
			repaint();
		}
	}

	/**
	* Indicates whether the graph draws values flow(s)
	*/
	protected boolean drawGrid = true;

	public void setDrawGrid(boolean value) {
		drawGrid = value;
		if (isShowing()) {
			repaint();
		}
	}

	/**
	* Line segmentation: difference Vs. ratio to previous moment,
	* show only segments where segmLo<=change<segmHi
	*/
	public static int TG_SegmDiff = 0, TG_SegmRatio = 1, TG_SegmValues = 2;
	protected boolean segmentation = false;
	protected int segmMode = TG_SegmDiff;
	protected float segmLo = Float.NaN, segmHi = Float.NaN;

	protected void setSegmentationOn(float segmLo, float segmHi, int segmMode) {
		segmentation = true;
		this.segmMode = segmMode;
		this.segmLo = segmLo;
		this.segmHi = segmHi;
		clearSegmTM();
		reset(false);
	}

	protected void setSegmentationOff() {
		if (segmentation) {
			segmentation = false;
			clearSegmTM();
			reset(false);
		}
	}

	/**
	* Colors of the background of the canvas and of the plot area
	*/
	public static Color graphBkgColor = new Color(220, 220, 220);
	protected Color bkgColor = Color.white, plotAreaColor = new Color(180, 180, 180);
	protected Color plotAreaColorBrighter = new Color(190, 190, 190);
	protected Color plotAreaColorDarker = new Color(160, 160, 160);;

	public Color getPlotAreaColor() {
		return plotAreaColor;
	}

	/**
	* The focuser is used for focusing on value subranges of the attribute
	*/
	protected Focuser focuser = null;

	/**
	* Indicates "destroyed" state
	*/
	protected boolean destroyed = false;

	/**
	* Used for showing attribute values near mouse positions
	*/
	protected PopupManager popM = null;

	/**
	* Selection boxes to be drawn in background. Elements of the vector are
	* instances of the class spade.vis.geometry.RealRectangle.
	*/
	protected Vector boxes = null;

	/**
	* Segmentation mode: Time moments uswed for selecting lines having values
	*/
	protected boolean segmTM[] = null;

	protected void clearSegmTM() {
		if (segmTM != null) {
			boolean anySelected = false;
			for (int n = 0; n < segmTM.length && !anySelected; n++) {
				anySelected = segmTM[n];
			}
			segmTM = null;
			if (anySelected) {
				supervisor.getHighlighter(table.getEntitySetIdentifier()).clearSelection(this);
				if (isShowing()) {
					repaint();
				}
			}
		}
	}

	/**
	* Used to generate unique identifiers of instances
	*/
	protected int instanceN = 0;
	protected static int nInstances = 0;

	boolean firstDraw = true; // for some strange reason, 1st drawing of long lines takes long time :(

	public TimeGraph() {
		instanceN = nInstances++;
	}

	/**
	* Sets the table with time-dependent data
	*/
	public void setTable(AttributeDataPortion table) {
		this.table = table;
		if (table != null && supervisor != null) {
			supervisor.registerHighlightListener(this, table.getEntitySetIdentifier());
		}
		if (table != null) {
			table.addPropertyChangeListener(this);
			tf = table.getObjectFilter();
			if (tf != null) {
				tf.addPropertyChangeListener(this);
			}
		}
		if (table instanceof DataTable) {
			dTable = (DataTable) table;
		}
	}

	/**
	* Returns its reference to the table with time-dependent data
	*/
	public AttributeDataPortion getTable() {
		return table;
	}

	/**
	* Sets the data transformer, which may perform temporal aggregation, compute
	* changes, etc. The time graph must register itself as a listener of data
	* change events generated by the transformer. When such events are received,
	* the time graph must be redrawn.
	*/
	public void setAttributeTransformer(AttributeTransformer transformer) {
		aTrans = transformer;
		if (aTrans != null) {
			aTrans.addPropertyChangeListener(this);
		}
		if (isShowing()) {
			reset(true);
		}
	}

	/**
	* Returns its data transformer, which performs temporal aggregation, compute
	* changes, etc.
	*/
	public AttributeTransformer getAttributeTransformer() {
		return aTrans;
	}

	/**
	* Sets the time-dependent super-attribute to be represented, i.e. an attribute
	* having references to child attributes corresponding to different values
	* of a temporal parameter.
	*/
	public void setAttribute(Attribute attr) {
		if (attr != null && attr.getChildrenCount() > 1) {
			supAttr = attr;
		}
	}

	/**
	* Returns its time-dependent super-attribute.
	*/
	public Attribute getAttribute() {
		return supAttr;
	}

	/**
	* Sets the temporal parameter
	*/
	public void setTemporalParameter(Parameter par) {
		if (par.isTemporal() && par.getValueCount() > 1) {
			this.par = par;
		}
	}

	/**
	* The attribute may additionally depend on some other parameters. The values
	* of these additional parameters must be fixed by the user. This method
	* is used to specify the user-selected value of the given parameter.
	*/
	public void setOtherParameterValue(String paramName, Object paramValue) {
		if (paramName == null || paramValue == null)
			return;
		if (otherParNames == null) {
			otherParNames = new Vector(5, 5);
			otherParValues = new Vector(5, 5);
			otherParNames.addElement(paramName);
			otherParValues.addElement(paramValue);
			return;
		}
		int idx = StringUtil.indexOfStringInVectorIgnoreCase(paramName, otherParNames);
		if (idx >= 0) {
			otherParValues.setElementAt(paramValue, idx);
		} else {
			otherParNames.addElement(paramName);
			otherParValues.addElement(paramValue);
		}
	}

	public Vector getOtherParamNames() {
		if (otherParNames == null)
			return null;
		return (Vector) otherParNames.clone();
	}

	public Vector getOtherParamValues() {
		if (otherParValues == null)
			return null;
		return (Vector) otherParValues.clone();
	}

	/**
	* Sets a reference to the system's supervisor. The supervisor is used for
	* propagating object events among system components.
	*/
	public void setSupervisor(Supervisor sup) {
		supervisor = sup;
		if (supervisor != null) {
			supervisor.addPropertyChangeListener(this);
			supervisor.registerObjectEventSource(this);
			if (table != null) {
				supervisor.registerHighlightListener(this, table.getEntitySetIdentifier());
			}
			addMouseListener(this);
			addMouseMotionListener(this);
			tryGetColorsForTimes();
			if (isShowing()) {
				repaint();
			}
		}
	}

	/**
	 * Tries to get from the supervisor the assignment of colors to times
	 */
	protected void tryGetColorsForTimes() {
		colorsForTimes = null;
		if (supervisor != null && supervisor.coloredObjectsAreTimes()) {
			Vector assignment = supervisor.getColorsForTimes();
			if (assignment != null) {
				colorsForTimes = TimeUtil.getColorsForTimeIntervals(assignment);
			}
			if (colorsForTimes != null && par != null && par.getValueCount() > 0) {
				//check the validity of the time intervals
				TimeMoment t0 = (TimeMoment) par.getFirstValue(), tEnd = (TimeMoment) par.getLastValue();
				for (int i = colorsForTimes.size() - 1; i >= 0; i--) {
					TimeMoment t = (TimeMoment) colorsForTimes.elementAt(i)[0];
					boolean valid = t != null && t.getClass().getName().equals(t0.getClass().getName()) && t.compareTo(t0) >= 0 && t.compareTo(tEnd) <= 0;
					if (!valid) {
						colorsForTimes.removeElementAt(i);
					}
				}
				if (colorsForTimes.size() < 1) {
					colorsForTimes = null;
				}
			}
		}
	}

	/**
	* Returns the current operation over subsequent selections. This is one
	* of the constants SEL_REPLACE(=0), SEL_OR(=1), SEL_AND(=2), SEL_TOGGLE(=3)
	*/
	public int getSelectionOperation() {
		return selectMode;
	}

	/**
	* Sets the current operation to be applied to subsequent selections. This must
	* be one of the constants SEL_REPLACE(=0), SEL_OR(=1), SEL_AND(=2),
	* SEL_TOGGLE(=3).
	*/
	public void setSelectionOperation(int operation) {
		if (operation >= SEL_OPER_FIRST && operation <= SEL_OPER_LAST) {
			selectMode = operation;
		}
		if (boxes != null && boxes.size() > 0) {
			boxes.removeAllElements();
			if (isShowing()) {
				repaint();
			}
		}
	}

	/**
	* Retrieves and sorts the indices of the columns with values of the
	* attribute to be visualized. The columns are sorted according to their
	* time references.
	*/
	protected void makeColumnList() {
		absMin = Float.NaN;
		absMax = Float.NaN;
		if (table == null || supAttr == null || par == null)
			return;
		if (colNs == null) {
			colNs = new IntArray(par.getValueCount(), 5);
		} else {
			colNs.removeAllElements();
		}
		int nCols = 0;
		for (int i = 0; i < par.getValueCount(); i++) {
			int colN = -1;
			Object value = par.getValue(i);
			for (int j = 0; j < supAttr.getChildrenCount() && colN < 0; j++) {
				Attribute attr = supAttr.getChild(j);
				if (attr.hasParamValue(par.getName(), value)) {
					boolean ok = true;
					if (attr.getParameterCount() > 1 && otherParNames != null) {
						for (int k = 0; k < attr.getParameterCount() && ok; k++) {
							String pname = attr.getParamName(k);
							if (!pname.equalsIgnoreCase(par.getName())) {
								int idx = StringUtil.indexOfStringInVectorIgnoreCase(pname, otherParNames);
								if (idx >= 0) {
									ok = attr.getParamValue(k).equals(otherParValues.elementAt(idx));
								}
							}
						}
					}
					if (ok) {
						colN = table.getAttrIndex(attr.getIdentifier());
					}
				}
			}
			if (colN >= 0) {
				++nCols;
			}
			colNs.addElement(colN); //there must be an element for each parameter
			//value. If such a column has not been found, the element is -1.
		}
		if (nCols < 2) {
			colNs = null; //actually no columns relevant to this parameter have been found
			return;
		}
		start = (TimeMoment) par.getValue(0);
		end = (TimeMoment) par.getValue(par.getValueCount() - 1);
		tFocusStart = start;
		tFocusEnd = end;
		//iLen=end.subtract(start);
		//iLen=tFocusEnd.subtract(tFocusStart);
		idxTFstart = 0;
		idxTFend = colNs.size() - 1;
		iLen = idxTFend - idxTFstart;
	}

	protected boolean resetWasCalledAtLeastOnce = false;

	/**
	* Prepares all data structures
	*/
	protected void setup() {
		if (colNs == null || table == null)
			return;
		findClassifier();
		if (lines == null) {
			lines = new Vector(table.getDataItemCount(), 5);
		} else {
			lines.removeAllElements();
		}
		Highlighter hl = null;
		if (supervisor != null) {
			hl = supervisor.getHighlighter(table.getEntitySetIdentifier());
		}
		for (int i = 0; i < table.getDataItemCount(); i++) {
			ThematicDataItem data = (ThematicDataItem) table.getDataItem(i);
			boolean hasValues = false;
			for (int j = 0; j < colNs.size() && !hasValues; j++) {
				//for (int j=idxTFstart; j<=idxTFend; j++) {
				double val = getNumericAttrValue(colNs.elementAt(j), data);
				if (Double.isNaN(val) || Double.isInfinite(val)) {
					continue;
				}
				hasValues = true;
			}
			if (hasValues) {
				TimeLine line = new TimeLine();
				line.objIdx = i;
				line.objId = data.getId();
				line.setNPoints(colNs.size());
				if (hl != null) {
					line.selected = hl.isObjectSelected(line.objId);
				}
				lines.addElement(line);
			}
		}
		classifyLines();
		if (Double.isNaN(absMin)) {
			absMin = 0f; // to avoid recursive draw - setup
		}
		if (resetWasCalledAtLeastOnce) {
			resetAvgAndMedianLines();
			resetClasses();
		} else {
			reset(true);
			resetWasCalledAtLeastOnce = true;
		}
		if (popM == null) {
			popM = new PopupManager(this, "", true);
			popM.setMayBreakLines(false);
			popM.setOnlyForActiveWindow(false);
		}
		if (focuser == null) {
			focuser = new Focuser();
			focuser.setIsVertical(true);
			focuser.setSpacingFromAxis(0);
			focuser.setIsLeft(false); //right position
			focuser.setBkgColor(bkgColor);
			focuser.setPlotAreaColor(plotAreaColor);
			focuser.addFocusListener(this);
		}
		boolean needRefocus = false;
		if (Double.isNaN(focusMin) || focusMin < absMin || focusMin > absMax) {
			focusMin = absMin;
			needRefocus = true;
		}
		if (Double.isNaN(focusMax) || focusMax < absMin || focusMax > absMax) {
			focusMax = absMax;
			needRefocus = true;
		}
		if (focuser != null && (needRefocus || absMin != focuser.getAbsMin() || absMax != focuser.getAbsMax())) {
			focuser.setAbsMinMax(absMin, absMax);
			focuser.setCurrMinMax(focusMin, focusMax);
		}
	}

	/**
	* A convenience method for accessing original or transformed attribute values.
	* If the graph has no attribute transformer, the values are got from the
	* table, otherwise from the attribute transformer.
	*/
	protected double getNumericAttrValue(int colN, ThematicDataItem data) {
		if (data == null || colN < 0)
			return Double.NaN;
		if (aTrans == null)
			return data.getNumericAttrValue(colN);
		return aTrans.getNumericAttrValue(colN, data);
	}

	/**
	* Depending on the current settings for the visual comparison, determines
	* the (transformed) values to be visualized.
	*/
	protected void reset(boolean resetValueRange) {
		if (colNs == null || table == null || lines == null)
			return;
		findClassifier();
		if (resetValueRange) {
			focusMin = absMin = focusMax = absMax = Double.NaN;
			if (boxes != null) {
				boxes.removeAllElements();
			}
		}
		if (Double.isNaN(absMin) || Double.isNaN(absMax)) {
			resetValueRange = true;
		}
		for (int i = 0; i < lines.size(); i++) {
			TimeLine line = (TimeLine) lines.elementAt(i);
			ThematicDataItem data = (ThematicDataItem) table.getDataItem(line.objIdx);
			for (int j = 0; j < colNs.size(); j++) {
				double val = getNumericAttrValue(colNs.elementAt(j), data);
				if (j >= idxTFstart && j <= idxTFend) {
					line.setValue(val, j);
				}
				if (Double.isNaN(val) || Double.isInfinite(val)) {
					continue;
				}
				if (resetValueRange) {
					if (Double.isNaN(absMin) || val < absMin) {
						absMin = val;
					}
					if (Double.isNaN(absMax) || val > absMax) {
						absMax = val;
					}
				}
			}
			// post processing - if segmentation mode
			if (segmentation) {
				for (int j = idxTFend; j >= idxTFstart; j--) {
					double curr = line.getValue(j);
					if (!Double.isNaN(curr) && !Double.isInfinite(curr)) {
						if (segmMode == TG_SegmValues) {
							if (!Double.isNaN(segmLo) && curr < segmLo) {
								line.setValue(Double.NaN, j);
								continue;
							}
							if (!Double.isNaN(segmHi) && curr > segmHi) {
								line.setValue(Double.NaN, j);
								continue;
							}
						} else if (j == 0 || Double.isNaN(line.getValue(j - 1)) || Double.isInfinite(line.getValue(j - 1))) {
							line.setValue(Double.NaN, j);
						} else {
							double prev = line.getValue(j - 1);
							if (!Double.isNaN(segmLo)) {
								if (segmMode == TG_SegmDiff && curr - prev < segmLo) {
									line.setValue(Double.NaN, j);
									continue;
								}
								if (segmMode == TG_SegmRatio && (prev == 0f || curr / prev < segmLo)) {
									line.setValue(Double.NaN, j);
									continue;
								}
							}
							if (!Double.isNaN(segmHi)) {
								if (segmMode == TG_SegmDiff && curr - prev > segmHi) {
									line.setValue(Double.NaN, j);
									continue;
								}
								if (segmMode == TG_SegmRatio && (prev == 0f || curr / prev > segmHi)) {
									line.setValue(Double.NaN, j);
									continue;
								}
							}
						}
					}
				}
			}
		}
		classifyLines();
		resetAvgAndMedianLines();
		resetClasses();
		boolean needRefocus = resetValueRange;
		if (Double.isNaN(focusMin) || focusMin < absMin || focusMin > absMax) {
			focusMin = absMin;
			needRefocus = true;
		}
		if (Double.isNaN(focusMax) || focusMax < absMin || focusMax > absMax) {
			focusMax = absMax;
			needRefocus = true;
		}
		if (focuser != null && (needRefocus || absMin != focuser.getAbsMin() || absMax != focuser.getAbsMax())) {
			focuser.setAbsMinMax(absMin, absMax);
			focuser.setCurrMinMax(focusMin, focusMax);
		}
		if (isShowing()) {
			repaint();
		}
	}

	public void resetValueRanges() {
		absMin = absMax = Double.NaN;
		if (boxes != null) {
			boxes.removeAllElements();
		}
		for (int i = 0; i < lines.size(); i++) {
			TimeLine line = (TimeLine) lines.elementAt(i);
			ThematicDataItem data = (ThematicDataItem) table.getDataItem(line.objIdx);
			for (int j = 0; j < colNs.size(); j++) {
				double val = (line.hasTransformedValues()) ? line.getTransformedValue(j) : getNumericAttrValue(colNs.elementAt(j), data);
				if (Double.isNaN(val) || Double.isInfinite(val)) {
					continue;
				}
				if (Double.isNaN(absMin) || val < absMin) {
					absMin = val;
				}
				if (Double.isNaN(absMax) || val > absMax) {
					absMax = val;
				}
			}
		}
		boolean needRefocus = false;
		if (Double.isNaN(focusMin) || focusMin < absMin || focusMin > absMax) {
			focusMin = absMin;
			needRefocus = true;
		}
		if (Double.isNaN(focusMax) || focusMax < absMin || focusMax > absMax) {
			focusMax = absMax;
			needRefocus = true;
		}
		if (focuser != null && (needRefocus || absMin != focuser.getAbsMin() || absMax != focuser.getAbsMax())) {
			focuser.setAbsMinMax(absMin, absMax);
			focuser.setCurrMinMax(focusMin, focusMax);
		}
	}

	protected boolean toSaveClasses = false;

	@Override
	public void itemStateChanged(ItemEvent ie) {
		if (ie.getSource() instanceof Checkbox) {
			Checkbox cb = (Checkbox) ie.getSource();
			toSaveClasses = true;
			cb.setEnabled(false);
			resetClasses();
		}
	}

	private void resetClasses() {
		if (dTable != null && nclasses > 1 && toSaveClasses && attrIdClassAverage == null) {
			// adding the column to the table
			Vector sourceAttrs = new Vector(1, 1);
			sourceAttrs.addElement(supAttr.getIdentifier());
			//Following text: ..
			int idx = dTable.addDerivedAttribute("Average class " + (instanceN + 1), AttributeTypes.real, AttributeTypes.average, sourceAttrs);
			attrIdClassAverage = dTable.getAttributeId(idx);
			idx = dTable.addDerivedAttribute("Class variation " + (instanceN + 1), AttributeTypes.real, AttributeTypes.average, sourceAttrs);
			attrIdClassVariance = dTable.getAttributeId(idx);
			idx = dTable.addDerivedAttribute("Increase class " + (instanceN + 1), AttributeTypes.real, AttributeTypes.average, sourceAttrs);
			attrIdClassIncrease = dTable.getAttributeId(idx);
			idx = dTable.addDerivedAttribute("Decrease class " + (instanceN + 1), AttributeTypes.real, AttributeTypes.average, sourceAttrs);
			attrIdClassDecrease = dTable.getAttributeId(idx);
			attrIdClassNumbers = new String[10];
			Vector Nids = new Vector(12, 1);
			Nids.addElement(attrIdClassIncrease);
			Nids.addElement(attrIdClassDecrease);
			Color plotAreaColorBrighter = new Color(224, 224, 224);
			for (int i = 0; i < attrIdClassNumbers.length; i++) {
				idx = dTable.addDerivedAttribute("Class " + (instanceN + 1) + " N " + (i + 1), AttributeTypes.real, AttributeTypes.average, sourceAttrs);
				attrIdClassNumbers[i] = dTable.getAttributeId(idx);
				Nids.addElement(attrIdClassNumbers[i]);
				supervisor.getAttrColorHandler().setColorForAttribute((i % 2 == 0) ? plotAreaColor : plotAreaColorBrighter, attrIdClassNumbers[i]);
			}
			dTable.getSemanticsManager().setAttributesComparable(Nids);
		}
		br = new float[nclasses + 1][];
		for (int i = 0; i < br.length; i++) {
			br[i] = new float[1 + idxTFend - idxTFstart];
		}
		for (int j = idxTFstart; j <= idxTFend; j++) {
			DoubleArray vals = new DoubleArray(lines.size(), 10);
			for (int k = 0; k < lines.size(); k++) {
				TimeLine line = (TimeLine) lines.elementAt(k);
				if (!isActive(line.objIdx)) {
					continue;
				}
				double val = line.getValue(j);
				if (!Double.isNaN(val) && !Double.isInfinite(val)) {
					vals.addElement(val);
				}
			}
			double breaks[] = NumValManager.breakToIntervals(vals, nclasses, true);
			if (breaks == null || breaks.length < br.length) {
				// a case when requested number of breaks can not be created...
				if (vals.size() > 0) {
					br[0][j - idxTFstart] = (float) NumValManager.getMin(vals);
					br[br.length - 1][j - idxTFstart] = (float) NumValManager.getMax(vals);
				} else {
					br[0][j - idxTFstart] = (float) absMin;
					br[br.length - 1][j - idxTFstart] = (float) absMax;
				}
				//for (int i=1; i<br.length-1; i++)
				//br[i][j-idxTFstart]=0.5f*(br[0][j-idxTFstart]+br[br.length-1][j-idxTFstart]);
				for (int i = 0; i < br.length; i++) {
					br[i][j - idxTFstart] = Float.NaN; //0.5f*(br[0][j-idxTFstart]+br[br.length-1][j-idxTFstart]);
				}
			} else {
				for (int i = 0; i < br.length; i++) {
					br[i][j - idxTFstart] = (float) breaks[i];
				}
			}
		}
		if (dTable != null && nclasses > 1 && toSaveClasses) {
			int idxa = table.getAttrIndex(attrIdClassAverage), idxv = table.getAttrIndex(attrIdClassVariance), idxi = table.getAttrIndex(attrIdClassIncrease), idxd = table.getAttrIndex(attrIdClassDecrease);
			int N[] = new int[attrIdClassNumbers.length], idxn[] = new int[attrIdClassNumbers.length];
			for (int i = 0; i < N.length; i++) {
				idxn[i] = table.getAttrIndex(attrIdClassNumbers[i]);
			}
			for (int k = 0; k < lines.size(); k++) {
				TimeLine line = (TimeLine) lines.elementAt(k);
				FloatArray fa = new FloatArray(idxTFend - idxTFstart, 10);
				for (int i = 0; i < N.length; i++) {
					N[i] = 0;
				}
				for (int j = idxTFstart; j <= idxTFend; j++) {
					double val = line.getValue(j);
					if (Double.isNaN(val) || Double.isInfinite(val) || Double.isNaN(br[0][j - idxTFstart])) {
						continue;
					}
					int cln = 0;
					for (int i = 1; cln == 0 && i < br.length; i++)
						if (val <= br[i][j - idxTFstart]) {
							cln = i;
						}
					fa.addElement(cln);
					if (cln > 0) {
						N[cln - 1]++;
					}
				}
				int nplus = 0, nminus = 0;
				for (int j = 1; j <= fa.size(); j++) {
					if (fa.elementAt(j) > fa.elementAt(j - 1)) {
						nplus++;
					}
					if (fa.elementAt(j) < fa.elementAt(j - 1)) {
						nminus++;
					}
				}
				float avg = NumValManager.getMean(fa);
				dTable.setNumericAttributeValue(avg, 0, nclasses, idxa, line.objIdx);
				float variance = NumValManager.getVariance(fa, avg);
				dTable.setNumericAttributeValue(variance, 0, nclasses, idxv, line.objIdx);
				dTable.setNumericAttributeValue(nplus, idxi, line.objIdx);
				dTable.setNumericAttributeValue(nminus, idxd, line.objIdx);
				for (int i = 0; i < idxn.length; i++) {
					dTable.setNumericAttributeValue(N[i], idxn[i], line.objIdx);
				}
			}
			// inform all displays about change of values
			Vector attr = new Vector(2, 1);
			attr.addElement(attrIdClassAverage);
			attr.addElement(attrIdClassVariance);
			attr.addElement(attrIdClassIncrease);
			attr.addElement(attrIdClassDecrease);
			for (int i = 0; i < N.length; i++) {
				attr.addElement(attrIdClassNumbers[i]);
			}
			dTable.notifyPropertyChange("values", null, attr);
		}
	}

	/*
	* This variable is used when some object classification is broadcasted.
	* In this case it indicates what classes should be shown on the graph
	*/
	protected boolean showClassesLines[] = null, showClassesFlows[] = null;

	protected void setShowClassesLines(boolean showClasses[]) {
		this.showClassesLines = showClasses;
		draw(getGraphics());
	}

	protected void setShowClassesFlows(boolean showClasses[]) {
		this.showClassesFlows = showClasses;
		draw(getGraphics());
	}

	protected void findClassifier() {
		cl = null;
		tcl = null;
		if (supervisor != null && supervisor.getObjectColorer() != null && (supervisor.getObjectColorer() instanceof Classifier) && supervisor.getObjectColorer().getEntitySetIdentifier().equals(table.getEntitySetIdentifier())) {
			cl = (Classifier) supervisor.getObjectColorer();
		}
		if (cl instanceof TableClassifier) {
			tcl = (TableClassifier) cl;
			if (!table.equals(tcl.getTable())) {
				tcl = null;
			}
		}
	}

	protected void classifyLines() {
		if (lines == null)
			return;
		if (cl == null) {
			for (int i = 0; i < lines.size(); i++) {
				((TimeLine) lines.elementAt(i)).classIdx = -1;
			}
		} else if (tcl != null) {
			for (int i = 0; i < lines.size(); i++) {
				TimeLine line = (TimeLine) lines.elementAt(i);
				line.classIdx = tcl.getRecordClass(line.objIdx);
			}
		} else {
			for (int i = 0; i < lines.size(); i++) {
				TimeLine line = (TimeLine) lines.elementAt(i);
				line.classIdx = cl.getObjectClass(line.objIdx);
			}
		}
	}

	protected boolean toDrawLine(TimeLine line) {
		boolean toDraw = true;
		if (showClassesLines != null && cl != null) {
			toDraw = line.classIdx + 1 < showClassesLines.length && showClassesLines[line.classIdx + 1];
		}
		return toDraw;
	}

	private void resetAvgAndMedianLines() {
		int nObjClasses = 0;
		if (cl != null) {
			nObjClasses = cl.getNClasses();
		}
		if (nObjClasses == 0) {
			if (vLinesAvg != null) {
				vLinesAvg.removeAllElements();
				vLinesAvg = null;
			}
			if (vLinesMedian != null) {
				vLinesMedian.removeAllElements();
				vLinesMedian = null;
			}
		} else {
			vLinesAvg = new Vector(nObjClasses);
			vLinesMedian = new Vector(nObjClasses);
			for (int k = 0; k < nObjClasses; k++) {
				TimeLine line = new TimeLine();
				line.objIdx = -1;
				line.setNPoints(colNs.size());
				vLinesAvg.addElement(line);
				line = new TimeLine();
				line.objIdx = -1;
				line.setNPoints(colNs.size());
				vLinesMedian.addElement(line);
			}
		}
		if (lineAvg == null) {
			lineAvg = new TimeLine();
			lineAvg.objIdx = -1;
			lineAvg.setNPoints(colNs.size());
			lineMedian = new TimeLine();
			lineMedian.objIdx = -1;
			lineMedian.setNPoints(colNs.size());
			lineAvgValues = new float[colNs.size()];
			lineMedianValues = new float[colNs.size()];
		}
		for (int k = -1; k < nObjClasses; k++) {
			for (int j = idxTFstart; j <= idxTFend; j++) {
				DoubleArray fa = new DoubleArray(lines.size(), 10);
				for (int i = 0; i < lines.size(); i++) {
					TimeLine line = (TimeLine) lines.elementAt(i);
					if (!isActive(line.objIdx) || Double.isNaN(line.getValue(j)) || Double.isInfinite(line.getValue(j))) {
						continue;
					}
					if (k == -1 || k == line.classIdx) {
						fa.addElement(line.getValue(j));
					}
				}
				if (k == -1) {
					lineAvgValues[j] = (float) NumValManager.getMean(fa);
					lineAvg.setValue(lineAvgValues[j], j);
					lineMedianValues[j] = (float) NumValManager.getMedian(fa);
					lineMedian.setValue(lineMedianValues[j], j);
				} else {
					TimeLine line = (TimeLine) vLinesAvg.elementAt(k);
					line.setValue(NumValManager.getMean(fa), j);
					line = (TimeLine) vLinesMedian.elementAt(k);
					line.setValue(NumValManager.getMedian(fa), j);
				}
			}
		}
		computeCommonTrendLine();
		subtractTrendFromDataIfNeeded();
	}

	protected int getTimeMomentIdx(TimeMoment seekTM) {
		int idx = -1;
		for (int i = 0; i < par.getValueCount() && idx < 0; i++) {
			TimeMoment tm = (TimeMoment) par.getValue(i);
			if (tm.compareTo(seekTM) == 0) {
				idx = i;
			} else if (tm.compareTo(seekTM) > 0)
				if (i > 0) {
					idx = i - 1;
				} else {
					idx = i;
				}
		}
		return idx;
	}

	/*
	* site extent of time focusing (start, finish)
	*/
	protected boolean checkTimeInterval(TimeMoment tFocusStart, TimeMoment tFocusEnd) {
		if (tFocusStart == null || tFocusEnd == null)
			return false;
		int idx1 = getTimeMomentIdx(tFocusStart), idx2 = getTimeMomentIdx(tFocusEnd);
		return idx2 > idx1;
	}

	public void setTimeFocusStart(TimeMoment tFocusStart) {
		if (tFocusStart != null && start != null && tFocusStart.getPrecision() != start.getPrecision()) {
			tFocusStart = tFocusStart.getCopy();
			tFocusStart.setPrecision(start.getPrecision());
		}
		if (!checkTimeInterval(tFocusStart, this.tFocusEnd))
			return;
		this.tFocusStart = tFocusStart;
		idxTFstart = getTimeMomentIdx(tFocusStart);
		iLen = idxTFend - idxTFstart;
		lineAvg = null;
		lineMedian = null;
		lineAvgValues = null;
		lineMedianValues = null;
		if (colNs == null) {
			makeColumnList();
		}
		setup();
		reset(false);
	}

	public void setTimeFocusEnd(TimeMoment tFocusEnd) {
		if (tFocusEnd != null && start != null && tFocusEnd.getPrecision() != start.getPrecision()) {
			tFocusEnd = tFocusEnd.getCopy();
			tFocusEnd.setPrecision(start.getPrecision());
		}
		if (!checkTimeInterval(this.tFocusStart, tFocusEnd))
			return;
		this.tFocusEnd = tFocusEnd;
		idxTFend = getTimeMomentIdx(tFocusEnd);
		iLen = idxTFend - idxTFstart;
		lineAvg = null;
		lineMedian = null;
		lineAvgValues = null;
		lineMedianValues = null;
		if (colNs == null) {
			makeColumnList();
		}
		setup();
		reset(false);
	}

	public void setTimeFocusStartEnd(TimeMoment tFocusStart, TimeMoment tFocusEnd) {
		if (tFocusStart != null && start != null && tFocusStart.getPrecision() != start.getPrecision()) {
			tFocusStart = tFocusStart.getCopy();
			tFocusStart.setPrecision(start.getPrecision());
		}
		if (tFocusEnd != null && start != null && tFocusEnd.getPrecision() != start.getPrecision()) {
			tFocusEnd = tFocusEnd.getCopy();
			tFocusEnd.setPrecision(start.getPrecision());
		}
		if (!checkTimeInterval(tFocusStart, tFocusEnd))
			return;
		this.tFocusStart = tFocusStart;
		this.tFocusEnd = tFocusEnd;
		idxTFstart = getTimeMomentIdx(tFocusStart);
		idxTFend = getTimeMomentIdx(tFocusEnd);
		iLen = idxTFend - idxTFstart;
		lineAvg = null;
		lineMedian = null;
		lineAvgValues = null;
		lineMedianValues = null;
		if (colNs == null) {
			makeColumnList();
		}
		setup();
		reset(false);
	}

	public void setTimeFocusFullExtent() {
		tFocusStart = start;
		tFocusEnd = end;
		idxTFstart = 0;
		idxTFend = par.getValueCount() - 1;
		iLen = idxTFend - idxTFstart;
		lineAvg = null;
		lineMedian = null;
		lineAvgValues = null;
		lineMedianValues = null;
		if (colNs == null) {
			makeColumnList();
		}
		setup();
		reset(false);
	}

	/**
	* Determines the horizontal screen position for the given time moment.
	* x0 is the beginning of the x-axis (i.e. the position of the y-axis)
	*/
	public int getScrX(TimeMoment t) {
		return getScrX(t, x0, width);
	}

	protected int getScrX(TimeMoment t, int x0, int width) {
		if (t == null) {
			System.out.println("* null Time moment");
			return 0;
		}
		char c = t.getPrecision();
		t.setPrecision(tFocusStart.getPrecision());
		long l = t.subtract(tFocusStart);
		t.setPrecision(c);
		//return Math.round(1.0f*l/iLen*width)+x0;
		return Math.round(1.0f * l / tFocusEnd.subtract(tFocusStart) * width) + x0;
	}

	/**
	* Determines the horizontal screen position for the position within the whole
	* time span, specified as a float number.
	* x0 is the beginning of the x-axis (i.e. the position of the y-axis)
	*/
	protected int getScrX(float pos, int x0, int width) {
		return Math.round((pos - idxTFstart) / iLen * width) + x0;
	}

	/**
	* Determines the horizontal screen position for the position within the whole
	* time span, specified as a float number.
	*/
	public int getScrX(float pos) {
		return getScrX(pos, x0, width);
	}

	/**
	* Transforms the horizontal screen position into a position within the whole
	* time span, specified as a float number.
	* x0 is the beginning of the x-axis (i.e. the position of the y-axis)
	*/
	protected float getAbsX(int scrX, int x0, int width) {
		return idxTFstart + 1f * iLen * (scrX - x0) / width;
	}

	/**
	* Transforms the horizontal screen position into a position within the whole
	* time span, specified as a float number.
	*/
	public float getAbsX(int scrX) {
		return getAbsX(scrX, x0, width);
	}

	/**
	* Determines the vertical screen position for the given attribute value.
	* y0 is the beginning of the y-axis (i.e. the position of the x-axis).
	* Note that the x-axis is located below the graph.
	*/
	protected int getScrY(double value, int y0, int height) {
		return y0 - height + (int) Math.round((focusMax - value) / (focusMax - focusMin) * height);
	}

	/**
	* Determines the vertical screen position for the given attribute value.
	*/
	public int getScrY(double value) {
		return getScrY(value, y0, height);
	}

	/**
	* Transforms the vertical screen position into the corresponding attribute value.
	* y0 is the beginning of the y-axis (i.e. the position of the x-axis).
	* Note that the x-axis is located below the graph.
	*/
	protected double getAbsY(int scrY, int y0, int height) {
		return focusMax - (focusMax - focusMin) * (scrY - y0 + height) / height;
	}

	/**
	* Transforms the vertical screen position into the corresponding attribute value.
	*/
	public double getAbsY(int scrY) {
		return getAbsY(scrY, y0, height);
	}

	/**
	* For the given pair of y-coordinates returns the corresponding value range of
	* the attribute (possibly, transformed).
	*/
	public NumRange getValueRangeBetween(int y1, int y2) {
		if (y1 == y2)
			return null;
		NumRange nr = new NumRange();
		nr.minValue = getAbsY(y1, y0, height);
		nr.maxValue = getAbsY(y2, y0, height);
		if (nr.minValue > nr.maxValue) {
			double val = nr.minValue;
			nr.minValue = nr.maxValue;
			nr.maxValue = val;
		}
		return nr;
	}

	/**
	* For the given pair of x-coordinates, returns the list of column numbers
	* which fit between these positions on the time axis
	*/
	public IntArray getColumnsBetween(int x1, int x2) {
		if (x1 == x2)
			return null;
		if (x1 > x2) {
			int k = x1;
			x1 = x2;
			x2 = k;
		}
		int x = x0, i = idxTFstart;
		while (x1 - 3 > x && i < idxTFend) {
			++i;
			x = getScrX((TimeMoment) par.getValue(i), x0, width);
		}
		if (x1 - 3 > x)
			return null;
		int i1 = i;
		while (x2 + 3 > x && i < idxTFend) {
			++i;
			x = getScrX((TimeMoment) par.getValue(i), x0, width);
		}
		if (x >= x2 + 3) {
			--i;
		}
		if (i < i1)
			return null;
		IntArray columns = new IntArray(i - i1 + 1, 1);
		for (int j = i1; j <= i; j++) {
			columns.addElement(colNs.elementAt(j));
		}
		return columns;
	}

	/**
	* Determines the color in which the given line should be
	* drawn. The color depends on the highlighting/selection state.
	* If multi-color brushing is used (e.g. broadcasting of classification),
	* the supervisor knows about the appropriate color.
	*/
	protected Color getColorForLine(TimeLine line) {
		if (!getDrawOnlySelected() && line.selected)
			return Color.black;
		if (supervisor == null)
			return Color.gray;
		return supervisor.getColorForDataItem(table.getDataItem(line.objIdx), table.getEntitySetIdentifier(), table.getContainerIdentifier(), (getDrawOnlySelected()) ? Color.black : Color.gray);
	}

	/**
	* Sets the graphic's clip region to the plot area.
	*/
	protected void setClipToPlotArea(Graphics g) {
		g.setClip(x0, y0 - height, width, height);
	}

	/**
	* Sets the graphic's clip region to the whole area.
	*/
	protected void setClipToWholeArea(Graphics g) {
		Dimension d = getSize();
		g.setClip(0, 0, d.width, d.height);
	}

	/**
	* Checks whether the object with the given index is active, i.e. satisfies
	* the filter.
	*/
	public boolean isActive(int n) {
		if (tf == null)
			return true;
		return tf.isActive(n);
	}

	protected int leftIndent = 0, rightIndent = 0;

	public int getLeftIndent() {
		return leftIndent;
	}

	public int getRightIndent() {
		return rightIndent;
	}

	public String getTitle() {
		String name = supAttr.getName();
		if (otherParNames != null) {
			for (int i = 0; i < otherParNames.size(); i++) {
				name += ((i > 0) ? "; " : " (") + (String) otherParNames.elementAt(i) + "=" + otherParValues.elementAt(i).toString();
			}
			name += ")";
		}
		//to do: extend the name by explaining how the data were transformed
		return name;
	}

	public void updateX0andWidth() { // Used by event bar for sunchronising widths
		Graphics g = getGraphics();
		if (g == null)
			return;
		Dimension d = getSize();
		FontMetrics fm = g.getFontMetrics();
		String strMin = StringUtil.doubleToStr(focusMin, focusMin, focusMax), strMax = StringUtil.doubleToStr(focusMax, focusMin, focusMax);
		int sw1 = fm.stringWidth(strMin), sw2 = fm.stringWidth(strMax);
		x0 = 5 + Math.max(sw1, sw2);
		width = d.width - x0 - 5 - focuser.getRequiredWidth(g);
	}

	/**
	* Draws the graph
	*/
	public void draw(Graphics g) {
		//System.out.println("* draw start");
		//long t=System.currentTimeMillis();
		if (g == null)
			return;
		Dimension d = getSize();
		g.setColor(bkgColor);
		g.fillRect(0, 0, d.width, d.height);
		g.setColor(Color.black);
		g.drawRect(0, 0, d.width - 1, d.height - 1);
		if (colNs == null) {
			makeColumnList();
		}
		if (colNs == null) {
			g.drawString("No time-dependent columns detected!", 10, d.height / 2);
			return;
		}
		if (Double.isNaN(absMin) || Double.isNaN(absMax)) {
			if (Double.isNaN(absMin) && Double.isNaN(absMax)) {
				setup();
			}
			if (Double.isNaN(absMin) || Double.isNaN(absMax)) {
				g.drawString("No numeric values found!", 10, d.height / 2);
				return;
			}
			if (absMin >= absMax) {
				g.drawString("All values are the same: " + absMin + "!", 10, d.height / 2);
				return;
			}
		}
		FontMetrics fm = g.getFontMetrics();
		int asc = fm.getAscent(), fh = fm.getHeight();
		String strMin = StringUtil.doubleToStr(focusMin, focusMin, focusMax), strMax = StringUtil.doubleToStr(focusMax, focusMin, focusMax);
		int sw1 = fm.stringWidth(strMin), sw2 = fm.stringWidth(strMax);
		x0 = 5 + Math.max(sw1, sw2);
		width = d.width - x0 - 5 - focuser.getRequiredWidth(g);
		leftIndent = x0;
		rightIndent = d.width - x0 - width;
		y0 = d.height - fh - 2;
		height = y0 - 5 - fh;
		if (height < 20)
			return;
		if (colorsForTimes == null) {
			g.setColor(graphBkgColor);
			g.fillRect(x0, y0 - height, width, height);
		} else {
			g.setColor(Color.white);
			g.fillRect(x0, y0 - height, width, height);
			int stepW = (int) (width / iLen);
			for (int i = 0; i < colorsForTimes.size(); i++) {
				Object pair[] = colorsForTimes.elementAt(i);
				if (pair[0] instanceof TimeMoment) {
					TimeMoment t0 = (TimeMoment) pair[0];
					int p0 = getScrX(t0, x0, width);
					int p1 = x0 + width + stepW;
					if (i < colorsForTimes.size() - 1) {
						Object pair1[] = colorsForTimes.elementAt(i + 1);
						if (pair1[0] instanceof TimeMoment) {
							TimeMoment t1 = (TimeMoment) pair1[0];
							p1 = getScrX(t1, x0, width);
						}
					} else if (p1 - p0 < 3) {
						p0 = p1 - 3;
					}
					Color cLabBkg = bkgColor, cGraphBkg = graphBkgColor;
					if (pair[1] != null && (pair[1] instanceof Color)) {
						cLabBkg = (Color) pair[1];
						cGraphBkg = new Color(cLabBkg.getRed(), cLabBkg.getGreen(), cLabBkg.getBlue(), 100);
					}
					g.setColor(cGraphBkg);
					g.fillRect(p0, y0 - height, p1 - p0, height);
					g.setColor(cLabBkg);
					g.fillRect(p0, y0 + 1, p1 - p0, d.height - y0 - 1);
				}
			}
		}
		//draw the focuser
		focuser.setAlignmentParameters(x0 + width, y0, height); //right position
		focuser.draw(g);
		//draw TA classification
		if (taBreaks != null) {
			int ybottom = y0;
			for (int k = 0; k <= taBreaks.length; k++) {
				int y = (k < taBreaks.length) ? getScrY(taBreaks[k], y0, height) : y0 - height;
				g.setColor(taClassColors[k]);
				if (k < taBreaks.length) {
					g.drawLine(0, y, x0 + width, y);
				}
				if (drawValueClasses) {
					g.fillRect(x0 - 2, y, width + 2, ybottom - y);
				}
				ybottom = y;
			}
		}
		//draw classification
		int grPos[] = new int[1 + (int) iLen];
		grPos[0] = x0;
		for (int i = 1; i <= iLen; i++) {
			grPos[i] = getScrX((TimeMoment) par.getValue(idxTFstart + i), x0, width);
		}
		if (drawValueFlow) {
			for (int j = idxTFstart; j < idxTFend; j++) {
				if (Float.isNaN(br[0][j - idxTFstart]) || Float.isNaN(br[0][1 + j - idxTFstart])) {
					continue;
				}
				int x[] = new int[4], y[] = new int[4];
				for (int i = 0; i < nclasses; i++) {
					x[0] = grPos[j - idxTFstart];
					y[0] = getScrY(br[i][j - idxTFstart], y0, height);
					x[1] = grPos[j - idxTFstart];
					y[1] = getScrY(br[i + 1][j - idxTFstart], y0, height);
					x[2] = grPos[1 + j - idxTFstart];
					y[2] = getScrY(br[i + 1][j + 1 - idxTFstart], y0, height);
					x[3] = grPos[1 + j - idxTFstart];
					y[3] = getScrY(br[i][j + 1 - idxTFstart], y0, height);
					g.setColor((nclasses == 1) ? plotAreaColor : (i % 2 == 0) ? plotAreaColorDarker : plotAreaColorBrighter);
					g.fillPolygon(x, y, x.length);
				}
			}
		}
		//plot title
		g.setColor(Color.black);
		g.drawString(getTitle(), 5, asc + 2);
		//draw selection boxes
		if (boxes != null && boxes.size() > 0) {
			g.setColor(Color.magenta.darker());
			for (int i = 0; i < boxes.size(); i++) {
				RealRectangle r = (RealRectangle) boxes.elementAt(i);
				//System.out.println("* box "+i+", x:"+r.rx1+".."+r.rx2+", y:"+r.ry1+".."+r.ry2);
				int sx1 = getScrX(r.rx1, x0, width), sx2 = getScrX(r.rx2, x0, width), sy1 = getScrY(r.ry1, y0, height), sy2 = getScrY(r.ry2, y0, height);
				g.drawRect(sx1, sy2, sx2 - sx1, sy1 - sy2);
			}
		}
		int textY = y0 + 2 + asc;
		g.setColor(Color.gray);
		//draw the frame
		g.drawLine(x0, y0, x0, y0 - height);
		g.drawLine(x0 + width, y0, x0 + width, y0 - height);
		g.drawLine(x0, y0 - height, x0 + width, y0 - height);
		g.drawLine(x0, y0, x0 + width, y0);
		//draw horizontal grid lines
		if (drawGrid) {
			float base = 0f;
			//to do: think how to select the right base in a case of computing ratios
			//if (visCompMode!=COMP_NONE && computeRatios) base=1.0f;
			GridPosition gridp[] = GraphGridSupport.makeGrid((float) focusMin, (float) focusMax, base, height, 3 * fh, 5 * fh);
			if (gridp != null) {
				g.setColor(Color.getHSBColor(0.7f, 0.3f, 0.85f));
				for (GridPosition element : gridp) {
					int gy = y0 - element.offset;
					g.drawLine(x0, gy, x0 + width, gy);
					if (gy > y0 - height + fh + 1 && gy < y0 - fh - 1) {
						g.drawString(element.strVal, x0 - fm.stringWidth(element.strVal) - 2, gy + asc / 2);
					}
				}
			}
		}
		g.setColor(Color.darkGray);
		g.drawString(strMin, x0 - sw1 - 2, y0);
		g.drawString(strMax, x0 - sw2 - 2, y0 - height + asc);
		String str = tFocusStart.toString(); // par.getValue(0).toString();
		int sw = fm.stringWidth(str);
/*
    if (colorsForTimes!=null) {
      g.setColor(Color.white);
      g.drawString(str,x0-sw+1,textY+1);
      g.setColor(Color.darkGray);
    }
    g.drawString(str,x0-sw+0,textY);
*/
		//draw vertical grid lines and show the time moments
		g.setColor(Color.darkGray);
		g.drawLine(grPos[0], y0, grPos[0], y0 + 4);
		boolean canDrawGrid = drawGrid && width / grPos.length >= 15;
		int prevTextEnd = x0 + 7;
		for (int i = 0; i <= iLen; i++) {
			if (canDrawGrid && i > 0 && i < iLen) {
				g.setColor(Color.gray);
				for (int y = y0 - height; y <= y0 - 3; y += 8) {
					g.drawLine(grPos[i], y, grPos[i], y + 3);
				}
			}
			//if (cbTM!=null && cbTM[idxTFstart+i].getState()) {
			if (segmTM != null && segmTM[idxTFstart + i]) {
				g.setColor(Color.magenta);
				g.fillRect(grPos[i] - 2, y0 + 1, 5, 4);
				g.fillRect(grPos[i] - 1, y0 - height, 3, height);
			}
			if (trendTM != null && trendTM[idxTFstart + i]) {
				g.setColor(Color.magenta);
				g.fillRect(grPos[i] - 2, y0 + 1, 5, 4);
				g.fillRect(grPos[i] - 1, y0 - height, 3, height);
			}
			g.setColor(Color.gray);
			g.drawLine(grPos[i], y0, grPos[i], y0 + 2);
		}
		if (iLiderPeriod > 0 && iLiderOffset >= 0) {
			g.setColor(Color.black);
			//for (int i=iLiderPeriod-iLiderOffset; i<=iLen; i+=iLiderPeriod)
			//g.drawLine(grPos[i],y0-height,grPos[i],y0);
			for (int i = idxTFstart; i <= idxTFend; i++)
				if (i % iLiderPeriod == iLiderOffset) {
					g.drawLine(grPos[i - idxTFstart], y0 - height, grPos[i - idxTFstart], y0);
				}
		}
		if (currTMidx >= 0 && currTMidx >= idxTFstart && currTMidx <= idxTFend) {
			g.setXORMode(Color.white);
			g.setColor(Color.blue.brighter());
			g.drawLine(grPos[currTMidx - idxTFstart], y0 - height, grPos[currTMidx - idxTFstart], y0);
			int tx[] = new int[4], ty[] = new int[4];
			tx[0] = tx[3] = grPos[currTMidx - idxTFstart];
			ty[0] = ty[3] = y0 - height;
			tx[1] = tx[0] - 4;
			tx[2] = tx[0] + 4;
			ty[1] = ty[2] = ty[0] - 4;
			g.fillPolygon(tx, ty, 4);
			g.setPaintMode();
		}
		// drawing labels of time moments
		Vector<TimeMoment> vtlt = TimeLineDates.getTicks(tFocusStart, tFocusEnd, width);
		if (vtlt != null && vtlt.size() > 0) {
			g.setColor(Color.darkGray);
			int prevx = -1;
			for (int i = 0; i < vtlt.size(); i++) {
				int x = getScrX(vtlt.elementAt(i));
				if (i > 0 && x > 0 && prevx + fm.stringWidth(vtlt.elementAt(i - 1).toString()) > x) {
					x = -1; // skipping too frequent labels
				}
				if (x > 0) {
					g.drawString(vtlt.elementAt(i).toString(), x, textY);
					g.drawLine(x, y0, x, y0 + 4);
					prevx = x;
				}
			}
		}
/*
    else {
      int labelTotalLength=0;
      for (int i=1; i<=iLen; i++)
        labelTotalLength+=fm.stringWidth(str);
      int labelStep=1;
      float labelDx=grPos[(int)iLen]-grPos[0];
      if (labelDx==0 || labelTotalLength>labelDx)
        labelStep=(int)Math.ceil(labelTotalLength/labelDx);
      for (int i=labelStep; i<=iLen; i+=labelStep) {
        str=par.getValue(idxTFstart+i).toString();
        sw=fm.stringWidth(str);
        g.setColor(Color.darkGray);
        g.drawLine(grPos[i],y0,grPos[i],y0+4);
        g.setClip(prevTextEnd,y0,grPos[i]+6-prevTextEnd,d.height-y0);
        if (colorsForTimes!=null) {
          g.setColor(Color.white);
          g.drawString(str,grPos[i]-sw+5+1,textY+1);
        }
        //g.setColor((segmTM==null || !segmTM[idxTFstart+i])?Color.darkGray:Color.magenta);
        if (segmentation && segmTM!=null && segmTM[idxTFstart+i])
          g.setColor(Color.magenta);
        else
          if (showTrend && trendTM!=null && trendTM[idxTFstart+i])
            g.setColor(Color.magenta);
          else
            g.setColor(Color.darkGray);
        g.drawString(str,grPos[i]-sw+5,textY);
        prevTextEnd=grPos[i]+5;
      }
    }
*/
		g.setClip(0, 0, d.width, d.height);
		// calculate screen coordinates for all lines
		for (int i = 0; i < lines.size(); i++) {
			TimeLine line = (TimeLine) lines.elementAt(i);
			//for (int j=0; j<colNs.size(); j++) {
			for (int j = idxTFstart; j <= idxTFend; j++) {
				double val = line.getTransformedValue(j);
				if (Double.isNaN(val) || Double.isInfinite(val)) {
					line.setPoint(null, j);
				} else {
					Point pt = new Point(grPos[j - idxTFstart], getScrY(val, y0, height));
					line.setPoint(pt, j);
				}
			}
		}
		// draw flows of classes
		if (cl != null && cl.getNClasses() > 0 && showClassesFlows != null && showClassesFlows.length == cl.getNClasses() + 1) {
			for (int k = -1; k < cl.getNClasses(); k++)
				if (showClassesFlows[k + 1]) {
					//Color color=classifier.getClassColor(k);
					//g.setColor(new Color(color.getRed(),color.getGreen(),color.getBlue(),64));
					g.setColor((k == -1) ? new Color(160, 160, 160) : CS.desaturate(cl.getClassColor(k)));
					for (int j = idxTFstart; j < idxTFend; j++) {
						int x1 = -1, x2 = -1, y1min = -1, y1max = -1, y2min = -1, y2max = -1;
						for (int i = 0; i < lines.size(); i++) {
							TimeLine line = (TimeLine) lines.elementAt(i);
							if (!isActive(line.objIdx)) {
								continue;
							}
							if (k == line.classIdx) {
								int y1 = -1, y2 = -1;
								if (line.getPoint(j) != null) {
									y1 = line.getPoint(j).y;
									if (y1min == -1 || y1min > y1) {
										y1min = y1;
									}
									if (y1max == -1 || y1max < y1) {
										y1max = y1;
									}
									x1 = line.getPoint(j).x;
								}
								if (line.getPoint(j + 1) != null) {
									y2 = line.getPoint(j + 1).y;
									if (y2min == -1 || y2min > y2) {
										y2min = y2;
									}
									if (y2max == -1 || y2max < y2) {
										y2max = y2;
									}
									x2 = line.getPoint(j + 1).x;
								}
							}
						}
						if (x1 == -1) {
							continue;
						}
						int x[] = new int[4], y[] = new int[4];
						x[0] = x1;
						x[1] = x2;
						x[2] = x2;
						x[3] = x1;
						y[0] = y1min;
						y[1] = y2min;
						y[2] = y2max;
						y[3] = y1max;
						g.fillPolygon(x, y, 4);
					}
				}
		}
		//System.out.println("* draw after flows, elapsed time="+(System.currentTimeMillis()-t)/1000f+" sec");
		// draw lines
		g.setColor(Color.gray);
		setClipToPlotArea(g);
		boolean someSelected = false;
		if (firstDraw) {
			firstDraw = false; // as the TimeGraph is redrawn many times, we just skip the 1st drawing
		} else {
			for (int i = 0; i < lines.size(); i++) {
				TimeLine line = (TimeLine) lines.elementAt(i);
				someSelected = someSelected || line.selected;
				if (!line.selected && !drawOnlySelected && isActive(line.objIdx)) {
					if (toDrawLine(line)) {
						g.setColor(getColorForLine(line));
						/* experiment with alpha ... works very slow
						           Color color=getColorForLine(line);
						     color=new Color(color.getRed(),color.getGreen(),color.getBlue(),64);
						           g.setColor(color);
						 */
						line.draw(g);
						//System.out.println("* i="+i+" "+(System.currentTimeMillis()-t)/1000f+" sec");
					}
				}
			}
		}
		//System.out.println("* draw after draw lines, elapsed time="+(System.currentTimeMillis()-t)/1000f+" sec");
		//now draw selected lines (they must be drawn on top for better visibility)
		if (someSelected) {
			for (int i = 0; i < lines.size(); i++) {
				TimeLine line = (TimeLine) lines.elementAt(i);
				if (line.selected && isActive(line.objIdx)) {
					if (drawOnlySelected && supervisor.getObjectColorer() != null) {
						g.setColor(getColorForLine(line));
					} else {
						g.setColor(Color.black);
					}
					line.draw(g);
				}
			}
		}
		// draw events on top of time lines
		drawEvents(g);
		// draw average and median lines
		drawAverageAndMedianLines(g, grPos);
		g.setClip(0, 0, d.width, d.height);
		prevTMidx = -1;
		notifyGraphDrawn();
		//System.out.println("* draw end, elapsed time="+(System.currentTimeMillis()-t)/1000f+" sec");
	}

	protected synchronized void drawCurrTimeMomentLine() {
		Graphics g = getGraphics();
		if (g == null)
			return;
		g.setXORMode(Color.white);
		g.setColor(Color.blue.brighter());
		if (prevTMidx >= 0 && prevTMidx >= idxTFstart && prevTMidx <= idxTFend) {
			int x = getScrX(prevTMidx, x0, width);
			g.drawLine(x, y0 - height, x, y0);
			int tx[] = new int[4], ty[] = new int[4];
			tx[0] = tx[3] = x;
			ty[0] = ty[3] = y0 - height;
			tx[1] = tx[0] - 4;
			tx[2] = tx[0] + 4;
			ty[1] = ty[2] = ty[0] - 4;
			g.fillPolygon(tx, ty, 4);
		}
		if (currTMidx >= 0 && currTMidx >= idxTFstart && currTMidx <= idxTFend) {
			int x = getScrX(currTMidx, x0, width);
			g.drawLine(x, y0 - height, x, y0);
			int tx[] = new int[4], ty[] = new int[4];
			tx[0] = tx[3] = x;
			ty[0] = ty[3] = y0 - height;
			tx[1] = tx[0] - 4;
			tx[2] = tx[0] + 4;
			ty[1] = ty[2] = ty[0] - 4;
			g.fillPolygon(tx, ty, 4);
		}
		g.setPaintMode();
	}

	protected void drawEvents(Graphics g) {
		if (tblEvents == null)
			return;
		g.setColor(Color.yellow);
		String lastEventType = tblEvents.getAttrValueAsString(tblEventTypeIdx, tblEvents.getDataItemCount() - 1);
		for (int j = 0; j < tblEvents.getDataItemCount(); j++)
			if (tblEventsObjFilter == null || tblEventsObjFilter.isActive(j))
				if (tblEvents.getAttrValueAsString(tblEventTypeIdx, j).equals(lastEventType)) {
					if (currTimeLimits != null) {
						TimeReference eTRef = tblEvents.getDataItem(j).getTimeReference();
						if (eTRef != null && !currTimeLimits.doesFit(eTRef.getValidFrom())) {
							continue; //time-filtered
						}
					}
					String idLine = tblEvents.getAttrValueAsString(tblEventsIDidx, j);
					TimeLine line = null;
					for (int i = 0; i < lines.size() && line == null; i++) {
						line = (TimeLine) lines.elementAt(i);
						if (isActive(line.objIdx) && line.objId.equals(idLine)) {
							;
						} else {
							line = null;
						}
					}
					if (line == null) {
						continue;
					}
					if (drawOnlySelected && !line.selected) {
						continue;
					}
					TimeMoment tm = (TimeMoment) tblEvents.getAttrValue(tblEventsTidx, j);
					int idx = par.getValueIndex(tm);
					if (idx >= idxTFstart && idx <= idxTFend) {
						if (selectedEvents == null || selectedEvents.indexOf(tblEvents.getDataItemId(j)) == -1) {
							g.setColor(Color.yellow);
						} else {
							g.setColor(Color.green.brighter());
						}
						Point p = line.getPoint(idx);
						g.drawLine(p.x - 2, p.y - 2, p.x + 2, p.y + 2);
						g.drawLine(p.x - 2, p.y + 2, p.x + 2, p.y - 2);
						g.drawLine(p.x - 1, p.y - 2, p.x + 3, p.y + 2);
						g.drawLine(p.x - 1, p.y + 2, p.x + 3, p.y - 2);
					}
				}
	}

	protected void drawAverageAndMedianLines(Graphics g, int grPos[]) {
		for (int am = 0; am <= 1; am++) {
			for (int k = -1; k < ((vLinesAvg == null) ? 0 : vLinesAvg.size()); k++) {
				TimeLine line = null;
				if (k == -1) {
					line = (am == 0) ? lineAvg : lineMedian;
				} else {
					line = (TimeLine) ((am == 0) ? vLinesAvg.elementAt(k) : vLinesMedian.elementAt(k));
				}
				for (int j = idxTFstart; j <= idxTFend; j++) {
					double val = line.getValue(j);
					if (Double.isNaN(val) || Double.isInfinite(val)) {
						line.setPoint(null, j);
					} else {
						Point pt = new Point(grPos[j - idxTFstart], getScrY(val, y0, height));
						line.setPoint(pt, j);
					}
				}
			}
		}
		if (showLineAvg) {
			for (int k = -1; k < ((vLinesAvg == null) ? 0 : vLinesAvg.size()); k++) {
				TimeLine line = (k == -1) ? lineAvg : (TimeLine) vLinesAvg.elementAt(k);
				if (k == -1 || (cl != null && showClassesLines != null && k + 1 < showClassesLines.length && showClassesLines[k + 1])) {
					g.setColor((k == -1 || cl == null) ? Color.black : cl.getClassColor(k).darker());
					line.drawBorderedLine(g);
				}
			}
		}
		if (showLineMedian) {
			for (int k = -1; k < ((vLinesMedian == null) ? 0 : vLinesMedian.size()); k++) {
				TimeLine line = (k == -1) ? lineMedian : (TimeLine) vLinesMedian.elementAt(k);
				if (k == -1 || (showClassesLines != null && k + 1 < showClassesLines.length && showClassesLines[k + 1])) {
					g.setColor((k == -1 || cl == null) ? Color.black : cl.getClassColor(k).darker());
					line.drawBorderedLine(g);
				}
			}
		}
		if (showTrend && lineTrend != null) {
			for (int j = idxTFstart; j <= idxTFend; j++) {
				double val = lineTrend.getValue(j);
				if (Double.isNaN(val) || Double.isInfinite(val)) {
					lineTrend.setPoint(null, j);
				} else {
					Point pt = new Point(grPos[j - idxTFstart], getScrY(val, y0, height));
					lineTrend.setPoint(pt, j);
				}
			}
			g.setColor(Color.black);
			lineTrend.drawBorderedLine(g);
		}
	}

	/**
	* Draws the graph
	*/
	@Override
	public void paint(Graphics g) {
		draw(g);
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(450, 200);
	}

	/**
	* Notification about change of the set of objects to be transiently
	* highlighted. The argument "source" is usually a reference to a
	* Highlighter. The argument setId is the identifier of the set the
	* highlighted objects belong to (e.g. map layer or table).
	* The argument highlighted is a vector of identifiers of
	* currently highlighted objects.
	*/
	@Override
	public void highlightSetChanged(Object source, String setId, Vector highlighted) {
		if (tblEvents != null && setId.equals(tblEvents.getEntitySetIdentifier()))
			return;
		if (lines == null)
			return;
		Graphics g = getGraphics();
		if (g == null)
			return;
		setClipToPlotArea(g);
		//remove previous highlighting
		if (hLines != null && hLines.size() > 0) {
			boolean someSelected = false;
			for (int i = 0; i < hLines.size(); i++) {
				TimeLine l = (TimeLine) hLines.elementAt(i);
				if (isActive(l.objIdx) && toDrawLine(l))
					if (!l.selected && !drawOnlySelected) {
						g.setColor(getColorForLine(l));
						l.draw(g);
					} else {
						someSelected = true;
					}
			}
			if (someSelected) {
				for (int i = 0; i < hLines.size(); i++) {
					TimeLine l = (TimeLine) hLines.elementAt(i);
					if (l.selected && isActive(l.objIdx) && toDrawLine(l)) {
						if (drawOnlySelected) {
							g.setColor(getColorForLine(l));
						} else {
							g.setColor(Color.black);
						}
						l.draw(g);
					}
				}
			}
			hLines.removeAllElements();
		}
		if (highlighted != null && highlighted.size() > 0) {
			if (hLines == null) {
				hLines = new Vector(20, 10);
			}
			g.setColor(Color.white);
			for (int i = 0; i < lines.size(); i++) {
				TimeLine l = (TimeLine) lines.elementAt(i);
				if (StringUtil.isStringInVectorIgnoreCase(l.objId, highlighted) && (l.selected || !drawOnlySelected) && isActive(l.objIdx) && toDrawLine(l)) {
					l.draw(g);
					hLines.addElement(l);
				}
			}
		}
		setClipToWholeArea(g);
	}

	/**
	* Notification about change of the set of objects to be selected (durably
	* highlighted). The argument "source" is usually a reference to a
	* Highlighter. The argument setId is the identifier of the set the
	* highlighted objects belong to (e.g. map layer or table).
	* The argument selected is a vector of identifiers of
	* currently selected objects.
	*/
	@Override
	public void selectSetChanged(Object source, String setId, Vector selected) {
		if (tblEvents != null && setId.equals(tblEvents.getEntitySetIdentifier())) {
			selectedEvents = selected;
			// select lines containing the events
			Highlighter hl = supervisor.getHighlighter(table.getEntitySetIdentifier());
			if (selectedEvents == null) {
				hl.clearSelection(this);
				draw(getGraphics());
				return;
			}
			Vector<String> v = new Vector(100, 100);
			for (int evtIdx = 0; evtIdx < tblEvents.getDataItemCount(); evtIdx++)
				if (tblEventsObjFilter == null || tblEventsObjFilter.isActive(evtIdx))
					if (selectedEvents.indexOf(tblEvents.getDataItemId(evtIdx)) >= 0) {
						v.addElement((String) tblEvents.getAttrValue(tblEvents.getAttrIndex("__time_series_name__"), evtIdx));
					}
			hl.replaceSelectedObjects(tblEvents, v);
			// redraw
			draw(getGraphics());
			return;
		}
		if (!this.equals(source)) {
			clearSegmTM();
		}
		if (lines == null)
			return;
		Graphics g = getGraphics();
		if (g == null)
			return;
		if (drawOnlySelected || tblEvents != null) {
			int nSelected = 0;
			for (int i = 0; i < lines.size(); i++) {
				TimeLine l = (TimeLine) lines.elementAt(i);
				l.selected = StringUtil.isStringInVectorIgnoreCase(l.objId, selected);
				if (l.selected) {
					++nSelected;
				}
			}
			if (nSelected < 1) {
				currTimeLimits = null;
			}
			synchronized (this) {
				draw(g);
			}
			if (!source.equals(tblEvents)) {
				selectEventsByTimeLines();
			}
			return;
		}
		//redraw the lines that are no more selected
		setClipToPlotArea(g);
		for (int i = 0; i < lines.size(); i++) {
			TimeLine l = (TimeLine) lines.elementAt(i);
			if (l.selected && !StringUtil.isStringInVectorIgnoreCase(l.objId, selected)) {
				l.selected = false;
				if (isActive(l.objIdx) && toDrawLine(l)) {
					g.setColor(getColorForLine(l));
					l.draw(g);
				}
			}
		}
		selectEventsByTimeLines();
		if (selected == null || selected.size() < 1) {
			setClipToWholeArea(g);
			return;
		}
		//redraw the lines that are currently selected
		for (int i = 0; i < lines.size(); i++) {
			TimeLine l = (TimeLine) lines.elementAt(i);
			if (l.selected || StringUtil.isStringInVectorIgnoreCase(l.objId, selected)) {
				l.selected = true;
				if (isActive(l.objIdx) && toDrawLine(l))
					if (hLines != null && hLines.contains(l)) {
						g.setColor(Color.white);
						l.draw(g);
					} else {
						if (drawOnlySelected) {
							g.setColor(getColorForLine(l));
						} else {
							g.setColor(Color.black);
						}
						l.draw(g);
					}
			}
		}
		setClipToWholeArea(g);
	}

	/**
	* Checks whether the graph shows the attributes listed in the vector
	*/
	public boolean showsAttributes(Vector v) {
		if (v == null)
			return false;
		for (int i = 0; i < v.size(); i++) {
			int idx = table.getAttrIndex((String) v.elementAt(i));
			if (colNs.indexOf(idx) >= 0)
				return true;
		}
		return false;
	}

	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		if (pce.getPropertyName().equals("time_limits")) {
			currTimeLimits = (TimeLimits) pce.getNewValue();
		} else if (pce.getSource() instanceof BroadcastClassesCP) {
			BroadcastClassesCPinfo bccpi = (BroadcastClassesCPinfo) pce.getNewValue();
			boolean flags[] = bccpi.showClasses;
			if (pce.getPropertyName().equals(TimeGraphPanel.eventShowClassesFlows)) {
				setShowClassesFlows(flags);
			} else {
				setShowClassesLines(flags);
			}
		} else if (pce.getSource().equals(fint)) {
			if (pce.getPropertyName().equals("current_interval")) {
				//System.out.println("Interval from "+pce.getOldValue()+" to "+pce.getNewValue());
				TimeMoment currtm = (TimeMoment) pce.getNewValue();
				if (currtm == null)
					return;
				if (currtm.getPrecision() != start.getPrecision()) {
					currtm = currtm.getCopy();
					currtm.setPrecision(start.getPrecision());
				}
				prevTMidx = currTMidx;
				currTMidx = getTimeMomentIdx(currtm);
				if (currTMidx != prevTMidx) {
					drawCurrTimeMomentLine();
				}
			} else if (pce.getPropertyName().equals("animation")) {
				System.out.println("Animation " + pce.getNewValue()); //"start" or "stop"
			}
		} else if (pce.getPropertyName().equals(Supervisor.eventObjectColors)) {
			if (table.getEntitySetIdentifier().equals(pce.getNewValue())) {
				findClassifier();
				classifyLines();
				resetAvgAndMedianLines();
				draw(getGraphics());
			}
		} else if (pce.getSource().equals(supervisor) && pce.getPropertyName().equals(Supervisor.eventTimeColors)) {
			if (pce.getNewValue() == null) {
				colorsForTimes = null;
			} else {
				tryGetColorsForTimes();
			}
			draw(getGraphics());
		} else if (pce.getSource().equals(tf)) {
			if (pce.getPropertyName().equals("destroyed")) {
				tf.removePropertyChangeListener(this);
				tf = null;
			} else {
				reset(false);
				draw(getGraphics());
			}
		} else if (pce.getSource().equals(aTrans) && pce.getPropertyName().equals("values")) {
			reset(true);
		} else if (pce.getSource().equals(table)) {
			if (pce.getPropertyName().equals("destroyed") || pce.getPropertyName().equals("structure_complete")) {
				destroy();
			} else if (pce.getPropertyName().equalsIgnoreCase("filter")) {
				if (tf != null) {
					tf.removePropertyChangeListener(this);
				}
				tf = table.getObjectFilter();
				if (tf != null) {
					tf.addPropertyChangeListener(this);
				}
				repaint();
			} else if (pce.getPropertyName().equals("values")) {
				Vector v = (Vector) pce.getNewValue(); // list of changed attributes
				if (showsAttributes(v)) {
					reset(true);
				}
			} else if (pce.getPropertyName().equals("data_added") || pce.getPropertyName().equals("data_removed") || pce.getPropertyName().equals("data_updated")) {
				setup();
				reset(true);
			}
		} else if (pce.getSource().equals(tblEvents)) {
			if (pce.getPropertyName().equalsIgnoreCase("filter")) {
				if (tblEventsObjFilter != null) {
					tblEventsObjFilter.removePropertyChangeListener(this);
				}
				tblEventsObjFilter = tblEvents.getObjectFilter();
				if (tblEventsObjFilter != null) {
					tblEventsObjFilter.addPropertyChangeListener(this);
				}
			}
		}
		if (pce.getSource().equals(tblEventsObjFilter)) {
			draw(getGraphics());
		}
	}

//---------------- implementation of the EventSource interface ------------
	/**
	* The EventSource answers whether it can produce the specified
	* type of events.
	*/
	@Override
	public boolean doesProduceEvent(String eventId) {
		return eventId != null && (eventId.equals(ObjectEvent.click) || eventId.equals(ObjectEvent.dblClick) || eventId.equals(ObjectEvent.point) || eventId.equals(ObjectEvent.frame));
	}

	/**
	* Returns a unique identifier of the event source (may be produced
	* automatically, used only internally, not shown to the user).
	* The identifier is used for explicit linking of producers and recipients of
	* object events.
	* To produce an identifier, the base class Plot uses the methods
	* getPlotTypeName() and getInstanceN() that are to be defined in the
	* descendants
	*/
	@Override
	public String getIdentifier() {
		return "TimeGraph_" + instanceN;
	}

//----------------------- reaction to mouse events -----------------
	/**
	* Checks whether the given point is in the plot area (where the lines are drawn)
	*/
	protected boolean isPointInPlotArea(int x, int y) {
		return x >= x0 && x <= x0 + width && y >= y0 - height && y <= y0;
	}

	/**
	* Checks if there are any highlighted lines
	*/
	public boolean hasHighlighted() {
		return hLines != null && hLines.size() > 0;
	}

	/**
	* Checks if there are any selected lines
	*/
	public boolean hasSelected() {
		if (lines == null)
			return false;
		for (int i = 0; i < lines.size(); i++)
			if (((TimeLine) lines.elementAt(i)).selected)
				return true;
		return false;
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	} //see mouseReleased

	@Override
	public void mouseExited(MouseEvent e) { //dehighlight all highlighted objects
		if (supervisor == null || lines == null)
			return;
		ObjectEvent oevt = new ObjectEvent(this, ObjectEvent.point, e, table.getEntitySetIdentifier()); //no objects to highlight
		supervisor.processObjectEvent(oevt);
	}

	/**
	* The index of the point in the line(s) currently shown in the popup window.
	*/
	private int pIdx = -1;

	protected DataTable tblEvents = null;
	protected int tblEventsIDidx = -1, tblEventsTidx = -1, tblEventTypeIdx = -1, tblEventVidx = -1;
	protected ObjectFilter tblEventsObjFilter = null;
	protected Vector selectedEvents = null;
	/**
	 * Used for the selection of the events by time
	 */
	protected TimeLimits currTimeLimits = null;

	public void setEventLayer(DataTable tblEvents) {
		if (tblEvents != null) {
			Highlighter hl = supervisor.getHighlighter(tblEvents.getEntitySetIdentifier());
			if (hl != null) {
				hl.removeHighlightListener(this);
			}
		}
		this.tblEvents = tblEvents;
		if (tblEvents == null)
			return;
		tblEventsIDidx = tblEvents.findAttrByName("Time series Id");
		tblEventsTidx = tblEvents.findAttrByName("Start time");
		tblEventTypeIdx = tblEvents.findAttrByName("Event type");
		tblEventVidx = tblEvents.findAttrByName("Event value");
		if (tblEventsIDidx < 0 || tblEventsTidx < 0 || tblEventTypeIdx < 0 || tblEventVidx < 0) {
			tblEvents = null;
		}
		if (tblEvents != null) {
			tblEvents.addPropertyChangeListener(this);
			supervisor.getHighlighter(tblEvents.getEntitySetIdentifier()).addHighlightListener(this);
		}
		if (tblEvents.getObjectFilter() != null) {
			tblEventsObjFilter = tblEvents.getObjectFilter();
			tblEvents.getObjectFilter().addPropertyChangeListener(this);
		}
		repaint();
	}

	public DataTable getEventTable() {
		return tblEvents;
	}

	protected void selectEventsByTimeLines() {
		if (tblEvents == null)
			return;
		Highlighter evh = supervisor.getHighlighter(tblEvents.getEntitySetIdentifier());
		if (evh == null)
			return;
		Vector vs = new Vector(10, 10);
		String str = "";
		for (int i = 0; i < lines.size(); i++) {
			TimeLine l = (TimeLine) lines.elementAt(i);
			if (isActive(l.objIdx) && l.selected) {
				for (int j = 0; j < tblEvents.getDataItemCount(); j++)
					if (tblEvents.getAttrValueAsString(tblEventsIDidx, j).equals(l.objId)) {
						boolean timeFiltered = false;
						TimeReference eTRef = tblEvents.getDataItem(j).getTimeReference();
						if (currTimeLimits != null) {
							timeFiltered = eTRef != null && !currTimeLimits.doesFit(eTRef.getValidFrom());
						}
						if (!timeFiltered) {
							vs.addElement(tblEvents.getThematicData(j).getId());
							str += " " + tblEvents.getThematicData(j).getId();
						}
					}
			}
		}
		//System.out.println("* select "+vs.size()+" events: "+str);
		if (vs.size() > 0) {
			evh.replaceSelectedObjects(this, vs);
		} else {
			evh.clearSelection(this);
			currTimeLimits = null;
		}
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		if (supervisor == null || lines == null)
			return;
		int x = e.getX(), y = e.getY();
		if (!isPointInPlotArea(x, y)) {
			if (hasHighlighted()) {
				ObjectEvent oevt = new ObjectEvent(this, ObjectEvent.point, e, table.getEntitySetIdentifier());
				//to dehighlight all the objects
				supervisor.processObjectEvent(oevt);
			}
			if (popM != null) {
				PopupManager.hideWindow();
				if (y <= y0 - height || y >= y0) {
					popM.setKeepHidden(true);
				} else {
					double val = getAbsY(y, y0, height);
					popM.setText(StringUtil.doubleToStr(val, absMin, absMax));
					popM.setKeepHidden(false);
					popM.startShow(e.getX(), e.getY());
				}
				pIdx = -1;
			}
			return;
		}
		ObjectEvent oevt = new ObjectEvent(this, ObjectEvent.point, e, table.getEntitySetIdentifier());
		int count = 0, idx = -1;
		IntArray lineNs = new IntArray(20, 10);
		for (int i = 0; i < lines.size() && count < 20; i++) {
			TimeLine l = (TimeLine) lines.elementAt(i);
			if ((!drawOnlySelected || l.selected) && isActive(l.objIdx)) {
				int fit = l.fitPointIdx(x, y);
				if (fit >= 0) {
					oevt.addEventAffectedObject(l.objId);
					++count;
					lineNs.addElement(i);
					if (idx < 0) {
						idx = fit;
					}
				}
			}
		}
		if (lineNs.size() < 1) {
			PopupManager.hideWindow();
			double val = getAbsY(y, y0, height);
			String strTime = "";
			if (lines != null && lines.size() > 0) {
				TimeLine l = (TimeLine) lines.elementAt(0);
				idx = l.getClosestPointIdx(x);
				if (idx > 0) {
					strTime += par.getValue(idx);
				}
			}
			String text = ((strTime.length() > 0) ? "time=" + strTime + "\n" : "") + "value=" + StringUtil.doubleToStr(val, absMin, absMax);
			popM.setText(text);
			popM.setKeepHidden(false);
			popM.startShow(e.getX(), e.getY());
			pIdx = -1;
			if (!hasHighlighted())
				return;
			//to avoid unnecessary messages to highlighter about mouse
			//movements in the object-free area
		}

		boolean same = hLines != null && hLines.size() == lineNs.size();
		if (hLines != null) {
			for (int i = 0; i < hLines.size() && same; i++) {
				TimeLine l = (TimeLine) hLines.elementAt(i);
				same = lineNs.indexOf(i) >= 0;
			}
		}
		if (!same) {
			supervisor.processObjectEvent(oevt);
		}
		if (!same || pIdx != idx) {
			PopupManager.hideWindow();
			pIdx = idx;
			if (pIdx >= 0 && pIdx < colNs.size()) {
				int colN = colNs.elementAt(pIdx);
				String txt = par.getValue(pIdx).toString() + "\n";
				String strLineId = "";
				for (int i = 0; i < lineNs.size(); i++) {
					if (i > 0) {
						txt += "\n------------------\n";
					}
					TimeLine line = (TimeLine) lines.elementAt(lineNs.elementAt(i));
					String id = table.getDataItemId(line.objIdx), name = table.getDataItemName(line.objIdx);
					if (name == null || name.equalsIgnoreCase(id)) {
						txt += id;
					} else {
						txt += id + " " + name;
					}
					ThematicDataItem data = (ThematicDataItem) table.getDataItem(line.objIdx);
					String str = data.getAttrValue(colN).toString();
					if (aTrans != null) {
						String t = aTrans.getTransformedValueAsString(line.objIdx, colN);
						if (t != null) {
							str += " >> " + t;
						}
					}
					txt += ": " + str + "\n";
					if (tblEvents != null) { // may be, an event exists in this time series?
						boolean found = false;
						for (int j = tblEvents.getDataItemCount() - 1; j >= 0 && !found; j--)
							if (tblEvents.getAttrValueAsString(tblEventsIDidx, j).equals(id) && par.getValue(pIdx).equals(tblEvents.getAttrValue(tblEventsTidx, j))) {
								txt += tblEvents.getAttrValueAsString(tblEventTypeIdx, j) + ": " + tblEvents.getAttrValueAsString(tblEventVidx, j) + "\n";
								for (idx = 0; idx < tblEvents.getAttrCount(); idx++)
									if (tblEvents.getAttributeName(idx).endsWith(": counts")) {
										txt += "\n--- " + tblEvents.getAttributeName(idx) + " ---\n";
										str = tblEvents.getAttrValueAsString(idx, j);
										if (str != null && str.length() > 0) {
											txt += "\n" + str;
										}
									}
							}
					}
				}
				if (txt.length() > 0) {
					popM.setText(txt);
				}
			}
		}
		popM.setKeepHidden(false);
		popM.startShow(e.getX(), e.getY());
	}

	/**
	* Indicates the mouse being currently dragged
	*/
	private boolean drag = false;
	/**
	* Mouse position at the beginning of dragging
	*/
	private int startX = -1, startY = -1;
	/**
	* Mouse position at the current moment of of dragging
	*/
	private int lastX = -1, lastY = -1;

	/**
	* Sets the component, to which to pass all mouse drag events instead of
	* processing them in the time graph.
	*/
	public void setMouseDragConsumer(MouseDragEventConsumer consumer) {
		mouseDragConsumer = consumer;
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if (focuser.isMouseCaptured()) {
			focuser.mouseDragged(e.getX(), e.getY(), getGraphics());
			return;
		}
		int x = e.getX(), y = e.getY();
		if (!drag) {
			if (startX < 0 || startY < 0)
				return;
			drag = true;
			if (mouseDragConsumer != null) {
				mouseDragConsumer.mouseDragBegin(startX, startY, x, y);
			} else {
				drawFrame(startX, startY, x, y);
			}
		} else {
			if (x < x0) {
				x = x0;
			} else if (x > x0 + width) {
				x = x0 + width;
			}
			if (y < y0 - height) {
				y = y0 - height;
			} else if (y > y0) {
				y = y0;
			}
			if (x == lastX && y == lastY)
				return;
			if (mouseDragConsumer != null) {
				mouseDragConsumer.mouseDragging(lastX, lastY, x, y);
			} else {
				if (lastX >= 0 && lastY >= 0) {
					drawFrame(startX, startY, lastX, lastY); //erase old
				}
				drawFrame(startX, startY, x, y);
			}
		}
		lastX = x;
		lastY = y;
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
		focuser.captureMouse(e.getX(), e.getY());
		if (!focuser.isMouseCaptured() && isPointInPlotArea(e.getX(), e.getY())) {
			startX = e.getX();
			startY = e.getY();
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (focuser.isMouseCaptured()) {
			focuser.releaseMouse();
			return;
		}
		if (drag) //finish dragging
			if (mouseDragConsumer != null) {
				mouseDragConsumer.mouseDragEnd(lastX, lastY);
			} else {
				clearSegmTM();
				if (lastX >= 0 && lastY >= 0) {
					drawFrame(startX, startY, lastX, lastY); //erase old
				}
				if (startX >= 0 && startY >= 0 && lastX >= 0 && lastY >= 0) {
					selectInFrame(startX, startY, lastX, lastY, e);
				}
			}
		else {
			int x = e.getX(), y = e.getY();
			boolean alreadyProcessed = false;
			if (segmentation || showTrend) {
				//selection of a reference time moment
				int pvIdx = -1;
				int dist = 0;
				if (x <= x0) {
					pvIdx = idxTFstart;
				} else if (x >= x0 + width) {
					pvIdx = idxTFend;
				} else {
					dist = Math.abs(x - x0);
					for (int i = 1; i <= iLen && pvIdx < 0; i++) {
						int pos = getScrX((TimeMoment) par.getValue(idxTFstart + i), x0, width);
						int d = Math.abs(x - pos);
						if (d < dist) {
							dist = d;
						} else {
							pvIdx = idxTFstart + i - 1;
						}
					}
					if (pvIdx < 0) {
						pvIdx = idxTFend;
					}
				}
				if (dist < 3) { // if close to vertical line, than segmentation/showTrend selection, otherwise normal selection of lines
					if (segmentation) {
						if (segmTM == null) {
							segmTM = new boolean[par.getValueCount()];
							segmTM[pvIdx] = true;
						} else {
							segmTM[pvIdx] = !segmTM[pvIdx];
						}
						repaint();
						selectLinesInSegmMode();
					} else { // showTrend
						if (trendTM == null) {
							trendTM = new boolean[par.getValueCount()];
							trendTM[0] = true;
							trendTM[trendTM.length - 1] = true;
							trendTM[pvIdx] = true;
						} else if (pvIdx != 0 && pvIdx != trendTM.length - 1) {
							trendTM[pvIdx] = !trendTM[pvIdx];
						}
						computeCommonTrendLine();
						repaint();
					}
					alreadyProcessed = true;
				}
			}
			if (!alreadyProcessed) { //object selection by click
				clearSegmTM();
				if (boxes != null && boxes.size() > 0) {
					boxes.removeAllElements();
					repaint();
				}
				if (!isPointInPlotArea(x, y)) {
					if (hasSelected()) {
						ObjectEvent oevt = new ObjectEvent(this, (e.getClickCount() > 1) ? ObjectEvent.dblClick : ObjectEvent.click, e, table.getEntitySetIdentifier());
						//to deselect all the objects
						supervisor.processObjectEvent(oevt);
					}
					return;
				}
				ObjectEvent oevt = new ObjectEvent(this, (e.getClickCount() > 1) ? ObjectEvent.dblClick : ObjectEvent.click, e, table.getEntitySetIdentifier());
				for (int i = 0; i < lines.size(); i++) {
					TimeLine l = (TimeLine) lines.elementAt(i);
					if (isActive(l.objIdx) && l.fitPointIdx(x, y) >= 0) {
						oevt.addEventAffectedObject(l.objId);
					}
				}
				if (oevt.getAffectedObjectCount() < 1 && !hasSelected())
					return;
				//to avoid unnecessary messages to highlighter
				supervisor.processObjectEvent(oevt);
				selectEventsByTimeLines();
			}
		}
		drag = false;
		startX = startY = lastX = lastY = -1;
	}

	protected void drawFrame(int x0, int y0, int x, int y) {
		if (x - x0 != 0 || y - y0 != 0) {
			Graphics gr = getGraphics();
			gr.setColor(Color.magenta);
			gr.setXORMode(plotAreaColor);
			gr.drawLine(x0, y0, x, y0);
			gr.drawLine(x, y0, x, y);
			gr.drawLine(x, y, x0, y);
			gr.drawLine(x0, y, x0, y0);
			gr.setPaintMode();
			gr.dispose();
		}
	}

	protected void selectInFrame(int x1, int y1, int x2, int y2, MouseEvent sourceME) {
		if (lines == null || lines.size() < 1 || supervisor == null)
			return;
		if (x2 < x1) {
			int x = x1;
			x1 = x2;
			x2 = x;
		}
		if (y2 < y1) {
			int y = y1;
			y1 = y2;
			y2 = y;
		}
		Vector selected = new Vector(20, 20);
		for (int i = 0; i < lines.size(); i++) {
			TimeLine line = (TimeLine) lines.elementAt(i);
			if (isActive(line.objIdx) && line.fitsInRectangle(x1, y1, x2, y2)) {
				selected.addElement(((TimeLine) lines.elementAt(i)).objId);
			}
		}
		Highlighter hl = supervisor.getHighlighter(table.getEntitySetIdentifier());
		Vector oldSel = hl.getSelectedObjects();
		boolean someWereSelected = oldSel != null && oldSel.size() > 0;
		boolean needRepaint = false;
		if (boxes != null && boxes.size() > 0 && (selectMode == SEL_REPLACE || selectMode == SEL_TOGGLE || (selectMode == SEL_AND && !someWereSelected))) {
			boxes.removeAllElements();
			needRepaint = true;
		}
		if (selectMode != SEL_TOGGLE) {
			if (boxes == null) {
				boxes = new Vector(10, 10);
			}
			RealRectangle r = new RealRectangle(getAbsX(x1, x0, width), (float) getAbsY(y1, y0, height), getAbsX(x2, x0, width), (float) getAbsY(y2, y0, height));
			boxes.addElement(r);
			needRepaint = true;
		}
		if (needRepaint) {
			repaint();
		}
		switch (selectMode) {
		case SEL_REPLACE:
			if (selected.size() < 1) {
				hl.clearSelection(this);
			} else {
				hl.replaceSelectedObjects(this, selected);
			}
			break;
		case SEL_OR:
			if (selected.size() > 0) {
				if (!someWereSelected) {
					hl.makeObjectsSelected(this, selected);
				} else {
					for (int i = selected.size() - 1; i >= 0; i--)
						if (StringUtil.isStringInVectorIgnoreCase((String) selected.elementAt(i), oldSel)) {
							selected.removeElementAt(i);
						}
					if (selected.size() > 0) {
						hl.makeObjectsSelected(this, selected);
					}
				}
			}
			break;
		case SEL_AND:
			if (selected.size() < 1) {
				hl.clearSelection(this);
			} else if (someWereSelected) {
				for (int i = oldSel.size() - 1; i >= 0; i--)
					if (!StringUtil.isStringInVectorIgnoreCase((String) oldSel.elementAt(i), selected)) {
						oldSel.removeElementAt(i);
					}
				if (oldSel.size() < 1) {
					hl.clearSelection(this);
				} else {
					hl.replaceSelectedObjects(this, oldSel);
				}
			} else {
				hl.makeObjectsSelected(this, selected);
			}
			break;
		default:
			if (selected.size() > 0) {
				hl.makeObjectsSelected(this, selected);
			} else {
				hl.clearSelection(this);
			}
			break;
		}
	}

	protected void buildBox(int idx) {
		float min = Float.NaN, max = Float.NaN;
		for (int i = 0; i < lines.size(); i++) {
			TimeLine line = (TimeLine) lines.elementAt(i);
			if (!isActive(line.objIdx)) {
				continue;
			}
			double v = line.getValue(idx);
			if (Double.isNaN(v) || Double.isInfinite(v)) {
				continue;
			}
			if (Double.isNaN(min) || v < min) {
				min = (float) v;
			}
			if (Double.isNaN(max) || v > max) {
				max = (float) v;
			}
		}
		RealRectangle r = new RealRectangle(idx - 0.1f, min, idx + 0.1f, max);
		boxes.addElement(r);
	}

	protected void selectLinesByBoxes() {
		Highlighter hl = supervisor.getHighlighter(table.getEntitySetIdentifier());
		if (boxes.size() == 0) {
			hl.clearSelection(this);
			return;
		}
		Vector selected = new Vector(20, 20);
		for (int i = 0; i < lines.size(); i++) {
			TimeLine line = (TimeLine) lines.elementAt(i);
			if (!isActive(line.objIdx)) {
				continue;
			}
			boolean ok = true;
			for (int n = 0; n < boxes.size(); n++) {
				RealRectangle r = (RealRectangle) boxes.elementAt(n);
				int idx = Math.round(r.ry1);
				double v = line.getValue(idx);
				if (Double.isNaN(v) || Double.isInfinite(v)) {
					ok = false;
					break;
				}
			}
			if (ok) {
				selected.addElement(line.objId);
			}
		}
		if (selected.size() > 0) {
			hl.replaceSelectedObjects(this, selected);
		} else {
			hl.clearSelection(this);
		}
	}

	protected void selectLinesInSegmMode() {
		boxes = null;
		Highlighter hl = supervisor.getHighlighter(table.getEntitySetIdentifier());
		if (segmTM == null) {
			hl.clearSelection(this);
			return;
		}
		boolean anySelected = false;
		for (int n = 0; n < segmTM.length && !anySelected; n++) {
			anySelected = segmTM[n];
		}
		if (!anySelected) {
			hl.clearSelection(this);
			return;
		}
		Vector selected = new Vector(20, 20);
		for (int i = 0; i < lines.size(); i++) {
			TimeLine line = (TimeLine) lines.elementAt(i);
			if (!isActive(line.objIdx)) {
				continue;
			}
			boolean ok = true;
			for (int n = 0; n < segmTM.length; n++)
				if (segmTM[n]) {
					double v = line.getValue(n);
					if (Double.isNaN(v) || Double.isInfinite(v)) {
						ok = false;
						break;
					}
				}
			if (ok) {
				selected.addElement(line.objId);
			}
		}
		if (selected.size() > 0) {
			hl.replaceSelectedObjects(this, selected);
		} else {
			hl.clearSelection(this);
		}
	}

//-------------------- FocusListener interface ---------------------------------
	@Override
	public void focusChanged(Object source, double lowerLimit, double upperLimit) {
		if (source.equals(focuser)) {
			if (focusMin != lowerLimit || focusMax != upperLimit) {
				focusMin = lowerLimit;
				focusMax = upperLimit;
				repaint();
			} else {
				eraseFocusLines();
			}
			lastLowPos = lastUpPos = -1;
		}
	}

	@Override
	public void limitIsMoving(Object source, int n, double currValue) {
		if (source.equals(focuser)) {
			drawFocusLines(n);
			//change the label on the y-axis
			Graphics g = getGraphics();
			if (g == null)
				return;
			String label = StringUtil.doubleToStr(currValue, absMin, absMax);
			FontMetrics fm = g.getFontMetrics();
			int fh = fm.getHeight(), asc = fm.getAscent(), sw = fm.stringWidth(label), sx = x0 - sw - 2;
			if (sx < 0) {
				sx = 0;
			}
			g.setClip(0, 0, x0, y0);
			g.setColor(bkgColor);
			if (n == 0) {
				g.fillRect(0, y0 - fh, x0, fh);
				g.setColor(Color.black);
				g.drawString(label, sx, y0 - fh - 2 + asc);
			} else {
				g.fillRect(0, y0 - height, x0, fh);
				g.setColor(Color.black);
				g.drawString(label, sx, y0 - height + asc + 1);
			}
			setClipToWholeArea(g);
		}
	}

	private int lastLowPos = -1, lastUpPos = -1;

	protected void drawFocusLines(int nMovingLimit) {
		Graphics gr = getGraphics();
		if (gr == null)
			return;
		gr.setColor(Color.magenta);
		gr.setXORMode(plotAreaColor);
		if (nMovingLimit == 0) {
			int lowPos = getScrY(focuser.getCurrMin(), y0, height);
			if (lowPos != lastLowPos) {
				if (lastLowPos >= y0 - height && lastLowPos <= y0) {
					gr.drawLine(x0, lastLowPos, x0 + width, lastLowPos); //erase previous
				}
				lastLowPos = lowPos;
				if (lastLowPos >= y0 - height && lastLowPos <= y0) {
					gr.drawLine(x0, lastLowPos, x0 + width, lastLowPos); //draw new
					gr.drawLine(x0 + width + 1, lastLowPos, focuser.getAxisPosition(), focuser.getMinPos());
				}
			}
		} else {
			int upPos = getScrY(focuser.getCurrMax(), y0, height);
			if (upPos != lastUpPos) {
				if (lastUpPos >= y0 - height && lastUpPos <= y0) {
					gr.drawLine(x0, lastUpPos, x0 + width, lastUpPos); //erase previous
				}
				lastUpPos = upPos;
				if (lastUpPos >= y0 - height && lastUpPos <= y0) {
					gr.drawLine(x0, lastUpPos, x0 + width, lastUpPos); //draw new
					gr.drawLine(x0 + width + 1, lastUpPos, focuser.getAxisPosition(), focuser.getMaxPos());
				}
			}
		}
		gr.setPaintMode();
	}

	protected void eraseFocusLines() {
		Graphics gr = getGraphics();
		if (gr == null)
			return;
		gr.setColor(Color.magenta);
		gr.setXORMode(plotAreaColor);
		if (lastLowPos >= y0 - height && lastLowPos <= y0) {
			gr.drawLine(x0, lastLowPos, x0 + width, lastLowPos); //erase previous
		}
		lastLowPos = -1;
		if (lastUpPos >= y0 - height && lastUpPos <= y0) {
			gr.drawLine(x0, lastUpPos, x0 + width, lastUpPos); //erase previous
		}
		lastUpPos = -1;
		gr.setPaintMode();
	}

	/**
	* The FocusInterval propagates the events of changing the current time moment
	* in the system.
	*/
	public void setFocusInterval(FocusInterval fint) {
		if (this.fint != null) {
			this.fint.removePropertyChangeListener(this);
		}
		this.fint = fint;
		if (fint != null) {
			fint.addPropertyChangeListener(this);
			currTMidx = getTimeMomentIdx(fint.getCurrIntervalEnd());
			drawCurrTimeMomentLine();
		}
	}

	public FocusInterval getFocusInterval() {
		return fint;
	}

	/**
	* Registeres a listener of the time graph being redrawn.
	*/
	@Override
	public synchronized void addPropertyChangeListener(PropertyChangeListener l) {
		if (l == null)
			return;
		if (pcSupport == null) {
			pcSupport = new PropertyChangeSupport(this);
		}
		pcSupport.addPropertyChangeListener(l);
	}

	/**
	* Unregisteres a listener of time graph being redrawn.
	*/
	@Override
	public synchronized void removePropertyChangeListener(PropertyChangeListener l) {
		if (l == null || pcSupport == null)
			return;
		pcSupport.removePropertyChangeListener(l);
	}

	/**
	* The method used to notify all the listeners about the time graph being
	* redrawn. The corresponding property name is "drawn".
	*/
	public void notifyGraphDrawn() {
		if (pcSupport == null)
			return;
		pcSupport.firePropertyChange("drawn", null, null);
	}

	//-------------------- Destroyable interface ---------------------------------
	/**
	* Stops listening of all events, unregisters itself from object event sources
	*/
	@Override
	public void destroy() {
		supervisor.removeHighlightListener(this, table.getEntitySetIdentifier());
		supervisor.removePropertyChangeListener(this);
		supervisor.removeObjectEventSource(this);
		table.removePropertyChangeListener(this);
		if (tf != null) {
			tf.removePropertyChangeListener(this);
		}
		if (fint != null) {
			fint.removePropertyChangeListener(this);
		}
		if (aTrans != null) {
			aTrans.removePropertyChangeListener(this);
		}
		if (tblEvents != null) {
			tblEvents.removePropertyChangeListener(this);
		}
		if (tblEventsObjFilter != null) {
			tblEventsObjFilter.removePropertyChangeListener(this);
		}
		destroyed = true;
	}

	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

	//--------- storing transformed data in the table ------------
	protected Attribute transAttr = null;
	protected int firstColumnIdx = -1;

	public void storeTransformedData() {
		if (dTable == null)
			return;
		if (colNs == null || colNs.size() < 1 || supAttr == null)
			return;
		Panel p = new Panel(new ColumnLayout());
		p.add(new Label("Store the transformed values of the attribute"));
		p.add(new Label(supAttr.getName(), Label.CENTER));
		p.add(new Label("in the table"));
		p.add(new Label(table.getName(), Label.CENTER));
		p.add(new Line(false));
		CheckboxGroup cbg = null;
		Checkbox useOldCB = null, makeNewCB = null;
		String aName = supAttr.getName() + " (transformed)";
		if (transAttr == null) {
			p.add(new Label("Add a new time-dependent attribute to the table"));
		} else {
			cbg = new CheckboxGroup();
			useOldCB = new Checkbox("Modify the values of the previously cleated attribute", true, cbg);
			p.add(useOldCB);
			p.add(new Label(transAttr.getName(), Label.CENTER));
			makeNewCB = new Checkbox("Add a new time-dependent attribute to the table", false, cbg);
			p.add(makeNewCB);
		}
		p.add(new Label("Number of new columns: " + colNs.size()));
		p.add(new Label("Edit the name of the attribute if desired:"));
		TextField tf = new TextField(aName, 60);
		p.add(tf);
		OKDialog okd = new OKDialog(CManager.getAnyFrame(this), "New attribute", true);
		okd.addContent(p);
		okd.show();
		if (okd.wasCancelled())
			return;
		boolean newColumns = transAttr == null || makeNewCB.getState();
		if (newColumns) {
			//create a new time-dependent attribute and add columns to the table
			transAttr = new Attribute(supAttr.getIdentifier() + "_" + table.getAttrCount(), supAttr.getType());
			transAttr.setName(aName);
			String str = tf.getText();
			if (str != null) {
				str = str.trim();
				if (str.length() > 0) {
					transAttr.setName(str);
				}
			}
			firstColumnIdx = table.getAttrCount();
			for (int i = 0; i < colNs.size(); i++) {
				Attribute at1 = table.getAttribute(colNs.elementAt(i));
				Attribute child = new Attribute(at1.getIdentifier() + "_t" + firstColumnIdx, at1.getType());
				for (int j = 0; j < at1.getParameterCount(); j++) {
					child.addParamValPair(at1.getParamValPair(j));
				}
				transAttr.addChild(child);
				table.addAttribute(child);
			}
		}
		//put the data in the columns
		int prec = StringUtil.getPreferredPrecision(absMin, absMin, absMax);
		for (int i = 0; i < lines.size(); i++) {
			TimeLine line = (TimeLine) lines.elementAt(i);
			if (!isActive(line.objIdx)) {
				continue;
			}
			DataRecord rec = dTable.getDataRecord(line.objIdx);
			for (int j = idxTFstart; j <= idxTFend; j++) {
				double val = line.getTransformedValue(j);
				if (Double.isNaN(val)) {
					rec.setAttrValue(null, firstColumnIdx + j);
				} else {
					rec.setNumericAttrValue(val, StringUtil.doubleToStr(val, prec), firstColumnIdx + j);
				}
			}
		}
		Vector resultAttrs = new Vector(colNs.size(), 1);
		for (int i = 0; i < colNs.size(); i++) {
			resultAttrs.addElement(table.getAttributeId(firstColumnIdx + i));
		}
		if (newColumns) {
			table.notifyPropertyChange("new_attributes", null, resultAttrs);
		} else {
			table.notifyPropertyChange("values", null, resultAttrs);
		}
	}
}
