package spade.lib.color.ColorBrewer;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: Oct 29, 2009
 * Time: 11:48:36 AM
 * To change this template use File | Settings | File Templates.
 */
public class Gradient extends Canvas implements MouseListener {

	public static int width = 200;
	public static final int height = 24;

	protected int[][] scheme;
	protected int classes;
	protected int current = -1;
	protected int previous = -1;

	public Gradient(int[][] scheme, int classes) {
		setSize(width, height);
		this.scheme = scheme;
		this.classes = classes;
		this.addMouseListener(this);
	}

	public void setClasses(int classes) {
		this.classes = classes;
		repaint();
	}

	@Override
	public void paint(Graphics g) {
		width = getWidth();
		for (int i = 0; i < width; i++) {
			g.setColor(Schemes.getColor(i / ((float) width - 1), scheme, classes));
			g.drawLine(i, 0, i, height - 1);
		}
		g.setXORMode(Color.black);
		g.setColor(Color.white);
//    if(previous>0) g.drawLine(previous, 0, previous, height-1);
		if (current > 0) {
			g.drawLine(current, 0, current, height - 1);
		}
		g.setPaintMode();
	}

	public void updateDescription(int x) {
		if (x < 0) {
			ColorDialog.descr.setText("");
		} else {
			String text = "";
			int r = 0, g = 0, b = 0;
			float[] hsbvals = new float[3];
//      text += "width="+width+", x="+x+"\n";

			text += "Color scheme used:\n";
			text += "\tRed\tGreen\tBlue\tHue\tSatur.\tBright.\n";
			for (int i = 0; i < scheme.length; i++) {
				r = scheme[i][0];
				g = scheme[i][1];
				b = scheme[i][2];
				Color.RGBtoHSB(r, g, b, hsbvals);
				text += (char) ('A' + i) + "\t" + r + "\t" + g + "\t" + b + "\t" + (int) (hsbvals[0] * 255) + "\t" + (int) (hsbvals[1] * 255) + "\t" + (int) (hsbvals[2] * 255) + "\n";
			}
			text += "Value clicked is:\n";
			Color clicked = Schemes.getColor((float) x / width, scheme, classes);
			r = clicked.getRed();
			g = clicked.getGreen();
			b = clicked.getBlue();
			Color.RGBtoHSB(r, g, b, hsbvals);
			text += "\t" + r + "\t" + g + "\t" + b + "\t" + (int) (hsbvals[0] * 255) + "\t" + (int) (hsbvals[1] * 255) + "\t" + (int) (hsbvals[2] * 255) + "\n";

			int gap;
			float position, k = 0;
			position = (float) x / width;
			if (classes != 0) {
				gap = (int) (position * classes);
				if (gap == classes) {
					gap--;
				}
				position = (float) gap / classes + 1 / (float) classes / 2;
			}
			gap = (int) (position * (scheme.length - 1));
			k = (position * (scheme.length - 1) - gap);

			if (k == 0) {
				text += "Selected value is equal to " + (char) ('A' + gap);
			} else {
				text += "Interpolated between " + (char) ('A' + gap) + " and " + (char) ('A' + gap + 1);
			}

			ColorDialog.schemeChanged(scheme, text);
		}
	}

	@Override
	public void mouseExited(MouseEvent ev) {
		previous = current = -1;
		repaint();
	}

	@Override
	public void mouseEntered(MouseEvent ev) {
	}

	@Override
	public void mouseReleased(MouseEvent ev) {
	}

	@Override
	public void mousePressed(MouseEvent ev) {
		previous = current;
		current = ev.getX();
		repaint();
		updateDescription(ev.getX());
	}

	@Override
	public void mouseClicked(MouseEvent ev) {
	}
}
