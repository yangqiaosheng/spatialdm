package data_load.readers;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;

import spade.lib.basicwin.StringInRectangle;
import spade.vis.database.AttributeTypes;

public class ColumnDraw extends Canvas {
	protected DataSample sample = null;
	protected int colN = 0;
	protected String typestr = null;
	int W = 0, HH = 0, VH = 0, H = 0, minw = 0, fh = 0, asc = 0;
	protected String colName = null;
	protected StringInRectangle sr = null;
	public boolean IsHighlighted = false;
	boolean headerHeightChanged = false, columnWidthChanged = true;

	public ColumnDraw(DataSample data, int colN) {
		super();
		sample = data;
		this.colN = colN;
		if (sample.getFieldType(colN) == AttributeTypes.character) {
			typestr = "string";
		} else if (sample.getFieldType(colN) == AttributeTypes.logical) {
			typestr = "logical";
		} else if (sample.getFieldType(colN) == AttributeTypes.integer) {
			typestr = "integer";
		} else if (sample.getFieldType(colN) == AttributeTypes.real) {
			typestr = "real";
		} else {
			typestr = "string";
		}
		colName = sample.getFieldName(colN);
	}

	public Dimension getHeaderSize(FontMetrics fm) {
		if ((W <= 0 || HH <= 0) && fm != null) {
			minw = fm.stringWidth(typestr) + 4;
			for (int i = 0; i < sample.getNRecords(); i++) {
				String val = sample.getValue(i, colN);
				if (val != null) {
					int w = fm.stringWidth(val) + 4;
					if (minw < w) {
						minw = w;
					}
				}
			}
			fh = fm.getHeight();
			asc = fm.getAscent();
			if (sr == null) {
				sr = new StringInRectangle(colName);
			}
			sr.setMetrics(fm);
			sr.setRectSize(minw, 0);
			Dimension d = sr.countSizes();
			W = (d.width + 4 > minw) ? d.width + 4 : minw;
			HH = d.height;
		}
		return new Dimension(W, HH);
	}

	public void setHeaderHeight(int h) {
		HH = h;
	}

	public int getTotalHeight() {
		if (VH == 0) {
			VH = fh * (sample.getNRecords() + 2); //plus type and N of the field
		}
		if (H < HH + VH) {
			H = HH + VH;
		}
		return H;
	}

	public void setTotalHeight(int h) {
		H = h;
	}

	@Override
	public Dimension getPreferredSize() {
		if (W > 0 && HH > 0)
			return new Dimension(W, getTotalHeight());
		return new Dimension(100, 100);
	}

	public void setHighlighted(boolean flag) {
		IsHighlighted = flag;
		repaint();
	}

	@Override
	public void paint(Graphics g) {
		FontMetrics fm = g.getFontMetrics();
		if (W <= 0 || HH <= 0) {
			getHeaderSize(fm);
		}
		Dimension d = getSize();
		g.setColor(Color.black);
		g.drawLine(0, 0, 0, d.height);
		g.setColor((IsHighlighted) ? new Color(0, 96, 0) : new Color(160, 255, 160));
		g.fillRect(1, 0, d.width, HH + fh);
		g.setColor((IsHighlighted) ? Color.white : Color.black);
		g.drawString(String.valueOf(colN + 1), 3, asc);
		sr.setBounds(0, fh, d.width, HH);
		Dimension hs = sr.countSizes();
		sr.draw(g);
		int y = fh + HH + asc;
		g.setColor((IsHighlighted) ? new Color(24, 96, 96) : new Color(140, 225, 225));
		g.fillRect(1, y - asc, d.width, fh);
		g.setColor((IsHighlighted) ? Color.white : Color.blue);
		g.drawString(typestr, 3, y);
		y += fh;
		g.setColor((IsHighlighted) ? Color.pink : Color.lightGray);
		g.fillRect(1, y - asc, d.width, d.height - HH - fh);
		g.setColor(Color.black);
		for (int i = 0; i < sample.getNRecords(); i++) {
			String val = sample.getValue(i, colN);
			if (val != null)
				if (AttributeTypes.isNumericType(sample.getFieldType(colN))) {
					int w = fm.stringWidth(val);
					g.drawString(val, d.width - w - 3, y);
				} else {
					g.drawString(val, 3, y);
				}
			y += fh;
		}
	}
}