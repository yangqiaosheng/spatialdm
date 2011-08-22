package spade.lib.basicwin;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.util.StringTokenizer;

public class StringInRectangle {
	String s = null;
	int l = 0, t = 0, w = 0, h = 0;
	public static final int Left = -1, Center = 0, Right = 1;
	int Position = Left;
	FontMetrics fm = null;
	int fh = 0, asc = 0;

	public StringInRectangle() {
	}

	public StringInRectangle(String AString) {
		setString(AString);
	}

	public void setString(String AString) {
		s = AString;
	}

	public void setRectSize(int width, int height) {
		w = width;
		h = height;
	}

	public void setBounds(int left, int top, int width, int height) {
		l = left;
		t = top;
		w = width;
		h = height;
	}

	public void setPosition(int pos) {
		Position = pos;
	}

	public void setMetrics(FontMetrics fmetr) {
		if (fmetr != null) {
			fm = fmetr;
			asc = fm.getAscent();
			fh = fm.getHeight();
			if (fh > 1.5 * asc) {
				fh = (int) (1.5 * asc);
			}
		}
	}

	public void getMetrics(Graphics g) {
		if (fm == null) {
			setMetrics(g.getFontMetrics());
		}
	}

	public void drawRight(Graphics g) {
		if (s == null || s.equals(""))
			return;
		getMetrics(g);
		int sw = fm.stringWidth(s), x = w - sw - 1;
		if (x < 0) {
			x = 0;
		}
		g.drawString(s, l + x, t + asc);
	}

	public void drawCenter(Graphics g) {
		if (s == null || s.equals(""))
			return;
		getMetrics(g);
		int sw = fm.stringWidth(s), x = (w - sw) / 2;
		if (x < 0) {
			x = 0;
		}
		g.drawString(s, l + x, t + asc);
	}

	public Dimension countSizes(Graphics g) {
		getMetrics(g);
		return countSizes();
	}

	public Dimension countSizes() {
		Dimension d = new Dimension(0, 0);
		if (s == null || s.length() < 1 || fm == null)
			return d;
		StringTokenizer st = new StringTokenizer(s, "\n");
		while (st.hasMoreTokens()) {
			String ss = st.nextToken().trim();
			while (ss != null && ss.length() > 0) {
				int idx = ss.indexOf(' ');
				if (idx < 0) {
					d.height += fh;
					int lw = fm.stringWidth(ss);
					if (lw > d.width) {
						d.width = lw;
					}
					break;
				}
				int lw = fm.stringWidth(ss.substring(0, idx));
				while (lw < w && idx > 0 && idx < ss.length() - 1) {
					int idx1 = idx + 1;
					while (idx1 < ss.length() && ss.charAt(idx1) == ' ') {
						++idx1;
					}
					idx1 = ss.indexOf(' ', idx1 + 1);
					if (idx1 < 0) {
						idx1 = ss.length();
					}
					int lw1 = fm.stringWidth(ss.substring(0, idx1));
					if (lw1 > w) {
						break;
					} else {
						idx = idx1;
						lw = lw1;
					}
				}
				d.height += fh;
				if (lw > d.width) {
					d.width = lw;
				}
				if (idx > 0 && idx < ss.length() - 1) {
					ss = ss.substring(idx + 1).trim();
				} else {
					ss = null;
				}
			}
		}
		return d;
	}

	public Point draw(Graphics g) {
		return drawText(g, s, l, t, w, Position);
	}

	/**
	* Used to draw any text (e.g. in the legend) with breaking it, when necessary,
	* into several lines. (x,y) specify the upper left corner, and maxW -
	* the width of the rectangle in which to draw the text. The function
	* returns the lower right corner of the rectangle used for the text.
	*/
	public static Point drawText(Graphics g, String txt, int x, int y, int maxW, boolean centered) {
		return drawText(g, txt, x, y, maxW, (centered) ? Center : Left);
	}

	/**
	* Used to draw any text (e.g. in the legend) with breaking it, when necessary,
	* into several lines. (x,y) specify the upper left corner, and maxW -
	* the width of the rectangle in which to draw the text. The function
	* returns the lower right corner of the rectangle used for the text.
	*/
	public static Point drawText(Graphics g, String txt, int x, int y, int maxW, int pos) {
		Point p = new Point(x, y);
		if (txt == null || txt.length() < 1 || g == null)
			return p;
		FontMetrics fm = g.getFontMetrics();
		int asc = fm.getAscent(), fh = fm.getHeight();
		if (fh > 1.5 * asc) {
			fh = (int) (1.5 * asc);
		}
		y += asc;
		StringTokenizer st = new StringTokenizer(txt, "\r\n");
		while (st.hasMoreTokens()) {
			String ss = st.nextToken().trim();
			while (ss != null && ss.length() > 0) {
				int idx = ss.indexOf(' ');
				if (idx < 0) {
					int lw = fm.stringWidth(ss);
					int sx = 0;
					if (pos == Center) {
						sx = (maxW - lw) / 2;
					} else if (pos == Right) {
						sx = maxW - lw;
					}
					g.drawString(ss, x + sx, y);
					if (x + sx + lw > p.x) {
						p.x = x + sx + lw;
					}
					y += fh;
					break;
				}
				int lw = fm.stringWidth(ss.substring(0, idx));
				while (lw < maxW && idx < ss.length() - 1) {
					int idx1 = idx + 1;
					while (idx1 < ss.length() && ss.charAt(idx1) == ' ') {
						++idx1;
					}
					idx1 = ss.indexOf(' ', idx1);
					if (idx1 < 0) {
						idx1 = ss.length();
					}
					int lw1 = fm.stringWidth(ss.substring(0, idx1));
					if (lw1 > maxW) {
						break;
					} else {
						idx = idx1;
						lw = lw1;
					}
				}
				int sx = 0;
				if (pos == Center) {
					sx = (maxW - lw) / 2;
				} else if (pos == Right) {
					sx = maxW - lw;
				}
				g.drawString(ss.substring(0, idx), x + sx, y);
				if (x + sx + lw > p.x) {
					p.x = x + sx + lw;
				}
				y += fh;
				if (idx + 1 < ss.length()) {
					ss = ss.substring(idx + 1).trim();
				} else {
					ss = null;
				}
			}
		}
		p.y = y - asc + 2;
		return p;
	}

	/**
	* Used to draw any text (e.g. in the legend) WITHOUT breaking it
	* into several lines. (x,y) specify the upper left corner, and maxW -
	* the width of the rectangle in which to draw the text. If the string does not
	* fit to the rectangle, the symbol "..." is drawn.
	*/
	public static void drawString(Graphics g, String txt, int x, int y, int maxW, int pos) {
		if (txt == null || txt.length() < 1 || g == null)
			return;
		FontMetrics fm = g.getFontMetrics();
		int sw = fm.stringWidth(txt);
		if (sw > maxW) {
			txt = "...";
			sw = fm.stringWidth(txt);
			if (sw > maxW) {
				txt = "..";
				sw = fm.stringWidth(txt);
				if (sw > maxW) {
					txt = ".";
					sw = fm.stringWidth(txt);
				}
			}
		}
		if (pos == Center) {
			x += (maxW - sw) / 2;
		} else if (pos == Right) {
			x += maxW - sw;
		}
		int asc = fm.getAscent();
		g.drawString(txt, x, y + asc);
	}
};