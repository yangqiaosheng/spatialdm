package spade.analysis.system;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Mar 10, 2009
 * Time: 3:37:01 PM
 * An interface for logging actions.
 */
public interface ActionLogger {
	/**
	 * Adds the given action description (must be instance of core.ActionDescr) to the actions log
	 */
	public void logAction(Object actionDescr);
}
