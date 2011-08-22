package spade.analysis.datamanage;

import java.util.Vector;

/** This class is needed to manage the data for MultiCorreltions.class and
 * TableConstructor.class.<br>
 * <br>
 * For adding data, you have to create a Vector and send it to one of
 * the methods addRow or addColumn. These methods creates a new linear
 * table (row for row). if you want to update your content, you have to
 * call the method updateRow(int, vector). The int value represents the row,
 * which will be updated.<br>
 * <br>
 * If you want to access the data, you can access the public vectors
 * row. The row and column sizes are saved in columnCount and rowCount.<br>
 *
 * @author Christian Gro�
 * @version 1.1
 */
public class DataStorage {

	/** This int value saves the number of added rows.
	 */
	public int rows = 0;
	public int rowCount = -1;
	/** This int value represents the number of used columns.
	 */
	public int columnCount = 0;
	/** This is the Vector, which saves the added Vectors. If you want
	 * to access one of the rows, you call row.get(k), which returns row k.<br>
	 * <br>
	 * Caution: as the variable is public, you can create a
	 * NullPointer exeption, if the row k is selected, but rowCount is smaller then k!
	 */
	public Vector row = new Vector();
	/** This Vector contains the maximum String sizes of the Vector fields,
	 * which are added to the rows. This is needed for the TableConstructor.
	 * The TC calculates its rectangle sizes based on these values
	 */
	public Vector columnSize = new Vector();

	/** returns an int value, which was saved as an Object.<br>
	 * <br>
	 * the method calls: Integer.parseInt(String.valueOf(o));<br>
	 * <br>
	 * @param o object, which is an int
	 * @return int value
	 */
	public int obj2int(Object o) {
		// Converts an Object to an Integer, as far as that is possible
		return Integer.parseInt(String.valueOf(o));
	}

	/** Removes a row and writes a new row on the place of the old one<br>
	 * . The place is selected by the given int value.<br>
	 *
	 * @param adresse row which will be updated
	 * @param content content which will be added to the adress
	 */
	public void updateRow(int adresse, Vector content) {

		if (adresse <= row.size()) {

			String zk = "";

			// loads the reference of the old vector, and removes the vector
			Vector alteReihe = (Vector) row.elementAt(adresse);
			//row.removeElementAt(adresse);

			// copies all values from content to v
			Vector v = new Vector();

			for (int i = 0; i < content.size(); i++) {
				v.addElement(content.elementAt(i));
			}

			// adds the new Vector as a row to row
			row.setElementAt(v, adresse);

			// checks the size of the Strings within the Vector
			for (int i = 0; i < content.size(); i++) {

				if (columnSize.size() <= i) {
					columnSize.addElement("0");
				}

				if (content.elementAt(i) != null) {
					zk = String.valueOf(content.elementAt(i));

					int size = obj2int(columnSize.elementAt(i));
					if (size < zk.length()) {
						//columnSize.remove(i);
						columnSize.setElementAt(String.valueOf(zk.length()), i);
					}
				}
			}
		}
	}

	/** Adds a new row at the end of the table.<br>
	 *  This row contains the given Vector. To access the last row:
	 * row.get(row.size()-1);<br>
	 * <br>
	 * @param content Vector with alphanumeric values<br>
	 */
	public void addRow(Vector content) {
		/* This method adds a new row to the table structure. <br>
		 *  the new row will be saved as a new Vector in the Vector row.<br>
		   <br>
		 *  Information from content will be copied to v<br>
		 *  v is local, its reference will be added to row. Variable <br>
		 *  v will be deleted at the and of addRow(..)*/

		row.addElement(content); // the new row has been added to the vector
		rowCount++; // rowCount is also used in drawTable(Graphics g)
		String zk = ""; // temporary String variable.

		if (content.size() > columnCount) {
			/* if the size of the new content is larger then the current table,
			 * more columns have to be displayed. ColumnCount is also used in
			 * drawTable(Graphics g)			 */
			columnCount = content.size();
		}

		for (int i = 0; i < content.size(); i++) {
			/* In this iteration the text sizes will be compared to each other<br>
			 * in drawTable(..) each border of a column will be adjusted by<br>
			 * the maximum text size in one of the column fields:
			 * max(column[1..n].length())<br>
			 <br>
			 * This part is important because the drawRectangle method will need
			     * the current drawing position AND the maximum textsize in this column.
			 */

			if (columnSize.size() <= i) {
				// because ColumnSize is a Vector, each new field has to be
				// added, if it doesn't exist. It will be set with "0" = 0;
				columnSize.addElement("0");
			}

			if (content.elementAt(i) != null) {
				/* field[i] is the i-th field of the Vector, which has been added
				 * (content)<br>
				 *  if field[i] exists, field[i].size will be compared to the
				 * current maxSize of the column[i]. if column[i].size is smaller
				 *  then field[i].size, the new maxSize will be changed.
				 */

				zk = String.valueOf(content.elementAt(i));

				int size = obj2int(columnSize.elementAt(i));
				if (size < zk.length()) {
					//columnSize.remove(i);
					columnSize.setElementAt(String.valueOf(zk.length()), i);
				}
			}
		}
	}

	/** adds a new column at end of the dataTable
	 * .
	 *
	 * @param content Vector which has alphanumeric values
	 */
	public void addColumn(Vector content) {

		int size = 0;
		String s = null;
		Vector reihe = null;
		int maxSize = 0;

		for (int i = 0; i < row.size(); i++) {
			Vector k = (Vector) row.elementAt(i);
			if (maxSize < k.size()) {
				maxSize = k.size();
			}
		}

		for (int i = 0; i < content.size(); i++) {

			if (row.size() > i) {
				reihe = (Vector) row.elementAt(i);

				for (int c = reihe.size(); c < maxSize; c++) {
					reihe.addElement("");
				}
				reihe.addElement(content.elementAt(i));

			} else {
				reihe = new Vector();

				for (int c = 0; c < maxSize; c++) {
					reihe.addElement("");
				}

				reihe.addElement(content.elementAt(i));
				row.addElement(reihe);
			}

			s = String.valueOf(content.elementAt(i));

			if (s.length() > size) {
				size = s.length();
			}

		}

		columnCount++;

		if (content.size() - 1 > rowCount) {
			rowCount = content.size() - 1;
		}

		columnSize.addElement(String.valueOf(size));
	}
}
