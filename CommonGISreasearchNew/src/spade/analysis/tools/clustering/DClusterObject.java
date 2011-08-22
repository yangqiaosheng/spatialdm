package spade.analysis.tools.clustering;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Dec 30, 2008
 * Time: 5:49:25 PM
 * Represents an object participating in clustering.
 * Contains a reference to the original object and additional information,
 * such as the index of the object in the container, the results of the clustering,
 * etc.
 */
public class DClusterObject {
	/**
	 * The original object participating in clustering
	 */
	public Object originalObject = null;
	/**
	 * The index of the object in the container
	 */
	public int idx = -1;
	/**
	 * The identifier of the object
	 */
	public String id = null;
	/**
	 * The numeric identifier (if exists); may be -1!
	 */
	public int numId = -1;
	/**
	 * The reachability and core distances computed by OPTICS
	 */
	public double reachabilityDistance = Double.NaN, coreDistance = Double.NaN;
	/**
	 * The index of the cluster assigned to this object; -1 means "noise"
	 */
	public int clusterIdx = -1;
	/**
	 * Indicates whether this object is a cluster specimen (representative),
	 * i.e. a centre (medoid) of one of its subclusters
	 */
	public boolean isSpecimen = false;
	/**
	 * The index of the subcluster within the cluster (i.e. the index of the closest
	 * cluster specimen)
	 */
	public int subIdx = -1;
	/**
	 * The identifier of the closest cluster specimen, i.e. the centre of the subcluster
	 */
	public String specimenId = null;

	public DClusterObject(Object origObj, String objId, int objIdx) {
		originalObject = origObj;
		id = objId;
		idx = objIdx;
	}

	@Override
	public Object clone() {
		DClusterObject clobj = new DClusterObject(originalObject, id, idx);
		clobj.numId = this.numId;
		clobj.reachabilityDistance = this.reachabilityDistance;
		clobj.coreDistance = this.coreDistance;
		clobj.clusterIdx = this.clusterIdx;
		clobj.subIdx = this.subIdx;
		clobj.isSpecimen = this.isSpecimen;
		clobj.specimenId = this.specimenId;
		return clobj;
	}
}
