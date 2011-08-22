package core;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 21-Apr-2004
 * Time: 12:47:23
 * Contains static variables used for counting instances of various components.
 * Typically, instance numbers are used for producing unique identifiers of
 * components. After removing a project or loading another project, all such
 * counts need to be reset. Keeping all counts in a single place simplifies
 * their resetting.
 */
public class InstanceCounts {
	/**
	* Used to generate unique identifiers of instances of MapView.
	*/
	protected static int mapViewInstanceCount = 0;
	/**
	* Used to generate unique identifiers of instances of Plot.
	*/
	protected static int plotInstanceCount = 0;

	/**
	 * Increments the number of instances of MapView and returns the result.
	 */
	public static int incMapViewInstanceCount() {
		mapViewInstanceCount++;
		return mapViewInstanceCount;
	}

	/**
	 * Increments the number of instances of Plot and returns the result.
	 */
	public static int incPlotInstanceCount() {
		plotInstanceCount++;
		return plotInstanceCount;
	}

	/**
	* Resets all counts.
	*/
	public static void reset() {
		mapViewInstanceCount = 0;
		plotInstanceCount = 0;
	}
}
