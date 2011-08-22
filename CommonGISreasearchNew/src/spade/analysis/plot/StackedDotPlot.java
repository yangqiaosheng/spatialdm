package spade.analysis.plot;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Enumeration;
import java.util.Vector;

import spade.analysis.classification.Classifier;
import spade.analysis.system.Supervisor;
import spade.lib.basicwin.Focuser;
import spade.lib.basicwin.Metrics;
import spade.lib.basicwin.TImgButton;
import spade.lib.util.Arrays;
import spade.lib.util.Formats;
import spade.lib.util.QSortAlgorithm;
import spade.lib.util.StringUtil;
import spade.vis.action.ObjectEventHandler;

/**
* A class that represents values of single numeric attribute on a dot plot.
* The data to represent are taken from an AttributeDataPortion.
*/

public class StackedDotPlot extends DotPlot implements ActionListener, PropertyChangeListener { //
	public static final int minH = 2 * Metrics.mm();
	/**
	* Used to generate unique identifiers of instances of DotPlot
	*/
	/*protected static int nInstances=0;

	protected boolean isHorizontal=true, focuserLeft=false;
	protected boolean toDrawTexts=true, focuserDrawsTexts=true;;
	/*
	* The number of the field (attribute) to be represented.
	*/
	//protected int fn=-1;

	/**
	 * Jitterplot   */

	private float[][] xpi;
	private int Xpixel;
	private boolean zeroexcluded = false;
	private Vector Jitters = new Vector();
	private Vector attributes;
	//is the plot to be shifted according to some broadcasted class?
	private int shifting = 0;
	private boolean shifted = false;
	//Did some broadcasting occur?
	private boolean cbroadcasted = false;
	//is the plot to be drawn inversed?
	private boolean inverse = false;
	//was inverse activated?
	private boolean inversed = false;
	private int hoehe[] = null;
	private double w1[] = { 0, 0, 0 };
	private double w2[] = { 0, 0, 0 };
	private TImgButton b;

	//Indicator: more space needed
	private int[] X = new int[3];
	private int[] Y = new int[3];

	/**
	* The minimum and maximum values of the field
	*/
	//protected float min=Float.NaN, max=Float.NaN;

	/**
	* Focusers are used for focusing on value subranges of the attributes
	*/
	private Focuser focuser = null;

	@Override
	public Focuser getFocuser() {
		return focuser;
	}

	/**
	* Constructs a DotPlot. The argument isIndependent shows whether
	* this plot is displayed separately and, hence, should be registered at the
	* supervisor as an event source or it is a part of some larger plot.
	* The variable allowSelection shows whether the plot should listen to
	* mouse events and transform them to object selection events.
	* Supervisor provides access of a plot to the Highlighter (common for
	* all data displays) and in this way links together all displays
	* The argument handler is a reference to the component the plot
	* should send object events to. In a case when the plot is displayed
	* independently, the ObjectEventHandler is the supervisor (the supervisor
	* implements this interface). Otherwise, the handler is the larger plot in
	* which this plot is included as a part.
	* The larger plot should implement the ObjectEventHandler interface.
	*/
	public StackedDotPlot(boolean isHorisontal, boolean isIndependent, boolean allowSelection, Supervisor sup, ObjectEventHandler handler, Vector attri, TImgButton bu) {
		super(isHorisontal, isIndependent, allowSelection, sup, handler);
		this.isHorisontal = isHorisontal;
		focuserLeft = isHorisontal;
		b = bu;
		b.setEnabled(findClassifier() != null);
	}

	protected Classifier findClassifier() {
		if (supervisor != null && supervisor.getObjectColorer() != null && (supervisor.getObjectColorer() instanceof Classifier) && supervisor.getObjectColorer().getEntitySetIdentifier().equals(dataTable.getEntitySetIdentifier()))
			return (Classifier) supervisor.getObjectColorer();
		return null;
	}

	@Override
	public void setFieldNumber(int n) {
		fn = n;
	}

	@Override
	public void findMinMax() {
		min = max = Double.NaN;
		for (int i = 0; i < dataTable.getDataItemCount(); i++) {
			double v = dataTable.getNumericAttrValue(fn, i);
			if (!Double.isNaN(v)) {
				if (Double.isNaN(min) || v < min) {
					min = v;
				}
				if (Double.isNaN(max) || v > max) {
					max = v;
				}
			}
		}
	}

	@Override
	public void setMinMax(double minValue, double maxValue) {
		min = minValue;
		max = maxValue;
	}

	/*
	* Resets its internal data, including the array of screen objects.
	* Called when records are added to or removed from the table.
	*/
	@Override
	public void reset() {
		min = max = Double.NaN;
		if (dataTable == null || fn < 0) {
			dots = null;
			return;
		}
		setup();
		if (focuser != null) {
			focuser.setAbsMinMax(min, max);
		}
		redraw();
	}

	@Override
	public void setup() {
		if (dataTable == null || fn < 0)
			return;
		if (Double.isNaN(min) || Double.isNaN(max)) {
			findMinMax();
		}
		if (dots == null || dots.length != dataTable.getDataItemCount()) {
			dots = new DotPlotObject[dataTable.getDataItemCount()];
			for (int i = 0; i < dots.length; i++) {
				dots[i] = new DotPlotObject();
			}
		}
		for (int i = 0; i < dots.length; i++) {
			dots[i].reset();
			dots[i].id = dataTable.getDataItemId(i);
		}
		applyFilter();
		plotImageValid = bkgImageValid = false;
		constructFocuser();
	}

	@Override
	public boolean reloadAttributeData(Vector v) {
		if (v == null)
			return false;
		for (int i = 0; i < v.size(); i++) {
			String AttrID = (String) v.elementAt(i);
			if (AttrID.equals(dataTable.getAttributeId(fn))) {
				double prevMin = min, prevMax = max;
				findMinMax();
				if (focuser != null && (!Formats.areEqual(min, focuser.getAbsMin()) || !Formats.areEqual(max, focuser.getAbsMax()))) {
					//System.out.println("min: "+prevMin+" "+min+", max: "+prggvMax+" "+max);
					setup();
					focuser.setAbsMinMax(min, max);
					//focuser.setCurrMinMax(min,max);
					//focuser.notifyFocusChange();
				}

				else {
					min = prevMin;
					max = prevMax;
				}

				return true;
			}
		}
		return false;
	}

	@Override
	public boolean hasData() {
		return dots != null && !Double.isNaN(min);
	}

	@Override
	public void setFocuserOnLeft(boolean value) {
		focuserLeft = value;
	}

	@Override
	protected void constructFocuser() {
		if (!isZoomable)
			return;
		if (focuser == null) {
			focuser = new Focuser();
			focuser.setAttributeNumber(fn);
			focuser.setIsVertical(!isHorisontal);
			focuser.setAbsMinMax(min, max);
			focuser.setSpacingFromAxis(0);
			focuser.setIsLeft(focuserLeft); //top position
			//focuser.setIsLeft(false); //this is to check bottom position
			focuser.setBkgColor(bkgColor);
			focuser.setPlotAreaColor(plotAreaColor);
			//focuser.toDrawCurrMinMax=!isHorizontal;
			focuser.setToDrawCurrMinMax(!isHorisontal);
			focuser.addFocusListener(this);
			focuser.setTextDrawing(focuserDrawsTexts);
		}
	}

	@Override
	protected void defineAlignment() {
		if (mayDefineAlignment && aligner != null) {
			if (isHorisontal) {
				aligner.setMargins(mx1, mx2, -1, -1);
			} else {
				aligner.setMargins(-1, -1, my2, my1);
			}
		}
	}

	@Override
	protected void drawFocuser(Graphics g) {
		if (focuser == null)
			return;
		if (inverse) {
			if (isZoomable) {
				if (isHorisontal)
					if (focuserLeft) {
						focuser.setAlignmentParameters(bounds.x + mx1, bounds.y + (2 * my1) + (hoehe[0] * PlotObject.dm) + Focuser.mainWidth + Metrics.mm() - height, width); //top position
					} else {
						focuser.setAlignmentParameters(bounds.x + mx1, bounds.y + bounds.height - my2 + (hoehe[0] * PlotObject.dm), width); //bottom position
					}
				else if (focuserLeft) {
					focuser.setAlignmentParameters(bounds.x + mx1, bounds.y + my1 + (hoehe[0] * PlotObject.dm), height); //left position
				} else {
					focuser.setAlignmentParameters(bounds.x + mx1 + width, bounds.y + my1 + height + (hoehe[0] * PlotObject.dm), height); //right position
				}
				focuser.draw(g);
			}
		} else {
			if (isZoomable) {
				if (isHorisontal)
					if (focuserLeft) {
						focuser.setAlignmentParameters(bounds.x + mx1, bounds.y + my1, width); //top position
					} else {
						focuser.setAlignmentParameters(bounds.x + mx1, bounds.y + bounds.height - my2, width); //bottom position
					}
				else if (focuserLeft) {
					focuser.setAlignmentParameters(bounds.x + mx1, bounds.y + my1 + height, height); //left position
				} else {
					focuser.setAlignmentParameters(bounds.x + mx1 + width, bounds.y + my1 + height, height); //right position
				}
				focuser.draw(g);
			}
		}

	}

	private String prevMinStr = null, prevMaxStr = null;

	@Override
	public void setTextDrawing(boolean value) {
		toDrawTexts = value;
	}

	@Override
	public void setFocuserDrawsTexts(boolean value) {
		focuserDrawsTexts = value;
		if (focuser != null) {
			focuser.setTextDrawing(value);
		}
	}

	@Override
	public void drawTexts(Graphics g) {
		if (!toDrawTexts)
			return;
		if (focuser != null) {
			drawTexts(g, focuser.getCurrMin(), focuser.getCurrMax());
		} else {
			drawTexts(g, min, max);
		}
	}

	@Override
	public void drawTexts(Graphics g, double mint, double maxt) {
		if (!toDrawTexts)
			return;
		if (isHorisontal) {
			FontMetrics fm = g.getFontMetrics();
			int fh = fm.getHeight(), asc = fm.getAscent();
			int lmax = (prevMaxStr == null) ? 0 : fm.stringWidth(prevMaxStr);
			g.setColor(Color.lightGray);
			int y;
			if (inverse) {
				y = (focuserLeft) ? bounds.y + height - (hoehe[0] * PlotObject.dm) + asc : bounds.y + asc;
			} else {
				y = (focuserLeft) ? bounds.y + my1 + height + asc : bounds.y + asc;
			}
			if (focuser != null && prevMinStr != null && focuser.MinDelimMoving()) {
				g.drawString(prevMinStr, bounds.x + mx1, y);
			}
			if (focuser != null && prevMaxStr != null && focuser.MaxDelimMoving()) {
				g.drawString(prevMaxStr, bounds.x + mx1 + width - lmax, y);
			}
			//g.fillRect(bounds.x+mx1,bounds.y+my1+height,width,asc);
			g.setColor(Color.blue);
			prevMinStr = StringUtil.doubleToStr(mint, mint, maxt);
			prevMaxStr = StringUtil.doubleToStr(maxt, mint, maxt);
			lmax = fm.stringWidth(prevMaxStr);
			g.drawString(prevMinStr, bounds.x + mx1, y);
			g.drawString(prevMaxStr, bounds.x + mx1 + width - lmax, y);
		} else if (focuser != null) {
			; // drawing is done in the focuser
		} else {
			int x = (focuserLeft) ? mx1 + minH + 2 : 2;
			int asc = g.getFontMetrics().getAscent();
			g.setColor(Color.black);
			g.drawString(StringUtil.doubleToStr(mint, mint, maxt), x, mapY(mint));
			g.drawString(StringUtil.doubleToStr(maxt, mint, maxt), x, mapY(maxt) + asc);
		}
	}

	@Override
	public void draw(Graphics g) {

		if (bounds == null || !hasData())
			return;
		if (isHidden) {
			g.setColor(bkgColor);
			g.fillRect(bounds.x, bounds.y, bounds.width + 1, bounds.height + 1);
			return;
		}
		//Clean Background Canvas
		if (bounds != null) {
			g.setColor(plotAreaColor);
			g.fillRect(bounds.x - 1, bounds.y - 1, bounds.width + 2, bounds.height + 1);
		}

		//construct Jitters
		w1[0] = width;
		w1[1] = focuser.getCurrMax();
		w1[2] = focuser.getCurrMin();

		if (shifted == false && ((Arrays.equals(w2, w1)) == false) || cbroadcasted) {
			hoehe = constructJitters();
			groupJitters(Jitters);
			cbroadcasted = false;
			if ((!inversed) && (inverse)) {
				inverseJitter();
			}
		}

		else if (shifted == false && (Arrays.equals(w2, w1))) {
		} else if (shifted) {
			shiftJitterGroups();
			if (inverse) {
				inverseJitter();
			}
		}
		w2[0] = w1[0];
		w2[1] = w1[1];
		w2[2] = w1[2];

		if (inversed && (!inverse)) {
			inverseJitter();
			inversed = false;
		} else if (inversed && (inverse)) {
			inverseJitter();
			inversed = false;
		}

		constructFocuser();
		FontMetrics fm = g.getFontMetrics();
		int fh = fm.getHeight(), asc = fm.getAscent();
		mx1 = -1;
		mx2 = -1;
		my1 = -1;
		my2 = -1;
		if (!mayDefineAlignment && aligner != null)
			if (isHorisontal) {
				mx1 = aligner.getLeft();
				mx2 = aligner.getRight();
			} else {
				my1 = aligner.getBottom();
				my2 = aligner.getTop();
			}
		if (mx1 < 0 || mx2 < 0) {
			mx1 = mx2 = (isHorisontal) ? minMarg : 0;
			if (isZoomable && !isHorisontal)
				if (focuserLeft) {
					mx1 += focuser.getRequiredWidth(g) + 1; //left position
				} else {
					mx2 += focuser.getRequiredWidth(g) + 1; //right position
				}
		}
		if (my1 < 0 || my2 < 0) {
			my1 = my2 = (isHorisontal) ? 1 : minMarg;
			if (toDrawTexts)
				if (isHorisontal)
					if (focuserLeft) {
						my2 += asc + 1;
					} else {
						my1 += asc + 1;
					}
				else if (!isZoomable && !focuserLeft) {
					String str = StringUtil.doubleToStr(min, min, max);
					int sw = fm.stringWidth(str);
					str = StringUtil.doubleToStr(max, min, max);
					int sw1 = fm.stringWidth(str);
					if (sw < sw1) {
						sw = sw1;
					}
					mx1 += sw;
					if (bounds.width - mx1 - mx2 < minH) {
						mx1 = bounds.width - mx2 - minH;
					}
				}
			if (isZoomable && isHorisontal)
				if (focuserLeft) {
					my1 = focuser.getRequiredWidth(g); //top position
				} else {
					my2 = bounds.height - my1 - minH; //bottom position
					if (my2 < focuser.getRequiredWidth(g)) {
						my2 = focuser.getRequiredWidth(g);
					}
				}
		}
		defineAlignment();
		if (bounds.width - mx1 - mx2 < minH || bounds.height - my1 - my2 < minH)
			return;
		width = bounds.width - mx1 - mx2;

		if (!isHorisontal && width > minH) {
			width = minH;
		}
		height = bounds.height - my1 - my2;
		if (isHorisontal && height > minH) {
			height = minH;
		}

		drawFocuser(g);

		//draw Lines
		int x0 = bounds.x + mx1 - PlotObject.rad;
		if (inverse) {
			g.setColor(plotAreaColor);
			g.fillRect(bounds.x + mx1, bounds.y + my1 + (hoehe[0] * PlotObject.dm), width + 1, height + 1);
			g.setColor(Color.gray);
			g.drawLine(bounds.x + mx1, bounds.y + my1 + (hoehe[0] * PlotObject.dm), bounds.x + mx1, bounds.y + my1 + height + (hoehe[0] * PlotObject.dm));
			g.drawLine(bounds.x + mx1 + width, bounds.y + my1 + (hoehe[0] * PlotObject.dm), bounds.x + mx1 + width, bounds.y + my1 + height + (hoehe[0] * PlotObject.dm));
			g.drawLine(bounds.x + mx1, bounds.y + my1 + (hoehe[0] * PlotObject.dm), bounds.x + mx1 + width, bounds.y + my1 + (hoehe[0] * PlotObject.dm));
			g.drawLine(bounds.x + mx1, bounds.y + my1 + height + (hoehe[0] * PlotObject.dm), bounds.x + mx1 + width, bounds.y + my1 + height + (hoehe[0] * PlotObject.dm));
			g.setColor(Color.gray);
			int p = 0;
			if (isHorisontal) {
				p = width / 10;
				for (int i = 1; i <= 10; i++) {
					g.drawLine(bounds.x + mx1 + p, bounds.y + my1 + (hoehe[0] * PlotObject.dm), bounds.x + mx1 + p, bounds.y + my1 + height + (hoehe[0] * PlotObject.dm));
					if (i < 10) {
						p += (width - p) / (10 - i);
					}
				}
			} else {
				p = 0;
				for (int i = 0; i <= 9; i++) {
					g.drawLine(bounds.x + mx1, bounds.y + my1 + p + (hoehe[0] * PlotObject.dm), bounds.x + mx1 + width, bounds.y + my1 + p + (hoehe[0] * PlotObject.dm));
					p += (height - p) / (10 - i);
				}
			}
			g.drawLine(bounds.x + mx1, bounds.y + my1 + (hoehe[0] * PlotObject.dm), bounds.x + mx1 + width, bounds.y + my1 + (hoehe[0] * PlotObject.dm));
		} else {
			g.setColor(plotAreaColor);
			g.fillRect(bounds.x + mx1, bounds.y + my1, width + 1, height + 1);
			g.setColor(Color.gray);
			g.drawLine(bounds.x + mx1, bounds.y + my1, bounds.x + mx1, bounds.y + my1 + height);
			g.drawLine(bounds.x + mx1 + width, bounds.y + my1, bounds.x + mx1 + width, bounds.y + my1 + height);
			g.drawLine(bounds.x + mx1, bounds.y + my1, bounds.x + mx1 + width, bounds.y + my1);
			g.drawLine(bounds.x + mx1, bounds.y + my1 + height, bounds.x + mx1 + width, bounds.y + my1 + height);
			g.setColor(Color.gray);
			int p = 0;
			if (isHorisontal) {
				p = width / 10;
				for (int i = 1; i <= 10; i++) {
					g.drawLine(bounds.x + mx1 + p, bounds.y + my1, bounds.x + mx1 + p, bounds.y + my1 + height);
					if (i < 10) {
						p += (width - p) / (10 - i);
					}
				}
			} else {
				p = 0;
				for (int i = 0; i <= 9; i++) {
					g.drawLine(bounds.x + mx1, bounds.y + my1 + p, bounds.x + mx1 + width, bounds.y + my1 + p);
					p += (height - p) / (10 - i);
				}
			}
			g.drawLine(bounds.x + mx1, bounds.y + my1, bounds.x + mx1 + width, bounds.y + my1);
		}

		//sort Jitters
		/*if (!((dataTable.getAttrIndex("% of population on medicare"))==-1)){
		  int fnum = dataTable.getAttrIndex("% of population on medicare");
		  sortJitters(Jitters, fnum);
		}
		else System.out.println("Attribut nicht gefunden!");*/

		//change Plotarea according to Jitterheight
		if (isHorisontal) {
			height = height + (hoehe[0] * PlotObject.dm);
		}

		//draw texts and Indicators according to PLotarea
		drawTexts(g);

		int test = 0;
		// draw dots all at once
		for (int i = 0; i < dots.length; i++) {
			if (isPointInPlotArea(dots[i].x, dots[i].y)) {
				if (dots[i].isActive) {
					g.setColor(getColorForPlotObject(i));
					dots[i].draw(g);
				} else {
					g.setColor(filteredColor);
					dots[i].draw(g);
				}
			} else {
				test++;
			}
		}
		/* System.out.println("points not in plotarea: "+test);
		 System.out.println("hoehe: "+hoehe[0]);*/

		drawIndicator(g);
		// draw selected points
		drawAllSelectedObjects(g); //selected objects should not be covered by others
		if (annotationSurfacePresent()) {
			getAnnotationSurface().paint(g);
		}
	}

	public void inverseJitter() {

		if (inverse) {
			for (int i = 0; i < dots.length; i++) {
				int f = dots[i].y - (bounds.y + my1 + Metrics.mm());
				int y = dots[i].y - f + ((hoehe[0] * PlotObject.dm) - f);
				dots[i].reset();
				dots[i].x = mapX(dataTable.getNumericAttrValue(fn, i));
				dots[i].y = y;

			}
		} else if (!inverse) {
			for (int i = 0; i < dots.length; i++) {
				int f = dots[i].y - (bounds.y + my1 + Metrics.mm());
				int y = dots[i].y + ((hoehe[0] * PlotObject.dm) - f) - f;
				dots[i].reset();
				dots[i].x = mapX(dataTable.getNumericAttrValue(fn, i));
				dots[i].y = y;
			}
		}
	}

	public void drawIndicator(Graphics g) {
		FontMetrics fm = g.getFontMetrics();
		int fh = fm.getHeight();
		if (height + my1 + fh > canvas.getSize().height) {
			X[0] = hoehe[1] - 10;
			X[1] = hoehe[1] + 10;
			X[2] = hoehe[1];
			Y[0] = bounds.y + canvas.getSize().height - 20;
			Y[1] = bounds.y + canvas.getSize().height - 20;
			Y[2] = bounds.y + canvas.getSize().height - 20 + 8;
			g.setColor(Color.red);
			g.fillPolygon(X, Y, 3);
			g.setColor(Color.white);
			g.drawPolygon(X, Y, 3);
		}
	}

	public int[] constructJitters() {

		int count = 0;
		int c = dots.length;
		int d = 0;

		// find missing values
		for (int i = 0; i < dots.length; i++) {
			double v = dataTable.getNumericAttrValue(fn, i);
			if (Double.isNaN(v)) {
				c--;
			}
		}

		// Fill 3-dim Array with all pixelvalues; exclude missing values
		xpi = new float[c][3];
		for (int i = 0; i < dots.length; i++) {
			double v = dataTable.getNumericAttrValue(fn, i);
			if (Double.isNaN(v)) {
			} else {
				if (d < c) {
					//Pixelvalue
					xpi[d][0] = mapX(v);
					//Attributevalue
					xpi[d][1] = (float) v;
					//Objectid
					xpi[d][2] = i;
					d++;
				}
			}
		}
		//sort
		QSortAlgorithm.sort(xpi, 1);
		/*for (int i=0;i<xpi.length;i++) {
		  System.out.println(xpi[i][0]+", "+xpi[i][1]+", "+xpi[i][2]);    }*/

		//Iteration through the Array pixel by pixel
		int i = 0;
		int a = 0;
		count = 0;
		Jitters.removeAllElements();
		boolean loop = true;
		boolean add = false;
		//biggest Jitternumber and its xvalue
		int[] h = new int[2];

		while (loop && i < xpi.length) {
			if (i + 1 < xpi.length) {
				a = i + 1;
			} else {
				loop = false;
			}
			double v = xpi[i][1];
			if (zeroexcluded ? v != 0.0 : true) {
				//horisontal plot
				if (isHorisontal) {
					//dotplot
					Xpixel = (int) xpi[i][0];
					dots[(int) xpi[i][2]].reset();
					dots[(int) xpi[i][2]].x = Xpixel;
					dots[(int) xpi[i][2]].y = bounds.y + my1 + Metrics.mm();
					count = 0;
					add = false;
					//If there are duplicate pixelvalues, draw dots as Jitter
					if (loop) {
						Vector jitter = new Vector();
						jitter.addElement(Integer.toString((int) xpi[i][2]));
						while (xpi[i][0] == xpi[a][0]) {
							count++;
							add = true;
							jitter.addElement(Integer.toString((int) xpi[a][2]));
							dots[(int) xpi[a][2]].reset();
							dots[(int) xpi[a][2]].x = Xpixel;
							dots[(int) xpi[a][2]].y = bounds.y + my1 + Metrics.mm() + (count * PlotObject.dm);
							if (h[0] < count && xpi[a][1] <= focuser.getCurrMax() && xpi[a][1] >= focuser.getCurrMin()) {
								h[0] = count;
								h[1] = Xpixel;
							}
							i = a;

							if (i + 1 < xpi.length) {
								a = i + 1;
							} else {
								loop = false;
								a = i;
								break;
							}
						}
						if (add) {
							Jitters.addElement(jitter);
						}

					}
					//If there are no duplicates, draw dot as dotplot
				}
				//vertical plot
				else {
					dots[i].y = mapY(v);
					dots[i].x = bounds.x + mx1 + Metrics.mm(); //mapY(v2);
				}
			}
			//set i to next object (with a different pixelvalue)
			i = a;
		}
		return h;
	}

	private void sortJitters(Vector Jitters, int attr) {
		Enumeration i = Jitters.elements();
		while (i.hasMoreElements()) {
			Vector n = (Vector) i.nextElement();
			Enumeration k = n.elements();
			double[][] s = new double[n.size()][2];
			int a = 0;
			//Jitter as 2-dim array

			while (k.hasMoreElements()) {
				int objid = Integer.parseInt((String) k.nextElement());
				s[a][1] = objid;
				s[a][0] = dataTable.getNumericAttrValue(attr, objid);
				a++;
			}
			QSortAlgorithm.sort(s, 0);

			for (a = 0; a < s.length; a++) {
				int objid = (int) s[a][1];
				dots[objid].reset();
				dots[objid].x = mapX(dataTable.getNumericAttrValue(fn, objid));
				dots[objid].y = bounds.y + my1 + Metrics.mm() + (a * PlotObject.dm);
			}

		}
	}

	private void groupJitters(Vector Jitters) {
		Classifier c = findClassifier();
		if (c == null)
			return;

		Enumeration i = Jitters.elements();
		while (i.hasMoreElements()) {
			Vector n = (Vector) i.nextElement();
			Enumeration k = n.elements();
			double[][] s = new double[n.size()][2];
			int a = 0;
			//Jitter as 2-dim array

			while (k.hasMoreElements()) {
				int objid = Integer.parseInt((String) k.nextElement());
				s[a][1] = objid;
				s[a][0] = c.getObjectClass(dataTable.getDataItemId(objid));
				a++;
			}
			QSortAlgorithm.sort(s, 0);

			for (a = 0; a < s.length; a++) {
				int objid = (int) s[a][1];
				dots[objid].reset();
				dots[objid].x = mapX(dataTable.getNumericAttrValue(fn, objid));
				dots[objid].y = bounds.y + my1 + Metrics.mm() + (a * PlotObject.dm);
			}
		}
	}

	private void shiftJitterGroups() {
		if (inverse) {
			for (int i = 0; i < dots.length; i++) {
				int f = dots[i].y - (bounds.y + my1 + Metrics.mm());
				int y = dots[i].y + ((hoehe[0] * PlotObject.dm) - f) - f;
				dots[i].reset();
				dots[i].x = mapX(dataTable.getNumericAttrValue(fn, i));
				dots[i].y = y;
			}
		}

		Classifier c = findClassifier();
		if (c == null)
			return;

		Enumeration i = Jitters.elements();
		if (shifting + 1 < c.getNClasses()) {
			shifting++;
		} else {
			shifting = 0;
		}

		while (i.hasMoreElements()) {
			Vector n = (Vector) i.nextElement();
			Enumeration k = n.elements();
			double[][] s = new double[n.size()][2];
			int a = 0;
			//Jitter as 2-dim array
			while (k.hasMoreElements()) {
				int objid = Integer.parseInt((String) k.nextElement());
				s[a][1] = objid;
				int d = c.getObjectClass(dataTable.getDataItemId(objid));
				int r = d + shifting;
				s[a][0] = (r < c.getNClasses() ? r : Math.abs(r - c.getNClasses()));
				a++;
			}
			QSortAlgorithm.sort(s, 0);

			for (a = 0; a < s.length; a++) {
				int objid = (int) s[a][1];
				dots[objid].reset();
				dots[objid].x = mapX(dataTable.getNumericAttrValue(fn, objid));
				dots[objid].y = bounds.y + my1 + Metrics.mm() + (a * PlotObject.dm);
			}
			shifted = false;
		}

	}

	@Override
	public int mapX(double v) {
		return bounds.x + mx1 + (int) (Math.round((v - min) / (max - min) * (width / 5)) * 5);
	}

	@Override
	public int mapY(double v) {
		return bounds.y + my1 + height - (int) Math.round((v - min) / (max - min) * height);
	}

	@Override
	public double absX(int x) {
		return min + (max - min) * ((x - mx1 - bounds.x) / 5) / (width * 5);
	}

	@Override
	public double absY(int y) {
		return min + (max - min) * (height - y + my1 + bounds.y) / height;
	}

	@Override
	public Dimension getPreferredSize() {
		int w = minH + 2;
		if (isZoomable) {
			w += Focuser.mainWidth + Metrics.mm() * 2;
			if (focuserDrawsTexts) {
				w += (isHorisontal) ? Metrics.mm() * 3 : Metrics.mm() * 8;
			}
		}
		if (toDrawTexts)
			if (isHorisontal) {
				w += Metrics.mm() * 3;
			} else if (!focuserDrawsTexts) {
				w += Metrics.mm() * 8;
			}
		return (isHorisontal) ? new Dimension(40 * Metrics.mm(), w + height) : new Dimension(w, 40 * Metrics.mm());
	}

	// focuser methods - begin
	private int[] prevLinePos = null;

	private void drawLimitLine(int n, int pos) { // n==0 -> min; n==1 -> max
		if (canvas == null)
			return;
		if (prevLinePos == null) {
			prevLinePos = new int[2];
			prevLinePos[0] = prevLinePos[1] = -1;
		}
		if (pos != prevLinePos[n]) {
			Graphics gr = canvas.getGraphics();
			gr.setColor(Color.magenta);
			gr.setXORMode(plotAreaColor);
			if (isHorisontal) {
				int y1 = bounds.y + my1, y2 = bounds.y + my1 + height, y = 0;
				if (focuser == null) {
					y1 -= Metrics.mm();
					y2 += Metrics.mm();
				} else {
					y = focuser.getAxisPosition();
					if (y > y2) {
						y1 -= Metrics.mm();
					} else {
						y2 += Metrics.mm();
					}
				}
				if (prevLinePos[n] >= bounds.x + mx1 && prevLinePos[n] <= bounds.x + mx1 + width) {
					//erase the previous line
					gr.drawLine(prevLinePos[n], y1, prevLinePos[n], y2);
				}
				prevLinePos[n] = pos;
				if (prevLinePos[n] >= bounds.x + mx1 && prevLinePos[n] <= bounds.x + mx1 + width) {
					// draw new line
					//gr.setColor(Color.blue);
					gr.drawLine(prevLinePos[n], y1, prevLinePos[n], y2);
					/*if (focuser!=null) {
					  //connect the line to the delimiter
					  int x=(n==0)?focuser.getMinPos():focuser.getMaxPos();
					  //gr.setColor(Color.green);
					  if (y<y1)
					    gr.drawLine(x,y,prevLinePos[n],y1);
					  else
					    gr.drawLine(x,y,prevLinePos[n],y2);
					}*/
				}

			} else {
				int x1 = bounds.x + mx1, x2 = bounds.x + mx1 + width, x = 0;
				if (focuser == null) {
					x1 -= Metrics.mm();
					x2 += Metrics.mm();
				} else {
					x = focuser.getAxisPosition();
					if (x > x2) {
						x1 -= Metrics.mm();
					} else {
						x2 += Metrics.mm();
					}
				}
				if (prevLinePos[n] >= bounds.y + my1 && prevLinePos[n] <= bounds.y + my1 + height) {
					//erase the previous line
					gr.drawLine(x1, prevLinePos[n], x2, prevLinePos[n]);
				}
				prevLinePos[n] = pos;
				if (prevLinePos[n] >= bounds.y + my1 && prevLinePos[n] <= bounds.y + my1 + height) {
					// draw new line
					gr.drawLine(x1, prevLinePos[n], x2, prevLinePos[n]);
					if (focuser != null) {
						//connect the line to the delimiter
						int y = (n == 0) ? focuser.getMinPos() : focuser.getMaxPos();
						if (x < x1) {
							gr.drawLine(x, y, x1, prevLinePos[n]);
						} else {
							gr.drawLine(x, y, x2, prevLinePos[n]);
						}
					}
				}
			}
			gr.setPaintMode();
			gr.dispose();
		}
	}

	@Override
	public void focusChanged(Object source, double lowerLimit, double upperLimit) {
		if (isHidden)
			return;
		if (!isZoomable || focuser == null)
			return;
		if (!(source instanceof Focuser))
			return;
		if (prevLinePos != null) {
			prevLinePos[0] = prevLinePos[1] = -1;
		}
		// adjust focuser to a next value
		if (!focuser.getIsUsedForQuery()) {
			if (lowerLimit < min) { // dragging left or down
				double breakVal = focuser.getAbsMin();
				for (int i = 0; i < dataTable.getDataItemCount(); i++) {
					double v = dataTable.getNumericAttrValue(fn, i);
					if (Double.isNaN(v) || v > lowerLimit) {
						continue;
					}
					if (v > breakVal) {
						breakVal = v;
					}
				}
				lowerLimit = breakVal;
			} else { // dragging right or up
				double breakVal = focuser.getAbsMax();
				for (int i = 0; i < dataTable.getDataItemCount(); i++) {
					double v = dataTable.getNumericAttrValue(fn, i);
					if (Double.isNaN(v) || v < lowerLimit) {
						continue;
					}
					if (v < breakVal) {
						breakVal = v;
					}
				}
				lowerLimit = breakVal;
			}
			if (upperLimit < max) { // dragging left or down
				double breakVal = focuser.getAbsMin();
				for (int i = 0; i < dataTable.getDataItemCount(); i++) {
					double v = dataTable.getNumericAttrValue(fn, i);
					if (Double.isNaN(v) || v > upperLimit) {
						continue;
					}
					if (v > breakVal) {
						breakVal = v;
					}
				}
				upperLimit = breakVal;
			} else { // dragging right or up
				double breakVal = focuser.getAbsMax();
				for (int i = 0; i < dataTable.getDataItemCount(); i++) {
					double v = dataTable.getNumericAttrValue(fn, i);
					if (Double.isNaN(v) || v < upperLimit) {
						continue;
					}
					if (v < breakVal) {
						breakVal = v;
					}
				}
				upperLimit = breakVal;
			}
			min = lowerLimit;
			max = upperLimit;
			focuser.setCurrMinMax(lowerLimit, upperLimit);
		}
		if (canvas != null) {
			canvas.repaint();
		}
	}

	@Override
	public void limitIsMoving(Object source, int n, double currValue) {
		if (isHidden)
			return;
		if (!isZoomable || focuser == null)
			return;
		if (canvas == null)
			return;
		if (!(source instanceof Focuser))
			return;
		drawLimitLine(n, (isHorisontal) ? mapX(currValue) : mapY(currValue));
		Graphics g = canvas.getGraphics();
		drawTexts(g, focuser.getCurrMin(), focuser.getCurrMax());
		g.dispose();
	}

	// focuser methods - end

	protected int dragX1 = -1, dragY1 = -1, dragX2 = dragX1, dragY2 = dragY1;
	protected boolean dragging = false;

	@Override
	public void mouseDragged(MouseEvent e) {
		if (isHidden)
			return;
		if (canvas == null)
			return;
		int x = e.getX(), y = e.getY();
		if (isZoomable) {
			if (focuser.isMouseCaptured()) {
				Graphics g = canvas.getGraphics();
				focuser.mouseDragged(x, y, g);
				g.dispose();
				return;
			}
		}
		if (!dragging && !isPointInPlotArea(dragX1, dragY1))
			return;
		dragging = dragging || Math.abs(x - dragX1) > 5 || Math.abs(y - dragY1) > 5;
		if (!dragging)
			return;
		if (x < bounds.x + mx1) {
			x = bounds.x + mx1;
		}
		if (x > bounds.x + mx1 + width) {
			x = bounds.x + mx1 + width;
		}
		if (y < bounds.y + my1) {
			y = bounds.y + my1;
		}
		if (y > bounds.y + my1 + height) {
			y = bounds.y + my1 + height;
		}
		if (x == dragX2 && y == dragY2)
			return;
		drawFrame(dragX1, dragY1, dragX2, dragY2);
		dragX2 = x;
		dragY2 = y;
		drawFrame(dragX1, dragY1, dragX2, dragY2);
		if ((dragX2 - dragX1) * (dragY2 - dragY1) > 0) {
			canvas.setCursor(new Cursor(Cursor.NW_RESIZE_CURSOR));
		} else {
			canvas.setCursor(new Cursor(Cursor.NE_RESIZE_CURSOR));
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand() == "switch") {
			shifted = true;
			redraw();
			return;
		} else if (e.getActionCommand() == "inverse") {
			if (inverse) {
				inverse = false;
			} else {
				inverse = true;
			}
			inversed = true;
			redraw();
			return;
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals(Supervisor.eventObjectColors) && dataTable.getEntitySetIdentifier().equals(evt.getNewValue())) {
			cbroadcasted = true;
			shifted = false;
			shifting = 0;
			b.setEnabled(findClassifier() != null);
			redraw();
		} else {
			super.propertyChange(evt);
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (isHidden)
			return;
		int x = e.getX(), y = e.getY();
		if (isPointInPlotArea(x, y)) {
			dragX1 = dragX2 = x;
			dragY1 = dragY2 = y;
		} else if (isZoomable) {
			focuser.captureMouse(x, y);
			if (focuser.isMouseCaptured()) {
				canvas.setCursor(new Cursor((isHorisontal) ? Cursor.W_RESIZE_CURSOR : Cursor.N_RESIZE_CURSOR));
			}
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (isHidden)
			return;
		if (isZoomable && focuser.isMouseCaptured()) {
			focuser.releaseMouse();
			canvas.setCursor(Cursor.getDefaultCursor());
			return;
		}
		if (!dragging) {
			selectObjectAt(dragX1, dragY1, e);
		} else {
			drawFrame(dragX1, dragY1, dragX2, dragY2);
			dragging = false;
			selectInFrame(dragX1, dragY1, dragX2, dragY2, e);
			canvas.setCursor(Cursor.getDefaultCursor());
		}
		dragX1 = dragY1 = dragX2 = dragY2 = -1;
	}

	@Override
	protected void drawFrame(int x0, int y0, int x, int y) {
		if (canvas == null)
			return;
		Graphics gr = canvas.getGraphics();
		gr.setColor(Color.magenta);
		gr.setXORMode(plotAreaColor);
		gr.drawLine(x0, y0, x, y0);
		gr.drawLine(x, y0, x, y);
		gr.drawLine(x, y, x0, y);
		gr.drawLine(x0, y, x0, y0);
		gr.setPaintMode();
		gr.dispose();
	}

	@Override
	public void destroy() {
		super.destroy();
		if (focuser != null) {
			focuser.destroy();
		}
	}

	@Override
	public Vector getAttributeList() {
		Vector a = null;
		if (dataTable != null && fn > -1) {
			a = new Vector(1, 2);
			a.addElement(dataTable.getAttributeId(fn));
		}
		return a;
	}

	/**
	* Used to generate unique identifiers of instances of Plot's descendants.
	* The base class Plot calls this method in the constructor.
	*/
	@Override
	protected void countInstance() {
		instanceN = ++nInstances;
	}

	/**
	* Returns "Dot_Plot".
	* Used to generate unique identifiers of instances of Plot's descendants.
	* Plot uses this method in the method getIdentifier().
	*/
	@Override
	public String getPlotTypeName() {
		return "stacked_Dot_plot";
	}
}
