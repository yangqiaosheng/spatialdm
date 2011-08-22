package spade.analysis.plot;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import spade.analysis.system.Supervisor;
import spade.lib.basicwin.Focuser;
import spade.lib.basicwin.Metrics;
import spade.lib.util.DoubleArray;
import spade.lib.util.NumValManager;
import spade.lib.util.StringUtil;
import spade.vis.action.ObjectEventHandler;

/**
* A class that represents values of two numeric attributes
* on a scatter plot with focusers.
* The data to represent are taken from an AttributeDataPortion.
*/

public class ScatterPlotWithFocusers extends ScatterPlot {

	/**
	 * Focusers are used for focusing on value subranges of the attributes
	 */
	private Focuser horFocuser = null, vertFocuser = null;

	public ScatterPlotWithFocusers(boolean isIndependent, boolean allowSelection, Supervisor sup, ObjectEventHandler handler) {
		super(isIndependent, allowSelection, sup, handler);
	}

	/**
	* Resets its internal data, including the array of screen objects.
	* Called when records are added to or removed from the table.
	*
	*/
	@Override
	public void reset() {
		min1 = max1 = min2 = max2 = Double.NaN;
		if (dataTable == null || fn1 < 0 || fn2 < 0) {
			dots = null;
			return;
		}
		setup();
		if (horFocuser != null) {
			horFocuser.setMinMax(min1, max1, min1, max1);
			if (minTime1 != null && maxTime1 != null) {
				horFocuser.setAbsMinMaxTime(minTime1, maxTime1);
			}
		}
		if (vertFocuser != null) {
			vertFocuser.setMinMax(min2, max2, min2, max2);
			if (minTime2 != null && maxTime2 != null) {
				vertFocuser.setAbsMinMaxTime(minTime2, maxTime2);
			}
		}
		if (canvas != null) {
			canvas.repaint();
		} else {
			redraw();
		}
	}

	@Override
	public void setup() {
		super.setup();
		recalcCorrelation();
	}

	/**
	* data about correlation
	*/
	protected double correlation = Double.NaN;
	protected DoubleArray xVals = null, yVals = null;
	/**
	* data about regression line y=M*x+B
	*/
	protected double mb[] = null;

	protected void recalcCorrelation() {
		xVals = new DoubleArray(dots.length, 10);
		yVals = new DoubleArray(dots.length, 10);
		for (int i = 0; i < dots.length; i++)
			if (dots[i].isActive) {
				double v1 = getNumericAttrValue(fn1, i), v2 = getNumericAttrValue(fn2, i);
				xVals.addElement(v1);
				yVals.addElement(v2);
			}
		correlation = NumValManager.getCorrelation(xVals, yVals);
		mb = NumValManager.getLinearRegression(xVals, yVals);
		//if (mb!=null) System.out.println("* Y="+StringUtil.floatToStr(mb[0],2)+"*X+"+StringUtil.floatToStr(mb[1],2));
	}

	/**
	* after changing a query, it is necessary to recalculate
	* correlation between selected subsets
	*/
	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		if (pce.getSource().equals(tf) && !destroyed) {
			applyFilter();
			plotImageValid = false;
			recalcCorrelation();
			if (canvas != null) {
				canvas.repaint();
			}
		} else {
			super.propertyChange(pce);
		}
	}

	@Override
	public boolean reloadAttributeData(Vector v) {
		if (v == null)
			return false;
		for (int i = 0; i < v.size(); i++) {
			String AttrID = (String) v.elementAt(i);
			if (AttrID.equals(dataTable.getAttributeId(fn1)) || AttrID.equals(dataTable.getAttributeId(fn2))) {
				setup();
				Graphics g = canvas.getGraphics();
				if (AttrID.equals(dataTable.getAttributeId(fn1)) && horFocuser != null) {
					horFocuser.setAbsMinMax(min1, max1);
					if (minTime1 != null && maxTime1 != null) {
						horFocuser.setAbsMinMaxTime(minTime1, maxTime1);
					}
					horFocuser.setCurrMinMax(min1, max1);
					horFocuser.notifyFocusChange();
					horFocuser.draw(g);
				}
				if (AttrID.equals(dataTable.getAttributeId(fn2)) && vertFocuser != null) {
					vertFocuser.setAbsMinMax(min2, max2);
					if (minTime2 != null && maxTime2 != null) {
						vertFocuser.setAbsMinMaxTime(minTime2, maxTime2);
					}
					vertFocuser.setCurrMinMax(min2, max2);
					vertFocuser.notifyFocusChange();
					vertFocuser.draw(g);
				}
				g.dispose();
				return false;
			}
		}
		return false;
	}

//ID
	/**
	 * Returns custom properties of the tool: String -> String
	 * By default, returns null.
	 */
	@Override
	public Hashtable getProperties() {
		Hashtable prop = new Hashtable();
		prop.put("rangeHorizontal", horFocuser.getCurrMin() + " " + horFocuser.getCurrMax());
		prop.put("rangeVertical", vertFocuser.getCurrMin() + " " + vertFocuser.getCurrMax());
		return prop;
	}

	/**
	 * After the plot is constructed, it may be requested to setup its individual
	 * properties according to the given list of stored properties.
	 * The base Plot class does nothing in this method.
	 */
	@Override
	public void setProperties(Hashtable properties) {
		super.setProperties(properties);
		try {
			constructFocusers();
			String s = (String) properties.get("rangeHorizontal");
			StringTokenizer st = new StringTokenizer(s, " ");
			horFocuser.setCurrMinMax(new Double(st.nextToken()).doubleValue(), new Double(st.nextToken()).doubleValue());
			s = (String) properties.get("rangeVertical");
			st = new StringTokenizer(s, " ");
			vertFocuser.setCurrMinMax(new Double(st.nextToken()).doubleValue(), new Double(st.nextToken()).doubleValue());
		} catch (Exception ex) {
		}
	}

//~ID

	protected void constructFocusers() {
		if (!isZoomable)
			return;
		if (horFocuser == null) {
			horFocuser = new Focuser();
			horFocuser.setAttributeNumber(fn1);
			horFocuser.setIsVertical(false);
			horFocuser.setAbsMinMax(min1, max1);
			if (minTime1 != null && maxTime1 != null) {
				horFocuser.setAbsMinMaxTime(minTime1, maxTime1);
			}
			horFocuser.setSpacingFromAxis(0);
			horFocuser.setIsLeft(true); //top position
			//horFocuser.setIsLeft(false); //this is to check bottom position
			horFocuser.setBkgColor(bkgColor);
			horFocuser.setPlotAreaColor(plotAreaColor);
			horFocuser.addFocusListener(this);
		}
		if (vertFocuser == null) {
			vertFocuser = new Focuser();
			vertFocuser.setAttributeNumber(fn2);
			vertFocuser.setIsVertical(true);
			vertFocuser.setAbsMinMax(min2, max2);
			if (minTime2 != null && maxTime2 != null) {
				vertFocuser.setAbsMinMaxTime(minTime2, maxTime2);
			}
			vertFocuser.setSpacingFromAxis(0);
			vertFocuser.setIsLeft(false); //right position
			//vertFocuser.setIsLeft(true);  //this is to check left position
			vertFocuser.setBkgColor(bkgColor);
			vertFocuser.setPlotAreaColor(plotAreaColor);
			vertFocuser.addFocusListener(this);
		}
	}

	@Override
	public void drawReferenceFrame(Graphics g) {
		constructFocusers();
		FontMetrics fm = g.getFontMetrics();
		int asc = fm.getAscent();
		String strmin1 = numToString(min1, min1, max1, minTime1), strmin2 = numToString(min2, min2, max2, maxTime2), strmax1 = numToString(max1, min1, max1, minTime1), strmax2 = numToString(max2, min2, max2, maxTime2);
		int lmin2 = fm.stringWidth(strmin2), lmax1 = fm.stringWidth(strmax1), lmax2 = fm.stringWidth(strmax2), leftTextFieldW = (lmin2 > lmax2) ? lmin2 : lmax2;
		mx1 = minMarg + leftTextFieldW;
		mx2 = minMarg;
		if (isZoomable) {
			mx2 += vertFocuser.getRequiredWidth(g); //right position
		}

		my1 = minMarg;
		my2 = minMarg + asc;
		if (isZoomable) {
			my1 = horFocuser.getRequiredWidth(g); //top position
		}

		if (bounds.width - mx1 - mx2 < Metrics.mm() * 5 || bounds.height - my1 - my2 < Metrics.mm() * 5)
			return;
		width = bounds.width - mx1 - mx2;
		height = bounds.height - my1 - my2;

		drawFocusers(g);

		int x0 = bounds.x + mx1 - PlotObject.rad;
		g.setColor(plotAreaColor);
		g.fillRect(bounds.x + mx1, bounds.y + my1, width, height);
		g.setColor(Color.darkGray);
		g.drawLine(bounds.x + mx1, bounds.y + my1, bounds.x + mx1, bounds.y + my1 + height);
		g.drawLine(bounds.x + mx1, bounds.y + my1 + height, bounds.x + mx1 + width, bounds.y + my1 + height);
		g.setColor(Color.gray);
		int p = width / 10;
		for (int i = 1; i <= 10; i++) {
			g.drawLine(bounds.x + mx1 + p, bounds.y + my1, bounds.x + mx1 + p, bounds.y + my1 + height);
			if (i < 10) {
				p += (width - p) / (10 - i);
			}
		}
		p = 0;
		for (int i = 0; i <= 9; i++) {
			g.drawLine(bounds.x + mx1, bounds.y + my1 + p, bounds.x + mx1 + width, bounds.y + my1 + p);
			p += (height - p) / (10 - i);
		}
		g.setColor(Color.blue.darker());
		g.drawString(strmax2, bounds.x + mx1 - lmax2 - 2, bounds.y + my1 + asc);
		g.drawString(strmin2, bounds.x + mx1 - lmin2 - 2, bounds.y + my1 + height);
		g.drawString(strmin1, bounds.x + mx1, bounds.y + my1 + height + asc);
		g.drawString(strmax1, bounds.x + mx1 + width - lmax1, bounds.y + my1 + height + asc);
	}

	protected boolean bShowCorrelation = true;

	protected void showCorrelation(Graphics g) {
		if (!bShowCorrelation)
			return;
		FontMetrics fm = g.getFontMetrics();
		int asc = fm.getAscent(), fh = fm.getHeight(), x0 = bounds.x + mx1 + width + 10, y0 = bounds.y;
		g.setColor(plotAreaColor);
		g.fillRect(x0, y0, width - x0, 2 * asc);
		g.setColor(Color.red);
		g.drawString("C(X,Y):", x0, y0 + asc);
		g.drawString(StringUtil.doubleToStr(correlation, 3), x0, y0 + 2 * asc);
		if (mb != null) {
			double fx1 = min1, fy1 = mb[0] * fx1 + mb[1], fx2 = max1, fy2 = mb[0] * fx2 + mb[1];
			if (mb[0] != 0f && (fy1 > max2 || fy1 < min2)) {
				if (fy1 > max2) {
					fy1 = max2;
					fx1 = (fy1 - mb[1]) / mb[0];
				} else {
					fy1 = min2;
					fx1 = (fy1 - mb[1]) / mb[0];
				}
			}
			if (mb[0] != 0f && (fy2 > max2 || fy2 < min2)) {
				if (fy2 > max2) {
					fy2 = max2;
					fx2 = (fy2 - mb[1]) / mb[0];
				} else {
					fy2 = min2;
					fx2 = (fy2 - mb[1]) / mb[0];
				}
			}
			int iy1 = mapY(fy1), iy2 = mapY(fy2), ix1 = mapX(fx1), ix2 = mapX(fx2);
			g.drawLine(ix1, iy1, ix2, iy2);
		}
	}

	@Override
	public void drawOnlyActive(Graphics g) {
		super.drawOnlyActive(g);
		showCorrelation(g);
	}

	private void drawLimitLine(int attrN, int n, int pos) {
		if (canvas == null)
			return;
		if (attrN != fn1 && attrN != fn2)
			return;
		if (prevLinePos == null) {
			prevLinePos = new int[2];
			prevLinePos[0] = prevLinePos[1] = -1;
		}
		if (pos != prevLinePos[n]) {
			Graphics gr = canvas.getGraphics();
			gr.setColor(Color.magenta);
			gr.setXORMode(plotAreaColor);
			if (attrN == fn1)
				if (prevLinePos[n] >= bounds.x + mx1 && prevLinePos[n] <= bounds.x + mx1 + width) {
					//erase the previous line
					gr.drawLine(prevLinePos[n], bounds.y + my1, prevLinePos[n], bounds.y + my1 + height);
				} else {
					;
				}
			else if (prevLinePos[n] >= bounds.y + my1 && prevLinePos[n] <= bounds.y + my1 + height) {
				//erase the previous line
				gr.drawLine(bounds.x + mx1, prevLinePos[n], bounds.x + mx1 + width, prevLinePos[n]);
			}
			prevLinePos[n] = pos;
			if (attrN == fn1)
				if (prevLinePos[n] >= bounds.x + mx1 && prevLinePos[n] <= bounds.x + mx1 + width) {
					//draw new line
					gr.drawLine(prevLinePos[n], bounds.y + my1, prevLinePos[n], bounds.y + my1 + height);
					if (horFocuser != null) {
						int y = horFocuser.getAxisPosition(), x = (n == 0) ? horFocuser.getMinPos() : horFocuser.getMaxPos();
						if (y < bounds.y + my1) {
							gr.drawLine(x, y, prevLinePos[n], bounds.y + my1);
						} else {
							gr.drawLine(x, y, prevLinePos[n], bounds.y + my1 + height);
						}
					}
				} else {
					;
				}
			else if (prevLinePos[n] >= bounds.y + my1 && prevLinePos[n] <= bounds.y + my1 + height) {
				//draw new line
				gr.drawLine(bounds.x + mx1, prevLinePos[n], bounds.x + mx1 + width, prevLinePos[n]);
				if (vertFocuser != null) {
					int x = vertFocuser.getAxisPosition(), y = (n == 0) ? vertFocuser.getMinPos() : vertFocuser.getMaxPos();
					if (x < bounds.x + mx1) {
						gr.drawLine(x, y, bounds.x + mx1, prevLinePos[n]);
					} else {
						gr.drawLine(x, y, bounds.x + mx1 + width, prevLinePos[n]);
					}
				}
			}
			gr.setPaintMode();
			gr.dispose();
		}
	}

	protected void drawFocusers(Graphics g) {
		if (isZoomable) {
			horFocuser.setAlignmentParameters(bounds.x + mx1, bounds.y + my1, width); //top position
			//horFocuser.setAlignmentParameters(bounds.x+mx1,
			//  bounds.y+bounds.height-horFocuser.getRequiredWidth(g),width); //bottom position
			horFocuser.draw(g);
			vertFocuser.setAlignmentParameters(bounds.x + mx1 + width, bounds.y + my1 + height, height); //right position
			//vertFocuser.setAlignmentParameters(bounds.x+mx1,bounds.y+my1+height,height); //left position
			vertFocuser.draw(g);
		}
	}

	// protected int dragX1=-1, dragY1=-1, dragX2=dragX1, dragY2=dragY1;
	//  protected boolean dragging=false;

	@Override
	public void mouseDragged(MouseEvent e) {
		int x = e.getX(), y = e.getY();
		super.mouseDragged(e);
		if (isZoomable) {
			if (horFocuser.isMouseCaptured()) {
				Graphics g = canvas.getGraphics();
				horFocuser.mouseDragged(x, y, g);
				g.dispose();
				return;
			}
			if (vertFocuser.isMouseCaptured()) {
				Graphics g = canvas.getGraphics();
				vertFocuser.mouseDragged(x, y, g);
				g.dispose();
				return;
			}
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {
		int x = e.getX(), y = e.getY();
		if (isPointInPlotArea(x, y)) {
			dragX1 = dragX2 = x;
			dragY1 = dragY2 = y;
		} else {
			if (isZoomable)
				if (!horFocuser.captureMouse(x, y)) {
					vertFocuser.captureMouse(x, y);
				}
			if (x >= bounds.x + mx1 + width + 10 && y <= bounds.y + my1) {
				bShowCorrelation = !bShowCorrelation;
				plotImageValid = bkgImageValid = false;
				redraw();
			}
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (isZoomable) {
			if (horFocuser.isMouseCaptured()) {
				horFocuser.releaseMouse();
				return;
			}
			if (vertFocuser.isMouseCaptured()) {
				vertFocuser.releaseMouse();
				return;
			}
		}
		super.mouseReleased(e);
	}

	@Override
	public void focusChanged(Object source, double lowerLimit, double upperLimit) {
		if (!(source instanceof Focuser))
			return;
		plotImageValid = bkgImageValid = false;
		Focuser foc = (Focuser) source;
		int attrN = foc.getAttributeNumber();
		if (prevLinePos != null) {
			prevLinePos[0] = prevLinePos[1] = -1;
		}
		if (attrN == fn1) {
			min1 = lowerLimit;
			max1 = upperLimit;
		} else {
			min2 = lowerLimit;
			max2 = upperLimit;
		}
		if (canvas != null) {
			canvas.repaint();
		}
	}

	@Override
	public void limitIsMoving(Object source, int n, double currValue) {
		if (!(source instanceof Focuser))
			return;
		Focuser foc = (Focuser) source;
		if (foc.getAttributeNumber() == fn1) {
			drawLimitLine(fn1, n, mapX(currValue));
		} else if (foc.getAttributeNumber() == fn2) {
			drawLimitLine(fn2, n, mapY(currValue));
		}
	}

}
