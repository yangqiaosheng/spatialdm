package spade.lib.util;

import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 02-Mar-2007
 * Time: 14:16:27
 * A matrix where rows and columns may have identifiers and cells may contain
 * values of several attributes.
 */
public class Matrix {
	/**
	 * Current number of allocated rows and columns (not all of them may be
	 * really used).
	 */
	protected int nRows = 50, nColumns = 50;
	/**
	 * The increment, which is used when the allocated space is not sufficient
	 * for adding a new item.
	 */
	protected int incr = 10;
	/**
	 * The identifiers of the rows (not necessarily present)
	 */
	protected Vector rowIds = null;
	/**
	 * The identifiers of the columns (not necessarily present)
	 */
	protected Vector colIds = null;
	/**
	 * The names or identifiers of attributes (not necessarily present).
	 */
	protected Vector attrs = null;
	/**
	 * For each attribute, contains an allocated space of the size
	 * nRows x nColumns where the values of the attribute may be
	 * stored. If no attributes are specified, the vector contains one
	 * allocated matrix.
	 */
	protected Vector data = null;

	/**
	 * Creates a matrix with the initial capacity determined by the given
	 * number of rows and columns and the given increment.
	 */
	public Matrix(int nRows, int nColumns, int increment) {
		if (nRows < 1) {
			nRows = 1;
		}
		if (nColumns < 1) {
			nColumns = 1;
		}
		if (increment < 1) {
			increment = 1;
		}
		this.nRows = nRows;
		this.nColumns = nColumns;
		this.incr = increment;
		data = new Vector(5, 5);
		data.addElement(makeMatrix(nRows, nColumns));
	}

	/**
	 * Creates a matrix with the default capacity and increment.
	 */
	public Matrix() {
		this(50, 50, 10);
	}

	/**
	 * Allocates a matrix of the size rowNum x colNum and fills it with null
	 * values
	 */
	protected Object[][] makeMatrix(int rowNum, int colNum) {
		Object values[][] = new Object[rowNum][colNum];
		for (int i = 0; i < rowNum; i++) {
			for (int j = 0; j < colNum; j++) {
				values[i][j] = null;
			}
		}
		return values;
	}

	/**
	 * Adds an attribute with the given index and allocates a matrix for this
	 * attribute. If this is the first added attribute, the matrix already
	 * exists. If an attribute with this name already exists (the case is ignored),
	 * the new attribute is not added.
	 * Returns the index of the attribute in the list of attributes.
	 * If the name is null, nothing is done, and the method returns -1.
	 */
	public int addAttribute(String attrName) {
		if (attrName == null)
			return -1;
		int idx = -1;
		if (attrs == null) {
			attrs = new Vector(5, 5);
		} else {
			idx = StringUtil.indexOfStringInVectorIgnoreCase(attrName, attrs);
		}
		if (idx >= 0)
			return idx;
		attrs.addElement(attrName);
		idx = attrs.size() - 1;
		while (idx >= data.size()) {
			data.addElement(makeMatrix(nRows, nColumns));
		}
		return idx;
	}

	/**
	 * Returns the index of the attribute with the given name, ignoring the case
	 */
	public int getAttrIndex(String attrName) {
		return StringUtil.indexOfStringInVectorIgnoreCase(attrName, attrs);
	}

	/**
	 * Returns the name of the attribute with the given index
	 */
	public String getAttrName(int idx) {
		if (idx < 0 || attrs == null || idx >= attrs.size())
			return null;
		return (String) attrs.elementAt(idx);
	}

	/**
	 * Returns the number of attributes
	 */
	public int getNAttributes() {
		return data.size();
	}

	/**
	 * Returns the number of rows
	 */
	public int getNRows() {
		return nRows;
	}

	/**
	 * Returns the number of columns
	 */
	public int getNColumns() {
		return nColumns;
	}

	/**
	 * Adds the given identifier (if not null) to the list of identifiers of the
	 * rows. If such identifier is already present in the list (the case is
	 * ignored), it is not added once more. Returns the index of the identifier
	 * in the list.
	 */
	public int addRowId(String id) {
		if (id == null)
			return -1;
		int idx = -1;
		if (rowIds == null) {
			rowIds = new Vector(nRows, incr);
		} else {
			idx = StringUtil.indexOfStringInVectorIgnoreCase(id, rowIds);
		}
		if (idx >= 0)
			return idx;
		rowIds.addElement(id);
		return rowIds.size() - 1;
	}

	/**
	 * Returns the index of the row with the given identifier, ignoring the case
	 */
	public int getRowIndex(String rowId) {
		return StringUtil.indexOfStringInVectorIgnoreCase(rowId, rowIds);
	}

	/**
	 * Returns the identifier of the row with the given index
	 */
	public String getRowIdentifier(int idx) {
		if (idx < 0 || rowIds == null || idx >= rowIds.size())
			return null;
		return (String) rowIds.elementAt(idx);
	}

	/**
	 * Adds the given identifier (if not null) to the list of identifiers of the
	 * columns. If such identifier is already present in the list (the case is
	 * ignored), it is not added once more. Returns the index of the identifier
	 * in the list.
	 */
	public int addColumnId(String id) {
		if (id == null)
			return -1;
		int idx = -1;
		if (colIds == null) {
			colIds = new Vector(nColumns, incr);
		} else {
			idx = StringUtil.indexOfStringInVectorIgnoreCase(id, colIds);
		}
		if (idx >= 0)
			return idx;
		colIds.addElement(id);
		return colIds.size() - 1;
	}

	/**
	 * Returns the index of the column with the given identifier, ignoring the case
	 */
	public int getColumnIndex(String columnId) {
		return StringUtil.indexOfStringInVectorIgnoreCase(columnId, colIds);
	}

	/**
	 * Returns the identifier of the column with the given index
	 */
	public String getColumnIdentifier(int idx) {
		if (idx < 0 || colIds == null || idx >= colIds.size())
			return null;
		return (String) colIds.elementAt(idx);
	}

	/**
	 * Puts the given value of the attribute with the given index in the matrix
	 * in the specified row and column.
	 */
	public void put(Object value, int attrIdx, int rowIdx, int colIdx) {
		if (rowIdx < 0 || colIdx < 0)
			return;
		if (attrIdx < 0) {
			attrIdx = 0;
		}
		if (rowIdx >= nRows || colIdx >= nColumns) {
			int newRowNum = nRows, newColNum = nColumns;
			while (rowIdx >= newRowNum) {
				newRowNum += incr;
			}
			while (colIdx >= newColNum) {
				newColNum += incr;
			}
			for (int n = 0; n < data.size(); n++) {
				Object oldVal[][] = (Object[][]) data.elementAt(n);
				Object newVal[][] = makeMatrix(newRowNum, newColNum);
				for (int i = 0; i < nRows; i++) {
					for (int j = 0; j < nColumns; j++) {
						newVal[i][j] = oldVal[i][j];
					}
				}
				data.setElementAt(newVal, n);
			}
			nRows = newRowNum;
			nColumns = newColNum;
		}
		while (attrIdx >= data.size()) {
			data.addElement(makeMatrix(nRows, nColumns));
		}
		Object values[][] = (Object[][]) data.elementAt(attrIdx);
		values[rowIdx][colIdx] = value;
	}

	/**
	 * Puts the given value in the matrix in the specified row and column.
	 * This is assumed to be the value of the first attribute, if more than
	 * one attributes are present.
	 */
	public void put(Object value, int rowIdx, int colIdx) {
		put(value, 0, rowIdx, colIdx);
	}

	/**
	 * Puts the given value of the attribute with the given name in the matrix
	 * in the specified row and column. If the attribute with such a name does not
	 * exist yet, it is added. If the name is null, assigns the value to the
	 * first attribute.
	 */
	public void put(Object value, String attrName, int rowIdx, int colIdx) {
		if (attrName == null) {
			put(value, 0, rowIdx, colIdx);
			return;
		}
		int idx = getAttrIndex(attrName);
		if (idx < 0) {
			idx = addAttribute(attrName);
		}
		put(value, idx, rowIdx, colIdx);
	}

	/**
	 * Puts the given value of the attribute with the given index in the matrix
	 * in the row and column specified by their identifiers. If these identifiers
	 * are not present yet in the corresponding lists, they are added.
	 */
	public void put(Object value, int attrIdx, String rowId, String colId) {
		int rowIdx = getRowIndex(rowId);
		if (rowIdx < 0) {
			rowIdx = addRowId(rowId);
		}
		int colIdx = getColumnIndex(colId);
		if (colIdx < 0) {
			colIdx = addColumnId(colId);
		}
		put(value, attrIdx, rowIdx, colIdx);
	}

	/**
	 * Puts the given value in the row and column specified by their identifiers.
	 * If these identifiers are not present yet in the corresponding lists,
	 * they are added.
	 * If more than one attributes are present, the value is assigned to the
	 * first attribute.
	 */
	public void put(Object value, String rowId, String colId) {
		put(value, 0, rowId, colId);
	}

	/**
	 * Puts the given value of the attribute with the given name in the matrix
	 * in the row and column specified by their identifiers. If the attribute with
	 * such a name does not exist yet, it is added. If the name is null, the value
	 * is assigned to the first attribute.
	 * If the identifiers of the row and column are not present yet in the
	 * corresponding lists, they are added.
	 */
	public void put(Object value, String attrName, String rowId, String colId) {
		if (attrName == null) {
			put(value, 0, rowId, colId);
			return;
		}
		int idx = getAttrIndex(attrName);
		if (idx < 0) {
			idx = addAttribute(attrName);
		}
		put(value, idx, rowId, colId);
	}

	/**
	 * Returns the value of the attribute with the given index from the specified
	 * row and column
	 */
	public Object get(int attrIdx, int rowIdx, int colIdx) {
		if (attrIdx >= data.size())
			return null;
		if (rowIdx < 0 || rowIdx >= nRows || colIdx < 0 || colIdx >= nColumns)
			return null;
		Object values[][] = (Object[][]) data.elementAt(attrIdx);
		return values[rowIdx][colIdx];
	}

	/**
	 * Returns the value of the first or single attribute from the specified
	 * row and column
	 */
	public Object get(int rowIdx, int colIdx) {
		return get(0, rowIdx, colIdx);
	}

	/**
	 * Returns the value of the attribute with the given name from the specified
	 * row and column. If the name is null, returns the value of the first attribute.
	 */
	public Object get(String attrName, int rowIdx, int colIdx) {
		if (attrName == null)
			return get(0, rowIdx, colIdx);
		int idx = getAttrIndex(attrName);
		if (idx < 0)
			return null;
		return get(idx, rowIdx, colIdx);
	}

	/**
	 * Returns the value of the attribute with the given index from the
	 * row and column specified by their identifiers.
	 */
	public Object get(int attrIdx, String rowId, String colId) {
		if (attrIdx >= data.size())
			return null;
		int rowIdx = getRowIndex(rowId);
		if (rowIdx < 0)
			return null;
		int colIdx = getColumnIndex(colId);
		if (colIdx < 0)
			return null;
		return get(attrIdx, rowIdx, colIdx);
	}

	/**
	 * Returns the value of the first or single attribute from the
	 * row and column specified by their identifiers.
	 */
	public Object get(String rowId, String colId) {
		return get(0, rowId, colId);
	}

	/**
	 * Returns the value of the attribute with the given name from the
	 * row and column specified by their identifiers. If the name is null, returns
	 * the value of the first attribute.
	 */
	public Object get(String attrName, String rowId, String colId) {
		if (attrName == null)
			return get(0, rowId, colId);
		int idx = getAttrIndex(attrName);
		if (idx < 0)
			return null;
		return get(idx, rowId, colId);
	}

	/**
	 * Returns the maximum value of the attribute with the given index, if the
	 * values inplement the interface Comparable
	 */
	public Object getMaximum(int attrIdx) {
		if (attrIdx < 0) {
			attrIdx = 0;
		}
		if (attrIdx >= data.size())
			return null;
		Object values[][] = (Object[][]) data.elementAt(attrIdx);
		java.lang.Comparable max = null;
		for (int i = 0; i < nRows; i++) {
			for (int j = 0; j < nColumns; j++)
				if (values[i][j] != null && (values[i][j] instanceof java.lang.Comparable))
					if (max == null || max.compareTo(values[i][j]) < 0) {
						max = (java.lang.Comparable) values[i][j];
					}
		}
		return max;
	}

	/**
	 * Returns the maximum value of the first or single attribute, if the
	 * values inplement the interface Comparable
	 */
	public Object getMaximum() {
		return getMaximum(0);
	}

	/**
	 * Returns the maximum value of the attribute with the given name, if the
	 * values inplement the interface Comparable. If the name is null, returns
	 * the maximum value of the first attribute.
	 */
	public Object getMaximum(String attrName) {
		if (attrName == null)
			return getMaximum(0);
		int idx = getAttrIndex(attrName);
		if (idx < 0)
			return null;
		return getMaximum(idx);
	}
}
