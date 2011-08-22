package spade.analysis.tools.distances;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Jun 18, 2008
 * Time: 12:09:59 PM
 * This interface must be implemented by the classes for computing
 * distances between trajectories where time transformation is used
 */
public interface TimeTransformationUser {
	/**
	 * Returns the required type of time transformation
	 */
	public int getTimeTransformationType();
}
