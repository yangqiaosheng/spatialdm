package spade.lib.basicwin;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class FoldablePanel extends Panel implements ActionListener {
	protected static int trW = 8, trH = 4;

	protected Component content = null, caption = null;
	protected Panel op = null, cp = null;
	protected ActionCanvas oac = null;
	protected boolean isOpen = false, isEnabled = true;
	protected ActionSupport asup = null;

	public FoldablePanel() {
		init();
	}

	public FoldablePanel(Component content) {
		setContent(content);
		init();
	}

	public FoldablePanel(Component content, Component caption) {
		setContent(content);
		setCaption(caption);
		init();
	}

	public void addActionListener(ActionListener listener) {
		if (listener == null)
			return;
		if (asup == null) {
			asup = new ActionSupport();
		}
		asup.addActionListener(listener);
	}

	protected void init() {
		setLayout(new ColumnLayout());
		TriangleDrawer td = new TriangleDrawer(TriangleDrawer.S);
		td.setPreferredSize(trW, trH);
		td.setMargins(3, 1);
		oac = new ActionCanvas(td);
		oac.addActionListener(this);
		oac.setActionCommand("open");
		op = new Panel(new BorderLayout());
		op.add(oac, "West");
		//op.add(oac,"East");
		if (caption != null) {
			op.add(caption, "Center");
		}
		td = new TriangleDrawer(TriangleDrawer.N);
		td.setPreferredSize(trW, trH);
		td.setMargins(3, 1);
		ActionCanvas ac = new ActionCanvas(td);
		ac.addActionListener(this);
		ac.setActionCommand("close");
		cp = new Panel(new BorderLayout());
		cp.add(ac, "East");
		add(op);
	}

	public void setContent(Component c) {
		content = c;
	}

	public void setCaption(Component c) {
		caption = c;
	}

	public void open() {
		if (isOpen || content == null || !isEnabled)
			return;
		boolean wasVisible = isShowing();
		if (wasVisible) {
			setVisible(false);
		}
		if (caption == null) {
			remove(op);
		} else {
			op.remove(oac);
		}
		add(content);
		add(cp);
		isOpen = true;
		if (wasVisible) {
			setVisible(true);
			CManager.validateAll(content);
			CManager.scrollToExpose(this);
		}
	}

	public void close() {
		if (!isOpen)
			return;
		boolean wasVisible = isShowing();
		if (wasVisible) {
			setVisible(false);
		}
		remove(cp);
		if (content != null) {
			remove(content);
		}
		if (caption == null) {
			add(op);
		} else {
			op.add(oac, "West");
		}
		//else op.add(oac,"East");
		isOpen = false;
		if (wasVisible) {
			setVisible(true);
			CManager.validateAll(this);
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if (cmd == null)
			return;
		if (cmd.equals("open")) {
			open();
		} else if (cmd.equals("close")) {
			close();
		}
		if (asup != null) {
			asup.fireActionEvent(this, cmd);
		}
	}

	@Override
	public void setEnabled(boolean value) {
		if (isOpen && !value) {
			close();
		}
		isEnabled = value;
		oac.setEnabled(value);
	}
}