package spade.analysis.tools.index;

import java.util.LinkedList;
import java.util.List;

import weka.clusterers.forOPTICSAndDBScan.DataObjects.DataObject;
import weka.clusterers.forOPTICSAndDBScan.Databases.Database;
import weka.core.Instance;
import weka.core.Instances;

public class EventList extends GeoDataObject {
	public boolean processed = false;
	public List<Event> events = null;
	double[] coordinates = null;
	Cluster cluster = new Cluster();

	// reference to cluster list of clusters growing be events of this list of
	// events
	// List<cluster> refToClusterList = null;
	void setCoordinates(double[] co) {
		coordinates = co;
	}

	double[] getCoordinates() {
		return coordinates;
	}

	public EventList(Instance originalInstance, String key, Database database) {
		super(originalInstance, key, database);
		events = new LinkedList<Event>();
	}

	public void add(DataObject obj) {
		long tmp = (long) obj.getInstance().value(2);

		try {
			((Event) obj).begin = tmp;
			((Event) obj).end = tmp;
			((Event) obj).c = cluster;
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		events.add((Event) (obj));
	}

	public void addInstances(Instances insts) {
		for (int i = 0; i < insts.numInstances(); i++) {
			Event obj = new Event(insts.instance(i), i + "", null);
			obj.c = cluster;
			add(obj);
		}
	}

	public List<Event> getEvents() {
		return events;
	}

	/**
	 * Sets the clusterID (cluster), to which this DataObject belongs to
	 * @param clusterID Number of the Cluster
	 */
	@Override
	public void setClusterLabel(int clusterID) {
		super.setClusterLabel(clusterID);
		cluster.label = clusterID;
	}

}
