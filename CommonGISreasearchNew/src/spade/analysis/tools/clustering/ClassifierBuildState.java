package spade.analysis.tools.clustering;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: 19-Jan-2009
 * Time: 14:42:16
 * Stores a state in the process of building a classifier.
 * Enables "undo" operations.
 */
public class ClassifierBuildState {
	/**
	 * Contains an assignment of the objects to clusters and their subclusters
	 */
	public DClusterObject clusteredObjects[] = null;
	/**
	 * Contains information about the clusters.
	 */
	public ClustersInfo clustersInfo = null;
	/**
	 * The number of objects in the layer containing cluster specimens
	 */
	public int nObjInSpecimensLayer = 0;
	/**
	 * The number of records in the table describing the cluster specimens
	 * (it should be equal to nObjInSpecimenLayer, but just in case...)
	 */
	public int nRecInSpecimensTable = 0;
}