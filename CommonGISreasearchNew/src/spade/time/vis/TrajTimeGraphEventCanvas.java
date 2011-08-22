package spade.time.vis;

import java.awt.Color;
import java.awt.Graphics;

import spade.lib.basicwin.Metrics;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: Aug 12, 2009
 * Time: 4:39:48 PM
 * To change this template use File | Settings | File Templates.
 */
public class TrajTimeGraphEventCanvas extends TimeLineLabelsCanvas {

	@Override
	public void draw(Graphics g) {
		if (g == null)
			return;
		drawPlotBackground(g);
		getGrid();
		if (gridPos != null && gridTimes != null) {
			drawGrid(g, getSize().height);
		}
		g.setColor(Color.BLACK);
		g.drawString("events", 10, Metrics.fh);
	}

	@Override
	protected void drawGrid(Graphics g, int h) {
		g.setColor(Color.GRAY);
		for (int i = 0; i < gridPos.size(); i++) {
			int x = gridPos.elementAt(i) - shift + alignDiff;
			g.drawLine(x, 0, x, getSize().height);
		}
	}

}
