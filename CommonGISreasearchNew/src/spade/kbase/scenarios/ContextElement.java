package spade.kbase.scenarios;

import java.util.Vector;

/**
* Represents an element of task context such as layer, attributes etc.
*/
public class ContextElement {
	public static final String contextTypes[] = { "territory", "bound_rect", "layer", "attributes", "attr_filter", "appearance" };
	public static final String methods[] = { "select_one", "select_many", "system", "system_help" };

	public String type = null; //one of the contextTypes
	public String method = null; //one of the "methods"

	public String localId = null, refersTo = null; //attributes of the element
	public Vector restrictions = null; //Vector of Restriction

	public String name = null;
	public String prompt = null;
	public String instruction = null; //may be absent
	public boolean isOptional = false;
	/**
	* Identifier of the help topic to start when the user needs help about
	* the way of defining this context element. May be null.
	*/
	public String helpId = null;

	/**
	* Identifier of the task this context element belongs to (i.e. specification
	* of the context element is contained in the description of the task)
	*/
	public String taskId = null;

	public void setContextType(String value) {
		if (value != null && value.length() > 0) {
			for (String contextType : contextTypes)
				if (value.equalsIgnoreCase(contextType)) {
					type = contextType;
					break;
				}
		}
	}

	public void setMethod(String value) {
		if (value != null && value.length() > 0) {
			for (String method2 : methods)
				if (value.equalsIgnoreCase(method2)) {
					method = value;
					break;
				}
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String txt) {
		name = txt;
	}

	public String getPrompt() {
		return prompt;
	}

	public void setPrompt(String txt) {
		prompt = txt;
	}

	public String getInstruction() {
		return instruction;
	}

	public void setInstruction(String txt) {
		instruction = txt;
	}

	public String getHelpTopicId() {
		return helpId;
	}

	public void setHelpTopicId(String str) {
		helpId = str;
	}

	public String getTaskId() {
		return taskId;
	}

	public void setTaskId(String str) {
		taskId = str;
	}

	public void addRestriction(Restriction restriction) {
		if (restriction == null)
			return;
		if (restrictions == null) {
			restrictions = new Vector(3, 3);
		}
		restrictions.addElement(restriction);
	}

	public boolean isValid() {
		return type != null;
	}

	@Override
	public String toString() {
		String str = "ContextElement: type=" + type + " method=" + method + " id=" + localId;
		if (isOptional) {
			str += " optional";
		}
		if (refersTo != null) {
			str += " refers_to=" + refersTo;
		}
		str += "\n  Name=<" + name + ">\n";
		str += "  Prompt=<" + prompt + ">\n";
		if (instruction != null) {
			str += "  Instruction=<" + instruction + ">\n";
		}
		str += "/ContextElement\n";
		return str;
	}
}
