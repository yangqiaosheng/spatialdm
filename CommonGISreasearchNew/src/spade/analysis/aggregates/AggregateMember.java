package spade.analysis.aggregates;

import java.util.Vector;

import spade.time.TimeMoment;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Apr 21, 2008
 * Time: 12:41:27 PM
 * Describes a a member of some aggregate.
 */
public class AggregateMember {
	/**
	 * The unique identifier of the aggregate member
	 */
	public String id = null;
	/**
	 * The time of entering the aggregate
	 */
	public TimeMoment enterTime = null;
	/**
	 * The time of exiting the aggregate
	 */
	public TimeMoment exitTime = null;

	/**
	 * In the given vector of aggregate members (instances of AggregateMember)
	 * finds a member with the given identifier and returns its index or
	 * -1 if not found.
	 */
	public static int findMemberById(Vector members, String id) {
		if (id == null || members == null || members.size() < 1)
			return -1;
		for (int i = 0; i < members.size(); i++)
			if (members.elementAt(i) instanceof AggregateMember) {
				AggregateMember mb = (AggregateMember) members.elementAt(i);
				if (mb.id.equalsIgnoreCase(id))
					return i;
			}
		return -1;
	}
}
