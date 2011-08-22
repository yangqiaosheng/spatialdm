//======================= TImgButton.java =======================
//
// Copyright 1998, all rights reserved.
// Professional GEO Systems BV.
//
// Changed nl.pgs.lava.javaui.ExtButton.java by
// GMD, AiS.KD, 22/09/2000, 15/10/2000, 17/10/2000
// with permission of Professional GEO Systems BV.

package spade.lib.basicwin;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

//import spade.lib.basicwin.PopupWindow;

public class TImgButton extends Canvas implements MouseListener {

	public static final int MODE_CLICK = 1;
	public static final int MODE_SELECT = 2;

	public static final int BW_OUTSIDE = 0;
	public static final int BW_BORDER = 4;
	public static final int BW_INSIDE = 0;
	public static final int BW_TOTAL = BW_OUTSIDE + BW_BORDER + BW_INSIDE;
	public static boolean isApplication = true;

	protected boolean isGridCell;
	protected ActionListener parentGrid; //listens to button pressing events
	//when the button acts as radiobutton

	protected int showType;
	public final int ST_TEXT = 1;
	public final int ST_IMAGE = 2;

	protected boolean highlighted;
	protected boolean enabled = true;

	protected int mode;

	protected String actionCmd = "X";
	protected ActionSupport asup = null;

	protected String text;
	protected Drawer imgDrawer = null;

	protected int labelWidth, labelHeight;

	public TImgButton() {
		init("");
		addMouseListener(this);
	}

	public TImgButton(String label, boolean dummy) {
		init(label);
		addMouseListener(this);
	}

	public TImgButton(String URLspec) {
		useImageFromURL(URLspec);
		addMouseListener(this);
	}

	public TImgButton(Drawer iconDrawer) {
		init(iconDrawer);
		addMouseListener(this);
	}

	public TImgButton(Image icon) {
		init(icon);
		if (isShowing()) {
			repaint();
		}
		addMouseListener(this);
	}

	public synchronized void addActionListener(ActionListener l) {
		if (l == null)
			return;
		if (asup == null) {
			asup = new ActionSupport();
		}
		asup.addActionListener(l);
	}

	/**
	* Get the status of this button: highlighted or not.
	*/
	public boolean getHighlight() {
		return highlighted;
	}

	public Image getIcon() {
		if ((imgDrawer != null) && (imgDrawer instanceof ImageDrawer))
			return ((ImageDrawer) imgDrawer).getImage();
		return null;
	}

	public void init(String text) {
		this.text = text;
		labelWidth = 40;
		labelHeight = 20;
		showType = ST_TEXT;
		mode = MODE_CLICK;
		setBackground(Color.lightGray);
	}

	public void init(Drawer drawer) {
		imgDrawer = drawer;
		labelWidth = imgDrawer.getIconSize().width;
		labelHeight = imgDrawer.getIconSize().height;
		showType = ST_IMAGE;
		mode = MODE_CLICK;
		setBackground(Color.lightGray);
	}

	public void init(Image icon) {
		imgDrawer = new ImageDrawer(icon);
		labelWidth = imgDrawer.getIconSize().width;
		labelHeight = imgDrawer.getIconSize().height;
		showType = ST_IMAGE;
		mode = MODE_CLICK;
		setBackground(Color.lightGray);
	}

	public Insets insets() {
		return new Insets(0, 0, 0, 0);
	}

	@Override
	public Dimension getMinimumSize() {
		return new Dimension(2 * BW_TOTAL + labelWidth, 2 * BW_TOTAL + labelHeight);
	}

	protected void sendActionEventToListeners() {
		if (asup != null) {
			asup.fireActionEvent(this, actionCmd);
		}
	}

	//--- Implementation of MouseListener Interface
	@Override
	public void mouseClicked(MouseEvent me) { /* mousePressed, mouseReleased */
	}

	@Override
	public void mousePressed(MouseEvent me) {
		if (!enabled)
			return;
		switch (mode) {
		case MODE_SELECT:
			// Send action event to parent
			Event e = new Event(this, Event.ACTION_EVENT, null);
			if (isGridCell)
				if (parentGrid != null) {
					ActionEvent ae = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, actionCmd);
					parentGrid.actionPerformed(ae);
				} else {
					sendActionEventToListeners();
				}
			break;

		case MODE_CLICK:
			setHighlight(true);
			break;
		}
	}

	@Override
	public void mouseReleased(MouseEvent me) {
		if (!enabled)
			return;
		switch (mode) {
		case MODE_SELECT:
			return;

		case MODE_CLICK:
			setHighlight(false);
			if (isGridCell && parentGrid != null) {
				ActionEvent ae = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, actionCmd);
				parentGrid.actionPerformed(ae);
			} else {
				sendActionEventToListeners();
			}
			break;
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	// ~~MouseListener Interface

	@Override
	public void paint(Graphics g) {
		Dimension d = getSize();

		// Draw label
		if (showType == ST_TEXT) {
			g.drawString(text, 10, d.height / 2);
		}
		if (showType == ST_IMAGE) {
			if (imgDrawer != null) {
				imgDrawer.draw(g, BW_TOTAL, BW_TOTAL);
			} else {
				int tx = BW_OUTSIDE + 1;
				int ty = BW_OUTSIDE + 1;
				int tw = labelWidth - 2 * BW_OUTSIDE;
				int th = labelHeight - 2 * BW_OUTSIDE;
				g.setColor(Color.black);
				g.drawLine(tx, ty, tx + tw, ty + th);
				g.drawLine(tx, ty + th, tx + tw, ty);
			}
		}

		// Set border
		g.setColor(Color.lightGray);
		int tx = BW_OUTSIDE;
		int ty = BW_OUTSIDE;
		int tw = d.width - 2 * BW_OUTSIDE - 1;
		int th = d.height - 2 * BW_OUTSIDE - 1;
		g.draw3DRect(tx, ty, tw, th, !highlighted);
		g.draw3DRect(tx + 1, ty + 1, tw - 2, th - 2, !highlighted);
	}

	@Override
	public Dimension getPreferredSize() {
		return getMinimumSize();
	}

	@Override
	public void repaint() {
		if (isGridCell) {
			//parentGrid.paintChild(this);
			super.repaint();
		} else {
			super.repaint();
		}
	}

	public void setGridCell(boolean isGridCell) {
		this.isGridCell = isGridCell;
	}

	/**
	* Set this button to (non) highlighted, only possible
	* when in MODE_SELECT mode.
	*/
	public void setHighlight(boolean newHighlight) {
		highlighted = newHighlight;
		repaint();
	}

	/**
	* Set the mode of this button to MODE_CLICK (an ordinary
	* button) or MODE_SELECT (on/off).
	*/
	public void setMode(int newMode) {
		mode = newMode;
	}

	public void setParentGrid(ActionListener parentGrid) {
		this.parentGrid = parentGrid;
	}

	public boolean setIcon(Image img) {
		if (imgDrawer == null || img == null)
			return false;
		init(img);
		if (isShowing()) {
			repaint();
		}
		return true;
	}

	@Override
	public void setEnabled(boolean enabled) {
		//super.setEnabled(enabled); // to allow tooltips for disabled buttons
		this.enabled = enabled;
		if (imgDrawer != null) {
			imgDrawer.setDrawEnabled(enabled);
			if (isShowing()) {
				repaint();
			}
		}
	}

	public boolean isButtonEnabled() {
		return enabled;
	}

	public boolean useImageFromURL(String URLSpec) {
		init(new ImageDrawer(URLSpec, this));
		if (isShowing()) {
			repaint();
		}
		return true;
	}

	public void setActionCommand(String cmd) {
		actionCmd = cmd;
	}

	public String getActionCommand() {
		return actionCmd;
	}
}
