package spade.analysis.plot;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;

import spade.lib.basicwin.Drawable;

public class SegmentedBarPlot implements Drawable {

	//protected DataTable dTable=null;
	protected Color classColors[] = null;
	protected int totalClassCounts[] = null, classCounts[] = null;
	protected boolean isActive = true;

	protected int W = 100, H = 10;

	public SegmentedBarPlot(/*DataTable dTable, */Color classColors[], int totalClassCounts[], int classCounts[]) {
		//this.dTable=dTable;
		this.classColors = classColors;
		this.totalClassCounts = totalClassCounts;
		this.classCounts = classCounts;
	}

	public int[] getTotalClassCounts() {
		return totalClassCounts;
	}

	public void setIsActive(boolean isActive) {
		this.isActive = isActive;
	}

	public void draw(Graphics g, int y, int w, int h) {
		if (!isActive)
			return;
		W = w;
		H = h;
		int x = 0;
		for (int i = 1; i < totalClassCounts.length; i++) {
			if (totalClassCounts[0] == 0) {
				continue;
			}
			int dx = Math.round(w * totalClassCounts[i] / totalClassCounts[0]), ddx = Math.round(w * classCounts[i] / totalClassCounts[0]);
			if (ddx == dx) {
				ddx--;
			}
			//...
			x += dx;
			g.setColor(classColors[i - 1]);
			g.fillRect(x - ddx, y, ddx, h);
			g.setColor(Color.darkGray);
			g.drawLine(x, y, x, y + h);
		}
	}

	/**
	 * methods of Drawable interface
	 */
	@Override
	public void setCanvas(Canvas c) {
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(W, H);
	}

	@Override
	public void setBounds(Rectangle bounds) {
		W = bounds.width;
		H = bounds.height;
	}

	@Override
	public Rectangle getBounds() {
		return new Rectangle(W, H);
	}

	@Override
	public void draw(Graphics g) {
		draw(g, 0, W, H);
	}

	@Override
	public void destroy() {
	}

	@Override
	public boolean isDestroyed() {
		return false;
	}
}
