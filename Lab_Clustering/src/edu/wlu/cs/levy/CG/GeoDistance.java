package edu.wlu.cs.levy.CG;



/**
 * 
 */

/**
 * @author Admin
 * 
 */
public class GeoDistance extends DistanceMetric {

	protected double distance(double[] a, double[] b) {

		return geoDist(a[0], a[1], b[0], b[1]);
	}

	public static final long R_EARTH = 6371000;

	/**
	 * Computes the metric distance between two points on the Earths specified
	 * by their geograogical coordinates (latitudes and longitudes). Note that
	 * longitude is X and latitude is Y! Returns the distance in meters.
	 */
	public static double geoDist(double lon1, double lat1, double lon2, double lat2) {
		double rlon1 = lon1 * Math.PI / 180, rlon2 = lon2 * Math.PI / 180, rlat1 = lat1 * Math.PI / 180, rlat2 = lat2 * Math.PI / 180;
		double dlon = (rlon1 - rlon2) / 2, dlat = (rlat1 - rlat2) / 2, lat12 = (rlat1 + rlat2) / 2;
		double sindlat = Math.sin(dlat), sindlon = Math.sin(dlon);
		double cosdlon = Math.cos(dlon), coslat12 = Math.cos(lat12), f = sindlat * sindlat * cosdlon * cosdlon + sindlon * sindlon * coslat12
				* coslat12;
		// alternative formula:
		// double
		// f=sindlat*sindlat+sindlon*sindlon*Math.cos(rlat1)*Math.cos(rlat2);
		f = Math.sqrt(f);
		f = Math.asin(f) * 2; // the angle between the points
		f *= R_EARTH;
		return (double) f;
	}

}
