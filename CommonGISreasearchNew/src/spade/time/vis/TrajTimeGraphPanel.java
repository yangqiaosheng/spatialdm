package spade.time.vis;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.TextField;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import spade.analysis.manipulation.DichromaticSlider;
import spade.analysis.manipulation.Slider;
import spade.analysis.manipulation.SliderListener;
import spade.analysis.system.DataLoader;
import spade.analysis.system.ESDACore;
import spade.analysis.system.Supervisor;
import spade.analysis.system.SystemUI;
import spade.analysis.tools.moves.EventExtractor;
import spade.analysis.util.GeoObjectsSelector;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.Dialogs;
import spade.lib.basicwin.FocusListener;
import spade.lib.basicwin.Focuser;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.MultiSelector;
import spade.lib.basicwin.NotificationLine;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.PlotCanvas;
import spade.lib.basicwin.RowLayout;
import spade.lib.basicwin.SelectDialog;
import spade.lib.basicwin.TabbedPanel;
import spade.lib.basicwin.TextCanvas;
import spade.lib.color.ColorSelDialog;
import spade.lib.color.Rainbow;
import spade.lib.util.DoubleArray;
import spade.lib.util.IdMaker;
import spade.lib.util.IntArray;
import spade.lib.util.ListSelector;
import spade.lib.util.NumValManager;
import spade.lib.util.StringUtil;
import spade.time.Date;
import spade.time.FocusInterval;
import spade.time.TimeMoment;
import spade.time.TimePositionNotifier;
import spade.time.TimeReference;
import spade.time.ui.TimeSlider;
import spade.time.ui.TimeSliderPanel;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataItem;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.database.ObjectContainer;
import spade.vis.database.SpatialEntity;
import spade.vis.database.ThematicDataItem;
import spade.vis.database.ThematicDataOwner;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DMovingObject;
import spade.vis.dmap.DMovingObjectSegmFilter;
import spade.vis.dmap.DMovingObjectSegmFilterCombiner;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RealPoint;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 01-Feb-2007
 * Time: 16:49:00
 * Includes an EventCanvas and interactive manipulation controls.
 */
public class TrajTimeGraphPanel extends Panel implements SliderListener, FocusListener, ActionListener, ItemListener, ComponentListener, PropertyChangeListener, Destroyable {
	/**
	 * The layer with trajectories, which are visualised
	 */
	protected DGeoLayer trLayer = null;
	/**
	 * The canvas in which the visualisation is done
	 */
	protected TrajTimeGraphCanvas evCanvas = null;
	/**
	 * The canvas in which labels are drawn (to remain fixed independently of
	 * the scroller position)
	 */
	protected TimeLineLabelsCanvas tLabCanvas = null;
	/**
	 * The canvas for drawing events, shows time ticks in the same way as TimeLineLabelsCanvas
	 */
	//protected TrajTimeGraphEventCanvas tEventCanvas=null;
	/**
	 * The canvas for legend for attribute values
	 */
	protected TrajTimeGraphLegendCanvas tLegendCanvas = null;

	/**
	 * The canvas for the focuser, used in time graph for selecting attribute interval
	 */
	protected TrajTimeGraphFocuserCanvas tFocuserCanvas = null;

	protected TrajTimeGraphInfoCanvas tInfoCanvas = null;
	/**
	 * The scroll pane containing the canvas
	 */
	protected ScrollPane scp = null;
	/**
	 * Supervisor provides access of a plot to the Highlighter (common for
	 * all data displays) and in this way links together all displays
	 */
	protected Supervisor supervisor = null;
	/**
	 * Shows the names of the attributes used for grouping
	 */
	protected TextField attrTF = null;
	/**
	 * Allows to switch between showing all or only active events
	 */
	protected Checkbox onlyActiveCB = null;
	/**
	* Used for changing the temporal extent of the visualization
	*/
	protected FocusInterval focusInterval = null;
	/**
	 * The UI control for changing the temporal extent of the visualization
	 */
	protected TimeSlider timeSlider = null;
	/**
	 * Used for extending the focuser to the whole time extent
	 */
	protected Button bStartEnd = null;

	/**
	 * Used for selecting attribute of the trajectory to be visualized
	 */
	protected Choice chTrajAttr = null;
	/**
	 * The index of the last computed attribute
	 */
	protected int lastComputedAttr = -1;

	protected Panel controlP = null; // main control panel
	//controlPextra=null; // placeholder for further UI elements

	/**
	 * range of values of the displayed attribute
	 */
	protected double dMin = Float.NaN, dMax = Float.NaN;
	protected boolean atStart = true; // don't show range dialog at start

	/**
	 * units of values of the displayed attribute
	 */
	protected String sUnits = "";

	/**
	 * controls for classification
	 */
	protected Checkbox cbTSummary = null;
	protected Label labelTSummaryBreaks = null, labelTSummaryMin = null, labelTSummaryMax = null;
	protected TextField tfTSummary = null;
	protected Rainbow rainbow = null;
	protected DichromaticSlider dslTSummary = null;
	protected Slider slTSummary = null;
	protected PlotCanvas canvasSliderTSummary = null;
	protected Checkbox cbDynUpdate = null;
	/**
	 * Controld for setting the maximum time of value validity
	 */
	protected TextField tfValidTime = null;
	protected Choice chTimeUnit = null;
	/**
	 * The date components included in the choice
	 */
	protected char dateComp[] = null;

	/**
	 * controls for display
	 */
	protected Checkbox chViewMode[] = null; // time graph or time line
	protected TextField tfBarWidth = null;
	protected Checkbox cbOrderAsc = null; //
	protected Button bClearFilter = null;
	protected Label notLine = null;

	protected TimePositionNotifier tpn = null;

	protected int whatIsFiltered = DMovingObjectSegmFilter.FilterForPoints;

	/**
	 * Constructs the visualisation display.
	 * @param oCont - container with time-referenced items (events)
	 */
	public TrajTimeGraphPanel(ObjectContainer oCont) {
		if (oCont instanceof DGeoLayer) {
			trLayer = (DGeoLayer) oCont;
			trLayer.addPropertyChangeListener(this);
		} else
			return;
		setLayout(new BorderLayout());

		Panel mainp = new Panel();
		add(mainp, BorderLayout.CENTER);
		mainp.setLayout(new BorderLayout());

		mainp.add(tInfoCanvas = new TrajTimeGraphInfoCanvas(), BorderLayout.NORTH);
		Panel lp = new Panel(new BorderLayout());
		lp.add(tLegendCanvas = new TrajTimeGraphLegendCanvas(), BorderLayout.CENTER);
		Panel pp = new Panel(new FlowLayout(FlowLayout.CENTER, 0, 2));
		bClearFilter = new Button("Clear");
		bClearFilter.setActionCommand("clear_segment_filter");
		bClearFilter.addActionListener(this);
		bClearFilter.setEnabled(false);
		pp.add(bClearFilter);
		lp.add(pp, BorderLayout.SOUTH);
		mainp.add(lp, BorderLayout.WEST);
		tLegendCanvas.setActionListener(this);
		mainp.add(tFocuserCanvas = new TrajTimeGraphFocuserCanvas(this), BorderLayout.EAST);
		Panel infop = new Panel(new BorderLayout());
		mainp.add(infop, BorderLayout.CENTER);
		//tEventCanvas=new TrajTimeGraphEventCanvas();
		//infop.add(tEventCanvas,BorderLayout.SOUTH);

		evCanvas = new TrajTimeGraphCanvas();
		evCanvas.setObjectContainer(oCont);
		scp = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
		scp.add(evCanvas);
		infop.add(scp, BorderLayout.CENTER);
		scp.addComponentListener(this);
		notLine = new NotificationLine("");
		infop.add(notLine, BorderLayout.SOUTH);

		tLabCanvas = new TimeLineLabelsCanvas();
		infop.add(tLabCanvas, BorderLayout.NORTH);
/*
    TimeLineLabelsCanvas tllc[]=new TimeLineLabelsCanvas[2];
    tllc[0]=tLabCanvas;
    tllc[1]=tEventCanvas;
*/
		TimeLineLabelsCanvas tllc[] = new TimeLineLabelsCanvas[1];
		tllc[0] = tLabCanvas;
		evCanvas.setTimeLineLabelsCanvas(tllc);

		boolean hasAttributes = false;
		if (oCont != null && oCont.getObjectCount() > 0) {
			ThematicDataItem td = null;
			for (int i = 0; i < oCont.getObjectCount() && td == null; i++) {
				DataItem data = oCont.getObjectData(i);
				if (data == null) {
					continue;
				}
				if (data instanceof ThematicDataItem) {
					td = (ThematicDataItem) data;
				} else if (data instanceof ThematicDataOwner) {
					td = ((ThematicDataOwner) data).getThematicData();
				}
			}
			if (td != null) {
				hasAttributes = td.getAttrCount() > 0;
			}
		}
		Panel sortP = null;
		if (hasAttributes) {
			Panel p = new Panel(new BorderLayout());
			p.add(new Label("Sort/group by:"), BorderLayout.NORTH);
			attrTF = new TextField("<no attributes selected>");
			attrTF.setEditable(false);
			p.add(attrTF, BorderLayout.CENTER);
			Button b = new Button("Change");
			b.setActionCommand("change");
			b.addActionListener(this);
			p.add(b, BorderLayout.EAST);
			p.add(cbOrderAsc = new Checkbox("Ascending order", true), BorderLayout.SOUTH);
			cbOrderAsc.addItemListener(this);
			pp = new Panel(new ColumnLayout());
			pp.add(p);
			sortP = new Panel(new BorderLayout());
			sortP.add(pp, BorderLayout.CENTER);
			sortP.add(new Line(true), BorderLayout.EAST);
		}

		controlP = new Panel(new ColumnLayout());
		TabbedPanel tp = new TabbedPanel();
		controlP.add(new Line(false));
		controlP.add(tp);
		add(controlP, BorderLayout.SOUTH);

		Panel controlPtime = new Panel(new BorderLayout());
		TimeMoment t0 = evCanvas.getDataStart(), t1 = evCanvas.getDataEnd();
		if (t0 == null || t1 == null) {
			evCanvas.setup();
			t0 = evCanvas.getDataStart();
			t1 = evCanvas.getDataEnd();
		}
		if (t0 != null && t1 != null) {
			focusInterval = new FocusInterval();
			focusInterval.setDataInterval(t0, t1);
			focusInterval.addPropertyChangeListener(this);
			timeSlider = new TimeSlider(focusInterval);
			TimeSliderPanel tspan = new TimeSliderPanel(timeSlider, this, true);
			bStartEnd = new Button("Full extent");
			bStartEnd.addActionListener(this);
			bStartEnd.setActionCommand("full_extent");
			bStartEnd.setEnabled(false);
			controlPtime.add(tspan, BorderLayout.CENTER);
			controlPtime.add(bStartEnd, BorderLayout.WEST);
		}

		Panel controlPdata = new Panel(new BorderLayout());
		Panel controlPdataLeft = new Panel(new ColumnLayout());
		controlPdata.add(controlPdataLeft, BorderLayout.WEST);
		controlPdataLeft.add(new Label("Trajectory attribute:"));
		chTrajAttr = new Choice();
		controlPdataLeft.add(chTrajAttr);
		for (String tr_point_attribute : tr_point_attributes) {
			chTrajAttr.add(tr_point_attribute);
		}
		Panel p = new Panel(new FlowLayout(FlowLayout.CENTER, 20, 2));
		Button b = new Button("Compute");
		b.setActionCommand("compute_traj_attr");
		b.addActionListener(this);
		p.add(b);
		b = new Button("Attach to position records");
		b.setActionCommand("attach_traj_attr");
		b.addActionListener(this);
		p.add(b);
		b = new Button("Get statistics");
		b.setActionCommand("traj_statistics");
		b.addActionListener(this);
		p.add(b);
		controlPdataLeft.add(p);
		p = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 2));
		p.add(new Label("Max value validity time:"));
		tfValidTime = new TextField(3);
		p.add(tfValidTime);
		if (t0 != null && (t0 instanceof Date)) {
			chTimeUnit = new Choice();
			int nComp = 0;
			Date d = (Date) t0;
			for (int i = Date.time_symbols.length - 1; i >= 0; i--)
				if (d.hasElement(Date.time_symbols[i])) {
					chTimeUnit.add(Date.getTextForTimeSymbolInPlural(Date.time_symbols[i]));
					nComp++;
				}
			if (nComp < 2) {
				chTimeUnit = null;
			} else {
				dateComp = new char[nComp];
				int k = 0;
				for (int i = Date.time_symbols.length - 1; i >= 0; i--)
					if (d.hasElement(Date.time_symbols[i])) {
						dateComp[k++] = Date.time_symbols[i];
					}
				p.add(chTimeUnit);
			}
		}
		b = new Button("Apply");
		b.setActionCommand("apply_valid_time");
		b.addActionListener(this);
		p.add(b);
		controlPdataLeft.add(p);

		b = new Button("Extract events");
		b.setActionCommand("extract_events");
		b.addActionListener(this);
		p = new Panel(new FlowLayout(FlowLayout.CENTER, 0, 2));
		p.add(b);
		pp = new Panel(new ColumnLayout());
		pp.add(p);
		b = new Button("Get statistics for events");
		b.setActionCommand("event_statistics");
		b.addActionListener(this);
		p = new Panel(new FlowLayout(FlowLayout.CENTER, 0, 2));
		p.add(b);
		pp.add(p);
		controlPdata.add(pp, BorderLayout.EAST);

		Panel controlPdisplay = new Panel(new RowLayout());
		Panel controlPdisplayFirstColumn = new Panel(new ColumnLayout());
		controlPdisplay.add(controlPdisplayFirstColumn);
		p = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		p.add(new Label("Appearance:"));
		controlPdisplayFirstColumn.add(p);
		CheckboxGroup cbg = new CheckboxGroup();
		chViewMode = new Checkbox[2];
		p.add(chViewMode[0] = new Checkbox("Time graph", cbg, true));
		p.add(chViewMode[1] = new Checkbox("Time line", cbg, false));
		chViewMode[0].addItemListener(this);
		chViewMode[1].addItemListener(this);
		controlPdisplayFirstColumn.add(new Line(false));
		onlyActiveCB = new Checkbox("Show only selected lines", true);
		onlyActiveCB.addItemListener(this);
		controlPdisplayFirstColumn.add(onlyActiveCB);
		evCanvas.setShowOnlyActiveEvents(onlyActiveCB.getState());
		controlPdisplayFirstColumn.add(new Line(false));
		p = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		controlPdisplayFirstColumn.add(p);
		controlPdisplayFirstColumn.add(new Line(false));
		p.add(new Label("Bar width:"));
		p.add(tfBarWidth = new TextField("5", 3));
		tfBarWidth.addActionListener(this);
		controlPdisplay.add(new Line(true));
		if (sortP != null) {
			controlPdisplay.add(sortP);
		}

		Panel controlPclasses = createClassificationPanel();

		//Panel controlPevents=new Panel();

		tp.addComponent("time", controlPtime);
		tp.addComponent("data", controlPdata);
		tp.addComponent("display", controlPdisplay);
		tp.addComponent("classes", controlPclasses);
		//tp.addComponent("events",controlPevents);
		tp.makeLayout(true);

		tpn = new TimePositionNotifier();
		evCanvas.setTimePositionNotifier(tpn);
		tLabCanvas.setTimePositionNotifier(tpn);
		//tEventCanvas.setTimePositionNotifier(tpn);
		tpn.addPropertyChangeListener(this);

		lastComputedAttr = 0;
		setupForNewTrajectoryAttribute();
		setSegmFilters(false);
	}

	protected Panel createClassificationPanel() {
		cbTSummary = new Checkbox("Value classes", false);
		cbTSummary.addItemListener(this);
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
		// slTSummary.setNegativeHue(0.1f);
		canvasSliderTSummary.setContent(dslTSummary);
		canvasSliderTSummary.setVisible(false);
		cbDynUpdate = new Checkbox("Dynamic update", true);

		Panel ControlPanelTSummary = new Panel(new ColumnLayout());
		//1st line: checkbox + button: show/hide cumulative frequency curve
		ControlPanelTSummary.add(cbTSummary);
		//2nd line: textfield and labels for min/max
		Panel p = new Panel(new BorderLayout());
		Panel pp = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		pp.add(labelTSummaryBreaks);
		pp.add(labelTSummaryMin);
		p.add(pp, BorderLayout.WEST);
		p.add(tfTSummary, BorderLayout.CENTER);
		pp = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		pp.add(labelTSummaryMax);
		pp.add(new Label(""));
		p.add(pp, BorderLayout.EAST);
		ControlPanelTSummary.add(p);
		//3rd line: rainbow and slider
		p = new Panel(new BorderLayout());
		pp = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		pp.add(new Label(""));
		pp.add(rainbow);
		pp.add(new Label(""));
		p.add(pp, BorderLayout.WEST);
		p.add(canvasSliderTSummary, BorderLayout.CENTER);
		p.add(new Label(""), BorderLayout.EAST);
		ControlPanelTSummary.add(p);
		pp = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		ControlPanelTSummary.add(pp);
		pp.add(cbDynUpdate);

		/*
		Panel p=new Panel(new FlowLayout(FlowLayout.LEFT,0,0));
		ControlPanelTSummary.add(p);
		p.add(cbTSummary);
		p=new Panel(new BorderLayout());
		ControlPanelTSummary.add(p);
		p.add(labelTSummaryBreaks,BorderLayout.WEST);
		Panel pp=new Panel(new FlowLayout(FlowLayout.LEFT,0,0));
		p.add(pp,BorderLayout.EAST);
		pp.add(new Label(""));
		pp.add(rainbow);
		pp.add(new Label(""));
		pp=new Panel(new GridLayout(1,2,0,0));
		p.add(pp,BorderLayout.CENTER);
		Panel ppp=new Panel(new BorderLayout());
		pp.add(ppp);
		ppp.add(labelTSummaryMin,BorderLayout.WEST);
		ppp.add(tfTSummary,BorderLayout.CENTER);
		ppp.add(labelTSummaryMax,BorderLayout.EAST);
		pp.add(canvasSliderTSummary);
		*/
		return ControlPanelTSummary;
	}

	public void setSupervisor(Supervisor supervisor) {
		this.supervisor = supervisor;
		evCanvas.setSupervisor(supervisor);
		if (timeSlider != null) {
			timeSlider.setSupervisor(supervisor);
		}
		if (tpn != null) {
			tpn.setSupervisor(supervisor);
		}
	}

	/**
	 * A ListSelector specifies which objects should be drawn
	 */
	public void setListSelector(ListSelector listSelector) {
		evCanvas.setListSelector(listSelector);
	}

	public Dimension getPreferredSize() {
		Dimension d = evCanvas.getPreferredSize(), ss = Toolkit.getDefaultToolkit().getScreenSize();
		int w = Math.min(d.width, ss.width * 2 / 3), h = Math.min(d.height, ss.height * 2 / 3);
		w += scp.getVScrollbarWidth() + 10;
		h += scp.getHScrollbarHeight() + 10 + tLabCanvas.getPreferredSize().height;
		//h+=tEventCanvas.getPreferredSize().height;
		d = controlP.getPreferredSize();
		h += d.height;
		if (w < d.width) {
			w = d.width;
		}
		return new Dimension(w, h);
	}

	private ThematicDataItem getThematicDataItem() {
		if (trLayer == null)
			return null;
		for (int i = 0; i < trLayer.getObjectCount(); i++) {
			ThematicDataItem data = trLayer.getObject(i).getData();
			if (data != null)
				return data;
		}
		return null;
	}

	public void itemStateChanged(ItemEvent e) {
		if (e.getSource().equals(cbOrderAsc)) {
			evCanvas.setSortType(cbOrderAsc.getState());
		}
		if (e.getSource().equals(onlyActiveCB)) {
			evCanvas.setShowOnlyActiveEvents(onlyActiveCB.getState());
		}
		if (e.getSource().equals(chViewMode[0]) || e.getSource().equals(chViewMode[1])) {
			// redraw main canvas
			evCanvas.setIsTGraphMode(chViewMode[0].getState());
			if (chViewMode[1].getState()) {
				Color colors[] = tSummaryClassColors();
				evCanvas.setBreaksAndFocuserMinMax(breakValues, colors, dMin, dMax);
			}
			// modify legend
			tLegendCanvas.setIsTGraphMode(chViewMode[0].getState());
			// change UI layout, show/hide controls
			tFocuserCanvas.setVisible(chViewMode[0].getState());
			CManager.validateAll(tFocuserCanvas);
		}
		if (e.getSource().equals(cbTSummary)) {
			setupSummary();
		}
	}

	protected void setupSummary() {
		if (!cbTSummary.getState()) {
			for (int i = 0; i < trLayer.getObjectCount(); i++) {
				((DMovingObject) trLayer.getObject(i)).clearFilter(this);
			}
			evCanvas.setBreaksAndFocuserMinMax(null, null, dMin, dMax);
			breakValues = null;
			tLegendCanvas.setBreaks(null, null);
			tLegendCanvas.clearSegmSelection();
			labelTSummaryMin.setVisible(false);
			labelTSummaryMax.setVisible(false);
			rainbow.setVisible(false);
			tfTSummary.setVisible(false);
			canvasSliderTSummary.setVisible(false);
			cbDynUpdate.setVisible(false);
			trLayer.propertyChange(new PropertyChangeEvent(this, "TrajSegmFilter", null, null));
			return;
		}
		labelTSummaryMin.setVisible(true);
		labelTSummaryMax.setVisible(true);
		rainbow.setVisible(true);
		tfTSummary.setVisible(true);
		canvasSliderTSummary.setVisible(true);
		cbDynUpdate.setVisible(true);
		invalidate();
		validate();
		double absMin = dMin, absMax = dMax;
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
		breakValues = new float[br.size()];
		for (int i = 0; i < br.size(); i++) {
			breakValues[i] = (float) br.elementAt(i);
		}
		Color colors[] = tSummaryClassColors();
		evCanvas.setBreaksAndFocuserMinMax(breakValues, colors, dMin, dMax);
		tLegendCanvas.setBreaks(breakValues, colors);
		//for (int n=0; n<tigr.length; n++) {
		//tsummary[n].setBreaks(breaks,colors);
		// ... tigr[n].setTAbreaks(breaks,colors);
		//tsummary[n].setIndents(tigr[n].getLeftIndent(),tigr[n].getRightIndent());
		//}
		//ControlPanelTSummary.invalidate(); ControlPanelTSummary.validate();
		slTSummary.addSliderListener(this);
	}

	/**
	 * Total count of trajectory points
	 */
	protected long nPointsTotal = 0;
	/**
	 * Counts of trajectory points and segments (sequences of points) satisfying current
	 * segment filter of this panel
	 */
	protected long nPointsAfterFilter = 0, nSegmentsAfterFilter = 0, nTrajAfterFilter = 0;
	/**
	 * Counts of trajectory points and segments (sequences of points) satisfying current
	 * segment filter, possibly, combined
	 */
	protected long nPointsAllFilters = 0, nSegmentsAllFilters = 0, nTrajAllFilters = 0;

	protected void setSegmFilters(boolean activatedByOthers) {
		Cursor cursor = getCursor();
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		int nObj = trLayer.getObjectCount();
		nPointsTotal = 0;
		nPointsAfterFilter = 0;
		nSegmentsAfterFilter = 0;
		nTrajAfterFilter = 0;
		nPointsAllFilters = 0;
		nSegmentsAllFilters = 0;
		nTrajAllFilters = 0;
		if (breakValues == null || breakValues.length == 0 || tLegendCanvas.areAllSegmentsSelected()) {
			for (int i = 0; i < nObj; i++) {
				DMovingObject dmo = ((DMovingObject) trLayer.getObject(i));
				dmo.clearFilter(this);
				Vector trk = dmo.getTrack();
				if (trk == null) {
					continue;
				}
				int np = trk.size();
				nPointsTotal += np;
				if (!trLayer.isObjectActive(i) || !dmo.isAnySegmActive()) {
					continue;
				}
				++nTrajAllFilters;
				if (dmo.areAllSegmentsActive()) {
					nPointsAllFilters += np;
					++nSegmentsAllFilters;
				} else {
					boolean seq = false;
					for (int j = 0; j < np; j++)
						if (dmo.isSegmActive(j)) {
							++nPointsAllFilters;
							if (!seq) {
								++nSegmentsAllFilters;
								seq = true;
							}
						} else {
							seq = false;
						}
				}
			}
			nPointsAfterFilter = nPointsTotal;
			nSegmentsAfterFilter = nTrajAfterFilter = nObj;
			bClearFilter.setEnabled(false);
		} else {
			for (int i = 0; i < nObj; i++) {
				DMovingObject dmo = ((DMovingObject) trLayer.getObject(i));
				Vector trk = dmo.getTrack();
				if (trk == null) {
					continue;
				}
				int np = trk.size();
				nPointsTotal += np;
				boolean bitmap[] = dmo.getSegmFilterBitmap(this);
				if (bitmap == null) {
					bitmap = new boolean[np];
				}
				double attrVals[] = this.evCanvas.getAttrVals(i);
				boolean trCounted = false;
				if (attrVals == null) {
					for (int b = 0; b < bitmap.length; b++) {
						bitmap[b] = false; //the trajectories for which no values have been computed are not shown
					}
				} else {
					for (int b = 0; b < bitmap.length; b++)
						if (b >= attrVals.length || Double.isNaN(attrVals[b])) {
							bitmap[b] = false;
						} else {
							// find the class number
							int classN = -1;
							for (int c = 0; c <= breakValues.length && classN == -1; c++)
								if (c == breakValues.length) {
									classN = c;
								} else if (attrVals[b] < breakValues[c]) {
									classN = c;
								}
							//check if this class is active
							bitmap[b] = classN >= tLegendCanvas.getSegmSelection().length || tLegendCanvas.getSegmSelection()[classN];
							if (bitmap[b]) {
								++nPointsAfterFilter;
								if (b == 0 || !bitmap[b - 1]) {
									++nSegmentsAfterFilter;
								}
								if (!trCounted) {
									++nTrajAfterFilter;
									trCounted = true;
								}
							}
						}
				}
				dmo.setSegmFilterBitmap(this, whatIsFiltered, bitmap);
/*
        String id=dmo.getIdentifier();
        if (id.equals("122640") || id.equals("127842") || id.equals("37299")) {
          System.out.println("Id = "+id+", np = "+np+"; isAnySegmActive = "+dmo.isAnySegmActive()+
          "; areAllSegmentsActive = "+dmo.areAllSegmentsActive());
        }
*/
				if (!trLayer.isObjectActive(i) || !dmo.isAnySegmActive()) {
					continue;
				}
				++nTrajAllFilters;
				if (dmo.areAllSegmentsActive()) {
					nPointsAllFilters += np;
					++nSegmentsAllFilters;
				} else {
					boolean seq = false;
					for (int j = 0; j < np; j++)
						if (dmo.isSegmActive(j)) {
							++nPointsAllFilters;
							if (!seq) {
								++nSegmentsAllFilters;
								seq = true;
							}
						} else {
							seq = false;
						}
				}
			}
			bClearFilter.setEnabled(true);
		}
		if (nPointsAllFilters == nPointsTotal) {
			notLine.setText("Points: " + nPointsTotal + "; trajectories: " + nObj + " (no filtering)");
		} else {
			notLine.setText("Points: " + nPointsTotal + "--" + nPointsAfterFilter + "--" + nPointsAllFilters + "; 100%--" + StringUtil.floatToStr(nPointsAfterFilter * 100.0f / nPointsTotal, 1) + "%--"
					+ StringUtil.floatToStr(nPointsAllFilters * 100.0f / nPointsTotal, 1) + "%. Segments: " + nObj + "--" + nSegmentsAfterFilter + "--" + nSegmentsAllFilters + ". Trajectories: " + nObj + "--" + nTrajAfterFilter + "--"
					+ nTrajAllFilters + "; 100%--" + StringUtil.floatToStr(nTrajAfterFilter * 100.0f / nObj, 1) + "%--" + StringUtil.floatToStr(nTrajAllFilters * 100.0f / nObj, 1) + "% (total--local filter--all filters)");
		}
		evCanvas.setBreaksAndFocuserMinMax(breakValues, tSummaryClassColors(), dMin, dMax);
		if (!activatedByOthers) {
			trLayer.propertyChange(new PropertyChangeEvent(this, "TrajSegmFilter", null, null));
		}
		setCursor(cursor);
	}

	protected void applyMaxValueValidityTime() {
		if (tfValidTime == null)
			return;
		String str = tfValidTime.getText();
		if (str == null) {
			evCanvas.setMaxValueValidityTime(-1, '\u0000');
			return;
		}
		long time = -1;
		try {
			time = Long.parseLong(str.trim());
		} catch (Exception e) {
		}
		if (time <= 0) {
			evCanvas.setMaxValueValidityTime(-1, '\u0000');
			return;
		}
		if (chTimeUnit == null || dateComp == null) {
			evCanvas.setMaxValueValidityTime(time, '\u0000');
			return;
		}
		int idx = chTimeUnit.getSelectedIndex();
		evCanvas.setMaxValueValidityTime(time, dateComp[idx]);
		return;
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource().equals(rainbow)) {
			startChangeColors();
			return;
		}
		if (e.getSource().equals(tfBarWidth)) {
			String str = tfBarWidth.getText();
			if (str != null) {
				str = str.trim();
			}
			int k;
			try {
				k = Integer.parseInt(str);
			} catch (NumberFormatException nfe) {
				k = 5;
			}
			if (k < 3 || k > 21) {
				k = 5;
			}
			tfBarWidth.setText("" + k);
			evCanvas.setUnitSize(k);
		}
		String cmd = e.getActionCommand();
		if (cmd == null)
			return;
		if (cmd.equals("full_extent")) {
			focusInterval.showWholeInterval();
		} else if (cmd.equals("segmSelectionChanged")) {
			setSegmFilters(false);
		} else if (cmd.equals("clear_segment_filter")) {
			tLegendCanvas.clearSegmSelection();
			setSegmFilters(false);
		} else if (cmd.equals("apply_valid_time")) {
			applyMaxValueValidityTime();
		} else if (cmd.equalsIgnoreCase("change")) {
			Vector attrIds = new Vector(50, 50), attrNames = new Vector(50, 50);
			ThematicDataItem td = getThematicDataItem();
			if (td != null) {
				for (int i = 0; i < td.getAttrCount(); i++) {
					attrIds.addElement(td.getAttributeId(i));
					attrNames.addElement(td.getAttributeName(i));
				}
			}
			if (attrIds.size() < 1) {
				remove(attrTF.getParent());
				attrTF = null;
				return;
			}
			MultiSelector ms = new MultiSelector(attrNames, true);
			Vector sortAttrIds = evCanvas.getSortAttrIds();
			if (sortAttrIds != null && sortAttrIds.size() > 0) {
				int selIdxs[] = new int[sortAttrIds.size()];
				for (int i = 0; i < sortAttrIds.size(); i++) {
					selIdxs[i] = StringUtil.indexOfStringInVectorIgnoreCase((String) sortAttrIds.elementAt(i), attrIds);
				}
				ms.selectItems(selIdxs);
			}
			Panel p = new Panel(new BorderLayout());
			p.add(new Label("Select the attribute(s) to be used for " + "sorting or grouping:"), BorderLayout.NORTH);
			p.add(ms, BorderLayout.CENTER);
			OKDialog okd = new OKDialog(CManager.getAnyFrame(this), "Select attributes", true);
			okd.addContent(p);
			okd.show();
			if (okd.wasCancelled())
				return;
			int selIdxs[] = ms.getSelectedIndexes();
			if (selIdxs == null) {
				evCanvas.setSortAttrIds(null);
				attrTF.setText("<no attributes selected>");
			} else {
				String str = "";
				if (sortAttrIds != null) {
					sortAttrIds.removeAllElements();
				} else {
					sortAttrIds = new Vector(10, 10);
				}
				for (int i = 0; i < selIdxs.length; i++) {
					int idx = selIdxs[i];
					sortAttrIds.addElement(attrIds.elementAt(idx));
					if (i > 0) {
						str += "; ";
					}
					str += "\"" + (String) attrNames.elementAt(idx) + "\"";
				}
				evCanvas.setSortAttrIds(sortAttrIds);
				attrTF.setText(str);
			}
		} else if (cmd.equals("compute_traj_attr")) {
			setupForNewTrajectoryAttribute();
		} else if (cmd.equals("attach_traj_attr")) {
			attachAttrValuesToPositions();
		} else if (cmd.equals("traj_statistics")) {
			getStatisticsForTrajectories();
		} else if (cmd.equals("event_statistics")) {
			getStatisticsForEvents();
		} else if (cmd.equals("extract_events")) {
			ESDACore core = (ESDACore) supervisor.getSystemSettings().getParameter("core");
			if (core == null)
				return;
			EventExtractor evExt = new EventExtractor();
			evExt.extractEventsFromTrajectories(trLayer, true, evCanvas.getAttrVals(), tInfoCanvas.getAttrName(), core);
		}
	}

	/**
	 * Table containing the position records.
	 * Initially does not exist. Is created when computed attribute values
	 * are attached to position records for the first time.
	 * Used to ensure unique attribute identifiers
	 */
	protected DataTable posTable = null;

	/**
	 * Attaches the computed attribute values to the position records
	 * in the trajectories
	 */
	protected void attachAttrValuesToPositions() {
		double dAttrVals[][] = evCanvas.getAttrVals();
		if (dAttrVals == null || dAttrVals.length < 1)
			return;
		String aName = tInfoCanvas.getAttrName();
		aName = Dialogs.askForStringValue(CManager.getFrame(this), "Attribute name?", aName, null, "Attribute name?", true);
		if (aName == null)
			return;
		if (posTable == null) {
			DataRecord rec = null;
			for (int i = 0; rec == null && i < trLayer.getObjectCount(); i++) {
				Vector track = ((DMovingObject) trLayer.getObject(i)).getTrack();
				if (track != null && track.size() > 0) {
					SpatialEntity spe = (SpatialEntity) track.elementAt(0);
					ThematicDataItem data = spe.getThematicData();
					if (data != null && (data instanceof DataRecord)) {
						rec = (DataRecord) data;
					}
				}
			}
			posTable = new DataTable();
			if (rec != null) {
				for (int i = 0; i < rec.getAttrCount(); i++) {
					posTable.addAttribute(rec.getAttribute(i));
				}
			}
		}
		int aIdx = posTable.getAttrCount();
		posTable.addAttribute(aName, IdMaker.makeId(aName, posTable), AttributeTypes.real);
		posTable.makeUniqueAttrIdentifiers();
		for (int i = 0; i < dAttrVals.length; i++) {
			DMovingObject mObj = (DMovingObject) trLayer.getObject(i);
			Vector track = mObj.getTrack();
			if (track == null || track.size() < 1) {
				continue;
			}
			double d[] = dAttrVals[i];
			for (int j = 0; j < track.size(); j++) {
				SpatialEntity spe = (SpatialEntity) track.elementAt(j);
				ThematicDataItem data = spe.getThematicData();
				DataRecord rec = null;
				if (data != null && (data instanceof DataRecord)) {
					rec = (DataRecord) data;
				}
				if (rec == null) {
					rec = new DataRecord(mObj.getIdentifier() + "_" + (j + 1));
					spe.setThematicData(rec);
				}
				int nOld = rec.getAttrCount();
				rec.setAttrList(posTable.getAttrList());
				for (int k = nOld; k < rec.getAttrCount(); k++) {
					rec.setAttrValue(null, k);
				}
				if (d != null && d.length > j && !Double.isNaN(d[j])) {
					rec.setNumericAttrValue(d[j], aIdx);
				}
			}
		}
	}

	/**
	 * Computes statistics of the values of the current trajectory attribute
	 * taking into account current filter, if any
	 */
	protected void getStatisticsForTrajectories() {
		double dAttrVals[][] = evCanvas.getAttrVals();
		if (dAttrVals == null || dAttrVals.length < 1)
			return;
		DataTable table = null;
		if (trLayer.getThematicData() != null && (trLayer.getThematicData() instanceof DataTable)) {
			table = (DataTable) trLayer.getThematicData();
		}
		if (table == null)
			return;
		String suffix = tInfoCanvas.getAttrName();
/*
    suffix =Dialogs.askForStringValue(CManager.getFrame(this),
      "Suffix for the attribute names?",suffix,
      "New attributes will be added to the table \""+table.getName()+
        "\" describing the trajectories. Their names will start with \"Mean of\", " +
        "\"Min of\", etc. Provide a suitable suffix for the names.",
      "New attributes",true);
    if (suffix ==null) return;
*/
		boolean filtered = false;
		for (int i = 0; i < trLayer.getObjectCount() && !filtered; i++) {
			DMovingObject mobj = (DMovingObject) trLayer.getObject(i);
			DMovingObjectSegmFilterCombiner sfc = mobj.getSegmFilterCombiner();
			if (sfc == null) {
				continue;
			}
			filtered = !sfc.areAllSegmentsActive();
		}
		String filterName = "";
		if (filtered) {
			filterName = Dialogs.askForStringValue(CManager.getFrame(this), "Name (description) of the filter?", filterName, "The statistics will be computed for the points of the trajectories that "
					+ "satisfy the current filter. Provide a suitable name or description of " + "the filter, to be attached to the attribute names.", "Filter name?", true);
		}
		if (filterName == null)
			return;
		String statNames[] = { "N values", "Min", "Max", "Median", "Mean", "St.dev." };
		int aIdx0 = table.getAttrCount();
		for (int i = 0; i < statNames.length; i++) {
			String aName = statNames[i] + " of " + suffix;
			if (filtered) {
				aName += " (" + ((filterName.length() < 1) ? "after filter" : filterName) + ")";
			}
			String souName = aName;
			for (int j = 2; table.findAttrByName(aName) >= 0; j++) {
				aName = souName + " " + j;
			}
			table.addAttribute(aName, IdMaker.makeId(aName, table), (i == 0) ? AttributeTypes.integer : AttributeTypes.real);
		}
		table.makeUniqueAttrIdentifiers();
		DoubleArray dar = new DoubleArray(100, 100);
		int perc[] = { 0, 100, 50 };
		for (int i = 0; i < dAttrVals.length; i++) {
			DMovingObject mObj = (DMovingObject) trLayer.getObject(i);
			double d[] = dAttrVals[i];
			if (d == null || d.length < 1) {
				continue;
			}
			DataRecord rec = (DataRecord) mObj.getData();
			if (rec == null) {
				continue;
			}
			dar.removeAllElements();
			DMovingObjectSegmFilterCombiner sfc = null;
			if (filtered) {
				sfc = mObj.getSegmFilterCombiner();
			}
			for (int j = 0; j < d.length; j++)
				if (!Double.isNaN(d[j]) && (sfc == null || sfc.isSegmentActive(j))) {
					dar.addElement(d[j]);
				}
			rec.setNumericAttrValue(dar.size(), String.valueOf(dar.size()), aIdx0);
			if (dar.size() < 1) {
				continue;
			}
			if (dar.size() < 2) {
				double val = dar.elementAt(0);
				String strVal = String.valueOf(val);
				for (int j = 1; j <= 4; j++) {
					rec.setNumericAttrValue(val, strVal, aIdx0 + j);
				}
				rec.setNumericAttrValue(0, "0", aIdx0 + 5);
				continue;
			}
			double minMaxMed[] = NumValManager.getPercentiles(dar, perc);
			for (int j = 0; j < minMaxMed.length; j++) {
				rec.setNumericAttrValue(minMaxMed[j], aIdx0 + j + 1);
			}
			double mean = NumValManager.getMean(dar);
			rec.setNumericAttrValue(mean, aIdx0 + 4);
			double stDev = NumValManager.getStdD(dar, mean);
			rec.setNumericAttrValue(stDev, aIdx0 + 5);
		}

		int colNs[] = new int[table.getAttrCount() - aIdx0];
		for (int i = 0; i < colNs.length; i++) {
			colNs[i] = aIdx0 + i;
		}
		table.setNiceStringsForNumbers(colNs, false);

		Vector aIds = new Vector(statNames.length, 10);
		for (int i = aIdx0; i < table.getAttrCount(); i++) {
			aIds.addElement(table.getAttributeId(i));
		}
		table.notifyPropertyChange("new_attributes", null, aIds);
		showMessage("Computation finished!", false);
	}

	/**
	 * Creates a choice with texts identifying different time units
	 */
	protected Choice makeTimeUnitsChoice(TimeMoment sampleTime) {
		if (sampleTime == null)
			return null;
		int precIdx = sampleTime.getPrecisionIdx();
		if (precIdx < 0)
			return null;
		Choice ch = new Choice();
		for (int i = 0; i <= precIdx; i++) {
			switch (i) {
			case 0:
				ch.addItem("years");
				break;
			case 1:
				ch.addItem("months");
				break;
			case 2:
				ch.addItem("days");
				break;
			case 3:
				ch.addItem("hours");
				break;
			case 4:
				ch.addItem("minutes");
				break;
			case 5:
				ch.addItem("seconds");
				break;
			}
		}
		ch.select(precIdx);
		return ch;
	}

	/**
	 * Computes statistics of trajectory attribute values around the times of
	 * occurrence of events. Creates new attributes in the table describing events.
	 * Takes into account the current segment filter.
	 */
	protected void getStatisticsForEvents() {
		double dAttrVals[][] = evCanvas.getAttrVals();
		if (dAttrVals == null || dAttrVals.length < 1)
			return;
		Vector<DGeoLayer> evLayers = getLayersWithEvents();
		if (evLayers == null) {
			showMessage("No layers with events found!", true);
			return;
		}
		List list = new List(Math.min(Math.max(2, evLayers.size()), 5));
		for (int i = 0; i < evLayers.size(); i++) {
			list.add(evLayers.elementAt(i).getName());
		}
		list.select(evLayers.size() - 1);
		Panel p = new Panel(new ColumnLayout());
		p.add(new Label("Select the layer with the events:"));
		p.add(list);
		p.add(new Label("Compute statistics of the attribute values for the events"));
		DMovingObject dmo = ((DMovingObject) trLayer.getObject(0));
		TimeMoment sampleTime = dmo.getStartTime();
		Choice chUnits = makeTimeUnitsChoice(sampleTime);
		Panel pp = new Panel(new FlowLayout(FlowLayout.CENTER, 0, 1));
		if (chUnits != null) {
			pp.add(new Label("Time interval, in"));
			pp.add(chUnits);
			pp.add(new Label(":"));
		} else {
			pp.add(new Label("Time interval:"));
		}
		p.add(pp);
		pp = new Panel(new RowLayout());
		pp.add(new Label("from"));
		TextField tfFrom = new TextField("1", 3);
		pp.add(tfFrom);
		Choice chBeforeAfter1 = new Choice();
		chBeforeAfter1.add("before");
		chBeforeAfter1.add("after");
		chBeforeAfter1.select(0);
		pp.add(chBeforeAfter1);
		pp.add(new Label("the event"));
		Choice chStartEnd1 = new Choice();
		chStartEnd1.add("start time");
		chStartEnd1.add("end time");
		chStartEnd1.select(0);
		pp.add(chStartEnd1);
		p.add(pp);
		pp = new Panel(new RowLayout());
		pp.add(new Label("to"));
		TextField tfTo = new TextField("1", 3);
		pp.add(tfTo);
		Choice chBeforeAfter2 = new Choice();
		chBeforeAfter2.add("before");
		chBeforeAfter2.add("after");
		chBeforeAfter2.select(1);
		pp.add(chBeforeAfter2);
		pp.add(new Label("the event"));
		Choice chStartEnd2 = new Choice();
		chStartEnd2.add("start time");
		chStartEnd2.add("end time");
		chStartEnd2.select(0);
		pp.add(chStartEnd2);
		p.add(pp);
		p.add(new Line(false));
		Checkbox cbOnlyActiveEvents = new Checkbox("Consider only active (after filtering) events", true);
		p.add(cbOnlyActiveEvents);
		Checkbox cbOnlyTrEvents = new Checkbox("Consider only the relevant trajectory for each event", true);
		p.add(cbOnlyTrEvents);
		OKDialog ok = new OKDialog(CManager.getFrame(this), "Statistics for events", true);
		ok.addContent(p);
		ok.show();
		if (ok.wasCancelled())
			return;
		int idx = list.getSelectedIndex();
		if (idx < 0)
			return;
		int iStart = -1, iEnd = -1;
		try {
			String str = tfFrom.getText();
			if (str != null) {
				iStart = Integer.parseInt(str.trim());
			}
			str = tfTo.getText();
			if (str != null) {
				iEnd = Integer.parseInt(str.trim());
			}
		} catch (Exception e) {
		}
		if (iStart < 0 || iEnd < 0) {
			showMessage("The time interval is not properly specified!", true);
			return;
		}
		if (chBeforeAfter1.getSelectedIndex() == 0) {
			iStart = -iStart;
		}
		if (chBeforeAfter2.getSelectedIndex() == 0) {
			iEnd = -iEnd;
		}
		if (iEnd < iStart) {
			showMessage("The interval end is earlier than the start!", true);
			return;
		}
		DGeoLayer evLayer = evLayers.elementAt(idx);
		boolean onlyTrEvents = cbOnlyTrEvents.getState(), onlyActive = cbOnlyActiveEvents.getState();
		AttributeDataPortion evTable = null;
		int trIdColN = -1;
		if (onlyTrEvents) {
			evTable = evLayer.getThematicData();
			if (evTable == null) {
				if (!Dialogs.askYesOrNo(CManager.getAnyFrame(this), "The layer with the events has no table. Therefore, it is impossible " + "to select only the relevant trajectory for each event. Compute statistics " + "from all trajectories?",
						"Relevant trajectories"))
					return;
			} else {
				trIdColN = getColumnWithTrajIds(evTable);
				if (trIdColN < 0)
					return;
			}
			if (trIdColN < 0) {
				onlyTrEvents = false;
			}
		}

		char unit = 's';
		if (chUnits != null) {
			switch (chUnits.getSelectedIndex()) {
			case 0:
				unit = 'y';
				break;
			case 1:
				unit = 'm';
				break;
			case 2:
				unit = 'd';
				break;
			case 3:
				unit = 'h';
				break;
			case 4:
				unit = 't';
				break;
			case 5:
				unit = 's';
				break;
			}
		}
		boolean fromStart1 = chStartEnd1.getSelectedIndex() == 0, fromStart2 = chStartEnd2.getSelectedIndex() == 0;
		DataTable dTable = null;
		if (evTable != null && (evTable instanceof DataTable)) {
			dTable = (DataTable) evTable;
		} else {
			ESDACore core = (ESDACore) supervisor.getSystemSettings().getParameter("core");
			if (core == null) {
				showMessage("No access to the system core!", true);
				return;
			}
			dTable = new DataTable();
			dTable.setName(evLayer.getName());
			DataLoader dLoader = core.getDataLoader();
			int tblN = dLoader.addTable(dTable);
			dLoader.setLink(evLayer, tblN);
			evLayer.setLinkedToTable(true);
		}
		String suffix = tInfoCanvas.getAttrName();
/*
    suffix =Dialogs.askForStringValue(CManager.getFrame(this),
      "Suffix for the attribute names?",suffix,
      "New attributes will be added to the table \""+dTable.getName()+
        "\" describing the events. Their names will start with \"Mean of\", " +
        "\"Min of\", etc. Provide a suitable suffix for the names.",
      "New attributes",true);
    if (suffix ==null) return;
*/
		boolean filtered = false;
		for (int i = 0; i < trLayer.getObjectCount() && !filtered; i++) {
			DMovingObject mobj = (DMovingObject) trLayer.getObject(i);
			DMovingObjectSegmFilterCombiner sfc = mobj.getSegmFilterCombiner();
			if (sfc == null) {
				continue;
			}
			filtered = !sfc.areAllSegmentsActive();
		}
		String timeTxt = String.valueOf(iStart) + " " + chUnits.getSelectedItem() + " from " + ((fromStart1) ? "start" : "end") + " - " + String.valueOf(iEnd) + " " + chUnits.getSelectedItem() + " from " + ((fromStart2) ? "start" : "end");
		timeTxt = Dialogs.askForStringValue(CManager.getFrame(this), "Description of the time interval?", timeTxt, null, "Time interval description?", true);
		if (timeTxt != null) {
			suffix += "; " + timeTxt;
		}
		String statNames[] = { "N values", "Min", "Max", "Median", "Mean", "St.dev." };
		int aIdx0 = dTable.getAttrCount();
		for (int i = 0; i < statNames.length; i++) {
			String aName = statNames[i] + " of " + suffix;
			String souName = aName;
			for (int j = 2; dTable.findAttrByName(aName) >= 0; j++) {
				aName = souName + " " + j;
			}
			dTable.addAttribute(aName, IdMaker.makeId(aName, dTable), (i == 0) ? AttributeTypes.integer : AttributeTypes.real);
		}
		dTable.makeUniqueAttrIdentifiers();
		DoubleArray dar = new DoubleArray(100, 100);
		int perc[] = { 0, 100, 50 };
		for (int i = 0; i < evLayer.getObjectCount(); i++)
			if (!onlyActive || evLayer.isObjectActive(i)) {
				DGeoObject evt = evLayer.getObject(i);
				TimeReference tref = evt.getTimeReference();
				if (tref == null || tref.getValidFrom() == null) {
					continue;
				}
				TimeMoment t1 = tref.getValidFrom(), t2 = tref.getValidUntil();
				if (t2 == null) {
					t2 = t1;
				}
				t1 = (fromStart1) ? t1.getCopy() : t2.getCopy();
				t2 = (fromStart2) ? t1.getCopy() : t2.getCopy();
				char prec = t1.getPrecision();
				t1.setPrecision(unit);
				t2.setPrecision(unit);
				t1.add(iStart);
				t2.add(iEnd);
				t1.setPrecision(prec);
				t2.setPrecision(prec);
				TimeReference interval = new TimeReference(t1, t2);
				int recN = dTable.indexOf(evt.getIdentifier());
				DataRecord rec = (recN >= 0) ? dTable.getDataRecord(recN) : null;
				if (rec == null) {
					rec = new DataRecord(evt.getIdentifier(), evt.getName());
					dTable.addDataRecord(rec);
				}
				dar.removeAllElements();
				String trId = evt.getData().getAttrValueAsString(trIdColN);
				for (int j = 0; j < dAttrVals.length; j++) {
					DMovingObject mObj = (DMovingObject) trLayer.getObject(j);
					if (!onlyTrEvents || trId.equals(mObj.getIdentifier())) {
						double d[] = dAttrVals[j];
						if (d == null || d.length < 1) {
							continue;
						}
						DMovingObjectSegmFilterCombiner sfc = null;
						if (filtered) {
							sfc = mObj.getSegmFilterCombiner();
						}
						for (int k = 0; k < d.length; k++)
							if (!Double.isNaN(d[k]) && (sfc == null || sfc.isSegmentActive(k))) {
								TimeReference posT = mObj.getPositionTime(k);
								if (posT == null) {
									continue;
								}
								if (interval.isValid(posT.getValidFrom(), posT.getValidUntil())) {
									dar.addElement(d[k]);
								}
							}
						if (onlyTrEvents) {
							break;
						}
					}
				}
				rec.setNumericAttrValue(dar.size(), String.valueOf(dar.size()), aIdx0);
				if (dar.size() < 1) {
					continue;
				}
				if (dar.size() < 2) {
					double val = dar.elementAt(0);
					String strVal = String.valueOf(val);
					for (int j = 1; j <= 4; j++) {
						rec.setNumericAttrValue(val, strVal, aIdx0 + j);
					}
					rec.setNumericAttrValue(0, "0", aIdx0 + 5);
					continue;
				}
				double minMaxMed[] = NumValManager.getPercentiles(dar, perc);
				for (int j = 0; j < minMaxMed.length; j++) {
					rec.setNumericAttrValue(minMaxMed[j], aIdx0 + j + 1);
				}
				double mean = NumValManager.getMean(dar);
				rec.setNumericAttrValue(mean, aIdx0 + 4);
				double stDev = NumValManager.getStdD(dar, mean);
				rec.setNumericAttrValue(stDev, aIdx0 + 5);
			}

		int colNs[] = new int[dTable.getAttrCount() - aIdx0];
		for (int i = 0; i < colNs.length; i++) {
			colNs[i] = aIdx0 + i;
		}
		dTable.setNiceStringsForNumbers(colNs, false);

		Vector aIds = new Vector(statNames.length, 10);
		for (int i = aIdx0; i < dTable.getAttrCount(); i++) {
			aIds.addElement(dTable.getAttributeId(i));
		}
		dTable.notifyPropertyChange("new_attributes", null, aIds);
		showMessage("Computation finished!", false);
	}

	/**
	 * Listens to changes of the time focuser
	 */
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getPropertyName().equals("current_moment") && (e.getNewValue() instanceof TimePositionNotifier)) {
			TimePositionNotifier tpn = (TimePositionNotifier) e.getNewValue();
			//TimePositionNotifier tpn=(TimePositionNotifier)e.getSource();
			String str = String.valueOf(tpn.getMouseTime());
			if (e.getOldValue() instanceof TrajTimeGraphCanvas && tpn.lastId != null) {
				str = "<" + StringUtil.floatToStr((float) tpn.lastValue, (float) dMin, (float) dMax) + "> \"" + tpn.lastId + "\" t=" + tpn.getMouseTime() + " @ " + tpn.lastDataTime
				/* +" ["+tpn.lastMapX+","+tpn.lastMapY+"]" */;
			}
			tInfoCanvas.setTimeRef((tpn.getMouseTime() == null) ? "" : str);
			if (supervisor != null) {
				SystemUI ui = supervisor.getUI();
				if (ui != null)
					if (tpn.getMouseX() < 0 || tpn.lastId == null) {
						ui.erasePositionMarksOnMaps();
					} else {
						ui.markPositionOnMaps((float) tpn.lastMapX, (float) tpn.lastMapY);
					}
			}
			return;
		}
		if (e.getSource() instanceof FocusInterval) {
			if (e.getPropertyName().equalsIgnoreCase("current_interval")) {
				TimeMoment t1 = (TimeMoment) e.getOldValue(), t2 = (TimeMoment) e.getNewValue();
				if (t2.subtract(t1) < 1) {
					t1 = t1.getCopy();
					t2 = t1.getCopy();
					t2.add(1);
					focusInterval.setCurrInterval(t1, t2);
					return;
				}
				evCanvas.setFocusInterval(t1, t2);
				applyMaxValueValidityTime();
				TimeMoment start = evCanvas.getDataStart(), end = evCanvas.getDataEnd();
				bStartEnd.setEnabled(start.compareTo(t1) < 0 || end.compareTo(t2) > 0);
			}
			return;
		}
		if (e.getPropertyName().equalsIgnoreCase("ObjectFilter")) {
			if (this.equals(e.getNewValue()))
				return;
			//System.out.println("* cross-filtering, src="+this);
			setSegmFilters(true);
		}
	}

	/**
	 * Invoked when the component's size changes.
	 */
	public void componentResized(ComponentEvent e) {
		Dimension sps = scp.getSize(), cs = evCanvas.getSize();
		int w = sps.width - scp.getVScrollbarWidth() - 8;
		evCanvas.setSize(w, cs.height);
		cs = tLabCanvas.getSize();
		tLabCanvas.setSize(w, cs.height);
		//tEventCanvas.setSize(w,cs.height);
		evCanvas.invalidate();
		tLabCanvas.invalidate();
		//tEventCanvas.invalidate();
		scp.invalidate();
		CManager.validateAll(this);
	}

	/**
	 * Invoked when the component's position changes.
	 */
	public void componentMoved(ComponentEvent e) {
	}

	/**
	 * Invoked when the component has been made visible.
	 */
	public void componentShown(ComponentEvent e) {
	}

	/**
	 * Invoked when the component has been made invisible.
	 */
	public void componentHidden(ComponentEvent e) {
	}

	/**
	 * Attributes that can be computed for points of trajectories
	 */
	public static String tr_point_attributes[] = { "Cumulative path length", "Remaining trip length", "Path length in time interval", "Speed", "Direction", "Turn", "Distance to start", "Distance to end", "Distance to point defined in the table",
			"Distance to selected object(s)", "Distance to selected trajectories", "Spatial distance to events", "Temporal distance to events", "Temporal distance to time moment defined in the table", "Minimal distance to past points",
			"Date/time component", "Existing position-related attribute" };

	/**
	 * Invoked when data are replaced: new attribute of the trajectories is selected for display
	 */
	public void setupForNewTrajectoryAttribute() {
		int sel = chTrajAttr.getSelectedIndex();
		// acquire time and distance units
		DMovingObject dmo = ((DMovingObject) trLayer.getObject(0));
		sUnits = (dmo.isGeographic()) ? "km" : "distance units";
		TimeMoment tm = ((SpatialEntity) dmo.getTrack().elementAt(0)).getTimeReference().getValidFrom();
		boolean isPhysicalTime = (tm instanceof Date) ? true : false;
		// clear segment filters
		for (int i = 0; i < trLayer.getObjectCount(); i++) {
			((DMovingObject) trLayer.getObject(i)).clearFilter(this);
		}
		tLegendCanvas.clearSegmSelection();
		// compute new values
		double d[][] = new double[trLayer.getObjectCount()][];
		String attrName = chTrajAttr.getSelectedItem();
		whatIsFiltered = DMovingObjectSegmFilter.FilterForPoints;
		int mStep = (d.length > 1000) ? 100 : (d.length > 200) ? 50 : 25;
		switch (sel) {
		case 0: //Cumulative path length
			tInfoCanvas.setAttrName(attrName + " (" + sUnits + ")");
			for (int i = 0; i < d.length; i++) {
				d[i] = ((DMovingObject) trLayer.getObject(i)).getDistancesCumulative();
			}
			whatIsFiltered = DMovingObjectSegmFilter.FilterForPoints;
			break;
		case 1: //Remaining trip length
			tInfoCanvas.setAttrName(attrName + " (" + sUnits + ")");
			for (int i = 0; i < d.length; i++) {
				d[i] = ((DMovingObject) trLayer.getObject(i)).getRemainingTripDistances();
			}
			whatIsFiltered = DMovingObjectSegmFilter.FilterForPoints;
			break;
		case 2: //Path length in time interval
			if (!computePathLengthOnInterval(d)) {
				chTrajAttr.select(lastComputedAttr);
				return;
			}
			tInfoCanvas.setAttrName(attrName + " (" + sUnits + ")");
			whatIsFiltered = DMovingObjectSegmFilter.FilterForPoints;
			break;
		case 3: //Speed
			if (isPhysicalTime)
				if (tm.getPrecision() == 's' || tm.getPrecision() == 't' || tm.getPrecision() == 'h') {
					sUnits += "/h";
				} else {
					sUnits += "/" + tm.getPrecision();
				}
			else {
				sUnits += " / time units";
			}
			tInfoCanvas.setAttrName(attrName + " (" + sUnits + ")");
			for (int i = 0; i < d.length; i++) {
				d[i] = ((DMovingObject) trLayer.getObject(i)).getSpeeds();
			}
			whatIsFiltered = DMovingObjectSegmFilter.FilterForSegments;
			break;
		case 4: //Direction
			tInfoCanvas.setAttrName(attrName + " (degree, N=0)");
			for (int i = 0; i < d.length; i++) {
				d[i] = ((DMovingObject) trLayer.getObject(i)).getDirections();
			}
			whatIsFiltered = DMovingObjectSegmFilter.FilterForPoints;
			break;
		case 5: //Turn
			tInfoCanvas.setAttrName(attrName + " (degree; >0=right)");
			for (int i = 0; i < d.length; i++) {
				double dir[] = ((DMovingObject) trLayer.getObject(i)).getDirections();
				if (dir == null) {
					continue;
				}
				d[i] = new double[dir.length];
				if (dir.length < 3) {
					for (int j = 0; j < d[i].length; j++) {
						d[i][j] = Double.NaN;
					}
				} else {
					for (int j = 1; j < dir.length - 1; j++)
						if (Double.isNaN(dir[j - 1]) || Double.isNaN(dir[j])) {
							d[i][j] = Double.NaN;
						} else {
							d[i][j] = dir[j] - dir[j - 1];
							if (d[i][j] > 180) {
								d[i][j] -= 360;
							} else if (d[i][j] < -180) {
								d[i][j] += 360;
							}
						}
					d[i][0] = d[i][d[i].length - 1] = Double.NaN;
				}
			}
			whatIsFiltered = DMovingObjectSegmFilter.FilterForPoints;
			break;
		case 6: //Distance to start
			tInfoCanvas.setAttrName(attrName + " (" + sUnits + ")");
			for (int i = 0; i < d.length; i++) {
				d[i] = ((DMovingObject) trLayer.getObject(i)).getDistancesToStart();
			}
			whatIsFiltered = DMovingObjectSegmFilter.FilterForPoints;
			break;
		case 7: //Distance to end
			tInfoCanvas.setAttrName(attrName + " (" + sUnits + ")");
			for (int i = 0; i < d.length; i++) {
				d[i] = ((DMovingObject) trLayer.getObject(i)).getDistancesToEnd();
			}
			whatIsFiltered = DMovingObjectSegmFilter.FilterForPoints;
			break;
		case 8: { //Distance to point defined in the table
			DataTable tbl = (DataTable) trLayer.getThematicData();
			if (tbl == null)
				return;
			IntArray colNs = new IntArray(tbl.getAttrCount(), 10);
			List listX = new List(Math.min(tbl.getAttrCount(), 10)), listY = new List(Math.min(tbl.getAttrCount(), 10));
			for (int i = 0; i < tbl.getAttrCount(); i++) {
				Attribute at = tbl.getAttribute(i);
				if (at.isNumeric()) {
					colNs.addElement(i);
					listX.add(at.getName());
					listY.add(at.getName());
				}
			}
			if (colNs.size() < 2) {
				showMessage("The table must have at least 2 numeric columns!", true);
				return;
			}
			Panel p = new Panel(new GridLayout(1, 2));
			Panel pp = new Panel(new BorderLayout());
			pp.add(new Label("X coordinate:"), BorderLayout.NORTH);
			pp.add(listX, BorderLayout.CENTER);
			p.add(pp);
			pp = new Panel(new BorderLayout());
			pp.add(new Label("Y coordinate:"), BorderLayout.NORTH);
			pp.add(listY, BorderLayout.CENTER);
			p.add(pp);
			pp = new Panel(new ColumnLayout());
			pp.add(new Label("Computing distances to points", Label.CENTER));
			pp.add(new Label("Specify the table columns containing the coordinates of the points:"));
			pp.add(p);
			OKDialog dia = new OKDialog(CManager.getAnyFrame(this), "Point layer", true);
			dia.addContent(p);
			dia.show();
			if (dia.wasCancelled()) {
				chTrajAttr.select(lastComputedAttr);
				return;
			}
			int iX = listX.getSelectedIndex(), iY = listY.getSelectedIndex();
			if (iX < 0 || iY < 0) {
				showMessage("The columns have not been selected!", true);
				return;
			}
			int cX = colNs.elementAt(iX), cY = colNs.elementAt(iY);
			showMessage("Computing...", false);
			for (int i = 0; i < d.length; i++) {
				DMovingObject mobj = (DMovingObject) trLayer.getObject(i);
				ThematicDataItem rec = mobj.getData();
				if (rec == null) {
					continue;
				}
				double x = rec.getNumericAttrValue(cX);
				if (Double.isNaN(x)) {
					continue;
				}
				double y = rec.getNumericAttrValue(cY);
				if (Double.isNaN(y)) {
					continue;
				}
				RealPoint pt = new RealPoint((float) x, (float) y);
				d[i] = mobj.getDistancesToPoint(pt);
				if ((i + 1) % mStep == 0) {
					showMessage((i + 1) + " trajectories processed", false);
				}
			}
			showMessage("Computing finished!", false);
			tInfoCanvas.setAttrName("Distance to " + tbl.getAttributeName(cX) + "," + tbl.getAttributeName(cY) + " (" + sUnits + ")");
			whatIsFiltered = DMovingObjectSegmFilter.FilterForPoints;
			break;
		}
		case 9: //Distance to selected object(s)
			Vector<DGeoObject> vdgo = GeoObjectsSelector.selectGeoObjects(supervisor, Geometry.undefined, "Reference objects for computing distances");
			if (vdgo == null || vdgo.size() == 0) {
				chTrajAttr.select(lastComputedAttr);
				return;
			}
			String layerName = GeoObjectsSelector.lastSelectedLayer.getName();
			boolean objHaveTimes = false;
			for (int i = 0; i < vdgo.size() && !objHaveTimes; i++) {
				TimeReference tr = vdgo.elementAt(i).getTimeReference();
				objHaveTimes = tr != null && tr.getValidFrom() != null;
			}
			if (objHaveTimes && Dialogs.askYesOrNo(CManager.getFrame(this), "Treat the objects as events (= take into account their times)?", "Distances to events?")) {
				int ut[] = askTimeIntervalLength(dmo.getStartTime(), "Tolerance:", "Set time tolerance");
				char unit = (char) ut[0];
				int tolerance = ut[1];
				tInfoCanvas.setAttrName("Distance to " + vdgo.size() + " event(s) from " + layerName + " (" + sUnits + ")");
				long tstart = System.currentTimeMillis();
				showMessage("Computing...", false);
				for (int i = 0; i < d.length; i++) {
					d[i] = ((DMovingObject) trLayer.getObject(i)).getDistancesToEvents(vdgo, tolerance, unit);
					if ((i + 1) % mStep == 0) {
						showMessage((i + 1) + " trajectories processed", false);
					}
				}
				showMessage("Computing finished!", false);
				System.out.println("* Computing distances to " + vdgo.size() + " events: elapsed time " + StringUtil.floatToStr((System.currentTimeMillis() - tstart) / 1000f, 3) + " (s)");
			} else {
				tInfoCanvas.setAttrName("Distance to " + vdgo.size() + " object(s) from " + layerName + " (" + sUnits + ")");
				long tstart = System.currentTimeMillis();
				showMessage("Computing...", false);
				for (int i = 0; i < d.length; i++) {
					d[i] = ((DMovingObject) trLayer.getObject(i)).getDistancesToObjects(vdgo);
					if ((i + 1) % mStep == 0) {
						showMessage((i + 1) + " trajectories processed", false);
					}
				}
				showMessage("Computing finished!", false);
				System.out.println("* Computing distances to " + vdgo.size() + " objects: elapsed time " + StringUtil.floatToStr((System.currentTimeMillis() - tstart) / 1000f, 3) + " (s)");
			}
			whatIsFiltered = DMovingObjectSegmFilter.FilterForPoints;
			break;
		case 10: //Distance to selected trajectories
			vdgo = GeoObjectsSelector.selectGeoObjects(supervisor, Geometry.line, "Reference trajectories for computing distances");
			if (vdgo == null || vdgo.size() == 0) {
				chTrajAttr.select(lastComputedAttr);
				return;
			}
			layerName = GeoObjectsSelector.lastSelectedLayer.getName();
			tInfoCanvas.setAttrName("Distance to " + vdgo.size() + " trajectories from " + layerName + " (" + sUnits + ")");
			int ut[] = askTimeIntervalLength(dmo.getStartTime(), "Tolerance:", "Set time tolerance");
			char unit = (char) ut[0];
			int tolerance = ut[1];
			long tstart = System.currentTimeMillis();
			showMessage("Computing...", false);
			for (int i = 0; i < d.length; i++) {
				d[i] = ((DMovingObject) trLayer.getObject(i)).getDistancesToTrajectories(vdgo, tolerance, unit);
				if ((i + 1) % mStep == 0) {
					showMessage((i + 1) + " trajectories processed", false);
				}
			}
			showMessage("Computing finished!", false);
			System.out.println("* Computing distances to " + vdgo.size() + " trajectories: elapsed time " + StringUtil.floatToStr((System.currentTimeMillis() - tstart) / 1000f, 3) + " (s)");
			whatIsFiltered = DMovingObjectSegmFilter.FilterForPoints;
			break;
		case 11: //Spatial distance to events
			if (!computeSpatialDistancesToEvents(d)) {
				chTrajAttr.select(lastComputedAttr);
				return;
			}
			whatIsFiltered = DMovingObjectSegmFilter.FilterForPoints;
			break;
		case 12: //Temporal distance to events
			if (!computeTemporalDistancesToEvents(d)) {
				chTrajAttr.select(lastComputedAttr);
				return;
			}
			whatIsFiltered = DMovingObjectSegmFilter.FilterForPoints;
			break;
		case 13: //Temporal distance to time moment defined in the table
			if (!computeTemporalDistancesToTimeMoments(d)) {
				chTrajAttr.select(lastComputedAttr);
				return;
			}
			whatIsFiltered = DMovingObjectSegmFilter.FilterForPoints;
			break;
		case 14: //Minimal distance to past points
			Panel p = new Panel(new ColumnLayout());
			Panel pp = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
			TextField tf = new TextField("1", 5);
			pp.add(new Label("Min travel distance (" + sUnits + "):"));
			pp.add(tf);
			p.add(pp);
			OKDialog ok = new OKDialog(CManager.getFrame(this), "Set travel distance threshold", true);
			ok.addContent(p);
			ok.show();
			if (ok.wasCancelled()) {
				chTrajAttr.select(lastComputedAttr);
				return;
			}
			float value = 1f;
			try {
				value = Integer.valueOf(tf.getText()).intValue();
			} catch (NumberFormatException nfe) {
				value = 1f;
			}
			tInfoCanvas.setAttrName(attrName + " after " + value + " " + sUnits + " travel (" + sUnits + ")");
			value *= 1000f;
			showMessage("Computing...", false);
			for (int i = 0; i < d.length; i++) {
				d[i] = ((DMovingObject) trLayer.getObject(i)).getDistancesToPast(value);
				if ((i + 1) % mStep == 0) {
					showMessage((i + 1) + " trajectories processed", false);
				}
			}
			showMessage("Computing finished!", false);
			whatIsFiltered = DMovingObjectSegmFilter.FilterForPoints;
			break;
		case 15: //Date/time component
			if (!getDateComponent(d)) {
				chTrajAttr.select(lastComputedAttr);
				return;
			}
			whatIsFiltered = DMovingObjectSegmFilter.FilterForPoints;
			break;
		case 16: { //Existing position-related attribute
			Vector track = null;
			for (int i = 0; track == null && i < trLayer.getObjectCount(); i++) {
				track = ((DMovingObject) trLayer.getObject(i)).getTrack();
				if (track != null && track.size() < 1) {
					track = null;
				}
			}
			SpatialEntity spe = (SpatialEntity) track.elementAt(0);
			ThematicDataItem data = spe.getThematicData();
			if (data == null || data.getAttrCount() < 1) {
				chTrajAttr.select(lastComputedAttr);
				Dialogs.showMessage(CManager.getAnyFrame(this), "The position records have no attributes!", "No attributes!");
				return;
			}
			List aList = new List(5);
			IntArray aIdxs = new IntArray(data.getAttrCount(), 1);
			for (int i = 0; i < data.getAttrCount(); i++)
				if (AttributeTypes.isNumericType(data.getAttrType(i))) {
					aList.add(data.getAttributeName(i));
					aIdxs.addElement(i);
				}
			if (aIdxs.size() < 1) {
				chTrajAttr.select(lastComputedAttr);
				Dialogs.showMessage(CManager.getAnyFrame(this), "There are no numeric attributes attached to the positions!", "No numeric attributes!");
				return;
			}
			aList.select(0);
			p = new Panel(new BorderLayout());
			p.add(new Label("Select the attribute:"), BorderLayout.NORTH);
			p.add(aList, BorderLayout.CENTER);
			ok = new OKDialog(CManager.getFrame(this), "Select the attribute", true);
			ok.addContent(p);
			ok.show();
			if (ok.wasCancelled() || aList.getSelectedIndex() < 0) {
				chTrajAttr.select(lastComputedAttr);
				return;
			}
			tInfoCanvas.setAttrName(aList.getSelectedItem() + " from position records");
			int colN = aIdxs.elementAt(aList.getSelectedIndex());
			for (int i = 0; i < d.length; i++) {
				track = ((DMovingObject) trLayer.getObject(i)).getTrack();
				if (track == null || track.size() < 1) {
					d[i] = null;
					continue;
				}
				d[i] = new double[track.size()];
				for (int j = 0; j < track.size(); j++) {
					data = ((SpatialEntity) track.elementAt(j)).getThematicData();
					if (data == null) {
						d[i][j] = Double.NaN;
					} else {
						d[i][j] = data.getNumericAttrValue(colN);
					}

				}
			}
			whatIsFiltered = DMovingObjectSegmFilter.FilterForPoints;
		}
			break;
		}
		lastComputedAttr = sel;
		dMin = Double.NaN;
		dMax = Double.NaN;
		for (double[] element : d)
			if (element != null) {
				for (int j = 0; j < element.length; j++) {
					if (Double.isNaN(dMin) || element[j] < dMin) {
						dMin = element[j];
					}
					if (Double.isNaN(dMax) || element[j] > dMax) {
						dMax = element[j];
					}
				}
			}
		// allow user to set the interval of interest
		if (atStart) {
			atStart = false;
		} else {
			TextField tfMin = new TextField(StringUtil.doubleToStr(dMin, dMin, dMax)), tfMax = new TextField(StringUtil.doubleToStr(dMax, dMin, dMax));
			double d01 = Double.NaN, d02 = Double.NaN;
			try {
				d01 = Double.valueOf(tfMin.getText());
				d02 = Double.valueOf(tfMax.getText());
			} catch (NumberFormatException nfo) {
			}
			Panel p = new Panel(new ColumnLayout());
			p.add(new Label(tInfoCanvas.getAttrName()));
			p.add(new Line(false));
			p.add(new Label("Set the range of values you are interested in:"));
			Panel pp = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
			pp.add(new Label("Min:"));
			pp.add(tfMin);
			p.add(pp);
			pp = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
			pp.add(new Label("Max:"));
			pp.add(tfMax);
			p.add(pp);
			p.add(new Label("Values outside the range will be replaced by NaNs"));
			p.add(new Line(false));
			OKDialog ok = new OKDialog(CManager.getAnyFrame(), "Set value range", true);
			ok.addContent(p);
			ok.show();
			if (!ok.wasCancelled()) {
				double d1 = Double.NaN, d2 = Double.NaN;
				try {
					d1 = Double.valueOf(tfMin.getText());
					d2 = Double.valueOf(tfMax.getText());
				} catch (NumberFormatException nfo) {
				}
				if (!Double.isNaN(d1) && !Double.isNaN(d2) && d1 <= d2 && ((d1 > d01 && d1 < d02) || (d2 > d01 && d2 < d02))) {
					boolean changed = false;
					if (d1 > d01) {
						dMin = d1;
						changed = true;
					}
					if (d2 < d02) {
						dMax = d2;
						changed = true;
					}
					if (changed) {
						for (int i = 0; i < d.length; i++)
							if (d[i] != null) {
								for (int j = 0; j < d[i].length; j++)
									if (d[i][j] < dMin || d[i][j] > dMax) {
										d[i][j] = Double.NaN;
									}
							}
						//find the actual dMin and dMax
						dMin = dMax = Double.NaN;
						for (double[] element : d)
							if (element != null) {
								for (int j = 0; j < element.length; j++) {
									if (Double.isNaN(dMin) || element[j] < dMin) {
										dMin = element[j];
									}
									if (Double.isNaN(dMax) || element[j] > dMax) {
										dMax = element[j];
									}
								}
							}
					}
				}
			}
		}
		//
		evCanvas.setAttrVals(d);
		setupSummary();
		//evCanvas.setBreaksAndFocuserMinMax(breakValues,tSummaryClassColors(),dMin,dMax);
		tFocuserCanvas.setFocuserMinMax(dMin, dMax);
		tLegendCanvas.setFocuserMinMax(dMin, dMax);
	}

	protected boolean computePathLengthOnInterval(double d[][]) {
		if (d == null)
			return false;
		DMovingObject dmo = ((DMovingObject) trLayer.getObject(0));
		int ut[] = askTimeIntervalLength(dmo.getStartTime(), "Time interval length:", "Interval length");
		char unit = (char) ut[0];
		int length = ut[1];
		String str = Date.getTextForTimeSymbolInPlural(unit);
		if (str == null) {
			str = "";
		}
		showMessage("Computing...", false);
		int mStep = (d.length > 1000) ? 100 : (d.length > 200) ? 50 : 25;
		for (int i = 0; i < d.length; i++) {
			d[i] = ((DMovingObject) trLayer.getObject(i)).getPathLengthInInterval(length, unit);
			if ((i + 1) % mStep == 0) {
				showMessage((i + 1) + " trajectories processed", false);
			}
		}
		showMessage("Computing finished!", false);
		tInfoCanvas.setAttrName(chTrajAttr.getSelectedItem() + ": " + length + str + " (" + sUnits + ")");
		return true;
	}

	protected Vector<DGeoLayer> getLayersWithEvents() {
		ESDACore core = (ESDACore) supervisor.getSystemSettings().getParameter("core");
		if (core == null)
			return null;
		LayerManager lman = core.getUI().getCurrentMapViewer().getLayerManager();
		if (lman == null)
			return null;
		Vector<DGeoLayer> evLayers = new Vector<DGeoLayer>(lman.getLayerCount(), 1);
		for (int i = 0; i < lman.getLayerCount(); i++) {
			GeoLayer layer = lman.getGeoLayer(i);
			if (!(layer instanceof DGeoLayer)) {
				continue;
			}
			if (layer.getObjectCount() > 0 && layer.hasTimeReferences()) {
				if (layer.getObjectAt(0) instanceof DMovingObject) {
					continue;
				}
				evLayers.addElement((DGeoLayer) layer);
			}
		}
		if (evLayers.size() < 1)
			return null;
		return evLayers;
	}

	protected int getColumnWithTrajIds(AttributeDataPortion evTable) {
		if (evTable == null)
			return -1;
		List list = new List(10);
		for (int i = 0; i < evTable.getAttrCount(); i++) {
			list.add(evTable.getAttributeName(i));
		}
		int aIdx = evTable.getAttrIndex(EventExtractor.trajIdColId);
		if (aIdx >= 0) {
			list.select(aIdx);
		}
		Panel p = new Panel(new ColumnLayout());
		TextCanvas tc = new TextCanvas();
		tc.setText("For the selection of relevant events for each trajectory, " + "some column in the table describing the events must contain the identifiers " + "of the relevant trajectories");
		p.add(tc);
		p.add(new Label("Specify the column with the identifiers of the trajectories:"));
		p.add(list);
		OKDialog ok = new OKDialog(CManager.getFrame(this), "Relevant events", true);
		ok.addContent(p);
		ok.show();
		if (ok.wasCancelled())
			return -1;
		return list.getSelectedIndex();
	}

	protected boolean computeSpatialDistancesToEvents(double d[][]) {
		if (d == null)
			return false;
		Vector<DGeoLayer> evLayers = getLayersWithEvents();
		if (evLayers == null) {
			showMessage("No layers with events found!", true);
			return false;
		}
		List list = new List(Math.min(Math.max(2, evLayers.size()), 5));
		for (int i = 0; i < evLayers.size(); i++) {
			list.add(evLayers.elementAt(i).getName());
		}
		list.select(evLayers.size() - 1);
		Panel p = new Panel(new ColumnLayout());
		p.add(new Label("Select the layer with the events:"));
		p.add(list);
		p.add(new Label("Compute spatial distances to the nearest events"));
		Checkbox cbPast = new Checkbox("in the past", true);
		p.add(cbPast);
		Checkbox cbFuture = new Checkbox("in the future", false);
		p.add(cbFuture);
		p.add(new Line(false));
		Checkbox cbOnlyActiveEvents = new Checkbox("Consider only active (after filtering) events", true);
		p.add(cbOnlyActiveEvents);
		Checkbox cbOnlyTrEvents = new Checkbox("Select only relevant events for each trajectory", true);
		p.add(cbOnlyTrEvents);
		Panel pp = new Panel(new BorderLayout());
		pp.add(new Label("Maximum distance to an event in time:"), BorderLayout.WEST);
		TextField tf = new TextField(3);
		pp.add(tf, BorderLayout.CENTER);
		DMovingObject dmo = ((DMovingObject) trLayer.getObject(0));
		TimeMoment sampleTime = dmo.getStartTime();
		Choice ch = makeTimeUnitsChoice(sampleTime);
		if (ch != null) {
			pp.add(ch, BorderLayout.EAST);
		}
		p.add(pp);
		OKDialog ok = new OKDialog(CManager.getFrame(this), "Spatial distance to events", true);
		ok.addContent(p);
		ok.show();
		if (ok.wasCancelled())
			return false;
		int idx = list.getSelectedIndex();
		if (idx < 0)
			return false;
		DGeoLayer evLayer = evLayers.elementAt(idx);
		boolean past = cbPast.getState(), future = cbFuture.getState(), onlyTrEvents = cbOnlyTrEvents.getState(), onlyActive = cbOnlyActiveEvents.getState();
		AttributeDataPortion evTable = null;
		int trIdColN = -1;
		if (onlyTrEvents) {
			evTable = evLayer.getThematicData();
			if (evTable == null) {
				if (!Dialogs.askYesOrNo(CManager.getAnyFrame(this), "The layer with the events has no table. Therefore, it is impossible " + "to select only relevant events for each trajectory. Compute distances " + "to any events?",
						"Relevant events"))
					return false;
			} else {
				trIdColN = getColumnWithTrajIds(evTable);
				if (trIdColN < 0)
					return false;
			}
			if (trIdColN < 0) {
				onlyTrEvents = false;
			}
		}

		int maxTLen = -1;
		try {
			String str = tf.getText();
			if (str != null) {
				maxTLen = Integer.parseInt(str.trim());
			}
		} catch (Exception e) {
		}
		char unit = 's';
		if (maxTLen >= 0 && ch != null) {
			switch (ch.getSelectedIndex()) {
			case 0:
				unit = 'y';
				break;
			case 1:
				unit = 'm';
				break;
			case 2:
				unit = 'd';
				break;
			case 3:
				unit = 'h';
				break;
			case 4:
				unit = 't';
				break;
			case 5:
				unit = 's';
				break;
			}
		}
		tInfoCanvas.setAttrName(chTrajAttr.getSelectedItem() + " from " + evLayer.getName());
		Vector<DGeoObject> events = null;
		if (!onlyTrEvents) {
			events = new Vector<DGeoObject>(evLayer.getObjectCount(), 10);
			for (int i = 0; i < evLayer.getObjectCount(); i++)
				if (!onlyActive || evLayer.isObjectActive(i)) {
					DGeoObject evt = evLayer.getObject(i);
					if (evt.getGeometry() != null && evt.getTimeReference() != null) {
						events.addElement(evt);
					}
				}
		}
		showMessage("Computing...", false);
		int mStep = (d.length > 1000) ? 100 : (d.length > 200) ? 50 : 25;
		for (int i = 0; i < d.length; i++) {
			DMovingObject mObj = (DMovingObject) trLayer.getObject(i);
			if (onlyTrEvents) {
				events = EventExtractor.getEventsForObject(mObj.getIdentifier(), evLayer, evTable, trIdColN, onlyActive);
			}
			d[i] = mObj.getDistancesToEvents(events, past, future, maxTLen, unit);
			if ((i + 1) % mStep == 0) {
				showMessage((i + 1) + " trajectories processed", false);
			}
		}
		showMessage("Computing finished!", false);
		return true;
	}

	protected boolean computeTemporalDistancesToEvents(double d[][]) {
		if (d == null)
			return false;
		Vector<DGeoLayer> evLayers = getLayersWithEvents();
		if (evLayers == null) {
			showMessage("No layers with events found!", true);
			return false;
		}
		List list = new List(Math.min(Math.max(2, evLayers.size()), 5));
		for (int i = 0; i < evLayers.size(); i++) {
			list.add(evLayers.elementAt(i).getName());
		}
		list.select(evLayers.size() - 1);
		Panel p = new Panel(new ColumnLayout());
		p.add(new Label("Select the layer with the events:"));
		p.add(list);
		p.add(new Label("Compute temporal distances to the nearest events"));
		Checkbox cbPast = new Checkbox("in the past", true);
		p.add(cbPast);
		Checkbox cbFuture = new Checkbox("in the future", true);
		p.add(cbFuture);
		p.add(new Line(false));
		Checkbox cbOnlyActiveEvents = new Checkbox("Consider only active (after filtering) events", true);
		p.add(cbOnlyActiveEvents);
		Checkbox cbOnlyTrEvents = new Checkbox("Select only relevant events for each trajectory", true);
		p.add(cbOnlyTrEvents);
		DMovingObject dmo = ((DMovingObject) trLayer.getObject(0));
		TimeMoment sampleTime = dmo.getStartTime();
		Choice ch = makeTimeUnitsChoice(sampleTime);
		if (ch != null) {
			Panel pp = new Panel(new BorderLayout());
			pp.add(new Label("Measure the temporal distance in"), BorderLayout.WEST);
			pp.add(ch, BorderLayout.EAST);
			p.add(pp);
		}
		OKDialog ok = new OKDialog(CManager.getFrame(this), "Temporal distance to events", true);
		ok.addContent(p);
		ok.show();
		if (ok.wasCancelled())
			return false;
		int idx = list.getSelectedIndex();
		if (idx < 0)
			return false;
		DGeoLayer evLayer = evLayers.elementAt(idx);
		boolean past = cbPast.getState(), future = cbFuture.getState(), onlyTrEvents = cbOnlyTrEvents.getState(), onlyActive = cbOnlyActiveEvents.getState();
		AttributeDataPortion evTable = null;
		int trIdColN = -1;
		if (onlyTrEvents) {
			evTable = evLayer.getThematicData();
			if (evTable == null) {
				if (!Dialogs.askYesOrNo(CManager.getAnyFrame(this), "The layer with the events has no table. Therefore, it is impossible " + "to select only relevant events for each trajectory. Compute temporal distances " + "to any events?",
						"Relevant events"))
					return false;
			} else {
				trIdColN = getColumnWithTrajIds(evTable);
				if (trIdColN < 0)
					return false;
			}
			if (trIdColN < 0) {
				onlyTrEvents = false;
			}
		}

		char unit = 's';
		if (ch != null) {
			switch (ch.getSelectedIndex()) {
			case 0:
				unit = 'y';
				break;
			case 1:
				unit = 'm';
				break;
			case 2:
				unit = 'd';
				break;
			case 3:
				unit = 'h';
				break;
			case 4:
				unit = 't';
				break;
			case 5:
				unit = 's';
				break;
			}
			tInfoCanvas.setAttrName(chTrajAttr.getSelectedItem() + " from " + evLayer.getName() + " (" + ch.getSelectedItem() + ")");
		} else {
			tInfoCanvas.setAttrName(chTrajAttr.getSelectedItem() + " from " + evLayer.getName());
		}
		Vector<DGeoObject> events = null;
		if (!onlyTrEvents) {
			events = new Vector<DGeoObject>(evLayer.getObjectCount(), 10);
			for (int i = 0; i < evLayer.getObjectCount(); i++)
				if (!onlyActive || evLayer.isObjectActive(i)) {
					DGeoObject evt = evLayer.getObject(i);
					if (evt.getGeometry() != null && evt.getTimeReference() != null) {
						events.addElement(evt);
					}
				}
		}
		showMessage("Computing...", false);
		int mStep = (d.length > 1000) ? 100 : (d.length > 200) ? 50 : 25;
		for (int i = 0; i < d.length; i++) {
			DMovingObject mObj = (DMovingObject) trLayer.getObject(i);
			if (onlyTrEvents) {
				events = EventExtractor.getEventsForObject(mObj.getIdentifier(), evLayer, evTable, trIdColN, onlyActive);
			}
			d[i] = mObj.getTemporalDistancesToEvents(events, past, future, unit);
			if ((i + 1) % mStep == 0) {
				showMessage((i + 1) + " trajectories processed", false);
			}
		}
		showMessage("Computing finished!", false);
		return true;
	}

	protected boolean computeTemporalDistancesToTimeMoments(double d[][]) {
		if (d == null)
			return false;
		DataTable tbl = (DataTable) trLayer.getThematicData();
		if (tbl == null)
			return false;
		IntArray colNs = new IntArray(tbl.getAttrCount(), 10);
		List listT = new List(Math.min(tbl.getAttrCount(), 10));
		for (int i = 0; i < tbl.getAttrCount(); i++) {
			Attribute at = tbl.getAttribute(i);
			if (at.isTemporal()) {
				colNs.addElement(i);
				listT.add(at.getName());
			}
		}
		if (colNs.size() < 1) {
			showMessage("The table has no column with times!", true);
			return false;
		}
		listT.select(0);
		Panel pp = new Panel(new BorderLayout());
		pp.add(new Label("Select the column with the time moments:"), BorderLayout.NORTH);
		pp.add(listT, BorderLayout.CENTER);
		DMovingObject dmo = ((DMovingObject) trLayer.getObject(0));
		TimeMoment sampleTime = dmo.getStartTime();
		Choice ch = makeTimeUnitsChoice(sampleTime);
		if (ch != null) {
			Panel p = new Panel(new BorderLayout());
			p.add(new Label("Measure the temporal distance in"), BorderLayout.WEST);
			p.add(ch, BorderLayout.EAST);
			pp.add(p, BorderLayout.SOUTH);
		}
		OKDialog dia = new OKDialog(CManager.getAnyFrame(this), "Point layer", true);
		dia.addContent(pp);
		dia.show();
		if (dia.wasCancelled())
			return false;
		int idx = listT.getSelectedIndex();
		if (idx < 0) {
			showMessage("The column has not been selected!", true);
			return false;
		}
		int cN = colNs.elementAt(idx);
		char unit = 's';
		if (ch != null) {
			switch (ch.getSelectedIndex()) {
			case 0:
				unit = 'y';
				break;
			case 1:
				unit = 'm';
				break;
			case 2:
				unit = 'd';
				break;
			case 3:
				unit = 'h';
				break;
			case 4:
				unit = 't';
				break;
			case 5:
				unit = 's';
				break;
			}
		}
		showMessage("Computing...", false);
		int mStep = (d.length > 1000) ? 100 : (d.length > 200) ? 50 : 25;
		for (int i = 0; i < d.length; i++) {
			DMovingObject mobj = (DMovingObject) trLayer.getObject(i);
			ThematicDataItem rec = mobj.getData();
			if (rec == null) {
				continue;
			}
			Object val = rec.getAttrValue(cN);
			if (val == null) {
				continue;
			}
			if (!(val instanceof TimeMoment)) {
				showMessage("The column contains unsuitable values " + "(not time moments)!", true);
				return false;
			}
			if (!val.getClass().equals(sampleTime.getClass())) {
				showMessage("The values in the table column are " + "not compatible with the times in the trajectories!", true);
				return false;
			}
			d[i] = mobj.getTemporalDistancesToTimeMoment((TimeMoment) val, unit);
			if ((i + 1) % mStep == 0) {
				showMessage((i + 1) + " trajectories processed", false);
			}
		}
		showMessage("Computing finished!", false);
		if (ch != null) {
			tInfoCanvas.setAttrName("Temporal distance to " + tbl.getAttributeName(cN) + " (" + ch.getSelectedItem() + ")");
		} else {
			tInfoCanvas.setAttrName("Temporal distance to " + tbl.getAttributeName(cN));
		}
		whatIsFiltered = DMovingObjectSegmFilter.FilterForPoints;
		return true;
	}

	protected int[] askTimeIntervalLength(TimeMoment sampleTime, String prompt, String title) {
		int precIdx = sampleTime.getPrecisionIdx();
		Panel p = new Panel(new ColumnLayout()), pp = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		Choice ch = makeTimeUnitsChoice(sampleTime);
		if (ch != null) {
			pp.add(new Label("Time precision:"));
			pp.add(ch);
			p.add(pp);
			pp = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		}
		TextField tf = new TextField("1", 5);
		pp.add(new Label(prompt));
		pp.add(tf);
		p.add(pp);
		OKDialog ok = new OKDialog(CManager.getFrame(this), title, false);
		ok.addContent(p);
		ok.show();
		char unit = 't';
		int tolerance = 1;
		if (precIdx >= 0) {
			switch (ch.getSelectedIndex()) {
			case 0:
				unit = 'y';
				break;
			case 1:
				unit = 'm';
				break;
			case 2:
				unit = 'd';
				break;
			case 3:
				unit = 'h';
				break;
			case 4:
				unit = 't';
				break;
			case 5:
				unit = 's';
				break;
			}
		}
		try {
			tolerance = Integer.valueOf(tf.getText()).intValue();
		} catch (NumberFormatException nfe) {
			tolerance = 1;
		}
		int ut[] = new int[2];
		ut[0] = unit;
		ut[1] = tolerance;
		return ut;
	}

	public boolean getDateComponent(double d[][]) {
		TimeReference tSpan = trLayer.getTimeSpan();
		if (tSpan == null || tSpan.getValidFrom() == null) {
			showMessage("No dates/times in the data!", true);
			return false;
		}
		if (!(tSpan.getValidFrom() instanceof Date)) {
			showMessage("The times in the data are abstract and have no components!", true);
			return false;
		}
		Date d1 = (Date) tSpan.getValidFrom(), d2 = (Date) tSpan.getValidUntil();
		SelectDialog selDia = new SelectDialog(CManager.getAnyFrame(this), "Date component", "What component of the date/time to extract?");
		char prec = d1.getPrecision();
		int nOptions = 0;
		if (d1.hasElement('y')) {
			d1.setPrecision('y');
			d2.setPrecision('y');
			long diff = d2.subtract(d1);
			if (diff > 1) {
				selDia.addOption("year", "year", true);
				++nOptions;
			}
		}
		if (d1.hasElement('m')) {
			d1.setPrecision('m');
			d2.setPrecision('m');
			long diff = d2.subtract(d1);
			if (diff > 1) {
				selDia.addOption("month", "month", nOptions == 0);
				++nOptions;
			}
		}
		if (d1.hasElement('d')) {
			d1.setPrecision('d');
			d2.setPrecision('d');
			long diff = d2.subtract(d1);
			if (diff > 1) {
				selDia.addOption("day of month", "day_of_month", false);
				selDia.addOption("day of week", "day_of_week", nOptions == 0);
				nOptions += 2;
			}
		}
		if (d1.hasElement('h')) {
			d1.setPrecision('h');
			d2.setPrecision('h');
			long diff = d2.subtract(d1);
			if (diff > 1) {
				selDia.addOption("hour of the day", "hour", nOptions == 0);
				++nOptions;
			}
		}
		d1.setPrecision(prec);
		d2.setPrecision(prec);
		if (nOptions < 1) {
			showMessage("No components found in the dates!", true);
			return false;
		}
		selDia.show();
		if (selDia.wasCancelled())
			return false;
		String dComp = selDia.getSelectedOptionId();
		if (dComp == null)
			return false;
		showMessage("Computing...", false);
		int mStep = (d.length > 1000) ? 100 : (d.length > 200) ? 50 : 25;
		for (int i = 0; i < d.length; i++) {
			DMovingObject mObj = (DMovingObject) trLayer.getObject(i);
			Vector track = mObj.getTrack();
			if (track == null || track.size() < 1) {
				continue;
			}
			d[i] = new double[track.size()];
			for (int j = 0; j < track.size(); j++) {
				SpatialEntity spe = (SpatialEntity) track.elementAt(j);
				TimeReference tr = spe.getTimeReference();
				if (tr == null || tr.getValidFrom() == null || !(tr.getValidFrom() instanceof Date)) {
					continue;
				}
				d1 = (Date) tr.getValidFrom();
				d[i][j] = (dComp.equals("year")) ? d1.getElementValue('y') : (dComp.equals("month")) ? d1.getElementValue('m') : (dComp.equals("day_of_month")) ? d1.getElementValue('d') : (dComp.equals("day_of_week")) ? d1.getDayOfWeek() : (dComp
						.equals("hour")) ? d1.getElementValue('h') : Double.NaN;
			}
			if ((i + 1) % mStep == 0) {
				showMessage((i + 1) + " trajectories processed", false);
			}
		}
		showMessage("Computing finished!", false);
		tInfoCanvas.setAttrName(chTrajAttr.getSelectedItem() + ": " + selDia.getSelectedOptionName());
		return true;
	}

	// ------------- SliderListener interface -----------------------
	private float breakValues[] = null;

	/**
	* Results of the classification. Min and Max values are not listed in <breaks>.
	* If there are no breaks, breaks==null.
	* Note that breaks.length is not always equal to the real number of breaks!
	* Use nBreaks!
	*/
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
			//evCanvas.setBreaksAndFocuserMinMax(breakValues,colors,dMin,dMax);
			setSegmFilters(false);
			tLegendCanvas.setBreaks(breakValues, colors);
		}
	}

	/**
	* This function is called during the process of moving a delimiter
	* between classes. <n> shows number of the delimiter (n==0 corresponds
	* to a delimiter between 0th and 1st classes)
	*/
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
		if (!Float.isNaN(oldNthBreakValue) && Math.abs(currValue - oldNthBreakValue) < 0.01f * (dMax - dMin))
			return;
		breakValues[n] = (float) currValue;
		Color colors[] = tSummaryClassColors();
		if (cbDynUpdate.getState()) {
			setSegmFilters(false);
		} else {
			evCanvas.setBreaksAndFocuserMinMax(breakValues, colors, dMin, dMax);
		}
		tLegendCanvas.setBreaks(breakValues, colors);
	}

	/**
	* Change of colors
	*/
	public void colorsChanged(Object source) {
		Color colors[] = tSummaryClassColors();
		if (colors == null)
			return;
		if (colors.length != 1 + breakValues.length)
			return;
		evCanvas.setBreaksAndFocuserMinMax(breakValues, colors, dMin, dMax);
		tLegendCanvas.setBreaks(breakValues, colors);
	}

	// ------------- SliderListener interface - end -----------------------

	protected Color[] tSummaryClassColors() {
		Color colors[] = new Color[1 + slTSummary.getNBreaks()];
		for (int i = 0; i < colors.length; i++) {
			colors[i] = slTSummary.getColor(i);
		}
		return colors;
	}

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
		Color colors[] = tSummaryClassColors();
		evCanvas.setBreaksAndFocuserMinMax(breakValues, colors, dMin, dMax);
		tLegendCanvas.setBreaks(breakValues, colors);
	}

	public void showMessage(String txt, boolean isError) {
		if (supervisor != null && supervisor.getUI() != null) {
			supervisor.getUI().showMessage(txt, isError);
		}
		if (isError) {
			System.err.println(txt);
		}
	}

//---------------- FocusListener interface ------------
	public void focusChanged(Object source, double lowerLimit, double upperLimit) {
		dMin = lowerLimit;
		dMax = upperLimit;
		evCanvas.setBreaksAndFocuserMinMax(breakValues, tSummaryClassColors(), dMin, dMax);
		tLegendCanvas.setFocuserMinMax(dMin, dMax);
	}

	public void limitIsMoving(Object source, int n, double currValue) {
		Focuser f = (Focuser) source;
		dMin = f.getCurrMin();
		dMax = f.getCurrMax();
		evCanvas.setBreaksAndFocuserMinMax(breakValues, tSummaryClassColors(), dMin, dMax);
		tLegendCanvas.setFocuserMinMax(dMin, dMax);
	}

	//-------------- Destroyable interface
	boolean isDestroyed = false;

	public boolean isDestroyed() {
		return isDestroyed;
	}

	public void destroy() {
		isDestroyed = true;
		if (tpn != null) {
			tpn.destroy();
		}
		trLayer.removePropertyChangeListener(this);
		for (int i = 0; i < trLayer.getObjectCount(); i++) {
			((DMovingObject) trLayer.getObject(i)).clearFilter(this);
		}
		trLayer.propertyChange(new PropertyChangeEvent(this, "TrajSegmFilter", null, null));
	}
}
