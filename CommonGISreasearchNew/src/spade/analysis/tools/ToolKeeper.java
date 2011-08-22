package spade.analysis.tools;

import java.util.Vector;

/**
* The class is used for keeping specifications of various tools, checking
* tool availability, constructing the tools, etc.
*/
public class ToolKeeper {
	/**
	* The list of all available tools. For each tool specifies
	* its internal identifier, name, and the full name of the class that
	* implements or constructs this tool.
	*/
	protected String tools[][] = null;
	/**
	* The list of identifiers of available tools (to avoid multiple checking of
	* presence of classes)
	*/
	protected Vector availableTools = null;
	/**
	* The instances of tools that allow only one instance to be created during a
	* session (such tools implement the interface SingleInstanceTool). When such
	* a tool is needed, the ToolKeeper first tries to find a previously created
	* instance in this vector. Only if there is no such instance, a new instance
	* is produced, and the reference is stored for further use.
	*/
	protected Vector singleInstanceTools = null;
	/**
	* The error message
	*/
	protected String err = null;

	/**
	* Sets the list of tool descriptions, which specifies for each tool
	* its internal identifier, name, and the full name of the class that
	* implements or constructs this tool.
	*/
	public ToolKeeper(String toolDescr[][]) {
		tools = toolDescr;
	}

	/**
	* Takes the list of tool descriptions from the given ToolDescriptor.
	*/
	public ToolKeeper(ToolDescriptor descriptor) {
		if (descriptor != null) {
			tools = descriptor.getToolDescription();
		}
	}

	/**
	* Returns the index of he tool with the given identifier in the array of
	* ALL tools
	*/
	public int getToolIndex(String toolId) {
		if (toolId == null)
			return -1;
		for (int i = 0; i < tools.length; i++)
			if (toolId.equals(tools[i][0]))
				return i;
		return -1;
	}

	/**
	* Returns the full name of the tool with the given identifier
	*/
	public String getToolName(String toolId) {
		int n = getToolIndex(toolId);
		if (n < 0)
			return null;
		return tools[n][1];
	}

	/**
	* Returns a vector of identifiers of available tools
	*/
	public Vector getAvailableToolList() {
		if (availableTools == null) {
			availableTools = new Vector(20, 5);
			for (String[] tool : tools)
				if (tool[2] != null) { //the name of the class implementing the tool
					try {
						Class.forName(tool[2]);
						availableTools.addElement(tool[0]);
						System.out.println("available: " + tool[0] + ", class = " + tool[2]);
					} catch (Exception e) {
						System.out.println("NOT available: " + tool[0] + ", class = " + tool[2]);
					}
				}
			availableTools.trimToSize();
		}
		return availableTools;
	}

	/**
	* Replies whether the tool with the given identifier
	* is implemented or available in the system.
	*/
	public boolean isToolAvailable(String toolId) {
		if (toolId == null)
			return false;
		if (availableTools == null) {
			getAvailableToolList();
		}
		if (availableTools == null)
			return false;
		return availableTools.contains(toolId);
	}

	/**
	* Returns the number of available tools
	*/
	public int getAvailableToolCount() {
		if (availableTools == null) {
			getAvailableToolList();
		}
		return availableTools.size();
	}

	/**
	* Returns the identifier of the AVAILABLE tool with the given index in the
	* list of available tools
	*/
	public String getAvailableToolId(int toolN) {
		if (toolN < 0 || toolN >= getAvailableToolCount())
			return null;
		return (String) availableTools.elementAt(toolN);
	}

	/**
	* Returns the full name of the AVAILABLE tool with the given index in the
	* list of available tools
	*/
	public String getAvailableToolName(int toolN) {
		return getToolName(getAvailableToolId(toolN));
	}

	/**
	* Returns the index of the available tool with the given identifier
	*/
	public int getAvailableToolIndex(String toolId) {
		if (toolId == null)
			return -1;
		if (availableTools == null) {
			getAvailableToolList();
		}
		if (availableTools == null)
			return -1;
		return availableTools.indexOf(toolId);
	}

	/**
	* Returns the class name of the tool with the given identifier
	*/
	public String getToolClassName(String toolId) {
		return getToolClassName(getToolIndex(toolId));
	}

	/**
	* Returns the class name of the tool with the given index
	*/
	public String getToolClassName(int idx) {
		if (idx < 0)
			return null;
		return tools[idx][2];
	}

	/**
	* Constructs a class instance according to the given tool identifier.
	*/
	public Object getTool(String toolId) {
		return getTool(getToolIndex(toolId));
	}

	/**
	* Constructs a display generator for the tool with the given index
	*/
	public Object getTool(int midx) {
		if (midx < 0)
			return null;
		String className = tools[midx][2];
		if (className == null)
			return null; //the tool is not implemented
		//check if an instance of this tool already exists
		if (singleInstanceTools != null) {
			for (int i = 0; i < singleInstanceTools.size(); i++) {
				Object obj = singleInstanceTools.elementAt(i);
				if (className.equals(obj.getClass().getName()))
					return obj;
			}
		}
		try {
			Object tool = Class.forName(className).newInstance();
			if (tool != null) {
				if (tool instanceof SingleInstanceTool) {
					if (singleInstanceTools == null) {
						singleInstanceTools = new Vector(10, 5);
					}
					singleInstanceTools.addElement(tool);
				}
				return tool;
			}
		} catch (Exception e) {
			err = e.toString();
			System.out.println(err);
		}
		return null;
	}

	/**
	* If construction of a tool failed, returns the message
	* explaining the reason of this.
	*/
	public String getErrorMessage() {
		return err;
	}
}