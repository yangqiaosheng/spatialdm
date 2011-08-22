package spade.analysis.tools.events;

import spade.analysis.tools.ToolDescriptor;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Aug 4, 2009
 * Time: 11:21:00 AM
 * Contains descriptions of available tools for events (time-referenced geo objects).
 */
public class EventToolRegister implements ToolDescriptor {
	/**
	* The list of available tools.
	* For each tool specifies its identifier, name, and the full name of
	* the class implementing it. Each tool must implement the interface
	* spade.analysis.tools.DataAnalyser.
	*/
	protected static final String tools[][] = { { "events_by_areas", "Summarize events by areas and time intervals", "spade.analysis.tools.events.EventsByAreasSummarizer" } };

	/**
	* Returns the description of the known tools
	*/
	@Override
	public String[][] getToolDescription() {
		return tools;
	}

	/**
	 * Returns the name of the class implementing the tool with the given identifier
	 */
	public static String getToolClassName(String toolId) {
		if (toolId == null)
			return null;
		for (String[] tool : tools)
			if (toolId.equals(tool[0]))
				return tool[2];
		return null;
	}
}
