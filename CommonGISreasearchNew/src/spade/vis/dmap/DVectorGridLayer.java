package spade.vis.dmap;

import java.awt.Graphics;
import java.util.Vector;

import spade.lib.util.IdMaker;
import spade.lib.util.StringUtil;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.database.Parameter;
import spade.vis.database.SpatialEntity;
import spade.vis.database.ThematicDataItem;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RealRectangle;
import spade.vis.map.MapContext;
import spade.vis.mapvis.Visualizer;
import spade.vis.space.GeoLayer;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 06-Oct-2006
 * Time: 17:27:14
 * A layer with a coarse regular grid. Consists of rectangles (instances of
 * RealRectangle)
 */
public class DVectorGridLayer extends DGeoLayer {
	/**
	 * Number of columns in the grid
	 */
	protected int nCols = 0;
	/**
	 * Number of rows in the grid
	 */
	protected int nRows = 0;
	/**
	 * x-coordinates of the vertical grid lines
	 */
	protected float colXCoord[] = null;
	/**
	 * y-coordinates of the horizontal grid lines
	 */
	protected float rowYCoord[] = null;
	/**
	 * The array of objects, where the geometries are instances of RealRectangle
	 */
	protected DGeoObject gridObj[][] = null;

	/**
	* Returns the spatial type of its objects: area.
	*/
	@Override
	public char getType() {
		return Geometry.area;
	}

	/**
	* Does not allow to change the spatial type of the objects comprising this
	* GeoLayer.
	*/
	@Override
	public void setType(char type) {
	}

	/**
	 * Returns the subtype of the objects, which may be one of the special
	 * subtypes: circle, rectangle, vector, link, ... If not any of these subtypes,
	 * returns Geometry.undefined.
	 */
	@Override
	public char getSubtype() {
		return Geometry.rectangle;
	}

	/**
	 * Constructs geographical objects according to the given coordinates of the
	 * vertical and horizontal grid lines
	 */
	public void constructObjects(float xCoord[], float yCoord[]) {
		if (xCoord == null || yCoord == null || xCoord.length < 2 || yCoord.length < 2)
			return;
		int oldCount = getObjectCount();
		nCols = xCoord.length - 1;
		nRows = yCoord.length - 1;
		int max = (nCols > nRows) ? nCols : nRows;
		int nDigits = 1;
		do {
			max /= 10;
			if (max > 0) {
				++nDigits;
			}
		} while (max > 0);

		colXCoord = xCoord;
		rowYCoord = yCoord;
		geoObj = new Vector(nCols * nRows, 100);
		gridObj = new DGeoObject[nRows][nCols];
		for (int r = 0; r < nRows; r++) {
			float y0 = rowYCoord[r], y1 = rowYCoord[r + 1];
			String idStart = StringUtil.padString(String.valueOf(r + 1), '0', nDigits, true) + "_";
			for (int c = 0; c < nCols; c++) {
				RealRectangle rr = new RealRectangle(colXCoord[c], y0, colXCoord[c + 1], y1);
				SpatialEntity spe = new SpatialEntity(idStart + StringUtil.padString(String.valueOf(c + 1), '0', nDigits, true));
				spe.setGeometry(rr);
				DGeoObject gObj = new DGeoObject();
				gObj.setup(spe);
				gridObj[r][c] = gObj;
				geoObj.addElement(gObj);
				gObj.getSpatialData().setIndexInContainer(geoObj.size() - 1);
			}
		}
		hasAllObjects = true;
		linkedToTable = false;
		hasHoles = false;
		hasLabels = false;
		if (oldCount < 1 && geoObj.size() > 0) {
			notifyPropertyChange("got_data", null, null);
		}
	}

	public int getNCols() {
		return nCols;
	}

	public int getNRows() {
		return nRows;
	}

	/**
	 * Returns the position of the object with the given index in the grid: [row,column]
	 */
	public int[] getRowAndColumn(int objIdx) {
		if (gridObj == null)
			return null;
		int pos[] = { objIdx / nCols, objIdx % nCols };
		return pos;
	}

	/**
	 * Returns the object at the given position in the grid
	 */
	public DGeoObject getObject(int row, int col) {
		if (gridObj == null)
			return null;
		if (row < 0 || row >= nRows || col < 0 || col >= nCols)
			return null;
		return gridObj[row][col];
	}

	/**
	 * Returns the index of the object at the given position in the grid
	 */
	public int getObjectIndex(int row, int col) {
		DGeoObject gobj = getObject(row, col);
		if (gobj == null)
			return -1;
		return gobj.getSpatialData().getIndexInContainer();
	}

	/**
	* This method is used to pass to the layer source thematic data for
	* visualisation on a map. Returns the number of thematic data items
	* successfully linked to corresponding geographical objects.
	*/
	@Override
	public int receiveThematicData(AttributeDataPortion dp) {
		if (dTable != null && dTable.equals(dp))
			return dTable.getDataItemCount();
		return super.receiveThematicData(dp);
	}

	/**
	 * Attaches thematic data to the objects; xfn and yfn are the numbers of the
	 * table fields that contain the indexes of columns and rows, respectively.
	 * Returns the number of objects that have been linked to thematic data.
	 */
	public int attachTable(AttributeDataPortion table, int xfn, int yfn) {
		if (table == null || !table.hasData() || xfn < 0 || yfn < 0)
			return 0;
		if (gridObj == null || geoObj == null)
			return 0;
		if (dTable != null && !dTable.equals(table)) {
			dTable.removePropertyChangeListener(this);
		}
		if (table != null && table.equals(dTable)) {
			table.addPropertyChangeListener(this);
		}
		dTable = table;
		linkedToTable = false;
		int nlinked = 0, nTRec = dTable.getDataItemCount();
		System.out.println("Layer " + getName() + " is being linked to table " + dTable.getName());
		long t = System.currentTimeMillis();
		for (int i = 0; i < nTRec; i++) {
			ThematicDataItem dit = (ThematicDataItem) dTable.getDataItem(i);
			if (dit == null) {
				continue;
			}
			int cx = (int) Math.round(dit.getNumericAttrValue(xfn)) - 1, ry = (int) Math.round(dit.getNumericAttrValue(yfn) - 1);
			if (cx >= 0 && cx < nCols && ry >= 0 && ry < nRows && gridObj[ry][cx] != null) {
				gridObj[ry][cx].setThematicData(dit);
				dit.setId(gridObj[ry][cx].getIdentifier());
				++nlinked;
			}
		}
		linkedToTable = nlinked > 0;
		t = System.currentTimeMillis() - t;
		System.out.println("Layer " + name + ": " + nlinked + " objects were linked to thematic data during " + t + " msec");
		if (nlinked < geoObj.size()) {
			DataTable dt = null;
			if (dTable instanceof DataTable) {
				dt = (DataTable) dTable;
			}
			// where there are no thematic data, attach empty records
			// this is useful for enabling the use of the layer in further
			// geocomputations, e.g. aggregation of raster or point data by grid cells
			for (int r = 0; r < nRows; r++) {
				for (int c = 0; c < nCols; c++)
					if (gridObj[r][c] != null)
						if (gridObj[r][c].getData() == null) {
							DataRecord rec = new DataRecord(gridObj[r][c].getIdentifier());
							if (dt != null) {
								dt.addDataRecord(rec);
							}
							for (int i = 0; i < dt.getAttrCount(); i++) {
								rec.addAttrValue(null);
							}
							gridObj[r][c].setThematicData(rec);
						}
			}
		}
		dTable.rebuildDataIndex();
		notifyPropertyChange("ObjectData", null, null);
		return nlinked;
	}

	/**
	 * Adds the attributes and their values from the given table to the table
	 * which is currently attached to this layer (i.e. joins two tables).
	 * If the layer has no table yet, exits and returns 0.
	 * The arguments xfn and yfn are the numbers of the table fields that contain
	 * the indexes of columns and rows, respectively. Returns the number of updated
	 * records.
	 */
	public int addThematicData(AttributeDataPortion table, int xfn, int yfn) {
		if (table == null || !table.hasData() || xfn < 0 || yfn < 0)
			return 0;
		if (gridObj == null || geoObj == null || dTable == null || !dTable.hasData())
			return 0;
		if (dTable.equals(table))
			return Math.min(dTable.getDataItemCount(), table.getDataItemCount());
		if (!(dTable instanceof DataTable)) {
			System.out.println("Layer " + getName() + ": the current table is not an instance of DataTable!");
			return 0;
		}
		DataTable dt = (DataTable) dTable;
		int nUpdated = 0;
		int first = -1;
		System.out.println("Layer " + getName() + " takes new thematic data from table " + table.getName());
		long t = System.currentTimeMillis();
		if (table.getParamCount() > 0) {
			for (int i = 0; i < table.getParamCount(); i++) {
				Parameter par = table.getParameter(i);
				Parameter par0 = dt.getParameter(par.getName());
				if (par0 == null) {
					dt.addParameter(par);
				} else if (par0.isSame(par)) {
					continue;
				} else {
					for (int j = 0; j < par.getValueCount(); j++)
						if (par0.getValueIndex(par.getValue(j)) < 0) {
							par0.addValue(par.getValue(j));
						}
				}
			}
		}
		int nOldAttr = dt.getAttrCount();
		for (int i = 0; i < table.getAttrCount(); i++)
			if (i != xfn && i != yfn) {
				Attribute attr = table.getAttribute(i);
				attr.setIdentifier(IdMaker.makeId(attr.getIdentifier(), dt));
				dt.addAttribute(attr);
			}
		for (int i = 0; i < table.getDataItemCount(); i++) {
			ThematicDataItem dit = (ThematicDataItem) table.getDataItem(i);
			if (dit == null) {
				continue;
			}
			int cx = (int) Math.round(dit.getNumericAttrValue(xfn)) - 1, ry = (int) Math.round(dit.getNumericAttrValue(yfn) - 1);
			if (cx >= 0 && cx < nCols && ry >= 0 && ry < nRows && gridObj[ry][cx] != null) {
				DataRecord rec = (DataRecord) gridObj[ry][cx].getData();
				if (rec == null) {
					rec = new DataRecord(gridObj[ry][cx].getIdentifier());
					gridObj[ry][cx].setThematicData(dit);
					dt.addDataRecord(rec);
				}
				int cN = nOldAttr;
				for (int j = 0; j < table.getAttrCount(); j++)
					if (j != xfn && j != yfn) {
						rec.setAttrValue(dit.getAttrValue(j), cN++);
					}
				++nUpdated;
			}
		}
		t = System.currentTimeMillis() - t;
		if (nUpdated < 1) {
			System.out.println("Layer " + name + ": could not update the thematic data; no corresponding table records found!");
			return 0;
		}
		System.out.println("Layer " + name + ": thematic data of " + nUpdated + " objects were updated during " + t + " msec");
		notifyPropertyChange("ObjectData", null, null);
		return nUpdated;
	}

	/**
	* Returns a copy of this GeoLayer. The reference to the visualizer is also copied.
	*/
	@Override
	public GeoLayer makeCopy() {
		DVectorGridLayer layer = new DVectorGridLayer();
		Vector gObjSaved = geoObj;
		geoObj = null;
		copyTo(layer);
		geoObj = gObjSaved;
		layer.copyData(nCols, nRows, gridObj);
		layer.copyGridParams(colXCoord, rowYCoord);
		return layer;
	}

	/**
	 * Copies geo objects for this layer from the data provided
	 */
	public void copyData(int nCols, int nRows, DGeoObject obj[][]) {
		if (obj == null)
			return;
		this.nCols = nCols;
		this.nRows = nRows;
		gridObj = new DGeoObject[nRows][nCols];
		geoObj = new Vector(nRows * nCols, 1000);
		for (int r = 0; r < nRows; r++) {
			for (int c = 0; c < nCols; c++)
				if (obj[r][c] != null) {
					gridObj[r][c] = (DGeoObject) obj[r][c].makeCopy();
					geoObj.addElement(gridObj[r][c]);
				} else {
					gridObj[r][c] = null;
				}
		}
	}

	/**
	 * Copies the grid parameters
	 */
	public void copyGridParams(float xCoord[], float yCoord[]) {
		if (xCoord != null && xCoord.length > 0) {
			colXCoord = new float[xCoord.length];
			for (int i = 0; i < xCoord.length; i++) {
				colXCoord[i] = xCoord[i];
			}
		}
		if (yCoord != null && yCoord.length > 0) {
			rowYCoord = new float[yCoord.length];
			for (int i = 0; i < yCoord.length; i++) {
				rowYCoord[i] = yCoord[i];
			}
		}
	}

	/**
	* A method from the basic GeoLayer interface.
	* This method is used to find the objects pointed to with the mouse.
	* Only objects having identifiers can be picked.
	* When the argument findOne is true, the method returns after finding the
	* first object at the mouse position
	*/
	@Override
	public Vector findObjectsAt(int x, int y, MapContext mc, boolean findOne) {
		if (!drawParm.drawLayer || geoObj == null || geoObj.size() < 1)
			return null;
		if (mc == null)
			return null;
		if (gridObj == null || colXCoord == null || colXCoord.length < 2 || rowYCoord == null || rowYCoord.length < 2)
			return super.findObjectsAt(x, y, mc, findOne);
		Vector pointed = new Vector(10, 10);
		float rx = mc.absX(x), ry = mc.absY(y);
		if (rx < colXCoord[0])
			return null;
		if (ry < rowYCoord[0])
			return null;
		for (int c = 1; c < colXCoord.length; c++)
			if (rx <= colXCoord[c]) {
				for (int r = 1; r < rowYCoord.length; r++)
					if (ry <= rowYCoord[r]) {
						if (gridObj[r - 1][c - 1] != null && (dTable == null || (gridObj[r - 1][c - 1].getData() != null && !gridObj[r - 1][c - 1].getData().isEmpty()))) {
							pointed.addElement(gridObj[r - 1][c - 1].getIdentifier());
						}
						if (ry < rowYCoord[r]) {
							break;
						}
					}
				if (rx < colXCoord[c]) {
					break;
				}
			}
		if (pointed.size() < 1)
			return null;
		return pointed;
	}

	/**
	 * Finds an object containing the specified position irrespective of
	 * the filters and visibility. Returns the index of the object or -1 if not found.
	 */
	@Override
	public int findObjectContainingPosition(float rx, float ry) {
		if (geoObj == null || geoObj.size() < 1)
			return -1;
		if (gridObj == null || colXCoord == null || colXCoord.length < 2 || rowYCoord == null || rowYCoord.length < 2)
			return super.findObjectContainingPosition(rx, ry);
		if (rx < colXCoord[0])
			return -1;
		if (ry < rowYCoord[0])
			return -1;
		for (int c = 1; c < colXCoord.length; c++)
			if (rx <= colXCoord[c]) {
				for (int r = 1; r < rowYCoord.length; r++)
					if (ry <= rowYCoord[r]) {
						if (gridObj[r - 1][c - 1] != null && (dTable == null || (gridObj[r - 1][c - 1].getData() != null && !gridObj[r - 1][c - 1].getData().isEmpty())))
							return gridObj[r - 1][c - 1].getSpatialData().getIndexInContainer();
						if (ry < rowYCoord[r]) {
							break;
						}
					}
				if (rx < colXCoord[c]) {
					break;
				}
			}
		return -1;
	}

	@Override
	public void draw(Graphics g, MapContext mc) {
		if (geoObj == null || g == null || mc == null)
			return;
		lastPixelValue = mc.getPixelValue();
		if ((vis != null || bkgVis != null) && dTable != null && !linkedToTable) {
			linkToThematicData();
		}
		RealRectangle rr = mc.getVisibleTerritory();
		Visualizer v1 = null, v2 = null;
		if (vis != null && vis.isEnabled()) {
			v1 = vis;
		}
		if (bkgVis != null && bkgVis.isEnabled()) {
			v2 = bkgVis;
		}
		nActive = 0;
		for (int i = 0; i < getObjectCount(); i++) {
			DGeoObject gobj = getObject(i);
			if (dTable != null && (gobj.getData() == null || gobj.getData().isEmpty())) {
				continue;
			}
			if (!isObjectActive(i)) {
				continue;
			}
			++nActive;
			if (gobj.fitsInRectangle(rr.rx1, rr.ry1, rr.rx2, rr.ry2)) {
				gobj.setDrawingParameters(drawParm);
				gobj.setVisualizer(v1);
				gobj.setBackgroundVisualizer(v2);
				gobj.draw(g, mc);
			}
		}
	}

	@Override
	public void drawDiagrams(Graphics g, MapContext mc) {
		if (geoObj == null || g == null || mc == null)
			return;
		if (vis == null || !vis.isEnabled() || !vis.isDiagramPresentation())
			return;
		if ((vis != null || bkgVis != null) && dTable != null && !linkedToTable) {
			linkToThematicData();
		}
		RealRectangle rr = mc.getVisibleTerritory();
		for (int i = 0; i < getObjectCount(); i++) {
			DGeoObject gobj = getObject(i);
			if (gobj.getData() == null || gobj.getData().isEmpty()) {
				continue;
			}
			if (isObjectActive(i) && gobj.fitsInRectangle(rr.rx1, rr.ry1, rr.rx2, rr.ry2)) {
				gobj.setDrawingParameters(drawParm);
				gobj.setVisualizer(vis);
				gobj.setBackgroundVisualizer(bkgVis);
				gobj.drawDiagram(g, mc);
			}
		}
	}

	@Override
	public void drawLabels(Graphics g, MapContext mc) {
		return; //no labels for grid cells!
	}

	/**
	 * Returns the x-coordinates of the vertical grid lines
	 */
	public float[] getColXCoords() {
		return colXCoord;
	}

	/**
	 * Returns the y-coordinates of the horizontal grid lines
	 */
	public float[] getRowYCoords() {
		return rowYCoord;
	}
}
