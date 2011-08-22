package spade.time.vis;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import spade.lib.basicwin.Metrics;
import spade.lib.util.StringUtil;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: Aug 12, 2009
 * Time: 4:46:31 PM
 * To change this template use File | Settings | File Templates.
 */
public class TrajTimeGraphLegendCanvas extends Canvas implements MouseListener {

	/**
	 * action listener (TrajTimeGraphPanel) to be notified about the class selection
	 */
	protected ActionListener actionListener = null;

	public void setActionListener(ActionListener actionListener) {
		this.actionListener = actionListener;
	}

	/**
	 * mode: TGraph or TLine
	 */
	protected boolean isTGraphMode = true;
	/**
	 * range of values of the displayed attribute
	 */
	protected double dMin = Float.NaN, dMax = Float.NaN;
	/**
	 * class breaks and corresponding colors
	 */
	protected float breaks[] = null;
	protected Color colors[] = null;

	/**
	 * indicators if classes of segments are selected
	 */
	protected boolean segmSel[] = null;

	public boolean[] getSegmSelection() {
		return segmSel;
	}

	public boolean areAllSegmentsSelected() {
		if (segmSel == null)
			return true;
		else {
			for (int i = 0; i < segmSel.length; i++)
				if (!segmSel[i])
					return false;
		}
		return true;
	}

	/**
	 * clear selection
	 */
	public void clearSegmSelection() {
		if (segmSel != null) {
			for (int i = 0; i < segmSel.length; i++) {
				segmSel[i] = true;
			}
			redraw();
		}
	}

	/**
	 * positions of selection checkboxes
	 */
	protected int ySelected[] = null, xSel = -1, dxSel = -1, dySel = -1;

	public void setBreaks(float breaks[], Color colors[]) {
		if (colors != null) {
			if (segmSel == null) {
				segmSel = new boolean[colors.length];
				for (int i = 0; i < segmSel.length; i++) {
					segmSel[i] = true;
				}
			} else {
				boolean prevSel[] = segmSel;
				segmSel = new boolean[colors.length];
				for (int i = 0; i < segmSel.length; i++) {
					segmSel[i] = (i < prevSel.length) ? prevSel[i] : true;
					//ToDo: less-destructive update of selections
				}
			}
		}
		this.breaks = breaks;
		this.colors = colors;
		redraw();
	}

	public void setIsTGraphMode(boolean isTGraphMode) {
		this.isTGraphMode = isTGraphMode;
		if (isTGraphMode) {
			removeMouseListener(this);
		} else {
			addMouseListener(this);
		}
		redraw();
	}

	public void setFocuserMinMax(double min, double max) {
		this.dMin = min;
		this.dMax = max;
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
		g.setColor(getBackground());
		g.fillRect(0, 0, w + 1, h + 1);
		FontMetrics fm = g.getFontMetrics();
		if (Metrics.fh < 1 && fm.getHeight() > 0) {
			Metrics.fh = fm.getHeight();
		}
		// ...
		if (isTGraphMode) {
			g.setColor(Color.BLACK);
			g.drawString("legend", 10, 10);
			ySelected = null;
		} else {
			// calculate max width of strings
			int maxw = 0, currw;
			currw = Metrics.stringWidth(StringUtil.floatToStr((float) dMax, (float) dMin, (float) dMax));
			if (maxw < currw) {
				maxw = currw;
			}
			if (breaks != null) {
				for (int i = breaks.length - 1; i >= 0; i--) {
					currw = Metrics.stringWidth(StringUtil.floatToStr(breaks[i], (float) dMin, (float) dMax));
					if (maxw < currw) {
						maxw = currw;
					}
				}
			}
			currw = Metrics.stringWidth(StringUtil.floatToStr((float) dMin, (float) dMin, (float) dMax));
			if (maxw < currw) {
				maxw = currw;
			}
			// draw
			int y = Metrics.fh, x1 = 5, x2 = 25, dy = 2 * Metrics.fh;
			g.setColor(Color.BLACK);
			g.drawLine(x1, y, x2 + 5, y);
			String str = StringUtil.floatToStr((float) dMax, (float) dMin, (float) dMax);
			g.drawString(str, x2 + 8 + maxw - Metrics.stringWidth(str), y);
			if (colors == null) {
				y += dy;
				ySelected = null;
			} else {
				ySelected = new int[colors.length];
				xSel = x1;
				dxSel = x2 - x1;
				dySel = dy;
				for (int i = colors.length - 1; i >= 0; i--) {
					g.setColor(colors[i]);
					g.fillRect(x1, y, x2 - x1, dy);
					if (!segmSel[i]) {
						int x12 = (x1 + x2) / 2, y12 = y + dy / 2;
						g.setColor(Color.WHITE);
						g.drawLine(x12 - 4, y12 - 4, x12 + 4, y12 + 4);
						g.drawLine(x12 - 4, y12 + 4, x12 + 4, y12 - 4);
						g.setColor(Color.BLACK);
						g.drawLine(x12 - 2, y12 - 2, x12 + 2, y12 + 2);
						g.drawLine(x12 - 2, y12 + 2, x12 + 2, y12 - 2);
					}
					g.setColor(Color.BLACK);
					g.drawRect(x1, y, x2 - x1, dy);
					if (i < breaks.length) {
						str = StringUtil.floatToStr(breaks[i], (float) dMin, (float) dMax);
						g.drawString(str, x2 + 8 + maxw - Metrics.stringWidth(str), y);
					}
					ySelected[i] = y;
					y += dy;
				}
			}
			g.drawLine(x1, y, x2 + 5, y);
			str = StringUtil.floatToStr((float) dMin, (float) dMin, (float) dMax);
			g.drawString(str, x2 + 8 + maxw - Metrics.stringWidth(str), y);
		}
	}

	@Override
	public void mouseClicked(MouseEvent me) {
		if (ySelected == null || ySelected.length == 0)
			return;
		int mx = me.getX(), my = me.getY();
		if (mx < xSel || mx > xSel + dxSel)
			return;
		for (int i = 0; i < ySelected.length; i++)
			if (my >= ySelected[i] && my <= ySelected[i] + dySel) {
				//System.out.println("* mouse clicked at "+i);
				segmSel[i] = !segmSel[i];
				redraw();
				if (actionListener != null) {
					actionListener.actionPerformed(new ActionEvent(this, i, "segmSelectionChanged"));
				}
			}
	}

	@Override
	public void mousePressed(MouseEvent me) {
	}

	@Override
	public void mouseReleased(MouseEvent me) {
	}

	@Override
	public void mouseExited(MouseEvent me) {
	}

	@Override
	public void mouseEntered(MouseEvent me) {
	}

}
