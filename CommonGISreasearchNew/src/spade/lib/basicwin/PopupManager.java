package spade.lib.basicwin;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class PopupManager implements MouseListener, MouseMotionListener {

	protected Component master = null; // Component with popup window
	protected Component content = null; // Arbitrary content of the popup

	protected Point pLoc = null; // Component's location on the screen
	protected boolean useDelay = true, mouseIsPressed = false, keepHidden = false;
	protected static PopupWindow pw = null; // popup window has been managed by.
	protected static MouseHandlerThread mh = null;
	protected Cursor cursor = null; // Cursor of the component with popup window
	/**
	* Allows or disallows breaking long text lines
	*/
	public boolean breakLines = true;
	/**
	* Allow activate only for active windows (like tooltips)
	*/
	public boolean onlyForActiveWindow = true;

	public void setOnlyForActiveWindow(boolean onlyForActiveWindow) {
		this.onlyForActiveWindow = onlyForActiveWindow;
	}

	public static int mouseSensitivity = Math.round(2.0f * java.awt.Toolkit.getDefaultToolkit().getScreenResolution() / 25.33f);

	public PopupManager(Component attachedTo, String text, boolean useDelay) {
		if (text != null) {
			TextCanvas tc = new TextCanvas();
			tc.setText(text);
			tc.setMayBreakLines(breakLines);
			content = tc;
		}
		attachTo(attachedTo);
		this.useDelay = useDelay;
	}

	public PopupManager(Component attachedTo, Component content, boolean useDelay) {
		this.content = content;
		attachTo(attachedTo);
		this.useDelay = useDelay;
	}

	/**
	* Allows or disallows to break lines (by default breaking is allowed)
	*/
	public void setMayBreakLines(boolean value) {
		breakLines = value;
		if (content != null && (content instanceof TextCanvas)) {
			((TextCanvas) content).setMayBreakLines(value);
		}
	}

	public void setKeepHidden(boolean value) {
		keepHidden = value;
	}

	public boolean getKeepHidden() {
		return keepHidden;
	}

	protected void showWindow() {
		//System.out.println("Show <"+((content==null)?"null":((TextCanvas)content).getText())+">");
		if (keepHidden || pw == null)
			return;
		if (pw.isShowing())
			return;
		if (master == null || !master.isShowing())
			return;
		if (!pw.hasValidContent())
			return;
		content.invalidate();
		pw.invalidate();
		pw.pack();
		if (!useDelay) {
			pw.show();
			pw.toFront();
		} else {
			mh = new MouseHandlerThread(pw);
			mh.start();
		}
		//System.out.println("Started the thread");
	}

	public static void hideWindow() {
		if (pw == null)
			return;
		//System.out.println("Hide <"+((content==null)?"null":((TextCanvas)content).getText())+">");
		if (mh != null && !mh.finished) {
			//System.out.println("Stop the thread!");
			synchronized (mh) {
				mh.mouseRelocated = true;
			}
		}
		mh = null;
		synchronized (pw) {
			if (pw.isShowing()) {
				pw.dispose();
			}
		}
		//System.out.println("Window hidden");
	}

	public void startShow(int mouseX, int mouseY) {
		if (pw != null && pw.isShowing())
			return;
		if (mh != null && !mh.finished)
			return;
		if (content == null)
			return;
		if (!master.isShowing())
			return;
		Frame f = CManager.getFrame(master);
		if (f == null) {
			f = CManager.getDummyFrame();
		} else if (onlyForActiveWindow && f.getTitle() != null && f.getTitle().length() > 0 && f.getFocusOwner() == null)
			return; //not active
		if (pw == null) {
			pw = new PopupWindow(f);
			//pw.setEnabled(false);
		}
		if (!content.equals(pw.getContent())) {
			pw.setContent(content);
		}
		try {
			pLoc = master.getLocationOnScreen();
		} catch (Exception e) {
			pw = null;
			return;
		}
		pw.setLocation(pLoc.x + mouseX, pLoc.y + mouseY);
		pw.setPosition();
		showWindow();
	}

	//--- Implementation of MouseMotionListener Interface
	@Override
	public void mouseDragged(MouseEvent me) {
	}

	@Override
	public void mouseMoved(MouseEvent me) { // relocate tooltip if visible
		if (keepHidden || mouseIsPressed)
			return;
		if (master == null || !master.isShowing())
			return;
		if (pw == null)
			return;
		Point pos = null, pLoc = null;
		try {
			pos = pw.getLastMousePosition();
			pLoc = master.getLocationOnScreen();
		} catch (Exception e) {
			if (pw.isShowing()) {
				hideWindow();
			}
			return;
		}
		int x = me.getX() + pLoc.x, y = me.getY() + pLoc.y;
		if (pw.isShowing() && pos.x - x > mouseSensitivity || pos.y - y > mouseSensitivity) {
			hideWindow();
		}
		pw.setLocation(x, y);
		pw.setPosition();
	}

	// ~~MouseMotionListener Interface

	//--- Implementation of MouseListener Interface
	@Override
	public void mouseClicked(MouseEvent me) {
	}

	@Override
	public void mousePressed(MouseEvent me) {
		mouseIsPressed = keepHidden = true;
		hideWindow();
	}

	@Override
	public void mouseReleased(MouseEvent me) {
		mouseIsPressed = keepHidden = false;
	}

	@Override
	public void mouseEntered(MouseEvent me) { // need to show tooltip
		//System.out.println("Mouse entered");
		hideWindow();
		mouseIsPressed = keepHidden = false;
		if (content == null)
			return;
		if (master == null || !master.isShowing())
			return;
		startShow(me.getX(), me.getY());
	}

	@Override
	public void mouseExited(MouseEvent me) { // hide tooltip if it is visible
		//System.out.println("Mouse exited");
		hideWindow();
		pw = null;
		mouseIsPressed = keepHidden = false;
	}

	// ~~MouseListener Interface

	/* This function sets the content of popup window:
	*  it can be any Component like Panel, Canvas, etc.
	*/
	public void setContent(Component c) {
		content = c;
		if (pw != null) {
			boolean wasShown = pw.isShowing();
			if (wasShown) {
				hideWindow();
			}
			pw.setContent(content);
			pw.pack();
			if (wasShown && content != null) {
				showWindow();
			}
		}
	}

	public void setText(String t) {
		if (t == null) {
			setContent(null);
			return;
		}
		TextCanvas tc = null;
		if (content == null || !(content instanceof TextCanvas)) {
			tc = new TextCanvas();
		} else {
			tc = (TextCanvas) content;
		}
		tc.setText(t);
		tc.setMayBreakLines(breakLines);
		tc.invalidate();
		setContent(tc);
	}

	public boolean attachTo(Component comp) {
		if (comp == null) {
			System.out.println("Cannot attach. Component is invalid");
			return false;
		}
		if (content == null) {
			System.out.println("Cannot attach. Content of popup window has been not specified");
			return false;
		}
		master = comp;
		master.addMouseListener(this);
		master.addMouseMotionListener(this);
//    cursor=master.getCursor();
//    if (cursor!=null) System.out.println(cursor.toString());
		return true;
	}

	public void setUseDelay(boolean useDelay) {
		this.useDelay = useDelay;
	}
}
