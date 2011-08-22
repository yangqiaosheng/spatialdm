package guide_tools.tutorial;

import java.io.PrintStream;
import java.util.Vector;

public class MapSpec {
	/**
	* The identifier of the table with data to be visualized
	*/
	public String tblId = null;
	/**
	* The list of the attributes from the table that must be visualized
	*/
	public Vector attr = null;
	/**
	* The identifier of the map representation method to be used (see in
	* spade.vis.mapvis.DataMapper).
	* The identifier may be null. This means that no presentation of thematic
	* data should be on the map.
	*/
	public String methodId = null;

	public void printToStream(PrintStream ps) {
		ps.println("Map specification:");
		if (methodId == null) {
			ps.println("  no visualization of thematic data");
			return;
		}
		ps.println("Table: " + tblId);
		ps.println("Attributes:");
		if (attr != null) {
			for (int i = 0; i < attr.size(); i++) {
				ps.println("  " + attr.elementAt(i).toString());
			}
		}
		ps.println("Method: " + methodId);
	}
}