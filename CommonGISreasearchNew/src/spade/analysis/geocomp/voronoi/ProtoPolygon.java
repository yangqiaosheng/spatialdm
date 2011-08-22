package spade.analysis.geocomp.voronoi;

import java.util.Vector;

import external.paul_chew.Pnt;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Feb 4, 2009
 * Time: 12:09:49 PM
 * Includes what is needed for building a single polygon
 */
public class ProtoPolygon {
	/**
	 * The centroid of the polygon
	 */
	public Pnt centre = null;
	/**
	 * The edges of the polygon.
	 */
	public Vector<Edge> edges = null;

	/**
	 * Before adding checks if the edge with these ends has been already added.
	 * Returns the resulting new edge or the existing edge with the same ends.
	 */
	public Edge addEdge(Pnt p1, Pnt p2) {
		if (p1 == null || p2 == null)
			return null;
		if (edges == null) {
			edges = new Vector<Edge>(20, 10);
		}
		for (int i = 0; i < edges.size(); i++)
			if (edges.elementAt(i).hasEndPoints(p1, p2))
				return edges.elementAt(i);
		Edge e = new Edge(p1, p2);
		edges.addElement(e);
		return e;
	}

	/**
	 * Before adding checks if the edge with these ends has been already added.
	 * Returns the resulting new edge or the existing edge with the same ends.
	 */
	public Edge addEdge(Edge e) {
		if (e == null || e.p1 == null || e.p2 == null)
			return null;
		if (edges == null) {
			edges = new Vector<Edge>(20, 10);
		}
		for (int i = 0; i < edges.size(); i++)
			if (edges.elementAt(i).hasEndPoints(e.p1, e.p2))
				return edges.elementAt(i);
		edges.addElement(e);
		return e;
	}

	/**
	 * Creates a polygon from the existing adges.
	 */
	public Pnt[] getPolygon() {
		if (edges == null || edges.size() < 1)
			return null;
		Vector<Edge> eOrd = new Vector<Edge>(edges.size(), 10); //ordered edges
		Edge e = edges.elementAt(0);
		eOrd.addElement(e);
		edges.removeElementAt(0);
		Pnt p1 = e.p1, p2 = e.p2;
		while (!edges.isEmpty()) {
			boolean attached = false;
			for (int i = 0; i < edges.size() && !attached; i++) {
				e = edges.elementAt(i);
				int idx = -1;
				if (e.hasStart(p2)) {
					idx = eOrd.size();
				} else if (e.hasEnd(p2)) {
					e.swapEnds();
					idx = eOrd.size();
				} else if (e.hasStart(p1)) {
					e.swapEnds();
					idx = 0;
				} else if (e.hasEnd(p1)) {
					idx = 0;
				}
				if (idx >= 0) {
					if (idx == 0) {
						eOrd.insertElementAt(e, 0);
						p1 = e.p1;
					} else {
						eOrd.addElement(e);
						p2 = e.p2;
					}
					edges.removeElementAt(i);
					attached = true;
				}
			}
			if (!attached) {
				System.out.println(">>> Error: cannot join polygon edges!");
				return null;
			}
		}
		edges = eOrd;
		int len = eOrd.size() + 1;
		if (!p1.same(p2)) {
			++len;
			System.out.println(">>> Polygon around " + centre + " is not closed!");
		}
		Pnt points[] = new Pnt[len];
		points[0] = p1;
		for (int i = 0; i < eOrd.size(); i++) {
			points[i + 1] = eOrd.elementAt(i).p2;
		}
		if (len > eOrd.size() + 1) {
			points[len - 1] = p1; //to have a closed polygon
		}
		return points;
	}

}
