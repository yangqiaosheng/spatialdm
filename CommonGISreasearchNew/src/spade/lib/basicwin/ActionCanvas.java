package spade.lib.basicwin;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

public class ActionCanvas extends Canvas implements MouseListener, WindowListener {
	protected String actionCmd = "X";
	protected Drawer imgDrawer = null;
	public boolean stretchIcon = false;
	protected boolean isEnabled = true;
	protected Color origBkgColor = null;
	protected ActionSupport asup = null;
	protected Dimension minSize = null;

	public ActionCanvas(String imageURL) {
		imgDrawer = new ImageDrawer(imageURL, this);
	}

	public ActionCanvas(Drawer iconDrawer) {
		imgDrawer = iconDrawer;
	}

	public ActionCanvas(Image img) {
		imgDrawer = new ImageDrawer(img);
	}

	public void setImageDrawer(Drawer iconDrawer) {
		imgDrawer = iconDrawer;
	}

	public void setActionCommand(String cmd) {
		actionCmd = cmd;
	}

	public void setStretchIcon(boolean value) {
		stretchIcon = value;
	}

	@Override
	public void setEnabled(boolean value) {
		imgDrawer.setDrawEnabled(value);
		isEnabled = value;
		repaint();
	}

	@Override
	public void setMinimumSize(Dimension size) {
		if (size != null) {
			minSize = (Dimension) size.clone();
		}
		super.setMinimumSize(size);
	}

	@Override
	public Dimension getPreferredSize() {
		if (imgDrawer == null)
			if (minSize != null)
				return minSize;
			else
				return new Dimension(10, 10);
		Dimension size = imgDrawer.getIconSize();
		if (minSize == null)
			return size;
		if (size.width < minSize.width) {
			size.width = minSize.width;
		}
		if (size.height < minSize.height) {
			size.height = minSize.height;
		}
		return size;
	}

	public synchronized void addActionListener(ActionListener l) {
		if (l == null)
			return;
		if (asup == null) {
			asup = new ActionSupport();
			addMouseListener(this);
			Window w = CManager.getWindow(this);
			if (w == null) {
				w = CManager.getAnyFrame();
			}
			if (w != null) {
				w.addWindowListener(this);
			}
		}
		asup.addActionListener(l);
	}

	@Override
	public void paint(Graphics g) {
		Dimension d = getSize();
		imgDrawer.draw(g, 0, 0, d.width, d.height, stretchIcon);
	}

	protected void sendActionEventToListeners() {
		if (asup != null && isEnabled) {
			asup.fireActionEvent(this, actionCmd);
		}
	}

	@Override
	public void mousePressed(MouseEvent me) {
	}

	@Override
	public void mouseReleased(MouseEvent me) {
	}

	@Override
	public void mouseClicked(MouseEvent me) {
		if (origBkgColor != null) {
			setBackground(origBkgColor);
		}
		sendActionEventToListeners();
	}

	@Override
	public void mouseExited(MouseEvent me) {
		if (origBkgColor != null) {
			setBackground(origBkgColor);
		}
	}

	@Override
	public void mouseEntered(MouseEvent me) {
		if (origBkgColor == null) {
			origBkgColor = getBackground();
		}
		setBackground(Color.yellow);
	}

	@Override
	public void windowActivated(WindowEvent we) {
	}

	@Override
	public void windowOpened(WindowEvent we) {
	}

	@Override
	public void windowClosing(WindowEvent we) {
	}

	@Override
	public void windowClosed(WindowEvent we) {
	}

	@Override
	public void windowIconified(WindowEvent we) {
	}

	@Override
	public void windowDeiconified(WindowEvent we) {
	}

	@Override
	public void windowDeactivated(WindowEvent we) {
		if (origBkgColor != null) {
			setBackground(origBkgColor);
		}
	}

}