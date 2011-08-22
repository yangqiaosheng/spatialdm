package spade.analysis.tools.clustering;

import java.awt.Image;

import spade.lib.util.Comparable;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Mar 10, 2009
 * Time: 5:12:05 PM
 */
public class ClusterImage implements Comparable {
	public String clusterLabel = null;
	/**
	 * Clusters are enumerated starting from 0
	 */
	public int clusterIdx = -1;
	/**
	 * Number of objects in the cluster
	 */
	public int size = 0;
	/**
	 * A pictorial representation of the cluster
	 */
	public Image image = null;

	/**
	*  Returns 0 if equal, <0 if THIS is less than the argument, >0 otherwise
	*/
	@Override
	public int compareTo(Comparable c) {
		if (c == null || !(c instanceof ClusterImage))
			return -1;
		ClusterImage cim = (ClusterImage) c;
		if (size > cim.size)
			return -1;
		if (size < cim.size)
			return 1;
		return 0;
	}
}
