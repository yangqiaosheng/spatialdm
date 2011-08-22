package spade.analysis.datamanage;

import java.util.Vector;

import spade.vis.database.AttributeDataPortion;
import spade.vis.database.DataItem;

/** This class is used as a data record. The field "ID" will contain the id of a
 * DataItem (-> AttributeDataPortion) and the field "field" will be the
 * field number saved as an int.
 *
 * @author Christian Gro�
 * @version 1.0
 */
class SmallDataManagement {
	/** This field saves the int value of the DataItem position in the
	 * AttributeDataPortion
	 */
	public int field;
	/** This field saves the ID of a DataItem.
	 */
	public String ID;

	/** This constructor constructs a SmallDataManagement. The given
	 * values, field and ID, will be saved public, so that other classes
	 * can access these values.<br>
	 * <br>
	 * @param field Integer value, where the DataItem is saved in
	 *     AttributeDataPortion
	 * @param ID The value of the DataItem ID
	 */
	public SmallDataManagement(int field, String ID) {
		this.field = field;
		this.ID = ID;
	}
}

/** This class is used to filter the attribute data for selected regions.
 * The regions will be given as a Vector filled with indices, for which the
 * DataItems will be searched, which ID will be saved within an int array.<br>
 * <br>
 * If the DataFilter is needed, you have to first add an AttributeDataPortion
 * with the method: setTable(AttributeDataPortion). Then you can send a Vector
 * with DataItem IDs, on which the next result of a call from filter(int) will
 * be based. Filter returns a Vector with the  selected Attribute,
 * which is filtered for the selected regions.<br>
 */
public class DataFilter {

	/** This field saves the AttributeDataPortion
	 */
	private AttributeDataPortion dataTable = null;
	/** The Region IDs as int values
	 */
	private int[] regionIDs = null;

	/** This Vector will save the indices. The Vector will have 27
	 * fields, one for each letter in the alphabet plus one. All other letters will be
	 * saved in field 27.
	 */
	public Vector indices = null;

	/** This method is given a Vector with region IDs and returns an
	 * Int Array with the DataItem IDs.<br>
	 * <br>
	 * The method sorts the given DataItems (within an AttributeDataPortion) in
	 * a new Vector of SmallDataManagement instances that are given ID and
	 * position int of the DataItem. 27 Vectors will be created for all
	 * alphabet letters (other Letters will be put in the 27th Vector).<br>
	 * <br>
	 * This indices are needed to optimize the runtime.<br>
	 * <br>
	 * @param dataTable AttributeDataPortion of which DataItem IDs an index will
	 *      be created
	 * @param v region as ID Strings
	 * @return IDs of DataItems
	 */

	private Vector generateIndices(AttributeDataPortion dataTable) {
		String tempID = "";
		char sort;

		Vector indices = new Vector();

		for (int i = 0; i < 27; i++) {
			indices.addElement(new Vector());
		}

		for (int k = 0; k < dataTable.getDataItemCount(); k++) {
			DataItem d = this.dataTable.getDataItem(k); // gets the k-th DataItem, which include the regionname;
			tempID = d.getId();
			tempID = tempID.toLowerCase();
			sort = tempID.charAt(0);

			if ((sort < 97) || (sort > 124)) {
				sort = 123;
			}

			((Vector) indices.elementAt(sort - 97)).addElement(new SmallDataManagement(k, tempID));

		}
		return indices;
	}

	/** Sets the AttributeDataPortion, for which the indices will be created
	 * and which gives the values, which will be filtered<br>
	 * <br>
	 * @param dataTable The AttributeDataPortion, which is needed to filter
	 */
	public void setTable(AttributeDataPortion dataTable) {

		this.dataTable = dataTable;
		indices = generateIndices(dataTable);

	}

	/** This method is needed to calculate the region IDs for the
	 * given DataItem IDs that are added in setRegion(Vector).
	 * Based on the returned int array, the method filter
	 * returns the filtered values of the AttributeDataPortion
	 *
	 * @param v Vector of DataItem - IDs
	 * @return int array that includes the positions of the DataItems in the
	 *     given AttributeDataPortion
	 */
	private int[] getRegionIDs(Vector v) {

		if ((v != null) && (dataTable != null)) {

			int[] temp = new int[v.size()];

			String vString = null;

			DataItem d = null;
			String dId = "";
			Vector index = null;

			SmallDataManagement dataField = null;

			char pos = 0;

			int k = 0;

			for (int x = 0; x < v.size(); x++) {

				dId = (v.elementAt(x).toString()).toLowerCase();

				if (dId.length() > 0) {
					pos = (dId.toLowerCase()).charAt(0);
				} else {
					pos = 123;
				}

				if ((pos < 97) || (pos > 124)) {
					pos = 123;
				}

				index = (Vector) indices.elementAt(pos - 97);

				for (int i = 0; i < index.size(); i++) {
					dataField = (SmallDataManagement) index.elementAt(i);

					if (dId.compareTo(dataField.ID) == 0) {
						temp[k++] = dataField.field;
					}
				}

			}

			return temp;

		} else
			return null;

	}

	/** This method saves the position of DataItems, which
	 * are selected within the Vector.
	 *
	 * @param v Vector with region ID strings
	 */
	public void setRegion(Vector v) {
		this.regionIDs = getRegionIDs(v);
	}

	/** Removes the selected regions, so that filter() won't return any value.
	 */
	public void clearRegions() {
		this.regionIDs = null;
	}

	/** With the given int value, an attribute is selected, which will be filtered.<br>
	 * <br>
	     * An attribute Vector from the previously added AttributeDataPortion will be
	     * filtered. This filter method is based on the given DataItem ID Vector, which
	 * has been set to setRegion(vector).<br>
	 * <br>
	 * The method returns a filtered Vector which contains only values for the
	 * selected DataItems<br>
	 * .<br>
	 * <br>
	 * @param selectedAttribute Attribute which will be filtered
	 * @return filtered Vector with attribute for the selected region
	 */
	public Vector filter(int selectedAttribute) {
		// selectedAttribute = attribute index
		Vector temp = new Vector();

		if ((regionIDs != null) && (dataTable != null)) {
			for (int regionID : regionIDs) {

				Object value = dataTable.getAttrValue(selectedAttribute, regionID);
				if (value != null) {
					float v = Float.valueOf((String) value).floatValue();
					temp.addElement(new Float(v));
				} else {
					temp.addElement(new Float(Float.NaN));
				}

			}
		}
		return temp;
	}
}
