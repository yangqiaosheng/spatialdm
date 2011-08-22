package spade.analysis.plot;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Vector;

import spade.analysis.calc.PairDataContainer;
import spade.lib.basicwin.HotSpot;
import spade.lib.basicwin.Metrics;
import spade.lib.util.StringUtil;

/**
 * <p>�berschrift: </p>
 * <p>Beschreibung: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: Fraunhofer AIS.SPADE</p>
 * @author unbekannt
 * @version 1.0
 */
public class LowerTrBarMatrix extends PairDataMatrix implements MouseListener {

	class BarCell extends Cell {

		Rectangle bar;
		HotSpot hotSpot;

		BarCell(float v, int x, int y, PairDataMatrixItem d1, PairDataMatrixItem d2) {
			super(v, x, y, d1, d2);
			bar = new Rectangle();
			hotSpot = new HotSpot(canvas);
			setup();
		}

		boolean containsBar(int x, int y) {
			return bar.contains(x, y);
		}

		int getCompareY() {
			if (drawSigned && value < 0)
				return bar.y + bar.height - y;
			else
				return bar.y - y;
		}

		@Override
		void setup() {
			int zl = drawSigned ? (y + (cellHeight / 2) - 2) : y + cellHeight - lineWidth;
			int h = Math.round((drawSigned ? (cellHeight / 2) : cellHeight) / maxValue * (drawSigned ? value : Math.abs(value)));
			if (drawSigned && h < 0) {
				bar.y = zl;
			} else {
				bar.y = zl - Math.abs(h);
			}
			bar.height = Math.abs(h);
			bar.x = x + relBarX;
			bar.width = barWidth;
			hotSpot.setLocation(x, y);
			hotSpot.setSize(cellWidth, cellHeight);
			hotSpot.setPopup(item1.screenDescription + " -\n" + item2.screenDescription + ":\n" + StringUtil.floatToStr(dataSource.getValue(item1.indexInContainer, item2.indexInContainer)));
		}

		@Override
		void draw(Graphics g) {
			g.setColor(cellBkgColor);
			g.fillRect(x, y - lineWidth, cellWidth, cellHeight);

			g.setColor((value < 0) ? negBarColor : posBarColor);
			g.fillRect(bar.x, bar.y, bar.width, bar.height);
			g.setColor(frgColor);
			g.drawRect(bar.x, bar.y, bar.width, bar.height);
			if (drawSigned) {
				int ol = (cellWidth - barWidth) / 4;
				g.drawLine(x + relBarX - ol, y - 2 + cellHeight / 2, x + relBarX + barWidth + ol, y - 2 + cellHeight / 2);
			}
			if (relCompareY != 0) {
				g.setColor(Color.white);
				g.drawLine(x + relBarX, y + relCompareY, x + relBarX + barWidth, y + relCompareY);
			}
		}
	}

	protected boolean showDiagonal;
	protected boolean drawSigned;

	protected Color negBarColor;
	protected Color posBarColor;

/* percentage of space of cells used for the bars: 0 <= barspace <= 1 */
	protected float relativeBarWidth;
	protected int barWidth;
	protected int relBarX;
	protected int relCompareY;

	protected Vector actionListener;

	public LowerTrBarMatrix(PairDataContainer source, boolean showDiag) {
		super(source);
		showDiagonal = showDiag;
		actionListener = new Vector();
		drawSigned = true;
		lineWidth = 2;
		relCompareY = 0;
		setRelativeBarWidth(0.45f);
		negBarColor = Color.red;
		posBarColor = Color.blue;
	}

	public void addActionListener(ActionListener listener) {
		actionListener.addElement(listener);
	}

	public void removeActionListener(ActionListener listener) {
		actionListener.removeElement(listener);
	}

	public void setDrawSigned(boolean ds) {
		drawSigned = ds;
		relCompareY = 0;
		for (int i = 0; i < cells.size(); i++) {
			((BarCell) cells.elementAt(i)).setup();
		}
	}

	@Override
	public void setCellWidth(int w) {
		super.setCellWidth(w);
		setRelativeBarWidth(relativeBarWidth);
	}

/*
public void setShowDiagonal(boolean dd) {
  if (dd==showDiagonal) return;
  showDiagonal=dd;
  for (int i=0; i<cells.size(); i++) {
    BarCell c=(BarCell)cells.elementAt(i);
    if (dd) c.y+=cellHeight; else c.y-=cellHeight;
    c.setup();
  }
}
*/

	/** Sets the relative amount of horizontal space used for each bar.
	 * Calculates barWidth with: cellWidth*relativeBarWidth
	 *
	 * @param rbw 0 <= rbw <= 1
	 */
	public void setRelativeBarWidth(float rbw) {
		if ((rbw > 1) || (rbw < 0))
			return;
		relativeBarWidth = rbw;
		barWidth = Math.round(cellWidth * relativeBarWidth);
		relBarX = Math.round(cellWidth * ((1 - relativeBarWidth) / 2));
	}

	@Override
	public void drawLines(Graphics g) {
		int y = topMargin + Metrics.getFontMetrics().getHeight();
		int ml = lineWidth + (items.size() - (showDiagonal ? 0 : 1)) * (cellHeight + lineWidth);
		g.setColor(frgColor);
		g.fillRect(leftMargin, y, lineWidth, ml);
		g.fillRect(leftMargin, y + ml - lineWidth, ml, lineWidth);
		for (int i = 0; i < items.size() - (showDiagonal ? 0 : 1); i++) {
			g.fillRect(leftMargin, y, (i + 1) * (cellWidth + lineWidth), lineWidth);
			g.fillRect(leftMargin + (i + 1) * (lineWidth + cellWidth), y, lineWidth, ml - i * (cellHeight + lineWidth));
			y += (lineWidth + cellHeight);
		}
	}

	@Override
	public void drawDescriptions(Graphics g) {
		int y = topMargin + Metrics.getFontMetrics().getHeight() - 1;
		int x = leftMargin + lineWidth;
		if (showDiagonal) {
			x += cellWidth + lineWidth;
		}
		for (int i = 0; i < items.size(); i++) {
			g.drawString(((PairDataMatrixItem) items.elementAt(i)).screenDescription, x, y);
			y += lineWidth + cellWidth;
			x += lineWidth + cellHeight;
		}
	}

	@Override
	public void addItem(PairDataMatrixItem item) {
		super.addItem(item);
		for (int i = 0; i < items.size() - (showDiagonal ? 0 : 1); i++) {
			cells.addElement(new BarCell(dataSource.getValue(((PairDataMatrixItem) items.elementAt(i)).indexInContainer, item.indexInContainer), leftMargin + lineWidth + i * (lineWidth + cellWidth), topMargin + lineWidth
					+ Metrics.getFontMetrics().getHeight() + (items.size() - 1) * (lineWidth + cellHeight) - (showDiagonal ? (-1 * lineWidth) : cellHeight), (PairDataMatrixItem) items.elementAt(i), item));
		}
	}

	@Override
	public Dimension getPreferredSize() {
		if (items.size() == 0 || (items.size() == 1 && !showDiagonal))
			return new Dimension(leftMargin + rightMargin, topMargin + bottomMargin);

		return new Dimension(leftMargin + rightMargin + lineWidth + Metrics.stringWidth(((PairDataMatrixItem) items.lastElement()).screenDescription) + (items.size() - (showDiagonal ? 0 : 1)) * (cellWidth + lineWidth),

		topMargin + bottomMargin + lineWidth + Metrics.getFontMetrics().getHeight() + (items.size() - (showDiagonal ? 0 : 1)) * (cellWidth + lineWidth));
	}

	protected BarCell getCell(int i, int j) {
		if (j > i)
			return null;
		int pos = (i * (i + 1)) / 2 + j;
		if (cells.size() > pos)
			return (BarCell) cells.elementAt(pos);
		else
			return null;
	}

//overwritten methods
	@Override
	public void setCanvas(Canvas c) {
		if (canvas != null) {
			canvas.removeMouseListener(this);
		}
		super.setCanvas(c);
		if (c != null) {
			c.addMouseListener(this);
			if (cells != null) {
				for (int i = 0; i < cells.size(); i++) {
					((BarCell) cells.elementAt(i)).hotSpot.setOwner(canvas);
				}
			}
		}
		setVisible(visible);
	}

	@Override
	public void setVisible(boolean vis) {
		super.setVisible(vis);
		if (canvas == null)
			return;
		else {
			canvas.removeMouseListener(this);
		}
		for (int i = 0; i < cells.size(); i++) {
			((BarCell) cells.elementAt(i)).hotSpot.setEnabled(vis);
		}
		if (vis) {
			canvas.addMouseListener(this);
		}
	}

	@Override
	public void destroy() {
		super.destroy();
		if (canvas != null) {
			canvas.removeMouseListener(this);
		}
	}

//end overwritten methods

//methods of MouseListener
	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (cells == null)
			return;
		BarCell cell = getCell((e.getY() - topMargin - Metrics.getFontMetrics().getHeight()) / (cellHeight + lineWidth), (e.getX() - leftMargin) / (cellWidth + lineWidth));
		if (cell == null)
			return;
		if (cell.contains(e.getX(), e.getY())) {
			if (cell.containsBar(e.getX(), e.getY())) {
				if (relCompareY == cell.getCompareY()) {
					relCompareY = 0;
				} else {
					relCompareY = cell.getCompareY();
				}
				for (int j = 0; j < cells.size(); j++) {
					((Cell) cells.elementAt(j)).draw(canvas.getGraphics());
				}
				return;
			} else {
				for (int j = 0; j < actionListener.size(); j++) {
					((ActionListener) actionListener.elementAt(j)).actionPerformed(new ActionEvent(this, -1, String.valueOf(cell.item1.indexInContainer).concat(",").concat(String.valueOf(cell.item2.indexInContainer))));
				}
				return;
			}
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}
//end methods MouseListener
}
