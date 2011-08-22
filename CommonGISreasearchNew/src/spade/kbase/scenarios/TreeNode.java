package spade.kbase.scenarios;

import java.util.Vector;

/**
* Represents description of a task, task group, or scenario.
*/
public class TreeNode {
	static public final String nodeTypes[] = { "scenario", "task_group", "task" };

	protected String id = null, parentId = null, type = null;
	public Vector restrictions = null; //Vector of instances of Restriction
	public Vector context = null; //Vector of instances of ContextElement
	public Vector instruments = null; //Vector of instances of Instrument

	public String name = null, shortName = null;
	public String explanation = null;

	public TreeNode(String id, String parentId) {
		this.id = id;
		if (parentId != null) {
			parentId = parentId.trim();
			if (parentId.length() > 0) {
				this.parentId = parentId;
			}
		}
	}

	public String getId() {
		return id;
	}

	public String getParentId() {
		return parentId;
	}

	public String getName() {
		return name;
	}

	public void setName(String txt) {
		name = txt;
	}

	public String getShortName() {
		if (shortName == null)
			return name;
		return shortName;
	}

	public void setShortName(String txt) {
		shortName = txt;
	}

	public String getExplanation() {
		return explanation;
	}

	public void setExplanation(String txt) {
		explanation = txt;
	}

	public void setNodeType(String value) {
		if (value != null && value.length() > 0) {
			for (String nodeType : nodeTypes)
				if (value.equalsIgnoreCase(nodeType)) {
					type = nodeType;
					break;
				}
		}
	}

	public String getNodeType() {
		return type;
	}

	public void addRestriction(Restriction restr) {
		if (restr == null)
			return;
		if (restrictions == null) {
			restrictions = new Vector(10, 5);
		}
		restrictions.addElement(restr);
	}

	public void addContext(ContextElement cel) {
		if (cel == null)
			return;
		if (context == null) {
			context = new Vector(10, 5);
		}
		context.addElement(cel);
	}

	public void addInstrument(Instrument instr) {
		if (instr == null)
			return;
		if (instruments == null) {
			instruments = new Vector(10, 5);
		}
		instruments.addElement(instr);
	}

	@Override
	public String toString() {
		String str = "Node (" + type + ") id=" + id;
		if (parentId != null) {
			str += " parent=" + parentId;
		}
		str += "\n  Name=<" + name + ">\n";
		if (shortName != null) {
			str += "  Short name=<" + shortName + ">\n";
		}
		if (explanation != null) {
			str += "  Explanation=<" + explanation + ">\n";
		}
		if (restrictions != null && restrictions.size() > 0) {
			for (int i = 0; i < restrictions.size(); i++) {
				str += "  " + restrictions.elementAt(i).toString() + "\n";
			}
		}
		if (context != null && context.size() > 0) {
			for (int i = 0; i < context.size(); i++) {
				str += context.elementAt(i).toString();
			}
		}
		if (instruments != null && instruments.size() > 0) {
			for (int i = 0; i < instruments.size(); i++) {
				str += instruments.elementAt(i).toString();
			}
		}
		str += "/Node\n";
		return str;
	}
}
