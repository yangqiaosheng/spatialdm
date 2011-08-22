package spade.lib.basicwin;

import java.awt.Component;
import java.awt.Panel;

public class SplitPanel extends Panel {
	SplitLayout spl = null;

	public SplitPanel(boolean splitVertical) {
		super();
		spl = new SplitLayout(this, (splitVertical) ? SplitLayout.VERT : SplitLayout.HOR);
		setLayout(spl);
	}

	public void setAllowSwapParts(boolean allowSwapParts) {
		if (spl != null) {
			spl.setAllowSwapParts(allowSwapParts);
		}
	}

	public void addSplitComponent(Component c) {
		addSplitComponent(c, 1.0f);
	}

	public void addSplitComponentAt(Component c, int idx) {
		addSplitComponentAt(c, 1.0f, idx);
	}

	public void addSplitComponent(Component c, float part) {
		spl.addComponent(c, part);
	}

	public void addSplitComponentAt(Component c, float part, int idx) {
		spl.addComponentAt(c, part, idx);
	}

	public void replaceSplitComponent(Component c, int idx) {
		if (idx < 0 || idx >= getSplitComponentCount())
			return;
		remove(idx * 2);
		add(c, idx * 2);
		invalidate();
		validate();

	}

	public void swapSplitComponents(int i, int j) {
		if ((i < 0 || i >= getSplitComponentCount()) || (j < 0 || j >= getSplitComponentCount()))
			return;
		spl.swapComponents(i, j);
		invalidate();
		validate();
	}

	public void removeSplitComponent(int idx) {
		spl.removeComponent(idx);
	}

	public void forceEqualizeParts() {
		spl.forceEqualParts();
	}

	public Component getSplitComponent(int idx) {
		return getComponent(idx * 2);
	}

	public int getComponentIndex(Component c) {
		for (int i = 0; i < getComponentCount(); i += 2)
			if (getComponent(i) == c)
				return i / 2;
		return -1;
	}

	public int getSplitComponentCount() {
		return (getComponentCount() + 1) / 2;
	}
}
