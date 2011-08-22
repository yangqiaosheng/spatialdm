package spade.analysis.plot;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Hashtable;
import java.util.Vector;

import spade.analysis.classification.Classifier;
import spade.analysis.classification.TableClassifier;
import spade.analysis.generators.PlotGeneratorsDescriptor;
import spade.analysis.system.Supervisor;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.SplitLayout;
import spade.lib.basicwin.StringInRectangle;
import spade.lib.util.DoubleArray;
import spade.lib.util.FloatArray;
import spade.lib.util.NumValManager;
import spade.lib.util.StringUtil;
import spade.vis.action.HighlightListener;
import spade.vis.action.Highlighter;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.DataTable;
import spade.vis.database.DataTreater;
import spade.vis.database.ObjectFilter;
import spade.vis.spec.SaveableTool;
import spade.vis.spec.ToolSpec;

class dataCanvas extends Canvas {
	double absmin, absmax;
	DoubleArray famin, faq1, famedian, faq2, famax, famean, fastdd;
	DoubleArray ianvals, ianNaNs;
	Vector vcolors;
	int longLineIdx[] = null;
	int fh = 0;

	@Override
	public Dimension getPreferredSize() {
		if (fh == 0 || famin == null || famin.size() == 0)
			return new Dimension(150, 30);
		else
			return new Dimension(150, fh * (1 + famin.size()));
	}

	protected int precision = 0;

	public void setPrecision(double min, double max) {
		precision = StringUtil.getPreferredPrecision(min, min, max);
	}

	public void setData(double absmin, double absmax, DoubleArray famin, DoubleArray faq1, DoubleArray famedian, DoubleArray faq2, DoubleArray famax, DoubleArray famean, DoubleArray fastdd, DoubleArray ianvals, DoubleArray ianNaNs, Vector vcolors,
			int longLineIdx[]) {
		this.absmin = absmin;
		this.absmax = absmax;
		this.famin = famin;
		this.faq1 = faq1;
		this.famedian = famedian;
		this.faq2 = faq2;
		this.famax = famax;
		this.famean = famean;
		this.fastdd = fastdd;
		this.vcolors = vcolors;
		this.ianvals = ianvals;
		this.ianNaNs = ianNaNs;
		this.longLineIdx = longLineIdx;
		setPrecision(absmin, absmax);
		repaint();
	}

	public void setMultiAttrMinMax(double min, double max) {
		this.absmin = min;
		this.absmax = max;
		setPrecision(absmin, absmax);
		repaint();
	}
}

class tableStatsCanvas extends dataCanvas {
	protected static String texts[] = { "N", "N?", "min", "q1", "med", "q3", "max", "ave", "stdd" };

	protected void drawColumn(int data, Graphics g, int x1, int x2, int dy) {
		DoubleArray fa = null;
		switch (data) {
		case 0:
			fa = ianvals;
			break;
		case 1:
			fa = ianNaNs;
			break;
		case 2:
			fa = famin;
			break;
		case 3:
			fa = faq1;
			break;
		case 4:
			fa = famedian;
			break;
		case 5:
			fa = faq2;
			break;
		case 6:
			fa = famax;
			break;
		case 7:
			fa = famean;
			break;
		case 8:
			fa = fastdd;
			break;
		}
		if (fa == null)
			return;
		double minfa = NumValManager.getMin(fa), maxfa = NumValManager.getMax(fa);
		if (data <= 1) {
			minfa = Math.round(minfa);
			maxfa = Math.round(maxfa);
		}
		for (int i = 0; i < fa.size(); i++) {
			double v = fa.elementAt(i);
			if (Double.isNaN(v)) {
				continue;
			}
			if (data <= 1) {
				v = Math.round(v);
				// paint table lens
				int len = (int) Math.round((x2 - x1 + 1) * v / maxfa);
				if (len > 0) {
					g.setColor(Color.lightGray);
					g.fillRect(x2 - len + 1, (i + 1) * dy + 1, len, dy - 1);
				}
			}
			// draw the value
			g.setColor(Color.black);
			StringInRectangle.drawString(g, (data <= 1) ? String.valueOf(Math.round(v)) : StringUtil.doubleToStr(v, precision), x1, (i + 1) * dy - 1, x2 - x1 + 1, StringInRectangle.Right);
		}
	}

	@Override
	public void paint(Graphics g) {
		if (fh == 0) {
			FontMetrics fm = g.getFontMetrics();
			fh = fm.getHeight();
		}
		int x1 = 15, x2 = getSize().width - 5;
		int n = vcolors.size(); // famin.size();
		//int dy=this.getSize().height/(n+1);
		//if (dy>20) dy=20;
		int dy = fh;
		g.setColor(Color.black);
		for (int i = 0; i <= n; i++) {
			int y = (i + 1) * dy;
			g.drawLine(x1, y, x2, y);
			if (i == 0 || i == n) {
				continue;
			}
			y += 6;
			g.setColor((Color) vcolors.elementAt(i));
			g.fillRect(5, y, 5, 5);
			g.setColor(Color.black);
			g.drawRect(5, y, 5, 5);
		}
		if (longLineIdx != null) {
			for (int element : longLineIdx) {
				int y = (element + 1) * dy - 1;
				g.drawLine(0, y, getSize().width, y);
			}
		}
		for (int j = 0; j <= texts.length; j++) {
			int x = x1 + Math.round((x2 - x1) * j / (texts.length + 0f));
			g.drawLine(x, dy, x, (n + 1) * dy);
			if (j == texts.length) {
				break;
			}
			g.drawString(texts[j], x + 2, dy - 1);
			drawColumn(j, g, x + 1, x1 + Math.round((x2 - x1) * (j + 1) / (texts.length + 0f)) - 1, dy);
		}
	}
}

class tukeyPlotsCanvas extends dataCanvas {
	protected double rel(double v) {
		return (v - absmin) / (absmax - absmin);
	}

	protected int pos(double rel, int x1, int x2) {
		return x1 + (int) Math.round((x2 - x1) * rel);
	}

	@Override
	public void paint(Graphics g) {
		if (fh == 0) {
			FontMetrics fm = g.getFontMetrics();
			fh = fm.getHeight();
		}
		int x1 = 15, x2 = this.getSize().width - 5;
		int n = vcolors.size(); // famin.size();
		//int dy=this.getSize().height/(n+1);
		//if (dy>20) dy=20;
		int dy = fh;
		for (int i = 0; i < n; i++) {
			int y = (i + 1) * dy + 6;
			if (i > 0) {
				g.setColor((Color) vcolors.elementAt(i));
				g.fillRect(5, y, 5, 5);
				g.setColor(Color.black);
				g.drawRect(5, y, 5, 5);
			} else {
				g.setColor(Color.black);
			}
			int x = pos(rel(famin.elementAt(i)), x1, x2); // x:min
			g.drawLine(x, y - 3, x, y + 3);
			int xx = pos(rel(faq1.elementAt(i)), x1, x2); // xx:q1
			g.drawLine(x, y, xx, y);
			g.drawLine(xx, y - 3, xx, y + 3);
			x = pos(rel(faq2.elementAt(i)), x1, x2); // x:q3
			g.drawLine(xx, y - 3, x, y - 3);
			g.drawLine(xx, y + 3, x, y + 3);
			g.drawLine(x, y - 3, x, y + 3);
			xx = pos(rel(famedian.elementAt(i)), x1, x2); // xx:median
			g.drawLine(xx, y - 3, xx, y + 3);
			xx = pos(rel(famax.elementAt(i)), x1, x2); // xx:max
			g.drawLine(x, y, xx, y);
			g.drawLine(xx, y - 3, xx, y + 3);
			y += 8;
			g.setColor(Color.lightGray);
			x = pos(rel(famean.elementAt(i)), x1, x2);
			g.drawLine(x, y - 3, x, y + 3);
			double f = fastdd.elementAt(i);
			if (Double.isNaN(f)) {
				continue;
			}
			xx = pos(rel(famean.elementAt(i) - f), x1, x2);
			g.drawLine(xx, y - 1, xx, y + 1);
			g.drawLine(xx, y, x, y);
			xx = pos(rel(famean.elementAt(i) + f), x1, x2);
			g.drawLine(xx, y - 1, xx, y + 1);
			g.drawLine(x, y, xx, y);
		}
	}
}

public class StatisticsPanel extends Panel implements DataTreater, SaveableTool, Destroyable, PropertyChangeListener, HighlightListener, PrintableImage {

	/**
	* Identifier of this visualization method
	*/
	protected String methodId = null;
//ID
	/**
	* Used to generate unique identifiers of instances
	*/
	protected static int nInstances = 0;
	protected int instanceN = 0;
//~ID
	/**
	* Indicates whether the Panel was destroyed
	*/
	protected boolean destroyed = false;
	/**
	* As a SaveableTool, a plot may be registered somewhere and, hence, must
	* notify the component where it is registered about its destroying. This vector
	* contains the listeners to be notified about destroying of the visualizer.
	*/
	protected Vector destroyListeners = null;

	protected Supervisor supervisor = null;
	protected AttributeDataPortion dTable = null;
	protected ObjectFilter tf = null;
	protected String id = null; // ID of the attribute for which tthe statistics is provided

	protected DoubleArray vals = null; // for grid layers

	protected tukeyPlotsCanvas tpc = null;
	protected tableStatsCanvas tsc = null;

	protected double absmin = Double.NaN, absmax = Double.NaN;

	public StatisticsPanel(Supervisor supervisor, String AttrID, AttributeDataPortion dataTable) {
		super();
//ID
		instanceN = ++nInstances;
//~ID
		this.supervisor = supervisor;
		dTable = dataTable;
		id = AttrID;
		makeComponents(false);
	}

	public StatisticsPanel(DoubleArray vals) {
		super();
//ID
		instanceN = ++nInstances;
//~ID
		this.vals = vals;
		makeComponents(false);
	}

	public StatisticsPanel(FloatArray fvals) {
		super();
//ID
		instanceN = ++nInstances;
//~ID
		if (fvals != null && fvals.size() > 0) {
			vals = new DoubleArray(fvals.size(), 10);
			for (int i = 0; i < fvals.size(); i++) {
				vals.addElement(fvals.elementAt(i));
			}
		}
		makeComponents(false);
	}

	public StatisticsPanel(Supervisor supervisor, String AttrID, AttributeDataPortion dataTable, boolean isInMultiPanel) {
		super();
//ID
		instanceN = ++nInstances;
//~ID
		this.supervisor = supervisor;
		dTable = dataTable;
		id = AttrID;
		makeComponents(isInMultiPanel);
	}

	public StatisticsPanel(DoubleArray vals, boolean isInMultiPanel) {
		super();
//ID
		instanceN = ++nInstances;
//~ID
		this.vals = vals;
		makeComponents(isInMultiPanel);
	}

	public StatisticsPanel(FloatArray fvals, boolean isInMultiPanel) {
		super();
//ID
		instanceN = ++nInstances;
//~ID
		if (fvals != null && fvals.size() > 0) {
			vals = new DoubleArray(fvals.size(), 10);
			for (int i = 0; i < fvals.size(); i++) {
				vals.addElement(fvals.elementAt(i));
			}
		}
		makeComponents(isInMultiPanel);
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(250, 50);
	}

	public void makeComponents(boolean isInMultiPanel) {
		if (supervisor != null) {
			supervisor.addPropertyChangeListener(this);
			supervisor.registerDataDisplayer(this);
			if (dTable != null) {
				supervisor.registerHighlightListener(this, dTable.getEntitySetIdentifier());
			}
		}
		if (dTable != null) {
			dTable.addPropertyChangeListener(this);
		}
		tpc = new tukeyPlotsCanvas();
		tsc = new tableStatsCanvas();
		SplitLayout spl = null;
		if (isInMultiPanel) {
			spl = new SplitLayout(this, SplitLayout.VERT);
			setLayout(spl);
		} else {
			setLayout(new BorderLayout());
			ScrollPane sp = new ScrollPane();
			add(sp, BorderLayout.CENTER);
			Panel p = new Panel();
			sp.add(p);
			spl = new SplitLayout(p, SplitLayout.VERT);
			p.setLayout(spl);
		}
		spl.addComponent(tpc, 0.3f);
		spl.addComponent(tsc, 0.7f);
		reset();
	}

	protected boolean isActive(int n) {
		if (tf == null)
			return true;
		return tf.isActive(n);
	}

	protected Classifier findClassifier() {
		if (supervisor != null && supervisor.getObjectColorer() != null && (supervisor.getObjectColorer() instanceof Classifier) && supervisor.getObjectColorer().getEntitySetIdentifier().equals(dTable.getEntitySetIdentifier()))
			return (Classifier) supervisor.getObjectColorer();
		return null;
	}

	public void reset() {
		Vector cc = new Vector(10, 10);
		int attrN = (dTable == null) ? -1 : dTable.getAttrIndex(id);

		// looking for selection of objects
		boolean selection[] = null;
		if (dTable != null) {
			Highlighter highlighter = supervisor.getHighlighter(dTable.getEntitySetIdentifier());
			if (highlighter != null && highlighter.getSelectedObjects() != null) {
				selection = new boolean[dTable.getDataItemCount()];
				for (int i = 0; i < selection.length; i++) {
					selection[i] = false;
				}
				Vector vs = highlighter.getSelectedObjects();
				for (int j = 0; j < vs.size(); j++) {
					String id = (String) vs.elementAt(j);
					int n = ((DataTable) dTable).getObjectIndex(id);
					if (n >= 0) {
						selection[n] = true;
					}
				}
			}
		}

		// looking for broadcasted classification
		Classifier cl = findClassifier();
		int ncl = 0; // number of classes of broadcasted classification
		int classes[] = null;
		boolean missedVals = false;
		if (cl != null) {
			ncl = cl.getNClasses();
			classes = new int[dTable.getDataItemCount()];
			TableClassifier tcl = null;
			if (cl instanceof TableClassifier) {
				tcl = (TableClassifier) cl;
				if (!dTable.equals(tcl.getTable())) {
					tcl = null;
				}
			}
			for (int i = 0; i < dTable.getDataItemCount(); i++) {
				classes[i] = (tcl != null) ? tcl.getRecordClass(i) : cl.getObjectClass(i);
				if (classes[i] == -1) {
					missedVals = true;
				}
			}
		}

		absmin = Double.NaN;
		absmax = Double.NaN;
		int nclasses = ncl + ((selection == null) ? 0 : 2);
		DoubleArray ianvals = new DoubleArray(1 + nclasses, 1), ianNaNs = new DoubleArray(1 + nclasses, 1);
		DoubleArray famin = new DoubleArray(1 + nclasses, 1), faq1 = new DoubleArray(1 + nclasses, 1), famedian = new DoubleArray(1 + nclasses, 1), faq2 = new DoubleArray(1 + nclasses, 1), famax = new DoubleArray(1 + nclasses, 1), famean = new DoubleArray(
				1 + nclasses, 1), fastdd = new DoubleArray(1 + nclasses, 1), favariance = new DoubleArray(1 + nclasses, 1);
		Vector vcolors = new Vector(1 + nclasses, 1);
		int maxIterator = 1 + nclasses + ((missedVals) ? 1 : 0);
		for (int cln = 0; cln < maxIterator; cln++) { // class number
			DoubleArray fa = null;
			if (dTable == null) {
				fa = vals;
			} else {
				fa = new DoubleArray(dTable.getDataItemCount(), 10);
				for (int i = 0; i < dTable.getDataItemCount(); i++) {
					if (!isActive(i)) {
						continue;
					}
					if (selection != null && cln >= maxIterator - 2)
						if (cln == maxIterator - 2) { // selected
							if (selection[i]) {
								fa.addElement(dTable.getNumericAttrValue(attrN, i));
							}
						} else { // not selected
							if (!selection[i]) {
								fa.addElement(dTable.getNumericAttrValue(attrN, i));
							}
						}
					else if (cln == 0 || classes[i] == cln - 1 || (cln == 1 + ncl && classes[i] == -1)) {
						fa.addElement(dTable.getNumericAttrValue(attrN, i));
					}
				}
			}
			int nvals = fa.size(), nNaNs = NumValManager.getNofNaN(fa);
			ianvals.addElement(nvals);
			ianNaNs.addElement(nNaNs);
			double breaks[] = NumValManager.breakToIntervals(fa, 4, false);
			double min = (breaks == null) ? Double.NaN : breaks[0], q1 = (breaks == null || breaks.length < 5) ? Double.NaN : breaks[1], median = (breaks == null || breaks.length < 2) ? Double.NaN : (breaks.length == 5) ? breaks[2] : breaks[1], q3 = (breaks == null || breaks.length < 5) ? Double.NaN
					: breaks[3], max = (breaks == null || breaks.length < 1) ? Double.NaN : breaks[breaks.length - 1], mean = NumValManager.getMean(fa), variance = NumValManager.getVariance(fa, mean), stdd = NumValManager.getStdD(fa, mean);
			famin.addElement(min);
			faq1.addElement(q1);
			famedian.addElement(median);
			faq2.addElement(q3);
			famax.addElement(max);
			famean.addElement(mean);
			favariance.addElement(variance);
			fastdd.addElement(stdd);
			Color c = null;
			if (selection != null && cln >= maxIterator - 2)
				if (cln == maxIterator - 2) {
					c = Color.black; // selected
				} else {
					c = Color.white; // not selected
				}
			else if (cln == 0) {
				c = Color.white;
			} else {
				c = cl.getClassColor((cln == 1 + ncl) ? -1 : cln - 1);
			}
			if (c == null) {
				c = Color.lightGray;
			}
			vcolors.addElement(c);
			//System.out.println("* cl="+cln+", n="+nvals+", breaks="+min+" "+q1+" "+median+" "+q3+" "+max+", stdd="+stdd);
			if (cln == 0) {
				absmin = min;
				absmax = max;
			}
		}
		int longLineIdx[] = null;
		if (ncl == 0)
			if (selection == null) {
				;
			} else {
				longLineIdx = new int[1];
				longLineIdx[0] = 1;
			}
		else if (selection == null) {
			longLineIdx = new int[1];
			longLineIdx[0] = 1;
		} else {
			longLineIdx = new int[2];
			longLineIdx[0] = 1;
			longLineIdx[1] = 1 + ncl;
		}
		tpc.setData(absmin, absmax, famin, faq1, famedian, faq2, famax, famean, fastdd, ianvals, ianNaNs, vcolors, longLineIdx);
		tsc.setData(absmin, absmax, famin, faq1, famedian, faq2, famax, famean, fastdd, ianvals, ianNaNs, vcolors, longLineIdx);
		CManager.validateAll(tpc);
	}

	public double getMin() {
		return absmin;
	}

	public double getMax() {
		return absmax;
	}

	public void setMultiAttrMinMax(double min, double max) {
		tpc.setMultiAttrMinMax(min, max);
		tsc.setMultiAttrMinMax(min, max);
	}

	//----------------------- HighlightListener interface --------------------
	/**
	* Notification from the highlighter about change of the set of objects to be
	* selected (durably highlighted).
	*/
	@Override
	public void selectSetChanged(Object source, String setId, Vector hlObj) {
		if (dTable == null || !StringUtil.sameStrings(setId, dTable.getEntitySetIdentifier()))
			return;
		reset();
		/*
		System.out.print("* selection:");
		if (hlObj!=null)
		  for (int i=0; i<hlObj.size(); i++)
		    System.out.print(" "+hlObj.elementAt(i));
		System.out.println();
		*/
	}

	@Override
	public void highlightSetChanged(Object source, String setId, Vector hlObj) {
	}

	//----------------------- PropertyChangeListener interface --------------------
	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		//System.out.println("* "+pce);
		if (pce.getSource().equals(dTable)) { // table events
			if (pce.getPropertyName().equals("destroyed") || pce.getPropertyName().equals("structure_complete")) {
				destroy();
			} else if (pce.getPropertyName().equals("values")) {
				Vector v = (Vector) pce.getNewValue(); // list of changed attributes
				if (v.indexOf(new String(id)) >= 0) {
					//System.out.println("* found");
					reset();
				} else
					return; // System.out.println("* not found");
			} else if (pce.getPropertyName().equals("data_added") || pce.getPropertyName().equals("data_removed") || pce.getPropertyName().equals("data_updated")) {
				reset();
			} else if (pce.getPropertyName().equals("filter")) {
				if (tf != null) {
					tf.removePropertyChangeListener(this);
				}
				tf = dTable.getObjectFilter();
				if (tf != null) {
					tf.addPropertyChangeListener(this);
				}
				repaint();
			}
		} else if (pce.getSource().equals(supervisor) && pce.getPropertyName().equals(Supervisor.eventObjectColors)) {
			// supervisor event: change of classification
			if (dTable.getEntitySetIdentifier().equals(pce.getNewValue())) {
				reset();
			}
		} else if (pce.getSource().equals(tf)) {
			if (pce.getPropertyName().equals("destroyed")) {
				tf.removePropertyChangeListener(this);
				tf = null;
			} else {
				reset();
			}
		}
	}

	//---------------------- Destroyable interface -------------------------------
	/**
	* Makes necessary operations for destroying, e.g. unregisters from
	* listening highlighting and other events.
	*/
	@Override
	public void destroy() {
		if (supervisor != null) {
			supervisor.removePropertyChangeListener(this);
			supervisor.registerDataDisplayer(this);
			if (dTable != null) {
				supervisor.removeDataDisplayer(this);
			}
		}
		if (dTable != null) {
			dTable.removePropertyChangeListener(this);
		}
		destroyed = true;
		if (destroyListeners != null) {
			PropertyChangeEvent pce = new PropertyChangeEvent(this, "destroyed", null, null);
			for (int i = 0; i < destroyListeners.size(); i++) {
				((PropertyChangeListener) destroyListeners.elementAt(i)).propertyChange(pce);
			}
		}
	}

	/**
	* Replies whether is destroyed or not
	*/
	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

//ID
	public int getInstanceN() {
		return instanceN;
	}

//~ID

//--------------------- DataTreater interface -----------------------------
	/**
	* Returns vector of IDs of attribute(s) on this display
	*/
	@Override
	public Vector getAttributeList() {
		Vector v = new Vector(1, 1);
		if (id != null) {
			v.addElement(new String(id));
		}
		return v;
	}

	/**
	* Replies whether it is linked to the data set (table) with the given identifier
	*/
	@Override
	public boolean isLinkedToDataSet(String setId) {
		return setId != null && dTable != null && setId.equals(dTable.getContainerIdentifier());
	}

	/**
	* A method from the DataTreater interface.
	* Must return a vector of colors used for representation of the attributes
	* this Data Treater deals with.
	*/
	@Override
	public Vector getAttributeColors() {
		return null;
	}

//---------------- implementation of the SaveableTool interface ------------
	/**
	* Adds a listener to be notified about destroying of the visualize.
	* As a SaveableTool, a visualizer may be registered somewhere and, hence, must
	* notify the component where it is registered about its destroying.
	*/
	@Override
	public void addDestroyingListener(PropertyChangeListener lst) {
		if (lst == null)
			return;
		if (destroyListeners == null) {
			destroyListeners = new Vector(5, 5);
		}
		if (!destroyListeners.contains(lst)) {
			destroyListeners.addElement(lst);
		}
	}

	/**
	* Returns the keyword used in the opening tag of a stored state description
	* of this tool. A visualizer description (specification) is usually stored as
	* a sequence of lines starting with <map> and ending with </map>. Hence,
	* this method returns "map".
	*/
	@Override
	public String getTagName() {
		return "chart";
	}

	/**
	* Returns the specification of this visualizer (normally an instance of the
	* class spade.vis.spec.ToolSpec) for storing in a file.
	*/
	@Override
	public Object getSpecification() {
		ToolSpec spec = new ToolSpec();
		spec.tagName = getTagName();
		spec.methodId = getMethodId();
		if (dTable != null) {
			spec.table = dTable.getContainerIdentifier();
		}
		spec.attributes = getAttributeList();
		spec.properties = getProperties();
		return spec;
	}

	/**
	* Returns custom properties of the tool: String -> String
	* By default, returns null.
	*/
	public Hashtable getProperties() {
		return null;
	}

	/**
	* After the plot is constructed, it may be requested to setup its individual
	* properties according to the given list of stored properties.
	* The base Plot class does nothing in this method.
	*/
	@Override
	public void setProperties(Hashtable properties) {
	}

	/**
	* Through this function the component constructing the plot can set the
	* identifier of the visualization method.
	*/
	public void setMethodId(String id) {
		methodId = id;
	}

	/**
	* Returns the identifier of the visualization method implemented by this
	* class.
	*/
	public String getMethodId() {
		return methodId;
	}

//ID
	@Override
	public Image getImage() {
		int gap = 2, indent = 20;
		StringBuffer sb = new StringBuffer();
		StringInRectangle str = new StringInRectangle();
		str.setPosition(StringInRectangle.Left);
		str.setPosition(StringInRectangle.Center);
		sb.append(dTable.getAttributeName(dTable.getAttrIndex((String) getAttributeList().elementAt(0))));
//    sb.append((String)getAttributeList().elementAt(0));
		str.setString(sb.toString());
		Dimension lsize = str.countSizes(tpc.getGraphics());
		int h = 0, w = 0, currX = 0, currY = 0;
		w += tpc.getBounds().width;
		w += tsc.getBounds().width;
		h = Math.max(h, tpc.getBounds().height);
		h = Math.max(h, tsc.getBounds().height);
		str.setRectSize(w - indent, lsize.width * lsize.height / w * 2);
		lsize = str.countSizes(tpc.getGraphics());
		Image li = tpc.createImage(w - indent, lsize.height);
		str.draw(li.getGraphics());
		Image img = tpc.createImage(w, h + lsize.height + 2 * gap);

		img.getGraphics().drawImage(li, indent, gap, null);
		currY += lsize.height + 2 * gap;
		Image ic = tpc.createImage(tpc.getBounds().width, tpc.getBounds().height);
		tpc.paint(ic.getGraphics());
		img.getGraphics().drawImage(ic, currX, currY, null);
		currX += tpc.getBounds().width;
		ic = tsc.createImage(tsc.getBounds().width, tsc.getBounds().height);
		tsc.paint(ic.getGraphics());
		img.getGraphics().drawImage(ic, currX, currY, null);

		return img;
	}

	@Override
	public String getName() {
		String name = PlotGeneratorsDescriptor.getToolName(getMethodId()) + " " + getInstanceN();
		if (name == null) {
			name = getMethodId() + " " + getInstanceN();
		}
		return name;
	}
//~ID
}