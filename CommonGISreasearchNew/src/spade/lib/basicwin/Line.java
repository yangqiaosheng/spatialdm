package spade.lib.basicwin;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

public class Line extends Canvas {
	boolean isVert = false;

	public Line(boolean isVertical) {
		super();
		isVert = isVertical;
	}

	@Override
	public Dimension getPreferredSize() {
		/*
		Dimension d=null;
		ScrollPane scp=CManager.getScrollPane(this);
		if (scp!=null) d=scp.getViewportSize();
		if (d==null && getParent()!=null) d=getParent().getSize();
		if (d!=null && d.width>0 && d.height>0) {
		  if (isVert) return new Dimension(4,d.height);
		  else return new Dimension(d.width,4);
		}
		*/
		if (isVert)
			return new Dimension(4, 50);
		return new Dimension(50, 4);
	}

	@Override
	public void paint(Graphics g) {
		Dimension d = getSize();
		g.setColor(Color.darkGray);
		if (isVert) {
			g.drawLine(1, 0, 1, d.height);
		} else {
			g.drawLine(0, 1, d.width, 1);
		}
		g.setColor(Color.gray);
		if (isVert) {
			g.drawLine(2, 0, 2, d.height);
		} else {
			g.drawLine(0, 2, d.width, 2);
		}
	}
}