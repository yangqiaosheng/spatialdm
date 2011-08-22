package spade.analysis.plot;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import spade.analysis.system.Supervisor;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.Metrics;
import spade.vis.action.ObjectEventHandler;

/**
* A class that represents values of two numeric attributes
* on a scatter plot with sliders.
* The data to represent are taken from an AttributeDataPortion.
*/

public class ScatterPlotWithCrossLines extends ScatterPlot {
	protected int lastCanvasW = 0;

	public ScatterPlotWithCrossLines(boolean isIndependent, boolean allowSelection, Supervisor sup, ObjectEventHandler handler) {
		super(isIndependent, allowSelection, sup, handler);
		bkgColor = Color.lightGray;
		allowClearSelection = false;
	}

	protected double hMin = Double.NaN, hMax = Double.NaN, vMin = Double.NaN, vMax = Double.NaN;

	public void setHMin(double hMin) {
		this.hMin = hMin;
	}

	public void setHMax(double hMax) {
		this.hMax = hMax;
	}

	public void setVMin(double vMin) {
		this.vMin = vMin;
	}

	public void setVMax(double vMax) {
		this.vMax = vMax;
	}

	/**
	 * Resets its internal data, including the array of screen objects.
	 * Called when records are added to or removed from the table.
	 */
	@Override
	public void reset() {
		min1 = max1 = min2 = max2 = Double.NaN;
		if (dataTable == null || fn1 < 0 || fn2 < 0) {
			dots = null;
			return;
		}
		setup();
		hMin = min1;
		hMax = max1;
		vMin = min2;
		vMax = max2;
	}

	@Override
	public void setup() {
		super.setup();
		//redraw();
		if (canvas != null) {
			canvas.repaint(); // to ensure repainting the background
		}
	}

	@Override
	public Dimension getPreferredSize() {
		if (lastCanvasW > 0)
			return new Dimension(lastCanvasW, lastCanvasW);
		return new Dimension(40 * Metrics.mm(), 40 * Metrics.mm());
	}

	private boolean validating = false;

	@Override
	public void draw(Graphics g) {
		if (validating)
			return;
		if (!hasData() || bounds == null)
			return;
		int len, ll, lll, w0, h0, dmx, dmy;

		mx1 = mx2 = my1 = my2 = minMarg;
		if (bounds.width - mx1 - mx2 < Metrics.mm() * 5 || bounds.height - my1 - my2 < Metrics.mm() * 5)
			return;
		if (canvas != null) {
			Dimension csize = canvas.getSize();
			if (csize != null && csize.width > 0)
				if (Math.abs(csize.width - lastCanvasW) > Metrics.mm() * 10) {
					lastCanvasW = csize.width;
					validating = true;
					CManager.validateAll(canvas);
					validating = false;
					return;
				} else {
					lastCanvasW = csize.width;
				}
		}

		width = bounds.width - mx1 - mx2; //width of the ScatterPlot bounds!
		height = bounds.height - my1 - my2; // height of the ScatterPlot bounds!

		int x0 = bounds.x + mx1 - PlotObject.rad;
		g.setColor(bkgColor);
		g.fillRect(bounds.x + mx1, bounds.y + my1, width, height);
		g.setColor(Color.black);
		g.drawRect(bounds.x + mx1, bounds.y + my1, width, height);

		//-------------------- cross-lines ---------------------
		g.setColor(Color.magenta);
		if (!Double.isNaN(hMin) && hMin >= min1) {
			int xp = mapX(hMin);
			g.drawLine(xp, bounds.y + my1, xp, bounds.y + my1 + height);
		}
		if (!Double.isNaN(hMax) && hMax <= max1) {
			int xp = mapX(hMax);
			g.drawLine(xp, bounds.y + my1, xp, bounds.y + my1 + height);
		}
		if (!Double.isNaN(vMin) && vMin >= min2) {
			int yp = mapY(vMin);
			g.drawLine(bounds.x + mx1, yp, bounds.x + mx1 + width, yp);
		}
		if (!Double.isNaN(vMax) && vMax <= max2) {
			int yp = mapY(vMax);
			g.drawLine(bounds.x + mx1, yp, bounds.x + mx1 + width, yp);
		}
		//-------------------- DOTS ----------------------------
		for (int i = 0; i < dots.length; i++) {
			dots[i].reset();
			if (!dots[i].isActive) {
				continue;
			}
			double v1 = getNumericAttrValue(fn1, i), v2 = getNumericAttrValue(fn2, i);
			if (!Double.isNaN(v1) && !Double.isNaN(v2)) {
				dots[i].x = mapX(v1);
				dots[i].y = mapY(v2);
				if (isPointInPlotArea(dots[i].x, dots[i].y)) {
					g.setColor(getColorForPlotObject(i));
					//g.setColor(Color.gray);
					dots[i].draw(g);
				}
			}
		}
		drawAllSelectedObjects(g); //selected objects should not be covered by others
		if (annotationSurfacePresent()) {
			getAnnotationSurface().paint(g);
		}
	}

	public void colorsChanged(Object source) {
	}

}
