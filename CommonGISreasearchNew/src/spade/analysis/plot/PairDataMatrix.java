package spade.analysis.plot;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Vector;

import spade.analysis.calc.PairDataContainer;
import spade.lib.basicwin.Drawable;
import spade.lib.basicwin.Metrics;

/**
 * <p>�berschrift: </p>
 * <p>Beschreibung: </p>
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: Fraunhofer AIS.SPADE</p>
 * @author unbekannt
 * @version 1.0
 */
public abstract class PairDataMatrix implements Drawable {

	abstract class Cell {

		PairDataMatrixItem item1;
		PairDataMatrixItem item2;
		float value;
		//upper left corner of the cell
		int x;
		int y;

		void updateValue() {
			value = dataSource.getValue(item1.indexInContainer, item2.indexInContainer);
			setup();
		}

		boolean contains(int px, int py) {
			return (px >= x && px <= x + cellWidth && py >= y && py <= y + cellHeight);
		}

		Cell(float v, int x, int y, PairDataMatrixItem d1, PairDataMatrixItem d2) {
			value = v;
			this.x = x;
			this.y = y;
			item1 = d1;
			item2 = d2;
		}

		abstract void draw(Graphics g);

		abstract void setup();
	}

	protected int cellWidth;
	protected int cellHeight;
	protected int lineWidth;
	protected float maxValue;
	protected float minValue;

	protected int leftMargin;
	protected int rightMargin;
	protected int topMargin;
	protected int bottomMargin;

	protected Color bkgColor;
	protected Color frgColor;
	protected Color cellBkgColor;

	protected boolean destroyed;
	protected boolean visible;
	protected Canvas canvas;
	protected Rectangle bounds;

	protected Vector cells;
	protected Vector items;
	protected PairDataContainer dataSource;

	public PairDataMatrix(PairDataContainer source) {
		dataSource = source;
		cells = new Vector();
		items = new Vector();
		topMargin = bottomMargin = rightMargin = leftMargin = 5;
		cellWidth = cellHeight = Metrics.mm() * 20;
		maxValue = 1;
		minValue = -1;
		frgColor = Color.black;
		bkgColor = Color.lightGray;
		cellBkgColor = Color.gray;
		visible = true;
	}

	public void setVisible(boolean vis) {
		visible = vis;
	}

	public void updateValues() {
		for (int i = 0; i < cells.size(); i++) {
			((Cell) cells.elementAt(i)).updateValue();
		}
	}

	public void setCellWidth(int w) {
		cellWidth = w;
	}

	public void setCellHeight(int h) {
		cellHeight = h;
	}

	public void setBackgroundColor(Color c) {
		bkgColor = c;
	}

	public void setForegroundColor(Color c) {
		frgColor = c;
	}

	public void setCellBackgroundColor(Color c) {
		cellBkgColor = c;
	}

	public void addItem(PairDataMatrixItem item) {
		items.addElement(item);
	}

	protected abstract void drawLines(Graphics g);

	protected abstract void drawDescriptions(Graphics g);

	protected void drawBackground(Graphics g) {
		if (bounds == null)
			return;
		g.setColor(bkgColor);
		g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
	}

// methods of Drawable
	@Override
	public void setCanvas(Canvas c) {
		canvas = c;
	}

	@Override
	public abstract Dimension getPreferredSize();

	@Override
	public void setBounds(Rectangle b) {
		bounds = b;
		for (int i = 0; i < cells.size(); i++) {
			((Cell) cells.elementAt(i)).setup();
		}
	}

	@Override
	public Rectangle getBounds() {
		return bounds;
	}

	/** Draws the Object.
	 * Invokes drawBackground, drawLines, drawDescriptions and cell.draw of every cell in this order.
	 *
	 * @param g Graphics-object, on which the PairDataMatrix will be painted
	 */
	@Override
	public void draw(Graphics g) {
		if (!visible)
			return;
		drawBackground(g);
		drawLines(g);
		drawDescriptions(g);
		for (int i = 0; i < cells.size(); i++) {
			((Cell) cells.elementAt(i)).draw(g);
		}
	}

	@Override
	public void destroy() {
		destroyed = true;
	}

	@Override
	public boolean isDestroyed() {
		return destroyed;
	}
// end methods of Drawable

}