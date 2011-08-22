package spade.lib.basicwin;

import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Toolkit;

public class Centimeter extends Canvas {

	protected int gap = 1;
	protected int verticalDivider = 4;

	@Override
	public Dimension getMinimumSize() {
		int dpcm = (int) Math.round((Toolkit.getDefaultToolkit().getScreenResolution()) / 2.533);
		return new Dimension(dpcm, dpcm / verticalDivider);
	}

	@Override
	public Dimension getPreferredSize() {
		Dimension min = getMinimumSize();
		return new Dimension(min.width + 2 * gap, min.height + 2 * gap);
	}

	@Override
	public void paint(Graphics g) {
		Dimension size = getSize();
		int width = size.width;
		int height = size.height;
		int dpcm = getMinimumSize().width;

		g.drawLine((width - dpcm) / 2, height / 2, (width - dpcm) / 2 + dpcm, height / 2);
		g.drawLine((width - dpcm) / 2, (height - dpcm / verticalDivider) / 2, (width - dpcm) / 2, (height - dpcm / verticalDivider) / 2 + dpcm / verticalDivider);
		g.drawLine((width - dpcm) / 2 + dpcm, (height - dpcm / verticalDivider) / 2, (width - dpcm) / 2 + dpcm, (height - dpcm / verticalDivider) / 2 + dpcm / verticalDivider);
	}

}