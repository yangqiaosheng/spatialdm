package spade.analysis.aggregates;

import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Apr 21, 2008
 * Time: 12:43:23 PM
 * An aggregate contains several members
 */
public interface Aggregate {
	/**
	 * Returns the unique identifier of this aggregate
	 */
	public String getIdentifier();

	/**
	 * Returns the members of this aggregate represented by instances of the class AggregateMember
	 */
	public Vector getAggregateMembers();

	/**
	 * Returns the total number of members
	 */
	public int getMemberCount();

	/**
	 * Returns the identifier of the member with the given index
	 */
	public String getMemberId(int idx);
}
