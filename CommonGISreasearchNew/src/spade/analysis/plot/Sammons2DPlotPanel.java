package spade.analysis.plot;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import spade.analysis.system.Supervisor;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.Dialogs;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.PlotCanvas;
import spade.lib.basicwin.SelectDialog;
import spade.lib.util.IdMaker;
import spade.lib.util.IntArray;
import spade.lib.util.QSortAlgorithm;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.geometry.RealPoint;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Apr 7, 2010
 * Time: 4:51:25 PM
 * UI to control the Sammons projection plot
 */
public class Sammons2DPlotPanel extends Panel implements ItemListener, ActionListener, Destroyable, PropertyChangeListener {
	/**
	 * The plot of the Sammon's projection of the data
	 */
	protected Sammons2DPlot samPlot = null;
	/**
	 * The canvas containing the plot
	 */
	protected PlotCanvas canvas = null;
	/**
	 * Used for switching between the "rectangular" and "polar" color scales or
	 * the use of original colors coming from the SOM tool
	 */
	protected Checkbox cbRectColorScale = null, cbPolarColorScale = null, cbNoColorScale = null;
	/**
	 * Whether to flip the color scale relative to the horizontal axis
	 */
	protected Checkbox cbFlip = null;
	/**
	 * Whether to mirror the color scale relative to the vertical axis
	 */
	protected Checkbox cbMirror = null;
	/**
	 * Whether to show object labels on the display
	 */
	protected Checkbox cbLabels = null;
	/**
	 * The buttons are disabled while computations are performed
	 */
	protected Button but[] = null;
	/**
	 * Allows the user to specify the number of iterations to refine or
	 * re-run the projection
	 */
	protected TextField tfNIter = null;
	/**
	 * Allows the user to specify the radius for the Voronoi tessellation
	 */
	protected TextField tfRadius = null;
	/**
	 * Used for broadcasting object colors
	 */
	protected Checkbox broadcastCB = null;
	protected Supervisor supervisor = null;
	/**
	 * The table column with the object classes
	 */
	protected int clColN = -1;
	/**
	 * The table columns with the object coordinates in the projection
	 */
	protected int xColN = -1, yColN = -1;

	public Sammons2DPlotPanel(Sammons2DPlot sp) {
		if (sp == null)
			return;
		this.samPlot = sp;
		supervisor = samPlot.getSupervisor();
		setName(sp.getName());
		canvas = new PlotCanvas();
		canvas.setBackground(Plot.plotAreaColor);
		sp.setCanvas(canvas);
		canvas.setContent(sp);
		setLayout(new BorderLayout());
		add(canvas, BorderLayout.CENTER);

		Panel controlP = new Panel(new ColumnLayout());
		add(controlP, BorderLayout.EAST);
		controlP.add(new Label("Color scale:"));
		CheckboxGroup cbg = new CheckboxGroup();
		cbRectColorScale = new Checkbox("rectangular", true, cbg);
		controlP.add(cbRectColorScale);
		cbRectColorScale.addItemListener(this);
		cbPolarColorScale = new Checkbox("polar", false, cbg);
		controlP.add(cbPolarColorScale);
		cbPolarColorScale.addItemListener(this);
		cbNoColorScale = new Checkbox("none", false, cbg);
		controlP.add(cbNoColorScale);
		cbNoColorScale.addItemListener(this);
		Panel p = new Panel(new GridLayout(1, 2));
		cbFlip = new Checkbox("flip v", false);
		cbFlip.addItemListener(this);
		p.add(cbFlip);
		cbMirror = new Checkbox("mirror >", false);
		cbMirror.addItemListener(this);
		p.add(cbMirror);
		controlP.add(p);
		controlP.add(new Line(false));
		cbLabels = new Checkbox("Show labels", false);
		controlP.add(cbLabels);
		cbLabels.addItemListener(this);
		controlP.add(new Line(false));
		but = new Button[5];
		but[0] = new Button("Refine projection");
		but[0].setActionCommand("refine_projection");
		but[0].addActionListener(this);
		p = new Panel(new FlowLayout(FlowLayout.CENTER, 0, 2));
		p.add(but[0]);
		controlP.add(p);
		but[1] = new Button("Re-run projection");
		but[1].setActionCommand("reproject");
		but[1].addActionListener(this);
		p = new Panel(new FlowLayout(FlowLayout.CENTER, 0, 2));
		p.add(but[1]);
		controlP.add(p);
		tfNIter = new TextField("1000", 5);
		p = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		p.add(new Label("N iterations:"));
		p.add(tfNIter);
		controlP.add(p);
		controlP.add(new Line(false));
		but[2] = new Button("Tessellate");
		but[2].setActionCommand("tessellate");
		but[2].addActionListener(this);
		p = new Panel(new FlowLayout(FlowLayout.CENTER, 0, 2));
		p.add(but[2]);
		controlP.add(p);
		tfRadius = new TextField("10", 4);
		p = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		p.add(new Label("Group radius (%):"));
		p.add(tfRadius);
		controlP.add(p);
		but[3] = new Button("Put classes in table column");
		but[3].setActionCommand("store_classes");
		if (samPlot.getDataTable() instanceof DataTable) {
			but[3].addActionListener(this);
			p = new Panel(new FlowLayout(FlowLayout.CENTER, 0, 2));
			p.add(but[3]);
			controlP.add(p);
		}
		controlP.add(new Line(false));
		broadcastCB = new Checkbox("Broadcast colors", false);
		broadcastCB.addItemListener(this);
		controlP.add(broadcastCB);

		but[4] = new Button("Put coordinates in table");
		but[4].setActionCommand("coords_in_table");
		but[4].addActionListener(this);
		p = new Panel(new FlowLayout(FlowLayout.CENTER, 0, 2));
		p.add(but[4]);
		controlP.add(p);

		supervisor.addPropertyChangeListener(this);
		return;
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		boolean colorsChanged = false;
		if (e.getSource().equals(cbRectColorScale) || e.getSource().equals(cbPolarColorScale) || e.getSource().equals(cbNoColorScale)) {
			samPlot.setColorScale((cbNoColorScale.getState()) ? Sammons2DPlot.color_scale_none : (cbRectColorScale.getState()) ? Sammons2DPlot.color_scale_rectangular : Sammons2DPlot.color_scale_polar);
			if (samPlot.getColorScale() == Sammons2DPlot.color_scale_none) {
				if (samPlot.equals(supervisor.getObjectColorer())) {
					supervisor.removeObjectColorer(samPlot);
				}
				broadcastCB.setState(false);
				broadcastCB.setEnabled(false);
			} else {
				broadcastCB.setEnabled(true);
				colorsChanged = true;
			}
		} else if (e.getSource().equals(broadcastCB)) {
			if (broadcastCB.getState()) { // enable broadcasting
				supervisor.setObjectColorer(samPlot);
			} else { // disable broadcasting
				supervisor.removeObjectColorer(samPlot);
			}
		} else if (e.getSource().equals(cbFlip)) {
			samPlot.setFlipColors(cbFlip.getState());
			colorsChanged = true;
		} else if (e.getSource().equals(cbMirror)) {
			samPlot.setMirrorColors(cbMirror.getState());
			colorsChanged = true;
		} else if (e.getSource().equals(cbLabels)) {
			samPlot.setShowLabels(cbLabels.getState());
			samPlot.invalidateImages();
			samPlot.redraw();
		}
		if (colorsChanged) {
			if (clColN >= 0 && samPlot.hasClasses()) {
				Attribute at = samPlot.getDataTable().getAttribute(clColN);
				String values[] = at.getValueList();
				boolean ok = true;
				if (values != null) {
					Color colors[] = new Color[values.length];
					for (int i = 0; i < values.length && ok; i++) {
						try {
							int clN = Integer.parseInt(values[i]);
							colors[i] = samPlot.getColorForCell(clN);
							ok = colors[i] != null;
						} catch (Exception ex) {
							ok = false;
						}
					}
					if (ok) {
						at.setValueListAndColors(values, colors);
						Vector v = new Vector(1, 1);
						v.addElement(at.getIdentifier());
						samPlot.getDataTable().notifyPropertyChange("values", null, v);
					}
				}
			}
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("reproject") || e.getActionCommand().equals("refine_projection")) {
			int nIter = 1000;
			String str = tfNIter.getText();
			if (str == null) {
				tfNIter.setText("1000");
			} else {
				try {
					nIter = Integer.parseInt(str);
				} catch (Exception ex) {
					tfNIter.setText("1000");
				}
				if (nIter < 1) {
					tfNIter.setText("1000");
					nIter = 1000;
				}
			}
			for (Button element : but) {
				element.setEnabled(false);
			}
			if (e.getActionCommand().equals("reproject")) {
				samPlot.getProjection(nIter);
			} else {
				samPlot.refineProjection(nIter);
			}
			samPlot.redraw();
			for (Button element : but) {
				element.setEnabled(true);
			}
		} else if (e.getActionCommand().equals("tessellate")) {
			String str = tfRadius.getText();
			if (str == null || str.trim().length() < 1) {
				showMessage("The radius is not specified!", true);
				return;
			}
			double rad = -1;
			try {
				rad = Double.parseDouble(str.trim());
			} catch (Exception ex) {
				showMessage("The radius is not properly specified!", true);
				return;
			}
			if (rad <= 0) {
				showMessage("The radius must be positive!", true);
				return;
			}
			if (rad >= 100) {
				showMessage("The radius must be less than 100!", true);
				return;
			}
			for (Button element : but) {
				element.setEnabled(false);
			}
			samPlot.tessellate(rad);
			for (Button element : but) {
				element.setEnabled(true);
			}
		} else if (e.getActionCommand().equals("store_classes")) {
			if (!samPlot.hasClasses()) {
				showMessage("No tessellation defined!", true);
				return;
			}
			int objInCell[] = samPlot.getObjectClasses();
			String pref = "Class N", aName = pref;
			DataTable table = (DataTable) samPlot.getDataTable();
			for (int i = 2; table.findAttrByName(aName) >= 0; i++) {
				aName = pref + " (" + i + ")";
			}
			if (clColN < 0) {
				aName = Dialogs.askForStringValue(CManager.getAnyFrame(this), "Column name?", aName, "A new column with class numbers will be added to the table.", "Column name?", true);
				if (aName == null)
					return;
			} else {
				SelectDialog selDia = new SelectDialog(CManager.getAnyFrame(this), "Column?", "Put the class numbers in the previously created column " + "or make a new column?");
				selDia.addOption("Use previously created column", "prev", true);
				selDia.addLabel(table.getAttributeName(clColN));
				selDia.addOption("Create a new column", "new", false);
				TextField tf = new TextField(aName);
				selDia.addComponent(tf);
				selDia.show();
				if (selDia.wasCancelled())
					return;
				if (selDia.getSelectedOptionN() == 1) {
					aName = tf.getText().trim();
				} else {
					aName = null;
				}
			}
			Attribute at = null;
			boolean newAttr = false;
			if (aName != null) {
				at = new Attribute(IdMaker.makeId(aName, table), AttributeTypes.character);
				at.setName(aName);
				clColN = table.getAttrCount();
				table.addAttribute(at);
				table.makeUniqueAttrIdentifiers();
				newAttr = true;
			} else {
				at = table.getAttribute(clColN);
			}
			IntArray vList = new IntArray(100, 100);
			for (int i = 0; i < objInCell.length; i++) {
				DataRecord rec = table.getDataRecord(i);
				if (objInCell[i] < 0) {
					rec.setAttrValue(null, clColN);
				} else {
					rec.setAttrValue(String.valueOf(objInCell[i]), clColN);
					if (vList.indexOf(objInCell[i]) < 0) {
						vList.addElement(objInCell[i]);
					}
				}
			}
			int numbers[] = vList.getTrimmedArray();
			QSortAlgorithm.sort(numbers);
			Color colors[] = new Color[numbers.length];
			String values[] = new String[numbers.length];
			for (int i = 0; i < numbers.length; i++) {
				values[i] = String.valueOf(numbers[i]);
				colors[i] = samPlot.getColorForCell(numbers[i]);
			}
			at.setValueListAndColors(values, colors);
			Vector v = new Vector(1, 1);
			v.addElement(at.getIdentifier());
			if (newAttr) {
				table.notifyPropertyChange("new_attributes", null, v);
			} else {
				table.notifyPropertyChange("values", null, v);
			}
		} else if (e.getActionCommand().equals("coords_in_table")) {
			RealPoint coords[] = samPlot.getObjectCoordinates();
			if (coords == null) {
				showMessage("Could not get the coordinates!", true);
				return;
			}
			String pref1 = "Projection X", pref2 = "Projection Y", aName1 = pref1, aName2 = pref2;
			DataTable table = (DataTable) samPlot.getDataTable();
			for (int i = 2; table.findAttrByName(aName1) >= 0 || table.findAttrByName(aName2) >= 0; i++) {
				aName1 = pref1 + " (" + i + ")";
				aName2 = pref2 + " (" + i + ")";
			}
			if (xColN < 0 || yColN < 0) {
				String labels[] = { "X-coordinate:", "Y-coordinate" };
				String names[] = { aName1, aName2 };
				names = Dialogs.editStringValues(CManager.getAnyFrame(this), labels, names, "Columns with x- and y-coordinates will be created in the table +\"" + table.getName() + "\". " + "Specify column names:", "Put coordinates in table", true);
				if (names == null)
					return;
				aName1 = names[0];
				aName2 = names[1];
			} else {
				SelectDialog selDia = new SelectDialog(CManager.getAnyFrame(this), "Column?", "Put the coordinates in the previously created columns " + "or make new columns?");
				selDia.addOption("Use previously created columns", "prev", true);
				selDia.addLabel(table.getAttributeName(clColN));
				selDia.addOption("Create new columns", "new", false);
				selDia.addLabel("Column names for the X- and Y- coordinates:");
				TextField tf1 = new TextField(aName1);
				selDia.addComponent(tf1);
				TextField tf2 = new TextField(aName2);
				selDia.addComponent(tf2);
				selDia.show();
				if (selDia.wasCancelled())
					return;
				if (selDia.getSelectedOptionN() == 1) {
					aName1 = tf1.getText().trim();
					aName2 = tf1.getText().trim();
				} else {
					aName1 = aName2 = null;
				}
			}
			Attribute at1 = null, at2 = null;
			boolean newAttr = false;
			if (aName1 != null) {
				at1 = new Attribute(IdMaker.makeId(aName1, table), AttributeTypes.real);
				at1.setName(aName1);
				xColN = table.getAttrCount();
				table.addAttribute(at1);
				at2 = new Attribute(IdMaker.makeId(aName2, table), AttributeTypes.real);
				at2.setName(aName2);
				yColN = table.getAttrCount();
				table.addAttribute(at2);
				table.makeUniqueAttrIdentifiers();
				newAttr = true;
			} else {
				at1 = table.getAttribute(xColN);
				at2 = table.getAttribute(yColN);
			}
			for (int i = 0; i < coords.length; i++) {
				DataRecord rec = table.getDataRecord(i);
				if (coords[i] != null) {
					rec.setNumericAttrValue(coords[i].x, xColN);
					rec.setNumericAttrValue(coords[i].y, yColN);
				}
			}
			Vector v = new Vector(2, 1);
			v.addElement(at1.getIdentifier());
			v.addElement(at2.getIdentifier());
			if (newAttr) {
				table.notifyPropertyChange("new_attributes", null, v);
			} else {
				table.notifyPropertyChange("values", null, v);
			}
		}
	}

	/**
	* Reacts to supervisor's notifications about changing colors. In particular,
	* checks whether the source of the colors (i.e. the current classifier in the
	* supervisor) has changed. If so, sets the state of the checkbox depending
	* on whether or not this classifier is currently used.
	*/
	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		if (pce.getSource().equals(supervisor) && pce.getPropertyName().equals(Supervisor.eventObjectColors)) {
			broadcastCB.setState(samPlot.equals(supervisor.getObjectColorer()));
		}
	}

	protected void showMessage(String msg, boolean isError) {
		if (isError) {
			System.out.println("ERROR: " + msg);
		}
		if (supervisor == null || supervisor.getUI() == null)
			return;
		supervisor.getUI().showMessage(msg, isError);
	}

	/**
	* Makes necessary operations for destroying and notifies its listeners about
	* being destroyed.
	*/
	@Override
	public void destroy() {
		if (samPlot == null)
			return;
		samPlot.destroy();
		supervisor.removeObjectColorer(samPlot);
	}

	/**
	* Replies whether is destroyed or not
	*/
	@Override
	public boolean isDestroyed() {
		if (samPlot == null)
			return true;
		return samPlot.isDestroyed();
	}
}
