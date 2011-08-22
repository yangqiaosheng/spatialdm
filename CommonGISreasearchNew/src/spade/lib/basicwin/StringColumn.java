package spade.lib.basicwin;

import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Scrollbar;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.util.Vector;

public class StringColumn extends Canvas implements AdjustmentListener {
	protected int sh = 0, basel = 0;
	protected static FontMetrics fm = null;
	protected Vector lines = null;
	protected int maxw = 0;
	protected int ystart = 0;

	public void init() {
		if (lines != null) {
			lines.removeAllElements();
		}
	}

	public void addString(String str) {
		if (lines == null) {
			lines = new Vector(30, 10);
		}
		lines.addElement(str);
	}

	public void setString(String str, int n) {
		if (n < 0)
			return;
		if (lines == null) {
			lines = new Vector(30, 10);
		}
		while (lines.size() <= n) {
			lines.addElement(null);
		}
		lines.setElementAt(str, n);
	}

	// Added by PG
	public String getString(int n) {
		if (n < 0 || lines == null || n >= lines.size())
			return null;
		return (String) lines.elementAt(n);
	}

	public void addSeparator() {
		addString("__SEPARATOR");
	}

	public void setSeparator(int lineN) {
		setString("__SEPARATOR", lineN);
	}

	public int getNLines() {
		if (lines == null)
			return 0;
		return lines.size();
	}

	// ~PG
	public void countSizes(FontMetrics fmetr) {
		if (fm == null) {
			fm = fmetr;
		}
		countSizes();
	}

	protected void tryGetFontMetrics() {
		if (fm != null)
			return;
		if (Metrics.fmetr == null) {
			Graphics gr = getGraphics();
			if (gr == null) {
				Image offScreenImage = createImage(20, 20);
				if (offScreenImage == null)
					return;
				gr = offScreenImage.getGraphics();
			}
			if (gr == null)
				return;
			Metrics.setFontMetrics(gr.getFontMetrics());
		}
		fm = Metrics.fmetr;
	}

	public void countSizes() {
		if (lines == null || lines.size() < 1)
			return;
		tryGetFontMetrics();
		if (fm == null)
			return;
		if (basel == 0) {
			basel = fm.getAscent();
		}
		if (sh == 0) {
			sh = fm.getHeight();
			if (sh > basel * 1.5) {
				sh = (int) (basel * 1.5);
			}
		}
		maxw = 0;
		for (int i = 0; i < lines.size(); i++) {
			String str = (String) lines.elementAt(i);
			if (str != null && !str.equals("__SEPARATOR")) {
				int width = fm.stringWidth(str);
				if (width > maxw) {
					maxw = width;
				}
			}
		}
	}

	@Override
	public Dimension getPreferredSize() {
		if (sh < 1) {
			countSizes();
		}
		return new Dimension(maxw, sh * getNLines());
	}

	@Override
	public Dimension getMinimumSize() {
		if (sh < 1) {
			countSizes();
		}
		return new Dimension(maxw / 2, sh * 3);
	}

	@Override
	public void adjustmentValueChanged(AdjustmentEvent e) {
		if (e.getSource() instanceof Scrollbar) {
			setPosition((Scrollbar) e.getSource());
		}
	}

	public void setPosition(Scrollbar sb) {
		ystart = sb.getValue();
		repaint();
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
						g.drawLine(0, y + sh / 2, maxw, y + sh / 2);
					} else {
						g.drawString(str, 0, y + basel);
					}
			}
			y += sh;
			if (y - ystart > d.height) {
				break;
			}
		}
	}
}
