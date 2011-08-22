package spade.analysis.tools.clustering;

import java.util.Vector;

import spade.vis.database.DataTable;
import spade.vis.database.ObjectContainer;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Jan 6, 2009
 * Time: 4:41:11 PM
 * Contains information about clusters, which can be used for classification
 * of objects from a database.
 */
public class ClustersInfo {
	/**
	 * The table where the clustering results are stored
	 */
	public DataTable table = null;
	/**
	 * The index of the table column with the cluster numbers
	 */
	public int clustersColN = -1;
	/**
	 * The container of the objects, to which the clustering has been applied.
	 */
	public ObjectContainer objContainer = null;
	/**
	 * Measures distances between objects
	 */
	public DistanceMeterExt<DClusterObject> distanceMeter = null;
	/**
	 * The information about the clusters, which can be used for classification
	 * of other compatible objects
	 */
	public Vector<SingleClusterInfo> clusterInfos = null;

	@Override
	public Object clone() {
		ClustersInfo clInfo = new ClustersInfo();
		clInfo.table = this.table;
		clInfo.clustersColN = this.clustersColN;
		clInfo.objContainer = this.objContainer;
		clInfo.distanceMeter = this.distanceMeter;
		if (clusterInfos != null) {
			clInfo.init(Math.max(clusterInfos.size(), 10), 10);
			for (int i = 0; i < clusterInfos.size(); i++) {
				clInfo.addSingleClusterInfo((SingleClusterInfo) clusterInfos.elementAt(i).clone());
			}
		}
		return clInfo;
	}

	public void init(int capacity, int increment) {
		clusterInfos = new Vector<SingleClusterInfo>(capacity, increment);
	}

	public int getClustersCount() {
		if (clusterInfos == null)
			return 0;
		return clusterInfos.size();
	}

	public SingleClusterInfo getSingleClusterInfo(int idx) {
		if (clusterInfos == null || idx < 0 || idx >= clusterInfos.size())
			return null;
		return clusterInfos.elementAt(idx);
	}

	public void addSingleClusterInfo(SingleClusterInfo clin) {
		if (clin == null)
			return;
		if (clusterInfos == null) {
			init(50, 50);
			clusterInfos.addElement(clin);
			return;
		}
		int idx = getClusterIndex(clin.clusterN);
		if (idx >= 0) {
			clusterInfos.removeElementAt(idx);
		}
		clusterInfos.addElement(clin);
	}

	/**
	 * Finds the index of the cluster with the given number.
	 * Returns -1 if not found.
	 */
	public int getClusterIndex(int clusterN) {
		if (clusterInfos == null || clusterInfos.size() < 1)
			return -1;
		for (int i = 0; i < clusterInfos.size(); i++)
			if (clusterInfos.elementAt(i).clusterN == clusterN)
				return i;
		return -1;
	}

	/**
	 * Removes the cluster information with the given index
	 */
	public void removeClusterInfo(int idx) {
		if (clusterInfos == null || idx < 0 || idx >= clusterInfos.size())
			return;
		clusterInfos.removeElementAt(idx);
	}

	/**
	 * Removes the information about the cluster with the given number.
	 */
	public void removeInfoAboutClusterN(int clusterN) {
		int idx = getClusterIndex(clusterN);
		if (idx >= 0) {
			clusterInfos.removeElementAt(idx);
		}
	}

	/**
	 * Removes the information about all clusters defined so far
	 */
	public void clear() {
		if (clusterInfos != null) {
			clusterInfos.removeAllElements();
		}
	}
}
