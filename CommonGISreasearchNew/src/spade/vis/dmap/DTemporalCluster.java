/**
 * 
 */
package spade.vis.dmap;

import java.util.ArrayList;
import java.util.Random;

import spade.time.TimeMoment;
import spade.vis.database.SpatialEntity;
import spade.vis.database.ThematicDataItem;
import spade.vis.geometry.Geometry;

/**
 * @author Admin
 * 
 */
public class DTemporalCluster extends DMovingObject {

	@Override
	public double[] getSpeeds() {
		Random rm = new Random(System.currentTimeMillis());
		double[] s = new double[55];
		for (int i = 0; i < 55; i++) {
			s[i] = rm.nextDouble();
		}

		return s;
	}

	java.util.List<Integer> counts = new ArrayList<Integer>();
	double[] cs = null;
	java.util.List<Double> geodists = new ArrayList<Double>();
	double[] geodis = null;
	java.util.List<Double> tempdists = new ArrayList<Double>();
	double[] tmpdis = null;
	java.util.List<Double> geodense = new ArrayList<Double>();
	double[] geoden = null;
	java.util.List<Double> tempdense = new ArrayList<Double>();
	double[] tmpden = null;
	java.util.List<Double> Lc_ids = new ArrayList<Double>();
	double[] Ac_ids = null;

	public double[] getCounts() {
		if (cs == null) {

			setup();
		}
		return cs;
	}

	public double[] getGeodist() {
		if (geodis == null) {
			setup();
		}
		return geodis;
	}

	public double[] getTempdist() {
		if (tmpdis == null) {
			setup();
		}
		return tmpdis;
	}

	public double[] getGeodense() {
		if (geoden == null) {
			setup();
		}
		return geoden;
	}

	public double[] getTempdense() {
		if (tmpden == null) {
			setup();
		}
		return tmpden;
	}

	public double[] getCIDS() {
		if (Ac_ids == null) {
			setup();
		}
		return Ac_ids;
	}

	public void setup() {
		for (int i = 0; i < track.size(); i++) {
			ThematicDataItem themData = ((SpatialEntity) track.get(i)).getThematicData();
			counts.add(Integer.parseInt(themData.getAttrValueAsString(2)));
			geodists.add(Double.parseDouble(themData.getAttrValueAsString(7)));
			geodense.add(Double.parseDouble(themData.getAttrValueAsString(8)));
			tempdists.add(Double.parseDouble(themData.getAttrValueAsString(11)));
			tempdense.add(Double.parseDouble(themData.getAttrValueAsString(12)));
			Lc_ids.add(Double.parseDouble(themData.getAttrValueAsString(0)));
		}
		tmpden = new double[tempdense.size()];
		for (int i = 0; i < tempdense.size(); i++) {
			tmpden[i] = tempdense.get(i);
		}
		geoden = new double[geodense.size()];
		for (int i = 0; i < geodense.size(); i++) {
			geoden[i] = geodense.get(i);
		}
		tmpdis = new double[tempdists.size()];
		for (int i = 0; i < tempdists.size(); i++) {
			tmpdis[i] = tempdists.get(i);
		}
		geodis = new double[geodists.size()];
		for (int i = 0; i < geodists.size(); i++) {
			geodis[i] = geodists.get(i);
		}
		cs = new double[counts.size()];
		for (int i = 0; i < counts.size(); i++) {
			cs[i] = counts.get(i);
		}
		Ac_ids = new double[Lc_ids.size()];
		for (int i = 0; i < Lc_ids.size(); i++) {
			Ac_ids[i] = Lc_ids.get(i);
		}
	}

	/**
	 * Adds a new position to the track: a geometry and the time interval of its
	 * validity.
	 */
	@Override
	public void addPosition(Geometry position, TimeMoment fromT, TimeMoment toT, ThematicDataItem themData) {
		super.addPosition(position, fromT, toT, themData);

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
