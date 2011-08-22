package spade.time.ui;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.Window;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.classification.TableClassifier;
import spade.analysis.system.ESDACore;
import spade.analysis.transform.AttributeTransformer;
import spade.analysis.transform.TransformerGenerator;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.SelectDialog;
import spade.lib.basicwin.TextCanvas;
import spade.lib.lang.Language;
import spade.lib.util.IntArray;
import spade.lib.util.VectorUtil;
import spade.time.Date;
import spade.time.FocusInterval;
import spade.time.TimeMoment;
import spade.time.manage.TemporalDataManager;
import spade.time.transform.TimeAttrTransformer;
import spade.time.vis.DataVisAnimator;
import spade.time.vis.TimeGraph;
import spade.time.vis.TimeGraphPanel;
import spade.time.vis.TimeGraphSummary;
import spade.time.vis.TimeLineView;
import spade.time.vis.TrajTimeGraphPanel;
import spade.time.vis.ValueFlowVisualizer;
import spade.time.vis.VisAttrDescriptor;
import spade.vis.database.AttrDescriptor;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.ObjectContainer;
import spade.vis.database.Parameter;
import spade.vis.database.TimeFilter;
import spade.vis.dmap.DMovingObject;
import spade.vis.geometry.Geometry;
import spade.vis.map.MapViewer;
import spade.vis.mapvis.DataMapper;
import spade.vis.mapvis.DataPresenter;
import spade.vis.mapvis.Visualizer;
import spade.vis.space.GeoLayer;
import ui.AttributeChooser;

/**
 * Provides access to all system functions dealing with time-referenced data
 */
public class TimeFunctionsUI implements TimeUI, PropertyChangeListener {
	static ResourceBundle res = Language.getTextResource("spade.time.ui.Res");
	/**
	 * Available methods for the visualization and analysis of time-series
	 * thematic data: identifiers and full names.
	 */
	static protected String tsVisMethods[][] = { { "animated_map", res.getString("animated_map") }, { "diagram_map", res.getString("diagram_map") }, { "time_graph", res.getString("time_graph") }, };
	/**
	 * A reference to the manager of time-referenced data
	 */
	protected TemporalDataManager timeMan = null;
	/**
	 * A reference to the system's core
	 */
	protected ESDACore core = null;
	/**
	 * A reference to an already created time filter panel, which is used for
	 * events and movement data. This allows to avoid creation of two panels for
	 * the same data set.
	 */
	protected TimeControlPanel timeFilterControlPanel = null;
	/**
	 * A reference to an already created display time control panel, which is
	 * used in visualisation of time-series data. This allows to avoid creation
	 * of two panels for the same data set.
	 */
	protected TimeControlPanel displayTimeControlPanel = null;
	/**
	 * The list of property change listeners to be notified about opening or
	 * closing of the time control panels.
	 */
	protected Vector listeners = null;
	/**
	 * Used for transforming time references from absolute to relative
	 */
	protected TimeTransformUI ttransUI = null;

	/**
	 * Sets a reference to the manager of time-referenced data
	 */
	public void setDataManager(TemporalDataManager tdMan) {
		timeMan = tdMan;
		timeMan.addPropertyChangeListener(this);
	}

	/**
	 * Sets a reference to the system's core
	 */
	public void setCore(ESDACore core) {
		this.core = core;
	}

	/**
	 * Returns the number of available methods for the visualitation and
	 * analysis of time-series data.
	 */
	public int getTimeSeriesVisMethodsCount() {
		return tsVisMethods.length;
	}

	/**
	 * Returns the identifier of the method for the visualitation and analysis
	 * of time-series data with the given index.
	 */
	public String getTimeSeriesVisMethodId(int idx) {
		if (idx >= 0 && idx < tsVisMethods.length)
			return tsVisMethods[idx][0];
		return null;
	}

	/**
	 * Returns the name of the method for the visualitation and analysis of
	 * time-series data with the given index.
	 */
	public String getTimeSeriesVisMethodName(int idx) {
		if (idx >= 0 && idx < tsVisMethods.length)
			return tsVisMethods[idx][1];
		return null;
	}

	/**
	 * Returns true if there is a container (e.g. map layer) with
	 * time-referenced objects. This allows one to start time analysis
	 * functions.
	 */
	public boolean canStartTimeAnalysis() {
		return timeMan != null && timeMan.getContainerCount() > 0;
	}

	/**
	 * Builds a time line display of a user-selected time-referenced container
	 */
	protected void buildTimeLineView() {
		if (timeMan == null)
			return;
		int nCont = timeMan.getContainerCount();
		if (nCont < 1)
			return;
		Vector objSetIds = new Vector(nCont, 1);
		Vector objSetNames = new Vector(nCont, 1);
		IntArray contNs = new IntArray(nCont, 1);
		for (int i = 0; i < nCont; i++) {
			ObjectContainer oCont = timeMan.getContainer(i);
			String id = oCont.getEntitySetIdentifier();
			int idx = objSetIds.indexOf(id);
			if (idx < 0) {
				objSetIds.addElement(id);
				contNs.addElement(i);
				objSetNames.addElement(oCont.getName());
			} else {
				if (objSetNames.elementAt(idx) == null || (oCont instanceof GeoLayer)) {
					objSetNames.setElementAt(oCont.getName(), idx);
				}
				if (oCont instanceof AttributeDataPortion) {
					contNs.setElementAt(i, idx);
				}
			}
		}
		if (objSetIds.size() < 1) {
			core.getUI().showMessage("No suitable object sets found!", true);
			return;
		}
		List lst = new List(3);
		for (int i = 0; i < objSetNames.size(); i++) {
			lst.add((String) objSetNames.elementAt(i));
		}
		lst.select(0);
		Panel p = new Panel(new BorderLayout());
		TextCanvas tc = new TextCanvas();
		tc.setText("Build a time line display of the events from the set:");
		p.add(tc, BorderLayout.NORTH);
		p.add(lst, BorderLayout.CENTER);
		Frame mainFrame = null;
		if (core.getUI() != null) {
			mainFrame = core.getUI().getMainFrame();
		}
		if (mainFrame == null) {
			mainFrame = CManager.getAnyFrame();
		}
		OKDialog okd = new OKDialog(mainFrame, "Build a time line display", true);
		okd.addContent(p);
		okd.show();
		if (okd.wasCancelled())
			return;
		int selIdx = lst.getSelectedIndex();
		if (selIdx < 0)
			return;
		ObjectContainer oCont = timeMan.getContainer(contNs.elementAt(selIdx));
		TimeLineView tlv = new TimeLineView(oCont);
		tlv.setName("Time line: " + objSetNames.elementAt(selIdx));
		tlv.setSupervisor(core.getSupervisor());
		// core.getSupervisor().registerTool(tlv);
		core.getDisplayProducer().showGraph(tlv);
	}

	/**
	 * Builds a time line display of the data from the container with the given
	 * identifier
	 */
	public Component buildTimeLineView(String containerId) {
		if (containerId == null || timeMan == null || timeMan.getContainerCount() < 1)
			return null;
		int idx = -1;
		for (int i = 0; i < timeMan.getContainerCount() && idx < 0; i++)
			if (containerId.equalsIgnoreCase(timeMan.getContainer(i).getContainerIdentifier())) {
				idx = i;
			}
		if (idx < 0)
			return null;
		ObjectContainer oCont = timeMan.getContainer(idx);
		TimeLineView tlv = new TimeLineView(oCont);
		tlv.setName("Time line: " + oCont.getName());
		tlv.setSupervisor(core.getSupervisor());
		// core.getSupervisor().registerTool(tlv);
		core.getDisplayProducer().showGraph(tlv);
		return tlv;
	}

	/**
	 * Builds a trajectory time graph adapted from buildTimeLineView - some
	 * change may be needed
	 */
	protected void buildTrajTimeGraph() {
		if (timeMan == null)
			return;
		int nCont = timeMan.getContainerCount();
		if (nCont < 1)
			return;
		Vector objSetIds = new Vector(nCont, 1);
		Vector objSetNames = new Vector(nCont, 1);
		IntArray contNs = new IntArray(nCont, 1);
		for (int i = 0; i < nCont; i++) {
			ObjectContainer oCont = timeMan.getContainer(i);
			String id = oCont.getEntitySetIdentifier();
			int idx = objSetIds.indexOf(id);
			if (idx < 0) {
				objSetIds.addElement(id);
				contNs.addElement(i);
				objSetNames.addElement(oCont.getName());
			} else {
				if (objSetNames.elementAt(idx) == null || (oCont instanceof GeoLayer)) {
					objSetNames.setElementAt(oCont.getName(), idx);
					// if (oCont instanceof AttributeDataPortion)
					// contNs.setElementAt(i,idx);
				}
			}
		}
		if (objSetIds.size() < 1) {
			core.getUI().showMessage("No suitable object sets found!", true);
			return;
		}
		List lst = new List(3);
		for (int i = 0; i < objSetNames.size(); i++) {
			lst.add((String) objSetNames.elementAt(i));
		}
		lst.select(0);
		Panel p = new Panel(new BorderLayout());
		TextCanvas tc = new TextCanvas();
		tc.setText("Build a trajectory time graph:");
		p.add(tc, BorderLayout.NORTH);
		p.add(lst, BorderLayout.CENTER);
		Frame mainFrame = null;
		if (core.getUI() != null) {
			mainFrame = core.getUI().getMainFrame();
		}
		if (mainFrame == null) {
			mainFrame = CManager.getAnyFrame();
		}
		OKDialog okd = new OKDialog(mainFrame, "Build a trajectory time graph", true);
		okd.addContent(p);
		okd.show();
		if (okd.wasCancelled())
			return;
		int selIdx = lst.getSelectedIndex();
		if (selIdx < 0)
			return;
		ObjectContainer oCont = timeMan.getContainer(contNs.elementAt(selIdx));
		TrajTimeGraphPanel ttgp = new TrajTimeGraphPanel(oCont);
		ttgp.setName("Time graph: " + objSetNames.elementAt(selIdx));
		ttgp.setSupervisor(core.getSupervisor());
		// core.getSupervisor().registerTool(ttgp);
		core.getDisplayProducer().showGraph(ttgp);
	}

	/**
	 * Creates the UI for the time filter
	 */
	protected void makeTimeFilterUI() {
		if (timeFilterControlPanel != null) {
			Window win = CManager.getWindow(timeFilterControlPanel);
			if (win != null) {
				win.toFront();
			}
			return;
		}
		FocusInterval fint = new FocusInterval();
		TimeMoment first = null, last = null;
		char precision = 0;
		for (int i = 0; i < timeMan.getContainerCount(); i++) {
			TimeFilter filter = timeMan.getTimeFilter(i);
			TimeMoment t1 = filter.getEarliestMoment(), t2 = filter.getLatestMoment();
			if (t1 != null && t2 != null) {
				if (first == null || first.compareTo(t1) > 0) {
					first = t1;
				}
				if (last == null || last.compareTo(t2) < 0) {
					last = t2;
				}
				fint.addPropertyChangeListener(filter);
				char prec = t1.getPrecision();
				if (prec != 0)
					if (precision == 0) {
						precision = prec;
					} else if (precision != prec) {
						for (int j = Date.time_symbols.length - 1; j >= 0; j--)
							if (precision == Date.time_symbols[j]) {
								break;
							} else if (prec == Date.time_symbols[j]) {
								precision = prec;
								break;
							}
					}
			}
		}
		if (first == null || last == null)
			return;
		first = first.getCopy();
		last = last.getCopy();
		if (precision != 0) {
			first.setPrecision(precision);
			last.setPrecision(precision);
		}
		fint.setDataInterval(first, last);
		timeFilterControlPanel = new TimeControlPanel(fint, this, 1, true);
		timeFilterControlPanel.setSupervisor(core.getSupervisor());
		if (displayTimeControlPanel != null) {
			timeFilterControlPanel.setMaster(displayTimeControlPanel);
		}
		core.getSupervisor().registerTool(timeFilterControlPanel);
		core.getDisplayProducer().makeWindow(timeFilterControlPanel, timeFilterControlPanel.getName());
		notifyPropertyChange("open_time_filter_controls");
	}

	/**
	 * Reacts to user's activation of the time functions: displays appropriate
	 * dialogs, etc.
	 */
	public void startTimeAnalysis() {
		if (!canStartTimeAnalysis())
			return;
		SelectDialog selDia = new SelectDialog(core.getUI().getMainFrame(), "Time functions", null);
		if (timeFilterControlPanel == null) {
			selDia.addOption("Time filter controls", "filter", true);
		}
		selDia.addOption("Time line view", "time_line", timeFilterControlPanel != null);
		// check if we have any layer with trajectories
		boolean isTraj = false;
		for (int i = 0; i < timeMan.getContainerCount() && !isTraj; i++) {
			ObjectContainer oCont = timeMan.getContainer(i);
			if (oCont != null && oCont.getObject(0) != null && oCont.getObject(0) instanceof DMovingObject) {
				isTraj = true;
			}
		}
		if (isTraj) {
			selDia.addOption("Trajectory time graph", "traj_timegraph", timeFilterControlPanel != null);
		}
		if (ttransUI == null) {
			ttransUI = new TimeTransformUI();
		}
		if (ttransUI.canTransform(timeMan)) {
			selDia.addOption("Transformation of time references", "transform", false);
		}
		selDia.show();
		if (selDia.wasCancelled())
			return;
		String function = selDia.getSelectedOptionId();
		if (function.equals("filter")) {
			makeTimeFilterUI();
		} else if (function.equals("time_line")) {
			buildTimeLineView();
		} else if (function.equals("traj_timegraph")) {
			buildTrajTimeGraph();
		} else if (function.equals("transform")) {
			if (ttransUI.transformTimes(timeMan, core)) {
				timeMan.destroyTimeFilters();
				if (timeFilterControlPanel != null && !timeFilterControlPanel.isDestroyed()) {
					timeFilterControlPanel.destroy();
				}
				core.getDisplayProducer().closeDestroyedTools();
				notifyPropertyChange("close_time_filter_controls");
				timeFilterControlPanel = null;
			}
		}
	}

	/**
	 * Returns the FocusInterval, which propagates the events of changing the
	 * current time moment, from the time filter control panel.
	 */
	public FocusInterval getTimeFilterFocusInterval() {
		if (timeFilterControlPanel == null)
			return null;
		return timeFilterControlPanel.getFocusInterval();
	}

	/**
	 * Returns the FocusInterval, which propagates the events of changing the
	 * current time moment, from the display time control panel. If the panel
	 * does not exist yet, creates it. This method is used in visualisation of
	 * time-series data.
	 * 
	 * @param par
	 *            - the temporal parameter
	 */
	public FocusInterval getFocusInterval(Parameter par) {
		if (displayTimeControlPanel != null)
			return displayTimeControlPanel.getFocusInterval();
		FocusInterval fint = new FocusInterval();
		TimeMoment t0 = ((TimeMoment) par.getValue(0)).getCopy();
		TimeMoment t1 = ((TimeMoment) par.getValue(par.getValueCount() - 1)).getCopy();
		fint.setDataInterval(t0, t1);
		t1 = (TimeMoment) par.getValue(1);
		long step = t1.subtract(t0);
		// check whether the step is constant
		for (int i = 2; i < par.getValueCount() && step > 1; i++) {
			t0 = t1;
			t1 = (TimeMoment) par.getValue(i);
			long step1 = t1.subtract(t0);
			if (step != step1) {
				step = 1;
			}
		}
		if (step < 1) {
			step = 1;
		}
		displayTimeControlPanel = new TimeControlPanel(fint, this, (int) step, false);
		displayTimeControlPanel.setSupervisor(core.getSupervisor());
		if (timeFilterControlPanel != null) {
			timeFilterControlPanel.setMaster(displayTimeControlPanel);
		}
		core.getSupervisor().registerTool(displayTimeControlPanel);
		core.getDisplayProducer().makeWindow(displayTimeControlPanel, displayTimeControlPanel.getName());
		notifyPropertyChange("open_display_time_controls");
		return fint;
	}

	/**
	 * Visualizes time-dependent data from the given table assuming that the
	 * attributes to visualize and the visualization methods have been
	 * previously selected. The selection of the attributes is stored in the
	 * argument attrChooser, which is expected to be an instance of the class
	 * ui.AttributeChooser. If this argument is null or has inappropriate type,
	 * displays a dialog for attribute selection. The visualization method is
	 * specified by its identifier passed in the argument visMethodId. If this
	 * argument is null, displays a dialog for method selection. Returns 0 if
	 * successfully done, 1 if failed or the user cancelled the visualization,
	 * and -1 if the button "Back" has been pressed.
	 */
	public int visualizeTable(AttributeDataPortion table, Object attrSelector, String visMethodId) {
		if (table == null)
			return 1;
		if (!table.hasData()) {
			table.loadData();
		}
		if (!table.hasData()) {
			if (core.getUI() != null) {
				core.getUI().showMessage(res.getString("failed_load_data") + " " + table.getName(), true);
			}
			return 1;
		}
		if (core.getUI() != null) {
			core.getUI().clearStatusLine();
		}
		Parameter par = table.getTemporalParameter();
		if (par == null) {
			if (core.getUI() != null) {
				core.getUI().showMessage(table.getName() + ": " + res.getString("no_temp_param"), true);
			}
			return 1;
		}
		AttributeChooser attrChooser = null;
		if (attrSelector != null && (attrSelector instanceof AttributeChooser)) {
			attrChooser = (AttributeChooser) attrSelector;
		}
		if (attrChooser == null || attrChooser.getSelectedAttributes() == null) {
			attrChooser = new AttributeChooser();
			attrChooser.setColumnsMayBeReordered(false);
			Vector selTopAttr = attrChooser.selectTopLevelAttributes(table, res.getString("sel_attrs"), core.getUI());
			if (selTopAttr == null || selTopAttr.size() < 1)
				return 1;
		}
		Frame mainFrame = null;
		if (core.getUI() != null) {
			mainFrame = core.getUI().getMainFrame();
		}
		if (mainFrame == null) {
			mainFrame = CManager.getAnyFrame();
		}
		int visMethodN = -1;
		if (visMethodId != null) {
			for (int i = 0; i < tsVisMethods.length && visMethodN < 0; i++)
				if (tsVisMethods[i][0].equalsIgnoreCase(visMethodId)) {
					visMethodN = i;
				}
		}
		if (visMethodN < 0) {
			SelectDialog selDia = new SelectDialog(mainFrame, res.getString("vis_method"), res.getString("sel_vis_method") + ":");
			for (int i = 0; i < tsVisMethods.length; i++) {
				selDia.addOption(tsVisMethods[i][1], tsVisMethods[i][0], i == 0);
			}
			selDia.show();
			if (selDia.wasCancelled())
				return 1;
			visMethodN = selDia.getSelectedOptionN();
			visMethodId = selDia.getSelectedOptionId();
		}
		// check whether there is a map layer for this table
		GeoLayer layer = null;
		if (!visMethodId.equals("time_graph")) {
			layer = core.getDataKeeper().getTableLayer(table);
			if (layer == null) {
				if (core.getUI() != null) {
					core.getUI().showMessage(table.getName() + res.getString("no_map_layer"), true);
				}
				return -1;
			}
		}
		Vector attrDescr = attrChooser.getAttrDescriptors();
		// Check if at list one time-dependent attribute is selected
		IntArray timeIndep = new IntArray(attrDescr.size(), 1);
		for (int i = 0; i < attrDescr.size(); i++) {
			AttrDescriptor ad = (AttrDescriptor) attrDescr.elementAt(i);
			if (!ad.attr.dependsOnParameter(par)) {
				timeIndep.addElement(i);
			}
		}
		if (timeIndep.size() == attrDescr.size()) { // no time-dependent
			// attribute
			if (core.getUI() != null) {
				core.getUI().showMessage(res.getString("no_temp_attr_selected"), true);
			}
			return 1;
		}
		// If the selected visualization method is time graph or value flow map,
		// any time-independent attributes must be excluded from the
		// consideration.
		if (timeIndep.size() > 0 && (visMethodId.equals("diagram_map") || visMethodId.equals("time_graph"))) {
			Panel p = new Panel(new ColumnLayout());
			p.add(new Label(res.getString((timeIndep.size() > 1) ? "attrs_do_not_depend" : "attr_does_not_depend")));
			for (int i = 0; i < timeIndep.size(); i++) {
				AttrDescriptor ad = (AttrDescriptor) attrDescr.elementAt(timeIndep.elementAt(i));
				p.add(new Label(ad.attr.getName()));
			}
			p.add(new Line(false));
			TextCanvas tc = new TextCanvas();
			tc.addTextLine(res.getString((timeIndep.size() > 1) ? "These_attributes" : "This_attribute") + " " + res.getString("cannot_be_visualised_on") + " "
					+ res.getString((visMethodId.equals("time_graph")) ? "time_graph_small" : "diagram_map_small") + " " + res.getString("and_will_be_omitted"));
			p.add(tc);
			OKDialog okd = new OKDialog(mainFrame, res.getString("Time_independent_attrs"), true, true);
			okd.addContent(p);
			okd.show();
			if (okd.wasCancelled())
				return 1;
			if (okd.wasBackPressed())
				return -1;
			attrChooser.excludeAttributes(timeIndep);
			attrDescr = attrChooser.getAttrDescriptors();
		}
		// select values of any non-temporal parameters, if exist
		boolean depNotTemp = false;
		for (int i = 0; i < attrDescr.size() && !depNotTemp; i++) {
			AttrDescriptor ad = (AttrDescriptor) attrDescr.elementAt(i);
			int n = ad.getNVaryingParams();
			depNotTemp = n > 1 || (n > 0 && !ad.attr.dependsOnParameter(par));
		}
		boolean backpressed = false;
		do {
			backpressed = false;
			if (depNotTemp) {
				Vector tpar = new Vector(1, 1);
				tpar.addElement(par);
				attrDescr = attrChooser.selectParamValues(tpar);
				if (attrDescr == null)
					if (attrChooser.backButtonPressed())
						return -1;
					else
						return 1;
			}
			// enumerate all selected attributes taking into account
			// non-temporal parameters
			Vector attrDescrEx = null;
			if (!depNotTemp) {
				attrDescrEx = (Vector) attrDescr.clone(); // no non-temporal
			} else {
				int nComb = 1;
				for (int i = 0; i < table.getParamCount(); i++) {
					Parameter p = table.getParameter(i);
					if (!p.equals(par)) {
						nComb *= p.getValueCount();
					}
				}
				if (nComb == 1) {
					attrDescrEx = (Vector) attrDescr.clone();
				} else {
					attrDescrEx = new Vector(attrDescr.size() * nComb, 1);
					for (int i = 0; i < attrDescr.size(); i++) {
						AttrDescriptor ad = (AttrDescriptor) attrDescr.elementAt(i);
						if (ad.getNVaryingParams() < 1) {
							attrDescrEx.addElement(ad); // this attribute does
						} else if (ad.attr.dependsOnParameter(par) && ad.getNVaryingParams() < 2) {
							attrDescrEx.addElement(ad); // this attribute
						} else {
							// find all relevant combinations of values of
							// non-temporal parameters
							int len = ad.parVals.length;
							if (ad.attr.dependsOnParameter(par)) {
								--len;
							}
							Vector valLists[] = new Vector[len];
							String parNames[] = new String[len];
							int idxs[] = new int[ad.parVals.length];
							int k = 0;
							for (int j = 0; j < ad.parVals.length; j++)
								if (!par.getName().equals((String) ad.parVals[j].elementAt(0))) {
									valLists[k] = (Vector) ad.parVals[j].clone();
									parNames[k] = (String) valLists[k].elementAt(0);
									valLists[k].removeElementAt(0);
									idxs[j] = k;
									++k;
								} else {
									idxs[j] = -1;
								}
							Vector combs = VectorUtil.getAllCombinations(valLists);
							if (combs.size() < 2) {
								attrDescrEx.addElement(ad); // values of
								// non-temporal
								// parameters do not
								// vary
							} else {
								// for each combination, create a descriptor
								for (k = 0; k < combs.size(); k++) {
									Object comb[] = (Object[]) combs.elementAt(k);
									AttrDescriptor ad1 = new AttrDescriptor();
									ad1.children = new Vector(ad.children.size(), 1);
									for (int j = 0; j < ad.children.size(); j++) {
										Attribute child = (Attribute) ad.children.elementAt(j);
										boolean ok = true;
										for (int n = 0; n < parNames.length && ok; n++) {
											ok = child.hasParamValue(parNames[n], comb[n]);
										}
										if (ok) {
											ad1.children.addElement(child);
										}
									}
									if (ad1.children.size() > 0) {
										ad1.children.trimToSize();
										attrDescrEx.addElement(ad1);
									} else {
										continue;
									}
									ad1.attr = ad.attr;
									ad1.parVals = new Vector[ad.parVals.length];
									for (int j = 0; j < ad.parVals.length; j++)
										if (idxs[j] < 0) {
											ad1.parVals[j] = ad.parVals[j];
										} else {
											ad1.parVals[j] = new Vector(2, 1);
											ad1.parVals[j].addElement(parNames[idxs[j]]);
											ad1.parVals[j].addElement(comb[idxs[j]]);
										}
								}
							}
						}
					}
				}
			}
			if (attrDescrEx.size() > 1 && (visMethodId.equals("diagram_map") || visMethodId.equals("time_graph"))) {
				Panel p = new Panel(new BorderLayout());
				Panel pp = new Panel(new ColumnLayout());
				pp.add(new Label(res.getString("According_selection") + " " + attrDescrEx.size() + " " + res.getString((visMethodId.equals("time_graph")) ? "time_graphs" : "maps") + " " + res.getString("must_be_built")));
				pp.add(new Label(res.getString("check_what_to_visualise")));
				p.add(pp, BorderLayout.NORTH);
				Checkbox cb[] = new Checkbox[attrDescrEx.size()];
				pp = new Panel(new ColumnLayout());
				for (int i = 0; i < attrDescrEx.size(); i++) {
					AttrDescriptor ad = (AttrDescriptor) attrDescrEx.elementAt(i);
					cb[i] = new Checkbox(ad.getName(), true);
					pp.add(cb[i]);
				}
				if (attrDescrEx.size() < 10) {
					p.add(pp, BorderLayout.CENTER);
					p.add(new Line(false), BorderLayout.SOUTH);
				} else {
					ScrollPane scp = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
					scp.add(pp);
					p.add(scp, BorderLayout.CENTER);
				}
				OKDialog okd = new OKDialog(mainFrame, res.getString("Verify_selection"), true, true);
				okd.addContent(p);
				okd.show();
				if (okd.wasCancelled())
					return 1;
				if (okd.wasBackPressed())
					if (!depNotTemp)
						return -1;
					else {
						backpressed = true;
					}
				else {
					for (int i = attrDescrEx.size() - 1; i >= 0; i--)
						if (!cb[i].getState()) {
							attrDescrEx.removeElementAt(i);
						}
					if (attrDescrEx.size() < 1)
						return 1;
				}
			}
			if (!backpressed) {
				if (visMethodId.equals("time_graph")) {
					buildTimeGraph(table, par, attrDescrEx);
				} else if (visMethodId.equals("diagram_map")) {
					buildValueFlowMap(table, par, layer, attrDescrEx);
				} else {
					int res = buildAnimatedMap(table, par, layer, attrDescrEx);
					if (res == 1)
						return 1;
					if (res < 0)
						if (!depNotTemp)
							return -1;
						else {
							backpressed = true;
						}
				}
			}
		} while (backpressed);
		return 0;
	}

	/**
	 * Visualizes the specified time-dependent attribute(s) from the given table
	 * on a time graph or multiple time graphs.
	 */
	protected void buildTimeGraph(AttributeDataPortion table, Parameter par, Vector attrDescr) {
		if (table == null || par == null || attrDescr == null || attrDescr.size() < 1)
			return;
		FocusInterval fint = getFocusInterval(par);
		if (fint == null)
			return;
		TimeGraph tg[] = new TimeGraph[attrDescr.size()];
		TimeGraphSummary tsummary[] = new TimeGraphSummary[attrDescr.size()];
		for (int i = 0; i < attrDescr.size(); i++) {
			AttrDescriptor ad = (AttrDescriptor) attrDescr.elementAt(i);
			tg[i] = new TimeGraph();
			tg[i].setTable(table);
			tg[i].setSupervisor(core.getSupervisor());
			tg[i].setAttribute(ad.attr);
			tg[i].setTemporalParameter(par);
			tg[i].setFocusInterval(fint);
			if (ad.parVals.length > 1) {
				for (Vector parVal : ad.parVals)
					if (parVal.size() == 2) {
						tg[i].setOtherParameterValue((String) parVal.elementAt(0), parVal.elementAt(1));
					}
			}
			tsummary[i] = new TimeGraphSummary();
			tsummary[i].setTable(table);
			tsummary[i].setSupervisor(core.getSupervisor());
			tsummary[i].setAttribute(ad.attr);
			tsummary[i].setTemporalParameter(par);
		}
		TimeGraphPanel tgp = new TimeGraphPanel(core.getSupervisor(), tg, tsummary);
		tgp.setName(res.getString("time_graph"));
		//tgp.setSupervisor(core.getSupervisor());
		core.getSupervisor().registerTool(tgp);
		core.getDisplayProducer().showGraph(tgp);
	}

	/**
	 * Visualizes the specified time-dependent attribute(s) from the given table
	 * on a map or multiple maps using value flow diagram
	 */
	protected void buildValueFlowMap(AttributeDataPortion table, Parameter par, GeoLayer layer, Vector attrDescr) {
		if (table == null || par == null || attrDescr == null || attrDescr.size() < 1 || layer == null)
			return;
		if (core.getUI() == null) {
			System.out.println("ERROR: " + res.getString("no_ui"));
			return;
		}
		boolean useMainView = layer.getVisualizer() == null || !core.getUI().getUseNewMapForNewVis();
		for (int n = 0; n < attrDescr.size(); n++) {
			MapViewer mapView = core.getUI().getMapViewer((useMainView) ? "main" : "_blank_");
			if (mapView == null || mapView.getLayerManager() == null) {
				core.getUI().showMessage(res.getString("no_map_view"), true);
				return;
			}
			AttrDescriptor ad = (AttrDescriptor) attrDescr.elementAt(n);
			if (!useMainView) {
				// find the copy of the geographical layer in the new map view
				int lidx = mapView.getLayerManager().getIndexOfLayer(layer.getContainerIdentifier());
				if (lidx < 0) {
					core.getUI().showMessage(res.getString("no_layer_in_new_map"), true);
					return;
				}
				layer = mapView.getLayerManager().getGeoLayer(lidx);
				if (mapView instanceof Component) {
					Component c = (Component) mapView;
					c.setName(ad.getName() + ": " + res.getString("value_flows"));
					Frame win = CManager.getFrame(c);
					if (win != null) {
						win.setName(c.getName());
						win.setTitle(c.getName());
					}
				}
			}
			useMainView = false;
			ValueFlowVisualizer vis = new ValueFlowVisualizer();
			vis.setDataSource(table);
			vis.setAttribute(ad.attr);
			vis.setTemporalParameter(par);
			VisAttrDescriptor vd = new VisAttrDescriptor();
			vd.parent = ad.attr;
			vd.isTimeDependent = true;
			if (ad.children != null && ad.children.size() > 0) {
				vd.attr = (Attribute) ad.children.elementAt(0);
				vd.attrId = vd.attr.getIdentifier();
			}
			if (ad.parVals.length > 1) {
				vd.fixedParams = new Vector(ad.parVals.length - 1, 1);
				vd.fixedParamVals = new Vector(ad.parVals.length - 1, 1);
				for (Vector parVal : ad.parVals)
					if (parVal.size() == 2) {
						String name = (String) parVal.elementAt(0);
						Object value = parVal.elementAt(1);
						vd.fixedParams.addElement(name);
						vd.fixedParamVals.addElement(value);
						vis.setOtherParameterValue(name, value);
					}
			}
			Vector vvd = new Vector(1, 1), vaid = new Vector(1, 1);
			vvd.addElement(vd);
			vaid.addElement(ad.attr.getIdentifier());
			AttributeTransformer prevTrans = null, firstTrans = null;
			for (int i = 0; i < TransformerGenerator.getTransformerCount(); i++) {
				AttributeTransformer atrans = TransformerGenerator.makeTransformer(i);
				if (atrans == null) {
					continue;
				}
				atrans.setDataTable(table);
				if (atrans instanceof TimeAttrTransformer) {
					((TimeAttrTransformer) atrans).setAttributeDescriptions(vvd);
				} else if (prevTrans != null) {
					atrans.setColumnNumbers(prevTrans.getColumnNumbers());
				} else {
					atrans.setAttributes(vaid);
				}
				if (!atrans.isValid()) {
					continue;
				}
				if (prevTrans != null) {
					atrans.setPreviousTransformer(prevTrans);
				}
				atrans.setAllowIndividualTransformation(vis.getAllowTransformIndividually());
				prevTrans = atrans;
				if (firstTrans == null) {
					firstTrans = atrans;
				}
			}
			if (firstTrans != null) {
				vis.setAttributeTransformer(firstTrans, true);
			}
			vis.setup();
			if (core.getDisplayProducer().displayOnMap(vis, "value_flow", table, vaid, layer, true, mapView) == null) {
				core.getUI().showMessage(core.getDisplayProducer().getErrorMessage(), true);
				return;
			}
			core.getSupervisor().registerTool(vis);
		}
		core.getUI().clearStatusLine();
	}

	/**
	 * Visualizes the specified time-dependent attribute(s) from the given table
	 * on an animated map. Returns 0 if successfully done, 1 if failed or the
	 * user cancelled the visualization, and -1 if the button "Back" has been
	 * pressed.
	 */
	protected int buildAnimatedMap(AttributeDataPortion table, Parameter par, GeoLayer layer, Vector attrDescr) {
		if (table == null || par == null || attrDescr == null || attrDescr.size() < 1 || layer == null)
			return 1;
		if (core.getUI() == null) {
			System.out.println("ERROR: " + res.getString("no_ui"));
			return 1;
		}
		DataMapper dataMapper = (DataMapper) core.getDisplayProducer().getDataMapper();
		AnimMapChoosePanel p = new AnimMapChoosePanel(table, layer, par, attrDescr, dataMapper, core.getSupervisor());
		Frame mainFrame = null;
		if (core.getUI() != null) {
			mainFrame = core.getUI().getMainFrame();
		}
		if (mainFrame == null) {
			mainFrame = CManager.getAnyFrame();
		}
		OKDialog selDia = new OKDialog(mainFrame, res.getString("vis_method"), true, true);
		selDia.addContent(p);
		selDia.show();
		if (selDia.wasCancelled())
			return 1;
		if (selDia.wasBackPressed())
			return -1;
		String methodId = p.getMapVisMethodId();
		if (methodId == null)
			return -1;
		Object vis = dataMapper.constructVisualizer(methodId, layer.getType());
		if (vis == null) {
			core.getUI().showMessage(dataMapper.getErrorMessage(), true);
			return 1;
		}
		Object mapViewSelection = p.getMapViewSelection();
		Vector selAttrDescr = p.geAttributeDescriptors();
		Vector selAttrIds = p.getAttrIds();
		if ((vis instanceof DataPresenter) || (vis instanceof TableClassifier)) {
			DataPresenter dpres = null;
			TableClassifier tcl = null;
			boolean allowTransform = false;
			if (vis instanceof DataPresenter) {
				dpres = (DataPresenter) vis;
				Vector attrNames = new Vector(selAttrIds.size(), 1);
				for (int i = 0; i < selAttrDescr.size(); i++) {
					VisAttrDescriptor ad = (VisAttrDescriptor) selAttrDescr.elementAt(i);
					attrNames.addElement(ad.getName());
				}
				dpres.setAttributes(selAttrIds, attrNames);
				allowTransform = dpres.getAllowTransform();
			} else {
				tcl = (TableClassifier) vis;
				tcl.setAttributes(selAttrIds);
				allowTransform = tcl.getAllowTransform();
			}
			if (allowTransform) {
				Vector v = new Vector(selAttrDescr.size());
				for (int i = 0; i < selAttrDescr.size(); i++) {
					VisAttrDescriptor ad = (VisAttrDescriptor) selAttrDescr.elementAt(i);
					v.addElement(ad.attrId);
				}
				AttributeTransformer prevTrans = null, firstTrans = null;
				for (int i = 0; i < TransformerGenerator.getTransformerCount(); i++) {
					AttributeTransformer atrans = TransformerGenerator.makeTransformer(i);
					if (atrans == null) {
						continue;
					}
					atrans.setDataTable(table);
					if (atrans instanceof TimeAttrTransformer) {
						((TimeAttrTransformer) atrans).setAttributeDescriptions(selAttrDescr);
					} else if (prevTrans != null) {
						atrans.setColumnNumbers(prevTrans.getColumnNumbers());
					} else {
						atrans.setAttributes(v);
					}
					if (!atrans.isValid()) {
						continue;
					}
					if (prevTrans != null) {
						atrans.setPreviousTransformer(prevTrans);
					}
					if (dpres != null) {
						atrans.setAllowIndividualTransformation(dpres.getAllowTransformIndividually());
					} else {
						atrans.setAllowIndividualTransformation(tcl.getAllowTransformIndividually());
					}
					prevTrans = atrans;
					if (firstTrans == null) {
						firstTrans = atrans;
					}
				}
				if (firstTrans != null)
					if (dpres != null) {
						dpres.setAttributeTransformer(firstTrans, true);
					} else {
						tcl.setAttributeTransformer(firstTrans, true);
					}
			}
			boolean useMainView = false;
			MapViewer mapView = null;
			if (mapViewSelection == null) {
				mapViewSelection = "auto";
			}
			if (mapViewSelection instanceof MapViewer) {
				mapView = (MapViewer) mapViewSelection;
			} else {
				String mLoc = (String) mapViewSelection;
				if (mLoc.equals("main")) {
					mapView = core.getUI().getMapViewer("main");
					useMainView = true;
				} else if (mLoc.equals("new")) {
					mapView = core.getUI().getMapViewer("_blank_");
				} else {
					Visualizer lvis = layer.getVisualizer(), lbvis = layer.getBackgroundVisualizer();
					boolean layerHasVisualizer = lvis != null || lbvis != null;
					if (layerHasVisualizer && layer.getType() == Geometry.area && (lvis == null || lbvis == null))
						if ((vis instanceof Visualizer) && ((Visualizer) vis).isDiagramPresentation()) {
							layerHasVisualizer = lvis != null && lvis.isDiagramPresentation();
						} else {
							layerHasVisualizer = lvis == null || !lvis.isDiagramPresentation();
						}
					useMainView = !layerHasVisualizer || !core.getUI().getUseNewMapForNewVis();
					mapView = core.getUI().getMapViewer((useMainView) ? "main" : "_blank_");
				}
			}
			if (mapView == null || mapView.getLayerManager() == null) {
				core.getUI().showMessage(res.getString("no_map_view"), true);
				return 1;
			}
			if (!useMainView) {
				// find the copy of the geographical layer in the new map view
				int lidx = mapView.getLayerManager().getIndexOfLayer(layer.getContainerIdentifier());
				if (lidx < 0) {
					core.getUI().showMessage(res.getString("no_layer_in_new_map"), true);
					return 1;
				}
				layer = mapView.getLayerManager().getGeoLayer(lidx);
			}
			Visualizer pres = core.getDisplayProducer().displayOnMap(vis, methodId, table, selAttrIds, layer, false, mapView);
			if (pres == null) {
				core.getUI().showMessage(core.getDisplayProducer().getErrorMessage(), true);
				return 1;
			}
			if (!useMainView && (mapView instanceof Component)) {
				Component c = (Component) mapView;
				c.setName(c.getName() + ": " + pres.getVisualizationName() + " (" + res.getString("animation") + ")");
				Frame win = CManager.getFrame(c);
				if (win != null) {
					win.setName(c.getName());
					win.setTitle(c.getName());
				}
			}
			DataVisAnimator anim = new DataVisAnimator();
			if (dpres != null) {
				if (!anim.setup(table, selAttrDescr, par, dpres)) {
					anim = null;
				}
			} else if (tcl != null) {
				if (!anim.setup(table, selAttrDescr, par, tcl)) {
					anim = null;
				}
			}
			if (anim != null) {
				anim.setWrapVisualizer(pres);
			}
			core.getDisplayProducer().makeMapManipulator(pres, methodId, table, layer, mapView);
			if (anim != null) {
				FocusInterval fint = getFocusInterval(par);
				anim.setFocusInterval(fint);
				core.getSupervisor().registerTool(anim);
			}
		}
		return 0;
	}

	/**
	 * Reacts to the time panel being destroyed.
	 */
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getSource().equals(timeFilterControlPanel) && e.getPropertyName().equals("destroyed")) {
			notifyPropertyChange("close_time_filter_controls");
			timeFilterControlPanel = null;
		} else if (e.getSource().equals(displayTimeControlPanel) && e.getPropertyName().equals("destroyed")) {
			notifyPropertyChange("close_display_time_controls");
			displayTimeControlPanel = null;
		} else if (e.getSource().equals(timeMan) && e.getPropertyName().equals("all_data_removed")) {
			if (displayTimeControlPanel != null && timeMan.getTemporalTableCount() < 1) {
				if (!displayTimeControlPanel.isDestroyed()) {
					displayTimeControlPanel.destroy();
				}
				notifyPropertyChange("close_display_time_controls");
				displayTimeControlPanel = null;
			}
			if (timeFilterControlPanel != null && timeMan.getContainerCount() < 1) {
				if (!timeFilterControlPanel.isDestroyed()) {
					timeFilterControlPanel.destroy();
				}
				notifyPropertyChange("close_time_filter_controls");
				timeFilterControlPanel = null;
			}
		}
	}

	/**
	 * Returns the panel with display time controls, if already exists.
	 */
	public TimeControlPanel getDisplayTimeControls() {
		return displayTimeControlPanel;
	}

	/**
	 * Returns the panel with time filter controls, if already exists.
	 */
	public TimeControlPanel getTimeFilterControls() {
		return timeFilterControlPanel;
	}

	/**
	 * Registers a listener of opening or closing of the time control panels
	 */
	public synchronized void addPropertyChangeListener(PropertyChangeListener l) {
		if (l == null)
			return;
		if (listeners == null) {
			listeners = new Vector(5, 5);
		}
		if (!listeners.contains(l)) {
			listeners.addElement(l);
		}
	}

	/**
	 * Removes the listener of opening or closing of the time control panels
	 */
	public synchronized void removePropertyChangeListener(PropertyChangeListener l) {
		if (l == null || listeners == null)
			return;
		listeners.removeElement(l);
	}

	protected void notifyPropertyChange(String propName) {
		if (listeners == null || listeners.size() < 1)
			return;
		PropertyChangeEvent pce = new PropertyChangeEvent(this, propName, null, null);
		for (int i = 0; i < listeners.size(); i++) {
			((PropertyChangeListener) listeners.elementAt(i)).propertyChange(pce);
		}
	}

	public static void main(String[] args) {
		try {
			String command = "cmd /c C:\\Programme\\R\\R-2.10.1\\bin\\RScript c:\\R\\test.R deltaI=15 dataInS=C:\\R\\data.txt dataOutS=C:\\R\\results.csv"; // Rscript
			// C:\\\\R\\\\start";// < "
			// +
			// RScript;
			// + " " + Path + FileToR + " " + Path + FileFromR;
			System.out.println("R system call starting: " + command);
			Process p = Runtime.getRuntime().exec(command);

			int exit_value = p.waitFor();
			System.out.println("R system call ending with " + exit_value);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}

	}
}
