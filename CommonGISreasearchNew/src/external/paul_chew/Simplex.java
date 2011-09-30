package external.paul_chew;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * A Simplex is an immutable set of vertices (usually Pnts).
 *
 * @author Paul Chew
 *
 * Created July 2005. Derived from an earlier, messier version.
 */
public class Simplex extends AbstractSet implements Set {

	private List<Pnt> vertices; // The simplex's vertices
	private long idNumber; // The id number
	private static long idGenerator = 0; // Used to create id numbers
	public static boolean moreInfo = false; // True iff more info in toString
	/**
	 * Added by N.A.: the centre (e.g. of the circumcircle);
	 * must be set externally
	 */
	public Pnt centre = null;

	/**
	 * Constructor.
	 * @param collection a Collection holding the Simplex vertices
	 * @throws IllegalArgumentException if there are duplicate vertices
	 */
	public Simplex(Collection<Pnt> collection) {
		this.vertices = Collections.unmodifiableList(new ArrayList<Pnt>(collection));
		this.idNumber = idGenerator++;
		Set<Pnt> noDups = new HashSet<Pnt>(this);
		if (noDups.size() != this.vertices.size())
			throw new IllegalArgumentException("Duplicate vertices in Simplex");
	}

	/**
	 * Constructor.
	 * @param vertices the vertices of the Simplex.
	 * @throws IllegalArgumentException if there are duplicate vertices
	 */
	public Simplex(Pnt[] vertices) {
		this(Arrays.asList(vertices));
	}

	/**
	 * String representation.
	 * @return the String representation of this Simplex
	 */
	@Override
	public String toString() {
		if (!moreInfo)
			return "Simplex" + idNumber;
		return "Simplex" + idNumber + super.toString();
	}

	/**
	 * Added by N.A.
	 * @return the unique numeric identifier of this simplex
	 */
	public long getIdNumber() {
		return idNumber;
	}

	/**
	   * Dimension of the Simplex.
	   * @return dimension of Simplex (one less than number of vertices)
	   */
	public int dimension() {
		return this.vertices.size() - 1;
	}

	/**
	 * True iff simplices are neighbors.
	 * Two simplices are neighbors if they are the same dimension and they share
	 * a facet.
	 * @param simplex the other Simplex
	 * @return true iff this Simplex is a neighbor of simplex
	 */
	public boolean isNeighbor(Simplex simplex) {
		HashSet h = new HashSet(this);
		h.removeAll(simplex);
		return (this.size() == simplex.size()) && (h.size() == 1);
	}

	/**
	 * Report the facets of this Simplex.
	 * Each facet is a set of vertices.
	 * @return an Iterable for the facets of this Simplex
	 */
	public List facets() {
		List theFacets = new LinkedList();
		for (Iterator it = this.iterator(); it.hasNext();) {
			Object v = it.next();
			Set facet = new HashSet(this);
			facet.remove(v);
			theFacets.add(facet);
		}
		return theFacets;
	}

	/**
	 * Report the boundary of a Set of Simplices.
	 * The boundary is a Set of facets where each facet is a Set of vertices.
	 * @return an Iterator for the facets that make up the boundary
	 */
	public static Set boundary(Set simplexSet) {
		Set theBoundary = new HashSet();
		for (Iterator it = simplexSet.iterator(); it.hasNext();) {
			Simplex simplex = (Simplex) it.next();
			for (Iterator otherIt = simplex.facets().iterator(); otherIt.hasNext();) {
				Set facet = (Set) otherIt.next();
				if (theBoundary.contains(facet)) {
					theBoundary.remove(facet);
				} else {
					theBoundary.add(facet);
				}
			}
		}
		return theBoundary;
	}

	/* Remaining methods are those required by AbstractSet */

	/**
	 * @return Iterator for Simplex's vertices.
	 */
	@Override
	public Iterator iterator() {
		return this.vertices.iterator();
	}

	/**
	 * @return the size (# of vertices) of this Simplex
	 */
	@Override
	public int size() {
		return this.vertices.size();
	}

	/**
	 * @return the hashCode of this Simplex
	 */
	@Override
	public int hashCode() {
		return (int) (idNumber ^ (idNumber >>> 32));
	}

	/**
	 * We want to allow for different simplices that share the same vertex set.
	 * @return true for equal Simplices
	 */
	@Override
	public boolean equals(Object o) {
		return (this == o);
	}
}