package spade.analysis.classification;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
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

import spade.analysis.manipulation.DichromaticSlider;
import spade.analysis.manipulation.Slider;
import spade.analysis.manipulation.SliderListener;
import spade.analysis.plot.CumulativeHistogram;
import spade.analysis.system.Supervisor;
import spade.analysis.transform.AttributeTransformer;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.FoldablePanel;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.PercentBar;
import spade.lib.basicwin.PlotCanvas;
import spade.lib.color.Rainbow;
import spade.lib.lang.Language;
import spade.lib.util.DoubleArray;
import spade.lib.util.FloatArray;
import spade.lib.util.IntArray;
import spade.lib.util.NumRange;
import spade.lib.util.StringUtil;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.AttributeTypes;
import spade.vis.database.ObjectFilter;
import ui.AttributeChooser;

/**
* A part of NumAttr1ClassManipulator: an assembly of UI elements that supports
* interactive classification of objects on the basis of a single numeric
* attribute by breaking its value range into intervals.
* A NumAttr1ClassManipulator, besides a NumericClassifierControlPanel,
* contains a control for moving class breaks (slider). A manipulator may have
* different layouts: the slider may be placed horisontally above the
* control panel or vertically on the left of the control panel.
*/

public class NumericClassifierControlPanel extends Panel implements ActionListener, ItemListener, SliderListener, PropertyChangeListener, Destroyable {
	static ResourceBundle res = Language.getTextResource("spade.analysis.classification.Res");
	public static final int maxNIntervals = 15;
	private Label lQual = null, lComp = null, lMin = null, lMax = null;
	private Choice chErrorsBy = null, chDelAttr = null;
	protected boolean mayUseEntropy = false;
	private TextField tfVals = null;
	private Button bAutoClass = null;
	/**
	* The slider is used for specifying class breaks
	*/
	private Slider slider = null;
	/**
	* The dichromatic slider is used for specifying class colors
	*/
	private DichromaticSlider dSlider = null;
	protected AttributeDataPortion dTable = null;
	protected ObjectFilter tFilter = null;
	protected AutoNumClassifier anc = null;
	/*
	* errors of optimal classifications
	*/
	protected double maxErrors[][] = null;
	protected float absError = Float.NaN, relError = Float.NaN;
	private PercentBar pbError = null, pbQuality = null;

	// Parameters of the last automatic classification applied
	public static final int EqualSizeClasses = 0, EqualIntervals = 1, NestedMeans = 2, OptimalClass = 3;
	/**
	* The last aplied method of automatic classification.
	*/
	private int autoClassMethod = -1;
	/**
	* The last selected desired number of classes in automatic classification.
	*/
	private int autoClassNumber = 5;
	/**
	* The last actual number of classes in automatic classification.
	*/
	private int autoClassCount = 5;
	/**
	* The last selected desired statistical error in automatic classification.
	*/
	private float autoClassError = 50f;
	/**
	* Indicates whether to reaply the last method of automatic classification
	* after changes in data.
	*/
	private boolean reapplyAutoClass = false;
	/**
	* Indicates whether the class breaks passed to the method breaksChanged(...)
	* originate from some automatic classification method.
	*/
	private boolean automaticBreaks = false;

	private boolean autoClassApplyToQueryResults = false;

	/**
	* Indicates "destroyed" state. Initially is false.
	*/
	protected boolean destroyed = false;

	/**
	* The number of the attribute on the basis of which classification is done
	*/
	protected int attrN = -1;

	protected Supervisor sup = null;

	protected NumAttr1Classifier classifier = null;

	protected ClassificationStatisticsCanvas csc = null;

	protected CumulativeHistogram cc = null;
	/**
	* Indicate whether the corresponding components are in an "open" or "closed"
	* states
	*/
	protected boolean qualityPanActive = false, statPanActive = false, cumCurveActive = false;

	public NumericClassifierControlPanel(AttributeDataPortion dTable, int attrN, Slider slider, DichromaticSlider dSlider, Supervisor sup, NumAttr1Classifier classifier) {
		super();
		this.dTable = dTable;
		this.attrN = attrN;
		this.slider = slider;
		this.dSlider = dSlider;
		this.sup = sup;
		this.classifier = classifier;
		classifier.addPropertyChangeListener(this);
		AttributeTransformer aTrans = classifier.getAttributeTransformer();
		if (aTrans != null) {
			aTrans.addPropertyChangeListener(this);
		} else {
			dTable.addPropertyChangeListener(this);
		}
		tFilter = dTable.getObjectFilter();
		if (tFilter != null) {
			tFilter.addPropertyChangeListener(this);
		}
		anc = new AutoNumClassifier(dTable, attrN);
		anc.setAttributeTransformer(classifier.getAttributeTransformer());
		autoClassCount = 0;

		slider.setMaxNBreaks(100);
		slider.setUseShades(false);
		setSliderMinMaxIfNeeded();
		slider.addSliderListener(this);

		lMin = new Label(classifier.getValueAsString(slider.getMin()), Label.LEFT);
		tfVals = new TextField("");
		slider.setTextField(tfVals);
		lMax = new Label(classifier.getValueAsString(slider.getMax()), Label.RIGHT);
		mayUseEntropy = slider.getMin() >= 0f;

		slider.setMiddleColor(Color.yellow);
		if (dTable.getAttributeOrigin(attrN) == AttributeTypes.evaluate_rank) {
			slider.setPositiveHue(-1000.0f);
			//slider.setPositiveHue(0.0f);
			slider.setNegativeHue(0.33f);
		} else {
			slider.setPositiveHue(-1101.0f);
			//slider.setPositiveHue(0.33f);
			slider.setNegativeHue(0.0f);
		}

		float breaks[] = makeBreaks();

		if (breaks != null) {
			slider.setMidPoint((breaks[breaks.length / 2 - 1] + breaks[breaks.length / 2]) / 2);
			autoClassCount = breaks.length - 1;
			for (int i = 1; i < breaks.length - 1; i++) {
				slider.addBreak(breaks[i]);
			}
		}
		adjustToClassifierProperties();

		cc = new CumulativeHistogram(sup);
		cc.setDataSource(dTable);
		if (aTrans != null) {
			cc.setAttributeTransformer(aTrans, false);
		}
		int fn[] = new int[1];
		fn[0] = attrN;
		cc.setup(fn);
		slider.addSliderListener(cc);

		makeUI();

		classifier.setPositiveHue(slider.getPositiveHue());
		classifier.setNegativeHue(slider.getNegativeHue());
		classifier.setMiddleColor(slider.getMiddleColor());
		slider.notifyBreaksChange();
	}

	protected float[] makeBreaks() {
		float breaks[] = getBreaksFromClassifier();
		if (breaks != null)
			return breaks;

		if (dTable.getAttributeOrigin(attrN) != AttributeTypes.evaluate_rank && dTable.getAttributeOrigin(attrN) != AttributeTypes.evaluate_score) {
			autoClassMethod = EqualSizeClasses;
			autoClassNumber = 5;
			breaks = anc.doEqualClasses(5);
			if (breaks != null)
				return breaks;
		}
		//if this is ranking or evaluation score, divide by default into 3
		//classes and make green-yellow-red color scale
		autoClassMethod = EqualIntervals;
		autoClassNumber = 3;
		return anc.doEqualIntervals(3);
	}

	protected float[] getBreaksFromClassifier() {
		if (classifier == null)
			return null;
		FloatArray origBreaks = classifier.getBreaks();
		if (origBreaks == null)
			return null;
		FloatArray newBreaks = new FloatArray(origBreaks.size() + 2, 1);
		if (!Double.isNaN(classifier.minVal)) {
			newBreaks.addElement((float) classifier.minVal);
		} else {
			newBreaks.addElement((float) slider.getMin());
		}
		for (int i = 0; i < origBreaks.size(); i++) {
			newBreaks.addElement(origBreaks.elementAt(i));
		}
		if (!Double.isNaN(classifier.maxVal)) {
			newBreaks.addElement((float) classifier.maxVal);
		} else {
			newBreaks.addElement((float) slider.getMax());
		}
		return newBreaks.getTrimmedArray();
	}

	public void adjustToClassifierProperties() {
		if (classifier == null)
			return;

		slider.setNegativeHue(classifier.getNegativeHue());
		slider.setPositiveHue(classifier.getPositiveHue());
		slider.setMiddleColor(classifier.getMiddleColor());

		float breaks[] = getBreaksFromClassifier();
		if (breaks != null) {
			slider.removeAllBreaks();
			for (int i = 1; i < breaks.length - 1; i++) {
				slider.addBreak(breaks[i]);
			}
		}

		double middle = classifier.getMiddleValue();
		if (!Double.isNaN(middle)) {
			slider.setMidPoint(middle);
		}
		slider.notifyBreaksChange();
	}

	/**
	* Returns the slider used for break specification
	*/
	public Slider getBreakSlider() {
		return slider;
	}

	protected void showMessage(String txt, boolean trouble) {
		if (sup != null && sup.getUI() != null) {
			sup.getUI().showMessage(txt, trouble);
		}
	}

	public void makeUI() {
		setLayout(new BorderLayout());

		Panel p = new Panel();
		p.setLayout(new ColumnLayout());
		Panel pp = new Panel();
		pp.setLayout(new BorderLayout());
		pp.add(lMin, "West");
		pp.add(tfVals, "Center");
		pp.add(lMax, "East");
		p.add(pp);
		p.setBackground(new Color(223, 223, 223));

		pp = new Panel();
		pp.setBackground(Color.white);
		bAutoClass = new Button("Automatic classification");
		bAutoClass.addActionListener(this);
		bAutoClass.setActionCommand("AutoClass");
		pp.add(bAutoClass);
		p.add(pp);

		Panel fpi = new Panel();
		fpi.setLayout(new ColumnLayout());
		// following text: "Quality of the classification = 100%"
		fpi.add(lQual = new Label(res.getString("Quality_of_the"), Label.CENTER));
		fpi.add(pbError = new PercentBar(true));
		pbError.setValue(100f);
		// following text: "Quality Vs. best for 0 classes= 0 %"
		fpi.add(lComp = new Label(res.getString("Quality_Vs_best_for_0"), Label.CENTER));
		fpi.add(pbQuality = new PercentBar(true));
		pbQuality.setValue(0f);
		pp = new Panel();
		pp.setLayout(new BorderLayout());
		// following text:"Errors computed by"
		pp.add(new Label(res.getString("Errors_computed_by"), Label.CENTER), "West");
		pp.add(chErrorsBy = new Choice(), "East");
		fpi.add(pp);
		chErrorsBy.addItemListener(this);
		// following text:"mean"
		chErrorsBy.add(res.getString("mean"));
		// following text:"median"
		chErrorsBy.add(res.getString("median"));
		// following text:"entropy"
		if (mayUseEntropy) {
			chErrorsBy.add(res.getString("entropy"));
		}
		// following text:"Statistical quality"
		FoldablePanel fp = new FoldablePanel(fpi, new Label(res.getString("Statistical_quality")));
		fp.setName("statistical_quality");
		fp.addActionListener(this);
		fp.setBackground(new Color(223, 223, 223));
		p.add(fp);

		//updateFields();

		fpi = new Panel();
		fpi.setLayout(new BorderLayout());
		csc = new ClassificationStatisticsCanvas(classifier);
		fpi.add(csc, "Center");
		// following text:"Classification statistics"
		fp = new FoldablePanel(fpi, new Label(res.getString("Classification")));
		fp.setName("class_statistics");
		fp.addActionListener(this);
		fp.setBackground(Color.white);
		p.add(fp);

		fpi = new Panel();
		fpi.setLayout(new BorderLayout());
		PlotCanvas canvas_cc = new PlotCanvas();
		cc.setCanvas(canvas_cc);
		canvas_cc.setContent(cc);
		fpi.add(canvas_cc, "Center");

		pp = new Panel();
		pp.setLayout(new ColumnLayout());
		Button b = new Button(res.getString("Add_quantitative"));
		b.setActionCommand("add");
		b.addActionListener(this);
		pp.add(b);
		pp.add(chDelAttr = new Choice());
		fpi.add(pp, "South");
		chDelAttr.addItemListener(this);
		fillDelAttr();
		// following text:"Cumulative curve"
		fp = new FoldablePanel(fpi, new Label(res.getString("Cumulative_curve")));
		fp.setName("cumulative_curve");
		fp.addActionListener(this);
		fp.setBackground(new Color(223, 223, 223));
		p.add(fp);

		add(p, "Center");

		// to receive messages about color change
		// and to force cc to repaint - the last is not destroyable...
		sup.addPropertyChangeListener(this);
	}

	protected int getCurrentMetrics() {
		if (chErrorsBy != null) {
			switch (chErrorsBy.getSelectedIndex()) {
			case 0:
				return OptNumClass.SUBMETHOD_MEAN;
			case 1:
				return OptNumClass.SUBMETHOD_MEDIAN;
			default:
				return OptNumClass.SUBMETHOD_INFO;
			}
		}
		return OptNumClass.SUBMETHOD_MEAN;
	}

	@Override
	public void destroy() {
		if (dTable != null) {
			dTable.removePropertyChangeListener(this);
		}
		if (tFilter != null) {
			tFilter.removePropertyChangeListener(this);
		}
		if (cc != null && sup != null) {
			sup.removePropertyChangeListener(this);
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

	protected boolean setSliderMinMaxIfNeeded() {
		if (classifier.isAttrTemporal())
			return false;
		AttributeTransformer aTrans = classifier.getAttributeTransformer();
		String aid = classifier.getAttrId();
		NumRange nr = (aTrans != null) ? aTrans.getAttrValueRange(aid) : dTable.getAttrValueRange(aid);
		if (nr == null || Double.isNaN(nr.minValue))
			return false;
		if (Double.isNaN(slider.getMin()) || slider.getMin() != nr.minValue || Double.isNaN(slider.getMax()) || slider.getMax() != nr.maxValue) {
			slider.setMinMax(nr.minValue, nr.maxValue);
			return true;
		}
		return false;
	}

	protected void automaticClassification() {
		// following text: "Automatic classification"
		OKDialog dlg = new OKDialog(CManager.getAnyFrame(), res.getString("Automatic") + " - 1", true);
		CheckboxGroup cbg = new CheckboxGroup();
		Checkbox cbm[] = new Checkbox[4], cbf[] = null;
		if (autoClassMethod < 0) {
			autoClassMethod = EqualSizeClasses;
		}
		// following text: "Equal size classes"
		cbm[0] = new Checkbox(res.getString("Equal_size_classes"), autoClassMethod == EqualSizeClasses, cbg);
		// following text: "Equal interval"
		cbm[1] = new Checkbox(res.getString("Equal_interval"), autoClassMethod == EqualIntervals, cbg);
		// following text: "Nested means"
		cbm[2] = new Checkbox(res.getString("Nested_means"), autoClassMethod == NestedMeans, cbg);
		// following text:"Optimal classification"
		cbm[3] = new Checkbox(res.getString("Optimal"), autoClassMethod == OptimalClass, cbg);
		if (tFilter.areObjectsFiltered()) {
			cbg = new CheckboxGroup();
			cbf = new Checkbox[2];
			// following text: "all objects"
			cbf[0] = new Checkbox(res.getString("all_objects"), !autoClassApplyToQueryResults, cbg);
			// following text: "query results"
			cbf[1] = new Checkbox(res.getString("query_results"), autoClassApplyToQueryResults, cbg);
		}
		Checkbox reapplyCB = new Checkbox(res.getString("reapply"), reapplyAutoClass);
		Panel p = new Panel();
		p.setLayout(new ColumnLayout());
		p.add(new Label(res.getString("Automatic") + " - 1"));
		p.add(new Line(false));
		// following text:"Method"
		p.add(new Label(res.getString("Method"), Label.CENTER));
		for (Checkbox element : cbm) {
			p.add(element);
		}
		if (cbf != null) {
			// following text: "Apply to"
			p.add(new Label(res.getString("Apply_to"), Label.CENTER));
			for (Checkbox element : cbf) {
				p.add(element);
			}
		}
		p.add(new Line(false));
		p.add(reapplyCB);
		dlg.addContent(p);
		dlg.show();
		if (dlg.wasCancelled())
			return;
		for (int i = 0; i < cbm.length; i++)
			if (cbm[i].getState()) {
				autoClassMethod = i;
				break;
			}
		if (cbf != null) {
			autoClassApplyToQueryResults = cbf[1].getState();
		}
		reapplyAutoClass = reapplyCB.getState();
		if (autoClassMethod != NestedMeans) {
			// following text: "Automatic classification"
			dlg = new OKDialog(CManager.getAnyFrame(), res.getString("Automatic") + " - 2", true);
			cbg = new CheckboxGroup();
			Checkbox cb[] = new Checkbox[maxNIntervals], cbmetrics[] = null;
			TextField tf = null;
			for (int i = 0; i < cb.length - 1; i++) {
				cb[i] = new Checkbox(String.valueOf(2 + i), autoClassNumber == 2 + i, cbg);
			}
			if (autoClassMethod == OptimalClass) {
				// following text:"Max error="
				cb[cb.length - 1] = new Checkbox(res.getString("Max_error_"), false, cbg);
				tf = new TextField((Float.isNaN(absError)) ? "50.0" : spade.lib.util.StringUtil.floatToStr(absError, 1), 4);
				cbg = new CheckboxGroup();
				cbmetrics = (mayUseEntropy) ? new Checkbox[3] : new Checkbox[2];
				// following text:"mean"
				cbmetrics[0] = new Checkbox(res.getString("mean"), 0 == chErrorsBy.getSelectedIndex(), cbg);
				// following text:"median"
				cbmetrics[1] = new Checkbox(res.getString("median"), 1 == chErrorsBy.getSelectedIndex(), cbg);
				// following text:"entropy"
				if (mayUseEntropy) {
					cbmetrics[2] = new Checkbox(res.getString("entropy"), 2 == chErrorsBy.getSelectedIndex(), cbg);
				}
			} else {
				cb[cb.length - 1] = null;
			}
			p = new Panel();
			p.setLayout(new ColumnLayout());
			switch (autoClassMethod) {
			case EqualSizeClasses:
				p.add(new Label(res.getString("Equal_size_classes")));
				break;
			case EqualIntervals:
				p.add(new Label(res.getString("Equal_interval")));
				break;
			case OptimalClass:
				p.add(new Label(res.getString("Optimal")));
				break;
			}
			p.add(new Line(false));
			// following text:"N of classes="
			p.add(new Label(res.getString("Nof_classes_"), Label.CENTER));
			Panel subp = new Panel();
			subp.setLayout(new FlowLayout());
			for (int i = 0; i < cb.length - 1; i++) {
				subp.add(cb[i]);
			}
			p.add(subp);
			if (cb[cb.length - 1] != null) {
				subp = new Panel();
				subp.setLayout(new FlowLayout());
				subp.add(cb[cb.length - 1]);
				subp.add(tf);
				subp.add(new Label("%"));
				p.add(subp);
				p.add(new Line(false));
				p.add(new Label(res.getString("Errors_computed_by"), Label.CENTER));
				subp = new Panel();
				subp.setLayout(new FlowLayout());
				for (Checkbox cbmetric : cbmetrics) {
					subp.add(cbmetric);
				}
				p.add(subp);
				p.add(new Line(false));
			}
			dlg.addContent(p);
			dlg.show();
			if (dlg.wasCancelled())
				return;
			if (cbmetrics != null) {
				for (int i = 0; i < cbmetrics.length; i++)
					if (cbmetrics[i].getState()) {
						chErrorsBy.select(i);
						break;
					}
			}
			autoClassNumber = -1;
			for (int i = 0; i < cb.length - 1; i++)
				if (cb[i].getState()) {
					autoClassNumber = i + 2;
					break;
				}
			if (autoClassMethod != EqualIntervals && autoClassNumber > dTable.getDataItemCount()) {
				autoClassNumber = dTable.getDataItemCount();
			}
			if (autoClassNumber == -1) {
				String str = tf.getText();
				float threshold = Float.NaN;
				try {
					threshold = Float.valueOf(str).floatValue();
				} catch (NumberFormatException nfe) {
					autoClassNumber = slider.getNBreaks() + 1;
					updateFields();
					return;
				}
				if (threshold <= 0 || threshold >= 100) {
					autoClassNumber = slider.getNBreaks() + 1;
					updateFields();
					return;
				}
				autoClassError = threshold;
			}
		}
		applyAutoClass();
	}

	protected void applyAutoClass() {
		if (autoClassMethod < 0) {
			autoClassMethod = EqualSizeClasses;
		}
		if (anc == null) {
			anc = new AutoNumClassifier(dTable, attrN);
			anc.setAttributeTransformer(classifier.getAttributeTransformer());
		}
		anc.setUseFilter(autoClassApplyToQueryResults);
		float breaks[] = null;
		if (autoClassNumber < 2 && (autoClassMethod == EqualSizeClasses || autoClassMethod == EqualIntervals) || (autoClassMethod == OptimalClass && autoClassNumber != -1 && autoClassError <= 0)) {
			autoClassNumber = slider.getNBreaks() + 1;
		}
		if (autoClassNumber < 2) {
			autoClassNumber = 2;
		}
		switch (autoClassMethod) {
		case EqualSizeClasses:
			breaks = anc.doEqualClasses(autoClassNumber);
			break;
		case EqualIntervals:
			if (classifier.hasSubAttributes()) {
				breaks = AutoNumClassifier.doEqualIntervals(autoClassNumber, classifier.getMinVal(), classifier.getMaxVal());
			} else {
				breaks = anc.doEqualIntervals(autoClassNumber);
			}
			break;
		case NestedMeans:
			breaks = anc.doNestedMeans();
			break;
		case OptimalClass:
			int subMethod = getCurrentMetrics();
			Cursor oldCursor = getCursor();
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			// following text: "Wait: heavy computations!"
			showMessage(res.getString("Wait_heavy"), false);
			if (autoClassNumber != -1) {
				breaks = anc.doOptimalClassification(autoClassNumber, subMethod);
			} else {
				float threshold = autoClassError;
				if (maxErrors == null || maxErrors[subMethod - OptNumClass.SUBMETHOD_MEAN] == null) {
					getMaxErrors();
				}
				if (maxErrors != null) {
					for (int i = 2; i <= 9; i++) {
						breaks = anc.doOptimalClassification(i, subMethod);
						double error = anc.estimateError(breaks, subMethod);
						showMessage(null, false);
						float err = (float) (100 * error / maxErrors[subMethod - OptNumClass.SUBMETHOD_MEAN][0]);
						if (err <= threshold) {
							break;
						}
					}
				}
			}
			showMessage(null, false);
			setCursor(oldCursor);
			break;
		}
		if (breaks == null)
			return;
		/*
		System.out.println("* number of breaks="+breaks.length);
		for (int i=0; i<breaks.length; i++) System.out.println("* i="+i+", break[i]="+breaks[i]);
		*/
		slider.removeAllBreaks();
		for (int i = 1; i < breaks.length - 1; i++) {
			slider.addBreak(breaks[i]);
		}
		if (slider.getNBreaks() == 0) {
			slider.addBreak((slider.getMax() + slider.getMin()) / 2);
		}
		if (breaks.length > 2) {
			int n = (breaks.length - 1) / 2;
			dSlider.setMidPoint((breaks[n] + breaks[n + 1]) / 2);
			dSlider.redraw();
		}
		autoClassCount = autoClassNumber = slider.getNBreaks() + 1;
		slider.redraw();
		automaticBreaks = true;
		slider.notifyBreaksChange();
		csc.repaint();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof Rainbow) {
			classifier.startChangeColors();
			return;
		}
		String command = e.getActionCommand();
		if (command.endsWith("AutoClass")) {
			automaticClassification();
		} else if (command.equals("add")) { //add attributes
			int oldFn[] = cc.getFn();
			int n = oldFn.length;
			Vector excludeAttrIds = new Vector(n, 10);
			for (int i = 0; i < n; i++) {
				excludeAttrIds.addElement(dTable.getAttributeId(oldFn[i]));
			}
			AttributeChooser attrSel = new AttributeChooser();
			Vector colIds = null;
			if (attrSel.selectColumns(dTable, null, excludeAttrIds, true, res.getString("Select_one_or_more"), null) != null) {
				colIds = attrSel.getSelectedColumnIds();
			}
			if (colIds == null || colIds.size() < 1)
				return;
			IntArray colNs = new IntArray(colIds.size(), 1);
			for (int i = 0; i < colIds.size(); i++) {
				int cn = dTable.getAttrIndex((String) colIds.elementAt(i));
				if (cn < 0) {
					continue;
				}
				boolean found = false;
				for (int j = 0; j < n && !found; j++) {
					found = oldFn[j] == cn;
				}
				if (!found) {
					colNs.addElement(cn);
				}
			}
			if (colNs.size() < 1)
				return;
			int newFn[] = new int[n + colNs.size()];
			for (int i = 0; i < n; i++) {
				newFn[i] = oldFn[i];
			}
			for (int i = 0; i < colNs.size(); i++) {
				newFn[n + i] = colNs.elementAt(i);
			}
			cc.setFn(newFn);
			fillDelAttr();
			CManager.validateAll(cc.getCanvas());
			return;
		} else if (e.getSource() instanceof FoldablePanel) {
			String name = ((FoldablePanel) e.getSource()).getName();
			if (name == null)
				return;
			boolean open = command.equals("open");
			if (name.equals("statistical_quality")) {
				qualityPanActive = open;
				if (open) {
					updateFields();
				}
			} else if (name.equals("class_statistics")) {
				statPanActive = open;
				if (open) {
					csc.repaint();
				}
			} else if (name.equals("cumulative_curve")) {
				cumCurveActive = open;
				if (open) {
					fillDelAttr();
				}
			}
		}
	}

	@Override
	public void itemStateChanged(ItemEvent ie) {
		/*
		if (ie.getSource().equals(chAddAttr)) {
		  if (chAddAttr==null || chAddAttr.getSelectedIndex()==0) return;
		  int oldFn[]=cc.getFn();
		  int newFn[]=new int[oldFn.length+1];
		  for (int i=0; i<oldFn.length; i++) newFn[i]=oldFn[i];
		  newFn[newFn.length-1]=chAddAttrN[chAddAttr.getSelectedIndex()-1];
		  cc.setFn(newFn);
		  fillAddAttr();
		  fillDelAttr();
		  CManager.validateAll(cc.getCanvas());
		  return;
		}
		*/
		if (ie.getSource().equals(chDelAttr)) {
			if (chDelAttr == null || chDelAttr.getSelectedIndex() == 0)
				return;
			int oldFn[] = cc.getFn();
			int newFn[] = new int[oldFn.length - 1];
			int j = -1;
			for (int i = 0; i < oldFn.length; i++)
				if (i < chDelAttr.getSelectedIndex()) {
					newFn[i] = oldFn[i];
				} else if (i > chDelAttr.getSelectedIndex()) {
					newFn[i - 1] = oldFn[i];
				}
			cc.setFn(newFn);
			fillDelAttr();
			CManager.validateAll(cc.getCanvas());
			return;
		}
		updateFields();
	}

	protected int[] chAddAttrN = null;

	public void fillDelAttr() {
		if (!chDelAttr.isShowing())
			return;
		chDelAttr.setVisible(false);
		chDelAttr.removeAll();
		// following text:"Remove attribute"
		chDelAttr.add(res.getString("Remove_attribute"));
		int fn[] = cc.getFn();
		for (int j = 1; j < fn.length; j++) {
			chDelAttr.add(dTable.getAttributeName(fn[j]));
		}
		chDelAttr.setEnabled(chDelAttr.getItemCount() > 1);
		chDelAttr.setVisible(true);
	}

	private IntArray changingClasses = new IntArray(2, 2);

	/*
	* Results of the classification. Min and Max values are not listed in <breaks>.
	* If there are no breaks, breaks==null.
	* Note that breaks.length is not always equal to the real number of breaks!
	* Use nBreaks!
	*/
	@Override
	public void breaksChanged(Object source, double[] breaks, int nBreaks) {
		if (!automaticBreaks) {
			reapplyAutoClass = false;
		}
		automaticBreaks = false;
		slider.exposeAllClasses();
		slider.redraw();
		classifier.exposeAllClasses();
		classifier.setBreaks(DoubleArray.double2float(breaks), nBreaks);
		for (int i = 0; i < classifier.getNClasses(); i++) {
			classifier.setClassColor(slider.getColor(i), i);
		}
		classifier.notifyClassesChange();
		changingClasses.removeAllElements();
		updateFields();
	}

	/*
	* This function is called during the process of moving a delimiter
	* between classes. <n> shows number of the delimiter (n==0 corresponds
	* to a delimiter between 0th and 1st classes)
	*/
	@Override
	public void breakIsMoving(Object source, int n, double currValue) {
		reapplyAutoClass = false;
		automaticBreaks = false;
		if (classifier.getHiddenClassCount() == 0) {
			for (int i = 0; i < n; i++) {
				classifier.setClassIsHidden(true, i);
				slider.setClassIsHidden(true, i);
			}
			for (int i = n + 2; i < classifier.getNClasses(); i++) {
				classifier.setClassIsHidden(true, i);
				slider.setClassIsHidden(true, i);
			}
		}
		if (changingClasses.size() == 0) {
			changingClasses.addElement(n);
			changingClasses.addElement(n + 1);
		}
		classifier.setBreak((float) currValue, n);
		classifier.notifyChange("classes", changingClasses);
		updateFields();
	}

	/**
	* Change of colors in the slider
	*/
	@Override
	public void colorsChanged(Object source) {
		for (int i = 0; i < classifier.getNClasses(); i++) {
			classifier.setClassColor(slider.getColor(i), i);
		}
		classifier.notifyColorsChange();
		if (csc != null) {
			csc.repaint();
		}
	}

	/**
	* Calculates statistical errors
	*/
	protected void getMaxErrors() {
		Cursor oldCursor = getCursor();
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		System.out.println("Call getOptimalClassificationErrors");
		// following text: "Wait: heavy computations!"
		showMessage(res.getString("Wait_heavy"), false);
		long t = System.currentTimeMillis();
		int ncl = slider.getNBreaks() + 1;
		if (ncl < 2) {
			ncl = 2;
		}
		maxErrors = anc.getOptimalClassificationErrors(ncl, getCurrentMetrics());
		if (maxErrors == null) {
			showMessage("Failed to compute the classification quality!", true);
			setCursor(oldCursor);
			return;
		}
		if (maxErrors[0] == null) {
			maxErrors = anc.getOptimalClassificationErrors(slider.getNBreaks() + 1, OptNumClass.SUBMETHOD_MEAN);
		}
		t = System.currentTimeMillis() - t;
		System.out.println("getOptimalClassificationErrors took " + t + " msec");
		showMessage(null, false);
		setCursor(oldCursor);
	}

	protected void updateFields() {
		if (statPanActive && csc != null) {
			csc.repaint();
		}
		if (qualityPanActive) {
			if (slider == null)
				return;
			getMaxErrors();

			float breaks[] = new float[2 + slider.getNBreaks()];
			breaks[0] = (float) slider.getMin();
			for (int i = 0; i < slider.getNBreaks(); i++) {
				breaks[i + 1] = (float) slider.getBreakValue(i);
			}
			breaks[breaks.length - 1] = (float) slider.getMax();
			int subMethod = getCurrentMetrics(), ncl = slider.getNBreaks() + 1;
			float f = anc.estimateError(breaks, subMethod);
			if (maxErrors != null) {
				absError = (float) (100 * f / maxErrors[subMethod - OptNumClass.SUBMETHOD_MEAN][0]);
				//tfError.setText(StringUtil.floatToStr((float)absError,0f,100f));
				if (pbError != null) {
					pbError.setValue((100 - absError));
				}
				// following text:"Quality of the classification = "
				lQual.setText(res.getString("Quality_of_the1") + StringUtil.floatToStr((100 - absError), 0f, 100f) + " %");
				if (ncl > maxErrors[0].length) {
					ncl = maxErrors[0].length;
				}
				relError = (f == 0) ? 0f : (float) (100 * maxErrors[subMethod - OptNumClass.SUBMETHOD_MEAN][ncl - 1] / f);
				// following text:"Quality Vs. best for "+ncl+" classes = "
				lComp.setText(res.getString("Quality_Vs_best_for") + ncl + res.getString("classes_") + StringUtil.floatToStr(relError, 0f, 100f) + " %");
				pbQuality.setValue(relError);
			}
		}
	}

	protected void adjustToFilterChanges() {
		//System.out.println("Filter has been changed");
		anc.setUseFilter(autoClassApplyToQueryResults);
		if (reapplyAutoClass) {
			applyAutoClass();
		}
		updateFields();
	}

	/*
	* Query change
	*/
	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		if (pce.getSource().equals(classifier) && pce.getPropertyName().equals("hues")) {
			slider.setPositiveHue(classifier.getPositiveHue());
			slider.setNegativeHue(classifier.getNegativeHue());
			slider.setMiddleColor(classifier.getMiddleColor());
			slider.redraw();
			slider.notifyColorsChanged();
			return;
		}
		if (pce.getSource() == sup) {
			if (cc != null && pce.getPropertyName().equals(Supervisor.eventAttrColors)) {
				cc.colorsChanged(null);
			}
		} else if (pce.getPropertyName().equals("Filter")) {
			if (tFilter == null)
				return;
			if (pce.getPropertyName().equals("destroyed")) {
				tFilter.removePropertyChangeListener(this);
				tFilter = null;
			} else {
				adjustToFilterChanges();
			}
		} else if (pce.getSource().equals(dTable)) {
			if (pce.getPropertyName().equals("destroyed") || pce.getPropertyName().equals("structure_complete")) {
				destroy();
			} else if (pce.getPropertyName().equals("filter")) {
				if (tFilter != null) {
					tFilter.removePropertyChangeListener(this);
				}
				tFilter = dTable.getObjectFilter();
				if (tFilter != null) {
					tFilter.addPropertyChangeListener(this);
				}
				adjustToFilterChanges();
			} else if (pce.getPropertyName().equals("values")) {
				Vector v = (Vector) pce.getNewValue(); // list of changed attributes
				if (v != null && v.contains(dTable.getAttributeId(attrN))) {
					adjustToDataChange();
				}
			} else if (pce.getPropertyName().equals("data_removed") || pce.getPropertyName().equals("data_added") || pce.getPropertyName().equals("data_updated")) {
				adjustToDataChange();
			}
		} else if (pce.getSource() instanceof AttributeTransformer) {
			if (pce.getPropertyName().equals("values")) {
				boolean b = reapplyAutoClass;
				automaticBreaks = true;
				if (setSliderMinMaxIfNeeded()) {
					float min = (float) slider.getMin(), max = (float) slider.getMax();
					lMin.setText(StringUtil.floatToStr(min, min, max));
					lMax.setText(StringUtil.floatToStr(max, min, max));
					CManager.validateAll(lMin);
				}
				anc.clearAll();
				applyAutoClass();
				updateFields();
				cc.reset();
				reapplyAutoClass = b;
			} else if (pce.getPropertyName().equals("destroyed")) {
				destroy();
			}
		}
		if (!destroyed && csc != null) {
			csc.repaint();
		}
	}

	protected void adjustToDataChange() {
		if (setSliderMinMaxIfNeeded()) {
			float min = (float) slider.getMin(), max = (float) slider.getMax();
			lMin.setText(StringUtil.floatToStr(min, min, max));
			lMax.setText(StringUtil.floatToStr(max, min, max));
			CManager.validateAll(lMin);
		}
		anc.clearAll();
		if (reapplyAutoClass) {
			applyAutoClass();
		}
		updateFields();
	}

	/**
	* Changes the number of the attribute on the basis of which classification is
	* currently done (e.g. for time-series data)
	*/
	public void setColumnN(int colN) {
		if (attrN == colN)
			return;
		attrN = colN;
		anc.setColumnNumber(attrN);
		maxErrors = null;
		if (reapplyAutoClass) {
			applyAutoClass();
		}
		updateFields();
		int fn[] = cc.getFn();
		fn[0] = attrN;
		cc.setFn(fn);
	}

}
