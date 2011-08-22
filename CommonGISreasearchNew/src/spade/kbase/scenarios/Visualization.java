package spade.kbase.scenarios;

import java.util.StringTokenizer;
import java.util.Vector;

/**
* Description of a map visualisation method (including instructions about
* its use)
*/
public class Visualization {
	static public final String visVariables[] = { "size", "value", "colour", "shape", "orientation", "presence" };
	static public final String visMethods[] = { "qualitative_colour", "shape", "logical_bunch", "value_paint", "multiples", "standalone_bars", "parallel_bars", "radial_bars", "segmented_bars", "doublesided_bars", "pies", "inclusion_signs",
			"utility_bars", "utility_wheels", "triangles", "class1D", "class2D", "class_dominant" };
	static public final int minComplexity = 1, maxComplexity = 3;

	public String name = null;
	public String explanation = null;
	public String variable = null; //visual variable employed in the method
	public String method = null; //visualization method
	public int complexity = 1; //from 1 to 3
	public Vector restrictions = null; //Vector of instances of Restriction
	/**
	* A map visualization method may be used for several primitive tasks
	* (see Common.primTasks). This vector contains the list of tasks supported
	* by the manipulator
	*/
	public Vector tasksSupported = null;
	/**
	* There may be seperate instructions for the primitive tasks supported.
	*/
	public Vector instructions = null, instrTasks = null;
	public Vector manipulators = null; //Vector of instances of Manipulator

	public String getName() {
		return name;
	}

	public void setName(String txt) {
		name = txt;
	}

	public String getExplanation() {
		return explanation;
	}

	public void setExplanation(String txt) {
		explanation = txt;
	}

	public void setVariable(String value) {
		if (value != null && value.length() > 0) {
			for (String visVariable : visVariables)
				if (value.equalsIgnoreCase(visVariable)) {
					variable = visVariable;
					break;
				}
		}
	}

	public void setMethod(String value) {
		if (value != null && value.length() > 0) {
			for (String visMethod : visMethods)
				if (value.equalsIgnoreCase(visMethod)) {
					method = visMethod;
					break;
				}
		}
	}

	public void setComplexity(int val) {
		if (val < minComplexity) {
			complexity = minComplexity;
		} else if (val > maxComplexity) {
			complexity = maxComplexity;
		} else {
			complexity = val;
		}
	}

	public void setComplexity(String str) {
		if (str != null) {
			try {
				int k = Integer.valueOf(str).intValue();
				setComplexity(k);
			} catch (NumberFormatException nfe) {
			}
		}
	}

	/**
	* Adds a task to the list of primitive tasks supported by this manipulator
	*/
	public void addSupportedTask(String task) {
		if (task == null)
			return;
		if (tasksSupported == null) {
			tasksSupported = new Vector(5, 5);
		}
		if (tasksSupported.indexOf(task) < 0) {
			tasksSupported.addElement(task);
		}
	}

	/**
	* The argument liststr contains a list of values separated by whitespaces
	*/
	public void setSupportedTasks(String liststr) {
		if (liststr == null)
			return;
		StringTokenizer st = new StringTokenizer(liststr, " ,;\t\n\r\f");
		if (!st.hasMoreTokens())
			return;
		while (st.hasMoreTokens()) {
			addSupportedTask(st.nextToken());
		}
	}

	public void addRestriction(Restriction restr) {
		if (restr == null)
			return;
		if (restrictions == null) {
			restrictions = new Vector(10, 5);
		}
		restrictions.addElement(restr);
	}

	public void addInstruction(String instr) {
		addInstruction(instr, null);
	}

	/**
	* The argument "task" indicates for what task (way of use of the visualization
	* method) the instruction is relevant
	*/
	public void addInstruction(String instr, String task) {
		if (instr == null)
			return;
		if (instructions == null) {
			instructions = new Vector(10, 5);
		}
		instructions.addElement(instr);
		if (task == null)
			return;
		if (instrTasks == null) {
			instrTasks = new Vector(10, 5);
		}
		for (int i = instrTasks.size(); i < instructions.size() - 1; i++) {
			instrTasks.addElement(null);
		}
		instrTasks.addElement(task);
	}

	/**
	* Returns the general instruction (i.e. not marked by a particular task)
	*/
	public String getGeneralInstruction() {
		if (instructions == null || instructions.size() < 1)
			return null;
		if (instrTasks == null)
			return (String) instructions.elementAt(0);
		for (int i = 0; i < instrTasks.size(); i++)
			if (instrTasks.elementAt(i) == null)
				return (String) instructions.elementAt(i);
		if (instructions.size() > instrTasks.size())
			return (String) instructions.elementAt(instrTasks.size());
		return null;
	}

	/**
	* Returns instruction associated with the given task (see the list of tasks
	* above)
	*/
	public String getInstruction(String task) {
		if (task == null)
			return getGeneralInstruction();
		if (instructions == null || instructions.size() < 1)
			return null;
		if (instrTasks == null || instrTasks.size() < 1)
			return null;
		for (int i = 0; i < instrTasks.size(); i++)
			if (task.equalsIgnoreCase((String) instrTasks.elementAt(i)))
				return (String) instructions.elementAt(i);
		return null;
	}

	/**
	* Adds a map manipulation tool specification
	*/
	public void addManipulator(Manipulator man) {
		if (man == null)
			return;
		if (manipulators == null) {
			manipulators = new Vector(10, 5);
		}
		manipulators.addElement(man);
	}

	@Override
	public String toString() {
		String str = "Visualization: method=" + method + " variable=" + variable + " complexity=" + complexity;
		str += "\n  Name=<" + name + ">\n";
		if (restrictions != null && restrictions.size() > 0) {
			for (int i = 0; i < restrictions.size(); i++) {
				str += "  " + restrictions.elementAt(i).toString() + "\n";
			}
		}
		if (explanation != null) {
			str += "  Explanation=<" + explanation + ">\n";
		}
		if (instructions != null && instructions.size() > 0) {
			for (int i = 0; i < instructions.size(); i++)
				if (instrTasks == null || instrTasks.size() <= i || instrTasks.elementAt(i) == null) {
					str += "  Instruction=<" + instructions.elementAt(i) + ">\n";
				} else {
					str += "  Instruction(" + instrTasks.elementAt(i) + ")=<" + instructions.elementAt(i) + ">\n";
				}
		}
		if (manipulators != null && manipulators.size() > 0) {
			for (int i = 0; i < manipulators.size(); i++) {
				str += manipulators.elementAt(i).toString();
			}
		}
		str += "/Visualization\n";
		return str;
	}

	/**
	* Determines whether this visualization method is applicable to the given
	* number of attributes with given types and relationships
	*/
	public boolean isApplicable(int nAttrs, char attrTypes[], String attrRel) {
		if (restrictions == null || restrictions.size() < 1)
			return true;
		for (int i = 0; i < restrictions.size(); i++) {
			Restriction r = (Restriction) restrictions.elementAt(i);
			if (r.type == null || r.values == null || r.values.size() < 1) {
				continue;
			}
			if (r.type.equals("min_attr_number") || r.type.equals("max_attr_number")) {
				int n = -1;
				String str = (String) r.values.elementAt(0);
				try {
					n = Integer.valueOf(str).intValue();
				} catch (NumberFormatException nfe) {
				}
				if (n < 0) {
					continue;
				}
				if (r.type.equals("min_attr_number"))
					if (nAttrs < n)
						return false;
					else {
						;
					}
				else if (nAttrs > n)
					return false;
			} else if (r.type.equals("attr_type")) {
				for (int j = 0; j < r.values.size(); j++) {
					char type = Common.encodeAttrType((String) r.values.elementAt(j));
					if (type == 'U') {
						continue;
					}
					boolean found = false;
					for (int k = 0; k < nAttrs && !found; k++) {
						found = attrTypes[k] == type;
					}
					if (!found)
						return false;
				}
			} else if (r.type.equals("attr_relation")) {
				if (attrRel == null)
					return false;
				String rel = (String) r.values.elementAt(0);
				if (rel.equalsIgnoreCase(attrRel))
					return true;
				if (rel.equalsIgnoreCase("comparable"))
					return attrRel.equalsIgnoreCase("parts_of_whole") || attrRel.equalsIgnoreCase("included");
				return false;
			}
		}
		return true;
	}

	/**
	* Checks if the given task is supported by this visualization method itself
	* (without regard of the possible manipulators)
	*/
	public boolean isTaskSupported(String task) {
		if (task == null || tasksSupported == null)
			return false;
		for (int i = 0; i < tasksSupported.size(); i++)
			if (task.equalsIgnoreCase((String) tasksSupported.elementAt(i)))
				return true;
		return false;
	}

	/**
	* Returns the number of possible manipulators of this visualization method
	*/
	public int getManipulatorCount() {
		if (manipulators == null)
			return 0;
		return manipulators.size();
	}

	/**
	* Returns the specification of the manipulator with the given index
	*/
	public Manipulator getManipulator(int idx) {
		if (idx < 0 || idx >= getManipulatorCount())
			return null;
		return (Manipulator) manipulators.elementAt(idx);
	}

	/**
	* Checks if the given task is supported by at least one of manipulators
	* of this visualization method
	*/
	public boolean isTaskSupportedByManipulator(String task) {
		if (task == null || getManipulatorCount() < 1)
			return false;
		for (int j = 0; j < getManipulatorCount(); j++)
			if (getManipulator(j).isTaskSupported(task))
				return true;
		return false;
	}
}