package spade.lib.basicwin;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.ScrollPane;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.StringTokenizer;
import java.util.Vector;

public class TextCanvas extends Canvas implements ComponentListener {
	/**
	* Preferred width and height. Used when the size of the text is not yet
	* available. The width is used for determining the size of the canvas
	* when it is not included in a scrollpane.
	*/
	protected int prefWidth = 150, prefHeight = 50;
	/**
	* The text to be displayed. Each string of the text is shown starting from a
	* new line.
	*/
	protected Vector lines = null;
	/**
	* The color of the text to be displayed. Each string of the text has
	* same color.
	*/
	protected Color textColor = null;
	/**
	* Current width and height needed to place the text
	*/
	protected int w = 0, h = 0;
	/**
	* Left margin
	*/
	protected int marg = 3;
	/**
	* Used to count preferred sizes.
	*/
	protected Image offScreenImage = null;
	/**
	* Last size of the canvas
	*/
	protected Dimension lastSize = null;
	/**
	* Used for debugging
	*/
	//public boolean debug=false;

	public boolean drawFrame = false, toBeCentered = false, breakLines = true;
	/**
	* Indicates if this TextCanvas has been already registered as a listener of
	* the ScrollPane it is included into (if any)
	*/
	protected boolean listensToScrollPaneResizing = false;
	protected boolean hasNoScrollPane = false, hasNoColumnContainer = false;

	public TextCanvas() {
		addComponentListener(this);
	}

	/**
	* Adds a string to the text to be displayed. Each string is shown starting
	* from a new line.
	*/
	public void addTextLine(String line) {
		if (line == null)
			return;
		if (lines == null) {
			lines = new Vector(10, 10);
		}
		if (line.indexOf('\n') < 0) {
			lines.addElement(line);
		} else {
			StringTokenizer st = new StringTokenizer(line, "\r\n");
			while (st.hasMoreTokens()) {
				String str = st.nextToken();
				if (str != null) {
					lines.addElement(str.trim());
				}
			}
		}
		int len = line.length();
		if (len > 100) {
			prefWidth = 400;
		} else if (len > 75) {
			prefWidth = 300;
		} else if (len > 50) {
			prefWidth = 200;
		}
		if (isShowing()) {
			regulateSize();
		}
	}

	public void addTextLine(String line, Color c) {
		addTextLine(line);
		textColor = c;
	}

	/**
	* Removes all the strings
	*/
	public void clear() {
		if (lines != null) {
			lines.removeAllElements();
		}
		w = h = 0;
		if (isShowing()) {
			regulateSize();
		}
	}

	/**
	* Removes all previous lines and adds the new text
	*/
	public void setText(String text) {
		if (lines != null) {
			lines.removeAllElements();
		}
		addTextLine(text);
		w = h = 0;
	}

	public void setText(String text, Color c) {
		setText(text);
		textColor = c;
	}

	public void setTextColor(Color c) {
		textColor = c;
	}

	public boolean hasText() {
		return (lines != null && !lines.isEmpty());
	}

	public String getText() {
		if (!hasText())
			return null;
		StringBuffer sb = new StringBuffer(lines.elementAt(0).toString());
		for (int i = 1; i < lines.size(); i++) {
			sb.append(" ").append(lines.elementAt(i).toString());
		}
		return sb.toString();
	}

	public String getTextWithLineBreaks() {
		if (!hasText())
			return null;
		StringBuffer sb = new StringBuffer(lines.elementAt(0).toString() + "\r\n");
		for (int i = 1; i < lines.size(); i++) {
			sb.append(lines.elementAt(i).toString() + "\r\n");
		}
		return sb.toString();
	}

	/**
	* Allows or disallows to break lines (by default breaking is allowed)
	*/
	public void setMayBreakLines(boolean value) {
		breakLines = value;
	}

	/**
	* If this textCanvas is included in a ScrollPane (directly or indirectly,
	* through a panel with ColumnLayout), returns this ScrollPane
	*/
	protected ScrollPane getScrollPane() {
		if (hasNoScrollPane)
			return null;
		Container c = getParent();
		while (c != null) {
			if (c instanceof ScrollPane)
				return (ScrollPane) c;
			if (c.getLayout() instanceof ColumnLayout) {
				c = c.getParent();
			} else {
				c = null;
			}
		}
		hasNoScrollPane = true;
		return null;
	}

	/**
	* Returns the topmost container with column layout.
	*/
	protected Container getColumnContainer() {
		if (hasNoColumnContainer)
			return null;
		Container c = getParent(), cc = null;
		while (c != null) {
			if (c.getLayout() instanceof ColumnLayout) {
				cc = c;
				c = c.getParent();
			} else {
				c = null;
			}
		}
		hasNoColumnContainer = cc == null;
		return cc;
	}

	/**
	* Returns true if resizes itself
	*/
	protected boolean regulateSize() {
		boolean resized = false;
		countSizes();
		ScrollPane scp = getScrollPane();
		if (scp != null) {
			Dimension vpSize = scp.getViewportSize();
			if (h < vpSize.height - 1 && w < vpSize.width - 1) {
				if (scp == getParent()) {
					h = vpSize.height - 1;
				}
				w = vpSize.width - 1;
			}
			if (lastSize == null || w != lastSize.width || h != lastSize.height) {
				lastSize = new Dimension(w, h);
				setSize(lastSize);
				resized = true;
			}
		} else {
			lastSize = getSize();
		}
		if (isShowing()) {
			CManager.validateAll(this);
			repaint();
		} else {
			CManager.invalidateAll(this);
		}
		return resized;
	}

	public void reset() {
		w = h = 0;
		lastSize = null;
	}

	public void setLeftMargin(int width) {
		marg = width;
	}

	protected boolean widthChanged() {
		if (lastSize == null)
			return true;
		return lastSize.width != getAvailableWidth();
	}

	protected void startListeningToScrollPaneResizing() {
		Container p = getScrollPane();
		if (p == null) {
			p = getColumnContainer();
		}
		if (p != null) {
			removeComponentListener(this);
			p.addComponentListener(this);
			listensToScrollPaneResizing = true;
		}
	}

	@Override
	public void paint(Graphics g) {
		if (!listensToScrollPaneResizing && !hasNoScrollPane) {
			startListeningToScrollPaneResizing();
		}
		lastSize = getSize();
		draw(g, lastSize.width);
		if (drawFrame) {
			g.drawRect(0, 0, lastSize.width - 1, lastSize.height - 1);
		}
	}

	/**
	* Draws the text. Each string starts from a new line. Tries to
	* keep the recommended width prefW. Returns the rectangle occupied.
	*/
	public Rectangle draw(Graphics g, int prefW) {
		if (lines == null || lines.size() < 1)
			return null;
		int y = 0, maxW = 0;
		Color cOrig = g.getColor();
		if (textColor != null) {
			g.setColor(textColor);
		}
		FontMetrics fm = g.getFontMetrics();
		int fh = fm.getHeight(), asc = fm.getAscent();
		for (int i = 0; i < lines.size(); i++) {
			String str = (String) lines.elementAt(i);
			if (str == null || str.length() < 1) {
				y += fh;
				continue;
			}
			int sw = fm.stringWidth(str);
			if (breakLines && sw > prefW - 2 * marg) {
				Point p = StringInRectangle.drawText(g, str, marg, y, prefW - 2 * marg, toBeCentered);
				if (p != null) {
					y = p.y;
					if (p.x > maxW) {
						maxW = p.x;
					}
				}
			} else {
				int x0 = marg;
				if (toBeCentered) {
					x0 = (prefW - 2 * marg - sw) / 2;
					if (x0 < marg) {
						x0 = marg;
					}
				}
				g.drawString(str, x0, y + asc);
				if (x0 + sw > maxW) {
					maxW = x0 + sw;
				}
				y += fh;
			}
		}
		g.setColor(cOrig);
		return new Rectangle(0, 0, maxW + marg, y);
	}

	/**
	* Counts the size of the canvas needed to draw the whole legend.
	* For this purpose uses a bitmap in memory.
	*/
	protected void countSizes() {
		if (lines == null || lines.size() < 1) {
			w = h = 0;
			return;
		}
		if (offScreenImage == null) {
			offScreenImage = createImage(50, 50);
		}
		if (offScreenImage == null)
			return;
		Dimension d = null;
		ScrollPane scp = getScrollPane();
		boolean isScrolled = scp != null && scp.isShowing(), vScrollbarPresent = false;
		Dimension vpSize = null;
		int sbw = 0;
		if (isScrolled) {
			sbw = scp.getVScrollbarWidth();
			vpSize = scp.getViewportSize();
			d = new Dimension(vpSize.width - 4, 0);
		} else {
			Container p = getColumnContainer();
			if (p != null && p.isShowing()) {
				d = new Dimension(p.getSize().width, 0);
			}
		}
		if (d == null) {
			d = new Dimension(prefWidth, 0);
		}
		Graphics img = offScreenImage.getGraphics();
		Rectangle r = draw(img, d.width);
		if (r == null) {
			r = new Rectangle(0, 0, 0, 0);
		}
		if (isScrolled && r.height > vpSize.height) {
			vScrollbarPresent = true;
			vpSize.width -= sbw;
			r = draw(img, vpSize.width - 2);
		}
		w = r.width;
		h = r.height;
		img.dispose();
		/*
		if (debug) {
		  System.out.println("TextCanvas: "+lines.elementAt(0).toString());
		  System.out.println("Preferred width: "+d.width+", w="+w+", h="+h);
		}
		*/
	}

	protected int getAvailableWidth() {
		Dimension d = getSize();
		ScrollPane scp = getScrollPane();
		if (scp != null) {
			d = scp.getViewportSize();
		}
		if (d != null)
			return d.width;
		return 0;
	}

	@Override
	public Dimension getPreferredSize() {
		if (w < 1 || h < 1) {
			countSizes();
		}
		if (w < 1 || h < 1)
			return new Dimension(prefWidth, prefHeight);
		return new Dimension(w, h);
	}

	public void setPreferredSize(int width, int height) {
		prefWidth = width;
		prefHeight = height;
	}

	@Override
	public void setPreferredSize(Dimension d) {
		setPreferredSize(d.width, d.height);
	}

	/**
	* Returns true if the size changed
	*/
	protected boolean checkAndAdjustSize() {
		if (getParent() != null && widthChanged())
			return regulateSize();
		return false;
	}

	@Override
	public void componentResized(ComponentEvent evt) {
		checkAndAdjustSize();
	}

	@Override
	public void componentHidden(ComponentEvent evt) {
	}

	@Override
	public void componentShown(ComponentEvent evt) {
	}

	@Override
	public void componentMoved(ComponentEvent evt) {
	}

}