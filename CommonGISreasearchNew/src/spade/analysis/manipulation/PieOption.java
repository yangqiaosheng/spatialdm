package spade.analysis.manipulation;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

public class PieOption extends Canvas {

	public final int mm = Math.round(java.awt.Toolkit.getDefaultToolkit().getScreenResolution() / 25.33f);
	public final int minD = 3 * mm, maxD = 10 * mm;
	public int margin = 5;

	public boolean useMinDiameter = true, fillCircles = false, drawBorder = false;

	protected Color fgColor = Color.black, bgColor = Color.white, borderColor = Color.gray;

	public PieOption(boolean useMinDiam) {
		super();
		useMinDiameter = useMinDiam;
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(3 * maxD + 2 * margin, maxD + 2 * margin);
	}

	public void setColor(Color fgColor) {
		this.fgColor = fgColor;
	}

	public void setBackgroundColor(Color bgColor) {
		this.bgColor = bgColor;
	}

	public void setBorderColor(Color borderColor) {
		this.borderColor = borderColor;
	}

	public void setFillCircles(boolean fillCircles) {
		this.fillCircles = fillCircles;
	}

	public void setDrawBorder(boolean drawBorder) {
		this.drawBorder = drawBorder;
		if (drawBorder) {
			margin = 6;
		} else {
			margin = 5;
		}
	}

	@Override
	public void paint(Graphics g) {
		Dimension size = getSize();
		int w = size.width, h = size.height;
		g.setColor(bgColor);
		g.fillRect(0, 0, w, h);
		g.setColor(fgColor);

		// centers of circles
		int x1 = margin + minD / 2, x2 = w - margin - maxD / 2, y1 = h - margin - minD / 2, y2 = h - margin - maxD / 2;

		float l = (float) Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
		float alpha = (float) Math.asin(maxD / (2 * l));

		int b = (int) Math.round(maxD * Math.sin(2 * alpha) / 2), c = (int) Math.round(maxD * Math.cos(2 * alpha) / 2);

		if (useMinDiameter) {
			if (fillCircles) {
				g.fillOval(margin, y1 - minD / 2, minD, minD);
			}
			g.drawOval(margin, y1 - minD / 2, minD, minD);
		}
		if (fillCircles) {
			g.fillOval(x2 - maxD / 2, y2 - maxD / 2, maxD, maxD);
		}
		g.drawOval(x2 - maxD / 2, y2 - maxD / 2, maxD, maxD);
		g.drawLine(x1, margin + maxD, x2, margin + maxD);
		g.drawLine(x1, useMinDiameter ? (h - margin - minD) : (h - margin), x2 - b, y2 - c);
		if (drawBorder) {
			g.setColor(borderColor);
			g.drawRect(0, 0, w - 1, h - 1);
		}
	}

}