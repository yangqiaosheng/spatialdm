package spade.analysis.tools.moves;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

import spade.lib.basicwin.Drawing;
import spade.vis.geometry.Sign;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: Mar 4, 2008
 * Time: 11:59:24 AM
 * To change this template use File | Settings | File Templates.
 */
public class DirectionAndSpeedSign extends Sign {

	protected double values[] = null; // 0: no move; 1..4 or 1..8: move in 4/8 directions
	protected double min, max;
	protected boolean drawSegments[] = null;
	protected Color segmentColors[] = null;

	/**
	* The constructor is used to set the default sizes and sign properties that
	* can be modified
	*/
	public DirectionAndSpeedSign() {
		setMayChangeProperty(MAX_SIZE, true);
		setMayChangeProperty(USE_FRAME, true);
		maxW = 10 * mm;
		maxH = 10 * mm;
		usesFrame = false;
		isRound = true;
	}

	/**
	* Sets the array of attribute values to be represented
	*/
	public void setValues(double values[]) {
		this.values = values;
	}

	/**
	* Sets which sign segments are to be drawn
	*/
	public void setDrawSegment(boolean drawSegments[]) {
		this.drawSegments = drawSegments;
	}

	/**
	* Sets colors for sign segments
	*/
	public void setSegmentColors(Color segmentColors[]) {
		this.segmentColors = segmentColors;
	}

	/**
	* Sets the minimum and maximum attribute values to be represented
	*/
	public void setMinMax(double minVal, double maxVal) {
		min = minVal;
		max = maxVal;
	}

	/**
	* This function is to be used for point objects.
	*/
	@Override
	public void draw(Graphics g, int x, int y) {
		labelX = x;
		labelY = y;
		if (values == null || Double.isNaN(max) || drawSegments == null || segmentColors == null)
			return;
		int x0 = x, y0 = y, // center of the rectangle
		diameter = Math.min(maxW, maxH), maxRadius = diameter / 2, maxRadius2 = (int) Math.round(maxRadius / Math.sqrt(2f)), radius = (int) Math.round(maxRadius * (values[0] - min) / (max - min));
		if (usesFrame) {
			g.setColor(Color.gray);
			g.drawOval(x0 - maxRadius, y0 - maxRadius, 2 * maxRadius, 2 * maxRadius);
		}
		g.setColor(segmentColors[0]);
		if (drawSegments[0])
			if (values[0] > max) {
				g.drawOval(x0 - maxRadius, y0 - maxRadius, 2 * maxRadius, 2 * maxRadius);
			} else if (values[0] > min) {
				g.drawOval(x0 - radius, y0 - radius, 2 * radius, 2 * radius);
				g.drawOval(x0 - radius + 1, y0 - radius + 1, 2 * radius - 2, 2 * radius - 2);
				//g.fillOval(x0-radius,y0-radius,2*radius,2*radius);
			} else if (values[0] > 0) {
				g.drawLine(x0, y0, x0, y0);
			}
		//g.drawRect(x0-1,y0-1,3,3);
		if (values.length == 5) {
			if (drawSegments[1]) { // 1=N
				radius = (int) Math.round(maxRadius * (values[1] - min) / (max - min));
				g.setColor(segmentColors[1]);
				if (values[1] > max) {
					g.drawRect(x0 - 1, y0 - 0 - maxRadius, 3, maxRadius);
				} else if (values[1] > min) {
					g.fillRect(x0 - 1, y0 - 0 - radius, 3 + 1, radius + 1);
				} else if (values[1] > 0) {
					g.drawLine(x0 - 1, y0 - 1, x0 + 1, y0 - 1);
				}
			}
			if (drawSegments[2]) { // 2=E
				radius = (int) Math.round(maxRadius * (values[2] - min) / (max - min));
				g.setColor(segmentColors[2]);
				if (values[2] > max) {
					g.drawRect(x0 + 1, y0 - 1, maxRadius, 3);
				} else if (values[2] > min) {
					g.fillRect(x0 + 1, y0 - 1, radius + 1, 3 + 1);
				} else if (values[2] > 0) {
					g.drawLine(x0 + 1, y0 - 1, x0 + 1, y0 + 1);
				}
			}
			if (drawSegments[3]) { // 3=S
				radius = (int) Math.round(maxRadius * (values[3] - min) / (max - min));
				g.setColor(segmentColors[3]);
				if (values[3] > max) {
					g.drawRect(x0 - 1, y0 + 1, 3, maxRadius);
				} else if (values[3] > min) {
					g.fillRect(x0 - 1, y0 + 1, 3 + 1, radius + 1);
				} else if (values[3] > 0) {
					g.drawLine(x0 - 1, y0 + 1, x0 + 1, y0 + 1);
				}
			}
			if (drawSegments[4]) { // 4=W
				radius = (int) Math.round(maxRadius * (values[4] - min) / (max - min));
				g.setColor(segmentColors[4]);
				if (values[4] > max) {
					g.drawRect(x0 - 0 - maxRadius, y0 - 1, maxRadius, 3);
				} else if (values[4] > min) {
					g.fillRect(x0 - 0 - radius, y0 - 1, radius + 1, 3 + 1);
				} else if (values[4] > 0) {
					g.drawLine(x0 - 1, y0 - 1, x0 - 1, y0 + 1);
				}
			}
		} else { // 9 segments
			int xx[] = new int[2], yy[] = new int[2];
			if (drawSegments[1]) { // 1=N
				radius = (int) Math.round(maxRadius * (values[1] - min) / (max - min));
				g.setColor(segmentColors[1]);
				if (values[1] > max) {
					g.drawRect(x0 - 1, y0 - 0 - maxRadius, 3, maxRadius);
				} else if (values[1] > min) {
					g.fillRect(x0 - 1, y0 - 0 - radius, 3 + 1, radius + 1);
				} else if (values[1] > 0) {
					g.drawLine(x0 - 1, y0 - 1, x0 + 1, y0 - 1);
				}
			}
			if (drawSegments[2]) { // 2=NE
				radius = (int) Math.round(maxRadius * (values[2] - min) / ((max - min) * Math.sqrt(2f)));
				xx[0] = x0 - 1;
				xx[1] = (values[2] > max) ? xx[0] + maxRadius2 : xx[0] + radius;
				yy[0] = y0 - 1;
				yy[1] = (values[2] > max) ? yy[0] - maxRadius2 : yy[0] - radius;
				g.setColor(segmentColors[2]);
				if (values[2] > max) {
					g.drawLine(xx[0], yy[0], xx[1], yy[1]);
					g.drawLine(xx[0] + 2, yy[0] + 2, xx[1] + 2, yy[1] + 2);
					g.drawLine(xx[0], yy[0], xx[0] + 2, yy[0] + 2);
					g.drawLine(xx[1], yy[1], xx[1] + 2, yy[1] + 2);
				} else if (values[2] > min) {
					Drawing.drawLine(g, 3, xx[0] + 1, yy[0] + 1, xx[1] + 1, yy[1] + 1, false, false);
				} else if (values[2] > 0) {
					Drawing.drawLine(g, 3, x0 - 1, y0 - 1, x0 + 1, y0 + 1, false, false);
					//g.drawLine(x0-1,y0-1,x0+1,y0+1);
				}
			}
			if (drawSegments[3]) { // 3=E
				radius = (int) Math.round(maxRadius * (values[3] - min) / (max - min));
				g.setColor(segmentColors[3]);
				if (values[3] > max) {
					g.drawRect(x0 + 1, y0 - 1, maxRadius, 3);
				} else if (values[3] > min) {
					g.fillRect(x0 + 1, y0 - 1, radius + 1, 3 + 1);
				} else if (values[3] > 0) {
					g.drawLine(x0 + 1, y0 - 1, x0 + 1, y0 + 1);
				}
			}
			if (drawSegments[4]) { // 4=SE
				radius = (int) Math.round(maxRadius * (values[4] - min) / ((max - min) * Math.sqrt(2f)));
				xx[0] = x0 - 1;
				xx[1] = (values[4] > max) ? xx[0] + maxRadius2 : xx[0] + radius;
				yy[0] = y0 + 1;
				yy[1] = (values[4] > max) ? yy[0] + maxRadius2 : yy[0] + radius;
				g.setColor(segmentColors[4]);
				if (values[4] > max) {
					g.drawLine(xx[0], yy[0], xx[1], yy[1]);
					g.drawLine(xx[0] + 2, yy[0] - 2, xx[1] + 2, yy[1] - 2);
					g.drawLine(xx[0], yy[0], xx[0] + 2, yy[0] - 2);
					g.drawLine(xx[1], yy[1], xx[1] + 2, yy[1] - 2);
				} else if (values[4] > min) {
					Drawing.drawLine(g, 3, xx[0] + 1, yy[0] - 1, xx[1] + 1, yy[1] - 1, false, false);
				} else if (values[4] > 0) {
					Drawing.drawLine(g, 3, xx[0] - 1, yy[0] - 0, xx[0] + 1, yy[0] - 2, false, false);
				}
			}
			if (drawSegments[5]) { // 5=S
				radius = (int) Math.round(maxRadius * (values[5] - min) / (max - min));
				g.setColor(segmentColors[5]);
				if (values[5] > max) {
					g.drawRect(x0 - 1, y0 + 1, 3, maxRadius);
				} else if (values[5] > min) {
					g.fillRect(x0 - 1, y0 + 1, 3 + 1, radius + 1);
				} else if (values[5] > 0) {
					g.drawLine(x0 - 1, y0 + 1, x0 + 1, y0 + 1);
				}
			}
			if (drawSegments[6]) { // 6=SW
				radius = (int) Math.round(maxRadius * (values[6] - min) / ((max - min) * Math.sqrt(2f)));
				xx[0] = x0 + 1;
				xx[1] = (values[6] > max) ? xx[0] - maxRadius2 : xx[0] - radius;
				yy[0] = y0 + 1;
				yy[1] = (values[6] > max) ? yy[0] + maxRadius2 : yy[0] + radius;
				g.setColor(segmentColors[6]);
				if (values[6] > max) {
					g.drawLine(xx[0], yy[0], xx[1], yy[1]);
					g.drawLine(xx[0] - 2, yy[0] - 2, xx[1] - 2, yy[1] - 2);
					g.drawLine(xx[0], yy[0], xx[0] - 2, yy[0] - 2);
					g.drawLine(xx[1], yy[1], xx[1] - 2, yy[1] - 2);
				} else if (values[6] > min) {
					Drawing.drawLine(g, 3, xx[0] - 1, yy[0] - 1, xx[1] - 1, yy[1] - 1, false, false);
				} else if (values[6] > 0) {
					Drawing.drawLine(g, 3, x0 - 1, y0 - 0, x0 + 1, y0 + 2, false, false);
					//g.drawLine(x0-1,y0-0,x0+1,y0+2);            
				}
			}
			if (drawSegments[7]) { // 7=W
				radius = (int) Math.round(maxRadius * (values[7] - min) / (max - min));
				g.setColor(segmentColors[7]);
				if (values[7] > max) {
					g.drawRect(x0 - 0 - maxRadius, y0 - 1, maxRadius, 3);
				} else if (values[7] > min) {
					g.fillRect(x0 - 0 - radius, y0 - 1, radius + 1, 3 + 1);
				} else if (values[7] > 0) {
					g.drawLine(x0 - 1, y0 - 1, x0 - 1, y0 + 1);
				}
			}
			if (drawSegments[8]) { // 8=NW
				radius = (int) Math.round(maxRadius * (values[8] - min) / ((max - min) * Math.sqrt(2f)));
				xx[0] = x0 + 1;
				xx[1] = (values[8] > max) ? xx[0] - maxRadius2 : xx[0] - radius;
				yy[0] = y0 - 1;
				yy[1] = (values[8] > max) ? yy[0] - maxRadius2 : yy[0] - radius;
				g.setColor(segmentColors[8]);
				if (values[8] > max) {
					g.drawLine(xx[0], yy[0], xx[1], yy[1]);
					g.drawLine(xx[0] - 2, yy[0] + 2, xx[1] - 2, yy[1] + 2);
					g.drawLine(xx[0], yy[0], xx[0] - 2, yy[0] + 2);
					g.drawLine(xx[1], yy[1], xx[1] - 2, yy[1] + 2);
				} else if (values[8] > min) {
					Drawing.drawLine(g, 3, xx[0] - 1, yy[0] + 1, xx[1] - 1, yy[1] + 1, false, false);
				} else if (values[8] > 0) {
					Drawing.drawLine(g, 3, xx[0] - 1, yy[0] + 0, xx[0] + 1, yy[0] + 2, false, false);
				}
			}
		}
	}

	/**
	* This function is to be used for area objects.
	* The rectangle in which to fit the diagram is specified.
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

}
