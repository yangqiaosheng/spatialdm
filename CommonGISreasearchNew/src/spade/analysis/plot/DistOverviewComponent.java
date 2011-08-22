package spade.analysis.plot;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Canvas;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.ScrollPane;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.datamanage.DataFilter;
import spade.analysis.plot.bargraph.DistrictOverviewGraph;
import spade.analysis.system.Supervisor;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.Drawable;
import spade.lib.basicwin.Metrics;
import spade.lib.basicwin.PlotCanvas;
import spade.lib.basicwin.SplitPanel;
import spade.lib.lang.Language;
import spade.lib.util.FloatArray;
import spade.lib.util.NumValManager;
import spade.lib.util.StringUtil;
import spade.vis.action.HighlightListener;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.DataTreater;
import ui.AttributeChooser;

/**
 * Displays the value of highlighted or selected districts on the map in addition to
 * the statistical values of mean, median and the absolute maximum an minimum value of
 * an attribute on the entire map.
 *
 * @author Gabriel Klein
 * @author Mario Boley
 * @author Peter Gatalsky
 * @version 1.2.1
 */
public class DistOverviewComponent extends Panel implements Destroyable, DataTreater, ItemListener, HighlightListener, ActionListener {

	static ResourceBundle res = Language.getTextResource("spade.analysis.plot.Res");

	/**
	 * Stores the boolean value whether the component has been destroyed or not.
	 */
	protected boolean destroyed = false;

	/**
	 * Is a reference to the component's supervisor and gives access to its methods.
	 */
	protected Supervisor supervisor = null;

	/**
	 * Stores the IDs of currently selected attributes.
	 */
	protected Vector attributes = null;

	/**
	 * Stores the data table.
	 */
	protected AttributeDataPortion dataTable = null;
	protected Vector attributeColors = null;

	/**
	 * This Vector stores the values of all attributes for the current district.
	 */
	protected Vector values = null;

	/**
	 * This Vector stores the medians of all attributes for the current district.
	 */
	protected Vector medians = new Vector();

	/**
	 * This Vector stores the averages of all attributes for the current district.
	 */
	protected Vector means = new Vector();

	/**
	 * This Vector stores the maximum values of all attributes on the whole map.
	 */
	protected Vector max = new Vector();

	/**
	 * This Vector stores the minimum values of all attributes on the whole map.
	 */
	protected Vector min = new Vector();

	/**
	 * This Vector stores the descriptions of the selected attributes for display
	 * on the graph.
	 */
	protected Vector descrip = new Vector();

	/**
	 * The drop-down box contains all districts on the current map.
	 */
	protected Choice districts = null;

	/**
	 * Contains the bar graph.
	 */
	protected PlotCanvas graphCanvas = null;

	/**
	 * Draws the bar graph.
	 */
	protected DistrictOverviewGraph graph = null;

	/**
	 * Contains the method to filter the value of a single attribute for one district.
	 */
	protected DataFilter flt = new DataFilter();

	/**
	 * The Vector is used to store the previous selected or higlighted district(s)
	 * and at the beginning of each readraw is checked for equality so that
	 * the same calculations do not have to be made twice.
	 */
	protected Vector previousDistricts = new Vector();

	/**
	 * The Vector is used to store selected districs even when JustSelectedDistrictMode
	 * is not enabled so that calculations can be performed at once when districts have
	 * already been selected and the radio button "selected area" is clicked.
	 */
	protected Vector selectedDistricts = null;

	protected SplitPanel panelMain = null;

	/**
	 * The ScrollPane contains the graph.
	 */
	protected ScrollPane graphScroll = null;

	protected ScrollPane tableScroll = null;
	protected PlotCanvas tableCanvas = null;

	protected VisTable table = null;
	protected boolean createNewTable = true;
	protected boolean tableShown = false;

	protected Panel control = null;
	protected Panel controlAll = null;
	protected Panel controlButton = null;
	protected Panel controlRadio = null;
	protected Panel controlBox = null;
	protected Panel controlLegend = null;

	protected AttributeChooser attrSelect = new AttributeChooser();

	protected String headerBeginning = res.getString("statistical_information_for_");
	protected String multiDist = res.getString("_value_averages_are_shown_");
	protected Label header = new Label();

	/**
	 * The CheckBoxGroup is for checking which radio button is currently
	 * selected
	 * (mouse over mode / selection mode)
	 */
	protected CheckboxGroup cbgReactionTo = new CheckboxGroup();
	protected Checkbox cbHighlight = new Checkbox(res.getString("mouseover"), cbgReactionTo, true);
	protected Checkbox cbSelect = new Checkbox(res.getString("selection_change"), cbgReactionTo, false);

	protected Checkbox autoScroll = new Checkbox(res.getString("auto_scroll"), true);

	/**
	 * Stores the index of the currently selected item in the drop-down list.
	 */
	protected int index = 0;

	/**
	 * Initialises all values and variables and draws the initial window.
	 *
	 * @param sup Supervisor
	 * @param dt Data table
	 * @param attr Selected attributes
	 */
	public DistOverviewComponent(Supervisor sup, AttributeDataPortion dt, Vector attr) {
		supervisor = sup;
		supervisor.registerDataDisplayer(this);
		dataTable = dt;
		attributes = attr;
		supervisor.registerHighlightListener(this, dataTable.getEntitySetIdentifier());

		districts = new Choice();
		for (int i = 0; i < dataTable.getDataItemCount(); i++) {
			districts.add(dataTable.getDataItemName(i));
		}
		districts.addItemListener(this);
		districts.setEnabled(true);

		values = new Vector();
		flt.setTable(dataTable);
		Vector regions = new Vector();
		regions.addElement(dataTable.getAttributeId(0));
		flt.setRegion(regions);
		for (int i = 0; i < attributes.size(); i++) {
			int t = dataTable.getAttrIndex((String) attributes.elementAt(i));
			Vector v = flt.filter(t);
			float f = ((Float) v.elementAt(0)).floatValue();
			values.addElement(new Float(f));
		}
		flt.clearRegions();
		updateAllValues();

		setName(res.getString("district_overview_component"));
		graph = new DistrictOverviewGraph();
		graph.addActionListener(this);
		graph.setValues(values);
		graph.setAverages(means);
		graph.setMedians(medians);
		graph.setMaximums(max);
		graph.setMinimums(min);
		graph.setBackgroundColor(Color.lightGray);
		graph.setMargin(20);
		graph.setRightMargin(20);
		graph.setMinSpaceWidth(15);
		graph.setMinBarWidth(25);
		for (int i = 0; i < attributes.size(); i++) {
			String str = dataTable.getAttributeName((String) attributes.elementAt(i));
			descrip.addElement(new String(str));
		}
		graph.setDescriptions(descrip);

		graphCanvas = new PlotCanvas();
		graph.setCanvas(graphCanvas);
		graphCanvas.setContent(graph);

		panelMain = new SplitPanel(false);
		graphScroll = new ScrollPane();
		graphScroll.add(graphCanvas);
		graphScroll.setSize(new Dimension(110 * Metrics.mm(), 90 * Metrics.mm()));
		panelMain.add(graphScroll);

		setLayout(new BorderLayout());
		controlRadio = new Panel(new ColumnLayout());
		controlRadio.add(new Label(res.getString("show_information_by_")));
		cbHighlight.addItemListener(this);
		controlRadio.add(cbHighlight);
		cbSelect.addItemListener(this);
		controlRadio.add(cbSelect);
		ColumnLayout controlButtonLayout = new ColumnLayout();
		controlButtonLayout.setAlignment(ColumnLayout.Hor_Centered);
		controlButton = new Panel(controlButtonLayout);
		Button changeAttribs = new Button(res.getString("add_change_attributes"));
		changeAttribs.addActionListener(this);
		controlButton.add(changeAttribs);
		Button showTable = new Button(res.getString("show_hide_table"));
		showTable.setEnabled(true);
		showTable.addActionListener(this);
		controlButton.add(showTable);
		controlButton.add(autoScroll);
		autoScroll.setEnabled(false);
		autoScroll.setVisible(false);
		controlLegend = new Panel(new FlowLayout(FlowLayout.RIGHT));
		PlotCanvas legendCanvas = new PlotCanvas();
		Legend legend = new Legend();
		legend.setCanvas(legendCanvas);
		legendCanvas.setContent(legend);
		controlLegend.add(legendCanvas);
		controlAll = new Panel(new GridLayout());
		controlAll.add(controlRadio);
		controlAll.add(controlButton);
		controlAll.add(controlLegend);
		controlBox = new Panel(new BorderLayout());
		controlBox.add(new Label(res.getString("districts_")), "West");
		controlBox.add(districts, "Center");
		control = new Panel(new ColumnLayout());
		control.add(controlAll);
		control.add(controlBox);
		add(control, "South");

		header.setText(headerBeginning + districts.getSelectedItem());
		add(header, "North");

		add(panelMain, "Center");

	}

	/**
	 * Converts a Vector of all attribute values from the dataTable
	 * to FloatArrays so that statistical operations from NumValManager can be
	 * performed.
	 *
	 * @param str Attribute name
	 * @return FloatArray of attribute values.
	 */
	protected FloatArray calcArray(String str) {
		FloatArray f = new FloatArray();

		Vector v = new Vector();
		String id = "";
		for (int i = 0; i < attributes.size(); i++) {
			String currentName = dataTable.getAttributeName((String) attributes.elementAt(i));
			if (StringUtil.sameStringsIgnoreCase(str, currentName)) {
				id = (String) attributes.elementAt(i);
			}
		}
		int idx = dataTable.getAttrIndex(id);

		for (int i = 0; i < dataTable.getDataItemCount(); i++) {
			float a = (float) dataTable.getNumericAttrValue(idx, i);
			v.addElement(new Float(a));
		}

		for (int i = 0; i < v.size(); i++) {
			Float b = (Float) v.elementAt(i);
			f.addElement(b.floatValue());
		}

		return f;
	}

	/**
	 * Rounds given statistical float values to four
	 * after-decimal units.
	 *
	 * @param value Float value to be rounded
	 * @return Rounded float value.
	 */
	protected float myRound(float value) {
		float erg = 0f;
		float temp = Math.round(value * 1000f);
		erg = temp / 1000f;
		return erg;
	}

	/**
	 * Unregisters the component from the supervisor and removes the HighlightListener.
	 */
	@Override
	public void destroy() {
		supervisor.removeDataDisplayer(this);
		supervisor.removeHighlightListener(this, dataTable.getEntitySetIdentifier());
		graph.removeActionListener(this);
		destroyed = true;
	}

	/**
	 * Returns the boolean value whether the component has been destroyed.
	 *
	 * @return Boolean value whether the component is destroyed.
	 */
	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

	/**
	 * Returns the list of currently selected attributes.
	 *
	 * @return Vector of ID strings.
	 */
	@Override
	public Vector getAttributeList() {
		return attributes;
	}

	/**
	 * Returns the boolean value whether the component is linked to the data
	 * set on the map.
	 *
	 * @param setId Data set on the map
	 * @return Returns the boolean value whether the component is linked to the
	 *     data set on the map.
	 */
	@Override
	public boolean isLinkedToDataSet(String setId) {
		if (dataTable.getEntitySetIdentifier() == setId)
			return true;
		return false;
	}

	@Override
	public Vector getAttributeColors() {
		return attributeColors;
	}

	/**
	 * Recalculates correlation values after selection changes have been made.
	 *
	 * @param vect Vector of district IDs
	 */
	protected void recalc(Vector vect) {
		flt.setRegion(vect);
		values = new Vector();
		for (int i = 0; i < attributes.size(); i++) {
			int t = dataTable.getAttrIndex((String) attributes.elementAt(i));
			Vector v = flt.filter(t);
			float f = 0f;
			for (int j = 0; j < v.size(); j++) {
				f += ((Float) v.elementAt(j)).floatValue();
			}
			f /= v.size();
			values.addElement(new Float(f));
		}
		flt.clearRegions();

	}

	/**
	 * Redraws the window components after value changes.
	 */
	protected void redraw() {
		graph.setValues(values);
		graph.setup();
		graph.draw(graphCanvas.getGraphics());
		graphScroll.doLayout();
		doLayout();
		header.setText(headerBeginning + districts.getSelectedItem());
		if (tableShown) {
			initTable();
			table.draw(tableCanvas.getGraphics());
			tableScroll.validate();
		}
	}

	protected void update() {
		values = new Vector();
		flt.setTable(dataTable);
		Vector regions = new Vector();
		if ((cbgReactionTo.getSelectedCheckbox() == cbSelect) && (selectedDistricts.size() > 1)) {
			for (int i = 0; i < selectedDistricts.size(); i++) {
				String str = (String) selectedDistricts.elementAt(i);
				regions.addElement(str);
			}
		} else {
			String str = dataTable.getDataItemId(districts.getSelectedIndex());
			regions.addElement(str);

		}

		flt.setRegion(regions);
		for (int i = 0; i < attributes.size(); i++) {
			int t = dataTable.getAttrIndex((String) attributes.elementAt(i));
			Vector v = flt.filter(t);
			float f = ((Float) v.elementAt(0)).floatValue();
			values.addElement(new Float(f));
		}
		flt.clearRegions();

		updateAllValues();

		graph.setValues(values);
		graph.setAverages(means);
		graph.setMedians(medians);
		graph.setMaximums(max);
		graph.setMinimums(min);
		descrip = (Vector) getDesc(attributes).clone();
		graph.setDescriptions(descrip);

		graph.setup();
		graph.draw(graphCanvas.getGraphics());

		initTable();

		if (tableShown) {
			panelMain.removeSplitComponent(1);
			Dimension currentWindowSize = panelMain.getSize();
			int fontHeight = Metrics.getFontMetrics().getHeight();
			int tableHeight = (fontHeight + 1) * ((attributes.size() + 1) < 10 ? attributes.size() + 1 : 10) + 4;
			float proportion = 2 * (float) tableHeight / currentWindowSize.height;
			panelMain.addSplitComponent(tableScroll, proportion < 1.0f ? proportion : 1.0f);
			CManager.validateFully(panelMain);
		}

	}

	protected void updateAllValues() {

		// init vars
		medians = new Vector();
		means = new Vector();
		max = new Vector();
		min = new Vector();

		// recalc values
		for (int i = 0; i < attributes.size(); i++) {
			String text = dataTable.getAttributeName((String) attributes.elementAt(i));
			medians.addElement(new Float(NumValManager.getMedian(calcArray(text))));
			means.addElement(new Float(NumValManager.getMean(calcArray(text))));
			max.addElement(new Float(NumValManager.getMax(calcArray(text))));
			min.addElement(new Float(NumValManager.getMin(calcArray(text))));
		}

	}

	/**
	 * Method is executed when the mouse pointer moves over a
	 * district on the map.
	 *
	 * @param source --
	 * @param setId ID of the data set on the map.
	 * @param dist Vector that contains the ID of the highlighted district.
	 */
	@Override
	public void highlightSetChanged(Object source, String setId, Vector dist) {
		//if ((previousDistricts!=null) && (dist!=null)) { if (dist.equals(previousDistricts)) return; }
		if ((cbgReactionTo.getSelectedCheckbox() == cbHighlight) && (dist != null) && (!previousDistricts.equals(dist))) {
			previousDistricts = dist;
			for (int i = 0; i < dataTable.getDataItemCount(); i++)
				if (dataTable.getDataItemId(i).compareTo((String) dist.elementAt(0)) == 0) {
					index = i;
				}
			districts.select(index);
			recalc(dist);
			redraw();
		}
	}

	/**
	 * Method is executed when the user selects or deselects districts
	 * on the map.
	 *
	 * @param source --
	 * @param setId ID of the data set on the map.
	 * @param dist Vector that contains the IDs of the currently selected
	 * districts.
	 */
	@Override
	public void selectSetChanged(Object source, String setId, Vector dist) {
		selectedDistricts = dist;
		for (int i = 0; i < dataTable.getDataItemCount(); i++) {
			if (dist != null)
				if (dataTable.getDataItemId(i).compareTo((String) dist.elementAt(0)) == 0) {
					index = i;
				}
		}
		if ((cbgReactionTo.getSelectedCheckbox() == cbSelect) && (dist != null) && (previousDistricts != dist)) {
			previousDistricts = dist;
			recalc(dist);
			redraw();
			if (dist.size() > 1) {
				header.setText(headerBeginning + res.getString("multiple_districts") + multiDist);
			} else {
				header.setText(headerBeginning + districts.getSelectedItem());
			}
		}
	}

	/**
	 * Method is executed whenever a radio button is clicked or a region is
	 * selected from the drop-down list.
	 *
	 * @param e Action event
	 */
	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getItem().equals(res.getString("selection_change"))) {
			districts.setEnabled(false);
		}
		if ((e.getItem().equals(res.getString("selection_change"))) && (selectedDistricts != null)) {
			selectSetChanged(null, null, selectedDistricts);
		} else if (e.getItem().equals(res.getString("mouseover"))) {
			districts.setEnabled(true);
		}

		if (e.getSource().getClass() == districts.getClass()) {
			// calculate for region selected in the combo box...
			Vector v = new Vector();
			String str = dataTable.getDataItemId(districts.getSelectedIndex());
			v.addElement(str);
			highlightSetChanged(null, null, v);

			//set highlighting on the map
			supervisor.getHighlighter(dataTable.getEntitySetIdentifier()).makeObjectsHighlighted(this, v);

		}

	}

	/*
	protected String parseDouble(double d) {
	  if (Double.isNaN(d)) {
	    return "---";
	  }
	  if (Double.isInfinite(d)) {
	    return "---";
	  }
	  DecimalFormat df = new DecimalFormat("########0.00");
	  String s = df.format(d);

	  String k = "., ";

	  s = s.replace(k.charAt(0), k.charAt(2));
	  s = s.replace(k.charAt(1), k.charAt(0));
	  return s;
	}
	*/
	@Override
	public void actionPerformed(ActionEvent e) {
		/* Add/Change Attributes */
		if (e.getActionCommand().equals(res.getString("add_change_attributes"))) {
			createNewTable = true;
			attrSelect.setColumnsMayBeReordered(false);
			String prompt = res.getString("choose_attributes");
			attrSelect.selectColumns(dataTable, attributes, null, true, prompt, null);
			if (attrSelect.getSelectedColumnIds() == null)
				return;
			else {
				Vector vect = (Vector) attrSelect.getSelectedColumnIds().clone();
				attributes = vect;
				descrip = (Vector) getDesc(attributes).clone();

			}
			update();
			validate();
		}

		/* Show/Hide Table */
		else if (e.getActionCommand().equals(res.getString("show_hide_table"))) {
			if (tableShown) { // table is already visible = remove table from view
				autoScroll.setEnabled(false);
				autoScroll.setVisible(false);
				panelMain.removeSplitComponent(panelMain.getComponentIndex(tableScroll));
				CManager.validateAll(this);
				panelMain.validate();
				validate();
				tableShown = false;
				return;
			} else { // table not visible -> has to be created = add table to view
				Dimension currentWindowSize = panelMain.getSize();
				if (createNewTable) { // table is created for the first time
					initTable();
					tableCanvas = new PlotCanvas();
					table.setCanvas(tableCanvas);
					tableCanvas.setContent(table);
					if (tableScroll == null) {
						tableScroll = new ScrollPane();
					}
					createNewTable = false;
				}
				int fontHeight = Metrics.getFontMetrics().getHeight();
				int tableHeight = (fontHeight + 1) * ((attributes.size() + 1) < 10 ? attributes.size() + 1 : 10) + 4;
				float proportion = 2 * (float) tableHeight / currentWindowSize.height;
				tableScroll.add(tableCanvas);
				panelMain.addSplitComponent(tableScroll, proportion < 1.0f ? proportion : 1.0f);
				autoScroll.setEnabled(true);
				autoScroll.setVisible(true);
				CManager.validateFully(panelMain);
				tableShown = true;
			}
		}
		/* Highlight row in table */
		else {
			// mouse was moved over a bar in the graph => highlight row in the table
			// if table is shown => redraw
			if (e.getActionCommand().equals("-1")) {
				if (tableShown) {
					table.unmarkAll();
					table.draw(tableCanvas.getGraphics());
					tableScroll.validate();
				}
			} else {
				int rowToMark = (Integer.valueOf(e.getActionCommand())).intValue() + 1;
				if (tableShown) {
					if (autoScroll.getState()) {
						int yMax = tableCanvas.getPreferredSize().height;
						double target = (double) yMax * (double) rowToMark / ((double) attributes.size() + 1);
						tableScroll.setScrollPosition(0, (int) target);
					}
					table.unmarkAll();
					//table.draw(tableCanvas.getGraphics());
					//tableScroll.validate();
					table.markRow(rowToMark);
					table.draw(tableCanvas.getGraphics());
					tableScroll.validate();
				}
			}

		}
	}

	protected Vector getDesc(Vector v) {
		Vector res = new Vector();
		for (int i = 0; i < v.size(); i++) {
			String id = (String) attributes.elementAt(i);
			res.addElement(dataTable.getAttributeName(id));
		}
		return res;
	}

	protected void initTable() {
		if (createNewTable) { // a new instance of table has to be created
			table = new VisTable();
			Vector s = new Vector();
			s.addElement(new String(res.getString("attribute")));
			s.addElement(new String(res.getString("value")));
			s.addElement(new String(res.getString("average")));
			s.addElement(new String(res.getString("min")));
			s.addElement(new String(res.getString("median")));
			s.addElement(new String(res.getString("max")));
			table.appendRow(s);

			for (int i = 0; i < attributes.size(); i++) {
				s = new Vector();
				s.addElement(new String(dataTable.getAttributeName((String) attributes.elementAt(i))));
				float val = ((Float) values.elementAt(i)).floatValue();
				float mean = ((Float) means.elementAt(i)).floatValue();
				float minimum = ((Float) min.elementAt(i)).floatValue();
				float median = ((Float) medians.elementAt(i)).floatValue();
				float maximum = ((Float) max.elementAt(i)).floatValue();

				String valString = StringUtil.floatToStr(val, minimum, maximum);
				String meanString = StringUtil.floatToStr(mean, minimum, maximum);
				String minimumString = StringUtil.floatToStr(minimum, minimum, maximum);
				String medianString = StringUtil.floatToStr(median, minimum, maximum);
				String maximumString = StringUtil.floatToStr(maximum, minimum, maximum);

				s.addElement(new String(valString));
				s.addElement(new String(meanString));
				s.addElement(new String(minimumString));
				s.addElement(new String(medianString));
				s.addElement(new String(maximumString));

				table.appendRow(s);
			}
			table.setLineWidth(1);
			table.setColumnBackgroundColor(0, Color.lightGray);
			table.setRowBackgroundColor(0, Color.yellow);
			table.globalAutoSetColumnWidth();
			table.setAlignmentGlobally(VisTable.RIGHT);
			table.setRowAlignment(0, VisTable.LEFT);
			table.setColumnAlignment(0, VisTable.LEFT);
			table.setCanvas(tableCanvas);
			if (tableCanvas != null) {
				tableCanvas.setContent(table);
			}

		} else { // table already exists
			for (int i = 0; i < attributes.size(); i++) {
				float val = ((Float) values.elementAt(i)).floatValue();
				table.setContent(1, i + 1, new Float(val));
			}
		}

	}

	protected class Legend implements Drawable {

		private Canvas canvas = null;
		private Rectangle bounds = null;
		private boolean isDestroyed = false;

		public Color gridColor = new Color(0xaaaaaa);
		public Color fontColor = new Color(0x000000);
		public Color valueColor = Color.black;
		public Color medianColor = Color.red;
		public Color meanColor = Color.blue;

		public int topMargin = 5;
		public int leftMargin = 5;

		public Legend() {
		}

		@Override
		public void setCanvas(Canvas c) {
			canvas = c;

		}

		@Override
		public Dimension getPreferredSize() {
			Dimension d = new Dimension(108, 70);
			return d;
		}

		@Override
		public void setBounds(Rectangle b) {
			bounds = b;
		}

		@Override
		public Rectangle getBounds() {
			return bounds;
		}

		@Override
		public void draw(Graphics g) {
			g.setColor(gridColor);
			int width1 = 65;
			int width2 = 35;

			int height = 15;

			// table border
			g.drawRect(leftMargin, topMargin, width1 + width2, height * 4);

			// cell border column 1
			Font font = new Font("Arial", 0, 12);
			g.setFont(font);

			g.drawRect(leftMargin, topMargin + height, width1, height);
			g.drawRect(leftMargin, topMargin + 2 * height, width1, height);
			g.drawRect(leftMargin, topMargin + 3 * height, width1, height);

			g.setColor(fontColor);
			g.drawString(res.getString("legend"), leftMargin + 2, topMargin + height - 2);
			g.drawString(res.getString("value"), leftMargin + 2, topMargin + height * 2 - 2);
			g.drawString(res.getString("mean"), leftMargin + 2, topMargin + height * 3 - 2);
			g.drawString(res.getString("median"), leftMargin + 2, topMargin + height * 4 - 2);

			g.setColor(gridColor);
			// cell border column 2
			g.drawRect(leftMargin + width1, topMargin + height, width2, height);
			g.drawRect(leftMargin + width1 + 2, topMargin + height + 2, width2 - 4, height - 4);

			g.drawRect(leftMargin + width1, topMargin + 2 * height, width2, height);
			g.drawRect(leftMargin + width1 + 2, topMargin + 2 * height + 2, width2 - 4, height - 4);

			g.drawRect(leftMargin + width1, topMargin + 3 * height, width2, height);
			g.drawRect(leftMargin + width1 + 2, topMargin + 3 * height + 2, width2 - 4, height - 4);

			g.setColor(valueColor);
			g.fillRect(leftMargin + width1 + 2, topMargin + height + 2, width2 - 4, height - 4);

			g.setColor(meanColor);
			g.fillRect(leftMargin + width1 + 2, topMargin + 2 * height + 2, width2 - 4, height - 4);

			g.setColor(medianColor);
			g.fillRect(leftMargin + width1 + 2, topMargin + 3 * height + 2, width2 - 4, height - 4);

		}

		@Override
		public void destroy() {
			isDestroyed = true;
		}

		@Override
		public boolean isDestroyed() {
			return isDestroyed;
		}

	}

}
