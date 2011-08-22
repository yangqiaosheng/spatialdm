package spade.vis.geometry;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

import spade.lib.color.CS;
import spade.lib.util.IntArray;

public class BarChart extends StructSign {
	public boolean mayModifyShades = true;

	/**
	* The constructor is used to set different maximum sizes of a sign and
	* to set the variable "canDrawFrame" defined in the ancestor to true
	*/
	public BarChart() {
		setMaxSizes(0, 8 * mm);
		setMayChangeProperty(MAX_SIZE, true);
		setMayChangeProperty(USE_FRAME, true);
		usesFrame = false;
	}

	protected boolean cmpMode = false;

	public boolean getCmpMode() {
		return cmpMode;
	}

	public void setCmpMode(boolean cmpMode) {
		this.cmpMode = cmpMode;
	}

	public float cmpValue = Float.NaN;

	// used only for 1-component bars to draw frames in comparison mode
	// otherwise (not a comparison mode or multi-component sign) isNaN
	// possible values between 0 and +1

	@Override
	public void setNSegments(int ns) {
		super.setNSegments(ns);
		if (getMaxWidth() == 0)
			if (ns == 1) {
				setMaxWidth(4 * mm);
			} else {
				setMaxWidth(8 * mm);
			}
		else {
			;
		}
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
		drawBarChart(g, x + (w - maxW) / 2, y + h / 2, false);
	}

	/**
	* This function is to be used for point objects.
	*/
	@Override
	public void draw(Graphics g, int x, int y) {
		if (colors == null) {
			setDefaultColors();
		}
		drawBarChart(g, x, y, true);
	}

	public void drawBarChart(Graphics g, int x, int y, boolean isPointObject) {
		if (parts == null || nsegm < 1)
			return;
		boolean allP = areAllPositive();
		// finding real height of the chart
		float maxpos = 0f, maxneg = 0f;
		if (allP) {
			for (int i = 0; i < nsegm; i++)
				if (!Float.isNaN(parts[i]) && Math.abs(parts[i]) < 2 && parts[i] > maxpos) {
					maxpos = parts[i];
				} else {
					;
				}
		} else {
			for (int i = 0; i < nsegm; i++)
				if (!Float.isNaN(parts[i])) {
					if (parts[i] < 2 && parts[i] > maxpos) {
						maxpos = parts[i];
					}
					if (parts[i] > -2 && parts[i] < maxneg) {
						maxneg = parts[i];
					}
				}
		}
		height = Math.round(maxH * (maxpos - maxneg));
		if (height < 2 * mm) {
			height = 2 * mm;
		}
		int maxY = y;
		labelX = x;
		//if (!isPointObject) y-=(maxH-height)/2;
		//  end
		IntArray ord = null;
		if (orderMethod != OrderPreserved) {
			ord = getOrder();
		}
		int x1 = x;
		float sumW = 0f, ew = 1.0f / nsegm;
		if (cmpMode) {
			float negativePart = 0f;
			for (int i = 0; i < nsegm; i++) {
				int idx = (ord == null) ? i : ord.elementAt(i);
				if (Float.isNaN(parts[idx])) {
					continue;
				}
				if (/*parts[idx]<0 && */parts[idx] < negativePart) {
					negativePart = parts[idx];
				}
			}
			int yref = y;
			x1 = x;
			sumW = 0f;
			for (int i = 0; i < nsegm; i++) {
				int idx = (ord == null) ? i : ord.elementAt(i);
				sumW += (weights == null) ? ew : weights[idx];
				int x2 = x + Math.round(sumW * maxW);
				if (Float.isNaN(parts[idx])) {
					x1 = x2;
					continue;
				}

				// *** process out-of-focus values
				if (Math.abs(parts[idx]) > 2) {
					if (parts[idx] > 0) {
						g.setColor(colors[idx]);
					} else {
						Color c = colors[idx];
						if (mayModifyShades) {
							c = new Color(Color.HSBtoRGB(CS.getHue(c), CS.getSaturation(c) / 2f, CS.getBrightness(c)));
						}
						g.setColor(c);
					}
					/*  // Old style drawing like "mushrooms"
					int X[]=new int[4], Y[]=new int[4];
					X[0]=x1+(x2-x1)/4; X[1]=x2-(x2-x1)/4; X[2]=(x1+x2)/2; X[3]=X[0];
					Y[0]=Y[1]=Y[3]=yref;
					Y[2]=(parts[idx]>0) ? yref-(x2-x1)/2 : yref+(x2-x1)/2;
					for (int j=0; j<Y.length; j++)
					  if (parts[idx]>0) Y[j]-=10; else Y[j]+=10;
					g.fillPolygon(X,Y,4);
					g.setColor(Color.black);
					g.drawPolygon(X,Y,4);
					g.drawLine(X[2],yref,X[2],Y[0]);
					*/
					int barH = Math.abs(parts[idx]) > 5 ? maxH : minH;
					int y2 = yref - (parts[idx] > 0 ? barH : 0);
					g.drawRect(x1, y2, x2 - x1, barH);
					if (parts[idx] < 0 && maxY < yref + barH) {
						maxY = yref + barH;
					}

					x1 = x2;
					continue;
				}

				int y2 = yref - Math.round(parts[idx] * maxH);
				if (parts[idx] >= 0) {
					if (nsegm == 1 && usesFrame && !Float.isNaN(cmpValue)) {
						g.setColor(Color.gray);
						int dy = Math.round(maxH * (1 - cmpValue));
						g.drawRect(x1, yref - dy, x2 - x1, dy);
					}
					//g.setColor(colors[idx]);
					g.setColor(java2d.Drawing2D.getTransparentColor(colors[idx], transparency));
					g.fillRect(x1, y2, x2 - x1, yref - y2);
					g.setColor(Color.black);
					g.drawRect(x1, y2, x2 - x1, yref - y2);
				} else {
					if (nsegm == 1 && usesFrame && !Float.isNaN(cmpValue)) {
						g.setColor(Color.gray);
						int dy = Math.round(maxH * cmpValue);
						g.drawRect(x1, yref, x2 - x1, dy);
					}
					//g.setColor(colors[idx]);
					Color c = colors[idx];
					if (mayModifyShades) {
						c = new Color(Color.HSBtoRGB(CS.getHue(c), CS.getSaturation(c) / 2f, CS.getBrightness(c)));
					}
					//g.setColor(c);
					g.setColor(java2d.Drawing2D.getTransparentColor(c, transparency));
					g.fillRect(x1, yref, x2 - x1, y2 - yref);
					g.setColor(Color.black);
					g.drawRect(x1, yref, x2 - x1, y2 - yref);
					if (maxY < y2) {
						maxY = y2;
					}
				}
				x1 = x2;
			}
			g.setColor(Color.black);
			g.drawOval(x - 1, yref - 1, 3, 3);
			g.drawLine(x, yref, x + maxW + 3, yref); // 3 pixels more to have a possibility
			//to see if the only rectangle is upper or below the base line
			//g.drawLine(x,y-maxH,x,y);
		} else {
			if (usesFrame) {
				g.setColor(Color.gray);
				g.drawRect(x, y - maxH, maxW, maxH);
			}
			for (int i = 0; i < nsegm; i++) {
				int idx = (ord == null) ? i : ord.elementAt(i);
				sumW += (weights == null) ? ew : weights[idx];
				int x2 = x + Math.round(sumW * maxW);
				if (Float.isNaN(parts[idx])) {
					x1 = x2;
					continue;
				}

				// process "out-of-focus" values
				if (Math.abs(parts[idx]) > 2) {
					g.setColor(colors[idx]);
					/* //  Mushrooms
					int X[]=new int[4], Y[]=new int[4];
					X[0]=x1; X[1]=x2; X[2]=(x1+x2)/2; X[3]=x1;
					Y[0]=Y[1]=Y[3]=y;
					Y[2]=(parts[idx]>0) ? y-(x2-x1)/2 : y+(x2-x1)/2;
					g.fillPolygon(X,Y,4);
					g.setColor(Color.black);
					g.drawPolygon(X,Y,4);
					*/

					int barH = Math.abs(parts[idx]) > 5 ? maxH : minH;
					int y2 = y - (parts[idx] > 0 ? barH : 0);
					g.drawRect(x1, y2, x2 - x1, barH);
					if (parts[idx] < 0 && maxY < y + barH) {
						maxY = y + barH;
					}

					x1 = x2;
					continue;
				}
				int y2 = y - Math.round(parts[idx] * maxH);
				//g.setColor(colors[idx]);
				g.setColor(java2d.Drawing2D.getTransparentColor(colors[idx], transparency));
				g.fillRect(x1, y2, x2 - x1, y - y2);
				g.setColor(Color.black);
				g.drawRect(x1, y2, x2 - x1, y - y2);
				if (maxY < y2) {
					maxY = y2;
				}
				x1 = x2;
			}
		}
		labelY = maxY + 2;
	}

}
