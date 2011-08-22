package spade.analysis.plot;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.lib.basicwin.Drawable;
import spade.lib.basicwin.Metrics;
import spade.lib.lang.Language;
import spade.vis.database.AttributeDataPortion;

/**
* Shows names of attributes represented on a parallel coordinates plot and
* allows the suer to change the order of the attributes (and, respectively,
* the order of the axes in the plot).
*/
public class FNReorder implements Drawable, MouseListener, MouseMotionListener {

	static ResourceBundle res = Language.getTextResource("spade.analysis.plot.Res");
	/**
	* The canvas in which this object should draw itself
	*/
	protected Canvas canvas = null;
	/**
	* The boundaries in which the object should fit itself
	*/
	protected Rectangle bounds = null;
	/**
	* The table with the attributes to be shown on the plot
	*/
	protected AttributeDataPortion dataTable = null;
	/**
	* Indicates "destroyed" state. Initially is false.
	*/
	protected boolean destroyed = false;

	protected int fn[] = null; // field numbers
	protected float groupBreak = Float.NaN; // break between values

	public float getGroupBreak() {
		return groupBreak;
	}

	protected Vector alv = null;

	public void addActionListener(ActionListener al) {
		if (alv == null) {
			alv = new Vector(5, 2);
		}
		if (!alv.contains(al)) {
			alv.addElement(al);
		}
	}

	//protected boolean isHorizontal=true;

	protected int yy[] = null, fh = -1;

	public FNReorder(AttributeDataPortion tbl, Canvas c, int fn[], int Y0, int DY) {
		this(tbl, c, fn, (fn[fn.length - 1] == -1) ? fn.length - 1.5f : Float.NaN, Y0, DY);
	}

	protected int Y0 = -1, DY = -1;

	public FNReorder(AttributeDataPortion tbl, Canvas c, int fn[], float groupBreak, int Y0, int DY) {
		dataTable = tbl;
		assignFn(fn);
		this.groupBreak = groupBreak;
		this.Y0 = Y0;
		this.DY = DY;
		setCanvas(c);
		yy = new int[fn.length];
	}

	/*
	public void setup (int fn[], float groupBreak) {
	  this.fn=fn; this.groupBreak=groupBreak;
	  yy=new int[fn.length];
	}

	public void setup (int fn[]) {
	  setup(fn,(fn[fn.length-1]==-1) ? fn.length-1.5f : Float.NaN);
	}
	  */

	@Override
	public void setCanvas(Canvas c) {
		canvas = c;
		if (canvas != null) {
			canvas.addMouseListener(this);
			canvas.addMouseMotionListener(this);
		}
	}

	public void setGroupBreak(float groupBreak) {
		this.groupBreak = groupBreak;
		Graphics g = canvas.getGraphics();
		draw(g);
		g.dispose();
	}

	private boolean FNfinishedByDummy = false;

	protected void assignFn(int fn[]) {
		if (fn[fn.length - 1] == -1) {
			this.fn = new int[fn.length - 1];
			for (int i = 0; i < fn.length - 1; i++) {
				this.fn[i] = fn[i];
			}
			FNfinishedByDummy = true;
		} else {
			this.fn = fn;
			FNfinishedByDummy = false;
		}
	}

	public void setFn(int fn[]) {
		assignFn(fn);
		yy = new int[fn.length];
		Graphics g = canvas.getGraphics();
		draw(g);
		g.dispose();
	}

	public int[] getFn() {
		if (FNfinishedByDummy) {
			int newFn[] = new int[fn.length + 1];
			for (int i = 0; i < fn.length; i++) {
				newFn[i] = fn[i];
			}
			newFn[fn.length] = -1;
			return newFn;
		} else
			return fn;
	}

	public int nRestOf = -1;

	protected void drawText(Graphics g, int n) {
		int k = fn[n];
		String str = (k >= 0) ? dataTable.getAttributeName(k) : "SUM";
		if (k == nRestOf) {
			str += res.getString("_the_rest_of_");
		}
		g.drawString(str, 5, yy[n]);
	}

	@Override
	public void draw(Graphics g) {
		if (g == null)
			return;
		g.setColor(Color.white);
		g.fillRect(0, 0, canvas.getSize().width, canvas.getSize().height);
		g.setColor(Color.black);
		FontMetrics fm = g.getFontMetrics();
		fh = fm.getHeight();
		int asc = fm.getAscent();
		int dy = DY;
		if (dy <= 0) {
			dy = (canvas.getSize().height - 3 * fh - asc) / (fn.length - 1);
		}
		if (Y0 == -1) {
			Y0 = fh;
		}
		if (!Float.isNaN(groupBreak)) {
			int yyy = Math.round(Y0 + dy * groupBreak + asc);
			g.drawLine(0, yyy, canvas.getSize().width, yyy);
		}
		for (int i = 0; i < fn.length; i++) {
			yy[i] = Y0 + dy * i + asc;
			drawText(g, i);
			//g.drawString(dataTable.getAttributeName(fn[i]),5,yy[i]);
		}
		g.setColor(Color.magenta);
		g.drawString(res.getString("drag_to_reorder"), 3, canvas.getSize().height - fh + asc);
	}

	public int mapX(float v) {
		return 0;
	}

	public int mapY(float v) {
		return 0;
	}

	public float absX(int x) {
		return 0;
	}

	public float absY(int y) {
		return 0;
	}

	protected void drawArrow(int x1, int y1, int x2, int y2, boolean OK) {
		Graphics g = canvas.getGraphics();
		g.setColor((OK) ? Color.green : Color.red);
		g.setXORMode(Color.lightGray);
		g.drawLine(x1, y1, x2, y2);
		g.setPaintMode();
		g.dispose();
	}

	protected int dragX1 = -1, dragY1 = -1, dragX2 = dragX1, dragY2 = dragY1, dragged = -1, draggedTo = -1;
	protected boolean dragging = false;

	public int getDragged() {
		return dragged;
	}

	public int getDraggedTo() {
		return draggedTo;
	}

	protected void drawTarget(boolean OK) {
		if (draggedTo == -1)
			return;
		int y = 0;
		if (draggedTo < dragged) {
			y = yy[draggedTo] - fh;
		}
		if (draggedTo > dragged) {
			y = yy[draggedTo] + fh / 2;
		}
		Graphics g = canvas.getGraphics();
		g.setColor((OK) ? Color.green : Color.red);
		g.setXORMode(Color.lightGray);
		if (OK) {
			g.drawLine(5, y, 15, y);
			g.drawLine(10, y - 5, 15, y);
			g.drawLine(10, y + 5, 15, y);
		} else {
			g.drawLine(5, y - 5, 15, y + 5);
			g.drawLine(5, y + 5, 15, y - 5);
		}
		g.setPaintMode();
		g.dispose();
	}

	protected boolean isPointInPlotArea(int x, int y) {
		if (bounds == null)
			return false;
		return x >= bounds.x && x <= bounds.x + bounds.width && y >= bounds.y && y <= bounds.y + bounds.height;
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if (canvas == null)
			return;
		int x = e.getX(), y = e.getY();
		if (!dragging && !isPointInPlotArea(dragX1, dragY1))
			return;
		dragging = dragging || Math.abs(x - dragX1) > 5 || Math.abs(y - dragY1) > 5;
		if (!dragging)
			return;
		// hiding old arrow
		if (x == dragX2 && y == dragY2)
			return;
		drawArrow(dragX1, dragY1, dragX2, dragY2, Float.isNaN(groupBreak) || (draggedTo - groupBreak) * (dragged - groupBreak) > 0);
		drawTarget(Float.isNaN(groupBreak) || (draggedTo - groupBreak) * (dragged - groupBreak) > 0);
		// finding the target
		draggedTo = -1;
		if (y < dragY1 && dragged > 0) { // dragging up
			for (int i = 0; i < dragged; i++)
				if (y < yy[i]) {
					draggedTo = i;
					break;
				}
		} else if (y > dragY1 && dragged < fn.length - 1) { // dragging down
			for (int i = fn.length - 1; i > dragged; i--)
				if (y > yy[i] - fh) {
					draggedTo = i;
					break;
				}
		}
		//System.out.println("* dragged="+dragged+", To="+draggedTo);
		// drawing new arrow
		dragX2 = x;
		dragY2 = y;
		drawArrow(dragX1, dragY1, dragX2, dragY2, Float.isNaN(groupBreak) || (draggedTo - groupBreak) * (dragged - groupBreak) > 0);
		drawTarget(Float.isNaN(groupBreak) || (draggedTo - groupBreak) * (dragged - groupBreak) > 0);
	}

	protected Cursor savedCursor = null;

	@Override
	public void mousePressed(MouseEvent e) {
		int x = e.getX(), y = e.getY();
		dragX1 = dragX2 = x;
		dragY1 = dragY2 = y;
		// finding an item to be dragged
		dragged = -1;
		int dy = (yy[1] - yy[0] - fh) / 2;
		for (int i = 0; dragged == -1 && i < fn.length; i++)
			if ((i == 0 && y <= yy[0] + dy) || (i > 0 && i < fn.length - 1 && y > yy[i - 1] + dy && y <= yy[i] + dy) || (i == fn.length - 1 && y > yy[i - 1] + dy)) {
				dragged = i;
			}
		draggedTo = -1;
		//System.out.println(" dragged="+dragged+", y="+y+", dy="+dy+", yy[dragged]="+yy[dragged]);
		Graphics g = canvas.getGraphics();
		g.setColor(Color.red);
		drawText(g, dragged);
		g.setColor(Color.magenta);
		g.setXORMode(Color.lightGray);
		g.fillOval(x - 2, y - 2, 5, 5);
		g.setPaintMode();
		g.dispose();
		savedCursor = canvas.getCursor();
		canvas.setCursor(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (dragged >= 0 && draggedTo >= 0) {
			if (Float.isNaN(groupBreak) || (draggedTo - groupBreak) * (dragged - groupBreak) > 0) {
				int n = fn[dragged];
				if (draggedTo < dragged) {
					for (int i = dragged; i > draggedTo; i--) {
						fn[i] = fn[i - 1];
					}
					fn[draggedTo] = n;
				} else {
					for (int i = dragged; i < draggedTo; i++) {
						fn[i] = fn[i + 1];
					}
					fn[draggedTo] = n;
				}
				if (alv != null) {
					for (int i = 0; i < alv.size(); i++) {
						ActionListener al = (ActionListener) alv.elementAt(i);
						al.actionPerformed(new ActionEvent(this, 0, null));
					}
				}
			}
		}
		Graphics g = canvas.getGraphics();
		draw(g);
		g.dispose();
		dragging = false;
		dragX1 = dragY1 = dragX2 = dragY2 = dragged = -1;
		if (savedCursor != null) {
			canvas.setCursor(savedCursor);
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mouseMoved(MouseEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

//------------- implementation of the Drawable interface ------------

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(30 * Metrics.mm(), (DY > 0) ? DY * (fn.length + 1) : 60 * Metrics.mm());
	}

	@Override
	public void setBounds(Rectangle bounds) {
		this.bounds = bounds;
	}

	@Override
	public Rectangle getBounds() {
		return bounds;
	}

	/**
	* Makes necessary operations for destroying, e.g. unregisters from
	* listening highlighting and other events.
	*/
	@Override
	public void destroy() {
		if (canvas != null) {
			canvas.removeMouseListener(this);
			canvas.removeMouseMotionListener(this);
		}
		destroyed = true;
	}

	/**
	* Replies whether is destroyed or not
	*/
	@Override
	public boolean isDestroyed() {
		return destroyed;
	}
}
