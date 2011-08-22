package spade.analysis.plot;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Vector;

import spade.analysis.system.Supervisor;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.PopupManager;
import spade.lib.util.StringUtil;
import spade.time.TimeMoment;
import spade.vis.action.HighlightListener;
import spade.vis.action.Highlighter;
import spade.vis.action.ObjectEvent;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.AttributeTypes;
import spade.vis.database.ObjectFilter;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: Apr 7, 2010
 * Time: 6:15:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class Histogram2dCanvas extends Canvas implements MouseMotionListener, MouseListener, PropertyChangeListener, HighlightListener, Destroyable {

	/**
	 * The table with data, in particular, with values of the attribute to be
	 * represented by the graph.
	 */
	protected AttributeDataPortion table = null;

	/**
	 * ObjectFilter may be associated with the table and contain results of data
	 * querying. Only data satisfying the current query (if any) are displayed
	 */
	protected ObjectFilter of = null;

	/**
	 * IDs, column numbers, min/max values of the two attributes
	 */
	protected Vector attributes = null;
	protected int attrIdx[] = null;
	protected double vmin[] = null, vmax[] = null;
	protected TimeMoment vmint[] = null, vmaxt[] = null;
	protected boolean bAdjustMinMaxbyQuery = false, bBufferMinMax = true;

	public double getMin(int n) {
		return vmin[n];
	}

	public double getMax(int n) {
		return vmax[n];
	}

	/**
	 * The supervisor is used for propagating object events among system components
	 */
	protected Supervisor supervisor = null;

	/**
	 * statistics: nBins, breaks, counts for bin
	 */
	protected int nbins[] = null;
	protected float roundingTo[] = null;
	protected double breaks[][] = null;
	protected HashMap<String, Vector<Integer>> allItems = null, selectedItems = null;
	protected double maxCount = 0;
	protected String binLabels[][] = null;

	// Data structures for popups showing mouse-over details
	protected PopupManager popM = null;
	protected Rectangle hotspots[][] = null;

	// defined in Histogram2dPanel:
	// public static String renderingStyles[]={"bubbles","rectangles","vertical bars","horizontal bars"};
	protected int renderingStyle = 2;

	public void setRenderingStyle(int renderingStyle) {
		this.renderingStyle = renderingStyle;
		redraw();
	}

	protected boolean bSquareCells = false;

	public void setSquareCells(boolean bSquareCells) {
		this.bSquareCells = bSquareCells;
		redraw();
	}

	protected boolean bCumMode = false, bCumAlongX = true, bCumFromMin = true;

	public void setCumulativeMode(boolean bCumMode, boolean bCumAlongX, boolean bCumFromMin) {
		this.bCumMode = bCumMode;
		this.bCumAlongX = bCumAlongX;
		this.bCumFromMin = bCumFromMin;
		allItems = null;
		selectedItems = null;
		findCounts();
		redraw();
	}

	protected int aggrOper = 0, // 0=count, 1=max, 2=avg, 3=N distinct
			aggrAttrN = 0;

	public void setAggregation(int aggrOper, int aggrAttrN) {
		this.aggrOper = aggrOper;
		this.aggrAttrN = aggrAttrN;
		if (aggrOper == -1) {
			aggrOper = 0;
		}
		if (aggrAttrN == -1) {
			aggrAttrN = 0;
		}
		allItems = null;
		selectedItems = null;
		setup();
	}

	protected void addCellToList(Vector<Integer> vi, Vector<Integer> extra) {
		if (extra != null) {
			for (int i = 0; i < extra.size(); i++) {
				vi.addElement(extra.elementAt(i));
			}
		}
	}

	protected Vector<Integer> getListForCell(HashMap<String, Vector<Integer>> map, int bin_0, int bin_1) {
		Vector<Integer> vi = new Vector<Integer>(100, 100);
		if (bCumAlongX) {
			if (bCumFromMin) {
				for (int i = 0; i <= bin_0; i++) {
					addCellToList(vi, map.get(i + "-" + bin_1));
				}
			} else {
				for (int i = bin_0; i < nbins[0]; i++) {
					addCellToList(vi, map.get(i + "-" + bin_1));
				}
			}
		} else {
			if (bCumFromMin) {
				for (int j = 0; j <= bin_1; j++) {
					addCellToList(vi, map.get(bin_0 + "-" + j));
				}
			} else {
				for (int j = bin_1; j < nbins[1]; j++) {
					addCellToList(vi, map.get(bin_0 + "-" + j));
				}
			}
		}
		return vi;
	}

	public double getValue(HashMap<String, Vector<Integer>> map, int bin_0, int bin_1) {
		if (bCumMode) {
			Vector<Integer> vi = getListForCell(map, bin_0, bin_1);
			return getValue(vi);
		} else
			return getValue(map.get(bin_0 + "-" + bin_1));
	}

	public double getValue(Vector<Integer> vi) {
		if (vi == null)
			return Double.NaN;
		double d = Double.NaN;
		switch (aggrOper) {
		case 0: // count
			d = vi.size();
			break;
		case 1: // max
			for (int i = 0; i < vi.size(); i++) {
				double dd = table.getNumericAttrValue(aggrAttrN, vi.elementAt(i));
				if (i == 0) {
					d = dd;
				} else if (dd > d) {
					d = dd;
				}
			}
			break;
		case 2: // avg
			d = 0;
			for (int i = 0; i < vi.size(); i++) {
				d += table.getNumericAttrValue(aggrAttrN, vi.elementAt(i));
			}
			d /= vi.size();
			break;
		case 3: // N distinct
			Vector<String> vs = new Vector<String>(100, 100);
			for (int i = 0; i < vi.size(); i++) {
				String s = table.getAttrValueAsString(aggrAttrN, vi.elementAt(i));
				if (vs.indexOf(s) == -1) {
					vs.addElement(s);
				}
			}
			d = vs.size();
			break;
		}
		//System.out.println("* key="+key+", value="+d);
		return d;
	}

	public Histogram2dCanvas(AttributeDataPortion table, Vector attributes, Supervisor supervisor) {
		this.table = table;
		this.attributes = attributes;
		this.supervisor = supervisor;
		if (supervisor != null) {
			supervisor.addPropertyChangeListener(this);
			//supervisor.registerObjectEventSource(this);
			supervisor.registerHighlightListener(this, table.getEntitySetIdentifier());
			table.addPropertyChangeListener(this);
		}
		of = table.getObjectFilter();
		if (of != null) {
			of.addPropertyChangeListener(this);
		}
		attrIdx = table.getAttrIndices(attributes);
		nbins = new int[2];
		nbins[0] = nbins[1] = 3;
		breaks = new double[2][];
		binLabels = new String[2][];
		vmin = new double[2];
		vmax = new double[2];
		vmint = new TimeMoment[2];
		vmaxt = new TimeMoment[2];
		for (int i = 0; i <= 1; i++) {
			vmin[i] = vmax[i] = Float.NaN;
			vmint[i] = vmaxt[i] = null;
		}
		setup();
		addMouseListener(this);
		addMouseMotionListener(this);
	}

	public void setNBins(int attrN, int nbins) {
		roundingTo = null;
		for (int i = 0; i <= 1; i++)
			if (attrN == i && nbins != this.nbins[i]) {
				this.nbins[i] = nbins;
				allItems = null;
				selectedItems = null;
				setup();
			}
	}

	public void setRoundingTo(float factor1, float factor2) {
		roundingTo = new float[2];
		roundingTo[0] = factor1;
		roundingTo[1] = factor2;
		allItems = null;
		selectedItems = null;
		setup();
	}

	public void setBufferMinMax(boolean bufferMinMax) {
		if (bBufferMinMax != bufferMinMax) {
			bBufferMinMax = bufferMinMax;
			setup();
		}
	}

	public void setAdjustMinMaxbyQuery(boolean bAdjustMinMaxbyQuery) {
		this.bAdjustMinMaxbyQuery = bAdjustMinMaxbyQuery;
		allItems = null;
		selectedItems = null;
		setup();
	}

	protected void findMinMax() {
		for (int i = 0; i <= 1; i++) {
			vmin[i] = vmax[i] = Float.NaN;
			vmint[i] = vmaxt[i] = null;
			for (int j = 0; j < table.getDataItemCount(); j++)
				if (!bAdjustMinMaxbyQuery || of == null || of.isActive(j)) {
					if (Double.isNaN(vmin[i]) || table.getNumericAttrValue(attrIdx[i], j) < vmin[i]) {
						vmin[i] = table.getNumericAttrValue(attrIdx[i], j);
					}
					if (Double.isNaN(vmax[i]) || table.getNumericAttrValue(attrIdx[i], j) > vmax[i]) {
						vmax[i] = table.getNumericAttrValue(attrIdx[i], j);
					}
					Object val = table.getAttrValue(attrIdx[i], j);
					if (val instanceof TimeMoment) {
						TimeMoment tm = (TimeMoment) val;
						if (vmint[i] == null || vmint[i].compareTo(tm) > 0) {
							vmint[i] = tm;
						}
						if (vmaxt[i] == null || vmaxt[i].compareTo(tm) < 0) {
							vmaxt[i] = tm;
						}
					}
				}
			//System.out.println("* i="+i+", min="+vmin[i]+", max="+vmax[i]);
		}
	}

	protected int findBin(int attr, double value) {
		for (int i = 0; i < breaks[attr].length; i++)
			if (value <= breaks[attr][i])
				return i;
		return nbins[attr] - 1;
	}

	protected void findCounts() {
		if (allItems == null) {
			allItems = new HashMap<String, Vector<Integer>>(100, 100);
			selectedItems = new HashMap<String, Vector<Integer>>(100, 100);
			hotspots = new Rectangle[nbins[0]][];
			for (int i = 0; i < nbins[0]; i++) {
				hotspots[i] = new Rectangle[nbins[1]];
				for (int j = 0; j < nbins[1]; j++) {
					hotspots[i][j] = null;
				}
			}
		}
		Highlighter highlighter = supervisor.getHighlighter(table.getEntitySetIdentifier());
		Vector selected = (highlighter == null) ? null : highlighter.getSelectedObjects();
		for (int rec = 0; rec < table.getDataItemCount(); rec++)
			if (of == null || of.isActive(rec)) {
				int i = findBin(0, table.getNumericAttrValue(attrIdx[0], rec)), j = findBin(1, table.getNumericAttrValue(attrIdx[1], rec));
				if (i >= 0 && j >= 0) {
					String key = i + "-" + j;
					Vector<Integer> vi = allItems.get(key);
					if (vi == null) {
						vi = new Vector<Integer>(10, 10);
						allItems.put(key, vi);
					}
					vi.addElement(rec);
					if (selected != null && selected.contains(table.getDataItemId(rec))) {
						vi = selectedItems.get(key);
						if (vi == null) {
							vi = new Vector<Integer>(10, 10);
							selectedItems.put(key, vi);
						}
						vi.addElement(rec);
					}
				}
			}
		maxCount = 0;
		for (int i = 0; i < nbins[0]; i++) {
			for (int j = 0; j < nbins[1]; j++) {
				double v = getValue(allItems, i, j);
				if (v > maxCount) {
					maxCount = v;
				}
			}
		}
	}

	public void findCountsOfSelected(Vector selected) {
		selectedItems = new HashMap<String, Vector<Integer>>(100, 100);
		for (int rec = 0; rec < table.getDataItemCount(); rec++)
			if (of == null || of.isActive(rec)) {
				int i = findBin(0, table.getNumericAttrValue(attrIdx[0], rec)), j = findBin(1, table.getNumericAttrValue(attrIdx[1], rec));
				if (i >= 0 && j >= 0) {
					if (selected != null && selected.contains(table.getDataItemId(rec))) {
						String key = i + "-" + j;
						Vector<Integer> vi = selectedItems.get(key);
						if (vi == null) {
							vi = new Vector<Integer>(10, 10);
							selectedItems.put(key, vi);
						}
						vi.addElement(rec);
					}
				}
			}
	}

	protected void setupRoundedBins() {
		for (int i = 0; i <= 1; i++) {
			double min = Math.floor(vmin[i] / roundingTo[i]) * roundingTo[i];
			if (min + (vmax[i] - min) / roundingTo[i] <= vmin[i]) {
				min += (vmax[i] - min) / roundingTo[i];
			}
			nbins[i] = 1 + (int) Math.ceil((vmax[i] - min) / roundingTo[i]);
			min -= roundingTo[i] / 2;
			if (min + (nbins[i] - 1) * roundingTo[i] >= vmax[i]) {
				nbins[i]--;
			}
			breaks[i] = new double[nbins[i] - 1];
			binLabels[i] = new String[nbins[i]];
			for (int j = 0; j < breaks[i].length; j++) {
				breaks[i][j] = min + (j + 1) * roundingTo[i];
				binLabels[i][j] = "about " + StringUtil.doubleToStr(breaks[i][j] - roundingTo[i] / 2, vmin[i], vmax[i]) + " ("
						+ ((j == 0) ? "<= " + StringUtil.doubleToStr(breaks[i][j], vmin[i], vmax[i]) : "> " + StringUtil.doubleToStr(breaks[i][j - 1], vmin[i], vmax[i]) + " and " + "<= " + StringUtil.doubleToStr(breaks[i][j], vmin[i], vmax[i]))
						+ ")";
			}
			int j = nbins[i] - 1;
			binLabels[i][j] = (nbins[i] == 1) ? "all" : "about " + StringUtil.doubleToStr(breaks[i][j - 1] + roundingTo[i] / 2, vmin[i], vmax[i]) + " ( > " + StringUtil.doubleToStr(breaks[i][j - 1], vmin[i], vmax[i]) + " and " + "<= "
					+ StringUtil.doubleToStr(vmax[i], vmin[i], vmax[i]) + ")";
			//System.out.println("* i="+i+", min="+vmin[i]+", max="+vmax[i]+", nbins="+nbins[i]);
		}
	}

	protected void setupBins() {
		if (roundingTo != null) {
			setupRoundedBins();
			return;
		}
		for (int i = 0; i <= 1; i++) {
			breaks[i] = new double[nbins[i] - 1];
			binLabels[i] = new String[nbins[i]];
			double min = vmin[i], max = vmax[i];
			if (bBufferMinMax) {
				double d = (vmax[i] - vmin[i]) / Math.max(1, nbins[i] - 1);
				min -= d / 2;
				max += d / 2;
			}
			for (int j = 0; j < breaks[i].length; j++) {
				breaks[i][j] = min + (j + 1) * (max - min) / nbins[i];
				if (j == 0) {
					binLabels[i][j] = "<= " + StringUtil.doubleToStr(breaks[i][j], vmin[i], vmax[i]);
				} else {
					binLabels[i][j] = "> " + StringUtil.doubleToStr(breaks[i][j - 1], vmin[i], vmax[i]) + " and " + "<= " + StringUtil.doubleToStr(breaks[i][j], vmin[i], vmax[i]);
				}
			}
			binLabels[i][nbins[i] - 1] = (nbins[i] == 1) ? "all" : "> " + StringUtil.doubleToStr(breaks[i][nbins[i] - 2], vmin[i], vmax[i]);
		}
	}

	public void setup() {
		findMinMax();
		setupBins();
		findCounts();
		redraw();
	}

	public void setupAfterSelectionChanged(Vector selected) {
		findCountsOfSelected(selected);
		redraw();
	}

	@Override
	public void paint(Graphics g) {
		draw(g);
	}

	public void draw(Graphics g) {
		if (allItems == null)
			return;
		if (popM == null) {
			popM = new PopupManager(this, "", true);
			popM.setOnlyForActiveWindow(false);
		}
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, getBounds().width, getBounds().height);
		int minMarg = 3;

		int prec[] = { 0, 0 };
		for (int i = 0; i < 2; i++)
			if (table.getAttributeType(attrIdx[i]) != AttributeTypes.integer) {
				prec[i] = StringUtil.getPreferredPrecision((vmin[i] + vmax[i]) / 2, vmin[i], vmax[i]);
			}
		String strmin0 = numToString(vmin[0], prec[0], vmint[0]), strmin1 = numToString(vmin[1], prec[1], vmint[1]), strmax0 = numToString(vmax[0], prec[0], vmaxt[0]), strmax1 = numToString(vmax[1], prec[1], vmaxt[1]);
		FontMetrics fm = g.getFontMetrics();
		int asc = fm.getAscent(), lmin1 = fm.stringWidth(strmin1), lmax0 = fm.stringWidth(strmax0), lmax1 = fm.stringWidth(strmax1), leftTextFieldW = (lmin1 > lmax1) ? lmin1 : lmax1, mx1 = minMarg + leftTextFieldW, mx2 = minMarg, my1 = minMarg + asc, my2 = minMarg
				+ 2 * asc;
		int dxall = getBounds().width - (mx1 + mx2 + 1), dyall = getBounds().height - (my1 + my2 + 1), dx = dxall / nbins[0], dy = dyall / nbins[1];
		if (bSquareCells) {
			dx = dy = Math.min(dx, dy);
		}
		dxall = dx * nbins[0];
		dyall = dy * nbins[1];

		// draw frame
		g.setColor(Color.LIGHT_GRAY);
		for (int x = 0; x <= nbins[0]; x++) {
			g.drawLine(mx1 + x * dx, my1, mx1 + x * dx, my1 + dyall);
		}
		for (int y = 0; y <= nbins[1]; y++) {
			g.drawLine(mx1, my1 + y * dy, mx1 + dxall, my1 + y * dy);
		}
		//g.drawRect(mx1,my1,dxall,dyall);
		g.setColor(Color.DARK_GRAY);
		g.drawString("Y:" + table.getAttributeName(attrIdx[1]), minMarg, asc);
		String str = "X:" + table.getAttributeName(attrIdx[0]);
		g.drawString(str, mx1 + dxall - fm.stringWidth(str), my1 + dyall + 2 * asc);
		str = "Maximal ";
		switch (renderingStyle) {
		case 0:
		case 1:
			str += "area";
			break;
		case 2:
			str += "height";
			break;
		case 3:
			str += "width";
			break;
		}
		str += ": ";
		if (aggrOper == 0 || aggrOper == 3) {
			str += (int) maxCount;
		} else {
			str += maxCount;
		}
		g.drawString(str, minMarg + 5, my1 + dyall + 2 * asc);

		// draw legend
		g.setColor(Color.blue.darker());
		g.drawString(strmax1, mx1 - lmax1 - 2, my1 + asc);
		g.drawString(strmin1, mx1 - lmin1 - 2, my1 + dyall);
		g.drawString(strmin0, mx1, my1 + dyall + asc);
		g.drawString(strmax0, mx1 + dxall - lmax0, my1 + dyall + asc);

		// draw cells
		for (int x = 0; x < nbins[0]; x++) {
			for (int y = 0; y < nbins[1]; y++) {
				if (getValue(allItems, x, y) > 0) {
					Rectangle r = new Rectangle(mx1 + x * dx, my1 + (nbins[1] - y - 1) * dy, dx, dy);
					hotspots[x][y] = r;
					// background
					g.setColor(Color.GRAY);
					g.drawRect(r.x, r.y, r.width, r.height);
					// count
					double ratio = 1d * getValue(allItems, x, y) / maxCount;
					if (renderingStyle < 2) {
						ratio = Math.sqrt(ratio);
					}
					drawSingleCell(g, r, ratio);
					// count of selected
					double v = getValue(selectedItems, x, y);
					if (v > 0) {
						ratio = 1d * v / maxCount;
						if (renderingStyle < 2) {
							ratio = Math.sqrt(ratio);
						}
						g.setColor(Color.BLACK);
						drawSingleCell(g, r, ratio);
					}
				}
			}
		}

		// ready!
	}

	public void drawSingleCell(Graphics g, Rectangle r, double ratio) {
		int dx = r.width, dy = r.height;
		int w = (int) ((dx - 1) * ratio), h = (int) ((dy - 1) * ratio);
		if (w <= 1) {
			w = 2;
		}
		if (h <= 1) {
			h = 2;
		}
		int dw = (dx - 1 - w) / 2, dh = (dy - 1 - h) / 2;
		if (renderingStyle < 2) {
			if (dx - 1 > w + 2 * dw) {
				w++;
			}
			if (dy - 1 > h + 2 * dh) {
				h++;
			}
		}
		switch (renderingStyle) {
		case 0:
			g.fillOval(r.x + dw, r.y + dh, w + 1, h + 1);
			break;
		case 1:
			g.fillRect(r.x + dw + 1, r.y + dh + 1, w, h);
			break;
		case 2:
			dh = dy - 1 - h;
			g.fillRect(r.x + 1, r.y + dh + 1, dx, h);
			break;
		case 3:
			g.fillRect(r.x + 1, r.y + 1, w, dy);
			break;
		}
	}

	public void redraw() {
		Graphics g = getGraphics();
		if (g != null) {
			draw(g);
			g.dispose();
		}
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(500, 400);
	}

	public String numToString(double num, int precision, TimeMoment time) {
		if (time == null)
			return StringUtil.doubleToStr(num, precision);
		return time.valueOf((long) num).toString();
	}

//----------------------- HighlightListener interface --------------------------
	/**
	 * Called when the set of transiently highlighted objects changes. The argument
	 * "source" is usually a reference to a Highlighter. The argument "setId" is
	 * the identifier of the set the highlighted objects belong to. The argument
	 * "highlighted" is a vector of identifiers of currently highlighted objects.
	 * For marking highlighted objects on a map or graph, white color is used.
	 */
	@Override
	public void highlightSetChanged(Object source, String setId, Vector highlighted) {
	}

	/**
	 * Called when the set of selected (durably highlighted) objects changes.
	 * The argument "source" is usually a reference to a Highlighter. The argument
	 * "setId" is the identifier of the set the highlighted objects belong to. The
	 * argument "highlighted" is a vector of identifiers of currently highlighted
	 * objects. For marking selected objects on a map or graph, black color is used.
	 */
	@Override
	public void selectSetChanged(Object source, String setId, Vector selected) {
		if (!setId.equalsIgnoreCase(table.getEntitySetIdentifier()))
			return; //not relevant
		this.setupAfterSelectionChanged(selected);
		//System.out.println("* select set changed");
	}

//--------------------- PropertyChangeListener interface ------------------------
	/**
	 * Called when one of the following changes occur:
	 * 1) object classification or colors;
	 * 2) filter conditions;
	 * 3) data in the table: new attributes, values of existing attributes, new
	 *    table rows (objects);
	 * 4) the table or filter are destroyed, i.e. removed from the system
	 */
	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		//ToDo: update if classes changed
		if (pce.getPropertyName().equals(Supervisor.eventObjectColors)) {
			if (table.getEntitySetIdentifier().equals(pce.getNewValue())) {
				//do some actions on the change of the classes or class colors
				//System.out.println("* classes have changed");
			}
			return;
		}

		if (pce.getSource().equals(of)) { //change of the filter
			if (pce.getPropertyName().equals("destroyed")) {
				of.removePropertyChangeListener(this);
				of = null;
			} else {
				//do some actions on the change of the filter
				//System.out.println("* filter changed");
				allItems = null;
				selectedItems = null;
				setup();
			}
			return;
		}

		if (pce.getSource().equals(table)) {
			if (pce.getPropertyName().equals("destroyed") || pce.getPropertyName().equals("structure_complete")) {
				destroy();
			} else if (pce.getPropertyName().equals("filter")) {
				//a (new) filter is attached to the table or removed
				if (of != null) {
					of.removePropertyChangeListener(this);
				}
				of = table.getObjectFilter();
				if (of != null) {
					of.addPropertyChangeListener(this);
				}
				allItems = null;
				selectedItems = null;
				setup();
			} else if (pce.getPropertyName().equals("values")) { //values of some attribute(s) changed
				Vector v = (Vector) pce.getNewValue(); // list of changed attributes
				if (v != null && (v.contains(attributes.elementAt(0)) || v.contains(attributes.elementAt(1)))) {
					//Values of the attribute represented on this graph have changed.
					//Do some actions on the change of the values:
					//System.out.println("* values changed");
					allItems = null;
					selectedItems = null;
					setup();
				}
			} else if (pce.getPropertyName().equals("data_added") || pce.getPropertyName().equals("data_removed") || pce.getPropertyName().equals("data_updated")) {
				//Some rows in the table have been added/removed/changed
				//Do some actions on the change of the data:
				//System.out.println("* values changed");
				allItems = null;
				selectedItems = null;
				setup();
			}
		}
		return;
	}

//--------------------- MouseMotionListener interface ------------------------
	@Override
	public void mouseMoved(MouseEvent e) {
		if (popM == null)
			return;
		int x = -1, y = -1;
		for (int i = 0; i < hotspots.length; i++) {
			for (int j = 0; j < hotspots[i].length; j++)
				if (hotspots[i][j] != null && hotspots[i][j].contains(e.getX(), e.getY())) {
					x = i;
					y = j;
					break;
				}
		}
		if (x >= 0) {
			String key = x + "-" + y;
			String str = "x=" + x + ", y=" + y;
			str = table.getAttributeName(attrIdx[0]) + ":\n";
			str += binLabels[0][x] + "\n";
			str += table.getAttributeName(attrIdx[1]) + ":\n";
			str += binLabels[1][y] + "\n" + "-------------\n";
			double count = getValue(allItems, x, y);
			if (bCumMode) {
				double countCell = getValue(allItems.get(key));
				if (!Double.isNaN(countCell)) {
					str += "Value: " + StringUtil.doubleToStr(countCell, (aggrOper == 0 || aggrOper == 3) ? 0 : 2) + "\n";
				}
				str += "Cumulative: ";
			}
			str += ((count == 0) ? "" : StringUtil.doubleToStr(count, (aggrOper == 0 || aggrOper == 3) ? 0 : 2) + " (" + StringUtil.doubleToStr(100 * count / maxCount, 2) + "% of "
					+ StringUtil.doubleToStr(maxCount, (aggrOper == 0 || aggrOper == 3) ? 0 : 2) + ")");
			double countsel = (selectedItems.get(key) == null) ? 0 : getValue(selectedItems, x, y);
			if (countsel > 0) {
				str += "\n-------------\n" + StringUtil.doubleToStr(countsel, (aggrOper == 0 || aggrOper == 3) ? 0 : 2) + " of them are selected";
			}
			popM.setText(str);
			popM.setKeepHidden(false);
			popM.startShow(e.getX(), e.getY());
		} else {
			String str = "";
			popM.setText(str);
		}
	}

	@Override
	public void mouseDragged(MouseEvent e) {
	}

//--------------------- MouseListener interface ------------------------

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		int x = -1, y = -1;
		for (int i = 0; i < hotspots.length; i++) {
			for (int j = 0; j < hotspots[i].length; j++)
				if (hotspots[i][j] != null && hotspots[i][j].contains(e.getX(), e.getY())) {
					x = i;
					y = j;
					break;
				}
		}
		ObjectEvent oevt = new ObjectEvent(this, (e.getClickCount() > 1) ? ObjectEvent.dblClick : ObjectEvent.click, e, table.getEntitySetIdentifier());
		if (x >= 0) {
			Vector<Integer> vi = (bCumMode) ? getListForCell(allItems, x, y) : allItems.get(x + "-" + y);
			if (vi != null) {
				for (int i = 0; i < vi.size(); i++) {
					oevt.addEventAffectedObject(table.getDataItemId(vi.elementAt(i)));
/*
     for (int rec=0; rec<table.getDataItemCount(); rec++)
			if (of==null || of.isActive(rec)) {
			  int i=findBin(0,table.getNumericAttrValue(attrIdx[0],rec)),
			      j=findBin(1,table.getNumericAttrValue(attrIdx[1],rec));
			  if (i==x && j==y)
			    oevt.addEventAffectedObject(table.getDataItemId(rec));
			}
*/
				}
			}
		}
		supervisor.processObjectEvent(oevt);
	}

//--------------------- Destroyable interface ------------------------
	protected boolean destroyed = false;

	/**
	* Notifies the listeners that this tool is destroyed, i.e.
	* sends a PropertyChangeEvent with the name "destroyed" to its
	* destroying listener(s), @see addDestroyingListener.
	*/
	@Override
	public void destroy() {
		destroyed = true;
		supervisor.removeHighlightListener(this, table.getEntitySetIdentifier());
		supervisor.removePropertyChangeListener(this);
		table.removePropertyChangeListener(this);
		if (of != null) {
			of.removePropertyChangeListener(this);
		}
	}

	/**
	* Replies whether is destroyed or not
	*/
	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

}
