package geo;

import java.io.Serializable;

import edu.wlu.cs.levy.CG.GeoDistance;

import weka.clusterers.forOPTICSAndDBScan.DataObjects.DataObject;
import weka.clusterers.forOPTICSAndDBScan.Databases.Database;
import weka.core.Instance;
import weka.core.RevisionHandler;
import weka.core.RevisionUtils;

/**
 *  @author Haolin Zhi (zhi@cs.uni-bonn.de)
 *  @author Peca Iulian (pkiulian@gmail.com)
 *  @version $Revision: 1.5 $
 */
public class GeoDataObject implements DataObject, RevisionHandler, Serializable {

	/** for serialization */
	private static final long serialVersionUID = -3417720553766544582L;

	/**
	 * Holds the original instance
	 */
	private Instance instance;

	/**
	 * Holds the (unique) key that is associated with this DataObject
	 */
	private String key;

	/**
	 * Holds the ID of the cluster, to which this DataObject is assigned
	 */
	private int clusterID;

	/**
	 * Holds the status for this DataObject (true, if it has been processed,else false)
	 */
	private boolean processed;

	/**
	 * Holds the coreDistance for this DataObject
	 */
	private double c_dist;

	/**
	 * Holds the reachabilityDistance for this DataObject
	 */
	private double r_dist;

	/**
	 * Holds the database, that is the keeper of this DataObject
	 */
	private Database database;

	/**
	 * Constructs a new DataObject. The original instance is kept as instance-variable
	 * 
	 * @param originalInstance
	 *            the original instance
	 */
	public GeoDataObject(Instance originalInstance, String key, Database database) {
		this.database = database;
		this.key = key;
		instance = originalInstance;
		clusterID = DataObject.UNCLASSIFIED;
		processed = false;
		c_dist = DataObject.UNDEFINED;
		r_dist = DataObject.UNDEFINED;
	}

	/**
	 * Compares two DataObjects in respect to their attribute-values
	 * 
	 * @param dataObject The DataObject, that is compared with this.dataObject
	 * @return Returns true, if the DataObjects correspond in each value, else returns false
	 */
	@Override
	public boolean equals(DataObject dataObject) {
		if (this == dataObject)
			return true;
		if (!(dataObject instanceof GeoDataObject))
			return false;

		final GeoDataObject geoDataObject = (GeoDataObject) dataObject;

		if (getInstance().equalHeaders(geoDataObject.getInstance())) {
			for (int i = 0; i < getInstance().numValues(); i++) {
				double i_value_Instance_1 = getInstance().valueSparse(i);
				double i_value_Instance_2 = geoDataObject.getInstance().valueSparse(i);

				if (i_value_Instance_1 != i_value_Instance_2)
					return false;
			}
			return true;
		}
		return false;
	}

	/**
	 * Calculates the geo-distance between dataObject and this.dataObject
	 * 
	 * @param dataObject The DataObject, that is used for distance-calculation with this.dataObject
	 * @return double-value The geo-distance between dataObject and this.dataObject NaN, if the computation could not be performed
	 */
	@Override
	public double distance(DataObject dataObject) {
		double dist = 0.0;
		if (!(dataObject instanceof GeoDataObject))
			return Double.NaN;

		if (getInstance().equalHeaders(dataObject.getInstance())) {
			dist = GeoDistance.geoDist(this.getInstance().value(2), this.getInstance().value(3), dataObject.getInstance().value(2), dataObject.getInstance().value(3));
			return dist;
		}
		return Double.NaN;
	}

	/**
	 * Returns the original instance
	 * 
	 * @return originalInstance
	 */
	public Instance getInstance() {
		return instance;
	}

	/**
	 * Returns the key for this DataObject
	 * 
	 * @return key
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Sets the key for this DataObject
	 * 
	 * @param key
	 *            The key is represented as string
	 */
	public void setKey(String key) {
		this.key = key;
	}

	/**
	 * Sets the clusterID (cluster), to which this DataObject belongs to
	 * 
	 * @param clusterID Number of the Cluster
	 */
	public void setClusterLabel(int clusterID) {
		this.clusterID = clusterID;
	}

	/**
	 * Returns the clusterID, to which this DataObject belongs to
	 * 
	 * @return clusterID
	 */
	public int getClusterLabel() {
		return clusterID;
	}

	/**
	 * Marks this dataObject as processed
	 * 
	 * @param processed True, if the DataObject has been already processed, false else
	 */
	public void setProcessed(boolean processed) {
		this.processed = processed;
	}

	/**
	 * Gives information about the status of a dataObject
	 * 
	 * @return True, if this dataObject has been processed, else false
	 */
	public boolean isProcessed() {
		return processed;
	}

	/**
	 * Sets a new coreDistance for this dataObject
	 * 
	 * @param c_dist coreDistance
	 */
	public void setCoreDistance(double c_dist) {
		this.c_dist = c_dist;
	}

	/**
	 * Returns the coreDistance for this dataObject
	 * 
	 * @return coreDistance
	 */
	public double getCoreDistance() {
		return c_dist;
	}

	/**
	 * Sets a new reachability-distance for this dataObject
	 */
	public void setReachabilityDistance(double r_dist) {
		this.r_dist = r_dist;
	}

	/**
	 * Returns the reachabilityDistance for this dataObject
	 */
	public double getReachabilityDistance() {
		return r_dist;
	}

	@Override
	public String toString() {
		return instance.toString();
	}

	/**
	 * Returns the revision string.
	 * 
	 * @return the revision
	 */
	public String getRevision() {
		return RevisionUtils.extract("$Revision: 1.5 $");
	}

}
