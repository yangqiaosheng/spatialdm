package spade.analysis.tools.somlink;

import java.awt.Point;
import java.util.Vector;

import useSOM.SOMCellInfo;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Nov 3, 2009
 * Time: 2:43:59 PM
 * Joins neighbouring SOM cells according to the given distance threshold
 */
public class SOMCellsJoiner {
	/**
	 * @param cellInfos - the descriptors of the cells to join
	 * @param distanceThr - the distance threshold
	 * @param nNbs - how many neighbours are considered: either 4 or 8
	 * @return a vector of groups where each group is a vector of points
	 *   (instances of java.awt.Point) so that x- and y-coordinates
	 *   correspond to the positions of the cells in the matrix
	 */
	public static Vector<Vector> joinCells(SOMCellInfo cellInfos[][], double distanceThr, int nNbs) {
		if (cellInfos == null || cellInfos.length < 1 || cellInfos[0].length < 1)
			return null;
		int xdim = cellInfos.length, ydim = cellInfos[0].length;
		boolean processed[][] = new boolean[xdim][ydim];
		for (int x = 0; x < xdim; x++) {
			for (int y = 0; y < ydim; y++) {
				processed[x][y] = false;
			}
		}
		Vector<Vector> groups = new Vector<Vector>(20, 20);
		for (int x = 0; x < xdim; x++) {
			for (int y = 0; y < ydim; y++)
				if (!processed[x][y] && cellInfos[x][y].nObj > 0) {
					Vector<Point> group = new Vector<Point>(10, 10);
					group.addElement(new Point(x, y));
					processed[x][y] = true;
					for (int k = 0; k < group.size(); k++) {
						int x0 = group.elementAt(k).x, y0 = group.elementAt(k).y;
						SOMCellInfo sci = cellInfos[x0][y0];
						if (sci.distances == null) {
							continue;
						}
						// N, W, S, E, NW, NE, SW, SE (compass directions)
						for (int i = 0; i < Math.min(nNbs, sci.distances.length); i++)
							if (!Double.isNaN(sci.distances[i]) && sci.distances[i] < distanceThr) {
								Point pt = null;
								switch (i) {
								case 0: //N
									if (y0 > 0) {
										pt = new Point(x0, y0 - 1);
									}
									break;
								case 1: //W
									if (x0 > 0) {
										pt = new Point(x0 - 1, y0);
									}
									break;
								case 2: //S
									if (y0 < ydim - 1) {
										pt = new Point(x0, y0 + 1);
									}
									break;
								case 3: //E
									if (x0 < xdim - 1) {
										pt = new Point(x0 + 1, y0);
									}
									break;
								case 4: //NW
									if (y0 > 0 && x0 > 0) {
										pt = new Point(x0 - 1, y0 - 1);
									}
									break;
								case 5: //NE
									if (y0 > 0 && x0 < xdim - 1) {
										pt = new Point(x0 + 1, y0 - 1);
									}
									break;
								case 6: //SW
									if (y0 < ydim - 1 && x0 > 0) {
										pt = new Point(x0 - 1, y0 + 1);
									}
									break;
								case 7: //SE
									if (y0 < ydim - 1 && x0 < xdim - 1) {
										pt = new Point(x0 + 1, y0 + 1);
									}
									break;
								}
								if (pt == null) {
									continue;
								}
								if (processed[pt.x][pt.y]) {
									continue;
								}
								if (cellInfos[pt.x][pt.y].nObj < 1) {
									continue;
								}
								group.addElement(pt);
								processed[pt.x][pt.y] = true;
							}
					}
					int pos = -1;
					for (int i = 0; i < groups.size() && pos < 0; i++)
						if (group.size() > groups.elementAt(i).size()) {
							pos = i;
						}
					if (pos < 0) {
						groups.addElement(group);
					} else {
						groups.insertElementAt(group, pos);
					}
				}
		}
		if (groups.size() < 1 || groups.size() >= xdim * ydim)
			return null;
		return groups;
	}
}
