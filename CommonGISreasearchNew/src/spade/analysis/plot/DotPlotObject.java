package spade.analysis.plot;

import java.awt.Graphics;

/**
* Drawing of dots on a dot plot or a scatter plot
*/
public class DotPlotObject extends PlotObject {

	@Override
	public void reset() {
		x = -1;
		y = -1;
	}

	@Override
	public void draw(Graphics g) {
		if (x >= 0 && y >= 0) {
			g.drawOval(x - rad, y - rad, dm, dm);
			g.drawOval(x - rad + 1, y - rad + 1, dm - 2, dm - 2);
		}
	}

	public void fill(Graphics g) {
		if (x >= 0 && y >= 0) {
			g.fillOval(x - rad, y - rad, dm, dm);
		}
	}

	@Override
	public boolean contains(int mx, int my) {
		return (x >= 0 && y >= 0 && mx >= x - rad && mx <= x + rad && my >= y - rad && my <= y + rad);
	}
}
