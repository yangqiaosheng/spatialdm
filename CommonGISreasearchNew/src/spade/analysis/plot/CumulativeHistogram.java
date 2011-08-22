package spade.analysis.plot;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import spade.analysis.manipulation.Slider;
import spade.analysis.manipulation.SliderListener;
import spade.analysis.system.Supervisor;
import spade.analysis.transform.AttributeTransformer;
import spade.lib.basicwin.Drawable;
import spade.lib.basicwin.Metrics;
import spade.lib.util.IntArray;
import spade.lib.util.NumRange;
import spade.lib.util.StringUtil;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.DataTreater;
import spade.vis.database.ObjectFilter;

/**
* Implementation of the graph type known in statistics as cumulative curve.
* The data to represent are taken from an AttributeDataPortion.
*/

public class CumulativeHistogram implements PropertyChangeListener, Drawable, DataTreater, SliderListener {
	/**
	* The canvas in which this object should draw itself
	*/
	protected Canvas canvas = null;
	/**
	* The boundaries in which the object should fit itself
	*/
	protected Rectangle bounds = null;
	/**
	* The table with the attributes to be shown on the plot
	*/
	protected AttributeDataPortion dataTable = null;
	/**
	 * ObjectFilter may be associated with the table and contain results of data
	 * querying. Only data satisfying the current query (if any) are displayed
	 */
	protected ObjectFilter filter = null;
	/**
	* A Cumulative Curve may optionally be connected to a transformer of attribute
	* values. In this case, it represents transformed attribute values.
	*/
	protected AttributeTransformer aTrans = null;

	protected int fn[] = null; // field numbers
	double min = Double.NaN, max = Double.NaN;
	double x[] = null; // y[]=null;
	Vector vy = new Vector(5, 5);
	double xbreaks[] = null; // ybreaks[]=null;
	Vector vybreaks = new Vector(5, 5);
	double val[] = null; // values of the attribute under classification
	int ro[] = null; // reverse order ...
	double sumVal[] = null;
	int nNonActive = 0; // number of objects non satisying current query (if any)

	protected Supervisor supervisor = null;

	private Color classColors[] = null; // class colors
	/**
	* Indicates whether all internal variables are correctly set
	*/
	protected boolean isValid = false;

	/**
	* Indicates "destroyed" state. Initially is false.
	*/
	protected boolean destroyed = false;

	/**
	* Used to generate unique identifiers and names of instances of CumulativeHistogram
	*/
	protected static int nInstances = 0;
	protected int instanceN = 0;

	public CumulativeHistogram(Supervisor sup) {
		instanceN = ++nInstances;
		supervisor = sup;
	}

	public void setDataSource(AttributeDataPortion tbl) {
		this.dataTable = tbl;
		if (dataTable != null) {
			dataTable.addPropertyChangeListener(this);
			filter = dataTable.getObjectFilter();
			if (filter != null) {
				filter.addPropertyChangeListener(this);
			}
		}
		if (supervisor != null && tbl != null) {
			supervisor.registerDataDisplayer(this);
		}
	}

	/**
	* Connects the display to a transformer of attribute values. After this,
	* it must represent transformed attribute values. The argument listenChanges
	* determines whether the display will listen to the changes of the transformed
	* values and appropriately reset itself. This is not always desirable; for
	* example, a plot may be a part of a map manipulator, which determines
	* itself when and how the plot must change.
	*/
	public void setAttributeTransformer(AttributeTransformer transformer, boolean listenChanges) {
		if (aTrans != null) {
			aTrans.removePropertyChangeListener(this);
		}
		aTrans = transformer;
		if (aTrans != null) {
			if (listenChanges) {
				aTrans.addPropertyChangeListener(this);
			}
		}
	}

	/**
	* Returns its transformer of attribute values (if exists)
	*/
	public AttributeTransformer getAttributeTransformer() {
		return aTrans;
	}

	/**
	* Depending on the presence of an attribute transformer, gets attribute values
	* either from the transformer or the table
	*/
	public double getNumericAttrValue(int attrN, int recN) {
		if (aTrans != null)
			return aTrans.getNumericAttrValue(attrN, recN);
		if (dataTable != null)
			return dataTable.getNumericAttrValue(attrN, recN);
		return Double.NaN;
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

	public void setup(int fn[]) {
		this.fn = fn;
		isValid = false;
	}

	@Override
	public void setCanvas(Canvas c) {
		canvas = c;
	}

	public Canvas getCanvas() {
		return canvas;
	}

	public void setFn(int fn[]) { // set new field numbers
		this.fn = fn;
		reset();
		if (supervisor != null) {
			supervisor.notifyGlobalPropertyChange(Supervisor.eventDisplayedAttrs);
		}
	}

	public void reset() {
		x = null;
		sumVal = null;
		isValid = false;
		if (vybreaks == null) {
			vybreaks = new Vector(5, 5);
		} else {
			vybreaks.removeAllElements();
		}
		forcedDraw();
	}

	public int[] getFn() {
		return fn;
	}

	public boolean reloadAttributeData(Vector v) {
		if (v == null)
			return false;
		for (int i = 0; i < v.size(); i++) {
			String AttrID = (String) v.elementAt(i);
			for (int element : fn)
				if (AttrID.equals(dataTable.getAttributeId(element))) {
					reset();
					return false;
				}
		}
		return false;
	}

	protected int[] getOrderIncreaseIndex(double vals[]) {
		if (vals == null || vals.length == 0)
			return null;
		int index[] = new int[vals.length];
		// count NaN(s)
		int NNaN = 0;
		for (int i = 0; i < vals.length; i++)
			if (Double.isNaN(vals[i])) {
				index[NNaN] = i;
				NNaN++;
			}
		// now looking for max values
		for (int i = 0; i < vals.length - NNaN; i++) {
			double max = Double.NaN;
			int maxN = -1;
			for (int j = 0; j < vals.length; j++)
				if (!Double.isNaN(vals[j]))
					if (Double.isNaN(max) || vals[j] > max) {
						max = vals[j];
						maxN = j;
					}
			if (maxN >= 0) {
				index[index.length - 1 - i] = maxN;
				vals[maxN] = Double.NaN;
			}
		}
		return index;
	}

	public void compute() {
		IntArray colNs = new IntArray(1, 1);
		colNs.addElement(fn[0]);
		NumRange nr = (aTrans != null) ? aTrans.getValueRangeInColumns(colNs) : dataTable.getValueRangeInColumns(colNs);
		min = nr.minValue;
		max = nr.maxValue;
		if (val == null || val.length != dataTable.getDataItemCount()) {
			val = new double[dataTable.getDataItemCount()];
		}
		nNonActive = 0;
		for (int i = 0; i < dataTable.getDataItemCount(); i++)
			if (isActive(i)) {
				val[i] = getNumericAttrValue(fn[0], i);
			} else {
				val[i] = Double.NaN;
				nNonActive++;
			}
		ro = getOrderIncreaseIndex((val.clone()));
		if (vy == null) {
			vy = new Vector(5, 5);
		} else {
			vy.removeAllElements();
		}
		for (int element : fn) {
			double y[] = new double[val.length];
			vy.addElement(y);
		}
		if (x == null || x.length != val.length) {
			x = new double[val.length];
		}
		double[] subSumVal = null;
		if (fn.length > 1) {
			if (sumVal == null || sumVal.length != fn.length - 1) {
				sumVal = new double[fn.length - 1];
				for (int i = 0; i < sumVal.length; i++) {
					sumVal[i] = 0f;
					for (int j = 0; j < dataTable.getDataItemCount(); j++)
						if (isActive(j)) {
							double v = getNumericAttrValue(fn[i + 1], j);
							if (!Double.isNaN(v)) {
								sumVal[i] += v;
							}
						}
				}
			}
			subSumVal = new double[fn.length - 1];
			for (int i = 0; i < subSumVal.length; i++) {
				subSumVal[i] = 0f;
			}
		}
		int n = 0;
		for (int i = 0; i < val.length; i++) {
			if (!isActive(ro[i])) {
				continue;
			}
			n++;
			x[i] = (Double.isNaN(val[ro[i]])) ? 0f : (val[ro[i]] - min) / (max - min);
			for (int k = 0; k < fn.length; k++) {
				double y[] = (double[]) vy.elementAt(k);
				if (k == 0) {
					y[i] = (n) / (val.length - 0f - nNonActive);
				} else {
					double v = getNumericAttrValue(fn[k], ro[i]);
					if (!Double.isNaN(v)) {
						subSumVal[k - 1] += v;
					}
					y[i] = subSumVal[k - 1] / sumVal[k - 1];
				}
			}
		}
		isValid = true;
	}

	public void computeYbreaks() {
		if (!isValid) {
			compute();
		}
		if (xbreaks == null)
			return;
		vybreaks.removeAllElements();
		for (int element : fn) {
			double[] ybreaks = new double[1 + xbreaks.length]; // first element - for missing data class
			for (int i = 0; i < ybreaks.length; i++) {
				ybreaks[i] = 0f;
			}
			vybreaks.addElement(ybreaks);
		}
		for (int k = 0; k < fn.length; k++) {
			double ybreaks[] = (double[]) vybreaks.elementAt(k);
			for (int i = 0; i < ybreaks.length; i++) {
				ybreaks[i] = (i == 0) ? 0f : 1f;
				if (k == 0) {
					int n = 0;
					for (int j = 0; j < ro.length; j++) {
						if (!isActive(ro[j])) {
							continue;
						}
						if (i == 0) { // preprocessing sum for missing values
							n++;
						}
						if ((i == 0 && Double.isNaN(val[ro[j]])) || (i > 0 && val[ro[j]] >= xbreaks[i - 1])) {
							ybreaks[i] = (n + ((i == 0) ? 1f : 0f)) / (val.length - nNonActive);
							if (i > 0) {
								break;
							}
						}
						if (i > 0) { // processing sum
							n++;
						}
					}
				} else {
					double subSumVal = 0f;
					for (int j = 0; j < ro.length; j++) {
						if (!isActive(ro[j])) {
							continue;
						}
						if (i == 0) { // preprocessing sum for missing values
							double v = getNumericAttrValue(fn[k], ro[j]);
							if (!Double.isNaN(v)) {
								subSumVal += v;
							}
						}
						if ((i == 0 && Double.isNaN(val[ro[j]])) || (i > 0 && val[ro[j]] >= xbreaks[i - 1])) {
							ybreaks[i] = subSumVal / sumVal[k - 1];
							if (i > 0) {
								break;
							}
						}
						if (i > 0) { // processing sum
							double v = getNumericAttrValue(fn[k], ro[j]);
							if (!Double.isNaN(v)) {
								subSumVal += v;
							}
						}
					}
				}
			}
		}
	}

	public void forcedDraw() {
		if (canvas == null || !canvas.isShowing())
			return;
		Graphics g = canvas.getGraphics();
		if (g == null)
			return;
		draw(g);
		g.dispose();
	}

	@Override
	public void draw(Graphics g) {
		int x1 = 20, y1 = 20, x2 = 0, y2 = 0;
		if (getBounds() == null)
			return;
		g.setColor(canvas.getBackground());
		g.fillRect(bounds.x, bounds.y, bounds.width + 1, bounds.height + 1);
		if (fn == null)
			return;
		if (!isValid) {
			compute();
		}
		if (x == null)
			return;
		if (vybreaks.size() < 1) {
			computeYbreaks();
		}
		if (x1 < 5 + Metrics.stringWidth("100%")) {
			x1 = 5 + Metrics.stringWidth("100%");
		}
		if (x1 < 15 * fn.length) {
			x1 = 15 * fn.length;
		}
		if (y1 < 5 + Metrics.fh) {
			y1 = 5 + Metrics.fh;
		}
		x2 = getBounds().width - 5;
		y2 = getBounds().height - (10 + fn.length * Metrics.fh);
		int dx = x2 - x1, dy = y2 - y1;
		/* used for drawing in a special scale: 2 pixels per object
		 * requires re-definition of getPreferredSize() - see below
		dy=2*(dataTable.getDataItemCount()-nNonActive);
		y2=y1+dy;
		*/
		g.setColor(Color.lightGray);
		g.drawLine(0, y1, x2, y1);
		g.drawLine(0, y2, x2, y2);
		g.drawLine(x1, 0, x1, y2);
		g.drawLine(x2, 0, x2, y2);

		g.setColor(Color.black);
		g.drawString("100%", 5, 5 + Metrics.asc);
		for (int i = 0; i < fn.length; i++) {
			int yy = y2 + 10 + i * Metrics.fh + 5;
			String str = "";
			if (xbreaks != null) {
				double[] ybreaks = (double[]) vybreaks.elementAt(i);
				/*
				if (i==0) {
				  str="* ";
				  for (int j=0; j<ybreaks.length; j++)
				    str+=StringUtil.floatToStr(ybreaks[j],2)+" ";
				  System.out.println(str);
				  str="";
				}
				*/
				for (int j = 1; j <= ybreaks.length; j++)
					if (j == 0) {
						str = StringUtil.doubleToStr(100 * ybreaks[j], 1) + "%";
					} else if (j < ybreaks.length) {
						str += " " + StringUtil.doubleToStr(100 * (ybreaks[j] - ybreaks[j - 1]), 1) + "%";
					} else {
						str += " " + StringUtil.doubleToStr(100 * (1 - ybreaks[j - 1]), 1) + "%";
					}
			}
			str += " " + dataTable.getAttributeName(fn[i]) + ((i == 0) ? " (N)" : "");
			if (i == 0) {
				g.setColor(Color.black);
			} else {
				g.setColor(supervisor.getColorForAttribute(dataTable.getAttributeId(fn[i])));
			}
			g.drawString(str, x1 + 15, yy + Metrics.asc - 5);
			g.drawLine(x1 - 15 * i - 3, y1, x1 - 15 * i - 3, yy);
			g.drawLine(x1 - 15 * i - 3, yy, x1 + 10, yy);
		}

		// Drawing lines to texts
		if (xbreaks != null) {
			for (int i = 0; i < xbreaks.length; i++) {
				double[] ybreaks = (double[]) vybreaks.elementAt(0);
				int xx = x1 + (int) Math.round(dx * (xbreaks[i] - min) / (max - min)), yy = y2 - (int) Math.round(ybreaks[i + 1] * dy);
				g.setColor(Color.lightGray);
				g.drawLine(xx, yy, xx, y2);
				g.drawLine(x1, yy, xx, yy);
				for (int k = 1; k < fn.length; k++) {
					ybreaks = (double[]) vybreaks.elementAt(k);
					yy = y2 - (int) Math.round(ybreaks[i + 1] * dy);
					g.setColor(Color.lightGray);
					g.drawLine(xx, yy, xx, y2);
					g.drawLine(x1 - 15 * k, yy, xx, yy);
				}
			}
		}

		// drawing thick axes
		if (xbreaks != null && classColors != null) {
			for (int i = 0; i <= xbreaks.length; i++) {
				int xx1 = x1;
				if (i > 0) {
					xx1 = x1 + (int) Math.round(dx * (xbreaks[i - 1] - min) / (max - min));
				}
				int xx2 = x2;
				if (i < xbreaks.length) {
					xx2 = x1 + (int) Math.round(dx * (xbreaks[i] - min) / (max - min));
				}
				g.setColor(classColors[i]);
				g.fillRect(xx1, y2, xx2 - xx1, 6);
				g.setColor(Color.black);
				g.drawRect(xx1, y2, xx2 - xx1, 6);
				for (int k = 0; k < fn.length; k++) {
					double[] ybreaks = (double[]) vybreaks.elementAt(k);
					int yy1 = y2;
					//if (i>0) yy1=y2-Math.round(ybreaks[i-1]*dy);
					yy1 = y2 - (int) Math.round(ybreaks[i - 1 + 1] * dy);
					int yy2 = y1;
					if (i < xbreaks.length) {
						yy2 = y2 - (int) Math.round(ybreaks[i + 1] * dy);
					}
					if (i == 0) {
						g.setColor(Color.black);
						g.drawRect(x1 - 15 * k - 6, yy1, 6, y2 - yy1);
					}
					g.setColor(classColors[i]);
					g.fillRect(x1 - 15 * k - 6, yy2, 6, yy1 - yy2);
					g.setColor(Color.black);
					g.drawRect(x1 - 15 * k - 6, yy2, 6, yy1 - yy2);
				}
			}
		}

		// the curve and generalized curves
		for (int k = 0; k < fn.length; k++) {
			if (k == 0) {
				g.setColor(Color.black);
			} else {
				g.setColor(supervisor.getColorForAttribute(dataTable.getAttributeId(fn[k])));
			}
			double[] y = (double[]) vy.elementAt(k);
			int xs, ys, xf = 0, yf = 0;
			for (int i = 0; i < x.length - 1; i++) {
				xs = (int) Math.round(x1 + x[i] * dx);
				ys = (int) Math.round(y2 - y[i] * dy);
				xf = (int) Math.round(x1 + x[i + 1] * dx);
				yf = (int) Math.round(y2 - y[i + 1] * dy);
				g.drawLine(xs, ys, xf, ys);
				g.drawLine(xf, ys, xf, yf);
			}
			if (x[x.length - 1] != 1 || y[y.length - 1] != 1) {
				xs = xf;
				ys = yf;
				xf = x1 + dx;
				yf = y2 - dy;
				g.drawLine(xs, ys, xf, ys);
				g.drawLine(xf, ys, xf, yf);
			}
		}

	}

	public int mapX(float v) {
		return 0;
	}

	public int mapY(float v) {
		return 0;
	}

	public float absX(int x) {
		return 0;
	}

	public float absY(int y) {
		return 0;
	}

	/**
	* Reaction to change of data in the table or table removal
	*/
	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		if (pce.getSource().equals(dataTable)) {
			if (pce.getPropertyName().equals("destroyed") || pce.getPropertyName().equals("structure_complete")) {
				destroy();
			} else if (pce.getPropertyName().equals("filter")) {
				//a (new) filter is attached to the table or removed
				if (filter != null) {
					filter.removePropertyChangeListener(this);
				}
				filter = dataTable.getObjectFilter();
				if (filter != null) {
					filter.addPropertyChangeListener(this);
				}
				//do some actions on the change of the filter
				sumVal = null;
				compute();
				computeYbreaks();
				forcedDraw();
			} else if (pce.getPropertyName().equals("values")) {
				Vector v = (Vector) pce.getNewValue(); // list of changed attributes
				reloadAttributeData(v);
			} else if (pce.getPropertyName().equals("data_added") || pce.getPropertyName().equals("data_removed") || pce.getPropertyName().equals("data_updated")) {
				reset();
			}
		} else if (pce.getSource().equals(aTrans)) {
			if (pce.getPropertyName().equals("values")) {
				reset();
			} else if (pce.getPropertyName().equals("destroyed")) {
				destroy();
			}
		} else if (pce.getSource().equals(filter)) { //change of the filter
			if (pce.getPropertyName().equals("destroyed")) {
				filter.removePropertyChangeListener(this);
				filter = null;
			} else {
				//do some actions on the change of the filter
				sumVal = null;
				compute();
				computeYbreaks();
				forcedDraw();
			}
		}
	}

	/**
	* processing of messages from the classification slider - begin
	*/
	@Override
	public void breaksChanged(Object source, double[] breaks, int nBreaks) {
		xbreaks = new double[nBreaks];
		for (int i = 0; i < nBreaks; i++) {
			xbreaks[i] = breaks[i];
		}
		vybreaks.removeAllElements();
		if (source instanceof Slider) {
			Slider sl = (Slider) source;
			if (classColors == null || classColors.length != xbreaks.length + 1) {
				classColors = new Color[xbreaks.length + 1];
			}
			for (int i = 0; i < classColors.length; i++) {
				classColors[i] = sl.getColor(i);
			}
		}
		forcedDraw();
	}

	@Override
	public void breakIsMoving(Object source, int n, double currValue) {
		if (xbreaks == null) {
			xbreaks = new double[1];
		}
		xbreaks[n] = currValue;
		vybreaks.removeAllElements();
		forcedDraw();
	}

	@Override
	public void colorsChanged(Object source) {
		if (source instanceof Slider) {
			Slider sl = (Slider) source;
			if (classColors == null || classColors.length != xbreaks.length + 1) {
				classColors = new Color[xbreaks.length + 1];
			}
			for (int i = 0; i < classColors.length; i++) {
				classColors[i] = sl.getColor(i);
			}
		}
		forcedDraw();
	}

	// processing of messages from the classification slider - end

	/**
	* A method from the DataTreater interface.
	* Returns a vector of IDs of the attributes this Data Treater deals with.
	*/
	@Override
	public Vector getAttributeList() {
		Vector a = null;
		if (dataTable != null && fn != null && fn.length > 0) {
			a = new Vector(fn.length, 2);
			for (int element : fn) {
				a.addElement(dataTable.getAttributeId(element));
			}
		}
		return a;
	}

	/**
	* A method from the DataTreater interface.
	* Replies whether it is linked to the data set (table) with the given identifier
	*/
	@Override
	public boolean isLinkedToDataSet(String setId) {
		return setId != null && dataTable != null && setId.equals(dataTable.getContainerIdentifier());
	}

	/**
	* A method from the DataTreater interface.
	* Must return a vector of colors used for representation of the attributes
	* this Data Treater deals with. By default, returns null.
	*/
	@Override
	public Vector getAttributeColors() {
		return null;
	}

	//------------- implementation of the Drawable interface ------------

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(40 * Metrics.mm(), 50 * Metrics.mm() + fn.length * Metrics.fh);
	}

	//public Dimension getPreferredSize() { return new Dimension(300,700); } // needed for test purposes
	@Override
	public void setBounds(Rectangle bounds) {
		this.bounds = bounds;
	}

	@Override
	public Rectangle getBounds() {
		return bounds;
	}

	/**
	* Makes necessary operations for destroying, e.g. unregisters from
	* listening highlighting and other events.
	*/
	@Override
	public void destroy() {
		if (supervisor != null) {
			supervisor.removeDataDisplayer(this);
		}
		if (dataTable != null) {
			dataTable.removePropertyChangeListener(this);
		}
		if (aTrans != null) {
			aTrans.removePropertyChangeListener(this);
		}
		destroyed = true;
	}

	/**
	* Replies whether is destroyed or not
	*/
	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

	public int getInstanceN() {
		return instanceN;
	}

}
