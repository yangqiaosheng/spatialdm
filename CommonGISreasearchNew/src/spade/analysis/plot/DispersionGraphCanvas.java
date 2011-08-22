package spade.analysis.plot;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.TextField;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Hashtable;
import java.util.Vector;

import spade.analysis.classification.Classifier;
import spade.analysis.generators.PlotGeneratorsDescriptor;
import spade.analysis.system.Supervisor;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.FocusListener;
import spade.lib.basicwin.Focuser;
import spade.lib.basicwin.StringInRectangle;
import spade.lib.util.QSortAlgorithm;
import spade.time.TimeMoment;
import spade.vis.action.HighlightListener;
import spade.vis.action.ObjectEvent;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.DataTreater;
import spade.vis.database.ObjectFilter;
import spade.vis.event.EventSource;
import spade.vis.spec.SaveableTool;
import spade.vis.spec.ToolSpec;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: Fraunhofer AIS.SPADE</p>
 * @author unascribed
 * @version 1.0
 */

public class DispersionGraphCanvas extends Canvas implements HighlightListener, PropertyChangeListener, Destroyable, MouseListener, MouseMotionListener, FocusListener, EventSource, DataTreater, SaveableTool, PrintableImage {
	/**
	* Identifier of this visualization method
	*/
	protected String methodId = null;
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

	protected boolean destroyed = false;
	protected int instanceN = 0;
	protected static int nInstances = 0;

	protected double absMin = Double.NaN, absMax = Double.NaN;
	/**
	 * If the attribute is temporal, these fields contain the absolute minimum and
	 * maximum time moments.
	 */
	protected TimeMoment minTime = null, maxTime = null;

	protected double focusMin = Double.NaN, focusMax = Double.NaN;

	public Color bkgColor = Color.white, plotAreaColor = Color.lightGray;
	protected int mW = 10;
	protected Rectangle plotArea = null;
	protected DotPlotObject dtp = null;

	protected Vector dots[] = null;
//  protected Hashtable hiObj = null;
	protected boolean changed = true;
	protected static int dm = 8;
	protected int classNIndex[] = null;
	protected Color colorSet[] = null;

	/**
	 *
	 */
	protected boolean isInverse = false;

	/**
	* Used for informing possible listeners about selection of a reference
	* object or time moment by clicking in the plot.
	*/
	protected PropertyChangeSupport pcSupport = null;
	/**
	* As a SaveableTool, a plot may be registered somewhere and, hence, must
	* notify the component where it is registered about its destroying. This vector
	* contains the listeners to be notified about destroying of the visualizer.
	*/
	protected Vector destroyListeners = null;

	protected Focuser focuser = null;
	protected TextField currMinTF;
	protected TextField currMaxTF;

	public DispersionGraphCanvas() {
		instanceN = ++nInstances;
	}

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
		}
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(400, 300);
	}

	@Override
	public void paint(Graphics g) {
		draw(g);
	}

	protected void calcMinMax() {
		absMin = Double.NaN;
		absMax = Double.NaN;
		minTime = null;
		maxTime = null;
		if (table == null || attrId == null)
			return;
		int attrN = table.getAttrIndex(attrId);
		boolean temporal = table.isAttributeTemporal(attrN);
		for (int i = 0; i < table.getDataItemCount(); i++) {
			double val = table.getNumericAttrValue(attrN, i);
			if (Double.isNaN(val)) {
				continue;
			}
			if (Double.isNaN(absMin) || val < absMin) {
				absMin = val;
			}
			if (Double.isNaN(absMax) || val > absMax) {
				absMax = val;
			}
			if (temporal) {
				Object oval = table.getAttrValue(attrN, i);
				if (oval == null || !(oval instanceof TimeMoment)) {
					continue;
				}
				TimeMoment t = (TimeMoment) oval;
				if (minTime == null || minTime.compareTo(t) > 0) {
					minTime = t;
				}
				if (maxTime == null || maxTime.compareTo(t) < 0) {
					maxTime = t;
				}
			}
		}
		if (minTime != null) {
			minTime = minTime.getCopy();
		}
		if (maxTime != null) {
			maxTime = maxTime.getCopy();
		}
	}

	protected void constructFocuser() {
		if (focuser == null) {
			focuser = new Focuser();
			focuser.setSpacingFromAxis(-1);
			focuser.setBkgColor(bkgColor);
			focuser.setPlotAreaColor(plotAreaColor);
			focuser.addFocusListener(this);
			if (currMinTF != null && currMaxTF != null) {
				focuser.setTextFields(currMinTF, currMaxTF);
			}
			focuser.setAbsMinMax(absMin, absMax);
			if (minTime != null && maxTime != null) {
				focuser.setAbsMinMaxTime(minTime, maxTime);
			}
			focuser.setCurrMinMax(focusMin, focusMax);
		}
	}

	/**
	* Unlike the standard "paint", the method "draw" can be called directly,
	* i.e. not through an event. This is done for simplifying redrawing when
	* data/filter/classification/selection changes occur.
	*/
	public void draw(Graphics g) {
		Dimension size = getSize();
		calcMinMax();
		if (Double.isNaN(absMin) || Double.isNaN(absMin) || absMax == absMin)
			return;

		if (focuser == null) {
			constructFocuser();
		}

		FontMetrics fm = g.getFontMetrics();
		int asc = fm.getAscent();
		int ah = asc + focuser.getBounds().height;

		if (Double.isNaN(focusMin)) {
			focusMin = absMin;
		}
		if (Double.isNaN(focusMax)) {
			focusMax = absMax;
		}

		plotArea = new Rectangle(mW, 0, size.width - mW * 2, 0);
		int maxN = 0;

		if (isInverse) { // down
			focuser.setIsLeft(true); //right position
			focuser.setAlignmentParameters(mW, ah, size.width - mW * 2); //right position
			plotArea.height = size.height - ah;// - asc;
			maxN = plotArea.height / dm;
			plotArea.height = maxN * dm;
			plotArea.y = ah + 1;
		} else { // up
			focuser.setIsLeft(false);
			focuser.setAlignmentParameters(mW, size.height - ah, size.width - mW * 2); //right positi
			plotArea.height = size.height - ah /*- asc*/- 3;
			maxN = plotArea.height / dm;
			plotArea.height = maxN * dm;
			plotArea.y = size.height - ah - plotArea.height;

		}
		focuser.draw(g);
		drawPlotArea(g);
	}

	protected void drawPlotArea(Graphics g) {
		g.setColor(Color.lightGray);
		g.fillRect(plotArea.x, plotArea.y, plotArea.width, plotArea.height);
		g.setColor(Color.gray);
		if (dtp == null) {
			dtp = new DotPlotObject();
		}

		int n = plotArea.width / dm;
		int d = (plotArea.width - n * dm) / 2;

		calcDots();
		setSelectedDots();

		g.setClip(plotArea);
		drawAllDots(g);
	}

	public void setTextFields(TextField min, TextField max) {
		currMinTF = min;
		currMaxTF = max;
		if (focuser != null) {
			focuser.setTextFields(currMinTF, currMaxTF);
		}
	}

	public void setCurrMinMax(double min, double max) {
		if (focuser == null) {
			constructFocuser();
		}
		focuser.setCurrMinMax(min, max);
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

	protected void calcDots() {
		dotHt.clear();
		int attrN = table.getAttrIndex(attrId);
		dots = new Vector[plotArea.width / dm];
		double val = 0;
		for (int i = 0; i < table.getDataItemCount(); i++) {
			val = table.getNumericAttrValue(attrN, i);
			if (Double.isNaN(val)) {
				continue;
			}
			int n = (int) ((val - focusMin) * dots.length / (focusMax - focusMin));
			if (n >= dots.length) {
				n--;
			}
			if (n < dots.length && n >= 0 && !Double.isNaN(val) && isActive(i)) {
				if (dots[n] == null) {
					dots[n] = new Vector();
				}
				String id = table.getDataItemId(i);
				dots[n].addElement(new DotObject(id, val));
				dotHt.put(id, new Point(n, dots[n].size() - 1));
			}
		}
		setClassNDots();
		sortClassNDots();
	}

	/**
	* Used when data/filter/classification/selection changes occur.
	*/
	public void redraw() {
		draw(getGraphics());
	}

	public void setInverse(boolean s) {
		isInverse = s;
		changed = true;
		repaint();
	}

	public boolean isInverse() {
		return isInverse;
	}

	protected Hashtable dotHt = new Hashtable();

	protected Point findDotPos(String id) {
		return (Point) dotHt.get(id);
	}

	protected Point findDotPos(int x, int y) {
		if (plotArea == null)
			return null;
		int i = (x - plotArea.x) / dm;
		int j = 0;

		if (isInverse) {
			j = (y - plotArea.y) / dm;
		} else {
			j = (plotArea.y + plotArea.height - y) / dm;
		}

		// System.out.println("ij:"+i+":"+j);
		return new Point(i, j);
	}

	protected String findId(int x, int y) {
		Point dp = findDotPos(x, y);
		if (dp == null)
			return null;
		String id = null;
		try {
			if (dots[dp.x] != null) {
				id = ((DotObject) dots[dp.x].elementAt(dp.y)).id;
			}

		} catch (ArrayIndexOutOfBoundsException ex) {
		}
		//System.out.println("id:"+id);
		return id;
	}

	protected void drawFrame(int x0, int y0, int x, int y) {
		Graphics gr = getGraphics();
		gr.setColor(Color.magenta);
		gr.setXORMode(plotAreaColor);
		gr.drawLine(x0, y0, x, y0);
		gr.drawLine(x, y0, x, y);
		gr.drawLine(x, y, x0, y);
		gr.drawLine(x0, y, x0, y0);
		gr.setPaintMode();
		gr.dispose();
	}

	protected Vector highObj;

	//-------------------------------- HighlightListener ----------------------------------
	@Override
	public void highlightSetChanged(Object source, String setId, Vector highlighted) {
		Graphics g = getGraphics();
		if (highObj != null) {
			for (int i = 0; i < highObj.size(); i++) {
				String id = (String) highObj.elementAt(i);
				Point p = findDotPos(id);
				if (p != null) {
					Color color = getDotColor(p.x, p.y);
					g.setColor(color);
					drawDot(g, p.x, p.y, false, false);
				}
			}
		}

		if (highlighted != null) {
			for (int i = 0; i < highlighted.size(); i++) {
				String id = (String) highlighted.elementAt(i);
				Point p = findDotPos(id);
				if (p != null) {
					g.setColor(Color.white);
					drawDot(g, p.x, p.y, false, true);
				}
			}
		}

		highObj = highlighted;

		/*
		Hashtable ht = new Hashtable();
		if(highlighted != null ){
		for(int i = 0; i< highlighted.size(); i++){
		   String id = (String)highlighted.elementAt(i);
		   if(hiObj != null) hiObj.remove(id);
		   Point p = findDotPos(id);
		   if(p != null)ht.put(id,p);
		}
		}

		Graphics g = getGraphics();
		//g.setColor(Color.gray);
		Enumeration enum = null;
		if(hiObj != null){
		  enum = hiObj.elements();
		  while (enum.hasMoreElements()) {
		    Point p = (Point)enum.nextElement();
		    g.setColor(getDotColor(p.x,p.y));
		    drawDot(g,p.x,p.y,false);
		  }
		}
		g.setColor(Color.white);
		enum = ht.elements();
		while (enum.hasMoreElements()) {
		  Point p = (Point)enum.nextElement();
		  drawDot(g,p.x,p.y,false);
		}


		hiObj = ht;
		g.dispose();
		*/

	}

	public void shiftColors() {
		if (classNIndex == null)
			return;
		int n = classNIndex[1];
		for (int i = 1; i < classNIndex.length - 1; i++) {
			classNIndex[i] = classNIndex[i + 1];
		}
		classNIndex[classNIndex.length - 1] = n;

		//for(int i = 0; i < classNIndex.length; i++){
		//  System.out.print(" "+classNIndex[i]);
		//}

		//System.out.println();

		sortClassNDots();
		drawAllDots(getGraphics());

	}

	protected Color getDotColor(int i, int j) {
		if (dots[i] != null) {
			if (j < dots[i].size()) {
				DotObject dt = (DotObject) dots[i].elementAt(j);
				Color color = Color.gray;
				if (colorSet != null) {
					color = colorSet[dt.classN + 1];
				}
				return color;
			}
		}
		return null;
	}

	protected void setSelectedDots() {

		for (Vector dot : dots) {
			if (dot != null) {
				for (int j = 0; j < dot.size(); j++) {
					DotObject dt = (DotObject) dot.elementAt(j);
					dt.isSelected = false;
				}
			}
		}
		Vector so = supervisor.getHighlighter(table.getEntitySetIdentifier()).getSelectedObjects();
		if (so == null)
			return;
		for (int i = 0; i < so.size(); i++) {
			String id = (String) so.elementAt(i);
			Point p = findDotPos(id);
			if (p != null && dots[p.x] != null) {
				DotObject dt = (DotObject) dots[p.x].elementAt(p.y);
				dt.isSelected = true;
			}
		}
	}

	protected void setClassNDots() {
		Classifier cl = null;
		if (supervisor != null && supervisor.getObjectColorer() != null && (supervisor.getObjectColorer() instanceof Classifier) && supervisor.getObjectColorer().getEntitySetIdentifier().equals(table.getEntitySetIdentifier())) {
			cl = (Classifier) supervisor.getObjectColorer();
		}
		if (cl == null) {
			colorSet = null;
			classNIndex = null;
			return;
		}

		int attrN = table.getAttrIndex(attrId);

		colorSet = new Color[cl.getNClasses() + 1];
		classNIndex = new int[cl.getNClasses() + 1];
		colorSet[0] = Color.darkGray; // NaN
		classNIndex[0] = 0;
		for (int i = 1; i < colorSet.length; i++) {
			colorSet[i] = (cl.isClassHidden(i - 1)) ? Classifier.hiddenClassColor : cl.getClassColor(i - 1);
			classNIndex[i] = i;
		}

		for (Vector dot : dots) {
			if (dot != null) {
				for (int j = 0; j < dot.size(); j++) {
					DotObject dt = (DotObject) dot.elementAt(j);
					int classN = cl.getObjectClass(dt.id);
					dt.classN = classN;
					dt.cInd = classNIndex;
				}
			}
		}

	}

	protected void sortClassNDots() {

		for (int i = 0; i < dots.length; i++) {
			if (dots[i] != null) {
				QSortAlgorithm.sort(dots[i]);
				for (int j = 0; j < dots[i].size(); j++) {
					DotObject dt = (DotObject) dots[i].elementAt(j);
					dotHt.put(dt.id, new Point(i, j));
				}
			}
		}
		//qsort
		// dotHt->new id->ij

	}

	protected void drawAllDots(Graphics g) {
		for (int i = 0; i < dots.length; i++) {

			Vector v = dots[i];
			if (v == null) {
				continue;
			}
			for (int j = 0; j < v.size(); j++) {
				drawDot(g, i, j, true, false);

			}
		}
	}

	protected void selectObjectAt(int x, int y, MouseEvent e) {
		if (!plotArea.contains(x, y))
			return;
		ObjectEvent oevt = new ObjectEvent(this, (e.getClickCount() > 1) ? ObjectEvent.dblClick : ObjectEvent.click, e, table.getEntitySetIdentifier());
		String id = findId(x, y);
		if (id != null) {
			oevt.addEventAffectedObject(id);
			supervisor.processObjectEvent(oevt);
		}
	}

	protected void drawDot(Graphics g, int i, int j, boolean drawLimit, boolean highligh) {
		int n = plotArea.width / dm;
		dtp.x = plotArea.x + i * dm + dm / 2;

		Color fgc = Color.gray;
		Color bkc = Color.lightGray;
		Color bkc2 = Color.lightGray;

		if (dots[i] != null) {
			if (j < dots[i].size()) {
				DotObject dt = (DotObject) dots[i].elementAt(j);
				if (colorSet != null) {
					fgc = colorSet[dt.classN + 1];
				}
				if (dt.isSelected) {
					bkc = Color.black;
				}
				if (highligh) {
					bkc = bkc2 = Color.white;
				}
			}
		}

		if (isInverse) {
			dtp.y = plotArea.y + j * dm + (dm - 1) / 2;
			if (dtp.y < plotArea.y + plotArea.height) {
				g.setColor(bkc2);
				g.fillOval(dtp.x - 4, dtp.y - 4, 9, 9);
				g.setColor(bkc);
				dtp.fill(g);
				g.setColor(fgc);
				dtp.draw(g);

			} else if (drawLimit) {
				g.setColor(Color.red);
				g.drawLine(dtp.x - dm / 2, plotArea.y + plotArea.height - 1, dtp.x + dm / 2, plotArea.y + plotArea.height - 1);

			}

		} else {
			dtp.y = plotArea.y + plotArea.height - j * dm - (dm - 1) / 2 - 1;
			if (dtp.y > plotArea.y) {
				g.setColor(bkc2);
				g.fillOval(dtp.x - 4, dtp.y - 4, 9, 9);
				g.setColor(bkc);
				dtp.fill(g);
				g.setColor(fgc);
				dtp.draw(g);
			} else if (drawLimit) {
				g.setColor(Color.red);
				g.drawLine(dtp.x - dm / 2, plotArea.y + 1, dtp.x + dm / 2, plotArea.y + 1);

			}

		}

	}

	@Override
	public void selectSetChanged(Object source, String setId, Vector selected) {

		if (!setId.equalsIgnoreCase(table.getEntitySetIdentifier()))
			return;
		setSelectedDots();
		drawAllDots(getGraphics());

	}

	//-------------------------------- PropertyChangeListener ---------------------------
	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		if (pce.getPropertyName().equals(Supervisor.eventObjectColors)) {
			if (table.getEntitySetIdentifier().equals(pce.getNewValue())) {
				//do some actions on the change of the classes or class colors
				setClassNDots();
				sortClassNDots();
				drawAllDots(getGraphics());
			}
		} else if (pce.getSource().equals(filter)) { //change of the filter
			if (pce.getPropertyName().equals("destroyed")) {
				filter.removePropertyChangeListener(this);
				filter = null;
			} else {
				//do some actions on the change of the filter
				redraw();
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
					//do some actions on the change of the filter
				}

			} else if (pce.getPropertyName().equals("values")) { //values of some attribute(s) changed
				Vector v = (Vector) pce.getNewValue(); // list of changed attributes
				if (v != null && v.contains(attrId)) {
					//Values of the attribute represented on this graph have changed.
					//Do some actions on the change of the values:

					redraw();
				}
			} else if (pce.getPropertyName().equals("data_added") || pce.getPropertyName().equals("data_removed") || pce.getPropertyName().equals("data_updated")) {
				//Some rows in the table have been added/removed/changed
				//Do some actions on the change of the data:
				//...

				redraw();
			}
		}

	}

	//--------------------------------------- Destroyable -----------------------------------
	@Override
	public void destroy() {
		supervisor.removeHighlightListener(this, table.getEntitySetIdentifier());
		supervisor.removePropertyChangeListener(this);
		table.removePropertyChangeListener(this);
		if (filter != null) {
			filter.removePropertyChangeListener(this);
		}
		destroyed = true;
		if (destroyListeners != null) {
			PropertyChangeEvent pce = new PropertyChangeEvent(this, "destroyed", null, null);
			for (int i = 0; i < destroyListeners.size(); i++) {
				((PropertyChangeListener) destroyListeners.elementAt(i)).propertyChange(pce);
			}
		}
	}

	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

//ID
	public int getInstanceN() {
		return instanceN;
	}

//~ID

	//---------------------------------------- MouseListener --------------------------------
	@Override
	public void mouseClicked(MouseEvent e) {

	}

	@Override
	public void mousePressed(MouseEvent e) {

		int x = e.getX(), y = e.getY();
		if (plotArea.contains(x, y)) {
			dragX1 = dragX2 = x;
			dragY1 = dragY2 = y;

		} else {
			focuser.captureMouse(e.getX(), e.getY());
		}

	}

	@Override
	public void mouseReleased(MouseEvent e) {

		int x = e.getX();
		int y = e.getY();
		if (focuser.captureMouse(x, y)) {
			focuser.releaseMouse();

			lastPos[0] = lastPos[1] = -1;
			return;
		}

		if (!dragging) {

			if (plotArea.contains(x, y)) {
				ObjectEvent oevt = new ObjectEvent(this, (e.getClickCount() > 1) ? ObjectEvent.dblClick : ObjectEvent.click, e, table.getEntitySetIdentifier());

				Point p = findDotPos(x, y);
				if (p != null && dots[p.x] != null) {
					if (dots[p.x].size() > p.y) {
						DotObject dtt = (DotObject) dots[p.x].elementAt(p.y);
						for (int i = 0; i < dots[p.x].size(); i++) {
							DotObject dt = (DotObject) dots[p.x].elementAt(i);
							if (classNIndex == null || (classNIndex != null && dtt.classN == dt.classN)) {
								oevt.addEventAffectedObject(dt.id);
							}
						}
					} else {

					}
					supervisor.processObjectEvent(oevt);
				}
			}

		} else {
/*
      drawFrame(dragX1,dragY1,dragX2,dragY2);
      dragging=false;
      Point p1 = findDotPos(dragX1,dragY1);
      Point p2 = findDotPos(dragX2,dragY2);
      if(p1.x > p2.x) {int a = p1.x; p1.x = p2.x; p2.x = a; }
      if(p1.y > p2.y) {int a = p1.y; p1.y = p2.y; p2.y = a; }

      ObjectEvent oevt=new ObjectEvent(this,ObjectEvent.frame,e,table.getEntitySetIdentifier());
      for(int i = p1.x; i <= p2.x; i++){
        if(dots[i] != null){
          for(int j = p1.y; j < dots[i].size() && j <= p2.y; j++){
            DotObject dt = (DotObject)dots[i].elementAt(j);
            oevt.addEventAffectedObject(dt.id);
          }
        }

      }

      supervisor.processObjectEvent(oevt);

      setCursor(Cursor.getDefaultCursor());
      */
		}

		dragX1 = dragY1 = dragX2 = dragY2 = -1;
	}

	@Override
	public void mouseEntered(MouseEvent e) {

	}

	@Override
	public void mouseExited(MouseEvent e) {

	}

	protected int dragX1 = -1, dragY1 = -1, dragX2 = dragX1, dragY2 = dragY1;
	protected boolean dragging = false;

	@Override
	public void mouseDragged(MouseEvent e) {
		int x = e.getX(), y = e.getY();
		if (!dragging && focuser.captureMouse(x, y)) {
			focuser.mouseDragged(x, y, getGraphics());
			return;
		}
		/*
		if (!dragging && !plotArea.contains(dragX1,dragY1)) return;
		dragging =dragging || Math.abs(x-dragX1)>5 || Math.abs(y-dragY1)>5;
		if (!dragging) return;
		if(x > plotArea.x+plotArea.width) x = plotArea.x+plotArea.width;
		if(y > plotArea.y+plotArea.height) y = plotArea.y+plotArea.height;
		if(x < plotArea.x) x = plotArea.x;
		if(y < plotArea.y) y = plotArea.y;

		if (x==dragX2 && y==dragY2) return;
		drawFrame(dragX1,dragY1,dragX2,dragY2);
		dragX2=x; dragY2=y;
		drawFrame(dragX1,dragY1,dragX2,dragY2);
		if ((dragX2-dragX1)*(dragY2-dragY1)>0)
		  setCursor(new Cursor(Cursor.NW_RESIZE_CURSOR));
		else
		  setCursor(new Cursor(Cursor.NE_RESIZE_CURSOR));
		*/
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		int x = e.getX(), y = e.getY();
		String id = findId(x, y);
		if (id == null) {

			if (id == null) {
				ObjectEvent oevt = new ObjectEvent(this, ObjectEvent.point, e, table.getEntitySetIdentifier());
				//to dehighlight all the objects
				supervisor.processObjectEvent(oevt);
			}
			return;
		}

		ObjectEvent oevt = new ObjectEvent(this, ObjectEvent.point, e, table.getEntitySetIdentifier());

		oevt.addEventAffectedObject(id);
		supervisor.processObjectEvent(oevt);

	}

	//----------------------------- Focuser Listener ----------------------------------------
	@Override
	public void focusChanged(Object source, double lowerLimit, double upperLimit) {

		//System.out.println("focusChanged");
		if (source.equals(focuser)) {
			if (focusMin != lowerLimit || focusMax != upperLimit) {
				focusMin = lowerLimit;
				focusMax = upperLimit;
				changed = true;
				repaint();
			}

		}

	}

	protected int lastPos[] = new int[] { -1, -1 };

	public void drawHorLimit(int n, int xpos) {
		Graphics g = getGraphics();
		g.setColor(Color.magenta);
		g.setXORMode(plotAreaColor);

		if (lastPos[n] >= 0) {
			g.drawLine(lastPos[n], plotArea.y + plotArea.height, lastPos[n], plotArea.y);
		}

		if (xpos >= plotArea.x && xpos < plotArea.x + plotArea.width) {
			if (isInverse) {
				g.drawLine(n == 0 ? focuser.getMinPos() : focuser.getMaxPos(), focuser.getAxisPosition(), xpos, plotArea.y - 1);
			} else {
				g.drawLine(n == 0 ? focuser.getMinPos() : focuser.getMaxPos(), focuser.getAxisPosition(), xpos, plotArea.y + plotArea.height);
			}
			g.drawLine(xpos, plotArea.y + plotArea.height, xpos, plotArea.y);
			lastPos[n] = xpos;
		}

	}

	@Override
	public void limitIsMoving(Object source, int n, double currValue) {
		if (source.equals(focuser)) {
			int x = plotArea.x + (int) Math.round((plotArea.width * (currValue - focusMin)) / (focusMax - focusMin));
			drawHorLimit(n, x);

		}
	}

	//----------------------------- Event Source --------------------------------------------
	@Override
	public boolean doesProduceEvent(String eventId) {
		return eventId != null && (eventId.equals(ObjectEvent.click) || eventId.equals(ObjectEvent.dblClick) || eventId.equals(ObjectEvent.point));

	}

	@Override
	public String getIdentifier() {
		return "DispersionGraphCanvas_" + instanceN;
	}

	//--------------------- DataTreater interface -----------------------------
	/**
	* Returns vector of IDs of attribute(s) on this display
	*/
	@Override
	public Vector getAttributeList() {
		Vector a = null;
		if (table != null) {
			a = new Vector();
			a.addElement(attrId);
		}
		return a;

	}

	/**
	* Replies whether it is linked to the data set (table) with the given identifier
	*/
	@Override
	public boolean isLinkedToDataSet(String setId) {
		return setId != null && table != null && setId.equals(table.getContainerIdentifier());
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
		if (table != null) {
			spec.table = table.getContainerIdentifier();
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
		Hashtable ht = new Hashtable();
		ht.put("inverse", (isInverse()) ? "true" : "false");
		if (focuser != null) {
			ht.put("focus_min", String.valueOf(focuser.getCurrMin()));
			ht.put("focus_max", String.valueOf(focuser.getCurrMax()));
		}
		return ht;
	}

	/**
	* After the plot is constructed, it may be requested to setup its individual
	* properties according to the given list of stored properties.
	* The base Plot class does nothing in this method.
	*/
	@Override
	public void setProperties(Hashtable properties) {
		if (properties == null)
			return;
		String str = (String) properties.get("inverse");
		if (str != null) {
			setInverse(str.equalsIgnoreCase("true"));
		}
		double min = Double.NaN, max = Double.NaN;
		str = (String) properties.get("focus_min");
		if (str != null) {
			try {
				min = Double.valueOf(str).doubleValue();
			} catch (NumberFormatException nfe) {
			}
		}
		if (!Double.isNaN(min)) {
			str = (String) properties.get("focus_max");
			if (str != null) {
				try {
					max = Double.valueOf(str).floatValue();
				} catch (NumberFormatException nfe) {
				}
			}
			if (!Double.isNaN(max)) {
				setCurrMinMax(min, max);
			}
		}
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
		int gap = 2;
		StringBuffer sb = new StringBuffer();
		StringInRectangle str = new StringInRectangle();
		str.setPosition(StringInRectangle.Center);
		sb.append(table.getAttributeName(table.getAttrIndex((String) getAttributeList().elementAt(0))));
		str.setString(sb.toString());
		Dimension lsize = str.countSizes(getGraphics());
		int w = getBounds().width;
		int h = getBounds().height;
		str.setRectSize(w, lsize.width * lsize.height / w * 2);
		lsize = str.countSizes(getGraphics());
		Image li = createImage(w, lsize.height);
		str.draw(li.getGraphics());
		Image img = createImage(w, h + lsize.height + 2 * gap);
//    Image img = createImage(w, h);
		draw(img.getGraphics());
		img.getGraphics().drawImage(li, 0, h + gap, null);
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

///////////////////////////////////////////////////////////

class DotObject implements spade.lib.util.Comparable {
	public String id = null;
	public boolean isSelected = false;//, isHighlighted = false;
	public int classN = -1;
	public double value = Double.NaN;
	public int cInd[] = null;

	public DotObject(String id, double value) {
		this.id = id;
		this.value = value;
	}

	/**
	*  Returns 0 if equal, <0 if THIS is less than the argument, >0 otherwise
	*/
	@Override
	public int compareTo(spade.lib.util.Comparable c) {
		if (cInd == null)
			return 0;
		int n = ((DotObject) c).classN;
		int a = cInd[classN + 1];
		int b = cInd[n + 1];
		if (a < b)
			return 1;
		if (a > b)
			return -1;
		return 0;
	}

}
