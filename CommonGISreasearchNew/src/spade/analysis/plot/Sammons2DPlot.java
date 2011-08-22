package spade.analysis.plot;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Vector;

import spade.analysis.classification.ObjectColorer;
import spade.analysis.geocomp.voronoi.VoronoiNew;
import spade.analysis.system.Supervisor;
import spade.analysis.tools.clustering.PointOrganizerSpatialIndex;
import spade.lib.color.ColorScale2D;
import spade.vis.action.ObjectEventHandler;
import spade.vis.database.DataItem;
import spade.vis.database.ObjectContainer;
import spade.vis.database.ObjectFilter;
import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealPolyline;
import external.gunther_foidl.SammonsProjection;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Apr 7, 2010
 * Time: 2:18:54 PM
 * Uses Sammons projection (external.gunther_foidl.SammonsProjection) to
 * put multidimensional data in 2-dimensional display.
 */
public class Sammons2DPlot extends Plot implements ObjectColorer {
	public static final int color_scale_none = 0, color_scale_rectangular = 1, color_scale_polar = 2, color_scale_first = 0, color_scale_last = 2;
	/**
	* Used to generate unique identifiers of instances ScatterPlot
	*/
	protected static int nInstances = 0;
	/**
	 * Field (column) numbers
	 */
	protected int colNs[] = null;
	/**
	 * A pre-computed distance matrix
	 */
	protected float distMatrix[][] = null;
	/**
	 * Current color scale
	 */
	protected int colorScale = color_scale_rectangular;
	/**
	 * Whether to flip the color scale horizontally and vertically
	 */
	protected boolean flipColors = false, mirrorColors = false;
	/**
	 * An instance of SammonsProjection is created once and then can
	 * be reused, i.e. the algorithm can be resumed to run some more
	 * iterations
	 */
	protected SammonsProjection samPr = null;
	/**
	 * The indexes of the table records in the Sammon's projection
	 * (only the records satisfying the filter are used for the projection)
	 */
	protected int iPr[] = null;
	/**
	 * The minimum and maximum x- and y-values in the Sammon's projection
	 */
	protected double minX = Double.NaN, maxX = Double.NaN, minY = Double.NaN, maxY = Double.NaN;
	/**
	 * The tessellation of the projected space
	 */
	protected RealPolyline cells[] = null;
	/**
	 * The assignments of the objects to the Voronoi cells
	 */
	protected int objInCell[] = null;
	/**
	 * Whether to show object labels
	 */
	protected boolean showLabels = false;

	/**
	* Constructs a Plot. The argument isIndependent shows whether
	* this plot is displayed separately and, hence, should be registered at the
	* supervisor as an event source or it is a part of some larger plot.
	* The variable allowSelection shows whether the plot should listen to
	* mouse events and transform them to object selection events.
	* Supervisor provides access of a plot to the Highlighter (common for
	* all data displays) and in this way links together all displays
	* The argument handler is a reference to the component the plot
	* should send object events to. In a case when the plot is displayed
	* independently, the ObjectEventHandler is the supervisor (the supervisor
	* implements this interface). Otherwise, the handler is the larger plot in
	* which this plot is included as a part.
	* The larger plot should implement the ObjectEventHandler interface.
	*/
	public Sammons2DPlot(boolean isIndependent, boolean allowSelection, Supervisor sup, ObjectEventHandler handler) {
		super(isIndependent, allowSelection, sup, handler);
	}

	/**
	 * @param colNs - the numbers of table columns with the original data
	 */
	public boolean setup(int colNs[]) {
		this.colNs = colNs;
		if (colNs == null || colNs.length < 3)
			return false;
		if (dataTable == null || dataTable.getDataItemCount() < 3)
			return false;
		if (!getProjection(10))
			return false;
		if (dots == null || dots.length != dataTable.getDataItemCount()) {
			dots = new DotPlotObject[dataTable.getDataItemCount()];
			for (int i = 0; i < dots.length; i++) {
				dots[i] = new DotPlotObject();
				dots[i].id = dataTable.getDataItemId(i);
			}
		}
		for (PlotObject dot : dots) {
			dot.reset();
		}
		applyFilter();
		plotImageValid = bkgImageValid = false;
		return true;
	}

	/**
	 * @param distMatrix - a pre-computed distance matrix among ALL objects from the table
	 */
	public boolean setup(float distMatrix[][]) {
		if (distMatrix == null)
			return false;
		this.distMatrix = distMatrix;
		if (dataTable == null || dataTable.getDataItemCount() < 3)
			return false;
		if (!getProjection(10))
			return false;
		if (dots == null || dots.length != dataTable.getDataItemCount()) {
			dots = new DotPlotObject[dataTable.getDataItemCount()];
			for (int i = 0; i < dots.length; i++) {
				dots[i] = new DotPlotObject();
				dots[i].id = dataTable.getDataItemId(i);
			}
		}
		for (PlotObject dot : dots) {
			dot.reset();
		}
		applyFilter();
		plotImageValid = bkgImageValid = false;
		return true;
	}

	public boolean setup() {
		if (colNs != null)
			return setup(colNs);
		return setup(distMatrix);
	}

	/**
	 * Computes a new Sammon's projection
	 */
	public boolean getProjection(int nIterations) {
		plotImageValid = bkgImageValid = false;
		samPr = null;
		iPr = null;
		cells = null;
		objInCell = null;
		int nData = dataTable.getDataItemCount();
		ObjectFilter filter = dataTable.getObjectFilter();
		if (filter != null && !filter.areObjectsFiltered()) {
			filter = null;
		}
		if (filter != null) {
			nData = 0;
			for (int i = 0; i < dataTable.getDataItemCount(); i++)
				if (filter.isActive(i)) {
					++nData;
				}
		}
		if (nData < 3) {
			notifyColorsChange();
			return false;
		}
		if (iPr == null) {
			iPr = new int[dataTable.getDataItemCount()];
		}
		for (int i = 0; i < iPr.length; i++) {
			iPr[i] = -1;
		}
		if (distMatrix == null) {
			double data[][] = new double[nData][colNs.length];
			int idx = -1;
			for (int i = 0; i < dataTable.getDataItemCount(); i++)
				if (filter == null || filter.isActive(i)) {
					++idx;
					iPr[i] = idx;
					for (int j = 0; j < colNs.length; j++) {
						data[idx][j] = dataTable.getNumericAttrValue(colNs[j], i);
					}
				}
			for (int j = 0; j < colNs.length; j++) {
				double min = Double.NaN, max = Double.NaN;
				for (int i = 0; i < nData; i++)
					if (!Double.isNaN(data[i][j])) {
						if (Double.isNaN(min) || min > data[i][j]) {
							min = data[i][j];
						}
						if (Double.isNaN(max) || max < data[i][j]) {
							max = data[i][j];
						}
					}
				if (!Double.isNaN(min) && min < max) {
					double ratio = 1.0 / (max - min);
					for (int i = 0; i < nData; i++) {
						data[i][j] = ratio * (data[i][j] - min);
					}
				}
			}
			samPr = new SammonsProjection(data, 2, nIterations, true);
		} else {
			int activeRecNs[] = new int[nData];
			if (filter == null) {
				for (int i = 0; i < activeRecNs.length; i++) {
					activeRecNs[i] = iPr[i] = i;
				}
			} else {
				int idx = -1;
				for (int i = 0; i < dataTable.getDataItemCount(); i++)
					if (filter == null || filter.isActive(i)) {
						++idx;
						iPr[i] = idx;
						activeRecNs[idx] = i;
					}
			}
			double matr[][] = new double[nData][nData];
			for (int i = 0; i < nData; i++) {
				for (int j = 0; j < nData; j++) {
					matr[i][j] = 0;
				}
			}
			for (int i = 0; i < nData - 1; i++) {
				for (int j = i + 1; j < nData; j++) {
					matr[i][j] = matr[j][i] = distMatrix[activeRecNs[i]][activeRecNs[j]];
				}
			}
			samPr = new SammonsProjection(matr, null, 2, nIterations, true);
		}
		samPr.CreateMapping();
		boolean ok = getCoordinatesRanges();
		notifyColorsChange();
		return ok;
	}

	/**
	 * Refines the previously computed Sammon's projection by
	 * running the algorithm for the given number of additional iterations
	 */
	public void refineProjection(int nIterations) {
		if (samPr == null) {
			getProjection(nIterations);
			return;
		}
		//samPr.runMore(nIterations);
		samPr.reset(nIterations, true);
		samPr.CreateMapping();
		getCoordinatesRanges();
		cells = null;
		objInCell = null;
		plotImageValid = bkgImageValid = false;
		notifyColorsChange();
	}

	/**
	 * Finds the minimum and maximum x- and y- values in the Sammon's projection
	 */
	protected boolean getCoordinatesRanges() {
		if (samPr == null)
			return false;
		double proj[][] = samPr.getProjection();
		if (proj == null)
			return false;
		minX = Double.NaN;
		maxX = Double.NaN;
		minY = Double.NaN;
		maxY = Double.NaN;
		for (double[] element : proj) {
			double x = element[0], y = element[1];
			if (!Double.isNaN(x) && !Double.isNaN(y)) {
				if (Double.isNaN(minX) || minX > x) {
					minX = x;
				}
				if (Double.isNaN(maxX) || maxX < x) {
					maxX = x;
				}
				if (Double.isNaN(minY) || minY > y) {
					minY = y;
				}
				if (Double.isNaN(maxY) || maxY < y) {
					maxY = y;
				}
			}
		}
		return !Double.isNaN(minX) && !Double.isNaN(minY) && minX < maxX && minY < maxY;
	}

	/**
	* Resets its internal data, including the array of screen objects.
	* Called when records are added to or removed from the table.
	*/
	@Override
	public void reset() {
		minX = Double.NaN;
		maxX = Double.NaN;
		minY = Double.NaN;
		maxY = Double.NaN;
		if (dataTable == null || (colNs == null && distMatrix == null)) {
			dots = null;
			return;
		}
		samPr = null;
		iPr = null;
		cells = null;
		objInCell = null;
		setup();
		redraw();
	}

	@Override
	public boolean reloadAttributeData(Vector v) {
		if (v == null || colNs == null)
			return false;
		boolean relevantChange = false;
		for (int i = 0; i < colNs.length && !relevantChange; i++) {
			relevantChange = v.contains(dataTable.getAttributeId(colNs[i]));
		}
		if (relevantChange) {
			setup();
			return true;
		}
		return false;
	}

	@Override
	public int mapX(double v) {
		return bounds.x + mx1 + (int) Math.round((v - minX) / (maxX - minX) * width);
	}

	@Override
	public int mapY(double v) {
		return bounds.y + my1 + height - (int) Math.round((v - minY) / (maxY - minY) * height);
	}

	@Override
	public double absX(int x) {
		return minX + (maxX - minX) * (x - mx1 - bounds.x) / width;
	}

	@Override
	public double absY(int y) {
		return minY + (maxY - minY) * (height - y + my1 + bounds.y) / height;
	}

	@Override
	public void countScreenCoordinates() {
		if (dots == null || samPr == null || iPr == null)
			return;
		double proj[][] = samPr.getProjection();
		if (proj == null)
			return;
		for (int i = 0; i < dots.length; i++) {
			int idx = iPr[i];
			if (idx >= 0 && !Double.isNaN(proj[idx][0]) && !Double.isNaN(proj[idx][1])) {
				dots[i].x = mapX(proj[idx][0]);
				dots[i].y = mapY(proj[idx][1]);
			} else {
				dots[i].reset();
			}
		}
	}

	public void setColorScale(int colorScale) {
		if (colorScale >= color_scale_first && colorScale <= color_scale_last) {
			this.colorScale = colorScale;
			plotImageValid = bkgImageValid = false;
			redraw();
			notifyColorsChange();
		}
	}

	public int getColorScale() {
		return colorScale;
	}

	/**
	 * Whether to flip the color scale relative to the horizontal axis
	 */
	public void setFlipColors(boolean flipColors) {
		if (flipColors == this.flipColors)
			return;
		this.flipColors = flipColors;
		if (colorScale == color_scale_none)
			return;
		plotImageValid = bkgImageValid = false;
		redraw();
		notifyColorsChange();
	}

	/**
	 * Whether to flip the color scale relative to the vertical axis
	 */
	public void setMirrorColors(boolean mirrorColors) {
		if (mirrorColors == this.mirrorColors)
			return;
		this.mirrorColors = mirrorColors;
		if (colorScale == color_scale_none)
			return;
		plotImageValid = bkgImageValid = false;
		redraw();
		notifyColorsChange();
	}

	/**
	 * Whether to show object labels
	 */
	public void setShowLabels(boolean showLabels) {
		this.showLabels = showLabels;
	}

	/**
	 * Tessellates the projection space into Voronoi polygons
	 * @param radius - the desired radius, in percents of the
	 *   projection space widths
	 */
	public void tessellate(double radius) {
		if (samPr == null)
			return;
		double proj[][] = samPr.getProjection();
		if (proj == null)
			return;
		double wx = maxX - minX, wy = maxY - minY, w = Math.max(wx, wy);
		radius = radius * w / 100;
		PointOrganizerSpatialIndex pOrg = new PointOrganizerSpatialIndex();
		pOrg.setSpatialExtent((float) minX, (float) minY, (float) maxX, (float) maxY);
		pOrg.setMaxRad(radius);
		pOrg.setGeo(false, 1, 1);
		long t0 = System.currentTimeMillis();
		showMessage("Grouping the points by proximity ...", false);
		for (int i = 0; i < proj.length; i++)
			if (!Double.isNaN(proj[i][0]) && !Double.isNaN(proj[i][1])) {
				pOrg.addPoint(new RealPoint((float) proj[i][0], (float) proj[i][1]));
			}
		int nGroups = pOrg.getGroupCount();
		if (nGroups < 2) {
			showMessage("Failed to group the points!", true);
			return;
		}
		pOrg.reDistributePoints();
		long t = System.currentTimeMillis();
		System.out.println(nGroups + " point clusters obtained in " + (t - t0) + " msec.");
		showMessage(nGroups + " point clusters obtained in " + (t - t0) + " msec. Optimizing the grouping...", false);
		t0 = System.currentTimeMillis();
		pOrg.optimizeGrouping();
		nGroups = pOrg.getGroupCount();
		t = System.currentTimeMillis();
		showMessage(nGroups + " groups after optimization; elapsed time " + (t - t0) + " msec.", false);
		System.out.println(nGroups + " groups after optimization; elapsed tim " + (t - t0) + " msec.");

		Vector<RealPoint> points = new Vector<RealPoint>(nGroups, 10);
		for (int i = 0; i < nGroups; i++) {
			RealPoint pt = pOrg.getMedoidOrCentroid(i);
			if (pt != null) {
				points.addElement(pt);
			}
		}
		float dy = 2 * (float) radius, dx = 2 * (float) radius, dx2 = dx / 2, dy2 = dy / 2;
		float y1 = pOrg.y1 - dy - dy2, y2 = pOrg.y2 + dy + dy2;
		float x1 = pOrg.x1 - dx - dx2, x2 = pOrg.x2 + dx + dx2;
		/*
		//introducing additional points in empty areas and on the boundaries
		int k=0;
		for (float y=y1; y<=y2+dy2; y+=dy) {
		  float ddx=(k%2==0)?0:dx2;
		  ++k;
		  for (float x=x1+ddx; x<=x2+dx2; x+=dx)
		    if (pOrg.isFarFromAll(x,y))
		      points.addElement(new RealPoint(x,y));
		}
		*/
		showMessage("Building Voronoi polygons; wait...", false);
		System.currentTimeMillis();
		VoronoiNew voronoi = new VoronoiNew(points);
		if (!voronoi.isValid()) {
			showMessage("Failed to triangulate!", true);
			return;
		}
		voronoi.setBuildNeighbourhoodMatrix(true);
		RealPolyline areas[] = voronoi.getPolygons(x1, y1, x2, y2);
		if (areas == null) {
			showMessage("Failed to build polygons!", true);
			return;
		}
		t = System.currentTimeMillis();
		int nPolygons = 0;
		for (RealPolyline area : areas)
			if (area != null) {
				++nPolygons;
			}
		showMessage("Got " + nPolygons + " Voronoi polygons; elapsed time = " + (t - t0) + " msec.", false);
		System.out.println("Got " + nPolygons + " Voronoi polygons; elapsed time = " + (t - t0) + " msec.");
		if (nPolygons > 0) {
			cells = areas;
			objInCell = null;
			plotImageValid = bkgImageValid = false;
			redraw();
			notifyColorsChange();
		}
	}

	/**
	 * Finds the color for the cell with the given index
	 */
	public Color getColorForCell(int cellIdx) {
		if (cells == null || cellIdx < 0 || cellIdx >= cells.length || cells[cellIdx] == null)
			return null;
		float c[] = cells[cellIdx].getCentroid();
		return getColor(c[0], c[1]);
	}

	/**
	 * Finds the color for the given (x,y)-position in the projection space
	 */
	public Color getColor(float x, float y) {
		if (flipColors) {
			y = (float) (maxY - (y - minY));
		}
		if (mirrorColors) {
			x = (float) (maxX - (x - minX));
		}
		return (colorScale == color_scale_polar) ? ColorScale2D.getColorCircular(x, y, (float) minX, (float) maxX, (float) minY, (float) maxY) : ColorScale2D.getColor(x, y, (float) minX, (float) maxX, (float) minY, (float) maxY);
	}

	/**
	 * Draws the background of the plot
	 */
	@Override
	public void drawReferenceFrame(Graphics g) {
		if (colorScale == color_scale_none) {
			g.setColor(plotAreaColor);
			g.fillRect(bounds.x, bounds.y, bounds.width + 1, bounds.height + 1);
		}
		if (cells != null) {
			for (int i = 0; i < cells.length; i++)
				if (cells[i] != null) {
					int np = cells[i].p.length;
					int xp[] = new int[np], yp[] = new int[np];
					for (int j = 0; j < np; j++) {
						xp[j] = mapX(cells[i].p[j].x);
						yp[j] = mapY(cells[i].p[j].y);
					}
					if (colorScale != color_scale_none) {
						g.setColor(getColorForCell(i));
						g.fillPolygon(xp, yp, np);
					}
					g.setColor(Color.darkGray);
					g.drawPolygon(xp, yp, np);
				}
		}
		if (cells == null && colorScale != color_scale_none) {
			int nSteps = 50, scrX = 0;
			for (int i = 0; i < nSteps; i++) {
				int w = (bounds.width - scrX) / (nSteps - i);
				float x = (float) absX(scrX);
				int scrY = 0;
				for (int j = 0; j < nSteps; j++) {
					int h = (bounds.height - scrY) / (nSteps - j);
					float y = (float) absY(scrY);
					Color c = getColor(x, y);
					g.setColor(c);
					g.fillRect(scrX, scrY, w + 1, h + 1);
					scrY += h;
				}
				scrX += w;
			}
		}
	}

	@Override
	public void drawAllInInactiveMode(Graphics g) {
		super.drawAllInInactiveMode(g);
		if (showLabels) {
			g.setColor(Color.black);
			int asc = g.getFontMetrics().getAscent();
			for (int i = 0; i < dots.length; i++)
				if (isPointInPlotArea(dots[i].x, dots[i].y)) {
					String name = dataTable.getDataItemName(i);
					if (name == null) {
						name = dots[i].id;
					}
					g.drawString(name, dots[i].x - PlotObject.rad - 10, dots[i].y + PlotObject.rad + asc);
				}
		}
	}

	/**
	 * Returns the coordinates of the objects in the projection
	 */
	public RealPoint[] getObjectCoordinates() {
		if (samPr == null || iPr == null)
			return null;
		double proj[][] = samPr.getProjection();
		if (proj == null)
			return null;
		RealPoint coords[] = new RealPoint[iPr.length];
		for (int i = 0; i < iPr.length; i++) {
			int idx = iPr[i];
			if (idx >= 0 && !Double.isNaN(proj[idx][0]) && !Double.isNaN(proj[idx][1])) {
				coords[i] = new RealPoint((float) proj[idx][0], (float) proj[idx][1]);
			} else {
				coords[i] = null;
			}
		}
		return coords;
	}

	/**
	 * For each object, determines in which cell of the Voronoi tessellation
	 * its projection belongs
	 */
	protected boolean determineObjectCells() {
		objInCell = null;
		if (cells == null || samPr == null || iPr == null)
			return false;
		double proj[][] = samPr.getProjection();
		if (proj == null)
			return false;
		objInCell = new int[iPr.length];
		for (int i = 0; i < iPr.length; i++) {
			objInCell[i] = -1;
			int idx = iPr[i];
			if (idx >= 0 && !Double.isNaN(proj[idx][0]) && !Double.isNaN(proj[idx][1])) {
				float x = (float) proj[idx][0], y = (float) proj[idx][1];
				for (int j = 0; j < cells.length; j++)
					if (cells[j] != null && cells[j].contains(x, y, 0, true)) {
						objInCell[i] = j;
					}
			}
		}
		return true;
	}

	/**
	 * Returns true if there is a tessellation of the projection space
	 */
	public boolean hasClasses() {
		if (objInCell == null) {
			determineObjectCells();
		}
		return objInCell != null;
	}

	/**
	 * Returns the assignment of the objects to the cells, i.e. classes
	 */
	public int[] getObjectClasses() {
		return objInCell;
	}

//-------------------------- ObjectColorer ----------------------------------------
	/**
	* Returns the color for the object with the given identifier, depending on
	* the current classification.
	*/
	@Override
	public Color getColorForObject(String objId) {
		int idx = dataTable.indexOf(objId);
		if (idx < 0)
			return null;
		return getColorForObject(idx);
	}

	/**
	* Returns the color for the object with the given index in the container, depending on
	* the current classification.
	*/
	@Override
	public Color getColorForObject(int objIdx) {
		if (colorScale == color_scale_none)
			return null;
		if (samPr == null || iPr == null)
			return null;
		int oPIdx = iPr[objIdx];
		if (oPIdx < 0)
			return null;
		if (cells != null) {
			if (objInCell == null)
				if (!determineObjectCells())
					return null;
			int cIdx = objInCell[oPIdx];
			if (cIdx < 0)
				return null;
			return getColorForCell(cIdx);
		}
		double proj[][] = samPr.getProjection();
		if (proj == null)
			return null;
		float x = (float) proj[oPIdx][0], y = (float) proj[oPIdx][1];
		Color c = getColor(x, y);
		return c;
	}

	/**
	* Returns the color for the given data item, depending on the current
	* classification. The data item belongs to the container with the identifier
	* passed as the argument containerId. The ObjectColorer can check whether
	* the given data item is relevant for it.
	*/
	@Override
	public Color getColorForDataItem(DataItem dit, String containerId) {
		if (containerId == null || !containerId.equals(getContainerIdentifier()))
			return getColorForObject(dit.getId());
		return getColorForObject(dit.getIndexInContainer());
	}

	/**
	* Returns the color for the given data item, depending on the current
	* classification without checking the container of the data item.
	*/
	@Override
	public Color getColorForDataItem(DataItem dit) {
		return getColorForObject(dit.getId());
	}

	/**
	 * Returns the identifier of the set of objects this ObjectColorer deals with
	 */
	@Override
	public String getEntitySetIdentifier() {
		return dataTable.getEntitySetIdentifier();
	}

	/**
	 * Returns the identifier of the container this ObjectColorer deals with
	 */
	@Override
	public String getContainerIdentifier() {
		return dataTable.getContainerIdentifier();
	}

	/**
	 * Returns a reference to the container with the colored objects
	 */
	@Override
	public ObjectContainer getObjectContainer() {
		return (ObjectContainer) dataTable;
	}

	/**
	* To handle the list of listeners and notify them about changes of the
	* object set or object data, a PropertyChangeSupport is used.
	*/
	protected PropertyChangeSupport pcSupport = null;

	/**
	* Registeres a listener of changes of classification. The
	* listener must implement the PropertyChangeListener interface.
	*/
	@Override
	public synchronized void addPropertyChangeListener(PropertyChangeListener l) {
		if (l == null)
			return;
		if (pcSupport == null) {
			pcSupport = new PropertyChangeSupport(this);
		}
		pcSupport.addPropertyChangeListener(l);
	}

	/**
	* Unregisteres a listener of changes of oclassification.
	*/
	@Override
	public synchronized void removePropertyChangeListener(PropertyChangeListener l) {
		if (l == null || pcSupport == null)
			return;
		pcSupport.removePropertyChangeListener(l);
	}

	/**
	* Notify all the listeners about changes of the classes.
	*/
	public void notifyClassesChange() {
		notifyChange("classes", null);
	}

	/**
	* Notify all the listeners about changes of the colors.
	*/
	public void notifyColorsChange() {
		notifyChange("colors", null);
	}

	/**
	* An internal method used to notify all the listeners about changes of
	* classification (what=="classes") or colors (what=="colors").
	* The classifier may also notify the listeners when it is destroyed.
	* In this case the property name is "destroyed".
	*/
	public void notifyChange(String what) {
		notifyChange(what, null);
	}

	protected void notifyChange(String what, Object value) {
		if (pcSupport == null)
			return;
		pcSupport.firePropertyChange(what, null, value);
	}

	/**
	* Makes necessary operations for destroying and notifies its listeners about
	* being destroyed.
	*/
	@Override
	public void destroy() {
		if (isDestroyed())
			return;
		super.destroy();
		notifyChange("destroyed", null);
	}

//---------------- reaction to mouse interactions ---------------------------------

	protected int dragX1 = -1, dragY1 = -1, dragX2 = dragX1, dragY2 = dragY1;
	protected boolean dragging = false;
	protected int prevLinePos[] = null;

	@Override
	public void mouseDragged(MouseEvent e) {
		if (canvas == null)
			return;
		int x = e.getX(), y = e.getY();

		if (!dragging && !isPointInPlotArea(dragX1, dragY1))
			return;
		dragging = dragging || Math.abs(x - dragX1) > 5 || Math.abs(y - dragY1) > 5;
		if (!dragging)
			return;
		if (x < bounds.x + mx1) {
			x = bounds.x + mx1;
		}
		if (x > bounds.x + mx1 + width) {
			x = bounds.x + mx1 + width;
		}
		if (y < bounds.y + my1) {
			y = bounds.y + my1;
		}
		if (y > bounds.y + my1 + height) {
			y = bounds.y + my1 + height;
		}
		if (x == dragX2 && y == dragY2)
			return;
		drawFrame(dragX1, dragY1, dragX2, dragY2);
		dragX2 = x;
		dragY2 = y;
		drawFrame(dragX1, dragY1, dragX2, dragY2);
		if ((dragX2 - dragX1) * (dragY2 - dragY1) > 0) {
			canvas.setCursor(new Cursor(Cursor.NW_RESIZE_CURSOR));
		} else {
			canvas.setCursor(new Cursor(Cursor.NE_RESIZE_CURSOR));
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {
		int x = e.getX(), y = e.getY();
		if (isPointInPlotArea(x, y)) {
			dragX1 = dragX2 = x;
			dragY1 = dragY2 = y;
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (!dragging)
			if (selectObjectAt(dragX1, dragY1, e)) {
				;
			} else {
				processMouseClick(dragX1, dragY1);
			}
		else {
			drawFrame(dragX1, dragY1, dragX2, dragY2);
			dragging = false;
			selectInFrame(dragX1, dragY1, dragX2, dragY2, e);
			canvas.setCursor(Cursor.getDefaultCursor());
		}
		dragX1 = dragY1 = dragX2 = dragY2 = -1;
	}

	protected void processMouseClick(int scrX, int scrY) {
	}

	@Override
	public Vector getAttributeList() {
		Vector a = null;
		if (dataTable != null && colNs != null) {
			a = new Vector(colNs.length, 1);
			for (int colN : colNs) {
				a.addElement(dataTable.getAttributeId(colN));
			}
		}
		return a;
	}

	/**
	* Used to generate unique identifiers of instances of Plot's descendants.
	* The base class Plot calls this method in the constructor.
	*/
	@Override
	protected void countInstance() {
		instanceN = ++nInstances;
	}

	/**
	* Used to generate unique identifiers of instances of Plot's descendants.
	* Plot uses this method in the method getIdentifier().
	*/
	@Override
	public String getPlotTypeName() {
		return "Sammons_Projection_2D";
	}

	protected void showMessage(String msg, boolean isError) {
		if (isError) {
			System.out.println("ERROR: " + msg);
		}
		if (supervisor == null || supervisor.getUI() == null)
			return;
		supervisor.getUI().showMessage(msg, isError);
	}
}
