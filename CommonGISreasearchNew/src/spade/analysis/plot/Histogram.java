package spade.analysis.plot;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;

public class Histogram extends Canvas {

	protected int margin = 10;

	int[] val = null;
	int[] rval = null;
	int maxCount = 1;
	float min, max;

	int precision = 2000;

	public Histogram(float min, float max) {
		this.max = max;
		this.min = min;
		val = new int[precision];
		clearHistory();
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(300, 200);
	}

	public void addValue(float v) {
		if (Float.isNaN(v) || Float.isInfinite(v))
			return;
		val[(int) ((v - min) / (max - min) * (precision - 1))]++;
	}

	public void clearHistory() {
		for (int i = 0; i < precision; i++) {
			val[i] = 0;
		}
	}

	public void reduceStatistics() {
		Dimension size = getSize();
		if (size == null)
			return;
		int width = size.width - 2 * margin;
		if (rval != null && rval.length == width)
			return;
		rval = new int[width];
		for (int i = 0; i < width; i++) {
			rval[i] = 0;
		}
		for (int i = 0; i < precision; i++) {
			int index = (int) ((float) i / (precision - 1) * (width - 1));
			rval[index] += val[i];
		}
		maxCount = 1;
		for (int i = 0; i < width; i++)
			if (maxCount < rval[i]) {
				maxCount = rval[i];
			}
	}

	@Override
	public void paint(Graphics g) {
		reduceStatistics();
		FontMetrics fm = g.getFontMetrics();
		int fh = fm.getHeight(), asc = fm.getAscent();
		String str = String.valueOf(maxCount);
		g.setColor(Color.black);
		g.drawString(str, 2, 1 + asc);
		Dimension size = getSize();
		int ys = fh + 2, yf = size.height - 2 * fh - 4, maxh = yf - ys;
		float ratio = ((float) maxh) / maxCount;
		for (int i = 0; i < rval.length; i++) {
			g.drawLine(margin + i, yf - Math.round(ratio * rval[i]), margin + i, yf);
		}
		str = String.valueOf(min);
		g.drawString(str, 2, yf + 1 + asc);
		str = String.valueOf(max);
		int w = fm.stringWidth(str);
		g.drawString(str, size.width - 2 - w, yf + 1 + asc);

	}
}
