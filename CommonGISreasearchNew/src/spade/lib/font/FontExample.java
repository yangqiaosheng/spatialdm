package spade.lib.font;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.util.Vector;

import spade.lib.util.StringUtil;
import spade.vis.dmap.DrawingParameters;

public class FontExample extends Canvas implements FontListener {
	int style = DrawingParameters.NORMAL;
	Color color = null;
	Color cShadow = null;
	Font f = null;
	String exampleText = "Aa";
	int txtW = 100, txtH = 80;

	public FontExample(Font f) {
		this(f, Color.black);
	}

	public FontExample(Font f, Color c) {
		this.f = f;
		if (f != null) {
			exampleText = f.getName();
		}
		this.color = c;
	}

	public void setColor(Color c) {
		color = c;
	}

	@Override
	public void setFont(Font f) {
		this.f = f;
	}

	public void setStyle(int s) {
		style = s;
	}

	public void draw() {
		Graphics g = getGraphics();
		if (g == null)
			return;
		paint(g);
	}

	@Override
	public void paint(Graphics g) {
		g.setFont(f);
		g.setColor(color);
		cShadow = ((color.getRed() + color.getGreen() + color.getBlue()) > 3 * 128) ? Color.black : Color.white;

		FontMetrics fm = g.getFontMetrics(f);
		if (exampleText.length() > 2) { // make example text shorter
			Vector fn = StringUtil.getNames(exampleText, " -.");
			if (fn != null && fn.size() > 1) {
				exampleText = (String) fn.elementAt(0);
			}
		}
		txtW = fm.stringWidth(exampleText);
		txtH = fm.getHeight();
		int x = 0 + (getSize().width - txtW) / 2, y = (getSize().height - txtH) / 2 + fm.getAscent();

		if (style == DrawingParameters.SHADOWED) {
			g.setColor(cShadow);
			g.drawString(exampleText, x + 1, y + 1);
		} else if (style == (DrawingParameters.UNDERLINED | DrawingParameters.SHADOWED)) {
			g.setColor(cShadow);
			g.drawString(exampleText, x + 1, y + 1);
			g.drawLine(x, y + 3, x + txtW, y + 3);
		}
		g.setColor(color);
		g.drawString(exampleText, x, y);
		if (style == DrawingParameters.UNDERLINED || style == (DrawingParameters.UNDERLINED | DrawingParameters.SHADOWED)) {
			g.drawLine(x, y + 2, x + txtW, y + 2);
		}
	}

	@Override
	public void fontChanged(Font f, int style, Object sel) {
		this.f = f;
		exampleText = f.getName();
		this.style = style;
		repaint();
	}

	@Override
	public Dimension getPreferredSize() {
		Dimension d = new Dimension(txtW, txtH);
		return d;
	}
}
