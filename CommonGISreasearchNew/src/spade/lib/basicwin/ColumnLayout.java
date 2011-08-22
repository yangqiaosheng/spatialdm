package spade.lib.basicwin;

import java.awt.Button;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.ScrollPane;

public class ColumnLayout implements LayoutManager {
	public static final int Hor_Centered = 0, Hor_Left = 1, Hor_Right = 2, Hor_Stretched = 3, al_first = 0, al_last = 3;
	protected int H = 0, W = 0;
	protected boolean stretchLast = false, adjustWidthToViewport = true;
	protected int align = Hor_Stretched;

	public void setStretchLast(boolean flag) {
		stretchLast = flag;
	}

	public void setAdjustWidthToViewport(boolean flag) {
		adjustWidthToViewport = flag;
	}

	public void setAlignment(int value) {
		if (value >= al_first && value <= al_last) {
			align = value;
		}
	}

	void CountWH(Container target) {
		W = 0;
		H = 0;
		int ncomp = target.getComponentCount();
		for (int i = 0; i < ncomp; i++) {
			Dimension d = target.getComponent(i).getPreferredSize();
			H += d.height;
			if (d.width > W) {
				W = d.width;
			}
		}
	}

	@Override
	public Dimension minimumLayoutSize(Container target) {
		if (W <= 0) {
			CountWH(target);
		}
		return new Dimension(W, H);
	}

	@Override
	public Dimension preferredLayoutSize(Container target) {
		CountWH(target);
		Container par = target.getParent();
		if (par != null && (par instanceof ScrollPane)) { //take the whole available width
			ScrollPane scp = (ScrollPane) par;
			Dimension ds = scp.getViewportSize();
			if (ds.height < H) {
				ds.width -= scp.getVScrollbarWidth() + 4;
			}
			if (adjustWidthToViewport || ds.width > W) {
				W = ds.width;
			}
		}
		return new Dimension(W, H);
	}

	@Override
	public void layoutContainer(Container target) {
		Dimension dt = target.getSize();
		int y = 0;
		int ncomp = target.getComponentCount();
		for (int i = 0; i < ncomp; i++) {
			Component comp = target.getComponent(i);
			Dimension d = comp.getPreferredSize();
			int h = d.height;
			if (i == ncomp - 1 && y < dt.height && (stretchLast || h == 0)) {
				h = dt.height - y;
			}
			if (y < dt.height && y + h > dt.height) {
				h = dt.height - y;
			}
			int x0 = 0, wd = dt.width; //values for Hor_Stretched
			if ((align != Hor_Stretched || (comp instanceof Button)) && wd > d.width) {
				int dx = wd - d.width;
				wd = d.width;
				if (align == Hor_Centered || (comp instanceof Button)) {
					x0 = dx / 2;
				} else if (align == Hor_Right) {
					x0 = dx;
				}
			}
			comp.setBounds(x0, y, wd, h);
			y += h;
		}
	}

	@Override
	public void addLayoutComponent(String name, Component comp) {
	};

	@Override
	public void removeLayoutComponent(Component comp) {
	};
}
