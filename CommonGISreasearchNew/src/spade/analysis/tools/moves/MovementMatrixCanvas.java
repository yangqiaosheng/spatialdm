package spade.analysis.tools.moves;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.ScrollPane;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Vector;

import spade.analysis.system.Supervisor;
import spade.lib.basicwin.PopupManager;
import spade.lib.util.BubbleSort;
import spade.lib.util.Comparator;
import spade.lib.util.Matrix;
import spade.lib.util.StringUtil;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 22-Feb-2007
 * Time: 16:03:39
 * A component that draws the movement matrix. A movement matrix
 * represents a set of movements in an aggregated way in a matrix with the
 * rows and columns corresponding to the source and destination locations,
 * respectively, and symbols in the cells represent various aggregated
 * values: N of moves, total amount of items moved, average time, etc.
 */
public class MovementMatrixCanvas extends Canvas implements MouseListener, MouseMotionListener, Comparator {
	class SingleCellInfo {
		public Rectangle r = null; // bar outline
		public String val;
		public int idxSrc, idxDest;
	}

	class ComparisonItemInt {
		public int idx;
		public int value;
	}

	class ComparisonItemFloat {
		public int idx;
		public float value;
	}

	class ComparisonItemString {
		public int idx;
		public String value;
	}

	protected static Color normalColor = new Color(96, 96, 96), filteredColor = new Color(168, 168, 168), activeColor = Color.white, selectedColor = Color.black, bkgColor = new Color(192, 192, 192);

	protected AggregatedMovesInformer ma = null;

	public void setAggregatedMovesInformer(AggregatedMovesInformer ma) {
		this.ma = ma;
	}

	int na = 0; // number of attribute to display: 0..nAttr-1

	public void setNa(int na) {
		this.na = na;
		//sort(); // after setNa method setFocuserValue is always called, it sorts
	}

	protected void sort() {
		if (bSort) {
			switch (iSortMode) {
			case 0:
				sortAlphabetically();
				break;
			case 1:
				sortBySources();
				sortByDestinations();
				break;
			case 2:
			case 3:
				System.out.println("* click on source or destination to sort");
				boolean found = false;
				if (selSrcID != null) {
					for (int iy = 0; iy < getNofSrcOrdered() && !found; iy++)
						if (selSrcID.equals(getSrcIdOrdered(iy))) {
							found = true;
							if (iSortMode == 2) {
								sortByValuesInRow(iy);
							} else {
								sortByDistancesInRow(iy);
							}
						}
				}
				found = false;
				if (selDestID != null) {
					for (int ix = 0; ix < getNofDestOrdered() && !found; ix++)
						if (selDestID.equals(getDestIdOrdered(ix))) {
							found = true;
							if (iSortMode == 2) {
								sortByValuesInColumn(ix);
							} else {
								sortByDistancesInColumn(ix);
							}
						}
				}
				break;
			default:
				srcOrder = null;
				destOrder = null;
				break;
			}
		} else {
			srcOrder = null;
			destOrder = null;
		}
		redraw();
	}

	// orders of sources and destinations according to a selected principle
	// may be shorter that full lists if some sources or destinations are skipped,
	// for example, due to zero numbers of transported objets (empty rides)
	protected int srcOrder[] = null, destOrder[] = null;
	protected boolean bSort = false, bAscSort = false, bRemoveZero = false;
	protected int iSortMode = 0;

	public void setSortMode(boolean bSort, boolean bAscSort, boolean bRemoveZero, int iSortMode) {
		this.bSort = bSort;
		this.bAscSort = bAscSort;
		this.bRemoveZero = bRemoveZero;
		this.iSortMode = iSortMode;
		sort();
	}

	/**
	 * Returns 0 if the objects are equal, <0 if the first object is less than the
	 * second one, >0 otherwise
	 */
	@Override
	public int compare(Object obj1, Object obj2) {
		if (obj1 instanceof ComparisonItemInt) {
			ComparisonItemInt ci1 = (ComparisonItemInt) obj1, ci2 = (ComparisonItemInt) obj2;
			if (ci1.value == ci2.value)
				return 0;
			else
				return (ci1.value < ci2.value) ? -1 : 1;
		} else if (obj1 instanceof ComparisonItemFloat) {
			ComparisonItemFloat ci1 = (ComparisonItemFloat) obj1, ci2 = (ComparisonItemFloat) obj2;
			if (ci1.value == ci2.value)
				return 0;
			else
				return (ci1.value < ci2.value) ? -1 : 1;
		} else {
			ComparisonItemString ci1 = (ComparisonItemString) obj1, ci2 = (ComparisonItemString) obj2;
			return ci1.value.compareTo(ci2.value);
		}
	}

	public void sortByValuesInRow(int iy) {
		System.out.println("* sort by values from " + iy + " " + getSrcNameOrdered(iy));
		destOrder = null;
		Vector v = new Vector(getNofDestOrdered());
		for (int nd = 0; nd < getNofDestOrdered(); nd++) {
			Integer i = (Integer) getMatrixValueOrdered(na, iy, nd);
			int value = (i == null) ? -1 : i.intValue();
			ComparisonItemInt ci = new ComparisonItemInt();
			ci.idx = nd;
			ci.value = (bAscSort) ? value : -value;
			v.add(ci);
		}
		if (v.size() == 0)
			return;
		BubbleSort.sort(v, this);
		destOrder = new int[v.size()];
		for (int nd = 0; nd < destOrder.length; nd++) {
			destOrder[nd] = ((ComparisonItemInt) v.elementAt(nd)).idx;
		}
		redraw();
	}

	public void sortByValuesInColumn(int ix) {
		System.out.println("* sort by values to " + ix + " " + getDestNameOrdered(ix));
		srcOrder = null;
		Vector v = new Vector(getNofSrcOrdered());
		for (int ns = 0; ns < getNofSrcOrdered(); ns++) {
			Integer i = (Integer) getMatrixValueOrdered(na, ns, ix);
			int value = (i == null) ? -1 : i.intValue();
			ComparisonItemInt ci = new ComparisonItemInt();
			ci.idx = ns;
			ci.value = (bAscSort) ? value : -value;
			v.add(ci);
		}
		if (v.size() == 0)
			return;
		BubbleSort.sort(v, this);
		srcOrder = new int[v.size()];
		for (int ns = 0; ns < srcOrder.length; ns++) {
			srcOrder[ns] = ((ComparisonItemInt) v.elementAt(ns)).idx;
		}
		redraw();
	}

	public void sortByDistancesInRow(int iy) {
		System.out.println("* sort by distances from " + iy + " " + getSrcNameOrdered(iy));
		destOrder = null;
		Vector v = new Vector(getNofDestOrdered());
		for (int nd = 0; nd < getNofDestOrdered(); nd++) {
			Float fl = (Float) distanceMatrix.get(0, getSrcIdOrdered(iy), getDestIdOrdered(nd));
			float value = (fl == null) ? -1 : fl.floatValue();
			ComparisonItemFloat ci = new ComparisonItemFloat();
			ci.idx = nd;
			ci.value = (bAscSort) ? value : -value;
			v.add(ci);
		}
		if (v.size() == 0)
			return;
		BubbleSort.sort(v, this);
		destOrder = new int[v.size()];
		for (int nd = 0; nd < destOrder.length; nd++) {
			destOrder[nd] = ((ComparisonItemFloat) v.elementAt(nd)).idx;
		}
		redraw();
	}

	public void sortByDistancesInColumn(int ix) {
		System.out.println("* sort by distances to " + ix + " " + getDestNameOrdered(ix));
		srcOrder = null;
		Vector v = new Vector(getNofSrcOrdered());
		for (int ns = 0; ns < getNofSrcOrdered(); ns++) {
			Float fl = (Float) distanceMatrix.get(0, getSrcIdOrdered(ns), getDestIdOrdered(ix));
			float value = (fl == null) ? -1 : fl.floatValue();
			ComparisonItemFloat ci = new ComparisonItemFloat();
			ci.idx = ns;
			ci.value = (bAscSort) ? value : -value;
			v.add(ci);
		}
		if (v.size() == 0)
			return;
		BubbleSort.sort(v, this);
		srcOrder = new int[v.size()];
		for (int ns = 0; ns < srcOrder.length; ns++) {
			srcOrder[ns] = ((ComparisonItemFloat) v.elementAt(ns)).idx;
		}
		redraw();
	}

	public void sortBySources() {
		srcOrder = null;
		Vector v = new Vector(getNofSrcOrdered());
		for (int ns = 0; ns < getNofSrcOrdered(); ns++) {
			Integer i = (Integer) getTotalSrcValueOrdered(na, ns);
			int value = (i == null) ? -1 : i.intValue();
			if (value > 0 || !bRemoveZero) {
				ComparisonItemInt ci = new ComparisonItemInt();
				ci.idx = ns;
				ci.value = (bAscSort) ? value : -value;
				v.add(ci);
			}
		}
		if (v.size() == 0)
			return;
		BubbleSort.sort(v, this);
		srcOrder = new int[v.size()];
		for (int ns = 0; ns < srcOrder.length; ns++) {
			srcOrder[ns] = ((ComparisonItemInt) v.elementAt(ns)).idx;
		}
	}

	public void sortByDestinations() {
		destOrder = null;
		Vector v = new Vector(getNofDestOrdered());
		for (int nd = 0; nd < getNofDestOrdered(); nd++) {
			Integer i = (Integer) getTotalDestValueOrdered(na, nd);
			int value = (i == null) ? -1 : i.intValue();
			if (value > 0 || !bRemoveZero) {
				ComparisonItemInt ci = new ComparisonItemInt();
				ci.idx = nd;
				ci.value = (bAscSort) ? value : -value;
				v.add(ci);
			}
		}
		if (v.size() == 0)
			return;
		BubbleSort.sort(v, this);
		destOrder = new int[v.size()];
		for (int nd = 0; nd < destOrder.length; nd++) {
			destOrder[nd] = ((ComparisonItemInt) v.elementAt(nd)).idx;
		}
	}

	public void sortAlphabetically() {
		srcOrder = null;
		Vector v = new Vector(getNofSrcOrdered());
		for (int ns = 0; ns < getNofSrcOrdered(); ns++) {
			Integer i = (Integer) getTotalSrcValueOrdered(na, ns);
			int value = (i == null) ? -1 : i.intValue();
			if (value > 0 || !bRemoveZero) {
				ComparisonItemString ci = new ComparisonItemString();
				ci.idx = ns;
				ci.value = this.getSrcNameOrdered(ns);
				v.add(ci);
			}
		}
		if (v.size() == 0)
			return;
		BubbleSort.sort(v, this);
		srcOrder = new int[v.size()];
		for (int ns = 0; ns < srcOrder.length; ns++) {
			srcOrder[ns] = ((ComparisonItemString) v.elementAt(ns)).idx;
		}
		destOrder = null;
		v = new Vector(getNofDestOrdered());
		for (int nd = 0; nd < getNofDestOrdered(); nd++) {
			Integer i = (Integer) getTotalDestValueOrdered(na, nd);
			int value = (i == null) ? -1 : i.intValue();
			if (value > 0 || !bRemoveZero) {
				ComparisonItemString ci = new ComparisonItemString();
				ci.idx = nd;
				ci.value = this.getDestNameOrdered(nd);
				v.add(ci);
			}
		}
		if (v.size() == 0)
			return;
		BubbleSort.sort(v, this);
		destOrder = new int[v.size()];
		for (int nd = 0; nd < destOrder.length; nd++) {
			destOrder[nd] = ((ComparisonItemString) v.elementAt(nd)).idx;
		}

	}

	protected int getNofSrcOrdered() {
		if (srcOrder == null)
			return ma.getNofSources();
		else
			return srcOrder.length;
	}

	protected int getNofDestOrdered() {
		if (destOrder == null)
			return ma.getNofDestinations();
		else
			return destOrder.length;
	}

	protected String[] getSrcNamesOrdered() {
		String names[] = new String[getNofSrcOrdered()];
		for (int i = 0; i < names.length; i++) {
			names[i] = getSrcNameOrdered(i);
		}
		return names;
	}

	protected String getSrcNameOrdered(int ns) {
		if (srcOrder == null)
			return ma.getSrcName(ns);
		else
			return ma.getSrcName(srcOrder[ns]);
	}

	protected String getSrcIdOrdered(int ns) {
		if (srcOrder == null)
			return ma.getSrcId(ns);
		else
			return ma.getSrcId(srcOrder[ns]);
	}

	protected String[] getDestNamesOrdered() {
		String names[] = new String[getNofDestOrdered()];
		for (int i = 0; i < names.length; i++) {
			names[i] = getDestNameOrdered(i);
		}
		return names;
	}

	protected String getDestNameOrdered(int nd) {
		if (destOrder == null)
			return ma.getDestName(nd);
		else
			return ma.getDestName(destOrder[nd]);
	}

	protected String getDestIdOrdered(int nd) {
		if (destOrder == null)
			return ma.getDestId(nd);
		else
			return ma.getDestId(destOrder[nd]);
	}

	protected Object getMatrixValueOrdered(int na, int ns, int nd) {
		int idxs = (srcOrder == null) ? ns : srcOrder[ns], idxd = (destOrder == null) ? nd : destOrder[nd];
		return ma.getMatrixValue(na, idxs, idxd);
	}

	protected Object getTotalSrcValueOrdered(int na, int ns) {
		int idxs = (srcOrder == null) ? ns : srcOrder[ns];
		return ma.getTotalSrcValue(na, idxs);
	}

	protected Object getTotalDestValueOrdered(int na, int nd) {
		int idxd = (destOrder == null) ? nd : destOrder[nd];
		return ma.getTotalDestValue(na, idxd);
	}

	/**
	* The unique identifier of this plot. The identifier is used
	* 1) for explicit linking of producers and recipients of object events;
	* 2) for correct restoring of system states with multiple plots.
	*/
	protected String plotId = null;
	/**
	* Used to generate unique identifiers of instances of TimeLineCanvas
	*/
	protected static int instanceN = 0;
	/**
	 * The width and height of the plot area
	 */
	protected int plotW = 0, plotH = 0;

	protected int drawMode = 1;

	public int getDrawMode() {
		return drawMode;
	}

	public void setDrawMode(int drawMode) {
		if (this.drawMode == drawMode)
			return;
		this.drawMode = drawMode;
		redraw();
	}

	protected Matrix distanceMatrix = null;

	protected float focuserValue = Float.NaN;

	public void setFocuserValue(float focuserValue) {
		this.focuserValue = focuserValue;
		sort();
	}

	// Data structures for popups showing mouse-over details
	protected PopupManager popM = null;
	protected Vector hotspots = null;

	// Data structures for propagating selection
	Supervisor supervisor = null;
	String locationsSetId = null, aggrOrdersSetId = null;

	/**
	* Used for handling the list of time selection listeners and notifying them
	* about selections of sources and/or destinations.
	*/
	protected PropertyChangeSupport pcSupport = null;

	/**
	* Registers a listener of selections of sources and/or destinations.
	*/
	public synchronized void addSiteSelectionListener(PropertyChangeListener l) {
		if (l == null)
			return;
		if (pcSupport == null) {
			pcSupport = new PropertyChangeSupport(this);
		}
		pcSupport.addPropertyChangeListener(l);
	}

	/**
	* Removes the selection listener
	*/
	public synchronized void removeSiteSelectionListener(PropertyChangeListener l) {
		if (l == null || pcSupport == null)
			return;
		pcSupport.removePropertyChangeListener(l);
	}

	/**
	* Used for notifying the selection listeners about a selection of a source and/or destination.
	*/
	protected void notifySiteSelection(String souId, String destId) {
		if (pcSupport == null)
			return;
		pcSupport.firePropertyChange("site_selection", souId, destId);
	}

	public MovementMatrixCanvas(Supervisor supervisor, String locationsSetId, String aggrOrdersSetId, Matrix distanceMatrix) {
		super();
		this.supervisor = supervisor;
		this.locationsSetId = locationsSetId;
		this.aggrOrdersSetId = aggrOrdersSetId;
		this.distanceMatrix = distanceMatrix;
		++instanceN;
		plotId = "movement_matrix_canvas_" + instanceN;
		setBackground(bkgColor);
		addMouseMotionListener(this);
		addMouseListener(this);
		if (popM == null) {
			popM = new PopupManager(this, "", true);
			popM.setOnlyForActiveWindow(false);
		}
	}

	/**
	 * Initial setup; also called when the data change
	 */
	public void setup() {
	}

	@Override
	public Dimension getPreferredSize() {
		if (plotW == 0 || plotH == 0)
			return new Dimension(300, 600);
		return new Dimension(plotW, plotH);
	}

	@Override
	public void paint(Graphics g) {
		draw(g);
	}

	private int wPrev = -1, hPrev = -1, x[] = null, y[] = null;

	public void draw(Graphics g) {
		Dimension size = null;
		if (getParent() != null && (getParent() instanceof ScrollPane)) {
			size = ((ScrollPane) getParent()).getViewportSize();
			if (size != null && size.width != plotW && size.height != plotH) {
				plotW = size.width;
				plotH = size.height;
				setSize(plotW, plotH);
				invalidate();
				getParent().invalidate();
				getParent().validate();
			}
		}
		if (size == null) {
			size = getSize();
		}
		int w = size.width, h = size.height;
		if (w != plotW || h != plotH) {
			//recount sizes...
			plotW = w;
			plotH = h;
		}
		g.setColor(bkgColor);
		g.fillRect(0, 0, w + 1, h + 1);
		w--;
		h--;
		hotspots = new Vector(100, 100);
		int nCellsforTexts = 4;
		int nsrc = ma.getNofSources(), ndest = ma.getNofDestinations();
		int dx = w / (ndest + 1 + nCellsforTexts), dy = h / (nsrc + 1 + nCellsforTexts);
		/*
		         +----+---+---+--------------+ y[0]
		    texts+    +   +   +              +
		  tests  +    +   +   +              +
		  +------+----+---+---+--------------+ y[1]
		  |      |    |   |   |              |
		  ...
		  +------+----+---+---+--------------+ y[ndest+1]
		  x[0]  x[1]                     x[nsrc+1]
		*/
		if (w != wPrev || h != hPrev) {
			x = new int[ndest + 2];
			y = new int[nsrc + 2];
			for (int ix = ndest + 1; ix >= 0; ix--)
				if (ix == ndest + 1) {
					x[ix] = w;
				} else if (ix == 0) {
					x[ix] = 1;
				} else {
					x[ix] = w - (ndest + 1 - ix) * dx;
				}
			for (int iy = nsrc + 1; iy >= 0; iy--)
				if (iy == nsrc + 1) {
					y[iy] = h;
				} else if (iy == 0) {
					y[iy] = 1;
				} else {
					y[iy] = h - (nsrc + 1 - iy) * dy;
				}
			wPrev = w;
			hPrev = h;
		}
		nsrc = getNofSrcOrdered();
		ndest = getNofDestOrdered();
		// draw horizontal and vertical lines of the table
		g.setColor(Color.BLACK);
		for (int ix = 0; ix < 2 + ndest; ix++) {
			g.drawLine(x[ix], (ix < 1) ? y[1] : y[0], x[ix], y[1 + nsrc]);
		}
		for (int iy = 0; iy < 2 + nsrc; iy++) {
			g.drawLine((iy < 1) ? x[1] : x[0], y[iy], x[1 + ndest], y[iy]);
		}
		// highlight selected rows and columns
		boolean found = false;
		if (selSrcID != null) {
			for (int iy = 0; iy < nsrc && !found; iy++)
				if (selSrcID.equals(getSrcIdOrdered(iy))) {
					g.setColor(Color.red);
					g.drawRect(x[0], y[1 + iy], x[1 + ndest] - x[0], dy);
					found = true;
				}
		}
		found = false;
		if (selDestID != null) {
			for (int ix = 0; ix < ndest && !found; ix++)
				if (selDestID.equals(getDestIdOrdered(ix))) {
					g.setColor(Color.red);
					g.drawRect(x[1 + ix], y[0], dx, y[1 + nsrc] - y[0]);
					found = true;
				}
		}
		// draw names of sources and corresponding table lens
		int max = ma.getMaxIntSrcValue(na);
		for (int iy = 0; iy < nsrc; iy++) { // names of sources
			String srcName = getSrcNameOrdered(iy);
			SingleCellInfo sci = new SingleCellInfo();
			sci.r = new Rectangle(x[0], y[1 + iy], x[1] - x[0], dy);
			Object o = getTotalSrcValueOrdered(na, iy);
			int i = (o != null && o instanceof Integer) ? ((Integer) o).intValue() : -1;
			sci.idxSrc = iy;
			sci.idxDest = -1;
			sci.val = "From \"" + srcName + "\"\n" + "Total value=" + i + "\n" + "(max value=" + max + ")";
			hotspots.addElement(sci);
			drawValueInCell(g, false, i, max, 0f, 0f, 3, x[0], y[1 + iy], x[1] - x[0], dy);
			g.setColor(Color.WHITE);
			g.drawString(srcName, 1 + x[0] + 2, 1 + y[1 + iy] + dy - 2);
			g.setColor(Color.BLACK);
			g.drawString(srcName, x[0] + 2, y[1 + iy] + dy - 2);
		}
		// draw names of destinations and corresponding table lens
		max = ma.getMaxIntDestValue(na);
		for (int ix = 0; ix < ndest; ix++) { // names of destinations
			String destName = getDestNameOrdered(ix);
			SingleCellInfo sci = new SingleCellInfo();
			sci.r = new Rectangle(x[1 + ix], y[0], dx, y[1] - y[0]);
			Object o = getTotalDestValueOrdered(na, ix);
			int i = (o != null && o instanceof Integer) ? ((Integer) o).intValue() : -1;
			sci.idxSrc = -1;
			sci.idxDest = ix;
			sci.val = "To \"" + getDestNameOrdered(ix) + "\"\n" + "Total value=" + i + "\n" + "(max value=" + max + ")";
			hotspots.addElement(sci);
			drawValueInCell(g, false, i, max, 0f, 0f, 2, x[1 + ix], y[0], dx, y[1] - y[0]);
			g.setColor(Color.WHITE);
			drawVerticalString(g, destName, 1 + x[1 + ix] + 2, 1 + y[0] + 2, dx, y[1] - y[0]);
			g.setColor(Color.BLACK);
			drawVerticalString(g, destName, x[1 + ix] + 2, y[0] + 2, dx, y[1] - y[0]);
		}
		// compute max values in the matrix
		max = ma.getMaxIntMatrixValue(na);
		float max2 = 0f;
		//(drawMode==4)?((Float)distanceMatrix.getMaximum()).floatValue():0f;
		if (drawMode == 4 && distanceMatrix != null) {
			for (int iy = 0; iy < nsrc; iy++) {
				for (int ix = 0; ix < ndest; ix++) { // body of the table
					Float fl = (Float) distanceMatrix.get(0, getSrcIdOrdered(iy), getDestIdOrdered(ix));
					if (fl != null) {
						Object o = getMatrixValueOrdered(na, iy, ix);
						int i = (o != null && o instanceof Integer) ? ((Integer) o).intValue() : -1;
						float v2 = fl.floatValue();
						if (i > 0 && v2 > max2) {
							max2 = v2;
						}
					}
				}
			}
		}
		if (Float.isNaN(focuserValue)) {
			focuserValue = max;
		}
		// fill the matrix
		for (int iy = 0; iy < nsrc; iy++) {
			for (int ix = 0; ix < ndest; ix++) { // body of the table
				SingleCellInfo sci = new SingleCellInfo();
				sci.r = new Rectangle(x[1 + ix], y[1 + iy], dx, dy);
				Object o = getMatrixValueOrdered(na, iy, ix);
				int i = (o != null && o instanceof Integer) ? ((Integer) o).intValue() : -1;
				sci.idxSrc = iy;
				sci.idxDest = ix;
				sci.val = "from \"" + getSrcNameOrdered(iy) + "\"\n" + "to \"" + getDestNameOrdered(ix) + "\"\n" + "value=" + i + "\n" + "(max value=" + max + ")";
				hotspots.addElement(sci);
				float v2 = 0f;
				if (drawMode == 4 && distanceMatrix != null) {
					Float fl = (Float) distanceMatrix.get(0, getSrcIdOrdered(iy), getDestIdOrdered(ix));
					if (fl != null) {
						v2 = fl.floatValue();
						sci.val += "\n" + "distance=" + StringUtil.floatToStr(v2, 0) + "\n" + "(max distance=" + StringUtil.floatToStr(max2, 0) + ")";
					}
					//else
					//System.out.println("* Panic: "+getSrcIdOrdered(iy)+" "+getSrcNameOrdered(iy)+", "+getDestIdOrdered(ix)+" "+getDestNameOrdered(ix));
				}
				drawValueInCell(g, i > focuserValue, i, Math.round(focuserValue), v2, max2, drawMode, x[1 + ix], y[1 + iy], dx, dy);
			}
		}
	}

	protected void drawValueInCell(Graphics g, boolean moreThanFocus, int value, int maxvalue, float secondvalue, float maxsecondvalue, int drawMode, int x1, int y1, int dx, int dy) {
		g.setClip(1 + x1, 1 + y1, dx - 1, dy - 1);
		if (value < 0) {
			g.setColor(Color.white);
			g.fillRect(1 + x1, 1 + y1, dx - 1, dy - 1);
		} else {
			g.setColor(Color.darkGray);
			if (moreThanFocus) {
				g.drawRect(1 + x1, 1 + y1, dx - 2, dy - 2);
				return;
			}
			float ratio = ((float) value) / maxvalue;
			if (drawMode == 1) {
				ratio = (float) Math.sqrt(ratio);
			}
			switch (drawMode) {
			case 1:
				int ww,
				hh;
				ww = Math.round(ratio * (dx - 1));
				hh = Math.round(ratio * (dy - 1));
				if (ratio > 0 && ww < 2) {
					ww = 2;
				}
				if (ratio > 0 && hh < 2) {
					hh = 2;
				}
				int dx2 = (dx - ww) / 2,
				dy2 = (dy - hh) / 2;
				g.fillRect(1 + x1 + dx2, 1 + y1 + dy2, dx - 1 - 2 * dx2, dy - 1 - 2 * dy2);
				break;
			case 2:
				hh = Math.round(ratio * (dy - 1));
				if (ratio > 0 && hh < 1) {
					hh = 1;
				}
				g.fillRect(1 + x1, y1 + dy - hh, dx - 1, hh);
				break;
			case 3:
				ww = Math.round(ratio * (dx - 1));
				if (ratio > 0 && ww < 1) {
					ww = 1;
				}
				g.fillRect(1 + x1, 1 + y1, ww, dy - 1);
				break;
			case 4:
				if (maxsecondvalue <= 0f) {
					break;
				}
				float ratiow = secondvalue / maxsecondvalue;
				ww = Math.round(ratiow * (dx - 1));
				hh = Math.round(ratio * (dy - 1));
				if (ratiow > 0 && ww < 2) {
					ww = 2;
				}
				if (ratio > 0 && hh < 2) {
					hh = 2;
				}
				dx2 = (dx - ww) / 2;
				dy2 = (dy - hh) / 2;
				int xs = x1 + dx2,
				xf = x1 + dx - dx2 - 1,
				ys = y1 + dy2,
				yf = y1 + dy - dy2,
				ym = y1 + dy / 2;
				g.drawLine(xs, ys, xs, yf);
				g.drawLine(xs + 1, ys, xs + 1, yf);
				g.drawLine(xs, ym, xf, ym);
				g.drawLine(xs, ym + 1, xf, ym + 1);
				//g.setColor(Color.black);
				//g.fillRect(1+x1+dx2,1+y1+dy2,dx-1-2*dx2,dy-1-2*dy2);
				/*
				g.setColor(Color.darkGray);
				int trX[]=new int[4], trY[]=new int[4];
				trX[0]=x1+dx/2;     trY[0]=y1+dy2;
				trX[1]=x1+dx2+1;    trY[1]=y1+dy-dy2;
				trX[2]=x1+dx-dx2-1; trY[2]=trY[1];
				trX[3]=trX[0];      trY[3]=trY[0];
				g.fillPolygon(trX,trY,4);
				g.drawPolygon(trX,trY,4);
				*/
				break;
			}
		}
	}

	public void drawVerticalString(Graphics g, String str, int x, int y, int width, int height) {
		int v = g.getFontMetrics(getFont()).getHeight() - 4;
		y += v;
		int j = 0;
		int k = str.length();
		while (j < k + 1) {
			if (j == k) {
				g.drawString(str.substring(j), x, y + (j * v));
			} else {
				g.drawString(str.substring(j, j + 1), x, y + (j * v));
			}
			j++;
		}
		/*
		Graphics2D g2d = (Graphics2D)g;
		// clockwise 90 degrees
		AffineTransform at = new AffineTransform();
		at.setToRotation(-Math.PI/2.0, width/2.0, height/2.0);
		g2d.setTransform(at);
		g2d.drawString(str, x, y);
		*/
	}

	public void redraw() {
		if (ma == null)
			return;
		Graphics g = getGraphics();
		if (g != null) {
			draw(g);
			g.dispose();
		}
	}

	/**
	* Returns a unique identifier of the event source (may be produced
	* automatically, used only internally, not shown to the user).
	* The identifier is used for explicit linking of producers and recipients of
	* object events.
	*/
	public String getIdentifier() {
		return plotId;
	}

	//===Mouse===

	public String selSrcID = null, selDestID = null;

	@Override
	public void mouseClicked(MouseEvent e) {
		if (popM == null || hotspots == null || hotspots.size() == 0)
			return;
		SingleCellInfo sci = null;
		boolean found = false;
		for (int i = 0; i < hotspots.size() && !found; i++) {
			sci = (SingleCellInfo) hotspots.elementAt(i);
			if (sci.r.contains(e.getX(), e.getY())) {
				found = true;
			}
		}
		Vector v = new Vector(2);
		if (found) {
			if (sci.idxSrc >= 0 && sci.idxDest < 0) { // click to a row... reorder matrix
				selSrcID = getSrcIdOrdered(sci.idxSrc);
				switch (iSortMode) {
				case 2:
					sortByValuesInRow(sci.idxSrc);
					break;
				case 3:
					sortByDistancesInRow(sci.idxSrc);
					break;
				default:
					redraw();
				}
				if (pcSupport == null && supervisor != null) {
					v.add(selSrcID);
					if (selDestID != null) {
						v.add(selDestID);
					}
					supervisor.getHighlighter(locationsSetId).replaceSelectedObjects(this, v);
				} else {
					notifySiteSelection(selSrcID, null);
				}
				return;
			}
			if (sci.idxSrc < 0 && sci.idxDest >= 0) { // click to a column... reorder matrix
				selDestID = getDestIdOrdered(sci.idxDest);
				switch (iSortMode) {
				case 2:
					sortByValuesInColumn(sci.idxDest);
					break;
				case 3:
					sortByDistancesInColumn(sci.idxDest);
					break;
				default:
					redraw();
				}
				if (pcSupport == null && supervisor != null) {
					v.add(selDestID);
					if (selSrcID != null) {
						v.add(selSrcID);
					}
					supervisor.getHighlighter(locationsSetId).replaceSelectedObjects(this, v);
				} else {
					notifySiteSelection(null, selDestID);
				}
				return;
			}
			if (sci.idxSrc >= 0 && sci.idxDest >= 0) { // click to a cell... send selection to orders (simple and aggregated)
				String selSrcId = getSrcIdOrdered(sci.idxSrc), selDestId = getDestIdOrdered(sci.idxDest);
				//System.out.println("* click to: src="+selSrcId+", dest="+selDestId);
				int idxs = (srcOrder == null) ? sci.idxSrc : srcOrder[sci.idxSrc], idxd = (destOrder == null) ? sci.idxDest : destOrder[sci.idxDest];
				Vector orderIDs = ma.getObjIDsforSelection(idxs, idxd);
				//System.out.println("* list is "+((orderIDs==null)?"empty":"non-empty"));
				if (pcSupport == null && supervisor != null) {
					if (orderIDs == null) {
						supervisor.getHighlighter(ma.getSetIDforSelection()).clearSelection(this);
						supervisor.getHighlighter(aggrOrdersSetId).clearSelection(this);
					} else {
						supervisor.getHighlighter(ma.getSetIDforSelection()).replaceSelectedObjects(this, orderIDs);
						Vector vid = new Vector(1, 1);
						vid.addElement(selSrcId + "__" + selDestId);
						supervisor.getHighlighter(aggrOrdersSetId).replaceSelectedObjects(this, vid);
					}
				} else {
					notifySiteSelection(selSrcId, selDestId);
				}
				return;
			}
		}
		selSrcID = selDestID = null;
		if (pcSupport == null && supervisor != null) {
			supervisor.getHighlighter(locationsSetId).clearSelection(this);
		} else {
			notifySiteSelection(null, null);
		}
		redraw();
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		if (popM == null || hotspots == null || hotspots.size() == 0)
			return;
		SingleCellInfo sci = null;
		boolean found = false;
		for (int i = 0; i < hotspots.size() && !found; i++) {
			sci = (SingleCellInfo) hotspots.elementAt(i);
			if (sci.r.contains(e.getX(), e.getY())) {
				found = true;
			}
		}
		if (!found) {
			popM.setText("");
			return;
		}
		String str = sci.val;
		popM.setText(str);
		popM.setKeepHidden(false);
		popM.startShow(e.getX(), e.getY());
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void mouseDragged(MouseEvent e) {
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
	}
	//===Mouse===
}
