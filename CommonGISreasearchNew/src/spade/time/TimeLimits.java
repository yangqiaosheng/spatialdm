package spade.time;

import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Jul 5, 2010
 * Time: 5:35:09 PM
 * Consists of one or more time intervals, represented by TimeReference instances.
 * Checks whether a given time reference fits in one of these intervals.
 */
public class TimeLimits {
	/**
	 * The time intervals setting the limits
	 */
	public Vector<TimeReference> limits = null;

	public void init(int capacity, int increment) {
		limits = new Vector(capacity);
	}

	public void addLimit(TimeReference tref) {
		if (tref == null)
			return;
		if (limits == null) {
			init(1, 10);
		}
		limits.addElement(tref);
	}

	public void addLimit(TimeMoment t1, TimeMoment t2) {
		if (t1 == null)
			return;
		if (t2 == null) {
			t2 = t1;
		}
		addLimit(new TimeReference(t1, t2));
	}

	public void addLimits(TimeLimits tLim) {
		if (tLim == null || tLim.limits == null || tLim.limits.size() < 1)
			return;
		if (limits == null) {
			init(tLim.limits.size(), 10);
		}
		for (int i = 0; i < tLim.limits.size(); i++) {
			addLimit(tLim.limits.elementAt(i));
		}
	}

	/**
	 * Checks whether the given time reference fits in any of the time limits
	 */
	public boolean doesFit(TimeReference validTime) {
		if (validTime == null)
			return true; //does not apply
		if (limits == null || limits.size() < 1)
			return true; //no limits
		for (int i = 0; i < limits.size(); i++) {
			TimeReference tr = limits.elementAt(i);
			if (validTime.isValid(tr.validFrom, tr.validUntil))
				return true;
		}
		return false;
	}

	/**
	 * Checks whether the given time interval fits in any of the time limits
	 */
	public boolean doesFit(TimeMoment start, TimeMoment end) {
		if (limits == null || limits.size() < 1)
			return true; //no limits
		if (start == null)
			return true; //does not apply
		if (end == null) {
			end = start;
		}
		return doesFit(new TimeReference(start, end));
	}

	/**
	 * Checks whether the given time moment fits in any of the time limits
	 */
	public boolean doesFit(TimeMoment t) {
		if (t == null)
			return true; //does not apply
		if (limits == null || limits.size() < 1)
			return true; //no limits
		for (int i = 0; i < limits.size(); i++) {
			TimeReference tr = limits.elementAt(i);
			if (t.compareTo(tr.getValidFrom()) >= 0 && t.compareTo(tr.getValidUntil()) <= 0)
				return true;
		}
		return false;
	}
}
