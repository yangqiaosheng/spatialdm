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

/**
* Draws a rectangle for selection of a bright color
*/
public class BrightColorSelection extends Canvas implements MouseListener {

	protected ActionListener actionListener = null;

	static final int mm = Math.round(Toolkit.getDefaultToolkit().getScreenResolution() / 25.33f);

	protected Color currColor = Color.white;
	protected float currHue = 0f, currSat = 0f;
	protected int currHuePos = 0, currSatPos = 0;

	public void setCurrColor(Color color) {
		if (color != null) {
			currColor = color;
			float hsb[] = new float[3];
			Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsb);
			currHue = hsb[0];
			currSat = hsb[1];
		}
	}

	public Color getCurrColor() {
		return currColor;
	}

	public void draw(Graphics g) {
		paint(g);
	}

	@Override
	public void paint(Graphics g) {
		Dimension d = getSize();
		int w = d.width - 2, h = d.height - 2;
		for (int x = 0; x < w; x += 2) {
			for (int y = 0; y < h; y += 2) {
				g.setColor(Color.getHSBColor(1.0f * x / w, 1.0f * y / h, 1.0f));
				g.fillRect(1 + x, 1 + y, 3, 3);
			}
		}
		g.setColor(Color.darkGray);
		g.drawRect(0, 0, d.width, d.height);
		currHuePos = 1 + Math.round(currHue * w);
		currSatPos = 1 + Math.round(currSat * h);
		g.setXORMode(currColor);
		g.drawLine(currHuePos - 2, currSatPos, currHuePos + 2, currSatPos);
		g.drawLine(currHuePos, currSatPos - 2, currHuePos, currSatPos + 2);
		g.setPaintMode();
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(80, 80);
	}

	public void setActionListener(ActionListener actionListener) {
		if (this.actionListener == null) {
			addMouseListener(this);
		}
		this.actionListener = actionListener;
	}

	@Override
	public void mousePressed(MouseEvent me) {
		Dimension d = getSize();
		if (me.getX() < 1 || me.getX() > d.width - 1 || me.getY() < 1 || me.getY() > d.height - 1 || (me.getX() == currHuePos && me.getY() == currSatPos))
			return;
		Graphics g = getGraphics();
		//erase previous cross
		g.setXORMode(currColor);
		g.drawLine(currHuePos - 2, currSatPos, currHuePos + 2, currSatPos);
		g.drawLine(currHuePos, currSatPos - 2, currHuePos, currSatPos + 2);
		currHuePos = me.getX();
		currSatPos = me.getY();
		int w = d.width - 2, h = d.height - 2;
		currHue = 1.0f * (currHuePos - 1) / w;
		currSat = 1.0f * (currSatPos - 1) / h;
		currColor = Color.getHSBColor(currHue, currSat, 1.0f);
		g.setXORMode(currColor);
		g.drawLine(currHuePos - 2, currSatPos, currHuePos + 2, currSatPos);
		g.drawLine(currHuePos, currSatPos - 2, currHuePos, currSatPos + 2);
		g.setPaintMode();
		if (actionListener != null) {
			actionListener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "color_changed"));
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