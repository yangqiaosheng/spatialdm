package spade.vis.geometry;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

public class MosaicSign extends Sign {

	protected int ncols = -1, nrows = 1;
	protected Color segmColors[] = null;

	public void setSegmColors(Color segmColors[]) {
		this.segmColors = segmColors;
		adjustNcolsrows();
	}

	public Color[] getSegmColors() {
		return segmColors;
	}

	public void setNColumns(int ncols) {
		this.ncols = ncols;
		adjustNcolsrows();
	}

	public int getNColumns() {
		return ncols;
	}

	protected int W, H;

	public MosaicSign() {

		defaultMaxWidth = defaultMaxHeight = 30 * mm;
		defaultMinWidth = defaultMinHeight = 1 * mm;
		maxW = defaultMaxWidth;
		maxH = defaultMaxHeight;
		minW = defaultMinWidth;
		minH = defaultMinHeight;
		setSizes(5 * mm, 4 * mm);
		setMayChangeProperty(SIZE, true);
	}

	@Override
	public void draw(Graphics g, Rectangle r) {
		draw(g, r.x, r.y, r.width, r.height);
	}

	@Override
	public void draw(Graphics g, int x, int y, int w, int h) {
		int dx = Math.round(W / ncols);
		if (dx == 0) {
			dx = 1;
		}
		int realW = dx * ncols;
		drawMosaicSign(g, x + (w - realW) / 2, y + h / 2, false);
	}

	@Override
	public void draw(Graphics g, int x, int y) {
		int dy = Math.round(H / nrows);
		if (dy == 0) {
			dy = 1;
		}
		int realH = dy * nrows;
		drawMosaicSign(g, x, y - realH / 2, true);
	}

	private void adjustNcolsrows() {
		if (segmColors == null)
			return;
		nrows = 1;
		if (ncols < 0) {
			ncols = 10;
		}
		if (ncols > segmColors.length) {
			ncols = segmColors.length;
		}
		nrows = (int) Math.ceil((float) segmColors.length / ncols);
	}

	protected void drawMosaicSign(Graphics g, int x, int y, boolean showPointReference) {
		W = getWidth();
		H = getHeight();
		labelX = x;
		labelY = y;
		if (W < 0 || H < 0 || segmColors == null)
			return;
		int dx = Math.round(W / ncols), dy = Math.round(H / nrows);
		if (dx == 0) {
			dx = 1;
		}
		if (dy == 0) {
			dy = 1;
		}
		int w = dx * ncols, h = dy * nrows, N = 0;
		y -= h / 2;
		labelY += h / 2;
		int lastx2 = x, lasty1 = y;
		for (int r = 0; r < nrows; r++) {
			int y1 = y + r * dy, y2 = y + (r + 1) * dy;
			for (int c = 0; c < ncols; c++) {
				int x1 = x + c * dx, x2 = x + (c + 1) * dx;
				if (N < segmColors.length) {
					if (segmColors[N] != null) {
						g.setColor(java2d.Drawing2D.getTransparentColor(segmColors[N], transparency));
						g.fillRect(x1, y1, x2 - x1, y2 - y1);
					}
					if (r == nrows - 1) {
						lastx2 = x2;
					}
				}
				N++;
			}
			if (r == nrows - 1) {
				lasty1 = y1;
			}
		}
		if (this.getMustDrawFrame()) {
			// frame the sign
			g.setColor(Color.gray);
			if (nrows == 1 || N == segmColors.length + 1) {
				g.drawRect(x - 1, y - 1, w, h);
			} else {
				g.drawLine(x - 1, y - 1, x - 1, y + h);
				g.drawLine(x - 1, y + h, lastx2, y + h);
				g.drawLine(lastx2, y + h, lastx2, lasty1);
				g.drawLine(lastx2, lasty1, x + w, lasty1);
				g.drawLine(x + w, lasty1, x + w, y - 1);
				g.drawLine(x + w, y - 1, x - 1, y - 1);

			}
			if (showPointReference) {
				g.drawLine(x - 2, y + h - 1, x - 1, y + h - 1);
				g.drawLine(x - 1, y + h - 1, x - 1, y + h);
			}
		}

	}

}
