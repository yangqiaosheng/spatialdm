/**
 * 
 */
package edu.wlu.cs.levy.CG;

import java.text.SimpleDateFormat;

/**
 * @author Admin
 *
 */
public class TempDistance extends DistanceMetric {

	/* (non-Javadoc)
	 * @see edu.wlu.cs.levy.CG.DistanceMetric#distance(double[], double[])
	 */
	@Override
	protected double distance(double[] a, double[] b) {
		// TODO Auto-generated method stub
		try {
			
			double t_diff = Math.abs(a[2] - b[2]);
			return t_diff / 1000;
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
