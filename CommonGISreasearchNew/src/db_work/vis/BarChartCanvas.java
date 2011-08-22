package db_work.vis;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.Vector;

import spade.lib.basicwin.PopupManager;
import spade.lib.util.StringUtil;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: 10-Feb-2006
 * Time: 12:28:25
 * To change this template use File | Settings | File Templates.
 */

class SingleBarInfo {
	Rectangle r = null; // bar outline
	int xidx;
	//int yidx;
}

public class BarChartCanvas extends Canvas implements MouseMotionListener {

	protected static Color bkgColor = Color.white;

	protected PopupManager popM = null;
	protected Vector hotspots = null;

	protected String labels[] = null; // labels for bars
	protected float totals[] = null; // source data
	protected float t[] = null; // transformed data used for rendering
	protected float maxT = Float.NaN, minT = Float.NaN;
	protected float absMin = Float.NaN, absMax = Float.NaN;

	public float getMin() {
		return minT;
	}

	public float getMax() {
		return maxT;
	}

	public void setAbsMinMax(float absMin, float absMax) {
		this.absMin = absMin;
		this.absMax = absMax;
	}

	protected boolean useAbsMinMax = false;

	public void setUseAbsMinMax(boolean useAbsMinMax) {
		this.useAbsMinMax = useAbsMinMax;
		Graphics g = getGraphics();
		draw(g);
		g.dispose();
	}

	protected void processT() {
		t = totals.clone();
		if (isCumulative) {
			for (int i = 1; i < t.length; i++) {
				t[i] += t[i - 1];
			}
		}
		maxT = Float.NaN;
		minT = Float.NaN;
		for (float element : t) {
			if (Float.isNaN(maxT) || maxT < element) {
				maxT = element;
			}
			if (Float.isNaN(minT) || minT > element) {
				minT = element;
			}
		}
	}

	protected boolean isCumulative = false;

	public void setIsCumulative(boolean isCumulative) {
		if (this.isCumulative == isCumulative || totals == null)
			return;
		this.isCumulative = isCumulative;
		processT();
		Graphics g = getGraphics();
		draw(g);
		g.dispose();
	}

	public BarChartCanvas(String labels[], float totals[]) {
		this.labels = labels;
		this.totals = totals;
		processT();
		addMouseMotionListener(this);
		if (popM == null) {
			popM = new PopupManager(this, "", true);
			popM.setOnlyForActiveWindow(false);
		}
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(400, 200);
	}

	@Override
	public void paint(Graphics g) {
		draw(g);
	}

	public void draw(Graphics g) {
		if (g == null)
			return;
		float min = (useAbsMinMax) ? absMin : minT, max = (useAbsMinMax) ? absMax : maxT;
		Dimension d = getSize();
		g.setColor(bkgColor);
		g.fillRect(0, 0, d.width, d.height);
		g.setColor(Color.black);
		g.drawRect(0, 0, d.width - 1, d.height - 1);
		if (totals == null)
			return;

		hotspots = new Vector(100, 100);

		int x1 = 10, x2 = d.width - 1 - 10, dx = x2 - x1, y1 = 10, y2 = d.height - 1 - 10, dy = y2 - y1;
		g.setColor(Color.lightGray);
		g.fillRect(x1 - 1, y1 - 1, dx + 3, dy + 3);
		int yzero = Math.round((min > 0) ? y2 : ((max > 0) ? y2 - (y1 - y2) * min / (max - min) : y1));
		g.setColor(Color.white);
		g.drawLine(x1, yzero, x2, yzero);
		int x = x1;
		for (int i = 0; i < t.length; i++) {
			int nextx = x1 + (dx * (i + 1)) / t.length;
			/*
			int h=Math.round(dy*t[i]/maxT);
			if (t[i]>0) g.drawRect(x,y2-h,nextx-x,h);
			*/
			SingleBarInfo sbi = new SingleBarInfo();
			sbi.xidx = i; //sbi.yidx=0;
			if (t[i] > 0) {
				int h = Math.round((yzero - y1) * t[i] / max);
				sbi.r = new Rectangle(x, yzero - h, nextx - x, h);
			} else // t[i]<=0
			if (t[i] == 0f) {
				// ?
			} else // t[i]<0
			if (maxT > 0) {
				int h = Math.round((y2 - yzero) * t[i] / min);
				sbi.r = new Rectangle(x, yzero, nextx - x, h);
			} else {
				int h = Math.round(dy * t[i] / min);
				sbi.r = new Rectangle(x, yzero, nextx - x, h);
			}
			if (sbi.r != null) {
				g.setColor(Color.lightGray.darker());
				g.fillRect(sbi.r.x, sbi.r.y, sbi.r.width, sbi.r.height);
				g.setColor(Color.black);
				g.drawRect(sbi.r.x, sbi.r.y, sbi.r.width, sbi.r.height);
				hotspots.addElement(sbi);
			}
			x = nextx;
		}
	}

	//===Mouse===
	public void mouseReleased(MouseEvent e) {
	}

	public void mousePressed(MouseEvent e) {
	}

	public void mouseClicked(MouseEvent e) {
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		if (popM == null || hotspots == null || hotspots.size() == 0)
			return;
		SingleBarInfo sbi = null;
		boolean found = false;
		for (int i = 0; i < hotspots.size() && !found; i++) {
			sbi = (SingleBarInfo) hotspots.elementAt(i);
			if (sbi.r.contains(e.getX(), e.getY())) {
				found = true;
			}
		}
		if (!found) {
			popM.setText("");
			return;
		}
		String str = "idx=" + (sbi.xidx + 1);
		if (labels != null && labels[sbi.xidx] != null) {
			str += "\n" + labels[sbi.xidx];
		}
		str += "\nvalue=" + StringUtil.floatToStr(totals[sbi.xidx], minT, maxT);
		if (isCumulative) {
			str += "\ncumulative=" + StringUtil.floatToStr(t[sbi.xidx], minT, maxT);
		}
		popM.setText(str);
		popM.setKeepHidden(false);
		popM.startShow(e.getX(), e.getY());
	}

	@Override
	public void mouseDragged(MouseEvent e) {
	}
	//===Mouse===
}
