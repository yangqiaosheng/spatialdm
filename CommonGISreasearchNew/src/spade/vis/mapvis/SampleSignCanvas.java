package spade.vis.mapvis;

import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Graphics;

import spade.vis.geometry.Sign;

class SampleSignCanvas extends Canvas {
	Sign si = null;

	public SampleSignCanvas(Sign si) {
		super();
		this.si = si;
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);
		si.draw(g, 0, 0, getSize().width, getSize().height);
	}

	public void redraw() {
		Graphics g = getGraphics();
		paint(g);
		g.dispose();
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(120, 120);
	}
}
