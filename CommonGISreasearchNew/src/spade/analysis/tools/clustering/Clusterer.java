package spade.analysis.tools.clustering;

import java.util.Vector;

import spade.vis.database.DataTable;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Aug 5, 2009
 * Time: 12:06:01 PM
 * A common interface implemented by LayerClusterer and TableClusterer.
 */
public interface Clusterer {
	/**
	 * Returns the distance threshold used for the clustering
	 */
	public double getDistanceThreshold();

	/**
	 * Returns the required minimum number of objects in the neighbourhood of a core object
	 */
	public int getMinNeighbours();

	/**
	 * Returns the clustered objects in the order produced by OPTICS (or another algorithm)
	 */
	public Vector<DClusterObject> getObjectsOrdered();

	/**
	 * Returns the result of clustering as DataTable
	 */
	public DataTable getResult();
}
