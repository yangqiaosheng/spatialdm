package spade.analysis.generators;

import java.awt.Component;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.plot.QueryOrSearchTool;
import spade.analysis.system.Supervisor;
import spade.analysis.tools.ToolKeeper;
import spade.lib.lang.Language;
import spade.vis.database.ObjectContainer;

/**
* Produces tools for query and search such as dynamic queries or object lists.
*/
public class QueryAndSearchToolProducer {
	static ResourceBundle res = Language.getTextResource("spade.analysis.generators.Res");
	/**
	* The object managing descriptions of all available query and search tools. A
	* class implementing such a tool must descend from java.awt.Component
	* (but must not be a Window) and implement the interface
	* spade.analysis.plot.QueryOrSearchTool
	*/
	protected ToolKeeper toolKeeper = new ToolKeeper(new QueryAndSearchToolsDescriptor());
	/**
	* The error message
	*/
	protected String err = null;

	/**
	* Returns a reference to its tool keeper
	*/
	public ToolKeeper getToolKeeper() {
		return toolKeeper;
	}

	/**
	* Returns the number of available tools
	*/
	public int getAvailableToolCount() {
		return toolKeeper.getAvailableToolCount();
	}

	/**
	* Returns the identifier of the AVAILABLE tool with the given index
	*/
	public String getAvailableToolId(int toolN) {
		return toolKeeper.getAvailableToolId(toolN);
	}

	/**
	* Returns the full name of the AVAILABLE tool with the given index
	*/
	public String getAvailableToolName(int toolN) {
		return toolKeeper.getAvailableToolName(toolN);
	}

	/**
	* Returns the index of the available tool with the given identifier
	*/
	public int getAvailableToolIndex(String toolId) {
		return toolKeeper.getAvailableToolIndex(toolId);
	}

	/**
	* Replies whether the tool with the given identifier
	* is implemented or available in the system.
	*/
	public boolean isToolAvailable(String toolId) {
		return toolKeeper.isToolAvailable(toolId);
	}

	/**
	* Replies whether the tool with the given identifier requires attributes
	* for its operation.
	*/
	public boolean isToolAttributeFree(String toolId) {
		String className = toolKeeper.getToolClassName(toolId);
		if (className == null)
			return false;
		try {
			Class cl = Class.forName(className);
			if (cl == null)
				return false;
			Class inter[] = cl.getInterfaces();
			if (inter == null || inter.length < 1)
				return false;
			for (Class element : inter)
				if (element.getName().equals("spade.analysis.plot.AttributeFreeTool"))
					return true;
		} catch (Exception e) {
			return false;
		}
		return false;
	}

	/**
	 * Replies whether the tool implements the interface ObjectsSuitabilityChecker,
	 * i.e. can check if a given object container is suitable for this tool
	 */
	public boolean canToolCheckObjectsSuitability(String toolId) {
		String className = toolKeeper.getToolClassName(toolId);
		if (className == null)
			return false;
		try {
			Class cl = Class.forName(className);
			if (cl == null)
				return false;
			Class inter[] = cl.getInterfaces();
			if (inter == null || inter.length < 1)
				return false;
			for (Class element : inter)
				if (element.getName().equals("spade.analysis.plot.ObjectsSuitabilityChecker"))
					return true;
		} catch (Exception e) {
			return false;
		}
		return false;
	}

	/**
	 * Generates an instance of the tool
	 */
	public QueryOrSearchTool makeToolInstance(String toolId) {
		Object obj = toolKeeper.getTool(toolId);
		if (obj == null) {
			err = toolKeeper.getErrorMessage();
			return null;
		}
		if (!(obj instanceof QueryOrSearchTool)) {
			// following string:  toolId+": class "+obj.getClass().getName()+" does not implement "
			err = toolId + res.getString("_class") + obj.getClass().getName() + " " + res.getString("does_not_implement") + " QueryOrSearchTool!";
			return null;
		}
		return (QueryOrSearchTool) obj;
	}

	/**
	* Constructs the query or search tool with the given identifier
	*/
	public QueryOrSearchTool constructTool(String toolId, ObjectContainer cont, Vector attributes, Supervisor sup) {
		Object obj = toolKeeper.getTool(toolId);
		if (obj == null) {
			err = toolKeeper.getErrorMessage();
			return null;
		}
		if (!(obj instanceof Component)) {
			err = toolId + res.getString("_class") + obj.getClass().getName() + " " + res.getString("does_not_descend_from") + " Component!";
			return null;
		}
		if (!(obj instanceof QueryOrSearchTool)) {
			// following string:  toolId+": class "+obj.getClass().getName()+" does not implement "
			err = toolId + res.getString("_class") + obj.getClass().getName() + " " + res.getString("does_not_implement") + " QueryOrSearchTool!";
			return null;
		}
		if (!cont.hasData()) {
			cont.loadData();
		}
		QueryOrSearchTool qst = (QueryOrSearchTool) obj;
		qst.setObjectContainer(cont);
		if (attributes != null && attributes.size() > 0) {
			qst.setAttributeList(attributes);
		}
		qst.setSupervisor(sup);
		if (qst.construct())
			return qst;
		err = qst.getErrorMessage();
		return null;
	}

	/**
	* If construction of the graphical display failed, returns the message
	* explaining the reason of this.
	*/
	public String getErrorMessage() {
		return err;
	}
}
