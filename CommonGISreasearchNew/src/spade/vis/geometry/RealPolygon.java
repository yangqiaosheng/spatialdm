package spade.vis.geometry;

import java.util.Vector;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: Fraunhofer AIS.SPADE</p>
 * @author unascribed
 * @version 1.0
 */

public class RealPolygon extends RealPolyline {
	//public RealPolyline pp[];
	public Vector pp;

	public RealPolygon() {
	}

	@Override
	public boolean contains(float x, float y, float tolerateDist, boolean treatAsArea) {
		boolean r = super.contains(x, y, tolerateDist, treatAsArea);
		if (!r)
			return r;
		if (pp == null)
			return r;
		for (int i = 0; i < pp.size(); i++) {
			if (((RealPolyline) pp.elementAt(i)).contains(x, y, tolerateDist, treatAsArea))
				return false;
		}
		return r;

	}

	public boolean contains(RealPolyline pl) {
		for (int i = 0; i < pl.p.length; i++) {
			if (!super.contains(pl.p[i].x, pl.p[i].y, 0.0f))
				return false;
		}

		return true;
	}
}
