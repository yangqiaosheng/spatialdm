package spade.analysis.tools.distances;

import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.util.HashMap;

import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Dialogs;
import spade.lib.basicwin.OKDialog;
import spade.time.TimeReference;
import spade.vis.dmap.DGeoObject;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RealPoint;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: Oct 6, 2008
 * Time: 4:31:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class SpatioTemporalDistancePoints extends SpatialDistance {

	/**
	 * Indicate which of the properties of the trajectories are taken into account
	 */
	protected boolean useDurations = true;
	/**
	 * The threshold for the duration difference, if used
	 */
	protected long maxDurDiff = 0;

	/**
	 * Returns the name of this Distance Computer
	 */
	@Override
	public String getMethodName() {
		methodName = "Spatio-temporal distance";
		return methodName;
	}

	/**
	 * If necessary, asks the user about additional parameters required for
	 * this Distance Computer. Returns true if successful.
	 * Used here for finding time range;
	 * later will be extended for selecting time components to be used
	 */
	@Override
	public boolean askParameters() {
		Panel p = new Panel(new ColumnLayout());
		//Checkbox cbDurations=new Checkbox("Durations",true);
		//p.add(cbDurations);
		p.add(new Label("Threshold for the difference between the times:"));
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
		useDurations = true; //cbDurations.getState();
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
				d = Dialogs.askForIntValue(CManager.getAnyFrame(), "Threshold for the difference between the times?", 10, 0, Integer.MAX_VALUE, "Method of computing distances: " + methodName, "Method parameter", false);
			}
			maxDurDiff = d;
		}

		return true;
	}

	/**
	 * Returns a string describing the current parameter settings of the method.
	 * May return null if no additional parameters are used.
	 */
	@Override
	public String getParameterDescription() {
		if (useDurations)
			return "Accounts for durations; (threshold = " + maxDurDiff + ")";
		return "Ignores durations";
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
		params.put("useDurations", new Boolean(useDurations).toString());
		params.put("maxDurDiff", String.valueOf(maxDurDiff));
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
		String val = (String) params.get("useDurations");
		if (val != null) {
			useDurations = Boolean.parseBoolean(val);
		}
		val = (String) params.get("maxTDiff");
		if (val != null) {
			try {
				maxDurDiff = Long.parseLong(val);
			} catch (NumberFormatException e) {
			}
		}
	}

	/**
	 * Determines the distance between two objects taking into account the
	 * distance threshold. If in the process of computation it turns out that the
	 * distance is higher than the threshold, the method may stop further
	 * computing and return an arbitrary value greater than the threshold.
	 */
	@Override
	public double findDistance(Object obj1, Object obj2, boolean useThreshold) {
		//if (maxDurDiff==0)
		//if (!getParameters()) return Double.NaN;
		useThreshold = useThreshold && !Double.isNaN(distanceThreshold);
		if (obj1 == null || obj2 == null)
			return (useThreshold) ? Double.POSITIVE_INFINITY : Double.NaN;
		if (!(obj1 instanceof DGeoObject) || !(obj2 instanceof DGeoObject))
			return (useThreshold) ? Double.POSITIVE_INFINITY : Double.NaN;
		DGeoObject gobj1 = (DGeoObject) obj1, gobj2 = (DGeoObject) obj2;
		Geometry g1 = gobj1.getGeometry(), g2 = gobj2.getGeometry();
		if (g1 == null || g2 == null)
			return (useThreshold) ? Double.POSITIVE_INFINITY : Double.NaN;
		double distGeo;
		if ((g1 instanceof RealPoint) && (g2 instanceof RealPoint)) {
			distGeo = distance((RealPoint) g1, (RealPoint) g2, useThreshold);
		} else {
			distGeo = distance(g1, g2, useThreshold);
		}
		if (useThreshold && distGeo > distanceThreshold)
			return Double.NaN;
		if (maxDurDiff <= 0)
			return distGeo;
		if (gobj1.getTimeReference() == null || gobj2.getTimeReference() == null)
			return (useThreshold) ? Double.POSITIVE_INFINITY : Double.NaN;
		double distTime = distanceThreshold * TimeReference.getTemporalDistance(gobj1.getTimeReference(), gobj2.getTimeReference()) / maxDurDiff;
		if (distTime > distanceThreshold)
			return (useThreshold) ? Double.POSITIVE_INFINITY : Double.NaN;
		else
			return Math.sqrt(distGeo * distGeo + distTime * distTime);
	}
}
