package spade.analysis.vis3d;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;

public class EyePointer {
	static final int vert_ext = 6; // vertical extent
	static final int hor_ext = 15; //  horisontal extent
	private int x, y; // location (x,y) of upper left corner in parent's canvas
	private int offset_x, offset_y; // offset from (0,0) in parent's canvas
	int i_radius = 2 * vert_ext / 3; // radius of iris
	int z_radius = 2 * i_radius / 3; // radius of eye's center
	protected Point eyePosition = null; //  coordinates of eye's center
	protected Rectangle eyeBounds = new Rectangle(2 * hor_ext + 1, 2 * vert_ext + 1);
	protected Color eyeIrisColor = Color.getHSBColor(0.62f, 1.0f, 0.85f); // light blue
//  boolean position_set=false;

	int minX = 0, maxX = 1600, minY = 0, maxY = 1200;

	public EyePointer(int x0, int y0) {
		offset_x = x0 - hor_ext;
		offset_y = y0 - vert_ext;
		eyePosition = new Point(hor_ext + offset_x, vert_ext + offset_y);
	}

	public void drawInPos(Point pos, Graphics g) {
		if (pos == null)
			return;
		this.drawInPos(pos.x, pos.y, g);
	}

	public void drawInPos(int pos_x, int pos_y, Graphics g) {
		Color c_orig = g.getColor();
		x = pos_x - hor_ext;
		y = pos_y - vert_ext;
		/* if (!position_set) {
		   if (x<minX || y<minY ) {
		     if (x<minX) x=minX;
		     if (y<minY) y=minY;
		     eyePosition.setLocation(x+hor_ext,y+vert_ext);
		   } else eyePosition.setLocation(pos_x,pos_y);
		   if (pos_x>maxX-hor_ext || pos_y>maxY-vert_ext ) {
		     if (pos_x>maxX-hor_ext) x=maxX-hor_ext;
		     if (pos_y>maxY-vert_ext) y=maxY-vert_ext;
		     eyePosition.setLocation(x,y);
		   } else { eyePosition.setLocation(x,y); }
		 }
		 */
		// setting location of the eye and bounds
		eyePosition.setLocation(x + hor_ext, y + vert_ext);
		eyeBounds.setLocation(x, y);
		// setting additional drawing parameters
		int x0 = x + hor_ext / 3;
		int y0 = y + vert_ext / 2; // left-top corner of eye's apple bounds

		int xc = x + eyeBounds.width / 2 + 1;
		int yc = y + eyeBounds.height / 2 + 1; // eye's center
		int W = eyeBounds.width - 2 * hor_ext / 3;
		int H = eyeBounds.height - 2 * vert_ext / 2; // width,height of eye's apple bounds

		// drawing eyelashes: thickness=5, startAngle=30, endAngle=150, step=10
		for (int i = 30; i <= 150; i += 10) {
			g.drawArc(x0 - 2, y0 - 2, W + 4, H + 4, i, 5);
			g.drawArc(x0 - 1, y0 - 1, W + 2, H + 2, i, 5);
		}
		// eye apple bounds
		g.drawOval(x0, y0, W, H);
		// eye iris
		g.setColor(eyeIrisColor);
		g.fillOval(xc - i_radius, yc - i_radius, 2 * i_radius, 2 * i_radius);
		// eye center
		g.setColor(Color.darkGray);
		g.fillOval(xc - z_radius, yc - z_radius, 2 * z_radius, 2 * z_radius);
		g.setColor(c_orig);
	}

	//public void draw(Graphics g) { drawInPos(g); }
	public void draw(Graphics g) {
		// position_set=true;
		drawInPos(eyePosition, g);
		// position_set=false;
	}

	public void paint(Graphics g) {
		draw(g);
	}

	public int getXPosition() {
		return eyePosition.x;
	}

	public int getYPosition() {
		return eyePosition.y;
	}

	public Point getPosition() {
		return new Point(eyePosition.x, eyePosition.y);
	}

	public void setPosition(int x, int y) {
		eyePosition.x = x;
		eyePosition.y = y;
	}

	public void setPosition(Point pos) {
		this.setPosition(pos.x, pos.y);
	}

	public void setMin(int minX, int minY) {
		this.minX = minX;
		this.minY = minY;
	}

	public void setMax(int maxX, int maxY) {
		this.maxX = maxX;
		this.maxY = maxY;
	}

	public boolean isIn(Point location) {
		return eyeBounds.contains(location);
	}
}
