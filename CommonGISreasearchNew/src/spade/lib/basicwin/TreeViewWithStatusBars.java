package spade.lib.basicwin;

import java.awt.Graphics;
import java.util.Vector;

import spade.analysis.plot.SegmentedBarPlot;

public class TreeViewWithStatusBars extends TreeView {

	protected Vector vsbp = null;

	@Override
	public void setup() {
		super.setup();
		// process vsbp
	}

	public void setVSBP(Vector vsbp) {
		this.vsbp = vsbp;
	}

	@Override
	public void drawItem(Graphics g, int n, int x, int y, int w, boolean isActive) {
		if (order == null || n < 0 || n >= order.size())
			return;
		int dx = 0;
		if (vsbp != null && n < vsbp.size()) {
			int idx = order.elementAt(n);
			if (idx != -1) {
				dx = 100;
				SegmentedBarPlot sbp = (SegmentedBarPlot) vsbp.elementAt(idx);
				sbp.draw(g, y, dx, itemH());
			}
		}
		super.drawItem(g, n, x + dx, y, w, isActive);
	}

}