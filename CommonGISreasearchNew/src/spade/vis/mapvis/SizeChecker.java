package spade.vis.mapvis;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 30-Jan-2007
 * Time: 11:37:37
 * Checks the correctness of sizes, e.g. that the maximum size is greater than
 * the minimum size.
 */
public interface SizeChecker {
	/**
	 * Checks the correctness of sizes, e.g. that the maximum size is greater than
	 * the minimum size.
	 * The sizes are specified in the agrument array.
	 * Returns a message explaining what is wrong with the sizes. If everything is
	 * right, returns null.
	 */
	public String checkSizes(float sizes[]);
}
