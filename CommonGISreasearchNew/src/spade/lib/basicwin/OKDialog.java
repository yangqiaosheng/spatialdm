package spade.lib.basicwin;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Panel;
import java.awt.Point;
import java.awt.ScrollPane;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ResourceBundle;

import spade.lib.lang.Language;

public class OKDialog extends Dialog implements ActionListener, WindowListener {
//  static ResourceBundle res = ResourceBundle.getBundle("spade.lib.basicwin.Res", Language.lang);
	static ResourceBundle res = Language.getTextResource("spade.lib.basicwin.Res");
	public static final int OK_CANCEL_MODE = 1, YES_NO_MODE = 2;

	protected int mode = OK_CANCEL_MODE;

	protected boolean cancelled = false, backPressed = false;
	protected NotificationLine lStatus = null;
	protected DialogContent content = null;
	protected int minWidth = 200, minHeight = 100;
	protected Component component = null;
	protected ScrollPane sp = null;

	public OKDialog(boolean modal, Frame owner, String title, boolean hasCancelButton) {
		this(owner, title, OK_CANCEL_MODE, hasCancelButton, false, modal);
	}

	public OKDialog(Frame owner, String title, int mode, boolean hasCancelButton, boolean hasBackButton, boolean m) {
		super(owner, title, m); // the dialog is modal
		PopupManager.hideWindow();
		setBackground(Color.lightGray);
		setLayout(new BorderLayout());

		Panel p = new Panel(new FlowLayout());
		// following text:"Yes":"OK"
		Button b = new Button((mode == YES_NO_MODE) ? res.getString("Yes") : res.getString("OK"));
		p.add(b);
		b.addActionListener(this);
		b.setActionCommand("OK");
		if (hasBackButton) {
			// following text:"Back"
			b = new Button(res.getString("Back"));
			p.add(b);
			b.addActionListener(this);
			b.setActionCommand("back");
		}
		if (hasCancelButton) {
			// following text:
			b = new Button((mode == YES_NO_MODE) ? res.getString("No") : res.getString("Cancel"));
			b.addActionListener(this);
			b.setActionCommand("cancel");
			Panel pp = new Panel(new BorderLayout());
			pp.add(p, BorderLayout.CENTER);
			p = new Panel(new FlowLayout(FlowLayout.CENTER));
			p.add(b);
			pp.add(p, BorderLayout.EAST);
			add(pp, BorderLayout.SOUTH);
		} else {
			add(p, BorderLayout.SOUTH);
		}
		addWindowListener(this);
	}

	public OKDialog(Frame owner, String title, boolean hasCancelButton) {
		this(owner, title, OK_CANCEL_MODE, hasCancelButton, false);
	}

	public OKDialog(Frame owner, String title, boolean hasCancelButton, boolean hasBackButton) {
		this(owner, title, OK_CANCEL_MODE, hasCancelButton, hasBackButton);
	}

	public OKDialog(Frame owner, String title, int mode, boolean hasCancelButton) {
		this(owner, title, mode, hasCancelButton, false);
	}

	public OKDialog(Frame owner, String title, int mode, boolean hasCancelButton, boolean hasBackButton) {
		super(owner, title, true); //the dialog is modal
		PopupManager.hideWindow();
		setBackground(Color.lightGray);
		setLayout(new BorderLayout());

		Panel p = new Panel(new FlowLayout());
		// following text:"Yes":"OK"
		Button b = new Button((mode == YES_NO_MODE) ? res.getString("Yes") : res.getString("OK"));
		p.add(b);
		b.addActionListener(this);
		b.setActionCommand("OK");
		if (hasBackButton) {
			// following text:"Back"
			b = new Button(res.getString("Back"));
			p.add(b);
			b.addActionListener(this);
			b.setActionCommand("back");
		}
		if (hasCancelButton) {
			// following text:
			b = new Button((mode == YES_NO_MODE) ? res.getString("No") : res.getString("Cancel"));
			b.addActionListener(this);
			b.setActionCommand("cancel");
			Panel pp = new Panel(new BorderLayout());
			pp.add(p, BorderLayout.CENTER);
			p = new Panel(new FlowLayout(FlowLayout.CENTER));
			p.add(b);
			pp.add(p, BorderLayout.EAST);
			add(pp, BorderLayout.SOUTH);
		} else {
			add(p, BorderLayout.SOUTH);
		}
		addWindowListener(this);
	}

	protected DialogContent getDialogContent(Component c) {
		if (c == null)
			return null;
		if (c instanceof DialogContent)
			return (DialogContent) c;
		if (c instanceof Container) {
			Container cont = (Container) c;
			DialogContent dc = null;
			for (int i = 0; i < cont.getComponentCount() && dc == null; i++) {
				dc = getDialogContent(cont.getComponent(i));
			}
			return dc;
		}
		return null;
	}

	public void addContent(Component c) {
		if (component != null) {
			remove(component);
		}
		if (sp != null) {
			remove(sp);
		}
		component = c;
		add(c, BorderLayout.CENTER);
		content = getDialogContent(c);
		if (content != null) {
			getStatusLine();
			if (content instanceof NotificationLineManager) {
				((NotificationLineManager) content).setNotificationLine(lStatus);
			}
		}
	}

	public NotificationLine getStatusLine() {
		if (lStatus == null) {
			lStatus = new NotificationLine("");
			add(lStatus, BorderLayout.NORTH);
		}
		return lStatus;
	}

	public void setMinimumWidth(int width) {
		minWidth = width;
	}

	public void setMinimumHeight(int height) {
		minHeight = height;
	}

	@Override
	public void show() {
		show(null);
	}

	public void show(Point location) {
		if (lStatus != null) {
			lStatus.showMessage(null, false);
		}
		pack();
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension sz = getSize();
		sz.width += 10;
		sz.height += 10;
		if (component != null && (sz.width > d.width * 2 / 3 || sz.height > d.height * 2 / 3)) {
			remove(component);
			sp = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
			sp.add(component);
			add(sp, BorderLayout.CENTER);
		} else {
			sp = null;
		}

		int w = Metrics.stringWidth(getTitle());
		if (w > 0 && sz.width < w + 80) {
			sz.width = w + 80;
		}
		if (sz.width < minWidth) {
			sz.width = minWidth;
		}
		if (sz.height < minHeight) {
			sz.height = minHeight;
		}
		if (sz.width > d.width * 2 / 3) {
			sz.width = d.width * 2 / 3;
		}
		if (sz.height > d.height * 2 / 3) {
			sz.height = d.height * 2 / 3;
		}

		setSize(sz.width, sz.height);

		if (location == null) {
			setLocation((d.width - sz.width) / 2, (d.height - sz.height) / 2);
		} else {
			if (location.x + sz.width > d.width) {
				location.x = d.width - sz.width;
			}
			if (location.y + sz.height > d.height) {
				location.y = d.height - sz.height;
			}
			setLocation(location);
		}
		super.show();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if (cmd.equals("cancel")) {
			cancelled = true;
			backPressed = false;
			dispose();
			return;
		}
		if (cmd.equals("back")) {
			cancelled = false;
			backPressed = true;
			dispose();
			return;
		}
		if (content != null && !content.canClose()) {
			if (lStatus != null) {
				lStatus.showMessage(content.getErrorMessage(), true);
			}
			return;
		}
		cancelled = false;
		backPressed = false;
		dispose();
	}

	public boolean wasCancelled() {
		return cancelled;
	}

	public boolean wasBackPressed() {
		return backPressed;
	}

	@Override
	public void windowClosing(WindowEvent evt) {
		cancelled = true;
		dispose();
	}

	@Override
	public void windowActivated(WindowEvent evt) {
	}

	@Override
	public void windowDeactivated(WindowEvent evt) {
	}

	@Override
	public void windowOpened(WindowEvent evt) {
	}

	@Override
	public void windowClosed(WindowEvent evt) {
	}

	@Override
	public void windowIconified(WindowEvent evt) {
	}

	@Override
	public void windowDeiconified(WindowEvent evt) {
	}

}
