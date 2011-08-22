package spade.analysis.tools.similarity;

import java.util.Vector;

import spade.analysis.tools.distances.TimeTransformationUser;
import spade.analysis.tools.distances.TrajectoryDistancesRegister;
import spade.lib.basicwin.SelectDialog;
import spade.time.TimeMoment;
import spade.time.TimeReference;
import spade.vis.database.SpatialEntity;
import spade.vis.dmap.DMovingObject;
import spade.vis.dmap.TrajectoryObject;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 24-Apr-2007
 * Time: 12:13:04
 */
public class TrajectorySimilarityComputer extends SimilarityComputer {
	/**
	 * The duration of the longest trajectory
	 */
	protected long maxDuration = 0;
	/**
	 * The earliest and latest time moments in the layer
	 */
	protected TimeMoment start = null, end = null;
	/**
	 * Contains instances of TrajectoryObject prepared for the computation
	 */
	protected Vector trajectories = null;
	/**
	 * A trajectory object constructed from the selected geographical object
	 */
	protected TrajectoryObject selTr = null;
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
	 * Generates an appropriate distance computer depending on the type
	 * of the objects in the layer. May ask the user for parameters of the
	 * similarity computation. Returns true if successful.
	 */
	@Override
	protected boolean getDistanceComputer() {
		SelectDialog selDia = new SelectDialog(core.getUI().getMainFrame(), "Distance function", "Choose a distance function:");
		for (int i = 0; i < TrajectoryDistancesRegister.getMethodCount(); i++) {
			selDia.addOption(TrajectoryDistancesRegister.getMethodName(i), String.valueOf(i + 1), i == 0);
		}
		selDia.show();
		if (selDia.wasCancelled())
			return false;
		int n = selDia.getSelectedOptionN();
		distComp = TrajectoryDistancesRegister.getMethodInstance(n);
		if (distComp == null) {
			core.getUI().showMessage("Failed to construct an instance of " + TrajectoryDistancesRegister.getMethodClassName(n), true);
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

	/**
	 * Can do some preparation of the data for the computing if necessary
	 */
	@Override
	protected void prepareData() {
		if (timeMode != TrajectoryObject.TIME_NOT_TRANSFORMED && useLayerStartEnd) {
			for (int i = 0; i < layer.getObjectCount(); i++)
				if (layer.getObjectAt(i) instanceof DMovingObject) {
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
		trajectories = new Vector(layer.getObjectCount(), 1);
		for (int i = 0; i < layer.getObjectCount(); i++)
			if (layer.getObject(i) instanceof DMovingObject) {
				TrajectoryObject trObj = new TrajectoryObject();
				trObj.mobj = (DMovingObject) layer.getObject(i);
				switch (timeMode) {
				case TrajectoryObject.TIME_RELATIVE_ENDS:
					trObj.times = transformTimesSynchEnds(trObj.mobj);
					break;
				}
				trajectories.addElement(trObj);
				if (selObj != null && selObj.equals(trObj.mobj)) {
					selTr = trObj;
				}
			} else {
				trajectories.addElement(null);
			}
		if (selTr == null && selObj != null) {
			TrajectoryObject trObj = new TrajectoryObject();
			trObj.mobj = (DMovingObject) selObj;
			switch (timeMode) {
			case TrajectoryObject.TIME_RELATIVE_ENDS:
				trObj.times = transformTimesSynchEnds(trObj.mobj);
				break;
			}
		}
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

	/**
	 * Computes the distance for the object with the given index in the layer.
	 */
	@Override
	protected double getDistanceForObject(int idx) {
		if (trajectories == null || selTr == null || idx >= trajectories.size() || trajectories.elementAt(idx) == null)
			return Double.NaN;
		return distComp.findDistance(selTr, trajectories.elementAt(idx), false);
	}

	/**
	 * Computes the distance between the objects with the given indexes in the layer.
	 */
	@Override
	protected double getDistanceBetweenObjects(int idx1, int idx2) {
		if (trajectories == null || idx1 < 0 || idx2 < 0 || idx1 >= trajectories.size() || trajectories.elementAt(idx1) == null || idx2 >= trajectories.size() || trajectories.elementAt(idx2) == null)
			return Double.NaN;
		return distComp.findDistance(trajectories.elementAt(idx1), trajectories.elementAt(idx2), false);
	}
}
