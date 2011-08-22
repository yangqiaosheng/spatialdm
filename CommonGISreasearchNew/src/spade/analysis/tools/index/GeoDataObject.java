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
public class GeoDataObject extends EuclidianDataObject {

	/**
	 * @param originalInstance
	 * @param key
	 * @param database
	 */
	public GeoDataObject(Instance originalInstance, String key, Database database) {
		super(originalInstance, key, database);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Calculates the euclidian-distance between dataObject and this.dataObject with
	 * geo references
	 * @param dataObject The DataObject, that is used for distance-calculation with this.dataObject
	 * @return double-value The euclidian-distance between dataObject and this.dataObject
	 *                      NaN, if the computation could not be performed
	 */
	@Override
	public double distance(DataObject dataObject) {
		double dist = 0.0;

		if (!(dataObject instanceof EuclidianDataObject))
			return Double.NaN;

		if (getInstance().equalHeaders(dataObject.getInstance())) {
			double lo1 = getInstance().valueSparse(0);
			double la1 = getInstance().valueSparse(1);
			double lo2 = dataObject.getInstance().valueSparse(0);
			double la2 = dataObject.getInstance().valueSparse(1);

			return geoDist(lo1, la1, lo2, la2);
		}
		return Double.NaN;
	}

	public static final long R_EARTH = 6371000;

	/**
	 * Computes the metric distance between two points on the Earths specified
	 * by their geograogical coordinates (latitudes and longitudes).
	 * Note that longitude is X and latitude is Y!
	 * Returns the distance in meters.
	 */
	public static double geoDist(double lon1, double lat1, double lon2, double lat2) {
		double rlon1 = lon1 * Math.PI / 180, rlon2 = lon2 * Math.PI / 180, rlat1 = lat1 * Math.PI / 180, rlat2 = lat2 * Math.PI / 180;
		double dlon = (rlon1 - rlon2) / 2, dlat = (rlat1 - rlat2) / 2, lat12 = (rlat1 + rlat2) / 2;
		double sindlat = Math.sin(dlat), sindlon = Math.sin(dlon);
		double cosdlon = Math.cos(dlon), coslat12 = Math.cos(lat12), f = sindlat * sindlat * cosdlon * cosdlon + sindlon * sindlon * coslat12 * coslat12;
		// alternative formula:
		// double f=sindlat*sindlat+sindlon*sindlon*Math.cos(rlat1)*Math.cos(rlat2);
		f = Math.sqrt(f);
		f = Math.asin(f) * 2; //the angle between the points
		f *= R_EARTH;
		return f;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
