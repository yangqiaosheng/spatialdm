package spade.lib.basicwin;

import java.awt.Canvas;
import java.awt.Dimension;

/**
 * This class is intended to be a wrapper for routines drawing directly in Graphics object
 * making them layout-friendly and causing to act as normal AWT components.
 * You can use it in your code like this:
 *
 *   protected PaintCanvas myCircle = new PaintCanvas(0, 10) {
 *     public void paint(Graphics g) {
 *       cs.drawOval(g, 0, 0, getWidth(), getHeight());
 *     }
 *   };
 */
public class PaintCanvas extends Canvas {
	int prefW = 0, prefH = 0;

	public PaintCanvas(int prefW, int prefH) {
		this.prefW = prefW;
		this.prefH = prefH;
		setVisible(true);
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(prefW, prefH);
	}
/*  public void update(Graphics g) {
      paint(g);
  }*/
}
