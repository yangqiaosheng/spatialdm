package spade.analysis.tools.clustering;

import it.unipi.di.sax.kmedoids.ClusterUtilities;
import it.unipi.di.sax.kmedoids.KMedoids;
import it.unipi.di.sax.optics.DistanceMeter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Jan 28, 2009
 * Time: 1:52:14 PM
 * For a given sequence of ordered objects produces
 * round clusters with the given maximal radius.
 */
public class RoundClustersProducer<T> {
	protected ClusterUtilities<T> utils = new ClusterUtilities<T>();
	protected HashMap<T, ArrayList<T>> clusters = null;
	protected ArrayList<T> lastCluster = null;
	protected T lastCentroid = null;

	public HashMap<T, ArrayList<T>> getRoundClusters(Vector<T> objectsOrdered, int fromIdx, int toIdx, DistanceMeter<T> dm, double eps) {
		clusters = null;
		lastCluster = null;
		lastCentroid = null;
		if (objectsOrdered == null || objectsOrdered.size() < fromIdx + 1 || dm == null || eps <= 0)
			return null;
		if (fromIdx < 0) {
			fromIdx = 0;
		}
		if (toIdx >= objectsOrdered.size()) {
			toIdx = objectsOrdered.size() - 1;
		}
		if (fromIdx > toIdx)
			return null;

		clusters = new HashMap<T, ArrayList<T>>();
		addNewCluster(objectsOrdered.elementAt(fromIdx));

		for (int i = fromIdx + 1; i <= toIdx; i++) {
			T current = objectsOrdered.elementAt(i);
			if (dm.distance(current, lastCentroid) < eps) {
				lastCluster.add(current);
				T newCentroid = utils.getCentroid(lastCluster, dm);
				if (!newCentroid.equals(lastCentroid)) {
					clusters.remove(lastCentroid);
					lastCentroid = newCentroid;
					clusters.put(lastCentroid, lastCluster);
				}
				continue;
			}

			// select the nearest centroid in the HashMap
			T nearestCentroid = null;
			double minDistance = Double.POSITIVE_INFINITY;

			for (T c : clusters.keySet()) {
				double d = dm.distance(current, c);
				if (d < eps && d < minDistance) {
					minDistance = d;
					nearestCentroid = c;
				}
			}
			if (nearestCentroid != null) {
				// update the cluster whose centroid is nearestCentroid
				lastCentroid = nearestCentroid;
				lastCluster = clusters.get(lastCentroid);
				lastCluster.add(current);
				T newCentroid = utils.getCentroid(lastCluster, dm);
				if (!newCentroid.equals(lastCentroid)) {
					clusters.remove(lastCentroid);
					lastCentroid = newCentroid;
					clusters.put(lastCentroid, lastCluster);
				}
			} else {
				// create a new cluster whose centroid is current
				addNewCluster(current);
			}
		}
		if (clusters.size() < 1)
			return null;
		if (clusters.size() > 1) {
			//try to attach the clusters consisting of isolated objects to other clusters
			boolean changed = false;
			do {
				changed = false;
				T isolated = null;
				T nearestCentroid = null;
				double minDistance = Double.POSITIVE_INFINITY;
				for (T c : clusters.keySet())
					if (clusters.get(c).size() < 2) {
						isolated = c;
						for (T c1 : clusters.keySet())
							if (!isolated.equals(c1)) {
								double d = dm.distance(isolated, c1);
								if (d < eps && d < minDistance) {
									minDistance = d;
									nearestCentroid = c1;
								}
							}
					}
				if (isolated != null && nearestCentroid != null) {
					clusters.remove(isolated);
					ArrayList<T> cluster = clusters.get(nearestCentroid);
					cluster.add(isolated);
					T newCentroid = utils.getCentroid(cluster, dm);
					if (!newCentroid.equals(nearestCentroid)) {
						clusters.remove(nearestCentroid);
						clusters.put(newCentroid, cluster);
					}
					changed = true;
				}
			} while (changed && clusters.size() > 1);
			//try to re-distribute the objects among the clusters
			ArrayList<T> objects = new ArrayList<T>(toIdx - fromIdx + 1);
			ArrayList<T> seeds = new ArrayList<T>(clusters.size());
			for (T c : clusters.keySet()) {
				seeds.add(c);
				ArrayList<T> members = clusters.get(c);
				objects.addAll(members);
			}
			KMedoids<T> km = new KMedoids<T>(dm);
			HashMap<T, ArrayList<T>> result = km.doClustering(objects, seeds);
			if (result != null && result.size() > 1)
				return result;
		}
		return clusters;
	}

	private void addNewCluster(T centroid) {
		if (centroid == null)
			return;
		lastCluster = new ArrayList<T>();
		lastCluster.add(centroid);
		lastCentroid = centroid;
		clusters.put(lastCentroid, lastCluster);
	}
}
