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

public class HeightControl extends Canvas implements MouseMotionListener, MouseListener, EyePositionListener {

	public final static int margin = 10; // Margin between edge of Canvas and controls
	public final static float r = 2.0f; // ratio between sizes of map and control

	private int ssize; // Side size of square
	private int center; // Coordinate of center
	private int max_pos; // Maximum x,y coordinates
	private Point oldMousePos = null;
	private EyePointer eye = null;

	private EyePosition ep = null;
	Vector epList = null;
	int distance;

	boolean mouseInEye = false, mouseOnAxis = false;
	boolean allowDynamicUpdate = false;
	boolean inverted = false;

	int init_x;
	int init_y; // initial position of eye (in canvas)
	int obj_x;
	int scrObjW, scrObjH; // Width and Height of object
	int axis_xpos; // screen position of "height"-axis
	int axis_ypos_top, axis_ypos_bottom; // ... top and bottom y-positions
	Rectangle axis_area; // rectangular height-axis area to react on mouse movements

	float mapMinZ, mapMaxZ, mapMinX, mapMaxX, mapMinY, mapMaxY;
	float dz, dx, dy; // "map" dimensions
	float d; // Z dimension if real coords
	float minPosZ, maxPosZ; // range for height
	float zratio, ratio;

	public HeightControl(EyePosition eyePos, float minX, float maxX, float minY, float maxY, float minZ, float maxZ) {
		super();
		ep = eyePos;
		mapMinX = minX;
		mapMinY = minY;
		mapMinZ = minZ;
		mapMaxX = maxX;
		mapMaxY = maxY;
		mapMaxZ = maxZ;

		dx = maxX - minX;
		dy = maxY - minY;
		dz = maxZ - minZ; // Z-Attribute values vary in this range
		d = dz * r; // increase this range in r times (to vary ViewPoint in it)
		minPosZ = minZ;
		maxPosZ = minZ + d;

		addMouseListener(this);
		addMouseMotionListener(this);
	}

	public void rebuild(EyePosition eyePos, float minX, float maxX, float minY, float maxY, float minZ, float maxZ) {
		if (eyePos != null) {
			ep = eyePos;
		}
		mapMinX = minX;
		mapMinY = minY;
		mapMinZ = minZ;
		mapMaxX = maxX;
		mapMaxY = maxY;
		mapMaxZ = maxZ;

		dx = maxX - minX;
		dy = maxY - minY;
		dz = maxZ - minZ;
		d = dz * r;
		minPosZ = minZ;
		maxPosZ = minZ + d;

		calculateDrawingParams();
		ep.setZ(getAbsEyePos(ep.getScreenZ()));
		ep.setScreenZ(axis_ypos_bottom - ep.getScreenZ());
		notifyPositionChanged();
		repaint();
	}

	protected int getScreenEyePos(float absPos) {
		if (!inverted)
			return axis_ypos_bottom - Math.round((absPos - minPosZ) / zratio);
		else
			return axis_ypos_bottom - scrObjH - Math.round((minPosZ - absPos) / zratio);
	}

	protected float getAbsEyePos(int scrPos) {
		if (!inverted)
			return minPosZ + (axis_ypos_bottom - scrPos) * zratio;
		else
			return minPosZ - (axis_ypos_bottom - scrObjH - scrPos) * zratio;
	}

	private void calculateDrawingParams() {
		Dimension size = getSize();
		int a = (size.width < size.height) ? size.width : size.height;
		ssize = a - margin * 2;
		center = margin + ssize / 2;
		max_pos = margin + ssize;
		axis_xpos = margin - margin / 4 + EyePointer.hor_ext;
		axis_ypos_top = margin + EyePointer.vert_ext;
		axis_ypos_bottom = max_pos - EyePointer.vert_ext;
		if (axis_area == null) {
			axis_area = new Rectangle(axis_xpos - EyePointer.hor_ext, axis_ypos_top, axis_xpos + EyePointer.hor_ext, axis_ypos_bottom - axis_ypos_top);
		} else {
			axis_area.setSize(axis_xpos + EyePointer.hor_ext, axis_ypos_bottom - axis_ypos_top);
		}
		zratio = d / (axis_ypos_bottom - axis_ypos_top);
		ratio = ((dy < dx) ? dx : dy) * FlatControl.r / ssize;
		scrObjW = Math.round(dx / ratio);
		scrObjH = Math.round(dz / zratio);
		int xs = ep.getScreenX();
		int ys = ep.getScreenY();
		distance = 2 * Math.max(Math.abs(xs - center), Math.abs(ys - center));

		obj_x = distance + axis_xpos - scrObjW;
		if (obj_x < axis_xpos) {
			obj_x = axis_xpos;
		}
		if (obj_x > max_pos - scrObjW) {
			obj_x = max_pos - scrObjW;
		}
		if (eye == null) {
			eye = new EyePointer(0, 0);
		}
		eye.setPosition(axis_xpos, getScreenEyePos(ep.getZ()));
		if (ep.getScreenZ() < 0) {
			ep.setScreenZ(getScreenEyePos(ep.getZ()));
		}
	}

	public float getRatio() {
		return zratio;
	}

	@Override
	public void paint(Graphics g) {
		calculateDrawingParams();
		if ((ssize + 2 * margin) < getPreferredSize().width)
			return;
		g.drawRect(margin, margin, ssize, ssize); // Y,X-flat
		// Height (Y) axis
		g.drawLine(axis_xpos, axis_ypos_top, axis_xpos, axis_ypos_bottom);
		// drawing short marks on axis and rails for "object"
		for (int i = axis_ypos_top; i < axis_ypos_bottom; i += scrObjH / 2) {
			g.drawLine(axis_xpos - margin / 4, i, axis_xpos + margin / 4, i);
		}
		g.drawLine(axis_xpos - margin / 4, axis_ypos_bottom, max_pos, axis_ypos_bottom);
		g.drawRect(obj_x, axis_ypos_bottom - scrObjH, scrObjW, scrObjH); // rectangular  "object"
		for (int i = axis_ypos_bottom - scrObjH; i < axis_ypos_bottom; i += 3) {
			g.drawLine(obj_x, i, obj_x + scrObjW, i); // hatching the object
		}
		// bottom of axis
		g.drawLine(axis_xpos - margin / 4, axis_ypos_bottom, axis_xpos + margin / 4, axis_ypos_bottom);
		eye.paint(g);
		// drawing the line from object's center to the eye
		g.drawLine(obj_x + scrObjW / 2, axis_ypos_bottom - scrObjH / 2, eye.getXPosition(), eye.getYPosition());
	}

	public void reset() {
		if (oldMousePos != null) {
			oldMousePos.setLocation(init_x, init_y);
		} else {
			oldMousePos = new Point(init_x, init_y);
		}
		eye.setPosition(oldMousePos);
		repaint();
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(80, 80);
	}

	@Override
	public Dimension preferredSize() {
		return new Dimension(80, 80);
	}

	@Override
	public void setSize(Dimension d) {
		// Set size to smallest dimension
		if (d.width < d.height) {
			super.setSize(d.width, d.width);
		} else {
			super.setSize(d.height, d.height);
		}
		calculateDrawingParams();
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
		if (p == null)
			return;
		if (eye != null && eye.isIn(p)) {
			setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
		} else if (axis_area != null && axis_area.contains(p)) {
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			mouseOnAxis = true;
		} else {
			setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			mouseOnAxis = false;
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (!mouseInEye && !mouseOnAxis)
			return;
		Point pos = e.getPoint();
		if (pos.y > axis_ypos_bottom) {
			pos.y = axis_ypos_bottom;//-eye.vert_ext;
		}
		if (pos.y < axis_ypos_top) {
			pos.y = axis_ypos_top;//+eye.vert_ext;
		}
		if (pos.y > axis_ypos_bottom - scrObjH && obj_x == axis_xpos) {
			pos.y = axis_ypos_bottom - scrObjH;
		}

		eye.setPosition(axis_xpos, pos.y);
		if (mouseInEye) {
			mouseInEye = !mouseInEye;
		}
		if (mouseOnAxis) {
			mouseOnAxis = !mouseOnAxis;
		}
		oldMousePos = null;
		ep.setZ(getAbsEyePos(pos.y));
		ep.setScreenZ(axis_ypos_bottom - pos.y);
		notifyPositionChanged();
		repaint();
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (eye.isIn(e.getPoint())) {
			mouseInEye = true;
			oldMousePos = eye.getPosition();
		}
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if (!mouseInEye)
			return;
		Point pos = e.getPoint();
		if (pos.x > max_pos) {
			pos.x = max_pos;
		}
		if (pos.x < margin) {
			pos.x = margin;
		}
		if (pos.y > axis_ypos_bottom) {
			pos.y = axis_ypos_bottom;
		}
		if (pos.y < axis_ypos_top) {
			pos.y = axis_ypos_top;
		}
		if (pos.y > axis_ypos_bottom - scrObjH && obj_x == axis_xpos) {
			pos.y = axis_ypos_bottom - scrObjH;
		}
		if (oldMousePos == null) {
			oldMousePos = pos;
		}
		Graphics g = getGraphics();
		g.setColor(Color.magenta);
		g.setXORMode(getBackground());
		// redraw "old" postion
		eye.draw(g);
		g.drawLine(obj_x + scrObjW / 2, axis_ypos_bottom - scrObjH / 2, eye.getXPosition(), eye.getYPosition());
		// draw current position
		eye.drawInPos(axis_xpos, pos.y, g);
		g.drawLine(obj_x + scrObjW / 2, axis_ypos_bottom - scrObjH / 2, axis_xpos, pos.y);
		g.setPaintMode();
		oldMousePos = pos;
		g.dispose();
		if (allowDynamicUpdate) {
			ep.setZ(getAbsEyePos(pos.y));
			ep.setScreenZ(axis_ypos_bottom - pos.y);
			notifyPositionChanged();
		}
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
		for (int i = 0; i < epList.size(); i++)
			if (epList.elementAt(i).equals(epl)) {
				epList.removeElementAt(i);
			}
	}

	public void notifyPositionChanged() {
		EyePositionListener epl;
		for (int i = 0; i < epList.size(); i++) {
			epl = (EyePositionListener) (epList.elementAt(i));
			epl.eyePositionChanged(ep);
		}
	}

	@Override
	public void eyePositionChanged(EyePosition e) {
		ep = e;
		repaint();
	}

	public boolean getAllowDynamicUpdate() {
		return allowDynamicUpdate;
	}

	public void setAllowDynamicUpdate(boolean allow) {
		if (allowDynamicUpdate != allow) {
			allowDynamicUpdate = !allowDynamicUpdate;
		}
	}

	public void setInverted(boolean flag) {
		if (inverted != flag) {
			inverted = !inverted;
		}
		ep.setZ(dz - ep.getZ());
		notifyPositionChanged();
		repaint();
	}
}
