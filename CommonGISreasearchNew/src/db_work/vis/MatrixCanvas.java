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

class SingleCellInfo {
	Rectangle r = null; // bar outline
	int xidx;
	int yidx;
	String val;
}

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: 12-Jun-2006
 * Time: 16:32:05
 * To change this template use File | Settings | File Templates.
 */
public class MatrixCanvas extends Canvas implements MouseMotionListener {

	protected static Color bkgColor = Color.white;

	protected PopupManager popM = null;
	protected Vector hotspots = null;

	protected float totals[][] = null;
	protected float mint = Float.NaN, maxt = Float.NaN;
	protected int nv = 0, nh = 0;
	protected Vector labelsV = null, labelsH = null;

	protected int fillMode = 0; // 0=xy; 1=x; 2=y;

	public void setFillMode(int fillMode) {
		if (this.fillMode == fillMode)
			return;
		this.fillMode = fillMode;
		Graphics g = getGraphics();
		draw(g);
		g.dispose();
	}

	protected boolean xAsc = true, yAsc = false;

	public void setXAsc(boolean xAsc) {
		if (this.xAsc == xAsc)
			return;
		this.xAsc = xAsc;
		Graphics g = getGraphics();
		draw(g);
		g.dispose();
	}

	public void setYAsc(boolean yAsc) {
		if (this.yAsc == yAsc)
			return;
		this.yAsc = yAsc;
		Graphics g = getGraphics();
		draw(g);
		g.dispose();
	}

	public MatrixCanvas(String attrName, Vector labelsV, Vector labelsH, float totals[][]) {
		this.totals = totals;
		this.labelsV = labelsV;
		this.labelsH = labelsH;
		addMouseMotionListener(this);
		if (popM == null) {
			popM = new PopupManager(this, "", true);
			popM.setOnlyForActiveWindow(false);
		}
	}

	protected void processT() {
		mint = maxt = Float.NaN;
		nv = totals.length;
		nh = totals[0].length;
		for (int v = 0; v < nv; v++) {
			for (int h = 0; h < nh; h++) {
				if (Float.isNaN(mint) || totals[v][h] < mint) {
					mint = totals[v][h];
				}
				if (Float.isNaN(maxt) || totals[v][h] > maxt) {
					maxt = totals[v][h];
				}
			}
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
		if (Float.isNaN(mint)) {
			processT();
		}
		Dimension d = getSize();
		g.setColor(bkgColor);
		g.fillRect(0, 0, d.width, d.height);
		g.setColor(Color.black);
		g.drawRect(0, 0, d.width - 1, d.height - 1);
		int x1 = 10, x2 = d.width - 1 - 10, dx = x2 - x1, y1 = 10, y2 = d.height - 1 - 10, dy = y2 - y1, x, y = y1;
		hotspots = new Vector(100, 100);
		for (int v = 0; v < nv; v++) {
			x = x1;
			int nexty = y1 + dy * (v + 1) / nv;
			for (int h = 0; h < nh; h++) {
				int nextx = x1 + dx * (h + 1) / nh;
				SingleCellInfo sci = new SingleCellInfo();
				sci.r = new Rectangle(x, y, nextx - x, nexty - y);
				sci.xidx = (xAsc) ? h : nh - 1 - h;
				sci.yidx = (yAsc) ? v : nv - 1 - v;
				sci.val = "Value=" + totals[sci.yidx][sci.xidx];
				if (Float.isNaN(totals[sci.yidx][sci.xidx])) {
					g.setColor(new Color(255, 255, 127));
					g.fillRect(sci.r.x + 1, sci.r.y + 1, sci.r.width - 1, sci.r.height - 1);
				} else {
					float ratio = (totals[sci.yidx][sci.xidx] - mint) / (maxt - mint);
					if (fillMode == 0) {
						ratio = (float) Math.sqrt(ratio);
					}
					g.setColor(Color.darkGray);
					switch (fillMode) {
					case 1:
						int ww = Math.round(ratio * sci.r.width);
						if (ww <= 1) {
							ww = 2;
						}
						g.fillRect(sci.r.x, sci.r.y, ww, sci.r.height);
						break;
					case 2:
						int hh = Math.round(ratio * sci.r.height);
						if (hh <= 1) {
							hh = 2;
						}
						g.fillRect(sci.r.x, sci.r.y + sci.r.height - hh, sci.r.width, hh);
						break;
					default:
						ww = Math.round(ratio * sci.r.width);
						hh = Math.round(ratio * sci.r.height);
						if (ww <= 1) {
							ww = 2;
						}
						if (hh <= 1) {
							hh = 2;
						}
						g.fillRect(sci.r.x + 1 + (sci.r.width - ww) / 2, sci.r.y + 1 + (sci.r.height - hh) / 2, ww, hh);
					}
					g.setColor(Color.BLACK);
					g.drawRect(sci.r.x, sci.r.y, sci.r.width, sci.r.height);
				}
				hotspots.addElement(sci);
				x = nextx;
			}
			y = nexty;
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
		SingleCellInfo sci = null;
		boolean found = false;
		for (int i = 0; i < hotspots.size() && !found; i++) {
			sci = (SingleCellInfo) hotspots.elementAt(i);
			if (sci.r.contains(e.getX(), e.getY())) {
				found = true;
			}
		}
		if (!found) {
			popM.setText("");
			return;
		}
		String str = "idx=" + (sci.xidx + 1) + " (" + (String) labelsH.elementAt(sci.xidx) + ")\n" + "idy=" + (sci.yidx + 1) + " (" + (String) labelsV.elementAt(sci.yidx) + ")\n" + sci.val;
		/*
		if (labels!=null && labels[sci.xidx]!=null) str+="\n"+labels[sci.xidx];
		str+="\nvalue="+StringUtil.floatToStr(totals[sci.xidx],minT,maxT);
		if (isCumulative)
		  str+="\ncumulative="+StringUtil.floatToStr(t[sci.xidx],minT,maxT);
		*/
		popM.setText(str);
		popM.setKeepHidden(false);
		popM.startShow(e.getX(), e.getY());
	}

	@Override
	public void mouseDragged(MouseEvent e) {
	}
	//===Mouse===
}
