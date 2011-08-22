package spade.lib.color;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import spade.lib.basicwin.ActionSupport;

public class ColorCanvas extends Canvas implements MouseListener {
	static final int mm = Math.round(Toolkit.getDefaultToolkit().getScreenResolution() / 25.33f);

	protected ActionListener actionListener = null;
	protected Color color = null;
	protected int width = 4 * mm, height = 3 * mm;
	protected String actionCommand = null;
	protected ActionSupport asup = null;

	public void setColor(Color color) {
		this.color = color;
		if (isShowing()) {
			repaint();
		}
	}

	public Color getColor() {
		return color;
	}

	@Override
	public void paint(Graphics g) {
		if (color != null) {
			Dimension d = getSize();
			g.setColor(color);
			g.fillRect(0, 0, d.width - 1, d.height - 1);
			g.setColor(Color.black);
			g.drawRect(0, 0, d.width - 1, d.height - 1);
		}
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(width, height);
	}

	public void setPreferredSize(int w, int h) {
		width = w;
		height = h;
	}

	public void setActionListener(ActionListener actionListener) {
		if (this.actionListener == null) {
			addMouseListener(this);
		} else {
			asup.removeActionListener(this.actionListener);
		}
		this.actionListener = actionListener;
		if (asup == null) {
			asup = new ActionSupport();
		}
		asup.addActionListener(actionListener);
	}

	public void setActionCommand(String cmd) {
		actionCommand = cmd;
	}

	@Override
	public void mousePressed(MouseEvent me) {
		if (asup != null) {
			asup.fireActionEvent(this, actionCommand);
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
