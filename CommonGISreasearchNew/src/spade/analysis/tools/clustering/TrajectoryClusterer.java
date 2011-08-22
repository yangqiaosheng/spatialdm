package spade.analysis.tools.clustering;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;

import spade.analysis.tools.distances.TimeTransformationUser;
import spade.analysis.tools.distances.TrajectoryDistancesRegister;
import spade.lib.util.GeoDistance;
import spade.time.Date;
import spade.time.TimeCount;
import spade.time.TimeMoment;
import spade.time.TimeReference;
import spade.vis.database.SpatialEntity;
import spade.vis.database.TimeFilter;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DMovingObject;
import spade.vis.dmap.TrajectoryObject;
import spade.vis.geometry.RealPoint;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.strtree.STRtree;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 20-Apr-2007
 * Time: 16:35:57
 * Uses the clustering method OPTICS to cluster trajectories taking into
 * account distances between them in space and time
 */
public class TrajectoryClusterer extends LayerClusterer {
	/**
	 * Indicates whether the earliest and latest time moments in the layer
	 * and the maximum trajectory duration need to be computed
	 */
	protected boolean useLayerStartEnd = false;
	/**
	 * Indicates how the times should be treated and transformed
	 */
	protected int timeMode = TrajectoryObject.TIME_NOT_TRANSFORMED;
	/**
	 * The duration of the longest trajectory
	 */
	protected long maxDuration = 0;
	/**
	 * The earliest and latest time moments in the layer
	 */
	protected TimeMoment start = null, end = null;
	/**
	 * A STRTree index to query teh points spatially
	 */
	protected STRtree index = null;

	/**
	 * Returns its parameters, which are necessary and sufficient
	 * for setting up the DistanceComputer. Each parameter has a name,
	 * which is used as a key in the HashMap.
	 * The values of the parameters must be stored in the hashmap as strings!
	 * This is necessary for saving the parameters in a text file.
	 * If the argument is not null, adds its parameter-value pairs to the
	 * given hashmap.
	 */
	@Override
	public HashMap getParameters(HashMap params) {
		params = super.getParameters(params);
		if (params == null) {
			params = new HashMap(20);
		}
		params.put("useLayerStartEnd", new Boolean(useLayerStartEnd).toString());
		params.put("timeMode", String.valueOf(timeMode));
		params.put("maxDuration", String.valueOf(maxDuration));
		if (start != null) {
			if (start instanceof Date) {
				params.put("dateScheme", ((Date) start).scheme);
			}
			params.put("start", start.toString());
			if (end != null) {
				params.put("end", end.toString());
			}
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
		super.setup(params);
		String val = (String) params.get("useLayerStartEnd");
		if (val != null) {
			useLayerStartEnd = Boolean.parseBoolean(val);
		}
		val = (String) params.get("timeMode");
		if (val != null) {
			try {
				timeMode = Integer.parseInt(val);
			} catch (NumberFormatException e) {
			}
		}
		val = (String) params.get("maxDuration");
		if (val != null) {
			try {
				maxDuration = Integer.parseInt(val);
			} catch (NumberFormatException e) {
			}
		}
		String scheme = (String) params.get("dateScheme");
		val = (String) params.get("start");
		if (val != null)
			if (scheme != null) {
				Date d = new Date();
				d.setDateScheme(scheme);
				d.setMoment(val);
				start = d;
			} else {
				TimeCount t = new TimeCount();
				t.setMoment(val);
				start = t;
			}
		val = (String) params.get("end");
		if (val != null)
			if (scheme != null) {
				Date d = new Date();
				d.setDateScheme(scheme);
				d.setMoment(val);
				end = d;
			} else {
				TimeCount t = new TimeCount();
				t.setMoment(val);
				end = t;
			}
	}

	/**
	 * Makes a copy of this clusterer, ignoring any results obtained so far
	 */
	@Override
	public LayerClusterer getCopy() {
		TrajectoryClusterer trCl = new TrajectoryClusterer();
		copyFields(trCl);
		trCl.useLayerStartEnd = useLayerStartEnd;
		trCl.timeMode = timeMode;
		trCl.maxDuration = maxDuration;
		trCl.start = start;
		trCl.end = end;
		return trCl;
	}

	/**
	 * Returns a list (vector) of names of available distance functions
	 */
	@Override
	public Vector getDistanceComputerNames() {
		Vector names = new Vector(TrajectoryDistancesRegister.getMethodCount(), 1);
		for (int i = 0; i < TrajectoryDistancesRegister.getMethodCount(); i++) {
			names.addElement(TrajectoryDistancesRegister.getMethodName(i));
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
		distComp = TrajectoryDistancesRegister.getMethodInstance(methodN);
		if (distComp == null) {
			core.getUI().showMessage("Failed to construct an instance of " + TrajectoryDistancesRegister.getMethodClassName(methodN), true);
			return false;
		}
		description = "Distance function: " + distComp.getMethodName();
		distComp.askParameters();
		String str = distComp.getParameterDescription();
		if (str != null) {
			description += "; " + str;
		}
		if (distComp instanceof TimeTransformationUser) {
			timeMode = ((TimeTransformationUser) distComp).getTimeTransformationType();
		}
		return true;
	}

	@Override
	public DClusterObject makeDClusterObject(DGeoObject gobj, int indexInContainer) {
		if (gobj == null)
			return null;
		if (!(gobj instanceof DMovingObject))
			return null;
		DMovingObject mobj = (DMovingObject) gobj;
		TrajectoryObject trObj = new TrajectoryObject();
		trObj.mobj = mobj;
		switch (timeMode) {
		case TrajectoryObject.TIME_RELATIVE_ENDS:
			trObj.times = transformTimesSynchEnds(trObj.mobj);
			break;
		}
		DClusterObject clObj = new DClusterObject(trObj, mobj.getIdentifier(), indexInContainer);
		return clObj;
	}

	/**
	 * Can do some preparation of the data for the computing if necessary
	 */
	@Override
	protected Vector<DClusterObject> prepareData() {
		if (timeMode != TrajectoryObject.TIME_NOT_TRANSFORMED && useLayerStartEnd) {
			for (int i = 0; i < layer.getObjectCount(); i++)
				if (layer.isObjectActive(i) && (layer.getObjectAt(i) instanceof DMovingObject)) {
					DMovingObject mobj = (DMovingObject) layer.getObjectAt(i);
					TimeMoment t1 = mobj.getStartTime(), t2 = mobj.getEndTime();
					if (t1 == null || t2 == null) {
						continue;
					}
					long dur = t2.subtract(t1);
					if (maxDuration < dur) {
						maxDuration = dur;
					}
					if (start == null || start.compareTo(t1) > 0) {
						start = t1;
					}
					if (end == null || end.compareTo(t2) < 0) {
						end = t2;
					}
				}
		}
		TimeFilter timeFilter = layer.getTimeFilter();
		TimeMoment t1 = null, t2 = null;
		boolean timeFiltered = false;
		if (timeFilter != null) {
			t1 = timeFilter.getFilterPeriodStart();
			if (t1 != null) {
				t2 = timeFilter.getFilterPeriodEnd();
				timeFiltered = true;
				if (start == null || start.compareTo(t1) < 0) {
					start = t1.getCopy();
				}
				if (end == null || end.compareTo(t2) > 0) {
					end = t2.getCopy();
				}
			}
		}
		Vector<DClusterObject> objects = new Vector<DClusterObject>();
		for (int i = 0; i < layer.getObjectCount(); i++)
			if (layer.isObjectActive(i) && (layer.getObject(i) instanceof DMovingObject)) {
				DMovingObject mobj = (DMovingObject) layer.getObject(i);
				if (timeFiltered) {
					mobj = (DMovingObject) mobj.getObjectCopyForTimeInterval(t1, t2);
				}
				if (mobj == null) {
					continue;
				}
				objects.addElement(makeDClusterObject(mobj, i));
			}

// 	Sax: Construct also an Index for the kNN query
		if (distComp.usesMultiplePoints()) {
			if (core != null && core.getUI() != null) {
				core.getUI().showMessage("Constructing an index for optimizing the clustering...", false);
			}
			index = new STRtree(50);
			double rad = distanceThreshold / 50;
			if (layer.isGeographic()) {
				rad = GeoDistance.distToAngle(rad);
			}
			//log.debug("Start constructing the tree.");
			//    Envelope env = new Envelope();
			for (DClusterObject object : objects) {
				DMovingObject mobj = ((TrajectoryObject) object.originalObject).mobj;
				int nPositions = mobj.getPositionCount();
				for (int j = 0; j < nPositions; j++) {
					SpatialEntity se = mobj.getPosition(j);
					RealPoint rp = se.getCentre();
					Envelope e = new Envelope(rp.x - rad, rp.x + rad, rp.y - rad, rp.y + rad);
					//			env.expandToInclude(e);
					index.insert(e, object);
					//			log.debug("Adding " + object.hashCode() + " " +e + " " + e.getWidth() + " " + e.getHeight());
				}
			}
			//    log.debug("Finished tree construction: " + env + " " + env.getWidth() + " " + env.getHeight());

			if (core != null && core.getUI() != null) {
				core.getUI().showMessage("Index constructed; clustering in progress...", false);
			}
		}
		return objects;
	}

	@Override
	public Collection neighbors(DClusterObject core, Collection objects, double eps) {
		// Perform a pruning using the index
		if (index != null && (core.originalObject instanceof TrajectoryObject)) {
			//log.debug("CLUSTERING: query the neigh of " + core);
			HashSet<DClusterObject> candidates = new HashSet<DClusterObject>();
			DMovingObject mobj = ((TrajectoryObject) core.originalObject).mobj;
			int nPositions = mobj.getPositionCount();
			double rad = eps;
			if (layer.isGeographic()) {
				rad = GeoDistance.distToAngle(eps);
			}
			for (int i = 0; i < nPositions; i++) {
				RealPoint rp = mobj.getPositionAsPoint(i);
				Envelope e = new Envelope(rp.x - rad, rp.x + rad, rp.y - rad, rp.y + rad);

				List<DClusterObject> l = index.query(e);
				candidates.addAll(l);
//				for (TrajectoryObject obj : l) {
//						candidates.add(obj);
//				}
			}
			//log.debug("CLUSTERING: selected " + candidates.size() + " trajs out of " + objects.size() );
			return super.neighbors(core, candidates, eps);
		} else
			return super.neighbors(core, objects, eps);
	}

	/**
	 * Transforms the absolute times into relative in the following way:
	 * 1) the end time of the trajectory is assumed to be equal to maxDuration
	 * 2) the remaining times are transformed to (maxDuration-<distance to the end time>)
	 */
	protected long[] transformTimesSynchEnds(DMovingObject mobj) {
		if (mobj == null)
			return null;
		Vector track = mobj.getTrack();
		if (track == null || track.size() < 1)
			return null;
		long tt[] = new long[track.size()];
		TimeMoment endTime = mobj.getEndTime();
		for (int i = 0; i < track.size(); i++) {
			tt[i] = -1;
			SpatialEntity spe = (SpatialEntity) track.elementAt(i);
			TimeReference tref = spe.getTimeReference();
			if (tref == null) {
				continue;
			}
			TimeMoment t = tref.getValidUntil();
			if (t == null) {
				continue;
			}
			long dif = endTime.subtract(t);
			tt[i] = maxDuration - dif;
		}
		return tt;
	}

	@Override
	protected DGeoObject getDGeoObject(DClusterObject o) {
		if (o == null)
			return null;
		if (o.originalObject instanceof TrajectoryObject)
			return ((TrajectoryObject) o.originalObject).mobj;
		if (o.originalObject instanceof DGeoObject)
			return (DGeoObject) o.originalObject;
		return null;
	}
}
