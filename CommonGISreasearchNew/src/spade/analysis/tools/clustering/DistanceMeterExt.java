package spade.analysis.tools.clustering;

import it.unipi.di.sax.optics.DistanceMeter;

import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Jan 27, 2009
 * Time: 11:41:19 AM
 * Extends the interface from the OPTICS library to avoid changes in the library.
 */
public interface DistanceMeterExt<T> extends DistanceMeter<T> {
	/**
	 * Returns its parameters, which are necessary and sufficient
	 * for setting up the DistanceMeter. Each parameter has a name,
	 * which is used as a key in the HashMap.
	 * The values of the parameters must be stored in the hashmap as strings!
	 * This is necessary for saving the parameters in a text file.
	 * If the argument is not null, adds its parameter-value pairs to the
	 * given hashmap.
	 */
	public HashMap getParameters(HashMap params);

	/**
	 * Does its internal settings according to the given list of parameters
	 */
	public void setup(HashMap params);

	/**
	 * Returns true if it has all necessary internal settings
	 */
	public boolean hasValidSettings();

	/**
	 * Informs if it deals with geographical coordinates (latitudes and longitudes)
	 */
	public boolean isGeographic();
}
