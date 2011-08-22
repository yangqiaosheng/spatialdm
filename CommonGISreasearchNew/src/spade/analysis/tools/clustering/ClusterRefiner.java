package spade.analysis.tools.clustering;

import spade.vis.database.DataTable;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Jan 2, 2009
 * Time: 5:38:27 PM
 * An interface for a class which can refine clusters produced by OPTICS
 * by means of further clustering (e.g. using KMedoids algorithm).
 */
public interface ClusterRefiner {
	/**
	 * Refines currently selected OPTICS-generated clusters
	 * @param lClusterer - contains the results of the clustering by OPTICS
	 * @param table - the table with the clusters
	 * @param origClusterColN - the index of the table column containing the original
	 *   assignment of the objects to the clusters
	 * @param refinedClusterColN - the index of the table column containing the
	 *   assignment of the objects to the refined clusters, if this column has been 
	 *   previously created
	 * @return the index of the table column containing the assignment of the objects to the
	 *   refined clusters. If this column did not exist before, the ClusterRefiner creates it.
	 */
	public int refineClusters(LayerClusterer lClusterer, DataTable table, int origClusterColN, int refinedClusterColN);
}
