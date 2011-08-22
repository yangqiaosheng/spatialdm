package spade.analysis.tools.index;

import weka.clusterers.forOPTICSAndDBScan.DataObjects.DataObject;
import weka.clusterers.forOPTICSAndDBScan.DataObjects.EuclidianDataObject;
import weka.clusterers.forOPTICSAndDBScan.Databases.Database;
import weka.core.Instance;

/**
 * 
 */

/**
 * @author Admin
 * 
 */
public class TemporalDataObject extends EuclidianDataObject {

	StringBuffer label = null;

	public void setLabel(StringBuffer st) {
		label = st;
	}

	public StringBuffer getLabel() {
		return label;
	}

	/**
	 * @param originalInstance
	 * @param key
	 * @param database
	 */
	public TemporalDataObject(Instance originalInstance, String key, Database database) {
		super(originalInstance, key, database);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Calculates the euclidian-distance between dataObject and this.dataObject
	 * with geo references
	 * 
	 * @param dataObject
	 *            The DataObject, that is used for distance-calculation with
	 *            this.dataObject
	 * @return double-value The euclidian-distance between dataObject and
	 *         this.dataObject NaN, if the computation could not be performed
	 */
	@Override
	public double distance(DataObject dataObject) {
		double dist = 0.0;

		if (!(dataObject instanceof EuclidianDataObject))
			return Double.NaN;
		try {
			long tmp = (long) getInstance().value(2);
			long tmp2 = (long) dataObject.getInstance().value(2);
			long t_diff = tmp2 - tmp;

			t_diff = Math.abs(t_diff);
			return t_diff;
		} catch (Exception e) {
			// TODO: handle exception
			return Double.NaN;
		}

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
