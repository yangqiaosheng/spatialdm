package spade.analysis.geocomp;

import java.util.Vector;

import spade.vis.database.DataTable;
import spade.vis.space.GeoLayer;

/**
* Describes attributes (results of calculations) added to a table
*/
public class AttrSpec {
	/**
	* The table in which the attribute was added
	*/
	public DataTable table = null;
	/**
	* The layer the table is attached to
	*/
	public GeoLayer layer = null;
	/**
	* The identifiers of the new attributes added to the table
	*/
	public Vector attrIds = null;
}