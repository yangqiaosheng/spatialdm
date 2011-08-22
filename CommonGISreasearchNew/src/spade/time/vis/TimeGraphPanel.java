package spade.time.vis;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.CardLayout;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.classification.BroadcastClassesCP;
import spade.analysis.manipulation.DichromaticSlider;
import spade.analysis.manipulation.Slider;
import spade.analysis.manipulation.SliderListener;
import spade.analysis.plot.PrintableImage;
import spade.analysis.system.Supervisor;
import spade.analysis.transform.AttributeTransformer;
import spade.analysis.transform.TransformerGenerator;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.PlotCanvas;
import spade.lib.basicwin.RowLayout;
import spade.lib.basicwin.SplitLayout;
import spade.lib.basicwin.TabbedPanel;
import spade.lib.color.ColorSelDialog;
import spade.lib.color.Rainbow;
import spade.lib.lang.Language;
import spade.lib.util.DoubleArray;
import spade.lib.util.IntArray;
import spade.lib.util.StringUtil;
import spade.time.FocusInterval;
import spade.time.TimeMoment;
import spade.time.query.TimeQueryBuilder;
import spade.time.query.TimeQueryControls;
import spade.time.ui.TimeSlider;
import spade.time.ui.TimeSliderPanel;
import spade.vis.database.AttributeDataPortion;
import spade.vis.spec.AnimationAttrSpec;
import spade.vis.spec.SaveableTool;
import spade.vis.spec.TemporalToolSpec;

/**
 * Contains one or more TimeGraphs and various manipulation controls.
 */
public class TimeGraphPanel extends Panel implements ItemListener, ActionListener, PropertyChangeListener, SliderListener, Destroyable, SaveableTool, PrintableImage {
	public static String eventShowClassesLines = "eventShowClassesLines", eventShowClassesFlows = "eventShowClassesFlows";

	static ResourceBundle res = Language.getTextResource("spade.time.vis.Res");
	/**
	 * The time graph(s) to be manipulated.
	 */
	protected TimeGraph tigr[] = null;
	/**
	 * The time slider for focusing on time subranges
	 */
	protected TimeSlider timeSlider = null;
	/**
	 * The attribute transformer transforms the data displayed on the time
	 * graph(s): aggregates, computes changes, compares to mean, median, or
	 * selected object, etc.
	 */
	protected AttributeTransformer aTrans = null;
	/**
	 * The UI components for data transformation
	 */
	protected Vector transUIs = null;

	protected Supervisor supervisor = null;

	/**
	 * A combobox for choosing the selection operation
	 */
	protected Choice chSelectMode = null;

	/**
	 * Checkboxes for switching on/off showing only selected objects, average
	 * and median lines
	 */
	protected Checkbox cbShowSelectionOnly = null, cbShowLineAvg = null, cbShowLineMedian = null, cbShowTrend = null, rbTrendFromAvg = null, rbTrendFromMedian = null, cbSubtractTrend = null, rbSubTrFromOrig = null, rbSubTrFromTransformed = null,
			cbDrawValueFlow = null, cbDrawValueClasses = null, cbDrawGrid = null;
	protected Button bStoreTransformed1 = null, bStoreTransformed2 = null;
	protected TextField tfLiderPeriod = null, tfLiderOffset = null;
	protected Choice chQuantiles = null;
	protected BroadcastClassesCP pClassesLines = null, pClassesFlows = null;

	protected Button bAdvancedUI = null;

	/**
	 * Edit fields for specifying temporal extent of the time graph Button for
	 * setting the full time extend
	 */
	protected Button bStartEnd = null;
	protected FocusInterval focusInterval = null;

	/**
	 * Controls for line segmentation
	 */
	protected Checkbox cbSegmentation = null, cbSegmDiff = null, cbSegmRatio = null, cbSegmValues = null;
	protected TextField tfSegmLo = null, tfSegmHi = null;
	protected Component compSegm[] = null;
	protected Button bSegmIncrease = null, bSegmDecrease = null;

	/**
	 * Graph and controls for temporal aggregation
	 */
	protected TimeGraphSummary tsummary[] = null;
	protected Checkbox cbTSummary = null, // switch on/off
			cbSaveTiGr = null, cbSaveTAggr = null;
	protected Label labelTSummaryMin = null, labelTSummaryMax = null, // min/max
			// values
			labelTSummaryBreaks = null, labelTSummaryOrder = null;
	protected TextField tfTSummary = null; // class breaks
	protected Button bTSummaryShift = null; // alignment
	protected DichromaticSlider dslTSummary = null;
	protected Slider slTSummary = null;
	protected Rainbow rainbow = null; // color selection control
	protected PlotCanvas canvasSliderTSummary = null;
	protected Panel pTSummary[] = null, ControlPanelTSummary = null; // Panels
	// with
	// graphs
	// and
	// with
	// all
	// controls
	protected Panel pAllGraphs = null; // Panel with all graphs

	/*
	 * Panel containing time graph and time aggregator
	 */
	protected SplitLayout graphSplit[] = null;
	/**
	 * The panel with card layout which shows either the selection control panel
	 * or the query control panel
	 */
	protected Panel selQueryPanel = null;
	/**
	 * Radio buttons for switching between selection and querying
	 */
	protected Checkbox rbSelect = null, rbQuery = null;

	protected Checkbox cbCommonMinMax = null;
	/**
	 * A TimeQueryBuilder suports the formulation of queries concerning
	 * time-series data. The user formulates the queries graphically by drawing
	 * and manipulating boxes on a time graph. An individual TimeQueryBuilder is
	 * constructed for each time graph.
	 */
	protected TimeQueryBuilder queryBuilders[] = null;
	protected TimeQueryControls queryPanel = null;

	/**
	 * RTimeSeries Control Panel
	 */
	protected RTimeSeriesPanel cpRStatistics = null;

	/**
	 * Creates the panel with the given time graph in it.
	 */
	public TimeGraphPanel(Supervisor supervisor, TimeGraph timeGraph[], TimeGraphSummary timeAggregation[]) {
		nInstances++;
		instanceN = nInstances;
		tigr = timeGraph;
		for (TimeGraph element : tigr) {
			TimeMoment tmf = (TimeMoment) element.getTemporalParameter().getFirstValue(), tml = (TimeMoment) element.getTemporalParameter().getLastValue();
			element.setTimeFocusStart(tmf);
			element.setTimeFocusEnd(tml);
		}
		tsummary = timeAggregation;
		// Parameter tpar=tigr[0].getTemporalParameter();
		aTrans = tigr[0].getAttributeTransformer();
		if (aTrans == null) {
			buildAttributeTransformer();
		} else {
			transUIs = new Vector(10, 10);
			AttributeTransformer at = aTrans;
			while (at != null) {
				Component ui = at.getIndividualUI();
				if (ui != null) {
					transUIs.addElement(ui);
				}
				at = at.getNextTransformer();
			}
		}
		if (aTrans != null) {
			aTrans.addPropertyChangeListener(this);
		}
		queryBuilders = new TimeQueryBuilder[tigr.length];
		queryPanel = new TimeQueryControls();
		for (int i = 0; i < tigr.length; i++) {
			queryBuilders[i] = new TimeQueryBuilder();
			queryBuilders[i].setTimeGraph(tigr[i]);
			queryBuilders[i].setAttributeTransformer(aTrans);
			queryBuilders[i].setControlPanel(queryPanel);
		}
		setSupervisor(supervisor);
		createControls();
		createAdvancedLayout(); // instead of createBasicLayout() for dummies
		pClassesLines.setSupervisor(supervisor);
		pClassesFlows.setSupervisor(supervisor);
		setControlStates();
	}

	protected void createControls() {
		pTSummary = new Panel[tigr.length];
		graphSplit = new SplitLayout[tigr.length];
		int n2 = tigr.length / 2;
		if (2 * n2 < tigr.length) {
			n2++;
		}
		pAllGraphs = new Panel();
		pAllGraphs.setLayout(new GridLayout(n2, 2, 0, 0));
		for (int n = 0; n < tigr.length; n++) {
			pTSummary[n] = new Panel();
			graphSplit[n] = new SplitLayout(pTSummary[n], SplitLayout.HOR);
			graphSplit[n].setAllowSwapParts(false);
			pTSummary[n].setLayout(graphSplit[n]);
			graphSplit[n].addComponent(tigr[n], 0.5f);
			pAllGraphs.add(pTSummary[n]);
		}

		bAdvancedUI = new Button("Activate advanced user interface");
		bAdvancedUI.addActionListener(this);

		bStartEnd = new Button(((TimeMoment) tigr[0].getTemporalParameter().getFirstValue()).toString() + ".." + ((TimeMoment) tigr[0].getTemporalParameter().getLastValue()).toString());
		bStartEnd.addActionListener(this);
		bStartEnd.setEnabled(false);
		focusInterval = new FocusInterval();
		focusInterval.setDataInterval((TimeMoment) tigr[0].getTemporalParameter().getFirstValue(), (TimeMoment) tigr[0].getTemporalParameter().getLastValue());
		focusInterval.addPropertyChangeListener(this);

		cbShowSelectionOnly = new Checkbox(res.getString("only_selected"), tigr[0].getDrawOnlySelected());
		cbShowSelectionOnly.addItemListener(this);
		cbCommonMinMax = new Checkbox("Common min/max", false);
		cbCommonMinMax.addItemListener(this);
		cbDrawValueFlow = new Checkbox("Value flow", true);
		cbDrawValueFlow.addItemListener(this);
		cbDrawValueClasses = new Checkbox("Value classes", true);
		cbDrawValueClasses.addItemListener(this);
		cbDrawGrid = new Checkbox("Grid;", true);
		cbDrawGrid.addItemListener(this);
		tfLiderPeriod = new TextField("24");
		tfLiderPeriod.addActionListener(this);
		tfLiderOffset = new TextField("0");
		tfLiderOffset.addActionListener(this);
		cbShowLineAvg = new Checkbox("Average", false);
		cbShowLineAvg.addItemListener(this);
		cbShowLineMedian = new Checkbox("Median", false);
		cbShowLineMedian.addItemListener(this);

		cbShowTrend = new Checkbox("Show common trend line built from", false);
		cbShowTrend.addItemListener(this);
		CheckboxGroup cbg = new CheckboxGroup();
		rbTrendFromAvg = new Checkbox("average", true, cbg);
		rbTrendFromAvg.addItemListener(this);
		rbTrendFromMedian = new Checkbox("median values", false, cbg);
		rbTrendFromMedian.addItemListener(this);
		cbSubtractTrend = new Checkbox("Subtract individual trends from", false);
		cbSubtractTrend.addItemListener(this);
		cbg = new CheckboxGroup();
		rbSubTrFromOrig = new Checkbox("original data", true, cbg);
		rbSubTrFromOrig.addItemListener(this);
		rbSubTrFromOrig.setEnabled(false);
		rbSubTrFromTransformed = new Checkbox("transformed data", false, cbg);
		rbSubTrFromTransformed.addItemListener(this);
		rbSubTrFromTransformed.setEnabled(false);
		bStoreTransformed1 = new Button("Store transformed data");
		bStoreTransformed1.setActionCommand("store_transformed");
		bStoreTransformed1.addActionListener(this);
		bStoreTransformed2 = new Button("Store transformed data");
		bStoreTransformed2.setActionCommand("store_transformed");
		bStoreTransformed2.addActionListener(this);

		chQuantiles = new Choice();
		for (int i = 0; i < 10; i++) {
			chQuantiles.add(String.valueOf(i + 1));
		}
		chQuantiles.addItemListener(this);
		cbSaveTiGr = new Checkbox("Save", false);
		cbSaveTiGr.setEnabled(false);
		for (TimeGraph element : tigr) {
			cbSaveTiGr.addItemListener(element);
		}
		pClassesLines = new BroadcastClassesCP(tigr[0].getTable().getEntitySetIdentifier(), "Classes (lines):", eventShowClassesLines, true, true, false);
		pClassesFlows = new BroadcastClassesCP(tigr[0].getTable().getEntitySetIdentifier(), "Classes (flows):", eventShowClassesFlows, false, false, true);
		for (TimeGraph element : tigr) {
			pClassesLines.addPropertyChangeListener(element);
			pClassesFlows.addPropertyChangeListener(element);
		}

		cbSegmentation = new Checkbox("Show only selected line segments", false);
		cbSegmentation.addItemListener(this);
		compSegm = new Component[10];
		compSegm[0] = new Label("where");
		compSegm[1] = tfSegmLo = new TextField("0", 6);
		tfSegmLo.addActionListener(this);
		compSegm[2] = new Label("<= difference since previous moment <=");
		compSegm[3] = tfSegmHi = new TextField("", 6);
		tfSegmHi.addActionListener(this);
		compSegm[4] = bSegmIncrease = new Button("increase");
		bSegmIncrease.addActionListener(this);
		compSegm[5] = bSegmDecrease = new Button("decrease");
		bSegmDecrease.addActionListener(this);
		compSegm[6] = new Label("Segmentation criterion: ");
		cbg = new CheckboxGroup();
		compSegm[7] = cbSegmDiff = new Checkbox("difference", cbg, true);
		cbSegmDiff.addItemListener(this);
		compSegm[8] = cbSegmRatio = new Checkbox("ratio", cbg, false);
		cbSegmRatio.addItemListener(this);
		compSegm[9] = cbSegmValues = new Checkbox("Value range", cbg, false);
		cbSegmValues.addItemListener(this);

		cbTSummary = new Checkbox("Value classes", false);
		cbTSummary.addItemListener(this);
		cbSaveTAggr = new Checkbox("Save", false);
		cbSaveTAggr.setEnabled(false);
		for (TimeGraphSummary element : tsummary) {
			cbSaveTAggr.addItemListener(element);
		}
		labelTSummaryOrder = new Label("Order:");
		bTSummaryShift = new Button("^");
		bTSummaryShift.addActionListener(this);
		labelTSummaryBreaks = new Label("Breaks:");
		rainbow = new Rainbow();
		rainbow.setActionListener(this);
		labelTSummaryMin = new Label("-1");
		tfTSummary = new TextField("0", 20);
		labelTSummaryMax = new Label("+1");
		dslTSummary = new DichromaticSlider();
		dslTSummary.setMidPoint(0f);
		slTSummary = dslTSummary.getClassificationSlider();
		slTSummary.setMinMax(-1f, 1f);
		canvasSliderTSummary = new PlotCanvas();
		dslTSummary.setCanvas(canvasSliderTSummary);
		slTSummary.setTextField(tfTSummary);
		slTSummary.addSliderListener(this);
		slTSummary.setPositiveHue(-1101f);
		slTSummary.setNegativeHue(0.1f);
		canvasSliderTSummary.setContent(dslTSummary);
		canvasSliderTSummary.setVisible(false);

		chSelectMode = new Choice();
		chSelectMode.addItemListener(this);
		for (int i = TimeGraph.SEL_OPER_FIRST; i <= TimeGraph.SEL_OPER_LAST; i++) {
			switch (i) {
			case TimeGraph.SEL_REPLACE:
				chSelectMode.add(res.getString("sel_replace"));
				break;
			case TimeGraph.SEL_OR:
				chSelectMode.add(res.getString("sel_or"));
				break;
			case TimeGraph.SEL_AND:
				chSelectMode.add(res.getString("sel_and"));
				break;
			case TimeGraph.SEL_TOGGLE:
				chSelectMode.add(res.getString("sel_toggle"));
				break;
			}
		}
		chSelectMode.select(tigr[0].getSelectionOperation());

		cbg = new CheckboxGroup();
		rbSelect = new Checkbox(res.getString("Object_selection"), true, cbg);
		rbSelect.addItemListener(this);
		rbQuery = new Checkbox(res.getString("Query_building"), false, cbg);
		rbQuery.addItemListener(this);
	}

	protected void createBasicLayout() {
		setLayout(new BorderLayout());
		add(pAllGraphs, BorderLayout.CENTER);

		TabbedPanel tp = new TabbedPanel();
		add(tp, BorderLayout.SOUTH);

		Panel p = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0)), pp = new Panel(new BorderLayout());
		tp.addComponent("Time extent", pp);
		pp.add(p, BorderLayout.WEST);
		p.add(bStartEnd);
		if (timeSlider == null) {
			timeSlider = new TimeSlider(focusInterval);
			if (supervisor != null) {
				timeSlider.setSupervisor(supervisor);
			}
		}
		pp.add(new TimeSliderPanel(timeSlider, this, true), BorderLayout.CENTER);

		p = new Panel();
		p.setLayout(new ColumnLayout());
		tp.addComponent("Display", p);
		pp = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		pp.add(cbShowSelectionOnly);
		if (tigr.length > 1) {
			pp.add(cbCommonMinMax);
		}
		pp.add(cbDrawValueFlow);
		// pp.add(cbDrawValueClasses);
		pp.add(cbDrawGrid);
		p.add(pp);
		pp = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		pp.add(new Label("Statistics:"));
		pp.add(cbShowLineAvg);
		pp.add(cbShowLineMedian);
		// pp.add(new Label("Quantiles:"));
		// pp.add(chQuantiles);
		// pp.add(cbSaveTiGr);
		p.add(pp);
		pp = new Panel();
		pp.setLayout(new RowLayout(3, 0));
		pp.add(pClassesLines);
		p.add(pp);
		pp = new Panel();
		pp.setLayout(new RowLayout(3, 0));
		pp.add(pClassesFlows);
		p.add(pp);

		// the interface for data transformation
		if (transUIs != null && transUIs.size() > 0) {
			p = new Panel(new RowLayout(5, 0));
			for (int i = 0; i < transUIs.size(); i++) {
				if (i > 0) {
					p.add(new Line(true));
				}
				p.add((Component) transUIs.elementAt(i));
			}
			tp.addComponent("Transformation", p);
		}

		p = new Panel();
		p.add(bAdvancedUI);
		tp.addComponent("Advanced", p);

		tp.makeLayout(true);
	}

	protected void createAdvancedLayout() {
		removeAll();

		setLayout(new BorderLayout());
		add(pAllGraphs, BorderLayout.CENTER);

		TabbedPanel tp = new TabbedPanel();
		add(tp, BorderLayout.SOUTH);

		Panel p = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0)), pp = new Panel(new BorderLayout());
		tp.addComponent("Time extent", pp);
		pp.add(p, BorderLayout.WEST);
		p.add(bStartEnd);
		if (timeSlider == null) {
			timeSlider = new TimeSlider(focusInterval);
			if (supervisor != null) {
				timeSlider.setSupervisor(supervisor);
			}
		}
		pp.add(new TimeSliderPanel(timeSlider, this, true), BorderLayout.CENTER);

		p = new Panel();
		p.setLayout(new ColumnLayout());
		tp.addComponent("Display", p);
		pp = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		pp.add(cbShowSelectionOnly);
		if (tigr.length > 1) {
			pp.add(cbCommonMinMax);
		}
		pp.add(cbDrawValueFlow);
		pp.add(cbDrawValueClasses);
		pp.add(cbDrawGrid);
		pp.add(new Label("Vert.grid period:"));
		pp.add(tfLiderPeriod);
		pp.add(new Label("offset:"));
		pp.add(tfLiderOffset);
		p.add(pp);
		pp = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		pp.add(new Label("Statistics:"));
		pp.add(cbShowLineAvg);
		pp.add(cbShowLineMedian);
		pp.add(new Label("Quantiles:"));
		pp.add(chQuantiles);
		pp.add(cbSaveTiGr);
		p.add(pp);
		pp = new Panel();
		pp.setLayout(new RowLayout(3, 0));
		pp.add(pClassesLines);
		p.add(pp);
		pp = new Panel();
		pp.setLayout(new RowLayout(3, 0));
		pp.add(pClassesFlows);
		p.add(pp);

		// the interface for data transformation
		if (transUIs != null && transUIs.size() > 0) {
			p = new Panel(new RowLayout(5, 0));
			for (int i = 0; i < transUIs.size(); i++) {
				if (i > 0) {
					p.add(new Line(true));
				}
				p.add((Component) transUIs.elementAt(i));
			}
			p.add(bStoreTransformed1);
			tp.addComponent("Transformation", p);
		}

		p = new Panel(new ColumnLayout());
		tp.addComponent("Trend", p);
		pp = new Panel(new RowLayout());
		pp.add(cbShowTrend);
		pp.add(rbTrendFromAvg);
		pp.add(rbTrendFromMedian);
		p.add(pp);
		pp = new Panel(new RowLayout());
		pp.add(cbSubtractTrend);
		pp.add(rbSubTrFromOrig);
		pp.add(rbSubTrFromTransformed);
		p.add(pp);
		pp = new Panel(new FlowLayout(FlowLayout.RIGHT, 5, 2));
		pp.add(bStoreTransformed2);
		p.add(pp);

		p = new Panel();
		p.setLayout(new ColumnLayout());
		tp.addComponent("Segmentation", p);
		p.add(cbSegmentation);
		pp = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		p.add(pp);
		pp.add(compSegm[0]);
		pp.add(tfSegmLo);
		pp.add(compSegm[2]);
		pp.add(tfSegmHi);
		pp.add(bSegmIncrease);
		pp.add(bSegmDecrease);
		pp = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		p.add(pp);
		pp.add(compSegm[6]);
		pp.add(cbSegmDiff);
		pp.add(cbSegmRatio);
		pp.add(cbSegmValues);
		for (Component element : compSegm) {
			element.setVisible(false);
		}

		ControlPanelTSummary = new Panel(new ColumnLayout());
		tp.addComponent("Classification", ControlPanelTSummary);
		p = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		ControlPanelTSummary.add(p);
		p.add(cbTSummary);
		p.add(cbSaveTAggr);
		p.add(labelTSummaryOrder);
		p.add(bTSummaryShift);
		p = new Panel(new BorderLayout());
		ControlPanelTSummary.add(p);
		p.add(labelTSummaryBreaks, BorderLayout.WEST);
		pp = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		p.add(pp, BorderLayout.EAST);
		pp.add(new Label(""));
		pp.add(rainbow);
		pp.add(new Label(""));
		pp = new Panel(new GridLayout(1, 2, 0, 0));
		p.add(pp, BorderLayout.CENTER);
		Panel ppp = new Panel(new BorderLayout());
		pp.add(ppp);
		ppp.add(labelTSummaryMin, BorderLayout.WEST);
		ppp.add(tfTSummary, BorderLayout.CENTER);
		ppp.add(labelTSummaryMax, BorderLayout.EAST);
		pp.add(canvasSliderTSummary);

		if (supervisor.getSystemSettings().getParameterAsString("PATH_TO_RSTATISTICS") != null) {
			Vector attrs = new Vector();
			attrs.add(tigr[0].getAttribute());
			cpRStatistics = new RTimeSeriesPanel(supervisor, tigr[0], attrs);
			tp.addComponent("R statistics", cpRStatistics);
		}

		selQueryPanel = new Panel(new CardLayout());
		p = new Panel(new RowLayout());
		p.add(new Label(res.getString("Dragging")));
		p.add(chSelectMode);
		p.add(new Label(res.getString("current_sel")));
		selQueryPanel.add(p, "select");
		selQueryPanel.add(queryPanel, "query");
		p = new Panel(new BorderLayout());
		pp = new Panel(new RowLayout(3, 0));
		pp.add(new Label(res.getString("Use_dragging_for")));
		pp.add(rbSelect);
		pp.add(rbQuery);
		p.add(pp, BorderLayout.NORTH);
		p.add(selQueryPanel, BorderLayout.CENTER);
		tp.addComponent(res.getString("sel_or_query"), p);

		tp.makeLayout(true);
	}

	/**
	 * Creates an attribute transformer to transform the data displayed on the
	 * time graph(s): aggregate, compute changes, compare to mean, median, or
	 * selected object, etc.
	 */
	protected void buildAttributeTransformer() {
		aTrans = null;
		if (tigr == null || tigr.length < 1 || tigr[0] == null || tigr[0].getTemporalParameter() == null || tigr[0].getTemporalParameter().getValueCount() < 2)
			return;
		// first create an array of all table columns to be transformed
		IntArray columns = new IntArray(tigr[0].getTemporalParameter().getValueCount() * tigr.length, 10);
		for (TimeGraph element : tigr) {
			IntArray cols = element.getColumnNumbers();
			if (cols == null) {
				continue;
			}
			for (int j = 0; j < cols.size(); j++) {
				columns.addElement(cols.elementAt(j));
			}
		}
		if (columns.size() < 2)
			return;
		AttributeDataPortion table = tigr[0].getTable();
		AttributeTransformer prevTrans = null;
		transUIs = new Vector(TransformerGenerator.getTransformerCount(), 1);
		for (int i = 0; i < TransformerGenerator.getTransformerCount(); i++) {
			AttributeTransformer trans = TransformerGenerator.makeTransformer(i);
			if (trans == null) {
				continue;
			}
			trans.setDataTable(table);
			trans.setColumnNumbers(columns);
			if (!trans.isValid()) {
				continue;
			}
			Component c = trans.getUI();
			if (c != null) {
				transUIs.addElement(c);
			}
			if (prevTrans != null) {
				trans.setPreviousTransformer(prevTrans);
			}
			prevTrans = trans;
			if (aTrans == null) {
				aTrans = trans;
			}
		}
	}

	/*
	 * protected void checkClassesRelevance () { boolean relevant=
	 * supervisor!=null && tigr!=null && supervisor.getObjectColorer()!=null &&
	 * (supervisor.getObjectColorer() instanceof Classifier) &&
	 * supervisor.getObjectColorer().getEntitySetIdentifier().equals(
	 * tigr[0].getTable().getEntitySetIdentifier());
	 * //lClassesLines.setVisible(relevant);
	 * //lClassesFlows.setVisible(relevant); }
	 */

	protected void setSupervisor(Supervisor supervisor) {
		this.supervisor = supervisor;
		if (supervisor != null) {
			supervisor.addPropertyChangeListener(this);
			if (timeSlider != null) {
				timeSlider.setSupervisor(supervisor);
			}
		}
		// checkClassesRelevance();
	}

	protected void setControlStates() {
		cbSaveTAggr.setVisible(cbTSummary.getState());
		labelTSummaryBreaks.setVisible(cbTSummary.getState());
		labelTSummaryMin.setVisible(cbTSummary.getState());
		tfTSummary.setVisible(cbTSummary.getState());
		labelTSummaryMax.setVisible(cbTSummary.getState());
		labelTSummaryOrder.setVisible(cbTSummary.getState());
		bTSummaryShift.setVisible(cbTSummary.getState());
		rainbow.setVisible(cbTSummary.getState());
		if (cbTSummary.getState()) {
			setupSummary();
		}
	}

	protected void setupSummary() {
		if (!cbTSummary.getState())
			return;
		double absMin = tigr[0].getAbsMin(), absMax = tigr[0].getAbsMax();
		labelTSummaryMin.setText(StringUtil.doubleToStr(absMin, absMin, absMax));
		labelTSummaryMax.setText(StringUtil.doubleToStr(absMax, absMin, absMax));
		slTSummary.removeSliderListener(this);
		slTSummary.setMinMax(absMin, absMax);
		DoubleArray br = slTSummary.getBreaks();
		if (br.size() < 1) {
			br.addElement((absMin + absMax) / 2f);
			slTSummary.addBreak(br.elementAt(0));
		}
		double mp = slTSummary.getMidPoint();
		if (mp <= absMin || mp >= absMax) {
			dslTSummary.setMidPoint((br.elementAt(0) + absMin) / 2f);
		}
		String txt = StringUtil.doubleToStr(br.elementAt(0), absMin, absMax);
		for (int i = 1; i < br.size(); i++) {
			txt += " " + StringUtil.doubleToStr(br.elementAt(i), absMin, absMax);
		}
		tfTSummary.setText(txt);
		float breaks[] = new float[br.size()];
		for (int i = 0; i < br.size(); i++) {
			breaks[i] = (float) br.elementAt(i);
		}
		Color colors[] = new Color[br.size() + 2];
		for (int i = 0; i < colors.length - 1; i++) {
			colors[i] = slTSummary.getColor(i);
		}
		colors[colors.length - 1] = Color.lightGray;
		for (int n = 0; n < tigr.length; n++) {
			tsummary[n].setBreaks(breaks, colors);
			tigr[n].setTAbreaks(breaks, colors);
			tsummary[n].setIndents(tigr[n].getLeftIndent(), tigr[n].getRightIndent());
		}
		ControlPanelTSummary.invalidate();
		ControlPanelTSummary.validate();
		slTSummary.addSliderListener(this);
	}

	public void itemStateChanged(ItemEvent e) {
		if (e.getSource().equals(chSelectMode)) {
			for (TimeGraph element : tigr) {
				element.setSelectionOperation(chSelectMode.getSelectedIndex());
			}
			// ??? setGroupMinMax();
			return;
		}
		if (e.getSource().equals(cbShowSelectionOnly)) {
			for (TimeGraph element : tigr) {
				element.setDrawOnlySelected(cbShowSelectionOnly.getState());
			}
			return;
		}
		if (e.getSource().equals(cbDrawValueFlow)) {
			for (TimeGraph element : tigr) {
				element.setDrawValueFlow(cbDrawValueFlow.getState());
			}
			return;
		}
		if (e.getSource().equals(cbDrawValueClasses)) {
			for (TimeGraph element : tigr) {
				element.setDrawValueClasses(cbDrawValueClasses.getState());
			}
			return;
		}
		if (e.getSource().equals(cbDrawGrid)) {
			for (TimeGraph element : tigr) {
				element.setDrawGrid(cbDrawGrid.getState());
			}
			return;
		}
		if (e.getSource().equals(cbShowLineAvg)) {
			for (TimeGraph element : tigr) {
				element.setShowLineAvg(cbShowLineAvg.getState());
			}
			return;
		}
		if (e.getSource().equals(cbShowLineMedian)) {
			for (TimeGraph element : tigr) {
				element.setShowLineMedian(cbShowLineMedian.getState());
			}
			return;
		}
		if (e.getSource().equals(cbShowTrend)) {
			for (TimeGraph element : tigr) {
				element.setShowTrendLine(cbShowTrend.getState());
				if (cbShowTrend.getState()) {
					element.setSegmentationOff();
				}
			}
			cbSegmentation.setState(false);
			for (Component element : compSegm) {
				element.setVisible(cbSegmentation.getState());
			}
			return;
		}
		if (e.getSource().equals(rbTrendFromAvg) || e.getSource().equals(rbTrendFromMedian)) {
			for (TimeGraph element : tigr) {
				element.setBuildTrendFromMedians(rbTrendFromMedian.getState());
			}
			return;
		}
		if (e.getSource().equals(cbSubtractTrend) || e.getSource().equals(rbSubTrFromOrig) || e.getSource().equals(rbSubTrFromTransformed)) {
			rbSubTrFromOrig.setEnabled(cbSubtractTrend.getState());
			rbSubTrFromTransformed.setEnabled(cbSubtractTrend.getState());
			for (TimeGraph element : tigr) {
				element.setSubtractTrend(cbSubtractTrend.getState(), rbSubTrFromTransformed.getState());
			}
			return;
		}
		if (e.getSource().equals(cbTSummary)) {
			if (cbTSummary.getState()) {
				if (!cbSaveTAggr.getState()) {
					cbSaveTAggr.setEnabled(true);
				}
				for (int n = 0; n < tigr.length; n++) {
					if (tsummary[n].notReady()) {
						tsummary[n].setLines(tigr[n].getLines());
						tsummary[n].setIndices(tigr[0].getIdxTFstart(), tigr[0].getIdxTFend());
					}
					graphSplit[n].addComponent(tsummary[n], 0.5f);
				}
			} else {
				for (int n = 0; n < tigr.length; n++) {
					graphSplit[n].removeComponent(1);
					tigr[n].setTAbreaks(null, null);
				}
			}
			canvasSliderTSummary.setVisible(cbTSummary.getState());
			setControlStates();
			CManager.validateAll(pAllGraphs);
			return;
		}
		if (e.getSource().equals(chQuantiles)) {
			int ncl = 1 + chQuantiles.getSelectedIndex();
			if (ncl > 1 && !cbSaveTiGr.getState()) {
				cbSaveTiGr.setEnabled(true);
			}
			for (TimeGraph element : tigr) {
				element.setNClasses(ncl, true);
			}
			return;
		}
		if (e.getSource().equals(cbCommonMinMax)) {
			setCommonMinMax();
			return;
		}
		if (e.getSource().equals(cbSegmentation)) {
			for (Component element : compSegm) {
				element.setVisible(cbSegmentation.getState());
			}
			if (cbSegmentation.getState()) {
				CManager.validateAll(compSegm[0]);
				setSegmentationMode();
				for (TimeGraph element : tigr)
					if (element.getShowTrendLine()) {
						element.setShowTrendLine(false);
					}
				cbShowTrend.setState(false);
			} else {
				for (TimeGraph element : tigr) {
					element.setSegmentationOff();
				}
			}
			return;
		}
		if (e.getSource().equals(cbSegmRatio) || e.getSource().equals(cbSegmDiff) || e.getSource().equals(cbSegmValues)) {
			setSegmentationMode();
			return;
		}
		if (e.getSource().equals(rbSelect) || e.getSource().equals(rbQuery)) {
			CardLayout cl = (CardLayout) selQueryPanel.getLayout();
			boolean queryEnabled = rbQuery.getState();
			if (queryEnabled) {
				cl.show(selQueryPanel, "query");
			} else {
				cl.show(selQueryPanel, "select");
			}
			for (TimeQueryBuilder queryBuilder : queryBuilders) {
				queryBuilder.setEnabled(queryEnabled);
			}
			return;
		}
	}

	protected void setSegmentationMode() {
		String txt = "<= " + ((cbSegmValues.getState()) ? "value range" : ((cbSegmDiff.getState()) ? "difference to previous moment" : "ratio to previous moment")) + " <=";
		((Label) compSegm[2]).setText(txt);
		CManager.validateAll(compSegm[2]);
		bSegmIncrease.setEnabled(!cbSegmValues.getState());
		bSegmDecrease.setEnabled(!cbSegmValues.getState());
		float segmLo = (tfSegmLo.getText() == null || tfSegmLo.getText().length() == 0) ? Float.NaN : Float.valueOf(tfSegmLo.getText()).floatValue();
		if (Float.isNaN(segmLo)) {
			tfSegmLo.setText("");
		}
		float segmHi = (tfSegmHi.getText() == null || tfSegmHi.getText().length() == 0) ? Float.NaN : Float.valueOf(tfSegmHi.getText()).floatValue();
		if (Float.isNaN(segmHi)) {
			tfSegmHi.setText("");
		}
		if (Float.isNaN(segmLo) && Float.isNaN(segmHi))
			return;
		int segmMode = (cbSegmDiff.getState()) ? TimeGraph.TG_SegmDiff : ((cbSegmRatio.getState()) ? TimeGraph.TG_SegmRatio : TimeGraph.TG_SegmValues);
		for (TimeGraph element : tigr) {
			element.setSegmentationOn(segmLo, segmHi, segmMode);
		}
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource().equals(rainbow)) {
			startChangeColors();
			return;
		}
		if (e.getSource().equals(bAdvancedUI)) {
			createAdvancedLayout();
			CManager.invalidateAll(this);
			CManager.validateAll(this);
			return;
		}
		if (e.getSource().equals(bSegmIncrease)) {
			tfSegmLo.setText((cbSegmDiff.getState()) ? "0" : "1");
			tfSegmHi.setText("");
			setSegmentationMode();
			return;
		}
		if (e.getSource().equals(bSegmDecrease)) {
			tfSegmLo.setText("");
			tfSegmHi.setText((cbSegmDiff.getState()) ? "0" : "1");
			setSegmentationMode();
			return;
		}
		if (e.getSource().equals(tfLiderPeriod) || e.getSource().equals(tfLiderOffset)) {
			int n = 0, m = 0;
			String str = tfLiderPeriod.getText();
			try {
				if (str != null) {
					n = Integer.valueOf(str).intValue();
				}
				str = tfLiderOffset.getText();
				if (str != null) {
					m = Integer.valueOf(str).intValue();
				}
			} catch (NumberFormatException nfe) {
				n = m = -1;
			}
			if (n >= 0 && m >= 0) {
				for (TimeGraph element : tigr) {
					element.setLiderLines(n, m);
				}
			} else {
				tfLiderPeriod.setText("" + tigr[0].getLiderPeriod());
				tfLiderOffset.setText("" + tigr[0].getLiderOffset());
			}
			return;
		}
		if (e.getSource().equals(tfLiderOffset))
			return;
		if (e.getSource().equals(tfSegmLo) || e.getSource().equals(tfSegmHi)) {
			setSegmentationMode();
			return;
		}
		if (e.getSource().equals(bStartEnd)) {
			for (int n = 0; n < tigr.length; n++) {
				tigr[n].setTimeFocusFullExtent();
				tsummary[n].setIndices(tigr[0].getIdxTFstart(), tigr[0].getIdxTFend());
			}
			// ??? setGroupMinMax();
			bStartEnd.setEnabled(false);
			focusInterval.setCurrInterval((TimeMoment) tigr[0].getTemporalParameter().getFirstValue(), (TimeMoment) tigr[0].getTemporalParameter().getLastValue());
			return;
		}
		if (e.getSource().equals(bTSummaryShift)) {
			for (int n = 0; n < tigr.length; n++) {
				tsummary[n].shift();
			}
			return;
		}
		if (e.getActionCommand().equals("store_transformed")) {
			for (TimeGraph element : tigr) {
				element.storeTransformedData();
			}
		}
	}

	public void propertyChange(PropertyChangeEvent e) {
		if (e.getSource().equals(aTrans) && e.getPropertyName().equals("values")) {
			if (tigr[0].getAttributeTransformer() == null) {
				// we must ensure that the time graph controls react to
				// transformation
				// changes after the time graph(s)
				aTrans.removePropertyChangeListener(this);
				for (TimeGraph element : tigr) {
					element.setAttributeTransformer(aTrans);
				}
				aTrans.addPropertyChangeListener(this);
				// now the time graph controls follow the time graph(s) in the
				// list of
				// transformation change listeners
			}
			setupSummary();
			return;
		}
		/*
		 * if (e.getPropertyName().equals(Supervisor.eventObjectColors) &&
		 * tigr[0].getTable().getEntitySetIdentifier().equals(e.getNewValue()))
		 * { checkClassesRelevance(); return; }
		 */
		if (e.getSource() instanceof FocusInterval) {
			if (e.getPropertyName().equalsIgnoreCase("current_interval")) {
				TimeMoment t1 = (TimeMoment) e.getOldValue(), t2 = (TimeMoment) e.getNewValue();
				setFocusInterval(t1, t2);
			}
		}
	}

	protected void setFocusInterval(TimeMoment t1, TimeMoment t2) {
		if (t2.equals(t1))
			return;
		for (int n = 0; n < tigr.length; n++) {
			tigr[n].setTimeFocusStartEnd(t1, t2);
			tsummary[n].setIndices(tigr[0].getIdxTFstart(), tigr[0].getIdxTFend());
		}
		bStartEnd.setEnabled(0 != tigr[0].getFocusStart().compareTo((TimeMoment) tigr[0].getTemporalParameter().getFirstValue()) || 0 != tigr[0].getFocusEnd().compareTo((TimeMoment) tigr[0].getTemporalParameter().getLastValue()));
		cpRStatistics.redrawTimeCanvas();
	}

	public void setCommonMinMax() {
		for (TimeGraph element : tigr) {
			element.recalcAbsMinMax();
		}
		if (cbCommonMinMax != null && cbCommonMinMax.getState()) {
			double commonMin = Double.NaN, commonMax = Double.NaN;
			for (TimeGraph element : tigr) {
				double v = element.getAbsMin();
				if (Double.isNaN(commonMin) || v < commonMin) {
					commonMin = v;
				}
				v = element.getAbsMax();
				if (Double.isNaN(commonMax) || v > commonMax) {
					commonMax = v;
				}
			}
			for (TimeGraph element : tigr) {
				element.setUseCommonMinMax(commonMin, commonMax);
			}
		} else {
			for (TimeGraph element : tigr) {
				element.setUseCommonMinMax();
			}
		}
	}

	protected Color[] tSummaryClassColors() {
		Color colors[] = new Color[2 + slTSummary.getNBreaks()];
		for (int i = 0; i < colors.length - 1; i++) {
			colors[i] = slTSummary.getColor(i);
		}
		colors[colors.length - 1] = Color.lightGray;
		return colors;
	}

	// SliderListener interface -------- begin
	private float breakValues[] = null;

	public void breaksChanged(Object source, double[] breaks, int nBreaks) {
		if (!source.equals(slTSummary))
			return;
		if (nBreaks == 0) {
			setupSummary();
			return;
		}
		breakValues = new float[nBreaks];
		for (int i = 0; i < nBreaks; i++) {
			breakValues[i] = (float) breaks[i];
		}
		if (breaks.length > 0) {
			Color colors[] = tSummaryClassColors();
			for (int n = 0; n < tigr.length; n++) {
				tsummary[n].setBreaks(breakValues, colors);
				tigr[n].setTAbreaks(breakValues, colors);
			}
		}
	}

	public void breakIsMoving(Object source, int n, double currValue) {
		if (!source.equals(slTSummary))
			return;
		float oldNthBreakValue = (breakValues == null || breakValues.length < n + 1) ? Float.NaN : breakValues[n];
		if (breakValues == null || breakValues.length != slTSummary.getNBreaks()) {
			breakValues = new float[slTSummary.getNBreaks()];
			for (int i = 0; i < breakValues.length; i++) {
				breakValues[i] = (float) slTSummary.getBreakValue(i);
			}
		}
		if (!Float.isNaN(oldNthBreakValue) && Math.abs(currValue - oldNthBreakValue) < 0.01f * (tigr[0].getAbsMax() - tigr[0].getAbsMin()))
			return;
		breakValues[n] = (float) currValue;
		Color colors[] = tSummaryClassColors();
		for (int i = 0; i < tigr.length; i++) {
			tsummary[i].setBreaks(breakValues, colors);
		}
	}

	public void colorsChanged(Object source) {
		Color colors[] = tSummaryClassColors();
		if (colors == null)
			return;
		if (colors.length != 2 + breakValues.length)
			return;
		for (int n = 0; n < tigr.length; n++) {
			tigr[n].setTAbreaks(breakValues, colors);
			tsummary[n].setBreaks(breakValues, colors);
		}
	}

	// SliderListener interface -------- end

	public void startChangeColors() {
		float hues[] = new float[2];
		hues[0] = slTSummary.getPositiveHue();
		hues[1] = slTSummary.getNegativeHue();
		String prompts[] = new String[2];
		prompts[0] = "Positive color ?";
		prompts[1] = "Negative color ?";
		ColorSelDialog csd = new ColorSelDialog(2, hues, slTSummary.getMiddleColor(), prompts, true, true);
		OKDialog okd = new OKDialog(CManager.getAnyFrame(null), csd.getName(), true);
		okd.addContent(csd);
		okd.show();
		if (okd.wasCancelled())
			return;
		slTSummary.setPositiveHue(csd.getHueForItem(0));
		slTSummary.setNegativeHue(csd.getHueForItem(1));
		slTSummary.setMiddleColor(csd.getMidColor());
		slTSummary.redraw();
		// slTSummary.notifyColorsChanged();
		Color colors[] = tSummaryClassColors();
		if (breakValues == null) {
			breakValues = tsummary[0].getBreaks();
		}
		for (int n = 0; n < tigr.length; n++) {
			tigr[n].setTAbreaks(breakValues, colors);
			tsummary[n].setBreaks(breakValues, colors);
		}
	}

	private static int nInstances = 0;
	private int instanceN = 0;

	public Image getImage() {
		if (tigr == null || tigr.length == 0)
			return null;
		int w = tigr[0].getBounds().width;
		int h = tigr[0].getBounds().height;
		Image img = createImage(w, h * tigr.length);
		for (int i = 0; i < tigr.length; i++) {
			TimeGraph gr = (TimeGraph) tigr[i];
			Image ic = createImage(w, h);
			gr.draw(ic.getGraphics());
			img.getGraphics().drawImage(ic, 0, h * i, null);
		}
		return img;
	}

	public String getName() {
		String name = "Time graph " + instanceN;
		if (tigr != null && tigr.length > 0 && tigr[0] != null && tigr[0].getTable() != null) {
			name += ": " + tigr[0].getTable().getName();
		}
		return name;
	}

	// -------------------- Destroyable interface
	// ---------------------------------
	protected boolean destroyed = false;
	/**
	 * As a SaveableTool, a TimeGraphPanel may be registered somewhere and,
	 * hence, must notify the component where it is registered about its
	 * destroying. This vector contains the listeners to be notified about
	 * destroying of the TimeGraphPanel.
	 */
	protected Vector destroyListeners = null;

	/**
	 * Adds a listener to be notified about destroying the tool. A SaveableTool
	 * may be registered somewhere and, hence, must notify the component where
	 * it is registered about its destroying.
	 */
	public void addDestroyingListener(PropertyChangeListener lst) {
		if (lst == null)
			return;
		if (destroyListeners == null) {
			destroyListeners = new Vector(5, 5);
		}
		if (!destroyListeners.contains(lst)) {
			destroyListeners.addElement(lst);
		}
	}

	/**
	 * Stops listening of all events, unregisters itself from object event
	 * sources
	 */
	public void destroy() {
		if (isDestroyed())
			return;
		if (destroyListeners != null) {
			PropertyChangeEvent pce = new PropertyChangeEvent(this, "destroyed", null, null);
			for (int i = 0; i < destroyListeners.size(); i++) {
				((PropertyChangeListener) destroyListeners.elementAt(i)).propertyChange(pce);
			}
		}
		if (supervisor != null) {
			supervisor.removePropertyChangeListener(this);
		}
		destroyed = true;
	}

	public boolean isDestroyed() {
		return destroyed;
	}

	/**
	 * Returns the keyword used in the opening tag of a stored state description
	 * of this tool. A tool state description (specification) is stored as a
	 * sequence of lines starting with <tagName> and ending with </tagName>,
	 * where tagName is a unique keyword for a particular class of tools.
	 */
	public String getTagName() {
		return "temporal_vis";
	}

	/**
	 * Returns the specification (i.e. state description) of this tool for
	 * storing in a file. The specification must allow correct re-construction
	 * of the tool.
	 */
	public Object getSpecification() {
		TemporalToolSpec spec = new TemporalToolSpec();
		spec.tagName = getTagName();
		spec.methodId = "time_graph";
		spec.properties = getProperties();
		// spec.properties=getVisProperties();
		if (tigr != null && tigr.length > 0) {
			AttributeDataPortion table = tigr[0].getTable();
			if (table != null) {
				spec.table = table.getContainerIdentifier();
			}
			spec.attrSpecs = new Vector(tigr.length, 1);
			for (TimeGraph element : tigr) {
				AnimationAttrSpec asp = new AnimationAttrSpec();
				asp.parent = element.getAttribute().getIdentifier();
				asp.isTimeDependent = true;
				asp.fixedParams = element.getOtherParamNames();
				asp.fixedParamVals = element.getOtherParamValues();
				spec.attrSpecs.addElement(asp);
			}
			if (spec.attrSpecs.size() < 1) {
				spec.attrSpecs = null;
			}
		}
		if (aTrans != null) {
			spec.transformSeqSpec = aTrans.getSpecSequence();
		}
		return spec;
	}

	/**
	 * After the tool is constructed, it may be requested to setup its
	 * individual properties according to the given list of stored properties.
	 */
	public void setProperties(Hashtable properties) {
		try {
			cbShowSelectionOnly.setState(Boolean.valueOf((String) properties.get("showSelectionOnly")).booleanValue());
			for (TimeGraph element : tigr) {
				element.setDrawOnlySelected(cbShowSelectionOnly.getState());
			}
		} catch (Exception ex) {
		}
		try {
			boolean b = Boolean.valueOf((String) properties.get("commonMinMax")).booleanValue();
			cbCommonMinMax.setState(b);
			for (TimeGraph element : tigr) {
				element.setup();
				element.reset(false);
			}
			setCommonMinMax(); // still does not work - to be done!
		} catch (Exception ex) {
		}
		try {
			cbShowLineAvg.setState(Boolean.valueOf((String) properties.get("showLineAvg")).booleanValue());
			for (TimeGraph element : tigr) {
				element.setShowLineAvg(cbShowLineAvg.getState());
			}
		} catch (Exception ex) {
		}
		try {
			cbShowLineMedian.setState(Boolean.valueOf((String) properties.get("showLineMedian")).booleanValue());
			for (TimeGraph element : tigr) {
				element.setShowLineMedian(cbShowLineMedian.getState());
			}
		} catch (Exception ex) {
		}
		try {
			String val = (String) properties.get("focus_start");
			if (val != null) {
				TimeMoment t = focusInterval.getCurrIntervalStart().getCopy();
				t.setMoment(val);
				focusInterval.setCurrIntervalStart(t);
				setFocusInterval(focusInterval.getCurrIntervalStart(), focusInterval.getCurrIntervalEnd());
			}
		} catch (Exception ex) {
		}
		try {
			String val = (String) properties.get("focus_end");
			if (val != null) {
				TimeMoment t = focusInterval.getCurrIntervalEnd().getCopy();
				t.setMoment(val);
				focusInterval.setCurrIntervalEnd(t);
				setFocusInterval(focusInterval.getCurrIntervalStart(), focusInterval.getCurrIntervalEnd());
			}
		} catch (Exception ex) {
		}
	}

	/**
	 * Returns custom properties of the tool: String -> String By default,
	 * returns null.
	 */
	public Hashtable getProperties() {
		Hashtable prop = new Hashtable();
		prop.put("showSelectionOnly", String.valueOf(cbShowSelectionOnly.getState()));
		prop.put("commonMinMax", String.valueOf(cbCommonMinMax.getState()));
		prop.put("showLineAvg", String.valueOf(cbShowLineAvg.getState()));
		prop.put("showLineMedian", String.valueOf(cbShowLineMedian.getState()));
		prop.put("focus_start", focusInterval.getCurrIntervalStart().toString());
		prop.put("focus_end", focusInterval.getCurrIntervalEnd().toString());
		return prop;
	}
}
