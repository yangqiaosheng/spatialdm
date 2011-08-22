package spade.time;

public class TimeReference {
	/**
	* The time moment when an object appears or becomes valid.
	*/
	protected TimeMoment validFrom = null;
	/**
	* The time moment when an object disappears or becomes invalid.
	*/
	protected TimeMoment validUntil = null;
	/**
	 * The original versions of validFrom and validUntil in the case
	 * when validFrom and validUntil are transformed times.
	 */
	protected TimeMoment origFrom = null, origUntil = null;

	public TimeReference() {
	}

	public TimeReference(TimeMoment t1, TimeMoment t2) {
		setValidFrom(t1);
		setValidUntil(t2);
	}

	/**
	* Sets the time moment when an object appears or becomes valid.
	*/
	public void setValidFrom(TimeMoment t) {
		validFrom = t;
	}

	/**
	* Returns the time moment when an object appears or becomes valid.
	*/
	public TimeMoment getValidFrom() {
		return validFrom;
	}

	/**
	* Sets the time moment when an object disappears or becomes invalid.
	* In particular, the time moment validUntil may be equal to the time moment
	* validFrom.
	*/
	public void setValidUntil(TimeMoment t) {
		validUntil = t;
	}

	/**
	* Returns the time moment when an object disappears or becomes invalid.
	* In particular, the time moment validUntil may be equal to the time moment
	* validFrom.
	*/
	public TimeMoment getValidUntil() {
		return validUntil;
	}

	/**
	* Checks if the object is valid at the specified time moment
	*/
	public boolean isValid(TimeMoment t) {
		if (t == null)
			return true;
		if (validFrom == null && validUntil == null)
			return true; //unlimited validity
		if (validFrom == null)
			return validUntil.compareTo(t) >= 0;
		return validFrom.compareTo(t) <= 0 && (validUntil == null || validUntil.compareTo(t) >= 0);
	}

	/**
	* Checks if the object is (at least partly) valid on the specified time interval
	*/
	public boolean isValid(TimeMoment t1, TimeMoment t2) {
		return isValid(t1, t2, true, true);
	}

	/**
	* Checks if the object is (at least partly) valid on the specified time interval
	*/
	public boolean isValid(TimeMoment t1, TimeMoment t2, boolean includeIntervalStart, boolean includeIntervalEnd) {
		if (t1 == null && t2 == null)
			return true;
		if (validFrom == null && validUntil == null)
			return true; //unlimited validity
		if (t1 == null) {
			if (validFrom == null)
				return true;
			int c = validFrom.compareTo(t2);
			return c < 0 || (c == 0 && includeIntervalEnd);
		}
		if (t2 == null) {
			if (validUntil == null)
				return true;
			int c = validUntil.compareTo(t1);
			return c > 0 || (c == 0 && includeIntervalStart);
		}
		if (validFrom == null) {
			int c = validUntil.compareTo(t1);
			return c > 0 || (c == 0 && includeIntervalStart);
		}
		int c = validFrom.compareTo(t2);
		if (c > 0 || (c == 0 && !includeIntervalEnd))
			return false;
		if (validUntil == null)
			return true;
		c = validUntil.compareTo(t1);
		return c > 0 || (c == 0 && includeIntervalStart);
	}

	@Override
	public String toString() {
		if (validFrom == null)
			return null;
		String str = validFrom.toString();
		if (validUntil != null) {
			str += " .. " + validUntil;
		}
		if (origFrom != null && origUntil != null) {
			str += " (" + origFrom + ".." + origUntil + ")";
		}
		return str;
	}

	/**
	 * Replaces validFrom and validUntil by the transformed time references.
	 * Keeps the original validFrom and validUntil in origFrom and origUntil.
	 */
	public void setTransformedTimes(TimeMoment from, TimeMoment until) {
		if (from == null || until == null)
			return;
		if (origFrom == null) {
			origFrom = validFrom;
		}
		if (origUntil == null) {
			origUntil = validUntil;
		}
		validFrom = from;
		validUntil = until;
	}

	/**
	 * Returns the original validFrom (the state before any transformations)
	 */
	public TimeMoment getOrigFrom() {
		if (origFrom != null)
			return origFrom;
		return validFrom;
	}

	/**
	 * Returns the original validUntil (the state before any transformations)
	 */
	public TimeMoment getOrigUntil() {
		if (origUntil != null)
			return origUntil;
		return validUntil;
	}

	/**
	 * Restores the original (not transformed) state of the time reference
	 */
	public void restoreOriginalTimes() {
		if (origFrom != null) {
			validFrom = origFrom;
		}
		if (origUntil != null) {
			validUntil = origUntil;
		}
		origFrom = null;
		origUntil = null;
	}

	@Override
	public Object clone() {
		TimeReference tr = new TimeReference();
		if (validFrom != null)
			if (origFrom != null) {
				tr.setValidFrom(origFrom.getCopy());
			} else {
				tr.setValidFrom(validFrom.getCopy());
			}
		if (validUntil != null)
			if (origUntil != null) {
				tr.setValidUntil(origUntil.getCopy());
			} else {
				tr.setValidUntil(validUntil.getCopy());
			}
		if (origFrom != null && origUntil != null) {
			tr.setTransformedTimes(validFrom.getCopy(), validUntil.getCopy());
		}
		return tr;
	}

	/**
	 * Subtracts the second reference from the first one. Hence, the distance will be negative
	 * if tr1 is earlier than tr2.
	 */
	public static long getTemporalDistance(TimeReference tr1, TimeReference tr2) {
		if (tr1 == null || tr2 == null)
			return 0l;
		TimeMoment t1 = tr1.getValidFrom(), t2 = tr1.getValidUntil(), t3 = tr2.getValidFrom(), t4 = tr2.getValidUntil();
		if (t1 == null || t3 == null)
			return 0l;
		if (t1.equals(t2)) {
			t2 = null;
		}
		if (t3.equals(t4)) {
			t4 = null;
		}
		long d13 = t1.subtract(t3);
		if (d13 == 0 || (t2 == null && t4 == null))
			return d13;
		if (t2 != null) {
			long d23 = t2.subtract(t3);
			if (d23 <= 0)
				return d23;
			if (d13 < 0 && d23 > 0)
				return 0;
			if (t4 == null)
				return d13;
			long d14 = t1.subtract(t4);
			if (d14 >= 0)
				return d14;
			return 0;
		}
		if (d13 < 0)
			return d13;
		long d14 = t1.subtract(t4);
		if (d14 <= 0)
			return 0;
		return d14;
	}

	/**
	 * Subtracts the second reference from the first one. Hence, the distance will be negative
	 * if tr1 is earlier than tr2. The computation is done in the given time units.
	 */
	public static long getTemporalDistance(TimeReference tr1, TimeReference tr2, char unit) {
		if (tr1 == null || tr2 == null)
			return 0l;
		TimeMoment t1 = tr1.getValidFrom(), t2 = tr1.getValidUntil(), t3 = tr2.getValidFrom(), t4 = tr2.getValidUntil();
		if (t1 == null || t3 == null)
			return 0l;
		if (t1.equals(t2)) {
			t2 = null;
		}
		if (t3.equals(t4)) {
			t4 = null;
		}
		long d13 = t1.subtract(t3, unit);
		if (d13 == 0 || (t2 == null && t4 == null))
			return d13;
		if (t2 != null) {
			long d23 = t2.subtract(t3, unit);
			if (d23 <= 0)
				return d23;
			if (d13 < 0 && d23 > 0)
				return 0;
			if (t4 == null)
				return d13;
			long d14 = t1.subtract(t4, unit);
			if (d14 >= 0)
				return d14;
			return 0;
		}
		if (d13 < 0)
			return d13;
		long d14 = t1.subtract(t4, unit);
		if (d14 <= 0)
			return 0;
		return d14;
	}

	/**
	* Returns the common part of two time intervals
	*/
	public static TimeReference intersection(TimeMoment t1, TimeMoment t2, TimeMoment t3, TimeMoment t4) {
		if (t1 == null || t2 == null || t3 == null || t4 == null)
			return null;
		int c13 = t1.compareTo(t3), c24 = t2.compareTo(t4);
		TimeMoment tStart = (c13 > 0) ? t1 : t3, tEnd = (c24 < 0) ? t2 : t4;
		int c = tStart.compareTo(tEnd);
		if (c > 0)
			return null;
		return new TimeReference(tStart, tEnd);
	}

	/**
	* Returns the common part of two time intervals
	*/
	public static TimeReference intersection(TimeReference tr1, TimeReference tr2) {
		if (tr1 == null || tr2 == null)
			return null;
		TimeMoment t1 = tr1.getValidFrom(), t2 = tr1.getValidUntil(), t3 = tr2.getValidFrom(), t4 = tr2.getValidUntil();
		if (t2 == null) {
			t2 = t1;
		}
		if (t4 == null) {
			t4 = t3;
		}
		return intersection(t1, t2, t3, t4);
	}

	/**
	* Checks if two time intervals have a common part
	*/
	public static boolean doIntersect(TimeMoment t1, TimeMoment t2, TimeMoment t3, TimeMoment t4) {
		if (t1 == null || t2 == null || t3 == null || t4 == null)
			return false;
		int c13 = t1.compareTo(t3), c24 = t2.compareTo(t4);
		TimeMoment tStart = (c13 > 0) ? t1 : t3, tEnd = (c24 < 0) ? t2 : t4;
		int c = tStart.compareTo(tEnd);
		return c <= 0;
	}

	/**
	* Checks if two time intervals have a common part
	*/
	public static boolean doIntersect(TimeReference tr1, TimeReference tr2) {
		if (tr1 == null || tr2 == null)
			return false;
		TimeMoment t1 = tr1.getValidFrom(), t2 = tr1.getValidUntil(), t3 = tr2.getValidFrom(), t4 = tr2.getValidUntil();
		if (t2 == null) {
			t2 = t1;
		}
		if (t4 == null) {
			t4 = t3;
		}
		return doIntersect(t1, t2, t3, t4);
	}
}