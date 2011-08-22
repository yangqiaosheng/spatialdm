package spade.analysis.classification;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Hashtable;
import java.util.Vector;

import spade.analysis.generators.PlotGeneratorsDescriptor;
import spade.analysis.plot.PrintableImage;
import spade.analysis.system.Supervisor;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.PopupManager;
import spade.lib.basicwin.StringInRectangle;
import spade.lib.util.GraphGridSupport;
import spade.lib.util.GridPosition;
import spade.lib.util.IntArray;
import spade.vis.action.HighlightListener;
import spade.vis.action.ObjectEvent;
import spade.vis.database.AttributeDataPortion;
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

public class RangedDistCanvas extends Canvas implements PropertyChangeListener, HighlightListener, MouseListener, MouseMotionListener, Destroyable, SaveableTool, PrintableImage {
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

	Supervisor sup;
	Classifier cl;
	/**
	* Indicates "destroyed" state. Initially is false.
	*/
	protected boolean destroyed = false;
	/**
	* As a SaveableTool, a plot may be registered somewhere and, hence, must
	* notify the component where it is registered about its destroying. This vector
	* contains the listeners to be notified about destroying of the visualizer.
	*/
	protected Vector destroyListeners = null;

	int mH = 2, mW = 2;
	Rectangle plotArea;

	long distVal[] = null;
	Color colorSet[] = null;
	long maxV = 0;
	IndexItem index[] = null;
	long selectedDistVal[] = null;
	boolean isRanged = true;
	boolean isMaxV = false;

	/**
	* Used for showing objects in each box
	*/
	protected PopupManager popM = null;

	public RangedDistCanvas(Supervisor sup, Classifier classifier) {
//ID
		instanceN = ++nInstances;
//~ID
		this.sup = sup;
		this.cl = classifier;
		cl.addPropertyChangeListener(this);
		addMouseListener(this);
		addMouseMotionListener(this);
		if (popM == null) {
			popM = new PopupManager(this, "", true);
			popM.setOnlyForActiveWindow(false);
		}
		if (sup != null) {
			sup.addPropertyChangeListener(this);
			//Classifier cl=(Classifier)sup.getObjectColorer();
			if (cl != null) {
				TableClassifier tcl = null;
				if (cl instanceof TableClassifier) {
					tcl = (TableClassifier) cl;
				}
				if (tcl != null) {
					AttributeDataPortion table = tcl.getTable();
					sup.registerHighlightListener(this, table.getEntitySetIdentifier());

				}
			}
		}

	}

	@Override
	public Dimension getPreferredSize() {
		int mm = Math.round(getToolkit().getScreenResolution() / 25.33f);

		return new Dimension(30 * mm, 40 * mm);
	}

	@Override
	public void paint(Graphics g) {
		draw(g);

	}

	public void draw(Graphics g) {

		calcDist();

		if (g == null)
			return;

		Dimension d = getSize();
		FontMetrics fm = g.getFontMetrics();
		int rw = fm.stringWidth(String.valueOf(maxV) + "00") + 2;
		plotArea = new Rectangle(rw, mH, d.width - (mW + rw), d.height - mH * 2);
		int ws = (plotArea.width / distVal.length) * distVal.length;
		int dw = plotArea.width - ws;
		plotArea.width = ws;
		g.setColor(Color.white);
		g.fillRect(plotArea.x + plotArea.width, plotArea.y, dw + 1, plotArea.height + 1);

		g.setColor(Color.white);
		g.fillRect(0, 0, rw, d.height);
		//g.setColor(Color.lightGray);
		//g.fillRect(plotArea.x,plotArea.y,plotArea.width,plotArea.height);
		if (maxV == 0)
			return;
		if (distVal == null)
			return;
		if (distVal.length == 0)
			return;
		Rectangle b = plotArea;
		String str = String.valueOf(Math.round(maxV));

		g.setColor(Color.black);
		g.drawString(str, b.x - fm.stringWidth(str) - 2, b.y + fm.getAscent());

		int asc = fm.getAscent(), fh = fm.getHeight();

		g.setColor(Color.lightGray);
		for (int i = 0; i < distVal.length; i++) {
			int x = i * (plotArea.width / distVal.length);
			int w = (plotArea.width / distVal.length);
			int h = Math.round(distVal[index[i].index] * plotArea.height / maxV);

			g.fillRect(plotArea.x + x + 1, plotArea.y, w, plotArea.height - h + 1);
		}

		GridPosition grpos[] = GraphGridSupport.makeGrid(0, maxV, 0, b.height, 3 * fh, 5 * fh);
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

		for (int i = 0; i < distVal.length; i++) {
			int x = i * (plotArea.width / distVal.length);
			int w = (plotArea.width / distVal.length);
			int h = Math.round(distVal[index[i].index] * plotArea.height / maxV);
			g.setColor(colorSet[index[i].index]);
			g.fillRect(plotArea.x + x + 1, plotArea.y + plotArea.height - h + 1, w, h - 1);

			if (selectedDistVal != null && maxV > 0) {
				h = Math.round(selectedDistVal[index[i].index] * plotArea.height / maxV);
				if (h > 0) {
					fillVerHatchRect(g, plotArea.x + x + 1, plotArea.y + plotArea.height - h + 1, w, h - 1);
				}
			}

		}

		g.setColor(Color.black);
		g.drawRect(plotArea.x, plotArea.y, plotArea.width, plotArea.height);

	}

	protected void fillVerHatchRect(Graphics g, int x, int y, int w, int h) {
		//if(h == 0) return;
		g.setColor(Color.black);
		for (int i = 0; i < w; i += 2) {
			g.drawLine(x + i, y, x + i, y + h);
		}

	}

	protected void calcDist() {
		distVal = null;
		//Classifier cl=(Classifier)sup.getObjectColorer();
		if (cl == null)
			return;
		TableClassifier tcl = null;
		if (cl instanceof TableClassifier) {
			tcl = (TableClassifier) cl;
		} else
			return;

		IntArray a = tcl.getClassSizes();
		if (a == null)
			return;
		distVal = new long[a.size()];
		colorSet = new Color[a.size()];
		index = new IndexItem[a.size()];
		//maxV = 0;
		for (int i = 0; i < distVal.length; i++) {
			distVal[i] = a.elementAt(i);
			colorSet[i] = tcl.getClassColor(i);
			if (maxV < distVal[i] && !isMaxV) {
				maxV = distVal[i];

			}
			index[i] = new IndexItem(distVal);
			index[i].index = i;
		}

		if (isRanged) {
			spade.lib.util.QSortAlgorithm.sort(index);
		}

		calcSelectedDist();
	}

	@Override
	public void propertyChange(PropertyChangeEvent pce) {

		String pn = pce.getPropertyName();
		if (pn.equals("classes") || pn.equals("colors")) {
			//do some actions on the change of the classes or class colors

			draw(getGraphics());

		}

		//System.out.println(pce.getPropertyName());

	}

	@Override
	public void highlightSetChanged(Object source, String setId, Vector highlighted) {

	}

	@Override
	public void selectSetChanged(Object source, String setId, Vector selected) {

		// if(distVal == null) //;
		//if (!setId.equalsIgnoreCase(table.getEntitySetIdentifier())) return;
		repaint();
		//draw(getGraphics());
	}

	public void FitToBorder() {
		if (isMaxV)
			return;
		maxV = 0;
	}

	public void setRanged(boolean val) {
		isRanged = val;
		repaint();

	}

	public void setCommonMaxV(long v) {
		if (v == 0) {
			maxV = 0;
			isMaxV = false;
		} else {
			maxV = v;
			isMaxV = true;
		}
	}

	protected void calcSelectedDist() {

		selectedDistVal = new long[distVal.length];
		if (sup != null) {
			//Classifier cl=(Classifier)sup.getObjectColorer();
			if (cl != null) {
				TableClassifier tcl = null;
				if (cl instanceof TableClassifier) {
					tcl = (TableClassifier) cl;
				}

				if (tcl != null) {

					AttributeDataPortion table = tcl.getTable();
					Vector so = sup.getHighlighter(table.getEntitySetIdentifier()).getSelectedObjects();

					if (so != null) {

						for (int i = 0; i < so.size(); i++) {
							int n = tcl.getObjectClass((String) so.elementAt(i));
							if (n >= 0 && n < selectedDistVal.length) {
								selectedDistVal[n]++;
							}
						}
					} else {
						selectedDistVal = null;
					}

				}
			}

		}

	}

	protected int findClassN(int x0, int y0) {
		if (maxV == 0)
			return -1;
		int ii = -1;
		for (int i = 0; i < distVal.length; i++) {
			int x = i * (plotArea.width / distVal.length);
			int w = (plotArea.width / distVal.length);
			int h = Math.round(distVal[index[i].index] * plotArea.height / maxV);
			Rectangle r = new Rectangle(plotArea.x + x + 1, plotArea.y + plotArea.height - h + 1, w, h - 1);
			if (r.contains(x0, y0)) {
				ii = index[i].index;
				break;
			}
		}
		return ii;
	}

	@Override
	public void mousePressed(MouseEvent e) {
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		int x0 = e.getX();
		int y0 = e.getY();
		if (plotArea.contains(x0, y0)) {
			if (distVal == null)
				return;
			int ii = findClassN(x0, y0);

			TableClassifier tcl = null;
			if (cl instanceof TableClassifier) {
				tcl = (TableClassifier) cl;
			} else
				return;
			AttributeDataPortion table = tcl.getTable();
			ObjectEvent oevt = new ObjectEvent(this, (e.getClickCount() > 1) ? ObjectEvent.dblClick : ObjectEvent.click, e, table.getEntitySetIdentifier());

			if (ii >= 0) {
				for (int i = 0; i < table.getDataItemCount(); i++) {
					String id = table.getDataItemId(i);
					if (ii == cl.getObjectClass(id)) {
						oevt.addEventAffectedObject(id);
					}
				}
			}
			if (sup != null) {
				sup.processObjectEvent(oevt);
			}
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void mouseDragged(MouseEvent e) {
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		if (popM == null || plotArea == null)
			return;
		PopupManager.hideWindow();
		int x = e.getX(), y = e.getY();
		if (!plotArea.contains(x, y))
			return;

		int cN = findClassN(x, y);
		if (cN < 0) {
			popM.setKeepHidden(true);
			return;
		}
		String txt = cl.getClassName(cN) + "\n";

		TableClassifier tcl = null;
		if (cl instanceof TableClassifier) {
			tcl = (TableClassifier) cl;
		} else
			return;
		AttributeDataPortion table = tcl.getTable();
		int n = 0;
		for (int i = 0; i < table.getDataItemCount(); i++) {
			String id = table.getDataItemId(i);
			if (cN == cl.getObjectClass(id)) {
				if (n < 10 && n > 0) {
					txt += "; ";
				}
				if (n < 10) {
					txt += table.getDataItemName(i);
				}
				// selected ?
				n++;
			}
		}
		if (n >= 10) {
			txt += " ... (" + n + " in total)\n";
		} else {
			txt += "\n";
		}
		if (selectedDistVal != null && cN >= 0 && selectedDistVal[cN] > 0) {
			txt += selectedDistVal[cN] + " of them are selected\n";
		}
		txt += "Click to select all";
		popM.setText(txt);
		popM.setKeepHidden(false);
		popM.startShow(x, y);
	}

	/**
	* Makes necessary operations for destroying, e.g. unregisters from
	* listening highlighting and other events.
	*/
	@Override
	public void destroy() {
		if (sup != null) {
			sup.removePropertyChangeListener(this);
			if (cl != null) {
				TableClassifier tcl = null;
				if (cl instanceof TableClassifier) {
					tcl = (TableClassifier) cl;
				}
				if (tcl != null) {
					AttributeDataPortion table = tcl.getTable();
					sup.removeHighlightListener(this, table.getEntitySetIdentifier());
				}
			}
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
		if (cl != null) {
			TableClassifier tcl = null;
			if (cl instanceof TableClassifier) {
				tcl = (TableClassifier) cl;
			}
			if (tcl != null) {
				spec.table = tcl.getTable().getContainerIdentifier();
				spec.attributes = tcl.getAttributeList();
			}
		}
		spec.properties = getProperties();
		return spec;
	}

	/**
	* Returns custom properties of the tool: String -> String
	* By default, returns null.
	*/
	public Hashtable getProperties() {
		if (cl == null)
			return null;
		Hashtable prop = cl.getVisProperties();
		if (prop == null) {
			prop = new Hashtable();
		}
		prop.put("class_method", cl.getMethodId());
		prop.put("ranged", String.valueOf(isRanged));
		return prop;
	}

	/**
	* After the plot is constructed, it may be requested to setup its individual
	* properties according to the given list of stored properties.
	* The base Plot class does nothing in this method.
	*/
	@Override
	public void setProperties(Hashtable properties) {
		if (properties != null && cl != null) {
			cl.setVisProperties(properties);
		}
		try {
			setRanged(new Boolean((String) properties.get("ranged")).booleanValue());
		} catch (Exception ex) {
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
		int gap = 2, indent = 20;
		StringBuffer sb = new StringBuffer();
		StringInRectangle str = new StringInRectangle();
		str.setPosition(StringInRectangle.Center);
		if (cl instanceof QualitativeClassifier) {
			sb.append(((QualitativeClassifier) cl).getAttributeName());
		}
		str.setString(sb.toString());
		Dimension lsize = str.countSizes(getGraphics());
		int w = getBounds().width;
		int h = getBounds().height;
		str.setRectSize(w - indent, lsize.width * lsize.height / w * 2);
		lsize = str.countSizes(getGraphics());
		Image li = createImage(w - indent, lsize.height);
		str.draw(li.getGraphics());
		Image img = createImage(w, h + lsize.height + 2 * gap);
//    Image img = createImage(w, h);
		draw(img.getGraphics());
		img.getGraphics().drawImage(li, indent, h + gap, null);
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

class IndexItem implements spade.lib.util.Comparable {
	public int index = 0;
	public long distVal[];

	public IndexItem(long distVal[]) {
		this.distVal = distVal;
	}

	@Override
	public int compareTo(spade.lib.util.Comparable c) {

		if (distVal == null)
			return 0;
		IndexItem n = (IndexItem) c;
		long a = distVal[index];
		long b = n.distVal[n.index];
		if (a < b)
			return 1;
		if (a > b)
			return -1;
		return 0;
	}
}