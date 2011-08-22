package spade.analysis.tools.distances;

import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.util.HashMap;
import java.util.Vector;

import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Dialogs;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.OKDialog;
import spade.time.Date;
import spade.vis.database.SpatialEntity;
import spade.vis.dmap.TrajectoryObject;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: Jun 20, 2008
 * Time: 12:13:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class SpatialDistanceTrajectories_EndsAndTimes extends SpatialDistance implements TimeTransformationUser {

	boolean useStarts = true, useEnds = false, absTime = true;
	long maxTDiff = 0;

	/**
	 * If necessary, asks the user about additional parameters required for
	 * this Distance Computer. Returns true if successful.
	 */
	@Override
	public boolean askParameters() {
		Panel p = new Panel(new ColumnLayout());
		p.add(new Label("Method of computing distances: "));
		p.add(new Label(methodName, Label.CENTER));
		p.add(new Line(false));
		p.add(new Label("Geography:"));
		Checkbox cbse[] = new Checkbox[2];
		p.add(cbse[0] = new Checkbox("Starts", true));
		p.add(cbse[1] = new Checkbox("Ends", false));
		p.add(new Line(false));
		p.add(new Label("Time:"));
		CheckboxGroup cbgt = new CheckboxGroup();
		Checkbox cbt[] = new Checkbox[2];
		p.add(cbt[0] = new Checkbox("Absolute", true, cbgt));
		p.add(cbt[1] = new Checkbox("Time of day", false, cbgt));
		p.add(new Label("Threshold for the time difference:"));
		Panel pp = new Panel(new FlowLayout(FlowLayout.RIGHT));
		TextField tfDurDiff = new TextField("600", 5);
		pp.add(tfDurDiff);
		pp.add(new Label("smallest time units"));
		p.add(pp);
		p.add(new Line(false));
		OKDialog dia = new OKDialog(CManager.getAnyFrame(), "Method parameters", false);
		dia.addContent(p);
		dia.show();
		if (dia.wasCancelled())
			return false;
		useStarts = cbse[0].getState();
		useEnds = cbse[1].getState();
		absTime = cbt[0].getState();
		long d = 0;
		String str = tfDurDiff.getText();
		if (str != null) {
			try {
				d = Long.parseLong(str);
			} catch (NumberFormatException e) {
			}
		}
		if (d < 0) {
			d = Dialogs.askForIntValue(CManager.getAnyFrame(), "Threshold for the time difference?", 10, 0, Integer.MAX_VALUE, "Method of computing distances: " + methodName, "Method parameter", false);
		}
		maxTDiff = d;
		return useStarts || useEnds;
	}

	/**
	 * Returns a string describing the current parameter settings of the method.
	 * May return null if no additional parameters are used.
	 */
	@Override
	public String getParameterDescription() {
		String str = null;
		if (useStarts) {
			str = "starts";
		}
		if (useEnds)
			if (str == null) {
				str = "ends";
			} else {
				str += "; ends";
			}
		str += "; time threshold = " + maxTDiff;
		return "Accounted properties: " + str;
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
		params.put("useStarts", new Boolean(useStarts).toString());
		params.put("useEnds", new Boolean(useEnds).toString());
		params.put("absTime", new Boolean(absTime).toString());
		params.put("maxTDiff", String.valueOf(maxTDiff));
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
		String val = (String) params.get("useStarts");
		if (val != null) {
			useStarts = Boolean.parseBoolean(val);
		}
		val = (String) params.get("useEnds");
		if (val != null) {
			useEnds = Boolean.parseBoolean(val);
		}
		val = (String) params.get("absTime");
		if (val != null) {
			absTime = Boolean.parseBoolean(val);
		}
		val = (String) params.get("maxTDiff");
		if (val != null) {
			try {
				maxTDiff = Long.parseLong(val);
			} catch (NumberFormatException e) {
			}
		}
	}

	/**
	 * Returns the required type of time transformation
	 */
	@Override
	public int getTimeTransformationType() {
		return TrajectoryObject.TIME_NOT_TRANSFORMED;
	}

	/**
	 * Determines the distance between two trajectories
	 */
	public double distance(TrajectoryObject obj1, TrajectoryObject obj2, boolean useThreshold) {
		if (obj1 == null || obj2 == null)
			return (useThreshold) ? Double.POSITIVE_INFINITY : Double.NaN;
		Vector tr1 = obj1.mobj.getTrack(), tr2 = obj2.mobj.getTrack();
		double dPoints = 0d, dTime = 0d, dTotal = 0d;
		if (useStarts) {
			SpatialEntity sp1 = (SpatialEntity) tr1.elementAt(0);
			SpatialEntity sp2 = (SpatialEntity) tr2.elementAt(0);
			dPoints = distance(sp1.getGeometry(), sp2.getGeometry(), true);
			if (useThreshold && (dPoints > distanceThreshold))
				return Double.POSITIVE_INFINITY;
			long ddif = 0;
			if (!absTime && (obj1.mobj.getStartTime() instanceof Date)) {
				ddif = Math.abs(((Date) obj1.mobj.getStartTime()).getCopyWithTimeOnly().toNumber() - ((Date) obj2.mobj.getStartTime()).getCopyWithTimeOnly().toNumber());
			} else {
				ddif = Math.abs(obj1.mobj.getStartTime().toNumber() - obj2.mobj.getStartTime().toNumber());
			}
			if (useThreshold && (ddif > maxTDiff))
				return Double.POSITIVE_INFINITY;
			dTime = distanceThreshold * ddif / maxTDiff;
			double d = Math.sqrt(dPoints * dPoints + dTime * dTime);
			if (useEnds) {
				dTotal = d / 2;
			} else {
				dTotal = d;
			}
		}
		if (useEnds) {
			SpatialEntity sp1 = (SpatialEntity) tr1.elementAt(tr1.size() - 1);
			SpatialEntity sp2 = (SpatialEntity) tr2.elementAt(tr2.size() - 1);
			dPoints = distance(sp1.getGeometry(), sp2.getGeometry(), true);
			if (useThreshold && (dPoints > distanceThreshold))
				return Double.POSITIVE_INFINITY;
			//long ddif=Math.abs(obj1.mobj.getEndTime().toNumber()-obj2.mobj.getEndTime().toNumber());
			long ddif = 0;
			if (!absTime && (obj1.mobj.getStartTime() instanceof Date)) {
				ddif = Math.abs(((Date) obj1.mobj.getEndTime()).getCopyWithTimeOnly().toNumber() - ((Date) obj2.mobj.getEndTime()).getCopyWithTimeOnly().toNumber());
			} else {
				ddif = Math.abs(obj1.mobj.getStartTime().toNumber() - obj2.mobj.getStartTime().toNumber());
			}
			if (useThreshold && (ddif > maxTDiff))
				return Double.POSITIVE_INFINITY;
			dTime = distanceThreshold * ddif / maxTDiff;
			double d = Math.sqrt(dPoints * dPoints + dTime * dTime);
			if (useStarts) {
				dTotal += d / 2;
			} else {
				dTotal = d;
			}
		}
		return dTotal;
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
