package spade.vis.geometry;

import spade.lib.util.FloatArray;

public class RealPolyline extends Geometry {
	public RealPoint p[] = null;
	public boolean isClosed = false;
	public RealRectangle boundRect = null, labelRect = null;

	public boolean sameFirstAndLastPoints() {
		return p != null && p.length > 3 && p[0].x == p[p.length - 1].x && p[0].y == p[p.length - 1].y;
	}

	public boolean getIsClosed() {
		if (isClosed)
			return true;
		isClosed = sameFirstAndLastPoints();
		return isClosed;
	}

	/**
	* The function allowing to determine the type of this geometry. Returns line
	* if not closed, area if closed, undefined if no coordinates are given.
	*/
	@Override
	public char getType() {
		if (p == null)
			return undefined;
		if (getIsClosed())
			return area;
		return line;
	}

	@Override
	public float[] getCentroid() {
		if (centroid == null && p != null) {
			RealRectangle rr = getLabelRect();
			if (rr != null) {
				setCentroid((rr.rx1 + rr.rx2) / 2, (rr.ry1 + rr.ry2) / 2);
			}
		}
		return centroid;
	}

	/**
	* Determines the bounding rectangle of the geometry. Returns an array of 4
	* floats: 1) minimum x; 2) minimum y; 3) maximum x; 4) maximum y.
	* Uses the same static array for all instances!
	*/
	@Override
	public float[] getBoundRect() {
		determineBounds();
		if (boundRect == null)
			return null;
		bounds[0] = boundRect.rx1;
		bounds[1] = boundRect.ry1;
		bounds[2] = boundRect.rx2;
		bounds[3] = boundRect.ry2;
		return bounds;
	}

	/**
	* Returns the width of the geometry (in real coordinates).
	*/
	@Override
	public float getWidth() {
		determineBounds();
		if (boundRect == null)
			return 0;
		return boundRect.getWidth();
	}

	/**
	* Returns the height of the geometry (in real coordinates)
	*/
	@Override
	public float getHeight() {
		determineBounds();
		if (boundRect == null)
			return 0;
		return boundRect.getHeight();
	}

	/**
	* Determines the bounding rectangle of the geometry and stores it internally.
	*/
	protected void determineBounds() {
		if (boundRect == null) {
			if (p == null)
				return;
			boundRect = new RealRectangle();
			boundRect.rx1 = boundRect.rx2 = p[0].x;
			boundRect.ry1 = boundRect.ry2 = p[0].y;
			for (int i = 1; i < p.length; i++) {
				if (p[i].x < boundRect.rx1) {
					boundRect.rx1 = p[i].x;
				} else if (p[i].x > boundRect.rx2) {
					boundRect.rx2 = p[i].x;
				}
				if (p[i].y < boundRect.ry1) {
					boundRect.ry1 = p[i].y;
				} else if (p[i].y > boundRect.ry2) {
					boundRect.ry2 = p[i].y;
				}
			}
		}
	}

	protected void addCross(float x, FloatArray cross) {
		int idx = -1;
		for (int i = 0; i < cross.size() && idx < 0; i++)
			if (x < cross.elementAt(i)) {
				idx = i;
			}
		if (idx < 0) {
			cross.addElement(x);
			return;
		}
		cross.insertElementAt(x, idx);
	}

	public RealRectangle getLabelRect() {
		if (labelRect == null) {
			determineBounds();
			if (boundRect == null)
				return null;
			labelRect = (RealRectangle) boundRect.clone();
			if (p.length >= 3 && labelRect.rx2 > labelRect.rx1 && labelRect.ry2 > labelRect.ry1) {
				FloatArray cross = new FloatArray(20, 10);
				float y = (labelRect.ry1 + labelRect.ry2) / 2;
				int last = p.length - 1;
				if (p[0].x == p[last].x && p[0].y == p[last].y) {
					--last;
				}
				for (int i = 0; i <= last; i++) {
					float y1 = p[i].y, y2 = (i < last) ? p[i + 1].y : p[0].y;
					if (y1 != y2 && between(y1, y2, y)) {
						float ratio = (y - y1) / (y2 - y1);
						float x2 = (i < last) ? p[i + 1].x : p[0].x;
						addCross(p[i].x + ratio * (x2 - p[i].x), cross);
					}
				}
				if (cross.size() >= 2) {
					labelRect.rx1 = cross.elementAt(0);
					labelRect.rx2 = cross.elementAt(1);
				}
				if (cross.size() > 2)
					if (cross.size() % 2 != 0) {
						labelRect.rx2 = cross.elementAt(cross.size() - 1);
					} else {
						for (int i = 2; i < cross.size() - 1; i += 2)
							if (cross.elementAt(i + 1) - cross.elementAt(i) > labelRect.rx2 - labelRect.rx1) {
								labelRect.rx1 = cross.elementAt(i);
								labelRect.rx2 = cross.elementAt(i + 1);
							}
					}
			}
		}
		return labelRect;
	}

	/**
	* Used to determine whether at least a part of the geometry is visible in the
	* current map viewport.
	*/
	@Override
	public boolean fitsInRectangle(float x1, float y1, float x2, float y2) {
		determineBounds();
		if (boundRect == null)
			return false;
		return boundRect.fitsInRectangle(x1, y1, x2, y2);
	}

	/**
	* Used to determine whether a considerable part of the geometry is contained
	* within the given rectangle (e.g.selection of objects in a map with a frame)
	*/
	@Override
	public boolean isInRectangle(float x1, float y1, float x2, float y2) {
		determineBounds();
		if (boundRect == null)
			return false;
		if (!boundRect.fitsInRectangle(x1, y1, x2, y2))
			return false;
		for (RealPoint element : p)
			if (element.fitsInRectangle(x1, y1, x2, y2))
				return true;
		return false;
/*
    int npfit=0;
    int thr=Math.min(p.length/4,5);
    if (thr<1) thr=1;
    for (int i=0; i<p.length && npfit<thr; i++)
      if (p[i].fitsInRectangle(x1,y1,x2,y2)) ++npfit;
    return (npfit>=thr);
*/
	}

	/**
	* Checks if the point (x,y) belongs to the object. The point, in particular,
	* may represent the position of the mouse in the map.
	*/
	@Override
	public boolean contains(float x, float y, float tolerateDist) {
		return contains(x, y, tolerateDist, getIsClosed());
	}

	/**
	* Checks if the point (x,y) belongs to the object. The argument treatAsArea
	* indicates whether the geometry must be treated as a closed region. In this
	* case, the method returns true when the point is inside the region or on the
	* boundary; otherwise, true is returned only when the point is on the line.
	*/
	@Override
	public boolean contains(float x, float y, float tolerateDist, boolean treatAsArea) {
		determineBounds();
		if (boundRect == null)
			return false;
		if ((boundRect.rx2 - boundRect.rx1) > tolerateDist && (boundRect.ry2 - boundRect.ry1) > tolerateDist && !boundRect.contains(x, y, 0.0f))
			return false;
		if (treatAsArea)
			return isPointInPolygon(x, y, tolerateDist);
		return isPointOnLine(x, y, tolerateDist);
	}

	@Override
	public Object clone() {
		RealPolyline line = new RealPolyline();
		line.isClosed = isClosed;
		if (p != null) {
			line.p = new RealPoint[p.length];
			for (int i = 0; i < p.length; i++) {
				line.p[i] = (RealPoint) p[i].clone();
			}
		}
		if (boundRect != null) {
			line.boundRect = (RealRectangle) boundRect.clone();
		}
		if (labelRect != null) {
			line.labelRect = (RealRectangle) labelRect.clone();
		}
		return line;
	}

	public boolean isPointOnLine(float x, float y, float tolerateDist) {
		//return distanceToPolyline(x, y) < tolerateDist;
		if (p == null)
			return false;
		for (int i = 0; i < p.length; i++) {
			if (isThePoint(x, y, p[i].x, p[i].y, tolerateDist))
				return true;
			if (i >= p.length - 1) {
				break;
			}
			if (prov(p[i].y, p[i + 1].y, y, tolerateDist) && prov(p[i].x, p[i + 1].x, x, tolerateDist)) {
				float dx = p[i + 1].x - p[i].x;
				if (Math.abs(dx) < tolerateDist) //vertical line
					return true;
				float dy = p[i + 1].y - p[i].y;
				if (Math.abs(dy) < tolerateDist) //horizontal line
					return true;
				if (Math.abs(dx) > Math.abs(dy)) {
					float r = p[i].y + (dy * (x - p[i].x)) / dx;
					//this is the ordinate of the point on the line
					//with the abscissa equal to x
					if (Math.abs(r - y) < tolerateDist)
						return true;
				} else {
					float r = p[i].x + (dx * (y - p[i].y)) / dy;
					//this is the abscissa of the point on the line
					//with the ordinate equal to y
					if (Math.abs(r - x) < tolerateDist)
						return true;
				}
			}
		}
		return false;
	}

//ID

	public double distanceToPolyline(double cx, double cy) {
		RealPoint p1, p2;
		double x2x1, y2y1, r, dist, dist1, tx1, tx2, ty1, ty2, xx1, yy1, t1;
		if (p == null)
			return Double.NaN;
		dist1 = Double.POSITIVE_INFINITY;

		for (int j = 0; j < p.length - 1; j++) {

			dist = 0;
			p1 = p[j];
			p2 = p[j + 1];

			x2x1 = p2.x - p1.x;
			y2y1 = p2.y - p1.y;
			r = Math.sqrt(x2x1 * x2x1 + y2y1 * y2y1);

			xx1 = cx - p1.x;
			yy1 = cy - p1.y;

			if (x2x1 == 0) {
				if (y2y1 == 0) {
					// x1=x2 & y1=y2
					dist = Math.sqrt(xx1 * xx1 + yy1 * yy1);
				} else {
					// x1=x2
					tx1 = tx2 = p1.x;
					ty1 = Math.min(p1.y, p2.y);
					ty2 = Math.max(p1.y, p2.y);

					if (cy > ty2) {
						dist = Math.sqrt((cx - tx2) * (cx - tx2) + (cy - ty2) * (cy - ty2));
					} else if (cy < ty1) {
						dist = Math.sqrt((cx - tx1) * (cx - tx1) + (cy - ty1) * (cy - ty1));
					} else {
						dist = Math.abs(xx1);
					}
				}
			} else {
				if (y2y1 == 0) {
					// y1=y2
					ty1 = ty2 = p1.y;
					tx1 = Math.min(p1.x, p2.x);
					tx2 = Math.max(p1.x, p2.x);

					if (cx > tx2) {
						dist = Math.sqrt((cx - tx2) * (cx - tx2) + (cy - ty2) * (cy - ty2));
					} else if (cx < tx1) {
						dist = Math.sqrt((cx - tx1) * (cx - tx1) + (cy - ty1) * (cy - ty1));
					} else {
						dist = Math.abs(yy1);
					}
				} else {

					t1 = (yy1 * y2y1 + xx1 * x2x1) / r / r;
					if (t1 > 1) {
						dist = Math.sqrt((cx - p2.x) * (cx - p2.x) + (cy - p2.y) * (cy - p2.y));
					} else if (t1 < 0) {
						dist = Math.sqrt((cx - p1.x) * (cx - p1.x) + (cy - p1.y) * (cy - p1.y));
					} else {
						dist = Math.abs((yy1 * x2x1 - xx1 * y2y1) / r);
					}
				}
			}
			dist1 = Math.min(dist, dist1);
		}
		return dist1;
	}

	public double totalLengthInCircle(double cx, double cy, double r) {
		RealPoint p1, p2;
		boolean in1, in2;
		double lx1, ly1, lx2, ly2, A, B, C, D, E, len, len1, tx1, tx2, ty1, ty2;
		if (p == null)
			return Double.NaN;
		len1 = 0;

		for (int j = 0; j < p.length - 1; j++) {
			len = 0;
			p1 = p[j];
			p2 = p[j + 1];
			lx1 = p1.x - cx;
			ly1 = p1.y - cy;
			lx2 = p2.x - cx;
			ly2 = p2.y - cy;
			if (lx1 != lx2 && ly1 != ly2) { //diagonal
				A = 1 / (lx1 - lx2);
				B = -1 / (ly1 - ly2);
				C = -ly1 * B - lx1 * A;
			} else if (lx1 == lx2 && ly1 == ly2) {
				continue;
			} else //one point
			if (lx1 == lx2) { //vertical
				A = 1;
				B = 0;
				C = -lx1;
			} else if (ly1 == ly2) { //horizontal
				A = 0;
				B = 1;
				C = -ly1;
			} else {
				continue;
			}
			E = A * A + B * B;
			D = A * A * r * r + B * B * r * r - C * C;
			if (D <= 0) {
				continue; // no intersection
			}
			D = Math.sqrt(D);
			if (lx1 * lx1 + ly1 * ly1 < r * r && lx2 * lx2 + ly2 * ly2 < r * r) {
				len = Math.sqrt((lx2 - lx1) * (lx2 - lx1) + (ly2 - ly1) * (ly2 - ly1));
			} else {
				tx1 = (-B * D - A * C) / E;
				tx2 = (B * D - A * C) / E;
				ty1 = (A * D - B * C) / E;
				ty2 = (-A * D - B * C) / E;
				in1 = (tx1 >= Math.min(lx2, lx1)) && (tx1 <= Math.max(lx2, lx1)) && (ty1 >= Math.min(ly2, ly1)) && (ty1 <= Math.max(ly2, ly1));
				in2 = (tx2 >= Math.min(lx2, lx1)) && (tx2 <= Math.max(lx2, lx1)) && (ty2 >= Math.min(ly2, ly1)) && (ty2 <= Math.max(ly2, ly1));
				if (in1 ^ in2)
					if (lx1 * lx1 + ly1 * ly1 < r * r)
						if (in1) {
							len = Math.sqrt((tx1 - lx1) * (tx1 - lx1) + (ty1 - ly1) * (ty1 - ly1));
						} else {
							len = Math.sqrt((tx2 - lx1) * (tx2 - lx1) + (ty2 - ly1) * (ty2 - ly1));
						}
					else if (in1) {
						len = Math.sqrt((tx1 - lx2) * (tx1 - lx2) + (ty1 - ly2) * (ty1 - ly2));
					} else {
						len = Math.sqrt((tx2 - lx2) * (tx2 - lx2) + (ty2 - ly2) * (ty2 - ly2));
					}
				else if (in1 && in2) {
					len = Math.sqrt((tx2 - tx1) * (tx2 - tx1) + (ty2 - ty1) * (ty2 - ty1));
				}
			}
			len1 += len;
		}
		return len1;
	}

//~ID
	public boolean isPointInPolygon(float x, float y, float tolerateDist) {
		if (p == null || p.length < 3)
			return false;
		int ncross = 0;
		int last = p.length - 1;
		if (p[0].x == p[last].x && p[0].y == p[last].y) {
			--last;
		}
		//check by building a line from (x,y) vertically down and counting
		//number of crosses with the polygon boundary
		for (int i = 0; i <= last; i++) {
			float x1 = p[i].x, x2 = (i < last) ? p[i + 1].x : p[0].x, y1 = p[i].y, y2 = (i < last) ? p[i + 1].y : p[0].y;
			if (x1 == x2) { //vertical edge
				if (x1 == x && between(y1, y2, y))
					return true;
				continue; //skip this edge: the point is not on it
			}
			if (x == x1) { //the line, probably, crosses a vertex
				if (y == y1)
					return true; //the point coincides witht the vertex
				if (y > y1) {
					continue; //the vertex is above the point
				}
				//skip a sequence of vertical edges:
				//here the line may touch the polygon without crossing
				float prev = x, next = x;
				int k = 0, j = i;
				while (prev == x && k <= last) {
					if (j > 0) {
						--j;
					} else {
						j = last;
					}
					prev = p[j].x;
					++k;
				}
				if (prev == x)
					return false;
				k = 0;
				j = i;
				while (next == x && k <= last) {
					if (j < last) {
						++j;
					} else {
						j = 0;
					}
					next = p[j].x;
					++k;
				}
				if (between(prev, next, x)) {
					++ncross; //the line crosses the polygon
				}
				continue;
			}
			if (between(x1, x2, x)) {
				if (y1 > y && y2 > y) {
					++ncross;
				} else if (y1 < y && y2 < y) {
					;
				} else {
					//calculating crossing point
					float r = y1 + ((y2 - y1) * (x - x1)) / (x2 - x1);
					if (Math.abs(y - r) < tolerateDist)
						return true;
					if (r > y) {
						++ncross;
					}
				}
			}
		}
		return ncross % 2 == 1;
	}
}
