package spade.analysis.tools.distances;

import java.util.HashMap;
import java.util.Vector;

import spade.lib.basicwin.CManager;
import spade.lib.basicwin.Dialogs;
import spade.time.TimeMoment;
import spade.vis.database.SpatialEntity;
import spade.vis.dmap.TrajectoryObject;
import spade.vis.geometry.RealPoint;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: 20-Aug-2007
 * Time: 10:18:19
 * Estimates the distance between two trajectories according to their shapes
 * and temporal synchronization of the movements.
 */
public class SpatialDistanceTrajectories_Shapes_Synchro extends SpatialDistanceTrajectories {
	/**
	 * The time step for finding corresponding positions
	 */
	public int timeStep = 1;

	/**
	 * Informs whether multiple points (e.g. of trajectories) are
	 * used for computing distances.
	 * Returns true.
	 */
	@Override
	public boolean usesMultiplePoints() {
		return true;
	}

	/**
	 * If necessary, asks the user about additional parameters required for
	 * this Distance Computer. Returns true if successful.
	 */
	@Override
	public boolean askParameters() {
		timeStep = Dialogs.askForIntValue(CManager.getAnyFrame(), "Time step (in smallest units)?", 1, 1, 10000000, "Method of computing distances: " + methodName, "Method parameter", false);
		return timeStep > 0;
	}

	/**
	 * Returns a string describing the current parameter settings of the method.
	 * May return null if no additional parameters are used.
	 */
	@Override
	public String getParameterDescription() {
		return "Time step = " + timeStep;
	}

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
		params.put("timeStep", String.valueOf(timeStep));
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
		String val = (String) params.get("timeStep");
		if (val != null) {
			try {
				timeStep = Integer.parseInt(val);
			} catch (NumberFormatException e) {
			}
		}
	}

	/**
	 * Determines the distance between two trajectories.
	 * First tries to find the common point (closest positions
	 * between the trajectories).
	 */
	@Override
	public double distance(TrajectoryObject obj1, TrajectoryObject obj2, boolean useThreshold) {
		useThreshold = useThreshold && !Double.isNaN(distanceThreshold);
		if (obj1 == null || obj2 == null)
			return (useThreshold) ? Double.POSITIVE_INFINITY : Double.NaN;
		Vector tr1 = obj1.mobj.getTrack(), tr2 = obj2.mobj.getTrack();
		if (tr1 == null || tr2 == null || tr1.size() < 1 || tr2.size() < 1)
			return (useThreshold) ? Double.POSITIVE_INFINITY : Double.NaN;
		if (obj1.mobj.getStartTime() == null || obj2.mobj.getStartTime() == null)
			return (useThreshold) ? Double.POSITIVE_INFINITY : Double.NaN;
		if (timeStep < 1) {
			timeStep = 1;
		}
		double penaltyDist = 0, commonDist = 0;
		int i1 = 0, i2 = 0;
		double dist = 0;
		int np = 0;
/*
    if ((obj1.mobj.getIdentifier().equals("18") || obj2.mobj.getIdentifier().equals("18")) &&
        (obj1.mobj.getIdentifier().equals("153") || obj2.mobj.getIdentifier().equals("153"))) {
      System.out.println("id1=["+obj1.mobj.getIdentifier()+"], id2=["+obj2.mobj.getIdentifier()+"]");
    }
*/
		//first try to find the closest positions of the trajectories
		//starting from their beginnings
		RealPoint p1 = null, p2 = null;
		TimeMoment t01 = null, t02 = null;
		while (i1 < tr1.size() && i2 < tr2.size() && p1 == null && p2 == null) {
			SpatialEntity sp1 = (SpatialEntity) tr1.elementAt(i1);
			SpatialEntity sp2 = (SpatialEntity) tr2.elementAt(i2);
			double d = distance(sp1.getGeometry(), sp2.getGeometry(), false);
			if (i1 < tr1.size() - 1) {
				int i01 = i1 + 1;
				boolean closer = false;
				do {
					SpatialEntity sp01 = (SpatialEntity) tr1.elementAt(i01);
					double d0 = distance(sp01.getGeometry(), sp2.getGeometry(), false);
					closer = d0 < d;
					if (closer) {
						penaltyDist += distance(sp1.getGeometry(), sp01.getGeometry(), false);
						d = d0;
						i1 = i01;
						sp1 = sp01;
						++i01;
					}
				} while (closer && i01 < tr1.size());
			}
			if (i2 < tr2.size() - 1) {
				int i02 = i2 + 1;
				boolean closer = false;
				do {
					SpatialEntity sp02 = (SpatialEntity) tr2.elementAt(i02);
					double d0 = distance(sp1.getGeometry(), sp02.getGeometry(), false);
					closer = d0 < d;
					if (closer) {
						penaltyDist += distance(sp2.getGeometry(), sp02.getGeometry(), false);
						d = d0;
						i2 = i02;
						sp2 = sp02;
						++i02;
					}
				} while (closer && i02 < tr2.size());
			}
			p1 = sp1.getCentre();
			p2 = sp2.getCentre();
			dist = d;
			np = 1;
			t01 = sp1.getTimeReference().getValidUntil().getCopy();
			t02 = sp2.getTimeReference().getValidUntil().getCopy();
		}
		++i1;
		++i2;
		if (i1 >= tr1.size() || i2 >= tr2.size())
			return (useThreshold) ? Double.POSITIVE_INFINITY : Double.NaN;
		RealPoint commonP = null;
		while (i1 < tr1.size() && i2 < tr2.size()) {
			//find the positions reached in time timeStep from the last common point
			SpatialEntity sp1 = (SpatialEntity) tr1.elementAt(i1);
			RealPoint p = sp1.getCentre();
			p1 = p;
			long timeDiff = sp1.getTimeReference().getValidFrom().subtract(t01);
			while (timeDiff < timeStep && i1 + 1 < tr1.size()) {
				++i1;
				sp1 = (SpatialEntity) tr1.elementAt(i1);
				p = sp1.getCentre();
				p1 = p;
				timeDiff = sp1.getTimeReference().getValidFrom().subtract(t01);
			}
			if (timeDiff > timeStep) {
				--i1;
				sp1 = (SpatialEntity) tr1.elementAt(i1);
				p1 = sp1.getCentre();
			}
			SpatialEntity sp2 = (SpatialEntity) tr2.elementAt(i2);
			p = sp2.getCentre();
			p2 = p;
			timeDiff = sp2.getTimeReference().getValidFrom().subtract(t02);
			while (timeDiff < timeStep && i2 + 1 < tr2.size()) {
				++i2;
				sp2 = (SpatialEntity) tr2.elementAt(i2);
				p = sp2.getCentre();
				p2 = p;
				timeDiff = sp2.getTimeReference().getValidFrom().subtract(t02);
			}
			if (timeDiff > timeStep) {
				--i2;
				sp2 = (SpatialEntity) tr2.elementAt(i2);
				p2 = sp2.getCentre();
			}
			dist += distance(p1, p2, false);
			++np;
			if (useThreshold && dist / np > distanceThreshold)
				return Double.POSITIVE_INFINITY;
			RealPoint pp = new RealPoint((p1.x + p2.x) / 2, (p1.y + p2.y) / 2);
			if (commonP != null) {
				double dd = distance(pp, commonP, false);
				commonDist += dd;
			}
			commonP = pp;
			//penaltyDist-=0.5*Math.min(d1,d2);
			t01.add(timeStep);
			t02.add(timeStep);
			++i1;
			++i2;
		}
		if (np < 1)
			return (useThreshold) ? Double.POSITIVE_INFINITY : Double.NaN;
		RealPoint cpOld = commonP;
		while (i1 < tr1.size()) {
			RealPoint p = ((SpatialEntity) tr1.elementAt(i1)).getCentre();
			penaltyDist += distance(p, commonP, false);
			commonP = p;
			++i1;
		}
		commonP = cpOld;
		while (i2 < tr2.size()) {
			RealPoint p = ((SpatialEntity) tr2.elementAt(i2)).getCentre();
			penaltyDist += distance(p, commonP, false);
			commonP = p;
			++i2;
		}
		dist /= np;
		if (penaltyDist > 0) {
			if (commonDist == 0)
				return (useThreshold) ? Double.POSITIVE_INFINITY : Double.NaN;
			//dist+=penaltyDist;
			dist += penaltyDist * penaltyDist / commonDist;
		}
		//if (penaltyDist>0) dist+=penaltyDist;
		if (useThreshold && dist > distanceThreshold)
			return Double.POSITIVE_INFINITY;
		return dist;
	}
}
