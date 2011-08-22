package spade.vis.geometry;

import java.util.Vector;

import spade.lib.util.FloatArray;
import spade.lib.util.IntArray;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: 31-Jan-2008
 * Time: 15:27:19
 * Keeps a collection of circles and efficiently finds circles
 * containing a given point (by means of indexing)
 */
public class CircleCollector {
	/**
	 * The collection of circles
	 */
	public Vector circles = null;
	/**
	 * Indexes of the circles ordered according to their minimum x-coordinates
	 */
	protected IntArray idxOrdX = null;
	/**
	 * The minimum and maximum x-coordinates of the circles in the order specified by orderX
	 */
	protected FloatArray xMinX = null, xMaxX = null;
	/**
	 * Indexes of the circles ordered according to their minimum y-coordinates
	 */
	protected IntArray idxOrdY = null;
	/**
	 * The minimum and maximum y-coordinates of the circles  in the order specified by orderY
	 */
	protected FloatArray yMinY = null, yMaxY = null;
	/**
	 * The maximum circle diameter
	 */
	protected float maxDiam = 0;
	/**
	 * Order of the circles according to their minimum X- and Y-coordinates, respectively
	 */
	protected int cOrdX[] = null, cOrdY[] = null;

	/**
	 * Allocates initial memory for the given expected number of circles
	 */
	public void allocate(int nCircles, int incr) {
		circles = new Vector(nCircles, incr);
		idxOrdX = new IntArray(nCircles, incr);
		idxOrdY = new IntArray(nCircles, incr);
		xMinX = new FloatArray(nCircles, incr);
		xMaxX = new FloatArray(nCircles, incr);
		yMinY = new FloatArray(nCircles, incr);
		yMaxY = new FloatArray(nCircles, incr);
	}

	/**
	 * Adds the given circle to its collection and to the indexes.
	 * Returns its index in the vector.
	 */
	public int addCircle(RealCircle cir) {
		if (cir == null || Float.isNaN(cir.cx) || Float.isNaN(cir.cy) || Float.isNaN(cir.rad))
			return -1;
		if (circles == null) {
			allocate(100, 100);
		}
		circles.addElement(cir);
		cOrdY = null;
		int cIdx = circles.size() - 1;
		float diam = 2 * cir.rad, minX = cir.cx - cir.rad, maxX = minX + diam, minY = cir.cy - cir.rad, maxY = minY + diam;
		if (diam > maxDiam) {
			maxDiam = diam;
		}
		int iIns = -1;
		for (int i = 0; i < xMinX.size() && iIns < 0; i++)
			if (minX < xMinX.elementAt(i)) {
				iIns = i;
			} else if (minX == xMinX.elementAt(i) && maxX < xMaxX.elementAt(i)) {
				iIns = i;
			}
		if (iIns < 0) {
			idxOrdX.addElement(cIdx);
			xMinX.addElement(minX);
			xMaxX.addElement(maxX);
		} else {
			idxOrdX.insertElementAt(cIdx, iIns);
			xMinX.insertElementAt(minX, iIns);
			xMaxX.insertElementAt(maxX, iIns);
		}
		iIns = -1;
		for (int i = 0; i < yMinY.size() && iIns < 0; i++)
			if (minY < yMinY.elementAt(i)) {
				iIns = i;
			} else if (minY == yMinY.elementAt(i) && maxY < yMaxY.elementAt(i)) {
				iIns = i;
			}
		if (iIns < 0) {
			idxOrdY.addElement(cIdx);
			yMinY.addElement(minY);
			yMaxY.addElement(maxY);
		} else {
			idxOrdY.insertElementAt(cIdx, iIns);
			yMinY.insertElementAt(minY, iIns);
			yMaxY.insertElementAt(maxY, iIns);
		}
		return circles.size() - 1;
	}

	/**
	 * Finalises the indexes
	 */
	public void setupIndex() {
		if (cOrdY != null)
			return;
		if (circles == null || circles.size() < 1)
			return;
		cOrdY = new int[circles.size()];
		cOrdX = new int[circles.size()];
		for (int i = 0; i < cOrdY.length; i++) {
			cOrdY[idxOrdY.elementAt(i)] = i;
			cOrdX[idxOrdX.elementAt(i)] = i;
		}
	}

	/**
	 * For the point with given x- and y-coordinates, finds a containing circle and
	 * returns its index in the vector.
	 */
	public int getContainingCircleIndex(float x, float y) {
		if (maxDiam <= 0)
			return -1;
		if (cOrdY == null) {
			setupIndex();
		}
		int idxX[] = findRange(x, maxDiam, xMinX), idxY[] = findRange(y, maxDiam, yMinY);
		for (int i = idxX[0]; i <= idxX[1]; i++) {
			int cIdx = idxOrdX.elementAt(i), yIdx = cOrdY[cIdx];
			if (yIdx < idxY[0] || yIdx > idxY[1]) {
				continue;
			}
			if (x > xMaxX.elementAt(i) || y > yMaxY.elementAt(yIdx)) {
				continue;
			}
			RealCircle cir = (RealCircle) circles.elementAt(cIdx);
			if (cir.contains(x, y, 0))
				return cIdx;
		}
		return -1;
	}

	/**
	 * For the point with given x- and y-coordinates, finds a containing circle and
	 * returns this circle.
	 */
	public RealCircle getContainingCircle(float x, float y) {
		int cIdx = getContainingCircleIndex(x, y);
		if (cIdx < 0)
			return null;
		return (RealCircle) circles.elementAt(cIdx);
	}

	/**
	 * Returns the indexes of the circles situated in space between the two circles
	 * with the given indexes (i.e. the centres are between the boundaries of the given
	 * two circles).
	 */
	public int[] getCirclesBetween(int cIdx1, int cIdx2) {
		if (cIdx1 == cIdx2 || cIdx1 < 0 || cIdx2 < 0 || cIdx1 >= circles.size() || cIdx2 >= circles.size())
			return null;
		RealCircle cir1 = (RealCircle) circles.elementAt(cIdx1), cir2 = (RealCircle) circles.elementAt(cIdx2);
		float x1 = Math.min(cir1.cx - cir1.rad, cir2.cx - cir2.rad), x2 = Math.max(cir1.cx + cir1.rad, cir2.cx + cir2.rad);
		int ix1[] = findRange(x1, maxDiam, xMinX), ix2[] = findRange(x2, maxDiam, xMinX);
		float y1 = Math.min(cir1.cy - cir1.rad, cir2.cy - cir2.rad), y2 = Math.max(cir1.cy + cir1.rad, cir2.cy + cir2.rad);
		int iy1[] = findRange(y1, maxDiam, yMinY), iy2[] = findRange(y2, maxDiam, yMinY);
		IntArray idxs = new IntArray(20, 10);
		for (int i = ix1[0]; i <= ix2[1]; i++) {
			int cIdx = idxOrdX.elementAt(i), yIdx = cOrdY[cIdx];
			if (cIdx == cIdx1 || cIdx == cIdx2) {
				continue;
			}
			if (yIdx < iy1[0] || yIdx > iy2[1]) {
				continue;
			}
			RealCircle cir = (RealCircle) circles.elementAt(cIdx);
			if (cir.cx >= x1 && cir.cx <= x2 && cir.cy >= y1 && cir.cy <= y2) {
				idxs.addElement(cIdx);
			}
		}
		if (idxs.size() < 1)
			return null;
		return idxs.getTrimmedArray();
	}

	protected static int[] findRange(float val, float maxDiam, FloatArray minCoords) {
		int minIdx = 0, maxIdx = minCoords.size() - 1;
		while (minIdx + 1 < maxIdx) {
			int idx = (minIdx + maxIdx) / 2;
			float v0 = val - maxDiam;
			if (val < minCoords.elementAt(idx)) {
				maxIdx = idx;
			} else if (v0 > minCoords.elementAt(idx)) {
				minIdx = idx;
			} else {
				for (int i = idx + 1; i < maxIdx; i++)
					if (val < minCoords.elementAt(i)) {
						maxIdx = i;
						break;
					}
				for (int i = idx - 1; i > minIdx; i--)
					if (v0 > minCoords.elementAt(i)) {
						minIdx = i;
						break;
					}
				break;
			}
		}
		int idxs[] = { minIdx, maxIdx };
		return idxs;
	}
}
