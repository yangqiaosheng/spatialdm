package spade.lib.basicwin;

import java.awt.Component;
import java.awt.Window;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 21-Dec-2006
 * Time: 13:58:37
 */
public class ActiveFoldablePanel extends FoldablePanel {
	public ActiveFoldablePanel() {
		super();
	}

	public ActiveFoldablePanel(Component content) {
		super(content);
	}

	public ActiveFoldablePanel(Component content, Component caption) {
		super(content, caption);
	}

	@Override
	public void open() {
		if (isOpen || content == null)
			return;
		super.open();
		if (isOpen) {
			Window win = CManager.getWindow(this);
			if (win != null) {
				win.pack();
			}
		}
	}

	@Override
	public void close() {
		if (!isOpen)
			return;
		super.close();
		if (!isOpen) {
			Window win = CManager.getWindow(this);
			if (win != null) {
				win.pack();
			}
		}
	}
}
