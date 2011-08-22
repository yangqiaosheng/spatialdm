package spade.analysis.plot;

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.List;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.classification.BroadcastClassesCP;
import spade.analysis.classification.BroadcastClassesCPinfo;
import spade.analysis.classification.Classifier;
import spade.analysis.classification.TableClassifier;
import spade.analysis.system.Supervisor;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.OKDialog;
import spade.lib.color.CS;
import spade.lib.lang.Language;
import spade.lib.util.DoubleArray;
import spade.lib.util.IntArray;
import spade.lib.util.NumValManager;
import spade.lib.util.StringUtil;
import spade.vis.action.Highlighter;
import spade.vis.action.ObjectEvent;
import spade.vis.action.ObjectEventHandler;
import spade.vis.database.AttrTransform;
import spade.vis.database.Attribute;
import spade.vis.database.DataTable;
import spade.vis.database.TableStat;

/**
* A class that represents values of multiple numeric attribute on a parallel
* coordinates plot.
* The data to represent are taken from an AttributeDataPortion.
*/
public class PCPlot extends Plot implements ItemListener, ActionListener, PropertyChangeListener {
	static ResourceBundle res = Language.getTextResource("spade.analysis.plot.Res");
	/**
	* Used to generate unique identifiers of instances of PCPlot
	*/
	protected static int nInstances = 0;

	public static final int maxNQuantiles = 12;

	protected int fn[] = null; // field numbers

	protected TableStat tStat = null;

	protected AttrTransform attrTransform = null;

	protected TableClassifier tcl = null;

	public static int AlignIndVal = 1, AlignCommonMinMax = 2, AlignSelected = 3, AlignMedAndQ = 4, AlignScaledMedAndQ = 5, AlignMeanStdD = 6, AlignStdD = 7, Align2Classes = 8, AlignICRwithWeights = 9, AlignICRwithoutWeights = 10,
			AlignICCwithWeights = 11, AlignICCwithoutWeights = 12, AlignICLwithWeights = 13, AlignICLwithoutWeights = 14;
	/**
	* Alignment of the PCP
	* --------------------
	* 1 individual values
	* 2 common min and max
	* 3 alignment to selected object(s)
	* 4 median and quartiles
	* 5 scaled median and quartiles
	* 6 mean and std.deviation
	* 7 std. deviation
	* 8 samples of 2 classes
	* 9 criteria with weights
	* 10 criteria without weights
	*/
	protected int AlignmentMode = AlignIndVal;

	public static int normalMode = 0, similarityMode = 1, similarityClassMode = 2, dominantAttrMode = 3, criteriaMode = 4;
	/*
	* special modes of the PCP:
	* 0 normal operations (referenceObjects, referenceObjects1, and referenceObjects2 are equal null)
	* 1 for similarity (referenceObjects!=null)
	* 2 for similarity classification (referenceObjects1,2!=null)
	* 3 for dominant attribute classification
	* 4 for multiple criteria evaluation
	*/
	protected int specialMode = normalMode;

	public void setSpecialMode(int specialMode) {
		this.specialMode = specialMode;
	}

	public int getSpecialMode() {
		return specialMode;
	}

	//protected boolean isHorizontal=false;

	protected Vector referenceObjects = null, // used for SCalc
			referenceObjects1 = null, // used for SCCalc
			referenceObjects2 = null; // ...
	protected float weights[] = null; // used for multiple criteria evaluation
	// last 2 weights have a special meaning: +1 corresponds to the atribute
	// representing the integrated criterion (to be maximized);
	// -1 - order by integrated criterion (to be minimized)

	protected int x1 = -1, x2 = -1, y[] = null;

	/*
	 * Checkboxes in Control Panel that affect the appearance of the PCP
	 */
	protected Checkbox cbLines = null, cbAggrs = null, cbFlows = null, cbShapes = null, cbHreact = null, cbHproduce = null, cbUseAlpha = null, cbQuantiles[] = null;

	public void setIndividualCheckboxes(Checkbox cbLines, Checkbox cbAggrs, Checkbox cbFlows, Checkbox cbShapes, Checkbox cbHreact, Checkbox cbHproduce, Checkbox cbUseAlpha) {
		this.cbLines = cbLines;
		cbLines.addItemListener(this);
		this.cbAggrs = cbAggrs;
		cbAggrs.addItemListener(this);
		this.cbFlows = cbFlows;
		cbFlows.addItemListener(this);
		this.cbShapes = cbShapes;
		cbShapes.addItemListener(this);
		this.cbHreact = cbHreact;
		cbHreact.addItemListener(this);
		this.cbHproduce = cbHproduce;
		cbHproduce.addItemListener(this);
		this.cbUseAlpha = cbUseAlpha;
		cbUseAlpha.addItemListener(this);
	}

	public void setQuantileCheckboxes(Checkbox cbQuantiles[]) {
		this.cbQuantiles = cbQuantiles;
		if (showQuantilesFlags == null || showQuantilesFlags.length != cbQuantiles.length) {
			showQuantilesFlags = new boolean[cbQuantiles.length];
		}
		for (int i = 0; i < cbQuantiles.length; i++) {
			cbQuantiles[i].addItemListener(this);
			showQuantilesFlags[i] = cbQuantiles[i].getState();
		}
		vABSdata = null;
		classFlowsN = cbQuantiles.length;
		calcFFlowPercentiles();
		calcIFlowPercentiles();
	}

	protected boolean classLinesFlags[] = null, classFlowsFlags[] = null, showQuantilesFlags[] = null, bShowQuantilesUsingAlpha = false;
	protected int classFlowsN = 10;
	/*
	 * structure of fPercentiles (absolute values of percentiles) and
	 * iPercentiles (their screen positions):
	 *    fPercentiles[0] - percentiles for the whole table
	 *    1..N - -/-/- for classes 1..N
	 *    (N+1) - for the dummy <remainder> class
	 */
	float fPercentiles[][][] = null;
	int iPercentiles[][][] = null;
	int iClassSizes[] = null;

	/*
	 * Data structures for aggregates-based selection of lines
	 * Each entry contains:
	 *   1) attribute index in table (reflected in <fn>) and
	 *   2) class index (first index in iPercentiles and fPercentiles)
	 *   3) quantile index (last index in iPercentiles and fPercentiles)
	 * All entries with the same attribute are combined using OR,
	 * multiple attributes are combined using AND
	 */
	protected IntArray vABSdata = null;

	protected int alpha = 255; // used for drawing lines

	/**
	* Constructs a Plot. The argument isIndependent shows whether
	* this plot is displayed separately and, hence, should be registered at the
	* supervisor as an event source or it is a part of some larger plot.
	* The variable allowSelection shows whether the plot should listen to
	* mouse events and transform them to object selection events.
	* Supervisor provides access of a plot to the Highlighter (common for
	* all data displays) and in this way links together all displays
	* The argument handler is a reference to the component the plot
	* should send object events to. In a case when the plot is displayed
	* independently, the ObjectEventHandler is the supervisor (the supervisor
	* implements this interface). Otherwise, the handler is the larger plot in
	* which this plot is included as a part.
	* The larger plot should implement the ObjectEventHandler interface.
	*/
	public PCPlot(boolean isIndependent, boolean allowSelection, Supervisor sup, ObjectEventHandler handler) {
		super(isIndependent, allowSelection, sup, handler);
	}

	/**
	* Resets its internal data, including the array of screen objects.
	* Called when records are added to or removed from the table.
	*/
	@Override
	public void reset() {
		setup(fn);
		redraw();
	}

	public void setup(int fn[]) {
		plotImageValid = bkgImageValid = false;
		this.fn = fn;
		if (dataTable == null || fn.length == 0)
			return;
		tStat = new TableStat();
		tStat.setDataTable(dataTable, aTrans);
		if (dots == null || dots.length != dataTable.getDataItemCount()) {
			dots = new LinePlotObject[dataTable.getDataItemCount()];
			for (int i = 0; i < dots.length; i++) {
				dots[i] = new LinePlotObject();
			}
		}
		for (int i = 0; i < dots.length; i++) {
			((LinePlotObject) dots[i]).reset(fn.length);
			dots[i].id = dataTable.getDataItemId(i);
		}
		applyFilter();
		attrTransform = new AttrTransform(dataTable, tStat, fn);
		if (specialMode == criteriaMode) {
			AlignmentMode = AlignICRwithWeights;
			return;
		}
		if (referenceObjects == null && supervisor != null) {
			Highlighter hl = supervisor.getHighlighter(dataTable.getEntitySetIdentifier());
			if (hl != null) {
				referenceObjects = hl.getSelectedObjects();
			}
		}
		if (referenceObjects != null) {
			tStat.ComputeMaxRefValRatio(referenceObjects, fn);
			attrTransform.setRefVal(tStat.getRefVal(), tStat.getMaxRefValRatio());
		}
		if (tStat.getMaxRefValRatio() > 0 && (specialMode == PCPlot.normalMode || specialMode == PCPlot.similarityMode)) {
			AlignmentMode = AlignSelected;
		} else if (referenceObjects1 != null && referenceObjects2 != null) {
			AlignmentMode = Align2Classes;
		} else {
			AlignmentMode = AlignIndVal;
		}
		if (AlignmentMode == Align2Classes) {
			attrTransform.setR(tStat.computeScaleTransformations(referenceObjects1, referenceObjects2, fn));
		}
	}

	public void setup(int fn[], Vector referenceObjects) {
		this.referenceObjects = referenceObjects;
		setup(fn);
	}

	public void setup(int fn[], Vector referenceObjects1, Vector referenceObjects2) {
		this.referenceObjects1 = referenceObjects1;
		this.referenceObjects2 = referenceObjects2;
		setup(fn);
	}

	public void setup(int fn[], float weights[]) {
		this.weights = weights;
		specialMode = criteriaMode;
		setup(fn);
	}

/*
  public void setCanvas (Canvas c) {
    canvas=c;
    if (canvas!=null && selectionEnabled) {
      canvas.addMouseListener(this);
      canvas.addMouseMotionListener(this);
    }
  }
*/

	public void setFn(int fn[]) { // set new field numbers
		this.fn = fn;
		attrTransform.setFn(fn);
		tStat.setDataTable(dataTable, aTrans);
		if (referenceObjects == null && supervisor != null && dataTable != null) {
			Highlighter hl = supervisor.getHighlighter(dataTable.getEntitySetIdentifier());
			if (hl != null) {
				referenceObjects = hl.getSelectedObjects();
			}
		}
		if (referenceObjects != null) {
			tStat.ComputeMaxRefValRatio(referenceObjects, fn);
			attrTransform.setRefVal(tStat.getRefVal(), tStat.getMaxRefValRatio());
		}
		if (AlignmentMode == PCPlot.Align2Classes) {
			attrTransform.setR(tStat.computeScaleTransformations(referenceObjects1, referenceObjects2, fn));
		}
		y = null;
		for (int i = 0; i < dots.length; i++) {
			((LinePlotObject) dots[i]).reset(fn.length);
			dots[i].id = dataTable.getDataItemId(i);
		}
		calcFFlowPercentiles();
		calcIFlowPercentiles();
		Graphics g = canvas.getGraphics();
		draw(g);
		g.dispose();
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		if (ae.getSource() instanceof FNReorder) {
			setFn(((FNReorder) ae.getSource()).getFn());
		}
		if (ae.getSource() instanceof Button) {
			countFlowOccurances();
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		boolean mustRebuild = false;
		if (pce.getSource() instanceof BroadcastClassesCP) {
			BroadcastClassesCPinfo bccpi = (BroadcastClassesCPinfo) pce.getNewValue();
			findClassifier();
			if (pce.getPropertyName().equals("eventShowClassesLines")) {
				classLinesFlags = bccpi.showClasses;
			}
			if (pce.getPropertyName().equals("eventShowClassesFlows")) {
				classFlowsFlags = bccpi.showClasses;
			}
			redraw();
		} else { // other events, e.g. data change, query, etc.
			//System.out.println("* event from "+pce.getSource()+" name="+
			//                   pce.getPropertyName()+" new value="+pce.getNewValue());
			if (pce.getSource().equals(dataTable)) {
				if (pce.getPropertyName().equals("values")) {
					//this case is covered in superclass
				} else {
					mustRebuild = (pce.getPropertyName().equals("data_added") || pce.getPropertyName().equals("data_removed") || pce.getPropertyName().equals("data_updated"));
				}

			} else if (pce.getSource().equals(aTrans)) {
				mustRebuild = pce.getPropertyName().equals("values");
			} else if (pce.getSource().equals(tf)) {
				mustRebuild = !pce.getPropertyName().equals("destroyed");
			}
		}
		super.propertyChange(pce);
		if (mustRebuild) {
			vABSdata = null;
			calcFFlowPercentiles();
			calcIFlowPercentiles();
			redraw();
		}
	}

	public void setWeights(float weights[]) {
		/*
		 * A.O. 2004-07-30 this is the old behavior, where
		 * setWeights redraw the Plot immediately.
		 */
		setWeights(weights, true);
	}

	public void setWeights(float weights[], boolean redraw) {
		this.weights = weights;
		if (weights == null)
			return;
		attrTransform.setWeights(weights);
		if (redraw) {
			Graphics g = canvas.getGraphics();
			draw(g);
			g.dispose();
		}
	}

	@Override
	public boolean reloadAttributeData(Vector v) {
		if (v == null)
			return false;
		boolean changed = false;
		for (int j = 0; j < fn.length && !changed; j++) {
			changed = v.contains(dataTable.getAttributeId(fn[j]));
		}
		if (changed) {
			setFn(this.fn);
			vABSdata = null;
		}
		return false;
	}

	@Override
	protected boolean isPointInPlotArea(int x, int y) {
		return true;
	}

	protected void findClassifier() {
		Classifier cl = null;
		tcl = null;
		if (supervisor != null && supervisor.getObjectColorer() != null && (supervisor.getObjectColorer() instanceof Classifier) && supervisor.getObjectColorer().getEntitySetIdentifier().equals(dataTable.getEntitySetIdentifier())) {
			cl = (Classifier) supervisor.getObjectColorer();
		}
		if (cl != null && (cl instanceof TableClassifier)) {
			tcl = (TableClassifier) cl;
			if (!dataTable.equals(tcl.getTable())) {
				tcl = null;
			}
		}
	}

	protected void drawPrepareDots() {
		for (int i = 0; i < dots.length; i++) {
			for (int j = 0; j < fn.length; j++) {
				double v = Double.NaN;
				if (dataTable.isAttributeNumeric(fn[j]) || dataTable.isAttributeTemporal(fn[j])) {
					v = getNumericAttrValue(fn[j], i);
				} else if (dataTable instanceof DataTable) {
					Attribute attr = ((DataTable) dataTable).getAttribute(fn[j]);
					if (attr.isClassification()) {
						String strv = dataTable.getAttrValueAsString(fn[j], i);
						if (strv == null || strv.length() == 0) {
							continue;
						}
						int nv = attr.getValueN(strv);
						if (nv >= 0) {
							v = nv;
						}
					}
				}
				if (!Double.isNaN(v)) {
					((LinePlotObject) dots[i]).X[j] = mapX(v, j);
					((LinePlotObject) dots[i]).Y[j] = y[j];
				} else {
					((LinePlotObject) dots[i]).X[j] = -1;
					((LinePlotObject) dots[i]).Y[j] = -1;
				}
			}
		}
	}

	// ---------------------------------------------------------------------
	// beginning of the code for the multidimensional classification...
	// to be moved to a separate class
	protected int findCombIdx(int recn, int nint, float fbint[][]) {
		int n = 0;
		for (int i = 0; i < fn.length; i++) {
			double v = getNumericAttrValue(fn[i], recn);
			if (Double.isNaN(v))
				return -1;
			for (int j = 1; j < fbint[i].length; j++)
				if (v <= fbint[i][j]) {
					n += (j - 1) * (int) Math.pow(nint, fn.length - i - 1);
					break;
				}
		}
		return n;
	}

	protected int[] findIntervalNumbers(int idx, int nint) {
		if (idx == -1)
			return null;
		int comb[] = new int[fn.length];
		for (int i = fn.length - 1; i >= 0; i--) {
			int n = idx % nint;
			comb[i] = n;
			idx = (idx - n) / nint;
		}
		return comb;
	}

	protected void countFlowOccurances() {
		int nint = classFlowsN;
		if (nint < 2) {
			classFlowsN = nint = 4;
			calcFFlowPercentiles();
			calcIFlowPercentiles();
			Graphics g = canvas.getGraphics();
			if (g != null) {
				draw(g);
				g.dispose();
			}
		}
		float fbint[][] = fPercentiles[0]; // bounds of intervals
		int ncomb = (int) Math.pow(nint, fn.length);
		Vector ids[] = new Vector[ncomb]; // Vector of identifiers belonging to each combination
		for (int i = 0; i < dots.length; i++) {
			int idx = findCombIdx(i, nint, fbint);
			if (idx == -1) {
				continue;
			}
			if (ids[idx] == null) {
				ids[idx] = new Vector(10, 10);
			}
			ids[idx].addElement(dots[i].id);
		}
		float counts[] = new float[ncomb];
		for (int i = 0; i < counts.length; i++) {
			counts[i] = (ids[i] == null) ? 0f : ids[i].size();
		}
		int order[] = NumValManager.getOrderDecrease(counts);
		int orderidx[] = new int[order.length];
		for (int i = 0; i < order.length; i++) {
			orderidx[order[i] - 1] = i;
		}
		for (int i = 0; i < counts.length; i++) {
			counts[i] = (ids[i] == null) ? 0f : ids[i].size();
		}
		List list = new List(10);
		for (int element : orderidx)
			if (element >= 0 && element < counts.length && counts[element] > 0) {
				int comb[] = findIntervalNumbers(element, nint);
				String str = (comb == null) ? "error" : "";
				if (comb != null) {
					str = String.valueOf(ids[element].size()) + " =>";
					for (int element2 : comb) {
						str += " " + String.valueOf(element2);
					}
				}
				list.add(str);
			}
		OKDialog dlg = new OKDialog(CManager.getAnyFrame(), "...", false);
		dlg.addContent(list);
		dlg.setModal(false);
		dlg.show();
	}

	// end of the code for the multidimensional classification...
	// ---------------------------------------------------------------------

	protected void drawLines(Graphics g) {
		boolean drawn = false;
		if (classLinesFlags != null && tcl != null) {
			if (tcl.getNClasses() > 0 && classLinesFlags.length == tcl.getNClasses() + 1) {
				for (int k = -1; k < tcl.getNClasses(); k++)
					if ((k + 1 < classLinesFlags.length) && classLinesFlags[k + 1]) {
						Color c = (k == -1) ? new Color(160, 160, 160) : tcl.getClassColor(k);
						if (alpha < 255) {
							c = CS.getAlphaColor(c, alpha);
						}
						g.setColor(c);
						for (int i = 0; i < dots.length; i++)
							if (dots[i].isActive && k == tcl.getRecordClass(i)) {
								dots[i].draw(g);
							}
					}
				drawn = true;
			}
		}
		if (!drawn && (cbLines == null || cbLines.getState())) {
			for (int i = 0; i < dots.length; i++)
				if (dots[i].isActive) {
					Color c = getColorForPlotObject(i);
					if (alpha < 255) {
						c = CS.getAlphaColor(c, alpha);
					}
					g.setColor(c);
					dots[i].draw(g);
				}
		}
		drawAllSelectedObjects(g); //selected objects should not be covered by others
	}

	@Override
	protected void drawAllSelectedObjects(Graphics g) {
		if (isHidden || g == null || !selectionEnabled || !hasData())
			return;
		boolean needColorSelected = classLinesFlags != null;
		for (int i = 0; needColorSelected && i < classLinesFlags.length; i++) {
			needColorSelected = !classLinesFlags[i];
		}
		if (!needColorSelected) {
			super.drawAllSelectedObjects(g);
			return;
		}
		if (hasSelected) {
			for (int i = 0; i < dots.length; i++)
				if (dots[i].isSelected && isPointInPlotArea(dots[i].x, dots[i].y))
					if (dots[i].isActive) {
						Color c = supervisor.getColorForDataItem(dataTable.getDataItem(i), dataTable.getEntitySetIdentifier(), dataTable.getContainerIdentifier(), normalColor);
						g.setColor(c);
						dots[i].draw(g);
					}
		}
		if (hasHighlighted) {
			g.setColor(activeColor);
			for (PlotObject dot : dots)
				if (dot.isHighlighted && isPointInPlotArea(dot.x, dot.y))
					if (dot.isActive) {
						dot.draw(g);
					}
		}
	}

	protected void calcFFlowPercentiles() {
		if (fn == null)
			return;
		int ndim = 2 + ((tcl == null) ? 0 : tcl.getNClasses());
		fPercentiles = new float[ndim][][];
		iPercentiles = new int[ndim][][];
		iClassSizes = new int[ndim];
		int N = classFlowsN;
		for (int k = -2; k < ndim - 2; k++) {
			fPercentiles[k + 2] = new float[fn.length][];
			int perc[] = new int[N + 1];
			for (int n = 0; n < perc.length; n++) {
				perc[n] = Math.round(n * 100.0f / N);
			}
			for (int j = 0; j < fn.length; j++) {
				DoubleArray values = new DoubleArray(dots.length, 10);
				for (int i = 0; i < dots.length; i++)
					if (dots[i].isActive && (k == -2 || ((k == ndim - 3 && tcl != null && -1 == tcl.getRecordClass(i))) || (k > -2 && k < ndim - 1 && tcl != null && k + 1 == tcl.getRecordClass(i)))) {
						double v = Float.NaN;
						if (dataTable.isAttributeNumeric(fn[j]) || dataTable.isAttributeTemporal(fn[j])) {
							v = getNumericAttrValue(fn[j], i);
						} else if (dataTable instanceof DataTable) {
							Attribute attr = ((DataTable) dataTable).getAttribute(fn[j]);
							if (attr.isClassification()) {
								String strv = dataTable.getAttrValueAsString(fn[j], i);
								if (strv == null || strv.length() == 0) {
									continue;
								}
								int nv = attr.getValueN(strv);
								if (nv >= 0) {
									v = nv;
								}
							}
						}
						if (!Double.isNaN(v)) {
							values.addElement(v);
						}
					}
				iClassSizes[k + 2] = values.size();
				if (values.size() > 0) {
					double fPerc[] = NumValManager.getPercentiles(values, perc);
					if ((weights != null && weights[j] < 0) || (AlignmentMode == PCPlot.Align2Classes && attrTransform.getScaleOrientation(j) < 0)) { // revert fPerc
						double rfPerc[] = fPerc.clone();
						for (int i = 0; i < rfPerc.length; i++) {
							fPerc[i] = rfPerc[fPerc.length - 1 - i];
						}
					}
					fPercentiles[k + 2][j] = DoubleArray.double2float(fPerc);
				}
			}
		}
	}

	protected void calcIFlowPercentiles() {
		if (fPercentiles == null)
			return;
		int ndim = 2 + ((tcl == null) ? 0 : tcl.getNClasses());
		if (fPercentiles == null || fPercentiles.length != ndim) {
			calcFFlowPercentiles();
		}
		for (int k = -2; k < ndim - 2; k++)
			if (fPercentiles[k + 2] != null) {
				iPercentiles[k + 2] = new int[fn.length][];
				for (int j = 0; j < fn.length; j++)
					if (fPercentiles[k + 2][j] != null) {
						int iPerc[] = new int[fPercentiles[k + 2][j].length];
						for (int i = 0; i < iPerc.length; i++) {
							iPerc[i] = mapX(fPercentiles[k + 2][j][i], j);
						}
						iPercentiles[k + 2][j] = iPerc;
					}
			}
	}

	protected void drawFlows(Graphics g) {
		if (fPercentiles == null) {
			vABSdata = null;
			calcFFlowPercentiles();
			calcIFlowPercentiles();
			if (fPercentiles == null || iPercentiles == null)
				return;
		}
		if (iPercentiles.length > 2 && (classFlowsFlags == null || iPercentiles.length != 1 + classFlowsFlags.length))
			return;
		for (int k = 0; k < iPercentiles.length; k++) {
			if (k == 0 && cbAggrs != null && !cbAggrs.getState()) {
				continue;
			}
			if (k > 0 && (classFlowsFlags == null || tcl == null || !classFlowsFlags[k - 1])) {
				continue;
			}
			int kk = k;
			if (k == 0) {
				kk = 0;
			} else if (k == 1) {
				kk = iPercentiles.length - 1;
			} else {
				kk = k - 1;
			}
			if (iPercentiles[kk] == null || iPercentiles[kk][0] == null) {
				continue;
			}
			if (cbFlows.getState()) {
				for (int np = 1; np < iPercentiles[kk][0].length; np++) {
					int xx[] = new int[2 * fn.length], yy[] = new int[2 * fn.length];
					for (int j = 0; j < fn.length; j++) {
						yy[j] = yy[yy.length - 1 - j] = y[j];
						if (iPercentiles[kk][j] != null) {
							xx[j] = iPercentiles[kk][j][np - 1];
							xx[yy.length - 1 - j] = iPercentiles[kk][j][np];
						}
					}
					Color c = (k < 2) ? new Color(160, 160, 160) : CS.desaturate(tcl.getClassColor(k - 2));
					boolean dark = np == 2 * (np / 2);
					int alpha = (dark) ? 96 : 128;
					Color ca = ((bShowQuantilesUsingAlpha) ? CS.getAlphaColor(c, alpha) : ((dark) ? c : ((k < 2) ? new Color(176, 176, 176) : CS.desaturate(c))));
					if (showQuantilesFlags == null || showQuantilesFlags[np - 1]) {
						g.setColor(ca);
						g.fillPolygon(xx, yy, xx.length);
					}
					g.setColor(c);
					g.drawPolygon(xx, yy, xx.length);
				}
			} else {
				float ratio = 1f;
				if (kk > 0 && iClassSizes[0] > 0 && iClassSizes[kk] > 0) {
					ratio = (1f * iClassSizes[kk]) / iClassSizes[0];
				}
				int dy = (int) (ratio * 0.5f * (y[1] - y[0]));
				for (int np = 1; np < iPercentiles[kk][0].length; np++) {
					//int xx[]=new int[2*fn.length], yy[]=new int[2*fn.length];
					Color c = (k < 2) ? new Color(160, 160, 160) : CS.desaturate(tcl.getClassColor(k - 2));
					boolean dark = np == 2 * (np / 2);
					int alpha = (dark) ? 96 : 128;
					Color ca = ((bShowQuantilesUsingAlpha) ? CS.getAlphaColor(c, alpha) : ((dark) ? c : ((k < 2) ? new Color(176, 176, 176) : CS.desaturate(c))));
					for (int j = 0; j < fn.length; j++) {
						int x0 = iPercentiles[kk][j][np - 1], w = iPercentiles[kk][j][np] - x0, y0 = y[j] - dy, h = 2 * dy;
						if (showQuantilesFlags == null || showQuantilesFlags[np - 1]) {
							g.setColor(ca);
							g.fillOval(x0, y0, w + 1, h + 1);
						}
						g.setColor(c);
						g.drawOval(x0, y0, w, h);
					}
				}
			}
		}
	}

	protected void drawABS(Graphics g) {
		if (vABSdata == null)
			return;
		for (int i = 0; i < vABSdata.size(); i += 3) {
			int fni = vABSdata.elementAt(i), classIdx = vABSdata.elementAt(i + 1), quantileIdx = vABSdata.elementAt(i + 2);
			// find idx in fn
			int fnidx = -1;
			for (int j = 0; j < fn.length && fnidx == -1; j++)
				if (fni == fn[j]) {
					fnidx = j;
				}
			if (fnidx == -1) {
				continue; // something is wrong
			}
			// screen coordinates
			int scry = y[fnidx], scrx1 = mapX(fPercentiles[classIdx][fnidx][quantileIdx], fnidx), scrx2 = mapX(fPercentiles[classIdx][fnidx][quantileIdx + 1], fnidx);
			// draw
			if (i == 0) {
				g.setColor(Color.magenta);
			}
			for (int j = -1; j <= 1; j++) {
				g.drawLine(scrx1, scry + j, scrx2, scry + j);
			}
		}
	}

	public void drawAlignedString(Graphics g, String str, int x, int y, int align) {
		switch (align) {
		case 1: // left
			g.drawString(str, x, y);
			break;
		default:
			int len = g.getFontMetrics().stringWidth(str);
			if (align == 2) {
				g.drawString(str, x - len / 2, y);
			} else {
				g.drawString(str, x - len, y);
			}
		}
	}

	@Override
	public void draw(Graphics g) {
		if (bounds == null)
			return;
		//System.out.println("*     Draw started: "+(new java.util.Date()));
		g.setColor(plotAreaColor);
		g.fillRect(0, 0, bounds.width, bounds.height);
		FontMetrics fm = g.getFontMetrics();
		int fh = fm.getHeight(), asc = fm.getAscent();
		int dy = (bounds.height - 3 * fh - asc) / (fn.length - 1);
		x1 = 10;
		x2 = bounds.width - 20;
		if (y == null) {
			y = new int[fn.length];
		}
		for (int j = 0; j < fn.length; j++) {
			y[j] = asc + fh + dy * j;
		}
		for (int j = 0; j < fn.length; j++) {
			g.setColor(Color.gray);
			g.drawLine(x1 - 1, y[j], x2 + 1, y[j]);
			if (AlignmentMode >= PCPlot.AlignICRwithWeights && weights != null) {
				int XXX[] = new int[4], YYY[] = new int[4];
				int xxx = mapX(tStat.getMax(fn[j]), j);
				XXX[0] = xxx;
				YYY[0] = y[j] - 3;
				XXX[1] = xxx + 3 * ((weights[j] > 0) ? 1 : -1);
				YYY[1] = y[j];
				XXX[2] = xxx;
				YYY[2] = y[j] + 3;
				XXX[3] = XXX[0];
				YYY[3] = YYY[0];
				g.setColor(Color.black);
				g.fillPolygon(XXX, YYY, 4);
				g.drawPolygon(XXX, YYY, 4);
			}
		}

		if (AlignmentMode != PCPlot.Align2Classes && AlignmentMode < PCPlot.AlignICRwithWeights) {
			// NOT two classes, NOT multicriteria evaluation
			g.setColor(Color.black);
			drawAlignedString(g, "min", x1 - 1, y[0] - asc, 1);
			drawAlignedString(g, "max", x2 + 1, y[0] - asc, 3);
		}
		if (AlignmentMode >= PCPlot.AlignICRwithWeights) {
			// multicriteria evaluation
			if (weights == null)
				return;
			g.setColor(Color.black);
			// following String:
			drawAlignedString(g, res.getString("Bad"), x1 - 1, y[0] - asc, 1);
			// following String:
			drawAlignedString(g, res.getString("Good"), x2 + 1, y[0] - asc, 3);
		}
		if (AlignmentMode < 3 || AlignmentMode == PCPlot.AlignMedAndQ) {
			g.setColor(Color.white);
			g.drawLine(x1 - 1, y[0] - 15, x1 - 1, y[y.length - 1] + 15);
			g.drawLine(x2 + 1, y[0] - 15, x2 + 1, y[y.length - 1] + 15);
		}
		if (AlignmentMode == PCPlot.AlignSelected) { // selected objects
			g.setColor(Color.black);
			// following String:
			drawAlignedString(g, res.getString("selected"), (x1 + x2) / 2, y[0] - asc, 2);
			g.setColor(Color.white);
			g.drawLine(Math.round(0.5f * (x1 + x2)), y[0] - 15, Math.round(0.5f * (x1 + x2)), y[y.length - 1] + 15);
		}
		if (AlignmentMode == PCPlot.AlignICCwithWeights || AlignmentMode == PCPlot.AlignICCwithoutWeights) {
			// multicriteria evaluation, central alignment
			g.setColor(Color.black);
			drawAlignedString(g, "0", (x1 + x2) / 2, y[0] - asc, 2);
			g.setColor(Color.white);
			g.drawLine(Math.round(0.5f * (x1 + x2)), y[0] - 15, Math.round(0.5f * (x1 + x2)), y[y.length - 1] + 15);
		}
		if (AlignmentMode == PCPlot.AlignMedAndQ) { // median and quartiles
			int x12 = (x1 + x2) / 2, dx = (x2 - x1) / 4;
			g.setColor(Color.black);
			// following String:
			drawAlignedString(g, res.getString("median"), x12, y[0] - asc, 2);
			drawAlignedString(g, "Q1", x12 - dx, y[0] - asc, 2);
			drawAlignedString(g, "Q2", x12 + dx, y[0] - asc, 2);
			g.setColor(Color.white);
			g.drawLine(x12 - dx, y[0] - 15, x12 - dx, y[y.length - 1] + 15);
			g.drawLine(x12, y[0] - 15, x12, y[y.length - 1] + 15);
			g.drawLine(x12 + dx, y[0] - 15, x12 + dx, y[y.length - 1] + 15);
		}
		if (AlignmentMode == PCPlot.AlignScaledMedAndQ) { // scaled median and quartiles
			int x12 = (x1 + x2) / 2, x01 = x12 - (int) Math.round(0.5f * (x2 - x1) / (1f + tStat.getRatioMQ(fn))), x02 = x12 + (int) Math.round(0.5f * (x2 - x1) / (1f + tStat.getRatioMQ(fn)));
			g.setColor(Color.black);
			drawAlignedString(g, res.getString("median"), x12, y[0] - asc, 2);
			drawAlignedString(g, "Q1", x01, y[0] - asc, 2);
			drawAlignedString(g, "Q2", x02, y[0] - asc, 2);
			g.setColor(Color.white);
			g.drawLine(x01, y[0] - 15, x01, y[y.length - 1] + 15);
			g.drawLine(x12, y[0] - 15, x12, y[y.length - 1] + 15);
			g.drawLine(x02, y[0] - 15, x02, y[y.length - 1] + 15);
		}
		if (AlignmentMode == PCPlot.Align2Classes) { // two classes
			int x12 = (x1 + x2) / 2, x01 = x12 - Math.round(0.5f * (x2 - x1) * attrTransform.getClassBias()), x02 = x12 + Math.round(0.5f * (x2 - x1) * attrTransform.getClassBias());
			g.setColor(Color.white);
			for (int i = -1; i <= +1; i++) {
				g.drawLine(x01 + i, y[0] - 8, x01 + i, y[y.length - 1] + 15);
				g.drawLine(x12 + i, y[0] - 8, x12 + i, y[y.length - 1] + 15);
				g.drawLine(x02 + i, y[0] - 8, x02 + i, y[y.length - 1] + 15);
			}
			g.setColor(Color.black);
			drawAlignedString(g, "0", x12, y[0] - asc, 2);
			String name = "";
			for (int i = 0; i < referenceObjects1.size(); i++) {
				name += ((i == 0) ? "" : "+") + (String) referenceObjects1.elementAt(i);
			}
			drawAlignedString(g, name, x01, y[0] - asc, 2);
			name = "";
			for (int i = 0; i < referenceObjects2.size(); i++) {
				name += ((i == 0) ? "" : "+") + (String) referenceObjects2.elementAt(i);
			}
			drawAlignedString(g, name, x02, y[0] - asc, 2);
			for (int j = 0; j < fn.length; j++) {
				if (attrTransform.getScaleOrientation(j) != 0) {
					int XXX[] = new int[4], YYY[] = new int[4];
					if (attrTransform.getScaleOrientation(j) == 1) {
						int xxx = mapX(tStat.getMax(fn[j]), j) + 3;
						XXX[0] = xxx;
						YYY[0] = y[j] - 5;
						XXX[1] = xxx + 5;
						YYY[1] = y[j];
						XXX[2] = xxx;
						YYY[2] = y[j] + 5;
					} else { // ==-1
						int xxx = mapX(tStat.getMax(fn[j]), j) - 1;
						XXX[0] = xxx;
						YYY[0] = y[j] - 5;
						XXX[1] = xxx - 5;
						YYY[1] = y[j];
						XXX[2] = xxx;
						YYY[2] = y[j] + 5;
					}
					XXX[3] = XXX[0];
					YYY[3] = YYY[0];
					g.fillPolygon(XXX, YYY, 4);
					g.drawPolygon(XXX, YYY, 4);
				}
			}
		}
		if (AlignmentMode == PCPlot.AlignMeanStdD) { // means and std. deviation
			int x12 = (x1 + x2) / 2, dx = (x2 - x1) / 2, x01 = x12 + (int) Math.round(dx / tStat.getRatioMStdD(fn)), x02 = x12 - (int) Math.round(dx / tStat.getRatioMStdD(fn));
			g.setColor(Color.black);
			// following String:
			drawAlignedString(g, res.getString("mean"), x12, y[0] - asc, 2);
			drawAlignedString(g, "+StD", x01, y[0] - asc, 2);
			drawAlignedString(g, "-StD", x02, y[0] - asc, 2);
			g.setColor(Color.white);
			g.drawLine(x12, y[0] - 15, x12, y[y.length - 1] + 15);
			g.drawLine(x01, y[0] - 15, x01, y[y.length - 1] + 15);
			g.drawLine(x02, y[0] - 15, x02, y[y.length - 1] + 15);
		}

		g.setColor(Color.black);
		for (int j = 0; j < fn.length; j++) {
			int xs = mapX(tStat.getMin(fn[j]), j), xf = mapX(tStat.getMax(fn[j]), j);
			g.drawLine(xs - 1, y[j], xf + 1, y[j]);
			g.drawLine(xs - 1, y[j] - 5, xs - 1, y[j] + 5);
			if (specialMode != PCPlot.criteriaMode) {
				g.drawLine(xf + 1, y[j] - 5, xf + 1, y[j] + 5);
			}
		}

		drawPrepareDots();
		//System.out.println("* Before drawFlows: "+(new java.util.Date()));
		calcIFlowPercentiles();
		drawFlows(g);
		//System.out.println("* Before drawLines: "+(new java.util.Date()));
		drawLines(g);
		//System.out.println("*    Draw finished: "+(new java.util.Date()));
		drawABS(g);
		if (annotationSurfacePresent()) {
			getAnnotationSurface().paint(g);
		}
	}

	public int mapX(double v, int n) {
		return x1 + (int) Math.round((x2 - x1) * attrTransform.value(v, n, AlignmentMode));
	}

	@Override
	public int mapX(double v) {
		return 0;
	}

	@Override
	public int mapY(double v) {
		return 0;
	} // bounds.y+my1+height-Math.round((v-min1)/(max1-min1)*height);

	@Override
	public double absX(int x) {
		return 0;
	} // min1+(max1-min1)*(x-mx1-bounds.x)/width;

	@Override
	public double absY(int y) {
		return 0;
	} // min1+(max1-min1)*(height-y+my1+bounds.y)/height;

	// --- Aggregate Based Selection methods - begin
	protected void ABSprocessClick(int fnIdx, int scrx) {
		if (iPercentiles == null || fnIdx >= fn.length)
			return;
		boolean anyClassFound = false;
		for (int i = 0; i < iPercentiles.length; i++) {
			boolean isAggregateDrawn = false;
			// check if this aggregate is currently on screen
			if (i == 0) {
				isAggregateDrawn = cbAggrs != null && cbAggrs.getState();
			} else {
				isAggregateDrawn = classFlowsFlags != null && tcl != null && ((i < iPercentiles.length - 1 && classFlowsFlags[i]) || (i == iPercentiles.length - 1 && classFlowsFlags[0]));
			}
			if (!isAggregateDrawn) {
				continue;
			}
			// process screen position
			if (scrx >= iPercentiles[0][fnIdx][0] && scrx <= iPercentiles[0][fnIdx][iPercentiles[0][fnIdx].length - 1] && iPercentiles[i] != null && iPercentiles[i][fnIdx] != null) {
				for (int inq = 0; inq < iPercentiles[i][fnIdx].length - 1; inq++)
					if (scrx >= iPercentiles[i][fnIdx][inq] && scrx <= iPercentiles[i][fnIdx][inq + 1]) {
						anyClassFound = true;
						ABScheckAttributeIntervalSelection(i, fnIdx, inq);
					}
			}
		}
		if (!anyClassFound) {
			ABSclearAttributeSelection(fnIdx);
		}
		ABSmodifyObjectSelection();
	}

	protected void ABScheckAttributeIntervalSelection(int classIdx, int fnIdx, int quantileIdx) {
		if (vABSdata == null) {
			vABSdata = new IntArray(30, 30);
		}
		if (vABSdata.size() > 0) {
			for (int i = vABSdata.size() / 3; i >= 0; i--)
				if (vABSdata.elementAt(3 * i) == fn[fnIdx])
					if (vABSdata.elementAt(3 * i + 1) == classIdx && vABSdata.elementAt(3 * i + 2) == quantileIdx) {
						vABSdata.removeElementAt(3 * i + 2);
						vABSdata.removeElementAt(3 * i + 1);
						vABSdata.removeElementAt(3 * i);
						return;
					}
		}
		vABSdata.addElement(fn[fnIdx]);
		vABSdata.addElement(classIdx);
		vABSdata.addElement(quantileIdx);
	}

	protected void ABSclearAttributeSelection(int fnIdx) {
		if (vABSdata == null || vABSdata.size() == 0)
			return;
		for (int i = vABSdata.size() / 3; i >= 0; i--)
			if (vABSdata.elementAt(3 * i) == fn[fnIdx]) {
				vABSdata.removeElementAt(3 * i + 2);
				vABSdata.removeElementAt(3 * i + 1);
				vABSdata.removeElementAt(3 * i);
			}
	}

	protected void ABSmodifyObjectSelection() {
		String setId = dataTable.getEntitySetIdentifier();
		if (vABSdata == null || vABSdata.size() == 0) {
			supervisor.getHighlighter(setId).clearSelection(this);
			return;
		}
		boolean selected[] = new boolean[dataTable.getDataItemCount()];
		int nSelected = 0;
		for (int i = 0; i < selected.length; i++) {
			//System.out.println("* try i="+i+", id="+dataTable.getDataItemId(i));
			selected[i] = true;
			for (int fnIdx = 0; fnIdx < fn.length && selected[i]; fnIdx++) {
				double v = getNumericAttrValue(fn[fnIdx], i);
				//System.out.println("  attr="+fn[fnIdx]+", v="+v);
				if (Double.isNaN(v)) {
					selected[i] = false;
					break;
				}
				boolean anyOrCondition = false;
				for (int cond = 0; cond < vABSdata.size() / 3 && !anyOrCondition; cond++)
					if (vABSdata.elementAt(3 * cond) == fn[fnIdx]) { // check the condition
						selected[i] = false;
						int classIdx = vABSdata.elementAt(3 * cond + 1), quantileIdx = vABSdata.elementAt(3 * cond + 2);
						anyOrCondition = v >= fPercentiles[classIdx][fnIdx][quantileIdx] && v <= fPercentiles[classIdx][fnIdx][quantileIdx + 1];
						//System.out.println("  condition="+cond+", result="+anyOrCondition);
					}
				if (anyOrCondition) {
					selected[i] = true;
					//System.out.println(" result="+selected[i]);
				}
			}
			if (selected[i]) {
				nSelected++;
			}
		}
		if (nSelected == 0) {
			supervisor.getHighlighter(setId).clearSelection(this);
		} else {
			Vector vObjIds = new Vector(nSelected, 10);
			for (int i = 0; i < selected.length; i++)
				if (selected[i]) {
					vObjIds.addElement(new String(dataTable.getDataItemId(i)));
				}
			supervisor.getHighlighter(setId).replaceSelectedObjects(this, vObjIds);
		}
	}

	// --- Aggregate Based Selection methods - end

	@Override
	public void selectSetChanged(Object source, String setId, Vector hlObj) {
		if (canvas == null)
			return;
		if (!selectionEnabled || !hasData())
			return;
		if (!StringUtil.sameStrings(setId, dataTable.getEntitySetIdentifier()))
			return;
		if (source != this && vABSdata != null) {
			vABSdata = null;
		}
		super.selectSetChanged(source, setId, hlObj);
		if (specialMode == PCPlot.normalMode) {
			tStat.ComputeMaxRefValRatio(supervisor.getHighlighter(setId).getSelectedObjects(), fn);
			attrTransform.setRefVal(tStat.getRefVal(), tStat.getMaxRefValRatio());
			Graphics g = canvas.getGraphics();
			draw(g);
			g.dispose();
		}
	}

	@Override
	public void highlightSetChanged(Object source, String setId, Vector hlObj) {
		if (!source.equals(this) && cbHreact != null && !cbHreact.getState())
			return;
		Classifier classifier = (Classifier) supervisor.getObjectColorer();
		if (classifier != null && classifier.getNClasses() > 0 && classLinesFlags != null && classLinesFlags.length == classifier.getNClasses() + 1) {
			boolean needRedrawSelected = false;
			hasHighlighted = false;
			Graphics g = canvas.getGraphics();
			for (int i = 0; i < dots.length; i++)
				if (dots[i].isActive) {
					if (dots[i].isHighlighted)
						if (hlObj == null || !StringUtil.isStringInVectorIgnoreCase(dots[i].id, hlObj)) {
							dots[i].isHighlighted = false;
							if (!isHidden && g != null && isPointInPlotArea(dots[i].x, dots[i].y)) {
								int k = classifier.getObjectClass(dots[i].id);
								if ((k + 1 < classLinesFlags.length) && classLinesFlags[k + 1]) {
									g.setColor(getColorForPlotObject(i));
									dots[i].draw(g);
								}
								needRedrawSelected = true;
							}
						} else {
							;
						}
					else if (hlObj != null && StringUtil.isStringInVectorIgnoreCase(dots[i].id, hlObj)) {
						dots[i].isHighlighted = true;
						if (!isHidden && g != null && isPointInPlotArea(dots[i].x, dots[i].y)) {
							g.setColor(activeColor);
							dots[i].draw(g);
						}
					}
					hasHighlighted = hasHighlighted || dots[i].isHighlighted;
				}
			if (!isHidden && needRedrawSelected) {
				//drawAllSelectedObjects(g);
				draw(g);
			}
		} else {
			super.highlightSetChanged(source, setId, hlObj);
		}
	}

	protected int dragX1 = -1, dragY1 = -1, dragX2 = dragX1, dragY2 = dragY1;
	protected boolean dragging = false;

	@Override
	public void mouseDragged(MouseEvent e) {
		if (canvas == null)
			return;
		int x = e.getX(), y = e.getY();
		if (!dragging && !isPointInPlotArea(dragX1, dragY1))
			return;
		dragging = dragging || Math.abs(x - dragX1) > 5 || Math.abs(y - dragY1) > 5;
		if (!dragging)
			return;
		if (x < bounds.x + mx1) {
			x = bounds.x + mx1;
		}
		if (x > bounds.x + mx1 + width) {
			x = bounds.x + mx1 + width;
		}
		if (y < bounds.y + my1) {
			y = bounds.y + my1;
		}
		if (y > bounds.y + my1 + height) {
			y = bounds.y + my1 + height;
		}
		if (x == dragX2 && y == dragY2)
			return;
		drawFrame(dragX1, dragY1, dragX2, dragY2);
		dragX2 = x;
		dragY2 = y;
		drawFrame(dragX1, dragY1, dragX2, dragY2);
	}

	@Override
	public void mousePressed(MouseEvent e) {
		int x = e.getX(), y = e.getY();
		if (isPointInPlotArea(x, y)) {
			dragX1 = dragX2 = x;
			dragY1 = dragY2 = y;
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (dragging) {
			drawFrame(dragX1, dragY1, dragX2, dragY2);
			dragging = false;
			vABSdata = null;
			selectInFrame(dragX1, dragY1, dragX2, dragY2, e);
		} else {
			int idx = -1;
			for (int i = 0; i < y.length && idx == -1; i++)
				if (dragY1 >= y[i] - 1 && dragY1 <= y[i] + 1) {
					idx = i;
				}
			if (idx == -1) {
				vABSdata = null;
				selectObjectAt(dragX1, dragY1, e);
			} else {
				ABSprocessClick(idx, dragX1);
			}
		}
		dragX1 = dragY1 = dragX2 = dragY2 = -1;
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		if (cbHproduce == null || cbHproduce.getState()) {
			super.mouseMoved(e);
		}
	}

	@Override
	protected void selectInFrame(int x0, int y0, int x, int y, MouseEvent sourceME) {
		if (objEvtHandler == null || dataTable == null || dots == null)
			return;
		if (x < x0) {
			int x1 = x0;
			x0 = x;
			x = x1;
		}
		if (y < y0) {
			int y1 = y0;
			y0 = y;
			y = y1;
		}
		Rectangle r = new Rectangle(x0, y0, x - x0 + 1, y - y0 + 1);
		ObjectEvent oevt = new ObjectEvent(this, ObjectEvent.frame, sourceME, dataTable.getEntitySetIdentifier());
		for (PlotObject dot : dots)
			if (dot.isActive && ((LinePlotObject) dot).intersects(x0, y0, x, y)) {
				oevt.addEventAffectedObject(dot.id);
			}
		objEvtHandler.processObjectEvent(oevt);
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() instanceof Choice) {
			Choice c = (Choice) e.getSource();
			String str = c.getSelectedItem();
			/*
			if (str!=null && str.startsWith("alpha"))
			  switch (c.getSelectedIndex()) {
			    case 0: case 1: case 2: case 3:
			      alpha=32*(1+c.getSelectedIndex()); break;
			    case 4:
			      alpha=192; break;
			    default:
			      alpha=255;
			  }
			else { */
			AlignmentMode = ((Choice) e.getSource()).getSelectedIndex() + 1;
			if (specialMode == criteriaMode) {
				AlignmentMode = AlignICRwithWeights + ((Choice) e.getSource()).getSelectedIndex();
			}
			//}
			Graphics g = canvas.getGraphics();
			draw(g);
			g.dispose();
		}
		if (e.getSource() instanceof Checkbox) {
			if (showQuantilesFlags == null || showQuantilesFlags.length != cbQuantiles.length) {
				showQuantilesFlags = new boolean[cbQuantiles.length];
			}
			for (int i = 0; i < cbQuantiles.length; i++) {
				showQuantilesFlags[i] = cbQuantiles[i].getState();
			}
			bShowQuantilesUsingAlpha = cbUseAlpha != null && cbUseAlpha.getState();
			Graphics g = canvas.getGraphics();
			draw(g);
			g.dispose();
		}
	}

	public void setAlignmentMode(int n) {
		AlignmentMode = n + 1;
		Graphics g = canvas.getGraphics();
		draw(g);
		g.dispose();
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

//ID
	/**
	 * Returns custom properties of the tool: String -> String
	 * By default, returns null.
	 */
	@Override
	public Hashtable getProperties() {
		Hashtable prop = new Hashtable();
		prop.put("alignment", String.valueOf(AlignmentMode - 1));
		prop.put("showLines", String.valueOf(cbLines.getState()));
		prop.put("showAggregates", String.valueOf(cbAggrs.getState()));
		prop.put("showAggregatesAsFlows", String.valueOf(cbFlows.getState()));
		prop.put("showAggregatesAsShapes", String.valueOf(cbShapes.getState()));
		prop.put("highlightingReact", String.valueOf(cbHreact.getState()));
		prop.put("highlightingProduce", String.valueOf(cbHproduce.getState()));
		/* <quantilesN> control is not accessible here, therefore we do not store its state
		   and states of the checkboxes - anyway, we can not restore them
		prop.put("quantilesN",String.valueOf(classFlowsN));
		for (int i=0; i<maxNQuantiles; i++) {
		  String str="quantile"+((i<10)?"0":"")+String.valueOf(i);
		  prop.put(str,String.valueOf(cbQuantiles[i].getState()));
		}*/
		return prop;
	}

	/**
	 * After the plot is constructed, it may be requested to setup its individual
	 * properties according to the given list of stored properties.
	 * The base Plot class does nothing in this method.
	 */
	@Override
	public void setProperties(Hashtable properties) {
		super.setProperties(properties);
		try {
			setAlignmentMode(Integer.parseInt((String) properties.get("alignment")));
		} catch (Exception ex) {
		}
		try {
			cbLines.setState(Boolean.valueOf((String) properties.get("showLines")).booleanValue());
		} catch (Exception ex) {
		}
		try {
			cbAggrs.setState(Boolean.valueOf((String) properties.get("showAggregates")).booleanValue());
		} catch (Exception ex) {
		}
		try {
			cbFlows.setState(Boolean.valueOf((String) properties.get("showAggregatesAsFlows")).booleanValue());
		} catch (Exception ex) {
		}
		try {
			cbShapes.setState(Boolean.valueOf((String) properties.get("showAggregatesAsShapes")).booleanValue());
		} catch (Exception ex) {
		}
		try {
			cbHreact.setState(Boolean.valueOf((String) properties.get("highlightingReact")).booleanValue());
		} catch (Exception ex) {
		}
		try {
			cbHproduce.setState(Boolean.valueOf((String) properties.get("highlightingProduce")).booleanValue());
		} catch (Exception ex) {
		}
	}

//~ID

	/**
	* Used to generate unique identifiers of instances of Plot's descendants.
	* The base class Plot calls this method in the constructor.
	*/
	@Override
	protected void countInstance() {
		instanceN = ++nInstances;
	}

	/**
	* Returns "Parallel_Coordinates".
	* Used to generate unique identifiers of instances of Plot's descendants.
	* Plot uses this method in the method getIdentifier().
	*/
	@Override
	public String getPlotTypeName() {
		return "Parallel_Coordinates";
	}

}
