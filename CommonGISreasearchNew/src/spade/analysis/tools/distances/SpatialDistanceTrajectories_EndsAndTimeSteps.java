package spade.analysis.tools.distances;

import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Label;
import java.awt.Panel;
import java.util.HashMap;
import java.util.Vector;

import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.InputIntPanel;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.OKDialog;
import spade.vis.database.SpatialEntity;
import spade.vis.dmap.TrajectoryObject;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: 26-Apr-2007
 * Time: 12:47:57
 * To change this template use File | Settings | File Templates.
 */
public class SpatialDistanceTrajectories_EndsAndTimeSteps extends SpatialDistance implements TimeTransformationUser {
	protected int step = 1;
	protected boolean adjustStart = false;

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
		Panel p = new Panel(new ColumnLayout());
		InputIntPanel iip = null;
		p.add(iip = new InputIntPanel("Time step (in minutes)?", 1, 1, 10000000, "Method of computing distances: " + methodName));
		p.add(new Line(false));
		p.add(new Label("Adjusting trajectories by:"));
		CheckboxGroup cbg = new CheckboxGroup();
		Checkbox cb[] = new Checkbox[2];
		p.add(cb[0] = new Checkbox("Starts", false, cbg));
		p.add(cb[1] = new Checkbox("Ends", true, cbg));
		p.add(new Line(false));
		OKDialog dia = new OKDialog(CManager.getAnyFrame(), "Method parameters", false);
		dia.addContent(p);
		dia.show();
		if (dia.wasCancelled())
			return false;
		step = iip.getEnteredValue();
		adjustStart = cb[0].getState();
		/*
		step = Dialogs.askForIntValue(CManager.getAnyFrame(),
		      "Time step (in minutes)?",1,1,10000000,
		      "Method of computing distances: "+methodName,"Method parameter",false);
		*/
		return step > 0;
	}

	/**
	 * Returns a string describing the current parameter settings of the method.
	 * May return null if no additional parameters are used.
	 */
	@Override
	public String getParameterDescription() {
		return "Time step = " + step + " minutes; adjustment: " + ((adjustStart) ? "starts" : "ends");
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
		params.put("adjustStart", new Boolean(adjustStart).toString());
		params.put("step", String.valueOf(step));
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
		String val = (String) params.get("adjustStart");
		if (val != null) {
			adjustStart = Boolean.parseBoolean(val);
		}
		val = (String) params.get("step");
		if (val != null) {
			try {
				step = Integer.parseInt(val);
			} catch (NumberFormatException e) {
			}
		}
	}

	/**
	 * Returns the required type of time transformation
	 */
	@Override
	public int getTimeTransformationType() {
		return TrajectoryObject.TIME_RELATIVE_ENDS;
	}

	/**
	 * Determines the distance between two trajectories
	 */
	public double distance(TrajectoryObject obj1, TrajectoryObject obj2, boolean useThreshold) {
		useThreshold = useThreshold && !Double.isNaN(distanceThreshold);
		if (obj1 == null || obj2 == null)
			return (useThreshold) ? Double.POSITIVE_INFINITY : Double.NaN;
		Vector tr1 = obj1.mobj.getTrack(), tr2 = obj2.mobj.getTrack();
		SpatialEntity sp1 = (SpatialEntity) tr1.elementAt(0);
		SpatialEntity sp2 = (SpatialEntity) tr2.elementAt(0);
		double d = distance(sp1.getGeometry(), sp2.getGeometry(), true), dsum = d;
		//if (useThreshold && (d>distanceThreshold))
		//  return Double.POSITIVE_INFINITY;
		sp1 = (SpatialEntity) tr1.elementAt(tr1.size() - 1);
		sp2 = (SpatialEntity) tr2.elementAt(tr2.size() - 1);
		d = distance(sp1.getGeometry(), sp2.getGeometry(), true);
		//if (useThreshold && (d>distanceThreshold))
		//  return Double.POSITIVE_INFINITY;
		dsum += d;
		if (useThreshold && (dsum > 2 * distanceThreshold))
			return Double.POSITIVE_INFINITY;
		int idx1[] = obj1.getAbsTimeIdx(60 * step, adjustStart), idx2[] = obj2.getAbsTimeIdx(60 * step, adjustStart);
		int N = Math.max(idx1.length, idx2.length), nn = 0;
		for (int i = 0; i < N; i++) {
			if (i >= idx1.length || i >= idx2.length) {
				if (!Double.isNaN(distanceThreshold)) {
					dsum += 2 * distanceThreshold;
					nn++;
				}
			} else {
				int i1 = idx1[i], i2 = idx2[i];
				if (i1 > 0 && i2 > 0) { // sometimes it is impossible to get N midpoints without the use of endpoints
					sp1 = (SpatialEntity) tr1.elementAt(i1);
					sp2 = (SpatialEntity) tr2.elementAt(i2);
					d = distance(sp1.getGeometry(), sp2.getGeometry(), true);
					//if (useThreshold && (d>distanceThreshold))
					//  return Double.POSITIVE_INFINITY;
					dsum += d;
					nn++;
				} else {
					if (!Double.isNaN(distanceThreshold)) {
						dsum += 2 * distanceThreshold;
						nn++;
					}
				}
			}
		}
		dsum /= (2 + nn);
		if (useThreshold && (dsum > distanceThreshold))
			return Double.POSITIVE_INFINITY;
		else
			return dsum;
	}

	/**
	 * Determines the distance between two objects taking into account the
	 * distance threshold. If in the process of computation it turns out that the
	 * distance is higher than the threshold, the method may stop further
	 * computing and return an arbitrary value greater than the threshold.
	 * The objects must be instances of TrajectoryObject.
	 */
	@Override
	public double findDistance(Object obj1, Object obj2, boolean useThreshold) {
		useThreshold = useThreshold && !Double.isNaN(distanceThreshold);
		if (obj1 == null || obj2 == null)
			return (useThreshold) ? Double.POSITIVE_INFINITY : Double.NaN;
		if (!(obj1 instanceof TrajectoryObject) || !(obj2 instanceof TrajectoryObject))
			return (useThreshold) ? Double.POSITIVE_INFINITY : Double.NaN;
		return distance((TrajectoryObject) obj1, (TrajectoryObject) obj2, useThreshold);
	}

}
