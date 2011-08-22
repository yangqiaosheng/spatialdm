package spade.lib.color;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class Rainbow extends Canvas implements MouseListener {

	protected ActionListener actionListener = null;

	static final int mm = Math.round(Toolkit.getDefaultToolkit().getScreenResolution() / 25.33f);

	public boolean usedToSelectHue = false;
	protected float currHue = -1f;
	protected int currHuePos = -1;
	protected int prefW = 8 * mm, prefH = 3 * mm;

	public void setCurrHue(float hue) {
		currHue = hue;
	}

	public float getCurrHue() {
		return currHue;
	}

	public void draw(Graphics g) {
		paint(g);
	}

	@Override
	public void paint(Graphics g) {
		Dimension d = getSize();
		int x = 1, w = d.width - 2, xf = x + w + 1;
		int nsteps = 8;
		if (usedToSelectHue) {
			nsteps = (d.width - 2) / 3;
		}
		for (int i = 0; i < nsteps; i++) {
			int dw = (xf - x) / (nsteps - i);
			g.setColor(Color.getHSBColor(((float) x) / w, 1f, 1f));
			g.fillRect(x, 0, dw, d.height - 1);
			x += dw;
		}
		g.setColor(Color.black);
		g.drawRect(0, 0, d.width - 1, d.height - 1);
		if (usedToSelectHue && currHue >= 0.0f && currHue <= 1.0f) {
			//black line will show the current hue
			currHuePos = Math.round(currHue * w);
			g.drawLine(currHuePos, 0, currHuePos, d.height);
		}
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(prefW, prefH);
	}

	public void setPreferredSize(int w, int h) {
		prefW = w;
		prefH = h;
	}

	public void setActionListener(ActionListener actionListener) {
		if (this.actionListener == null) {
			addMouseListener(this);
		}
		this.actionListener = actionListener;
	}

	@Override
	public void mousePressed(MouseEvent me) {
		if (usedToSelectHue) {
			Graphics g = getGraphics();
			Dimension d = getSize();
			int w = d.width - 2;
			if (currHuePos > 0 && currHuePos < d.width) { //erase previous black line
				g.setColor(Color.getHSBColor(currHue, 1f, 1f));
				g.drawLine(currHuePos, 0, currHuePos, d.height);
			}
			currHuePos = me.getX();
			g.setColor(Color.black);
			g.drawLine(currHuePos, 0, currHuePos, d.height);
			g.dispose();
			currHue = ((float) currHuePos) / w;
		}
		if (actionListener != null) {
			actionListener.actionPerformed(new ActionEvent(this, 0, null));
		}
	}

	@Override
	public void mouseReleased(MouseEvent me) {
	}

	@Override
	public void mouseClicked(MouseEvent me) {
	}

	@Override
	public void mouseExited(MouseEvent me) {
	}

	@Override
	public void mouseEntered(MouseEvent me) {
	}

}
