package spade.analysis.vis3d;

import java.awt.Point;
import java.awt.Rectangle;

import spade.vis.dmap.MapMetrics;
import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealRectangle;

public class MapMetrics3D extends MapMetrics {
	// types of 3D projections
	public static final int PERSPECTIVE = -1;
	public static final int PARALLEL_ORTHOGRAPHIC = 0;
	public static final int PARALLEL_OBLIQUE_CAVALIER = 1;
	public static final int PARALLEL_OBLIQUE_CABINET = 2;

	public int projectionType = PERSPECTIVE;
	/*
	* All Z-values are relative. Absolute attribute values see DGeoLayerZ
	*/
	protected double z0 = 0.0f, minZ = 0.0f, maxZ = 1.0f;
	protected double absMinZ = Double.NaN, absMaxZ = Double.NaN;

//  perspect
	protected double xe = 0f, ye = 0f, ze = 0f; // Descartes view point coords
//  float x1,y1,x2,y2;              // Real (world) coords of viewed area
	protected double xc0, yc0, zc0; // Real (world) coords of the center of object
	protected double v11, v21, v12, v22, v31, v32, v13, v23, v33, v43; // Transform matrix

	/**
	* The factor to transform from normalized 2D coordinates to screen coordinates
	*/
	protected float scaleFactor = 1.0f;
	/**
	* The margin in vertical dimension at the bottom of the 3D canvas
	*/
	protected int yMarg = 0;
	/**
	* Flag indicates that application of parallel projection rules is needed
	*/
	protected boolean isParallelProjection = false;
	/**
	* Flag indicates that classical focussing is enabled/disabled
	*/
	public boolean isClassicalFocussing = false;
	/**
	* Flag indicates that XY-flat scaling by map zoom is enabled/disabled
	*/
	public boolean isXYScaling = false;
	/**
	* Flag indicates inverse representation of Z-axis
	*/
	public boolean isInverseZ = false;

	/*
	* Function returns 2D coords of the point (x,y,z)
	* depending on current viewpoint position using
	* transformation matrix corresponding to it.
	*/
	public Point get3DTransform(double x, double y, double z) {
		RealPoint p = getNormalizedCoords(x, y, z);
		double xt = p.x * scaleFactor + viewport.width / 2;
		double yt = p.y * scaleFactor;
		yt = viewport.height / 2 - yt + yMarg;
		int y_transformed = (isInverseZ ? viewport.height + yMarg - (int) Math.round(yt) : (int) Math.round(yt));
		return new Point((int) Math.round(xt), y_transformed);
	}

	/* Initialize 3D transformation:
	*  (x,y,z) - View point position in descartes coordinates.
	*/
	public void setup3D() {
		this.setup3D(xe, ye, ze);
	}

	public void setup3D(double x, double y, double z) {
		if (visTerr == null)
			return;
		/**
		*   Perspective projection transformation matrix
		*
		*   -sin(th)     -cos(ph)cos(th)     -sin(ph)cos(th)     0
		*    cos(th)     -cos(ph)sin(th)     -sin(ph)sin(th)     0
		*      0              sin(ph)             -cos(ph)       0
		*      0                0                   rho          1
		*/
		/* For parallel oblique: v31=L*cos(a); v32=L*sin(a); orthographic v33=0;     */
		xc0 = (visTerr.rx1 + visTerr.rx2) / 2;
		yc0 = (visTerr.ry1 + visTerr.ry2) / 2;
		zc0 = isFocussingActive() ? (absMinZ + absMaxZ) / 2 : (minZ + maxZ) / 2;
		xe = x;
		ye = y;
		ze = z;
		x -= xc0;
		y -= yc0;
		z -= zc0;
		float rho, theta, phi; // Sphere view point coords
		/* for parallel projection (by default orthographic)
		float alpha=(float)Math.PI/2, l=0.0f,cosa=0.0f,sina=1.0f;

		if (isParallelProjection) {
		  switch(projectionType) {
		    case PARALLEL_OBLIQUE_CAVALIER: {alpha=(float)Math.PI/4; l=1; break;}
		    case PARALLEL_OBLIQUE_CABINET:  {alpha=(float)Math.atan(2); l=0.5f; break;}
		  }
		  cosa=(float)Math.cos(alpha); sina=(float)Math.sin(alpha);
		}
		*/
		rho = (float) Math.sqrt(x * x + y * y + z * z);
		if (rho == 0) {
			theta = phi = 0;
		} else {
			if (x > 0) {
				theta = (float) Math.atan(y / x);
			} else if (x < 0) {
				theta = (float) (Math.PI + Math.atan(y / x));
			} else if (y >= 0) {
				theta = (float) Math.PI / 2;
			} else {
				theta = (float) (3 * Math.PI / 2);
			}
			phi = (float) Math.acos(z / rho);
		}
		float costh = (float) Math.cos(theta), sinth = (float) Math.sin(theta), cosph = (float) Math.cos(phi), sinph = (float) Math.sin(phi);
		v11 = -sinth;
		v12 = -cosph * costh;
		v13 = -sinph * costh;
		v21 = costh;
		v22 = -cosph * sinth;
		v23 = -sinph * sinth;
		v31 = 0.0f;
		v32 = sinph;
		v33 = -cosph;
		v43 = rho;
		if (isParallelProjection) {
			/*  v31=l*cosa; v32=l*sina*sinph; */v33 = 0.0f;
		}
		computeScaleFactor();
	}

	/*
	* The function returns normalized 2D coords of the point (x,y,z)
	* depending on current viewpoint position using the transformation matrix
	* corresponding to the viewpoint. No scaling is applied!
	*/
	protected RealPoint getNormalizedCoords(double x, double y, double z) {
		x -= xc0;
		y -= yc0;
		z -= zc0;
		double x_e = x * v11 + y * v21 + z * v31, y_e = x * v12 + y * v22 + z * v32, z_e = x * v13 + y * v23 + z * v33 + v43, xt = x_e / z_e, yt = y_e / z_e;
		RealPoint p = new RealPoint();
		p.x = (float) xt;
		p.y = (float) yt;
		return p;
	}

	/**
	* Calculates the scaling factor for transforming from normalized 2D
	* coordinates to the screen coordinates.
	*/
	public void computeScaleFactor() {
		if (viewport == null || visTerr == null)
			return;
		double x[] = { visTerr.rx1, visTerr.rx1, visTerr.rx1, visTerr.rx1, visTerr.rx2, visTerr.rx2, visTerr.rx2, visTerr.rx2 }, y[] = { visTerr.ry1, visTerr.ry1, visTerr.ry2, visTerr.ry2, visTerr.ry1, visTerr.ry1, visTerr.ry2, visTerr.ry2 }, z1[] = {
				absMinZ, absMaxZ, absMinZ, absMaxZ, absMinZ, absMaxZ, absMinZ, absMaxZ }, z[] = { minZ, maxZ, minZ, maxZ, minZ, maxZ, minZ, maxZ };
		double minY = Double.MAX_VALUE, maxY = Double.MIN_VALUE, minX = minY, maxX = maxY;
		int iMinY = 0, iMaxY = 0;
		for (int i = 0; i < x.length; i++) {
			RealPoint p = null;
			p = isFocussingActive() ? getNormalizedCoords(x[i], y[i], z1[i]) : getNormalizedCoords(x[i], y[i], z[i]);
			if (p.x < minX) {
				minX = p.x;
			}
			if (p.x > maxX) {
				maxX = p.x;
			}
			if (p.y < minY) {
				minY = p.y;
				iMinY = i;
			}
			if (p.y > maxY) {
				maxY = p.y;
				iMaxY = i;
			}
		}
		double r1 = 1.0f * viewport.width / (maxX - minX), r2 = 1.0f * viewport.height / (maxY - minY);
		scaleFactor = (float) Math.min(r1, r2);
		yMarg = 0;
		Point p1 = isFocussingActive() ? get3DTransform(x[iMinY], y[iMinY], z1[iMinY]) : get3DTransform(x[iMinY], y[iMinY], z[iMinY]), p2 = isFocussingActive() ? get3DTransform(x[iMaxY], y[iMaxY], z1[iMaxY]) : get3DTransform(x[iMaxY], y[iMaxY],
				z[iMaxY]);
		yMarg = (viewport.height - (p1.y - p2.y)) / 2 - p2.y;
	}

	@Override
	protected void reset() {
		x0 = 0.0f;
		y0 = 0.0f;
		z0 = 0.0f;
		step = 1.0f;
		scrMaxY = 0;
		vpx = vpy = 0;
	}

	public void setXYScaling(boolean flag) {
		if (isXYScaling == flag)
			return;
		isXYScaling = !isXYScaling;
	}

	public void setInvertZDimension(boolean flag) {
		if (isInverseZ == flag)
			return;
		isInverseZ = !isInverseZ;
		computeScaleFactor();
	}

	@Override
	public void setVisibleTerritory(RealRectangle rr) {
		if (rr == null) {
			visTerr = null;
			reset();
		} else {
			if (visTerr == null) {
				visTerr = (RealRectangle) rr.clone();
			} else {
				visTerr.rx1 = rr.rx1;
				visTerr.ry1 = rr.ry1;
				visTerr.rx2 = rr.rx2;
				visTerr.ry2 = rr.ry2;
			}
			setup();
		}
		// System.out.println("Visible territory changed!");
	}

	public void setZLimits(double lowZ, double upZ) {
		if (Double.isNaN(absMinZ)) {
			absMinZ = lowZ;
		}
		if (Double.isNaN(absMaxZ)) {
			absMaxZ = upZ;
		}
		minZ = lowZ;
		maxZ = upZ;
	}

	public void resetZLimits(double lowZ, double upZ) {
		absMinZ = absMaxZ = Float.NaN;
		setZLimits(lowZ, upZ);
	}

	public void setMinZ(double lowZ) {
		if (Double.isNaN(absMinZ)) {
			absMinZ = lowZ;
		}
		minZ = lowZ;
	}

	public void setMaxZ(double upZ) {
		if (Double.isNaN(absMaxZ)) {
			absMaxZ = upZ;
		}
		maxZ = upZ;
	}

	public double getMinZ() {
		return minZ;
	}

	public double getMaxZ() {
		return maxZ;
	}

	public double getAbsMinZ() {
		return absMinZ;
	}

	public double getAbsMaxZ() {
		return absMaxZ;
	}

	public boolean isFocussingActive() {
		return ((absMinZ != minZ) || (absMaxZ != maxZ) && !isClassicalFocussing);
	}

	@Override
	public RealRectangle getVisibleTerritory() {
		//if (viewport!=null)
		//return new RealRectangle(absX(0),absY(viewport.height),absX(viewport.width),absY(0));
		if (visTerr == null)
			return new RealRectangle(0, 0, 200, 200);
		return visTerr;
	}

	@Override
	public void setViewportBounds(int x, int y, int width, int height) {
		if (viewport == null) {
			viewport = new Rectangle(x, y, width, height);
		} else {
			viewport.x = x;
			viewport.y = y;
			viewport.width = width;
			viewport.height = height;
		}
		vpx = x;
		vpy = y;
		setup();
	}

	@Override
	public void setViewportBounds(Rectangle r) {
		if (r == null) {
			viewport = null;
			reset();
		} else {
			setViewportBounds(r.x, r.y, r.width, r.height);
		}
	}

	@Override
	public Rectangle getViewportBounds() {
		return viewport;
	}

	@Override
	public void setup() {
		if (visTerr == null || viewport == null) {
			reset();
			return;
		}
		float stepx = (visTerr.rx2 - visTerr.rx1) / viewport.width, stepy = (visTerr.ry2 - visTerr.ry1) / viewport.height;

		step = Math.max(stepx, stepy);
		// System.out.println("Step: "+step);
		x0 = visTerr.rx1;
		y0 = visTerr.ry1;
		scrMaxY = (int) Math.ceil((visTerr.ry2 - y0) / step);
		setup3D(xe, ye, ze);
	}

	public boolean getParallelProjectionEnabled() {
		return isParallelProjection;
	}

	public void setProjectionType(int type) {
		if (projectionType != type) {
			projectionType = type;
			isParallelProjection = (type != PERSPECTIVE);
			setup3D(xe, ye, ze);
			//System.out.println("Parallel Projection is "+(isParallelProjectionEnabled?"en":"dis")+"abled");
		}
	}

	@Override
	public int scrX(float x, float y) {
		Point pnt = get3DTransform(x, y, z0);
		return pnt.x;
	}

	@Override
	public int scrY(float x, float y) {
		Point pnt = get3DTransform(x, y, z0);
		return pnt.y;
	}

	@Override
	public float absX(int x) {
		return visTerr.rx1 - 10; //to avoid reaction to the mouse
	}

	@Override
	public float absY(int y) {
		return visTerr.ry1 - 10;
	}

	@Override
	public Point getScreenPoint(RealPoint rp) {
		return get3DTransform(rp.x, rp.y, z0);
	}

	@Override
	public RealPoint getRealPoint(Point sp) {
		RealPoint rp = new RealPoint();
		rp.x = absX(sp.x);
		rp.y = absY(sp.y);
		return rp;
	}

	@Override
	public float getPixelValue() {
		return (float) step;
	}

	public void setZ0(double z0) {
		this.z0 = z0;
	}

	public double getZ0() {
		return z0;
	}

	public double getViewpointZ() {
		return ze;
	}

	public RealPoint getViewpointXY() {
		RealPoint vp = new RealPoint();
		vp.x = (float) xe;
		vp.y = (float) ye;
		return vp;
	}

	public float getDistanceToViewPoint(float x, float y, float z) {
		return (float) Math.sqrt(Math.pow((xe - x), 2) + Math.pow((ye - y), 2) + Math.pow((ze - z), 2));
	}

	public boolean isViewPointChanged(float x, float y, float z) {
		return (getDistanceToViewPoint(x, y, z) > 0.0f);
	}
}
