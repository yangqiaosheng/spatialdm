package spade.analysis.tools.clustering;

import java.util.Vector;

import spade.analysis.tools.moves.PointsInCircle;
import spade.lib.util.GeoDistance;
import spade.lib.util.IntArray;
import spade.lib.util.ObjectWithMeasure;
import spade.lib.util.Position3D;
import spade.lib.util.QSortAlgorithm;
import spade.vis.geometry.GeoComp;
import spade.vis.geometry.RealPoint;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Apr 21, 2009
 * Time: 5:30:29 PM
 * Organizes spatial points in groups (clusters) so that the spatial extent
 * of each group fits in a circle with the given maximum radius.
 * Uses a spatial index for effectiveness and better grouping.
 */
public class PointOrganizerSpatialIndex extends PointOrganizer {
	/**
	 * If the coordinates are geographic, these are the maximum horizontal
	 * and vertical radii in degrees of longitude and latitude. Otherwise,
	 * both values are equal to maxRad.
	 */
	public double maxRadX = Double.NaN, maxRadY = Double.NaN;
	/**
	 * The spatial extent of the whole set of points (needs to be set in order
	 * to enable the indexing)
	 */
	public float x1 = Float.NaN, y1 = Float.NaN, x2 = Float.NaN, y2 = Float.NaN;
	/**
	 * The matrix is built according to the spatial extent and the maximum radius
	 * and contains the indexes of the groups of points with the centres fitting
	 * in the corresponding cells of the spatial grid.
	 */
	protected int grIdxs[][] = null;
	/**
	 * The dimensions of the matrix
	 */
	protected int nCols = 0, nRows = 0;
	/**
	 * Additional indexes of groups if more than two group centres fit in
	 * the same grid cell.
	 */
	protected Vector<IntArray> moreIdxs = null;

	/**
	 * Sets the spatial extent of the whole set of points
	 */
	public void setSpatialExtent(float x1, float y1, float x2, float y2) {
		this.x1 = x1;
		this.x2 = x2;
		this.y1 = y1;
		this.y2 = y2;
	}

	public void makeMatrix() {
		if (Double.isNaN(maxRad) || Float.isNaN(x1))
			return;
		if (!geo) {
			maxRadX = maxRadY = maxRad;
		} else {
			float my = (y1 + y2) / 2;
			double width = GeoDistance.geoDist(x1, my, x2, my);
			float mx = (x1 + x2) / 2;
			double height = GeoDistance.geoDist(mx, y1, mx, y2);
			double geoFactorX = width / (x2 - x1);
			double geoFactorY = height / (y2 - y1);
			maxRadX = maxRad / geoFactorX;
			maxRadY = maxRad / geoFactorY;
		}
		nCols = (int) Math.round(Math.ceil((x2 - x1) / maxRadX));
		nRows = (int) Math.round(Math.ceil((y2 - y1) / maxRadY));
		if (nCols < 1) {
			nCols = 1;
		}
		if (nRows < 1) {
			nRows = 1;
		}
		grIdxs = new int[nCols][nRows];
		for (int i = 0; i < nCols; i++) {
			for (int j = 0; j < nRows; j++) {
				grIdxs[i][j] = -1;
			}
		}
	}

	/**
	 * Finds the cluster the given point may belong to.
	 * Returns the two-dimensional index of the cluster in the matrix  or null if not found.
	 */
	public Position3D findPlaceForPoint(RealPoint p) {
		if (p == null)
			return null;
		return findPlaceForPoint(p.x, p.y);
	}

	/**
	 * Finds the cluster the given point may belong to.
	 * Returns the two-dimensional index of the cluster in the matrix  or null if not found.
	 */
	public Position3D findPlaceForPoint(float x, float y) {
		return findPlaceForPoint(x, y, -1);
	}

	/**
	 * Finds the cluster the given point may belong to.
	 * Ignores the group with the specified index.
	 * Returns the two-dimensional index of the cluster in the matrix  or null if not found.
	 */
	public Position3D findPlaceForPoint(float x, float y, int ignoreGroupIdx) {
		if (grIdxs == null || centroids == null || centroids.size() < 1)
			return null;
		int colN = (int) Math.floor((x - x1) / maxRadX), rowN = (int) Math.floor((y - y1) / maxRadY);
		if (colN < 0) {
			colN = 0;
		}
		if (rowN < 0) {
			rowN = 0;
		}
		int row1 = Math.max(0, rowN - 1), row2 = Math.min(rowN + 1, nRows - 1), col1 = Math.max(0, colN - 1), col2 = Math.min(colN + 1, nCols - 1);
		Position3D xyz = null;
		double dist = Double.NaN;
		for (int col = col1; col <= col2; col++) {
			for (int row = row1; row <= row2; row++) {
				int idx = grIdxs[col][row];
				if (idx == -1) {
					continue;
				}
				if (idx >= 0) {
					if (idx == ignoreGroupIdx) {
						continue;
					}
					RealPoint q = centroids.elementAt(idx);
					double d = GeoComp.distance(x, y, q.x, q.y, geo);
					if (d >= maxRad) {
						continue;
					}
					if (Double.isNaN(dist) || dist > d) {
						dist = d;
						if (xyz == null) {
							xyz = new Position3D(col, row, 0);
						} else {
							xyz.x = col;
							xyz.y = row;
							xyz.z = 0;
						}
					}
				} else {
					int mIdx = -idx - 10;
					IntArray idxs = moreIdxs.elementAt(mIdx);
					for (int i = 0; i < idxs.size(); i++) {
						idx = idxs.elementAt(i);
						if (idx == ignoreGroupIdx) {
							continue;
						}
						RealPoint q = centroids.elementAt(idx);
						double d = GeoComp.distance(x, y, q.x, q.y, geo);
						if (d >= maxRad) {
							continue;
						}
						if (Double.isNaN(dist) || dist > d) {
							dist = d;
							if (xyz == null) {
								xyz = new Position3D(col, row, i);
							} else {
								xyz.x = col;
								xyz.y = row;
								xyz.z = i;
							}
						}
					}
				}
			}
		}
		return xyz;
	}

	/**
	 * Finds the closest centroid, irrespective of the distance threshold.
	 * Returns the two-dimensional index of the cluster in the matrix  or null if not found.
	 */
	public Position3D findClosestGroupForPoint(float x, float y) {
		if (grIdxs == null || centroids == null || centroids.size() < 1)
			return null;
		int colN = (int) Math.floor((x - x1) / maxRadX), rowN = (int) Math.floor((y - y1) / maxRadY);
		if (colN < 0) {
			colN = 0;
		}
		if (rowN < 0) {
			rowN = 0;
		}
		int row1 = Math.max(0, rowN - 1), row2 = Math.min(rowN + 1, nRows - 1), col1 = Math.max(0, colN - 1), col2 = Math.min(colN + 1, nCols - 1);
		Position3D xyz = null;
		double dist = Double.NaN;
		for (int col = col1; col <= col2; col++) {
			for (int row = row1; row <= row2; row++) {
				int idx = grIdxs[col][row];
				if (idx == -1) {
					continue;
				}
				if (idx >= 0) {
					RealPoint q = centroids.elementAt(idx);
					double d = GeoComp.distance(x, y, q.x, q.y, geo);
					if (Double.isNaN(dist) || dist > d) {
						dist = d;
						if (xyz == null) {
							xyz = new Position3D(col, row, 0);
						} else {
							xyz.x = col;
							xyz.y = row;
							xyz.z = 0;
						}
					}
				} else {
					int mIdx = -idx - 10;
					IntArray idxs = moreIdxs.elementAt(mIdx);
					for (int i = 0; i < idxs.size(); i++) {
						idx = idxs.elementAt(i);
						RealPoint q = centroids.elementAt(idx);
						double d = GeoComp.distance(x, y, q.x, q.y, geo);
						if (Double.isNaN(dist) || dist > d) {
							dist = d;
							if (xyz == null) {
								xyz = new Position3D(col, row, i);
							} else {
								xyz.x = col;
								xyz.y = row;
								xyz.z = i;
							}
						}
					}
				}
			}
		}
		return xyz;
	}

	/**
	 * Checks if the given point is far from all cluster centres,
	 * i.e. the distance is more than 2*maxRad
	 */
	@Override
	public boolean isFarFromAll(float x, float y) {
		if (grIdxs == null || centroids == null || centroids.size() < 1)
			return true;
		int colN = (int) Math.floor((x - x1) / maxRadX), rowN = (int) Math.floor((y - y1) / maxRadY);
		if (colN < 0) {
			colN = 0;
		}
		if (rowN < 0) {
			rowN = 0;
		}
		int row1 = Math.max(0, rowN - 2), row2 = Math.min(rowN + 2, nRows - 1), col1 = Math.max(0, colN - 2), col2 = Math.min(colN + 2, nCols - 1);
		double mr2 = maxRad * 2.1;
		for (int col = col1; col <= col2; col++) {
			for (int row = row1; row <= row2; row++) {
				int idx = grIdxs[col][row];
				if (idx == -1) {
					continue;
				}
				if (idx >= 0) {
					RealPoint q = centroids.elementAt(idx);
					double d = GeoComp.distance(x, y, q.x, q.y, geo);
					if (d < mr2)
						return false;
				} else {
					int mIdx = -idx - 10;
					IntArray idxs = moreIdxs.elementAt(mIdx);
					for (int i = 0; i < idxs.size(); i++) {
						idx = idxs.elementAt(i);
						RealPoint q = centroids.elementAt(idx);
						double d = GeoComp.distance(x, y, q.x, q.y, geo);
						if (d < mr2)
							return false;
					}
				}
			}
		}
		return true;
	}

	/**
	 * Adds a new point to the collection. Checks if the point fits
	 * in any of the existing groups. If so, adds it to the group and
	 * re-computes the centroid. If not, creates a new group with
	 * the centroid in this point.
	 */
	@Override
	public void addPoint(RealPoint p) {
		if (p == null)
			return;
		if (grIdxs == null) {
			makeMatrix();
		}
		if (grIdxs == null)
			return;
		Position3D xyz = findPlaceForPoint(p);
		if (xyz != null) {
			//attach the point to the cluster
			int grIdx = grIdxs[xyz.x][xyz.y];
			if (grIdx < 0) {
				int mIdx = -grIdx - 10;
				IntArray idxs = moreIdxs.elementAt(mIdx);
				grIdx = idxs.elementAt(xyz.z);
			}
			groups.elementAt(grIdx).addPoint(p);
			RealPoint centre = groups.elementAt(grIdx).getCentre();
			centroids.setElementAt(centre, grIdx);
			int colN = (int) Math.floor((centre.x - x1) / maxRadX), rowN = (int) Math.floor((centre.y - y1) / maxRadY);
			if (colN < 0) {
				colN = 0;
			}
			if (rowN < 0) {
				rowN = 0;
			}
			if (colN != xyz.x || rowN != xyz.y) {
				putGroupInCell(grIdx, colN, rowN);
				if (grIdxs[xyz.x][xyz.y] >= 0) {
					grIdxs[xyz.x][xyz.y] = -1;
				} else {
					int mIdx = -grIdxs[xyz.x][xyz.y] - 10;
					IntArray idxs = moreIdxs.elementAt(mIdx);
					idxs.removeElementAt(xyz.z);
					if (idxs.size() < 1) {
						grIdxs[xyz.x][xyz.y] = -1;
					} else if (idxs.size() == 1) {
						grIdxs[xyz.x][xyz.y] = idxs.elementAt(0);
						idxs.removeAllElements();
					}
				}
			}
		} else {
			//make a new cluster
			if (groups == null) {
				groups = new Vector<PointsInCircle>(100, 100);
			}
			if (centroids == null) {
				centroids = new Vector<RealPoint>(100, 100);
			}
			PointsInCircle gr = new PointsInCircle((float) maxRad, geoFactorX, geoFactorY);
			gr.addPoint(p);
			groups.addElement(gr);
			centroids.addElement(p);
			int grIdx = groups.size() - 1;
			int colN = (int) Math.floor((p.x - x1) / maxRadX), rowN = (int) Math.floor((p.y - y1) / maxRadY);
			if (colN < 0) {
				colN = 0;
			}
			if (rowN < 0) {
				rowN = 0;
			}
			putGroupInCell(grIdx, colN, rowN);
		}
	}

	protected void putGroupInCell(int grIdx, int colN, int rowN) {
		if (colN >= nCols) {
			colN = nCols - 1;
		}
		if (rowN >= nRows) {
			rowN = nRows - 1;
		}
		if (grIdxs[colN][rowN] == -1) {
			grIdxs[colN][rowN] = grIdx;
		} else if (grIdxs[colN][rowN] >= 0) {
			IntArray idxs = new IntArray(5, 5);
			idxs.addElement(grIdxs[colN][rowN]);
			idxs.addElement(grIdx);
			if (moreIdxs == null) {
				moreIdxs = new Vector<IntArray>(10, 10);
			}
			moreIdxs.addElement(idxs);
			grIdxs[colN][rowN] = -10 - (moreIdxs.size() - 1);
		} else {
			int mIdx = -grIdxs[colN][rowN] - 10;
			IntArray idxs = moreIdxs.elementAt(mIdx);
			idxs.addElement(grIdx);
		}
	}

	/**
	 * Redistributes the points so that each point is attached to the closest centre
	 */
	public void reDistributePoints() {
		if (grIdxs == null || groups == null || centroids == null || groups.size() < 2)
			return;
		for (int iter = 0; iter < 2; iter++) {
			for (int i = 0; i < nCols; i++) {
				for (int j = 0; j < nRows; j++) {
					grIdxs[i][j] = -1;
				}
			}
			if (moreIdxs != null) {
				moreIdxs.removeAllElements();
			}
			Vector<PointsInCircle> oldGroups = groups;
			groups = new Vector<PointsInCircle>(oldGroups.size(), 100);
			for (int i = 0; i < centroids.size(); i++) {
				RealPoint p = centroids.elementAt(i);
				int colN = (int) Math.floor((p.x - x1) / maxRadX), rowN = (int) Math.floor((p.y - y1) / maxRadY);
				if (colN < 0) {
					colN = 0;
				}
				if (rowN < 0) {
					rowN = 0;
				}
				putGroupInCell(i, colN, rowN);
				PointsInCircle gr = new PointsInCircle((float) maxRad, geoFactorX, geoFactorY);
				groups.addElement(gr);
			}
			for (int i = 0; i < oldGroups.size(); i++) {
				PointsInCircle gr = oldGroups.elementAt(i);
				for (int j = 0; j < gr.getPointCount(); j++) {
					RealPoint p = gr.getPoint(j);
					Position3D xyz = findClosestGroupForPoint(p.x, p.y);
					if (xyz != null) {
						//attach the point to the cluster
						int grIdx = grIdxs[xyz.x][xyz.y];
						if (grIdx < 0) {
							int mIdx = -grIdx - 10;
							IntArray idxs = moreIdxs.elementAt(mIdx);
							grIdx = idxs.elementAt(xyz.z);
						}
						groups.elementAt(grIdx).addPoint(p);
					}
				}
			}
			if (iter == 0) {
				for (int i = 0; i < nCols; i++) {
					for (int j = 0; j < nRows; j++) {
						grIdxs[i][j] = -1;
					}
				}
				if (moreIdxs != null) {
					moreIdxs.removeAllElements();
				}
				centroids.removeAllElements();
				for (int i = 0; i < groups.size(); i++) {
					PointsInCircle gr = groups.elementAt(i);
					RealPoint centre = gr.getCentre();
					centroids.addElement(centre);
					int colN = (int) Math.floor((centre.x - x1) / maxRadX), rowN = (int) Math.floor((centre.y - y1) / maxRadY);
					if (colN < 0) {
						colN = 0;
					}
					if (rowN < 0) {
						rowN = 0;
					}
					putGroupInCell(i, colN, rowN);
				}
			}
		}
	}

	@Override
	public void mergeCloseGroups() {
		Vector<PointsInCircle> newGroups = new Vector<PointsInCircle>(groups.size(), 100);
		Vector<RealPoint> newCentroids = new Vector<RealPoint>(groups.size(), 100);
		boolean merged[] = new boolean[groups.size()];
		for (int i = 0; i < centroids.size(); i++)
			if (!merged[i]) {
				RealPoint p = centroids.elementAt(i);
				Position3D xyz = findPlaceForPoint(p.x, p.y, i);
				if (xyz == null) {
					continue;
				}
				int grIdx = grIdxs[xyz.x][xyz.y];
				if (grIdx < 0) {
					int mIdx = -grIdx - 10;
					IntArray idxs = moreIdxs.elementAt(mIdx);
					grIdx = idxs.elementAt(xyz.z);
				}
				if (grIdx == i) {
					continue;
				}
				//merge two groups
				PointsInCircle mGroup = new PointsInCircle((float) maxRad, geoFactorX, geoFactorY);
				PointsInCircle gr1 = groups.elementAt(i);
				PointsInCircle gr2 = groups.elementAt(grIdx);
				for (int k = 0; k < gr1.getPointCount(); k++) {
					mGroup.addPoint(gr1.getPoint(k));
				}
				for (int k = 0; k < gr2.getPointCount(); k++) {
					mGroup.addPoint(gr2.getPoint(k));
				}
				double rad = mGroup.getRadius();
				if (rad > maxRad * 1.1) {
					continue;
				}
				newGroups.addElement(mGroup);
				newCentroids.addElement(mGroup.getCentre());
				merged[i] = true;
				merged[grIdx] = true;
			}
		if (newGroups.size() < 1) {
			reDistributePoints();
			return;
		}
		for (int i = 0; i < groups.size(); i++)
			if (!merged[i]) {
				newGroups.addElement(groups.elementAt(i));
				newCentroids.addElement(centroids.elementAt(i));
			}
		groups = newGroups;
		centroids = newCentroids;
		reDistributePoints();
	}

	/**
	 * Optimizes the distribution of the points: tries to re-group the points
	 * around the medoids of the biggest groups
	 */
	public void optimizeGrouping() {
		if (groups == null || groups.size() < 2)
			return;
		Vector<ObjectWithMeasure> grOrd = new Vector<ObjectWithMeasure>(groups.size(), 1);
		double aver = 0;
		int count = 0;
/**/
		for (int i = 0; i < groups.size(); i++) {
			PointsInCircle gr = groups.elementAt(i);
			double dens = 0;
			if (gr.getPointCount() >= 3) {
				double rad = gr.getMeanDistToMedian();
				if (rad > 0) {
					dens = gr.getPointCount() / (rad * rad);
				}
				aver += dens;
				++count;
			}
			grOrd.addElement(new ObjectWithMeasure(new Integer(i), dens, true));
		}
/*
    for (int i=0; i<groups.size(); i++) {
      PointsInCircle gr=groups.elementAt(i);
      double size =gr.getPointCount();
      grOrd.addElement(new ObjectWithMeasure(new Integer(i),size,true));
      aver +=size;
    }
/**/
		aver /= count;
		QSortAlgorithm.sort(grOrd);
		int mIdx = Math.round(grOrd.size() / 2f) + 1;
		//int mIdx=Math.round(grOrd.size()*0.75f)+1;
		if (mIdx >= grOrd.size()) {
			mIdx = grOrd.size() - 1;
		}
		double median = Math.min(grOrd.elementAt(mIdx).measure, aver);

		Vector<PointsInCircle> oldGroups = groups;
		Vector<RealPoint> oldCentroids = centroids;
		for (int i = 0; i < nCols; i++) {
			for (int j = 0; j < nRows; j++) {
				grIdxs[i][j] = -1;
			}
		}
		if (moreIdxs != null) {
			moreIdxs.removeAllElements();
		}
		groups = null;
		centroids = null;
		//for (int i=0; i<grOrd.size() && grOrd.elementAt(i).measure>=aver; i++)  {
		for (int i = 0; i < grOrd.size() && grOrd.elementAt(i).measure >= median; i++) {
			int idx = ((Integer) grOrd.elementAt(i).obj).intValue();
			addPoint(oldGroups.elementAt(idx).getQuasiMedoid());
		}
		//add all points
		if (groups != null) {
			for (int i = 0; i < groups.size(); i++) {
				groups.elementAt(i).removeAllPoints(); //to avoid duplication of points: some of them were added!
			}
		}
		for (int i = 0; i < grOrd.size(); i++) {
			int idx = ((Integer) grOrd.elementAt(i).obj).intValue();
			PointsInCircle group = oldGroups.elementAt(idx);
			for (int j = 0; j < group.getPointCount(); j++) {
				addPoint(group.getPoint(j));
			}
		}

		reDistributePoints();
	}

}
