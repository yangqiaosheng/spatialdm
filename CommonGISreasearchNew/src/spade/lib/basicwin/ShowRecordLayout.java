package spade.lib.basicwin;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Scrollbar;

public class ShowRecordLayout implements LayoutManager {
	StringColumn left = null, right = null;
	Scrollbar scbar = null;
	int scw = 0;
	static int space = 0;
	boolean ScBarIsShown = true;

	@Override
	public void addLayoutComponent(String name, Component comp) {
		if (name.equals("Left")) {
			left = (StringColumn) comp;
		} else if (name.equals("Right")) {
			right = (StringColumn) comp;
		} else if (name.equals("Scrollbar")) {
			scbar = (Scrollbar) comp;
		}
	}

	@Override
	public void removeLayoutComponent(Component comp) {
	}

	@Override
	public Dimension preferredLayoutSize(Container parent) {
		if (space == 0) {
			space = Metrics.stringWidth("  ");
		}
		Dimension d1 = left.getPreferredSize(), d2 = right.getPreferredSize();
		if (scw == 0) {
			scw = scbar.getPreferredSize().width;
		}
		return new Dimension(scw + d1.width + d2.width + space, d1.height);
	}

	@Override
	public Dimension minimumLayoutSize(Container parent) {
		Dimension d1 = left.getMinimumSize(), d2 = right.getMinimumSize();
		return new Dimension(scw + d1.width + d2.width + space, d1.height);
	}

	@Override
	public void layoutContainer(Container target) {
		Insets insets = target.getInsets();
		Dimension size = target.getSize(), sizepref = preferredLayoutSize(target);
		int top = insets.top, h = size.height - insets.bottom - top, leftmarg = insets.left, w = size.width - insets.right - leftmarg - scw;
		boolean ShowScrollbar = true;
		if (w > sizepref.width - scw) {
			w = sizepref.width - scw;
		}
		if (h >= sizepref.height) {
			h = sizepref.height;
			w += scw;
			ShowScrollbar = false;
		}
		int w1 = left.getPreferredSize().width, w2 = right.getPreferredSize().width;
		int ws = w - space;
		if (w1 + w2 > ws) {
			w1 = ws - w2;
			if (w1 < 10) {
				w1 = 10;
				w2 = ws - w1;
			}
		}
		left.setBounds(leftmarg, top, w1, h);
		right.setBounds(leftmarg + w1 + space, top, w2, h);
		if (ShowScrollbar) {
			int value = scbar.getValue();
			if (value + h > sizepref.height) {
				value = sizepref.height - h;
			}
			scbar.setBounds(leftmarg + w, top, scw, h);
			scbar.setValues(value, h, 0, sizepref.height/*-h*/);
			scbar.setUnitIncrement(left.sh);
			scbar.setBlockIncrement(h - left.sh);
			if (!ScBarIsShown) {
				scbar.setVisible(true);
			}
			ScBarIsShown = true;
		} else {
			scbar.setValue(0);
			scbar.setVisible(false);
			ScBarIsShown = false;
		}
		left.setPosition(scbar);
		right.setPosition(scbar);
	}
}
