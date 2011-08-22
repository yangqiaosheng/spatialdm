package spade.analysis.plot;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Cursor;
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
import spade.analysis.datamanage.DataFilter;
import spade.analysis.generators.PlotGeneratorsDescriptor;
import spade.analysis.plot.bargraph.CorrelationBarGraph;
import spade.analysis.system.Supervisor;
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

/**
 * Displays a value table and two-dimensional bar graph of correlation coefficient values.
 * Different attributes can be selected and the attribute to which the correlation
 * is calculated can be chosen from a drop-down list.
 */
public class CorrelationOneToNComponent extends Panel implements DataTreater, Destroyable, ItemListener, HighlightListener, ActionListener, SaveableTool, PropertyChangeListener, PrintableImage {

	static ResourceBundle res = Language.getTextResource("spade.analysis.plot.Res");
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

	/**
	 * Stores the IDs of the attributes selected in the Display Wizard.
	 */
	protected Vector attributes = null;

	/**
	 * Contains attribute data for all districts.
	 */
	protected AttributeDataPortion dataTable = null;
	protected Vector attributeColors = null;

	protected Supervisor supervisor = null;

	/**
	 * Stores the boolean value whether the component has been destroyed or not.
	 */
	protected boolean destroyed = false;

	/** CorrelationContainer contains methods to calculate correlation coefficients from
	 * selected data.
	 */
	protected CorrelationDataContainer correlationContainer = null;

	/**
	 * Draws the bar graph.
	 */
	protected CorrelationBarGraph bg = null;

	/**
	 * Contains the method to filter the value of a single attribute for one district.
	 */
	protected DataFilter flt = new DataFilter();

	/**
	 * Stores the boolean value whether the SelectedDistrictMode is enabled.
	 */
	protected boolean districtMode = false;

	/** Contains the bar graph.
	 */
	protected PlotCanvas graphCanvas = null;
	/**
	 * Contains control elements to change between signed / unsigned view.
	 */
	protected Panel controlSigned = null;
	/**
	 * Contains control elements to enable or disable the SelectedDistrictMode
	 */
	protected Panel controlJust = null;
	protected Panel controlInhalt = null;
	protected Panel controlButton = null;

	/** Stores the name of the attribute to which all correlations are
	 * calculated.
	 */
	protected String compareTo = null;
	protected String headerTextBeginning = res.getString("correlation_to");
	protected String headerInstruction = res.getString("_click_attribute_name_to_");
	protected Label headerText = null;
	protected Panel header = new Panel(new ColumnLayout());

	protected ScrollPane graphScroll = null;

	/**
	 * CheckboxGroup used to check which checkbox is currently enabled
	 * (signed / unsigned).
	 */
	protected CheckboxGroup cbgSigned = new CheckboxGroup();
	protected Checkbox signed = new Checkbox(res.getString("signed"), cbgSigned, false);
	protected Checkbox unsigned = new Checkbox(res.getString("unsigned"), cbgSigned, true);
	/**
	 * CheckboxGroup used to check which checkbox is currently enabled
	 * (whole map / SelectedDistrictMode).
	 */
	protected CheckboxGroup cbgSelected = new CheckboxGroup();
	protected Checkbox map = new Checkbox(res.getString("whole_map"), cbgSelected, true);
	protected Checkbox selected = new Checkbox(res.getString("selected_area"), cbgSelected, false);

	/** Stores the descriptions of the bar graph.
	 */
	protected Vector desvec = null;
	/** Stores the IDs of districts every time they are selected or deselected.
	 */
	protected Vector selectedDistricts = new Vector();
	/** Stores the IDs of every district on the map.
	 */
	protected Vector globalIDs = null;

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
	public CorrelationOneToNComponent(Supervisor sup, AttributeDataPortion dt, Vector attr) {
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

		graphCanvas = new PlotCanvas();
		bg = new CorrelationBarGraph();
		graphCanvas.setContent(bg);
		bg.setCanvas(graphCanvas);
		bg.addActionListener(this);
		desvec = (Vector) getDesc(attributes).clone();
		compareTo = (String) attributes.elementAt(0);
		headerText = new Label(headerTextBeginning + dataTable.getAttributeName(compareTo) + headerInstruction);
		bg.setValues(correlationContainer.getValueRow(dataTable.getAttrIndex(compareTo)));
		bg.setDescriptions(desvec);
		bg.setMargin(5);
		bg.setSpace(0, 0, 0);
		bg.setDrawSigned(cbgSigned.getSelectedCheckbox() == signed ? true : false);

		setLayout(new BorderLayout());
		setName(res.getString("1_to_n_correlation_component"));

		controlSigned = new Panel(new ColumnLayout());
		controlSigned.add(new Label(res.getString("show_numbers_as_")));
		signed.addItemListener(this);
		unsigned.addItemListener(this);
		controlSigned.add(signed);
		controlSigned.add(unsigned);

		controlJust = new Panel(new ColumnLayout());
		controlJust.add(new Label(res.getString("calculate_correlations_for_")));
		map.addItemListener(this);
		selected.addItemListener(this);
		controlJust.add(map);
		controlJust.add(selected);

		controlButton = new Panel(new ColumnLayout());
		Button changeAttribs = new Button(res.getString("add_change_attributes"));
		changeAttribs.addActionListener(this);
		controlButton.add(new Label(" "));
		controlButton.add(changeAttribs);

		controlInhalt = new Panel(new GridLayout(1, 0));
		controlInhalt.add(controlSigned);
		controlInhalt.add(controlJust);
		controlInhalt.add(controlButton);
		this.add(controlInhalt, "South");

		graphScroll = new ScrollPane();
		graphScroll.add(graphCanvas);
		graphScroll.setSize(new Dimension(110 * Metrics.mm(), 90 * Metrics.mm()));

		add(graphScroll, "Center");

		header.add(headerText);
		add(header, "North");

	}

	protected Vector getDesc(Vector v) {
		Vector res = new Vector();
		for (int i = 0; i < v.size(); i++) {
			String z = (String) attributes.elementAt(i);
			res.addElement(dataTable.getAttributeName(z));
		}
		return res;
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

	/** Redraws the window components after value changes.
	 */
	protected void redraw() {

		bg.setValues(correlationContainer.getValueRow(dataTable.getAttrIndex(compareTo)));
		bg.setDescriptions(desvec);
		bg.setDrawSigned(cbgSigned.getSelectedCheckbox() == signed ? true : false);
		bg.setup();
		bg.draw(graphCanvas.getGraphics());
		graphScroll.doLayout();
		//CManager.validateAll(this);
		//validate();
		//graphScroll.validate();
		//graphCanvas.validate();

	}

	/** Calculates the IDs of all districts on the map and stores them in the Vector
	 * globalIDs.
	 *
	 * @return Vector of ID strings
	 */
	protected Vector calcIDsForWholeMap() {

		Vector res = new Vector();
		for (int i = 0; i < dataTable.getDataItemCount(); i++) {
			String id = dataTable.getDataItemId(i);
			res.addElement(new String(id));
		}
		return res;

	}

	/** Method is executed when a radio button is clicked or the selection in the drop-down
	 * list changes.
	 *
	 * @param e Action event
	 */
	@Override
	public void itemStateChanged(ItemEvent e) {

		setCursor(new Cursor(Cursor.WAIT_CURSOR));
		if ((e.getItem().equals(res.getString("selected_area"))) || (e.getItem().equals(res.getString("whole_map")))) {

			if (e.getItem().equals(res.getString("selected_area"))) {

				districtMode = true;
				if (selectedDistricts == null) {
					setCursor(Cursor.getDefaultCursor());
					return;
				}
				if (selectedDistricts.size() == 0) {
					setCursor(Cursor.getDefaultCursor());
					return;
				}
				if (selectedDistricts.size() != 0) {

					correlationContainer.calculate(attributes, selectedDistricts);
					redraw();

				}

			} else {

				districtMode = false;
				correlationContainer.calculate(attributes, globalIDs);
				redraw();

			}

		} else {
			if (!districtMode) {

				if (!(e.getItem().equals(res.getString("signed"))) && !(e.getItem().equals(res.getString("unsigned")))) {
					correlationContainer.calculate(attributes, globalIDs);
				}
			}
			if (districtMode) {

				if (selectedDistricts != null) {

					if (!(e.getItem().equals(res.getString("signed"))) && !(e.getItem().equals(res.getString("unsigned")))) {
						correlationContainer.calculate(attributes, selectedDistricts);
					}

				}
			}

			if ((e.getItem().equals(res.getString("signed"))) || (e.getItem().equals(res.getString("unsigned")))) {

				if (e.getItem().equals(res.getString("signed"))) {
					bg.setDrawSigned(true);
				} else {
					bg.setDrawSigned(false);
				}
				bg.setup();
				bg.draw(graphCanvas.getGraphics());
				setCursor(Cursor.getDefaultCursor());
				return;

			}

			redraw();

		}

		setCursor(Cursor.getDefaultCursor());
	}

	/** Method is normally executed when the mouse moves over a different district
	 * on the map.
	 * Method is not used in this component.
	 *
	 * @param source --
	 * @param setId Data set on the map
	 * @param dist ID of highlighted district
	 */
	@Override
	public void highlightSetChanged(Object source, String setId, Vector dist) {

		// Does nothing because highlighting is not used in this component.

	}

	/** Method is executed when the selection of districts on the map changes.
	 *
	 * @param source --
	 * @param setId Data set on the map
	 * @param dist IDs of the selected districts
	 */
	@Override
	public void selectSetChanged(Object source, String setId, Vector dist) {

		selectedDistricts = dist;
		if (dist == null)
			return;
		if ((dist.size() != 0) & (districtMode)) {

			correlationContainer.calculate(attributes, selectedDistricts);

			redraw();

		}

	}

	@Override
	public void actionPerformed(ActionEvent e) {
		setCursor(new Cursor(Cursor.WAIT_CURSOR));
		if (e.getActionCommand().equals(res.getString("add_change_attributes"))) { // added or removed attributes
			AttributeChooser attrSelect = new AttributeChooser();
			//attrSelect.setColumnsMayBeReordered(false);
			String prompt = res.getString("choose_attributes");
			attrSelect.selectColumns(dataTable, attributes, null, true, prompt, supervisor.getUI());
			if (attrSelect.getSelectedColumnIds() == null) {
				setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				return;
			} else {
				attributes = (Vector) attrSelect.getSelectedColumnIds().clone();
				desvec = getDesc(attributes);
			}

			boolean compareToNew = true;
			for (int i = 0; i < attributes.size(); i++) {
				if (StringUtil.sameStringsIgnoreCase(compareTo, (String) attributes.elementAt(i))) {
					compareToNew = false;
					break;
				}
			}
			if (compareToNew) {
				compareTo = (String) attributes.elementAt(0);
			}
			if (districtMode) {
				correlationContainer.calculate(attributes, selectedDistricts);
			} else {
				correlationContainer.calculate(attributes, globalIDs);
			}
			//redraw();
		} else { // correlation target changed - no calculations necessary
			// get attribute id from name
			if (StringUtil.sameStringsIgnoreCase(dataTable.getAttributeId(dataTable.findAttrByName(e.getActionCommand())), compareTo)) {
				setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				return;
			}
			for (int i = 0; i < attributes.size(); i++) {
				String currentName = dataTable.getAttributeName((String) attributes.elementAt(i));
				if (StringUtil.sameStringsIgnoreCase(e.getActionCommand(), currentName)) {
					compareTo = (String) attributes.elementAt(i);
				}
			}

			headerText.setText(headerTextBeginning + dataTable.getAttributeName(compareTo) + headerInstruction);

			if (districtMode) {
				//correlationContainer.calculate(attributes, selectedDistricts);
			} else {
				//correlationContainer.calculate(attributes, globalIDs);
			}
			//redraw();
		}
		redraw();
		setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
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
				//attributes.set(i,id);    // Java 2
			}
			if ((String) properties.get("selectedDistricts") != null) {
				selectedDistricts = new Vector();
				selectedDistricts = stringToVector((String) properties.get("selectedDistricts"));
				supervisor.getHighlighter(dataTable.getEntitySetIdentifier()).makeObjectsSelected(this, selectedDistricts);
			}
			compareTo = (String) properties.get("compareAttribute");
			System.out.println("compareAttribute: " + compareTo);

			signed.setState(new Boolean((String) properties.get("signed_state")).booleanValue());
			unsigned.setState(new Boolean((String) properties.get("unsigned_state")).booleanValue());
			map.setState(new Boolean((String) properties.get("map_state")).booleanValue());
			selected.setState(new Boolean((String) properties.get("selected_state")).booleanValue());
			districtMode = cbgSelected.getSelectedCheckbox() == selected ? true : false;
			headerText.setText(headerTextBeginning + dataTable.getAttributeName(compareTo) + headerInstruction);
			redraw();
		}

		catch (Exception ex) {
			System.out.println("Exception!");
			System.out.println(ex);
		}

	}

	protected Vector stringToVector(String str) {
		Vector res = new Vector();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < str.length(); i++) {
			if (str.charAt(i) == '[') {
				continue;
			}
			if (str.charAt(i) == ' ') {
				continue;
			}
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
	 * @return Hashtable
	 */
	public Hashtable getProperties() {
		Hashtable properties = new Hashtable();

		// which districts are selected?
		if (selectedDistricts != null) {
			properties.put("selectedDistricts", selectedDistricts.toString());
		} else {
			properties.put("selectedDistricts", "");
		}
		// to which attribute is correlated?
		properties.put("compareAttribute", compareTo);
		// cbgSigned: which checkbox is selected?
		properties.put("signed_state", String.valueOf(signed.getState()));
		properties.put("unsigned_state", String.valueOf(unsigned.getState()));
		// cbgSelected: which checkbox is selected?
		properties.put("map_state", String.valueOf(map.getState()));
		properties.put("selected_state", String.valueOf(selected.getState()));

		return properties;
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
	 * Through this function the component constructing the plot can set the identifier of the
	 * visualization method.
	 *
	 * @param id String
	 */
	public void setMethodId(String id) {
		methodId = id;
	}

	/**
	 * Returns the identifier of the visualization method implemented by this class.
	 *
	 * @return String
	 */
	public String getMethodId() {
		return methodId;
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
