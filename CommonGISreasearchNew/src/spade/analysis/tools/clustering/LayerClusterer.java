package spade.analysis.tools.clustering;

import it.unipi.di.sax.optics.AnotherOptics;
import it.unipi.di.sax.optics.ClusterListener;
import it.unipi.di.sax.optics.ClusterObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.Vector;

import spade.analysis.system.ESDACore;
import spade.analysis.tools.distances.DistanceByAttributes;
import spade.analysis.tools.distances.DistanceComputer;
import spade.lib.util.ObjectWithMeasure;
import spade.lib.util.QSortAlgorithm;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 20-Apr-2007
 * Time: 16:37:58
 * Uses the clustering method OPTICS to cluster objects in a map layer.
 * Used as a base class for clusterers oriented to specific types of geographical
 * objects.
 */
public abstract class LayerClusterer implements DistanceMeterExt<DClusterObject>, ClusterListener, Clusterer {
	/**
	 * The layer with point objects that need to be clustered
	 */
	protected DGeoLayer layer = null;
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
	 * Whether the distance computers may use the threshold to
	 * reduce the computing time. By default is true.
	 */
	protected boolean mayUseThresholdInComputingDistances = true;
	/**
	 * The required minimum number of objects in the neighbourhood of a core object
	 */
	protected int minNeighbours = 3;
	/**
	 * The DistanceComputer is used to compute distances
	 */
	protected DistanceComputer distComp = null;
	/**
	 * Generated description of the method used
	 */
	protected String description = null;
	/**
	 * A matrix with the pairwise distances between the objects of the layer (to avoid
	 * repeated computing of the distances)
	 */
	protected double distMatrix[][] = null;
	/**
	 * Involves thematic attributes in the clustering
	 */
	protected DistanceByAttributes distAttr = null;

	public DistanceByAttributes getDistByAttrComputer() {
		return distAttr;
	}

	public void setDistByAttrComputer(DistanceByAttributes distAttr) {
		this.distAttr = distAttr;
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
		if (distComp != null) {
			params.put("distanceComputer", distComp.getClass().getName());
			distComp.getParameters(params);
		}
		if (!Double.isNaN(distanceThreshold)) {
			params.put("distanceThreshold", String.valueOf(distanceThreshold));
		}
		params.put("minNeighbours", String.valueOf(minNeighbours));
		if (distAttr != null) {
			distAttr.getParameters(params);
		}
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
		val = (String) params.get("distanceComputer");
		if (val != null) {
			try {
				Object obj = Class.forName(val).newInstance();
				if (obj != null && (obj instanceof DistanceComputer)) {
					distComp = (DistanceComputer) obj;
				}
			} catch (Exception e) {
			}
		}
		if (distComp != null) {
			distComp.setup(params);
		}
		if (distAttr != null) {
			distAttr.setup(params);
		}
	}

	/**
	 * Returns true if it has all necessary internal settings
	 */
	@Override
	public boolean hasValidSettings() {
		return distComp != null;
	}

	/**
	 * Makes a copy of this clusterer, ignoring any results obtained so far
	 */
	public abstract LayerClusterer getCopy();

	/**
	 * Copies the values of its field to the given LayerClusterer,
	 * ignoring any results obtained so far
	 */
	public void copyFields(LayerClusterer lclust) {
		if (lclust == null)
			return;
		lclust.layer = layer;
		lclust.core = core;
		lclust.distanceThreshold = distanceThreshold;
		lclust.minNeighbours = minNeighbours;
		lclust.distComp = distComp;
		lclust.description = description;
		lclust.setMayUseThresholdInComputingDistances(mayUseThresholdInComputingDistances);
		lclust.setDistByAttrComputer(distAttr);
	}

	/**
	 * Returns a list (vector) of names of available distance functions
	 */
	public abstract Vector getDistanceComputerNames();

	/**
	 * Returns the layer with point objects that is clustered
	 */
	public DGeoLayer getLayer() {
		return layer;
	}

	/**
	 * Sets the layer with point objects that need to be clustered
	 */
	public void setLayer(DGeoLayer layer) {
		this.layer = layer;
	}

	public void setSystemCore(ESDACore core) {
		this.core = core;
	}

	/**
	 * Returns a generated description of the method used
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Returns the distance threshold
	 */
	@Override
	public double getDistanceThreshold() {
		return distanceThreshold;
	}

	/**
	 * Returns the required minimum number of objects in the neighbourhood of a core object
	 */
	@Override
	public int getMinNeighbours() {
		return minNeighbours;
	}

	/**
	 * Generates an appropriate distance computer depending on the type
	 * of the objects in the layer.If several distance computers exist,
	 * creates a computer with the given index
	 * @param methodN - the index of the distance computing method in the
	 *                  register of the available methods
	 * @return true if successful
	 */
	abstract public boolean generateDistanceComputer(int methodN);

	/**
	 * Returns the distance computer used to compute distances 
	 */
	public DistanceComputer getDistanceComputer() {
		return distComp;
	}

	/**
	 * Sets the distance computer generated externally
	 */
	public void setDistanceComputer(DistanceComputer computer) {
		distComp = computer;
	}

	/**
	 * Sets the description generated externally
	 */
	public void setDescription(String descr) {
		description = descr;
	}

	/**
	 * Informs if it deals with geographical coordinates (latitudes and longitudes)
	 */
	@Override
	public boolean isGeographic() {
		if (layer != null)
			return layer.isGeographic();
		if (distComp != null)
			return distComp.getCoordinatesAreGeographic();
		return false;
	}

	/**
	 * Can do some preparation of the data for the computing if necessary
	 */
	abstract protected Vector<DClusterObject> prepareData();

	public DClusterObject makeDClusterObject(DGeoObject gobj, int indexInContainer) {
		if (gobj == null)
			return null;
		DClusterObject clObj = new DClusterObject(gobj, gobj.getIdentifier(), indexInContainer);
		return clObj;
	}

	/**
	 * Must be overridden if the cluster objects do not contain direct references
	 * to DGeoObjects!
	 */
	protected DGeoObject getDGeoObject(DClusterObject clObj) {
		if (clObj == null)
			return null;
		if (clObj.originalObject instanceof DGeoObject)
			return (DGeoObject) clObj.originalObject;
		return null;
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
	 * Whether the distance computers may use the threshold to
	 * reduce the computing time. By default is true.
	 */
	public void setMayUseThresholdInComputingDistances(boolean mayUse) {
		this.mayUseThresholdInComputingDistances = mayUse;
	}

	/**
	 * Runs the clustering tool OPTICS using the specified distance threshold
	 * and the required minimum number of objects in the neighbourhood of a
	 * cluster core object
	 */
	public void doClustering(double distance, int minNeighbours) {
		if (layer == null || distComp == null)
			return;
		distanceThreshold = distance;
		this.minNeighbours = minNeighbours;
		distComp.setCoordinatesAreGeographic(layer.isGeographic());
		distComp.setDistanceThreshold(distanceThreshold);
		long t0 = System.currentTimeMillis();
		Vector<DClusterObject> objects = prepareData();
		if (objects == null || objects.size() < 2)
			return;
		int nObj = layer.getObjectCount();
		long freeMem = Runtime.getRuntime().freeMemory(), needMem = ((long) nObj) * nObj * Double.SIZE / 8;
		if (needMem >= freeMem / 3) {
			System.out.println("Garbage collector started, free memory before: " + freeMem);
			Runtime.getRuntime().gc();
			freeMem = Runtime.getRuntime().freeMemory();
			System.out.println("Garbage collector finished, free memory after: " + freeMem);
		}
		try {
			distMatrix = new double[nObj][nObj];
			for (int i = 0; i < nObj; i++) {
				for (int j = 0; j < nObj; j++)
					if (i == j) {
						distMatrix[i][j] = 0;
					} else {
						distMatrix[i][j] = Double.NaN;
					}
			}
			System.out.println("Clustering: distance matrix constructed!");
		} catch (OutOfMemoryError out) {
			System.out.println("Clustering: not enough memory for distance matrix, need: " + needMem);
			distMatrix = null;
		}
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
	 * Used for clustering
	 */
	@Override
	public double distance(DClusterObject o1, DClusterObject o2) {
		if (o1 == null || o2 == null)
			return Double.POSITIVE_INFINITY;
		if (distMatrix != null && o1.idx >= 0 && o2.idx >= 0 && !Double.isNaN(distMatrix[o1.idx][o2.idx]))
			return distMatrix[o1.idx][o2.idx];
		double d = distComp.findDistance(o1.originalObject, o2.originalObject, mayUseThresholdInComputingDistances);
		if (distMatrix != null && o1.idx >= 0 && o2.idx >= 0) {
			distMatrix[o1.idx][o2.idx] = distMatrix[o2.idx][o1.idx] = d;
		}
		return d;
	}

	/**
	 * Erases the values Double.POSITIVE_INFINITY in the distance matrix
	 */
	public void eraseInfinitiesInDistanceMatrix() {
		if (distMatrix != null) {
			for (int i = 0; i < distMatrix.length; i++) {
				for (int j = 0; j < distMatrix.length; j++)
					if (i != j && Double.isInfinite(distMatrix[i][j])) {
						distMatrix[i][j] = Double.NaN;
					}
			}
		}
	}

	/**
	 * Returns the result of clustering as DataTable
	 */
	@Override
	public DataTable getResult() {
		return clTable;
	}

	/**
	 * Used for clustering
	 */
	@Override
	public Collection<DClusterObject> neighbors(DClusterObject core, Collection<DClusterObject> objects, double eps) {
		if (objects == null || objects.size() < 1)
			return null;

		double attrDist[] = null;
		if (distAttr != null) {
			int oIdxs[] = new int[objects.size()];
			int k = 0;
			for (DClusterObject o : objects) {
				oIdxs[k++] = o.idx;
			}
			attrDist = distAttr.getDistancesByAttributes(core.idx, oIdxs);
			if (attrDist == null)
				return null;
		}

		Vector<ObjectWithMeasure> neighborsWithDist = new Vector<ObjectWithMeasure>(100, 100);

		int k = -1;
		for (DClusterObject o : objects) {
			++k;
			if (!o.equals(core)) {
				double aDist = 0;
				if (attrDist != null) {
					if (attrDist[k] > 1) {
						continue; //too far in the attribute space
					}
					aDist *= eps;
				}
				double dist = distance(o, core);
				if (dist > eps) {
					continue; //too far in the geographic space or space-time 
				}
				if (aDist > 0) {
					dist = Math.sqrt(dist * dist + aDist * aDist);
				}
				if (dist <= eps) {
					ObjectWithMeasure om = new ObjectWithMeasure(o, dist);
					neighborsWithDist.addElement(om);
				}
			}
		}

		if (neighborsWithDist.size() > 1) {
			QSortAlgorithm.sort(neighborsWithDist);
		}
		Vector<DClusterObject> neighbors = new Vector<DClusterObject>(neighborsWithDist.size(), 10);
		for (int i = 0; i < neighborsWithDist.size(); i++) {
			neighbors.addElement((DClusterObject) neighborsWithDist.elementAt(i).obj);
		}

		return neighbors;
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
			clTable.setName("Clustering results for " + layer.getName());
			clTable.setEntitySetIdentifier(layer.getEntitySetIdentifier());
			clTable.addAttribute("Reachability distance", "reach_dist", AttributeTypes.real);
			clTable.addAttribute("Core distance", "core_dist", AttributeTypes.real);
		}
		DClusterObject clObj = getDClusterObject(o);
		if (clObj == null)
			return;
		DGeoObject gobj = getDGeoObject(clObj);
		if (gobj == null)
			return;
		clObj.coreDistance = o.getCoreDistance();
		clObj.reachabilityDistance = o.getReachabilityDistance();
		if (objectsOrdered == null) {
			objectsOrdered = new Vector<DClusterObject>(layer.getObjectCount(), 10);
		}
		objectsOrdered.addElement(clObj);
		DataRecord rec = new DataRecord(gobj.getIdentifier(), gobj.getLabel());
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
	 * Returns the clustered objects in the order produced by OPTICS (or another algorithm)
	 */
	@Override
	public Vector<DClusterObject> getObjectsOrdered() {
		return objectsOrdered;
	}
}
