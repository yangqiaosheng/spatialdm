package spade.analysis.plot;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.ScrollPane;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import spade.analysis.manipulation.Slider;
import spade.analysis.manipulation.SliderListener;
import spade.analysis.system.Supervisor;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.Metrics;
import spade.vis.action.ObjectEventHandler;

/**
* A class that represents values of two numeric attributes
* on a scatter plot with sliders.
* The data to represent are taken from an AttributeDataPortion.
*/

public class ScatterPlotWithSliders extends ScatterPlot implements SliderListener {
	protected int lastCanvasW = 0;

	protected Slider horSlider = null, vertSlider = null;

	public Slider getHSlider() {
		return horSlider;
	}

	public Slider getVSlider() {
		return vertSlider;
	}

	protected Color spColors[] = null;

	public void setSpColors(Color spColors[]) {
		this.spColors = spColors;
	}

	protected ActionListener spClickListener = null;

	public void setClickListener(ActionListener spClickListener) {
		this.spClickListener = spClickListener;
	}

	public ScatterPlotWithSliders(boolean isIndependent, boolean allowSelection, Supervisor sup, ObjectEventHandler handler) {
		super(isIndependent, allowSelection, sup, handler);
		allowClearSelection = false;
	}

	/**
	 * Resets its internal data, including the array of screen objects.
	 * Called when records are added to or removed from the table.
	 */
	@Override
	public void reset() {
		min1 = max1 = min2 = max2 = Float.NaN;
		if (dataTable == null || fn1 < 0 || fn2 < 0) {
			dots = null;
			return;
		}
		setup();
	}

	public void setMinMax(float minHor, float maxHor, float minVert, float maxVert) {
		super.setMinMax(minHor, maxHor, minVert, maxVert);
		if (horSlider != null) {
			horSlider.setMinMax(min1, max1);
		}
		if (vertSlider != null) {
			vertSlider.setMinMax(min2, max2);
		}
	}

	@Override
	public void setup() {
		super.setup();
		if (horSlider != null) {
			horSlider.setMinMax(min1, max1);
			if (minTime1 != null && maxTime1 != null) {
				horSlider.setAbsMinMaxTime(minTime1, maxTime1);
			}
		}
		if (vertSlider != null) {
			vertSlider.setMinMax(min2, max2);
			if (minTime2 != null && maxTime2 != null) {
				vertSlider.setAbsMinMaxTime(minTime2, maxTime2);
			}
		}
		//redraw();
		if (canvas != null) {
			canvas.repaint(); // to ensure repainting the background
		}
	}

	public void constructSliders() {
		if (horSlider == null) {
			horSlider = new Slider();
			horSlider.setIsHorisontal(true); //bottom position
			horSlider.setMinMax(min1, max1);
			if (minTime1 != null && maxTime1 != null) {
				horSlider.setAbsMinMaxTime(minTime1, maxTime1);
			}
			horSlider.setMaxNBreaks(4);
			horSlider.addSliderListener(this);
			horSlider.setCanvas(canvas);
		}
		if (vertSlider == null) {
			vertSlider = new Slider();
			vertSlider.setIsHorisontal(false); //left position
			vertSlider.setMinMax(min2, max2);
			if (minTime2 != null && maxTime2 != null) {
				vertSlider.setAbsMinMaxTime(minTime2, maxTime2);
			}
			vertSlider.setMaxNBreaks(4);
			vertSlider.addSliderListener(this);
			vertSlider.setCanvas(canvas);
		}
	}

	@Override
	protected Color getColorForPlotObject(int idx) {
		return Color.gray;
	}

	@Override
	public Dimension getPreferredSize() {
		//if (lastCanvasW>0)
		//return new Dimension(lastCanvasW,lastCanvasW);
		lastCanvasW = getAvailableWidth();
		if (lastCanvasW <= 0) {
			lastCanvasW = 40 * Metrics.mm();
		}
		return new Dimension(lastCanvasW, lastCanvasW);
	}

	protected int getAvailableWidth() {
		if (canvas != null && canvas.isShowing()) {
			ScrollPane scp = CManager.getScrollPane(canvas);
			if (scp != null)
				return scp.getSize().width - scp.getVScrollbarWidth() - 5;
			return canvas.getSize().width;
		}
		return 0;
	}

	@Override
	public void draw(Graphics g) {
		int w = getAvailableWidth();
		if (w > 0 && Math.abs(w - lastCanvasW) > Metrics.mm() * 10) {
			CManager.validateAll(canvas);
			return;
		}
		if (!hasData() || bounds == null)
			return;
		int len, ll, lll, w0, h0, dmx, dmy;

		mx1 = mx2 = my1 = my2 = minMarg;
		if (bounds.width - mx1 - mx2 < Metrics.mm() * 5 || bounds.height - my1 - my2 < Metrics.mm() * 5)
			return;

		Dimension dm = vertSlider.getPreferredSize();
		int heightVertSlider = dm.height, widthVertSlider = dm.width;
		Rectangle rect = new Rectangle(mx1, my1, widthVertSlider, bounds.height - widthVertSlider - my2 - my1/*-2*fh*/);
		vertSlider.setBounds(rect);
		vertSlider.draw(g);
		mx1 += 3;
		width = bounds.width - mx1 - mx2 - widthVertSlider; //width of the ScatterPlot bounds!
		height = bounds.height - my1 - my2 - widthVertSlider; // height of the ScatterPlot bounds!
		mx1 += widthVertSlider;

		int x0 = bounds.x + mx1, y0 = bounds.y + my1;
		g.setColor(bkgColor);
		g.fillRect(x0 - PlotObject.rad, y0 - PlotObject.rad, PlotObject.rad, height + 2 * PlotObject.rad);
		g.fillRect(x0 + width, y0 - PlotObject.rad, PlotObject.rad, height + 2 * PlotObject.rad);
		g.fillRect(x0, y0 - PlotObject.rad, width, PlotObject.rad);
		g.fillRect(x0, y0 + height, width, PlotObject.rad);
		g.setColor(plotAreaColor);
		g.drawRect(x0, y0, width, height);
		g.drawLine(x0, y0, x0, y0 + height);
		g.drawLine(x0, y0 + height, x0 + width, y0 + height);

		//--------the-Break-Line-Drawing--------------------------
		int heightHorSlider = horSlider.getPreferredSize().height;
		int widthHorSlider = horSlider.getPreferredSize().width;
		rect = new Rectangle(mx1, bounds.height - my2 - heightHorSlider + 3, width, heightHorSlider);
		horSlider.setBounds(rect);
		horSlider.draw(g);
		int vnbr = vertSlider.getNBreaks(), hnbr = horSlider.getNBreaks();
		int n = -1;
		for (int v = -1; v < vnbr; v++) {
			for (int h = -1; h < hnbr; h++) {
				n++;
				if (spColors == null || n >= spColors.length) {
					break;
				}
				int x1, x2, y1, y2;
				if (h == -1) {
					x1 = x0;
				} else {
					x1 = mapX(horSlider.getBreakValue(h));
				}
				if (h < hnbr - 1) {
					x2 = mapX(horSlider.getBreakValue(h + 1));
				} else {
					x2 = x0 + width;
				}
				if (v == -1) {
					y2 = y0 + height;
				} else {
					y2 = mapY(vertSlider.getBreakValue(v));
				}
				if (v < vnbr - 1) {
					y1 = mapY(vertSlider.getBreakValue(v + 1));
				} else {
					y1 = y0;
				}
				g.setColor(spColors[n]);
				g.fillRect(x1, y1, x2 - x1, y2 - y1);
			}
		}
		g.setColor(Color.black);
		for (int i = 0; i < vnbr; i++) {
			dmy = mapY(vertSlider.getBreakValue(i));
			g.drawLine(x0, dmy, x0 + width, dmy);
		}
		for (int i = 0; i < hnbr; i++) {
			dmx = mapX(horSlider.getBreakValue(i));
			g.drawLine(dmx, y0, dmx, y0 + height);
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
					//g.setColor(getColorForPlotObject(i));
					g.setColor(Color.gray);
					dots[i].draw(g);
				}
			}
		}
		drawAllSelectedObjects(g); //selected objects should not be covered by others
		if (annotationSurfacePresent()) {
			getAnnotationSurface().paint(g);
		}
	}

	private void drawLimitLine(int attrN, int n, int pos) {
	}

	@Override
	protected void processMouseClick(int scrX, int scrY) {
		double x = absX(scrX), y = absY(scrY);
		spClickListener.actionPerformed(new ActionEvent(this, 0, "" + x + " " + y));
	}

	// Implementation of SliderListener
	@Override
	public void breaksChanged(Object source, double[] breaks, int nBreaks) {
		//redraw();
		if (canvas != null) {
			canvas.repaint();
		}
	}

	@Override
	public void breakIsMoving(Object source, int n, double currValue) {
		//if (canvas!=null) canvas.repaint();
		redraw();
	}

	@Override
	public void colorsChanged(Object source) {
	}

}
