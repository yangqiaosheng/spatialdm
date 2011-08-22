package spade.time.vis;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Label;
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
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.manipulation.DichromaticSlider;
import spade.analysis.manipulation.Slider;
import spade.analysis.manipulation.SliderListener;
import spade.analysis.system.Supervisor;
import spade.analysis.system.SystemUI;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.FocusListener;
import spade.lib.basicwin.Focuser;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.MultiSelector;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.PlotCanvas;
import spade.lib.basicwin.RowLayout;
import spade.lib.basicwin.TabbedPanel;
import spade.lib.color.ColorSelDialog;
import spade.lib.color.Rainbow;
import spade.lib.lang.Language;
import spade.lib.util.DoubleArray;
import spade.lib.util.ListSelector;
import spade.lib.util.StringUtil;
import spade.time.Date;
import spade.time.FocusInterval;
import spade.time.TimeMoment;
import spade.time.TimePositionNotifier;
import spade.time.ui.TimeSlider;
import spade.time.ui.TimeSliderPanel;
import spade.vis.database.DataItem;
import spade.vis.database.ObjectContainer;
import spade.vis.database.SpatialEntity;
import spade.vis.database.ThematicDataItem;
import spade.vis.database.ThematicDataOwner;
import spade.vis.dmap.DTemporalCluster;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 01-Feb-2007
 * Time: 16:49:00
 * Includes an EventCanvas and interactive manipulation controls.
 */
public class ClusterTimeGraphPanel extends Panel implements SliderListener, FocusListener, ActionListener, ItemListener, ComponentListener, PropertyChangeListener, Destroyable {
	static ResourceBundle res = Language.getTextResource("spade.time.vis.SchedulerTexts_time_vis");
	/**
	 * The container with time-referenced items (events), which are visualised
	 */
	protected ObjectContainer oCont = null;
	/**
	 * The canvas in which the visualisation is done
	 */
	protected ClusterTimeGraphCanvas evCanvas = null;
	/**
	 * The canvas in which labels are drawn (to remain fixed independently of
	 * the scroller position)
	 */
	protected TimeLineLabelsCanvas tLabCanvas = null;
	/**
	 * The canvas for drawing events, shows time ticks in the same way as TimeLineLabelsCanvas
	 */
	protected TrajTimeGraphEventCanvas tEventCanvas = null;
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
	protected Checkbox cbTrajAttr[] = null;
	protected CheckboxGroup cbgTrajAttr = null;

	protected Panel controlP = null; // main control panel
	//controlPextra=null; // placeholder for further UI elements

	/**
	 * range of values of the displayed attribute
	 */
	protected double dMin = Float.NaN, dMax = Float.NaN;

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

	/**
	 * controls for display
	 */
	protected Checkbox chViewMode[] = null; // time graph or time line
	protected Checkbox cbPointing[] = null;
	protected TextField tfBarWidth = null;
	protected Checkbox cbOrderAsc = null; //

	protected TimePositionNotifier tpn = null;

	//mapping of ids and cluster objects
	Map<String, DataItem> map = new HashMap<String, DataItem>();

	public void setTableRef(String ref) {
		evCanvas.setTableRef(ref);
	}

	/**
	 * Constructs the visualisation display.
	 * @param oCont - container with time-referenced items (events)
	 */
	public ClusterTimeGraphPanel(ObjectContainer oCont) {
		this.oCont = oCont;
		setLayout(new BorderLayout());

		Panel mainp = new Panel();
		add(mainp, BorderLayout.CENTER);
		mainp.setLayout(new BorderLayout());

		mainp.add(tInfoCanvas = new TrajTimeGraphInfoCanvas(), BorderLayout.NORTH);
		mainp.add(tLegendCanvas = new TrajTimeGraphLegendCanvas(), BorderLayout.WEST);
		mainp.add(tFocuserCanvas = new TrajTimeGraphFocuserCanvas(this), BorderLayout.EAST);
		Panel infop = new Panel(new BorderLayout());
		mainp.add(infop, BorderLayout.CENTER);
		infop.add(tEventCanvas = new TrajTimeGraphEventCanvas(), BorderLayout.SOUTH);

		evCanvas = new ClusterTimeGraphCanvas();

		evCanvas.setObjectContainer(oCont);
		scp = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
		scp.add(evCanvas);
		infop.add(scp, BorderLayout.CENTER);
		scp.addComponentListener(this);

		infop.add(tLabCanvas = new TimeLineLabelsCanvas(), BorderLayout.NORTH);
		TimeLineLabelsCanvas tllc[] = new TimeLineLabelsCanvas[2];
		tllc[0] = tLabCanvas;
		tllc[1] = tEventCanvas;
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
			Panel pp = new Panel(new ColumnLayout());
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

		Panel controlPdata = new Panel(new ColumnLayout());
		Panel p = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		controlPdata.add(p);
		p.add(new Label("Attribute:"));
		cbTrajAttr = new Checkbox[6];
		cbgTrajAttr = new CheckboxGroup();
		cbTrajAttr[0] = new Checkbox("Number of entries", true, cbgTrajAttr);
		cbTrajAttr[1] = new Checkbox("Geo distance", false, cbgTrajAttr);
		cbTrajAttr[2] = new Checkbox("Temporal distance", false, cbgTrajAttr);
		cbTrajAttr[3] = new Checkbox("Geo density", false, cbgTrajAttr);
		cbTrajAttr[4] = new Checkbox("Temporal density", false, cbgTrajAttr);
		cbTrajAttr[5] = new Checkbox("Cluster IDS (not for simple visualization)", false, cbgTrajAttr);

		for (Checkbox element : cbTrajAttr) {
			element.addItemListener(this);
			p.add(element);
		}

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
		p.add(new Label("Mouse:"));
		cbg = new CheckboxGroup();
		cbPointing = new Checkbox[2];
		p.add(cbPointing[0] = new Checkbox("highlighting", true, cbg));
		p.add(cbPointing[1] = new Checkbox("time referencing", false, cbg));
		for (Checkbox element : cbPointing) {
			element.addItemListener(this);
		}
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

		Panel controlPevents = new Panel();

		tp.addComponent("time", controlPtime);
		tp.addComponent("data", controlPdata);
		tp.addComponent("display", controlPdisplay);
		tp.addComponent("classes", controlPclasses);
		tp.addComponent("events", controlPevents);
		tp.makeLayout(true);

		tpn = new TimePositionNotifier();
		evCanvas.setTimePositionNotifier(tpn);
		tLabCanvas.setTimePositionNotifier(tpn);
		tEventCanvas.setTimePositionNotifier(tpn);
		tpn.addPropertyChangeListener(this);

		setupForNewTrajectoryAttribute();
	}

	public void setColorsForTimes(Vector<Object[]> c) {
		evCanvas.setColorsForTimes(c);
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
		slTSummary.setPositiveHue(0.3f);
		slTSummary.setNegativeHue(0.1f);
		canvasSliderTSummary.setContent(dslTSummary);
		canvasSliderTSummary.setVisible(false);
		Panel ControlPanelTSummary = new Panel(new ColumnLayout());
		Panel p = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		ControlPanelTSummary.add(p);
		p.add(cbTSummary);
		p = new Panel(new BorderLayout());
		ControlPanelTSummary.add(p);
		p.add(labelTSummaryBreaks, BorderLayout.WEST);
		Panel pp = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
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

	@Override
	public Dimension getPreferredSize() {
		Dimension d = evCanvas.getPreferredSize(), ss = Toolkit.getDefaultToolkit().getScreenSize();
		int w = Math.min(d.width, ss.width * 2 / 3), h = Math.min(d.height, ss.height * 2 / 3);
		w += scp.getVScrollbarWidth() + 10;
		h += scp.getHScrollbarHeight() + 10 + tLabCanvas.getPreferredSize().height + tEventCanvas.getPreferredSize().height;
		d = controlP.getPreferredSize();
		h += d.height;
		if (w < d.width) {
			w = d.width;
		}
		return new Dimension(w, h);
	}

	private ThematicDataItem getThematicDataItem() {
		if (oCont == null)
			return null;
		for (int i = 0; i < oCont.getObjectCount(); i++) {
			DataItem data = oCont.getObjectData(i);
			if (data == null) {
				continue;
			}
			if (data instanceof ThematicDataItem)
				return (ThematicDataItem) data;
			if (data instanceof ThematicDataOwner)
				return ((ThematicDataOwner) data).getThematicData();
		}
		return null;
	}

	@Override
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
		for (Checkbox element : cbTrajAttr)
			if (e.getSource().equals(element)) {
				setupForNewTrajectoryAttribute();
				return;
			}
		if (e.getSource().equals(cbTSummary)) {
			setupSummary();
		}
		if (e.getSource().equals(cbPointing[0]) || e.getSource().equals(cbPointing[1])) {
			evCanvas.setHighlightingIsActive(cbPointing[0].getState());
		}
	}

	protected void setupSummary() {
		if (!cbTSummary.getState()) {
			evCanvas.setBreaksAndFocuserMinMax(null, null, dMin, dMax);
			breakValues = null;
			tLegendCanvas.setBreaks(null, null);
			labelTSummaryMin.setVisible(false);
			labelTSummaryMax.setVisible(false);
			rainbow.setVisible(false);
			tfTSummary.setVisible(false);
			canvasSliderTSummary.setVisible(false);
			return;
		}
		labelTSummaryMin.setVisible(true);
		labelTSummaryMax.setVisible(true);
		rainbow.setVisible(true);
		tfTSummary.setVisible(true);
		canvasSliderTSummary.setVisible(true);
		invalidate();
		validate();
		double absMin = dMin, absMax = dMax;
		labelTSummaryMin.setText(StringUtil.doubleToStr(absMin, absMin, absMax));
		labelTSummaryMax.setText(StringUtil.doubleToStr(absMax, absMin, absMax));
		slTSummary.removeSliderListener(this);
		slTSummary.setMinMax(absMin, absMax);
		DoubleArray br = slTSummary.getBreaks();
		if (br.size() < 1) {
			int sel = -1;
			for (int i = 0; i < cbTrajAttr.length && sel == -1; i++)
				if (cbTrajAttr[i].getState()) {
					sel = i;
				}
			double d[][] = new double[oCont.getObjectCount()][];
			switch (sel) {
			case 0:
				tInfoCanvas.setAttrName("number of entries (" + sUnits + ")");
				for (int i = 0; i < d.length; i++) {
					d[i] = ((DTemporalCluster) oCont.getObject(i)).getCounts();
				}
				break;
			case 1:
				tInfoCanvas.setAttrName("geo distance (" + sUnits + ")");
				for (int i = 0; i < d.length; i++) {
					d[i] = ((DTemporalCluster) oCont.getObject(i)).getGeodist();
				}
				break;
			case 2:
				tInfoCanvas.setAttrName("temporal distance (" + sUnits + ")");
				for (int i = 0; i < d.length; i++) {
					d[i] = ((DTemporalCluster) oCont.getObject(i)).getTempdist();
				}
				break;
			case 3:
				tInfoCanvas.setAttrName("geo density (" + sUnits + ")");
				for (int i = 0; i < d.length; i++) {
					d[i] = ((DTemporalCluster) oCont.getObject(i)).getGeodense();
				}
				break;
			case 4:
				tInfoCanvas.setAttrName("temporal density (" + sUnits + ")");
				for (int i = 0; i < d.length; i++) {
					d[i] = ((DTemporalCluster) oCont.getObject(i)).getTempdense();
				}
				break;
			case 5:
				tInfoCanvas.setAttrName("Cluster IDS (" + sUnits + ")");
				for (int i = 0; i < d.length; i++) {
					d[i] = ((DTemporalCluster) oCont.getObject(i)).getCIDS();
				}
				break;
			}
			for (int i = 0; i < d[0].length; i++) {
				slTSummary.addBreak(d[0][i]);
			}
			//br.addElement((absMin+absMax)/2f);
			//slTSummary.addBreak(br.elementAt(0));
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

	@Override
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
		}
		if (cmd.equalsIgnoreCase("change")) {
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
		}
	}

	/**
	 * Listens to changes of the time focuser
	 */
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getPropertyName().equals("current_moment") && (e.getNewValue() instanceof TimePositionNotifier)) {
			TimePositionNotifier tpn = (TimePositionNotifier) e.getNewValue();
			//TimePositionNotifier tpn=(TimePositionNotifier)e.getSource();
			String str = "";
			if (e.getOldValue() instanceof ClusterTimeGraphCanvas && tpn.lastId != null) {
				str = "<" + StringUtil.floatToStr((float) tpn.lastValue, (float) dMin, (float) dMax) + "> \"" + tpn.lastId + "\" t=" + tpn.getMouseTime().toString() + " @ " + tpn.lastDataTime + " c_id " + tpn.lastC_id;
				//String c_id = ((DTemporalCluster)map.get(tpn.lastId)).get
			}

			tInfoCanvas.setTimeRef((tpn.getMouseX() < 0) ? "" : str);
			if (supervisor != null) {
				SystemUI ui = supervisor.getUI();
				if (ui != null)
					if (tpn.getMouseX() < 0 || tpn.lastId == null) {
						ui.erasePositionMarksOnMaps();
					} else {
						ui.markPositionOnMaps((float) tpn.lastMapX, (float) tpn.lastMapY);
					}
			}
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
				TimeMoment start = evCanvas.getDataStart(), end = evCanvas.getDataEnd();
				bStartEnd.setEnabled(start.compareTo(t1) < 0 || end.compareTo(t2) > 0);
			}
		}
	}

	/**
	 * Invoked when the component's size changes.
	 */
	@Override
	public void componentResized(ComponentEvent e) {
		Dimension sps = scp.getSize(), cs = evCanvas.getSize();
		int w = sps.width - scp.getVScrollbarWidth() - 8;
		evCanvas.setSize(w, cs.height);
		cs = tLabCanvas.getSize();
		tLabCanvas.setSize(w, cs.height);
		tEventCanvas.setSize(w, cs.height);
		evCanvas.invalidate();
		tLabCanvas.invalidate();
		tEventCanvas.invalidate();
		scp.invalidate();
		CManager.validateAll(this);
	}

	/**
	 * Invoked when the component's position changes.
	 */
	@Override
	public void componentMoved(ComponentEvent e) {
	}

	/**
	 * Invoked when the component has been made visible.
	 */
	@Override
	public void componentShown(ComponentEvent e) {
	}

	/**
	 * Invoked when the component has been made invisible.
	 */
	@Override
	public void componentHidden(ComponentEvent e) {
	}

	/**
	 * Invoked when data are replaced: new attribute of the trajectories is selected for display
	 */
	public void setupForNewTrajectoryAttribute() {
		int sel = -1;
		for (int i = 0; i < cbTrajAttr.length && sel == -1; i++)
			if (cbTrajAttr[i].getState()) {
				sel = i;
			}
		DTemporalCluster dmo = ((DTemporalCluster) oCont.getObject(0));
		sUnits = (dmo.isGeographic()) ? "km" : "distance units";
		TimeMoment tm = ((SpatialEntity) dmo.getTrack().elementAt(0)).getTimeReference().getValidFrom();
		boolean isPhysicalTime = (tm instanceof Date) ? true : false;
		double d[][] = new double[oCont.getObjectCount()][];
		switch (sel) {
		case 0:
			tInfoCanvas.setAttrName("number of entries (" + sUnits + ")");
			for (int i = 0; i < d.length; i++) {
				d[i] = ((DTemporalCluster) oCont.getObject(i)).getCounts();
			}
			break;
		case 1:
			tInfoCanvas.setAttrName("geo distance (" + sUnits + ")");
			for (int i = 0; i < d.length; i++) {
				d[i] = ((DTemporalCluster) oCont.getObject(i)).getGeodist();
			}
			break;
		case 2:
			tInfoCanvas.setAttrName("temporal distance (" + sUnits + ")");
			for (int i = 0; i < d.length; i++) {
				d[i] = ((DTemporalCluster) oCont.getObject(i)).getTempdist();
			}
			break;
		case 3:
			tInfoCanvas.setAttrName("geo density (" + sUnits + ")");
			for (int i = 0; i < d.length; i++) {
				d[i] = ((DTemporalCluster) oCont.getObject(i)).getGeodense();
			}
			break;
		case 4:
			tInfoCanvas.setAttrName("temporal density (" + sUnits + ")");
			for (int i = 0; i < d.length; i++) {
				d[i] = ((DTemporalCluster) oCont.getObject(i)).getTempdense();
			}
			break;
		case 5:
			tInfoCanvas.setAttrName("Cluster IDS (" + sUnits + ")");
			for (int i = 0; i < d.length; i++) {
				d[i] = ((DTemporalCluster) oCont.getObject(i)).getCIDS();
			}
			break;
		}
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
		evCanvas.setAttrVals(d);
		setupSummary();
		//evCanvas.setBreaksAndFocuserMinMax(breakValues,tSummaryClassColors(),dMin,dMax);
		tFocuserCanvas.setFocuserMinMax(dMin, dMax);
		tLegendCanvas.setFocuserMinMax(dMin, dMax);
	}

	// ------------- SliderListener interface -----------------------
	private float breakValues[] = null;

	/**
	* Results of the classification. Min and Max values are not listed in <breaks>.
	* If there are no breaks, breaks==null.
	* Note that breaks.length is not always equal to the real number of breaks!
	* Use nBreaks!
	*/
	@Override
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
			evCanvas.setBreaksAndFocuserMinMax(breakValues, colors, dMin, dMax);
			tLegendCanvas.setBreaks(breakValues, colors);
		}
	}

	/**
	* This function is called during the process of moving a delimiter
	* between classes. <n> shows number of the delimiter (n==0 corresponds
	* to a delimiter between 0th and 1st classes)
	*/
	@Override
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
		evCanvas.setBreaksAndFocuserMinMax(breakValues, colors, dMin, dMax);
		tLegendCanvas.setBreaks(breakValues, colors);
	}

	/**
	* Change of colors
	*/
	@Override
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

//---------------- FocusListener interface ------------
	@Override
	public void focusChanged(Object source, double lowerLimit, double upperLimit) {
		dMin = lowerLimit;
		dMax = upperLimit;
		evCanvas.setBreaksAndFocuserMinMax(breakValues, tSummaryClassColors(), dMin, dMax);
		tLegendCanvas.setFocuserMinMax(dMin, dMax);
	}

	@Override
	public void limitIsMoving(Object source, int n, double currValue) {
		Focuser f = (Focuser) source;
		dMin = f.getCurrMin();
		dMax = f.getCurrMax();
		evCanvas.setBreaksAndFocuserMinMax(breakValues, tSummaryClassColors(), dMin, dMax);
		tLegendCanvas.setFocuserMinMax(dMin, dMax);
	}

	protected boolean destroyed = false;

	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

	public void destroy() {
		if (destroyed)
			return;
		if (tpn != null) {
			tpn.destroy();
		}
		destroyed = true;
	}

}
