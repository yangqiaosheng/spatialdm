package spade.vis.geometry;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

public class Circle extends Sign {
	/**
	* Indicates whether the border of the circle should be drawn
	*/
	protected boolean drawBorder = true;

	/**     ok
	* fill=true, if the circle will be colored;
	* the Manipulator uses it, if focuserMin<value<focuserMax to fill circles and
	* fill=false, the value is outside of the focuser - the circle has only  border.
	*/
	public boolean fill = true;

	/**
	* The constructor is used to set the variable isRound defined in the ancestor
	* to true and to set different maximum sizes of a sign
	*/
	public Circle() {
		usesMinSize = true; //ok
		isRound = true;
		setSizes(mm, mm);
		color = Color.green.darker(); //ok
	}

	/**
	* Sets whether the border of the circle should be drawn
	*/
	public void setDrawBorder(boolean value) {
		drawBorder = value;
	}

	/**
	* This function is to be used for point objects.
	*/
	@Override
	public void draw(Graphics g, int x, int y) {
		int diam = getDiameter();
		g.setColor(java2d.Drawing2D.getTransparentColor(color, transparency));
		//g.setColor(color);
		if (fill) {
			g.fillOval(x - diam / 2 - 1, y - diam / 2 - 1, diam + 1, diam + 1);
		}
		if (drawBorder) {
			g.setColor(borderColor);
			g.drawOval(x - diam / 2, y - diam / 2, diam, diam);
		}
		labelX = x - diam / 2;
		labelY = y + diam / 2 + 2;
	}

	/**
	* This function is to be used for area objects.
	* The rectangle in which to fit the diagram is specified.
	* Returns the bounding rectangle of the drawn diagram.
	*/
	@Override
	public void draw(Graphics g, Rectangle r) {
		draw(g, r.x, r.y, r.width, r.height);
	}

	/**
	* Same as previous, only the arguments are not wrapped into a Rectangle object
	*/
	@Override
	public void draw(Graphics g, int x, int y, int w, int h) {
		draw(g, x + w / 2, y + h / 2);
	}

	public boolean toBeDrawnVCentered() {
		return true;
	}

	public boolean shouldBeHCentered() {
		return true;
	}
}
