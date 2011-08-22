/**
 * 
 */
package spade.analysis.tools;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import spade.analysis.tools.index.EpsilonRange;
import spade.analysis.tools.index.Event;
import spade.analysis.tools.index.EventList;
import spade.analysis.tools.index.GeoDataObject;
import spade.time.Date;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import weka.clusterers.AbstractClusterer;
import weka.clusterers.ClusterEvaluation;
import weka.clusterers.forOPTICSAndDBScan.DataObjects.DataObject;
import weka.clusterers.forOPTICSAndDBScan.Databases.Database;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;

/**
 * @author Christian P.
 * 
 */
public class StepwiseDBSCAN extends AbstractClusterer implements OptionHandler, TechnicalInformationHandler {

	public class boundingBox {
		double min_x = Double.MAX_VALUE, min_y = Double.MAX_VALUE, max_x = -Double.MAX_VALUE, max_y = -Double.MAX_VALUE;
		double min_t = Double.MAX_VALUE, max_t = 0.0;
		double eps = 0.0;
		Date min, max;
		int count = 0;
		int parent_cluster = -1;
		int cluster = -1;
		int children = 0;

		public boundingBox(double eps) {
			this.eps = eps;
		}

		public void newChild() {
			children++;
		}

		public String getMinTime() {
			return min.toString();
		}

		public String getMaxTime() {
			return max.toString();
		}

		public int getParent() {
			return parent_cluster;
		}

		public double getdist() {
			return GeoDataObject.geoDist(min_x, min_y, max_x, max_y);
		}

		public int getCount() {
			return count;
		}

		public double getTempDist() {
			return Math.abs(max_t - min_t) / 1000;
		}

		public double getGeoDensity() {
			return getdist() / getCount();
		}

		public double getTempDensity() {
			return getTempDist() / getCount();
		}

		public void update(Instance i, int p, int c) {
			double x = i.valueSparse(0);
			double y = i.valueSparse(1);
			parent_cluster = p;
			cluster = c;
			count++;
			if (x < min_x) {
				min_x = x;
			}
			if (y < min_y) {
				min_y = y;
			}

			if (x > max_x) {
				max_x = x;
			}
			if (y > max_y) {
				max_y = y;
			}
			try {
				long t = (long) i.valueSparse(2);

				if (t < min_t) {
					min_t = t;
				}
				if (t > max_t) {
					max_t = t;
				}
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
		}

		public String getStatistics() {
			Date df1 = new Date();
			df1.setDateScheme("dd/mm/yyyy hh:tt:ss");
			df1.setPrecision('s');
			df1.setSecond(0);
			df1.setMinute(0);
			df1.setHour(0);
			df1.setDay(0);
			df1.setMonth(0);
			df1.setYear(0);
			min = (Date) df1.valueOf((long) min_t);
			max = (Date) df1.valueOf((long) max_t);
			return parent_cluster + ", " + min_x + ", " + min_y + ", " + max_x + ", " + max_y + ", " + getdist() + ", " + min.toString() + ", " + min.toString() + ", " + getTempDist() + ", " + cluster + "," + count + "," + eps + "\n";
		}

		public Vector<String> getStatisticsValues() {
			Date df1 = new Date();
			df1.setDateScheme("dd/mm/yyyy hh:tt:ss");
			df1.setPrecision('s');
			df1.setSecond(0);
			df1.setMinute(0);
			df1.setHour(0);
			df1.setDay(0);
			df1.setMonth(0);
			df1.setYear(0);
			min = (Date) df1.valueOf((long) min_t);
			max = (Date) df1.valueOf((long) max_t);
			Vector<String> tmp = new Vector<String>();
			tmp.add(cluster + "");
			tmp.add(parent_cluster + "");

			tmp.add(count + "");
			tmp.add(min_x + "");
			tmp.add(min_y + "");
			tmp.add(max_x + "");
			tmp.add(max_y + "");
			tmp.add(getdist() + "");
			tmp.add(getGeoDensity() + "");
			tmp.add(min.toString() + "");
			tmp.add(min.toString() + "");
			tmp.add(getTempDist() + "");
			tmp.add(getTempDensity() + "");
			tmp.add(children + "");
			tmp.add(eps + "");
			return tmp;
		}

	}

	public Vector<String> getStatisticsNames() {
		Vector<String> tmp = new Vector<String>();
		tmp.add("ClusterID");
		tmp.add("ClusterID parent");

		tmp.add("Count");
		tmp.add("Min(x)");
		tmp.add("Min(y)");
		tmp.add("Max(x)");
		tmp.add("Max(y)");
		tmp.add("Geo distance");
		tmp.add("Geo density");
		tmp.add("Min(Time)");
		tmp.add("Max(Time)");
		tmp.add("Temp dist");
		tmp.add("Temp density");
		tmp.add("Children");
		tmp.add("Eps");
		return tmp;
	}

	public Vector<Character> getStatisticsTypes() {
		Vector<Character> tmp = new Vector<Character>(Arrays.asList(AttributeTypes.integer, AttributeTypes.integer, AttributeTypes.integer, AttributeTypes.real, AttributeTypes.real, AttributeTypes.real, AttributeTypes.real, AttributeTypes.real,
				AttributeTypes.real, AttributeTypes.time, AttributeTypes.time, AttributeTypes.real, AttributeTypes.real, AttributeTypes.integer, AttributeTypes.real));

		return tmp;
	}

	public java.util.List<String> getStatistics() {
		java.util.List<String> tmp = new ArrayList<String>();
		for (boundingBox s : map_box.values()) {
			tmp.add(s.getStatistics());
		}
		for (boundingBox s : tmp_box.values()) {
			tmp.add(s.getStatistics());
		}

		return tmp;
	}

	public java.util.List<String> getStatisticsParents() {
		java.util.List<String> tmp = new ArrayList<String>();
		for (boundingBox s : map_box.values()) {
			tmp.add(s.getStatistics());
		}

		return tmp;
	}

	public java.util.List<String> getStatisticsChrildren() {
		java.util.List<String> tmp = new ArrayList<String>();

		for (boundingBox s : tmp_box.values()) {
			tmp.add(s.getStatistics());
		}

		return tmp;
	}

	public boundingBox getSpatialBounding(Integer id) {
		return map_box.get(id);
	}

	public boundingBox getSupBounding(Integer id) {
		return tmp_box.get(id);
	}

	public DataTable getStats() {
		DataTable freqTable = new DataTable();

		Vector<String> attrNames = getStatisticsNames();
		Vector<Character> attrTypes = getStatisticsTypes();
		for (int i = 0; i < attrNames.size(); i++) {
			freqTable.addAttribute(attrNames.get(i), attrTypes.get(i));
		}
		int id = 0;
		for (boundingBox s : map_box.values()) {
			id++;
			DataRecord rec = new DataRecord(id + "");
			rec.setAttrValues(s.getStatisticsValues());
			freqTable.addDataRecord(rec);
		}
		for (boundingBox s : tmp_box.values()) {
			id++;
			DataRecord rec = new DataRecord(id + "");
			rec.setAttrValues(s.getStatisticsValues());
			freqTable.addDataRecord(rec);
		}
		return freqTable;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	/** for serialization */
	static final long serialVersionUID = -1666498248451219728L;

	/**
	 * Replace missing values in training instances
	 */
	private ReplaceMissingValues replaceMissingValues_Filter;

	/**
	 * Holds the number of clusters generated
	 */
	private int numberOfGeneratedClusters;

	/**
	 * Holds the distance-type that is used (default =
	 * weka.clusterers.forOPTICSAndDBScan.DataObjects.EuclidianDataObject)
	 */
	private String database_distanceType = "weka.clusterers.forOPTICSAndDBScan.DataObjects.EuclidianDataObject";

	/**
	 * Holds the type of the used database (default =
	 * weka.clusterers.forOPTICSAndDBScan.Databases.SequentialDatabase)
	 */
	private String database_Type = "weka.clusterers.forOPTICSAndDBScan.Databases.SequentialDatabase";

	/**
	 * The database that is used for DBScan
	 */
	private Database database;

	/**
	 * Holds the current clusterID
	 */
	private int clusterID;

	/**
	 * Counter for the processed instances
	 */
	private int processed_InstanceID;

	/**
	 * Holds the time-value (seconds) for the duration of the clustering-process
	 */
	private double elapsedTime;
	/**
	 * the spatial threshold
	 */
	double epsilon = 0.0;
	/**
	 * the temporal threshold
	 */
	double minTempEspsilon = 0.0;
	/**
	 * The minimum number of points in the epsilon neighbourhood
	 */
	int minPoints = 3;
	/**
	 * Spatial distance for minimum temporal threshold
	 */
	double spa_dist = 0.0;
	/**
	 * Minimum entries for minimum temporal threshold
	 */
	int entries = 0;
	/**
	 * the total number of clusters
	 */
	int numClusters = 0;
	/**
	 * the minimum spatial extend of a cluster
	 */
	double minSpatial = 0.0;
	/**
	 * the minimum temporal extend of a cluster
	 */
	double minTemporal = 0.0;

	/**
	 * the number of created clusters
	 * 
	 * @return
	 */
	public int numClusters() {
		return numClusters;
	}

	/**
	 * Sets the clustering parameters
	 * 
	 * @param eps
	 *            The spatial threshold
	 * @param minTempEps
	 *            The temporal threshold
	 */
	public void setParameters(double eps, double minTempEps, int minP, double spa_dist, int entries) {
		epsilon = eps;
		minTempEspsilon = minTempEps;
		minPoints = minP;
		this.spa_dist = spa_dist;
		this.entries = entries;
	}

	/*
	 * The cluster assignments
	 */
	double[] results = null;

	/*
	 * The parent cluster assignments
	 */
	double[] parents = null;

	/*
	 * The number of different parent clusters
	 */
	int numParents = 0;

	public int getNumParents() {
		return numParents;
	}

	/**
	 * for each cluster the spatial expand and some statistics are calculated
	 */
	HashMap<Integer, boundingBox> map_box = new HashMap<Integer, boundingBox>();

	/**
	 * for each temporal cluster in the spatial clusters statistics and temporal
	 * extend are calculated
	 */
	HashMap<Integer, boundingBox> tmp_box = new HashMap<Integer, boundingBox>();

	/**
	 * builds the clustering for the instances in a stepswise manner first a
	 * spatial clustering finds spatial clusters according to epsilon then a
	 * temporal clustering is applied to all spatial clusters to find clusters
	 * that vary in the temporal dimension according to the spatial extend of
	 * the corresponding clusters
	 * 
	 * @param inst
	 *            Instances to be clustered
	 */
	public void build(Instances inst) throws Exception {
		this.setEpsilon(epsilon);
		this.setMinPoints(minPoints);
		this.setDatabase_distanceType("spade.analysis.tools.index.GeoDataObject");
		this.setDatabase_Type("spade.analysis.tools.index.SpatialIndexDatabase");
		// opt.setDatabase_Type("spade.analysis.tools.SequentialDatabase");
		// opt.setWriteOPTICSresults(true);
		this.buildClusterer(inst);
		TreeMap<Integer, Instances> map = new TreeMap<Integer, Instances>();
		// create instances empty, but with attribute infos
		FastVector fv = new FastVector();
		for (int i = 0; i < inst.numAttributes(); i++) {
			fv.addElement(inst.attribute(i));
		}
		// evaluate the cluster results
		ClusterEvaluation eval = new ClusterEvaluation();
		eval.setClusterer(this);
		eval.evaluateClusterer(inst);
		// get cluster ids for corresponding instances
		double[] assignments = eval.getClusterAssignments();
		// the spatial extend of all the points
		boundingBox all_points = new boundingBox(epsilon);
		// the maximal value of spatial distance among the spatial clusters
		double max_s = 0.0;
		// the maximal value of temporal distance among the spatial clusters
		double min_s = Double.MAX_VALUE;
		// the minimum value of spatial distance among the spatial clusters
		double max_t = 0.0;
		// the minimum value of temporal distance among the spatial clusters
		double min_t = Double.MAX_VALUE;
		// the maximum cluster id
		int maxId = 0;
		// the mapping from each instance to its cluster
		HashMap<Instance, Integer> c_map = new HashMap<Instance, Integer>();
		// the mapping from each instance to its parent cluster
		HashMap<Instance, Integer> p_map = new HashMap<Instance, Integer>();
		ArrayList<Instance> tmp_inst = new ArrayList<Instance>();
		// create for each cluster an instances object containing all
		// instances
		// that belong to one and the same cluster
		for (int i = 0; i < inst.numInstances(); i++) {

			// current instance
			Instance in = inst.instance(i);
			int l = -1;
			// current label
			try {
				l = this.clusterInstance(in);// (int) assignments[i];
			} catch (Exception e) {
				// TODO: handle exception
				l = -1;
			}

			// c_map.put(in, l);
			if (l > maxId) {
				maxId = l;
			}

			all_points.update(in, -1, -1);
			if (!map.containsKey(l)) {
				Instances cluster_objects = new Instances("", fv, 10);
				boundingBox bound = new boundingBox(epsilon);
				bound.update(in, -1, l);
				map_box.put(l, bound);
				cluster_objects.add(in);
				map.put(l, cluster_objects);
			} else {
				Instances cluster_objects = map.get(l);
				boundingBox bound = map_box.get(l);
				bound.update(in, -1, l);
				cluster_objects.add(in);
			}
			// c_map.put(map.get(l).lastInstance().toString(), l);
			tmp_inst.add(map.get(l).lastInstance());
		}
		// the ten powers of the maximum number of cluster ids
		int digits = 100;
		while ((maxId % 10) != 0) {
			maxId = maxId / 10;
			digits *= 10;
		}
		double extend = 0.0;// all_points.getdist();
		// find cluster with maximum spatial extend
		for (Integer cluster : map.keySet()) {
			// noise must not be clustered
			if (cluster < 0) {
				continue;
			}
			if (extend < map_box.get(cluster).getdist()) {
				extend = map_box.get(cluster).getdist();
			}

		}
		// temporal cluster
		this.setDatabase_distanceType("spade.analysis.tools.index.TemporalDataObject");
		this.setDatabase_Type("spade.analysis.tools.index.TemporalIndexDatabase");
		this.setEpsilon(minTempEspsilon);
		double minTemp = minTempEspsilon;
		// the results are written in a file
		int id = 0;
		results = new double[assignments.length];
		parents = new double[assignments.length];
		BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("C:/clus_xxx.txt", true));
		bufferedWriter.write("ID,C_ID,X,Y,DT \n");
		HashMap<Integer, Integer> clusters = new HashMap<Integer, Integer>();

		// bounding box of the current spatial cluster
		boundingBox bb = null;
		for (Integer cluster : map.keySet()) {
			// noise must not be clustered
			if (cluster < 0) {
				continue;
			}
			numParents++;
			bb = map_box.get(cluster);
			// set temporal threshold according to spatial extend of the
			// cluster
			double eps = (Math.min(1, (double) entries / (double) map_box.get(cluster).getCount())) * (spa_dist / Math.max(1, map_box.get(cluster).getdist())) * minTemp;
			System.out.println("Cluster " + cluster + " Eps(t) " + eps);
			this.setEpsilon(eps);
			// temporal clustering of all points of a certain cluster
			Instances c_inst = map.get(cluster);
			this.buildClusterer(c_inst);
			// the results of the clustering of the cluster
			eval.setClusterer(this);
			eval.evaluateClusterer(c_inst);
			assignments = eval.getClusterAssignments();
			// write theme in a file with a unique id
			int c = 0;
			for (int i = 0; i < c_inst.numInstances(); i++) {
				// point id
				id++;
				// current instance
				Instance in = c_inst.instance(i);
				// current label
				int l = -1;
				try {
					l = this.clusterInstance(in);// (int) assignments[i];
				} catch (Exception e) {
					// TODO: handle exception
					l = -1;
				}

				// exclude noise
				if (l < 0) {
					c_map.put(in, -1);
					p_map.put(in, cluster);
				} else {
					// create cluster id 1, 2, 3, ...
					if (!clusters.containsKey(l)) {
						// increase number of clusters
						numClusters++;
						c = numClusters;
						// add count of children of spatial super cluster
						bb.newChild();
						clusters.put(l, numClusters);
						boundingBox bound = new boundingBox(eps);
						bound.update(in, cluster, c);
						tmp_box.put(c, bound);
					} else {
						c = clusters.get(l);
						boundingBox bound = tmp_box.get(c);
						bound.update(in, cluster, c);
					}

					bufferedWriter.write(id + "," + cluster + "_" + c + "," + in.toString() + "\n");

					// make unique cluster id of two cluster ids
					// l = (int)Math.round(Double.parseDouble(l + "."
					// + cluster)*digits);
					c_map.put(in, c);
					p_map.put(in, cluster);
				}

			}
			clusters.clear();
			bufferedWriter.flush();
		}
		bufferedWriter.close();

		for (int i = 0; i < tmp_inst.size(); i++) {
			if (c_map.containsKey(tmp_inst.get(i))) {
				results[i] = c_map.get(tmp_inst.get(i));
				parents[i] = p_map.get(tmp_inst.get(i));
			} else {
				results[i] = -1;
				parents[i] = -1;
			}
		}
	}

	List<Event> instEvents = new LinkedList<Event>();

	/**
	 * builds the clustering for the instances in a stepswise manner first a
	 * spatial clustering finds spatial clusters according to epsilon then a
	 * temporal clustering is applied to all spatial clusters to find clusters
	 * that vary in the temporal dimension according to the spatial extend of
	 * the corresponding clusters
	 * 
	 * @param inst
	 *            Instances to be clustered
	 */
	public void build2(Instances inst) throws Exception {
		this.setEpsilon(epsilon);
		this.setMinPoints(minPoints);
		this.setDatabase_distanceType("spade.analysis.tools.index.Event");
		this.setDatabase_Type("spade.analysis.tools.index.SpatialListIndexDatabase");
		// opt.setDatabase_Type("spade.analysis.tools.SequentialDatabase");
		// opt.setWriteOPTICSresults(true);
		this.buildClusterer(inst);
		TreeMap<Integer, Instances> map = new TreeMap<Integer, Instances>();
		// create instances empty, but with attribute infos
		FastVector fv = new FastVector();
		for (int i = 0; i < inst.numAttributes(); i++) {
			fv.addElement(inst.attribute(i));
		}
		// the spatial extend of all the points
		boundingBox all_points = new boundingBox(epsilon);
		// the maximal value of spatial distance among the spatial clusters
		double max_s = 0.0;
		// the maximal value of temporal distance among the spatial clusters
		double min_s = Double.MAX_VALUE;
		// the minimum value of spatial distance among the spatial clusters
		double max_t = 0.0;
		// the minimum value of temporal distance among the spatial clusters
		double min_t = Double.MAX_VALUE;
		// the maximum cluster id
		int maxId = 0;
		// the mapping from each instance to its cluster
		HashMap<Instance, Integer> c_map = new HashMap<Instance, Integer>();
		// the mapping from each instance to its parent cluster
		HashMap<Instance, Integer> p_map = new HashMap<Instance, Integer>();
		ArrayList<Instance> tmp_inst = new ArrayList<Instance>();
		// create for each cluster an instances object containing all
		// instances
		// that belong to one and the same cluster
		for (int i = 0; i < instEvents.size(); i++) {

			// current instance
			Instance in = instEvents.get(i).getInstance();
			int l = -1;
			// current label
			try {
				l = instEvents.get(i).getClusterLabel();// (int) assignments[i];
				//System.out.println(l);
			} catch (Exception e) {
				// TODO: handle exception
				l = -1;
			}

			// c_map.put(in, l);
			if (l > maxId) {
				maxId = l;
			}

			all_points.update(in, -1, -1);
			if (!map.containsKey(l)) {
				Instances cluster_objects = new Instances("", fv, 10);
				boundingBox bound = new boundingBox(epsilon);
				bound.update(in, -1, l);
				map_box.put(l, bound);
				cluster_objects.add(in);
				map.put(l, cluster_objects);
			} else {
				Instances cluster_objects = map.get(l);
				boundingBox bound = map_box.get(l);
				bound.update(in, -1, l);
				cluster_objects.add(in);
			}
			// c_map.put(map.get(l).lastInstance().toString(), l);
			tmp_inst.add(map.get(l).lastInstance());
		}
		// the ten powers of the maximum number of cluster ids
		int digits = 100;
		while ((maxId % 10) != 0) {
			maxId = maxId / 10;
			digits *= 10;
		}
		double extend = 0.0;// all_points.getdist();
		// find cluster with maximum spatial extend
		for (Integer cluster : map.keySet()) {
			// noise must not be clustered
			if (cluster < 0) {
				continue;
			}
			if (extend < map_box.get(cluster).getdist()) {
				extend = map_box.get(cluster).getdist();
			}

		}
		double minTemp = minTempEspsilon;
		// the results are written in a file
		results = new double[inst.numInstances()];
		parents = new double[inst.numInstances()];
		BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("C:/clus_xxx.txt", true));
		bufferedWriter.write("ID,C_ID,X,Y,DT \n");
		HashMap<Integer, Integer> clusters = new HashMap<Integer, Integer>();
		HashMap<Integer, EventList> clusterLists = new HashMap<Integer, EventList>();
		// bounding box of the current spatial cluster
		boundingBox bb = null;
		// the latest cluster
		int c_id = 0;
		for (Integer cluster : map.keySet()) {
			// noise must not be clustered
			if (cluster < 0) {
				continue;
			}
			numParents++;
			bb = map_box.get(cluster);
			// set temporal threshold according to spatial extend of the
			// cluster
			double eps = (Math.min(1, (double) entries / (double) map_box.get(cluster).getCount())) * (spa_dist / Math.max(1, map_box.get(cluster).getdist())) * minTemp;
			System.out.println("Cluster " + cluster + " Eps(t) " + eps);
			// temporal clustering of all points of a certain cluster
			Instances c_inst = map.get(cluster);

			// list of events of current spatial cluster
			EventList el = new EventList(null, cluster + "", null);
			// add instances of current spatial clustering
			el.addInstances(c_inst);
			// do temporal clustering by finding dense regions
			Iterator<Event> it = el.events.iterator();

			// was the last event in a cluster (not noise)
			boolean done = false;
			// if an event is noise
			boolean isNoise = false;
			// the next event
			Event next = null;
			// the last event
			Event last = null;
			// the latest bounding box
			boundingBox bound = null;
			// the starting time of the last event of the current cluster
			long start = 0;
			// mapping from the found temporal clusters to its objects
			// (Map<Integer, List<Event>>
			LinkedList<Event> temp_cluster = new LinkedList<Event>();
			while (it.hasNext()) {
				next = it.next();
				if (last != null) {
					//System.out.println("Last " + last.begin + " Next "
					//		+ next.begin);
					if (next.begin < last.begin) {
						System.out.println("Error");
					}

					// is the next event close enough in time to be in the same
					// cluster as the last one
					if ((next.begin <= last.end + eps)) { // &&(next.begin<=last.end+minTemp))
						// {
						//System.out.println("Close in time");
						temp_cluster.addLast(last);
						if (!it.hasNext()) {
							done = true;
						}
					} else {
						done = true;

						temp_cluster.add(last);

					}
					if (done) {
						if (temp_cluster.size() >= minPoints) {

							c_id++;
							//System.out.println("New cluster " + c_id);
							bb.newChild();
							bound = new boundingBox(eps);
							tmp_box.put(c_id, bound);
							for (int i = 0; i < temp_cluster.size(); i++) {
								Event tmp = temp_cluster.get(i);
								tmp.setClusterLabel(c_id);
								c_map.put(tmp.getInstance(), c_id);
								p_map.put(tmp.getInstance(), cluster);
								bound.update(tmp.getInstance(), cluster, c_id);
							}
							if (!it.hasNext()) {
								next.setClusterLabel(c_id);
								c_map.put(next.getInstance(), c_id);
								p_map.put(next.getInstance(), cluster);
								bound.update(next.getInstance(), cluster, c_id);
							}
						} else {
							//System.out.println("Noise");
							for (int i = 0; i < temp_cluster.size(); i++) {
								Event tmp = temp_cluster.get(i);
								tmp.setClusterLabel(-1);
								c_map.put(tmp.getInstance(), -1);
								p_map.put(tmp.getInstance(), cluster);
							}
							if (!it.hasNext()) {
								next.setClusterLabel(-1);
								c_map.put(next.getInstance(), -1);
								p_map.put(next.getInstance(), cluster);
							}
						}
						temp_cluster.clear();
						done = false;
					}

				}
				last = next;
			}
		}
		numClusters = c_id;
		for (int i = 0; i < tmp_inst.size(); i++) {
			if (c_map.containsKey(tmp_inst.get(i))) {
				results[i] = c_map.get(tmp_inst.get(i));
				parents[i] = p_map.get(tmp_inst.get(i));
			} else {
				results[i] = -1;
				parents[i] = -1;
			}
		}
	}

	/**
	 * Cluster assignments for all instances
	 * 
	 * @return
	 */
	public double[] getAssignments() {
		return results;
	}

	public double[] getParents() {
		return parents;
	}

	/**
	 * Returns default capabilities of the clusterer.
	 * 
	 * @return the capabilities of this clusterer
	 */
	public Capabilities getCapabilities() {
		Capabilities result = super.getCapabilities();
		result.disableAll();
		result.enable(Capability.NO_CLASS);

		// attributes
		result.enable(Capability.NOMINAL_ATTRIBUTES);
		result.enable(Capability.NUMERIC_ATTRIBUTES);
		result.enable(Capability.DATE_ATTRIBUTES);
		result.enable(Capability.MISSING_VALUES);

		return result;
	}

	/**
	 * Generate Clustering via DBScan
	 * 
	 * @param instances
	 *            The instances that need to be clustered
	 * @throws java.lang.Exception
	 *             If clustering was not successful
	 */
	public void buildClusterer(Instances instances) throws Exception {
		// can clusterer handle the data?
		getCapabilities().testWithFail(instances);

		long time_1 = System.currentTimeMillis();

		processed_InstanceID = 0;
		numberOfGeneratedClusters = 0;
		clusterID = 0;

		replaceMissingValues_Filter = new ReplaceMissingValues();
		replaceMissingValues_Filter.setInputFormat(instances);
		Instances filteredInstances = Filter.useFilter(instances, replaceMissingValues_Filter);

		database = databaseForName(getDatabase_Type(), filteredInstances);
		for (int i = 0; i < database.getInstances().numInstances(); i++) {
			DataObject dataObject = dataObjectForName(getDatabase_distanceType(), database.getInstances().instance(i), Integer.toString(i), database);
			database.insert(dataObject);
			// add the latest created event in the current list
			instEvents.add((Event) dataObject);
		}
		database.setMinMaxValues();

		Iterator iterator = database.dataObjectIterator();
		while (iterator.hasNext()) {
			DataObject dataObject = (DataObject) iterator.next();
			if (dataObject.getClusterLabel() == DataObject.UNCLASSIFIED) {
				if (expandCluster(dataObject)) {
					clusterID++;
					numberOfGeneratedClusters++;
				}
			}
		}

		long time_2 = System.currentTimeMillis();
		elapsedTime = (double) (time_2 - time_1) / 1000.0;
	}

	/**
	 * Assigns this dataObject to a cluster or remains it as NOISE
	 * 
	 * @param dataObject
	 *            The DataObject that needs to be assigned
	 * @return true, if the DataObject could be assigned, else false
	 */
	private boolean expandCluster(DataObject dataObject) {
		EpsilonRange<DataObject> seedList = new EpsilonRange<DataObject>(database.epsilonRangeQuery(getEpsilon(), dataObject));
		/** dataObject is NO coreObject */
		if (seedList.entries() < getMinPoints()) {
			dataObject.setClusterLabel(DataObject.NOISE);
			return false;
		}

		/** dataObject is coreObject */
		for (int i = 0; i < seedList.size(); i++) {
			DataObject seedListDataObject = (DataObject) seedList.get(i);
			/**
			 * label this seedListDataObject with the current clusterID, because
			 * it is in epsilon-range
			 */
			seedListDataObject.setClusterLabel(clusterID);
			if (seedListDataObject.equals(dataObject)) {
				seedList.remove(i);
				i--;
			}
		}

		/** Iterate the seedList of the startDataObject */
		for (int j = 0; j < seedList.size(); j++) {
			DataObject seedListDataObject = (DataObject) seedList.get(j);
			EpsilonRange<DataObject> seedListDataObject_Neighbourhood = new EpsilonRange<DataObject>(database.epsilonRangeQuery(getEpsilon(), seedListDataObject));

			/** seedListDataObject is coreObject */
			if (seedListDataObject_Neighbourhood.entries() >= getMinPoints()) {
				for (int i = 0; i < seedListDataObject_Neighbourhood.size(); i++) {
					DataObject p = (DataObject) seedListDataObject_Neighbourhood.get(i);
					if (p.getClusterLabel() == DataObject.UNCLASSIFIED || p.getClusterLabel() == DataObject.NOISE) {
						if (p.getClusterLabel() == DataObject.UNCLASSIFIED) {
							seedList.add(p);
						}
						p.setClusterLabel(clusterID);
					}
				}
			}
			seedList.remove(j);
			j--;
		}

		return true;
	}

	/**
	 * Classifies a given instance.
	 * 
	 * @param instance
	 *            The instance to be assigned to a cluster
	 * @return int The number of the assigned cluster as an integer
	 * @throws java.lang.Exception
	 *             If instance could not be clustered successfully
	 */
	public int clusterInstance(Instance instance) throws Exception {
		if (processed_InstanceID >= database.size()) {
			processed_InstanceID = 0;
		}
		int cnum = (database.getDataObject(Integer.toString(processed_InstanceID++))).getClusterLabel();
		if (cnum == DataObject.NOISE)
			throw new Exception();
		else
			return cnum;
	}

	/**
	 * Returns the number of clusters.
	 * 
	 * @return int The number of clusters generated for a training dataset.
	 * @throws java.lang.Exception
	 *             if number of clusters could not be returned successfully
	 */
	public int numberOfClusters() throws Exception {
		return numberOfGeneratedClusters;
	}

	/**
	 * Returns an enumeration of all the available options..
	 * 
	 * @return Enumeration An enumeration of all available options.
	 */
	public Enumeration listOptions() {
		Vector vector = new Vector();

		vector.addElement(new Option("\tepsilon (default = 0.9)", "E", 1, "-E <double>"));
		vector.addElement(new Option("\tminPoints (default = 6)", "M", 1, "-M <int>"));
		vector.addElement(new Option("\tindex (database) used for DBScan (default = weka.clusterers.forOPTICSAndDBScan.Databases.SequentialDatabase)", "I", 1, "-I <String>"));
		vector.addElement(new Option("\tdistance-type (default = weka.clusterers.forOPTICSAndDBScan.DataObjects.EuclidianDataObject)", "D", 1, "-D <String>"));
		return vector.elements();
	}

	/**
	 * Sets the OptionHandler's options using the given list. All options will
	 * be set (or reset) during this call (i.e. incremental setting of options
	 * is not possible).
	 * <p/>
	 * 
	 * <!-- options-start --> Valid options are:
	 * <p/>
	 * 
	 * <pre>
	 * -E &lt;double&gt;
	 *  epsilon (default = 0.9)
	 * </pre>
	 * 
	 * <pre>
	 * -M &lt;int&gt;
	 *  minPoints (default = 6)
	 * </pre>
	 * 
	 * <pre>
	 * -I &lt;String&gt;
	 *  index (database) used for DBScan (default = weka.clusterers.forOPTICSAndDBScan.Databases.SequentialDatabase)
	 * </pre>
	 * 
	 * <pre>
	 * -D &lt;String&gt;
	 *  distance-type (default = weka.clusterers.forOPTICSAndDBScan.DataObjects.EuclidianDataObject)
	 * </pre>
	 * 
	 * <!-- options-end -->
	 * 
	 * @param options
	 *            The list of options as an array of strings
	 * @throws java.lang.Exception
	 *             If an option is not supported
	 */
	public void setOptions(String[] options) throws Exception {
		String optionString = Utils.getOption('E', options);
		if (optionString.length() != 0) {
			setEpsilon(Double.parseDouble(optionString));
		}

		optionString = Utils.getOption('M', options);
		if (optionString.length() != 0) {
			setMinPoints(Integer.parseInt(optionString));
		}

		optionString = Utils.getOption('I', options);
		if (optionString.length() != 0) {
			setDatabase_Type(optionString);
		}

		optionString = Utils.getOption('D', options);
		if (optionString.length() != 0) {
			setDatabase_distanceType(optionString);
		}
	}

	/**
	 * Gets the current option settings for the OptionHandler.
	 * 
	 * @return String[] The list of current option settings as an array of
	 *         strings
	 */
	public String[] getOptions() {
		String[] options = new String[8];
		int current = 0;

		options[current++] = "-E";
		options[current++] = "" + getEpsilon();
		options[current++] = "-M";
		options[current++] = "" + getMinPoints();
		options[current++] = "-I";
		options[current++] = "" + getDatabase_Type();
		options[current++] = "-D";
		options[current++] = "" + getDatabase_distanceType();

		return options;
	}

	/**
	 * Returns a new Class-Instance of the specified database
	 * 
	 * @param database_Type
	 *            String of the specified database
	 * @param instances
	 *            Instances that were delivered from WEKA
	 * @return Database New constructed Database
	 */
	public Database databaseForName(String database_Type, Instances instances) {
		Object o = null;

		Constructor co = null;
		try {
			co = (Class.forName(database_Type)).getConstructor(new Class[] { Instances.class });
			o = co.newInstance(new Object[] { instances });
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}

		return (Database) o;
	}

	/**
	 * Returns a new Class-Instance of the specified database
	 * 
	 * @param database_distanceType
	 *            String of the specified distance-type
	 * @param instance
	 *            The original instance that needs to hold by this DataObject
	 * @param key
	 *            Key for this DataObject
	 * @param database
	 *            Link to the database
	 * @return DataObject New constructed DataObject
	 */
	public DataObject dataObjectForName(String database_distanceType, Instance instance, String key, Database database) {
		Object o = null;

		Constructor co = null;
		try {
			co = (Class.forName(database_distanceType)).getConstructor(new Class[] { Instance.class, String.class, Database.class });
			o = co.newInstance(new Object[] { instance, key, database });
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}

		return (DataObject) o;
	}

	/**
	 * Sets a new value for minPoints
	 * 
	 * @param minPoints
	 *            MinPoints
	 */
	public void setMinPoints(int minPoints) {
		this.minPoints = minPoints;
	}

	/**
	 * Sets a new value for epsilon
	 * 
	 * @param epsilon
	 *            Epsilon
	 */
	public void setEpsilon(double epsilon) {
		this.epsilon = epsilon;
	}

	/**
	 * Returns the value of epsilon
	 * 
	 * @return double Epsilon
	 */
	public double getEpsilon() {
		return epsilon;
	}

	/**
	 * Returns the value of minPoints
	 * 
	 * @return int MinPoints
	 */
	public int getMinPoints() {
		return minPoints;
	}

	/**
	 * Returns the distance-type
	 * 
	 * @return String Distance-type
	 */
	public String getDatabase_distanceType() {
		return database_distanceType;
	}

	/**
	 * Returns the type of the used index (database)
	 * 
	 * @return String Index-type
	 */
	public String getDatabase_Type() {
		return database_Type;
	}

	/**
	 * Sets a new distance-type
	 * 
	 * @param database_distanceType
	 *            The new distance-type
	 */
	public void setDatabase_distanceType(String database_distanceType) {
		this.database_distanceType = database_distanceType;
	}

	/**
	 * Sets a new database-type
	 * 
	 * @param database_Type
	 *            The new database-type
	 */
	public void setDatabase_Type(String database_Type) {
		this.database_Type = database_Type;
	}

	/**
	 * Returns the tip text for this property
	 * 
	 * @return tip text for this property suitable for displaying in the
	 *         explorer/experimenter gui
	 */
	public String epsilonTipText() {
		return "radius of the epsilon-range-queries";
	}

	/**
	 * Returns the tip text for this property
	 * 
	 * @return tip text for this property suitable for displaying in the
	 *         explorer/experimenter gui
	 */
	public String minPointsTipText() {
		return "minimun number of DataObjects required in an epsilon-range-query";
	}

	/**
	 * Returns the tip text for this property
	 * 
	 * @return tip text for this property suitable for displaying in the
	 *         explorer/experimenter gui
	 */
	public String database_TypeTipText() {
		return "used database";
	}

	/**
	 * Returns the tip text for this property
	 * 
	 * @return tip text for this property suitable for displaying in the
	 *         explorer/experimenter gui
	 */
	public String database_distanceTypeTipText() {
		return "used distance-type";
	}

	/**
	 * Returns a string describing this DataMining-Algorithm
	 * 
	 * @return String Information for the gui-explorer
	 */
	public String globalInfo() {
		return getTechnicalInformation().toString();
	}

	/**
	 * Returns an instance of a TechnicalInformation object, containing detailed
	 * information about the technical background of this class, e.g., paper
	 * reference or book this class is based on.
	 * 
	 * @return the technical information about this class
	 */
	public TechnicalInformation getTechnicalInformation() {
		TechnicalInformation result;

		result = new TechnicalInformation(Type.INPROCEEDINGS);
		result.setValue(Field.AUTHOR, "Martin Ester and Hans-Peter Kriegel and Joerg Sander and Xiaowei Xu");
		result.setValue(Field.TITLE, "A Density-Based Algorithm for Discovering Clusters in Large Spatial Databases with Noise");
		result.setValue(Field.BOOKTITLE, "Second International Conference on Knowledge Discovery and Data Mining");
		result.setValue(Field.EDITOR, "Evangelos Simoudis and Jiawei Han and Usama M. Fayyad");
		result.setValue(Field.YEAR, "1996");
		result.setValue(Field.PAGES, "226-231");
		result.setValue(Field.PUBLISHER, "AAAI Press");

		return result;
	}

	/**
	 * Returns a description of the clusterer
	 * 
	 * @return a string representation of the clusterer
	 */
	public String toString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("DBScan clustering results\n" + "========================================================================================\n\n");
		stringBuffer.append("Clustered DataObjects: " + database.size() + "\n");
		stringBuffer.append("Number of attributes: " + database.getInstances().numAttributes() + "\n");
		stringBuffer.append("Epsilon: " + getEpsilon() + "; minPoints: " + getMinPoints() + "\n");
		stringBuffer.append("Index: " + getDatabase_Type() + "\n");
		stringBuffer.append("Distance-type: " + getDatabase_distanceType() + "\n");
		stringBuffer.append("Number of generated clusters: " + numberOfGeneratedClusters + "\n");
		DecimalFormat decimalFormat = new DecimalFormat(".##");
		stringBuffer.append("Elapsed time: " + decimalFormat.format(elapsedTime) + "\n\n");

		for (int i = 0; i < database.size(); i++) {
			DataObject dataObject = database.getDataObject(Integer.toString(i));
			stringBuffer.append("(" + Utils.doubleToString(Double.parseDouble(dataObject.getKey()), (Integer.toString(database.size()).length()), 0) + ".) " + Utils.padRight(dataObject.toString(), 69) + "  -->  "
					+ ((dataObject.getClusterLabel() == DataObject.NOISE) ? "NOISE\n" : dataObject.getClusterLabel() + "\n"));
		}
		return stringBuffer.toString() + "\n";
	}

	/**
	 * Returns the revision string.
	 * 
	 * @return the revision
	 */
	public String getRevision() {
		return RevisionUtils.extract("$Revision: 1.1.2.4 $");
	}

}
