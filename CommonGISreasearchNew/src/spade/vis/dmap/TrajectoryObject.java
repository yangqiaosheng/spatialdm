package spade.vis.dmap;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 23-Apr-2007
 * Time: 18:33:17
 * Contains additional information for geographical objects representing trajectories
 */
public class TrajectoryObject {
	/**
	 * Possible uses and transformations of the times of the trajectory points
	 */
	public static final int TIME_NOT_TRANSFORMED = 0, TIME_RELATIVE_STARTS = 1, TIME_RELATIVE_ENDS = 2;
	/**
	 * The object from a map layer representing a trajectory
	 */
	public DMovingObject mobj = null;
	/**
	 * Transformed time references of the points of the trajectory
	 */
	public long times[] = null;

	/**
	 * Get indices of N equidistant (according to distances between points) intermediate points
	 * Sometimes returns zeros at the end because starts and ends are not included...
	 */
	public int[] getNRelDistIdx(int n) {
		int idx[] = new int[n];
		double dist[] = mobj.getDistances().clone();
		for (int i = 1; i < dist.length; i++) {
			dist[i] += dist[i - 1];
		}
		double start = dist[0], end = dist[dist.length - 1];
		double dval[] = new double[n];
		for (int i = 0; i < n; i++) {
			dval[i] = start + Math.round((end - start) * (1f + i) / (1f + n));
		}
		int idxn = 0;
		for (int i = 1; i < dist.length - 1 && idxn < n; i++)
			if (dist[i - 1] <= dval[idxn] && dist[i] > dval[idxn]) {
				do {
					idx[idxn] = i;
					idxn++;
				} while (idxn < n && dist[i - 1] <= dval[idxn] && dist[i] > dval[idxn]);
			}
		//System.out.println("* id="+mobj.getIdentifier()+", n="+idxn);
		return idx;
	}

	/**
	 * Get indices of all intermediate points with required step
	 * (counts back from the end)
	 * Sometimes returns zeros at the end because starts and ends are not included...
	 */
	public int[] getAbsDistIdx(double step, boolean adjustStart) {
		double dist[] = mobj.getDistances().clone();
		for (int i = 1; i < dist.length; i++) {
			dist[i] += dist[i - 1];
		}
		double start = dist[0], end = dist[dist.length - 1];
		int n = (int) Math.round((end - start) / step) - 1;
		if (n < 0) {
			n = 0;
		}
		//System.out.println("* !!! n="+n);
		int idx[] = new int[n];
		double dval[] = new double[n];
		if (adjustStart) {
			for (int i = 0; i < n; i++) {
				dval[i] = start + (i + 1) * step;
			}
			int idxn = 0;
			for (int i = 1; i < dist.length - 1 && idxn < n; i++)
				if (dist[i - 1] <= dval[idxn] && dist[i] > dval[idxn]) {
					do {
						idx[idxn] = i;
						idxn++;
					} while (idxn < n && dist[i - 1] <= dval[idxn] && dist[i] > dval[idxn]);
				}
		} else {
			for (int i = 0; i < n; i++) {
				dval[i] = end - (n - i) * step;
			}
			int idxn = 0;
			for (int i = dist.length - 1; i > 0 && idxn < n; i--)
				if (dist[i - 1] <= dval[n - 1 - idxn] && dist[i] > dval[n - 1 - idxn]) {
					do {
						idx[idxn] = i;
						idxn++;
					} while (idxn < n && dist[i - 1] <= dval[n - 1 - idxn] && dist[i] > dval[n - 1 - idxn]);
				}
		}
		return idx;
	}

	/**
	 * Get indices of N equidistant (in time) intermediate points
	 * Sometimes returns zeros at the end because starts and ends are not included...
	 */
	public int[] getNRelTimesIdx(int n) {
		int idx[] = new int[n];
		long start = times[0], end = times[times.length - 1];
		long tval[] = new long[n];
		for (int i = 0; i < n; i++) {
			tval[i] = start + Math.round((end - start) * (1f + i) / (1f + n));
		}
		int idxn = 0;
		for (int i = 1; i < times.length - 1 && idxn < n; i++)
			if (times[i - 1] <= tval[idxn] && times[i] > tval[idxn]) {
				do {
					idx[idxn] = i;
					idxn++;
				} while (idxn < n && times[i - 1] <= tval[idxn] && times[i] > tval[idxn]);
			}
		//System.out.println("* id="+mobj.getIdentifier()+", n="+idxn);
		return idx;
	}

	/**
	 * Get indices of all intermediate points with required step
	 * (counts back from the end)
	 * Sometimes returns zeros at the end because starts and ends are not included...
	 */
	public int[] getAbsTimeIdx(int step, boolean adjustStart) {
		long start = times[0], end = times[times.length - 1];
		int n = (int) (end - start) / step;
		if (n < 0) {
			n = 0;
		}
		//System.out.println("* !!! n="+n);
		int idx[] = new int[n];
		long tval[] = new long[n];
		if (adjustStart) {
			for (int i = 0; i < n; i++) {
				tval[i] = start + (i + 1) * step;
			}
			int idxn = 0;
			for (int i = 1; i < times.length - 1 && idxn < n; i++)
				if (times[i - 1] <= tval[idxn] && times[i] > tval[idxn]) {
					do {
						idx[idxn] = i;
						idxn++;
					} while (idxn < n && times[i - 1] <= tval[idxn] && times[i] > tval[idxn]);
				}
		} else {
			for (int i = 0; i < n; i++) {
				tval[i] = end - (n - i) * step;
			}
			int idxn = 0;
			for (int i = times.length - 1; i > 0 && idxn < n; i--)
				if (times[i - 1] <= tval[n - 1 - idxn] && times[i] > tval[n - 1 - idxn]) {
					do {
						idx[idxn] = i;
						idxn++;
					} while (idxn < n && times[i - 1] <= tval[n - 1 - idxn] && times[i] > tval[n - 1 - idxn]);
				}
		}
		return idx;
	}

}
