package spade.lib.basicwin;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Panel;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ResourceBundle;

import spade.lib.lang.Language;

/**
* Starts a non-modal dialog, i.e. a frame with OK, Cancel, and, possibly, Back
* buttons and arbitrary content.
*/
public class OKFrame extends Frame implements ActionListener, WindowListener {
	/**
	* The owner of the Frame that waits for a notification about the frame being
	* finished
	*/
	//static ResourceBundle res = ResourceBundle.getBundle("spade.lib.basicwin.Res", Language.lang);
	static ResourceBundle res = Language.getTextResource("spade.lib.basicwin.Res");
	protected ActionListener owner = null;

	protected boolean cancelled = false, backPressed = false;
	protected NotificationLine lStatus = null;
	protected DialogContent content = null;
	protected Component mainComponent = null;
	protected boolean finished = false;

	public OKFrame(ActionListener owner, String title, boolean hasCancelButton) {
		this(owner, title, hasCancelButton, false);
	}

	public OKFrame(ActionListener owner, String title, boolean hasCancelButton, boolean hasBackButton) {
		super(title);
		PopupManager.hideWindow();
		this.owner = owner;
		setBackground(Color.lightGray);
		setLayout(new BorderLayout());

		Panel p = new Panel(new FlowLayout());

		Button b = new Button(res.getString("OK"));
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
			// following text:"Cancel"
			b = new Button(res.getString("Cancel"));
			b.addActionListener(this);
			b.setActionCommand("cancel");
			Panel pp = new Panel(new BorderLayout());
			pp.add(p, "Center");
			p = new Panel(new FlowLayout(FlowLayout.CENTER));
			p.add(b);
			pp.add(p, "East");
			add(pp, "South");
		} else {
			add(p, "South");
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
		if (c == null)
			return;
		mainComponent = c;
		add(mainComponent, "Center");
		content = getDialogContent(mainComponent);
		if (content != null) {
			lStatus = new NotificationLine("");
			add(lStatus, "North");
		}
	}

	public Component getMainComponent() {
		return mainComponent;
	}

	public void start() {
		pack();
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension sz = getSize();
		setLocation((d.width - sz.width) / 2, (d.height - sz.height) / 2);
		super.show();
	}

	public void close() {
		finished = true;
		if (owner != null) {
			owner.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "closed"));
		}
		dispose();
		CManager.destroyComponent(this);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if (cmd.equals("cancel")) {
			cancelled = true;
		} else if (cmd.equals("back")) {
			backPressed = true;
		} else if (cmd.equals("OK")) {
			if (content != null && !content.canClose()) {
				lStatus.showMessage(content.getErrorMessage(), true);
				return;
			}
		}
		close();
	}

	public boolean wasCancelled() {
		return cancelled;
	}

	public boolean wasBackPressed() {
		return backPressed;
	}

	public boolean isFinished() {
		return finished;
	}

	@Override
	public void windowClosing(WindowEvent evt) {
		cancelled = true;
		close();
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
