package core;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.generators.DataDisplayer;
import spade.analysis.generators.QueryAndSearchToolProducer;
import spade.analysis.plot.QueryOrSearchTool;
import spade.analysis.system.DisplayProducer;
import spade.analysis.system.Supervisor;
import spade.analysis.system.SystemUI;
import spade.analysis.system.ToolManager;
import spade.analysis.system.WindowManager;
import spade.analysis.transform.TransformerGenerator;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.LayoutSelector;
import spade.lib.basicwin.OKDialog;
import spade.lib.help.Helper;
import spade.lib.lang.Language;
import spade.lib.util.Parameters;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.DataTreater;
import spade.vis.database.ObjectContainer;
import spade.vis.map.MapViewer;
import spade.vis.mapvis.DataMapper;
import spade.vis.mapvis.DataPresenter;
import spade.vis.mapvis.SimpleDataMapper;
import spade.vis.mapvis.Visualizer;
import spade.vis.space.GeoLayer;
import spade.vis.spec.SaveableTool;
import spade.vis.spec.ToolSpec;

public class DisplayProducerImplement implements DisplayProducer, WindowManager, ToolManager, WindowListener {
	static ResourceBundle res = Language.getTextResource("core.Res");
	/**
	* Windows with different graphical displays opened during the session.
	* Elements of the Vector are instances of Window or its subclasses (frames or
	* dialogs).
	*/
	protected Vector windows = null;
	/**
	* Frame with graphical data displays
	*/
	protected Frame graphFrame = null;
	/**
	* The panel that contains all graphical displays (plots), but not
	* query and navigation tools
	*/
	protected LayoutSelector graphPanel = null;
	/**
	* Frame with query and navigation tools
	*/
	protected Frame queryFrame = null;
	/**
	* The panel that contains all query and navigation tools
	*/
	protected LayoutSelector queryPanel = null;
	/**
	* A DisplayProducer provides access from non-UI modules to the system UI
	* (that may vary from configuration to configuration). This helps to make
	* non-UI modules independent from current UI implementation
	*/
	protected SystemUI ui = null;
	/**
	* When a display is created, it is supplied with a reference to the supervisor
	* that links together all the displays
	*/
	protected Supervisor sup = null;
	/**
	* Used for representation of data on a map, i.e. for construction of
	* data visualizers and corresponding manipulators
	*/
	protected DataMapper dataMapper = null;
	/**
	* Used for construction of various non-cartographic data displays (plots)
	*/
	protected DataDisplayer dataDisplayer = new DataDisplayer();
	/**
	* Used for construction of query and search tools such as dynamic queries
	* and object lists
	*/
	protected QueryAndSearchToolProducer qsProducer = new QueryAndSearchToolProducer();
	/**
	* Contains the error message
	*/
	protected String err = null;
	/**
	* Contains listeners of appearing particular windows, such as the window
	* with non-cartographical displays or with a dynamic query.
	*/
	protected Vector winList = null;

	/**
	* Provides access from non-UI modules to the system UI (that may vary
	* from configuration to configuration). This helps to make non-UI modules
	* independent from current UI implementation
	*/
	@Override
	public SystemUI getUI() {
		return ui;
	}

	public void setUI(SystemUI ui) {
		this.ui = ui;
	}

	/**
	* When a display is created, it is supplied with a reference to the supervisor
	* that links together all the displays. This method sets the supervisor
	* to be used for these purposes.
	*/
	public void setSupervisor(Supervisor sup) {
		this.sup = sup;
	}

	/**
	* Returns the supervisor that links together all displays
	*/
	@Override
	public Supervisor getSupervisor() {
		return sup;
	}

	/**
	* Returns the number of available query and search tools such as dynamic query
	*/
	@Override
	public int getQueryAndSearchToolCount() {
		return qsProducer.getAvailableToolCount();
	}

	/**
	* Returns the identifier of the available query or search tool with the given
	* index
	*/
	@Override
	public String getQueryOrSearchToolId(int idx) {
		return qsProducer.getAvailableToolId(idx);
	}

	/**
	* Returns the name of the available query or search tool with the given
	* index
	*/
	@Override
	public String getQueryOrSearchToolName(int idx) {
		return qsProducer.getAvailableToolName(idx);
	}

	/**
	* Returns the name of the available query or search tool with the given
	* identifier
	*/
	@Override
	public String getQueryOrSearchToolName(String toolId) {
		return qsProducer.getAvailableToolName(qsProducer.getAvailableToolIndex(toolId));
	}

	/**
	* Replies whether the tool with the given identifier requires attributes
	* for its operation.
	*/
	@Override
	public boolean isToolAttributeFree(String toolId) {
		if (toolId == null)
			return false;
		if (qsProducer.isToolAvailable(toolId))
			return qsProducer.isToolAttributeFree(toolId);
		if (dataDisplayer.isMethodAvailable(toolId))
			return DataDisplayer.isMethodAttributeFree(toolId);
		return false;
	}

	/**
	 * Replies whether the tool with the given identifier implements the interface
	 * ObjectsSuitabilityChecker, i.e. can check if a given object container is
	 * suitable for this tool
	 */
	@Override
	public boolean canToolCheckObjectsSuitability(String toolId) {
		if (toolId == null)
			return false;
		if (qsProducer.isToolAvailable(toolId))
			return qsProducer.canToolCheckObjectsSuitability(toolId);
		return false;
	}

	/**
	 * Generates an instance of the tool with the given identifier
	 */
	@Override
	public Object makeToolInstance(String toolId) {
		if (toolId == null)
			return false;
		if (qsProducer.isToolAvailable(toolId))
			return qsProducer.makeToolInstance(toolId);
		return null;
	}

	/**
	* Creates a query or search tool with the given identifier. Returns a
	* reference to the tool constructed or null if failed.
	*/
	@Override
	public Object makeQueryOrSearchTool(String toolId, ObjectContainer oCont) {
		return makeQueryOrSearchTool(toolId, oCont, null);
	}

	/**
	* Creates the query or search tool with the given identifier and the specified
	* attributes. Returns a reference to the tool constructed or null if failed.
	*/
	public Object makeQueryOrSearchTool(String toolId, ObjectContainer oCont, Vector attributes) {
		if (oCont == null || toolId == null)
			return null;
		String className = qsProducer.getToolKeeper().getToolClassName(toolId);
		if (className == null) {
			if (sup != null && sup.getUI() != null) {
				sup.getUI().showMessage(toolId + res.getString("_unknown_tool_"), true);
			}
			return null;
		}
		//check whether such tool has been already created for this container
		QueryOrSearchTool tool = null;
		if (queryPanel != null && queryPanel.getElementCount() > 0) {
			boolean found = false;
			for (int i = 0; i < queryPanel.getElementCount() && !found; i++) {
				Component c = queryPanel.getElement(i);
				if (c != null && c.getClass().getName().equals(className) && (c instanceof QueryOrSearchTool)) {
					tool = (QueryOrSearchTool) c;
					found = oCont.equals(tool.getObjectContainer());
				}
			}
			if (found) {
				queryFrame.toFront();
				if (sup != null && sup.getUI() != null) {
					sup.getUI().showMessage(res.getString("already_open"), false);
				}
				return tool;
			}
		}
		tool = qsProducer.constructTool(toolId, oCont, attributes, sup);
		if (tool == null) {
			if (sup != null && sup.getUI() != null) {
				sup.getUI().showMessage(qsProducer.getErrorMessage(), true);
			}
			return null;
		}
		if (tool instanceof SaveableTool) {
			sup.registerTool((SaveableTool) tool);
		}
		if (queryPanel == null) {
			queryPanel = new LayoutSelector(LayoutSelector.VERTICAL);
			queryPanel.setTagName("query_window");
			sup.registerTool(queryPanel);
		}
		queryPanel.addElement((Component) tool);
		if (queryFrame == null) {
			queryFrame = makeWindow(queryPanel, res.getString("Query_search"));
			queryPanel.setOwner(queryFrame);
			notifyWindowCreated("query_frame", queryFrame);
		} else {
			Dimension dp = queryFrame.getPreferredSize(), d = queryFrame.getSize(), ss = Toolkit.getDefaultToolkit().getScreenSize();
			if (dp.width > ss.width * 3 / 4) {
				dp.width = ss.width * 3 / 4;
			}
			if (dp.height > ss.height * 3 / 4) {
				dp.height = ss.height * 3 / 4;
			}
			if (d.width < dp.width || d.height < dp.height) {
				if (d.width < dp.width) {
					d.width = dp.width;
				}
				if (d.height < dp.height) {
					d.height = dp.height;
				}
				Rectangle bounds = queryFrame.getBounds();
				bounds.width = d.width;
				bounds.height = d.height;
				if (bounds.x + bounds.width > ss.width) {
					bounds.x = ss.width - bounds.width;
				}
				if (bounds.y + bounds.height > ss.height) {
					bounds.y = ss.height - bounds.height;
				}
				queryFrame.setBounds(bounds);
			}
		}
		CManager.validateAll(queryPanel);
		queryFrame.toFront();
		return tool;
	}

	/**
	* Returns the number of available display methods (excluding map
	* visualization methods, dynamic query and other query/search/navigation
	* tools)
	*/
	@Override
	public int getDisplayMethodCount() {
		return dataDisplayer.getAvailableMethodCount();
	}

	/**
	* Returns the name of the display method with the given index (to be shown
	* to the user)
	*/
	@Override
	public String getDisplayMethodName(int idx) {
		return dataDisplayer.getAvailableMethodName(idx);
	}

	/**
	* Returns the identifier (internal) of the display method with the given index
	*/
	@Override
	public String getDisplayMethodId(int idx) {
		return dataDisplayer.getAvailableMethodId(idx);
	}

	/**
	* Checks if the display method with the given identifier is applicable to the
	* given data
	*/
	@Override
	public boolean isDisplayMethodApplicable(int methodN, AttributeDataPortion dtab, Vector attr) {
		if (dataDisplayer.isDisplayMethodApplicable(dataDisplayer.getAvailableMethodId(methodN), dtab, attr))
			return true;
		err = dataDisplayer.getErrorMessage();
		return false;
	}

	/**
	* Replies if the given display type is attribute-free (i.e. does not visualize
	* any attributes)
	*/
	@Override
	public boolean isDisplayMethodAttributeFree(String methodId) {
		return DataDisplayer.isMethodAttributeFree(methodId);
	}

	/**
	* Applies the display method with the given index to the data in order to
	* generate a data display. Returns the resulting display (as a Component)
	*/
	@Override
	public Component makeDisplay(AttributeDataPortion dtab, Vector attr, //attributes to visualize
			int methodN) //N of vis. method to apply
	{
		return makeDisplay(dtab, attr, dataDisplayer.getAvailableMethodId(methodN), null);
	}

	/**
	* Applies the display method with the given identifier to the data in order to
	* generate a data display. Returns the resulting display (as a Component)
	* The argument properties may specify individual properties for the
	* display to be constructed.
	*/
	@Override
	public Component makeDisplay(AttributeDataPortion dtab, Vector attr, //attributes to visualize
			String methodId, //identifier of vis. method to apply
			Hashtable properties) {
		Component c = dataDisplayer.makeDisplay(sup, dtab, attr, methodId, properties);
		if (c == null) {
			err = dataDisplayer.getErrorMessage();
			sup.getUI().showMessage(err, true);
		} else {
			sup.notifyGlobalPropertyChange(Supervisor.eventDisplayedAttrs);
		}
		return c;
	}

	/**
	* Tries to produce a graphical display according to the given specification.
	* Returns the resulting display (as a Component).
	* If the graphic cannot be produced, returns null.
	*/
	@Override
	public Component makeDisplay(ToolSpec spec, AttributeDataPortion dtab) {
		Component c = dataDisplayer.makeDisplay(spec, sup, dtab);
		if (c == null) {
			err = dataDisplayer.getErrorMessage();
			sup.getUI().showMessage(err, true);
		} else {
			sup.notifyGlobalPropertyChange(Supervisor.eventDisplayedAttrs);
		}
		return c;
	}

	/**
	* Puts the graphical display into the frame where all graphics are shown.
	* If such a frame does not exist yet, it is created
	*/
	@Override
	public void showGraph(Component c) {
		if (c == null)
			return;
		if (graphPanel == null) {
			graphPanel = new LayoutSelector(LayoutSelector.WINDOWS);
			graphPanel.setTagName("chart_window");
			sup.registerTool(graphPanel);
		}
		graphPanel.addElement(c);
		//if (graphPanel==null) graphPanel=new MDIPanel();
		//graphPanel.addWindow(c);
		if (graphFrame == null) {
			// following string: "Graphical analysis tools"
			graphFrame = makeWindow(graphPanel, res.getString("Graphical_analysis"));
			graphPanel.setOwner(graphFrame);
			graphFrame.setState(Frame.ICONIFIED);
			notifyWindowCreated("graph_frame", graphFrame);
		} else {
			Dimension dp = graphFrame.getPreferredSize(), d = graphFrame.getSize(), ss = Toolkit.getDefaultToolkit().getScreenSize();
			if (dp.width > ss.width * 3 / 4) {
				dp.width = ss.width * 3 / 4;
			}
			if (dp.height > ss.height * 3 / 4) {
				dp.height = ss.height * 3 / 4;
			}
			if (d.width < dp.width || d.height < dp.height) {
				if (d.width < dp.width) {
					d.width = dp.width;
				}
				if (d.height < dp.height) {
					d.height = dp.height;
				}
				Rectangle bounds = graphFrame.getBounds();
				bounds.width = d.width;
				bounds.height = d.height;
				if (bounds.x + bounds.width > ss.width) {
					bounds.x = ss.width - bounds.width;
				}
				if (bounds.y + bounds.height > ss.height) {
					bounds.y = ss.height - bounds.height;
				}
				graphFrame.setBounds(bounds);
			}
		}
		CManager.validateAll(graphPanel);
		graphFrame.toFront();
	}

	/**
	* Applies one of available visualization generators to generate a data
	* display. Puts the resulting display (a Component) in the common frame
	* containing all graphics. Returns a reference to the component built.
	*/
	@Override
	public Object display(AttributeDataPortion dtab, Vector attr, //attributes to visualize
			int methodN) //N of vis. method to apply
	{
		Component c = makeDisplay(dtab, attr, methodN);
		showGraph(c);
		return c;
	}

	/**
	* Applies the display method with the given identifier to the data in order to
	* generate a data display. Displays the resulting component on the screen.
	* The argument properties may specify individual properties for the
	* display to be constructed. Returns a reference to the component built.
	*/
	@Override
	public Object display(AttributeDataPortion dtab, Vector attr, //attributes to visualize
			String methodId, //N of vis. method to apply
			Hashtable properties) {
		Component c = makeDisplay(dtab, attr, methodId, properties);
		showGraph(c);
		return c;
	}

	/**
	* Constructs a frame with the given title containing the given component
	*/
	@Override
	public Frame makeWindow(Component c, String title) {
		Frame win = new Frame(title);
		win.setLayout(new BorderLayout());
		win.add(c, BorderLayout.CENTER);
		return addAndShowWindow(win);
	}

	/**
	* Constructs a frame with the given title and the desired size containing the
	* given component.
	*/
	@Override
	public Frame makeWindow(Component c, String title, int width, int height) {
		Frame win = new Frame(title);
		win.setLayout(new BorderLayout());
		win.add(c, BorderLayout.CENTER);
		win.setSize(width, height);
		return addAndShowWindow(win);
	}

//ID
	/**
	* Returns a reference to the frame with non-cartographical displays (if exists).
	*/
	@Override
	public Frame getChartFrame() {
		return graphFrame;
	}

//~ID
	/**
	* Returns a reference to the frame with query tools (if exists).
	*/
	@Override
	public Frame getQueryFrame() {
		return queryFrame;
	}

	protected Frame addAndShowWindow(Frame win) {
		if (win == null)
			return null;
		Dimension d = win.getSize(), ss = Toolkit.getDefaultToolkit().getScreenSize();
		if (d == null || d.width < 50 || d.height < 30) {
			win.pack();
			d = win.getSize();
		}
		if (d.width > ss.width * 3 / 4) {
			d.width = ss.width * 3 / 4;
		}
		if (d.height > ss.height * 3 / 4) {
			d.height = ss.height * 3 / 4;
		}
		int x = ss.width - d.width, y = 0;
		if (windows != null && windows.size() > 0) {
			Window win1 = (Window) windows.elementAt(windows.size() - 1);
			Rectangle r = win1.getBounds();
			y = r.y + r.height;
		}
		if (y + d.height > ss.height) {
			y = ss.height - d.height;
		}
		win.setBounds(x, y, d.width, d.height);
		registerWindow(win);
		if (allWindowsVisible) {
			win.setVisible(true);
		}
		return win;
	}

	/**
	* Returns the dataMapper used for representation of data on a map
	*/
	@Override
	public SimpleDataMapper getDataMapper() {
		if (dataMapper == null) {
			dataMapper = new DataMapper();
			if (sup != null && sup.getSystemSettings() != null) {
				dataMapper.setMethodSwitch((Parameters) sup.getSystemSettings().getParameter("MapVisMethods"));
			}
		}
		return dataMapper;
	}

	/**
	* Visualizes the specified attributes on the given map layers by the
	* "default" method selected by the DataMapper according to numbers and
	* types of attributes. Returns the Visualizer constructed.
	*/
	@Override
	public Visualizer displayOnMap(AttributeDataPortion dtab, Vector attr, //the attributes to visualize on the map
			GeoLayer themLayer, //the layer to send the presentation
			MapViewer mapView) //where to add map manipulator
	{
		if (dtab == null || attr == null || attr.size() < 1 || themLayer == null)
			return null;
		getDataMapper();
		String methodId = dataMapper.getDefaultMethodId(dtab, attr, themLayer.getType());
		if (methodId == null)
			return null;
		return displayOnMap(methodId, dtab, attr, themLayer, mapView);
	}

	/**
	* Visualizes the specified attributes on the given map layers by the method
	* specified through its identifier. Returns the Visualizer constructed.
	*/
	@Override
	public Visualizer displayOnMap(String methodId, AttributeDataPortion dtab, Vector attr, //the attributes to visualize on the map
			GeoLayer themLayer, //the layer to send the presentation
			MapViewer mapView) //where to add map manipulator
	{
		if (methodId == null || dtab == null || attr == null || attr.size() < 1 || themLayer == null)
			return null;
		getDataMapper();
		Object vis = dataMapper.constructVisualizer(methodId, themLayer.getType());
		if (vis == null) {
			sup.getUI().showMessage(dataMapper.getErrorMessage(), true);
			return null;
		}
		TransformerGenerator.makeTransformerChain(vis, dtab, attr);
		return displayOnMap(vis, methodId, dtab, attr, themLayer, true, mapView);
	}

	/**
	* Visualizes the specified attributes on the given map layers using the given
	* visualizer or classifier (previously constructed). By default, creates a
	* map manipulator. Returns the Visualizer constructed.
	*/
	@Override
	public Visualizer displayOnMap(Object visualizer, String methodId, AttributeDataPortion dtab, Vector attr, //the attributes to visualize on the map
			GeoLayer themLayer, //the layer to send the presentation
			MapViewer mapView) //where to add map manipulator
	{
		return displayOnMap(visualizer, methodId, dtab, attr, themLayer, true, mapView);
	}

	/**
	* Visualizes the specified attributes on the given map layers using the given
	* visualizer or classifier (previously constructed). Returns the Visualizer
	* constructed. The DataMapper is responsible for creating a Visualizer
	* from the given Classifier.
	*/
	@Override
	public Visualizer displayOnMap(Object visualizer, String methodId, AttributeDataPortion dtab, Vector attr, //the attributes to visualize on the map
			GeoLayer themLayer, //the layer to send the presentation
			boolean makeManipulator, //whether to create a manipulator
			MapViewer mapView) //where to add map manipulator
	{
		System.out.println("displayOnMap: methodId = " + methodId + "; visualizer = " + visualizer + "; mapView = " + mapView);
		if (methodId == null || visualizer == null || mapView == null)
			return null;
		getDataMapper();
		Visualizer vis = null;
		if (attr != null && attr.size() > 0) {
			vis = dataMapper.visualizeAttributes(visualizer, methodId, dtab, attr, themLayer.getType());
		}
		if (vis == null) {
			sup.getUI().showMessage(dataMapper.getErrorMessage(), true);
			System.out.println(dataMapper.getErrorMessage());
			return null;
		}
		System.out.println("Visualizer constructed!");
		vis.setLocation((mapView.getIsPrimary()) ? "main" : mapView.getIdentifier());
		if (vis instanceof DataPresenter) {
			((DataPresenter) vis).setAttrColorHandler(sup.getAttrColorHandler());
		}
		boolean otherTable = !themLayer.hasThematicData(dtab);
		if (otherTable) {
			themLayer.receiveThematicData(dtab);
			if (dtab.getObjectFilter() != null) {
				themLayer.setThematicFilter(dtab.getObjectFilter());
			}
		}
		boolean allowBkgVis = sup.getSystemSettings().checkParameterValue("Allow_Background_Visualization", "true");
		Visualizer oldVis = themLayer.getVisualizer(), oldBkgVis = themLayer.getBackgroundVisualizer();
		if (oldVis != null && (!allowBkgVis || otherTable || oldVis.isDiagramPresentation() == vis.isDiagramPresentation())) {
			if (oldVis instanceof DataTreater) {
				sup.removeDataDisplayer((DataTreater) vis);
			}
			mapView.removeMapManipulator(oldVis, themLayer.getContainerIdentifier());
			oldVis.destroy();
			oldVis = null;
		}
		if (oldBkgVis != null && (!allowBkgVis || otherTable || !vis.isDiagramPresentation())) {
			if (oldBkgVis instanceof DataTreater) {
				sup.removeDataDisplayer((DataTreater) oldBkgVis);
			}
			mapView.removeMapManipulator(oldBkgVis, themLayer.getContainerIdentifier());
			oldBkgVis.destroy();
			oldBkgVis = null;
		}
		if (oldVis != null)
			if (!oldVis.isDiagramPresentation()) {
				themLayer.setVisualizer(vis);
				themLayer.setBackgroundVisualizer(oldVis);
			} else {
				themLayer.setBackgroundVisualizer(vis);
			}
		else {
			themLayer.setVisualizer(vis);
		}
		themLayer.setLayerDrawn(true);
		if (vis instanceof DataTreater) {
			sup.registerDataDisplayer((DataTreater) vis);
		}
		getUI().bringMapToTop(mapView);
		sup.notifyGlobalPropertyChange(Supervisor.eventDisplayedAttrs);
		if (makeManipulator) {
			makeMapManipulator(vis, methodId, dtab, themLayer, mapView);
		}
		return vis;
	}

	/**
	 * Attaches the given visualizer to the given layer without producing a
	 * manipulator.
	 */
	@Override
	public void setVisualizerInLayer(Visualizer vis, GeoLayer layer, MapViewer mapView) {
		if (vis == null || layer == null || mapView == null)
			return;
		vis.setLocation((mapView.getIsPrimary()) ? "main" : mapView.getIdentifier());
		if (vis instanceof DataPresenter) {
			((DataPresenter) vis).setAttrColorHandler(sup.getAttrColorHandler());
		}
		boolean allowBkgVis = sup.getSystemSettings().checkParameterValue("Allow_Background_Visualization", "true");
		Visualizer oldVis = layer.getVisualizer(), oldBkgVis = layer.getBackgroundVisualizer();
		if (oldVis != null && (!allowBkgVis || oldVis.isDiagramPresentation() == vis.isDiagramPresentation())) {
			if (oldVis instanceof DataTreater) {
				sup.removeDataDisplayer((DataTreater) vis);
			}
			mapView.removeMapManipulator(oldVis, layer.getContainerIdentifier());
			oldVis.destroy();
			oldVis = null;
		}
		if (oldBkgVis != null && (!allowBkgVis || !vis.isDiagramPresentation())) {
			if (oldBkgVis instanceof DataTreater) {
				sup.removeDataDisplayer((DataTreater) oldBkgVis);
			}
			mapView.removeMapManipulator(oldBkgVis, layer.getContainerIdentifier());
			oldBkgVis.destroy();
			oldBkgVis = null;
		}
		if (oldVis != null)
			if (!oldVis.isDiagramPresentation()) {
				layer.setVisualizer(vis);
				layer.setBackgroundVisualizer(oldVis);
			} else {
				layer.setBackgroundVisualizer(vis);
			}
		else {
			layer.setVisualizer(vis);
		}
		layer.setLayerDrawn(true);
		if (vis instanceof DataTreater) {
			sup.registerDataDisplayer((DataTreater) vis);
		}
		getUI().bringMapToTop(mapView);
	}

	/**
	* Creates an appropriate manipulator for the given visualizer and adds it to
	* the map window.
	*/
	@Override
	public void makeMapManipulator(Object visualizer, String methodId, AttributeDataPortion dtab, GeoLayer themLayer, //the layer to manipulate
			MapViewer mapView) //where to add map manipulator
	{
		if (visualizer == null || methodId == null || themLayer == null || mapView == null)
			return;
		if (!(visualizer instanceof Visualizer))
			return;
		Visualizer vis = (Visualizer) visualizer;
		Component manipulator = dataMapper.getMapManipulator(methodId, vis, sup, dtab);
		if (manipulator != null) {
			mapView.addMapManipulator(manipulator, vis, themLayer.getContainerIdentifier());
		}
	}

	/**
	* Erases visualization of thematic data on the specified GeoLayer
	*/
	@Override
	public void eraseDataFromMap(GeoLayer themLayer, //the layer to erase presentation
			MapViewer mapView) //from where to remove map manipulator
	{
		for (int i = 0; i < 2; i++) {
			Visualizer oldVis = (i == 0) ? themLayer.getVisualizer() : themLayer.getBackgroundVisualizer();
			if (oldVis != null) {
				if (oldVis instanceof DataTreater) {
					sup.removeDataDisplayer((DataTreater) oldVis);
				}
				mapView.removeMapManipulator(oldVis, themLayer.getContainerIdentifier());
				oldVis.destroy();
			}
		}
		themLayer.setVisualizer(null);
		themLayer.setBackgroundVisualizer(null);
		sup.notifyGlobalPropertyChange(Supervisor.eventDisplayedAttrs);
	}

//----------------- implementation of the ToolManager interface ----------------
	/**
	* Check availability of a data analysis tool  with the given identifier.
	* The identifier of the tool should be one of "dot_plot","scatter_plot",
	* "parallel_coordinates","attribute_query","dynamic_query","find_by_name",
	* "classify" ("freehand" classification!).
	* More identifiers may be added in the future.
	*/
	@Override
	public boolean isToolAvailable(String toolId) {
		if (toolId == null)
			return false;
		if (toolId.equals("attribute_query")) {
			toolId = "dynamic_query";
		} else if (toolId.equals("find_by_name")) {
			toolId = "object_index";
		}
		if (dataDisplayer.isMethodAvailable(toolId))
			return true;
		if (qsProducer.isToolAvailable(toolId))
			return true;
		return false;
	}

	/**
	* Replies whether help about the specified tool is available in the system
	*/
	@Override
	public boolean canHelpWithTool(String toolId) {
		return Helper.canHelp(toolId);
	}

	/**
	* Displays help about the specified tool
	*/
	@Override
	public void helpWithTool(String toolId) {
		Helper.help(toolId);
	}

	/**
	* Checks whether the specified analysis tool is applicable to the
	* given table and the given attributes
	*/
	@Override
	public boolean isToolApplicable(String toolId, AttributeDataPortion table, Vector attrs) {
		err = null;
		if (toolId.equals("attribute_query")) {
			toolId = "dynamic_query";
		} else if (toolId.equals("find_by_name")) {
			toolId = "object_index";
		}
		if (qsProducer.isToolAvailable(toolId))
			return true; //assuming that a query or search tool ccan always be constructed
		if (!dataDisplayer.isMethodAvailable(toolId)) {
			// following string: "Unknown analysis tool requested: "
			err = res.getString("Unknown_analysis_tool") + toolId;
			return false;
		}
		boolean result = dataDisplayer.isDisplayMethodApplicable(toolId, table, attrs);
		if (!result) {
			err = dataDisplayer.getErrorMessage();
		}
		return result;
	}

	/**
	* Applies the specified analysis tool to the given table and the given
	* attributes. Returns a reference to the tool constructed or null if failed.
	* The tool may take into account the table filter passed to it.
	* The argument properties may specify individual properties for the
	* tool to be constructed.
	*/
	@Override
	public Object applyTool(String toolId, AttributeDataPortion table, Vector attrs, Hashtable properties) {
		return applyTool(toolId, table, attrs, null, properties);
	}

	/**
	* Applies the specified analysis tool to the given table and the given
	* attributes. Returns a reference to the tool constructed or null if failed.
	* The tool may show its results on a map if the layer identifier is provided.
	* The argument properties may specify individual properties for the
	* tool to be constructed.
	*/
	@Override
	public Object applyTool(String toolId, AttributeDataPortion table, Vector attrs, String geoLayerId, Hashtable properties) {
		err = null;
		if (toolId.equals("attribute_query")) {
			toolId = "dynamic_query";
		} else if (toolId.equals("find_by_name")) {
			toolId = "object_index";
		}
		if (qsProducer.isToolAvailable(toolId))
			return makeQueryOrSearchTool(toolId, (ObjectContainer) table, attrs);
		if (dataDisplayer.isMethodAvailable(toolId))
			return display(table, attrs, toolId, properties);
		// following string: ": unknown_tool!"
		err = toolId + res.getString("_unknown_tool_");
		return null;
	}

	/**
	* Constructs and applies the tool according to the given specification.
	*/
	@Override
	public Object applyTool(ToolSpec spec, AttributeDataPortion table) {
		return applyTool(spec, table, null);
	}

	/**
	* Constructs and applies the tool according to the given specification.
	*/
	@Override
	public Object applyTool(ToolSpec spec, AttributeDataPortion table, String geoLayerId) {
		if (spec == null)
			return null;
		String toolId = spec.methodId;
		if (toolId == null)
			return null;
		err = null;
		if (toolId.equals("attribute_query")) {
			toolId = "dynamic_query";
		} else if (toolId.equals("find_by_name")) {
			toolId = "object_index";
		}
		if (qsProducer.isToolAvailable(toolId))
			return makeQueryOrSearchTool(toolId, (ObjectContainer) table, spec.attributes);
		if (dataDisplayer.isMethodAvailable(toolId)) {
			Component c = makeDisplay(spec, table);
			showGraph(c);
			return c;
		}
		// following string: ": unknown_tool!"
		err = toolId + res.getString("_unknown_tool_");
		return null;
	}

	/**
	* If failed to apply the requested tool, the error message explains the
	* reason
	*/
	@Override
	public String getErrorMessage() {
		return err;
	}

	/**
	* Closes all tools that are currently open
	*/
	@Override
	public void closeAllTools() {
		closeAllWindows();
	}

	/**
	 * Closes tools that are destroyed
	 */
	@Override
	public void closeDestroyedTools() {
		if (graphPanel != null) {
			graphPanel.removeDestroyedComponents();
			if (graphPanel.isEmpty()) {
				graphFrame.dispose();
				graphFrame = null;
				graphPanel = null;
			}
		}
		if (queryPanel != null) {
			queryPanel.removeDestroyedComponents();
			if (queryPanel.isEmpty()) {
				queryFrame.dispose();
				queryFrame = null;
				queryPanel = null;
			}
		}
		if (windows != null) {
			for (int i = windows.size() - 1; i >= 0; i--)
				if (CManager.isComponentDestroyed((Window) windows.elementAt(i))) {
					((Window) windows.elementAt(i)).dispose();
					windows.removeElementAt(i);
				}
		}
	}

	/**
	* When a table is removed, the DisPlayProducer must close all displays that
	* are linked to this table. The table is specified by its identifier.
	*/
	@Override
	public void tableIsRemoved(String tableId) {
		if (tableId == null)
			return;
		closeDestroyedTools();
	}

//------------------ management of windows -----------------------------------
	protected void closeDisplay(Window win) {
		win.dispose();
		CManager.destroyComponent(win);
		sup.notifyGlobalPropertyChange(Supervisor.eventDisplayedAttrs);
	}

//--------------------- WindowManager interface ----------------------------
	@Override
	public void registerWindow(Window win) {
		if (win != null) {
			win.addWindowListener(this);
			if (windows == null) {
				windows = new Vector(10, 10);
			}
			windows.addElement(win);
		}
	}

	@Override
	public void closeAllWindows() {
		if (graphPanel != null) {
			graphPanel.close();
		}
		if (queryPanel != null) {
			queryPanel.close();
		}
		if (windows != null) {
			for (int i = 0; i < windows.size(); i++) {
				closeDisplay((Window) windows.elementAt(i));
			}
			windows.removeAllElements();
		}
		graphFrame = null;
		graphPanel = null;
		queryFrame = null;
		queryPanel = null;
	}

	private boolean allWindowsVisible = true;

	@Override
	public void setAllWindowsVisible(boolean visible) {
		if (visible && !allWindowsVisible || !visible && allWindowsVisible) {
			if (windows != null) {
				for (int i = 0; i < windows.size(); i++) {
					((Window) windows.elementAt(i)).setVisible(visible);
				}
			}
			sup.getUI().getMainFrame().setVisible(visible);
		}
		allWindowsVisible = visible;
	}

	@Override
	public int getWindowCount() {
		if (windows == null)
			return 0;
		return windows.size();
	}

	@Override
	public Window getWindow(int idx) {
		if (windows == null || idx < 0 || idx >= windows.size())
			return null;
		return (Window) windows.elementAt(idx);
	}

	@Override
	public void showWindowList() {
		if (windows == null || windows.size() < 1)
			return;
		WinListManage wlp = new WinListManage(windows);
		if (!wlp.isOK())
			return;
		Frame fr = ui.getMainFrame();
		if (fr == null) {
			fr = CManager.getAnyFrame();
		}
		OKDialog okd = new OKDialog(fr, res.getString("Windows"), false);
		okd.addContent(wlp);
		okd.show();
	}

//--------------- Reaction to window events ------------------------------------
	@Override
	public void windowClosing(WindowEvent e) {
		if (windows == null)
			return;
		for (int i = 0; i < windows.size(); i++)
			if (e.getSource().equals(windows.elementAt(i))) {
				closeDisplay((Window) windows.elementAt(i));
				break;
			}
		for (int i = 0; i < windows.size(); i++)
			if (e.getSource().equals(windows.elementAt(i))) {
				windows.removeElementAt(i);
				break;
			}
		if (e.getSource() == graphFrame) {
			graphFrame = null;
			graphPanel = null;
		}
		if (e.getSource() == queryFrame) {
			queryFrame = null;
			queryPanel = null;
		}
	}

	@Override
	public void windowOpened(WindowEvent e) {
	}

	@Override
	public void windowClosed(WindowEvent e) {
		if (windows == null)
			return;
		int idx = -1;
		for (int i = 0; i < windows.size() && idx < 0; i++)
			if (e.getSource().equals(windows.elementAt(i))) {
				idx = i;
			}
		if (idx >= 0) {
			windows.removeElementAt(idx);
		}
		if (e.getSource() == graphFrame) {
			graphFrame = null;
			graphPanel = null;
		}
		if (e.getSource() == queryFrame) {
			queryFrame = null;
			queryPanel = null;
		}
		if (e.getWindow().equals(CManager.getAnyFrame())) {
			CManager.setMainFrame(null);
		}
	}

	@Override
	public void windowIconified(WindowEvent e) {
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
	}

	@Override
	public void windowActivated(WindowEvent e) {
		if (e.getWindow() instanceof Frame) {
			CManager.setMainFrame((Frame) e.getWindow());
		}
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
	}

	/**
	* Registers a listener of appearing particular windows, such as the window
	* with non-cartographical displays or with a dynamic query.
	*/
	@Override
	public void addWinCreateListener(PropertyChangeListener lst) {
		if (lst == null)
			return;
		if (winList == null) {
			winList = new Vector(10, 10);
		}
		if (!winList.contains(lst)) {
			winList.addElement(lst);
		}
	}

	/**
	* Removes a listener of appearing particular windows.
	*/
	@Override
	public void removeWinCreateListener(PropertyChangeListener lst) {
		if (lst == null || winList == null)
			return;
		int idx = winList.indexOf(lst);
		if (idx >= 0) {
			winList.removeElementAt(idx);
		}
	}

	/**
	* Notifies listeners, if any, about appearance of a particular window, such as
	* the window with non-cartographical displays or with a dynamic query.
	* The key specify what type of window has been created. The key is used as the
	* name of the property in the property change event sent to the listeners.
	*/
	protected void notifyWindowCreated(String key, Window win) {
		if (key == null || winList == null || winList.size() < 1)
			return;
		PropertyChangeEvent pce = new PropertyChangeEvent(this, key, null, win);
		Vector wl = (Vector) winList.clone();
		for (int i = 0; i < wl.size(); i++) {
			((PropertyChangeListener) wl.elementAt(i)).propertyChange(pce);
		}
	}
}
