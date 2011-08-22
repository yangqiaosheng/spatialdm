package spade.analysis.tools.clustering;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Jan 7, 2009
 * Time: 2:06:45 PM
 * Contains information about the assignment of an object to a cluster.
 */
public class ObjectToClusterAssignment {
	/**
	 * The identifier of the object
	 */
	public String id = null;
	/**
	 * The number of the cluster (starting from 1); -1 means not assigned
	 */
	public int clusterN = -1;
	/**
	 * The index of the cluster representative this object is similar to
	 * (starting from 1); -1 means no similar representative
	 */
	public int specimenIdx = -1;
	/**
	 * The distance to the specimen
	 */
	public double distance = Double.NaN;
}
