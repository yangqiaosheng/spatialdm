package spade.analysis.tools.clustering;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;

import spade.analysis.tools.distances.SpatialDistancePoints;
import spade.analysis.tools.distances.SpatioTemporalDistancePoints;
import spade.lib.util.GeoDistance;
import spade.vis.geometry.Geometry;
import spade.vis.space.GeoObject;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.strtree.STRtree;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 19-Apr-2007
 * Time: 12:16:54
 * Uses the clustering method OPTICS to cluster point objects taking into
 * account distances between them
 */
public class PointLayerClusterer extends LayerClusterer {
	/**
	 * A STRTree index to query teh points spatially
	 */
	protected STRtree index = null;

	/**
	 * Makes a copy of this clusterer, ignoring any results obtained so far
	 */
	@Override
	public LayerClusterer getCopy() {
		PointLayerClusterer pCl = new PointLayerClusterer();
		copyFields(pCl);
		return pCl;
	}

	/**
	 * Returns a list (vector) of names of available distance functions
	 */
	@Override
	public Vector getDistanceComputerNames() {
		Vector names = new Vector(1, 1);
		names.addElement("Spatial distance between points");
		if (layer.hasTimeReferences()) {
			names.addElement("Spatio-temporal distance between points");
		}
		return names;
	}

	/**
	 * Generates an appropriate distance computer depending on the type
	 * of the objects in the layer.If several distance computers exist,
	 * creates a computer with the given index
	 * @param methodN - the index of the distance computing method in the
	 *                  register of the available methods
	 * @return true if successful
	 */
	@Override
	public boolean generateDistanceComputer(int methodN) {
		switch (methodN) {
		case 1:
			distComp = new SpatioTemporalDistancePoints();
			description = "Distance: spatio-temporal distance";
			break;
		default:
			distComp = new SpatialDistancePoints();
			description = "Distance: spatial distance";
		}
		description = "Distance function: " + distComp.getMethodName();
		distComp.askParameters();
		String str = distComp.getParameterDescription();
		if (str != null) {
			description += "; " + str;
		}
		return true;
	}

	/**
	 * Can do some preparation of the data for the computing if necessary
	 */
	@Override
	protected Vector<DClusterObject> prepareData() {
		Vector<DClusterObject> objects = new Vector<DClusterObject>();
		for (int i = 0; i < layer.getObjectCount(); i++)
			if (layer.isObjectActive(i)) {
				GeoObject obj = layer.getObjectAt(i);
				objects.addElement(new DClusterObject(obj, obj.getIdentifier(), i));
			}
		if (objects == null || objects.size() < 1000)
			return objects;
		if (core != null && core.getUI() != null) {
			core.getUI().showMessage("Constructing an index for optimizing the clustering...", false);
		}
		index = new STRtree(50);
		double rad = distanceThreshold / 50;
		if (layer.isGeographic()) {
			rad = GeoDistance.distToAngle(rad);
		}
		for (DClusterObject object : objects) {
			Geometry geom = ((GeoObject) object.originalObject).getGeometry();
			float ct[] = geom.getCentroid();
			Envelope e = new Envelope(ct[0] - rad, ct[0] + rad, ct[1] - rad, ct[1] + rad);
			index.insert(e, object);
		}
		if (core != null && core.getUI() != null) {
			core.getUI().showMessage("Index constructed; clustering in progress...", false);
		}
		return objects;
	}

	/**
	 * Used for clustering
	 */
	@Override
	public Collection<DClusterObject> neighbors(DClusterObject core, Collection<DClusterObject> objects, double eps) {
		// Perform a pruning using the index
		if (index != null) {
			//log.debug("CLUSTERING: query the neigh of " + core);
			HashSet<DClusterObject> candidates = new HashSet<DClusterObject>();
			GeoObject obj = (GeoObject) core.originalObject;
			double rad = eps;
			if (layer.isGeographic()) {
				rad = GeoDistance.distToAngle(eps);
			}
			Geometry geom = obj.getGeometry();
			float ct[] = geom.getCentroid();
			Envelope e = new Envelope(ct[0] - rad, ct[0] + rad, ct[1] - rad, ct[1] + rad);
			List<DClusterObject> l = index.query(e);
			candidates.addAll(l);
			return super.neighbors(core, candidates, eps);
		} else
			return super.neighbors(core, objects, eps);
	}
}
