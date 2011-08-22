package spade.lib.basicwin;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.ScrollPane;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Feb 24, 2009
 * Time: 2:10:22 PM
 * Puts the components as in grid, but ajusts the grid dimensions
 * to the current size of the container
 */
public class FlexibleGridLayout implements LayoutManager {
	protected int maxH = 0, maxW = 0;
	protected int hGap = 0, vGap = 0;

	public FlexibleGridLayout() {
	}

	public FlexibleGridLayout(int hGap, int vGap) {
		setGaps(hGap, vGap);
	}

	public void setGaps(int hGap, int vGap) {
		this.hGap = hGap;
		this.vGap = vGap;
	}

	@Override
	public void addLayoutComponent(String name, Component comp) {
	};

	@Override
	public void removeLayoutComponent(Component comp) {
	};

	protected void countWH(Container target) {
		maxW = 0;
		maxH = 0;
		int ncomp = target.getComponentCount();
		for (int i = 0; i < ncomp; i++) {
			Dimension d = target.getComponent(i).getPreferredSize();
			if (d.height > maxH) {
				maxH = d.height;
			}
			if (d.width > maxW) {
				maxW = d.width;
			}
		}
	}

	@Override
	public Dimension preferredLayoutSize(Container target) {
		if (target.getComponentCount() < 1)
			return new Dimension(50, 50);
		countWH(target);
		Container par = target.getParent();
		int tW = 0;
		if (par != null && (par instanceof ScrollPane)) { //take the whole available width
			ScrollPane scp = (ScrollPane) par;
			Dimension ds = scp.getViewportSize();
			if (ds != null) {
				tW = ds.width;
			}
		}
		if (tW < 1) {
			Dimension ds = target.getSize();
			if (ds != null) {
				tW = ds.width;
			}
		}
		int ncomp = target.getComponentCount();
		if (tW < 1)
			if (ncomp > 1) {
				tW = maxW * 2 + hGap;
			} else {
				tW = maxW;
			}
		int nCols = (maxW > 0) ? tW / maxW : 1;
		if (nCols < 1) {
			nCols = 1;
		}
		int nRows = (int) Math.round(Math.ceil(1.0 * ncomp / nCols));
		int tH = nRows * maxH + (nRows - 1) * vGap;
		return new Dimension(tW, tH);
	}

	@Override
	public Dimension minimumLayoutSize(Container target) {
		return preferredLayoutSize(target);
	}

	@Override
	public void layoutContainer(Container target) {
		if (maxW < 1 || maxH < 1) {
			countWH(target);
		}
		Dimension dt = target.getSize();
		int x = 0, y = 0;
		int ncomp = target.getComponentCount();
		for (int i = 0; i < ncomp; i++) {
			Component comp = target.getComponent(i);
			comp.setBounds(x, y, maxW, maxH);
			x += maxW + hGap;
			if (x + maxW > dt.width) {
				x = 0;
				y += maxH + vGap;
			}
		}
	}
}
