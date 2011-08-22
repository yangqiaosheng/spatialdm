package spade.analysis.tools.clustering;

import it.unipi.di.sax.optics.AnotherOptics;
import it.unipi.di.sax.optics.ClusterListener;
import it.unipi.di.sax.optics.ClusterObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.Vector;

import spade.analysis.system.ESDACore;
import spade.lib.util.BubbleSort;
import spade.lib.util.ObjectWithMeasure;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.database.ObjectFilter;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Aug 5, 2009
 * Time: 10:48:27 AM
 * Uses the clustering method OPTICS to cluster objects in a map layer.
 * It is assumed that the distances between the table records have been previously
 * computed and the distance matrix is provided to the TableClusterer.
 */
public class TableClusterer implements DistanceMeterExt<DClusterObject>, ClusterListener, Clusterer {
	/**
	 * The table where the records need to be clustered
	 */
	protected DataTable table = null;
	/**
	 * The matrix of pairwise distances between the records
	 */
	protected float distMatrix[][] = null;
	/**
	 * The objects of the layer in the order produced by OPTICS (or another algorithm)
	 */
	protected Vector<DClusterObject> objectsOrdered = null;
	/**
	 * As a result of clustering, a table is produced.
	 */
	protected DataTable clTable = null;
	/**
	 * System core; used for displaying messages, access to data, etc.
	 */
	protected ESDACore core = null;
	/**
	 * The distance threshold
	 */
	protected double distanceThreshold = Double.NaN;
	/**
	 * The required minimum number of objects in the neighbourhood of a core object
	 */
	protected int minNeighbours = 3;

	public void setSystemCore(ESDACore core) {
		this.core = core;
	}

	public void setTable(DataTable table) {
		this.table = table;
		if (table != null) {
			distMatrix = table.getDistanceMatrix();
		}
	}

	public DataTable getTable() {
		return table;
	}

	public void setDistanceMatrix(float[][] distMatrix) {
		this.distMatrix = distMatrix;
	}

	/**
	 * Returns true if it has all necessary internal settings
	 */
	@Override
	public boolean hasValidSettings() {
		return distMatrix != null;
	}

	/**
	 * Returns a generated description of the method used
	 */
	public String getDescription() {
		return "Pre-computed distance matrix";
	}

	@Override
	public double getDistanceThreshold() {
		return distanceThreshold;
	}

	public void setDistanceThreshold(double distanceThreshold) {
		this.distanceThreshold = distanceThreshold;
	}

	@Override
	public int getMinNeighbours() {
		return minNeighbours;
	}

	public void setMinNeighbours(int minNeighbours) {
		this.minNeighbours = minNeighbours;
	}

	/**
	 * Forms cluster objects with indexes of the objects in the layer
	 */
	protected Vector<DClusterObject> prepareData() {
		Vector<DClusterObject> objects = new Vector<DClusterObject>();
		ObjectFilter oFilter = table.getObjectFilter();
		for (int i = 0; i < table.getDataItemCount(); i++)
			if (oFilter == null || oFilter.isActive(i)) {
				String id = table.getDataItemId(i);
				DClusterObject clo = new DClusterObject(table.getDataRecord(i), id, i);
				clo.numId = i;
				objects.addElement(clo);
			}
		return objects;
	}

	/**
	 * Used for clustering
	 */
	@Override
	public double distance(DClusterObject o1, DClusterObject o2) {
		if (o1 == null || o2 == null)
			return Double.POSITIVE_INFINITY;
		if (distMatrix != null && o1.idx >= 0 && o2.idx >= 0 && o1.idx < distMatrix.length && o2.idx < distMatrix.length && !Float.isNaN(distMatrix[o1.idx][o2.idx]))
			return distMatrix[o1.idx][o2.idx];
		return Double.POSITIVE_INFINITY;
	}

	/**
	 * Used for clustering
	 */
	@Override
	public Collection<DClusterObject> neighbors(DClusterObject core, Collection<DClusterObject> objects, double eps) {
		Vector neighborsWithDist = new Vector(100, 100);

		for (DClusterObject o : objects) {
			if (!o.equals(core)) {
				double dist = distance(o, core);
				if (dist <= eps) {
					ObjectWithMeasure om = new ObjectWithMeasure(o, dist);
					neighborsWithDist.addElement(om);
				}
			}
		}
		if (neighborsWithDist.size() > 1) {
			BubbleSort.sort(neighborsWithDist);
		}
		Vector<DClusterObject> neighbors = new Vector<DClusterObject>(neighborsWithDist.size(), 10);
		for (int i = 0; i < neighborsWithDist.size(); i++) {
			neighbors.addElement((DClusterObject) ((ObjectWithMeasure) neighborsWithDist.elementAt(i)).obj);
		}

		return neighbors;
	}

	/**
	 * Runs the clustering tool OPTICS using the specified distance threshold
	 * and the required minimum number of objects in the neighbourhood of a
	 * cluster core object
	 */
	public void doClustering(double distance, int minNeighbours) {
		if (table == null || distMatrix == null)
			return;
		distanceThreshold = distance;
		this.minNeighbours = minNeighbours;
		long t0 = System.currentTimeMillis();
		Vector<DClusterObject> objects = prepareData();
		if (objects == null || objects.size() < 2)
			return;
		//JOptics optics = new JOptics(this);
		AnotherOptics optics = new AnotherOptics(this);
		optics.addClusterListener(this);
		optics.optics(objects, distance, minNeighbours);
		long t = System.currentTimeMillis();
		if (core != null && core.getUI() != null) {
			core.getUI().showMessage("Clustering took " + (t - t0) + " msec, " + ((t - t0) / 1000) + " sec, " + ((t - t0) / 60000) + " min", false);
		}
		System.out.println("Clustering took " + (t - t0) + " msec, " + ((t - t0) / 1000) + " sec, " + (1.0f * (t - t0) / 60000) + " min");
	}

	/**
	 * Receives an object from the clustering tool
	 */
	@Override
	public void emit(ClusterObject o) {
		if (o == null)
			return;
		if (clTable == null) {
			clTable = new DataTable();
			clTable.setName("Clustering results for " + table.getName());
			clTable.setEntitySetIdentifier(table.getEntitySetIdentifier());
			clTable.addAttribute("Reachability distance", "reach_dist", AttributeTypes.real);
			clTable.addAttribute("Core distance", "core_dist", AttributeTypes.real);
		}
		DClusterObject clObj = getDClusterObject(o);
		if (clObj == null)
			return;
		clObj.coreDistance = o.getCoreDistance();
		clObj.reachabilityDistance = o.getReachabilityDistance();
		if (objectsOrdered == null) {
			objectsOrdered = new Vector<DClusterObject>(table.getDataItemCount(), 10);
		}
		objectsOrdered.addElement(clObj);
		DataRecord obj = (DataRecord) clObj.originalObject;
		DataRecord rec = new DataRecord(obj.getId(), obj.getName());
		clTable.addDataRecord(rec);
		if (clObj.reachabilityDistance != Double.POSITIVE_INFINITY) {
			rec.setNumericAttrValue(clObj.reachabilityDistance, String.valueOf((float) clObj.reachabilityDistance), 0);
		} else {
			rec.setAttrValue(null, 0);
			clObj.reachabilityDistance = Double.NaN;
		}
		if (clObj.coreDistance != Double.POSITIVE_INFINITY) {
			rec.setNumericAttrValue(clObj.coreDistance, String.valueOf((float) clObj.coreDistance), 1);
		} else {
			rec.setAttrValue(null, 1);
			clObj.coreDistance = Double.NaN;
		}
		if (core != null && core.getUI() != null && clTable.getDataItemCount() % 100 == 0) {
			core.getUI().showMessage("Clustering..." + clTable.getDataItemCount() + " objects processed", false);
		}
	}

	/**
	 * Extracts a DClusterObject from the given ClusterObject
	 */
	protected DClusterObject getDClusterObject(ClusterObject o) {
		if (o == null)
			return null;
		Object obj = o.getOriginalObject();
		while (obj != null && (obj instanceof ClusterObject)) {
			obj = ((ClusterObject) obj).getOriginalObject();
		}
		if (obj == null)
			return null;
		if (obj instanceof DClusterObject)
			return (DClusterObject) obj;
		return null;
	}

	/**
	 * Returns the result of clustering as DataTable
	 */
	@Override
	public DataTable getResult() {
		return clTable;
	}

	/**
	 * Returns the clustered objects in the order produced by OPTICS (or another algorithm)
	 */
	@Override
	public Vector<DClusterObject> getObjectsOrdered() {
		return objectsOrdered;
	}

	/**
	 * Returns its parameters, which are necessary and sufficient
	 * for setting up the DistanceMeter. Each parameter has a name,
	 * which is used as a key in the HashMap.
	 * The values of the parameters must be stored in the hashmap as strings!
	 * This is necessary for saving the parameters in a text file.
	 * If the argument is not null, adds its parameter-value pairs to the
	 * given hashmap.
	 */
	@Override
	public HashMap getParameters(HashMap params) {
		if (params == null) {
			params = new HashMap(20);
		}
		if (!Double.isNaN(distanceThreshold)) {
			params.put("distanceThreshold", String.valueOf(distanceThreshold));
		}
		params.put("minNeighbours", String.valueOf(minNeighbours));
		return params;
	}

	/**
	 * Does its internal settings according to the given list of parameters
	 */
	@Override
	public void setup(HashMap params) {
		if (params == null)
			return;
		String val = (String) params.get("minNeighbours");
		if (val != null) {
			try {
				minNeighbours = Integer.parseInt(val);
			} catch (NumberFormatException e) {
			}
		}
		val = (String) params.get("distanceThreshold");
		if (val != null) {
			try {
				distanceThreshold = Double.parseDouble(val);
			} catch (NumberFormatException e) {
			}
		}
	}

	/**
	 * Informs if it deals with geographical coordinates (latitudes and longitudes)
	 */
	@Override
	public boolean isGeographic() {
		return false;
	}
}
