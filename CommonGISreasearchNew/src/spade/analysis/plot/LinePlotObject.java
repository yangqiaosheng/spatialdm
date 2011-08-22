package spade.analysis.plot;

import java.awt.Graphics;

/**
* Drawing of lines on a parallel coordinate plot.
*/
public class LinePlotObject extends PlotObject {

	public int X[] = null, Y[] = null;

	public void reset(int len) {
		if (X == null || Y == null || X.length != len) {
			X = new int[len];
			Y = new int[len];
		}
		for (int i = 0; i < X.length; i++) {
			X[i] = -1;
			Y[i] = -1;
		}
	}

	@Override
	public void draw(Graphics g) {
		if (X == null || Y == null)
			return;
		for (int i = 0; i < X.length - 1; i++)
			if (def(X[i]) && def(X[i + 1]) && def(Y[i]) && def(Y[i + 1])) {
				g.drawLine(X[i], Y[i], X[i + 1], Y[i + 1]);
			}
	}

	public void fill(Graphics g) {
	}

	@Override
	public boolean contains(int mx, int my) {
		if (X == null || Y == null)
			return false;
		if ((Y[0] != -1 && my < Y[0]) || (Y[Y.length - 1] != -1 && my > Y[Y.length - 1]))
			return false;
		for (int i = 0; i < Y.length - 1; i++)
			if (Y[i + 1] != -1 && Y[i] != -1 && my <= Y[i + 1]) {
				int x0 = X[i] + (X[i + 1] - X[i]) * (my - Y[i]) / (Y[i + 1] - Y[i]);
				return Math.abs(x0 - mx) < 3;
			}
		return false;
	}

	private boolean def(int val) {
		return val != -1;
	} // defined

	public boolean IntersectsH(int mx1, int mx2, int my) {
		if (X == null || Y == null)
			return false;
		if (my < Y[0] || my > Y[Y.length - 1])
			return false;
		for (int i = 0; i < Y.length - 1; i++)
			if (my <= Y[i + 1] && def(X[i]) && def(X[i + 1]) && def(Y[i]) && def(Y[i + 1])) {
				int x0 = X[i] + (X[i + 1] - X[i]) * (my - Y[i]) / (Y[i + 1] - Y[i]);
				return x0 >= mx1 && x0 <= mx2;
			}
		return false;
	}

	public boolean IntersectsV(int mx, int my1, int my2) {
		// check ... looks to work not correctly !
		if (X == null || Y == null)
			return false;
		if (mx < X[0] || mx > X[X.length - 1])
			return false;
		for (int i = 0; i < X.length - 1; i++)
			if (mx <= X[i + 1] && def(X[i]) && def(X[i + 1]) && def(Y[i]) && def(Y[i + 1])) {
				int y0 = Y[i] + (Y[i + 1] - Y[i]) * (mx - X[i]) / (X[i + 1] - X[i]);
				return y0 >= my1 && y0 <= my2;
			}
		return false;
	}

	public boolean intersects(int x1, int y1, int x2, int y2) {
		boolean ok = IntersectsH(x1, x2, y1) || IntersectsH(x1, x2, y2);
		if (ok)
			return true;
		for (int i = 0; i < Y.length; i++)
			if (Y[i] >= y1 && Y[i] <= y2 && X[i] >= x1 && X[i] <= x2)
				return true;
		return IntersectsV(x1, y1, y2) || IntersectsV(x2, y1, y2);
	}
}
