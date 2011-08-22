package spade.analysis.tools.clustering;

import spade.lib.util.IntArray;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Jan 8, 2009
 * Time: 6:59:29 PM
 * Stores results of testing of the assignment of objects to a cluster.
 */
public class ClusterTestResult {
	public int clusterN = -1;
	/**
	 * Contains object indexes
	 */
	public IntArray correctlyClassified = null;
	/**
	 * Contains object indexes
	 */
	public IntArray falsePositives = null;
	/**
	 * Contains object indexes
	 */
	public IntArray falseNegatives = null;

	public int getNCorrectlyClassified() {
		if (correctlyClassified == null)
			return 0;
		return correctlyClassified.size();
	}

	public int getNFalsePositives() {
		if (falsePositives == null)
			return 0;
		return falsePositives.size();
	}

	public int getNFalseNegatives() {
		if (falseNegatives == null)
			return 0;
		return falseNegatives.size();
	}

	public void addCorrectlyClassified(int idx) {
		if (correctlyClassified == null) {
			correctlyClassified = new IntArray(100, 100);
		}
		correctlyClassified.addElement(idx);
	}

	public void addFalsePositive(int idx) {
		if (falsePositives == null) {
			falsePositives = new IntArray(100, 100);
		}
		falsePositives.addElement(idx);
	}

	public void addFalseNegative(int idx) {
		if (falseNegatives == null) {
			falseNegatives = new IntArray(100, 100);
		}
		falseNegatives.addElement(idx);
	}
}
