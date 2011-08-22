package spade.time.vis;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;

import spade.lib.basicwin.FocusListener;
import spade.lib.basicwin.Focuser;
import spade.lib.basicwin.FocuserCanvas;
import spade.lib.basicwin.Metrics;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: Aug 12, 2009
 * Time: 4:44:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class TrajTimeGraphFocuserCanvas extends FocuserCanvas {

	protected double min = 0d, max = 1d;

	public TrajTimeGraphFocuserCanvas(FocusListener fl) {
		super(new Focuser(), true);
		focuser.addFocusListener(fl);
		focuser.setIsVertical(true);
		focuser.setAbsMinMax(min, max);
		focuser.setToDrawCurrMinMax(true);
		focuser.setTextDrawing(true);
		focuser.setIsLeft(false);
		focuser.setIsUsedForQuery(false);
		//focuser.setBkgColor(Color.WHITE);
		focuser.setPlotAreaColor(Color.WHITE);
	}

	public void setFocuserMinMax(double min, double max) {
		this.min = min;
		this.max = max;
		focuser.setAbsMinMax(min, max);
		focuser.setCurrMinMax(min, max);
		redraw();
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(100, 50);
	}

	@Override
	public void paint(Graphics g) {
		draw(g);
	}

	public void redraw() {
		draw(getGraphics());
	}

	public void draw(Graphics g) {
		if (g == null)
			return;
		Dimension size = getSize();
		int w = size.width, h = size.height;
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, w + 1, h + 1);
		FontMetrics fm = g.getFontMetrics();
		if (Metrics.fh < 1 && fm.getHeight() > 0) {
			Metrics.fh = fm.getHeight();
		}
		//
		int height = getBounds().height - 2 * Metrics.fh;
		focuser.setAlignmentParameters(0, Metrics.fh + height, height);
		focuser.draw(g);
		// ...
		g.setColor(Color.BLACK);
		g.drawString("focuser", 10, 10);
	}

}
