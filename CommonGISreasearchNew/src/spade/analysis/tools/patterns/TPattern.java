package spade.analysis.tools.patterns;

import spade.lib.util.FloatArray;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Jan 29, 2009
 * Time: 7:45:26 PM
 * Represents a t-pattern, which includes a sequence of regions and
 * travel durations (minimum, maximum) between them.
 */
public class TPattern extends ObjectGroup {
	/**
	 * The support and length of the pattern
	 */
	public int support = 0, length = 0;
	/**
	 * The minimum travel times between the regions
	 */
	public FloatArray minTravTimes = null;
	/**
	 * The maximum travel times between the regions
	 */
	public FloatArray maxTravTimes = null;
}
