package spade.lib.basicwin;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.Vector;

public class ColoredStringColumn extends StringColumn {
	protected Vector colors = null; // colors refer to strings in the StringColumn

	public static final int RECTANGLE = 0;
	public static final int CIRCLE = 1;
	public static final int TRIANGLE_TOP = 2;
	public static final int TRIANGLE_LEFT = 3;
	public static final int TRIANGLE_RIGHT = 4;
	public static final int TRIANGLE_BOTTOM = 5;

	public boolean useFontColor = false; // use string coloring by font's color
	public int figureWidth = 14; // limit for with of figure (pixels)
	public int figureType = RECTANGLE; // default figure representing color
//  public int figureType=CIRCLE;   // default figure representing color

	@Override
	public void init() {
		super.init();
		if (colors != null) {
			colors.removeAllElements();
		}
	}

	public void addString(String str, Color c) {
		super.addString(str);
		if (colors == null) {
			colors = new Vector(30, 10);
		}
		colors.addElement(c);
	}

	@Override
	public void addString(String str) {
		addString(str, null);
	}

	@Override
	public void setString(String str, int n) {
		setString(str, null, n);
	}

	public void setString(String str, Color c, int n) {
		super.setString(str, n);
		setStringColor(c, n);
	}

	public void setStringColor(Color c, int n) {
		if (colors == null) {
			colors = new Vector(30, 10);
		}
		while (colors.size() <= n) {
			colors.addElement(null);
		}
		colors.setElementAt(c, n);
	}

	public Color getStringColor(int n) {
		if (n < 0 || colors == null || n >= colors.size())
			return null;
		return (Color) colors.elementAt(n);
	}

	public void addSeparator(Color c) {
		addString("__SEPARATOR", c);
	}

	@Override
	public void addSeparator() {
		addSeparator(null);
	}

	public void setSeparator(int lineN, Color c) {
		setString("__SEPARATOR", c, lineN);
	}

	@Override
	public void setSeparator(int lineN) {
		setSeparator(lineN, null);
	}

	@Override
	public void countSizes() {
		maxw = 0;
		super.countSizes();
		if (maxw > 0) {
			maxw += figureWidth;
		}
	}

	protected int drawFigure(Graphics g, int y_pos, int n) {
		if (useFontColor || colors == null || n < 0 || n >= colors.size() || colors.elementAt(n) == null)
			return 0;
//    if (figureType!=RECTANGLE) return 0;
		Color prevC = g.getColor();
		g.setColor((Color) colors.elementAt(n));
		switch (figureType) {
		case RECTANGLE: {
			g.fillRect(1, y_pos + 1, figureWidth - 3, sh - 2);
			g.setColor(Color.black);
			g.drawRect(1, y_pos + 1, figureWidth - 3, sh - 2);
			break;
		}
		case CIRCLE: {
			int y, d = (figureWidth - 1) > sh ? sh - 2 : figureWidth - 3;
			y = y_pos + sh / 2 - d / 2;
			g.fillOval(1, y, d, d);
			g.setColor(Color.black);
			g.drawOval(1, y, d, d);
			break;
		}
		default: {
			g.fillRect(1, y_pos + 1, figureWidth - 3, sh - 2);
			g.setColor(Color.black);
			g.drawRect(1, y_pos + 1, figureWidth - 3, sh - 2);
		}
		}
		g.setColor(prevC);
		return figureWidth;
	}

	@Override
	public void paint(Graphics g) {
		if (lines == null || lines.size() < 1)
			return;
		Dimension d = getSize();
		int y = -ystart;
		for (int i = 0; i < lines.size(); i++) {
			if (y + sh > 0) {
				String str = (String) lines.elementAt(i);
				if (str != null)
					if (str.equals("__SEPARATOR")) {
						Color oldC = g.getColor();
						Color c = this.getStringColor(i);
						if (c != null) {
							g.setColor(c);
						}
						g.drawLine(0, y + sh / 2, maxw, y + sh / 2);
						if (c != null) {
							g.setColor(oldC);
						}
					} else {
						g.drawString(str, drawFigure(g, y, i), y + basel);
					}
			}
			y += sh;
			if (y - ystart > d.height) {
				break;
			}
		}
	}
}
