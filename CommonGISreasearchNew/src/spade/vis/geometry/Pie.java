package spade.vis.geometry;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

import spade.lib.util.IntArray;

public class Pie extends StructSign {
	public boolean showNumberByAngle = true; //when false, then "by radius"

	/**
	* The constructor is used to set the variables isRound and canDrawFrame
	* defined in the ancestor to true
	*/
	public Pie() {
		isRound = true;
		setMayChangeProperty(MAX_SIZE, true);
		usesFrame = true;
	}

	protected boolean isTransparent = false;

	public void setIsTransparent(boolean isTransparent) {
		this.isTransparent = isTransparent;
	}

	protected int pieOrder[] = null;

	public void setPieOrder(int pieOrder[]) {
		this.pieOrder = pieOrder;
	}

	protected float visCompVals[] = null;

	public void setVisCompVals(float visCompVals[]) {
		this.visCompVals = visCompVals;
	}

	/**
	* This function is to be used for area objects.
	* The rectangle in which to fit the diagram is specified.
	* Returns the bounding rectangle of the drawn diagram.
	*/
	@Override
	public void draw(Graphics g, Rectangle r) {
		draw(g, r.x, r.y, r.width, r.height);
	}

	/**
	* Same as previous, only the arguments are not wrapped into a Rectangle object
	*/
	@Override
	public void draw(Graphics g, int x, int y, int w, int h) {
		draw(g, x + w / 2, y + h / 2);
	}

	/**
	* This function is to be used for point objects.
	*/
	@Override
	public void draw(Graphics g, int x, int y) {
		if (colors == null) {
			setDefaultColors();
		}
		if (showNumberByAngle) {
			drawClassicalPie(g, x, y);
		} else {
			drawDistortedPie(g, x, y);
		}
	}

	public void drawClassicalPie(Graphics g, int x, int y) {
		int diam = getDiameter(), r = diam / 2;
		for (int step = 0; step <= 1; step++) {
			if (step == 0) {
				diam += 10;
				r += 5;
				if (r < 10) {
					continue;
				}
				if (visCompVals == null) {
					continue;
				}
			} else {
				diam -= 10;
				r -= 5;
			}
			float sum = 0.0f;
			for (int i = 0; i < nsegm; i++) {
				sum += (step == 0) ? visCompVals[i] : parts[i];
			}
			float sumParts = 0.0f;
			int sumAngles = 0;
			for (int i = 0; i < nsegm; i++) {
				int n = (pieOrder == null) ? i : pieOrder[i];
				float partsn = (step == 0) ? visCompVals[n] : parts[n];
				//this is to ensure 360 in total
				int angle = Math.round((sumParts + partsn) / sum * 360) - sumAngles;
				if (angle <= 0) {
					continue;
				}
				int stangle = 450 - sumAngles, dif = stangle - 90;
				if (dif < 10) {
					int len = (int) Math.round(diam / 2.0 * Math.sin(dif / (2 * Math.PI)));
					if (Math.abs(len) < 6) {
						break;
					}
				}
				g.setColor(colors[n]);
				if (isTransparent) {
					g.drawArc(x - r, y - r, diam, diam, stangle, -angle);
				} else {
					g.setColor(java2d.Drawing2D.getTransparentColor(colors[n], transparency));
					g.fillArc(x - r, y - r, diam, diam, stangle, -angle);
					if (step == 1) {
						g.setColor(Color.black);
						g.drawArc(x - r, y - r, diam, diam, stangle, -angle);
					}
				}
				sumAngles += angle;
				sumParts += partsn;
			}
		}
		labelX = x - r;
		labelY = y + r + 2;
	}

	public void drawDistortedPie(Graphics g, int x, int y) {
		//calculate angles
		int sumAngles = 0;
		IntArray ord = null;
		if (orderMethod != OrderPreserved) {
			ord = getOrder();
		}
		int diam = this.getMaxHeight();
		for (int i = 0; i < nsegm; i++) {
			int idx = (ord == null) ? i : ord.elementAt(i);
			//this is to ensure 360 in total
			int angle = 0;
			if (weights == null) {
				angle = Math.round((360.0f - sumAngles) / (nsegm - i));
			} else {
				angle = Math.round(weights[idx] * 360.0f);
			}
			if (angle < 1) {
				continue;
			}
			int stangle = 450 - sumAngles;
			int d = Math.round(parts[idx] * diam), r = d / 2;
			if (r > 1) {
				//g.setColor(colors[idx]);
				g.setColor(java2d.Drawing2D.getTransparentColor(colors[idx], transparency));
				g.fillArc(x - r, y - r, d, d, stangle, -angle);
				g.setColor(Color.gray);
				g.drawArc(x - r, y - r, d, d, stangle, -angle);
			}
			sumAngles += angle;
		}
		if (!usesFrame)
			return;
		g.setColor(Color.gray);
		if (nsegm > 1) {
			sumAngles = 0;
			for (int i = 0; i < nsegm; i++) {
				int idx = (ord == null) ? i : ord.elementAt(i);
				//this is to ensure 360 in total
				int angle = 0;
				if (weights == null) {
					angle = Math.round((360.0f - sumAngles) / (nsegm - i));
				} else {
					angle = Math.round(weights[idx] * 360.0f);
				}
				int stangle = 450 - sumAngles, dif = 180 - stangle;
				int x1 = x - (int) Math.round(diam * Math.cos(dif * Math.PI / 180) / 2), y1 = y - (int) Math.round(diam * Math.sin(dif * Math.PI / 180) / 2);
				g.drawLine(x, y, x1, y1);
				sumAngles += angle;
			}
		}
		g.drawOval(x - diam / 2, y - diam / 2, diam, diam);
		labelX = x - diam / 2;
		labelY = y + diam / 2 + 2;
	}
}
