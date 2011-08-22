package data_load.cache;

import java.util.Vector;

import spade.vis.geometry.RealRectangle;

public class Index {
	public RealRectangle extent = null;
	public boolean[][] loaded, used;
	public float deltaX, deltaY;
	public int Xdiv, Ydiv;

	public Index(RealRectangle extent, int Xdiv, int Ydiv) {
		this.extent = extent;
		this.Xdiv = Xdiv;
		this.Ydiv = Ydiv;
		deltaX = (extent.rx2 - extent.rx1) / (Xdiv != 0 ? Xdiv : 1);
		deltaY = (extent.ry2 - extent.ry1) / (Ydiv != 0 ? Ydiv : 1);
		loaded = new boolean[Xdiv][Ydiv];
		used = new boolean[Xdiv][Ydiv];
		markCells(this.loaded, false, 0, 0, Xdiv - 1, Ydiv - 1);
	}

	public boolean nothingLoaded() {
		for (int i = 0; i < Xdiv; i++) {
			for (int j = 0; j < Ydiv; j++)
				if (loaded[i][j])
					return false;
		}
		return true;
	}

	public boolean transform(Vector requestedBounds, Vector transformedBounds) {
		Vector originalBounds;
		if (requestedBounds == null || requestedBounds.size() == 0) {//all data requested
			if (nothingLoaded())
				return true;//all data needed
			originalBounds = new Vector();
			originalBounds.addElement(extent);
		} else {
			originalBounds = (Vector) requestedBounds.clone();
		}
		markCells(this.used, false, 0, 0, Xdiv - 1, Ydiv - 1);
		for (int i = 0; i < originalBounds.size(); i++) {
			RealRectangle r = (RealRectangle) originalBounds.elementAt(i);
			int startXCell = getXCell(r.rx1);
			int startYCell = getYCell(r.ry1);
			int endXCell = getXCell(r.rx2);
			int endYCell = getYCell(r.ry2);
			for (int xcell = startXCell; xcell <= endXCell; xcell++) {
				for (int ycell = startYCell; ycell <= endYCell; ycell++) {
					if (!loaded[xcell][ycell] && !used[xcell][ycell]) {
						transformedBounds.addElement(expandRectangle(xcell, ycell, endXCell, endYCell));
					}
				}
			}
			markCells(this.loaded, true, startXCell, startYCell, endXCell, endYCell);
		}
		System.out.println("originalBounds = " + originalBounds);
		System.out.println("transformedBounds = " + transformedBounds);
		return (transformedBounds.size() > 0);
	}

	public int getXCell(float x) {
		return Math.max(Math.min((int) ((x - extent.rx1) / deltaX), Xdiv - 1), 0);
	}

	public int getYCell(float y) {
		return Math.max(Math.min((int) ((y - extent.ry1) / deltaY), Ydiv - 1), 0);
	}

	public RealRectangle getRealRectangle(int startXCell, int startCol, int endXCell, int endCol) {
		RealRectangle r = new RealRectangle();
		r.rx1 = extent.rx1 + startXCell * deltaX;
		r.ry1 = extent.ry1 + startCol * deltaY;
		r.rx2 = extent.rx1 + (endXCell + 1) * deltaX;
		r.ry2 = extent.ry1 + (endCol + 1) * deltaY;
		return r;
	}

	public RealRectangle expandRectangle(int startXCell, int startYCell, int endXCell, int endYCell) {
		int xcell = startXCell, ycell = startYCell;
		for (int xc = startXCell; xc <= endXCell; xc++) {
			if (!loaded[xcell][startYCell] && !used[xcell][startYCell]) {
				xcell = xc;
			} else {
				xcell--;
				break;
			}
		}
		endXCell = xcell;
		boolean interrupted = false;
		while (!interrupted && ycell < endYCell) {
			for (xcell = startXCell; xcell <= endXCell; xcell++)
				if (loaded[xcell][ycell + 1] || used[xcell][ycell + 1]) {
					interrupted = true;
				}
			ycell++;
		}
		endYCell = ycell;
		if (interrupted) {
			endYCell--;
		}
		markCells(this.used, true, startXCell, startYCell, endXCell, endYCell);
		return getRealRectangle(startXCell, startYCell, endXCell, endYCell);
	}

	public void debug(boolean[][] values) {
		try {
			if (System.getProperty("debug") == null)
				return;
		} catch (Exception e) {
			return;
		}
		for (int j = Ydiv - 1; j >= 0; j--) {
			for (int i = 0; i < Xdiv; i++) {
				System.out.print(values[i][j] ? "1" : "0");
			}
			System.out.println();
		}
	}

	public void markCells(boolean[][] array, boolean value, int startXCell, int startCol, int endXCell, int endCol) {
		for (int i = startXCell; i <= endXCell; i++) {
			for (int j = startCol; j <= endCol; j++) {
				array[i][j] = value;
			}
		}
	}

}
