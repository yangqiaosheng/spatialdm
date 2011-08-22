package spade.analysis.tools.index;

import weka.clusterers.forOPTICSAndDBScan.Databases.Database;
import weka.core.Instance;

public class Event extends GeoDataObject {

	public Event(Instance originalInstance, String key, Database database) {
		super(originalInstance, key, database);
		// TODO Auto-generated constructor stub
	}

	// link to enveloping cluster
	public Cluster c = null;
	public long begin = 0;
	public long end = 0;
	public StringBuffer label = null;

	/**
	 * Returns the clusterID, to which this DataObject belongs to
	 * @return clusterID
	 */
	@Override
	public int getClusterLabel() {
		if (c == null)
			return super.getClusterLabel();
		return c.label;
	}
}
