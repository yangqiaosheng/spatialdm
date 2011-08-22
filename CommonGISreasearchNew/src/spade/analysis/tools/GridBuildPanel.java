package spade.analysis.tools;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Map;
import java.util.Vector;

import spade.analysis.geocomp.voronoi.VoronoiNew;
import spade.analysis.system.DataLoader;
import spade.analysis.system.ESDACore;
import spade.analysis.system.SystemUI;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.NotificationLine;
import spade.vis.database.SpatialEntity;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DVectorGridLayer;
import spade.vis.dmap.DrawingParameters;
import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealPolyline;
import spade.vis.geometry.RealRectangle;
import spade.vis.map.MapContext;
import spade.vis.map.MapDraw;
import spade.vis.map.MapViewer;
import spade.vis.space.LayerManager;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 06-Oct-2006
 * Time: 15:49:02
 * A UI for constructing a map layer consisting of rectangles that form a
 * regular grid
 */
public class GridBuildPanel extends Panel implements ActionListener, ItemListener, PropertyChangeListener {
	/**
	* The system's core providing access to all its components
	*/
	protected ESDACore core = null;
	/**
	 * The owner receives a message when building is finished
	 */
	protected ActionListener owner = null;
	/**
	 * If the coordinates are geographic (longitudes and latitudes)
	 */
	public boolean geo = false;
	/**
	 * Data boundaries (if known)
	 */
	protected RealRectangle dataBounds = null;
	/**
	* The map in which the layer is constructed
	*/
	protected MapDraw map = null;
	/**
	 * Indicates if anything was drawn on the map
	 */
	protected boolean wasDrawn = false;
	/**
	* The number of the map
	*/
	protected int mapN = -1;
	/**
	 * The layer manager (may be null, if no geographical data have been loaded yet)
	 */
	protected LayerManager lman = null;
	/**
	 * Used for constructing unique identifiers and names
	 */
	private static int nInstances = 0;
	/**
	 * UI controls
	 */
	protected Button buildBt = null;
	protected TextField lNameTF = null, x0TF = null, y0TF = null, x1TF = null, y1TF = null, gwTF = null, ghTF = null, cwTF = null, chTF = null, nColTF = null, nRowTF = null;
	protected Checkbox rectCB = null, hexCB = null;
	protected Checkbox squareCB = null, unionCB = null, intersectCB = null;
	protected List layerList = null;
	protected NotificationLine lStatus = null;
	protected Vector<DGeoLayer> layers = null;
	/**
	 * Indicates that the user prefers to specify cell width and height rather
	 * than number of columns and rows
	 */
	protected boolean cellSizeUserSpecified = false;
	/**
	 * The resulting grid layer
	 */
	protected DVectorGridLayer gridLayer = null;

	public GridBuildPanel(ESDACore core, ActionListener owner, boolean geo) {
		this(core, owner, Float.NaN, Float.NaN, Float.NaN, Float.NaN, geo);
	}

	public GridBuildPanel(ESDACore core, ActionListener owner, float x1, float x2, float y1, float y2, boolean geo) {
		super();
		this.core = core;
		this.owner = owner;
		this.geo = geo;
		++nInstances;

		setLayout(new BorderLayout());

		Panel p = new Panel(new BorderLayout());
		Panel pp = new Panel(new FlowLayout(FlowLayout.CENTER, 10, 2));
		buildBt = new Button("Build");
		buildBt.addActionListener(this);
		buildBt.setActionCommand("build");
		pp.add(buildBt);
		p.add(pp, BorderLayout.CENTER);
		Button b = new Button("Cancel");
		b.addActionListener(this);
		b.setActionCommand("cancel");
		pp = new Panel(new FlowLayout(FlowLayout.CENTER, 10, 2));
		pp.add(b);
		p.add(pp, BorderLayout.EAST);
		add(p, BorderLayout.SOUTH);

		p = new Panel(new BorderLayout());
		p.add(new Label("Layer name:"), BorderLayout.WEST);
		lNameTF = new TextField("Grid " + nInstances, 30);
		p.add(lNameTF, BorderLayout.CENTER);
		b = new Button("Set");
		b.setActionCommand("set_name");
		b.addActionListener(this);
		p.add(b, BorderLayout.EAST);
		if (!Float.isNaN(x1) && !Float.isNaN(x2) && !Float.isNaN(y1) && !Float.isNaN(y2)) {
			dataBounds = new RealRectangle(x1, y1, x2, y2);
		}
		if (dataBounds == null) {
			add(p, BorderLayout.NORTH);
		} else {
			pp = new Panel(new ColumnLayout());
			pp.add(p);
			pp.add(new Line(false));
			pp.add(new Label("Data bounds:", Label.CENTER));
			GridBagLayout gridbag = new GridBagLayout();
			p = new Panel(gridbag);
			GridBagConstraints c = new GridBagConstraints();
			c.weightx = 1.0f;
			c.weighty = 1.0f;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridwidth = GridBagConstraints.REMAINDER;
			Label l = new Label("min X =", Label.RIGHT);
			c.gridwidth = 1;
			gridbag.setConstraints(l, c);
			p.add(l);
			TextField tf = new TextField(String.valueOf(x1));
			tf.setEditable(false);
			c.gridwidth = 2;
			gridbag.setConstraints(tf, c);
			p.add(tf);
			l = new Label("min Y =", Label.RIGHT);
			c.gridwidth = 1;
			gridbag.setConstraints(l, c);
			p.add(l);
			tf = new TextField(String.valueOf(y1));
			tf.setEditable(false);
			c.gridwidth = GridBagConstraints.REMAINDER;
			gridbag.setConstraints(tf, c);
			p.add(tf);
			l = new Label("max X =", Label.RIGHT);
			c.gridwidth = 1;
			gridbag.setConstraints(l, c);
			p.add(l);
			tf = new TextField(String.valueOf(x2));
			tf.setEditable(false);
			c.gridwidth = 2;
			gridbag.setConstraints(tf, c);
			p.add(tf);
			l = new Label("max Y =", Label.RIGHT);
			c.gridwidth = 1;
			gridbag.setConstraints(l, c);
			p.add(l);
			tf = new TextField(String.valueOf(y2));
			tf.setEditable(false);
			c.gridwidth = GridBagConstraints.REMAINDER;
			gridbag.setConstraints(tf, c);
			p.add(tf);
			pp.add(p);
			pp.add(new Line(false));
			add(pp, BorderLayout.NORTH);
		}

		p = new Panel(new GridLayout(5, 1, 0, 2));
		p.add(new Label("x0 :", Label.RIGHT));
		p.add(new Label("x1 :", Label.RIGHT));
		p.add(new Label("Grid width:", Label.RIGHT));
		p.add(new Label("N columns:", Label.RIGHT));
		p.add(new Label("Cell width:", Label.RIGHT));
		pp = new Panel(new BorderLayout());
		pp.add(p, BorderLayout.WEST);
		p = new Panel(new GridLayout(5, 1, 0, 2));
		x0TF = new TextField(10);
		if (!Float.isNaN(x1)) {
			x0TF.setText(String.valueOf(x1));
		}
		x0TF.addActionListener(this);
		p.add(x0TF);
		x1TF = new TextField(10);
		if (!Float.isNaN(x2)) {
			x1TF.setText(String.valueOf(x2));
		}
		x1TF.setEditable(false);
		p.add(x1TF);
		gwTF = new TextField(10);
		gwTF.addActionListener(this);
		if (geo) {
			Panel p1 = new Panel(new BorderLayout());
			p1.add(gwTF, BorderLayout.CENTER);
			p1.add(new Label("m"), BorderLayout.EAST);
			p.add(p1);
		} else {
			p.add(gwTF);
		}
		nColTF = new TextField(10);
		nColTF.addActionListener(this);
		p.add(nColTF);
		cwTF = new TextField(10);
		cwTF.addActionListener(this);
		if (geo) {
			Panel p1 = new Panel(new BorderLayout());
			p1.add(cwTF, BorderLayout.CENTER);
			p1.add(new Label("m"), BorderLayout.EAST);
			p.add(p1);
		} else {
			p.add(cwTF);
		}
		pp.add(p, BorderLayout.CENTER);
		Panel gp = new Panel(new GridLayout(1, 2, 0, 0));
		gp.add(pp);

		p = new Panel(new GridLayout(5, 1, 0, 2));
		p.add(new Label("y0 :", Label.RIGHT));
		p.add(new Label("y1 :", Label.RIGHT));
		p.add(new Label("Grid height:", Label.RIGHT));
		p.add(new Label("N rows:", Label.RIGHT));
		p.add(new Label("Cell height:", Label.RIGHT));
		pp = new Panel(new BorderLayout());
		pp.add(p, BorderLayout.WEST);
		p = new Panel(new GridLayout(5, 1, 0, 2));
		y0TF = new TextField(10);
		if (!Float.isNaN(y1)) {
			y0TF.setText(String.valueOf(y1));
		}
		y0TF.addActionListener(this);
		p.add(y0TF);
		y1TF = new TextField(10);
		if (!Float.isNaN(y2)) {
			y1TF.setText(String.valueOf(y2));
		}
		y1TF.setEditable(false);
		p.add(y1TF);
		ghTF = new TextField(10);
		ghTF.addActionListener(this);
		if (geo) {
			Panel p1 = new Panel(new BorderLayout());
			p1.add(ghTF, BorderLayout.CENTER);
			p1.add(new Label("m"), BorderLayout.EAST);
			p.add(p1);
		} else {
			p.add(ghTF);
		}
		nRowTF = new TextField(10);
		nRowTF.addActionListener(this);
		p.add(nRowTF);
		chTF = new TextField(10);
		chTF.addActionListener(this);
		if (geo) {
			Panel p1 = new Panel(new BorderLayout());
			p1.add(chTF, BorderLayout.CENTER);
			p1.add(new Label("m"), BorderLayout.EAST);
			p.add(p1);
		} else {
			p.add(chTF);
		}
		pp.add(p, BorderLayout.CENTER);
		gp.add(pp);

		Panel mainP = new Panel(new ColumnLayout());
		mainP.add(gp);
		p = new Panel(new GridLayout(2, 2));
		CheckboxGroup cbg = new CheckboxGroup();
		rectCB = new Checkbox("rectangular", true, cbg);
		rectCB.addItemListener(this);
		hexCB = new Checkbox("hexagonal", false, cbg);
		hexCB.addItemListener(this);
		p.add(rectCB);
		p.add(hexCB);
		squareCB = new Checkbox("square cells", true);
		squareCB.addItemListener(this);
		p.add(squareCB);
		mainP.add(p);

		SystemUI ui = core.getUI();
		mapN = (ui != null) ? ui.getCurrentMapN() : 0;
		if (mapN < 0) {
			mapN = 0;
		}
		lman = (core.getDataKeeper() != null) ? core.getDataKeeper().getMap(mapN) : null;
		if (lman != null && lman.getLayerCount() > 0) {
			layerList = new List(Math.min(lman.getLayerCount() + 1, 5), true);
			layers = new Vector<DGeoLayer>(lman.getLayerCount(), 1);
			for (int i = 0; i < lman.getLayerCount(); i++)
				if (lman.getGeoLayer(i) instanceof DGeoLayer) {
					DGeoLayer layer = (DGeoLayer) lman.getGeoLayer(i);
					RealRectangle rr = layer.getWholeLayerBounds();
					if (rr == null) {
						rr = layer.getCurrentLayerBounds();
					}
					if (rr != null) {
						layerList.add(layer.getName());
						layers.addElement(layer);
					}
				}
			if (layerList.getItemCount() < 1) {
				layerList = null;
			} else {
				layerList.addItemListener(this);
				mainP.add(new Label("Use the boundaries of the layer(s):"));
				if (layerList.getItemCount() > 1) {
					p = new Panel(new ColumnLayout());
					b = new Button("Select all");
					b.setActionCommand("select_all_layers");
					b.addActionListener(this);
					pp = new Panel(new FlowLayout(FlowLayout.CENTER, 3, 2));
					pp.add(b);
					p.add(pp);
					p.add(new Label(""));
					cbg = new CheckboxGroup();
					unionCB = new Checkbox("union", true, cbg);
					unionCB.addItemListener(this);
					p.add(unionCB);
					intersectCB = new Checkbox("intersection", false, cbg);
					intersectCB.addItemListener(this);
					p.add(intersectCB);
					pp = new Panel(new BorderLayout(5, 0));
					pp.add(layerList, BorderLayout.CENTER);
					pp.add(p, BorderLayout.EAST);
					mainP.add(pp);
				} else {
					mainP.add(layerList);
				}
			}
		}
		lStatus = new NotificationLine(null);
		mainP.add(lStatus);

		add(mainP, BorderLayout.CENTER);

		if (ui != null) {
			MapViewer mview = ui.getCurrentMapViewer();
			if (mview != null) {
				map = mview.getMapDrawer();
			}
			drawDataBounds();
		}
	}

	/**
	 * Uses the current settings of x0 and grid width to set the value of x1
	 */
	protected void setX1() {
		float x0 = Float.NaN, w = Float.NaN;
		try {
			x0 = Float.valueOf(x0TF.getText()).floatValue();
		} catch (Exception ex) {
		}
		if (Float.isNaN(x0))
			return;
		try {
			w = Float.valueOf(gwTF.getText()).floatValue();
		} catch (Exception ex) {
		}
		if (Float.isNaN(w))
			return;
		float x1 = Float.NaN, y0 = 0, y1 = 0;
		try {
			x1 = Float.valueOf(x1TF.getText()).floatValue();
			y0 = Float.valueOf(y0TF.getText()).floatValue();
			y1 = Float.valueOf(y1TF.getText()).floatValue();
		} catch (Exception ex) {
		}
		if (!Float.isNaN(x1)) {
			double wh[] = DGeoLayer.getExtentXY(x0, y0, x1, y1, geo);
			double ratio = (x1 - x0) / wh[0];
			w *= ratio;
		}
		x1TF.setText(String.valueOf(x0 + w));
	}

	/**
	 * Uses the current settings of y0 and grid height to set the value of y1
	 */
	protected void setY1() {
		float y0 = Float.NaN, h = Float.NaN;
		try {
			y0 = Float.valueOf(y0TF.getText()).floatValue();
		} catch (Exception ex) {
		}
		if (Float.isNaN(y0))
			return;
		try {
			h = Float.valueOf(ghTF.getText()).floatValue();
		} catch (Exception ex) {
		}
		if (Float.isNaN(h))
			return;
		float y1 = Float.NaN, x0 = 0, x1 = 0;
		try {
			y1 = Float.valueOf(y1TF.getText()).floatValue();
			x0 = Float.valueOf(x0TF.getText()).floatValue();
			x1 = Float.valueOf(x1TF.getText()).floatValue();
		} catch (Exception ex) {
		}
		if (!Float.isNaN(x1)) {
			double wh[] = DGeoLayer.getExtentXY(x0, y0, x1, y1, geo);
			double ratio = (y1 - y0) / wh[1];
			h *= ratio;
		}
		y1TF.setText(String.valueOf(y0 + h));
	}

	/**
	 * Counts and sets the number of columns according to the specified cell width.
	 * x1 is adjusted so that all columns have equal widths.
	 * In case of geographic coordinates, it is assumed that colW is given in meters
	 */
	protected int countNCols(float colW) {
		if (colW <= 0)
			return -1;
		float x0 = Float.NaN, w = Float.NaN;
		try {
			x0 = Float.valueOf(x0TF.getText()).floatValue();
		} catch (Exception ex) {
		}
		if (Float.isNaN(x0))
			return -1;
		try {
			w = Float.valueOf(gwTF.getText()).floatValue();
		} catch (Exception ex) {
		}
		int ncols = -1;
		if (Float.isNaN(w)) {
			try {
				ncols = Integer.valueOf(nColTF.getText()).intValue();
			} catch (Exception ex) {
				return -1;
			}
		} else {
			ncols = (int) Math.ceil(1.0 * w / colW);
		}
		if (ncols < 1) {
			ncols = 1;
		}
		nColTF.setText(String.valueOf(ncols));
		w = colW * ncols;
		gwTF.setText(String.valueOf(w));
		setX1();
		return ncols;
	}

	/**
	 * Counts and sets the cell width according to the specified number of columns
	 */
	protected float countCellWidth(int ncols) {
		if (ncols < 1)
			return Float.NaN;
		float w = Float.NaN;
		try {
			w = Float.valueOf(gwTF.getText()).floatValue();
		} catch (Exception ex) {
		}
		if (Float.isNaN(w))
			return Float.NaN;
		float cw = w / ncols;
		cwTF.setText(String.valueOf(cw));
		return cw;
	}

	/**
	 * Counts and sets the grid width according to the current number of columns
	 * and column width
	 */
	protected float countGridWidth() {
		int ncols = -1;
		try {
			ncols = Integer.valueOf(nColTF.getText()).intValue();
		} catch (Exception ex) {
			return Float.NaN;
		}
		if (ncols < 1)
			return Float.NaN;
		float colW = Float.NaN;
		try {
			colW = Float.valueOf(cwTF.getText()).floatValue();
		} catch (Exception ex) {
		}
		if (Float.isNaN(colW))
			return Float.NaN;
		float w = colW * ncols;
		gwTF.setText(String.valueOf(w));
		float x0 = Float.NaN;
		try {
			x0 = Float.valueOf(x0TF.getText()).floatValue();
		} catch (Exception ex) {
		}
		setX1();
		return w;
	}

	/**
	 * Counts and sets the number of rows according to the specified cell height
	 * y1 is adjusted so that all columns have equal heights.
	 * In case of geographic coordinates, it is assumed that rowH is given in meters
	 */
	protected int countNRows(float rowH) {
		if (rowH <= 0)
			return -1;
		float y0 = Float.NaN, h = Float.NaN;
		try {
			y0 = Float.valueOf(y0TF.getText()).floatValue();
		} catch (Exception ex) {
		}
		if (Float.isNaN(y0))
			return -1;
		try {
			h = Float.valueOf(ghTF.getText()).floatValue();
		} catch (Exception ex) {
		}
		int nrows = -1;
		if (Float.isNaN(h)) {
			try {
				nrows = Integer.valueOf(nRowTF.getText()).intValue();
			} catch (Exception ex) {
				return -1;
			}
		} else {
			nrows = (int) Math.ceil(1.0 * h / rowH);
		}
		if (nrows < 1) {
			nrows = 1;
		}
		nRowTF.setText(String.valueOf(nrows));
		h = rowH * nrows;
		ghTF.setText(String.valueOf(h));
		setY1();
		return nrows;
	}

	/**
	 * Counts and sets the cell height according to the specified number of rows
	 */
	protected float countCellHeight(int nrows) {
		if (nrows < 1)
			return Float.NaN;
		float rowH = Float.NaN;
		try {
			rowH = Float.valueOf(ghTF.getText()).floatValue();
		} catch (Exception ex) {
		}
		if (Float.isNaN(rowH))
			return Float.NaN;
		float ch = rowH / nrows;
		cwTF.setText(String.valueOf(ch));
		return ch;
	}

	/**
	 * Counts and sets the grid height according to the current number of rows
	 * and row height
	 */
	protected float countGridHeight() {
		int nrows = -1;
		try {
			nrows = Integer.valueOf(nRowTF.getText()).intValue();
		} catch (Exception ex) {
			return Float.NaN;
		}
		if (nrows < 1)
			return Float.NaN;
		float rowH = Float.NaN;
		try {
			rowH = Float.valueOf(chTF.getText()).floatValue();
		} catch (Exception ex) {
		}
		if (Float.isNaN(rowH))
			return Float.NaN;
		float h = rowH * nrows;
		ghTF.setText(String.valueOf(h));
		float y0 = Float.NaN;
		try {
			y0 = Float.valueOf(y0TF.getText()).floatValue();
		} catch (Exception ex) {
		}
		setY1();
		return h;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		lStatus.showMessage(null, false);
		String cmd = e.getActionCommand();
		if (e.getSource() instanceof TextField) {
			TextField tf = (TextField) e.getSource();
			String txt = tf.getText();
			float val = Float.NaN;
			try {
				val = Float.valueOf(txt).floatValue();
			} catch (Exception ex) {
				lStatus.showMessage("Not a number entered!", true);
				return;
			}
			if (tf.equals(cwTF) || tf.equals(chTF) || tf.equals(nColTF) || tf.equals(nRowTF)) {
				if (val <= 0) {
					lStatus.showMessage("The number in this field must be positive!", true);
					return;
				}
				int ival = Math.round(val);
				if (tf.equals(nColTF) || tf.equals(nRowTF)) {
					if (ival < 1) {
						lStatus.showMessage("The number of rows/columns must be at least 1!", true);
						return;
					} else {
						tf.setText(String.valueOf(ival));
					}
				} else {
					cellSizeUserSpecified = true;
				}
				if (tf.equals(cwTF)) {
					countNCols(val);
					if (squareCB.getState()) {
						chTF.setText(txt);
						countNRows(val);
					}
				} else if (tf.equals(chTF)) {
					countNRows(val);
					if (squareCB.getState()) {
						cwTF.setText(txt);
						countNCols(val);
					}
				} else if (tf.equals(nColTF)) {
					if (cellSizeUserSpecified) {
						countGridWidth();
					} else {
						float w = countCellWidth(ival);
						if (!Float.isNaN(w) && squareCB.getState()) {
							chTF.setText(cwTF.getText());
							countNRows(w);
						}
					}
				} else if (tf.equals(nRowTF)) {
					if (cellSizeUserSpecified) {
						countGridHeight();
					} else {
						float h = countCellHeight(ival);
						if (!Float.isNaN(h) && squareCB.getState()) {
							cwTF.setText(chTF.getText());
							countNCols(h);
						}
					}
				}
			} else if (tf.equals(x0TF) || tf.equals(gwTF)) {
				if (cellSizeUserSpecified) {
					float w = Float.NaN;
					try {
						w = Float.valueOf(cwTF.getText()).floatValue();
					} catch (Exception ex) {
					}
					if (Float.isNaN(w) || w <= 0) {
						cellSizeUserSpecified = false;
					} else {
						countNCols(w);
					}
				}
				if (!cellSizeUserSpecified) {
					int ncols = -1;
					try {
						ncols = Integer.valueOf(nColTF.getText()).intValue();
					} catch (Exception ex) {
					}
					if (ncols > 0) {
						float w = countCellWidth(ncols);
						if (!Float.isNaN(w) && squareCB.getState()) {
							chTF.setText(cwTF.getText());
							countNRows(w);
						}
					}
				}
			} else if (tf.equals(y0TF) || tf.equals(ghTF)) {
				if (cellSizeUserSpecified) {
					float h = Float.NaN;
					try {
						h = Float.valueOf(chTF.getText()).floatValue();
					} catch (Exception ex) {
					}
					if (Float.isNaN(h) || h <= 0) {
						cellSizeUserSpecified = false;
					} else {
						countNRows(h);
					}
				}
				if (!cellSizeUserSpecified) {
					int nrows = -1;
					try {
						nrows = Integer.valueOf(nRowTF.getText()).intValue();
					} catch (Exception ex) {
					}
					if (nrows > 0) {
						float h = countCellHeight(nrows);
						if (!Float.isNaN(h) && squareCB.getState()) {
							cwTF.setText(chTF.getText());
							countNCols(h);
						}
					}
				}
			}
			drawGrid();
			return;
		}
		if (cmd == null)
			return;
		if (cmd.equals("cancel")) {
			if (map != null) {
				map.removePropertyChangeListener(this);
				if (wasDrawn) {
					map.redraw();
				}
			}
			owner.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "cancel"));
			return;
		}
		if (cmd.equals("select_all_layers")) {
			if (layerList == null)
				return;
			layerList.removeItemListener(this);
			for (int i = 0; i < layerList.getItemCount(); i++) {
				layerList.select(i);
			}
			layerList.addItemListener(this);
			getGridBounds();
			return;
		}
		if (cmd.equals("build")) {
			if (!validateGridParameters(true))
				return;
			if (map != null) {
				map.removePropertyChangeListener(this);
				if (wasDrawn) {
					map.redraw();
				}
			}
			if (rectCB.getState()) {
				gridLayer = new DVectorGridLayer();
				gridLayer.constructObjects(getVertXCoord(), getHorzYCoord());
				gridLayer.setName(lNameTF.getText());
				DrawingParameters dp = new DrawingParameters();
				dp.fillContours = false;
				dp.lineColor = new Color((int) Math.round(255 * Math.random()), (int) Math.round(255 * Math.random()), (int) Math.round(255 * Math.random()));
				gridLayer.setDrawingParameters(dp);
				DataLoader loader = core.getDataLoader();
				loader.addMapLayer(gridLayer, mapN);
			} else {
				float x0 = Float.NaN, dx = Float.NaN;
				int ncols = 0;
				try {
					x0 = Float.valueOf(x0TF.getText()).floatValue();
					dx = Float.valueOf(cwTF.getText()).floatValue();
					ncols = Integer.valueOf(nColTF.getText()).intValue();
				} catch (Exception ex) {
					return;
				}
				if (Float.isNaN(x0) || Float.isNaN(dx) || ncols < 1)
					return;
				float y0 = Float.NaN, dy = Float.NaN;
				int nrows = 0;
				try {
					y0 = Float.valueOf(y0TF.getText()).floatValue();
					dy = Float.valueOf(chTF.getText()).floatValue();
					nrows = Integer.valueOf(nRowTF.getText()).intValue();
				} catch (Exception ex) {
					return;
				}
				if (Float.isNaN(y0) || Float.isNaN(dy) || nrows < 1)
					return;
				if (geo) {
					float x1 = Float.NaN, y1 = Float.NaN;
					try {
						x1 = Float.valueOf(x1TF.getText()).floatValue();
						y1 = Float.valueOf(y1TF.getText()).floatValue();
					} catch (Exception ex) {
						return;
					}
					dx = (x1 - x0) / ncols;
					dy = (y1 - y0) / nrows;
				}
				float dx2 = dx / 2;
				Vector<RealPoint> points = new Vector<RealPoint>((ncols + 1) * (nrows + 1), 10);
				float y = y0;
				for (int row = 0; row <= nrows; row++) {
					float x = x0;
					int nc = ncols;
					if (row % 2 == 1) {
						x -= dx2;
						++nc;
					}
					for (int col = 0; col <= nc; col++) {
						points.addElement(new RealPoint(x, y));
						x += dx;
					}
					y += dy;
				}
				VoronoiNew voronoi = new VoronoiNew(points);
				if (!voronoi.isValid())
					return;
				voronoi.setBuildNeighbourhoodMatrix(true);
				float x1 = x0 - dx2, x2 = x1 + (ncols + 1) * dx, y1 = y0 - dy / 2, y2 = y1 + (nrows + 1) * dy;
				RealPolyline areas[] = voronoi.getPolygons(x1, y1, x2, y2);
				if (areas == null)
					return;
				Vector<DGeoObject> cells = new Vector<DGeoObject>(areas.length, 10);
				int cellIdxs[] = new int[areas.length];
				for (int i = 0; i < areas.length; i++)
					if (areas[i] != null) {
						SpatialEntity spe = new SpatialEntity(String.valueOf(i + 1));
						spe.setGeometry(areas[i]);
						DGeoObject obj = new DGeoObject();
						obj.setup(spe);
						cells.addElement(obj);
						cellIdxs[i] = cells.size() - 1;
					} else {
						cellIdxs[i] = -1;
						//adding information about the neighbours of the cells
					}

				Map<Integer, Integer> neighbourMap = voronoi.getNeighbourhoodMap();
				if (neighbourMap != null) {
					for (int i = 0; i < areas.length; i++)
						if (cellIdxs[i] >= 0) {
							DGeoObject pObj = cells.elementAt(cellIdxs[i]);
							for (int j = 0; j < areas.length; j++)
								if (j != i && neighbourMap.get(i) == j && cellIdxs[j] >= 0) {
									pObj.addNeighbour(cells.elementAt(cellIdxs[j]));
								}
						}
				}
				DGeoLayer hexLayer = new DGeoLayer();
				hexLayer.setGeoObjects(cells, true);
				hexLayer.setName(lNameTF.getText());
				DrawingParameters dp = new DrawingParameters();
				dp.fillContours = false;
				dp.lineColor = new Color((int) Math.round(255 * Math.random()), (int) Math.round(255 * Math.random()), (int) Math.round(255 * Math.random()));
				hexLayer.setDrawingParameters(dp);
				DataLoader loader = core.getDataLoader();
				loader.addMapLayer(hexLayer, mapN);
			}
			owner.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "finish"));
			return;
		}
	}

	/**
	 * Returns the resulting grid layer
	 */
	public DVectorGridLayer getGridLayer() {
		return gridLayer;
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource().equals(rectCB) || e.getSource().equals(hexCB)) {
			squareCB.setEnabled(rectCB.getState());
			if (!squareCB.isEnabled()) {
				squareCB.setState(false);
			}
		} else if (e.getSource().equals(squareCB)) {
			if (squareCB.getState()) {
				validateGridParameters(false);
				drawGrid();
			}
		} else if (e.getSource().equals(layerList) || e.getSource().equals(unionCB) || e.getSource().equals(intersectCB)) {
			getGridBounds();
		}
	}

	protected void getGridBounds() {
		if (lman == null || layerList == null)
			return;
		int idx[] = layerList.getSelectedIndexes();
		if (idx == null || idx.length < 1)
			return;
		float x0 = Float.NaN, x1 = Float.NaN, y0 = Float.NaN, y1 = Float.NaN;
		boolean union = (unionCB != null) ? unionCB.getState() : true;
		for (int element : idx) {
			DGeoLayer layer = layers.elementAt(element);
			RealRectangle rr = layer.getWholeLayerBounds();
			if (rr == null) {
				rr = layer.getCurrentLayerBounds();
			}
			if (rr == null) {
				continue;
			}
			if (Float.isNaN(x0) || (union && x0 > rr.rx1) || (!union && x0 < rr.rx1)) {
				x0 = rr.rx1;
			}
			if (Float.isNaN(y0) || (union && y0 > rr.ry1) || (!union && y0 < rr.ry1)) {
				y0 = rr.ry1;
			}
			if (Float.isNaN(x1) || (union && x1 < rr.rx2) || (!union && x1 > rr.rx2)) {
				x1 = rr.rx2;
			}
			if (Float.isNaN(y1) || (union && y1 < rr.ry2) || (!union && y1 > rr.ry2)) {
				y1 = rr.ry2;
			}
		}
		if (idx.length == layerList.getItemCount()) {
			for (int element : idx) {
				layerList.deselect(element);
			}
		}
		if (Float.isNaN(x0))
			return;
		x0TF.setText(String.valueOf(x0));
		y0TF.setText(String.valueOf(y0));
		x1TF.setText(String.valueOf(x1));
		y1TF.setText(String.valueOf(y1));
		double wh[] = DGeoLayer.getExtentXY(x0, y0, x1, y1, geo);
		gwTF.setText(String.valueOf((float) wh[0]));
		ghTF.setText(String.valueOf((float) wh[1]));
		validateGridParameters(false);
		drawGrid();
	}

	/**
	 * Validates the grid parameters so that they are consistent.
	 * Returns true if all parameters are available.
	 */
	protected boolean validateGridParameters(boolean showMessage) {
		float x0 = Float.NaN, w = Float.NaN, y0 = Float.NaN, h = Float.NaN, colW = Float.NaN, rowH = Float.NaN;
		try {
			x0 = Float.valueOf(x0TF.getText()).floatValue();
		} catch (Exception ex) {
		}
		try {
			w = Float.valueOf(gwTF.getText()).floatValue();
		} catch (Exception ex) {
		}
		try {
			y0 = Float.valueOf(y0TF.getText()).floatValue();
		} catch (Exception ex) {
		}
		try {
			h = Float.valueOf(ghTF.getText()).floatValue();
		} catch (Exception ex) {
		}
		try {
			colW = Float.valueOf(cwTF.getText()).floatValue();
			if (colW <= 0) {
				if (!Float.isNaN(w)) {
					colW = w;
				}
				if (colW <= 0) {
					cwTF.setText("");
					colW = Float.NaN;
				} else {
					cwTF.setText(String.valueOf(colW));
				}
			}
		} catch (Exception ex) {
		}
		try {
			rowH = Float.valueOf(chTF.getText()).floatValue();
			if (rowH <= 0) {
				if (!Float.isNaN(h)) {
					rowH = h;
				}
				if (rowH <= 0) {
					chTF.setText("");
					rowH = Float.NaN;
				} else {
					chTF.setText(String.valueOf(rowH));
				}
			}
		} catch (Exception ex) {
		}
		int ncols = -1;
		try {
			ncols = Integer.valueOf(nColTF.getText()).intValue();
			if (ncols < 1) {
				ncols = 1;
				nColTF.setText(String.valueOf(ncols));
			}
		} catch (Exception ex) {
		}
		if (squareCB.getState() && Float.isNaN(colW) && !Float.isNaN(rowH)) {
			colW = rowH;
			cwTF.setText(String.valueOf(colW));
			int n = countNCols(colW);
			if (n > 0) {
				ncols = n;
				w = Float.valueOf(gwTF.getText()).floatValue();
			}
		}
		if (!Float.isNaN(x0))
			if (Float.isNaN(w))
				if (!Float.isNaN(colW) && colW > 0 && ncols > 0) {
					w = colW * ncols;
					gwTF.setText(String.valueOf(w));
				} else {
					gwTF.setText("");
				}
		if (!Float.isNaN(x0) && !Float.isNaN(w))
			if (Float.isNaN(colW) && ncols > 0) {
				colW = countCellWidth(ncols);
			}
		if (!Float.isNaN(colW) && (ncols < 0 || cellSizeUserSpecified)) {
			ncols = countNCols(colW);
			if (ncols > 0) {
				w = Float.valueOf(gwTF.getText()).floatValue();
			}
		}
		int nrows = -1;
		try {
			nrows = Integer.valueOf(nRowTF.getText()).intValue();
			if (nrows < 1) {
				nrows = 1;
				nRowTF.setText(String.valueOf(nrows));
			}
		} catch (Exception ex) {
		}
		if (squareCB.getState() && !Float.isNaN(colW) && (Float.isNaN(rowH) || rowH != colW)) {
			rowH = colW;
			chTF.setText(String.valueOf(rowH));
			int n = countNRows(rowH);
			if (n > 0) {
				nrows = n;
				h = Float.valueOf(ghTF.getText()).floatValue();
			}
		}
		if (!Float.isNaN(y0))
			if (Float.isNaN(h))
				if (!Float.isNaN(rowH) && rowH > 0 && nrows > 0) {
					h = rowH * nrows;
					ghTF.setText(String.valueOf(h));
				} else {
					ghTF.setText("");
				}
		if (!Float.isNaN(y0) && !Float.isNaN(h))
			if (Float.isNaN(rowH) && nrows > 0) {
				rowH = countCellHeight(nrows);
			}
		if (!Float.isNaN(rowH)) {
			nrows = countNRows(rowH);
			if (nrows > 0) {
				h = Float.valueOf(ghTF.getText()).floatValue();
			}
		}
		if (squareCB.getState() && Float.isNaN(colW) && !Float.isNaN(rowH)) {
			colW = rowH;
			cwTF.setText(String.valueOf(colW));
			int n = countNCols(colW);
			if (n > 0) {
				ncols = n;
				w = Float.valueOf(gwTF.getText()).floatValue();
			}
		}
		if (Float.isNaN(x0)) {
			if (showMessage) {
				lStatus.showMessage("x0 is not specified!", true);
			}
			return false;
		}
		if (Float.isNaN(w)) {
			if (showMessage) {
				lStatus.showMessage("Grid width is not specified!", true);
			}
			return false;
		}
		if (w <= 0) {
			if (showMessage) {
				lStatus.showMessage("Grid width must be positive!", true);
			}
			return false;
		}
		if (Float.isNaN(y0)) {
			if (showMessage) {
				lStatus.showMessage("y0 is not specified!", true);
			}
			return false;
		}
		if (Float.isNaN(h)) {
			if (showMessage) {
				lStatus.showMessage("Grid height is not specified!", true);
			}
			return false;
		}
		if (h <= 0) {
			if (showMessage) {
				lStatus.showMessage("Grid height must be positive!", true);
			}
			return false;
		}
		if (Float.isNaN(colW)) {
			if (showMessage) {
				lStatus.showMessage("Cell width is not specified!", true);
			}
			return false;
		}
		if (colW <= 0) {
			if (showMessage) {
				lStatus.showMessage("Cell width must be positive!", true);
			}
			return false;
		}
		if (Float.isNaN(rowH)) {
			if (showMessage) {
				lStatus.showMessage("Cell height is not specified!", true);
			}
			return false;
		}
		if (rowH <= 0) {
			if (showMessage) {
				lStatus.showMessage("Cell height must be positive!", true);
			}
			return false;
		}
		if (ncols < 1) {
			lStatus.showMessage("Number of columns must be at least 1!", true);
			return false;
		}
		if (nrows < 1) {
			lStatus.showMessage("Number of rows must be at least 1!", true);
			return false;
		}
		return true;
	}

	/**
	 * If a map is available and the bounds of the data are known, draws a rectangle
	 * indicating the current data bounds
	 */
	protected void drawDataBounds() {
		if (map == null || dataBounds == null)
			return;
		Graphics g = map.getGraphics();
		if (g == null)
			return;
		map.removePropertyChangeListener(this);
		if (wasDrawn) {
			map.redraw();
		}
		map.addPropertyChangeListener(this);
		MapContext mc = map.getMapContext();
		int sx0 = mc.scrX(dataBounds.rx1, dataBounds.ry1), sx1 = mc.scrX(dataBounds.rx2, dataBounds.ry2), sy0 = mc.scrY(dataBounds.rx1, dataBounds.ry1), sy1 = mc.scrY(dataBounds.rx2, dataBounds.ry2);
		g.setColor(Color.red);
		g.drawRect(sx0, sy1, sx1 - sx0, sy0 - sy1);
		wasDrawn = true;
	}

	/**
	 * Tries to draw a grid with the current parameters, if a map is available
	 */
	protected void drawGrid() {
		if (map == null)
			return;
		Graphics g = map.getGraphics();
		if (g == null)
			return;
		map.removePropertyChangeListener(this);
		if (wasDrawn) {
			map.redraw();
		}
		map.addPropertyChangeListener(this);
		MapContext mc = map.getMapContext();
		if (dataBounds != null) {
			int sx0 = mc.scrX(dataBounds.rx1, dataBounds.ry1), sx1 = mc.scrX(dataBounds.rx2, dataBounds.ry2), sy0 = mc.scrY(dataBounds.rx1, dataBounds.ry1), sy1 = mc.scrY(dataBounds.rx2, dataBounds.ry2);
			g.setColor(Color.red);
			g.drawRect(sx0, sy1, sx1 - sx0, sy0 - sy1);
			wasDrawn = true;
		}
		float x0 = Float.NaN, x1 = Float.NaN, y0 = Float.NaN, y1 = Float.NaN;
		try {
			x0 = Float.valueOf(x0TF.getText()).floatValue();
			y0 = Float.valueOf(y0TF.getText()).floatValue();
			x1 = Float.valueOf(x1TF.getText()).floatValue();
			y1 = Float.valueOf(y1TF.getText()).floatValue();
		} catch (Exception e) {
			return;
		}
		if (Float.isNaN(x0) || Float.isNaN(x1) || Float.isNaN(y0) || Float.isNaN(y1))
			return;
		int sx0 = mc.scrX(x0, y0), sx1 = mc.scrX(x1, y1), sy0 = mc.scrY(x0, y0), sy1 = mc.scrY(x1, y1);
		g.setColor(Color.yellow);
		g.drawRect(sx0, sy1, sx1 - sx0, sy0 - sy1);
		wasDrawn = true;
		float colW = Float.NaN;
		int ncols = -1;
		try {
			colW = Float.valueOf(cwTF.getText()).floatValue();
			ncols = Integer.valueOf(nColTF.getText()).intValue();
		} catch (Exception ex) {
		}
		if (!Float.isNaN(colW) && ncols > 1) {
			if (geo) {
				colW = (x1 - x0) / ncols;
			}
			float x = x0;
			for (int i = 0; i < ncols - 1; i++) {
				x += colW;
				int sx = mc.scrX(x, y0);
				g.drawLine(sx, sy0, sx, sy1);
			}
		}
		float h = Float.NaN;
		int nrows = -1;
		try {
			h = Float.valueOf(chTF.getText()).floatValue();
			nrows = Integer.valueOf(nRowTF.getText()).intValue();
		} catch (Exception ex) {
		}
		if (!Float.isNaN(h) && nrows > 1) {
			if (geo) {
				h = (y1 - y0) / nrows;
			}
			float y = y0;
			for (int i = 0; i < nrows - 1; i++) {
				y += h;
				int sy = mc.scrY(x0, y);
				g.drawLine(sx0, sy, sx1, sy);
			}
		}
	}

	/**
	* Reacts to map repainting, zooming, etc. In such cases must restore the
	* drawing of the current grid.
	*/
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getSource().equals(map)) {
			wasDrawn = false;
			drawGrid();
		}
	}

	/**
	 * Returns the x-coordinates of the vertical grid lines
	 */
	public float[] getVertXCoord() {
		float x0 = Float.NaN, colW = Float.NaN;
		int ncols = 0;
		try {
			x0 = Float.valueOf(x0TF.getText()).floatValue();
			colW = Float.valueOf(cwTF.getText()).floatValue();
			ncols = Integer.valueOf(nColTF.getText()).intValue();
		} catch (Exception ex) {
			return null;
		}
		if (Float.isNaN(x0) || Float.isNaN(colW) || ncols < 1)
			return null;
		if (geo) {
			float x1 = Float.NaN;
			try {
				x1 = Float.valueOf(x1TF.getText()).floatValue();
			} catch (Exception ex) {
			}
			colW = (x1 - x0) / ncols;
		}
		float coord[] = new float[ncols + 1];
		coord[0] = x0;
		for (int i = 0; i < ncols; i++) {
			coord[i + 1] = coord[i] + colW;
		}
		return coord;
	}

	/**
	 * Returns the y-coordinates of the horizontal grid lines
	 */
	public float[] getHorzYCoord() {
		float y0 = Float.NaN, colH = Float.NaN;
		int nrows = 0;
		try {
			y0 = Float.valueOf(y0TF.getText()).floatValue();
			colH = Float.valueOf(chTF.getText()).floatValue();
			nrows = Integer.valueOf(nRowTF.getText()).intValue();
		} catch (Exception ex) {
			return null;
		}
		if (Float.isNaN(y0) || Float.isNaN(colH) || nrows < 1)
			return null;
		if (geo) {
			float y1 = Float.NaN;
			try {
				y1 = Float.valueOf(y1TF.getText()).floatValue();
			} catch (Exception ex) {
			}
			colH = (y1 - y0) / nrows;
		}
		float coord[] = new float[nrows + 1];
		coord[0] = y0;
		for (int i = 0; i < nrows; i++) {
			coord[i + 1] = coord[i] + colH;
		}
		return coord;
	}
}
