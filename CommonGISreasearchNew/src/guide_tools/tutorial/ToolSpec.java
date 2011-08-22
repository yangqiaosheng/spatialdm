package guide_tools.tutorial;

import java.io.PrintStream;
import java.util.Vector;

public class ToolSpec {
	/**
	* The type of the tool, e.g. dynamic_query, scatter_plot etc. (see
	* the identifiers defined in spade.analysis.system.ToolManager)
	*/
	public String toolType = null;
	/**
	* The identifier of the table with data to be visualized by the tool
	*/
	public String tblId = null;
	/**
	* The list of the attributes from the table that must be visualized by the tool
	*/
	public Vector attr = null;

	public void printToStream(PrintStream ps) {
		ps.println("Supplementary analysis tool: " + toolType);
		ps.println("Table: " + tblId);
		ps.println("Attributes:");
		if (attr != null) {
			for (int i = 0; i < attr.size(); i++) {
				ps.println("  " + attr.elementAt(i).toString());
			}
		}
	}
}