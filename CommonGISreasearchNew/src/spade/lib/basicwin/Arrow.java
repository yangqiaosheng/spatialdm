package spade.lib.basicwin;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class Arrow extends Canvas implements MouseListener {
	static final String Clicked = "Clicked";
	private boolean IsMax = true;
	private int num = -1;
	ActionListener al = null;

	public Arrow(ActionListener al, boolean IsMax, int num) {
		super();
		this.al = al;
		this.IsMax = IsMax;
		this.num = num;
		this.addMouseListener(this);
	}

	public boolean isMax() {
		return IsMax;
	}

	public void setIsMax(boolean IsMax) {
		this.IsMax = IsMax;
		repaint();
	}

	@Override
	public void paint(Graphics g) {
		g.setColor(Color.white);
		g.fillRect(0, 0, getSize().width - 1, getSize().height - 1);
		g.setColor(Color.black);
		g.drawRect(0, 0, getSize().width - 1, getSize().height - 1);
		if (isEnabled()) {
			int x = getSize().width / 2, d = 5;
			int X[] = new int[4], Y[] = new int[4];
			X[0] = getSize().width - d;
			X[1] = X[0];
			X[2] = X[0] - d;
			X[3] = X[0];
			if (IsMax) {
				Y[0] = d;
				Y[1] = Y[0] + d;
				Y[2] = Y[0];
				Y[3] = Y[0];
				g.drawLine(X[0], Y[0], X[0] - 2 * d, Y[0] + 2 * d);
			} else {
				Y[0] = getSize().height - d;
				Y[1] = Y[0] - d;
				Y[2] = Y[0];
				Y[3] = Y[0];
				g.drawLine(X[0], Y[0], X[0] - 2 * d, Y[0] - 2 * d);
			}
			g.fillPolygon(X, Y, X.length);
			g.drawPolygon(X, Y, X.length);
		}
	}

	@Override
	public void setEnabled(boolean b) {
		super.setEnabled(b);
		repaint();
	}

	@Override
	public void mousePressed(MouseEvent me) {
		if (!isEnabled())
			return;
		IsMax = !IsMax;
		repaint();
		//getParent().postEvent(new Event(this,CLICKED,new Integer(num)));
		al.actionPerformed(new ActionEvent(this, num, Arrow.Clicked));
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

	@Override
	public void mouseClicked(MouseEvent me) {
	}

	@Override
	public Dimension minimumSize() {
		return preferredSize();
	}

	@Override
	public Dimension preferredSize() {
		return new Dimension(Metrics.fh, (5 * Metrics.mm()));
	}
}
