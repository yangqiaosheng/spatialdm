package spade.analysis.aggregates;

import java.util.Vector;

import spade.vis.database.ObjectContainer;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Apr 21, 2008
 * Time: 12:50:01 PM
 * An ObjectContainer containing aggregates.
 */
public interface AggregateContainer extends ObjectContainer {
	/**
	 * Returns the aggregates - instances of Aggregate
	 */
	public Vector getAggregates();

	/**
	 * Returns a reference to a container (e.g. map layer) of the aggregate members
	 */
	public ObjectContainer getMemberContainer();
}
