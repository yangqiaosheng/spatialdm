package spade.lib.basicwin;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;

public class RowLayout implements LayoutManager {
	protected int H = 0, W = 0, hgap = 0, vgap = 0;
	protected boolean stretchLast = false;

	public RowLayout() {
	}

	public RowLayout(int horGap, int vertGap) {
		setGaps(horGap, vertGap);
	}

	public void setGaps(int horGap, int vertGap) {
		hgap = horGap;
		vgap = vertGap;
	}

	public void setStretchLast(boolean value) {
		stretchLast = value;
	}

	protected void CountWH(Container target) {
		W = 0;
		H = 0;
		int ncomp = target.getComponentCount();
		for (int i = 0; i < ncomp; i++) {
			Dimension d = target.getComponent(i).getPreferredSize();
			W += d.width;
			if (i > 0) {
				W += hgap;
			}
			if (d.height > H) {
				H = d.height;
			}
		}
		H += 2 * vgap;
	}

	@Override
	public Dimension minimumLayoutSize(Container target) {
		int w = 0, h = 0;
		for (int i = 0; i < target.getComponentCount(); i++) {
			Dimension d = target.getComponent(i).getMinimumSize();
			w += d.width;
			if (i > 0) {
				w += hgap;
			}
			if (d.height > h) {
				h = d.height;
			}
		}
		h += 2 * vgap;
		return new Dimension(w, h);
	}

	@Override
	public Dimension preferredLayoutSize(Container target) {
		CountWH(target);
		return new Dimension(W, H);
	}

	@Override
	public void layoutContainer(Container target) {
		Dimension dt = target.getSize();
		int x = 0;
		boolean notEnough = dt.width < preferredLayoutSize(target).width;
		int minW = minimumLayoutSize(target).width, avH = dt.height - 2 * vgap;
		int ncomp = target.getComponentCount();
		for (int i = 0; i < ncomp; i++) {
			Component comp = target.getComponent(i);
			Dimension d = /*(notEnough)?comp.getMinimumSize():*/comp.getPreferredSize();
			int w = d.width;
			if (notEnough) {
				float ratio = ((float) dt.width - w) / minW;
				if (ratio > 1) {
					w = Math.round(ratio * w);
					d = comp.getPreferredSize();
					if (w > d.width) {
						w = d.width;
					}
				}
			}
			if (w < dt.width && x + w > dt.width) {
				w = dt.width - x;
			} else if (stretchLast && i == ncomp - 1) {
				w = dt.width - x;
			}
			comp.setBounds(x, vgap, w, Math.min(avH, d.height));
			x += w + hgap;
			if (notEnough) {
				minW -= w;
			}
		}
	}

	@Override
	public void addLayoutComponent(String name, Component comp) {
	};

	@Override
	public void removeLayoutComponent(Component comp) {
	};
}