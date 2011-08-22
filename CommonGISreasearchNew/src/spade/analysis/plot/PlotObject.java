package spade.analysis.plot;

import java.awt.Graphics;

import spade.lib.basicwin.Metrics;

/**
* The base class for DotPlotObject (drawing of dots on a dot plot or scatter
* plot) and LinePlotObject (drawing of lines on a parallel coordinates plot).
*/
public class PlotObject {
	public int x = -1, y = -1;
	public static final int rad = Math.round(0.65f * Metrics.mm()), dm = rad * 2;

	public String id = null;
	public boolean isHighlighted = false, isSelected = false, isActive = true;

	public void reset() {
		x = -1;
		y = -1;
	}

	public void draw(Graphics g) {
	}

	public boolean contains(int mx, int my) {
		return true;
	}
}
