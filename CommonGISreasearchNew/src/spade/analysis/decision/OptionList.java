package spade.analysis.decision;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.lib.basicwin.CManager;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.ItemPainter;
import spade.lib.basicwin.Metrics;
import spade.lib.basicwin.OwnListDraw;
import spade.lib.lang.Language;
import spade.lib.util.IntArray;
import spade.vis.action.HighlightListener;
import spade.vis.action.Highlighter;
import spade.vis.action.ObjectEvent;
import spade.vis.action.ObjectEventHandler;
import spade.vis.database.DataTreater;

/**
* Keeps and displays a list of options of a decision-making problem and allows to
* reoder and (optionally) classify them.
* Notifies PropertyChangeListeners about changes of classes
*/
public class OptionList implements ItemPainter, DataTreater, HighlightListener, Destroyable, MouseListener, MouseMotionListener {
	static ResourceBundle res = Language.getTextResource("spade.analysis.decision.Res");

	/**
	* A structure representing an option in a decision-making problem. An option
	* is characterised by its index (e.g. in a table), identifier, name, and
	* state: selected or not. The state is used for linking between displays.
	*/
	private class Option {
		public int idx = -1;
		public String id = null, name = null;
		public boolean selected = false;
		/**
		* The position of this option in the initial order of options (before
		* user's editing). May be -1 (not specified).
		*/
		public int initPos = -1;

		public Option(int index, String identifier, String name) {
			idx = index;
			id = identifier;
			this.name = name;
		}

		public Option(int index, String identifier, String name, int initialPosition) {
			this(index, identifier, name);
			initPos = initialPosition;
		}
	}

	/**
	* The list of options to be shown to the user and, possibly, reordered by
	* the user.
	*/
	protected Vector options = null;
	/**
	* Maximum item width
	*/
	protected int maxItemWidth = 0;
	/**
	* The margin needed for the sign showing the state of the option
	*/
	protected int marg = 16;
	/**
	* The width of the color field showing the class of each option
	* when the options are classified
	*/
	protected int colorFW = 15;
	/**
	* The width of the rectangle showing the state of the option: selected or not
	*/
	protected int rectSize = 8;
	/**
	* Background color, background color for the active element,
	* text color, text color for the active element
	*/
	protected Color bkgColor = Color.lightGray, activeBkgColor = Color.blue.darker(), txtColor = Color.black, activeTxtColor = Color.white;
	/**
	* The number of the option that was last pointed with the mouse
	*/
	protected int lastPointed = -1;
	/**
	* The vector of attributes the values of which must be displayed when the
	* mouse points at an item in the option list
	*/
	protected Vector attr = null;
	/**
	* The vector of colors for the attributes the values of which must be
	* displayed when the mouse points at an item in the option list
	*/
	protected Vector attrColors = null;
	/**
	* An option list sends object events that occur in it to an appropriate
	* ObjectEventHandler.
	*/
	protected ObjectEventHandler objEvtHandler = null;
	/**
	* A Highlighter links together all papallel displays of the same objects
	*/
	protected Highlighter highlighter = null;
	/**
	* The identifier of the object collection the options belong to
	*/
	protected String setId = null;
	/**
	* The identifier of the table the options belong to. May be null
	*/
	protected String tableId = null;
	/**
	* The canvas in which the OptionList draws the items of the list
	*/
	protected OwnListDraw listDrawer = null;
	/**
	* Indicates whether the decision type is classification or ranking
	*/
	protected boolean useClasses = false;
	/**
	* The number of options in each class
	*/
	protected IntArray classOptN = null;
	/**
	* Used for registering listeners and notification about changes of
	* classification
	*/
	protected PropertyChangeSupport pcSupport = null;
	/**
	* Indicates "destroyed" state. Initially is false.
	*/
	protected boolean destroyed = false;

	public void setBkgColor(Color color) {
		if (color != null) {
			bkgColor = color;
		}
	}

	public void setTextColor(Color color) {
		if (color != null) {
			txtColor = color;
		}
	}

	/**
	* Sets the ObjectEventHandler that will get object events from this
	* OptionList
	*/
	public void setObjectEventHandler(ObjectEventHandler handler) {
		objEvtHandler = handler;
	}

	/**
	* Sets a reference to a Highlighter links together all papallel displays of
	* the same objects
	*/
	public void setHighlighter(Highlighter hl) {
		highlighter = hl;
		if (highlighter != null) {
			highlighter.addHighlightListener(this);
			Vector selected = highlighter.getSelectedObjects();
			if (selected != null && selected.size() > 0) {
				for (int i = 0; i < getOptionCount(); i++) {
					Option opt = (Option) options.elementAt(i);
					if (selected.contains(opt.id)) {
						opt.selected = true;
					}
				}
			}
		}
	}

	/**
	* Sets the identifier of the object collection the options belong to
	*/
	public void setObjectCollectionId(String id) {
		setId = id;
	}

	/**
	* Sets the identifier of the table the options belong to
	*/
	public void setTableId(String id) {
		tableId = id;
	}

	/**
	* Sets the OwnListDraw (canvas) in which the OptionList draws the items of the
	* list
	*/
	public void setListDrawer(OwnListDraw ld) {
		listDrawer = ld;
		ld.addMouseListener(this);
		ld.addMouseMotionListener(this);
	}

	/**
	* Sets whether the decision type is classification or ranking
	*/
	public void setUseClasses(boolean value) {
		if (useClasses == value)
			return;
		useClasses = value;
		if (getOptionCount() > 0 && getClassCount() == 0) {
			makeDefaultClasses(3);
		}
		if (listDrawer != null && listDrawer.isShowing() && getOptionCount() > 0) {
			Frame win = CManager.getFrame(listDrawer);
			if (win == null) {
				CManager.validateAll(listDrawer);
			} else {
				CManager.invalidateAll(listDrawer);
				Dimension d = win.getSize();
				if (useClasses) {
					win.setSize(d.width + 2 * colorFW, d.height);
				} else {
					win.setSize(d.width - 2 * colorFW, d.height);
				}
				win.validate();
			}
		}
	}

	/**
	* Divides options into the specified number of default classes
	*/
	protected void makeDefaultClasses(int nclasses) {
		if (nclasses < 1) {
			nclasses = 1;
		}
		if (classOptN == null) {
			classOptN = new IntArray(10, 5);
		} else {
			classOptN.removeAllElements();
		}
		if (nclasses < 2) {
			classOptN.addElement(getOptionCount());
		} else {
			int nInClass = getOptionCount() / nclasses;
			if (nInClass == 0) {
				nInClass = 1;
			} else if (nInClass > 3) {
				nInClass = 3;
			}
			int nRest = getOptionCount();
			for (int i = 0; i < nclasses - 1 && nRest > 0; i++) {
				classOptN.addElement(nInClass);
				nRest -= nInClass;
			}
			if (nRest > 0) {
				classOptN.addElement(nRest);
			}
		}
	}

	/**
	* Returns the current number of classes
	*/
	public int getClassCount() {
		if (classOptN == null)
			return 0;
		return classOptN.size();
	}

	/**
	* Changes the current number of classes
	*/
	public void setClassNumber(int nclasses) {
		int oldNC = getClassCount();
		if (oldNC == nclasses)
			return;
		if (nclasses < 1) {
			nclasses = 1;
		}
		if (oldNC == 0) {
			makeDefaultClasses(nclasses);
		} else if (oldNC > nclasses) { //unite classes
			int ntot = 0;
			for (int i = nclasses - 1; i < oldNC; i++) {
				ntot += classOptN.elementAt(i);
			}
			classOptN.setElementAt(ntot, nclasses - 1);
			for (int i = oldNC - 1; i >= nclasses; i--) {
				classOptN.removeElementAt(i);
			}
		} else { //split classes
			if (nclasses > getOptionCount()) {
				nclasses = getOptionCount();
			}
			while (getClassCount() < nclasses) {
				//find the largest class
				int idx = 0, size = classOptN.elementAt(0);
				for (int i = 0; i < getClassCount(); i++)
					if (classOptN.elementAt(i) > size) {
						idx = i;
						size = classOptN.elementAt(i);
					}
				if (classOptN.elementAt(idx) < 2) {
					break;
				}
				int n1 = classOptN.elementAt(idx) / 2, n2 = classOptN.elementAt(idx) - n1;
				classOptN.setElementAt(n1, idx);
				classOptN.insertElementAt(n2, idx + 1);
			}
		}
		if (getClassCount() != oldNC) {
			if (listDrawer != null && listDrawer.isShowing()) {
				listDrawer.repaint();
			}
			notifyClassesChange();
		}
	}

	/**
	* Returns the number of elements in the class with the given index
	*/
	public int getClassElemCount(int classIdx) {
		if (classIdx < 0 || classIdx >= getClassCount())
			return 0;
		return classOptN.elementAt(classIdx);
	}

	/**
	* Sets the number of elements in the specified class to the specified value
	*/
	public void setClassSize(int classIdx, int nElements) {
		if (classIdx < 0 || classIdx >= getClassCount() || getClassCount() < 2)
			return;
		if (nElements < 0) {
			nElements = 0;
		}
		int diff = classOptN.elementAt(classIdx) - nElements;
		if (diff == 0)
			return;
		int idx1 = (classIdx < getClassCount() - 1) ? classIdx + 1 : classIdx - 1;
		if (diff >= 0) { //the class is to be reduced
			classOptN.setElementAt(classOptN.elementAt(idx1) + diff, idx1);
			classOptN.setElementAt(nElements, classIdx);
		} else { //the class is to be extended
			diff = -diff;
			if (diff > classOptN.elementAt(idx1) - 1) {
				diff = classOptN.elementAt(idx1) - 1;
			}
			classOptN.setElementAt(classOptN.elementAt(idx1) - diff, idx1);
			classOptN.setElementAt(classOptN.elementAt(classIdx) + diff, classIdx);
		}
		removeEmptyClasses();
		if (listDrawer != null && listDrawer.isShowing()) {
			listDrawer.repaint();
		}
		notifyClassesChange();
	}

	/**
	* Removes classes that contain no elements. Returnes "true" if any class
	* was removed
	*/
	protected boolean removeEmptyClasses() {
		boolean changed = false;
		if (classOptN != null) {
			for (int i = classOptN.size() - 1; i >= 0; i--)
				if (classOptN.elementAt(i) < 1) {
					classOptN.removeElementAt(i);
					changed = true;
				}
		}
		return changed;
	}

	/**
	* Returns the class of the option with the given number
	*/
	protected int getOptionClass(int optIdx) {
		if (getClassCount() < 2)
			return 0;
		for (int i = 0; i < getClassCount() - 1; i++) {
			optIdx -= getClassElemCount(i);
			if (optIdx < 0)
				return i;
		}
		return getClassCount() - 1;
	}

	/**
	* Adds an option to the option list. An option is characterised by its index
	* (e.g. in a table), identifier, name, and an integer value of its initial
	* position in the ordered list of options (that can be edited by the user).
	*/
	public void addOption(int index, String id, String name, int initialPosition) {
		Option opt = new Option(index, id, name, initialPosition);
		boolean inserted = false;
		if (initialPosition >= 0 && options != null && options.size() > 0) {
			for (int i = 0; i < options.size() && !inserted; i++) {
				Option opt1 = (Option) options.elementAt(i);
				if (initialPosition < opt1.initPos) {
					options.insertElementAt(opt, i);
					inserted = true;
				}
			}
		}
		if (!inserted) {
			if (options == null) {
				options = new Vector(50, 10);
			}
			options.addElement(opt);
		}
		if (useClasses) { //"initialPosition" is the class number
			if (classOptN == null) {
				classOptN = new IntArray(10, 5);
			}
			int clN = initialPosition - 1;
			while (classOptN.size() <= clN) {
				classOptN.addElement(0);
			}
			classOptN.setElementAt(classOptN.elementAt(clN) + 1, clN);
		}
	}

	public int getOptionCount() {
		if (options == null)
			return 0;
		return options.size();
	}

	/**
	* Moves the option with the specified index one position up the list
	*/
	public void optionUp(int idx) {
		if (idx < 1 || idx >= getOptionCount())
			return;
		Object opt = options.elementAt(idx);
		options.removeElementAt(idx);
		options.insertElementAt(opt, idx - 1);
	}

	/**
	* Moves the option with the specified index one position down the list
	*/
	public void optionDown(int idx) {
		if (idx < 0 || idx + 1 >= getOptionCount())
			return;
		Object opt = options.elementAt(idx);
		options.removeElementAt(idx);
		options.insertElementAt(opt, idx + 1);
	}

	@Override
	public int itemH() {
		if (Metrics.fh > 0)
			return Metrics.fh + 4;
		return 20;
	}

	@Override
	public int maxItemW() {
		if (maxItemWidth > 0)
			return maxItemWidth + marg + 4 + ((useClasses) ? 2 * colorFW : 0);
		if (Metrics.getFontMetrics() == null)
			return 0;
		for (int i = 0; i < getOptionCount(); i++) {
			Option opt = (Option) options.elementAt(i);
			String name = opt.name;
			if (name == null) {
				name = opt.id;
			}
			if (name == null) {
				name = String.valueOf(i + 1);
			}
			int w = Metrics.stringWidth(name);
			if (maxItemWidth < w) {
				maxItemWidth = w;
			}
		}
		if (maxItemWidth > 0)
			return maxItemWidth + marg + 4 + ((useClasses) ? 2 * colorFW : 0);
		return 0;
	}

	/**
	* Generates a color for the class with the given index. The first class
	* is the best and is shown by green, the last class is the worst and is
	* shown by red.
	*/
	public Color getClassColor(int idx) {
		int nc = getClassCount();
		if (idx < 0 || idx >= nc)
			return bkgColor;
		if (idx == 0)
			return Color.green.darker();
		if (idx == nc - 1)
			return Color.red.darker();
		//return a color between red and green
		return Color.getHSBColor((0.33f * (nc - idx - 1)) / (nc - 1), 1.0f, 1.0f);
	}

	private boolean firstDraw = true;

	@Override
	public void drawItem(Graphics g, int n, int x, int y, int w, boolean isActive) {
		if (firstDraw && listDrawer != null) {
			if (useClasses) {
				removeEmptyClasses();
			}
			bkgColor = listDrawer.getBackground();
			txtColor = listDrawer.getForeground();
			firstDraw = false;
		}
		int h = itemH();
		if (useClasses) {
			g.setColor(getClassColor(getOptionClass(n)));
			g.fillRect(x, y, colorFW + 1, h + 1);
			g.fillRect(x + w - colorFW, y, colorFW, h);
			x += colorFW;
			w -= 2 * colorFW;
		}
		Option opt = (Option) options.elementAt(n);
		String name = opt.name;
		if (name == null) {
			name = opt.id;
		}
		if (name == null) {
			name = String.valueOf(n + 1);
		}
		g.setColor((isActive) ? activeBkgColor : bkgColor);
		g.fillRect(x, y, w + 1, h + 1);
		g.setColor((isActive) ? activeTxtColor : txtColor);
		g.drawString(name, x + marg, y + (h - Metrics.fh) / 2 + Metrics.asc);
		int dw = (marg - rectSize) / 2, dh = (h - rectSize) / 2;
		g.drawOval(x + dw, y + dh, rectSize, rectSize);
		if (opt.selected) {
			g.drawRect(x + dw + 1, y + dh + 1, rectSize - 2, rectSize - 2);
			g.drawRect(x + dw + 2, y + dh + 2, rectSize - 4, rectSize - 4);
		}
	}

	@Override
	public void drawEmptyList(Graphics g, int x, int y, int w, int h) {
		String msg = res.getString("No_options_available_");
		int mw = Metrics.stringWidth(msg);
		g.drawString(msg, x + (w - mw) / 2, y + (h - Metrics.fh) / 2 + Metrics.asc);
	}

	/**
	* Makes the object event handler dehighlight the object that was previously
	* highlighted
	*/
	protected void dehighlight() {
		if (objEvtHandler == null || attr == null)
			return;
		if (lastPointed >= 0) {
			ObjectEvent oevt = new ObjectEvent(this, ObjectEvent.point, null, setId);
			objEvtHandler.processObjectEvent(oevt); //to dehighlight
		}
		lastPointed = -1;
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		int x = e.getX(), m = marg;
		if (useClasses) { //possibly, the user wishes to split a class into two
			if (moveBreakAfterClass >= 0)
				return;
			if (x <= colorFW || x >= colorFW + maxItemWidth + marg) {
				int n = e.getY() / itemH() - 1;
				int ncl = getOptionClass(n), nOptCurr = classOptN.elementAt(ncl);
				//how many options are in previous classes?
				int nOptPrev = 0;
				for (int i = 0; i < ncl; i++) {
					nOptPrev += classOptN.elementAt(i);
				}
				n = n - nOptPrev + 1;
				if (n == 0 || n == nOptCurr)
					return;
				//split the class
				classOptN.insertElementAt(n, ncl);
				classOptN.setElementAt(nOptCurr - n, ncl + 1);
				listDrawer.repaint();
				notifyClassesChange();
				return;
			}
			m += colorFW;
		}
		restoreCursor();
		if (objEvtHandler == null || attr == null)
			return;
		if (x > m - (marg - rectSize) / 2)
			return; //does not fit in "checkbox"
		int n = e.getY() / itemH();
		Option opt = (Option) options.elementAt(n);
		if (opt.id == null)
			return;
		ObjectEvent oevt = new ObjectEvent(this, ObjectEvent.click, e, setId);
		oevt.addEventAffectedObject(opt.id);
		objEvtHandler.processObjectEvent(oevt);
	}

	@Override
	public void mousePressed(MouseEvent e) {
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		drag = false;
		if (removeEmptyClasses()) {
			listDrawer.repaint();
			notifyClassesChange();
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		if (drag)
			return;
		lastPointed = -1;
	}

	@Override
	public void mouseExited(MouseEvent e) {
		if (drag)
			return;
		restoreCursor();
		dehighlight();
	}

	private boolean drag = false;
	private int moveBreakAfterClass = -1;

	/**
	* Moving of a class boundary
	*/
	@Override
	public void mouseDragged(MouseEvent e) {
		if (!drag) {
			if (moveBreakAfterClass < 0)
				return;
			drag = true;
		}
		int nobj = 0, clN = moveBreakAfterClass;
		for (int i = 0; i <= clN; i++) {
			nobj += getClassElemCount(i);
		}
		int n = e.getY() / itemH() + 1;
		if (n >= nobj && n <= nobj + 1)
			return; //nothing changed;
		if (n < nobj && getClassElemCount(clN) < 1)
			return; //current class is already empty
		if (n > nobj && getClassElemCount(clN + 1) < 1)
			return; //next class is already empty
		if (listDrawer != null) {
			listDrawer.makeVisible((n < nobj) ? n - 2 : n);
		}
		if (n < nobj) {
			classOptN.setElementAt(classOptN.elementAt(clN) - 1, clN);
			classOptN.setElementAt(classOptN.elementAt(clN + 1) + 1, clN + 1);
		} else {
			classOptN.setElementAt(classOptN.elementAt(clN) + 1, clN);
			classOptN.setElementAt(classOptN.elementAt(clN + 1) - 1, clN + 1);
		}
		listDrawer.repaintItem(nobj - 1);
		listDrawer.repaintItem(nobj);
		notifyClassesChange();
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		if (objEvtHandler == null || attr == null)
			return;
		int n = e.getY() / itemH();
		if (useClasses && (e.getX() <= colorFW || e.getX() >= colorFW + maxItemWidth + marg)) {
			dehighlight();
			//check if the mouse is on the class border
			int mod = e.getY() - (n * itemH());
			if (mod < itemH() / 3) {
				--n;
			} else if (mod < itemH() * 2 / 3) {
				restoreCursor();
				return;
			}
			if (n < 0) {
				restoreCursor();
				return;
			}
			int cln = getOptionClass(n);
			if (cln < getClassCount() - 1 && getOptionClass(n + 1) > cln) {
				moveBreakAfterClass = cln;
				setMoveCursor();
			} else {
				restoreCursor();
			}
			return;
		}
		restoreCursor();
		if (n == lastPointed)
			return;
		if (n < 0 || n >= getOptionCount()) {
			dehighlight();
			return;
		}
		lastPointed = n;
		Option opt = (Option) options.elementAt(n);
		if (opt.id == null)
			return;
		ObjectEvent oevt = new ObjectEvent(this, ObjectEvent.point, e, setId);
		oevt.addEventAffectedObject(opt.id);
		objEvtHandler.processObjectEvent(oevt);
	}

	private Cursor oldCursor = null;

	/**
	* Changes the cursor to vertical movement ("resize") cursor
	*/
	protected void setMoveCursor() {
		if (listDrawer == null || oldCursor != null)
			return;
		oldCursor = listDrawer.getCursor();
		listDrawer.setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
	}

	/**
	* Restores the old cursor of the listDrawer
	*/
	protected void restoreCursor() {
		moveBreakAfterClass = -1;
		if (listDrawer == null || oldCursor == null)
			return;
		listDrawer.setCursor(oldCursor);
		oldCursor = null;
	}

	/**
	* Sets the vector of attributes the values of which must be displayed when the
	* mouse points at an item in the option list
	*/
	public void setAttributeList(Vector attrList) {
		attr = attrList;
	}

	/**
	* Implementation of the DataTreater interface.
	* Returns a vector of IDs of the attributes this Data Treater deals with
	*/
	@Override
	public Vector getAttributeList() {
		return attr;
	}

	/**
	* Sets the vector of colors for the attributes the values of which must be
	* displayed when the mouse points at an item in the option list
	*/
	public void setAttributeColors(Vector colors) {
		attrColors = colors;
	}

	/**
	* A method from the DataTreater interface.
	* Must return a vector of colors used for representation of the attributes
	* this Data Treater deals with.
	*/
	@Override
	public Vector getAttributeColors() {
		return attrColors;
	}

	/**
	* A method from the DataTreater interface.
	* Must replies whether it is linked to the data set (table) with the given
	* identifier.
	*/
	@Override
	public boolean isLinkedToDataSet(String setId) {
		return tableId != null && tableId.equals(setId);
	}

	/**
	* Notification about change of the set of objects to be transiently
	* highlighted.
	*/
	@Override
	public void highlightSetChanged(Object source, String setId, Vector highlighted) {
	}

	/**
	* Notification about change of the set of objects to be selected (durably
	* highlighted).
	*/
	@Override
	public void selectSetChanged(Object source, String setId, Vector selected) {
		boolean changed = false;
		if (this.setId != null && this.setId.equals(setId)) {
			for (int i = 0; i < getOptionCount(); i++) {
				Option opt = (Option) options.elementAt(i);
				if (opt.selected)
					if (selected == null || !selected.contains(opt.id)) {
						opt.selected = false;
						changed = true;
					} else {
						;
					}
				else if (selected != null && selected.contains(opt.id)) {
					opt.selected = true;
					changed = true;
				}
			}
		}
		if (changed && listDrawer != null) {
			listDrawer.paint(listDrawer.getGraphics());
		}
	}

	/**
	* Returns the result of ordering the options: an array in which for each
	* option (according to its index in the table) its order is given (an
	* integer number starting from 1)
	*/
	public int[] getOptionsOrder() {
		if (getOptionCount() < 1)
			return null;
		int order[] = new int[getOptionCount()];
		for (int i = 0; i < getOptionCount(); i++) {
			Option opt = (Option) options.elementAt(i);
			order[opt.idx] = i + 1;
		}
		return order;
	}

	/**
	* Returns the result of classification of the options: an array in which for
	* each option (according to its index in the table) its class is given (an
	* integer number starting from 1)
	*/
	public int[] getOptionsClasses() {
		if (getOptionCount() < 1)
			return null;
		int classes[] = new int[getOptionCount()];
		for (int i = 0; i < getOptionCount(); i++) {
			Option opt = (Option) options.elementAt(i);
			classes[opt.idx] = getOptionClass(i) + 1;
		}
		return classes;
	}

	/**
	* Removes itself from listeners of the highlighter
	*/
	@Override
	public void destroy() {
		if (highlighter != null) {
			highlighter.removeHighlightListener(this);
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

	/**
	* Registers a listener of changes of classification
	*/
	public void addPropertyChangeListener(PropertyChangeListener pcl) {
		if (pcl == null)
			return;
		if (pcSupport == null) {
			pcSupport = new PropertyChangeSupport(this);
		}
		pcSupport.addPropertyChangeListener(pcl);
	}

	/**
	* Notifies the listeners (if any) about changes of classes
	*/
	protected void notifyClassesChange() {
		if (pcSupport != null) {
			pcSupport.firePropertyChange("classes", null, null);
		}
	}
}