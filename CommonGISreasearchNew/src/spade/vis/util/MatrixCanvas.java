package spade.vis.util;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Nov 9, 2009
 * Time: 12:02:02 PM
 * Displays a matrix with a specified number of columns where each cell
 * represents an item from a certain set of items of the given size where
 * some of the items may be selected. The cells corresponding to the selected
 * items are marked by black filling.
 */
public class MatrixCanvas extends Canvas {
	public static int defaultPixelSize = 8;

	public int pixelSize = defaultPixelSize;
	/**
	 * The number of items
	 */
	public int nItems = 0;
	/**
	 * The selection status of the items
	 */
	public boolean itemSelected[] = null;
	/**
	 * The number of columns in the matrix
	 */
	public int nColumns = 0;

	/**
	 * Sets the number of items
	 */
	public void setNItems(int nItems) {
		this.nItems = nItems;
	}

	/**
	 * Sets the selected or non-selected status of the item with the given index
	 */
	public void setItemSelectionStatus(int idx, boolean selected) {
		if (idx < 0 || idx >= nItems)
			return;
		if (itemSelected == null) {
			itemSelected = new boolean[nItems];
			for (int i = 0; i < itemSelected.length; i++) {
				itemSelected[i] = false;
			}
		}
		itemSelected[idx] = selected;
	}

	/**
	 * Sets the number of columns in the matrix
	 */
	public void setNColumns(int nColumns) {
		this.nColumns = nColumns;
	}

	/**
	 * Returns the number of columns in the matrix
	 */
	public int getNColumns() {
		return nColumns;
	}

	public void setPixelSize(int pixelSize) {
		this.pixelSize = pixelSize;
	}

	@Override
	public Dimension getPreferredSize() {
		if (nItems < 1 || nColumns < 1)
			return new Dimension(pixelSize * 5, pixelSize * 5);
		int nRows = (int) Math.ceil(1.0 * nItems / nColumns);
		return new Dimension(pixelSize * nColumns, pixelSize * nRows);
	}

	@Override
	public void paint(Graphics g) {
		if (itemSelected == null)
			return;
		draw(g, getSize());
	}

	public void draw(Graphics g, Dimension d) {
		if (g == null || d == null || d.width < 1 || d.height < 1)
			return;
		if (nColumns < 1) {
			nColumns = nItems;
			int nRows = 1;
			while (nColumns > nRows * 2) {
				++nColumns;
				nRows = (int) Math.ceil(1.0 * nItems / nColumns);
			}
		}
		int nRows = (int) Math.ceil(1.0 * nItems / nColumns);
		int cw = d.width / nColumns, ch = d.height / nRows;
		if (cw > ch) {
			cw = ch;
		} else {
			ch = cw;
		}
		int x = 0, y = 0, c = 0;
		g.setColor(Color.black);
		for (boolean element : itemSelected) {
			g.drawRect(x, y, cw, ch);
			if (element) {
				g.fillRect(x, y, cw, ch);
			}
			++c;
			if (c >= nColumns) {
				c = 0;
				x = 0;
				y += ch;
			} else {
				x += cw;
			}
		}
	}
}
