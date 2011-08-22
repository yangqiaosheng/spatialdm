package spade.lib.basicwin;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.List;
import java.awt.Point;
import java.awt.ScrollPane;
import java.awt.Toolkit;
import java.awt.Window;

public class PopupWindow extends Window {
	public static Color bgColor = Color.getHSBColor(0.17f, 0.2f, 0.999f);
	public static Dimension scrDimension = Toolkit.getDefaultToolkit().getScreenSize();

	protected Component content = null; // Arbitrary content of the popup
	/**
	* The shift of the window relative to the cursor position
	*/
	protected int shiftX = 11, shiftY = 21;
	/**
	* The size increments for better display of a scrollpane or list
	*/
	protected int incrX = 0, incrY = 0;
	/**
	* position of popup window on the screen
	*/
	public Point pos = new Point();

	public PopupWindow(Frame parent) {
		super(parent);
		super.setLocation(0, 0);
		if (pos == null) {
			pos = new Point();
		}
		setBackground(bgColor);
		//setEnabled(false);
	}

	public void setOffset(int x_offset, int y_offset) {
		shiftX = x_offset;
		shiftY = y_offset;
	}

	public void setCursorMetrics(Cursor cursor) {
		if (cursor == null)
			//System.out.println("No cursor defined for the component");
			return;
		int cursor_type = cursor.getType();
		switch (cursor_type) {
		case Cursor.CROSSHAIR_CURSOR: {
			setOffset(5, 10);
			break;
		}
		case Cursor.TEXT_CURSOR: {
			setOffset(1, 21);
			break;
		}
		case Cursor.DEFAULT_CURSOR: {
			setOffset(11, 21);
			break;
		}
		default: {
			setOffset(11, 21);
		}
		}
		//System.out.println("Cursor's metrics successfully defined");
	}

	/**
	*  This function sets the location of popup window on the screen
	*  It depends from different situations with mouse pointer position
	*  on the screen. The function defines the place of location the window:
	*  under-right|under-left|over-right|over-left mouse, or another point.
	*/
	public void setPosition() {
		int x = pos.x + shiftX, y = pos.y + shiftY;
		Dimension d = getPreferredSize();
		int right = x + d.width, bottom = y + d.height;
		if (bottom >= scrDimension.height) {
			y -= d.height;
		}
		if (right >= scrDimension.width) {
			x -= d.width;
		}
		if (x < 0) {
			x = 0;
		}
		if (y < 0) {
			y = 0;
		}
		super.setLocation(x, y);
	}

	@Override
	public void setLocation(int x, int y) {
		if (pos == null) {
			pos = new Point();
		}
		pos.setLocation(x, y);
	}

	@Override
	public void show() {
		super.show();
		int x = super.getLocation().x, y = super.getLocation().y;
		Dimension d = getSize();
		int right = x + d.width, bottom = y + d.height;
		if (bottom >= scrDimension.height) {
			y -= d.height;
		}
		if (right >= scrDimension.width) {
			x -= d.width;
		}
		if (x < 0) {
			x = 0;
		}
		if (y < 0) {
			y = 0;
		}
		super.setLocation(x, y);
	}

	public Point getLastMousePosition() {
		return pos;
	}

	/* This function sets the content of popup window:
	*  it can be any Component like Panel, Canvas, etc.
	*/
	public boolean setContent(Component cInside) {
		if (content == null)
			if (cInside == null)
				return false;
			else {
				;
			}
		else if (content.equals(cInside))
			return true;
		setVisible(false);
		removeAll();
		content = cInside;
		incrX = 0;
		incrY = 0;
		if (cInside == null)
			return false;
		add(cInside, "Center");
		cInside.setBackground(bgColor);
		pack();
		Dimension size = getSize();
		incrX = 2;
		incrY = 2;
		if (size != null)
			if (content instanceof ScrollPane) {
				ScrollPane scp = (ScrollPane) content;
				Dimension d = scp.getViewportSize();
				if (d.height < scp.getVAdjustable().getMaximum()) {
					incrX = scp.getVScrollbarWidth() + 2;
				}
				if (d.width < scp.getHAdjustable().getMaximum()) {
					incrY = scp.getHScrollbarHeight() + 2;
				}
			} else if (content instanceof List) {
				List lst = (List) content;
				Dimension d = lst.getSize(), dpref = lst.getPreferredSize();
				if (lst.getItemCount() > lst.getRows()) {
					incrX = 20;
				}
				if (dpref.width > d.width) {
					incrY = 40;
				}
			}
		if (size != null) {
			setSize(size.width + incrX, size.height + incrY);
		}
		return true;
	}

	public boolean setText(String t) {
		if (t == null)
			return setContent(null);
		TextCanvas textInside = new TextCanvas();
		textInside.setText(t);
		return setContent(textInside);
	}

	/* Returns the content of popup window:
	*  it can be any Component like Panel, Canvas, etc.
	*/
	public Component getContent() {
		return content;
	}

	public String getText() {
		if (content == null || !(content instanceof TextCanvas))
			return null;
		return ((TextCanvas) content).getText();
	}

	public boolean hasValidContent() {
		return content != null;
	}

	@Override
	public void validate() {
		if (incrX > 0 || incrY > 0) {
			Dimension dpref = getPreferredSize();
			if (dpref != null) {
				setSize(dpref.width + incrX, dpref.height + incrY);
			}
		}
		super.validate();
	}

}
