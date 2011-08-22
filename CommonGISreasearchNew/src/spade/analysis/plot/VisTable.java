package spade.analysis.plot;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Vector;

import spade.lib.basicwin.Drawable;
import spade.lib.basicwin.Metrics;
import spade.lib.util.StringUtil;

/**
 * <p>�berschrift: </p>
 * <p>Beschreibung: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: Fraunhofer AIS.SPADE</p>
 * @author Mario Boley
 * @version 1.1
 */
public class VisTable implements Drawable {

	public static int LEFT = -1;
	public static int CENTER = 0;
	public static int RIGHT = 1;

	/** Class representing a single cell of a VisTable
	 *
	 */
	public class Cell {
		boolean visible;
		boolean marked;
		Color fgColor;
		Color bkgColor;
		int style = Font.PLAIN;
		Object content;
		int alignment;

		Cell() {
			visible = true;
			marked = false;
			fgColor = Color.black;
			bkgColor = backgroundColor;
			content = new String("");
			alignment = LEFT;
		}

		Cell(Object c) {
			this();
			content = c;
		}
	}

	class Column {
		int width;
		Vector cells;

		void addCells(int s) {
			for (int i = 0; i < s; i++) {
				cells.addElement(new Cell());
			}
		}

		void addCells(Vector c) {
			for (int i = 0; i < c.size(); i++) {
				cells.addElement(new Cell(c.elementAt(i)));
			}
		}

		void addCell(Object c) {
			cells.addElement(new Cell(c));
		}

		void setCellBackground(Color c) {
			if (c == null)
				return;
			for (int i = 0; i < cells.size(); i++) {
				((Cell) cells.elementAt(i)).bkgColor = c;
			}
		}

		void setCellTextColor(Color c) {
			if (c == null)
				return;
			for (int i = 0; i < cells.size(); i++) {
				((Cell) cells.elementAt(i)).fgColor = c;
			}
		}

		/** Calculates the the width that is required to draw all the column's strings without cutting
		 *
		 * @return the required pixel-width of the longest string + 2 * horizontalCellMargin of the Table
		 */
		int getRequiredWidth() {
			int requiredWidth = horizontalCellMargin * 2;
			for (int i = 0; i < cells.size(); i++) {
				if (Metrics.stringWidth(((Cell) cells.elementAt(i)).content.toString()) + 2 * horizontalCellMargin > requiredWidth) {
					requiredWidth = Metrics.stringWidth(((Cell) cells.elementAt(i)).content.toString()) + 2 * horizontalCellMargin;
				}
			}
			return requiredWidth;
		}

		Column(int s, int w) {
			cells = new Vector(s);
			width = w;
			for (int i = 0; i < s; i++) {
				cells.addElement(new Cell());
			}
		}

		Column(Vector c, int w) {
			cells = new Vector(c.size());
			width = w;
			for (int i = 0; i < c.size(); i++) {
				cells.addElement(new Cell(c.elementAt(i)));
			}
		}

	}

	int horizontalCellMargin;
	int lineWidth;
	int rowCount;
	int standardColumnWidth;
	int cellHeight;

	int topMargin, bottomMargin, leftMargin, rightMargin;

	int height, width;

	Color lineColor, backgroundColor, textColor, markTextColor, markBackgroundColor;

	Canvas canvas;
	Vector columns;
	Rectangle bounds;
	boolean destroyed;

	Vector markedCells;

	public VisTable() {
		height = 0;
		horizontalCellMargin = 2;
		lineWidth = 3;
		width = lineWidth;
		standardColumnWidth = Metrics.mm() * 30;
		cellHeight = Metrics.getFontMetrics().getHeight() + 4;
		topMargin = bottomMargin = leftMargin = rightMargin = 5;
		lineColor = Color.black;
		textColor = Color.black;
		backgroundColor = Color.white;
		markTextColor = Color.black;
		markBackgroundColor = Color.lightGray;
		destroyed = false;
		columns = new Vector();
		markedCells = new Vector();
	}

	@Override
	public void setCanvas(Canvas c) {
		canvas = c;
	}

	@Override
	public Dimension getPreferredSize() {
		if (columns == null || columns.isEmpty())
			return new Dimension(0, 0);
		return new Dimension(leftMargin + rightMargin + width, topMargin + bottomMargin + height);
	}

	@Override
	public void setBounds(Rectangle b) {
		bounds = b;
	}

	@Override
	public Rectangle getBounds() {
		return bounds;
	}

	public int getColumnWidth(int colN) {
		if (columns.isEmpty() || colN >= columns.size())
			return -1;
		return ((Column) columns.elementAt(colN)).width;
	}

	// methods concerning marking

/*  public void invertCellMarking(int startCol, int startRow, int endCol, int endRow) {
    if ((startCol>=columns.size()) || (startRow >= rowCount)) return;
    for (int i=startCol; i <= Math.min(endCol,columns.size()); i++) {
      for (int j=startRow; j <= Math.min(endRow,rowCount); j++) {
        getCell(i,j).marked = !getCell(i,j).marked;
        if (getCell(i,j).marked) markedCells.addElement(getCell(i,j));
          else markedCells.removeElement(getCell(i,j));
      }
    }
  } */

	public void markCells(int startCol, int startRow, int endCol, int endRow) {
		if ((startCol >= columns.size()) || (startRow >= rowCount))
			return;
		for (int i = startCol; i <= Math.min(endCol, columns.size()); i++) {
			for (int j = startRow; j <= Math.min(endRow, rowCount); j++) {
				getCell(i, j).marked = true;
				markedCells.addElement(getCell(i, j));
			}
		}
	}

	public void markRow(int rowN) {
		if (rowN >= rowCount)
			return;
		for (int i = 0; i < columns.size(); i++) {
			getCell(i, rowN).marked = true;
			markedCells.addElement(getCell(i, rowN));
		}
	}

	public void unmarkCells(int startCol, int startRow, int endCol, int endRow) {
		if ((startCol >= columns.size()) || (startRow >= rowCount))
			return;
		for (int i = startCol; i <= Math.min(endCol, columns.size()); i++) {
			for (int j = startRow; j <= Math.min(endRow, rowCount); j++) {
				getCell(i, j).marked = false;
				markedCells.removeElement(getCell(i, j));
			}
		}
	}

	public void unmarkAll() {
		for (int i = 0; i < markedCells.size(); i++) {
			((Cell) markedCells.elementAt(i)).marked = false;
		}
		markedCells.removeAllElements();
	}

	// end methods concerning marking

	// methods for changing layout

	public void setColumnWidth(int col, int w) {
		if (columns.isEmpty() || col >= columns.size() || col < 0)
			return;
		width -= ((Column) columns.elementAt(col)).width;
		((Column) columns.elementAt(col)).width = w;
		width += w;
	}

	public void autoSetColumnWidth(int col) {
		if (col >= columns.size())
			return;
		setColumnWidth(col, ((Column) columns.elementAt(col)).getRequiredWidth());
	}

	public void globalAutoSetColumnWidth() {
		for (int i = 0; i < columns.size(); i++) {
			setColumnWidth(i, ((Column) columns.elementAt(i)).getRequiredWidth());
		}
	}

	public void setColumnBackgroundColor(int column, Color color) {
		if (columns.size() <= column)
			return;
		((Column) columns.elementAt(column)).setCellBackground(color);
	}

	public void setRowBackgroundColor(int row, Color color) {
		if (rowCount <= row)
			return;
		for (int i = 0; i < columns.size(); i++) {
			getCell(i, row).bkgColor = color;
		}
	}

	public void setCellBackgroundColor(int colN, int rowN, Color color) {
		if (getCell(colN, rowN) != null) {
			getCell(colN, rowN).bkgColor = color;
		}
	}

	public void setColumnTextColor(int column, Color color) {
		if (columns.size() <= column)
			return;
		((Column) columns.elementAt(column)).setCellTextColor(color);
	}

	public void setRowTextColor(int row, Color color) {
		if (rowCount <= row)
			return;
		for (int i = 0; i < columns.size(); i++) {
			getCell(i, row).fgColor = color;
		}
	}

	public void setCellTextColor(int colN, int rowN, Color color) {
		if (getCell(colN, rowN) != null) {
			getCell(colN, rowN).fgColor = color;
		}
	}

	public void setColumnStyle(int colN, int style) {
		if ((colN < 0) || (colN >= columns.size()))
			return;
		for (int i = 0; i < rowCount; i++) {
			getCell(colN, i).style = style;
		}
	}

	public void setRowStyle(int rowN, int style) {
		if ((rowN < 0) || (rowN >= rowCount))
			return;
		for (int i = 0; i < columns.size(); i++) {
			getCell(i, rowN).style = style;
		}
	}

	public void setCellStyle(int colN, int rowN, int style) {
		if (getCell(colN, rowN) != null) {
			getCell(colN, rowN).style = style;
		}
	}

	public void setColumnAlignment(int colN, int alignment) {
		if ((colN < 0) || (colN >= columns.size()))
			return;
		for (int i = 0; i < rowCount; i++) {
			getCell(colN, i).alignment = alignment;
		}
	}

	public void setRowAlignment(int rowN, int alignment) {
		if ((rowN < 0) || (rowN >= rowCount))
			return;
		for (int i = 0; i < columns.size(); i++) {
			getCell(i, rowN).alignment = alignment;
		}
	}

	public void setAlignmentGlobally(int alignment) {
		for (int i = 0; i < columns.size(); i++) {
			for (int j = 0; j < rowCount; j++) {
				getCell(i, j).alignment = alignment;
			}
		}
	}

	public void setLineWidth(int w) {
		lineWidth = Math.max(w, 1);

		height = rowCount * (cellHeight + lineWidth) + lineWidth;

		if (columns == null || columns.isEmpty())
			return;
		width = 0;
		int i = 0;
		while (i < columns.size()) {
			width += ((Column) columns.elementAt(i)).width;
			i++;
		}
		width += ((columns.size() + 1) * lineWidth);

	}

	// end methods for changing layout

	protected int getColumnX(int c) {
		if (columns == null || columns.isEmpty() || columns.size() < c)
			return -1;
		int w = leftMargin;
		int i = 0;
		while (i < c) {
			w += getColumnWidth(i);
			i++;
		}
		return w + ((c + 1) * lineWidth);
	}

	protected int getLineY(int l) {
		return topMargin + lineWidth + l * (cellHeight + lineWidth);
	}

	protected Cell getCell(int colN, int linN) {
		if (colN >= columns.size() || linN >= rowCount)
			return null;
		return (Cell) ((Column) columns.elementAt(colN)).cells.elementAt(linN);
	}

	// methods for adding data to the table

	public void appendColumn(Vector c) {
		columns.addElement(new Column(c, standardColumnWidth));
		int diff = c.size() - rowCount;
		if (diff > 0) {
			for (int i = 0; i < columns.size() - 1; i++) {
				((Column) columns.elementAt(i)).addCells(diff);
			}
		} else if (diff < 0) {
			((Column) columns.lastElement()).addCells(-1 * diff);
		}
		rowCount = Math.max(rowCount, c.size());
		height = lineWidth + rowCount * (cellHeight + lineWidth);
		width += standardColumnWidth + lineWidth;
	}

	public void appendColumn(int w) {
		columns.addElement(new Column(rowCount, w));
		width += w + lineWidth;
	}

	public void appendRow(Vector c) {
		setRowContent(rowCount, c);
	}

	public void appendRow() {
		height += cellHeight + lineWidth;
		for (int i = 0; i < columns.size(); i++) {
			((Column) columns.elementAt(i)).addCell("");
		}
		rowCount++;
	}

	public void setContent(int col, int row, Object c) {
		if (c == null)
			return;
		for (int i = columns.size(); i < col + 1; i++) {
			appendColumn(standardColumnWidth);
		}
		for (int i = rowCount; i < row + 1; i++) {
			appendRow();
		}
		getCell(col, row).content = c;
	}

	/** Sets content of specified row.
	 * If this does not match the current dimension of the table the following happens:
	 * - vector of contents is longer than the row: appends an appropriate amount of columns
	 * - vector is shorter than row has entries: row is filled up with empty strings
	 * - rowN >= current amount of rows: appends an appropriate number of rows
	 *
	 * @param rowN the row to be updated (0 <= rowN)
	 * @param content vector of objects to be put into the row
	 */
	public void setRowContent(int rowN, Vector content) {
		if (content == null || rowN < 0)
			return;
		int diff = columns.size() - content.size();
		if (diff < 0) {
			for (int i = 0; i < -1 * diff; i++) {
				appendColumn(standardColumnWidth);
			}
		} else if (diff > 0) {
			for (int i = content.size(); i < columns.size(); i++) {
				content.addElement("");
			}
		}
		for (int i = rowCount - 1; i < rowN; i++) {
			appendRow();
		}
		for (int i = 0; i < content.size(); i++) {
			getCell(i, rowN).content = content.elementAt(i);
		}
	}

	public void setColumnContent(int colN, Vector content) {
		if (content == null || colN < 0)
			return;
		int diff = rowCount - content.size();
		if (diff < 0) {
			for (int i = rowCount; i < rowCount - diff; i++) {
				appendRow();
			}
		} else if (diff > 0) {
			for (int i = content.size(); i < rowCount; i++) {
				content.addElement("");
			}
		}
		for (int i = columns.size() - 1; i < colN; i++) {
			appendColumn(standardColumnWidth);
		}
		for (int i = 0; i < content.size(); i++) {
			getCell(colN, i).content = content.elementAt(i);
		}
	}

	// end methods for adding data to the table

	// draw methods

	public void drawCell(int colN, int linN, Graphics g) {
		if (g == null)
			return;
		if (colN >= columns.size() || linN >= rowCount)
			return;
		Column col = (Column) columns.elementAt(colN);
		Cell cell = getCell(colN, linN);
		g.setColor(cell.marked ? markBackgroundColor : cell.bkgColor);
		g.fillRect(getColumnX(colN), getLineY(linN), col.width, cellHeight);
		int cX = getColumnX(colN);
		String str = cell.content.toString();

		int strWidth = g.getFontMetrics().stringWidth(str);
		int colWidth = getColumnWidth(colN) - 2 * horizontalCellMargin;
		if (strWidth > colWidth) {
			colWidth -= g.getFontMetrics().stringWidth("...");
			str = StringUtil.getCutString(str, colWidth, g);
			str = str + "...";
		}
		if (cell.alignment == LEFT) {
			cX += horizontalCellMargin;
		} else if (cell.alignment == RIGHT) {
			cX += col.width - Metrics.stringWidth(str);
		} else if (cell.alignment == CENTER) {
			cX += col.width / 2 - Metrics.stringWidth(str) / 2;
		}
		g.setColor(cell.marked ? markTextColor : cell.fgColor);

		g.setFont(new Font(g.getFont().getName(), cell.marked ? Font.BOLD : cell.style, g.getFont().getSize()));
		g.drawString(str, cX, getLineY(linN) + cellHeight / 2 + g.getFontMetrics().getHeight() / 3);
	}

	public void drawLines(Graphics g) {
		if (columns.isEmpty() || rowCount == 0)
			return;
		g.setColor(lineColor);
		for (int i = 0; i <= columns.size(); i++) {
			g.fillRect(getColumnX(i) - lineWidth, topMargin, lineWidth, height);
		}
		for (int i = 0; i <= rowCount; i++) {
			g.fillRect(leftMargin, getLineY(i) - lineWidth, width, lineWidth);
		}
	}

	/** Draws the table
	 *
	 * @param g the Graphics-context to be drawn with
	 */
	@Override
	public void draw(Graphics g) {
		for (int i = 0; i < columns.size(); i++) {
			for (int j = 0; j < rowCount; j++) {
				drawCell(i, j, g);
			}
		}
		drawLines(g);
	}

	// end draw methods

	@Override
	public void destroy() {
		destroyed = true;
		if (canvas == null)
			return;
	}

	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

}