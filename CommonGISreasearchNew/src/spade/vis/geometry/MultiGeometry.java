package spade.vis.geometry;

import java.util.Vector;

public class MultiGeometry extends Geometry {
	protected Vector parts = null;
	protected RealRectangle bRect = null;

	public void addPart(Geometry part) {
		if (part == null)
			return;
		if (parts == null) {
			parts = new Vector(5, 5);
		}
		if (part instanceof MultiGeometry) {
			MultiGeometry mg = (MultiGeometry) part;
			for (int i = 0; i < mg.getPartsCount(); i++) {
				parts.addElement(mg.getPart(i));
			}
		} else {
			parts.addElement(part);
		}
		bRect = null;
	}

	public int getPartsCount() {
		if (parts == null)
			return 0;
		return parts.size();
	}

	public Geometry getPart(int n) {
		if (parts == null)
			return null;
		if (n < 0 && n >= parts.size())
			return null;
		return (Geometry) parts.elementAt(n);
	}

	/**
	* Determines the bounding rectangle of the geometry and stores it internally.
	*/
	protected void determineBounds() {
		if (bRect == null && parts != null) {
			for (int i = 0; i < parts.size(); i++) {
				Geometry part = (Geometry) parts.elementAt(i);
				float b[] = part.getBoundRect();
				if (b == null) {
					continue;
				}
				if (bRect == null) {
					bRect = new RealRectangle(b);
				} else {
					if (bRect.rx1 > b[0]) {
						bRect.rx1 = b[0];
					}
					if (bRect.ry1 > b[1]) {
						bRect.ry1 = b[1];
					}
					if (bRect.rx2 < b[2]) {
						bRect.rx2 = b[2];
					}
					if (bRect.ry2 < b[3]) {
						bRect.ry2 = b[3];
					}
				}
			}
		}
	}

	/**
	* Determines the bounding rectangle of the geometry. Returns an array of 4
	* floats: 1) minimum x; 2) minimum y; 3) maximum x; 4) maximum y.
	* Uses the same static array for all instances!
	*/
	@Override
	public float[] getBoundRect() {
		determineBounds();
		if (bRect == null)
			return null;
		bounds[0] = bRect.rx1;
		bounds[1] = bRect.ry1;
		bounds[2] = bRect.rx2;
		bounds[3] = bRect.ry2;
		return bounds;
	}

	/**
	* Returns the width of the geometry (in real coordinates).
	*/
	@Override
	public float getWidth() {
		determineBounds();
		if (bRect == null)
			return 0;
		return bRect.getWidth();
	}

	/**
	* Returns the height of the geometry (in real coordinates)
	*/
	@Override
	public float getHeight() {
		determineBounds();
		if (bRect == null)
			return 0;
		return bRect.getHeight();
	}

	public RealRectangle getLabelRect() {
		if (parts == null)
			return null;
		RealRectangle rr = null;
		//find the widest part where the label will be put
		for (int i = 0; i < parts.size(); i++) {
			Geometry part = (Geometry) parts.elementAt(i);
			if (part instanceof RealPolyline) {
				RealRectangle lr = ((RealPolyline) part).getLabelRect();
				if (lr != null)
					if (rr == null || lr.rx2 - lr.rx1 > rr.rx2 - rr.rx1) {
						rr = lr;
					}
			}
//ID
			if (part instanceof RealPoint) {
				RealRectangle lr = new RealRectangle(((RealPoint) part).x, ((RealPoint) part).y, ((RealPoint) part).x, ((RealPoint) part).y);
				if (lr != null)
					if (rr == null || lr.rx2 - lr.rx1 > rr.rx2 - rr.rx1) {
						rr = lr;
					}
			}
//~ID
		}
		if (rr != null)
			return rr;
		return null;
	}

	/**
	* The function allowing to determine the type of this geometry.
	* Check types of the parts. If the parts have different types, the
	* priorities are following: line>area>point
	* Returns undefined if there are no parts.
	*/
	@Override
	public char getType() {
		if (parts == null)
			return undefined;
		char currtype = undefined;
		for (int i = 0; i < parts.size(); i++) {
			switch (getPart(i).getType()) {
			case line:
				return line;
			case area:
				currtype = area;
				break;
			case point:
				if (currtype != area) {
					currtype = point;
				}
				break;
			}
		}
		return currtype;
	}

	/**
	* Used to determine whether at least a part of the geometry is visible in the
	* current map viewport.
	*/
	@Override
	public boolean fitsInRectangle(float x1, float y1, float x2, float y2) {
		if (parts == null)
			return false;
		for (int i = 0; i < parts.size(); i++) {
			Geometry g = (Geometry) parts.elementAt(i);
			if (g.fitsInRectangle(x1, y1, x2, y2))
				return true;
		}
		return false;
	}

	/**
	* Used to determine whether a considerable part of the geometry is contained
	* within the given rectangle (e.g.selection of objects in a map with a frame)
	*/
	@Override
	public boolean isInRectangle(float x1, float y1, float x2, float y2) {
		if (parts == null)
			return false;
		for (int i = 0; i < parts.size(); i++) {
			Geometry g = (Geometry) parts.elementAt(i);
			if (g.isInRectangle(x1, y1, x2, y2))
				return true;
		}
		return false;
	}

	/**
	* Checks if the point (x,y) belongs to the object. The point, in particular,
	* may represent the position of the mouse in the map.
	*/
	@Override
	public boolean contains(float x, float y, float tolerateDist) {
		if (parts == null)
			return false;
		for (int i = 0; i < parts.size(); i++) {
			Geometry g = (Geometry) parts.elementAt(i);
			if (g.contains(x, y, tolerateDist))
				return true;
		}
		return false;
	}

	/**
	* Checks if the point (x,y) belongs to the object. The argument treatAsArea
	* indicates whether the geometry must be treated as a closed region. In this
	* case, the method returns true when the point is inside the region or on the
	* boundary; otherwise, true is returned only when the point is on the line.
	*/
	@Override
	public boolean contains(float x, float y, float tolerateDist, boolean treatAsArea) {
		if (parts == null)
			return false;
		for (int i = 0; i < parts.size(); i++) {
			Geometry g = (Geometry) parts.elementAt(i);
			if (g.contains(x, y, tolerateDist, treatAsArea))
				return true;
		}
		return false;
	}

	@Override
	public Object clone() {
		MultiGeometry mg = new MultiGeometry();
		if (parts != null) {
			for (int i = 0; i < parts.size(); i++) {
				mg.addPart((Geometry) ((Geometry) parts.elementAt(i)).clone());
			}
		}
		return mg;
	}
}