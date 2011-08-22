package spade.analysis.geocomp.voronoi;

import external.paul_chew.Pnt;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Feb 4, 2009
 * Time: 12:12:37 PM
 * An edge of a polygon defined by 2 points, instances of Pnt.
 */
public class Edge {
	/**
	 * The ends of the line
	 */
	public Pnt p1 = null, p2 = null;
	/**
	 * AAa numeric identifier of the line or its index in an array or another container.
	 * -1 means that no index or identifier has been assigned.
	 */
	public int numId = -1;

	public Edge() {
	};

	public Edge(Pnt p1, Pnt p2) {
		this.p1 = p1;
		this.p2 = p2;
	}

	public void swapEnds() {
		Pnt p = p1;
		p1 = p2;
		p2 = p;
	}

	/**
	 * Returns true if the given point coincides with
	 * the start point of the line.
	 * If the points have unique numeric identifiers,
	 * only these identifiers are checked.
	 */
	public boolean hasStart(Pnt p) {
		if (p == null || p1 == null)
			return false;
		return p1.same(p);
	}

	/**
	 * Returns true if the given point coincides with
	 * the end point of the line.
	 * If the points have unique numeric identifiers,
	 * only these identifiers are checked.
	 */
	public boolean hasEnd(Pnt p) {
		if (p == null || p2 == null)
			return false;
		return p2.same(p);
	}

	/**
	 * Returns true if each of the 2 points coincides with
	 * one of the ends of the line.
	 * If the points have unique numeric identifiers,
	 * only these identifiers are checked.
	 */
	public boolean hasEndPoints(Pnt q1, Pnt q2) {
		if (q1 == null || q2 == null || p1 == null || p2 == null)
			return false;
		if (p1.same(q1))
			return p2.same(q2);
		if (p1.same(q2))
			return p2.same(q1);
		return false;
	}

	@Override
	public String toString() {
		return p1.toString() + "|" + p2.toString();
	}
}
