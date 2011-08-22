package spade.analysis.tools.clustering;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Jan 6, 2009
 * Time: 5:02:31 PM
 * Describes a representative object of a cluster, which may be used for
 * the assignment of cluster membership to other objects
 */
public class ClusterSpecimenInfo {
	public DClusterObject specimen = null;
	/**
	 * The distance threshold: If the distance from an object to the
	 * specimen is less or equal, the object is treated as being similar
	 * to the specimen
	 */
	public double distanceThr = 0;
	/**
	 * Keeps the original distance threshold to enable restoration
	 * when the user has changed the distance threshold but wants
	 * to reset the original value.
	 */
	public double origDistanceThr = 0;
	/**
	 * The original number of objects similar to the specimen
	 */
	public int nSimilarOrig = 0;
	/**
	 * The mean distance of the original similar objects to the specimen
	 */
	public double meanDistOrig = 0;
	/**
	 * The number of new objects, which have been found similar to the specimen
	 */
	public int nSimilarNew = 0;
	/**
	 * The mean distance of the new similar objects to the specimen
	 */
	public double meanDistNew = 0;

	@Override
	public Object clone() {
		ClusterSpecimenInfo csi = new ClusterSpecimenInfo();
		csi.specimen = this.specimen;
		csi.distanceThr = this.distanceThr;
		csi.origDistanceThr = this.origDistanceThr;
		csi.nSimilarOrig = this.nSimilarOrig;
		csi.nSimilarNew = this.nSimilarNew;
		csi.meanDistOrig = this.meanDistOrig;
		csi.meanDistNew = this.meanDistNew;
		return csi;
	}
}
