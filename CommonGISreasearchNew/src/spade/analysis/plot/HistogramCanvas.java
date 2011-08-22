package spade.analysis.plot;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Vector;

import spade.analysis.classification.Classifier;
import spade.analysis.classification.TableClassifier;
import spade.analysis.system.Supervisor;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.FocusListener;
import spade.lib.basicwin.Focuser;
import spade.lib.basicwin.PopupManager;
import spade.lib.util.GraphGridSupport;
import spade.lib.util.GridPosition;
import spade.lib.util.StringUtil;
import spade.time.TimeMoment;
import spade.vis.action.HighlightListener;
import spade.vis.action.ObjectEvent;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.ObjectFilter;

/**
 * Draws a frequency histogram of distribution of values of a single numeric
 * attribute.
 */
public class HistogramCanvas extends Canvas implements HighlightListener, PropertyChangeListener, Destroyable, MouseMotionListener, MouseListener, FocusListener, spade.vis.event.EventSource {
	/**
	 * The table with data, in particular, with values of the attribute to be
	 * represented by the graph.
	 */
	protected AttributeDataPortion table = null;

	/**
	 * ObjectFilter may be associated with the table and contain results of data
	 * querying. Only data satisfying the current query (if any) are displayed
	 */
	protected ObjectFilter filter = null;

	/**
	 * The supervisor is used for propagating object events among system components
	 */
	protected Supervisor supervisor = null;

	/**
	 * The identifier of the attribute to be visualized
	 */
	protected String attrId = null;

	/**
	 * Absolute minimum and maximum values of the attribute found in the table.
	 * Must be redefined when data in the table change.
	 */
	protected double absMin = Double.NaN, absMax = Double.NaN;
	/**
	 * If the attribute is temporal, these fields contain the absolute minimum and
	 * maximum time moments.
	 */
	protected TimeMoment minTime = null, maxTime = null;

	/**
	 * Indicates the mouse being currently dragged
	 */
	//private boolean drag=false;
	/**
	 * Mouse position at the beginning of dragging
	 */
	private int startX = -1, startY = -1;

	/**
	 * Mouse position at the current moment of of dragging
	 */
	private int lastX = -1, lastY = -1;

	/**
	 * Plot area
	 */
	private Rectangle pa;

	/**
	 *
	 */
	protected double absMaxF = 1000, focusMaxF = 1000;

	/**
	 * Minimum and maximum values shown on the graph (set by a focuser)
	 */
	protected double focusMin = Double.NaN, focusMax = Double.NaN;

	/**
	 * Colors of the background of the canvas and of the plot area
	 */
	public Color bkgColor = Color.white, plotAreaColor = Color.lightGray;

	/**
	 * Indicates "destroyed" state
	 */
	protected boolean destroyed = false;
	protected int instanceN = 0;
	protected static int nInstances = 0;

	/**
	 *
	 */
	protected Focuser vF = null;

	/**
	 *
	 */
	protected Focuser hF = null;

	/**
	 *
	 *
	 */
	protected int count = 10;

	protected long freq[] = null;
	protected long selectedFreq[] = null;
	protected long filteredFreq[] = null;

	protected double step = 0;

	protected boolean changed = true;

	protected Color colorOrder[] = null;
	protected Object colorFreq[] = null;
	protected Object colorSelectedFreq[] = null;
	protected int colorIndex[] = null;
	protected boolean synch = false;
	protected double maxF = 0;

	/**
	 * Used for informing possible listeners about selection of a reference
	 * object or time moment by clicking in the plot.
	 */
	protected PropertyChangeSupport pcSupport = null;

	public static final int SHIFT_UP = 1;
	public static final int SHIFT_DOWN = 2;
	protected PopupManager popM = null;
	protected String popMText = null; // used if addSelectedObjects(point,null)
	protected String lastLimitStr = null;
	protected int lastPos[] = new int[] { -1, -1 };

	public HistogramCanvas() {
		instanceN = nInstances++;
	}

	/**
	 *
	 */
	public void setCount(int count) {
		if (this.count == count)
			return;
		this.count = count;
		changed = true;
		calcFreq();
		repaint();
	}

	public double getMaxF() {
		return maxF;
	}

	public void setMaxF(double maxF) {
		absMaxF = maxF;
		changed = true;
	}

	public void setSynch(boolean synch) {
		this.synch = synch;
		changed = true;
		repaint();

	}

	public int getCount() {
		return count;
	}

	/**
	 * Sets the table with the data, in particular, with values of the attribute to
	 * be represented by the graph.
	 */
	public void setTable(AttributeDataPortion table) {
		this.table = table;
		if (table != null) {
			if (supervisor != null) {
				supervisor.registerHighlightListener(this, table.getEntitySetIdentifier());
			}
			table.addPropertyChangeListener(this);
			filter = table.getObjectFilter();
			if (filter != null) {
				filter.addPropertyChangeListener(this);
			}
		}
	}

	/**
	 * Sets the numeric attribute to be represented on this graph.
	 */
	public void setAttribute(String attrId) {
		this.attrId = attrId;
	}

	/**
	 * Sets a reference to the system's supervisor. The supervisor is used for
	 * propagating object events among system components.
	 */
	public void setSupervisor(Supervisor sup) {
		supervisor = sup;
		if (supervisor != null) {
			supervisor.addPropertyChangeListener(this);
			supervisor.registerObjectEventSource(this);
			if (table != null) {
				supervisor.registerHighlightListener(this, table.getEntitySetIdentifier());
			}

			addMouseListener(this);
			addMouseMotionListener(this);
			if (popM == null) {
				popM = new PopupManager(this, "popup test", true);
				popM.setOnlyForActiveWindow(true);
			}

		}
	}

	/**
	 * Checks whether the object with the given index in the table is active, i.e.
	 * satisfies the filter.
	 */
	protected boolean isActive(int n) {
		if (filter == null)
			return true;
		return filter.isActive(n);
	}

	protected void calcSelectedFreq() {
		if (table == null || attrId == null)
			return;

		if (colorFreq != null) {
			calcColorSelectedFreq();
			return;
		}

		int attrN = table.getAttrIndex(attrId);
		selectedFreq = new long[count];
		for (int i = 0; i < count; i++) {
			selectedFreq[i] = 0;
		}
		Vector so = supervisor.getHighlighter(table.getEntitySetIdentifier()).getSelectedObjects();

		int n = 0;
		double val = 0;

		try {
			for (int i = 0; i < table.getDataItemCount(); i++) {
				val = table.getNumericAttrValue(attrN, i);
				if (Double.isNaN(val)) {
					continue;
				}
				n = (int) ((val - absMin) * count / (absMax - absMin));
				if (n >= selectedFreq.length) {
					n--;
				}
				if (n < selectedFreq.length && n >= 0 && !Double.isNaN(val) && isActive(i)) {

					if (so != null) {
						String id = table.getDataItemId(i);
						for (int j = 0; j < so.size(); j++) {
							String s = (String) so.elementAt(j);
							if (s.equals(id)) {
								selectedFreq[n]++;
								break;
							}
						}
					}

				}
			}
		} catch (ArrayIndexOutOfBoundsException ex) {
			ex.printStackTrace();
		}

	}

	protected Classifier findClassifier() {
		if (supervisor != null && supervisor.getObjectColorer() != null && (supervisor.getObjectColorer() instanceof Classifier) && supervisor.getObjectColorer().getEntitySetIdentifier().equals(table.getEntitySetIdentifier()))
			return (Classifier) supervisor.getObjectColorer();
		return null;
	}

	protected void calcColorSelectedFreq() {
		if (table == null || attrId == null)
			return;
		int attrN = table.getAttrIndex(attrId);

		Classifier cl = findClassifier();
		if (cl == null)
			return;

		TableClassifier tcl = null;
		if (cl instanceof TableClassifier) {
			tcl = (TableClassifier) cl;
			if (!table.equals(tcl.getTable())) {
				tcl = null;
			}
		}

		colorSelectedFreq = new Object[count];
		for (int i = 0; i < count; i++) {
			long cf[] = new long[cl.getNClasses() + 1];
			colorSelectedFreq[i] = cf;
			for (int j = 0; j < cf.length; j++) {
				cf[colorIndex[j]] = 0;
			}
		}

		Vector so = supervisor.getHighlighter(table.getEntitySetIdentifier()).getSelectedObjects();

		int n = 0;
		double val = 0;

		try {
			for (int i = 0; i < table.getDataItemCount(); i++) {
				val = table.getNumericAttrValue(attrN, i);
				if (Double.isNaN(val)) {
					continue;
				}
				n = (int) ((val - absMin) * count / (absMax - absMin));
				if (n >= colorSelectedFreq.length) {
					n--;
				}
				if (n < colorSelectedFreq.length && n >= 0 && !Double.isNaN(val) && isActive(i)) {

					if (so != null) {
						String id = table.getDataItemId(i);
						int classN = (tcl != null) ? tcl.getRecordClass(i) : cl.getObjectClass(id);
						//classId
						for (int j = 0; j < so.size(); j++) {
							String s = (String) so.elementAt(j);
							if (s.equals(id)) {
								long cf[] = (long[]) colorSelectedFreq[n];
								cf[classN + 1]++;
								break;
							}
						}
					}

				}
			}
		} catch (ArrayIndexOutOfBoundsException ex) {
			ex.printStackTrace();
		}

	}

	protected void calcFilteredFreq() {

		if (table == null || attrId == null || filter == null)
			return;
		int attrN = table.getAttrIndex(attrId);
		filteredFreq = new long[count];
		for (int i = 0; i < count; i++) {
			filteredFreq[i] = 0;
		}

		int n = 0;
		double val = 0;

		try {
			for (int i = 0; i < table.getDataItemCount(); i++) {
				val = table.getNumericAttrValue(attrN, i);
				if (Double.isNaN(val)) {
					continue;
				}
				n = (int) ((val - absMin) * count / (absMax - absMin));
				if (n >= filteredFreq.length) {
					n--;
				}
				if (n < filteredFreq.length && n >= 0 && !Double.isNaN(val)) {

					if (!isActive(i)) {
						filteredFreq[n]++;
					}
				}
			}
		} catch (ArrayIndexOutOfBoundsException ex) {
			ex.printStackTrace();
		}

	}

	public void setAbsMinMax(double absMin, double absMax) {
		this.absMin = absMin;
		this.absMax = absMax;
		changed = true;
		calcFreq();
		repaint();
	}

	public void setMinMaxTime(TimeMoment minTime, TimeMoment maxTime) {
		this.minTime = minTime;
		this.maxTime = maxTime;
	}

	protected void calcFreq() {
		/*
		     absMin=Float.NaN; absMax=Float.NaN;
		     if (table==null || attrId==null) return;

		     for (int i=0; i<table.getDataItemCount(); i++) {
		  float val=table.getNumericAttrValue(attrN,i);
		  if (Float.isNaN(val)) continue;
		  if (Float.isNaN(absMin) || val<absMin) absMin=val;
		  if (Float.isNaN(absMax) || val>absMax) absMax=val;
		     }
		 */
		if (Double.isNaN(absMin) || Double.isNaN(absMin) || absMax == absMin)
			return;
		int attrN = table.getAttrIndex(attrId);
		step = (int) ((absMax - absMin) / count);

//    absMaxF = 0;

		// classifier
		if (findClassifier() != null) {
			//freq = null;
			calcColorFreq();
			//return;
		} else {
			colorFreq = null;
		}

		freq = new long[count];

		for (int i = 0; i < count; i++) {
			freq[i] = 0;
		}

		int n = 0;

		double val = 0;
		maxF = 0;

		try {
			for (int i = 0; i < table.getDataItemCount(); i++) {
				val = table.getNumericAttrValue(attrN, i);

				if (Double.isNaN(val)) {
					continue;
				}

				n = (int) ((val - absMin) * count / (absMax - absMin));

				if (n >= freq.length) {
					n--;
				}
				if (n < freq.length && n >= 0 && !Double.isNaN(val)) {
					freq[n]++;
					if (maxF < freq[n]) {
						maxF = freq[n];
					}
				}
			}

		} catch (ArrayIndexOutOfBoundsException ex) {
			ex.printStackTrace();
		}

		if (!synch) {
			absMaxF = maxF;
		}

		//changed = true; //>>>>>>>>>>>>>!!!!<<<<<<<
		//if(changed){
		focusMin = absMin;
		focusMax = absMax;
		focusMaxF = absMaxF;
		// }

		calcSelectedFreq();
		calcFilteredFreq();

		changed = false;

	}

	protected void calcColorFreq() {

		Classifier cl = findClassifier();
		if (cl == null)
			return;

		int attrN = table.getAttrIndex(attrId);
		TableClassifier tcl = null;
		if (cl instanceof TableClassifier) {
			tcl = (TableClassifier) cl;
		}

		colorFreq = new Object[count];

		for (int i = 0; i < count; i++) {
			long cf[] = new long[cl.getNClasses() + 1];
			colorFreq[i] = cf;
			for (int j = 0; j < cf.length; j++) {
				cf[j] = 0;
			}
		}

		colorIndex = new int[cl.getNClasses() + 1];
		for (int i = 0; i < colorIndex.length; i++) {
			colorIndex[i] = i + 1;
		}
		colorIndex[colorIndex.length - 1] = 0;

		colorOrder = new Color[cl.getNClasses() + 1];
		colorOrder[0] = Color.darkGray; // NaN
		for (int i = 1; i < colorOrder.length; i++) {
			colorOrder[i] = (cl.isClassHidden(i - 1)) ? Classifier.hiddenClassColor : cl.getClassColor(i - 1);
		}

		double val = 0;
		int n = 0;

		for (int i = 0; i < table.getDataItemCount(); i++) {
			int recN = i;
			int classN = (tcl != null) ? tcl.getRecordClass(recN) : cl.getObjectClass(table.getDataItemId(recN));
			val = table.getNumericAttrValue(attrN, i);
			if (Double.isNaN(val)) {
				continue;
			}
			n = (int) ((val - absMin) * count / (absMax - absMin));
			if (n >= colorFreq.length) {
				n--;
			}
			if (n < colorFreq.length && n >= 0 && !Double.isNaN(val) && isActive(i)) {
				long cf[] = (long[]) colorFreq[n];
				cf[classN + 1]++;

			}
		}

		changed = false;

	}

	@Override
	public void paint(Graphics g) {
		draw(g);
	}

	/**
	 * Unlike the standard "paint", the method "draw" can be called directly,
	 * i.e. not through an event. This is done for simplifying redrawing when
	 * data/filter/classification/selection changes occur.
	 */
	public void draw(Graphics g) {
		if (g == null)
			return;
		//changed = true; //>>>>>
		if (changed) {
			calcFreq();
		}
		Dimension d = getSize();
		FontMetrics fm = g.getFontMetrics();
		int asc = fm.getAscent();

		g.setColor(bkgColor);
		g.fillRect(0, 0, d.width, d.height);

		if (Double.isNaN(absMin) || Double.isNaN(absMax)) {
			g.drawString("No numeric values found!", 10, d.height / 2);
			return;
		}
		if (absMin >= absMax) {
			g.drawString("All values are the same: " + absMin + "!", 10, d.height / 2);
			return;
		}

		if (hF == null) {
			hF = new Focuser();
			hF.setSpacingFromAxis(-1);
			hF.setIsLeft(false); //right position
			hF.setBkgColor(bkgColor);
			hF.setPlotAreaColor(plotAreaColor);
			hF.addFocusListener(this);
		}

		if (vF == null) {
			vF = new Focuser();
			vF.setIsVertical(true);
			vF.setSpacingFromAxis(-1);
			vF.setIsLeft(false); //right position
			vF.setBkgColor(bkgColor);
			vF.setPlotAreaColor(plotAreaColor);
			vF.setSingleDelimiter("top");
			vF.addFocusListener(this);
		}

		int rw = fm.stringWidth(StringUtil.doubleToStr(absMaxF, 3)) + 2;
		int lw = fm.stringWidth(String.valueOf((int) absMaxF)) + 2;
		int lw1 = fm.stringWidth("00.000") + 2;
		if (lw1 > lw) {
			lw = lw1;
		}

		pa = new Rectangle(lw, 6, d.width - rw - lw, d.height - hF.getBounds().height - asc - asc / 2);
		g.setColor(plotAreaColor);
		g.fillRect(pa.x, pa.y, pa.width, pa.height);
		Rectangle b = pa;
		vF.setAbsMinMax(0, absMaxF);
		vF.setCurrMinMax(0, focusMaxF);
		vF.setAlignmentParameters(b.x + b.width, b.y + b.height - 2, b.height - 2); //right position
		vF.draw(g);

		hF.setAbsMinMax(absMin, absMax);
		hF.setAbsMinMaxTime(minTime, maxTime);
		hF.setCurrMinMax(focusMin, focusMax);
		//
		hF.setAlignmentParameters(b.x, b.y + b.height, b.width); //right position
		hF.draw(g);

		drawInHistArea(g);

	}

	public void drawInHistArea(Graphics g) {
		if (g == null || pa == null)
			return;
		Dimension d = getSize();

		Rectangle b = pa;

		FontMetrics fm = g.getFontMetrics();
		String str = String.valueOf(Math.round(focusMaxF));

		g.setColor(Color.black);
		g.drawString(str, b.x - fm.stringWidth(str) - 2, b.y + fm.getAscent());

		g.setClip(0, 0, d.width, d.height);
		int asc = fm.getAscent(), fh = fm.getHeight();
		GridPosition grpos[] = GraphGridSupport.makeGrid(0, (float) focusMaxF, 0, b.height, 3 * fh, 5 * fh);
		if (grpos != null) {
			g.setColor(Color.getHSBColor(0.7f, 0.3f, 0.85f));
			for (GridPosition grpo : grpos) {
				int gy = b.y + b.height - grpo.offset;
				g.drawLine(b.x, gy, b.x + b.width - 1, gy);

				if (gy < b.y + b.height - fh - 1 && gy > b.y + fh + 1) {
					int sw = fm.stringWidth(grpo.strVal);
					g.drawString(grpo.strVal, b.x - sw - 2, gy + asc / 2);
				}
			}
		}

		g.setClip(b.x, b.y, b.width, b.height);

		boolean sp = false;
		int lh = 0, rh = 0;

		int len = colorFreq == null ? freq.length : colorFreq.length;
		for (int i = 0; i < len; i++) {
			double x0 = absMin + (absMax - absMin) * i / count;
			double x1 = absMin + (absMax - absMin) * (i + 1) / count;

			int x = (int) (b.x + b.width * (x0 - focusMin) / (focusMax - focusMin));
			int w = (int) (b.x + b.width * (x1 - focusMin) / (focusMax - focusMin)) - x;

			if (i == 0 && w > 5) {
				sp = true;
			}
			if (sp) {
				w--;
			}

			int h = 0;
			int y = 0;

			if (colorFreq == null) {
				h = (int) ((b.height * freq[i]) / focusMaxF);
				y = b.y + b.height - h;
				// s/w base-segment
				g.setColor(Color.gray);
				if (h > 1) {
					g.fillRect(x, y, w, h);
				} else if (h <= 1 && freq[i] > 0) {
					g.drawLine(x, pa.y + pa.height - 2, x + w - 1, pa.y + pa.height - 2);
				} else if (freq[i] == 0) {
					g.setColor(Color.white);
					g.drawLine(x, pa.y + pa.height - 2, x + w - 1, pa.y + pa.height - 2);
				}

			} else { // is color
				int hh = 0;
				for (int j = 0; j < colorOrder.length; j++) {
					long cf[] = (long[]) colorFreq[i];
					long ccf[] = (long[]) colorSelectedFreq[i];
					if (cf[colorIndex[j]] > 0) {
						h = (int) ((b.height * cf[colorIndex[j]]) / focusMaxF);
						if (h == 0) {
							h = 1; //~MO 07.09.04
						}
						y = b.y + b.height - h - hh - 1; //~MO 07.09.04
						hh += h;

						g.setColor(colorOrder[colorIndex[j]]);

						if (h > 1) {
							g.fillRect(x, y, w, h);
						} else if (h <= 1 && cf[colorIndex[j]] > 0) {
							g.drawLine(x, y, x + w - 1, y); //~MO 07.09.04
						}
						//  g.drawLine(x, pa.y + pa.height - 2, x + w - 1,
						//             pa.y + pa.height - 2);
						// select
						if (ccf[colorIndex[j]] > 0) {
							int ch = (int) ((b.height * ccf[colorIndex[j]]) / focusMaxF);
							fillVerHatchRect(g, x, y + h - ch, w, ch);
						}

					}
				}

				int th = (int) ((b.height * filteredFreq[i]) / focusMaxF);
				g.setColor(Color.getHSBColor(0.0f, 0.0f, 0.87f));
				if (th > 1) {
					g.fillRect(x, b.y + b.height - hh - th, w, th);
				} else if (th <= 1 && filteredFreq[i] > 0) {
					g.drawLine(x, pa.y + pa.height - 2, x + w - 1, pa.y + pa.height - 2);
				}

				long ssf = 0;
				long cf[] = (long[]) colorFreq[i];
				for (long element : cf) {
					ssf += element;
				}

				if (ssf == 0) {
					g.setColor(Color.white);
					g.drawLine(x, pa.y + pa.height - 2, x + w, pa.y + pa.height - 2);
				}

			} // end of colorFreq

			// filtered, no color
			if (filteredFreq != null && colorFreq == null) {
				//th = h -(int)((b.height*filteredFreq[i])/focusMaxF);
				//ty = b.y + b.height - h;
				int th = (int) ((b.height * filteredFreq[i]) / focusMaxF);
				g.setColor(Color.getHSBColor(0.0f, 0.0f, 0.87f));
				if (th > 1) {
					g.fillRect(x, y, w, th);
				} else if (th <= 1 && filteredFreq[i] > 0) {
					g.drawLine(x, pa.y + pa.height - 2, x + w - 1, pa.y + pa.height - 2);
				}
			}

			// select - no color
			if (colorFreq == null) {
				int sh = (int) ((b.height * selectedFreq[i]) / focusMaxF);
				int sy = b.y + b.height - sh;
				g.setColor(Color.black);
				if (sh > 1) {
					g.fillRect(x, sy, w, sh);
				} else if (sh <= 1 && selectedFreq[i] > 0) {
					g.drawLine(x, pa.y + pa.height - 2, x + w - 1, pa.y + pa.height - 2);
				}

				//}else{
				/////
				//}
			}

			// red top line
			if (freq[i] > focusMaxF) {
				g.setColor(Color.red);
				g.fillRect(x, b.y, w, 2);
			}
			// plot area frame
			g.setColor(Color.darkGray);
			g.drawRect(b.x, b.y, b.width - 1, b.height - 1);
			g.setColor(Color.red);

			if (hF.getMinPos() > b.x) {
				g.fillRect(b.x, b.y, 2, b.height);
			}
			if (hF.getMaxPos() < b.x + b.width) {
				g.fillRect(b.x + b.width - 1 - 1, b.y, 2, b.height);
			}
		}

	}

	/**
	 * Used when data/filter/classification/selection changes occur.
	 */
	public void redraw() {
		draw(getGraphics());
	}

	public void setCurrMinMax(double lower, double upper) {
		hF.setCurrMinMax(lower, upper);
	}

	public void setCurrMaxF(double lower, double upper) {
		vF.setCurrMinMax(lower, upper);
	}

	/**
	 * Registers a listener to be informed about selection of a reference
	 * object or time moment by clicking in the plot.
	 */
	@Override
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		if (listener == null)
			return;
		if (pcSupport == null) {
			pcSupport = new PropertyChangeSupport(this);
		}
		pcSupport.addPropertyChangeListener(listener);
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(300, 200);
	}

//----------------------- spade.lib.basicwin.FocusListener interface ------------
	@Override
	public void focusChanged(Object source, double lowerLimit, double upperLimit) {
		double limit[] = new double[2];
		limit[0] = lowerLimit;
		limit[1] = upperLimit;
		if (source.equals(hF)) {
			if (focusMin != lowerLimit || focusMax != upperLimit) {
				focusMin = lowerLimit;
				focusMax = upperLimit;
				pcSupport.firePropertyChange("curr_min_max", "", limit);
				//repaint();
				redraw();

			}

		} else if (source.equals(vF)) {
			double ul = Math.round(upperLimit);
			if (ul < 1) {
				ul = 1;
			}
			if (focusMaxF != ul) {
				//drawVerLimit(-1,null);
				focusMaxF = ul;
				pcSupport.firePropertyChange("curr_max_f", "", limit);
				//repaint();
				redraw();

			} else {
				redraw();
			}

			lastPos[0] = lastPos[1] = -1;
			lastLimitStr = null;

		}

	}

	public void drawHorLimit(int n, int xpos) {
		Graphics g = getGraphics();
		g.setColor(Color.magenta);
		g.setXORMode(plotAreaColor);

		if (lastPos[n] >= 0) {
			g.drawLine(lastPos[n], pa.y + pa.height, lastPos[n], pa.y);
		}

		if (xpos >= pa.x && xpos < pa.x + pa.width) {
			g.drawLine(n == 0 ? hF.getMinPos() : hF.getMaxPos(), hF.getAxisPosition(), xpos, pa.y + pa.height);
			g.drawLine(xpos, pa.y + pa.height, xpos, pa.y);
			lastPos[n] = xpos;
		}
	}

	public void drawVerLimit(int ypos, String str) {
		Graphics g = getGraphics();
		g.setColor(Color.magenta);
		g.setXORMode(plotAreaColor);
		FontMetrics fm = g.getFontMetrics();

		if (lastPos[0] >= 0) {
			g.drawLine(pa.x, lastPos[0], pa.x + pa.width, lastPos[0]);
			if (lastLimitStr != null) {
				drawLimitStr(g, lastLimitStr, lastPos[0]);
			}
		}

		if (str == null)
			return;

		if (ypos >= pa.y && ypos < pa.y + pa.height) {
			g.drawLine(vF.getAxisPosition(), vF.getMaxPos(), pa.x + pa.width, ypos);
			g.drawLine(pa.x, ypos, pa.x + pa.width, ypos);
			drawLimitStr(g, str, ypos);
			lastPos[0] = ypos;
			lastLimitStr = str;
		} else {
			drawLimitStr(g, str, pa.y);
			lastPos[0] = pa.y;
			lastLimitStr = str;
		}

//      g.drawLine(pa.x,ypos,pa.x+pa.width,ypos);

	}

	protected void drawLimitStr(Graphics g, String str, int y) {
		Color c = g.getColor();
		g.setXORMode(plotAreaColor);
		FontMetrics fm = g.getFontMetrics();
		g.setColor(Color.black);
		g.drawString(str, pa.x + pa.width - fm.stringWidth(str) - 2, y + (y > pa.y + pa.height / 2 ? -2 : +fm.getHeight() + 2));
		g.setColor(Color.white);
		g.drawString(str, pa.x + pa.width - fm.stringWidth(str) - 2 + 1, y + (y > pa.y + pa.height / 2 ? -2 : +fm.getHeight() + 2) + 1);
		g.setColor(c);
	}

	@Override
	public void limitIsMoving(Object source, int n, double currValue) {

		if (source.equals(vF)) {
			int y = pa.y + (int) Math.round((pa.height * (focusMaxF - currValue)) / (focusMaxF));
			drawVerLimit(y, StringUtil.doubleToStr(currValue, focusMin, focusMax));
		} else if (source.equals(hF)) {
			int x = pa.x + (int) Math.round((pa.width * (currValue - focusMin)) / (focusMax - focusMin));
			drawHorLimit(n, x);

		}

	}

	protected void fillVerHatchRect(Graphics g, int x, int y, int w, int h) {
		//if(h == 0) return;
		g.setColor(Color.black);
		for (int i = 0; i < w; i += 2) {
			g.drawLine(x + i, y, x + i, y + h);
		}

	}

	public void shiftColors(int op) {

		if (colorIndex == null)
			return;
		if (op == SHIFT_UP) {

			int n = colorIndex[colorIndex.length - 2];
			for (int i = colorIndex.length - 2; i > 0; i--) {
				colorIndex[i] = colorIndex[i - 1];
			}
			colorIndex[0] = n;

		} else if (op == SHIFT_DOWN) {

			int n = colorIndex[0];
			for (int i = 0; i < colorIndex.length - 2; i++) {
				colorIndex[i] = colorIndex[i + 1];
			}
			colorIndex[colorIndex.length - 2] = n;
		}
		drawInHistArea(getGraphics());

	}

	protected void dragSelect(int x, int y) {
		if (startX < 0) {
			startX = x;
			startY = y;
		} else {
			if (lastX >= 0) {
				drawDragSelect(lastX, lastY);
			}
			lastX = x;
			lastY = y;
			drawDragSelect(lastX, lastY);

		}

	}

	protected void finishDragSelect(MouseEvent e) {
		if (startX < 0) {
			drawDragSelect(lastX, lastY);
			startX = -1;
			startY = -1;
			lastX = -1;
			lastY = -1;
			return;
		}
		drawDragSelect(lastX, lastY);
		double a[] = this.calcBarData(startX < e.getX() ? startX : e.getX());
		double b[] = this.calcBarData(startX < e.getX() ? e.getX() : startX);

		ObjectEvent oevt = new ObjectEvent(this, ObjectEvent.click, e, table.getEntitySetIdentifier());
		int attrN = table.getAttrIndex(attrId);
		if (attrN < 0)
			return;

		for (int i = 0; i < table.getDataItemCount(); i++) {
			double val = table.getNumericAttrValue(attrN, i);
			if (Double.isNaN(val)) {
				continue;
			}
			int n = (int) ((val - absMin) / (absMax - absMin) * count);
			if (n <= 0 || val <= absMin) {
				n = 0;
			}
			if (n >= count - 1 || val >= absMax) {
				n = count - 1;
			}
			if (n >= (int) a[2] && n <= (int) b[2]) {
				oevt.addEventAffectedObject(table.getDataItemId(i));
			}
		}

		supervisor.processObjectEvent(oevt);

		startX = -1;
		startY = -1;
		lastX = -1;
		lastY = -1;
	}

	public String numToString(double num, double min, double max) {
		if (minTime == null)
			return StringUtil.doubleToStr(num, min, max);
		TimeMoment t = minTime.valueOf((long) num);
		if (t == null)
			return minTime.toString();
		return t.toString();
	}

	protected void drawDragSelect(int x, int y) {
		if (startX < 0 || x < 0)
			return;

		Graphics g = getGraphics();

		g.setColor(Color.magenta);
		g.setXORMode(plotAreaColor);

		int x1 = calcMidX(startX);
		int x2 = calcMidX(x);

		g.drawLine(x1, startY, x2, startY);
		g.drawLine(x1, startY + 4, x1, startY - 4);
		g.drawLine(x2, startY + 4, x2, startY - 4);
		double a[] = calcBarData(startX);
		double b[] = calcBarData(x);

		String str = null;

		if (startX <= x) {
			str = numToString(a[0], focusMin, focusMax) + " ... " + numToString(b[1], focusMin, focusMax);
		} else {
			str = numToString(b[0], focusMin, focusMax) + " ... " + numToString(a[1], focusMin, focusMax);
		}

		g.setColor(Color.black);
		g.drawString(str, (x1 + x2) / 2 - g.getFontMetrics().stringWidth(str) / 2 + 1, startY - 14 + 1);
		g.setColor(Color.white);
		g.drawString(str, (x1 + x2) / 2 - g.getFontMetrics().stringWidth(str) / 2, startY - 14);
	}

	/**
	 * calculate middle position (x-coodinate) of corresponded
	 * bar by point
	 * @param x int
	 * @return int
	 */
	protected int calcMidX(int x) {
		Rectangle b = pa;

		double w = (absMax - absMin) / count; // block-width in units
		double u = b.width / (focusMax - focusMin); // unit  in pixels
		double d = ((focusMin - absMin) * u + x - b.x) / (w * u); // offset in blocks
		d = Math.round(d - 0.5); // round
		return (int) ((d * w * u) - (focusMin - absMin) * u + b.x + w * u / 2);

	}

	/**
	 *
	 * @param x int
	 * @return double[] 0-min value, 1-max value, 2-middle of bar abs in pixels
	 */
	protected double[] calcBarData(int x) {
		Rectangle b = pa;
		double vals[] = new double[3];
		double w = (absMax - absMin) / count; // block-width in units
		double u = b.width / (focusMax - focusMin); // unit  in pixels
		double d = ((focusMin - absMin) * u + x - b.x) / (w * u); // offset in blocks
		d = Math.round(d - 0.5);
		vals[0] = d * w + absMin;
		vals[1] = vals[0] + w;
		//vals[2] = (d*w*u)  - (focusMin-absMin)*u + b.x + w*u/2;
		vals[2] = d;

		return vals;
	}

//----------------------- MouseMotionListener interface --------------------------
	/*
	   private int lastFreqPos =0;
	   private int lastVFPos = 0;
	   private int lastValuePos = 0;
	   private int lastFocusPos = 0;
	   private int lastValueMaxPos = 0;
	   private int lastFocusMaxPos = 0;
	 */

	@Override
	public void mouseDragged(MouseEvent e) {

		if (hF.captureMouse(e.getX(), e.getY())) {
			hF.mouseDragged(e.getX(), e.getY(), getGraphics());
			return;
		} else if (vF.captureMouse(e.getX(), e.getY())) {
			vF.mouseDragged(e.getX(), e.getY(), getGraphics());
			return;
		} else if (pa.contains(e.getX(), e.getY())) {
			dragSelect(e.getX(), e.getY());
		}

	}

	protected int findInterval(Point point, double vals[]) {
		boolean sp = false;
		double x0 = Double.NaN;
		double x1 = Double.NaN;
		int k = -1;

		for (int i = 0; i < freq.length; i++) {

			x0 = absMin + (absMax - absMin) * i / count;
			x1 = absMin + (absMax - absMin) * (i + 1) / count;
			Rectangle b = pa;
			int x = (int) (b.x + b.width * (x0 - focusMin) / (focusMax - focusMin));
			int w = (int) (b.x + b.width * (x1 - focusMin) / (focusMax - focusMin)) - x;

			if (i == 0 && w > 5) {
				sp = true;
			}

			int h = (int) ((b.height * freq[i]) / focusMaxF);
			int y = b.y + b.height - h;

			//if(freq[i] > focusMaxF){
			if (y <= b.y) {
				y = b.y;
				h = b.height;
			}

			if (sp) {
				w--;
			}

			Rectangle r = new Rectangle(x, y, w, h);
			if (h < 3) {
				r.y -= 3;
				r.height += 3;
			}
			if (r.contains(point)) {

				k = i;
				break;
			}
		}
		if (vals != null && vals.length > 1) {
			vals[0] = x0;
			vals[1] = x1;
		}

		return k;
	}

	/**
	 *  this method is used for the PopupWindow and sets the text into popMText-variable if parameter "oevt" is null,
	 *  else "oevt" has identifiers of selected objects.
	 */

	protected void addSelectedObjects(Point point, ObjectEvent oevt) {
		if (colorFreq != null) {
			addColorSelectedObjects(point, oevt);
			return;
		}

		if (table == null || attrId == null)
			return;
		int attrN = table.getAttrIndex(attrId);
		double vals[] = new double[2];
		int k = findInterval(point, vals);

		//boolean sp = false;
		double x0 = vals[0]; //Float.NaN;
		double x1 = vals[1]; //Float.NaN;

		if (Double.isNaN(x0) || Double.isNaN(x1) || k < 0) {
			popMText = null;
			return;
		}

		// k - interval nummer

		popMText = "(" + numToString(x0, absMin, absMax) + "," + numToString(x1, absMin, absMax) + "]\n";
		int nt = 0;
		for (int i = 0; i < table.getDataItemCount(); i++) {
			double val = table.getNumericAttrValue(attrN, i);
			if (Double.isNaN(val)) {
				continue;
			}

			int n = (int) ((val - absMin) * count / (absMax - absMin));
			if (n >= freq.length) {
				n--;
			}
			if (n < freq.length && n >= 0 && !Double.isNaN(val)) {
				if (n == k) { //
					if (oevt != null) {
						oevt.addEventAffectedObject(table.getDataItemId(i));
					} else {
						if (nt < 10 && nt > 0) {
							popMText += "; ";
						}
						if (nt < 10) {
							popMText += table.getDataItemName(i);
						}
						nt++;
					}
				}
			}

		}

		if (nt == 0) {
			popMText = null;
		} else {
			if (nt >= 10) {
				popMText += " ... (" + nt + " in total)\n";
			} else {
				popMText += "\n";
			}
			if (selectedFreq != null && selectedFreq[k] > 0) {
				popMText += selectedFreq[k] + " of them are selected\n";
			}
			popMText += "Click to select all";
		}

	}

	protected void addColorSelectedObjects(Point point, ObjectEvent oevt) {
		if (table == null || attrId == null)
			return;

		Classifier cl = findClassifier();
		if (cl == null)
			return;
		int attrN = table.getAttrIndex(attrId);
		TableClassifier tcl = null;
		if (cl instanceof TableClassifier) {
			tcl = (TableClassifier) cl;
		}

		boolean sp = false;
		double x0 = Double.NaN;
		double x1 = Double.NaN;

		int k = -1;
		int kk = -1;
		br: for (int i = 0; i < colorFreq.length; i++) {

			x0 = absMin + (absMax - absMin) * i / count;
			x1 = absMin + (absMax - absMin) * (i + 1) / count;
			Rectangle b = pa;
			int x = (int) (b.x + b.width * (x0 - focusMin) / (focusMax - focusMin));
			int w = (int) (b.x + b.width * (x1 - focusMin) / (focusMax - focusMin)) - x;

			if (i == 0 && w > 5) {
				sp = true;
			}

			long cf[] = (long[]) colorFreq[i];
			int hh = 0;
			for (int j = 0; j < cf.length; j++) {
				if (cf[colorIndex[j]] > 0) {

					int h = (int) ((b.height * cf[colorIndex[j]]) / focusMaxF);
					int y = b.y + b.height - h - hh;
					hh += h;

					//

					//if(freq[i] > focusMaxF){
					if (y <= b.y) {
						y = b.y;
						h = b.height;
					}

					if (sp) {
						w--;
					}

					Rectangle r = new Rectangle(x, y, w, h);
					if (h < 3) {
						r.y -= 3;
						r.height += 3;
					}
					if (r.contains(point)) {
						k = i;
						kk = colorIndex[j];
						break br;
					}
				}

			} // for j
				// if(k >= 0) break;
		} // for i

		if (Double.isNaN(x0) || Double.isNaN(x1) || k < 0)
			return;

		popMText = "(" + numToString(x0, absMin, absMax) + "," + numToString(x1, absMin, absMax) + "]\n";
		int nt = 0;

		for (int i = 0; i < table.getDataItemCount(); i++) {
			double val = table.getNumericAttrValue(attrN, i);
			if (Double.isNaN(val)) {
				continue;
			}
			String id = table.getDataItemId(i);
			int classN = (tcl != null) ? tcl.getRecordClass(i) : cl.getObjectClass(id);
			if ((val >= x0 && val < x1 && k < colorFreq.length - 1 && kk == classN + 1) || (val >= x0 && val <= x1 && k == colorFreq.length - 1 && kk == classN + 1)) {
				if (oevt != null) {
					oevt.addEventAffectedObject(id);
				} else {

					if (nt < 10 && nt > 0) {
						popMText += "; ";
					}
					if (nt < 10) {
						popMText += table.getDataItemName(i);
					}
					nt++;

				}
			}

		}
		if (nt == 0) {
			popMText = null;
		} else {
			if (nt >= 10) {
				popMText += " ... (" + nt + " in total)\n";
			} else {
				popMText += "\n";
			}

			if (colorSelectedFreq != null && colorSelectedFreq[k] != null) {
				long ccf[] = (long[]) colorSelectedFreq[k];
				if (ccf[kk] > 0) {
					popMText += ccf[kk] + " of them are selected\n";
				}
			}
			popMText += "Click to select all";
		}

	}

	//----------------------- MouseListener interface --------------------------

	@Override
	public void mouseMoved(MouseEvent e) {
		if (popM == null)
			return;

		if (pa == null)
			return;
		if (!pa.contains(e.getPoint()))
			return;
		addSelectedObjects(e.getPoint(), null);
		if (popMText == null) {
			popM.setKeepHidden(true);
			PopupManager.hideWindow();
			return;
		}

		popM.setText(popMText);
		if (popM.getKeepHidden()) {
			popM.setKeepHidden(false);
			popM.startShow(e.getX(), e.getY());
		}

	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (hF.captureMouse(e.getX(), e.getY()))
			return;
		if (vF.captureMouse(e.getX(), e.getY()))
			return;
		if (!pa.contains(e.getPoint()))
			return;

	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (hF.captureMouse(e.getX(), e.getY())) {
			hF.releaseMouse();
			lastPos[0] = lastPos[1] = -1;
			return;
		} else if (vF.captureMouse(e.getX(), e.getY())) {
			vF.releaseMouse();
			lastPos[0] = lastPos[1] = -1;
			lastLimitStr = null;
			return;
		} else if (pa.contains(e.getPoint())) {
			ObjectEvent oevt = new ObjectEvent(this, (e.getClickCount() > 1) ? ObjectEvent.dblClick : ObjectEvent.click, e, table.getEntitySetIdentifier());
			if (startX < 0) {
				addSelectedObjects(e.getPoint(), oevt);
				supervisor.processObjectEvent(oevt);
			}
		}

		if (e.getClickCount() <= 1) {
			finishDragSelect(e);
		}

	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

//----------------------- HighlightListener interface --------------------------
	/**
	 * Called when the set of transiently highlighted objects changes. The argument
	 * "source" is usually a reference to a Highlighter. The argument "setId" is
	 * the identifier of the set the highlighted objects belong to. The argument
	 * "highlighted" is a vector of identifiers of currently highlighted objects.
	 * For marking highlighted objects on a map or graph, white color is used.
	 */
	@Override
	public void highlightSetChanged(Object source, String setId, Vector highlighted) {

	}

	/**
	 * Called when the set of selected (durably highlighted) objects changes.
	 * The argument "source" is usually a reference to a Highlighter. The argument
	 * "setId" is the identifier of the set the highlighted objects belong to. The
	 * argument "highlighted" is a vector of identifiers of currently highlighted
	 * objects. For marking selected objects on a map or graph, black color is used.
	 */
	@Override
	public void selectSetChanged(Object source, String setId, Vector selected) {
		if (!setId.equalsIgnoreCase(table.getEntitySetIdentifier()))
			return; //not relevant
		calcSelectedFreq();
		drawInHistArea(getGraphics());
		//redraw();

	}

//--------------------- PropertyChangeListener interface ------------------------
	/**
	 * Called when one of the following changes occur:
	 * 1) object classification or colors;
	 * 2) filter conditions;
	 * 3) data in the table: new attributes, values of existing attributes, new
	 *    table rows (objects);
	 * 4) the table or filter are destroyed, i.e. removed from the system
	 */
	@Override
	public void propertyChange(PropertyChangeEvent pce) {

		if (pce.getPropertyName().equals(Supervisor.eventObjectColors)) {
			if (table.getEntitySetIdentifier().equals(pce.getNewValue())) {
				//do some actions on the change of the classes or class colors
				calcFreq();
				redraw();
			}
		} else if (pce.getSource().equals(filter)) { //change of the filter
			if (pce.getPropertyName().equals("destroyed")) {
				filter.removePropertyChangeListener(this);
				filter = null;
			} else {
				//do some actions on the change of the filter

				calcFreq();
				drawInHistArea(getGraphics());
				// redraw();
			}
		} else if (pce.getSource().equals(table)) {
			if (pce.getPropertyName().equals("destroyed") || pce.getPropertyName().equals("structure_complete")) {
				destroy();
			} else if (pce.getPropertyName().equals("filter")) {
				//a (new) filter is attached to the table or removed
				if (filter != null) {
					filter.removePropertyChangeListener(this);
				}
				filter = table.getObjectFilter();
				if (filter != null) {
					filter.addPropertyChangeListener(this);
				}
				//do some actions on the change of the filter
				//...//
				drawInHistArea(getGraphics());
			} else if (pce.getPropertyName().equals("values")) { //values of some attribute(s) changed
				Vector v = (Vector) pce.getNewValue(); // list of changed attributes
				if (v != null && v.contains(attrId)) {
					//Values of the attribute represented on this graph have changed.
					//Do some actions on the change of the values:

					calcFreq();
					redraw();
				}
			} else if (pce.getPropertyName().equals("data_added") || pce.getPropertyName().equals("data_removed") || pce.getPropertyName().equals("data_updated")) {
				//Some rows in the table have been added/removed/changed
				//Do some actions on the change of the data:

				changed = true;
				calcFreq();
				redraw();
			}
		}
	}

	@Override
	public boolean doesProduceEvent(String eventId) {
		return eventId != null && (eventId.equals(ObjectEvent.click) || eventId.equals(ObjectEvent.dblClick));

	}

	/**
	 * Returns a unique identifier of the event source (may be produced
	 * automatically, used only internally, not shown to the user).
	 */
	@Override
	public String getIdentifier() {
		return "HistogramCanvas_" + instanceN;
	}

//--------------------------- Destroyable interface ----------------------------
	/**
	 * Stops listening of all events, unregisters itself from object event sources
	 */
	@Override
	public void destroy() {
		supervisor.removeHighlightListener(this, table.getEntitySetIdentifier());
		supervisor.removePropertyChangeListener(this);
		table.removePropertyChangeListener(this);
		if (filter != null) {
			filter.removePropertyChangeListener(this);
		}
		destroyed = true;
	}

	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

//ID
	public double getValuesLimit() {
		return vF.getCurrMax();
	}

	public double getFocusedMin() {
		return hF.getCurrMin();
	}

	public double getFocusedMax() {
		return hF.getCurrMax();
	}

	public void setFocuserParameters(double min, double max, double maxFreq) {
		focusMin = min;
		focusMax = max;
		focusMaxF = maxFreq;
		changed = false;
	}

//~ID
}
