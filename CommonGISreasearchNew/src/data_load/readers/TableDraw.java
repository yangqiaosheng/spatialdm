package data_load.readers;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Panel;
import java.awt.Point;
import java.awt.ScrollPane;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Vector;

public class TableDraw extends Panel implements MouseListener {
	protected DataSample sample = null;
	protected ColumnDraw cd[] = null;
	protected int W = 0, HH = 0, H = 0;
	protected Vector actionListeners = null;
	protected int active = -1;

	public TableDraw(DataSample sample) {
		this.sample = sample;
		cd = new ColumnDraw[sample.getNFields()];
		setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		for (int i = 0; i < cd.length; i++) {
			cd[i] = new ColumnDraw(sample, i);
			add(cd[i]);
			cd[i].addMouseListener(this);
		}
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent evt) {
				resized();
			}
		});
	}

	public void addActionListener(ActionListener l) {
		if (l == null)
			return;
		if (actionListeners == null) {
			actionListeners = new Vector(10, 10);
		}
		actionListeners.addElement(l);
	}

	public void removeActionListener(ActionListener l) {
		if (l == null || actionListeners == null)
			return;
		actionListeners.removeElement(l);
	}

	public void countSizes(FontMetrics fm) {
		if ((W <= 0 || HH <= 0) && fm != null) {
			W = 0;
			HH = 0;
			for (ColumnDraw element : cd) {
				element.setHeaderHeight(0);
				Dimension d = element.getHeaderSize(fm);
				W += d.width;
				if (HH < d.height) {
					HH = d.height;
				}
			}
			H = HH;
			for (ColumnDraw element : cd) {
				element.setHeaderHeight(HH);
				element.setTotalHeight(0);
				int h = element.getTotalHeight();
				if (h > H) {
					H = h;
				}
			}
			for (ColumnDraw element : cd) {
				element.setTotalHeight(H);
				element.invalidate();
			}
		}
		setSize(W, H);
	}

	@Override
	public Dimension getPreferredSize() {
		if (W > 0 && H > 0)
			return new Dimension(W, H);
		return new Dimension(200, 100);
	}

	protected void resized() {
		if (W > 0 && H > 0)
			return;
		Graphics g = getGraphics();
		if (g == null && getParent() != null) {
			g = getParent().getGraphics();
		}
		if (g == null)
			return;
		countSizes(g.getFontMetrics());
		invalidate();
		getParent().validate();
	}

	public void highlightColumn(int colN, boolean flag) {
		if (flag && active >= 0 && active < cd.length) {
			cd[active].setHighlighted(false);
		}
		active = -1;
		if (colN >= 0 && colN < cd.length) {
			cd[colN].setHighlighted(flag);
			if (flag) {
				active = colN;
			}
		}
		if (flag && getParent() instanceof ScrollPane) {
			ScrollPane sp = (ScrollPane) getParent();
			Point p1 = cd[colN].getLocation(), p2 = sp.getScrollPosition();
			int cw = cd[colN].getSize().width, spw = sp.getViewportSize().width;
			if (p1.x < p2.x || p1.x + cw > p2.x + spw) {
				sp.setScrollPosition(p1.x, 0);
			}
		}
	}

	public void setColumnName(int n, String name) {
		if (cd == null || name == null || n < 0 || n >= cd.length)
			return;
		cd[n].setName(name);
		checkHeaderResized(cd[n]);
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
	public void mousePressed(MouseEvent e) {
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (actionListeners != null && actionListeners.size() > 0) {
			Object s = e.getSource();
			for (int j = 0; j < cd.length; j++)
				if (s == cd[j]) {
					for (int i = 0; i < actionListeners.size(); i++) {
						ActionListener l = (ActionListener) actionListeners.elementAt(i);
						l.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, String.valueOf(j)));
					}
					break;
				}
		}
	}

	void checkHeaderResized(ColumnDraw c) {
		if (!c.headerHeightChanged && !c.columnWidthChanged)
			return;
		if (c.headerHeightChanged) {
			Graphics g = getGraphics();
			if (g == null)
				return;
			W = 0;
			HH = 0;
			countSizes(g.getFontMetrics());
		} else {
			W = 0;
			for (ColumnDraw element : cd) {
				W += element.W;
			}
			c.setSize(c.W, H);
			setSize(W, H);
		}
		c.headerHeightChanged = false;
		c.columnWidthChanged = false;
		invalidate();
		getParent().validate();
	}
}