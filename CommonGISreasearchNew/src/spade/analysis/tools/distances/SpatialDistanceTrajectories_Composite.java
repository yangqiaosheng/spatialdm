package spade.analysis.tools.distances;

import java.awt.Checkbox;
import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.util.HashMap;
import java.util.Vector;

import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Dialogs;
import spade.lib.basicwin.OKDialog;
import spade.vis.database.SpatialEntity;
import spade.vis.dmap.TrajectoryObject;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Jun 19, 2008
 * Time: 1:51:40 PM
 * Measures the distance between 2 trajectories on the basis of their
 * starts, ends, path lengths, and durations. The user may select which
 * of these properties to take into account.
 */
public class SpatialDistanceTrajectories_Composite extends SpatialDistanceTrajectories {
	/**
	 * Indicate which of the properties of the trajectories are taken into account
	 */
	protected boolean useStarts = true, useEnds = true, useLengths = true, useDurations = true;
	/**
	 * The threshold for the duration difference, if used
	 */
	protected long maxDurDiff = 0;

	/**
	 * If necessary, asks the user about additional parameters required for
	 * this Distance Computer. Returns true if successful.
	 */
	@Override
	public boolean askParameters() {
		Panel p = new Panel(new ColumnLayout());
		p.add(new Label("Method of computing distances: "));
		p.add(new Label(methodName, Label.CENTER));
		p.add(new Label("What properties to take into account?"));
		Checkbox cbStarts = new Checkbox("Start points", true);
		p.add(cbStarts);
		Checkbox cbEnds = new Checkbox("End points", true);
		p.add(cbEnds);
		Checkbox cbLengths = new Checkbox("Path lengths", true);
		p.add(cbLengths);
		Checkbox cbDurations = new Checkbox("Durations", true);
		p.add(cbDurations);
		p.add(new Label("Threshold for the difference between the durations:"));
		Panel pp = new Panel(new FlowLayout(FlowLayout.RIGHT));
		TextField tfDurDiff = new TextField("600", 5);
		pp.add(tfDurDiff);
		pp.add(new Label("smallest time units"));
		p.add(pp);
		OKDialog dia = new OKDialog(CManager.getAnyFrame(), "Method parameters", true);
		dia.addContent(p);
		dia.show();
		if (dia.wasCancelled())
			return false;
		useStarts = cbStarts.getState();
		useEnds = cbEnds.getState();
		useLengths = cbLengths.getState();
		useDurations = cbDurations.getState();
		if (useDurations) {
			long d = 0;
			String str = tfDurDiff.getText();
			if (str != null) {
				try {
					d = Long.parseLong(str);
				} catch (NumberFormatException e) {
				}
			}
			if (d < 0) {
				d = Dialogs.askForIntValue(CManager.getAnyFrame(), "Threshold for the difference between the durations?", 10, 0, Integer.MAX_VALUE, "Method of computing distances: " + methodName, "Method parameter", false);
			}
			maxDurDiff = d;
		}

		return useStarts || useEnds || useLengths || useDurations;
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
		if (useLengths)
			if (str == null) {
				str = "path lengths";
			} else {
				str += "; path lengths";
			}
		if (useDurations) {
			if (str == null) {
				str = "durations";
			} else {
				str += "; durations";
			}
			str += " (threshold = " + maxDurDiff + ")";
		}
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
		params.put("useLengths", new Boolean(useLengths).toString());
		params.put("useDurations", new Boolean(useDurations).toString());
		if (useDurations) {
			params.put("maxDurDiff", String.valueOf(maxDurDiff));
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
		String val = (String) params.get("useStarts");
		if (val != null) {
			useStarts = Boolean.parseBoolean(val);
		}
		val = (String) params.get("useEnds");
		if (val != null) {
			useEnds = Boolean.parseBoolean(val);
		}
		val = (String) params.get("useLengths");
		if (val != null) {
			useLengths = Boolean.parseBoolean(val);
		}
		val = (String) params.get("useDurations");
		if (val != null) {
			useDurations = Boolean.parseBoolean(val);
		}
		if (useDurations) {
			val = (String) params.get("maxDurDiff");
			if (val != null) {
				try {
					maxDurDiff = Long.parseLong(val);
				} catch (NumberFormatException e) {
				}
			}
		}
	}

	/**
	 * Determines the distance between two trajectories
	 */
	@Override
	public double distance(TrajectoryObject obj1, TrajectoryObject obj2, boolean useThreshold) {
		useThreshold = useThreshold && !Double.isNaN(distanceThreshold);
		if (obj1 == null || obj2 == null)
			return (useThreshold) ? Double.POSITIVE_INFINITY : Double.NaN;
		double dif = 0;
		double nddif = 0;
		if (useLengths) {
			dif = Math.abs(obj1.mobj.getTrackLength() - obj2.mobj.getTrackLength());
			if (useThreshold && (dif > distanceThreshold))
				return Double.POSITIVE_INFINITY;
		}
		if (useDurations) {
			long ddif = Math.abs(obj1.mobj.getDuration() - obj2.mobj.getDuration());
			if (useThreshold && (ddif > maxDurDiff))
				return Double.POSITIVE_INFINITY;
			nddif = distanceThreshold * ddif / maxDurDiff;
		}
		if (useStarts || useEnds) {
			Vector tr1 = obj1.mobj.getTrack(), tr2 = obj2.mobj.getTrack();
			double dPoints = 0;
			if (useStarts) {
				SpatialEntity sp1 = (SpatialEntity) tr1.elementAt(0);
				SpatialEntity sp2 = (SpatialEntity) tr2.elementAt(0);
				dPoints = distance(sp1.getGeometry(), sp2.getGeometry(), true);
				if (useThreshold && (dPoints > distanceThreshold))
					return Double.POSITIVE_INFINITY;
			}
			if (useEnds) {
				SpatialEntity sp1 = (SpatialEntity) tr1.elementAt(tr1.size() - 1);
				SpatialEntity sp2 = (SpatialEntity) tr2.elementAt(tr2.size() - 1);
				double d1 = distance(sp1.getGeometry(), sp2.getGeometry(), true);
				if (useThreshold && (d1 > distanceThreshold))
					return Double.POSITIVE_INFINITY;
				if (useStarts) {
					dPoints = (dPoints + d1) / 2;
				} else {
					dPoints = d1;
				}
			}
			dif += dPoints;
		}
		if (useDurations && nddif > 0) {
			dif = Math.sqrt(dif * dif + nddif * nddif);
		}
		return dif;
	}
}
