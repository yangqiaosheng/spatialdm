package spade.lib.basicwin;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Image;
import java.awt.Panel;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ResourceBundle;

import spade.lib.lang.Language;

public class PopupToolbar extends Panel implements ActionListener, MouseListener {

	//static ResourceBundle res = ResourceBundle.getBundle("spade.lib.basicwin.Res", Language.lang);
	static ResourceBundle res = Language.getTextResource("spade.lib.basicwin.Res");
	protected Component master = null; // Component with popup toolbar
	protected Component toolbar = null; // Content of the popup toolbar, usually TImgButtonGroup

	protected TImgButtonGroup bgr = null;
	protected TImgButton tib = null;

	protected Image img = null;

	protected Point pLoc = null; // Component's location on the screen
	protected int masterH = 0; // Component's height
	protected PopupWindow pw = null; // popup window has been managed by.
	protected Cursor cursor = null; // Cursor of the component with popup window

	public boolean isVertical = false;

	public PopupToolbar(Component attachedTo, Component toolbar) {
		super();
		setLayout(new BorderLayout());
		this.toolbar = toolbar;
		if (toolbar != null && toolbar instanceof TImgButtonGroup) {
			bgr = (TImgButtonGroup) toolbar;
			bgr.addActionListener(this);
			if (bgr.getSelectedButton() != null) {
				img = bgr.getSelectedButton().getIcon();
			}
			if (img != null) {
				tib = new TImgButton(img);
			}
			if (tib != null) {
				add(tib, "Center");
				//tib.addActionListener(this);
			}
		}
		attachTo(attachedTo);
	}

	/*public PopupToolbar (Component attachedTo, TImgButtonGroup toolbar) {
	  super();
	  setLayout(new BorderLayout());
	  if (toolbar!=null)
	    toolbar.addActionListener(this);
	  this.toolbar=toolbar;
	  attachTo(attachedTo);
	} */
	public PopupToolbar(TImgButtonGroup toolbar) {
		super();
		setLayout(new BorderLayout());
		if (toolbar != null) {
			toolbar.addActionListener(this);
		}
		this.toolbar = toolbar;
		bgr = toolbar;
		if (bgr.getSelectedButton() != null) {
			img = bgr.getSelectedButton().getIcon();
		}
		if (img != null) {
			tib = new TImgButton(img);
		}
		if (tib != null) {
			add(tib, "Center");
			tib.addActionListener(this);
			// following text: "Click to choose another option"
			new PopupManager(tib, res.getString("Click_to_choose"), true);
		}
		attachTo(tib);
	}

	public void showToolbar() {
		if (pw == null) {
			System.out.println("Cannot show toolbar. Popup window is undefined.");
			return;
		}
		pw.setVisible(true);
		pw.toFront();
	}

	public void hideToolbar() {
		if (pw == null)
			return;
		pw.setVisible(false);
	}

	//--- Implementation of MouseListener Interface
	@Override
	public void mouseClicked(MouseEvent me) {
		if (master == null)
			return;
		if (pw == null) {
			pw = new PopupWindow(CManager.getAnyFrame());
			pw.setContent(toolbar);
			System.out.println("Popup window initialized.");

			//      pw.setCursorMetrics(cursor);
		}
		if (pw != null) {
			pw.pack();
			if (master != null) {
				pLoc = master.getLocationOnScreen();
				masterH = master.getSize().height;
				pw.setLocation(pLoc.x, pLoc.y + masterH + 2);
			} else {
				pw.setLocation(pLoc.x, pLoc.y);
			}
			pw.setOffset(0, 0);
			pw.setBackground(Color.gray);
			pw.setEnabled(true);
			pw.setPosition();
		}
		if (pw != null && pw.hasValidContent()) {
			if (pw.isShowing()) {
				hideToolbar();
			} else {
				showToolbar();
			}
		}
	}

	@Override
	public void mousePressed(MouseEvent me) {
	}

	@Override
	public void mouseReleased(MouseEvent me) {
	}

	@Override
	public void mouseEntered(MouseEvent me) {
	}

	@Override
	public void mouseExited(MouseEvent me) {
	}

	// ~~MouseListener Interface

	@Override
	public void actionPerformed(ActionEvent e) {
		if (bgr != null) {
			TImgButton b = bgr.getSelectedButton();
			if (b != null) {
				img = b.getIcon();
				if (tib != null) {
					tib.setIcon(img);
				}
			}
		}
		hideToolbar();
	}

	/* This function sets the toolbar of popup window:
	*  it can be any Component like Panel, Canvas, etc.
	*/
	public boolean setToolbar(Component c) {
		if (pw != null) {
			boolean wasShown = pw.isShowing();
			if (wasShown) {
				hideToolbar();
			}
			pw.setContent(c);
			if (wasShown) {
				showToolbar();
			}
		}
		toolbar = c;
		if (toolbar != null && toolbar instanceof TImgButtonGroup) {
			((TImgButtonGroup) toolbar).addActionListener(this);
		}
		return false;
	}

	@Override
	public void setLocation(int x, int y) {
		//super.setLocation(x,y);
		if (pLoc == null) {
			pLoc = new Point(x, y);
			return;
		}
		pLoc.setLocation(x, y);
	}

	public boolean attachTo(Component comp) {
		if (comp == null) {
			System.out.println("Cannot attach. Component is invalid");
			return false;
		}
		if (toolbar == null) {
			System.out.println("Cannot attach. toolbar has been not defined");
			return false;
		}
		master = comp;
		master.addMouseListener(this);
//    cursor=master.getCursor();
//    if (cursor!=null) System.out.println(cursor.toString());
		return true;
	}

	public void setVertical(boolean flag) {
		isVertical = flag;
	}
}
