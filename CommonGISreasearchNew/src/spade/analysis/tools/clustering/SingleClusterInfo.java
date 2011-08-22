package spade.analysis.tools.clustering;

import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Jan 6, 2009
 * Time: 4:51:38 PM
 * Contains information about a cluster, which can be used for classification
 * of objects from a database: whether they may belong to this cluster.
 * A cluster is defined by one or more representative objects (specimens) and
 * corresponding distance thresholds (the thresholds may be different).
 */
public class SingleClusterInfo {
	/**
	 * The number of the cluster (starting from 1)
	 */
	public int clusterN = -1;
	public String clusterLabel = null;
	public int origSize = 0;
	public Vector<ClusterSpecimenInfo> specimens = null;

	@Override
	public Object clone() {
		SingleClusterInfo clIn = new SingleClusterInfo();
		clIn.clusterN = this.clusterN;
		clIn.clusterLabel = this.clusterLabel;
		clIn.origSize = this.origSize;
		if (specimens != null) {
			clIn.init(Math.max(specimens.size(), 10), 10);
			for (int i = 0; i < specimens.size(); i++) {
				clIn.addSpecimen((ClusterSpecimenInfo) specimens.elementAt(i).clone());
			}
		}
		return clIn;
	}

	public void init(int capacity, int increment) {
		specimens = new Vector<ClusterSpecimenInfo>(capacity, increment);
	}

	public int getSpecimensCount() {
		if (specimens == null)
			return 0;
		return specimens.size();
	}

	/**
	 * Adds a new specimen of this cluster
	 * @param specimen - the specimen
	 * @param distanceThreshold - the distance threshold:
	 *     If the distance from an object to the specimen is less or equal,
	 *     the object is treated as being similar to the specimen
	 * @param nSimilarObjects - the original number of objects similar to the specimen
	 * @param meanDistance - the mean distance of the similar objects to the specimen
	 */
	public void addSpecimen(DClusterObject specimen, double distanceThreshold, int nSimilarObjects, double meanDistance) {
		if (specimen == null || Double.isNaN(distanceThreshold))
			return;
		ClusterSpecimenInfo csp = new ClusterSpecimenInfo();
		csp.specimen = specimen;
		csp.distanceThr = distanceThreshold;
		csp.origDistanceThr = csp.distanceThr;
		csp.nSimilarOrig = nSimilarObjects;
		csp.meanDistOrig = meanDistance;
		addSpecimen(csp);
	}

	/**
	 * Adds a new specimen of this cluster
	 * @param specimenInfo - describes the specimen
	 */
	public void addSpecimen(ClusterSpecimenInfo specimenInfo) {
		if (specimenInfo == null)
			return;
		if (specimens == null) {
			init(20, 10);
		}
		specimens.addElement(specimenInfo);
	}

	/**
	 * Returns the information about the cluster specimen with the given index 
	 */
	public ClusterSpecimenInfo getClusterSpecimenInfo(int idx) {
		if (idx < 0 || idx >= getSpecimensCount())
			return null;
		return specimens.elementAt(idx);
	}
}
