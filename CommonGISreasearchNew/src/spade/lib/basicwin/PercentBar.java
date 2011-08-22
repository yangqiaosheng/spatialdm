package spade.lib.basicwin;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

public class PercentBar extends Canvas {
	protected boolean leftToRight = true; // orientation
	protected float value = 0f, // current value (between 0 and 100)
			value2 = Float.NaN; // additional value to be shown in the opposite side
	protected static int insetsSize = 10;

	public PercentBar(boolean leftToRight) {
		super();
		this.leftToRight = leftToRight;
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(100, 10);
	}

	public void setValue(float value) {
		this.value = value;
		Graphics g = getGraphics();
		if (g != null) {
			paint(g);
			g.dispose();
		}
	}

	public void setValue2(float value2) {
		this.value2 = value2;
		Graphics g = getGraphics();
		if (g != null) {
			paint(g);
			g.dispose();
		}
	}

	@Override
	public void paint(Graphics g) {
		int w = getBounds().width - 2 * insetsSize, h = getBounds().height - 1;
		int d = Math.round(value * w / 100), d2 = (Float.isNaN(value2)) ? 0 : Math.round(value2 * w / 100);
		if (leftToRight) {
			if (d2 > 0) {
				g.setColor(Color.lightGray);
				g.fillRect(insetsSize + w - d2, 0, d2, h);
			}
			g.setColor(Color.white);
			g.fillRect(insetsSize + d, 0, w - d - d2, h);
			g.setColor(Color.black);
			g.fillRect(insetsSize, 0, d, h);
		} else {
			if (d2 > 0) {
				g.setColor(Color.lightGray);
				g.fillRect(insetsSize, 0, d2, h);
			}
			g.setColor(Color.white);
			g.fillRect(insetsSize, d2, w - d - d2, h);
			g.setColor(Color.black);
			g.fillRect(insetsSize + w - d, 0, d, h);
		}
		g.drawRect(insetsSize, 0, w, h);
	}

}