package spade.vis.dataview;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Panel;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Scrollbar;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.classification.Classifier;
import spade.analysis.classification.TableClassifier;
import spade.analysis.system.Supervisor;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.HotSpot;
import spade.lib.basicwin.Icons;
import spade.lib.basicwin.Metrics;
import spade.lib.basicwin.PopupManager;
import spade.lib.lang.Language;
import spade.lib.util.Comparable;
import spade.lib.util.IntArray;
import spade.lib.util.NumRange;
import spade.lib.util.QSortAlgorithm;
import spade.lib.util.StringUtil;
import spade.vis.action.HighlightListener;
import spade.vis.action.Highlighter;
import spade.vis.action.ObjectEvent;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.ObjectFilter;
import spade.vis.event.EventSource;

//helper class for quick sorting
class TableValue implements Comparable {
	int index;
	double numValue = Double.NaN;
	Object objValue = null;

	public TableValue(double val, int ind) {
		this.numValue = val;
		this.index = ind;
	}

	public TableValue(Object val, int ind) {
		this.objValue = val;
		this.index = ind;
	}

	/**
	*  Returns 0 if equal, <0 if THIS is less than the argument, >0 otherwise
	*/
	@Override
	public int compareTo(Comparable c) {
		if (c == null || !(c instanceof TableValue))
			return -1;
		TableValue tv = (TableValue) c;
		if (index == tv.index)
			return 0;
		if (Double.isNaN(numValue) && objValue == null)
			if (Double.isNaN(tv.numValue) && tv.objValue == null)
				if (index < tv.index)
					return -1;
				else
					return 1;
			else
				return 1;
		int result = 0;
		if (!Double.isNaN(numValue)) {
			if (!Double.isNaN(tv.numValue))
				if (numValue < tv.numValue) {
					result = -1;
				} else if (numValue > tv.numValue) {
					result = 1;
				} else {
					result = 0;
				}
			else {
				result = -1;
			}
		} else {
			if (tv.objValue == null)
				return -1;
			if (objValue instanceof String) {
				if (!(tv.objValue instanceof String))
					return -1;
				String str1 = (String) objValue, str2 = (String) tv.objValue;
				result = str1.compareTo(str2);
			} else if (objValue instanceof Comparable) {
				if (!(tv.objValue instanceof Comparable))
					return -1;
				Comparable v1 = (Comparable) objValue, v2 = (Comparable) tv.objValue;
				result = v1.compareTo(v2);
			}
		}
		if (result == 0)
			if (index < tv.index) {
				result = -1;
			} else {
				result = 1;
			}
		return result;
	}
}

//Visual table
public class TableViewer extends Panel implements AdjustmentListener, ComponentListener, ActionListener, MouseListener, MouseMotionListener, Destroyable, HighlightListener, PropertyChangeListener, EventSource {
	static ResourceBundle res = Language.getTextResource("spade.vis.dataview.Res");

	protected int col0widthmin; //minimum width of first column
	protected int row0heightmin; //minimum height of first row
	static int colwidthmin = 20; //minimum columns width
	static int rowheightmin = 1; //minimum row height (in the condensed view)
	static int colwidthmax = Metrics.mm() * 30; //maximum columns width (except first column) /������������ ������ ������� �������(�������� ������)/
	static int uphor = 3; //horizontal indent of data in cell /�������������� ������ � ������ ��� ������ ������/
	static int upstr = 15; //Distance between lines when they print in the same cell /���������� ����� �������� ��� ������ �� � ����� ������/
	protected Scrollbar vScroll; //Vertical scrollbar
	protected Scrollbar hScroll; //Horizontal scrollbar
	protected Image im;
	protected Graphics gr;
	protected FontMetrics fm;
	protected Font font;
	protected AttributeDataPortion dtab; //Table

	protected IntArray attrBasedOrder = null; //ordering of rows according to the selected attribute
	protected IntArray classBasedOrder = null; //ordering of rows according to the classification (if exists)
	protected IntArray queryNumbers = null; //index numbers of initial table satisfying to query
	/**
	* Identifier of the current sorting attribute in the table.
	* No sorting: sortattr==null
	* Sorting by Name: sortattr=="_NAME_"
	*/
	protected String sortattr = null;
	protected boolean descending = false; //is there descending sorting
	protected boolean tablelens = false; //is table lens active
	/**
	* Indicates whether "condensed view" mode is on or off (valid only in
	* combination with the "table lens" mode)
	*/
	protected boolean condensed = false;
	protected IntArray visibleAttr = null; //Showing attributes
	/**
	* Indicates whether object identifiers need to be shown
	*/
	protected boolean showIds = false;
	/**
	* Indicates whether table rows need to be grouped by classes (if exist)
	*/
	protected boolean groupByClasses = true;
	/**
	* Indicates whether the table was destroyed
	*/
	protected boolean destroyed = false;

	/**
	* Supervisor provides access to the Highlighter (common for
	* all data displays) and in this way links together all displays
	*/
	protected Supervisor supervisor = null;
	/**
	* ObjectFilter contains results of data querying. Only data satisfying the
	* current query (if any) are displayed
	*/
	protected ObjectFilter tf = null;
	/**
	* Array of numbers of currently selected records
	*/
	protected IntArray selNumbers = null;
	/**
	* "Hot spot" for switching on and off drawing of the identifiers
	*/
	protected HotSpot showIdsHSp = null;
	/**
	* Used for showing "extended" contents of cells
	*/
	protected PopupManager cellPopM = null;
	/**
	* Indicates whether object identifiers are different from their names.
	* Only in this case the "hot spot" for switching on and off the identifiers
	* appears in the table view.
	*/
	protected boolean namesDifferFromIds = false;
	/**
	 * Indicates that the names of the table items (records) should be treated
	 * as numbers rather than strings
	 */
	protected boolean treatItemNamesAsNumbers = false;
	/**
	* The size of the "checkbox" icon (depends on the font height)
	*/
	protected int cbSize = 0;
	/**
	* current width of the table
	*/
	protected int tablewidth = 0;
	/**
	* current height of the table
	*/
	protected int tableheight = 0;
	/**
	* current width of 1-st column in normal and "condensed" mode
	*/
	protected int col0width = 0, cCol0Width = 0;
	/**
	* current height of 1-st row in normal and "condensed" mode
	*/
	protected int row0height = 0, cRow0Height = 0;
	/**
	* current width of columns (except first column) in normal and "condensed" mode
	*/
	protected int colwidth = 0, cColWidth = 0;
	/**
	* current row height (except first row) in normal and "condensed" mode
	*/
	protected int rowheight = 20, cRowHeight = 0;
	/**
	* The owner of the table view, which will receive messages about changes
	* of the attribute used for sorting table rows.
	*/
	protected PropertyChangeListener owner = null;
	/**
	* Indicates whether some classes are currently represented on the graph
	*/
	protected boolean hasClasses = false;
	/**
	 * The colors of the data items (table rows) may be preset. This array,
	 * which may be null, contains the colors.
	 */
	protected Color itemColors[] = null;

	public String getVisibleAttrId(int idx) {
		if (idx < 0 || visibleAttr == null || idx >= visibleAttr.size())
			return null;
		return dtab.getAttributeId(visibleAttr.elementAt(idx));
	}

	public void setSortAttr(String attrId) {
		if (StringUtil.sameStrings(sortattr, attrId))
			return;
		sortattr = attrId;
		defineAttrBasedOrder();
		defineClassBasedOrder();
		setDataAccordingQuery();
	}

	public void setSortAttr(int attrIdx) {
		if (attrIdx < -1) {
			setSortAttr(null);
		} else if (attrIdx == -1) {
			setSortAttr("_NAME_");
		} else {
			setSortAttr(getVisibleAttrId(attrIdx));
		}
	}

	public String getSortAttr() {
		return sortattr;
	}

	public int getSortAttrIdx() {
		if (sortattr == null)
			return -2;
		if (sortattr.equals("_NAME_"))
			return -1;
		if (visibleAttr == null)
			return -2;
		int idx = dtab.getAttrIndex(sortattr);
		if (idx < 0)
			return -2;
		return visibleAttr.indexOf(idx);
	}

	public void setDescending(boolean descending) {
		if (this.descending != descending) {
			this.descending = descending;
			defineClassBasedOrder();
			setDataAccordingQuery();
		}
	}

	public boolean getDescending() {
		return descending;
	}

	public void setTableLens(boolean tableLens) {
		this.tablelens = tableLens;
		if (!tablelens) {
			condensed = false;
		}
	}

	public void setCondensedMode(boolean value) {
		condensed = value;
	}

	public int getHScrollValue() {
		if (hScroll == null || !hScroll.isEnabled())
			return 0;
		return hScroll.getValue();
	}

	public int getVScrollValue() {
		if (vScroll == null || !vScroll.isEnabled())
			return 0;
		return vScroll.getValue();
	}

	public Scrollbar getHScroll() {
		return hScroll;
	}

	public IntArray getVisibleAttrNs() {
		return visibleAttr;
	}

	public void setVisibleAttributes(Vector visAttr) {
		if (visAttr == null || visAttr.size() < 1)
			return;
		if (visibleAttr == null) {
			visibleAttr = new IntArray(dtab.getAttrCount(), 10);
		} else {
			visibleAttr.removeAllElements();
		}
		for (int i = 0; i < visAttr.size(); i++) {
			visibleAttr.addElement(dtab.getAttrIndex((String) visAttr.elementAt(i)));
		}
		reset();
		cellSizeCalculation();
	}

	public TableViewer(AttributeDataPortion dt, Supervisor sup, PropertyChangeListener owner) {
		if (dt == null)
			return;
		dtab = dt;
		dtab.addPropertyChangeListener(this);
		tf = dt.getObjectFilter();
		if (tf != null) {
			tf.addPropertyChangeListener(this);
		}
		this.owner = owner;
		supervisor = sup;
		if (supervisor != null) {
			supervisor.addPropertyChangeListener(this);
			supervisor.registerObjectEventSource(this);
			supervisor.registerHighlightListener(this, dtab.getEntitySetIdentifier());
		}
		checkWhatSelected();
		namesDifferFromIds = false;
		for (int i = 0; i < dtab.getDataItemCount() && !namesDifferFromIds; i++) {
			String str = dtab.getDataItemName(i);
			namesDifferFromIds = str == null || str.length() < 1 || !str.equals(dtab.getDataItemId(i));
		}

		setLayout(null);
		addComponentListener(this);
		addMouseListener(this);
		vScroll = new Scrollbar(Scrollbar.VERTICAL);
		add(vScroll);
		hScroll = new Scrollbar(Scrollbar.HORIZONTAL);
		add(hScroll);
		vScroll.addAdjustmentListener(this);
		hScroll.addAdjustmentListener(this);
	}

	/**
	 * Sets whether the names of the table items (records) should be treated
	 * as numbers rather than strings
	 */
	public void setTreatItemNamesAsNumbers(boolean value) {
		treatItemNamesAsNumbers = value;
	}

	public void setShowIdentifiers(boolean show) {
		if (showIds != show) {
			showIds = show;
			if (sortattr != null && sortattr.equals("_NAME_")) {
				defineAttrBasedOrder();
				defineClassBasedOrder();
				setDataAccordingQuery();
			}
		}
	}

	public void setGroupByClasses(boolean group) {
		if (groupByClasses != group) {
			groupByClasses = group;
			defineClassBasedOrder();
			setDataAccordingQuery();
		}
	}

	public boolean getGroupByClasses() {
		return groupByClasses;
	}

	/**
	* When a TableView is constructed, it gets from the Highlighter the list
	* of currently selected objects in order to show them in "highlighted" mode.
	*/
	protected void checkWhatSelected() {
		if (supervisor == null || dtab == null)
			return;
		Highlighter highlighter = supervisor.getHighlighter(dtab.getEntitySetIdentifier());
		if (highlighter == null)
			return;
		if (selNumbers == null) {
			selNumbers = new IntArray(20, 10);
		} else {
			selNumbers.removeAllElements();
		}
		Vector selIds = highlighter.getSelectedObjects();
		if (selIds == null || selIds.size() < 1)
			return;
		for (int i = 0; i < dtab.getDataItemCount() && selNumbers.size() < selIds.size(); i++)
			if (selIds.contains(dtab.getDataItemId(i))) {
				selNumbers.addElement(i);
			}
	}

	/**
	* Checks if the record with the given number is currently selected, i.e.
	* should be drawn in "highlighted" mode
	*/
	protected boolean isRecordSelected(int recN) {
		return selNumbers != null && selNumbers.indexOf(recN) >= 0;
	}

	/**
	 * Returns the numbers of currently selected records
	 */
	public IntArray getSelNumbers() {
		return selNumbers;
	}

	/**
	 * Returns the numbers of records corresponding to the objects with the given
	 * identifiers
	 */
	public IntArray getRecordNumbers(Vector objIds) {
		if (objIds == null || objIds.size() < 1)
			return null;
		IntArray numbers = new IntArray(objIds.size(), 10);
		for (int i = 0; i < dtab.getDataItemCount() && numbers.size() < objIds.size(); i++)
			if (objIds.contains(dtab.getDataItemId(i))) {
				numbers.addElement(i);
			}
		return numbers;
	}

	/**
	 * Sets the numbers of currently selected records. Returns true if the
	 * current selection has changed.
	 */
	public boolean setSelNumbers(IntArray numbers) {
		if (selNumbers == null)
			if (numbers == null)
				return false;
			else {
				selNumbers = numbers;
			}
		else if (numbers == null || numbers.size() < 1) {
			selNumbers.removeAllElements();
		} else if (selNumbers.contains(numbers) && numbers.contains(selNumbers))
			return false;
		else {
			selNumbers.removeAllElements();
			for (int i = 0; i < numbers.size(); i++) {
				selNumbers.addElement(numbers.elementAt(i));
			}
		}
		if (isShowing()) {
			tablerepaint();
			update(gr);
			repaint();
		}
		return true;
	}

	/**
	* Checks if the table record with the specified number satisfies to the query
	* (table filter)
	*/
	public boolean satisfiesToQuery(int recN) {
		if (tf == null)
			return true;
		return tf.isActive(recN);
	}

	@Override
	public void paint(Graphics g) {
		if (im == null) {
			initializationGraphicsObjects();
			cellSizeCalculation();
			tablerepaint();
		}
		if (im != null) {
			g.drawImage(im, 0, 0, this);
		}
	}

	@Override
	public void update(Graphics g) {
		paint(g);
	}

// Adjustment cell size accordingly current data /�������� �������� ����� ������� � ������������ � �������� �������/
	private void cellSizeCalculation() {
		if (gr == null)
			return;
		col0widthmin = 0;
		colwidth = 0;
		row0heightmin = 0;

		String s;
		int nprev;
		int nnext;
		int dx;
		int dy;
		int maxwordlength = 0;
		boolean ent;
		//Adjustment width of 0-th column by widest record in it /��������� ������ ������� ������� �� ����� ������� ������ � ���/
		setHighlightedFont();
		FontMetrics highlightedFM = gr.getFontMetrics();
		rowheight = highlightedFM.getHeight();
		for (int i = 0; i < dtab.getDataItemCount(); i++) {
			String name = dtab.getDataItemName(i);
			if (name == null) {
				name = dtab.getDataItemId(i);
			} else if (showIds) {
				name = dtab.getDataItemId(i) + "   " + name;
			}
			if (col0widthmin < highlightedFM.stringWidth(name)) {
				col0widthmin = highlightedFM.stringWidth(name);
			}
		}
		//Adjustment width of columns(except first)
		if (visibleAttr != null) {
			for (int i = 0; i < visibleAttr.size(); i++) {
				dx = visibleAttr.elementAt(i);
				for (int j = 0; j < dtab.getDataItemCount(); j++) {
					String value = dtab.getAttrValueAsString(dx, j);
					if (value != null) {
						int w = fm.stringWidth(value);
						if (colwidth < w)
							if (dtab.isAttributeNumeric(dx) || dtab.isAttributeTemporal(dx)) {
								colwidth = w;
							} else {
								colwidth = Math.min(w, colwidthmax);
							}
					}
				}
			}
		}
		colwidth = colwidth + 2 * uphor;
		if (colwidth < colwidthmin) {
			colwidth = colwidthmin;
		}
		setPlainFont();
		//Increase of columns width (except 1-th) to width of widest word in table
		//titles if necessary
		if (visibleAttr != null) {
			for (int i = 0; i < visibleAttr.size(); i++) {
				dx = visibleAttr.elementAt(i);
				if (dtab.getAttributeName(dx) != null) {
					s = dtab.getAttributeName(dx);
				} else {
					s = dtab.getAttributeId(dx);
				}
				nnext = -1;
				dx = 0;
				dy = 0;
				do {
					nprev = nnext;
					nnext = s.indexOf(0x020, nprev + 1);
					if (nnext == -1) {
						if (maxwordlength < fm.stringWidth(s.substring(nprev + 1))) {
							maxwordlength = fm.stringWidth(s.substring(nprev + 1));
						}
					} else {
						if (maxwordlength < fm.stringWidth(s.substring(nprev + 1, nnext))) {
							maxwordlength = fm.stringWidth(s.substring(nprev + 1, nnext));
						}
					}
				} while (nnext != -1);
			}
		}
		if (colwidth < maxwordlength + 2 * uphor) {
			colwidth = maxwordlength + 2 * uphor;
			if (colwidth > colwidthmax) {
				colwidth = colwidthmax;
			}
		}
		//Calculation required height of upper table row
		upstr = fm.getHeight() + 1;
		if (visibleAttr != null) {
			for (int i = 0; i < visibleAttr.size(); i++) {
				dx = visibleAttr.elementAt(i);
				if (dtab.getAttributeName(dx) != null) {
					s = dtab.getAttributeName(dx);
				} else {
					s = dtab.getAttributeId(dx);
				}
				nnext = -1;
				dx = 0;
				dy = 0;
				do {
					nprev = nnext;
					nnext = s.indexOf(0x020, nprev + 1);
					if (nnext == -1) {
						ent = (colwidth - 2 * uphor < dx + fm.stringWidth(s.substring(nprev + 1)));
					} else {
						ent = (colwidth - 2 * uphor < dx + fm.stringWidth(s.substring(nprev + 1, nnext)));
					}
					if ((dx > 0) && ent) {
						dx = 0;
						dy = dy + upstr;
					}
					if (nnext != -1) {
						dx = dx + fm.stringWidth(s.substring(nprev + 1, nnext) + " ");
					}
				} while (nnext != -1);
				if (row0heightmin < dy) {
					row0heightmin = dy;
				}
			}
		}
		row0heightmin = row0heightmin + upstr;
		cRow0Height = 0;
		cRowHeight = 0;
		cCol0Width = 0;
		cColWidth = 0;
		tablewidth = tableheight = 0;
	}

	/**
	* Calculates cell sizes for a "condensed" view
	*/
	private void condensedSizesCalculation() {
		if (gr == null)
			return;
		Dimension d = getSize();
		if (d == null || d.width < 10 || d.height < 10)
			return;
		if (tablewidth < 10 || tableheight < 10) {
			determineTableSizes();
		}
		if (tablewidth < 10 || tableheight < 10)
			return;
		cRow0Height = row0height;
		cRowHeight = rowheight;
		cCol0Width = col0width;
		cColWidth = colwidth;
		int nActiveRows = queryNumbers.size();
		if (nActiveRows < 1)
			return;
		//determine the row height for the maximum number of table rows to be visible
		int nVisRows = (tableheight - row0height) / rowheight;
		while (nVisRows < nActiveRows && cRow0Height - rowheight >= rowheight) {
			cRow0Height -= rowheight;
			++nVisRows;
		}
		if (nVisRows < nActiveRows) {
			cRow0Height = rowheight;
			cRowHeight = (tableheight - cRow0Height) / nActiveRows;
			if (cRowHeight < rowheightmin) {
				cRowHeight = rowheightmin;
			}
		}
		nVisRows = (tableheight - cRow0Height) / cRowHeight;
		if (nVisRows > nActiveRows) {
			nVisRows = nActiveRows;
		}
		cRowHeight = (tableheight - cRow0Height) / nVisRows;
		cRow0Height = Math.max(tableheight - nVisRows * cRowHeight, rowheight);
		FontMetrics fm = gr.getFontMetrics();
		boolean lowRows = cRowHeight < fm.getHeight();
		if (lowRows) {
			cCol0Width = colwidthmin;
		}
		//determine the column width for the maximum number of columns to be visible
		int nCols = (visibleAttr == null) ? 1 : visibleAttr.size();
		int nVisCols = (tablewidth - cCol0Width) / colwidth;
		if (nVisCols < nCols && cCol0Width > col0widthmin) {
			cCol0Width = col0widthmin;
		}
		cColWidth = (tablewidth - cCol0Width) / nCols;
		if (cColWidth < colwidthmin * 3 && !lowRows) {
			cCol0Width = colwidthmin;
			cColWidth = (tablewidth - cCol0Width) / nCols;
		}
		if (cColWidth < colwidthmin) {
			cColWidth = colwidthmin;
		}
		nVisCols = (tablewidth - cCol0Width) / cColWidth;
		if (nVisCols > nCols) {
			nVisCols = nCols;
		}
		cColWidth = (tablewidth - cCol0Width) / nVisCols;
		cCol0Width = tablewidth - nVisCols * cColWidth;
	}

	protected void defineAttrBasedOrder() {
		if (sortattr == null || dtab.getDataItemCount() < 2) {
			attrBasedOrder = null;
			return;
		}
		int idx = -1;
		if (!sortattr.equals("_NAME_")) {
			idx = dtab.getAttrIndex(sortattr);
			if (idx < 0) {
				sortattr = null;
				attrBasedOrder = null;
				return;
			}
		}
		TableValue values[] = new TableValue[dtab.getDataItemCount()];
		if (idx < 0)
			if (showIds) {
				for (int i = 0; i < dtab.getDataItemCount(); i++) {
					values[i] = new TableValue(dtab.getDataItemId(i), i);
				}
			} else if (treatItemNamesAsNumbers) {
				for (int i = 0; i < dtab.getDataItemCount(); i++) {
					String str = dtab.getDataItemName(i);
					double f = Double.NaN;
					try {
						f = Double.valueOf(str).floatValue();
					} catch (NumberFormatException e) {
					}
					if (!Double.isNaN(f)) {
						values[i] = new TableValue(f, i);
					} else {
						values[i] = new TableValue(str, i);
					}
				}
			} else {
				for (int i = 0; i < dtab.getDataItemCount(); i++) {
					values[i] = new TableValue(dtab.getDataItemName(i), i);
				}
			}
		else if (dtab.isAttributeNumeric(idx) || dtab.isAttributeTemporal(idx)) {
			for (int i = 0; i < dtab.getDataItemCount(); i++) {
				values[i] = new TableValue(dtab.getNumericAttrValue(idx, i), i);
			}
		} else {
			for (int i = 0; i < dtab.getDataItemCount(); i++) {
				values[i] = new TableValue(dtab.getAttrValue(idx, i), i);
			}
		}
		QSortAlgorithm.sort(values, false);
		if (attrBasedOrder == null) {
			attrBasedOrder = new IntArray(dtab.getDataItemCount(), 10);
		} else {
			attrBasedOrder.removeAllElements();
		}
		for (TableValue value : values) {
			attrBasedOrder.addElement(value.index);
		}
	}

	protected Classifier findClassifier() {
		Classifier cl = null;
		if (supervisor != null && supervisor.getObjectColorer() != null && (supervisor.getObjectColorer() instanceof Classifier) && supervisor.getObjectColorer().getEntitySetIdentifier().equals(dtab.getEntitySetIdentifier())) {
			cl = (Classifier) supervisor.getObjectColorer();
		}
		hasClasses = cl != null;
		return cl;
	}

	protected void defineClassBasedOrder() {
		if (!groupByClasses) {
			classBasedOrder = null;
			return;
		}
		Classifier cl = findClassifier();
		if (cl == null || cl.getNClasses() < 2) {
			classBasedOrder = null;
			return;
		}
		TableClassifier tcl = null;
		if (cl instanceof TableClassifier) {
			tcl = (TableClassifier) cl;
			if (!dtab.equals(tcl.getTable())) {
				tcl = null;
			}
		}
		if (classBasedOrder == null) {
			classBasedOrder = new IntArray(dtab.getDataItemCount(), 10);
		} else {
			classBasedOrder.removeAllElements();
		}
		IntArray classes = new IntArray(queryNumbers.size(), 10);
		boolean descSort = descending && sortattr != null;
		for (int i = 0; i < dtab.getDataItemCount(); i++) {
			int recN = i;
			if (attrBasedOrder != null)
				if (descSort) {
					recN = attrBasedOrder.elementAt(attrBasedOrder.size() - i - 1);
				} else {
					recN = attrBasedOrder.elementAt(i);
				}
			int classN = cl.getObjectClass(recN);
			int pos = -1;
			for (int j = 0; j < classes.size() && pos < 0; j++)
				if (classN < classes.elementAt(j)) {
					pos = j;
				}
			if (pos < 0) {
				pos = classes.size();
			}
			classBasedOrder.insertElementAt(recN, pos);
			classes.insertElementAt(classN, pos);
		}
	}

	//Recording numbers of objects satisfying to query in Vector queryNumbers and resorting of data
	private void setDataAccordingQuery() {
		if (queryNumbers == null) {
			queryNumbers = new IntArray(dtab.getDataItemCount(), 10);
		} else {
			queryNumbers.removeAllElements();
		}
		boolean descSort = descending && sortattr != null;
		for (int i = 0; i < dtab.getDataItemCount(); i++) {
			int recN = i;
			if (classBasedOrder != null) {
				recN = classBasedOrder.elementAt(i);
			} else if (attrBasedOrder != null)
				if (descSort) {
					recN = attrBasedOrder.elementAt(attrBasedOrder.size() - i - 1);
				} else {
					recN = attrBasedOrder.elementAt(i);
				}
			if (satisfiesToQuery(recN)) {
				queryNumbers.addElement(recN);
			}
		}
		condensedSizesCalculation();
	}

	private void setHighlightedFont() {
		if (gr == null || font == null) {
			this.initializationGraphicsObjects();
		}
		if (gr == null || font == null)
			return;
		gr.setFont(new Font(font.getName(), Font.BOLD, font.getSize()));
	}

	private void setPlainFont() {
		if (gr == null || font == null) {
			this.initializationGraphicsObjects();
		}
		if (gr == null || font == null)
			return;
		gr.setFont(new Font(font.getName(), Font.PLAIN, font.getSize()));
	}

	private void setSortedColumnBackgroundColor() {
		if (gr == null) {
			this.initializationGraphicsObjects();
		}
		if (gr == null)
			return;
		gr.setColor(Color.pink);
	}

	private void setFirstColumnAndRowBackgroundColor() {
		if (gr == null) {
			this.initializationGraphicsObjects();
		}
		if (gr == null)
			return;
		gr.setColor(Color.gray);
	}

	private Color getFirstColumnAndRowBackgroundColor() {
		return Color.gray;
	}

	private void setPlainColor() {
		if (gr == null) {
			this.initializationGraphicsObjects();
		}
		if (gr == null)
			return;
		gr.setColor(Color.black);
	}

	private void initializationGraphicsObjects() {
		Dimension dimMyTable = getSize();
		im = createImage(dimMyTable.width, dimMyTable.height);
		if (im == null)
			return;
		gr = im.getGraphics();
		fm = gr.getFontMetrics();
		font = gr.getFont();
	}

// definition number of row in the initial table (AttributeDataPortion) by row number in the table on screen
	private int toAttributeDataPortion(int rowN) {
		return queryNumbers.elementAt(rowN);
	}

// definition number of row in the table on screen by row number in the initial table (AttributeDataPortion)
	private int toScreenTable(int rowN) {
		return queryNumbers.indexOf(rowN);
	}

	protected void determineTableSizes() {
		if (colwidth < 1 || rowheight < 1) {
			cellSizeCalculation();
		}
		if (colwidth < 1 || rowheight < 1)
			return;
		int scrollwidth = 16; //width of scrollbars
		Dimension d = getSize();
		tablewidth = d.width - scrollwidth;
		tableheight = d.height - scrollwidth;
		col0width = col0widthmin + ((tablewidth - col0widthmin) % colwidth) - 1;
		row0height = row0heightmin + ((tableheight - row0heightmin) % rowheight) - 1;
		if (row0height < row0heightmin) {
			row0height = row0heightmin;
		}
	}

	protected int getVisibleColNumber() {
		if (!condensed)
			return Math.round(1.0f * (tablewidth - col0width) / colwidth) + 1;
		return Math.round(1.0f * (tablewidth - cCol0Width) / cColWidth) + 1;
	}

	protected int getVisibleRowNumber() {
		if (!condensed)
			return Math.round(1.0f * (tableheight - row0height) / rowheight) + 1;
		return Math.round(1.0f * (tableheight - cRow0Height) / cRowHeight) + 1;
	}

	/**
	 * Sets the colors of the data items (table rows). If the colors are given,
	 * the TableViewer will not address the supervisor for the current colors
	 * but will take the specified colors.
	 */
	public void setItemColors(Color itemColors[]) {
		this.itemColors = itemColors;
	}

	public boolean hasItemColors() {
		return itemColors != null && itemColors.length > 0;
	}

	/**
	 * Finds the appropriate color for the item (table row) with the given index.
	 * If the colors are preset, simply takes the color corresponding to the item.
	 * Otherwise, asks the supervisor for the current color.
	 */
	protected Color getColorForItem(int idx, Color defaultColor) {
		if (hasItemColors()) {
			if (idx < 0 || idx >= itemColors.length || itemColors[idx] == null)
				return defaultColor;
			return itemColors[idx];
		}
		if (supervisor == null)
			return defaultColor;
		return supervisor.getColorForDataItem(dtab.getDataItem(idx), dtab.getEntitySetIdentifier(), dtab.getContainerIdentifier(), defaultColor);
	}

//Table repainting /����������� �������/
	/**
	* Table repainting
	* 1) Should check what records have been selected by the query (table filter) and
	*    show only these records (use satisfiesToQuery(int))                          - IT IS MADE
	* 2) Selected records should be shown in "highlighted" mode
	*    (use isRecordSelected(int))                                                  - IT IS MADE
	*/
	public synchronized void tablerepaint() {
		if (gr == null)
			return;
		Dimension dimMyTable = getSize();
		if (dimMyTable == null || dimMyTable.width < 10 || dimMyTable.height < 10)
			return;
		if (queryNumbers == null)
			return;
		findClassifier();
		determineTableSizes();
		if (condensed) {
			condensedSizesCalculation();
		}
		int col0W = (condensed) ? cCol0Width : col0width, colW = (condensed) ? cColWidth : colwidth, row0H = (condensed) ? cRow0Height : row0height, rowH = (condensed) ? cRowHeight : rowheight;
		int colnumber = (visibleAttr == null) ? 1 : visibleAttr.size() + 1;
		int rownumber = queryNumbers.size() + 1;
		if (tablewidth > (col0W + colW * (colnumber - 1))) {
			tablewidth = col0W + colW * (colnumber - 1);
		} else if (tablewidth < col0W + colW) {
			tablewidth = col0W + colW;
		}
		if (tableheight > row0H + rowH * (rownumber - 1)) {
			tableheight = row0H + rowH * (rownumber - 1);
		} else if (tableheight < (row0H + rowH)) {
			tableheight = row0H + rowH;
		}

		gr.clearRect(0, 0, dimMyTable.width, dimMyTable.height);
		gr.setClip(0, 0, tablewidth, tableheight);

		int scrollwidth = 16; //width of scrollbars
		int visiblecolnumber = getVisibleColNumber();
		int visiblerownumber = getVisibleRowNumber();
		hScroll.setEnabled(visiblecolnumber < colnumber);
		vScroll.setEnabled(visiblerownumber < rownumber);
		hScroll.setBounds(col0W, tableheight, tablewidth - col0W, scrollwidth);
		vScroll.setBounds(tablewidth, row0H, scrollwidth, tableheight - row0H);
		hScroll.setBlockIncrement((visiblecolnumber - 1));
		vScroll.setBlockIncrement((visiblerownumber - 1));
		hScroll.setValues(getHScrollValue(), (visiblecolnumber - 1), 0, (colnumber - 1));
		vScroll.setValues(getVScrollValue(), (visiblerownumber - 1), 0, (rownumber - 1));
		//drawing top left cell
		if (sortattr != null && sortattr.equals("_NAME_")) {
			setSortedColumnBackgroundColor();
		} else {
			setFirstColumnAndRowBackgroundColor();
		}
		//System.out.println("1");
		gr.fillRect(0, 0, col0W, row0H);
		setPlainColor();
		//System.out.println("2");
		gr.drawRect(0, 0, col0W, row0H);
		int fontHeight = gr.getFontMetrics().getHeight(), asc = gr.getFontMetrics().getAscent();
		int mx = (condensed) ? 1 : uphor;
		if (namesDifferFromIds) {
			if (cbSize < 1) {
				cbSize = fontHeight;
			}
			if (cbSize > row0H || cbSize + 30 > col0W) {
				if (showIdsHSp != null) {
					showIdsHSp.setSize(0, 0);
				}
			} else {
				gr.setColor(Color.black);
				int y = (row0H - cbSize) / 2;
				if (showIds) {
					Icons.drawChecked(gr, 3, y, cbSize, cbSize);
				} else {
					Icons.drawUnchecked(gr, 3, y, cbSize, cbSize);
				}
				if (showIdsHSp == null) {
					showIdsHSp = new HotSpot(this);
					showIdsHSp.addActionListener(this);
					showIdsHSp.setPopup(res.getString("click_to_switch"));
				}
				showIdsHSp.setLocation(3, y);
				showIdsHSp.setSize(cbSize, cbSize);
				//System.out.println("3");
				gr.drawString(res.getString("show_ids"), 6 + cbSize, y + asc);
			}
		}
		if (visibleAttr != null && visibleAttr.size() > 0) {
			//drawing first row
			for (int i = 0; i < visiblecolnumber - 1; i++) {
				int dx = visibleAttr.elementAt(getHScrollValue() + i);
				if (sortattr != null && sortattr.equals(dtab.getAttributeId(dx))) {
					setSortedColumnBackgroundColor();
				} else {
					setFirstColumnAndRowBackgroundColor();
				}
				int x = col0W + i * colW;
				//System.out.println("4 (fillRect): "+x+",0,"+colW+","+row0H);
				gr.fillRect(x, 0, colW, row0H);
				setPlainColor();
				gr.setClip(x, 0, colW, row0H);
				String s = dtab.getAttributeName(dx);
				if (s == null) {
					s = dtab.getAttributeId(dx);
				}
				int nnext = -1, nprev = nnext;
				dx = 0;
				int y = (row0H % fontHeight) / 2;
				do {
					nprev = nnext;
					nnext = s.indexOf(0x020, nprev + 1);
					boolean ent;
					if (nnext == -1) {
						ent = (colW - 2 * mx < dx + fm.stringWidth(s.substring(nprev + 1)));
					} else {
						ent = (colW - 2 * mx < dx + fm.stringWidth(s.substring(nprev + 1, nnext)));
					}
					if ((dx > 0) && ent) {
						dx = 0;
						y += fontHeight;
					}
					if (y + asc > row0H) {
						break;
					}
					if (nnext != -1) {
						//System.out.println("5 (drawString): "+((col0W + i*colW+mx)+dx)+","+(y+asc));
						gr.drawString(s.substring(nprev + 1, nnext), (col0W + i * colW + mx) + dx, y + asc);
						dx = dx + fm.stringWidth(s.substring(nprev + 1, nnext) + " ");
					} else {
						//System.out.println("6 (drawString): "+((col0W + i*colW+mx)+dx)+","+(y+asc));
						gr.drawString(s.substring(nprev + 1), (col0W + i * colW + mx) + dx, y + asc);
					}
				} while (nnext != -1);
				gr.setClip(0, 0, tablewidth, tableheight);
				//System.out.println("7 (drawRect): "+(col0W + i*colW)+",0,"+colW+","+row0H);
				gr.drawRect((col0W + i * colW), 0, colW, row0H);
			}
		}
		//drawing first column
		int ypos = row0H;
		for (int i = 0; i < visiblerownumber - 1; i++) {
			int dy = toAttributeDataPortion(getVScrollValue() + i);
			Color textColor = Color.black, defColor = getFirstColumnAndRowBackgroundColor();
			Color cl = defColor;
			if (hasClasses || hasItemColors()) {
				cl = getColorForItem(dy, defColor);
			}
			if (!cl.equals(defColor))
				if (cl.getGreen() < 150 && cl.getRed() < 160) {
					textColor = Color.yellow;
				} else {
					;
				}
			else if (rowH < fontHeight && isRecordSelected(dy)) {
				cl = Color.darkGray;
			}
			gr.setColor(cl);
			//System.out.println("8 (fillRect): 0,"+(ypos +1)+","+col0W+","+rowH);
			gr.fillRect(0, ypos + 1, col0W, rowH);
			if (rowH >= fontHeight) {
				String s = dtab.getDataItemName(dy);
				if (s == null || s.equals("")) {
					s = dtab.getDataItemId(dy);
				} else if (showIds) {
					s = dtab.getDataItemId(dy) + "   " + s;
				}
				if (isRecordSelected(dy)) {
					setHighlightedFont();
				}
				gr.setColor(textColor);
				gr.setClip(0, ypos, col0W, rowH);
				//System.out.println("9 (drawString): "+mx+","+(ypos+asc));
				gr.drawString((s == null) ? "" : s, mx, ypos + asc);
				gr.setClip(0, 0, tablewidth, tableheight);
				setPlainFont();
				gr.setColor(Color.black);
				//System.out.println("10 (drawRect): 0,"+ypos+","+col0W+","+rowH);
				gr.drawRect(0, ypos, col0W, rowH);
				if (isRecordSelected(dy)) {
					gr.drawLine(0, ypos + rowH - 1, col0W, ypos + rowH - 1);
				}
			} else {
				gr.setColor(Color.black);
				gr.drawLine(0, ypos, 0, ypos + rowH);
				gr.drawLine(col0W, ypos, col0W, ypos + rowH);
			}
			ypos += rowH;
		}
		gr.setColor(Color.black);
		gr.drawLine(0, row0H, col0W, row0H);
		if (visibleAttr != null && visibleAttr.size() > 0) {
			//drawing table body:
			int xpos = col0W;
			for (int i = 0; i < visiblecolnumber - 1; i++) {
				//calculation minimum value and (max-min) value for given attribute (it is needed for TableLens)
				int dx = visibleAttr.elementAt(getHScrollValue() + i);
				double minbound = Double.NaN, diapaz = 0f;
				if ((dtab.isAttributeNumeric(dx) || dtab.isAttributeTemporal(dx)) && tablelens) {
					NumRange range = getAttrValueRange(dx);
					if (range != null && range.minValue < range.maxValue) {
						minbound = range.minValue;
						diapaz = range.maxValue - minbound;
					}
				}
				//------------------------------------------------------------------------------------------
				ypos = row0H;
				for (int j = 0; j < visiblerownumber - 1; j++) {
					int dy = toAttributeDataPortion(getVScrollValue() + j);
					if (rowH < fontHeight && isRecordSelected(dy)) {
						gr.setColor(Color.darkGray);
					} else {
						gr.setColor(Color.lightGray);
					}
					//System.out.println("11 (fillRect): "+(xpos +1)+","+(ypos +1)+","+colW+","+rowH);
					gr.fillRect(xpos + 1, ypos + 1, colW, rowH);
					gr.setColor(Color.black);
					String s = dtab.getAttrValueAsString(dx, dy);
					if (s != null) {
						if (dtab.isAttributeNumeric(dx) || dtab.isAttributeTemporal(dx)) {
							if (tablelens) {
								Color cl = Color.gray;
								if (hasClasses || hasItemColors()) {
									cl = getColorForItem(dy, Color.gray);
								}
								gr.setColor(cl);
								int lenswidth = 0;
								if (diapaz > 0 && !Double.isNaN(dtab.getNumericAttrValue(dx, dy))) {
									lenswidth = (int) Math.round((dtab.getNumericAttrValue(dx, dy) - minbound) * (colW - 1) / diapaz);
								}
								//System.out.println("12 (fillRect): "+(col0W + (i+1)*colW-lenswidth)+","+(row0H + j*rowH+1)+","+lenswidth+","+rowH);
								gr.fillRect((col0W + (i + 1) * colW - lenswidth), (row0H + j * rowH + 1), lenswidth, rowH);
								setPlainColor();
							}
							if (rowH >= fontHeight) {
								gr.setClip(xpos, ypos, colW, rowH);
								if (isRecordSelected(dy)) {
									setHighlightedFont();
								}
								int width = fm.stringWidth(s);
								//System.out.println("13 (drawString): "+(xpos +colW-width-mx)+","+(ypos +asc));
								gr.drawString(s, xpos + colW - width - mx, ypos + asc);
								gr.setClip(0, 0, tablewidth, tableheight);
								setPlainFont();
							}
						} else if (rowH >= fontHeight) {
							gr.setClip(xpos, ypos, colW, rowH);
							//System.out.println("14 (drawString): "+(xpos +mx)+","+(ypos +asc));
							gr.drawString(s, xpos + mx, ypos + asc);
							gr.setClip(0, 0, tablewidth, tableheight);
						}
					}
					if (rowH >= fontHeight) {
						//System.out.println("15 (drawRect): "+xpos+","+ypos+","+colW+","+rowH);
						gr.drawRect(xpos, ypos, colW, rowH);
						if (isRecordSelected(dy)) {
							gr.drawLine(xpos, ypos + rowH - 1, xpos + colW, ypos + rowH - 1);
						}
					} else {
						gr.drawLine(xpos + colW, ypos, xpos + colW, ypos + rowH);
					}
					ypos += rowH;
				}
				xpos += colW;
			}
		}
		if (cellPopM == null) {
			cellPopM = new PopupManager(this, "", true);
			addMouseMotionListener(this);
		}
	}

	/**
	* Finds the minimum and maximum values in the given table column taking into
	* account the current filter
	*/
	protected NumRange getAttrValueRange(int colN) {
		if (dtab == null || queryNumbers == null || queryNumbers.size() < 1 || colN < 0 || colN >= dtab.getAttrCount())
			return null;
		NumRange nr = new NumRange();
		for (int i = 0; i < queryNumbers.size(); i++) {
			double val = dtab.getNumericAttrValue(colN, queryNumbers.elementAt(i));
			if (!Double.isNaN(val)) {
				if (Double.isNaN(nr.minValue) || nr.minValue > val) {
					nr.minValue = val;
				}
				if (Double.isNaN(nr.maxValue) || nr.maxValue < val) {
					nr.maxValue = val;
				}
			}
		}
		if (Double.isNaN(nr.maxValue))
			return null;
		return nr;
	}

	/**
	* Determines the table row and column corresponding to the given screen
	* coordinates (of the mouse). -1 means the mouse being outside the table,
	* column==0 means the column with the object names, row==0 means the caption
	* of the table. Indexing of actual rows and columns, hence, starts from 1.
	*/
	protected Point getColAndRow(int x, int y) {
		Point p = new Point(-1, -1);
		if (x < 0 || x > tablewidth || y < 0 || y > tableheight)
			return p;
		int col0W = (condensed) ? cCol0Width : col0width, colW = (condensed) ? cColWidth : colwidth, row0H = (condensed) ? cRow0Height : row0height, rowH = (condensed) ? cRowHeight : rowheight;
		if (x < col0W) {
			p.x = 0;
		} else {
			p.x = (x - col0W) / colW + 1 + getHScrollValue();
		}
		if (y < row0H) {
			p.y = 0;
		} else {
			p.y = (y - row0H) / rowH + 1 + getVScrollValue();
		}
		return p;
	}

//----------------------------------------------------------------------------
	/**
	* This function is needed in order to determine correctly the size of the
	* window needed for the TableViewer
	*/
	@Override
	public Dimension getPreferredSize() {
		int w = 500, h = 400; //count more exactly!!!
		if (w > Metrics.scrW() - 150) {
			w = Metrics.scrW() - 150;
		}
		if (h > Metrics.scrH() - 150) {
			h = Metrics.scrH() - 150;
		}
		return new Dimension(w, h);
	}

//---------------------- AdjustmentListener interface ------------------------
	@Override
	public void adjustmentValueChanged(AdjustmentEvent e) {

		tablerepaint();
		repaint();
	}

//---------------------- ComponentListener interface --------------------------
	@Override
	public void componentResized(ComponentEvent e) {
		initializationGraphicsObjects();
		if (im != null) {
			im.flush();
			tablerepaint();
			repaint();
		}
	}

	@Override
	public void componentHidden(ComponentEvent e) {
	}

	@Override
	public void componentMoved(ComponentEvent e) {
	}

	@Override
	public void componentShown(ComponentEvent e) {
		initializationGraphicsObjects();
	}

//---------------------- MouseListener interface -----------------------------
	protected int dragStartX = -1, dragStartY = -1, dragLastX = -1, dragLastY = -1;
	protected boolean dragging = false;

	/**
	* Upon mouse click on a table row generate object click event and send it
	* to the supervisor
	*/
	@Override
	public void mouseClicked(MouseEvent e) {
		if (e.getClickCount() > 1)
			return; //ignore repeated clicks
		int x = e.getX(), y = e.getY();
		if (showIdsHSp != null && showIdsHSp.contains(x, y))
			return;
		Rectangle r = vScroll.getBounds();
		if (x >= r.x)
			return;
		if (y > r.y + r.height)
			return;
		if (y < r.y) { //this may be a click in the table caption
			int col0W = (condensed) ? cCol0Width : col0width, colW = (condensed) ? cColWidth : colwidth;
			int colN = (x <= col0W) ? -1 : (x - col0W) / colW + getHScrollValue(), attrN = (colN < 0 || visibleAttr == null) ? -1 : visibleAttr.elementAt(colN);
			String attr = (colN < 0) ? "_NAME_" : dtab.getAttributeId(attrN);
			if (sortattr != null && sortattr.equals(attr)) {
				setDescending(!descending);
			} else {
				setSortAttr(attr);
			}
			tablerepaint();
			repaint();
			if (owner != null) {
				owner.propertyChange(new PropertyChangeEvent(this, "sorting", null, null));
			}
		} else {
			if (supervisor == null)
				return;
			int rowN = getVScrollValue() + (y - r.y) / ((condensed) ? cRowHeight : rowheight);
			rowN = toAttributeDataPortion(rowN);
			if (rowN < 0)
				return;
			ObjectEvent oevt = new ObjectEvent(this, ObjectEvent.click, e, dtab.getEntitySetIdentifier());
			oevt.addEventAffectedObject(dtab.getDataItemId(rowN));
			supervisor.processObjectEvent(oevt);
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (dragging)
			return;
		dragStartX = dragLastX = e.getX();
		dragStartY = dragLastY = e.getY();
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		dragging = true;
		if (e.getX() == dragLastX && e.getY() == dragLastY)
			return;
		drawFrame(dragStartX, dragStartY, dragLastX, dragLastY);
		dragLastX = e.getX();
		dragLastY = e.getY();
		drawFrame(dragStartX, dragStartY, dragLastX, dragLastY);
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (dragging) {
			drawFrame(dragStartX, dragStartY, dragLastX, dragLastY);
			dragging = false;
			Rectangle r = vScroll.getBounds();
			if ((dragStartY <= r.y && dragLastY <= r.y) || (dragStartY >= r.y + r.height && dragLastY >= r.y + r.height)) {
				dragStartX = dragStartY = dragLastX = dragLastY = -1;
				return;
			}
			int r1, r2;
			if (dragStartY < r.y) {
				r1 = 0;
			} else if (dragStartY > r.y + r.height) {
				r1 = getVisibleRowNumber() - 1;
			} else {
				r1 = getVScrollValue() + (dragStartY - r.y) / ((condensed) ? cRowHeight : rowheight);
			}
			if (dragLastY < r.y) {
				r2 = 0;
			} else if (dragLastY > r.y + r.height) {
				r2 = getVisibleRowNumber() - 1;
			} else {
				r2 = getVScrollValue() + (dragLastY - r.y) / ((condensed) ? cRowHeight : rowheight);
			}
			if (r1 > r2) {
				int rr = r1;
				r1 = r2;
				r2 = rr;
			}
			Vector oids = new Vector(r2 - r1 + 1, 1);
			for (int i = r1; i <= r2; i++) {
				int recN = toAttributeDataPortion(i);
				oids.addElement(dtab.getDataItemId(recN));
			}
			ObjectEvent oe = new ObjectEvent(this, ObjectEvent.frame, e, dtab.getEntitySetIdentifier(), oids);
			supervisor.processObjectEvent(oe);
		}
		dragStartX = dragStartY = dragLastX = dragLastY = -1;
	}

	protected void drawFrame(int x0, int y0, int x, int y) {
		if (x - x0 != 0 || y - y0 != 0) {
			Graphics g = getGraphics();
			g.setColor(Color.magenta);
			g.setXORMode(Color.gray);
			g.drawLine(x0, y0, x, y0);
			g.drawLine(x, y, x0, y);
			g.setPaintMode();
		}
	}

	protected int currCol = -1, currRow = -1;

	@Override
	public void mouseExited(MouseEvent e) {
		currCol = currRow = -1;
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		if (dragging)
			return;
		if (cellPopM == null)
			return;
		Point p = getColAndRow(e.getX(), e.getY());
		if (p.x < 0 || p.y < 0) {
			PopupManager.hideWindow();
			cellPopM.setKeepHidden(true);
			currCol = currRow = -1;
			return;
		}
		if (p.x == currCol && p.y == currRow)
			return;
		currCol = p.x;
		currRow = p.y;
		if (currRow == 0 && (!condensed || currCol == 0)) {
			PopupManager.hideWindow();
			cellPopM.setKeepHidden(true);
			return;
		}
		String str = null;
		int aidx = -1;
		if (currCol > 0 && visibleAttr != null) {
			aidx = visibleAttr.elementAt(currCol - 1);
		}
		if (currRow == 0) {
			//show the name of the attribute
			str = dtab.getAttributeName(aidx);
		} else {
			int rowN = toAttributeDataPortion(currRow - 1);
			if (currCol == 0) {
				String id = dtab.getDataItemId(rowN), name = dtab.getDataItemName(rowN);
				if (name == null || name.equals(id)) {
					str = id;
				} else {
					str = id + "   " + name;
				}
			} else {
				str = dtab.getAttrValueAsString(aidx, rowN);
			}
		}
		if (str == null || str.length() < 1) {
			PopupManager.hideWindow();
			cellPopM.setKeepHidden(true);
			return;
		}
		int w = 0, h = 0;
		if (condensed) {
			w = (currCol == 0) ? cCol0Width : cColWidth;
			h = (currRow == 0) ? cRow0Height : cRowHeight;
		} else {
			w = (currCol == 0) ? col0width : colwidth;
			h = (currRow == 0) ? row0height : rowheight;
		}
		FontMetrics fm = getGraphics().getFontMetrics();
		if (fm.getHeight() > h || fm.stringWidth(str) > w) {
			cellPopM.setKeepHidden(false);
			cellPopM.setText(str);
			cellPopM.startShow(e.getX(), e.getY());
		} else {
			PopupManager.hideWindow();
			cellPopM.setKeepHidden(true);
		}
	}

//---------------------- HighlightListener interface -------------------------
	/**
	* Notification about change of the set of objects to be transiently
	* highlighted. A TableViewer does not react to this event.
	*/
	@Override
	public void highlightSetChanged(Object source, String setId, Vector highlighted) {
	}

	/**
	* Notification about change of the set of objects to be selected (durably
	* highlighted).
	* In response, a TableViewer should highlight/dehighlight the corresponding
	* rows in the table.
	* The argument "source" is usually a reference to a Highlighter.
	* The argument setId is the identifier of the set the highlighted objects
	* belong to (e.g. map layer or table). The argument "selected" is a vector
	* of identifiers of currently selected objects.
	*/
	@Override
	public void selectSetChanged(Object source, String setId, Vector selected) {
		IntArray changedRec = new IntArray(10, 10);
		// 1) Add the records that have been just selected to the list of selected
		//    records
		if (selected != null && selected.size() > 0) {
			if (selNumbers == null) {
				selNumbers = new IntArray(20, 10);
			}
			for (int i = 0; i < dtab.getDataItemCount(); i++)
				if (selNumbers.indexOf(i) < 0 && //it is not yet in selNumbers
						selected.contains(dtab.getDataItemId(i))) {
					selNumbers.addElement(i);
					changedRec.addElement(i);
				}
		}
//Set vertical scrollbar to show first changed(new selected) record if it is not visible
		if (changedRec.size() > 0) {
			int rowN = toScreenTable(changedRec.elementAt(0));
			if ((rowN >= 0) && ((rowN < getVScrollValue()) || (rowN >= (getVScrollValue() + vScroll.getVisibleAmount())))) {
				vScroll.setValue(rowN);
			}
		}
//-----------------------------------------------------------------------------
		// 2) check earlier selected records - possibly, they are no more selected
		if (selNumbers != null && selNumbers.size() > 0) {
			if (selected == null || selected.size() < 1) { //all records are deselected
				for (int i = 0; i < selNumbers.size(); i++) {
					changedRec.addElement(selNumbers.elementAt(i));
				}
				selNumbers.removeAllElements();
			} else {
				for (int i = selNumbers.size() - 1; i >= 0; i--)
					if (!selected.contains(dtab.getDataItemId(selNumbers.elementAt(i)))) {
						//remove this record from the list of selected records
						changedRec.addElement(selNumbers.elementAt(i));
						selNumbers.removeElementAt(i);
					}
			}
		}
		if (changedRec.size() < 1)
			return;
		tablerepaint();
		update(gr);
		repaint();
	}

//------------------------- EventSource interface ------------------------------
	/**
	* The EventSource answers whether it can produce the specified
	* type of events. A TableViewer produces object click events
	* that are propagated to other components.
	*/
	@Override
	public boolean doesProduceEvent(String eventId) {
		return eventId != null && (eventId.equals(ObjectEvent.click) || eventId.equals(ObjectEvent.frame));
	}

	/**
	* Returns a unique identifier of the event source (takes the identifier of
	* the table).
	*/
	@Override
	public String getIdentifier() {
		if (dtab == null)
			return "null table";
		return "Table " + dtab.getContainerIdentifier();
	}

//----------------------- PropertyChangeListener interface --------------------
	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		if ((pce.getSource().equals(tf)) && (pce.getPropertyName().equals("destroyed"))) {
			tf.removePropertyChangeListener(this);
			tf = null;
			return;
		}
		if (pce.getPropertyName().equalsIgnoreCase("filter")) {
			setDataAccordingQuery(); //change of table filter
		} else if (pce.getPropertyName().equals(Supervisor.eventObjectColors)) {//propagated classification of objects
			if (dtab.getEntitySetIdentifier().equals(pce.getNewValue())) {
				defineClassBasedOrder();
				setDataAccordingQuery();
			}
		} else if (pce.getSource() == dtab) {
			if (pce.getPropertyName().equals("destroyed") || pce.getPropertyName().equals("structure_complete")) {
				destroy();
				return;
			}
			if (pce.getPropertyName().equals("filter")) {
				if (tf != null) {
					tf.removePropertyChangeListener(this);
				}
				tf = dtab.getObjectFilter();
				if (tf != null) {
					tf.addPropertyChangeListener(this);
				}
				setDataAccordingQuery(); //change of table filter
			}
			if (pce.getPropertyName().equals("values")) {
				Vector v = (Vector) pce.getNewValue(); // list of changed attributes
				if (v == null || v.size() < 1 || visibleAttr == null)
					return;
				int firstChanged = -1;
				for (int i = 0; i < visibleAttr.size() && firstChanged < 0; i++)
					if (v.contains(dtab.getAttributeId(visibleAttr.elementAt(i)))) {
						firstChanged = i;
					}
				if (firstChanged < 0)
					return;
				if (v.contains(sortattr)) {
					defineAttrBasedOrder();
					defineClassBasedOrder();
				}
				reset();
				//Set horizontal scrollbar to show first changed attribute if it is not visible
				hScroll.setValues(getHScrollValue(), getVisibleColNumber() - 1, hScroll.getMinimum(), dtab.getAttrCount());
				if (firstChanged < getHScrollValue() || firstChanged >= getHScrollValue() + hScroll.getVisibleAmount()) {
					hScroll.setValue(firstChanged);
				}
			} else if (pce.getPropertyName().equals("data_added") || pce.getPropertyName().equals("data_removed") || pce.getPropertyName().equals("data_updated")) {
				defineAttrBasedOrder();
				defineClassBasedOrder();
				reset();
			}
		}
		tablerepaint();
		repaint();
	}

	protected void reset() {
		checkWhatSelected();
		setDataAccordingQuery();
		cellSizeCalculation();
	}

	/**
	* Reacts to the "hot spot" clicking
	*/
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource().equals(showIdsHSp)) {
			setShowIdentifiers(!showIds);
			tablerepaint();
			repaint();
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
			if (dtab != null) {
				supervisor.removeHighlightListener(this, dtab.getEntitySetIdentifier());
			}
			supervisor.removePropertyChangeListener(this);
			supervisor.removeObjectEventSource(this);
		}
		if (dtab != null) {
			dtab.removePropertyChangeListener(this);
		}
		if (tf != null) {
			tf.removePropertyChangeListener(this);
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
}
