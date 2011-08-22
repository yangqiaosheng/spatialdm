package spade.lib.ui;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: 23-Oct-2007
 * Time: 10:11:09
 * Used for aligning a plot with a "ruler", e.g. representing a
 * time axis
 */
public interface RulerController {
	/**
	 * Informs the ruler about the required position of the starting point
	 * within the component
	 */
	public int getStartPos();

	/**
	 * Informs the ruler about the required position of the end point
	 * within the component
	 */
	public int getEndPos();
}
