package spade.analysis.vis3d;

// awt
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Vector;

/*
* This is graphical user interface class for visual specification of viewpoint
* in horisontal flat by mouse drag/click and notification of interested
* listeners (normally MapCanvas3D) in changes of x,y coords of the viewpoint
*/

public class FlatControl extends Canvas implements MouseMotionListener, MouseListener {//, EyePositionListener {
	/*
	* Margin between edge of Canvas and controls
	*/
	public final static int margin = 10;
	/*
	* ratio between sizes of map and control
	*/
	public final static int r = 5;
	/*
	* area of FlatControl where mouse operations are possible
	*/
	private Rectangle active_area;

	/*
	* area on FlatControl covered by space-time-cube
	*/
	private Rectangle cube_area;
	/*
	* former mouse and eye pointers positions
	*/
	private Point oldMousePos = null, oldEyePos = null;
	/*
	* mouse pointer position of previous mouse event
	*/
	private Point prevMousePos = null;
	/*
	* eye primitive for representation of user's viewpoint on FlatControl
	*/
	EyePointer eye = null;
	/*
	* flag indicates that mouse pointer is located over eye poiner
	*/
	boolean mouseInEye = false;
	/*
	* flag indicates that each modification of eye pointer position in FlatControl
	* will be reflected in redrawing 3D display.
	*/
	boolean allowDynamicUpdate = false;
	/*
	* Vector of listeners that are interested in changing of eye pointer position
	*/
	Vector epList = null;

	int init_x = 0, init_y = 0; // initial position of eye (internal)
	int scrObjW, scrObjH; // size of "map" in screen coordinates

	int center; // coordinate of the center
	int ssize; // side of square in pixels
	int max_pos; // maximal position in pixels
	float mapMinX, mapMinY, mapMaxX, mapMaxY; // map extent
	float mapMinZ, mapMaxZ;
	float dx, dy; // map sizes
	float d; // real flat control  size
	float ratio; // ratio between screen and real coords
	float minPosX, minPosY, maxPosX, maxPosY;
	protected EyePosition eyePos = null;

	/*
	* Constructs FlatControl using current viewpoint position and
	* limits of represented territory in real world coordinates
	*/

	public FlatControl(EyePosition pos, float minX, float maxX, float minY, float maxY, float minZ, float maxZ) {
		super();
		setSize(new Dimension(80, 80));
		eyePos = pos;
		mapMinX = minX;
		mapMinY = minY;
		mapMinZ = minZ;
		mapMaxX = maxX;
		mapMaxY = maxY;
		mapMaxZ = maxZ;

		dx = maxX - minX;
		dy = maxY - minY;

		d = (dy < dx) ? dx : dy;
		d *= r; // dimension of FlatControl in real coords
		calculateDrawingParams();
		minPosX = (minX + maxX) / 2 - d / 2;
		maxPosX = (minX + maxX) / 2 + d / 2;
		minPosY = (minY + maxY) / 2 - d / 2;
		maxPosY = (minY + maxY) / 2 + d / 2;
		addMouseListener(this);
		addMouseMotionListener(this);
	}

	public void rebuild(EyePosition pos, float minX, float maxX, float minY, float maxY, float minZ, float maxZ) {
		if (pos != null) {
			eyePos = pos;
		}
		mapMinX = minX;
		mapMinY = minY;
		mapMinZ = minZ;
		mapMaxX = maxX;
		mapMaxY = maxY;
		mapMaxZ = maxZ;
		dx = maxX - minX;
		dy = maxY - minY;

		d = (dy < dx) ? dx : dy;
		d *= r;
		calculateDrawingParams();
		minPosX = (minX + maxX) / 2 - d / 2;
		maxPosX = (minX + maxX) / 2 + d / 2;
		minPosY = (minY + maxY) / 2 - d / 2;
		maxPosY = (minY + maxY) / 2 + d / 2;

		calculateDrawingParams();
		eyePos.setX(minPosX + ratio * (eyePos.getScreenX() - margin));
		eyePos.setY(minPosY + ratio * (max_pos - eyePos.getScreenY()));
		notifyPositionChanged();
		repaint();
	}

	public float calculateDrawingParams() {
		Dimension size = getSize();
		int a = (size.width < size.height) ? size.width : size.height;
		ssize = a - 2 * margin;
		center = a / 2;
		max_pos = a - margin;
		//ratio=d/(ssize-2*eye.hor_ext);
		ratio = d / ssize;
		scrObjW = Math.round(dx / ratio);
		scrObjH = Math.round(dy / ratio);
		if (active_area == null) {
			active_area = new Rectangle(margin, margin, ssize, ssize);
		} else {
			active_area.setSize(ssize, ssize);
		}
		if (cube_area == null) {
			cube_area = new Rectangle(center - scrObjW / 2 - 1, center - scrObjH / 2 - 1, scrObjW + 2, scrObjH + 2);
		} else {
			cube_area.setLocation(center - scrObjW / 2 - 1, center - scrObjH / 2 - 1);
			cube_area.setSize(scrObjW + 2, scrObjH + 2);
		}
		if (eye == null) {
			eye = new EyePointer(0, 0);
		}
		eye.setMin(margin, margin);
		eye.setMax(max_pos, max_pos);
		Point p = new Point(margin + Math.round((eyePos.getX() - minPosX) / ratio), max_pos - Math.round((eyePos.getY() - minPosY) / ratio));
//    int newEyeScrX=margin+Math.round((eyePos.getX()-minPosX)/ratio);
//    int newEyeScrY=max_pos-Math.round((eyePos.getY()-minPosY)/ratio);
		//System.out.println("eyeX="+newEyeScrX+" eyeY="+newEyeScrY);
//    eye.setPosition(newEyeScrX,newEyeScrY);

		eye.setPosition(p);
		if (eyePos.getScreenX() < 0) {
			eyePos.setScreenX(p.x);
		}
		if (eyePos.getScreenY() < 0) {
			eyePos.setScreenY(p.y);
		}
		return ratio;
	}

	@Override
	public void paint(Graphics g) {
		calculateDrawingParams();
		if ((ssize + 2 * margin) < getPreferredSize().width)
			return;
		g.drawRect(margin, margin, ssize, ssize); // X,Z-flat
		//g.drawRect(center-scrObjW/2,center-scrObjH/2,scrObjW,scrObjH); // "map"
		g.drawRect(cube_area.x, cube_area.y, cube_area.width, cube_area.height);

		g.drawLine(center - scrObjW / 2, margin, center - scrObjW / 2, max_pos);
		g.drawLine(center + scrObjW / 2, margin, center + scrObjW / 2, max_pos);

		g.drawLine(margin, center - scrObjH / 2, max_pos, center - scrObjH / 2);
		g.drawLine(margin, center + scrObjH / 2, max_pos, center + scrObjH / 2);

		for (int i = center - scrObjW / 2; i < center + scrObjW / 2; i += 3) {
			g.drawLine(i, center - scrObjH / 2, i, center + scrObjH / 2);
		}
		for (int i = center - scrObjH / 2; i < center + scrObjH / 2; i += 3) {
			g.drawLine(center - scrObjW / 2, i, center + scrObjW / 2, i);
		}

		g.drawLine(center, center, eye.getXPosition(), eye.getYPosition());
		eye.paint(g);
	}

	public void reset() {
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(80, 80);
	}

	@Override
	public Dimension preferredSize() {
		return new Dimension(80, 80);
	}

	public float getRatio() {
		return ratio;
	}

	@Override
	public void setSize(Dimension d) {
		// Set size to smallest dimension
		if (d.width < d.height) {
			super.setSize(d.width, d.width);
		} else {
			super.setSize(d.height, d.height);
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		Point p = e.getPoint();
		Cursor cur = getCursor();
		if (!active_area.contains(p)) {
			if (cur == null || cur.getType() != Cursor.DEFAULT_CURSOR) {
				setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			}
			return;
		}
		if (eye.isIn(p)) {
			if (cur == null || cur.getType() != Cursor.MOVE_CURSOR) {
				setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
			}
		} else if (cur.getType() != Cursor.CROSSHAIR_CURSOR) {
			setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		Point p = e.getPoint();
		// checking available bounds
		if (isOverTop(p)) {
			p.x = prevMousePos.x;
			p.y = prevMousePos.y;
		} else {
			if (p.x > max_pos) {
				p.x = max_pos;
			}
			if (p.y > max_pos) {
				p.y = max_pos;
			}
			if (p.x < margin) {
				p.x = margin;
			}
			if (p.y < margin) {
				p.y = margin;
			}
		}

		eye.setPosition(p);
		eyePos.setScreenX(p.x);
		eyePos.setScreenY(p.y);
		eyePos.setX(minPosX + ratio * (p.x - margin));
		eyePos.setY(minPosY + ratio * (max_pos - p.y));
		notifyPositionChanged();
		mouseInEye = false;
		oldMousePos = null;
		oldEyePos = p;
		repaint();
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (eye.isIn(e.getPoint())) {
			mouseInEye = true;
			oldMousePos = eye.getPosition();
			setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if (!mouseInEye)
			return;
		Point pos = e.getPoint();
		if (prevMousePos == null) {
			prevMousePos = pos;
		}
		if (isOverTop(pos)) {
			pos.x = prevMousePos.x;
			pos.y = prevMousePos.y;
		} else {
			// draw connecting line only inside appropriate bounds
			if (pos.x > max_pos) {
				pos.x = max_pos;
			}
			if (pos.y > max_pos) {
				pos.y = max_pos;
			}
			if (pos.x < margin) {
				pos.x = margin;
			}
			if (pos.y < margin) {
				pos.y = margin;
			}
		}
		if (oldMousePos == null) {
			oldMousePos = pos;
		}
		prevMousePos = pos;
		Graphics g = getGraphics();
		g.setColor(Color.magenta);
		g.setXORMode(getBackground());
		eye.draw(g);
		eye.drawInPos(pos.x, pos.y, g);

		g.drawLine(center, center, oldMousePos.x, oldMousePos.y);
		g.drawLine(center, center, pos.x, pos.y);
		//g.drawLine(center,center,eye.getXPosition(),eye.getYPosition());
		g.setPaintMode();
		oldMousePos = pos;
		//oldMousePos=eye.getPosition();
		g.dispose();
		if (allowDynamicUpdate) {
			eye.setPosition(pos);
			eyePos.setScreenX(pos.x);
			eyePos.setScreenY(pos.y);
			eyePos.setX(minPosX + ratio * (pos.x - margin));
			eyePos.setY(minPosY + ratio * (max_pos - pos.y));
			notifyPositionChanged();
		}
	}

	protected boolean isOverTop(Point pos) {
		return cube_area.contains(pos) & eyePos.getZ() <= mapMaxZ;
	}

	public Point getViewPoint() {
		return new Point();
	}

	public void addEyePositionListener(EyePositionListener epl) {
		if (epList == null) {
			epList = new Vector(2, 2);
		} else {
			for (int i = 0; i < epList.size(); i++)
				if (epList.elementAt(i).equals(epl)) {
					System.out.println("WARNING: Listener " + epl + " was already added!");
					return;
				}
		}
		epList.addElement(epl);
	}

	public void removeEyePositionListener(EyePositionListener epl) {
		if (epl == null)
			return;
		else {
			for (int i = 0; i < epList.size(); i++)
				if (epList.elementAt(i).equals(epl)) {
					epList.removeElementAt(i);
					return;
				}
		}
	}

	public void notifyPositionChanged() {
		EyePositionListener epl;
		//System.out.println("FlatControl:: epX="+eyePos.getX()+" epY="+eyePos.getY());
		for (int i = 0; i < epList.size(); i++) {
			epl = (EyePositionListener) (epList.elementAt(i));
			epl.eyePositionChanged(eyePos);
		}
	}

	public boolean getAllowDynamicUpdate() {
		return allowDynamicUpdate;
	}

	public void setAllowDynamicUpdate(boolean allow) {
		if (allowDynamicUpdate != allow) {
			allowDynamicUpdate = !allowDynamicUpdate;
		}
	}
}
