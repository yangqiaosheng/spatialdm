package spade.time.vis;

import java.awt.Canvas;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import spade.analysis.system.Supervisor;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.PopupManager;
import spade.lib.util.FloatArray;
import spade.lib.util.NumValManager;
import spade.lib.util.StringUtil;
import spade.vis.action.HighlightListener;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataTable;
import spade.vis.database.ObjectFilter;
import spade.vis.database.Parameter;

public class TimeGraphSummary extends Canvas implements MouseListener, MouseMotionListener, PropertyChangeListener, ItemListener, HighlightListener, Destroyable {

	/**
	* The supervisor is used for propagating object events among system components
	*/
	protected Supervisor supervisor = null;
	/**
	* The table with time-dependent data
	*/
	protected AttributeDataPortion table = null;
	protected DataTable dTable = null;
	/**
	* ObjectFilter may be associated with the table and contain results of data
	* querying. Only data satisfying the current query (if any) are displayed
	*/
	protected ObjectFilter tf = null;
	/**
	* The time-dependent super-attribute to be represented, i.e. an attribute
	* having references to child attributes corresponding to different values
	* of a temporal parameter.
	*/
	protected Attribute supAttr = null;
	/**
	* The temporal parameter
	*/
	protected Parameter par = null;
	/**
	* Indicates "destroyed" state
	*/
	protected boolean destroyed = false;
	/**
	* Used for showing objects in each box
	*/
	protected PopupManager popM = null;
	/**
	* Vector of lines to be drawn
	*/
	protected Vector lines = null;
	/*
	* indexes of line points currently plotted
	*/
	protected int idxTFstart = -1, idxTFend = -1;
	/*
	* values of the breaks
	*/
	protected float breaks[] = null;
	/*
	* colours for classes
	*/
	protected Color classColours[] = null;
	/*
	* bias for shifting classes (alignment)
	*/
	protected int bias = 0;

	public void shift() {
		if (bias < breaks.length) {
			bias++;
		} else {
			bias = 0;
		}
		draw(getGraphics());
	}

	/**
	* Identifiers of attributes that store average classification and variance
	* of the dynamic of classification
	*/
	protected String attrIdClassAverage = null, attrIdClassVariance = null, attrIdClassIncrease = null, attrIdClassDecrease = null, attrIdClassNumbers[] = null;
	/*
	* left and right indents
	*/
	protected int indentLeft = 0, indentRight = 0;
	/*
	* np - number of objects in each class for each moment of time
	* npsel - the same for selected objects
	*/
	protected int np[][] = null, npsel[][] = null;

	/**
	* Used to generate unique identifiers of instances
	*/
	protected int instanceN = 0;
	protected static int nInstances = 0;

	public TimeGraphSummary() {
		instanceN = nInstances++;
	}

	public boolean notReady() {
		return lines == null;
	}

	/**
	* Sets the table with time-dependent data
	*/
	public void setTable(AttributeDataPortion table) {
		this.table = table;
		if (table != null && supervisor != null) {
			supervisor.registerHighlightListener(this, table.getEntitySetIdentifier());
		}
		if (table instanceof DataTable) {
			dTable = (DataTable) table;
		}
		if (table != null) {
			table.addPropertyChangeListener(this);
			tf = table.getObjectFilter();
			if (tf != null) {
				tf.addPropertyChangeListener(this);
			}
		}
	}

	/**
	* Sets the time-dependent super-attribute to be represented, i.e. an attribute
	* having references to child attributes corresponding to different values
	* of a temporal parameter.
	*/
	public void setAttribute(Attribute attr) {
		if (attr != null && attr.getChildrenCount() > 1) {
			supAttr = attr;
		}
	}

	/**
	* Sets the temporal parameter
	*/
	public void setTemporalParameter(Parameter par) {
		if (par.isTemporal() && par.getValueCount() > 1) {
			this.par = par;
		}
	}

	/**
	* Sets a reference to the system's supervisor. The supervisor is used for
	* propagating object events among system components.
	*/
	public void setSupervisor(Supervisor sup) {
		supervisor = sup;
		if (supervisor != null) {
			//supervisor.addPropertyChangeListener(this);
			if (table != null) {
				supervisor.registerHighlightListener(this, table.getEntitySetIdentifier());
			}
			addMouseListener(this);
			addMouseMotionListener(this);
		}
		if (popM == null) {
			popM = new PopupManager(this, "", true);
			popM.setOnlyForActiveWindow(false);
		}
	}

	/**
	* Checks whether the object with the given index is active, i.e. satisfies
	* the filter.
	*/
	public boolean isActive(int n) {
		if (tf == null)
			return true;
		return tf.isActive(n);
	}

	public void setLines(Vector lines) {
		this.lines = lines;
		if (lines != null && lines.size() > 0) {
			idxTFstart = 0;
			idxTFend = ((TimeLine) lines.elementAt(0)).getNPoints();
		}
	}

	public void setIndices(int idxTFstart, int idxTFend) {
		this.idxTFstart = idxTFstart;
		this.idxTFend = idxTFend;
		if (breaks != null) {
			createNP();
			countNP();
			countNPsel();
			draw(getGraphics());
		}
	}

	public void setOneBreak(float val, Color colors[]) {
		float breaks[] = new float[1];
		breaks[0] = val;
		setBreaks(breaks, colors);
	}

	public void setBreaks(float breaks[], Color colors[]) {
		this.breaks = breaks;
		if (classColours == null || 2 + breaks.length != classColours.length) {
			createNP();
		}
		classColours = colors;
		countNP();
		countNPsel();
		bias = 0;
		draw(getGraphics());
	}

	public float[] getBreaks() {
		return breaks;
	}

	public int getBreakCount() {
		if (breaks == null)
			return 0;
		return breaks.length;
	}

	public float getBreakValue(int idx) {
		if (breaks == null || idx < 0 || idx >= breaks.length)
			return Float.NaN;
		return breaks[idx];
	}

	public Color[] getClassColors() {
		return classColours;
	}

	protected void createNP() {
		int ncl = 1 + breaks.length + 1; // last class - missing values
		np = new int[ncl][];
		npsel = new int[ncl][];
		for (int i = 0; i < ncl; i++) {
			np[i] = new int[1 + idxTFend - idxTFstart];
			npsel[i] = new int[1 + idxTFend - idxTFstart];
		}
		/*
		classColours=new Color[ncl];
		for (int i=0; i<ncl; i++)
		  if (i==ncl-1)
		    classColours[i]=Color.lightGray;
		  else {
		    int rgb=96+159*(ncl-1-i)/(ncl-1);
		    classColours[i]=new Color(0,rgb,rgb);
		  }*/
	}

	protected boolean toSaveClasses = false;

	@Override
	public void itemStateChanged(ItemEvent ie) {
		if (ie.getSource() instanceof Checkbox) {
			Checkbox cb = (Checkbox) ie.getSource();
			toSaveClasses = true;
			cb.setEnabled(false);
			countNP();
		}
	}

	protected void countNP() {
		if (lines == null || np == null)
			return;
		if (dTable != null && attrIdClassAverage == null && toSaveClasses) {
			// adding columns to the table
			Vector sourceAttrs = new Vector(1, 1);
			sourceAttrs.addElement(supAttr.getIdentifier());
			int idx = dTable.addDerivedAttribute("TA Average class " + (instanceN + 1), AttributeTypes.real, AttributeTypes.average, sourceAttrs);
			attrIdClassAverage = dTable.getAttributeId(idx);
			idx = dTable.addDerivedAttribute("TA Class variation " + (instanceN + 1), AttributeTypes.real, AttributeTypes.average, sourceAttrs);
			attrIdClassVariance = dTable.getAttributeId(idx);
			idx = dTable.addDerivedAttribute("TA Increase class " + (instanceN + 1), AttributeTypes.real, AttributeTypes.average, sourceAttrs);
			attrIdClassIncrease = dTable.getAttributeId(idx);
			idx = dTable.addDerivedAttribute("TA Decrease class " + (instanceN + 1), AttributeTypes.real, AttributeTypes.average, sourceAttrs);
			attrIdClassDecrease = dTable.getAttributeId(idx);
			attrIdClassNumbers = new String[10];
			Vector Nids = new Vector(12, 1);
			Nids.addElement(attrIdClassIncrease);
			Nids.addElement(attrIdClassDecrease);
			for (int i = 0; i < attrIdClassNumbers.length; i++) {
				idx = dTable.addDerivedAttribute("TA class " + (instanceN + 1) + " N " + (i + 1), AttributeTypes.real, AttributeTypes.average, sourceAttrs);
				attrIdClassNumbers[i] = dTable.getAttributeId(idx);
				Nids.addElement(attrIdClassNumbers[i]);
			}
			dTable.getSemanticsManager().setAttributesComparable(Nids);
		}
		if (attrIdClassNumbers != null) {
			for (int i = 0; i < classColours.length; i++) {
				supervisor.getAttrColorHandler().setColorForAttribute(classColours[i], attrIdClassNumbers[i]);
			}
		}
		for (int j = idxTFstart; j <= idxTFend; j++) {
			for (int i = 0; i < np.length; i++) {
				np[i][j - idxTFstart] = 0;
			}
			for (int k = 0; k < lines.size(); k++) {
				TimeLine tl = (TimeLine) lines.elementAt(k);
				if (!isActive(tl.objIdx)) {
					continue;
				}
				double v = tl.getValue(j);
				if (Double.isNaN(v)) {
					np[np.length - 1][j - idxTFstart]++;
				} else {
					int classn = -1;
					for (int i = 0; classn == -1 && i < breaks.length; i++)
						if (v < breaks[i]) {
							classn = i;
						}
					if (classn == -1) {
						classn = breaks.length;
					}
					np[classn][j - idxTFstart]++;
				}
			}
		}
		if (dTable != null && toSaveClasses) {
			int idxa = table.getAttrIndex(attrIdClassAverage), idxv = table.getAttrIndex(attrIdClassVariance), idxi = table.getAttrIndex(attrIdClassIncrease), idxd = table.getAttrIndex(attrIdClassDecrease);
			int N[] = new int[attrIdClassNumbers.length], idxn[] = new int[attrIdClassNumbers.length];
			for (int i = 0; i < N.length; i++) {
				idxn[i] = table.getAttrIndex(attrIdClassNumbers[i]);
			}
			for (int k = 0; k < lines.size(); k++) {
				TimeLine line = (TimeLine) lines.elementAt(k);
				FloatArray fa = new FloatArray(idxTFend - idxTFstart, 10);
				for (int i = 0; i < N.length; i++) {
					N[i] = 0;
				}
				for (int j = idxTFstart; j <= idxTFend; j++) {
					double val = line.getValue(j);
					if (Double.isNaN(val)) {
						continue;
					}
					int classn = -1;
					for (int i = 0; classn == -1 && i < breaks.length; i++)
						if (val < breaks[i]) {
							classn = i + 1;
						}
					if (classn == -1) {
						classn = breaks.length + 1;
					}
					fa.addElement(classn);
					if (classn > 0) {
						N[classn - 1]++;
					}
				}
				int nplus = 0, nminus = 0;
				for (int j = 1; j <= fa.size(); j++) {
					if (fa.elementAt(j) > fa.elementAt(j - 1)) {
						nplus++;
					}
					if (fa.elementAt(j) < fa.elementAt(j - 1)) {
						nminus++;
					}
				}
				float avg = NumValManager.getMean(fa);
				dTable.setNumericAttributeValue(avg, 0, 11 + breaks.length, idxa, line.objIdx);
				float variance = NumValManager.getVariance(fa, avg);
				dTable.setNumericAttributeValue(variance, 0, 1 + breaks.length, idxv, line.objIdx);
				dTable.setNumericAttributeValue(nplus, idxi, line.objIdx);
				dTable.setNumericAttributeValue(nminus, idxd, line.objIdx);
				for (int i = 0; i < idxn.length; i++) {
					dTable.setNumericAttributeValue(N[i], idxn[i], line.objIdx);
				}
			}
			// inform all displays about change of values
			Vector attr = new Vector(2, 1);
			attr.addElement(attrIdClassAverage);
			attr.addElement(attrIdClassVariance);
			attr.addElement(attrIdClassIncrease);
			attr.addElement(attrIdClassDecrease);
			for (int i = 0; i < N.length; i++) {
				attr.addElement(attrIdClassNumbers[i]);
			}
			dTable.notifyPropertyChange("values", null, attr);
		}
	}

	protected void countNPsel() {
		if (lines == null || npsel == null)
			return;
		for (int j = idxTFstart; j <= idxTFend; j++) {
			for (int i = 0; i < npsel.length; i++) {
				npsel[i][j - idxTFstart] = 0;
			}
			for (int k = 0; k < lines.size(); k++) {
				TimeLine tl = (TimeLine) lines.elementAt(k);
				if (!isActive(tl.objIdx)) {
					continue;
				}
				if (!tl.selected) {
					continue;
				}
				double v = tl.getValue(j);
				if (Double.isNaN(v)) {
					npsel[npsel.length - 1][j - idxTFstart]++;
				} else {
					int classn = -1;
					for (int i = 0; classn == -1 && i < breaks.length; i++)
						if (v < breaks[i]) {
							classn = i;
						}
					if (classn == -1) {
						classn = breaks.length;
					}
					npsel[classn][j - idxTFstart]++;
				}
			}
		}
	}

	@Override
	public void selectSetChanged(Object source, String setId, Vector selected) {
		countNPsel();
		draw(getGraphics());
	}

	@Override
	public void highlightSetChanged(Object source, String setId, Vector highlighted) {
	}

	public void setIndents(int indentLeft, int indentRight) {
		this.indentLeft = indentLeft;
		this.indentRight = indentRight;
	}

	// coordinates of blocks on the plot
	protected int ax1[] = null, ax2[] = null, ay1[] = null, ay2[] = null, aj[] = null, ai[] = null;

	/**
	* Draws the graph
	*/
	public void draw(Graphics g) {
		if (g == null || lines == null || np == null)
			return;
		Dimension d = getSize();
		g.setColor(Color.white);
		g.fillRect(0, 0, d.width, d.height);
		int ncl = np.length, ntm = np[0].length;
		int dx = (d.width - indentLeft - indentRight) * ntm / (ntm - 1), x1 = indentLeft - Math.round(0.5f * dx / ntm), x2 = d.width - indentRight + Math.round(0.5f * dx / ntm);
		g.setColor(Color.black);
		g.drawRect(x1, 0, x2 - x1, d.height - 1);
		// horisontal lines
		//for (int i=1; i<=ncl; i++)
		//  g.drawLine(x1,i*(d.height-1)/ncl,x2,i*(d.height-1)/ncl);
		int N = (1 + idxTFend - idxTFstart) * ncl;
		if (ax1 == null || ax1.length != N) {
			ax1 = new int[N];
			ax2 = new int[N];
			ay1 = new int[N];
			ay2 = new int[N];
			ai = new int[N];
			aj = new int[N];
		}
		int x = x1, n = 0;
		int nActiveLines = 0;
		for (int k = 0; k < lines.size(); k++) {
			TimeLine tl = (TimeLine) lines.elementAt(k);
			if (isActive(tl.objIdx)) {
				nActiveLines++;
			}
		}
		if (nActiveLines > 0) {
			for (int j = idxTFstart; j <= idxTFend; j++) {
				int sum = 0;
				for (int[] element : np) {
					sum += element[j - idxTFstart];
				}
				int y = 1, nextX = x1 + dx * (j + 1 - idxTFstart) / ntm;
				if (sum != 0) {
					for (int i = ncl - 1; i >= 0; i--) {
						int dy = 0;
						int classn = (i == ncl - 1) ? i : i - bias;
						if (classn < 0) {
							classn += ncl - 1;
						}
						dy = (d.height - 2) * np[classn][j - idxTFstart] / nActiveLines; //lines.size();
						g.setColor(classColours[classn]);
						if (i == 0) {
							dy = d.height - 1 - y;
						}
						g.fillRect(x + 1, y, nextX - x, dy);
						ai[n] = classn;
						aj[n] = j;
						ax1[n] = x + 1;
						ax2[n] = 1 + nextX;
						ay1[n] = y;
						ay2[n] = y + dy;
						n++;
						if (npsel[classn][j - idxTFstart] > 0) {
							int dysel = dy * npsel[classn][j - idxTFstart] / np[classn][j - idxTFstart];
							g.setColor(Color.black);
							for (int k = x; k <= nextX; k += 3) {
								g.drawLine(k, y + dy - dysel, k, y + dy);
							}
							g.drawLine(x, y + dy, nextX, y + dy);
						}
						y += dy;
					}
				}
				x = nextX;
				g.setColor(Color.black);
				g.drawLine(x, 0, x, d.height);
			}
		}
	}

	/**
	* Draws the graph
	*/
	@Override
	public void paint(Graphics g) {
		draw(g);
	}

	//===Mouse===
	@Override
	public void mouseReleased(MouseEvent e) {
		if (ax1 == null || ax1.length == 0)
			return;
		int x = e.getX(), y = e.getY(), n = -1;
		for (int k = 0; k < ax1.length; k++)
			if (x >= ax1[k] && x <= ax2[k] && y >= ay1[k] && y <= ay2[k]) {
				n = k;
				break;
			}
		if (n == -1)
			return;
		Vector vId = new Vector(50, 10);
		for (int k = 0; k < lines.size(); k++) {
			TimeLine tl = (TimeLine) lines.elementAt(k);
			if (!isActive(tl.objIdx)) {
				continue;
			}
			double v = tl.getValue(aj[n]);
			int classn = (Double.isNaN(v)) ? 1 + breaks.length : -1;
			for (int i = 0; classn == -1 && i < breaks.length; i++)
				if (v < breaks[i]) {
					classn = i;
				}
			if (classn == -1) {
				classn = breaks.length;
			}
			if (classn == ai[n]) {
				//System.out.println("* selected:"+tl.objId);
				vId.addElement(tl.objId);
			}
		}
		if (vId.size() > 0 && supervisor != null) {
			supervisor.getHighlighter(table.getEntitySetIdentifier()).makeObjectsSelected(this, vId);
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		if (popM == null || ax1 == null || ax1.length == 0)
			return;
		PopupManager.hideWindow();
		int x = e.getX(), y = e.getY(), n = -1;
		for (int k = 0; k < ax1.length; k++)
			if (x >= ax1[k] && x <= ax2[k] && y >= ay1[k] && y <= ay2[k]) {
				n = k;
				break;
			}
		if (n == -1) {
			popM.setKeepHidden(true);
		} else {
			String str = par.getValue(aj[n]).toString() + "\n"; //"Class N "+ai[n]+"\n\n";
			if (ai[n] == breaks.length + 1) {
				str += "Missing value\n";
			} else if (ai[n] == 0) {
				str += "Value < " + StringUtil.floatToStr(breaks[ai[n]], breaks[0], breaks[breaks.length - 1]) + "\n";
			} else if (ai[n] < breaks.length) {
				str += StringUtil.floatToStr(breaks[ai[n] - 1], breaks[0], breaks[breaks.length - 1]) + " <= Value < " + StringUtil.floatToStr(breaks[ai[n]], breaks[0], breaks[breaks.length - 1]) + "\n";
			} else {
				str += "Value >= " + StringUtil.floatToStr(breaks[breaks.length - 1], breaks[0], breaks[breaks.length - 1]) + "\n";
			}
			for (int k = 0; k < lines.size(); k++) {
				TimeLine tl = (TimeLine) lines.elementAt(k);
				if (!isActive(tl.objIdx)) {
					continue;
				}
				double v = tl.getValue(aj[n]);
				int classn = (Double.isNaN(v)) ? 1 + breaks.length : -1;
				for (int i = 0; classn == -1 && i < breaks.length; i++)
					if (v < breaks[i]) {
						classn = i;
					}
				if (classn == -1) {
					classn = breaks.length;
				}
				if (classn == ai[n])
					if (str.length() < 200) {
						str += dTable.getDataItemName(tl.objIdx) + "; ";
					} else {
						str += "...";
						break;
					}
			}
			str += "(" + np[ai[n]][aj[n] - idxTFstart] + " in total)\n";
			if (npsel[ai[n]][aj[n] - idxTFstart] > 0) {
				str += String.valueOf(npsel[ai[n]][aj[n] - idxTFstart]) + " of them are selected\n";
			}
			str += "Click to select all";
			popM.setText(str);
			popM.setKeepHidden(false);
			popM.startShow(e.getX(), e.getY());
		}
	}

	@Override
	public void mouseDragged(MouseEvent e) {
	}

	//===Mouse===

	protected void reflectDataChange() {
		countNP();
		countNPsel();
		draw(getGraphics());
	}

	// -------------- interface PropertyChangeListener begin
	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		if (pce.getSource().equals(tf)) {
			if (pce.getPropertyName().equals("destroyed")) {
				tf.removePropertyChangeListener(this);
				tf = null;
			} else {
				reflectDataChange();
			}
			return;
		}
		if (pce.getSource().equals(table)) {
			if (pce.getPropertyName().equals("destroyed") || pce.getPropertyName().equals("structure_complete")) {
				destroy();
			} else if (pce.getPropertyName().equals("filter")) {
				if (tf != null) {
					tf.removePropertyChangeListener(this);
				}
				tf = table.getObjectFilter();
				if (tf != null) {
					tf.addPropertyChangeListener(this);
				}
				reflectDataChange();
			} else if (pce.getPropertyName().equals("values")) {
				Vector v = (Vector) pce.getNewValue(); // list of changed attributes
				// check if the attributes are relevant to the graph
				if (v.indexOf(supAttr.getIdentifier()) >= 0) {
					reflectDataChange();
				}
			} else if (pce.getPropertyName().equals("data_added") || pce.getPropertyName().equals("data_removed") || pce.getPropertyName().equals("data_updated")) {
				reflectDataChange();
			}
		}
	}

	// ---------- interface PropertyChangeListener end

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(450, 200);
	}

	/**
	* Stops listening of all events, unregisters itself from object event sources
	*/
	@Override
	public void destroy() {
		supervisor.removeHighlightListener(this, table.getEntitySetIdentifier());
		//supervisor.removePropertyChangeListener(this);
		table.removePropertyChangeListener(this);
		if (tf != null) {
			tf.removePropertyChangeListener(this);
		}
		destroyed = true;
	}

	@Override
	public boolean isDestroyed() {
		return destroyed;
	}
}