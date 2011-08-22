package spade.analysis.plot;

import spade.vis.database.ObjectContainer;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Apr 21, 2008
 * Time: 4:18:44 PM
 * A tool (e.g. a query tool) implementing this interface can check if
 * a given object container is suitable for this tool
 */
public interface ObjectsSuitabilityChecker {
	public boolean isSuitable(ObjectContainer oCont);
}
