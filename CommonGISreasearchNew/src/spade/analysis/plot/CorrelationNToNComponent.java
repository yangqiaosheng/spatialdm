package spade.analysis.plot;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Label;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.calc.CorrelationDataContainer;
import spade.analysis.generators.PlotGeneratorsDescriptor;
import spade.analysis.plot.correlation.MultiCorrelationGraphics;
import spade.analysis.system.ESDACore;
import spade.analysis.system.Supervisor;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.Metrics;
import spade.lib.basicwin.PlotCanvas;
import spade.lib.lang.Language;
import spade.lib.util.StringUtil;
import spade.vis.action.HighlightListener;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.DataTreater;
import spade.vis.spec.SaveableTool;
import spade.vis.spec.ToolSpec;
import ui.AttributeChooser;
import ui.BasicUI;

/** Displays a value table and either a circle diagram or three-dimansional bar graph representing
 * correlation coefficient values of selected attributes among each other.
 */
public class CorrelationNToNComponent extends Panel implements DataTreater, Destroyable, ItemListener, HighlightListener, ActionListener, SaveableTool, PropertyChangeListener, PrintableImage {

	static ResourceBundle res = Language.getTextResource("spade.analysis.plot.Res");

	protected Vector globalIDs = null;

	/**
	* Identifier of this visualization method
	*/
	protected String methodId = null;
//ID
	/**
	* Used to generate unique identifiers of instances
	*/
	protected static int nInstances = 0;
	protected int instanceN = 0;
//~ID

	/** Stores the IDs of currently selected attributes.
	 */
	protected Vector attributes = null;
	/** Stores the data table.
	 */
	protected AttributeDataPortion dataTable = null;
	protected Vector attributeColors = null;

	/** Is a reference to the component's supervisor and gives access to its methods.
	 */
	protected Supervisor supervisor = null;

	/** Stores the boolean value whether the component has been destroyed or not.
	 */
	protected boolean destroyed = false;

	/** Contains methods to calculate correlation values from given data.
	 */
	protected CorrelationDataContainer correlationContainer = null;
	/** Draws the circle diagram or bar graph.
	 */
	protected MultiCorrelationGraphics graph = null;

	protected LowerTrBarMatrix matrixGraph = null;

	/** Contains the value table.
	 */
	protected PlotCanvas tableCanvas = null;
	/** Contains the circle diagram or bar graph.
	 */
	protected PlotCanvas graphCanvas = null;
	/** Scrollpane contains the component's control elements.
	 */
	protected Panel controlInhalt = null;
	/** Split-layout panel that divides the value table above from the control elements below.
	 */
	protected ScrollPane graphScroll = null;
	/** Contains the value table and header.
	 */
	protected ScrollPane tabelle = null;
	protected Panel pane = new Panel(new BorderLayout());

	/** CheckboxGroup used to check which checkbox is currently enabled
	 * (circle diagram / 3d bar graph).
	 */
	protected CheckboxGroup cbgDisplayMode = new CheckboxGroup();
	protected Checkbox halfmatrix = new Checkbox(res.getString("half_matrix"), cbgDisplayMode, true);
	protected Checkbox threedim = new Checkbox(res.getString("3d_graphic"), cbgDisplayMode, false);
	/** CheckboxGroup used to check which checkbox is currently enabled
	 * (signed / unsigned).
	 */
	protected CheckboxGroup cbgSigned = new CheckboxGroup();
	protected Checkbox signed = new Checkbox(res.getString("signed"), cbgSigned, false);
	protected Checkbox unsigned = new Checkbox(res.getString("unsigned"), cbgSigned, true);
	/** CheckboxGroup used to check which checkbox is currently enabled
	 * (whole map / SelectedDistrictMode).
	 */
	protected CheckboxGroup cbgCalculationMode = new CheckboxGroup();
	protected Checkbox map = new Checkbox(res.getString("whole_map"), cbgCalculationMode, true);
	protected Checkbox selected = new Checkbox(res.getString("selected_area"), cbgCalculationMode, false);

	protected Panel controlDisplay = null;
	protected Panel controlCalculationMethod = null;
	protected Panel controlSigned = null;
	protected Panel controlComboBox = null;

	/** Drop-down list that contains the selected attributes. Row highlighting for the 3d
	 * bar graph can be selected.
	 */
	protected Choice attribs = new Choice();
	/** Stores the index of the currently highlighted row in the bar graph.
	 */
	protected int index = 1;
	/** Stores the boolean value whether the SelectedDistrictMode is enabled or disabled.
	 */
	protected boolean districtMode = false;
	/** Stores the IDs of districts every time they are selected or deselected.
	 */
	protected Vector selectedDistricts = new Vector();

	protected Panel panelMain = null;
	protected Panel control = null;

	protected Label headerText = new Label();

	/**
	* As a SaveableTool, a plot may be registered somewhere and, hence, must
	* notify the component where it  isregistered about its destroying. This vector
	* contains the listeners to be notified about destroying of the visualizer.
	*/
	protected Vector destroyListeners = null;

	/** Initialises all values and variables and draws the initial window.
	 *
	 * @param sup Supervisor
	 * @param dt Data table
	 * @param attr Selected attributes
	 */
	public CorrelationNToNComponent(Supervisor sup, AttributeDataPortion dt, Vector attr) {
//ID
		instanceN = ++nInstances;
//~ID

		supervisor = sup;
		supervisor.registerDataDisplayer(this);
		dataTable = dt;
		attributes = attr;
		supervisor.registerHighlightListener(this, dataTable.getEntitySetIdentifier());
		selectedDistricts = supervisor.getHighlighter(dataTable.getEntitySetIdentifier()).getSelectedObjects();
		globalIDs = calcIDsForWholeMap();
		correlationContainer = new CorrelationDataContainer(dataTable, attributes, globalIDs);

		graph = new MultiCorrelationGraphics();
		for (int i = 0; i < attributes.size(); i++) {
			int idx = dataTable.getAttrIndex((String) attributes.elementAt(i));
			graph.addRow(correlationContainer.getValueRow(idx));
			graph.addDescriptor(dataTable.getAttributeName(String.valueOf(attributes.elementAt(i))));
		}
		graph.setRectangleSize(110);
		graph.setAdapting(false);
		graph.rowSelected = 1;
		graph.backgroundColor = Color.lightGray;

		graphCanvas = new PlotCanvas();
		graph.setCanvas(graphCanvas);

		matrixGraph = new LowerTrBarMatrix(correlationContainer, false);
		for (int i = 0; i < attributes.size(); i++) {
			//Attribute a = dataTable.getAttribute((String)attributes.elementAt(i));
			//String id = a.getIdentifier();
			int idx = dataTable.getAttrIndex((String) attributes.elementAt(i));
			matrixGraph.addItem(new PairDataMatrixItem(idx, dataTable.getAttributeName(String.valueOf(attributes.elementAt(i)))));
		}
		matrixGraph.setCanvas(graphCanvas);
		matrixGraph.setDrawSigned(false);
		matrixGraph.addActionListener(this);
		graphCanvas.setContent(matrixGraph);

		setLayout(new BorderLayout());
		setName(res.getString("n_to_n_correlation_component"));

		panelMain = new Panel();
		panelMain.setLayout(new GridLayout());

		graphScroll = new ScrollPane();
		graphScroll.setSize(new Dimension(130 * Metrics.mm(), 110 * Metrics.mm()));
		graphScroll.add(graphCanvas);
		panelMain.add(graphScroll);

		add(panelMain, "Center");

		control = new Panel(new GridLayout());

		controlDisplay = new Panel(new ColumnLayout());
		controlDisplay.add(new Label(res.getString("display_graph_as")));
		threedim.addItemListener(this);
		controlDisplay.add(threedim);
		halfmatrix.addItemListener(this);
		controlDisplay.add(halfmatrix);
		control.add(controlDisplay);

		controlSigned = new Panel(new ColumnLayout());
		controlSigned.add(new Label(res.getString("show_numbers_as_")));
		signed.addItemListener(this);
		controlSigned.add(signed);
		unsigned.addItemListener(this);
		controlSigned.add(unsigned);
		control.add(controlSigned);

		controlCalculationMethod = new Panel(new ColumnLayout());
		controlCalculationMethod.add(new Label(res.getString("calculate_correlations_for_")));
		map.addItemListener(this);
		controlCalculationMethod.add(map);
		selected.addItemListener(this);
		controlCalculationMethod.add(selected);
		control.add(controlCalculationMethod);

		controlComboBox = new Panel(new ColumnLayout());
		controlComboBox.add(new Label(res.getString("highlight_")));
		for (int i = 0; i < attributes.size(); i++) {
			String attributeName = dataTable.getAttributeName((String) attributes.elementAt(i));
			attribs.add(attributeName);
		}
		attribs.addItemListener(this);
		controlComboBox.add(attribs);
		attribs.setEnabled(false);
		Button changeAttribs = new Button(res.getString("add_change_attributes"));
		changeAttribs.addActionListener(this);
		controlComboBox.add(new Label(""));
		controlComboBox.add(changeAttribs);
		control.add(controlComboBox);
		add(control, "South");

		add(headerText, "North");

	}

	/** Returns the list of currently selected attributes.
	 *
	 * @return Vector of ID strings.
	 */
	@Override
	public Vector getAttributeList() {

		return attributes;

	}

	/** Returns the boolean value whether the component is linked to the data
	 * set on the map.
	 *
	 * @param setId Data set on the map
	 * @return Boolean value whether there is a link.
	 */
	@Override
	public boolean isLinkedToDataSet(String setId) {

		if (dataTable.getEntitySetIdentifier() == setId)
			return true;

		else
			return false;

	}

	@Override
	public Vector getAttributeColors() {

		return attributeColors;

	}

	/** Unregisters the component from the supervisor and removes the HighlightListener.
	 */
	@Override
	public void destroy() {

		supervisor.removeDataDisplayer(this);
		supervisor.removeHighlightListener(this, dataTable.getEntitySetIdentifier());
		matrixGraph.removeActionListener(this);
		if (destroyListeners != null) {
			PropertyChangeEvent pce = new PropertyChangeEvent(this, "destroyed", null, null);
			for (int i = 0; i < destroyListeners.size(); i++) {
				((PropertyChangeListener) destroyListeners.elementAt(i)).propertyChange(pce);
			}
		}
		destroyed = true;

	}

	/** Returns the boolean value whether the component has been destroyed.
	 *
	 * @return Boolean value whether the component is destroyed.
	 */
	@Override
	public boolean isDestroyed() {

		return destroyed;

	}

//ID
	public int getInstanceN() {
		return instanceN;
	}

//~ID

	/** Method is executed when the mouse moves over a different district on the map.
	 *
	 * @param source Source object
	 * @param setId Data set on the map
	 * @param dist ID of highlighted district
	 */
	@Override
	public void highlightSetChanged(Object source, String setId, Vector dist) {

		// does nothing because higlighting is not used in this component...

	}

	/** Method is executed when the selection of districts on the map changes.
	 *
	 * @param source Source object
	 * @param setId Data set on the map
	 * @param dist IDs of the selected districts
	 */
	@Override
	public void selectSetChanged(Object source, String setId, Vector dist) {

		selectedDistricts = dist;
		if ((districtMode) & (dist != null)) {
			correlationContainer.calculate(attributes, selectedDistricts);

			for (int i = 0; i < attribs.getItemCount(); i++) {
				if (StringUtil.sameStringsIgnoreCase((String) attributes.elementAt(i), attribs.getSelectedItem())) {
					index = i + 1;
				}
			}

			for (int i = 0; i < attributes.size(); i++) {
				int idx = dataTable.getAttrIndex((String) attributes.elementAt(i));
				graph.updateRow(i, correlationContainer.getValueRow(idx));
			}

			matrixGraph.updateValues();

			if (cbgDisplayMode.getSelectedCheckbox() == threedim) {
				graph.draw(graphCanvas.getGraphics());
			} else {
				matrixGraph.draw(graphCanvas.getGraphics());
			}
			graphScroll.validate();
			graphCanvas.validate();
		}

	}

	/** Method is executed when a radio button is clicked or the selection in the drop-down
	 * list changes.
	 *
	 * @param e Action event
	 */
	@Override
	public void itemStateChanged(ItemEvent e) {

		if (e.getItem().equals(res.getString("selected_area"))) {

			districtMode = true;
			if (selectedDistricts != null) {
				if (selectedDistricts.size() != 0) {
					selectSetChanged(null, null, selectedDistricts);
				}
			}
		} else if (e.getItem().equals(res.getString("whole_map"))) {

			if (districtMode) {

				for (int i = 0; i < attribs.getItemCount(); i++) {
					String attributeName = (String) attributes.elementAt(i);
					String compareWith = (String) e.getItem();
					if (dataTable.getAttributeName(attributeName).equalsIgnoreCase(compareWith)) {
						index = i + 1;
					}
				}
				correlationContainer.calculate(attributes, globalIDs);
				for (int i = 0; i < attributes.size(); i++) {
					int idx = dataTable.getAttrIndex((String) attributes.elementAt(i));
					graph.updateRow(i, correlationContainer.getValueRow(idx));
				}
				matrixGraph.updateValues();
				if (cbgDisplayMode.getSelectedCheckbox() == threedim) {
					attribs.setEnabled(true);
					graph.switchAbs(cbgSigned.getSelectedCheckbox() == signed ? true : false);
					graph.rowSelected = index;
					graph.draw(graphCanvas.getGraphics());
				} else {
					attribs.setEnabled(false);
					matrixGraph.draw(graphCanvas.getGraphics());
				}
				graphScroll.validate();
				graphCanvas.validate();

			}
			districtMode = false;

		}

		else if ((e.getItem().equals(res.getString("signed"))) || (e.getItem().equals(res.getString("unsigned")))) { // also click auf signed/unsigned

			//graph.switchAbs(cbgSigned.getSelectedCheckbox()==signed?true:false);
			graph.switchAbs(cbgSigned.getSelectedCheckbox() == signed);
			graph.rowSelected = index;
			//matrixGraph.setDrawSigned(cbgSigned.getSelectedCheckbox()==signed?true:false);
			matrixGraph.setDrawSigned(cbgSigned.getSelectedCheckbox() == signed);

			if (cbgDisplayMode.getSelectedCheckbox() == threedim) { // 3d gew�hlt
				graph.draw(graphCanvas.getGraphics());
			} else {
				matrixGraph.draw(graphCanvas.getGraphics());
			}

		} else if (e.getItem().equals(res.getString("3d_graphic"))) {
			matrixGraph.setVisible(false);
			headerText.setText(res.getString("highlighting_row_for_") + attribs.getSelectedItem());
			attribs.setEnabled(true);
			graph.set3d(true);
			graph.switchAbs(cbgSigned.getSelectedCheckbox() == signed);
			graph.rowSelected = index;
			graphCanvas.repaint();
			graphCanvas.setContent(graph);
			graph.draw(graphCanvas.getGraphics());
		} else if (e.getItem().equals(res.getString("half_matrix"))) {
			matrixGraph.setVisible(true);
			headerText.setText("");
			attribs.setEnabled(false);
			graphCanvas.repaint();
			graphCanvas.setContent(matrixGraph);
			matrixGraph.setDrawSigned(cbgSigned.getSelectedCheckbox() == signed);
			matrixGraph.draw(graphCanvas.getGraphics());
			CManager.validateFully(this);
		} else { // item from drop-down list chosen
			// calculate index of attribute selected in list
			for (int i = 0; i < attribs.getItemCount(); i++) {
				String attributeName = dataTable.getAttributeName((String) attributes.elementAt(i));
				String listItem = (String) e.getItem();
				if (attributeName.equalsIgnoreCase(listItem)) {
					index = i + 1;
				}
			}

			headerText.setText(res.getString("highlighting_row_for_") + e.getItem());
			graph.rowSelected = index;
			graph.draw(graphCanvas.getGraphics());
		}

	}

	protected Vector calcIDsForWholeMap() {
		Vector res = new Vector();
		for (int i = 0; i < dataTable.getDataItemCount(); i++) {
			String id = dataTable.getDataItemId(i);
			res.addElement(new String(id));
		}
		return res;
	}

	/**
	 * Invoked when an action occurs.
	 *
	 * @param e ActionEvent
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals(res.getString("add_change_attributes"))) {
			AttributeChooser attrSelect = new AttributeChooser();
			String prompt = res.getString("choose_attributes");
			attrSelect.selectColumns(dataTable, attributes, null, true, prompt, supervisor.getUI());
			if (attrSelect.getSelectedColumnIds() == null)
				return;
			else {
				attributes = (Vector) attrSelect.getSelectedColumnIds().clone();
			}

			if (districtMode) {
				correlationContainer.calculate(attributes, selectedDistricts);
			} else {
				correlationContainer.calculate(attributes, globalIDs);
			}

			boolean halfMatrixShown = (cbgDisplayMode.getSelectedCheckbox() == halfmatrix) ? true : false;

			/* setup of matrix graph */
			matrixGraph.removeActionListener(this);
			matrixGraph = new LowerTrBarMatrix(correlationContainer, false);
			for (int i = 0; i < attributes.size(); i++) {
				int idx = dataTable.getAttrIndex((String) attributes.elementAt(i));
				matrixGraph.addItem(new PairDataMatrixItem(idx, dataTable.getAttributeName(String.valueOf(attributes.elementAt(i)))));
			}
			matrixGraph.setCanvas(graphCanvas);
			matrixGraph.setDrawSigned(false);
			matrixGraph.addActionListener(this);
			graphCanvas.setContent(matrixGraph);
			/******************************/

			/* setup of 3d bar graph */
			graph = new MultiCorrelationGraphics();
			for (int i = 0; i < attributes.size(); i++) {
				int idx = dataTable.getAttrIndex((String) attributes.elementAt(i));
				graph.addRow(correlationContainer.getValueRow(idx));
				graph.addDescriptor(dataTable.getAttributeName(String.valueOf(attributes.elementAt(i))));
			}
			graph.setRectangleSize(110);
			graph.setAdapting(false);
			graph.rowSelected = 1;
			graph.backgroundColor = Color.lightGray;
			graph.setCanvas(graphCanvas);
			/******************************/

			//graphCanvas.validate();
			if (halfMatrixShown) {
				graphCanvas.setContent(matrixGraph);
				//matrixGraph.draw(graphCanvas.getGraphics());
			} else {
				graphCanvas.setContent(graph);
				graph.draw(graphCanvas.getGraphics());
			}
			graphCanvas.repaint();

			/* setup of combo box */
			attribs.removeAll();
			for (int i = 0; i < attributes.size(); i++) {
				String attributeName = dataTable.getAttributeName((String) attributes.elementAt(i));
				attribs.add(attributeName);
			}
			/***********************/

		}

		else {
			String str = e.getActionCommand();
			StringBuffer sb = new StringBuffer();
			Vector rev = new Vector();
			for (int i = 0; i < str.length(); i++) {
				if (str.charAt(i) == ',') {
					int idx = Integer.valueOf(sb.toString()).intValue();
					rev.addElement(dataTable.getAttributeId(idx));
					sb = new StringBuffer();
					continue;
				}
				sb.append(str.charAt(i));
			}
			rev.addElement(dataTable.getAttributeId(Integer.valueOf(sb.toString()).intValue()));
			Vector attr = new Vector();
			for (int i = rev.size() - 1; i >= 0; i--) {
				attr.addElement(rev.elementAt(i));
			}
			ESDACore core = ((BasicUI) supervisor.getUI()).getCore();
			core.getDisplayProducer().display(dataTable, attr, "scatter_plot", null);
		}
	}

	protected Vector stringToVector(String str) {
		Vector res = new Vector();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < str.length(); i++) {
			if ((str.charAt(i) != ',') && (str.charAt(i) != ']')) {
				sb.append(str.charAt(i));
			} else if ((str.charAt(i) == ',') || (str.charAt(i) == ']')) {
				String s = sb.toString();
				sb = new StringBuffer();
				res.addElement(s);
			}
		}
		return res;
	}

	/**
	 * Returns the keyword used in the opening tag of a stored state description of this tool.
	 *
	 * @return String
	 */
	@Override
	public String getTagName() {
		return "chart";
	}

	protected String getMethodId() {
		return methodId;
	}

	public void setMethodId(String id) {
		methodId = id;
	}

	/**
	 * Returns the specification (i.e.
	 *
	 * @return Object
	 */
	@Override
	public Object getSpecification() {
		ToolSpec spec = new ToolSpec();
		spec.tagName = getTagName();
		spec.methodId = getMethodId();
		if (dataTable != null) {
			spec.table = dataTable.getContainerIdentifier();
		}
		spec.attributes = getAttributeList();
		spec.properties = getProperties();
		return spec;
	}

	public Hashtable getProperties() {
		Hashtable properties = new Hashtable();

		// which districts are selected?
		if (selectedDistricts != null) {
			properties.put("selectedDistricts", selectedDistricts.toString());
		} else {
			properties.put("selectedDistricts", "");
		}
		// cbgSigned: which checkbox is selected?
		properties.put("signed_state", String.valueOf(signed.getState()));
		properties.put("unsigned_state", String.valueOf(unsigned.getState()));
		// cbgSelected: which checkbox is selected?
		properties.put("map_state", String.valueOf(map.getState()));
		properties.put("selected_state", String.valueOf(selected.getState()));
		// which graph is chosen?
		properties.put("halfmatrix_state", String.valueOf(halfmatrix.getState()));
		properties.put("threedim_state", String.valueOf(threedim.getState()));
		// which row in three-dim graph is seleced?
		properties.put("selected_row", new Integer(graph.rowSelected));
		// selection in combo box?
		properties.put("combobox_selection", new Integer(attribs.getSelectedIndex()));

		return properties;
	}

	/**
	 * After the tool is constructed, it may be requested to setup its individual properties according
	 * to the given list of stored properties.
	 *
	 * @param properties Hashtable
	 */
	@Override
	public void setProperties(Hashtable properties) {
		if (properties == null)
			return;

		try {

			for (int i = 0; i < attributes.size(); i++) {
				String elem = (String) attributes.elementAt(i);
				int idx = dataTable.findAttrByName(elem);
				String id = dataTable.getAttributeId(idx);
				attributes.setElementAt(id, i);
				//attributes.setElementAt(i,id);     // Java 2
			}
			if ((String) properties.get("selectedDistricts") != null) {
				selectedDistricts = new Vector();
				selectedDistricts = stringToVector((String) properties.get("selectedDistricts"));
				supervisor.getHighlighter(dataTable.getEntitySetIdentifier()).makeObjectsSelected(this, selectedDistricts);
			}

			signed.setState(new Boolean((String) properties.get("signed_state")).booleanValue());
			unsigned.setState(new Boolean((String) properties.get("unsigned_state")).booleanValue());
			map.setState(new Boolean((String) properties.get("map_state")).booleanValue());
			selected.setState(new Boolean((String) properties.get("selected_state")).booleanValue());
			halfmatrix.setState(new Boolean((String) properties.get("halfmatrix_state")).booleanValue());
			threedim.setState(new Boolean((String) properties.get("threedim_state")).booleanValue());
			districtMode = cbgCalculationMode.getSelectedCheckbox() == selected ? true : false;

			index = graph.rowSelected = Integer.valueOf((String) properties.get("selected_row")).intValue();
			attribs.select(Integer.valueOf((String) properties.get("combobox_selection")).intValue());

			headerText.setText(res.getString("highlighting_row_for_") + attribs.getSelectedItem());

			if (cbgDisplayMode.getSelectedCheckbox() == threedim) {
				graph.draw(graphCanvas.getGraphics());
			} else {
				matrixGraph.draw(graphCanvas.getGraphics());
			}
		}

		catch (Exception ex) {
			System.out.println("Exception!");
			System.out.println(ex);
		}
	}

	/**
	 * Adds a listener to be notified about destroying the tool.
	 *
	 * @param lst PropertyChangeListener
	 */
	@Override
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
	 * This method gets called when a bound property is changed.
	 *
	 * @param evt A PropertyChangeEvent object describing the event source and the property that has
	 *   changed.
	 */
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
	}

//ID
	@Override
	public Image getImage() {
		int w = graphCanvas.getBounds().width;
		int h = graphCanvas.getBounds().height;
		Image img = graphCanvas.createImage(w, h);
		graphCanvas.paint(img.getGraphics());
		return img;
	}

	@Override
	public String getName() {
		String name = PlotGeneratorsDescriptor.getToolName(getMethodId()) + " " + getInstanceN();
		if (name == null) {
			name = getMethodId() + " " + getInstanceN();
		}
		return name;
	}
//~ID
}
